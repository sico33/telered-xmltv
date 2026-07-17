package com.google.common.util.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.LockSupport;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class InterruptibleTask<T> extends AtomicReference<Runnable> implements Runnable {
    private static final Runnable DONE;
    private static final int MAX_BUSY_WAIT_SPINS = 1000;
    private static final Runnable PARKED;

    static final class Blocker extends AbstractOwnableSynchronizer implements Runnable {
        private final InterruptibleTask<?> task;

        private Blocker(InterruptibleTask<?> interruptibleTask) {
            this.task = interruptibleTask;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setOwner(Thread thread) {
            super.setExclusiveOwnerThread(thread);
        }

        @CheckForNull
        Thread getOwner() {
            return super.getExclusiveOwnerThread();
        }

        @Override // java.lang.Runnable
        public void run() {
        }

        public String toString() {
            return this.task.toString();
        }
    }

    private static final class DoNothingRunnable implements Runnable {
        private DoNothingRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
        }
    }

    static {
        DONE = new DoNothingRunnable();
        PARKED = new DoNothingRunnable();
    }

    InterruptibleTask() {
    }

    private void waitForInterrupt(Thread thread) {
        boolean z;
        int i = 0;
        boolean z2 = false;
        Runnable runnable = get();
        Blocker blocker = null;
        while (true) {
            if (!(runnable instanceof Blocker) && runnable != PARKED) {
                break;
            }
            Blocker blocker2 = runnable instanceof Blocker ? (Blocker) runnable : blocker;
            i++;
            if (i <= 1000) {
                Thread.yield();
                z = z2;
            } else if (runnable == PARKED || compareAndSet(runnable, PARKED)) {
                boolean z3 = Thread.interrupted() || z2;
                LockSupport.park(blocker2);
                z = z3;
            } else {
                z = z2;
            }
            z2 = z;
            runnable = get();
            blocker = blocker2;
        }
        if (z2) {
            thread.interrupt();
        }
    }

    abstract void afterRanInterruptiblyFailure(Throwable th);

    abstract void afterRanInterruptiblySuccess(@ParametricNullness T t);

    final void interruptTask() {
        Runnable runnable = get();
        if (runnable instanceof Thread) {
            Blocker blocker = new Blocker();
            blocker.setOwner(Thread.currentThread());
            if (compareAndSet(runnable, blocker)) {
                try {
                    ((Thread) runnable).interrupt();
                } finally {
                    if (getAndSet(DONE) == PARKED) {
                        LockSupport.unpark((Thread) runnable);
                    }
                }
            }
        }
    }

    abstract boolean isDone();

    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.lang.Runnable
    public final void run() {
        Object objRunInterruptibly = null;
        Thread threadCurrentThread = Thread.currentThread();
        if (compareAndSet(null, threadCurrentThread)) {
            boolean z = !isDone();
            if (z) {
                try {
                    objRunInterruptibly = runInterruptibly();
                } catch (Throwable th) {
                    try {
                        Platform.restoreInterruptIfIsInterruptedException(th);
                        if (!compareAndSet(threadCurrentThread, DONE)) {
                            waitForInterrupt(threadCurrentThread);
                        }
                        if (z) {
                            afterRanInterruptiblyFailure(th);
                            return;
                        }
                        return;
                    } catch (Throwable th2) {
                        if (!compareAndSet(threadCurrentThread, DONE)) {
                            waitForInterrupt(threadCurrentThread);
                        }
                        if (z) {
                            afterRanInterruptiblySuccess(NullnessCasts.uncheckedCastNullableTToT(null));
                        }
                        throw th2;
                    }
                }
            }
            if (!compareAndSet(threadCurrentThread, DONE)) {
                waitForInterrupt(threadCurrentThread);
            }
            if (z) {
                afterRanInterruptiblySuccess(NullnessCasts.uncheckedCastNullableTToT(objRunInterruptibly));
            }
        }
    }

    @ParametricNullness
    abstract T runInterruptibly() throws Exception;

    abstract String toPendingString();

    @Override // java.util.concurrent.atomic.AtomicReference
    public final String toString() {
        String str;
        Runnable runnable = get();
        if (runnable == DONE) {
            str = "running=[DONE]";
        } else if (runnable instanceof Blocker) {
            str = "running=[INTERRUPTED]";
        } else {
            str = runnable instanceof Thread ? "running=[RUNNING ON " + ((Thread) runnable).getName() + "]" : "running=[NOT STARTED YET]";
        }
        return str + ", " + toPendingString();
    }
}
