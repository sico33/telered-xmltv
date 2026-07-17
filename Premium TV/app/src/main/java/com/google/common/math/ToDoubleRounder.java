package com.google.common.math;

import com.google.common.base.Preconditions;
import java.lang.Comparable;
import java.lang.Number;
import java.math.RoundingMode;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class ToDoubleRounder<X extends Number & Comparable<X>> {

    /* JADX INFO: renamed from: com.google.common.math.ToDoubleRounder$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final int[] $SwitchMap$java$math$RoundingMode = new int[RoundingMode.values().length];

        static {
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.DOWN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_EVEN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_DOWN.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_UP.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.FLOOR.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.CEILING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.UP.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.UNNECESSARY.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
        }
    }

    ToDoubleRounder() {
    }

    abstract X minus(X x, X x2);

    final double roundToDouble(X x, RoundingMode roundingMode) {
        double dNextDown;
        Number x2;
        Preconditions.checkNotNull(x, "x");
        Preconditions.checkNotNull(roundingMode, "mode");
        double dRoundToDoubleArbitrarily = roundToDoubleArbitrarily(x);
        if (Double.isInfinite(dRoundToDoubleArbitrarily)) {
            switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 4:
                    return ((double) sign(x)) * Double.MAX_VALUE;
                case 5:
                    return dRoundToDoubleArbitrarily != Double.POSITIVE_INFINITY ? Double.NEGATIVE_INFINITY : Double.MAX_VALUE;
                case 6:
                    return dRoundToDoubleArbitrarily == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : -1.7976931348623157E308d;
                case 7:
                    return dRoundToDoubleArbitrarily;
                case 8:
                    throw new ArithmeticException(x + " cannot be represented precisely as a double");
            }
        }
        Number x3 = toX(dRoundToDoubleArbitrarily, RoundingMode.UNNECESSARY);
        int iCompareTo = ((Comparable) x).compareTo(x3);
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                if (sign(x) >= 0) {
                    return iCompareTo < 0 ? DoubleUtils.nextDown(dRoundToDoubleArbitrarily) : dRoundToDoubleArbitrarily;
                }
                return iCompareTo > 0 ? Math.nextUp(dRoundToDoubleArbitrarily) : dRoundToDoubleArbitrarily;
            case 2:
            case 3:
            case 4:
                if (iCompareTo >= 0) {
                    double dNextUp = Math.nextUp(dRoundToDoubleArbitrarily);
                    if (dNextUp == Double.POSITIVE_INFINITY) {
                        return dRoundToDoubleArbitrarily;
                    }
                    dNextDown = dRoundToDoubleArbitrarily;
                    dRoundToDoubleArbitrarily = dNextUp;
                    x2 = x3;
                    x3 = toX(dNextUp, RoundingMode.CEILING);
                } else {
                    dNextDown = DoubleUtils.nextDown(dRoundToDoubleArbitrarily);
                    if (dNextDown == Double.NEGATIVE_INFINITY) {
                        return dRoundToDoubleArbitrarily;
                    }
                    x2 = toX(dNextDown, RoundingMode.FLOOR);
                }
                int iCompareTo2 = ((Comparable) minus(x, x2)).compareTo(minus(x3, x));
                if (iCompareTo2 < 0) {
                    return dNextDown;
                }
                if (iCompareTo2 > 0) {
                    return dRoundToDoubleArbitrarily;
                }
                switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
                    case 2:
                        if ((Double.doubleToRawLongBits(dNextDown) & 1) != 0) {
                            dNextDown = dRoundToDoubleArbitrarily;
                        }
                        return dNextDown;
                    case 3:
                        if (sign(x) < 0) {
                            dNextDown = dRoundToDoubleArbitrarily;
                        }
                        return dNextDown;
                    case 4:
                        return sign(x) < 0 ? dNextDown : dRoundToDoubleArbitrarily;
                    default:
                        throw new AssertionError("impossible");
                }
            case 5:
                return iCompareTo < 0 ? DoubleUtils.nextDown(dRoundToDoubleArbitrarily) : dRoundToDoubleArbitrarily;
            case 6:
                return iCompareTo > 0 ? Math.nextUp(dRoundToDoubleArbitrarily) : dRoundToDoubleArbitrarily;
            case 7:
                if (sign(x) >= 0) {
                    return iCompareTo <= 0 ? dRoundToDoubleArbitrarily : Math.nextUp(dRoundToDoubleArbitrarily);
                }
                return iCompareTo < 0 ? DoubleUtils.nextDown(dRoundToDoubleArbitrarily) : dRoundToDoubleArbitrarily;
            case 8:
                MathPreconditions.checkRoundingUnnecessary(iCompareTo == 0);
                return dRoundToDoubleArbitrarily;
            default:
                throw new AssertionError("impossible");
        }
    }

    abstract double roundToDoubleArbitrarily(X x);

    abstract int sign(X x);

    abstract X toX(double d, RoundingMode roundingMode);
}
