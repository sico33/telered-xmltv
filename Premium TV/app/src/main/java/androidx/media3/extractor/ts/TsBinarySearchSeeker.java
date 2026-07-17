package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.BinarySearchSeeker;
import androidx.media3.extractor.ExtractorInput;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class TsBinarySearchSeeker extends BinarySearchSeeker {
    private static final int MINIMUM_SEARCH_RANGE_BYTES = 940;
    private static final long SEEK_TOLERANCE_US = 100000;

    public TsBinarySearchSeeker(TimestampAdjuster pcrTimestampAdjuster, long streamDurationUs, long inputLength, int pcrPid, int timestampSearchBytes) {
        super(new BinarySearchSeeker.DefaultSeekTimestampConverter(), new TsPcrSeeker(pcrPid, pcrTimestampAdjuster, timestampSearchBytes), streamDurationUs, 0L, streamDurationUs + 1, 0L, inputLength, 188L, MINIMUM_SEARCH_RANGE_BYTES);
    }

    private static final class TsPcrSeeker implements BinarySearchSeeker.TimestampSeeker {
        private final ParsableByteArray packetBuffer = new ParsableByteArray();
        private final int pcrPid;
        private final TimestampAdjuster pcrTimestampAdjuster;
        private final int timestampSearchBytes;

        public TsPcrSeeker(int pcrPid, TimestampAdjuster pcrTimestampAdjuster, int timestampSearchBytes) {
            this.pcrPid = pcrPid;
            this.pcrTimestampAdjuster = pcrTimestampAdjuster;
            this.timestampSearchBytes = timestampSearchBytes;
        }

        @Override // androidx.media3.extractor.BinarySearchSeeker.TimestampSeeker
        public BinarySearchSeeker.TimestampSearchResult searchForTimestamp(ExtractorInput input, long targetTimestamp) throws IOException {
            long inputPosition = input.getPosition();
            int bytesToSearch = (int) Math.min(this.timestampSearchBytes, input.getLength() - inputPosition);
            this.packetBuffer.reset(bytesToSearch);
            input.peekFully(this.packetBuffer.getData(), 0, bytesToSearch);
            return searchForPcrValueInBuffer(this.packetBuffer, targetTimestamp, inputPosition);
        }

        private BinarySearchSeeker.TimestampSearchResult searchForPcrValueInBuffer(ParsableByteArray packetBuffer, long targetPcrTimeUs, long bufferStartOffset) {
            long j;
            int limit = packetBuffer.limit();
            long startOfLastPacketPosition = -1;
            long endOfLastPacketPosition = -1;
            long lastPcrTimeUsInRange = C.TIME_UNSET;
            while (true) {
                if (packetBuffer.bytesLeft() < 188) {
                    j = C.TIME_UNSET;
                    break;
                }
                int startOfPacket = TsUtil.findSyncBytePosition(packetBuffer.getData(), packetBuffer.getPosition(), limit);
                int endOfPacket = startOfPacket + TsExtractor.TS_PACKET_SIZE;
                if (endOfPacket > limit) {
                    j = C.TIME_UNSET;
                    break;
                }
                long pcrValue = TsUtil.readPcrFromPacket(packetBuffer, startOfPacket, this.pcrPid);
                if (pcrValue != C.TIME_UNSET) {
                    long startOfLastPacketPosition2 = startOfLastPacketPosition;
                    long pcrTimeUs = this.pcrTimestampAdjuster.adjustTsTimestamp(pcrValue);
                    if (pcrTimeUs > targetPcrTimeUs) {
                        if (lastPcrTimeUsInRange == C.TIME_UNSET) {
                            return BinarySearchSeeker.TimestampSearchResult.overestimatedResult(pcrTimeUs, bufferStartOffset);
                        }
                        return BinarySearchSeeker.TimestampSearchResult.targetFoundResult(bufferStartOffset + startOfLastPacketPosition2);
                    }
                    if (100000 + pcrTimeUs > targetPcrTimeUs) {
                        long startOfPacketInStream = bufferStartOffset + ((long) startOfPacket);
                        return BinarySearchSeeker.TimestampSearchResult.targetFoundResult(startOfPacketInStream);
                    }
                    long lastPcrTimeUsInRange2 = startOfPacket;
                    startOfLastPacketPosition = lastPcrTimeUsInRange2;
                    lastPcrTimeUsInRange = pcrTimeUs;
                }
                packetBuffer.setPosition(endOfPacket);
                endOfLastPacketPosition = endOfPacket;
                limit = limit;
            }
            if (lastPcrTimeUsInRange != j) {
                long endOfLastPacketPositionInStream = bufferStartOffset + endOfLastPacketPosition;
                return BinarySearchSeeker.TimestampSearchResult.underestimatedResult(lastPcrTimeUsInRange, endOfLastPacketPositionInStream);
            }
            return BinarySearchSeeker.TimestampSearchResult.NO_TIMESTAMP_IN_RANGE_RESULT;
        }

        @Override // androidx.media3.extractor.BinarySearchSeeker.TimestampSeeker
        public void onSeekFinished() {
            this.packetBuffer.reset(Util.EMPTY_BYTE_ARRAY);
        }
    }
}
