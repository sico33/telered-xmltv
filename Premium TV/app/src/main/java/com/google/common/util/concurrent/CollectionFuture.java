package com.google.common.util.concurrent;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class CollectionFuture<V, C> extends AggregateFuture<V, C> {

    @CheckForNull
    private List<Present<V>> values;

    static final class ListFuture<V> extends CollectionFuture<V, List<V>> {
        ListFuture(ImmutableCollection<? extends ListenableFuture<? extends V>> immutableCollection, boolean z) {
            super(immutableCollection, z);
            init();
        }

        @Override // com.google.common.util.concurrent.CollectionFuture
        public List<V> combine(List<Present<V>> list) {
            ArrayList arrayListNewArrayListWithCapacity = Lists.newArrayListWithCapacity(list.size());
            Iterator<Present<V>> it = list.iterator();
            while (it.hasNext()) {
                Present<V> next = it.next();
                arrayListNewArrayListWithCapacity.add(next != null ? next.value : null);
            }
            return Collections.unmodifiableList(arrayListNewArrayListWithCapacity);
        }
    }

    private static final class Present<V> {

        @ParametricNullness
        final V value;

        Present(@ParametricNullness V v) {
            this.value = v;
        }
    }

    CollectionFuture(ImmutableCollection<? extends ListenableFuture<? extends V>> immutableCollection, boolean z) {
        super(immutableCollection, z, true);
        List<Present<V>> listEmptyList = immutableCollection.isEmpty() ? Collections.emptyList() : Lists.newArrayListWithCapacity(immutableCollection.size());
        for (int i = 0; i < immutableCollection.size(); i++) {
            listEmptyList.add(null);
        }
        this.values = listEmptyList;
    }

    @Override // com.google.common.util.concurrent.AggregateFuture
    final void collectOneValue(int i, @ParametricNullness V v) {
        List<Present<V>> list = this.values;
        if (list != null) {
            list.set(i, new Present<>(v));
        }
    }

    abstract C combine(List<Present<V>> list);

    @Override // com.google.common.util.concurrent.AggregateFuture
    final void handleAllCompleted() {
        List<Present<V>> list = this.values;
        if (list != null) {
            set(combine(list));
        }
    }

    @Override // com.google.common.util.concurrent.AggregateFuture
    void releaseResources(AggregateFuture.ReleaseResourcesReason releaseResourcesReason) {
        super.releaseResources(releaseResourcesReason);
        this.values = null;
    }
}
