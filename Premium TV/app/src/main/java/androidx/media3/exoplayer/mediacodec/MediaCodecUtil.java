package androidx.media3.exoplayer.mediacodec;

import android.media.MediaCodecList;
import android.text.TextUtils;
import android.util.Pair;
import androidx.core.view.MotionEventCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.extractor.metadata.icy.IcyHeaders;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class MediaCodecUtil {
    private static final String CODEC_ID_AV01 = "av01";
    private static final String CODEC_ID_AVC1 = "avc1";
    private static final String CODEC_ID_AVC2 = "avc2";
    private static final String CODEC_ID_HEV1 = "hev1";
    private static final String CODEC_ID_HVC1 = "hvc1";
    private static final String CODEC_ID_MP4A = "mp4a";
    private static final String CODEC_ID_VP09 = "vp09";
    private static final String TAG = "MediaCodecUtil";
    private static final Pattern PROFILE_PATTERN = Pattern.compile("^\\D?(\\d+)$");
    private static final HashMap<CodecKey, List<MediaCodecInfo>> decoderInfosCache = new HashMap<>();
    private static int maxH264DecodableFrameSize = -1;

    private interface MediaCodecListCompat {
        int getCodecCount();

        android.media.MediaCodecInfo getCodecInfoAt(int i);

        boolean isFeatureRequired(String str, String str2, android.media.MediaCodecInfo.CodecCapabilities codecCapabilities);

        boolean isFeatureSupported(String str, String str2, android.media.MediaCodecInfo.CodecCapabilities codecCapabilities);

        boolean secureDecodersExplicit();
    }

    /* JADX INFO: Access modifiers changed from: private */
    interface ScoreProvider<T> {
        int getScore(T t);
    }

    public static class DecoderQueryException extends Exception {
        private DecoderQueryException(Throwable cause) {
            super("Failed to query underlying media codecs", cause);
        }
    }

    private MediaCodecUtil() {
    }

    public static void warmDecoderInfoCache(String mimeType, boolean secure, boolean tunneling) {
        try {
            getDecoderInfos(mimeType, secure, tunneling);
        } catch (DecoderQueryException e) {
            Log.e(TAG, "Codec warming failed", e);
        }
    }

    public static synchronized void clearDecoderInfoCache() {
        decoderInfosCache.clear();
    }

    public static MediaCodecInfo getDecryptOnlyDecoderInfo() throws DecoderQueryException {
        return getDecoderInfo(MimeTypes.AUDIO_RAW, false, false);
    }

    public static MediaCodecInfo getDecoderInfo(String mimeType, boolean secure, boolean tunneling) throws DecoderQueryException {
        List<MediaCodecInfo> decoderInfos = getDecoderInfos(mimeType, secure, tunneling);
        if (decoderInfos.isEmpty()) {
            return null;
        }
        return decoderInfos.get(0);
    }

    public static synchronized List<MediaCodecInfo> getDecoderInfos(String mimeType, boolean secure, boolean tunneling) throws DecoderQueryException {
        MediaCodecListCompat mediaCodecList;
        CodecKey key = new CodecKey(mimeType, secure, tunneling);
        List<MediaCodecInfo> cachedDecoderInfos = decoderInfosCache.get(key);
        if (cachedDecoderInfos != null) {
            return cachedDecoderInfos;
        }
        if (Util.SDK_INT >= 21) {
            mediaCodecList = new MediaCodecListCompatV21(secure, tunneling);
        } else {
            mediaCodecList = new MediaCodecListCompatV16();
        }
        ArrayList<MediaCodecInfo> decoderInfos = getDecoderInfosInternal(key, mediaCodecList);
        if (secure && decoderInfos.isEmpty() && 21 <= Util.SDK_INT && Util.SDK_INT <= 23) {
            MediaCodecListCompat mediaCodecList2 = new MediaCodecListCompatV16();
            decoderInfos = getDecoderInfosInternal(key, mediaCodecList2);
            if (!decoderInfos.isEmpty()) {
                Log.w(TAG, "MediaCodecList API didn't list secure decoder for: " + mimeType + ". Assuming: " + decoderInfos.get(0).name);
            }
        }
        applyWorkarounds(mimeType, decoderInfos);
        ImmutableList<MediaCodecInfo> immutableDecoderInfos = ImmutableList.copyOf((Collection) decoderInfos);
        decoderInfosCache.put(key, immutableDecoderInfos);
        return immutableDecoderInfos;
    }

    @RequiresNonNull({"#2.sampleMimeType"})
    public static List<MediaCodecInfo> getDecoderInfosSoftMatch(MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws DecoderQueryException {
        List<MediaCodecInfo> decoderInfos = mediaCodecSelector.getDecoderInfos(format.sampleMimeType, requiresSecureDecoder, requiresTunnelingDecoder);
        List<MediaCodecInfo> alternativeDecoderInfos = getAlternativeDecoderInfos(mediaCodecSelector, format, requiresSecureDecoder, requiresTunnelingDecoder);
        return ImmutableList.builder().addAll((Iterable) decoderInfos).addAll((Iterable) alternativeDecoderInfos).build();
    }

    public static List<MediaCodecInfo> getAlternativeDecoderInfos(MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws DecoderQueryException {
        String alternativeMimeType = getAlternativeCodecMimeType(format);
        if (alternativeMimeType == null) {
            return ImmutableList.of();
        }
        return mediaCodecSelector.getDecoderInfos(alternativeMimeType, requiresSecureDecoder, requiresTunnelingDecoder);
    }

    public static List<MediaCodecInfo> getDecoderInfosSortedByFormatSupport(List<MediaCodecInfo> decoderInfos, final Format format) {
        List<MediaCodecInfo> decoderInfos2 = new ArrayList<>(decoderInfos);
        sortByScore(decoderInfos2, new ScoreProvider() { // from class: androidx.media3.exoplayer.mediacodec.MediaCodecUtil$$ExternalSyntheticLambda3
            @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.ScoreProvider
            public final int getScore(Object obj) {
                return MediaCodecUtil.lambda$getDecoderInfosSortedByFormatSupport$0(format, (MediaCodecInfo) obj);
            }
        });
        return decoderInfos2;
    }

    static /* synthetic */ int lambda$getDecoderInfosSortedByFormatSupport$0(Format format, MediaCodecInfo mediaCodecInfo) {
        return mediaCodecInfo.isFormatFunctionallySupported(format) ? 1 : 0;
    }

    public static int maxH264DecodableFrameSize() throws DecoderQueryException {
        if (maxH264DecodableFrameSize == -1) {
            int result = 0;
            MediaCodecInfo decoderInfo = getDecoderInfo(MimeTypes.VIDEO_H264, false, false);
            if (decoderInfo != null) {
                for (android.media.MediaCodecInfo.CodecProfileLevel profileLevel : decoderInfo.getProfileLevels()) {
                    result = Math.max(avcLevelToMaxFrameSize(profileLevel.level), result);
                }
                result = Math.max(result, Util.SDK_INT >= 21 ? 345600 : 172800);
            }
            maxH264DecodableFrameSize = result;
        }
        return maxH264DecodableFrameSize;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:33:0x0071  */
    public static Pair<Integer, Integer> getCodecProfileAndLevel(Format format) {
        if (format.codecs == null) {
            return null;
        }
        String[] parts = format.codecs.split("\\.");
        if (MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType)) {
            return getDolbyVisionProfileAndLevel(format.codecs, parts);
        }
        byte b = 0;
        String str = parts[0];
        switch (str.hashCode()) {
            case 3004662:
                if (!str.equals(CODEC_ID_AV01)) {
                    b = -1;
                } else {
                    b = 5;
                }
                break;
            case 3006243:
                if (!str.equals(CODEC_ID_AVC1)) {
                    b = -1;
                }
                break;
            case 3006244:
                if (!str.equals(CODEC_ID_AVC2)) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            case 3199032:
                if (!str.equals(CODEC_ID_HEV1)) {
                    b = -1;
                } else {
                    b = 3;
                }
                break;
            case 3214780:
                if (!str.equals(CODEC_ID_HVC1)) {
                    b = -1;
                } else {
                    b = 4;
                }
                break;
            case 3356560:
                if (!str.equals(CODEC_ID_MP4A)) {
                    b = -1;
                } else {
                    b = 6;
                }
                break;
            case 3624515:
                if (!str.equals(CODEC_ID_VP09)) {
                    b = -1;
                } else {
                    b = 2;
                }
                break;
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
            case 1:
                return getAvcProfileAndLevel(format.codecs, parts);
            case 2:
                return getVp9ProfileAndLevel(format.codecs, parts);
            case 3:
            case 4:
                return getHevcProfileAndLevel(format.codecs, parts, format.colorInfo);
            case 5:
                return getAv1ProfileAndLevel(format.codecs, parts, format.colorInfo);
            case 6:
                return getAacCodecProfileAndLevel(format.codecs, parts);
            default:
                return null;
        }
    }

    public static String getAlternativeCodecMimeType(Format format) {
        Pair<Integer, Integer> codecProfileAndLevel;
        if (MimeTypes.AUDIO_E_AC3_JOC.equals(format.sampleMimeType)) {
            return MimeTypes.AUDIO_E_AC3;
        }
        if (MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType) && (codecProfileAndLevel = getCodecProfileAndLevel(format)) != null) {
            int profile = ((Integer) codecProfileAndLevel.first).intValue();
            if (profile == 16 || profile == 256) {
                return MimeTypes.VIDEO_H265;
            }
            if (profile == 512) {
                return MimeTypes.VIDEO_H264;
            }
            if (profile == 1024) {
                return MimeTypes.VIDEO_AV1;
            }
            return null;
        }
        return null;
    }

    /* JADX WARN: Code duplicated, block: B:42:0x00bb A[PHI: r20
  0x00bb: PHI (r20v1 'capabilities' android.media.MediaCodecInfo$CodecCapabilities) = 
  (r20v2 'capabilities' android.media.MediaCodecInfo$CodecCapabilities)
  (r20v4 'capabilities' android.media.MediaCodecInfo$CodecCapabilities)
 binds: [B:40:0x00b8, B:33:0x00a4] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Code duplicated, block: B:49:0x00e4  */
    /* JADX WARN: Code duplicated, block: B:67:0x0132 A[Catch: Exception -> 0x0185, TRY_ENTER, TryCatch #5 {Exception -> 0x0185, blocks: (B:3:0x000a, B:5:0x0021, B:70:0x0154, B:8:0x0032, B:11:0x0043, B:64:0x012a, B:67:0x0132, B:69:0x0138, B:71:0x015c, B:72:0x0183), top: B:87:0x000a }] */
    /* JADX WARN: Code duplicated, block: B:93:0x015c A[ADDED_TO_REGION, REMOVE, SYNTHETIC] */
    private static ArrayList<MediaCodecInfo> getDecoderInfosInternal(CodecKey key, MediaCodecListCompat mediaCodecList) throws DecoderQueryException {
        boolean secureDecodersExplicit;
        int i;
        String name;
        String codecMimeType;
        android.media.MediaCodecInfo.CodecCapabilities capabilities;
        CodecKey codecKey = key;
        try {
            ArrayList<MediaCodecInfo> decoderInfos = new ArrayList<>();
            String mimeType = codecKey.mimeType;
            int numberOfCodecs = mediaCodecList.getCodecCount();
            boolean secureDecodersExplicit2 = mediaCodecList.secureDecodersExplicit();
            int i2 = 0;
            while (i2 < numberOfCodecs) {
                android.media.MediaCodecInfo codecInfo = mediaCodecList.getCodecInfoAt(i2);
                if (isAlias(codecInfo)) {
                    secureDecodersExplicit = secureDecodersExplicit2;
                    i = i2;
                } else {
                    String name2 = codecInfo.getName();
                    if (isCodecUsableDecoder(codecInfo, name2, secureDecodersExplicit2, mimeType)) {
                        String codecMimeType2 = getCodecMimeType(codecInfo, name2, mimeType);
                        if (codecMimeType2 == null) {
                            secureDecodersExplicit = secureDecodersExplicit2;
                            i = i2;
                        } else {
                            try {
                                android.media.MediaCodecInfo.CodecCapabilities capabilities2 = codecInfo.getCapabilitiesForType(codecMimeType2);
                                boolean tunnelingSupported = mediaCodecList.isFeatureSupported("tunneled-playback", codecMimeType2, capabilities2);
                                boolean tunnelingRequired = mediaCodecList.isFeatureRequired("tunneled-playback", codecMimeType2, capabilities2);
                                if ((codecKey.tunneling || !tunnelingRequired) && (!codecKey.tunneling || tunnelingSupported)) {
                                    boolean secureSupported = mediaCodecList.isFeatureSupported("secure-playback", codecMimeType2, capabilities2);
                                    boolean secureRequired = mediaCodecList.isFeatureRequired("secure-playback", codecMimeType2, capabilities2);
                                    if ((codecKey.secure || !secureRequired) && (!codecKey.secure || secureSupported)) {
                                        boolean hardwareAccelerated = isHardwareAccelerated(codecInfo, mimeType);
                                        boolean softwareOnly = isSoftwareOnly(codecInfo, mimeType);
                                        boolean vendor = isVendor(codecInfo);
                                        if (secureDecodersExplicit2) {
                                            capabilities = capabilities2;
                                            try {
                                                if (codecKey.secure == secureSupported) {
                                                    secureDecodersExplicit = secureDecodersExplicit2;
                                                    i = i2;
                                                    codecMimeType = codecMimeType2;
                                                    try {
                                                        decoderInfos.add(MediaCodecInfo.newInstance(name2, mimeType, codecMimeType, capabilities, hardwareAccelerated, softwareOnly, vendor, false, false));
                                                    } catch (Exception e) {
                                                        e = e;
                                                        name = name2;
                                                        if (Util.SDK_INT <= 23) {
                                                        }
                                                        Log.e(TAG, "Failed to query codec " + name + " (" + codecMimeType + ")");
                                                        throw e;
                                                    }
                                                }
                                            } catch (Exception e2) {
                                                e = e2;
                                                secureDecodersExplicit = secureDecodersExplicit2;
                                                i = i2;
                                                name = name2;
                                                codecMimeType = codecMimeType2;
                                                if (Util.SDK_INT <= 23) {
                                                }
                                                Log.e(TAG, "Failed to query codec " + name + " (" + codecMimeType + ")");
                                                throw e;
                                            }
                                        } else {
                                            capabilities = capabilities2;
                                        }
                                        if (secureDecodersExplicit2) {
                                            secureDecodersExplicit = secureDecodersExplicit2;
                                            i = i2;
                                            codecMimeType = codecMimeType2;
                                            android.media.MediaCodecInfo.CodecCapabilities capabilities3 = capabilities;
                                            if (secureDecodersExplicit) {
                                            }
                                        } else {
                                            try {
                                                if (codecKey.secure) {
                                                    secureDecodersExplicit = secureDecodersExplicit2;
                                                    i = i2;
                                                    codecMimeType = codecMimeType2;
                                                    android.media.MediaCodecInfo.CodecCapabilities capabilities4 = capabilities;
                                                    if (secureDecodersExplicit && secureSupported) {
                                                        try {
                                                            name = name2;
                                                            try {
                                                                decoderInfos.add(MediaCodecInfo.newInstance(name2 + ".secure", mimeType, codecMimeType, capabilities4, hardwareAccelerated, softwareOnly, vendor, false, true));
                                                                return decoderInfos;
                                                            } catch (Exception e3) {
                                                                e = e3;
                                                                if (Util.SDK_INT <= 23 || decoderInfos.isEmpty()) {
                                                                    Log.e(TAG, "Failed to query codec " + name + " (" + codecMimeType + ")");
                                                                    throw e;
                                                                }
                                                                Log.e(TAG, "Skipping codec " + name + " (failed to query capabilities)");
                                                                i2 = i + 1;
                                                                codecKey = key;
                                                                secureDecodersExplicit2 = secureDecodersExplicit;
                                                            }
                                                        } catch (Exception e4) {
                                                            e = e4;
                                                            name = name2;
                                                        }
                                                    }
                                                } else {
                                                    secureDecodersExplicit = secureDecodersExplicit2;
                                                    i = i2;
                                                    codecMimeType = codecMimeType2;
                                                    decoderInfos.add(MediaCodecInfo.newInstance(name2, mimeType, codecMimeType, capabilities, hardwareAccelerated, softwareOnly, vendor, false, false));
                                                }
                                            } catch (Exception e5) {
                                                e = e5;
                                                secureDecodersExplicit = secureDecodersExplicit2;
                                                i = i2;
                                                codecMimeType = codecMimeType2;
                                                name = name2;
                                                if (Util.SDK_INT <= 23) {
                                                }
                                                Log.e(TAG, "Failed to query codec " + name + " (" + codecMimeType + ")");
                                                throw e;
                                            }
                                        }
                                    } else {
                                        secureDecodersExplicit = secureDecodersExplicit2;
                                        i = i2;
                                    }
                                } else {
                                    secureDecodersExplicit = secureDecodersExplicit2;
                                    i = i2;
                                }
                            } catch (Exception e6) {
                                e = e6;
                                secureDecodersExplicit = secureDecodersExplicit2;
                                i = i2;
                                name = name2;
                                codecMimeType = codecMimeType2;
                            }
                        }
                    } else {
                        secureDecodersExplicit = secureDecodersExplicit2;
                        i = i2;
                    }
                }
                i2 = i + 1;
                codecKey = key;
                secureDecodersExplicit2 = secureDecodersExplicit;
            }
            return decoderInfos;
        } catch (Exception e7) {
            throw new DecoderQueryException(e7);
        }
    }

    private static String getCodecMimeType(android.media.MediaCodecInfo info, String name, String mimeType) {
        String[] supportedTypes = info.getSupportedTypes();
        for (String supportedType : supportedTypes) {
            if (supportedType.equalsIgnoreCase(mimeType)) {
                return supportedType;
            }
        }
        if (mimeType.equals(MimeTypes.VIDEO_DOLBY_VISION)) {
            if ("OMX.MS.HEVCDV.Decoder".equals(name)) {
                return "video/hevcdv";
            }
            if ("OMX.RTK.video.decoder".equals(name) || "OMX.realtek.video.decoder.tunneled".equals(name)) {
                return "video/dv_hevc";
            }
            return null;
        }
        if (mimeType.equals(MimeTypes.AUDIO_ALAC) && "OMX.lge.alac.decoder".equals(name)) {
            return "audio/x-lg-alac";
        }
        if (mimeType.equals(MimeTypes.AUDIO_FLAC) && "OMX.lge.flac.decoder".equals(name)) {
            return "audio/x-lg-flac";
        }
        if (mimeType.equals(MimeTypes.AUDIO_AC3) && "OMX.lge.ac3.decoder".equals(name)) {
            return "audio/lg-ac3";
        }
        return null;
    }

    private static boolean isCodecUsableDecoder(android.media.MediaCodecInfo info, String name, boolean secureDecodersExplicit, String mimeType) {
        if (info.isEncoder() || (!secureDecodersExplicit && name.endsWith(".secure"))) {
            return false;
        }
        if (Util.SDK_INT < 21 && ("CIPAACDecoder".equals(name) || "CIPMP3Decoder".equals(name) || "CIPVorbisDecoder".equals(name) || "CIPAMRNBDecoder".equals(name) || "AACDecoder".equals(name) || "MP3Decoder".equals(name))) {
            return false;
        }
        if (Util.SDK_INT < 24 && (("OMX.SEC.aac.dec".equals(name) || "OMX.Exynos.AAC.Decoder".equals(name)) && "samsung".equals(Util.MANUFACTURER) && (Util.DEVICE.startsWith("zeroflte") || Util.DEVICE.startsWith("zerolte") || Util.DEVICE.startsWith("zenlte") || "SC-05G".equals(Util.DEVICE) || "marinelteatt".equals(Util.DEVICE) || "404SC".equals(Util.DEVICE) || "SC-04G".equals(Util.DEVICE) || "SCV31".equals(Util.DEVICE)))) {
            return false;
        }
        if (Util.SDK_INT == 19 && "OMX.SEC.vp8.dec".equals(name) && "samsung".equals(Util.MANUFACTURER) && (Util.DEVICE.startsWith("d2") || Util.DEVICE.startsWith("serrano") || Util.DEVICE.startsWith("jflte") || Util.DEVICE.startsWith("santos") || Util.DEVICE.startsWith("t0"))) {
            return false;
        }
        if (Util.SDK_INT == 19 && Util.DEVICE.startsWith("jflte") && "OMX.qcom.video.decoder.vp8".equals(name)) {
            return false;
        }
        return (Util.SDK_INT <= 23 && MimeTypes.AUDIO_E_AC3_JOC.equals(mimeType) && "OMX.MTK.AUDIO.DECODER.DSPAC3".equals(name)) ? false : true;
    }

    private static void applyWorkarounds(String mimeType, List<MediaCodecInfo> decoderInfos) {
        if (MimeTypes.AUDIO_RAW.equals(mimeType)) {
            if (Util.SDK_INT < 26 && Util.DEVICE.equals("R9") && decoderInfos.size() == 1 && decoderInfos.get(0).name.equals("OMX.MTK.AUDIO.DECODER.RAW")) {
                decoderInfos.add(MediaCodecInfo.newInstance("OMX.google.raw.decoder", MimeTypes.AUDIO_RAW, MimeTypes.AUDIO_RAW, null, false, true, false, false, false));
            }
            sortByScore(decoderInfos, new ScoreProvider() { // from class: androidx.media3.exoplayer.mediacodec.MediaCodecUtil$$ExternalSyntheticLambda1
                @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.ScoreProvider
                public final int getScore(Object obj) {
                    return MediaCodecUtil.lambda$applyWorkarounds$1((MediaCodecInfo) obj);
                }
            });
        }
        if (Util.SDK_INT < 21 && decoderInfos.size() > 1) {
            String firstCodecName = decoderInfos.get(0).name;
            if ("OMX.SEC.mp3.dec".equals(firstCodecName) || "OMX.SEC.MP3.Decoder".equals(firstCodecName) || "OMX.brcm.audio.mp3.decoder".equals(firstCodecName)) {
                sortByScore(decoderInfos, new ScoreProvider() { // from class: androidx.media3.exoplayer.mediacodec.MediaCodecUtil$$ExternalSyntheticLambda2
                    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.ScoreProvider
                    public final int getScore(Object obj) {
                        return MediaCodecUtil.lambda$applyWorkarounds$2((MediaCodecInfo) obj);
                    }
                });
            }
        }
        if (Util.SDK_INT < 32 && decoderInfos.size() > 1 && "OMX.qti.audio.decoder.flac".equals(decoderInfos.get(0).name)) {
            decoderInfos.add(decoderInfos.remove(0));
        }
    }

    static /* synthetic */ int lambda$applyWorkarounds$1(MediaCodecInfo decoderInfo) {
        String name = decoderInfo.name;
        if (name.startsWith("OMX.google") || name.startsWith("c2.android")) {
            return 1;
        }
        if (Util.SDK_INT < 26 && name.equals("OMX.MTK.AUDIO.DECODER.RAW")) {
            return -1;
        }
        return 0;
    }

    static /* synthetic */ int lambda$applyWorkarounds$2(MediaCodecInfo mediaCodecInfo) {
        return mediaCodecInfo.name.startsWith("OMX.google") ? 1 : 0;
    }

    private static boolean isAlias(android.media.MediaCodecInfo info) {
        return Util.SDK_INT >= 29 && isAliasV29(info);
    }

    private static boolean isAliasV29(android.media.MediaCodecInfo info) {
        return info.isAlias();
    }

    private static boolean isHardwareAccelerated(android.media.MediaCodecInfo codecInfo, String mimeType) {
        if (Util.SDK_INT >= 29) {
            return isHardwareAcceleratedV29(codecInfo);
        }
        return !isSoftwareOnly(codecInfo, mimeType);
    }

    private static boolean isHardwareAcceleratedV29(android.media.MediaCodecInfo codecInfo) {
        return codecInfo.isHardwareAccelerated();
    }

    private static boolean isSoftwareOnly(android.media.MediaCodecInfo codecInfo, String mimeType) {
        if (Util.SDK_INT >= 29) {
            return isSoftwareOnlyV29(codecInfo);
        }
        if (MimeTypes.isAudio(mimeType)) {
            return true;
        }
        String codecName = Ascii.toLowerCase(codecInfo.getName());
        if (codecName.startsWith("arc.")) {
            return false;
        }
        if (codecName.startsWith("omx.google.") || codecName.startsWith("omx.ffmpeg.")) {
            return true;
        }
        if ((codecName.startsWith("omx.sec.") && codecName.contains(".sw.")) || codecName.equals("omx.qcom.video.decoder.hevcswvdec") || codecName.startsWith("c2.android.") || codecName.startsWith("c2.google.")) {
            return true;
        }
        return (codecName.startsWith("omx.") || codecName.startsWith("c2.")) ? false : true;
    }

    private static boolean isSoftwareOnlyV29(android.media.MediaCodecInfo codecInfo) {
        return codecInfo.isSoftwareOnly();
    }

    private static boolean isVendor(android.media.MediaCodecInfo codecInfo) {
        if (Util.SDK_INT >= 29) {
            return isVendorV29(codecInfo);
        }
        String codecName = Ascii.toLowerCase(codecInfo.getName());
        return (codecName.startsWith("omx.google.") || codecName.startsWith("c2.android.") || codecName.startsWith("c2.google.")) ? false : true;
    }

    private static boolean isVendorV29(android.media.MediaCodecInfo codecInfo) {
        return codecInfo.isVendor();
    }

    private static Pair<Integer, Integer> getDolbyVisionProfileAndLevel(String codec, String[] parts) {
        if (parts.length < 3) {
            Log.w(TAG, "Ignoring malformed Dolby Vision codec string: " + codec);
            return null;
        }
        Matcher matcher = PROFILE_PATTERN.matcher(parts[1]);
        if (!matcher.matches()) {
            Log.w(TAG, "Ignoring malformed Dolby Vision codec string: " + codec);
            return null;
        }
        String profileString = matcher.group(1);
        Integer profile = dolbyVisionStringToProfile(profileString);
        if (profile == null) {
            Log.w(TAG, "Unknown Dolby Vision profile string: " + profileString);
            return null;
        }
        String levelString = parts[2];
        Integer level = dolbyVisionStringToLevel(levelString);
        if (level == null) {
            Log.w(TAG, "Unknown Dolby Vision level string: " + levelString);
            return null;
        }
        return new Pair<>(profile, level);
    }

    private static Pair<Integer, Integer> getHevcProfileAndLevel(String codec, String[] parts, ColorInfo colorInfo) {
        int profile;
        if (parts.length < 4) {
            Log.w(TAG, "Ignoring malformed HEVC codec string: " + codec);
            return null;
        }
        Matcher matcher = PROFILE_PATTERN.matcher(parts[1]);
        if (!matcher.matches()) {
            Log.w(TAG, "Ignoring malformed HEVC codec string: " + codec);
            return null;
        }
        String profileString = matcher.group(1);
        if (IcyHeaders.REQUEST_HEADER_ENABLE_METADATA_VALUE.equals(profileString)) {
            profile = 1;
        } else if (ExifInterface.GPS_MEASUREMENT_2D.equals(profileString)) {
            if (colorInfo != null && colorInfo.colorTransfer == 6) {
                profile = 4096;
            } else {
                profile = 2;
            }
        } else {
            Log.w(TAG, "Unknown HEVC profile string: " + profileString);
            return null;
        }
        String levelString = parts[3];
        Integer level = hevcCodecStringToProfileLevel(levelString);
        if (level == null) {
            Log.w(TAG, "Unknown HEVC level string: " + levelString);
            return null;
        }
        return new Pair<>(Integer.valueOf(profile), level);
    }

    private static Pair<Integer, Integer> getAvcProfileAndLevel(String codec, String[] parts) {
        int profileInteger;
        int profileInteger2;
        if (parts.length < 2) {
            Log.w(TAG, "Ignoring malformed AVC codec string: " + codec);
            return null;
        }
        try {
            if (parts[1].length() == 6) {
                profileInteger = Integer.parseInt(parts[1].substring(0, 2), 16);
                profileInteger2 = Integer.parseInt(parts[1].substring(4), 16);
            } else if (parts.length >= 3) {
                int profileInteger3 = Integer.parseInt(parts[1]);
                profileInteger = profileInteger3;
                profileInteger2 = Integer.parseInt(parts[2]);
            } else {
                Log.w(TAG, "Ignoring malformed AVC codec string: " + codec);
                return null;
            }
            int profile = avcProfileNumberToConst(profileInteger);
            if (profile == -1) {
                Log.w(TAG, "Unknown AVC profile: " + profileInteger);
                return null;
            }
            int level = avcLevelNumberToConst(profileInteger2);
            if (level == -1) {
                Log.w(TAG, "Unknown AVC level: " + profileInteger2);
                return null;
            }
            return new Pair<>(Integer.valueOf(profile), Integer.valueOf(level));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ignoring malformed AVC codec string: " + codec);
            return null;
        }
    }

    private static Pair<Integer, Integer> getVp9ProfileAndLevel(String codec, String[] parts) {
        if (parts.length < 3) {
            Log.w(TAG, "Ignoring malformed VP9 codec string: " + codec);
            return null;
        }
        try {
            int profileInteger = Integer.parseInt(parts[1]);
            int levelInteger = Integer.parseInt(parts[2]);
            int profile = vp9ProfileNumberToConst(profileInteger);
            if (profile == -1) {
                Log.w(TAG, "Unknown VP9 profile: " + profileInteger);
                return null;
            }
            int level = vp9LevelNumberToConst(levelInteger);
            if (level == -1) {
                Log.w(TAG, "Unknown VP9 level: " + levelInteger);
                return null;
            }
            return new Pair<>(Integer.valueOf(profile), Integer.valueOf(level));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ignoring malformed VP9 codec string: " + codec);
            return null;
        }
    }

    private static Pair<Integer, Integer> getAv1ProfileAndLevel(String codec, String[] parts, ColorInfo colorInfo) {
        int profile;
        if (parts.length < 4) {
            Log.w(TAG, "Ignoring malformed AV1 codec string: " + codec);
            return null;
        }
        try {
            int profileInteger = Integer.parseInt(parts[1]);
            int levelInteger = Integer.parseInt(parts[2].substring(0, 2));
            int bitDepthInteger = Integer.parseInt(parts[3]);
            if (profileInteger != 0) {
                Log.w(TAG, "Unknown AV1 profile: " + profileInteger);
                return null;
            }
            if (bitDepthInteger != 8 && bitDepthInteger != 10) {
                Log.w(TAG, "Unknown AV1 bit depth: " + bitDepthInteger);
                return null;
            }
            if (bitDepthInteger == 8) {
                profile = 1;
            } else if (colorInfo != null && (colorInfo.hdrStaticInfo != null || colorInfo.colorTransfer == 7 || colorInfo.colorTransfer == 6)) {
                profile = 4096;
            } else {
                profile = 2;
            }
            int level = av1LevelNumberToConst(levelInteger);
            if (level == -1) {
                Log.w(TAG, "Unknown AV1 level: " + levelInteger);
                return null;
            }
            return new Pair<>(Integer.valueOf(profile), Integer.valueOf(level));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ignoring malformed AV1 codec string: " + codec);
            return null;
        }
    }

    private static int avcLevelToMaxFrameSize(int avcLevel) {
        switch (avcLevel) {
            case 1:
            case 2:
                return 25344;
            case 8:
            case 16:
            case 32:
                return 101376;
            case 64:
                return 202752;
            case 128:
            case 256:
                return 414720;
            case 512:
                return 921600;
            case 1024:
                return 1310720;
            case 2048:
            case 4096:
                return 2097152;
            case 8192:
                return 2228224;
            case 16384:
                return 5652480;
            case 32768:
            case 65536:
                return 9437184;
            case 131072:
            case 262144:
            case 524288:
                return 35651584;
            default:
                return -1;
        }
    }

    private static Pair<Integer, Integer> getAacCodecProfileAndLevel(String codec, String[] parts) {
        if (parts.length != 3) {
            Log.w(TAG, "Ignoring malformed MP4A codec string: " + codec);
            return null;
        }
        try {
            int objectTypeIndication = Integer.parseInt(parts[1], 16);
            String mimeType = MimeTypes.getMimeTypeFromMp4ObjectType(objectTypeIndication);
            if (MimeTypes.AUDIO_AAC.equals(mimeType)) {
                int audioObjectTypeIndication = Integer.parseInt(parts[2]);
                int profile = mp4aAudioObjectTypeToProfile(audioObjectTypeIndication);
                if (profile != -1) {
                    return new Pair<>(Integer.valueOf(profile), 0);
                }
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ignoring malformed MP4A codec string: " + codec);
        }
        return null;
    }

    static /* synthetic */ int lambda$sortByScore$3(ScoreProvider scoreProvider, Object a, Object b) {
        return scoreProvider.getScore(b) - scoreProvider.getScore(a);
    }

    private static <T> void sortByScore(List<T> list, final ScoreProvider<T> scoreProvider) {
        Collections.sort(list, new Comparator() { // from class: androidx.media3.exoplayer.mediacodec.MediaCodecUtil$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return MediaCodecUtil.lambda$sortByScore$3(scoreProvider, obj, obj2);
            }
        });
    }

    private static final class MediaCodecListCompatV21 implements MediaCodecListCompat {
        private final int codecKind;
        private android.media.MediaCodecInfo[] mediaCodecInfos;

        public MediaCodecListCompatV21(boolean includeSecure, boolean includeTunneling) {
            int i;
            if (includeSecure || includeTunneling) {
                i = 1;
            } else {
                i = 0;
            }
            this.codecKind = i;
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public int getCodecCount() {
            ensureMediaCodecInfosInitialized();
            return this.mediaCodecInfos.length;
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public android.media.MediaCodecInfo getCodecInfoAt(int index) {
            ensureMediaCodecInfosInitialized();
            return this.mediaCodecInfos[index];
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public boolean secureDecodersExplicit() {
            return true;
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public boolean isFeatureSupported(String feature, String mimeType, android.media.MediaCodecInfo.CodecCapabilities capabilities) {
            return capabilities.isFeatureSupported(feature);
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public boolean isFeatureRequired(String feature, String mimeType, android.media.MediaCodecInfo.CodecCapabilities capabilities) {
            return capabilities.isFeatureRequired(feature);
        }

        @EnsuresNonNull({"mediaCodecInfos"})
        private void ensureMediaCodecInfosInitialized() {
            if (this.mediaCodecInfos == null) {
                this.mediaCodecInfos = new MediaCodecList(this.codecKind).getCodecInfos();
            }
        }
    }

    private static final class MediaCodecListCompatV16 implements MediaCodecListCompat {
        private MediaCodecListCompatV16() {
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public int getCodecCount() {
            return MediaCodecList.getCodecCount();
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public android.media.MediaCodecInfo getCodecInfoAt(int index) {
            return MediaCodecList.getCodecInfoAt(index);
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public boolean secureDecodersExplicit() {
            return false;
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public boolean isFeatureSupported(String feature, String mimeType, android.media.MediaCodecInfo.CodecCapabilities capabilities) {
            return "secure-playback".equals(feature) && MimeTypes.VIDEO_H264.equals(mimeType);
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecUtil.MediaCodecListCompat
        public boolean isFeatureRequired(String feature, String mimeType, android.media.MediaCodecInfo.CodecCapabilities capabilities) {
            return false;
        }
    }

    private static final class CodecKey {
        public final String mimeType;
        public final boolean secure;
        public final boolean tunneling;

        public CodecKey(String mimeType, boolean secure, boolean tunneling) {
            this.mimeType = mimeType;
            this.secure = secure;
            this.tunneling = tunneling;
        }

        public int hashCode() {
            int result = (1 * 31) + this.mimeType.hashCode();
            return (((result * 31) + (this.secure ? 1231 : 1237)) * 31) + (this.tunneling ? 1231 : 1237);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != CodecKey.class) {
                return false;
            }
            CodecKey other = (CodecKey) obj;
            if (TextUtils.equals(this.mimeType, other.mimeType) && this.secure == other.secure && this.tunneling == other.tunneling) {
                return true;
            }
            return false;
        }
    }

    private static int avcProfileNumberToConst(int profileNumber) {
        switch (profileNumber) {
            case 66:
                return 1;
            case 77:
                return 2;
            case 88:
                return 4;
            case 100:
                return 8;
            case 110:
                return 16;
            case 122:
                return 32;
            case 244:
                return 64;
            default:
                return -1;
        }
    }

    private static int avcLevelNumberToConst(int levelNumber) {
        switch (levelNumber) {
            case 10:
                return 1;
            case 11:
                return 4;
            case 12:
                return 8;
            case 13:
                return 16;
            case 20:
                return 32;
            case 21:
                return 64;
            case 22:
                return 128;
            case 30:
                return 256;
            case 31:
                return 512;
            case 32:
                return 1024;
            case MotionEventCompat.AXIS_GENERIC_9 /* 40 */:
                return 2048;
            case MotionEventCompat.AXIS_GENERIC_10 /* 41 */:
                return 4096;
            case 42:
                return 8192;
            case DefaultRenderersFactory.MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY /* 50 */:
                return 16384;
            case 51:
                return 32768;
            case 52:
                return 65536;
            default:
                return -1;
        }
    }

    private static int vp9ProfileNumberToConst(int profileNumber) {
        switch (profileNumber) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 8;
            default:
                return -1;
        }
    }

    private static int vp9LevelNumberToConst(int levelNumber) {
        switch (levelNumber) {
            case 10:
                return 1;
            case 11:
                return 2;
            case 20:
                return 4;
            case 21:
                return 8;
            case 30:
                return 16;
            case 31:
                return 32;
            case MotionEventCompat.AXIS_GENERIC_9 /* 40 */:
                return 64;
            case MotionEventCompat.AXIS_GENERIC_10 /* 41 */:
                return 128;
            case DefaultRenderersFactory.MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY /* 50 */:
                return 256;
            case 51:
                return 512;
            case 60:
                return 2048;
            case 61:
                return 4096;
            case 62:
                return 8192;
            default:
                return -1;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:86:0x0139  */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    private static Integer hevcCodecStringToProfileLevel(String codecString) {
        byte b;
        if (codecString == null) {
            return null;
        }
        switch (codecString.hashCode()) {
            case 70821:
                if (!codecString.equals("H30")) {
                    b = -1;
                } else {
                    b = Ascii.CR;
                }
                break;
            case 70914:
                if (!codecString.equals("H60")) {
                    b = -1;
                } else {
                    b = Ascii.SO;
                }
                break;
            case 70917:
                if (!codecString.equals("H63")) {
                    b = -1;
                } else {
                    b = Ascii.SI;
                }
                break;
            case 71007:
                if (!codecString.equals("H90")) {
                    b = -1;
                } else {
                    b = 16;
                }
                break;
            case 71010:
                if (!codecString.equals("H93")) {
                    b = -1;
                } else {
                    b = 17;
                }
                break;
            case 74665:
                if (!codecString.equals("L30")) {
                    b = -1;
                } else {
                    b = 0;
                }
                break;
            case 74758:
                if (!codecString.equals("L60")) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            case 74761:
                if (!codecString.equals("L63")) {
                    b = -1;
                } else {
                    b = 2;
                }
                break;
            case 74851:
                if (!codecString.equals("L90")) {
                    b = -1;
                } else {
                    b = 3;
                }
                break;
            case 74854:
                if (!codecString.equals("L93")) {
                    b = -1;
                } else {
                    b = 4;
                }
                break;
            case 2193639:
                if (!codecString.equals("H120")) {
                    b = -1;
                } else {
                    b = Ascii.DC2;
                }
                break;
            case 2193642:
                if (!codecString.equals("H123")) {
                    b = -1;
                } else {
                    b = 19;
                }
                break;
            case 2193732:
                if (!codecString.equals("H150")) {
                    b = -1;
                } else {
                    b = Ascii.DC4;
                }
                break;
            case 2193735:
                if (!codecString.equals("H153")) {
                    b = -1;
                } else {
                    b = Ascii.NAK;
                }
                break;
            case 2193738:
                if (!codecString.equals("H156")) {
                    b = -1;
                } else {
                    b = Ascii.SYN;
                }
                break;
            case 2193825:
                if (!codecString.equals("H180")) {
                    b = -1;
                } else {
                    b = Ascii.ETB;
                }
                break;
            case 2193828:
                if (!codecString.equals("H183")) {
                    b = -1;
                } else {
                    b = Ascii.CAN;
                }
                break;
            case 2193831:
                if (!codecString.equals("H186")) {
                    b = -1;
                } else {
                    b = Ascii.EM;
                }
                break;
            case 2312803:
                if (!codecString.equals("L120")) {
                    b = -1;
                } else {
                    b = 5;
                }
                break;
            case 2312806:
                if (!codecString.equals("L123")) {
                    b = -1;
                } else {
                    b = 6;
                }
                break;
            case 2312896:
                if (!codecString.equals("L150")) {
                    b = -1;
                } else {
                    b = 7;
                }
                break;
            case 2312899:
                if (!codecString.equals("L153")) {
                    b = -1;
                } else {
                    b = 8;
                }
                break;
            case 2312902:
                if (!codecString.equals("L156")) {
                    b = -1;
                } else {
                    b = 9;
                }
                break;
            case 2312989:
                if (!codecString.equals("L180")) {
                    b = -1;
                } else {
                    b = 10;
                }
                break;
            case 2312992:
                if (!codecString.equals("L183")) {
                    b = -1;
                } else {
                    b = Ascii.VT;
                }
                break;
            case 2312995:
                if (!codecString.equals("L186")) {
                    b = -1;
                } else {
                    b = Ascii.FF;
                }
                break;
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
                return 1;
            case 1:
                return 4;
            case 2:
                return 16;
            case 3:
                return 64;
            case 4:
                return 256;
            case 5:
                return 1024;
            case 6:
                return 4096;
            case 7:
                return 16384;
            case 8:
                return 65536;
            case 9:
                return 262144;
            case 10:
                return 1048576;
            case 11:
                return 4194304;
            case 12:
                return 16777216;
            case 13:
                return 2;
            case 14:
                return 8;
            case 15:
                return 32;
            case 16:
                return 128;
            case 17:
                return 512;
            case 18:
                return 2048;
            case 19:
                return 8192;
            case 20:
                return 32768;
            case 21:
                return 131072;
            case 22:
                return 524288;
            case 23:
                return 2097152;
            case 24:
                return 8388608;
            case 25:
                return 33554432;
            default:
                return null;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:41:0x0084  */
    private static Integer dolbyVisionStringToProfile(String profileString) {
        if (profileString == null) {
            return null;
        }
        switch (profileString) {
            case "00":
                return 1;
            case "01":
                return 2;
            case "02":
                return 4;
            case "03":
                return 8;
            case "04":
                return 16;
            case "05":
                return 32;
            case "06":
                return 64;
            case "07":
                return 128;
            case "08":
                return 256;
            case "09":
                return 512;
            case "10":
                return 1024;
            default:
                return null;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:47:0x009c  */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    private static Integer dolbyVisionStringToLevel(String levelString) {
        byte b;
        if (levelString == null) {
            return null;
        }
        switch (levelString.hashCode()) {
            case 1537:
                if (!levelString.equals("01")) {
                    b = -1;
                } else {
                    b = 0;
                }
                break;
            case 1538:
                if (!levelString.equals("02")) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            case 1539:
                if (!levelString.equals("03")) {
                    b = -1;
                } else {
                    b = 2;
                }
                break;
            case 1540:
                if (!levelString.equals("04")) {
                    b = -1;
                } else {
                    b = 3;
                }
                break;
            case 1541:
                if (!levelString.equals("05")) {
                    b = -1;
                } else {
                    b = 4;
                }
                break;
            case 1542:
                if (!levelString.equals("06")) {
                    b = -1;
                } else {
                    b = 5;
                }
                break;
            case 1543:
                if (!levelString.equals("07")) {
                    b = -1;
                } else {
                    b = 6;
                }
                break;
            case 1544:
                if (!levelString.equals("08")) {
                    b = -1;
                } else {
                    b = 7;
                }
                break;
            case 1545:
                if (!levelString.equals("09")) {
                    b = -1;
                } else {
                    b = 8;
                }
                break;
            case 1567:
                if (!levelString.equals("10")) {
                    b = -1;
                } else {
                    b = 9;
                }
                break;
            case 1568:
                if (!levelString.equals("11")) {
                    b = -1;
                } else {
                    b = 10;
                }
                break;
            case 1569:
                if (!levelString.equals("12")) {
                    b = -1;
                } else {
                    b = Ascii.VT;
                }
                break;
            case 1570:
                if (!levelString.equals("13")) {
                    b = -1;
                } else {
                    b = Ascii.FF;
                }
                break;
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 8;
            case 4:
                return 16;
            case 5:
                return 32;
            case 6:
                return 64;
            case 7:
                return 128;
            case 8:
                return 256;
            case 9:
                return 512;
            case 10:
                return 1024;
            case 11:
                return 2048;
            case 12:
                return 4096;
            default:
                return null;
        }
    }

    private static int av1LevelNumberToConst(int levelNumber) {
        switch (levelNumber) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 8;
            case 4:
                return 16;
            case 5:
                return 32;
            case 6:
                return 64;
            case 7:
                return 128;
            case 8:
                return 256;
            case 9:
                return 512;
            case 10:
                return 1024;
            case 11:
                return 2048;
            case 12:
                return 4096;
            case 13:
                return 8192;
            case 14:
                return 16384;
            case 15:
                return 32768;
            case 16:
                return 65536;
            case 17:
                return 131072;
            case 18:
                return 262144;
            case 19:
                return 524288;
            case 20:
                return 1048576;
            case 21:
                return 2097152;
            case 22:
                return 4194304;
            case 23:
                return 8388608;
            default:
                return -1;
        }
    }

    private static int mp4aAudioObjectTypeToProfile(int profileNumber) {
        switch (profileNumber) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 17:
                return 17;
            case 20:
                return 20;
            case 23:
                return 23;
            case 29:
                return 29;
            case MotionEventCompat.AXIS_GENERIC_8 /* 39 */:
                return 39;
            case 42:
                return 42;
            default:
                return -1;
        }
    }
}
