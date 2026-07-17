package androidx.media3.common.util;

import androidx.media3.extractor.ts.PsExtractor;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Chars;
import com.google.common.primitives.UnsignedBytes;
import com.google.errorprone.annotations.CheckReturnValue;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
@CheckReturnValue
public final class ParsableByteArray {
    private static final char[] CR_AND_LF = {'\r', '\n'};
    private static final char[] LF = {'\n'};
    private static final ImmutableSet<Charset> SUPPORTED_CHARSETS_FOR_READLINE = ImmutableSet.of(Charsets.US_ASCII, Charsets.UTF_8, Charsets.UTF_16, Charsets.UTF_16BE, Charsets.UTF_16LE);
    private byte[] data;
    private int limit;
    private int position;

    public ParsableByteArray() {
        this.data = Util.EMPTY_BYTE_ARRAY;
    }

    public ParsableByteArray(int limit) {
        this.data = new byte[limit];
        this.limit = limit;
    }

    public ParsableByteArray(byte[] data) {
        this.data = data;
        this.limit = data.length;
    }

    public ParsableByteArray(byte[] data, int limit) {
        this.data = data;
        this.limit = limit;
    }

    public void reset(int limit) {
        reset(capacity() < limit ? new byte[limit] : this.data, limit);
    }

    public void reset(byte[] data) {
        reset(data, data.length);
    }

    public void reset(byte[] data, int limit) {
        this.data = data;
        this.limit = limit;
        this.position = 0;
    }

    public void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity > capacity()) {
            this.data = Arrays.copyOf(this.data, requiredCapacity);
        }
    }

    public int bytesLeft() {
        return this.limit - this.position;
    }

    public int limit() {
        return this.limit;
    }

    public void setLimit(int limit) {
        Assertions.checkArgument(limit >= 0 && limit <= this.data.length);
        this.limit = limit;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        Assertions.checkArgument(position >= 0 && position <= this.limit);
        this.position = position;
    }

    public byte[] getData() {
        return this.data;
    }

    public int capacity() {
        return this.data.length;
    }

    public void skipBytes(int bytes) {
        setPosition(this.position + bytes);
    }

    public void readBytes(ParsableBitArray bitArray, int length) {
        readBytes(bitArray.data, 0, length);
        bitArray.setPosition(0);
    }

    public void readBytes(byte[] buffer, int offset, int length) {
        System.arraycopy(this.data, this.position, buffer, offset, length);
        this.position += length;
    }

    public void readBytes(ByteBuffer buffer, int length) {
        buffer.put(this.data, this.position, length);
        this.position += length;
    }

    public int peekUnsignedByte() {
        return this.data[this.position] & 255;
    }

    public char peekChar() {
        return (char) (((this.data[this.position] & 255) << 8) | (this.data[this.position + 1] & 255));
    }

    public char peekChar(Charset charset) {
        Assertions.checkArgument(SUPPORTED_CHARSETS_FOR_READLINE.contains(charset), "Unsupported charset: " + charset);
        return (char) (peekCharacterAndSize(charset) >> 16);
    }

    public int readUnsignedByte() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        return bArr[i] & 255;
    }

    public int readUnsignedShort() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = (bArr[i] & 255) << 8;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        return i2 | (bArr2[i3] & 255);
    }

    public int readLittleEndianUnsignedShort() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = bArr[i] & 255;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        return i2 | ((bArr2[i3] & 255) << 8);
    }

    public short readShort() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = (bArr[i] & 255) << 8;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        return (short) (i2 | (bArr2[i3] & 255));
    }

    public short readLittleEndianShort() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = bArr[i] & 255;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        return (short) (i2 | ((bArr2[i3] & 255) << 8));
    }

    public int readUnsignedInt24() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = (bArr[i] & 255) << 16;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        int i4 = i2 | ((bArr2[i3] & 255) << 8);
        byte[] bArr3 = this.data;
        int i5 = this.position;
        this.position = i5 + 1;
        return i4 | (bArr3[i5] & 255);
    }

    public int readInt24() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = ((bArr[i] & 255) << 24) >> 8;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        int i4 = i2 | ((bArr2[i3] & 255) << 8);
        byte[] bArr3 = this.data;
        int i5 = this.position;
        this.position = i5 + 1;
        return i4 | (bArr3[i5] & 255);
    }

    public int readLittleEndianInt24() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = bArr[i] & 255;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        int i4 = i2 | ((bArr2[i3] & 255) << 8);
        byte[] bArr3 = this.data;
        int i5 = this.position;
        this.position = i5 + 1;
        return i4 | ((bArr3[i5] & 255) << 16);
    }

    public int readLittleEndianUnsignedInt24() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = bArr[i] & 255;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        int i4 = i2 | ((bArr2[i3] & 255) << 8);
        byte[] bArr3 = this.data;
        int i5 = this.position;
        this.position = i5 + 1;
        return i4 | ((bArr3[i5] & 255) << 16);
    }

    public long readUnsignedInt() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        long j = (((long) bArr[i]) & 255) << 24;
        byte[] bArr2 = this.data;
        int i2 = this.position;
        this.position = i2 + 1;
        long j2 = j | ((((long) bArr2[i2]) & 255) << 16);
        byte[] bArr3 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        long j3 = j2 | ((((long) bArr3[i3]) & 255) << 8);
        byte[] bArr4 = this.data;
        int i4 = this.position;
        this.position = i4 + 1;
        return j3 | (255 & ((long) bArr4[i4]));
    }

    public long readLittleEndianUnsignedInt() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        long j = ((long) bArr[i]) & 255;
        byte[] bArr2 = this.data;
        int i2 = this.position;
        this.position = i2 + 1;
        long j2 = j | ((((long) bArr2[i2]) & 255) << 8);
        byte[] bArr3 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        long j3 = j2 | ((((long) bArr3[i3]) & 255) << 16);
        byte[] bArr4 = this.data;
        int i4 = this.position;
        this.position = i4 + 1;
        return j3 | ((255 & ((long) bArr4[i4])) << 24);
    }

    public int readInt() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = (bArr[i] & 255) << 24;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        int i4 = i2 | ((bArr2[i3] & 255) << 16);
        byte[] bArr3 = this.data;
        int i5 = this.position;
        this.position = i5 + 1;
        int i6 = i4 | ((bArr3[i5] & 255) << 8);
        byte[] bArr4 = this.data;
        int i7 = this.position;
        this.position = i7 + 1;
        return i6 | (bArr4[i7] & 255);
    }

    public int readLittleEndianInt() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = bArr[i] & 255;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        int i4 = i2 | ((bArr2[i3] & 255) << 8);
        byte[] bArr3 = this.data;
        int i5 = this.position;
        this.position = i5 + 1;
        int i6 = i4 | ((bArr3[i5] & 255) << 16);
        byte[] bArr4 = this.data;
        int i7 = this.position;
        this.position = i7 + 1;
        return i6 | ((bArr4[i7] & 255) << 24);
    }

    public long readLong() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        long j = (((long) bArr[i]) & 255) << 56;
        byte[] bArr2 = this.data;
        int i2 = this.position;
        this.position = i2 + 1;
        long j2 = j | ((((long) bArr2[i2]) & 255) << 48);
        byte[] bArr3 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        long j3 = j2 | ((((long) bArr3[i3]) & 255) << 40);
        byte[] bArr4 = this.data;
        int i4 = this.position;
        this.position = i4 + 1;
        long j4 = j3 | ((((long) bArr4[i4]) & 255) << 32);
        byte[] bArr5 = this.data;
        int i5 = this.position;
        this.position = i5 + 1;
        long j5 = j4 | ((((long) bArr5[i5]) & 255) << 24);
        byte[] bArr6 = this.data;
        int i6 = this.position;
        this.position = i6 + 1;
        long j6 = j5 | ((((long) bArr6[i6]) & 255) << 16);
        byte[] bArr7 = this.data;
        int i7 = this.position;
        this.position = i7 + 1;
        long j7 = j6 | ((((long) bArr7[i7]) & 255) << 8);
        byte[] bArr8 = this.data;
        int i8 = this.position;
        this.position = i8 + 1;
        return j7 | (255 & ((long) bArr8[i8]));
    }

    public long readLittleEndianLong() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        long j = ((long) bArr[i]) & 255;
        byte[] bArr2 = this.data;
        int i2 = this.position;
        this.position = i2 + 1;
        long j2 = j | ((((long) bArr2[i2]) & 255) << 8);
        byte[] bArr3 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        long j3 = j2 | ((((long) bArr3[i3]) & 255) << 16);
        byte[] bArr4 = this.data;
        int i4 = this.position;
        this.position = i4 + 1;
        long j4 = j3 | ((((long) bArr4[i4]) & 255) << 24);
        byte[] bArr5 = this.data;
        int i5 = this.position;
        this.position = i5 + 1;
        long j5 = j4 | ((((long) bArr5[i5]) & 255) << 32);
        byte[] bArr6 = this.data;
        int i6 = this.position;
        this.position = i6 + 1;
        long j6 = j5 | ((((long) bArr6[i6]) & 255) << 40);
        byte[] bArr7 = this.data;
        int i7 = this.position;
        this.position = i7 + 1;
        long j7 = j6 | ((((long) bArr7[i7]) & 255) << 48);
        byte[] bArr8 = this.data;
        int i8 = this.position;
        this.position = i8 + 1;
        return j7 | ((255 & ((long) bArr8[i8])) << 56);
    }

    public int readUnsignedFixedPoint1616() {
        byte[] bArr = this.data;
        int i = this.position;
        this.position = i + 1;
        int i2 = (bArr[i] & 255) << 8;
        byte[] bArr2 = this.data;
        int i3 = this.position;
        this.position = i3 + 1;
        int result = i2 | (bArr2[i3] & 255);
        this.position += 2;
        return result;
    }

    public int readSynchSafeInt() {
        int b1 = readUnsignedByte();
        int b2 = readUnsignedByte();
        int b3 = readUnsignedByte();
        int b4 = readUnsignedByte();
        return (b1 << 21) | (b2 << 14) | (b3 << 7) | b4;
    }

    public int readUnsignedIntToInt() {
        int result = readInt();
        if (result < 0) {
            throw new IllegalStateException("Top bit not zero: " + result);
        }
        return result;
    }

    public int readLittleEndianUnsignedIntToInt() {
        int result = readLittleEndianInt();
        if (result < 0) {
            throw new IllegalStateException("Top bit not zero: " + result);
        }
        return result;
    }

    public long readUnsignedLongToLong() {
        long result = readLong();
        if (result < 0) {
            throw new IllegalStateException("Top bit not zero: " + result);
        }
        return result;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public String readString(int length) {
        return readString(length, Charsets.UTF_8);
    }

    public String readString(int length, Charset charset) {
        String result = new String(this.data, this.position, length, charset);
        this.position += length;
        return result;
    }

    public String readNullTerminatedString(int length) {
        if (length == 0) {
            return "";
        }
        int stringLength = length;
        int lastIndex = (this.position + length) - 1;
        if (lastIndex < this.limit && this.data[lastIndex] == 0) {
            stringLength--;
        }
        String result = Util.fromUtf8Bytes(this.data, this.position, stringLength);
        this.position += length;
        return result;
    }

    public String readNullTerminatedString() {
        return readDelimiterTerminatedString((char) 0);
    }

    public String readDelimiterTerminatedString(char delimiter) {
        if (bytesLeft() == 0) {
            return null;
        }
        int stringLimit = this.position;
        while (stringLimit < this.limit && this.data[stringLimit] != delimiter) {
            stringLimit++;
        }
        String string = Util.fromUtf8Bytes(this.data, this.position, stringLimit - this.position);
        this.position = stringLimit;
        if (this.position < this.limit) {
            this.position++;
        }
        return string;
    }

    public String readLine() {
        return readLine(Charsets.UTF_8);
    }

    public String readLine(Charset charset) {
        Assertions.checkArgument(SUPPORTED_CHARSETS_FOR_READLINE.contains(charset), "Unsupported charset: " + charset);
        if (bytesLeft() == 0) {
            return null;
        }
        if (!charset.equals(Charsets.US_ASCII)) {
            readUtfCharsetFromBom();
        }
        int lineLimit = findNextLineTerminator(charset);
        String line = readString(lineLimit - this.position, charset);
        if (this.position == this.limit) {
            return line;
        }
        skipLineTerminator(charset);
        return line;
    }

    public long readUtf8EncodedLong() {
        int length = 0;
        long value = this.data[this.position];
        for (int j = 7; j >= 0; j--) {
            if ((((long) (1 << j)) & value) == 0) {
                if (j < 6) {
                    value &= (long) ((1 << j) - 1);
                    length = 7 - j;
                    break;
                }
                if (j != 7) {
                    break;
                }
                length = 1;
                break;
            }
        }
        if (length == 0) {
            throw new NumberFormatException("Invalid UTF-8 sequence first byte: " + value);
        }
        for (int i = 1; i < length; i++) {
            int x = this.data[this.position + i];
            if ((x & PsExtractor.AUDIO_STREAM) != 128) {
                throw new NumberFormatException("Invalid UTF-8 sequence continuation byte: " + value);
            }
            value = (value << 6) | ((long) (x & 63));
        }
        int i2 = this.position;
        this.position = i2 + length;
        return value;
    }

    public Charset readUtfCharsetFromBom() {
        if (bytesLeft() >= 3 && this.data[this.position] == -17 && this.data[this.position + 1] == -69 && this.data[this.position + 2] == -65) {
            this.position += 3;
            return Charsets.UTF_8;
        }
        if (bytesLeft() >= 2) {
            if (this.data[this.position] == -2 && this.data[this.position + 1] == -1) {
                this.position += 2;
                return Charsets.UTF_16BE;
            }
            if (this.data[this.position] == -1 && this.data[this.position + 1] == -2) {
                this.position += 2;
                return Charsets.UTF_16LE;
            }
            return null;
        }
        return null;
    }

    private int findNextLineTerminator(Charset charset) {
        int stride;
        if (charset.equals(Charsets.UTF_8) || charset.equals(Charsets.US_ASCII)) {
            stride = 1;
        } else if (charset.equals(Charsets.UTF_16) || charset.equals(Charsets.UTF_16LE) || charset.equals(Charsets.UTF_16BE)) {
            stride = 2;
        } else {
            throw new IllegalArgumentException("Unsupported charset: " + charset);
        }
        for (int i = this.position; i < this.limit - (stride - 1); i += stride) {
            if ((charset.equals(Charsets.UTF_8) || charset.equals(Charsets.US_ASCII)) && Util.isLinebreak(this.data[i])) {
                return i;
            }
            if ((charset.equals(Charsets.UTF_16) || charset.equals(Charsets.UTF_16BE)) && this.data[i] == 0 && Util.isLinebreak(this.data[i + 1])) {
                return i;
            }
            if (charset.equals(Charsets.UTF_16LE) && this.data[i + 1] == 0 && Util.isLinebreak(this.data[i])) {
                return i;
            }
        }
        int i2 = this.limit;
        return i2;
    }

    private void skipLineTerminator(Charset charset) {
        if (readCharacterIfInList(charset, CR_AND_LF) == '\r') {
            readCharacterIfInList(charset, LF);
        }
    }

    private char readCharacterIfInList(Charset charset, char[] chars) {
        int characterAndSize = peekCharacterAndSize(charset);
        if (characterAndSize != 0 && Chars.contains(chars, (char) (characterAndSize >> 16))) {
            this.position += 65535 & characterAndSize;
            return (char) (characterAndSize >> 16);
        }
        return (char) 0;
    }

    private int peekCharacterAndSize(Charset charset) {
        byte character;
        short characterSize;
        if ((charset.equals(Charsets.UTF_8) || charset.equals(Charsets.US_ASCII)) && bytesLeft() >= 1) {
            character = (byte) Chars.checkedCast(UnsignedBytes.toInt(this.data[this.position]));
            characterSize = 1;
        } else if ((charset.equals(Charsets.UTF_16) || charset.equals(Charsets.UTF_16BE)) && bytesLeft() >= 2) {
            character = (byte) Chars.fromBytes(this.data[this.position], this.data[this.position + 1]);
            characterSize = 2;
        } else if (charset.equals(Charsets.UTF_16LE) && bytesLeft() >= 2) {
            character = (byte) Chars.fromBytes(this.data[this.position + 1], this.data[this.position]);
            characterSize = 2;
        } else {
            return 0;
        }
        return (Chars.checkedCast(character) << 16) + characterSize;
    }
}
