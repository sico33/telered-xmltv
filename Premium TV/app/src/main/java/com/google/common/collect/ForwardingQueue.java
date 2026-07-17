package com.google.common.collect;

import java.util.NoSuchElementException;
import java.util.Queue;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class ForwardingQueue<E> extends ForwardingCollection<E> implements Queue<E> {
    protected ForwardingQueue() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.google.common.collect.ForwardingCollection, com.google.common.collect.ForwardingObject
    public abstract Queue<E> delegate();

    @Override // java.util.Queue
    @ParametricNullness
    public E element() {
        return delegate().element();
    }

    public boolean offer(@ParametricNullness E e) {
        return delegate().offer(e);
    }

    @Override // java.util.Queue
    @CheckForNull
    public E peek() {
        return delegate().peek();
    }

    @Override // java.util.Queue
    @CheckForNull
    public E poll() {
        return delegate().poll();
    }

    @Override // java.util.Queue
    @ParametricNullness
    public E remove() {
        return delegate().remove();
    }

    protected boolean standardOffer(@ParametricNullness E e) {
        try {
            return add(e);
        } catch (IllegalStateException e2) {
            return false;
        }
    }

    @CheckForNull
    protected E standardPeek() {
        try {
            return element();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @CheckForNull
    protected E standardPoll() {
        try {
            return remove();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
