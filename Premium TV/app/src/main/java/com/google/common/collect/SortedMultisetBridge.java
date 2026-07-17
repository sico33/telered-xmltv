package com.google.common.collect;

import java.util.SortedSet;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
interface SortedMultisetBridge<E> extends Multiset<E> {

    /* JADX INFO: renamed from: com.google.common.collect.SortedMultisetBridge$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
    }

    @Override // com.google.common.collect.Multiset
    SortedSet<E> elementSet();
}
