package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class RegularImmutableTable<R, C, V> extends ImmutableTable<R, C, V> {

    private final class CellSet extends IndexedImmutableSet<Table.Cell<R, C, V>> {
        final RegularImmutableTable this$0;

        private CellSet(RegularImmutableTable regularImmutableTable) {
            this.this$0 = regularImmutableTable;
        }

        @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            if (!(obj instanceof Table.Cell)) {
                return false;
            }
            Table.Cell cell = (Table.Cell) obj;
            Object obj2 = this.this$0.get(cell.getRowKey(), cell.getColumnKey());
            return obj2 != null && obj2.equals(cell.getValue());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.IndexedImmutableSet
        public Table.Cell<R, C, V> get(int i) {
            return this.this$0.getCell(i);
        }

        @Override // com.google.common.collect.ImmutableCollection
        boolean isPartialView() {
            return false;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.this$0.size();
        }

        @Override // com.google.common.collect.IndexedImmutableSet, com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    private final class Values extends ImmutableList<V> {
        final RegularImmutableTable this$0;

        private Values(RegularImmutableTable regularImmutableTable) {
            this.this$0 = regularImmutableTable;
        }

        @Override // java.util.List
        public V get(int i) {
            return (V) this.this$0.getValue(i);
        }

        @Override // com.google.common.collect.ImmutableCollection
        boolean isPartialView() {
            return true;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
        public int size() {
            return this.this$0.size();
        }

        @Override // com.google.common.collect.ImmutableList, com.google.common.collect.ImmutableCollection
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    RegularImmutableTable() {
    }

    static <R, C, V> RegularImmutableTable<R, C, V> forCells(Iterable<Table.Cell<R, C, V>> iterable) {
        return forCellsInternal(iterable, null, null);
    }

    static <R, C, V> RegularImmutableTable<R, C, V> forCells(List<Table.Cell<R, C, V>> list, @CheckForNull final Comparator<? super R> comparator, @CheckForNull final Comparator<? super C> comparator2) {
        Preconditions.checkNotNull(list);
        if (comparator != null || comparator2 != null) {
            Collections.sort(list, new Comparator(comparator, comparator2) { // from class: com.google.common.collect.RegularImmutableTable$$ExternalSyntheticLambda0
                public final Comparator f$0;
                public final Comparator f$1;

                {
                    this.f$0 = comparator;
                    this.f$1 = comparator2;
                }

                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return RegularImmutableTable.lambda$forCells$0(this.f$0, this.f$1, (Table.Cell) obj, (Table.Cell) obj2);
                }
            });
        }
        return forCellsInternal(list, comparator, comparator2);
    }

    private static <R, C, V> RegularImmutableTable<R, C, V> forCellsInternal(Iterable<Table.Cell<R, C, V>> iterable, @CheckForNull Comparator<? super R> comparator, @CheckForNull Comparator<? super C> comparator2) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        LinkedHashSet linkedHashSet2 = new LinkedHashSet();
        ImmutableList immutableListCopyOf = ImmutableList.copyOf(iterable);
        for (Table.Cell<R, C, V> cell : iterable) {
            linkedHashSet.add(cell.getRowKey());
            linkedHashSet2.add(cell.getColumnKey());
        }
        return forOrderedComponents(immutableListCopyOf, comparator == null ? ImmutableSet.copyOf((Collection) linkedHashSet) : ImmutableSet.copyOf((Collection) ImmutableList.sortedCopyOf(comparator, linkedHashSet)), comparator2 == null ? ImmutableSet.copyOf((Collection) linkedHashSet2) : ImmutableSet.copyOf((Collection) ImmutableList.sortedCopyOf(comparator2, linkedHashSet2)));
    }

    static <R, C, V> RegularImmutableTable<R, C, V> forOrderedComponents(ImmutableList<Table.Cell<R, C, V>> immutableList, ImmutableSet<R> immutableSet, ImmutableSet<C> immutableSet2) {
        return ((long) immutableList.size()) > (((long) immutableSet.size()) * ((long) immutableSet2.size())) / 2 ? new DenseImmutableTable(immutableList, immutableSet, immutableSet2) : new SparseImmutableTable(immutableList, immutableSet, immutableSet2);
    }

    static /* synthetic */ int lambda$forCells$0(Comparator comparator, Comparator comparator2, Table.Cell cell, Table.Cell cell2) {
        int iCompare = comparator == null ? 0 : comparator.compare(cell.getRowKey(), cell2.getRowKey());
        if (iCompare != 0) {
            return iCompare;
        }
        if (comparator2 != null) {
            return comparator2.compare(cell.getColumnKey(), cell2.getColumnKey());
        }
        return 0;
    }

    final void checkNoDuplicate(R r, C c, @CheckForNull V v, V v2) {
        Preconditions.checkArgument(v == null, "Duplicate key: (row=%s, column=%s), values: [%s, %s].", r, c, v2, v);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.google.common.collect.ImmutableTable, com.google.common.collect.AbstractTable
    public final ImmutableSet<Table.Cell<R, C, V>> createCellSet() {
        return isEmpty() ? ImmutableSet.of() : new CellSet();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.google.common.collect.ImmutableTable, com.google.common.collect.AbstractTable
    public final ImmutableCollection<V> createValues() {
        return isEmpty() ? ImmutableList.of() : new Values();
    }

    abstract Table.Cell<R, C, V> getCell(int i);

    abstract V getValue(int i);

    @Override // com.google.common.collect.ImmutableTable
    abstract Object writeReplace();
}
