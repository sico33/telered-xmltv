package com.google.common.collect;

import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface BiMap<K, V> extends Map<K, V> {

    /* JADX INFO: renamed from: com.google.common.collect.BiMap$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
    }

    @CheckForNull
    V forcePut(@ParametricNullness K k, @ParametricNullness V v);

    BiMap<V, K> inverse();

    @CheckForNull
    V put(@ParametricNullness K k, @ParametricNullness V v);

    void putAll(Map<? extends K, ? extends V> map);

    @Override // java.util.Map
    Set<V> values();
}
