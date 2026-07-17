package com.google.common.collect;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class ForwardingNavigableMap<K, V> extends ForwardingSortedMap<K, V> implements NavigableMap<K, V> {

    protected class StandardDescendingMap extends Maps.DescendingMap<K, V> {
        final ForwardingNavigableMap this$0;

        public StandardDescendingMap(ForwardingNavigableMap forwardingNavigableMap) {
            this.this$0 = forwardingNavigableMap;
        }

        @Override // com.google.common.collect.Maps.DescendingMap
        protected Iterator<Map.Entry<K, V>> entryIterator() {
            return new Iterator<Map.Entry<K, V>>(this) { // from class: com.google.common.collect.ForwardingNavigableMap.StandardDescendingMap.1

                @CheckForNull
                private Map.Entry<K, V> nextOrNull;
                final StandardDescendingMap this$1;

                @CheckForNull
                private Map.Entry<K, V> toRemove = null;

                {
                    this.this$1 = this;
                    this.nextOrNull = this.this$1.forward().lastEntry();
                }

                @Override // java.util.Iterator
                public boolean hasNext() {
                    return this.nextOrNull != null;
                }

                @Override // java.util.Iterator
                public Map.Entry<K, V> next() {
                    if (this.nextOrNull == null) {
                        throw new NoSuchElementException();
                    }
                    try {
                        return this.nextOrNull;
                    } finally {
                        this.toRemove = this.nextOrNull;
                        this.nextOrNull = this.this$1.forward().lowerEntry(this.nextOrNull.getKey());
                    }
                }

                @Override // java.util.Iterator
                public void remove() {
                    if (this.toRemove == null) {
                        throw new IllegalStateException("no calls to next() since the last call to remove()");
                    }
                    this.this$1.forward().remove(this.toRemove.getKey());
                    this.toRemove = null;
                }
            };
        }

        @Override // com.google.common.collect.Maps.DescendingMap
        NavigableMap<K, V> forward() {
            return this.this$0;
        }
    }

    protected class StandardNavigableKeySet extends Maps.NavigableKeySet<K, V> {
        public StandardNavigableKeySet(ForwardingNavigableMap forwardingNavigableMap) {
            super(forwardingNavigableMap);
        }
    }

    protected ForwardingNavigableMap() {
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public Map.Entry<K, V> ceilingEntry(@ParametricNullness K k) {
        return delegate().ceilingEntry(k);
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public K ceilingKey(@ParametricNullness K k) {
        return delegate().ceilingKey(k);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.google.common.collect.ForwardingSortedMap, com.google.common.collect.ForwardingMap, com.google.common.collect.ForwardingObject
    public abstract NavigableMap<K, V> delegate();

    @Override // java.util.NavigableMap
    public NavigableSet<K> descendingKeySet() {
        return delegate().descendingKeySet();
    }

    @Override // java.util.NavigableMap
    public NavigableMap<K, V> descendingMap() {
        return delegate().descendingMap();
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public Map.Entry<K, V> firstEntry() {
        return delegate().firstEntry();
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public Map.Entry<K, V> floorEntry(@ParametricNullness K k) {
        return delegate().floorEntry(k);
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public K floorKey(@ParametricNullness K k) {
        return delegate().floorKey(k);
    }

    @Override // java.util.NavigableMap
    public NavigableMap<K, V> headMap(@ParametricNullness K k, boolean z) {
        return delegate().headMap(k, z);
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public Map.Entry<K, V> higherEntry(@ParametricNullness K k) {
        return delegate().higherEntry(k);
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public K higherKey(@ParametricNullness K k) {
        return delegate().higherKey(k);
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public Map.Entry<K, V> lastEntry() {
        return delegate().lastEntry();
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public Map.Entry<K, V> lowerEntry(@ParametricNullness K k) {
        return delegate().lowerEntry(k);
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public K lowerKey(@ParametricNullness K k) {
        return delegate().lowerKey(k);
    }

    @Override // java.util.NavigableMap
    public NavigableSet<K> navigableKeySet() {
        return delegate().navigableKeySet();
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public Map.Entry<K, V> pollFirstEntry() {
        return delegate().pollFirstEntry();
    }

    @Override // java.util.NavigableMap
    @CheckForNull
    public Map.Entry<K, V> pollLastEntry() {
        return delegate().pollLastEntry();
    }

    @CheckForNull
    protected Map.Entry<K, V> standardCeilingEntry(@ParametricNullness K k) {
        return tailMap(k, true).firstEntry();
    }

    @CheckForNull
    protected K standardCeilingKey(@ParametricNullness K k) {
        return (K) Maps.keyOrNull(ceilingEntry(k));
    }

    protected NavigableSet<K> standardDescendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @CheckForNull
    protected Map.Entry<K, V> standardFirstEntry() {
        return (Map.Entry) Iterables.getFirst(entrySet(), null);
    }

    protected K standardFirstKey() {
        Map.Entry<K, V> entryFirstEntry = firstEntry();
        if (entryFirstEntry != null) {
            return entryFirstEntry.getKey();
        }
        throw new NoSuchElementException();
    }

    @CheckForNull
    protected Map.Entry<K, V> standardFloorEntry(@ParametricNullness K k) {
        return headMap(k, true).lastEntry();
    }

    @CheckForNull
    protected K standardFloorKey(@ParametricNullness K k) {
        return (K) Maps.keyOrNull(floorEntry(k));
    }

    protected SortedMap<K, V> standardHeadMap(@ParametricNullness K k) {
        return headMap(k, false);
    }

    @CheckForNull
    protected Map.Entry<K, V> standardHigherEntry(@ParametricNullness K k) {
        return tailMap(k, false).firstEntry();
    }

    @CheckForNull
    protected K standardHigherKey(@ParametricNullness K k) {
        return (K) Maps.keyOrNull(higherEntry(k));
    }

    @CheckForNull
    protected Map.Entry<K, V> standardLastEntry() {
        return (Map.Entry) Iterables.getFirst(descendingMap().entrySet(), null);
    }

    protected K standardLastKey() {
        Map.Entry<K, V> entryLastEntry = lastEntry();
        if (entryLastEntry != null) {
            return entryLastEntry.getKey();
        }
        throw new NoSuchElementException();
    }

    @CheckForNull
    protected Map.Entry<K, V> standardLowerEntry(@ParametricNullness K k) {
        return headMap(k, false).lastEntry();
    }

    @CheckForNull
    protected K standardLowerKey(@ParametricNullness K k) {
        return (K) Maps.keyOrNull(lowerEntry(k));
    }

    @CheckForNull
    protected Map.Entry<K, V> standardPollFirstEntry() {
        return (Map.Entry) Iterators.pollNext(entrySet().iterator());
    }

    @CheckForNull
    protected Map.Entry<K, V> standardPollLastEntry() {
        return (Map.Entry) Iterators.pollNext(descendingMap().entrySet().iterator());
    }

    @Override // com.google.common.collect.ForwardingSortedMap
    protected SortedMap<K, V> standardSubMap(@ParametricNullness K k, @ParametricNullness K k2) {
        return subMap(k, true, k2, false);
    }

    protected SortedMap<K, V> standardTailMap(@ParametricNullness K k) {
        return tailMap(k, true);
    }

    @Override // java.util.NavigableMap
    public NavigableMap<K, V> subMap(@ParametricNullness K k, boolean z, @ParametricNullness K k2, boolean z2) {
        return delegate().subMap(k, z, k2, z2);
    }

    @Override // java.util.NavigableMap
    public NavigableMap<K, V> tailMap(@ParametricNullness K k, boolean z) {
        return delegate().tailMap(k, z);
    }
}
