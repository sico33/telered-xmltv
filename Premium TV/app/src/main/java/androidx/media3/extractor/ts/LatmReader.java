package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.AacUtil;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class LatmReader implements ElementaryStreamReader {
    private static final int INITIAL_BUFFER_SIZE = 1024;
    private static final int STATE_FINDING_SYNC_1 = 0;
    private static final int STATE_FINDING_SYNC_2 = 1;
    private static final int STATE_READING_HEADER = 2;
    private static final int STATE_READING_SAMPLE = 3;
    private static final int SYNC_BYTE_FIRST = 86;
    private static final int SYNC_BYTE_SECOND = 224;
    private int audioMuxVersionA;
    private int bytesRead;
    private int channelCount;
    private String codecs;
    private Format format;
    private String formatId;
    private int frameLengthType;
    private final String language;
    private int numSubframes;
    private long otherDataLenBits;
    private boolean otherDataPresent;
    private TrackOutput output;
    private final int roleFlags;
    private long sampleDurationUs;
    private int sampleRateHz;
    private int sampleSize;
    private int secondHeaderByte;
    private int state;
    private boolean streamMuxRead;
    private final ParsableByteArray sampleDataBuffer = new ParsableByteArray(1024);
    private final ParsableBitArray sampleBitArray = new ParsableBitArray(this.sampleDataBuffer.getData());
    private long timeUs = C.TIME_UNSET;

    public LatmReader(String language, int roleFlags) {
        this.language = language;
        this.roleFlags = roleFlags;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.state = 0;
        this.timeUs = C.TIME_UNSET;
        this.streamMuxRead = false;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        idGenerator.generateNewId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 1);
        this.formatId = idGenerator.getFormatId();
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetStarted(long pesTimeUs, int flags) {
        this.timeUs = pesTimeUs;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void consume(ParsableByteArray data) throws ParserException {
        Assertions.checkStateNotNull(this.output);
        while (data.bytesLeft() > 0) {
            switch (this.state) {
                case 0:
                    if (data.readUnsignedByte() == SYNC_BYTE_FIRST) {
                        this.state = 1;
                    }
                    break;
                case 1:
                    int secondByte = data.readUnsignedByte();
                    if ((secondByte & 224) == 224) {
                        this.secondHeaderByte = secondByte;
                        this.state = 2;
                    } else if (secondByte != SYNC_BYTE_FIRST) {
                        this.state = 0;
                    }
                    break;
                case 2:
                    this.sampleSize = ((this.secondHeaderByte & (-225)) << 8) | data.readUnsignedByte();
                    if (this.sampleSize > this.sampleDataBuffer.getData().length) {
                        resetBufferForSize(this.sampleSize);
                    }
                    this.bytesRead = 0;
                    this.state = 3;
                    break;
                case 3:
                    int bytesToRead = Math.min(data.bytesLeft(), this.sampleSize - this.bytesRead);
                    data.readBytes(this.sampleBitArray.data, this.bytesRead, bytesToRead);
                    this.bytesRead += bytesToRead;
                    if (this.bytesRead == this.sampleSize) {
                        this.sampleBitArray.setPosition(0);
                        parseAudioMuxElement(this.sampleBitArray);
                        this.state = 0;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetFinished(boolean isEndOfInput) {
    }

    @RequiresNonNull({"output"})
    private void parseAudioMuxElement(ParsableBitArray data) throws ParserException {
        boolean useSameStreamMux = data.readBit();
        if (!useSameStreamMux) {
            this.streamMuxRead = true;
            parseStreamMuxConfig(data);
        } else if (!this.streamMuxRead) {
            return;
        }
        if (this.audioMuxVersionA == 0) {
            if (this.numSubframes != 0) {
                throw ParserException.createForMalformedContainer(null, null);
            }
            int muxSlotLengthBytes = parsePayloadLengthInfo(data);
            parsePayloadMux(data, muxSlotLengthBytes);
            if (this.otherDataPresent) {
                data.skipBits((int) this.otherDataLenBits);
                return;
            }
            return;
        }
        throw ParserException.createForMalformedContainer(null, null);
    }

    @RequiresNonNull({"output"})
    private void parseStreamMuxConfig(ParsableBitArray data) throws ParserException {
        int bits;
        boolean otherDataLenEsc;
        int audioMuxVersion = data.readBits(1);
        if (audioMuxVersion == 1) {
            bits = data.readBits(1);
        } else {
            bits = 0;
        }
        this.audioMuxVersionA = bits;
        if (this.audioMuxVersionA == 0) {
            if (audioMuxVersion == 1) {
                latmGetValue(data);
            }
            if (!data.readBit()) {
                throw ParserException.createForMalformedContainer(null, null);
            }
            this.numSubframes = data.readBits(6);
            int numProgram = data.readBits(4);
            int numLayer = data.readBits(3);
            if (numProgram != 0 || numLayer != 0) {
                throw ParserException.createForMalformedContainer(null, null);
            }
            if (audioMuxVersion == 0) {
                int startPosition = data.getPosition();
                int readBits = parseAudioSpecificConfig(data);
                data.setPosition(startPosition);
                byte[] initData = new byte[(readBits + 7) / 8];
                data.readBits(initData, 0, readBits);
                Format format = new Format.Builder().setId(this.formatId).setSampleMimeType(MimeTypes.AUDIO_AAC).setCodecs(this.codecs).setChannelCount(this.channelCount).setSampleRate(this.sampleRateHz).setInitializationData(Collections.singletonList(initData)).setLanguage(this.language).setRoleFlags(this.roleFlags).build();
                if (!format.equals(this.format)) {
                    this.format = format;
                    this.sampleDurationUs = 1024000000 / ((long) format.sampleRate);
                    this.output.format(format);
                }
            } else {
                int ascLen = (int) latmGetValue(data);
                int bitsRead = parseAudioSpecificConfig(data);
                data.skipBits(ascLen - bitsRead);
            }
            parseFrameLength(data);
            this.otherDataPresent = data.readBit();
            this.otherDataLenBits = 0L;
            if (this.otherDataPresent) {
                if (audioMuxVersion == 1) {
                    this.otherDataLenBits = latmGetValue(data);
                } else {
                    do {
                        otherDataLenEsc = data.readBit();
                        this.otherDataLenBits = (this.otherDataLenBits << 8) + ((long) data.readBits(8));
                    } while (otherDataLenEsc);
                }
            }
            boolean crcCheckPresent = data.readBit();
            if (crcCheckPresent) {
                data.skipBits(8);
                return;
            }
            return;
        }
        throw ParserException.createForMalformedContainer(null, null);
    }

    private void parseFrameLength(ParsableBitArray data) {
        this.frameLengthType = data.readBits(3);
        switch (this.frameLengthType) {
            case 0:
                data.skipBits(8);
                return;
            case 1:
                data.skipBits(9);
                return;
            case 2:
            default:
                throw new IllegalStateException();
            case 3:
            case 4:
            case 5:
                data.skipBits(6);
                return;
            case 6:
            case 7:
                data.skipBits(1);
                return;
        }
    }

    private int parseAudioSpecificConfig(ParsableBitArray data) throws ParserException {
        int bitsLeft = data.bitsLeft();
        AacUtil.Config config = AacUtil.parseAudioSpecificConfig(data, true);
        this.codecs = config.codecs;
        this.sampleRateHz = config.sampleRateHz;
        this.channelCount = config.channelCount;
        return bitsLeft - data.bitsLeft();
    }

    private int parsePayloadLengthInfo(ParsableBitArray data) throws ParserException {
        int tmp;
        int muxSlotLengthBytes = 0;
        if (this.frameLengthType == 0) {
            do {
                tmp = data.readBits(8);
                muxSlotLengthBytes += tmp;
            } while (tmp == 255);
            return muxSlotLengthBytes;
        }
        throw ParserException.createForMalformedContainer(null, null);
    }

    @RequiresNonNull({"output"})
    private void parsePayloadMux(ParsableBitArray data, int muxLengthBytes) {
        int bitPosition = data.getPosition();
        int i = bitPosition & 7;
        ParsableByteArray parsableByteArray = this.sampleDataBuffer;
        if (i != 0) {
            data.readBits(parsableByteArray.getData(), 0, muxLengthBytes * 8);
            this.sampleDataBuffer.setPosition(0);
        } else {
            parsableByteArray.setPosition(bitPosition >> 3);
        }
        this.output.sampleData(this.sampleDataBuffer, muxLengthBytes);
        Assertions.checkState(this.timeUs != C.TIME_UNSET);
        this.output.sampleMetadata(this.timeUs, 1, muxLengthBytes, 0, null);
        this.timeUs += this.sampleDurationUs;
    }

    private void resetBufferForSize(int newSize) {
        this.sampleDataBuffer.reset(newSize);
        this.sampleBitArray.reset(this.sampleDataBuffer.getData());
    }

    private static long latmGetValue(ParsableBitArray data) {
        int bytesForValue = data.readBits(2);
        return data.readBits((bytesForValue + 1) * 8);
    }
}
