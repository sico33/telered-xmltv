package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class MinMaxPriorityQueue<E> extends AbstractQueue<E> {
    private static final int DEFAULT_CAPACITY = 11;
    private static final int EVEN_POWERS_OF_TWO = 1431655765;
    private static final int ODD_POWERS_OF_TWO = -1431655766;
    private final MinMaxPriorityQueue<E>.Heap maxHeap;
    final int maximumSize;
    private final MinMaxPriorityQueue<E>.Heap minHeap;
    private int modCount;
    private Object[] queue;
    private int size;

    public static final class Builder<B> {
        private static final int UNSET_EXPECTED_SIZE = -1;
        private final Comparator<B> comparator;
        private int expectedSize;
        private int maximumSize;

        private Builder(Comparator<B> comparator) {
            this.expectedSize = -1;
            this.maximumSize = Integer.MAX_VALUE;
            this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public <T extends B> Ordering<T> ordering() {
            return Ordering.from(this.comparator);
        }

        public <T extends B> MinMaxPriorityQueue<T> create() {
            return create(Collections.emptySet());
        }

        public <T extends B> MinMaxPriorityQueue<T> create(Iterable<? extends T> iterable) {
            MinMaxPriorityQueue<T> minMaxPriorityQueue = new MinMaxPriorityQueue<>(this, MinMaxPriorityQueue.initialQueueSize(this.expectedSize, this.maximumSize, iterable));
            Iterator<? extends T> it = iterable.iterator();
            while (it.hasNext()) {
                minMaxPriorityQueue.offer(it.next());
            }
            return minMaxPriorityQueue;
        }

        public Builder<B> expectedSize(int i) {
            Preconditions.checkArgument(i >= 0);
            this.expectedSize = i;
            return this;
        }

        public Builder<B> maximumSize(int i) {
            Preconditions.checkArgument(i > 0);
            this.maximumSize = i;
            return this;
        }
    }

    class Heap {
        final Ordering<E> ordering;
        MinMaxPriorityQueue<E>.Heap otherHeap;
        final MinMaxPriorityQueue this$0;

        Heap(MinMaxPriorityQueue minMaxPriorityQueue, Ordering<E> ordering) {
            this.this$0 = minMaxPriorityQueue;
            this.ordering = ordering;
        }

        private int getGrandparentIndex(int i) {
            return getParentIndex(getParentIndex(i));
        }

        private int getLeftChildIndex(int i) {
            return (i * 2) + 1;
        }

        private int getParentIndex(int i) {
            return (i - 1) / 2;
        }

        private int getRightChildIndex(int i) {
            return (i * 2) + 2;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean verifyIndex(int i) {
            if (getLeftChildIndex(i) < this.this$0.size && compareElements(i, getLeftChildIndex(i)) > 0) {
                return false;
            }
            if (getRightChildIndex(i) < this.this$0.size && compareElements(i, getRightChildIndex(i)) > 0) {
                return false;
            }
            if (i <= 0 || compareElements(i, getParentIndex(i)) <= 0) {
                return i <= 2 || compareElements(getGrandparentIndex(i), i) <= 0;
            }
            return false;
        }

        void bubbleUp(int i, E e) {
            int iCrossOverUp = crossOverUp(i, e);
            if (iCrossOverUp != i) {
                this = this.otherHeap;
                i = iCrossOverUp;
            }
            this.bubbleUpAlternatingLevels(i, e);
        }

        int bubbleUpAlternatingLevels(int i, E e) {
            while (i > 2) {
                int grandparentIndex = getGrandparentIndex(i);
                Object objElementData = this.this$0.elementData(grandparentIndex);
                if (this.ordering.compare((E) objElementData, e) <= 0) {
                    break;
                }
                this.this$0.queue[i] = objElementData;
                i = grandparentIndex;
            }
            this.this$0.queue[i] = e;
            return i;
        }

        int compareElements(int i, int i2) {
            return this.ordering.compare((E) this.this$0.elementData(i), (E) this.this$0.elementData(i2));
        }

        int crossOver(int i, E e) {
            int iFindMinChild = findMinChild(i);
            if (iFindMinChild <= 0 || this.ordering.compare((E) this.this$0.elementData(iFindMinChild), e) >= 0) {
                return crossOverUp(i, e);
            }
            this.this$0.queue[i] = this.this$0.elementData(iFindMinChild);
            this.this$0.queue[iFindMinChild] = e;
            return iFindMinChild;
        }

        /* JADX WARN: Code duplicated, block: B:19:0x005e  */
        int crossOverUp(int i, E e) {
            Object objElementData;
            int i2;
            int rightChildIndex;
            if (i == 0) {
                this.this$0.queue[0] = e;
                return 0;
            }
            int parentIndex = getParentIndex(i);
            Object objElementData2 = this.this$0.elementData(parentIndex);
            if (parentIndex == 0 || (rightChildIndex = getRightChildIndex(getParentIndex(parentIndex))) == parentIndex || getLeftChildIndex(rightChildIndex) < this.this$0.size) {
                objElementData = objElementData2;
                i2 = parentIndex;
            } else {
                objElementData = this.this$0.elementData(rightChildIndex);
                if (this.ordering.compare((E) objElementData, (E) objElementData2) < 0) {
                    i2 = rightChildIndex;
                } else {
                    objElementData = objElementData2;
                    i2 = parentIndex;
                }
            }
            int iCompare = this.ordering.compare((E) objElementData, e);
            MinMaxPriorityQueue minMaxPriorityQueue = this.this$0;
            if (iCompare >= 0) {
                minMaxPriorityQueue.queue[i] = e;
                return i;
            }
            minMaxPriorityQueue.queue[i] = objElementData;
            this.this$0.queue[i2] = e;
            return i2;
        }

        int fillHoleAt(int i) {
            while (true) {
                int iFindMinGrandChild = findMinGrandChild(i);
                if (iFindMinGrandChild <= 0) {
                    return i;
                }
                this.this$0.queue[i] = this.this$0.elementData(iFindMinGrandChild);
                i = iFindMinGrandChild;
            }
        }

        int findMin(int i, int i2) {
            if (i >= this.this$0.size) {
                return -1;
            }
            Preconditions.checkState(i > 0);
            int iMin = Math.min(i, this.this$0.size - i2);
            int i3 = i;
            for (int i4 = i + 1; i4 < iMin + i2; i4++) {
                if (compareElements(i4, i3) < 0) {
                    i3 = i4;
                }
            }
            return i3;
        }

        int findMinChild(int i) {
            return findMin(getLeftChildIndex(i), 2);
        }

        int findMinGrandChild(int i) {
            int leftChildIndex = getLeftChildIndex(i);
            if (leftChildIndex < 0) {
                return -1;
            }
            return findMin(getLeftChildIndex(leftChildIndex), 4);
        }

        int swapWithConceptuallyLastElement(E e) {
            int rightChildIndex;
            int parentIndex = getParentIndex(this.this$0.size);
            if (parentIndex != 0 && (rightChildIndex = getRightChildIndex(getParentIndex(parentIndex))) != parentIndex && getLeftChildIndex(rightChildIndex) >= this.this$0.size) {
                Object objElementData = this.this$0.elementData(rightChildIndex);
                if (this.ordering.compare((E) objElementData, e) < 0) {
                    this.this$0.queue[rightChildIndex] = e;
                    this.this$0.queue[this.this$0.size] = objElementData;
                    return rightChildIndex;
                }
            }
            return this.this$0.size;
        }

        @CheckForNull
        MoveDesc<E> tryCrossOverAndBubbleUp(int i, int i2, E e) {
            int iCrossOver = crossOver(i2, e);
            if (iCrossOver == i2) {
                return null;
            }
            MinMaxPriorityQueue minMaxPriorityQueue = this.this$0;
            Object objElementData = iCrossOver < i ? minMaxPriorityQueue.elementData(i) : minMaxPriorityQueue.elementData(getParentIndex(i));
            if (this.otherHeap.bubbleUpAlternatingLevels(iCrossOver, e) < i) {
                return new MoveDesc<>(e, objElementData);
            }
            return null;
        }
    }

    static class MoveDesc<E> {
        final E replaced;
        final E toTrickle;

        MoveDesc(E e, E e2) {
            this.toTrickle = e;
            this.replaced = e2;
        }
    }

    private class QueueIterator implements Iterator<E> {
        private boolean canRemove;
        private int cursor;
        private int expectedModCount;

        @CheckForNull
        private Queue<E> forgetMeNot;

        @CheckForNull
        private E lastFromForgetMeNot;
        private int nextCursor;

        @CheckForNull
        private List<E> skipMe;
        final MinMaxPriorityQueue this$0;

        private QueueIterator(MinMaxPriorityQueue minMaxPriorityQueue) {
            this.this$0 = minMaxPriorityQueue;
            this.cursor = -1;
            this.nextCursor = -1;
            this.expectedModCount = this.this$0.modCount;
        }

        private void checkModCount() {
            if (this.this$0.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        private boolean foundAndRemovedExactReference(Iterable<E> iterable, E e) {
            Iterator<E> it = iterable.iterator();
            while (it.hasNext()) {
                if (it.next() == e) {
                    it.remove();
                    return true;
                }
            }
            return false;
        }

        /* JADX WARN: Multi-variable type inference failed */
        private void nextNotInSkipMe(int i) {
            if (this.nextCursor < i) {
                if (this.skipMe != null) {
                    while (i < this.this$0.size() && foundAndRemovedExactReference(this.skipMe, this.this$0.elementData(i))) {
                        i++;
                    }
                }
                this.nextCursor = i;
            }
        }

        private boolean removeExact(Object obj) {
            for (int i = 0; i < this.this$0.size; i++) {
                if (this.this$0.queue[i] == obj) {
                    this.this$0.removeAt(i);
                    return true;
                }
            }
            return false;
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            checkModCount();
            nextNotInSkipMe(this.cursor + 1);
            if (this.nextCursor >= this.this$0.size()) {
                return (this.forgetMeNot == null || this.forgetMeNot.isEmpty()) ? false : true;
            }
            return true;
        }

        @Override // java.util.Iterator
        public E next() {
            checkModCount();
            nextNotInSkipMe(this.cursor + 1);
            if (this.nextCursor < this.this$0.size()) {
                this.cursor = this.nextCursor;
                this.canRemove = true;
                return (E) this.this$0.elementData(this.cursor);
            }
            if (this.forgetMeNot != null) {
                this.cursor = this.this$0.size();
                this.lastFromForgetMeNot = this.forgetMeNot.poll();
                if (this.lastFromForgetMeNot != null) {
                    this.canRemove = true;
                    return this.lastFromForgetMeNot;
                }
            }
            throw new NoSuchElementException("iterator moved past last element in queue.");
        }

        @Override // java.util.Iterator
        public void remove() {
            CollectPreconditions.checkRemove(this.canRemove);
            checkModCount();
            this.canRemove = false;
            this.expectedModCount++;
            if (this.cursor >= this.this$0.size()) {
                Preconditions.checkState(removeExact(Objects.requireNonNull(this.lastFromForgetMeNot)));
                this.lastFromForgetMeNot = null;
                return;
            }
            MoveDesc<E> moveDescRemoveAt = this.this$0.removeAt(this.cursor);
            if (moveDescRemoveAt != null) {
                if (this.forgetMeNot == null || this.skipMe == null) {
                    this.forgetMeNot = new ArrayDeque();
                    this.skipMe = new ArrayList(3);
                }
                if (!foundAndRemovedExactReference(this.skipMe, moveDescRemoveAt.toTrickle)) {
                    this.forgetMeNot.add(moveDescRemoveAt.toTrickle);
                }
                if (!foundAndRemovedExactReference(this.forgetMeNot, moveDescRemoveAt.replaced)) {
                    this.skipMe.add(moveDescRemoveAt.replaced);
                }
            }
            this.cursor--;
            this.nextCursor--;
        }
    }

    private MinMaxPriorityQueue(Builder<? super E> builder, int i) {
        Ordering ordering = builder.ordering();
        this.minHeap = new Heap(this, ordering);
        this.maxHeap = new Heap(this, ordering.reverse());
        this.minHeap.otherHeap = this.maxHeap;
        this.maxHeap.otherHeap = this.minHeap;
        this.maximumSize = ((Builder) builder).maximumSize;
        this.queue = new Object[i];
    }

    private int calculateNewCapacity() {
        int length = this.queue.length;
        return capAtMaximumSize(length < 64 ? (length + 1) * 2 : IntMath.checkedMultiply(length / 2, 3), this.maximumSize);
    }

    private static int capAtMaximumSize(int i, int i2) {
        return Math.min(i - 1, i2) + 1;
    }

    public static <E extends Comparable<E>> MinMaxPriorityQueue<E> create() {
        return new Builder(Ordering.natural()).create();
    }

    public static <E extends Comparable<E>> MinMaxPriorityQueue<E> create(Iterable<? extends E> iterable) {
        return new Builder(Ordering.natural()).create(iterable);
    }

    public static Builder<Comparable> expectedSize(int i) {
        return new Builder(Ordering.natural()).expectedSize(i);
    }

    @CheckForNull
    private MoveDesc<E> fillHole(int i, E e) {
        MinMaxPriorityQueue<E>.Heap heapHeapForIndex = heapForIndex(i);
        int iFillHoleAt = heapHeapForIndex.fillHoleAt(i);
        int iBubbleUpAlternatingLevels = heapHeapForIndex.bubbleUpAlternatingLevels(iFillHoleAt, e);
        if (iBubbleUpAlternatingLevels == iFillHoleAt) {
            return heapHeapForIndex.tryCrossOverAndBubbleUp(i, iFillHoleAt, e);
        }
        if (iBubbleUpAlternatingLevels < i) {
            return new MoveDesc<>(e, elementData(i));
        }
        return null;
    }

    private int getMaxElementIndex() {
        switch (this.size) {
            case 1:
                return 0;
            case 2:
                return 1;
            default:
                return this.maxHeap.compareElements(1, 2) <= 0 ? 1 : 2;
        }
    }

    private void growIfNeeded() {
        if (this.size > this.queue.length) {
            Object[] objArr = new Object[calculateNewCapacity()];
            System.arraycopy(this.queue, 0, objArr, 0, this.queue.length);
            this.queue = objArr;
        }
    }

    private MinMaxPriorityQueue<E>.Heap heapForIndex(int i) {
        return isEvenLevel(i) ? this.minHeap : this.maxHeap;
    }

    static int initialQueueSize(int i, int i2, Iterable<?> iterable) {
        if (i == -1) {
            i = 11;
        }
        if (iterable instanceof Collection) {
            i = Math.max(i, ((Collection) iterable).size());
        }
        return capAtMaximumSize(i, i2);
    }

    static boolean isEvenLevel(int i) {
        int i2 = ((i + 1) ^ (-1)) ^ (-1);
        Preconditions.checkState(i2 > 0, "negative index");
        return (EVEN_POWERS_OF_TWO & i2) > (i2 & ODD_POWERS_OF_TWO);
    }

    public static Builder<Comparable> maximumSize(int i) {
        return new Builder(Ordering.natural()).maximumSize(i);
    }

    public static <B> Builder<B> orderedBy(Comparator<B> comparator) {
        return new Builder<>(comparator);
    }

    private E removeAndGet(int i) {
        E eElementData = elementData(i);
        removeAt(i);
        return eElementData;
    }

    @Override // java.util.AbstractQueue, java.util.AbstractCollection, java.util.Collection, java.util.Queue
    public boolean add(E e) {
        offer(e);
        return true;
    }

    @Override // java.util.AbstractQueue, java.util.AbstractCollection, java.util.Collection
    public boolean addAll(Collection<? extends E> collection) {
        boolean z = false;
        Iterator<? extends E> it = collection.iterator();
        while (it.hasNext()) {
            offer(it.next());
            z = true;
        }
        return z;
    }

    int capacity() {
        return this.queue.length;
    }

    @Override // java.util.AbstractQueue, java.util.AbstractCollection, java.util.Collection
    public void clear() {
        for (int i = 0; i < this.size; i++) {
            this.queue[i] = null;
        }
        this.size = 0;
    }

    public Comparator<? super E> comparator() {
        return this.minHeap.ordering;
    }

    E elementData(int i) {
        return (E) Objects.requireNonNull(this.queue[i]);
    }

    boolean isIntact() {
        for (int i = 1; i < this.size; i++) {
            if (!heapForIndex(i).verifyIndex(i)) {
                return false;
            }
        }
        return true;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable
    public Iterator<E> iterator() {
        return new QueueIterator();
    }

    @Override // java.util.Queue
    public boolean offer(E e) {
        Preconditions.checkNotNull(e);
        this.modCount++;
        int i = this.size;
        this.size = i + 1;
        growIfNeeded();
        heapForIndex(i).bubbleUp(i, e);
        return this.size <= this.maximumSize || pollLast() != e;
    }

    @Override // java.util.Queue
    @CheckForNull
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return elementData(0);
    }

    @CheckForNull
    public E peekFirst() {
        return peek();
    }

    @CheckForNull
    public E peekLast() {
        if (isEmpty()) {
            return null;
        }
        return elementData(getMaxElementIndex());
    }

    @Override // java.util.Queue
    @CheckForNull
    public E poll() {
        if (isEmpty()) {
            return null;
        }
        return removeAndGet(0);
    }

    @CheckForNull
    public E pollFirst() {
        return poll();
    }

    @CheckForNull
    public E pollLast() {
        if (isEmpty()) {
            return null;
        }
        return removeAndGet(getMaxElementIndex());
    }

    @CheckForNull
    MoveDesc<E> removeAt(int i) {
        Preconditions.checkPositionIndex(i, this.size);
        this.modCount++;
        this.size--;
        if (this.size == i) {
            this.queue[this.size] = null;
            return null;
        }
        E eElementData = elementData(this.size);
        int iSwapWithConceptuallyLastElement = heapForIndex(this.size).swapWithConceptuallyLastElement(eElementData);
        if (iSwapWithConceptuallyLastElement == i) {
            this.queue[this.size] = null;
            return null;
        }
        E eElementData2 = elementData(this.size);
        this.queue[this.size] = null;
        MoveDesc<E> moveDescFillHole = fillHole(i, eElementData2);
        if (iSwapWithConceptuallyLastElement < i) {
            return moveDescFillHole == null ? new MoveDesc<>(eElementData, eElementData2) : new MoveDesc<>(eElementData, moveDescFillHole.replaced);
        }
        return moveDescFillHole;
    }

    public E removeFirst() {
        return remove();
    }

    public E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return removeAndGet(getMaxElementIndex());
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public int size() {
        return this.size;
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public Object[] toArray() {
        Object[] objArr = new Object[this.size];
        System.arraycopy(this.queue, 0, objArr, 0, this.size);
        return objArr;
    }
}
