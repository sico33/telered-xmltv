package androidx.media3.common;

import android.text.TextUtils;
import androidx.core.location.LocationRequestCompat;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ts.TsExtractor;
import com.google.common.base.Ascii;
import com.google.common.primitives.SignedBytes;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.dataflow.qual.Pure;

/* JADX INFO: loaded from: classes.dex */
public final class MimeTypes {
    public static final String APPLICATION_AIT = "application/vnd.dvb.ait";
    public static final String APPLICATION_CAMERA_MOTION = "application/x-camera-motion";
    public static final String APPLICATION_CEA608 = "application/cea-608";
    public static final String APPLICATION_CEA708 = "application/cea-708";
    public static final String APPLICATION_DVBSUBS = "application/dvbsubs";
    public static final String APPLICATION_EMSG = "application/x-emsg";
    public static final String APPLICATION_EXIF = "application/x-exif";
    public static final String APPLICATION_EXTERNALLY_LOADED_IMAGE = "application/x-image-uri";
    public static final String APPLICATION_ICY = "application/x-icy";
    public static final String APPLICATION_ID3 = "application/id3";
    public static final String APPLICATION_M3U8 = "application/x-mpegURL";
    public static final String APPLICATION_MATROSKA = "application/x-matroska";
    public static final String APPLICATION_MEDIA3_CUES = "application/x-media3-cues";
    public static final String APPLICATION_MP4 = "application/mp4";
    public static final String APPLICATION_MP4CEA608 = "application/x-mp4-cea-608";
    public static final String APPLICATION_MP4VTT = "application/x-mp4-vtt";
    public static final String APPLICATION_MPD = "application/dash+xml";
    public static final String APPLICATION_PGS = "application/pgs";

    @Deprecated
    public static final String APPLICATION_RAWCC = "application/x-rawcc";
    public static final String APPLICATION_RTSP = "application/x-rtsp";
    public static final String APPLICATION_SCTE35 = "application/x-scte35";
    public static final String APPLICATION_SS = "application/vnd.ms-sstr+xml";
    public static final String APPLICATION_SUBRIP = "application/x-subrip";
    public static final String APPLICATION_TTML = "application/ttml+xml";
    public static final String APPLICATION_TX3G = "application/x-quicktime-tx3g";
    public static final String APPLICATION_VOBSUB = "application/vobsub";
    public static final String APPLICATION_WEBM = "application/webm";
    public static final String AUDIO_AAC = "audio/mp4a-latm";
    public static final String AUDIO_AC3 = "audio/ac3";
    public static final String AUDIO_AC4 = "audio/ac4";
    public static final String AUDIO_ALAC = "audio/alac";
    public static final String AUDIO_ALAW = "audio/g711-alaw";
    public static final String AUDIO_AMR = "audio/amr";
    public static final String AUDIO_AMR_NB = "audio/3gpp";
    public static final String AUDIO_AMR_WB = "audio/amr-wb";
    public static final String AUDIO_DTS = "audio/vnd.dts";
    public static final String AUDIO_DTS_EXPRESS = "audio/vnd.dts.hd;profile=lbr";
    public static final String AUDIO_DTS_HD = "audio/vnd.dts.hd";
    public static final String AUDIO_DTS_X = "audio/vnd.dts.uhd;profile=p2";
    public static final String AUDIO_EXOPLAYER_MIDI = "audio/x-exoplayer-midi";
    public static final String AUDIO_E_AC3 = "audio/eac3";
    public static final String AUDIO_E_AC3_JOC = "audio/eac3-joc";
    public static final String AUDIO_FLAC = "audio/flac";
    public static final String AUDIO_MATROSKA = "audio/x-matroska";
    public static final String AUDIO_MIDI = "audio/midi";
    public static final String AUDIO_MLAW = "audio/g711-mlaw";
    public static final String AUDIO_MP4 = "audio/mp4";
    public static final String AUDIO_MPEG = "audio/mpeg";
    public static final String AUDIO_MPEGH_MHA1 = "audio/mha1";
    public static final String AUDIO_MPEGH_MHM1 = "audio/mhm1";
    public static final String AUDIO_MPEG_L1 = "audio/mpeg-L1";
    public static final String AUDIO_MPEG_L2 = "audio/mpeg-L2";
    public static final String AUDIO_MSGSM = "audio/gsm";
    public static final String AUDIO_OGG = "audio/ogg";
    public static final String AUDIO_OPUS = "audio/opus";
    public static final String AUDIO_RAW = "audio/raw";
    public static final String AUDIO_TRUEHD = "audio/true-hd";
    public static final String AUDIO_UNKNOWN = "audio/x-unknown";
    public static final String AUDIO_VORBIS = "audio/vorbis";
    public static final String AUDIO_WAV = "audio/wav";
    public static final String AUDIO_WEBM = "audio/webm";
    public static final String BASE_TYPE_APPLICATION = "application";
    public static final String BASE_TYPE_AUDIO = "audio";
    public static final String BASE_TYPE_IMAGE = "image";
    public static final String BASE_TYPE_TEXT = "text";
    public static final String BASE_TYPE_VIDEO = "video";
    public static final String CODEC_E_AC3_JOC = "ec+3";
    public static final String IMAGE_AVIF = "image/avif";
    public static final String IMAGE_BMP = "image/bmp";
    public static final String IMAGE_HEIC = "image/heic";
    public static final String IMAGE_HEIF = "image/heif";
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_JPEG_R = "image/jpeg_r";
    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_RAW = "image/raw";
    public static final String IMAGE_WEBP = "image/webp";
    public static final String TEXT_SSA = "text/x-ssa";
    public static final String TEXT_UNKNOWN = "text/x-unknown";
    public static final String TEXT_VTT = "text/vtt";
    public static final String VIDEO_AV1 = "video/av01";
    public static final String VIDEO_AVI = "video/x-msvideo";
    public static final String VIDEO_DIVX = "video/divx";
    public static final String VIDEO_DOLBY_VISION = "video/dolby-vision";
    public static final String VIDEO_FLV = "video/x-flv";
    public static final String VIDEO_H263 = "video/3gpp";
    public static final String VIDEO_H264 = "video/avc";
    public static final String VIDEO_H265 = "video/hevc";
    public static final String VIDEO_MATROSKA = "video/x-matroska";
    public static final String VIDEO_MJPEG = "video/mjpeg";
    public static final String VIDEO_MP2T = "video/mp2t";
    public static final String VIDEO_MP4 = "video/mp4";
    public static final String VIDEO_MP42 = "video/mp42";
    public static final String VIDEO_MP43 = "video/mp43";
    public static final String VIDEO_MP4V = "video/mp4v-es";
    public static final String VIDEO_MPEG = "video/mpeg";
    public static final String VIDEO_MPEG2 = "video/mpeg2";
    public static final String VIDEO_OGG = "video/ogg";
    public static final String VIDEO_PS = "video/mp2p";
    public static final String VIDEO_RAW = "video/raw";
    public static final String VIDEO_UNKNOWN = "video/x-unknown";
    public static final String VIDEO_VC1 = "video/wvc1";
    public static final String VIDEO_VP8 = "video/x-vnd.on2.vp8";
    public static final String VIDEO_VP9 = "video/x-vnd.on2.vp9";
    public static final String VIDEO_WEBM = "video/webm";
    private static final ArrayList<CustomMimeType> customMimeTypes = new ArrayList<>();
    private static final Pattern MP4A_RFC_6381_CODEC_PATTERN = Pattern.compile("^mp4a\\.([a-zA-Z0-9]{2})(?:\\.([0-9]{1,2}))?$");

    public static void registerCustomMimeType(String mimeType, String codecPrefix, int trackType) {
        CustomMimeType customMimeType = new CustomMimeType(mimeType, codecPrefix, trackType);
        int customMimeTypeCount = customMimeTypes.size();
        for (int i = 0; i < customMimeTypeCount; i++) {
            if (mimeType.equals(customMimeTypes.get(i).mimeType)) {
                customMimeTypes.remove(i);
                break;
            }
        }
        customMimeTypes.add(customMimeType);
    }

    public static boolean isAudio(String mimeType) {
        return BASE_TYPE_AUDIO.equals(getTopLevelType(mimeType));
    }

    public static boolean isVideo(String mimeType) {
        return BASE_TYPE_VIDEO.equals(getTopLevelType(mimeType));
    }

    @Pure
    public static boolean isText(String mimeType) {
        return "text".equals(getTopLevelType(mimeType)) || APPLICATION_MEDIA3_CUES.equals(mimeType) || APPLICATION_CEA608.equals(mimeType) || APPLICATION_CEA708.equals(mimeType) || APPLICATION_MP4CEA608.equals(mimeType) || APPLICATION_SUBRIP.equals(mimeType) || APPLICATION_TTML.equals(mimeType) || APPLICATION_TX3G.equals(mimeType) || APPLICATION_MP4VTT.equals(mimeType) || APPLICATION_RAWCC.equals(mimeType) || APPLICATION_VOBSUB.equals(mimeType) || APPLICATION_PGS.equals(mimeType) || APPLICATION_DVBSUBS.equals(mimeType);
    }

    public static boolean isImage(String mimeType) {
        return "image".equals(getTopLevelType(mimeType)) || APPLICATION_EXTERNALLY_LOADED_IMAGE.equals(mimeType);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:41:0x0081  */
    public static boolean allSamplesAreSyncSamples(String mimeType, String codec) {
        Mp4aObjectType objectType;
        int encoding;
        if (mimeType == null) {
            return false;
        }
        switch (mimeType) {
            case "audio/mpeg":
            case "audio/mpeg-L1":
            case "audio/mpeg-L2":
            case "audio/raw":
            case "audio/g711-alaw":
            case "audio/g711-mlaw":
            case "audio/flac":
            case "audio/ac3":
            case "audio/eac3":
            case "audio/eac3-joc":
                return true;
            case "audio/mp4a-latm":
                return (codec == null || (objectType = getObjectTypeFromMp4aRFC6381CodecString(codec)) == null || (encoding = objectType.getEncoding()) == 0 || encoding == 16) ? false : true;
            default:
                return false;
        }
    }

    public static String getVideoMediaMimeType(String codecs) {
        if (codecs == null) {
            return null;
        }
        String[] codecList = Util.splitCodecs(codecs);
        for (String codec : codecList) {
            String mimeType = getMediaMimeType(codec);
            if (mimeType != null && isVideo(mimeType)) {
                return mimeType;
            }
        }
        return null;
    }

    public static boolean containsCodecsCorrespondingToMimeType(String codecs, String mimeType) {
        return getCodecsCorrespondingToMimeType(codecs, mimeType) != null;
    }

    public static String getCodecsCorrespondingToMimeType(String codecs, String mimeType) {
        if (codecs == null || mimeType == null) {
            return null;
        }
        String[] codecList = Util.splitCodecs(codecs);
        StringBuilder builder = new StringBuilder();
        for (String codec : codecList) {
            if (mimeType.equals(getMediaMimeType(codec))) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(codec);
            }
        }
        if (builder.length() > 0) {
            return builder.toString();
        }
        return null;
    }

    public static String getAudioMediaMimeType(String codecs) {
        if (codecs == null) {
            return null;
        }
        String[] codecList = Util.splitCodecs(codecs);
        for (String codec : codecList) {
            String mimeType = getMediaMimeType(codec);
            if (mimeType != null && isAudio(mimeType)) {
                return mimeType;
            }
        }
        return null;
    }

    public static String getTextMediaMimeType(String codecs) {
        if (codecs == null) {
            return null;
        }
        String[] codecList = Util.splitCodecs(codecs);
        for (String codec : codecList) {
            String mimeType = getMediaMimeType(codec);
            if (mimeType != null && isText(mimeType)) {
                return mimeType;
            }
        }
        return null;
    }

    public static String getMediaMimeType(String codec) {
        Mp4aObjectType objectType;
        if (codec == null) {
            return null;
        }
        String codec2 = Ascii.toLowerCase(codec.trim());
        if (codec2.startsWith("avc1") || codec2.startsWith("avc3")) {
            return VIDEO_H264;
        }
        if (codec2.startsWith("hev1") || codec2.startsWith("hvc1")) {
            return VIDEO_H265;
        }
        if (codec2.startsWith("dvav") || codec2.startsWith("dva1") || codec2.startsWith("dvhe") || codec2.startsWith("dvh1")) {
            return VIDEO_DOLBY_VISION;
        }
        if (codec2.startsWith("av01")) {
            return VIDEO_AV1;
        }
        if (codec2.startsWith("vp9") || codec2.startsWith("vp09")) {
            return VIDEO_VP9;
        }
        if (codec2.startsWith("vp8") || codec2.startsWith("vp08")) {
            return VIDEO_VP8;
        }
        if (codec2.startsWith("mp4a")) {
            String mimeType = null;
            if (codec2.startsWith("mp4a.") && (objectType = getObjectTypeFromMp4aRFC6381CodecString(codec2)) != null) {
                mimeType = getMimeTypeFromMp4ObjectType(objectType.objectTypeIndication);
            }
            return mimeType == null ? AUDIO_AAC : mimeType;
        }
        if (codec2.startsWith("mha1")) {
            return AUDIO_MPEGH_MHA1;
        }
        if (codec2.startsWith("mhm1")) {
            return AUDIO_MPEGH_MHM1;
        }
        if (codec2.startsWith("ac-3") || codec2.startsWith("dac3")) {
            return AUDIO_AC3;
        }
        if (codec2.startsWith("ec-3") || codec2.startsWith("dec3")) {
            return AUDIO_E_AC3;
        }
        if (codec2.startsWith(CODEC_E_AC3_JOC)) {
            return AUDIO_E_AC3_JOC;
        }
        if (codec2.startsWith("ac-4") || codec2.startsWith("dac4")) {
            return AUDIO_AC4;
        }
        if (codec2.startsWith("dtsc")) {
            return AUDIO_DTS;
        }
        if (codec2.startsWith("dtse")) {
            return AUDIO_DTS_EXPRESS;
        }
        if (codec2.startsWith("dtsh") || codec2.startsWith("dtsl")) {
            return AUDIO_DTS_HD;
        }
        if (codec2.startsWith("dtsx")) {
            return AUDIO_DTS_X;
        }
        if (codec2.startsWith("opus")) {
            return AUDIO_OPUS;
        }
        if (codec2.startsWith("vorbis")) {
            return AUDIO_VORBIS;
        }
        if (codec2.startsWith("flac")) {
            return AUDIO_FLAC;
        }
        if (codec2.startsWith("stpp")) {
            return APPLICATION_TTML;
        }
        if (codec2.startsWith("wvtt")) {
            return TEXT_VTT;
        }
        if (codec2.contains("cea708")) {
            return APPLICATION_CEA708;
        }
        if (codec2.contains("eia608") || codec2.contains("cea608")) {
            return APPLICATION_CEA608;
        }
        return getCustomMimeTypeForCodec(codec2);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:14:0x0027  */
    public static Byte getMp4ObjectTypeFromMimeType(String sampleMimeType) {
        switch (sampleMimeType) {
            case "audio/mp4a-latm":
                return Byte.valueOf(SignedBytes.MAX_POWER_OF_TWO);
            case "audio/vorbis":
                return (byte) -35;
            case "video/mp4v-es":
                return (byte) 32;
            default:
                return null;
        }
    }

    public static String getMimeTypeFromMp4ObjectType(int objectType) {
        switch (objectType) {
            case 32:
                return VIDEO_MP4V;
            case 33:
                return VIDEO_H264;
            case 35:
                return VIDEO_H265;
            case 64:
            case LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY /* 102 */:
            case 103:
            case LocationRequestCompat.QUALITY_LOW_POWER /* 104 */:
                return AUDIO_AAC;
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
                return VIDEO_MPEG2;
            case 105:
            case 107:
                return AUDIO_MPEG;
            case 106:
                return VIDEO_MPEG;
            case 108:
                return IMAGE_JPEG;
            case 163:
                return VIDEO_VC1;
            case 165:
                return AUDIO_AC3;
            case 166:
                return AUDIO_E_AC3;
            case 169:
            case TsExtractor.TS_STREAM_TYPE_AC4 /* 172 */:
                return AUDIO_DTS;
            case 170:
            case 171:
                return AUDIO_DTS_HD;
            case 173:
                return AUDIO_OPUS;
            case 174:
                return AUDIO_AC4;
            case 177:
                return VIDEO_VP9;
            case 221:
                return AUDIO_VORBIS;
            default:
                return null;
        }
    }

    public static int getTrackType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return -1;
        }
        if (isAudio(mimeType)) {
            return 1;
        }
        if (isVideo(mimeType)) {
            return 2;
        }
        if (isText(mimeType)) {
            return 3;
        }
        if (isImage(mimeType)) {
            return 4;
        }
        if (APPLICATION_ID3.equals(mimeType) || APPLICATION_EMSG.equals(mimeType) || APPLICATION_SCTE35.equals(mimeType)) {
            return 5;
        }
        if (APPLICATION_CAMERA_MOTION.equals(mimeType)) {
            return 6;
        }
        return getTrackTypeForCustomMimeType(mimeType);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:41:0x008e  */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    public static int getEncoding(String mimeType, String codec) {
        byte b;
        Mp4aObjectType objectType;
        switch (mimeType.hashCode()) {
            case -2123537834:
                if (!mimeType.equals(AUDIO_E_AC3_JOC)) {
                    b = -1;
                } else {
                    b = 4;
                }
                break;
            case -1365340241:
                if (!mimeType.equals(AUDIO_DTS_EXPRESS)) {
                    b = -1;
                } else {
                    b = 8;
                }
                break;
            case -1095064472:
                if (!mimeType.equals(AUDIO_DTS)) {
                    b = -1;
                } else {
                    b = 6;
                }
                break;
            case -53558318:
                if (!mimeType.equals(AUDIO_AAC)) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            case 187078296:
                if (!mimeType.equals(AUDIO_AC3)) {
                    b = -1;
                } else {
                    b = 2;
                }
                break;
            case 187078297:
                if (!mimeType.equals(AUDIO_AC4)) {
                    b = -1;
                } else {
                    b = 5;
                }
                break;
            case 550520934:
                if (!mimeType.equals(AUDIO_DTS_X)) {
                    b = -1;
                } else {
                    b = 9;
                }
                break;
            case 1504578661:
                if (!mimeType.equals(AUDIO_E_AC3)) {
                    b = -1;
                } else {
                    b = 3;
                }
                break;
            case 1504831518:
                if (!mimeType.equals(AUDIO_MPEG)) {
                    b = -1;
                } else {
                    b = 0;
                }
                break;
            case 1504891608:
                if (!mimeType.equals(AUDIO_OPUS)) {
                    b = -1;
                } else {
                    b = Ascii.VT;
                }
                break;
            case 1505942594:
                if (!mimeType.equals(AUDIO_DTS_HD)) {
                    b = -1;
                } else {
                    b = 7;
                }
                break;
            case 1556697186:
                if (!mimeType.equals(AUDIO_TRUEHD)) {
                    b = -1;
                } else {
                    b = 10;
                }
                break;
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
                return 9;
            case 1:
                if (codec == null || (objectType = getObjectTypeFromMp4aRFC6381CodecString(codec)) == null) {
                    return 0;
                }
                return objectType.getEncoding();
            case 2:
                return 5;
            case 3:
                return 6;
            case 4:
                return 18;
            case 5:
                return 17;
            case 6:
                return 7;
            case 7:
                return 8;
            case 8:
                return 8;
            case 9:
                return 30;
            case 10:
                return 14;
            case 11:
                return 20;
            default:
                return 0;
        }
    }

    public static int getTrackTypeOfCodec(String codec) {
        return getTrackType(getMediaMimeType(codec));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:26:0x004c  */
    public static String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        String mimeType2 = Ascii.toLowerCase(mimeType);
        switch (mimeType2) {
            case "audio/x-flac":
                return AUDIO_FLAC;
            case "audio/mp3":
                return AUDIO_MPEG;
            case "audio/x-wav":
                return AUDIO_WAV;
            case "application/x-mpegurl":
                return APPLICATION_M3U8;
            case "audio/mpeg-l1":
                return AUDIO_MPEG_L1;
            case "audio/mpeg-l2":
                return AUDIO_MPEG_L2;
            default:
                return mimeType2;
        }
    }

    public static boolean isMatroska(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith(VIDEO_WEBM) || mimeType.startsWith(AUDIO_WEBM) || mimeType.startsWith(APPLICATION_WEBM) || mimeType.startsWith(VIDEO_MATROSKA) || mimeType.startsWith(AUDIO_MATROSKA) || mimeType.startsWith(APPLICATION_MATROSKA);
    }

    private static String getTopLevelType(String mimeType) {
        int indexOfSlash;
        if (mimeType == null || (indexOfSlash = mimeType.indexOf(47)) == -1) {
            return null;
        }
        return mimeType.substring(0, indexOfSlash);
    }

    private static String getCustomMimeTypeForCodec(String codec) {
        int customMimeTypeCount = customMimeTypes.size();
        for (int i = 0; i < customMimeTypeCount; i++) {
            CustomMimeType customMimeType = customMimeTypes.get(i);
            if (codec.startsWith(customMimeType.codecPrefix)) {
                return customMimeType.mimeType;
            }
        }
        return null;
    }

    private static int getTrackTypeForCustomMimeType(String mimeType) {
        int customMimeTypeCount = customMimeTypes.size();
        for (int i = 0; i < customMimeTypeCount; i++) {
            CustomMimeType customMimeType = customMimeTypes.get(i);
            if (mimeType.equals(customMimeType.mimeType)) {
                return customMimeType.trackType;
            }
        }
        return -1;
    }

    private MimeTypes() {
    }

    static Mp4aObjectType getObjectTypeFromMp4aRFC6381CodecString(String codec) {
        Matcher matcher = MP4A_RFC_6381_CODEC_PATTERN.matcher(codec);
        if (!matcher.matches()) {
            return null;
        }
        String objectTypeIndicationHex = (String) Assertions.checkNotNull(matcher.group(1));
        String audioObjectTypeIndicationDec = matcher.group(2);
        int audioObjectTypeIndication = 0;
        try {
            int objectTypeIndication = Integer.parseInt(objectTypeIndicationHex, 16);
            if (audioObjectTypeIndicationDec != null) {
                audioObjectTypeIndication = Integer.parseInt(audioObjectTypeIndicationDec);
            }
            return new Mp4aObjectType(objectTypeIndication, audioObjectTypeIndication);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static final class Mp4aObjectType {
        public final int audioObjectTypeIndication;
        public final int objectTypeIndication;

        public Mp4aObjectType(int objectTypeIndication, int audioObjectTypeIndication) {
            this.objectTypeIndication = objectTypeIndication;
            this.audioObjectTypeIndication = audioObjectTypeIndication;
        }

        public int getEncoding() {
            switch (this.audioObjectTypeIndication) {
                case 2:
                    return 10;
                case 5:
                    return 11;
                case 22:
                    return 1073741824;
                case 23:
                    return 15;
                case 29:
                    return 12;
                case 42:
                    return 16;
                default:
                    return 0;
            }
        }
    }

    private static final class CustomMimeType {
        public final String codecPrefix;
        public final String mimeType;
        public final int trackType;

        public CustomMimeType(String mimeType, String codecPrefix, int trackType) {
            this.mimeType = mimeType;
            this.codecPrefix = codecPrefix;
            this.trackType = trackType;
        }
    }
}
