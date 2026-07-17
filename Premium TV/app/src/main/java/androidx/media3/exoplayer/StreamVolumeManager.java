package androidx.media3.exoplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
final class StreamVolumeManager {
    private static final String TAG = "StreamVolumeManager";
    private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private final Context applicationContext;
    private final AudioManager audioManager;
    private final Handler eventHandler;
    private final Listener listener;
    private boolean muted;
    private VolumeChangeReceiver receiver;
    private int streamType = 3;
    private int volume;

    public interface Listener {
        void onStreamTypeChanged(int i);

        void onStreamVolumeChanged(int i, boolean z);
    }

    public StreamVolumeManager(Context context, Handler eventHandler, Listener listener) {
        this.applicationContext = context.getApplicationContext();
        this.eventHandler = eventHandler;
        this.listener = listener;
        this.audioManager = (AudioManager) Assertions.checkStateNotNull((AudioManager) this.applicationContext.getSystemService(MimeTypes.BASE_TYPE_AUDIO));
        this.volume = getVolumeFromManager(this.audioManager, this.streamType);
        this.muted = getMutedFromManager(this.audioManager, this.streamType);
        VolumeChangeReceiver receiver = new VolumeChangeReceiver();
        IntentFilter filter = new IntentFilter(VOLUME_CHANGED_ACTION);
        try {
            this.applicationContext.registerReceiver(receiver, filter);
            this.receiver = receiver;
        } catch (RuntimeException e) {
            Log.w(TAG, "Error registering stream volume receiver", e);
        }
    }

    public void setStreamType(int streamType) {
        if (this.streamType == streamType) {
            return;
        }
        this.streamType = streamType;
        updateVolumeAndNotifyIfChanged();
        this.listener.onStreamTypeChanged(streamType);
    }

    public int getMinVolume() {
        if (Util.SDK_INT >= 28) {
            return this.audioManager.getStreamMinVolume(this.streamType);
        }
        return 0;
    }

    public int getMaxVolume() {
        return this.audioManager.getStreamMaxVolume(this.streamType);
    }

    public int getVolume() {
        return this.volume;
    }

    public boolean isMuted() {
        return this.muted;
    }

    public void setVolume(int volume, int flags) {
        if (volume < getMinVolume() || volume > getMaxVolume()) {
            return;
        }
        this.audioManager.setStreamVolume(this.streamType, volume, flags);
        updateVolumeAndNotifyIfChanged();
    }

    public void increaseVolume(int flags) {
        if (this.volume >= getMaxVolume()) {
            return;
        }
        this.audioManager.adjustStreamVolume(this.streamType, 1, flags);
        updateVolumeAndNotifyIfChanged();
    }

    public void decreaseVolume(int flags) {
        if (this.volume <= getMinVolume()) {
            return;
        }
        this.audioManager.adjustStreamVolume(this.streamType, -1, flags);
        updateVolumeAndNotifyIfChanged();
    }

    public void setMuted(boolean muted, int flags) {
        int i = Util.SDK_INT;
        AudioManager audioManager = this.audioManager;
        if (i >= 23) {
            audioManager.adjustStreamVolume(this.streamType, muted ? -100 : 100, flags);
        } else {
            audioManager.setStreamMute(this.streamType, muted);
        }
        updateVolumeAndNotifyIfChanged();
    }

    public void release() {
        if (this.receiver != null) {
            try {
                this.applicationContext.unregisterReceiver(this.receiver);
            } catch (RuntimeException e) {
                Log.w(TAG, "Error unregistering stream volume receiver", e);
            }
            this.receiver = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVolumeAndNotifyIfChanged() {
        int newVolume = getVolumeFromManager(this.audioManager, this.streamType);
        boolean newMuted = getMutedFromManager(this.audioManager, this.streamType);
        if (this.volume != newVolume || this.muted != newMuted) {
            this.volume = newVolume;
            this.muted = newMuted;
            this.listener.onStreamVolumeChanged(newVolume, newMuted);
        }
    }

    private static int getVolumeFromManager(AudioManager audioManager, int streamType) {
        try {
            return audioManager.getStreamVolume(streamType);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not retrieve stream volume for stream type " + streamType, e);
            return audioManager.getStreamMaxVolume(streamType);
        }
    }

    private static boolean getMutedFromManager(AudioManager audioManager, int streamType) {
        if (Util.SDK_INT >= 23) {
            return audioManager.isStreamMute(streamType);
        }
        return getVolumeFromManager(audioManager, streamType) == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class VolumeChangeReceiver extends BroadcastReceiver {
        private VolumeChangeReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Handler handler = StreamVolumeManager.this.eventHandler;
            final StreamVolumeManager streamVolumeManager = StreamVolumeManager.this;
            handler.post(new Runnable() { // from class: androidx.media3.exoplayer.StreamVolumeManager$VolumeChangeReceiver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    streamVolumeManager.updateVolumeAndNotifyIfChanged();
                }
            });
        }
    }
}
