package com.google.common.collect;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
interface FilteredSetMultimap<K, V> extends FilteredMultimap<K, V>, SetMultimap<K, V> {

    /* JADX INFO: renamed from: com.google.common.collect.FilteredSetMultimap$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
    }

    @Override // com.google.common.collect.FilteredMultimap
    SetMultimap<K, V> unfiltered();
}
