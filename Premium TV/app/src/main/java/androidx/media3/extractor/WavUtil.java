package androidx.media3.extractor;

import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class WavUtil {
    public static final int DATA_FOURCC = 1684108385;
    public static final int DS64_FOURCC = 1685272116;
    public static final int FMT_FOURCC = 1718449184;
    public static final int RF64_FOURCC = 1380333108;
    public static final int RIFF_FOURCC = 1380533830;
    public static final int TYPE_ALAW = 6;
    public static final int TYPE_FLOAT = 3;
    public static final int TYPE_IMA_ADPCM = 17;
    public static final int TYPE_MLAW = 7;
    public static final int TYPE_PCM = 1;
    public static final int TYPE_WAVE_FORMAT_EXTENSIBLE = 65534;
    public static final int WAVE_FOURCC = 1463899717;

    public static int getTypeForPcmEncoding(int pcmEncoding) {
        switch (pcmEncoding) {
            case 2:
            case 3:
            case 21:
            case 22:
                return 1;
            case 4:
                return 3;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static int getPcmEncodingForType(int type, int bitsPerSample) {
        switch (type) {
            case 1:
            case TYPE_WAVE_FORMAT_EXTENSIBLE /* 65534 */:
                return Util.getPcmEncoding(bitsPerSample);
            case 3:
                return bitsPerSample == 32 ? 4 : 0;
            default:
                return 0;
        }
    }

    private WavUtil() {
    }
}
