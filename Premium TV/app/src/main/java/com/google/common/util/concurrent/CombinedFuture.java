package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class CombinedFuture<V> extends AggregateFuture<Object, V> {

    @CheckForNull
    private CombinedFuture<V>.CombinedFutureInterruptibleTask<?> task;

    private final class AsyncCallableInterruptibleTask extends CombinedFuture<V>.CombinedFutureInterruptibleTask<ListenableFuture<V>> {
        private final AsyncCallable<V> callable;
        final CombinedFuture this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AsyncCallableInterruptibleTask(CombinedFuture combinedFuture, AsyncCallable<V> asyncCallable, Executor executor) {
            super(combinedFuture, executor);
            this.this$0 = combinedFuture;
            this.callable = (AsyncCallable) Preconditions.checkNotNull(asyncCallable);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.util.concurrent.InterruptibleTask
        public ListenableFuture<V> runInterruptibly() throws Exception {
            return (ListenableFuture) Preconditions.checkNotNull(this.callable.call(), "AsyncCallable.call returned null instead of a Future. Did you mean to return immediateFuture(null)? %s", this.callable);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.util.concurrent.CombinedFuture.CombinedFutureInterruptibleTask
        public void setValue(ListenableFuture<V> listenableFuture) {
            this.this$0.setFuture(listenableFuture);
        }

        @Override // com.google.common.util.concurrent.InterruptibleTask
        String toPendingString() {
            return this.callable.toString();
        }
    }

    private final class CallableInterruptibleTask extends CombinedFuture<V>.CombinedFutureInterruptibleTask<V> {
        private final Callable<V> callable;
        final CombinedFuture this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        CallableInterruptibleTask(CombinedFuture combinedFuture, Callable<V> callable, Executor executor) {
            super(combinedFuture, executor);
            this.this$0 = combinedFuture;
            this.callable = (Callable) Preconditions.checkNotNull(callable);
        }

        @Override // com.google.common.util.concurrent.InterruptibleTask
        @ParametricNullness
        V runInterruptibly() throws Exception {
            return this.callable.call();
        }

        @Override // com.google.common.util.concurrent.CombinedFuture.CombinedFutureInterruptibleTask
        void setValue(@ParametricNullness V v) {
            this.this$0.set(v);
        }

        @Override // com.google.common.util.concurrent.InterruptibleTask
        String toPendingString() {
            return this.callable.toString();
        }
    }

    private abstract class CombinedFutureInterruptibleTask<T> extends InterruptibleTask<T> {
        private final Executor listenerExecutor;
        final CombinedFuture this$0;

        CombinedFutureInterruptibleTask(CombinedFuture combinedFuture, Executor executor) {
            this.this$0 = combinedFuture;
            this.listenerExecutor = (Executor) Preconditions.checkNotNull(executor);
        }

        @Override // com.google.common.util.concurrent.InterruptibleTask
        final void afterRanInterruptiblyFailure(Throwable th) {
            this.this$0.task = null;
            if (th instanceof ExecutionException) {
                this.this$0.setException(((ExecutionException) th).getCause());
                return;
            }
            boolean z = th instanceof CancellationException;
            CombinedFuture combinedFuture = this.this$0;
            if (z) {
                combinedFuture.cancel(false);
            } else {
                combinedFuture.setException(th);
            }
        }

        @Override // com.google.common.util.concurrent.InterruptibleTask
        final void afterRanInterruptiblySuccess(@ParametricNullness T t) {
            this.this$0.task = null;
            setValue(t);
        }

        final void execute() {
            try {
                this.listenerExecutor.execute(this);
            } catch (RejectedExecutionException e) {
                this.this$0.setException(e);
            }
        }

        @Override // com.google.common.util.concurrent.InterruptibleTask
        final boolean isDone() {
            return this.this$0.isDone();
        }

        abstract void setValue(@ParametricNullness T t);
    }

    CombinedFuture(ImmutableCollection<? extends ListenableFuture<?>> immutableCollection, boolean z, Executor executor, AsyncCallable<V> asyncCallable) {
        super(immutableCollection, z, false);
        this.task = new AsyncCallableInterruptibleTask(this, asyncCallable, executor);
        init();
    }

    CombinedFuture(ImmutableCollection<? extends ListenableFuture<?>> immutableCollection, boolean z, Executor executor, Callable<V> callable) {
        super(immutableCollection, z, false);
        this.task = new CallableInterruptibleTask(this, callable, executor);
        init();
    }

    @Override // com.google.common.util.concurrent.AggregateFuture
    void collectOneValue(int i, @CheckForNull Object obj) {
    }

    @Override // com.google.common.util.concurrent.AggregateFuture
    void handleAllCompleted() {
        CombinedFuture<V>.CombinedFutureInterruptibleTask<?> combinedFutureInterruptibleTask = this.task;
        if (combinedFutureInterruptibleTask != null) {
            combinedFutureInterruptibleTask.execute();
        }
    }

    @Override // com.google.common.util.concurrent.AbstractFuture
    protected void interruptTask() {
        CombinedFuture<V>.CombinedFutureInterruptibleTask<?> combinedFutureInterruptibleTask = this.task;
        if (combinedFutureInterruptibleTask != null) {
            combinedFutureInterruptibleTask.interruptTask();
        }
    }

    @Override // com.google.common.util.concurrent.AggregateFuture
    void releaseResources(AggregateFuture.ReleaseResourcesReason releaseResourcesReason) {
        super.releaseResources(releaseResourcesReason);
        if (releaseResourcesReason == AggregateFuture.ReleaseResourcesReason.OUTPUT_FUTURE_DONE) {
            this.task = null;
        }
    }
}
