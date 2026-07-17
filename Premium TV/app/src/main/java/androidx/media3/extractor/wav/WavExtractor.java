package androidx.media3.extractor.wav;

import android.net.Uri;
import android.util.Pair;
import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.WavUtil;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.ts.TsExtractor;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class WavExtractor implements Extractor {
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() { // from class: androidx.media3.extractor.wav.WavExtractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.ExtractorsFactory
        public final Extractor[] createExtractors() {
            return WavExtractor.lambda$static$0();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ Extractor[] createExtractors(Uri uri, Map map) {
            return createExtractors();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory experimentalSetTextTrackTranscodingEnabled(boolean z) {
            return ExtractorsFactory.CC.$default$experimentalSetTextTrackTranscodingEnabled(this, z);
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory setSubtitleParserFactory(SubtitleParser.Factory factory) {
            return ExtractorsFactory.CC.$default$setSubtitleParserFactory(this, factory);
        }
    };
    private static final int STATE_READING_FILE_TYPE = 0;
    private static final int STATE_READING_FORMAT = 2;
    private static final int STATE_READING_RF64_SAMPLE_DATA_SIZE = 1;
    private static final int STATE_READING_SAMPLE_DATA = 4;
    private static final int STATE_SKIPPING_TO_SAMPLE_DATA = 3;
    private static final String TAG = "WavExtractor";
    private static final int TARGET_SAMPLES_PER_SECOND = 10;
    private ExtractorOutput extractorOutput;
    private OutputWriter outputWriter;
    private TrackOutput trackOutput;
    private int state = 0;
    private long rf64SampleDataSize = -1;
    private int dataStartPosition = -1;
    private long dataEndPosition = -1;

    private interface OutputWriter {
        void init(int i, long j) throws ParserException;

        void reset(long j);

        boolean sampleData(ExtractorInput extractorInput, long j) throws IOException;
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ List getSniffFailureDetails() {
        return ImmutableList.of();
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ Extractor getUnderlyingImplementation() {
        return Extractor.CC.$default$getUnderlyingImplementation(this);
    }

    static /* synthetic */ Extractor[] lambda$static$0() {
        return new Extractor[]{new WavExtractor()};
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        return WavHeaderReader.checkFileType(input);
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
        this.trackOutput = output.track(0, 1);
        output.endTracks();
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        this.state = position == 0 ? 0 : 4;
        if (this.outputWriter != null) {
            this.outputWriter.reset(timeUs);
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        assertInitialized();
        switch (this.state) {
            case 0:
                readFileType(input);
                return 0;
            case 1:
                readRf64SampleDataSize(input);
                return 0;
            case 2:
                readFormat(input);
                return 0;
            case 3:
                skipToSampleData(input);
                return 0;
            case 4:
                return readSampleData(input);
            default:
                throw new IllegalStateException();
        }
    }

    @EnsuresNonNull({"extractorOutput", "trackOutput"})
    private void assertInitialized() {
        Assertions.checkStateNotNull(this.trackOutput);
        Util.castNonNull(this.extractorOutput);
    }

    private void readFileType(ExtractorInput input) throws IOException {
        Assertions.checkState(input.getPosition() == 0);
        if (this.dataStartPosition != -1) {
            input.skipFully(this.dataStartPosition);
            this.state = 4;
        } else {
            if (!WavHeaderReader.checkFileType(input)) {
                throw ParserException.createForMalformedContainer("Unsupported or unrecognized wav file type.", null);
            }
            input.skipFully((int) (input.getPeekPosition() - input.getPosition()));
            this.state = 1;
        }
    }

    private void readRf64SampleDataSize(ExtractorInput input) throws IOException {
        this.rf64SampleDataSize = WavHeaderReader.readRf64SampleDataSize(input);
        this.state = 2;
    }

    @RequiresNonNull({"extractorOutput", "trackOutput"})
    private void readFormat(ExtractorInput input) throws IOException {
        WavFormat wavFormat = WavHeaderReader.readFormat(input);
        if (wavFormat.formatType == 17) {
            this.outputWriter = new ImaAdPcmOutputWriter(this.extractorOutput, this.trackOutput, wavFormat);
        } else if (wavFormat.formatType == 6) {
            this.outputWriter = new PassthroughOutputWriter(this.extractorOutput, this.trackOutput, wavFormat, MimeTypes.AUDIO_ALAW, -1);
        } else if (wavFormat.formatType == 7) {
            this.outputWriter = new PassthroughOutputWriter(this.extractorOutput, this.trackOutput, wavFormat, MimeTypes.AUDIO_MLAW, -1);
        } else {
            int pcmEncoding = WavUtil.getPcmEncodingForType(wavFormat.formatType, wavFormat.bitsPerSample);
            if (pcmEncoding == 0) {
                throw ParserException.createForUnsupportedContainerFeature("Unsupported WAV format type: " + wavFormat.formatType);
            }
            this.outputWriter = new PassthroughOutputWriter(this.extractorOutput, this.trackOutput, wavFormat, MimeTypes.AUDIO_RAW, pcmEncoding);
        }
        this.state = 3;
    }

    private void skipToSampleData(ExtractorInput input) throws IOException {
        Pair<Long, Long> dataBounds = WavHeaderReader.skipToSampleData(input);
        this.dataStartPosition = ((Long) dataBounds.first).intValue();
        long dataSize = ((Long) dataBounds.second).longValue();
        if (this.rf64SampleDataSize != -1 && dataSize == 4294967295L) {
            dataSize = this.rf64SampleDataSize;
        }
        this.dataEndPosition = ((long) this.dataStartPosition) + dataSize;
        long inputLength = input.getLength();
        if (inputLength != -1 && this.dataEndPosition > inputLength) {
            Log.w(TAG, "Data exceeds input length: " + this.dataEndPosition + ", " + inputLength);
            this.dataEndPosition = inputLength;
        }
        ((OutputWriter) Assertions.checkNotNull(this.outputWriter)).init(this.dataStartPosition, this.dataEndPosition);
        this.state = 4;
    }

    private int readSampleData(ExtractorInput input) throws IOException {
        Assertions.checkState(this.dataEndPosition != -1);
        long bytesLeft = this.dataEndPosition - input.getPosition();
        return ((OutputWriter) Assertions.checkNotNull(this.outputWriter)).sampleData(input, bytesLeft) ? -1 : 0;
    }

    private static final class PassthroughOutputWriter implements OutputWriter {
        private final ExtractorOutput extractorOutput;
        private final Format format;
        private long outputFrameCount;
        private int pendingOutputBytes;
        private long startTimeUs;
        private final int targetSampleSizeBytes;
        private final TrackOutput trackOutput;
        private final WavFormat wavFormat;

        public PassthroughOutputWriter(ExtractorOutput extractorOutput, TrackOutput trackOutput, WavFormat wavFormat, String mimeType, int pcmEncoding) throws ParserException {
            this.extractorOutput = extractorOutput;
            this.trackOutput = trackOutput;
            this.wavFormat = wavFormat;
            int bytesPerFrame = (wavFormat.numChannels * wavFormat.bitsPerSample) / 8;
            if (wavFormat.blockSize != bytesPerFrame) {
                throw ParserException.createForMalformedContainer("Expected block size: " + bytesPerFrame + "; got: " + wavFormat.blockSize, null);
            }
            int constantBitrate = wavFormat.frameRateHz * bytesPerFrame * 8;
            this.targetSampleSizeBytes = Math.max(bytesPerFrame, (wavFormat.frameRateHz * bytesPerFrame) / 10);
            this.format = new Format.Builder().setSampleMimeType(mimeType).setAverageBitrate(constantBitrate).setPeakBitrate(constantBitrate).setMaxInputSize(this.targetSampleSizeBytes).setChannelCount(wavFormat.numChannels).setSampleRate(wavFormat.frameRateHz).setPcmEncoding(pcmEncoding).build();
        }

        @Override // androidx.media3.extractor.wav.WavExtractor.OutputWriter
        public void reset(long timeUs) {
            this.startTimeUs = timeUs;
            this.pendingOutputBytes = 0;
            this.outputFrameCount = 0L;
        }

        @Override // androidx.media3.extractor.wav.WavExtractor.OutputWriter
        public void init(int dataStartPosition, long dataEndPosition) {
            this.extractorOutput.seekMap(new WavSeekMap(this.wavFormat, 1, dataStartPosition, dataEndPosition));
            this.trackOutput.format(this.format);
        }

        @Override // androidx.media3.extractor.wav.WavExtractor.OutputWriter
        public boolean sampleData(ExtractorInput input, long bytesLeft) throws IOException {
            long bytesLeft2 = bytesLeft;
            while (bytesLeft2 > 0 && this.pendingOutputBytes < this.targetSampleSizeBytes) {
                int bytesToRead = (int) Math.min(this.targetSampleSizeBytes - this.pendingOutputBytes, bytesLeft2);
                int bytesAppended = this.trackOutput.sampleData((DataReader) input, bytesToRead, true);
                if (bytesAppended != -1) {
                    this.pendingOutputBytes += bytesAppended;
                    bytesLeft2 -= (long) bytesAppended;
                } else {
                    bytesLeft2 = 0;
                }
            }
            int bytesPerFrame = this.wavFormat.blockSize;
            int pendingFrames = this.pendingOutputBytes / bytesPerFrame;
            if (pendingFrames > 0) {
                long timeUs = this.startTimeUs + Util.scaleLargeTimestamp(this.outputFrameCount, 1000000L, this.wavFormat.frameRateHz);
                int size = pendingFrames * bytesPerFrame;
                int offset = this.pendingOutputBytes - size;
                this.trackOutput.sampleMetadata(timeUs, 1, size, offset, null);
                this.outputFrameCount += (long) pendingFrames;
                this.pendingOutputBytes = offset;
            }
            return bytesLeft2 <= 0;
        }
    }

    private static final class ImaAdPcmOutputWriter implements OutputWriter {
        private static final int[] INDEX_TABLE = {-1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8};
        private static final int[] STEP_TABLE = {7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, TsExtractor.TS_STREAM_TYPE_HDMV_DTS, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767};
        private final ParsableByteArray decodedData;
        private final ExtractorOutput extractorOutput;
        private final Format format;
        private final int framesPerBlock;
        private final byte[] inputData;
        private long outputFrameCount;
        private int pendingInputBytes;
        private int pendingOutputBytes;
        private long startTimeUs;
        private final int targetSampleSizeFrames;
        private final TrackOutput trackOutput;
        private final WavFormat wavFormat;

        public ImaAdPcmOutputWriter(ExtractorOutput extractorOutput, TrackOutput trackOutput, WavFormat wavFormat) throws ParserException {
            this.extractorOutput = extractorOutput;
            this.trackOutput = trackOutput;
            this.wavFormat = wavFormat;
            this.targetSampleSizeFrames = Math.max(1, wavFormat.frameRateHz / 10);
            ParsableByteArray scratch = new ParsableByteArray(wavFormat.extraData);
            scratch.readLittleEndianUnsignedShort();
            this.framesPerBlock = scratch.readLittleEndianUnsignedShort();
            int numChannels = wavFormat.numChannels;
            int expectedFramesPerBlock = (((wavFormat.blockSize - (numChannels * 4)) * 8) / (wavFormat.bitsPerSample * numChannels)) + 1;
            if (this.framesPerBlock != expectedFramesPerBlock) {
                throw ParserException.createForMalformedContainer("Expected frames per block: " + expectedFramesPerBlock + "; got: " + this.framesPerBlock, null);
            }
            int maxBlocksToDecode = Util.ceilDivide(this.targetSampleSizeFrames, this.framesPerBlock);
            this.inputData = new byte[wavFormat.blockSize * maxBlocksToDecode];
            this.decodedData = new ParsableByteArray(numOutputFramesToBytes(this.framesPerBlock, numChannels) * maxBlocksToDecode);
            int constantBitrate = ((wavFormat.frameRateHz * wavFormat.blockSize) * 8) / this.framesPerBlock;
            this.format = new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_RAW).setAverageBitrate(constantBitrate).setPeakBitrate(constantBitrate).setMaxInputSize(numOutputFramesToBytes(this.targetSampleSizeFrames, numChannels)).setChannelCount(wavFormat.numChannels).setSampleRate(wavFormat.frameRateHz).setPcmEncoding(2).build();
        }

        @Override // androidx.media3.extractor.wav.WavExtractor.OutputWriter
        public void reset(long timeUs) {
            this.pendingInputBytes = 0;
            this.startTimeUs = timeUs;
            this.pendingOutputBytes = 0;
            this.outputFrameCount = 0L;
        }

        @Override // androidx.media3.extractor.wav.WavExtractor.OutputWriter
        public void init(int dataStartPosition, long dataEndPosition) {
            this.extractorOutput.seekMap(new WavSeekMap(this.wavFormat, this.framesPerBlock, dataStartPosition, dataEndPosition));
            this.trackOutput.format(this.format);
        }

        @Override // androidx.media3.extractor.wav.WavExtractor.OutputWriter
        public boolean sampleData(ExtractorInput input, long bytesLeft) throws IOException {
            int pendingOutputFrames;
            int targetFramesRemaining = this.targetSampleSizeFrames - numOutputBytesToFrames(this.pendingOutputBytes);
            int blocksToDecode = Util.ceilDivide(targetFramesRemaining, this.framesPerBlock);
            int targetReadBytes = this.wavFormat.blockSize * blocksToDecode;
            boolean endOfSampleData = bytesLeft == 0;
            while (!endOfSampleData && this.pendingInputBytes < targetReadBytes) {
                int bytesToRead = (int) Math.min(targetReadBytes - this.pendingInputBytes, bytesLeft);
                int bytesAppended = input.read(this.inputData, this.pendingInputBytes, bytesToRead);
                if (bytesAppended == -1) {
                    endOfSampleData = true;
                } else {
                    this.pendingInputBytes += bytesAppended;
                }
            }
            int pendingBlockCount = this.pendingInputBytes / this.wavFormat.blockSize;
            if (pendingBlockCount > 0) {
                decode(this.inputData, pendingBlockCount, this.decodedData);
                this.pendingInputBytes -= this.wavFormat.blockSize * pendingBlockCount;
                int decodedDataSize = this.decodedData.limit();
                this.trackOutput.sampleData(this.decodedData, decodedDataSize);
                this.pendingOutputBytes += decodedDataSize;
                if (numOutputBytesToFrames(this.pendingOutputBytes) >= this.targetSampleSizeFrames) {
                    writeSampleMetadata(this.targetSampleSizeFrames);
                }
            }
            if (endOfSampleData && (pendingOutputFrames = numOutputBytesToFrames(this.pendingOutputBytes)) > 0) {
                writeSampleMetadata(pendingOutputFrames);
            }
            return endOfSampleData;
        }

        private void writeSampleMetadata(int sampleFrames) {
            long timeUs = this.startTimeUs + Util.scaleLargeTimestamp(this.outputFrameCount, 1000000L, this.wavFormat.frameRateHz);
            int size = numOutputFramesToBytes(sampleFrames);
            int offset = this.pendingOutputBytes - size;
            this.trackOutput.sampleMetadata(timeUs, 1, size, offset, null);
            this.outputFrameCount += (long) sampleFrames;
            this.pendingOutputBytes -= size;
        }

        private void decode(byte[] input, int blockCount, ParsableByteArray output) {
            for (int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
                for (int channelIndex = 0; channelIndex < this.wavFormat.numChannels; channelIndex++) {
                    decodeBlockForChannel(input, blockIndex, channelIndex, output.getData());
                }
            }
            int blockIndex2 = this.framesPerBlock;
            int decodedDataSize = numOutputFramesToBytes(blockIndex2 * blockCount);
            output.setPosition(0);
            output.setLimit(decodedDataSize);
        }

        private void decodeBlockForChannel(byte[] input, int blockIndex, int channelIndex, byte[] output) {
            int originalSample;
            int blockSize = this.wavFormat.blockSize;
            int numChannels = this.wavFormat.numChannels;
            int blockStartIndex = blockIndex * blockSize;
            int headerStartIndex = (channelIndex * 4) + blockStartIndex;
            int dataStartIndex = (numChannels * 4) + headerStartIndex;
            int dataSizeBytes = (blockSize / numChannels) - 4;
            int predictedSample = (short) (((input[headerStartIndex + 1] & 255) << 8) | (input[headerStartIndex] & 255));
            int stepIndex = Math.min(input[headerStartIndex + 2] & 255, 88);
            int step = STEP_TABLE[stepIndex];
            int outputIndex = ((this.framesPerBlock * blockIndex * numChannels) + channelIndex) * 2;
            output[outputIndex] = (byte) (predictedSample & 255);
            output[outputIndex + 1] = (byte) (predictedSample >> 8);
            int i = 0;
            while (i < dataSizeBytes * 2) {
                int dataSegmentIndex = i / 8;
                int dataSegmentOffset = (i / 2) % 4;
                int dataIndex = (dataSegmentIndex * numChannels * 4) + dataStartIndex + dataSegmentOffset;
                int originalSample2 = input[dataIndex] & 255;
                if (i % 2 == 0) {
                    originalSample = originalSample2 & 15;
                } else {
                    originalSample = originalSample2 >> 4;
                }
                int delta = originalSample & 7;
                int difference = (((delta * 2) + 1) * step) >> 3;
                if ((originalSample & 8) != 0) {
                    difference = -difference;
                }
                predictedSample = Util.constrainValue(predictedSample + difference, -32768, 32767);
                outputIndex += numChannels * 2;
                output[outputIndex] = (byte) (predictedSample & 255);
                output[outputIndex + 1] = (byte) (predictedSample >> 8);
                stepIndex = Util.constrainValue(stepIndex + INDEX_TABLE[originalSample], 0, STEP_TABLE.length - 1);
                step = STEP_TABLE[stepIndex];
                i++;
                blockSize = blockSize;
            }
        }

        private int numOutputBytesToFrames(int bytes) {
            return bytes / (this.wavFormat.numChannels * 2);
        }

        private int numOutputFramesToBytes(int frames) {
            return numOutputFramesToBytes(frames, this.wavFormat.numChannels);
        }

        private static int numOutputFramesToBytes(int frames, int numChannels) {
            return frames * 2 * numChannels;
        }
    }
}
