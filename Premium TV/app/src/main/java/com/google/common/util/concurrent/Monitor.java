package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Monitor {

    @CheckForNull
    private Guard activeGuards;
    private final boolean fair;
    private final ReentrantLock lock;

    public static abstract class Guard {
        final Condition condition;
        final Monitor monitor;

        @CheckForNull
        Guard next;
        int waiterCount = 0;

        protected Guard(Monitor monitor) {
            this.monitor = (Monitor) Preconditions.checkNotNull(monitor, "monitor");
            this.condition = monitor.lock.newCondition();
        }

        public abstract boolean isSatisfied();
    }

    public Monitor() {
        this(false);
    }

    public Monitor(boolean z) {
        this.activeGuards = null;
        this.fair = z;
        this.lock = new ReentrantLock(z);
    }

    private void await(Guard guard, boolean z) throws InterruptedException {
        if (z) {
            signalNextWaiter();
        }
        beginWaitingFor(guard);
        do {
            try {
                guard.condition.await();
            } finally {
                endWaitingFor(guard);
            }
        } while (!guard.isSatisfied());
    }

    private boolean awaitNanos(Guard guard, long j, boolean z) throws InterruptedException {
        boolean z2 = true;
        while (j > 0) {
            if (z2) {
                if (z) {
                    try {
                        signalNextWaiter();
                    } catch (Throwable th) {
                        if (z2) {
                            throw th;
                        }
                        endWaitingFor(guard);
                        throw th;
                    }
                }
                beginWaitingFor(guard);
                z2 = false;
            }
            j = guard.condition.awaitNanos(j);
            if (guard.isSatisfied()) {
                if (!z2) {
                    endWaitingFor(guard);
                }
                return true;
            }
        }
        if (z2) {
            return false;
        }
        endWaitingFor(guard);
        return false;
    }

    private void awaitUninterruptibly(Guard guard, boolean z) {
        if (z) {
            signalNextWaiter();
        }
        beginWaitingFor(guard);
        do {
            try {
                guard.condition.awaitUninterruptibly();
            } finally {
                endWaitingFor(guard);
            }
        } while (!guard.isSatisfied());
    }

    private void beginWaitingFor(Guard guard) {
        int i = guard.waiterCount;
        guard.waiterCount = i + 1;
        if (i == 0) {
            guard.next = this.activeGuards;
            this.activeGuards = guard;
        }
    }

    private void endWaitingFor(Guard guard) {
        int i = guard.waiterCount - 1;
        guard.waiterCount = i;
        if (i == 0) {
            Guard guard2 = this.activeGuards;
            Guard guard3 = null;
            while (guard2 != guard) {
                Guard guard4 = guard2;
                guard2 = guard2.next;
                guard3 = guard4;
            }
            if (guard3 == null) {
                this.activeGuards = guard2.next;
            } else {
                guard3.next = guard2.next;
            }
            guard2.next = null;
        }
    }

    private static long initNanoTime(long j) {
        if (j <= 0) {
            return 0L;
        }
        long jNanoTime = System.nanoTime();
        if (jNanoTime == 0) {
            return 1L;
        }
        return jNanoTime;
    }

    private boolean isSatisfied(Guard guard) {
        try {
            return guard.isSatisfied();
        } catch (Throwable th) {
            signalAllWaiters();
            throw th;
        }
    }

    private static long remainingNanos(long j, long j2) {
        if (j2 <= 0) {
            return 0L;
        }
        return j2 - (System.nanoTime() - j);
    }

    private void signalAllWaiters() {
        for (Guard guard = this.activeGuards; guard != null; guard = guard.next) {
            guard.condition.signalAll();
        }
    }

    private void signalNextWaiter() {
        for (Guard guard = this.activeGuards; guard != null; guard = guard.next) {
            if (isSatisfied(guard)) {
                guard.condition.signal();
                return;
            }
        }
    }

    private static long toSafeNanos(long j, TimeUnit timeUnit) {
        return Longs.constrainToRange(timeUnit.toNanos(j), 0L, 6917529027641081853L);
    }

    public void enter() {
        this.lock.lock();
    }

    /* JADX WARN: Code duplicated, block: B:19:0x0038  */
    public boolean enter(long j, TimeUnit timeUnit) throws Throwable {
        Throwable th;
        boolean zTryLock = true;
        long safeNanos = toSafeNanos(j, timeUnit);
        ReentrantLock reentrantLock = this.lock;
        if (this.fair || !reentrantLock.tryLock()) {
            boolean zInterrupted = Thread.interrupted();
            try {
                long jNanoTime = System.nanoTime();
                long jRemainingNanos = safeNanos;
                while (true) {
                    try {
                        zTryLock = reentrantLock.tryLock(jRemainingNanos, TimeUnit.NANOSECONDS);
                        break;
                    } catch (InterruptedException e) {
                        try {
                            jRemainingNanos = remainingNanos(jNanoTime, safeNanos);
                            zInterrupted = zTryLock;
                        } catch (Throwable th2) {
                            th = th2;
                            if (zTryLock) {
                                Thread.currentThread().interrupt();
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        zTryLock = zInterrupted;
                        th = th3;
                        if (zTryLock) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                }
                if (zInterrupted) {
                    Thread.currentThread().interrupt();
                }
            } catch (Throwable th4) {
                zTryLock = zInterrupted;
                th = th4;
            }
        }
        return zTryLock;
    }

    public boolean enterIf(Guard guard) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (!zIsSatisfied) {
                reentrantLock.unlock();
            }
            return zIsSatisfied;
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
    }

    public boolean enterIf(Guard guard, long j, TimeUnit timeUnit) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        if (!enter(j, timeUnit)) {
            return false;
        }
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (zIsSatisfied) {
                return zIsSatisfied;
            }
            this.lock.unlock();
            return zIsSatisfied;
        } catch (Throwable th) {
            this.lock.unlock();
            throw th;
        }
    }

    public boolean enterIfInterruptibly(Guard guard) throws InterruptedException {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (!zIsSatisfied) {
                reentrantLock.unlock();
            }
            return zIsSatisfied;
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
    }

    public boolean enterIfInterruptibly(Guard guard, long j, TimeUnit timeUnit) throws InterruptedException {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        if (!reentrantLock.tryLock(j, timeUnit)) {
            return false;
        }
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (zIsSatisfied) {
                return zIsSatisfied;
            }
            reentrantLock.unlock();
            return zIsSatisfied;
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
    }

    public void enterInterruptibly() throws InterruptedException {
        this.lock.lockInterruptibly();
    }

    public boolean enterInterruptibly(long j, TimeUnit timeUnit) throws InterruptedException {
        return this.lock.tryLock(j, timeUnit);
    }

    public void enterWhen(Guard guard) throws InterruptedException {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        boolean zIsHeldByCurrentThread = reentrantLock.isHeldByCurrentThread();
        reentrantLock.lockInterruptibly();
        try {
            if (guard.isSatisfied()) {
                return;
            }
            await(guard, zIsHeldByCurrentThread);
        } catch (Throwable th) {
            leave();
            throw th;
        }
    }

    /* JADX WARN: Code duplicated, block: B:13:0x0028  */
    /* JADX WARN: Code duplicated, block: B:17:0x0032  */
    /* JADX WARN: Code duplicated, block: B:19:0x0035  */
    /* JADX WARN: Code duplicated, block: B:23:0x003f  */
    /* JADX WARN: Code duplicated, block: B:26:0x004a A[Catch: all -> 0x004f, TRY_ENTER, TRY_LEAVE, TryCatch #1 {all -> 0x004f, blocks: (B:11:0x0022, B:15:0x002c, B:26:0x004a), top: B:40:0x0022 }] */
    public boolean enterWhen(Guard guard, long j, TimeUnit timeUnit) throws InterruptedException {
        long jInitNanoTime;
        boolean z = false;
        long safeNanos = toSafeNanos(j, timeUnit);
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        boolean zIsHeldByCurrentThread = reentrantLock.isHeldByCurrentThread();
        if (this.fair) {
            jInitNanoTime = initNanoTime(safeNanos);
            if (reentrantLock.tryLock(j, timeUnit)) {
                try {
                    if (guard.isSatisfied()) {
                        if (jInitNanoTime != 0) {
                            safeNanos = remainingNanos(jInitNanoTime, safeNanos);
                        }
                        z = awaitNanos(guard, safeNanos, zIsHeldByCurrentThread);
                    }
                    if (!z) {
                        reentrantLock.unlock();
                    }
                } catch (Throwable th) {
                    if (!zIsHeldByCurrentThread) {
                        try {
                            signalNextWaiter();
                        } finally {
                            reentrantLock.unlock();
                        }
                    }
                    throw th;
                }
            }
        } else {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (reentrantLock.tryLock()) {
                jInitNanoTime = 0;
            } else {
                jInitNanoTime = initNanoTime(safeNanos);
                if (reentrantLock.tryLock(j, timeUnit)) {
                }
            }
            if (guard.isSatisfied()) {
                if (jInitNanoTime != 0) {
                    safeNanos = remainingNanos(jInitNanoTime, safeNanos);
                }
                if (awaitNanos(guard, safeNanos, zIsHeldByCurrentThread)) {
                }
            }
            if (!z) {
                reentrantLock.unlock();
            }
        }
        return z;
    }

    public void enterWhenUninterruptibly(Guard guard) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        boolean zIsHeldByCurrentThread = reentrantLock.isHeldByCurrentThread();
        reentrantLock.lock();
        try {
            if (guard.isSatisfied()) {
                return;
            }
            awaitUninterruptibly(guard, zIsHeldByCurrentThread);
        } catch (Throwable th) {
            leave();
            throw th;
        }
    }

    /* JADX WARN: Code duplicated, block: B:19:0x0038 A[Catch: all -> 0x0064, TRY_ENTER, TRY_LEAVE, TryCatch #2 {all -> 0x0064, blocks: (B:19:0x0038, B:33:0x0060, B:34:0x0063, B:15:0x002f, B:26:0x004c, B:28:0x0052, B:30:0x0057), top: B:61:0x002f, inners: #7 }] */
    /* JADX WARN: Code duplicated, block: B:21:0x003d  */
    /* JADX WARN: Code duplicated, block: B:24:0x0046  */
    /* JADX WARN: Code duplicated, block: B:26:0x004c A[Catch: all -> 0x005f, InterruptedException -> 0x0071, TRY_ENTER, TRY_LEAVE, TryCatch #7 {all -> 0x005f, blocks: (B:15:0x002f, B:26:0x004c, B:28:0x0052, B:30:0x0057), top: B:61:0x002f, outer: #2 }] */
    /* JADX WARN: Code duplicated, block: B:30:0x0057 A[Catch: all -> 0x005f, InterruptedException -> 0x0071, TRY_ENTER, TRY_LEAVE, TryCatch #7 {all -> 0x005f, blocks: (B:15:0x002f, B:26:0x004c, B:28:0x0052, B:30:0x0057), top: B:61:0x002f, outer: #2 }] */
    /* JADX WARN: Code duplicated, block: B:69:0x0035 A[SYNTHETIC] */
    public boolean enterWhenUninterruptibly(Guard guard, long j, TimeUnit timeUnit) throws Throwable {
        boolean z;
        Throwable th;
        boolean z2;
        boolean zAwaitNanos;
        long jInitNanoTime;
        long jRemainingNanos;
        long safeNanos = toSafeNanos(j, timeUnit);
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        long j2 = 0;
        boolean zIsHeldByCurrentThread = reentrantLock.isHeldByCurrentThread();
        boolean zInterrupted = Thread.interrupted();
        try {
            if (!this.fair && reentrantLock.tryLock()) {
                z2 = zIsHeldByCurrentThread;
                while (true) {
                    if (!guard.isSatisfied()) {
                        if (j2 == 0) {
                            jInitNanoTime = initNanoTime(safeNanos);
                            jRemainingNanos = safeNanos;
                        } else {
                            jInitNanoTime = j2;
                            jRemainingNanos = remainingNanos(j2, safeNanos);
                        }
                        zAwaitNanos = awaitNanos(guard, jRemainingNanos, z2);
                        break;
                        break;
                    }
                    zAwaitNanos = true;
                    break;
                    zInterrupted = true;
                    z2 = false;
                }
                if (!zAwaitNanos) {
                    reentrantLock.unlock();
                }
                if (zInterrupted) {
                    Thread.currentThread().interrupt();
                }
                return zAwaitNanos;
            }
            long jInitNanoTime2 = initNanoTime(safeNanos);
            z = zInterrupted;
            long jRemainingNanos2 = safeNanos;
            while (true) {
                try {
                    break;
                } catch (InterruptedException e) {
                    z = true;
                    try {
                        jRemainingNanos2 = remainingNanos(jInitNanoTime2, safeNanos);
                    } catch (Throwable th2) {
                        th = th2;
                        z = true;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            if (!reentrantLock.tryLock(jRemainingNanos2, TimeUnit.NANOSECONDS)) {
                if (z) {
                    Thread.currentThread().interrupt();
                }
                return false;
            }
            zInterrupted = z;
            j2 = jInitNanoTime2;
            z2 = zIsHeldByCurrentThread;
            while (true) {
                try {
                    try {
                        try {
                            if (!guard.isSatisfied()) {
                                zAwaitNanos = true;
                                break;
                            }
                            if (j2 == 0) {
                                jInitNanoTime = initNanoTime(safeNanos);
                                jRemainingNanos = safeNanos;
                            } else {
                                jInitNanoTime = j2;
                                jRemainingNanos = remainingNanos(j2, safeNanos);
                            }
                            try {
                                zAwaitNanos = awaitNanos(guard, jRemainingNanos, z2);
                                break;
                            } catch (InterruptedException e2) {
                                j2 = jInitNanoTime;
                            }
                        } catch (Throwable th4) {
                            reentrantLock.unlock();
                            throw th4;
                        }
                    } catch (InterruptedException e3) {
                    }
                } catch (Throwable th5) {
                    z = zInterrupted;
                    th = th5;
                }
                zInterrupted = true;
                z2 = false;
            }
            if (!zAwaitNanos) {
                reentrantLock.unlock();
            }
            if (zInterrupted) {
                Thread.currentThread().interrupt();
            }
            return zAwaitNanos;
        } catch (Throwable th6) {
            z = zInterrupted;
            th = th6;
        }
        if (z) {
            Thread.currentThread().interrupt();
        }
        throw th;
    }

    public int getOccupiedDepth() {
        return this.lock.getHoldCount();
    }

    public int getQueueLength() {
        return this.lock.getQueueLength();
    }

    public int getWaitQueueLength(Guard guard) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        this.lock.lock();
        try {
            return guard.waiterCount;
        } finally {
            this.lock.unlock();
        }
    }

    public boolean hasQueuedThread(Thread thread) {
        return this.lock.hasQueuedThread(thread);
    }

    public boolean hasQueuedThreads() {
        return this.lock.hasQueuedThreads();
    }

    public boolean hasWaiters(Guard guard) {
        return getWaitQueueLength(guard) > 0;
    }

    public boolean isFair() {
        return this.fair;
    }

    public boolean isOccupied() {
        return this.lock.isLocked();
    }

    public boolean isOccupiedByCurrentThread() {
        return this.lock.isHeldByCurrentThread();
    }

    public void leave() {
        ReentrantLock reentrantLock = this.lock;
        try {
            if (reentrantLock.getHoldCount() == 1) {
                signalNextWaiter();
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    public boolean tryEnter() {
        return this.lock.tryLock();
    }

    public boolean tryEnterIf(Guard guard) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        if (!reentrantLock.tryLock()) {
            return false;
        }
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (zIsSatisfied) {
                return zIsSatisfied;
            }
            reentrantLock.unlock();
            return zIsSatisfied;
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
    }

    public void waitFor(Guard guard) throws InterruptedException {
        if (guard.monitor != this || !this.lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        if (guard.isSatisfied()) {
            return;
        }
        await(guard, true);
    }

    public boolean waitFor(Guard guard, long j, TimeUnit timeUnit) throws InterruptedException {
        long safeNanos = toSafeNanos(j, timeUnit);
        if (guard.monitor != this || !this.lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        if (guard.isSatisfied()) {
            return true;
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return awaitNanos(guard, safeNanos, true);
    }

    public void waitForUninterruptibly(Guard guard) {
        if (guard.monitor != this || !this.lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        if (guard.isSatisfied()) {
            return;
        }
        awaitUninterruptibly(guard, true);
    }

    /* JADX WARN: Code duplicated, block: B:22:0x004b  */
    public boolean waitForUninterruptibly(Guard guard, long j, TimeUnit timeUnit) throws Throwable {
        boolean zAwaitNanos = true;
        long safeNanos = toSafeNanos(j, timeUnit);
        if (guard.monitor != this || !this.lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        if (!guard.isSatisfied()) {
            long jInitNanoTime = initNanoTime(safeNanos);
            boolean zInterrupted = Thread.interrupted();
            long jRemainingNanos = safeNanos;
            boolean z = true;
            while (true) {
                try {
                    zAwaitNanos = awaitNanos(guard, jRemainingNanos, z);
                    if (!zInterrupted) {
                        break;
                    }
                    Thread.currentThread().interrupt();
                    break;
                } catch (InterruptedException e) {
                    try {
                        if (guard.isSatisfied()) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        z = false;
                        jRemainingNanos = remainingNanos(jInitNanoTime, safeNanos);
                        zInterrupted = zAwaitNanos;
                    } catch (Throwable th) {
                        th = th;
                        if (zAwaitNanos) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    zAwaitNanos = zInterrupted;
                    if (zAwaitNanos) {
                        Thread.currentThread().interrupt();
                    }
                    throw th;
                }
            }
        }
        return zAwaitNanos;
    }
}
