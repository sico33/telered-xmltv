package com.google.common.graph;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import com.google.errorprone.annotations.DoNotMock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@DoNotMock("Call forGraph or forTree, passing a lambda or a Graph with the desired edges (built with GraphBuilder)")
@ElementTypesAreNonnullByDefault
public abstract class Traverser<N> {
    private final SuccessorsFunction<N> successorFunction;

    private enum InsertionOrder {
        FRONT { // from class: com.google.common.graph.Traverser.InsertionOrder.1
            @Override // com.google.common.graph.Traverser.InsertionOrder
            <T> void insertInto(Deque<T> deque, T t) {
                deque.addFirst(t);
            }
        },
        BACK { // from class: com.google.common.graph.Traverser.InsertionOrder.2
            @Override // com.google.common.graph.Traverser.InsertionOrder
            <T> void insertInto(Deque<T> deque, T t) {
                deque.addLast(t);
            }
        };

        abstract <T> void insertInto(Deque<T> deque, T t);
    }

    private static abstract class Traversal<N> {
        final SuccessorsFunction<N> successorFunction;

        Traversal(SuccessorsFunction<N> successorsFunction) {
            this.successorFunction = successorsFunction;
        }

        static <N> Traversal<N> inGraph(SuccessorsFunction<N> successorsFunction) {
            return new Traversal<N>(successorsFunction, new HashSet()) { // from class: com.google.common.graph.Traverser.Traversal.1
                final Set val$visited;

                {
                    this.val$visited = set;
                }

                @Override // com.google.common.graph.Traverser.Traversal
                @CheckForNull
                N visitNext(Deque<Iterator<? extends N>> deque) {
                    Iterator<? extends N> first = deque.getFirst();
                    while (first.hasNext()) {
                        N next = first.next();
                        Objects.requireNonNull(next);
                        if (this.val$visited.add(next)) {
                            return next;
                        }
                    }
                    deque.removeFirst();
                    return null;
                }
            };
        }

        static <N> Traversal<N> inTree(SuccessorsFunction<N> successorsFunction) {
            return new Traversal<N>(successorsFunction) { // from class: com.google.common.graph.Traverser.Traversal.2
                @Override // com.google.common.graph.Traverser.Traversal
                @CheckForNull
                N visitNext(Deque<Iterator<? extends N>> deque) {
                    Iterator<? extends N> first = deque.getFirst();
                    if (first.hasNext()) {
                        return (N) Preconditions.checkNotNull(first.next());
                    }
                    deque.removeFirst();
                    return null;
                }
            };
        }

        private Iterator<N> topDown(Iterator<? extends N> it, InsertionOrder insertionOrder) {
            ArrayDeque arrayDeque = new ArrayDeque();
            arrayDeque.add(it);
            return new AbstractIterator<N>(this, arrayDeque, insertionOrder) { // from class: com.google.common.graph.Traverser.Traversal.3
                final Traversal this$0;
                final Deque val$horizon;
                final InsertionOrder val$order;

                {
                    this.this$0 = this;
                    this.val$horizon = arrayDeque;
                    this.val$order = insertionOrder;
                }

                /* JADX WARN: Type inference fix 'apply assigned field type' failed
                java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
                	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:596)
                	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
                	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
                 */
                @Override // com.google.common.collect.AbstractIterator
                @CheckForNull
                protected N computeNext() {
                    do {
                        N n = (N) this.this$0.visitNext(this.val$horizon);
                        if (n != null) {
                            Iterator<? extends N> it2 = this.this$0.successorFunction.successors(n).iterator();
                            if (!it2.hasNext()) {
                                return n;
                            }
                            this.val$order.insertInto(this.val$horizon, it2);
                            return n;
                        }
                    } while (!this.val$horizon.isEmpty());
                    return endOfData();
                }
            };
        }

        final Iterator<N> breadthFirst(Iterator<? extends N> it) {
            return topDown(it, InsertionOrder.BACK);
        }

        final Iterator<N> postOrder(Iterator<? extends N> it) {
            ArrayDeque arrayDeque = new ArrayDeque();
            ArrayDeque arrayDeque2 = new ArrayDeque();
            arrayDeque2.add(it);
            return new AbstractIterator<N>(this, arrayDeque2, arrayDeque) { // from class: com.google.common.graph.Traverser.Traversal.4
                final Traversal this$0;
                final Deque val$ancestorStack;
                final Deque val$horizon;

                {
                    this.this$0 = this;
                    this.val$horizon = arrayDeque2;
                    this.val$ancestorStack = arrayDeque;
                }

                /* JADX WARN: Type inference fix 'apply assigned field type' failed
                java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
                	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:596)
                	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
                	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
                 */
                @Override // com.google.common.collect.AbstractIterator
                @CheckForNull
                protected N computeNext() {
                    N n = (N) this.this$0.visitNext(this.val$horizon);
                    while (n != null) {
                        Iterator<? extends N> it2 = this.this$0.successorFunction.successors(n).iterator();
                        if (!it2.hasNext()) {
                            return n;
                        }
                        this.val$horizon.addFirst(it2);
                        this.val$ancestorStack.push(n);
                        n = (N) this.this$0.visitNext(this.val$horizon);
                    }
                    return !this.val$ancestorStack.isEmpty() ? (N) this.val$ancestorStack.pop() : endOfData();
                }
            };
        }

        final Iterator<N> preOrder(Iterator<? extends N> it) {
            return topDown(it, InsertionOrder.FRONT);
        }

        @CheckForNull
        abstract N visitNext(Deque<Iterator<? extends N>> deque);
    }

    private Traverser(SuccessorsFunction<N> successorsFunction) {
        this.successorFunction = (SuccessorsFunction) Preconditions.checkNotNull(successorsFunction);
    }

    public static <N> Traverser<N> forGraph(SuccessorsFunction<N> successorsFunction) {
        return new Traverser<N>(successorsFunction, successorsFunction) { // from class: com.google.common.graph.Traverser.1
            final SuccessorsFunction val$graph;

            {
                this.val$graph = successorsFunction;
            }

            @Override // com.google.common.graph.Traverser
            Traversal<N> newTraversal() {
                return Traversal.inGraph(this.val$graph);
            }
        };
    }

    public static <N> Traverser<N> forTree(SuccessorsFunction<N> successorsFunction) {
        if (successorsFunction instanceof BaseGraph) {
            Preconditions.checkArgument(((BaseGraph) successorsFunction).isDirected(), "Undirected graphs can never be trees.");
        }
        if (successorsFunction instanceof Network) {
            Preconditions.checkArgument(((Network) successorsFunction).isDirected(), "Undirected networks can never be trees.");
        }
        return new Traverser<N>(successorsFunction, successorsFunction) { // from class: com.google.common.graph.Traverser.2
            final SuccessorsFunction val$tree;

            {
                this.val$tree = successorsFunction;
            }

            @Override // com.google.common.graph.Traverser
            Traversal<N> newTraversal() {
                return Traversal.inTree(this.val$tree);
            }
        };
    }

    private ImmutableSet<N> validate(Iterable<? extends N> iterable) {
        ImmutableSet<N> immutableSetCopyOf = ImmutableSet.copyOf(iterable);
        UnmodifiableIterator<N> it = immutableSetCopyOf.iterator();
        while (it.hasNext()) {
            this.successorFunction.successors(it.next());
        }
        return immutableSetCopyOf;
    }

    public final Iterable<N> breadthFirst(Iterable<? extends N> iterable) {
        return new Iterable<N>(this, validate(iterable)) { // from class: com.google.common.graph.Traverser.3
            final Traverser this$0;
            final ImmutableSet val$validated;

            {
                this.this$0 = this;
                this.val$validated = immutableSet;
            }

            @Override // java.lang.Iterable
            public Iterator<N> iterator() {
                return this.this$0.newTraversal().breadthFirst(this.val$validated.iterator());
            }
        };
    }

    public final Iterable<N> breadthFirst(N n) {
        return breadthFirst((Iterable) ImmutableSet.of(n));
    }

    public final Iterable<N> depthFirstPostOrder(Iterable<? extends N> iterable) {
        return new Iterable<N>(this, validate(iterable)) { // from class: com.google.common.graph.Traverser.5
            final Traverser this$0;
            final ImmutableSet val$validated;

            {
                this.this$0 = this;
                this.val$validated = immutableSet;
            }

            @Override // java.lang.Iterable
            public Iterator<N> iterator() {
                return this.this$0.newTraversal().postOrder(this.val$validated.iterator());
            }
        };
    }

    public final Iterable<N> depthFirstPostOrder(N n) {
        return depthFirstPostOrder((Iterable) ImmutableSet.of(n));
    }

    public final Iterable<N> depthFirstPreOrder(Iterable<? extends N> iterable) {
        return new Iterable<N>(this, validate(iterable)) { // from class: com.google.common.graph.Traverser.4
            final Traverser this$0;
            final ImmutableSet val$validated;

            {
                this.this$0 = this;
                this.val$validated = immutableSet;
            }

            @Override // java.lang.Iterable
            public Iterator<N> iterator() {
                return this.this$0.newTraversal().preOrder(this.val$validated.iterator());
            }
        };
    }

    public final Iterable<N> depthFirstPreOrder(N n) {
        return depthFirstPreOrder((Iterable) ImmutableSet.of(n));
    }

    abstract Traversal<N> newTraversal();
}
