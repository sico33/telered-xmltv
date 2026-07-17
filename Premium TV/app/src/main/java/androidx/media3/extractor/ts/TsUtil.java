package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;

/* JADX INFO: loaded from: classes.dex */
public final class TsUtil {
    public static boolean isStartOfTsPacket(byte[] data, int start, int limit, int searchPosition) {
        int consecutiveSyncByteCount = 0;
        for (int i = -4; i <= 4; i++) {
            int currentPosition = (i * TsExtractor.TS_PACKET_SIZE) + searchPosition;
            if (currentPosition < start || currentPosition >= limit || data[currentPosition] != 71) {
                consecutiveSyncByteCount = 0;
            } else {
                consecutiveSyncByteCount++;
                if (consecutiveSyncByteCount == 5) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int findSyncBytePosition(byte[] data, int startPosition, int limitPosition) {
        int position = startPosition;
        while (position < limitPosition && data[position] != 71) {
            position++;
        }
        return position;
    }

    public static long readPcrFromPacket(ParsableByteArray packetBuffer, int startOfPacket, int pcrPid) {
        packetBuffer.setPosition(startOfPacket);
        if (packetBuffer.bytesLeft() < 5) {
            return C.TIME_UNSET;
        }
        int tsPacketHeader = packetBuffer.readInt();
        if ((8388608 & tsPacketHeader) != 0) {
            return C.TIME_UNSET;
        }
        int pid = (2096896 & tsPacketHeader) >> 8;
        if (pid != pcrPid) {
            return C.TIME_UNSET;
        }
        boolean adaptationFieldExists = (tsPacketHeader & 32) != 0;
        if (!adaptationFieldExists) {
            return C.TIME_UNSET;
        }
        int adaptationFieldLength = packetBuffer.readUnsignedByte();
        if (adaptationFieldLength >= 7 && packetBuffer.bytesLeft() >= 7) {
            int flags = packetBuffer.readUnsignedByte();
            boolean pcrFlagSet = (flags & 16) == 16;
            if (pcrFlagSet) {
                byte[] pcrBytes = new byte[6];
                packetBuffer.readBytes(pcrBytes, 0, pcrBytes.length);
                return readPcrValueFromPcrBytes(pcrBytes);
            }
        }
        return C.TIME_UNSET;
    }

    private static long readPcrValueFromPcrBytes(byte[] pcrBytes) {
        return ((((long) pcrBytes[0]) & 255) << 25) | ((((long) pcrBytes[1]) & 255) << 17) | ((((long) pcrBytes[2]) & 255) << 9) | ((((long) pcrBytes[3]) & 255) << 1) | ((255 & ((long) pcrBytes[4])) >> 7);
    }

    private TsUtil() {
    }
}
