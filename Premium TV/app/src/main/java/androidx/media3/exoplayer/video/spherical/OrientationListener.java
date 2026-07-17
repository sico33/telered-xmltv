package androidx.media3.exoplayer.video.spherical;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.view.Display;
import androidx.media3.extractor.ts.TsExtractor;

/* JADX INFO: loaded from: classes.dex */
final class OrientationListener implements SensorEventListener {
    private final Display display;
    private final Listener[] listeners;
    private boolean recenterMatrixComputed;
    private final float[] deviceOrientationMatrix4x4 = new float[16];
    private final float[] tempMatrix4x4 = new float[16];
    private final float[] recenterMatrix4x4 = new float[16];
    private final float[] angles = new float[3];

    public interface Listener {
        void onOrientationChange(float[] fArr, float f);
    }

    public OrientationListener(Display display, Listener... listeners) {
        this.display = display;
        this.listeners = listeners;
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(this.deviceOrientationMatrix4x4, event.values);
        rotateAroundZ(this.deviceOrientationMatrix4x4, this.display.getRotation());
        float roll = extractRoll(this.deviceOrientationMatrix4x4);
        rotateYtoSky(this.deviceOrientationMatrix4x4);
        recenter(this.deviceOrientationMatrix4x4);
        notifyListeners(this.deviceOrientationMatrix4x4, roll);
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void notifyListeners(float[] deviceOrientationMatrix, float roll) {
        for (Listener listener : this.listeners) {
            listener.onOrientationChange(deviceOrientationMatrix, roll);
        }
    }

    private void recenter(float[] matrix) {
        if (!this.recenterMatrixComputed) {
            FrameRotationQueue.computeRecenterMatrix(this.recenterMatrix4x4, matrix);
            this.recenterMatrixComputed = true;
        }
        System.arraycopy(matrix, 0, this.tempMatrix4x4, 0, this.tempMatrix4x4.length);
        Matrix.multiplyMM(matrix, 0, this.tempMatrix4x4, 0, this.recenterMatrix4x4, 0);
    }

    private float extractRoll(float[] matrix) {
        SensorManager.remapCoordinateSystem(matrix, 1, 131, this.tempMatrix4x4);
        SensorManager.getOrientation(this.tempMatrix4x4, this.angles);
        return this.angles[2];
    }

    private void rotateAroundZ(float[] matrix, int rotation) {
        int xAxis;
        int yAxis;
        switch (rotation) {
            case 0:
                return;
            case 1:
                xAxis = 2;
                yAxis = TsExtractor.TS_STREAM_TYPE_AC3;
                break;
            case 2:
                xAxis = TsExtractor.TS_STREAM_TYPE_AC3;
                yAxis = TsExtractor.TS_STREAM_TYPE_HDMV_DTS;
                break;
            case 3:
                xAxis = TsExtractor.TS_STREAM_TYPE_HDMV_DTS;
                yAxis = 1;
                break;
            default:
                throw new IllegalStateException();
        }
        System.arraycopy(matrix, 0, this.tempMatrix4x4, 0, this.tempMatrix4x4.length);
        SensorManager.remapCoordinateSystem(this.tempMatrix4x4, xAxis, yAxis, matrix);
    }

    private static void rotateYtoSky(float[] matrix) {
        Matrix.rotateM(matrix, 0, 90.0f, 1.0f, 0.0f, 0.0f);
    }
}
