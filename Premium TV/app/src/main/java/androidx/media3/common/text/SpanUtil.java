package androidx.media3.common.text;

import android.text.Spannable;
import android.text.style.RelativeSizeSpan;

/* JADX INFO: loaded from: classes.dex */
public final class SpanUtil {
    public static void addOrReplaceSpan(Spannable spannable, Object span, int start, int end, int spanFlags) {
        Object[] existingSpans = spannable.getSpans(start, end, span.getClass());
        for (Object existingSpan : existingSpans) {
            removeIfStartEndAndFlagsMatch(spannable, existingSpan, start, end, spanFlags);
        }
        spannable.setSpan(span, start, end, spanFlags);
    }

    public static void addInheritedRelativeSizeSpan(Spannable spannable, float size, int start, int end, int spanFlags) {
        for (RelativeSizeSpan existingSpan : (RelativeSizeSpan[]) spannable.getSpans(start, end, RelativeSizeSpan.class)) {
            if (spannable.getSpanStart(existingSpan) <= start && spannable.getSpanEnd(existingSpan) >= end) {
                size *= existingSpan.getSizeChange();
            }
            removeIfStartEndAndFlagsMatch(spannable, existingSpan, start, end, spanFlags);
        }
        spannable.setSpan(new RelativeSizeSpan(size), start, end, spanFlags);
    }

    private static void removeIfStartEndAndFlagsMatch(Spannable spannable, Object span, int start, int end, int spanFlags) {
        if (spannable.getSpanStart(span) == start && spannable.getSpanEnd(span) == end && spannable.getSpanFlags(span) == spanFlags) {
            spannable.removeSpan(span);
        }
    }

    private SpanUtil() {
    }
}
