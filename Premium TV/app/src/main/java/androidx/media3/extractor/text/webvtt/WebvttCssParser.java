package androidx.media3.extractor.text.webvtt;

import android.text.TextUtils;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ColorParser;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import com.google.common.base.Ascii;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class WebvttCssParser {
    private static final String PROPERTY_BGCOLOR = "background-color";
    private static final String PROPERTY_COLOR = "color";
    private static final String PROPERTY_FONT_FAMILY = "font-family";
    private static final String PROPERTY_FONT_SIZE = "font-size";
    private static final String PROPERTY_FONT_STYLE = "font-style";
    private static final String PROPERTY_FONT_WEIGHT = "font-weight";
    private static final String PROPERTY_RUBY_POSITION = "ruby-position";
    private static final String PROPERTY_TEXT_COMBINE_UPRIGHT = "text-combine-upright";
    private static final String PROPERTY_TEXT_DECORATION = "text-decoration";
    private static final String RULE_END = "}";
    private static final String RULE_START = "{";
    private static final String TAG = "WebvttCssParser";
    private static final String VALUE_ALL = "all";
    private static final String VALUE_BOLD = "bold";
    private static final String VALUE_DIGITS = "digits";
    private static final String VALUE_ITALIC = "italic";
    private static final String VALUE_OVER = "over";
    private static final String VALUE_UNDER = "under";
    private static final String VALUE_UNDERLINE = "underline";
    private static final Pattern VOICE_NAME_PATTERN = Pattern.compile("\\[voice=\"([^\"]*)\"\\]");
    private static final Pattern FONT_SIZE_PATTERN = Pattern.compile("^((?:[0-9]*\\.)?[0-9]+)(px|em|%)$");
    private final ParsableByteArray styleInput = new ParsableByteArray();
    private final StringBuilder stringBuilder = new StringBuilder();

    public List<WebvttCssStyle> parseBlock(ParsableByteArray input) {
        this.stringBuilder.setLength(0);
        int initialInputPosition = input.getPosition();
        skipStyleBlock(input);
        this.styleInput.reset(input.getData(), input.getPosition());
        this.styleInput.setPosition(initialInputPosition);
        List<WebvttCssStyle> styles = new ArrayList<>();
        while (true) {
            String selector = parseSelector(this.styleInput, this.stringBuilder);
            if (selector != null) {
                if (!RULE_START.equals(parseNextToken(this.styleInput, this.stringBuilder))) {
                    return styles;
                }
                WebvttCssStyle style = new WebvttCssStyle();
                applySelectorToStyle(style, selector);
                String token = null;
                boolean blockEndFound = false;
                while (!blockEndFound) {
                    int position = this.styleInput.getPosition();
                    token = parseNextToken(this.styleInput, this.stringBuilder);
                    blockEndFound = token == null || RULE_END.equals(token);
                    if (!blockEndFound) {
                        this.styleInput.setPosition(position);
                        parseStyleDeclaration(this.styleInput, style, this.stringBuilder);
                    }
                }
                if (RULE_END.equals(token)) {
                    styles.add(style);
                }
            } else {
                return styles;
            }
        }
    }

    private static String parseSelector(ParsableByteArray input, StringBuilder stringBuilder) {
        skipWhitespaceAndComments(input);
        if (input.bytesLeft() < 5) {
            return null;
        }
        String cueSelector = input.readString(5);
        if (!"::cue".equals(cueSelector)) {
            return null;
        }
        int position = input.getPosition();
        String token = parseNextToken(input, stringBuilder);
        if (token == null) {
            return null;
        }
        if (RULE_START.equals(token)) {
            input.setPosition(position);
            return "";
        }
        String target = null;
        if ("(".equals(token)) {
            target = readCueTarget(input);
        }
        if (")".equals(parseNextToken(input, stringBuilder))) {
            return target;
        }
        return null;
    }

    private static String readCueTarget(ParsableByteArray input) {
        int position = input.getPosition();
        int limit = input.limit();
        boolean cueTargetEndFound = false;
        while (position < limit && !cueTargetEndFound) {
            int position2 = position + 1;
            char c = (char) input.getData()[position];
            cueTargetEndFound = c == ')';
            position = position2;
        }
        return input.readString((position - 1) - input.getPosition()).trim();
    }

    private static void parseStyleDeclaration(ParsableByteArray input, WebvttCssStyle style, StringBuilder stringBuilder) {
        skipWhitespaceAndComments(input);
        String property = parseIdentifier(input, stringBuilder);
        if ("".equals(property) || !":".equals(parseNextToken(input, stringBuilder))) {
            return;
        }
        skipWhitespaceAndComments(input);
        String value = parsePropertyValue(input, stringBuilder);
        if (value == null || "".equals(value)) {
            return;
        }
        int position = input.getPosition();
        String token = parseNextToken(input, stringBuilder);
        if (!";".equals(token)) {
            if (RULE_END.equals(token)) {
                input.setPosition(position);
            } else {
                return;
            }
        }
        if ("color".equals(property)) {
            style.setFontColor(ColorParser.parseCssColor(value));
            return;
        }
        if (PROPERTY_BGCOLOR.equals(property)) {
            style.setBackgroundColor(ColorParser.parseCssColor(value));
            return;
        }
        boolean z = true;
        if (PROPERTY_RUBY_POSITION.equals(property)) {
            if (VALUE_OVER.equals(value)) {
                style.setRubyPosition(1);
                return;
            } else {
                if (VALUE_UNDER.equals(value)) {
                    style.setRubyPosition(2);
                    return;
                }
                return;
            }
        }
        if (PROPERTY_TEXT_COMBINE_UPRIGHT.equals(property)) {
            if (!"all".equals(value) && !value.startsWith(VALUE_DIGITS)) {
                z = false;
            }
            style.setCombineUpright(z);
            return;
        }
        if (PROPERTY_TEXT_DECORATION.equals(property)) {
            if ("underline".equals(value)) {
                style.setUnderline(true);
                return;
            }
            return;
        }
        if (PROPERTY_FONT_FAMILY.equals(property)) {
            style.setFontFamily(value);
            return;
        }
        if (PROPERTY_FONT_WEIGHT.equals(property)) {
            if ("bold".equals(value)) {
                style.setBold(true);
            }
        } else if (PROPERTY_FONT_STYLE.equals(property)) {
            if ("italic".equals(value)) {
                style.setItalic(true);
            }
        } else if (PROPERTY_FONT_SIZE.equals(property)) {
            parseFontSize(value, style);
        }
    }

    static void skipWhitespaceAndComments(ParsableByteArray input) {
        boolean skipping = true;
        while (input.bytesLeft() > 0 && skipping) {
            skipping = maybeSkipWhitespace(input) || maybeSkipComment(input);
        }
    }

    static String parseNextToken(ParsableByteArray input, StringBuilder stringBuilder) {
        skipWhitespaceAndComments(input);
        if (input.bytesLeft() == 0) {
            return null;
        }
        String identifier = parseIdentifier(input, stringBuilder);
        if ("".equals(identifier)) {
            return "" + ((char) input.readUnsignedByte());
        }
        return identifier;
    }

    private static boolean maybeSkipWhitespace(ParsableByteArray input) {
        switch (peekCharAtPosition(input, input.getPosition())) {
            case '\t':
            case '\n':
            case '\f':
            case '\r':
            case ' ':
                input.skipBytes(1);
                return true;
            default:
                return false;
        }
    }

    static void skipStyleBlock(ParsableByteArray input) {
        String line;
        do {
            line = input.readLine();
        } while (!TextUtils.isEmpty(line));
    }

    private static char peekCharAtPosition(ParsableByteArray input, int position) {
        return (char) input.getData()[position];
    }

    private static String parsePropertyValue(ParsableByteArray input, StringBuilder stringBuilder) {
        StringBuilder expressionBuilder = new StringBuilder();
        boolean expressionEndFound = false;
        while (!expressionEndFound) {
            int position = input.getPosition();
            String token = parseNextToken(input, stringBuilder);
            if (token == null) {
                return null;
            }
            if (RULE_END.equals(token) || ";".equals(token)) {
                input.setPosition(position);
                expressionEndFound = true;
            } else {
                expressionBuilder.append(token);
            }
        }
        return expressionBuilder.toString();
    }

    private static boolean maybeSkipComment(ParsableByteArray input) {
        int position = input.getPosition();
        int limit = input.limit();
        byte[] data = input.getData();
        if (position + 2 > limit) {
            return false;
        }
        int position2 = position + 1;
        if (data[position] != 47) {
            return false;
        }
        int position3 = position2 + 1;
        if (data[position2] == 42) {
            while (position3 + 1 < limit) {
                int position4 = position3 + 1;
                char skippedChar = (char) data[position3];
                if (skippedChar == '*' && ((char) data[position4]) == '/') {
                    int position5 = position4 + 1;
                    limit = position5;
                    position3 = position5;
                } else {
                    position3 = position4;
                }
            }
            input.skipBytes(limit - input.getPosition());
            return true;
        }
        return false;
    }

    private static String parseIdentifier(ParsableByteArray input, StringBuilder stringBuilder) {
        stringBuilder.setLength(0);
        int position = input.getPosition();
        int limit = input.limit();
        boolean identifierEndFound = false;
        while (position < limit && !identifierEndFound) {
            char c = (char) input.getData()[position];
            if ((c >= 'A' && c <= 'Z') || ((c >= 'a' && c <= 'z') || ((c >= '0' && c <= '9') || c == '#' || c == '-' || c == '.' || c == '_'))) {
                position++;
                stringBuilder.append(c);
            } else {
                identifierEndFound = true;
            }
        }
        input.skipBytes(position - input.getPosition());
        return stringBuilder.toString();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:18:0x0061  */
    private static void parseFontSize(String fontSize, WebvttCssStyle style) {
        Matcher matcher = FONT_SIZE_PATTERN.matcher(Ascii.toLowerCase(fontSize));
        if (!matcher.matches()) {
            Log.w(TAG, "Invalid font-size: '" + fontSize + "'.");
            return;
        }
        String unit = (String) Assertions.checkNotNull(matcher.group(2));
        switch (unit) {
            case "px":
                style.setFontSizeUnit(1);
                break;
            case "em":
                style.setFontSizeUnit(2);
                break;
            case "%":
                style.setFontSizeUnit(3);
                break;
            default:
                throw new IllegalStateException();
        }
        style.setFontSize(Float.parseFloat((String) Assertions.checkNotNull(matcher.group(1))));
    }

    private void applySelectorToStyle(WebvttCssStyle style, String selector) {
        if ("".equals(selector)) {
            return;
        }
        int voiceStartIndex = selector.indexOf(91);
        if (voiceStartIndex != -1) {
            Matcher matcher = VOICE_NAME_PATTERN.matcher(selector.substring(voiceStartIndex));
            if (matcher.matches()) {
                style.setTargetVoice((String) Assertions.checkNotNull(matcher.group(1)));
            }
            selector = selector.substring(0, voiceStartIndex);
        }
        String[] classDivision = Util.split(selector, "\\.");
        String tagAndIdDivision = classDivision[0];
        int idPrefixIndex = tagAndIdDivision.indexOf(35);
        if (idPrefixIndex != -1) {
            style.setTargetTagName(tagAndIdDivision.substring(0, idPrefixIndex));
            style.setTargetId(tagAndIdDivision.substring(idPrefixIndex + 1));
        } else {
            style.setTargetTagName(tagAndIdDivision);
        }
        if (classDivision.length > 1) {
            style.setTargetClasses((String[]) Util.nullSafeArrayCopyOfRange(classDivision, 1, classDivision.length));
        }
    }
}
