package kotlin.time;

import kotlin.Metadata;

/* JADX INFO: compiled from: TimeSource.kt */
/* JADX INFO: loaded from: classes2.dex */
@Metadata(d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\bg\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H&ø\u0001\u0000ø\u0001\u0001J\b\u0010\u0004\u001a\u00020\u0005H\u0016J\b\u0010\u0006\u001a\u00020\u0005H\u0016J\u0014\u0010\u0007\u001a\u00020\u00002\u0006\u0010\b\u001a\u00020\u0003H\u0096\u0002ø\u0001\u0001J\u0014\u0010\t\u001a\u00020\u00002\u0006\u0010\b\u001a\u00020\u0003H\u0096\u0002ø\u0001\u0001\u0082\u0002\b\n\u0002\b!\n\u0002\b\u0019¨\u0006\n"}, d2 = {"Lkotlin/time/TimeMark;", "", "elapsedNow", "Lkotlin/time/Duration;", "hasNotPassedNow", "", "hasPassedNow", "minus", "duration", "plus", "kotlin-stdlib"}, k = 1, mv = {1, 7, 1}, xi = 48)
public interface TimeMark {
    /* JADX INFO: renamed from: elapsedNow-UwyO8pc */
    long mo1599elapsedNowUwyO8pc();

    boolean hasNotPassedNow();

    boolean hasPassedNow();

    /* JADX INFO: renamed from: minus-LRDsOJo */
    TimeMark mo1600minusLRDsOJo(long j);

    /* JADX INFO: renamed from: plus-LRDsOJo */
    TimeMark mo1601plusLRDsOJo(long j);

    /* JADX INFO: compiled from: TimeSource.kt */
    @Metadata(k = 3, mv = {1, 7, 1}, xi = 48)
    public static final class DefaultImpls {
        /* JADX INFO: renamed from: plus-LRDsOJo, reason: not valid java name */
        public static TimeMark m1743plusLRDsOJo(TimeMark $this, long duration) {
            return new AdjustedTimeMark($this, duration, null);
        }

        /* JADX INFO: renamed from: minus-LRDsOJo, reason: not valid java name */
        public static TimeMark m1742minusLRDsOJo(TimeMark $this, long duration) {
            return $this.mo1601plusLRDsOJo(Duration.m1659unaryMinusUwyO8pc(duration));
        }

        public static boolean hasPassedNow(TimeMark $this) {
            return !Duration.m1640isNegativeimpl($this.mo1599elapsedNowUwyO8pc());
        }

        public static boolean hasNotPassedNow(TimeMark $this) {
            return Duration.m1640isNegativeimpl($this.mo1599elapsedNowUwyO8pc());
        }
    }
}
