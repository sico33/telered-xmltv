package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class AbstractScheduledService implements Service {
    private static final LazyLogger logger = new LazyLogger(AbstractScheduledService.class);
    private final AbstractService delegate = new ServiceDelegate();

    interface Cancellable {
        void cancel(boolean z);

        boolean isCancelled();
    }

    public static abstract class CustomScheduler extends Scheduler {

        private final class ReschedulableCallable implements Callable<Void> {

            @CheckForNull
            private SupplantableFuture cancellationDelegate;
            private final ScheduledExecutorService executor;
            private final ReentrantLock lock = new ReentrantLock();
            private final AbstractService service;
            final CustomScheduler this$0;
            private final Runnable wrappedRunnable;

            ReschedulableCallable(CustomScheduler customScheduler, AbstractService abstractService, ScheduledExecutorService scheduledExecutorService, Runnable runnable) {
                this.this$0 = customScheduler;
                this.wrappedRunnable = runnable;
                this.executor = scheduledExecutorService;
                this.service = abstractService;
            }

            private Cancellable initializeOrUpdateCancellationDelegate(Schedule schedule) {
                if (this.cancellationDelegate == null) {
                    SupplantableFuture supplantableFuture = new SupplantableFuture(this.lock, submitToExecutor(schedule));
                    this.cancellationDelegate = supplantableFuture;
                    return supplantableFuture;
                }
                if (!this.cancellationDelegate.currentFuture.isCancelled()) {
                    this.cancellationDelegate.currentFuture = submitToExecutor(schedule);
                }
                return this.cancellationDelegate;
            }

            private ScheduledFuture<Void> submitToExecutor(Schedule schedule) {
                return this.executor.schedule(this, schedule.delay, schedule.unit);
            }

            @Override // java.util.concurrent.Callable
            @CheckForNull
            public Void call() throws Exception {
                this.wrappedRunnable.run();
                reschedule();
                return null;
            }

            public Cancellable reschedule() throws Exception {
                Cancellable futureAsCancellable;
                try {
                    Schedule nextSchedule = this.this$0.getNextSchedule();
                    Throwable th = null;
                    this.lock.lock();
                    try {
                        futureAsCancellable = initializeOrUpdateCancellationDelegate(nextSchedule);
                    } catch (Throwable th2) {
                        th = th2;
                        try {
                            futureAsCancellable = new FutureAsCancellable(Futures.immediateCancelledFuture());
                        } catch (Throwable th3) {
                            this.lock.unlock();
                            throw th3;
                        }
                    }
                    this.lock.unlock();
                    if (th == null) {
                        return futureAsCancellable;
                    }
                    this.service.notifyFailed(th);
                    return futureAsCancellable;
                } catch (Throwable th4) {
                    Platform.restoreInterruptIfIsInterruptedException(th4);
                    this.service.notifyFailed(th4);
                    return new FutureAsCancellable(Futures.immediateCancelledFuture());
                }
            }
        }

        protected static final class Schedule {
            private final long delay;
            private final TimeUnit unit;

            public Schedule(long j, TimeUnit timeUnit) {
                this.delay = j;
                this.unit = (TimeUnit) Preconditions.checkNotNull(timeUnit);
            }
        }

        private static final class SupplantableFuture implements Cancellable {
            private Future<Void> currentFuture;
            private final ReentrantLock lock;

            SupplantableFuture(ReentrantLock reentrantLock, Future<Void> future) {
                this.lock = reentrantLock;
                this.currentFuture = future;
            }

            @Override // com.google.common.util.concurrent.AbstractScheduledService.Cancellable
            public void cancel(boolean z) {
                this.lock.lock();
                try {
                    this.currentFuture.cancel(z);
                } finally {
                    this.lock.unlock();
                }
            }

            @Override // com.google.common.util.concurrent.AbstractScheduledService.Cancellable
            public boolean isCancelled() {
                this.lock.lock();
                try {
                    return this.currentFuture.isCancelled();
                } finally {
                    this.lock.unlock();
                }
            }
        }

        public CustomScheduler() {
            super();
        }

        protected abstract Schedule getNextSchedule() throws Exception;

        @Override // com.google.common.util.concurrent.AbstractScheduledService.Scheduler
        final Cancellable schedule(AbstractService abstractService, ScheduledExecutorService scheduledExecutorService, Runnable runnable) {
            return new ReschedulableCallable(this, abstractService, scheduledExecutorService, runnable).reschedule();
        }
    }

    private static final class FutureAsCancellable implements Cancellable {
        private final Future<?> delegate;

        FutureAsCancellable(Future<?> future) {
            this.delegate = future;
        }

        @Override // com.google.common.util.concurrent.AbstractScheduledService.Cancellable
        public void cancel(boolean z) {
            this.delegate.cancel(z);
        }

        @Override // com.google.common.util.concurrent.AbstractScheduledService.Cancellable
        public boolean isCancelled() {
            return this.delegate.isCancelled();
        }
    }

    public static abstract class Scheduler {
        private Scheduler() {
        }

        public static Scheduler newFixedDelaySchedule(long j, long j2, TimeUnit timeUnit) {
            Preconditions.checkNotNull(timeUnit);
            Preconditions.checkArgument(j2 > 0, "delay must be > 0, found %s", j2);
            return new Scheduler(j, j2, timeUnit) { // from class: com.google.common.util.concurrent.AbstractScheduledService.Scheduler.1
                final long val$delay;
                final long val$initialDelay;
                final TimeUnit val$unit;

                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                {
                    super();
                    this.val$initialDelay = j;
                    this.val$delay = j2;
                    this.val$unit = timeUnit;
                }

                @Override // com.google.common.util.concurrent.AbstractScheduledService.Scheduler
                public Cancellable schedule(AbstractService abstractService, ScheduledExecutorService scheduledExecutorService, Runnable runnable) {
                    return new FutureAsCancellable(scheduledExecutorService.scheduleWithFixedDelay(runnable, this.val$initialDelay, this.val$delay, this.val$unit));
                }
            };
        }

        public static Scheduler newFixedRateSchedule(long j, long j2, TimeUnit timeUnit) {
            Preconditions.checkNotNull(timeUnit);
            Preconditions.checkArgument(j2 > 0, "period must be > 0, found %s", j2);
            return new Scheduler(j, j2, timeUnit) { // from class: com.google.common.util.concurrent.AbstractScheduledService.Scheduler.2
                final long val$initialDelay;
                final long val$period;
                final TimeUnit val$unit;

                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                {
                    super();
                    this.val$initialDelay = j;
                    this.val$period = j2;
                    this.val$unit = timeUnit;
                }

                @Override // com.google.common.util.concurrent.AbstractScheduledService.Scheduler
                public Cancellable schedule(AbstractService abstractService, ScheduledExecutorService scheduledExecutorService, Runnable runnable) {
                    return new FutureAsCancellable(scheduledExecutorService.scheduleAtFixedRate(runnable, this.val$initialDelay, this.val$period, this.val$unit));
                }
            };
        }

        abstract Cancellable schedule(AbstractService abstractService, ScheduledExecutorService scheduledExecutorService, Runnable runnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class ServiceDelegate extends AbstractService {

        @CheckForNull
        private volatile ScheduledExecutorService executorService;
        private final ReentrantLock lock;

        @CheckForNull
        private volatile Cancellable runningTask;
        private final Runnable task;
        final AbstractScheduledService this$0;

        class Task implements Runnable {
            final ServiceDelegate this$1;

            Task(ServiceDelegate serviceDelegate) {
                this.this$1 = serviceDelegate;
            }

            @Override // java.lang.Runnable
            public void run() {
                this.this$1.lock.lock();
                try {
                    boolean zIsCancelled = ((Cancellable) Objects.requireNonNull(this.this$1.runningTask)).isCancelled();
                    ServiceDelegate serviceDelegate = this.this$1;
                    if (zIsCancelled) {
                        serviceDelegate.lock.unlock();
                        return;
                    }
                    serviceDelegate.this$0.runOneIteration();
                } catch (Throwable th) {
                    try {
                        Platform.restoreInterruptIfIsInterruptedException(th);
                        try {
                            this.this$1.this$0.shutDown();
                        } catch (Exception e) {
                            Platform.restoreInterruptIfIsInterruptedException(e);
                            AbstractScheduledService.logger.get().log(Level.WARNING, "Error while attempting to shut down the service after failure.", (Throwable) e);
                        }
                        this.this$1.notifyFailed(th);
                        ((Cancellable) Objects.requireNonNull(this.this$1.runningTask)).cancel(false);
                    } catch (Throwable th2) {
                        this.this$1.lock.unlock();
                        throw th2;
                    }
                }
                this.this$1.lock.unlock();
            }
        }

        private ServiceDelegate(AbstractScheduledService abstractScheduledService) {
            this.this$0 = abstractScheduledService;
            this.lock = new ReentrantLock();
            this.task = new Task(this);
        }

        @Override // com.google.common.util.concurrent.AbstractService
        protected final void doStart() {
            this.executorService = MoreExecutors.renamingDecorator(this.this$0.executor(), (Supplier<String>) new Supplier(this) { // from class: com.google.common.util.concurrent.AbstractScheduledService$ServiceDelegate$$ExternalSyntheticLambda0
                public final AbstractScheduledService.ServiceDelegate f$0;

                {
                    this.f$0 = this;
                }

                @Override // com.google.common.base.Supplier
                public final Object get() {
                    return this.f$0.m205xcd8af3c3();
                }
            });
            this.executorService.execute(new Runnable(this) { // from class: com.google.common.util.concurrent.AbstractScheduledService$ServiceDelegate$$ExternalSyntheticLambda1
                public final AbstractScheduledService.ServiceDelegate f$0;

                {
                    this.f$0 = this;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m206xfa22122();
                }
            });
        }

        @Override // com.google.common.util.concurrent.AbstractService
        protected final void doStop() {
            Objects.requireNonNull(this.runningTask);
            Objects.requireNonNull(this.executorService);
            this.runningTask.cancel(false);
            this.executorService.execute(new Runnable(this) { // from class: com.google.common.util.concurrent.AbstractScheduledService$ServiceDelegate$$ExternalSyntheticLambda2
                public final AbstractScheduledService.ServiceDelegate f$0;

                {
                    this.f$0 = this;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m207x2d03b891();
                }
            });
        }

        /* JADX INFO: renamed from: lambda$doStart$0$com-google-common-util-concurrent-AbstractScheduledService$ServiceDelegate, reason: not valid java name */
        /* synthetic */ String m205xcd8af3c3() {
            return this.this$0.serviceName() + " " + state();
        }

        /* JADX INFO: renamed from: lambda$doStart$1$com-google-common-util-concurrent-AbstractScheduledService$ServiceDelegate, reason: not valid java name */
        /* synthetic */ void m206xfa22122() {
            this.lock.lock();
            try {
                this.this$0.startUp();
                Objects.requireNonNull(this.executorService);
                this.runningTask = this.this$0.scheduler().schedule(this.this$0.delegate, this.executorService, this.task);
                notifyStarted();
            } catch (Throwable th) {
                try {
                    Platform.restoreInterruptIfIsInterruptedException(th);
                    notifyFailed(th);
                    if (this.runningTask != null) {
                        this.runningTask.cancel(false);
                    }
                } finally {
                    this.lock.unlock();
                }
            }
        }

        /* JADX INFO: renamed from: lambda$doStop$2$com-google-common-util-concurrent-AbstractScheduledService$ServiceDelegate, reason: not valid java name */
        /* synthetic */ void m207x2d03b891() {
            try {
                this.lock.lock();
                try {
                    if (state() != Service.State.STOPPING) {
                        this.lock.unlock();
                        return;
                    }
                    this.this$0.shutDown();
                    this.lock.unlock();
                    notifyStopped();
                } catch (Throwable th) {
                    this.lock.unlock();
                    throw th;
                }
            } catch (Throwable th2) {
                Platform.restoreInterruptIfIsInterruptedException(th2);
                notifyFailed(th2);
            }
        }

        @Override // com.google.common.util.concurrent.AbstractService
        public String toString() {
            return this.this$0.toString();
        }
    }

    protected AbstractScheduledService() {
    }

    @Override // com.google.common.util.concurrent.Service
    public final void addListener(Service.Listener listener, Executor executor) {
        this.delegate.addListener(listener, executor);
    }

    @Override // com.google.common.util.concurrent.Service
    public final void awaitRunning() {
        this.delegate.awaitRunning();
    }

    @Override // com.google.common.util.concurrent.Service
    public final void awaitRunning(long j, TimeUnit timeUnit) throws TimeoutException {
        this.delegate.awaitRunning(j, timeUnit);
    }

    @Override // com.google.common.util.concurrent.Service
    public final void awaitTerminated() {
        this.delegate.awaitTerminated();
    }

    @Override // com.google.common.util.concurrent.Service
    public final void awaitTerminated(long j, TimeUnit timeUnit) throws TimeoutException {
        this.delegate.awaitTerminated(j, timeUnit);
    }

    protected ScheduledExecutorService executor() {
        ScheduledExecutorService scheduledExecutorServiceNewSingleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory(this) { // from class: com.google.common.util.concurrent.AbstractScheduledService.1ThreadFactoryImpl
            final AbstractScheduledService this$0;

            {
                this.this$0 = this;
            }

            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(Runnable runnable) {
                return MoreExecutors.newThread(this.this$0.serviceName(), runnable);
            }
        });
        addListener(new Service.Listener(this, scheduledExecutorServiceNewSingleThreadScheduledExecutor) { // from class: com.google.common.util.concurrent.AbstractScheduledService.1
            final ScheduledExecutorService val$executor;

            {
                this.val$executor = scheduledExecutorServiceNewSingleThreadScheduledExecutor;
            }

            @Override // com.google.common.util.concurrent.Service.Listener
            public void failed(Service.State state, Throwable th) {
                this.val$executor.shutdown();
            }

            @Override // com.google.common.util.concurrent.Service.Listener
            public void terminated(Service.State state) {
                this.val$executor.shutdown();
            }
        }, MoreExecutors.directExecutor());
        return scheduledExecutorServiceNewSingleThreadScheduledExecutor;
    }

    @Override // com.google.common.util.concurrent.Service
    public final Throwable failureCause() {
        return this.delegate.failureCause();
    }

    @Override // com.google.common.util.concurrent.Service
    public final boolean isRunning() {
        return this.delegate.isRunning();
    }

    protected abstract void runOneIteration() throws Exception;

    protected abstract Scheduler scheduler();

    protected String serviceName() {
        return getClass().getSimpleName();
    }

    protected void shutDown() throws Exception {
    }

    @Override // com.google.common.util.concurrent.Service
    public final Service startAsync() throws Exception {
        this.delegate.startAsync();
        return this;
    }

    protected void startUp() throws Exception {
    }

    @Override // com.google.common.util.concurrent.Service
    public final Service.State state() {
        return this.delegate.state();
    }

    @Override // com.google.common.util.concurrent.Service
    public final Service stopAsync() throws Exception {
        this.delegate.stopAsync();
        return this;
    }

    public String toString() {
        return serviceName() + " [" + state() + "]";
    }
}
