package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class ExecutionSequencer {
    private final AtomicReference<ListenableFuture<Void>> ref = new AtomicReference<>(Futures.immediateVoidFuture());
    private ThreadConfinedTaskQueue latestTaskQueue = new ThreadConfinedTaskQueue();

    enum RunningState {
        NOT_RUN,
        CANCELLED,
        STARTED
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class TaskNonReentrantExecutor extends AtomicReference<RunningState> implements Executor, Runnable {

        @CheckForNull
        Executor delegate;

        @CheckForNull
        ExecutionSequencer sequencer;

        @CheckForNull
        Thread submitting;

        @CheckForNull
        Runnable task;

        private TaskNonReentrantExecutor(Executor executor, ExecutionSequencer executionSequencer) {
            super(RunningState.NOT_RUN);
            this.delegate = executor;
            this.sequencer = executionSequencer;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean trySetCancelled() {
            return compareAndSet(RunningState.NOT_RUN, RunningState.CANCELLED);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean trySetStarted() {
            return compareAndSet(RunningState.NOT_RUN, RunningState.STARTED);
        }

        @Override // java.util.concurrent.Executor
        public void execute(Runnable runnable) {
            if (get() == RunningState.CANCELLED) {
                this.delegate = null;
                this.sequencer = null;
                return;
            }
            this.submitting = Thread.currentThread();
            try {
                ThreadConfinedTaskQueue threadConfinedTaskQueue = ((ExecutionSequencer) Objects.requireNonNull(this.sequencer)).latestTaskQueue;
                if (threadConfinedTaskQueue.thread == this.submitting) {
                    this.sequencer = null;
                    Preconditions.checkState(threadConfinedTaskQueue.nextTask == null);
                    threadConfinedTaskQueue.nextTask = runnable;
                    threadConfinedTaskQueue.nextExecutor = (Executor) Objects.requireNonNull(this.delegate);
                    this.delegate = null;
                } else {
                    Executor executor = (Executor) Objects.requireNonNull(this.delegate);
                    this.delegate = null;
                    this.task = runnable;
                    executor.execute(this);
                }
            } finally {
                this.submitting = null;
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            Executor executor;
            Thread threadCurrentThread = Thread.currentThread();
            if (threadCurrentThread != this.submitting) {
                Runnable runnable = (Runnable) Objects.requireNonNull(this.task);
                this.task = null;
                runnable.run();
                return;
            }
            ThreadConfinedTaskQueue threadConfinedTaskQueue = new ThreadConfinedTaskQueue();
            threadConfinedTaskQueue.thread = threadCurrentThread;
            ((ExecutionSequencer) Objects.requireNonNull(this.sequencer)).latestTaskQueue = threadConfinedTaskQueue;
            this.sequencer = null;
            try {
                Runnable runnable2 = (Runnable) Objects.requireNonNull(this.task);
                this.task = null;
                runnable2.run();
                while (true) {
                    Runnable runnable3 = threadConfinedTaskQueue.nextTask;
                    if (runnable3 == null || (executor = threadConfinedTaskQueue.nextExecutor) == null) {
                        break;
                    }
                    threadConfinedTaskQueue.nextTask = null;
                    threadConfinedTaskQueue.nextExecutor = null;
                    executor.execute(runnable3);
                }
                threadConfinedTaskQueue.thread = null;
            } catch (Throwable th) {
                threadConfinedTaskQueue.thread = null;
                throw th;
            }
        }
    }

    private static final class ThreadConfinedTaskQueue {

        @CheckForNull
        Executor nextExecutor;

        @CheckForNull
        Runnable nextTask;

        @CheckForNull
        Thread thread;

        private ThreadConfinedTaskQueue() {
        }
    }

    private ExecutionSequencer() {
    }

    public static ExecutionSequencer create() {
        return new ExecutionSequencer();
    }

    static /* synthetic */ void lambda$submitAsync$0(TrustedListenableFutureTask trustedListenableFutureTask, SettableFuture settableFuture, ListenableFuture listenableFuture, ListenableFuture listenableFuture2, TaskNonReentrantExecutor taskNonReentrantExecutor) {
        if (trustedListenableFutureTask.isDone()) {
            settableFuture.setFuture(listenableFuture);
        } else if (listenableFuture2.isCancelled() && taskNonReentrantExecutor.trySetCancelled()) {
            trustedListenableFutureTask.cancel(false);
        }
    }

    public <T> ListenableFuture<T> submit(Callable<T> callable, Executor executor) {
        Preconditions.checkNotNull(callable);
        Preconditions.checkNotNull(executor);
        return submitAsync(new AsyncCallable<T>(this, callable) { // from class: com.google.common.util.concurrent.ExecutionSequencer.1
            final Callable val$callable;

            {
                this.val$callable = callable;
            }

            @Override // com.google.common.util.concurrent.AsyncCallable
            public ListenableFuture<T> call() throws Exception {
                return Futures.immediateFuture(this.val$callable.call());
            }

            public String toString() {
                return this.val$callable.toString();
            }
        }, executor);
    }

    public <T> ListenableFuture<T> submitAsync(AsyncCallable<T> asyncCallable, Executor executor) {
        Preconditions.checkNotNull(asyncCallable);
        Preconditions.checkNotNull(executor);
        final TaskNonReentrantExecutor taskNonReentrantExecutor = new TaskNonReentrantExecutor(executor, this);
        AsyncCallable<T> asyncCallable2 = new AsyncCallable<T>(this, taskNonReentrantExecutor, asyncCallable) { // from class: com.google.common.util.concurrent.ExecutionSequencer.2
            final AsyncCallable val$callable;
            final TaskNonReentrantExecutor val$taskExecutor;

            {
                this.val$taskExecutor = taskNonReentrantExecutor;
                this.val$callable = asyncCallable;
            }

            @Override // com.google.common.util.concurrent.AsyncCallable
            public ListenableFuture<T> call() throws Exception {
                return !this.val$taskExecutor.trySetStarted() ? Futures.immediateCancelledFuture() : this.val$callable.call();
            }

            public String toString() {
                return this.val$callable.toString();
            }
        };
        final SettableFuture settableFutureCreate = SettableFuture.create();
        final ListenableFuture<Void> andSet = this.ref.getAndSet(settableFutureCreate);
        final TrustedListenableFutureTask trustedListenableFutureTaskCreate = TrustedListenableFutureTask.create(asyncCallable2);
        andSet.addListener(trustedListenableFutureTaskCreate, taskNonReentrantExecutor);
        final ListenableFuture<T> listenableFutureNonCancellationPropagating = Futures.nonCancellationPropagating(trustedListenableFutureTaskCreate);
        Runnable runnable = new Runnable(trustedListenableFutureTaskCreate, settableFutureCreate, andSet, listenableFutureNonCancellationPropagating, taskNonReentrantExecutor) { // from class: com.google.common.util.concurrent.ExecutionSequencer$$ExternalSyntheticLambda0
            public final TrustedListenableFutureTask f$0;
            public final SettableFuture f$1;
            public final ListenableFuture f$2;
            public final ListenableFuture f$3;
            public final ExecutionSequencer.TaskNonReentrantExecutor f$4;

            {
                this.f$0 = trustedListenableFutureTaskCreate;
                this.f$1 = settableFutureCreate;
                this.f$2 = andSet;
                this.f$3 = listenableFutureNonCancellationPropagating;
                this.f$4 = taskNonReentrantExecutor;
            }

            @Override // java.lang.Runnable
            public final void run() {
                ExecutionSequencer.lambda$submitAsync$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4);
            }
        };
        listenableFutureNonCancellationPropagating.addListener(runnable, MoreExecutors.directExecutor());
        trustedListenableFutureTaskCreate.addListener(runnable, MoreExecutors.directExecutor());
        return listenableFutureNonCancellationPropagating;
    }
}
