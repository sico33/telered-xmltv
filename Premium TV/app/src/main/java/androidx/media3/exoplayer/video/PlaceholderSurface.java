package androidx.media3.exoplayer.video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.EGLSurfaceTexture;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.Log;

/* JADX INFO: loaded from: classes.dex */
public final class PlaceholderSurface extends Surface {
    private static final String TAG = "PlaceholderSurface";
    private static int secureMode;
    private static boolean secureModeInitialized;
    public final boolean secure;
    private final PlaceholderSurfaceThread thread;
    private boolean threadReleased;

    public static synchronized boolean isSecureSupported(Context context) {
        if (!secureModeInitialized) {
            secureMode = getSecureMode(context);
            secureModeInitialized = true;
        }
        return secureMode != 0;
    }

    @Deprecated
    public static PlaceholderSurface newInstanceV17(Context context, boolean secure) {
        return newInstance(context, secure);
    }

    public static PlaceholderSurface newInstance(Context context, boolean secure) {
        Assertions.checkState(!secure || isSecureSupported(context));
        PlaceholderSurfaceThread thread = new PlaceholderSurfaceThread();
        return thread.init(secure ? secureMode : 0);
    }

    private PlaceholderSurface(PlaceholderSurfaceThread thread, SurfaceTexture surfaceTexture, boolean secure) {
        super(surfaceTexture);
        this.thread = thread;
        this.secure = secure;
    }

    @Override // android.view.Surface
    public void release() {
        super.release();
        synchronized (this.thread) {
            if (!this.threadReleased) {
                this.thread.release();
                this.threadReleased = true;
            }
        }
    }

    private static int getSecureMode(Context context) {
        if (GlUtil.isProtectedContentExtensionSupported(context)) {
            if (GlUtil.isSurfacelessContextExtensionSupported()) {
                return 1;
            }
            return 2;
        }
        return 0;
    }

    private static class PlaceholderSurfaceThread extends HandlerThread implements Handler.Callback {
        private static final int MSG_INIT = 1;
        private static final int MSG_RELEASE = 2;
        private EGLSurfaceTexture eglSurfaceTexture;
        private Handler handler;
        private Error initError;
        private RuntimeException initException;
        private PlaceholderSurface surface;

        public PlaceholderSurfaceThread() {
            super("ExoPlayer:PlaceholderSurface");
        }

        public PlaceholderSurface init(int secureMode) {
            start();
            this.handler = new Handler(getLooper(), this);
            this.eglSurfaceTexture = new EGLSurfaceTexture(this.handler);
            boolean wasInterrupted = false;
            synchronized (this) {
                this.handler.obtainMessage(1, secureMode, 0).sendToTarget();
                while (this.surface == null && this.initException == null && this.initError == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        wasInterrupted = true;
                    }
                }
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
            if (this.initException != null) {
                throw this.initException;
            }
            if (this.initError != null) {
                throw this.initError;
            }
            return (PlaceholderSurface) Assertions.checkNotNull(this.surface);
        }

        public void release() {
            Assertions.checkNotNull(this.handler);
            this.handler.sendEmptyMessage(2);
        }

        /* JADX WARN: Code duplicated, block: B:59:0x006d A[EXC_TOP_SPLITTER, SYNTHETIC] */
        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case 1:
                        try {
                            initInternal(msg.arg1);
                            synchronized (this) {
                                notify();
                                break;
                            }
                        } catch (GlUtil.GlException e) {
                            Log.e(PlaceholderSurface.TAG, "Failed to initialize placeholder surface", e);
                            this.initException = new IllegalStateException(e);
                            synchronized (this) {
                                notify();
                            }
                        } catch (Error e2) {
                            Log.e(PlaceholderSurface.TAG, "Failed to initialize placeholder surface", e2);
                            this.initError = e2;
                            synchronized (this) {
                                notify();
                            }
                        } catch (RuntimeException e3) {
                            Log.e(PlaceholderSurface.TAG, "Failed to initialize placeholder surface", e3);
                            this.initException = e3;
                            synchronized (this) {
                                notify();
                            }
                        }
                        return true;
                    case 2:
                        try {
                            releaseInternal();
                            break;
                        } catch (Throwable e4) {
                            try {
                                Log.e(PlaceholderSurface.TAG, "Failed to release placeholder surface", e4);
                            } finally {
                                quit();
                            }
                            break;
                        }
                        return true;
                    default:
                        return true;
                }
            } catch (Throwable th) {
                synchronized (this) {
                    notify();
                    throw th;
                }
            }
            synchronized (this) {
                notify();
                throw th;
            }
        }

        private void initInternal(int secureMode) throws GlUtil.GlException {
            Assertions.checkNotNull(this.eglSurfaceTexture);
            this.eglSurfaceTexture.init(secureMode);
            this.surface = new PlaceholderSurface(this, this.eglSurfaceTexture.getSurfaceTexture(), secureMode != 0);
        }

        private void releaseInternal() {
            Assertions.checkNotNull(this.eglSurfaceTexture);
            this.eglSurfaceTexture.release();
        }
    }
}
