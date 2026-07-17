package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import kotlin.UShort;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {
    private static final byte ABSENT = -1;
    private static final int BYTE_MASK = 255;
    private static final int BYTE_MAX_SIZE = 128;
    static final ImmutableMap<Object, Object> EMPTY = new RegularImmutableMap(null, new Object[0], 0);
    private static final int SHORT_MASK = 65535;
    private static final int SHORT_MAX_SIZE = 32768;
    private static final long serialVersionUID = 0;
    final transient Object[] alternatingKeysAndValues;

    @CheckForNull
    private final transient Object hashTable;
    private final transient int size;

    static class EntrySet<K, V> extends ImmutableSet<Map.Entry<K, V>> {
        private final transient Object[] alternatingKeysAndValues;
        private final transient int keyOffset;
        private final transient ImmutableMap<K, V> map;
        private final transient int size;

        EntrySet(ImmutableMap<K, V> immutableMap, Object[] objArr, int i, int i2) {
            this.map = immutableMap;
            this.alternatingKeysAndValues = objArr;
            this.keyOffset = i;
            this.size = i2;
        }

        @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            Object value = entry.getValue();
            return value != null && value.equals(this.map.get(key));
        }

        @Override // com.google.common.collect.ImmutableCollection
        int copyIntoArray(Object[] objArr, int i) {
            return asList().copyIntoArray(objArr, i);
        }

        @Override // com.google.common.collect.ImmutableSet
        ImmutableList<Map.Entry<K, V>> createAsList() {
            return new ImmutableList<Map.Entry<K, V>>(this) { // from class: com.google.common.collect.RegularImmutableMap.EntrySet.1
                final EntrySet this$0;

                {
                    this.this$0 = this;
                }

                @Override // java.util.List
                public Map.Entry<K, V> get(int i) {
                    Preconditions.checkElementIndex(i, this.this$0.size);
                    return new AbstractMap.SimpleImmutableEntry(Objects.requireNonNull(this.this$0.alternatingKeysAndValues[(i * 2) + this.this$0.keyOffset]), Objects.requireNonNull(this.this$0.alternatingKeysAndValues[(i * 2) + (this.this$0.keyOffset ^ 1)]));
                }

                @Override // com.google.common.collect.ImmutableCollection
                public boolean isPartialView() {
                    return true;
                }

                @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
                public int size() {
                    return this.this$0.size;
                }

                @Override // com.google.common.collect.ImmutableList, com.google.common.collect.ImmutableCollection
                Object writeReplace() {
                    return super.writeReplace();
                }
            };
        }

        @Override // com.google.common.collect.ImmutableCollection
        boolean isPartialView() {
            return true;
        }

        @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet, com.google.common.collect.SortedIterable
        public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
            return asList().iterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.size;
        }

        @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    static final class KeySet<K> extends ImmutableSet<K> {
        private final transient ImmutableList<K> list;
        private final transient ImmutableMap<K, ?> map;

        KeySet(ImmutableMap<K, ?> immutableMap, ImmutableList<K> immutableList) {
            this.map = immutableMap;
            this.list = immutableList;
        }

        @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
        public ImmutableList<K> asList() {
            return this.list;
        }

        @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            return this.map.get(obj) != null;
        }

        @Override // com.google.common.collect.ImmutableCollection
        int copyIntoArray(Object[] objArr, int i) {
            return asList().copyIntoArray(objArr, i);
        }

        @Override // com.google.common.collect.ImmutableCollection
        boolean isPartialView() {
            return true;
        }

        @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet, com.google.common.collect.SortedIterable
        public UnmodifiableIterator<K> iterator() {
            return asList().iterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.map.size();
        }

        @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    static final class KeysOrValuesAsList extends ImmutableList<Object> {
        private final transient Object[] alternatingKeysAndValues;
        private final transient int offset;
        private final transient int size;

        KeysOrValuesAsList(Object[] objArr, int i, int i2) {
            this.alternatingKeysAndValues = objArr;
            this.offset = i;
            this.size = i2;
        }

        @Override // java.util.List
        public Object get(int i) {
            Preconditions.checkElementIndex(i, this.size);
            return Objects.requireNonNull(this.alternatingKeysAndValues[(i * 2) + this.offset]);
        }

        @Override // com.google.common.collect.ImmutableCollection
        boolean isPartialView() {
            return true;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
        public int size() {
            return this.size;
        }

        @Override // com.google.common.collect.ImmutableList, com.google.common.collect.ImmutableCollection
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    private RegularImmutableMap(@CheckForNull Object obj, Object[] objArr, int i) {
        this.hashTable = obj;
        this.alternatingKeysAndValues = objArr;
        this.size = i;
    }

    static <K, V> RegularImmutableMap<K, V> create(int i, Object[] objArr) {
        return create(i, objArr, null);
    }

    static <K, V> RegularImmutableMap<K, V> create(int i, Object[] objArr, ImmutableMap.Builder<K, V> builder) {
        if (i == 0) {
            return (RegularImmutableMap) EMPTY;
        }
        if (i == 1) {
            CollectPreconditions.checkEntryNotNull(Objects.requireNonNull(objArr[0]), Objects.requireNonNull(objArr[1]));
            return new RegularImmutableMap<>(null, objArr, 1);
        }
        Preconditions.checkPositionIndex(i, objArr.length >> 1);
        Object objCreateHashTable = createHashTable(objArr, i, ImmutableSet.chooseTableSize(i), 0);
        if (objCreateHashTable instanceof Object[]) {
            Object[] objArr2 = (Object[]) objCreateHashTable;
            ImmutableMap.Builder.DuplicateKey duplicateKey = (ImmutableMap.Builder.DuplicateKey) objArr2[2];
            if (builder == null) {
                throw duplicateKey.exception();
            }
            builder.duplicateKey = duplicateKey;
            Object obj = objArr2[0];
            i = ((Integer) objArr2[1]).intValue();
            objArr = Arrays.copyOf(objArr, i * 2);
            objCreateHashTable = obj;
        }
        return new RegularImmutableMap<>(objCreateHashTable, objArr, i);
    }

    @CheckForNull
    private static Object createHashTable(Object[] objArr, int i, int i2, int i3) {
        Object[] objArr2;
        if (i == 1) {
            CollectPreconditions.checkEntryNotNull(Objects.requireNonNull(objArr[i3]), Objects.requireNonNull(objArr[i3 ^ 1]));
            return null;
        }
        int i4 = i2 - 1;
        ImmutableMap.Builder.DuplicateKey duplicateKey = null;
        if (i2 <= 128) {
            byte[] bArr = new byte[i2];
            Arrays.fill(bArr, (byte) -1);
            int i5 = 0;
            ImmutableMap.Builder.DuplicateKey duplicateKey2 = null;
            for (int i6 = 0; i6 < i; i6++) {
                int i7 = (i6 * 2) + i3;
                int i8 = (i5 * 2) + i3;
                Object objRequireNonNull = Objects.requireNonNull(objArr[i7]);
                Object objRequireNonNull2 = Objects.requireNonNull(objArr[i7 ^ 1]);
                CollectPreconditions.checkEntryNotNull(objRequireNonNull, objRequireNonNull2);
                int iSmear = Hashing.smear(objRequireNonNull.hashCode());
                while (true) {
                    int i9 = iSmear & i4;
                    int i10 = bArr[i9] & 255;
                    if (i10 == 255) {
                        bArr[i9] = (byte) i8;
                        if (i5 < i6) {
                            objArr[i8] = objRequireNonNull;
                            objArr[i8 ^ 1] = objRequireNonNull2;
                        }
                        i5++;
                        break;
                    }
                    if (objRequireNonNull.equals(objArr[i10])) {
                        duplicateKey2 = new ImmutableMap.Builder.DuplicateKey(objRequireNonNull, objRequireNonNull2, Objects.requireNonNull(objArr[i10 ^ 1]));
                        objArr[i10 ^ 1] = objRequireNonNull2;
                        break;
                    }
                    iSmear = i9 + 1;
                }
            }
            if (i5 == i) {
                return bArr;
            }
            objArr2 = new Object[]{bArr, Integer.valueOf(i5), duplicateKey2};
        } else if (i2 <= 32768) {
            short[] sArr = new short[i2];
            Arrays.fill(sArr, (short) -1);
            int i11 = 0;
            ImmutableMap.Builder.DuplicateKey duplicateKey3 = null;
            for (int i12 = 0; i12 < i; i12++) {
                int i13 = (i12 * 2) + i3;
                int i14 = (i11 * 2) + i3;
                Object objRequireNonNull3 = Objects.requireNonNull(objArr[i13]);
                Object objRequireNonNull4 = Objects.requireNonNull(objArr[i13 ^ 1]);
                CollectPreconditions.checkEntryNotNull(objRequireNonNull3, objRequireNonNull4);
                int iSmear2 = Hashing.smear(objRequireNonNull3.hashCode());
                while (true) {
                    int i15 = iSmear2 & i4;
                    int i16 = sArr[i15] & UShort.MAX_VALUE;
                    if (i16 == 65535) {
                        sArr[i15] = (short) i14;
                        if (i11 < i12) {
                            objArr[i14] = objRequireNonNull3;
                            objArr[i14 ^ 1] = objRequireNonNull4;
                        }
                        i11++;
                        break;
                    }
                    if (objRequireNonNull3.equals(objArr[i16])) {
                        duplicateKey3 = new ImmutableMap.Builder.DuplicateKey(objRequireNonNull3, objRequireNonNull4, Objects.requireNonNull(objArr[i16 ^ 1]));
                        objArr[i16 ^ 1] = objRequireNonNull4;
                        break;
                    }
                    iSmear2 = i15 + 1;
                }
            }
            if (i11 == i) {
                return sArr;
            }
            objArr2 = new Object[]{sArr, Integer.valueOf(i11), duplicateKey3};
        } else {
            int[] iArr = new int[i2];
            Arrays.fill(iArr, -1);
            int i17 = 0;
            for (int i18 = 0; i18 < i; i18++) {
                int i19 = (i18 * 2) + i3;
                int i20 = (i17 * 2) + i3;
                Object objRequireNonNull5 = Objects.requireNonNull(objArr[i19]);
                Object objRequireNonNull6 = Objects.requireNonNull(objArr[i19 ^ 1]);
                CollectPreconditions.checkEntryNotNull(objRequireNonNull5, objRequireNonNull6);
                int iSmear3 = Hashing.smear(objRequireNonNull5.hashCode());
                while (true) {
                    int i21 = iSmear3 & i4;
                    int i22 = iArr[i21];
                    if (i22 == -1) {
                        iArr[i21] = i20;
                        if (i17 < i18) {
                            objArr[i20] = objRequireNonNull5;
                            objArr[i20 ^ 1] = objRequireNonNull6;
                        }
                        i17++;
                        break;
                    }
                    if (objRequireNonNull5.equals(objArr[i22])) {
                        duplicateKey = new ImmutableMap.Builder.DuplicateKey(objRequireNonNull5, objRequireNonNull6, Objects.requireNonNull(objArr[i22 ^ 1]));
                        objArr[i22 ^ 1] = objRequireNonNull6;
                        break;
                    }
                    iSmear3 = i21 + 1;
                }
            }
            if (i17 == i) {
                return iArr;
            }
            objArr2 = new Object[]{iArr, Integer.valueOf(i17), duplicateKey};
        }
        return objArr2;
    }

    @CheckForNull
    static Object createHashTableOrThrow(Object[] objArr, int i, int i2, int i3) {
        Object objCreateHashTable = createHashTable(objArr, i, i2, i3);
        if (objCreateHashTable instanceof Object[]) {
            throw ((ImmutableMap.Builder.DuplicateKey) ((Object[]) objCreateHashTable)[2]).exception();
        }
        return objCreateHashTable;
    }

    @CheckForNull
    static Object get(@CheckForNull Object obj, Object[] objArr, int i, int i2, @CheckForNull Object obj2) {
        if (obj2 == null) {
            return null;
        }
        if (i == 1) {
            if (Objects.requireNonNull(objArr[i2]).equals(obj2)) {
                return Objects.requireNonNull(objArr[i2 ^ 1]);
            }
            return null;
        }
        if (obj == null) {
            return null;
        }
        if (obj instanceof byte[]) {
            byte[] bArr = (byte[]) obj;
            int length = bArr.length;
            int iSmear = Hashing.smear(obj2.hashCode());
            while (true) {
                int i3 = iSmear & (length - 1);
                int i4 = bArr[i3] & 255;
                if (i4 == 255) {
                    return null;
                }
                if (obj2.equals(objArr[i4])) {
                    return objArr[i4 ^ 1];
                }
                iSmear = i3 + 1;
            }
        } else if (obj instanceof short[]) {
            short[] sArr = (short[]) obj;
            int length2 = sArr.length;
            int iSmear2 = Hashing.smear(obj2.hashCode());
            while (true) {
                int i5 = iSmear2 & (length2 - 1);
                int i6 = sArr[i5] & UShort.MAX_VALUE;
                if (i6 == 65535) {
                    return null;
                }
                if (obj2.equals(objArr[i6])) {
                    return objArr[i6 ^ 1];
                }
                iSmear2 = i5 + 1;
            }
        } else {
            int[] iArr = (int[]) obj;
            int length3 = iArr.length;
            int iSmear3 = Hashing.smear(obj2.hashCode());
            while (true) {
                int i7 = iSmear3 & (length3 - 1);
                int i8 = iArr[i7];
                if (i8 == -1) {
                    return null;
                }
                if (obj2.equals(objArr[i8])) {
                    return objArr[i8 ^ 1];
                }
                iSmear3 = i7 + 1;
            }
        }
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new EntrySet(this, this.alternatingKeysAndValues, 0, this.size);
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableSet<K> createKeySet() {
        return new KeySet(this, new KeysOrValuesAsList(this.alternatingKeysAndValues, 0, this.size));
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableCollection<V> createValues() {
        return new KeysOrValuesAsList(this.alternatingKeysAndValues, 1, this.size);
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    @CheckForNull
    public V get(@CheckForNull Object obj) {
        V v = (V) get(this.hashTable, this.alternatingKeysAndValues, this.size, 0, obj);
        if (v == null) {
            return null;
        }
        return v;
    }

    @Override // com.google.common.collect.ImmutableMap
    boolean isPartialView() {
        return false;
    }

    @Override // java.util.Map
    public int size() {
        return this.size;
    }

    @Override // com.google.common.collect.ImmutableMap
    Object writeReplace() {
        return super.writeReplace();
    }
}
