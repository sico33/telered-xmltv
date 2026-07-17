package androidx.media3.extractor.ogg;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
abstract class StreamReader {
    private static final int STATE_END_OF_INPUT = 3;
    private static final int STATE_READ_HEADERS = 0;
    private static final int STATE_READ_PAYLOAD = 2;
    private static final int STATE_SKIP_HEADERS = 1;
    private long currentGranule;
    private ExtractorOutput extractorOutput;
    private boolean formatSet;
    private long lengthOfReadPacket;
    private OggSeeker oggSeeker;
    private long payloadStartPosition;
    private int sampleRate;
    private boolean seekMapSet;
    private int state;
    private long targetGranule;
    private TrackOutput trackOutput;
    private final OggPacket oggPacket = new OggPacket();
    private SetupData setupData = new SetupData();

    protected abstract long preparePayload(ParsableByteArray parsableByteArray);

    @EnsuresNonNullIf(expression = {"#3.format"}, result = false)
    protected abstract boolean readHeaders(ParsableByteArray parsableByteArray, long j, SetupData setupData) throws IOException;

    static class SetupData {
        Format format;
        OggSeeker oggSeeker;

        SetupData() {
        }
    }

    void init(ExtractorOutput output, TrackOutput trackOutput) {
        this.extractorOutput = output;
        this.trackOutput = trackOutput;
        reset(true);
    }

    protected void reset(boolean headerData) {
        if (headerData) {
            this.setupData = new SetupData();
            this.payloadStartPosition = 0L;
            this.state = 0;
        } else {
            this.state = 1;
        }
        this.targetGranule = -1L;
        this.currentGranule = 0L;
    }

    final void seek(long position, long timeUs) {
        this.oggPacket.reset();
        if (position == 0) {
            reset(!this.seekMapSet);
        } else if (this.state != 0) {
            this.targetGranule = convertTimeToGranule(timeUs);
            ((OggSeeker) Util.castNonNull(this.oggSeeker)).startSeek(this.targetGranule);
            this.state = 2;
        }
    }

    final int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        assertInitialized();
        switch (this.state) {
            case 0:
                return readHeadersAndUpdateState(input);
            case 1:
                input.skipFully((int) this.payloadStartPosition);
                this.state = 2;
                return 0;
            case 2:
                Util.castNonNull(this.oggSeeker);
                return readPayload(input, seekPosition);
            case 3:
                return -1;
            default:
                throw new IllegalStateException();
        }
    }

    @EnsuresNonNull({"trackOutput", "extractorOutput"})
    private void assertInitialized() {
        Assertions.checkStateNotNull(this.trackOutput);
        Util.castNonNull(this.extractorOutput);
    }

    @EnsuresNonNullIf(expression = {"setupData.format"}, result = true)
    private boolean readHeaders(ExtractorInput input) throws IOException {
        while (this.oggPacket.populate(input)) {
            this.lengthOfReadPacket = input.getPosition() - this.payloadStartPosition;
            if (readHeaders(this.oggPacket.getPayload(), this.payloadStartPosition, this.setupData)) {
                this.payloadStartPosition = input.getPosition();
            } else {
                return true;
            }
        }
        this.state = 3;
        return false;
    }

    @RequiresNonNull({"trackOutput"})
    private int readHeadersAndUpdateState(ExtractorInput input) throws IOException {
        if (!readHeaders(input)) {
            return -1;
        }
        this.sampleRate = this.setupData.format.sampleRate;
        if (!this.formatSet) {
            this.trackOutput.format(this.setupData.format);
            this.formatSet = true;
        }
        if (this.setupData.oggSeeker != null) {
            this.oggSeeker = this.setupData.oggSeeker;
        } else if (input.getLength() == -1) {
            this.oggSeeker = new UnseekableOggSeeker();
        } else {
            OggPageHeader firstPayloadPageHeader = this.oggPacket.getPageHeader();
            boolean isLastPage = (firstPayloadPageHeader.type & 4) != 0;
            this.oggSeeker = new DefaultOggSeeker(this, this.payloadStartPosition, input.getLength(), firstPayloadPageHeader.headerSize + firstPayloadPageHeader.bodySize, firstPayloadPageHeader.granulePosition, isLastPage);
        }
        this.state = 2;
        this.oggPacket.trimPayload();
        return 0;
    }

    @RequiresNonNull({"trackOutput", "oggSeeker", "extractorOutput"})
    private int readPayload(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        long position = this.oggSeeker.read(input);
        if (position >= 0) {
            seekPosition.position = position;
            return 1;
        }
        if (position < -1) {
            onSeekEnd(-(2 + position));
        }
        if (!this.seekMapSet) {
            SeekMap seekMap = (SeekMap) Assertions.checkStateNotNull(this.oggSeeker.createSeekMap());
            this.extractorOutput.seekMap(seekMap);
            this.seekMapSet = true;
        }
        if (this.lengthOfReadPacket <= 0 && !this.oggPacket.populate(input)) {
            this.state = 3;
            return -1;
        }
        this.lengthOfReadPacket = 0L;
        ParsableByteArray payload = this.oggPacket.getPayload();
        long granulesInPacket = preparePayload(payload);
        if (granulesInPacket >= 0 && this.currentGranule + granulesInPacket >= this.targetGranule) {
            long timeUs = convertGranuleToTime(this.currentGranule);
            this.trackOutput.sampleData(payload, payload.limit());
            this.trackOutput.sampleMetadata(timeUs, 1, payload.limit(), 0, null);
            this.targetGranule = -1L;
        }
        this.currentGranule += granulesInPacket;
        return 0;
    }

    protected long convertGranuleToTime(long granule) {
        return (1000000 * granule) / ((long) this.sampleRate);
    }

    protected long convertTimeToGranule(long timeUs) {
        return (((long) this.sampleRate) * timeUs) / 1000000;
    }

    protected void onSeekEnd(long currentGranule) {
        this.currentGranule = currentGranule;
    }

    private static final class UnseekableOggSeeker implements OggSeeker {
        private UnseekableOggSeeker() {
        }

        @Override // androidx.media3.extractor.ogg.OggSeeker
        public long read(ExtractorInput input) {
            return -1L;
        }

        @Override // androidx.media3.extractor.ogg.OggSeeker
        public void startSeek(long targetGranule) {
        }

        @Override // androidx.media3.extractor.ogg.OggSeeker
        public SeekMap createSeekMap() {
            return new SeekMap.Unseekable(C.TIME_UNSET);
        }
    }
}
