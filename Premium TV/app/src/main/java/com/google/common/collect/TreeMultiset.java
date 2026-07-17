package com.google.common.collect;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class TreeMultiset<E> extends AbstractSortedMultiset<E> implements Serializable {
    private static final long serialVersionUID = 1;
    private final transient AvlNode<E> header;
    private final transient GeneralRange<E> range;
    private final transient Reference<AvlNode<E>> rootReference;

    /* JADX INFO: renamed from: com.google.common.collect.TreeMultiset$4, reason: invalid class name */
    static /* synthetic */ class AnonymousClass4 {
        static final int[] $SwitchMap$com$google$common$collect$BoundType = new int[BoundType.values().length];

        static {
            try {
                $SwitchMap$com$google$common$collect$BoundType[BoundType.OPEN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$common$collect$BoundType[BoundType.CLOSED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private enum Aggregate {
        SIZE { // from class: com.google.common.collect.TreeMultiset.Aggregate.1
            @Override // com.google.common.collect.TreeMultiset.Aggregate
            int nodeAggregate(AvlNode<?> avlNode) {
                return ((AvlNode) avlNode).elemCount;
            }

            @Override // com.google.common.collect.TreeMultiset.Aggregate
            long treeAggregate(@CheckForNull AvlNode<?> avlNode) {
                if (avlNode == null) {
                    return 0L;
                }
                return ((AvlNode) avlNode).totalCount;
            }
        },
        DISTINCT { // from class: com.google.common.collect.TreeMultiset.Aggregate.2
            @Override // com.google.common.collect.TreeMultiset.Aggregate
            int nodeAggregate(AvlNode<?> avlNode) {
                return 1;
            }

            @Override // com.google.common.collect.TreeMultiset.Aggregate
            long treeAggregate(@CheckForNull AvlNode<?> avlNode) {
                if (avlNode == null) {
                    return 0L;
                }
                return ((AvlNode) avlNode).distinctElements;
            }
        };

        abstract int nodeAggregate(AvlNode<?> avlNode);

        abstract long treeAggregate(@CheckForNull AvlNode<?> avlNode);
    }

    private static final class AvlNode<E> {
        private int distinctElements;

        @CheckForNull
        private final E elem;
        private int elemCount;
        private int height;

        @CheckForNull
        private AvlNode<E> left;

        @CheckForNull
        private AvlNode<E> pred;

        @CheckForNull
        private AvlNode<E> right;

        @CheckForNull
        private AvlNode<E> succ;
        private long totalCount;

        AvlNode() {
            this.elem = null;
            this.elemCount = 1;
        }

        AvlNode(@ParametricNullness E e, int i) {
            Preconditions.checkArgument(i > 0);
            this.elem = e;
            this.elemCount = i;
            this.totalCount = i;
            this.distinctElements = 1;
            this.height = 1;
            this.left = null;
            this.right = null;
        }

        private AvlNode<E> addLeftChild(@ParametricNullness E e, int i) {
            this.left = new AvlNode<>(e, i);
            TreeMultiset.successor(pred(), this.left, this);
            this.height = Math.max(2, this.height);
            this.distinctElements++;
            this.totalCount += (long) i;
            return this;
        }

        private AvlNode<E> addRightChild(@ParametricNullness E e, int i) {
            this.right = new AvlNode<>(e, i);
            TreeMultiset.successor(this, this.right, succ());
            this.height = Math.max(2, this.height);
            this.distinctElements++;
            this.totalCount += (long) i;
            return this;
        }

        private int balanceFactor() {
            return height(this.left) - height(this.right);
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* JADX WARN: Multi-variable type inference failed */
        @CheckForNull
        public AvlNode<E> ceiling(Comparator<? super E> comparator, @ParametricNullness E e) {
            int iCompare = comparator.compare(e, getElement());
            if (iCompare < 0) {
                return this.left == null ? this : (AvlNode) MoreObjects.firstNonNull(this.left.ceiling(comparator, e), this);
            }
            if (iCompare != 0) {
                return this.right == null ? null : this.right.ceiling(comparator, e);
            }
            return this;
        }

        @CheckForNull
        private AvlNode<E> deleteMe() {
            int i = this.elemCount;
            this.elemCount = 0;
            TreeMultiset.successor(pred(), succ());
            AvlNode<E> avlNode = this.left;
            AvlNode<E> avlNode2 = this.right;
            if (avlNode == null) {
                return avlNode2;
            }
            AvlNode<E> avlNode3 = this.left;
            if (avlNode2 == null) {
                return avlNode3;
            }
            if (avlNode3.height >= this.right.height) {
                AvlNode<E> avlNodePred = pred();
                avlNodePred.left = this.left.removeMax(avlNodePred);
                avlNodePred.right = this.right;
                avlNodePred.distinctElements = this.distinctElements - 1;
                avlNodePred.totalCount = this.totalCount - ((long) i);
                return avlNodePred.rebalance();
            }
            AvlNode<E> avlNodeSucc = succ();
            avlNodeSucc.right = this.right.removeMin(avlNodeSucc);
            avlNodeSucc.left = this.left;
            avlNodeSucc.distinctElements = this.distinctElements - 1;
            avlNodeSucc.totalCount = this.totalCount - ((long) i);
            return avlNodeSucc.rebalance();
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* JADX WARN: Multi-variable type inference failed */
        @CheckForNull
        public AvlNode<E> floor(Comparator<? super E> comparator, @ParametricNullness E e) {
            int iCompare = comparator.compare(e, getElement());
            if (iCompare > 0) {
                return this.right == null ? this : (AvlNode) MoreObjects.firstNonNull(this.right.floor(comparator, e), this);
            }
            if (iCompare != 0) {
                return this.left == null ? null : this.left.floor(comparator, e);
            }
            return this;
        }

        private static int height(@CheckForNull AvlNode<?> avlNode) {
            if (avlNode == null) {
                return 0;
            }
            return ((AvlNode) avlNode).height;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public AvlNode<E> pred() {
            return (AvlNode) Objects.requireNonNull(this.pred);
        }

        private AvlNode<E> rebalance() {
            switch (balanceFactor()) {
                case -2:
                    Objects.requireNonNull(this.right);
                    if (this.right.balanceFactor() > 0) {
                        this.right = this.right.rotateRight();
                    }
                    return rotateLeft();
                case 2:
                    Objects.requireNonNull(this.left);
                    if (this.left.balanceFactor() < 0) {
                        this.left = this.left.rotateLeft();
                    }
                    return rotateRight();
                default:
                    recomputeHeight();
                    return this;
            }
        }

        private void recompute() {
            recomputeMultiset();
            recomputeHeight();
        }

        private void recomputeHeight() {
            this.height = Math.max(height(this.left), height(this.right)) + 1;
        }

        private void recomputeMultiset() {
            this.distinctElements = TreeMultiset.distinctElements(this.left) + 1 + TreeMultiset.distinctElements(this.right);
            this.totalCount = ((long) this.elemCount) + totalCount(this.left) + totalCount(this.right);
        }

        @CheckForNull
        private AvlNode<E> removeMax(AvlNode<E> avlNode) {
            if (this.right == null) {
                return this.left;
            }
            this.right = this.right.removeMax(avlNode);
            this.distinctElements--;
            this.totalCount -= (long) avlNode.elemCount;
            return rebalance();
        }

        @CheckForNull
        private AvlNode<E> removeMin(AvlNode<E> avlNode) {
            if (this.left == null) {
                return this.right;
            }
            this.left = this.left.removeMin(avlNode);
            this.distinctElements--;
            this.totalCount -= (long) avlNode.elemCount;
            return rebalance();
        }

        private AvlNode<E> rotateLeft() {
            Preconditions.checkState(this.right != null);
            AvlNode<E> avlNode = this.right;
            this.right = avlNode.left;
            avlNode.left = this;
            avlNode.totalCount = this.totalCount;
            avlNode.distinctElements = this.distinctElements;
            recompute();
            avlNode.recomputeHeight();
            return avlNode;
        }

        private AvlNode<E> rotateRight() {
            Preconditions.checkState(this.left != null);
            AvlNode<E> avlNode = this.left;
            this.left = avlNode.right;
            avlNode.right = this;
            avlNode.totalCount = this.totalCount;
            avlNode.distinctElements = this.distinctElements;
            recompute();
            avlNode.recomputeHeight();
            return avlNode;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public AvlNode<E> succ() {
            return (AvlNode) Objects.requireNonNull(this.succ);
        }

        private static long totalCount(@CheckForNull AvlNode<?> avlNode) {
            if (avlNode == null) {
                return 0L;
            }
            return ((AvlNode) avlNode).totalCount;
        }

        /* JADX WARN: Multi-variable type inference failed */
        AvlNode<E> add(Comparator<? super E> comparator, @ParametricNullness E e, int i, int[] iArr) {
            int iCompare = comparator.compare(e, getElement());
            if (iCompare < 0) {
                AvlNode<E> avlNode = this.left;
                if (avlNode == null) {
                    iArr[0] = 0;
                    return addLeftChild(e, i);
                }
                int i2 = avlNode.height;
                this.left = avlNode.add(comparator, e, i, iArr);
                if (iArr[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) i;
                return this.left.height != i2 ? rebalance() : this;
            }
            if (iCompare <= 0) {
                iArr[0] = this.elemCount;
                Preconditions.checkArgument(((long) this.elemCount) + ((long) i) <= 2147483647L);
                this.elemCount += i;
                this.totalCount += (long) i;
                return this;
            }
            AvlNode<E> avlNode2 = this.right;
            if (avlNode2 == null) {
                iArr[0] = 0;
                return addRightChild(e, i);
            }
            int i3 = avlNode2.height;
            this.right = avlNode2.add(comparator, e, i, iArr);
            if (iArr[0] == 0) {
                this.distinctElements++;
            }
            this.totalCount += (long) i;
            return this.right.height != i3 ? rebalance() : this;
        }

        /* JADX WARN: Multi-variable type inference failed */
        int count(Comparator<? super E> comparator, @ParametricNullness E e) {
            int iCompare = comparator.compare(e, getElement());
            if (iCompare < 0) {
                if (this.left == null) {
                    return 0;
                }
                return this.left.count(comparator, e);
            }
            if (iCompare <= 0) {
                return this.elemCount;
            }
            if (this.right != null) {
                return this.right.count(comparator, e);
            }
            return 0;
        }

        int getCount() {
            return this.elemCount;
        }

        @ParametricNullness
        E getElement() {
            return (E) NullnessCasts.uncheckedCastNullableTToT(this.elem);
        }

        /* JADX WARN: Multi-variable type inference failed */
        @CheckForNull
        AvlNode<E> remove(Comparator<? super E> comparator, @ParametricNullness E e, int i, int[] iArr) {
            int iCompare = comparator.compare(e, getElement());
            if (iCompare < 0) {
                AvlNode<E> avlNode = this.left;
                if (avlNode == null) {
                    iArr[0] = 0;
                    return this;
                }
                this.left = avlNode.remove(comparator, e, i, iArr);
                if (iArr[0] > 0) {
                    if (i >= iArr[0]) {
                        this.distinctElements--;
                        this.totalCount -= (long) iArr[0];
                    } else {
                        this.totalCount -= (long) i;
                    }
                }
                return iArr[0] != 0 ? rebalance() : this;
            }
            if (iCompare <= 0) {
                iArr[0] = this.elemCount;
                if (i >= this.elemCount) {
                    return deleteMe();
                }
                this.elemCount -= i;
                this.totalCount -= (long) i;
                return this;
            }
            AvlNode<E> avlNode2 = this.right;
            if (avlNode2 == null) {
                iArr[0] = 0;
                return this;
            }
            this.right = avlNode2.remove(comparator, e, i, iArr);
            if (iArr[0] > 0) {
                if (i >= iArr[0]) {
                    this.distinctElements--;
                    this.totalCount -= (long) iArr[0];
                } else {
                    this.totalCount -= (long) i;
                }
            }
            return rebalance();
        }

        /* JADX WARN: Multi-variable type inference failed */
        @CheckForNull
        AvlNode<E> setCount(Comparator<? super E> comparator, @ParametricNullness E e, int i, int i2, int[] iArr) {
            int iCompare = comparator.compare(e, getElement());
            if (iCompare < 0) {
                AvlNode<E> avlNode = this.left;
                if (avlNode == null) {
                    iArr[0] = 0;
                    return (i != 0 || i2 <= 0) ? this : addLeftChild(e, i2);
                }
                this.left = avlNode.setCount(comparator, e, i, i2, iArr);
                if (iArr[0] == i) {
                    if (i2 == 0 && iArr[0] != 0) {
                        this.distinctElements--;
                    } else if (i2 > 0 && iArr[0] == 0) {
                        this.distinctElements++;
                    }
                    this.totalCount += (long) (i2 - iArr[0]);
                }
                return rebalance();
            }
            if (iCompare <= 0) {
                iArr[0] = this.elemCount;
                if (i != this.elemCount) {
                    return this;
                }
                if (i2 == 0) {
                    return deleteMe();
                }
                this.totalCount += (long) (i2 - this.elemCount);
                this.elemCount = i2;
                return this;
            }
            AvlNode<E> avlNode2 = this.right;
            if (avlNode2 == null) {
                iArr[0] = 0;
                return (i != 0 || i2 <= 0) ? this : addRightChild(e, i2);
            }
            this.right = avlNode2.setCount(comparator, e, i, i2, iArr);
            if (iArr[0] == i) {
                if (i2 == 0 && iArr[0] != 0) {
                    this.distinctElements--;
                } else if (i2 > 0 && iArr[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) (i2 - iArr[0]);
            }
            return rebalance();
        }

        /* JADX WARN: Multi-variable type inference failed */
        @CheckForNull
        AvlNode<E> setCount(Comparator<? super E> comparator, @ParametricNullness E e, int i, int[] iArr) {
            int iCompare = comparator.compare(e, getElement());
            if (iCompare < 0) {
                AvlNode<E> avlNode = this.left;
                if (avlNode == null) {
                    iArr[0] = 0;
                    return i > 0 ? addLeftChild(e, i) : this;
                }
                this.left = avlNode.setCount(comparator, e, i, iArr);
                if (i == 0 && iArr[0] != 0) {
                    this.distinctElements--;
                } else if (i > 0 && iArr[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) (i - iArr[0]);
                return rebalance();
            }
            if (iCompare <= 0) {
                iArr[0] = this.elemCount;
                if (i == 0) {
                    return deleteMe();
                }
                this.totalCount += (long) (i - this.elemCount);
                this.elemCount = i;
                return this;
            }
            AvlNode<E> avlNode2 = this.right;
            if (avlNode2 == null) {
                iArr[0] = 0;
                return i > 0 ? addRightChild(e, i) : this;
            }
            this.right = avlNode2.setCount(comparator, e, i, iArr);
            if (i == 0 && iArr[0] != 0) {
                this.distinctElements--;
            } else if (i > 0 && iArr[0] == 0) {
                this.distinctElements++;
            }
            this.totalCount += (long) (i - iArr[0]);
            return rebalance();
        }

        public String toString() {
            return Multisets.immutableEntry(getElement(), getCount()).toString();
        }
    }

    private static final class Reference<T> {

        @CheckForNull
        private T value;

        private Reference() {
        }

        public void checkAndSet(@CheckForNull T t, @CheckForNull T t2) {
            if (this.value != t) {
                throw new ConcurrentModificationException();
            }
            this.value = t2;
        }

        void clear() {
            this.value = null;
        }

        @CheckForNull
        public T get() {
            return this.value;
        }
    }

    TreeMultiset(Reference<AvlNode<E>> reference, GeneralRange<E> generalRange, AvlNode<E> avlNode) {
        super(generalRange.comparator());
        this.rootReference = reference;
        this.range = generalRange;
        this.header = avlNode;
    }

    TreeMultiset(Comparator<? super E> comparator) {
        super(comparator);
        this.range = GeneralRange.all(comparator);
        this.header = new AvlNode<>();
        successor(this.header, this.header);
        this.rootReference = new Reference<>();
    }

    private long aggregateAboveRange(Aggregate aggregate, @CheckForNull AvlNode<E> avlNode) {
        if (avlNode == null) {
            return 0L;
        }
        int iCompare = comparator().compare(NullnessCasts.uncheckedCastNullableTToT(this.range.getUpperEndpoint()), avlNode.getElement());
        if (iCompare > 0) {
            return aggregateAboveRange(aggregate, ((AvlNode) avlNode).right);
        }
        if (iCompare != 0) {
            return aggregate.treeAggregate(((AvlNode) avlNode).right) + ((long) aggregate.nodeAggregate(avlNode)) + aggregateAboveRange(aggregate, ((AvlNode) avlNode).left);
        }
        switch (AnonymousClass4.$SwitchMap$com$google$common$collect$BoundType[this.range.getUpperBoundType().ordinal()]) {
            case 1:
                return ((long) aggregate.nodeAggregate(avlNode)) + aggregate.treeAggregate(((AvlNode) avlNode).right);
            case 2:
                return aggregate.treeAggregate(((AvlNode) avlNode).right);
            default:
                throw new AssertionError();
        }
    }

    private long aggregateBelowRange(Aggregate aggregate, @CheckForNull AvlNode<E> avlNode) {
        if (avlNode == null) {
            return 0L;
        }
        int iCompare = comparator().compare(NullnessCasts.uncheckedCastNullableTToT(this.range.getLowerEndpoint()), avlNode.getElement());
        if (iCompare < 0) {
            return aggregateBelowRange(aggregate, ((AvlNode) avlNode).left);
        }
        if (iCompare != 0) {
            return aggregate.treeAggregate(((AvlNode) avlNode).left) + ((long) aggregate.nodeAggregate(avlNode)) + aggregateBelowRange(aggregate, ((AvlNode) avlNode).right);
        }
        switch (AnonymousClass4.$SwitchMap$com$google$common$collect$BoundType[this.range.getLowerBoundType().ordinal()]) {
            case 1:
                return ((long) aggregate.nodeAggregate(avlNode)) + aggregate.treeAggregate(((AvlNode) avlNode).left);
            case 2:
                return aggregate.treeAggregate(((AvlNode) avlNode).left);
            default:
                throw new AssertionError();
        }
    }

    private long aggregateForEntries(Aggregate aggregate) {
        AvlNode<E> avlNode = this.rootReference.get();
        long jTreeAggregate = aggregate.treeAggregate(avlNode);
        if (this.range.hasLowerBound()) {
            jTreeAggregate -= aggregateBelowRange(aggregate, avlNode);
        }
        return this.range.hasUpperBound() ? jTreeAggregate - aggregateAboveRange(aggregate, avlNode) : jTreeAggregate;
    }

    public static <E extends Comparable> TreeMultiset<E> create() {
        return new TreeMultiset<>(Ordering.natural());
    }

    public static <E extends Comparable> TreeMultiset<E> create(Iterable<? extends E> iterable) {
        TreeMultiset<E> treeMultisetCreate = create();
        Iterables.addAll(treeMultisetCreate, iterable);
        return treeMultisetCreate;
    }

    public static <E> TreeMultiset<E> create(@CheckForNull Comparator<? super E> comparator) {
        return comparator == null ? new TreeMultiset<>(Ordering.natural()) : new TreeMultiset<>(comparator);
    }

    static int distinctElements(@CheckForNull AvlNode<?> avlNode) {
        if (avlNode == null) {
            return 0;
        }
        return ((AvlNode) avlNode).distinctElements;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @CheckForNull
    public AvlNode<E> firstNode() {
        AvlNode<E> avlNodeSucc;
        AvlNode<E> avlNode = this.rootReference.get();
        if (avlNode == null) {
            return null;
        }
        if (this.range.hasLowerBound()) {
            Object objUncheckedCastNullableTToT = NullnessCasts.uncheckedCastNullableTToT(this.range.getLowerEndpoint());
            avlNodeSucc = avlNode.ceiling(comparator(), objUncheckedCastNullableTToT);
            if (avlNodeSucc == null) {
                return null;
            }
            if (this.range.getLowerBoundType() == BoundType.OPEN && comparator().compare(objUncheckedCastNullableTToT, avlNodeSucc.getElement()) == 0) {
                avlNodeSucc = avlNodeSucc.succ();
            }
        } else {
            avlNodeSucc = this.header.succ();
        }
        if (avlNodeSucc == this.header || !this.range.contains(avlNodeSucc.getElement())) {
            avlNodeSucc = null;
        }
        return avlNodeSucc;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @CheckForNull
    public AvlNode<E> lastNode() {
        AvlNode<E> avlNodePred;
        AvlNode<E> avlNode = this.rootReference.get();
        if (avlNode == null) {
            return null;
        }
        if (this.range.hasUpperBound()) {
            Object objUncheckedCastNullableTToT = NullnessCasts.uncheckedCastNullableTToT(this.range.getUpperEndpoint());
            avlNodePred = avlNode.floor(comparator(), objUncheckedCastNullableTToT);
            if (avlNodePred == null) {
                return null;
            }
            if (this.range.getUpperBoundType() == BoundType.OPEN && comparator().compare(objUncheckedCastNullableTToT, avlNodePred.getElement()) == 0) {
                avlNodePred = avlNodePred.pred();
            }
        } else {
            avlNodePred = this.header.pred();
        }
        if (avlNodePred == this.header || !this.range.contains(avlNodePred.getElement())) {
            avlNodePred = null;
        }
        return avlNodePred;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        Comparator comparator = (Comparator) Objects.requireNonNull(objectInputStream.readObject());
        Serialization.getFieldSetter(AbstractSortedMultiset.class, "comparator").set(this, comparator);
        Serialization.getFieldSetter(TreeMultiset.class, "range").set(this, GeneralRange.all(comparator));
        Serialization.getFieldSetter(TreeMultiset.class, "rootReference").set(this, new Reference());
        AvlNode avlNode = new AvlNode();
        Serialization.getFieldSetter(TreeMultiset.class, "header").set(this, avlNode);
        successor(avlNode, avlNode);
        Serialization.populateMultiset(this, objectInputStream);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T> void successor(AvlNode<T> avlNode, AvlNode<T> avlNode2) {
        ((AvlNode) avlNode).succ = avlNode2;
        ((AvlNode) avlNode2).pred = avlNode;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T> void successor(AvlNode<T> avlNode, AvlNode<T> avlNode2, AvlNode<T> avlNode3) {
        successor(avlNode, avlNode2);
        successor(avlNode2, avlNode3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Multiset.Entry<E> wrapEntry(AvlNode<E> avlNode) {
        return new Multisets.AbstractEntry<E>(this, avlNode) { // from class: com.google.common.collect.TreeMultiset.1
            final TreeMultiset this$0;
            final AvlNode val$baseEntry;

            {
                this.this$0 = this;
                this.val$baseEntry = avlNode;
            }

            @Override // com.google.common.collect.Multiset.Entry
            public int getCount() {
                int count = this.val$baseEntry.getCount();
                return count == 0 ? this.this$0.count(getElement()) : count;
            }

            @Override // com.google.common.collect.Multiset.Entry
            @ParametricNullness
            public E getElement() {
                return (E) this.val$baseEntry.getElement();
            }
        };
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(elementSet().comparator());
        Serialization.writeMultiset(this, objectOutputStream);
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public int add(@ParametricNullness E e, int i) {
        CollectPreconditions.checkNonnegative(i, "occurrences");
        if (i == 0) {
            return count(e);
        }
        Preconditions.checkArgument(this.range.contains(e));
        AvlNode<E> avlNode = this.rootReference.get();
        if (avlNode != null) {
            int[] iArr = new int[1];
            this.rootReference.checkAndSet(avlNode, avlNode.add(comparator(), e, i, iArr));
            return iArr[0];
        }
        comparator().compare(e, e);
        AvlNode<E> avlNode2 = new AvlNode<>(e, i);
        successor(this.header, avlNode2, this.header);
        this.rootReference.checkAndSet(avlNode, avlNode2);
        return 0;
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
    public void clear() {
        if (this.range.hasLowerBound() || this.range.hasUpperBound()) {
            Iterators.clear(entryIterator());
            return;
        }
        AvlNode<E> avlNodeSucc = this.header.succ();
        while (avlNodeSucc != this.header) {
            AvlNode<E> avlNodeSucc2 = avlNodeSucc.succ();
            ((AvlNode) avlNodeSucc).elemCount = 0;
            ((AvlNode) avlNodeSucc).left = null;
            ((AvlNode) avlNodeSucc).right = null;
            ((AvlNode) avlNodeSucc).pred = null;
            ((AvlNode) avlNodeSucc).succ = null;
            avlNodeSucc = avlNodeSucc2;
        }
        successor(this.header, this.header);
        this.rootReference.clear();
    }

    @Override // com.google.common.collect.AbstractSortedMultiset, com.google.common.collect.SortedMultiset, com.google.common.collect.SortedIterable
    public /* bridge */ /* synthetic */ Comparator comparator() {
        return super.comparator();
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ boolean contains(@CheckForNull Object obj) {
        return super.contains(obj);
    }

    @Override // com.google.common.collect.Multiset
    public int count(@CheckForNull Object obj) {
        try {
            AvlNode<E> avlNode = this.rootReference.get();
            if (!this.range.contains(obj) || avlNode == null) {
                return 0;
            }
            return avlNode.count(comparator(), obj);
        } catch (ClassCastException | NullPointerException e) {
            return 0;
        }
    }

    @Override // com.google.common.collect.AbstractSortedMultiset
    Iterator<Multiset.Entry<E>> descendingEntryIterator() {
        return new Iterator<Multiset.Entry<E>>(this) { // from class: com.google.common.collect.TreeMultiset.3

            @CheckForNull
            AvlNode<E> current;

            @CheckForNull
            Multiset.Entry<E> prevEntry = null;
            final TreeMultiset this$0;

            {
                this.this$0 = this;
                this.current = this.this$0.lastNode();
            }

            @Override // java.util.Iterator
            public boolean hasNext() {
                if (this.current == null) {
                    return false;
                }
                if (!this.this$0.range.tooLow(this.current.getElement())) {
                    return true;
                }
                this.current = null;
                return false;
            }

            @Override // java.util.Iterator
            public Multiset.Entry<E> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Objects.requireNonNull(this.current);
                Multiset.Entry<E> entryWrapEntry = this.this$0.wrapEntry(this.current);
                this.prevEntry = entryWrapEntry;
                if (this.current.pred() == this.this$0.header) {
                    this.current = null;
                } else {
                    this.current = this.current.pred();
                }
                return entryWrapEntry;
            }

            @Override // java.util.Iterator
            public void remove() {
                Preconditions.checkState(this.prevEntry != null, "no calls to next() since the last call to remove()");
                this.this$0.setCount(this.prevEntry.getElement(), 0);
                this.prevEntry = null;
            }
        };
    }

    @Override // com.google.common.collect.AbstractSortedMultiset, com.google.common.collect.SortedMultiset
    public /* bridge */ /* synthetic */ SortedMultiset descendingMultiset() {
        return super.descendingMultiset();
    }

    @Override // com.google.common.collect.AbstractMultiset
    int distinctElements() {
        return Ints.saturatedCast(aggregateForEntries(Aggregate.DISTINCT));
    }

    @Override // com.google.common.collect.AbstractMultiset
    Iterator<E> elementIterator() {
        return Multisets.elementIterator(entryIterator());
    }

    @Override // com.google.common.collect.AbstractSortedMultiset, com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ NavigableSet elementSet() {
        return super.elementSet();
    }

    @Override // com.google.common.collect.AbstractMultiset
    Iterator<Multiset.Entry<E>> entryIterator() {
        return new Iterator<Multiset.Entry<E>>(this) { // from class: com.google.common.collect.TreeMultiset.2

            @CheckForNull
            AvlNode<E> current;

            @CheckForNull
            Multiset.Entry<E> prevEntry;
            final TreeMultiset this$0;

            {
                this.this$0 = this;
                this.current = this.this$0.firstNode();
            }

            @Override // java.util.Iterator
            public boolean hasNext() {
                if (this.current == null) {
                    return false;
                }
                if (!this.this$0.range.tooHigh(this.current.getElement())) {
                    return true;
                }
                this.current = null;
                return false;
            }

            @Override // java.util.Iterator
            public Multiset.Entry<E> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Multiset.Entry<E> entryWrapEntry = this.this$0.wrapEntry((AvlNode) Objects.requireNonNull(this.current));
                this.prevEntry = entryWrapEntry;
                if (this.current.succ() == this.this$0.header) {
                    this.current = null;
                } else {
                    this.current = this.current.succ();
                }
                return entryWrapEntry;
            }

            @Override // java.util.Iterator
            public void remove() {
                Preconditions.checkState(this.prevEntry != null, "no calls to next() since the last call to remove()");
                this.this$0.setCount(this.prevEntry.getElement(), 0);
                this.prevEntry = null;
            }
        };
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public /* bridge */ /* synthetic */ Set entrySet() {
        return super.entrySet();
    }

    @Override // com.google.common.collect.AbstractSortedMultiset, com.google.common.collect.SortedMultiset
    @CheckForNull
    public /* bridge */ /* synthetic */ Multiset.Entry firstEntry() {
        return super.firstEntry();
    }

    @Override // com.google.common.collect.SortedMultiset
    public SortedMultiset<E> headMultiset(@ParametricNullness E e, BoundType boundType) {
        return new TreeMultiset(this.rootReference, this.range.intersect(GeneralRange.upTo(comparator(), e, boundType)), this.header);
    }

    @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
    public /* bridge */ /* synthetic */ boolean isEmpty() {
        return super.isEmpty();
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, com.google.common.collect.Multiset
    public Iterator<E> iterator() {
        return Multisets.iteratorImpl(this);
    }

    @Override // com.google.common.collect.AbstractSortedMultiset, com.google.common.collect.SortedMultiset
    @CheckForNull
    public /* bridge */ /* synthetic */ Multiset.Entry lastEntry() {
        return super.lastEntry();
    }

    @Override // com.google.common.collect.AbstractSortedMultiset, com.google.common.collect.SortedMultiset
    @CheckForNull
    public /* bridge */ /* synthetic */ Multiset.Entry pollFirstEntry() {
        return super.pollFirstEntry();
    }

    @Override // com.google.common.collect.AbstractSortedMultiset, com.google.common.collect.SortedMultiset
    @CheckForNull
    public /* bridge */ /* synthetic */ Multiset.Entry pollLastEntry() {
        return super.pollLastEntry();
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public int remove(@CheckForNull Object obj, int i) {
        CollectPreconditions.checkNonnegative(i, "occurrences");
        if (i == 0) {
            return count(obj);
        }
        AvlNode<E> avlNode = this.rootReference.get();
        int[] iArr = new int[1];
        try {
            if (!this.range.contains(obj) || avlNode == null) {
                return 0;
            }
            this.rootReference.checkAndSet(avlNode, avlNode.remove(comparator(), obj, i, iArr));
            return iArr[0];
        } catch (ClassCastException | NullPointerException e) {
            return 0;
        }
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public int setCount(@ParametricNullness E e, int i) {
        CollectPreconditions.checkNonnegative(i, "count");
        if (!this.range.contains(e)) {
            Preconditions.checkArgument(i == 0);
            return 0;
        }
        AvlNode<E> avlNode = this.rootReference.get();
        if (avlNode != null) {
            int[] iArr = new int[1];
            this.rootReference.checkAndSet(avlNode, avlNode.setCount(comparator(), e, i, iArr));
            return iArr[0];
        }
        if (i <= 0) {
            return 0;
        }
        add(e, i);
        return 0;
    }

    @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
    public boolean setCount(@ParametricNullness E e, int i, int i2) {
        CollectPreconditions.checkNonnegative(i2, "newCount");
        CollectPreconditions.checkNonnegative(i, "oldCount");
        Preconditions.checkArgument(this.range.contains(e));
        AvlNode<E> avlNode = this.rootReference.get();
        if (avlNode != null) {
            int[] iArr = new int[1];
            this.rootReference.checkAndSet(avlNode, avlNode.setCount(comparator(), e, i, i2, iArr));
            return iArr[0] == i;
        }
        if (i != 0) {
            return false;
        }
        if (i2 <= 0) {
            return true;
        }
        add(e, i2);
        return true;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public int size() {
        return Ints.saturatedCast(aggregateForEntries(Aggregate.SIZE));
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.AbstractSortedMultiset, com.google.common.collect.SortedMultiset
    public /* bridge */ /* synthetic */ SortedMultiset subMultiset(@ParametricNullness Object obj, BoundType boundType, @ParametricNullness Object obj2, BoundType boundType2) {
        return super.subMultiset(obj, boundType, obj2, boundType2);
    }

    @Override // com.google.common.collect.SortedMultiset
    public SortedMultiset<E> tailMultiset(@ParametricNullness E e, BoundType boundType) {
        return new TreeMultiset(this.rootReference, this.range.intersect(GeneralRange.downTo(comparator(), e, boundType)), this.header);
    }
}
