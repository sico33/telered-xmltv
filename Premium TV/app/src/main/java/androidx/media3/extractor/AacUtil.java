package androidx.media3.extractor;

import androidx.media3.common.ParserException;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableBitArray;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class AacUtil {
    public static final int AAC_ELD_MAX_RATE_BYTES_PER_SECOND = 8000;
    public static final int AAC_HE_AUDIO_SAMPLE_COUNT = 2048;
    public static final int AAC_HE_V2_MAX_RATE_BYTES_PER_SECOND = 7000;
    public static final int AAC_LC_AUDIO_SAMPLE_COUNT = 1024;
    public static final int AAC_LC_MAX_RATE_BYTES_PER_SECOND = 100000;
    public static final int AAC_LD_AUDIO_SAMPLE_COUNT = 512;
    public static final int AAC_XHE_AUDIO_SAMPLE_COUNT = 1024;
    public static final int AAC_XHE_MAX_RATE_BYTES_PER_SECOND = 256000;
    public static final int AUDIO_OBJECT_TYPE_AAC_ELD = 23;
    public static final int AUDIO_OBJECT_TYPE_AAC_ER_BSAC = 22;
    public static final int AUDIO_OBJECT_TYPE_AAC_LC = 2;
    public static final int AUDIO_OBJECT_TYPE_AAC_PS = 29;
    public static final int AUDIO_OBJECT_TYPE_AAC_SBR = 5;
    public static final int AUDIO_OBJECT_TYPE_AAC_XHE = 42;
    private static final int AUDIO_OBJECT_TYPE_ESCAPE = 31;
    private static final int AUDIO_SPECIFIC_CONFIG_CHANNEL_CONFIGURATION_INVALID = -1;
    private static final int AUDIO_SPECIFIC_CONFIG_FREQUENCY_INDEX_ARBITRARY = 15;
    private static final String CODECS_STRING_PREFIX = "mp4a.40.";
    private static final String TAG = "AacUtil";
    public static final int AAC_HE_V1_MAX_RATE_BYTES_PER_SECOND = 16000;
    private static final int[] AUDIO_SPECIFIC_CONFIG_SAMPLING_RATE_TABLE = {96000, 88200, 64000, OpusUtil.SAMPLE_RATE, 44100, 32000, 24000, 22050, AAC_HE_V1_MAX_RATE_BYTES_PER_SECOND, 12000, 11025, 8000, 7350};
    private static final int[] AUDIO_SPECIFIC_CONFIG_CHANNEL_COUNT_TABLE = {0, 1, 2, 3, 4, 5, 6, 8, -1, -1, -1, 7, 8, -1, 8, -1};

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface AacAudioObjectType {
    }

    public static final class Config {
        public final int channelCount;
        public final String codecs;
        public final int sampleRateHz;

        private Config(int sampleRateHz, int channelCount, String codecs) {
            this.sampleRateHz = sampleRateHz;
            this.channelCount = channelCount;
            this.codecs = codecs;
        }
    }

    public static Config parseAudioSpecificConfig(byte[] audioSpecificConfig) throws ParserException {
        return parseAudioSpecificConfig(new ParsableBitArray(audioSpecificConfig), false);
    }

    public static Config parseAudioSpecificConfig(ParsableBitArray bitArray, boolean forceReadToEnd) throws ParserException {
        int audioObjectType = getAudioObjectType(bitArray);
        int sampleRateHz = getSamplingFrequency(bitArray);
        int channelConfiguration = bitArray.readBits(4);
        String codecs = CODECS_STRING_PREFIX + audioObjectType;
        if (audioObjectType == 5 || audioObjectType == 29) {
            sampleRateHz = getSamplingFrequency(bitArray);
            audioObjectType = getAudioObjectType(bitArray);
            if (audioObjectType == 22) {
                channelConfiguration = bitArray.readBits(4);
            }
        }
        if (forceReadToEnd) {
            switch (audioObjectType) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 6:
                case 7:
                case 17:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                    parseGaSpecificConfig(bitArray, audioObjectType, channelConfiguration);
                    switch (audioObjectType) {
                        case 17:
                        case 19:
                        case 20:
                        case 21:
                        case 22:
                        case 23:
                            int epConfig = bitArray.readBits(2);
                            if (epConfig == 2 || epConfig == 3) {
                                throw ParserException.createForUnsupportedContainerFeature("Unsupported epConfig: " + epConfig);
                            }
                            break;
                    }
                    break;
                case 5:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 18:
                default:
                    throw ParserException.createForUnsupportedContainerFeature("Unsupported audio object type: " + audioObjectType);
            }
        }
        int channelCount = AUDIO_SPECIFIC_CONFIG_CHANNEL_COUNT_TABLE[channelConfiguration];
        if (channelCount == -1) {
            throw ParserException.createForMalformedContainer(null, null);
        }
        return new Config(sampleRateHz, channelCount, codecs);
    }

    public static byte[] buildAacLcAudioSpecificConfig(int sampleRate, int channelCount) {
        int sampleRateIndex = -1;
        for (int i = 0; i < AUDIO_SPECIFIC_CONFIG_SAMPLING_RATE_TABLE.length; i++) {
            if (sampleRate == AUDIO_SPECIFIC_CONFIG_SAMPLING_RATE_TABLE[i]) {
                sampleRateIndex = i;
            }
        }
        int channelConfig = -1;
        for (int i2 = 0; i2 < AUDIO_SPECIFIC_CONFIG_CHANNEL_COUNT_TABLE.length; i2++) {
            if (channelCount == AUDIO_SPECIFIC_CONFIG_CHANNEL_COUNT_TABLE[i2]) {
                channelConfig = i2;
            }
        }
        if (sampleRate == -1 || channelConfig == -1) {
            throw new IllegalArgumentException("Invalid sample rate or number of channels: " + sampleRate + ", " + channelCount);
        }
        return buildAudioSpecificConfig(2, sampleRateIndex, channelConfig);
    }

    public static byte[] buildAudioSpecificConfig(int audioObjectType, int sampleRateIndex, int channelConfig) {
        byte[] specificConfig = {(byte) (((audioObjectType << 3) & 248) | ((sampleRateIndex >> 1) & 7)), (byte) (((sampleRateIndex << 7) & 128) | ((channelConfig << 3) & 120))};
        return specificConfig;
    }

    private static int getAudioObjectType(ParsableBitArray bitArray) {
        int audioObjectType = bitArray.readBits(5);
        if (audioObjectType == 31) {
            return bitArray.readBits(6) + 32;
        }
        return audioObjectType;
    }

    private static int getSamplingFrequency(ParsableBitArray bitArray) throws ParserException {
        int frequencyIndex = bitArray.readBits(4);
        if (frequencyIndex == 15) {
            if (bitArray.bitsLeft() < 24) {
                throw ParserException.createForMalformedContainer("AAC header insufficient data", null);
            }
            int samplingFrequency = bitArray.readBits(24);
            return samplingFrequency;
        }
        if (frequencyIndex < 13) {
            int samplingFrequency2 = AUDIO_SPECIFIC_CONFIG_SAMPLING_RATE_TABLE[frequencyIndex];
            return samplingFrequency2;
        }
        throw ParserException.createForMalformedContainer("AAC header wrong Sampling Frequency Index", null);
    }

    private static void parseGaSpecificConfig(ParsableBitArray bitArray, int audioObjectType, int channelConfiguration) {
        boolean frameLengthFlag = bitArray.readBit();
        if (frameLengthFlag) {
            Log.w(TAG, "Unexpected frameLengthFlag = 1");
        }
        boolean dependsOnCoreDecoder = bitArray.readBit();
        if (dependsOnCoreDecoder) {
            bitArray.skipBits(14);
        }
        boolean extensionFlag = bitArray.readBit();
        if (channelConfiguration == 0) {
            throw new UnsupportedOperationException();
        }
        if (audioObjectType == 6 || audioObjectType == 20) {
            bitArray.skipBits(3);
        }
        if (extensionFlag) {
            if (audioObjectType == 22) {
                bitArray.skipBits(16);
            }
            if (audioObjectType == 17 || audioObjectType == 19 || audioObjectType == 20 || audioObjectType == 23) {
                bitArray.skipBits(3);
            }
            bitArray.skipBits(1);
        }
    }

    private AacUtil() {
    }
}
