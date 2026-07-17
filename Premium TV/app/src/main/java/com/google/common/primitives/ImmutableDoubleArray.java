package com.google.common.primitives;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
@Immutable
public final class ImmutableDoubleArray implements Serializable {
    private static final ImmutableDoubleArray EMPTY = new ImmutableDoubleArray(new double[0]);
    private final double[] array;
    private final int end;
    private final transient int start;

    static class AsList extends AbstractList<Double> implements RandomAccess, Serializable {
        private final ImmutableDoubleArray parent;

        private AsList(ImmutableDoubleArray immutableDoubleArray) {
            this.parent = immutableDoubleArray;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
        public boolean contains(@CheckForNull Object obj) {
            return indexOf(obj) >= 0;
        }

        @Override // java.util.AbstractList, java.util.Collection, java.util.List
        public boolean equals(@CheckForNull Object obj) {
            if (obj instanceof AsList) {
                return this.parent.equals(((AsList) obj).parent);
            }
            if (!(obj instanceof List)) {
                return false;
            }
            List list = (List) obj;
            if (size() != list.size()) {
                return false;
            }
            int i = this.parent.start;
            Iterator it = list.iterator();
            while (true) {
                int i2 = i;
                if (!it.hasNext()) {
                    return true;
                }
                Object next = it.next();
                if (!(next instanceof Double) || !ImmutableDoubleArray.areEqual(this.parent.array[i2], ((Double) next).doubleValue())) {
                    break;
                }
                i = i2 + 1;
            }
            return false;
        }

        @Override // java.util.AbstractList, java.util.List
        public Double get(int i) {
            return Double.valueOf(this.parent.get(i));
        }

        @Override // java.util.AbstractList, java.util.Collection, java.util.List
        public int hashCode() {
            return this.parent.hashCode();
        }

        @Override // java.util.AbstractList, java.util.List
        public int indexOf(@CheckForNull Object obj) {
            if (obj instanceof Double) {
                return this.parent.indexOf(((Double) obj).doubleValue());
            }
            return -1;
        }

        @Override // java.util.AbstractList, java.util.List
        public int lastIndexOf(@CheckForNull Object obj) {
            if (obj instanceof Double) {
                return this.parent.lastIndexOf(((Double) obj).doubleValue());
            }
            return -1;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
        public int size() {
            return this.parent.length();
        }

        @Override // java.util.AbstractList, java.util.List
        public List<Double> subList(int i, int i2) {
            return this.parent.subArray(i, i2).asList();
        }

        @Override // java.util.AbstractCollection
        public String toString() {
            return this.parent.toString();
        }
    }

    public static final class Builder {
        private double[] array;
        private int count = 0;

        Builder(int i) {
            this.array = new double[i];
        }

        private void ensureRoomFor(int i) {
            int i2 = this.count + i;
            if (i2 > this.array.length) {
                this.array = Arrays.copyOf(this.array, expandedCapacity(this.array.length, i2));
            }
        }

        private static int expandedCapacity(int i, int i2) {
            if (i2 < 0) {
                throw new AssertionError("cannot store more than MAX_VALUE elements");
            }
            int iHighestOneBit = (i >> 1) + i + 1;
            if (iHighestOneBit < i2) {
                iHighestOneBit = Integer.highestOneBit(i2 - 1) << 1;
            }
            if (iHighestOneBit < 0) {
                return Integer.MAX_VALUE;
            }
            return iHighestOneBit;
        }

        public Builder add(double d) {
            ensureRoomFor(1);
            this.array[this.count] = d;
            this.count++;
            return this;
        }

        public Builder addAll(ImmutableDoubleArray immutableDoubleArray) {
            ensureRoomFor(immutableDoubleArray.length());
            System.arraycopy(immutableDoubleArray.array, immutableDoubleArray.start, this.array, this.count, immutableDoubleArray.length());
            this.count += immutableDoubleArray.length();
            return this;
        }

        public Builder addAll(Iterable<Double> iterable) {
            if (iterable instanceof Collection) {
                return addAll((Collection<Double>) iterable);
            }
            Iterator<Double> it = iterable.iterator();
            while (it.hasNext()) {
                add(it.next().doubleValue());
            }
            return this;
        }

        public Builder addAll(Collection<Double> collection) {
            ensureRoomFor(collection.size());
            for (Double d : collection) {
                double[] dArr = this.array;
                int i = this.count;
                this.count = i + 1;
                dArr[i] = d.doubleValue();
            }
            return this;
        }

        public Builder addAll(double[] dArr) {
            ensureRoomFor(dArr.length);
            System.arraycopy(dArr, 0, this.array, this.count, dArr.length);
            this.count += dArr.length;
            return this;
        }

        public ImmutableDoubleArray build() {
            return this.count == 0 ? ImmutableDoubleArray.EMPTY : new ImmutableDoubleArray(this.array, 0, this.count);
        }
    }

    private ImmutableDoubleArray(double[] dArr) {
        this(dArr, 0, dArr.length);
    }

    private ImmutableDoubleArray(double[] dArr, int i, int i2) {
        this.array = dArr;
        this.start = i;
        this.end = i2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean areEqual(double d, double d2) {
        return Double.doubleToLongBits(d) == Double.doubleToLongBits(d2);
    }

    public static Builder builder() {
        return new Builder(10);
    }

    public static Builder builder(int i) {
        Preconditions.checkArgument(i >= 0, "Invalid initialCapacity: %s", i);
        return new Builder(i);
    }

    public static ImmutableDoubleArray copyOf(Iterable<Double> iterable) {
        return iterable instanceof Collection ? copyOf((Collection<Double>) iterable) : builder().addAll(iterable).build();
    }

    public static ImmutableDoubleArray copyOf(Collection<Double> collection) {
        return collection.isEmpty() ? EMPTY : new ImmutableDoubleArray(Doubles.toArray(collection));
    }

    public static ImmutableDoubleArray copyOf(double[] dArr) {
        return dArr.length == 0 ? EMPTY : new ImmutableDoubleArray(Arrays.copyOf(dArr, dArr.length));
    }

    private boolean isPartialView() {
        return this.start > 0 || this.end < this.array.length;
    }

    public static ImmutableDoubleArray of() {
        return EMPTY;
    }

    public static ImmutableDoubleArray of(double d) {
        return new ImmutableDoubleArray(new double[]{d});
    }

    public static ImmutableDoubleArray of(double d, double d2) {
        return new ImmutableDoubleArray(new double[]{d, d2});
    }

    public static ImmutableDoubleArray of(double d, double d2, double d3) {
        return new ImmutableDoubleArray(new double[]{d, d2, d3});
    }

    public static ImmutableDoubleArray of(double d, double d2, double d3, double d4) {
        return new ImmutableDoubleArray(new double[]{d, d2, d3, d4});
    }

    public static ImmutableDoubleArray of(double d, double d2, double d3, double d4, double d5) {
        return new ImmutableDoubleArray(new double[]{d, d2, d3, d4, d5});
    }

    public static ImmutableDoubleArray of(double d, double d2, double d3, double d4, double d5, double d6) {
        return new ImmutableDoubleArray(new double[]{d, d2, d3, d4, d5, d6});
    }

    public static ImmutableDoubleArray of(double d, double... dArr) {
        Preconditions.checkArgument(dArr.length <= 2147483646, "the total number of elements must fit in an int");
        double[] dArr2 = new double[dArr.length + 1];
        dArr2[0] = d;
        System.arraycopy(dArr, 0, dArr2, 1, dArr.length);
        return new ImmutableDoubleArray(dArr2);
    }

    public List<Double> asList() {
        return new AsList();
    }

    public boolean contains(double d) {
        return indexOf(d) >= 0;
    }

    public boolean equals(@CheckForNull Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ImmutableDoubleArray)) {
            return false;
        }
        ImmutableDoubleArray immutableDoubleArray = (ImmutableDoubleArray) obj;
        if (length() != immutableDoubleArray.length()) {
            return false;
        }
        for (int i = 0; i < length(); i++) {
            if (!areEqual(get(i), immutableDoubleArray.get(i))) {
                return false;
            }
        }
        return true;
    }

    public double get(int i) {
        Preconditions.checkElementIndex(i, length());
        return this.array[this.start + i];
    }

    public int hashCode() {
        int iHashCode = 1;
        for (int i = this.start; i < this.end; i++) {
            iHashCode = (iHashCode * 31) + Doubles.hashCode(this.array[i]);
        }
        return iHashCode;
    }

    public int indexOf(double d) {
        for (int i = this.start; i < this.end; i++) {
            if (areEqual(this.array[i], d)) {
                return i - this.start;
            }
        }
        return -1;
    }

    public boolean isEmpty() {
        return this.end == this.start;
    }

    public int lastIndexOf(double d) {
        int i = this.end;
        do {
            i--;
            if (i < this.start) {
                return -1;
            }
        } while (!areEqual(this.array[i], d));
        return i - this.start;
    }

    public int length() {
        return this.end - this.start;
    }

    Object readResolve() {
        return isEmpty() ? EMPTY : this;
    }

    public ImmutableDoubleArray subArray(int i, int i2) {
        Preconditions.checkPositionIndexes(i, i2, length());
        return i == i2 ? EMPTY : new ImmutableDoubleArray(this.array, this.start + i, this.start + i2);
    }

    public double[] toArray() {
        return Arrays.copyOfRange(this.array, this.start, this.end);
    }

    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(length() * 5);
        sb.append('[').append(this.array[this.start]);
        int i = this.start;
        while (true) {
            i++;
            if (i >= this.end) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ").append(this.array[i]);
        }
    }

    public ImmutableDoubleArray trimmed() {
        return isPartialView() ? new ImmutableDoubleArray(toArray()) : this;
    }

    Object writeReplace() {
        return trimmed();
    }
}
