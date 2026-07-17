package com.google.common.math;

import androidx.media3.exoplayer.upstream.CmcdData;
import androidx.media3.extractor.AacUtil;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.math.RoundingMode;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class IntMath {
    static final int FLOOR_SQRT_MAX_INT = 46340;
    static final int MAX_POWER_OF_SQRT2_UNSIGNED = -1257966797;
    static final int MAX_SIGNED_POWER_OF_TWO = 1073741824;
    static final byte[] maxLog10ForLeadingZeros = {9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0, 0};
    static final int[] powersOf10 = {1, 10, 100, 1000, 10000, AacUtil.AAC_LC_MAX_RATE_BYTES_PER_SECOND, 1000000, 10000000, 100000000, 1000000000};
    static final int[] halfPowersOf10 = {3, 31, 316, 3162, 31622, 316227, 3162277, 31622776, 316227766, Integer.MAX_VALUE};
    private static final int[] factorials = {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600};
    static int[] biggestBinomials = {Integer.MAX_VALUE, Integer.MAX_VALUE, 65536, 2345, 477, 193, 110, 75, 58, 49, 43, 39, 37, 35, 34, 34, 33};

    /* JADX INFO: renamed from: com.google.common.math.IntMath$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final int[] $SwitchMap$java$math$RoundingMode = new int[RoundingMode.values().length];

        static {
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.UNNECESSARY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.DOWN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.FLOOR.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.UP.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.CEILING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_DOWN.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_UP.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_EVEN.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
        }
    }

    private IntMath() {
    }

    public static int binomial(int i, int i2) {
        MathPreconditions.checkNonNegative("n", i);
        MathPreconditions.checkNonNegative("k", i2);
        Preconditions.checkArgument(i2 <= i, "k (%s) > n (%s)", i2, i);
        if (i2 > (i >> 1)) {
            i2 = i - i2;
        }
        if (i2 >= biggestBinomials.length || i > biggestBinomials[i2]) {
            return Integer.MAX_VALUE;
        }
        switch (i2) {
            case 0:
                return 1;
            case 1:
                return i;
            default:
                long j = 1;
                for (int i3 = 0; i3 < i2; i3++) {
                    j = (j * ((long) (i - i3))) / ((long) (i3 + 1));
                }
                return (int) j;
        }
    }

    public static int ceilingPowerOfTwo(int i) {
        MathPreconditions.checkPositive("x", i);
        if (i <= 1073741824) {
            return 1 << (-Integer.numberOfLeadingZeros(i - 1));
        }
        throw new ArithmeticException("ceilingPowerOfTwo(" + i + ") not representable as an int");
    }

    public static int checkedAdd(int i, int i2) {
        long j = ((long) i2) + ((long) i);
        MathPreconditions.checkNoOverflow(j == ((long) ((int) j)), "checkedAdd", i, i2);
        return (int) j;
    }

    public static int checkedMultiply(int i, int i2) {
        long j = ((long) i2) * ((long) i);
        MathPreconditions.checkNoOverflow(j == ((long) ((int) j)), "checkedMultiply", i, i2);
        return (int) j;
    }

    public static int checkedPow(int i, int i2) {
        MathPreconditions.checkNonNegative("exponent", i2);
        switch (i) {
            case -2:
                MathPreconditions.checkNoOverflow(i2 < 32, "checkedPow", i, i2);
                return (i2 & 1) == 0 ? 1 << i2 : (-1) << i2;
            case -1:
                return (i2 & 1) != 0 ? -1 : 1;
            case 0:
                return i2 != 0 ? 0 : 1;
            case 1:
                return 1;
            case 2:
                MathPreconditions.checkNoOverflow(i2 < 31, "checkedPow", i, i2);
                return 1 << i2;
            default:
                int iCheckedMultiply = 1;
                while (true) {
                    switch (i2) {
                        case 0:
                            return iCheckedMultiply;
                        case 1:
                            return checkedMultiply(iCheckedMultiply, i);
                        default:
                            iCheckedMultiply = (i2 & 1) != 0 ? checkedMultiply(iCheckedMultiply, i) : iCheckedMultiply;
                            i2 >>= 1;
                            if (i2 > 0) {
                                MathPreconditions.checkNoOverflow((i <= FLOOR_SQRT_MAX_INT) & (-46340 <= i), "checkedPow", i, i2);
                                i *= i;
                            }
                            break;
                    }
                }
                break;
        }
    }

    public static int checkedSubtract(int i, int i2) {
        long j = ((long) i) - ((long) i2);
        MathPreconditions.checkNoOverflow(j == ((long) ((int) j)), "checkedSubtract", i, i2);
        return (int) j;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:24:0x0046  */
    /* JADX WARN: Code duplicated, block: B:39:0x0065  */
    public static int divide(int i, int i2, RoundingMode roundingMode) {
        int i3;
        boolean z = true;
        Preconditions.checkNotNull(roundingMode);
        if (i2 == 0) {
            throw new ArithmeticException("/ by zero");
        }
        int i4 = i / i2;
        int i5 = i - (i2 * i4);
        if (i5 == 0) {
            return i4;
        }
        int i6 = ((i ^ i2) >> 31) | 1;
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(i5 == 0);
                z = false;
                if (z) {
                    i3 = i4 + i6;
                } else {
                    i3 = i4;
                }
                return i3;
            case 2:
                z = false;
                if (z) {
                    i3 = i4 + i6;
                } else {
                    i3 = i4;
                }
                return i3;
            case 3:
                if (i6 >= 0) {
                    z = false;
                }
                if (z) {
                    i3 = i4 + i6;
                } else {
                    i3 = i4;
                }
                return i3;
            case 4:
                if (z) {
                    i3 = i4 + i6;
                } else {
                    i3 = i4;
                }
                return i3;
            case 5:
                if (i6 <= 0) {
                    z = false;
                }
                if (z) {
                    i3 = i4 + i6;
                } else {
                    i3 = i4;
                }
                return i3;
            case 6:
            case 7:
            case 8:
                int iAbs = Math.abs(i5);
                int iAbs2 = iAbs - (Math.abs(i2) - iAbs);
                if (iAbs2 == 0) {
                    if (roundingMode != RoundingMode.HALF_UP) {
                        if (!(((i4 & 1) != 0) & (roundingMode == RoundingMode.HALF_EVEN))) {
                            z = false;
                        }
                    }
                } else if (iAbs2 <= 0) {
                    z = false;
                }
                if (z) {
                    i3 = i4 + i6;
                } else {
                    i3 = i4;
                }
                return i3;
            default:
                throw new AssertionError();
        }
    }

    public static int factorial(int i) {
        MathPreconditions.checkNonNegative("n", i);
        if (i < factorials.length) {
            return factorials[i];
        }
        return Integer.MAX_VALUE;
    }

    public static int floorPowerOfTwo(int i) {
        MathPreconditions.checkPositive("x", i);
        return Integer.highestOneBit(i);
    }

    public static int gcd(int i, int i2) {
        MathPreconditions.checkNonNegative(CmcdData.Factory.OBJECT_TYPE_AUDIO_ONLY, i);
        MathPreconditions.checkNonNegative("b", i2);
        if (i == 0) {
            return i2;
        }
        if (i2 == 0) {
            return i;
        }
        int iNumberOfTrailingZeros = Integer.numberOfTrailingZeros(i);
        int iNumberOfTrailingZeros2 = i >> iNumberOfTrailingZeros;
        int iNumberOfTrailingZeros3 = Integer.numberOfTrailingZeros(i2);
        int i3 = i2 >> iNumberOfTrailingZeros3;
        while (iNumberOfTrailingZeros2 != i3) {
            int i4 = iNumberOfTrailingZeros2 - i3;
            int i5 = (i4 >> 31) & i4;
            int i6 = (i4 - i5) - i5;
            i3 += i5;
            iNumberOfTrailingZeros2 = i6 >> Integer.numberOfTrailingZeros(i6);
        }
        return iNumberOfTrailingZeros2 << Math.min(iNumberOfTrailingZeros, iNumberOfTrailingZeros3);
    }

    public static boolean isPowerOfTwo(int i) {
        return (((i + (-1)) & i) == 0) & (i > 0);
    }

    public static boolean isPrime(int i) {
        return LongMath.isPrime(i);
    }

    static int lessThanBranchFree(int i, int i2) {
        return (((i - i2) ^ (-1)) ^ (-1)) >>> 31;
    }

    public static int log10(int i, RoundingMode roundingMode) {
        MathPreconditions.checkPositive("x", i);
        int iLog10Floor = log10Floor(i);
        int i2 = powersOf10[iLog10Floor];
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(i == i2);
                break;
            case 2:
            case 3:
                break;
            case 4:
            case 5:
                return lessThanBranchFree(i2, i) + iLog10Floor;
            case 6:
            case 7:
            case 8:
                return lessThanBranchFree(halfPowersOf10[iLog10Floor], i) + iLog10Floor;
            default:
                throw new AssertionError();
        }
        return iLog10Floor;
    }

    private static int log10Floor(int i) {
        byte b = maxLog10ForLeadingZeros[Integer.numberOfLeadingZeros(i)];
        return b - lessThanBranchFree(i, powersOf10[b]);
    }

    public static int log2(int i, RoundingMode roundingMode) {
        MathPreconditions.checkPositive("x", i);
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(isPowerOfTwo(i));
                break;
            case 2:
            case 3:
                break;
            case 4:
            case 5:
                return 32 - Integer.numberOfLeadingZeros(i - 1);
            case 6:
            case 7:
            case 8:
                int iNumberOfLeadingZeros = Integer.numberOfLeadingZeros(i);
                return (31 - iNumberOfLeadingZeros) + lessThanBranchFree(MAX_POWER_OF_SQRT2_UNSIGNED >>> iNumberOfLeadingZeros, i);
            default:
                throw new AssertionError();
        }
        return 31 - Integer.numberOfLeadingZeros(i);
    }

    public static int mean(int i, int i2) {
        return (i & i2) + ((i ^ i2) >> 1);
    }

    public static int mod(int i, int i2) {
        if (i2 <= 0) {
            throw new ArithmeticException("Modulus " + i2 + " must be > 0");
        }
        int i3 = i % i2;
        return i3 >= 0 ? i3 : i3 + i2;
    }

    public static int pow(int i, int i2) {
        MathPreconditions.checkNonNegative("exponent", i2);
        switch (i) {
            case -2:
                if (i2 < 32) {
                    return (i2 & 1) == 0 ? 1 << i2 : -(1 << i2);
                }
                return 0;
            case -1:
                return (i2 & 1) != 0 ? -1 : 1;
            case 0:
                return i2 == 0 ? 1 : 0;
            case 1:
                return 1;
            case 2:
                if (i2 < 32) {
                    return 1 << i2;
                }
                return 0;
            default:
                int i3 = 1;
                int i4 = i;
                while (true) {
                    switch (i2) {
                        case 0:
                            return i3;
                        case 1:
                            return i4 * i3;
                        default:
                            i3 *= (i2 & 1) == 0 ? 1 : i4;
                            i4 *= i4;
                            i2 >>= 1;
                            break;
                    }
                }
                break;
        }
    }

    public static int saturatedAdd(int i, int i2) {
        return Ints.saturatedCast(((long) i) + ((long) i2));
    }

    public static int saturatedMultiply(int i, int i2) {
        return Ints.saturatedCast(((long) i) * ((long) i2));
    }

    public static int saturatedPow(int i, int i2) {
        MathPreconditions.checkNonNegative("exponent", i2);
        switch (i) {
            case -2:
                if (i2 >= 32) {
                    return (i2 & 1) + Integer.MAX_VALUE;
                }
                return (i2 & 1) == 0 ? 1 << i2 : (-1) << i2;
            case -1:
                return (i2 & 1) != 0 ? -1 : 1;
            case 0:
                return i2 != 0 ? 0 : 1;
            case 1:
                return 1;
            case 2:
                if (i2 >= 31) {
                    return Integer.MAX_VALUE;
                }
                return 1 << i2;
            default:
                int iSaturatedMultiply = 1;
                int i3 = i2;
                int i4 = i;
                while (true) {
                    switch (i3) {
                        case 0:
                            return iSaturatedMultiply;
                        case 1:
                            return saturatedMultiply(iSaturatedMultiply, i4);
                        default:
                            iSaturatedMultiply = (i3 & 1) != 0 ? saturatedMultiply(iSaturatedMultiply, i4) : iSaturatedMultiply;
                            i3 >>= 1;
                            if (i3 > 0) {
                                if ((i4 > FLOOR_SQRT_MAX_INT) || (-46340 > i4)) {
                                    return ((i >>> 31) & i2 & 1) + Integer.MAX_VALUE;
                                }
                                i4 *= i4;
                            }
                            break;
                    }
                }
                break;
        }
    }

    public static int saturatedSubtract(int i, int i2) {
        return Ints.saturatedCast(((long) i) - ((long) i2));
    }

    public static int sqrt(int i, RoundingMode roundingMode) {
        MathPreconditions.checkNonNegative("x", i);
        int iSqrtFloor = sqrtFloor(i);
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(iSqrtFloor * iSqrtFloor == i);
                break;
            case 2:
            case 3:
                break;
            case 4:
            case 5:
                return lessThanBranchFree(iSqrtFloor * iSqrtFloor, i) + iSqrtFloor;
            case 6:
            case 7:
            case 8:
                return lessThanBranchFree((iSqrtFloor * iSqrtFloor) + iSqrtFloor, i) + iSqrtFloor;
            default:
                throw new AssertionError();
        }
        return iSqrtFloor;
    }

    private static int sqrtFloor(int i) {
        return (int) Math.sqrt(i);
    }
}
