package androidx.media3.common;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Util;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public final class Format {
    public static final int CUE_REPLACEMENT_BEHAVIOR_MERGE = 1;
    public static final int CUE_REPLACEMENT_BEHAVIOR_REPLACE = 2;
    public static final int NO_VALUE = -1;
    public static final long OFFSET_SAMPLE_RELATIVE = Long.MAX_VALUE;
    public final int accessibilityChannel;
    public final int averageBitrate;
    public final int bitrate;
    public final int channelCount;
    public final String codecs;
    public final ColorInfo colorInfo;
    public final String containerMimeType;
    public final int cryptoType;
    public final int cueReplacementBehavior;
    public final Object customData;
    public final DrmInitData drmInitData;
    public final int encoderDelay;
    public final int encoderPadding;
    public final float frameRate;
    private int hashCode;
    public final int height;
    public final String id;
    public final List<byte[]> initializationData;
    public final String label;
    public final List<Label> labels;
    public final String language;
    public final int maxInputSize;
    public final int maxNumReorderSamples;
    public final Metadata metadata;
    public final int pcmEncoding;
    public final int peakBitrate;
    public final float pixelWidthHeightRatio;
    public final byte[] projectionData;
    public final int roleFlags;
    public final int rotationDegrees;
    public final String sampleMimeType;
    public final int sampleRate;
    public final int selectionFlags;
    public final int stereoMode;
    public final long subsampleOffsetUs;
    public final int tileCountHorizontal;
    public final int tileCountVertical;
    public final int width;
    private static final Format DEFAULT = new Builder().build();
    private static final String FIELD_ID = Util.intToStringMaxRadix(0);
    private static final String FIELD_LABEL = Util.intToStringMaxRadix(1);
    private static final String FIELD_LANGUAGE = Util.intToStringMaxRadix(2);
    private static final String FIELD_SELECTION_FLAGS = Util.intToStringMaxRadix(3);
    private static final String FIELD_ROLE_FLAGS = Util.intToStringMaxRadix(4);
    private static final String FIELD_AVERAGE_BITRATE = Util.intToStringMaxRadix(5);
    private static final String FIELD_PEAK_BITRATE = Util.intToStringMaxRadix(6);
    private static final String FIELD_CODECS = Util.intToStringMaxRadix(7);
    private static final String FIELD_METADATA = Util.intToStringMaxRadix(8);
    private static final String FIELD_CONTAINER_MIME_TYPE = Util.intToStringMaxRadix(9);
    private static final String FIELD_SAMPLE_MIME_TYPE = Util.intToStringMaxRadix(10);
    private static final String FIELD_MAX_INPUT_SIZE = Util.intToStringMaxRadix(11);
    private static final String FIELD_INITIALIZATION_DATA = Util.intToStringMaxRadix(12);
    private static final String FIELD_DRM_INIT_DATA = Util.intToStringMaxRadix(13);
    private static final String FIELD_SUBSAMPLE_OFFSET_US = Util.intToStringMaxRadix(14);
    private static final String FIELD_WIDTH = Util.intToStringMaxRadix(15);
    private static final String FIELD_HEIGHT = Util.intToStringMaxRadix(16);
    private static final String FIELD_FRAME_RATE = Util.intToStringMaxRadix(17);
    private static final String FIELD_ROTATION_DEGREES = Util.intToStringMaxRadix(18);
    private static final String FIELD_PIXEL_WIDTH_HEIGHT_RATIO = Util.intToStringMaxRadix(19);
    private static final String FIELD_PROJECTION_DATA = Util.intToStringMaxRadix(20);
    private static final String FIELD_STEREO_MODE = Util.intToStringMaxRadix(21);
    private static final String FIELD_COLOR_INFO = Util.intToStringMaxRadix(22);
    private static final String FIELD_CHANNEL_COUNT = Util.intToStringMaxRadix(23);
    private static final String FIELD_SAMPLE_RATE = Util.intToStringMaxRadix(24);
    private static final String FIELD_PCM_ENCODING = Util.intToStringMaxRadix(25);
    private static final String FIELD_ENCODER_DELAY = Util.intToStringMaxRadix(26);
    private static final String FIELD_ENCODER_PADDING = Util.intToStringMaxRadix(27);
    private static final String FIELD_ACCESSIBILITY_CHANNEL = Util.intToStringMaxRadix(28);
    private static final String FIELD_CRYPTO_TYPE = Util.intToStringMaxRadix(29);
    private static final String FIELD_TILE_COUNT_HORIZONTAL = Util.intToStringMaxRadix(30);
    private static final String FIELD_TILE_COUNT_VERTICAL = Util.intToStringMaxRadix(31);
    private static final String FIELD_LABELS = Util.intToStringMaxRadix(32);

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface CueReplacementBehavior {
    }

    public static final class Builder {
        private int accessibilityChannel;
        private int averageBitrate;
        private int channelCount;
        private String codecs;
        private ColorInfo colorInfo;
        private String containerMimeType;
        private int cryptoType;
        private int cueReplacementBehavior;
        private Object customData;
        private DrmInitData drmInitData;
        private int encoderDelay;
        private int encoderPadding;
        private float frameRate;
        private int height;
        private String id;
        private List<byte[]> initializationData;
        private String label;
        private List<Label> labels;
        private String language;
        private int maxInputSize;
        private int maxNumReorderSamples;
        private Metadata metadata;
        private int pcmEncoding;
        private int peakBitrate;
        private float pixelWidthHeightRatio;
        private byte[] projectionData;
        private int roleFlags;
        private int rotationDegrees;
        private String sampleMimeType;
        private int sampleRate;
        private int selectionFlags;
        private int stereoMode;
        private long subsampleOffsetUs;
        private int tileCountHorizontal;
        private int tileCountVertical;
        private int width;

        public Builder() {
            this.labels = ImmutableList.of();
            this.averageBitrate = -1;
            this.peakBitrate = -1;
            this.maxInputSize = -1;
            this.maxNumReorderSamples = -1;
            this.subsampleOffsetUs = Long.MAX_VALUE;
            this.width = -1;
            this.height = -1;
            this.frameRate = -1.0f;
            this.pixelWidthHeightRatio = 1.0f;
            this.stereoMode = -1;
            this.channelCount = -1;
            this.sampleRate = -1;
            this.pcmEncoding = -1;
            this.accessibilityChannel = -1;
            this.cueReplacementBehavior = 1;
            this.tileCountHorizontal = -1;
            this.tileCountVertical = -1;
            this.cryptoType = 0;
        }

        private Builder(Format format) {
            this.id = format.id;
            this.label = format.label;
            this.labels = format.labels;
            this.language = format.language;
            this.selectionFlags = format.selectionFlags;
            this.roleFlags = format.roleFlags;
            this.averageBitrate = format.averageBitrate;
            this.peakBitrate = format.peakBitrate;
            this.codecs = format.codecs;
            this.metadata = format.metadata;
            this.customData = format.customData;
            this.containerMimeType = format.containerMimeType;
            this.sampleMimeType = format.sampleMimeType;
            this.maxInputSize = format.maxInputSize;
            this.maxNumReorderSamples = format.maxNumReorderSamples;
            this.initializationData = format.initializationData;
            this.drmInitData = format.drmInitData;
            this.subsampleOffsetUs = format.subsampleOffsetUs;
            this.width = format.width;
            this.height = format.height;
            this.frameRate = format.frameRate;
            this.rotationDegrees = format.rotationDegrees;
            this.pixelWidthHeightRatio = format.pixelWidthHeightRatio;
            this.projectionData = format.projectionData;
            this.stereoMode = format.stereoMode;
            this.colorInfo = format.colorInfo;
            this.channelCount = format.channelCount;
            this.sampleRate = format.sampleRate;
            this.pcmEncoding = format.pcmEncoding;
            this.encoderDelay = format.encoderDelay;
            this.encoderPadding = format.encoderPadding;
            this.accessibilityChannel = format.accessibilityChannel;
            this.cueReplacementBehavior = format.cueReplacementBehavior;
            this.tileCountHorizontal = format.tileCountHorizontal;
            this.tileCountVertical = format.tileCountVertical;
            this.cryptoType = format.cryptoType;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setId(int id) {
            this.id = Integer.toString(id);
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder setLabels(List<Label> labels) {
            this.labels = ImmutableList.copyOf((Collection) labels);
            return this;
        }

        public Builder setLanguage(String language) {
            this.language = language;
            return this;
        }

        public Builder setSelectionFlags(int selectionFlags) {
            this.selectionFlags = selectionFlags;
            return this;
        }

        public Builder setRoleFlags(int roleFlags) {
            this.roleFlags = roleFlags;
            return this;
        }

        public Builder setAverageBitrate(int averageBitrate) {
            this.averageBitrate = averageBitrate;
            return this;
        }

        public Builder setPeakBitrate(int peakBitrate) {
            this.peakBitrate = peakBitrate;
            return this;
        }

        public Builder setCodecs(String codecs) {
            this.codecs = codecs;
            return this;
        }

        public Builder setMetadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder setCustomData(Object customData) {
            this.customData = customData;
            return this;
        }

        public Builder setContainerMimeType(String containerMimeType) {
            this.containerMimeType = MimeTypes.normalizeMimeType(containerMimeType);
            return this;
        }

        public Builder setSampleMimeType(String sampleMimeType) {
            this.sampleMimeType = MimeTypes.normalizeMimeType(sampleMimeType);
            return this;
        }

        public Builder setMaxInputSize(int maxInputSize) {
            this.maxInputSize = maxInputSize;
            return this;
        }

        public Builder setMaxNumReorderSamples(int maxNumReorderSamples) {
            this.maxNumReorderSamples = maxNumReorderSamples;
            return this;
        }

        public Builder setInitializationData(List<byte[]> initializationData) {
            this.initializationData = initializationData;
            return this;
        }

        public Builder setDrmInitData(DrmInitData drmInitData) {
            this.drmInitData = drmInitData;
            return this;
        }

        public Builder setSubsampleOffsetUs(long subsampleOffsetUs) {
            this.subsampleOffsetUs = subsampleOffsetUs;
            return this;
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setFrameRate(float frameRate) {
            this.frameRate = frameRate;
            return this;
        }

        public Builder setRotationDegrees(int rotationDegrees) {
            this.rotationDegrees = rotationDegrees;
            return this;
        }

        public Builder setPixelWidthHeightRatio(float pixelWidthHeightRatio) {
            this.pixelWidthHeightRatio = pixelWidthHeightRatio;
            return this;
        }

        public Builder setProjectionData(byte[] projectionData) {
            this.projectionData = projectionData;
            return this;
        }

        public Builder setStereoMode(int stereoMode) {
            this.stereoMode = stereoMode;
            return this;
        }

        public Builder setColorInfo(ColorInfo colorInfo) {
            this.colorInfo = colorInfo;
            return this;
        }

        public Builder setChannelCount(int channelCount) {
            this.channelCount = channelCount;
            return this;
        }

        public Builder setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder setPcmEncoding(int pcmEncoding) {
            this.pcmEncoding = pcmEncoding;
            return this;
        }

        public Builder setEncoderDelay(int encoderDelay) {
            this.encoderDelay = encoderDelay;
            return this;
        }

        public Builder setEncoderPadding(int encoderPadding) {
            this.encoderPadding = encoderPadding;
            return this;
        }

        public Builder setAccessibilityChannel(int accessibilityChannel) {
            this.accessibilityChannel = accessibilityChannel;
            return this;
        }

        public Builder setCueReplacementBehavior(int cueReplacementBehavior) {
            this.cueReplacementBehavior = cueReplacementBehavior;
            return this;
        }

        public Builder setTileCountHorizontal(int tileCountHorizontal) {
            this.tileCountHorizontal = tileCountHorizontal;
            return this;
        }

        public Builder setTileCountVertical(int tileCountVertical) {
            this.tileCountVertical = tileCountVertical;
            return this;
        }

        public Builder setCryptoType(int cryptoType) {
            this.cryptoType = cryptoType;
            return this;
        }

        public Format build() {
            return new Format(this);
        }
    }

    private static boolean isLabelPartOfLabels(Builder builder) {
        if (builder.labels.isEmpty() && builder.label == null) {
            return true;
        }
        for (int i = 0; i < builder.labels.size(); i++) {
            if (((Label) builder.labels.get(i)).value.equals(builder.label)) {
                return true;
            }
        }
        return false;
    }

    private Format(Builder builder) {
        this.id = builder.id;
        this.language = Util.normalizeLanguageCode(builder.language);
        if (!builder.labels.isEmpty() || builder.label == null) {
            if (!builder.labels.isEmpty() && builder.label == null) {
                this.labels = builder.labels;
                this.label = getDefaultLabel(builder.labels, this.language);
            } else {
                Assertions.checkState(isLabelPartOfLabels(builder));
                this.labels = builder.labels;
                this.label = builder.label;
            }
        } else {
            this.labels = ImmutableList.of(new Label(this.language, builder.label));
            this.label = builder.label;
        }
        this.selectionFlags = builder.selectionFlags;
        this.roleFlags = builder.roleFlags;
        this.averageBitrate = builder.averageBitrate;
        this.peakBitrate = builder.peakBitrate;
        this.bitrate = this.peakBitrate != -1 ? this.peakBitrate : this.averageBitrate;
        this.codecs = builder.codecs;
        this.metadata = builder.metadata;
        this.customData = builder.customData;
        this.containerMimeType = builder.containerMimeType;
        this.sampleMimeType = builder.sampleMimeType;
        this.maxInputSize = builder.maxInputSize;
        this.maxNumReorderSamples = builder.maxNumReorderSamples;
        this.initializationData = builder.initializationData == null ? Collections.emptyList() : builder.initializationData;
        this.drmInitData = builder.drmInitData;
        this.subsampleOffsetUs = builder.subsampleOffsetUs;
        this.width = builder.width;
        this.height = builder.height;
        this.frameRate = builder.frameRate;
        this.rotationDegrees = builder.rotationDegrees == -1 ? 0 : builder.rotationDegrees;
        this.pixelWidthHeightRatio = builder.pixelWidthHeightRatio == -1.0f ? 1.0f : builder.pixelWidthHeightRatio;
        this.projectionData = builder.projectionData;
        this.stereoMode = builder.stereoMode;
        this.colorInfo = builder.colorInfo;
        this.channelCount = builder.channelCount;
        this.sampleRate = builder.sampleRate;
        this.pcmEncoding = builder.pcmEncoding;
        this.encoderDelay = builder.encoderDelay == -1 ? 0 : builder.encoderDelay;
        this.encoderPadding = builder.encoderPadding != -1 ? builder.encoderPadding : 0;
        this.accessibilityChannel = builder.accessibilityChannel;
        this.cueReplacementBehavior = builder.cueReplacementBehavior;
        this.tileCountHorizontal = builder.tileCountHorizontal;
        this.tileCountVertical = builder.tileCountVertical;
        if (builder.cryptoType != 0 || this.drmInitData == null) {
            this.cryptoType = builder.cryptoType;
        } else {
            this.cryptoType = 1;
        }
    }

    public Builder buildUpon() {
        return new Builder();
    }

    public Format withManifestFormatInfo(Format manifestFormat) {
        Metadata metadata;
        if (this == manifestFormat) {
            return this;
        }
        int trackType = MimeTypes.getTrackType(this.sampleMimeType);
        String id = manifestFormat.id;
        int tileCountHorizontal = manifestFormat.tileCountHorizontal;
        int tileCountVertical = manifestFormat.tileCountVertical;
        String label = manifestFormat.label != null ? manifestFormat.label : this.label;
        List<Label> labels = !manifestFormat.labels.isEmpty() ? manifestFormat.labels : this.labels;
        String language = this.language;
        if ((trackType == 3 || trackType == 1) && manifestFormat.language != null) {
            language = manifestFormat.language;
        }
        int averageBitrate = this.averageBitrate == -1 ? manifestFormat.averageBitrate : this.averageBitrate;
        int peakBitrate = this.peakBitrate == -1 ? manifestFormat.peakBitrate : this.peakBitrate;
        String codecs = this.codecs;
        if (codecs == null) {
            String codecsOfType = Util.getCodecsOfType(manifestFormat.codecs, trackType);
            if (Util.splitCodecs(codecsOfType).length == 1) {
                codecs = codecsOfType;
            }
        }
        if (this.metadata == null) {
            metadata = manifestFormat.metadata;
        } else {
            metadata = this.metadata.copyWithAppendedEntriesFrom(manifestFormat.metadata);
        }
        float frameRate = this.frameRate;
        if (frameRate == -1.0f && trackType == 2) {
            frameRate = manifestFormat.frameRate;
        }
        int selectionFlags = this.selectionFlags | manifestFormat.selectionFlags;
        int roleFlags = manifestFormat.roleFlags | this.roleFlags;
        DrmInitData drmInitData = DrmInitData.createSessionCreationData(manifestFormat.drmInitData, this.drmInitData);
        return buildUpon().setId(id).setLabel(label).setLabels(labels).setLanguage(language).setSelectionFlags(selectionFlags).setRoleFlags(roleFlags).setAverageBitrate(averageBitrate).setPeakBitrate(peakBitrate).setCodecs(codecs).setMetadata(metadata).setDrmInitData(drmInitData).setFrameRate(frameRate).setTileCountHorizontal(tileCountHorizontal).setTileCountVertical(tileCountVertical).build();
    }

    public Format copyWithCryptoType(int cryptoType) {
        return buildUpon().setCryptoType(cryptoType).build();
    }

    public int getPixelCount() {
        if (this.width == -1 || this.height == -1) {
            return -1;
        }
        return this.height * this.width;
    }

    public String toString() {
        return "Format(" + this.id + ", " + this.label + ", " + this.containerMimeType + ", " + this.sampleMimeType + ", " + this.codecs + ", " + this.bitrate + ", " + this.language + ", [" + this.width + ", " + this.height + ", " + this.frameRate + ", " + this.colorInfo + "], [" + this.channelCount + ", " + this.sampleRate + "])";
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            int result = (17 * 31) + (this.id == null ? 0 : this.id.hashCode());
            this.hashCode = (((((((((((((((((((((((((((((((((((((((((((((((((((((((((result * 31) + (this.label == null ? 0 : this.label.hashCode())) * 31) + this.labels.hashCode()) * 31) + (this.language == null ? 0 : this.language.hashCode())) * 31) + this.selectionFlags) * 31) + this.roleFlags) * 31) + this.averageBitrate) * 31) + this.peakBitrate) * 31) + (this.codecs == null ? 0 : this.codecs.hashCode())) * 31) + (this.metadata == null ? 0 : this.metadata.hashCode())) * 31) + (this.customData == null ? 0 : this.customData.hashCode())) * 31) + (this.containerMimeType == null ? 0 : this.containerMimeType.hashCode())) * 31) + (this.sampleMimeType != null ? this.sampleMimeType.hashCode() : 0)) * 31) + this.maxInputSize) * 31) + ((int) this.subsampleOffsetUs)) * 31) + this.width) * 31) + this.height) * 31) + Float.floatToIntBits(this.frameRate)) * 31) + this.rotationDegrees) * 31) + Float.floatToIntBits(this.pixelWidthHeightRatio)) * 31) + this.stereoMode) * 31) + this.channelCount) * 31) + this.sampleRate) * 31) + this.pcmEncoding) * 31) + this.encoderDelay) * 31) + this.encoderPadding) * 31) + this.accessibilityChannel) * 31) + this.tileCountHorizontal) * 31) + this.tileCountVertical) * 31) + this.cryptoType;
        }
        int result2 = this.hashCode;
        return result2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Format other = (Format) obj;
        if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode) {
            return false;
        }
        if (this.selectionFlags == other.selectionFlags && this.roleFlags == other.roleFlags && this.averageBitrate == other.averageBitrate && this.peakBitrate == other.peakBitrate && this.maxInputSize == other.maxInputSize && this.subsampleOffsetUs == other.subsampleOffsetUs && this.width == other.width && this.height == other.height && this.rotationDegrees == other.rotationDegrees && this.stereoMode == other.stereoMode && this.channelCount == other.channelCount && this.sampleRate == other.sampleRate && this.pcmEncoding == other.pcmEncoding && this.encoderDelay == other.encoderDelay && this.encoderPadding == other.encoderPadding && this.accessibilityChannel == other.accessibilityChannel && this.tileCountHorizontal == other.tileCountHorizontal && this.tileCountVertical == other.tileCountVertical && this.cryptoType == other.cryptoType && Float.compare(this.frameRate, other.frameRate) == 0 && Float.compare(this.pixelWidthHeightRatio, other.pixelWidthHeightRatio) == 0 && Objects.equals(this.id, other.id) && Objects.equals(this.label, other.label) && this.labels.equals(other.labels) && Objects.equals(this.codecs, other.codecs) && Objects.equals(this.containerMimeType, other.containerMimeType) && Objects.equals(this.sampleMimeType, other.sampleMimeType) && Objects.equals(this.language, other.language) && Arrays.equals(this.projectionData, other.projectionData) && Objects.equals(this.metadata, other.metadata) && Objects.equals(this.colorInfo, other.colorInfo) && Objects.equals(this.drmInitData, other.drmInitData) && initializationDataEquals(other) && Objects.equals(this.customData, other.customData)) {
            return true;
        }
        return false;
    }

    public boolean initializationDataEquals(Format other) {
        if (this.initializationData.size() != other.initializationData.size()) {
            return false;
        }
        for (int i = 0; i < this.initializationData.size(); i++) {
            if (!Arrays.equals(this.initializationData.get(i), other.initializationData.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static String toLogString(Format format) {
        if (format == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("id=").append(format.id).append(", mimeType=").append(format.sampleMimeType);
        if (format.containerMimeType != null) {
            builder.append(", container=").append(format.containerMimeType);
        }
        if (format.bitrate != -1) {
            builder.append(", bitrate=").append(format.bitrate);
        }
        if (format.codecs != null) {
            builder.append(", codecs=").append(format.codecs);
        }
        if (format.drmInitData != null) {
            Set<String> schemes = new LinkedHashSet<>();
            for (int i = 0; i < format.drmInitData.schemeDataCount; i++) {
                UUID schemeUuid = format.drmInitData.get(i).uuid;
                if (schemeUuid.equals(C.COMMON_PSSH_UUID)) {
                    schemes.add(C.CENC_TYPE_cenc);
                } else if (schemeUuid.equals(C.CLEARKEY_UUID)) {
                    schemes.add("clearkey");
                } else if (schemeUuid.equals(C.PLAYREADY_UUID)) {
                    schemes.add("playready");
                } else if (schemeUuid.equals(C.WIDEVINE_UUID)) {
                    schemes.add("widevine");
                } else if (schemeUuid.equals(C.UUID_NIL)) {
                    schemes.add("universal");
                } else {
                    schemes.add("unknown (" + schemeUuid + ")");
                }
            }
            builder.append(", drm=[");
            Joiner.on(',').appendTo(builder, (Iterable<? extends Object>) schemes);
            builder.append(']');
        }
        if (format.width != -1 && format.height != -1) {
            builder.append(", res=").append(format.width).append("x").append(format.height);
        }
        if (format.colorInfo != null && format.colorInfo.isValid()) {
            builder.append(", color=").append(format.colorInfo.toLogString());
        }
        if (format.frameRate != -1.0f) {
            builder.append(", fps=").append(format.frameRate);
        }
        if (format.channelCount != -1) {
            builder.append(", channels=").append(format.channelCount);
        }
        if (format.sampleRate != -1) {
            builder.append(", sample_rate=").append(format.sampleRate);
        }
        if (format.language != null) {
            builder.append(", language=").append(format.language);
        }
        if (!format.labels.isEmpty()) {
            builder.append(", labels=[");
            Joiner.on(',').appendTo(builder, (Iterable<? extends Object>) format.labels);
            builder.append("]");
        }
        if (format.selectionFlags != 0) {
            builder.append(", selectionFlags=[");
            Joiner.on(',').appendTo(builder, (Iterable<? extends Object>) Util.getSelectionFlagStrings(format.selectionFlags));
            builder.append("]");
        }
        if (format.roleFlags != 0) {
            builder.append(", roleFlags=[");
            Joiner.on(',').appendTo(builder, (Iterable<? extends Object>) Util.getRoleFlagStrings(format.roleFlags));
            builder.append("]");
        }
        if (format.customData != null) {
            builder.append(", customData=").append(format.customData);
        }
        return builder.toString();
    }

    @Deprecated
    public Bundle toBundle() {
        return toBundle(false);
    }

    public Bundle toBundle(boolean excludeMetadata) {
        Bundle bundle = new Bundle();
        bundle.putString(FIELD_ID, this.id);
        bundle.putString(FIELD_LABEL, this.label);
        bundle.putParcelableArrayList(FIELD_LABELS, BundleCollectionUtil.toBundleArrayList(this.labels, new Function() { // from class: androidx.media3.common.Format$$ExternalSyntheticLambda1
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return ((Label) obj).toBundle();
            }
        }));
        bundle.putString(FIELD_LANGUAGE, this.language);
        bundle.putInt(FIELD_SELECTION_FLAGS, this.selectionFlags);
        bundle.putInt(FIELD_ROLE_FLAGS, this.roleFlags);
        bundle.putInt(FIELD_AVERAGE_BITRATE, this.averageBitrate);
        bundle.putInt(FIELD_PEAK_BITRATE, this.peakBitrate);
        bundle.putString(FIELD_CODECS, this.codecs);
        if (!excludeMetadata) {
            bundle.putParcelable(FIELD_METADATA, this.metadata);
        }
        bundle.putString(FIELD_CONTAINER_MIME_TYPE, this.containerMimeType);
        bundle.putString(FIELD_SAMPLE_MIME_TYPE, this.sampleMimeType);
        bundle.putInt(FIELD_MAX_INPUT_SIZE, this.maxInputSize);
        for (int i = 0; i < this.initializationData.size(); i++) {
            bundle.putByteArray(keyForInitializationData(i), this.initializationData.get(i));
        }
        bundle.putParcelable(FIELD_DRM_INIT_DATA, this.drmInitData);
        bundle.putLong(FIELD_SUBSAMPLE_OFFSET_US, this.subsampleOffsetUs);
        bundle.putInt(FIELD_WIDTH, this.width);
        bundle.putInt(FIELD_HEIGHT, this.height);
        bundle.putFloat(FIELD_FRAME_RATE, this.frameRate);
        bundle.putInt(FIELD_ROTATION_DEGREES, this.rotationDegrees);
        bundle.putFloat(FIELD_PIXEL_WIDTH_HEIGHT_RATIO, this.pixelWidthHeightRatio);
        bundle.putByteArray(FIELD_PROJECTION_DATA, this.projectionData);
        bundle.putInt(FIELD_STEREO_MODE, this.stereoMode);
        if (this.colorInfo != null) {
            bundle.putBundle(FIELD_COLOR_INFO, this.colorInfo.toBundle());
        }
        bundle.putInt(FIELD_CHANNEL_COUNT, this.channelCount);
        bundle.putInt(FIELD_SAMPLE_RATE, this.sampleRate);
        bundle.putInt(FIELD_PCM_ENCODING, this.pcmEncoding);
        bundle.putInt(FIELD_ENCODER_DELAY, this.encoderDelay);
        bundle.putInt(FIELD_ENCODER_PADDING, this.encoderPadding);
        bundle.putInt(FIELD_ACCESSIBILITY_CHANNEL, this.accessibilityChannel);
        bundle.putInt(FIELD_TILE_COUNT_HORIZONTAL, this.tileCountHorizontal);
        bundle.putInt(FIELD_TILE_COUNT_VERTICAL, this.tileCountVertical);
        bundle.putInt(FIELD_CRYPTO_TYPE, this.cryptoType);
        return bundle;
    }

    public static Format fromBundle(Bundle bundle) {
        List<Label> labels;
        Builder builder = new Builder();
        BundleCollectionUtil.ensureClassLoader(bundle);
        builder.setId((String) defaultIfNull(bundle.getString(FIELD_ID), DEFAULT.id)).setLabel((String) defaultIfNull(bundle.getString(FIELD_LABEL), DEFAULT.label));
        List<Bundle> labelsBundles = bundle.getParcelableArrayList(FIELD_LABELS);
        if (labelsBundles == null) {
            labels = ImmutableList.of();
        } else {
            labels = BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.common.Format$$ExternalSyntheticLambda0
                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return Label.fromBundle((Bundle) obj);
                }
            }, labelsBundles);
        }
        builder.setLabels(labels).setLanguage((String) defaultIfNull(bundle.getString(FIELD_LANGUAGE), DEFAULT.language)).setSelectionFlags(bundle.getInt(FIELD_SELECTION_FLAGS, DEFAULT.selectionFlags)).setRoleFlags(bundle.getInt(FIELD_ROLE_FLAGS, DEFAULT.roleFlags)).setAverageBitrate(bundle.getInt(FIELD_AVERAGE_BITRATE, DEFAULT.averageBitrate)).setPeakBitrate(bundle.getInt(FIELD_PEAK_BITRATE, DEFAULT.peakBitrate)).setCodecs((String) defaultIfNull(bundle.getString(FIELD_CODECS), DEFAULT.codecs)).setMetadata((Metadata) defaultIfNull((Metadata) bundle.getParcelable(FIELD_METADATA), DEFAULT.metadata)).setContainerMimeType((String) defaultIfNull(bundle.getString(FIELD_CONTAINER_MIME_TYPE), DEFAULT.containerMimeType)).setSampleMimeType((String) defaultIfNull(bundle.getString(FIELD_SAMPLE_MIME_TYPE), DEFAULT.sampleMimeType)).setMaxInputSize(bundle.getInt(FIELD_MAX_INPUT_SIZE, DEFAULT.maxInputSize));
        List<byte[]> initializationData = new ArrayList<>();
        int i = 0;
        while (true) {
            byte[] data = bundle.getByteArray(keyForInitializationData(i));
            if (data == null) {
                break;
            }
            initializationData.add(data);
            i++;
        }
        builder.setInitializationData(initializationData).setDrmInitData((DrmInitData) bundle.getParcelable(FIELD_DRM_INIT_DATA)).setSubsampleOffsetUs(bundle.getLong(FIELD_SUBSAMPLE_OFFSET_US, DEFAULT.subsampleOffsetUs)).setWidth(bundle.getInt(FIELD_WIDTH, DEFAULT.width)).setHeight(bundle.getInt(FIELD_HEIGHT, DEFAULT.height)).setFrameRate(bundle.getFloat(FIELD_FRAME_RATE, DEFAULT.frameRate)).setRotationDegrees(bundle.getInt(FIELD_ROTATION_DEGREES, DEFAULT.rotationDegrees)).setPixelWidthHeightRatio(bundle.getFloat(FIELD_PIXEL_WIDTH_HEIGHT_RATIO, DEFAULT.pixelWidthHeightRatio)).setProjectionData(bundle.getByteArray(FIELD_PROJECTION_DATA)).setStereoMode(bundle.getInt(FIELD_STEREO_MODE, DEFAULT.stereoMode));
        Bundle colorInfoBundle = bundle.getBundle(FIELD_COLOR_INFO);
        if (colorInfoBundle != null) {
            builder.setColorInfo(ColorInfo.fromBundle(colorInfoBundle));
        }
        builder.setChannelCount(bundle.getInt(FIELD_CHANNEL_COUNT, DEFAULT.channelCount)).setSampleRate(bundle.getInt(FIELD_SAMPLE_RATE, DEFAULT.sampleRate)).setPcmEncoding(bundle.getInt(FIELD_PCM_ENCODING, DEFAULT.pcmEncoding)).setEncoderDelay(bundle.getInt(FIELD_ENCODER_DELAY, DEFAULT.encoderDelay)).setEncoderPadding(bundle.getInt(FIELD_ENCODER_PADDING, DEFAULT.encoderPadding)).setAccessibilityChannel(bundle.getInt(FIELD_ACCESSIBILITY_CHANNEL, DEFAULT.accessibilityChannel)).setTileCountHorizontal(bundle.getInt(FIELD_TILE_COUNT_HORIZONTAL, DEFAULT.tileCountHorizontal)).setTileCountVertical(bundle.getInt(FIELD_TILE_COUNT_VERTICAL, DEFAULT.tileCountVertical)).setCryptoType(bundle.getInt(FIELD_CRYPTO_TYPE, DEFAULT.cryptoType));
        return builder.build();
    }

    private static String keyForInitializationData(int initialisationDataIndex) {
        return FIELD_INITIALIZATION_DATA + "_" + Integer.toString(initialisationDataIndex, 36);
    }

    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static String getDefaultLabel(List<Label> labels, String language) {
        for (Label l : labels) {
            if (TextUtils.equals(l.language, language)) {
                return l.value;
            }
        }
        return labels.get(0).value;
    }
}
