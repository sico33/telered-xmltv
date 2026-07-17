package com.google.common.graph;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.UnmodifiableIterator;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class MultiEdgesConnecting<E> extends AbstractSet<E> {
    private final Map<E, ?> outEdgeToNode;
    private final Object targetNode;

    MultiEdgesConnecting(Map<E, ?> map, Object obj) {
        this.outEdgeToNode = (Map) Preconditions.checkNotNull(map);
        this.targetNode = Preconditions.checkNotNull(obj);
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean contains(@CheckForNull Object obj) {
        return this.targetNode.equals(this.outEdgeToNode.get(obj));
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
    public UnmodifiableIterator<E> iterator() {
        return new AbstractIterator<E>(this, this.outEdgeToNode.entrySet().iterator()) { // from class: com.google.common.graph.MultiEdgesConnecting.1
            final MultiEdgesConnecting this$0;
            final Iterator val$entries;

            {
                this.this$0 = this;
                this.val$entries = it;
            }

            @Override // com.google.common.collect.AbstractIterator
            @CheckForNull
            protected E computeNext() {
                while (this.val$entries.hasNext()) {
                    Map.Entry entry = (Map.Entry) this.val$entries.next();
                    if (this.this$0.targetNode.equals(entry.getValue())) {
                        return (E) entry.getKey();
                    }
                }
                return endOfData();
            }
        };
    }
}
