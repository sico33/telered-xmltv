package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.DoNotMock;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@DoNotMock("Use ImmutableList.of or another implementation")
@ElementTypesAreNonnullByDefault
public abstract class ImmutableCollection<E> extends AbstractCollection<E> implements Serializable {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final long serialVersionUID = 912559;

    static abstract class ArrayBasedBuilder<E> extends Builder<E> {
        Object[] contents;
        boolean forceCopy;
        int size;

        ArrayBasedBuilder(int i) {
            CollectPreconditions.checkNonnegative(i, "initialCapacity");
            this.contents = new Object[i];
            this.size = 0;
        }

        private void getReadyToExpandTo(int i) {
            if (this.contents.length < i) {
                this.contents = Arrays.copyOf(this.contents, expandedCapacity(this.contents.length, i));
                this.forceCopy = false;
            } else if (this.forceCopy) {
                this.contents = (Object[]) this.contents.clone();
                this.forceCopy = false;
            }
        }

        @Override // com.google.common.collect.ImmutableCollection.Builder
        public ArrayBasedBuilder<E> add(E e) {
            Preconditions.checkNotNull(e);
            getReadyToExpandTo(this.size + 1);
            Object[] objArr = this.contents;
            int i = this.size;
            this.size = i + 1;
            objArr[i] = e;
            return this;
        }

        @Override // com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> add(E... eArr) {
            addAll(eArr, eArr.length);
            return this;
        }

        /* JADX WARN: Code duplicated, block: B:8:0x0022  */
        @Override // com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> addAll(Iterable<? extends E> iterable) {
            if (iterable instanceof Collection) {
                Collection collection = (Collection) iterable;
                getReadyToExpandTo(this.size + collection.size());
                if (collection instanceof ImmutableCollection) {
                    this.size = ((ImmutableCollection) collection).copyIntoArray(this.contents, this.size);
                } else {
                    super.addAll(iterable);
                }
            } else {
                super.addAll(iterable);
            }
            return this;
        }

        final void addAll(Object[] objArr, int i) {
            ObjectArrays.checkElementsNotNull(objArr, i);
            getReadyToExpandTo(this.size + i);
            System.arraycopy(objArr, 0, this.contents, this.size, i);
            this.size += i;
        }
    }

    @DoNotMock
    public static abstract class Builder<E> {
        static final int DEFAULT_INITIAL_CAPACITY = 4;

        Builder() {
        }

        static int expandedCapacity(int i, int i2) {
            if (i2 < 0) {
                throw new AssertionError("cannot store more than MAX_VALUE elements");
            }
            int iHighestOneBit = (i >> 1) + i + 1;
            if (iHighestOneBit < i2) {
                iHighestOneBit = Integer.highestOneBit(i2 - 1) << 1;
            }
            if (iHighestOneBit < 0) {
                return Integer.MAX_VALUE;
            }
            return iHighestOneBit;
        }

        public abstract Builder<E> add(E e);

        public Builder<E> add(E... eArr) {
            for (E e : eArr) {
                add(e);
            }
            return this;
        }

        public Builder<E> addAll(Iterable<? extends E> iterable) {
            Iterator<? extends E> it = iterable.iterator();
            while (it.hasNext()) {
                add(it.next());
            }
            return this;
        }

        public Builder<E> addAll(Iterator<? extends E> it) {
            while (it.hasNext()) {
                add(it.next());
            }
            return this;
        }

        public abstract ImmutableCollection<E> build();
    }

    ImmutableCollection() {
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    @Deprecated
    public final boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    @Deprecated
    public final boolean addAll(Collection<? extends E> collection) {
        throw new UnsupportedOperationException();
    }

    public ImmutableList<E> asList() {
        return isEmpty() ? ImmutableList.of() : ImmutableList.asImmutableList(toArray());
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    @Deprecated
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
    public abstract boolean contains(@CheckForNull Object obj);

    int copyIntoArray(Object[] objArr, int i) {
        UnmodifiableIterator<E> it = iterator();
        while (it.hasNext()) {
            objArr[i] = it.next();
            i++;
        }
        return i;
    }

    @CheckForNull
    Object[] internalArray() {
        return null;
    }

    int internalArrayEnd() {
        throw new UnsupportedOperationException();
    }

    int internalArrayStart() {
        throw new UnsupportedOperationException();
    }

    abstract boolean isPartialView();

    @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet, com.google.common.collect.SortedIterable
    public abstract UnmodifiableIterator<E> iterator();

    @Override // java.util.AbstractCollection, java.util.Collection
    @Deprecated
    public final boolean remove(@CheckForNull Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    @Deprecated
    public final boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    @Deprecated
    public final boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public final Object[] toArray() {
        return toArray(EMPTY_ARRAY);
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public final <T> T[] toArray(T[] tArr) {
        Preconditions.checkNotNull(tArr);
        int size = size();
        if (tArr.length < size) {
            Object[] objArrInternalArray = internalArray();
            if (objArrInternalArray != null) {
                return (T[]) Platform.copy(objArrInternalArray, internalArrayStart(), internalArrayEnd(), tArr);
            }
            tArr = (T[]) ObjectArrays.newArray(tArr, size);
        } else if (tArr.length > size) {
            tArr[size] = null;
        }
        copyIntoArray(tArr, 0);
        return tArr;
    }

    Object writeReplace() {
        return new ImmutableList.SerializedForm(toArray());
    }
}
