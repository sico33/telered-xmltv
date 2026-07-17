package androidx.media3.exoplayer.video;

import android.os.Handler;
import android.os.SystemClock;
import androidx.media3.common.Format;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DecoderReuseEvaluation;

/* JADX INFO: loaded from: classes.dex */
public interface VideoRendererEventListener {
    void onDroppedFrames(int i, long j);

    void onRenderedFirstFrame(Object obj, long j);

    void onVideoCodecError(Exception exc);

    void onVideoDecoderInitialized(String str, long j, long j2);

    void onVideoDecoderReleased(String str);

    void onVideoDisabled(DecoderCounters decoderCounters);

    void onVideoEnabled(DecoderCounters decoderCounters);

    void onVideoFrameProcessingOffset(long j, int i);

    void onVideoInputFormatChanged(Format format, DecoderReuseEvaluation decoderReuseEvaluation);

    void onVideoSizeChanged(VideoSize videoSize);

    /* JADX INFO: renamed from: androidx.media3.exoplayer.video.VideoRendererEventListener$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static void $default$onVideoEnabled(VideoRendererEventListener _this, DecoderCounters counters) {
        }

        public static void $default$onVideoDecoderInitialized(VideoRendererEventListener _this, String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        }

        public static void $default$onVideoInputFormatChanged(VideoRendererEventListener _this, Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        }

        public static void $default$onDroppedFrames(VideoRendererEventListener _this, int count, long elapsedMs) {
        }

        public static void $default$onVideoFrameProcessingOffset(VideoRendererEventListener _this, long totalProcessingOffsetUs, int frameCount) {
        }

        public static void $default$onVideoSizeChanged(VideoRendererEventListener _this, VideoSize videoSize) {
        }

        public static void $default$onRenderedFirstFrame(VideoRendererEventListener _this, Object output, long renderTimeMs) {
        }

        public static void $default$onVideoDecoderReleased(VideoRendererEventListener _this, String decoderName) {
        }

        public static void $default$onVideoDisabled(VideoRendererEventListener _this, DecoderCounters counters) {
        }

        public static void $default$onVideoCodecError(VideoRendererEventListener _this, Exception videoCodecError) {
        }
    }

    public static final class EventDispatcher {
        private final Handler handler;
        private final VideoRendererEventListener listener;

        public EventDispatcher(Handler handler, VideoRendererEventListener listener) {
            this.handler = listener != null ? (Handler) Assertions.checkNotNull(handler) : null;
            this.listener = listener;
        }

        public void enabled(final DecoderCounters decoderCounters) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m147x7180d5d(decoderCounters);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$enabled$0$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m147x7180d5d(DecoderCounters decoderCounters) {
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onVideoEnabled(decoderCounters);
        }

        public void decoderInitialized(final String decoderName, final long initializedTimestampMs, final long initializationDurationMs) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m143xffa420d3(decoderName, initializedTimestampMs, initializationDurationMs);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$decoderInitialized$1$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m143xffa420d3(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onVideoDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
        }

        public void inputFormatChanged(final Format format, final DecoderReuseEvaluation decoderReuseEvaluation) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda7
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m148xd00d27ef(format, decoderReuseEvaluation);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$inputFormatChanged$2$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m148xd00d27ef(Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onVideoInputFormatChanged(format, decoderReuseEvaluation);
        }

        public void droppedFrames(final int droppedFrameCount, final long elapsedMs) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m146x9a4cf695(droppedFrameCount, elapsedMs);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$droppedFrames$3$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m146x9a4cf695(int droppedFrameCount, long elapsedMs) {
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onDroppedFrames(droppedFrameCount, elapsedMs);
        }

        public void reportVideoFrameProcessingOffset(final long totalProcessingOffsetUs, final int frameCount) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m150xc5ffb974(totalProcessingOffsetUs, frameCount);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$reportVideoFrameProcessingOffset$4$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m150xc5ffb974(long totalProcessingOffsetUs, int frameCount) {
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onVideoFrameProcessingOffset(totalProcessingOffsetUs, frameCount);
        }

        public void videoSizeChanged(final VideoSize videoSize) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m152xad971007(videoSize);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$videoSizeChanged$5$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m152xad971007(VideoSize videoSize) {
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onVideoSizeChanged(videoSize);
        }

        public void renderedFirstFrame(final Object output) {
            if (this.handler != null) {
                final long renderTimeMs = SystemClock.elapsedRealtime();
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m149xb1e96bac(output, renderTimeMs);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$renderedFirstFrame$6$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m149xb1e96bac(Object output, long renderTimeMs) {
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onRenderedFirstFrame(output, renderTimeMs);
        }

        public void decoderReleased(final String decoderName) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda9
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m144x45853f96(decoderName);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$decoderReleased$7$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m144x45853f96(String decoderName) {
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onVideoDecoderReleased(decoderName);
        }

        public void disabled(final DecoderCounters counters) {
            counters.ensureUpdated();
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda8
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m145x166f1720(counters);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$disabled$8$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m145x166f1720(DecoderCounters counters) {
            counters.ensureUpdated();
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onVideoDisabled(counters);
        }

        public void videoCodecError(final Exception videoCodecError) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.video.VideoRendererEventListener$EventDispatcher$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m151x90ab4908(videoCodecError);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$videoCodecError$9$androidx-media3-exoplayer-video-VideoRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m151x90ab4908(Exception videoCodecError) {
            ((VideoRendererEventListener) Util.castNonNull(this.listener)).onVideoCodecError(videoCodecError);
        }
    }
}
