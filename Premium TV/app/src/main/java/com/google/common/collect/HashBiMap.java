package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class HashBiMap<K, V> extends AbstractMap<K, V> implements BiMap<K, V>, Serializable {
    private static final int ABSENT = -1;
    private static final int ENDPOINT = -2;

    @LazyInit
    private transient Set<Map.Entry<K, V>> entrySet;
    private transient int firstInInsertionOrder;
    private transient int[] hashTableKToV;
    private transient int[] hashTableVToK;

    @CheckForNull
    @LazyInit
    private transient BiMap<V, K> inverse;

    @LazyInit
    private transient Set<K> keySet;
    transient K[] keys;
    private transient int lastInInsertionOrder;
    transient int modCount;
    private transient int[] nextInBucketKToV;
    private transient int[] nextInBucketVToK;
    private transient int[] nextInInsertionOrder;
    private transient int[] prevInInsertionOrder;
    transient int size;

    @LazyInit
    private transient Set<V> valueSet;
    transient V[] values;

    final class EntryForKey extends AbstractMapEntry<K, V> {
        int index;

        @ParametricNullness
        final K key;
        final HashBiMap this$0;

        EntryForKey(HashBiMap hashBiMap, int i) {
            this.this$0 = hashBiMap;
            this.key = (K) NullnessCasts.uncheckedCastNullableTToT(hashBiMap.keys[i]);
            this.index = i;
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        @ParametricNullness
        public K getKey() {
            return this.key;
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        @ParametricNullness
        public V getValue() {
            updateIndex();
            return this.index == -1 ? (V) NullnessCasts.unsafeNull() : (V) NullnessCasts.uncheckedCastNullableTToT(this.this$0.values[this.index]);
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        @ParametricNullness
        public V setValue(@ParametricNullness V v) {
            updateIndex();
            int i = this.index;
            HashBiMap hashBiMap = this.this$0;
            if (i == -1) {
                hashBiMap.put(this.key, v);
                return (V) NullnessCasts.unsafeNull();
            }
            V v2 = (V) NullnessCasts.uncheckedCastNullableTToT(hashBiMap.values[this.index]);
            if (Objects.equal(v2, v)) {
                return v;
            }
            this.this$0.replaceValueInEntry(this.index, v, false);
            return v2;
        }

        void updateIndex() {
            if (this.index == -1 || this.index > this.this$0.size || !Objects.equal(this.this$0.keys[this.index], this.key)) {
                this.index = this.this$0.findEntryByKey(this.key);
            }
        }
    }

    static final class EntryForValue<K, V> extends AbstractMapEntry<V, K> {
        final HashBiMap<K, V> biMap;
        int index;

        @ParametricNullness
        final V value;

        EntryForValue(HashBiMap<K, V> hashBiMap, int i) {
            this.biMap = hashBiMap;
            this.value = (V) NullnessCasts.uncheckedCastNullableTToT(hashBiMap.values[i]);
            this.index = i;
        }

        private void updateIndex() {
            if (this.index == -1 || this.index > this.biMap.size || !Objects.equal(this.value, this.biMap.values[this.index])) {
                this.index = this.biMap.findEntryByValue(this.value);
            }
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        @ParametricNullness
        public V getKey() {
            return this.value;
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        @ParametricNullness
        public K getValue() {
            updateIndex();
            return this.index == -1 ? (K) NullnessCasts.unsafeNull() : (K) NullnessCasts.uncheckedCastNullableTToT(this.biMap.keys[this.index]);
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        @ParametricNullness
        public K setValue(@ParametricNullness K k) {
            updateIndex();
            int i = this.index;
            HashBiMap<K, V> hashBiMap = this.biMap;
            if (i == -1) {
                hashBiMap.putInverse(this.value, k, false);
                return (K) NullnessCasts.unsafeNull();
            }
            K k2 = (K) NullnessCasts.uncheckedCastNullableTToT(hashBiMap.keys[this.index]);
            if (Objects.equal(k2, k)) {
                return k;
            }
            this.biMap.replaceKeyInEntry(this.index, k, false);
            return k2;
        }
    }

    final class EntrySet extends View<K, V, Map.Entry<K, V>> {
        final HashBiMap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        EntrySet(HashBiMap hashBiMap) {
            super(hashBiMap);
            this.this$0 = hashBiMap;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            Object value = entry.getValue();
            int iFindEntryByKey = this.this$0.findEntryByKey(key);
            return iFindEntryByKey != -1 && Objects.equal(value, this.this$0.values[iFindEntryByKey]);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.HashBiMap.View
        public Map.Entry<K, V> forEntry(int i) {
            return new EntryForKey(this.this$0, i);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            if (obj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) obj;
                Object key = entry.getKey();
                Object value = entry.getValue();
                int iSmearedHash = Hashing.smearedHash(key);
                int iFindEntryByKey = this.this$0.findEntryByKey(key, iSmearedHash);
                if (iFindEntryByKey != -1 && Objects.equal(value, this.this$0.values[iFindEntryByKey])) {
                    this.this$0.removeEntryKeyHashKnown(iFindEntryByKey, iSmearedHash);
                    return true;
                }
            }
            return false;
        }
    }

    static class Inverse<K, V> extends AbstractMap<V, K> implements BiMap<V, K>, Serializable {
        private final HashBiMap<K, V> forward;
        private transient Set<Map.Entry<V, K>> inverseEntrySet;

        Inverse(HashBiMap<K, V> hashBiMap) {
            this.forward = hashBiMap;
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            ((HashBiMap) this.forward).inverse = this;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public void clear() {
            this.forward.clear();
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            return this.forward.containsValue(obj);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsValue(@CheckForNull Object obj) {
            return this.forward.containsKey(obj);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public Set<Map.Entry<V, K>> entrySet() {
            Set<Map.Entry<V, K>> set = this.inverseEntrySet;
            if (set != null) {
                return set;
            }
            InverseEntrySet inverseEntrySet = new InverseEntrySet(this.forward);
            this.inverseEntrySet = inverseEntrySet;
            return inverseEntrySet;
        }

        @Override // com.google.common.collect.BiMap
        @CheckForNull
        public K forcePut(@ParametricNullness V v, @ParametricNullness K k) {
            return this.forward.putInverse(v, k, true);
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public K get(@CheckForNull Object obj) {
            return this.forward.getInverse(obj);
        }

        @Override // com.google.common.collect.BiMap
        public BiMap<K, V> inverse() {
            return this.forward;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public Set<V> keySet() {
            return this.forward.values();
        }

        @Override // java.util.AbstractMap, java.util.Map, com.google.common.collect.BiMap
        @CheckForNull
        public K put(@ParametricNullness V v, @ParametricNullness K k) {
            return this.forward.putInverse(v, k, false);
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public K remove(@CheckForNull Object obj) {
            return this.forward.removeInverse(obj);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public int size() {
            return this.forward.size;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public Set<K> values() {
            return this.forward.keySet();
        }
    }

    static class InverseEntrySet<K, V> extends View<K, V, Map.Entry<V, K>> {
        InverseEntrySet(HashBiMap<K, V> hashBiMap) {
            super(hashBiMap);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            Object value = entry.getValue();
            int iFindEntryByValue = this.biMap.findEntryByValue(key);
            return iFindEntryByValue != -1 && Objects.equal(this.biMap.keys[iFindEntryByValue], value);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.HashBiMap.View
        public Map.Entry<V, K> forEntry(int i) {
            return new EntryForValue(this.biMap, i);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            if (obj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) obj;
                Object key = entry.getKey();
                Object value = entry.getValue();
                int iSmearedHash = Hashing.smearedHash(key);
                int iFindEntryByValue = this.biMap.findEntryByValue(key, iSmearedHash);
                if (iFindEntryByValue != -1 && Objects.equal(this.biMap.keys[iFindEntryByValue], value)) {
                    this.biMap.removeEntryValueHashKnown(iFindEntryByValue, iSmearedHash);
                    return true;
                }
            }
            return false;
        }
    }

    final class KeySet extends View<K, V, K> {
        final HashBiMap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        KeySet(HashBiMap hashBiMap) {
            super(hashBiMap);
            this.this$0 = hashBiMap;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            return this.this$0.containsKey(obj);
        }

        @Override // com.google.common.collect.HashBiMap.View
        @ParametricNullness
        K forEntry(int i) {
            return (K) NullnessCasts.uncheckedCastNullableTToT(this.this$0.keys[i]);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            int iSmearedHash = Hashing.smearedHash(obj);
            int iFindEntryByKey = this.this$0.findEntryByKey(obj, iSmearedHash);
            if (iFindEntryByKey == -1) {
                return false;
            }
            this.this$0.removeEntryKeyHashKnown(iFindEntryByKey, iSmearedHash);
            return true;
        }
    }

    final class ValueSet extends View<K, V, V> {
        final HashBiMap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        ValueSet(HashBiMap hashBiMap) {
            super(hashBiMap);
            this.this$0 = hashBiMap;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            return this.this$0.containsValue(obj);
        }

        @Override // com.google.common.collect.HashBiMap.View
        @ParametricNullness
        V forEntry(int i) {
            return (V) NullnessCasts.uncheckedCastNullableTToT(this.this$0.values[i]);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            int iSmearedHash = Hashing.smearedHash(obj);
            int iFindEntryByValue = this.this$0.findEntryByValue(obj, iSmearedHash);
            if (iFindEntryByValue == -1) {
                return false;
            }
            this.this$0.removeEntryValueHashKnown(iFindEntryByValue, iSmearedHash);
            return true;
        }
    }

    static abstract class View<K, V, T> extends AbstractSet<T> {
        final HashBiMap<K, V> biMap;

        View(HashBiMap<K, V> hashBiMap) {
            this.biMap = hashBiMap;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            this.biMap.clear();
        }

        @ParametricNullness
        abstract T forEntry(int i);

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<T> iterator() {
            return new Iterator<T>(this) { // from class: com.google.common.collect.HashBiMap.View.1
                private int expectedModCount;
                private int index;
                private int indexToRemove = -1;
                private int remaining;
                final View this$0;

                {
                    this.this$0 = this;
                    this.index = ((HashBiMap) this.this$0.biMap).firstInInsertionOrder;
                    this.expectedModCount = this.this$0.biMap.modCount;
                    this.remaining = this.this$0.biMap.size;
                }

                private void checkForComodification() {
                    if (this.this$0.biMap.modCount != this.expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                }

                @Override // java.util.Iterator
                public boolean hasNext() {
                    checkForComodification();
                    return this.index != -2 && this.remaining > 0;
                }

                @Override // java.util.Iterator
                @ParametricNullness
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    T t = (T) this.this$0.forEntry(this.index);
                    this.indexToRemove = this.index;
                    this.index = ((HashBiMap) this.this$0.biMap).nextInInsertionOrder[this.index];
                    this.remaining--;
                    return t;
                }

                @Override // java.util.Iterator
                public void remove() {
                    checkForComodification();
                    CollectPreconditions.checkRemove(this.indexToRemove != -1);
                    this.this$0.biMap.removeEntry(this.indexToRemove);
                    if (this.index == this.this$0.biMap.size) {
                        this.index = this.indexToRemove;
                    }
                    this.indexToRemove = -1;
                    this.expectedModCount = this.this$0.biMap.modCount;
                }
            };
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.biMap.size;
        }
    }

    private HashBiMap(int i) {
        init(i);
    }

    private int bucket(int i) {
        return (this.hashTableKToV.length - 1) & i;
    }

    public static <K, V> HashBiMap<K, V> create() {
        return create(16);
    }

    public static <K, V> HashBiMap<K, V> create(int i) {
        return new HashBiMap<>(i);
    }

    public static <K, V> HashBiMap<K, V> create(Map<? extends K, ? extends V> map) {
        HashBiMap<K, V> hashBiMapCreate = create(map.size());
        hashBiMapCreate.putAll(map);
        return hashBiMapCreate;
    }

    private static int[] createFilledWithAbsent(int i) {
        int[] iArr = new int[i];
        Arrays.fill(iArr, -1);
        return iArr;
    }

    private void deleteFromTableKToV(int i, int i2) {
        Preconditions.checkArgument(i != -1);
        int iBucket = bucket(i2);
        int i3 = this.hashTableKToV[iBucket];
        int[] iArr = this.hashTableKToV;
        if (i3 == i) {
            iArr[iBucket] = this.nextInBucketKToV[i];
            this.nextInBucketKToV[i] = -1;
            return;
        }
        int i4 = iArr[iBucket];
        int i5 = this.nextInBucketKToV[i4];
        while (i5 != -1) {
            int[] iArr2 = this.nextInBucketKToV;
            if (i5 == i) {
                iArr2[i4] = this.nextInBucketKToV[i];
                this.nextInBucketKToV[i] = -1;
                return;
            } else {
                int i6 = i5;
                i5 = iArr2[i5];
                i4 = i6;
            }
        }
        throw new AssertionError("Expected to find entry with key " + this.keys[i]);
    }

    private void deleteFromTableVToK(int i, int i2) {
        Preconditions.checkArgument(i != -1);
        int iBucket = bucket(i2);
        int i3 = this.hashTableVToK[iBucket];
        int[] iArr = this.hashTableVToK;
        if (i3 == i) {
            iArr[iBucket] = this.nextInBucketVToK[i];
            this.nextInBucketVToK[i] = -1;
            return;
        }
        int i4 = iArr[iBucket];
        int i5 = this.nextInBucketVToK[i4];
        while (i5 != -1) {
            int[] iArr2 = this.nextInBucketVToK;
            if (i5 == i) {
                iArr2[i4] = this.nextInBucketVToK[i];
                this.nextInBucketVToK[i] = -1;
                return;
            } else {
                int i6 = i5;
                i5 = iArr2[i5];
                i4 = i6;
            }
        }
        throw new AssertionError("Expected to find entry with value " + this.values[i]);
    }

    private void ensureCapacity(int i) {
        if (this.nextInBucketKToV.length < i) {
            int iExpandedCapacity = ImmutableCollection.Builder.expandedCapacity(this.nextInBucketKToV.length, i);
            this.keys = (K[]) Arrays.copyOf(this.keys, iExpandedCapacity);
            this.values = (V[]) Arrays.copyOf(this.values, iExpandedCapacity);
            this.nextInBucketKToV = expandAndFillWithAbsent(this.nextInBucketKToV, iExpandedCapacity);
            this.nextInBucketVToK = expandAndFillWithAbsent(this.nextInBucketVToK, iExpandedCapacity);
            this.prevInInsertionOrder = expandAndFillWithAbsent(this.prevInInsertionOrder, iExpandedCapacity);
            this.nextInInsertionOrder = expandAndFillWithAbsent(this.nextInInsertionOrder, iExpandedCapacity);
        }
        if (this.hashTableKToV.length < i) {
            int iClosedTableSize = Hashing.closedTableSize(i, 1.0d);
            this.hashTableKToV = createFilledWithAbsent(iClosedTableSize);
            this.hashTableVToK = createFilledWithAbsent(iClosedTableSize);
            for (int i2 = 0; i2 < this.size; i2++) {
                int iBucket = bucket(Hashing.smearedHash(this.keys[i2]));
                this.nextInBucketKToV[i2] = this.hashTableKToV[iBucket];
                this.hashTableKToV[iBucket] = i2;
                int iBucket2 = bucket(Hashing.smearedHash(this.values[i2]));
                this.nextInBucketVToK[i2] = this.hashTableVToK[iBucket2];
                this.hashTableVToK[iBucket2] = i2;
            }
        }
    }

    private static int[] expandAndFillWithAbsent(int[] iArr, int i) {
        int length = iArr.length;
        int[] iArrCopyOf = Arrays.copyOf(iArr, i);
        Arrays.fill(iArrCopyOf, length, i, -1);
        return iArrCopyOf;
    }

    private void insertIntoTableKToV(int i, int i2) {
        Preconditions.checkArgument(i != -1);
        int iBucket = bucket(i2);
        this.nextInBucketKToV[i] = this.hashTableKToV[iBucket];
        this.hashTableKToV[iBucket] = i;
    }

    private void insertIntoTableVToK(int i, int i2) {
        Preconditions.checkArgument(i != -1);
        int iBucket = bucket(i2);
        this.nextInBucketVToK[i] = this.hashTableVToK[iBucket];
        this.hashTableVToK[iBucket] = i;
    }

    private void moveEntryToIndex(int i, int i2) {
        int[] iArr;
        int[] iArr2;
        if (i == i2) {
            return;
        }
        int i3 = this.prevInInsertionOrder[i];
        int i4 = this.nextInInsertionOrder[i];
        setSucceeds(i3, i2);
        setSucceeds(i2, i4);
        K k = this.keys[i];
        V v = this.values[i];
        this.keys[i2] = k;
        this.values[i2] = v;
        int iBucket = bucket(Hashing.smearedHash(k));
        int i5 = this.hashTableKToV[iBucket];
        int[] iArr3 = this.hashTableKToV;
        if (i5 == i) {
            iArr3[iBucket] = i2;
        } else {
            int i6 = iArr3[iBucket];
            int i7 = this.nextInBucketKToV[i6];
            while (true) {
                iArr = this.nextInBucketKToV;
                if (i7 == i) {
                    break;
                }
                int i8 = i7;
                i7 = iArr[i7];
                i6 = i8;
            }
            iArr[i6] = i2;
        }
        this.nextInBucketKToV[i2] = this.nextInBucketKToV[i];
        this.nextInBucketKToV[i] = -1;
        int iBucket2 = bucket(Hashing.smearedHash(v));
        int i9 = this.hashTableVToK[iBucket2];
        int[] iArr4 = this.hashTableVToK;
        if (i9 == i) {
            iArr4[iBucket2] = i2;
        } else {
            int i10 = iArr4[iBucket2];
            int i11 = this.nextInBucketVToK[i10];
            while (true) {
                iArr2 = this.nextInBucketVToK;
                if (i11 == i) {
                    break;
                }
                int i12 = i11;
                i11 = iArr2[i11];
                i10 = i12;
            }
            iArr2[i10] = i2;
        }
        this.nextInBucketVToK[i2] = this.nextInBucketVToK[i];
        this.nextInBucketVToK[i] = -1;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        int count = Serialization.readCount(objectInputStream);
        init(16);
        Serialization.populateMap(this, objectInputStream, count);
    }

    private void removeEntry(int i, int i2, int i3) {
        Preconditions.checkArgument(i != -1);
        deleteFromTableKToV(i, i2);
        deleteFromTableVToK(i, i3);
        setSucceeds(this.prevInInsertionOrder[i], this.nextInInsertionOrder[i]);
        moveEntryToIndex(this.size - 1, i);
        this.keys[this.size - 1] = null;
        this.values[this.size - 1] = null;
        this.size--;
        this.modCount++;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void replaceKeyInEntry(int i, @ParametricNullness K k, boolean z) {
        Preconditions.checkArgument(i != -1);
        int iSmearedHash = Hashing.smearedHash(k);
        int iFindEntryByKey = findEntryByKey(k, iSmearedHash);
        int i2 = this.lastInInsertionOrder;
        int i3 = -2;
        if (iFindEntryByKey != -1) {
            if (!z) {
                throw new IllegalArgumentException("Key already present in map: " + k);
            }
            i2 = this.prevInInsertionOrder[iFindEntryByKey];
            i3 = this.nextInInsertionOrder[iFindEntryByKey];
            removeEntryKeyHashKnown(iFindEntryByKey, iSmearedHash);
            if (i == this.size) {
                i = iFindEntryByKey;
            }
        }
        if (i2 == i) {
            i2 = this.prevInInsertionOrder[i];
        } else if (i2 == this.size) {
            i2 = iFindEntryByKey;
        }
        if (i3 == i) {
            iFindEntryByKey = this.nextInInsertionOrder[i];
        } else if (i3 != this.size) {
            iFindEntryByKey = i3;
        }
        setSucceeds(this.prevInInsertionOrder[i], this.nextInInsertionOrder[i]);
        deleteFromTableKToV(i, Hashing.smearedHash(this.keys[i]));
        this.keys[i] = k;
        insertIntoTableKToV(i, Hashing.smearedHash(k));
        setSucceeds(i2, i);
        setSucceeds(i, iFindEntryByKey);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void replaceValueInEntry(int i, @ParametricNullness V v, boolean z) {
        Preconditions.checkArgument(i != -1);
        int iSmearedHash = Hashing.smearedHash(v);
        int iFindEntryByValue = findEntryByValue(v, iSmearedHash);
        if (iFindEntryByValue != -1) {
            if (!z) {
                throw new IllegalArgumentException("Value already present in map: " + v);
            }
            removeEntryValueHashKnown(iFindEntryByValue, iSmearedHash);
            if (i == this.size) {
                i = iFindEntryByValue;
            }
        }
        deleteFromTableVToK(i, Hashing.smearedHash(this.values[i]));
        this.values[i] = v;
        insertIntoTableVToK(i, iSmearedHash);
    }

    private void setSucceeds(int i, int i2) {
        if (i == -2) {
            this.firstInInsertionOrder = i2;
        } else {
            this.nextInInsertionOrder[i] = i2;
        }
        if (i2 == -2) {
            this.lastInInsertionOrder = i;
        } else {
            this.prevInInsertionOrder[i2] = i;
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        Serialization.writeMap(this, objectOutputStream);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public void clear() {
        Arrays.fill(this.keys, 0, this.size, (Object) null);
        Arrays.fill(this.values, 0, this.size, (Object) null);
        Arrays.fill(this.hashTableKToV, -1);
        Arrays.fill(this.hashTableVToK, -1);
        Arrays.fill(this.nextInBucketKToV, 0, this.size, -1);
        Arrays.fill(this.nextInBucketVToK, 0, this.size, -1);
        Arrays.fill(this.prevInInsertionOrder, 0, this.size, -1);
        Arrays.fill(this.nextInInsertionOrder, 0, this.size, -1);
        this.size = 0;
        this.firstInInsertionOrder = -2;
        this.lastInInsertionOrder = -2;
        this.modCount++;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean containsKey(@CheckForNull Object obj) {
        return findEntryByKey(obj) != -1;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean containsValue(@CheckForNull Object obj) {
        return findEntryByValue(obj) != -1;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = this.entrySet;
        if (set != null) {
            return set;
        }
        EntrySet entrySet = new EntrySet(this);
        this.entrySet = entrySet;
        return entrySet;
    }

    int findEntry(@CheckForNull Object obj, int i, int[] iArr, int[] iArr2, Object[] objArr) {
        int i2 = iArr[bucket(i)];
        while (i2 != -1) {
            if (Objects.equal(objArr[i2], obj)) {
                return i2;
            }
            i2 = iArr2[i2];
        }
        return -1;
    }

    int findEntryByKey(@CheckForNull Object obj) {
        return findEntryByKey(obj, Hashing.smearedHash(obj));
    }

    int findEntryByKey(@CheckForNull Object obj, int i) {
        return findEntry(obj, i, this.hashTableKToV, this.nextInBucketKToV, this.keys);
    }

    int findEntryByValue(@CheckForNull Object obj) {
        return findEntryByValue(obj, Hashing.smearedHash(obj));
    }

    int findEntryByValue(@CheckForNull Object obj, int i) {
        return findEntry(obj, i, this.hashTableVToK, this.nextInBucketVToK, this.values);
    }

    @Override // com.google.common.collect.BiMap
    @CheckForNull
    public V forcePut(@ParametricNullness K k, @ParametricNullness V v) {
        return put(k, v, true);
    }

    @Override // java.util.AbstractMap, java.util.Map
    @CheckForNull
    public V get(@CheckForNull Object obj) {
        int iFindEntryByKey = findEntryByKey(obj);
        if (iFindEntryByKey == -1) {
            return null;
        }
        return this.values[iFindEntryByKey];
    }

    @CheckForNull
    K getInverse(@CheckForNull Object obj) {
        int iFindEntryByValue = findEntryByValue(obj);
        if (iFindEntryByValue == -1) {
            return null;
        }
        return this.keys[iFindEntryByValue];
    }

    void init(int i) {
        CollectPreconditions.checkNonnegative(i, "expectedSize");
        int iClosedTableSize = Hashing.closedTableSize(i, 1.0d);
        this.size = 0;
        this.keys = (K[]) new Object[i];
        this.values = (V[]) new Object[i];
        this.hashTableKToV = createFilledWithAbsent(iClosedTableSize);
        this.hashTableVToK = createFilledWithAbsent(iClosedTableSize);
        this.nextInBucketKToV = createFilledWithAbsent(i);
        this.nextInBucketVToK = createFilledWithAbsent(i);
        this.firstInInsertionOrder = -2;
        this.lastInInsertionOrder = -2;
        this.prevInInsertionOrder = createFilledWithAbsent(i);
        this.nextInInsertionOrder = createFilledWithAbsent(i);
    }

    @Override // com.google.common.collect.BiMap
    public BiMap<V, K> inverse() {
        BiMap<V, K> biMap = this.inverse;
        if (biMap != null) {
            return biMap;
        }
        Inverse inverse = new Inverse(this);
        this.inverse = inverse;
        return inverse;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Set<K> keySet() {
        Set<K> set = this.keySet;
        if (set != null) {
            return set;
        }
        KeySet keySet = new KeySet(this);
        this.keySet = keySet;
        return keySet;
    }

    @Override // java.util.AbstractMap, java.util.Map, com.google.common.collect.BiMap
    @CheckForNull
    public V put(@ParametricNullness K k, @ParametricNullness V v) {
        return put(k, v, false);
    }

    @CheckForNull
    V put(@ParametricNullness K k, @ParametricNullness V v, boolean z) {
        int iSmearedHash = Hashing.smearedHash(k);
        int iFindEntryByKey = findEntryByKey(k, iSmearedHash);
        if (iFindEntryByKey != -1) {
            V v2 = this.values[iFindEntryByKey];
            if (Objects.equal(v2, v)) {
                return v;
            }
            replaceValueInEntry(iFindEntryByKey, v, z);
            return v2;
        }
        int iSmearedHash2 = Hashing.smearedHash(v);
        int iFindEntryByValue = findEntryByValue(v, iSmearedHash2);
        if (!z) {
            Preconditions.checkArgument(iFindEntryByValue == -1, "Value already present: %s", v);
        } else if (iFindEntryByValue != -1) {
            removeEntryValueHashKnown(iFindEntryByValue, iSmearedHash2);
        }
        ensureCapacity(this.size + 1);
        this.keys[this.size] = k;
        this.values[this.size] = v;
        insertIntoTableKToV(this.size, iSmearedHash);
        insertIntoTableVToK(this.size, iSmearedHash2);
        setSucceeds(this.lastInInsertionOrder, this.size);
        setSucceeds(this.size, -2);
        this.size++;
        this.modCount++;
        return null;
    }

    /* JADX WARN: Code duplicated, block: B:14:0x0050  */
    /* JADX WARN: Code duplicated, block: B:21:0x0076  */
    @CheckForNull
    K putInverse(@ParametricNullness V v, @ParametricNullness K k, boolean z) {
        int i;
        int i2;
        int iSmearedHash = Hashing.smearedHash(v);
        int iFindEntryByValue = findEntryByValue(v, iSmearedHash);
        if (iFindEntryByValue != -1) {
            K k2 = this.keys[iFindEntryByValue];
            if (Objects.equal(k2, k)) {
                return k;
            }
            replaceKeyInEntry(iFindEntryByValue, k, z);
            return k2;
        }
        int i3 = this.lastInInsertionOrder;
        int iSmearedHash2 = Hashing.smearedHash(k);
        int iFindEntryByKey = findEntryByKey(k, iSmearedHash2);
        if (z) {
            if (iFindEntryByKey != -1) {
                i = this.prevInInsertionOrder[iFindEntryByKey];
                removeEntryKeyHashKnown(iFindEntryByKey, iSmearedHash2);
            }
            ensureCapacity(this.size + 1);
            this.keys[this.size] = k;
            this.values[this.size] = v;
            insertIntoTableKToV(this.size, iSmearedHash2);
            insertIntoTableVToK(this.size, iSmearedHash);
            if (i == -2) {
                i2 = this.firstInInsertionOrder;
            } else {
                i2 = this.nextInInsertionOrder[i];
            }
            setSucceeds(i, this.size);
            setSucceeds(this.size, i2);
            this.size++;
            this.modCount++;
            return null;
        }
        Preconditions.checkArgument(iFindEntryByKey == -1, "Key already present: %s", k);
        i = i3;
        ensureCapacity(this.size + 1);
        this.keys[this.size] = k;
        this.values[this.size] = v;
        insertIntoTableKToV(this.size, iSmearedHash2);
        insertIntoTableVToK(this.size, iSmearedHash);
        if (i == -2) {
            i2 = this.firstInInsertionOrder;
        } else {
            i2 = this.nextInInsertionOrder[i];
        }
        setSucceeds(i, this.size);
        setSucceeds(this.size, i2);
        this.size++;
        this.modCount++;
        return null;
    }

    @Override // java.util.AbstractMap, java.util.Map
    @CheckForNull
    public V remove(@CheckForNull Object obj) {
        int iSmearedHash = Hashing.smearedHash(obj);
        int iFindEntryByKey = findEntryByKey(obj, iSmearedHash);
        if (iFindEntryByKey == -1) {
            return null;
        }
        V v = this.values[iFindEntryByKey];
        removeEntryKeyHashKnown(iFindEntryByKey, iSmearedHash);
        return v;
    }

    void removeEntry(int i) {
        removeEntryKeyHashKnown(i, Hashing.smearedHash(this.keys[i]));
    }

    void removeEntryKeyHashKnown(int i, int i2) {
        removeEntry(i, i2, Hashing.smearedHash(this.values[i]));
    }

    void removeEntryValueHashKnown(int i, int i2) {
        removeEntry(i, Hashing.smearedHash(this.keys[i]), i2);
    }

    @CheckForNull
    K removeInverse(@CheckForNull Object obj) {
        int iSmearedHash = Hashing.smearedHash(obj);
        int iFindEntryByValue = findEntryByValue(obj, iSmearedHash);
        if (iFindEntryByValue == -1) {
            return null;
        }
        K k = this.keys[iFindEntryByValue];
        removeEntryValueHashKnown(iFindEntryByValue, iSmearedHash);
        return k;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public int size() {
        return this.size;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Set<V> values() {
        Set<V> set = this.valueSet;
        if (set != null) {
            return set;
        }
        ValueSet valueSet = new ValueSet(this);
        this.valueSet = valueSet;
        return valueSet;
    }
}
