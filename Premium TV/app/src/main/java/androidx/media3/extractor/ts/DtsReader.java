package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.DtsUtil;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import com.google.common.primitives.Ints;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class DtsReader implements ElementaryStreamReader {
    private static final int CORE_HEADER_SIZE = 18;
    static final int EXTSS_HEADER_SIZE_MAX = 4096;
    static final int FTOC_MAX_HEADER_SIZE = 5408;
    private static final int STATE_FINDING_EXTSS_HEADER_SIZE = 2;
    private static final int STATE_FINDING_SYNC = 0;
    private static final int STATE_FINDING_UHD_HEADER_SIZE = 4;
    private static final int STATE_READING_CORE_HEADER = 1;
    private static final int STATE_READING_EXTSS_HEADER = 3;
    private static final int STATE_READING_SAMPLE = 6;
    private static final int STATE_READING_UHD_HEADER = 5;
    private int bytesRead;
    private Format format;
    private String formatId;
    private int frameType;
    private final ParsableByteArray headerScratchBytes;
    private final String language;
    private TrackOutput output;
    private final int roleFlags;
    private long sampleDurationUs;
    private int sampleSize;
    private int syncBytes;
    private int state = 0;
    private long timeUs = C.TIME_UNSET;
    private final AtomicInteger uhdAudioChunkId = new AtomicInteger();
    private int extensionSubstreamHeaderSize = -1;
    private int uhdHeaderSize = -1;

    public DtsReader(String language, int roleFlags, int maxHeaderSize) {
        this.headerScratchBytes = new ParsableByteArray(new byte[maxHeaderSize]);
        this.language = language;
        this.roleFlags = roleFlags;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.state = 0;
        this.bytesRead = 0;
        this.syncBytes = 0;
        this.timeUs = C.TIME_UNSET;
        this.uhdAudioChunkId.set(0);
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        idGenerator.generateNewId();
        this.formatId = idGenerator.getFormatId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 1);
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
                    if (skipToNextSyncWord(data)) {
                        if (this.frameType == 3 || this.frameType == 4) {
                            this.state = 4;
                        } else if (this.frameType == 1) {
                            this.state = 1;
                        } else {
                            this.state = 2;
                        }
                    }
                    break;
                case 1:
                    if (continueRead(data, this.headerScratchBytes.getData(), 18)) {
                        parseCoreHeader();
                        this.headerScratchBytes.setPosition(0);
                        this.output.sampleData(this.headerScratchBytes, 18);
                        this.state = 6;
                    }
                    break;
                case 2:
                    if (continueRead(data, this.headerScratchBytes.getData(), 7)) {
                        this.extensionSubstreamHeaderSize = DtsUtil.parseDtsHdHeaderSize(this.headerScratchBytes.getData());
                        this.state = 3;
                    }
                    break;
                case 3:
                    if (continueRead(data, this.headerScratchBytes.getData(), this.extensionSubstreamHeaderSize)) {
                        parseExtensionSubstreamHeader();
                        this.headerScratchBytes.setPosition(0);
                        this.output.sampleData(this.headerScratchBytes, this.extensionSubstreamHeaderSize);
                        this.state = 6;
                    }
                    break;
                case 4:
                    if (continueRead(data, this.headerScratchBytes.getData(), 6)) {
                        this.uhdHeaderSize = DtsUtil.parseDtsUhdHeaderSize(this.headerScratchBytes.getData());
                        if (this.bytesRead > this.uhdHeaderSize) {
                            int extraBytes = this.bytesRead - this.uhdHeaderSize;
                            this.bytesRead -= extraBytes;
                            data.setPosition(data.getPosition() - extraBytes);
                        }
                        this.state = 5;
                    }
                    break;
                case 5:
                    if (continueRead(data, this.headerScratchBytes.getData(), this.uhdHeaderSize)) {
                        parseUhdHeader();
                        this.headerScratchBytes.setPosition(0);
                        this.output.sampleData(this.headerScratchBytes, this.uhdHeaderSize);
                        this.state = 6;
                    }
                    break;
                case 6:
                    int bytesToRead = Math.min(data.bytesLeft(), this.sampleSize - this.bytesRead);
                    this.output.sampleData(data, bytesToRead);
                    this.bytesRead += bytesToRead;
                    if (this.bytesRead == this.sampleSize) {
                        Assertions.checkState(this.timeUs != C.TIME_UNSET);
                        this.output.sampleMetadata(this.timeUs, this.frameType == 4 ? 0 : 1, this.sampleSize, 0, null);
                        this.timeUs += this.sampleDurationUs;
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

    private boolean continueRead(ParsableByteArray source, byte[] target, int targetLength) {
        int bytesToRead = Math.min(source.bytesLeft(), targetLength - this.bytesRead);
        source.readBytes(target, this.bytesRead, bytesToRead);
        this.bytesRead += bytesToRead;
        return this.bytesRead == targetLength;
    }

    private boolean skipToNextSyncWord(ParsableByteArray pesBuffer) {
        while (pesBuffer.bytesLeft() > 0) {
            this.syncBytes <<= 8;
            this.syncBytes |= pesBuffer.readUnsignedByte();
            this.frameType = DtsUtil.getFrameType(this.syncBytes);
            if (this.frameType != 0) {
                byte[] headerData = this.headerScratchBytes.getData();
                headerData[0] = (byte) ((this.syncBytes >> 24) & 255);
                headerData[1] = (byte) ((this.syncBytes >> 16) & 255);
                headerData[2] = (byte) ((this.syncBytes >> 8) & 255);
                headerData[3] = (byte) (this.syncBytes & 255);
                this.bytesRead = 4;
                this.syncBytes = 0;
                return true;
            }
        }
        return false;
    }

    @RequiresNonNull({"output"})
    private void parseCoreHeader() {
        byte[] frameData = this.headerScratchBytes.getData();
        if (this.format == null) {
            this.format = DtsUtil.parseDtsFormat(frameData, this.formatId, this.language, this.roleFlags, null);
            this.output.format(this.format);
        }
        this.sampleSize = DtsUtil.getDtsFrameSize(frameData);
        this.sampleDurationUs = Ints.checkedCast(Util.sampleCountToDurationUs(DtsUtil.parseDtsAudioSampleCount(frameData), this.format.sampleRate));
    }

    @RequiresNonNull({"output"})
    private void parseExtensionSubstreamHeader() throws ParserException {
        DtsUtil.DtsHeader dtsHeader = DtsUtil.parseDtsHdHeader(this.headerScratchBytes.getData());
        updateFormatWithDtsHeaderInfo(dtsHeader);
        this.sampleSize = dtsHeader.frameSize;
        this.sampleDurationUs = dtsHeader.frameDurationUs == C.TIME_UNSET ? 0L : dtsHeader.frameDurationUs;
    }

    @RequiresNonNull({"output"})
    private void parseUhdHeader() throws ParserException {
        DtsUtil.DtsHeader dtsHeader = DtsUtil.parseDtsUhdHeader(this.headerScratchBytes.getData(), this.uhdAudioChunkId);
        if (this.frameType == 3) {
            updateFormatWithDtsHeaderInfo(dtsHeader);
        }
        this.sampleSize = dtsHeader.frameSize;
        this.sampleDurationUs = dtsHeader.frameDurationUs == C.TIME_UNSET ? 0L : dtsHeader.frameDurationUs;
    }

    @RequiresNonNull({"output"})
    private void updateFormatWithDtsHeaderInfo(DtsUtil.DtsHeader dtsHeader) {
        if (dtsHeader.sampleRate == -2147483647 || dtsHeader.channelCount == -1) {
            return;
        }
        if (this.format == null || dtsHeader.channelCount != this.format.channelCount || dtsHeader.sampleRate != this.format.sampleRate || !Util.areEqual(dtsHeader.mimeType, this.format.sampleMimeType)) {
            Format.Builder formatBuilder = this.format == null ? new Format.Builder() : this.format.buildUpon();
            this.format = formatBuilder.setId(this.formatId).setSampleMimeType(dtsHeader.mimeType).setChannelCount(dtsHeader.channelCount).setSampleRate(dtsHeader.sampleRate).setLanguage(this.language).setRoleFlags(this.roleFlags).build();
            this.output.format(this.format);
        }
    }
}
