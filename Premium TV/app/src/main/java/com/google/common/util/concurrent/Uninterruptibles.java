package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Uninterruptibles {
    private Uninterruptibles() {
    }

    public static void awaitTerminationUninterruptibly(ExecutorService executorService) {
        Verify.verify(awaitTerminationUninterruptibly(executorService, Long.MAX_VALUE, TimeUnit.NANOSECONDS));
    }

    public static boolean awaitTerminationUninterruptibly(ExecutorService executorService, long j, TimeUnit timeUnit) throws Throwable {
        Throwable th;
        boolean zAwaitTermination;
        boolean z = true;
        boolean z2 = false;
        try {
            long nanos = timeUnit.toNanos(j);
            long jNanoTime = System.nanoTime();
            long jNanoTime2 = nanos;
            while (true) {
                try {
                    zAwaitTermination = executorService.awaitTermination(jNanoTime2, TimeUnit.NANOSECONDS);
                    break;
                } catch (InterruptedException e) {
                    try {
                        jNanoTime2 = (jNanoTime + nanos) - System.nanoTime();
                        z2 = z;
                    } catch (Throwable th2) {
                        th = th2;
                        if (z) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                }
            }
            if (z2) {
                Thread.currentThread().interrupt();
            }
            return zAwaitTermination;
        } catch (Throwable th3) {
            z = z2;
            th = th3;
        }
    }

    public static void awaitUninterruptibly(CountDownLatch countDownLatch) {
        boolean z = false;
        while (true) {
            try {
                countDownLatch.await();
                break;
            } catch (InterruptedException e) {
                z = true;
            } catch (Throwable th) {
                if (z) {
                    Thread.currentThread().interrupt();
                }
                throw th;
            }
        }
        if (z) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean awaitUninterruptibly(CountDownLatch countDownLatch, long j, TimeUnit timeUnit) throws Throwable {
        Throwable th;
        boolean zAwait;
        boolean z = true;
        boolean z2 = false;
        try {
            long nanos = timeUnit.toNanos(j);
            long jNanoTime = System.nanoTime();
            long jNanoTime2 = nanos;
            while (true) {
                try {
                    zAwait = countDownLatch.await(jNanoTime2, TimeUnit.NANOSECONDS);
                    break;
                } catch (InterruptedException e) {
                    try {
                        jNanoTime2 = (jNanoTime + nanos) - System.nanoTime();
                        z2 = z;
                    } catch (Throwable th2) {
                        th = th2;
                        if (z) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                }
            }
            if (z2) {
                Thread.currentThread().interrupt();
            }
            return zAwait;
        } catch (Throwable th3) {
            z = z2;
            th = th3;
        }
    }

    public static boolean awaitUninterruptibly(Condition condition, long j, TimeUnit timeUnit) throws Throwable {
        Throwable th;
        boolean zAwait;
        boolean z = true;
        boolean z2 = false;
        try {
            long nanos = timeUnit.toNanos(j);
            long jNanoTime = System.nanoTime();
            long jNanoTime2 = nanos;
            while (true) {
                try {
                    zAwait = condition.await(jNanoTime2, TimeUnit.NANOSECONDS);
                    break;
                } catch (InterruptedException e) {
                    try {
                        jNanoTime2 = (jNanoTime + nanos) - System.nanoTime();
                        z2 = z;
                    } catch (Throwable th2) {
                        th = th2;
                        if (z) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                }
            }
            if (z2) {
                Thread.currentThread().interrupt();
            }
            return zAwait;
        } catch (Throwable th3) {
            z = z2;
            th = th3;
        }
    }

    @ParametricNullness
    public static <V> V getUninterruptibly(Future<V> future) throws ExecutionException {
        V v;
        boolean z = false;
        while (true) {
            try {
                v = future.get();
                break;
            } catch (InterruptedException e) {
                z = true;
            } catch (Throwable th) {
                if (z) {
                    Thread.currentThread().interrupt();
                }
                throw th;
            }
        }
        if (z) {
            Thread.currentThread().interrupt();
        }
        return v;
    }

    @ParametricNullness
    public static <V> V getUninterruptibly(Future<V> future, long j, TimeUnit timeUnit) throws Throwable {
        Throwable th;
        V v;
        boolean z = true;
        boolean z2 = false;
        try {
            long nanos = timeUnit.toNanos(j);
            long jNanoTime = System.nanoTime();
            long jNanoTime2 = nanos;
            while (true) {
                try {
                    v = future.get(jNanoTime2, TimeUnit.NANOSECONDS);
                    break;
                } catch (InterruptedException e) {
                    try {
                        jNanoTime2 = (jNanoTime + nanos) - System.nanoTime();
                        z2 = z;
                    } catch (Throwable th2) {
                        th = th2;
                        if (z) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                }
            }
            if (z2) {
                Thread.currentThread().interrupt();
            }
            return v;
        } catch (Throwable th3) {
            z = z2;
            th = th3;
        }
    }

    public static void joinUninterruptibly(Thread thread) {
        boolean z = false;
        while (true) {
            try {
                thread.join();
                break;
            } catch (InterruptedException e) {
                z = true;
            } catch (Throwable th) {
                if (z) {
                    Thread.currentThread().interrupt();
                }
                throw th;
            }
        }
        if (z) {
            Thread.currentThread().interrupt();
        }
    }

    public static void joinUninterruptibly(Thread thread, long j, TimeUnit timeUnit) throws Throwable {
        Throwable th;
        boolean z = true;
        boolean z2 = false;
        Preconditions.checkNotNull(thread);
        try {
            long nanos = timeUnit.toNanos(j);
            long jNanoTime = System.nanoTime();
            long jNanoTime2 = nanos;
            while (true) {
                try {
                    TimeUnit.NANOSECONDS.timedJoin(thread, jNanoTime2);
                    break;
                } catch (InterruptedException e) {
                    try {
                        jNanoTime2 = (jNanoTime + nanos) - System.nanoTime();
                        z2 = true;
                    } catch (Throwable th2) {
                        th = th2;
                        if (z) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                }
            }
            if (z2) {
                Thread.currentThread().interrupt();
            }
        } catch (Throwable th3) {
            z = z2;
            th = th3;
        }
    }

    public static <E> void putUninterruptibly(BlockingQueue<E> blockingQueue, E e) {
        boolean z = false;
        while (true) {
            try {
                blockingQueue.put(e);
                break;
            } catch (InterruptedException e2) {
                z = true;
            } catch (Throwable th) {
                if (z) {
                    Thread.currentThread().interrupt();
                }
                throw th;
            }
        }
        if (z) {
            Thread.currentThread().interrupt();
        }
    }

    public static void sleepUninterruptibly(long j, TimeUnit timeUnit) throws Throwable {
        Throwable th;
        boolean z = true;
        boolean z2 = false;
        try {
            long nanos = timeUnit.toNanos(j);
            long jNanoTime = System.nanoTime();
            long jNanoTime2 = nanos;
            while (true) {
                try {
                    TimeUnit.NANOSECONDS.sleep(jNanoTime2);
                    break;
                } catch (InterruptedException e) {
                    try {
                        jNanoTime2 = (jNanoTime + nanos) - System.nanoTime();
                        z2 = true;
                    } catch (Throwable th2) {
                        th = th2;
                        if (z) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                }
            }
            if (z2) {
                Thread.currentThread().interrupt();
            }
        } catch (Throwable th3) {
            z = z2;
            th = th3;
        }
    }

    public static <E> E takeUninterruptibly(BlockingQueue<E> blockingQueue) {
        E eTake;
        boolean z = false;
        while (true) {
            try {
                eTake = blockingQueue.take();
                break;
            } catch (InterruptedException e) {
                z = true;
            } catch (Throwable th) {
                if (z) {
                    Thread.currentThread().interrupt();
                }
                throw th;
            }
        }
        if (z) {
            Thread.currentThread().interrupt();
        }
        return eTake;
    }

    public static boolean tryAcquireUninterruptibly(Semaphore semaphore, int i, long j, TimeUnit timeUnit) throws Throwable {
        Throwable th;
        boolean zTryAcquire;
        boolean z = true;
        boolean z2 = false;
        try {
            long nanos = timeUnit.toNanos(j);
            long jNanoTime = System.nanoTime();
            long jNanoTime2 = nanos;
            while (true) {
                try {
                    zTryAcquire = semaphore.tryAcquire(i, jNanoTime2, TimeUnit.NANOSECONDS);
                    break;
                } catch (InterruptedException e) {
                    try {
                        jNanoTime2 = (jNanoTime + nanos) - System.nanoTime();
                        z2 = z;
                    } catch (Throwable th2) {
                        th = th2;
                        if (z) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                }
            }
            if (z2) {
                Thread.currentThread().interrupt();
            }
            return zTryAcquire;
        } catch (Throwable th3) {
            z = z2;
            th = th3;
        }
    }

    public static boolean tryAcquireUninterruptibly(Semaphore semaphore, long j, TimeUnit timeUnit) {
        return tryAcquireUninterruptibly(semaphore, 1, j, timeUnit);
    }

    public static boolean tryLockUninterruptibly(Lock lock, long j, TimeUnit timeUnit) throws Throwable {
        Throwable th;
        boolean zTryLock;
        boolean z = true;
        boolean z2 = false;
        try {
            long nanos = timeUnit.toNanos(j);
            long jNanoTime = System.nanoTime();
            long jNanoTime2 = nanos;
            while (true) {
                try {
                    zTryLock = lock.tryLock(jNanoTime2, TimeUnit.NANOSECONDS);
                    break;
                } catch (InterruptedException e) {
                    try {
                        jNanoTime2 = (jNanoTime + nanos) - System.nanoTime();
                        z2 = z;
                    } catch (Throwable th2) {
                        th = th2;
                        if (z) {
                            Thread.currentThread().interrupt();
                        }
                        throw th;
                    }
                }
            }
            if (z2) {
                Thread.currentThread().interrupt();
            }
            return zTryLock;
        } catch (Throwable th3) {
            z = z2;
            th = th3;
        }
    }
}
