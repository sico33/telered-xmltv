package com.google.common.graph;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.math.IntMath;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class AbstractNetwork<N, E> implements Network<N, E> {

    /* JADX INFO: renamed from: com.google.common.graph.AbstractNetwork$1, reason: invalid class name */
    class AnonymousClass1 extends AbstractGraph<N> {
        final AbstractNetwork this$0;

        /* JADX INFO: renamed from: com.google.common.graph.AbstractNetwork$1$1, reason: invalid class name and collision with other inner class name */
        class C00191 extends AbstractSet<EndpointPair<N>> {
            final AnonymousClass1 this$1;

            C00191(AnonymousClass1 anonymousClass1) {
                this.this$1 = anonymousClass1;
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                if (!(obj instanceof EndpointPair)) {
                    return false;
                }
                EndpointPair<?> endpointPair = (EndpointPair) obj;
                return this.this$1.isOrderingCompatible(endpointPair) && this.this$1.nodes().contains(endpointPair.nodeU()) && this.this$1.successors(endpointPair.nodeU()).contains(endpointPair.nodeV());
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<EndpointPair<N>> iterator() {
                return Iterators.transform(this.this$1.this$0.edges().iterator(), new Function(this) { // from class: com.google.common.graph.AbstractNetwork$1$1$$ExternalSyntheticLambda0
                    public final AbstractNetwork.AnonymousClass1.C00191 f$0;

                    {
                        this.f$0 = this;
                    }

                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return this.f$0.m196lambda$iterator$0$comgooglecommongraphAbstractNetwork$1$1(obj);
                    }
                });
            }

            /* JADX INFO: renamed from: lambda$iterator$0$com-google-common-graph-AbstractNetwork$1$1, reason: not valid java name */
            /* synthetic */ EndpointPair m196lambda$iterator$0$comgooglecommongraphAbstractNetwork$1$1(Object obj) {
                return this.this$1.this$0.incidentNodes(obj);
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return this.this$1.this$0.edges().size();
            }
        }

        AnonymousClass1(AbstractNetwork abstractNetwork) {
            this.this$0 = abstractNetwork;
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
        public Set<EndpointPair<N>> edges() {
            return this.this$0.allowsParallelEdges() ? super.edges() : new C00191(this);
        }

        @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.BaseGraph
        public ElementOrder<N> incidentEdgeOrder() {
            return ElementOrder.unordered();
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

        @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.PredecessorsFunction
        public Set<N> predecessors(N n) {
            return this.this$0.predecessors((Object) n);
        }

        @Override // com.google.common.graph.AbstractGraph, com.google.common.graph.AbstractBaseGraph, com.google.common.graph.SuccessorsFunction
        public Set<N> successors(N n) {
            return this.this$0.successors((Object) n);
        }
    }

    private Predicate<E> connectedPredicate(N n, N n2) {
        return new Predicate<E>(this, n, n2) { // from class: com.google.common.graph.AbstractNetwork.2
            final AbstractNetwork this$0;
            final Object val$nodePresent;
            final Object val$nodeToCheck;

            {
                this.this$0 = this;
                this.val$nodePresent = n;
                this.val$nodeToCheck = n2;
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // com.google.common.base.Predicate
            public boolean apply(E e) {
                return this.this$0.incidentNodes(e).adjacentNode(this.val$nodePresent).equals(this.val$nodeToCheck);
            }
        };
    }

    private static <N, E> Map<E, EndpointPair<N>> edgeIncidentNodesMap(final Network<N, E> network) {
        Set<E> setEdges = network.edges();
        Objects.requireNonNull(network);
        return Maps.asMap(setEdges, new Function(network) { // from class: com.google.common.graph.AbstractNetwork$$ExternalSyntheticLambda0
            public final Network f$0;

            {
                this.f$0 = network;
            }

            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return this.f$0.incidentNodes(obj);
            }
        });
    }

    @Override // com.google.common.graph.Network
    public Set<E> adjacentEdges(E e) {
        EndpointPair<N> endpointPairIncidentNodes = incidentNodes(e);
        return Sets.difference(Sets.union(incidentEdges(endpointPairIncidentNodes.nodeU()), incidentEdges(endpointPairIncidentNodes.nodeV())), ImmutableSet.of((Object) e));
    }

    @Override // com.google.common.graph.Network
    public Graph<N> asGraph() {
        return new AnonymousClass1(this);
    }

    @Override // com.google.common.graph.Network
    public int degree(N n) {
        return isDirected() ? IntMath.saturatedAdd(inEdges(n).size(), outEdges(n).size()) : IntMath.saturatedAdd(incidentEdges(n).size(), edgesConnecting(n, n).size());
    }

    @Override // com.google.common.graph.Network
    @CheckForNull
    public E edgeConnectingOrNull(EndpointPair<N> endpointPair) {
        validateEndpoints(endpointPair);
        return edgeConnectingOrNull(endpointPair.nodeU(), endpointPair.nodeV());
    }

    @Override // com.google.common.graph.Network
    @CheckForNull
    public E edgeConnectingOrNull(N n, N n2) {
        Set<E> setEdgesConnecting = edgesConnecting(n, n2);
        switch (setEdgesConnecting.size()) {
            case 0:
                return null;
            case 1:
                return setEdgesConnecting.iterator().next();
            default:
                throw new IllegalArgumentException(String.format("Cannot call edgeConnecting() when parallel edges exist between %s and %s. Consider calling edgesConnecting() instead.", n, n2));
        }
    }

    @Override // com.google.common.graph.Network
    public Set<E> edgesConnecting(EndpointPair<N> endpointPair) {
        validateEndpoints(endpointPair);
        return edgesConnecting(endpointPair.nodeU(), endpointPair.nodeV());
    }

    @Override // com.google.common.graph.Network
    public Set<E> edgesConnecting(N n, N n2) {
        Set<E> setOutEdges = outEdges(n);
        Set<E> setInEdges = inEdges(n2);
        return setOutEdges.size() <= setInEdges.size() ? Collections.unmodifiableSet(Sets.filter(setOutEdges, connectedPredicate(n, n2))) : Collections.unmodifiableSet(Sets.filter(setInEdges, connectedPredicate(n2, n)));
    }

    @Override // com.google.common.graph.Network
    public final boolean equals(@CheckForNull Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Network)) {
            return false;
        }
        Network network = (Network) obj;
        return isDirected() == network.isDirected() && nodes().equals(network.nodes()) && edgeIncidentNodesMap(this).equals(edgeIncidentNodesMap(network));
    }

    @Override // com.google.common.graph.Network
    public boolean hasEdgeConnecting(EndpointPair<N> endpointPair) {
        Preconditions.checkNotNull(endpointPair);
        if (isOrderingCompatible(endpointPair)) {
            return hasEdgeConnecting(endpointPair.nodeU(), endpointPair.nodeV());
        }
        return false;
    }

    @Override // com.google.common.graph.Network
    public boolean hasEdgeConnecting(N n, N n2) {
        Preconditions.checkNotNull(n);
        Preconditions.checkNotNull(n2);
        return nodes().contains(n) && successors((Object) n).contains(n2);
    }

    @Override // com.google.common.graph.Network
    public final int hashCode() {
        return edgeIncidentNodesMap(this).hashCode();
    }

    @Override // com.google.common.graph.Network
    public int inDegree(N n) {
        return isDirected() ? inEdges(n).size() : degree(n);
    }

    protected final boolean isOrderingCompatible(EndpointPair<?> endpointPair) {
        return endpointPair.isOrdered() == isDirected();
    }

    @Override // com.google.common.graph.Network
    public int outDegree(N n) {
        return isDirected() ? outEdges(n).size() : degree(n);
    }

    @Override // com.google.common.graph.PredecessorsFunction
    public /* bridge */ /* synthetic */ Iterable predecessors(Object obj) {
        return predecessors(obj);
    }

    @Override // com.google.common.graph.SuccessorsFunction
    public /* bridge */ /* synthetic */ Iterable successors(Object obj) {
        return successors(obj);
    }

    public String toString() {
        return "isDirected: " + isDirected() + ", allowsParallelEdges: " + allowsParallelEdges() + ", allowsSelfLoops: " + allowsSelfLoops() + ", nodes: " + nodes() + ", edges: " + edgeIncidentNodesMap(this);
    }

    protected final void validateEndpoints(EndpointPair<?> endpointPair) {
        Preconditions.checkNotNull(endpointPair);
        Preconditions.checkArgument(isOrderingCompatible(endpointPair), "Mismatch: endpoints' ordering is not compatible with directionality of the graph");
    }
}
