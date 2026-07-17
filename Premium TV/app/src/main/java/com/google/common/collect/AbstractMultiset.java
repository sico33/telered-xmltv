package com.google.common.collect;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class AbstractMultiset<E> extends AbstractCollection<E> implements Multiset<E> {

    @CheckForNull
    @LazyInit
    private transient Set<E> elementSet;

    @CheckForNull
    @LazyInit
    private transient Set<Multiset.Entry<E>> entrySet;

    class ElementSet extends Multisets.ElementSet<E> {
        final AbstractMultiset this$0;

        ElementSet(AbstractMultiset abstractMultiset) {
            this.this$0 = abstractMultiset;
        }

        @Override // com.google.common.collect.Multisets.ElementSet, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<E> iterator() {
            return this.this$0.elementIterator();
        }

        @Override // com.google.common.collect.Multisets.ElementSet
        Multiset<E> multiset() {
            return this.this$0;
        }
    }

    class EntrySet extends Multisets.EntrySet<E> {
        final AbstractMultiset this$0;

        EntrySet(AbstractMultiset abstractMultiset) {
            this.this$0 = abstractMultiset;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<Multiset.Entry<E>> iterator() {
            return this.this$0.entryIterator();
        }

        @Override // com.google.common.collect.Multisets.EntrySet
        Multiset<E> multiset() {
            return this.this$0;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.this$0.distinctElements();
        }
    }

    AbstractMultiset() {
    }

    public int add(@ParametricNullness E e, int i) {
        throw new UnsupportedOperationException();
    }

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public final boolean add(@ParametricNullness E e) {
        add(e, 1);
        return true;
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public final boolean addAll(Collection<? extends E> collection) {
        return Multisets.addAllImpl(this, collection);
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public abstract void clear();

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public boolean contains(@CheckForNull Object obj) {
        return count(obj) > 0;
    }

    Set<E> createElementSet() {
        return new ElementSet(this);
    }

    Set<Multiset.Entry<E>> createEntrySet() {
        return new EntrySet(this);
    }

    abstract int distinctElements();

    abstract Iterator<E> elementIterator();

    @Override // com.google.common.collect.Multiset
    public Set<E> elementSet() {
        Set<E> set = this.elementSet;
        if (set != null) {
            return set;
        }
        Set<E> setCreateElementSet = createElementSet();
        this.elementSet = setCreateElementSet;
        return setCreateElementSet;
    }

    abstract Iterator<Multiset.Entry<E>> entryIterator();

    @Override // com.google.common.collect.Multiset
    public Set<Multiset.Entry<E>> entrySet() {
        Set<Multiset.Entry<E>> set = this.entrySet;
        if (set != null) {
            return set;
        }
        Set<Multiset.Entry<E>> setCreateEntrySet = createEntrySet();
        this.entrySet = setCreateEntrySet;
        return setCreateEntrySet;
    }

    @Override // java.util.Collection, com.google.common.collect.Multiset
    public final boolean equals(@CheckForNull Object obj) {
        return Multisets.equalsImpl(this, obj);
    }

    @Override // java.util.Collection, com.google.common.collect.Multiset
    public final int hashCode() {
        return entrySet().hashCode();
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public boolean isEmpty() {
        return entrySet().isEmpty();
    }

    public int remove(@CheckForNull Object obj, int i) {
        throw new UnsupportedOperationException();
    }

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public final boolean remove(@CheckForNull Object obj) {
        return remove(obj, 1) > 0;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public final boolean removeAll(Collection<?> collection) {
        return Multisets.removeAllImpl(this, collection);
    }

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public final boolean retainAll(Collection<?> collection) {
        return Multisets.retainAllImpl(this, collection);
    }

    public int setCount(@ParametricNullness E e, int i) {
        return Multisets.setCountImpl(this, e, i);
    }

    public boolean setCount(@ParametricNullness E e, int i, int i2) {
        return Multisets.setCountImpl(this, e, i, i2);
    }

    @Override // java.util.AbstractCollection, com.google.common.collect.Multiset
    public final String toString() {
        return entrySet().toString();
    }
}
