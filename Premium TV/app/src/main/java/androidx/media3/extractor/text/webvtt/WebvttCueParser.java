package androidx.media3.extractor.text.webvtt;

import android.graphics.Color;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.HorizontalTextInVerticalContextSpan;
import androidx.media3.common.text.RubySpan;
import androidx.media3.common.text.SpanUtil;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public final class WebvttCueParser {
    private static final char CHAR_AMPERSAND = '&';
    private static final char CHAR_GREATER_THAN = '>';
    private static final char CHAR_LESS_THAN = '<';
    private static final char CHAR_SEMI_COLON = ';';
    private static final char CHAR_SLASH = '/';
    private static final char CHAR_SPACE = ' ';
    public static final Pattern CUE_HEADER_PATTERN = Pattern.compile("^(\\S+)\\s+-->\\s+(\\S+)(.*)?$");
    private static final Pattern CUE_SETTING_PATTERN = Pattern.compile("(\\S+?):(\\S+)");
    private static final Map<String, Integer> DEFAULT_BACKGROUND_COLORS;
    static final float DEFAULT_POSITION = 0.5f;
    private static final Map<String, Integer> DEFAULT_TEXT_COLORS;
    private static final String ENTITY_AMPERSAND = "amp";
    private static final String ENTITY_GREATER_THAN = "gt";
    private static final String ENTITY_LESS_THAN = "lt";
    private static final String ENTITY_NON_BREAK_SPACE = "nbsp";
    private static final int STYLE_BOLD = 1;
    private static final int STYLE_ITALIC = 2;
    private static final String TAG = "WebvttCueParser";
    private static final String TAG_BOLD = "b";
    private static final String TAG_CLASS = "c";
    private static final String TAG_ITALIC = "i";
    private static final String TAG_LANG = "lang";
    private static final String TAG_RUBY = "ruby";
    private static final String TAG_RUBY_TEXT = "rt";
    private static final String TAG_UNDERLINE = "u";
    private static final String TAG_VOICE = "v";
    private static final int TEXT_ALIGNMENT_CENTER = 2;
    private static final int TEXT_ALIGNMENT_END = 3;
    private static final int TEXT_ALIGNMENT_LEFT = 4;
    private static final int TEXT_ALIGNMENT_RIGHT = 5;
    private static final int TEXT_ALIGNMENT_START = 1;

    static {
        Map<String, Integer> defaultColors = new HashMap<>();
        defaultColors.put("white", Integer.valueOf(Color.rgb(255, 255, 255)));
        defaultColors.put("lime", Integer.valueOf(Color.rgb(0, 255, 0)));
        defaultColors.put("cyan", Integer.valueOf(Color.rgb(0, 255, 255)));
        defaultColors.put("red", Integer.valueOf(Color.rgb(255, 0, 0)));
        defaultColors.put("yellow", Integer.valueOf(Color.rgb(255, 255, 0)));
        defaultColors.put("magenta", Integer.valueOf(Color.rgb(255, 0, 255)));
        defaultColors.put("blue", Integer.valueOf(Color.rgb(0, 0, 255)));
        defaultColors.put("black", Integer.valueOf(Color.rgb(0, 0, 0)));
        DEFAULT_TEXT_COLORS = Collections.unmodifiableMap(defaultColors);
        Map<String, Integer> defaultBackgroundColors = new HashMap<>();
        defaultBackgroundColors.put("bg_white", Integer.valueOf(Color.rgb(255, 255, 255)));
        defaultBackgroundColors.put("bg_lime", Integer.valueOf(Color.rgb(0, 255, 0)));
        defaultBackgroundColors.put("bg_cyan", Integer.valueOf(Color.rgb(0, 255, 255)));
        defaultBackgroundColors.put("bg_red", Integer.valueOf(Color.rgb(255, 0, 0)));
        defaultBackgroundColors.put("bg_yellow", Integer.valueOf(Color.rgb(255, 255, 0)));
        defaultBackgroundColors.put("bg_magenta", Integer.valueOf(Color.rgb(255, 0, 255)));
        defaultBackgroundColors.put("bg_blue", Integer.valueOf(Color.rgb(0, 0, 255)));
        defaultBackgroundColors.put("bg_black", Integer.valueOf(Color.rgb(0, 0, 0)));
        DEFAULT_BACKGROUND_COLORS = Collections.unmodifiableMap(defaultBackgroundColors);
    }

    public static WebvttCueInfo parseCue(ParsableByteArray webvttData, List<WebvttCssStyle> styles) {
        String firstLine = webvttData.readLine();
        if (firstLine == null) {
            return null;
        }
        Matcher cueHeaderMatcher = CUE_HEADER_PATTERN.matcher(firstLine);
        if (cueHeaderMatcher.matches()) {
            return parseCue(null, cueHeaderMatcher, webvttData, styles);
        }
        String secondLine = webvttData.readLine();
        if (secondLine == null) {
            return null;
        }
        Matcher cueHeaderMatcher2 = CUE_HEADER_PATTERN.matcher(secondLine);
        if (!cueHeaderMatcher2.matches()) {
            return null;
        }
        return parseCue(firstLine.trim(), cueHeaderMatcher2, webvttData, styles);
    }

    static Cue.Builder parseCueSettingsList(String cueSettingsList) {
        WebvttCueInfoBuilder builder = new WebvttCueInfoBuilder();
        parseCueSettingsList(cueSettingsList, builder);
        return builder.toCueBuilder();
    }

    public static Cue newCueForText(CharSequence text) {
        WebvttCueInfoBuilder infoBuilder = new WebvttCueInfoBuilder();
        infoBuilder.text = text;
        return infoBuilder.toCueBuilder().build();
    }

    static SpannedString parseCueText(String id, String markup, List<WebvttCssStyle> styles) {
        int entityEndIndex;
        SpannableStringBuilder spannedText = new SpannableStringBuilder();
        ArrayDeque<StartTag> startTagStack = new ArrayDeque<>();
        int pos = 0;
        List<Element> nestedElements = new ArrayList<>();
        while (pos < markup.length()) {
            char curr = markup.charAt(pos);
            switch (curr) {
                case '&':
                    int semiColonEndIndex = markup.indexOf(59, pos + 1);
                    int spaceEndIndex = markup.indexOf(32, pos + 1);
                    if (semiColonEndIndex == -1) {
                        entityEndIndex = spaceEndIndex;
                    } else if (spaceEndIndex == -1) {
                        entityEndIndex = semiColonEndIndex;
                    } else {
                        entityEndIndex = Math.min(semiColonEndIndex, spaceEndIndex);
                    }
                    if (entityEndIndex != -1) {
                        applyEntity(markup.substring(pos + 1, entityEndIndex), spannedText);
                        if (entityEndIndex == spaceEndIndex) {
                            spannedText.append((CharSequence) " ");
                        }
                        pos = entityEndIndex + 1;
                    } else {
                        spannedText.append(curr);
                        pos++;
                    }
                    break;
                case '<':
                    if (pos + 1 >= markup.length()) {
                        pos++;
                    } else {
                        int ltPos = pos;
                        boolean isClosingTag = markup.charAt(ltPos + 1) == '/';
                        pos = findEndOfTag(markup, ltPos + 1);
                        boolean isVoidTag = markup.charAt(pos + (-2)) == '/';
                        String fullTagExpression = markup.substring((isClosingTag ? 2 : 1) + ltPos, isVoidTag ? pos - 2 : pos - 1);
                        if (!fullTagExpression.trim().isEmpty()) {
                            String tagName = getTagName(fullTagExpression);
                            if (isSupportedTag(tagName)) {
                                if (isClosingTag) {
                                    while (true) {
                                        if (startTagStack.isEmpty()) {
                                            pos = pos;
                                        } else {
                                            StartTag startTag = startTagStack.pop();
                                            applySpansForTag(id, startTag, nestedElements, spannedText, styles);
                                            if (startTagStack.isEmpty()) {
                                                nestedElements.clear();
                                            } else {
                                                nestedElements.add(new Element(startTag, spannedText.length()));
                                            }
                                            if (!startTag.name.equals(tagName)) {
                                                pos = pos;
                                            }
                                        }
                                    }
                                } else {
                                    pos = pos;
                                    if (!isVoidTag) {
                                        startTagStack.push(StartTag.buildStartTag(fullTagExpression, spannedText.length()));
                                    }
                                }
                                pos = pos;
                            }
                        }
                    }
                    break;
                default:
                    spannedText.append(curr);
                    pos++;
                    break;
            }
        }
        while (!startTagStack.isEmpty()) {
            applySpansForTag(id, startTagStack.pop(), nestedElements, spannedText, styles);
        }
        applySpansForTag(id, StartTag.buildWholeCueVirtualTag(), Collections.emptyList(), spannedText, styles);
        return SpannedString.valueOf(spannedText);
    }

    private static WebvttCueInfo parseCue(String id, Matcher cueHeaderMatcher, ParsableByteArray webvttData, List<WebvttCssStyle> styles) {
        WebvttCueInfoBuilder builder = new WebvttCueInfoBuilder();
        try {
            builder.startTimeUs = WebvttParserUtil.parseTimestampUs((String) Assertions.checkNotNull(cueHeaderMatcher.group(1)));
            builder.endTimeUs = WebvttParserUtil.parseTimestampUs((String) Assertions.checkNotNull(cueHeaderMatcher.group(2)));
            parseCueSettingsList((String) Assertions.checkNotNull(cueHeaderMatcher.group(3)), builder);
            StringBuilder textBuilder = new StringBuilder();
            String line = webvttData.readLine();
            while (!TextUtils.isEmpty(line)) {
                if (textBuilder.length() > 0) {
                    textBuilder.append("\n");
                }
                textBuilder.append(line.trim());
                line = webvttData.readLine();
            }
            String line2 = textBuilder.toString();
            builder.text = parseCueText(id, line2, styles);
            return builder.build();
        } catch (NumberFormatException e) {
            Log.w(TAG, "Skipping cue with bad header: " + cueHeaderMatcher.group());
            return null;
        }
    }

    private static void parseCueSettingsList(String cueSettingsList, WebvttCueInfoBuilder builder) {
        Matcher cueSettingMatcher = CUE_SETTING_PATTERN.matcher(cueSettingsList);
        while (cueSettingMatcher.find()) {
            String name = (String) Assertions.checkNotNull(cueSettingMatcher.group(1));
            String value = (String) Assertions.checkNotNull(cueSettingMatcher.group(2));
            try {
                if ("line".equals(name)) {
                    parseLineAttribute(value, builder);
                } else if ("align".equals(name)) {
                    builder.textAlignment = parseTextAlignment(value);
                } else if ("position".equals(name)) {
                    parsePositionAttribute(value, builder);
                } else if ("size".equals(name)) {
                    builder.size = WebvttParserUtil.parsePercentage(value);
                } else if (!"vertical".equals(name)) {
                    Log.w(TAG, "Unknown cue setting " + name + ":" + value);
                } else {
                    builder.verticalType = parseVerticalAttribute(value);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Skipping bad cue setting: " + cueSettingMatcher.group());
            }
        }
    }

    private static void parseLineAttribute(String s, WebvttCueInfoBuilder builder) {
        int commaIndex = s.indexOf(44);
        if (commaIndex != -1) {
            builder.lineAnchor = parseLineAnchor(s.substring(commaIndex + 1));
            s = s.substring(0, commaIndex);
        }
        if (s.endsWith("%")) {
            builder.line = WebvttParserUtil.parsePercentage(s);
            builder.lineType = 0;
        } else {
            builder.line = Integer.parseInt(s);
            builder.lineType = 1;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:17:0x0034  */
    private static int parseLineAnchor(String s) {
        switch (s) {
            case "start":
                return 0;
            case "center":
            case "middle":
                return 1;
            case "end":
                return 2;
            default:
                Log.w(TAG, "Invalid anchor value: " + s);
                return Integer.MIN_VALUE;
        }
    }

    private static void parsePositionAttribute(String s, WebvttCueInfoBuilder builder) {
        int commaIndex = s.indexOf(44);
        if (commaIndex != -1) {
            builder.positionAnchor = parsePositionAnchor(s.substring(commaIndex + 1));
            s = s.substring(0, commaIndex);
        }
        builder.position = WebvttParserUtil.parsePercentage(s);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:23:0x0048  */
    private static int parsePositionAnchor(String s) {
        switch (s) {
            case "line-left":
            case "start":
                return 0;
            case "center":
            case "middle":
                return 1;
            case "line-right":
            case "end":
                return 2;
            default:
                Log.w(TAG, "Invalid anchor value: " + s);
                return Integer.MIN_VALUE;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:11:0x001e  */
    private static int parseVerticalAttribute(String s) {
        switch (s) {
            case "rl":
                return 1;
            case "lr":
                return 2;
            default:
                Log.w(TAG, "Invalid 'vertical' value: " + s);
                return Integer.MIN_VALUE;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:23:0x004b  */
    private static int parseTextAlignment(String s) {
        switch (s) {
            case "start":
                return 1;
            case "left":
                return 4;
            case "center":
            case "middle":
                return 2;
            case "end":
                return 3;
            case "right":
                return 5;
            default:
                Log.w(TAG, "Invalid alignment value: " + s);
                return 2;
        }
    }

    private static int findEndOfTag(String markup, int startPos) {
        int index = markup.indexOf(62, startPos);
        return index == -1 ? markup.length() : index + 1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:17:0x0030  */
    private static void applyEntity(String entity, SpannableStringBuilder spannedText) {
        switch (entity) {
            case "lt":
                spannedText.append('<');
                break;
            case "gt":
                spannedText.append('>');
                break;
            case "nbsp":
                spannedText.append(CHAR_SPACE);
                break;
            case "amp":
                spannedText.append('&');
                break;
            default:
                Log.w(TAG, "ignoring unsupported entity: '&" + entity + ";'");
                break;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:29:0x005e  */
    private static boolean isSupportedTag(String tagName) {
        switch (tagName) {
            case "b":
            case "c":
            case "i":
            case "lang":
            case "ruby":
            case "rt":
            case "u":
            case "v":
                return true;
            default:
                return false;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:29:0x0065  */
    private static void applySpansForTag(String cueId, StartTag startTag, List<Element> nestedElements, SpannableStringBuilder text, List<WebvttCssStyle> styles) {
        int start = startTag.position;
        int end = text.length();
        switch (startTag.name) {
            case "b":
                text.setSpan(new StyleSpan(1), start, end, 33);
                break;
            case "i":
                text.setSpan(new StyleSpan(2), start, end, 33);
                break;
            case "ruby":
                applyRubySpans(text, cueId, startTag, nestedElements, styles);
                break;
            case "u":
                text.setSpan(new UnderlineSpan(), start, end, 33);
                break;
            case "c":
                applyDefaultColors(text, startTag.classes, start, end);
                break;
            case "lang":
            case "v":
            case "":
                break;
            default:
                return;
        }
        List<StyleMatch> applicableStyles = getApplicableStyles(styles, cueId, startTag);
        for (int i = 0; i < applicableStyles.size(); i++) {
            applyStyleToText(text, applicableStyles.get(i).style, start, end);
        }
    }

    private static void applyRubySpans(SpannableStringBuilder text, String cueId, StartTag startTag, List<Element> nestedElements, List<WebvttCssStyle> styles) {
        String str = cueId;
        int rubyTagPosition = getRubyPosition(styles, str, startTag);
        List<Element> sortedNestedElements = new ArrayList<>(nestedElements.size());
        sortedNestedElements.addAll(nestedElements);
        Collections.sort(sortedNestedElements, Element.BY_START_POSITION_ASC);
        int deletedCharCount = 0;
        int lastRubyTextEnd = startTag.position;
        int i = 0;
        while (i < sortedNestedElements.size()) {
            if (TAG_RUBY_TEXT.equals(sortedNestedElements.get(i).startTag.name)) {
                Element rubyTextElement = sortedNestedElements.get(i);
                int rubyPosition = firstKnownRubyPosition(getRubyPosition(styles, str, rubyTextElement.startTag), rubyTagPosition, 1);
                int adjustedRubyTextStart = rubyTextElement.startTag.position - deletedCharCount;
                int adjustedRubyTextEnd = rubyTextElement.endPosition - deletedCharCount;
                CharSequence rubyText = text.subSequence(adjustedRubyTextStart, adjustedRubyTextEnd);
                text.delete(adjustedRubyTextStart, adjustedRubyTextEnd);
                text.setSpan(new RubySpan(rubyText.toString(), rubyPosition), lastRubyTextEnd, adjustedRubyTextStart, 33);
                deletedCharCount += rubyText.length();
                lastRubyTextEnd = adjustedRubyTextStart;
            }
            i++;
            str = cueId;
        }
    }

    private static int getRubyPosition(List<WebvttCssStyle> styles, String cueId, StartTag startTag) {
        List<StyleMatch> styleMatches = getApplicableStyles(styles, cueId, startTag);
        for (int i = 0; i < styleMatches.size(); i++) {
            WebvttCssStyle style = styleMatches.get(i).style;
            if (style.getRubyPosition() != -1) {
                return style.getRubyPosition();
            }
        }
        return -1;
    }

    private static int firstKnownRubyPosition(int position1, int position2, int position3) {
        if (position1 != -1) {
            return position1;
        }
        if (position2 != -1) {
            return position2;
        }
        if (position3 != -1) {
            return position3;
        }
        throw new IllegalArgumentException();
    }

    private static void applyDefaultColors(SpannableStringBuilder text, Set<String> classes, int start, int end) {
        for (String className : classes) {
            if (DEFAULT_TEXT_COLORS.containsKey(className)) {
                int color = DEFAULT_TEXT_COLORS.get(className).intValue();
                text.setSpan(new ForegroundColorSpan(color), start, end, 33);
            } else if (DEFAULT_BACKGROUND_COLORS.containsKey(className)) {
                int color2 = DEFAULT_BACKGROUND_COLORS.get(className).intValue();
                text.setSpan(new BackgroundColorSpan(color2), start, end, 33);
            }
        }
    }

    private static void applyStyleToText(SpannableStringBuilder spannedText, WebvttCssStyle style, int start, int end) {
        if (style == null) {
            return;
        }
        if (style.getStyle() != -1) {
            SpanUtil.addOrReplaceSpan(spannedText, new StyleSpan(style.getStyle()), start, end, 33);
        }
        if (style.isLinethrough()) {
            spannedText.setSpan(new StrikethroughSpan(), start, end, 33);
        }
        if (style.isUnderline()) {
            spannedText.setSpan(new UnderlineSpan(), start, end, 33);
        }
        if (style.hasFontColor()) {
            SpanUtil.addOrReplaceSpan(spannedText, new ForegroundColorSpan(style.getFontColor()), start, end, 33);
        }
        if (style.hasBackgroundColor()) {
            SpanUtil.addOrReplaceSpan(spannedText, new BackgroundColorSpan(style.getBackgroundColor()), start, end, 33);
        }
        if (style.getFontFamily() != null) {
            SpanUtil.addOrReplaceSpan(spannedText, new TypefaceSpan(style.getFontFamily()), start, end, 33);
        }
        switch (style.getFontSizeUnit()) {
            case 1:
                SpanUtil.addOrReplaceSpan(spannedText, new AbsoluteSizeSpan((int) style.getFontSize(), true), start, end, 33);
                break;
            case 2:
                SpanUtil.addOrReplaceSpan(spannedText, new RelativeSizeSpan(style.getFontSize()), start, end, 33);
                break;
            case 3:
                SpanUtil.addOrReplaceSpan(spannedText, new RelativeSizeSpan(style.getFontSize() / 100.0f), start, end, 33);
                break;
        }
        if (style.getCombineUpright()) {
            spannedText.setSpan(new HorizontalTextInVerticalContextSpan(), start, end, 33);
        }
    }

    private static String getTagName(String tagExpression) {
        String tagExpression2 = tagExpression.trim();
        Assertions.checkArgument(!tagExpression2.isEmpty());
        return Util.splitAtFirst(tagExpression2, "[ \\.]")[0];
    }

    private static List<StyleMatch> getApplicableStyles(List<WebvttCssStyle> declaredStyles, String id, StartTag tag) {
        List<StyleMatch> applicableStyles = new ArrayList<>();
        for (int i = 0; i < declaredStyles.size(); i++) {
            WebvttCssStyle style = declaredStyles.get(i);
            int score = style.getSpecificityScore(id, tag.name, tag.classes, tag.voice);
            if (score > 0) {
                applicableStyles.add(new StyleMatch(score, style));
            }
        }
        Collections.sort(applicableStyles);
        return applicableStyles;
    }

    private static final class WebvttCueInfoBuilder {
        public CharSequence text;
        public long startTimeUs = 0;
        public long endTimeUs = 0;
        public int textAlignment = 2;
        public float line = -3.4028235E38f;
        public int lineType = 1;
        public int lineAnchor = 0;
        public float position = -3.4028235E38f;
        public int positionAnchor = Integer.MIN_VALUE;
        public float size = 1.0f;
        public int verticalType = Integer.MIN_VALUE;

        public WebvttCueInfo build() {
            return new WebvttCueInfo(toCueBuilder().build(), this.startTimeUs, this.endTimeUs);
        }

        public Cue.Builder toCueBuilder() {
            int positionAnchor;
            float position = this.position != -3.4028235E38f ? this.position : derivePosition(this.textAlignment);
            if (this.positionAnchor != Integer.MIN_VALUE) {
                positionAnchor = this.positionAnchor;
            } else {
                positionAnchor = derivePositionAnchor(this.textAlignment);
            }
            Cue.Builder cueBuilder = new Cue.Builder().setTextAlignment(convertTextAlignment(this.textAlignment)).setLine(computeLine(this.line, this.lineType), this.lineType).setLineAnchor(this.lineAnchor).setPosition(position).setPositionAnchor(positionAnchor).setSize(Math.min(this.size, deriveMaxSize(positionAnchor, position))).setVerticalType(this.verticalType);
            if (this.text != null) {
                cueBuilder.setText(this.text);
            }
            return cueBuilder;
        }

        private static float computeLine(float line, int lineType) {
            if (line != -3.4028235E38f && lineType == 0 && (line < 0.0f || line > 1.0f)) {
                return 1.0f;
            }
            if (line != -3.4028235E38f) {
                return line;
            }
            return lineType == 0 ? 1.0f : -3.4028235E38f;
        }

        private static float derivePosition(int textAlignment) {
            switch (textAlignment) {
                case 4:
                    return 0.0f;
                case 5:
                    return 1.0f;
                default:
                    return 0.5f;
            }
        }

        private static int derivePositionAnchor(int textAlignment) {
            switch (textAlignment) {
                case 1:
                case 4:
                    return 0;
                case 2:
                default:
                    return 1;
                case 3:
                case 5:
                    return 2;
            }
        }

        private static Layout.Alignment convertTextAlignment(int textAlignment) {
            switch (textAlignment) {
                case 1:
                case 4:
                    return Layout.Alignment.ALIGN_NORMAL;
                case 2:
                    return Layout.Alignment.ALIGN_CENTER;
                case 3:
                case 5:
                    return Layout.Alignment.ALIGN_OPPOSITE;
                default:
                    Log.w(WebvttCueParser.TAG, "Unknown textAlignment: " + textAlignment);
                    return null;
            }
        }

        private static float deriveMaxSize(int positionAnchor, float position) {
            switch (positionAnchor) {
                case 0:
                    return 1.0f - position;
                case 1:
                    if (position <= 0.5f) {
                        return 2.0f * position;
                    }
                    return (1.0f - position) * 2.0f;
                case 2:
                    return position;
                default:
                    throw new IllegalStateException(String.valueOf(positionAnchor));
            }
        }
    }

    private static final class StyleMatch implements Comparable<StyleMatch> {
        public final int score;
        public final WebvttCssStyle style;

        public StyleMatch(int score, WebvttCssStyle style) {
            this.score = score;
            this.style = style;
        }

        @Override // java.lang.Comparable
        public int compareTo(StyleMatch another) {
            return Integer.compare(this.score, another.score);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class StartTag {
        public final Set<String> classes;
        public final String name;
        public final int position;
        public final String voice;

        private StartTag(String name, int position, String voice, Set<String> classes) {
            this.position = position;
            this.name = name;
            this.voice = voice;
            this.classes = classes;
        }

        public static StartTag buildStartTag(String fullTagExpression, int position) {
            String voice;
            String fullTagExpression2 = fullTagExpression.trim();
            Assertions.checkArgument(!fullTagExpression2.isEmpty());
            int voiceStartIndex = fullTagExpression2.indexOf(" ");
            if (voiceStartIndex == -1) {
                voice = "";
            } else {
                String voice2 = fullTagExpression2.substring(voiceStartIndex);
                voice = voice2.trim();
                fullTagExpression2 = fullTagExpression2.substring(0, voiceStartIndex);
            }
            String[] nameAndClasses = Util.split(fullTagExpression2, "\\.");
            String name = nameAndClasses[0];
            Set<String> classes = new HashSet<>();
            for (int i = 1; i < nameAndClasses.length; i++) {
                classes.add(nameAndClasses[i]);
            }
            return new StartTag(name, position, voice, classes);
        }

        public static StartTag buildWholeCueVirtualTag() {
            return new StartTag("", 0, "", Collections.emptySet());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class Element {
        private static final Comparator<Element> BY_START_POSITION_ASC = new Comparator() { // from class: androidx.media3.extractor.text.webvtt.WebvttCueParser$Element$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return Integer.compare(((WebvttCueParser.Element) obj).startTag.position, ((WebvttCueParser.Element) obj2).startTag.position);
            }
        };
        private final int endPosition;
        private final StartTag startTag;

        private Element(StartTag startTag, int endPosition) {
            this.startTag = startTag;
            this.endPosition = endPosition;
        }
    }
}
