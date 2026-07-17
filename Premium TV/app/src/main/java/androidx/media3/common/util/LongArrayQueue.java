package androidx.media3.common.util;

import java.util.NoSuchElementException;

/* JADX INFO: loaded from: classes.dex */
public final class LongArrayQueue {
    public static final int DEFAULT_INITIAL_CAPACITY = 16;
    private long[] data;
    private int headIndex;
    private int size;
    private int tailIndex;
    private int wrapAroundMask;

    public LongArrayQueue() {
        this(16);
    }

    public LongArrayQueue(int minCapacity) {
        Assertions.checkArgument(minCapacity >= 0 && minCapacity <= 1073741824);
        int minCapacity2 = minCapacity == 0 ? 1 : minCapacity;
        minCapacity2 = Integer.bitCount(minCapacity2) != 1 ? Integer.highestOneBit(minCapacity2 - 1) << 1 : minCapacity2;
        this.headIndex = 0;
        this.tailIndex = -1;
        this.size = 0;
        this.data = new long[minCapacity2];
        this.wrapAroundMask = this.data.length - 1;
    }

    public void add(long value) {
        if (this.size == this.data.length) {
            doubleArraySize();
        }
        this.tailIndex = (this.tailIndex + 1) & this.wrapAroundMask;
        this.data[this.tailIndex] = value;
        this.size++;
    }

    public long remove() {
        if (this.size == 0) {
            throw new NoSuchElementException();
        }
        long value = this.data[this.headIndex];
        this.headIndex = (this.headIndex + 1) & this.wrapAroundMask;
        this.size--;
        return value;
    }

    public long element() {
        if (this.size == 0) {
            throw new NoSuchElementException();
        }
        return this.data[this.headIndex];
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public void clear() {
        this.headIndex = 0;
        this.tailIndex = -1;
        this.size = 0;
    }

    int capacity() {
        return this.data.length;
    }

    private void doubleArraySize() {
        int newCapacity = this.data.length << 1;
        if (newCapacity < 0) {
            throw new IllegalStateException();
        }
        long[] newData = new long[newCapacity];
        int itemsToRight = this.data.length - this.headIndex;
        int itemsToLeft = this.headIndex;
        System.arraycopy(this.data, this.headIndex, newData, 0, itemsToRight);
        System.arraycopy(this.data, 0, newData, itemsToRight, itemsToLeft);
        this.headIndex = 0;
        this.tailIndex = this.size - 1;
        this.data = newData;
        this.wrapAroundMask = this.data.length - 1;
    }
}
