package androidx.media3.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import androidx.media3.common.text.Cue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class CanvasSubtitleOutput extends View implements SubtitleView.Output {
    private float bottomPaddingFraction;
    private List<Cue> cues;
    private final List<SubtitlePainter> painters;
    private CaptionStyleCompat style;
    private float textSize;
    private int textSizeType;

    public CanvasSubtitleOutput(Context context) {
        this(context, null);
    }

    public CanvasSubtitleOutput(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.painters = new ArrayList();
        this.cues = Collections.emptyList();
        this.textSizeType = 0;
        this.textSize = 0.0533f;
        this.style = CaptionStyleCompat.DEFAULT;
        this.bottomPaddingFraction = 0.08f;
    }

    @Override // androidx.media3.ui.SubtitleView.Output
    public void update(List<Cue> cues, CaptionStyleCompat style, float textSize, int textSizeType, float bottomPaddingFraction) {
        this.cues = cues;
        this.style = style;
        this.textSize = textSize;
        this.textSizeType = textSizeType;
        this.bottomPaddingFraction = bottomPaddingFraction;
        while (this.painters.size() < cues.size()) {
            this.painters.add(new SubtitlePainter(getContext()));
        }
        invalidate();
    }

    @Override // android.view.View
    public void dispatchDraw(Canvas canvas) {
        List<Cue> cues = this.cues;
        if (cues.isEmpty()) {
            return;
        }
        int rawViewHeight = getHeight();
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getWidth() - getPaddingRight();
        int bottom = rawViewHeight - getPaddingBottom();
        if (bottom <= top || right <= left) {
            return;
        }
        int viewHeightMinusPadding = bottom - top;
        float defaultViewTextSizePx = SubtitleViewUtils.resolveTextSize(this.textSizeType, this.textSize, rawViewHeight, viewHeightMinusPadding);
        if (defaultViewTextSizePx <= 0.0f) {
            return;
        }
        int cueCount = cues.size();
        for (int i = 0; i < cueCount; i++) {
            Cue cue = cues.get(i);
            if (cue.verticalType != Integer.MIN_VALUE) {
                cue = repositionVerticalCue(cue);
            }
            float cueTextSizePx = SubtitleViewUtils.resolveTextSize(cue.textSizeType, cue.textSize, rawViewHeight, viewHeightMinusPadding);
            SubtitlePainter painter = this.painters.get(i);
            painter.draw(cue, this.style, defaultViewTextSizePx, cueTextSizePx, this.bottomPaddingFraction, canvas, left, top, right, bottom);
        }
    }

    private static Cue repositionVerticalCue(Cue cue) {
        Cue.Builder cueBuilder = cue.buildUpon().setPosition(-3.4028235E38f).setPositionAnchor(Integer.MIN_VALUE).setTextAlignment(null);
        if (cue.lineType == 0) {
            cueBuilder.setLine(1.0f - cue.line, 0);
        } else {
            cueBuilder.setLine((-cue.line) - 1.0f, 1);
        }
        switch (cue.lineAnchor) {
            case 0:
                cueBuilder.setLineAnchor(2);
                break;
            case 2:
                cueBuilder.setLineAnchor(0);
                break;
        }
        return cueBuilder.build();
    }
}
