package androidx.media3.common.text;

import android.os.Bundle;
import android.text.Spannable;
import android.text.Spanned;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
final class CustomSpanBundler {
    private static final int HORIZONTAL_TEXT_IN_VERTICAL_CONTEXT = 3;
    private static final int RUBY = 1;
    private static final int TEXT_EMPHASIS = 2;
    private static final int UNKNOWN = -1;
    private static final String FIELD_START_INDEX = Util.intToStringMaxRadix(0);
    private static final String FIELD_END_INDEX = Util.intToStringMaxRadix(1);
    private static final String FIELD_FLAGS = Util.intToStringMaxRadix(2);
    private static final String FIELD_TYPE = Util.intToStringMaxRadix(3);
    private static final String FIELD_PARAMS = Util.intToStringMaxRadix(4);

    public static ArrayList<Bundle> bundleCustomSpans(Spanned text) {
        ArrayList<Bundle> bundledCustomSpans = new ArrayList<>();
        for (RubySpan span : (RubySpan[]) text.getSpans(0, text.length(), RubySpan.class)) {
            Bundle bundle = spanToBundle(text, span, 1, span.toBundle());
            bundledCustomSpans.add(bundle);
        }
        for (TextEmphasisSpan span2 : (TextEmphasisSpan[]) text.getSpans(0, text.length(), TextEmphasisSpan.class)) {
            Bundle bundle2 = spanToBundle(text, span2, 2, span2.toBundle());
            bundledCustomSpans.add(bundle2);
        }
        for (HorizontalTextInVerticalContextSpan horizontalTextInVerticalContextSpan : (HorizontalTextInVerticalContextSpan[]) text.getSpans(0, text.length(), HorizontalTextInVerticalContextSpan.class)) {
            Bundle bundle3 = spanToBundle(text, horizontalTextInVerticalContextSpan, 3, null);
            bundledCustomSpans.add(bundle3);
        }
        return bundledCustomSpans;
    }

    public static void unbundleAndApplyCustomSpan(Bundle customSpanBundle, Spannable text) {
        int start = customSpanBundle.getInt(FIELD_START_INDEX);
        int end = customSpanBundle.getInt(FIELD_END_INDEX);
        int flags = customSpanBundle.getInt(FIELD_FLAGS);
        int customSpanType = customSpanBundle.getInt(FIELD_TYPE, -1);
        Bundle span = customSpanBundle.getBundle(FIELD_PARAMS);
        switch (customSpanType) {
            case 1:
                text.setSpan(RubySpan.fromBundle((Bundle) Assertions.checkNotNull(span)), start, end, flags);
                break;
            case 2:
                text.setSpan(TextEmphasisSpan.fromBundle((Bundle) Assertions.checkNotNull(span)), start, end, flags);
                break;
            case 3:
                text.setSpan(new HorizontalTextInVerticalContextSpan(), start, end, flags);
                break;
        }
    }

    private static Bundle spanToBundle(Spanned spanned, Object span, int spanType, Bundle params) {
        Bundle bundle = new Bundle();
        bundle.putInt(FIELD_START_INDEX, spanned.getSpanStart(span));
        bundle.putInt(FIELD_END_INDEX, spanned.getSpanEnd(span));
        bundle.putInt(FIELD_FLAGS, spanned.getSpanFlags(span));
        bundle.putInt(FIELD_TYPE, spanType);
        if (params != null) {
            bundle.putBundle(FIELD_PARAMS, params);
        }
        return bundle;
    }

    private CustomSpanBundler() {
    }
}
