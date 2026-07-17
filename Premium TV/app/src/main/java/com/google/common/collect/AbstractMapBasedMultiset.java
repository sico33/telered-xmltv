package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class AbstractMapBasedMultiset<E> extends AbstractMultiset<E> implements Serializable {
    private static final long serialVersionUID = 0;
    transient ObjectCountHashMap<E> backingMap;
    transient long size;

    abstract class Itr<T> implements Iterator<T> {
        int entryIndex;
        int expectedModCount;
        final AbstractMapBasedMultiset this$0;
        int toRemove = -1;

        Itr(AbstractMapBasedMultiset abstractMapBasedMultiset) {
            this.this$0 = abstractMapBasedMultiset;
            this.entryIndex = this.this$0.backingMap.firstIndex();
            this.expectedModCount = this.this$0.backingMap.modCount;
        }

        private void checkForConcurrentModification() {
            if (this.this$0.backingMap.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            checkForConcurrentModification();
            return this.entryIndex >= 0;
        }

        @Override // java.util.Iterator
        @ParametricNullness
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T tResult = result(this.entryIndex);
            this.toRemove = this.entryIndex;
            this.entryIndex = this.this$0.backingMap.nextIndex(this.entryIndex);
            return tResult;
        }

        @Override // java.util.Iterator
        public void remove() {
            checkForConcurrentModification();
            CollectPreconditions.checkRemove(this.toRemove != -1);
            this.this$0.size -= (long) this.this$0.backingMap.removeEntry(this.toRemove);
            this.entryIndex = this.this$0.backingMap.nextIndexAfterRemove(this.entryIndex, this.toRemove);
            this.toRemove = -1;
            this.expectedModCount = this.this$0.backingMap.modCount;
        }

        @ParametricNullness
        abstract T result(int i);
    }

    AbstractMapBasedMultiset(int i) {
        this.backingMap = newBackingMap(i);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        int count = Serialization.readCount(objectInputStream);
        this.backingMap = newBackingMap(3);
        Serialization.populateMultiset(this, objectInputStream, count);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        Serialization.writeMultiset(this, objectOutputStream);
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public final int add(@ParametricNullness E e, int i) {
        if (i == 0) {
            return count(e);
        }
        Preconditions.checkArgument(i > 0, "occurrences cannot be negative: %s", i);
        int iIndexOf = this.backingMap.indexOf(e);
        ObjectCountHashMap<E> objectCountHashMap = this.backingMap;
        if (iIndexOf == -1) {
            objectCountHashMap.put(e, i);
            this.size += (long) i;
            return 0;
        }
        int value = objectCountHashMap.getValue(iIndexOf);
        long j = ((long) value) + ((long) i);
        Preconditions.checkArgument(j <= 2147483647L, "too many occurrences: %s", j);
        this.backingMap.setValue(iIndexOf, (int) j);
        this.size += (long) i;
        return value;
    }

    void addTo(Multiset<? super E> multiset) {
        Preconditions.checkNotNull(multiset);
        int iFirstIndex = this.backingMap.firstIndex();
        while (iFirstIndex >= 0) {
            multiset.add(this.backingMap.getKey(iFirstIndex), this.backingMap.getValue(iFirstIndex));
            iFirstIndex = this.backingMap.nextIndex(iFirstIndex);
        }
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
    public final void clear() {
        this.backingMap.clear();
        this.size = 0L;
    }

    @Override // com.google.common.collect.Multiset
    public final int count(@CheckForNull Object obj) {
        return this.backingMap.get(obj);
    }

    @Override // com.google.common.collect.AbstractMultiset
    final int distinctElements() {
        return this.backingMap.size();
    }

    @Override // com.google.common.collect.AbstractMultiset
    final Iterator<E> elementIterator() {
        return new AbstractMapBasedMultiset<E>.Itr<E>(this) { // from class: com.google.common.collect.AbstractMapBasedMultiset.1
            final AbstractMapBasedMultiset this$0;

            {
                this.this$0 = this;
            }

            @Override // com.google.common.collect.AbstractMapBasedMultiset.Itr
            @ParametricNullness
            E result(int i) {
                return this.this$0.backingMap.getKey(i);
            }
        };
    }

    @Override // com.google.common.collect.AbstractMultiset
    final Iterator<Multiset.Entry<E>> entryIterator() {
        return new AbstractMapBasedMultiset<E>.Itr<Multiset.Entry<E>>(this) { // from class: com.google.common.collect.AbstractMapBasedMultiset.2
            final AbstractMapBasedMultiset this$0;

            {
                this.this$0 = this;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.google.common.collect.AbstractMapBasedMultiset.Itr
            public Multiset.Entry<E> result(int i) {
                return this.this$0.backingMap.getEntry(i);
            }
        };
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, com.google.common.collect.Multiset
    public final Iterator<E> iterator() {
        return Multisets.iteratorImpl(this);
    }

    abstract ObjectCountHashMap<E> newBackingMap(int i);

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public final int remove(@CheckForNull Object obj, int i) {
        if (i == 0) {
            return count(obj);
        }
        Preconditions.checkArgument(i > 0, "occurrences cannot be negative: %s", i);
        int iIndexOf = this.backingMap.indexOf(obj);
        if (iIndexOf == -1) {
            return 0;
        }
        int value = this.backingMap.getValue(iIndexOf);
        ObjectCountHashMap<E> objectCountHashMap = this.backingMap;
        if (value > i) {
            objectCountHashMap.setValue(iIndexOf, value - i);
        } else {
            objectCountHashMap.removeEntry(iIndexOf);
            i = value;
        }
        this.size -= (long) i;
        return value;
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public final int setCount(@ParametricNullness E e, int i) {
        CollectPreconditions.checkNonnegative(i, "count");
        ObjectCountHashMap<E> objectCountHashMap = this.backingMap;
        int iRemove = i == 0 ? objectCountHashMap.remove(e) : objectCountHashMap.put(e, i);
        this.size += (long) (i - iRemove);
        return iRemove;
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public final boolean setCount(@ParametricNullness E e, int i, int i2) {
        CollectPreconditions.checkNonnegative(i, "oldCount");
        CollectPreconditions.checkNonnegative(i2, "newCount");
        int iIndexOf = this.backingMap.indexOf(e);
        if (iIndexOf == -1) {
            if (i != 0) {
                return false;
            }
            if (i2 > 0) {
                this.backingMap.put(e, i2);
                this.size += (long) i2;
            }
            return true;
        }
        if (this.backingMap.getValue(iIndexOf) != i) {
            return false;
        }
        ObjectCountHashMap<E> objectCountHashMap = this.backingMap;
        if (i2 == 0) {
            objectCountHashMap.removeEntry(iIndexOf);
            this.size -= (long) i;
        } else {
            objectCountHashMap.setValue(iIndexOf, i2);
            this.size += (long) (i2 - i);
        }
        return true;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public final int size() {
        return Ints.saturatedCast(this.size);
    }
}
