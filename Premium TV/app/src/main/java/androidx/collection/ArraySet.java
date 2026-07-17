package androidx.collection;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class ArraySet<E> implements Collection<E>, Set<E> {
    private static final int BASE_SIZE = 4;
    private static final int CACHE_SIZE = 10;
    private static final boolean DEBUG = false;
    private static final String TAG = "ArraySet";
    private static Object[] sBaseCache;
    private static int sBaseCacheSize;
    private static Object[] sTwiceBaseCache;
    private static int sTwiceBaseCacheSize;
    Object[] mArray;
    private int[] mHashes;
    int mSize;
    private static final Object sBaseCacheLock = new Object();
    private static final Object sTwiceBaseCacheLock = new Object();

    private int binarySearch(int hash) {
        try {
            return ContainerHelpers.binarySearch(this.mHashes, this.mSize, hash);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }
    }

    private int indexOf(Object key, int hash) {
        int N = this.mSize;
        if (N == 0) {
            return -1;
        }
        int index = binarySearch(hash);
        if (index < 0 || key.equals(this.mArray[index])) {
            return index;
        }
        int end = index + 1;
        while (end < N && this.mHashes[end] == hash) {
            if (key.equals(this.mArray[end])) {
                return end;
            }
            end++;
        }
        for (int i = index - 1; i >= 0 && this.mHashes[i] == hash; i--) {
            if (key.equals(this.mArray[i])) {
                return i;
            }
        }
        int i2 = ~end;
        return i2;
    }

    private int indexOfNull() {
        int N = this.mSize;
        if (N == 0) {
            return -1;
        }
        int index = binarySearch(0);
        if (index < 0 || this.mArray[index] == null) {
            return index;
        }
        int end = index + 1;
        while (end < N && this.mHashes[end] == 0) {
            if (this.mArray[end] == null) {
                return end;
            }
            end++;
        }
        for (int i = index - 1; i >= 0 && this.mHashes[i] == 0; i--) {
            if (this.mArray[i] == null) {
                return i;
            }
        }
        int i2 = ~end;
        return i2;
    }

    private void allocArrays(int size) {
        if (size == 8) {
            synchronized (sTwiceBaseCacheLock) {
                if (sTwiceBaseCache != null) {
                    Object[] array = sTwiceBaseCache;
                    try {
                        this.mArray = array;
                        sTwiceBaseCache = (Object[]) array[0];
                        this.mHashes = (int[]) array[1];
                        if (this.mHashes != null) {
                            array[1] = null;
                            array[0] = null;
                            sTwiceBaseCacheSize--;
                            return;
                        }
                    } catch (ClassCastException e) {
                    }
                    System.out.println("ArraySet Found corrupt ArraySet cache: [0]=" + array[0] + " [1]=" + array[1]);
                    sTwiceBaseCache = null;
                    sTwiceBaseCacheSize = 0;
                }
            }
        } else if (size == 4) {
            synchronized (sBaseCacheLock) {
                if (sBaseCache != null) {
                    Object[] array2 = sBaseCache;
                    try {
                        this.mArray = array2;
                        sBaseCache = (Object[]) array2[0];
                        this.mHashes = (int[]) array2[1];
                        if (this.mHashes != null) {
                            array2[1] = null;
                            array2[0] = null;
                            sBaseCacheSize--;
                            return;
                        }
                    } catch (ClassCastException e2) {
                    }
                    System.out.println("ArraySet Found corrupt ArraySet cache: [0]=" + array2[0] + " [1]=" + array2[1]);
                    sBaseCache = null;
                    sBaseCacheSize = 0;
                }
            }
        }
        this.mHashes = new int[size];
        this.mArray = new Object[size];
    }

    private static void freeArrays(int[] hashes, Object[] array, int size) {
        if (hashes.length == 8) {
            synchronized (sTwiceBaseCacheLock) {
                if (sTwiceBaseCacheSize < 10) {
                    array[0] = sTwiceBaseCache;
                    array[1] = hashes;
                    for (int i = size - 1; i >= 2; i--) {
                        array[i] = null;
                    }
                    sTwiceBaseCache = array;
                    sTwiceBaseCacheSize++;
                }
            }
            return;
        }
        if (hashes.length == 4) {
            synchronized (sBaseCacheLock) {
                if (sBaseCacheSize < 10) {
                    array[0] = sBaseCache;
                    array[1] = hashes;
                    for (int i2 = size - 1; i2 >= 2; i2--) {
                        array[i2] = null;
                    }
                    sBaseCache = array;
                    sBaseCacheSize++;
                }
            }
        }
    }

    public ArraySet() {
        this(0);
    }

    public ArraySet(int capacity) {
        if (capacity == 0) {
            this.mHashes = ContainerHelpers.EMPTY_INTS;
            this.mArray = ContainerHelpers.EMPTY_OBJECTS;
        } else {
            allocArrays(capacity);
        }
        this.mSize = 0;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public ArraySet(ArraySet<E> arraySet) {
        this();
        if (arraySet != 0) {
            addAll((ArraySet) arraySet);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public ArraySet(Collection<E> collection) {
        this();
        if (collection != 0) {
            addAll(collection);
        }
    }

    public ArraySet(E[] array) {
        this();
        if (array != null) {
            for (E value : array) {
                add(value);
            }
        }
    }

    @Override // java.util.Collection, java.util.Set
    public void clear() {
        if (this.mSize != 0) {
            int[] ohashes = this.mHashes;
            Object[] oarray = this.mArray;
            int osize = this.mSize;
            this.mHashes = ContainerHelpers.EMPTY_INTS;
            this.mArray = ContainerHelpers.EMPTY_OBJECTS;
            this.mSize = 0;
            freeArrays(ohashes, oarray, osize);
        }
        if (this.mSize != 0) {
            throw new ConcurrentModificationException();
        }
    }

    public void ensureCapacity(int minimumCapacity) {
        int oSize = this.mSize;
        if (this.mHashes.length < minimumCapacity) {
            int[] ohashes = this.mHashes;
            Object[] oarray = this.mArray;
            allocArrays(minimumCapacity);
            if (this.mSize > 0) {
                System.arraycopy(ohashes, 0, this.mHashes, 0, this.mSize);
                System.arraycopy(oarray, 0, this.mArray, 0, this.mSize);
            }
            freeArrays(ohashes, oarray, this.mSize);
        }
        if (this.mSize != oSize) {
            throw new ConcurrentModificationException();
        }
    }

    @Override // java.util.Collection, java.util.Set
    public boolean contains(Object key) {
        return indexOf(key) >= 0;
    }

    public int indexOf(Object key) {
        return key == null ? indexOfNull() : indexOf(key, key.hashCode());
    }

    public E valueAt(int i) {
        return (E) this.mArray[i];
    }

    @Override // java.util.Collection, java.util.Set
    public boolean isEmpty() {
        return this.mSize <= 0;
    }

    @Override // java.util.Collection, java.util.Set
    public boolean add(E value) {
        int hash;
        int index;
        int oSize = this.mSize;
        if (value == null) {
            hash = 0;
            index = indexOfNull();
        } else {
            hash = value.hashCode();
            index = indexOf(value, hash);
        }
        if (index >= 0) {
            return false;
        }
        int index2 = ~index;
        if (oSize >= this.mHashes.length) {
            int n = 8;
            if (oSize >= 8) {
                n = (oSize >> 1) + oSize;
            } else if (oSize < 4) {
                n = 4;
            }
            int[] ohashes = this.mHashes;
            Object[] oarray = this.mArray;
            allocArrays(n);
            if (oSize != this.mSize) {
                throw new ConcurrentModificationException();
            }
            if (this.mHashes.length > 0) {
                System.arraycopy(ohashes, 0, this.mHashes, 0, ohashes.length);
                System.arraycopy(oarray, 0, this.mArray, 0, oarray.length);
            }
            freeArrays(ohashes, oarray, oSize);
        }
        if (index2 < oSize) {
            System.arraycopy(this.mHashes, index2, this.mHashes, index2 + 1, oSize - index2);
            System.arraycopy(this.mArray, index2, this.mArray, index2 + 1, oSize - index2);
        }
        if (oSize != this.mSize || index2 >= this.mHashes.length) {
            throw new ConcurrentModificationException();
        }
        this.mHashes[index2] = hash;
        this.mArray[index2] = value;
        this.mSize++;
        return true;
    }

    public void addAll(ArraySet<? extends E> array) {
        int N = array.mSize;
        ensureCapacity(this.mSize + N);
        if (this.mSize == 0) {
            if (N > 0) {
                System.arraycopy(array.mHashes, 0, this.mHashes, 0, N);
                System.arraycopy(array.mArray, 0, this.mArray, 0, N);
                if (this.mSize != 0) {
                    throw new ConcurrentModificationException();
                }
                this.mSize = N;
                return;
            }
            return;
        }
        for (int i = 0; i < N; i++) {
            add(array.valueAt(i));
        }
    }

    @Override // java.util.Collection, java.util.Set
    public boolean remove(Object object) {
        int index = indexOf(object);
        if (index >= 0) {
            removeAt(index);
            return true;
        }
        return false;
    }

    public E removeAt(int i) {
        int i2 = this.mSize;
        E e = (E) this.mArray[i];
        if (i2 <= 1) {
            clear();
        } else {
            int i3 = i2 - 1;
            if (this.mHashes.length > 8 && this.mSize < this.mHashes.length / 3) {
                int i4 = this.mSize > 8 ? this.mSize + (this.mSize >> 1) : 8;
                int[] iArr = this.mHashes;
                Object[] objArr = this.mArray;
                allocArrays(i4);
                if (i > 0) {
                    System.arraycopy(iArr, 0, this.mHashes, 0, i);
                    System.arraycopy(objArr, 0, this.mArray, 0, i);
                }
                if (i < i3) {
                    System.arraycopy(iArr, i + 1, this.mHashes, i, i3 - i);
                    System.arraycopy(objArr, i + 1, this.mArray, i, i3 - i);
                }
            } else {
                if (i < i3) {
                    System.arraycopy(this.mHashes, i + 1, this.mHashes, i, i3 - i);
                    System.arraycopy(this.mArray, i + 1, this.mArray, i, i3 - i);
                }
                this.mArray[i3] = null;
            }
            if (i2 != this.mSize) {
                throw new ConcurrentModificationException();
            }
            this.mSize = i3;
        }
        return e;
    }

    public boolean removeAll(ArraySet<? extends E> array) {
        int N = array.mSize;
        int originalSize = this.mSize;
        for (int i = 0; i < N; i++) {
            remove(array.valueAt(i));
        }
        int i2 = this.mSize;
        return originalSize != i2;
    }

    @Override // java.util.Collection, java.util.Set
    public int size() {
        return this.mSize;
    }

    @Override // java.util.Collection, java.util.Set
    public Object[] toArray() {
        Object[] result = new Object[this.mSize];
        System.arraycopy(this.mArray, 0, result, 0, this.mSize);
        return result;
    }

    @Override // java.util.Collection, java.util.Set
    public <T> T[] toArray(T[] tArr) {
        if (tArr.length < this.mSize) {
            tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), this.mSize));
        }
        System.arraycopy(this.mArray, 0, tArr, 0, this.mSize);
        if (tArr.length > this.mSize) {
            tArr[this.mSize] = null;
        }
        return tArr;
    }

    @Override // java.util.Collection, java.util.Set
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Set)) {
            return false;
        }
        Set<?> set = (Set) object;
        if (size() != set.size()) {
            return false;
        }
        for (int i = 0; i < this.mSize; i++) {
            try {
                E mine = valueAt(i);
                if (!set.contains(mine)) {
                    return false;
                }
            } catch (ClassCastException e) {
                return false;
            } catch (NullPointerException e2) {
                return false;
            }
        }
        return true;
    }

    @Override // java.util.Collection, java.util.Set
    public int hashCode() {
        int[] hashes = this.mHashes;
        int result = 0;
        int s = this.mSize;
        for (int i = 0; i < s; i++) {
            result += hashes[i];
        }
        return result;
    }

    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder(this.mSize * 14);
        buffer.append('{');
        for (int i = 0; i < this.mSize; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            Object value = valueAt(i);
            if (value != this) {
                buffer.append(value);
            } else {
                buffer.append("(this Set)");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    @Override // java.util.Collection, java.lang.Iterable, java.util.Set
    public Iterator<E> iterator() {
        return new ElementIterator();
    }

    private class ElementIterator extends IndexBasedArrayIterator<E> {
        ElementIterator() {
            super(ArraySet.this.mSize);
        }

        @Override // androidx.collection.IndexBasedArrayIterator
        protected E elementAt(int i) {
            return (E) ArraySet.this.valueAt(i);
        }

        @Override // androidx.collection.IndexBasedArrayIterator
        protected void removeAt(int index) {
            ArraySet.this.removeAt(index);
        }
    }

    @Override // java.util.Collection, java.util.Set
    public boolean containsAll(Collection<?> collection) {
        for (Object item : collection) {
            if (!contains(item)) {
                return false;
            }
        }
        return true;
    }

    @Override // java.util.Collection, java.util.Set
    public boolean addAll(Collection<? extends E> collection) {
        ensureCapacity(this.mSize + collection.size());
        boolean added = false;
        for (E value : collection) {
            added |= add(value);
        }
        return added;
    }

    @Override // java.util.Collection, java.util.Set
    public boolean removeAll(Collection<?> collection) {
        boolean removed = false;
        for (Object value : collection) {
            removed |= remove(value);
        }
        return removed;
    }

    @Override // java.util.Collection, java.util.Set
    public boolean retainAll(Collection<?> collection) {
        boolean removed = false;
        for (int i = this.mSize - 1; i >= 0; i--) {
            if (!collection.contains(this.mArray[i])) {
                removeAt(i);
                removed = true;
            }
        }
        return removed;
    }
}
