package com.google.common.util.concurrent;

import com.google.common.base.Supplier;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class AbstractIdleService implements Service {
    private final Service delegate;
    private final Supplier<String> threadNameSupplier;

    /* JADX INFO: Access modifiers changed from: private */
    final class DelegateService extends AbstractService {
        final AbstractIdleService this$0;

        private DelegateService(AbstractIdleService abstractIdleService) {
            this.this$0 = abstractIdleService;
        }

        @Override // com.google.common.util.concurrent.AbstractService
        protected final void doStart() {
            MoreExecutors.renamingDecorator(this.this$0.executor(), (Supplier<String>) this.this$0.threadNameSupplier).execute(new Runnable(this) { // from class: com.google.common.util.concurrent.AbstractIdleService$DelegateService$$ExternalSyntheticLambda1
                public final AbstractIdleService.DelegateService f$0;

                {
                    this.f$0 = this;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m203x2ed323e8();
                }
            });
        }

        @Override // com.google.common.util.concurrent.AbstractService
        protected final void doStop() {
            MoreExecutors.renamingDecorator(this.this$0.executor(), (Supplier<String>) this.this$0.threadNameSupplier).execute(new Runnable(this) { // from class: com.google.common.util.concurrent.AbstractIdleService$DelegateService$$ExternalSyntheticLambda0
                public final AbstractIdleService.DelegateService f$0;

                {
                    this.f$0 = this;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m204xb13e6319();
                }
            });
        }

        /* JADX INFO: renamed from: lambda$doStart$0$com-google-common-util-concurrent-AbstractIdleService$DelegateService, reason: not valid java name */
        /* synthetic */ void m203x2ed323e8() {
            try {
                this.this$0.startUp();
                notifyStarted();
            } catch (Throwable th) {
                Platform.restoreInterruptIfIsInterruptedException(th);
                notifyFailed(th);
            }
        }

        /* JADX INFO: renamed from: lambda$doStop$1$com-google-common-util-concurrent-AbstractIdleService$DelegateService, reason: not valid java name */
        /* synthetic */ void m204xb13e6319() {
            try {
                this.this$0.shutDown();
                notifyStopped();
            } catch (Throwable th) {
                Platform.restoreInterruptIfIsInterruptedException(th);
                notifyFailed(th);
            }
        }

        @Override // com.google.common.util.concurrent.AbstractService
        public String toString() {
            return this.this$0.toString();
        }
    }

    private final class ThreadNameSupplier implements Supplier<String> {
        final AbstractIdleService this$0;

        private ThreadNameSupplier(AbstractIdleService abstractIdleService) {
            this.this$0 = abstractIdleService;
        }

        @Override // com.google.common.base.Supplier
        public String get() {
            return this.this$0.serviceName() + " " + this.this$0.state();
        }
    }

    protected AbstractIdleService() {
        this.threadNameSupplier = new ThreadNameSupplier();
        this.delegate = new DelegateService();
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
        return new Executor(this) { // from class: com.google.common.util.concurrent.AbstractIdleService$$ExternalSyntheticLambda0
            public final AbstractIdleService f$0;

            {
                this.f$0 = this;
            }

            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                this.f$0.m202xc998c392(runnable);
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

    /* JADX INFO: renamed from: lambda$executor$0$com-google-common-util-concurrent-AbstractIdleService, reason: not valid java name */
    /* synthetic */ void m202xc998c392(Runnable runnable) {
        MoreExecutors.newThread(this.threadNameSupplier.get(), runnable).start();
    }

    protected String serviceName() {
        return getClass().getSimpleName();
    }

    protected abstract void shutDown() throws Exception;

    @Override // com.google.common.util.concurrent.Service
    public final Service startAsync() {
        this.delegate.startAsync();
        return this;
    }

    protected abstract void startUp() throws Exception;

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
}
