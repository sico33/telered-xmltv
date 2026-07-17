package com.google.common.collect;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class ForwardingSortedMultiset<E> extends ForwardingMultiset<E> implements SortedMultiset<E> {

    protected abstract class StandardDescendingMultiset extends DescendingMultiset<E> {
        final ForwardingSortedMultiset this$0;

        public StandardDescendingMultiset(ForwardingSortedMultiset forwardingSortedMultiset) {
            this.this$0 = forwardingSortedMultiset;
        }

        @Override // com.google.common.collect.DescendingMultiset
        SortedMultiset<E> forwardMultiset() {
            return this.this$0;
        }
    }

    protected class StandardElementSet extends SortedMultisets.NavigableElementSet<E> {
        public StandardElementSet(ForwardingSortedMultiset forwardingSortedMultiset) {
            super(forwardingSortedMultiset);
        }
    }

    protected ForwardingSortedMultiset() {
    }

    @Override // com.google.common.collect.SortedMultiset, com.google.common.collect.SortedIterable
    public Comparator<? super E> comparator() {
        return delegate().comparator();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.google.common.collect.ForwardingMultiset, com.google.common.collect.ForwardingCollection, com.google.common.collect.ForwardingObject
    public abstract SortedMultiset<E> delegate();

    @Override // com.google.common.collect.SortedMultiset
    public SortedMultiset<E> descendingMultiset() {
        return delegate().descendingMultiset();
    }

    @Override // com.google.common.collect.ForwardingMultiset, com.google.common.collect.Multiset
    public NavigableSet<E> elementSet() {
        return delegate().elementSet();
    }

    @Override // com.google.common.collect.SortedMultiset
    @CheckForNull
    public Multiset.Entry<E> firstEntry() {
        return delegate().firstEntry();
    }

    @Override // com.google.common.collect.SortedMultiset
    public SortedMultiset<E> headMultiset(@ParametricNullness E e, BoundType boundType) {
        return delegate().headMultiset(e, boundType);
    }

    @Override // com.google.common.collect.SortedMultiset
    @CheckForNull
    public Multiset.Entry<E> lastEntry() {
        return delegate().lastEntry();
    }

    @Override // com.google.common.collect.SortedMultiset
    @CheckForNull
    public Multiset.Entry<E> pollFirstEntry() {
        return delegate().pollFirstEntry();
    }

    @Override // com.google.common.collect.SortedMultiset
    @CheckForNull
    public Multiset.Entry<E> pollLastEntry() {
        return delegate().pollLastEntry();
    }

    @CheckForNull
    protected Multiset.Entry<E> standardFirstEntry() {
        Iterator<Multiset.Entry<E>> it = entrySet().iterator();
        if (!it.hasNext()) {
            return null;
        }
        Multiset.Entry<E> next = it.next();
        return Multisets.immutableEntry(next.getElement(), next.getCount());
    }

    @CheckForNull
    protected Multiset.Entry<E> standardLastEntry() {
        Iterator<Multiset.Entry<E>> it = descendingMultiset().entrySet().iterator();
        if (!it.hasNext()) {
            return null;
        }
        Multiset.Entry<E> next = it.next();
        return Multisets.immutableEntry(next.getElement(), next.getCount());
    }

    @CheckForNull
    protected Multiset.Entry<E> standardPollFirstEntry() {
        Iterator<Multiset.Entry<E>> it = entrySet().iterator();
        if (!it.hasNext()) {
            return null;
        }
        Multiset.Entry<E> next = it.next();
        Multiset.Entry<E> entryImmutableEntry = Multisets.immutableEntry(next.getElement(), next.getCount());
        it.remove();
        return entryImmutableEntry;
    }

    @CheckForNull
    protected Multiset.Entry<E> standardPollLastEntry() {
        Iterator<Multiset.Entry<E>> it = descendingMultiset().entrySet().iterator();
        if (!it.hasNext()) {
            return null;
        }
        Multiset.Entry<E> next = it.next();
        Multiset.Entry<E> entryImmutableEntry = Multisets.immutableEntry(next.getElement(), next.getCount());
        it.remove();
        return entryImmutableEntry;
    }

    protected SortedMultiset<E> standardSubMultiset(@ParametricNullness E e, BoundType boundType, @ParametricNullness E e2, BoundType boundType2) {
        return tailMultiset(e, boundType).headMultiset(e2, boundType2);
    }

    @Override // com.google.common.collect.SortedMultiset
    public SortedMultiset<E> subMultiset(@ParametricNullness E e, BoundType boundType, @ParametricNullness E e2, BoundType boundType2) {
        return delegate().subMultiset(e, boundType, e2, boundType2);
    }

    @Override // com.google.common.collect.SortedMultiset
    public SortedMultiset<E> tailMultiset(@ParametricNullness E e, BoundType boundType) {
        return delegate().tailMultiset(e, boundType);
    }
}
