package androidx.media3.extractor;

import androidx.media3.common.DataReader;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultExtractorInput implements ExtractorInput {
    private static final int PEEK_MAX_FREE_SPACE = 524288;
    private static final int PEEK_MIN_FREE_SPACE_AFTER_RESIZE = 65536;
    private static final int SCRATCH_SPACE_SIZE = 4096;
    private final DataReader dataReader;
    private int peekBufferLength;
    private int peekBufferPosition;
    private long position;
    private final long streamLength;
    private byte[] peekBuffer = new byte[65536];
    private final byte[] scratchSpace = new byte[4096];

    static {
        MediaLibraryInfo.registerModule("media3.extractor");
    }

    public DefaultExtractorInput(DataReader dataReader, long position, long length) {
        this.dataReader = dataReader;
        this.position = position;
        this.streamLength = length;
    }

    @Override // androidx.media3.extractor.ExtractorInput, androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int bytesRead = readFromPeekBuffer(buffer, offset, length);
        if (bytesRead == 0) {
            bytesRead = readFromUpstream(buffer, offset, length, 0, true);
        }
        commitBytesRead(bytesRead);
        return bytesRead;
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public boolean readFully(byte[] target, int offset, int length, boolean allowEndOfInput) throws IOException {
        int bytesRead = readFromPeekBuffer(target, offset, length);
        while (bytesRead < length && bytesRead != -1) {
            bytesRead = readFromUpstream(target, offset, length, bytesRead, allowEndOfInput);
        }
        commitBytesRead(bytesRead);
        return bytesRead != -1;
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void readFully(byte[] target, int offset, int length) throws IOException {
        readFully(target, offset, length, false);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public int skip(int length) throws IOException {
        int bytesSkipped = skipFromPeekBuffer(length);
        if (bytesSkipped == 0) {
            bytesSkipped = readFromUpstream(this.scratchSpace, 0, Math.min(length, this.scratchSpace.length), 0, true);
        }
        commitBytesRead(bytesSkipped);
        return bytesSkipped;
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public boolean skipFully(int length, boolean allowEndOfInput) throws IOException {
        int bytesSkipped = skipFromPeekBuffer(length);
        while (bytesSkipped < length && bytesSkipped != -1) {
            int minLength = Math.min(length, this.scratchSpace.length + bytesSkipped);
            bytesSkipped = readFromUpstream(this.scratchSpace, -bytesSkipped, minLength, bytesSkipped, allowEndOfInput);
        }
        commitBytesRead(bytesSkipped);
        return bytesSkipped != -1;
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void skipFully(int length) throws IOException {
        skipFully(length, false);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public int peek(byte[] target, int offset, int length) throws IOException {
        int bytesPeeked;
        ensureSpaceForPeek(length);
        int peekBufferRemainingBytes = this.peekBufferLength - this.peekBufferPosition;
        if (peekBufferRemainingBytes == 0) {
            bytesPeeked = readFromUpstream(this.peekBuffer, this.peekBufferPosition, length, 0, true);
            if (bytesPeeked == -1) {
                return -1;
            }
            this.peekBufferLength += bytesPeeked;
        } else {
            bytesPeeked = Math.min(length, peekBufferRemainingBytes);
        }
        System.arraycopy(this.peekBuffer, this.peekBufferPosition, target, offset, bytesPeeked);
        this.peekBufferPosition += bytesPeeked;
        return bytesPeeked;
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public boolean peekFully(byte[] target, int offset, int length, boolean allowEndOfInput) throws IOException {
        if (!advancePeekPosition(length, allowEndOfInput)) {
            return false;
        }
        System.arraycopy(this.peekBuffer, this.peekBufferPosition - length, target, offset, length);
        return true;
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void peekFully(byte[] target, int offset, int length) throws IOException {
        peekFully(target, offset, length, false);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public boolean advancePeekPosition(int length, boolean allowEndOfInput) throws IOException {
        ensureSpaceForPeek(length);
        int bytesPeeked = this.peekBufferLength - this.peekBufferPosition;
        while (bytesPeeked < length) {
            int length2 = length;
            boolean allowEndOfInput2 = allowEndOfInput;
            bytesPeeked = readFromUpstream(this.peekBuffer, this.peekBufferPosition, length2, bytesPeeked, allowEndOfInput2);
            if (bytesPeeked == -1) {
                return false;
            }
            this.peekBufferLength = this.peekBufferPosition + bytesPeeked;
            length = length2;
            allowEndOfInput = allowEndOfInput2;
        }
        this.peekBufferPosition += length;
        return true;
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void advancePeekPosition(int length) throws IOException {
        advancePeekPosition(length, false);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void resetPeekPosition() {
        this.peekBufferPosition = 0;
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public long getPeekPosition() {
        return this.position + ((long) this.peekBufferPosition);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public long getPosition() {
        return this.position;
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public long getLength() {
        return this.streamLength;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: E extends java.lang.Throwable */
    @Override // androidx.media3.extractor.ExtractorInput
    public <E extends Throwable> void setRetryPosition(long position, E e) throws Throwable {
        Assertions.checkArgument(position >= 0);
        this.position = position;
        throw e;
    }

    private void ensureSpaceForPeek(int length) {
        int requiredLength = this.peekBufferPosition + length;
        if (requiredLength > this.peekBuffer.length) {
            int newPeekCapacity = Util.constrainValue(this.peekBuffer.length * 2, 65536 + requiredLength, 524288 + requiredLength);
            this.peekBuffer = Arrays.copyOf(this.peekBuffer, newPeekCapacity);
        }
    }

    private int skipFromPeekBuffer(int length) {
        int bytesSkipped = Math.min(this.peekBufferLength, length);
        updatePeekBuffer(bytesSkipped);
        return bytesSkipped;
    }

    private int readFromPeekBuffer(byte[] target, int offset, int length) {
        if (this.peekBufferLength == 0) {
            return 0;
        }
        int peekBytes = Math.min(this.peekBufferLength, length);
        System.arraycopy(this.peekBuffer, 0, target, offset, peekBytes);
        updatePeekBuffer(peekBytes);
        return peekBytes;
    }

    private void updatePeekBuffer(int bytesConsumed) {
        this.peekBufferLength -= bytesConsumed;
        this.peekBufferPosition = 0;
        byte[] newPeekBuffer = this.peekBuffer;
        if (this.peekBufferLength < this.peekBuffer.length - 524288) {
            newPeekBuffer = new byte[this.peekBufferLength + 65536];
        }
        System.arraycopy(this.peekBuffer, bytesConsumed, newPeekBuffer, 0, this.peekBufferLength);
        this.peekBuffer = newPeekBuffer;
    }

    private int readFromUpstream(byte[] target, int offset, int length, int bytesAlreadyRead, boolean allowEndOfInput) throws IOException {
        if (Thread.interrupted()) {
            throw new InterruptedIOException();
        }
        int bytesRead = this.dataReader.read(target, offset + bytesAlreadyRead, length - bytesAlreadyRead);
        if (bytesRead == -1) {
            if (bytesAlreadyRead == 0 && allowEndOfInput) {
                return -1;
            }
            throw new EOFException();
        }
        return bytesAlreadyRead + bytesRead;
    }

    private void commitBytesRead(int bytesRead) {
        if (bytesRead != -1) {
            this.position += (long) bytesRead;
        }
    }
}
