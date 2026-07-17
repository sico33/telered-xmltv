package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class TopKSelector<T> {
    private final T[] buffer;
    private int bufferSize;
    private final Comparator<? super T> comparator;
    private final int k;

    @CheckForNull
    private T threshold;

    private TopKSelector(Comparator<? super T> comparator, int i) {
        this.comparator = (Comparator) Preconditions.checkNotNull(comparator, "comparator");
        this.k = i;
        Preconditions.checkArgument(i >= 0, "k (%s) must be >= 0", i);
        Preconditions.checkArgument(i <= 1073741823, "k (%s) must be <= Integer.MAX_VALUE / 2", i);
        this.buffer = (T[]) new Object[IntMath.checkedMultiply(i, 2)];
        this.bufferSize = 0;
        this.threshold = null;
    }

    public static <T extends Comparable<? super T>> TopKSelector<T> greatest(int i) {
        return greatest(i, Ordering.natural());
    }

    public static <T> TopKSelector<T> greatest(int i, Comparator<? super T> comparator) {
        return new TopKSelector<>(Ordering.from(comparator).reverse(), i);
    }

    public static <T extends Comparable<? super T>> TopKSelector<T> least(int i) {
        return least(i, Ordering.natural());
    }

    public static <T> TopKSelector<T> least(int i, Comparator<? super T> comparator) {
        return new TopKSelector<>(comparator, i);
    }

    private int partition(int i, int i2, int i3) {
        Object objUncheckedCastNullableTToT = NullnessCasts.uncheckedCastNullableTToT(this.buffer[i3]);
        this.buffer[i3] = this.buffer[i2];
        int i4 = i;
        while (i < i2) {
            if (this.comparator.compare((Object) NullnessCasts.uncheckedCastNullableTToT(this.buffer[i]), objUncheckedCastNullableTToT) < 0) {
                swap(i4, i);
                i4++;
            }
            i++;
        }
        this.buffer[i2] = this.buffer[i4];
        ((T[]) this.buffer)[i4] = objUncheckedCastNullableTToT;
        return i4;
    }

    private void swap(int i, int i2) {
        T t = this.buffer[i];
        this.buffer[i] = this.buffer[i2];
        this.buffer[i2] = t;
    }

    private void trim() {
        int i;
        int i2;
        int i3 = 0;
        int i4 = (this.k * 2) - 1;
        int iLog2 = IntMath.log2(i4 + 0, RoundingMode.CEILING);
        int i5 = 0;
        int i6 = 0;
        while (i6 < i4) {
            int iPartition = partition(i6, i4, ((i6 + i4) + 1) >>> 1);
            if (iPartition <= this.k) {
                if (iPartition >= this.k) {
                    break;
                }
                int iMax = Math.max(iPartition, i6 + 1);
                i = i4;
                i2 = iMax;
                i3 = iPartition;
            } else {
                i = iPartition - 1;
                i2 = i6;
            }
            int i7 = i5 + 1;
            if (i7 >= iLog2 * 3) {
                Arrays.sort(this.buffer, i2, i + 1, this.comparator);
                break;
            } else {
                i5 = i7;
                i6 = i2;
                i4 = i;
            }
        }
        this.bufferSize = this.k;
        this.threshold = (T) NullnessCasts.uncheckedCastNullableTToT(this.buffer[i3]);
        while (true) {
            i3++;
            if (i3 >= this.k) {
                return;
            }
            if (this.comparator.compare((Object) NullnessCasts.uncheckedCastNullableTToT(this.buffer[i3]), (Object) NullnessCasts.uncheckedCastNullableTToT(this.threshold)) > 0) {
                this.threshold = this.buffer[i3];
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    TopKSelector<T> combine(TopKSelector<T> topKSelector) {
        for (int i = 0; i < topKSelector.bufferSize; i++) {
            offer(NullnessCasts.uncheckedCastNullableTToT(topKSelector.buffer[i]));
        }
        return this;
    }

    public void offer(@ParametricNullness T t) {
        if (this.k == 0) {
            return;
        }
        if (this.bufferSize == 0) {
            this.buffer[0] = t;
            this.threshold = t;
            this.bufferSize = 1;
            return;
        }
        if (this.bufferSize < this.k) {
            T[] tArr = this.buffer;
            int i = this.bufferSize;
            this.bufferSize = i + 1;
            tArr[i] = t;
            if (this.comparator.compare(t, (Object) NullnessCasts.uncheckedCastNullableTToT(this.threshold)) > 0) {
                this.threshold = t;
                return;
            }
            return;
        }
        if (this.comparator.compare(t, (Object) NullnessCasts.uncheckedCastNullableTToT(this.threshold)) < 0) {
            T[] tArr2 = this.buffer;
            int i2 = this.bufferSize;
            this.bufferSize = i2 + 1;
            tArr2[i2] = t;
            if (this.bufferSize == this.k * 2) {
                trim();
            }
        }
    }

    public void offerAll(Iterable<? extends T> iterable) {
        offerAll(iterable.iterator());
    }

    public void offerAll(Iterator<? extends T> it) {
        while (it.hasNext()) {
            offer(it.next());
        }
    }

    public List<T> topK() {
        T[] tArr = this.buffer;
        Arrays.sort(tArr, 0, this.bufferSize, this.comparator);
        if (this.bufferSize > this.k) {
            Arrays.fill(this.buffer, this.k, this.buffer.length, (Object) null);
            this.bufferSize = this.k;
            this.threshold = this.buffer[this.k - 1];
        }
        return Collections.unmodifiableList(Arrays.asList(Arrays.copyOf(tArr, this.bufferSize)));
    }
}
