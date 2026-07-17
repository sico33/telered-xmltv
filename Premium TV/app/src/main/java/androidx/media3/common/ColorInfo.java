package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Util;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.dataflow.qual.Pure;

/* JADX INFO: loaded from: classes.dex */
public final class ColorInfo {
    public final int chromaBitdepth;
    public final int colorRange;
    public final int colorSpace;
    public final int colorTransfer;
    private int hashCode;
    public final byte[] hdrStaticInfo;
    public final int lumaBitdepth;
    public static final ColorInfo SDR_BT709_LIMITED = new Builder().setColorSpace(1).setColorRange(2).setColorTransfer(3).build();
    public static final ColorInfo SRGB_BT709_FULL = new Builder().setColorSpace(1).setColorRange(1).setColorTransfer(2).build();
    private static final String FIELD_COLOR_SPACE = Util.intToStringMaxRadix(0);
    private static final String FIELD_COLOR_RANGE = Util.intToStringMaxRadix(1);
    private static final String FIELD_COLOR_TRANSFER = Util.intToStringMaxRadix(2);
    private static final String FIELD_HDR_STATIC_INFO = Util.intToStringMaxRadix(3);
    private static final String FIELD_LUMA_BITDEPTH = Util.intToStringMaxRadix(4);
    private static final String FIELD_CHROMA_BITDEPTH = Util.intToStringMaxRadix(5);

    public static final class Builder {
        private int chromaBitdepth;
        private int colorRange;
        private int colorSpace;
        private int colorTransfer;
        private byte[] hdrStaticInfo;
        private int lumaBitdepth;

        public Builder() {
            this.colorSpace = -1;
            this.colorRange = -1;
            this.colorTransfer = -1;
            this.lumaBitdepth = -1;
            this.chromaBitdepth = -1;
        }

        private Builder(ColorInfo colorInfo) {
            this.colorSpace = colorInfo.colorSpace;
            this.colorRange = colorInfo.colorRange;
            this.colorTransfer = colorInfo.colorTransfer;
            this.hdrStaticInfo = colorInfo.hdrStaticInfo;
            this.lumaBitdepth = colorInfo.lumaBitdepth;
            this.chromaBitdepth = colorInfo.chromaBitdepth;
        }

        public Builder setColorSpace(int colorSpace) {
            this.colorSpace = colorSpace;
            return this;
        }

        public Builder setColorRange(int colorRange) {
            this.colorRange = colorRange;
            return this;
        }

        public Builder setColorTransfer(int colorTransfer) {
            this.colorTransfer = colorTransfer;
            return this;
        }

        public Builder setHdrStaticInfo(byte[] hdrStaticInfo) {
            this.hdrStaticInfo = hdrStaticInfo;
            return this;
        }

        public Builder setLumaBitdepth(int lumaBitdepth) {
            this.lumaBitdepth = lumaBitdepth;
            return this;
        }

        public Builder setChromaBitdepth(int chromaBitdepth) {
            this.chromaBitdepth = chromaBitdepth;
            return this;
        }

        public ColorInfo build() {
            return new ColorInfo(this.colorSpace, this.colorRange, this.colorTransfer, this.hdrStaticInfo, this.lumaBitdepth, this.chromaBitdepth);
        }
    }

    @EnsuresNonNullIf(expression = {"#1"}, result = false)
    public static boolean isEquivalentToAssumedSdrDefault(ColorInfo colorInfo) {
        if (colorInfo == null) {
            return true;
        }
        return (colorInfo.colorSpace == -1 || colorInfo.colorSpace == 1 || colorInfo.colorSpace == 2) && (colorInfo.colorRange == -1 || colorInfo.colorRange == 2) && ((colorInfo.colorTransfer == -1 || colorInfo.colorTransfer == 3) && colorInfo.hdrStaticInfo == null && ((colorInfo.chromaBitdepth == -1 || colorInfo.chromaBitdepth == 8) && (colorInfo.lumaBitdepth == -1 || colorInfo.lumaBitdepth == 8)));
    }

    @Pure
    public static int isoColorPrimariesToColorSpace(int isoColorPrimaries) {
        switch (isoColorPrimaries) {
            case 1:
                return 1;
            case 2:
            case 3:
            case 8:
            default:
                return -1;
            case 4:
            case 5:
            case 6:
            case 7:
                return 2;
            case 9:
                return 6;
        }
    }

    @Pure
    public static int isoTransferCharacteristicsToColorTransfer(int isoTransferCharacteristics) {
        switch (isoTransferCharacteristics) {
            case 1:
            case 6:
            case 7:
                return 3;
            case 4:
                return 10;
            case 13:
                return 2;
            case 16:
                return 6;
            case 18:
                return 7;
            default:
                return -1;
        }
    }

    public static boolean isTransferHdr(ColorInfo colorInfo) {
        return colorInfo != null && (colorInfo.colorTransfer == 7 || colorInfo.colorTransfer == 6);
    }

    private ColorInfo(int colorSpace, int colorRange, int colorTransfer, byte[] hdrStaticInfo, int lumaBitdepth, int chromaBitdepth) {
        this.colorSpace = colorSpace;
        this.colorRange = colorRange;
        this.colorTransfer = colorTransfer;
        this.hdrStaticInfo = hdrStaticInfo;
        this.lumaBitdepth = lumaBitdepth;
        this.chromaBitdepth = chromaBitdepth;
    }

    public Builder buildUpon() {
        return new Builder();
    }

    public boolean isValid() {
        return isBitdepthValid() || isDataSpaceValid();
    }

    public boolean isBitdepthValid() {
        return (this.lumaBitdepth == -1 || this.chromaBitdepth == -1) ? false : true;
    }

    public boolean isDataSpaceValid() {
        return (this.colorSpace == -1 || this.colorRange == -1 || this.colorTransfer == -1) ? false : true;
    }

    public String toLogString() {
        String dataspaceString;
        if (isDataSpaceValid()) {
            dataspaceString = Util.formatInvariant("%s/%s/%s", colorSpaceToString(this.colorSpace), colorRangeToString(this.colorRange), colorTransferToString(this.colorTransfer));
        } else {
            dataspaceString = "NA/NA/NA";
        }
        String bitdepthsString = isBitdepthValid() ? this.lumaBitdepth + "/" + this.chromaBitdepth : "NA/NA";
        return dataspaceString + "/" + bitdepthsString;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ColorInfo other = (ColorInfo) obj;
        if (this.colorSpace == other.colorSpace && this.colorRange == other.colorRange && this.colorTransfer == other.colorTransfer && Arrays.equals(this.hdrStaticInfo, other.hdrStaticInfo) && this.lumaBitdepth == other.lumaBitdepth && this.chromaBitdepth == other.chromaBitdepth) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            int result = (17 * 31) + this.colorSpace;
            this.hashCode = (((((((((result * 31) + this.colorRange) * 31) + this.colorTransfer) * 31) + Arrays.hashCode(this.hdrStaticInfo)) * 31) + this.lumaBitdepth) * 31) + this.chromaBitdepth;
        }
        int result2 = this.hashCode;
        return result2;
    }

    public String toString() {
        return "ColorInfo(" + colorSpaceToString(this.colorSpace) + ", " + colorRangeToString(this.colorRange) + ", " + colorTransferToString(this.colorTransfer) + ", " + (this.hdrStaticInfo != null) + ", " + lumaBitdepthToString(this.lumaBitdepth) + ", " + chromaBitdepthToString(this.chromaBitdepth) + ")";
    }

    private static String lumaBitdepthToString(int val) {
        return val != -1 ? val + "bit Luma" : "NA";
    }

    private static String chromaBitdepthToString(int val) {
        return val != -1 ? val + "bit Chroma" : "NA";
    }

    private static String colorSpaceToString(int colorSpace) {
        switch (colorSpace) {
            case -1:
                return "Unset color space";
            case 1:
                return "BT709";
            case 2:
                return "BT601";
            case 6:
                return "BT2020";
            default:
                return "Undefined color space " + colorSpace;
        }
    }

    private static String colorTransferToString(int colorTransfer) {
        switch (colorTransfer) {
            case -1:
                return "Unset color transfer";
            case 0:
            case 4:
            case 5:
            case 8:
            case 9:
            default:
                return "Undefined color transfer " + colorTransfer;
            case 1:
                return "Linear";
            case 2:
                return "sRGB";
            case 3:
                return "SDR SMPTE 170M";
            case 6:
                return "ST2084 PQ";
            case 7:
                return "HLG";
            case 10:
                return "Gamma 2.2";
        }
    }

    private static String colorRangeToString(int colorRange) {
        switch (colorRange) {
            case -1:
                return "Unset color range";
            case 0:
            default:
                return "Undefined color range " + colorRange;
            case 1:
                return "Full range";
            case 2:
                return "Limited range";
        }
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(FIELD_COLOR_SPACE, this.colorSpace);
        bundle.putInt(FIELD_COLOR_RANGE, this.colorRange);
        bundle.putInt(FIELD_COLOR_TRANSFER, this.colorTransfer);
        bundle.putByteArray(FIELD_HDR_STATIC_INFO, this.hdrStaticInfo);
        bundle.putInt(FIELD_LUMA_BITDEPTH, this.lumaBitdepth);
        bundle.putInt(FIELD_CHROMA_BITDEPTH, this.chromaBitdepth);
        return bundle;
    }

    public static ColorInfo fromBundle(Bundle bundle) {
        return new ColorInfo(bundle.getInt(FIELD_COLOR_SPACE, -1), bundle.getInt(FIELD_COLOR_RANGE, -1), bundle.getInt(FIELD_COLOR_TRANSFER, -1), bundle.getByteArray(FIELD_HDR_STATIC_INFO), bundle.getInt(FIELD_LUMA_BITDEPTH, -1), bundle.getInt(FIELD_CHROMA_BITDEPTH, -1));
    }
}
