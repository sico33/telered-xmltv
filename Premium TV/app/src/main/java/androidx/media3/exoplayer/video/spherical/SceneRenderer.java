package androidx.media3.exoplayer.video.spherical;

import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.Matrix;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.TimedValueQueue;
import androidx.media3.exoplayer.video.VideoFrameMetadataListener;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: classes.dex */
final class SceneRenderer implements VideoFrameMetadataListener, CameraMotionListener {
    private static final String TAG = "SceneRenderer";
    private byte[] lastProjectionData;
    private SurfaceTexture surfaceTexture;
    private int textureId;
    private final AtomicBoolean frameAvailable = new AtomicBoolean();
    private final AtomicBoolean resetRotationAtNextFrame = new AtomicBoolean(true);
    private final ProjectionRenderer projectionRenderer = new ProjectionRenderer();
    private final FrameRotationQueue frameRotationQueue = new FrameRotationQueue();
    private final TimedValueQueue<Long> sampleTimestampQueue = new TimedValueQueue<>();
    private final TimedValueQueue<Projection> projectionQueue = new TimedValueQueue<>();
    private final float[] rotationMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    private volatile int defaultStereoMode = 0;
    private int lastStereoMode = -1;

    public void setDefaultStereoMode(int stereoMode) {
        this.defaultStereoMode = stereoMode;
    }

    public SurfaceTexture init() {
        try {
            GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            GlUtil.checkGlError();
            this.projectionRenderer.init();
            GlUtil.checkGlError();
            this.textureId = GlUtil.createExternalTexture();
        } catch (GlUtil.GlException e) {
            Log.e(TAG, "Failed to initialize the renderer", e);
        }
        this.surfaceTexture = new SurfaceTexture(this.textureId);
        this.surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() { // from class: androidx.media3.exoplayer.video.spherical.SceneRenderer$$ExternalSyntheticLambda0
            @Override // android.graphics.SurfaceTexture.OnFrameAvailableListener
            public final void onFrameAvailable(SurfaceTexture surfaceTexture) {
                this.f$0.m153x200ab998(surfaceTexture);
            }
        });
        return this.surfaceTexture;
    }

    /* JADX INFO: renamed from: lambda$init$0$androidx-media3-exoplayer-video-spherical-SceneRenderer, reason: not valid java name */
    /* synthetic */ void m153x200ab998(SurfaceTexture surfaceTexture) {
        this.frameAvailable.set(true);
    }

    public void drawFrame(float[] viewProjectionMatrix, boolean rightEye) {
        GLES20.glClear(16384);
        try {
            GlUtil.checkGlError();
        } catch (GlUtil.GlException e) {
            Log.e(TAG, "Failed to draw a frame", e);
        }
        if (this.frameAvailable.compareAndSet(true, false)) {
            ((SurfaceTexture) Assertions.checkNotNull(this.surfaceTexture)).updateTexImage();
            try {
                GlUtil.checkGlError();
            } catch (GlUtil.GlException e2) {
                Log.e(TAG, "Failed to draw a frame", e2);
            }
            if (this.resetRotationAtNextFrame.compareAndSet(true, false)) {
                GlUtil.setToIdentity(this.rotationMatrix);
            }
            long lastFrameTimestampNs = this.surfaceTexture.getTimestamp();
            Long sampleTimestampUs = this.sampleTimestampQueue.poll(lastFrameTimestampNs);
            if (sampleTimestampUs != null) {
                this.frameRotationQueue.pollRotationMatrix(this.rotationMatrix, sampleTimestampUs.longValue());
            }
            Projection projection = this.projectionQueue.pollFloor(lastFrameTimestampNs);
            if (projection != null) {
                this.projectionRenderer.setProjection(projection);
            }
        }
        Matrix.multiplyMM(this.tempMatrix, 0, viewProjectionMatrix, 0, this.rotationMatrix, 0);
        this.projectionRenderer.draw(this.textureId, this.tempMatrix, rightEye);
    }

    public void shutdown() {
        this.projectionRenderer.shutdown();
    }

    @Override // androidx.media3.exoplayer.video.VideoFrameMetadataListener
    public void onVideoFrameAboutToBeRendered(long presentationTimeUs, long releaseTimeNs, Format format, MediaFormat mediaFormat) {
        this.sampleTimestampQueue.add(releaseTimeNs, Long.valueOf(presentationTimeUs));
        setProjection(format.projectionData, format.stereoMode, releaseTimeNs);
    }

    @Override // androidx.media3.exoplayer.video.spherical.CameraMotionListener
    public void onCameraMotion(long timeUs, float[] rotation) {
        this.frameRotationQueue.setRotation(timeUs, rotation);
    }

    @Override // androidx.media3.exoplayer.video.spherical.CameraMotionListener
    public void onCameraMotionReset() {
        this.sampleTimestampQueue.clear();
        this.frameRotationQueue.reset();
        this.resetRotationAtNextFrame.set(true);
    }

    private void setProjection(byte[] projectionData, int stereoMode, long timeNs) {
        Projection projection;
        byte[] oldProjectionData = this.lastProjectionData;
        int oldStereoMode = this.lastStereoMode;
        this.lastProjectionData = projectionData;
        this.lastStereoMode = stereoMode == -1 ? this.defaultStereoMode : stereoMode;
        if (oldStereoMode == this.lastStereoMode && Arrays.equals(oldProjectionData, this.lastProjectionData)) {
            return;
        }
        Projection projectionFromData = null;
        if (this.lastProjectionData != null) {
            projectionFromData = ProjectionDecoder.decode(this.lastProjectionData, this.lastStereoMode);
        }
        if (projectionFromData != null && ProjectionRenderer.isSupported(projectionFromData)) {
            projection = projectionFromData;
        } else {
            projection = Projection.createEquirectangular(this.lastStereoMode);
        }
        this.projectionQueue.add(timeNs, projection);
    }
}
