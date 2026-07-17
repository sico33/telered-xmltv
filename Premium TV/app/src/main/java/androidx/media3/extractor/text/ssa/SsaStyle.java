package androidx.media3.extractor.text.ssa;

import android.graphics.Color;
import android.graphics.PointF;
import android.text.TextUtils;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import com.google.common.base.Ascii;
import com.google.common.primitives.Ints;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class SsaStyle {
    public static final int SSA_ALIGNMENT_BOTTOM_CENTER = 2;
    public static final int SSA_ALIGNMENT_BOTTOM_LEFT = 1;
    public static final int SSA_ALIGNMENT_BOTTOM_RIGHT = 3;
    public static final int SSA_ALIGNMENT_MIDDLE_CENTER = 5;
    public static final int SSA_ALIGNMENT_MIDDLE_LEFT = 4;
    public static final int SSA_ALIGNMENT_MIDDLE_RIGHT = 6;
    public static final int SSA_ALIGNMENT_TOP_CENTER = 8;
    public static final int SSA_ALIGNMENT_TOP_LEFT = 7;
    public static final int SSA_ALIGNMENT_TOP_RIGHT = 9;
    public static final int SSA_ALIGNMENT_UNKNOWN = -1;
    public static final int SSA_BORDER_STYLE_BOX = 3;
    public static final int SSA_BORDER_STYLE_OUTLINE = 1;
    public static final int SSA_BORDER_STYLE_UNKNOWN = -1;
    private static final String TAG = "SsaStyle";
    public final int alignment;
    public final boolean bold;
    public final int borderStyle;
    public final float fontSize;
    public final boolean italic;
    public final String name;
    public final Integer outlineColor;
    public final Integer primaryColor;
    public final boolean strikeout;
    public final boolean underline;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface SsaAlignment {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface SsaBorderStyle {
    }

    private SsaStyle(String name, int alignment, Integer primaryColor, Integer outlineColor, float fontSize, boolean bold, boolean italic, boolean underline, boolean strikeout, int borderStyle) {
        this.name = name;
        this.alignment = alignment;
        this.primaryColor = primaryColor;
        this.outlineColor = outlineColor;
        this.fontSize = fontSize;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikeout = strikeout;
        this.borderStyle = borderStyle;
    }

    public static SsaStyle fromStyleLine(String styleLine, Format format) {
        int alignment;
        Integer color;
        Integer color2;
        float fontSize;
        int borderStyle;
        Assertions.checkArgument(styleLine.startsWith("Style:"));
        String[] styleValues = TextUtils.split(styleLine.substring("Style:".length()), ",");
        if (styleValues.length != format.length) {
            Log.w(TAG, Util.formatInvariant("Skipping malformed 'Style:' line (expected %s values, found %s): '%s'", Integer.valueOf(format.length), Integer.valueOf(styleValues.length), styleLine));
            return null;
        }
        try {
            String strTrim = styleValues[format.nameIndex].trim();
            if (format.alignmentIndex != -1) {
                alignment = parseAlignment(styleValues[format.alignmentIndex].trim());
            } else {
                alignment = -1;
            }
            if (format.primaryColorIndex != -1) {
                color = parseColor(styleValues[format.primaryColorIndex].trim());
            } else {
                color = null;
            }
            if (format.outlineColorIndex != -1) {
                color2 = parseColor(styleValues[format.outlineColorIndex].trim());
            } else {
                color2 = null;
            }
            if (format.fontSizeIndex != -1) {
                fontSize = parseFontSize(styleValues[format.fontSizeIndex].trim());
            } else {
                fontSize = -3.4028235E38f;
            }
            boolean z = format.boldIndex != -1 && parseBooleanValue(styleValues[format.boldIndex].trim());
            boolean z2 = format.italicIndex != -1 && parseBooleanValue(styleValues[format.italicIndex].trim());
            boolean z3 = format.underlineIndex != -1 && parseBooleanValue(styleValues[format.underlineIndex].trim());
            try {
                boolean z4 = format.strikeoutIndex != -1 && parseBooleanValue(styleValues[format.strikeoutIndex].trim());
                if (format.borderStyleIndex != -1) {
                    borderStyle = parseBorderStyle(styleValues[format.borderStyleIndex].trim());
                } else {
                    borderStyle = -1;
                }
                return new SsaStyle(strTrim, alignment, color, color2, fontSize, z, z2, z3, z4, borderStyle);
            } catch (RuntimeException e) {
                e = e;
                Log.w(TAG, "Skipping malformed 'Style:' line: '" + styleLine + "'", e);
                return 0;
            }
        } catch (RuntimeException e2) {
            e = e2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int parseAlignment(String alignmentStr) {
        try {
            int alignment = Integer.parseInt(alignmentStr.trim());
            if (isValidAlignment(alignment)) {
                return alignment;
            }
        } catch (NumberFormatException e) {
        }
        Log.w(TAG, "Ignoring unknown alignment: " + alignmentStr);
        return -1;
    }

    private static boolean isValidAlignment(int alignment) {
        switch (alignment) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return true;
            default:
                return false;
        }
    }

    private static int parseBorderStyle(String borderStyleStr) {
        try {
            int borderStyle = Integer.parseInt(borderStyleStr.trim());
            if (isValidBorderStyle(borderStyle)) {
                return borderStyle;
            }
        } catch (NumberFormatException e) {
        }
        Log.w(TAG, "Ignoring unknown BorderStyle: " + borderStyleStr);
        return -1;
    }

    private static boolean isValidBorderStyle(int alignment) {
        switch (alignment) {
            case 1:
            case 3:
                return true;
            case 2:
            default:
                return false;
        }
    }

    public static Integer parseColor(String ssaColorExpression) {
        long abgr;
        try {
            if (ssaColorExpression.startsWith("&H")) {
                abgr = Long.parseLong(ssaColorExpression.substring(2), 16);
            } else {
                abgr = Long.parseLong(ssaColorExpression);
            }
            Assertions.checkArgument(abgr <= 4294967295L);
            int a = Ints.checkedCast(((abgr >> 24) & 255) ^ 255);
            int b = Ints.checkedCast((abgr >> 16) & 255);
            int g = Ints.checkedCast((abgr >> 8) & 255);
            int r = Ints.checkedCast(255 & abgr);
            return Integer.valueOf(Color.argb(a, r, g, b));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to parse color expression: '" + ssaColorExpression + "'", e);
            return null;
        }
    }

    private static float parseFontSize(String fontSize) {
        try {
            return Float.parseFloat(fontSize);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse font size: '" + fontSize + "'", e);
            return -3.4028235E38f;
        }
    }

    private static boolean parseBooleanValue(String booleanValue) {
        try {
            int value = Integer.parseInt(booleanValue);
            if (value != 1 && value != -1) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse boolean value: '" + booleanValue + "'", e);
            return false;
        }
    }

    static final class Format {
        public final int alignmentIndex;
        public final int boldIndex;
        public final int borderStyleIndex;
        public final int fontSizeIndex;
        public final int italicIndex;
        public final int length;
        public final int nameIndex;
        public final int outlineColorIndex;
        public final int primaryColorIndex;
        public final int strikeoutIndex;
        public final int underlineIndex;

        private Format(int nameIndex, int alignmentIndex, int primaryColorIndex, int outlineColorIndex, int fontSizeIndex, int boldIndex, int italicIndex, int underlineIndex, int strikeoutIndex, int borderStyleIndex, int length) {
            this.nameIndex = nameIndex;
            this.alignmentIndex = alignmentIndex;
            this.primaryColorIndex = primaryColorIndex;
            this.outlineColorIndex = outlineColorIndex;
            this.fontSizeIndex = fontSizeIndex;
            this.boldIndex = boldIndex;
            this.italicIndex = italicIndex;
            this.underlineIndex = underlineIndex;
            this.strikeoutIndex = strikeoutIndex;
            this.borderStyleIndex = borderStyleIndex;
            this.length = length;
        }

        public static Format fromFormatLine(String styleFormatLine) {
            String[] keys = TextUtils.split(styleFormatLine.substring("Format:".length()), ",");
            int i = 0;
            int nameIndex = -1;
            int alignmentIndex = -1;
            int primaryColorIndex = -1;
            int outlineColorIndex = -1;
            int fontSizeIndex = -1;
            int boldIndex = -1;
            int italicIndex = -1;
            int underlineIndex = -1;
            int strikeoutIndex = -1;
            int borderStyleIndex = -1;
            while (true) {
                int nameIndex2 = keys.length;
                if (i < nameIndex2) {
                    switch (Ascii.toLowerCase(keys[i].trim())) {
                        case "name":
                            int alignmentIndex2 = i;
                            nameIndex = alignmentIndex2;
                            break;
                        case "alignment":
                            int primaryColorIndex2 = i;
                            alignmentIndex = primaryColorIndex2;
                            break;
                        case "primarycolour":
                            int outlineColorIndex2 = i;
                            primaryColorIndex = outlineColorIndex2;
                            break;
                        case "outlinecolour":
                            int fontSizeIndex2 = i;
                            outlineColorIndex = fontSizeIndex2;
                            break;
                        case "fontsize":
                            int boldIndex2 = i;
                            fontSizeIndex = boldIndex2;
                            break;
                        case "bold":
                            int italicIndex2 = i;
                            boldIndex = italicIndex2;
                            break;
                        case "italic":
                            int underlineIndex2 = i;
                            italicIndex = underlineIndex2;
                            break;
                        case "underline":
                            int strikeoutIndex2 = i;
                            underlineIndex = strikeoutIndex2;
                            break;
                        case "strikeout":
                            int borderStyleIndex2 = i;
                            strikeoutIndex = borderStyleIndex2;
                            break;
                        case "borderstyle":
                            borderStyleIndex = i;
                            break;
                    }
                    i++;
                } else {
                    if (nameIndex != -1) {
                        return new Format(nameIndex, alignmentIndex, primaryColorIndex, outlineColorIndex, fontSizeIndex, boldIndex, italicIndex, underlineIndex, strikeoutIndex, borderStyleIndex, keys.length);
                    }
                    return null;
                }
            }
        }
    }

    static final class Overrides {
        private static final String TAG = "SsaStyle.Overrides";
        public final int alignment;
        public final PointF position;
        private static final Pattern BRACES_PATTERN = Pattern.compile("\\{([^}]*)\\}");
        private static final String PADDED_DECIMAL_PATTERN = "\\s*\\d+(?:\\.\\d+)?\\s*";
        private static final Pattern POSITION_PATTERN = Pattern.compile(Util.formatInvariant("\\\\pos\\((%1$s),(%1$s)\\)", PADDED_DECIMAL_PATTERN));
        private static final Pattern MOVE_PATTERN = Pattern.compile(Util.formatInvariant("\\\\move\\(%1$s,%1$s,(%1$s),(%1$s)(?:,%1$s,%1$s)?\\)", PADDED_DECIMAL_PATTERN));
        private static final Pattern ALIGNMENT_OVERRIDE_PATTERN = Pattern.compile("\\\\an(\\d+)");

        private Overrides(int alignment, PointF position) {
            this.alignment = alignment;
            this.position = position;
        }

        public static Overrides parseFromDialogue(String text) {
            int alignment = -1;
            PointF position = null;
            Matcher matcher = BRACES_PATTERN.matcher(text);
            while (matcher.find()) {
                String braceContents = (String) Assertions.checkNotNull(matcher.group(1));
                try {
                    PointF parsedPosition = parsePosition(braceContents);
                    if (parsedPosition != null) {
                        position = parsedPosition;
                    }
                } catch (RuntimeException e) {
                }
                try {
                    int parsedAlignment = parseAlignmentOverride(braceContents);
                    if (parsedAlignment != -1) {
                        alignment = parsedAlignment;
                    }
                } catch (RuntimeException e2) {
                }
            }
            return new Overrides(alignment, position);
        }

        public static String stripStyleOverrides(String dialogueLine) {
            return BRACES_PATTERN.matcher(dialogueLine).replaceAll("");
        }

        private static PointF parsePosition(String styleOverride) {
            String x;
            String y;
            Matcher positionMatcher = POSITION_PATTERN.matcher(styleOverride);
            Matcher moveMatcher = MOVE_PATTERN.matcher(styleOverride);
            boolean hasPosition = positionMatcher.find();
            boolean hasMove = moveMatcher.find();
            if (hasPosition) {
                if (hasMove) {
                    Log.i(TAG, "Override has both \\pos(x,y) and \\move(x1,y1,x2,y2); using \\pos values. override='" + styleOverride + "'");
                }
                x = positionMatcher.group(1);
                y = positionMatcher.group(2);
            } else if (hasMove) {
                x = moveMatcher.group(1);
                y = moveMatcher.group(2);
            } else {
                return null;
            }
            return new PointF(Float.parseFloat(((String) Assertions.checkNotNull(x)).trim()), Float.parseFloat(((String) Assertions.checkNotNull(y)).trim()));
        }

        private static int parseAlignmentOverride(String braceContents) {
            Matcher matcher = ALIGNMENT_OVERRIDE_PATTERN.matcher(braceContents);
            if (matcher.find()) {
                return SsaStyle.parseAlignment((String) Assertions.checkNotNull(matcher.group(1)));
            }
            return -1;
        }
    }
}
