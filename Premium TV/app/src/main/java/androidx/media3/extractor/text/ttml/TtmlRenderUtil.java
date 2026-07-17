package androidx.media3.extractor.text.ttml;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import androidx.media3.common.text.HorizontalTextInVerticalContextSpan;
import androidx.media3.common.text.RubySpan;
import androidx.media3.common.text.SpanUtil;
import androidx.media3.common.text.TextEmphasisSpan;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class TtmlRenderUtil {
    private static final String TAG = "TtmlRenderUtil";

    public static TtmlStyle resolveStyle(TtmlStyle style, String[] styleIds, Map<String, TtmlStyle> globalStyles) {
        int i = 0;
        if (style == null) {
            if (styleIds == null) {
                return null;
            }
            if (styleIds.length == 1) {
                return globalStyles.get(styleIds[0]);
            }
            if (styleIds.length > 1) {
                TtmlStyle chainedStyle = new TtmlStyle();
                int length = styleIds.length;
                while (i < length) {
                    String id = styleIds[i];
                    chainedStyle.chain(globalStyles.get(id));
                    i++;
                }
                return chainedStyle;
            }
        } else {
            if (styleIds != null && styleIds.length == 1) {
                return style.chain(globalStyles.get(styleIds[0]));
            }
            if (styleIds != null && styleIds.length > 1) {
                int length2 = styleIds.length;
                while (i < length2) {
                    String id2 = styleIds[i];
                    style.chain(globalStyles.get(id2));
                    i++;
                }
                return style;
            }
        }
        return style;
    }

    public static void applyStylesToSpan(Spannable builder, int start, int end, TtmlStyle style, TtmlNode parent, Map<String, TtmlStyle> globalStyles, int verticalType) {
        TtmlNode textNode;
        int markShape;
        int markFill;
        int position;
        if (style.getStyle() != -1) {
            builder.setSpan(new StyleSpan(style.getStyle()), start, end, 33);
        }
        if (style.isLinethrough()) {
            builder.setSpan(new StrikethroughSpan(), start, end, 33);
        }
        if (style.isUnderline()) {
            builder.setSpan(new UnderlineSpan(), start, end, 33);
        }
        if (style.hasFontColor()) {
            SpanUtil.addOrReplaceSpan(builder, new ForegroundColorSpan(style.getFontColor()), start, end, 33);
        }
        if (style.hasBackgroundColor()) {
            SpanUtil.addOrReplaceSpan(builder, new BackgroundColorSpan(style.getBackgroundColor()), start, end, 33);
        }
        if (style.getFontFamily() != null) {
            SpanUtil.addOrReplaceSpan(builder, new TypefaceSpan(style.getFontFamily()), start, end, 33);
        }
        if (style.getTextEmphasis() != null) {
            TextEmphasis textEmphasis = (TextEmphasis) Assertions.checkNotNull(style.getTextEmphasis());
            if (textEmphasis.markShape == -1) {
                if (verticalType == 2 || verticalType == 1) {
                    markShape = 3;
                } else {
                    markShape = 1;
                }
                markFill = 1;
            } else {
                markShape = textEmphasis.markShape;
                markFill = textEmphasis.markFill;
            }
            if (textEmphasis.position == -2) {
                position = 1;
            } else {
                position = textEmphasis.position;
            }
            SpanUtil.addOrReplaceSpan(builder, new TextEmphasisSpan(markShape, markFill, position), start, end, 33);
        }
        switch (style.getRubyType()) {
            case 2:
                TtmlNode containerNode = findRubyContainerNode(parent, globalStyles);
                if (containerNode != null && (textNode = findRubyTextNode(containerNode, globalStyles)) != null) {
                    if (textNode.getChildCount() == 1 && textNode.getChild(0).text != null) {
                        String rubyText = (String) Util.castNonNull(textNode.getChild(0).text);
                        TtmlStyle textStyle = resolveStyle(textNode.style, textNode.getStyleIds(), globalStyles);
                        int rubyPosition = textStyle != null ? textStyle.getRubyPosition() : -1;
                        if (rubyPosition == -1) {
                            TtmlStyle containerStyle = resolveStyle(containerNode.style, containerNode.getStyleIds(), globalStyles);
                            rubyPosition = containerStyle != null ? containerStyle.getRubyPosition() : rubyPosition;
                        }
                        builder.setSpan(new RubySpan(rubyText, rubyPosition), start, end, 33);
                    } else {
                        Log.i(TAG, "Skipping rubyText node without exactly one text child.");
                    }
                }
                break;
            case 3:
            case 4:
                builder.setSpan(new DeleteTextSpan(), start, end, 33);
                break;
        }
        if (style.getTextCombine()) {
            SpanUtil.addOrReplaceSpan(builder, new HorizontalTextInVerticalContextSpan(), start, end, 33);
        }
        switch (style.getFontSizeUnit()) {
            case 1:
                SpanUtil.addOrReplaceSpan(builder, new AbsoluteSizeSpan((int) style.getFontSize(), true), start, end, 33);
                break;
            case 2:
                SpanUtil.addOrReplaceSpan(builder, new RelativeSizeSpan(style.getFontSize()), start, end, 33);
                break;
            case 3:
                SpanUtil.addInheritedRelativeSizeSpan(builder, style.getFontSize() / 100.0f, start, end, 33);
                break;
        }
    }

    private static TtmlNode findRubyTextNode(TtmlNode rubyContainerNode, Map<String, TtmlStyle> globalStyles) {
        Deque<TtmlNode> childNodesStack = new ArrayDeque<>();
        childNodesStack.push(rubyContainerNode);
        while (!childNodesStack.isEmpty()) {
            TtmlNode childNode = childNodesStack.pop();
            TtmlStyle style = resolveStyle(childNode.style, childNode.getStyleIds(), globalStyles);
            if (style != null && style.getRubyType() == 3) {
                return childNode;
            }
            for (int i = childNode.getChildCount() - 1; i >= 0; i--) {
                childNodesStack.push(childNode.getChild(i));
            }
        }
        return null;
    }

    private static TtmlNode findRubyContainerNode(TtmlNode node, Map<String, TtmlStyle> globalStyles) {
        while (node != null) {
            TtmlStyle style = resolveStyle(node.style, node.getStyleIds(), globalStyles);
            if (style != null && style.getRubyType() == 1) {
                return node;
            }
            node = node.parent;
        }
        return null;
    }

    static void endParagraph(SpannableStringBuilder builder) {
        int position = builder.length() - 1;
        while (position >= 0 && builder.charAt(position) == ' ') {
            position--;
        }
        if (position >= 0 && builder.charAt(position) != '\n') {
            builder.append('\n');
        }
    }

    static String applyTextElementSpacePolicy(String in) {
        String out = in.replaceAll("\r\n", "\n");
        return out.replaceAll(" *\n *", "\n").replaceAll("\n", " ").replaceAll("[ \t\\x0B\f\r]+", " ");
    }

    private TtmlRenderUtil() {
    }
}
