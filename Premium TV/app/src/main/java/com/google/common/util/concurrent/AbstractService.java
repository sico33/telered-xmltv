package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class AbstractService implements Service {
    private static final ListenerCallQueue.Event<Service.Listener> STARTING_EVENT = new ListenerCallQueue.Event<Service.Listener>() { // from class: com.google.common.util.concurrent.AbstractService.1
        @Override // com.google.common.util.concurrent.ListenerCallQueue.Event
        public void call(Service.Listener listener) {
            listener.starting();
        }

        public String toString() {
            return "starting()";
        }
    };
    private static final ListenerCallQueue.Event<Service.Listener> RUNNING_EVENT = new ListenerCallQueue.Event<Service.Listener>() { // from class: com.google.common.util.concurrent.AbstractService.2
        @Override // com.google.common.util.concurrent.ListenerCallQueue.Event
        public void call(Service.Listener listener) {
            listener.running();
        }

        public String toString() {
            return "running()";
        }
    };
    private static final ListenerCallQueue.Event<Service.Listener> STOPPING_FROM_STARTING_EVENT = stoppingEvent(Service.State.STARTING);
    private static final ListenerCallQueue.Event<Service.Listener> STOPPING_FROM_RUNNING_EVENT = stoppingEvent(Service.State.RUNNING);
    private static final ListenerCallQueue.Event<Service.Listener> TERMINATED_FROM_NEW_EVENT = terminatedEvent(Service.State.NEW);
    private static final ListenerCallQueue.Event<Service.Listener> TERMINATED_FROM_STARTING_EVENT = terminatedEvent(Service.State.STARTING);
    private static final ListenerCallQueue.Event<Service.Listener> TERMINATED_FROM_RUNNING_EVENT = terminatedEvent(Service.State.RUNNING);
    private static final ListenerCallQueue.Event<Service.Listener> TERMINATED_FROM_STOPPING_EVENT = terminatedEvent(Service.State.STOPPING);
    private final Monitor monitor = new Monitor();
    private final Monitor.Guard isStartable = new IsStartableGuard(this);
    private final Monitor.Guard isStoppable = new IsStoppableGuard(this);
    private final Monitor.Guard hasReachedRunning = new HasReachedRunningGuard(this);
    private final Monitor.Guard isStopped = new IsStoppedGuard(this);
    private final ListenerCallQueue<Service.Listener> listeners = new ListenerCallQueue<>();
    private volatile StateSnapshot snapshot = new StateSnapshot(Service.State.NEW);

    /* JADX INFO: renamed from: com.google.common.util.concurrent.AbstractService$6, reason: invalid class name */
    static /* synthetic */ class AnonymousClass6 {
        static final int[] $SwitchMap$com$google$common$util$concurrent$Service$State = new int[Service.State.values().length];

        static {
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.NEW.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.STARTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.RUNNING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.STOPPING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.TERMINATED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.FAILED.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private final class HasReachedRunningGuard extends Monitor.Guard {
        final AbstractService this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        HasReachedRunningGuard(AbstractService abstractService) {
            super(abstractService.monitor);
            this.this$0 = abstractService;
        }

        @Override // com.google.common.util.concurrent.Monitor.Guard
        public boolean isSatisfied() {
            return this.this$0.state().compareTo(Service.State.RUNNING) >= 0;
        }
    }

    private final class IsStartableGuard extends Monitor.Guard {
        final AbstractService this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        IsStartableGuard(AbstractService abstractService) {
            super(abstractService.monitor);
            this.this$0 = abstractService;
        }

        @Override // com.google.common.util.concurrent.Monitor.Guard
        public boolean isSatisfied() {
            return this.this$0.state() == Service.State.NEW;
        }
    }

    private final class IsStoppableGuard extends Monitor.Guard {
        final AbstractService this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        IsStoppableGuard(AbstractService abstractService) {
            super(abstractService.monitor);
            this.this$0 = abstractService;
        }

        @Override // com.google.common.util.concurrent.Monitor.Guard
        public boolean isSatisfied() {
            return this.this$0.state().compareTo(Service.State.RUNNING) <= 0;
        }
    }

    private final class IsStoppedGuard extends Monitor.Guard {
        final AbstractService this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        IsStoppedGuard(AbstractService abstractService) {
            super(abstractService.monitor);
            this.this$0 = abstractService;
        }

        @Override // com.google.common.util.concurrent.Monitor.Guard
        public boolean isSatisfied() {
            return this.this$0.state().compareTo(Service.State.TERMINATED) >= 0;
        }
    }

    private static final class StateSnapshot {

        @CheckForNull
        final Throwable failure;
        final boolean shutdownWhenStartupFinishes;
        final Service.State state;

        StateSnapshot(Service.State state) {
            this(state, false, null);
        }

        StateSnapshot(Service.State state, boolean z, @CheckForNull Throwable th) {
            Preconditions.checkArgument(!z || state == Service.State.STARTING, "shutdownWhenStartupFinishes can only be set if state is STARTING. Got %s instead.", state);
            Preconditions.checkArgument((th != null) == (state == Service.State.FAILED), "A failure cause should be set if and only if the state is failed.  Got %s and %s instead.", state, th);
            this.state = state;
            this.shutdownWhenStartupFinishes = z;
            this.failure = th;
        }

        Service.State externalState() {
            return (this.shutdownWhenStartupFinishes && this.state == Service.State.STARTING) ? Service.State.STOPPING : this.state;
        }

        Throwable failureCause() {
            Preconditions.checkState(this.state == Service.State.FAILED, "failureCause() is only valid if the service has failed, service is %s", this.state);
            return (Throwable) Objects.requireNonNull(this.failure);
        }
    }

    protected AbstractService() {
    }

    private void checkCurrentState(Service.State state) {
        Service.State state2 = state();
        if (state2 != state) {
            if (state2 != Service.State.FAILED) {
                throw new IllegalStateException("Expected the service " + this + " to be " + state + ", but was " + state2);
            }
            throw new IllegalStateException("Expected the service " + this + " to be " + state + ", but the service has FAILED", failureCause());
        }
    }

    private void dispatchListenerEvents() throws Exception {
        if (this.monitor.isOccupiedByCurrentThread()) {
            return;
        }
        this.listeners.dispatch();
    }

    private void enqueueFailedEvent(Service.State state, Throwable th) {
        this.listeners.enqueue(new ListenerCallQueue.Event<Service.Listener>(this, state, th) { // from class: com.google.common.util.concurrent.AbstractService.5
            final Throwable val$cause;
            final Service.State val$from;

            {
                this.val$from = state;
                this.val$cause = th;
            }

            @Override // com.google.common.util.concurrent.ListenerCallQueue.Event
            public void call(Service.Listener listener) {
                listener.failed(this.val$from, this.val$cause);
            }

            public String toString() {
                return "failed({from = " + this.val$from + ", cause = " + this.val$cause + "})";
            }
        });
    }

    private void enqueueRunningEvent() {
        this.listeners.enqueue(RUNNING_EVENT);
    }

    private void enqueueStartingEvent() {
        this.listeners.enqueue(STARTING_EVENT);
    }

    private void enqueueStoppingEvent(Service.State state) {
        if (state == Service.State.STARTING) {
            this.listeners.enqueue(STOPPING_FROM_STARTING_EVENT);
        } else {
            if (state != Service.State.RUNNING) {
                throw new AssertionError();
            }
            this.listeners.enqueue(STOPPING_FROM_RUNNING_EVENT);
        }
    }

    private void enqueueTerminatedEvent(Service.State state) {
        switch (AnonymousClass6.$SwitchMap$com$google$common$util$concurrent$Service$State[state.ordinal()]) {
            case 1:
                this.listeners.enqueue(TERMINATED_FROM_NEW_EVENT);
                return;
            case 2:
                this.listeners.enqueue(TERMINATED_FROM_STARTING_EVENT);
                return;
            case 3:
                this.listeners.enqueue(TERMINATED_FROM_RUNNING_EVENT);
                return;
            case 4:
                this.listeners.enqueue(TERMINATED_FROM_STOPPING_EVENT);
                return;
            case 5:
            case 6:
                throw new AssertionError();
            default:
                return;
        }
    }

    private static ListenerCallQueue.Event<Service.Listener> stoppingEvent(Service.State state) {
        return new ListenerCallQueue.Event<Service.Listener>(state) { // from class: com.google.common.util.concurrent.AbstractService.4
            final Service.State val$from;

            {
                this.val$from = state;
            }

            @Override // com.google.common.util.concurrent.ListenerCallQueue.Event
            public void call(Service.Listener listener) {
                listener.stopping(this.val$from);
            }

            public String toString() {
                return "stopping({from = " + this.val$from + "})";
            }
        };
    }

    private static ListenerCallQueue.Event<Service.Listener> terminatedEvent(Service.State state) {
        return new ListenerCallQueue.Event<Service.Listener>(state) { // from class: com.google.common.util.concurrent.AbstractService.3
            final Service.State val$from;

            {
                this.val$from = state;
            }

            @Override // com.google.common.util.concurrent.ListenerCallQueue.Event
            public void call(Service.Listener listener) {
                listener.terminated(this.val$from);
            }

            public String toString() {
                return "terminated({from = " + this.val$from + "})";
            }
        };
    }

    @Override // com.google.common.util.concurrent.Service
    public final void addListener(Service.Listener listener, Executor executor) {
        this.listeners.addListener(listener, executor);
    }

    @Override // com.google.common.util.concurrent.Service
    public final void awaitRunning() {
        this.monitor.enterWhenUninterruptibly(this.hasReachedRunning);
        try {
            checkCurrentState(Service.State.RUNNING);
        } finally {
            this.monitor.leave();
        }
    }

    @Override // com.google.common.util.concurrent.Service
    public final void awaitRunning(long j, TimeUnit timeUnit) throws TimeoutException {
        if (!this.monitor.enterWhenUninterruptibly(this.hasReachedRunning, j, timeUnit)) {
            throw new TimeoutException("Timed out waiting for " + this + " to reach the RUNNING state.");
        }
        try {
            checkCurrentState(Service.State.RUNNING);
        } finally {
            this.monitor.leave();
        }
    }

    @Override // com.google.common.util.concurrent.Service
    public final void awaitTerminated() {
        this.monitor.enterWhenUninterruptibly(this.isStopped);
        try {
            checkCurrentState(Service.State.TERMINATED);
        } finally {
            this.monitor.leave();
        }
    }

    @Override // com.google.common.util.concurrent.Service
    public final void awaitTerminated(long j, TimeUnit timeUnit) throws TimeoutException {
        if (!this.monitor.enterWhenUninterruptibly(this.isStopped, j, timeUnit)) {
            throw new TimeoutException("Timed out waiting for " + this + " to reach a terminal state. Current state: " + state());
        }
        try {
            checkCurrentState(Service.State.TERMINATED);
        } finally {
            this.monitor.leave();
        }
    }

    protected void doCancelStart() {
    }

    protected abstract void doStart();

    protected abstract void doStop();

    @Override // com.google.common.util.concurrent.Service
    public final Throwable failureCause() {
        return this.snapshot.failureCause();
    }

    @Override // com.google.common.util.concurrent.Service
    public final boolean isRunning() {
        return state() == Service.State.RUNNING;
    }

    protected final void notifyFailed(Throwable th) throws Exception {
        Preconditions.checkNotNull(th);
        this.monitor.enter();
        try {
            Service.State state = state();
            switch (AnonymousClass6.$SwitchMap$com$google$common$util$concurrent$Service$State[state.ordinal()]) {
                case 1:
                case 5:
                    throw new IllegalStateException("Failed while in state:" + state, th);
                case 2:
                case 3:
                case 4:
                    this.snapshot = new StateSnapshot(Service.State.FAILED, false, th);
                    enqueueFailedEvent(state, th);
                    break;
            }
            this.monitor.leave();
            dispatchListenerEvents();
        } catch (Throwable th2) {
            this.monitor.leave();
            dispatchListenerEvents();
            throw th2;
        }
    }

    protected final void notifyStarted() throws Exception {
        this.monitor.enter();
        try {
            if (this.snapshot.state != Service.State.STARTING) {
                IllegalStateException illegalStateException = new IllegalStateException("Cannot notifyStarted() when the service is " + this.snapshot.state);
                notifyFailed(illegalStateException);
                throw illegalStateException;
            }
            if (this.snapshot.shutdownWhenStartupFinishes) {
                this.snapshot = new StateSnapshot(Service.State.STOPPING);
                doStop();
            } else {
                this.snapshot = new StateSnapshot(Service.State.RUNNING);
                enqueueRunningEvent();
            }
            this.monitor.leave();
            dispatchListenerEvents();
        } catch (Throwable th) {
            this.monitor.leave();
            dispatchListenerEvents();
            throw th;
        }
    }

    protected final void notifyStopped() throws Exception {
        this.monitor.enter();
        try {
            Service.State state = state();
            switch (AnonymousClass6.$SwitchMap$com$google$common$util$concurrent$Service$State[state.ordinal()]) {
                case 1:
                case 5:
                case 6:
                    throw new IllegalStateException("Cannot notifyStopped() when the service is " + state);
                case 2:
                case 3:
                case 4:
                    this.snapshot = new StateSnapshot(Service.State.TERMINATED);
                    enqueueTerminatedEvent(state);
                    break;
            }
            this.monitor.leave();
            dispatchListenerEvents();
        } catch (Throwable th) {
            this.monitor.leave();
            dispatchListenerEvents();
            throw th;
        }
    }

    @Override // com.google.common.util.concurrent.Service
    public final Service startAsync() throws Exception {
        if (!this.monitor.enterIf(this.isStartable)) {
            throw new IllegalStateException("Service " + this + " has already been started");
        }
        try {
            this.snapshot = new StateSnapshot(Service.State.STARTING);
            enqueueStartingEvent();
            doStart();
        } catch (Throwable th) {
            try {
                Platform.restoreInterruptIfIsInterruptedException(th);
                notifyFailed(th);
            } finally {
                this.monitor.leave();
                dispatchListenerEvents();
            }
        }
        return this;
    }

    @Override // com.google.common.util.concurrent.Service
    public final Service.State state() {
        return this.snapshot.externalState();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // com.google.common.util.concurrent.Service
    public final Service stopAsync() throws Exception {
        if (this.monitor.enterIf(this.isStoppable)) {
            try {
                Service.State state = state();
                switch (AnonymousClass6.$SwitchMap$com$google$common$util$concurrent$Service$State[state.ordinal()]) {
                    case 1:
                        this.snapshot = new StateSnapshot(Service.State.TERMINATED);
                        enqueueTerminatedEvent(Service.State.NEW);
                        break;
                    case 2:
                        this.snapshot = new StateSnapshot(Service.State.STARTING, true, null);
                        enqueueStoppingEvent(Service.State.STARTING);
                        doCancelStart();
                        break;
                    case 3:
                        this.snapshot = new StateSnapshot(Service.State.STOPPING);
                        enqueueStoppingEvent(Service.State.RUNNING);
                        doStop();
                        break;
                    case 4:
                    case 5:
                    case 6:
                        throw new AssertionError("isStoppable is incorrectly implemented, saw: " + state);
                    default:
                        break;
                }
            } catch (Throwable th) {
                try {
                    Platform.restoreInterruptIfIsInterruptedException(th);
                    notifyFailed(th);
                } finally {
                    this.monitor.leave();
                    dispatchListenerEvents();
                }
            }
        }
        return this;
    }

    public String toString() {
        return getClass().getSimpleName() + " [" + state() + "]";
    }
}
