package androidx.media3.common;

/* JADX INFO: loaded from: classes.dex */
public interface Effect {
    long getDurationAfterEffectApplied(long j);

    /* JADX INFO: renamed from: androidx.media3.common.Effect$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static long $default$getDurationAfterEffectApplied(Effect _this, long durationUs) {
            return durationUs;
        }
    }
}
