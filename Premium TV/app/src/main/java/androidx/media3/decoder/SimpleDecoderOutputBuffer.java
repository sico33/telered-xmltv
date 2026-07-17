package androidx.media3.decoder;

import androidx.media3.common.util.Assertions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: classes.dex */
public class SimpleDecoderOutputBuffer extends DecoderOutputBuffer {
    public ByteBuffer data;
    private final DecoderOutputBuffer.Owner<SimpleDecoderOutputBuffer> owner;

    public SimpleDecoderOutputBuffer(DecoderOutputBuffer.Owner<SimpleDecoderOutputBuffer> owner) {
        this.owner = owner;
    }

    public ByteBuffer init(long timeUs, int size) {
        this.timeUs = timeUs;
        if (this.data == null || this.data.capacity() < size) {
            this.data = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        }
        this.data.position(0);
        this.data.limit(size);
        return this.data;
    }

    public ByteBuffer grow(int newSize) {
        ByteBuffer oldData = (ByteBuffer) Assertions.checkNotNull(this.data);
        Assertions.checkArgument(newSize >= oldData.limit());
        ByteBuffer newData = ByteBuffer.allocateDirect(newSize).order(ByteOrder.nativeOrder());
        int restorePosition = oldData.position();
        oldData.position(0);
        newData.put(oldData);
        newData.position(restorePosition);
        newData.limit(newSize);
        this.data = newData;
        return newData;
    }

    @Override // androidx.media3.decoder.DecoderOutputBuffer, androidx.media3.decoder.Buffer
    public void clear() {
        super.clear();
        if (this.data != null) {
            this.data.clear();
        }
    }

    @Override // androidx.media3.decoder.DecoderOutputBuffer
    public void release() {
        this.owner.releaseOutputBuffer(this);
    }
}
