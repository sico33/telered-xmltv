package androidx.media3.exoplayer.audio;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.extractor.OpusUtil;
import com.google.common.base.Ascii;
import com.google.common.primitives.UnsignedBytes;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class OggOpusAudioPacketizer {
    private static final int CHECKSUM_INDEX = 22;
    private static final int FIRST_AUDIO_SAMPLE_PAGE_SEQUENCE_NUMBER = 2;
    private static final int OGG_PACKET_HEADER_LENGTH = 28;
    private static final int SERIAL_NUMBER = 0;
    private static final byte[] OGG_DEFAULT_ID_HEADER_PAGE = {79, 103, 103, 83, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Ascii.FS, -43, -59, -9, 1, 19, 79, 112, 117, 115, 72, 101, 97, 100, 1, 2, 56, 1, -128, -69, 0, 0, 0, 0, 0};
    private static final byte[] OGG_DEFAULT_COMMENT_HEADER_PAGE = {79, 103, 103, 83, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, Ascii.VT, -103, 87, 83, 1, Ascii.DLE, 79, 112, 117, 115, 84, 97, 103, 115, 0, 0, 0, 0, 0, 0, 0, 0};
    private ByteBuffer outputBuffer = AudioProcessor.EMPTY_BUFFER;
    private int granulePosition = 0;
    private int pageSequenceNumber = 2;

    public void packetize(DecoderInputBuffer inputBuffer, List<byte[]> initializationData) {
        byte[] providedOggIdHeaderPayloadBytes;
        Assertions.checkNotNull(inputBuffer.data);
        if (inputBuffer.data.limit() - inputBuffer.data.position() == 0) {
            return;
        }
        if (this.pageSequenceNumber == 2 && (initializationData.size() == 1 || initializationData.size() == 3)) {
            providedOggIdHeaderPayloadBytes = initializationData.get(0);
        } else {
            providedOggIdHeaderPayloadBytes = null;
        }
        this.outputBuffer = packetizeInternal(inputBuffer.data, providedOggIdHeaderPayloadBytes);
        inputBuffer.clear();
        inputBuffer.ensureSpaceForWrite(this.outputBuffer.remaining());
        inputBuffer.data.put(this.outputBuffer);
        inputBuffer.flip();
    }

    public void reset() {
        this.outputBuffer = AudioProcessor.EMPTY_BUFFER;
        this.granulePosition = 0;
        this.pageSequenceNumber = 2;
    }

    private ByteBuffer packetizeInternal(ByteBuffer inputBuffer, byte[] providedOggIdHeaderPayloadBytes) {
        int outputPacketSize;
        int oggIdHeaderPageSize;
        int length;
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int inputBufferSize = limit - position;
        int numSegments = (inputBufferSize + 255) / 255;
        int headerSize = numSegments + 27;
        int outputPacketSize2 = headerSize + inputBufferSize;
        if (this.pageSequenceNumber != 2) {
            outputPacketSize = outputPacketSize2;
            oggIdHeaderPageSize = 0;
        } else {
            if (providedOggIdHeaderPayloadBytes != null) {
                length = providedOggIdHeaderPayloadBytes.length + 28;
            } else {
                length = OGG_DEFAULT_ID_HEADER_PAGE.length;
            }
            int oggIdHeaderPageSize2 = length;
            outputPacketSize = outputPacketSize2 + OGG_DEFAULT_COMMENT_HEADER_PAGE.length + oggIdHeaderPageSize2;
            oggIdHeaderPageSize = oggIdHeaderPageSize2;
        }
        ByteBuffer buffer = replaceOutputBuffer(outputPacketSize);
        if (this.pageSequenceNumber == 2) {
            if (providedOggIdHeaderPayloadBytes != null) {
                writeOggIdHeaderPage(buffer, providedOggIdHeaderPayloadBytes);
            } else {
                buffer.put(OGG_DEFAULT_ID_HEADER_PAGE);
            }
            buffer.put(OGG_DEFAULT_COMMENT_HEADER_PAGE);
        }
        int numSamples = OpusUtil.parsePacketAudioSampleCount(inputBuffer);
        this.granulePosition += numSamples;
        writeOggPacketHeader(buffer, this.granulePosition, this.pageSequenceNumber, numSegments, false);
        int bytesLeft = inputBufferSize;
        for (int i = 0; i < numSegments; i++) {
            if (bytesLeft >= 255) {
                buffer.put((byte) -1);
                bytesLeft -= 255;
            } else {
                buffer.put((byte) bytesLeft);
                bytesLeft = 0;
            }
        }
        for (int i2 = position; i2 < limit; i2++) {
            buffer.put(inputBuffer.get(i2));
        }
        int i3 = inputBuffer.limit();
        inputBuffer.position(i3);
        buffer.flip();
        if (this.pageSequenceNumber == 2) {
            int checksum = Util.crc32(buffer.array(), buffer.arrayOffset() + oggIdHeaderPageSize + OGG_DEFAULT_COMMENT_HEADER_PAGE.length, buffer.limit() - buffer.position(), 0);
            buffer.putInt(oggIdHeaderPageSize + OGG_DEFAULT_COMMENT_HEADER_PAGE.length + 22, checksum);
        } else {
            int checksum2 = Util.crc32(buffer.array(), buffer.arrayOffset(), buffer.limit() - buffer.position(), 0);
            buffer.putInt(22, checksum2);
        }
        this.pageSequenceNumber++;
        return buffer;
    }

    private void writeOggIdHeaderPage(ByteBuffer buffer, byte[] idHeaderPayloadBytes) {
        writeOggPacketHeader(buffer, 0L, 0, 1, true);
        buffer.put(UnsignedBytes.checkedCast(idHeaderPayloadBytes.length));
        buffer.put(idHeaderPayloadBytes);
        int checksum = Util.crc32(buffer.array(), buffer.arrayOffset(), idHeaderPayloadBytes.length + 28, 0);
        buffer.putInt(22, checksum);
        buffer.position(idHeaderPayloadBytes.length + 28);
    }

    private void writeOggPacketHeader(ByteBuffer byteBuffer, long granulePosition, int pageSequenceNumber, int numberPageSegments, boolean isIdHeaderPacket) {
        byteBuffer.put((byte) 79);
        byteBuffer.put((byte) 103);
        byteBuffer.put((byte) 103);
        byteBuffer.put((byte) 83);
        byteBuffer.put((byte) 0);
        byteBuffer.put(isIdHeaderPacket ? (byte) 2 : (byte) 0);
        byteBuffer.putLong(granulePosition);
        byteBuffer.putInt(0);
        byteBuffer.putInt(pageSequenceNumber);
        byteBuffer.putInt(0);
        byteBuffer.put(UnsignedBytes.checkedCast(numberPageSegments));
    }

    private ByteBuffer replaceOutputBuffer(int size) {
        if (this.outputBuffer.capacity() < size) {
            this.outputBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        } else {
            this.outputBuffer.clear();
        }
        return this.outputBuffer;
    }
}
