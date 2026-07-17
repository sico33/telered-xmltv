package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class MpeghReader implements ElementaryStreamReader {
    private static final int MAX_MHAS_PACKET_HEADER_SIZE = 15;
    private static final int MHAS_SYNC_WORD_LENGTH = 3;
    private static final int MIN_MHAS_PACKET_HEADER_SIZE = 2;
    private static final int STATE_FINDING_SYNC = 0;
    private static final int STATE_READING_PACKET_HEADER = 1;
    private static final int STATE_READING_PACKET_PAYLOAD = 2;
    private boolean configFound;
    private boolean dataPending;
    private int flags;
    private String formatId;
    private int frameBytes;
    private TrackOutput output;
    private int payloadBytesRead;
    private int syncBytes;
    private int truncationSamples;
    private int state = 0;
    private final ParsableByteArray headerScratchBytes = new ParsableByteArray(new byte[15], 2);
    private final ParsableBitArray headerScratchBits = new ParsableBitArray();
    private final ParsableByteArray dataScratchBytes = new ParsableByteArray();
    private MpeghUtil.MhasPacketHeader header = new MpeghUtil.MhasPacketHeader();
    private int samplingRate = C.RATE_UNSET_INT;
    private int standardFrameLength = -1;
    private long mainStreamLabel = -1;
    private boolean rapPending = true;
    private boolean headerDataFinished = true;
    private double timeUs = -9.223372036854776E18d;
    private double timeUsPending = -9.223372036854776E18d;

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.state = 0;
        this.syncBytes = 0;
        this.headerScratchBytes.reset(2);
        this.payloadBytesRead = 0;
        this.frameBytes = 0;
        this.samplingRate = C.RATE_UNSET_INT;
        this.standardFrameLength = -1;
        this.truncationSamples = 0;
        this.mainStreamLabel = -1L;
        this.configFound = false;
        this.dataPending = false;
        this.headerDataFinished = true;
        this.rapPending = true;
        this.timeUs = -9.223372036854776E18d;
        this.timeUsPending = -9.223372036854776E18d;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        idGenerator.generateNewId();
        this.formatId = idGenerator.getFormatId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 1);
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetStarted(long pesTimeUs, int flags) {
        this.flags = flags;
        if (!this.rapPending && (this.frameBytes != 0 || !this.headerDataFinished)) {
            this.dataPending = true;
        }
        if (pesTimeUs != C.TIME_UNSET) {
            if (this.dataPending) {
                this.timeUsPending = pesTimeUs;
            } else {
                this.timeUs = pesTimeUs;
            }
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void consume(ParsableByteArray data) throws ParserException {
        Assertions.checkStateNotNull(this.output);
        while (data.bytesLeft() > 0) {
            switch (this.state) {
                case 0:
                    if (skipToNextSync(data)) {
                        this.state = 1;
                    }
                    break;
                case 1:
                    copyData(data, this.headerScratchBytes, false);
                    if (this.headerScratchBytes.bytesLeft() == 0) {
                        boolean header = parseHeader();
                        ParsableByteArray parsableByteArray = this.headerScratchBytes;
                        if (header) {
                            parsableByteArray.setPosition(0);
                            this.output.sampleData(this.headerScratchBytes, this.headerScratchBytes.limit());
                            this.headerScratchBytes.reset(2);
                            this.dataScratchBytes.reset(this.header.packetLength);
                            this.headerDataFinished = true;
                            this.state = 2;
                        } else if (parsableByteArray.limit() < 15) {
                            this.headerScratchBytes.setLimit(this.headerScratchBytes.limit() + 1);
                            this.headerDataFinished = false;
                        }
                    } else {
                        this.headerDataFinished = false;
                    }
                    break;
                case 2:
                    if (shouldParsePacket(this.header.packetType)) {
                        copyData(data, this.dataScratchBytes, true);
                    }
                    writeSampleData(data);
                    if (this.payloadBytesRead == this.header.packetLength) {
                        if (this.header.packetType == 1) {
                            parseConfig(new ParsableBitArray(this.dataScratchBytes.getData()));
                        } else if (this.header.packetType == 17) {
                            this.truncationSamples = MpeghUtil.parseAudioTruncationInfo(new ParsableBitArray(this.dataScratchBytes.getData()));
                        } else if (this.header.packetType == 2) {
                            finalizeFrame();
                        }
                        this.state = 1;
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

    private void copyData(ParsableByteArray source, ParsableByteArray target, boolean resetSourcePosition) {
        int sourcePosition = source.getPosition();
        int bytesToRead = Math.min(source.bytesLeft(), target.bytesLeft());
        source.readBytes(target.getData(), target.getPosition(), bytesToRead);
        target.skipBytes(bytesToRead);
        if (resetSourcePosition) {
            source.setPosition(sourcePosition);
        }
    }

    private boolean skipToNextSync(ParsableByteArray pesBuffer) {
        if ((this.flags & 2) == 0) {
            pesBuffer.setPosition(pesBuffer.limit());
            return false;
        }
        if ((this.flags & 4) != 0) {
            return true;
        }
        while (pesBuffer.bytesLeft() > 0) {
            this.syncBytes <<= 8;
            this.syncBytes |= pesBuffer.readUnsignedByte();
            if (MpeghUtil.isSyncWord(this.syncBytes)) {
                pesBuffer.setPosition(pesBuffer.getPosition() - 3);
                this.syncBytes = 0;
                return true;
            }
        }
        return false;
    }

    private boolean parseHeader() throws ParserException {
        int headerLength = this.headerScratchBytes.limit();
        this.headerScratchBits.reset(this.headerScratchBytes.getData(), headerLength);
        boolean result = MpeghUtil.parseMhasPacketHeader(this.headerScratchBits, this.header);
        if (result) {
            this.payloadBytesRead = 0;
            this.frameBytes += this.header.packetLength + headerLength;
        }
        return result;
    }

    private boolean shouldParsePacket(int packetType) {
        return packetType == 1 || packetType == 17;
    }

    @RequiresNonNull({"output"})
    private void writeSampleData(ParsableByteArray data) {
        int bytesToRead = Math.min(data.bytesLeft(), this.header.packetLength - this.payloadBytesRead);
        this.output.sampleData(data, bytesToRead);
        this.payloadBytesRead += bytesToRead;
    }

    @RequiresNonNull({"output"})
    private void parseConfig(ParsableBitArray bitArray) throws ParserException {
        MpeghUtil.Mpegh3daConfig config = MpeghUtil.parseMpegh3daConfig(bitArray);
        this.samplingRate = config.samplingFrequency;
        this.standardFrameLength = config.standardFrameLength;
        if (this.mainStreamLabel != this.header.packetLabel) {
            this.mainStreamLabel = this.header.packetLabel;
            String codecs = config.profileLevelIndication != -1 ? "mhm1" + String.format(".%02X", Integer.valueOf(config.profileLevelIndication)) : "mhm1";
            List<byte[]> initializationData = null;
            if (config.compatibleProfileLevelSet != null && config.compatibleProfileLevelSet.length > 0) {
                initializationData = ImmutableList.of(Util.EMPTY_BYTE_ARRAY, config.compatibleProfileLevelSet);
            }
            Format format = new Format.Builder().setId(this.formatId).setSampleMimeType(MimeTypes.AUDIO_MPEGH_MHM1).setSampleRate(this.samplingRate).setCodecs(codecs).setInitializationData(initializationData).build();
            this.output.format(format);
        }
        this.configFound = true;
    }

    @RequiresNonNull({"output"})
    private void finalizeFrame() {
        int flag;
        if (!this.configFound) {
            flag = 0;
        } else {
            this.rapPending = false;
            flag = 1;
        }
        int flag2 = this.standardFrameLength;
        double sampleDurationUs = (((double) (flag2 - this.truncationSamples)) * 1000000.0d) / ((double) this.samplingRate);
        long pts = Math.round(this.timeUs);
        if (this.dataPending) {
            this.dataPending = false;
            this.timeUs = this.timeUsPending;
        } else {
            this.timeUs += sampleDurationUs;
        }
        this.output.sampleMetadata(pts, flag, this.frameBytes, 0, null);
        this.configFound = false;
        this.truncationSamples = 0;
        this.frameBytes = 0;
    }
}
