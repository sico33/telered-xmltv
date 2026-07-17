package kotlin.time;

import kotlin.Metadata;

/* JADX INFO: compiled from: longSaturatedMath.kt */
/* JADX INFO: loaded from: classes2.dex */
@Metadata(d1 = {"\u0000\u0012\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\f\u001a*\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00012\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0001H\u0002ø\u0001\u0000¢\u0006\u0004\b\u0006\u0010\u0007\u001a\"\u0010\b\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00012\u0006\u0010\u0003\u001a\u00020\u0004H\u0000ø\u0001\u0000¢\u0006\u0004\b\t\u0010\n\u001a\"\u0010\u000b\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00012\u0006\u0010\u0003\u001a\u00020\u0004H\u0002ø\u0001\u0000¢\u0006\u0004\b\f\u0010\n\u001a \u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00012\u0006\u0010\u000f\u001a\u00020\u0001H\u0000ø\u0001\u0000¢\u0006\u0002\u0010\n\u0082\u0002\u0004\n\u0002\b\u0019¨\u0006\u0010"}, d2 = {"checkInfiniteSumDefined", "", "longNs", "duration", "Lkotlin/time/Duration;", "durationNs", "checkInfiniteSumDefined-PjuGub4", "(JJJ)J", "saturatingAdd", "saturatingAdd-pTJri5U", "(JJ)J", "saturatingAddInHalves", "saturatingAddInHalves-pTJri5U", "saturatingDiff", "valueNs", "originNs", "kotlin-stdlib"}, k = 2, mv = {1, 7, 1}, xi = 48)
public final class LongSaturatedMathKt {
    /* JADX INFO: renamed from: saturatingAdd-pTJri5U, reason: not valid java name */
    public static final long m1735saturatingAddpTJri5U(long longNs, long duration) {
        long durationNs = Duration.m1627getInWholeNanosecondsimpl(duration);
        if (((longNs - 1) | 1) == Long.MAX_VALUE) {
            return m1734checkInfiniteSumDefinedPjuGub4(longNs, duration, durationNs);
        }
        if (((durationNs - 1) | 1) == Long.MAX_VALUE) {
            return m1736saturatingAddInHalvespTJri5U(longNs, duration);
        }
        long result = longNs + durationNs;
        if (((longNs ^ result) & (durationNs ^ result)) < 0) {
            return longNs < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return result;
    }

    /* JADX INFO: renamed from: checkInfiniteSumDefined-PjuGub4, reason: not valid java name */
    private static final long m1734checkInfiniteSumDefinedPjuGub4(long longNs, long duration, long durationNs) {
        if (!Duration.m1639isInfiniteimpl(duration) || (longNs ^ durationNs) >= 0) {
            return longNs;
        }
        throw new IllegalArgumentException("Summing infinities of different signs");
    }

    /* JADX INFO: renamed from: saturatingAddInHalves-pTJri5U, reason: not valid java name */
    private static final long m1736saturatingAddInHalvespTJri5U(long longNs, long duration) {
        long half = Duration.m1610divUwyO8pc(duration, 2);
        if (((Duration.m1627getInWholeNanosecondsimpl(half) - 1) | 1) == Long.MAX_VALUE) {
            return (long) (longNs + Duration.m1650toDoubleimpl(duration, DurationUnit.NANOSECONDS));
        }
        return m1735saturatingAddpTJri5U(m1735saturatingAddpTJri5U(longNs, half), half);
    }

    public static final long saturatingDiff(long valueNs, long originNs) {
        if ((1 | (originNs - 1)) == Long.MAX_VALUE) {
            return Duration.m1659unaryMinusUwyO8pc(DurationKt.toDuration(originNs, DurationUnit.DAYS));
        }
        long result = valueNs - originNs;
        if (((result ^ valueNs) & (~(result ^ originNs))) < 0) {
            long resultMs = (valueNs / 1000000) - (originNs / 1000000);
            long resultNs = (valueNs % 1000000) - (originNs % 1000000);
            Duration.Companion companion = Duration.INSTANCE;
            long duration = DurationKt.toDuration(resultMs, DurationUnit.MILLISECONDS);
            Duration.Companion companion2 = Duration.INSTANCE;
            return Duration.m1643plusLRDsOJo(duration, DurationKt.toDuration(resultNs, DurationUnit.NANOSECONDS));
        }
        Duration.Companion companion3 = Duration.INSTANCE;
        return DurationKt.toDuration(result, DurationUnit.NANOSECONDS);
    }
}
