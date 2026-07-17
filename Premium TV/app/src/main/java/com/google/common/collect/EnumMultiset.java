package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.Enum;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class EnumMultiset<E extends Enum<E>> extends AbstractMultiset<E> implements Serializable {
    private static final long serialVersionUID = 0;
    private transient int[] counts;
    private transient int distinctElements;
    private transient E[] enumConstants;
    private transient long size;
    private transient Class<E> type;

    /* JADX INFO: renamed from: com.google.common.collect.EnumMultiset$2, reason: invalid class name */
    class AnonymousClass2 extends EnumMultiset<E>.Itr<Multiset.Entry<E>> {
        final EnumMultiset this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass2(EnumMultiset enumMultiset) {
            super(enumMultiset);
            this.this$0 = enumMultiset;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.EnumMultiset.Itr
        public Multiset.Entry<E> output(int i) {
            return new Multisets.AbstractEntry<E>(this, i) { // from class: com.google.common.collect.EnumMultiset.2.1
                final AnonymousClass2 this$1;
                final int val$index;

                {
                    this.this$1 = this;
                    this.val$index = i;
                }

                @Override // com.google.common.collect.Multiset.Entry
                public int getCount() {
                    return this.this$1.this$0.counts[this.val$index];
                }

                @Override // com.google.common.collect.Multiset.Entry
                public E getElement() {
                    return (E) this.this$1.this$0.enumConstants[this.val$index];
                }
            };
        }
    }

    abstract class Itr<T> implements Iterator<T> {
        final EnumMultiset this$0;
        int index = 0;
        int toRemove = -1;

        Itr(EnumMultiset enumMultiset) {
            this.this$0 = enumMultiset;
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            while (this.index < this.this$0.enumConstants.length) {
                if (this.this$0.counts[this.index] > 0) {
                    return true;
                }
                this.index++;
            }
            return false;
        }

        @Override // java.util.Iterator
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T tOutput = output(this.index);
            this.toRemove = this.index;
            this.index++;
            return tOutput;
        }

        abstract T output(int i);

        @Override // java.util.Iterator
        public void remove() {
            CollectPreconditions.checkRemove(this.toRemove >= 0);
            if (this.this$0.counts[this.toRemove] > 0) {
                EnumMultiset.access$210(this.this$0);
                EnumMultiset.access$322(this.this$0, this.this$0.counts[this.toRemove]);
                this.this$0.counts[this.toRemove] = 0;
            }
            this.toRemove = -1;
        }
    }

    private EnumMultiset(Class<E> cls) {
        this.type = cls;
        Preconditions.checkArgument(cls.isEnum());
        this.enumConstants = cls.getEnumConstants();
        this.counts = new int[this.enumConstants.length];
    }

    static /* synthetic */ int access$210(EnumMultiset enumMultiset) {
        int i = enumMultiset.distinctElements;
        enumMultiset.distinctElements = i - 1;
        return i;
    }

    static /* synthetic */ long access$322(EnumMultiset enumMultiset, long j) {
        long j2 = enumMultiset.size - j;
        enumMultiset.size = j2;
        return j2;
    }

    private void checkIsE(Object obj) {
        Preconditions.checkNotNull(obj);
        if (!isActuallyE(obj)) {
            throw new ClassCastException("Expected an " + this.type + " but got " + obj);
        }
    }

    public static <E extends Enum<E>> EnumMultiset<E> create(Class<E> cls) {
        return new EnumMultiset<>(cls);
    }

    public static <E extends Enum<E>> EnumMultiset<E> create(Iterable<E> iterable) {
        Iterator<E> it = iterable.iterator();
        Preconditions.checkArgument(it.hasNext(), "EnumMultiset constructor passed empty Iterable");
        EnumMultiset<E> enumMultiset = new EnumMultiset<>(it.next().getDeclaringClass());
        Iterables.addAll(enumMultiset, iterable);
        return enumMultiset;
    }

    public static <E extends Enum<E>> EnumMultiset<E> create(Iterable<E> iterable, Class<E> cls) {
        EnumMultiset<E> enumMultisetCreate = create(cls);
        Iterables.addAll(enumMultisetCreate, iterable);
        return enumMultisetCreate;
    }

    private boolean isActuallyE(@CheckForNull Object obj) {
        Enum r4;
        int iOrdinal;
        return (obj instanceof Enum) && (iOrdinal = (r4 = (Enum) obj).ordinal()) < this.enumConstants.length && this.enumConstants[iOrdinal] == r4;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.type = (Class) Objects.requireNonNull(objectInputStream.readObject());
        this.enumConstants = this.type.getEnumConstants();
        this.counts = new int[this.enumConstants.length];
        Serialization.populateMultiset(this, objectInputStream);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(this.type);
        Serialization.writeMultiset(this, objectOutputStream);
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public int add(E e, int i) {
        checkIsE(e);
        CollectPreconditions.checkNonnegative(i, "occurrences");
        if (i == 0) {
            return count(e);
        }
        int iOrdinal = e.ordinal();
        int i2 = this.counts[iOrdinal];
        long j = ((long) i2) + ((long) i);
        Preconditions.checkArgument(j <= 2147483647L, "too many occurrences: %s", j);
        this.counts[iOrdinal] = (int) j;
        if (i2 == 0) {
            this.distinctElements++;
        }
        this.size += (long) i;
        return i2;
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
    public void clear() {
        Arrays.fill(this.counts, 0);
        this.size = 0L;
        this.distinctElements = 0;
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ boolean contains(@CheckForNull Object obj) {
        return super.contains(obj);
    }

    @Override // com.google.common.collect.Multiset
    public int count(@CheckForNull Object obj) {
        if (obj == null || !isActuallyE(obj)) {
            return 0;
        }
        return this.counts[((Enum) obj).ordinal()];
    }

    @Override // com.google.common.collect.AbstractMultiset
    int distinctElements() {
        return this.distinctElements;
    }

    @Override // com.google.common.collect.AbstractMultiset
    Iterator<E> elementIterator() {
        return new EnumMultiset<E>.Itr<E>(this) { // from class: com.google.common.collect.EnumMultiset.1
            final EnumMultiset this$0;

            {
                this.this$0 = this;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.google.common.collect.EnumMultiset.Itr
            public E output(int i) {
                return (E) this.this$0.enumConstants[i];
            }
        };
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ Set elementSet() {
        return super.elementSet();
    }

    @Override // com.google.common.collect.AbstractMultiset
    Iterator<Multiset.Entry<E>> entryIterator() {
        return new AnonymousClass2(this);
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ Set entrySet() {
        return super.entrySet();
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
    public /* bridge */ /* synthetic */ boolean isEmpty() {
        return super.isEmpty();
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, com.google.common.collect.Multiset
    public Iterator<E> iterator() {
        return Multisets.iteratorImpl(this);
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public int remove(@CheckForNull Object obj, int i) {
        if (obj == null || !isActuallyE(obj)) {
            return 0;
        }
        Enum r0 = (Enum) obj;
        CollectPreconditions.checkNonnegative(i, "occurrences");
        if (i == 0) {
            return count(obj);
        }
        int iOrdinal = r0.ordinal();
        int i2 = this.counts[iOrdinal];
        if (i2 == 0) {
            return 0;
        }
        int[] iArr = this.counts;
        if (i2 > i) {
            iArr[iOrdinal] = i2 - i;
            this.size -= (long) i;
            return i2;
        }
        iArr[iOrdinal] = 0;
        this.distinctElements--;
        this.size -= (long) i2;
        return i2;
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public int setCount(E e, int i) {
        checkIsE(e);
        CollectPreconditions.checkNonnegative(i, "count");
        int iOrdinal = e.ordinal();
        int i2 = this.counts[iOrdinal];
        this.counts[iOrdinal] = i;
        this.size += (long) (i - i2);
        if (i2 == 0 && i > 0) {
            this.distinctElements++;
        } else if (i2 > 0 && i == 0) {
            this.distinctElements--;
        }
        return i2;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ boolean setCount(@ParametricNullness Object obj, int i, int i2) {
        return super.setCount(obj, i, i2);
    }

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public int size() {
        return Ints.saturatedCast(this.size);
    }
}
