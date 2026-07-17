package com.google.common.util.concurrent;

import androidx.core.app.NotificationCompat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class ListenerCallQueue<L> {
    private static final LazyLogger logger = new LazyLogger(ListenerCallQueue.class);
    private final List<PerListenerQueue<L>> listeners = Collections.synchronizedList(new ArrayList());

    interface Event<L> {
        void call(L l);
    }

    private static final class PerListenerQueue<L> implements Runnable {
        final Executor executor;
        boolean isThreadScheduled;
        final L listener;
        final Queue<Event<L>> waitQueue = Queues.newArrayDeque();
        final Queue<Object> labelQueue = Queues.newArrayDeque();

        PerListenerQueue(L l, Executor executor) {
            this.listener = (L) Preconditions.checkNotNull(l);
            this.executor = (Executor) Preconditions.checkNotNull(executor);
        }

        void add(Event<L> event, Object obj) {
            synchronized (this) {
                this.waitQueue.add(event);
                this.labelQueue.add(obj);
            }
        }

        void dispatch() throws Exception {
            boolean z = true;
            synchronized (this) {
                if (this.isThreadScheduled) {
                    z = false;
                } else {
                    this.isThreadScheduled = true;
                }
            }
            if (z) {
                try {
                    this.executor.execute(this);
                } catch (Exception e) {
                    synchronized (this) {
                        this.isThreadScheduled = false;
                        ListenerCallQueue.logger.get().log(Level.SEVERE, "Exception while running callbacks for " + this.listener + " on " + this.executor, (Throwable) e);
                        throw e;
                    }
                }
            }
        }

        /* JADX WARN: Code duplicated, block: B:17:0x0055  */
        /* JADX WARN: Code duplicated, block: B:18:0x0056  */
        /* JADX WARN: Code restructure failed: missing block: B:10:0x001e, code lost:
        
            r0.call(r8.listener);
         */
        /* JADX WARN: Code restructure failed: missing block: B:12:0x0024, code lost:
        
            r0 = move-exception;
         */
        /* JADX WARN: Code restructure failed: missing block: B:13:0x0025, code lost:
        
            com.google.common.util.concurrent.ListenerCallQueue.logger.get().log(java.util.logging.Level.SEVERE, "Exception while executing callback: " + r8.listener + " " + r3, (java.lang.Throwable) r0);
         */
        @Override // java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public void run() throws java.lang.Throwable {
            /*
                r8 = this;
                r2 = 1
                r1 = 0
            L2:
                monitor-enter(r8)     // Catch: java.lang.Throwable -> L52
                boolean r0 = r8.isThreadScheduled     // Catch: java.lang.Throwable -> L5b
                com.google.common.base.Preconditions.checkState(r0)     // Catch: java.lang.Throwable -> L5b
                java.util.Queue<com.google.common.util.concurrent.ListenerCallQueue$Event<L>> r0 = r8.waitQueue     // Catch: java.lang.Throwable -> L5b
                java.lang.Object r0 = r0.poll()     // Catch: java.lang.Throwable -> L5b
                com.google.common.util.concurrent.ListenerCallQueue$Event r0 = (com.google.common.util.concurrent.ListenerCallQueue.Event) r0     // Catch: java.lang.Throwable -> L5b
                java.util.Queue<java.lang.Object> r3 = r8.labelQueue     // Catch: java.lang.Throwable -> L5b
                java.lang.Object r3 = r3.poll()     // Catch: java.lang.Throwable -> L5b
                if (r0 != 0) goto L1d
                r0 = 0
                r8.isThreadScheduled = r0     // Catch: java.lang.Throwable -> L5b
                monitor-exit(r8)     // Catch: java.lang.Throwable -> L65
                return
            L1d:
                monitor-exit(r8)     // Catch: java.lang.Throwable -> L5b
                L r4 = r8.listener     // Catch: java.lang.Exception -> L24 java.lang.Throwable -> L52
                r0.call(r4)     // Catch: java.lang.Exception -> L24 java.lang.Throwable -> L52
                goto L2
            L24:
                r0 = move-exception
                com.google.common.util.concurrent.LazyLogger r4 = com.google.common.util.concurrent.ListenerCallQueue.access$000()     // Catch: java.lang.Throwable -> L52
                java.util.logging.Logger r4 = r4.get()     // Catch: java.lang.Throwable -> L52
                java.util.logging.Level r5 = java.util.logging.Level.SEVERE     // Catch: java.lang.Throwable -> L52
                java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L52
                r6.<init>()     // Catch: java.lang.Throwable -> L52
                java.lang.String r7 = "Exception while executing callback: "
                java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L52
                L r7 = r8.listener     // Catch: java.lang.Throwable -> L52
                java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L52
                java.lang.String r7 = " "
                java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L52
                java.lang.StringBuilder r3 = r6.append(r3)     // Catch: java.lang.Throwable -> L52
                java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L52
                r4.log(r5, r3, r0)     // Catch: java.lang.Throwable -> L52
                goto L2
            L52:
                r0 = move-exception
            L53:
                if (r2 == 0) goto L5a
                monitor-enter(r8)
                r1 = 0
                r8.isThreadScheduled = r1     // Catch: java.lang.Throwable -> L62
                monitor-exit(r8)     // Catch: java.lang.Throwable -> L62
            L5a:
                throw r0
            L5b:
                r0 = move-exception
                r1 = r2
            L5d:
                monitor-exit(r8)     // Catch: java.lang.Throwable -> L65
                throw r0     // Catch: java.lang.Throwable -> L5f
            L5f:
                r0 = move-exception
                r2 = r1
                goto L53
            L62:
                r0 = move-exception
                monitor-exit(r8)     // Catch: java.lang.Throwable -> L62
                throw r0
            L65:
                r0 = move-exception
                goto L5d
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.util.concurrent.ListenerCallQueue.PerListenerQueue.run():void");
        }
    }

    ListenerCallQueue() {
    }

    private void enqueueHelper(Event<L> event, Object obj) {
        Preconditions.checkNotNull(event, NotificationCompat.CATEGORY_EVENT);
        Preconditions.checkNotNull(obj, "label");
        synchronized (this.listeners) {
            Iterator<PerListenerQueue<L>> it = this.listeners.iterator();
            while (it.hasNext()) {
                it.next().add(event, obj);
            }
        }
    }

    public void addListener(L l, Executor executor) {
        Preconditions.checkNotNull(l, "listener");
        Preconditions.checkNotNull(executor, "executor");
        this.listeners.add(new PerListenerQueue<>(l, executor));
    }

    public void dispatch() throws Exception {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.listeners.size()) {
                return;
            }
            this.listeners.get(i2).dispatch();
            i = i2 + 1;
        }
    }

    public void enqueue(Event<L> event) {
        enqueueHelper(event, event);
    }

    public void enqueue(Event<L> event, String str) {
        enqueueHelper(event, str);
    }
}
