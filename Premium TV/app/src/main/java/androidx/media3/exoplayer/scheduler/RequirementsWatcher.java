package androidx.media3.exoplayer.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class RequirementsWatcher {
    private final Context context;
    private final Handler handler = Util.createHandlerForCurrentOrMainLooper();
    private final Listener listener;
    private NetworkCallback networkCallback;
    private int notMetRequirements;
    private DeviceStatusChangeReceiver receiver;
    private final Requirements requirements;

    public interface Listener {
        void onRequirementsStateChanged(RequirementsWatcher requirementsWatcher, int i);
    }

    public RequirementsWatcher(Context context, Listener listener, Requirements requirements) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.requirements = requirements;
    }

    public int start() {
        this.notMetRequirements = this.requirements.getNotMetRequirements(this.context);
        IntentFilter filter = new IntentFilter();
        if (this.requirements.isNetworkRequired()) {
            if (Util.SDK_INT >= 24) {
                registerNetworkCallbackV24();
            } else {
                filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            }
        }
        if (this.requirements.isChargingRequired()) {
            filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
            filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        }
        if (this.requirements.isIdleRequired()) {
            if (Util.SDK_INT >= 23) {
                filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
            } else {
                filter.addAction("android.intent.action.SCREEN_ON");
                filter.addAction("android.intent.action.SCREEN_OFF");
            }
        }
        if (this.requirements.isStorageNotLowRequired()) {
            filter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
            filter.addAction("android.intent.action.DEVICE_STORAGE_OK");
        }
        this.receiver = new DeviceStatusChangeReceiver();
        this.context.registerReceiver(this.receiver, filter, null, this.handler);
        return this.notMetRequirements;
    }

    public void stop() {
        this.context.unregisterReceiver((BroadcastReceiver) Assertions.checkNotNull(this.receiver));
        this.receiver = null;
        if (Util.SDK_INT >= 24 && this.networkCallback != null) {
            unregisterNetworkCallbackV24();
        }
    }

    public Requirements getRequirements() {
        return this.requirements;
    }

    private void registerNetworkCallbackV24() {
        ConnectivityManager connectivityManager = (ConnectivityManager) Assertions.checkNotNull((ConnectivityManager) this.context.getSystemService("connectivity"));
        this.networkCallback = new NetworkCallback();
        connectivityManager.registerDefaultNetworkCallback(this.networkCallback);
    }

    private void unregisterNetworkCallbackV24() {
        ConnectivityManager connectivityManager = (ConnectivityManager) Assertions.checkNotNull((ConnectivityManager) this.context.getSystemService("connectivity"));
        connectivityManager.unregisterNetworkCallback((ConnectivityManager.NetworkCallback) Assertions.checkNotNull(this.networkCallback));
        this.networkCallback = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkRequirements() {
        int notMetRequirements = this.requirements.getNotMetRequirements(this.context);
        if (this.notMetRequirements != notMetRequirements) {
            this.notMetRequirements = notMetRequirements;
            this.listener.onRequirementsStateChanged(this, notMetRequirements);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recheckNotMetNetworkRequirements() {
        if ((this.notMetRequirements & 3) == 0) {
            return;
        }
        checkRequirements();
    }

    private class DeviceStatusChangeReceiver extends BroadcastReceiver {
        private DeviceStatusChangeReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!isInitialStickyBroadcast()) {
                RequirementsWatcher.this.checkRequirements();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class NetworkCallback extends ConnectivityManager.NetworkCallback {
        private boolean networkValidated;
        private boolean receivedCapabilitiesChange;

        private NetworkCallback() {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            postCheckRequirements();
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            postCheckRequirements();
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onBlockedStatusChanged(Network network, boolean blocked) {
            if (!blocked) {
                postRecheckNotMetNetworkRequirements();
            }
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            boolean networkValidated = networkCapabilities.hasCapability(16);
            if (!this.receivedCapabilitiesChange || this.networkValidated != networkValidated) {
                this.receivedCapabilitiesChange = true;
                this.networkValidated = networkValidated;
                postCheckRequirements();
            } else if (networkValidated) {
                postRecheckNotMetNetworkRequirements();
            }
        }

        private void postCheckRequirements() {
            RequirementsWatcher.this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.scheduler.RequirementsWatcher$NetworkCallback$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m106xcc18be42();
                }
            });
        }

        /* JADX INFO: renamed from: lambda$postCheckRequirements$0$androidx-media3-exoplayer-scheduler-RequirementsWatcher$NetworkCallback, reason: not valid java name */
        /* synthetic */ void m106xcc18be42() {
            if (RequirementsWatcher.this.networkCallback != null) {
                RequirementsWatcher.this.checkRequirements();
            }
        }

        private void postRecheckNotMetNetworkRequirements() {
            RequirementsWatcher.this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.scheduler.RequirementsWatcher$NetworkCallback$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m107xfb2bca45();
                }
            });
        }

        /* JADX INFO: renamed from: lambda$postRecheckNotMetNetworkRequirements$1$androidx-media3-exoplayer-scheduler-RequirementsWatcher$NetworkCallback, reason: not valid java name */
        /* synthetic */ void m107xfb2bca45() {
            if (RequirementsWatcher.this.networkCallback != null) {
                RequirementsWatcher.this.recheckNotMetNetworkRequirements();
            }
        }
    }
}
