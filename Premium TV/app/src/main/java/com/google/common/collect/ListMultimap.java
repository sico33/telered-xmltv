package com.google.common.collect;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface ListMultimap<K, V> extends Multimap<K, V> {

    /* JADX INFO: renamed from: com.google.common.collect.ListMultimap$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
    }

    Map<K, Collection<V>> asMap();

    boolean equals(@CheckForNull Object obj);

    @Override // com.google.common.collect.Multimap
    List<V> get(@ParametricNullness K k);

    @Override // com.google.common.collect.Multimap
    List<V> removeAll(@CheckForNull Object obj);

    @Override // com.google.common.collect.Multimap
    List<V> replaceValues(@ParametricNullness K k, Iterable<? extends V> iterable);
}
