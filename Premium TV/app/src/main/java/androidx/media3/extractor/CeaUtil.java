package androidx.media3.extractor;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;

/* JADX INFO: loaded from: classes.dex */
public final class CeaUtil {
    private static final int COUNTRY_CODE = 181;
    private static final int PAYLOAD_TYPE_CC = 4;
    private static final int PROVIDER_CODE_ATSC = 49;
    private static final int PROVIDER_CODE_DIRECTV = 47;
    private static final String TAG = "CeaUtil";
    public static final int USER_DATA_IDENTIFIER_GA94 = 1195456820;
    public static final int USER_DATA_TYPE_CODE_MPEG_CC = 3;

    public static void consume(long presentationTimeUs, ParsableByteArray seiBuffer, TrackOutput[] outputs) {
        while (true) {
            if (seiBuffer.bytesLeft() > 1) {
                int payloadType = readNon255TerminatedValue(seiBuffer);
                int payloadSize = readNon255TerminatedValue(seiBuffer);
                int nextPayloadPosition = seiBuffer.getPosition() + payloadSize;
                if (payloadSize == -1 || payloadSize > seiBuffer.bytesLeft()) {
                    Log.w(TAG, "Skipping remainder of malformed SEI NAL unit.");
                    nextPayloadPosition = seiBuffer.limit();
                } else if (payloadType == 4 && payloadSize >= 8) {
                    int countryCode = seiBuffer.readUnsignedByte();
                    int providerCode = seiBuffer.readUnsignedShort();
                    int userIdentifier = 0;
                    if (providerCode == PROVIDER_CODE_ATSC) {
                        userIdentifier = seiBuffer.readInt();
                    }
                    int userDataTypeCode = seiBuffer.readUnsignedByte();
                    if (providerCode == 47) {
                        seiBuffer.skipBytes(1);
                    }
                    boolean messageIsSupportedCeaCaption = countryCode == COUNTRY_CODE && (providerCode == PROVIDER_CODE_ATSC || providerCode == 47) && userDataTypeCode == 3;
                    if (providerCode == PROVIDER_CODE_ATSC) {
                        messageIsSupportedCeaCaption &= userIdentifier == 1195456820;
                    }
                    if (messageIsSupportedCeaCaption) {
                        consumeCcData(presentationTimeUs, seiBuffer, outputs);
                    }
                }
                seiBuffer.setPosition(nextPayloadPosition);
            } else {
                return;
            }
        }
    }

    public static void consumeCcData(long presentationTimeUs, ParsableByteArray ccDataBuffer, TrackOutput[] outputs) {
        int firstByte = ccDataBuffer.readUnsignedByte();
        boolean processCcDataFlag = (firstByte & 64) != 0;
        if (!processCcDataFlag) {
            return;
        }
        int ccCount = firstByte & 31;
        ccDataBuffer.skipBytes(1);
        int sampleLength = ccCount * 3;
        int sampleStartPosition = ccDataBuffer.getPosition();
        int length = outputs.length;
        int i = 0;
        while (i < length) {
            int i2 = i;
            TrackOutput output = outputs[i2];
            ccDataBuffer.setPosition(sampleStartPosition);
            output.sampleData(ccDataBuffer, sampleLength);
            Assertions.checkState(presentationTimeUs != C.TIME_UNSET);
            output.sampleMetadata(presentationTimeUs, 1, sampleLength, 0, null);
            i = i2 + 1;
        }
    }

    private static int readNon255TerminatedValue(ParsableByteArray buffer) {
        int value = 0;
        while (buffer.bytesLeft() != 0) {
            int b = buffer.readUnsignedByte();
            value += b;
            if (b != 255) {
                return value;
            }
        }
        return -1;
    }

    private CeaUtil() {
    }
}
