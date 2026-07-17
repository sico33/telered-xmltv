package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
class CompactHashMap<K, V> extends AbstractMap<K, V> implements Serializable {
    static final double HASH_FLOODING_FPP = 0.001d;
    private static final int MAX_HASH_BUCKET_LENGTH = 9;
    private static final Object NOT_FOUND = new Object();

    @CheckForNull
    transient int[] entries;

    @CheckForNull
    @LazyInit
    private transient Set<Map.Entry<K, V>> entrySetView;

    @CheckForNull
    @LazyInit
    private transient Set<K> keySetView;

    @CheckForNull
    transient Object[] keys;
    private transient int metadata;
    private transient int size;

    @CheckForNull
    private transient Object table;

    @CheckForNull
    transient Object[] values;

    @CheckForNull
    @LazyInit
    private transient Collection<V> valuesView;

    class EntrySetView extends AbstractSet<Map.Entry<K, V>> {
        final CompactHashMap this$0;

        EntrySetView(CompactHashMap compactHashMap) {
            this.this$0 = compactHashMap;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            this.this$0.clear();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            Map<K, V> mapDelegateOrNull = this.this$0.delegateOrNull();
            if (mapDelegateOrNull != null) {
                return mapDelegateOrNull.entrySet().contains(obj);
            }
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            int iIndexOf = this.this$0.indexOf(entry.getKey());
            return iIndexOf != -1 && Objects.equal(this.this$0.value(iIndexOf), entry.getValue());
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<Map.Entry<K, V>> iterator() {
            return this.this$0.entrySetIterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            int iHashTableMask;
            int iRemove;
            Map<K, V> mapDelegateOrNull = this.this$0.delegateOrNull();
            if (mapDelegateOrNull != null) {
                return mapDelegateOrNull.entrySet().remove(obj);
            }
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            if (!this.this$0.needsAllocArrays() && (iRemove = CompactHashing.remove(entry.getKey(), entry.getValue(), (iHashTableMask = this.this$0.hashTableMask()), this.this$0.requireTable(), this.this$0.requireEntries(), this.this$0.requireKeys(), this.this$0.requireValues())) != -1) {
                this.this$0.moveLastEntry(iRemove, iHashTableMask);
                CompactHashMap.access$1210(this.this$0);
                this.this$0.incrementModCount();
                return true;
            }
            return false;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.this$0.size();
        }
    }

    private abstract class Itr<T> implements Iterator<T> {
        int currentIndex;
        int expectedMetadata;
        int indexToRemove;
        final CompactHashMap this$0;

        private Itr(CompactHashMap compactHashMap) {
            this.this$0 = compactHashMap;
            this.expectedMetadata = this.this$0.metadata;
            this.currentIndex = this.this$0.firstEntryIndex();
            this.indexToRemove = -1;
        }

        private void checkForConcurrentModification() {
            if (this.this$0.metadata != this.expectedMetadata) {
                throw new ConcurrentModificationException();
            }
        }

        @ParametricNullness
        abstract T getOutput(int i);

        @Override // java.util.Iterator
        public boolean hasNext() {
            return this.currentIndex >= 0;
        }

        void incrementExpectedModCount() {
            this.expectedMetadata += 32;
        }

        @Override // java.util.Iterator
        @ParametricNullness
        public T next() {
            checkForConcurrentModification();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            this.indexToRemove = this.currentIndex;
            T output = getOutput(this.currentIndex);
            this.currentIndex = this.this$0.getSuccessor(this.currentIndex);
            return output;
        }

        @Override // java.util.Iterator
        public void remove() {
            checkForConcurrentModification();
            CollectPreconditions.checkRemove(this.indexToRemove >= 0);
            incrementExpectedModCount();
            this.this$0.remove(this.this$0.key(this.indexToRemove));
            this.currentIndex = this.this$0.adjustAfterRemove(this.currentIndex, this.indexToRemove);
            this.indexToRemove = -1;
        }
    }

    class KeySetView extends AbstractSet<K> {
        final CompactHashMap this$0;

        KeySetView(CompactHashMap compactHashMap) {
            this.this$0 = compactHashMap;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            this.this$0.clear();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            return this.this$0.containsKey(obj);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<K> iterator() {
            return this.this$0.keySetIterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            Map<K, V> mapDelegateOrNull = this.this$0.delegateOrNull();
            if (mapDelegateOrNull != null) {
                return mapDelegateOrNull.keySet().remove(obj);
            }
            return this.this$0.removeHelper(obj) != CompactHashMap.NOT_FOUND;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.this$0.size();
        }
    }

    final class MapEntry extends AbstractMapEntry<K, V> {

        @ParametricNullness
        private final K key;
        private int lastKnownIndex;
        final CompactHashMap this$0;

        MapEntry(CompactHashMap compactHashMap, int i) {
            this.this$0 = compactHashMap;
            this.key = (K) compactHashMap.key(i);
            this.lastKnownIndex = i;
        }

        private void updateLastKnownIndex() {
            if (this.lastKnownIndex == -1 || this.lastKnownIndex >= this.this$0.size() || !Objects.equal(this.key, this.this$0.key(this.lastKnownIndex))) {
                this.lastKnownIndex = this.this$0.indexOf(this.key);
            }
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        @ParametricNullness
        public K getKey() {
            return this.key;
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        @ParametricNullness
        public V getValue() {
            Map<K, V> mapDelegateOrNull = this.this$0.delegateOrNull();
            if (mapDelegateOrNull != null) {
                return (V) NullnessCasts.uncheckedCastNullableTToT(mapDelegateOrNull.get(this.key));
            }
            updateLastKnownIndex();
            return this.lastKnownIndex == -1 ? (V) NullnessCasts.unsafeNull() : (V) this.this$0.value(this.lastKnownIndex);
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        @ParametricNullness
        public V setValue(@ParametricNullness V v) {
            Map<K, V> mapDelegateOrNull = this.this$0.delegateOrNull();
            if (mapDelegateOrNull != null) {
                return (V) NullnessCasts.uncheckedCastNullableTToT(mapDelegateOrNull.put(this.key, v));
            }
            updateLastKnownIndex();
            int i = this.lastKnownIndex;
            CompactHashMap compactHashMap = this.this$0;
            if (i == -1) {
                compactHashMap.put(this.key, v);
                return (V) NullnessCasts.unsafeNull();
            }
            V v2 = (V) compactHashMap.value(this.lastKnownIndex);
            this.this$0.setValue(this.lastKnownIndex, v);
            return v2;
        }
    }

    class ValuesView extends AbstractCollection<V> {
        final CompactHashMap this$0;

        ValuesView(CompactHashMap compactHashMap) {
            this.this$0 = compactHashMap;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public void clear() {
            this.this$0.clear();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable
        public Iterator<V> iterator() {
            return this.this$0.valuesIterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public int size() {
            return this.this$0.size();
        }
    }

    CompactHashMap() {
        init(3);
    }

    CompactHashMap(int i) {
        init(i);
    }

    static /* synthetic */ int access$1210(CompactHashMap compactHashMap) {
        int i = compactHashMap.size;
        compactHashMap.size = i - 1;
        return i;
    }

    public static <K, V> CompactHashMap<K, V> create() {
        return new CompactHashMap<>();
    }

    public static <K, V> CompactHashMap<K, V> createWithExpectedSize(int i) {
        return new CompactHashMap<>(i);
    }

    private int entry(int i) {
        return requireEntries()[i];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int hashTableMask() {
        return (1 << (this.metadata & 31)) - 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int indexOf(@CheckForNull Object obj) {
        if (needsAllocArrays()) {
            return -1;
        }
        int iSmearedHash = Hashing.smearedHash(obj);
        int iHashTableMask = hashTableMask();
        int iTableGet = CompactHashing.tableGet(requireTable(), iSmearedHash & iHashTableMask);
        if (iTableGet == 0) {
            return -1;
        }
        int hashPrefix = CompactHashing.getHashPrefix(iSmearedHash, iHashTableMask);
        do {
            int i = iTableGet - 1;
            int iEntry = entry(i);
            if (CompactHashing.getHashPrefix(iEntry, iHashTableMask) == hashPrefix && Objects.equal(obj, key(i))) {
                return i;
            }
            iTableGet = CompactHashing.getNext(iEntry, iHashTableMask);
        } while (iTableGet != 0);
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public K key(int i) {
        return (K) requireKeys()[i];
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        int i = objectInputStream.readInt();
        if (i < 0) {
            throw new InvalidObjectException("Invalid size: " + i);
        }
        init(i);
        for (int i2 = 0; i2 < i; i2++) {
            put(objectInputStream.readObject(), objectInputStream.readObject());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Object removeHelper(@CheckForNull Object obj) {
        int iHashTableMask;
        int iRemove;
        if (!needsAllocArrays() && (iRemove = CompactHashing.remove(obj, null, (iHashTableMask = hashTableMask()), requireTable(), requireEntries(), requireKeys(), null)) != -1) {
            V vValue = value(iRemove);
            moveLastEntry(iRemove, iHashTableMask);
            this.size--;
            incrementModCount();
            return vValue;
        }
        return NOT_FOUND;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int[] requireEntries() {
        return (int[]) java.util.Objects.requireNonNull(this.entries);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Object[] requireKeys() {
        return (Object[]) java.util.Objects.requireNonNull(this.keys);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Object requireTable() {
        return java.util.Objects.requireNonNull(this.table);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Object[] requireValues() {
        return (Object[]) java.util.Objects.requireNonNull(this.values);
    }

    private void resizeMeMaybe(int i) {
        int iMin;
        int length = requireEntries().length;
        if (i <= length || (iMin = Math.min(1073741823, (Math.max(1, length >>> 1) + length) | 1)) == length) {
            return;
        }
        resizeEntries(iMin);
    }

    private int resizeTable(int i, int i2, int i3, int i4) {
        Object objCreateTable = CompactHashing.createTable(i2);
        int i5 = i2 - 1;
        if (i4 != 0) {
            CompactHashing.tableSet(objCreateTable, i3 & i5, i4 + 1);
        }
        Object objRequireTable = requireTable();
        int[] iArrRequireEntries = requireEntries();
        for (int i6 = 0; i6 <= i; i6++) {
            int iTableGet = CompactHashing.tableGet(objRequireTable, i6);
            while (iTableGet != 0) {
                int i7 = iTableGet - 1;
                int i8 = iArrRequireEntries[i7];
                int hashPrefix = CompactHashing.getHashPrefix(i8, i) | i6;
                int i9 = hashPrefix & i5;
                int iTableGet2 = CompactHashing.tableGet(objCreateTable, i9);
                CompactHashing.tableSet(objCreateTable, i9, iTableGet);
                iArrRequireEntries[i7] = CompactHashing.maskCombine(hashPrefix, iTableGet2, i5);
                iTableGet = CompactHashing.getNext(i8, i);
            }
        }
        this.table = objCreateTable;
        setHashTableMask(i5);
        return i5;
    }

    private void setEntry(int i, int i2) {
        requireEntries()[i] = i2;
    }

    private void setHashTableMask(int i) {
        this.metadata = CompactHashing.maskCombine(this.metadata, 32 - Integer.numberOfLeadingZeros(i), 31);
    }

    private void setKey(int i, K k) {
        requireKeys()[i] = k;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setValue(int i, V v) {
        requireValues()[i] = v;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public V value(int i) {
        return (V) requireValues()[i];
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(size());
        Iterator<Map.Entry<K, V>> itEntrySetIterator = entrySetIterator();
        while (itEntrySetIterator.hasNext()) {
            Map.Entry<K, V> next = itEntrySetIterator.next();
            objectOutputStream.writeObject(next.getKey());
            objectOutputStream.writeObject(next.getValue());
        }
    }

    void accessEntry(int i) {
    }

    int adjustAfterRemove(int i, int i2) {
        return i - 1;
    }

    int allocArrays() {
        Preconditions.checkState(needsAllocArrays(), "Arrays already allocated");
        int i = this.metadata;
        int iTableSize = CompactHashing.tableSize(i);
        this.table = CompactHashing.createTable(iTableSize);
        setHashTableMask(iTableSize - 1);
        this.entries = new int[i];
        this.keys = new Object[i];
        this.values = new Object[i];
        return i;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public void clear() {
        if (needsAllocArrays()) {
            return;
        }
        incrementModCount();
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        if (mapDelegateOrNull != null) {
            this.metadata = Ints.constrainToRange(size(), 3, 1073741823);
            mapDelegateOrNull.clear();
            this.table = null;
            this.size = 0;
            return;
        }
        Arrays.fill(requireKeys(), 0, this.size, (Object) null);
        Arrays.fill(requireValues(), 0, this.size, (Object) null);
        CompactHashing.tableClear(requireTable());
        Arrays.fill(requireEntries(), 0, this.size, 0);
        this.size = 0;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean containsKey(@CheckForNull Object obj) {
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        if (mapDelegateOrNull != null) {
            return mapDelegateOrNull.containsKey(obj);
        }
        return indexOf(obj) != -1;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean containsValue(@CheckForNull Object obj) {
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        if (mapDelegateOrNull != null) {
            return mapDelegateOrNull.containsValue(obj);
        }
        for (int i = 0; i < this.size; i++) {
            if (Objects.equal(obj, value(i))) {
                return true;
            }
        }
        return false;
    }

    Map<K, V> convertToHashFloodingResistantImplementation() {
        Map<K, V> mapCreateHashFloodingResistantDelegate = createHashFloodingResistantDelegate(hashTableMask() + 1);
        int iFirstEntryIndex = firstEntryIndex();
        while (iFirstEntryIndex >= 0) {
            mapCreateHashFloodingResistantDelegate.put(key(iFirstEntryIndex), value(iFirstEntryIndex));
            iFirstEntryIndex = getSuccessor(iFirstEntryIndex);
        }
        this.table = mapCreateHashFloodingResistantDelegate;
        this.entries = null;
        this.keys = null;
        this.values = null;
        incrementModCount();
        return mapCreateHashFloodingResistantDelegate;
    }

    Set<Map.Entry<K, V>> createEntrySet() {
        return new EntrySetView(this);
    }

    Map<K, V> createHashFloodingResistantDelegate(int i) {
        return new LinkedHashMap(i, 1.0f);
    }

    Set<K> createKeySet() {
        return new KeySetView(this);
    }

    Collection<V> createValues() {
        return new ValuesView(this);
    }

    @CheckForNull
    Map<K, V> delegateOrNull() {
        if (this.table instanceof Map) {
            return (Map) this.table;
        }
        return null;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Set<Map.Entry<K, V>> entrySet() {
        if (this.entrySetView != null) {
            return this.entrySetView;
        }
        Set<Map.Entry<K, V>> setCreateEntrySet = createEntrySet();
        this.entrySetView = setCreateEntrySet;
        return setCreateEntrySet;
    }

    Iterator<Map.Entry<K, V>> entrySetIterator() {
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        return mapDelegateOrNull != null ? mapDelegateOrNull.entrySet().iterator() : new CompactHashMap<K, V>.Itr<Map.Entry<K, V>>(this) { // from class: com.google.common.collect.CompactHashMap.2
            final CompactHashMap this$0;

            {
                this.this$0 = this;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.google.common.collect.CompactHashMap.Itr
            public Map.Entry<K, V> getOutput(int i) {
                return new MapEntry(this.this$0, i);
            }
        };
    }

    int firstEntryIndex() {
        return isEmpty() ? -1 : 0;
    }

    @Override // java.util.AbstractMap, java.util.Map
    @CheckForNull
    public V get(@CheckForNull Object obj) {
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        if (mapDelegateOrNull != null) {
            return mapDelegateOrNull.get(obj);
        }
        int iIndexOf = indexOf(obj);
        if (iIndexOf == -1) {
            return null;
        }
        accessEntry(iIndexOf);
        return value(iIndexOf);
    }

    int getSuccessor(int i) {
        if (i + 1 < this.size) {
            return i + 1;
        }
        return -1;
    }

    void incrementModCount() {
        this.metadata += 32;
    }

    void init(int i) {
        Preconditions.checkArgument(i >= 0, "Expected size must be >= 0");
        this.metadata = Ints.constrainToRange(i, 1, 1073741823);
    }

    void insertEntry(int i, @ParametricNullness K k, @ParametricNullness V v, int i2, int i3) {
        setEntry(i, CompactHashing.maskCombine(i2, 0, i3));
        setKey(i, k);
        setValue(i, v);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Set<K> keySet() {
        if (this.keySetView != null) {
            return this.keySetView;
        }
        Set<K> setCreateKeySet = createKeySet();
        this.keySetView = setCreateKeySet;
        return setCreateKeySet;
    }

    Iterator<K> keySetIterator() {
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        return mapDelegateOrNull != null ? mapDelegateOrNull.keySet().iterator() : new CompactHashMap<K, V>.Itr<K>(this) { // from class: com.google.common.collect.CompactHashMap.1
            final CompactHashMap this$0;

            {
                this.this$0 = this;
            }

            @Override // com.google.common.collect.CompactHashMap.Itr
            @ParametricNullness
            K getOutput(int i) {
                return (K) this.this$0.key(i);
            }
        };
    }

    void moveLastEntry(int i, int i2) {
        int i3;
        int i4;
        Object objRequireTable = requireTable();
        int[] iArrRequireEntries = requireEntries();
        Object[] objArrRequireKeys = requireKeys();
        Object[] objArrRequireValues = requireValues();
        int size = size() - 1;
        if (i >= size) {
            objArrRequireKeys[i] = null;
            objArrRequireValues[i] = null;
            iArrRequireEntries[i] = 0;
            return;
        }
        Object obj = objArrRequireKeys[size];
        objArrRequireKeys[i] = obj;
        objArrRequireValues[i] = objArrRequireValues[size];
        objArrRequireKeys[size] = null;
        objArrRequireValues[size] = null;
        iArrRequireEntries[i] = iArrRequireEntries[size];
        iArrRequireEntries[size] = 0;
        int iSmearedHash = Hashing.smearedHash(obj) & i2;
        int iTableGet = CompactHashing.tableGet(objRequireTable, iSmearedHash);
        int i5 = size + 1;
        if (iTableGet == i5) {
            CompactHashing.tableSet(objRequireTable, iSmearedHash, i + 1);
            return;
        }
        do {
            i3 = iTableGet - 1;
            i4 = iArrRequireEntries[i3];
            iTableGet = CompactHashing.getNext(i4, i2);
        } while (iTableGet != i5);
        iArrRequireEntries[i3] = CompactHashing.maskCombine(i4, i + 1, i2);
    }

    boolean needsAllocArrays() {
        return this.table == null;
    }

    @Override // java.util.AbstractMap, java.util.Map
    @CheckForNull
    public V put(@ParametricNullness K k, @ParametricNullness V v) {
        int i;
        int i2;
        if (needsAllocArrays()) {
            allocArrays();
        }
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        if (mapDelegateOrNull != null) {
            return mapDelegateOrNull.put(k, v);
        }
        int[] iArrRequireEntries = requireEntries();
        Object[] objArrRequireKeys = requireKeys();
        Object[] objArrRequireValues = requireValues();
        int i3 = this.size;
        int i4 = i3 + 1;
        int iSmearedHash = Hashing.smearedHash(k);
        int iHashTableMask = hashTableMask();
        int i5 = iSmearedHash & iHashTableMask;
        int iTableGet = CompactHashing.tableGet(requireTable(), i5);
        if (iTableGet != 0) {
            int hashPrefix = CompactHashing.getHashPrefix(iSmearedHash, iHashTableMask);
            int i6 = 0;
            do {
                i = iTableGet - 1;
                i2 = iArrRequireEntries[i];
                if (CompactHashing.getHashPrefix(i2, iHashTableMask) == hashPrefix && Objects.equal(k, objArrRequireKeys[i])) {
                    V v2 = (V) objArrRequireValues[i];
                    objArrRequireValues[i] = v;
                    accessEntry(i);
                    return v2;
                }
                iTableGet = CompactHashing.getNext(i2, iHashTableMask);
                i6++;
            } while (iTableGet != 0);
            if (i6 >= 9) {
                return convertToHashFloodingResistantImplementation().put(k, v);
            }
            if (i4 > iHashTableMask) {
                iHashTableMask = resizeTable(iHashTableMask, CompactHashing.newCapacity(iHashTableMask), iSmearedHash, i3);
            } else {
                iArrRequireEntries[i] = CompactHashing.maskCombine(i2, i3 + 1, iHashTableMask);
            }
        } else if (i4 > iHashTableMask) {
            iHashTableMask = resizeTable(iHashTableMask, CompactHashing.newCapacity(iHashTableMask), iSmearedHash, i3);
        } else {
            CompactHashing.tableSet(requireTable(), i5, i3 + 1);
        }
        resizeMeMaybe(i4);
        insertEntry(i3, k, v, iSmearedHash, iHashTableMask);
        this.size = i4;
        incrementModCount();
        return null;
    }

    @Override // java.util.AbstractMap, java.util.Map
    @CheckForNull
    public V remove(@CheckForNull Object obj) {
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        if (mapDelegateOrNull != null) {
            return mapDelegateOrNull.remove(obj);
        }
        V v = (V) removeHelper(obj);
        if (v == NOT_FOUND) {
            return null;
        }
        return v;
    }

    void resizeEntries(int i) {
        this.entries = Arrays.copyOf(requireEntries(), i);
        this.keys = Arrays.copyOf(requireKeys(), i);
        this.values = Arrays.copyOf(requireValues(), i);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public int size() {
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        return mapDelegateOrNull != null ? mapDelegateOrNull.size() : this.size;
    }

    public void trimToSize() {
        if (needsAllocArrays()) {
            return;
        }
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        if (mapDelegateOrNull != null) {
            Map<K, V> mapCreateHashFloodingResistantDelegate = createHashFloodingResistantDelegate(size());
            mapCreateHashFloodingResistantDelegate.putAll(mapDelegateOrNull);
            this.table = mapCreateHashFloodingResistantDelegate;
            return;
        }
        int i = this.size;
        if (i < requireEntries().length) {
            resizeEntries(i);
        }
        int iTableSize = CompactHashing.tableSize(i);
        int iHashTableMask = hashTableMask();
        if (iTableSize < iHashTableMask) {
            resizeTable(iHashTableMask, iTableSize, 0, 0);
        }
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Collection<V> values() {
        if (this.valuesView != null) {
            return this.valuesView;
        }
        Collection<V> collectionCreateValues = createValues();
        this.valuesView = collectionCreateValues;
        return collectionCreateValues;
    }

    Iterator<V> valuesIterator() {
        Map<K, V> mapDelegateOrNull = delegateOrNull();
        return mapDelegateOrNull != null ? mapDelegateOrNull.values().iterator() : new CompactHashMap<K, V>.Itr<V>(this) { // from class: com.google.common.collect.CompactHashMap.3
            final CompactHashMap this$0;

            {
                this.this$0 = this;
            }

            @Override // com.google.common.collect.CompactHashMap.Itr
            @ParametricNullness
            V getOutput(int i) {
                return (V) this.this$0.value(i);
            }
        };
    }
}
