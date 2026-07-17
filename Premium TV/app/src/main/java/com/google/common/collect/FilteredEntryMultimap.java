package com.google.common.collect;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
class FilteredEntryMultimap<K, V> extends AbstractMultimap<K, V> implements FilteredMultimap<K, V> {
    final Predicate<? super Map.Entry<K, V>> predicate;
    final Multimap<K, V> unfiltered;

    class AsMap extends Maps.ViewCachingAbstractMap<K, Collection<V>> {
        final FilteredEntryMultimap this$0;

        /* JADX INFO: renamed from: com.google.common.collect.FilteredEntryMultimap$AsMap$1EntrySetImpl, reason: invalid class name */
        class C1EntrySetImpl extends Maps.EntrySet<K, Collection<V>> {
            final AsMap this$1;

            C1EntrySetImpl(AsMap asMap) {
                this.this$1 = asMap;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return new AbstractIterator<Map.Entry<K, Collection<V>>>(this) { // from class: com.google.common.collect.FilteredEntryMultimap.AsMap.1EntrySetImpl.1
                    final Iterator<Map.Entry<K, Collection<V>>> backingIterator;
                    final C1EntrySetImpl this$2;

                    {
                        this.this$2 = this;
                        this.backingIterator = this.this$2.this$1.this$0.unfiltered.asMap().entrySet().iterator();
                    }

                    /* JADX INFO: Access modifiers changed from: protected */
                    @Override // com.google.common.collect.AbstractIterator
                    @CheckForNull
                    public Map.Entry<K, Collection<V>> computeNext() {
                        while (this.backingIterator.hasNext()) {
                            Map.Entry<K, Collection<V>> next = this.backingIterator.next();
                            K key = next.getKey();
                            Collection collectionFilterCollection = FilteredEntryMultimap.filterCollection(next.getValue(), new ValuePredicate(this.this$2.this$1.this$0, key));
                            if (!collectionFilterCollection.isEmpty()) {
                                return Maps.immutableEntry(key, collectionFilterCollection);
                            }
                        }
                        return endOfData();
                    }
                };
            }

            @Override // com.google.common.collect.Maps.EntrySet
            Map<K, Collection<V>> map() {
                return this.this$1;
            }

            @Override // com.google.common.collect.Maps.EntrySet, com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean removeAll(Collection<?> collection) {
                return this.this$1.this$0.removeEntriesIf(Predicates.in(collection));
            }

            @Override // com.google.common.collect.Maps.EntrySet, com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean retainAll(Collection<?> collection) {
                return this.this$1.this$0.removeEntriesIf(Predicates.not(Predicates.in(collection)));
            }

            @Override // com.google.common.collect.Maps.EntrySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return Iterators.size(iterator());
            }
        }

        AsMap(FilteredEntryMultimap filteredEntryMultimap) {
            this.this$0 = filteredEntryMultimap;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public void clear() {
            this.this$0.clear();
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            return get(obj) != null;
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        Set<Map.Entry<K, Collection<V>>> createEntrySet() {
            return new C1EntrySetImpl(this);
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        Set<K> createKeySet() {
            return new Maps.KeySet<K, Collection<V>>(this) { // from class: com.google.common.collect.FilteredEntryMultimap.AsMap.1KeySetImpl
                final AsMap this$1;

                {
                    this.this$1 = this;
                }

                @Override // com.google.common.collect.Maps.KeySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
                public boolean remove(@CheckForNull Object obj) {
                    return this.this$1.remove(obj) != null;
                }

                @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
                public boolean removeAll(Collection<?> collection) {
                    return this.this$1.this$0.removeEntriesIf(Maps.keyPredicateOnEntries(Predicates.in(collection)));
                }

                @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
                public boolean retainAll(Collection<?> collection) {
                    return this.this$1.this$0.removeEntriesIf(Maps.keyPredicateOnEntries(Predicates.not(Predicates.in(collection))));
                }
            };
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        Collection<Collection<V>> createValues() {
            return new Maps.Values<K, Collection<V>>(this) { // from class: com.google.common.collect.FilteredEntryMultimap.AsMap.1ValuesImpl
                final AsMap this$1;

                {
                    this.this$1 = this;
                }

                @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
                public boolean remove(@CheckForNull Object obj) {
                    if (obj instanceof Collection) {
                        Collection collection = (Collection) obj;
                        Iterator<Map.Entry<K, Collection<V>>> it = this.this$1.this$0.unfiltered.asMap().entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<K, Collection<V>> next = it.next();
                            Collection collectionFilterCollection = FilteredEntryMultimap.filterCollection(next.getValue(), new ValuePredicate(this.this$1.this$0, next.getKey()));
                            if (!collectionFilterCollection.isEmpty() && collection.equals(collectionFilterCollection)) {
                                if (collectionFilterCollection.size() == next.getValue().size()) {
                                    it.remove();
                                } else {
                                    collectionFilterCollection.clear();
                                }
                                return true;
                            }
                        }
                    }
                    return false;
                }

                @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
                public boolean removeAll(Collection<?> collection) {
                    return this.this$1.this$0.removeEntriesIf(Maps.valuePredicateOnEntries(Predicates.in(collection)));
                }

                @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
                public boolean retainAll(Collection<?> collection) {
                    return this.this$1.this$0.removeEntriesIf(Maps.valuePredicateOnEntries(Predicates.not(Predicates.in(collection))));
                }
            };
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Collection<V> get(@CheckForNull Object obj) {
            Collection<V> collection = this.this$0.unfiltered.asMap().get(obj);
            if (collection == null) {
                return null;
            }
            Collection<V> collectionFilterCollection = FilteredEntryMultimap.filterCollection(collection, new ValuePredicate(this.this$0, obj));
            if (collectionFilterCollection.isEmpty()) {
                collectionFilterCollection = null;
            }
            return collectionFilterCollection;
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Collection<V> remove(@CheckForNull Object obj) {
            Collection<V> collection = this.this$0.unfiltered.asMap().get(obj);
            if (collection == null) {
                return null;
            }
            ArrayList arrayListNewArrayList = Lists.newArrayList();
            Iterator<V> it = collection.iterator();
            while (it.hasNext()) {
                V next = it.next();
                if (this.this$0.satisfies(obj, next)) {
                    it.remove();
                    arrayListNewArrayList.add(next);
                }
            }
            if (arrayListNewArrayList.isEmpty()) {
                return null;
            }
            return this.this$0.unfiltered instanceof SetMultimap ? Collections.unmodifiableSet(Sets.newLinkedHashSet(arrayListNewArrayList)) : Collections.unmodifiableList(arrayListNewArrayList);
        }
    }

    class Keys extends Multimaps.Keys<K, V> {
        final FilteredEntryMultimap this$0;

        /* JADX INFO: renamed from: com.google.common.collect.FilteredEntryMultimap$Keys$1, reason: invalid class name */
        class AnonymousClass1 extends Multisets.EntrySet<K> {
            final Keys this$1;

            AnonymousClass1(Keys keys) {
                this.this$1 = keys;
            }

            private boolean removeEntriesIf(final Predicate<? super Multiset.Entry<K>> predicate) {
                return this.this$1.this$0.removeEntriesIf(new Predicate(predicate) { // from class: com.google.common.collect.FilteredEntryMultimap$Keys$1$$ExternalSyntheticLambda0
                    public final Predicate f$0;

                    {
                        this.f$0 = predicate;
                    }

                    @Override // com.google.common.base.Predicate
                    public final boolean apply(Object obj) {
                        Map.Entry entry = (Map.Entry) obj;
                        return this.f$0.apply(Multisets.immutableEntry(entry.getKey(), ((Collection) entry.getValue()).size()));
                    }
                });
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<Multiset.Entry<K>> iterator() {
                return this.this$1.entryIterator();
            }

            @Override // com.google.common.collect.Multisets.EntrySet
            Multiset<K> multiset() {
                return this.this$1;
            }

            @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean removeAll(Collection<?> collection) {
                return removeEntriesIf(Predicates.in(collection));
            }

            @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean retainAll(Collection<?> collection) {
                return removeEntriesIf(Predicates.not(Predicates.in(collection)));
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return this.this$1.this$0.keySet().size();
            }
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        Keys(FilteredEntryMultimap filteredEntryMultimap) {
            super(filteredEntryMultimap);
            this.this$0 = filteredEntryMultimap;
        }

        @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        public Set<Multiset.Entry<K>> entrySet() {
            return new AnonymousClass1(this);
        }

        @Override // com.google.common.collect.Multimaps.Keys, com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        public int remove(@CheckForNull Object obj, int i) {
            CollectPreconditions.checkNonnegative(i, "occurrences");
            if (i == 0) {
                return count(obj);
            }
            Collection<V> collection = this.this$0.unfiltered.asMap().get(obj);
            if (collection == null) {
                return 0;
            }
            Iterator<V> it = collection.iterator();
            int i2 = 0;
            while (it.hasNext()) {
                if (this.this$0.satisfies(obj, it.next()) && (i2 = i2 + 1) <= i) {
                    it.remove();
                }
            }
            return i2;
        }
    }

    final class ValuePredicate implements Predicate<V> {

        @ParametricNullness
        private final K key;
        final FilteredEntryMultimap this$0;

        ValuePredicate(@ParametricNullness FilteredEntryMultimap filteredEntryMultimap, K k) {
            this.this$0 = filteredEntryMultimap;
            this.key = k;
        }

        @Override // com.google.common.base.Predicate
        public boolean apply(@ParametricNullness V v) {
            return this.this$0.satisfies(this.key, v);
        }
    }

    FilteredEntryMultimap(Multimap<K, V> multimap, Predicate<? super Map.Entry<K, V>> predicate) {
        this.unfiltered = (Multimap) Preconditions.checkNotNull(multimap);
        this.predicate = (Predicate) Preconditions.checkNotNull(predicate);
    }

    static <E> Collection<E> filterCollection(Collection<E> collection, Predicate<? super E> predicate) {
        return collection instanceof Set ? Sets.filter((Set) collection, predicate) : Collections2.filter(collection, predicate);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean satisfies(@ParametricNullness K k, @ParametricNullness V v) {
        return this.predicate.apply(Maps.immutableEntry(k, v));
    }

    @Override // com.google.common.collect.Multimap
    public void clear() {
        entries().clear();
    }

    @Override // com.google.common.collect.Multimap
    public boolean containsKey(@CheckForNull Object obj) {
        return asMap().get(obj) != null;
    }

    @Override // com.google.common.collect.AbstractMultimap
    Map<K, Collection<V>> createAsMap() {
        return new AsMap(this);
    }

    @Override // com.google.common.collect.AbstractMultimap
    Collection<Map.Entry<K, V>> createEntries() {
        return filterCollection(this.unfiltered.entries(), this.predicate);
    }

    @Override // com.google.common.collect.AbstractMultimap
    Set<K> createKeySet() {
        return asMap().keySet();
    }

    @Override // com.google.common.collect.AbstractMultimap
    Multiset<K> createKeys() {
        return new Keys(this);
    }

    @Override // com.google.common.collect.AbstractMultimap
    Collection<V> createValues() {
        return new FilteredMultimapValues(this);
    }

    @Override // com.google.common.collect.AbstractMultimap
    Iterator<Map.Entry<K, V>> entryIterator() {
        throw new AssertionError("should never be called");
    }

    @Override // com.google.common.collect.FilteredMultimap
    public Predicate<? super Map.Entry<K, V>> entryPredicate() {
        return this.predicate;
    }

    @Override // com.google.common.collect.Multimap
    public Collection<V> get(@ParametricNullness K k) {
        return filterCollection(this.unfiltered.get(k), new ValuePredicate(this, k));
    }

    @Override // com.google.common.collect.Multimap
    public Collection<V> removeAll(@CheckForNull Object obj) {
        return (Collection) MoreObjects.firstNonNull(asMap().remove(obj), unmodifiableEmptyCollection());
    }

    boolean removeEntriesIf(Predicate<? super Map.Entry<K, Collection<V>>> predicate) {
        Iterator<Map.Entry<K, Collection<V>>> it = this.unfiltered.asMap().entrySet().iterator();
        boolean z = false;
        while (true) {
            boolean z2 = z;
            if (!it.hasNext()) {
                return z2;
            }
            Map.Entry<K, Collection<V>> next = it.next();
            K key = next.getKey();
            Collection collectionFilterCollection = filterCollection(next.getValue(), new ValuePredicate(this, key));
            if (!collectionFilterCollection.isEmpty() && predicate.apply(Maps.immutableEntry(key, collectionFilterCollection))) {
                if (collectionFilterCollection.size() == next.getValue().size()) {
                    it.remove();
                } else {
                    collectionFilterCollection.clear();
                }
                z2 = true;
            }
            z = z2;
        }
    }

    @Override // com.google.common.collect.Multimap
    public int size() {
        return entries().size();
    }

    @Override // com.google.common.collect.FilteredMultimap
    public Multimap<K, V> unfiltered() {
        return this.unfiltered;
    }

    Collection<V> unmodifiableEmptyCollection() {
        return this.unfiltered instanceof SetMultimap ? Collections.emptySet() : Collections.emptyList();
    }
}
