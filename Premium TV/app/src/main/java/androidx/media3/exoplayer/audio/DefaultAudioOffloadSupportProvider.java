package androidx.media3.exoplayer.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultAudioOffloadSupportProvider implements DefaultAudioSink.AudioOffloadSupportProvider {
    private static final String OFFLOAD_VARIABLE_RATE_SUPPORTED_KEY = "offloadVariableRateSupported";
    private final Context context;
    private Boolean isOffloadVariableRateSupported;

    public DefaultAudioOffloadSupportProvider() {
        this(null);
    }

    public DefaultAudioOffloadSupportProvider(Context context) {
        this.context = context;
    }

    @Override // androidx.media3.exoplayer.audio.DefaultAudioSink.AudioOffloadSupportProvider
    public AudioOffloadSupport getAudioOffloadSupport(Format format, AudioAttributes audioAttributes) {
        Assertions.checkNotNull(format);
        Assertions.checkNotNull(audioAttributes);
        if (Util.SDK_INT < 29 || format.sampleRate == -1) {
            return AudioOffloadSupport.DEFAULT_UNSUPPORTED;
        }
        boolean isOffloadVariableRateSupported = isOffloadVariableRateSupported(this.context);
        int encoding = MimeTypes.getEncoding((String) Assertions.checkNotNull(format.sampleMimeType), format.codecs);
        if (encoding == 0 || Util.SDK_INT < Util.getApiLevelThatAudioFormatIntroducedAudioEncoding(encoding)) {
            return AudioOffloadSupport.DEFAULT_UNSUPPORTED;
        }
        int channelConfig = Util.getAudioTrackChannelConfig(format.channelCount);
        if (channelConfig == 0) {
            return AudioOffloadSupport.DEFAULT_UNSUPPORTED;
        }
        try {
            AudioFormat audioFormat = Util.getAudioFormat(format.sampleRate, channelConfig, encoding);
            if (Util.SDK_INT >= 31) {
                return Api31.getOffloadedPlaybackSupport(audioFormat, audioAttributes.getAudioAttributesV21().audioAttributes, isOffloadVariableRateSupported);
            }
            return Api29.getOffloadedPlaybackSupport(audioFormat, audioAttributes.getAudioAttributesV21().audioAttributes, isOffloadVariableRateSupported);
        } catch (IllegalArgumentException e) {
            return AudioOffloadSupport.DEFAULT_UNSUPPORTED;
        }
    }

    private boolean isOffloadVariableRateSupported(Context context) {
        AudioManager audioManager;
        if (this.isOffloadVariableRateSupported != null) {
            return this.isOffloadVariableRateSupported.booleanValue();
        }
        boolean z = false;
        if (context != null && (audioManager = (AudioManager) context.getSystemService(MimeTypes.BASE_TYPE_AUDIO)) != null) {
            String offloadVariableRateSupportedKeyValue = audioManager.getParameters(OFFLOAD_VARIABLE_RATE_SUPPORTED_KEY);
            if (offloadVariableRateSupportedKeyValue != null && offloadVariableRateSupportedKeyValue.equals("offloadVariableRateSupported=1")) {
                z = true;
            }
            this.isOffloadVariableRateSupported = Boolean.valueOf(z);
        } else {
            this.isOffloadVariableRateSupported = false;
        }
        return this.isOffloadVariableRateSupported.booleanValue();
    }

    private static final class Api29 {
        private Api29() {
        }

        public static AudioOffloadSupport getOffloadedPlaybackSupport(AudioFormat audioFormat, android.media.AudioAttributes audioAttributes, boolean isOffloadVariableRateSupported) {
            if (!AudioManager.isOffloadedPlaybackSupported(audioFormat, audioAttributes)) {
                return AudioOffloadSupport.DEFAULT_UNSUPPORTED;
            }
            return new AudioOffloadSupport.Builder().setIsFormatSupported(true).setIsSpeedChangeSupported(isOffloadVariableRateSupported).build();
        }
    }

    private static final class Api31 {
        private Api31() {
        }

        public static AudioOffloadSupport getOffloadedPlaybackSupport(AudioFormat audioFormat, android.media.AudioAttributes audioAttributes, boolean isOffloadVariableRateSupported) {
            int playbackOffloadSupport = AudioManager.getPlaybackOffloadSupport(audioFormat, audioAttributes);
            if (playbackOffloadSupport == 0) {
                return AudioOffloadSupport.DEFAULT_UNSUPPORTED;
            }
            AudioOffloadSupport.Builder audioOffloadSupport = new AudioOffloadSupport.Builder();
            boolean isGaplessSupported = Util.SDK_INT > 32 && playbackOffloadSupport == 2;
            return audioOffloadSupport.setIsFormatSupported(true).setIsGaplessSupported(isGaplessSupported).setIsSpeedChangeSupported(isOffloadVariableRateSupported).build();
        }
    }
}
