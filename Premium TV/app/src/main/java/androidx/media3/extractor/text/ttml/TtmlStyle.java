package androidx.media3.extractor.text.ttml;

import android.text.Layout;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
final class TtmlStyle {
    public static final int FONT_SIZE_UNIT_EM = 2;
    public static final int FONT_SIZE_UNIT_PERCENT = 3;
    public static final int FONT_SIZE_UNIT_PIXEL = 1;
    private static final int OFF = 0;
    private static final int ON = 1;
    public static final int RUBY_TYPE_BASE = 2;
    public static final int RUBY_TYPE_CONTAINER = 1;
    public static final int RUBY_TYPE_DELIMITER = 4;
    public static final int RUBY_TYPE_TEXT = 3;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_BOLD_ITALIC = 3;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_NORMAL = 0;
    public static final int UNSPECIFIED = -1;
    public static final float UNSPECIFIED_SHEAR = Float.MAX_VALUE;
    private int backgroundColor;
    private int fontColor;
    private String fontFamily;
    private float fontSize;
    private boolean hasBackgroundColor;
    private boolean hasFontColor;
    private String id;
    private Layout.Alignment multiRowAlign;
    private Layout.Alignment textAlign;
    private TextEmphasis textEmphasis;
    private int linethrough = -1;
    private int underline = -1;
    private int bold = -1;
    private int italic = -1;
    private int fontSizeUnit = -1;
    private int rubyType = -1;
    private int rubyPosition = -1;
    private int textCombine = -1;
    private float shearPercentage = Float.MAX_VALUE;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface FontSizeUnit {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface RubyType {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface StyleFlags {
    }

    public int getStyle() {
        if (this.bold == -1 && this.italic == -1) {
            return -1;
        }
        return (this.bold == 1 ? 1 : 0) | (this.italic == 1 ? 2 : 0);
    }

    public boolean isLinethrough() {
        return this.linethrough == 1;
    }

    public TtmlStyle setLinethrough(boolean z) {
        this.linethrough = z ? 1 : 0;
        return this;
    }

    public boolean isUnderline() {
        return this.underline == 1;
    }

    public TtmlStyle setUnderline(boolean z) {
        this.underline = z ? 1 : 0;
        return this;
    }

    public TtmlStyle setBold(boolean z) {
        this.bold = z ? 1 : 0;
        return this;
    }

    public TtmlStyle setItalic(boolean z) {
        this.italic = z ? 1 : 0;
        return this;
    }

    public String getFontFamily() {
        return this.fontFamily;
    }

    public TtmlStyle setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }

    public int getFontColor() {
        if (!this.hasFontColor) {
            throw new IllegalStateException("Font color has not been defined.");
        }
        return this.fontColor;
    }

    public TtmlStyle setFontColor(int fontColor) {
        this.fontColor = fontColor;
        this.hasFontColor = true;
        return this;
    }

    public boolean hasFontColor() {
        return this.hasFontColor;
    }

    public int getBackgroundColor() {
        if (!this.hasBackgroundColor) {
            throw new IllegalStateException("Background color has not been defined.");
        }
        return this.backgroundColor;
    }

    public TtmlStyle setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        this.hasBackgroundColor = true;
        return this;
    }

    public boolean hasBackgroundColor() {
        return this.hasBackgroundColor;
    }

    public TtmlStyle setShearPercentage(float shearPercentage) {
        this.shearPercentage = shearPercentage;
        return this;
    }

    public float getShearPercentage() {
        return this.shearPercentage;
    }

    public TtmlStyle chain(TtmlStyle ancestor) {
        return inherit(ancestor, true);
    }

    public TtmlStyle inherit(TtmlStyle ancestor) {
        return inherit(ancestor, false);
    }

    private TtmlStyle inherit(TtmlStyle ancestor, boolean chaining) {
        if (ancestor != null) {
            if (!this.hasFontColor && ancestor.hasFontColor) {
                setFontColor(ancestor.fontColor);
            }
            if (this.bold == -1) {
                this.bold = ancestor.bold;
            }
            if (this.italic == -1) {
                this.italic = ancestor.italic;
            }
            if (this.fontFamily == null && ancestor.fontFamily != null) {
                this.fontFamily = ancestor.fontFamily;
            }
            if (this.linethrough == -1) {
                this.linethrough = ancestor.linethrough;
            }
            if (this.underline == -1) {
                this.underline = ancestor.underline;
            }
            if (this.rubyPosition == -1) {
                this.rubyPosition = ancestor.rubyPosition;
            }
            if (this.textAlign == null && ancestor.textAlign != null) {
                this.textAlign = ancestor.textAlign;
            }
            if (this.multiRowAlign == null && ancestor.multiRowAlign != null) {
                this.multiRowAlign = ancestor.multiRowAlign;
            }
            if (this.textCombine == -1) {
                this.textCombine = ancestor.textCombine;
            }
            if (this.fontSizeUnit == -1) {
                this.fontSizeUnit = ancestor.fontSizeUnit;
                this.fontSize = ancestor.fontSize;
            }
            if (this.textEmphasis == null) {
                this.textEmphasis = ancestor.textEmphasis;
            }
            if (this.shearPercentage == Float.MAX_VALUE) {
                this.shearPercentage = ancestor.shearPercentage;
            }
            if (chaining && !this.hasBackgroundColor && ancestor.hasBackgroundColor) {
                setBackgroundColor(ancestor.backgroundColor);
            }
            if (chaining && this.rubyType == -1 && ancestor.rubyType != -1) {
                this.rubyType = ancestor.rubyType;
            }
        }
        return this;
    }

    public TtmlStyle setId(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return this.id;
    }

    public TtmlStyle setRubyType(int rubyType) {
        this.rubyType = rubyType;
        return this;
    }

    public int getRubyType() {
        return this.rubyType;
    }

    public TtmlStyle setRubyPosition(int position) {
        this.rubyPosition = position;
        return this;
    }

    public int getRubyPosition() {
        return this.rubyPosition;
    }

    public Layout.Alignment getTextAlign() {
        return this.textAlign;
    }

    public TtmlStyle setTextAlign(Layout.Alignment textAlign) {
        this.textAlign = textAlign;
        return this;
    }

    public Layout.Alignment getMultiRowAlign() {
        return this.multiRowAlign;
    }

    public TtmlStyle setMultiRowAlign(Layout.Alignment multiRowAlign) {
        this.multiRowAlign = multiRowAlign;
        return this;
    }

    public boolean getTextCombine() {
        return this.textCombine == 1;
    }

    public TtmlStyle setTextCombine(boolean z) {
        this.textCombine = z ? 1 : 0;
        return this;
    }

    public TextEmphasis getTextEmphasis() {
        return this.textEmphasis;
    }

    public TtmlStyle setTextEmphasis(TextEmphasis textEmphasis) {
        this.textEmphasis = textEmphasis;
        return this;
    }

    public TtmlStyle setFontSize(float fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public TtmlStyle setFontSizeUnit(int fontSizeUnit) {
        this.fontSizeUnit = fontSizeUnit;
        return this;
    }

    public int getFontSizeUnit() {
        return this.fontSizeUnit;
    }

    public float getFontSize() {
        return this.fontSize;
    }
}
