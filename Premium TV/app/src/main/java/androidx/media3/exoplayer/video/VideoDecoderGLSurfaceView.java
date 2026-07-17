package androidx.media3.exoplayer.video;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.GlProgram;
import androidx.media3.common.util.GlUtil;
import androidx.media3.decoder.VideoDecoderOutputBuffer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicReference;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class VideoDecoderGLSurfaceView extends GLSurfaceView implements VideoDecoderOutputBufferRenderer {
    private static final String TAG = "VideoDecoderGLSV";
    private final Renderer renderer;

    public VideoDecoderGLSurfaceView(Context context) {
        this(context, null);
    }

    public VideoDecoderGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.renderer = new Renderer(this);
        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setRenderer(this.renderer);
        setRenderMode(0);
    }

    @Override // androidx.media3.exoplayer.video.VideoDecoderOutputBufferRenderer
    public void setOutputBuffer(VideoDecoderOutputBuffer outputBuffer) {
        this.renderer.setOutputBuffer(outputBuffer);
    }

    @Deprecated
    public VideoDecoderOutputBufferRenderer getVideoDecoderOutputBufferRenderer() {
        return this;
    }

    private static final class Renderer implements GLSurfaceView.Renderer {
        private static final String FRAGMENT_SHADER = "precision mediump float;\nvarying vec2 interp_tc_y;\nvarying vec2 interp_tc_u;\nvarying vec2 interp_tc_v;\nuniform sampler2D y_tex;\nuniform sampler2D u_tex;\nuniform sampler2D v_tex;\nuniform mat3 mColorConversion;\nvoid main() {\n  vec3 yuv;\n  yuv.x = texture2D(y_tex, interp_tc_y).r - 0.0625;\n  yuv.y = texture2D(u_tex, interp_tc_u).r - 0.5;\n  yuv.z = texture2D(v_tex, interp_tc_v).r - 0.5;\n  gl_FragColor = vec4(mColorConversion * yuv, 1.0);\n}\n";
        private static final String VERTEX_SHADER = "varying vec2 interp_tc_y;\nvarying vec2 interp_tc_u;\nvarying vec2 interp_tc_v;\nattribute vec4 in_pos;\nattribute vec2 in_tc_y;\nattribute vec2 in_tc_u;\nattribute vec2 in_tc_v;\nvoid main() {\n  gl_Position = in_pos;\n  interp_tc_y = in_tc_y;\n  interp_tc_u = in_tc_u;\n  interp_tc_v = in_tc_v;\n}\n";
        private int colorMatrixLocation;
        private GlProgram program;
        private VideoDecoderOutputBuffer renderedOutputBuffer;
        private final GLSurfaceView surfaceView;
        private static final float[] kColorConversion601 = {1.164f, 1.164f, 1.164f, 0.0f, -0.392f, 2.017f, 1.596f, -0.813f, 0.0f};
        private static final float[] kColorConversion709 = {1.164f, 1.164f, 1.164f, 0.0f, -0.213f, 2.112f, 1.793f, -0.533f, 0.0f};
        private static final float[] kColorConversion2020 = {1.168f, 1.168f, 1.168f, 0.0f, -0.188f, 2.148f, 1.683f, -0.652f, 0.0f};
        private static final String[] TEXTURE_UNIFORMS = {"y_tex", "u_tex", "v_tex"};
        private static final FloatBuffer TEXTURE_VERTICES = GlUtil.createBuffer(new float[]{-1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f});
        private final int[] yuvTextures = new int[3];
        private final int[] texLocations = new int[3];
        private final int[] previousWidths = new int[3];
        private final int[] previousStrides = new int[3];
        private final AtomicReference<VideoDecoderOutputBuffer> pendingOutputBufferReference = new AtomicReference<>();
        private final FloatBuffer[] textureCoords = new FloatBuffer[3];

        public Renderer(GLSurfaceView surfaceView) {
            this.surfaceView = surfaceView;
            for (int i = 0; i < 3; i++) {
                int[] iArr = this.previousWidths;
                this.previousStrides[i] = -1;
                iArr[i] = -1;
            }
        }

        @Override // android.opengl.GLSurfaceView.Renderer
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            try {
                this.program = new GlProgram(VERTEX_SHADER, FRAGMENT_SHADER);
                int posLocation = this.program.getAttributeArrayLocationAndEnable("in_pos");
                GLES20.glVertexAttribPointer(posLocation, 2, 5126, false, 0, (Buffer) TEXTURE_VERTICES);
                this.texLocations[0] = this.program.getAttributeArrayLocationAndEnable("in_tc_y");
                this.texLocations[1] = this.program.getAttributeArrayLocationAndEnable("in_tc_u");
                this.texLocations[2] = this.program.getAttributeArrayLocationAndEnable("in_tc_v");
                this.colorMatrixLocation = this.program.getUniformLocation("mColorConversion");
                GlUtil.checkGlError();
                setupTextures();
                GlUtil.checkGlError();
            } catch (GlUtil.GlException e) {
                Log.e(VideoDecoderGLSurfaceView.TAG, "Failed to set up the textures and program", e);
            }
        }

        @Override // android.opengl.GLSurfaceView.Renderer
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // android.opengl.GLSurfaceView.Renderer
        public void onDrawFrame(GL10 unused) {
            float[] colorConversion;
            char c;
            VideoDecoderOutputBuffer pendingOutputBuffer = this.pendingOutputBufferReference.getAndSet(null);
            if (pendingOutputBuffer == null && this.renderedOutputBuffer == null) {
                return;
            }
            if (pendingOutputBuffer != null) {
                if (this.renderedOutputBuffer != null) {
                    this.renderedOutputBuffer.release();
                }
                this.renderedOutputBuffer = pendingOutputBuffer;
            }
            VideoDecoderOutputBuffer outputBuffer = (VideoDecoderOutputBuffer) Assertions.checkNotNull(this.renderedOutputBuffer);
            float[] colorConversion2 = kColorConversion709;
            switch (outputBuffer.colorspace) {
                case 1:
                    float[] colorConversion3 = kColorConversion601;
                    colorConversion = colorConversion3;
                    break;
                case 2:
                default:
                    colorConversion = colorConversion2;
                    break;
                case 3:
                    float[] colorConversion4 = kColorConversion2020;
                    colorConversion = colorConversion4;
                    break;
            }
            char c2 = 1;
            GLES20.glUniformMatrix3fv(this.colorMatrixLocation, 1, false, colorConversion, 0);
            int[] yuvStrides = (int[]) Assertions.checkNotNull(outputBuffer.yuvStrides);
            ByteBuffer[] yuvPlanes = (ByteBuffer[]) Assertions.checkNotNull(outputBuffer.yuvPlanes);
            int i = 0;
            while (i < 3) {
                int h = i == 0 ? outputBuffer.height : (outputBuffer.height + 1) / 2;
                GLES20.glActiveTexture(33984 + i);
                GLES20.glBindTexture(3553, this.yuvTextures[i]);
                GLES20.glPixelStorei(3317, 1);
                GLES20.glTexImage2D(3553, 0, 6409, yuvStrides[i], h, 0, 6409, 5121, yuvPlanes[i]);
                i++;
            }
            int i2 = (widths[0] + 1) / 2;
            int[] widths = {outputBuffer.width, i2, i2};
            int i3 = 0;
            while (i3 < 3) {
                if (this.previousWidths[i3] == widths[i3] && this.previousStrides[i3] == yuvStrides[i3]) {
                    c = c2;
                } else {
                    Assertions.checkState(yuvStrides[i3] != 0 ? c2 : 0);
                    float widthRatio = widths[i3] / yuvStrides[i3];
                    FloatBuffer[] floatBufferArr = this.textureCoords;
                    c = c2;
                    float[] fArr = new float[8];
                    fArr[0] = 0.0f;
                    fArr[c] = 0.0f;
                    fArr[2] = 0.0f;
                    fArr[3] = 1.0f;
                    fArr[4] = widthRatio;
                    fArr[5] = 0.0f;
                    fArr[6] = widthRatio;
                    fArr[7] = 1.0f;
                    floatBufferArr[i3] = GlUtil.createBuffer(fArr);
                    GLES20.glVertexAttribPointer(this.texLocations[i3], 2, 5126, false, 0, (Buffer) this.textureCoords[i3]);
                    this.previousWidths[i3] = widths[i3];
                    this.previousStrides[i3] = yuvStrides[i3];
                }
                i3++;
                c2 = c;
            }
            GLES20.glClear(16384);
            GLES20.glDrawArrays(5, 0, 4);
            try {
                GlUtil.checkGlError();
            } catch (GlUtil.GlException e) {
                Log.e(VideoDecoderGLSurfaceView.TAG, "Failed to draw a frame", e);
            }
        }

        public void setOutputBuffer(VideoDecoderOutputBuffer outputBuffer) {
            VideoDecoderOutputBuffer oldPendingOutputBuffer = this.pendingOutputBufferReference.getAndSet(outputBuffer);
            if (oldPendingOutputBuffer != null) {
                oldPendingOutputBuffer.release();
            }
            this.surfaceView.requestRender();
        }

        @RequiresNonNull({"program"})
        private void setupTextures() {
            try {
                GLES20.glGenTextures(3, this.yuvTextures, 0);
                for (int i = 0; i < 3; i++) {
                    GLES20.glUniform1i(this.program.getUniformLocation(TEXTURE_UNIFORMS[i]), i);
                    GLES20.glActiveTexture(33984 + i);
                    GlUtil.bindTexture(3553, this.yuvTextures[i], 9729);
                }
                GlUtil.checkGlError();
            } catch (GlUtil.GlException e) {
                Log.e(VideoDecoderGLSurfaceView.TAG, "Failed to set up the textures", e);
            }
        }
    }
}
