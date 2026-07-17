package androidx.media3.common.util;

import android.content.Context;
import android.opengl.GLES20;
import java.io.IOException;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class GlProgram {
    private static final int GL_SAMPLER_EXTERNAL_2D_Y2Y_EXT = 35815;
    private final Map<String, Attribute> attributeByName;
    private final Attribute[] attributes;
    private boolean externalTexturesRequireNearestSampling;
    private final int programId;
    private final Map<String, Uniform> uniformByName;
    private final Uniform[] uniforms;

    public GlProgram(Context context, String vertexShaderFilePath, String fragmentShaderFilePath) throws IOException, GlUtil.GlException {
        this(Util.loadAsset(context, vertexShaderFilePath), Util.loadAsset(context, fragmentShaderFilePath));
    }

    public GlProgram(String vertexShaderGlsl, String fragmentShaderGlsl) throws GlUtil.GlException {
        this.programId = GLES20.glCreateProgram();
        GlUtil.checkGlError();
        addShader(this.programId, 35633, vertexShaderGlsl);
        addShader(this.programId, 35632, fragmentShaderGlsl);
        GLES20.glLinkProgram(this.programId);
        int[] linkStatus = {0};
        GLES20.glGetProgramiv(this.programId, 35714, linkStatus, 0);
        GlUtil.checkGlException(linkStatus[0] == 1, "Unable to link shader program: \n" + GLES20.glGetProgramInfoLog(this.programId));
        GLES20.glUseProgram(this.programId);
        this.attributeByName = new HashMap();
        int[] attributeCount = new int[1];
        GLES20.glGetProgramiv(this.programId, 35721, attributeCount, 0);
        this.attributes = new Attribute[attributeCount[0]];
        for (int i = 0; i < attributeCount[0]; i++) {
            Attribute attribute = Attribute.create(this.programId, i);
            this.attributes[i] = attribute;
            this.attributeByName.put(attribute.name, attribute);
        }
        this.uniformByName = new HashMap();
        int[] uniformCount = new int[1];
        GLES20.glGetProgramiv(this.programId, 35718, uniformCount, 0);
        this.uniforms = new Uniform[uniformCount[0]];
        for (int i2 = 0; i2 < uniformCount[0]; i2++) {
            Uniform uniform = Uniform.create(this.programId, i2);
            this.uniforms[i2] = uniform;
            this.uniformByName.put(uniform.name, uniform);
        }
        GlUtil.checkGlError();
    }

    private static void addShader(int programId, int type, String glsl) throws GlUtil.GlException {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, glsl);
        GLES20.glCompileShader(shader);
        int[] result = {0};
        GLES20.glGetShaderiv(shader, 35713, result, 0);
        GlUtil.checkGlException(result[0] == 1, GLES20.glGetShaderInfoLog(shader) + ", source: \n" + glsl);
        GLES20.glAttachShader(programId, shader);
        GLES20.glDeleteShader(shader);
        GlUtil.checkGlError();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getAttributeLocation(int programId, String attributeName) {
        return GLES20.glGetAttribLocation(programId, attributeName);
    }

    private int getAttributeLocation(String attributeName) {
        return getAttributeLocation(this.programId, attributeName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getUniformLocation(int programId, String uniformName) {
        return GLES20.glGetUniformLocation(programId, uniformName);
    }

    public int getUniformLocation(String uniformName) {
        return getUniformLocation(this.programId, uniformName);
    }

    public void use() throws GlUtil.GlException {
        GLES20.glUseProgram(this.programId);
        GlUtil.checkGlError();
    }

    public void delete() throws GlUtil.GlException {
        GLES20.glDeleteProgram(this.programId);
        GlUtil.checkGlError();
    }

    public int getAttributeArrayLocationAndEnable(String attributeName) throws GlUtil.GlException {
        int location = getAttributeLocation(attributeName);
        GLES20.glEnableVertexAttribArray(location);
        GlUtil.checkGlError();
        return location;
    }

    public void setBufferAttribute(String name, float[] values, int size) {
        ((Attribute) Assertions.checkNotNull(this.attributeByName.get(name))).setBuffer(values, size);
    }

    public void setSamplerTexIdUniform(String name, int texId, int texUnitIndex) {
        ((Uniform) Assertions.checkNotNull(this.uniformByName.get(name))).setSamplerTexId(texId, texUnitIndex);
    }

    public void setIntUniform(String name, int value) {
        ((Uniform) Assertions.checkNotNull(this.uniformByName.get(name))).setInt(value);
    }

    public void setIntsUniform(String name, int[] value) {
        ((Uniform) Assertions.checkNotNull(this.uniformByName.get(name))).setInts(value);
    }

    public void setFloatUniform(String name, float value) {
        ((Uniform) Assertions.checkNotNull(this.uniformByName.get(name))).setFloat(value);
    }

    public void setFloatsUniform(String name, float[] value) {
        ((Uniform) Assertions.checkNotNull(this.uniformByName.get(name))).setFloats(value);
    }

    public void setFloatsUniformIfPresent(String name, float[] value) {
        Uniform uniform = this.uniformByName.get(name);
        if (uniform == null) {
            return;
        }
        uniform.setFloats(value);
    }

    public void bindAttributesAndUniforms() throws GlUtil.GlException {
        for (Attribute attribute : this.attributes) {
            attribute.bind();
        }
        for (Uniform uniform : this.uniforms) {
            uniform.bind(this.externalTexturesRequireNearestSampling);
        }
    }

    public void setExternalTexturesRequireNearestSampling(boolean externalTexturesRequireNearestSampling) {
        this.externalTexturesRequireNearestSampling = externalTexturesRequireNearestSampling;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getCStringLength(byte[] cString) {
        for (int i = 0; i < cString.length; i++) {
            if (cString[i] == 0) {
                return i;
            }
        }
        int i2 = cString.length;
        return i2;
    }

    private static final class Attribute {
        private Buffer buffer;
        private final int location;
        public final String name;
        private int size;

        public static Attribute create(int programId, int index) {
            int[] attributeNameMaxLength = new int[1];
            GLES20.glGetProgramiv(programId, 35722, attributeNameMaxLength, 0);
            byte[] nameBytes = new byte[attributeNameMaxLength[0]];
            GLES20.glGetActiveAttrib(programId, index, attributeNameMaxLength[0], new int[1], 0, new int[1], 0, new int[1], 0, nameBytes, 0);
            String name = new String(nameBytes, 0, GlProgram.getCStringLength(nameBytes));
            int location = GlProgram.getAttributeLocation(programId, name);
            return new Attribute(name, location);
        }

        private Attribute(String name, int location) {
            this.name = name;
            this.location = location;
        }

        public void setBuffer(float[] buffer, int size) {
            this.buffer = GlUtil.createBuffer(buffer);
            this.size = size;
        }

        public void bind() throws GlUtil.GlException {
            Buffer buffer = (Buffer) Assertions.checkNotNull(this.buffer, "call setBuffer before bind");
            GLES20.glBindBuffer(34962, 0);
            GLES20.glVertexAttribPointer(this.location, this.size, 5126, false, 0, buffer);
            GLES20.glEnableVertexAttribArray(this.location);
            GlUtil.checkGlError();
        }
    }

    private static final class Uniform {
        private final float[] floatValue = new float[16];
        private final int[] intValue = new int[4];
        private final int location;
        public final String name;
        private int texIdValue;
        private int texUnitIndex;
        private final int type;

        public static Uniform create(int programId, int index) {
            int[] length = new int[1];
            GLES20.glGetProgramiv(programId, 35719, length, 0);
            int[] type = new int[1];
            byte[] nameBytes = new byte[length[0]];
            GLES20.glGetActiveUniform(programId, index, length[0], new int[1], 0, new int[1], 0, type, 0, nameBytes, 0);
            String name = new String(nameBytes, 0, GlProgram.getCStringLength(nameBytes));
            int location = GlProgram.getUniformLocation(programId, name);
            return new Uniform(name, location, type[0]);
        }

        private Uniform(String name, int location, int type) {
            this.name = name;
            this.location = location;
            this.type = type;
        }

        public void setSamplerTexId(int texId, int texUnitIndex) {
            this.texIdValue = texId;
            this.texUnitIndex = texUnitIndex;
        }

        public void setInt(int value) {
            this.intValue[0] = value;
        }

        public void setInts(int[] value) {
            System.arraycopy(value, 0, this.intValue, 0, value.length);
        }

        public void setFloat(float value) {
            this.floatValue[0] = value;
        }

        public void setFloats(float[] value) {
            System.arraycopy(value, 0, this.floatValue, 0, value.length);
        }

        public void bind(boolean externalTexturesRequireNearestSampling) throws GlUtil.GlException {
            int i;
            int i2;
            switch (this.type) {
                case 5124:
                    GLES20.glUniform1iv(this.location, 1, this.intValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 5126:
                    GLES20.glUniform1fv(this.location, 1, this.floatValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 35664:
                    GLES20.glUniform2fv(this.location, 1, this.floatValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 35665:
                    GLES20.glUniform3fv(this.location, 1, this.floatValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 35666:
                    GLES20.glUniform4fv(this.location, 1, this.floatValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 35667:
                    GLES20.glUniform2iv(this.location, 1, this.intValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 35668:
                    GLES20.glUniform3iv(this.location, 1, this.intValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 35669:
                    GLES20.glUniform4iv(this.location, 1, this.intValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 35675:
                    GLES20.glUniformMatrix3fv(this.location, 1, false, this.floatValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 35676:
                    GLES20.glUniformMatrix4fv(this.location, 1, false, this.floatValue, 0);
                    GlUtil.checkGlError();
                    return;
                case 35678:
                case GlProgram.GL_SAMPLER_EXTERNAL_2D_Y2Y_EXT /* 35815 */:
                case 36198:
                    if (this.texIdValue == 0) {
                        throw new IllegalStateException("No call to setSamplerTexId() before bind.");
                    }
                    GLES20.glActiveTexture(this.texUnitIndex + 33984);
                    GlUtil.checkGlError();
                    if (this.type == 35678) {
                        i = 3553;
                    } else {
                        i = 36197;
                    }
                    int i3 = this.texIdValue;
                    if (this.type == 35678 && !externalTexturesRequireNearestSampling) {
                        i2 = 9729;
                    } else {
                        i2 = 9728;
                    }
                    GlUtil.bindTexture(i, i3, i2);
                    GLES20.glUniform1i(this.location, this.texUnitIndex);
                    GlUtil.checkGlError();
                    return;
                default:
                    throw new IllegalStateException("Unexpected uniform type: " + this.type);
            }
        }
    }
}
