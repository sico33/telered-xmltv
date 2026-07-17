package androidx.media3.extractor.ts;

import androidx.core.app.FrameMetricsAggregator;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.AacUtil;
import androidx.media3.extractor.DiscardingTrackOutput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import java.util.Arrays;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class AdtsReader implements ElementaryStreamReader {
    private static final int CRC_SIZE = 2;
    private static final int HEADER_SIZE = 5;
    private static final int ID3_HEADER_SIZE = 10;
    private static final byte[] ID3_IDENTIFIER = {73, 68, 51};
    private static final int ID3_SIZE_OFFSET = 6;
    private static final int MATCH_STATE_FF = 512;
    private static final int MATCH_STATE_I = 768;
    private static final int MATCH_STATE_ID = 1024;
    private static final int MATCH_STATE_START = 256;
    private static final int MATCH_STATE_VALUE_SHIFT = 8;
    private static final int STATE_CHECKING_ADTS_HEADER = 1;
    private static final int STATE_FINDING_SAMPLE = 0;
    private static final int STATE_READING_ADTS_HEADER = 3;
    private static final int STATE_READING_ID3_HEADER = 2;
    private static final int STATE_READING_SAMPLE = 4;
    private static final String TAG = "AdtsReader";
    private static final int VERSION_UNSET = -1;
    private final ParsableBitArray adtsScratch;
    private int bytesRead;
    private int currentFrameVersion;
    private TrackOutput currentOutput;
    private long currentSampleDuration;
    private final boolean exposeId3;
    private int firstFrameSampleRateIndex;
    private int firstFrameVersion;
    private String formatId;
    private boolean foundFirstFrame;
    private boolean hasCrc;
    private boolean hasOutputFormat;
    private final ParsableByteArray id3HeaderBuffer;
    private TrackOutput id3Output;
    private final String language;
    private int matchState;
    private TrackOutput output;
    private final int roleFlags;
    private long sampleDurationUs;
    private int sampleSize;
    private int state;
    private long timeUs;

    public AdtsReader(boolean exposeId3) {
        this(exposeId3, null, 0);
    }

    public AdtsReader(boolean exposeId3, String language, int roleFlags) {
        this.adtsScratch = new ParsableBitArray(new byte[7]);
        this.id3HeaderBuffer = new ParsableByteArray(Arrays.copyOf(ID3_IDENTIFIER, 10));
        setFindingSampleState();
        this.firstFrameVersion = -1;
        this.firstFrameSampleRateIndex = -1;
        this.sampleDurationUs = C.TIME_UNSET;
        this.timeUs = C.TIME_UNSET;
        this.exposeId3 = exposeId3;
        this.language = language;
        this.roleFlags = roleFlags;
    }

    public static boolean isAdtsSyncWord(int candidateSyncWord) {
        return (65526 & candidateSyncWord) == 65520;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.timeUs = C.TIME_UNSET;
        resetSync();
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        idGenerator.generateNewId();
        this.formatId = idGenerator.getFormatId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 1);
        this.currentOutput = this.output;
        if (this.exposeId3) {
            idGenerator.generateNewId();
            this.id3Output = extractorOutput.track(idGenerator.getTrackId(), 5);
            this.id3Output.format(new Format.Builder().setId(idGenerator.getFormatId()).setSampleMimeType(MimeTypes.APPLICATION_ID3).build());
            return;
        }
        this.id3Output = new DiscardingTrackOutput();
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetStarted(long pesTimeUs, int flags) {
        this.timeUs = pesTimeUs;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void consume(ParsableByteArray data) throws ParserException {
        assertTracksCreated();
        while (data.bytesLeft() > 0) {
            switch (this.state) {
                case 0:
                    findNextSample(data);
                    break;
                case 1:
                    checkAdtsHeader(data);
                    break;
                case 2:
                    if (continueRead(data, this.id3HeaderBuffer.getData(), 10)) {
                        parseId3Header();
                    }
                    break;
                case 3:
                    int targetLength = this.hasCrc ? 7 : 5;
                    if (continueRead(data, this.adtsScratch.data, targetLength)) {
                        parseAdtsHeader();
                    }
                    break;
                case 4:
                    readSample(data);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetFinished(boolean isEndOfInput) {
    }

    public long getSampleDurationUs() {
        return this.sampleDurationUs;
    }

    private void resetSync() {
        this.foundFirstFrame = false;
        setFindingSampleState();
    }

    private boolean continueRead(ParsableByteArray source, byte[] target, int targetLength) {
        int bytesToRead = Math.min(source.bytesLeft(), targetLength - this.bytesRead);
        source.readBytes(target, this.bytesRead, bytesToRead);
        this.bytesRead += bytesToRead;
        return this.bytesRead == targetLength;
    }

    private void setFindingSampleState() {
        this.state = 0;
        this.bytesRead = 0;
        this.matchState = 256;
    }

    private void setReadingId3HeaderState() {
        this.state = 2;
        this.bytesRead = ID3_IDENTIFIER.length;
        this.sampleSize = 0;
        this.id3HeaderBuffer.setPosition(0);
    }

    private void setReadingSampleState(TrackOutput outputToUse, long currentSampleDuration, int priorReadBytes, int sampleSize) {
        this.state = 4;
        this.bytesRead = priorReadBytes;
        this.currentOutput = outputToUse;
        this.currentSampleDuration = currentSampleDuration;
        this.sampleSize = sampleSize;
    }

    private void setReadingAdtsHeaderState() {
        this.state = 3;
        this.bytesRead = 0;
    }

    private void setCheckingAdtsHeaderState() {
        this.state = 1;
        this.bytesRead = 0;
    }

    private void findNextSample(ParsableByteArray pesBuffer) {
        byte[] adtsData = pesBuffer.getData();
        int data = pesBuffer.getPosition();
        int endOffset = pesBuffer.limit();
        while (data < endOffset) {
            int position = data + 1;
            int data2 = adtsData[data] & 255;
            if (this.matchState == 512 && isAdtsSyncBytes((byte) -1, (byte) data2) && (this.foundFirstFrame || checkSyncPositionValid(pesBuffer, position - 2))) {
                this.currentFrameVersion = (data2 & 8) >> 3;
                this.hasCrc = (data2 & 1) == 0;
                if (!this.foundFirstFrame) {
                    setCheckingAdtsHeaderState();
                } else {
                    setReadingAdtsHeaderState();
                }
                pesBuffer.setPosition(position);
                return;
            }
            switch (this.matchState | data2) {
                case 329:
                    this.matchState = MATCH_STATE_I;
                    data = position;
                    break;
                case FrameMetricsAggregator.EVERY_DURATION /* 511 */:
                    this.matchState = 512;
                    data = position;
                    break;
                case 836:
                    this.matchState = 1024;
                    data = position;
                    break;
                case 1075:
                    setReadingId3HeaderState();
                    pesBuffer.setPosition(position);
                    break;
                default:
                    if (this.matchState != 256) {
                        this.matchState = 256;
                        data = position - 1;
                    } else {
                        data = position;
                    }
                    break;
            }
            return;
        }
        pesBuffer.setPosition(data);
    }

    private void checkAdtsHeader(ParsableByteArray buffer) {
        if (buffer.bytesLeft() == 0) {
            return;
        }
        this.adtsScratch.data[0] = buffer.getData()[buffer.getPosition()];
        this.adtsScratch.setPosition(2);
        int currentFrameSampleRateIndex = this.adtsScratch.readBits(4);
        if (this.firstFrameSampleRateIndex != -1 && currentFrameSampleRateIndex != this.firstFrameSampleRateIndex) {
            resetSync();
            return;
        }
        if (!this.foundFirstFrame) {
            this.foundFirstFrame = true;
            this.firstFrameVersion = this.currentFrameVersion;
            this.firstFrameSampleRateIndex = currentFrameSampleRateIndex;
        }
        setReadingAdtsHeaderState();
    }

    private boolean checkSyncPositionValid(ParsableByteArray pesBuffer, int syncPositionCandidate) {
        pesBuffer.setPosition(syncPositionCandidate + 1);
        if (!tryRead(pesBuffer, this.adtsScratch.data, 1)) {
            return false;
        }
        this.adtsScratch.setPosition(4);
        int currentFrameVersion = this.adtsScratch.readBits(1);
        if (this.firstFrameVersion != -1 && currentFrameVersion != this.firstFrameVersion) {
            return false;
        }
        if (this.firstFrameSampleRateIndex != -1) {
            if (!tryRead(pesBuffer, this.adtsScratch.data, 1)) {
                return true;
            }
            this.adtsScratch.setPosition(2);
            int currentFrameSampleRateIndex = this.adtsScratch.readBits(4);
            if (currentFrameSampleRateIndex != this.firstFrameSampleRateIndex) {
                return false;
            }
            pesBuffer.setPosition(syncPositionCandidate + 2);
        }
        if (!tryRead(pesBuffer, this.adtsScratch.data, 4)) {
            return true;
        }
        this.adtsScratch.setPosition(14);
        int frameSize = this.adtsScratch.readBits(13);
        if (frameSize < 7) {
            return false;
        }
        byte[] data = pesBuffer.getData();
        int dataLimit = pesBuffer.limit();
        int nextSyncPosition = syncPositionCandidate + frameSize;
        if (nextSyncPosition >= dataLimit) {
            return true;
        }
        if (data[nextSyncPosition] == -1) {
            if (nextSyncPosition + 1 == dataLimit) {
                return true;
            }
            return isAdtsSyncBytes((byte) -1, data[nextSyncPosition + 1]) && ((data[nextSyncPosition + 1] & 8) >> 3) == currentFrameVersion;
        }
        if (data[nextSyncPosition] != 73) {
            return false;
        }
        if (nextSyncPosition + 1 == dataLimit) {
            return true;
        }
        if (data[nextSyncPosition + 1] != 68) {
            return false;
        }
        return nextSyncPosition + 2 == dataLimit || data[nextSyncPosition + 2] == 51;
    }

    private boolean isAdtsSyncBytes(byte firstByte, byte secondByte) {
        int syncWord = ((firstByte & 255) << 8) | (secondByte & 255);
        return isAdtsSyncWord(syncWord);
    }

    private boolean tryRead(ParsableByteArray source, byte[] target, int targetLength) {
        if (source.bytesLeft() < targetLength) {
            return false;
        }
        source.readBytes(target, 0, targetLength);
        return true;
    }

    @RequiresNonNull({"id3Output"})
    private void parseId3Header() {
        this.id3Output.sampleData(this.id3HeaderBuffer, 10);
        this.id3HeaderBuffer.setPosition(6);
        setReadingSampleState(this.id3Output, 0L, 10, this.id3HeaderBuffer.readSynchSafeInt() + 10);
    }

    @RequiresNonNull({"output"})
    private void parseAdtsHeader() throws ParserException {
        int sampleSize;
        this.adtsScratch.setPosition(0);
        boolean z = this.hasOutputFormat;
        ParsableBitArray parsableBitArray = this.adtsScratch;
        if (!z) {
            int audioObjectType = parsableBitArray.readBits(2) + 1;
            if (audioObjectType != 2) {
                Log.w(TAG, "Detected audio object type: " + audioObjectType + ", but assuming AAC LC.");
                audioObjectType = 2;
            }
            this.adtsScratch.skipBits(5);
            int channelConfig = this.adtsScratch.readBits(3);
            byte[] audioSpecificConfig = AacUtil.buildAudioSpecificConfig(audioObjectType, this.firstFrameSampleRateIndex, channelConfig);
            AacUtil.Config aacConfig = AacUtil.parseAudioSpecificConfig(audioSpecificConfig);
            Format format = new Format.Builder().setId(this.formatId).setSampleMimeType(MimeTypes.AUDIO_AAC).setCodecs(aacConfig.codecs).setChannelCount(aacConfig.channelCount).setSampleRate(aacConfig.sampleRateHz).setInitializationData(Collections.singletonList(audioSpecificConfig)).setLanguage(this.language).setRoleFlags(this.roleFlags).build();
            this.sampleDurationUs = 1024000000 / ((long) format.sampleRate);
            this.output.format(format);
            this.hasOutputFormat = true;
        } else {
            parsableBitArray.skipBits(10);
        }
        this.adtsScratch.skipBits(4);
        int sampleSize2 = (this.adtsScratch.readBits(13) - 2) - 5;
        if (!this.hasCrc) {
            sampleSize = sampleSize2;
        } else {
            sampleSize = sampleSize2 - 2;
        }
        setReadingSampleState(this.output, this.sampleDurationUs, 0, sampleSize);
    }

    @RequiresNonNull({"currentOutput"})
    private void readSample(ParsableByteArray data) {
        int bytesToRead = Math.min(data.bytesLeft(), this.sampleSize - this.bytesRead);
        this.currentOutput.sampleData(data, bytesToRead);
        this.bytesRead += bytesToRead;
        if (this.bytesRead == this.sampleSize) {
            Assertions.checkState(this.timeUs != C.TIME_UNSET);
            this.currentOutput.sampleMetadata(this.timeUs, 1, this.sampleSize, 0, null);
            this.timeUs += this.currentSampleDuration;
            setFindingSampleState();
        }
    }

    @EnsuresNonNull({"output", "currentOutput", "id3Output"})
    private void assertTracksCreated() {
        Assertions.checkNotNull(this.output);
        Util.castNonNull(this.currentOutput);
        Util.castNonNull(this.id3Output);
    }
}
