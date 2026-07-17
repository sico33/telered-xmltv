package com.google.common.collect;

import java.util.List;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class ForwardingListMultimap<K, V> extends ForwardingMultimap<K, V> implements ListMultimap<K, V> {
    protected ForwardingListMultimap() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.ForwardingObject
    public abstract ListMultimap<K, V> delegate();

    @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
    public List<V> get(@ParametricNullness K k) {
        return delegate().get((Object) k);
    }

    @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
    public List<V> removeAll(@CheckForNull Object obj) {
        return delegate().removeAll(obj);
    }

    @Override // com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap
    public List<V> replaceValues(@ParametricNullness K k, Iterable<? extends V> iterable) {
        return delegate().replaceValues((Object) k, (Iterable) iterable);
    }
}
