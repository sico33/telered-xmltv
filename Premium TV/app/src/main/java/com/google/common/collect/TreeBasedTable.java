package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public class TreeBasedTable<R, C, V> extends StandardRowSortedTable<R, C, V> {
    private static final long serialVersionUID = 0;
    private final Comparator<? super C> columnComparator;

    private static class Factory<C, V> implements Supplier<TreeMap<C, V>>, Serializable {
        private static final long serialVersionUID = 0;
        final Comparator<? super C> comparator;

        Factory(Comparator<? super C> comparator) {
            this.comparator = comparator;
        }

        @Override // com.google.common.base.Supplier
        public TreeMap<C, V> get() {
            return new TreeMap<>(this.comparator);
        }
    }

    private class TreeRow extends StandardTable<R, C, V>.Row implements SortedMap<C, V> {

        @CheckForNull
        final C lowerBound;
        final TreeBasedTable this$0;

        @CheckForNull
        final C upperBound;

        @CheckForNull
        transient SortedMap<C, V> wholeRow;

        TreeRow(TreeBasedTable treeBasedTable, R r) {
            this(treeBasedTable, r, null, null);
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        TreeRow(TreeBasedTable treeBasedTable, @CheckForNull R r, @CheckForNull C c, C c2) {
            super(treeBasedTable, r);
            this.this$0 = treeBasedTable;
            this.lowerBound = c;
            this.upperBound = c2;
            Preconditions.checkArgument(c == null || c2 == null || compare(c, c2) <= 0);
        }

        @Override // java.util.SortedMap
        public Comparator<? super C> comparator() {
            return this.this$0.columnComparator();
        }

        int compare(Object obj, Object obj2) {
            return comparator().compare(obj, obj2);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.StandardTable.Row
        @CheckForNull
        public SortedMap<C, V> computeBackingRowMap() {
            updateWholeRowField();
            SortedMap<C, V> sortedMapTailMap = this.wholeRow;
            if (sortedMapTailMap == null) {
                return null;
            }
            if (this.lowerBound != null) {
                sortedMapTailMap = sortedMapTailMap.tailMap(this.lowerBound);
            }
            return this.upperBound != null ? sortedMapTailMap.headMap(this.upperBound) : sortedMapTailMap;
        }

        @Override // com.google.common.collect.StandardTable.Row, java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            return rangeContains(obj) && super.containsKey(obj);
        }

        @Override // java.util.SortedMap
        public C firstKey() {
            updateBackingRowMapField();
            if (this.backingRowMap != null) {
                return (C) ((SortedMap) this.backingRowMap).firstKey();
            }
            throw new NoSuchElementException();
        }

        @Override // java.util.SortedMap
        public SortedMap<C, V> headMap(C c) {
            Preconditions.checkArgument(rangeContains(Preconditions.checkNotNull(c)));
            return new TreeRow(this.this$0, this.rowKey, this.lowerBound, c);
        }

        @Override // java.util.AbstractMap, java.util.Map, java.util.SortedMap
        public SortedSet<C> keySet() {
            return new Maps.SortedKeySet(this);
        }

        @Override // java.util.SortedMap
        public C lastKey() {
            updateBackingRowMapField();
            if (this.backingRowMap != null) {
                return (C) ((SortedMap) this.backingRowMap).lastKey();
            }
            throw new NoSuchElementException();
        }

        @Override // com.google.common.collect.StandardTable.Row
        void maintainEmptyInvariant() {
            updateWholeRowField();
            if (this.wholeRow == null || !this.wholeRow.isEmpty()) {
                return;
            }
            this.this$0.backingMap.remove(this.rowKey);
            this.wholeRow = null;
            this.backingRowMap = null;
        }

        @Override // com.google.common.collect.StandardTable.Row, java.util.AbstractMap, java.util.Map
        @CheckForNull
        public V put(C c, V v) {
            Preconditions.checkArgument(rangeContains(Preconditions.checkNotNull(c)));
            return (V) super.put(c, v);
        }

        boolean rangeContains(@CheckForNull Object obj) {
            return obj != null && (this.lowerBound == null || compare(this.lowerBound, obj) <= 0) && (this.upperBound == null || compare(this.upperBound, obj) > 0);
        }

        @Override // java.util.SortedMap
        public SortedMap<C, V> subMap(C c, C c2) {
            Preconditions.checkArgument(rangeContains(Preconditions.checkNotNull(c)) && rangeContains(Preconditions.checkNotNull(c2)));
            return new TreeRow(this.this$0, this.rowKey, c, c2);
        }

        @Override // java.util.SortedMap
        public SortedMap<C, V> tailMap(C c) {
            Preconditions.checkArgument(rangeContains(Preconditions.checkNotNull(c)));
            return new TreeRow(this.this$0, this.rowKey, c, this.upperBound);
        }

        void updateWholeRowField() {
            if (this.wholeRow == null || (this.wholeRow.isEmpty() && this.this$0.backingMap.containsKey(this.rowKey))) {
                this.wholeRow = (SortedMap) this.this$0.backingMap.get(this.rowKey);
            }
        }
    }

    TreeBasedTable(Comparator<? super R> comparator, Comparator<? super C> comparator2) {
        super(new TreeMap(comparator), new Factory(comparator2));
        this.columnComparator = comparator2;
    }

    public static <R extends Comparable, C extends Comparable, V> TreeBasedTable<R, C, V> create() {
        return new TreeBasedTable<>(Ordering.natural(), Ordering.natural());
    }

    public static <R, C, V> TreeBasedTable<R, C, V> create(TreeBasedTable<R, C, ? extends V> treeBasedTable) {
        TreeBasedTable<R, C, V> treeBasedTable2 = new TreeBasedTable<>(treeBasedTable.rowComparator(), treeBasedTable.columnComparator());
        treeBasedTable2.putAll(treeBasedTable);
        return treeBasedTable2;
    }

    public static <R, C, V> TreeBasedTable<R, C, V> create(Comparator<? super R> comparator, Comparator<? super C> comparator2) {
        Preconditions.checkNotNull(comparator);
        Preconditions.checkNotNull(comparator2);
        return new TreeBasedTable<>(comparator, comparator2);
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ Set cellSet() {
        return super.cellSet();
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ void clear() {
        super.clear();
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.StandardTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ Map column(Object obj) {
        return super.column(obj);
    }

    @Deprecated
    public Comparator<? super C> columnComparator() {
        return this.columnComparator;
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ Set columnKeySet() {
        return super.columnKeySet();
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ Map columnMap() {
        return super.columnMap();
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ boolean contains(@CheckForNull Object obj, @CheckForNull Object obj2) {
        return super.contains(obj, obj2);
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ boolean containsColumn(@CheckForNull Object obj) {
        return super.containsColumn(obj);
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ boolean containsRow(@CheckForNull Object obj) {
        return super.containsRow(obj);
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ boolean containsValue(@CheckForNull Object obj) {
        return super.containsValue(obj);
    }

    @Override // com.google.common.collect.StandardTable
    Iterator<C> createColumnKeyIterator() {
        Comparator<? super C> comparatorColumnComparator = columnComparator();
        return new AbstractIterator<C>(this, Iterators.mergeSorted(Iterables.transform(this.backingMap.values(), new Function() { // from class: com.google.common.collect.TreeBasedTable$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return ((Map) obj).keySet().iterator();
            }
        }), comparatorColumnComparator), comparatorColumnComparator) { // from class: com.google.common.collect.TreeBasedTable.1

            @CheckForNull
            C lastValue;
            final Comparator val$comparator;
            final Iterator val$merged;

            {
                this.val$merged = it;
                this.val$comparator = comparatorColumnComparator;
            }

            @Override // com.google.common.collect.AbstractIterator
            @CheckForNull
            protected C computeNext() {
                while (this.val$merged.hasNext()) {
                    C c = (C) this.val$merged.next();
                    if (!(this.lastValue != null && this.val$comparator.compare(c, this.lastValue) == 0)) {
                        this.lastValue = c;
                        return this.lastValue;
                    }
                }
                this.lastValue = null;
                return endOfData();
            }
        };
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ boolean equals(@CheckForNull Object obj) {
        return super.equals(obj);
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    @CheckForNull
    public /* bridge */ /* synthetic */ Object get(@CheckForNull Object obj, @CheckForNull Object obj2) {
        return super.get(obj, obj2);
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ int hashCode() {
        return super.hashCode();
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ boolean isEmpty() {
        return super.isEmpty();
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    @CheckForNull
    public /* bridge */ /* synthetic */ Object put(Object obj, Object obj2, Object obj3) {
        return super.put(obj, obj2, obj3);
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ void putAll(Table table) {
        super.putAll(table);
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    @CheckForNull
    public /* bridge */ /* synthetic */ Object remove(@CheckForNull Object obj, @CheckForNull Object obj2) {
        return super.remove(obj, obj2);
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.Table
    public SortedMap<C, V> row(R r) {
        return new TreeRow(this, r);
    }

    @Deprecated
    public Comparator<? super R> rowComparator() {
        return (Comparator) Objects.requireNonNull(rowKeySet().comparator());
    }

    @Override // com.google.common.collect.StandardRowSortedTable, com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public SortedSet<R> rowKeySet() {
        return super.rowKeySet();
    }

    @Override // com.google.common.collect.StandardRowSortedTable, com.google.common.collect.StandardTable, com.google.common.collect.Table
    public SortedMap<R, Map<C, V>> rowMap() {
        return super.rowMap();
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ int size() {
        return super.size();
    }

    @Override // com.google.common.collect.AbstractTable
    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    @Override // com.google.common.collect.StandardTable, com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public /* bridge */ /* synthetic */ Collection values() {
        return super.values();
    }
}
