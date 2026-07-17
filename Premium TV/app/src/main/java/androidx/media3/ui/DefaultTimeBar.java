package androidx.media3.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.util.Collections;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArraySet;

/* JADX INFO: loaded from: classes.dex */
public class DefaultTimeBar extends View implements TimeBar {
    private static final String ACCESSIBILITY_CLASS_NAME = "android.widget.SeekBar";
    public static final int BAR_GRAVITY_BOTTOM = 1;
    public static final int BAR_GRAVITY_CENTER = 0;
    public static final int DEFAULT_AD_MARKER_COLOR = -1291845888;
    public static final int DEFAULT_AD_MARKER_WIDTH_DP = 4;
    public static final int DEFAULT_BAR_HEIGHT_DP = 4;
    public static final int DEFAULT_BUFFERED_COLOR = -855638017;
    private static final int DEFAULT_INCREMENT_COUNT = 20;
    public static final int DEFAULT_PLAYED_AD_MARKER_COLOR = 872414976;
    public static final int DEFAULT_PLAYED_COLOR = -1;
    public static final int DEFAULT_SCRUBBER_COLOR = -1;
    public static final int DEFAULT_SCRUBBER_DISABLED_SIZE_DP = 0;
    public static final int DEFAULT_SCRUBBER_DRAGGED_SIZE_DP = 16;
    public static final int DEFAULT_SCRUBBER_ENABLED_SIZE_DP = 12;
    public static final int DEFAULT_TOUCH_TARGET_HEIGHT_DP = 26;
    public static final int DEFAULT_UNPLAYED_COLOR = 872415231;
    private static final int FINE_SCRUB_RATIO = 3;
    private static final int FINE_SCRUB_Y_THRESHOLD_DP = -50;
    private static final float HIDDEN_SCRUBBER_SCALE = 0.0f;
    private static final float SHOWN_SCRUBBER_SCALE = 1.0f;
    private static final long STOP_SCRUBBING_TIMEOUT_MS = 1000;
    private int adGroupCount;
    private long[] adGroupTimesMs;
    private final Paint adMarkerPaint;
    private final int adMarkerWidth;
    private final int barGravity;
    private final int barHeight;
    private final Rect bufferedBar;
    private final Paint bufferedPaint;
    private long bufferedPosition;
    private final float density;
    private long duration;
    private final int fineScrubYThreshold;
    private final StringBuilder formatBuilder;
    private final Formatter formatter;
    private int keyCountIncrement;
    private long keyTimeIncrement;
    private int lastCoarseScrubXPosition;
    private Rect lastExclusionRectangle;
    private final CopyOnWriteArraySet<TimeBar.OnScrubListener> listeners;
    private boolean[] playedAdGroups;
    private final Paint playedAdMarkerPaint;
    private final Paint playedPaint;
    private long position;
    private final Rect progressBar;
    private long scrubPosition;
    private final Rect scrubberBar;
    private final int scrubberDisabledSize;
    private final int scrubberDraggedSize;
    private final Drawable scrubberDrawable;
    private final int scrubberEnabledSize;
    private final int scrubberPadding;
    private boolean scrubberPaddingDisabled;
    private final Paint scrubberPaint;
    private float scrubberScale;
    private ValueAnimator scrubberScalingAnimator;
    private boolean scrubbing;
    private final Rect seekBounds;
    private final Runnable stopScrubbingRunnable;
    private final Point touchPosition;
    private final int touchTargetHeight;
    private final Paint unplayedPaint;

    public DefaultTimeBar(Context context) {
        this(context, null);
    }

    public DefaultTimeBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DefaultTimeBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, attrs);
    }

    public DefaultTimeBar(Context context, AttributeSet attrs, int defStyleAttr, AttributeSet timebarAttrs) {
        this(context, attrs, defStyleAttr, timebarAttrs, 0);
    }

    public DefaultTimeBar(Context context, AttributeSet attrs, int defStyleAttr, AttributeSet timebarAttrs, int defStyleRes) throws Throwable {
        super(context, attrs, defStyleAttr);
        this.seekBounds = new Rect();
        this.progressBar = new Rect();
        this.bufferedBar = new Rect();
        this.scrubberBar = new Rect();
        this.playedPaint = new Paint();
        this.bufferedPaint = new Paint();
        this.unplayedPaint = new Paint();
        this.adMarkerPaint = new Paint();
        this.playedAdMarkerPaint = new Paint();
        this.scrubberPaint = new Paint();
        this.scrubberPaint.setAntiAlias(true);
        this.listeners = new CopyOnWriteArraySet<>();
        this.touchPosition = new Point();
        Resources res = context.getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        this.density = displayMetrics.density;
        this.fineScrubYThreshold = dpToPx(this.density, FINE_SCRUB_Y_THRESHOLD_DP);
        int defaultBarHeight = dpToPx(this.density, 4);
        int defaultTouchTargetHeight = dpToPx(this.density, 26);
        int defaultAdMarkerWidth = dpToPx(this.density, 4);
        int defaultScrubberEnabledSize = dpToPx(this.density, 12);
        int defaultScrubberDisabledSize = dpToPx(this.density, 0);
        int defaultScrubberDraggedSize = dpToPx(this.density, 16);
        if (timebarAttrs == null) {
            this.barHeight = defaultBarHeight;
            this.touchTargetHeight = defaultTouchTargetHeight;
            this.barGravity = 0;
            this.adMarkerWidth = defaultAdMarkerWidth;
            this.scrubberEnabledSize = defaultScrubberEnabledSize;
            this.scrubberDisabledSize = defaultScrubberDisabledSize;
            this.scrubberDraggedSize = defaultScrubberDraggedSize;
            this.playedPaint.setColor(-1);
            this.scrubberPaint.setColor(-1);
            this.bufferedPaint.setColor(DEFAULT_BUFFERED_COLOR);
            this.unplayedPaint.setColor(DEFAULT_UNPLAYED_COLOR);
            this.adMarkerPaint.setColor(DEFAULT_AD_MARKER_COLOR);
            this.playedAdMarkerPaint.setColor(DEFAULT_PLAYED_AD_MARKER_COLOR);
            this.scrubberDrawable = null;
        } else {
            TypedArray a = context.getTheme().obtainStyledAttributes(timebarAttrs, R.styleable.DefaultTimeBar, defStyleAttr, defStyleRes);
            try {
                this.scrubberDrawable = a.getDrawable(R.styleable.DefaultTimeBar_scrubber_drawable);
                if (this.scrubberDrawable != null) {
                    try {
                        setDrawableLayoutDirection(this.scrubberDrawable);
                        defaultTouchTargetHeight = Math.max(this.scrubberDrawable.getMinimumHeight(), defaultTouchTargetHeight);
                    } catch (Throwable th) {
                        th = th;
                        a.recycle();
                        throw th;
                    }
                }
                try {
                    this.barHeight = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_bar_height, defaultBarHeight);
                    this.touchTargetHeight = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_touch_target_height, defaultTouchTargetHeight);
                    this.barGravity = a.getInt(R.styleable.DefaultTimeBar_bar_gravity, 0);
                    this.adMarkerWidth = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_ad_marker_width, defaultAdMarkerWidth);
                    this.scrubberEnabledSize = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_scrubber_enabled_size, defaultScrubberEnabledSize);
                    this.scrubberDisabledSize = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_scrubber_disabled_size, defaultScrubberDisabledSize);
                    this.scrubberDraggedSize = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_scrubber_dragged_size, defaultScrubberDraggedSize);
                    int playedColor = a.getInt(R.styleable.DefaultTimeBar_played_color, -1);
                    int scrubberColor = a.getInt(R.styleable.DefaultTimeBar_scrubber_color, -1);
                    int bufferedColor = a.getInt(R.styleable.DefaultTimeBar_buffered_color, DEFAULT_BUFFERED_COLOR);
                    try {
                        int unplayedColor = a.getInt(R.styleable.DefaultTimeBar_unplayed_color, DEFAULT_UNPLAYED_COLOR);
                        try {
                            int adMarkerColor = a.getInt(R.styleable.DefaultTimeBar_ad_marker_color, DEFAULT_AD_MARKER_COLOR);
                            try {
                                int playedAdMarkerColor = a.getInt(R.styleable.DefaultTimeBar_played_ad_marker_color, DEFAULT_PLAYED_AD_MARKER_COLOR);
                                this.playedPaint.setColor(playedColor);
                                this.scrubberPaint.setColor(scrubberColor);
                                this.bufferedPaint.setColor(bufferedColor);
                                this.unplayedPaint.setColor(unplayedColor);
                                this.adMarkerPaint.setColor(adMarkerColor);
                                this.playedAdMarkerPaint.setColor(playedAdMarkerColor);
                                a.recycle();
                            } catch (Throwable th2) {
                                th = th2;
                                a.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            } catch (Throwable th6) {
                th = th6;
            }
        }
        this.formatBuilder = new StringBuilder();
        this.formatter = new Formatter(this.formatBuilder, Locale.getDefault());
        this.stopScrubbingRunnable = new Runnable() { // from class: androidx.media3.ui.DefaultTimeBar$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m159lambda$new$0$androidxmedia3uiDefaultTimeBar();
            }
        };
        if (this.scrubberDrawable != null) {
            this.scrubberPadding = (this.scrubberDrawable.getMinimumWidth() + 1) / 2;
        } else {
            this.scrubberPadding = (Math.max(this.scrubberDisabledSize, Math.max(this.scrubberEnabledSize, this.scrubberDraggedSize)) + 1) / 2;
        }
        this.scrubberScale = 1.0f;
        this.scrubberScalingAnimator = new ValueAnimator();
        this.scrubberScalingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: androidx.media3.ui.DefaultTimeBar$$ExternalSyntheticLambda1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                this.f$0.m160lambda$new$1$androidxmedia3uiDefaultTimeBar(valueAnimator);
            }
        });
        this.duration = C.TIME_UNSET;
        this.keyTimeIncrement = C.TIME_UNSET;
        this.keyCountIncrement = 20;
        setFocusable(true);
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
    }

    /* JADX INFO: renamed from: lambda$new$0$androidx-media3-ui-DefaultTimeBar, reason: not valid java name */
    /* synthetic */ void m159lambda$new$0$androidxmedia3uiDefaultTimeBar() {
        stopScrubbing(false);
    }

    /* JADX INFO: renamed from: lambda$new$1$androidx-media3-ui-DefaultTimeBar, reason: not valid java name */
    /* synthetic */ void m160lambda$new$1$androidxmedia3uiDefaultTimeBar(ValueAnimator animation) {
        this.scrubberScale = ((Float) animation.getAnimatedValue()).floatValue();
        invalidate(this.seekBounds);
    }

    public void showScrubber() {
        if (this.scrubberScalingAnimator.isStarted()) {
            this.scrubberScalingAnimator.cancel();
        }
        this.scrubberPaddingDisabled = false;
        this.scrubberScale = 1.0f;
        invalidate(this.seekBounds);
    }

    public void showScrubber(long showAnimationDurationMs) {
        if (this.scrubberScalingAnimator.isStarted()) {
            this.scrubberScalingAnimator.cancel();
        }
        this.scrubberPaddingDisabled = false;
        this.scrubberScalingAnimator.setFloatValues(this.scrubberScale, 1.0f);
        this.scrubberScalingAnimator.setDuration(showAnimationDurationMs);
        this.scrubberScalingAnimator.start();
    }

    public void hideScrubber(boolean disableScrubberPadding) {
        if (this.scrubberScalingAnimator.isStarted()) {
            this.scrubberScalingAnimator.cancel();
        }
        this.scrubberPaddingDisabled = disableScrubberPadding;
        this.scrubberScale = 0.0f;
        invalidate(this.seekBounds);
    }

    public void hideScrubber(long hideAnimationDurationMs) {
        if (this.scrubberScalingAnimator.isStarted()) {
            this.scrubberScalingAnimator.cancel();
        }
        this.scrubberScalingAnimator.setFloatValues(this.scrubberScale, 0.0f);
        this.scrubberScalingAnimator.setDuration(hideAnimationDurationMs);
        this.scrubberScalingAnimator.start();
    }

    public void setPlayedColor(int playedColor) {
        this.playedPaint.setColor(playedColor);
        invalidate(this.seekBounds);
    }

    public void setScrubberColor(int scrubberColor) {
        this.scrubberPaint.setColor(scrubberColor);
        invalidate(this.seekBounds);
    }

    public void setBufferedColor(int bufferedColor) {
        this.bufferedPaint.setColor(bufferedColor);
        invalidate(this.seekBounds);
    }

    public void setUnplayedColor(int unplayedColor) {
        this.unplayedPaint.setColor(unplayedColor);
        invalidate(this.seekBounds);
    }

    public void setAdMarkerColor(int adMarkerColor) {
        this.adMarkerPaint.setColor(adMarkerColor);
        invalidate(this.seekBounds);
    }

    public void setPlayedAdMarkerColor(int playedAdMarkerColor) {
        this.playedAdMarkerPaint.setColor(playedAdMarkerColor);
        invalidate(this.seekBounds);
    }

    @Override // androidx.media3.ui.TimeBar
    public void addListener(TimeBar.OnScrubListener listener) {
        Assertions.checkNotNull(listener);
        this.listeners.add(listener);
    }

    @Override // androidx.media3.ui.TimeBar
    public void removeListener(TimeBar.OnScrubListener listener) {
        this.listeners.remove(listener);
    }

    @Override // androidx.media3.ui.TimeBar
    public void setKeyTimeIncrement(long time) {
        Assertions.checkArgument(time > 0);
        this.keyCountIncrement = -1;
        this.keyTimeIncrement = time;
    }

    @Override // androidx.media3.ui.TimeBar
    public void setKeyCountIncrement(int count) {
        Assertions.checkArgument(count > 0);
        this.keyCountIncrement = count;
        this.keyTimeIncrement = C.TIME_UNSET;
    }

    @Override // androidx.media3.ui.TimeBar
    public void setPosition(long position) {
        if (this.position == position) {
            return;
        }
        this.position = position;
        setContentDescription(getProgressText());
        update();
    }

    @Override // androidx.media3.ui.TimeBar
    public void setBufferedPosition(long bufferedPosition) {
        if (this.bufferedPosition == bufferedPosition) {
            return;
        }
        this.bufferedPosition = bufferedPosition;
        update();
    }

    @Override // androidx.media3.ui.TimeBar
    public void setDuration(long duration) {
        if (this.duration == duration) {
            return;
        }
        this.duration = duration;
        if (this.scrubbing && duration == C.TIME_UNSET) {
            stopScrubbing(true);
        }
        update();
    }

    @Override // androidx.media3.ui.TimeBar
    public long getPreferredUpdateDelay() {
        int timeBarWidthDp = pxToDp(this.density, this.progressBar.width());
        if (timeBarWidthDp == 0 || this.duration == 0 || this.duration == C.TIME_UNSET) {
            return Long.MAX_VALUE;
        }
        return this.duration / ((long) timeBarWidthDp);
    }

    @Override // androidx.media3.ui.TimeBar
    public void setAdGroupTimesMs(long[] adGroupTimesMs, boolean[] playedAdGroups, int adGroupCount) {
        Assertions.checkArgument(adGroupCount == 0 || !(adGroupTimesMs == null || playedAdGroups == null));
        this.adGroupCount = adGroupCount;
        this.adGroupTimesMs = adGroupTimesMs;
        this.playedAdGroups = playedAdGroups;
        update();
    }

    @Override // android.view.View, androidx.media3.ui.TimeBar
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.scrubbing && !enabled) {
            stopScrubbing(true);
        }
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        canvas.save();
        drawTimeBar(canvas);
        drawPlayhead(canvas);
        canvas.restore();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || this.duration <= 0) {
            return false;
        }
        Point touchPosition = resolveRelativeTouchPosition(event);
        int x = touchPosition.x;
        int y = touchPosition.y;
        switch (event.getAction()) {
            case 0:
                if (isInSeekBar(x, y)) {
                    positionScrubber(x);
                    startScrubbing(getScrubberPosition());
                    update();
                    invalidate();
                    return true;
                }
                return false;
            case 1:
            case 3:
                if (this.scrubbing) {
                    stopScrubbing(event.getAction() == 3);
                    return true;
                }
                return false;
            case 2:
                if (this.scrubbing) {
                    if (y < this.fineScrubYThreshold) {
                        int relativeX = x - this.lastCoarseScrubXPosition;
                        positionScrubber(this.lastCoarseScrubXPosition + (relativeX / 3));
                    } else {
                        this.lastCoarseScrubXPosition = x;
                        positionScrubber(x);
                    }
                    updateScrubbing(getScrubberPosition());
                    update();
                    invalidate();
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:14:0x001f  */
    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled()) {
            long positionIncrement = getPositionIncrement();
            switch (keyCode) {
                case 21:
                    positionIncrement = -positionIncrement;
                    if (scrubIncrementally(positionIncrement)) {
                        removeCallbacks(this.stopScrubbingRunnable);
                        postDelayed(this.stopScrubbingRunnable, 1000L);
                        return true;
                    }
                    break;
                case 22:
                    if (scrubIncrementally(positionIncrement)) {
                        removeCallbacks(this.stopScrubbingRunnable);
                        postDelayed(this.stopScrubbingRunnable, 1000L);
                        return true;
                    }
                    break;
                case 23:
                case 66:
                    if (this.scrubbing) {
                        stopScrubbing(false);
                        return true;
                    }
                    break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override // android.view.View
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (this.scrubbing && !gainFocus) {
            stopScrubbing(false);
        }
    }

    @Override // android.view.View
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    @Override // android.view.View
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.scrubberDrawable != null) {
            this.scrubberDrawable.jumpToCurrentState();
        }
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height;
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == 0) {
            height = this.touchTargetHeight;
        } else if (heightMode == 1073741824) {
            height = heightSize;
        } else {
            height = Math.min(this.touchTargetHeight, heightSize);
        }
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), height);
        updateDrawableState();
    }

    @Override // android.view.View
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int seekBoundsY;
        int progressBarY;
        int width = right - left;
        int height = bottom - top;
        int seekLeft = getPaddingLeft();
        int seekRight = width - getPaddingRight();
        int scrubberPadding = this.scrubberPaddingDisabled ? 0 : this.scrubberPadding;
        if (this.barGravity == 1) {
            seekBoundsY = (height - getPaddingBottom()) - this.touchTargetHeight;
            progressBarY = ((height - getPaddingBottom()) - this.barHeight) - Math.max(scrubberPadding - (this.barHeight / 2), 0);
        } else {
            seekBoundsY = (height - this.touchTargetHeight) / 2;
            progressBarY = (height - this.barHeight) / 2;
        }
        this.seekBounds.set(seekLeft, seekBoundsY, seekRight, this.touchTargetHeight + seekBoundsY);
        this.progressBar.set(this.seekBounds.left + scrubberPadding, progressBarY, this.seekBounds.right - scrubberPadding, this.barHeight + progressBarY);
        if (Util.SDK_INT >= 29) {
            setSystemGestureExclusionRectsV29(width, height);
        }
        update();
    }

    @Override // android.view.View
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (this.scrubberDrawable != null && setDrawableLayoutDirection(this.scrubberDrawable, layoutDirection)) {
            invalidate();
        }
    }

    @Override // android.view.View
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (event.getEventType() == 4) {
            event.getText().add(getProgressText());
        }
        event.setClassName(ACCESSIBILITY_CLASS_NAME);
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(ACCESSIBILITY_CLASS_NAME);
        info.setContentDescription(getProgressText());
        if (this.duration <= 0) {
            return;
        }
        if (Util.SDK_INT >= 21) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        } else {
            info.addAction(4096);
            info.addAction(8192);
        }
    }

    @Override // android.view.View
    public boolean performAccessibilityAction(int action, Bundle args) {
        if (super.performAccessibilityAction(action, args)) {
            return true;
        }
        if (this.duration <= 0) {
            return false;
        }
        if (action == 8192) {
            if (scrubIncrementally(-getPositionIncrement())) {
                stopScrubbing(false);
            }
        } else {
            if (action != 4096) {
                return false;
            }
            if (scrubIncrementally(getPositionIncrement())) {
                stopScrubbing(false);
            }
        }
        sendAccessibilityEvent(4);
        return true;
    }

    private void startScrubbing(long scrubPosition) {
        this.scrubPosition = scrubPosition;
        this.scrubbing = true;
        setPressed(true);
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        for (TimeBar.OnScrubListener listener : this.listeners) {
            listener.onScrubStart(this, scrubPosition);
        }
    }

    private void updateScrubbing(long scrubPosition) {
        if (this.scrubPosition == scrubPosition) {
            return;
        }
        this.scrubPosition = scrubPosition;
        for (TimeBar.OnScrubListener listener : this.listeners) {
            listener.onScrubMove(this, scrubPosition);
        }
    }

    private void stopScrubbing(boolean canceled) {
        removeCallbacks(this.stopScrubbingRunnable);
        this.scrubbing = false;
        setPressed(false);
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(false);
        }
        invalidate();
        for (TimeBar.OnScrubListener listener : this.listeners) {
            listener.onScrubStop(this, this.scrubPosition, canceled);
        }
    }

    private boolean scrubIncrementally(long positionChange) {
        if (this.duration <= 0) {
            return false;
        }
        long previousPosition = this.scrubbing ? this.scrubPosition : this.position;
        long scrubPosition = Util.constrainValue(previousPosition + positionChange, 0L, this.duration);
        if (scrubPosition == previousPosition) {
            return false;
        }
        if (!this.scrubbing) {
            startScrubbing(scrubPosition);
        } else {
            updateScrubbing(scrubPosition);
        }
        update();
        return true;
    }

    private void update() {
        this.bufferedBar.set(this.progressBar);
        this.scrubberBar.set(this.progressBar);
        long newScrubberTime = this.scrubbing ? this.scrubPosition : this.position;
        if (this.duration > 0) {
            int bufferedPixelWidth = (int) ((((long) this.progressBar.width()) * this.bufferedPosition) / this.duration);
            this.bufferedBar.right = Math.min(this.progressBar.left + bufferedPixelWidth, this.progressBar.right);
            int scrubberPixelPosition = (int) ((((long) this.progressBar.width()) * newScrubberTime) / this.duration);
            this.scrubberBar.right = Math.min(this.progressBar.left + scrubberPixelPosition, this.progressBar.right);
        } else {
            this.bufferedBar.right = this.progressBar.left;
            this.scrubberBar.right = this.progressBar.left;
        }
        invalidate(this.seekBounds);
    }

    private void positionScrubber(float xPosition) {
        this.scrubberBar.right = Util.constrainValue((int) xPosition, this.progressBar.left, this.progressBar.right);
    }

    private Point resolveRelativeTouchPosition(MotionEvent motionEvent) {
        this.touchPosition.set((int) motionEvent.getX(), (int) motionEvent.getY());
        return this.touchPosition;
    }

    private long getScrubberPosition() {
        if (this.progressBar.width() <= 0 || this.duration == C.TIME_UNSET) {
            return 0L;
        }
        return (((long) this.scrubberBar.width()) * this.duration) / ((long) this.progressBar.width());
    }

    private boolean isInSeekBar(float x, float y) {
        return this.seekBounds.contains((int) x, (int) y);
    }

    private void drawTimeBar(Canvas canvas) {
        int progressBarHeight = this.progressBar.height();
        int barTop = this.progressBar.centerY() - (progressBarHeight / 2);
        int barBottom = barTop + progressBarHeight;
        if (this.duration <= 0) {
            canvas.drawRect(this.progressBar.left, barTop, this.progressBar.right, barBottom, this.unplayedPaint);
            return;
        }
        int bufferedLeft = this.bufferedBar.left;
        int bufferedRight = this.bufferedBar.right;
        int progressLeft = Math.max(Math.max(this.progressBar.left, bufferedRight), this.scrubberBar.right);
        if (progressLeft < this.progressBar.right) {
            canvas.drawRect(progressLeft, barTop, this.progressBar.right, barBottom, this.unplayedPaint);
        }
        int bufferedLeft2 = Math.max(bufferedLeft, this.scrubberBar.right);
        if (bufferedRight > bufferedLeft2) {
            canvas.drawRect(bufferedLeft2, barTop, bufferedRight, barBottom, this.bufferedPaint);
        }
        if (this.scrubberBar.width() > 0) {
            canvas.drawRect(this.scrubberBar.left, barTop, this.scrubberBar.right, barBottom, this.playedPaint);
        }
        if (this.adGroupCount == 0) {
            return;
        }
        long[] adGroupTimesMs = (long[]) Assertions.checkNotNull(this.adGroupTimesMs);
        boolean[] playedAdGroups = (boolean[]) Assertions.checkNotNull(this.playedAdGroups);
        int adMarkerOffset = this.adMarkerWidth / 2;
        for (int i = 0; i < this.adGroupCount; i++) {
            long adGroupTimeMs = Util.constrainValue(adGroupTimesMs[i], 0L, this.duration);
            int markerPositionOffset = ((int) ((((long) this.progressBar.width()) * adGroupTimeMs) / this.duration)) - adMarkerOffset;
            int markerLeft = this.progressBar.left + Math.min(this.progressBar.width() - this.adMarkerWidth, Math.max(0, markerPositionOffset));
            Paint paint = playedAdGroups[i] ? this.playedAdMarkerPaint : this.adMarkerPaint;
            canvas.drawRect(markerLeft, barTop, this.adMarkerWidth + markerLeft, barBottom, paint);
        }
    }

    private void drawPlayhead(Canvas canvas) {
        int scrubberSize;
        if (this.duration <= 0) {
            return;
        }
        int playheadX = Util.constrainValue(this.scrubberBar.right, this.scrubberBar.left, this.progressBar.right);
        int playheadY = this.scrubberBar.centerY();
        if (this.scrubberDrawable == null) {
            if (this.scrubbing || isFocused()) {
                scrubberSize = this.scrubberDraggedSize;
            } else {
                scrubberSize = isEnabled() ? this.scrubberEnabledSize : this.scrubberDisabledSize;
            }
            int playheadRadius = (int) ((scrubberSize * this.scrubberScale) / 2.0f);
            canvas.drawCircle(playheadX, playheadY, playheadRadius, this.scrubberPaint);
            return;
        }
        int scrubberDrawableWidth = (int) (this.scrubberDrawable.getIntrinsicWidth() * this.scrubberScale);
        int scrubberDrawableHeight = (int) (this.scrubberDrawable.getIntrinsicHeight() * this.scrubberScale);
        this.scrubberDrawable.setBounds(playheadX - (scrubberDrawableWidth / 2), playheadY - (scrubberDrawableHeight / 2), (scrubberDrawableWidth / 2) + playheadX, (scrubberDrawableHeight / 2) + playheadY);
        this.scrubberDrawable.draw(canvas);
    }

    private void updateDrawableState() {
        if (this.scrubberDrawable != null && this.scrubberDrawable.isStateful() && this.scrubberDrawable.setState(getDrawableState())) {
            invalidate();
        }
    }

    private void setSystemGestureExclusionRectsV29(int width, int height) {
        if (this.lastExclusionRectangle != null && this.lastExclusionRectangle.width() == width && this.lastExclusionRectangle.height() == height) {
            return;
        }
        this.lastExclusionRectangle = new Rect(0, 0, width, height);
        setSystemGestureExclusionRects(Collections.singletonList(this.lastExclusionRectangle));
    }

    private String getProgressText() {
        return Util.getStringForTime(this.formatBuilder, this.formatter, this.position);
    }

    private long getPositionIncrement() {
        if (this.keyTimeIncrement != C.TIME_UNSET) {
            return this.keyTimeIncrement;
        }
        if (this.duration == C.TIME_UNSET) {
            return 0L;
        }
        return this.duration / ((long) this.keyCountIncrement);
    }

    private boolean setDrawableLayoutDirection(Drawable drawable) {
        return Util.SDK_INT >= 23 && setDrawableLayoutDirection(drawable, getLayoutDirection());
    }

    private static boolean setDrawableLayoutDirection(Drawable drawable, int layoutDirection) {
        return Util.SDK_INT >= 23 && drawable.setLayoutDirection(layoutDirection);
    }

    private static int dpToPx(float density, int dps) {
        return (int) ((dps * density) + 0.5f);
    }

    private static int pxToDp(float density, int px) {
        return (int) (px / density);
    }
}
