package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class AbstractMapBasedMultimap<K, V> extends AbstractMultimap<K, V> implements Serializable {
    private static final long serialVersionUID = 2447537837011683357L;
    private transient Map<K, Collection<V>> map;
    private transient int totalSize;

    private class AsMap extends Maps.ViewCachingAbstractMap<K, Collection<V>> {
        final transient Map<K, Collection<V>> submap;
        final AbstractMapBasedMultimap this$0;

        class AsMapEntries extends Maps.EntrySet<K, Collection<V>> {
            final AsMap this$1;

            AsMapEntries(AsMap asMap) {
                this.this$1 = asMap;
            }

            @Override // com.google.common.collect.Maps.EntrySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                return Collections2.safeContains(this.this$1.submap.entrySet(), obj);
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return new AsMapIterator(this.this$1);
            }

            @Override // com.google.common.collect.Maps.EntrySet
            Map<K, Collection<V>> map() {
                return this.this$1;
            }

            @Override // com.google.common.collect.Maps.EntrySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean remove(@CheckForNull Object obj) {
                if (!contains(obj)) {
                    return false;
                }
                this.this$1.this$0.removeValuesForKey(((Map.Entry) Objects.requireNonNull((Map.Entry) obj)).getKey());
                return true;
            }
        }

        class AsMapIterator implements Iterator<Map.Entry<K, Collection<V>>> {

            @CheckForNull
            Collection<V> collection;
            final Iterator<Map.Entry<K, Collection<V>>> delegateIterator;
            final AsMap this$1;

            AsMapIterator(AsMap asMap) {
                this.this$1 = asMap;
                this.delegateIterator = this.this$1.submap.entrySet().iterator();
            }

            @Override // java.util.Iterator
            public boolean hasNext() {
                return this.delegateIterator.hasNext();
            }

            @Override // java.util.Iterator
            public Map.Entry<K, Collection<V>> next() {
                Map.Entry<K, Collection<V>> next = this.delegateIterator.next();
                this.collection = next.getValue();
                return this.this$1.wrapEntry(next);
            }

            @Override // java.util.Iterator
            public void remove() {
                Preconditions.checkState(this.collection != null, "no calls to next() since the last call to remove()");
                this.delegateIterator.remove();
                AbstractMapBasedMultimap.access$220(this.this$1.this$0, this.collection.size());
                this.collection.clear();
                this.collection = null;
            }
        }

        AsMap(AbstractMapBasedMultimap abstractMapBasedMultimap, Map<K, Collection<V>> map) {
            this.this$0 = abstractMapBasedMultimap;
            this.submap = map;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public void clear() {
            if (this.submap == this.this$0.map) {
                this.this$0.clear();
            } else {
                Iterators.clear(new AsMapIterator(this));
            }
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            return Maps.safeContainsKey(this.submap, obj);
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        protected Set<Map.Entry<K, Collection<V>>> createEntrySet() {
            return new AsMapEntries(this);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean equals(@CheckForNull Object obj) {
            return this == obj || this.submap.equals(obj);
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Collection<V> get(@CheckForNull Object obj) {
            Collection<V> collection = (Collection) Maps.safeGet(this.submap, obj);
            if (collection == null) {
                return null;
            }
            return this.this$0.wrapCollection(obj, collection);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public int hashCode() {
            return this.submap.hashCode();
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap, java.util.AbstractMap, java.util.Map
        public Set<K> keySet() {
            return this.this$0.keySet();
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Collection<V> remove(@CheckForNull Object obj) {
            Collection<V> collectionRemove = this.submap.remove(obj);
            if (collectionRemove == null) {
                return null;
            }
            Collection<V> collectionCreateCollection = this.this$0.createCollection();
            collectionCreateCollection.addAll(collectionRemove);
            AbstractMapBasedMultimap.access$220(this.this$0, collectionRemove.size());
            collectionRemove.clear();
            return collectionCreateCollection;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public int size() {
            return this.submap.size();
        }

        @Override // java.util.AbstractMap
        public String toString() {
            return this.submap.toString();
        }

        Map.Entry<K, Collection<V>> wrapEntry(Map.Entry<K, Collection<V>> entry) {
            K key = entry.getKey();
            return Maps.immutableEntry(key, this.this$0.wrapCollection(key, entry.getValue()));
        }
    }

    private abstract class Itr<T> implements Iterator<T> {
        final Iterator<Map.Entry<K, Collection<V>>> keyIterator;
        final AbstractMapBasedMultimap this$0;

        @CheckForNull
        K key = null;

        @CheckForNull
        Collection<V> collection = null;
        Iterator<V> valueIterator = Iterators.emptyModifiableIterator();

        Itr(AbstractMapBasedMultimap abstractMapBasedMultimap) {
            this.this$0 = abstractMapBasedMultimap;
            this.keyIterator = abstractMapBasedMultimap.map.entrySet().iterator();
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return this.keyIterator.hasNext() || this.valueIterator.hasNext();
        }

        @Override // java.util.Iterator
        @ParametricNullness
        public T next() {
            if (!this.valueIterator.hasNext()) {
                Map.Entry<K, Collection<V>> next = this.keyIterator.next();
                this.key = next.getKey();
                this.collection = next.getValue();
                this.valueIterator = this.collection.iterator();
            }
            return output(NullnessCasts.uncheckedCastNullableTToT(this.key), this.valueIterator.next());
        }

        abstract T output(@ParametricNullness K k, @ParametricNullness V v);

        @Override // java.util.Iterator
        public void remove() {
            this.valueIterator.remove();
            if (((Collection) Objects.requireNonNull(this.collection)).isEmpty()) {
                this.keyIterator.remove();
            }
            AbstractMapBasedMultimap.access$210(this.this$0);
        }
    }

    private class KeySet extends Maps.KeySet<K, Collection<V>> {
        final AbstractMapBasedMultimap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        KeySet(AbstractMapBasedMultimap abstractMapBasedMultimap, Map<K, Collection<V>> map) {
            super(map);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override // com.google.common.collect.Maps.KeySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            Iterators.clear(iterator());
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean containsAll(Collection<?> collection) {
            return map().keySet().containsAll(collection);
        }

        @Override // java.util.AbstractSet, java.util.Collection, java.util.Set
        public boolean equals(@CheckForNull Object obj) {
            return this == obj || map().keySet().equals(obj);
        }

        @Override // java.util.AbstractSet, java.util.Collection, java.util.Set
        public int hashCode() {
            return map().keySet().hashCode();
        }

        @Override // com.google.common.collect.Maps.KeySet, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<K> iterator() {
            return new Iterator<K>(this, map().entrySet().iterator()) { // from class: com.google.common.collect.AbstractMapBasedMultimap.KeySet.1

                @CheckForNull
                Map.Entry<K, Collection<V>> entry;
                final KeySet this$1;
                final Iterator val$entryIterator;

                {
                    this.this$1 = this;
                    this.val$entryIterator = it;
                }

                @Override // java.util.Iterator
                public boolean hasNext() {
                    return this.val$entryIterator.hasNext();
                }

                @Override // java.util.Iterator
                @ParametricNullness
                public K next() {
                    this.entry = (Map.Entry) this.val$entryIterator.next();
                    return this.entry.getKey();
                }

                @Override // java.util.Iterator
                public void remove() {
                    Preconditions.checkState(this.entry != null, "no calls to next() since the last call to remove()");
                    Collection<V> value = this.entry.getValue();
                    this.val$entryIterator.remove();
                    AbstractMapBasedMultimap.access$220(this.this$1.this$0, value.size());
                    value.clear();
                    this.entry = null;
                }
            };
        }

        @Override // com.google.common.collect.Maps.KeySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            int i;
            Collection<V> collectionRemove = map().remove(obj);
            if (collectionRemove != null) {
                int size = collectionRemove.size();
                collectionRemove.clear();
                AbstractMapBasedMultimap.access$220(this.this$0, size);
                i = size;
            } else {
                i = 0;
            }
            return i > 0;
        }
    }

    private final class NavigableAsMap extends AbstractMapBasedMultimap<K, V>.SortedAsMap implements NavigableMap<K, Collection<V>> {
        final AbstractMapBasedMultimap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        NavigableAsMap(AbstractMapBasedMultimap abstractMapBasedMultimap, NavigableMap<K, Collection<V>> navigableMap) {
            super(abstractMapBasedMultimap, navigableMap);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public Map.Entry<K, Collection<V>> ceilingEntry(@ParametricNullness K k) {
            Map.Entry<K, Collection<V>> entryCeilingEntry = sortedMap().ceilingEntry(k);
            if (entryCeilingEntry == null) {
                return null;
            }
            return wrapEntry(entryCeilingEntry);
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public K ceilingKey(@ParametricNullness K k) {
            return sortedMap().ceilingKey(k);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedAsMap, com.google.common.collect.Maps.ViewCachingAbstractMap
        public NavigableSet<K> createKeySet() {
            return new NavigableKeySet(this.this$0, sortedMap());
        }

        @Override // java.util.NavigableMap
        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        @Override // java.util.NavigableMap
        public NavigableMap<K, Collection<V>> descendingMap() {
            return new NavigableAsMap(this.this$0, sortedMap().descendingMap());
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public Map.Entry<K, Collection<V>> firstEntry() {
            Map.Entry<K, Collection<V>> entryFirstEntry = sortedMap().firstEntry();
            if (entryFirstEntry == null) {
                return null;
            }
            return wrapEntry(entryFirstEntry);
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public Map.Entry<K, Collection<V>> floorEntry(@ParametricNullness K k) {
            Map.Entry<K, Collection<V>> entryFloorEntry = sortedMap().floorEntry(k);
            if (entryFloorEntry == null) {
                return null;
            }
            return wrapEntry(entryFloorEntry);
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public K floorKey(@ParametricNullness K k) {
            return sortedMap().floorKey(k);
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedAsMap, java.util.SortedMap, java.util.NavigableMap
        public NavigableMap<K, Collection<V>> headMap(@ParametricNullness K k) {
            return headMap(k, false);
        }

        @Override // java.util.NavigableMap
        public NavigableMap<K, Collection<V>> headMap(@ParametricNullness K k, boolean z) {
            return new NavigableAsMap(this.this$0, sortedMap().headMap(k, z));
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public Map.Entry<K, Collection<V>> higherEntry(@ParametricNullness K k) {
            Map.Entry<K, Collection<V>> entryHigherEntry = sortedMap().higherEntry(k);
            if (entryHigherEntry == null) {
                return null;
            }
            return wrapEntry(entryHigherEntry);
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public K higherKey(@ParametricNullness K k) {
            return sortedMap().higherKey(k);
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedAsMap, com.google.common.collect.AbstractMapBasedMultimap.AsMap, com.google.common.collect.Maps.ViewCachingAbstractMap, java.util.AbstractMap, java.util.Map
        public NavigableSet<K> keySet() {
            return (NavigableSet) super.keySet();
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public Map.Entry<K, Collection<V>> lastEntry() {
            Map.Entry<K, Collection<V>> entryLastEntry = sortedMap().lastEntry();
            if (entryLastEntry == null) {
                return null;
            }
            return wrapEntry(entryLastEntry);
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public Map.Entry<K, Collection<V>> lowerEntry(@ParametricNullness K k) {
            Map.Entry<K, Collection<V>> entryLowerEntry = sortedMap().lowerEntry(k);
            if (entryLowerEntry == null) {
                return null;
            }
            return wrapEntry(entryLowerEntry);
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public K lowerKey(@ParametricNullness K k) {
            return sortedMap().lowerKey(k);
        }

        @Override // java.util.NavigableMap
        public NavigableSet<K> navigableKeySet() {
            return keySet();
        }

        @CheckForNull
        Map.Entry<K, Collection<V>> pollAsMapEntry(Iterator<Map.Entry<K, Collection<V>>> it) {
            if (!it.hasNext()) {
                return null;
            }
            Map.Entry<K, Collection<V>> next = it.next();
            Collection<V> collectionCreateCollection = this.this$0.createCollection();
            collectionCreateCollection.addAll(next.getValue());
            it.remove();
            return Maps.immutableEntry(next.getKey(), this.this$0.unmodifiableCollectionSubclass(collectionCreateCollection));
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public Map.Entry<K, Collection<V>> pollFirstEntry() {
            return pollAsMapEntry(entrySet().iterator());
        }

        @Override // java.util.NavigableMap
        @CheckForNull
        public Map.Entry<K, Collection<V>> pollLastEntry() {
            return pollAsMapEntry(descendingMap().entrySet().iterator());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedAsMap
        public NavigableMap<K, Collection<V>> sortedMap() {
            return (NavigableMap) super.sortedMap();
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedAsMap, java.util.SortedMap, java.util.NavigableMap
        public NavigableMap<K, Collection<V>> subMap(@ParametricNullness K k, @ParametricNullness K k2) {
            return subMap(k, true, k2, false);
        }

        @Override // java.util.NavigableMap
        public NavigableMap<K, Collection<V>> subMap(@ParametricNullness K k, boolean z, @ParametricNullness K k2, boolean z2) {
            return new NavigableAsMap(this.this$0, sortedMap().subMap(k, z, k2, z2));
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedAsMap, java.util.SortedMap, java.util.NavigableMap
        public NavigableMap<K, Collection<V>> tailMap(@ParametricNullness K k) {
            return tailMap(k, true);
        }

        @Override // java.util.NavigableMap
        public NavigableMap<K, Collection<V>> tailMap(@ParametricNullness K k, boolean z) {
            return new NavigableAsMap(this.this$0, sortedMap().tailMap(k, z));
        }
    }

    private final class NavigableKeySet extends AbstractMapBasedMultimap<K, V>.SortedKeySet implements NavigableSet<K> {
        final AbstractMapBasedMultimap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        NavigableKeySet(AbstractMapBasedMultimap abstractMapBasedMultimap, NavigableMap<K, Collection<V>> navigableMap) {
            super(abstractMapBasedMultimap, navigableMap);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public K ceiling(@ParametricNullness K k) {
            return sortedMap().ceilingKey(k);
        }

        @Override // java.util.NavigableSet
        public Iterator<K> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override // java.util.NavigableSet
        public NavigableSet<K> descendingSet() {
            return new NavigableKeySet(this.this$0, sortedMap().descendingMap());
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public K floor(@ParametricNullness K k) {
            return sortedMap().floorKey(k);
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedKeySet, java.util.SortedSet, java.util.NavigableSet
        public NavigableSet<K> headSet(@ParametricNullness K k) {
            return headSet(k, false);
        }

        @Override // java.util.NavigableSet
        public NavigableSet<K> headSet(@ParametricNullness K k, boolean z) {
            return new NavigableKeySet(this.this$0, sortedMap().headMap(k, z));
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public K higher(@ParametricNullness K k) {
            return sortedMap().higherKey(k);
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public K lower(@ParametricNullness K k) {
            return sortedMap().lowerKey(k);
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public K pollFirst() {
            return (K) Iterators.pollNext(iterator());
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public K pollLast() {
            return (K) Iterators.pollNext(descendingIterator());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedKeySet
        public NavigableMap<K, Collection<V>> sortedMap() {
            return (NavigableMap) super.sortedMap();
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedKeySet, java.util.SortedSet, java.util.NavigableSet
        public NavigableSet<K> subSet(@ParametricNullness K k, @ParametricNullness K k2) {
            return subSet(k, true, k2, false);
        }

        @Override // java.util.NavigableSet
        public NavigableSet<K> subSet(@ParametricNullness K k, boolean z, @ParametricNullness K k2, boolean z2) {
            return new NavigableKeySet(this.this$0, sortedMap().subMap(k, z, k2, z2));
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap.SortedKeySet, java.util.SortedSet, java.util.NavigableSet
        public NavigableSet<K> tailSet(@ParametricNullness K k) {
            return tailSet(k, true);
        }

        @Override // java.util.NavigableSet
        public NavigableSet<K> tailSet(@ParametricNullness K k, boolean z) {
            return new NavigableKeySet(this.this$0, sortedMap().tailMap(k, z));
        }
    }

    private class RandomAccessWrappedList extends AbstractMapBasedMultimap<K, V>.WrappedList implements RandomAccess {
        RandomAccessWrappedList(@ParametricNullness AbstractMapBasedMultimap abstractMapBasedMultimap, K k, @CheckForNull List<V> list, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
            super(abstractMapBasedMultimap, k, list, wrappedCollection);
        }
    }

    private class SortedAsMap extends AbstractMapBasedMultimap<K, V>.AsMap implements SortedMap<K, Collection<V>> {

        @CheckForNull
        SortedSet<K> sortedKeySet;
        final AbstractMapBasedMultimap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        SortedAsMap(AbstractMapBasedMultimap abstractMapBasedMultimap, SortedMap<K, Collection<V>> sortedMap) {
            super(abstractMapBasedMultimap, sortedMap);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override // java.util.SortedMap
        @CheckForNull
        public Comparator<? super K> comparator() {
            return sortedMap().comparator();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        public SortedSet<K> createKeySet() {
            return new SortedKeySet(this.this$0, sortedMap());
        }

        @Override // java.util.SortedMap
        @ParametricNullness
        public K firstKey() {
            return sortedMap().firstKey();
        }

        public SortedMap<K, Collection<V>> headMap(@ParametricNullness K k) {
            return new SortedAsMap(this.this$0, sortedMap().headMap(k));
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap.AsMap, com.google.common.collect.Maps.ViewCachingAbstractMap, java.util.AbstractMap, java.util.Map
        public SortedSet<K> keySet() {
            SortedSet<K> sortedSet = this.sortedKeySet;
            if (sortedSet != null) {
                return sortedSet;
            }
            SortedSet<K> sortedSetCreateKeySet = createKeySet();
            this.sortedKeySet = sortedSetCreateKeySet;
            return sortedSetCreateKeySet;
        }

        @Override // java.util.SortedMap
        @ParametricNullness
        public K lastKey() {
            return sortedMap().lastKey();
        }

        SortedMap<K, Collection<V>> sortedMap() {
            return (SortedMap) this.submap;
        }

        public SortedMap<K, Collection<V>> subMap(@ParametricNullness K k, @ParametricNullness K k2) {
            return new SortedAsMap(this.this$0, sortedMap().subMap(k, k2));
        }

        public SortedMap<K, Collection<V>> tailMap(@ParametricNullness K k) {
            return new SortedAsMap(this.this$0, sortedMap().tailMap(k));
        }
    }

    private class SortedKeySet extends AbstractMapBasedMultimap<K, V>.KeySet implements SortedSet<K> {
        final AbstractMapBasedMultimap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        SortedKeySet(AbstractMapBasedMultimap abstractMapBasedMultimap, SortedMap<K, Collection<V>> sortedMap) {
            super(abstractMapBasedMultimap, sortedMap);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override // java.util.SortedSet
        @CheckForNull
        public Comparator<? super K> comparator() {
            return sortedMap().comparator();
        }

        @Override // java.util.SortedSet
        @ParametricNullness
        public K first() {
            return sortedMap().firstKey();
        }

        public SortedSet<K> headSet(@ParametricNullness K k) {
            return new SortedKeySet(this.this$0, sortedMap().headMap(k));
        }

        @Override // java.util.SortedSet
        @ParametricNullness
        public K last() {
            return sortedMap().lastKey();
        }

        SortedMap<K, Collection<V>> sortedMap() {
            return (SortedMap) super.map();
        }

        public SortedSet<K> subSet(@ParametricNullness K k, @ParametricNullness K k2) {
            return new SortedKeySet(this.this$0, sortedMap().subMap(k, k2));
        }

        public SortedSet<K> tailSet(@ParametricNullness K k) {
            return new SortedKeySet(this.this$0, sortedMap().tailMap(k));
        }
    }

    class WrappedCollection extends AbstractCollection<V> {

        @CheckForNull
        final AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor;

        @CheckForNull
        final Collection<V> ancestorDelegate;
        Collection<V> delegate;

        @ParametricNullness
        final K key;
        final AbstractMapBasedMultimap this$0;

        class WrappedIterator implements Iterator<V> {
            final Iterator<V> delegateIterator;
            final Collection<V> originalDelegate;
            final WrappedCollection this$1;

            WrappedIterator(WrappedCollection wrappedCollection) {
                this.this$1 = wrappedCollection;
                this.originalDelegate = this.this$1.delegate;
                this.delegateIterator = AbstractMapBasedMultimap.iteratorOrListIterator(wrappedCollection.delegate);
            }

            WrappedIterator(WrappedCollection wrappedCollection, Iterator<V> it) {
                this.this$1 = wrappedCollection;
                this.originalDelegate = this.this$1.delegate;
                this.delegateIterator = it;
            }

            Iterator<V> getDelegateIterator() {
                validateIterator();
                return this.delegateIterator;
            }

            @Override // java.util.Iterator
            public boolean hasNext() {
                validateIterator();
                return this.delegateIterator.hasNext();
            }

            @Override // java.util.Iterator
            @ParametricNullness
            public V next() {
                validateIterator();
                return this.delegateIterator.next();
            }

            @Override // java.util.Iterator
            public void remove() {
                this.delegateIterator.remove();
                AbstractMapBasedMultimap.access$210(this.this$1.this$0);
                this.this$1.removeIfEmpty();
            }

            void validateIterator() {
                this.this$1.refreshIfEmpty();
                if (this.this$1.delegate != this.originalDelegate) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        WrappedCollection(@ParametricNullness AbstractMapBasedMultimap abstractMapBasedMultimap, K k, @CheckForNull Collection<V> collection, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
            this.this$0 = abstractMapBasedMultimap;
            this.key = k;
            this.delegate = collection;
            this.ancestor = wrappedCollection;
            this.ancestorDelegate = wrappedCollection == null ? null : wrappedCollection.getDelegate();
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean add(@ParametricNullness V v) {
            refreshIfEmpty();
            boolean zIsEmpty = this.delegate.isEmpty();
            boolean zAdd = this.delegate.add(v);
            if (zAdd) {
                AbstractMapBasedMultimap.access$208(this.this$0);
                if (zIsEmpty) {
                    addToMap();
                }
            }
            return zAdd;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean addAll(Collection<? extends V> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int size = size();
            boolean zAddAll = this.delegate.addAll(collection);
            if (!zAddAll) {
                return zAddAll;
            }
            AbstractMapBasedMultimap.access$212(this.this$0, this.delegate.size() - size);
            if (size != 0) {
                return zAddAll;
            }
            addToMap();
            return zAddAll;
        }

        void addToMap() {
            if (this.ancestor != null) {
                this.ancestor.addToMap();
            } else {
                this.this$0.map.put(this.key, this.delegate);
            }
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public void clear() {
            int size = size();
            if (size == 0) {
                return;
            }
            this.delegate.clear();
            AbstractMapBasedMultimap.access$220(this.this$0, size);
            removeIfEmpty();
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean contains(@CheckForNull Object obj) {
            refreshIfEmpty();
            return this.delegate.contains(obj);
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean containsAll(Collection<?> collection) {
            refreshIfEmpty();
            return this.delegate.containsAll(collection);
        }

        @Override // java.util.Collection
        public boolean equals(@CheckForNull Object obj) {
            if (obj == this) {
                return true;
            }
            refreshIfEmpty();
            return this.delegate.equals(obj);
        }

        @CheckForNull
        AbstractMapBasedMultimap<K, V>.WrappedCollection getAncestor() {
            return this.ancestor;
        }

        Collection<V> getDelegate() {
            return this.delegate;
        }

        @ParametricNullness
        K getKey() {
            return this.key;
        }

        @Override // java.util.Collection
        public int hashCode() {
            refreshIfEmpty();
            return this.delegate.hashCode();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable
        public Iterator<V> iterator() {
            refreshIfEmpty();
            return new WrappedIterator(this);
        }

        void refreshIfEmpty() {
            Collection<V> collection;
            if (this.ancestor != null) {
                this.ancestor.refreshIfEmpty();
                if (this.ancestor.getDelegate() != this.ancestorDelegate) {
                    throw new ConcurrentModificationException();
                }
            } else {
                if (!this.delegate.isEmpty() || (collection = (Collection) this.this$0.map.get(this.key)) == null) {
                    return;
                }
                this.delegate = collection;
            }
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean remove(@CheckForNull Object obj) {
            refreshIfEmpty();
            boolean zRemove = this.delegate.remove(obj);
            if (zRemove) {
                AbstractMapBasedMultimap.access$210(this.this$0);
                removeIfEmpty();
            }
            return zRemove;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean removeAll(Collection<?> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int size = size();
            boolean zRemoveAll = this.delegate.removeAll(collection);
            if (!zRemoveAll) {
                return zRemoveAll;
            }
            AbstractMapBasedMultimap.access$212(this.this$0, this.delegate.size() - size);
            removeIfEmpty();
            return zRemoveAll;
        }

        void removeIfEmpty() {
            if (this.ancestor != null) {
                this.ancestor.removeIfEmpty();
            } else if (this.delegate.isEmpty()) {
                this.this$0.map.remove(this.key);
            }
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean retainAll(Collection<?> collection) {
            Preconditions.checkNotNull(collection);
            int size = size();
            boolean zRetainAll = this.delegate.retainAll(collection);
            if (zRetainAll) {
                AbstractMapBasedMultimap.access$212(this.this$0, this.delegate.size() - size);
                removeIfEmpty();
            }
            return zRetainAll;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public int size() {
            refreshIfEmpty();
            return this.delegate.size();
        }

        @Override // java.util.AbstractCollection
        public String toString() {
            refreshIfEmpty();
            return this.delegate.toString();
        }
    }

    class WrappedList extends AbstractMapBasedMultimap<K, V>.WrappedCollection implements List<V> {
        final AbstractMapBasedMultimap this$0;

        private class WrappedListIterator extends AbstractMapBasedMultimap<K, V>.WrappedCollection.WrappedIterator implements ListIterator<V> {
            final WrappedList this$1;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            WrappedListIterator(WrappedList wrappedList) {
                super(wrappedList);
                this.this$1 = wrappedList;
            }

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            public WrappedListIterator(WrappedList wrappedList, int i) {
                super(wrappedList, wrappedList.getListDelegate().listIterator(i));
                this.this$1 = wrappedList;
            }

            private ListIterator<V> getDelegateListIterator() {
                return (ListIterator) getDelegateIterator();
            }

            @Override // java.util.ListIterator
            public void add(@ParametricNullness V v) {
                boolean zIsEmpty = this.this$1.isEmpty();
                getDelegateListIterator().add(v);
                AbstractMapBasedMultimap.access$208(this.this$1.this$0);
                if (zIsEmpty) {
                    this.this$1.addToMap();
                }
            }

            @Override // java.util.ListIterator
            public boolean hasPrevious() {
                return getDelegateListIterator().hasPrevious();
            }

            @Override // java.util.ListIterator
            public int nextIndex() {
                return getDelegateListIterator().nextIndex();
            }

            @Override // java.util.ListIterator
            @ParametricNullness
            public V previous() {
                return getDelegateListIterator().previous();
            }

            @Override // java.util.ListIterator
            public int previousIndex() {
                return getDelegateListIterator().previousIndex();
            }

            @Override // java.util.ListIterator
            public void set(@ParametricNullness V v) {
                getDelegateListIterator().set(v);
            }
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        WrappedList(@ParametricNullness AbstractMapBasedMultimap abstractMapBasedMultimap, K k, @CheckForNull List<V> list, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
            super(abstractMapBasedMultimap, k, list, wrappedCollection);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override // java.util.List
        public void add(int i, @ParametricNullness V v) {
            refreshIfEmpty();
            boolean zIsEmpty = getDelegate().isEmpty();
            getListDelegate().add(i, v);
            AbstractMapBasedMultimap.access$208(this.this$0);
            if (zIsEmpty) {
                addToMap();
            }
        }

        @Override // java.util.List
        public boolean addAll(int i, Collection<? extends V> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int size = size();
            boolean zAddAll = getListDelegate().addAll(i, collection);
            if (!zAddAll) {
                return zAddAll;
            }
            AbstractMapBasedMultimap.access$212(this.this$0, getDelegate().size() - size);
            if (size != 0) {
                return zAddAll;
            }
            addToMap();
            return zAddAll;
        }

        @Override // java.util.List
        @ParametricNullness
        public V get(int i) {
            refreshIfEmpty();
            return getListDelegate().get(i);
        }

        List<V> getListDelegate() {
            return (List) getDelegate();
        }

        @Override // java.util.List
        public int indexOf(@CheckForNull Object obj) {
            refreshIfEmpty();
            return getListDelegate().indexOf(obj);
        }

        @Override // java.util.List
        public int lastIndexOf(@CheckForNull Object obj) {
            refreshIfEmpty();
            return getListDelegate().lastIndexOf(obj);
        }

        @Override // java.util.List
        public ListIterator<V> listIterator() {
            refreshIfEmpty();
            return new WrappedListIterator(this);
        }

        @Override // java.util.List
        public ListIterator<V> listIterator(int i) {
            refreshIfEmpty();
            return new WrappedListIterator(this, i);
        }

        @Override // java.util.List
        @ParametricNullness
        public V remove(int i) {
            refreshIfEmpty();
            V vRemove = getListDelegate().remove(i);
            AbstractMapBasedMultimap.access$210(this.this$0);
            removeIfEmpty();
            return vRemove;
        }

        @Override // java.util.List
        @ParametricNullness
        public V set(int i, @ParametricNullness V v) {
            refreshIfEmpty();
            return getListDelegate().set(i, v);
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.google.common.collect.AbstractMapBasedMultimap] */
        /* JADX WARN: Type inference failed for: r4v2, types: [com.google.common.collect.AbstractMapBasedMultimap$WrappedCollection] */
        /* JADX WARN: Type inference failed for: r4v3 */
        /* JADX WARN: Type inference failed for: r4v4 */
        /* JADX WARN: Type inference fix 'apply assigned field type' failed
        java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
        	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:596)
        	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
        	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
         */
        @Override // java.util.List
        public List<V> subList(int i, int i2) {
            refreshIfEmpty();
            ?? r0 = this.this$0;
            Object key = getKey();
            List<V> listSubList = getListDelegate().subList(i, i2);
            AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return r0.wrapList(key, listSubList, ancestor2);
        }
    }

    class WrappedNavigableSet extends AbstractMapBasedMultimap<K, V>.WrappedSortedSet implements NavigableSet<V> {
        final AbstractMapBasedMultimap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        WrappedNavigableSet(@ParametricNullness AbstractMapBasedMultimap abstractMapBasedMultimap, K k, @CheckForNull NavigableSet<V> navigableSet, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
            super(abstractMapBasedMultimap, k, navigableSet, wrappedCollection);
            this.this$0 = abstractMapBasedMultimap;
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r3v2, types: [com.google.common.collect.AbstractMapBasedMultimap$WrappedCollection] */
        /* JADX WARN: Type inference failed for: r3v3 */
        /* JADX WARN: Type inference failed for: r3v4 */
        private NavigableSet<V> wrap(NavigableSet<V> navigableSet) {
            AbstractMapBasedMultimap abstractMapBasedMultimap = this.this$0;
            K k = this.key;
            AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedNavigableSet(abstractMapBasedMultimap, k, navigableSet, ancestor2);
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public V ceiling(@ParametricNullness V v) {
            return getSortedSetDelegate().ceiling(v);
        }

        @Override // java.util.NavigableSet
        public Iterator<V> descendingIterator() {
            return new WrappedCollection.WrappedIterator(this, getSortedSetDelegate().descendingIterator());
        }

        @Override // java.util.NavigableSet
        public NavigableSet<V> descendingSet() {
            return wrap(getSortedSetDelegate().descendingSet());
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public V floor(@ParametricNullness V v) {
            return getSortedSetDelegate().floor(v);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.AbstractMapBasedMultimap.WrappedSortedSet
        public NavigableSet<V> getSortedSetDelegate() {
            return (NavigableSet) super.getSortedSetDelegate();
        }

        @Override // java.util.NavigableSet
        public NavigableSet<V> headSet(@ParametricNullness V v, boolean z) {
            return wrap(getSortedSetDelegate().headSet(v, z));
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public V higher(@ParametricNullness V v) {
            return getSortedSetDelegate().higher(v);
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public V lower(@ParametricNullness V v) {
            return getSortedSetDelegate().lower(v);
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public V pollFirst() {
            return (V) Iterators.pollNext(iterator());
        }

        @Override // java.util.NavigableSet
        @CheckForNull
        public V pollLast() {
            return (V) Iterators.pollNext(descendingIterator());
        }

        @Override // java.util.NavigableSet
        public NavigableSet<V> subSet(@ParametricNullness V v, boolean z, @ParametricNullness V v2, boolean z2) {
            return wrap(getSortedSetDelegate().subSet(v, z, v2, z2));
        }

        @Override // java.util.NavigableSet
        public NavigableSet<V> tailSet(@ParametricNullness V v, boolean z) {
            return wrap(getSortedSetDelegate().tailSet(v, z));
        }
    }

    class WrappedSet extends AbstractMapBasedMultimap<K, V>.WrappedCollection implements Set<V> {
        final AbstractMapBasedMultimap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        WrappedSet(@ParametricNullness AbstractMapBasedMultimap abstractMapBasedMultimap, K k, Set<V> set) {
            super(abstractMapBasedMultimap, k, set, null);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap.WrappedCollection, java.util.AbstractCollection, java.util.Collection
        public boolean removeAll(Collection<?> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int size = size();
            boolean zRemoveAllImpl = Sets.removeAllImpl((Set<?>) this.delegate, collection);
            if (!zRemoveAllImpl) {
                return zRemoveAllImpl;
            }
            AbstractMapBasedMultimap.access$212(this.this$0, this.delegate.size() - size);
            removeIfEmpty();
            return zRemoveAllImpl;
        }
    }

    class WrappedSortedSet extends AbstractMapBasedMultimap<K, V>.WrappedCollection implements SortedSet<V> {
        final AbstractMapBasedMultimap this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        WrappedSortedSet(@ParametricNullness AbstractMapBasedMultimap abstractMapBasedMultimap, K k, @CheckForNull SortedSet<V> sortedSet, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
            super(abstractMapBasedMultimap, k, sortedSet, wrappedCollection);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override // java.util.SortedSet
        @CheckForNull
        public Comparator<? super V> comparator() {
            return getSortedSetDelegate().comparator();
        }

        @Override // java.util.SortedSet
        @ParametricNullness
        public V first() {
            refreshIfEmpty();
            return getSortedSetDelegate().first();
        }

        SortedSet<V> getSortedSetDelegate() {
            return (SortedSet) getDelegate();
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r4v2, types: [com.google.common.collect.AbstractMapBasedMultimap$WrappedCollection] */
        /* JADX WARN: Type inference failed for: r4v3 */
        /* JADX WARN: Type inference failed for: r4v4 */
        @Override // java.util.SortedSet
        public SortedSet<V> headSet(@ParametricNullness V v) {
            refreshIfEmpty();
            AbstractMapBasedMultimap abstractMapBasedMultimap = this.this$0;
            Object key = getKey();
            SortedSet<V> sortedSetHeadSet = getSortedSetDelegate().headSet(v);
            AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedSortedSet(abstractMapBasedMultimap, key, sortedSetHeadSet, ancestor2);
        }

        @Override // java.util.SortedSet
        @ParametricNullness
        public V last() {
            refreshIfEmpty();
            return getSortedSetDelegate().last();
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r4v2, types: [com.google.common.collect.AbstractMapBasedMultimap$WrappedCollection] */
        /* JADX WARN: Type inference failed for: r4v3 */
        /* JADX WARN: Type inference failed for: r4v4 */
        @Override // java.util.SortedSet
        public SortedSet<V> subSet(@ParametricNullness V v, @ParametricNullness V v2) {
            refreshIfEmpty();
            AbstractMapBasedMultimap abstractMapBasedMultimap = this.this$0;
            Object key = getKey();
            SortedSet<V> sortedSetSubSet = getSortedSetDelegate().subSet(v, v2);
            AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedSortedSet(abstractMapBasedMultimap, key, sortedSetSubSet, ancestor2);
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r4v2, types: [com.google.common.collect.AbstractMapBasedMultimap$WrappedCollection] */
        /* JADX WARN: Type inference failed for: r4v3 */
        /* JADX WARN: Type inference failed for: r4v4 */
        @Override // java.util.SortedSet
        public SortedSet<V> tailSet(@ParametricNullness V v) {
            refreshIfEmpty();
            AbstractMapBasedMultimap abstractMapBasedMultimap = this.this$0;
            Object key = getKey();
            SortedSet<V> sortedSetTailSet = getSortedSetDelegate().tailSet(v);
            AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedSortedSet(abstractMapBasedMultimap, key, sortedSetTailSet, ancestor2);
        }
    }

    protected AbstractMapBasedMultimap(Map<K, Collection<V>> map) {
        Preconditions.checkArgument(map.isEmpty());
        this.map = map;
    }

    static /* synthetic */ int access$208(AbstractMapBasedMultimap abstractMapBasedMultimap) {
        int i = abstractMapBasedMultimap.totalSize;
        abstractMapBasedMultimap.totalSize = i + 1;
        return i;
    }

    static /* synthetic */ int access$210(AbstractMapBasedMultimap abstractMapBasedMultimap) {
        int i = abstractMapBasedMultimap.totalSize;
        abstractMapBasedMultimap.totalSize = i - 1;
        return i;
    }

    static /* synthetic */ int access$212(AbstractMapBasedMultimap abstractMapBasedMultimap, int i) {
        int i2 = abstractMapBasedMultimap.totalSize + i;
        abstractMapBasedMultimap.totalSize = i2;
        return i2;
    }

    static /* synthetic */ int access$220(AbstractMapBasedMultimap abstractMapBasedMultimap, int i) {
        int i2 = abstractMapBasedMultimap.totalSize - i;
        abstractMapBasedMultimap.totalSize = i2;
        return i2;
    }

    private Collection<V> getOrCreateCollection(@ParametricNullness K k) {
        Collection<V> collection = this.map.get(k);
        if (collection != null) {
            return collection;
        }
        Collection<V> collectionCreateCollection = createCollection(k);
        this.map.put(k, collectionCreateCollection);
        return collectionCreateCollection;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <E> Iterator<E> iteratorOrListIterator(Collection<E> collection) {
        return collection instanceof List ? ((List) collection).listIterator() : collection.iterator();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeValuesForKey(@CheckForNull Object obj) {
        Collection collection = (Collection) Maps.safeRemove(this.map, obj);
        if (collection != null) {
            int size = collection.size();
            collection.clear();
            this.totalSize -= size;
        }
    }

    Map<K, Collection<V>> backingMap() {
        return this.map;
    }

    @Override // com.google.common.collect.Multimap
    public void clear() {
        Iterator<Collection<V>> it = this.map.values().iterator();
        while (it.hasNext()) {
            it.next().clear();
        }
        this.map.clear();
        this.totalSize = 0;
    }

    @Override // com.google.common.collect.Multimap
    public boolean containsKey(@CheckForNull Object obj) {
        return this.map.containsKey(obj);
    }

    @Override // com.google.common.collect.AbstractMultimap
    Map<K, Collection<V>> createAsMap() {
        return new AsMap(this, this.map);
    }

    abstract Collection<V> createCollection();

    Collection<V> createCollection(@ParametricNullness K k) {
        return createCollection();
    }

    @Override // com.google.common.collect.AbstractMultimap
    Collection<Map.Entry<K, V>> createEntries() {
        return this instanceof SetMultimap ? new AbstractMultimap.EntrySet(this) : new AbstractMultimap.Entries(this);
    }

    @Override // com.google.common.collect.AbstractMultimap
    Set<K> createKeySet() {
        return new KeySet(this, this.map);
    }

    @Override // com.google.common.collect.AbstractMultimap
    Multiset<K> createKeys() {
        return new Multimaps.Keys(this);
    }

    final Map<K, Collection<V>> createMaybeNavigableAsMap() {
        if (this.map instanceof NavigableMap) {
            return new NavigableAsMap(this, (NavigableMap) this.map);
        }
        return this.map instanceof SortedMap ? new SortedAsMap(this, (SortedMap) this.map) : new AsMap(this, this.map);
    }

    final Set<K> createMaybeNavigableKeySet() {
        if (this.map instanceof NavigableMap) {
            return new NavigableKeySet(this, (NavigableMap) this.map);
        }
        return this.map instanceof SortedMap ? new SortedKeySet(this, (SortedMap) this.map) : new KeySet(this, this.map);
    }

    Collection<V> createUnmodifiableEmptyCollection() {
        return (Collection<V>) unmodifiableCollectionSubclass(createCollection());
    }

    @Override // com.google.common.collect.AbstractMultimap
    Collection<V> createValues() {
        return new AbstractMultimap.Values(this);
    }

    @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
    public Collection<Map.Entry<K, V>> entries() {
        return super.entries();
    }

    @Override // com.google.common.collect.AbstractMultimap
    Iterator<Map.Entry<K, V>> entryIterator() {
        return new AbstractMapBasedMultimap<K, V>.Itr<Map.Entry<K, V>>(this) { // from class: com.google.common.collect.AbstractMapBasedMultimap.2
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.google.common.collect.AbstractMapBasedMultimap.Itr
            public Map.Entry<K, V> output(@ParametricNullness K k, @ParametricNullness V v) {
                return Maps.immutableEntry(k, v);
            }
        };
    }

    @Override // com.google.common.collect.Multimap
    public Collection<V> get(@ParametricNullness K k) {
        Collection<V> collectionCreateCollection = this.map.get(k);
        if (collectionCreateCollection == null) {
            collectionCreateCollection = createCollection(k);
        }
        return wrapCollection(k, collectionCreateCollection);
    }

    @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
    public boolean put(@ParametricNullness K k, @ParametricNullness V v) {
        Collection<V> collection = this.map.get(k);
        if (collection != null) {
            if (!collection.add(v)) {
                return false;
            }
            this.totalSize++;
            return true;
        }
        Collection<V> collectionCreateCollection = createCollection(k);
        if (!collectionCreateCollection.add(v)) {
            throw new AssertionError("New Collection violated the Collection spec");
        }
        this.totalSize++;
        this.map.put(k, collectionCreateCollection);
        return true;
    }

    @Override // com.google.common.collect.Multimap
    public Collection<V> removeAll(@CheckForNull Object obj) {
        Collection<V> collectionRemove = this.map.remove(obj);
        if (collectionRemove == null) {
            return createUnmodifiableEmptyCollection();
        }
        Collection collectionCreateCollection = createCollection();
        collectionCreateCollection.addAll(collectionRemove);
        this.totalSize -= collectionRemove.size();
        collectionRemove.clear();
        return (Collection<V>) unmodifiableCollectionSubclass(collectionCreateCollection);
    }

    @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
    public Collection<V> replaceValues(@ParametricNullness K k, Iterable<? extends V> iterable) {
        Iterator<? extends V> it = iterable.iterator();
        if (!it.hasNext()) {
            return removeAll(k);
        }
        Collection<V> orCreateCollection = getOrCreateCollection(k);
        Collection<V> collectionCreateCollection = createCollection();
        collectionCreateCollection.addAll(orCreateCollection);
        this.totalSize -= orCreateCollection.size();
        orCreateCollection.clear();
        while (it.hasNext()) {
            if (orCreateCollection.add(it.next())) {
                this.totalSize++;
            }
        }
        return (Collection<V>) unmodifiableCollectionSubclass(collectionCreateCollection);
    }

    final void setMap(Map<K, Collection<V>> map) {
        this.map = map;
        this.totalSize = 0;
        for (Collection<V> collection : map.values()) {
            Preconditions.checkArgument(!collection.isEmpty());
            this.totalSize = collection.size() + this.totalSize;
        }
    }

    @Override // com.google.common.collect.Multimap
    public int size() {
        return this.totalSize;
    }

    <E> Collection<E> unmodifiableCollectionSubclass(Collection<E> collection) {
        return Collections.unmodifiableCollection(collection);
    }

    @Override // com.google.common.collect.AbstractMultimap
    Iterator<V> valueIterator() {
        return new AbstractMapBasedMultimap<K, V>.Itr<V>(this) { // from class: com.google.common.collect.AbstractMapBasedMultimap.1
            @Override // com.google.common.collect.AbstractMapBasedMultimap.Itr
            @ParametricNullness
            V output(@ParametricNullness K k, @ParametricNullness V v) {
                return v;
            }
        };
    }

    @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
    public Collection<V> values() {
        return super.values();
    }

    Collection<V> wrapCollection(@ParametricNullness K k, Collection<V> collection) {
        return new WrappedCollection(this, k, collection, null);
    }

    final List<V> wrapList(@ParametricNullness K k, List<V> list, @CheckForNull AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
        return list instanceof RandomAccess ? new RandomAccessWrappedList(this, k, list, wrappedCollection) : new WrappedList(this, k, list, wrappedCollection);
    }
}
