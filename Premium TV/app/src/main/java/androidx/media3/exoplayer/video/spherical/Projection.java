package androidx.media3.exoplayer.video.spherical;

import androidx.media3.common.util.Assertions;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
final class Projection {
    public static final int DRAW_MODE_TRIANGLES = 0;
    public static final int DRAW_MODE_TRIANGLES_FAN = 2;
    public static final int DRAW_MODE_TRIANGLES_STRIP = 1;
    public static final int POSITION_COORDS_PER_VERTEX = 3;
    public static final int TEXTURE_COORDS_PER_VERTEX = 2;
    public final Mesh leftMesh;
    public final Mesh rightMesh;
    public final boolean singleMesh;
    public final int stereoMode;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface DrawMode {
    }

    public static Projection createEquirectangular(int stereoMode) {
        return createEquirectangular(50.0f, 36, 72, 180.0f, 360.0f, stereoMode);
    }

    public static Projection createEquirectangular(float radius, int latitudes, int longitudes, float verticalFovDegrees, float horizontalFovDegrees, int stereoMode) {
        int i = latitudes;
        Assertions.checkArgument(radius > 0.0f);
        Assertions.checkArgument(i >= 1);
        Assertions.checkArgument(longitudes >= 1);
        Assertions.checkArgument(verticalFovDegrees > 0.0f && verticalFovDegrees <= 180.0f);
        Assertions.checkArgument(horizontalFovDegrees > 0.0f && horizontalFovDegrees <= 360.0f);
        float verticalFovRads = (float) Math.toRadians(verticalFovDegrees);
        float horizontalFovRads = (float) Math.toRadians(horizontalFovDegrees);
        float quadHeightRads = verticalFovRads / i;
        float quadWidthRads = horizontalFovRads / longitudes;
        int vertexCount = (((longitudes + 1) * 2) + 2) * i;
        float[] vertexData = new float[vertexCount * 3];
        float[] textureData = new float[vertexCount * 2];
        int vOffset = 0;
        int tOffset = 0;
        int j = 0;
        while (j < i) {
            float phiLow = (j * quadHeightRads) - (verticalFovRads / 2.0f);
            float phiHigh = ((j + 1) * quadHeightRads) - (verticalFovRads / 2.0f);
            for (int i2 = 0; i2 < longitudes + 1; i2++) {
                int k = 0;
                while (k < 2) {
                    float phi = k == 0 ? phiLow : phiHigh;
                    float verticalFovRads2 = verticalFovRads;
                    float theta = ((i2 * quadWidthRads) + 3.1415927f) - (horizontalFovRads / 2.0f);
                    int vOffset2 = vOffset + 1;
                    float horizontalFovRads2 = horizontalFovRads;
                    int j2 = j;
                    vertexData[vOffset] = -((float) (Math.cos(phi) * Math.sin(theta) * ((double) radius)));
                    int vOffset3 = vOffset2 + 1;
                    vertexData[vOffset2] = (float) (Math.sin(phi) * ((double) radius));
                    int vOffset4 = vOffset3 + 1;
                    float phiLow2 = phiLow;
                    vertexData[vOffset3] = (float) (Math.cos(phi) * Math.cos(theta) * ((double) radius));
                    int tOffset2 = tOffset + 1;
                    textureData[tOffset] = (i2 * quadWidthRads) / horizontalFovRads2;
                    int tOffset3 = tOffset2 + 1;
                    textureData[tOffset2] = ((j2 + k) * quadHeightRads) / verticalFovRads2;
                    if ((i2 == 0 && k == 0) || (i2 == longitudes && k == 1)) {
                        System.arraycopy(vertexData, vOffset4 - 3, vertexData, vOffset4, 3);
                        vOffset4 += 3;
                        System.arraycopy(textureData, tOffset3 - 2, textureData, tOffset3, 2);
                        tOffset3 += 2;
                    }
                    tOffset = tOffset3;
                    k++;
                    vOffset = vOffset4;
                    verticalFovRads = verticalFovRads2;
                    phiLow = phiLow2;
                    horizontalFovRads = horizontalFovRads2;
                    j = j2;
                }
            }
            j++;
            i = latitudes;
        }
        SubMesh subMesh = new SubMesh(0, vertexData, textureData, 1);
        return new Projection(new Mesh(subMesh), stereoMode);
    }

    public Projection(Mesh mesh, int stereoMode) {
        this(mesh, mesh, stereoMode);
    }

    public Projection(Mesh leftMesh, Mesh rightMesh, int stereoMode) {
        this.leftMesh = leftMesh;
        this.rightMesh = rightMesh;
        this.stereoMode = stereoMode;
        this.singleMesh = leftMesh == rightMesh;
    }

    public static final class SubMesh {
        public static final int VIDEO_TEXTURE_ID = 0;
        public final int mode;
        public final float[] textureCoords;
        public final int textureId;
        public final float[] vertices;

        public SubMesh(int textureId, float[] vertices, float[] textureCoords, int mode) {
            this.textureId = textureId;
            Assertions.checkArgument(((long) vertices.length) * 2 == ((long) textureCoords.length) * 3);
            this.vertices = vertices;
            this.textureCoords = textureCoords;
            this.mode = mode;
        }

        public int getVertexCount() {
            return this.vertices.length / 3;
        }
    }

    public static final class Mesh {
        private final SubMesh[] subMeshes;

        public Mesh(SubMesh... subMeshes) {
            this.subMeshes = subMeshes;
        }

        public int getSubMeshCount() {
            return this.subMeshes.length;
        }

        public SubMesh getSubMesh(int index) {
            return this.subMeshes[index];
        }
    }
}
