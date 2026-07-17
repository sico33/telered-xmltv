package androidx.media3.extractor.wav;

import android.util.Pair;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.WavUtil;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class WavHeaderReader {
    private static final String TAG = "WavHeaderReader";

    public static boolean checkFileType(ExtractorInput input) throws IOException {
        ParsableByteArray scratch = new ParsableByteArray(8);
        ChunkHeader chunkHeader = ChunkHeader.peek(input, scratch);
        if (chunkHeader.id != 1380533830 && chunkHeader.id != 1380333108) {
            return false;
        }
        input.peekFully(scratch.getData(), 0, 4);
        scratch.setPosition(0);
        int formType = scratch.readInt();
        if (formType != 1463899717) {
            Log.e(TAG, "Unsupported form type: " + formType);
            return false;
        }
        return true;
    }

    public static long readRf64SampleDataSize(ExtractorInput input) throws IOException {
        ParsableByteArray scratch = new ParsableByteArray(8);
        ChunkHeader chunkHeader = ChunkHeader.peek(input, scratch);
        if (chunkHeader.id != 1685272116) {
            input.resetPeekPosition();
            return -1L;
        }
        input.advancePeekPosition(8);
        scratch.setPosition(0);
        input.peekFully(scratch.getData(), 0, 8);
        long sampleDataSize = scratch.readLittleEndianLong();
        input.skipFully(((int) chunkHeader.size) + 8);
        return sampleDataSize;
    }

    public static WavFormat readFormat(ExtractorInput input) throws IOException {
        byte[] extraData;
        ParsableByteArray scratch = new ParsableByteArray(16);
        ChunkHeader chunkHeader = skipToChunk(WavUtil.FMT_FOURCC, input, scratch);
        Assertions.checkState(chunkHeader.size >= 16);
        input.peekFully(scratch.getData(), 0, 16);
        scratch.setPosition(0);
        int audioFormatType = scratch.readLittleEndianUnsignedShort();
        int numChannels = scratch.readLittleEndianUnsignedShort();
        int frameRateHz = scratch.readLittleEndianUnsignedIntToInt();
        int averageBytesPerSecond = scratch.readLittleEndianUnsignedIntToInt();
        int blockSize = scratch.readLittleEndianUnsignedShort();
        int bitsPerSample = scratch.readLittleEndianUnsignedShort();
        int bytesLeft = ((int) chunkHeader.size) - 16;
        if (bytesLeft > 0) {
            byte[] extraData2 = new byte[bytesLeft];
            input.peekFully(extraData2, 0, bytesLeft);
            extraData = extraData2;
        } else {
            byte[] extraData3 = Util.EMPTY_BYTE_ARRAY;
            extraData = extraData3;
        }
        input.skipFully((int) (input.getPeekPosition() - input.getPosition()));
        return new WavFormat(audioFormatType, numChannels, frameRateHz, averageBytesPerSecond, blockSize, bitsPerSample, extraData);
    }

    public static Pair<Long, Long> skipToSampleData(ExtractorInput input) throws IOException {
        input.resetPeekPosition();
        ParsableByteArray scratch = new ParsableByteArray(8);
        ChunkHeader chunkHeader = skipToChunk(1684108385, input, scratch);
        input.skipFully(8);
        long dataStartPosition = input.getPosition();
        return Pair.create(Long.valueOf(dataStartPosition), Long.valueOf(chunkHeader.size));
    }

    private static ChunkHeader skipToChunk(int chunkId, ExtractorInput input, ParsableByteArray scratch) throws IOException {
        ChunkHeader chunkHeader = ChunkHeader.peek(input, scratch);
        while (chunkHeader.id != chunkId) {
            Log.w(TAG, "Ignoring unknown WAV chunk: " + chunkHeader.id);
            long bytesToSkip = chunkHeader.size + 8;
            if (chunkHeader.size % 2 != 0) {
                bytesToSkip++;
            }
            if (bytesToSkip > 2147483647L) {
                throw ParserException.createForUnsupportedContainerFeature("Chunk is too large (~2GB+) to skip; id: " + chunkHeader.id);
            }
            input.skipFully((int) bytesToSkip);
            chunkHeader = ChunkHeader.peek(input, scratch);
        }
        return chunkHeader;
    }

    private WavHeaderReader() {
    }

    private static final class ChunkHeader {
        public static final int SIZE_IN_BYTES = 8;
        public final int id;
        public final long size;

        private ChunkHeader(int id, long size) {
            this.id = id;
            this.size = size;
        }

        public static ChunkHeader peek(ExtractorInput input, ParsableByteArray scratch) throws IOException {
            input.peekFully(scratch.getData(), 0, 8);
            scratch.setPosition(0);
            int id = scratch.readInt();
            long size = scratch.readLittleEndianUnsignedInt();
            return new ChunkHeader(id, size);
        }
    }
}
