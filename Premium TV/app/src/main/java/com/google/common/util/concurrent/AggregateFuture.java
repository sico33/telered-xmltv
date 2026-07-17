package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class AggregateFuture<InputT, OutputT> extends AggregateFutureState<OutputT> {
    private static final LazyLogger logger = new LazyLogger(AggregateFuture.class);
    private final boolean allMustSucceed;
    private final boolean collectsValues;

    @CheckForNull
    private ImmutableCollection<? extends ListenableFuture<? extends InputT>> futures;

    enum ReleaseResourcesReason {
        OUTPUT_FUTURE_DONE,
        ALL_INPUT_FUTURES_PROCESSED
    }

    AggregateFuture(ImmutableCollection<? extends ListenableFuture<? extends InputT>> immutableCollection, boolean z, boolean z2) {
        super(immutableCollection.size());
        this.futures = (ImmutableCollection) Preconditions.checkNotNull(immutableCollection);
        this.allMustSucceed = z;
        this.collectsValues = z2;
    }

    private static boolean addCausalChain(Set<Throwable> set, Throwable th) {
        while (th != null) {
            if (!set.add(th)) {
                return false;
            }
            th = th.getCause();
        }
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void collectValueFromNonCancelledFuture(int i, Future<? extends InputT> future) {
        try {
            collectOneValue(i, Futures.getDone(future));
        } catch (ExecutionException e) {
            handleException(e.getCause());
        } catch (Throwable th) {
            handleException(th);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: decrementCountAndMaybeComplete, reason: merged with bridge method [inline-methods] */
    public void m209lambda$init$1$comgooglecommonutilconcurrentAggregateFuture(@CheckForNull ImmutableCollection<? extends Future<? extends InputT>> immutableCollection) {
        int iDecrementRemainingAndGet = decrementRemainingAndGet();
        Preconditions.checkState(iDecrementRemainingAndGet >= 0, "Less than 0 remaining futures");
        if (iDecrementRemainingAndGet == 0) {
            processCompleted(immutableCollection);
        }
    }

    private void handleException(Throwable th) {
        Preconditions.checkNotNull(th);
        if (this.allMustSucceed && !setException(th) && addCausalChain(getOrInitSeenExceptions(), th)) {
            log(th);
        } else if (th instanceof Error) {
            log(th);
        }
    }

    private static void log(Throwable th) {
        logger.get().log(Level.SEVERE, th instanceof Error ? "Input Future failed with Error" : "Got more than one input Future failure. Logging failures after the first", th);
    }

    private void processCompleted(@CheckForNull ImmutableCollection<? extends Future<? extends InputT>> immutableCollection) {
        if (immutableCollection != null) {
            int i = 0;
            UnmodifiableIterator<? extends Future<? extends InputT>> it = immutableCollection.iterator();
            while (true) {
                int i2 = i;
                if (!it.hasNext()) {
                    break;
                }
                Future<? extends InputT> next = it.next();
                if (!next.isCancelled()) {
                    collectValueFromNonCancelledFuture(i2, next);
                }
                i = i2 + 1;
            }
        }
        clearSeenExceptions();
        handleAllCompleted();
        releaseResources(ReleaseResourcesReason.ALL_INPUT_FUTURES_PROCESSED);
    }

    @Override // com.google.common.util.concurrent.AggregateFutureState
    final void addInitialException(Set<Throwable> set) {
        Preconditions.checkNotNull(set);
        if (isCancelled()) {
            return;
        }
        addCausalChain(set, (Throwable) Objects.requireNonNull(tryInternalFastPathGetFailure()));
    }

    @Override // com.google.common.util.concurrent.AbstractFuture
    protected final void afterDone() {
        super.afterDone();
        ImmutableCollection<? extends ListenableFuture<? extends InputT>> immutableCollection = this.futures;
        releaseResources(ReleaseResourcesReason.OUTPUT_FUTURE_DONE);
        if ((immutableCollection != null) && isCancelled()) {
            boolean zWasInterrupted = wasInterrupted();
            UnmodifiableIterator<? extends ListenableFuture<? extends InputT>> it = immutableCollection.iterator();
            while (it.hasNext()) {
                it.next().cancel(zWasInterrupted);
            }
        }
    }

    abstract void collectOneValue(int i, @ParametricNullness InputT inputt);

    abstract void handleAllCompleted();

    final void init() {
        Objects.requireNonNull(this.futures);
        if (this.futures.isEmpty()) {
            handleAllCompleted();
            return;
        }
        if (!this.allMustSucceed) {
            final ImmutableCollection<? extends ListenableFuture<? extends InputT>> immutableCollection = this.collectsValues ? this.futures : null;
            Runnable runnable = new Runnable(this, immutableCollection) { // from class: com.google.common.util.concurrent.AggregateFuture$$ExternalSyntheticLambda1
                public final AggregateFuture f$0;
                public final ImmutableCollection f$1;

                {
                    this.f$0 = this;
                    this.f$1 = immutableCollection;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m209lambda$init$1$comgooglecommonutilconcurrentAggregateFuture(this.f$1);
                }
            };
            UnmodifiableIterator<? extends ListenableFuture<? extends InputT>> it = this.futures.iterator();
            while (it.hasNext()) {
                it.next().addListener(runnable, MoreExecutors.directExecutor());
            }
            return;
        }
        int i = 0;
        UnmodifiableIterator<? extends ListenableFuture<? extends InputT>> it2 = this.futures.iterator();
        while (true) {
            final int i2 = i;
            if (!it2.hasNext()) {
                return;
            }
            final ListenableFuture<? extends InputT> next = it2.next();
            next.addListener(new Runnable(this, next, i2) { // from class: com.google.common.util.concurrent.AggregateFuture$$ExternalSyntheticLambda0
                public final AggregateFuture f$0;
                public final ListenableFuture f$1;
                public final int f$2;

                {
                    this.f$0 = this;
                    this.f$1 = next;
                    this.f$2 = i2;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m208lambda$init$0$comgooglecommonutilconcurrentAggregateFuture(this.f$1, this.f$2);
                }
            }, MoreExecutors.directExecutor());
            i = i2 + 1;
        }
    }

    /* JADX INFO: renamed from: lambda$init$0$com-google-common-util-concurrent-AggregateFuture, reason: not valid java name */
    /* synthetic */ void m208lambda$init$0$comgooglecommonutilconcurrentAggregateFuture(ListenableFuture listenableFuture, int i) {
        try {
            if (listenableFuture.isCancelled()) {
                this.futures = null;
                cancel(false);
            } else {
                collectValueFromNonCancelledFuture(i, listenableFuture);
            }
        } finally {
            m209lambda$init$1$comgooglecommonutilconcurrentAggregateFuture(null);
        }
    }

    @Override // com.google.common.util.concurrent.AbstractFuture
    @CheckForNull
    protected final String pendingToString() {
        ImmutableCollection<? extends ListenableFuture<? extends InputT>> immutableCollection = this.futures;
        return immutableCollection != null ? "futures=" + immutableCollection : super.pendingToString();
    }

    void releaseResources(ReleaseResourcesReason releaseResourcesReason) {
        Preconditions.checkNotNull(releaseResourcesReason);
        this.futures = null;
    }
}
