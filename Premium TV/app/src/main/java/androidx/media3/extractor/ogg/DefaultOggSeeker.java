package androidx.media3.extractor.ogg;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorUtil;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;

/* JADX INFO: loaded from: classes.dex */
final class DefaultOggSeeker implements OggSeeker {
    private static final int DEFAULT_OFFSET = 30000;
    private static final int MATCH_BYTE_RANGE = 100000;
    private static final int MATCH_RANGE = 72000;
    private static final int STATE_IDLE = 4;
    private static final int STATE_READ_LAST_PAGE = 1;
    private static final int STATE_SEEK = 2;
    private static final int STATE_SEEK_TO_END = 0;
    private static final int STATE_SKIP = 3;
    private long end;
    private long endGranule;
    private final OggPageHeader pageHeader;
    private final long payloadEndPosition;
    private final long payloadStartPosition;
    private long positionBeforeSeekToEnd;
    private long start;
    private long startGranule;
    private int state;
    private final StreamReader streamReader;
    private long targetGranule;
    private long totalGranules;

    public DefaultOggSeeker(StreamReader streamReader, long payloadStartPosition, long payloadEndPosition, long firstPayloadPageSize, long firstPayloadPageGranulePosition, boolean firstPayloadPageIsLastPage) {
        Assertions.checkArgument(payloadStartPosition >= 0 && payloadEndPosition > payloadStartPosition);
        this.streamReader = streamReader;
        this.payloadStartPosition = payloadStartPosition;
        this.payloadEndPosition = payloadEndPosition;
        if (firstPayloadPageSize == payloadEndPosition - payloadStartPosition || firstPayloadPageIsLastPage) {
            this.totalGranules = firstPayloadPageGranulePosition;
            this.state = 4;
        } else {
            this.state = 0;
        }
        this.pageHeader = new OggPageHeader();
    }

    @Override // androidx.media3.extractor.ogg.OggSeeker
    public long read(ExtractorInput input) throws IOException {
        switch (this.state) {
            case 0:
                this.positionBeforeSeekToEnd = input.getPosition();
                this.state = 1;
                long lastPageSearchPosition = this.payloadEndPosition - 65307;
                if (lastPageSearchPosition > this.positionBeforeSeekToEnd) {
                    return lastPageSearchPosition;
                }
            case 1:
                this.totalGranules = readGranuleOfLastPage(input);
                this.state = 4;
                return this.positionBeforeSeekToEnd;
            case 2:
                long position = getNextSeekPosition(input);
                if (position != -1) {
                    return position;
                }
                this.state = 3;
                break;
            case 3:
                skipToPageOfTargetGranule(input);
                this.state = 4;
                return -(this.startGranule + 2);
            case 4:
                return -1L;
            default:
                throw new IllegalStateException();
        }
    }

    @Override // androidx.media3.extractor.ogg.OggSeeker
    public OggSeekMap createSeekMap() {
        if (this.totalGranules != 0) {
            return new OggSeekMap();
        }
        return null;
    }

    @Override // androidx.media3.extractor.ogg.OggSeeker
    public void startSeek(long targetGranule) {
        this.targetGranule = Util.constrainValue(targetGranule, 0L, this.totalGranules - 1);
        this.state = 2;
        this.start = this.payloadStartPosition;
        this.end = this.payloadEndPosition;
        this.startGranule = 0L;
        this.endGranule = this.totalGranules;
    }

    private long getNextSeekPosition(ExtractorInput input) throws IOException {
        if (this.start == this.end) {
            return -1L;
        }
        long currentPosition = input.getPosition();
        if (!this.pageHeader.skipToNextPage(input, this.end)) {
            if (this.start == currentPosition) {
                throw new IOException("No ogg page can be found.");
            }
            return this.start;
        }
        this.pageHeader.populate(input, false);
        input.resetPeekPosition();
        long granuleDistance = this.targetGranule - this.pageHeader.granulePosition;
        int pageSize = this.pageHeader.headerSize + this.pageHeader.bodySize;
        if (0 <= granuleDistance && granuleDistance < 72000) {
            return -1L;
        }
        if (granuleDistance >= 0) {
            this.start = input.getPosition() + ((long) pageSize);
            this.startGranule = this.pageHeader.granulePosition;
        } else {
            this.end = currentPosition;
            this.endGranule = this.pageHeader.granulePosition;
        }
        if (this.end - this.start < SilenceSkippingAudioProcessor.DEFAULT_MINIMUM_SILENCE_DURATION_US) {
            this.end = this.start;
            return this.start;
        }
        long offset = ((long) pageSize) * (granuleDistance <= 0 ? 2L : 1L);
        long nextPosition = (input.getPosition() - offset) + (((this.end - this.start) * granuleDistance) / (this.endGranule - this.startGranule));
        return Util.constrainValue(nextPosition, this.start, this.end - 1);
    }

    private void skipToPageOfTargetGranule(ExtractorInput input) throws IOException {
        while (true) {
            this.pageHeader.skipToNextPage(input);
            this.pageHeader.populate(input, false);
            if (this.pageHeader.granulePosition <= this.targetGranule) {
                input.skipFully(this.pageHeader.headerSize + this.pageHeader.bodySize);
                this.start = input.getPosition();
                this.startGranule = this.pageHeader.granulePosition;
            } else {
                input.resetPeekPosition();
                return;
            }
        }
    }

    long readGranuleOfLastPage(ExtractorInput input) throws IOException {
        this.pageHeader.reset();
        if (!this.pageHeader.skipToNextPage(input)) {
            throw new EOFException();
        }
        this.pageHeader.populate(input, false);
        input.skipFully(this.pageHeader.headerSize + this.pageHeader.bodySize);
        long granulePosition = this.pageHeader.granulePosition;
        while ((this.pageHeader.type & 4) != 4 && this.pageHeader.skipToNextPage(input) && input.getPosition() < this.payloadEndPosition) {
            boolean hasPopulated = this.pageHeader.populate(input, true);
            if (!hasPopulated || !ExtractorUtil.skipFullyQuietly(input, this.pageHeader.headerSize + this.pageHeader.bodySize)) {
                return granulePosition;
            }
            granulePosition = this.pageHeader.granulePosition;
        }
        return granulePosition;
    }

    private final class OggSeekMap implements SeekMap {
        private OggSeekMap() {
        }

        @Override // androidx.media3.extractor.SeekMap
        public boolean isSeekable() {
            return true;
        }

        @Override // androidx.media3.extractor.SeekMap
        public SeekMap.SeekPoints getSeekPoints(long timeUs) {
            long targetGranule = DefaultOggSeeker.this.streamReader.convertTimeToGranule(timeUs);
            long estimatedPosition = (DefaultOggSeeker.this.payloadStartPosition + BigInteger.valueOf(targetGranule).multiply(BigInteger.valueOf(DefaultOggSeeker.this.payloadEndPosition - DefaultOggSeeker.this.payloadStartPosition)).divide(BigInteger.valueOf(DefaultOggSeeker.this.totalGranules)).longValue()) - DashMediaSource.DEFAULT_FALLBACK_TARGET_LIVE_OFFSET_MS;
            return new SeekMap.SeekPoints(new SeekPoint(timeUs, Util.constrainValue(estimatedPosition, DefaultOggSeeker.this.payloadStartPosition, DefaultOggSeeker.this.payloadEndPosition - 1)));
        }

        @Override // androidx.media3.extractor.SeekMap
        public long getDurationUs() {
            return DefaultOggSeeker.this.streamReader.convertGranuleToTime(DefaultOggSeeker.this.totalGranules);
        }
    }
}
