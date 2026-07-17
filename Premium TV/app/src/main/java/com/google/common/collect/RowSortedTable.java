package com.google.common.collect;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface RowSortedTable<R, C, V> extends Table<R, C, V> {

    /* JADX INFO: renamed from: com.google.common.collect.RowSortedTable$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
    }

    @Override // com.google.common.collect.Table
    SortedSet<R> rowKeySet();

    @Override // com.google.common.collect.Table
    SortedMap<R, Map<C, V>> rowMap();
}
