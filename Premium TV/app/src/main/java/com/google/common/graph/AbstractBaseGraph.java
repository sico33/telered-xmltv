package com.google.common.graph;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class AbstractBaseGraph<N> implements BaseGraph<N> {

    /* JADX INFO: renamed from: com.google.common.graph.AbstractBaseGraph$2, reason: invalid class name */
    class AnonymousClass2 extends IncidentEdgeSet<N> {
        AnonymousClass2(AbstractBaseGraph abstractBaseGraph, BaseGraph baseGraph, Object obj) {
            super(baseGraph, obj);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public UnmodifiableIterator<EndpointPair<N>> iterator() {
            return this.graph.isDirected() ? Iterators.unmodifiableIterator(Iterators.concat(Iterators.transform(this.graph.predecessors((Object) this.node).iterator(), new Function(this) { // from class: com.google.common.graph.AbstractBaseGraph$2$$ExternalSyntheticLambda0
                public final AbstractBaseGraph.AnonymousClass2 f$0;

                {
                    this.f$0 = this;
                }

                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return this.f$0.m193lambda$iterator$0$comgooglecommongraphAbstractBaseGraph$2(obj);
                }
            }), Iterators.transform(Sets.difference(this.graph.successors((Object) this.node), ImmutableSet.of(this.node)).iterator(), new Function(this) { // from class: com.google.common.graph.AbstractBaseGraph$2$$ExternalSyntheticLambda1
                public final AbstractBaseGraph.AnonymousClass2 f$0;

                {
                    this.f$0 = this;
                }

                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return this.f$0.m194lambda$iterator$1$comgooglecommongraphAbstractBaseGraph$2(obj);
                }
            }))) : Iterators.unmodifiableIterator(Iterators.transform(this.graph.adjacentNodes(this.node).iterator(), new Function(this) { // from class: com.google.common.graph.AbstractBaseGraph$2$$ExternalSyntheticLambda2
                public final AbstractBaseGraph.AnonymousClass2 f$0;

                {
                    this.f$0 = this;
                }

                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return this.f$0.m195lambda$iterator$2$comgooglecommongraphAbstractBaseGraph$2(obj);
                }
            }));
        }

        /* JADX INFO: renamed from: lambda$iterator$0$com-google-common-graph-AbstractBaseGraph$2, reason: not valid java name */
        /* synthetic */ EndpointPair m193lambda$iterator$0$comgooglecommongraphAbstractBaseGraph$2(Object obj) {
            return EndpointPair.ordered(obj, this.node);
        }

        /* JADX INFO: renamed from: lambda$iterator$1$com-google-common-graph-AbstractBaseGraph$2, reason: not valid java name */
        /* synthetic */ EndpointPair m194lambda$iterator$1$comgooglecommongraphAbstractBaseGraph$2(Object obj) {
            return EndpointPair.ordered(this.node, obj);
        }

        /* JADX INFO: renamed from: lambda$iterator$2$com-google-common-graph-AbstractBaseGraph$2, reason: not valid java name */
        /* synthetic */ EndpointPair m195lambda$iterator$2$comgooglecommongraphAbstractBaseGraph$2(Object obj) {
            return EndpointPair.unordered(this.node, obj);
        }
    }

    AbstractBaseGraph() {
    }

    @Override // com.google.common.graph.BaseGraph
    public int degree(N n) {
        if (isDirected()) {
            return IntMath.saturatedAdd(predecessors((Object) n).size(), successors((Object) n).size());
        }
        Set<N> setAdjacentNodes = adjacentNodes(n);
        return IntMath.saturatedAdd(setAdjacentNodes.size(), (allowsSelfLoops() && setAdjacentNodes.contains(n)) ? 1 : 0);
    }

    protected long edgeCount() {
        Iterator<N> it = nodes().iterator();
        long jDegree = 0;
        while (it.hasNext()) {
            jDegree += (long) degree(it.next());
        }
        Preconditions.checkState((1 & jDegree) == 0);
        return jDegree >>> 1;
    }

    @Override // com.google.common.graph.BaseGraph
    public Set<EndpointPair<N>> edges() {
        return new AbstractSet<EndpointPair<N>>(this) { // from class: com.google.common.graph.AbstractBaseGraph.1
            final AbstractBaseGraph this$0;

            {
                this.this$0 = this;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                if (!(obj instanceof EndpointPair)) {
                    return false;
                }
                EndpointPair<?> endpointPair = (EndpointPair) obj;
                return this.this$0.isOrderingCompatible(endpointPair) && this.this$0.nodes().contains(endpointPair.nodeU()) && this.this$0.successors(endpointPair.nodeU()).contains(endpointPair.nodeV());
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public UnmodifiableIterator<EndpointPair<N>> iterator() {
                return EndpointPairIterator.of(this.this$0);
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean remove(@CheckForNull Object obj) {
                throw new UnsupportedOperationException();
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return Ints.saturatedCast(this.this$0.edgeCount());
            }
        };
    }

    @Override // com.google.common.graph.BaseGraph
    public boolean hasEdgeConnecting(EndpointPair<N> endpointPair) {
        Preconditions.checkNotNull(endpointPair);
        if (!isOrderingCompatible(endpointPair)) {
            return false;
        }
        N nNodeU = endpointPair.nodeU();
        return nodes().contains(nNodeU) && successors((Object) nNodeU).contains(endpointPair.nodeV());
    }

    @Override // com.google.common.graph.BaseGraph
    public boolean hasEdgeConnecting(N n, N n2) {
        Preconditions.checkNotNull(n);
        Preconditions.checkNotNull(n2);
        return nodes().contains(n) && successors((Object) n).contains(n2);
    }

    @Override // com.google.common.graph.BaseGraph
    public int inDegree(N n) {
        return isDirected() ? predecessors((Object) n).size() : degree(n);
    }

    @Override // com.google.common.graph.BaseGraph
    public ElementOrder<N> incidentEdgeOrder() {
        return ElementOrder.unordered();
    }

    @Override // com.google.common.graph.BaseGraph
    public Set<EndpointPair<N>> incidentEdges(N n) {
        Preconditions.checkNotNull(n);
        Preconditions.checkArgument(nodes().contains(n), "Node %s is not an element of this graph.", n);
        return new AnonymousClass2(this, this, n);
    }

    protected final boolean isOrderingCompatible(EndpointPair<?> endpointPair) {
        return endpointPair.isOrdered() == isDirected();
    }

    @Override // com.google.common.graph.BaseGraph
    public int outDegree(N n) {
        return isDirected() ? successors((Object) n).size() : degree(n);
    }

    @Override // com.google.common.graph.PredecessorsFunction
    public /* bridge */ /* synthetic */ Iterable predecessors(Object obj) {
        return predecessors(obj);
    }

    @Override // com.google.common.graph.SuccessorsFunction
    public /* bridge */ /* synthetic */ Iterable successors(Object obj) {
        return successors(obj);
    }

    protected final void validateEndpoints(EndpointPair<?> endpointPair) {
        Preconditions.checkNotNull(endpointPair);
        Preconditions.checkArgument(isOrderingCompatible(endpointPair), "Mismatch: endpoints' ordering is not compatible with directionality of the graph");
    }
}
