package androidx.media3.exoplayer.upstream;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.TraceUtil;
import androidx.media3.common.util.Util;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

/* JADX INFO: loaded from: classes.dex */
public final class Loader implements LoaderErrorThrower {
    private static final int ACTION_TYPE_DONT_RETRY = 2;
    private static final int ACTION_TYPE_DONT_RETRY_FATAL = 3;
    private static final int ACTION_TYPE_RETRY = 0;
    private static final int ACTION_TYPE_RETRY_AND_RESET_ERROR_COUNT = 1;
    public static final LoadErrorAction DONT_RETRY;
    public static final LoadErrorAction DONT_RETRY_FATAL;
    public static final LoadErrorAction RETRY;
    public static final LoadErrorAction RETRY_RESET_ERROR_COUNT;
    private static final String THREAD_NAME_PREFIX = "ExoPlayer:Loader:";
    private LoadTask<? extends Loadable> currentTask;
    private final ExecutorService downloadExecutorService;
    private IOException fatalError;

    public interface Callback<T extends Loadable> {
        void onLoadCanceled(T t, long j, long j2, boolean z);

        void onLoadCompleted(T t, long j, long j2);

        LoadErrorAction onLoadError(T t, long j, long j2, IOException iOException, int i);
    }

    public interface Loadable {
        void cancelLoad();

        void load() throws IOException;
    }

    public interface ReleaseCallback {
        void onLoaderReleased();
    }

    public static final class UnexpectedLoaderException extends IOException {
        public UnexpectedLoaderException(Throwable cause) {
            super("Unexpected " + cause.getClass().getSimpleName() + (cause.getMessage() != null ? ": " + cause.getMessage() : ""), cause);
        }
    }

    static {
        long j = C.TIME_UNSET;
        RETRY = createRetryAction(false, C.TIME_UNSET);
        RETRY_RESET_ERROR_COUNT = createRetryAction(true, C.TIME_UNSET);
        DONT_RETRY = new LoadErrorAction(2, j);
        DONT_RETRY_FATAL = new LoadErrorAction(3, j);
    }

    public static final class LoadErrorAction {
        private final long retryDelayMillis;
        private final int type;

        private LoadErrorAction(int type, long retryDelayMillis) {
            this.type = type;
            this.retryDelayMillis = retryDelayMillis;
        }

        public boolean isRetry() {
            return this.type == 0 || this.type == 1;
        }
    }

    public Loader(String threadNameSuffix) {
        this.downloadExecutorService = Util.newSingleThreadExecutor(THREAD_NAME_PREFIX + threadNameSuffix);
    }

    public static LoadErrorAction createRetryAction(boolean z, long j) {
        return new LoadErrorAction(z ? 1 : 0, j);
    }

    public boolean hasFatalError() {
        return this.fatalError != null;
    }

    public void clearFatalError() {
        this.fatalError = null;
    }

    public <T extends Loadable> long startLoading(T loadable, Callback<T> callback, int defaultMinRetryCount) {
        Looper looper = (Looper) Assertions.checkStateNotNull(Looper.myLooper());
        this.fatalError = null;
        long startTimeMs = SystemClock.elapsedRealtime();
        new LoadTask(looper, loadable, callback, defaultMinRetryCount, startTimeMs).start(0L);
        return startTimeMs;
    }

    public boolean isLoading() {
        return this.currentTask != null;
    }

    public void cancelLoading() {
        ((LoadTask) Assertions.checkStateNotNull(this.currentTask)).cancel(false);
    }

    public void release() {
        release(null);
    }

    public void release(ReleaseCallback callback) {
        if (this.currentTask != null) {
            this.currentTask.cancel(true);
        }
        if (callback != null) {
            this.downloadExecutorService.execute(new ReleaseTask(callback));
        }
        this.downloadExecutorService.shutdown();
    }

    @Override // androidx.media3.exoplayer.upstream.LoaderErrorThrower
    public void maybeThrowError() throws IOException {
        maybeThrowError(Integer.MIN_VALUE);
    }

    @Override // androidx.media3.exoplayer.upstream.LoaderErrorThrower
    public void maybeThrowError(int minRetryCount) throws IOException {
        if (this.fatalError != null) {
            throw this.fatalError;
        }
        if (this.currentTask != null) {
            this.currentTask.maybeThrowError(minRetryCount == Integer.MIN_VALUE ? this.currentTask.defaultMinRetryCount : minRetryCount);
        }
    }

    private final class LoadTask<T extends Loadable> extends Handler implements Runnable {
        private static final int MSG_FATAL_ERROR = 4;
        private static final int MSG_FINISH = 2;
        private static final int MSG_IO_EXCEPTION = 3;
        private static final int MSG_START = 1;
        private static final String TAG = "LoadTask";
        private Callback<T> callback;
        private boolean canceled;
        private IOException currentError;
        public final int defaultMinRetryCount;
        private int errorCount;
        private Thread executorThread;
        private final T loadable;
        private volatile boolean released;
        private final long startTimeMs;

        public LoadTask(Looper looper, T loadable, Callback<T> callback, int defaultMinRetryCount, long startTimeMs) {
            super(looper);
            this.loadable = loadable;
            this.callback = callback;
            this.defaultMinRetryCount = defaultMinRetryCount;
            this.startTimeMs = startTimeMs;
        }

        public void maybeThrowError(int minRetryCount) throws IOException {
            if (this.currentError != null && this.errorCount > minRetryCount) {
                throw this.currentError;
            }
        }

        public void start(long delayMillis) {
            Assertions.checkState(Loader.this.currentTask == null);
            Loader.this.currentTask = this;
            if (delayMillis > 0) {
                sendEmptyMessageDelayed(1, delayMillis);
            } else {
                execute();
            }
        }

        public void cancel(boolean released) {
            this.released = released;
            this.currentError = null;
            if (hasMessages(1)) {
                this.canceled = true;
                removeMessages(1);
                if (!released) {
                    sendEmptyMessage(2);
                }
            } else {
                synchronized (this) {
                    this.canceled = true;
                    this.loadable.cancelLoad();
                    Thread executorThread = this.executorThread;
                    if (executorThread != null) {
                        executorThread.interrupt();
                    }
                }
            }
            if (released) {
                finish();
                long nowMs = SystemClock.elapsedRealtime();
                ((Callback) Assertions.checkNotNull(this.callback)).onLoadCanceled(this.loadable, nowMs, nowMs - this.startTimeMs, true);
                this.callback = null;
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            boolean shouldLoad;
            try {
                synchronized (this) {
                    shouldLoad = !this.canceled;
                    this.executorThread = Thread.currentThread();
                }
                if (shouldLoad) {
                    TraceUtil.beginSection("load:" + this.loadable.getClass().getSimpleName());
                    try {
                        this.loadable.load();
                        TraceUtil.endSection();
                    } catch (Throwable th) {
                        TraceUtil.endSection();
                        throw th;
                    }
                }
                synchronized (this) {
                    this.executorThread = null;
                    Thread.interrupted();
                }
                if (!this.released) {
                    sendEmptyMessage(2);
                }
            } catch (IOException e) {
                if (!this.released) {
                    obtainMessage(3, e).sendToTarget();
                }
            } catch (Exception e2) {
                if (!this.released) {
                    Log.e(TAG, "Unexpected exception loading stream", e2);
                    obtainMessage(3, new UnexpectedLoaderException(e2)).sendToTarget();
                }
            } catch (OutOfMemoryError e3) {
                if (!this.released) {
                    Log.e(TAG, "OutOfMemory error loading stream", e3);
                    obtainMessage(3, new UnexpectedLoaderException(e3)).sendToTarget();
                }
            } catch (Error e4) {
                if (!this.released) {
                    Log.e(TAG, "Unexpected error loading stream", e4);
                    obtainMessage(4, e4).sendToTarget();
                }
                throw e4;
            }
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            long retryDelayMillis;
            if (this.released) {
                return;
            }
            if (msg.what == 1) {
                execute();
                return;
            }
            if (msg.what == 4) {
                throw ((Error) msg.obj);
            }
            finish();
            long nowMs = SystemClock.elapsedRealtime();
            long durationMs = nowMs - this.startTimeMs;
            Callback<T> callback = (Callback) Assertions.checkNotNull(this.callback);
            if (this.canceled) {
                callback.onLoadCanceled(this.loadable, nowMs, durationMs, false);
                return;
            }
            switch (msg.what) {
                case 2:
                    try {
                        callback.onLoadCompleted(this.loadable, nowMs, durationMs);
                        return;
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Unexpected exception handling load completed", e);
                        Loader.this.fatalError = new UnexpectedLoaderException(e);
                        return;
                    }
                case 3:
                    this.currentError = (IOException) msg.obj;
                    this.errorCount++;
                    LoadErrorAction action = callback.onLoadError(this.loadable, nowMs, durationMs, this.currentError, this.errorCount);
                    if (action.type != 3) {
                        if (action.type != 2) {
                            if (action.type == 1) {
                                this.errorCount = 1;
                            }
                            if (action.retryDelayMillis != C.TIME_UNSET) {
                                retryDelayMillis = action.retryDelayMillis;
                            } else {
                                retryDelayMillis = getRetryDelayMillis();
                            }
                            start(retryDelayMillis);
                            return;
                        }
                        return;
                    }
                    Loader.this.fatalError = this.currentError;
                    return;
                default:
                    return;
            }
        }

        private void execute() {
            this.currentError = null;
            Loader.this.downloadExecutorService.execute((Runnable) Assertions.checkNotNull(Loader.this.currentTask));
        }

        private void finish() {
            Loader.this.currentTask = null;
        }

        private long getRetryDelayMillis() {
            return Math.min((this.errorCount - 1) * 1000, 5000);
        }
    }

    private static final class ReleaseTask implements Runnable {
        private final ReleaseCallback callback;

        public ReleaseTask(ReleaseCallback callback) {
            this.callback = callback;
        }

        @Override // java.lang.Runnable
        public void run() {
            this.callback.onLoaderReleased();
        }
    }
}
