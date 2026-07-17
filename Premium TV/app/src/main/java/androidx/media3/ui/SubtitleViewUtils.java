package androidx.media3.ui;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.LanguageFeatureSpan;
import androidx.media3.common.util.Assertions;
import com.google.common.base.Predicate;

/* JADX INFO: loaded from: classes.dex */
final class SubtitleViewUtils {
    public static float resolveTextSize(int textSizeType, float textSize, int rawViewHeight, int viewHeightMinusPadding) {
        if (textSize == -3.4028235E38f) {
            return -3.4028235E38f;
        }
        switch (textSizeType) {
            case 0:
                return viewHeightMinusPadding * textSize;
            case 1:
                return rawViewHeight * textSize;
            case 2:
                return textSize;
            default:
                return -3.4028235E38f;
        }
    }

    public static void removeAllEmbeddedStyling(Cue.Builder cue) {
        cue.clearWindowColor();
        if (cue.getText() instanceof Spanned) {
            if (!(cue.getText() instanceof Spannable)) {
                cue.setText(SpannableString.valueOf(cue.getText()));
            }
            removeSpansIf((Spannable) Assertions.checkNotNull(cue.getText()), new Predicate() { // from class: androidx.media3.ui.SubtitleViewUtils$$ExternalSyntheticLambda0
                @Override // com.google.common.base.Predicate
                public final boolean apply(Object obj) {
                    return SubtitleViewUtils.lambda$removeAllEmbeddedStyling$0(obj);
                }
            });
        }
        removeEmbeddedFontSizes(cue);
    }

    static /* synthetic */ boolean lambda$removeAllEmbeddedStyling$0(Object span) {
        return !(span instanceof LanguageFeatureSpan);
    }

    public static void removeEmbeddedFontSizes(Cue.Builder cue) {
        cue.setTextSize(-3.4028235E38f, Integer.MIN_VALUE);
        if (cue.getText() instanceof Spanned) {
            if (!(cue.getText() instanceof Spannable)) {
                cue.setText(SpannableString.valueOf(cue.getText()));
            }
            removeSpansIf((Spannable) Assertions.checkNotNull(cue.getText()), new Predicate() { // from class: androidx.media3.ui.SubtitleViewUtils$$ExternalSyntheticLambda1
                @Override // com.google.common.base.Predicate
                public final boolean apply(Object obj) {
                    return SubtitleViewUtils.lambda$removeEmbeddedFontSizes$1(obj);
                }
            });
        }
    }

    static /* synthetic */ boolean lambda$removeEmbeddedFontSizes$1(Object span) {
        return (span instanceof AbsoluteSizeSpan) || (span instanceof RelativeSizeSpan);
    }

    private static void removeSpansIf(Spannable spannable, Predicate<Object> removeFilter) {
        Object[] spans = spannable.getSpans(0, spannable.length(), Object.class);
        for (Object span : spans) {
            if (removeFilter.apply(span)) {
                spannable.removeSpan(span);
            }
        }
    }

    private SubtitleViewUtils() {
    }
}
