package com.google.common.math;

import androidx.media3.common.C;
import androidx.media3.exoplayer.MediaPeriodQueue;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor;
import androidx.media3.exoplayer.upstream.CmcdData;
import androidx.media3.extractor.ts.TsExtractor;
import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLongs;
import java.math.RoundingMode;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class LongMath {
    static final long FLOOR_SQRT_MAX_LONG = 3037000499L;
    static final long MAX_POWER_OF_SQRT2_UNSIGNED = -5402926248376769404L;
    static final long MAX_SIGNED_POWER_OF_TWO = 4611686018427387904L;
    private static final int SIEVE_30 = -545925251;
    static final byte[] maxLog10ForLeadingZeros = {19, Ascii.DC2, Ascii.DC2, Ascii.DC2, Ascii.DC2, 17, 17, 17, Ascii.DLE, Ascii.DLE, Ascii.DLE, Ascii.SI, Ascii.SI, Ascii.SI, Ascii.SI, Ascii.SO, Ascii.SO, Ascii.SO, Ascii.CR, Ascii.CR, Ascii.CR, Ascii.FF, Ascii.FF, Ascii.FF, Ascii.FF, Ascii.VT, Ascii.VT, Ascii.VT, 10, 10, 10, 9, 9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0};
    static final long[] powersOf10 = {1, 10, 100, 1000, Renderer.DEFAULT_DURATION_TO_PROGRESS_US, SilenceSkippingAudioProcessor.DEFAULT_MINIMUM_SILENCE_DURATION_US, 1000000, 10000000, 100000000, C.NANOS_PER_SECOND, 10000000000L, 100000000000L, MediaPeriodQueue.INITIAL_RENDERER_POSITION_OFFSET_US, 10000000000000L, 100000000000000L, 1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L};
    static final long[] halfPowersOf10 = {3, 31, 316, 3162, 31622, 316227, 3162277, 31622776, 316227766, 3162277660L, 31622776601L, 316227766016L, 3162277660168L, 31622776601683L, 316227766016837L, 3162277660168379L, 31622776601683793L, 316227766016837933L, 3162277660168379331L};
    static final long[] factorials = {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600, 6227020800L, 87178291200L, 1307674368000L, 20922789888000L, 355687428096000L, 6402373705728000L, 121645100408832000L, 2432902008176640000L};
    static final int[] biggestBinomials = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 3810779, 121977, 16175, 4337, 1733, 887, 534, 361, 265, 206, 169, 143, 125, 111, 101, 94, 88, 83, 79, 76, 74, 72, 70, 69, 68, 67, 67, 66, 66, 66, 66};
    static final int[] biggestSimpleBinomials = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 2642246, 86251, 11724, 3218, 1313, 684, 419, 287, 214, 169, TsExtractor.TS_STREAM_TYPE_DTS_UHD, 119, 105, 95, 87, 81, 76, 73, 70, 68, 66, 64, 63, 62, 62, 61, 61, 61};
    private static final long[][] millerRabinBaseSets = {new long[]{291830, 126401071349994536L}, new long[]{885594168, 725270293939359937L, 3569819667048198375L}, new long[]{273919523040L, 15, 7363882082L, 992620450144556L}, new long[]{47636622961200L, 2, 2570940, 211991001, 3749873356L}, new long[]{7999252175582850L, 2, 4130806001517L, 149795463772692060L, 186635894390467037L, 3967304179347715805L}, new long[]{585226005592931976L, 2, 123635709730000L, 9233062284813009L, 43835965440333360L, 761179012939631437L, 1263739024124850375L}, new long[]{Long.MAX_VALUE, 2, 325, 9375, 28178, 450775, 9780504, 1795265022}};

    /* JADX INFO: renamed from: com.google.common.math.LongMath$1, reason: invalid class name */
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

    private enum MillerRabinTester {
        SMALL { // from class: com.google.common.math.LongMath.MillerRabinTester.1
            @Override // com.google.common.math.LongMath.MillerRabinTester
            long mulMod(long j, long j2, long j3) {
                return (j * j2) % j3;
            }

            @Override // com.google.common.math.LongMath.MillerRabinTester
            long squareMod(long j, long j2) {
                return (j * j) % j2;
            }
        },
        LARGE { // from class: com.google.common.math.LongMath.MillerRabinTester.2
            private long plusMod(long j, long j2, long j3) {
                return j >= j3 - j2 ? (j + j2) - j3 : j + j2;
            }

            private long times2ToThe32Mod(long j, long j2) {
                int i = 32;
                do {
                    int iMin = Math.min(i, Long.numberOfLeadingZeros(j));
                    j = UnsignedLongs.remainder(j << iMin, j2);
                    i -= iMin;
                } while (i > 0);
                return j;
            }

            @Override // com.google.common.math.LongMath.MillerRabinTester
            long mulMod(long j, long j2, long j3) {
                long j4 = j >>> 32;
                long j5 = j2 >>> 32;
                long j6 = 4294967295L & j;
                long j7 = 4294967295L & j2;
                long jTimes2ToThe32Mod = (j4 * j7) + times2ToThe32Mod(j4 * j5, j3);
                if (jTimes2ToThe32Mod < 0) {
                    jTimes2ToThe32Mod = UnsignedLongs.remainder(jTimes2ToThe32Mod, j3);
                }
                Long.signum(j6);
                return plusMod(times2ToThe32Mod(jTimes2ToThe32Mod + (j5 * j6), j3), UnsignedLongs.remainder(j6 * j7, j3), j3);
            }

            @Override // com.google.common.math.LongMath.MillerRabinTester
            long squareMod(long j, long j2) {
                long j3 = j >>> 32;
                long j4 = j & 4294967295L;
                long jTimes2ToThe32Mod = times2ToThe32Mod(j3 * j3, j2);
                long jRemainder = j3 * j4 * 2;
                if (jRemainder < 0) {
                    jRemainder = UnsignedLongs.remainder(jRemainder, j2);
                }
                return plusMod(times2ToThe32Mod(jRemainder + jTimes2ToThe32Mod, j2), UnsignedLongs.remainder(j4 * j4, j2), j2);
            }
        };

        /* synthetic */ MillerRabinTester(AnonymousClass1 anonymousClass1) {
            this();
        }

        private long powMod(long j, long j2, long j3) {
            long jMulMod = 1;
            long jSquareMod = j;
            while (j2 != 0) {
                if ((1 & j2) != 0) {
                    jMulMod = mulMod(jMulMod, jSquareMod, j3);
                }
                jSquareMod = squareMod(jSquareMod, j3);
                j2 >>= 1;
            }
            return jMulMod;
        }

        static boolean test(long j, long j2) {
            return (j2 <= LongMath.FLOOR_SQRT_MAX_LONG ? SMALL : LARGE).testWitness(j, j2);
        }

        private boolean testWitness(long j, long j2) {
            int iNumberOfTrailingZeros = Long.numberOfTrailingZeros(j2 - 1);
            long j3 = j % j2;
            if (j3 == 0) {
                return true;
            }
            long jPowMod = powMod(j3, (j2 - 1) >> iNumberOfTrailingZeros, j2);
            if (jPowMod == 1) {
                return true;
            }
            int i = 0;
            while (jPowMod != j2 - 1) {
                i++;
                if (i == iNumberOfTrailingZeros) {
                    return false;
                }
                jPowMod = squareMod(jPowMod, j2);
            }
            return true;
        }

        abstract long mulMod(long j, long j2, long j3);

        abstract long squareMod(long j, long j2);
    }

    private LongMath() {
    }

    public static long binomial(int i, int i2) {
        long jMultiplyFraction;
        long j;
        MathPreconditions.checkNonNegative("n", i);
        MathPreconditions.checkNonNegative("k", i2);
        Preconditions.checkArgument(i2 <= i, "k (%s) > n (%s)", i2, i);
        if (i2 > (i >> 1)) {
            i2 = i - i2;
        }
        switch (i2) {
            case 0:
                return 1L;
            case 1:
                return i;
            default:
                if (i < factorials.length) {
                    return factorials[i] / (factorials[i2] * factorials[i - i2]);
                }
                if (i2 >= biggestBinomials.length || i > biggestBinomials[i2]) {
                    return Long.MAX_VALUE;
                }
                if (i2 < biggestSimpleBinomials.length && i <= biggestSimpleBinomials[i2]) {
                    int i3 = i - 1;
                    long j2 = i;
                    for (int i4 = 2; i4 <= i2; i4++) {
                        j2 = (j2 * ((long) i3)) / ((long) i4);
                        i3--;
                    }
                    return j2;
                }
                int iLog2 = log2(i, RoundingMode.CEILING);
                int i5 = 2;
                int i6 = i - 1;
                int i7 = iLog2;
                long j3 = 1;
                long j4 = i;
                long j5 = 1;
                while (i5 <= i2) {
                    if (i7 + iLog2 < 63) {
                        long j6 = j4 * ((long) i6);
                        j5 *= (long) i5;
                        i7 += iLog2;
                        jMultiplyFraction = j3;
                        j = j6;
                    } else {
                        jMultiplyFraction = multiplyFraction(j3, j4, j5);
                        j = i6;
                        j5 = i5;
                        i7 = iLog2;
                    }
                    i5++;
                    i6--;
                    long j7 = j;
                    j3 = jMultiplyFraction;
                    j4 = j7;
                }
                return multiplyFraction(j3, j4, j5);
        }
    }

    public static long ceilingPowerOfTwo(long j) {
        MathPreconditions.checkPositive("x", j);
        if (j <= 4611686018427387904L) {
            return 1 << (-Long.numberOfLeadingZeros(j - 1));
        }
        throw new ArithmeticException("ceilingPowerOfTwo(" + j + ") is not representable as a long");
    }

    public static long checkedAdd(long j, long j2) {
        long j3 = j + j2;
        MathPreconditions.checkNoOverflow(((j ^ j3) >= 0) | ((j ^ j2) < 0), "checkedAdd", j, j2);
        return j3;
    }

    public static long checkedMultiply(long j, long j2) {
        int iNumberOfLeadingZeros = Long.numberOfLeadingZeros(j) + Long.numberOfLeadingZeros(j ^ (-1)) + Long.numberOfLeadingZeros(j2) + Long.numberOfLeadingZeros(j2 ^ (-1));
        if (iNumberOfLeadingZeros > 65) {
            return j * j2;
        }
        MathPreconditions.checkNoOverflow(iNumberOfLeadingZeros >= 64, "checkedMultiply", j, j2);
        MathPreconditions.checkNoOverflow((j2 != Long.MIN_VALUE) | (j >= 0), "checkedMultiply", j, j2);
        long j3 = j * j2;
        MathPreconditions.checkNoOverflow(j == 0 || j3 / j == j2, "checkedMultiply", j, j2);
        return j3;
    }

    public static long checkedPow(long j, int i) {
        long jCheckedMultiply = 1;
        MathPreconditions.checkNonNegative("exponent", i);
        if (!(j <= 2) || !((j > (-2) ? 1 : (j == (-2) ? 0 : -1)) >= 0)) {
            long j2 = j;
            while (true) {
                switch (i) {
                    case 0:
                        return jCheckedMultiply;
                    case 1:
                        return checkedMultiply(jCheckedMultiply, j2);
                    default:
                        if ((i & 1) != 0) {
                            jCheckedMultiply = checkedMultiply(jCheckedMultiply, j2);
                        }
                        i >>= 1;
                        if (i > 0) {
                            MathPreconditions.checkNoOverflow(-3037000499L <= j2 && j2 <= FLOOR_SQRT_MAX_LONG, "checkedPow", j2, i);
                            j2 *= j2;
                        }
                        break;
                }
            }
        } else {
            switch ((int) j) {
                case -2:
                    MathPreconditions.checkNoOverflow(i < 64, "checkedPow", j, i);
                    return (i & 1) == 0 ? 1 << i : (-1) << i;
                case -1:
                    return (i & 1) != 0 ? -1L : 1L;
                case 0:
                    return i == 0 ? 1L : 0L;
                case 1:
                    return 1L;
                case 2:
                    MathPreconditions.checkNoOverflow(i < 63, "checkedPow", j, i);
                    return 1 << i;
                default:
                    throw new AssertionError();
            }
        }
    }

    public static long checkedSubtract(long j, long j2) {
        long j3 = j - j2;
        MathPreconditions.checkNoOverflow(((j ^ j3) >= 0) | ((j ^ j2) >= 0), "checkedSubtract", j, j2);
        return j3;
    }

    /* JADX WARN: Code duplicated, block: B:18:0x0055  */
    /* JADX WARN: Code duplicated, block: B:38:0x0081  */
    public static long divide(long j, long j2, RoundingMode roundingMode) {
        long j3;
        Preconditions.checkNotNull(roundingMode);
        long j4 = j / j2;
        long j5 = j - (j2 * j4);
        if (j5 == 0) {
            return j4;
        }
        boolean z = true;
        int i = ((int) ((j ^ j2) >> 63)) | 1;
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(j5 == 0);
            case 2:
                z = false;
                if (z) {
                    j3 = ((long) i) + j4;
                } else {
                    j3 = j4;
                }
                return j3;
            case 3:
                z = i < 0;
                if (z) {
                    j3 = ((long) i) + j4;
                } else {
                    j3 = j4;
                }
                return j3;
            case 4:
                z = true;
                if (z) {
                    j3 = ((long) i) + j4;
                } else {
                    j3 = j4;
                }
                return j3;
            case 5:
                z = i > 0;
                if (z) {
                    j3 = ((long) i) + j4;
                } else {
                    j3 = j4;
                }
                return j3;
            case 6:
            case 7:
            case 8:
                long jAbs = Math.abs(j5);
                long jAbs2 = jAbs - (Math.abs(j2) - jAbs);
                if (jAbs2 != 0) {
                    z = jAbs2 > 0;
                } else if (roundingMode != RoundingMode.HALF_UP && (roundingMode != RoundingMode.HALF_EVEN || (1 & j4) == 0)) {
                    z = false;
                }
                if (z) {
                    j3 = ((long) i) + j4;
                } else {
                    j3 = j4;
                }
                return j3;
            default:
                throw new AssertionError();
        }
    }

    public static long factorial(int i) {
        MathPreconditions.checkNonNegative("n", i);
        if (i < factorials.length) {
            return factorials[i];
        }
        return Long.MAX_VALUE;
    }

    static boolean fitsInInt(long j) {
        return ((long) ((int) j)) == j;
    }

    public static long floorPowerOfTwo(long j) {
        MathPreconditions.checkPositive("x", j);
        return 1 << (63 - Long.numberOfLeadingZeros(j));
    }

    public static long gcd(long j, long j2) {
        MathPreconditions.checkNonNegative(CmcdData.Factory.OBJECT_TYPE_AUDIO_ONLY, j);
        MathPreconditions.checkNonNegative("b", j2);
        if (j == 0) {
            return j2;
        }
        if (j2 == 0) {
            return j;
        }
        int iNumberOfTrailingZeros = Long.numberOfTrailingZeros(j);
        long jNumberOfTrailingZeros = j >> iNumberOfTrailingZeros;
        int iNumberOfTrailingZeros2 = Long.numberOfTrailingZeros(j2);
        long j3 = j2 >> iNumberOfTrailingZeros2;
        while (jNumberOfTrailingZeros != j3) {
            long j4 = jNumberOfTrailingZeros - j3;
            long j5 = (j4 >> 63) & j4;
            long j6 = (j4 - j5) - j5;
            j3 += j5;
            jNumberOfTrailingZeros = j6 >> Long.numberOfTrailingZeros(j6);
        }
        return jNumberOfTrailingZeros << Math.min(iNumberOfTrailingZeros, iNumberOfTrailingZeros2);
    }

    public static boolean isPowerOfTwo(long j) {
        return (((j - 1) & j) == 0) & (j > 0);
    }

    public static boolean isPrime(long j) {
        if (j < 2) {
            MathPreconditions.checkNonNegative("n", j);
            return false;
        }
        if (j < 66) {
            return ((722865708377213483 >> (((int) j) + (-2))) & 1) != 0;
        }
        if (((1 << ((int) (j % 30))) & SIEVE_30) != 0 || j % 7 == 0 || j % 11 == 0 || j % 13 == 0) {
            return false;
        }
        if (j < 289) {
            return true;
        }
        for (long[] jArr : millerRabinBaseSets) {
            if (j <= jArr[0]) {
                for (int i = 1; i < jArr.length; i++) {
                    if (!MillerRabinTester.test(jArr[i], j)) {
                        return false;
                    }
                }
                return true;
            }
        }
        throw new AssertionError();
    }

    static int lessThanBranchFree(long j, long j2) {
        return (int) ((((j - j2) ^ (-1)) ^ (-1)) >>> 63);
    }

    public static int log10(long j, RoundingMode roundingMode) {
        MathPreconditions.checkPositive("x", j);
        int iLog10Floor = log10Floor(j);
        long j2 = powersOf10[iLog10Floor];
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(j == j2);
                break;
            case 2:
            case 3:
                break;
            case 4:
            case 5:
                return lessThanBranchFree(j2, j) + iLog10Floor;
            case 6:
            case 7:
            case 8:
                return lessThanBranchFree(halfPowersOf10[iLog10Floor], j) + iLog10Floor;
            default:
                throw new AssertionError();
        }
        return iLog10Floor;
    }

    static int log10Floor(long j) {
        byte b = maxLog10ForLeadingZeros[Long.numberOfLeadingZeros(j)];
        return b - lessThanBranchFree(j, powersOf10[b]);
    }

    public static int log2(long j, RoundingMode roundingMode) {
        MathPreconditions.checkPositive("x", j);
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(isPowerOfTwo(j));
                break;
            case 2:
            case 3:
                break;
            case 4:
            case 5:
                return 64 - Long.numberOfLeadingZeros(j - 1);
            case 6:
            case 7:
            case 8:
                int iNumberOfLeadingZeros = Long.numberOfLeadingZeros(j);
                return (63 - iNumberOfLeadingZeros) + lessThanBranchFree(MAX_POWER_OF_SQRT2_UNSIGNED >>> iNumberOfLeadingZeros, j);
            default:
                throw new AssertionError("impossible");
        }
        return 63 - Long.numberOfLeadingZeros(j);
    }

    public static long mean(long j, long j2) {
        return (j & j2) + ((j ^ j2) >> 1);
    }

    public static int mod(long j, int i) {
        return (int) mod(j, i);
    }

    public static long mod(long j, long j2) {
        if (j2 <= 0) {
            throw new ArithmeticException("Modulus must be positive");
        }
        long j3 = j % j2;
        return j3 >= 0 ? j3 : j3 + j2;
    }

    static long multiplyFraction(long j, long j2, long j3) {
        if (j == 1) {
            return j2 / j3;
        }
        long jGcd = gcd(j, j3);
        return (j2 / (j3 / jGcd)) * (j / jGcd);
    }

    public static long pow(long j, int i) {
        MathPreconditions.checkNonNegative("exponent", i);
        if (-2 <= j && j <= 2) {
            switch ((int) j) {
                case -2:
                    if (i < 64) {
                        return (i & 1) == 0 ? 1 << i : -(1 << i);
                    }
                    return 0L;
                case -1:
                    return (i & 1) != 0 ? -1L : 1L;
                case 0:
                    return i == 0 ? 1L : 0L;
                case 1:
                    return 1L;
                case 2:
                    if (i < 64) {
                        return 1 << i;
                    }
                    return 0L;
                default:
                    throw new AssertionError();
            }
        }
        long j2 = 1;
        long j3 = j;
        while (true) {
            switch (i) {
                case 0:
                    return j2;
                case 1:
                    return j2 * j3;
                default:
                    j2 *= (i & 1) == 0 ? 1L : j3;
                    j3 *= j3;
                    i >>= 1;
                    break;
            }
        }
    }

    public static double roundToDouble(long j, RoundingMode roundingMode) {
        long jFloor;
        long jCeil;
        double d;
        double d2 = j;
        long j2 = (long) d2;
        int iCompare = j2 == Long.MAX_VALUE ? -1 : Longs.compare(j, j2);
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(iCompare == 0);
                return d2;
            case 2:
                if (j >= 0) {
                    return iCompare < 0 ? DoubleUtils.nextDown(d2) : d2;
                }
                return iCompare > 0 ? Math.nextUp(d2) : d2;
            case 3:
                return iCompare < 0 ? DoubleUtils.nextDown(d2) : d2;
            case 4:
                if (j >= 0) {
                    return iCompare > 0 ? Math.nextUp(d2) : d2;
                }
                return iCompare < 0 ? DoubleUtils.nextDown(d2) : d2;
            case 5:
                return iCompare <= 0 ? d2 : Math.nextUp(d2);
            case 6:
            case 7:
            case 8:
                if (iCompare >= 0) {
                    double dNextUp = Math.nextUp(d2);
                    jFloor = j2;
                    d = dNextUp;
                    jCeil = (long) Math.ceil(dNextUp);
                } else {
                    double dNextDown = DoubleUtils.nextDown(d2);
                    jFloor = (long) Math.floor(dNextDown);
                    d2 = dNextDown;
                    jCeil = j2;
                    d = d2;
                }
                long j3 = jCeil - j;
                if (jCeil == Long.MAX_VALUE) {
                    j3++;
                }
                int iCompare2 = Longs.compare(j - jFloor, j3);
                if (iCompare2 < 0) {
                    return d2;
                }
                if (iCompare2 > 0) {
                    return d;
                }
                switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
                    case 6:
                        return j < 0 ? d : d2;
                    case 7:
                        if (j < 0) {
                            d = d2;
                        }
                        return d;
                    case 8:
                        return (DoubleUtils.getSignificand(d2) & 1) == 0 ? d2 : d;
                    default:
                        throw new AssertionError("impossible");
                }
            default:
                throw new AssertionError("impossible");
        }
    }

    public static long saturatedAdd(long j, long j2) {
        long j3 = j + j2;
        return ((j ^ j3) >= 0) | (((j ^ j2) > 0L ? 1 : ((j ^ j2) == 0L ? 0 : -1)) < 0) ? j3 : ((j3 >>> 63) ^ 1) + Long.MAX_VALUE;
    }

    public static long saturatedMultiply(long j, long j2) {
        int iNumberOfLeadingZeros = Long.numberOfLeadingZeros(j) + Long.numberOfLeadingZeros(j ^ (-1)) + Long.numberOfLeadingZeros(j2) + Long.numberOfLeadingZeros(j2 ^ (-1));
        if (iNumberOfLeadingZeros > 65) {
            return j * j2;
        }
        long j3 = ((j ^ j2) >>> 63) + Long.MAX_VALUE;
        if (((j2 == Long.MIN_VALUE) & (j < 0)) || (iNumberOfLeadingZeros < 64)) {
            return j3;
        }
        long j4 = j * j2;
        return (j == 0 || j4 / j == j2) ? j4 : j3;
    }

    public static long saturatedPow(long j, int i) {
        MathPreconditions.checkNonNegative("exponent", i);
        if (!(j <= 2) || !((j > (-2) ? 1 : (j == (-2) ? 0 : -1)) >= 0)) {
            long j2 = i & 1;
            long jSaturatedMultiply = 1;
            long j3 = j;
            while (true) {
                switch (i) {
                    case 0:
                        return jSaturatedMultiply;
                    case 1:
                        return saturatedMultiply(jSaturatedMultiply, j3);
                    default:
                        jSaturatedMultiply = (i & 1) != 0 ? saturatedMultiply(jSaturatedMultiply, j3) : jSaturatedMultiply;
                        i >>= 1;
                        if (i > 0) {
                            if ((j3 > FLOOR_SQRT_MAX_LONG) || (((-3037000499L) > j3 ? 1 : ((-3037000499L) == j3 ? 0 : -1)) > 0)) {
                                return ((j >>> 63) & j2) + Long.MAX_VALUE;
                            }
                            j3 *= j3;
                        }
                        break;
                }
            }
        } else {
            switch ((int) j) {
                case -2:
                    if (i >= 64) {
                        return ((long) (i & 1)) + Long.MAX_VALUE;
                    }
                    return (i & 1) == 0 ? 1 << i : (-1) << i;
                case -1:
                    return (i & 1) == 0 ? 1L : -1L;
                case 0:
                    return i != 0 ? 0L : 1L;
                case 1:
                    return 1L;
                case 2:
                    if (i >= 63) {
                        return Long.MAX_VALUE;
                    }
                    return 1 << i;
                default:
                    throw new AssertionError();
            }
        }
    }

    public static long saturatedSubtract(long j, long j2) {
        long j3 = j - j2;
        return ((j ^ j3) >= 0) | (((j ^ j2) > 0L ? 1 : ((j ^ j2) == 0L ? 0 : -1)) >= 0) ? j3 : ((j3 >>> 63) ^ 1) + Long.MAX_VALUE;
    }

    public static long sqrt(long j, RoundingMode roundingMode) {
        MathPreconditions.checkNonNegative("x", j);
        if (fitsInInt(j)) {
            return IntMath.sqrt((int) j, roundingMode);
        }
        long jSqrt = (long) Math.sqrt(j);
        long j2 = jSqrt * jSqrt;
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(j2 == j);
                return jSqrt;
            case 2:
            case 3:
                return j < j2 ? jSqrt - 1 : jSqrt;
            case 4:
            case 5:
                return j > j2 ? 1 + jSqrt : jSqrt;
            case 6:
            case 7:
            case 8:
                long j3 = jSqrt - ((long) (j >= j2 ? 0 : 1));
                return j3 + ((long) lessThanBranchFree((j3 * j3) + j3, j));
            default:
                throw new AssertionError();
        }
    }
}
