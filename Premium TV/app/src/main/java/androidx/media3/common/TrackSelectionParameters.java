package androidx.media3.common;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Looper;
import android.view.accessibility.CaptioningManager;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/* JADX INFO: loaded from: classes.dex */
public class TrackSelectionParameters {
    protected static final int FIELD_CUSTOM_ID_BASE = 1000;
    public final AudioOffloadPreferences audioOffloadPreferences;
    public final ImmutableSet<Integer> disabledTrackTypes;
    public final boolean forceHighestSupportedBitrate;
    public final boolean forceLowestBitrate;
    public final int ignoredTextSelectionFlags;
    public final boolean isPrioritizeImageOverVideoEnabled;
    public final int maxAudioBitrate;
    public final int maxAudioChannelCount;
    public final int maxVideoBitrate;
    public final int maxVideoFrameRate;
    public final int maxVideoHeight;
    public final int maxVideoWidth;
    public final int minVideoBitrate;
    public final int minVideoFrameRate;
    public final int minVideoHeight;
    public final int minVideoWidth;
    public final ImmutableMap<TrackGroup, TrackSelectionOverride> overrides;
    public final ImmutableList<String> preferredAudioLanguages;
    public final ImmutableList<String> preferredAudioMimeTypes;
    public final int preferredAudioRoleFlags;
    public final ImmutableList<String> preferredTextLanguages;
    public final int preferredTextRoleFlags;
    public final ImmutableList<String> preferredVideoMimeTypes;
    public final int preferredVideoRoleFlags;
    public final boolean selectUndeterminedTextLanguage;
    public final int viewportHeight;
    public final boolean viewportOrientationMayChange;
    public final int viewportWidth;
    public static final TrackSelectionParameters DEFAULT_WITHOUT_CONTEXT = new Builder().build();

    @Deprecated
    public static final TrackSelectionParameters DEFAULT = DEFAULT_WITHOUT_CONTEXT;
    private static final String FIELD_PREFERRED_AUDIO_LANGUAGES = Util.intToStringMaxRadix(1);
    private static final String FIELD_PREFERRED_AUDIO_ROLE_FLAGS = Util.intToStringMaxRadix(2);
    private static final String FIELD_PREFERRED_TEXT_LANGUAGES = Util.intToStringMaxRadix(3);
    private static final String FIELD_PREFERRED_TEXT_ROLE_FLAGS = Util.intToStringMaxRadix(4);
    private static final String FIELD_SELECT_UNDETERMINED_TEXT_LANGUAGE = Util.intToStringMaxRadix(5);
    private static final String FIELD_MAX_VIDEO_WIDTH = Util.intToStringMaxRadix(6);
    private static final String FIELD_MAX_VIDEO_HEIGHT = Util.intToStringMaxRadix(7);
    private static final String FIELD_MAX_VIDEO_FRAMERATE = Util.intToStringMaxRadix(8);
    private static final String FIELD_MAX_VIDEO_BITRATE = Util.intToStringMaxRadix(9);
    private static final String FIELD_MIN_VIDEO_WIDTH = Util.intToStringMaxRadix(10);
    private static final String FIELD_MIN_VIDEO_HEIGHT = Util.intToStringMaxRadix(11);
    private static final String FIELD_MIN_VIDEO_FRAMERATE = Util.intToStringMaxRadix(12);
    private static final String FIELD_MIN_VIDEO_BITRATE = Util.intToStringMaxRadix(13);
    private static final String FIELD_VIEWPORT_WIDTH = Util.intToStringMaxRadix(14);
    private static final String FIELD_VIEWPORT_HEIGHT = Util.intToStringMaxRadix(15);
    private static final String FIELD_VIEWPORT_ORIENTATION_MAY_CHANGE = Util.intToStringMaxRadix(16);
    private static final String FIELD_PREFERRED_VIDEO_MIMETYPES = Util.intToStringMaxRadix(17);
    private static final String FIELD_MAX_AUDIO_CHANNEL_COUNT = Util.intToStringMaxRadix(18);
    private static final String FIELD_MAX_AUDIO_BITRATE = Util.intToStringMaxRadix(19);
    private static final String FIELD_PREFERRED_AUDIO_MIME_TYPES = Util.intToStringMaxRadix(20);
    private static final String FIELD_FORCE_LOWEST_BITRATE = Util.intToStringMaxRadix(21);
    private static final String FIELD_FORCE_HIGHEST_SUPPORTED_BITRATE = Util.intToStringMaxRadix(22);
    private static final String FIELD_SELECTION_OVERRIDES = Util.intToStringMaxRadix(23);
    private static final String FIELD_DISABLED_TRACK_TYPE = Util.intToStringMaxRadix(24);
    private static final String FIELD_PREFERRED_VIDEO_ROLE_FLAGS = Util.intToStringMaxRadix(25);
    private static final String FIELD_IGNORED_TEXT_SELECTION_FLAGS = Util.intToStringMaxRadix(26);
    private static final String FIELD_AUDIO_OFFLOAD_MODE_PREFERENCE = Util.intToStringMaxRadix(27);
    private static final String FIELD_IS_GAPLESS_SUPPORT_REQUIRED = Util.intToStringMaxRadix(28);
    private static final String FIELD_IS_SPEED_CHANGE_SUPPORT_REQUIRED = Util.intToStringMaxRadix(29);
    private static final String FIELD_AUDIO_OFFLOAD_PREFERENCES = Util.intToStringMaxRadix(30);
    private static final String FIELD_IS_PREFER_IMAGE_OVER_VIDEO_ENABLED = Util.intToStringMaxRadix(31);

    public static class Builder {
        private AudioOffloadPreferences audioOffloadPreferences;
        private HashSet<Integer> disabledTrackTypes;
        private boolean forceHighestSupportedBitrate;
        private boolean forceLowestBitrate;
        private int ignoredTextSelectionFlags;
        private boolean isPrioritizeImageOverVideoEnabled;
        private int maxAudioBitrate;
        private int maxAudioChannelCount;
        private int maxVideoBitrate;
        private int maxVideoFrameRate;
        private int maxVideoHeight;
        private int maxVideoWidth;
        private int minVideoBitrate;
        private int minVideoFrameRate;
        private int minVideoHeight;
        private int minVideoWidth;
        private HashMap<TrackGroup, TrackSelectionOverride> overrides;
        private ImmutableList<String> preferredAudioLanguages;
        private ImmutableList<String> preferredAudioMimeTypes;
        private int preferredAudioRoleFlags;
        private ImmutableList<String> preferredTextLanguages;
        private int preferredTextRoleFlags;
        private ImmutableList<String> preferredVideoMimeTypes;
        private int preferredVideoRoleFlags;
        private boolean selectUndeterminedTextLanguage;
        private int viewportHeight;
        private boolean viewportOrientationMayChange;
        private int viewportWidth;

        @Deprecated
        public Builder() {
            this.maxVideoWidth = Integer.MAX_VALUE;
            this.maxVideoHeight = Integer.MAX_VALUE;
            this.maxVideoFrameRate = Integer.MAX_VALUE;
            this.maxVideoBitrate = Integer.MAX_VALUE;
            this.viewportWidth = Integer.MAX_VALUE;
            this.viewportHeight = Integer.MAX_VALUE;
            this.viewportOrientationMayChange = true;
            this.preferredVideoMimeTypes = ImmutableList.of();
            this.preferredVideoRoleFlags = 0;
            this.preferredAudioLanguages = ImmutableList.of();
            this.preferredAudioRoleFlags = 0;
            this.maxAudioChannelCount = Integer.MAX_VALUE;
            this.maxAudioBitrate = Integer.MAX_VALUE;
            this.preferredAudioMimeTypes = ImmutableList.of();
            this.audioOffloadPreferences = AudioOffloadPreferences.DEFAULT;
            this.preferredTextLanguages = ImmutableList.of();
            this.preferredTextRoleFlags = 0;
            this.ignoredTextSelectionFlags = 0;
            this.selectUndeterminedTextLanguage = false;
            this.isPrioritizeImageOverVideoEnabled = false;
            this.forceLowestBitrate = false;
            this.forceHighestSupportedBitrate = false;
            this.overrides = new HashMap<>();
            this.disabledTrackTypes = new HashSet<>();
        }

        public Builder(Context context) {
            this();
            setPreferredTextLanguageAndRoleFlagsToCaptioningManagerSettings(context);
            setViewportSizeToPhysicalDisplaySize(context, true);
        }

        protected Builder(TrackSelectionParameters initialValues) {
            init(initialValues);
        }

        protected Builder(Bundle bundle) {
            List<TrackSelectionOverride> overrideList;
            this.maxVideoWidth = bundle.getInt(TrackSelectionParameters.FIELD_MAX_VIDEO_WIDTH, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.maxVideoWidth);
            this.maxVideoHeight = bundle.getInt(TrackSelectionParameters.FIELD_MAX_VIDEO_HEIGHT, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.maxVideoHeight);
            this.maxVideoFrameRate = bundle.getInt(TrackSelectionParameters.FIELD_MAX_VIDEO_FRAMERATE, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.maxVideoFrameRate);
            this.maxVideoBitrate = bundle.getInt(TrackSelectionParameters.FIELD_MAX_VIDEO_BITRATE, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.maxVideoBitrate);
            this.minVideoWidth = bundle.getInt(TrackSelectionParameters.FIELD_MIN_VIDEO_WIDTH, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.minVideoWidth);
            this.minVideoHeight = bundle.getInt(TrackSelectionParameters.FIELD_MIN_VIDEO_HEIGHT, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.minVideoHeight);
            this.minVideoFrameRate = bundle.getInt(TrackSelectionParameters.FIELD_MIN_VIDEO_FRAMERATE, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.minVideoFrameRate);
            this.minVideoBitrate = bundle.getInt(TrackSelectionParameters.FIELD_MIN_VIDEO_BITRATE, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.minVideoBitrate);
            this.viewportWidth = bundle.getInt(TrackSelectionParameters.FIELD_VIEWPORT_WIDTH, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.viewportWidth);
            this.viewportHeight = bundle.getInt(TrackSelectionParameters.FIELD_VIEWPORT_HEIGHT, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.viewportHeight);
            this.viewportOrientationMayChange = bundle.getBoolean(TrackSelectionParameters.FIELD_VIEWPORT_ORIENTATION_MAY_CHANGE, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.viewportOrientationMayChange);
            this.preferredVideoMimeTypes = ImmutableList.copyOf((String[]) MoreObjects.firstNonNull(bundle.getStringArray(TrackSelectionParameters.FIELD_PREFERRED_VIDEO_MIMETYPES), new String[0]));
            this.preferredVideoRoleFlags = bundle.getInt(TrackSelectionParameters.FIELD_PREFERRED_VIDEO_ROLE_FLAGS, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.preferredVideoRoleFlags);
            String[] preferredAudioLanguages1 = (String[]) MoreObjects.firstNonNull(bundle.getStringArray(TrackSelectionParameters.FIELD_PREFERRED_AUDIO_LANGUAGES), new String[0]);
            this.preferredAudioLanguages = normalizeLanguageCodes(preferredAudioLanguages1);
            this.preferredAudioRoleFlags = bundle.getInt(TrackSelectionParameters.FIELD_PREFERRED_AUDIO_ROLE_FLAGS, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.preferredAudioRoleFlags);
            this.maxAudioChannelCount = bundle.getInt(TrackSelectionParameters.FIELD_MAX_AUDIO_CHANNEL_COUNT, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.maxAudioChannelCount);
            this.maxAudioBitrate = bundle.getInt(TrackSelectionParameters.FIELD_MAX_AUDIO_BITRATE, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.maxAudioBitrate);
            this.preferredAudioMimeTypes = ImmutableList.copyOf((String[]) MoreObjects.firstNonNull(bundle.getStringArray(TrackSelectionParameters.FIELD_PREFERRED_AUDIO_MIME_TYPES), new String[0]));
            this.audioOffloadPreferences = getAudioOffloadPreferencesFromBundle(bundle);
            this.preferredTextLanguages = normalizeLanguageCodes((String[]) MoreObjects.firstNonNull(bundle.getStringArray(TrackSelectionParameters.FIELD_PREFERRED_TEXT_LANGUAGES), new String[0]));
            this.preferredTextRoleFlags = bundle.getInt(TrackSelectionParameters.FIELD_PREFERRED_TEXT_ROLE_FLAGS, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.preferredTextRoleFlags);
            this.ignoredTextSelectionFlags = bundle.getInt(TrackSelectionParameters.FIELD_IGNORED_TEXT_SELECTION_FLAGS, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.ignoredTextSelectionFlags);
            this.selectUndeterminedTextLanguage = bundle.getBoolean(TrackSelectionParameters.FIELD_SELECT_UNDETERMINED_TEXT_LANGUAGE, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.selectUndeterminedTextLanguage);
            this.isPrioritizeImageOverVideoEnabled = bundle.getBoolean(TrackSelectionParameters.FIELD_IS_PREFER_IMAGE_OVER_VIDEO_ENABLED, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.isPrioritizeImageOverVideoEnabled);
            this.forceLowestBitrate = bundle.getBoolean(TrackSelectionParameters.FIELD_FORCE_LOWEST_BITRATE, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.forceLowestBitrate);
            this.forceHighestSupportedBitrate = bundle.getBoolean(TrackSelectionParameters.FIELD_FORCE_HIGHEST_SUPPORTED_BITRATE, TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.forceHighestSupportedBitrate);
            List<Bundle> overrideBundleList = bundle.getParcelableArrayList(TrackSelectionParameters.FIELD_SELECTION_OVERRIDES);
            if (overrideBundleList == null) {
                overrideList = ImmutableList.of();
            } else {
                overrideList = BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.common.TrackSelectionParameters$Builder$$ExternalSyntheticLambda0
                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return TrackSelectionOverride.fromBundle((Bundle) obj);
                    }
                }, overrideBundleList);
            }
            this.overrides = new HashMap<>();
            for (int i = 0; i < overrideList.size(); i++) {
                TrackSelectionOverride override = overrideList.get(i);
                this.overrides.put(override.mediaTrackGroup, override);
            }
            int[] disabledTrackTypeArray = (int[]) MoreObjects.firstNonNull(bundle.getIntArray(TrackSelectionParameters.FIELD_DISABLED_TRACK_TYPE), new int[0]);
            this.disabledTrackTypes = new HashSet<>();
            for (int disabledTrackType : disabledTrackTypeArray) {
                this.disabledTrackTypes.add(Integer.valueOf(disabledTrackType));
            }
        }

        private static AudioOffloadPreferences getAudioOffloadPreferencesFromBundle(Bundle bundle) {
            Bundle audioOffloadPreferencesBundle = bundle.getBundle(TrackSelectionParameters.FIELD_AUDIO_OFFLOAD_PREFERENCES);
            if (audioOffloadPreferencesBundle != null) {
                return AudioOffloadPreferences.fromBundle(audioOffloadPreferencesBundle);
            }
            return new AudioOffloadPreferences.Builder().setAudioOffloadMode(bundle.getInt(TrackSelectionParameters.FIELD_AUDIO_OFFLOAD_MODE_PREFERENCE, AudioOffloadPreferences.DEFAULT.audioOffloadMode)).setIsGaplessSupportRequired(bundle.getBoolean(TrackSelectionParameters.FIELD_IS_GAPLESS_SUPPORT_REQUIRED, AudioOffloadPreferences.DEFAULT.isGaplessSupportRequired)).setIsSpeedChangeSupportRequired(bundle.getBoolean(TrackSelectionParameters.FIELD_IS_SPEED_CHANGE_SUPPORT_REQUIRED, AudioOffloadPreferences.DEFAULT.isSpeedChangeSupportRequired)).build();
        }

        @EnsuresNonNull({"preferredVideoMimeTypes", "preferredAudioLanguages", "preferredAudioMimeTypes", "audioOffloadPreferences", "preferredTextLanguages", "overrides", "disabledTrackTypes"})
        private void init(TrackSelectionParameters parameters) {
            this.maxVideoWidth = parameters.maxVideoWidth;
            this.maxVideoHeight = parameters.maxVideoHeight;
            this.maxVideoFrameRate = parameters.maxVideoFrameRate;
            this.maxVideoBitrate = parameters.maxVideoBitrate;
            this.minVideoWidth = parameters.minVideoWidth;
            this.minVideoHeight = parameters.minVideoHeight;
            this.minVideoFrameRate = parameters.minVideoFrameRate;
            this.minVideoBitrate = parameters.minVideoBitrate;
            this.viewportWidth = parameters.viewportWidth;
            this.viewportHeight = parameters.viewportHeight;
            this.viewportOrientationMayChange = parameters.viewportOrientationMayChange;
            this.preferredVideoMimeTypes = parameters.preferredVideoMimeTypes;
            this.preferredVideoRoleFlags = parameters.preferredVideoRoleFlags;
            this.preferredAudioLanguages = parameters.preferredAudioLanguages;
            this.preferredAudioRoleFlags = parameters.preferredAudioRoleFlags;
            this.maxAudioChannelCount = parameters.maxAudioChannelCount;
            this.maxAudioBitrate = parameters.maxAudioBitrate;
            this.preferredAudioMimeTypes = parameters.preferredAudioMimeTypes;
            this.audioOffloadPreferences = parameters.audioOffloadPreferences;
            this.preferredTextLanguages = parameters.preferredTextLanguages;
            this.preferredTextRoleFlags = parameters.preferredTextRoleFlags;
            this.ignoredTextSelectionFlags = parameters.ignoredTextSelectionFlags;
            this.selectUndeterminedTextLanguage = parameters.selectUndeterminedTextLanguage;
            this.isPrioritizeImageOverVideoEnabled = parameters.isPrioritizeImageOverVideoEnabled;
            this.forceLowestBitrate = parameters.forceLowestBitrate;
            this.forceHighestSupportedBitrate = parameters.forceHighestSupportedBitrate;
            this.disabledTrackTypes = new HashSet<>(parameters.disabledTrackTypes);
            this.overrides = new HashMap<>(parameters.overrides);
        }

        protected Builder set(TrackSelectionParameters parameters) {
            init(parameters);
            return this;
        }

        public Builder setMaxVideoSizeSd() {
            return setMaxVideoSize(AdaptiveTrackSelection.DEFAULT_MAX_WIDTH_TO_DISCARD, AdaptiveTrackSelection.DEFAULT_MAX_HEIGHT_TO_DISCARD);
        }

        public Builder clearVideoSizeConstraints() {
            return setMaxVideoSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        public Builder setMaxVideoSize(int maxVideoWidth, int maxVideoHeight) {
            this.maxVideoWidth = maxVideoWidth;
            this.maxVideoHeight = maxVideoHeight;
            return this;
        }

        public Builder setMaxVideoFrameRate(int maxVideoFrameRate) {
            this.maxVideoFrameRate = maxVideoFrameRate;
            return this;
        }

        public Builder setMaxVideoBitrate(int maxVideoBitrate) {
            this.maxVideoBitrate = maxVideoBitrate;
            return this;
        }

        public Builder setMinVideoSize(int minVideoWidth, int minVideoHeight) {
            this.minVideoWidth = minVideoWidth;
            this.minVideoHeight = minVideoHeight;
            return this;
        }

        public Builder setMinVideoFrameRate(int minVideoFrameRate) {
            this.minVideoFrameRate = minVideoFrameRate;
            return this;
        }

        public Builder setMinVideoBitrate(int minVideoBitrate) {
            this.minVideoBitrate = minVideoBitrate;
            return this;
        }

        public Builder setViewportSizeToPhysicalDisplaySize(Context context, boolean viewportOrientationMayChange) {
            Point viewportSize = Util.getCurrentDisplayModeSize(context);
            return setViewportSize(viewportSize.x, viewportSize.y, viewportOrientationMayChange);
        }

        public Builder clearViewportSizeConstraints() {
            return setViewportSize(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        }

        public Builder setViewportSize(int viewportWidth, int viewportHeight, boolean viewportOrientationMayChange) {
            this.viewportWidth = viewportWidth;
            this.viewportHeight = viewportHeight;
            this.viewportOrientationMayChange = viewportOrientationMayChange;
            return this;
        }

        public Builder setPreferredVideoMimeType(String mimeType) {
            return mimeType == null ? setPreferredVideoMimeTypes(new String[0]) : setPreferredVideoMimeTypes(mimeType);
        }

        public Builder setPreferredVideoMimeTypes(String... mimeTypes) {
            this.preferredVideoMimeTypes = ImmutableList.copyOf(mimeTypes);
            return this;
        }

        public Builder setPreferredVideoRoleFlags(int preferredVideoRoleFlags) {
            this.preferredVideoRoleFlags = preferredVideoRoleFlags;
            return this;
        }

        public Builder setPreferredAudioLanguage(String preferredAudioLanguage) {
            if (preferredAudioLanguage == null) {
                return setPreferredAudioLanguages(new String[0]);
            }
            return setPreferredAudioLanguages(preferredAudioLanguage);
        }

        public Builder setPreferredAudioLanguages(String... preferredAudioLanguages) {
            this.preferredAudioLanguages = normalizeLanguageCodes(preferredAudioLanguages);
            return this;
        }

        public Builder setPreferredAudioRoleFlags(int preferredAudioRoleFlags) {
            this.preferredAudioRoleFlags = preferredAudioRoleFlags;
            return this;
        }

        public Builder setMaxAudioChannelCount(int maxAudioChannelCount) {
            this.maxAudioChannelCount = maxAudioChannelCount;
            return this;
        }

        public Builder setMaxAudioBitrate(int maxAudioBitrate) {
            this.maxAudioBitrate = maxAudioBitrate;
            return this;
        }

        public Builder setPreferredAudioMimeType(String mimeType) {
            return mimeType == null ? setPreferredAudioMimeTypes(new String[0]) : setPreferredAudioMimeTypes(mimeType);
        }

        public Builder setPreferredAudioMimeTypes(String... mimeTypes) {
            this.preferredAudioMimeTypes = ImmutableList.copyOf(mimeTypes);
            return this;
        }

        public Builder setAudioOffloadPreferences(AudioOffloadPreferences audioOffloadPreferences) {
            this.audioOffloadPreferences = audioOffloadPreferences;
            return this;
        }

        public Builder setPreferredTextLanguageAndRoleFlagsToCaptioningManagerSettings(Context context) {
            CaptioningManager captioningManager;
            if ((Util.SDK_INT < 23 && Looper.myLooper() == null) || (captioningManager = (CaptioningManager) context.getSystemService("captioning")) == null || !captioningManager.isEnabled()) {
                return this;
            }
            this.preferredTextRoleFlags = 1088;
            Locale preferredLocale = captioningManager.getLocale();
            if (preferredLocale != null) {
                this.preferredTextLanguages = ImmutableList.of(Util.getLocaleLanguageTag(preferredLocale));
            }
            return this;
        }

        public Builder setPreferredTextLanguage(String preferredTextLanguage) {
            if (preferredTextLanguage == null) {
                return setPreferredTextLanguages(new String[0]);
            }
            return setPreferredTextLanguages(preferredTextLanguage);
        }

        public Builder setPreferredTextLanguages(String... preferredTextLanguages) {
            this.preferredTextLanguages = normalizeLanguageCodes(preferredTextLanguages);
            return this;
        }

        public Builder setPreferredTextRoleFlags(int preferredTextRoleFlags) {
            this.preferredTextRoleFlags = preferredTextRoleFlags;
            return this;
        }

        public Builder setIgnoredTextSelectionFlags(int ignoredTextSelectionFlags) {
            this.ignoredTextSelectionFlags = ignoredTextSelectionFlags;
            return this;
        }

        public Builder setSelectUndeterminedTextLanguage(boolean selectUndeterminedTextLanguage) {
            this.selectUndeterminedTextLanguage = selectUndeterminedTextLanguage;
            return this;
        }

        public Builder setPrioritizeImageOverVideoEnabled(boolean isPrioritizeImageOverVideoEnabled) {
            this.isPrioritizeImageOverVideoEnabled = isPrioritizeImageOverVideoEnabled;
            return this;
        }

        public Builder setForceLowestBitrate(boolean forceLowestBitrate) {
            this.forceLowestBitrate = forceLowestBitrate;
            return this;
        }

        public Builder setForceHighestSupportedBitrate(boolean forceHighestSupportedBitrate) {
            this.forceHighestSupportedBitrate = forceHighestSupportedBitrate;
            return this;
        }

        public Builder addOverride(TrackSelectionOverride override) {
            this.overrides.put(override.mediaTrackGroup, override);
            return this;
        }

        public Builder setOverrideForType(TrackSelectionOverride override) {
            clearOverridesOfType(override.getType());
            this.overrides.put(override.mediaTrackGroup, override);
            return this;
        }

        public Builder clearOverride(TrackGroup mediaTrackGroup) {
            this.overrides.remove(mediaTrackGroup);
            return this;
        }

        public Builder clearOverridesOfType(int trackType) {
            Iterator<TrackSelectionOverride> it = this.overrides.values().iterator();
            while (it.hasNext()) {
                TrackSelectionOverride override = it.next();
                if (override.getType() == trackType) {
                    it.remove();
                }
            }
            return this;
        }

        public Builder clearOverrides() {
            this.overrides.clear();
            return this;
        }

        @Deprecated
        public Builder setDisabledTrackTypes(Set<Integer> disabledTrackTypes) {
            this.disabledTrackTypes.clear();
            this.disabledTrackTypes.addAll(disabledTrackTypes);
            return this;
        }

        public Builder setTrackTypeDisabled(int trackType, boolean disabled) {
            HashSet<Integer> hashSet = this.disabledTrackTypes;
            if (disabled) {
                hashSet.add(Integer.valueOf(trackType));
            } else {
                hashSet.remove(Integer.valueOf(trackType));
            }
            return this;
        }

        public TrackSelectionParameters build() {
            return new TrackSelectionParameters(this);
        }

        private static ImmutableList<String> normalizeLanguageCodes(String[] preferredTextLanguages) {
            ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
            for (String language : (String[]) Assertions.checkNotNull(preferredTextLanguages)) {
                listBuilder.add(Util.normalizeLanguageCode((String) Assertions.checkNotNull(language)));
            }
            return listBuilder.build();
        }
    }

    public static final class AudioOffloadPreferences {
        public static final int AUDIO_OFFLOAD_MODE_DISABLED = 0;
        public static final int AUDIO_OFFLOAD_MODE_ENABLED = 1;
        public static final int AUDIO_OFFLOAD_MODE_REQUIRED = 2;
        public static final AudioOffloadPreferences DEFAULT = new Builder().build();
        private static final String FIELD_AUDIO_OFFLOAD_MODE_PREFERENCE = Util.intToStringMaxRadix(1);
        private static final String FIELD_IS_GAPLESS_SUPPORT_REQUIRED = Util.intToStringMaxRadix(2);
        private static final String FIELD_IS_SPEED_CHANGE_SUPPORT_REQUIRED = Util.intToStringMaxRadix(3);
        public final int audioOffloadMode;
        public final boolean isGaplessSupportRequired;
        public final boolean isSpeedChangeSupportRequired;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface AudioOffloadMode {
        }

        public static final class Builder {
            private int audioOffloadMode = 0;
            private boolean isGaplessSupportRequired = false;
            private boolean isSpeedChangeSupportRequired = false;

            public Builder setAudioOffloadMode(int audioOffloadMode) {
                this.audioOffloadMode = audioOffloadMode;
                return this;
            }

            public Builder setIsGaplessSupportRequired(boolean isGaplessSupportRequired) {
                this.isGaplessSupportRequired = isGaplessSupportRequired;
                return this;
            }

            public Builder setIsSpeedChangeSupportRequired(boolean isSpeedChangeSupportRequired) {
                this.isSpeedChangeSupportRequired = isSpeedChangeSupportRequired;
                return this;
            }

            public AudioOffloadPreferences build() {
                return new AudioOffloadPreferences(this);
            }
        }

        private AudioOffloadPreferences(Builder builder) {
            this.audioOffloadMode = builder.audioOffloadMode;
            this.isGaplessSupportRequired = builder.isGaplessSupportRequired;
            this.isSpeedChangeSupportRequired = builder.isSpeedChangeSupportRequired;
        }

        public Builder buildUpon() {
            return new Builder().setAudioOffloadMode(this.audioOffloadMode).setIsGaplessSupportRequired(this.isGaplessSupportRequired).setIsSpeedChangeSupportRequired(this.isSpeedChangeSupportRequired);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            AudioOffloadPreferences other = (AudioOffloadPreferences) obj;
            if (this.audioOffloadMode == other.audioOffloadMode && this.isGaplessSupportRequired == other.isGaplessSupportRequired && this.isSpeedChangeSupportRequired == other.isSpeedChangeSupportRequired) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (((((1 * 31) + this.audioOffloadMode) * 31) + (this.isGaplessSupportRequired ? 1 : 0)) * 31) + (this.isSpeedChangeSupportRequired ? 1 : 0);
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(FIELD_AUDIO_OFFLOAD_MODE_PREFERENCE, this.audioOffloadMode);
            bundle.putBoolean(FIELD_IS_GAPLESS_SUPPORT_REQUIRED, this.isGaplessSupportRequired);
            bundle.putBoolean(FIELD_IS_SPEED_CHANGE_SUPPORT_REQUIRED, this.isSpeedChangeSupportRequired);
            return bundle;
        }

        public static AudioOffloadPreferences fromBundle(Bundle bundle) {
            return new Builder().setAudioOffloadMode(bundle.getInt(FIELD_AUDIO_OFFLOAD_MODE_PREFERENCE, DEFAULT.audioOffloadMode)).setIsGaplessSupportRequired(bundle.getBoolean(FIELD_IS_GAPLESS_SUPPORT_REQUIRED, DEFAULT.isGaplessSupportRequired)).setIsSpeedChangeSupportRequired(bundle.getBoolean(FIELD_IS_SPEED_CHANGE_SUPPORT_REQUIRED, DEFAULT.isSpeedChangeSupportRequired)).build();
        }
    }

    public static TrackSelectionParameters getDefaults(Context context) {
        return new Builder(context).build();
    }

    protected TrackSelectionParameters(Builder builder) {
        this.maxVideoWidth = builder.maxVideoWidth;
        this.maxVideoHeight = builder.maxVideoHeight;
        this.maxVideoFrameRate = builder.maxVideoFrameRate;
        this.maxVideoBitrate = builder.maxVideoBitrate;
        this.minVideoWidth = builder.minVideoWidth;
        this.minVideoHeight = builder.minVideoHeight;
        this.minVideoFrameRate = builder.minVideoFrameRate;
        this.minVideoBitrate = builder.minVideoBitrate;
        this.viewportWidth = builder.viewportWidth;
        this.viewportHeight = builder.viewportHeight;
        this.viewportOrientationMayChange = builder.viewportOrientationMayChange;
        this.preferredVideoMimeTypes = builder.preferredVideoMimeTypes;
        this.preferredVideoRoleFlags = builder.preferredVideoRoleFlags;
        this.preferredAudioLanguages = builder.preferredAudioLanguages;
        this.preferredAudioRoleFlags = builder.preferredAudioRoleFlags;
        this.maxAudioChannelCount = builder.maxAudioChannelCount;
        this.maxAudioBitrate = builder.maxAudioBitrate;
        this.preferredAudioMimeTypes = builder.preferredAudioMimeTypes;
        this.audioOffloadPreferences = builder.audioOffloadPreferences;
        this.preferredTextLanguages = builder.preferredTextLanguages;
        this.preferredTextRoleFlags = builder.preferredTextRoleFlags;
        this.ignoredTextSelectionFlags = builder.ignoredTextSelectionFlags;
        this.selectUndeterminedTextLanguage = builder.selectUndeterminedTextLanguage;
        this.isPrioritizeImageOverVideoEnabled = builder.isPrioritizeImageOverVideoEnabled;
        this.forceLowestBitrate = builder.forceLowestBitrate;
        this.forceHighestSupportedBitrate = builder.forceHighestSupportedBitrate;
        this.overrides = ImmutableMap.copyOf((Map) builder.overrides);
        this.disabledTrackTypes = ImmutableSet.copyOf((Collection) builder.disabledTrackTypes);
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TrackSelectionParameters other = (TrackSelectionParameters) obj;
        if (this.maxVideoWidth == other.maxVideoWidth && this.maxVideoHeight == other.maxVideoHeight && this.maxVideoFrameRate == other.maxVideoFrameRate && this.maxVideoBitrate == other.maxVideoBitrate && this.minVideoWidth == other.minVideoWidth && this.minVideoHeight == other.minVideoHeight && this.minVideoFrameRate == other.minVideoFrameRate && this.minVideoBitrate == other.minVideoBitrate && this.viewportOrientationMayChange == other.viewportOrientationMayChange && this.viewportWidth == other.viewportWidth && this.viewportHeight == other.viewportHeight && this.preferredVideoMimeTypes.equals(other.preferredVideoMimeTypes) && this.preferredVideoRoleFlags == other.preferredVideoRoleFlags && this.preferredAudioLanguages.equals(other.preferredAudioLanguages) && this.preferredAudioRoleFlags == other.preferredAudioRoleFlags && this.maxAudioChannelCount == other.maxAudioChannelCount && this.maxAudioBitrate == other.maxAudioBitrate && this.preferredAudioMimeTypes.equals(other.preferredAudioMimeTypes) && this.audioOffloadPreferences.equals(other.audioOffloadPreferences) && this.preferredTextLanguages.equals(other.preferredTextLanguages) && this.preferredTextRoleFlags == other.preferredTextRoleFlags && this.ignoredTextSelectionFlags == other.ignoredTextSelectionFlags && this.selectUndeterminedTextLanguage == other.selectUndeterminedTextLanguage && this.isPrioritizeImageOverVideoEnabled == other.isPrioritizeImageOverVideoEnabled && this.forceLowestBitrate == other.forceLowestBitrate && this.forceHighestSupportedBitrate == other.forceHighestSupportedBitrate && this.overrides.equals(other.overrides) && this.disabledTrackTypes.equals(other.disabledTrackTypes)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (((((((((((((((((((((((((((((((((((((((((((((((((((((((1 * 31) + this.maxVideoWidth) * 31) + this.maxVideoHeight) * 31) + this.maxVideoFrameRate) * 31) + this.maxVideoBitrate) * 31) + this.minVideoWidth) * 31) + this.minVideoHeight) * 31) + this.minVideoFrameRate) * 31) + this.minVideoBitrate) * 31) + (this.viewportOrientationMayChange ? 1 : 0)) * 31) + this.viewportWidth) * 31) + this.viewportHeight) * 31) + this.preferredVideoMimeTypes.hashCode()) * 31) + this.preferredVideoRoleFlags) * 31) + this.preferredAudioLanguages.hashCode()) * 31) + this.preferredAudioRoleFlags) * 31) + this.maxAudioChannelCount) * 31) + this.maxAudioBitrate) * 31) + this.preferredAudioMimeTypes.hashCode()) * 31) + this.audioOffloadPreferences.hashCode()) * 31) + this.preferredTextLanguages.hashCode()) * 31) + this.preferredTextRoleFlags) * 31) + this.ignoredTextSelectionFlags) * 31) + (this.selectUndeterminedTextLanguage ? 1 : 0)) * 31) + (this.isPrioritizeImageOverVideoEnabled ? 1 : 0)) * 31) + (this.forceLowestBitrate ? 1 : 0)) * 31) + (this.forceHighestSupportedBitrate ? 1 : 0)) * 31) + this.overrides.hashCode()) * 31) + this.disabledTrackTypes.hashCode();
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(FIELD_MAX_VIDEO_WIDTH, this.maxVideoWidth);
        bundle.putInt(FIELD_MAX_VIDEO_HEIGHT, this.maxVideoHeight);
        bundle.putInt(FIELD_MAX_VIDEO_FRAMERATE, this.maxVideoFrameRate);
        bundle.putInt(FIELD_MAX_VIDEO_BITRATE, this.maxVideoBitrate);
        bundle.putInt(FIELD_MIN_VIDEO_WIDTH, this.minVideoWidth);
        bundle.putInt(FIELD_MIN_VIDEO_HEIGHT, this.minVideoHeight);
        bundle.putInt(FIELD_MIN_VIDEO_FRAMERATE, this.minVideoFrameRate);
        bundle.putInt(FIELD_MIN_VIDEO_BITRATE, this.minVideoBitrate);
        bundle.putInt(FIELD_VIEWPORT_WIDTH, this.viewportWidth);
        bundle.putInt(FIELD_VIEWPORT_HEIGHT, this.viewportHeight);
        bundle.putBoolean(FIELD_VIEWPORT_ORIENTATION_MAY_CHANGE, this.viewportOrientationMayChange);
        bundle.putStringArray(FIELD_PREFERRED_VIDEO_MIMETYPES, (String[]) this.preferredVideoMimeTypes.toArray(new String[0]));
        bundle.putInt(FIELD_PREFERRED_VIDEO_ROLE_FLAGS, this.preferredVideoRoleFlags);
        bundle.putStringArray(FIELD_PREFERRED_AUDIO_LANGUAGES, (String[]) this.preferredAudioLanguages.toArray(new String[0]));
        bundle.putInt(FIELD_PREFERRED_AUDIO_ROLE_FLAGS, this.preferredAudioRoleFlags);
        bundle.putInt(FIELD_MAX_AUDIO_CHANNEL_COUNT, this.maxAudioChannelCount);
        bundle.putInt(FIELD_MAX_AUDIO_BITRATE, this.maxAudioBitrate);
        bundle.putStringArray(FIELD_PREFERRED_AUDIO_MIME_TYPES, (String[]) this.preferredAudioMimeTypes.toArray(new String[0]));
        bundle.putStringArray(FIELD_PREFERRED_TEXT_LANGUAGES, (String[]) this.preferredTextLanguages.toArray(new String[0]));
        bundle.putInt(FIELD_PREFERRED_TEXT_ROLE_FLAGS, this.preferredTextRoleFlags);
        bundle.putInt(FIELD_IGNORED_TEXT_SELECTION_FLAGS, this.ignoredTextSelectionFlags);
        bundle.putBoolean(FIELD_SELECT_UNDETERMINED_TEXT_LANGUAGE, this.selectUndeterminedTextLanguage);
        bundle.putInt(FIELD_AUDIO_OFFLOAD_MODE_PREFERENCE, this.audioOffloadPreferences.audioOffloadMode);
        bundle.putBoolean(FIELD_IS_GAPLESS_SUPPORT_REQUIRED, this.audioOffloadPreferences.isGaplessSupportRequired);
        bundle.putBoolean(FIELD_IS_SPEED_CHANGE_SUPPORT_REQUIRED, this.audioOffloadPreferences.isSpeedChangeSupportRequired);
        bundle.putBundle(FIELD_AUDIO_OFFLOAD_PREFERENCES, this.audioOffloadPreferences.toBundle());
        bundle.putBoolean(FIELD_IS_PREFER_IMAGE_OVER_VIDEO_ENABLED, this.isPrioritizeImageOverVideoEnabled);
        bundle.putBoolean(FIELD_FORCE_LOWEST_BITRATE, this.forceLowestBitrate);
        bundle.putBoolean(FIELD_FORCE_HIGHEST_SUPPORTED_BITRATE, this.forceHighestSupportedBitrate);
        bundle.putParcelableArrayList(FIELD_SELECTION_OVERRIDES, BundleCollectionUtil.toBundleArrayList(this.overrides.values(), new Function() { // from class: androidx.media3.common.TrackSelectionParameters$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return ((TrackSelectionOverride) obj).toBundle();
            }
        }));
        bundle.putIntArray(FIELD_DISABLED_TRACK_TYPE, Ints.toArray(this.disabledTrackTypes));
        return bundle;
    }

    public static TrackSelectionParameters fromBundle(Bundle bundle) {
        return new Builder(bundle).build();
    }
}
