package androidx.media3.common.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.media3.common.FlagSet;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

/* JADX INFO: loaded from: classes.dex */
public final class ListenerSet<T> {
    private static final int MSG_ITERATION_FINISHED = 1;
    private final Clock clock;
    private final ArrayDeque<Runnable> flushingEvents;
    private final HandlerWrapper handler;
    private final IterationFinishedEvent<T> iterationFinishedEvent;
    private final CopyOnWriteArraySet<ListenerHolder<T>> listeners;
    private final ArrayDeque<Runnable> queuedEvents;
    private boolean released;
    private final Object releasedLock;
    private boolean throwsWhenUsingWrongThread;

    public interface Event<T> {
        void invoke(T t);
    }

    public interface IterationFinishedEvent<T> {
        void invoke(T t, FlagSet flagSet);
    }

    public ListenerSet(Looper looper, Clock clock, IterationFinishedEvent<T> iterationFinishedEvent) {
        this(new CopyOnWriteArraySet(), looper, clock, iterationFinishedEvent, true);
    }

    private ListenerSet(CopyOnWriteArraySet<ListenerHolder<T>> listeners, Looper looper, Clock clock, IterationFinishedEvent<T> iterationFinishedEvent, boolean throwsWhenUsingWrongThread) {
        this.clock = clock;
        this.listeners = listeners;
        this.iterationFinishedEvent = iterationFinishedEvent;
        this.releasedLock = new Object();
        this.flushingEvents = new ArrayDeque<>();
        this.queuedEvents = new ArrayDeque<>();
        HandlerWrapper handler = clock.createHandler(looper, new Handler.Callback() { // from class: androidx.media3.common.util.ListenerSet$$ExternalSyntheticLambda0
            @Override // android.os.Handler.Callback
            public final boolean handleMessage(Message message) {
                return this.f$0.handleMessage(message);
            }
        });
        this.handler = handler;
        this.throwsWhenUsingWrongThread = throwsWhenUsingWrongThread;
    }

    public ListenerSet<T> copy(Looper looper, IterationFinishedEvent<T> iterationFinishedEvent) {
        return copy(looper, this.clock, iterationFinishedEvent);
    }

    public ListenerSet<T> copy(Looper looper, Clock clock, IterationFinishedEvent<T> iterationFinishedEvent) {
        return new ListenerSet<>(this.listeners, looper, clock, iterationFinishedEvent, this.throwsWhenUsingWrongThread);
    }

    public void add(T listener) {
        Assertions.checkNotNull(listener);
        synchronized (this.releasedLock) {
            if (this.released) {
                return;
            }
            this.listeners.add(new ListenerHolder<>(listener));
        }
    }

    public void remove(T listener) {
        verifyCurrentThread();
        for (ListenerHolder<T> listenerHolder : this.listeners) {
            if (listenerHolder.listener.equals(listener)) {
                listenerHolder.release(this.iterationFinishedEvent);
                this.listeners.remove(listenerHolder);
            }
        }
    }

    public void clear() {
        verifyCurrentThread();
        this.listeners.clear();
    }

    public int size() {
        verifyCurrentThread();
        return this.listeners.size();
    }

    public void queueEvent(final int eventFlag, final Event<T> event) {
        verifyCurrentThread();
        final CopyOnWriteArraySet<ListenerHolder<T>> listenerSnapshot = new CopyOnWriteArraySet<>(this.listeners);
        this.queuedEvents.add(new Runnable() { // from class: androidx.media3.common.util.ListenerSet$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ListenerSet.lambda$queueEvent$0(listenerSnapshot, eventFlag, event);
            }
        });
    }

    static /* synthetic */ void lambda$queueEvent$0(CopyOnWriteArraySet listenerSnapshot, int eventFlag, Event event) {
        Iterator it = listenerSnapshot.iterator();
        while (it.hasNext()) {
            ListenerHolder<T> holder = (ListenerHolder) it.next();
            holder.invoke(eventFlag, event);
        }
    }

    public void flushEvents() {
        verifyCurrentThread();
        if (this.queuedEvents.isEmpty()) {
            return;
        }
        if (!this.handler.hasMessages(1)) {
            this.handler.sendMessageAtFrontOfQueue(this.handler.obtainMessage(1));
        }
        boolean recursiveFlushInProgress = !this.flushingEvents.isEmpty();
        this.flushingEvents.addAll(this.queuedEvents);
        this.queuedEvents.clear();
        if (recursiveFlushInProgress) {
            return;
        }
        while (!this.flushingEvents.isEmpty()) {
            this.flushingEvents.peekFirst().run();
            this.flushingEvents.removeFirst();
        }
    }

    public void sendEvent(int eventFlag, Event<T> event) {
        queueEvent(eventFlag, event);
        flushEvents();
    }

    public void release() {
        verifyCurrentThread();
        synchronized (this.releasedLock) {
            this.released = true;
        }
        for (ListenerHolder<T> listenerHolder : this.listeners) {
            listenerHolder.release(this.iterationFinishedEvent);
        }
        this.listeners.clear();
    }

    @Deprecated
    public void setThrowsWhenUsingWrongThread(boolean throwsWhenUsingWrongThread) {
        this.throwsWhenUsingWrongThread = throwsWhenUsingWrongThread;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handleMessage(Message message) {
        for (ListenerHolder<T> holder : this.listeners) {
            holder.iterationFinished(this.iterationFinishedEvent);
            if (this.handler.hasMessages(1)) {
                break;
            }
        }
        return true;
    }

    private void verifyCurrentThread() {
        if (!this.throwsWhenUsingWrongThread) {
            return;
        }
        Assertions.checkState(Thread.currentThread() == this.handler.getLooper().getThread());
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class ListenerHolder<T> {
        private FlagSet.Builder flagsBuilder = new FlagSet.Builder();
        public final T listener;
        private boolean needsIterationFinishedEvent;
        private boolean released;

        public ListenerHolder(T listener) {
            this.listener = listener;
        }

        public void release(IterationFinishedEvent<T> event) {
            this.released = true;
            if (this.needsIterationFinishedEvent) {
                this.needsIterationFinishedEvent = false;
                event.invoke(this.listener, this.flagsBuilder.build());
            }
        }

        public void invoke(int eventFlag, Event<T> event) {
            if (!this.released) {
                if (eventFlag != -1) {
                    this.flagsBuilder.add(eventFlag);
                }
                this.needsIterationFinishedEvent = true;
                event.invoke(this.listener);
            }
        }

        public void iterationFinished(IterationFinishedEvent<T> event) {
            if (!this.released && this.needsIterationFinishedEvent) {
                FlagSet flagsToNotify = this.flagsBuilder.build();
                this.flagsBuilder = new FlagSet.Builder();
                this.needsIterationFinishedEvent = false;
                event.invoke(this.listener, flagsToNotify);
            }
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            return this.listener.equals(((ListenerHolder) other).listener);
        }

        public int hashCode() {
            return this.listener.hashCode();
        }
    }
}
