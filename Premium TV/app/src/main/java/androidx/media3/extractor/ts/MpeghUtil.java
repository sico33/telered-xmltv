package androidx.media3.extractor.ts;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.extractor.AacUtil;
import androidx.media3.extractor.MpegAudioUtil;
import androidx.media3.extractor.OpusUtil;
import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
final class MpeghUtil {
    private static final int MHAS_SYNC_WORD = 12583333;

    public static class MhasPacketHeader {
        public static final int PACTYPE_EARCON = 19;
        public static final int PACTYPE_PCMCONFIG = 20;
        public static final int PACTYPE_PCMDATA = 21;
        public static final int PACTYP_AUDIOSCENEINFO = 3;
        public static final int PACTYP_AUDIOTRUNCATION = 17;
        public static final int PACTYP_BUFFERINFO = 14;
        public static final int PACTYP_CRC16 = 9;
        public static final int PACTYP_CRC32 = 10;
        public static final int PACTYP_DESCRIPTOR = 11;
        public static final int PACTYP_FILLDATA = 0;
        public static final int PACTYP_GENDATA = 18;
        public static final int PACTYP_GLOBAL_CRC16 = 15;
        public static final int PACTYP_GLOBAL_CRC32 = 16;
        public static final int PACTYP_LOUDNESS = 22;
        public static final int PACTYP_LOUDNESS_DRC = 13;
        public static final int PACTYP_MARKER = 8;
        public static final int PACTYP_MPEGH3DACFG = 1;
        public static final int PACTYP_MPEGH3DAFRAME = 2;
        public static final int PACTYP_SYNC = 6;
        public static final int PACTYP_SYNCGAP = 7;
        public static final int PACTYP_USERINTERACTION = 12;
        public long packetLabel;
        public int packetLength;
        public int packetType;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {
        }
    }

    public static boolean isSyncWord(int word) {
        return (16777215 & word) == MHAS_SYNC_WORD;
    }

    public static boolean parseMhasPacketHeader(ParsableBitArray data, MhasPacketHeader header) throws ParserException {
        data.getBytePosition();
        header.packetType = readEscapedIntValue(data, 3, 8, 8);
        if (header.packetType == -1) {
            return false;
        }
        header.packetLabel = readEscapedLongValue(data, 2, 8, 32);
        if (header.packetLabel == -1) {
            return false;
        }
        if (header.packetLabel > 16) {
            throw ParserException.createForUnsupportedContainerFeature("Contains sub-stream with an invalid packet label " + header.packetLabel);
        }
        if (header.packetLabel == 0) {
            switch (header.packetType) {
                case 1:
                    throw ParserException.createForMalformedContainer("Mpegh3daConfig packet with invalid packet label 0", null);
                case 2:
                    throw ParserException.createForMalformedContainer("Mpegh3daFrame packet with invalid packet label 0", null);
                case 17:
                    throw ParserException.createForMalformedContainer("AudioTruncation packet with invalid packet label 0", null);
            }
        }
        header.packetLength = readEscapedIntValue(data, 11, 24, 24);
        return header.packetLength != -1;
    }

    private static int getOutputFrameLength(int index) throws ParserException {
        switch (index) {
            case 0:
                return 768;
            case 1:
                return 1024;
            case 2:
            case 3:
                return 2048;
            case 4:
                return 4096;
            default:
                throw ParserException.createForUnsupportedContainerFeature("Unsupported coreSbrFrameLengthIndex " + index);
        }
    }

    private static int getSbrRatioIndex(int index) throws ParserException {
        switch (index) {
            case 0:
            case 1:
                return 0;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 1;
            default:
                throw ParserException.createForUnsupportedContainerFeature("Unsupported coreSbrFrameLengthIndex " + index);
        }
    }

    private static double getResamplingRatio(int usacSamplingFrequency) throws ParserException {
        switch (usacSamplingFrequency) {
            case 14700:
            case AacUtil.AAC_HE_V1_MAX_RATE_BYTES_PER_SECOND /* 16000 */:
                return 3.0d;
            case 22050:
            case 24000:
                return 2.0d;
            case 29400:
            case 32000:
            case 58800:
            case 64000:
                return 1.5d;
            case 44100:
            case OpusUtil.SAMPLE_RATE /* 48000 */:
            case 88200:
            case 96000:
                return 1.0d;
            default:
                throw ParserException.createForUnsupportedContainerFeature("Unsupported sampling rate " + usacSamplingFrequency);
        }
    }

    public static Mpegh3daConfig parseMpegh3daConfig(ParsableBitArray data) throws ParserException {
        int usacSamplingFrequency;
        byte[] compatibleProfileLevelSet;
        byte[] compatibleProfileLevelSet2 = null;
        int profileLevelIndication = data.readBits(8);
        int samplingFrequencyIndex = data.readBits(5);
        if (samplingFrequencyIndex == 31) {
            usacSamplingFrequency = data.readBits(24);
        } else {
            int usacSamplingFrequency2 = getSamplingFrequency(samplingFrequencyIndex);
            usacSamplingFrequency = usacSamplingFrequency2;
        }
        int coreSbrFrameLengthIndex = data.readBits(3);
        int outputFrameLength = getOutputFrameLength(coreSbrFrameLengthIndex);
        int sbrRatioIndex = getSbrRatioIndex(coreSbrFrameLengthIndex);
        data.skipBits(2);
        skipSpeakerConfig3d(data);
        int numSignals = parseSignals3d(data);
        skipMpegh3daDecoderConfig(data, numSignals, sbrRatioIndex);
        if (!data.readBit()) {
            compatibleProfileLevelSet = null;
        } else {
            int i = 4;
            int numConfigExtensions = readEscapedIntValue(data, 2, 4, 8) + 1;
            int confExtIdx = 0;
            while (confExtIdx < numConfigExtensions) {
                int usacConfigExtType = readEscapedIntValue(data, i, 8, 16);
                int usacConfigExtLength = readEscapedIntValue(data, i, 8, 16);
                if (usacConfigExtType != 7) {
                    data.skipBits(usacConfigExtLength * 8);
                } else {
                    int numCompatibleSets = data.readBits(i) + 1;
                    data.skipBits(i);
                    compatibleProfileLevelSet2 = new byte[numCompatibleSets];
                    int idx = 0;
                    while (idx < numCompatibleSets) {
                        byte[] compatibleProfileLevelSet3 = compatibleProfileLevelSet2;
                        compatibleProfileLevelSet3[idx] = (byte) data.readBits(8);
                        idx++;
                        compatibleProfileLevelSet2 = compatibleProfileLevelSet3;
                    }
                }
                confExtIdx++;
                i = 4;
            }
            compatibleProfileLevelSet = compatibleProfileLevelSet2;
        }
        double resamplingRatio = getResamplingRatio(usacSamplingFrequency);
        int samplingFrequency = (int) (((double) usacSamplingFrequency) * resamplingRatio);
        int standardFrameLength = (int) (((double) outputFrameLength) * resamplingRatio);
        return new Mpegh3daConfig(profileLevelIndication, samplingFrequency, standardFrameLength, compatibleProfileLevelSet);
    }

    private static int getSamplingFrequency(int index) throws ParserException {
        switch (index) {
            case 0:
                return 96000;
            case 1:
                return 88200;
            case 2:
                return 64000;
            case 3:
                return OpusUtil.SAMPLE_RATE;
            case 4:
                return 44100;
            case 5:
                return 32000;
            case 6:
                return 24000;
            case 7:
                return 22050;
            case 8:
                return AacUtil.AAC_HE_V1_MAX_RATE_BYTES_PER_SECOND;
            case 9:
                return 12000;
            case 10:
                return 11025;
            case 11:
                return 8000;
            case 12:
                return 7350;
            case 13:
            case 14:
            default:
                throw ParserException.createForUnsupportedContainerFeature("Unsupported sampling rate index " + index);
            case 15:
                return 57600;
            case 16:
                return 51200;
            case 17:
                return MpegAudioUtil.MAX_RATE_BYTES_PER_SECOND;
            case 18:
                return 38400;
            case 19:
                return 34150;
            case 20:
                return 28800;
            case 21:
                return 25600;
            case 22:
                return AccessibilityNodeInfoCompat.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_MAX_LENGTH;
            case 23:
                return 19200;
            case 24:
                return 17075;
            case 25:
                return 14400;
            case 26:
                return 12800;
            case 27:
                return 9600;
        }
    }

    public static int parseAudioTruncationInfo(ParsableBitArray data) {
        if (data.readBit()) {
            data.skipBits(2);
            return data.readBits(13);
        }
        return 0;
    }

    private static void skipSpeakerConfig3d(ParsableBitArray data) {
        int speakerLayoutType = data.readBits(2);
        if (speakerLayoutType == 0) {
            data.skipBits(6);
            return;
        }
        int numberOfSpeakers = readEscapedIntValue(data, 5, 8, 16) + 1;
        if (speakerLayoutType == 1) {
            data.skipBits(numberOfSpeakers * 7);
        } else if (speakerLayoutType == 2) {
            skipMpegh3daFlexibleSpeakerConfig(data, numberOfSpeakers);
        }
    }

    private static void skipMpegh3daFlexibleSpeakerConfig(ParsableBitArray data, int numberOfSpeakers) {
        boolean angularPrecision = data.readBit();
        int elevationAngleBits = 5;
        int angularPrecisionDegrees = angularPrecision ? 1 : 5;
        if (angularPrecision) {
            elevationAngleBits = 7;
        }
        int azimuthAngleBits = angularPrecision ? 8 : 6;
        int i = 0;
        while (i < numberOfSpeakers) {
            int azimuthAngle = 0;
            if (data.readBit()) {
                data.skipBits(7);
            } else {
                int elevationClass = data.readBits(2);
                if (elevationClass == 3) {
                    int elevationAngleIdx = data.readBits(elevationAngleBits);
                    int elevationAngle = elevationAngleIdx * angularPrecisionDegrees;
                    if (elevationAngle != 0) {
                        data.skipBit();
                    }
                }
                int azimuthAngleIdx = data.readBits(azimuthAngleBits);
                azimuthAngle = azimuthAngleIdx * angularPrecisionDegrees;
                if (azimuthAngle != 0 && azimuthAngle != 180) {
                    data.skipBit();
                }
                data.skipBit();
            }
            if (azimuthAngle != 0 && azimuthAngle != 180 && data.readBit()) {
                i++;
            }
            i++;
        }
    }

    private static int parseSignals3d(ParsableBitArray data) {
        int numberOfSignals = 0;
        int numberOfSignalGroupsInBitstream = data.readBits(5);
        for (int grp = 0; grp < numberOfSignalGroupsInBitstream + 1; grp++) {
            int signalGroupType = data.readBits(3);
            int bsNumberOfSignals = readEscapedIntValue(data, 5, 8, 16);
            numberOfSignals += bsNumberOfSignals + 1;
            if ((signalGroupType == 0 || signalGroupType == 2) && data.readBit()) {
                skipSpeakerConfig3d(data);
            }
        }
        return numberOfSignals;
    }

    private static void skipMpegh3daDecoderConfig(ParsableBitArray data, int numSignals, int sbrRatioIndex) {
        int numElements = readEscapedIntValue(data, 4, 8, 16) + 1;
        data.skipBit();
        for (int elemIdx = 0; elemIdx < numElements; elemIdx++) {
            int usacElementType = data.readBits(2);
            switch (usacElementType) {
                case 0:
                    parseMpegh3daCoreConfig(data);
                    if (sbrRatioIndex > 0) {
                        skipSbrConfig(data);
                    }
                    break;
                case 1:
                    boolean enhancedNoiseFilling = parseMpegh3daCoreConfig(data);
                    if (enhancedNoiseFilling) {
                        data.skipBit();
                    }
                    int stereoConfigIndex = 0;
                    if (sbrRatioIndex > 0) {
                        skipSbrConfig(data);
                        stereoConfigIndex = data.readBits(2);
                    }
                    if (stereoConfigIndex > 0) {
                        data.skipBits(6);
                        int bsTempShapeConfig = data.readBits(2);
                        data.skipBits(4);
                        if (data.readBit()) {
                            data.skipBits(5);
                        }
                        if (stereoConfigIndex == 2 || stereoConfigIndex == 3) {
                            data.skipBits(6);
                        }
                        if (bsTempShapeConfig == 2) {
                            data.skipBit();
                        }
                    }
                    int nBits = ((int) Math.floor(Math.log(numSignals - 1) / Math.log(2.0d))) + 1;
                    int qceIndex = data.readBits(2);
                    if (qceIndex > 0 && data.readBit()) {
                        data.skipBits(nBits);
                    }
                    if (data.readBit()) {
                        data.skipBits(nBits);
                    }
                    if (sbrRatioIndex == 0 && qceIndex == 0) {
                        data.skipBit();
                    }
                    break;
                case 3:
                    readEscapedIntValue(data, 4, 8, 16);
                    int usacExtElementConfigLength = readEscapedIntValue(data, 4, 8, 16);
                    if (data.readBit()) {
                        readEscapedIntValue(data, 8, 16, 0);
                    }
                    data.skipBit();
                    if (usacExtElementConfigLength > 0) {
                        data.skipBits(usacExtElementConfigLength * 8);
                    }
                    break;
            }
        }
    }

    private static boolean parseMpegh3daCoreConfig(ParsableBitArray data) {
        data.skipBits(3);
        boolean enhancedNoiseFilling = data.readBit();
        if (enhancedNoiseFilling) {
            data.skipBits(13);
        }
        return enhancedNoiseFilling;
    }

    private static void skipSbrConfig(ParsableBitArray data) {
        data.skipBits(3);
        data.skipBits(8);
        boolean dfltHeaderExtra1 = data.readBit();
        boolean dfltHeaderExtra2 = data.readBit();
        if (dfltHeaderExtra1) {
            data.skipBits(5);
        }
        if (dfltHeaderExtra2) {
            data.skipBits(6);
        }
    }

    private static int readEscapedIntValue(ParsableBitArray data, int bits1, int bits2, int bits3) {
        int maxBitCount = Math.max(Math.max(bits1, bits2), bits3);
        Assertions.checkArgument(maxBitCount <= 31);
        IntMath.checkedAdd(IntMath.checkedAdd((1 << bits1) - 1, (1 << bits2) - 1), 1 << bits3);
        if (data.bitsLeft() < bits1) {
            return -1;
        }
        int value = data.readBits(bits1);
        if (value == (1 << bits1) - 1) {
            if (data.bitsLeft() < bits2) {
                return -1;
            }
            int valueAdd = data.readBits(bits2);
            int value2 = value + valueAdd;
            if (valueAdd == (1 << bits2) - 1) {
                if (data.bitsLeft() < bits3) {
                    return -1;
                }
                return value2 + data.readBits(bits3);
            }
            return value2;
        }
        return value;
    }

    private static long readEscapedLongValue(ParsableBitArray data, int bits1, int bits2, int bits3) {
        int maxBitCount = Math.max(Math.max(bits1, bits2), bits3);
        Assertions.checkArgument(maxBitCount <= 63);
        LongMath.checkedAdd(LongMath.checkedAdd((1 << bits1) - 1, (1 << bits2) - 1), 1 << bits3);
        if (data.bitsLeft() < bits1) {
            return -1L;
        }
        long value = data.readBitsToLong(bits1);
        if (value == (1 << bits1) - 1) {
            if (data.bitsLeft() < bits2) {
                return -1L;
            }
            long valueAdd = data.readBitsToLong(bits2);
            long value2 = value + valueAdd;
            if (valueAdd == (1 << bits2) - 1) {
                if (data.bitsLeft() < bits3) {
                    return -1L;
                }
                return value2 + data.readBitsToLong(bits3);
            }
            return value2;
        }
        return value;
    }

    private MpeghUtil() {
    }

    public static class Mpegh3daConfig {
        public final byte[] compatibleProfileLevelSet;
        public final int profileLevelIndication;
        public final int samplingFrequency;
        public final int standardFrameLength;

        private Mpegh3daConfig(int profileLevelIndication, int samplingFrequency, int standardFrameLength, byte[] compatibleProfileLevelSet) {
            this.profileLevelIndication = profileLevelIndication;
            this.samplingFrequency = samplingFrequency;
            this.standardFrameLength = standardFrameLength;
            this.compatibleProfileLevelSet = compatibleProfileLevelSet;
        }
    }
}
