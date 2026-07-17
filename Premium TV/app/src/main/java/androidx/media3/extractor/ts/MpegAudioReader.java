package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.MpegAudioUtil;
import androidx.media3.extractor.TrackOutput;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class MpegAudioReader implements ElementaryStreamReader {
    private static final int HEADER_SIZE = 4;
    private static final int STATE_FINDING_HEADER = 0;
    private static final int STATE_READING_FRAME = 2;
    private static final int STATE_READING_HEADER = 1;
    private String formatId;
    private int frameBytesRead;
    private long frameDurationUs;
    private int frameSize;
    private boolean hasOutputFormat;
    private final MpegAudioUtil.Header header;
    private final ParsableByteArray headerScratch;
    private final String language;
    private boolean lastByteWasFF;
    private TrackOutput output;
    private final int roleFlags;
    private int state;
    private long timeUs;

    public MpegAudioReader() {
        this(null, 0);
    }

    public MpegAudioReader(String language, int roleFlags) {
        this.state = 0;
        this.headerScratch = new ParsableByteArray(4);
        this.headerScratch.getData()[0] = -1;
        this.header = new MpegAudioUtil.Header();
        this.timeUs = C.TIME_UNSET;
        this.language = language;
        this.roleFlags = roleFlags;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.state = 0;
        this.frameBytesRead = 0;
        this.lastByteWasFF = false;
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
                    findHeader(data);
                    break;
                case 1:
                    readHeaderRemainder(data);
                    break;
                case 2:
                    readFrameRemainder(data);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetFinished(boolean isEndOfInput) {
    }

    private void findHeader(ParsableByteArray source) {
        byte[] data = source.getData();
        int startOffset = source.getPosition();
        int endOffset = source.limit();
        for (int i = startOffset; i < endOffset; i++) {
            boolean byteIsFF = (data[i] & 255) == 255;
            boolean found = this.lastByteWasFF && (data[i] & 224) == 224;
            this.lastByteWasFF = byteIsFF;
            if (found) {
                source.setPosition(i + 1);
                this.lastByteWasFF = false;
                this.headerScratch.getData()[1] = data[i];
                this.frameBytesRead = 2;
                this.state = 1;
                return;
            }
        }
        source.setPosition(endOffset);
    }

    @RequiresNonNull({"output"})
    private void readHeaderRemainder(ParsableByteArray source) {
        int bytesToRead = Math.min(source.bytesLeft(), 4 - this.frameBytesRead);
        source.readBytes(this.headerScratch.getData(), this.frameBytesRead, bytesToRead);
        this.frameBytesRead += bytesToRead;
        if (this.frameBytesRead < 4) {
            return;
        }
        this.headerScratch.setPosition(0);
        boolean parsedHeader = this.header.setForHeaderData(this.headerScratch.readInt());
        if (!parsedHeader) {
            this.frameBytesRead = 0;
            this.state = 1;
            return;
        }
        this.frameSize = this.header.frameSize;
        if (!this.hasOutputFormat) {
            this.frameDurationUs = (((long) this.header.samplesPerFrame) * 1000000) / ((long) this.header.sampleRate);
            Format format = new Format.Builder().setId(this.formatId).setSampleMimeType(this.header.mimeType).setMaxInputSize(4096).setChannelCount(this.header.channels).setSampleRate(this.header.sampleRate).setLanguage(this.language).setRoleFlags(this.roleFlags).build();
            this.output.format(format);
            this.hasOutputFormat = true;
        }
        this.headerScratch.setPosition(0);
        this.output.sampleData(this.headerScratch, 4);
        this.state = 2;
    }

    @RequiresNonNull({"output"})
    private void readFrameRemainder(ParsableByteArray source) {
        int bytesToRead = Math.min(source.bytesLeft(), this.frameSize - this.frameBytesRead);
        this.output.sampleData(source, bytesToRead);
        this.frameBytesRead += bytesToRead;
        if (this.frameBytesRead < this.frameSize) {
            return;
        }
        Assertions.checkState(this.timeUs != C.TIME_UNSET);
        this.output.sampleMetadata(this.timeUs, 1, this.frameSize, 0, null);
        this.timeUs += this.frameDurationUs;
        this.frameBytesRead = 0;
        this.state = 0;
    }
}
