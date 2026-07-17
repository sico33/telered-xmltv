package androidx.media3.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class AspectRatioFrameLayout extends FrameLayout {
    private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION = 0.01f;
    public static final int RESIZE_MODE_FILL = 3;
    public static final int RESIZE_MODE_FIT = 0;
    public static final int RESIZE_MODE_FIXED_HEIGHT = 2;
    public static final int RESIZE_MODE_FIXED_WIDTH = 1;
    public static final int RESIZE_MODE_ZOOM = 4;
    private AspectRatioListener aspectRatioListener;
    private final AspectRatioUpdateDispatcher aspectRatioUpdateDispatcher;
    private int resizeMode;
    private float videoAspectRatio;

    public interface AspectRatioListener {
        void onAspectRatioUpdated(float f, float f2, boolean z);
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResizeMode {
    }

    public AspectRatioFrameLayout(Context context) {
        this(context, null);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.resizeMode = 0;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout, 0, 0);
            try {
                this.resizeMode = a.getInt(R.styleable.AspectRatioFrameLayout_resize_mode, 0);
                a.recycle();
            } catch (Throwable th) {
                a.recycle();
                throw th;
            }
        }
        this.aspectRatioUpdateDispatcher = new AspectRatioUpdateDispatcher();
    }

    public void setAspectRatio(float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio;
            requestLayout();
        }
    }

    public void setAspectRatioListener(AspectRatioListener listener) {
        this.aspectRatioListener = listener;
    }

    public int getResizeMode() {
        return this.resizeMode;
    }

    public void setResizeMode(int resizeMode) {
        if (this.resizeMode != resizeMode) {
            this.resizeMode = resizeMode;
            requestLayout();
        }
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.videoAspectRatio <= 0.0f) {
            return;
        }
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        float viewAspectRatio = width / height;
        float aspectDeformation = (this.videoAspectRatio / viewAspectRatio) - 1.0f;
        if (Math.abs(aspectDeformation) <= MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
            this.aspectRatioUpdateDispatcher.scheduleUpdate(this.videoAspectRatio, viewAspectRatio, false);
            return;
        }
        switch (this.resizeMode) {
            case 0:
                float f = this.videoAspectRatio;
                if (aspectDeformation > 0.0f) {
                    height = (int) (width / f);
                } else {
                    width = (int) (height * f);
                }
                break;
            case 1:
                height = (int) (width / this.videoAspectRatio);
                break;
            case 2:
                width = (int) (height * this.videoAspectRatio);
                break;
            case 4:
                float f2 = this.videoAspectRatio;
                if (aspectDeformation > 0.0f) {
                    width = (int) (height * f2);
                } else {
                    height = (int) (width / f2);
                }
                break;
        }
        this.aspectRatioUpdateDispatcher.scheduleUpdate(this.videoAspectRatio, viewAspectRatio, true);
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(width, 1073741824), View.MeasureSpec.makeMeasureSpec(height, 1073741824));
    }

    private final class AspectRatioUpdateDispatcher implements Runnable {
        private boolean aspectRatioMismatch;
        private boolean isScheduled;
        private float naturalAspectRatio;
        private float targetAspectRatio;

        private AspectRatioUpdateDispatcher() {
        }

        public void scheduleUpdate(float targetAspectRatio, float naturalAspectRatio, boolean aspectRatioMismatch) {
            this.targetAspectRatio = targetAspectRatio;
            this.naturalAspectRatio = naturalAspectRatio;
            this.aspectRatioMismatch = aspectRatioMismatch;
            if (!this.isScheduled) {
                this.isScheduled = true;
                AspectRatioFrameLayout.this.post(this);
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            this.isScheduled = false;
            if (AspectRatioFrameLayout.this.aspectRatioListener != null) {
                AspectRatioFrameLayout.this.aspectRatioListener.onAspectRatioUpdated(this.targetAspectRatio, this.naturalAspectRatio, this.aspectRatioMismatch);
            }
        }
    }
}
