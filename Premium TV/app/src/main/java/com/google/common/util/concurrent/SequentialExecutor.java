package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class SequentialExecutor implements Executor {
    private static final LazyLogger log = new LazyLogger(SequentialExecutor.class);
    private final Executor executor;
    private final Deque<Runnable> queue = new ArrayDeque();
    private WorkerRunningState workerRunningState = WorkerRunningState.IDLE;
    private long workerRunCount = 0;
    private final QueueWorker worker = new QueueWorker();

    private final class QueueWorker implements Runnable {

        @CheckForNull
        Runnable task;
        final SequentialExecutor this$0;

        private QueueWorker(SequentialExecutor sequentialExecutor) {
            this.this$0 = sequentialExecutor;
        }

        /* JADX WARN: Code duplicated, block: B:34:0x008e  */
        /* JADX WARN: Code restructure failed: missing block: B:17:0x0047, code lost:
        
            if (r2 == false) goto L57;
         */
        /* JADX WARN: Code restructure failed: missing block: B:18:0x0049, code lost:
        
            java.lang.Thread.currentThread().interrupt();
         */
        /* JADX WARN: Code restructure failed: missing block: B:21:0x0056, code lost:
        
            r0 = java.lang.Thread.interrupted() | r2;
         */
        /* JADX WARN: Code restructure failed: missing block: B:22:0x0057, code lost:
        
            r7.task.run();
         */
        /* JADX WARN: Code restructure failed: missing block: B:24:0x005d, code lost:
        
            r7.task = null;
         */
        /* JADX WARN: Code restructure failed: missing block: B:26:0x0062, code lost:
        
            r2 = move-exception;
         */
        /* JADX WARN: Code restructure failed: missing block: B:27:0x0063, code lost:
        
            com.google.common.util.concurrent.SequentialExecutor.log.get().log(java.util.logging.Level.SEVERE, "Exception while executing runnable " + r7.task, (java.lang.Throwable) r2);
         */
        /* JADX WARN: Code restructure failed: missing block: B:29:0x0086, code lost:
        
            r7.task = null;
         */
        /* JADX WARN: Code restructure failed: missing block: B:31:0x0089, code lost:
        
            r1 = move-exception;
         */
        /* JADX WARN: Code restructure failed: missing block: B:32:0x008a, code lost:
        
            r2 = r0;
            r0 = r1;
         */
        /* JADX WARN: Code restructure failed: missing block: B:36:0x0096, code lost:
        
            r1 = move-exception;
         */
        /* JADX WARN: Code restructure failed: missing block: B:38:0x0098, code lost:
        
            r7.task = null;
         */
        /* JADX WARN: Code restructure failed: missing block: B:39:0x009a, code lost:
        
            throw r1;
         */
        /* JADX WARN: Code restructure failed: missing block: B:57:?, code lost:
        
            return;
         */
        /* JADX WARN: Code restructure failed: missing block: B:58:?, code lost:
        
            return;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        private void workOnQueue() throws java.lang.Throwable {
            /*
                r7 = this;
                r0 = 0
                r2 = r0
            L2:
                com.google.common.util.concurrent.SequentialExecutor r1 = r7.this$0     // Catch: java.lang.Throwable -> L9e
                java.util.Deque r3 = com.google.common.util.concurrent.SequentialExecutor.access$100(r1)     // Catch: java.lang.Throwable -> L9e
                monitor-enter(r3)     // Catch: java.lang.Throwable -> L9e
                if (r0 != 0) goto La0
                com.google.common.util.concurrent.SequentialExecutor r0 = r7.this$0     // Catch: java.lang.Throwable -> L9b
                com.google.common.util.concurrent.SequentialExecutor$WorkerRunningState r0 = com.google.common.util.concurrent.SequentialExecutor.access$200(r0)     // Catch: java.lang.Throwable -> L9b
                com.google.common.util.concurrent.SequentialExecutor$WorkerRunningState r1 = com.google.common.util.concurrent.SequentialExecutor.WorkerRunningState.RUNNING     // Catch: java.lang.Throwable -> L9b
                if (r0 != r1) goto L20
                monitor-exit(r3)     // Catch: java.lang.Throwable -> L9b
                if (r2 == 0) goto L1f
                java.lang.Thread r0 = java.lang.Thread.currentThread()
                r0.interrupt()
            L1f:
                return
            L20:
                com.google.common.util.concurrent.SequentialExecutor r0 = r7.this$0     // Catch: java.lang.Throwable -> L9b
                com.google.common.util.concurrent.SequentialExecutor.access$308(r0)     // Catch: java.lang.Throwable -> L9b
                com.google.common.util.concurrent.SequentialExecutor r0 = r7.this$0     // Catch: java.lang.Throwable -> L9b
                com.google.common.util.concurrent.SequentialExecutor$WorkerRunningState r1 = com.google.common.util.concurrent.SequentialExecutor.WorkerRunningState.RUNNING     // Catch: java.lang.Throwable -> L9b
                com.google.common.util.concurrent.SequentialExecutor.access$202(r0, r1)     // Catch: java.lang.Throwable -> L9b
                r1 = 1
            L2d:
                com.google.common.util.concurrent.SequentialExecutor r0 = r7.this$0     // Catch: java.lang.Throwable -> L9b
                java.util.Deque r0 = com.google.common.util.concurrent.SequentialExecutor.access$100(r0)     // Catch: java.lang.Throwable -> L9b
                java.lang.Object r0 = r0.poll()     // Catch: java.lang.Throwable -> L9b
                java.lang.Runnable r0 = (java.lang.Runnable) r0     // Catch: java.lang.Throwable -> L9b
                r7.task = r0     // Catch: java.lang.Throwable -> L9b
                java.lang.Runnable r0 = r7.task     // Catch: java.lang.Throwable -> L9b
                if (r0 != 0) goto L51
                com.google.common.util.concurrent.SequentialExecutor r0 = r7.this$0     // Catch: java.lang.Throwable -> L9b
                com.google.common.util.concurrent.SequentialExecutor$WorkerRunningState r1 = com.google.common.util.concurrent.SequentialExecutor.WorkerRunningState.IDLE     // Catch: java.lang.Throwable -> L9b
                com.google.common.util.concurrent.SequentialExecutor.access$202(r0, r1)     // Catch: java.lang.Throwable -> L9b
                monitor-exit(r3)     // Catch: java.lang.Throwable -> L9b
                if (r2 == 0) goto L1f
                java.lang.Thread r0 = java.lang.Thread.currentThread()
                r0.interrupt()
                goto L1f
            L51:
                monitor-exit(r3)     // Catch: java.lang.Throwable -> L9b
                boolean r0 = java.lang.Thread.interrupted()     // Catch: java.lang.Throwable -> L9e
                r0 = r0 | r2
                java.lang.Runnable r2 = r7.task     // Catch: java.lang.Exception -> L62 java.lang.Throwable -> L96
                r2.run()     // Catch: java.lang.Exception -> L62 java.lang.Throwable -> L96
                r2 = 0
                r7.task = r2     // Catch: java.lang.Throwable -> L89
            L5f:
                r2 = r0
                r0 = r1
                goto L2
            L62:
                r2 = move-exception
                com.google.common.util.concurrent.LazyLogger r3 = com.google.common.util.concurrent.SequentialExecutor.access$400()     // Catch: java.lang.Throwable -> L96
                java.util.logging.Logger r3 = r3.get()     // Catch: java.lang.Throwable -> L96
                java.util.logging.Level r4 = java.util.logging.Level.SEVERE     // Catch: java.lang.Throwable -> L96
                java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L96
                r5.<init>()     // Catch: java.lang.Throwable -> L96
                java.lang.String r6 = "Exception while executing runnable "
                java.lang.StringBuilder r5 = r5.append(r6)     // Catch: java.lang.Throwable -> L96
                java.lang.Runnable r6 = r7.task     // Catch: java.lang.Throwable -> L96
                java.lang.StringBuilder r5 = r5.append(r6)     // Catch: java.lang.Throwable -> L96
                java.lang.String r5 = r5.toString()     // Catch: java.lang.Throwable -> L96
                r3.log(r4, r5, r2)     // Catch: java.lang.Throwable -> L96
                r2 = 0
                r7.task = r2     // Catch: java.lang.Throwable -> L89
                goto L5f
            L89:
                r1 = move-exception
                r2 = r0
                r0 = r1
            L8c:
                if (r2 == 0) goto L95
                java.lang.Thread r1 = java.lang.Thread.currentThread()
                r1.interrupt()
            L95:
                throw r0
            L96:
                r1 = move-exception
                r2 = 0
                r7.task = r2     // Catch: java.lang.Throwable -> L89
                throw r1     // Catch: java.lang.Throwable -> L89
            L9b:
                r0 = move-exception
                monitor-exit(r3)     // Catch: java.lang.Throwable -> L9b
                throw r0     // Catch: java.lang.Throwable -> L9e
            L9e:
                r0 = move-exception
                goto L8c
            La0:
                r1 = r0
                goto L2d
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.util.concurrent.SequentialExecutor.QueueWorker.workOnQueue():void");
        }

        @Override // java.lang.Runnable
        public void run() throws Throwable {
            try {
                workOnQueue();
            } catch (Error e) {
                synchronized (this.this$0.queue) {
                    this.this$0.workerRunningState = WorkerRunningState.IDLE;
                    throw e;
                }
            }
        }

        public String toString() {
            Runnable runnable = this.task;
            return runnable != null ? "SequentialExecutorWorker{running=" + runnable + "}" : "SequentialExecutorWorker{state=" + this.this$0.workerRunningState + "}";
        }
    }

    enum WorkerRunningState {
        IDLE,
        QUEUING,
        QUEUED,
        RUNNING
    }

    SequentialExecutor(Executor executor) {
        this.executor = (Executor) Preconditions.checkNotNull(executor);
    }

    static /* synthetic */ long access$308(SequentialExecutor sequentialExecutor) {
        long j = sequentialExecutor.workerRunCount;
        sequentialExecutor.workerRunCount = 1 + j;
        return j;
    }

    @Override // java.util.concurrent.Executor
    public void execute(Runnable runnable) {
        boolean z = false;
        Preconditions.checkNotNull(runnable);
        synchronized (this.queue) {
            if (this.workerRunningState == WorkerRunningState.RUNNING || this.workerRunningState == WorkerRunningState.QUEUED) {
                this.queue.add(runnable);
                return;
            }
            long j = this.workerRunCount;
            Runnable runnable2 = new Runnable(this, runnable) { // from class: com.google.common.util.concurrent.SequentialExecutor.1
                final Runnable val$task;

                {
                    this.val$task = runnable;
                }

                @Override // java.lang.Runnable
                public void run() {
                    this.val$task.run();
                }

                public String toString() {
                    return this.val$task.toString();
                }
            };
            this.queue.add(runnable2);
            this.workerRunningState = WorkerRunningState.QUEUING;
            try {
                this.executor.execute(this.worker);
                if (this.workerRunningState != WorkerRunningState.QUEUING) {
                    return;
                }
                synchronized (this.queue) {
                    if (this.workerRunCount == j && this.workerRunningState == WorkerRunningState.QUEUING) {
                        this.workerRunningState = WorkerRunningState.QUEUED;
                    }
                }
            } catch (Throwable th) {
                synchronized (this.queue) {
                    if ((this.workerRunningState == WorkerRunningState.IDLE || this.workerRunningState == WorkerRunningState.QUEUING) && this.queue.removeLastOccurrence(runnable2)) {
                        z = true;
                    }
                    if (!(th instanceof RejectedExecutionException) || z) {
                        throw th;
                    }
                }
            }
        }
    }

    public String toString() {
        return "SequentialExecutor@" + System.identityHashCode(this) + "{" + this.executor + "}";
    }
}
