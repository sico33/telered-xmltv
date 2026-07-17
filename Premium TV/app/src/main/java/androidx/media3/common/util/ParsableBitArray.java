package androidx.media3.common.util;

import androidx.core.view.MotionEventCompat;
import com.google.common.base.Charsets;
import com.google.errorprone.annotations.CheckReturnValue;
import java.nio.charset.Charset;

/* JADX INFO: loaded from: classes.dex */
@CheckReturnValue
public final class ParsableBitArray {
    private int bitOffset;
    private int byteLimit;
    private int byteOffset;
    public byte[] data;

    public ParsableBitArray() {
        this.data = Util.EMPTY_BYTE_ARRAY;
    }

    public ParsableBitArray(byte[] data) {
        this(data, data.length);
    }

    public ParsableBitArray(byte[] data, int limit) {
        this.data = data;
        this.byteLimit = limit;
    }

    public void reset(byte[] data) {
        reset(data, data.length);
    }

    public void reset(ParsableByteArray parsableByteArray) {
        reset(parsableByteArray.getData(), parsableByteArray.limit());
        setPosition(parsableByteArray.getPosition() * 8);
    }

    public void reset(byte[] data, int limit) {
        this.data = data;
        this.byteOffset = 0;
        this.bitOffset = 0;
        this.byteLimit = limit;
    }

    public int bitsLeft() {
        return ((this.byteLimit - this.byteOffset) * 8) - this.bitOffset;
    }

    public int getPosition() {
        return (this.byteOffset * 8) + this.bitOffset;
    }

    public int getBytePosition() {
        Assertions.checkState(this.bitOffset == 0);
        return this.byteOffset;
    }

    public void setPosition(int position) {
        this.byteOffset = position / 8;
        this.bitOffset = position - (this.byteOffset * 8);
        assertValidOffset();
    }

    public void skipBit() {
        int i = this.bitOffset + 1;
        this.bitOffset = i;
        if (i == 8) {
            this.bitOffset = 0;
            this.byteOffset++;
        }
        assertValidOffset();
    }

    public void skipBits(int numBits) {
        int numBytes = numBits / 8;
        this.byteOffset += numBytes;
        this.bitOffset += numBits - (numBytes * 8);
        if (this.bitOffset > 7) {
            this.byteOffset++;
            this.bitOffset -= 8;
        }
        assertValidOffset();
    }

    public boolean readBit() {
        boolean returnValue = (this.data[this.byteOffset] & (128 >> this.bitOffset)) != 0;
        skipBit();
        return returnValue;
    }

    public int readBits(int numBits) {
        if (numBits == 0) {
            return 0;
        }
        int returnValue = 0;
        this.bitOffset += numBits;
        while (this.bitOffset > 8) {
            this.bitOffset -= 8;
            byte[] bArr = this.data;
            int i = this.byteOffset;
            this.byteOffset = i + 1;
            returnValue |= (bArr[i] & 255) << this.bitOffset;
        }
        int returnValue2 = (returnValue | ((this.data[this.byteOffset] & 255) >> (8 - this.bitOffset))) & ((-1) >>> (32 - numBits));
        if (this.bitOffset == 8) {
            this.bitOffset = 0;
            this.byteOffset++;
        }
        assertValidOffset();
        return returnValue2;
    }

    public long readBitsToLong(int numBits) {
        if (numBits > 32) {
            return Util.toLong(readBits(numBits - 32), readBits(32));
        }
        return Util.toUnsignedLong(readBits(numBits));
    }

    public void readBits(byte[] buffer, int offset, int numBits) {
        int to = (numBits >> 3) + offset;
        for (int i = offset; i < to; i++) {
            byte[] bArr = this.data;
            int i2 = this.byteOffset;
            this.byteOffset = i2 + 1;
            buffer[i] = (byte) (bArr[i2] << this.bitOffset);
            buffer[i] = (byte) (((255 & this.data[this.byteOffset]) >> (8 - this.bitOffset)) | buffer[i]);
        }
        int i3 = numBits & 7;
        if (i3 == 0) {
            return;
        }
        buffer[to] = (byte) (buffer[to] & (255 >> i3));
        if (this.bitOffset + i3 > 8) {
            byte b = buffer[to];
            byte[] bArr2 = this.data;
            int i4 = this.byteOffset;
            this.byteOffset = i4 + 1;
            buffer[to] = (byte) (b | ((bArr2[i4] & 255) << this.bitOffset));
            this.bitOffset -= 8;
        }
        this.bitOffset += i3;
        int lastDataByteTrailingBits = (255 & this.data[this.byteOffset]) >> (8 - this.bitOffset);
        buffer[to] = (byte) (buffer[to] | ((byte) (lastDataByteTrailingBits << (8 - i3))));
        if (this.bitOffset == 8) {
            this.bitOffset = 0;
            this.byteOffset++;
        }
        assertValidOffset();
    }

    public void byteAlign() {
        if (this.bitOffset == 0) {
            return;
        }
        this.bitOffset = 0;
        this.byteOffset++;
        assertValidOffset();
    }

    public void readBytes(byte[] buffer, int offset, int length) {
        Assertions.checkState(this.bitOffset == 0);
        System.arraycopy(this.data, this.byteOffset, buffer, offset, length);
        this.byteOffset += length;
        assertValidOffset();
    }

    public void skipBytes(int length) {
        Assertions.checkState(this.bitOffset == 0);
        this.byteOffset += length;
        assertValidOffset();
    }

    public String readBytesAsString(int length) {
        return readBytesAsString(length, Charsets.UTF_8);
    }

    public String readBytesAsString(int length, Charset charset) {
        byte[] bytes = new byte[length];
        readBytes(bytes, 0, length);
        return new String(bytes, charset);
    }

    public void putInt(int value, int numBits) {
        if (numBits < 32) {
            value &= (1 << numBits) - 1;
        }
        int firstByteReadSize = Math.min(8 - this.bitOffset, numBits);
        int firstByteRightPaddingSize = (8 - this.bitOffset) - firstByteReadSize;
        int firstByteBitmask = (MotionEventCompat.ACTION_POINTER_INDEX_MASK >> this.bitOffset) | ((1 << firstByteRightPaddingSize) - 1);
        this.data[this.byteOffset] = (byte) (this.data[this.byteOffset] & firstByteBitmask);
        int firstByteInputBits = value >>> (numBits - firstByteReadSize);
        this.data[this.byteOffset] = (byte) (this.data[this.byteOffset] | (firstByteInputBits << firstByteRightPaddingSize));
        int remainingBitsToRead = numBits - firstByteReadSize;
        int currentByteIndex = this.byteOffset + 1;
        while (true) {
            byte[] bArr = this.data;
            if (remainingBitsToRead > 8) {
                bArr[currentByteIndex] = (byte) (value >>> (remainingBitsToRead - 8));
                remainingBitsToRead -= 8;
                currentByteIndex++;
            } else {
                int lastByteRightPaddingSize = 8 - remainingBitsToRead;
                bArr[currentByteIndex] = (byte) (this.data[currentByteIndex] & ((1 << lastByteRightPaddingSize) - 1));
                int lastByteInput = value & ((1 << remainingBitsToRead) - 1);
                this.data[currentByteIndex] = (byte) (this.data[currentByteIndex] | (lastByteInput << lastByteRightPaddingSize));
                skipBits(numBits);
                assertValidOffset();
                return;
            }
        }
    }

    private void assertValidOffset() {
        Assertions.checkState(this.byteOffset >= 0 && (this.byteOffset < this.byteLimit || (this.byteOffset == this.byteLimit && this.bitOffset == 0)));
    }
}
