package com.google.common.collect;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class ForwardingSortedSet<E> extends ForwardingSet<E> implements SortedSet<E> {
    protected ForwardingSortedSet() {
    }

    @Override // java.util.SortedSet
    @CheckForNull
    public Comparator<? super E> comparator() {
        return delegate().comparator();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.google.common.collect.ForwardingSet, com.google.common.collect.ForwardingCollection, com.google.common.collect.ForwardingObject
    public abstract SortedSet<E> delegate();

    @Override // java.util.SortedSet
    @ParametricNullness
    public E first() {
        return delegate().first();
    }

    @Override // java.util.SortedSet
    public SortedSet<E> headSet(@ParametricNullness E e) {
        return delegate().headSet(e);
    }

    @Override // java.util.SortedSet
    @ParametricNullness
    public E last() {
        return delegate().last();
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.ForwardingCollection
    protected boolean standardContains(@CheckForNull Object obj) {
        try {
            return ForwardingSortedMap.unsafeCompare(comparator(), tailSet(obj).first(), obj) == 0;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        } catch (NoSuchElementException e3) {
            return false;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.ForwardingCollection
    protected boolean standardRemove(@CheckForNull Object obj) {
        try {
            Iterator<E> it = tailSet(obj).iterator();
            if (!it.hasNext()) {
                return false;
            }
            if (ForwardingSortedMap.unsafeCompare(comparator(), it.next(), obj) != 0) {
                return false;
            }
            it.remove();
            return true;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    protected SortedSet<E> standardSubSet(@ParametricNullness E e, @ParametricNullness E e2) {
        return tailSet(e).headSet(e2);
    }

    @Override // java.util.SortedSet
    public SortedSet<E> subSet(@ParametricNullness E e, @ParametricNullness E e2) {
        return delegate().subSet(e, e2);
    }

    @Override // java.util.SortedSet
    public SortedSet<E> tailSet(@ParametricNullness E e) {
        return delegate().tailSet(e);
    }
}
