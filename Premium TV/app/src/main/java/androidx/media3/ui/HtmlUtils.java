package androidx.media3.ui;

import android.graphics.Color;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
final class HtmlUtils {
    private HtmlUtils() {
    }

    public static String toCssRgba(int color) {
        return Util.formatInvariant("rgba(%d,%d,%d,%.3f)", Integer.valueOf(Color.red(color)), Integer.valueOf(Color.green(color)), Integer.valueOf(Color.blue(color)), Double.valueOf(((double) Color.alpha(color)) / 255.0d));
    }

    public static String cssAllClassDescendantsSelector(String className) {
        return "." + className + ",." + className + " *";
    }
}
