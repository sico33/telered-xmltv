package androidx.media3.container;

import androidx.media3.common.ColorInfo;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import com.google.common.base.Ascii;
import java.nio.ByteBuffer;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class NalUnitUtil {
    public static final int EXTENDED_SAR = 255;
    private static final int H264_NAL_UNIT_TYPE_SEI = 6;
    private static final int H264_NAL_UNIT_TYPE_SPS = 7;
    private static final int H265_NAL_UNIT_TYPE_PREFIX_SEI = 39;
    public static final int NAL_UNIT_TYPE_AUD = 9;
    public static final int NAL_UNIT_TYPE_IDR = 5;
    public static final int NAL_UNIT_TYPE_NON_IDR = 1;
    public static final int NAL_UNIT_TYPE_PARTITION_A = 2;
    public static final int NAL_UNIT_TYPE_PPS = 8;
    public static final int NAL_UNIT_TYPE_PREFIX = 14;
    public static final int NAL_UNIT_TYPE_SEI = 6;
    public static final int NAL_UNIT_TYPE_SPS = 7;
    private static final String TAG = "NalUnitUtil";
    public static final byte[] NAL_START_CODE = {0, 0, 0, 1};
    public static final float[] ASPECT_RATIO_IDC_VALUES = {1.0f, 1.0f, 1.0909091f, 0.90909094f, 1.4545455f, 1.2121212f, 2.1818182f, 1.8181819f, 2.909091f, 2.4242425f, 1.6363636f, 1.3636364f, 1.939394f, 1.6161616f, 1.3333334f, 1.5f, 2.0f};
    private static final Object scratchEscapePositionsLock = new Object();
    private static int[] scratchEscapePositions = new int[10];

    public static final class SpsData {
        public final int bitDepthChromaMinus8;
        public final int bitDepthLumaMinus8;
        public final int colorRange;
        public final int colorSpace;
        public final int colorTransfer;
        public final int constraintsFlagsAndReservedZero2Bits;
        public final boolean deltaPicOrderAlwaysZeroFlag;
        public final boolean frameMbsOnlyFlag;
        public final int frameNumLength;
        public final int height;
        public final int levelIdc;
        public final int maxNumRefFrames;
        public final int maxNumReorderFrames;
        public final int picOrderCntLsbLength;
        public final int picOrderCountType;
        public final float pixelWidthHeightRatio;
        public final int profileIdc;
        public final boolean separateColorPlaneFlag;
        public final int seqParameterSetId;
        public final int width;

        public SpsData(int profileIdc, int constraintsFlagsAndReservedZero2Bits, int levelIdc, int seqParameterSetId, int maxNumRefFrames, int width, int height, float pixelWidthHeightRatio, int bitDepthLumaMinus8, int bitDepthChromaMinus8, boolean separateColorPlaneFlag, boolean frameMbsOnlyFlag, int frameNumLength, int picOrderCountType, int picOrderCntLsbLength, boolean deltaPicOrderAlwaysZeroFlag, int colorSpace, int colorRange, int colorTransfer, int maxNumReorderFrames) {
            this.profileIdc = profileIdc;
            this.constraintsFlagsAndReservedZero2Bits = constraintsFlagsAndReservedZero2Bits;
            this.levelIdc = levelIdc;
            this.seqParameterSetId = seqParameterSetId;
            this.maxNumRefFrames = maxNumRefFrames;
            this.width = width;
            this.height = height;
            this.pixelWidthHeightRatio = pixelWidthHeightRatio;
            this.bitDepthLumaMinus8 = bitDepthLumaMinus8;
            this.bitDepthChromaMinus8 = bitDepthChromaMinus8;
            this.separateColorPlaneFlag = separateColorPlaneFlag;
            this.frameMbsOnlyFlag = frameMbsOnlyFlag;
            this.frameNumLength = frameNumLength;
            this.picOrderCountType = picOrderCountType;
            this.picOrderCntLsbLength = picOrderCntLsbLength;
            this.deltaPicOrderAlwaysZeroFlag = deltaPicOrderAlwaysZeroFlag;
            this.colorSpace = colorSpace;
            this.colorRange = colorRange;
            this.colorTransfer = colorTransfer;
            this.maxNumReorderFrames = maxNumReorderFrames;
        }
    }

    public static final class H265SpsData {
        public final int bitDepthChromaMinus8;
        public final int bitDepthLumaMinus8;
        public final int chromaFormatIdc;
        public final int colorRange;
        public final int colorSpace;
        public final int colorTransfer;
        public final int[] constraintBytes;
        public final int generalLevelIdc;
        public final int generalProfileCompatibilityFlags;
        public final int generalProfileIdc;
        public final int generalProfileSpace;
        public final boolean generalTierFlag;
        public final int height;
        public final int maxNumReorderPics;
        public final float pixelWidthHeightRatio;
        public final int seqParameterSetId;
        public final int width;

        public H265SpsData(int generalProfileSpace, boolean generalTierFlag, int generalProfileIdc, int generalProfileCompatibilityFlags, int chromaFormatIdc, int bitDepthLumaMinus8, int bitDepthChromaMinus8, int[] constraintBytes, int generalLevelIdc, int seqParameterSetId, int width, int height, float pixelWidthHeightRatio, int maxNumReorderPics, int colorSpace, int colorRange, int colorTransfer) {
            this.generalProfileSpace = generalProfileSpace;
            this.generalTierFlag = generalTierFlag;
            this.generalProfileIdc = generalProfileIdc;
            this.generalProfileCompatibilityFlags = generalProfileCompatibilityFlags;
            this.chromaFormatIdc = chromaFormatIdc;
            this.bitDepthLumaMinus8 = bitDepthLumaMinus8;
            this.bitDepthChromaMinus8 = bitDepthChromaMinus8;
            this.constraintBytes = constraintBytes;
            this.generalLevelIdc = generalLevelIdc;
            this.seqParameterSetId = seqParameterSetId;
            this.width = width;
            this.height = height;
            this.pixelWidthHeightRatio = pixelWidthHeightRatio;
            this.maxNumReorderPics = maxNumReorderPics;
            this.colorSpace = colorSpace;
            this.colorRange = colorRange;
            this.colorTransfer = colorTransfer;
        }
    }

    public static final class PpsData {
        public final boolean bottomFieldPicOrderInFramePresentFlag;
        public final int picParameterSetId;
        public final int seqParameterSetId;

        public PpsData(int picParameterSetId, int seqParameterSetId, boolean bottomFieldPicOrderInFramePresentFlag) {
            this.picParameterSetId = picParameterSetId;
            this.seqParameterSetId = seqParameterSetId;
            this.bottomFieldPicOrderInFramePresentFlag = bottomFieldPicOrderInFramePresentFlag;
        }
    }

    public static int unescapeStream(byte[] data, int limit) {
        int unescapedLength;
        synchronized (scratchEscapePositionsLock) {
            int position = 0;
            int scratchEscapeCount = 0;
            while (position < limit) {
                try {
                    position = findNextUnescapeIndex(data, position, limit);
                    if (position < limit) {
                        if (scratchEscapePositions.length <= scratchEscapeCount) {
                            scratchEscapePositions = Arrays.copyOf(scratchEscapePositions, scratchEscapePositions.length * 2);
                        }
                        scratchEscapePositions[scratchEscapeCount] = position;
                        position += 3;
                        scratchEscapeCount++;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            unescapedLength = limit - scratchEscapeCount;
            int escapedPosition = 0;
            int unescapedPosition = 0;
            for (int i = 0; i < scratchEscapeCount; i++) {
                int nextEscapePosition = scratchEscapePositions[i];
                int copyLength = nextEscapePosition - escapedPosition;
                System.arraycopy(data, escapedPosition, data, unescapedPosition, copyLength);
                int unescapedPosition2 = unescapedPosition + copyLength;
                int unescapedPosition3 = unescapedPosition2 + 1;
                data[unescapedPosition2] = 0;
                unescapedPosition = unescapedPosition3 + 1;
                data[unescapedPosition3] = 0;
                escapedPosition += copyLength + 3;
            }
            int i2 = unescapedLength - unescapedPosition;
            System.arraycopy(data, escapedPosition, data, unescapedPosition, i2);
        }
        return unescapedLength;
    }

    public static void discardToSps(ByteBuffer data) {
        int length = data.position();
        int consecutiveZeros = 0;
        for (int offset = 0; offset + 1 < length; offset++) {
            int value = data.get(offset) & 255;
            if (consecutiveZeros == 3) {
                if (value == 1 && (data.get(offset + 1) & Ascii.US) == 7) {
                    ByteBuffer offsetData = data.duplicate();
                    offsetData.position(offset - 3);
                    offsetData.limit(length);
                    data.position(0);
                    data.put(offsetData);
                    return;
                }
            } else if (value == 0) {
                consecutiveZeros++;
            }
            if (value != 0) {
                consecutiveZeros = 0;
            }
        }
        data.clear();
    }

    public static boolean isNalUnitSei(String mimeType, byte nalUnitHeaderFirstByte) {
        if (MimeTypes.VIDEO_H264.equals(mimeType) && (nalUnitHeaderFirstByte & Ascii.US) == 6) {
            return true;
        }
        return MimeTypes.VIDEO_H265.equals(mimeType) && ((nalUnitHeaderFirstByte & 126) >> 1) == 39;
    }

    public static int getNalUnitType(byte[] data, int offset) {
        return data[offset + 3] & Ascii.US;
    }

    public static int getH265NalUnitType(byte[] data, int offset) {
        return (data[offset + 3] & 126) >> 1;
    }

    public static SpsData parseSpsNalUnit(byte[] nalData, int nalOffset, int nalLimit) {
        return parseSpsNalUnitPayload(nalData, nalOffset + 1, nalLimit);
    }

    /* JADX WARN: Failed to apply debug info
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r0v0 ??, new type: androidx.media3.container.ParsableNalUnitBitArray
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.applyDebugInfo(TypeUpdate.java:77)
    	at jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor.applyDebugInfo(DebugInfoApplyVisitor.java:137)
    	at jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor.applyDebugInfo(DebugInfoApplyVisitor.java:133)
    	at jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor.searchAndApplyVarDebugInfo(DebugInfoApplyVisitor.java:79)
    	at jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor.lambda$applyDebugInfo$0(DebugInfoApplyVisitor.java:68)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor.applyDebugInfo(DebugInfoApplyVisitor.java:68)
    	at jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor.visit(DebugInfoApplyVisitor.java:55)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 8 more
     */
    /* JADX WARN: Failed to calculate best type for var: r0v0 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r0v0 ??, new type: androidx.media3.container.ParsableNalUnitBitArray
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.calculateFromBounds(FixTypesVisitor.java:159)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.setBestType(FixTypesVisitor.java:136)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.deduceType(FixTypesVisitor.java:241)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryDeduceTypes(FixTypesVisitor.java:224)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 6 more
     */
    /* JADX WARN: Failed to calculate best type for var: r0v0 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r0v0 ??, new type: androidx.media3.container.ParsableNalUnitBitArray
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r11v3 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r11v3 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r23v15 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r23v15 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r23v16 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r23v16 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r28v0 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r28v0 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r28v2 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r28v2 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r2v3 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r2v3 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.calculateFromBounds(FixTypesVisitor.java:159)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.setBestType(FixTypesVisitor.java:136)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.deduceType(FixTypesVisitor.java:241)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryDeduceTypes(FixTypesVisitor.java:224)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 6 more
     */
    /* JADX WARN: Failed to calculate best type for var: r2v3 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r2v3 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r2v4 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r2v4 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r32v3 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r32v3 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r33v3 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r33v3 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r38v5 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r38v5 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r38v6 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r38v6 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /* JADX WARN: Failed to calculate best type for var: r38v7 ??
    jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r38v7 ??, new type: int
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:147)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:125)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$1(TypeInferenceVisitor.java:103)
    	at java.base/java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:103)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
    Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
    	... 7 more
     */
    /*  JADX ERROR: Types fix failed
        jadx.core.utils.exceptions.JadxRuntimeException: Type update failed for variable: r2v3 ??, new type: int
        	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:109)
        	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:59)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryPossibleTypes(FixTypesVisitor.java:186)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.deduceType(FixTypesVisitor.java:245)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryDeduceTypes(FixTypesVisitor.java:224)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
        Caused by: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "arg" is null
        	at jadx.core.dex.visitors.typeinference.TypeUpdate.verifyType(TypeUpdate.java:210)
        	at jadx.core.dex.visitors.typeinference.TypeUpdate.queueTypeUpdate(TypeUpdate.java:171)
        	at jadx.core.dex.visitors.typeinference.TypeUpdate.sameFirstArgListener(TypeUpdate.java:454)
        	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:310)
        	at jadx.core.dex.visitors.typeinference.TypeUpdate.runUpdate(TypeUpdate.java:124)
        	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:91)
        	... 5 more
        */
    public static androidx.media3.container.NalUnitUtil.SpsData parseSpsNalUnitPayload(byte[] r40, int r41, int r42) {
        /*
            Method dump skipped, instruction units count: 672
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.media3.container.NalUnitUtil.parseSpsNalUnitPayload(byte[], int, int):androidx.media3.container.NalUnitUtil$SpsData");
    }

    public static H265SpsData parseH265SpsNalUnit(byte[] nalData, int nalOffset, int nalLimit) {
        return parseH265SpsNalUnitPayload(nalData, nalOffset + 2, nalLimit);
    }

    public static H265SpsData parseH265SpsNalUnitPayload(byte[] nalData, int nalOffset, int nalLimit) {
        int colorTransfer;
        float pixelWidthHeightRatio;
        int colorSpace;
        int colorSpace2;
        int colorPrimaries;
        ParsableNalUnitBitArray data = new ParsableNalUnitBitArray(nalData, nalOffset, nalLimit);
        data.skipBits(4);
        int maxSubLayersMinus1 = data.readBits(3);
        data.skipBit();
        int generalProfileSpace = data.readBits(2);
        boolean generalTierFlag = data.readBit();
        int generalProfileIdc = data.readBits(5);
        int generalProfileCompatibilityFlags = 0;
        for (int generalProfileCompatibilityFlags2 = 0; generalProfileCompatibilityFlags2 < 32; generalProfileCompatibilityFlags2++) {
            if (data.readBit()) {
                generalProfileCompatibilityFlags |= 1 << generalProfileCompatibilityFlags2;
            }
        }
        int[] constraintBytes = new int[6];
        for (int i = 0; i < constraintBytes.length; i++) {
            constraintBytes[i] = data.readBits(8);
        }
        int generalLevelIdc = data.readBits(8);
        int toSkip = 0;
        for (int i2 = 0; i2 < maxSubLayersMinus1; i2++) {
            if (data.readBit()) {
                toSkip += 89;
            }
            if (data.readBit()) {
                toSkip += 8;
            }
        }
        data.skipBits(toSkip);
        if (maxSubLayersMinus1 > 0) {
            data.skipBits((8 - maxSubLayersMinus1) * 2);
        }
        int seqParameterSetId = data.readUnsignedExpGolombCodedInt();
        int chromaFormatIdc = data.readUnsignedExpGolombCodedInt();
        if (chromaFormatIdc == 3) {
            data.skipBit();
        }
        int frameWidth = data.readUnsignedExpGolombCodedInt();
        int frameHeight = data.readUnsignedExpGolombCodedInt();
        if (data.readBit()) {
            int confWinLeftOffset = data.readUnsignedExpGolombCodedInt();
            int confWinRightOffset = data.readUnsignedExpGolombCodedInt();
            int confWinTopOffset = data.readUnsignedExpGolombCodedInt();
            int confWinBottomOffset = data.readUnsignedExpGolombCodedInt();
            int subWidthC = (chromaFormatIdc == 1 || chromaFormatIdc == 2) ? 2 : 1;
            int subHeightC = chromaFormatIdc == 1 ? 2 : 1;
            frameWidth -= (confWinLeftOffset + confWinRightOffset) * subWidthC;
            frameHeight -= (confWinTopOffset + confWinBottomOffset) * subHeightC;
        }
        int bitDepthLumaMinus8 = data.readUnsignedExpGolombCodedInt();
        int bitDepthChromaMinus8 = data.readUnsignedExpGolombCodedInt();
        int log2MaxPicOrderCntLsbMinus4 = data.readUnsignedExpGolombCodedInt();
        int maxNumReorderPics = -1;
        for (int i3 = data.readBit() ? 0 : maxSubLayersMinus1; i3 <= maxSubLayersMinus1; i3++) {
            data.readUnsignedExpGolombCodedInt();
            maxNumReorderPics = Math.max(data.readUnsignedExpGolombCodedInt(), maxNumReorderPics);
            data.readUnsignedExpGolombCodedInt();
        }
        data.readUnsignedExpGolombCodedInt();
        data.readUnsignedExpGolombCodedInt();
        data.readUnsignedExpGolombCodedInt();
        data.readUnsignedExpGolombCodedInt();
        data.readUnsignedExpGolombCodedInt();
        data.readUnsignedExpGolombCodedInt();
        boolean scalingListEnabled = data.readBit();
        if (scalingListEnabled && data.readBit()) {
            skipH265ScalingList(data);
        }
        data.skipBits(2);
        if (data.readBit()) {
            data.skipBits(8);
            data.readUnsignedExpGolombCodedInt();
            data.readUnsignedExpGolombCodedInt();
            data.skipBit();
        }
        skipShortTermReferencePictureSets(data);
        if (data.readBit()) {
            int numLongTermRefPicsSps = data.readUnsignedExpGolombCodedInt();
            int i4 = 0;
            while (i4 < numLongTermRefPicsSps) {
                int ltRefPicPocLsbSpsLength = log2MaxPicOrderCntLsbMinus4 + 4;
                int i5 = i4;
                int i6 = ltRefPicPocLsbSpsLength + 1;
                data.skipBits(i6);
                i4 = i5 + 1;
            }
        }
        data.skipBits(2);
        int colorRange = -1;
        int colorTransfer2 = -1;
        float pixelWidthHeightRatio2 = 1.0f;
        if (!data.readBit()) {
            colorTransfer = -1;
            pixelWidthHeightRatio = 1.0f;
            colorSpace = -1;
        } else {
            if (!data.readBit()) {
                colorSpace2 = -1;
            } else {
                colorSpace2 = -1;
                int aspectRatioIdc = data.readBits(8);
                if (aspectRatioIdc == 255) {
                    int sarWidth = data.readBits(16);
                    int sarHeight = data.readBits(16);
                    if (sarWidth != 0 && sarHeight != 0) {
                        pixelWidthHeightRatio2 = sarWidth / sarHeight;
                    }
                } else if (aspectRatioIdc < ASPECT_RATIO_IDC_VALUES.length) {
                    pixelWidthHeightRatio2 = ASPECT_RATIO_IDC_VALUES[aspectRatioIdc];
                } else {
                    Log.w(TAG, "Unexpected aspect_ratio_idc value: " + aspectRatioIdc);
                }
            }
            if (data.readBit()) {
                data.skipBit();
            }
            if (!data.readBit()) {
                colorPrimaries = colorSpace2;
            } else {
                data.skipBits(3);
                int colorRange2 = data.readBit() ? 1 : 2;
                if (!data.readBit()) {
                    colorRange = colorRange2;
                    colorPrimaries = colorSpace2;
                } else {
                    int colorPrimaries2 = data.readBits(8);
                    int transferCharacteristics = data.readBits(8);
                    data.skipBits(8);
                    int colorSpace3 = ColorInfo.isoColorPrimariesToColorSpace(colorPrimaries2);
                    colorPrimaries = colorSpace3;
                    colorTransfer2 = ColorInfo.isoTransferCharacteristicsToColorTransfer(transferCharacteristics);
                    colorRange = colorRange2;
                }
            }
            if (data.readBit()) {
                data.readUnsignedExpGolombCodedInt();
                data.readUnsignedExpGolombCodedInt();
            }
            data.skipBit();
            if (!data.readBit()) {
                colorSpace = colorPrimaries;
                colorTransfer = colorTransfer2;
                pixelWidthHeightRatio = pixelWidthHeightRatio2;
            } else {
                frameHeight *= 2;
                colorSpace = colorPrimaries;
                colorTransfer = colorTransfer2;
                pixelWidthHeightRatio = pixelWidthHeightRatio2;
            }
        }
        int bitDepthChromaMinus9 = maxNumReorderPics;
        return new H265SpsData(generalProfileSpace, generalTierFlag, generalProfileIdc, generalProfileCompatibilityFlags, chromaFormatIdc, bitDepthLumaMinus8, bitDepthChromaMinus8, constraintBytes, generalLevelIdc, seqParameterSetId, frameWidth, frameHeight, pixelWidthHeightRatio, bitDepthChromaMinus9, colorSpace, colorRange, colorTransfer);
    }

    public static PpsData parsePpsNalUnit(byte[] nalData, int nalOffset, int nalLimit) {
        return parsePpsNalUnitPayload(nalData, nalOffset + 1, nalLimit);
    }

    public static PpsData parsePpsNalUnitPayload(byte[] nalData, int nalOffset, int nalLimit) {
        ParsableNalUnitBitArray data = new ParsableNalUnitBitArray(nalData, nalOffset, nalLimit);
        int picParameterSetId = data.readUnsignedExpGolombCodedInt();
        int seqParameterSetId = data.readUnsignedExpGolombCodedInt();
        data.skipBit();
        boolean bottomFieldPicOrderInFramePresentFlag = data.readBit();
        return new PpsData(picParameterSetId, seqParameterSetId, bottomFieldPicOrderInFramePresentFlag);
    }

    public static int findNalUnit(byte[] data, int startOffset, int endOffset, boolean[] prefixFlags) {
        boolean z;
        boolean z2;
        int length = endOffset - startOffset;
        Assertions.checkState(length >= 0);
        if (length == 0) {
            return endOffset;
        }
        if (prefixFlags[0]) {
            clearPrefixFlags(prefixFlags);
            return startOffset - 3;
        }
        if (length > 1 && prefixFlags[1] && data[startOffset] == 1) {
            clearPrefixFlags(prefixFlags);
            return startOffset - 2;
        }
        if (length > 2 && prefixFlags[2] && data[startOffset] == 0 && data[startOffset + 1] == 1) {
            clearPrefixFlags(prefixFlags);
            return startOffset - 1;
        }
        int limit = endOffset - 1;
        int i = startOffset + 2;
        while (i < limit) {
            if ((data[i] & 254) == 0) {
                if (data[i - 2] == 0 && data[i - 1] == 0 && data[i] == 1) {
                    clearPrefixFlags(prefixFlags);
                    return i - 2;
                }
                i -= 2;
            }
            i += 3;
        }
        if (length > 2) {
            z = data[endOffset + (-3)] == 0 && data[endOffset + (-2)] == 0 && data[endOffset + (-1)] == 1;
        } else if (length == 2) {
            z = prefixFlags[2] && data[endOffset + (-2)] == 0 && data[endOffset + (-1)] == 1;
        } else {
            z = prefixFlags[1] && data[endOffset + (-1)] == 1;
        }
        prefixFlags[0] = z;
        if (length > 1) {
            z2 = data[endOffset + (-2)] == 0 && data[endOffset + (-1)] == 0;
        } else {
            z2 = prefixFlags[2] && data[endOffset + (-1)] == 0;
        }
        prefixFlags[1] = z2;
        prefixFlags[2] = data[endOffset + (-1)] == 0;
        return endOffset;
    }

    public static void clearPrefixFlags(boolean[] prefixFlags) {
        prefixFlags[0] = false;
        prefixFlags[1] = false;
        prefixFlags[2] = false;
    }

    private static int findNextUnescapeIndex(byte[] bytes, int offset, int limit) {
        for (int i = offset; i < limit - 2; i++) {
            if (bytes[i] == 0 && bytes[i + 1] == 0 && bytes[i + 2] == 3) {
                return i;
            }
        }
        return limit;
    }

    private static void skipScalingList(ParsableNalUnitBitArray bitArray, int size) {
        int lastScale = 8;
        int nextScale = 8;
        for (int i = 0; i < size; i++) {
            if (nextScale != 0) {
                int deltaScale = bitArray.readSignedExpGolombCodedInt();
                nextScale = ((lastScale + deltaScale) + 256) % 256;
            }
            lastScale = nextScale == 0 ? lastScale : nextScale;
        }
    }

    private static void skipHrdParameters(ParsableNalUnitBitArray data) {
        int codedPictureBufferCount = data.readUnsignedExpGolombCodedInt() + 1;
        data.skipBits(8);
        for (int i = 0; i < codedPictureBufferCount; i++) {
            data.readUnsignedExpGolombCodedInt();
            data.readUnsignedExpGolombCodedInt();
            data.skipBit();
        }
        data.skipBits(20);
    }

    private static void skipH265ScalingList(ParsableNalUnitBitArray bitArray) {
        for (int sizeId = 0; sizeId < 4; sizeId++) {
            int matrixId = 0;
            while (matrixId < 6) {
                int i = 1;
                if (!bitArray.readBit()) {
                    bitArray.readUnsignedExpGolombCodedInt();
                } else {
                    int coefNum = Math.min(64, 1 << ((sizeId << 1) + 4));
                    if (sizeId > 1) {
                        bitArray.readSignedExpGolombCodedInt();
                    }
                    for (int i2 = 0; i2 < coefNum; i2++) {
                        bitArray.readSignedExpGolombCodedInt();
                    }
                }
                if (sizeId == 3) {
                    i = 3;
                }
                matrixId += i;
            }
        }
    }

    private static void skipShortTermReferencePictureSets(ParsableNalUnitBitArray parsableNalUnitBitArray) {
        int unsignedExpGolombCodedInt;
        int unsignedExpGolombCodedInt2;
        int[] iArrCopyOf;
        int[] iArrCopyOf2;
        int unsignedExpGolombCodedInt3 = parsableNalUnitBitArray.readUnsignedExpGolombCodedInt();
        int i = -1;
        int i2 = -1;
        boolean z = false;
        int[] iArr = new int[0];
        int[] iArr2 = new int[0];
        int i3 = 0;
        while (i3 < unsignedExpGolombCodedInt3) {
            if ((i3 == 0 || !parsableNalUnitBitArray.readBit()) ? z : true) {
                int i4 = i + i2;
                int unsignedExpGolombCodedInt4 = (1 - ((parsableNalUnitBitArray.readBit() ? 1 : 0) * 2)) * (parsableNalUnitBitArray.readUnsignedExpGolombCodedInt() + 1);
                boolean[] zArr = new boolean[i4 + 1];
                for (int i5 = 0; i5 <= i4; i5++) {
                    if (!parsableNalUnitBitArray.readBit()) {
                        zArr[i5] = parsableNalUnitBitArray.readBit();
                    } else {
                        zArr[i5] = true;
                    }
                }
                int i6 = 0;
                int[] iArr3 = new int[i4 + 1];
                int[] iArr4 = new int[i4 + 1];
                for (int i7 = i2 - 1; i7 >= 0; i7--) {
                    int i8 = iArr2[i7] + unsignedExpGolombCodedInt4;
                    if (i8 < 0 && zArr[i + i7]) {
                        iArr3[i6] = i8;
                        i6++;
                    }
                }
                if (unsignedExpGolombCodedInt4 < 0 && zArr[i4]) {
                    iArr3[i6] = unsignedExpGolombCodedInt4;
                    i6++;
                }
                for (int i9 = 0; i9 < i; i9++) {
                    int i10 = iArr[i9] + unsignedExpGolombCodedInt4;
                    if (i10 < 0 && zArr[i9]) {
                        iArr3[i6] = i10;
                        i6++;
                    }
                }
                unsignedExpGolombCodedInt = i6;
                iArrCopyOf = Arrays.copyOf(iArr3, unsignedExpGolombCodedInt);
                int i11 = 0;
                for (int i12 = i - 1; i12 >= 0; i12--) {
                    int i13 = iArr[i12] + unsignedExpGolombCodedInt4;
                    if (i13 > 0 && zArr[i12]) {
                        iArr4[i11] = i13;
                        i11++;
                    }
                }
                if (unsignedExpGolombCodedInt4 > 0 && zArr[i4]) {
                    iArr4[i11] = unsignedExpGolombCodedInt4;
                    i11++;
                }
                for (int i14 = 0; i14 < i2; i14++) {
                    int i15 = iArr2[i14] + unsignedExpGolombCodedInt4;
                    if (i15 > 0 && zArr[i + i14]) {
                        iArr4[i11] = i15;
                        i11++;
                    }
                }
                unsignedExpGolombCodedInt2 = i11;
                iArrCopyOf2 = Arrays.copyOf(iArr4, unsignedExpGolombCodedInt2);
            } else {
                unsignedExpGolombCodedInt = parsableNalUnitBitArray.readUnsignedExpGolombCodedInt();
                unsignedExpGolombCodedInt2 = parsableNalUnitBitArray.readUnsignedExpGolombCodedInt();
                iArrCopyOf = new int[unsignedExpGolombCodedInt];
                int i16 = 0;
                while (i16 < unsignedExpGolombCodedInt) {
                    iArrCopyOf[i16] = (i16 > 0 ? iArrCopyOf[i16 - 1] : 0) - (parsableNalUnitBitArray.readUnsignedExpGolombCodedInt() + 1);
                    parsableNalUnitBitArray.skipBit();
                    i16++;
                }
                int[] iArr5 = new int[unsignedExpGolombCodedInt2];
                int i17 = 0;
                while (i17 < unsignedExpGolombCodedInt2) {
                    iArr5[i17] = (i17 > 0 ? iArr5[i17 - 1] : 0) + parsableNalUnitBitArray.readUnsignedExpGolombCodedInt() + 1;
                    parsableNalUnitBitArray.skipBit();
                    i17++;
                }
                iArrCopyOf2 = iArr5;
            }
            i = unsignedExpGolombCodedInt;
            i2 = unsignedExpGolombCodedInt2;
            iArr = iArrCopyOf;
            iArr2 = iArrCopyOf2;
            i3++;
            unsignedExpGolombCodedInt3 = unsignedExpGolombCodedInt3;
            z = false;
        }
    }

    private NalUnitUtil() {
    }
}
