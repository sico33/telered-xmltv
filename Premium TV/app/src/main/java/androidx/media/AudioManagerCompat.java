package androidx.media;

import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

/* JADX INFO: loaded from: classes.dex */
public final class AudioManagerCompat {
    public static final int AUDIOFOCUS_GAIN = 1;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT = 2;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;
    private static final String TAG = "AudioManCompat";

    public static int requestAudioFocus(AudioManager audioManager, AudioFocusRequestCompat focusRequest) {
        if (audioManager == null) {
            throw new IllegalArgumentException("AudioManager must not be null");
        }
        if (focusRequest == null) {
            throw new IllegalArgumentException("AudioFocusRequestCompat must not be null");
        }
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.requestAudioFocus(audioManager, focusRequest.getAudioFocusRequest());
        }
        return audioManager.requestAudioFocus(focusRequest.getOnAudioFocusChangeListener(), focusRequest.getAudioAttributesCompat().getLegacyStreamType(), focusRequest.getFocusGain());
    }

    public static int abandonAudioFocusRequest(AudioManager audioManager, AudioFocusRequestCompat focusRequest) {
        if (audioManager == null) {
            throw new IllegalArgumentException("AudioManager must not be null");
        }
        if (focusRequest == null) {
            throw new IllegalArgumentException("AudioFocusRequestCompat must not be null");
        }
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.abandonAudioFocusRequest(audioManager, focusRequest.getAudioFocusRequest());
        }
        return audioManager.abandonAudioFocus(focusRequest.getOnAudioFocusChangeListener());
    }

    public static int getStreamMaxVolume(AudioManager audioManager, int streamType) {
        return audioManager.getStreamMaxVolume(streamType);
    }

    public static int getStreamMinVolume(AudioManager audioManager, int streamType) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.getStreamMinVolume(audioManager, streamType);
        }
        return 0;
    }

    public static boolean isVolumeFixed(AudioManager audioManager) {
        return Api21Impl.isVolumeFixed(audioManager);
    }

    private AudioManagerCompat() {
    }

    private static class Api21Impl {
        static boolean isVolumeFixed(AudioManager audioManager) {
            return audioManager.isVolumeFixed();
        }

        private Api21Impl() {
        }
    }

    private static class Api26Impl {
        static int abandonAudioFocusRequest(AudioManager audioManager, AudioFocusRequest focusRequest) {
            return audioManager.abandonAudioFocusRequest(focusRequest);
        }

        static int requestAudioFocus(AudioManager audioManager, AudioFocusRequest focusRequest) {
            return audioManager.requestAudioFocus(focusRequest);
        }

        private Api26Impl() {
        }
    }

    private static class Api28Impl {
        static int getStreamMinVolume(AudioManager audioManager, int streamType) {
            return audioManager.getStreamMinVolume(streamType);
        }

        private Api28Impl() {
        }
    }
}
