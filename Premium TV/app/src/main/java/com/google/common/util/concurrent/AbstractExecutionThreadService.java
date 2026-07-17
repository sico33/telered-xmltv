package com.google.common.util.concurrent;

import com.google.common.base.Supplier;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class AbstractExecutionThreadService implements Service {
    private static final LazyLogger logger = new LazyLogger(AbstractExecutionThreadService.class);
    private final Service delegate = new AnonymousClass1(this);

    /* JADX INFO: renamed from: com.google.common.util.concurrent.AbstractExecutionThreadService$1, reason: invalid class name */
    class AnonymousClass1 extends AbstractService {
        final AbstractExecutionThreadService this$0;

        AnonymousClass1(AbstractExecutionThreadService abstractExecutionThreadService) {
            this.this$0 = abstractExecutionThreadService;
        }

        @Override // com.google.common.util.concurrent.AbstractService
        protected final void doStart() {
            MoreExecutors.renamingDecorator(this.this$0.executor(), (Supplier<String>) new Supplier(this) { // from class: com.google.common.util.concurrent.AbstractExecutionThreadService$1$$ExternalSyntheticLambda0
                public final AbstractExecutionThreadService.AnonymousClass1 f$0;

                {
                    this.f$0 = this;
                }

                @Override // com.google.common.base.Supplier
                public final Object get() {
                    return this.f$0.m200xa0f821c5();
                }
            }).execute(new Runnable(this) { // from class: com.google.common.util.concurrent.AbstractExecutionThreadService$1$$ExternalSyntheticLambda1
                public final AbstractExecutionThreadService.AnonymousClass1 f$0;

                {
                    this.f$0 = this;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m201x3d661e24();
                }
            });
        }

        @Override // com.google.common.util.concurrent.AbstractService
        protected void doStop() {
            this.this$0.triggerShutdown();
        }

        /* JADX INFO: renamed from: lambda$doStart$0$com-google-common-util-concurrent-AbstractExecutionThreadService$1, reason: not valid java name */
        /* synthetic */ String m200xa0f821c5() {
            return this.this$0.serviceName();
        }

        /* JADX INFO: renamed from: lambda$doStart$1$com-google-common-util-concurrent-AbstractExecutionThreadService$1, reason: not valid java name */
        /* synthetic */ void m201x3d661e24() {
            try {
                this.this$0.startUp();
                notifyStarted();
                if (isRunning()) {
                    try {
                        this.this$0.run();
                    } catch (Throwable th) {
                        Platform.restoreInterruptIfIsInterruptedException(th);
                        try {
                            this.this$0.shutDown();
                        } catch (Exception e) {
                            Platform.restoreInterruptIfIsInterruptedException(e);
                            AbstractExecutionThreadService.logger.get().log(Level.WARNING, "Error while attempting to shut down the service after failure.", (Throwable) e);
                        }
                        notifyFailed(th);
                        return;
                    }
                }
                this.this$0.shutDown();
                notifyStopped();
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

    protected AbstractExecutionThreadService() {
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

    protected Executor executor() {
        return new Executor(this) { // from class: com.google.common.util.concurrent.AbstractExecutionThreadService$$ExternalSyntheticLambda0
            public final AbstractExecutionThreadService f$0;

            {
                this.f$0 = this;
            }

            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                this.f$0.m199xafeb5522(runnable);
            }
        };
    }

    @Override // com.google.common.util.concurrent.Service
    public final Throwable failureCause() {
        return this.delegate.failureCause();
    }

    @Override // com.google.common.util.concurrent.Service
    public final boolean isRunning() {
        return this.delegate.isRunning();
    }

    /* JADX INFO: renamed from: lambda$executor$0$com-google-common-util-concurrent-AbstractExecutionThreadService, reason: not valid java name */
    /* synthetic */ void m199xafeb5522(Runnable runnable) {
        MoreExecutors.newThread(serviceName(), runnable).start();
    }

    protected abstract void run() throws Exception;

    protected String serviceName() {
        return getClass().getSimpleName();
    }

    protected void shutDown() throws Exception {
    }

    @Override // com.google.common.util.concurrent.Service
    public final Service startAsync() {
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
    public final Service stopAsync() {
        this.delegate.stopAsync();
        return this;
    }

    public String toString() {
        return serviceName() + " [" + state() + "]";
    }

    protected void triggerShutdown() {
    }
}
