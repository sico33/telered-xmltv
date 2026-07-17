package androidx.media3.exoplayer;

import android.content.Context;
import android.os.PowerManager;
import androidx.media3.common.util.Log;

/* JADX INFO: loaded from: classes.dex */
final class WakeLockManager {
    private static final String TAG = "WakeLockManager";
    private static final String WAKE_LOCK_TAG = "ExoPlayer:WakeLockManager";
    private final Context applicationContext;
    private boolean enabled;
    private boolean stayAwake;
    private PowerManager.WakeLock wakeLock;

    public WakeLockManager(Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    public void setEnabled(boolean enabled) {
        if (enabled && this.wakeLock == null) {
            PowerManager powerManager = (PowerManager) this.applicationContext.getSystemService("power");
            if (powerManager == null) {
                Log.w(TAG, "PowerManager is null, therefore not creating the WakeLock.");
                return;
            } else {
                this.wakeLock = powerManager.newWakeLock(1, WAKE_LOCK_TAG);
                this.wakeLock.setReferenceCounted(false);
            }
        }
        this.enabled = enabled;
        updateWakeLock();
    }

    public void setStayAwake(boolean stayAwake) {
        this.stayAwake = stayAwake;
        updateWakeLock();
    }

    private void updateWakeLock() {
        if (this.wakeLock == null) {
            return;
        }
        if (this.enabled && this.stayAwake) {
            this.wakeLock.acquire();
        } else {
            this.wakeLock.release();
        }
    }
}
