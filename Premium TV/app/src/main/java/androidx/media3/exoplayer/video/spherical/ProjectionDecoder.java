package androidx.media3.exoplayer.video.spherical;

import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import java.util.ArrayList;
import java.util.zip.Inflater;

/* JADX INFO: loaded from: classes.dex */
final class ProjectionDecoder {
    private static final int MAX_COORDINATE_COUNT = 10000;
    private static final int MAX_TRIANGLE_INDICES = 128000;
    private static final int MAX_VERTEX_COUNT = 32000;
    private static final int TYPE_DFL8 = 1684433976;
    private static final int TYPE_MESH = 1835365224;
    private static final int TYPE_MSHP = 1836279920;
    private static final int TYPE_PROJ = 1886547818;
    private static final int TYPE_RAW = 1918990112;
    private static final int TYPE_YTMP = 2037673328;

    private ProjectionDecoder() {
    }

    public static Projection decode(byte[] projectionData, int stereoMode) {
        ParsableByteArray input = new ParsableByteArray(projectionData);
        ArrayList<Projection.Mesh> meshes = null;
        try {
            meshes = isProj(input) ? parseProj(input) : parseMshp(input);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        if (meshes == null) {
            return null;
        }
        switch (meshes.size()) {
            case 1:
                return new Projection(meshes.get(0), stereoMode);
            case 2:
                return new Projection(meshes.get(0), meshes.get(1), stereoMode);
            default:
                return null;
        }
    }

    private static boolean isProj(ParsableByteArray input) {
        input.skipBytes(4);
        int type = input.readInt();
        input.setPosition(0);
        return type == 1886547818;
    }

    private static ArrayList<Projection.Mesh> parseProj(ParsableByteArray input) {
        int childEnd;
        input.skipBytes(8);
        int position = input.getPosition();
        int limit = input.limit();
        while (position < limit && (childEnd = input.readInt() + position) > position && childEnd <= limit) {
            int childAtomType = input.readInt();
            if (childAtomType == TYPE_YTMP || childAtomType == TYPE_MSHP) {
                input.setLimit(childEnd);
                return parseMshp(input);
            }
            position = childEnd;
            input.setPosition(position);
        }
        return null;
    }

    private static ArrayList<Projection.Mesh> parseMshp(ParsableByteArray input) {
        int version = input.readUnsignedByte();
        if (version != 0) {
            return null;
        }
        input.skipBytes(7);
        int encoding = input.readInt();
        if (encoding == TYPE_DFL8) {
            ParsableByteArray output = new ParsableByteArray();
            Inflater inflater = new Inflater(true);
            try {
                if (!Util.inflate(input, output, inflater)) {
                    inflater.end();
                    return null;
                }
                inflater.end();
                input = output;
            } catch (Throwable th) {
                inflater.end();
                throw th;
            }
        } else if (encoding != TYPE_RAW) {
            return null;
        }
        return parseRawMshpData(input);
    }

    private static ArrayList<Projection.Mesh> parseRawMshpData(ParsableByteArray input) {
        ArrayList<Projection.Mesh> meshes = new ArrayList<>();
        int position = input.getPosition();
        int limit = input.limit();
        while (position < limit) {
            int childEnd = input.readInt() + position;
            if (childEnd <= position || childEnd > limit) {
                return null;
            }
            int childAtomType = input.readInt();
            if (childAtomType == TYPE_MESH) {
                Projection.Mesh mesh = parseMesh(input);
                if (mesh == null) {
                    return null;
                }
                meshes.add(mesh);
            }
            position = childEnd;
            input.setPosition(position);
        }
        return meshes;
    }

    private static Projection.Mesh parseMesh(ParsableByteArray input) {
        int coordinateCount = input.readInt();
        Projection.Mesh mesh = null;
        if (coordinateCount > 10000) {
            return null;
        }
        float[] coordinates = new float[coordinateCount];
        for (int coordinate = 0; coordinate < coordinateCount; coordinate++) {
            coordinates[coordinate] = input.readFloat();
        }
        int vertexCount = input.readInt();
        if (vertexCount > MAX_VERTEX_COUNT) {
            return null;
        }
        double d = 2.0d;
        double log2 = Math.log(2.0d);
        int coordinateCountSizeBits = (int) Math.ceil(Math.log(((double) coordinateCount) * 2.0d) / log2);
        ParsableBitArray bitInput = new ParsableBitArray(input.getData());
        int i = 8;
        bitInput.setPosition(input.getPosition() * 8);
        float[] vertices = new float[vertexCount * 5];
        int[] coordinateIndices = new int[5];
        int vertexIndex = 0;
        int vertex = 0;
        while (vertex < vertexCount) {
            Projection.Mesh mesh2 = mesh;
            int i2 = 0;
            while (i2 < 5) {
                double d2 = d;
                int coordinateIndex = coordinateIndices[i2] + decodeZigZag(bitInput.readBits(coordinateCountSizeBits));
                if (coordinateIndex >= coordinateCount || coordinateIndex < 0) {
                    return mesh2;
                }
                vertices[vertexIndex] = coordinates[coordinateIndex];
                coordinateIndices[i2] = coordinateIndex;
                i2++;
                vertexIndex++;
                d = d2;
            }
            vertex++;
            mesh = mesh2;
        }
        Projection.Mesh mesh3 = mesh;
        double d3 = d;
        bitInput.setPosition((bitInput.getPosition() + 7) & (-8));
        int i3 = 32;
        int subMeshCount = bitInput.readBits(32);
        Projection.SubMesh[] subMeshes = new Projection.SubMesh[subMeshCount];
        int i4 = 0;
        while (i4 < subMeshCount) {
            int textureId = bitInput.readBits(i);
            int coordinateCount2 = coordinateCount;
            int drawMode = bitInput.readBits(i);
            int triangleIndexCount = bitInput.readBits(i3);
            if (triangleIndexCount > MAX_TRIANGLE_INDICES) {
                return mesh3;
            }
            float[] coordinates2 = coordinates;
            int vertexCountSizeBits = (int) Math.ceil(Math.log(((double) vertexCount) * d3) / log2);
            int index = 0;
            int index2 = triangleIndexCount * 3;
            float[] triangleVertices = new float[index2];
            int subMeshCount2 = subMeshCount;
            float[] textureCoords = new float[triangleIndexCount * 2];
            double log3 = log2;
            int counter = 0;
            while (counter < triangleIndexCount) {
                int index3 = index + decodeZigZag(bitInput.readBits(vertexCountSizeBits));
                if (index3 < 0 || index3 >= vertexCount) {
                    return mesh3;
                }
                triangleVertices[counter * 3] = vertices[index3 * 5];
                triangleVertices[(counter * 3) + 1] = vertices[(index3 * 5) + 1];
                triangleVertices[(counter * 3) + 2] = vertices[(index3 * 5) + 2];
                textureCoords[counter * 2] = vertices[(index3 * 5) + 3];
                textureCoords[(counter * 2) + 1] = vertices[(index3 * 5) + 4];
                counter++;
                index = index3;
            }
            subMeshes[i4] = new Projection.SubMesh(textureId, triangleVertices, textureCoords, drawMode);
            i4++;
            coordinateCount = coordinateCount2;
            coordinates = coordinates2;
            subMeshCount = subMeshCount2;
            log2 = log3;
            i3 = 32;
            i = 8;
        }
        return new Projection.Mesh(subMeshes);
    }

    private static int decodeZigZag(int n) {
        return (n >> 1) ^ (-(n & 1));
    }
}
