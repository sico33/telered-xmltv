package com.google.common.util.concurrent;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.internal.InternalFutureFailureAccess;
import com.google.common.util.concurrent.internal.InternalFutures;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Futures extends GwtFuturesCatchingSpecialization {

    private static final class CallbackListener<V> implements Runnable {
        final FutureCallback<? super V> callback;
        final Future<V> future;

        CallbackListener(Future<V> future, FutureCallback<? super V> futureCallback) {
            this.future = future;
            this.callback = futureCallback;
        }

        @Override // java.lang.Runnable
        public void run() {
            Throwable thTryInternalFastPathGetFailure;
            if ((this.future instanceof InternalFutureFailureAccess) && (thTryInternalFastPathGetFailure = InternalFutures.tryInternalFastPathGetFailure((InternalFutureFailureAccess) this.future)) != null) {
                this.callback.onFailure(thTryInternalFastPathGetFailure);
                return;
            }
            try {
                this.callback.onSuccess(Futures.getDone(this.future));
            } catch (ExecutionException e) {
                this.callback.onFailure(e.getCause());
            } catch (Throwable th) {
                this.callback.onFailure(th);
            }
        }

        public String toString() {
            return MoreObjects.toStringHelper(this).addValue(this.callback).toString();
        }
    }

    public static final class FutureCombiner<V> {
        private final boolean allMustSucceed;
        private final ImmutableList<ListenableFuture<? extends V>> futures;

        private FutureCombiner(boolean z, ImmutableList<ListenableFuture<? extends V>> immutableList) {
            this.allMustSucceed = z;
            this.futures = immutableList;
        }

        public <C> ListenableFuture<C> call(Callable<C> callable, Executor executor) {
            return new CombinedFuture(this.futures, this.allMustSucceed, executor, callable);
        }

        public <C> ListenableFuture<C> callAsync(AsyncCallable<C> asyncCallable, Executor executor) {
            return new CombinedFuture(this.futures, this.allMustSucceed, executor, asyncCallable);
        }

        public ListenableFuture<?> run(Runnable runnable, Executor executor) {
            return call(new Callable<Void>(this, runnable) { // from class: com.google.common.util.concurrent.Futures.FutureCombiner.1
                final Runnable val$combiner;

                {
                    this.val$combiner = runnable;
                }

                @Override // java.util.concurrent.Callable
                @CheckForNull
                public Void call() throws Exception {
                    this.val$combiner.run();
                    return null;
                }
            }, executor);
        }
    }

    private static final class InCompletionOrderFuture<T> extends AbstractFuture<T> {

        @CheckForNull
        private InCompletionOrderState<T> state;

        private InCompletionOrderFuture(InCompletionOrderState<T> inCompletionOrderState) {
            this.state = inCompletionOrderState;
        }

        @Override // com.google.common.util.concurrent.AbstractFuture
        protected void afterDone() {
            this.state = null;
        }

        @Override // com.google.common.util.concurrent.AbstractFuture, java.util.concurrent.Future
        public boolean cancel(boolean z) {
            InCompletionOrderState<T> inCompletionOrderState = this.state;
            if (!super.cancel(z)) {
                return false;
            }
            ((InCompletionOrderState) Objects.requireNonNull(inCompletionOrderState)).recordOutputCancellation(z);
            return true;
        }

        @Override // com.google.common.util.concurrent.AbstractFuture
        @CheckForNull
        protected String pendingToString() {
            InCompletionOrderState<T> inCompletionOrderState = this.state;
            if (inCompletionOrderState != null) {
                return "inputCount=[" + ((InCompletionOrderState) inCompletionOrderState).inputFutures.length + "], remaining=[" + ((InCompletionOrderState) inCompletionOrderState).incompleteOutputCount.get() + "]";
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class InCompletionOrderState<T> {
        private volatile int delegateIndex;
        private final AtomicInteger incompleteOutputCount;
        private final ListenableFuture<? extends T>[] inputFutures;
        private boolean shouldInterrupt;
        private boolean wasCancelled;

        private InCompletionOrderState(ListenableFuture<? extends T>[] listenableFutureArr) {
            this.wasCancelled = false;
            this.shouldInterrupt = true;
            this.delegateIndex = 0;
            this.inputFutures = listenableFutureArr;
            this.incompleteOutputCount = new AtomicInteger(listenableFutureArr.length);
        }

        private void recordCompletion() {
            if (this.incompleteOutputCount.decrementAndGet() == 0 && this.wasCancelled) {
                for (ListenableFuture<? extends T> listenableFuture : this.inputFutures) {
                    if (listenableFuture != null) {
                        listenableFuture.cancel(this.shouldInterrupt);
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void recordInputCompletion(ImmutableList<AbstractFuture<T>> immutableList, int i) {
            ListenableFuture<? extends T> listenableFuture = (ListenableFuture) Objects.requireNonNull(this.inputFutures[i]);
            this.inputFutures[i] = null;
            int i2 = this.delegateIndex;
            while (true) {
                int i3 = i2;
                if (i3 >= immutableList.size()) {
                    this.delegateIndex = immutableList.size();
                    return;
                } else {
                    if (immutableList.get(i3).setFuture(listenableFuture)) {
                        recordCompletion();
                        this.delegateIndex = i3 + 1;
                        return;
                    }
                    i2 = i3 + 1;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void recordOutputCancellation(boolean z) {
            this.wasCancelled = true;
            if (!z) {
                this.shouldInterrupt = false;
            }
            recordCompletion();
        }
    }

    private static final class NonCancellationPropagatingFuture<V> extends AbstractFuture.TrustedFuture<V> implements Runnable {

        @CheckForNull
        private ListenableFuture<V> delegate;

        NonCancellationPropagatingFuture(ListenableFuture<V> listenableFuture) {
            this.delegate = listenableFuture;
        }

        @Override // com.google.common.util.concurrent.AbstractFuture
        protected void afterDone() {
            this.delegate = null;
        }

        @Override // com.google.common.util.concurrent.AbstractFuture
        @CheckForNull
        protected String pendingToString() {
            ListenableFuture<V> listenableFuture = this.delegate;
            if (listenableFuture != null) {
                return "delegate=[" + listenableFuture + "]";
            }
            return null;
        }

        @Override // java.lang.Runnable
        public void run() {
            ListenableFuture<V> listenableFuture = this.delegate;
            if (listenableFuture != null) {
                setFuture(listenableFuture);
            }
        }
    }

    private Futures() {
    }

    public static <V> void addCallback(ListenableFuture<V> listenableFuture, FutureCallback<? super V> futureCallback, Executor executor) {
        Preconditions.checkNotNull(futureCallback);
        listenableFuture.addListener(new CallbackListener(listenableFuture, futureCallback), executor);
    }

    public static <V> ListenableFuture<List<V>> allAsList(Iterable<? extends ListenableFuture<? extends V>> iterable) {
        return new CollectionFuture.ListFuture(ImmutableList.copyOf(iterable), true);
    }

    @SafeVarargs
    public static <V> ListenableFuture<List<V>> allAsList(ListenableFuture<? extends V>... listenableFutureArr) {
        return new CollectionFuture.ListFuture(ImmutableList.copyOf(listenableFutureArr), true);
    }

    public static <V, X extends Throwable> ListenableFuture<V> catching(ListenableFuture<? extends V> listenableFuture, Class<X> cls, Function<? super X, ? extends V> function, Executor executor) {
        return AbstractCatchingFuture.create(listenableFuture, cls, function, executor);
    }

    public static <V, X extends Throwable> ListenableFuture<V> catchingAsync(ListenableFuture<? extends V> listenableFuture, Class<X> cls, AsyncFunction<? super X, ? extends V> asyncFunction, Executor executor) {
        return AbstractCatchingFuture.create(listenableFuture, cls, asyncFunction, executor);
    }

    @ParametricNullness
    public static <V, X extends Exception> V getChecked(Future<V> future, Class<X> cls) throws Exception {
        return (V) FuturesGetChecked.getChecked(future, cls);
    }

    @ParametricNullness
    public static <V, X extends Exception> V getChecked(Future<V> future, Class<X> cls, long j, TimeUnit timeUnit) throws Exception {
        return (V) FuturesGetChecked.getChecked(future, cls, j, timeUnit);
    }

    @ParametricNullness
    public static <V> V getDone(Future<V> future) throws ExecutionException {
        Preconditions.checkState(future.isDone(), "Future was expected to be done: %s", future);
        return (V) Uninterruptibles.getUninterruptibly(future);
    }

    @ParametricNullness
    public static <V> V getUnchecked(Future<V> future) {
        Preconditions.checkNotNull(future);
        try {
            return (V) Uninterruptibles.getUninterruptibly(future);
        } catch (ExecutionException e) {
            wrapAndThrowUnchecked(e.getCause());
            throw new AssertionError();
        }
    }

    private static <T> ListenableFuture<? extends T>[] gwtCompatibleToArray(Iterable<? extends ListenableFuture<? extends T>> iterable) {
        return (ListenableFuture[]) (iterable instanceof Collection ? (Collection) iterable : ImmutableList.copyOf(iterable)).toArray(new ListenableFuture[0]);
    }

    public static <V> ListenableFuture<V> immediateCancelledFuture() {
        ImmediateFuture.ImmediateCancelledFuture<Object> immediateCancelledFuture = ImmediateFuture.ImmediateCancelledFuture.INSTANCE;
        return immediateCancelledFuture != null ? immediateCancelledFuture : new ImmediateFuture.ImmediateCancelledFuture();
    }

    public static <V> ListenableFuture<V> immediateFailedFuture(Throwable th) {
        Preconditions.checkNotNull(th);
        return new ImmediateFuture.ImmediateFailedFuture(th);
    }

    public static <V> ListenableFuture<V> immediateFuture(@ParametricNullness V v) {
        return v == null ? (ListenableFuture<V>) ImmediateFuture.NULL : new ImmediateFuture(v);
    }

    public static ListenableFuture<Void> immediateVoidFuture() {
        return ImmediateFuture.NULL;
    }

    public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(Iterable<? extends ListenableFuture<? extends T>> iterable) {
        ListenableFuture[] listenableFutureArrGwtCompatibleToArray = gwtCompatibleToArray(iterable);
        final InCompletionOrderState inCompletionOrderState = new InCompletionOrderState(listenableFutureArrGwtCompatibleToArray);
        ImmutableList.Builder builderBuilderWithExpectedSize = ImmutableList.builderWithExpectedSize(listenableFutureArrGwtCompatibleToArray.length);
        for (int i = 0; i < listenableFutureArrGwtCompatibleToArray.length; i++) {
            builderBuilderWithExpectedSize.add(new InCompletionOrderFuture(inCompletionOrderState));
        }
        final ImmutableList<ListenableFuture<T>> immutableListBuild = builderBuilderWithExpectedSize.build();
        for (final int i2 = 0; i2 < listenableFutureArrGwtCompatibleToArray.length; i2++) {
            listenableFutureArrGwtCompatibleToArray[i2].addListener(new Runnable(inCompletionOrderState, immutableListBuild, i2) { // from class: com.google.common.util.concurrent.Futures$$ExternalSyntheticLambda0
                public final Futures.InCompletionOrderState f$0;
                public final ImmutableList f$1;
                public final int f$2;

                {
                    this.f$0 = inCompletionOrderState;
                    this.f$1 = immutableListBuild;
                    this.f$2 = i2;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.recordInputCompletion(this.f$1, this.f$2);
                }
            }, MoreExecutors.directExecutor());
        }
        return immutableListBuild;
    }

    public static <I, O> Future<O> lazyTransform(Future<I> future, Function<? super I, ? extends O> function) {
        Preconditions.checkNotNull(future);
        Preconditions.checkNotNull(function);
        return new Future<O>(future, function) { // from class: com.google.common.util.concurrent.Futures.1
            final Function val$function;
            final Future val$input;

            {
                this.val$input = future;
                this.val$function = function;
            }

            private O applyTransformation(I i) throws ExecutionException {
                try {
                    return (O) this.val$function.apply(i);
                } catch (Throwable th) {
                    throw new ExecutionException(th);
                }
            }

            @Override // java.util.concurrent.Future
            public boolean cancel(boolean z) {
                return this.val$input.cancel(z);
            }

            @Override // java.util.concurrent.Future
            public O get() throws ExecutionException, InterruptedException {
                return applyTransformation(this.val$input.get());
            }

            @Override // java.util.concurrent.Future
            public O get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
                return applyTransformation(this.val$input.get(j, timeUnit));
            }

            @Override // java.util.concurrent.Future
            public boolean isCancelled() {
                return this.val$input.isCancelled();
            }

            @Override // java.util.concurrent.Future
            public boolean isDone() {
                return this.val$input.isDone();
            }
        };
    }

    public static <V> ListenableFuture<V> nonCancellationPropagating(ListenableFuture<V> listenableFuture) {
        if (listenableFuture.isDone()) {
            return listenableFuture;
        }
        NonCancellationPropagatingFuture nonCancellationPropagatingFuture = new NonCancellationPropagatingFuture(listenableFuture);
        listenableFuture.addListener(nonCancellationPropagatingFuture, MoreExecutors.directExecutor());
        return nonCancellationPropagatingFuture;
    }

    public static <O> ListenableFuture<O> scheduleAsync(AsyncCallable<O> asyncCallable, long j, TimeUnit timeUnit, ScheduledExecutorService scheduledExecutorService) {
        TrustedListenableFutureTask trustedListenableFutureTaskCreate = TrustedListenableFutureTask.create(asyncCallable);
        final ScheduledFuture<?> scheduledFutureSchedule = scheduledExecutorService.schedule(trustedListenableFutureTaskCreate, j, timeUnit);
        trustedListenableFutureTaskCreate.addListener(new Runnable(scheduledFutureSchedule) { // from class: com.google.common.util.concurrent.Futures$$ExternalSyntheticLambda1
            public final Future f$0;

            {
                this.f$0 = scheduledFutureSchedule;
            }

            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.cancel(false);
            }
        }, MoreExecutors.directExecutor());
        return trustedListenableFutureTaskCreate;
    }

    public static ListenableFuture<Void> submit(Runnable runnable, Executor executor) {
        TrustedListenableFutureTask trustedListenableFutureTaskCreate = TrustedListenableFutureTask.create(runnable, null);
        executor.execute(trustedListenableFutureTaskCreate);
        return trustedListenableFutureTaskCreate;
    }

    public static <O> ListenableFuture<O> submit(Callable<O> callable, Executor executor) {
        TrustedListenableFutureTask trustedListenableFutureTaskCreate = TrustedListenableFutureTask.create(callable);
        executor.execute(trustedListenableFutureTaskCreate);
        return trustedListenableFutureTaskCreate;
    }

    public static <O> ListenableFuture<O> submitAsync(AsyncCallable<O> asyncCallable, Executor executor) {
        TrustedListenableFutureTask trustedListenableFutureTaskCreate = TrustedListenableFutureTask.create(asyncCallable);
        executor.execute(trustedListenableFutureTaskCreate);
        return trustedListenableFutureTaskCreate;
    }

    public static <V> ListenableFuture<List<V>> successfulAsList(Iterable<? extends ListenableFuture<? extends V>> iterable) {
        return new CollectionFuture.ListFuture(ImmutableList.copyOf(iterable), false);
    }

    @SafeVarargs
    public static <V> ListenableFuture<List<V>> successfulAsList(ListenableFuture<? extends V>... listenableFutureArr) {
        return new CollectionFuture.ListFuture(ImmutableList.copyOf(listenableFutureArr), false);
    }

    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> listenableFuture, Function<? super I, ? extends O> function, Executor executor) {
        return AbstractTransformFuture.create(listenableFuture, function, executor);
    }

    public static <I, O> ListenableFuture<O> transformAsync(ListenableFuture<I> listenableFuture, AsyncFunction<? super I, ? extends O> asyncFunction, Executor executor) {
        return AbstractTransformFuture.create(listenableFuture, asyncFunction, executor);
    }

    public static <V> FutureCombiner<V> whenAllComplete(Iterable<? extends ListenableFuture<? extends V>> iterable) {
        return new FutureCombiner<>(false, ImmutableList.copyOf(iterable));
    }

    @SafeVarargs
    public static <V> FutureCombiner<V> whenAllComplete(ListenableFuture<? extends V>... listenableFutureArr) {
        return new FutureCombiner<>(false, ImmutableList.copyOf(listenableFutureArr));
    }

    public static <V> FutureCombiner<V> whenAllSucceed(Iterable<? extends ListenableFuture<? extends V>> iterable) {
        return new FutureCombiner<>(true, ImmutableList.copyOf(iterable));
    }

    @SafeVarargs
    public static <V> FutureCombiner<V> whenAllSucceed(ListenableFuture<? extends V>... listenableFutureArr) {
        return new FutureCombiner<>(true, ImmutableList.copyOf(listenableFutureArr));
    }

    public static <V> ListenableFuture<V> withTimeout(ListenableFuture<V> listenableFuture, long j, TimeUnit timeUnit, ScheduledExecutorService scheduledExecutorService) {
        return listenableFuture.isDone() ? listenableFuture : TimeoutFuture.create(listenableFuture, j, timeUnit, scheduledExecutorService);
    }

    private static void wrapAndThrowUnchecked(Throwable th) {
        if (!(th instanceof Error)) {
            throw new UncheckedExecutionException(th);
        }
        throw new ExecutionError((Error) th);
    }
}
