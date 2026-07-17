package androidx.media3.exoplayer.hls;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.UriUtil;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceUtil;
import androidx.media3.datasource.DataSpec;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist;
import androidx.media3.exoplayer.source.chunk.MediaChunk;
import androidx.media3.exoplayer.upstream.CmcdData;
import androidx.media3.extractor.DefaultExtractorInput;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.metadata.id3.Id3Decoder;
import androidx.media3.extractor.metadata.id3.PrivFrame;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
final class HlsMediaChunk extends MediaChunk {
    public static final String PRIV_TIMESTAMP_FRAME_OWNER = "com.apple.streaming.transportStreamTimestamp";
    private static final AtomicInteger uidSource = new AtomicInteger();
    public final int discontinuitySequenceNumber;
    private final DrmInitData drmInitData;
    private HlsMediaChunkExtractor extractor;
    private final HlsExtractorFactory extractorFactory;
    private boolean extractorInvalidated;
    private final boolean hasGapTag;
    private final Id3Decoder id3Decoder;
    private boolean initDataLoadRequired;
    private final DataSource initDataSource;
    private final DataSpec initDataSpec;
    private final boolean initSegmentEncrypted;
    private final boolean isPrimaryTimestampSource;
    private boolean isPublished;
    private volatile boolean loadCanceled;
    private boolean loadCompleted;
    private final boolean mediaSegmentEncrypted;
    private final List<Format> muxedCaptionFormats;
    private int nextLoadPosition;
    private HlsSampleStreamWrapper output;
    public final int partIndex;
    private final PlayerId playerId;
    public final Uri playlistUrl;
    private final HlsMediaChunkExtractor previousExtractor;
    private ImmutableList<Integer> sampleQueueFirstSampleIndices;
    private final ParsableByteArray scratchId3Data;
    public final boolean shouldSpliceIn;
    private final TimestampAdjuster timestampAdjuster;
    private final long timestampAdjusterInitializationTimeoutMs;
    public final int uid;

    public static HlsMediaChunk createInstance(HlsExtractorFactory extractorFactory, DataSource dataSource, Format format, long startOfPlaylistInPeriodUs, HlsMediaPlaylist mediaPlaylist, HlsChunkSource.SegmentBaseHolder segmentBaseHolder, Uri playlistUrl, List<Format> muxedCaptionFormats, int trackSelectionReason, Object trackSelectionData, boolean isPrimaryTimestampSource, TimestampAdjusterProvider timestampAdjusterProvider, long timestampAdjusterInitializationTimeoutMs, HlsMediaChunk previousChunk, byte[] mediaSegmentKey, byte[] initSegmentKey, boolean shouldSpliceIn, PlayerId playerId, CmcdData.Factory cmcdDataFactory) {
        DataSpec dataSpec;
        byte[] mediaSegmentIv;
        boolean z;
        Uri uri;
        Id3Decoder id3Decoder;
        HlsMediaChunkExtractor previousExtractor;
        ParsableByteArray scratchId3Data;
        HlsMediaChunkExtractor hlsMediaChunkExtractor;
        byte[] initSegmentIv;
        HlsMediaPlaylist.SegmentBase mediaSegment = segmentBaseHolder.segmentBase;
        DataSpec dataSpec2 = new DataSpec.Builder().setUri(UriUtil.resolveToUri(mediaPlaylist.baseUri, mediaSegment.url)).setPosition(mediaSegment.byteRangeOffset).setLength(mediaSegment.byteRangeLength).setFlags(segmentBaseHolder.isPreload ? 8 : 0).build();
        if (cmcdDataFactory == null) {
            dataSpec = dataSpec2;
        } else {
            CmcdData cmcdData = cmcdDataFactory.setChunkDurationUs(mediaSegment.durationUs).createCmcdData();
            dataSpec = cmcdData.addToDataSpec(dataSpec2);
        }
        boolean mediaSegmentEncrypted = mediaSegmentKey != null;
        if (mediaSegmentEncrypted) {
            mediaSegmentIv = getEncryptionIvArray((String) Assertions.checkNotNull(mediaSegment.encryptionIV));
        } else {
            mediaSegmentIv = null;
        }
        DataSource mediaDataSource = buildDataSource(dataSource, mediaSegmentKey, mediaSegmentIv);
        HlsMediaPlaylist.Segment initSegment = mediaSegment.initializationSegment;
        DataSpec initDataSpec = null;
        boolean initSegmentEncrypted = false;
        DataSource initDataSource = null;
        if (initSegment != null) {
            initSegmentEncrypted = initSegmentKey != null;
            if (initSegmentEncrypted) {
                z = true;
                initSegmentIv = getEncryptionIvArray((String) Assertions.checkNotNull(initSegment.encryptionIV));
            } else {
                z = true;
                initSegmentIv = null;
            }
            Uri initSegmentUri = UriUtil.resolveToUri(mediaPlaylist.baseUri, initSegment.url);
            DataSpec initDataSpec2 = new DataSpec.Builder().setUri(initSegmentUri).setPosition(initSegment.byteRangeOffset).setLength(initSegment.byteRangeLength).build();
            if (cmcdDataFactory == null) {
                initDataSpec = initDataSpec2;
            } else {
                CmcdData cmcdData2 = cmcdDataFactory.setObjectType(CmcdData.Factory.OBJECT_TYPE_INIT_SEGMENT).createCmcdData();
                initDataSpec = cmcdData2.addToDataSpec(initDataSpec2);
            }
            initDataSource = buildDataSource(dataSource, initSegmentKey, initSegmentIv);
        } else {
            z = true;
        }
        long segmentStartTimeInPeriodUs = startOfPlaylistInPeriodUs + mediaSegment.relativeStartTimeUs;
        long segmentEndTimeInPeriodUs = segmentStartTimeInPeriodUs + mediaSegment.durationUs;
        int discontinuitySequenceNumber = mediaPlaylist.discontinuitySequence + mediaSegment.relativeDiscontinuitySequence;
        if (previousChunk != null) {
            boolean isSameInitData = (initDataSpec == previousChunk.initDataSpec || (initDataSpec != null && previousChunk.initDataSpec != null && initDataSpec.uri.equals(previousChunk.initDataSpec.uri) && initDataSpec.position == previousChunk.initDataSpec.position)) ? z : false;
            uri = playlistUrl;
            boolean isFollowingChunk = (uri.equals(previousChunk.playlistUrl) && previousChunk.loadCompleted) ? z : false;
            Id3Decoder id3Decoder2 = previousChunk.id3Decoder;
            ParsableByteArray scratchId3Data2 = previousChunk.scratchId3Data;
            if (isSameInitData && isFollowingChunk && !previousChunk.extractorInvalidated && previousChunk.discontinuitySequenceNumber == discontinuitySequenceNumber) {
                hlsMediaChunkExtractor = previousChunk.extractor;
            } else {
                hlsMediaChunkExtractor = null;
            }
            HlsMediaChunkExtractor previousExtractor2 = hlsMediaChunkExtractor;
            id3Decoder = id3Decoder2;
            previousExtractor = previousExtractor2;
            scratchId3Data = scratchId3Data2;
        } else {
            uri = playlistUrl;
            Id3Decoder id3Decoder3 = new Id3Decoder();
            id3Decoder = id3Decoder3;
            previousExtractor = null;
            scratchId3Data = new ParsableByteArray(10);
        }
        return new HlsMediaChunk(extractorFactory, mediaDataSource, dataSpec, format, mediaSegmentEncrypted, initDataSource, initDataSpec, initSegmentEncrypted, uri, muxedCaptionFormats, trackSelectionReason, trackSelectionData, segmentStartTimeInPeriodUs, segmentEndTimeInPeriodUs, segmentBaseHolder.mediaSequence, segmentBaseHolder.partIndex, !segmentBaseHolder.isPreload, discontinuitySequenceNumber, mediaSegment.hasGapTag, isPrimaryTimestampSource, timestampAdjusterProvider.getAdjuster(discontinuitySequenceNumber), timestampAdjusterInitializationTimeoutMs, mediaSegment.drmInitData, previousExtractor, id3Decoder, scratchId3Data, shouldSpliceIn, playerId);
    }

    public static boolean shouldSpliceIn(HlsMediaChunk previousChunk, Uri playlistUrl, HlsMediaPlaylist mediaPlaylist, HlsChunkSource.SegmentBaseHolder segmentBaseHolder, long startOfPlaylistInPeriodUs) {
        if (previousChunk == null) {
            return false;
        }
        if (playlistUrl.equals(previousChunk.playlistUrl) && previousChunk.loadCompleted) {
            return false;
        }
        long segmentStartTimeInPeriodUs = segmentBaseHolder.segmentBase.relativeStartTimeUs + startOfPlaylistInPeriodUs;
        return !isIndependent(segmentBaseHolder, mediaPlaylist) || segmentStartTimeInPeriodUs < previousChunk.endTimeUs;
    }

    private HlsMediaChunk(HlsExtractorFactory extractorFactory, DataSource mediaDataSource, DataSpec dataSpec, Format format, boolean mediaSegmentEncrypted, DataSource initDataSource, DataSpec initDataSpec, boolean initSegmentEncrypted, Uri playlistUrl, List<Format> muxedCaptionFormats, int trackSelectionReason, Object trackSelectionData, long startTimeUs, long endTimeUs, long chunkMediaSequence, int partIndex, boolean isPublished, int discontinuitySequenceNumber, boolean hasGapTag, boolean isPrimaryTimestampSource, TimestampAdjuster timestampAdjuster, long timestampAdjusterInitializationTimeoutMs, DrmInitData drmInitData, HlsMediaChunkExtractor previousExtractor, Id3Decoder id3Decoder, ParsableByteArray scratchId3Data, boolean shouldSpliceIn, PlayerId playerId) {
        super(mediaDataSource, dataSpec, format, trackSelectionReason, trackSelectionData, startTimeUs, endTimeUs, chunkMediaSequence);
        this.mediaSegmentEncrypted = mediaSegmentEncrypted;
        this.partIndex = partIndex;
        this.isPublished = isPublished;
        this.discontinuitySequenceNumber = discontinuitySequenceNumber;
        this.initDataSpec = initDataSpec;
        this.initDataSource = initDataSource;
        this.initDataLoadRequired = initDataSpec != null;
        this.initSegmentEncrypted = initSegmentEncrypted;
        this.playlistUrl = playlistUrl;
        this.isPrimaryTimestampSource = isPrimaryTimestampSource;
        this.timestampAdjuster = timestampAdjuster;
        this.timestampAdjusterInitializationTimeoutMs = timestampAdjusterInitializationTimeoutMs;
        this.hasGapTag = hasGapTag;
        this.extractorFactory = extractorFactory;
        this.muxedCaptionFormats = muxedCaptionFormats;
        this.drmInitData = drmInitData;
        this.previousExtractor = previousExtractor;
        this.id3Decoder = id3Decoder;
        this.scratchId3Data = scratchId3Data;
        this.shouldSpliceIn = shouldSpliceIn;
        this.playerId = playerId;
        this.sampleQueueFirstSampleIndices = ImmutableList.of();
        this.uid = uidSource.getAndIncrement();
    }

    public void init(HlsSampleStreamWrapper output, ImmutableList<Integer> sampleQueueWriteIndices) {
        this.output = output;
        this.sampleQueueFirstSampleIndices = sampleQueueWriteIndices;
    }

    public int getFirstSampleIndex(int sampleQueueIndex) {
        Assertions.checkState(!this.shouldSpliceIn);
        if (sampleQueueIndex >= this.sampleQueueFirstSampleIndices.size()) {
            return 0;
        }
        return this.sampleQueueFirstSampleIndices.get(sampleQueueIndex).intValue();
    }

    public void invalidateExtractor() {
        this.extractorInvalidated = true;
    }

    @Override // androidx.media3.exoplayer.source.chunk.MediaChunk
    public boolean isLoadCompleted() {
        return this.loadCompleted;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public void cancelLoad() {
        this.loadCanceled = true;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public void load() throws IOException {
        Assertions.checkNotNull(this.output);
        if (this.extractor == null && this.previousExtractor != null && this.previousExtractor.isReusable()) {
            this.extractor = this.previousExtractor;
            this.initDataLoadRequired = false;
        }
        maybeLoadInitData();
        if (!this.loadCanceled) {
            if (!this.hasGapTag) {
                loadMedia();
            }
            this.loadCompleted = !this.loadCanceled;
        }
    }

    public boolean isPublished() {
        return this.isPublished;
    }

    public void publish() {
        this.isPublished = true;
    }

    @RequiresNonNull({"output"})
    private void maybeLoadInitData() throws IOException {
        if (!this.initDataLoadRequired) {
            return;
        }
        Assertions.checkNotNull(this.initDataSource);
        Assertions.checkNotNull(this.initDataSpec);
        feedDataToExtractor(this.initDataSource, this.initDataSpec, this.initSegmentEncrypted, false);
        this.nextLoadPosition = 0;
        this.initDataLoadRequired = false;
    }

    @RequiresNonNull({"output"})
    private void loadMedia() throws IOException {
        feedDataToExtractor(this.dataSource, this.dataSpec, this.mediaSegmentEncrypted, true);
    }

    @RequiresNonNull({"output"})
    private void feedDataToExtractor(DataSource dataSource, DataSpec dataSpec, boolean dataIsEncrypted, boolean initializeTimestampAdjuster) throws IOException {
        DataSpec loadDataSpec;
        boolean skipLoadedBytes;
        long position;
        long j;
        int i = this.nextLoadPosition;
        if (dataIsEncrypted) {
            loadDataSpec = dataSpec;
            skipLoadedBytes = i != 0;
        } else {
            loadDataSpec = dataSpec.subrange(i);
            skipLoadedBytes = false;
        }
        try {
            ExtractorInput input = prepareExtraction(dataSource, loadDataSpec, initializeTimestampAdjuster);
            if (skipLoadedBytes) {
                input.skipFully(this.nextLoadPosition);
            }
            do {
                try {
                    try {
                        if (this.loadCanceled) {
                            break;
                        }
                    } catch (EOFException e) {
                        if ((this.trackFormat.roleFlags & 16384) == 0) {
                            throw e;
                        }
                        this.extractor.onTruncatedSegmentParsed();
                        position = input.getPosition();
                        j = dataSpec.position;
                    }
                } catch (Throwable th) {
                    this.nextLoadPosition = (int) (input.getPosition() - dataSpec.position);
                    throw th;
                }
            } while (this.extractor.read(input));
            position = input.getPosition();
            j = dataSpec.position;
            this.nextLoadPosition = (int) (position - j);
            DataSourceUtil.closeQuietly(dataSource);
        } catch (Throwable th2) {
            DataSourceUtil.closeQuietly(dataSource);
            throw th2;
        }
    }

    @EnsuresNonNull({"extractor"})
    @RequiresNonNull({"output"})
    private DefaultExtractorInput prepareExtraction(DataSource dataSource, DataSpec dataSpec, boolean initializeTimestampAdjuster) throws Throwable {
        HlsMediaChunkExtractor hlsMediaChunkExtractorCreateExtractor;
        long jAdjustTsTimestamp;
        long bytesToRead = dataSource.open(dataSpec);
        if (initializeTimestampAdjuster) {
            try {
                this.timestampAdjuster.sharedInitializeOrWait(this.isPrimaryTimestampSource, this.startTimeUs, this.timestampAdjusterInitializationTimeoutMs);
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            } catch (TimeoutException e2) {
                throw new IOException(e2);
            }
        }
        DefaultExtractorInput extractorInput = new DefaultExtractorInput(dataSource, dataSpec.position, bytesToRead);
        if (this.extractor == null) {
            long id3Timestamp = peekId3PrivTimestamp(extractorInput);
            extractorInput.resetPeekPosition();
            if (this.previousExtractor != null) {
                hlsMediaChunkExtractorCreateExtractor = this.previousExtractor.recreate();
            } else {
                hlsMediaChunkExtractorCreateExtractor = this.extractorFactory.createExtractor(dataSpec.uri, this.trackFormat, this.muxedCaptionFormats, this.timestampAdjuster, dataSource.getResponseHeaders(), extractorInput, this.playerId);
            }
            this.extractor = hlsMediaChunkExtractorCreateExtractor;
            boolean zIsPackedAudioExtractor = this.extractor.isPackedAudioExtractor();
            HlsSampleStreamWrapper hlsSampleStreamWrapper = this.output;
            if (zIsPackedAudioExtractor) {
                if (id3Timestamp != C.TIME_UNSET) {
                    jAdjustTsTimestamp = this.timestampAdjuster.adjustTsTimestamp(id3Timestamp);
                } else {
                    jAdjustTsTimestamp = this.startTimeUs;
                }
                hlsSampleStreamWrapper.setSampleOffsetUs(jAdjustTsTimestamp);
            } else {
                hlsSampleStreamWrapper.setSampleOffsetUs(0L);
            }
            this.output.onNewExtractor();
            this.extractor.init(this.output);
        }
        this.output.setDrmInitData(this.drmInitData);
        return extractorInput;
    }

    private long peekId3PrivTimestamp(ExtractorInput input) throws Throwable {
        input.resetPeekPosition();
        try {
            this.scratchId3Data.reset(10);
            input.peekFully(this.scratchId3Data.getData(), 0, 10);
            int id = this.scratchId3Data.readUnsignedInt24();
            if (id != 4801587) {
                return C.TIME_UNSET;
            }
            this.scratchId3Data.skipBytes(3);
            int id3Size = this.scratchId3Data.readSynchSafeInt();
            int requiredCapacity = id3Size + 10;
            if (requiredCapacity > this.scratchId3Data.capacity()) {
                byte[] data = this.scratchId3Data.getData();
                this.scratchId3Data.reset(requiredCapacity);
                System.arraycopy(data, 0, this.scratchId3Data.getData(), 0, 10);
            }
            input.peekFully(this.scratchId3Data.getData(), 10, id3Size);
            Metadata metadata = this.id3Decoder.decode(this.scratchId3Data.getData(), id3Size);
            if (metadata == null) {
                return C.TIME_UNSET;
            }
            int metadataLength = metadata.length();
            for (int i = 0; i < metadataLength; i++) {
                Metadata.Entry frame = metadata.get(i);
                if (frame instanceof PrivFrame) {
                    PrivFrame privFrame = (PrivFrame) frame;
                    if (PRIV_TIMESTAMP_FRAME_OWNER.equals(privFrame.owner)) {
                        System.arraycopy(privFrame.privateData, 0, this.scratchId3Data.getData(), 0, 8);
                        this.scratchId3Data.setPosition(0);
                        this.scratchId3Data.setLimit(8);
                        return this.scratchId3Data.readLong() & 8589934591L;
                    }
                }
            }
            return C.TIME_UNSET;
        } catch (EOFException e) {
            return C.TIME_UNSET;
        }
    }

    private static byte[] getEncryptionIvArray(String ivString) {
        String trimmedIv;
        if (Ascii.toLowerCase(ivString).startsWith("0x")) {
            trimmedIv = ivString.substring(2);
        } else {
            trimmedIv = ivString;
        }
        byte[] ivData = new BigInteger(trimmedIv, 16).toByteArray();
        byte[] ivDataWithPadding = new byte[16];
        int offset = ivData.length > 16 ? ivData.length - 16 : 0;
        System.arraycopy(ivData, offset, ivDataWithPadding, (ivDataWithPadding.length - ivData.length) + offset, ivData.length - offset);
        return ivDataWithPadding;
    }

    private static DataSource buildDataSource(DataSource dataSource, byte[] fullSegmentEncryptionKey, byte[] encryptionIv) {
        if (fullSegmentEncryptionKey != null) {
            Assertions.checkNotNull(encryptionIv);
            return new Aes128DataSource(dataSource, fullSegmentEncryptionKey, encryptionIv);
        }
        return dataSource;
    }

    private static boolean isIndependent(HlsChunkSource.SegmentBaseHolder segmentBaseHolder, HlsMediaPlaylist mediaPlaylist) {
        if (segmentBaseHolder.segmentBase instanceof HlsMediaPlaylist.Part) {
            return ((HlsMediaPlaylist.Part) segmentBaseHolder.segmentBase).isIndependent || (segmentBaseHolder.partIndex == 0 && mediaPlaylist.hasIndependentSegments);
        }
        return mediaPlaylist.hasIndependentSegments;
    }
}
