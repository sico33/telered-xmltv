package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Queue;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
@Deprecated
public abstract class TreeTraverser<T> {

    private final class BreadthFirstIterator extends UnmodifiableIterator<T> implements PeekingIterator<T> {
        private final Queue<T> queue = new ArrayDeque();
        final TreeTraverser this$0;

        BreadthFirstIterator(TreeTraverser treeTraverser, T t) {
            this.this$0 = treeTraverser;
            this.queue.add(t);
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return !this.queue.isEmpty();
        }

        @Override // java.util.Iterator, com.google.common.collect.PeekingIterator
        public T next() {
            T tRemove = this.queue.remove();
            Iterables.addAll(this.queue, this.this$0.children(tRemove));
            return tRemove;
        }

        @Override // com.google.common.collect.PeekingIterator
        public T peek() {
            return this.queue.element();
        }
    }

    private final class PostOrderIterator extends AbstractIterator<T> {
        private final ArrayDeque<PostOrderNode<T>> stack = new ArrayDeque<>();
        final TreeTraverser this$0;

        PostOrderIterator(TreeTraverser treeTraverser, T t) {
            this.this$0 = treeTraverser;
            this.stack.addLast(expand(t));
        }

        private PostOrderNode<T> expand(T t) {
            return new PostOrderNode<>(t, this.this$0.children(t).iterator());
        }

        @Override // com.google.common.collect.AbstractIterator
        @CheckForNull
        protected T computeNext() {
            while (!this.stack.isEmpty()) {
                PostOrderNode<T> last = this.stack.getLast();
                if (!last.childIterator.hasNext()) {
                    this.stack.removeLast();
                    return last.root;
                }
                this.stack.addLast(expand(last.childIterator.next()));
            }
            return endOfData();
        }
    }

    private static final class PostOrderNode<T> {
        final Iterator<T> childIterator;
        final T root;

        PostOrderNode(T t, Iterator<T> it) {
            this.root = (T) Preconditions.checkNotNull(t);
            this.childIterator = (Iterator) Preconditions.checkNotNull(it);
        }
    }

    private final class PreOrderIterator extends UnmodifiableIterator<T> {
        private final Deque<Iterator<T>> stack = new ArrayDeque();
        final TreeTraverser this$0;

        PreOrderIterator(TreeTraverser treeTraverser, T t) {
            this.this$0 = treeTraverser;
            this.stack.addLast(Iterators.singletonIterator(Preconditions.checkNotNull(t)));
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return !this.stack.isEmpty();
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
        @Override // java.util.Iterator
        public T next() {
            Iterator<T> last = this.stack.getLast();
            T t = (T) Preconditions.checkNotNull(last.next());
            if (!last.hasNext()) {
                this.stack.removeLast();
            }
            Iterator<T> it = this.this$0.children(t).iterator();
            if (it.hasNext()) {
                this.stack.addLast(it);
            }
            return t;
        }
    }

    @Deprecated
    public static <T> TreeTraverser<T> using(Function<T, ? extends Iterable<T>> function) {
        Preconditions.checkNotNull(function);
        return new TreeTraverser<T>(function) { // from class: com.google.common.collect.TreeTraverser.1
            final Function val$nodeToChildrenFunction;

            {
                this.val$nodeToChildrenFunction = function;
            }

            @Override // com.google.common.collect.TreeTraverser
            public Iterable<T> children(T t) {
                return (Iterable) this.val$nodeToChildrenFunction.apply(t);
            }
        };
    }

    @Deprecated
    public final FluentIterable<T> breadthFirstTraversal(T t) {
        Preconditions.checkNotNull(t);
        return new FluentIterable<T>(this, t) { // from class: com.google.common.collect.TreeTraverser.4
            final TreeTraverser this$0;
            final Object val$root;

            {
                this.this$0 = this;
                this.val$root = t;
            }

            @Override // java.lang.Iterable
            public UnmodifiableIterator<T> iterator() {
                return new BreadthFirstIterator(this.this$0, this.val$root);
            }
        };
    }

    public abstract Iterable<T> children(T t);

    UnmodifiableIterator<T> postOrderIterator(T t) {
        return new PostOrderIterator(this, t);
    }

    @Deprecated
    public final FluentIterable<T> postOrderTraversal(T t) {
        Preconditions.checkNotNull(t);
        return new FluentIterable<T>(this, t) { // from class: com.google.common.collect.TreeTraverser.3
            final TreeTraverser this$0;
            final Object val$root;

            {
                this.this$0 = this;
                this.val$root = t;
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // java.lang.Iterable
            public UnmodifiableIterator<T> iterator() {
                return this.this$0.postOrderIterator(this.val$root);
            }
        };
    }

    UnmodifiableIterator<T> preOrderIterator(T t) {
        return new PreOrderIterator(this, t);
    }

    @Deprecated
    public final FluentIterable<T> preOrderTraversal(T t) {
        Preconditions.checkNotNull(t);
        return new FluentIterable<T>(this, t) { // from class: com.google.common.collect.TreeTraverser.2
            final TreeTraverser this$0;
            final Object val$root;

            {
                this.this$0 = this;
                this.val$root = t;
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // java.lang.Iterable
            public UnmodifiableIterator<T> iterator() {
                return this.this$0.preOrderIterator(this.val$root);
            }
        };
    }
}
