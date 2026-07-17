package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.PositionHolder;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class PsDurationReader {
    private static final String TAG = "PsDurationReader";
    private static final int TIMESTAMP_SEARCH_BYTES = 20000;
    private boolean isDurationRead;
    private boolean isFirstScrValueRead;
    private boolean isLastScrValueRead;
    private final TimestampAdjuster scrTimestampAdjuster = new TimestampAdjuster(0);
    private long firstScrValue = C.TIME_UNSET;
    private long lastScrValue = C.TIME_UNSET;
    private long durationUs = C.TIME_UNSET;
    private final ParsableByteArray packetBuffer = new ParsableByteArray();

    PsDurationReader() {
    }

    public boolean isDurationReadFinished() {
        return this.isDurationRead;
    }

    public TimestampAdjuster getScrTimestampAdjuster() {
        return this.scrTimestampAdjuster;
    }

    public int readDuration(ExtractorInput input, PositionHolder seekPositionHolder) throws IOException {
        if (!this.isLastScrValueRead) {
            return readLastScrValue(input, seekPositionHolder);
        }
        if (this.lastScrValue == C.TIME_UNSET) {
            return finishReadDuration(input);
        }
        if (!this.isFirstScrValueRead) {
            return readFirstScrValue(input, seekPositionHolder);
        }
        if (this.firstScrValue == C.TIME_UNSET) {
            return finishReadDuration(input);
        }
        long minScrPositionUs = this.scrTimestampAdjuster.adjustTsTimestamp(this.firstScrValue);
        long maxScrPositionUs = this.scrTimestampAdjuster.adjustTsTimestampGreaterThanPreviousTimestamp(this.lastScrValue);
        this.durationUs = maxScrPositionUs - minScrPositionUs;
        return finishReadDuration(input);
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public static long readScrValueFromPack(ParsableByteArray packetBuffer) {
        int originalPosition = packetBuffer.getPosition();
        if (packetBuffer.bytesLeft() < 9) {
            return C.TIME_UNSET;
        }
        byte[] scrBytes = new byte[9];
        packetBuffer.readBytes(scrBytes, 0, scrBytes.length);
        packetBuffer.setPosition(originalPosition);
        return !checkMarkerBits(scrBytes) ? C.TIME_UNSET : readScrValueFromPackHeader(scrBytes);
    }

    private int finishReadDuration(ExtractorInput input) {
        this.packetBuffer.reset(Util.EMPTY_BYTE_ARRAY);
        this.isDurationRead = true;
        input.resetPeekPosition();
        return 0;
    }

    private int readFirstScrValue(ExtractorInput input, PositionHolder seekPositionHolder) throws IOException {
        int bytesToSearch = (int) Math.min(20000L, input.getLength());
        if (input.getPosition() != 0) {
            seekPositionHolder.position = 0;
            return 1;
        }
        this.packetBuffer.reset(bytesToSearch);
        input.resetPeekPosition();
        input.peekFully(this.packetBuffer.getData(), 0, bytesToSearch);
        this.firstScrValue = readFirstScrValueFromBuffer(this.packetBuffer);
        this.isFirstScrValueRead = true;
        return 0;
    }

    private long readFirstScrValueFromBuffer(ParsableByteArray packetBuffer) {
        int searchStartPosition = packetBuffer.getPosition();
        int searchEndPosition = packetBuffer.limit();
        for (int searchPosition = searchStartPosition; searchPosition < searchEndPosition - 3; searchPosition++) {
            int nextStartCode = peekIntAtPosition(packetBuffer.getData(), searchPosition);
            if (nextStartCode == 442) {
                packetBuffer.setPosition(searchPosition + 4);
                long scrValue = readScrValueFromPack(packetBuffer);
                if (scrValue != C.TIME_UNSET) {
                    return scrValue;
                }
            }
        }
        return C.TIME_UNSET;
    }

    private int readLastScrValue(ExtractorInput input, PositionHolder seekPositionHolder) throws IOException {
        long inputLength = input.getLength();
        int bytesToSearch = (int) Math.min(20000L, inputLength);
        long searchStartPosition = inputLength - ((long) bytesToSearch);
        if (input.getPosition() != searchStartPosition) {
            seekPositionHolder.position = searchStartPosition;
            return 1;
        }
        this.packetBuffer.reset(bytesToSearch);
        input.resetPeekPosition();
        input.peekFully(this.packetBuffer.getData(), 0, bytesToSearch);
        this.lastScrValue = readLastScrValueFromBuffer(this.packetBuffer);
        this.isLastScrValueRead = true;
        return 0;
    }

    private long readLastScrValueFromBuffer(ParsableByteArray packetBuffer) {
        int searchStartPosition = packetBuffer.getPosition();
        int searchEndPosition = packetBuffer.limit();
        for (int searchPosition = searchEndPosition - 4; searchPosition >= searchStartPosition; searchPosition--) {
            int nextStartCode = peekIntAtPosition(packetBuffer.getData(), searchPosition);
            if (nextStartCode == 442) {
                packetBuffer.setPosition(searchPosition + 4);
                long scrValue = readScrValueFromPack(packetBuffer);
                if (scrValue != C.TIME_UNSET) {
                    return scrValue;
                }
            }
        }
        return C.TIME_UNSET;
    }

    private int peekIntAtPosition(byte[] data, int position) {
        return ((data[position] & 255) << 24) | ((data[position + 1] & 255) << 16) | ((data[position + 2] & 255) << 8) | (data[position + 3] & 255);
    }

    private static boolean checkMarkerBits(byte[] scrBytes) {
        return (scrBytes[0] & 196) == 68 && (scrBytes[2] & 4) == 4 && (scrBytes[4] & 4) == 4 && (scrBytes[5] & 1) == 1 && (scrBytes[8] & 3) == 3;
    }

    private static long readScrValueFromPackHeader(byte[] scrBytes) {
        return (((((long) scrBytes[0]) & 56) >> 3) << 30) | ((((long) scrBytes[0]) & 3) << 28) | ((((long) scrBytes[1]) & 255) << 20) | (((((long) scrBytes[2]) & 248) >> 3) << 15) | ((((long) scrBytes[2]) & 3) << 13) | ((((long) scrBytes[3]) & 255) << 5) | ((((long) scrBytes[4]) & 248) >> 3);
    }
}
