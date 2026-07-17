package androidx.media3.exoplayer.mediacodec;

import android.graphics.Point;
import android.util.Pair;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.DecoderReuseEvaluation;

/* JADX INFO: loaded from: classes.dex */
public final class MediaCodecInfo {
    public static final int MAX_SUPPORTED_INSTANCES_UNKNOWN = -1;
    public static final String TAG = "MediaCodecInfo";
    public final boolean adaptive;
    public final android.media.MediaCodecInfo.CodecCapabilities capabilities;
    public final String codecMimeType;
    public final boolean hardwareAccelerated;
    private final boolean isVideo;
    public final String mimeType;
    public final String name;
    public final boolean secure;
    public final boolean softwareOnly;
    public final boolean tunneling;
    public final boolean vendor;

    public static MediaCodecInfo newInstance(String name, String mimeType, String codecMimeType, android.media.MediaCodecInfo.CodecCapabilities capabilities, boolean hardwareAccelerated, boolean softwareOnly, boolean vendor, boolean forceDisableAdaptive, boolean forceSecure) {
        return new MediaCodecInfo(name, mimeType, codecMimeType, capabilities, hardwareAccelerated, softwareOnly, vendor, (forceDisableAdaptive || capabilities == null || !isAdaptive(capabilities) || needsDisableAdaptationWorkaround(name)) ? false : true, capabilities != null && isTunneling(capabilities), forceSecure || (capabilities != null && isSecure(capabilities)));
    }

    MediaCodecInfo(String name, String mimeType, String codecMimeType, android.media.MediaCodecInfo.CodecCapabilities capabilities, boolean hardwareAccelerated, boolean softwareOnly, boolean vendor, boolean adaptive, boolean tunneling, boolean secure) {
        this.name = (String) Assertions.checkNotNull(name);
        this.mimeType = mimeType;
        this.codecMimeType = codecMimeType;
        this.capabilities = capabilities;
        this.hardwareAccelerated = hardwareAccelerated;
        this.softwareOnly = softwareOnly;
        this.vendor = vendor;
        this.adaptive = adaptive;
        this.tunneling = tunneling;
        this.secure = secure;
        this.isVideo = MimeTypes.isVideo(mimeType);
    }

    public String toString() {
        return this.name;
    }

    public android.media.MediaCodecInfo.CodecProfileLevel[] getProfileLevels() {
        if (this.capabilities == null || this.capabilities.profileLevels == null) {
            return new android.media.MediaCodecInfo.CodecProfileLevel[0];
        }
        return this.capabilities.profileLevels;
    }

    public int getMaxSupportedInstances() {
        if (Util.SDK_INT < 23 || this.capabilities == null) {
            return -1;
        }
        return getMaxSupportedInstancesV23(this.capabilities);
    }

    public boolean isFormatSupported(Format format) throws MediaCodecUtil.DecoderQueryException {
        if (!isSampleMimeTypeSupported(format) || !isCodecProfileAndLevelSupported(format, true)) {
            return false;
        }
        if (this.isVideo) {
            if (format.width <= 0 || format.height <= 0) {
                return true;
            }
            if (Util.SDK_INT >= 21) {
                return isVideoSizeAndRateSupportedV21(format.width, format.height, format.frameRate);
            }
            boolean isFormatSupported = format.width * format.height <= MediaCodecUtil.maxH264DecodableFrameSize();
            if (!isFormatSupported) {
                logNoSupport("legacyFrameSize, " + format.width + "x" + format.height);
            }
            return isFormatSupported;
        }
        if (Util.SDK_INT >= 21) {
            if (format.sampleRate != -1 && !isAudioSampleRateSupportedV21(format.sampleRate)) {
                return false;
            }
            if (format.channelCount != -1 && !isAudioChannelCountSupportedV21(format.channelCount)) {
                return false;
            }
        }
        return true;
    }

    public boolean isFormatFunctionallySupported(Format format) {
        return isSampleMimeTypeSupported(format) && isCodecProfileAndLevelSupported(format, false);
    }

    private boolean isSampleMimeTypeSupported(Format format) {
        return this.mimeType.equals(format.sampleMimeType) || this.mimeType.equals(MediaCodecUtil.getAlternativeCodecMimeType(format));
    }

    private boolean isCodecProfileAndLevelSupported(Format format, boolean checkPerformanceCapabilities) {
        Pair<Integer, Integer> codecProfileAndLevel = MediaCodecUtil.getCodecProfileAndLevel(format);
        if (codecProfileAndLevel == null) {
            return true;
        }
        int profile = ((Integer) codecProfileAndLevel.first).intValue();
        int level = ((Integer) codecProfileAndLevel.second).intValue();
        if (MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType)) {
            if (MimeTypes.VIDEO_H264.equals(this.mimeType)) {
                profile = 8;
                level = 0;
            } else if (MimeTypes.VIDEO_H265.equals(this.mimeType)) {
                profile = 2;
                level = 0;
            }
        }
        if (!this.isVideo && profile != 42) {
            return true;
        }
        android.media.MediaCodecInfo.CodecProfileLevel[] profileLevels = getProfileLevels();
        if (Util.SDK_INT <= 23 && MimeTypes.VIDEO_VP9.equals(this.mimeType) && profileLevels.length == 0) {
            profileLevels = estimateLegacyVp9ProfileLevels(this.capabilities);
        }
        for (android.media.MediaCodecInfo.CodecProfileLevel profileLevel : profileLevels) {
            if (profileLevel.profile == profile && ((profileLevel.level >= level || !checkPerformanceCapabilities) && !needsProfileExcludedWorkaround(this.mimeType, profile))) {
                return true;
            }
        }
        logNoSupport("codec.profileLevel, " + format.codecs + ", " + this.codecMimeType);
        return false;
    }

    public boolean isHdr10PlusOutOfBandMetadataSupported() {
        if (Util.SDK_INT >= 29 && MimeTypes.VIDEO_VP9.equals(this.mimeType)) {
            for (android.media.MediaCodecInfo.CodecProfileLevel capabilities : getProfileLevels()) {
                if (capabilities.profile == 16384) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSeamlessAdaptationSupported(Format format) {
        if (this.isVideo) {
            return this.adaptive;
        }
        Pair<Integer, Integer> profileLevel = MediaCodecUtil.getCodecProfileAndLevel(format);
        return profileLevel != null && ((Integer) profileLevel.first).intValue() == 42;
    }

    public DecoderReuseEvaluation canReuseCodec(Format oldFormat, Format newFormat) {
        Format oldFormat2;
        Format newFormat2;
        int discardReasons;
        int i;
        int discardReasons2 = 0;
        if (!Util.areEqual(oldFormat.sampleMimeType, newFormat.sampleMimeType)) {
            discardReasons2 = 0 | 8;
        }
        if (this.isVideo) {
            if (oldFormat.rotationDegrees != newFormat.rotationDegrees) {
                discardReasons2 |= 1024;
            }
            if (!this.adaptive && (oldFormat.width != newFormat.width || oldFormat.height != newFormat.height)) {
                discardReasons2 |= 512;
            }
            if ((!ColorInfo.isEquivalentToAssumedSdrDefault(oldFormat.colorInfo) || !ColorInfo.isEquivalentToAssumedSdrDefault(newFormat.colorInfo)) && !Util.areEqual(oldFormat.colorInfo, newFormat.colorInfo)) {
                discardReasons2 |= 2048;
            }
            if (needsAdaptationReconfigureWorkaround(this.name) && !oldFormat.initializationDataEquals(newFormat)) {
                discardReasons2 |= 2;
            }
            if (discardReasons2 != 0) {
                oldFormat2 = oldFormat;
                newFormat2 = newFormat;
                discardReasons = discardReasons2;
            } else {
                String str = this.name;
                if (oldFormat.initializationDataEquals(newFormat)) {
                    i = 3;
                } else {
                    i = 2;
                }
                return new DecoderReuseEvaluation(str, oldFormat, newFormat, i, 0);
            }
        } else {
            oldFormat2 = oldFormat;
            newFormat2 = newFormat;
            if (oldFormat2.channelCount != newFormat2.channelCount) {
                discardReasons2 |= 4096;
            }
            if (oldFormat2.sampleRate != newFormat2.sampleRate) {
                discardReasons2 |= 8192;
            }
            if (oldFormat2.pcmEncoding != newFormat2.pcmEncoding) {
                discardReasons2 |= 16384;
            }
            if (discardReasons2 == 0 && MimeTypes.AUDIO_AAC.equals(this.mimeType)) {
                Pair<Integer, Integer> oldCodecProfileLevel = MediaCodecUtil.getCodecProfileAndLevel(oldFormat2);
                Pair<Integer, Integer> newCodecProfileLevel = MediaCodecUtil.getCodecProfileAndLevel(newFormat2);
                if (oldCodecProfileLevel != null && newCodecProfileLevel != null) {
                    int oldProfile = ((Integer) oldCodecProfileLevel.first).intValue();
                    int newProfile = ((Integer) newCodecProfileLevel.first).intValue();
                    if (oldProfile == 42 && newProfile == 42) {
                        return new DecoderReuseEvaluation(this.name, oldFormat2, newFormat2, 3, 0);
                    }
                }
            }
            if (!oldFormat2.initializationDataEquals(newFormat2)) {
                discardReasons2 |= 32;
            }
            if (needsAdaptationFlushWorkaround(this.mimeType)) {
                discardReasons2 |= 2;
            }
            if (discardReasons2 != 0) {
                discardReasons = discardReasons2;
            } else {
                return new DecoderReuseEvaluation(this.name, oldFormat2, newFormat2, 1, 0);
            }
        }
        return new DecoderReuseEvaluation(this.name, oldFormat2, newFormat2, 0, discardReasons);
    }

    public boolean isVideoSizeAndRateSupportedV21(int width, int height, double frameRate) {
        if (this.capabilities == null) {
            logNoSupport("sizeAndRate.caps");
            return false;
        }
        android.media.MediaCodecInfo.VideoCapabilities videoCapabilities = this.capabilities.getVideoCapabilities();
        if (videoCapabilities == null) {
            logNoSupport("sizeAndRate.vCaps");
            return false;
        }
        if (Util.SDK_INT >= 29) {
            int evaluation = MediaCodecPerformancePointCoverageProvider.areResolutionAndFrameRateCovered(videoCapabilities, width, height, frameRate);
            if (evaluation == 2) {
                return true;
            }
            if (evaluation == 1) {
                logNoSupport("sizeAndRate.cover, " + width + "x" + height + "@" + frameRate);
                return false;
            }
        }
        if (!areSizeAndRateSupportedV21(videoCapabilities, width, height, frameRate)) {
            if (width >= height || !needsRotatedVerticalResolutionWorkaround(this.name) || !areSizeAndRateSupportedV21(videoCapabilities, height, width, frameRate)) {
                logNoSupport("sizeAndRate.support, " + width + "x" + height + "@" + frameRate);
                return false;
            }
            logAssumedSupport("sizeAndRate.rotated, " + width + "x" + height + "@" + frameRate);
        }
        return true;
    }

    public Point alignVideoSizeV21(int width, int height) {
        android.media.MediaCodecInfo.VideoCapabilities videoCapabilities;
        if (this.capabilities == null || (videoCapabilities = this.capabilities.getVideoCapabilities()) == null) {
            return null;
        }
        return alignVideoSizeV21(videoCapabilities, width, height);
    }

    public boolean isAudioSampleRateSupportedV21(int sampleRate) {
        if (this.capabilities == null) {
            logNoSupport("sampleRate.caps");
            return false;
        }
        android.media.MediaCodecInfo.AudioCapabilities audioCapabilities = this.capabilities.getAudioCapabilities();
        if (audioCapabilities == null) {
            logNoSupport("sampleRate.aCaps");
            return false;
        }
        if (!audioCapabilities.isSampleRateSupported(sampleRate)) {
            logNoSupport("sampleRate.support, " + sampleRate);
            return false;
        }
        return true;
    }

    public boolean isAudioChannelCountSupportedV21(int channelCount) {
        if (this.capabilities == null) {
            logNoSupport("channelCount.caps");
            return false;
        }
        android.media.MediaCodecInfo.AudioCapabilities audioCapabilities = this.capabilities.getAudioCapabilities();
        if (audioCapabilities == null) {
            logNoSupport("channelCount.aCaps");
            return false;
        }
        int maxInputChannelCount = adjustMaxInputChannelCount(this.name, this.mimeType, audioCapabilities.getMaxInputChannelCount());
        if (maxInputChannelCount < channelCount) {
            logNoSupport("channelCount.support, " + channelCount);
            return false;
        }
        return true;
    }

    private void logNoSupport(String message) {
        Log.d(TAG, "NoSupport [" + message + "] [" + this.name + ", " + this.mimeType + "] [" + Util.DEVICE_DEBUG_INFO + "]");
    }

    private void logAssumedSupport(String message) {
        Log.d(TAG, "AssumedSupport [" + message + "] [" + this.name + ", " + this.mimeType + "] [" + Util.DEVICE_DEBUG_INFO + "]");
    }

    private static int adjustMaxInputChannelCount(String name, String mimeType, int maxChannelCount) {
        int assumedMaxChannelCount;
        if (maxChannelCount > 1 || ((Util.SDK_INT >= 26 && maxChannelCount > 0) || MimeTypes.AUDIO_MPEG.equals(mimeType) || MimeTypes.AUDIO_AMR_NB.equals(mimeType) || MimeTypes.AUDIO_AMR_WB.equals(mimeType) || MimeTypes.AUDIO_AAC.equals(mimeType) || MimeTypes.AUDIO_VORBIS.equals(mimeType) || MimeTypes.AUDIO_OPUS.equals(mimeType) || MimeTypes.AUDIO_RAW.equals(mimeType) || MimeTypes.AUDIO_FLAC.equals(mimeType) || MimeTypes.AUDIO_ALAW.equals(mimeType) || MimeTypes.AUDIO_MLAW.equals(mimeType) || MimeTypes.AUDIO_MSGSM.equals(mimeType))) {
            return maxChannelCount;
        }
        if (MimeTypes.AUDIO_AC3.equals(mimeType)) {
            assumedMaxChannelCount = 6;
        } else if (MimeTypes.AUDIO_E_AC3.equals(mimeType)) {
            assumedMaxChannelCount = 16;
        } else {
            assumedMaxChannelCount = 30;
        }
        Log.w(TAG, "AssumedMaxChannelAdjustment: " + name + ", [" + maxChannelCount + " to " + assumedMaxChannelCount + "]");
        return assumedMaxChannelCount;
    }

    private static boolean isAdaptive(android.media.MediaCodecInfo.CodecCapabilities capabilities) {
        return capabilities.isFeatureSupported("adaptive-playback");
    }

    private static boolean isTunneling(android.media.MediaCodecInfo.CodecCapabilities capabilities) {
        return Util.SDK_INT >= 21 && isTunnelingV21(capabilities);
    }

    private static boolean isTunnelingV21(android.media.MediaCodecInfo.CodecCapabilities capabilities) {
        return capabilities.isFeatureSupported("tunneled-playback");
    }

    private static boolean isSecure(android.media.MediaCodecInfo.CodecCapabilities capabilities) {
        return Util.SDK_INT >= 21 && isSecureV21(capabilities);
    }

    private static boolean isSecureV21(android.media.MediaCodecInfo.CodecCapabilities capabilities) {
        return capabilities.isFeatureSupported("secure-playback");
    }

    private static boolean areSizeAndRateSupportedV21(android.media.MediaCodecInfo.VideoCapabilities capabilities, int width, int height, double frameRate) {
        Point alignedSize = alignVideoSizeV21(capabilities, width, height);
        int width2 = alignedSize.x;
        int height2 = alignedSize.y;
        if (frameRate == -1.0d || frameRate < 1.0d) {
            return capabilities.isSizeSupported(width2, height2);
        }
        double floorFrameRate = Math.floor(frameRate);
        return capabilities.areSizeAndRateSupported(width2, height2, floorFrameRate);
    }

    private static Point alignVideoSizeV21(android.media.MediaCodecInfo.VideoCapabilities capabilities, int width, int height) {
        int widthAlignment = capabilities.getWidthAlignment();
        int heightAlignment = capabilities.getHeightAlignment();
        return new Point(Util.ceilDivide(width, widthAlignment) * widthAlignment, Util.ceilDivide(height, heightAlignment) * heightAlignment);
    }

    private static int getMaxSupportedInstancesV23(android.media.MediaCodecInfo.CodecCapabilities capabilities) {
        return capabilities.getMaxSupportedInstances();
    }

    private static android.media.MediaCodecInfo.CodecProfileLevel[] estimateLegacyVp9ProfileLevels(android.media.MediaCodecInfo.CodecCapabilities capabilities) {
        int level;
        android.media.MediaCodecInfo.VideoCapabilities videoCapabilities;
        int maxBitrate = 0;
        if (capabilities != null && (videoCapabilities = capabilities.getVideoCapabilities()) != null) {
            maxBitrate = ((Integer) videoCapabilities.getBitrateRange().getUpper()).intValue();
        }
        if (maxBitrate >= 180000000) {
            level = 1024;
        } else if (maxBitrate >= 120000000) {
            level = 512;
        } else if (maxBitrate >= 60000000) {
            level = 256;
        } else if (maxBitrate >= 30000000) {
            level = 128;
        } else if (maxBitrate >= 18000000) {
            level = 64;
        } else if (maxBitrate >= 12000000) {
            level = 32;
        } else if (maxBitrate >= 7200000) {
            level = 16;
        } else if (maxBitrate >= 3600000) {
            level = 8;
        } else if (maxBitrate >= 1800000) {
            level = 4;
        } else if (maxBitrate >= 800000) {
            level = 2;
        } else {
            level = 1;
        }
        android.media.MediaCodecInfo.CodecProfileLevel profileLevel = new android.media.MediaCodecInfo.CodecProfileLevel();
        profileLevel.profile = 1;
        profileLevel.level = level;
        return new android.media.MediaCodecInfo.CodecProfileLevel[]{profileLevel};
    }

    private static boolean needsDisableAdaptationWorkaround(String name) {
        return Util.SDK_INT <= 22 && ("ODROID-XU3".equals(Util.MODEL) || "Nexus 10".equals(Util.MODEL)) && ("OMX.Exynos.AVC.Decoder".equals(name) || "OMX.Exynos.AVC.Decoder.secure".equals(name));
    }

    private static boolean needsAdaptationReconfigureWorkaround(String name) {
        return Util.MODEL.startsWith("SM-T230") && "OMX.MARVELL.VIDEO.HW.CODA7542DECODER".equals(name);
    }

    private static boolean needsAdaptationFlushWorkaround(String mimeType) {
        return MimeTypes.AUDIO_OPUS.equals(mimeType);
    }

    private static boolean needsRotatedVerticalResolutionWorkaround(String name) {
        if ("OMX.MTK.VIDEO.DECODER.HEVC".equals(name) && "mcv5a".equals(Util.DEVICE)) {
            return false;
        }
        return true;
    }

    private static boolean needsProfileExcludedWorkaround(String mimeType, int profile) {
        return MimeTypes.VIDEO_H265.equals(mimeType) && 2 == profile && ("sailfish".equals(Util.DEVICE) || "marlin".equals(Util.DEVICE));
    }
}
