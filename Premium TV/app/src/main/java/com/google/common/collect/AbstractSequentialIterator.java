package com.google.common.collect;

import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class AbstractSequentialIterator<T> extends UnmodifiableIterator<T> {

    @CheckForNull
    private T nextOrNull;

    protected AbstractSequentialIterator(@CheckForNull T t) {
        this.nextOrNull = t;
    }

    @CheckForNull
    protected abstract T computeNext(T t);

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.nextOrNull != null;
    }

    @Override // java.util.Iterator
    public final T next() {
        if (this.nextOrNull == null) {
            throw new NoSuchElementException();
        }
        T t = this.nextOrNull;
        this.nextOrNull = computeNext(t);
        return t;
    }
}
