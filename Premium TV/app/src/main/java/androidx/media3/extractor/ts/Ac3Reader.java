package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.Ac3Util;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import com.google.common.base.Ascii;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class Ac3Reader implements ElementaryStreamReader {
    private static final int HEADER_SIZE = 128;
    private static final int STATE_FINDING_SYNC = 0;
    private static final int STATE_READING_HEADER = 1;
    private static final int STATE_READING_SAMPLE = 2;
    private int bytesRead;
    private Format format;
    private String formatId;
    private final ParsableBitArray headerScratchBits;
    private final ParsableByteArray headerScratchBytes;
    private final String language;
    private boolean lastByteWas0B;
    private TrackOutput output;
    private final int roleFlags;
    private long sampleDurationUs;
    private int sampleSize;
    private int state;
    private long timeUs;

    public Ac3Reader() {
        this(null, 0);
    }

    public Ac3Reader(String language, int roleFlags) {
        this.headerScratchBits = new ParsableBitArray(new byte[128]);
        this.headerScratchBytes = new ParsableByteArray(this.headerScratchBits.data);
        this.state = 0;
        this.timeUs = C.TIME_UNSET;
        this.language = language;
        this.roleFlags = roleFlags;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.state = 0;
        this.bytesRead = 0;
        this.lastByteWas0B = false;
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
                        this.headerScratchBytes.getData()[0] = Ascii.VT;
                        this.headerScratchBytes.getData()[1] = 119;
                        this.bytesRead = 2;
                    }
                    break;
                case 1:
                    if (continueRead(data, this.headerScratchBytes.getData(), 128)) {
                        parseHeader();
                        this.headerScratchBytes.setPosition(0);
                        this.output.sampleData(this.headerScratchBytes, 128);
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
            if (!this.lastByteWas0B) {
                this.lastByteWas0B = pesBuffer.readUnsignedByte() == 11;
            } else {
                int secondByte = pesBuffer.readUnsignedByte();
                if (secondByte == 119) {
                    this.lastByteWas0B = false;
                    return true;
                }
                this.lastByteWas0B = secondByte == 11;
            }
        }
    }

    @RequiresNonNull({"output"})
    private void parseHeader() {
        this.headerScratchBits.setPosition(0);
        Ac3Util.SyncFrameInfo frameInfo = Ac3Util.parseAc3SyncframeInfo(this.headerScratchBits);
        if (this.format == null || frameInfo.channelCount != this.format.channelCount || frameInfo.sampleRate != this.format.sampleRate || !Util.areEqual(frameInfo.mimeType, this.format.sampleMimeType)) {
            Format.Builder formatBuilder = new Format.Builder().setId(this.formatId).setSampleMimeType(frameInfo.mimeType).setChannelCount(frameInfo.channelCount).setSampleRate(frameInfo.sampleRate).setLanguage(this.language).setRoleFlags(this.roleFlags).setPeakBitrate(frameInfo.bitrate);
            if (MimeTypes.AUDIO_AC3.equals(frameInfo.mimeType)) {
                formatBuilder.setAverageBitrate(frameInfo.bitrate);
            }
            this.format = formatBuilder.build();
            this.output.format(this.format);
        }
        this.sampleSize = frameInfo.frameSize;
        this.sampleDurationUs = (((long) frameInfo.sampleCount) * 1000000) / ((long) this.format.sampleRate);
    }
}
