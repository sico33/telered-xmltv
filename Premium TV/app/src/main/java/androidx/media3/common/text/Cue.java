package androidx.media3.common.text;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import androidx.core.view.ViewCompat;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.base.Objects;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import org.checkerframework.dataflow.qual.Pure;

/* JADX INFO: loaded from: classes.dex */
public final class Cue {
    public static final int ANCHOR_TYPE_END = 2;
    public static final int ANCHOR_TYPE_MIDDLE = 1;
    public static final int ANCHOR_TYPE_START = 0;
    public static final float DIMEN_UNSET = -3.4028235E38f;
    public static final int LINE_TYPE_FRACTION = 0;
    public static final int LINE_TYPE_NUMBER = 1;
    public static final int TEXT_SIZE_TYPE_ABSOLUTE = 2;
    public static final int TEXT_SIZE_TYPE_FRACTIONAL = 0;
    public static final int TEXT_SIZE_TYPE_FRACTIONAL_IGNORE_PADDING = 1;
    public static final int TYPE_UNSET = Integer.MIN_VALUE;
    public static final int VERTICAL_TYPE_LR = 2;
    public static final int VERTICAL_TYPE_RL = 1;
    public final Bitmap bitmap;
    public final float bitmapHeight;
    public final float line;
    public final int lineAnchor;
    public final int lineType;
    public final Layout.Alignment multiRowAlignment;
    public final float position;
    public final int positionAnchor;
    public final float shearDegrees;
    public final float size;
    public final CharSequence text;
    public final Layout.Alignment textAlignment;
    public final float textSize;
    public final int textSizeType;
    public final int verticalType;
    public final int windowColor;
    public final boolean windowColorSet;

    @Deprecated
    public static final Cue EMPTY = new Builder().setText("").build();
    private static final String FIELD_TEXT = Util.intToStringMaxRadix(0);
    private static final String FIELD_CUSTOM_SPANS = Util.intToStringMaxRadix(17);
    private static final String FIELD_TEXT_ALIGNMENT = Util.intToStringMaxRadix(1);
    private static final String FIELD_MULTI_ROW_ALIGNMENT = Util.intToStringMaxRadix(2);
    private static final String FIELD_BITMAP_PARCELABLE = Util.intToStringMaxRadix(3);
    private static final String FIELD_BITMAP_BYTES = Util.intToStringMaxRadix(18);
    private static final String FIELD_LINE = Util.intToStringMaxRadix(4);
    private static final String FIELD_LINE_TYPE = Util.intToStringMaxRadix(5);
    private static final String FIELD_LINE_ANCHOR = Util.intToStringMaxRadix(6);
    private static final String FIELD_POSITION = Util.intToStringMaxRadix(7);
    private static final String FIELD_POSITION_ANCHOR = Util.intToStringMaxRadix(8);
    private static final String FIELD_TEXT_SIZE_TYPE = Util.intToStringMaxRadix(9);
    private static final String FIELD_TEXT_SIZE = Util.intToStringMaxRadix(10);
    private static final String FIELD_SIZE = Util.intToStringMaxRadix(11);
    private static final String FIELD_BITMAP_HEIGHT = Util.intToStringMaxRadix(12);
    private static final String FIELD_WINDOW_COLOR = Util.intToStringMaxRadix(13);
    private static final String FIELD_WINDOW_COLOR_SET = Util.intToStringMaxRadix(14);
    private static final String FIELD_VERTICAL_TYPE = Util.intToStringMaxRadix(15);
    private static final String FIELD_SHEAR_DEGREES = Util.intToStringMaxRadix(16);

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface AnchorType {
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface LineType {
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextSizeType {
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface VerticalType {
    }

    private Cue(CharSequence text, Layout.Alignment textAlignment, Layout.Alignment multiRowAlignment, Bitmap bitmap, float line, int lineType, int lineAnchor, float position, int positionAnchor, int textSizeType, float textSize, float size, float bitmapHeight, boolean windowColorSet, int windowColor, int verticalType, float shearDegrees) {
        if (text == null) {
            Assertions.checkNotNull(bitmap);
        } else {
            Assertions.checkArgument(bitmap == null);
        }
        if (text instanceof Spanned) {
            this.text = SpannedString.valueOf(text);
        } else if (text != null) {
            this.text = text.toString();
        } else {
            this.text = null;
        }
        this.textAlignment = textAlignment;
        this.multiRowAlignment = multiRowAlignment;
        this.bitmap = bitmap;
        this.line = line;
        this.lineType = lineType;
        this.lineAnchor = lineAnchor;
        this.position = position;
        this.positionAnchor = positionAnchor;
        this.size = size;
        this.bitmapHeight = bitmapHeight;
        this.windowColorSet = windowColorSet;
        this.windowColor = windowColor;
        this.textSizeType = textSizeType;
        this.textSize = textSize;
        this.verticalType = verticalType;
        this.shearDegrees = shearDegrees;
    }

    public Builder buildUpon() {
        return new Builder();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Cue that = (Cue) obj;
        if (TextUtils.equals(this.text, that.text) && this.textAlignment == that.textAlignment && this.multiRowAlignment == that.multiRowAlignment && (this.bitmap != null ? !(that.bitmap == null || !this.bitmap.sameAs(that.bitmap)) : that.bitmap == null) && this.line == that.line && this.lineType == that.lineType && this.lineAnchor == that.lineAnchor && this.position == that.position && this.positionAnchor == that.positionAnchor && this.size == that.size && this.bitmapHeight == that.bitmapHeight && this.windowColorSet == that.windowColorSet && this.windowColor == that.windowColor && this.textSizeType == that.textSizeType && this.textSize == that.textSize && this.verticalType == that.verticalType && this.shearDegrees == that.shearDegrees) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hashCode(this.text, this.textAlignment, this.multiRowAlignment, this.bitmap, Float.valueOf(this.line), Integer.valueOf(this.lineType), Integer.valueOf(this.lineAnchor), Float.valueOf(this.position), Integer.valueOf(this.positionAnchor), Float.valueOf(this.size), Float.valueOf(this.bitmapHeight), Boolean.valueOf(this.windowColorSet), Integer.valueOf(this.windowColor), Integer.valueOf(this.textSizeType), Float.valueOf(this.textSize), Integer.valueOf(this.verticalType), Float.valueOf(this.shearDegrees));
    }

    public static final class Builder {
        private Bitmap bitmap;
        private float bitmapHeight;
        private float line;
        private int lineAnchor;
        private int lineType;
        private Layout.Alignment multiRowAlignment;
        private float position;
        private int positionAnchor;
        private float shearDegrees;
        private float size;
        private CharSequence text;
        private Layout.Alignment textAlignment;
        private float textSize;
        private int textSizeType;
        private int verticalType;
        private int windowColor;
        private boolean windowColorSet;

        public Builder() {
            this.text = null;
            this.bitmap = null;
            this.textAlignment = null;
            this.multiRowAlignment = null;
            this.line = -3.4028235E38f;
            this.lineType = Integer.MIN_VALUE;
            this.lineAnchor = Integer.MIN_VALUE;
            this.position = -3.4028235E38f;
            this.positionAnchor = Integer.MIN_VALUE;
            this.textSizeType = Integer.MIN_VALUE;
            this.textSize = -3.4028235E38f;
            this.size = -3.4028235E38f;
            this.bitmapHeight = -3.4028235E38f;
            this.windowColorSet = false;
            this.windowColor = ViewCompat.MEASURED_STATE_MASK;
            this.verticalType = Integer.MIN_VALUE;
        }

        private Builder(Cue cue) {
            this.text = cue.text;
            this.bitmap = cue.bitmap;
            this.textAlignment = cue.textAlignment;
            this.multiRowAlignment = cue.multiRowAlignment;
            this.line = cue.line;
            this.lineType = cue.lineType;
            this.lineAnchor = cue.lineAnchor;
            this.position = cue.position;
            this.positionAnchor = cue.positionAnchor;
            this.textSizeType = cue.textSizeType;
            this.textSize = cue.textSize;
            this.size = cue.size;
            this.bitmapHeight = cue.bitmapHeight;
            this.windowColorSet = cue.windowColorSet;
            this.windowColor = cue.windowColor;
            this.verticalType = cue.verticalType;
            this.shearDegrees = cue.shearDegrees;
        }

        public Builder setText(CharSequence text) {
            this.text = text;
            return this;
        }

        @Pure
        public CharSequence getText() {
            return this.text;
        }

        public Builder setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
            return this;
        }

        @Pure
        public Bitmap getBitmap() {
            return this.bitmap;
        }

        public Builder setTextAlignment(Layout.Alignment textAlignment) {
            this.textAlignment = textAlignment;
            return this;
        }

        @Pure
        public Layout.Alignment getTextAlignment() {
            return this.textAlignment;
        }

        public Builder setMultiRowAlignment(Layout.Alignment multiRowAlignment) {
            this.multiRowAlignment = multiRowAlignment;
            return this;
        }

        public Builder setLine(float line, int lineType) {
            this.line = line;
            this.lineType = lineType;
            return this;
        }

        @Pure
        public float getLine() {
            return this.line;
        }

        @Pure
        public int getLineType() {
            return this.lineType;
        }

        public Builder setLineAnchor(int lineAnchor) {
            this.lineAnchor = lineAnchor;
            return this;
        }

        @Pure
        public int getLineAnchor() {
            return this.lineAnchor;
        }

        public Builder setPosition(float position) {
            this.position = position;
            return this;
        }

        @Pure
        public float getPosition() {
            return this.position;
        }

        public Builder setPositionAnchor(int positionAnchor) {
            this.positionAnchor = positionAnchor;
            return this;
        }

        @Pure
        public int getPositionAnchor() {
            return this.positionAnchor;
        }

        public Builder setTextSize(float textSize, int textSizeType) {
            this.textSize = textSize;
            this.textSizeType = textSizeType;
            return this;
        }

        @Pure
        public int getTextSizeType() {
            return this.textSizeType;
        }

        @Pure
        public float getTextSize() {
            return this.textSize;
        }

        public Builder setSize(float size) {
            this.size = size;
            return this;
        }

        @Pure
        public float getSize() {
            return this.size;
        }

        public Builder setBitmapHeight(float bitmapHeight) {
            this.bitmapHeight = bitmapHeight;
            return this;
        }

        @Pure
        public float getBitmapHeight() {
            return this.bitmapHeight;
        }

        public Builder setWindowColor(int windowColor) {
            this.windowColor = windowColor;
            this.windowColorSet = true;
            return this;
        }

        public Builder clearWindowColor() {
            this.windowColorSet = false;
            return this;
        }

        public boolean isWindowColorSet() {
            return this.windowColorSet;
        }

        @Pure
        public int getWindowColor() {
            return this.windowColor;
        }

        public Builder setVerticalType(int verticalType) {
            this.verticalType = verticalType;
            return this;
        }

        public Builder setShearDegrees(float shearDegrees) {
            this.shearDegrees = shearDegrees;
            return this;
        }

        @Pure
        public int getVerticalType() {
            return this.verticalType;
        }

        public Cue build() {
            return new Cue(this.text, this.textAlignment, this.multiRowAlignment, this.bitmap, this.line, this.lineType, this.lineAnchor, this.position, this.positionAnchor, this.textSizeType, this.textSize, this.size, this.bitmapHeight, this.windowColorSet, this.windowColor, this.verticalType, this.shearDegrees);
        }
    }

    public Bundle toSerializableBundle() {
        Bundle bundle = toBundleWithoutBitmap();
        if (this.bitmap != null) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Assertions.checkState(this.bitmap.compress(Bitmap.CompressFormat.PNG, 0, output));
            bundle.putByteArray(FIELD_BITMAP_BYTES, output.toByteArray());
        }
        return bundle;
    }

    public Bundle toBinderBasedBundle() {
        Bundle bundle = toBundleWithoutBitmap();
        if (this.bitmap != null) {
            bundle.putParcelable(FIELD_BITMAP_PARCELABLE, this.bitmap);
        }
        return bundle;
    }

    @Deprecated
    public Bundle toBundle() {
        return toBinderBasedBundle();
    }

    private Bundle toBundleWithoutBitmap() {
        Bundle bundle = new Bundle();
        if (this.text != null) {
            bundle.putCharSequence(FIELD_TEXT, this.text);
            if (this.text instanceof Spanned) {
                ArrayList<Bundle> customSpanBundles = CustomSpanBundler.bundleCustomSpans((Spanned) this.text);
                if (!customSpanBundles.isEmpty()) {
                    bundle.putParcelableArrayList(FIELD_CUSTOM_SPANS, customSpanBundles);
                }
            }
        }
        bundle.putSerializable(FIELD_TEXT_ALIGNMENT, this.textAlignment);
        bundle.putSerializable(FIELD_MULTI_ROW_ALIGNMENT, this.multiRowAlignment);
        bundle.putFloat(FIELD_LINE, this.line);
        bundle.putInt(FIELD_LINE_TYPE, this.lineType);
        bundle.putInt(FIELD_LINE_ANCHOR, this.lineAnchor);
        bundle.putFloat(FIELD_POSITION, this.position);
        bundle.putInt(FIELD_POSITION_ANCHOR, this.positionAnchor);
        bundle.putInt(FIELD_TEXT_SIZE_TYPE, this.textSizeType);
        bundle.putFloat(FIELD_TEXT_SIZE, this.textSize);
        bundle.putFloat(FIELD_SIZE, this.size);
        bundle.putFloat(FIELD_BITMAP_HEIGHT, this.bitmapHeight);
        bundle.putBoolean(FIELD_WINDOW_COLOR_SET, this.windowColorSet);
        bundle.putInt(FIELD_WINDOW_COLOR, this.windowColor);
        bundle.putInt(FIELD_VERTICAL_TYPE, this.verticalType);
        bundle.putFloat(FIELD_SHEAR_DEGREES, this.shearDegrees);
        return bundle;
    }

    public static Cue fromBundle(Bundle bundle) {
        Builder builder = new Builder();
        CharSequence text = bundle.getCharSequence(FIELD_TEXT);
        if (text != null) {
            builder.setText(text);
            ArrayList<Bundle> customSpanBundles = bundle.getParcelableArrayList(FIELD_CUSTOM_SPANS);
            if (customSpanBundles != null) {
                SpannableString textWithCustomSpans = SpannableString.valueOf(text);
                for (Bundle customSpanBundle : customSpanBundles) {
                    CustomSpanBundler.unbundleAndApplyCustomSpan(customSpanBundle, textWithCustomSpans);
                }
                builder.setText(textWithCustomSpans);
            }
        }
        Layout.Alignment textAlignment = (Layout.Alignment) bundle.getSerializable(FIELD_TEXT_ALIGNMENT);
        if (textAlignment != null) {
            builder.setTextAlignment(textAlignment);
        }
        Layout.Alignment multiRowAlignment = (Layout.Alignment) bundle.getSerializable(FIELD_MULTI_ROW_ALIGNMENT);
        if (multiRowAlignment != null) {
            builder.setMultiRowAlignment(multiRowAlignment);
        }
        Bitmap bitmap = (Bitmap) bundle.getParcelable(FIELD_BITMAP_PARCELABLE);
        if (bitmap != null) {
            builder.setBitmap(bitmap);
        } else {
            byte[] bitmapBytes = bundle.getByteArray(FIELD_BITMAP_BYTES);
            if (bitmapBytes != null) {
                builder.setBitmap(BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length));
            }
        }
        if (bundle.containsKey(FIELD_LINE) && bundle.containsKey(FIELD_LINE_TYPE)) {
            builder.setLine(bundle.getFloat(FIELD_LINE), bundle.getInt(FIELD_LINE_TYPE));
        }
        if (bundle.containsKey(FIELD_LINE_ANCHOR)) {
            builder.setLineAnchor(bundle.getInt(FIELD_LINE_ANCHOR));
        }
        if (bundle.containsKey(FIELD_POSITION)) {
            builder.setPosition(bundle.getFloat(FIELD_POSITION));
        }
        if (bundle.containsKey(FIELD_POSITION_ANCHOR)) {
            builder.setPositionAnchor(bundle.getInt(FIELD_POSITION_ANCHOR));
        }
        if (bundle.containsKey(FIELD_TEXT_SIZE) && bundle.containsKey(FIELD_TEXT_SIZE_TYPE)) {
            builder.setTextSize(bundle.getFloat(FIELD_TEXT_SIZE), bundle.getInt(FIELD_TEXT_SIZE_TYPE));
        }
        if (bundle.containsKey(FIELD_SIZE)) {
            builder.setSize(bundle.getFloat(FIELD_SIZE));
        }
        if (bundle.containsKey(FIELD_BITMAP_HEIGHT)) {
            builder.setBitmapHeight(bundle.getFloat(FIELD_BITMAP_HEIGHT));
        }
        if (bundle.containsKey(FIELD_WINDOW_COLOR)) {
            builder.setWindowColor(bundle.getInt(FIELD_WINDOW_COLOR));
        }
        if (!bundle.getBoolean(FIELD_WINDOW_COLOR_SET, false)) {
            builder.clearWindowColor();
        }
        if (bundle.containsKey(FIELD_VERTICAL_TYPE)) {
            builder.setVerticalType(bundle.getInt(FIELD_VERTICAL_TYPE));
        }
        if (bundle.containsKey(FIELD_SHEAR_DEGREES)) {
            builder.setShearDegrees(bundle.getFloat(FIELD_SHEAR_DEGREES));
        }
        return builder.build();
    }
}
