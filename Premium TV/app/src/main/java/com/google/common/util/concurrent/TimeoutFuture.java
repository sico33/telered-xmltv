package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class TimeoutFuture<V> extends FluentFuture.TrustedFuture<V> {

    @CheckForNull
    private ListenableFuture<V> delegateRef;

    @CheckForNull
    private ScheduledFuture<?> timer;

    private static final class Fire<V> implements Runnable {

        @CheckForNull
        TimeoutFuture<V> timeoutFutureRef;

        Fire(TimeoutFuture<V> timeoutFuture) {
            this.timeoutFutureRef = timeoutFuture;
        }

        @Override // java.lang.Runnable
        public void run() {
            ListenableFuture<? extends V> listenableFuture;
            TimeoutFuture<V> timeoutFuture = this.timeoutFutureRef;
            if (timeoutFuture == null || (listenableFuture = ((TimeoutFuture) timeoutFuture).delegateRef) == null) {
                return;
            }
            this.timeoutFutureRef = null;
            if (listenableFuture.isDone()) {
                timeoutFuture.setFuture(listenableFuture);
                return;
            }
            try {
                ScheduledFuture scheduledFuture = ((TimeoutFuture) timeoutFuture).timer;
                ((TimeoutFuture) timeoutFuture).timer = null;
                String str = "Timed out";
                if (scheduledFuture != null) {
                    try {
                        long jAbs = Math.abs(scheduledFuture.getDelay(TimeUnit.MILLISECONDS));
                        if (jAbs > 10) {
                            str = "Timed out (timeout delayed by " + jAbs + " ms after scheduled time)";
                        }
                    } catch (Throwable th) {
                        timeoutFuture.setException(new TimeoutFutureException(str));
                        throw th;
                    }
                }
                timeoutFuture.setException(new TimeoutFutureException(str + ": " + listenableFuture));
                listenableFuture.cancel(true);
            } catch (Throwable th2) {
                listenableFuture.cancel(true);
                throw th2;
            }
        }
    }

    private static final class TimeoutFutureException extends TimeoutException {
        private TimeoutFutureException(String str) {
            super(str);
        }

        @Override // java.lang.Throwable
        public Throwable fillInStackTrace() {
            synchronized (this) {
                setStackTrace(new StackTraceElement[0]);
            }
            return this;
        }
    }

    private TimeoutFuture(ListenableFuture<V> listenableFuture) {
        this.delegateRef = (ListenableFuture) Preconditions.checkNotNull(listenableFuture);
    }

    static <V> ListenableFuture<V> create(ListenableFuture<V> listenableFuture, long j, TimeUnit timeUnit, ScheduledExecutorService scheduledExecutorService) {
        TimeoutFuture timeoutFuture = new TimeoutFuture(listenableFuture);
        Fire fire = new Fire(timeoutFuture);
        timeoutFuture.timer = scheduledExecutorService.schedule(fire, j, timeUnit);
        listenableFuture.addListener(fire, MoreExecutors.directExecutor());
        return timeoutFuture;
    }

    @Override // com.google.common.util.concurrent.AbstractFuture
    protected void afterDone() {
        maybePropagateCancellationTo(this.delegateRef);
        ScheduledFuture<?> scheduledFuture = this.timer;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        this.delegateRef = null;
        this.timer = null;
    }

    @Override // com.google.common.util.concurrent.AbstractFuture
    @CheckForNull
    protected String pendingToString() {
        ListenableFuture<V> listenableFuture = this.delegateRef;
        ScheduledFuture<?> scheduledFuture = this.timer;
        if (listenableFuture == null) {
            return null;
        }
        String str = "inputFuture=[" + listenableFuture + "]";
        if (scheduledFuture == null) {
            return str;
        }
        long delay = scheduledFuture.getDelay(TimeUnit.MILLISECONDS);
        return delay > 0 ? str + ", remaining delay=[" + delay + " ms]" : str;
    }
}
