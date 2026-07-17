package com.google.common.math;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Quantiles {

    public static final class Scale {
        private final int scale;

        private Scale(int i) {
            Preconditions.checkArgument(i > 0, "Quantile scale must be positive");
            this.scale = i;
        }

        public ScaleAndIndex index(int i) {
            return new ScaleAndIndex(this.scale, i);
        }

        public ScaleAndIndexes indexes(Collection<Integer> collection) {
            return new ScaleAndIndexes(this.scale, Ints.toArray(collection));
        }

        public ScaleAndIndexes indexes(int... iArr) {
            return new ScaleAndIndexes(this.scale, (int[]) iArr.clone());
        }
    }

    public static final class ScaleAndIndex {
        private final int index;
        private final int scale;

        private ScaleAndIndex(int i, int i2) {
            Quantiles.checkIndex(i2, i);
            this.scale = i;
            this.index = i2;
        }

        public double compute(Collection<? extends Number> collection) {
            return computeInPlace(Doubles.toArray(collection));
        }

        public double compute(double... dArr) {
            return computeInPlace((double[]) dArr.clone());
        }

        public double compute(int... iArr) {
            return computeInPlace(Quantiles.intsToDoubles(iArr));
        }

        public double compute(long... jArr) {
            return computeInPlace(Quantiles.longsToDoubles(jArr));
        }

        public double computeInPlace(double... dArr) {
            Preconditions.checkArgument(dArr.length > 0, "Cannot calculate quantiles of an empty dataset");
            if (Quantiles.containsNaN(dArr)) {
                return Double.NaN;
            }
            long length = ((long) this.index) * ((long) (dArr.length - 1));
            int iDivide = (int) LongMath.divide(length, this.scale, RoundingMode.DOWN);
            int i = (int) (length - (((long) iDivide) * ((long) this.scale)));
            Quantiles.selectInPlace(iDivide, dArr, 0, dArr.length - 1);
            if (i == 0) {
                return dArr[iDivide];
            }
            Quantiles.selectInPlace(iDivide + 1, dArr, iDivide + 1, dArr.length - 1);
            return Quantiles.interpolate(dArr[iDivide], dArr[iDivide + 1], i, this.scale);
        }
    }

    public static final class ScaleAndIndexes {
        private final int[] indexes;
        private final int scale;

        private ScaleAndIndexes(int i, int[] iArr) {
            for (int i2 : iArr) {
                Quantiles.checkIndex(i2, i);
            }
            Preconditions.checkArgument(iArr.length > 0, "Indexes must be a non empty array");
            this.scale = i;
            this.indexes = iArr;
        }

        public Map<Integer, Double> compute(Collection<? extends Number> collection) {
            return computeInPlace(Doubles.toArray(collection));
        }

        public Map<Integer, Double> compute(double... dArr) {
            return computeInPlace((double[]) dArr.clone());
        }

        public Map<Integer, Double> compute(int... iArr) {
            return computeInPlace(Quantiles.intsToDoubles(iArr));
        }

        public Map<Integer, Double> compute(long... jArr) {
            return computeInPlace(Quantiles.longsToDoubles(jArr));
        }

        public Map<Integer, Double> computeInPlace(double... dArr) {
            int i;
            Preconditions.checkArgument(dArr.length > 0, "Cannot calculate quantiles of an empty dataset");
            if (Quantiles.containsNaN(dArr)) {
                LinkedHashMap linkedHashMap = new LinkedHashMap();
                for (int i2 : this.indexes) {
                    linkedHashMap.put(Integer.valueOf(i2), Double.valueOf(Double.NaN));
                }
                return Collections.unmodifiableMap(linkedHashMap);
            }
            int[] iArr = new int[this.indexes.length];
            int[] iArr2 = new int[this.indexes.length];
            int[] iArr3 = new int[this.indexes.length * 2];
            int i3 = 0;
            int i4 = 0;
            while (true) {
                int i5 = i3;
                i = i4;
                if (i5 >= this.indexes.length) {
                    break;
                }
                long length = ((long) this.indexes[i5]) * ((long) (dArr.length - 1));
                int iDivide = (int) LongMath.divide(length, this.scale, RoundingMode.DOWN);
                int i6 = (int) (length - (((long) iDivide) * ((long) this.scale)));
                iArr[i5] = iDivide;
                iArr2[i5] = i6;
                iArr3[i] = iDivide;
                i4 = i + 1;
                if (i6 != 0) {
                    iArr3[i4] = iDivide + 1;
                    i4++;
                }
                i3 = i5 + 1;
            }
            Arrays.sort(iArr3, 0, i);
            Quantiles.selectAllInPlace(iArr3, 0, i - 1, dArr, 0, dArr.length - 1);
            LinkedHashMap linkedHashMap2 = new LinkedHashMap();
            int i7 = 0;
            while (true) {
                int i8 = i7;
                if (i8 >= this.indexes.length) {
                    return Collections.unmodifiableMap(linkedHashMap2);
                }
                int i9 = iArr[i8];
                int i10 = iArr2[i8];
                int[] iArr4 = this.indexes;
                if (i10 == 0) {
                    linkedHashMap2.put(Integer.valueOf(iArr4[i8]), Double.valueOf(dArr[i9]));
                } else {
                    linkedHashMap2.put(Integer.valueOf(iArr4[i8]), Double.valueOf(Quantiles.interpolate(dArr[i9], dArr[i9 + 1], i10, this.scale)));
                }
                i7 = i8 + 1;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void checkIndex(int i, int i2) {
        if (i < 0 || i > i2) {
            throw new IllegalArgumentException("Quantile indexes must be between 0 and the scale, which is " + i2);
        }
    }

    private static int chooseNextSelection(int[] iArr, int i, int i2, int i3, int i4) {
        int i5;
        if (i == i2) {
            return i;
        }
        int i6 = (i3 + i4) >>> 1;
        int i7 = i2;
        int i8 = i;
        while (i7 > i8 + 1) {
            int i9 = (i8 + i7) >>> 1;
            if (iArr[i9] > i6) {
                i5 = i8;
            } else {
                if (iArr[i9] >= i6) {
                    return i9;
                }
                int i10 = i7;
                i5 = i9;
                i9 = i10;
            }
            i8 = i5;
            i7 = i9;
        }
        return ((i3 + i4) - iArr[i8]) - iArr[i7] > 0 ? i7 : i8;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean containsNaN(double... dArr) {
        for (double d : dArr) {
            if (Double.isNaN(d)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static double interpolate(double d, double d2, double d3, double d4) {
        if (d == Double.NEGATIVE_INFINITY) {
            return d2 == Double.POSITIVE_INFINITY ? Double.NaN : Double.NEGATIVE_INFINITY;
        }
        if (d2 == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }
        return (((d2 - d) * d3) / d4) + d;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static double[] intsToDoubles(int[] iArr) {
        int length = iArr.length;
        double[] dArr = new double[length];
        for (int i = 0; i < length; i++) {
            dArr[i] = iArr[i];
        }
        return dArr;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static double[] longsToDoubles(long[] jArr) {
        int length = jArr.length;
        double[] dArr = new double[length];
        for (int i = 0; i < length; i++) {
            dArr[i] = jArr[i];
        }
        return dArr;
    }

    public static ScaleAndIndex median() {
        return scale(2).index(1);
    }

    private static void movePivotToStartOfSlice(double[] dArr, int i, int i2) {
        int i3 = (i + i2) >>> 1;
        boolean z = dArr[i2] < dArr[i3];
        boolean z2 = dArr[i3] < dArr[i];
        boolean z3 = dArr[i2] < dArr[i];
        if (z == z2) {
            swap(dArr, i3, i);
        } else if (z != z3) {
            swap(dArr, i, i2);
        }
    }

    private static int partition(double[] dArr, int i, int i2) {
        movePivotToStartOfSlice(dArr, i, i2);
        double d = dArr[i];
        int i3 = i2;
        while (i2 > i) {
            if (dArr[i2] > d) {
                swap(dArr, i3, i2);
                i3--;
            }
            i2--;
        }
        swap(dArr, i, i3);
        return i3;
    }

    public static Scale percentiles() {
        return scale(100);
    }

    public static Scale quartiles() {
        return scale(4);
    }

    public static Scale scale(int i) {
        return new Scale(i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void selectAllInPlace(int[] iArr, int i, int i2, double[] dArr, int i3, int i4) {
        int iChooseNextSelection = chooseNextSelection(iArr, i, i2, i3, i4);
        int i5 = iArr[iChooseNextSelection];
        selectInPlace(i5, dArr, i3, i4);
        int i6 = iChooseNextSelection - 1;
        while (i6 >= i && iArr[i6] == i5) {
            i6--;
        }
        if (i6 >= i) {
            selectAllInPlace(iArr, i, i6, dArr, i3, i5 - 1);
        }
        int i7 = iChooseNextSelection + 1;
        while (i7 <= i2 && iArr[i7] == i5) {
            i7++;
        }
        if (i7 <= i2) {
            selectAllInPlace(iArr, i7, i2, dArr, i5 + 1, i4);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void selectInPlace(int i, double[] dArr, int i2, int i3) {
        if (i != i2) {
            int i4 = i3;
            int i5 = i2;
            while (i4 > i5) {
                int iPartition = partition(dArr, i5, i4);
                if (iPartition >= i) {
                    i4 = iPartition - 1;
                }
                if (iPartition <= i) {
                    i5 = iPartition + 1;
                }
            }
            return;
        }
        int i6 = i2;
        for (int i7 = i2 + 1; i7 <= i3; i7++) {
            if (dArr[i6] > dArr[i7]) {
                i6 = i7;
            }
        }
        if (i6 != i2) {
            swap(dArr, i6, i2);
        }
    }

    private static void swap(double[] dArr, int i, int i2) {
        double d = dArr[i];
        dArr[i] = dArr[i2];
        dArr[i2] = d;
    }
}
