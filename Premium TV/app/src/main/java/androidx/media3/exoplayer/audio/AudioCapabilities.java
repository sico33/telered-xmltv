package androidx.media3.exoplayer.audio;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.provider.Settings;
import android.util.Pair;
import android.util.SparseArray;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.primitives.Ints;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class AudioCapabilities {
    static final int DEFAULT_MAX_CHANNEL_COUNT = 10;
    static final int DEFAULT_SAMPLE_RATE_HZ = 48000;
    private static final String EXTERNAL_SURROUND_SOUND_KEY = "external_surround_sound_enabled";
    private static final String FORCE_EXTERNAL_SURROUND_SOUND_KEY = "use_external_surround_sound_flag";
    private final SparseArray<AudioProfile> encodingToAudioProfile;
    private final int maxChannelCount;
    public static final AudioCapabilities DEFAULT_AUDIO_CAPABILITIES = new AudioCapabilities(ImmutableList.of(AudioProfile.DEFAULT_AUDIO_PROFILE));
    private static final ImmutableList<Integer> EXTERNAL_SURROUND_SOUND_ENCODINGS = ImmutableList.of(2, 5, 6);
    static final ImmutableMap<Integer, Integer> ALL_SURROUND_ENCODINGS_AND_MAX_CHANNELS = new ImmutableMap.Builder().put(5, 6).put(17, 6).put(7, 6).put(30, 10).put(18, 6).put(6, 8).put(8, 8).put(14, 8).buildOrThrow();

    @Deprecated
    public static AudioCapabilities getCapabilities(Context context) {
        return getCapabilities(context, AudioAttributes.DEFAULT, null);
    }

    public static AudioCapabilities getCapabilities(Context context, AudioAttributes audioAttributes, AudioDeviceInfo routedDevice) {
        AudioDeviceInfoApi23 routedDeviceApi23 = (Util.SDK_INT < 23 || routedDevice == null) ? null : new AudioDeviceInfoApi23(routedDevice);
        return getCapabilitiesInternal(context, audioAttributes, routedDeviceApi23);
    }

    static AudioCapabilities getCapabilitiesInternal(Context context, AudioAttributes audioAttributes, AudioDeviceInfoApi23 routedDevice) {
        Intent intent = context.registerReceiver(null, new IntentFilter("android.media.action.HDMI_AUDIO_PLUG"));
        return getCapabilitiesInternal(context, intent, audioAttributes, routedDevice);
    }

    static AudioCapabilities getCapabilitiesInternal(Context context, Intent intent, AudioAttributes audioAttributes, AudioDeviceInfoApi23 routedDevice) {
        AudioDeviceInfoApi23 currentDevice;
        AudioManager audioManager = (AudioManager) Assertions.checkNotNull(context.getSystemService(MimeTypes.BASE_TYPE_AUDIO));
        if (routedDevice == null) {
            if (Util.SDK_INT >= 33) {
                currentDevice = Api33.getDefaultRoutedDeviceForAttributes(audioManager, audioAttributes);
            } else {
                currentDevice = null;
            }
        } else {
            currentDevice = routedDevice;
        }
        if (Util.SDK_INT >= 33 && (Util.isTv(context) || Util.isAutomotive(context))) {
            return Api33.getCapabilitiesInternalForDirectPlayback(audioManager, audioAttributes);
        }
        if (Util.SDK_INT >= 23 && Api23.isBluetoothConnected(audioManager, currentDevice)) {
            return DEFAULT_AUDIO_CAPABILITIES;
        }
        ImmutableSet.Builder<Integer> supportedEncodings = new ImmutableSet.Builder<>();
        supportedEncodings.add(2);
        if (Util.SDK_INT >= 29 && (Util.isTv(context) || Util.isAutomotive(context))) {
            supportedEncodings.addAll(Api29.getDirectPlaybackSupportedEncodings(audioAttributes));
            return new AudioCapabilities(getAudioProfiles(Ints.toArray(supportedEncodings.build()), 10));
        }
        ContentResolver contentResolver = context.getContentResolver();
        boolean forceExternalSurroundSoundSetting = Settings.Global.getInt(contentResolver, FORCE_EXTERNAL_SURROUND_SOUND_KEY, 0) == 1;
        if ((forceExternalSurroundSoundSetting || deviceMaySetExternalSurroundSoundGlobalSetting()) && Settings.Global.getInt(contentResolver, EXTERNAL_SURROUND_SOUND_KEY, 0) == 1) {
            supportedEncodings.addAll(EXTERNAL_SURROUND_SOUND_ENCODINGS);
        }
        if (intent != null && !forceExternalSurroundSoundSetting && intent.getIntExtra("android.media.extra.AUDIO_PLUG_STATE", 0) == 1) {
            int[] encodingsFromExtra = intent.getIntArrayExtra("android.media.extra.ENCODINGS");
            if (encodingsFromExtra != null) {
                supportedEncodings.addAll(Ints.asList(encodingsFromExtra));
            }
            return new AudioCapabilities(getAudioProfiles(Ints.toArray(supportedEncodings.build()), intent.getIntExtra("android.media.extra.MAX_CHANNEL_COUNT", 10)));
        }
        return new AudioCapabilities(getAudioProfiles(Ints.toArray(supportedEncodings.build()), 10));
    }

    static Uri getExternalSurroundSoundGlobalSettingUri() {
        if (deviceMaySetExternalSurroundSoundGlobalSetting()) {
            return Settings.Global.getUriFor(EXTERNAL_SURROUND_SOUND_KEY);
        }
        return null;
    }

    @Deprecated
    public AudioCapabilities(int[] supportedEncodings, int maxChannelCount) {
        this(getAudioProfiles(supportedEncodings, maxChannelCount));
    }

    private AudioCapabilities(List<AudioProfile> audioProfiles) {
        this.encodingToAudioProfile = new SparseArray<>();
        for (int i = 0; i < audioProfiles.size(); i++) {
            AudioProfile audioProfile = audioProfiles.get(i);
            this.encodingToAudioProfile.put(audioProfile.encoding, audioProfile);
        }
        int maxChannelCount = 0;
        for (int i2 = 0; i2 < this.encodingToAudioProfile.size(); i2++) {
            maxChannelCount = Math.max(maxChannelCount, this.encodingToAudioProfile.valueAt(i2).maxChannelCount);
        }
        this.maxChannelCount = maxChannelCount;
    }

    public boolean supportsEncoding(int encoding) {
        return Util.contains(this.encodingToAudioProfile, encoding);
    }

    public int getMaxChannelCount() {
        return this.maxChannelCount;
    }

    @Deprecated
    public boolean isPassthroughPlaybackSupported(Format format) {
        return isPassthroughPlaybackSupported(format, AudioAttributes.DEFAULT);
    }

    public boolean isPassthroughPlaybackSupported(Format format, AudioAttributes audioAttributes) {
        return getEncodingAndChannelConfigForPassthrough(format, audioAttributes) != null;
    }

    @Deprecated
    public Pair<Integer, Integer> getEncodingAndChannelConfigForPassthrough(Format format) {
        return getEncodingAndChannelConfigForPassthrough(format, AudioAttributes.DEFAULT);
    }

    public Pair<Integer, Integer> getEncodingAndChannelConfigForPassthrough(Format format, AudioAttributes audioAttributes) {
        int channelCount;
        int encoding = MimeTypes.getEncoding((String) Assertions.checkNotNull(format.sampleMimeType), format.codecs);
        if (!ALL_SURROUND_ENCODINGS_AND_MAX_CHANNELS.containsKey(Integer.valueOf(encoding))) {
            return null;
        }
        if (encoding == 18 && !supportsEncoding(18)) {
            encoding = 6;
        } else if ((encoding == 8 && !supportsEncoding(8)) || (encoding == 30 && !supportsEncoding(30))) {
            encoding = 7;
        }
        if (!supportsEncoding(encoding)) {
            return null;
        }
        AudioProfile audioProfile = (AudioProfile) Assertions.checkNotNull(this.encodingToAudioProfile.get(encoding));
        if (format.channelCount == -1 || encoding == 18) {
            int channelCount2 = format.sampleRate;
            int sampleRate = channelCount2 != -1 ? format.sampleRate : 48000;
            channelCount = audioProfile.getMaxSupportedChannelCountForPassthrough(sampleRate, audioAttributes);
        } else {
            channelCount = format.channelCount;
            if (format.sampleMimeType.equals(MimeTypes.AUDIO_DTS_X) && Util.SDK_INT < 33) {
                if (channelCount > 10) {
                    return null;
                }
            } else if (!audioProfile.supportsChannelCount(channelCount)) {
                return null;
            }
        }
        int channelConfig = getChannelConfigForPassthrough(channelCount);
        if (channelConfig == 0) {
            return null;
        }
        return Pair.create(Integer.valueOf(encoding), Integer.valueOf(channelConfig));
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AudioCapabilities)) {
            return false;
        }
        AudioCapabilities audioCapabilities = (AudioCapabilities) other;
        return Util.contentEquals(this.encodingToAudioProfile, audioCapabilities.encodingToAudioProfile) && this.maxChannelCount == audioCapabilities.maxChannelCount;
    }

    public int hashCode() {
        return this.maxChannelCount + (Util.contentHashCode(this.encodingToAudioProfile) * 31);
    }

    public String toString() {
        return "AudioCapabilities[maxChannelCount=" + this.maxChannelCount + ", audioProfiles=" + this.encodingToAudioProfile + "]";
    }

    private static boolean deviceMaySetExternalSurroundSoundGlobalSetting() {
        return "Amazon".equals(Util.MANUFACTURER) || "Xiaomi".equals(Util.MANUFACTURER);
    }

    private static int getChannelConfigForPassthrough(int channelCount) {
        if (Util.SDK_INT <= 28) {
            if (channelCount == 7) {
                channelCount = 8;
            } else if (channelCount == 3 || channelCount == 4 || channelCount == 5) {
                channelCount = 6;
            }
        }
        if (Util.SDK_INT <= 26 && "fugu".equals(Util.DEVICE) && channelCount == 1) {
            channelCount = 2;
        }
        return Util.getAudioTrackChannelConfig(channelCount);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ImmutableList<AudioProfile> getAudioProfiles(List<android.media.AudioProfile> audioProfiles) {
        Map<Integer, Set<Integer>> formatToChannelMasks = new HashMap<>();
        formatToChannelMasks.put(2, new HashSet<>(Ints.asList(12)));
        for (int i = 0; i < audioProfiles.size(); i++) {
            android.media.AudioProfile audioProfile = audioProfiles.get(i);
            if (audioProfile.getEncapsulationType() != 1) {
                int encoding = audioProfile.getFormat();
                if (Util.isEncodingLinearPcm(encoding) || ALL_SURROUND_ENCODINGS_AND_MAX_CHANNELS.containsKey(Integer.valueOf(encoding))) {
                    if (formatToChannelMasks.containsKey(Integer.valueOf(encoding))) {
                        ((Set) Assertions.checkNotNull(formatToChannelMasks.get(Integer.valueOf(encoding)))).addAll(Ints.asList(audioProfile.getChannelMasks()));
                    } else {
                        formatToChannelMasks.put(Integer.valueOf(encoding), new HashSet<>(Ints.asList(audioProfile.getChannelMasks())));
                    }
                }
            }
        }
        ImmutableList.Builder<AudioProfile> localAudioProfiles = ImmutableList.builder();
        for (Map.Entry<Integer, Set<Integer>> formatAndChannelMasks : formatToChannelMasks.entrySet()) {
            localAudioProfiles.add(new AudioProfile(formatAndChannelMasks.getKey().intValue(), formatAndChannelMasks.getValue()));
        }
        return localAudioProfiles.build();
    }

    private static ImmutableList<AudioProfile> getAudioProfiles(int[] supportedEncodings, int maxChannelCount) {
        ImmutableList.Builder<AudioProfile> audioProfiles = ImmutableList.builder();
        if (supportedEncodings == null) {
            supportedEncodings = new int[0];
        }
        for (int encoding : supportedEncodings) {
            audioProfiles.add(new AudioProfile(encoding, maxChannelCount));
        }
        return audioProfiles.build();
    }

    private static final class AudioProfile {
        public static final AudioProfile DEFAULT_AUDIO_PROFILE;
        private final ImmutableSet<Integer> channelMasks;
        public final int encoding;
        public final int maxChannelCount;

        static {
            AudioProfile audioProfile;
            if (Util.SDK_INT >= 33) {
                audioProfile = new AudioProfile(2, getAllChannelMasksForMaxChannelCount(10));
            } else {
                audioProfile = new AudioProfile(2, 10);
            }
            DEFAULT_AUDIO_PROFILE = audioProfile;
        }

        public AudioProfile(int encoding, Set<Integer> channelMasks) {
            this.encoding = encoding;
            this.channelMasks = ImmutableSet.copyOf((Collection) channelMasks);
            int maxChannelCount = 0;
            UnmodifiableIterator<Integer> it = this.channelMasks.iterator();
            while (it.hasNext()) {
                int channelMask = it.next().intValue();
                maxChannelCount = Math.max(maxChannelCount, Integer.bitCount(channelMask));
            }
            this.maxChannelCount = maxChannelCount;
        }

        public AudioProfile(int encoding, int maxChannelCount) {
            this.encoding = encoding;
            this.maxChannelCount = maxChannelCount;
            this.channelMasks = null;
        }

        public boolean supportsChannelCount(int channelCount) {
            if (this.channelMasks == null) {
                return channelCount <= this.maxChannelCount;
            }
            int channelMask = Util.getAudioTrackChannelConfig(channelCount);
            if (channelMask == 0) {
                return false;
            }
            return this.channelMasks.contains(Integer.valueOf(channelMask));
        }

        public int getMaxSupportedChannelCountForPassthrough(int sampleRate, AudioAttributes audioAttributes) {
            if (this.channelMasks != null) {
                return this.maxChannelCount;
            }
            if (Util.SDK_INT >= 29) {
                return Api29.getMaxSupportedChannelCountForPassthrough(this.encoding, sampleRate, audioAttributes);
            }
            return ((Integer) Assertions.checkNotNull(AudioCapabilities.ALL_SURROUND_ENCODINGS_AND_MAX_CHANNELS.getOrDefault(Integer.valueOf(this.encoding), 0))).intValue();
        }

        private static ImmutableSet<Integer> getAllChannelMasksForMaxChannelCount(int maxChannelCount) {
            ImmutableSet.Builder<Integer> allChannelMasks = new ImmutableSet.Builder<>();
            for (int i = 1; i <= maxChannelCount; i++) {
                allChannelMasks.add(Integer.valueOf(Util.getAudioTrackChannelConfig(i)));
            }
            return allChannelMasks.build();
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AudioProfile)) {
                return false;
            }
            AudioProfile audioProfile = (AudioProfile) other;
            return this.encoding == audioProfile.encoding && this.maxChannelCount == audioProfile.maxChannelCount && Util.areEqual(this.channelMasks, audioProfile.channelMasks);
        }

        public int hashCode() {
            int result = this.encoding;
            return (((result * 31) + this.maxChannelCount) * 31) + (this.channelMasks == null ? 0 : this.channelMasks.hashCode());
        }

        public String toString() {
            return "AudioProfile[format=" + this.encoding + ", maxChannelCount=" + this.maxChannelCount + ", channelMasks=" + this.channelMasks + "]";
        }
    }

    private static final class Api23 {
        private Api23() {
        }

        public static boolean isBluetoothConnected(AudioManager audioManager, AudioDeviceInfoApi23 currentDevice) {
            AudioDeviceInfo[] audioDeviceInfos;
            if (currentDevice == null) {
                audioDeviceInfos = ((AudioManager) Assertions.checkNotNull(audioManager)).getDevices(2);
            } else {
                audioDeviceInfos = new AudioDeviceInfo[]{currentDevice.audioDeviceInfo};
            }
            ImmutableSet<Integer> allBluetoothDeviceTypesSet = getAllBluetoothDeviceTypes();
            for (AudioDeviceInfo audioDeviceInfo : audioDeviceInfos) {
                if (allBluetoothDeviceTypesSet.contains(Integer.valueOf(audioDeviceInfo.getType()))) {
                    return true;
                }
            }
            return false;
        }

        private static ImmutableSet<Integer> getAllBluetoothDeviceTypes() {
            ImmutableSet.Builder<Integer> allBluetoothDeviceTypes = new ImmutableSet.Builder().add((Object[]) new Integer[]{8, 7});
            if (Util.SDK_INT >= 31) {
                allBluetoothDeviceTypes.add(26, 27);
            }
            if (Util.SDK_INT >= 33) {
                allBluetoothDeviceTypes.add(30);
            }
            return allBluetoothDeviceTypes.build();
        }
    }

    private static final class Api29 {
        private Api29() {
        }

        public static ImmutableList<Integer> getDirectPlaybackSupportedEncodings(AudioAttributes audioAttributes) {
            ImmutableList.Builder<Integer> supportedEncodingsListBuilder = ImmutableList.builder();
            UnmodifiableIterator<Integer> it = AudioCapabilities.ALL_SURROUND_ENCODINGS_AND_MAX_CHANNELS.keySet().iterator();
            while (it.hasNext()) {
                int encoding = it.next().intValue();
                if (Util.SDK_INT >= Util.getApiLevelThatAudioFormatIntroducedAudioEncoding(encoding) && AudioTrack.isDirectPlaybackSupported(new AudioFormat.Builder().setChannelMask(12).setEncoding(encoding).setSampleRate(48000).build(), audioAttributes.getAudioAttributesV21().audioAttributes)) {
                    supportedEncodingsListBuilder.add(Integer.valueOf(encoding));
                }
            }
            supportedEncodingsListBuilder.add(2);
            return supportedEncodingsListBuilder.build();
        }

        public static int getMaxSupportedChannelCountForPassthrough(int encoding, int sampleRate, AudioAttributes audioAttributes) {
            for (int channelCount = 10; channelCount > 0; channelCount--) {
                int channelConfig = Util.getAudioTrackChannelConfig(channelCount);
                if (channelConfig != 0) {
                    AudioFormat audioFormat = new AudioFormat.Builder().setEncoding(encoding).setSampleRate(sampleRate).setChannelMask(channelConfig).build();
                    if (AudioTrack.isDirectPlaybackSupported(audioFormat, audioAttributes.getAudioAttributesV21().audioAttributes)) {
                        return channelCount;
                    }
                }
            }
            return 0;
        }
    }

    private static final class Api33 {
        private Api33() {
        }

        public static AudioCapabilities getCapabilitiesInternalForDirectPlayback(AudioManager audioManager, AudioAttributes audioAttributes) {
            List<android.media.AudioProfile> directAudioProfiles = audioManager.getDirectProfilesForAttributes(audioAttributes.getAudioAttributesV21().audioAttributes);
            return new AudioCapabilities(AudioCapabilities.getAudioProfiles(directAudioProfiles));
        }

        public static AudioDeviceInfoApi23 getDefaultRoutedDeviceForAttributes(AudioManager audioManager, AudioAttributes audioAttributes) {
            try {
                List<AudioDeviceInfo> audioDevices = ((AudioManager) Assertions.checkNotNull(audioManager)).getAudioDevicesForAttributes(audioAttributes.getAudioAttributesV21().audioAttributes);
                if (audioDevices.isEmpty()) {
                    return null;
                }
                return new AudioDeviceInfoApi23(audioDevices.get(0));
            } catch (RuntimeException e) {
                return null;
            }
        }
    }
}
