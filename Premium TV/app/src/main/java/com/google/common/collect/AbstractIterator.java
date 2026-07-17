package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class AbstractIterator<T> extends UnmodifiableIterator<T> {

    @CheckForNull
    private T next;
    private State state = State.NOT_READY;

    /* JADX INFO: renamed from: com.google.common.collect.AbstractIterator$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final int[] $SwitchMap$com$google$common$collect$AbstractIterator$State = new int[State.values().length];

        static {
            try {
                $SwitchMap$com$google$common$collect$AbstractIterator$State[State.DONE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$common$collect$AbstractIterator$State[State.READY.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private enum State {
        READY,
        NOT_READY,
        DONE,
        FAILED
    }

    protected AbstractIterator() {
    }

    private boolean tryToComputeNext() {
        this.state = State.FAILED;
        this.next = computeNext();
        if (this.state == State.DONE) {
            return false;
        }
        this.state = State.READY;
        return true;
    }

    @CheckForNull
    protected abstract T computeNext();

    @CheckForNull
    protected final T endOfData() {
        this.state = State.DONE;
        return null;
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        Preconditions.checkState(this.state != State.FAILED);
        switch (AnonymousClass1.$SwitchMap$com$google$common$collect$AbstractIterator$State[this.state.ordinal()]) {
            case 1:
                return false;
            case 2:
                return true;
            default:
                return tryToComputeNext();
        }
    }

    @Override // java.util.Iterator
    @ParametricNullness
    public final T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        this.state = State.NOT_READY;
        T t = (T) NullnessCasts.uncheckedCastNullableTToT(this.next);
        this.next = null;
        return t;
    }

    @ParametricNullness
    public final T peek() {
        if (hasNext()) {
            return (T) NullnessCasts.uncheckedCastNullableTToT(this.next);
        }
        throw new NoSuchElementException();
    }
}
