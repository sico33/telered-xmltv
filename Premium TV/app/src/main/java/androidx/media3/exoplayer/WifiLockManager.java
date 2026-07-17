package androidx.media3.exoplayer;

import android.content.Context;
import android.net.wifi.WifiManager;
import androidx.media3.common.util.Log;

/* JADX INFO: loaded from: classes.dex */
final class WifiLockManager {
    private static final String TAG = "WifiLockManager";
    private static final String WIFI_LOCK_TAG = "ExoPlayer:WifiLockManager";
    private final Context applicationContext;
    private boolean enabled;
    private boolean stayAwake;
    private WifiManager.WifiLock wifiLock;

    public WifiLockManager(Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    public void setEnabled(boolean enabled) {
        if (enabled && this.wifiLock == null) {
            WifiManager wifiManager = (WifiManager) this.applicationContext.getApplicationContext().getSystemService("wifi");
            if (wifiManager == null) {
                Log.w(TAG, "WifiManager is null, therefore not creating the WifiLock.");
                return;
            } else {
                this.wifiLock = wifiManager.createWifiLock(3, WIFI_LOCK_TAG);
                this.wifiLock.setReferenceCounted(false);
            }
        }
        this.enabled = enabled;
        updateWifiLock();
    }

    public void setStayAwake(boolean stayAwake) {
        this.stayAwake = stayAwake;
        updateWifiLock();
    }

    private void updateWifiLock() {
        if (this.wifiLock == null) {
            return;
        }
        if (this.enabled && this.stayAwake) {
            this.wifiLock.acquire();
        } else {
            this.wifiLock.release();
        }
    }
}
