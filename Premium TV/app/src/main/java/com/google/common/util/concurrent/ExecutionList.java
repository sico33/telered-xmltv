package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class ExecutionList {
    private static final LazyLogger log = new LazyLogger(ExecutionList.class);
    private boolean executed;

    @CheckForNull
    private RunnableExecutorPair runnables;

    private static final class RunnableExecutorPair {
        final Executor executor;

        @CheckForNull
        RunnableExecutorPair next;
        final Runnable runnable;

        RunnableExecutorPair(Runnable runnable, Executor executor, @CheckForNull RunnableExecutorPair runnableExecutorPair) {
            this.runnable = runnable;
            this.executor = executor;
            this.next = runnableExecutorPair;
        }
    }

    private static void executeListener(Runnable runnable, Executor executor) {
        try {
            executor.execute(runnable);
        } catch (Exception e) {
            log.get().log(Level.SEVERE, "RuntimeException while executing runnable " + runnable + " with executor " + executor, (Throwable) e);
        }
    }

    public void add(Runnable runnable, Executor executor) {
        Preconditions.checkNotNull(runnable, "Runnable was null.");
        Preconditions.checkNotNull(executor, "Executor was null.");
        synchronized (this) {
            if (this.executed) {
                executeListener(runnable, executor);
            } else {
                this.runnables = new RunnableExecutorPair(runnable, executor, this.runnables);
            }
        }
    }

    public void execute() {
        RunnableExecutorPair runnableExecutorPair = null;
        synchronized (this) {
            if (this.executed) {
                return;
            }
            this.executed = true;
            RunnableExecutorPair runnableExecutorPair2 = this.runnables;
            this.runnables = null;
            while (runnableExecutorPair2 != null) {
                RunnableExecutorPair runnableExecutorPair3 = runnableExecutorPair2.next;
                runnableExecutorPair2.next = runnableExecutorPair;
                runnableExecutorPair = runnableExecutorPair2;
                runnableExecutorPair2 = runnableExecutorPair3;
            }
            while (runnableExecutorPair != null) {
                executeListener(runnableExecutorPair.runnable, runnableExecutorPair.executor);
                runnableExecutorPair = runnableExecutorPair.next;
            }
        }
    }
}
