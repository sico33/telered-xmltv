package androidx.media3.exoplayer.video.spherical;

import android.content.Context;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
final class TouchTracker extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener, OrientationListener.Listener {
    static final float MAX_PITCH_DEGREES = 45.0f;
    private final GestureDetector gestureDetector;
    private final Listener listener;
    private final float pxPerDegrees;
    private final PointF previousTouchPointPx = new PointF();
    private final PointF accumulatedTouchOffsetDegrees = new PointF();
    private volatile float roll = 3.1415927f;

    public interface Listener {
        void onScrollChange(PointF pointF);

        boolean onSingleTapUp(MotionEvent motionEvent);

        /* JADX INFO: renamed from: androidx.media3.exoplayer.video.spherical.TouchTracker$Listener$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static boolean $default$onSingleTapUp(Listener _this, MotionEvent event) {
                return false;
            }
        }
    }

    public TouchTracker(Context context, Listener listener, float pxPerDegrees) {
        this.listener = listener;
        this.pxPerDegrees = pxPerDegrees;
        this.gestureDetector = new GestureDetector(context, this);
    }

    @Override // android.view.View.OnTouchListener
    public boolean onTouch(View v, MotionEvent event) {
        return this.gestureDetector.onTouchEvent(event);
    }

    @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
    public boolean onDown(MotionEvent e) {
        this.previousTouchPointPx.set(e.getX(), e.getY());
        return true;
    }

    @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float touchX = (e2.getX() - this.previousTouchPointPx.x) / this.pxPerDegrees;
        float touchY = (e2.getY() - this.previousTouchPointPx.y) / this.pxPerDegrees;
        this.previousTouchPointPx.set(e2.getX(), e2.getY());
        float r = this.roll;
        float cr = (float) Math.cos(r);
        float sr = (float) Math.sin(r);
        this.accumulatedTouchOffsetDegrees.x -= (cr * touchX) - (sr * touchY);
        this.accumulatedTouchOffsetDegrees.y += (sr * touchX) + (cr * touchY);
        this.accumulatedTouchOffsetDegrees.y = Math.max(-45.0f, Math.min(MAX_PITCH_DEGREES, this.accumulatedTouchOffsetDegrees.y));
        this.listener.onScrollChange(this.accumulatedTouchOffsetDegrees);
        return true;
    }

    @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
    public boolean onSingleTapUp(MotionEvent e) {
        return this.listener.onSingleTapUp(e);
    }

    @Override // androidx.media3.exoplayer.video.spherical.OrientationListener.Listener
    public void onOrientationChange(float[] deviceOrientationMatrix, float roll) {
        this.roll = -roll;
    }
}
