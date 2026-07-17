package androidx.media3.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import androidx.core.view.ViewCompat;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
final class SubtitlePainter {
    private static final float INNER_PADDING_RATIO = 0.125f;
    private static final String TAG = "SubtitlePainter";
    private int backgroundColor;
    private final Paint bitmapPaint;
    private Rect bitmapRect;
    private float bottomPaddingFraction;
    private Bitmap cueBitmap;
    private float cueBitmapHeight;
    private float cueLine;
    private int cueLineAnchor;
    private int cueLineType;
    private float cuePosition;
    private int cuePositionAnchor;
    private float cueSize;
    private CharSequence cueText;
    private Layout.Alignment cueTextAlignment;
    private float cueTextSizePx;
    private float defaultTextSizePx;
    private int edgeColor;
    private StaticLayout edgeLayout;
    private int edgeType;
    private int foregroundColor;
    private final float outlineWidth;
    private int parentBottom;
    private int parentLeft;
    private int parentRight;
    private int parentTop;
    private final float shadowOffset;
    private final float shadowRadius;
    private final float spacingAdd;
    private final float spacingMult;
    private StaticLayout textLayout;
    private int textLeft;
    private int textPaddingX;
    private final TextPaint textPaint;
    private int textTop;
    private int windowColor;
    private final Paint windowPaint;

    public SubtitlePainter(Context context) {
        int[] viewAttr = {android.R.attr.lineSpacingExtra, android.R.attr.lineSpacingMultiplier};
        TypedArray styledAttributes = context.obtainStyledAttributes(null, viewAttr, 0, 0);
        this.spacingAdd = styledAttributes.getDimensionPixelSize(0, 0);
        this.spacingMult = styledAttributes.getFloat(1, 1.0f);
        styledAttributes.recycle();
        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        int twoDpInPx = Math.round((displayMetrics.densityDpi * 2.0f) / 160.0f);
        this.outlineWidth = twoDpInPx;
        this.shadowRadius = twoDpInPx;
        this.shadowOffset = twoDpInPx;
        this.textPaint = new TextPaint();
        this.textPaint.setAntiAlias(true);
        this.textPaint.setSubpixelText(true);
        this.windowPaint = new Paint();
        this.windowPaint.setAntiAlias(true);
        this.windowPaint.setStyle(Paint.Style.FILL);
        this.bitmapPaint = new Paint();
        this.bitmapPaint.setAntiAlias(true);
        this.bitmapPaint.setFilterBitmap(true);
    }

    public void draw(Cue cue, CaptionStyleCompat style, float defaultTextSizePx, float cueTextSizePx, float bottomPaddingFraction, Canvas canvas, int cueBoxLeft, int cueBoxTop, int cueBoxRight, int cueBoxBottom) {
        boolean isTextCue = cue.bitmap == null;
        int windowColor = ViewCompat.MEASURED_STATE_MASK;
        if (isTextCue) {
            if (TextUtils.isEmpty(cue.text)) {
                return;
            } else {
                windowColor = cue.windowColorSet ? cue.windowColor : style.windowColor;
            }
        }
        if (areCharSequencesEqual(this.cueText, cue.text) && Util.areEqual(this.cueTextAlignment, cue.textAlignment) && this.cueBitmap == cue.bitmap && this.cueLine == cue.line && this.cueLineType == cue.lineType && Util.areEqual(Integer.valueOf(this.cueLineAnchor), Integer.valueOf(cue.lineAnchor)) && this.cuePosition == cue.position && Util.areEqual(Integer.valueOf(this.cuePositionAnchor), Integer.valueOf(cue.positionAnchor)) && this.cueSize == cue.size && this.cueBitmapHeight == cue.bitmapHeight && this.foregroundColor == style.foregroundColor && this.backgroundColor == style.backgroundColor && this.windowColor == windowColor && this.edgeType == style.edgeType && this.edgeColor == style.edgeColor && Util.areEqual(this.textPaint.getTypeface(), style.typeface) && this.defaultTextSizePx == defaultTextSizePx && this.cueTextSizePx == cueTextSizePx && this.bottomPaddingFraction == bottomPaddingFraction && this.parentLeft == cueBoxLeft && this.parentTop == cueBoxTop && this.parentRight == cueBoxRight && this.parentBottom == cueBoxBottom) {
            drawLayout(canvas, isTextCue);
            return;
        }
        this.cueText = cue.text;
        this.cueTextAlignment = cue.textAlignment;
        this.cueBitmap = cue.bitmap;
        this.cueLine = cue.line;
        this.cueLineType = cue.lineType;
        this.cueLineAnchor = cue.lineAnchor;
        this.cuePosition = cue.position;
        this.cuePositionAnchor = cue.positionAnchor;
        this.cueSize = cue.size;
        this.cueBitmapHeight = cue.bitmapHeight;
        this.foregroundColor = style.foregroundColor;
        this.backgroundColor = style.backgroundColor;
        this.windowColor = windowColor;
        this.edgeType = style.edgeType;
        this.edgeColor = style.edgeColor;
        this.textPaint.setTypeface(style.typeface);
        this.defaultTextSizePx = defaultTextSizePx;
        this.cueTextSizePx = cueTextSizePx;
        this.bottomPaddingFraction = bottomPaddingFraction;
        this.parentLeft = cueBoxLeft;
        this.parentTop = cueBoxTop;
        this.parentRight = cueBoxRight;
        this.parentBottom = cueBoxBottom;
        if (isTextCue) {
            Assertions.checkNotNull(this.cueText);
            setupTextLayout();
        } else {
            Assertions.checkNotNull(this.cueBitmap);
            setupBitmapLayout();
        }
        drawLayout(canvas, isTextCue);
    }

    @RequiresNonNull({"cueText"})
    private void setupTextLayout() {
        SpannableStringBuilder spannableStringBuilder;
        int availableWidth;
        int textLeft;
        int textRight;
        int textTop;
        int textTop2;
        int textLeft2;
        if (this.cueText instanceof SpannableStringBuilder) {
            spannableStringBuilder = (SpannableStringBuilder) this.cueText;
        } else {
            spannableStringBuilder = new SpannableStringBuilder(this.cueText);
        }
        SpannableStringBuilder cueText = spannableStringBuilder;
        int parentWidth = this.parentRight - this.parentLeft;
        int parentHeight = this.parentBottom - this.parentTop;
        this.textPaint.setTextSize(this.defaultTextSizePx);
        int textPaddingX = (int) ((this.defaultTextSizePx * INNER_PADDING_RATIO) + 0.5f);
        int availableWidth2 = parentWidth - (textPaddingX * 2);
        if (this.cueSize != -3.4028235E38f) {
            availableWidth = (int) (availableWidth2 * this.cueSize);
        } else {
            availableWidth = availableWidth2;
        }
        if (availableWidth <= 0) {
            Log.w(TAG, "Skipped drawing subtitle cue (insufficient space)");
            return;
        }
        if (this.cueTextSizePx > 0.0f) {
            cueText.setSpan(new AbsoluteSizeSpan((int) this.cueTextSizePx), 0, cueText.length(), 16711680);
        }
        SpannableStringBuilder cueTextEdge = new SpannableStringBuilder(cueText);
        if (this.edgeType == 1) {
            ForegroundColorSpan[] foregroundColorSpans = (ForegroundColorSpan[]) cueTextEdge.getSpans(0, cueTextEdge.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan foregroundColorSpan : foregroundColorSpans) {
                cueTextEdge.removeSpan(foregroundColorSpan);
            }
        }
        if (Color.alpha(this.backgroundColor) > 0) {
            if (this.edgeType == 0 || this.edgeType == 2) {
                cueText.setSpan(new BackgroundColorSpan(this.backgroundColor), 0, cueText.length(), 16711680);
            } else {
                cueTextEdge.setSpan(new BackgroundColorSpan(this.backgroundColor), 0, cueTextEdge.length(), 16711680);
            }
        }
        Layout.Alignment textAlignment = this.cueTextAlignment == null ? Layout.Alignment.ALIGN_CENTER : this.cueTextAlignment;
        int availableWidth3 = availableWidth;
        this.textLayout = new StaticLayout(cueText, this.textPaint, availableWidth, textAlignment, this.spacingMult, this.spacingAdd, true);
        int textHeight = this.textLayout.getHeight();
        int textWidth = 0;
        int lineCount = this.textLayout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            textWidth = Math.max((int) Math.ceil(this.textLayout.getLineWidth(i)), textWidth);
        }
        if (this.cueSize != -3.4028235E38f && textWidth < availableWidth3) {
            textWidth = availableWidth3;
        }
        int textWidth2 = textWidth + (textPaddingX * 2);
        if (this.cuePosition != -3.4028235E38f) {
            int anchorPosition = Math.round(parentWidth * this.cuePosition) + this.parentLeft;
            switch (this.cuePositionAnchor) {
                case 1:
                    textLeft2 = ((anchorPosition * 2) - textWidth2) / 2;
                    break;
                case 2:
                    textLeft2 = anchorPosition - textWidth2;
                    break;
                default:
                    textLeft2 = anchorPosition;
                    break;
            }
            textLeft = Math.max(textLeft2, this.parentLeft);
            textRight = Math.min(textLeft + textWidth2, this.parentRight);
        } else {
            int textRight2 = parentWidth - textWidth2;
            textLeft = this.parentLeft + (textRight2 / 2);
            textRight = textLeft + textWidth2;
        }
        int textWidth3 = textRight - textLeft;
        if (textWidth3 > 0) {
            if (this.cueLine == -3.4028235E38f) {
                textTop = (this.parentBottom - textHeight) - ((int) (parentHeight * this.bottomPaddingFraction));
            } else {
                if (this.cueLineType == 0) {
                    int anchorPosition2 = Math.round(parentHeight * this.cueLine) + this.parentTop;
                    if (this.cueLineAnchor == 2) {
                        textTop2 = anchorPosition2 - textHeight;
                    } else {
                        textTop2 = this.cueLineAnchor == 1 ? ((anchorPosition2 * 2) - textHeight) / 2 : anchorPosition2;
                    }
                } else {
                    int firstLineHeight = this.textLayout.getLineBottom(0) - this.textLayout.getLineTop(0);
                    float f = this.cueLine;
                    float f2 = this.cueLine;
                    textTop2 = f >= 0.0f ? Math.round(f2 * firstLineHeight) + this.parentTop : (Math.round((f2 + 1.0f) * firstLineHeight) + this.parentBottom) - textHeight;
                }
                if (textTop2 + textHeight > this.parentBottom) {
                    int textTop3 = this.parentBottom - textHeight;
                    textTop = textTop3;
                } else if (textTop2 >= this.parentTop) {
                    textTop = textTop2;
                } else {
                    int textTop4 = this.parentTop;
                    textTop = textTop4;
                }
            }
            this.textLayout = new StaticLayout(cueText, this.textPaint, textWidth3, textAlignment, this.spacingMult, this.spacingAdd, true);
            this.edgeLayout = new StaticLayout(cueTextEdge, this.textPaint, textWidth3, textAlignment, this.spacingMult, this.spacingAdd, true);
            this.textLeft = textLeft;
            this.textTop = textTop;
            this.textPaddingX = textPaddingX;
            return;
        }
        Log.w(TAG, "Skipped drawing subtitle cue (invalid horizontal positioning)");
    }

    @RequiresNonNull({"cueBitmap"})
    private void setupBitmapLayout() {
        int height;
        float f;
        float f2;
        Bitmap cueBitmap = this.cueBitmap;
        int parentWidth = this.parentRight - this.parentLeft;
        int parentHeight = this.parentBottom - this.parentTop;
        float anchorX = this.parentLeft + (parentWidth * this.cuePosition);
        float anchorY = this.parentTop + (parentHeight * this.cueLine);
        int width = Math.round(parentWidth * this.cueSize);
        if (this.cueBitmapHeight != -3.4028235E38f) {
            height = Math.round(parentHeight * this.cueBitmapHeight);
        } else {
            height = Math.round(width * (cueBitmap.getHeight() / cueBitmap.getWidth()));
        }
        if (this.cuePositionAnchor == 2) {
            f = anchorX - width;
        } else {
            f = this.cuePositionAnchor == 1 ? anchorX - (width / 2) : anchorX;
        }
        int x = Math.round(f);
        if (this.cueLineAnchor == 2) {
            f2 = anchorY - height;
        } else {
            f2 = this.cueLineAnchor == 1 ? anchorY - (height / 2) : anchorY;
        }
        int y = Math.round(f2);
        this.bitmapRect = new Rect(x, y, x + width, y + height);
    }

    private void drawLayout(Canvas canvas, boolean isTextCue) {
        if (isTextCue) {
            drawTextLayout(canvas);
            return;
        }
        Assertions.checkNotNull(this.bitmapRect);
        Assertions.checkNotNull(this.cueBitmap);
        drawBitmapLayout(canvas);
    }

    private void drawTextLayout(Canvas canvas) {
        Canvas canvas2;
        StaticLayout textLayout = this.textLayout;
        StaticLayout edgeLayout = this.edgeLayout;
        if (textLayout != null && edgeLayout != null) {
            int saveCount = canvas.save();
            canvas.translate(this.textLeft, this.textTop);
            if (Color.alpha(this.windowColor) <= 0) {
                canvas2 = canvas;
            } else {
                this.windowPaint.setColor(this.windowColor);
                canvas2 = canvas;
                canvas2.drawRect(-this.textPaddingX, 0.0f, textLayout.getWidth() + this.textPaddingX, textLayout.getHeight(), this.windowPaint);
            }
            if (this.edgeType == 1) {
                this.textPaint.setStrokeJoin(Paint.Join.ROUND);
                this.textPaint.setStrokeWidth(this.outlineWidth);
                this.textPaint.setColor(this.edgeColor);
                this.textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                edgeLayout.draw(canvas2);
            } else if (this.edgeType == 2) {
                this.textPaint.setShadowLayer(this.shadowRadius, this.shadowOffset, this.shadowOffset, this.edgeColor);
            } else if (this.edgeType == 3 || this.edgeType == 4) {
                boolean raised = this.edgeType == 3;
                int colorUp = raised ? -1 : this.edgeColor;
                int colorDown = raised ? this.edgeColor : -1;
                float offset = this.shadowRadius / 2.0f;
                this.textPaint.setColor(this.foregroundColor);
                this.textPaint.setStyle(Paint.Style.FILL);
                this.textPaint.setShadowLayer(this.shadowRadius, -offset, -offset, colorUp);
                edgeLayout.draw(canvas2);
                this.textPaint.setShadowLayer(this.shadowRadius, offset, offset, colorDown);
            }
            this.textPaint.setColor(this.foregroundColor);
            this.textPaint.setStyle(Paint.Style.FILL);
            textLayout.draw(canvas2);
            this.textPaint.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
            canvas2.restoreToCount(saveCount);
        }
    }

    @RequiresNonNull({"cueBitmap", "bitmapRect"})
    private void drawBitmapLayout(Canvas canvas) {
        canvas.drawBitmap(this.cueBitmap, (Rect) null, this.bitmapRect, this.bitmapPaint);
    }

    private static boolean areCharSequencesEqual(CharSequence first, CharSequence second) {
        return first == second || (first != null && first.equals(second));
    }
}
