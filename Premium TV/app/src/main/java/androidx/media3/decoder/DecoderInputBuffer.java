package androidx.media3.decoder;

import androidx.media3.common.Format;
import androidx.media3.common.MediaLibraryInfo;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/* JADX INFO: loaded from: classes.dex */
public class DecoderInputBuffer extends Buffer {
    public static final int BUFFER_REPLACEMENT_MODE_DIRECT = 2;
    public static final int BUFFER_REPLACEMENT_MODE_DISABLED = 0;
    public static final int BUFFER_REPLACEMENT_MODE_NORMAL = 1;
    private final int bufferReplacementMode;
    public final CryptoInfo cryptoInfo;
    public ByteBuffer data;
    public Format format;
    private final int paddingSize;
    public ByteBuffer supplementalData;
    public long timeUs;
    public boolean waitingForKeys;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface BufferReplacementMode {
    }

    static {
        MediaLibraryInfo.registerModule("media3.decoder");
    }

    public static final class InsufficientCapacityException extends IllegalStateException {
        public final int currentCapacity;
        public final int requiredCapacity;

        public InsufficientCapacityException(int currentCapacity, int requiredCapacity) {
            super("Buffer too small (" + currentCapacity + " < " + requiredCapacity + ")");
            this.currentCapacity = currentCapacity;
            this.requiredCapacity = requiredCapacity;
        }
    }

    public static DecoderInputBuffer newNoDataInstance() {
        return new DecoderInputBuffer(0);
    }

    public DecoderInputBuffer(int bufferReplacementMode) {
        this(bufferReplacementMode, 0);
    }

    public DecoderInputBuffer(int bufferReplacementMode, int paddingSize) {
        this.cryptoInfo = new CryptoInfo();
        this.bufferReplacementMode = bufferReplacementMode;
        this.paddingSize = paddingSize;
    }

    @EnsuresNonNull({"supplementalData"})
    public void resetSupplementalData(int length) {
        if (this.supplementalData == null || this.supplementalData.capacity() < length) {
            this.supplementalData = ByteBuffer.allocate(length);
        } else {
            this.supplementalData.clear();
        }
    }

    @EnsuresNonNull({"data"})
    public void ensureSpaceForWrite(int length) {
        int length2 = length + this.paddingSize;
        ByteBuffer currentData = this.data;
        if (currentData == null) {
            this.data = createReplacementByteBuffer(length2);
            return;
        }
        int capacity = currentData.capacity();
        int position = currentData.position();
        int requiredCapacity = position + length2;
        if (capacity >= requiredCapacity) {
            this.data = currentData;
            return;
        }
        ByteBuffer newData = createReplacementByteBuffer(requiredCapacity);
        newData.order(currentData.order());
        if (position > 0) {
            currentData.flip();
            newData.put(currentData);
        }
        this.data = newData;
    }

    public final boolean isEncrypted() {
        return getFlag(1073741824);
    }

    public final void flip() {
        if (this.data != null) {
            this.data.flip();
        }
        if (this.supplementalData != null) {
            this.supplementalData.flip();
        }
    }

    @Override // androidx.media3.decoder.Buffer
    public void clear() {
        super.clear();
        if (this.data != null) {
            this.data.clear();
        }
        if (this.supplementalData != null) {
            this.supplementalData.clear();
        }
        this.waitingForKeys = false;
    }

    private ByteBuffer createReplacementByteBuffer(int requiredCapacity) {
        if (this.bufferReplacementMode == 1) {
            return ByteBuffer.allocate(requiredCapacity);
        }
        if (this.bufferReplacementMode == 2) {
            return ByteBuffer.allocateDirect(requiredCapacity);
        }
        int currentCapacity = this.data == null ? 0 : this.data.capacity();
        throw new InsufficientCapacityException(currentCapacity, requiredCapacity);
    }
}
