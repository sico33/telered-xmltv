package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.Ac4Util;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class Ac4Reader implements ElementaryStreamReader {
    private static final int STATE_FINDING_SYNC = 0;
    private static final int STATE_READING_HEADER = 1;
    private static final int STATE_READING_SAMPLE = 2;
    private int bytesRead;
    private Format format;
    private String formatId;
    private boolean hasCRC;
    private final ParsableBitArray headerScratchBits;
    private final ParsableByteArray headerScratchBytes;
    private final String language;
    private boolean lastByteWasAC;
    private TrackOutput output;
    private final int roleFlags;
    private long sampleDurationUs;
    private int sampleSize;
    private int state;
    private long timeUs;

    public Ac4Reader() {
        this(null, 0);
    }

    public Ac4Reader(String language, int roleFlags) {
        this.headerScratchBits = new ParsableBitArray(new byte[16]);
        this.headerScratchBytes = new ParsableByteArray(this.headerScratchBits.data);
        this.state = 0;
        this.bytesRead = 0;
        this.lastByteWasAC = false;
        this.hasCRC = false;
        this.timeUs = C.TIME_UNSET;
        this.language = language;
        this.roleFlags = roleFlags;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.state = 0;
        this.bytesRead = 0;
        this.lastByteWasAC = false;
        this.hasCRC = false;
        this.timeUs = C.TIME_UNSET;
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
    public void consume(ParsableByteArray data) {
        Assertions.checkStateNotNull(this.output);
        while (data.bytesLeft() > 0) {
            switch (this.state) {
                case 0:
                    if (skipToNextSync(data)) {
                        this.state = 1;
                        this.headerScratchBytes.getData()[0] = -84;
                        this.headerScratchBytes.getData()[1] = (byte) (this.hasCRC ? 65 : 64);
                        this.bytesRead = 2;
                    }
                    break;
                case 1:
                    if (continueRead(data, this.headerScratchBytes.getData(), 16)) {
                        parseHeader();
                        this.headerScratchBytes.setPosition(0);
                        this.output.sampleData(this.headerScratchBytes, 16);
                        this.state = 2;
                    }
                    break;
                case 2:
                    int bytesToRead = Math.min(data.bytesLeft(), this.sampleSize - this.bytesRead);
                    this.output.sampleData(data, bytesToRead);
                    this.bytesRead += bytesToRead;
                    if (this.bytesRead == this.sampleSize) {
                        Assertions.checkState(this.timeUs != C.TIME_UNSET);
                        this.output.sampleMetadata(this.timeUs, 1, this.sampleSize, 0, null);
                        this.timeUs += this.sampleDurationUs;
                        this.state = 0;
                    }
                    break;
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

    private boolean skipToNextSync(ParsableByteArray pesBuffer) {
        while (true) {
            if (pesBuffer.bytesLeft() <= 0) {
                return false;
            }
            if (!this.lastByteWasAC) {
                this.lastByteWasAC = pesBuffer.readUnsignedByte() == 172;
            } else {
                int secondByte = pesBuffer.readUnsignedByte();
                this.lastByteWasAC = secondByte == 172;
                if (secondByte == 64 || secondByte == 65) {
                    this.hasCRC = secondByte == 65;
                    return true;
                }
            }
        }
    }

    @RequiresNonNull({"output"})
    private void parseHeader() {
        this.headerScratchBits.setPosition(0);
        Ac4Util.SyncFrameInfo frameInfo = Ac4Util.parseAc4SyncframeInfo(this.headerScratchBits);
        if (this.format == null || frameInfo.channelCount != this.format.channelCount || frameInfo.sampleRate != this.format.sampleRate || !MimeTypes.AUDIO_AC4.equals(this.format.sampleMimeType)) {
            this.format = new Format.Builder().setId(this.formatId).setSampleMimeType(MimeTypes.AUDIO_AC4).setChannelCount(frameInfo.channelCount).setSampleRate(frameInfo.sampleRate).setLanguage(this.language).setRoleFlags(this.roleFlags).build();
            this.output.format(this.format);
        }
        this.sampleSize = frameInfo.frameSize;
        this.sampleDurationUs = (((long) frameInfo.sampleCount) * 1000000) / ((long) this.format.sampleRate);
    }
}
