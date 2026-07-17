package androidx.media3.exoplayer;

import android.content.Context;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.SparseArray;
import androidx.media3.common.Format;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.MediaFormatUtil;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceUtil;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.exoplayer.source.SampleQueue;
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.DefaultAllocator;
import androidx.media3.extractor.DefaultExtractorInput;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.DiscardingTrackOutput;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.mp4.Mp4Extractor;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.EOFException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/* JADX INFO: loaded from: classes.dex */
public final class MediaExtractorCompat {
    public static final int SEEK_TO_CLOSEST_SYNC = 2;
    public static final int SEEK_TO_NEXT_SYNC = 1;
    public static final int SEEK_TO_PREVIOUS_SYNC = 0;
    private static final String TAG = "MediaExtractorCompat";
    private final Allocator allocator;
    private DataSource currentDataSource;
    private Extractor currentExtractor;
    private ExtractorInput currentExtractorInput;
    private final DataSource.Factory dataSourceFactory;
    private final ExtractorsFactory extractorsFactory;
    private final FormatHolder formatHolder;
    private boolean hasBeenPrepared;
    private final DecoderInputBuffer noDataBuffer;
    private long offsetInCurrentFile;
    private SeekPoint pendingSeek;
    private final PositionHolder positionHolder;
    private final DecoderInputBuffer sampleHolder;
    private final SparseArray<MediaExtractorSampleQueue> sampleQueues;
    private SeekMap seekMap;
    private final Set<Integer> selectedTrackIndices;
    private final ArrayDeque<Integer> trackIndicesPerSampleInQueuedOrder;
    private final ArrayList<MediaExtractorTrack> tracks;
    private boolean tracksEnded;
    private int upstreamFormatsCount;

    @Retention(RetentionPolicy.SOURCE)
    public @interface SeekMode {
    }

    public MediaExtractorCompat(Context context) {
        this(new DefaultExtractorsFactory(), new DefaultDataSource.Factory(context));
    }

    public MediaExtractorCompat(ExtractorsFactory extractorsFactory, DataSource.Factory dataSourceFactory) {
        this.extractorsFactory = extractorsFactory;
        this.dataSourceFactory = dataSourceFactory;
        this.positionHolder = new PositionHolder();
        this.allocator = new DefaultAllocator(true, 65536);
        this.tracks = new ArrayList<>();
        this.sampleQueues = new SparseArray<>();
        this.trackIndicesPerSampleInQueuedOrder = new ArrayDeque<>();
        this.formatHolder = new FormatHolder();
        this.sampleHolder = new DecoderInputBuffer(0);
        this.noDataBuffer = DecoderInputBuffer.newNoDataInstance();
        this.selectedTrackIndices = new HashSet();
    }

    public void setDataSource(Uri uri, long offset) throws IOException {
        int result;
        String message;
        Assertions.checkState(!this.hasBeenPrepared);
        this.hasBeenPrepared = true;
        this.offsetInCurrentFile = offset;
        DataSpec dataSpec = buildDataSpec(uri, this.offsetInCurrentFile);
        this.currentDataSource = this.dataSourceFactory.createDataSource();
        long length = this.currentDataSource.open(dataSpec);
        ExtractorInput currentExtractorInput = new DefaultExtractorInput(this.currentDataSource, 0L, length);
        Extractor currentExtractor = selectExtractor(currentExtractorInput);
        currentExtractor.init(new ExtractorOutputImpl());
        Throwable error = null;
        ExtractorInput currentExtractorInput2 = currentExtractorInput;
        boolean preparing = true;
        while (preparing) {
            try {
                result = currentExtractor.read(currentExtractorInput2, this.positionHolder);
            } catch (Exception | OutOfMemoryError e) {
                error = e;
                result = -1;
            }
            preparing = !this.tracksEnded || this.upstreamFormatsCount < this.sampleQueues.size() || this.seekMap == null;
            if (error != null || (preparing && result == -1)) {
                release();
                if (error != null) {
                    message = "Exception encountered while parsing input media.";
                } else {
                    message = "Reached end of input before preparation completed.";
                }
                throw ParserException.createForMalformedContainer(message, error);
            }
            if (result == 1) {
                currentExtractorInput2 = reopenCurrentDataSource(this.positionHolder.position);
            }
        }
        this.currentExtractorInput = currentExtractorInput2;
        this.currentExtractor = currentExtractor;
    }

    public void release() {
        SparseArray<MediaExtractorSampleQueue> sparseArray;
        int i = 0;
        while (true) {
            int size = this.sampleQueues.size();
            sparseArray = this.sampleQueues;
            if (i >= size) {
                break;
            }
            sparseArray.valueAt(i).release();
            i++;
        }
        sparseArray.clear();
        if (this.currentExtractor != null) {
            this.currentExtractor.release();
            this.currentExtractor = null;
        }
        this.currentExtractorInput = null;
        this.pendingSeek = null;
        DataSourceUtil.closeQuietly(this.currentDataSource);
        this.currentDataSource = null;
    }

    public int getTrackCount() {
        return this.tracks.size();
    }

    public MediaFormat getTrackFormat(int trackIndex) {
        return this.tracks.get(trackIndex).createDownstreamMediaFormat(this.formatHolder, this.noDataBuffer);
    }

    public void selectTrack(int trackIndex) {
        this.selectedTrackIndices.add(Integer.valueOf(trackIndex));
    }

    public void unselectTrack(int trackIndex) {
        this.selectedTrackIndices.remove(Integer.valueOf(trackIndex));
    }

    public void seekTo(long timeUs, int mode) {
        SeekMap.SeekPoints seekPoints;
        SeekPoint seekPoint;
        if (this.seekMap == null) {
            return;
        }
        if (this.selectedTrackIndices.size() == 1 && (this.currentExtractor instanceof Mp4Extractor)) {
            seekPoints = ((Mp4Extractor) this.currentExtractor).getSeekPoints(timeUs, this.tracks.get(this.selectedTrackIndices.iterator().next().intValue()).getIdOfBackingTrack());
        } else {
            seekPoints = this.seekMap.getSeekPoints(timeUs);
        }
        switch (mode) {
            case 0:
                seekPoint = seekPoints.first;
                break;
            case 1:
                seekPoint = seekPoints.second;
                break;
            case 2:
                if (Math.abs(timeUs - seekPoints.second.timeUs) < Math.abs(timeUs - seekPoints.first.timeUs)) {
                    seekPoint = seekPoints.second;
                } else {
                    seekPoint = seekPoints.first;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        this.trackIndicesPerSampleInQueuedOrder.clear();
        for (int i = 0; i < this.sampleQueues.size(); i++) {
            this.sampleQueues.valueAt(i).reset();
        }
        this.pendingSeek = seekPoint;
    }

    public boolean advance() {
        if (!advanceToSampleOrEndOfInput()) {
            return false;
        }
        skipOneSample();
        return advanceToSampleOrEndOfInput();
    }

    public int readSampleData(ByteBuffer buffer, int offset) {
        if (!advanceToSampleOrEndOfInput()) {
            return -1;
        }
        buffer.position(offset);
        buffer.limit(buffer.capacity());
        this.sampleHolder.data = buffer;
        peekNextSelectedTrackSample(this.sampleHolder, false);
        buffer.flip();
        buffer.position(offset);
        this.sampleHolder.data = null;
        return buffer.remaining();
    }

    public int getSampleTrackIndex() {
        if (!advanceToSampleOrEndOfInput()) {
            return -1;
        }
        return this.trackIndicesPerSampleInQueuedOrder.peekFirst().intValue();
    }

    public long getSampleTime() {
        if (!advanceToSampleOrEndOfInput()) {
            return -1L;
        }
        peekNextSelectedTrackSample(this.noDataBuffer, true);
        return this.noDataBuffer.timeUs;
    }

    public int getSampleFlags() {
        if (!advanceToSampleOrEndOfInput()) {
            return -1;
        }
        peekNextSelectedTrackSample(this.noDataBuffer, true);
        return 0 | (this.noDataBuffer.isEncrypted() ? 2 : 0) | (this.noDataBuffer.isKeyFrame() ? 1 : 0);
    }

    public Allocator getAllocator() {
        return this.allocator;
    }

    private void peekNextSelectedTrackSample(DecoderInputBuffer decoderInputBuffer, boolean omitSampleData) {
        MediaExtractorTrack trackOfSample = this.tracks.get(((Integer) Assertions.checkNotNull(this.trackIndicesPerSampleInQueuedOrder.peekFirst())).intValue());
        SampleQueue sampleQueue = trackOfSample.sampleQueue;
        int readFlags = (omitSampleData ? 4 : 0) | 1;
        int result = sampleQueue.read(this.formatHolder, decoderInputBuffer, readFlags, false);
        if (result == -5) {
            result = sampleQueue.read(this.formatHolder, decoderInputBuffer, readFlags, false);
        }
        this.formatHolder.clear();
        if (result != -4) {
            throw new IllegalStateException(Util.formatInvariant("Sample read result: %s\nTrack sample: %s\nTrackIndicesPerSampleInQueuedOrder: %s\nTracks added: %s\n", Integer.valueOf(result), trackOfSample, this.trackIndicesPerSampleInQueuedOrder, this.tracks));
        }
    }

    private Extractor selectExtractor(ExtractorInput input) throws IOException {
        Extractor[] extractors = this.extractorsFactory.createExtractors();
        Extractor result = null;
        for (Extractor extractor : extractors) {
            try {
                if (extractor.sniff(input)) {
                    result = extractor;
                    input.resetPeekPosition();
                    break;
                }
                input.resetPeekPosition();
            } catch (EOFException e) {
            } catch (Throwable th) {
                input.resetPeekPosition();
                throw th;
            }
        }
        if (result == null) {
            throw new UnrecognizedInputFormatException("None of the available extractors (" + Joiner.on(", ").join(Lists.transform(ImmutableList.copyOf(extractors), new Function() { // from class: androidx.media3.exoplayer.MediaExtractorCompat$$ExternalSyntheticLambda0
                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return ((Extractor) obj).getUnderlyingImplementation().getClass().getSimpleName();
                }
            })) + ") could read the stream.", (Uri) Assertions.checkNotNull(((DataSource) Assertions.checkNotNull(this.currentDataSource)).getUri()), ImmutableList.of());
        }
        return result;
    }

    @EnsuresNonNullIf(expression = {"trackIndicesPerSampleInQueuedOrder.peekFirst()"}, result = true)
    private boolean advanceToSampleOrEndOfInput() {
        try {
            maybeResolvePendingSeek();
            boolean seenEndOfInput = false;
            while (true) {
                if (!this.trackIndicesPerSampleInQueuedOrder.isEmpty()) {
                    if (this.selectedTrackIndices.contains(this.trackIndicesPerSampleInQueuedOrder.peekFirst())) {
                        return true;
                    }
                    skipOneSample();
                } else {
                    if (seenEndOfInput) {
                        return false;
                    }
                    try {
                        int result = ((Extractor) Assertions.checkNotNull(this.currentExtractor)).read((ExtractorInput) Assertions.checkNotNull(this.currentExtractorInput), this.positionHolder);
                        if (result == -1) {
                            seenEndOfInput = true;
                        } else if (result == 1) {
                            this.currentExtractorInput = reopenCurrentDataSource(this.positionHolder.position);
                        }
                    } catch (Exception | OutOfMemoryError e) {
                        Log.w(TAG, "Treating exception as the end of input.", e);
                        seenEndOfInput = true;
                    }
                }
            }
        } catch (IOException e2) {
            Log.w(TAG, "Treating exception as the end of input.", e2);
            return false;
        }
    }

    private void skipOneSample() {
        int trackIndex = this.trackIndicesPerSampleInQueuedOrder.removeFirst().intValue();
        MediaExtractorTrack track = this.tracks.get(trackIndex);
        if (!track.isCompatibilityTrack) {
            track.discardFrontSample();
        }
    }

    private ExtractorInput reopenCurrentDataSource(long newPositionInStream) throws IOException {
        long length;
        DataSource currentDataSource = (DataSource) Assertions.checkNotNull(this.currentDataSource);
        Uri currentUri = (Uri) Assertions.checkNotNull(currentDataSource.getUri());
        DataSourceUtil.closeQuietly(currentDataSource);
        long length2 = currentDataSource.open(buildDataSpec(currentUri, this.offsetInCurrentFile + newPositionInStream));
        if (length2 == -1) {
            length = length2;
        } else {
            length = length2 + newPositionInStream;
        }
        return new DefaultExtractorInput(currentDataSource, newPositionInStream, length);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSampleQueueFormatInitialized(MediaExtractorSampleQueue mediaExtractorSampleQueue, Format format) {
        boolean z = true;
        this.upstreamFormatsCount++;
        mediaExtractorSampleQueue.setMainTrackIndex(this.tracks.size());
        byte b = 0;
        this.tracks.add(new MediaExtractorTrack(mediaExtractorSampleQueue, false, null));
        String alternativeCodecMimeType = MediaCodecUtil.getAlternativeCodecMimeType(format);
        if (alternativeCodecMimeType != null) {
            mediaExtractorSampleQueue.setCompatibilityTrackIndex(this.tracks.size());
            this.tracks.add(new MediaExtractorTrack(mediaExtractorSampleQueue, z, alternativeCodecMimeType));
        }
    }

    private void maybeResolvePendingSeek() throws IOException {
        if (this.pendingSeek == null) {
            return;
        }
        SeekPoint pendingSeek = (SeekPoint) Assertions.checkNotNull(this.pendingSeek);
        ((Extractor) Assertions.checkNotNull(this.currentExtractor)).seek(pendingSeek.position, pendingSeek.timeUs);
        this.currentExtractorInput = reopenCurrentDataSource(pendingSeek.position);
        this.pendingSeek = null;
    }

    private static DataSpec buildDataSpec(Uri uri, long position) {
        return new DataSpec.Builder().setUri(uri).setPosition(position).setFlags(6).build();
    }

    private final class ExtractorOutputImpl implements ExtractorOutput {
        private ExtractorOutputImpl() {
        }

        @Override // androidx.media3.extractor.ExtractorOutput
        public TrackOutput track(int id, int type) {
            MediaExtractorSampleQueue sampleQueue = (MediaExtractorSampleQueue) MediaExtractorCompat.this.sampleQueues.get(id);
            if (sampleQueue == null) {
                if (MediaExtractorCompat.this.tracksEnded) {
                    return new DiscardingTrackOutput();
                }
                MediaExtractorSampleQueue sampleQueue2 = MediaExtractorCompat.this.new MediaExtractorSampleQueue(MediaExtractorCompat.this.allocator, id);
                MediaExtractorCompat.this.sampleQueues.put(id, sampleQueue2);
                return sampleQueue2;
            }
            return sampleQueue;
        }

        @Override // androidx.media3.extractor.ExtractorOutput
        public void endTracks() {
            MediaExtractorCompat.this.tracksEnded = true;
        }

        @Override // androidx.media3.extractor.ExtractorOutput
        public void seekMap(SeekMap seekMap) {
            MediaExtractorCompat.this.seekMap = seekMap;
        }
    }

    private static final class MediaExtractorTrack {
        public final String compatibilityTrackMimeType;
        public final boolean isCompatibilityTrack;
        public final MediaExtractorSampleQueue sampleQueue;

        private MediaExtractorTrack(MediaExtractorSampleQueue sampleQueue, boolean isCompatibilityTrack, String compatibilityTrackMimeType) {
            this.sampleQueue = sampleQueue;
            this.isCompatibilityTrack = isCompatibilityTrack;
            this.compatibilityTrackMimeType = compatibilityTrackMimeType;
        }

        public MediaFormat createDownstreamMediaFormat(FormatHolder scratchFormatHolder, DecoderInputBuffer scratchNoDataDecoderInputBuffer) {
            scratchFormatHolder.clear();
            this.sampleQueue.read(scratchFormatHolder, scratchNoDataDecoderInputBuffer, 2, false);
            Format result = (Format) Assertions.checkNotNull(scratchFormatHolder.format);
            MediaFormat mediaFormatResult = MediaFormatUtil.createMediaFormatFromFormat(result);
            scratchFormatHolder.clear();
            if (this.compatibilityTrackMimeType != null) {
                if (Util.SDK_INT >= 29) {
                    mediaFormatResult.removeKey("codecs-string");
                }
                mediaFormatResult.setString("mime", this.compatibilityTrackMimeType);
            }
            return mediaFormatResult;
        }

        public void discardFrontSample() {
            this.sampleQueue.skip(1);
            this.sampleQueue.discardToRead();
        }

        public int getIdOfBackingTrack() {
            return this.sampleQueue.trackId;
        }

        public String toString() {
            return String.format("MediaExtractorSampleQueue: %s, isCompatibilityTrack: %s, compatibilityTrackMimeType: %s", this.sampleQueue, Boolean.valueOf(this.isCompatibilityTrack), this.compatibilityTrackMimeType);
        }
    }

    private final class MediaExtractorSampleQueue extends SampleQueue {
        private int compatibilityTrackIndex;
        private int mainTrackIndex;
        public final int trackId;

        public MediaExtractorSampleQueue(Allocator allocator, int trackId) {
            super(allocator, null, null);
            this.trackId = trackId;
            this.mainTrackIndex = -1;
            this.compatibilityTrackIndex = -1;
        }

        public void setMainTrackIndex(int mainTrackIndex) {
            this.mainTrackIndex = mainTrackIndex;
        }

        public void setCompatibilityTrackIndex(int compatibilityTrackIndex) {
            this.compatibilityTrackIndex = compatibilityTrackIndex;
        }

        @Override // androidx.media3.exoplayer.source.SampleQueue
        public Format getAdjustedUpstreamFormat(Format format) {
            if (getUpstreamFormat() == null) {
                MediaExtractorCompat.this.onSampleQueueFormatInitialized(this, format);
            }
            return super.getAdjustedUpstreamFormat(format);
        }

        @Override // androidx.media3.exoplayer.source.SampleQueue, androidx.media3.extractor.TrackOutput
        public void sampleMetadata(long timeUs, int flags, int size, int offset, TrackOutput.CryptoData cryptoData) {
            int flags2 = flags & (-536870913);
            int flags3 = this.compatibilityTrackIndex;
            if (flags3 != -1) {
                MediaExtractorCompat.this.trackIndicesPerSampleInQueuedOrder.addLast(Integer.valueOf(this.compatibilityTrackIndex));
            }
            Assertions.checkState(this.mainTrackIndex != -1);
            MediaExtractorCompat.this.trackIndicesPerSampleInQueuedOrder.addLast(Integer.valueOf(this.mainTrackIndex));
            super.sampleMetadata(timeUs, flags2, size, offset, cryptoData);
        }

        public String toString() {
            return String.format("trackId: %s, mainTrackIndex: %s, compatibilityTrackIndex: %s", Integer.valueOf(this.trackId), Integer.valueOf(this.mainTrackIndex), Integer.valueOf(this.compatibilityTrackIndex));
        }
    }
}
