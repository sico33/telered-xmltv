package com.google.common.cache;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface RemovalListener<K, V> {
    void onRemoval(RemovalNotification<K, V> removalNotification);
}
