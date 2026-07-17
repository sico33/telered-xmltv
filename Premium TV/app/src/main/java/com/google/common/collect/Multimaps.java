package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collector;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Multimaps {

    static final class AsMap<K, V> extends Maps.ViewCachingAbstractMap<K, Collection<V>> {
        private final Multimap<K, V> multimap;

        class EntrySet extends Maps.EntrySet<K, Collection<V>> {
            final AsMap this$0;

            EntrySet(AsMap asMap) {
                this.this$0 = asMap;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return Maps.asMapEntryIterator(this.this$0.multimap.keySet(), new Function(this) { // from class: com.google.common.collect.Multimaps$AsMap$EntrySet$$ExternalSyntheticLambda0
                    public final Multimaps.AsMap.EntrySet f$0;

                    {
                        this.f$0 = this;
                    }

                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return this.f$0.m186xda360ac8(obj);
                    }
                });
            }

            /* JADX INFO: renamed from: lambda$iterator$0$com-google-common-collect-Multimaps$AsMap$EntrySet, reason: not valid java name */
            /* synthetic */ Collection m186xda360ac8(Object obj) {
                return this.this$0.multimap.get(obj);
            }

            @Override // com.google.common.collect.Maps.EntrySet
            Map<K, Collection<V>> map() {
                return this.this$0;
            }

            @Override // com.google.common.collect.Maps.EntrySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean remove(@CheckForNull Object obj) {
                if (!contains(obj)) {
                    return false;
                }
                this.this$0.removeValuesForKey(((Map.Entry) Objects.requireNonNull((Map.Entry) obj)).getKey());
                return true;
            }
        }

        AsMap(Multimap<K, V> multimap) {
            this.multimap = (Multimap) Preconditions.checkNotNull(multimap);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public void clear() {
            this.multimap.clear();
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            return this.multimap.containsKey(obj);
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap
        protected Set<Map.Entry<K, Collection<V>>> createEntrySet() {
            return new EntrySet(this);
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Collection<V> get(@CheckForNull Object obj) {
            if (containsKey(obj)) {
                return this.multimap.get(obj);
            }
            return null;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean isEmpty() {
            return this.multimap.isEmpty();
        }

        @Override // com.google.common.collect.Maps.ViewCachingAbstractMap, java.util.AbstractMap, java.util.Map
        public Set<K> keySet() {
            return this.multimap.keySet();
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public Collection<V> remove(@CheckForNull Object obj) {
            if (containsKey(obj)) {
                return this.multimap.removeAll(obj);
            }
            return null;
        }

        void removeValuesForKey(@CheckForNull Object obj) {
            this.multimap.keySet().remove(obj);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public int size() {
            return this.multimap.keySet().size();
        }
    }

    private static class CustomListMultimap<K, V> extends AbstractListMultimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Supplier<? extends List<V>> factory;

        CustomListMultimap(Map<K, Collection<V>> map, Supplier<? extends List<V>> supplier) {
            super(map);
            this.factory = (Supplier) Preconditions.checkNotNull(supplier);
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.factory = (Supplier) Objects.requireNonNull(objectInputStream.readObject());
            setMap((Map) Objects.requireNonNull(objectInputStream.readObject()));
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(this.factory);
            objectOutputStream.writeObject(backingMap());
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap
        Map<K, Collection<V>> createAsMap() {
            return createMaybeNavigableAsMap();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.AbstractListMultimap, com.google.common.collect.AbstractMapBasedMultimap
        public List<V> createCollection() {
            return this.factory.get();
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap
        Set<K> createKeySet() {
            return createMaybeNavigableKeySet();
        }
    }

    private static class CustomMultimap<K, V> extends AbstractMapBasedMultimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Supplier<? extends Collection<V>> factory;

        CustomMultimap(Map<K, Collection<V>> map, Supplier<? extends Collection<V>> supplier) {
            super(map);
            this.factory = (Supplier) Preconditions.checkNotNull(supplier);
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.factory = (Supplier) Objects.requireNonNull(objectInputStream.readObject());
            setMap((Map) Objects.requireNonNull(objectInputStream.readObject()));
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(this.factory);
            objectOutputStream.writeObject(backingMap());
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap
        Map<K, Collection<V>> createAsMap() {
            return createMaybeNavigableAsMap();
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap
        protected Collection<V> createCollection() {
            return this.factory.get();
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap
        Set<K> createKeySet() {
            return createMaybeNavigableKeySet();
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap
        <E> Collection<E> unmodifiableCollectionSubclass(Collection<E> collection) {
            if (collection instanceof NavigableSet) {
                return Sets.unmodifiableNavigableSet((NavigableSet) collection);
            }
            if (collection instanceof SortedSet) {
                return Collections.unmodifiableSortedSet((SortedSet) collection);
            }
            if (collection instanceof Set) {
                return Collections.unmodifiableSet((Set) collection);
            }
            return collection instanceof List ? Collections.unmodifiableList((List) collection) : Collections.unmodifiableCollection(collection);
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap
        Collection<V> wrapCollection(@ParametricNullness K k, Collection<V> collection) {
            if (collection instanceof List) {
                return wrapList(k, (List) collection, null);
            }
            if (collection instanceof NavigableSet) {
                return new AbstractMapBasedMultimap.WrappedNavigableSet(this, k, (NavigableSet) collection, null);
            }
            if (collection instanceof SortedSet) {
                return new AbstractMapBasedMultimap.WrappedSortedSet(this, k, (SortedSet) collection, null);
            }
            return collection instanceof Set ? new AbstractMapBasedMultimap.WrappedSet(this, k, (Set) collection) : new AbstractMapBasedMultimap.WrappedCollection(this, k, collection, null);
        }
    }

    private static class CustomSetMultimap<K, V> extends AbstractSetMultimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Supplier<? extends Set<V>> factory;

        CustomSetMultimap(Map<K, Collection<V>> map, Supplier<? extends Set<V>> supplier) {
            super(map);
            this.factory = (Supplier) Preconditions.checkNotNull(supplier);
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.factory = (Supplier) Objects.requireNonNull(objectInputStream.readObject());
            setMap((Map) Objects.requireNonNull(objectInputStream.readObject()));
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(this.factory);
            objectOutputStream.writeObject(backingMap());
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap
        Map<K, Collection<V>> createAsMap() {
            return createMaybeNavigableAsMap();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.AbstractSetMultimap, com.google.common.collect.AbstractMapBasedMultimap
        public Set<V> createCollection() {
            return this.factory.get();
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap
        Set<K> createKeySet() {
            return createMaybeNavigableKeySet();
        }

        @Override // com.google.common.collect.AbstractSetMultimap, com.google.common.collect.AbstractMapBasedMultimap
        <E> Collection<E> unmodifiableCollectionSubclass(Collection<E> collection) {
            if (collection instanceof NavigableSet) {
                return Sets.unmodifiableNavigableSet((NavigableSet) collection);
            }
            return collection instanceof SortedSet ? Collections.unmodifiableSortedSet((SortedSet) collection) : Collections.unmodifiableSet((Set) collection);
        }

        @Override // com.google.common.collect.AbstractSetMultimap, com.google.common.collect.AbstractMapBasedMultimap
        Collection<V> wrapCollection(@ParametricNullness K k, Collection<V> collection) {
            if (collection instanceof NavigableSet) {
                return new AbstractMapBasedMultimap.WrappedNavigableSet(this, k, (NavigableSet) collection, null);
            }
            return collection instanceof SortedSet ? new AbstractMapBasedMultimap.WrappedSortedSet(this, k, (SortedSet) collection, null) : new AbstractMapBasedMultimap.WrappedSet(this, k, (Set) collection);
        }
    }

    private static class CustomSortedSetMultimap<K, V> extends AbstractSortedSetMultimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Supplier<? extends SortedSet<V>> factory;

        @CheckForNull
        transient Comparator<? super V> valueComparator;

        CustomSortedSetMultimap(Map<K, Collection<V>> map, Supplier<? extends SortedSet<V>> supplier) {
            super(map);
            this.factory = (Supplier) Preconditions.checkNotNull(supplier);
            this.valueComparator = supplier.get().comparator();
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.factory = (Supplier) Objects.requireNonNull(objectInputStream.readObject());
            this.valueComparator = this.factory.get().comparator();
            setMap((Map) Objects.requireNonNull(objectInputStream.readObject()));
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(this.factory);
            objectOutputStream.writeObject(backingMap());
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap
        Map<K, Collection<V>> createAsMap() {
            return createMaybeNavigableAsMap();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.AbstractSortedSetMultimap, com.google.common.collect.AbstractSetMultimap, com.google.common.collect.AbstractMapBasedMultimap
        public SortedSet<V> createCollection() {
            return this.factory.get();
        }

        @Override // com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap
        Set<K> createKeySet() {
            return createMaybeNavigableKeySet();
        }

        @Override // com.google.common.collect.SortedSetMultimap
        @CheckForNull
        public Comparator<? super V> valueComparator() {
            return this.valueComparator;
        }
    }

    static abstract class Entries<K, V> extends AbstractCollection<Map.Entry<K, V>> {
        Entries() {
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public void clear() {
            multimap().clear();
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean contains(@CheckForNull Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return multimap().containsEntry(entry.getKey(), entry.getValue());
        }

        abstract Multimap<K, V> multimap();

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean remove(@CheckForNull Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return multimap().remove(entry.getKey(), entry.getValue());
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public int size() {
            return multimap().size();
        }
    }

    static class Keys<K, V> extends AbstractMultiset<K> {
        final Multimap<K, V> multimap;

        Keys(Multimap<K, V> multimap) {
            this.multimap = multimap;
        }

        @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
        public void clear() {
            this.multimap.clear();
        }

        @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
        public boolean contains(@CheckForNull Object obj) {
            return this.multimap.containsKey(obj);
        }

        @Override // com.google.common.collect.Multiset
        public int count(@CheckForNull Object obj) {
            Collection collection = (Collection) Maps.safeGet(this.multimap.asMap(), obj);
            if (collection == null) {
                return 0;
            }
            return collection.size();
        }

        @Override // com.google.common.collect.AbstractMultiset
        int distinctElements() {
            return this.multimap.asMap().size();
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<K> elementIterator() {
            throw new AssertionError("should never be called");
        }

        @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        public Set<K> elementSet() {
            return this.multimap.keySet();
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<Multiset.Entry<K>> entryIterator() {
            return new TransformedIterator<Map.Entry<K, Collection<V>>, Multiset.Entry<K>>(this, this.multimap.asMap().entrySet().iterator()) { // from class: com.google.common.collect.Multimaps.Keys.1
                /* JADX INFO: Access modifiers changed from: package-private */
                @Override // com.google.common.collect.TransformedIterator
                public Multiset.Entry<K> transform(Map.Entry<K, Collection<V>> entry) {
                    return new Multisets.AbstractEntry<K>(this, entry) { // from class: com.google.common.collect.Multimaps.Keys.1.1
                        final Map.Entry val$backingEntry;

                        {
                            this.val$backingEntry = entry;
                        }

                        @Override // com.google.common.collect.Multiset.Entry
                        public int getCount() {
                            return ((Collection) this.val$backingEntry.getValue()).size();
                        }

                        @Override // com.google.common.collect.Multiset.Entry
                        @ParametricNullness
                        public K getElement() {
                            return (K) this.val$backingEntry.getKey();
                        }
                    };
                }
            };
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, com.google.common.collect.Multiset
        public Iterator<K> iterator() {
            return Maps.keyIterator(this.multimap.entries().iterator());
        }

        @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        public int remove(@CheckForNull Object obj, int i) {
            CollectPreconditions.checkNonnegative(i, "occurrences");
            if (i == 0) {
                return count(obj);
            }
            Collection collection = (Collection) Maps.safeGet(this.multimap.asMap(), obj);
            if (collection == null) {
                return 0;
            }
            int size = collection.size();
            if (i >= size) {
                collection.clear();
            } else {
                Iterator it = collection.iterator();
                for (int i2 = 0; i2 < i; i2++) {
                    it.next();
                    it.remove();
                }
            }
            return size;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
        public int size() {
            return this.multimap.size();
        }
    }

    private static class MapMultimap<K, V> extends AbstractMultimap<K, V> implements SetMultimap<K, V>, Serializable {
        private static final long serialVersionUID = 7845222491160860175L;
        final Map<K, V> map;

        /* JADX INFO: renamed from: com.google.common.collect.Multimaps$MapMultimap$1, reason: invalid class name */
        class AnonymousClass1 extends Sets.ImprovedAbstractSet<V> {
            final MapMultimap this$0;
            final Object val$key;

            AnonymousClass1(MapMultimap mapMultimap, Object obj) {
                this.this$0 = mapMultimap;
                this.val$key = obj;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<V> iterator() {
                return new Iterator<V>(this) { // from class: com.google.common.collect.Multimaps.MapMultimap.1.1
                    int i;
                    final AnonymousClass1 this$1;

                    {
                        this.this$1 = this;
                    }

                    @Override // java.util.Iterator
                    public boolean hasNext() {
                        return this.i == 0 && this.this$1.this$0.map.containsKey(this.this$1.val$key);
                    }

                    @Override // java.util.Iterator
                    @ParametricNullness
                    public V next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        this.i++;
                        return (V) NullnessCasts.uncheckedCastNullableTToT(this.this$1.this$0.map.get(this.this$1.val$key));
                    }

                    @Override // java.util.Iterator
                    public void remove() {
                        CollectPreconditions.checkRemove(this.i == 1);
                        this.i = -1;
                        this.this$1.this$0.map.remove(this.this$1.val$key);
                    }
                };
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return this.this$0.map.containsKey(this.val$key) ? 1 : 0;
            }
        }

        MapMultimap(Map<K, V> map) {
            this.map = (Map) Preconditions.checkNotNull(map);
        }

        @Override // com.google.common.collect.Multimap
        public void clear() {
            this.map.clear();
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean containsEntry(@CheckForNull Object obj, @CheckForNull Object obj2) {
            return this.map.entrySet().contains(Maps.immutableEntry(obj, obj2));
        }

        @Override // com.google.common.collect.Multimap
        public boolean containsKey(@CheckForNull Object obj) {
            return this.map.containsKey(obj);
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean containsValue(@CheckForNull Object obj) {
            return this.map.containsValue(obj);
        }

        @Override // com.google.common.collect.AbstractMultimap
        Map<K, Collection<V>> createAsMap() {
            return new AsMap(this);
        }

        @Override // com.google.common.collect.AbstractMultimap
        Collection<Map.Entry<K, V>> createEntries() {
            throw new AssertionError("unreachable");
        }

        @Override // com.google.common.collect.AbstractMultimap
        Set<K> createKeySet() {
            return this.map.keySet();
        }

        @Override // com.google.common.collect.AbstractMultimap
        Multiset<K> createKeys() {
            return new Keys(this);
        }

        @Override // com.google.common.collect.AbstractMultimap
        Collection<V> createValues() {
            return this.map.values();
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public Set<Map.Entry<K, V>> entries() {
            return this.map.entrySet();
        }

        @Override // com.google.common.collect.AbstractMultimap
        Iterator<Map.Entry<K, V>> entryIterator() {
            return this.map.entrySet().iterator();
        }

        @Override // com.google.common.collect.Multimap
        public Set<V> get(@ParametricNullness K k) {
            return new AnonymousClass1(this, k);
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public int hashCode() {
            return this.map.hashCode();
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean put(@ParametricNullness K k, @ParametricNullness V v) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean putAll(@ParametricNullness K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean remove(@CheckForNull Object obj, @CheckForNull Object obj2) {
            return this.map.entrySet().remove(Maps.immutableEntry(obj, obj2));
        }

        @Override // com.google.common.collect.Multimap
        public Set<V> removeAll(@CheckForNull Object obj) {
            HashSet hashSet = new HashSet(2);
            if (this.map.containsKey(obj)) {
                hashSet.add(this.map.remove(obj));
            }
            return hashSet;
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public Set<V> replaceValues(@ParametricNullness K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.Multimap
        public int size() {
            return this.map.size();
        }
    }

    private static final class TransformedEntriesListMultimap<K, V1, V2> extends TransformedEntriesMultimap<K, V1, V2> implements ListMultimap<K, V2> {
        TransformedEntriesListMultimap(ListMultimap<K, V1> listMultimap, Maps.EntryTransformer<? super K, ? super V1, V2> entryTransformer) {
            super(listMultimap, entryTransformer);
        }

        @Override // com.google.common.collect.Multimaps.TransformedEntriesMultimap, com.google.common.collect.Multimap
        public List<V2> get(@ParametricNullness K k) {
            return m187x893217c0((Object) k, (Collection) this.fromMultimap.get(k));
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.google.common.collect.Multimaps.TransformedEntriesMultimap, com.google.common.collect.Multimap
        public List<V2> removeAll(@CheckForNull Object obj) {
            return m187x893217c0(obj, (Collection) this.fromMultimap.removeAll(obj));
        }

        @Override // com.google.common.collect.Multimaps.TransformedEntriesMultimap, com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public List<V2> replaceValues(@ParametricNullness K k, Iterable<? extends V2> iterable) {
            throw new UnsupportedOperationException();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.google.common.collect.Multimaps.TransformedEntriesMultimap
        /* JADX INFO: renamed from: transform */
        public List<V2> m187x893217c0(@ParametricNullness K k, Collection<V1> collection) {
            return Lists.transform((List) collection, Maps.asValueToValueFunction(this.transformer, k));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class TransformedEntriesMultimap<K, V1, V2> extends AbstractMultimap<K, V2> {
        final Multimap<K, V1> fromMultimap;
        final Maps.EntryTransformer<? super K, ? super V1, V2> transformer;

        TransformedEntriesMultimap(Multimap<K, V1> multimap, Maps.EntryTransformer<? super K, ? super V1, V2> entryTransformer) {
            this.fromMultimap = (Multimap) Preconditions.checkNotNull(multimap);
            this.transformer = (Maps.EntryTransformer) Preconditions.checkNotNull(entryTransformer);
        }

        @Override // com.google.common.collect.Multimap
        public void clear() {
            this.fromMultimap.clear();
        }

        @Override // com.google.common.collect.Multimap
        public boolean containsKey(@CheckForNull Object obj) {
            return this.fromMultimap.containsKey(obj);
        }

        @Override // com.google.common.collect.AbstractMultimap
        Map<K, Collection<V2>> createAsMap() {
            return Maps.transformEntries(this.fromMultimap.asMap(), new Maps.EntryTransformer(this) { // from class: com.google.common.collect.Multimaps$TransformedEntriesMultimap$$ExternalSyntheticLambda0
                public final Multimaps.TransformedEntriesMultimap f$0;

                {
                    this.f$0 = this;
                }

                @Override // com.google.common.collect.Maps.EntryTransformer
                public final Object transformEntry(Object obj, Object obj2) {
                    return this.f$0.m187x893217c0(obj, (Collection) obj2);
                }
            });
        }

        @Override // com.google.common.collect.AbstractMultimap
        Collection<Map.Entry<K, V2>> createEntries() {
            return new AbstractMultimap.Entries(this);
        }

        @Override // com.google.common.collect.AbstractMultimap
        Set<K> createKeySet() {
            return this.fromMultimap.keySet();
        }

        @Override // com.google.common.collect.AbstractMultimap
        Multiset<K> createKeys() {
            return this.fromMultimap.keys();
        }

        @Override // com.google.common.collect.AbstractMultimap
        Collection<V2> createValues() {
            return Collections2.transform(this.fromMultimap.entries(), Maps.asEntryToValueFunction(this.transformer));
        }

        @Override // com.google.common.collect.AbstractMultimap
        Iterator<Map.Entry<K, V2>> entryIterator() {
            return Iterators.transform(this.fromMultimap.entries().iterator(), Maps.asEntryToEntryFunction(this.transformer));
        }

        @Override // com.google.common.collect.Multimap
        public Collection<V2> get(@ParametricNullness K k) {
            return m187x893217c0(k, this.fromMultimap.get(k));
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean isEmpty() {
            return this.fromMultimap.isEmpty();
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean put(@ParametricNullness K k, @ParametricNullness V2 v2) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean putAll(Multimap<? extends K, ? extends V2> multimap) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean putAll(@ParametricNullness K k, Iterable<? extends V2> iterable) {
            throw new UnsupportedOperationException();
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public boolean remove(@CheckForNull Object obj, @CheckForNull Object obj2) {
            return get(obj).remove(obj2);
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.google.common.collect.Multimap
        public Collection<V2> removeAll(@CheckForNull Object obj) {
            return m187x893217c0(obj, this.fromMultimap.removeAll(obj));
        }

        @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        public Collection<V2> replaceValues(@ParametricNullness K k, Iterable<? extends V2> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.Multimap
        public int size() {
            return this.fromMultimap.size();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* JADX INFO: renamed from: transform, reason: merged with bridge method [inline-methods] */
        public Collection<V2> m187x893217c0(@ParametricNullness K k, Collection<V1> collection) {
            Function functionAsValueToValueFunction = Maps.asValueToValueFunction(this.transformer, k);
            return collection instanceof List ? Lists.transform((List) collection, functionAsValueToValueFunction) : Collections2.transform(collection, functionAsValueToValueFunction);
        }
    }

    private static class UnmodifiableListMultimap<K, V> extends UnmodifiableMultimap<K, V> implements ListMultimap<K, V> {
        private static final long serialVersionUID = 0;

        UnmodifiableListMultimap(ListMultimap<K, V> listMultimap) {
            super(listMultimap);
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.ForwardingObject
        public ListMultimap<K, V> delegate() {
            return (ListMultimap) super.delegate();
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public List<V> get(@ParametricNullness K k) {
            return Collections.unmodifiableList(delegate().get((Object) k));
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public List<V> removeAll(@CheckForNull Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public List<V> replaceValues(@ParametricNullness K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class UnmodifiableMultimap<K, V> extends ForwardingMultimap<K, V> implements Serializable {
        private static final long serialVersionUID = 0;
        final Multimap<K, V> delegate;

        @CheckForNull
        @LazyInit
        transient Collection<Map.Entry<K, V>> entries;

        @CheckForNull
        @LazyInit
        transient Set<K> keySet;

        @CheckForNull
        @LazyInit
        transient Multiset<K> keys;

        @CheckForNull
        @LazyInit
        transient Map<K, Collection<V>> map;

        @CheckForNull
        @LazyInit
        transient Collection<V> values;

        UnmodifiableMultimap(Multimap<K, V> multimap) {
            this.delegate = (Multimap) Preconditions.checkNotNull(multimap);
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap, com.google.common.collect.ListMultimap
        public Map<K, Collection<V>> asMap() {
            Map<K, Collection<V>> map = this.map;
            if (map != null) {
                return map;
            }
            Map<K, Collection<V>> mapUnmodifiableMap = Collections.unmodifiableMap(Maps.transformValues(this.delegate.asMap(), new Function() { // from class: com.google.common.collect.Multimaps$UnmodifiableMultimap$$ExternalSyntheticLambda0
                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return Multimaps.unmodifiableValueCollection((Collection) obj);
                }
            }));
            this.map = mapUnmodifiableMap;
            return mapUnmodifiableMap;
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public void clear() {
            throw new UnsupportedOperationException();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.ForwardingObject
        public Multimap<K, V> delegate() {
            return this.delegate;
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Collection<Map.Entry<K, V>> entries() {
            Collection<Map.Entry<K, V>> collection = this.entries;
            if (collection != null) {
                return collection;
            }
            Collection<Map.Entry<K, V>> collectionUnmodifiableEntries = Multimaps.unmodifiableEntries(this.delegate.entries());
            this.entries = collectionUnmodifiableEntries;
            return collectionUnmodifiableEntries;
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Collection<V> get(@ParametricNullness K k) {
            return Multimaps.unmodifiableValueCollection(this.delegate.get(k));
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Set<K> keySet() {
            Set<K> set = this.keySet;
            if (set != null) {
                return set;
            }
            Set<K> setUnmodifiableSet = Collections.unmodifiableSet(this.delegate.keySet());
            this.keySet = setUnmodifiableSet;
            return setUnmodifiableSet;
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Multiset<K> keys() {
            Multiset<K> multiset = this.keys;
            if (multiset != null) {
                return multiset;
            }
            Multiset<K> multisetUnmodifiableMultiset = Multisets.unmodifiableMultiset(this.delegate.keys());
            this.keys = multisetUnmodifiableMultiset;
            return multisetUnmodifiableMultiset;
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public boolean put(@ParametricNullness K k, @ParametricNullness V v) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public boolean putAll(@ParametricNullness K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public boolean remove(@CheckForNull Object obj, @CheckForNull Object obj2) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Collection<V> removeAll(@CheckForNull Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Collection<V> replaceValues(@ParametricNullness K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Collection<V> values() {
            Collection<V> collection = this.values;
            if (collection != null) {
                return collection;
            }
            Collection<V> collectionUnmodifiableCollection = Collections.unmodifiableCollection(this.delegate.values());
            this.values = collectionUnmodifiableCollection;
            return collectionUnmodifiableCollection;
        }
    }

    private static class UnmodifiableSetMultimap<K, V> extends UnmodifiableMultimap<K, V> implements SetMultimap<K, V> {
        private static final long serialVersionUID = 0;

        UnmodifiableSetMultimap(SetMultimap<K, V> setMultimap) {
            super(setMultimap);
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.ForwardingObject
        public SetMultimap<K, V> delegate() {
            return (SetMultimap) super.delegate();
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Set<Map.Entry<K, V>> entries() {
            return Maps.unmodifiableEntrySet(delegate().entries());
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Set<V> get(@ParametricNullness K k) {
            return Collections.unmodifiableSet(delegate().get((Object) k));
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Set<V> removeAll(@CheckForNull Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public Set<V> replaceValues(@ParametricNullness K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }
    }

    private static class UnmodifiableSortedSetMultimap<K, V> extends UnmodifiableSetMultimap<K, V> implements SortedSetMultimap<K, V> {
        private static final long serialVersionUID = 0;

        UnmodifiableSortedSetMultimap(SortedSetMultimap<K, V> sortedSetMultimap) {
            super(sortedSetMultimap);
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableSetMultimap, com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.ForwardingObject
        public SortedSetMultimap<K, V> delegate() {
            return (SortedSetMultimap) super.delegate();
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableSetMultimap, com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public SortedSet<V> get(@ParametricNullness K k) {
            return Collections.unmodifiableSortedSet(delegate().get((Object) k));
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableSetMultimap, com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public SortedSet<V> removeAll(@CheckForNull Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.Multimaps.UnmodifiableSetMultimap, com.google.common.collect.Multimaps.UnmodifiableMultimap, com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
        public SortedSet<V> replaceValues(@ParametricNullness K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.SortedSetMultimap
        @CheckForNull
        public Comparator<? super V> valueComparator() {
            return delegate().valueComparator();
        }
    }

    private Multimaps() {
    }

    public static <K, V> Map<K, List<V>> asMap(ListMultimap<K, V> listMultimap) {
        return listMultimap.asMap();
    }

    public static <K, V> Map<K, Collection<V>> asMap(Multimap<K, V> multimap) {
        return multimap.asMap();
    }

    public static <K, V> Map<K, Set<V>> asMap(SetMultimap<K, V> setMultimap) {
        return setMultimap.asMap();
    }

    public static <K, V> Map<K, SortedSet<V>> asMap(SortedSetMultimap<K, V> sortedSetMultimap) {
        return sortedSetMultimap.asMap();
    }

    static boolean equalsImpl(Multimap<?, ?> multimap, @CheckForNull Object obj) {
        if (obj == multimap) {
            return true;
        }
        if (obj instanceof Multimap) {
            return multimap.asMap().equals(((Multimap) obj).asMap());
        }
        return false;
    }

    public static <K, V> Multimap<K, V> filterEntries(Multimap<K, V> multimap, Predicate<? super Map.Entry<K, V>> predicate) {
        Preconditions.checkNotNull(predicate);
        if (multimap instanceof SetMultimap) {
            return filterEntries((SetMultimap) multimap, (Predicate) predicate);
        }
        return multimap instanceof FilteredMultimap ? filterFiltered((FilteredMultimap) multimap, predicate) : new FilteredEntryMultimap((Multimap) Preconditions.checkNotNull(multimap), predicate);
    }

    public static <K, V> SetMultimap<K, V> filterEntries(SetMultimap<K, V> setMultimap, Predicate<? super Map.Entry<K, V>> predicate) {
        Preconditions.checkNotNull(predicate);
        return setMultimap instanceof FilteredSetMultimap ? filterFiltered((FilteredSetMultimap) setMultimap, (Predicate) predicate) : new FilteredEntrySetMultimap((SetMultimap) Preconditions.checkNotNull(setMultimap), predicate);
    }

    private static <K, V> Multimap<K, V> filterFiltered(FilteredMultimap<K, V> filteredMultimap, Predicate<? super Map.Entry<K, V>> predicate) {
        return new FilteredEntryMultimap(filteredMultimap.unfiltered(), Predicates.and(filteredMultimap.entryPredicate(), predicate));
    }

    private static <K, V> SetMultimap<K, V> filterFiltered(FilteredSetMultimap<K, V> filteredSetMultimap, Predicate<? super Map.Entry<K, V>> predicate) {
        return new FilteredEntrySetMultimap(filteredSetMultimap.unfiltered(), Predicates.and(filteredSetMultimap.entryPredicate(), predicate));
    }

    public static <K, V> ListMultimap<K, V> filterKeys(ListMultimap<K, V> listMultimap, Predicate<? super K> predicate) {
        if (!(listMultimap instanceof FilteredKeyListMultimap)) {
            return new FilteredKeyListMultimap(listMultimap, predicate);
        }
        FilteredKeyListMultimap filteredKeyListMultimap = (FilteredKeyListMultimap) listMultimap;
        return new FilteredKeyListMultimap(filteredKeyListMultimap.unfiltered(), Predicates.and(filteredKeyListMultimap.keyPredicate, predicate));
    }

    public static <K, V> Multimap<K, V> filterKeys(Multimap<K, V> multimap, Predicate<? super K> predicate) {
        if (multimap instanceof SetMultimap) {
            return filterKeys((SetMultimap) multimap, (Predicate) predicate);
        }
        if (multimap instanceof ListMultimap) {
            return filterKeys((ListMultimap) multimap, (Predicate) predicate);
        }
        if (!(multimap instanceof FilteredKeyMultimap)) {
            return multimap instanceof FilteredMultimap ? filterFiltered((FilteredMultimap) multimap, Maps.keyPredicateOnEntries(predicate)) : new FilteredKeyMultimap(multimap, predicate);
        }
        FilteredKeyMultimap filteredKeyMultimap = (FilteredKeyMultimap) multimap;
        return new FilteredKeyMultimap(filteredKeyMultimap.unfiltered, Predicates.and(filteredKeyMultimap.keyPredicate, predicate));
    }

    public static <K, V> SetMultimap<K, V> filterKeys(SetMultimap<K, V> setMultimap, Predicate<? super K> predicate) {
        if (!(setMultimap instanceof FilteredKeySetMultimap)) {
            return setMultimap instanceof FilteredSetMultimap ? filterFiltered((FilteredSetMultimap) setMultimap, Maps.keyPredicateOnEntries(predicate)) : new FilteredKeySetMultimap(setMultimap, predicate);
        }
        FilteredKeySetMultimap filteredKeySetMultimap = (FilteredKeySetMultimap) setMultimap;
        return new FilteredKeySetMultimap(filteredKeySetMultimap.unfiltered(), Predicates.and(filteredKeySetMultimap.keyPredicate, predicate));
    }

    public static <K, V> Multimap<K, V> filterValues(Multimap<K, V> multimap, Predicate<? super V> predicate) {
        return filterEntries(multimap, Maps.valuePredicateOnEntries(predicate));
    }

    public static <K, V> SetMultimap<K, V> filterValues(SetMultimap<K, V> setMultimap, Predicate<? super V> predicate) {
        return filterEntries((SetMultimap) setMultimap, Maps.valuePredicateOnEntries(predicate));
    }

    static <T, K, V, M extends Multimap<K, V>> Collector<T, ?, M> flatteningToMultimap(java.util.function.Function<? super T, ? extends K> function, java.util.function.Function<? super T, ? extends Stream<? extends V>> function2, java.util.function.Supplier<M> supplier) {
        return CollectCollectors.flatteningToMultimap(function, function2, supplier);
    }

    public static <K, V> SetMultimap<K, V> forMap(Map<K, V> map) {
        return new MapMultimap(map);
    }

    public static <K, V> ImmutableListMultimap<K, V> index(Iterable<V> iterable, Function<? super V, K> function) {
        return index(iterable.iterator(), function);
    }

    public static <K, V> ImmutableListMultimap<K, V> index(Iterator<V> it, Function<? super V, K> function) {
        Preconditions.checkNotNull(function);
        ImmutableListMultimap.Builder builder = ImmutableListMultimap.builder();
        while (it.hasNext()) {
            V next = it.next();
            Preconditions.checkNotNull(next, it);
            builder.put((Object) function.apply(next), (Object) next);
        }
        return builder.build();
    }

    public static <K, V, M extends Multimap<K, V>> M invertFrom(Multimap<? extends V, ? extends K> multimap, M m) {
        Preconditions.checkNotNull(m);
        for (Map.Entry<? extends V, ? extends K> entry : multimap.entries()) {
            m.put(entry.getValue(), entry.getKey());
        }
        return m;
    }

    public static <K, V> ListMultimap<K, V> newListMultimap(Map<K, Collection<V>> map, Supplier<? extends List<V>> supplier) {
        return new CustomListMultimap(map, supplier);
    }

    public static <K, V> Multimap<K, V> newMultimap(Map<K, Collection<V>> map, Supplier<? extends Collection<V>> supplier) {
        return new CustomMultimap(map, supplier);
    }

    public static <K, V> SetMultimap<K, V> newSetMultimap(Map<K, Collection<V>> map, Supplier<? extends Set<V>> supplier) {
        return new CustomSetMultimap(map, supplier);
    }

    public static <K, V> SortedSetMultimap<K, V> newSortedSetMultimap(Map<K, Collection<V>> map, Supplier<? extends SortedSet<V>> supplier) {
        return new CustomSortedSetMultimap(map, supplier);
    }

    public static <K, V> ListMultimap<K, V> synchronizedListMultimap(ListMultimap<K, V> listMultimap) {
        return Synchronized.listMultimap(listMultimap, null);
    }

    public static <K, V> Multimap<K, V> synchronizedMultimap(Multimap<K, V> multimap) {
        return Synchronized.multimap(multimap, null);
    }

    public static <K, V> SetMultimap<K, V> synchronizedSetMultimap(SetMultimap<K, V> setMultimap) {
        return Synchronized.setMultimap(setMultimap, null);
    }

    public static <K, V> SortedSetMultimap<K, V> synchronizedSortedSetMultimap(SortedSetMultimap<K, V> sortedSetMultimap) {
        return Synchronized.sortedSetMultimap(sortedSetMultimap, null);
    }

    static <T, K, V, M extends Multimap<K, V>> Collector<T, ?, M> toMultimap(java.util.function.Function<? super T, ? extends K> function, java.util.function.Function<? super T, ? extends V> function2, java.util.function.Supplier<M> supplier) {
        return CollectCollectors.toMultimap(function, function2, supplier);
    }

    public static <K, V1, V2> ListMultimap<K, V2> transformEntries(ListMultimap<K, V1> listMultimap, Maps.EntryTransformer<? super K, ? super V1, V2> entryTransformer) {
        return new TransformedEntriesListMultimap(listMultimap, entryTransformer);
    }

    public static <K, V1, V2> Multimap<K, V2> transformEntries(Multimap<K, V1> multimap, Maps.EntryTransformer<? super K, ? super V1, V2> entryTransformer) {
        return new TransformedEntriesMultimap(multimap, entryTransformer);
    }

    public static <K, V1, V2> ListMultimap<K, V2> transformValues(ListMultimap<K, V1> listMultimap, Function<? super V1, V2> function) {
        Preconditions.checkNotNull(function);
        return transformEntries((ListMultimap) listMultimap, Maps.asEntryTransformer(function));
    }

    public static <K, V1, V2> Multimap<K, V2> transformValues(Multimap<K, V1> multimap, Function<? super V1, V2> function) {
        Preconditions.checkNotNull(function);
        return transformEntries(multimap, Maps.asEntryTransformer(function));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <K, V> Collection<Map.Entry<K, V>> unmodifiableEntries(Collection<Map.Entry<K, V>> collection) {
        return collection instanceof Set ? Maps.unmodifiableEntrySet((Set) collection) : new Maps.UnmodifiableEntries(Collections.unmodifiableCollection(collection));
    }

    @Deprecated
    public static <K, V> ListMultimap<K, V> unmodifiableListMultimap(ImmutableListMultimap<K, V> immutableListMultimap) {
        return (ListMultimap) Preconditions.checkNotNull(immutableListMultimap);
    }

    public static <K, V> ListMultimap<K, V> unmodifiableListMultimap(ListMultimap<K, V> listMultimap) {
        return ((listMultimap instanceof UnmodifiableListMultimap) || (listMultimap instanceof ImmutableListMultimap)) ? listMultimap : new UnmodifiableListMultimap(listMultimap);
    }

    @Deprecated
    public static <K, V> Multimap<K, V> unmodifiableMultimap(ImmutableMultimap<K, V> immutableMultimap) {
        return (Multimap) Preconditions.checkNotNull(immutableMultimap);
    }

    public static <K, V> Multimap<K, V> unmodifiableMultimap(Multimap<K, V> multimap) {
        return ((multimap instanceof UnmodifiableMultimap) || (multimap instanceof ImmutableMultimap)) ? multimap : new UnmodifiableMultimap(multimap);
    }

    @Deprecated
    public static <K, V> SetMultimap<K, V> unmodifiableSetMultimap(ImmutableSetMultimap<K, V> immutableSetMultimap) {
        return (SetMultimap) Preconditions.checkNotNull(immutableSetMultimap);
    }

    public static <K, V> SetMultimap<K, V> unmodifiableSetMultimap(SetMultimap<K, V> setMultimap) {
        return ((setMultimap instanceof UnmodifiableSetMultimap) || (setMultimap instanceof ImmutableSetMultimap)) ? setMultimap : new UnmodifiableSetMultimap(setMultimap);
    }

    public static <K, V> SortedSetMultimap<K, V> unmodifiableSortedSetMultimap(SortedSetMultimap<K, V> sortedSetMultimap) {
        return sortedSetMultimap instanceof UnmodifiableSortedSetMultimap ? sortedSetMultimap : new UnmodifiableSortedSetMultimap(sortedSetMultimap);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <V> Collection<V> unmodifiableValueCollection(Collection<V> collection) {
        if (collection instanceof SortedSet) {
            return Collections.unmodifiableSortedSet((SortedSet) collection);
        }
        if (collection instanceof Set) {
            return Collections.unmodifiableSet((Set) collection);
        }
        return collection instanceof List ? Collections.unmodifiableList((List) collection) : Collections.unmodifiableCollection(collection);
    }
}
