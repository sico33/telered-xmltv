package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class RegularImmutableSortedSet<E> extends ImmutableSortedSet<E> {
    static final RegularImmutableSortedSet<Comparable> NATURAL_EMPTY_SET = new RegularImmutableSortedSet<>(ImmutableList.of(), Ordering.natural());
    final transient ImmutableList<E> elements;

    RegularImmutableSortedSet(ImmutableList<E> immutableList, Comparator<? super E> comparator) {
        super(comparator);
        this.elements = immutableList;
    }

    private int unsafeBinarySearch(Object obj) throws ClassCastException {
        return Collections.binarySearch(this.elements, obj, unsafeComparator());
    }

    @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
    public ImmutableList<E> asList() {
        return this.elements;
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
    @CheckForNull
    public E ceiling(E e) {
        int iTailIndex = tailIndex(e, true);
        if (iTailIndex == size()) {
            return null;
        }
        return this.elements.get(iTailIndex);
    }

    @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean contains(@CheckForNull Object obj) {
        if (obj == null) {
            return false;
        }
        try {
            return unsafeBinarySearch(obj) >= 0;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean containsAll(Collection<?> collection) {
        if (collection instanceof Multiset) {
            collection = ((Multiset) collection).elementSet();
        }
        if (!SortedIterables.hasSameComparator(comparator(), collection) || collection.size() <= 1) {
            return super.containsAll(collection);
        }
        UnmodifiableIterator<E> it = iterator();
        Iterator<?> it2 = collection.iterator();
        if (!it.hasNext()) {
            return false;
        }
        Object next = it2.next();
        E next2 = it.next();
        while (true) {
            try {
                int iUnsafeCompare = unsafeCompare(next2, next);
                if (iUnsafeCompare < 0) {
                    if (!it.hasNext()) {
                        return false;
                    }
                    next2 = it.next();
                } else if (iUnsafeCompare == 0) {
                    if (!it2.hasNext()) {
                        return true;
                    }
                    next = it2.next();
                } else if (iUnsafeCompare > 0) {
                    return false;
                }
            } catch (ClassCastException | NullPointerException e) {
                return false;
            }
        }
    }

    @Override // com.google.common.collect.ImmutableCollection
    int copyIntoArray(Object[] objArr, int i) {
        return this.elements.copyIntoArray(objArr, i);
    }

    @Override // com.google.common.collect.ImmutableSortedSet
    ImmutableSortedSet<E> createDescendingSet() {
        Comparator comparatorReverseOrder = Collections.reverseOrder(this.comparator);
        return isEmpty() ? emptySet(comparatorReverseOrder) : new RegularImmutableSortedSet(this.elements.reverse(), comparatorReverseOrder);
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
    public UnmodifiableIterator<E> descendingIterator() {
        return this.elements.reverse().iterator();
    }

    @Override // com.google.common.collect.ImmutableSet, java.util.Collection, java.util.Set
    public boolean equals(@CheckForNull Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Set)) {
            return false;
        }
        Set set = (Set) obj;
        if (size() != set.size()) {
            return false;
        }
        if (isEmpty()) {
            return true;
        }
        if (!SortedIterables.hasSameComparator(this.comparator, set)) {
            return containsAll(set);
        }
        Iterator<E> it = set.iterator();
        try {
            UnmodifiableIterator<E> it2 = iterator();
            while (it2.hasNext()) {
                E next = it2.next();
                E next2 = it.next();
                if (next2 == null || unsafeCompare(next, next2) != 0) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        } catch (NoSuchElementException e2) {
            return false;
        }
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.SortedSet
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return this.elements.get(0);
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
    @CheckForNull
    public E floor(E e) {
        int iHeadIndex = headIndex(e, true) - 1;
        if (iHeadIndex == -1) {
            return null;
        }
        return this.elements.get(iHeadIndex);
    }

    RegularImmutableSortedSet<E> getSubSet(int i, int i2) {
        if (i == 0 && i2 == size()) {
            return this;
        }
        return i < i2 ? new RegularImmutableSortedSet<>(this.elements.subList(i, i2), this.comparator) : emptySet(this.comparator);
    }

    int headIndex(E e, boolean z) {
        int iBinarySearch = Collections.binarySearch(this.elements, Preconditions.checkNotNull(e), comparator());
        if (iBinarySearch >= 0) {
            return z ? iBinarySearch + 1 : iBinarySearch;
        }
        return iBinarySearch ^ (-1);
    }

    @Override // com.google.common.collect.ImmutableSortedSet
    ImmutableSortedSet<E> headSetImpl(E e, boolean z) {
        return getSubSet(0, headIndex(e, z));
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
    @CheckForNull
    public E higher(E e) {
        int iTailIndex = tailIndex(e, false);
        if (iTailIndex == size()) {
            return null;
        }
        return this.elements.get(iTailIndex);
    }

    @Override // com.google.common.collect.ImmutableSortedSet
    int indexOf(@CheckForNull Object obj) {
        if (obj == null) {
            return -1;
        }
        try {
            int iBinarySearch = Collections.binarySearch(this.elements, obj, unsafeComparator());
            if (iBinarySearch < 0) {
                iBinarySearch = -1;
            }
            return iBinarySearch;
        } catch (ClassCastException e) {
            return -1;
        }
    }

    @Override // com.google.common.collect.ImmutableCollection
    @CheckForNull
    Object[] internalArray() {
        return this.elements.internalArray();
    }

    @Override // com.google.common.collect.ImmutableCollection
    int internalArrayEnd() {
        return this.elements.internalArrayEnd();
    }

    @Override // com.google.common.collect.ImmutableCollection
    int internalArrayStart() {
        return this.elements.internalArrayStart();
    }

    @Override // com.google.common.collect.ImmutableCollection
    boolean isPartialView() {
        return this.elements.isPartialView();
    }

    @Override // com.google.common.collect.ImmutableSortedSet, com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet, com.google.common.collect.SortedIterable
    public UnmodifiableIterator<E> iterator() {
        return this.elements.iterator();
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.SortedSet
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return this.elements.get(size() - 1);
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
    @CheckForNull
    public E lower(E e) {
        int iHeadIndex = headIndex(e, false) - 1;
        if (iHeadIndex == -1) {
            return null;
        }
        return this.elements.get(iHeadIndex);
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
    public int size() {
        return this.elements.size();
    }

    @Override // com.google.common.collect.ImmutableSortedSet
    ImmutableSortedSet<E> subSetImpl(E e, boolean z, E e2, boolean z2) {
        return tailSetImpl(e, z).headSetImpl(e2, z2);
    }

    int tailIndex(E e, boolean z) {
        int iBinarySearch = Collections.binarySearch(this.elements, Preconditions.checkNotNull(e), comparator());
        if (iBinarySearch >= 0) {
            return z ? iBinarySearch : iBinarySearch + 1;
        }
        return iBinarySearch ^ (-1);
    }

    @Override // com.google.common.collect.ImmutableSortedSet
    ImmutableSortedSet<E> tailSetImpl(E e, boolean z) {
        return getSubSet(tailIndex(e, z), size());
    }

    Comparator<Object> unsafeComparator() {
        return this.comparator;
    }

    @Override // com.google.common.collect.ImmutableSortedSet, com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
    Object writeReplace() {
        return super.writeReplace();
    }
}
