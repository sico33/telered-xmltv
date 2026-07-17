package androidx.media3.exoplayer.audio;

import android.os.Handler;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DecoderReuseEvaluation;

/* JADX INFO: loaded from: classes.dex */
public interface AudioRendererEventListener {
    void onAudioCodecError(Exception exc);

    void onAudioDecoderInitialized(String str, long j, long j2);

    void onAudioDecoderReleased(String str);

    void onAudioDisabled(DecoderCounters decoderCounters);

    void onAudioEnabled(DecoderCounters decoderCounters);

    void onAudioInputFormatChanged(Format format, DecoderReuseEvaluation decoderReuseEvaluation);

    void onAudioPositionAdvancing(long j);

    void onAudioSinkError(Exception exc);

    void onAudioTrackInitialized(AudioSink.AudioTrackConfig audioTrackConfig);

    void onAudioTrackReleased(AudioSink.AudioTrackConfig audioTrackConfig);

    void onAudioUnderrun(int i, long j, long j2);

    void onSkipSilenceEnabledChanged(boolean z);

    /* JADX INFO: renamed from: androidx.media3.exoplayer.audio.AudioRendererEventListener$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static void $default$onAudioEnabled(AudioRendererEventListener _this, DecoderCounters counters) {
        }

        public static void $default$onAudioDecoderInitialized(AudioRendererEventListener _this, String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        }

        public static void $default$onAudioInputFormatChanged(AudioRendererEventListener _this, Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        }

        public static void $default$onAudioPositionAdvancing(AudioRendererEventListener _this, long playoutStartSystemTimeMs) {
        }

        public static void $default$onAudioUnderrun(AudioRendererEventListener _this, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
        }

        public static void $default$onAudioDecoderReleased(AudioRendererEventListener _this, String decoderName) {
        }

        public static void $default$onAudioDisabled(AudioRendererEventListener _this, DecoderCounters counters) {
        }

        public static void $default$onSkipSilenceEnabledChanged(AudioRendererEventListener _this, boolean skipSilenceEnabled) {
        }

        public static void $default$onAudioCodecError(AudioRendererEventListener _this, Exception audioCodecError) {
        }

        public static void $default$onAudioSinkError(AudioRendererEventListener _this, Exception audioSinkError) {
        }

        public static void $default$onAudioTrackInitialized(AudioRendererEventListener _this, AudioSink.AudioTrackConfig audioTrackConfig) {
        }

        public static void $default$onAudioTrackReleased(AudioRendererEventListener _this, AudioSink.AudioTrackConfig audioTrackConfig) {
        }
    }

    public static final class EventDispatcher {
        private final Handler handler;
        private final AudioRendererEventListener listener;

        public EventDispatcher(Handler handler, AudioRendererEventListener listener) {
            this.handler = listener != null ? (Handler) Assertions.checkNotNull(handler) : null;
            this.listener = listener;
        }

        public void enabled(final DecoderCounters decoderCounters) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m71x55ee20a7(decoderCounters);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$enabled$0$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m71x55ee20a7(DecoderCounters decoderCounters) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioEnabled(decoderCounters);
        }

        public void decoderInitialized(final String decoderName, final long initializedTimestampMs, final long initializationDurationMs) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda8
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m68x4e7a341d(decoderName, initializedTimestampMs, initializationDurationMs);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$decoderInitialized$1$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m68x4e7a341d(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
        }

        public void inputFormatChanged(final Format format, final DecoderReuseEvaluation decoderReuseEvaluation) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m72x1ee33b39(format, decoderReuseEvaluation);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$inputFormatChanged$2$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m72x1ee33b39(Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioInputFormatChanged(format, decoderReuseEvaluation);
        }

        public void positionAdvancing(final long playoutStartSystemTimeMs) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda11
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m73xa4e1944f(playoutStartSystemTimeMs);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$positionAdvancing$3$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m73xa4e1944f(long playoutStartSystemTimeMs) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioPositionAdvancing(playoutStartSystemTimeMs);
        }

        public void underrun(final int bufferSize, final long bufferSizeMs, final long elapsedSinceLastFeedMs) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m75x8e019017(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$underrun$4$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m75x8e019017(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }

        public void decoderReleased(final String decoderName) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda9
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m69x102cf822(decoderName);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$decoderReleased$5$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m69x102cf822(String decoderName) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioDecoderReleased(decoderName);
        }

        public void disabled(final DecoderCounters counters) {
            counters.ensureUpdated();
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda10
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m70xe116cfac(counters);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$disabled$6$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m70xe116cfac(DecoderCounters counters) {
            counters.ensureUpdated();
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioDisabled(counters);
        }

        public void skipSilenceEnabledChanged(final boolean skipSilenceEnabled) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda7
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m74x60b531cc(skipSilenceEnabled);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$skipSilenceEnabledChanged$7$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m74x60b531cc(boolean skipSilenceEnabled) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onSkipSilenceEnabledChanged(skipSilenceEnabled);
        }

        public void audioSinkError(final Exception audioSinkError) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m65xc89a3787(audioSinkError);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$audioSinkError$8$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m65xc89a3787(Exception audioSinkError) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioSinkError(audioSinkError);
        }

        public void audioCodecError(final Exception audioCodecError) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m64xdf751697(audioCodecError);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$audioCodecError$9$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m64xdf751697(Exception audioCodecError) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioCodecError(audioCodecError);
        }

        public void audioTrackInitialized(final AudioSink.AudioTrackConfig audioTrackConfig) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m66xee74b056(audioTrackConfig);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$audioTrackInitialized$10$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m66xee74b056(AudioSink.AudioTrackConfig audioTrackConfig) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioTrackInitialized(audioTrackConfig);
        }

        public void audioTrackReleased(final AudioSink.AudioTrackConfig audioTrackConfig) {
            if (this.handler != null) {
                this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.AudioRendererEventListener$EventDispatcher$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m67x12b02702(audioTrackConfig);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$audioTrackReleased$11$androidx-media3-exoplayer-audio-AudioRendererEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m67x12b02702(AudioSink.AudioTrackConfig audioTrackConfig) {
            ((AudioRendererEventListener) Util.castNonNull(this.listener)).onAudioTrackReleased(audioTrackConfig);
        }
    }
}
