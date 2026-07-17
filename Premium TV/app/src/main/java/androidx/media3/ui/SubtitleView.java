package androidx.media3.ui;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.FrameLayout;
import androidx.media3.common.text.Cue;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class SubtitleView extends FrameLayout {
    public static final float DEFAULT_BOTTOM_PADDING_FRACTION = 0.08f;
    public static final float DEFAULT_TEXT_SIZE_FRACTION = 0.0533f;
    public static final int VIEW_TYPE_CANVAS = 1;
    public static final int VIEW_TYPE_WEB = 2;
    private boolean applyEmbeddedFontSizes;
    private boolean applyEmbeddedStyles;
    private float bottomPaddingFraction;
    private List<Cue> cues;
    private float defaultTextSize;
    private int defaultTextSizeType;
    private View innerSubtitleView;
    private Output output;
    private CaptionStyleCompat style;
    private int viewType;

    interface Output {
        void update(List<Cue> list, CaptionStyleCompat captionStyleCompat, float f, int i, float f2);
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ViewType {
    }

    public SubtitleView(Context context) {
        this(context, null);
    }

    public SubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.cues = Collections.emptyList();
        this.style = CaptionStyleCompat.DEFAULT;
        this.defaultTextSizeType = 0;
        this.defaultTextSize = 0.0533f;
        this.bottomPaddingFraction = 0.08f;
        this.applyEmbeddedStyles = true;
        this.applyEmbeddedFontSizes = true;
        CanvasSubtitleOutput canvasSubtitleOutput = new CanvasSubtitleOutput(context);
        this.output = canvasSubtitleOutput;
        this.innerSubtitleView = canvasSubtitleOutput;
        addView(this.innerSubtitleView);
        this.viewType = 1;
    }

    public void setCues(List<Cue> cues) {
        this.cues = cues != null ? cues : Collections.emptyList();
        updateOutput();
    }

    public void setViewType(int viewType) {
        if (this.viewType == viewType) {
            return;
        }
        switch (viewType) {
            case 1:
                setView(new CanvasSubtitleOutput(getContext()));
                break;
            case 2:
                setView(new WebViewSubtitleOutput(getContext()));
                break;
            default:
                throw new IllegalArgumentException();
        }
        this.viewType = viewType;
    }

    private <T extends View & Output> void setView(T view) {
        removeView(this.innerSubtitleView);
        if (this.innerSubtitleView instanceof WebViewSubtitleOutput) {
            ((WebViewSubtitleOutput) this.innerSubtitleView).destroy();
        }
        this.innerSubtitleView = view;
        this.output = view;
        addView(view);
    }

    public void setFixedTextSize(int unit, float size) {
        Resources resources;
        Context context = getContext();
        if (context == null) {
            resources = Resources.getSystem();
        } else {
            resources = context.getResources();
        }
        setTextSize(2, TypedValue.applyDimension(unit, size, resources.getDisplayMetrics()));
    }

    public void setUserDefaultTextSize() {
        setFractionalTextSize(getUserCaptionFontScale() * 0.0533f);
    }

    public void setFractionalTextSize(float fractionOfHeight) {
        setFractionalTextSize(fractionOfHeight, false);
    }

    public void setFractionalTextSize(float fractionOfHeight, boolean ignorePadding) {
        int i;
        if (ignorePadding) {
            i = 1;
        } else {
            i = 0;
        }
        setTextSize(i, fractionOfHeight);
    }

    private void setTextSize(int textSizeType, float textSize) {
        this.defaultTextSizeType = textSizeType;
        this.defaultTextSize = textSize;
        updateOutput();
    }

    public void setApplyEmbeddedStyles(boolean applyEmbeddedStyles) {
        this.applyEmbeddedStyles = applyEmbeddedStyles;
        updateOutput();
    }

    public void setApplyEmbeddedFontSizes(boolean applyEmbeddedFontSizes) {
        this.applyEmbeddedFontSizes = applyEmbeddedFontSizes;
        updateOutput();
    }

    public void setUserDefaultStyle() {
        setStyle(getUserCaptionStyle());
    }

    public void setStyle(CaptionStyleCompat style) {
        this.style = style;
        updateOutput();
    }

    public void setBottomPaddingFraction(float bottomPaddingFraction) {
        this.bottomPaddingFraction = bottomPaddingFraction;
        updateOutput();
    }

    private float getUserCaptionFontScale() {
        CaptioningManager captioningManager;
        if (isInEditMode() || (captioningManager = (CaptioningManager) getContext().getSystemService("captioning")) == null || !captioningManager.isEnabled()) {
            return 1.0f;
        }
        return captioningManager.getFontScale();
    }

    private CaptionStyleCompat getUserCaptionStyle() {
        if (isInEditMode()) {
            return CaptionStyleCompat.DEFAULT;
        }
        CaptioningManager captioningManager = (CaptioningManager) getContext().getSystemService("captioning");
        if (captioningManager != null && captioningManager.isEnabled()) {
            return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
        }
        return CaptionStyleCompat.DEFAULT;
    }

    private void updateOutput() {
        this.output.update(getCuesWithStylingPreferencesApplied(), this.style, this.defaultTextSize, this.defaultTextSizeType, this.bottomPaddingFraction);
    }

    private List<Cue> getCuesWithStylingPreferencesApplied() {
        if (this.applyEmbeddedStyles && this.applyEmbeddedFontSizes) {
            return this.cues;
        }
        List<Cue> strippedCues = new ArrayList<>(this.cues.size());
        for (int i = 0; i < this.cues.size(); i++) {
            strippedCues.add(removeEmbeddedStyling(this.cues.get(i)));
        }
        return strippedCues;
    }

    private Cue removeEmbeddedStyling(Cue cue) {
        Cue.Builder strippedCue = cue.buildUpon();
        if (!this.applyEmbeddedStyles) {
            SubtitleViewUtils.removeAllEmbeddedStyling(strippedCue);
        } else if (!this.applyEmbeddedFontSizes) {
            SubtitleViewUtils.removeEmbeddedFontSizes(strippedCue);
        }
        return strippedCue.build();
    }
}
