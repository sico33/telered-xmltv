package com.google.common.graph;

import com.google.common.base.Preconditions;
import com.google.common.collect.UnmodifiableIterator;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
class MapIteratorCache<K, V> {
    private final Map<K, V> backingMap;

    @CheckForNull
    private volatile transient Map.Entry<K, V> cacheEntry;

    /* JADX INFO: renamed from: com.google.common.graph.MapIteratorCache$1, reason: invalid class name */
    class AnonymousClass1 extends AbstractSet<K> {
        final MapIteratorCache this$0;

        AnonymousClass1(MapIteratorCache mapIteratorCache) {
            this.this$0 = mapIteratorCache;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            return this.this$0.containsKey(obj);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public UnmodifiableIterator<K> iterator() {
            return new UnmodifiableIterator<K>(this, this.this$0.backingMap.entrySet().iterator()) { // from class: com.google.common.graph.MapIteratorCache.1.1
                final AnonymousClass1 this$1;
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
                public K next() {
                    Map.Entry entry = (Map.Entry) this.val$entryIterator.next();
                    this.this$1.this$0.cacheEntry = entry;
                    return (K) entry.getKey();
                }
            };
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.this$0.backingMap.size();
        }
    }

    MapIteratorCache(Map<K, V> map) {
        this.backingMap = (Map) Preconditions.checkNotNull(map);
    }

    final void clear() {
        clearCache();
        this.backingMap.clear();
    }

    void clearCache() {
        this.cacheEntry = null;
    }

    final boolean containsKey(@CheckForNull Object obj) {
        return getIfCached(obj) != null || this.backingMap.containsKey(obj);
    }

    @CheckForNull
    V get(Object obj) {
        Preconditions.checkNotNull(obj);
        V ifCached = getIfCached(obj);
        return ifCached == null ? getWithoutCaching(obj) : ifCached;
    }

    @CheckForNull
    V getIfCached(@CheckForNull Object obj) {
        Map.Entry<K, V> entry = this.cacheEntry;
        if (entry == null || entry.getKey() != obj) {
            return null;
        }
        return entry.getValue();
    }

    @CheckForNull
    final V getWithoutCaching(Object obj) {
        Preconditions.checkNotNull(obj);
        return this.backingMap.get(obj);
    }

    @CheckForNull
    final V put(K k, V v) {
        Preconditions.checkNotNull(k);
        Preconditions.checkNotNull(v);
        clearCache();
        return this.backingMap.put(k, v);
    }

    @CheckForNull
    final V remove(Object obj) {
        Preconditions.checkNotNull(obj);
        clearCache();
        return this.backingMap.remove(obj);
    }

    final Set<K> unmodifiableKeySet() {
        return new AnonymousClass1(this);
    }
}
