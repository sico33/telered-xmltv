package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
class StandardTable<R, C, V> extends AbstractTable<R, C, V> implements Serializable {
    private static final long serialVersionUID = 0;

    @GwtTransient
    final Map<R, Map<C, V>> backingMap;

    @CheckForNull
    @LazyInit
    private transient Set<C> columnKeySet;

    @CheckForNull
    @LazyInit
    private transient StandardTable<R, C, V>.ColumnMap columnMap;

    @GwtTransient
    final Supplier<? extends Map<C, V>> factory;

    @CheckForNull
    @LazyInit
    private transient Map<R, Map<C, V>> rowMap;

    private class CellIterator implements Iterator<Table.Cell<R, C, V>> {
        Iterator<Map.Entry<C, V>> columnIterator;

        @CheckForNull
        Map.Entry<R, Map<C, V>> rowEntry;
        final Iterator<Map.Entry<R, Map<C, V>>> rowIterator;
        final StandardTable this$0;

        private CellIterator(StandardTable standardTable) {
            this.this$0 = standardTable;
            this.rowIterator = this.this$0.backingMap.entrySet().iterator();
            this.columnIterator = Iterators.emptyModifiableIterator();
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return this.rowIterator.hasNext() || this.columnIterator.hasNext();
        }

        @Override // java.util.Iterator
        public Table.Cell<R, C, V> next() {
            if (!this.columnIterator.hasNext()) {
                this.rowEntry = this.rowIterator.next();
                this.columnIterator = this.rowEntry.getValue().entrySet().iterator();
            }
            Objects.requireNonNull(this.rowEntry);
            Map.Entry<C, V> next = this.columnIterator.next();
            return Tables.immutableCell(this.rowEntry.getKey(), next.getKey(), next.getValue());
        }

        @Override // java.util.Iterator
        public void remove() {
            this.columnIterator.remove();
            if (((Map) ((Map.Entry) Objects.requireNonNull(this.rowEntry)).getValue()).isEmpty()) {
                this.rowIterator.remove();
                this.rowEntry = null;
            }
        }
    }

    private class Column extends Maps.ViewCachingAbstractMap<R, V> {
        final C columnKey;
        final StandardTable this$0;

        private class EntrySet extends Sets.ImprovedAbstractSet<Map.Entry<R, V>> {
            final Column this$1;

            private EntrySet(Column column) {
                this.this$1 = column;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public void clear() {
                this.this$1.removeFromColumnIf(Predicates.alwaysTrue());
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entry = (Map.Entry) obj;
                return this.this$1.this$0.containsMapping(entry.getKey(), this.this$1.columnKey, entry.getValue());
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean isEmpty() {
                return !this.this$1.this$0.containsColumn(this.this$1.columnKey);
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<Map.Entry<R, V>> iterator() {
                return new EntrySetIterator();
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean remove(@CheckForNull Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entry = (Map.Entry) obj;
                return this.this$1.this$0.removeMapping(entry.getKey(), this.this$1.columnKey, entry.getValue());
            }

            @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean retainAll(Collection<?> collection) {
                return this.this$1.removeFromColumnIf(Predicates.not(Predicates.in(collection)));
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                int i = 0;
                Iterator<Map<C, V>> it = this.this$1.this$0.backingMap.values().iterator();
                while (true) {
                    int i2 = i;
                    if (!it.hasNext()) {
                        return i2;
                    }
                    i = it.next().containsKey(this.this$1.columnKey) ? i2 + 1 : i2;
                }
            }
        }

        private class EntrySetIterator extends AbstractIterator<Map.Entry<R, V>> {
            final Iterator<Map.Entry<R, Map<C, V>>> iterator;
            final Column this$1;

            private EntrySetIterator(Column column) {
                this.this$1 = column;
                this.iterator = this.this$1.this$0.backingMap.entrySet().iterator();
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // com.google.common.collect.AbstractIterator
            @CheckForNull
            public Map.Entry<R, V> computeNext() {
                while (this.iterator.hasNext()) {
                    Map.Entry<R, Map<C, V>> next = this.iterator.next();
                    if (next.getValue().containsKey(this.this$1.columnKey)) {
                        return new AbstractMapEntry<R, V>(this, next) { // from class: com.google.common.collect.StandardTable.Column.EntrySetIterator.1EntryImpl
                            final EntrySetIterator this$2;
                            final Map.Entry val$entry;

                            {
                                this.this$2 = this;
                                this.val$entry = next;
                            }

                            @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
                            public R getKey() {
                                return (R) this.val$entry.getKey();
                            }

                            @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
                            public V getValue() {
                                return (V) ((Map) this.val$entry.getValue()).get(this.this$2.this$1.columnKey);
                            }

                            /* JADX WARN: Multi-variable type inference failed */
                            @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
                            public V setValue(V v) {
                                return (V) NullnessCasts.uncheckedCastNullableTToT(((Map) this.val$entry.getValue()).put(this.this$2.this$1.columnKey, Preconditions.checkNotNull(v)));
                            }
                        };
                    }
                }
                return endOfData();
            }
        }

        private class KeySet extends Maps.KeySet<R, V> {
            final Column this$1;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            KeySet(Column column) {
                super(column);
                this.this$1 = column;
            }

            @Override // com.google.common.collect.Maps.KeySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                return this.this$1.this$0.contains(obj, this.this$1.columnKey);
            }

            @Override // com.google.common.collect.Maps.KeySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean remove(@CheckForNull Object obj) {
                return this.this$1.this$0.remove(obj, this.this$1.columnKey) != null;
            }

            @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean retainAll(Collection<?> collection) {
                return this.this$1.removeFromColumnIf(Maps.keyPredicateOnEntries(Predicates.not(Predicates.in(collection))));
            }
        }

        private class Values extends Maps.Values<R, V> {
            final Column this$1;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            Values(Column column) {
                super(column);
                this.this$1 = column;
            }

            @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
            public boolean remove(@CheckForNull Object obj) {
                return obj != null && this.this$1.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.equalTo(obj)));
            }

            @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
            public boolean removeAll(Collection<?> collection) {
                return this.this$1.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.in(collection)));
            }

            @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
            public boolean retainAll(Collection<?> collection) {
                return this.this$1.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.not(Predicates.in(collection))));
            }
        }

        Column(StandardTable standardTable, C c) {
            this.this$0 = standardTable;
            this.columnKey = (C) Preconditions.checkNotNull(c);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            return this.this$0.contains(obj, this.columnKey);
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        Set<Map.Entry<R, V>> createEntrySet() {
            return new EntrySet();
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        Set<R> createKeySet() {
            return new KeySet(this);
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        Collection<V> createValues() {
            return new Values(this);
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public V get(@CheckForNull Object obj) {
            return (V) this.this$0.get(obj, this.columnKey);
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public V put(R r, V v) {
            return (V) this.this$0.put(r, this.columnKey, v);
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public V remove(@CheckForNull Object obj) {
            return (V) this.this$0.remove(obj, this.columnKey);
        }

        boolean removeFromColumnIf(Predicate<? super Map.Entry<R, V>> predicate) {
            boolean z = false;
            Iterator<Map.Entry<R, Map<C, V>>> it = this.this$0.backingMap.entrySet().iterator();
            while (true) {
                boolean z2 = z;
                if (!it.hasNext()) {
                    return z2;
                }
                Map.Entry<R, Map<C, V>> next = it.next();
                Map<C, V> value = next.getValue();
                V v = value.get(this.columnKey);
                if (v != null && predicate.apply(Maps.immutableEntry(next.getKey(), v))) {
                    value.remove(this.columnKey);
                    z2 = true;
                    if (value.isEmpty()) {
                        it.remove();
                    }
                }
                z = z2;
            }
        }
    }

    private class ColumnKeyIterator extends AbstractIterator<C> {
        Iterator<Map.Entry<C, V>> entryIterator;
        final Iterator<Map<C, V>> mapIterator;
        final Map<C, V> seen;
        final StandardTable this$0;

        private ColumnKeyIterator(StandardTable standardTable) {
            this.this$0 = standardTable;
            this.seen = this.this$0.factory.get();
            this.mapIterator = this.this$0.backingMap.values().iterator();
            this.entryIterator = Iterators.emptyIterator();
        }

        @Override // com.google.common.collect.AbstractIterator
        @CheckForNull
        protected C computeNext() {
            while (true) {
                if (this.entryIterator.hasNext()) {
                    Map.Entry<C, V> next = this.entryIterator.next();
                    if (!this.seen.containsKey(next.getKey())) {
                        this.seen.put(next.getKey(), next.getValue());
                        return next.getKey();
                    }
                } else {
                    if (!this.mapIterator.hasNext()) {
                        return endOfData();
                    }
                    this.entryIterator = this.mapIterator.next().entrySet().iterator();
                }
            }
        }
    }

    private class ColumnKeySet extends StandardTable<R, C, V>.TableSet<C> {
        final StandardTable this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        private ColumnKeySet(StandardTable standardTable) {
            super();
            this.this$0 = standardTable;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            return this.this$0.containsColumn(obj);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<C> iterator() {
            return this.this$0.createColumnKeyIterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            boolean z = false;
            if (obj == null) {
                return false;
            }
            Iterator<Map<C, V>> it = this.this$0.backingMap.values().iterator();
            while (true) {
                boolean z2 = z;
                if (!it.hasNext()) {
                    return z2;
                }
                Map<C, V> next = it.next();
                if (next.keySet().remove(obj)) {
                    z2 = true;
                    if (next.isEmpty()) {
                        it.remove();
                    }
                }
                z = z2;
            }
        }

        @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean removeAll(Collection<?> collection) {
            Preconditions.checkNotNull(collection);
            boolean z = false;
            Iterator<Map<C, V>> it = this.this$0.backingMap.values().iterator();
            while (true) {
                boolean z2 = z;
                if (!it.hasNext()) {
                    return z2;
                }
                Map<C, V> next = it.next();
                if (Iterators.removeAll(next.keySet().iterator(), collection)) {
                    z2 = true;
                    if (next.isEmpty()) {
                        it.remove();
                    }
                }
                z = z2;
            }
        }

        @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean retainAll(Collection<?> collection) {
            Preconditions.checkNotNull(collection);
            boolean z = false;
            Iterator<Map<C, V>> it = this.this$0.backingMap.values().iterator();
            while (true) {
                boolean z2 = z;
                if (!it.hasNext()) {
                    return z2;
                }
                Map<C, V> next = it.next();
                if (next.keySet().retainAll(collection)) {
                    z2 = true;
                    if (next.isEmpty()) {
                        it.remove();
                    }
                }
                z = z2;
            }
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return Iterators.size(iterator());
        }
    }

    private class ColumnMap extends Maps.ViewCachingAbstractMap<C, Map<R, V>> {
        final StandardTable this$0;

        private final class ColumnMapEntrySet extends StandardTable<R, C, V>.TableSet<Map.Entry<C, Map<R, V>>> {
            final ColumnMap this$1;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            private ColumnMapEntrySet(ColumnMap columnMap) {
                super();
                this.this$1 = columnMap;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                if (obj instanceof Map.Entry) {
                    Map.Entry entry = (Map.Entry) obj;
                    if (this.this$1.this$0.containsColumn(entry.getKey())) {
                        return ((Map) Objects.requireNonNull(this.this$1.get(entry.getKey()))).equals(entry.getValue());
                    }
                }
                return false;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<Map.Entry<C, Map<R, V>>> iterator() {
                return Maps.asMapEntryIterator(this.this$1.this$0.columnKeySet(), new Function<C, Map<R, V>>(this) { // from class: com.google.common.collect.StandardTable.ColumnMap.ColumnMapEntrySet.1
                    final ColumnMapEntrySet this$2;

                    {
                        this.this$2 = this;
                    }

                    @Override // com.google.common.base.Function
                    public Map<R, V> apply(C c) {
                        return this.this$2.this$1.this$0.column(c);
                    }
                });
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean remove(@CheckForNull Object obj) {
                if (!contains(obj) || !(obj instanceof Map.Entry)) {
                    return false;
                }
                this.this$1.this$0.removeColumn(((Map.Entry) obj).getKey());
                return true;
            }

            @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean removeAll(Collection<?> collection) {
                Preconditions.checkNotNull(collection);
                return Sets.removeAllImpl(this, collection.iterator());
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean retainAll(Collection<?> collection) {
                Preconditions.checkNotNull(collection);
                boolean z = false;
                for (Object obj : Lists.newArrayList(this.this$1.this$0.columnKeySet().iterator())) {
                    if (!collection.contains(Maps.immutableEntry(obj, this.this$1.this$0.column(obj)))) {
                        this.this$1.this$0.removeColumn(obj);
                        z = true;
                    }
                }
                return z;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return this.this$1.this$0.columnKeySet().size();
            }
        }

        private class ColumnMapValues extends Maps.Values<C, Map<R, V>> {
            final ColumnMap this$1;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            ColumnMapValues(ColumnMap columnMap) {
                super(columnMap);
                this.this$1 = columnMap;
            }

            @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
            public boolean remove(@CheckForNull Object obj) {
                for (Map.Entry<C, Map<R, V>> entry : this.this$1.entrySet()) {
                    if (entry.getValue().equals(obj)) {
                        this.this$1.this$0.removeColumn(entry.getKey());
                        return true;
                    }
                }
                return false;
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
            public boolean removeAll(Collection<?> collection) {
                Preconditions.checkNotNull(collection);
                boolean z = false;
                for (Object obj : Lists.newArrayList(this.this$1.this$0.columnKeySet().iterator())) {
                    if (collection.contains(this.this$1.this$0.column(obj))) {
                        this.this$1.this$0.removeColumn(obj);
                        z = true;
                    }
                }
                return z;
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
            public boolean retainAll(Collection<?> collection) {
                Preconditions.checkNotNull(collection);
                boolean z = false;
                for (Object obj : Lists.newArrayList(this.this$1.this$0.columnKeySet().iterator())) {
                    if (!collection.contains(this.this$1.this$0.column(obj))) {
                        this.this$1.this$0.removeColumn(obj);
                        z = true;
                    }
                }
                return z;
            }
        }

        private ColumnMap(StandardTable standardTable) {
            this.this$0 = standardTable;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            return this.this$0.containsColumn(obj);
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        public Set<Map.Entry<C, Map<R, V>>> createEntrySet() {
            return new ColumnMapEntrySet();
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        Collection<Map<R, V>> createValues() {
            return new ColumnMapValues(this);
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Map<R, V> get(@CheckForNull Object obj) {
            if (this.this$0.containsColumn(obj)) {
                return this.this$0.column(Objects.requireNonNull(obj));
            }
            return null;
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap, java.util.AbstractMap, java.util.Map
        public Set<C> keySet() {
            return this.this$0.columnKeySet();
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Map<R, V> remove(@CheckForNull Object obj) {
            if (this.this$0.containsColumn(obj)) {
                return this.this$0.removeColumn(obj);
            }
            return null;
        }
    }

    class Row extends Maps.IteratorBasedAbstractMap<C, V> {

        @CheckForNull
        Map<C, V> backingRowMap;
        final R rowKey;
        final StandardTable this$0;

        Row(StandardTable standardTable, R r) {
            this.this$0 = standardTable;
            this.rowKey = (R) Preconditions.checkNotNull(r);
        }

        @Override // com.google.common.collect.Maps.IteratorBasedAbstractMap, java.util.AbstractMap, java.util.Map
        public void clear() {
            updateBackingRowMapField();
            if (this.backingRowMap != null) {
                this.backingRowMap.clear();
            }
            maintainEmptyInvariant();
        }

        @CheckForNull
        Map<C, V> computeBackingRowMap() {
            return this.this$0.backingMap.get(this.rowKey);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            updateBackingRowMapField();
            return (obj == null || this.backingRowMap == null || !Maps.safeContainsKey(this.backingRowMap, obj)) ? false : true;
        }

        @Override // com.google.common.collect.Maps.IteratorBasedAbstractMap
        Iterator<Map.Entry<C, V>> entryIterator() {
            updateBackingRowMapField();
            return this.backingRowMap == null ? Iterators.emptyModifiableIterator() : new Iterator<Map.Entry<C, V>>(this, this.backingRowMap.entrySet().iterator()) { // from class: com.google.common.collect.StandardTable.Row.1
                final Row this$1;
                final Iterator val$iterator;

                {
                    this.this$1 = this;
                    this.val$iterator = it;
                }

                @Override // java.util.Iterator
                public boolean hasNext() {
                    return this.val$iterator.hasNext();
                }

                @Override // java.util.Iterator
                public Map.Entry<C, V> next() {
                    return this.this$1.wrapEntry((Map.Entry) this.val$iterator.next());
                }

                @Override // java.util.Iterator
                public void remove() {
                    this.val$iterator.remove();
                    this.this$1.maintainEmptyInvariant();
                }
            };
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public V get(@CheckForNull Object obj) {
            updateBackingRowMapField();
            if (obj == null || this.backingRowMap == null) {
                return null;
            }
            return (V) Maps.safeGet(this.backingRowMap, obj);
        }

        void maintainEmptyInvariant() {
            updateBackingRowMapField();
            if (this.backingRowMap == null || !this.backingRowMap.isEmpty()) {
                return;
            }
            this.this$0.backingMap.remove(this.rowKey);
            this.backingRowMap = null;
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public V put(C c, V v) {
            Preconditions.checkNotNull(c);
            Preconditions.checkNotNull(v);
            return (this.backingRowMap == null || this.backingRowMap.isEmpty()) ? (V) this.this$0.put(this.rowKey, c, v) : this.backingRowMap.put(c, v);
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public V remove(@CheckForNull Object obj) {
            updateBackingRowMapField();
            if (this.backingRowMap == null) {
                return null;
            }
            V v = (V) Maps.safeRemove(this.backingRowMap, obj);
            maintainEmptyInvariant();
            return v;
        }

        @Override // com.google.common.collect.Maps.IteratorBasedAbstractMap, java.util.AbstractMap, java.util.Map
        public int size() {
            updateBackingRowMapField();
            if (this.backingRowMap == null) {
                return 0;
            }
            return this.backingRowMap.size();
        }

        final void updateBackingRowMapField() {
            if (this.backingRowMap == null || (this.backingRowMap.isEmpty() && this.this$0.backingMap.containsKey(this.rowKey))) {
                this.backingRowMap = computeBackingRowMap();
            }
        }

        Map.Entry<C, V> wrapEntry(Map.Entry<C, V> entry) {
            return new ForwardingMapEntry<C, V>(this, entry) { // from class: com.google.common.collect.StandardTable.Row.2
                final Map.Entry val$entry;

                {
                    this.val$entry = entry;
                }

                /* JADX INFO: Access modifiers changed from: protected */
                @Override // com.google.common.collect.ForwardingMapEntry, com.google.common.collect.ForwardingObject
                public Map.Entry<C, V> delegate() {
                    return this.val$entry;
                }

                @Override // com.google.common.collect.ForwardingMapEntry, java.util.Map.Entry
                public boolean equals(@CheckForNull Object obj) {
                    return standardEquals(obj);
                }

                /* JADX WARN: Multi-variable type inference failed */
                @Override // com.google.common.collect.ForwardingMapEntry, java.util.Map.Entry
                public V setValue(V v) {
                    return (V) super.setValue(Preconditions.checkNotNull(v));
                }
            };
        }
    }

    class RowMap extends Maps.ViewCachingAbstractMap<R, Map<C, V>> {
        final StandardTable this$0;

        private final class EntrySet extends StandardTable<R, C, V>.TableSet<Map.Entry<R, Map<C, V>>> {
            final RowMap this$1;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            private EntrySet(RowMap rowMap) {
                super();
                this.this$1 = rowMap;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entry = (Map.Entry) obj;
                return entry.getKey() != null && (entry.getValue() instanceof Map) && Collections2.safeContains(this.this$1.this$0.backingMap.entrySet(), entry);
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<Map.Entry<R, Map<C, V>>> iterator() {
                return Maps.asMapEntryIterator(this.this$1.this$0.backingMap.keySet(), new Function<R, Map<C, V>>(this) { // from class: com.google.common.collect.StandardTable.RowMap.EntrySet.1
                    final EntrySet this$2;

                    {
                        this.this$2 = this;
                    }

                    @Override // com.google.common.base.Function
                    public Map<C, V> apply(R r) {
                        return this.this$2.this$1.this$0.row(r);
                    }
                });
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean remove(@CheckForNull Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entry = (Map.Entry) obj;
                return entry.getKey() != null && (entry.getValue() instanceof Map) && this.this$1.this$0.backingMap.entrySet().remove(entry);
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return this.this$1.this$0.backingMap.size();
            }
        }

        RowMap(StandardTable standardTable) {
            this.this$0 = standardTable;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            return this.this$0.containsRow(obj);
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        protected Set<Map.Entry<R, Map<C, V>>> createEntrySet() {
            return new EntrySet();
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Map<C, V> get(@CheckForNull Object obj) {
            if (this.this$0.containsRow(obj)) {
                return this.this$0.row(Objects.requireNonNull(obj));
            }
            return null;
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Map<C, V> remove(@CheckForNull Object obj) {
            if (obj == null) {
                return null;
            }
            return this.this$0.backingMap.remove(obj);
        }
    }

    private abstract class TableSet<T> extends Sets.ImprovedAbstractSet<T> {
        final StandardTable this$0;

        private TableSet(StandardTable standardTable) {
            this.this$0 = standardTable;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            this.this$0.backingMap.clear();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean isEmpty() {
            return this.this$0.backingMap.isEmpty();
        }
    }

    StandardTable(Map<R, Map<C, V>> map, Supplier<? extends Map<C, V>> supplier) {
        this.backingMap = map;
        this.factory = supplier;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean containsMapping(@CheckForNull Object obj, @CheckForNull Object obj2, @CheckForNull Object obj3) {
        return obj3 != null && obj3.equals(get(obj, obj2));
    }

    private Map<C, V> getOrCreate(R r) {
        Map<C, V> map = this.backingMap.get(r);
        if (map != null) {
            return map;
        }
        Map<C, V> map2 = this.factory.get();
        this.backingMap.put(r, map2);
        return map2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Map<R, V> removeColumn(@CheckForNull Object obj) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        Iterator<Map.Entry<R, Map<C, V>>> it = this.backingMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<R, Map<C, V>> next = it.next();
            V vRemove = next.getValue().remove(obj);
            if (vRemove != null) {
                linkedHashMap.put(next.getKey(), vRemove);
                if (next.getValue().isEmpty()) {
                    it.remove();
                }
            }
        }
        return linkedHashMap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean removeMapping(@CheckForNull Object obj, @CheckForNull Object obj2, @CheckForNull Object obj3) {
        if (!containsMapping(obj, obj2, obj3)) {
            return false;
        }
        remove(obj, obj2);
        return true;
    }

    @Override // com.google.common.collect.AbstractTable
    Iterator<Table.Cell<R, C, V>> cellIterator() {
        return new CellIterator();
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public Set<Table.Cell<R, C, V>> cellSet() {
        return super.cellSet();
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public void clear() {
        this.backingMap.clear();
    }

    @Override // com.google.common.collect.Table
    public Map<R, V> column(C c) {
        return new Column(this, c);
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public Set<C> columnKeySet() {
        Set<C> set = this.columnKeySet;
        if (set != null) {
            return set;
        }
        ColumnKeySet columnKeySet = new ColumnKeySet();
        this.columnKeySet = columnKeySet;
        return columnKeySet;
    }

    @Override // com.google.common.collect.Table
    public Map<C, Map<R, V>> columnMap() {
        StandardTable<R, C, V>.ColumnMap columnMap = this.columnMap;
        if (columnMap != null) {
            return columnMap;
        }
        StandardTable<R, C, V>.ColumnMap columnMap2 = new ColumnMap();
        this.columnMap = columnMap2;
        return columnMap2;
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public boolean contains(@CheckForNull Object obj, @CheckForNull Object obj2) {
        return (obj == null || obj2 == null || !super.contains(obj, obj2)) ? false : true;
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public boolean containsColumn(@CheckForNull Object obj) {
        if (obj == null) {
            return false;
        }
        Iterator<Map<C, V>> it = this.backingMap.values().iterator();
        while (it.hasNext()) {
            if (Maps.safeContainsKey(it.next(), obj)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public boolean containsRow(@CheckForNull Object obj) {
        return obj != null && Maps.safeContainsKey(this.backingMap, obj);
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public boolean containsValue(@CheckForNull Object obj) {
        return obj != null && super.containsValue(obj);
    }

    Iterator<C> createColumnKeyIterator() {
        return new ColumnKeyIterator();
    }

    Map<R, Map<C, V>> createRowMap() {
        return new RowMap(this);
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    @CheckForNull
    public V get(@CheckForNull Object obj, @CheckForNull Object obj2) {
        if (obj == null || obj2 == null) {
            return null;
        }
        return (V) super.get(obj, obj2);
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public boolean isEmpty() {
        return this.backingMap.isEmpty();
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    @CheckForNull
    public V put(R r, C c, V v) {
        Preconditions.checkNotNull(r);
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(v);
        return getOrCreate(r).put(c, v);
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    @CheckForNull
    public V remove(@CheckForNull Object obj, @CheckForNull Object obj2) {
        if (obj == null || obj2 == null) {
            return null;
        }
        Map map = (Map) Maps.safeGet(this.backingMap, obj);
        if (map == null) {
            return null;
        }
        V v = (V) map.remove(obj2);
        if (map.isEmpty()) {
            this.backingMap.remove(obj);
        }
        return v;
    }

    @Override // com.google.common.collect.Table
    public Map<C, V> row(R r) {
        return new Row(this, r);
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public Set<R> rowKeySet() {
        return rowMap().keySet();
    }

    @Override // com.google.common.collect.Table
    public Map<R, Map<C, V>> rowMap() {
        Map<R, Map<C, V>> map = this.rowMap;
        if (map != null) {
            return map;
        }
        Map<R, Map<C, V>> mapCreateRowMap = createRowMap();
        this.rowMap = mapCreateRowMap;
        return mapCreateRowMap;
    }

    @Override // com.google.common.collect.Table
    public int size() {
        int size = 0;
        Iterator<Map<C, V>> it = this.backingMap.values().iterator();
        while (true) {
            int i = size;
            if (!it.hasNext()) {
                return i;
            }
            size = it.next().size() + i;
        }
    }

    @Override // com.google.common.collect.AbstractTable, com.google.common.collect.Table
    public Collection<V> values() {
        return super.values();
    }
}
