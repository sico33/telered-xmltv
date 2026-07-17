package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class ConcurrentHashMultiset<E> extends AbstractMultiset<E> implements Serializable {
    private static final long serialVersionUID = 1;
    private final transient ConcurrentMap<E, AtomicInteger> countMap;

    private class EntrySet extends AbstractMultiset<E>.EntrySet {
        final ConcurrentHashMultiset this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        private EntrySet(ConcurrentHashMultiset concurrentHashMultiset) {
            super(concurrentHashMultiset);
            this.this$0 = concurrentHashMultiset;
        }

        private List<Multiset.Entry<E>> snapshot() {
            ArrayList arrayListNewArrayListWithExpectedSize = Lists.newArrayListWithExpectedSize(size());
            Iterators.addAll(arrayListNewArrayListWithExpectedSize, iterator());
            return arrayListNewArrayListWithExpectedSize;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.AbstractMultiset.EntrySet, com.google.common.collect.Multisets.EntrySet
        public ConcurrentHashMultiset<E> multiset() {
            return this.this$0;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public Object[] toArray() {
            return snapshot().toArray();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public <T> T[] toArray(T[] tArr) {
            return (T[]) snapshot().toArray(tArr);
        }
    }

    private static class FieldSettersHolder {
        static final Serialization.FieldSetter<ConcurrentHashMultiset> COUNT_MAP_FIELD_SETTER = Serialization.getFieldSetter(ConcurrentHashMultiset.class, "countMap");

        private FieldSettersHolder() {
        }
    }

    ConcurrentHashMultiset(ConcurrentMap<E, AtomicInteger> concurrentMap) {
        Preconditions.checkArgument(concurrentMap.isEmpty(), "the backing map (%s) must be empty", concurrentMap);
        this.countMap = concurrentMap;
    }

    public static <E> ConcurrentHashMultiset<E> create() {
        return new ConcurrentHashMultiset<>(new ConcurrentHashMap());
    }

    public static <E> ConcurrentHashMultiset<E> create(Iterable<? extends E> iterable) {
        ConcurrentHashMultiset<E> concurrentHashMultisetCreate = create();
        Iterables.addAll(concurrentHashMultisetCreate, iterable);
        return concurrentHashMultisetCreate;
    }

    public static <E> ConcurrentHashMultiset<E> create(ConcurrentMap<E, AtomicInteger> concurrentMap) {
        return new ConcurrentHashMultiset<>(concurrentMap);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        FieldSettersHolder.COUNT_MAP_FIELD_SETTER.set(this, (ConcurrentMap) Objects.requireNonNull(objectInputStream.readObject()));
    }

    /* JADX WARN: Multi-variable type inference failed */
    private List<E> snapshot() {
        ArrayList arrayListNewArrayListWithExpectedSize = Lists.newArrayListWithExpectedSize(size());
        for (Multiset.Entry entry : entrySet()) {
            Object element = entry.getElement();
            for (int count = entry.getCount(); count > 0; count--) {
                arrayListNewArrayListWithExpectedSize.add(element);
            }
        }
        return arrayListNewArrayListWithExpectedSize;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(this.countMap);
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public int add(E e, int i) {
        AtomicInteger atomicIntegerPutIfAbsent;
        int i2;
        AtomicInteger atomicInteger;
        Preconditions.checkNotNull(e);
        if (i == 0) {
            return count(e);
        }
        CollectPreconditions.checkPositive(i, "occurrences");
        do {
            atomicIntegerPutIfAbsent = (AtomicInteger) Maps.safeGet(this.countMap, e);
            if (atomicIntegerPutIfAbsent == null && (atomicIntegerPutIfAbsent = this.countMap.putIfAbsent(e, new AtomicInteger(i))) == null) {
                return 0;
            }
            do {
                i2 = atomicIntegerPutIfAbsent.get();
                if (i2 == 0) {
                    atomicInteger = new AtomicInteger(i);
                    if (this.countMap.putIfAbsent(e, atomicInteger) == null) {
                        break;
                    }
                } else {
                    try {
                    } catch (ArithmeticException e2) {
                        throw new IllegalArgumentException("Overflow adding " + i + " occurrences to a count of " + i2);
                    }
                }
            } while (!atomicIntegerPutIfAbsent.compareAndSet(i2, IntMath.checkedAdd(i2, i)));
            return i2;
        } while (!this.countMap.replace(e, atomicIntegerPutIfAbsent, atomicInteger));
        return 0;
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
    public void clear() {
        this.countMap.clear();
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ boolean contains(@CheckForNull Object obj) {
        return super.contains(obj);
    }

    @Override // com.google.common.collect.Multiset
    public int count(@CheckForNull Object obj) {
        AtomicInteger atomicInteger = (AtomicInteger) Maps.safeGet(this.countMap, obj);
        if (atomicInteger == null) {
            return 0;
        }
        return atomicInteger.get();
    }

    @Override // com.google.common.collect.AbstractMultiset
    Set<E> createElementSet() {
        return new ForwardingSet<E>(this, this.countMap.keySet()) { // from class: com.google.common.collect.ConcurrentHashMultiset.1
            final Set val$delegate;

            {
                this.val$delegate = set;
            }

            @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                return obj != null && Collections2.safeContains(this.val$delegate, obj);
            }

            @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
            public boolean containsAll(Collection<?> collection) {
                return standardContainsAll(collection);
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // com.google.common.collect.ForwardingSet, com.google.common.collect.ForwardingCollection, com.google.common.collect.ForwardingObject
            public Set<E> delegate() {
                return this.val$delegate;
            }

            @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
            public boolean remove(@CheckForNull Object obj) {
                return obj != null && Collections2.safeRemove(this.val$delegate, obj);
            }

            @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
            public boolean removeAll(Collection<?> collection) {
                return standardRemoveAll(collection);
            }
        };
    }

    @Override // com.google.common.collect.AbstractMultiset
    @Deprecated
    public Set<Multiset.Entry<E>> createEntrySet() {
        return new EntrySet();
    }

    @Override // com.google.common.collect.AbstractMultiset
    int distinctElements() {
        return this.countMap.size();
    }

    @Override // com.google.common.collect.AbstractMultiset
    Iterator<E> elementIterator() {
        throw new AssertionError("should never be called");
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ Set elementSet() {
        return super.elementSet();
    }

    @Override // com.google.common.collect.AbstractMultiset
    Iterator<Multiset.Entry<E>> entryIterator() {
        return new ForwardingIterator<Multiset.Entry<E>>(this, new AbstractIterator<Multiset.Entry<E>>(this) { // from class: com.google.common.collect.ConcurrentHashMultiset.2
            private final Iterator<Map.Entry<E, AtomicInteger>> mapEntries;
            final ConcurrentHashMultiset this$0;

            {
                this.this$0 = this;
                this.mapEntries = this.this$0.countMap.entrySet().iterator();
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // com.google.common.collect.AbstractIterator
            @CheckForNull
            public Multiset.Entry<E> computeNext() {
                while (this.mapEntries.hasNext()) {
                    Map.Entry<E, AtomicInteger> next = this.mapEntries.next();
                    int i = next.getValue().get();
                    if (i != 0) {
                        return Multisets.immutableEntry(next.getKey(), i);
                    }
                }
                return endOfData();
            }
        }) { // from class: com.google.common.collect.ConcurrentHashMultiset.3

            @CheckForNull
            private Multiset.Entry<E> last;
            final ConcurrentHashMultiset this$0;
            final Iterator val$readOnlyIterator;

            {
                this.this$0 = this;
                this.val$readOnlyIterator = it;
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // com.google.common.collect.ForwardingIterator, com.google.common.collect.ForwardingObject
            public Iterator<Multiset.Entry<E>> delegate() {
                return this.val$readOnlyIterator;
            }

            @Override // com.google.common.collect.ForwardingIterator, java.util.Iterator
            public Multiset.Entry<E> next() {
                this.last = (Multiset.Entry) super.next();
                return this.last;
            }

            @Override // com.google.common.collect.ForwardingIterator, java.util.Iterator
            public void remove() {
                Preconditions.checkState(this.last != null, "no calls to next() since the last call to remove()");
                this.this$0.setCount(this.last.getElement(), 0);
                this.last = null;
            }
        };
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ Set entrySet() {
        return super.entrySet();
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
    public boolean isEmpty() {
        return this.countMap.isEmpty();
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, com.google.common.collect.Multiset
    public Iterator<E> iterator() {
        return Multisets.iteratorImpl(this);
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public int remove(@CheckForNull Object obj, int i) {
        int i2;
        int iMax;
        if (i == 0) {
            return count(obj);
        }
        CollectPreconditions.checkPositive(i, "occurrences");
        AtomicInteger atomicInteger = (AtomicInteger) Maps.safeGet(this.countMap, obj);
        if (atomicInteger == null) {
            return 0;
        }
        do {
            i2 = atomicInteger.get();
            if (i2 == 0) {
                return 0;
            }
            iMax = Math.max(0, i2 - i);
        } while (!atomicInteger.compareAndSet(i2, iMax));
        if (iMax == 0) {
            this.countMap.remove(obj, atomicInteger);
        }
        return i2;
    }

    public boolean removeExactly(@CheckForNull Object obj, int i) {
        int i2;
        int i3;
        if (i == 0) {
            return true;
        }
        CollectPreconditions.checkPositive(i, "occurrences");
        AtomicInteger atomicInteger = (AtomicInteger) Maps.safeGet(this.countMap, obj);
        if (atomicInteger == null) {
            return false;
        }
        do {
            i2 = atomicInteger.get();
            if (i2 < i) {
                return false;
            }
            i3 = i2 - i;
        } while (!atomicInteger.compareAndSet(i2, i3));
        if (i3 == 0) {
            this.countMap.remove(obj, atomicInteger);
        }
        return true;
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public int setCount(E e, int i) {
        AtomicInteger atomicIntegerPutIfAbsent;
        AtomicInteger atomicInteger;
        Preconditions.checkNotNull(e);
        CollectPreconditions.checkNonnegative(i, "count");
        do {
            atomicIntegerPutIfAbsent = (AtomicInteger) Maps.safeGet(this.countMap, e);
            if (atomicIntegerPutIfAbsent != null || (i != 0 && (atomicIntegerPutIfAbsent = this.countMap.putIfAbsent(e, new AtomicInteger(i))) != null)) {
                while (true) {
                    int i2 = atomicIntegerPutIfAbsent.get();
                    if (i2 == 0) {
                        break;
                    }
                    if (atomicIntegerPutIfAbsent.compareAndSet(i2, i)) {
                        if (i == 0) {
                            this.countMap.remove(e, atomicIntegerPutIfAbsent);
                        }
                        return i2;
                    }
                }
                if (i != 0) {
                    atomicInteger = new AtomicInteger(i);
                    if (this.countMap.putIfAbsent(e, atomicInteger) == null) {
                        break;
                    }
                } else {
                    return 0;
                }
            }
            return 0;
        } while (!this.countMap.replace(e, atomicIntegerPutIfAbsent, atomicInteger));
        return 0;
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public boolean setCount(E e, int i, int i2) {
        Preconditions.checkNotNull(e);
        CollectPreconditions.checkNonnegative(i, "oldCount");
        CollectPreconditions.checkNonnegative(i2, "newCount");
        AtomicInteger atomicInteger = (AtomicInteger) Maps.safeGet(this.countMap, e);
        if (atomicInteger == null) {
            if (i != 0) {
                return false;
            }
            if (i2 == 0) {
                return true;
            }
            return this.countMap.putIfAbsent(e, new AtomicInteger(i2)) == null;
        }
        int i3 = atomicInteger.get();
        if (i3 != i) {
            return false;
        }
        if (i3 == 0) {
            if (i2 == 0) {
                this.countMap.remove(e, atomicInteger);
                return true;
            }
            AtomicInteger atomicInteger2 = new AtomicInteger(i2);
            return this.countMap.putIfAbsent(e, atomicInteger2) == null || this.countMap.replace(e, atomicInteger, atomicInteger2);
        }
        if (!atomicInteger.compareAndSet(i3, i2)) {
            return false;
        }
        if (i2 == 0) {
            this.countMap.remove(e, atomicInteger);
        }
        return true;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public int size() {
        long j = 0;
        Iterator<AtomicInteger> it = this.countMap.values().iterator();
        while (true) {
            long j2 = j;
            if (!it.hasNext()) {
                return Ints.saturatedCast(j2);
            }
            j = ((long) it.next().get()) + j2;
        }
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public Object[] toArray() {
        return snapshot().toArray();
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public <T> T[] toArray(T[] tArr) {
        return (T[]) snapshot().toArray(tArr);
    }
}
