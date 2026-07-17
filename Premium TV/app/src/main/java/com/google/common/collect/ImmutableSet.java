package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collector;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
    private static final int CUTOFF = 751619276;
    private static final double DESIRED_LOAD_FACTOR = 0.7d;
    static final int MAX_TABLE_SIZE = 1073741824;
    private static final long serialVersionUID = 912559;

    @CheckForNull
    @LazyInit
    private transient ImmutableList<E> asList;

    public static class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {
        private int hashCode;

        @CheckForNull
        Object[] hashTable;

        public Builder() {
            super(4);
        }

        Builder(int i) {
            super(i);
            this.hashTable = new Object[ImmutableSet.chooseTableSize(i)];
        }

        private void addDeduping(E e) {
            Objects.requireNonNull(this.hashTable);
            int length = this.hashTable.length;
            int iHashCode = e.hashCode();
            int iSmear = Hashing.smear(iHashCode);
            while (true) {
                int i = iSmear & (length - 1);
                Object obj = this.hashTable[i];
                if (obj == null) {
                    this.hashTable[i] = e;
                    this.hashCode += iHashCode;
                    super.add((Object) e);
                    return;
                } else if (obj.equals(e)) {
                    return;
                } else {
                    iSmear = i + 1;
                }
            }
        }

        @Override // com.google.common.collect.ImmutableCollection.ArrayBasedBuilder, com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> add(E e) {
            Preconditions.checkNotNull(e);
            if (this.hashTable == null || ImmutableSet.chooseTableSize(this.size) > this.hashTable.length) {
                this.hashTable = null;
                super.add((Object) e);
            } else {
                addDeduping(e);
            }
            return this;
        }

        @Override // com.google.common.collect.ImmutableCollection.ArrayBasedBuilder, com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> add(E... eArr) {
            if (this.hashTable != null) {
                for (E e : eArr) {
                    add((Object) e);
                }
            } else {
                super.add((Object[]) eArr);
            }
            return this;
        }

        @Override // com.google.common.collect.ImmutableCollection.ArrayBasedBuilder, com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> addAll(Iterable<? extends E> iterable) {
            Preconditions.checkNotNull(iterable);
            if (this.hashTable != null) {
                Iterator<? extends E> it = iterable.iterator();
                while (it.hasNext()) {
                    add((Object) it.next());
                }
            } else {
                super.addAll((Iterable) iterable);
            }
            return this;
        }

        @Override // com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> addAll(Iterator<? extends E> it) {
            Preconditions.checkNotNull(it);
            while (it.hasNext()) {
                add((Object) it.next());
            }
            return this;
        }

        @Override // com.google.common.collect.ImmutableCollection.Builder
        public ImmutableSet<E> build() {
            ImmutableSet<E> immutableSetConstruct;
            switch (this.size) {
                case 0:
                    return ImmutableSet.of();
                case 1:
                    return ImmutableSet.of(Objects.requireNonNull(this.contents[0]));
                default:
                    if (this.hashTable == null || ImmutableSet.chooseTableSize(this.size) != this.hashTable.length) {
                        immutableSetConstruct = ImmutableSet.construct(this.size, this.contents);
                        this.size = immutableSetConstruct.size();
                    } else {
                        immutableSetConstruct = new RegularImmutableSet<>(ImmutableSet.shouldTrim(this.size, this.contents.length) ? Arrays.copyOf(this.contents, this.size) : this.contents, this.hashCode, this.hashTable, this.hashTable.length - 1, this.size);
                    }
                    this.forceCopy = true;
                    this.hashTable = null;
                    return immutableSetConstruct;
            }
        }

        /* JADX WARN: Multi-variable type inference failed */
        Builder<E> combine(Builder<E> builder) {
            if (this.hashTable != null) {
                for (int i = 0; i < builder.size; i++) {
                    add(Objects.requireNonNull(builder.contents[i]));
                }
            } else {
                addAll(builder.contents, builder.size);
            }
            return this;
        }
    }

    private static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final Object[] elements;

        SerializedForm(Object[] objArr) {
            this.elements = objArr;
        }

        Object readResolve() {
            return ImmutableSet.copyOf(this.elements);
        }
    }

    ImmutableSet() {
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static <E> Builder<E> builderWithExpectedSize(int i) {
        CollectPreconditions.checkNonnegative(i, "expectedSize");
        return new Builder<>(i);
    }

    static int chooseTableSize(int i) {
        int iMax = Math.max(i, 2);
        if (iMax >= CUTOFF) {
            Preconditions.checkArgument(iMax < 1073741824, "collection too large");
            return 1073741824;
        }
        int iHighestOneBit = Integer.highestOneBit(iMax - 1);
        do {
            iHighestOneBit <<= 1;
        } while (((double) iHighestOneBit) * DESIRED_LOAD_FACTOR < iMax);
        return iHighestOneBit;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <E> ImmutableSet<E> construct(int i, Object... objArr) {
        switch (i) {
            case 0:
                return of();
            case 1:
                return of(Objects.requireNonNull(objArr[0]));
            default:
                int iChooseTableSize = chooseTableSize(i);
                Object[] objArr2 = new Object[iChooseTableSize];
                int i2 = iChooseTableSize - 1;
                int i3 = 0;
                int i4 = 0;
                for (int i5 = 0; i5 < i; i5++) {
                    Object objCheckElementNotNull = ObjectArrays.checkElementNotNull(objArr[i5], i5);
                    int iHashCode = objCheckElementNotNull.hashCode();
                    int iSmear = Hashing.smear(iHashCode);
                    while (true) {
                        int i6 = iSmear & i2;
                        Object obj = objArr2[i6];
                        if (obj == null) {
                            objArr[i3] = objCheckElementNotNull;
                            objArr2[i6] = objCheckElementNotNull;
                            i4 += iHashCode;
                            i3++;
                        }
                        if (obj.equals(objCheckElementNotNull)) {
                        }
                        iSmear++;
                        break;
                        break;
                    }
                }
                Arrays.fill(objArr, i3, i, (Object) null);
                if (i3 == 1) {
                    return new SingletonImmutableSet(Objects.requireNonNull(objArr[0]));
                }
                if (chooseTableSize(i3) < iChooseTableSize / 2) {
                    return construct(i3, objArr);
                }
                return new RegularImmutableSet(shouldTrim(i3, objArr.length) ? Arrays.copyOf(objArr, i3) : objArr, i4, objArr2, i2, i3);
        }
    }

    public static <E> ImmutableSet<E> copyOf(Iterable<? extends E> iterable) {
        return iterable instanceof Collection ? copyOf((Collection) iterable) : copyOf(iterable.iterator());
    }

    public static <E> ImmutableSet<E> copyOf(Collection<? extends E> collection) {
        if ((collection instanceof ImmutableSet) && !(collection instanceof SortedSet)) {
            ImmutableSet<E> immutableSet = (ImmutableSet) collection;
            if (!immutableSet.isPartialView()) {
                return immutableSet;
            }
        }
        Object[] array = collection.toArray();
        return construct(array.length, array);
    }

    public static <E> ImmutableSet<E> copyOf(Iterator<? extends E> it) {
        if (!it.hasNext()) {
            return of();
        }
        E next = it.next();
        return !it.hasNext() ? of((Object) next) : new Builder().add((Object) next).addAll((Iterator) it).build();
    }

    public static <E> ImmutableSet<E> copyOf(E[] eArr) {
        switch (eArr.length) {
            case 0:
                return of();
            case 1:
                return of((Object) eArr[0]);
            default:
                return construct(eArr.length, (Object[]) eArr.clone());
        }
    }

    public static <E> ImmutableSet<E> of() {
        return RegularImmutableSet.EMPTY;
    }

    public static <E> ImmutableSet<E> of(E e) {
        return new SingletonImmutableSet(e);
    }

    public static <E> ImmutableSet<E> of(E e, E e2) {
        return construct(2, e, e2);
    }

    public static <E> ImmutableSet<E> of(E e, E e2, E e3) {
        return construct(3, e, e2, e3);
    }

    public static <E> ImmutableSet<E> of(E e, E e2, E e3, E e4) {
        return construct(4, e, e2, e3, e4);
    }

    public static <E> ImmutableSet<E> of(E e, E e2, E e3, E e4, E e5) {
        return construct(5, e, e2, e3, e4, e5);
    }

    @SafeVarargs
    public static <E> ImmutableSet<E> of(E e, E e2, E e3, E e4, E e5, E e6, E... eArr) {
        Preconditions.checkArgument(eArr.length <= 2147483641, "the total number of elements must fit in an int");
        Object[] objArr = new Object[eArr.length + 6];
        objArr[0] = e;
        objArr[1] = e2;
        objArr[2] = e3;
        objArr[3] = e4;
        objArr[4] = e5;
        objArr[5] = e6;
        System.arraycopy(eArr, 0, objArr, 6, eArr.length);
        return construct(objArr.length, objArr);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean shouldTrim(int i, int i2) {
        return i < (i2 >> 1) + (i2 >> 2);
    }

    static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
        return CollectCollectors.toImmutableSet();
    }

    @Override // com.google.common.collect.ImmutableCollection
    public ImmutableList<E> asList() {
        ImmutableList<E> immutableList = this.asList;
        if (immutableList != null) {
            return immutableList;
        }
        ImmutableList<E> immutableListCreateAsList = createAsList();
        this.asList = immutableListCreateAsList;
        return immutableListCreateAsList;
    }

    ImmutableList<E> createAsList() {
        return ImmutableList.asImmutableList(toArray());
    }

    @Override // java.util.Collection, java.util.Set
    public boolean equals(@CheckForNull Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj instanceof ImmutableSet) && isHashCodeFast() && ((ImmutableSet) obj).isHashCodeFast() && hashCode() != obj.hashCode()) {
            return false;
        }
        return Sets.equalsImpl(this, obj);
    }

    @Override // java.util.Collection, java.util.Set
    public int hashCode() {
        return Sets.hashCodeImpl(this);
    }

    boolean isHashCodeFast() {
        return false;
    }

    @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet, com.google.common.collect.SortedIterable
    public abstract UnmodifiableIterator<E> iterator();

    @Override // com.google.common.collect.ImmutableCollection
    Object writeReplace() {
        return new SerializedForm(toArray());
    }
}
