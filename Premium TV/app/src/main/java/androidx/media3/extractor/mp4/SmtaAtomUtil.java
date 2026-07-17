package androidx.media3.extractor.mp4;

import androidx.media3.common.C;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.metadata.mp4.SmtaMetadataEntry;
import androidx.media3.extractor.ts.PsExtractor;

/* JADX INFO: loaded from: classes.dex */
public final class SmtaAtomUtil {
    private static final int CAMCORDER_FRC_SUPERSLOW_MOTION = 9;
    private static final int CAMCORDER_FRC_SUPERSLOW_MOTION_HEVC = 22;
    private static final int CAMCORDER_NORMAL = 0;
    private static final int CAMCORDER_QFRC_SUPERSLOW_MOTION = 23;
    private static final int CAMCORDER_SINGLE_SUPERSLOW_MOTION = 7;
    private static final int CAMCORDER_SLOW_MOTION_V2 = 12;
    private static final int CAMCORDER_SLOW_MOTION_V2_120 = 13;
    private static final int CAMCORDER_SLOW_MOTION_V2_HEVC = 21;
    private static final int NO_VALUE = -1;

    private SmtaAtomUtil() {
    }

    public static Metadata parseSmta(ParsableByteArray smta, int limit) {
        smta.skipBytes(12);
        while (smta.getPosition() < limit) {
            int atomPosition = smta.getPosition();
            int atomSize = smta.readInt();
            int atomType = smta.readInt();
            if (atomType == 1935766900) {
                if (atomSize < 16) {
                    return null;
                }
                smta.skipBytes(4);
                int recordingMode = -1;
                int svcTemporalLayerCount = 0;
                for (int i = 0; i < 2; i++) {
                    int key = smta.readUnsignedByte();
                    int value = smta.readUnsignedByte();
                    if (key == 0) {
                        recordingMode = value;
                    } else if (key == 1) {
                        svcTemporalLayerCount = value;
                    }
                }
                int captureFrameRate = getCaptureFrameRate(recordingMode, smta, limit);
                if (captureFrameRate == -2147483647) {
                    return null;
                }
                return new Metadata(new SmtaMetadataEntry(captureFrameRate, svcTemporalLayerCount));
            }
            smta.setPosition(atomPosition + atomSize);
        }
        return null;
    }

    private static int getCaptureFrameRate(int recordingMode, ParsableByteArray smta, int limit) {
        if (recordingMode == 12) {
            return PsExtractor.VIDEO_STREAM_MASK;
        }
        if (recordingMode == 13) {
            return 120;
        }
        if (recordingMode != 21 || smta.bytesLeft() < 8 || smta.getPosition() + 8 > limit) {
            return C.RATE_UNSET_INT;
        }
        int atomSize = smta.readInt();
        int atomType = smta.readInt();
        return (atomSize < 12 || atomType != 1936877170) ? C.RATE_UNSET_INT : smta.readUnsignedFixedPoint1616();
    }
}
