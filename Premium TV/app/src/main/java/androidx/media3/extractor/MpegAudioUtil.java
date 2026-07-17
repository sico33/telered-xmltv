package androidx.media3.extractor;

import androidx.media3.common.MimeTypes;

/* JADX INFO: loaded from: classes.dex */
public final class MpegAudioUtil {
    public static final int MAX_FRAME_SIZE_BYTES = 4096;
    private static final int SAMPLES_PER_FRAME_L1 = 384;
    private static final int SAMPLES_PER_FRAME_L2 = 1152;
    private static final int SAMPLES_PER_FRAME_L3_V1 = 1152;
    private static final int SAMPLES_PER_FRAME_L3_V2 = 576;
    private static final String[] MIME_TYPE_BY_LAYER = {MimeTypes.AUDIO_MPEG_L1, MimeTypes.AUDIO_MPEG_L2, MimeTypes.AUDIO_MPEG};
    private static final int[] SAMPLING_RATE_V1 = {44100, OpusUtil.SAMPLE_RATE, 32000};
    private static final int[] BITRATE_V1_L1 = {32000, 64000, 96000, 128000, 160000, DtsUtil.DTS_MAX_RATE_BYTES_PER_SECOND, 224000, AacUtil.AAC_XHE_MAX_RATE_BYTES_PER_SECOND, 288000, 320000, 352000, 384000, 416000, 448000};
    private static final int[] BITRATE_V2_L1 = {32000, OpusUtil.SAMPLE_RATE, 56000, 64000, Ac3Util.AC3_MAX_RATE_BYTES_PER_SECOND, 96000, 112000, 128000, 144000, 160000, 176000, DtsUtil.DTS_MAX_RATE_BYTES_PER_SECOND, 224000, AacUtil.AAC_XHE_MAX_RATE_BYTES_PER_SECOND};
    private static final int[] BITRATE_V1_L2 = {32000, OpusUtil.SAMPLE_RATE, 56000, 64000, Ac3Util.AC3_MAX_RATE_BYTES_PER_SECOND, 96000, 112000, 128000, 160000, DtsUtil.DTS_MAX_RATE_BYTES_PER_SECOND, 224000, AacUtil.AAC_XHE_MAX_RATE_BYTES_PER_SECOND, 320000, 384000};
    public static final int MAX_RATE_BYTES_PER_SECOND = 40000;
    private static final int[] BITRATE_V1_L3 = {32000, MAX_RATE_BYTES_PER_SECOND, OpusUtil.SAMPLE_RATE, 56000, 64000, Ac3Util.AC3_MAX_RATE_BYTES_PER_SECOND, 96000, 112000, 128000, 160000, DtsUtil.DTS_MAX_RATE_BYTES_PER_SECOND, 224000, AacUtil.AAC_XHE_MAX_RATE_BYTES_PER_SECOND, 320000};
    private static final int[] BITRATE_V2 = {8000, AacUtil.AAC_HE_V1_MAX_RATE_BYTES_PER_SECOND, 24000, 32000, MAX_RATE_BYTES_PER_SECOND, OpusUtil.SAMPLE_RATE, 56000, 64000, Ac3Util.AC3_MAX_RATE_BYTES_PER_SECOND, 96000, 112000, 128000, 144000, 160000};

    public static final class Header {
        public int bitrate;
        public int channels;
        public int frameSize;
        public String mimeType;
        public int sampleRate;
        public int samplesPerFrame;
        public int version;

        public Header() {
        }

        public Header(Header header) {
            this.version = header.version;
            this.mimeType = header.mimeType;
            this.frameSize = header.frameSize;
            this.sampleRate = header.sampleRate;
            this.channels = header.channels;
            this.bitrate = header.bitrate;
            this.samplesPerFrame = header.samplesPerFrame;
        }

        public boolean setForHeaderData(int headerData) {
            int version;
            int layer;
            int bitrateIndex;
            int samplingRateIndex;
            if (!MpegAudioUtil.isMagicPresent(headerData) || (version = (headerData >>> 19) & 3) == 1 || (layer = (headerData >>> 17) & 3) == 0 || (bitrateIndex = (headerData >>> 12) & 15) == 0 || bitrateIndex == 15 || (samplingRateIndex = (headerData >>> 10) & 3) == 3) {
                return false;
            }
            this.version = version;
            this.mimeType = MpegAudioUtil.MIME_TYPE_BY_LAYER[3 - layer];
            this.sampleRate = MpegAudioUtil.SAMPLING_RATE_V1[samplingRateIndex];
            int i = 2;
            if (version == 2) {
                this.sampleRate /= 2;
            } else if (version == 0) {
                this.sampleRate /= 4;
            }
            int padding = (headerData >>> 9) & 1;
            this.samplesPerFrame = MpegAudioUtil.getFrameSizeInSamples(version, layer);
            if (layer == 3) {
                this.bitrate = version == 3 ? MpegAudioUtil.BITRATE_V1_L1[bitrateIndex - 1] : MpegAudioUtil.BITRATE_V2_L1[bitrateIndex - 1];
                this.frameSize = (((this.bitrate * 12) / this.sampleRate) + padding) * 4;
            } else {
                if (version != 3) {
                    this.bitrate = MpegAudioUtil.BITRATE_V2[bitrateIndex - 1];
                    this.frameSize = (((layer == 1 ? 72 : 144) * this.bitrate) / this.sampleRate) + padding;
                } else {
                    this.bitrate = layer == 2 ? MpegAudioUtil.BITRATE_V1_L2[bitrateIndex - 1] : MpegAudioUtil.BITRATE_V1_L3[bitrateIndex - 1];
                    this.frameSize = ((this.bitrate * 144) / this.sampleRate) + padding;
                }
            }
            if (((headerData >> 6) & 3) == 3) {
                i = 1;
            }
            this.channels = i;
            return true;
        }
    }

    public static int getFrameSize(int headerData) {
        int version;
        int layer;
        int bitrateIndex;
        int samplingRateIndex;
        int bitrate;
        if (!isMagicPresent(headerData) || (version = (headerData >>> 19) & 3) == 1 || (layer = (headerData >>> 17) & 3) == 0 || (bitrateIndex = (headerData >>> 12) & 15) == 0 || bitrateIndex == 15 || (samplingRateIndex = (headerData >>> 10) & 3) == 3) {
            return -1;
        }
        int samplingRate = SAMPLING_RATE_V1[samplingRateIndex];
        if (version == 2) {
            samplingRate /= 2;
        } else if (version == 0) {
            samplingRate /= 4;
        }
        int padding = (headerData >>> 9) & 1;
        if (layer == 3) {
            int bitrate2 = version == 3 ? BITRATE_V1_L1[bitrateIndex - 1] : BITRATE_V2_L1[bitrateIndex - 1];
            return (((bitrate2 * 12) / samplingRate) + padding) * 4;
        }
        if (version == 3) {
            bitrate = layer == 2 ? BITRATE_V1_L2[bitrateIndex - 1] : BITRATE_V1_L3[bitrateIndex - 1];
        } else {
            bitrate = BITRATE_V2[bitrateIndex - 1];
        }
        if (version == 3) {
            return ((bitrate * 144) / samplingRate) + padding;
        }
        return (((layer == 1 ? 72 : 144) * bitrate) / samplingRate) + padding;
    }

    public static int parseMpegAudioFrameSampleCount(int headerData) {
        int version;
        int layer;
        if (!isMagicPresent(headerData) || (version = (headerData >>> 19) & 3) == 1 || (layer = (headerData >>> 17) & 3) == 0) {
            return -1;
        }
        int bitrateIndex = (headerData >>> 12) & 15;
        int samplingRateIndex = (headerData >>> 10) & 3;
        if (bitrateIndex == 0 || bitrateIndex == 15 || samplingRateIndex == 3) {
            return -1;
        }
        return getFrameSizeInSamples(version, layer);
    }

    private MpegAudioUtil() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isMagicPresent(int headerData) {
        return (headerData & (-2097152)) == -2097152;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getFrameSizeInSamples(int version, int layer) {
        switch (layer) {
            case 1:
                if (version == 3) {
                    return 1152;
                }
                return SAMPLES_PER_FRAME_L3_V2;
            case 2:
                return 1152;
            case 3:
                return 384;
            default:
                throw new IllegalArgumentException();
        }
    }
}
