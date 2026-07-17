package androidx.media3.decoder;

import androidx.media3.common.Format;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public class VideoDecoderOutputBuffer extends DecoderOutputBuffer {
    public static final int COLORSPACE_BT2020 = 3;
    public static final int COLORSPACE_BT601 = 1;
    public static final int COLORSPACE_BT709 = 2;
    public static final int COLORSPACE_UNKNOWN = 0;
    public int colorspace;
    public ByteBuffer data;
    public int decoderPrivate;
    public Format format;
    public int height;
    public int mode;
    private final DecoderOutputBuffer.Owner<VideoDecoderOutputBuffer> owner;
    public ByteBuffer supplementalData;
    public int width;
    public ByteBuffer[] yuvPlanes;
    public int[] yuvStrides;

    public VideoDecoderOutputBuffer(DecoderOutputBuffer.Owner<VideoDecoderOutputBuffer> owner) {
        this.owner = owner;
    }

    @Override // androidx.media3.decoder.DecoderOutputBuffer
    public void release() {
        this.owner.releaseOutputBuffer(this);
    }

    public void init(long timeUs, int mode, ByteBuffer supplementalData) {
        this.timeUs = timeUs;
        this.mode = mode;
        if (supplementalData != null && supplementalData.hasRemaining()) {
            addFlag(268435456);
            int size = supplementalData.limit();
            if (this.supplementalData == null || this.supplementalData.capacity() < size) {
                this.supplementalData = ByteBuffer.allocate(size);
            } else {
                this.supplementalData.clear();
            }
            this.supplementalData.put(supplementalData);
            this.supplementalData.flip();
            supplementalData.position(0);
            return;
        }
        this.supplementalData = null;
    }

    public boolean initForYuvFrame(int width, int height, int yStride, int uvStride, int colorspace) {
        this.width = width;
        this.height = height;
        this.colorspace = colorspace;
        int uvHeight = (int) ((((long) height) + 1) / 2);
        if (!isSafeToMultiply(yStride, height) || !isSafeToMultiply(uvStride, uvHeight)) {
            return false;
        }
        int yLength = yStride * height;
        int uvLength = uvStride * uvHeight;
        int minimumYuvSize = (uvLength * 2) + yLength;
        if (!isSafeToMultiply(uvLength, 2) || minimumYuvSize < yLength) {
            return false;
        }
        if (this.data == null || this.data.capacity() < minimumYuvSize) {
            this.data = ByteBuffer.allocateDirect(minimumYuvSize);
        } else {
            this.data.position(0);
            this.data.limit(minimumYuvSize);
        }
        if (this.yuvPlanes == null) {
            this.yuvPlanes = new ByteBuffer[3];
        }
        ByteBuffer data = this.data;
        ByteBuffer[] yuvPlanes = this.yuvPlanes;
        yuvPlanes[0] = data.slice();
        yuvPlanes[0].limit(yLength);
        data.position(yLength);
        yuvPlanes[1] = data.slice();
        yuvPlanes[1].limit(uvLength);
        data.position(yLength + uvLength);
        yuvPlanes[2] = data.slice();
        yuvPlanes[2].limit(uvLength);
        if (this.yuvStrides == null) {
            this.yuvStrides = new int[3];
        }
        this.yuvStrides[0] = yStride;
        this.yuvStrides[1] = uvStride;
        this.yuvStrides[2] = uvStride;
        return true;
    }

    public void initForPrivateFrame(int width, int height) {
        this.width = width;
        this.height = height;
    }

    private static boolean isSafeToMultiply(int a, int b) {
        return a >= 0 && b >= 0 && (b <= 0 || a < Integer.MAX_VALUE / b);
    }
}
