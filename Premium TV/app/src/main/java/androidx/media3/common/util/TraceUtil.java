package androidx.media3.common.util;

import android.os.Trace;

/* JADX INFO: loaded from: classes.dex */
public final class TraceUtil {
    private TraceUtil() {
    }

    public static void beginSection(String sectionName) {
        Trace.beginSection(sectionName);
    }

    public static void endSection() {
        Trace.endSection();
    }
}
