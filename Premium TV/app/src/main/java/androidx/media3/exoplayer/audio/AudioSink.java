package androidx.media3.exoplayer.audio;

import android.media.AudioDeviceInfo;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.AuxEffectInfo;
import androidx.media3.common.Format;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.Clock;
import androidx.media3.exoplayer.analytics.PlayerId;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public interface AudioSink {
    public static final long CURRENT_POSITION_NOT_SET = Long.MIN_VALUE;
    public static final int OFFLOAD_MODE_DISABLED = 0;
    public static final int OFFLOAD_MODE_ENABLED_GAPLESS_NOT_REQUIRED = 2;
    public static final int OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED = 1;
    public static final int SINK_FORMAT_SUPPORTED_DIRECTLY = 2;
    public static final int SINK_FORMAT_SUPPORTED_WITH_TRANSCODING = 1;
    public static final int SINK_FORMAT_UNSUPPORTED = 0;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface OffloadMode {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface SinkFormatSupport {
    }

    void configure(Format format, int i, int[] iArr) throws ConfigurationException;

    void disableTunneling();

    void enableTunnelingV21();

    void flush();

    AudioAttributes getAudioAttributes();

    long getCurrentPositionUs(boolean z);

    AudioOffloadSupport getFormatOffloadSupport(Format format);

    int getFormatSupport(Format format);

    PlaybackParameters getPlaybackParameters();

    boolean getSkipSilenceEnabled();

    boolean handleBuffer(ByteBuffer byteBuffer, long j, int i) throws WriteException, InitializationException;

    void handleDiscontinuity();

    boolean hasPendingData();

    boolean isEnded();

    void pause();

    void play();

    void playToEndOfStream() throws WriteException;

    void release();

    void reset();

    void setAudioAttributes(AudioAttributes audioAttributes);

    void setAudioSessionId(int i);

    void setAuxEffectInfo(AuxEffectInfo auxEffectInfo);

    void setClock(Clock clock);

    void setListener(Listener listener);

    void setOffloadDelayPadding(int i, int i2);

    void setOffloadMode(int i);

    void setOutputStreamOffsetUs(long j);

    void setPlaybackParameters(PlaybackParameters playbackParameters);

    void setPlayerId(PlayerId playerId);

    void setPreferredDevice(AudioDeviceInfo audioDeviceInfo);

    void setSkipSilenceEnabled(boolean z);

    void setVolume(float f);

    boolean supportsFormat(Format format);

    public interface Listener {
        void onAudioCapabilitiesChanged();

        void onAudioSinkError(Exception exc);

        void onAudioTrackInitialized(AudioTrackConfig audioTrackConfig);

        void onAudioTrackReleased(AudioTrackConfig audioTrackConfig);

        void onOffloadBufferEmptying();

        void onOffloadBufferFull();

        void onPositionAdvancing(long j);

        void onPositionDiscontinuity();

        void onSilenceSkipped();

        void onSkipSilenceEnabledChanged(boolean z);

        void onUnderrun(int i, long j, long j2);

        /* JADX INFO: renamed from: androidx.media3.exoplayer.audio.AudioSink$Listener$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static void $default$onPositionAdvancing(Listener _this, long playoutStartSystemTimeMs) {
            }

            public static void $default$onOffloadBufferEmptying(Listener _this) {
            }

            public static void $default$onOffloadBufferFull(Listener _this) {
            }

            public static void $default$onAudioSinkError(Listener _this, Exception audioSinkError) {
            }

            public static void $default$onAudioCapabilitiesChanged(Listener _this) {
            }

            public static void $default$onAudioTrackInitialized(Listener _this, AudioTrackConfig audioTrackConfig) {
            }

            public static void $default$onAudioTrackReleased(Listener _this, AudioTrackConfig audioTrackConfig) {
            }

            public static void $default$onSilenceSkipped(Listener _this) {
            }
        }
    }

    public static final class AudioTrackConfig {
        public final int bufferSize;
        public final int channelConfig;
        public final int encoding;
        public final boolean offload;
        public final int sampleRate;
        public final boolean tunneling;

        public AudioTrackConfig(int encoding, int sampleRate, int channelConfig, boolean tunneling, boolean offload, int bufferSize) {
            this.encoding = encoding;
            this.sampleRate = sampleRate;
            this.channelConfig = channelConfig;
            this.tunneling = tunneling;
            this.offload = offload;
            this.bufferSize = bufferSize;
        }
    }

    public static final class ConfigurationException extends Exception {
        public final Format format;

        public ConfigurationException(Throwable cause, Format format) {
            super(cause);
            this.format = format;
        }

        public ConfigurationException(String message, Format format) {
            super(message);
            this.format = format;
        }
    }

    public static final class InitializationException extends Exception {
        public final int audioTrackState;
        public final Format format;
        public final boolean isRecoverable;

        public InitializationException(int audioTrackState, int sampleRate, int channelConfig, int bufferSize, Format format, boolean isRecoverable, Exception audioTrackException) {
            super("AudioTrack init failed " + audioTrackState + " Config(" + sampleRate + ", " + channelConfig + ", " + bufferSize + ") " + format + (isRecoverable ? " (recoverable)" : ""), audioTrackException);
            this.audioTrackState = audioTrackState;
            this.isRecoverable = isRecoverable;
            this.format = format;
        }
    }

    public static final class WriteException extends Exception {
        public final int errorCode;
        public final Format format;
        public final boolean isRecoverable;

        public WriteException(int errorCode, Format format, boolean isRecoverable) {
            super("AudioTrack write failed: " + errorCode);
            this.isRecoverable = isRecoverable;
            this.errorCode = errorCode;
            this.format = format;
        }
    }

    public static final class UnexpectedDiscontinuityException extends Exception {
        public final long actualPresentationTimeUs;
        public final long expectedPresentationTimeUs;

        public UnexpectedDiscontinuityException(long actualPresentationTimeUs, long expectedPresentationTimeUs) {
            super("Unexpected audio track timestamp discontinuity: expected " + expectedPresentationTimeUs + ", got " + actualPresentationTimeUs);
            this.actualPresentationTimeUs = actualPresentationTimeUs;
            this.expectedPresentationTimeUs = expectedPresentationTimeUs;
        }
    }

    /* JADX INFO: renamed from: androidx.media3.exoplayer.audio.AudioSink$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static void $default$setPlayerId(AudioSink _this, PlayerId playerId) {
        }

        public static void $default$setClock(AudioSink _this, Clock clock) {
        }

        public static void $default$setPreferredDevice(AudioSink _this, AudioDeviceInfo audioDeviceInfo) {
        }

        public static void $default$setOutputStreamOffsetUs(AudioSink _this, long outputStreamOffsetUs) {
        }

        public static void $default$setOffloadMode(AudioSink _this, int offloadMode) {
        }

        public static void $default$setOffloadDelayPadding(AudioSink _this, int delayInFrames, int paddingInFrames) {
        }

        public static void $default$release(AudioSink _this) {
        }
    }
}
