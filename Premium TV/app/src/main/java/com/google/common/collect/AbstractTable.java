package com.google.common.collect;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class AbstractTable<R, C, V> implements Table<R, C, V> {

    @CheckForNull
    @LazyInit
    private transient Set<Table.Cell<R, C, V>> cellSet;

    @CheckForNull
    @LazyInit
    private transient Collection<V> values;

    class CellSet extends AbstractSet<Table.Cell<R, C, V>> {
        final AbstractTable this$0;

        CellSet(AbstractTable abstractTable) {
            this.this$0 = abstractTable;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            this.this$0.clear();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            if (!(obj instanceof Table.Cell)) {
                return false;
            }
            Table.Cell cell = (Table.Cell) obj;
            Map map = (Map) Maps.safeGet(this.this$0.rowMap(), cell.getRowKey());
            return map != null && Collections2.safeContains(map.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<Table.Cell<R, C, V>> iterator() {
            return this.this$0.cellIterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            if (!(obj instanceof Table.Cell)) {
                return false;
            }
            Table.Cell cell = (Table.Cell) obj;
            Map map = (Map) Maps.safeGet(this.this$0.rowMap(), cell.getRowKey());
            return map != null && Collections2.safeRemove(map.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.this$0.size();
        }
    }

    class Values extends AbstractCollection<V> {
        final AbstractTable this$0;

        Values(AbstractTable abstractTable) {
            this.this$0 = abstractTable;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public void clear() {
            this.this$0.clear();
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean contains(@CheckForNull Object obj) {
            return this.this$0.containsValue(obj);
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

    AbstractTable() {
    }

    abstract Iterator<Table.Cell<R, C, V>> cellIterator();

    @Override // com.google.common.collect.Table
    public Set<Table.Cell<R, C, V>> cellSet() {
        Set<Table.Cell<R, C, V>> set = this.cellSet;
        if (set != null) {
            return set;
        }
        Set<Table.Cell<R, C, V>> setCreateCellSet = createCellSet();
        this.cellSet = setCreateCellSet;
        return setCreateCellSet;
    }

    @Override // com.google.common.collect.Table
    public void clear() {
        Iterators.clear(cellSet().iterator());
    }

    @Override // com.google.common.collect.Table
    public Set<C> columnKeySet() {
        return columnMap().keySet();
    }

    @Override // com.google.common.collect.Table
    public boolean contains(@CheckForNull Object obj, @CheckForNull Object obj2) {
        Map map = (Map) Maps.safeGet(rowMap(), obj);
        return map != null && Maps.safeContainsKey(map, obj2);
    }

    @Override // com.google.common.collect.Table
    public boolean containsColumn(@CheckForNull Object obj) {
        return Maps.safeContainsKey(columnMap(), obj);
    }

    @Override // com.google.common.collect.Table
    public boolean containsRow(@CheckForNull Object obj) {
        return Maps.safeContainsKey(rowMap(), obj);
    }

    @Override // com.google.common.collect.Table
    public boolean containsValue(@CheckForNull Object obj) {
        Iterator<Map<C, V>> it = rowMap().values().iterator();
        while (it.hasNext()) {
            if (it.next().containsValue(obj)) {
                return true;
            }
        }
        return false;
    }

    Set<Table.Cell<R, C, V>> createCellSet() {
        return new CellSet(this);
    }

    Collection<V> createValues() {
        return new Values(this);
    }

    @Override // com.google.common.collect.Table
    public boolean equals(@CheckForNull Object obj) {
        return Tables.equalsImpl(this, obj);
    }

    @Override // com.google.common.collect.Table
    @CheckForNull
    public V get(@CheckForNull Object obj, @CheckForNull Object obj2) {
        Map map = (Map) Maps.safeGet(rowMap(), obj);
        if (map == null) {
            return null;
        }
        return (V) Maps.safeGet(map, obj2);
    }

    @Override // com.google.common.collect.Table
    public int hashCode() {
        return cellSet().hashCode();
    }

    @Override // com.google.common.collect.Table
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override // com.google.common.collect.Table
    @CheckForNull
    public V put(@ParametricNullness R r, @ParametricNullness C c, @ParametricNullness V v) {
        return row(r).put(c, v);
    }

    @Override // com.google.common.collect.Table
    public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
        for (Table.Cell<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
            put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }
    }

    @Override // com.google.common.collect.Table
    @CheckForNull
    public V remove(@CheckForNull Object obj, @CheckForNull Object obj2) {
        Map map = (Map) Maps.safeGet(rowMap(), obj);
        if (map == null) {
            return null;
        }
        return (V) Maps.safeRemove(map, obj2);
    }

    @Override // com.google.common.collect.Table
    public Set<R> rowKeySet() {
        return rowMap().keySet();
    }

    public String toString() {
        return rowMap().toString();
    }

    @Override // com.google.common.collect.Table
    public Collection<V> values() {
        Collection<V> collection = this.values;
        if (collection != null) {
            return collection;
        }
        Collection<V> collectionCreateValues = createValues();
        this.values = collectionCreateValues;
        return collectionCreateValues;
    }

    Iterator<V> valuesIterator() {
        return new TransformedIterator<Table.Cell<R, C, V>, V>(this, cellSet().iterator()) { // from class: com.google.common.collect.AbstractTable.1
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.google.common.collect.TransformedIterator
            @ParametricNullness
            public V transform(Table.Cell<R, C, V> cell) {
                return cell.getValue();
            }
        };
    }
}
