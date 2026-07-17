package com.google.common.graph;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class AbstractValueGraph<N, V> extends AbstractBaseGraph<N> implements ValueGraph<N, V> {
    private static <N, V> Map<EndpointPair<N>, V> edgeValueMap(final ValueGraph<N, V> valueGraph) {
        return Maps.asMap(valueGraph.edges(), new Function(valueGraph) { // from class: com.google.common.graph.AbstractValueGraph$$ExternalSyntheticLambda0
            public final ValueGraph f$0;

            {
                this.f$0 = valueGraph;
            }

            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                EndpointPair endpointPair = (EndpointPair) obj;
                return Objects.requireNonNull(this.f$0.edgeValueOrDefault(endpointPair.nodeU(), endpointPair.nodeV(), null));
            }
        });
    }

    @Override // com.google.common.graph.ValueGraph
    public Graph<N> asGraph() {
        return new AbstractGraph<N>(this) { // from class: com.google.common.graph.AbstractValueGraph.1
            final AbstractValueGraph this$0;

            {
                this.this$0 = this;
            }

            @Override // com.google.common.graph.BaseGraph, com.google.common.graph.Graph
            public Set<N> adjacentNodes(N n) {
                return this.this$0.adjacentNodes(n);
            }

            @Override // com.google.common.graph.BaseGraph, com.google.common.graph.Graph
            public boolean allowsSelfLoops() {
                return this.this$0.allowsSelfLoops();
            }

            @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
            public int degree(N n) {
                return this.this$0.degree(n);
            }

            @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
            public Set<EndpointPair<N>> edges() {
                return this.this$0.edges();
            }

            @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
            public int inDegree(N n) {
                return this.this$0.inDegree(n);
            }

            @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
            public ElementOrder<N> incidentEdgeOrder() {
                return this.this$0.incidentEdgeOrder();
            }

            @Override // com.google.common.graph.BaseGraph, com.google.common.graph.Graph
            public boolean isDirected() {
                return this.this$0.isDirected();
            }

            @Override // com.google.common.graph.BaseGraph, com.google.common.graph.Graph
            public ElementOrder<N> nodeOrder() {
                return this.this$0.nodeOrder();
            }

            @Override // com.google.common.graph.BaseGraph, com.google.common.graph.Graph
            public Set<N> nodes() {
                return this.this$0.nodes();
            }

            @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
            public int outDegree(N n) {
                return this.this$0.outDegree(n);
            }

            @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.PredecessorsFunction
            public Set<N> predecessors(N n) {
                return this.this$0.predecessors((Object) n);
            }

            @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.SuccessorsFunction
            public Set<N> successors(N n) {
                return this.this$0.successors((Object) n);
            }
        };
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
    public /* bridge */ /* synthetic */ int degree(Object obj) {
        return super.degree(obj);
    }

    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
    public /* bridge */ /* synthetic */ Set edges() {
        return super.edges();
    }

    @Override // com.google.common.graph.ValueGraph
    public final boolean equals(@CheckForNull Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ValueGraph)) {
            return false;
        }
        ValueGraph valueGraph = (ValueGraph) obj;
        return isDirected() == valueGraph.isDirected() && nodes().equals(valueGraph.nodes()) && edgeValueMap(this).equals(edgeValueMap(valueGraph));
    }

    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
    public /* bridge */ /* synthetic */ boolean hasEdgeConnecting(EndpointPair endpointPair) {
        return super.hasEdgeConnecting(endpointPair);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
    public /* bridge */ /* synthetic */ boolean hasEdgeConnecting(Object obj, Object obj2) {
        return super.hasEdgeConnecting(obj, obj2);
    }

    @Override // com.google.common.graph.ValueGraph
    public final int hashCode() {
        return edgeValueMap(this).hashCode();
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
    public /* bridge */ /* synthetic */ int inDegree(Object obj) {
        return super.inDegree(obj);
    }

    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
    public /* bridge */ /* synthetic */ ElementOrder incidentEdgeOrder() {
        return super.incidentEdgeOrder();
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
    public /* bridge */ /* synthetic */ Set incidentEdges(Object obj) {
        return super.incidentEdges(obj);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
    public /* bridge */ /* synthetic */ int outDegree(Object obj) {
        return super.outDegree(obj);
    }

    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.PredecessorsFunction
    public /* bridge */ /* synthetic */ Iterable predecessors(Object obj) {
        return predecessors(obj);
    }

    @Override // com.google.common.graph.AbstractBaseGraph, com.google.common.graph.SuccessorsFunction
    public /* bridge */ /* synthetic */ Iterable successors(Object obj) {
        return successors(obj);
    }

    public String toString() {
        return "isDirected: " + isDirected() + ", allowsSelfLoops: " + allowsSelfLoops() + ", nodes: " + nodes() + ", edges: " + edgeValueMap(this);
    }
}
