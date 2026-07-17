package com.google.common.graph;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class UndirectedGraphConnections<N, V> implements GraphConnections<N, V> {
    private final Map<N, V> adjacentNodeValues;

    /* JADX INFO: renamed from: com.google.common.graph.UndirectedGraphConnections$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final int[] $SwitchMap$com$google$common$graph$ElementOrder$Type = new int[ElementOrder.Type.values().length];

        static {
            try {
                $SwitchMap$com$google$common$graph$ElementOrder$Type[ElementOrder.Type.UNORDERED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$common$graph$ElementOrder$Type[ElementOrder.Type.STABLE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private UndirectedGraphConnections(Map<N, V> map) {
        this.adjacentNodeValues = (Map) Preconditions.checkNotNull(map);
    }

    static <N, V> UndirectedGraphConnections<N, V> of(ElementOrder<N> elementOrder) {
        switch (AnonymousClass1.$SwitchMap$com$google$common$graph$ElementOrder$Type[elementOrder.type().ordinal()]) {
            case 1:
                return new UndirectedGraphConnections<>(new HashMap(2, 1.0f));
            case 2:
                return new UndirectedGraphConnections<>(new LinkedHashMap(2, 1.0f));
            default:
                throw new AssertionError(elementOrder.type());
        }
    }

    static <N, V> UndirectedGraphConnections<N, V> ofImmutable(Map<N, V> map) {
        return new UndirectedGraphConnections<>(ImmutableMap.copyOf((Map) map));
    }

    @Override // com.google.common.graph.GraphConnections
    public void addPredecessor(N n, V v) {
        addSuccessor(n, v);
    }

    @Override // com.google.common.graph.GraphConnections
    @CheckForNull
    public V addSuccessor(N n, V v) {
        return this.adjacentNodeValues.put(n, v);
    }

    @Override // com.google.common.graph.GraphConnections
    public Set<N> adjacentNodes() {
        return Collections.unmodifiableSet(this.adjacentNodeValues.keySet());
    }

    @Override // com.google.common.graph.GraphConnections
    public Iterator<EndpointPair<N>> incidentEdgeIterator(final N n) {
        return Iterators.transform(this.adjacentNodeValues.keySet().iterator(), new Function(n) { // from class: com.google.common.graph.UndirectedGraphConnections$$ExternalSyntheticLambda0
            public final Object f$0;

            {
                this.f$0 = n;
            }

            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return EndpointPair.unordered(this.f$0, obj);
            }
        });
    }

    @Override // com.google.common.graph.GraphConnections
    public Set<N> predecessors() {
        return adjacentNodes();
    }

    @Override // com.google.common.graph.GraphConnections
    public void removePredecessor(N n) {
        removeSuccessor(n);
    }

    @Override // com.google.common.graph.GraphConnections
    @CheckForNull
    public V removeSuccessor(N n) {
        return this.adjacentNodeValues.remove(n);
    }

    @Override // com.google.common.graph.GraphConnections
    public Set<N> successors() {
        return adjacentNodes();
    }

    @Override // com.google.common.graph.GraphConnections
    @CheckForNull
    public V value(N n) {
        return this.adjacentNodeValues.get(n);
    }
}
