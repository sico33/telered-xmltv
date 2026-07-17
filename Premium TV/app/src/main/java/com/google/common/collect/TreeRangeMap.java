package com.google.common.collect;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.lang.Comparable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class TreeRangeMap<K extends Comparable, V> implements RangeMap<K, V> {
    private static final RangeMap<Comparable<?>, Object> EMPTY_SUB_RANGE_MAP = new RangeMap<Comparable<?>, Object>() { // from class: com.google.common.collect.TreeRangeMap.1
        @Override // com.google.common.collect.RangeMap
        public Map<Range<Comparable<?>>, Object> asDescendingMapOfRanges() {
            return Collections.emptyMap();
        }

        @Override // com.google.common.collect.RangeMap
        public Map<Range<Comparable<?>>, Object> asMapOfRanges() {
            return Collections.emptyMap();
        }

        @Override // com.google.common.collect.RangeMap
        public void clear() {
        }

        @Override // com.google.common.collect.RangeMap
        @CheckForNull
        public Object get(Comparable<?> comparable) {
            return null;
        }

        @Override // com.google.common.collect.RangeMap
        @CheckForNull
        public Map.Entry<Range<Comparable<?>>, Object> getEntry(Comparable<?> comparable) {
            return null;
        }

        @Override // com.google.common.collect.RangeMap
        public void put(Range<Comparable<?>> range, Object obj) {
            Preconditions.checkNotNull(range);
            throw new IllegalArgumentException("Cannot insert range " + range + " into an empty subRangeMap");
        }

        @Override // com.google.common.collect.RangeMap
        public void putAll(RangeMap<Comparable<?>, ? extends Object> rangeMap) {
            if (!rangeMap.asMapOfRanges().isEmpty()) {
                throw new IllegalArgumentException("Cannot putAll(nonEmptyRangeMap) into an empty subRangeMap");
            }
        }

        @Override // com.google.common.collect.RangeMap
        public void putCoalescing(Range<Comparable<?>> range, Object obj) {
            Preconditions.checkNotNull(range);
            throw new IllegalArgumentException("Cannot insert range " + range + " into an empty subRangeMap");
        }

        @Override // com.google.common.collect.RangeMap
        public void remove(Range<Comparable<?>> range) {
            Preconditions.checkNotNull(range);
        }

        @Override // com.google.common.collect.RangeMap
        public Range<Comparable<?>> span() {
            throw new NoSuchElementException();
        }

        @Override // com.google.common.collect.RangeMap
        public RangeMap<Comparable<?>, Object> subRangeMap(Range<Comparable<?>> range) {
            Preconditions.checkNotNull(range);
            return this;
        }
    };
    private final NavigableMap<Cut<K>, RangeMapEntry<K, V>> entriesByLowerBound = Maps.newTreeMap();

    private final class AsMapOfRanges extends Maps.IteratorBasedAbstractMap<Range<K>, V> {
        final Iterable<Map.Entry<Range<K>, V>> entryIterable;
        final TreeRangeMap this$0;

        AsMapOfRanges(TreeRangeMap treeRangeMap, Iterable<RangeMapEntry<K, V>> iterable) {
            this.this$0 = treeRangeMap;
            this.entryIterable = iterable;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(@CheckForNull Object obj) {
            return get(obj) != null;
        }

        @Override // com.google.common.collect.Maps.IteratorBasedAbstractMap
        Iterator<Map.Entry<Range<K>, V>> entryIterator() {
            return this.entryIterable.iterator();
        }

        @Override // java.util.AbstractMap, java.util.Map
        @CheckForNull
        public V get(@CheckForNull Object obj) {
            if (obj instanceof Range) {
                Range range = (Range) obj;
                RangeMapEntry rangeMapEntry = (RangeMapEntry) this.this$0.entriesByLowerBound.get(range.lowerBound);
                if (rangeMapEntry != null && rangeMapEntry.getKey().equals(range)) {
                    return (V) rangeMapEntry.getValue();
                }
            }
            return null;
        }

        @Override // com.google.common.collect.Maps.IteratorBasedAbstractMap, java.util.AbstractMap, java.util.Map
        public int size() {
            return this.this$0.entriesByLowerBound.size();
        }
    }

    private static final class RangeMapEntry<K extends Comparable, V> extends AbstractMapEntry<Range<K>, V> {
        private final Range<K> range;
        private final V value;

        RangeMapEntry(Cut<K> cut, Cut<K> cut2, V v) {
            this(Range.create(cut, cut2), v);
        }

        RangeMapEntry(Range<K> range, V v) {
            this.range = range;
            this.value = v;
        }

        public boolean contains(K k) {
            return this.range.contains(k);
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        public Range<K> getKey() {
            return this.range;
        }

        Cut<K> getLowerBound() {
            return (Cut<K>) this.range.lowerBound;
        }

        Cut<K> getUpperBound() {
            return (Cut<K>) this.range.upperBound;
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        public V getValue() {
            return this.value;
        }
    }

    private class SubRangeMap implements RangeMap<K, V> {
        private final Range<K> subRange;
        final TreeRangeMap this$0;

        /* JADX INFO: renamed from: com.google.common.collect.TreeRangeMap$SubRangeMap$1, reason: invalid class name */
        class AnonymousClass1 extends TreeRangeMap<K, V>.SubRangeMap.SubRangeMapAsMap {
            final SubRangeMap this$1;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            AnonymousClass1(SubRangeMap subRangeMap) {
                super(subRangeMap);
                this.this$1 = subRangeMap;
            }

            @Override // com.google.common.collect.TreeRangeMap.SubRangeMap.SubRangeMapAsMap
            Iterator<Map.Entry<Range<K>, V>> entryIterator() {
                return this.this$1.subRange.isEmpty() ? Iterators.emptyIterator() : new AbstractIterator<Map.Entry<Range<K>, V>>(this, this.this$1.this$0.entriesByLowerBound.headMap(this.this$1.subRange.upperBound, false).descendingMap().values().iterator()) { // from class: com.google.common.collect.TreeRangeMap.SubRangeMap.1.1
                    final AnonymousClass1 this$2;
                    final Iterator val$backingItr;

                    {
                        this.this$2 = this;
                        this.val$backingItr = it;
                    }

                    /* JADX INFO: Access modifiers changed from: protected */
                    @Override // com.google.common.collect.AbstractIterator
                    @CheckForNull
                    public Map.Entry<Range<K>, V> computeNext() {
                        if (!this.val$backingItr.hasNext()) {
                            return (Map.Entry) endOfData();
                        }
                        RangeMapEntry rangeMapEntry = (RangeMapEntry) this.val$backingItr.next();
                        return rangeMapEntry.getUpperBound().compareTo((Cut) this.this$2.this$1.subRange.lowerBound) <= 0 ? (Map.Entry) endOfData() : Maps.immutableEntry(rangeMapEntry.getKey().intersection(this.this$2.this$1.subRange), rangeMapEntry.getValue());
                    }
                };
            }
        }

        class SubRangeMapAsMap extends AbstractMap<Range<K>, V> {
            final SubRangeMap this$1;

            SubRangeMapAsMap(SubRangeMap subRangeMap) {
                this.this$1 = subRangeMap;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public boolean removeEntryIf(Predicate<? super Map.Entry<Range<K>, V>> predicate) {
                ArrayList arrayListNewArrayList = Lists.newArrayList();
                for (Map.Entry<Range<K>, V> entry : entrySet()) {
                    if (predicate.apply(entry)) {
                        arrayListNewArrayList.add(entry.getKey());
                    }
                }
                Iterator it = arrayListNewArrayList.iterator();
                while (it.hasNext()) {
                    this.this$1.this$0.remove((Range) it.next());
                }
                return !arrayListNewArrayList.isEmpty();
            }

            @Override // java.util.AbstractMap, java.util.Map
            public void clear() {
                this.this$1.clear();
            }

            @Override // java.util.AbstractMap, java.util.Map
            public boolean containsKey(@CheckForNull Object obj) {
                return get(obj) != null;
            }

            Iterator<Map.Entry<Range<K>, V>> entryIterator() {
                if (this.this$1.subRange.isEmpty()) {
                    return Iterators.emptyIterator();
                }
                return new AbstractIterator<Map.Entry<Range<K>, V>>(this, this.this$1.this$0.entriesByLowerBound.tailMap((Cut) MoreObjects.firstNonNull((Cut) this.this$1.this$0.entriesByLowerBound.floorKey(this.this$1.subRange.lowerBound), this.this$1.subRange.lowerBound), true).values().iterator()) { // from class: com.google.common.collect.TreeRangeMap.SubRangeMap.SubRangeMapAsMap.3
                    final SubRangeMapAsMap this$2;
                    final Iterator val$backingItr;

                    {
                        this.this$2 = this;
                        this.val$backingItr = it;
                    }

                    /* JADX INFO: Access modifiers changed from: protected */
                    @Override // com.google.common.collect.AbstractIterator
                    @CheckForNull
                    public Map.Entry<Range<K>, V> computeNext() {
                        while (this.val$backingItr.hasNext()) {
                            RangeMapEntry rangeMapEntry = (RangeMapEntry) this.val$backingItr.next();
                            if (rangeMapEntry.getLowerBound().compareTo((Cut) this.this$2.this$1.subRange.upperBound) >= 0) {
                                return (Map.Entry) endOfData();
                            }
                            if (rangeMapEntry.getUpperBound().compareTo((Cut) this.this$2.this$1.subRange.lowerBound) > 0) {
                                return Maps.immutableEntry(rangeMapEntry.getKey().intersection(this.this$2.this$1.subRange), rangeMapEntry.getValue());
                            }
                        }
                        return (Map.Entry) endOfData();
                    }
                };
            }

            @Override // java.util.AbstractMap, java.util.Map
            public Set<Map.Entry<Range<K>, V>> entrySet() {
                return new Maps.EntrySet<Range<K>, V>(this) { // from class: com.google.common.collect.TreeRangeMap.SubRangeMap.SubRangeMapAsMap.2
                    final SubRangeMapAsMap this$2;

                    {
                        this.this$2 = this;
                    }

                    @Override // com.google.common.collect.Maps.EntrySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
                    public boolean isEmpty() {
                        return !iterator().hasNext();
                    }

                    @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
                    public Iterator<Map.Entry<Range<K>, V>> iterator() {
                        return this.this$2.entryIterator();
                    }

                    @Override // com.google.common.collect.Maps.EntrySet
                    Map<Range<K>, V> map() {
                        return this.this$2;
                    }

                    @Override // com.google.common.collect.Maps.EntrySet, com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
                    public boolean retainAll(Collection<?> collection) {
                        return this.this$2.removeEntryIf(Predicates.not(Predicates.in(collection)));
                    }

                    @Override // com.google.common.collect.Maps.EntrySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
                    public int size() {
                        return Iterators.size(iterator());
                    }
                };
            }

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
            @Override // java.util.AbstractMap, java.util.Map
            @CheckForNull
            public V get(@CheckForNull Object obj) {
                RangeMapEntry rangeMapEntry;
                V v = null;
                try {
                    if (!(obj instanceof Range)) {
                        return null;
                    }
                    Range range = (Range) obj;
                    if (!this.this$1.subRange.encloses(range) || range.isEmpty()) {
                        return null;
                    }
                    int iCompareTo = range.lowerBound.compareTo((Cut) this.this$1.subRange.lowerBound);
                    SubRangeMap subRangeMap = this.this$1;
                    if (iCompareTo == 0) {
                        Map.Entry entryFloorEntry = subRangeMap.this$0.entriesByLowerBound.floorEntry(range.lowerBound);
                        rangeMapEntry = entryFloorEntry != null ? (RangeMapEntry) entryFloorEntry.getValue() : null;
                    } else {
                        rangeMapEntry = (RangeMapEntry) subRangeMap.this$0.entriesByLowerBound.get(range.lowerBound);
                    }
                    if (rangeMapEntry == null || !rangeMapEntry.getKey().isConnected(this.this$1.subRange) || !rangeMapEntry.getKey().intersection(this.this$1.subRange).equals(range)) {
                        return null;
                    }
                    v = (V) rangeMapEntry.getValue();
                    return v;
                } catch (ClassCastException e) {
                    return v;
                }
            }

            @Override // java.util.AbstractMap, java.util.Map
            public Set<Range<K>> keySet() {
                return new Maps.KeySet<Range<K>, V>(this, this) { // from class: com.google.common.collect.TreeRangeMap.SubRangeMap.SubRangeMapAsMap.1
                    final SubRangeMapAsMap this$2;

                    {
                        this.this$2 = this;
                    }

                    @Override // com.google.common.collect.Maps.KeySet, java.util.AbstractCollection, java.util.Collection, java.util.Set
                    public boolean remove(@CheckForNull Object obj) {
                        return this.this$2.remove(obj) != null;
                    }

                    @Override // com.google.common.collect.Sets.ImprovedAbstractSet, java.util.AbstractCollection, java.util.Collection, java.util.Set
                    public boolean retainAll(Collection<?> collection) {
                        return this.this$2.removeEntryIf(Predicates.compose(Predicates.not(Predicates.in(collection)), Maps.keyFunction()));
                    }
                };
            }

            @Override // java.util.AbstractMap, java.util.Map
            @CheckForNull
            public V remove(@CheckForNull Object obj) {
                V v = (V) get(obj);
                if (v == null) {
                    return null;
                }
                this.this$1.this$0.remove((Range) Objects.requireNonNull(obj));
                return v;
            }

            @Override // java.util.AbstractMap, java.util.Map
            public Collection<V> values() {
                return new Maps.Values<Range<K>, V>(this, this) { // from class: com.google.common.collect.TreeRangeMap.SubRangeMap.SubRangeMapAsMap.4
                    final SubRangeMapAsMap this$2;

                    {
                        this.this$2 = this;
                    }

                    @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
                    public boolean removeAll(Collection<?> collection) {
                        return this.this$2.removeEntryIf(Predicates.compose(Predicates.in(collection), Maps.valueFunction()));
                    }

                    @Override // com.google.common.collect.Maps.Values, java.util.AbstractCollection, java.util.Collection
                    public boolean retainAll(Collection<?> collection) {
                        return this.this$2.removeEntryIf(Predicates.compose(Predicates.not(Predicates.in(collection)), Maps.valueFunction()));
                    }
                };
            }
        }

        SubRangeMap(TreeRangeMap treeRangeMap, Range<K> range) {
            this.this$0 = treeRangeMap;
            this.subRange = range;
        }

        @Override // com.google.common.collect.RangeMap
        public Map<Range<K>, V> asDescendingMapOfRanges() {
            return new AnonymousClass1(this);
        }

        @Override // com.google.common.collect.RangeMap
        public Map<Range<K>, V> asMapOfRanges() {
            return new SubRangeMapAsMap(this);
        }

        @Override // com.google.common.collect.RangeMap
        public void clear() {
            this.this$0.remove(this.subRange);
        }

        @Override // com.google.common.collect.RangeMap
        public boolean equals(@CheckForNull Object obj) {
            if (obj instanceof RangeMap) {
                return asMapOfRanges().equals(((RangeMap) obj).asMapOfRanges());
            }
            return false;
        }

        @Override // com.google.common.collect.RangeMap
        @CheckForNull
        public V get(K k) {
            if (this.subRange.contains(k)) {
                return (V) this.this$0.get(k);
            }
            return null;
        }

        @Override // com.google.common.collect.RangeMap
        @CheckForNull
        public Map.Entry<Range<K>, V> getEntry(K k) {
            Map.Entry<Range<K>, V> entry;
            if (!this.subRange.contains(k) || (entry = this.this$0.getEntry(k)) == null) {
                return null;
            }
            return Maps.immutableEntry(entry.getKey().intersection(this.subRange), entry.getValue());
        }

        @Override // com.google.common.collect.RangeMap
        public int hashCode() {
            return asMapOfRanges().hashCode();
        }

        @Override // com.google.common.collect.RangeMap
        public void put(Range<K> range, V v) {
            Preconditions.checkArgument(this.subRange.encloses(range), "Cannot put range %s into a subRangeMap(%s)", range, this.subRange);
            this.this$0.put(range, v);
        }

        @Override // com.google.common.collect.RangeMap
        public void putAll(RangeMap<K, ? extends V> rangeMap) {
            if (rangeMap.asMapOfRanges().isEmpty()) {
                return;
            }
            Range<K> rangeSpan = rangeMap.span();
            Preconditions.checkArgument(this.subRange.encloses(rangeSpan), "Cannot putAll rangeMap with span %s into a subRangeMap(%s)", rangeSpan, this.subRange);
            this.this$0.putAll(rangeMap);
        }

        @Override // com.google.common.collect.RangeMap
        public void putCoalescing(Range<K> range, V v) {
            if (this.this$0.entriesByLowerBound.isEmpty() || !this.subRange.encloses(range)) {
                put(range, v);
            } else {
                put(this.this$0.coalescedRange(range, Preconditions.checkNotNull(v)).intersection(this.subRange), v);
            }
        }

        /* JADX WARN: Multi-variable type inference failed */
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
        @Override // com.google.common.collect.RangeMap
        public void remove(Range<K> range) {
            if (range.isConnected(this.subRange)) {
                this.this$0.remove(range.intersection(this.subRange));
            }
        }

        @Override // com.google.common.collect.RangeMap
        public Range<K> span() {
            Cut cut;
            Map.Entry entryFloorEntry = this.this$0.entriesByLowerBound.floorEntry(this.subRange.lowerBound);
            if (entryFloorEntry == null || ((RangeMapEntry) entryFloorEntry.getValue()).getUpperBound().compareTo((Cut) this.subRange.lowerBound) <= 0) {
                Cut cut2 = (Cut) this.this$0.entriesByLowerBound.ceilingKey(this.subRange.lowerBound);
                if (cut2 == null || cut2.compareTo((Cut) this.subRange.upperBound) >= 0) {
                    throw new NoSuchElementException();
                }
                cut = cut2;
            } else {
                cut = this.subRange.lowerBound;
            }
            Map.Entry entryLowerEntry = this.this$0.entriesByLowerBound.lowerEntry(this.subRange.upperBound);
            if (entryLowerEntry != null) {
                return Range.create(cut, ((RangeMapEntry) entryLowerEntry.getValue()).getUpperBound().compareTo((Cut) this.subRange.upperBound) >= 0 ? this.subRange.upperBound : ((RangeMapEntry) entryLowerEntry.getValue()).getUpperBound());
            }
            throw new NoSuchElementException();
        }

        /* JADX WARN: Multi-variable type inference failed */
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
        @Override // com.google.common.collect.RangeMap
        public RangeMap<K, V> subRangeMap(Range<K> range) {
            boolean zIsConnected = range.isConnected(this.subRange);
            TreeRangeMap treeRangeMap = this.this$0;
            return !zIsConnected ? treeRangeMap.emptySubRangeMap() : treeRangeMap.subRangeMap(range.intersection(this.subRange));
        }

        @Override // com.google.common.collect.RangeMap
        public String toString() {
            return asMapOfRanges().toString();
        }
    }

    private TreeRangeMap() {
    }

    private static <K extends Comparable, V> Range<K> coalesce(Range<K> range, V v, @CheckForNull Map.Entry<Cut<K>, RangeMapEntry<K, V>> entry) {
        return (entry != null && entry.getValue().getKey().isConnected(range) && entry.getValue().getValue().equals(v)) ? (Range<K>) range.span(entry.getValue().getKey()) : range;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Type inference incomplete: some casts might be missing */
    public Range<K> coalescedRange(Range<K> range, V v) {
        return coalesce(coalesce(range, v, this.entriesByLowerBound.lowerEntry((Cut<K>) range.lowerBound)), v, this.entriesByLowerBound.floorEntry((Cut<K>) range.upperBound));
    }

    public static <K extends Comparable, V> TreeRangeMap<K, V> create() {
        return new TreeRangeMap<>();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public RangeMap<K, V> emptySubRangeMap() {
        return EMPTY_SUB_RANGE_MAP;
    }

    private void putRangeMapEntry(Cut<K> cut, Cut<K> cut2, V v) {
        this.entriesByLowerBound.put(cut, new RangeMapEntry(cut, cut2, v));
    }

    @Override // com.google.common.collect.RangeMap
    public Map<Range<K>, V> asDescendingMapOfRanges() {
        return new AsMapOfRanges(this, this.entriesByLowerBound.descendingMap().values());
    }

    @Override // com.google.common.collect.RangeMap
    public Map<Range<K>, V> asMapOfRanges() {
        return new AsMapOfRanges(this, this.entriesByLowerBound.values());
    }

    @Override // com.google.common.collect.RangeMap
    public void clear() {
        this.entriesByLowerBound.clear();
    }

    @Override // com.google.common.collect.RangeMap
    public boolean equals(@CheckForNull Object obj) {
        if (obj instanceof RangeMap) {
            return asMapOfRanges().equals(((RangeMap) obj).asMapOfRanges());
        }
        return false;
    }

    @Override // com.google.common.collect.RangeMap
    @CheckForNull
    public V get(K k) {
        Map.Entry<Range<K>, V> entry = getEntry(k);
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }

    @Override // com.google.common.collect.RangeMap
    @CheckForNull
    public Map.Entry<Range<K>, V> getEntry(K k) {
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryFloorEntry = this.entriesByLowerBound.floorEntry(Cut.belowValue(k));
        if (entryFloorEntry == null || !entryFloorEntry.getValue().contains(k)) {
            return null;
        }
        return entryFloorEntry.getValue();
    }

    @Override // com.google.common.collect.RangeMap
    public int hashCode() {
        return asMapOfRanges().hashCode();
    }

    @Override // com.google.common.collect.RangeMap
    public void put(Range<K> range, V v) {
        if (range.isEmpty()) {
            return;
        }
        Preconditions.checkNotNull(v);
        remove(range);
        this.entriesByLowerBound.put(range.lowerBound, new RangeMapEntry(range, v));
    }

    @Override // com.google.common.collect.RangeMap
    public void putAll(RangeMap<K, ? extends V> rangeMap) {
        for (Map.Entry<Range<K>, ? extends V> entry : rangeMap.asMapOfRanges().entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.RangeMap
    public void putCoalescing(Range<K> range, V v) {
        if (this.entriesByLowerBound.isEmpty()) {
            put(range, v);
        } else {
            put(coalescedRange(range, Preconditions.checkNotNull(v)), v);
        }
    }

    /* JADX WARN: Type inference incomplete: some casts might be missing */
    @Override // com.google.common.collect.RangeMap
    public void remove(Range<K> range) {
        if (range.isEmpty()) {
            return;
        }
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryLowerEntry = this.entriesByLowerBound.lowerEntry((Cut<K>) range.lowerBound);
        if (entryLowerEntry != null) {
            RangeMapEntry<K, V> value = entryLowerEntry.getValue();
            if (value.getUpperBound().compareTo((Cut) range.lowerBound) > 0) {
                if (value.getUpperBound().compareTo((Cut) range.upperBound) > 0) {
                    putRangeMapEntry(range.upperBound, value.getUpperBound(), entryLowerEntry.getValue().getValue());
                }
                putRangeMapEntry(value.getLowerBound(), range.lowerBound, entryLowerEntry.getValue().getValue());
            }
        }
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryLowerEntry2 = this.entriesByLowerBound.lowerEntry((Cut<K>) range.upperBound);
        if (entryLowerEntry2 != null) {
            RangeMapEntry<K, V> value2 = entryLowerEntry2.getValue();
            if (value2.getUpperBound().compareTo((Cut) range.upperBound) > 0) {
                putRangeMapEntry(range.upperBound, value2.getUpperBound(), entryLowerEntry2.getValue().getValue());
            }
        }
        this.entriesByLowerBound.subMap((Cut<K>) range.lowerBound, (Cut<K>) range.upperBound).clear();
    }

    @Override // com.google.common.collect.RangeMap
    public Range<K> span() {
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryFirstEntry = this.entriesByLowerBound.firstEntry();
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryLastEntry = this.entriesByLowerBound.lastEntry();
        if (entryFirstEntry == null || entryLastEntry == null) {
            throw new NoSuchElementException();
        }
        return Range.create(entryFirstEntry.getValue().getKey().lowerBound, entryLastEntry.getValue().getKey().upperBound);
    }

    @Override // com.google.common.collect.RangeMap
    public RangeMap<K, V> subRangeMap(Range<K> range) {
        return range.equals(Range.all()) ? this : new SubRangeMap(this, range);
    }

    @Override // com.google.common.collect.RangeMap
    public String toString() {
        return this.entriesByLowerBound.values().toString();
    }
}
