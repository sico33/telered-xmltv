package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class MoreExecutors {

    static class Application {
        Application() {
        }

        final void addDelayedShutdownHook(ExecutorService executorService, long j, TimeUnit timeUnit) {
            Preconditions.checkNotNull(executorService);
            Preconditions.checkNotNull(timeUnit);
            addShutdownHook(MoreExecutors.newThread("DelayedShutdownHook-for-" + executorService, new Runnable(this, executorService, j, timeUnit) { // from class: com.google.common.util.concurrent.MoreExecutors.Application.1
                final ExecutorService val$service;
                final long val$terminationTimeout;
                final TimeUnit val$timeUnit;

                {
                    this.val$service = executorService;
                    this.val$terminationTimeout = j;
                    this.val$timeUnit = timeUnit;
                }

                @Override // java.lang.Runnable
                public void run() {
                    try {
                        this.val$service.shutdown();
                        this.val$service.awaitTermination(this.val$terminationTimeout, this.val$timeUnit);
                    } catch (InterruptedException e) {
                    }
                }
            }));
        }

        void addShutdownHook(Thread thread) {
            Runtime.getRuntime().addShutdownHook(thread);
        }

        final ExecutorService getExitingExecutorService(ThreadPoolExecutor threadPoolExecutor) {
            return getExitingExecutorService(threadPoolExecutor, 120L, TimeUnit.SECONDS);
        }

        final ExecutorService getExitingExecutorService(ThreadPoolExecutor threadPoolExecutor, long j, TimeUnit timeUnit) {
            MoreExecutors.useDaemonThreadFactory(threadPoolExecutor);
            ExecutorService executorServiceUnconfigurableExecutorService = Executors.unconfigurableExecutorService(threadPoolExecutor);
            addDelayedShutdownHook(threadPoolExecutor, j, timeUnit);
            return executorServiceUnconfigurableExecutorService;
        }

        final ScheduledExecutorService getExitingScheduledExecutorService(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
            return getExitingScheduledExecutorService(scheduledThreadPoolExecutor, 120L, TimeUnit.SECONDS);
        }

        final ScheduledExecutorService getExitingScheduledExecutorService(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor, long j, TimeUnit timeUnit) {
            MoreExecutors.useDaemonThreadFactory(scheduledThreadPoolExecutor);
            ScheduledExecutorService scheduledExecutorServiceUnconfigurableScheduledExecutorService = Executors.unconfigurableScheduledExecutorService(scheduledThreadPoolExecutor);
            addDelayedShutdownHook(scheduledThreadPoolExecutor, j, timeUnit);
            return scheduledExecutorServiceUnconfigurableScheduledExecutorService;
        }
    }

    private static final class DirectExecutorService extends AbstractListeningExecutorService {
        private final Object lock;
        private int runningTasks;
        private boolean shutdown;

        private DirectExecutorService() {
            this.lock = new Object();
            this.runningTasks = 0;
            this.shutdown = false;
        }

        private void endTask() {
            synchronized (this.lock) {
                int i = this.runningTasks - 1;
                this.runningTasks = i;
                if (i == 0) {
                    this.lock.notifyAll();
                }
            }
        }

        private void startTask() {
            synchronized (this.lock) {
                if (this.shutdown) {
                    throw new RejectedExecutionException("Executor already shutdown");
                }
                this.runningTasks++;
            }
        }

        @Override // java.util.concurrent.ExecutorService
        public boolean awaitTermination(long j, TimeUnit timeUnit) throws InterruptedException {
            long nanos = timeUnit.toNanos(j);
            synchronized (this.lock) {
                while (true) {
                    if (this.shutdown && this.runningTasks == 0) {
                        return true;
                    }
                    if (nanos <= 0) {
                        return false;
                    }
                    long jNanoTime = System.nanoTime();
                    TimeUnit.NANOSECONDS.timedWait(this.lock, nanos);
                    nanos -= System.nanoTime() - jNanoTime;
                }
            }
        }

        @Override // java.util.concurrent.Executor
        public void execute(Runnable runnable) {
            startTask();
            try {
                runnable.run();
            } finally {
                endTask();
            }
        }

        @Override // java.util.concurrent.ExecutorService
        public boolean isShutdown() {
            boolean z;
            synchronized (this.lock) {
                z = this.shutdown;
            }
            return z;
        }

        @Override // java.util.concurrent.ExecutorService
        public boolean isTerminated() {
            boolean z;
            synchronized (this.lock) {
                z = this.shutdown && this.runningTasks == 0;
            }
            return z;
        }

        @Override // java.util.concurrent.ExecutorService
        public void shutdown() {
            synchronized (this.lock) {
                this.shutdown = true;
                if (this.runningTasks == 0) {
                    this.lock.notifyAll();
                }
            }
        }

        @Override // java.util.concurrent.ExecutorService
        public List<Runnable> shutdownNow() {
            shutdown();
            return Collections.emptyList();
        }
    }

    private static class ListeningDecorator extends AbstractListeningExecutorService {
        private final ExecutorService delegate;

        ListeningDecorator(ExecutorService executorService) {
            this.delegate = (ExecutorService) Preconditions.checkNotNull(executorService);
        }

        @Override // java.util.concurrent.ExecutorService
        public final boolean awaitTermination(long j, TimeUnit timeUnit) throws InterruptedException {
            return this.delegate.awaitTermination(j, timeUnit);
        }

        @Override // java.util.concurrent.Executor
        public final void execute(Runnable runnable) {
            this.delegate.execute(runnable);
        }

        @Override // java.util.concurrent.ExecutorService
        public final boolean isShutdown() {
            return this.delegate.isShutdown();
        }

        @Override // java.util.concurrent.ExecutorService
        public final boolean isTerminated() {
            return this.delegate.isTerminated();
        }

        @Override // java.util.concurrent.ExecutorService
        public final void shutdown() {
            this.delegate.shutdown();
        }

        @Override // java.util.concurrent.ExecutorService
        public final List<Runnable> shutdownNow() {
            return this.delegate.shutdownNow();
        }

        public final String toString() {
            return super.toString() + "[" + this.delegate + "]";
        }
    }

    private static final class ScheduledListeningDecorator extends ListeningDecorator implements ListeningScheduledExecutorService {
        final ScheduledExecutorService delegate;

        private static final class ListenableScheduledTask<V> extends ForwardingListenableFuture.SimpleForwardingListenableFuture<V> implements ListenableScheduledFuture<V> {
            private final ScheduledFuture<?> scheduledDelegate;

            public ListenableScheduledTask(ListenableFuture<V> listenableFuture, ScheduledFuture<?> scheduledFuture) {
                super(listenableFuture);
                this.scheduledDelegate = scheduledFuture;
            }

            @Override // com.google.common.util.concurrent.ForwardingFuture, java.util.concurrent.Future
            public boolean cancel(boolean z) {
                boolean zCancel = super.cancel(z);
                if (zCancel) {
                    this.scheduledDelegate.cancel(z);
                }
                return zCancel;
            }

            @Override // java.lang.Comparable
            public int compareTo(Delayed delayed) {
                return this.scheduledDelegate.compareTo(delayed);
            }

            @Override // java.util.concurrent.Delayed
            public long getDelay(TimeUnit timeUnit) {
                return this.scheduledDelegate.getDelay(timeUnit);
            }
        }

        private static final class NeverSuccessfulListenableFutureTask extends AbstractFuture.TrustedFuture<Void> implements Runnable {
            private final Runnable delegate;

            public NeverSuccessfulListenableFutureTask(Runnable runnable) {
                this.delegate = (Runnable) Preconditions.checkNotNull(runnable);
            }

            @Override // com.google.common.util.concurrent.AbstractFuture
            protected String pendingToString() {
                return "task=[" + this.delegate + "]";
            }

            @Override // java.lang.Runnable
            public void run() {
                try {
                    this.delegate.run();
                } catch (Throwable th) {
                    setException(th);
                    throw th;
                }
            }
        }

        ScheduledListeningDecorator(ScheduledExecutorService scheduledExecutorService) {
            super(scheduledExecutorService);
            this.delegate = (ScheduledExecutorService) Preconditions.checkNotNull(scheduledExecutorService);
        }

        @Override // java.util.concurrent.ScheduledExecutorService
        public ListenableScheduledFuture<?> schedule(Runnable runnable, long j, TimeUnit timeUnit) {
            TrustedListenableFutureTask trustedListenableFutureTaskCreate = TrustedListenableFutureTask.create(runnable, null);
            return new ListenableScheduledTask(trustedListenableFutureTaskCreate, this.delegate.schedule(trustedListenableFutureTaskCreate, j, timeUnit));
        }

        @Override // java.util.concurrent.ScheduledExecutorService
        public <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long j, TimeUnit timeUnit) {
            TrustedListenableFutureTask trustedListenableFutureTaskCreate = TrustedListenableFutureTask.create(callable);
            return new ListenableScheduledTask(trustedListenableFutureTaskCreate, this.delegate.schedule(trustedListenableFutureTaskCreate, j, timeUnit));
        }

        @Override // java.util.concurrent.ScheduledExecutorService
        public ListenableScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long j, long j2, TimeUnit timeUnit) {
            NeverSuccessfulListenableFutureTask neverSuccessfulListenableFutureTask = new NeverSuccessfulListenableFutureTask(runnable);
            return new ListenableScheduledTask(neverSuccessfulListenableFutureTask, this.delegate.scheduleAtFixedRate(neverSuccessfulListenableFutureTask, j, j2, timeUnit));
        }

        @Override // java.util.concurrent.ScheduledExecutorService
        public ListenableScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long j, long j2, TimeUnit timeUnit) {
            NeverSuccessfulListenableFutureTask neverSuccessfulListenableFutureTask = new NeverSuccessfulListenableFutureTask(runnable);
            return new ListenableScheduledTask(neverSuccessfulListenableFutureTask, this.delegate.scheduleWithFixedDelay(neverSuccessfulListenableFutureTask, j, j2, timeUnit));
        }
    }

    private MoreExecutors() {
    }

    public static void addDelayedShutdownHook(ExecutorService executorService, long j, TimeUnit timeUnit) {
        new Application().addDelayedShutdownHook(executorService, j, timeUnit);
    }

    public static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
    }

    public static ExecutorService getExitingExecutorService(ThreadPoolExecutor threadPoolExecutor) {
        return new Application().getExitingExecutorService(threadPoolExecutor);
    }

    public static ExecutorService getExitingExecutorService(ThreadPoolExecutor threadPoolExecutor, long j, TimeUnit timeUnit) {
        return new Application().getExitingExecutorService(threadPoolExecutor, j, timeUnit);
    }

    public static ScheduledExecutorService getExitingScheduledExecutorService(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        return new Application().getExitingScheduledExecutorService(scheduledThreadPoolExecutor);
    }

    public static ScheduledExecutorService getExitingScheduledExecutorService(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor, long j, TimeUnit timeUnit) {
        return new Application().getExitingScheduledExecutorService(scheduledThreadPoolExecutor, j, timeUnit);
    }

    /* JADX WARN: Code duplicated, block: B:28:0x0090 A[LOOP:2: B:26:0x008a->B:28:0x0090, LOOP_END] */
    @ParametricNullness
    static <T> T invokeAnyImpl(ListeningExecutorService listeningExecutorService, Collection<? extends Callable<T>> collection, boolean z, long j, TimeUnit timeUnit) throws Throwable {
        long jNanoTime;
        Throwable th;
        Iterator it;
        Future future;
        int i;
        int i2;
        ExecutionException e;
        Preconditions.checkNotNull(listeningExecutorService);
        Preconditions.checkNotNull(timeUnit);
        int size = collection.size();
        Preconditions.checkArgument(size > 0);
        ArrayList arrayListNewArrayListWithCapacity = Lists.newArrayListWithCapacity(size);
        LinkedBlockingQueue linkedBlockingQueueNewLinkedBlockingQueue = Queues.newLinkedBlockingQueue();
        long nanos = timeUnit.toNanos(j);
        if (z) {
            try {
                jNanoTime = System.nanoTime();
            } catch (Throwable th2) {
                th = th2;
                it = arrayListNewArrayListWithCapacity.iterator();
                while (it.hasNext()) {
                    ((Future) it.next()).cancel(true);
                }
                throw th;
            }
        } else {
            jNanoTime = 0;
        }
        Iterator<? extends Callable<T>> it2 = collection.iterator();
        arrayListNewArrayListWithCapacity.add(submitAndAddQueueListener(listeningExecutorService, it2.next(), linkedBlockingQueueNewLinkedBlockingQueue));
        long j2 = jNanoTime;
        int i3 = 1;
        ExecutionException executionException = null;
        long j3 = nanos;
        int i4 = size - 1;
        long j4 = j2;
        while (true) {
            try {
                Future future2 = (Future) linkedBlockingQueueNewLinkedBlockingQueue.poll();
                if (future2 != null) {
                    int i5 = i4;
                    future = future2;
                    i = i3;
                    i2 = i5;
                } else if (i4 > 0) {
                    int i6 = i4 - 1;
                    arrayListNewArrayListWithCapacity.add(submitAndAddQueueListener(listeningExecutorService, it2.next(), linkedBlockingQueueNewLinkedBlockingQueue));
                    future = future2;
                    i = i3 + 1;
                    i2 = i6;
                } else {
                    if (i3 == 0) {
                        if (executionException == null) {
                            throw new ExecutionException((Throwable) null);
                        }
                        throw executionException;
                    }
                    if (z) {
                        Future future3 = (Future) linkedBlockingQueueNewLinkedBlockingQueue.poll(j3, TimeUnit.NANOSECONDS);
                        if (future3 == null) {
                            throw new TimeoutException();
                        }
                        long jNanoTime2 = System.nanoTime();
                        j3 -= jNanoTime2 - j4;
                        j4 = jNanoTime2;
                        int i7 = i4;
                        future = future3;
                        i = i3;
                        i2 = i7;
                    } else {
                        int i8 = i4;
                        future = (Future) linkedBlockingQueueNewLinkedBlockingQueue.take();
                        i = i3;
                        i2 = i8;
                    }
                }
                if (future != null) {
                    i--;
                    try {
                        T t = (T) future.get();
                        Iterator it3 = arrayListNewArrayListWithCapacity.iterator();
                        while (it3.hasNext()) {
                            ((Future) it3.next()).cancel(true);
                        }
                        return t;
                    } catch (InterruptedException e2) {
                        throw e2;
                    } catch (ExecutionException e3) {
                        e = e3;
                    } catch (Exception e4) {
                        e = new ExecutionException(e4);
                    }
                } else {
                    e = executionException;
                }
                executionException = e;
                i4 = i2;
                i3 = i;
            } catch (Throwable th3) {
                th = th3;
                it = arrayListNewArrayListWithCapacity.iterator();
                while (it.hasNext()) {
                    ((Future) it.next()).cancel(true);
                }
                throw th;
            }
        }
    }

    private static boolean isAppEngineWithApiClasses() {
        if (System.getProperty("com.google.appengine.runtime.environment") == null) {
            return false;
        }
        try {
            Class.forName("com.google.appengine.api.utils.SystemProperty");
            try {
                return Class.forName("com.google.apphosting.api.ApiProxy").getMethod("getCurrentEnvironment", new Class[0]).invoke(null, new Object[0]) != null;
            } catch (ClassNotFoundException e) {
                return false;
            } catch (IllegalAccessException e2) {
                return false;
            } catch (NoSuchMethodException e3) {
                return false;
            } catch (InvocationTargetException e4) {
                return false;
            }
        } catch (ClassNotFoundException e5) {
            return false;
        }
    }

    public static ListeningExecutorService listeningDecorator(ExecutorService executorService) {
        if (executorService instanceof ListeningExecutorService) {
            return (ListeningExecutorService) executorService;
        }
        return executorService instanceof ScheduledExecutorService ? new ScheduledListeningDecorator((ScheduledExecutorService) executorService) : new ListeningDecorator(executorService);
    }

    public static ListeningScheduledExecutorService listeningDecorator(ScheduledExecutorService scheduledExecutorService) {
        return scheduledExecutorService instanceof ListeningScheduledExecutorService ? (ListeningScheduledExecutorService) scheduledExecutorService : new ScheduledListeningDecorator(scheduledExecutorService);
    }

    public static ListeningExecutorService newDirectExecutorService() {
        return new DirectExecutorService();
    }

    public static Executor newSequentialExecutor(Executor executor) {
        return new SequentialExecutor(executor);
    }

    static Thread newThread(String str, Runnable runnable) {
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(runnable);
        Thread thread = (Thread) Objects.requireNonNull(platformThreadFactory().newThread(runnable));
        try {
            thread.setName(str);
        } catch (SecurityException e) {
        }
        return thread;
    }

    public static ThreadFactory platformThreadFactory() {
        if (!isAppEngineWithApiClasses()) {
            return Executors.defaultThreadFactory();
        }
        try {
            return (ThreadFactory) Class.forName("com.google.appengine.api.ThreadManager").getMethod("currentRequestThreadFactory", new Class[0]).invoke(null, new Object[0]);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Couldn't invoke ThreadManager.currentRequestThreadFactory", e);
        } catch (IllegalAccessException e2) {
            throw new RuntimeException("Couldn't invoke ThreadManager.currentRequestThreadFactory", e2);
        } catch (NoSuchMethodException e3) {
            throw new RuntimeException("Couldn't invoke ThreadManager.currentRequestThreadFactory", e3);
        } catch (InvocationTargetException e4) {
            throw Throwables.propagate(e4.getCause());
        }
    }

    static Executor rejectionPropagatingExecutor(Executor executor, AbstractFuture<?> abstractFuture) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(abstractFuture);
        return executor == directExecutor() ? executor : new Executor(executor, abstractFuture) { // from class: com.google.common.util.concurrent.MoreExecutors.5
            final Executor val$delegate;
            final AbstractFuture val$future;

            {
                this.val$delegate = executor;
                this.val$future = abstractFuture;
            }

            @Override // java.util.concurrent.Executor
            public void execute(Runnable runnable) {
                try {
                    this.val$delegate.execute(runnable);
                } catch (RejectedExecutionException e) {
                    this.val$future.setException(e);
                }
            }
        };
    }

    static Executor renamingDecorator(Executor executor, Supplier<String> supplier) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(supplier);
        return new Executor(executor, supplier) { // from class: com.google.common.util.concurrent.MoreExecutors.2
            final Executor val$executor;
            final Supplier val$nameSupplier;

            {
                this.val$executor = executor;
                this.val$nameSupplier = supplier;
            }

            @Override // java.util.concurrent.Executor
            public void execute(Runnable runnable) {
                this.val$executor.execute(Callables.threadRenaming(runnable, (Supplier<String>) this.val$nameSupplier));
            }
        };
    }

    static ExecutorService renamingDecorator(ExecutorService executorService, Supplier<String> supplier) {
        Preconditions.checkNotNull(executorService);
        Preconditions.checkNotNull(supplier);
        return new WrappingExecutorService(executorService, supplier) { // from class: com.google.common.util.concurrent.MoreExecutors.3
            final Supplier val$nameSupplier;

            {
                this.val$nameSupplier = supplier;
            }

            @Override // com.google.common.util.concurrent.WrappingExecutorService
            protected Runnable wrapTask(Runnable runnable) {
                return Callables.threadRenaming(runnable, (Supplier<String>) this.val$nameSupplier);
            }

            @Override // com.google.common.util.concurrent.WrappingExecutorService
            protected <T> Callable<T> wrapTask(Callable<T> callable) {
                return Callables.threadRenaming(callable, (Supplier<String>) this.val$nameSupplier);
            }
        };
    }

    static ScheduledExecutorService renamingDecorator(ScheduledExecutorService scheduledExecutorService, Supplier<String> supplier) {
        Preconditions.checkNotNull(scheduledExecutorService);
        Preconditions.checkNotNull(supplier);
        return new WrappingScheduledExecutorService(scheduledExecutorService, supplier) { // from class: com.google.common.util.concurrent.MoreExecutors.4
            final Supplier val$nameSupplier;

            {
                this.val$nameSupplier = supplier;
            }

            @Override // com.google.common.util.concurrent.WrappingExecutorService
            protected Runnable wrapTask(Runnable runnable) {
                return Callables.threadRenaming(runnable, (Supplier<String>) this.val$nameSupplier);
            }

            @Override // com.google.common.util.concurrent.WrappingExecutorService
            protected <T> Callable<T> wrapTask(Callable<T> callable) {
                return Callables.threadRenaming(callable, (Supplier<String>) this.val$nameSupplier);
            }
        };
    }

    public static boolean shutdownAndAwaitTermination(ExecutorService executorService, long j, TimeUnit timeUnit) {
        long nanos = timeUnit.toNanos(j) / 2;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(nanos, TimeUnit.NANOSECONDS)) {
                executorService.shutdownNow();
                executorService.awaitTermination(nanos, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
        return executorService.isTerminated();
    }

    private static <T> ListenableFuture<T> submitAndAddQueueListener(ListeningExecutorService listeningExecutorService, Callable<T> callable, BlockingQueue<Future<T>> blockingQueue) {
        ListenableFuture<T> listenableFutureSubmit = listeningExecutorService.submit((Callable) callable);
        listenableFutureSubmit.addListener(new Runnable(blockingQueue, listenableFutureSubmit) { // from class: com.google.common.util.concurrent.MoreExecutors.1
            final ListenableFuture val$future;
            final BlockingQueue val$queue;

            {
                this.val$queue = blockingQueue;
                this.val$future = listenableFutureSubmit;
            }

            @Override // java.lang.Runnable
            public void run() {
                this.val$queue.add(this.val$future);
            }
        }, directExecutor());
        return listenableFutureSubmit;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void useDaemonThreadFactory(ThreadPoolExecutor threadPoolExecutor) {
        threadPoolExecutor.setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).setThreadFactory(threadPoolExecutor.getThreadFactory()).build());
    }
}
