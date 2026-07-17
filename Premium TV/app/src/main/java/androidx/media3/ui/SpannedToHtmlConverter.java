package androidx.media3.ui;

import android.text.Html;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.SparseArray;
import androidx.media3.common.text.HorizontalTextInVerticalContextSpan;
import androidx.media3.common.text.RubySpan;
import androidx.media3.common.text.TextEmphasisSpan;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.text.ttml.TtmlNode;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class SpannedToHtmlConverter {
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("(&#13;)?&#10;");

    private SpannedToHtmlConverter() {
    }

    public static HtmlAndCss convert(CharSequence text, float displayDensity) {
        if (text == null) {
            return new HtmlAndCss("", ImmutableMap.of());
        }
        if (!(text instanceof Spanned)) {
            return new HtmlAndCss(escapeHtml(text), ImmutableMap.of());
        }
        Spanned spanned = (Spanned) text;
        Set<Integer> backgroundColors = new HashSet<>();
        for (BackgroundColorSpan backgroundColorSpan : (BackgroundColorSpan[]) spanned.getSpans(0, spanned.length(), BackgroundColorSpan.class)) {
            backgroundColors.add(Integer.valueOf(backgroundColorSpan.getBackgroundColor()));
        }
        HashMap<String, String> cssRuleSets = new HashMap<>();
        Iterator<Integer> it = backgroundColors.iterator();
        while (it.hasNext()) {
            int backgroundColor = it.next().intValue();
            cssRuleSets.put(HtmlUtils.cssAllClassDescendantsSelector("bg_" + backgroundColor), Util.formatInvariant("background-color:%s;", HtmlUtils.toCssRgba(backgroundColor)));
        }
        SparseArray<Transition> spanTransitions = findSpanTransitions(spanned, displayDensity);
        StringBuilder html = new StringBuilder(spanned.length());
        int previousTransition = 0;
        for (int i = 0; i < spanTransitions.size(); i++) {
            int index = spanTransitions.keyAt(i);
            html.append(escapeHtml(spanned.subSequence(previousTransition, index)));
            Transition transition = spanTransitions.get(index);
            Collections.sort(transition.spansRemoved, SpanInfo.FOR_CLOSING_TAGS);
            for (SpanInfo spanInfo : transition.spansRemoved) {
                html.append(spanInfo.closingTag);
            }
            Collections.sort(transition.spansAdded, SpanInfo.FOR_OPENING_TAGS);
            for (SpanInfo spanInfo2 : transition.spansAdded) {
                html.append(spanInfo2.openingTag);
            }
            previousTransition = index;
        }
        int i2 = spanned.length();
        html.append(escapeHtml(spanned.subSequence(previousTransition, i2)));
        return new HtmlAndCss(html.toString(), cssRuleSets);
    }

    private static SparseArray<Transition> findSpanTransitions(Spanned spanned, float displayDensity) {
        SparseArray<Transition> spanTransitions = new SparseArray<>();
        for (Object span : spanned.getSpans(0, spanned.length(), Object.class)) {
            String openingTag = getOpeningTag(span, displayDensity);
            String closingTag = getClosingTag(span);
            int spanStart = spanned.getSpanStart(span);
            int spanEnd = spanned.getSpanEnd(span);
            if (openingTag != null) {
                Assertions.checkNotNull(closingTag);
                SpanInfo spanInfo = new SpanInfo(spanStart, spanEnd, openingTag, closingTag);
                getOrCreate(spanTransitions, spanStart).spansAdded.add(spanInfo);
                getOrCreate(spanTransitions, spanEnd).spansRemoved.add(spanInfo);
            }
        }
        return spanTransitions;
    }

    private static String getOpeningTag(Object span, float displayDensity) {
        float sizeCssPx;
        if (span instanceof StrikethroughSpan) {
            return "<span style='text-decoration:line-through;'>";
        }
        if (span instanceof ForegroundColorSpan) {
            ForegroundColorSpan colorSpan = (ForegroundColorSpan) span;
            return Util.formatInvariant("<span style='color:%s;'>", HtmlUtils.toCssRgba(colorSpan.getForegroundColor()));
        }
        if (span instanceof BackgroundColorSpan) {
            BackgroundColorSpan colorSpan2 = (BackgroundColorSpan) span;
            return Util.formatInvariant("<span class='bg_%s'>", Integer.valueOf(colorSpan2.getBackgroundColor()));
        }
        if (span instanceof HorizontalTextInVerticalContextSpan) {
            return "<span style='text-combine-upright:all;'>";
        }
        if (span instanceof AbsoluteSizeSpan) {
            AbsoluteSizeSpan absoluteSizeSpan = (AbsoluteSizeSpan) span;
            if (absoluteSizeSpan.getDip()) {
                sizeCssPx = absoluteSizeSpan.getSize();
            } else {
                sizeCssPx = absoluteSizeSpan.getSize() / displayDensity;
            }
            return Util.formatInvariant("<span style='font-size:%.2fpx;'>", Float.valueOf(sizeCssPx));
        }
        if (span instanceof RelativeSizeSpan) {
            return Util.formatInvariant("<span style='font-size:%.2f%%;'>", Float.valueOf(((RelativeSizeSpan) span).getSizeChange() * 100.0f));
        }
        if (span instanceof TypefaceSpan) {
            String fontFamily = ((TypefaceSpan) span).getFamily();
            if (fontFamily != null) {
                return Util.formatInvariant("<span style='font-family:\"%s\";'>", fontFamily);
            }
            return null;
        }
        if (span instanceof StyleSpan) {
            switch (((StyleSpan) span).getStyle()) {
                case 1:
                    return "<b>";
                case 2:
                    return "<i>";
                case 3:
                    return "<b><i>";
                default:
                    return null;
            }
        }
        if (span instanceof RubySpan) {
            RubySpan rubySpan = (RubySpan) span;
            switch (rubySpan.position) {
                case -1:
                    return "<ruby style='ruby-position:unset;'>";
                case 0:
                default:
                    return null;
                case 1:
                    return "<ruby style='ruby-position:over;'>";
                case 2:
                    return "<ruby style='ruby-position:under;'>";
            }
        }
        if (span instanceof UnderlineSpan) {
            return "<u>";
        }
        if (!(span instanceof TextEmphasisSpan)) {
            return null;
        }
        TextEmphasisSpan textEmphasisSpan = (TextEmphasisSpan) span;
        String style = getTextEmphasisStyle(textEmphasisSpan.markShape, textEmphasisSpan.markFill);
        String position = getTextEmphasisPosition(textEmphasisSpan.position);
        return Util.formatInvariant("<span style='-webkit-text-emphasis-style:%1$s;text-emphasis-style:%1$s;-webkit-text-emphasis-position:%2$s;text-emphasis-position:%2$s;display:inline-block;'>", style, position);
    }

    private static String getClosingTag(Object span) {
        if ((span instanceof StrikethroughSpan) || (span instanceof ForegroundColorSpan) || (span instanceof BackgroundColorSpan) || (span instanceof HorizontalTextInVerticalContextSpan) || (span instanceof AbsoluteSizeSpan) || (span instanceof RelativeSizeSpan) || (span instanceof TextEmphasisSpan)) {
            return "</span>";
        }
        if (span instanceof TypefaceSpan) {
            String fontFamily = ((TypefaceSpan) span).getFamily();
            if (fontFamily != null) {
                return "</span>";
            }
            return null;
        }
        if (span instanceof StyleSpan) {
            switch (((StyleSpan) span).getStyle()) {
                case 1:
                    return "</b>";
                case 2:
                    return "</i>";
                case 3:
                    return "</i></b>";
            }
        }
        if (span instanceof RubySpan) {
            RubySpan rubySpan = (RubySpan) span;
            return "<rt>" + escapeHtml(rubySpan.rubyText) + "</rt></ruby>";
        }
        if (span instanceof UnderlineSpan) {
            return "</u>";
        }
        return null;
    }

    private static String getTextEmphasisStyle(int shape, int fill) {
        StringBuilder builder = new StringBuilder();
        switch (fill) {
            case 1:
                builder.append("filled ");
                break;
            case 2:
                builder.append("open ");
                break;
        }
        switch (shape) {
            case 0:
                builder.append("none");
                break;
            case 1:
                builder.append(TtmlNode.TEXT_EMPHASIS_MARK_CIRCLE);
                break;
            case 2:
                builder.append(TtmlNode.TEXT_EMPHASIS_MARK_DOT);
                break;
            case 3:
                builder.append(TtmlNode.TEXT_EMPHASIS_MARK_SESAME);
                break;
            default:
                builder.append("unset");
                break;
        }
        return builder.toString();
    }

    private static String getTextEmphasisPosition(int position) {
        switch (position) {
            case 2:
                return "under left";
            default:
                return "over right";
        }
    }

    private static Transition getOrCreate(SparseArray<Transition> transitions, int key) {
        Transition transition = transitions.get(key);
        if (transition == null) {
            Transition transition2 = new Transition();
            transitions.put(key, transition2);
            return transition2;
        }
        return transition;
    }

    private static String escapeHtml(CharSequence text) {
        String escaped = Html.escapeHtml(text);
        return NEWLINE_PATTERN.matcher(escaped).replaceAll("<br>");
    }

    public static class HtmlAndCss {
        public final Map<String, String> cssRuleSets;
        public final String html;

        private HtmlAndCss(String html, Map<String, String> cssRuleSets) {
            this.html = html;
            this.cssRuleSets = cssRuleSets;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class SpanInfo {
        public final String closingTag;
        public final int end;
        public final String openingTag;
        public final int start;
        private static final Comparator<SpanInfo> FOR_OPENING_TAGS = new Comparator() { // from class: androidx.media3.ui.SpannedToHtmlConverter$SpanInfo$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return SpannedToHtmlConverter.SpanInfo.lambda$static$0((SpannedToHtmlConverter.SpanInfo) obj, (SpannedToHtmlConverter.SpanInfo) obj2);
            }
        };
        private static final Comparator<SpanInfo> FOR_CLOSING_TAGS = new Comparator() { // from class: androidx.media3.ui.SpannedToHtmlConverter$SpanInfo$$ExternalSyntheticLambda1
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return SpannedToHtmlConverter.SpanInfo.lambda$static$1((SpannedToHtmlConverter.SpanInfo) obj, (SpannedToHtmlConverter.SpanInfo) obj2);
            }
        };

        static /* synthetic */ int lambda$static$0(SpanInfo info1, SpanInfo info2) {
            int result = Integer.compare(info2.end, info1.end);
            if (result != 0) {
                return result;
            }
            int result2 = info1.openingTag.compareTo(info2.openingTag);
            if (result2 != 0) {
                return result2;
            }
            return info1.closingTag.compareTo(info2.closingTag);
        }

        static /* synthetic */ int lambda$static$1(SpanInfo info1, SpanInfo info2) {
            int result = Integer.compare(info2.start, info1.start);
            if (result != 0) {
                return result;
            }
            int result2 = info2.openingTag.compareTo(info1.openingTag);
            if (result2 != 0) {
                return result2;
            }
            return info2.closingTag.compareTo(info1.closingTag);
        }

        private SpanInfo(int start, int end, String openingTag, String closingTag) {
            this.start = start;
            this.end = end;
            this.openingTag = openingTag;
            this.closingTag = closingTag;
        }
    }

    private static final class Transition {
        private final List<SpanInfo> spansAdded = new ArrayList();
        private final List<SpanInfo> spansRemoved = new ArrayList();
    }
}
