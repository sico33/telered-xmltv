package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.PositionHolder;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class TsDurationReader {
    private static final String TAG = "TsDurationReader";
    private boolean isDurationRead;
    private boolean isFirstPcrValueRead;
    private boolean isLastPcrValueRead;
    private final int timestampSearchBytes;
    private final TimestampAdjuster pcrTimestampAdjuster = new TimestampAdjuster(0);
    private long firstPcrValue = C.TIME_UNSET;
    private long lastPcrValue = C.TIME_UNSET;
    private long durationUs = C.TIME_UNSET;
    private final ParsableByteArray packetBuffer = new ParsableByteArray();

    TsDurationReader(int timestampSearchBytes) {
        this.timestampSearchBytes = timestampSearchBytes;
    }

    public boolean isDurationReadFinished() {
        return this.isDurationRead;
    }

    public int readDuration(ExtractorInput input, PositionHolder seekPositionHolder, int pcrPid) throws IOException {
        if (pcrPid <= 0) {
            return finishReadDuration(input);
        }
        if (!this.isLastPcrValueRead) {
            return readLastPcrValue(input, seekPositionHolder, pcrPid);
        }
        if (this.lastPcrValue == C.TIME_UNSET) {
            return finishReadDuration(input);
        }
        if (!this.isFirstPcrValueRead) {
            return readFirstPcrValue(input, seekPositionHolder, pcrPid);
        }
        if (this.firstPcrValue == C.TIME_UNSET) {
            return finishReadDuration(input);
        }
        long minPcrPositionUs = this.pcrTimestampAdjuster.adjustTsTimestamp(this.firstPcrValue);
        long maxPcrPositionUs = this.pcrTimestampAdjuster.adjustTsTimestampGreaterThanPreviousTimestamp(this.lastPcrValue);
        this.durationUs = maxPcrPositionUs - minPcrPositionUs;
        return finishReadDuration(input);
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public TimestampAdjuster getPcrTimestampAdjuster() {
        return this.pcrTimestampAdjuster;
    }

    private int finishReadDuration(ExtractorInput input) {
        this.packetBuffer.reset(Util.EMPTY_BYTE_ARRAY);
        this.isDurationRead = true;
        input.resetPeekPosition();
        return 0;
    }

    private int readFirstPcrValue(ExtractorInput input, PositionHolder seekPositionHolder, int pcrPid) throws IOException {
        int bytesToSearch = (int) Math.min(this.timestampSearchBytes, input.getLength());
        if (input.getPosition() != 0) {
            seekPositionHolder.position = 0;
            return 1;
        }
        this.packetBuffer.reset(bytesToSearch);
        input.resetPeekPosition();
        input.peekFully(this.packetBuffer.getData(), 0, bytesToSearch);
        this.firstPcrValue = readFirstPcrValueFromBuffer(this.packetBuffer, pcrPid);
        this.isFirstPcrValueRead = true;
        return 0;
    }

    private long readFirstPcrValueFromBuffer(ParsableByteArray packetBuffer, int pcrPid) {
        int searchStartPosition = packetBuffer.getPosition();
        int searchEndPosition = packetBuffer.limit();
        for (int searchPosition = searchStartPosition; searchPosition < searchEndPosition; searchPosition++) {
            if (packetBuffer.getData()[searchPosition] == 71) {
                long pcrValue = TsUtil.readPcrFromPacket(packetBuffer, searchPosition, pcrPid);
                if (pcrValue != C.TIME_UNSET) {
                    return pcrValue;
                }
            }
        }
        return C.TIME_UNSET;
    }

    private int readLastPcrValue(ExtractorInput input, PositionHolder seekPositionHolder, int pcrPid) throws IOException {
        long inputLength = input.getLength();
        int bytesToSearch = (int) Math.min(this.timestampSearchBytes, inputLength);
        long searchStartPosition = inputLength - ((long) bytesToSearch);
        if (input.getPosition() != searchStartPosition) {
            seekPositionHolder.position = searchStartPosition;
            return 1;
        }
        this.packetBuffer.reset(bytesToSearch);
        input.resetPeekPosition();
        input.peekFully(this.packetBuffer.getData(), 0, bytesToSearch);
        this.lastPcrValue = readLastPcrValueFromBuffer(this.packetBuffer, pcrPid);
        this.isLastPcrValueRead = true;
        return 0;
    }

    private long readLastPcrValueFromBuffer(ParsableByteArray packetBuffer, int pcrPid) {
        int searchStartPosition = packetBuffer.getPosition();
        int searchEndPosition = packetBuffer.limit();
        for (int searchPosition = searchEndPosition - 188; searchPosition >= searchStartPosition; searchPosition--) {
            if (TsUtil.isStartOfTsPacket(packetBuffer.getData(), searchStartPosition, searchEndPosition, searchPosition)) {
                long pcrValue = TsUtil.readPcrFromPacket(packetBuffer, searchPosition, pcrPid);
                if (pcrValue != C.TIME_UNSET) {
                    return pcrValue;
                }
            }
        }
        return C.TIME_UNSET;
    }
}
