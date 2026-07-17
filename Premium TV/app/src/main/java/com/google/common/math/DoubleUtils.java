package com.google.common.math;

import com.google.common.base.Preconditions;
import java.math.BigInteger;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class DoubleUtils {
    static final int EXPONENT_BIAS = 1023;
    static final long EXPONENT_MASK = 9218868437227405312L;
    static final long IMPLICIT_BIT = 4503599627370496L;
    static final long ONE_BITS = 4607182418800017408L;
    static final int SIGNIFICAND_BITS = 52;
    static final long SIGNIFICAND_MASK = 4503599627370495L;
    static final long SIGN_MASK = Long.MIN_VALUE;

    private DoubleUtils() {
    }

    static double bigToDouble(BigInteger bigInteger) {
        boolean z = true;
        BigInteger bigIntegerAbs = bigInteger.abs();
        int iBitLength = bigIntegerAbs.bitLength() - 1;
        if (iBitLength < 63) {
            return bigInteger.longValue();
        }
        if (iBitLength > 1023) {
            return ((double) bigInteger.signum()) * Double.POSITIVE_INFINITY;
        }
        int i = (iBitLength - 52) - 1;
        long jLongValue = bigIntegerAbs.shiftRight(i).longValue();
        long j = (jLongValue >> 1) & SIGNIFICAND_MASK;
        if ((jLongValue & 1) == 0 || ((j & 1) == 0 && bigIntegerAbs.getLowestSetBit() >= i)) {
            z = false;
        }
        if (z) {
            j++;
        }
        return Double.longBitsToDouble((j + (((long) (iBitLength + 1023)) << 52)) | (((long) bigInteger.signum()) & Long.MIN_VALUE));
    }

    static double ensureNonNegative(double d) {
        Preconditions.checkArgument(!Double.isNaN(d));
        return Math.max(d, 0.0d);
    }

    static long getSignificand(double d) {
        Preconditions.checkArgument(isFinite(d), "not a normal value");
        int exponent = Math.getExponent(d);
        long jDoubleToRawLongBits = Double.doubleToRawLongBits(d) & SIGNIFICAND_MASK;
        return exponent == -1023 ? jDoubleToRawLongBits << 1 : IMPLICIT_BIT | jDoubleToRawLongBits;
    }

    static boolean isFinite(double d) {
        return Math.getExponent(d) <= 1023;
    }

    static boolean isNormal(double d) {
        return Math.getExponent(d) >= -1022;
    }

    static double nextDown(double d) {
        return -Math.nextUp(-d);
    }

    static double scaleNormalize(double d) {
        return Double.longBitsToDouble(ONE_BITS | (Double.doubleToRawLongBits(d) & SIGNIFICAND_MASK));
    }
}
