package androidx.media3.common.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: classes.dex */
public final class NetworkTypeObserver {
    private static NetworkTypeObserver staticInstance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<WeakReference<Listener>> listeners = new CopyOnWriteArrayList<>();
    private final Object networkTypeLock = new Object();
    private int networkType = 0;

    public interface Listener {
        void onNetworkTypeChanged(int i);
    }

    public static synchronized NetworkTypeObserver getInstance(Context context) {
        if (staticInstance == null) {
            staticInstance = new NetworkTypeObserver(context);
        }
        return staticInstance;
    }

    public static synchronized void resetForTests() {
        staticInstance = null;
    }

    private NetworkTypeObserver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(new Receiver(), filter);
    }

    public void register(final Listener listener) {
        removeClearedReferences();
        this.listeners.add(new WeakReference<>(listener));
        this.mainHandler.post(new Runnable() { // from class: androidx.media3.common.util.NetworkTypeObserver$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m37xc4ab8e3(listener);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$register$0$androidx-media3-common-util-NetworkTypeObserver, reason: not valid java name */
    /* synthetic */ void m37xc4ab8e3(Listener listener) {
        listener.onNetworkTypeChanged(getNetworkType());
    }

    public int getNetworkType() {
        int i;
        synchronized (this.networkTypeLock) {
            i = this.networkType;
        }
        return i;
    }

    private void removeClearedReferences() {
        for (WeakReference<Listener> listenerReference : this.listeners) {
            if (listenerReference.get() == null) {
                this.listeners.remove(listenerReference);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNetworkType(int networkType) {
        synchronized (this.networkTypeLock) {
            if (this.networkType == networkType) {
                return;
            }
            this.networkType = networkType;
            for (WeakReference<Listener> listenerReference : this.listeners) {
                Listener listener = listenerReference.get();
                if (listener != null) {
                    listener.onNetworkTypeChanged(networkType);
                } else {
                    this.listeners.remove(listenerReference);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getNetworkTypeFromConnectivityManager(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager == null) {
            return 0;
        }
        try {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                return 1;
            }
            switch (networkInfo.getType()) {
                case 0:
                case 4:
                case 5:
                    return getMobileNetworkType(networkInfo);
                case 1:
                    return 2;
                case 2:
                case 3:
                case 7:
                case 8:
                default:
                    return 8;
                case 6:
                    return 5;
                case 9:
                    return 7;
            }
        } catch (SecurityException e) {
            return 0;
        }
    }

    private static int getMobileNetworkType(NetworkInfo networkInfo) {
        switch (networkInfo.getSubtype()) {
            case 1:
            case 2:
                return 3;
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 14:
            case 15:
            case 17:
                return 4;
            case 13:
                return 5;
            case 16:
            case 19:
            default:
                return 6;
            case 18:
                return 2;
            case 20:
                return Util.SDK_INT >= 29 ? 9 : 0;
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int networkType = NetworkTypeObserver.getNetworkTypeFromConnectivityManager(context);
            if (Util.SDK_INT < 31 || networkType != 5) {
                NetworkTypeObserver.this.updateNetworkType(networkType);
            } else {
                Api31.disambiguate4gAnd5gNsa(context, NetworkTypeObserver.this);
            }
        }
    }

    private static final class Api31 {
        private Api31() {
        }

        public static void disambiguate4gAnd5gNsa(Context context, NetworkTypeObserver instance) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) Assertions.checkNotNull((TelephonyManager) context.getSystemService("phone"));
                DisplayInfoCallback callback = new DisplayInfoCallback(instance);
                telephonyManager.registerTelephonyCallback(context.getMainExecutor(), callback);
                telephonyManager.unregisterTelephonyCallback(callback);
            } catch (RuntimeException e) {
                instance.updateNetworkType(5);
            }
        }

        private static final class DisplayInfoCallback extends TelephonyCallback implements TelephonyCallback.DisplayInfoListener {
            private final NetworkTypeObserver instance;

            public DisplayInfoCallback(NetworkTypeObserver instance) {
                this.instance = instance;
            }

            public void onDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo) {
                int overrideNetworkType = telephonyDisplayInfo.getOverrideNetworkType();
                boolean is5gNsa = overrideNetworkType == 3 || overrideNetworkType == 4 || overrideNetworkType == 5;
                this.instance.updateNetworkType(is5gNsa ? 10 : 5);
            }
        }
    }
}
