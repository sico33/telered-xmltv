package com.google.common.graph;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class DirectedGraphConnections<N, V> implements GraphConnections<N, V> {
    private static final Object PRED = new Object();
    private final Map<N, Object> adjacentNodeValues;

    @CheckForNull
    private final List<NodeConnection<N>> orderedNodeConnections;
    private int predecessorCount;
    private int successorCount;

    /* JADX INFO: renamed from: com.google.common.graph.DirectedGraphConnections$5, reason: invalid class name */
    static /* synthetic */ class AnonymousClass5 {
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

    /* JADX INFO: Access modifiers changed from: private */
    static abstract class NodeConnection<N> {
        final N node;

        static final class Pred<N> extends NodeConnection<N> {
            Pred(N n) {
                super(n);
            }

            public boolean equals(@CheckForNull Object obj) {
                if (obj instanceof Pred) {
                    return this.node.equals(((Pred) obj).node);
                }
                return false;
            }

            public int hashCode() {
                return Pred.class.hashCode() + this.node.hashCode();
            }
        }

        static final class Succ<N> extends NodeConnection<N> {
            Succ(N n) {
                super(n);
            }

            public boolean equals(@CheckForNull Object obj) {
                if (obj instanceof Succ) {
                    return this.node.equals(((Succ) obj).node);
                }
                return false;
            }

            public int hashCode() {
                return Succ.class.hashCode() + this.node.hashCode();
            }
        }

        NodeConnection(N n) {
            this.node = (N) Preconditions.checkNotNull(n);
        }
    }

    private static final class PredAndSucc {
        private final Object successorValue;

        PredAndSucc(Object obj) {
            this.successorValue = obj;
        }
    }

    private DirectedGraphConnections(Map<N, Object> map, @CheckForNull List<NodeConnection<N>> list, int i, int i2) {
        this.adjacentNodeValues = (Map) Preconditions.checkNotNull(map);
        this.orderedNodeConnections = list;
        this.predecessorCount = Graphs.checkNonNegative(i);
        this.successorCount = Graphs.checkNonNegative(i2);
        Preconditions.checkState(i <= map.size() && i2 <= map.size());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isPredecessor(@CheckForNull Object obj) {
        return obj == PRED || (obj instanceof PredAndSucc);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isSuccessor(@CheckForNull Object obj) {
        return (obj == PRED || obj == null) ? false : true;
    }

    static /* synthetic */ EndpointPair lambda$incidentEdgeIterator$2(Object obj, NodeConnection nodeConnection) {
        return nodeConnection instanceof NodeConnection.Succ ? EndpointPair.ordered(obj, nodeConnection.node) : EndpointPair.ordered(nodeConnection.node, obj);
    }

    static <N, V> DirectedGraphConnections<N, V> of(ElementOrder<N> elementOrder) {
        ArrayList arrayList;
        switch (AnonymousClass5.$SwitchMap$com$google$common$graph$ElementOrder$Type[elementOrder.type().ordinal()]) {
            case 1:
                arrayList = null;
                break;
            case 2:
                arrayList = new ArrayList();
                break;
            default:
                throw new AssertionError(elementOrder.type());
        }
        return new DirectedGraphConnections<>(new HashMap(4, 1.0f), arrayList, 0, 0);
    }

    /* JADX WARN: Multi-variable type inference failed */
    static <N, V> DirectedGraphConnections<N, V> ofImmutable(N n, Iterable<EndpointPair<N>> iterable, Function<N, V> function) {
        int i;
        Preconditions.checkNotNull(n);
        Preconditions.checkNotNull(function);
        HashMap map = new HashMap();
        ImmutableList.Builder builder = ImmutableList.builder();
        int i2 = 0;
        int i3 = 0;
        for (EndpointPair<N> endpointPair : iterable) {
            if (endpointPair.nodeU().equals(n) && endpointPair.nodeV().equals(n)) {
                map.put(n, new PredAndSucc(function.apply(n)));
                builder.add(new NodeConnection.Pred(n));
                builder.add(new NodeConnection.Succ(n));
                i3++;
                i = i2 + 1;
            } else if (endpointPair.nodeV().equals(n)) {
                N nNodeU = endpointPair.nodeU();
                Object objPut = map.put(nNodeU, PRED);
                if (objPut != null) {
                    map.put(nNodeU, new PredAndSucc(objPut));
                }
                builder.add(new NodeConnection.Pred(nNodeU));
                i3++;
                i = i2;
            } else {
                Preconditions.checkArgument(endpointPair.nodeU().equals(n));
                N nNodeV = endpointPair.nodeV();
                V vApply = function.apply(nNodeV);
                Object objPut2 = map.put(nNodeV, vApply);
                if (objPut2 != null) {
                    Preconditions.checkArgument(objPut2 == PRED);
                    map.put(nNodeV, new PredAndSucc(vApply));
                }
                builder.add(new NodeConnection.Succ(nNodeV));
                i = i2 + 1;
            }
            i3 = i3;
            i2 = i;
        }
        return new DirectedGraphConnections<>(map, builder.build(), i3, i2);
    }

    @Override // com.google.common.graph.GraphConnections
    public void addPredecessor(N n, V v) {
        boolean z = true;
        Object objPut = this.adjacentNodeValues.put(n, PRED);
        if (objPut != null) {
            if (objPut instanceof PredAndSucc) {
                this.adjacentNodeValues.put(n, objPut);
                z = false;
            } else if (objPut != PRED) {
                this.adjacentNodeValues.put(n, new PredAndSucc(objPut));
            } else {
                z = false;
            }
        }
        if (z) {
            int i = this.predecessorCount + 1;
            this.predecessorCount = i;
            Graphs.checkPositive(i);
            if (this.orderedNodeConnections != null) {
                this.orderedNodeConnections.add(new NodeConnection.Pred(n));
            }
        }
    }

    @Override // com.google.common.graph.GraphConnections
    @CheckForNull
    public V addSuccessor(N n, V v) {
        Object obj = (V) this.adjacentNodeValues.put(n, v);
        if (obj == null) {
            obj = (V) null;
        } else if (obj instanceof PredAndSucc) {
            this.adjacentNodeValues.put(n, new PredAndSucc(v));
            obj = (V) ((PredAndSucc) obj).successorValue;
        } else if (obj == PRED) {
            this.adjacentNodeValues.put(n, new PredAndSucc(v));
            obj = (V) null;
        }
        if (obj == null) {
            int i = this.successorCount + 1;
            this.successorCount = i;
            Graphs.checkPositive(i);
            if (this.orderedNodeConnections != null) {
                this.orderedNodeConnections.add(new NodeConnection.Succ(n));
            }
        }
        if (obj == null) {
            return null;
        }
        return (V) obj;
    }

    @Override // com.google.common.graph.GraphConnections
    public Set<N> adjacentNodes() {
        return this.orderedNodeConnections == null ? Collections.unmodifiableSet(this.adjacentNodeValues.keySet()) : new AbstractSet<N>(this) { // from class: com.google.common.graph.DirectedGraphConnections.1
            final DirectedGraphConnections this$0;

            {
                this.this$0 = this;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                return this.this$0.adjacentNodeValues.containsKey(obj);
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public UnmodifiableIterator<N> iterator() {
                return new AbstractIterator<N>(this, this.this$0.orderedNodeConnections.iterator(), new HashSet()) { // from class: com.google.common.graph.DirectedGraphConnections.1.1
                    final Iterator val$nodeConnections;
                    final Set val$seenNodes;

                    {
                        this.val$nodeConnections = it;
                        this.val$seenNodes = set;
                    }

                    @Override // com.google.common.collect.AbstractIterator
                    @CheckForNull
                    protected N computeNext() {
                        while (this.val$nodeConnections.hasNext()) {
                            NodeConnection nodeConnection = (NodeConnection) this.val$nodeConnections.next();
                            if (this.val$seenNodes.add(nodeConnection.node)) {
                                return nodeConnection.node;
                            }
                        }
                        return endOfData();
                    }
                };
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return this.this$0.adjacentNodeValues.size();
            }
        };
    }

    @Override // com.google.common.graph.GraphConnections
    public Iterator<EndpointPair<N>> incidentEdgeIterator(final N n) {
        Preconditions.checkNotNull(n);
        return new AbstractIterator<EndpointPair<N>>(this, this.orderedNodeConnections == null ? Iterators.concat(Iterators.transform(predecessors().iterator(), new Function(n) { // from class: com.google.common.graph.DirectedGraphConnections$$ExternalSyntheticLambda0
            public final Object f$0;

            {
                this.f$0 = n;
            }

            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return EndpointPair.ordered(obj, this.f$0);
            }
        }), Iterators.transform(successors().iterator(), new Function(n) { // from class: com.google.common.graph.DirectedGraphConnections$$ExternalSyntheticLambda1
            public final Object f$0;

            {
                this.f$0 = n;
            }

            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return EndpointPair.ordered(this.f$0, obj);
            }
        })) : Iterators.transform(this.orderedNodeConnections.iterator(), new Function(n) { // from class: com.google.common.graph.DirectedGraphConnections$$ExternalSyntheticLambda2
            public final Object f$0;

            {
                this.f$0 = n;
            }

            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return DirectedGraphConnections.lambda$incidentEdgeIterator$2(this.f$0, (DirectedGraphConnections.NodeConnection) obj);
            }
        }), new AtomicBoolean(false)) { // from class: com.google.common.graph.DirectedGraphConnections.4
            final AtomicBoolean val$alreadySeenSelfLoop;
            final Iterator val$resultWithDoubleSelfLoop;

            {
                this.val$resultWithDoubleSelfLoop = it;
                this.val$alreadySeenSelfLoop = atomicBoolean;
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // com.google.common.collect.AbstractIterator
            @CheckForNull
            public EndpointPair<N> computeNext() {
                while (this.val$resultWithDoubleSelfLoop.hasNext()) {
                    EndpointPair<N> endpointPair = (EndpointPair) this.val$resultWithDoubleSelfLoop.next();
                    if (!endpointPair.nodeU().equals(endpointPair.nodeV()) || !this.val$alreadySeenSelfLoop.getAndSet(true)) {
                        return endpointPair;
                    }
                }
                return endOfData();
            }
        };
    }

    @Override // com.google.common.graph.GraphConnections
    public Set<N> predecessors() {
        return new AbstractSet<N>(this) { // from class: com.google.common.graph.DirectedGraphConnections.2
            final DirectedGraphConnections this$0;

            {
                this.this$0 = this;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                return DirectedGraphConnections.isPredecessor(this.this$0.adjacentNodeValues.get(obj));
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public UnmodifiableIterator<N> iterator() {
                List list = this.this$0.orderedNodeConnections;
                DirectedGraphConnections directedGraphConnections = this.this$0;
                return list == null ? new AbstractIterator<N>(this, directedGraphConnections.adjacentNodeValues.entrySet().iterator()) { // from class: com.google.common.graph.DirectedGraphConnections.2.1
                    final Iterator val$entries;

                    {
                        this.val$entries = it;
                    }

                    @Override // com.google.common.collect.AbstractIterator
                    @CheckForNull
                    protected N computeNext() {
                        while (this.val$entries.hasNext()) {
                            Map.Entry entry = (Map.Entry) this.val$entries.next();
                            if (DirectedGraphConnections.isPredecessor(entry.getValue())) {
                                return (N) entry.getKey();
                            }
                        }
                        return endOfData();
                    }
                } : new AbstractIterator<N>(this, directedGraphConnections.orderedNodeConnections.iterator()) { // from class: com.google.common.graph.DirectedGraphConnections.2.2
                    final Iterator val$nodeConnections;

                    {
                        this.val$nodeConnections = it;
                    }

                    @Override // com.google.common.collect.AbstractIterator
                    @CheckForNull
                    protected N computeNext() {
                        while (this.val$nodeConnections.hasNext()) {
                            NodeConnection nodeConnection = (NodeConnection) this.val$nodeConnections.next();
                            if (nodeConnection instanceof NodeConnection.Pred) {
                                return nodeConnection.node;
                            }
                        }
                        return endOfData();
                    }
                };
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return this.this$0.predecessorCount;
            }
        };
    }

    @Override // com.google.common.graph.GraphConnections
    public void removePredecessor(N n) {
        boolean z;
        Preconditions.checkNotNull(n);
        Object obj = this.adjacentNodeValues.get(n);
        if (obj == PRED) {
            this.adjacentNodeValues.remove(n);
            z = true;
        } else if (obj instanceof PredAndSucc) {
            this.adjacentNodeValues.put(n, ((PredAndSucc) obj).successorValue);
            z = true;
        } else {
            z = false;
        }
        if (z) {
            int i = this.predecessorCount - 1;
            this.predecessorCount = i;
            Graphs.checkNonNegative(i);
            if (this.orderedNodeConnections != null) {
                this.orderedNodeConnections.remove(new NodeConnection.Pred(n));
            }
        }
    }

    @Override // com.google.common.graph.GraphConnections
    @CheckForNull
    public V removeSuccessor(Object obj) {
        Preconditions.checkNotNull(obj);
        Object obj2 = (V) this.adjacentNodeValues.get(obj);
        if (obj2 == null || obj2 == PRED) {
            obj2 = (V) null;
        } else {
            boolean z = obj2 instanceof PredAndSucc;
            Map<N, Object> map = this.adjacentNodeValues;
            if (z) {
                map.put(obj, PRED);
                obj2 = (V) ((PredAndSucc) obj2).successorValue;
            } else {
                map.remove(obj);
            }
        }
        if (obj2 != null) {
            int i = this.successorCount - 1;
            this.successorCount = i;
            Graphs.checkNonNegative(i);
            if (this.orderedNodeConnections != null) {
                this.orderedNodeConnections.remove(new NodeConnection.Succ(obj));
            }
        }
        if (obj2 == null) {
            return null;
        }
        return (V) obj2;
    }

    @Override // com.google.common.graph.GraphConnections
    public Set<N> successors() {
        return new AbstractSet<N>(this) { // from class: com.google.common.graph.DirectedGraphConnections.3
            final DirectedGraphConnections this$0;

            {
                this.this$0 = this;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(@CheckForNull Object obj) {
                return DirectedGraphConnections.isSuccessor(this.this$0.adjacentNodeValues.get(obj));
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public UnmodifiableIterator<N> iterator() {
                List list = this.this$0.orderedNodeConnections;
                DirectedGraphConnections directedGraphConnections = this.this$0;
                return list == null ? new AbstractIterator<N>(this, directedGraphConnections.adjacentNodeValues.entrySet().iterator()) { // from class: com.google.common.graph.DirectedGraphConnections.3.1
                    final Iterator val$entries;

                    {
                        this.val$entries = it;
                    }

                    @Override // com.google.common.collect.AbstractIterator
                    @CheckForNull
                    protected N computeNext() {
                        while (this.val$entries.hasNext()) {
                            Map.Entry entry = (Map.Entry) this.val$entries.next();
                            if (DirectedGraphConnections.isSuccessor(entry.getValue())) {
                                return (N) entry.getKey();
                            }
                        }
                        return endOfData();
                    }
                } : new AbstractIterator<N>(this, directedGraphConnections.orderedNodeConnections.iterator()) { // from class: com.google.common.graph.DirectedGraphConnections.3.2
                    final Iterator val$nodeConnections;

                    {
                        this.val$nodeConnections = it;
                    }

                    @Override // com.google.common.collect.AbstractIterator
                    @CheckForNull
                    protected N computeNext() {
                        while (this.val$nodeConnections.hasNext()) {
                            NodeConnection nodeConnection = (NodeConnection) this.val$nodeConnections.next();
                            if (nodeConnection instanceof NodeConnection.Succ) {
                                return nodeConnection.node;
                            }
                        }
                        return endOfData();
                    }
                };
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return this.this$0.successorCount;
            }
        };
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.graph.GraphConnections
    @CheckForNull
    public V value(N n) {
        Preconditions.checkNotNull(n);
        V v = (V) this.adjacentNodeValues.get(n);
        if (v == PRED) {
            return null;
        }
        return v instanceof PredAndSucc ? (V) ((PredAndSucc) v).successorValue : v;
    }
}
