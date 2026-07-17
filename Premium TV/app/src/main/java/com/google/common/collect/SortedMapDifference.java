package com.google.common.collect;

import java.util.SortedMap;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface SortedMapDifference<K, V> extends MapDifference<K, V> {

    /* JADX INFO: renamed from: com.google.common.collect.SortedMapDifference$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
    }

    @Override // com.google.common.collect.MapDifference
    SortedMap<K, MapDifference.ValueDifference<V>> entriesDiffering();

    @Override // com.google.common.collect.MapDifference
    SortedMap<K, V> entriesInCommon();

    @Override // com.google.common.collect.MapDifference
    SortedMap<K, V> entriesOnlyOnLeft();

    @Override // com.google.common.collect.MapDifference
    SortedMap<K, V> entriesOnlyOnRight();
}
