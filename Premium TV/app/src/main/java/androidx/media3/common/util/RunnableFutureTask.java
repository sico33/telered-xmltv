package androidx.media3.common.util;

import java.lang.Exception;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
public abstract class RunnableFutureTask<R, E extends Exception> implements RunnableFuture<R> {
    private boolean canceled;
    private Exception exception;
    private R result;
    private Thread workThread;
    private final ConditionVariable started = new ConditionVariable();
    private final ConditionVariable finished = new ConditionVariable();
    private final Object cancelLock = new Object();

    protected abstract R doWork() throws Exception;

    protected RunnableFutureTask() {
    }

    public final void blockUntilStarted() {
        this.started.blockUninterruptible();
    }

    public final void blockUntilFinished() {
        this.finished.blockUninterruptible();
    }

    @Override // java.util.concurrent.Future
    public final R get() throws ExecutionException, InterruptedException {
        this.finished.block();
        return getResult();
    }

    @Override // java.util.concurrent.Future
    public final R get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        long timeoutMs = TimeUnit.MILLISECONDS.convert(timeout, unit);
        if (!this.finished.block(timeoutMs)) {
            throw new TimeoutException();
        }
        return getResult();
    }

    @Override // java.util.concurrent.Future
    public final boolean cancel(boolean interruptIfRunning) {
        synchronized (this.cancelLock) {
            if (!this.canceled && !this.finished.isOpen()) {
                this.canceled = true;
                cancelWork();
                Thread workThread = this.workThread;
                if (workThread != null) {
                    if (interruptIfRunning) {
                        workThread.interrupt();
                    }
                } else {
                    this.started.open();
                    this.finished.open();
                }
                return true;
            }
            return false;
        }
    }

    @Override // java.util.concurrent.Future
    public final boolean isDone() {
        return this.finished.isOpen();
    }

    @Override // java.util.concurrent.Future
    public final boolean isCancelled() {
        return this.canceled;
    }

    @Override // java.util.concurrent.RunnableFuture, java.lang.Runnable
    public final void run() {
        synchronized (this.cancelLock) {
            if (this.canceled) {
                return;
            }
            this.workThread = Thread.currentThread();
            this.started.open();
            try {
                try {
                    this.result = doWork();
                    synchronized (this.cancelLock) {
                        this.finished.open();
                        this.workThread = null;
                        Thread.interrupted();
                    }
                } catch (Exception e) {
                    this.exception = e;
                    synchronized (this.cancelLock) {
                        this.finished.open();
                        this.workThread = null;
                        Thread.interrupted();
                    }
                }
            } catch (Throwable th) {
                synchronized (this.cancelLock) {
                    this.finished.open();
                    this.workThread = null;
                    Thread.interrupted();
                    throw th;
                }
            }
        }
    }

    protected void cancelWork() {
    }

    private R getResult() throws ExecutionException {
        if (this.canceled) {
            throw new CancellationException();
        }
        if (this.exception != null) {
            throw new ExecutionException(this.exception);
        }
        return this.result;
    }
}
