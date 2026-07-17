package androidx.media3.common.util;

import android.util.Pair;
import androidx.exifinterface.media.ExifInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class CodecSpecificDataUtil {
    private static final int EXTENDED_PAR = 15;
    private static final int RECTANGULAR = 0;
    private static final int VISUAL_OBJECT_LAYER = 1;
    private static final int VISUAL_OBJECT_LAYER_START = 32;
    private static final byte[] NAL_START_CODE = {0, 0, 0, 1};
    private static final String[] HEVC_GENERAL_PROFILE_SPACE_STRINGS = {"", ExifInterface.GPS_MEASUREMENT_IN_PROGRESS, "B", "C"};

    public static Pair<Integer, Integer> parseAlacAudioSpecificConfig(byte[] audioSpecificConfig) {
        ParsableByteArray byteArray = new ParsableByteArray(audioSpecificConfig);
        byteArray.setPosition(9);
        int channelCount = byteArray.readUnsignedByte();
        byteArray.setPosition(20);
        int sampleRate = byteArray.readUnsignedIntToInt();
        return Pair.create(Integer.valueOf(sampleRate), Integer.valueOf(channelCount));
    }

    public static List<byte[]> buildCea708InitializationData(boolean isWideAspectRatio) {
        byte[] bArr = new byte[1];
        if (isWideAspectRatio) {
            bArr[0] = 1;
        } else {
            bArr[0] = 0;
        }
        return Collections.singletonList(bArr);
    }

    public static boolean parseCea708InitializationData(List<byte[]> initializationData) {
        return initializationData.size() == 1 && initializationData.get(0).length == 1 && initializationData.get(0)[0] == 1;
    }

    public static Pair<Integer, Integer> getVideoResolutionFromMpeg4VideoConfig(byte[] videoSpecificConfig) {
        int offset = 0;
        boolean foundVOL = false;
        ParsableByteArray scratchBytes = new ParsableByteArray(videoSpecificConfig);
        while (offset + 3 < videoSpecificConfig.length) {
            if (scratchBytes.readUnsignedInt24() != 1 || (videoSpecificConfig[offset + 3] & 240) != 32) {
                scratchBytes.setPosition(scratchBytes.getPosition() - 2);
                offset++;
            } else {
                foundVOL = true;
                break;
            }
        }
        Assertions.checkArgument(foundVOL, "Invalid input: VOL not found.");
        ParsableBitArray scratchBits = new ParsableBitArray(videoSpecificConfig);
        scratchBits.skipBits((offset + 4) * 8);
        scratchBits.skipBits(1);
        scratchBits.skipBits(8);
        if (scratchBits.readBit()) {
            scratchBits.skipBits(4);
            scratchBits.skipBits(3);
        }
        int aspectRatioInfo = scratchBits.readBits(4);
        if (aspectRatioInfo == 15) {
            scratchBits.skipBits(8);
            scratchBits.skipBits(8);
        }
        if (scratchBits.readBit()) {
            scratchBits.skipBits(2);
            scratchBits.skipBits(1);
            if (scratchBits.readBit()) {
                scratchBits.skipBits(79);
            }
        }
        int videoObjectLayerShape = scratchBits.readBits(2);
        Assertions.checkArgument(videoObjectLayerShape == 0, "Only supports rectangular video object layer shape.");
        Assertions.checkArgument(scratchBits.readBit());
        int vopTimeIncrementResolution = scratchBits.readBits(16);
        Assertions.checkArgument(scratchBits.readBit());
        if (scratchBits.readBit()) {
            Assertions.checkArgument(vopTimeIncrementResolution > 0);
            int numBitsToSkip = 0;
            for (int vopTimeIncrementResolution2 = vopTimeIncrementResolution - 1; vopTimeIncrementResolution2 > 0; vopTimeIncrementResolution2 >>= 1) {
                numBitsToSkip++;
            }
            scratchBits.skipBits(numBitsToSkip);
        }
        Assertions.checkArgument(scratchBits.readBit());
        int videoObjectLayerWidth = scratchBits.readBits(13);
        Assertions.checkArgument(scratchBits.readBit());
        int videoObjectLayerHeight = scratchBits.readBits(13);
        Assertions.checkArgument(scratchBits.readBit());
        scratchBits.skipBits(1);
        return Pair.create(Integer.valueOf(videoObjectLayerWidth), Integer.valueOf(videoObjectLayerHeight));
    }

    public static String buildAvcCodecString(int profileIdc, int constraintsFlagsAndReservedZero2Bits, int levelIdc) {
        return String.format("avc1.%02X%02X%02X", Integer.valueOf(profileIdc), Integer.valueOf(constraintsFlagsAndReservedZero2Bits), Integer.valueOf(levelIdc));
    }

    public static String buildHevcCodecString(int generalProfileSpace, boolean generalTierFlag, int generalProfileIdc, int generalProfileCompatibilityFlags, int[] constraintBytes, int generalLevelIdc) {
        StringBuilder builder = new StringBuilder(Util.formatInvariant("hvc1.%s%d.%X.%c%d", HEVC_GENERAL_PROFILE_SPACE_STRINGS[generalProfileSpace], Integer.valueOf(generalProfileIdc), Integer.valueOf(generalProfileCompatibilityFlags), Character.valueOf(generalTierFlag ? 'H' : 'L'), Integer.valueOf(generalLevelIdc)));
        int trailingZeroIndex = constraintBytes.length;
        while (trailingZeroIndex > 0 && constraintBytes[trailingZeroIndex - 1] == 0) {
            trailingZeroIndex--;
        }
        for (int i = 0; i < trailingZeroIndex; i++) {
            builder.append(String.format(".%02X", Integer.valueOf(constraintBytes[i])));
        }
        return builder.toString();
    }

    public static byte[] buildNalUnit(byte[] data, int offset, int length) {
        byte[] nalUnit = new byte[NAL_START_CODE.length + length];
        System.arraycopy(NAL_START_CODE, 0, nalUnit, 0, NAL_START_CODE.length);
        System.arraycopy(data, offset, nalUnit, NAL_START_CODE.length, length);
        return nalUnit;
    }

    public static byte[][] splitNalUnits(byte[] data) {
        if (!isNalStartCode(data, 0)) {
            return null;
        }
        List<Integer> starts = new ArrayList<>();
        int nalUnitIndex = 0;
        do {
            starts.add(Integer.valueOf(nalUnitIndex));
            nalUnitIndex = findNalStartCode(data, NAL_START_CODE.length + nalUnitIndex);
        } while (nalUnitIndex != -1);
        byte[][] split = new byte[starts.size()][];
        int i = 0;
        while (i < starts.size()) {
            int startIndex = starts.get(i).intValue();
            int endIndex = i < starts.size() + (-1) ? starts.get(i + 1).intValue() : data.length;
            byte[] nal = new byte[endIndex - startIndex];
            System.arraycopy(data, startIndex, nal, 0, nal.length);
            split[i] = nal;
            i++;
        }
        return split;
    }

    private static int findNalStartCode(byte[] data, int index) {
        int endIndex = data.length - NAL_START_CODE.length;
        for (int i = index; i <= endIndex; i++) {
            if (isNalStartCode(data, i)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isNalStartCode(byte[] data, int index) {
        if (data.length - index <= NAL_START_CODE.length) {
            return false;
        }
        for (int j = 0; j < NAL_START_CODE.length; j++) {
            if (data[index + j] != NAL_START_CODE[j]) {
                return false;
            }
        }
        return true;
    }

    private CodecSpecificDataUtil() {
    }
}
