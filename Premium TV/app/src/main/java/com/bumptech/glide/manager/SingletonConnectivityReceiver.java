package com.bumptech.glide.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import com.bumptech.glide.util.GlideSuppliers;
import com.bumptech.glide.util.Util;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
final class SingletonConnectivityReceiver {
    private static final String TAG = "ConnectivityMonitor";
    private static volatile SingletonConnectivityReceiver instance;
    private final FrameworkConnectivityMonitor frameworkConnectivityMonitor;
    private boolean isRegistered;
    final Set<ConnectivityMonitor.ConnectivityListener> listeners = new HashSet();

    private interface FrameworkConnectivityMonitor {
        boolean register();

        void unregister();
    }

    private static final class FrameworkConnectivityMonitorPostApi24 implements FrameworkConnectivityMonitor {
        private final GlideSuppliers.GlideSupplier<ConnectivityManager> connectivityManager;
        boolean isConnected;
        final ConnectivityMonitor.ConnectivityListener listener;
        private final ConnectivityManager.NetworkCallback networkCallback = new AnonymousClass1(this);

        /* JADX INFO: renamed from: com.bumptech.glide.manager.SingletonConnectivityReceiver$FrameworkConnectivityMonitorPostApi24$1, reason: invalid class name */
        class AnonymousClass1 extends ConnectivityManager.NetworkCallback {
            final FrameworkConnectivityMonitorPostApi24 this$0;

            AnonymousClass1(FrameworkConnectivityMonitorPostApi24 frameworkConnectivityMonitorPostApi24) {
                this.this$0 = frameworkConnectivityMonitorPostApi24;
            }

            private void postOnConnectivityChange(boolean z) {
                Util.postOnUiThread(new Runnable(this, z) { // from class: com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitorPostApi24.1.1
                    final AnonymousClass1 this$1;
                    final boolean val$newState;

                    {
                        this.this$1 = this;
                        this.val$newState = z;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        this.this$1.onConnectivityChange(this.val$newState);
                    }
                });
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                postOnConnectivityChange(true);
            }

            void onConnectivityChange(boolean z) {
                Util.assertMainThread();
                boolean z2 = this.this$0.isConnected;
                this.this$0.isConnected = z;
                if (z2 != z) {
                    this.this$0.listener.onConnectivityChanged(z);
                }
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                postOnConnectivityChange(false);
            }
        }

        FrameworkConnectivityMonitorPostApi24(GlideSuppliers.GlideSupplier<ConnectivityManager> glideSupplier, ConnectivityMonitor.ConnectivityListener connectivityListener) {
            this.connectivityManager = glideSupplier;
            this.listener = connectivityListener;
        }

        @Override // com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitor
        public boolean register() {
            this.isConnected = this.connectivityManager.get().getActiveNetwork() != null;
            try {
                this.connectivityManager.get().registerDefaultNetworkCallback(this.networkCallback);
                return true;
            } catch (RuntimeException e) {
                if (Log.isLoggable(SingletonConnectivityReceiver.TAG, 5)) {
                    Log.w(SingletonConnectivityReceiver.TAG, "Failed to register callback", e);
                }
                return false;
            }
        }

        @Override // com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitor
        public void unregister() {
            this.connectivityManager.get().unregisterNetworkCallback(this.networkCallback);
        }
    }

    private static final class FrameworkConnectivityMonitorPreApi24 implements FrameworkConnectivityMonitor {
        static final Executor EXECUTOR = AsyncTask.SERIAL_EXECUTOR;
        private final GlideSuppliers.GlideSupplier<ConnectivityManager> connectivityManager;
        final BroadcastReceiver connectivityReceiver = new BroadcastReceiver(this) { // from class: com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitorPreApi24.1
            final FrameworkConnectivityMonitorPreApi24 this$0;

            {
                this.this$0 = this;
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                this.this$0.onConnectivityChange();
            }
        };
        final Context context;
        volatile boolean isConnected;
        volatile boolean isRegistered;
        final ConnectivityMonitor.ConnectivityListener listener;

        FrameworkConnectivityMonitorPreApi24(Context context, GlideSuppliers.GlideSupplier<ConnectivityManager> glideSupplier, ConnectivityMonitor.ConnectivityListener connectivityListener) {
            this.context = context.getApplicationContext();
            this.connectivityManager = glideSupplier;
            this.listener = connectivityListener;
        }

        boolean isConnected() {
            try {
                NetworkInfo activeNetworkInfo = this.connectivityManager.get().getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            } catch (RuntimeException e) {
                if (Log.isLoggable(SingletonConnectivityReceiver.TAG, 5)) {
                    Log.w(SingletonConnectivityReceiver.TAG, "Failed to determine connectivity status when connectivity changed", e);
                }
                return true;
            }
        }

        void notifyChangeOnUiThread(boolean z) {
            Util.postOnUiThread(new Runnable(this, z) { // from class: com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitorPreApi24.5
                final FrameworkConnectivityMonitorPreApi24 this$0;
                final boolean val$isConnected;

                {
                    this.this$0 = this;
                    this.val$isConnected = z;
                }

                @Override // java.lang.Runnable
                public void run() {
                    this.this$0.listener.onConnectivityChanged(this.val$isConnected);
                }
            });
        }

        void onConnectivityChange() {
            EXECUTOR.execute(new Runnable(this) { // from class: com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitorPreApi24.4
                final FrameworkConnectivityMonitorPreApi24 this$0;

                {
                    this.this$0 = this;
                }

                @Override // java.lang.Runnable
                public void run() {
                    boolean z = this.this$0.isConnected;
                    this.this$0.isConnected = this.this$0.isConnected();
                    if (z != this.this$0.isConnected) {
                        if (Log.isLoggable(SingletonConnectivityReceiver.TAG, 3)) {
                            Log.d(SingletonConnectivityReceiver.TAG, "connectivity changed, isConnected: " + this.this$0.isConnected);
                        }
                        this.this$0.notifyChangeOnUiThread(this.this$0.isConnected);
                    }
                }
            });
        }

        @Override // com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitor
        public boolean register() {
            EXECUTOR.execute(new Runnable(this) { // from class: com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitorPreApi24.2
                final FrameworkConnectivityMonitorPreApi24 this$0;

                {
                    this.this$0 = this;
                }

                @Override // java.lang.Runnable
                public void run() {
                    this.this$0.isConnected = this.this$0.isConnected();
                    try {
                        this.this$0.context.registerReceiver(this.this$0.connectivityReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
                        this.this$0.isRegistered = true;
                    } catch (SecurityException e) {
                        if (Log.isLoggable(SingletonConnectivityReceiver.TAG, 5)) {
                            Log.w(SingletonConnectivityReceiver.TAG, "Failed to register", e);
                        }
                        this.this$0.isRegistered = false;
                    }
                }
            });
            return true;
        }

        @Override // com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitor
        public void unregister() {
            EXECUTOR.execute(new Runnable(this) { // from class: com.bumptech.glide.manager.SingletonConnectivityReceiver.FrameworkConnectivityMonitorPreApi24.3
                final FrameworkConnectivityMonitorPreApi24 this$0;

                {
                    this.this$0 = this;
                }

                @Override // java.lang.Runnable
                public void run() {
                    if (this.this$0.isRegistered) {
                        this.this$0.isRegistered = false;
                        this.this$0.context.unregisterReceiver(this.this$0.connectivityReceiver);
                    }
                }
            });
        }
    }

    private SingletonConnectivityReceiver(Context context) {
        GlideSuppliers.GlideSupplier glideSupplierMemorize = GlideSuppliers.memorize(new GlideSuppliers.GlideSupplier<ConnectivityManager>(this, context) { // from class: com.bumptech.glide.manager.SingletonConnectivityReceiver.1
            final SingletonConnectivityReceiver this$0;
            final Context val$context;

            {
                this.this$0 = this;
                this.val$context = context;
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.bumptech.glide.util.GlideSuppliers.GlideSupplier
            public ConnectivityManager get() {
                return (ConnectivityManager) this.val$context.getSystemService("connectivity");
            }
        });
        ConnectivityMonitor.ConnectivityListener connectivityListener = new ConnectivityMonitor.ConnectivityListener(this) { // from class: com.bumptech.glide.manager.SingletonConnectivityReceiver.2
            final SingletonConnectivityReceiver this$0;

            {
                this.this$0 = this;
            }

            @Override // com.bumptech.glide.manager.ConnectivityMonitor.ConnectivityListener
            public void onConnectivityChanged(boolean z) {
                ArrayList arrayList;
                Util.assertMainThread();
                synchronized (this.this$0) {
                    arrayList = new ArrayList(this.this$0.listeners);
                }
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    ((ConnectivityMonitor.ConnectivityListener) it.next()).onConnectivityChanged(z);
                }
            }
        };
        this.frameworkConnectivityMonitor = Build.VERSION.SDK_INT >= 24 ? new FrameworkConnectivityMonitorPostApi24(glideSupplierMemorize, connectivityListener) : new FrameworkConnectivityMonitorPreApi24(context, glideSupplierMemorize, connectivityListener);
    }

    static SingletonConnectivityReceiver get(Context context) {
        if (instance == null) {
            synchronized (SingletonConnectivityReceiver.class) {
                try {
                    if (instance == null) {
                        instance = new SingletonConnectivityReceiver(context.getApplicationContext());
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return instance;
    }

    private void maybeRegisterReceiver() {
        if (this.isRegistered || this.listeners.isEmpty()) {
            return;
        }
        this.isRegistered = this.frameworkConnectivityMonitor.register();
    }

    private void maybeUnregisterReceiver() {
        if (this.isRegistered && this.listeners.isEmpty()) {
            this.frameworkConnectivityMonitor.unregister();
            this.isRegistered = false;
        }
    }

    static void reset() {
        instance = null;
    }

    void register(ConnectivityMonitor.ConnectivityListener connectivityListener) {
        synchronized (this) {
            this.listeners.add(connectivityListener);
            maybeRegisterReceiver();
        }
    }

    void unregister(ConnectivityMonitor.ConnectivityListener connectivityListener) {
        synchronized (this) {
            this.listeners.remove(connectivityListener);
            maybeUnregisterReceiver();
        }
    }
}
