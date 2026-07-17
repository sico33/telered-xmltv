package com.google.common.collect;

import androidx.exifinterface.media.ExifInterface;
import com.google.errorprone.annotations.Immutable;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@Immutable(containerOf = {"R", "C", ExifInterface.GPS_MEASUREMENT_INTERRUPTED})
@ElementTypesAreNonnullByDefault
final class DenseImmutableTable<R, C, V> extends RegularImmutableTable<R, C, V> {
    private final int[] cellColumnIndices;
    private final int[] cellRowIndices;
    private final int[] columnCounts;
    private final ImmutableMap<C, Integer> columnKeyToIndex;
    private final ImmutableMap<C, ImmutableMap<R, V>> columnMap;
    private final int[] rowCounts;
    private final ImmutableMap<R, Integer> rowKeyToIndex;
    private final ImmutableMap<R, ImmutableMap<C, V>> rowMap;
    private final V[][] values;

    private final class Column extends ImmutableArrayMap<R, V> {
        private final int columnIndex;
        final DenseImmutableTable this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        Column(DenseImmutableTable denseImmutableTable, int i) {
            super(denseImmutableTable.columnCounts[i]);
            this.this$0 = denseImmutableTable;
            this.columnIndex = i;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap
        @CheckForNull
        V getValue(int i) {
            return (V) this.this$0.values[i][this.columnIndex];
        }

        @Override // com.google.common.collect.ImmutableMap
        boolean isPartialView() {
            return true;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap
        ImmutableMap<R, Integer> keyToIndex() {
            return this.this$0.rowKeyToIndex;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap, com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap, com.google.common.collect.ImmutableMap
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    private final class ColumnMap extends ImmutableArrayMap<C, ImmutableMap<R, V>> {
        final DenseImmutableTable this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        private ColumnMap(DenseImmutableTable denseImmutableTable) {
            super(denseImmutableTable.columnCounts.length);
            this.this$0 = denseImmutableTable;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap
        public ImmutableMap<R, V> getValue(int i) {
            return new Column(this.this$0, i);
        }

        @Override // com.google.common.collect.ImmutableMap
        boolean isPartialView() {
            return false;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap
        ImmutableMap<C, Integer> keyToIndex() {
            return this.this$0.columnKeyToIndex;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap, com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap, com.google.common.collect.ImmutableMap
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    private static abstract class ImmutableArrayMap<K, V> extends ImmutableMap.IteratorBasedImmutableMap<K, V> {
        private final int size;

        ImmutableArrayMap(int i) {
            this.size = i;
        }

        private boolean isFull() {
            return this.size == keyToIndex().size();
        }

        @Override // com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap, com.google.common.collect.ImmutableMap
        ImmutableSet<K> createKeySet() {
            return isFull() ? keyToIndex().keySet() : super.createKeySet();
        }

        @Override // com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap
        UnmodifiableIterator<Map.Entry<K, V>> entryIterator() {
            return new AbstractIterator<Map.Entry<K, V>>(this) { // from class: com.google.common.collect.DenseImmutableTable.ImmutableArrayMap.1
                private int index = -1;
                private final int maxIndex;
                final ImmutableArrayMap this$0;

                {
                    this.this$0 = this;
                    this.maxIndex = this.this$0.keyToIndex().size();
                }

                /* JADX INFO: Access modifiers changed from: protected */
                @Override // com.google.common.collect.AbstractIterator
                @CheckForNull
                public Map.Entry<K, V> computeNext() {
                    int i = this.index;
                    while (true) {
                        this.index = i + 1;
                        if (this.index >= this.maxIndex) {
                            return endOfData();
                        }
                        Object value = this.this$0.getValue(this.index);
                        if (value != null) {
                            return Maps.immutableEntry(this.this$0.getKey(this.index), value);
                        }
                        i = this.index;
                    }
                }
            };
        }

        @Override // com.google.common.collect.ImmutableMap, java.util.Map
        @CheckForNull
        public V get(@CheckForNull Object obj) {
            Integer num = keyToIndex().get(obj);
            if (num == null) {
                return null;
            }
            return getValue(num.intValue());
        }

        K getKey(int i) {
            return keyToIndex().keySet().asList().get(i);
        }

        @CheckForNull
        abstract V getValue(int i);

        abstract ImmutableMap<K, Integer> keyToIndex();

        @Override // java.util.Map
        public int size() {
            return this.size;
        }

        @Override // com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap, com.google.common.collect.ImmutableMap
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    private final class Row extends ImmutableArrayMap<C, V> {
        private final int rowIndex;
        final DenseImmutableTable this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        Row(DenseImmutableTable denseImmutableTable, int i) {
            super(denseImmutableTable.rowCounts[i]);
            this.this$0 = denseImmutableTable;
            this.rowIndex = i;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap
        @CheckForNull
        V getValue(int i) {
            return (V) this.this$0.values[this.rowIndex][i];
        }

        @Override // com.google.common.collect.ImmutableMap
        boolean isPartialView() {
            return true;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap
        ImmutableMap<C, Integer> keyToIndex() {
            return this.this$0.columnKeyToIndex;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap, com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap, com.google.common.collect.ImmutableMap
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    private final class RowMap extends ImmutableArrayMap<R, ImmutableMap<C, V>> {
        final DenseImmutableTable this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        private RowMap(DenseImmutableTable denseImmutableTable) {
            super(denseImmutableTable.rowCounts.length);
            this.this$0 = denseImmutableTable;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap
        public ImmutableMap<C, V> getValue(int i) {
            return new Row(this.this$0, i);
        }

        @Override // com.google.common.collect.ImmutableMap
        boolean isPartialView() {
            return false;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap
        ImmutableMap<R, Integer> keyToIndex() {
            return this.this$0.rowKeyToIndex;
        }

        @Override // com.google.common.collect.DenseImmutableTable.ImmutableArrayMap, com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap, com.google.common.collect.ImmutableMap
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    DenseImmutableTable(ImmutableList<Table.Cell<R, C, V>> immutableList, ImmutableSet<R> immutableSet, ImmutableSet<C> immutableSet2) {
        this.values = (V[][]) ((Object[][]) Array.newInstance((Class<?>) Object.class, immutableSet.size(), immutableSet2.size()));
        this.rowKeyToIndex = Maps.indexMap(immutableSet);
        this.columnKeyToIndex = Maps.indexMap(immutableSet2);
        this.rowCounts = new int[this.rowKeyToIndex.size()];
        this.columnCounts = new int[this.columnKeyToIndex.size()];
        int[] iArr = new int[immutableList.size()];
        int[] iArr2 = new int[immutableList.size()];
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= immutableList.size()) {
                this.cellRowIndices = iArr;
                this.cellColumnIndices = iArr2;
                this.rowMap = new RowMap();
                this.columnMap = new ColumnMap();
                return;
            }
            Table.Cell<R, C, V> cell = immutableList.get(i2);
            R rowKey = cell.getRowKey();
            C columnKey = cell.getColumnKey();
            int iIntValue = ((Integer) Objects.requireNonNull(this.rowKeyToIndex.get(rowKey))).intValue();
            int iIntValue2 = ((Integer) Objects.requireNonNull(this.columnKeyToIndex.get(columnKey))).intValue();
            checkNoDuplicate(rowKey, columnKey, this.values[iIntValue][iIntValue2], cell.getValue());
            this.values[iIntValue][iIntValue2] = cell.getValue();
            int[] iArr3 = this.rowCounts;
            iArr3[iIntValue] = iArr3[iIntValue] + 1;
            int[] iArr4 = this.columnCounts;
            iArr4[iIntValue2] = iArr4[iIntValue2] + 1;
            iArr[i2] = iIntValue;
            iArr2[i2] = iIntValue2;
            i = i2 + 1;
        }
    }

    @Override // com.google.common.collect.ImmutableTable, com.google.common.collect.Table
    public ImmutableMap<C, Map<R, V>> columnMap() {
        return ImmutableMap.copyOf((Map) this.columnMap);
    }

    @Override // com.google.common.collect.ImmutableTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    @CheckForNull
    public V get(@CheckForNull Object obj, @CheckForNull Object obj2) {
        Integer num = this.rowKeyToIndex.get(obj);
        Integer num2 = this.columnKeyToIndex.get(obj2);
        if (num == null || num2 == null) {
            return null;
        }
        return this.values[num.intValue()][num2.intValue()];
    }

    @Override // com.google.common.collect.RegularImmutableTable
    Table.Cell<R, C, V> getCell(int i) {
        int i2 = this.cellRowIndices[i];
        int i3 = this.cellColumnIndices[i];
        return cellOf(rowKeySet().asList().get(i2), columnKeySet().asList().get(i3), Objects.requireNonNull(this.values[i2][i3]));
    }

    @Override // com.google.common.collect.RegularImmutableTable
    V getValue(int i) {
        return (V) Objects.requireNonNull(this.values[this.cellRowIndices[i]][this.cellColumnIndices[i]]);
    }

    @Override // com.google.common.collect.ImmutableTable, com.google.common.collect.Table
    public ImmutableMap<R, Map<C, V>> rowMap() {
        return ImmutableMap.copyOf((Map) this.rowMap);
    }

    @Override // com.google.common.collect.Table
    public int size() {
        return this.cellRowIndices.length;
    }

    @Override // com.google.common.collect.RegularImmutableTable, com.google.common.collect.ImmutableTable
    Object writeReplace() {
        return ImmutableTable.SerializedForm.create(this, this.cellRowIndices, this.cellColumnIndices);
    }
}
