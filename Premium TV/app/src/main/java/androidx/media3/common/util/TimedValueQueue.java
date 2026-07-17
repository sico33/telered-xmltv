package androidx.media3.common.util;

import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class TimedValueQueue<V> {
    private static final int INITIAL_BUFFER_SIZE = 10;
    private int first;
    private int size;
    private long[] timestamps;
    private V[] values;

    public TimedValueQueue() {
        this(10);
    }

    public TimedValueQueue(int i) {
        this.timestamps = new long[i];
        this.values = (V[]) newArray(i);
    }

    public synchronized void add(long timestamp, V value) {
        clearBufferOnTimeDiscontinuity(timestamp);
        doubleCapacityIfFull();
        addUnchecked(timestamp, value);
    }

    public synchronized void clear() {
        this.first = 0;
        this.size = 0;
        Arrays.fill(this.values, (Object) null);
    }

    public synchronized int size() {
        return this.size;
    }

    public synchronized V pollFirst() {
        return this.size == 0 ? null : popFirst();
    }

    public synchronized V pollFloor(long timestamp) {
        return poll(timestamp, true);
    }

    public synchronized V poll(long timestamp) {
        return poll(timestamp, false);
    }

    private V poll(long timestamp, boolean onlyOlder) {
        V value = null;
        long previousTimeDiff = Long.MAX_VALUE;
        while (this.size > 0) {
            long timeDiff = timestamp - this.timestamps[this.first];
            if (timeDiff < 0 && (onlyOlder || (-timeDiff) >= previousTimeDiff)) {
                break;
            }
            previousTimeDiff = timeDiff;
            value = popFirst();
        }
        return value;
    }

    private V popFirst() {
        Assertions.checkState(this.size > 0);
        V value = this.values[this.first];
        this.values[this.first] = null;
        this.first = (this.first + 1) % this.values.length;
        this.size--;
        return value;
    }

    private void clearBufferOnTimeDiscontinuity(long timestamp) {
        if (this.size > 0) {
            int last = ((this.first + this.size) - 1) % this.values.length;
            if (timestamp <= this.timestamps[last]) {
                clear();
            }
        }
    }

    private void doubleCapacityIfFull() {
        int length = this.values.length;
        if (this.size < length) {
            return;
        }
        int i = length * 2;
        long[] jArr = new long[i];
        V[] vArr = (V[]) newArray(i);
        int i2 = length - this.first;
        System.arraycopy(this.timestamps, this.first, jArr, 0, i2);
        System.arraycopy(this.values, this.first, vArr, 0, i2);
        if (this.first > 0) {
            System.arraycopy(this.timestamps, 0, jArr, i2, this.first);
            System.arraycopy(this.values, 0, vArr, i2, this.first);
        }
        this.timestamps = jArr;
        this.values = vArr;
        this.first = 0;
    }

    private void addUnchecked(long timestamp, V value) {
        int next = (this.first + this.size) % this.values.length;
        this.timestamps[next] = timestamp;
        this.values[next] = value;
        this.size++;
    }

    private static <V> V[] newArray(int i) {
        return (V[]) new Object[i];
    }
}
