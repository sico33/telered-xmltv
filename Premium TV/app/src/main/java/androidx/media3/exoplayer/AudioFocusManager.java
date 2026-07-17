package androidx.media3.exoplayer;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
final class AudioFocusManager {
    private static final int AUDIOFOCUS_GAIN = 1;
    private static final int AUDIOFOCUS_GAIN_TRANSIENT = 2;
    private static final int AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4;
    private static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;
    private static final int AUDIOFOCUS_NONE = 0;
    private static final int AUDIO_FOCUS_STATE_HAVE_FOCUS = 2;
    private static final int AUDIO_FOCUS_STATE_LOSS_TRANSIENT = 3;
    private static final int AUDIO_FOCUS_STATE_LOSS_TRANSIENT_DUCK = 4;
    private static final int AUDIO_FOCUS_STATE_NOT_REQUESTED = 0;
    private static final int AUDIO_FOCUS_STATE_NO_FOCUS = 1;
    public static final int PLAYER_COMMAND_DO_NOT_PLAY = -1;
    public static final int PLAYER_COMMAND_PLAY_WHEN_READY = 1;
    public static final int PLAYER_COMMAND_WAIT_FOR_CALLBACK = 0;
    private static final String TAG = "AudioFocusManager";
    private static final float VOLUME_MULTIPLIER_DEFAULT = 1.0f;
    private static final float VOLUME_MULTIPLIER_DUCK = 0.2f;
    private AudioAttributes audioAttributes;
    private AudioFocusRequest audioFocusRequest;
    private final AudioManager audioManager;
    private int focusGainToRequest;
    private final AudioFocusListener focusListener;
    private PlayerControl playerControl;
    private boolean rebuildAudioFocusRequest;
    private float volumeMultiplier = 1.0f;
    private int audioFocusState = 0;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerCommand {
    }

    public interface PlayerControl {
        void executePlayerCommand(int i);

        void setVolumeMultiplier(float f);
    }

    public AudioFocusManager(Context context, Handler eventHandler, PlayerControl playerControl) {
        this.audioManager = (AudioManager) Assertions.checkNotNull((AudioManager) context.getApplicationContext().getSystemService(MimeTypes.BASE_TYPE_AUDIO));
        this.playerControl = playerControl;
        this.focusListener = new AudioFocusListener(eventHandler);
    }

    public float getVolumeMultiplier() {
        return this.volumeMultiplier;
    }

    public void setAudioAttributes(AudioAttributes audioAttributes) {
        if (!Util.areEqual(this.audioAttributes, audioAttributes)) {
            this.audioAttributes = audioAttributes;
            this.focusGainToRequest = convertAudioAttributesToFocusGain(audioAttributes);
            boolean z = true;
            if (this.focusGainToRequest != 1 && this.focusGainToRequest != 0) {
                z = false;
            }
            Assertions.checkArgument(z, "Automatic handling of audio focus is only available for USAGE_MEDIA and USAGE_GAME.");
        }
    }

    public int updateAudioFocus(boolean playWhenReady, int playbackState) {
        if (!shouldHandleAudioFocus(playbackState)) {
            abandonAudioFocusIfHeld();
            setAudioFocusState(0);
            return 1;
        }
        if (playWhenReady) {
            return requestAudioFocus();
        }
        switch (this.audioFocusState) {
            case 1:
                return -1;
            case 2:
            default:
                return 1;
            case 3:
                return 0;
        }
    }

    public void release() {
        this.playerControl = null;
        abandonAudioFocusIfHeld();
        setAudioFocusState(0);
    }

    AudioManager.OnAudioFocusChangeListener getFocusListener() {
        return this.focusListener;
    }

    private boolean shouldHandleAudioFocus(int playbackState) {
        return playbackState != 1 && this.focusGainToRequest == 1;
    }

    private int requestAudioFocus() {
        if (this.audioFocusState == 2) {
            return 1;
        }
        int requestResult = Util.SDK_INT >= 26 ? requestAudioFocusV26() : requestAudioFocusDefault();
        if (requestResult == 1) {
            setAudioFocusState(2);
            return 1;
        }
        setAudioFocusState(1);
        return -1;
    }

    private void abandonAudioFocusIfHeld() {
        if (this.audioFocusState == 1 || this.audioFocusState == 0) {
            return;
        }
        if (Util.SDK_INT >= 26) {
            abandonAudioFocusV26();
        } else {
            abandonAudioFocusDefault();
        }
    }

    private int requestAudioFocusDefault() {
        return this.audioManager.requestAudioFocus(this.focusListener, Util.getStreamTypeForAudioUsage(((AudioAttributes) Assertions.checkNotNull(this.audioAttributes)).usage), this.focusGainToRequest);
    }

    private int requestAudioFocusV26() {
        AudioFocusRequest.Builder builder;
        if (this.audioFocusRequest == null || this.rebuildAudioFocusRequest) {
            if (this.audioFocusRequest == null) {
                builder = new AudioFocusRequest.Builder(this.focusGainToRequest);
            } else {
                builder = new AudioFocusRequest.Builder(this.audioFocusRequest);
            }
            boolean willPauseWhenDucked = willPauseWhenDucked();
            this.audioFocusRequest = builder.setAudioAttributes(((AudioAttributes) Assertions.checkNotNull(this.audioAttributes)).getAudioAttributesV21().audioAttributes).setWillPauseWhenDucked(willPauseWhenDucked).setOnAudioFocusChangeListener(this.focusListener).build();
            this.rebuildAudioFocusRequest = false;
        }
        return this.audioManager.requestAudioFocus(this.audioFocusRequest);
    }

    private void abandonAudioFocusDefault() {
        this.audioManager.abandonAudioFocus(this.focusListener);
    }

    private void abandonAudioFocusV26() {
        if (this.audioFocusRequest != null) {
            this.audioManager.abandonAudioFocusRequest(this.audioFocusRequest);
        }
    }

    private boolean willPauseWhenDucked() {
        return this.audioAttributes != null && this.audioAttributes.contentType == 1;
    }

    private static int convertAudioAttributesToFocusGain(AudioAttributes audioAttributes) {
        if (audioAttributes == null) {
            return 0;
        }
        switch (audioAttributes.usage) {
            case 0:
                Log.w(TAG, "Specify a proper usage in the audio attributes for audio focus handling. Using AUDIOFOCUS_GAIN by default.");
                return 1;
            case 1:
            case 14:
                return 1;
            case 2:
            case 4:
                return 2;
            case 3:
                return 0;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 12:
            case 13:
                return 3;
            case 11:
                return audioAttributes.contentType == 1 ? 2 : 3;
            case 15:
            default:
                Log.w(TAG, "Unidentified audio usage: " + audioAttributes.usage);
                return 0;
            case 16:
                return 4;
        }
    }

    private void setAudioFocusState(int audioFocusState) {
        float volumeMultiplier;
        if (this.audioFocusState == audioFocusState) {
            return;
        }
        this.audioFocusState = audioFocusState;
        if (audioFocusState == 4) {
            volumeMultiplier = 0.2f;
        } else {
            volumeMultiplier = 1.0f;
        }
        if (this.volumeMultiplier == volumeMultiplier) {
            return;
        }
        this.volumeMultiplier = volumeMultiplier;
        if (this.playerControl != null) {
            this.playerControl.setVolumeMultiplier(volumeMultiplier);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePlatformAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case -3:
            case -2:
                if (focusChange == -2 || willPauseWhenDucked()) {
                    executePlayerCommand(0);
                    setAudioFocusState(3);
                } else {
                    setAudioFocusState(4);
                }
                break;
            case -1:
                executePlayerCommand(-1);
                abandonAudioFocusIfHeld();
                setAudioFocusState(1);
                break;
            case 0:
            default:
                Log.w(TAG, "Unknown focus change type: " + focusChange);
                break;
            case 1:
                setAudioFocusState(2);
                executePlayerCommand(1);
                break;
        }
    }

    private void executePlayerCommand(int playerCommand) {
        if (this.playerControl != null) {
            this.playerControl.executePlayerCommand(playerCommand);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {
        private final Handler eventHandler;

        public AudioFocusListener(Handler eventHandler) {
            this.eventHandler = eventHandler;
        }

        /* JADX INFO: renamed from: lambda$onAudioFocusChange$0$androidx-media3-exoplayer-AudioFocusManager$AudioFocusListener, reason: not valid java name */
        /* synthetic */ void m40x93d291e3(int focusChange) {
            AudioFocusManager.this.handlePlatformAudioFocusChange(focusChange);
        }

        @Override // android.media.AudioManager.OnAudioFocusChangeListener
        public void onAudioFocusChange(final int focusChange) {
            this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.AudioFocusManager$AudioFocusListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m40x93d291e3(focusChange);
                }
            });
        }
    }
}
