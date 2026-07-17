package androidx.media3.extractor.text.ttml;

import android.text.TextUtils;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class TextEmphasis {
    public static final int MARK_SHAPE_AUTO = -1;
    public static final int POSITION_OUTSIDE = -2;
    public final int markFill;
    public final int markShape;
    public final int position;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final ImmutableSet<String> SINGLE_STYLE_VALUES = ImmutableSet.of(TtmlNode.TEXT_EMPHASIS_AUTO, "none");
    private static final ImmutableSet<String> MARK_SHAPE_VALUES = ImmutableSet.of(TtmlNode.TEXT_EMPHASIS_MARK_DOT, TtmlNode.TEXT_EMPHASIS_MARK_SESAME, TtmlNode.TEXT_EMPHASIS_MARK_CIRCLE);
    private static final ImmutableSet<String> MARK_FILL_VALUES = ImmutableSet.of(TtmlNode.TEXT_EMPHASIS_MARK_FILLED, TtmlNode.TEXT_EMPHASIS_MARK_OPEN);
    private static final ImmutableSet<String> POSITION_VALUES = ImmutableSet.of(TtmlNode.ANNOTATION_POSITION_AFTER, TtmlNode.ANNOTATION_POSITION_BEFORE, TtmlNode.ANNOTATION_POSITION_OUTSIDE);

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Position {
    }

    private TextEmphasis(int markShape, int markFill, int position) {
        this.markShape = markShape;
        this.markFill = markFill;
        this.position = position;
    }

    public static TextEmphasis parse(String value) {
        if (value == null) {
            return null;
        }
        String parsingValue = Ascii.toLowerCase(value.trim());
        if (parsingValue.isEmpty()) {
            return null;
        }
        return parseWords(ImmutableSet.copyOf(TextUtils.split(parsingValue, WHITESPACE_PATTERN)));
    }

    private static TextEmphasis parseWords(ImmutableSet<String> nodes) {
        int position;
        int markFill;
        int markShape;
        int markShape2;
        Set<String> matchingPositions = Sets.intersection(POSITION_VALUES, nodes);
        switch ((String) Iterables.getFirst(matchingPositions, TtmlNode.ANNOTATION_POSITION_OUTSIDE)) {
            case "after":
                position = 2;
                break;
            case "outside":
                position = -2;
                break;
            case "before":
            default:
                position = 1;
                break;
        }
        Set<String> matchingSingleStyles = Sets.intersection(SINGLE_STYLE_VALUES, nodes);
        if (!matchingSingleStyles.isEmpty()) {
            switch (matchingSingleStyles.iterator().next()) {
                case "none":
                    markShape2 = 0;
                    break;
                case "auto":
                default:
                    markShape2 = -1;
                    break;
            }
            return new TextEmphasis(markShape2, 0, position);
        }
        Set<String> matchingFills = Sets.intersection(MARK_FILL_VALUES, nodes);
        Set<String> matchingShapes = Sets.intersection(MARK_SHAPE_VALUES, nodes);
        if (matchingFills.isEmpty() && matchingShapes.isEmpty()) {
            return new TextEmphasis(-1, 0, position);
        }
        switch ((String) Iterables.getFirst(matchingFills, TtmlNode.TEXT_EMPHASIS_MARK_FILLED)) {
            case "open":
                markFill = 2;
                break;
            case "filled":
            default:
                markFill = 1;
                break;
        }
        switch ((String) Iterables.getFirst(matchingShapes, TtmlNode.TEXT_EMPHASIS_MARK_CIRCLE)) {
            case "dot":
                markShape = 2;
                break;
            case "sesame":
                markShape = 3;
                break;
            case "circle":
            default:
                markShape = 1;
                break;
        }
        return new TextEmphasis(markShape, markFill, position);
    }
}
