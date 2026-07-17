package androidx.media3.exoplayer.source;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.TransferListener;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class SilenceMediaSource extends BaseMediaSource {
    private static final int CHANNEL_COUNT = 2;
    private static final int PCM_ENCODING = 2;
    private final long durationUs;
    private MediaItem mediaItem;
    private static final int SAMPLE_RATE_HZ = 44100;
    private static final Format FORMAT = new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_RAW).setChannelCount(2).setSampleRate(SAMPLE_RATE_HZ).setPcmEncoding(2).build();
    public static final String MEDIA_ID = "SilenceMediaSource";
    private static final MediaItem MEDIA_ITEM = new MediaItem.Builder().setMediaId(MEDIA_ID).setUri(Uri.EMPTY).setMimeType(FORMAT.sampleMimeType).build();
    private static final byte[] SILENCE_SAMPLE = new byte[Util.getPcmFrameSize(2, 2) * 1024];

    public static final class Factory {
        private long durationUs;
        private Object tag;

        public Factory setDurationUs(long durationUs) {
            this.durationUs = durationUs;
            return this;
        }

        public Factory setTag(Object tag) {
            this.tag = tag;
            return this;
        }

        public SilenceMediaSource createMediaSource() {
            Assertions.checkState(this.durationUs > 0);
            return new SilenceMediaSource(this.durationUs, SilenceMediaSource.MEDIA_ITEM.buildUpon().setTag(this.tag).build());
        }
    }

    public SilenceMediaSource(long durationUs) {
        this(durationUs, MEDIA_ITEM);
    }

    private SilenceMediaSource(long durationUs, MediaItem mediaItem) {
        Assertions.checkArgument(durationUs >= 0);
        this.durationUs = durationUs;
        this.mediaItem = mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        refreshSourceInfo(new SinglePeriodTimeline(this.durationUs, true, false, false, (Object) null, getMediaItem()));
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() {
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        return new SilenceMediaPeriod(this.durationUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public synchronized MediaItem getMediaItem() {
        return this.mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        return true;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public synchronized void updateMediaItem(MediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
    }

    private static final class SilenceMediaPeriod implements MediaPeriod {
        private static final TrackGroupArray TRACKS = new TrackGroupArray(new TrackGroup(SilenceMediaSource.FORMAT));
        private final long durationUs;
        private final ArrayList<SampleStream> sampleStreams = new ArrayList<>();

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public /* synthetic */ List getStreamKeys(List list) {
            return Collections.emptyList();
        }

        public SilenceMediaPeriod(long durationUs) {
            this.durationUs = durationUs;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public void prepare(MediaPeriod.Callback callback, long positionUs) {
            callback.onPrepared(this);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public void maybeThrowPrepareError() {
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public TrackGroupArray getTrackGroups() {
            return TRACKS;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
            long positionUs2 = constrainSeekPosition(positionUs);
            for (int i = 0; i < selections.length; i++) {
                if (streams[i] != null && (selections[i] == null || !mayRetainStreamFlags[i])) {
                    this.sampleStreams.remove(streams[i]);
                    streams[i] = null;
                }
                if (streams[i] == null && selections[i] != null) {
                    SilenceSampleStream stream = new SilenceSampleStream(this.durationUs);
                    stream.seekTo(positionUs2);
                    this.sampleStreams.add(stream);
                    streams[i] = stream;
                    streamResetFlags[i] = true;
                }
            }
            return positionUs2;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public void discardBuffer(long positionUs, boolean toKeyframe) {
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long readDiscontinuity() {
            return C.TIME_UNSET;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long seekToUs(long positionUs) {
            long positionUs2 = constrainSeekPosition(positionUs);
            for (int i = 0; i < this.sampleStreams.size(); i++) {
                ((SilenceSampleStream) this.sampleStreams.get(i)).seekTo(positionUs2);
            }
            return positionUs2;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
            return constrainSeekPosition(positionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public long getBufferedPositionUs() {
            return Long.MIN_VALUE;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public long getNextLoadPositionUs() {
            return Long.MIN_VALUE;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public boolean continueLoading(LoadingInfo loadingInfo) {
            return false;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public boolean isLoading() {
            return false;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public void reevaluateBuffer(long positionUs) {
        }

        private long constrainSeekPosition(long positionUs) {
            return Util.constrainValue(positionUs, 0L, this.durationUs);
        }
    }

    private static final class SilenceSampleStream implements SampleStream {
        private final long durationBytes;
        private long positionBytes;
        private boolean sentFormat;

        public SilenceSampleStream(long durationUs) {
            this.durationBytes = SilenceMediaSource.getAudioByteCount(durationUs);
            seekTo(0L);
        }

        public void seekTo(long positionUs) {
            this.positionBytes = Util.constrainValue(SilenceMediaSource.getAudioByteCount(positionUs), 0L, this.durationBytes);
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public boolean isReady() {
            return true;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public void maybeThrowError() {
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            if (!this.sentFormat || (readFlags & 2) != 0) {
                formatHolder.format = SilenceMediaSource.FORMAT;
                this.sentFormat = true;
                return -5;
            }
            long bytesRemaining = this.durationBytes - this.positionBytes;
            if (bytesRemaining != 0) {
                buffer.timeUs = SilenceMediaSource.getAudioPositionUs(this.positionBytes);
                buffer.addFlag(1);
                int bytesToWrite = (int) Math.min(SilenceMediaSource.SILENCE_SAMPLE.length, bytesRemaining);
                if ((readFlags & 4) == 0) {
                    buffer.ensureSpaceForWrite(bytesToWrite);
                    buffer.data.put(SilenceMediaSource.SILENCE_SAMPLE, 0, bytesToWrite);
                }
                if ((readFlags & 1) == 0) {
                    this.positionBytes += (long) bytesToWrite;
                }
                return -4;
            }
            buffer.addFlag(4);
            return -4;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int skipData(long positionUs) {
            long oldPositionBytes = this.positionBytes;
            seekTo(positionUs);
            return (int) ((this.positionBytes - oldPositionBytes) / ((long) SilenceMediaSource.SILENCE_SAMPLE.length));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long getAudioByteCount(long durationUs) {
        long audioSampleCount = (44100 * durationUs) / 1000000;
        return ((long) Util.getPcmFrameSize(2, 2)) * audioSampleCount;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long getAudioPositionUs(long bytes) {
        long audioSampleCount = bytes / ((long) Util.getPcmFrameSize(2, 2));
        return (1000000 * audioSampleCount) / 44100;
    }
}
