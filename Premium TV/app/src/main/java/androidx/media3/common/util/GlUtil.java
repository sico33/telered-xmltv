package androidx.media3.common.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class GlUtil {
    private static final String EXTENSION_COLORSPACE_BT2020_HLG = "EGL_EXT_gl_colorspace_bt2020_hlg";
    private static final String EXTENSION_COLORSPACE_BT2020_PQ = "EGL_EXT_gl_colorspace_bt2020_pq";
    private static final String EXTENSION_PROTECTED_CONTENT = "EGL_EXT_protected_content";
    private static final String EXTENSION_SURFACELESS_CONTEXT = "EGL_KHR_surfaceless_context";
    private static final String EXTENSION_YUV_TARGET = "GL_EXT_YUV_target";
    private static final long GL_FENCE_SYNC_FAILED = 0;
    public static final int HOMOGENEOUS_COORDINATE_VECTOR_SIZE = 4;
    public static final float LENGTH_NDC = 2.0f;
    public static final int[] EGL_CONFIG_ATTRIBUTES_RGBA_8888 = {12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 8, 12325, 0, 12326, 0, 12344};
    public static final int[] EGL_CONFIG_ATTRIBUTES_RGBA_1010102 = {12352, 4, 12324, 10, 12323, 10, 12322, 10, 12321, 2, 12325, 0, 12326, 0, 12344};
    private static final int EGL_GL_COLORSPACE_KHR = 12445;
    private static final int EGL_GL_COLORSPACE_BT2020_PQ_EXT = 13120;
    private static final int[] EGL_WINDOW_SURFACE_ATTRIBUTES_BT2020_PQ = {EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_PQ_EXT, 12344, 12344};
    private static final int EGL_GL_COLORSPACE_BT2020_HLG_EXT = 13632;
    private static final int[] EGL_WINDOW_SURFACE_ATTRIBUTES_BT2020_HLG = {EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_HLG_EXT, 12344, 12344};
    private static final int[] EGL_WINDOW_SURFACE_ATTRIBUTES_NONE = {12344};

    public static final class GlException extends Exception {
        public GlException(String message) {
            super(message);
        }
    }

    private GlUtil() {
    }

    public static float[] getNormalizedCoordinateBounds() {
        return new float[]{-1.0f, -1.0f, 0.0f, 1.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f};
    }

    public static float[] getTextureCoordinateBounds() {
        return new float[]{0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f};
    }

    public static float[] create4x4IdentityMatrix() {
        float[] matrix = new float[16];
        setToIdentity(matrix);
        return matrix;
    }

    public static void setToIdentity(float[] matrix) {
        Matrix.setIdentityM(matrix, 0);
    }

    public static float[] createVertexBuffer(List<float[]> vertexList) {
        float[] vertexBuffer = new float[vertexList.size() * 4];
        for (int i = 0; i < vertexList.size(); i++) {
            System.arraycopy(vertexList.get(i), 0, vertexBuffer, i * 4, 4);
        }
        return vertexBuffer;
    }

    public static boolean isProtectedContentExtensionSupported(Context context) {
        if (Util.SDK_INT < 24) {
            return false;
        }
        if (Util.SDK_INT < 26 && ("samsung".equals(Util.MANUFACTURER) || "XT1650".equals(Util.MODEL))) {
            return false;
        }
        if (Util.SDK_INT >= 26 || context.getPackageManager().hasSystemFeature("android.hardware.vr.high_performance")) {
            return isExtensionSupported(EXTENSION_PROTECTED_CONTENT);
        }
        return false;
    }

    public static boolean isSurfacelessContextExtensionSupported() {
        return isExtensionSupported(EXTENSION_SURFACELESS_CONTEXT);
    }

    public static boolean isYuvTargetExtensionSupported() {
        String glExtensions;
        if (Util.areEqual(EGL14.eglGetCurrentContext(), EGL14.EGL_NO_CONTEXT)) {
            try {
                EGLDisplay eglDisplay = getDefaultEglDisplay();
                EGLContext eglContext = createEglContext(eglDisplay);
                createFocusedPlaceholderEglSurface(eglContext, eglDisplay);
                glExtensions = GLES20.glGetString(7939);
                destroyEglContext(eglDisplay, eglContext);
            } catch (GlException e) {
                return false;
            }
        } else {
            glExtensions = GLES20.glGetString(7939);
        }
        return glExtensions != null && glExtensions.contains(EXTENSION_YUV_TARGET);
    }

    public static boolean isBt2020PqExtensionSupported() {
        return Util.SDK_INT >= 33 && isExtensionSupported(EXTENSION_COLORSPACE_BT2020_PQ);
    }

    public static boolean isBt2020HlgExtensionSupported() {
        return isExtensionSupported(EXTENSION_COLORSPACE_BT2020_HLG);
    }

    public static EGLDisplay getDefaultEglDisplay() throws GlException {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(0);
        checkGlException(!eglDisplay.equals(EGL14.EGL_NO_DISPLAY), "No EGL display.");
        checkGlException(EGL14.eglInitialize(eglDisplay, new int[1], 0, new int[1], 0), "Error in eglInitialize.");
        checkGlError();
        return eglDisplay;
    }

    public static EGLContext createEglContext(EGLDisplay eglDisplay) throws GlException {
        return createEglContext(EGL14.EGL_NO_CONTEXT, eglDisplay, 2, EGL_CONFIG_ATTRIBUTES_RGBA_8888);
    }

    public static EGLContext createEglContext(EGLContext sharedContext, EGLDisplay eglDisplay, int openGlVersion, int[] configAttributes) throws GlException {
        boolean z = true;
        Assertions.checkArgument(Arrays.equals(configAttributes, EGL_CONFIG_ATTRIBUTES_RGBA_8888) || Arrays.equals(configAttributes, EGL_CONFIG_ATTRIBUTES_RGBA_1010102));
        if (openGlVersion != 2 && openGlVersion != 3) {
            z = false;
        }
        Assertions.checkArgument(z);
        int[] contextAttributes = {12440, openGlVersion, 12344};
        EGLContext eglContext = EGL14.eglCreateContext(eglDisplay, getEglConfig(eglDisplay, configAttributes), sharedContext, contextAttributes, 0);
        if (eglContext == null) {
            EGL14.eglTerminate(eglDisplay);
            throw new GlException("eglCreateContext() failed to create a valid context. The device may not support EGL version " + openGlVersion);
        }
        checkGlError();
        return eglContext;
    }

    public static EGLSurface createEglSurface(EGLDisplay eglDisplay, Object surface, int colorTransfer, boolean isEncoderInputSurface) throws GlException {
        int[] configAttributes;
        int[] windowAttributes;
        if (colorTransfer == 3 || colorTransfer == 10) {
            configAttributes = EGL_CONFIG_ATTRIBUTES_RGBA_8888;
            windowAttributes = EGL_WINDOW_SURFACE_ATTRIBUTES_NONE;
        } else if (colorTransfer == 7 || colorTransfer == 6) {
            configAttributes = EGL_CONFIG_ATTRIBUTES_RGBA_1010102;
            if (isEncoderInputSurface) {
                windowAttributes = EGL_WINDOW_SURFACE_ATTRIBUTES_NONE;
            } else if (colorTransfer == 6) {
                if (!isBt2020PqExtensionSupported()) {
                    throw new GlException("BT.2020 PQ OpenGL output isn't supported.");
                }
                windowAttributes = EGL_WINDOW_SURFACE_ATTRIBUTES_BT2020_PQ;
            } else {
                if (!isBt2020HlgExtensionSupported()) {
                    throw new GlException("BT.2020 HLG OpenGL output isn't supported.");
                }
                windowAttributes = EGL_WINDOW_SURFACE_ATTRIBUTES_BT2020_HLG;
            }
        } else {
            throw new IllegalArgumentException("Unsupported color transfer: " + colorTransfer);
        }
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, getEglConfig(eglDisplay, configAttributes), surface, windowAttributes, 0);
        checkEglException("Error creating a new EGL surface");
        return eglSurface;
    }

    private static EGLSurface createPbufferSurface(EGLDisplay eglDisplay, int width, int height, int[] configAttributes) throws GlException {
        int[] pbufferAttributes = {12375, width, 12374, height, 12344};
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, getEglConfig(eglDisplay, configAttributes), pbufferAttributes, 0);
        checkEglException("Error creating a new EGL Pbuffer surface");
        return eglSurface;
    }

    public static EGLSurface createFocusedPlaceholderEglSurface(EGLContext eglContext, EGLDisplay eglDisplay) throws GlException {
        EGLSurface eglSurface;
        int[] configAttributes = EGL_CONFIG_ATTRIBUTES_RGBA_8888;
        if (isSurfacelessContextExtensionSupported()) {
            eglSurface = EGL14.EGL_NO_SURFACE;
        } else {
            eglSurface = createPbufferSurface(eglDisplay, 1, 1, configAttributes);
        }
        focusEglSurface(eglDisplay, eglContext, eglSurface, 1, 1);
        return eglSurface;
    }

    public static long getContextMajorVersion() throws GlException {
        int[] currentEglContextVersion = new int[1];
        EGL14.eglQueryContext(EGL14.eglGetDisplay(0), EGL14.eglGetCurrentContext(), 12440, currentEglContextVersion, 0);
        checkGlError();
        return currentEglContextVersion[0];
    }

    public static long createGlSyncFence() throws GlException {
        if (getContextMajorVersion() >= 3) {
            long syncObject = GLES30.glFenceSync(37143, 0);
            checkGlError();
            GLES20.glFlush();
            checkGlError();
            return syncObject;
        }
        return 0L;
    }

    public static void deleteSyncObject(long syncObject) throws GlException {
        deleteSyncObjectQuietly(syncObject);
        checkGlError();
    }

    public static void deleteSyncObjectQuietly(long syncObject) {
        GLES30.glDeleteSync(syncObject);
    }

    public static void awaitSyncObject(long syncObject) throws GlException {
        if (syncObject == 0) {
            GLES20.glFinish();
        } else {
            GLES30.glWaitSync(syncObject, 0, -1L);
            checkGlError();
        }
    }

    public static EGLContext getCurrentContext() {
        return EGL14.eglGetCurrentContext();
    }

    public static void checkGlError() throws GlException {
        StringBuilder errorMessageBuilder = new StringBuilder();
        boolean foundError = false;
        while (true) {
            int error = GLES20.glGetError();
            if (error == 0) {
                break;
            }
            if (foundError) {
                errorMessageBuilder.append('\n');
            }
            String errorString = GLU.gluErrorString(error);
            if (errorString == null) {
                errorString = "error code: 0x" + Integer.toHexString(error);
            }
            errorMessageBuilder.append("glError: ").append(errorString);
            foundError = true;
        }
        if (foundError) {
            throw new GlException(errorMessageBuilder.toString());
        }
    }

    private static void assertValidTextureSize(int width, int height) throws GlException {
        int[] maxTextureSizeBuffer = new int[1];
        GLES20.glGetIntegerv(3379, maxTextureSizeBuffer, 0);
        int maxTextureSize = maxTextureSizeBuffer[0];
        Assertions.checkState(maxTextureSize > 0, "Create a OpenGL context first or run the GL methods on an OpenGL thread.");
        if (width < 0 || height < 0) {
            throw new GlException("width or height is less than 0");
        }
        if (width > maxTextureSize || height > maxTextureSize) {
            throw new GlException("width or height is greater than GL_MAX_TEXTURE_SIZE " + maxTextureSize);
        }
    }

    public static void clearFocusedBuffers() throws GlException {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClearDepthf(1.0f);
        GLES20.glClear(16640);
        checkGlError();
    }

    public static void focusEglSurface(EGLDisplay eglDisplay, EGLContext eglContext, EGLSurface eglSurface, int width, int height) throws GlException {
        focusRenderTarget(eglDisplay, eglContext, eglSurface, 0, width, height);
    }

    public static void focusFramebuffer(EGLDisplay eglDisplay, EGLContext eglContext, EGLSurface eglSurface, int framebuffer, int width, int height) throws GlException {
        focusRenderTarget(eglDisplay, eglContext, eglSurface, framebuffer, width, height);
    }

    public static void focusFramebufferUsingCurrentContext(int framebuffer, int width, int height) throws GlException {
        int[] boundFramebuffer = new int[1];
        GLES20.glGetIntegerv(36006, boundFramebuffer, 0);
        if (boundFramebuffer[0] != framebuffer) {
            GLES20.glBindFramebuffer(36160, framebuffer);
        }
        checkGlError();
        GLES20.glViewport(0, 0, width, height);
        checkGlError();
    }

    public static FloatBuffer createBuffer(float[] data) {
        return (FloatBuffer) createBuffer(data.length).put(data).flip();
    }

    private static FloatBuffer createBuffer(int capacity) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity * 4);
        return byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public static int createExternalTexture() throws GlException {
        int texId = generateTexture();
        bindTexture(36197, texId, 9729);
        return texId;
    }

    public static int createTexture(Bitmap bitmap) throws GlException {
        int texId = generateTexture();
        setTexture(texId, bitmap);
        return texId;
    }

    public static int createTexture(int width, int height, boolean useHighPrecisionColorComponents) throws GlException {
        if (useHighPrecisionColorComponents) {
            return createTextureUninitialized(width, height, 34842, 5131);
        }
        return createTextureUninitialized(width, height, 6408, 5121);
    }

    private static int createTextureUninitialized(int width, int height, int internalFormat, int type) throws GlException {
        assertValidTextureSize(width, height);
        int texId = generateTexture();
        bindTexture(3553, texId, 9729);
        GLES20.glTexImage2D(3553, 0, internalFormat, width, height, 0, 6408, type, null);
        checkGlError();
        return texId;
    }

    public static int generateTexture() throws GlException {
        int[] texId = new int[1];
        GLES20.glGenTextures(1, texId, 0);
        checkGlError();
        return texId[0];
    }

    public static void setTexture(int texId, Bitmap bitmap) throws GlException {
        assertValidTextureSize(bitmap.getWidth(), bitmap.getHeight());
        bindTexture(3553, texId, 9729);
        GLUtils.texImage2D(3553, 0, bitmap, 0);
        checkGlError();
    }

    public static void bindTexture(int textureTarget, int texId, int sampleFilter) throws GlException {
        GLES20.glBindTexture(textureTarget, texId);
        checkGlError();
        GLES20.glTexParameteri(textureTarget, 10240, sampleFilter);
        checkGlError();
        GLES20.glTexParameteri(textureTarget, 10241, sampleFilter);
        checkGlError();
        GLES20.glTexParameteri(textureTarget, 10242, 33071);
        checkGlError();
        GLES20.glTexParameteri(textureTarget, 10243, 33071);
        checkGlError();
    }

    public static int createFboForTexture(int texId) throws GlException {
        int[] fboId = new int[1];
        GLES20.glGenFramebuffers(1, fboId, 0);
        checkGlError();
        GLES20.glBindFramebuffer(36160, fboId[0]);
        checkGlError();
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, texId, 0);
        checkGlError();
        return fboId[0];
    }

    public static void deleteTexture(int textureId) throws GlException {
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        checkGlError();
    }

    public static void destroyEglContext(EGLDisplay eglDisplay, EGLContext eglContext) throws GlException {
        if (eglDisplay == null) {
            return;
        }
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        checkEglException("Error releasing context");
        if (eglContext != null) {
            EGL14.eglDestroyContext(eglDisplay, eglContext);
            checkEglException("Error destroying context");
        }
        EGL14.eglReleaseThread();
        checkEglException("Error releasing thread");
        EGL14.eglTerminate(eglDisplay);
        checkEglException("Error terminating display");
    }

    public static void destroyEglSurface(EGLDisplay eglDisplay, EGLSurface eglSurface) throws GlException {
        if (eglDisplay == null || eglSurface == null) {
            return;
        }
        EGL14.eglDestroySurface(eglDisplay, eglSurface);
        checkEglException("Error destroying surface");
    }

    public static void deleteFbo(int fboId) throws GlException {
        GLES20.glDeleteFramebuffers(1, new int[]{fboId}, 0);
        checkGlError();
    }

    public static void deleteRbo(int rboId) throws GlException {
        GLES20.glDeleteRenderbuffers(1, new int[]{rboId}, 0);
        checkGlError();
    }

    public static void checkGlException(boolean expression, String errorMessage) throws GlException {
        if (!expression) {
            throw new GlException(errorMessage);
        }
    }

    private static EGLConfig getEglConfig(EGLDisplay eglDisplay, int[] attributes) throws GlException {
        EGLConfig[] eglConfigs = new EGLConfig[1];
        if (!EGL14.eglChooseConfig(eglDisplay, attributes, 0, eglConfigs, 0, 1, new int[1], 0)) {
            throw new GlException("eglChooseConfig failed.");
        }
        return eglConfigs[0];
    }

    private static boolean isExtensionSupported(String extensionName) {
        EGLDisplay display = EGL14.eglGetDisplay(0);
        String eglExtensions = EGL14.eglQueryString(display, 12373);
        return eglExtensions != null && eglExtensions.contains(extensionName);
    }

    private static void focusRenderTarget(EGLDisplay eglDisplay, EGLContext eglContext, EGLSurface eglSurface, int framebuffer, int width, int height) throws GlException {
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
        checkEglException("Error making context current");
        focusFramebufferUsingCurrentContext(framebuffer, width, height);
    }

    private static void checkEglException(String errorMessage) throws GlException {
        int error = EGL14.eglGetError();
        if (error != 12288) {
            throw new GlException(errorMessage + ", error code: 0x" + Integer.toHexString(error));
        }
    }
}
