package androidx.media3.extractor;

import androidx.media3.common.ParserException;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ts.PsExtractor;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class FlacFrameReader {

    public static final class SampleNumberHolder {
        public long sampleNumber;
    }

    public static boolean checkAndReadFrameHeader(ParsableByteArray data, FlacStreamMetadata flacStreamMetadata, int frameStartMarker, SampleNumberHolder sampleNumberHolder) {
        int frameStartPosition = data.getPosition();
        long frameHeaderBytes = data.readUnsignedInt();
        if ((frameHeaderBytes >>> 16) != frameStartMarker) {
            return false;
        }
        boolean isBlockSizeVariable = ((frameHeaderBytes >>> 16) & 1) == 1;
        int blockSizeKey = (int) ((frameHeaderBytes >> 12) & 15);
        int sampleRateKey = (int) ((frameHeaderBytes >> 8) & 15);
        int channelAssignmentKey = (int) (15 & (frameHeaderBytes >> 4));
        int bitsPerSampleKey = (int) ((frameHeaderBytes >> 1) & 7);
        boolean reservedBit = (frameHeaderBytes & 1) == 1;
        if (checkChannelAssignment(channelAssignmentKey, flacStreamMetadata) && checkBitsPerSample(bitsPerSampleKey, flacStreamMetadata) && !reservedBit) {
            if (checkAndReadFirstSampleNumber(data, flacStreamMetadata, isBlockSizeVariable, sampleNumberHolder) && checkAndReadBlockSizeSamples(data, flacStreamMetadata, blockSizeKey) && checkAndReadSampleRate(data, flacStreamMetadata, sampleRateKey) && checkAndReadCrc(data, frameStartPosition)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkFrameHeaderFromPeek(ExtractorInput input, FlacStreamMetadata flacStreamMetadata, int frameStartMarker, SampleNumberHolder sampleNumberHolder) throws IOException {
        long originalPeekPosition = input.getPeekPosition();
        byte[] frameStartBytes = new byte[2];
        input.peekFully(frameStartBytes, 0, 2);
        int frameStart = ((frameStartBytes[0] & 255) << 8) | (frameStartBytes[1] & 255);
        if (frameStart != frameStartMarker) {
            input.resetPeekPosition();
            input.advancePeekPosition((int) (originalPeekPosition - input.getPosition()));
            return false;
        }
        ParsableByteArray scratch = new ParsableByteArray(16);
        System.arraycopy(frameStartBytes, 0, scratch.getData(), 0, 2);
        int totalBytesPeeked = ExtractorUtil.peekToLength(input, scratch.getData(), 2, 14);
        scratch.setLimit(totalBytesPeeked);
        input.resetPeekPosition();
        input.advancePeekPosition((int) (originalPeekPosition - input.getPosition()));
        return checkAndReadFrameHeader(scratch, flacStreamMetadata, frameStartMarker, sampleNumberHolder);
    }

    public static long getFirstSampleNumber(ExtractorInput input, FlacStreamMetadata flacStreamMetadata) throws IOException {
        input.resetPeekPosition();
        boolean isBlockSizeVariable = true;
        input.advancePeekPosition(1);
        byte[] blockingStrategyByte = new byte[1];
        input.peekFully(blockingStrategyByte, 0, 1);
        if ((blockingStrategyByte[0] & 1) != 1) {
            isBlockSizeVariable = false;
        }
        input.advancePeekPosition(2);
        int maxUtf8SampleNumberSize = isBlockSizeVariable ? 7 : 6;
        ParsableByteArray scratch = new ParsableByteArray(maxUtf8SampleNumberSize);
        int totalBytesPeeked = ExtractorUtil.peekToLength(input, scratch.getData(), 0, maxUtf8SampleNumberSize);
        scratch.setLimit(totalBytesPeeked);
        input.resetPeekPosition();
        SampleNumberHolder sampleNumberHolder = new SampleNumberHolder();
        if (!checkAndReadFirstSampleNumber(scratch, flacStreamMetadata, isBlockSizeVariable, sampleNumberHolder)) {
            throw ParserException.createForMalformedContainer(null, null);
        }
        return sampleNumberHolder.sampleNumber;
    }

    public static int readFrameBlockSizeSamplesFromKey(ParsableByteArray data, int blockSizeKey) {
        switch (blockSizeKey) {
            case 1:
                return PsExtractor.AUDIO_STREAM;
            case 2:
            case 3:
            case 4:
            case 5:
                return 576 << (blockSizeKey - 2);
            case 6:
                return data.readUnsignedByte() + 1;
            case 7:
                return data.readUnsignedShort() + 1;
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                return 256 << (blockSizeKey - 8);
            default:
                return -1;
        }
    }

    private static boolean checkChannelAssignment(int channelAssignmentKey, FlacStreamMetadata flacStreamMetadata) {
        if (channelAssignmentKey <= 7) {
            return channelAssignmentKey == flacStreamMetadata.channels - 1;
        }
        return channelAssignmentKey <= 10 && flacStreamMetadata.channels == 2;
    }

    private static boolean checkBitsPerSample(int bitsPerSampleKey, FlacStreamMetadata flacStreamMetadata) {
        return bitsPerSampleKey == 0 || bitsPerSampleKey == flacStreamMetadata.bitsPerSampleLookupKey;
    }

    private static boolean checkAndReadFirstSampleNumber(ParsableByteArray data, FlacStreamMetadata flacStreamMetadata, boolean isBlockSizeVariable, SampleNumberHolder sampleNumberHolder) {
        try {
            long utf8Value = data.readUtf8EncodedLong();
            sampleNumberHolder.sampleNumber = isBlockSizeVariable ? utf8Value : ((long) flacStreamMetadata.maxBlockSizeSamples) * utf8Value;
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean checkAndReadBlockSizeSamples(ParsableByteArray data, FlacStreamMetadata flacStreamMetadata, int blockSizeKey) {
        int blockSizeSamples = readFrameBlockSizeSamplesFromKey(data, blockSizeKey);
        return blockSizeSamples != -1 && blockSizeSamples <= flacStreamMetadata.maxBlockSizeSamples;
    }

    private static boolean checkAndReadSampleRate(ParsableByteArray data, FlacStreamMetadata flacStreamMetadata, int sampleRateKey) {
        int expectedSampleRate = flacStreamMetadata.sampleRate;
        if (sampleRateKey == 0) {
            return true;
        }
        if (sampleRateKey <= 11) {
            return sampleRateKey == flacStreamMetadata.sampleRateLookupKey;
        }
        if (sampleRateKey == 12) {
            return data.readUnsignedByte() * 1000 == expectedSampleRate;
        }
        if (sampleRateKey > 14) {
            return false;
        }
        int sampleRate = data.readUnsignedShort();
        if (sampleRateKey == 14) {
            sampleRate *= 10;
        }
        return sampleRate == expectedSampleRate;
    }

    private static boolean checkAndReadCrc(ParsableByteArray data, int frameStartPosition) {
        int crc = data.readUnsignedByte();
        int frameEndPosition = data.getPosition();
        int expectedCrc = Util.crc8(data.getData(), frameStartPosition, frameEndPosition - 1, 0);
        return crc == expectedCrc;
    }

    private FlacFrameReader() {
    }
}
