package com.google.common.collect;

import com.google.common.base.Predicate;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class FilteredKeySetMultimap<K, V> extends FilteredKeyMultimap<K, V> implements FilteredSetMultimap<K, V> {

    class EntrySet extends FilteredKeyMultimap<K, V>.Entries implements Set<Map.Entry<K, V>> {
        EntrySet(FilteredKeySetMultimap filteredKeySetMultimap) {
            super(filteredKeySetMultimap);
        }

        @Override // java.util.Collection, java.util.Set
        public boolean equals(@CheckForNull Object obj) {
            return Sets.equalsImpl(this, obj);
        }

        @Override // java.util.Collection, java.util.Set
        public int hashCode() {
            return Sets.hashCodeImpl(this);
        }
    }

    FilteredKeySetMultimap(SetMultimap<K, V> setMultimap, Predicate<? super K> predicate) {
        super(setMultimap, predicate);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.google.common.collect.FilteredKeyMultimap, com.google.common.collect.AbstractMultimap
    public Set<Map.Entry<K, V>> createEntries() {
        return new EntrySet(this);
    }

    @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
    public Set<Map.Entry<K, V>> entries() {
        return (Set) super.entries();
    }

    @Override // com.google.common.collect.FilteredKeyMultimap, com.google.common.collect.Multimap
    public Set<V> get(@ParametricNullness K k) {
        return (Set) super.get((Object) k);
    }

    @Override // com.google.common.collect.FilteredKeyMultimap, com.google.common.collect.Multimap
    public Set<V> removeAll(@CheckForNull Object obj) {
        return (Set) super.removeAll(obj);
    }

    @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
    public Set<V> replaceValues(@ParametricNullness K k, Iterable<? extends V> iterable) {
        return (Set) super.replaceValues((Object) k, (Iterable) iterable);
    }

    @Override // com.google.common.collect.FilteredKeyMultimap, com.google.common.collect.FilteredMultimap
    public SetMultimap<K, V> unfiltered() {
        return (SetMultimap) this.unfiltered;
    }
}
