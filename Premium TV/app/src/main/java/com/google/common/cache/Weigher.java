package com.google.common.cache;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface Weigher<K, V> {
    int weigh(K k, V v);
}
