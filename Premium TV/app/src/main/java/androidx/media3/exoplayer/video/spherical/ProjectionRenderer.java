package androidx.media3.exoplayer.video.spherical;

import android.opengl.GLES20;
import android.util.Log;
import androidx.media3.common.util.GlProgram;
import androidx.media3.common.util.GlUtil;
import java.nio.Buffer;
import java.nio.FloatBuffer;

/* JADX INFO: loaded from: classes.dex */
final class ProjectionRenderer {
    private static final String FRAGMENT_SHADER = "// This is required since the texture data is GL_TEXTURE_EXTERNAL_OES.\n#extension GL_OES_EGL_image_external : require\nprecision mediump float;\n// Standard texture rendering shader.\nuniform samplerExternalOES uTexture;\nvarying vec2 vTexCoords;\nvoid main() {\n  gl_FragColor = texture2D(uTexture, vTexCoords);\n}\n";
    private static final String TAG = "ProjectionRenderer";
    private static final String VERTEX_SHADER = "uniform mat4 uMvpMatrix;\nuniform mat3 uTexMatrix;\nattribute vec4 aPosition;\nattribute vec2 aTexCoords;\nvarying vec2 vTexCoords;\n// Standard transformation.\nvoid main() {\n  gl_Position = uMvpMatrix * aPosition;\n  vTexCoords = (uTexMatrix * vec3(aTexCoords, 1)).xy;\n}\n";
    private MeshData leftMeshData;
    private int mvpMatrixHandle;
    private int positionHandle;
    private GlProgram program;
    private MeshData rightMeshData;
    private int stereoMode;
    private int texCoordsHandle;
    private int textureHandle;
    private int uTexMatrixHandle;
    private static final float[] TEX_MATRIX_WHOLE = {1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f};
    private static final float[] TEX_MATRIX_TOP = {1.0f, 0.0f, 0.0f, 0.0f, -0.5f, 0.0f, 0.0f, 0.5f, 1.0f};
    private static final float[] TEX_MATRIX_BOTTOM = {1.0f, 0.0f, 0.0f, 0.0f, -0.5f, 0.0f, 0.0f, 1.0f, 1.0f};
    private static final float[] TEX_MATRIX_LEFT = {0.5f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f};
    private static final float[] TEX_MATRIX_RIGHT = {0.5f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.5f, 1.0f, 1.0f};

    ProjectionRenderer() {
    }

    public static boolean isSupported(Projection projection) {
        Projection.Mesh leftMesh = projection.leftMesh;
        Projection.Mesh rightMesh = projection.rightMesh;
        return leftMesh.getSubMeshCount() == 1 && leftMesh.getSubMesh(0).textureId == 0 && rightMesh.getSubMeshCount() == 1 && rightMesh.getSubMesh(0).textureId == 0;
    }

    public void setProjection(Projection projection) {
        if (!isSupported(projection)) {
            return;
        }
        this.stereoMode = projection.stereoMode;
        this.leftMeshData = new MeshData(projection.leftMesh.getSubMesh(0));
        this.rightMeshData = projection.singleMesh ? this.leftMeshData : new MeshData(projection.rightMesh.getSubMesh(0));
    }

    public void init() {
        try {
            this.program = new GlProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            this.mvpMatrixHandle = this.program.getUniformLocation("uMvpMatrix");
            this.uTexMatrixHandle = this.program.getUniformLocation("uTexMatrix");
            this.positionHandle = this.program.getAttributeArrayLocationAndEnable("aPosition");
            this.texCoordsHandle = this.program.getAttributeArrayLocationAndEnable("aTexCoords");
            this.textureHandle = this.program.getUniformLocation("uTexture");
        } catch (GlUtil.GlException e) {
            Log.e(TAG, "Failed to initialize the program", e);
        }
    }

    public void draw(int textureId, float[] mvpMatrix, boolean rightEye) {
        float[] texMatrix;
        MeshData meshData = rightEye ? this.rightMeshData : this.leftMeshData;
        if (meshData == null) {
            return;
        }
        if (this.stereoMode == 1) {
            texMatrix = rightEye ? TEX_MATRIX_BOTTOM : TEX_MATRIX_TOP;
        } else if (this.stereoMode == 2) {
            texMatrix = rightEye ? TEX_MATRIX_RIGHT : TEX_MATRIX_LEFT;
        } else {
            float[] texMatrix2 = TEX_MATRIX_WHOLE;
            texMatrix = texMatrix2;
        }
        GLES20.glUniformMatrix3fv(this.uTexMatrixHandle, 1, false, texMatrix, 0);
        GLES20.glUniformMatrix4fv(this.mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, textureId);
        GLES20.glUniform1i(this.textureHandle, 0);
        try {
            GlUtil.checkGlError();
        } catch (GlUtil.GlException e) {
            Log.e(TAG, "Failed to bind uniforms", e);
        }
        GLES20.glVertexAttribPointer(this.positionHandle, 3, 5126, false, 12, (Buffer) meshData.vertexBuffer);
        try {
            GlUtil.checkGlError();
        } catch (GlUtil.GlException e2) {
            Log.e(TAG, "Failed to load position data", e2);
        }
        GLES20.glVertexAttribPointer(this.texCoordsHandle, 2, 5126, false, 8, (Buffer) meshData.textureBuffer);
        try {
            GlUtil.checkGlError();
        } catch (GlUtil.GlException e3) {
            Log.e(TAG, "Failed to load texture data", e3);
        }
        GLES20.glDrawArrays(meshData.drawMode, 0, meshData.vertexCount);
        try {
            GlUtil.checkGlError();
        } catch (GlUtil.GlException e4) {
            Log.e(TAG, "Failed to render", e4);
        }
    }

    public void shutdown() {
        if (this.program != null) {
            try {
                this.program.delete();
            } catch (GlUtil.GlException e) {
                Log.e(TAG, "Failed to delete the shader program", e);
            }
        }
    }

    private static class MeshData {
        private final int drawMode;
        private final FloatBuffer textureBuffer;
        private final FloatBuffer vertexBuffer;
        private final int vertexCount;

        public MeshData(Projection.SubMesh subMesh) {
            this.vertexCount = subMesh.getVertexCount();
            this.vertexBuffer = GlUtil.createBuffer(subMesh.vertices);
            this.textureBuffer = GlUtil.createBuffer(subMesh.textureCoords);
            switch (subMesh.mode) {
                case 1:
                    this.drawMode = 5;
                    break;
                case 2:
                    this.drawMode = 6;
                    break;
                default:
                    this.drawMode = 4;
                    break;
            }
        }
    }
}
