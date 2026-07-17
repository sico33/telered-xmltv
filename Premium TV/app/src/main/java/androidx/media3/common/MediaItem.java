package androidx.media3.common;

import android.net.Uri;
import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Util;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public final class MediaItem {
    public static final String DEFAULT_MEDIA_ID = "";
    public final ClippingConfiguration clippingConfiguration;

    @Deprecated
    public final ClippingProperties clippingProperties;
    public final LiveConfiguration liveConfiguration;
    public final LocalConfiguration localConfiguration;
    public final String mediaId;
    public final MediaMetadata mediaMetadata;

    @Deprecated
    public final LocalConfiguration playbackProperties;
    public final RequestMetadata requestMetadata;
    public static final MediaItem EMPTY = new Builder().build();
    private static final String FIELD_MEDIA_ID = Util.intToStringMaxRadix(0);
    private static final String FIELD_LIVE_CONFIGURATION = Util.intToStringMaxRadix(1);
    private static final String FIELD_MEDIA_METADATA = Util.intToStringMaxRadix(2);
    private static final String FIELD_CLIPPING_PROPERTIES = Util.intToStringMaxRadix(3);
    private static final String FIELD_REQUEST_METADATA = Util.intToStringMaxRadix(4);
    private static final String FIELD_LOCAL_CONFIGURATION = Util.intToStringMaxRadix(5);

    public static MediaItem fromUri(String uri) {
        return new Builder().setUri(uri).build();
    }

    public static MediaItem fromUri(Uri uri) {
        return new Builder().setUri(uri).build();
    }

    public static final class Builder {
        private AdsConfiguration adsConfiguration;
        private ClippingConfiguration.Builder clippingConfiguration;
        private String customCacheKey;
        private DrmConfiguration.Builder drmConfiguration;
        private long imageDurationMs;
        private LiveConfiguration.Builder liveConfiguration;
        private String mediaId;
        private MediaMetadata mediaMetadata;
        private String mimeType;
        private RequestMetadata requestMetadata;
        private List<StreamKey> streamKeys;
        private ImmutableList<SubtitleConfiguration> subtitleConfigurations;
        private Object tag;
        private Uri uri;

        public Builder() {
            this.clippingConfiguration = new ClippingConfiguration.Builder();
            this.drmConfiguration = new DrmConfiguration.Builder();
            this.streamKeys = Collections.emptyList();
            this.subtitleConfigurations = ImmutableList.of();
            this.liveConfiguration = new LiveConfiguration.Builder();
            this.requestMetadata = RequestMetadata.EMPTY;
            this.imageDurationMs = C.TIME_UNSET;
        }

        private Builder(MediaItem mediaItem) {
            DrmConfiguration.Builder builder;
            this();
            this.clippingConfiguration = mediaItem.clippingConfiguration.buildUpon();
            this.mediaId = mediaItem.mediaId;
            this.mediaMetadata = mediaItem.mediaMetadata;
            this.liveConfiguration = mediaItem.liveConfiguration.buildUpon();
            this.requestMetadata = mediaItem.requestMetadata;
            LocalConfiguration localConfiguration = mediaItem.localConfiguration;
            if (localConfiguration != null) {
                this.customCacheKey = localConfiguration.customCacheKey;
                this.mimeType = localConfiguration.mimeType;
                this.uri = localConfiguration.uri;
                this.streamKeys = localConfiguration.streamKeys;
                this.subtitleConfigurations = localConfiguration.subtitleConfigurations;
                this.tag = localConfiguration.tag;
                if (localConfiguration.drmConfiguration != null) {
                    builder = localConfiguration.drmConfiguration.buildUpon();
                } else {
                    builder = new DrmConfiguration.Builder();
                }
                this.drmConfiguration = builder;
                this.adsConfiguration = localConfiguration.adsConfiguration;
                this.imageDurationMs = localConfiguration.imageDurationMs;
            }
        }

        public Builder setMediaId(String mediaId) {
            this.mediaId = (String) Assertions.checkNotNull(mediaId);
            return this;
        }

        public Builder setUri(String uri) {
            return setUri(uri == null ? null : Uri.parse(uri));
        }

        public Builder setUri(Uri uri) {
            this.uri = uri;
            return this;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder setClippingConfiguration(ClippingConfiguration clippingConfiguration) {
            this.clippingConfiguration = clippingConfiguration.buildUpon();
            return this;
        }

        @Deprecated
        public Builder setClipStartPositionMs(long startPositionMs) {
            this.clippingConfiguration.setStartPositionMs(startPositionMs);
            return this;
        }

        @Deprecated
        public Builder setClipEndPositionMs(long endPositionMs) {
            this.clippingConfiguration.setEndPositionMs(endPositionMs);
            return this;
        }

        @Deprecated
        public Builder setClipRelativeToLiveWindow(boolean relativeToLiveWindow) {
            this.clippingConfiguration.setRelativeToLiveWindow(relativeToLiveWindow);
            return this;
        }

        @Deprecated
        public Builder setClipRelativeToDefaultPosition(boolean relativeToDefaultPosition) {
            this.clippingConfiguration.setRelativeToDefaultPosition(relativeToDefaultPosition);
            return this;
        }

        @Deprecated
        public Builder setClipStartsAtKeyFrame(boolean startsAtKeyFrame) {
            this.clippingConfiguration.setStartsAtKeyFrame(startsAtKeyFrame);
            return this;
        }

        public Builder setDrmConfiguration(DrmConfiguration drmConfiguration) {
            this.drmConfiguration = drmConfiguration != null ? drmConfiguration.buildUpon() : new DrmConfiguration.Builder();
            return this;
        }

        @Deprecated
        public Builder setDrmLicenseUri(Uri licenseUri) {
            this.drmConfiguration.setLicenseUri(licenseUri);
            return this;
        }

        @Deprecated
        public Builder setDrmLicenseUri(String licenseUri) {
            this.drmConfiguration.setLicenseUri(licenseUri);
            return this;
        }

        @Deprecated
        public Builder setDrmLicenseRequestHeaders(Map<String, String> licenseRequestHeaders) {
            this.drmConfiguration.setLicenseRequestHeaders(licenseRequestHeaders != null ? licenseRequestHeaders : ImmutableMap.of());
            return this;
        }

        @Deprecated
        public Builder setDrmUuid(UUID uuid) {
            this.drmConfiguration.setNullableScheme(uuid);
            return this;
        }

        @Deprecated
        public Builder setDrmMultiSession(boolean multiSession) {
            this.drmConfiguration.setMultiSession(multiSession);
            return this;
        }

        @Deprecated
        public Builder setDrmForceDefaultLicenseUri(boolean forceDefaultLicenseUri) {
            this.drmConfiguration.setForceDefaultLicenseUri(forceDefaultLicenseUri);
            return this;
        }

        @Deprecated
        public Builder setDrmPlayClearContentWithoutKey(boolean playClearContentWithoutKey) {
            this.drmConfiguration.setPlayClearContentWithoutKey(playClearContentWithoutKey);
            return this;
        }

        @Deprecated
        public Builder setDrmSessionForClearPeriods(boolean sessionForClearPeriods) {
            this.drmConfiguration.setForceSessionsForAudioAndVideoTracks(sessionForClearPeriods);
            return this;
        }

        @Deprecated
        public Builder setDrmSessionForClearTypes(List<Integer> sessionForClearTypes) {
            this.drmConfiguration.setForcedSessionTrackTypes(sessionForClearTypes != null ? sessionForClearTypes : ImmutableList.of());
            return this;
        }

        @Deprecated
        public Builder setDrmKeySetId(byte[] keySetId) {
            this.drmConfiguration.setKeySetId(keySetId);
            return this;
        }

        public Builder setStreamKeys(List<StreamKey> streamKeys) {
            List<StreamKey> listEmptyList;
            if (streamKeys != null && !streamKeys.isEmpty()) {
                listEmptyList = Collections.unmodifiableList(new ArrayList(streamKeys));
            } else {
                listEmptyList = Collections.emptyList();
            }
            this.streamKeys = listEmptyList;
            return this;
        }

        public Builder setCustomCacheKey(String customCacheKey) {
            this.customCacheKey = customCacheKey;
            return this;
        }

        @Deprecated
        public Builder setSubtitles(List<Subtitle> subtitles) {
            this.subtitleConfigurations = subtitles != null ? ImmutableList.copyOf((Collection) subtitles) : ImmutableList.of();
            return this;
        }

        public Builder setSubtitleConfigurations(List<SubtitleConfiguration> subtitleConfigurations) {
            this.subtitleConfigurations = ImmutableList.copyOf((Collection) subtitleConfigurations);
            return this;
        }

        public Builder setAdsConfiguration(AdsConfiguration adsConfiguration) {
            this.adsConfiguration = adsConfiguration;
            return this;
        }

        @Deprecated
        public Builder setAdTagUri(String adTagUri) {
            return setAdTagUri(adTagUri != null ? Uri.parse(adTagUri) : null);
        }

        @Deprecated
        public Builder setAdTagUri(Uri adTagUri) {
            return setAdTagUri(adTagUri, null);
        }

        @Deprecated
        public Builder setAdTagUri(Uri adTagUri, Object adsId) {
            this.adsConfiguration = adTagUri != null ? new AdsConfiguration.Builder(adTagUri).setAdsId(adsId).build() : null;
            return this;
        }

        public Builder setLiveConfiguration(LiveConfiguration liveConfiguration) {
            this.liveConfiguration = liveConfiguration.buildUpon();
            return this;
        }

        @Deprecated
        public Builder setLiveTargetOffsetMs(long liveTargetOffsetMs) {
            this.liveConfiguration.setTargetOffsetMs(liveTargetOffsetMs);
            return this;
        }

        @Deprecated
        public Builder setLiveMinOffsetMs(long liveMinOffsetMs) {
            this.liveConfiguration.setMinOffsetMs(liveMinOffsetMs);
            return this;
        }

        @Deprecated
        public Builder setLiveMaxOffsetMs(long liveMaxOffsetMs) {
            this.liveConfiguration.setMaxOffsetMs(liveMaxOffsetMs);
            return this;
        }

        @Deprecated
        public Builder setLiveMinPlaybackSpeed(float minPlaybackSpeed) {
            this.liveConfiguration.setMinPlaybackSpeed(minPlaybackSpeed);
            return this;
        }

        @Deprecated
        public Builder setLiveMaxPlaybackSpeed(float maxPlaybackSpeed) {
            this.liveConfiguration.setMaxPlaybackSpeed(maxPlaybackSpeed);
            return this;
        }

        public Builder setTag(Object tag) {
            this.tag = tag;
            return this;
        }

        public Builder setImageDurationMs(long imageDurationMs) {
            Assertions.checkArgument(imageDurationMs > 0 || imageDurationMs == C.TIME_UNSET);
            this.imageDurationMs = imageDurationMs;
            return this;
        }

        public Builder setMediaMetadata(MediaMetadata mediaMetadata) {
            this.mediaMetadata = mediaMetadata;
            return this;
        }

        public Builder setRequestMetadata(RequestMetadata requestMetadata) {
            this.requestMetadata = requestMetadata;
            return this;
        }

        public MediaItem build() {
            LocalConfiguration localConfiguration;
            Assertions.checkState(this.drmConfiguration.licenseUri == null || this.drmConfiguration.scheme != null);
            Uri uri = this.uri;
            if (uri == null) {
                localConfiguration = null;
            } else {
                LocalConfiguration localConfiguration2 = new LocalConfiguration(uri, this.mimeType, this.drmConfiguration.scheme != null ? this.drmConfiguration.build() : null, this.adsConfiguration, this.streamKeys, this.customCacheKey, this.subtitleConfigurations, this.tag, this.imageDurationMs);
                localConfiguration = localConfiguration2;
            }
            return new MediaItem(this.mediaId != null ? this.mediaId : "", this.clippingConfiguration.buildClippingProperties(), localConfiguration, this.liveConfiguration.build(), this.mediaMetadata != null ? this.mediaMetadata : MediaMetadata.EMPTY, this.requestMetadata);
        }
    }

    public static final class DrmConfiguration {
        public final boolean forceDefaultLicenseUri;
        public final ImmutableList<Integer> forcedSessionTrackTypes;
        private final byte[] keySetId;
        public final ImmutableMap<String, String> licenseRequestHeaders;
        public final Uri licenseUri;
        public final boolean multiSession;
        public final boolean playClearContentWithoutKey;

        @Deprecated
        public final ImmutableMap<String, String> requestHeaders;
        public final UUID scheme;

        @Deprecated
        public final ImmutableList<Integer> sessionForClearTypes;

        @Deprecated
        public final UUID uuid;
        private static final String FIELD_SCHEME = Util.intToStringMaxRadix(0);
        private static final String FIELD_LICENSE_URI = Util.intToStringMaxRadix(1);
        private static final String FIELD_LICENSE_REQUEST_HEADERS = Util.intToStringMaxRadix(2);
        private static final String FIELD_MULTI_SESSION = Util.intToStringMaxRadix(3);
        static final String FIELD_PLAY_CLEAR_CONTENT_WITHOUT_KEY = Util.intToStringMaxRadix(4);
        private static final String FIELD_FORCE_DEFAULT_LICENSE_URI = Util.intToStringMaxRadix(5);
        private static final String FIELD_FORCED_SESSION_TRACK_TYPES = Util.intToStringMaxRadix(6);
        private static final String FIELD_KEY_SET_ID = Util.intToStringMaxRadix(7);

        public static final class Builder {
            private boolean forceDefaultLicenseUri;
            private ImmutableList<Integer> forcedSessionTrackTypes;
            private byte[] keySetId;
            private ImmutableMap<String, String> licenseRequestHeaders;
            private Uri licenseUri;
            private boolean multiSession;
            private boolean playClearContentWithoutKey;
            private UUID scheme;

            public Builder(UUID scheme) {
                this();
                this.scheme = scheme;
            }

            @Deprecated
            private Builder() {
                this.licenseRequestHeaders = ImmutableMap.of();
                this.playClearContentWithoutKey = true;
                this.forcedSessionTrackTypes = ImmutableList.of();
            }

            private Builder(DrmConfiguration drmConfiguration) {
                this.scheme = drmConfiguration.scheme;
                this.licenseUri = drmConfiguration.licenseUri;
                this.licenseRequestHeaders = drmConfiguration.licenseRequestHeaders;
                this.multiSession = drmConfiguration.multiSession;
                this.playClearContentWithoutKey = drmConfiguration.playClearContentWithoutKey;
                this.forceDefaultLicenseUri = drmConfiguration.forceDefaultLicenseUri;
                this.forcedSessionTrackTypes = drmConfiguration.forcedSessionTrackTypes;
                this.keySetId = drmConfiguration.keySetId;
            }

            public Builder setScheme(UUID scheme) {
                this.scheme = scheme;
                return this;
            }

            /* JADX INFO: Access modifiers changed from: private */
            @Deprecated
            public Builder setNullableScheme(UUID scheme) {
                this.scheme = scheme;
                return this;
            }

            public Builder setLicenseUri(Uri licenseUri) {
                this.licenseUri = licenseUri;
                return this;
            }

            public Builder setLicenseUri(String licenseUri) {
                this.licenseUri = licenseUri == null ? null : Uri.parse(licenseUri);
                return this;
            }

            public Builder setLicenseRequestHeaders(Map<String, String> licenseRequestHeaders) {
                this.licenseRequestHeaders = ImmutableMap.copyOf((Map) licenseRequestHeaders);
                return this;
            }

            public Builder setMultiSession(boolean multiSession) {
                this.multiSession = multiSession;
                return this;
            }

            public Builder setForceDefaultLicenseUri(boolean forceDefaultLicenseUri) {
                this.forceDefaultLicenseUri = forceDefaultLicenseUri;
                return this;
            }

            public Builder setPlayClearContentWithoutKey(boolean playClearContentWithoutKey) {
                this.playClearContentWithoutKey = playClearContentWithoutKey;
                return this;
            }

            @Deprecated
            public Builder forceSessionsForAudioAndVideoTracks(boolean forceSessionsForAudioAndVideoTracks) {
                return setForceSessionsForAudioAndVideoTracks(forceSessionsForAudioAndVideoTracks);
            }

            public Builder setForceSessionsForAudioAndVideoTracks(boolean forceSessionsForAudioAndVideoTracks) {
                ImmutableList immutableListOf;
                if (forceSessionsForAudioAndVideoTracks) {
                    immutableListOf = ImmutableList.of(2, 1);
                } else {
                    immutableListOf = ImmutableList.of();
                }
                setForcedSessionTrackTypes(immutableListOf);
                return this;
            }

            public Builder setForcedSessionTrackTypes(List<Integer> forcedSessionTrackTypes) {
                this.forcedSessionTrackTypes = ImmutableList.copyOf((Collection) forcedSessionTrackTypes);
                return this;
            }

            public Builder setKeySetId(byte[] keySetId) {
                this.keySetId = keySetId != null ? Arrays.copyOf(keySetId, keySetId.length) : null;
                return this;
            }

            public DrmConfiguration build() {
                return new DrmConfiguration(this);
            }
        }

        private DrmConfiguration(Builder builder) {
            byte[] bArrCopyOf;
            Assertions.checkState((builder.forceDefaultLicenseUri && builder.licenseUri == null) ? false : true);
            this.scheme = (UUID) Assertions.checkNotNull(builder.scheme);
            this.uuid = this.scheme;
            this.licenseUri = builder.licenseUri;
            this.requestHeaders = builder.licenseRequestHeaders;
            this.licenseRequestHeaders = builder.licenseRequestHeaders;
            this.multiSession = builder.multiSession;
            this.forceDefaultLicenseUri = builder.forceDefaultLicenseUri;
            this.playClearContentWithoutKey = builder.playClearContentWithoutKey;
            this.sessionForClearTypes = builder.forcedSessionTrackTypes;
            this.forcedSessionTrackTypes = builder.forcedSessionTrackTypes;
            if (builder.keySetId != null) {
                bArrCopyOf = Arrays.copyOf(builder.keySetId, builder.keySetId.length);
            } else {
                bArrCopyOf = null;
            }
            this.keySetId = bArrCopyOf;
        }

        public byte[] getKeySetId() {
            if (this.keySetId != null) {
                return Arrays.copyOf(this.keySetId, this.keySetId.length);
            }
            return null;
        }

        public Builder buildUpon() {
            return new Builder();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DrmConfiguration)) {
                return false;
            }
            DrmConfiguration other = (DrmConfiguration) obj;
            return this.scheme.equals(other.scheme) && Util.areEqual(this.licenseUri, other.licenseUri) && Util.areEqual(this.licenseRequestHeaders, other.licenseRequestHeaders) && this.multiSession == other.multiSession && this.forceDefaultLicenseUri == other.forceDefaultLicenseUri && this.playClearContentWithoutKey == other.playClearContentWithoutKey && this.forcedSessionTrackTypes.equals(other.forcedSessionTrackTypes) && Arrays.equals(this.keySetId, other.keySetId);
        }

        public int hashCode() {
            return (((((((((((((this.scheme.hashCode() * 31) + (this.licenseUri != null ? this.licenseUri.hashCode() : 0)) * 31) + this.licenseRequestHeaders.hashCode()) * 31) + (this.multiSession ? 1 : 0)) * 31) + (this.forceDefaultLicenseUri ? 1 : 0)) * 31) + (this.playClearContentWithoutKey ? 1 : 0)) * 31) + this.forcedSessionTrackTypes.hashCode()) * 31) + Arrays.hashCode(this.keySetId);
        }

        public static DrmConfiguration fromBundle(Bundle bundle) {
            UUID scheme = UUID.fromString((String) Assertions.checkNotNull(bundle.getString(FIELD_SCHEME)));
            Uri licenseUri = (Uri) bundle.getParcelable(FIELD_LICENSE_URI);
            Bundle licenseMapAsBundle = BundleCollectionUtil.getBundleWithDefault(bundle, FIELD_LICENSE_REQUEST_HEADERS, Bundle.EMPTY);
            ImmutableMap<String, String> licenseRequestHeaders = BundleCollectionUtil.bundleToStringImmutableMap(licenseMapAsBundle);
            boolean multiSession = bundle.getBoolean(FIELD_MULTI_SESSION, false);
            boolean playClearContentWithoutKey = bundle.getBoolean(FIELD_PLAY_CLEAR_CONTENT_WITHOUT_KEY, false);
            boolean forceDefaultLicenseUri = bundle.getBoolean(FIELD_FORCE_DEFAULT_LICENSE_URI, false);
            ArrayList<Integer> forcedSessionTrackTypesArray = BundleCollectionUtil.getIntegerArrayListWithDefault(bundle, FIELD_FORCED_SESSION_TRACK_TYPES, new ArrayList());
            ImmutableList<Integer> forcedSessionTrackTypes = ImmutableList.copyOf((Collection) forcedSessionTrackTypesArray);
            byte[] keySetId = bundle.getByteArray(FIELD_KEY_SET_ID);
            Builder builder = new Builder(scheme);
            return builder.setLicenseUri(licenseUri).setLicenseRequestHeaders(licenseRequestHeaders).setMultiSession(multiSession).setForceDefaultLicenseUri(forceDefaultLicenseUri).setPlayClearContentWithoutKey(playClearContentWithoutKey).setForcedSessionTrackTypes(forcedSessionTrackTypes).setKeySetId(keySetId).build();
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString(FIELD_SCHEME, this.scheme.toString());
            if (this.licenseUri != null) {
                bundle.putParcelable(FIELD_LICENSE_URI, this.licenseUri);
            }
            if (!this.licenseRequestHeaders.isEmpty()) {
                bundle.putBundle(FIELD_LICENSE_REQUEST_HEADERS, BundleCollectionUtil.stringMapToBundle(this.licenseRequestHeaders));
            }
            if (this.multiSession) {
                bundle.putBoolean(FIELD_MULTI_SESSION, this.multiSession);
            }
            if (this.playClearContentWithoutKey) {
                bundle.putBoolean(FIELD_PLAY_CLEAR_CONTENT_WITHOUT_KEY, this.playClearContentWithoutKey);
            }
            if (this.forceDefaultLicenseUri) {
                bundle.putBoolean(FIELD_FORCE_DEFAULT_LICENSE_URI, this.forceDefaultLicenseUri);
            }
            if (!this.forcedSessionTrackTypes.isEmpty()) {
                bundle.putIntegerArrayList(FIELD_FORCED_SESSION_TRACK_TYPES, new ArrayList<>(this.forcedSessionTrackTypes));
            }
            if (this.keySetId != null) {
                bundle.putByteArray(FIELD_KEY_SET_ID, this.keySetId);
            }
            return bundle;
        }
    }

    public static final class AdsConfiguration {
        private static final String FIELD_AD_TAG_URI = Util.intToStringMaxRadix(0);
        public final Uri adTagUri;
        public final Object adsId;

        public static final class Builder {
            private Uri adTagUri;
            private Object adsId;

            public Builder(Uri adTagUri) {
                this.adTagUri = adTagUri;
            }

            public Builder setAdTagUri(Uri adTagUri) {
                this.adTagUri = adTagUri;
                return this;
            }

            public Builder setAdsId(Object adsId) {
                this.adsId = adsId;
                return this;
            }

            public AdsConfiguration build() {
                return new AdsConfiguration(this);
            }
        }

        private AdsConfiguration(Builder builder) {
            this.adTagUri = builder.adTagUri;
            this.adsId = builder.adsId;
        }

        public Builder buildUpon() {
            return new Builder(this.adTagUri).setAdsId(this.adsId);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AdsConfiguration)) {
                return false;
            }
            AdsConfiguration other = (AdsConfiguration) obj;
            return this.adTagUri.equals(other.adTagUri) && Util.areEqual(this.adsId, other.adsId);
        }

        public int hashCode() {
            int result = this.adTagUri.hashCode();
            return (result * 31) + (this.adsId != null ? this.adsId.hashCode() : 0);
        }

        public static AdsConfiguration fromBundle(Bundle bundle) {
            Uri adTagUri = (Uri) bundle.getParcelable(FIELD_AD_TAG_URI);
            Assertions.checkNotNull(adTagUri);
            return new Builder(adTagUri).build();
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putParcelable(FIELD_AD_TAG_URI, this.adTagUri);
            return bundle;
        }
    }

    public static final class LocalConfiguration {
        public final AdsConfiguration adsConfiguration;
        public final String customCacheKey;
        public final DrmConfiguration drmConfiguration;
        public final long imageDurationMs;
        public final String mimeType;
        public final List<StreamKey> streamKeys;
        public final ImmutableList<SubtitleConfiguration> subtitleConfigurations;

        @Deprecated
        public final List<Subtitle> subtitles;
        public final Object tag;
        public final Uri uri;
        private static final String FIELD_URI = Util.intToStringMaxRadix(0);
        private static final String FIELD_MIME_TYPE = Util.intToStringMaxRadix(1);
        private static final String FIELD_DRM_CONFIGURATION = Util.intToStringMaxRadix(2);
        private static final String FIELD_ADS_CONFIGURATION = Util.intToStringMaxRadix(3);
        private static final String FIELD_STREAM_KEYS = Util.intToStringMaxRadix(4);
        private static final String FIELD_CUSTOM_CACHE_KEY = Util.intToStringMaxRadix(5);
        private static final String FIELD_SUBTITLE_CONFIGURATION = Util.intToStringMaxRadix(6);
        private static final String FIELD_IMAGE_DURATION_MS = Util.intToStringMaxRadix(7);

        private LocalConfiguration(Uri uri, String mimeType, DrmConfiguration drmConfiguration, AdsConfiguration adsConfiguration, List<StreamKey> streamKeys, String customCacheKey, ImmutableList<SubtitleConfiguration> subtitleConfigurations, Object tag, long imageDurationMs) {
            this.uri = uri;
            this.mimeType = MimeTypes.normalizeMimeType(mimeType);
            this.drmConfiguration = drmConfiguration;
            this.adsConfiguration = adsConfiguration;
            this.streamKeys = streamKeys;
            this.customCacheKey = customCacheKey;
            this.subtitleConfigurations = subtitleConfigurations;
            ImmutableList.Builder<Subtitle> subtitles = ImmutableList.builder();
            for (int i = 0; i < subtitleConfigurations.size(); i++) {
                subtitles.add(subtitleConfigurations.get(i).buildUpon().buildSubtitle());
            }
            this.subtitles = subtitles.build();
            this.tag = tag;
            this.imageDurationMs = imageDurationMs;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LocalConfiguration)) {
                return false;
            }
            LocalConfiguration other = (LocalConfiguration) obj;
            return this.uri.equals(other.uri) && Util.areEqual(this.mimeType, other.mimeType) && Util.areEqual(this.drmConfiguration, other.drmConfiguration) && Util.areEqual(this.adsConfiguration, other.adsConfiguration) && this.streamKeys.equals(other.streamKeys) && Util.areEqual(this.customCacheKey, other.customCacheKey) && this.subtitleConfigurations.equals(other.subtitleConfigurations) && Util.areEqual(this.tag, other.tag) && Util.areEqual(Long.valueOf(this.imageDurationMs), Long.valueOf(other.imageDurationMs));
        }

        public int hashCode() {
            int result = this.uri.hashCode();
            return (int) ((((long) ((((((((((((((result * 31) + (this.mimeType == null ? 0 : this.mimeType.hashCode())) * 31) + (this.drmConfiguration == null ? 0 : this.drmConfiguration.hashCode())) * 31) + (this.adsConfiguration == null ? 0 : this.adsConfiguration.hashCode())) * 31) + this.streamKeys.hashCode()) * 31) + (this.customCacheKey == null ? 0 : this.customCacheKey.hashCode())) * 31) + this.subtitleConfigurations.hashCode()) * 31) + (this.tag != null ? this.tag.hashCode() : 0))) * 31) + this.imageDurationMs);
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putParcelable(FIELD_URI, this.uri);
            if (this.mimeType != null) {
                bundle.putString(FIELD_MIME_TYPE, this.mimeType);
            }
            if (this.drmConfiguration != null) {
                bundle.putBundle(FIELD_DRM_CONFIGURATION, this.drmConfiguration.toBundle());
            }
            if (this.adsConfiguration != null) {
                bundle.putBundle(FIELD_ADS_CONFIGURATION, this.adsConfiguration.toBundle());
            }
            if (!this.streamKeys.isEmpty()) {
                bundle.putParcelableArrayList(FIELD_STREAM_KEYS, BundleCollectionUtil.toBundleArrayList(this.streamKeys, new Function() { // from class: androidx.media3.common.MediaItem$LocalConfiguration$$ExternalSyntheticLambda0
                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return ((StreamKey) obj).toBundle();
                    }
                }));
            }
            if (this.customCacheKey != null) {
                bundle.putString(FIELD_CUSTOM_CACHE_KEY, this.customCacheKey);
            }
            if (!this.subtitleConfigurations.isEmpty()) {
                bundle.putParcelableArrayList(FIELD_SUBTITLE_CONFIGURATION, BundleCollectionUtil.toBundleArrayList(this.subtitleConfigurations, new Function() { // from class: androidx.media3.common.MediaItem$LocalConfiguration$$ExternalSyntheticLambda1
                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return ((MediaItem.SubtitleConfiguration) obj).toBundle();
                    }
                }));
            }
            if (this.imageDurationMs != C.TIME_UNSET) {
                bundle.putLong(FIELD_IMAGE_DURATION_MS, this.imageDurationMs);
            }
            return bundle;
        }

        public static LocalConfiguration fromBundle(Bundle bundle) {
            List<StreamKey> streamKeys;
            ImmutableList<SubtitleConfiguration> subtitleConfiguration;
            Bundle drmBundle = bundle.getBundle(FIELD_DRM_CONFIGURATION);
            DrmConfiguration drmConfiguration = drmBundle == null ? null : DrmConfiguration.fromBundle(drmBundle);
            Bundle adsBundle = bundle.getBundle(FIELD_ADS_CONFIGURATION);
            AdsConfiguration adsConfiguration = adsBundle != null ? AdsConfiguration.fromBundle(adsBundle) : null;
            List<Bundle> streamKeysBundles = bundle.getParcelableArrayList(FIELD_STREAM_KEYS);
            if (streamKeysBundles == null) {
                streamKeys = ImmutableList.of();
            } else {
                streamKeys = BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.common.MediaItem$LocalConfiguration$$ExternalSyntheticLambda2
                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return StreamKey.fromBundle((Bundle) obj);
                    }
                }, streamKeysBundles);
            }
            List<Bundle> subtitleBundles = bundle.getParcelableArrayList(FIELD_SUBTITLE_CONFIGURATION);
            if (subtitleBundles == null) {
                subtitleConfiguration = ImmutableList.of();
            } else {
                subtitleConfiguration = BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.common.MediaItem$LocalConfiguration$$ExternalSyntheticLambda3
                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return MediaItem.SubtitleConfiguration.fromBundle((Bundle) obj);
                    }
                }, subtitleBundles);
            }
            long imageDurationMs = bundle.getLong(FIELD_IMAGE_DURATION_MS, C.TIME_UNSET);
            return new LocalConfiguration((Uri) Assertions.checkNotNull((Uri) bundle.getParcelable(FIELD_URI)), bundle.getString(FIELD_MIME_TYPE), drmConfiguration, adsConfiguration, streamKeys, bundle.getString(FIELD_CUSTOM_CACHE_KEY), subtitleConfiguration, null, imageDurationMs);
        }
    }

    public static final class LiveConfiguration {
        public final long maxOffsetMs;
        public final float maxPlaybackSpeed;
        public final long minOffsetMs;
        public final float minPlaybackSpeed;
        public final long targetOffsetMs;
        public static final LiveConfiguration UNSET = new Builder().build();
        private static final String FIELD_TARGET_OFFSET_MS = Util.intToStringMaxRadix(0);
        private static final String FIELD_MIN_OFFSET_MS = Util.intToStringMaxRadix(1);
        private static final String FIELD_MAX_OFFSET_MS = Util.intToStringMaxRadix(2);
        private static final String FIELD_MIN_PLAYBACK_SPEED = Util.intToStringMaxRadix(3);
        private static final String FIELD_MAX_PLAYBACK_SPEED = Util.intToStringMaxRadix(4);

        public static final class Builder {
            private long maxOffsetMs;
            private float maxPlaybackSpeed;
            private long minOffsetMs;
            private float minPlaybackSpeed;
            private long targetOffsetMs;

            public Builder() {
                this.targetOffsetMs = C.TIME_UNSET;
                this.minOffsetMs = C.TIME_UNSET;
                this.maxOffsetMs = C.TIME_UNSET;
                this.minPlaybackSpeed = -3.4028235E38f;
                this.maxPlaybackSpeed = -3.4028235E38f;
            }

            private Builder(LiveConfiguration liveConfiguration) {
                this.targetOffsetMs = liveConfiguration.targetOffsetMs;
                this.minOffsetMs = liveConfiguration.minOffsetMs;
                this.maxOffsetMs = liveConfiguration.maxOffsetMs;
                this.minPlaybackSpeed = liveConfiguration.minPlaybackSpeed;
                this.maxPlaybackSpeed = liveConfiguration.maxPlaybackSpeed;
            }

            public Builder setTargetOffsetMs(long targetOffsetMs) {
                this.targetOffsetMs = targetOffsetMs;
                return this;
            }

            public Builder setMinOffsetMs(long minOffsetMs) {
                this.minOffsetMs = minOffsetMs;
                return this;
            }

            public Builder setMaxOffsetMs(long maxOffsetMs) {
                this.maxOffsetMs = maxOffsetMs;
                return this;
            }

            public Builder setMinPlaybackSpeed(float minPlaybackSpeed) {
                this.minPlaybackSpeed = minPlaybackSpeed;
                return this;
            }

            public Builder setMaxPlaybackSpeed(float maxPlaybackSpeed) {
                this.maxPlaybackSpeed = maxPlaybackSpeed;
                return this;
            }

            public LiveConfiguration build() {
                return new LiveConfiguration(this);
            }
        }

        private LiveConfiguration(Builder builder) {
            this(builder.targetOffsetMs, builder.minOffsetMs, builder.maxOffsetMs, builder.minPlaybackSpeed, builder.maxPlaybackSpeed);
        }

        @Deprecated
        public LiveConfiguration(long targetOffsetMs, long minOffsetMs, long maxOffsetMs, float minPlaybackSpeed, float maxPlaybackSpeed) {
            this.targetOffsetMs = targetOffsetMs;
            this.minOffsetMs = minOffsetMs;
            this.maxOffsetMs = maxOffsetMs;
            this.minPlaybackSpeed = minPlaybackSpeed;
            this.maxPlaybackSpeed = maxPlaybackSpeed;
        }

        public Builder buildUpon() {
            return new Builder();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LiveConfiguration)) {
                return false;
            }
            LiveConfiguration other = (LiveConfiguration) obj;
            return this.targetOffsetMs == other.targetOffsetMs && this.minOffsetMs == other.minOffsetMs && this.maxOffsetMs == other.maxOffsetMs && this.minPlaybackSpeed == other.minPlaybackSpeed && this.maxPlaybackSpeed == other.maxPlaybackSpeed;
        }

        public int hashCode() {
            int result = (int) (this.targetOffsetMs ^ (this.targetOffsetMs >>> 32));
            return (((((((result * 31) + ((int) (this.minOffsetMs ^ (this.minOffsetMs >>> 32)))) * 31) + ((int) (this.maxOffsetMs ^ (this.maxOffsetMs >>> 32)))) * 31) + (this.minPlaybackSpeed != 0.0f ? Float.floatToIntBits(this.minPlaybackSpeed) : 0)) * 31) + (this.maxPlaybackSpeed != 0.0f ? Float.floatToIntBits(this.maxPlaybackSpeed) : 0);
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            if (this.targetOffsetMs != UNSET.targetOffsetMs) {
                bundle.putLong(FIELD_TARGET_OFFSET_MS, this.targetOffsetMs);
            }
            if (this.minOffsetMs != UNSET.minOffsetMs) {
                bundle.putLong(FIELD_MIN_OFFSET_MS, this.minOffsetMs);
            }
            if (this.maxOffsetMs != UNSET.maxOffsetMs) {
                bundle.putLong(FIELD_MAX_OFFSET_MS, this.maxOffsetMs);
            }
            if (this.minPlaybackSpeed != UNSET.minPlaybackSpeed) {
                bundle.putFloat(FIELD_MIN_PLAYBACK_SPEED, this.minPlaybackSpeed);
            }
            if (this.maxPlaybackSpeed != UNSET.maxPlaybackSpeed) {
                bundle.putFloat(FIELD_MAX_PLAYBACK_SPEED, this.maxPlaybackSpeed);
            }
            return bundle;
        }

        public static LiveConfiguration fromBundle(Bundle bundle) {
            return new Builder().setTargetOffsetMs(bundle.getLong(FIELD_TARGET_OFFSET_MS, UNSET.targetOffsetMs)).setMinOffsetMs(bundle.getLong(FIELD_MIN_OFFSET_MS, UNSET.minOffsetMs)).setMaxOffsetMs(bundle.getLong(FIELD_MAX_OFFSET_MS, UNSET.maxOffsetMs)).setMinPlaybackSpeed(bundle.getFloat(FIELD_MIN_PLAYBACK_SPEED, UNSET.minPlaybackSpeed)).setMaxPlaybackSpeed(bundle.getFloat(FIELD_MAX_PLAYBACK_SPEED, UNSET.maxPlaybackSpeed)).build();
        }
    }

    public static class SubtitleConfiguration {
        public final String id;
        public final String label;
        public final String language;
        public final String mimeType;
        public final int roleFlags;
        public final int selectionFlags;
        public final Uri uri;
        private static final String FIELD_URI = Util.intToStringMaxRadix(0);
        private static final String FIELD_MIME_TYPE = Util.intToStringMaxRadix(1);
        private static final String FIELD_LANGUAGE = Util.intToStringMaxRadix(2);
        private static final String FIELD_SELECTION_FLAGS = Util.intToStringMaxRadix(3);
        private static final String FIELD_ROLE_FLAGS = Util.intToStringMaxRadix(4);
        private static final String FIELD_LABEL = Util.intToStringMaxRadix(5);
        private static final String FIELD_ID = Util.intToStringMaxRadix(6);

        public static final class Builder {
            private String id;
            private String label;
            private String language;
            private String mimeType;
            private int roleFlags;
            private int selectionFlags;
            private Uri uri;

            public Builder(Uri uri) {
                this.uri = uri;
            }

            private Builder(SubtitleConfiguration subtitleConfiguration) {
                this.uri = subtitleConfiguration.uri;
                this.mimeType = subtitleConfiguration.mimeType;
                this.language = subtitleConfiguration.language;
                this.selectionFlags = subtitleConfiguration.selectionFlags;
                this.roleFlags = subtitleConfiguration.roleFlags;
                this.label = subtitleConfiguration.label;
                this.id = subtitleConfiguration.id;
            }

            public Builder setUri(Uri uri) {
                this.uri = uri;
                return this;
            }

            public Builder setMimeType(String mimeType) {
                this.mimeType = MimeTypes.normalizeMimeType(mimeType);
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

            public Builder setLabel(String label) {
                this.label = label;
                return this;
            }

            public Builder setId(String id) {
                this.id = id;
                return this;
            }

            public SubtitleConfiguration build() {
                return new SubtitleConfiguration(this);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public Subtitle buildSubtitle() {
                return new Subtitle(this);
            }
        }

        private SubtitleConfiguration(Uri uri, String mimeType, String language, int selectionFlags, int roleFlags, String label, String id) {
            this.uri = uri;
            this.mimeType = MimeTypes.normalizeMimeType(mimeType);
            this.language = language;
            this.selectionFlags = selectionFlags;
            this.roleFlags = roleFlags;
            this.label = label;
            this.id = id;
        }

        private SubtitleConfiguration(Builder builder) {
            this.uri = builder.uri;
            this.mimeType = builder.mimeType;
            this.language = builder.language;
            this.selectionFlags = builder.selectionFlags;
            this.roleFlags = builder.roleFlags;
            this.label = builder.label;
            this.id = builder.id;
        }

        public Builder buildUpon() {
            return new Builder();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SubtitleConfiguration)) {
                return false;
            }
            SubtitleConfiguration other = (SubtitleConfiguration) obj;
            return this.uri.equals(other.uri) && Util.areEqual(this.mimeType, other.mimeType) && Util.areEqual(this.language, other.language) && this.selectionFlags == other.selectionFlags && this.roleFlags == other.roleFlags && Util.areEqual(this.label, other.label) && Util.areEqual(this.id, other.id);
        }

        public int hashCode() {
            int result = this.uri.hashCode();
            return (((((((((((result * 31) + (this.mimeType == null ? 0 : this.mimeType.hashCode())) * 31) + (this.language == null ? 0 : this.language.hashCode())) * 31) + this.selectionFlags) * 31) + this.roleFlags) * 31) + (this.label == null ? 0 : this.label.hashCode())) * 31) + (this.id != null ? this.id.hashCode() : 0);
        }

        public static SubtitleConfiguration fromBundle(Bundle bundle) {
            Uri uri = (Uri) Assertions.checkNotNull((Uri) bundle.getParcelable(FIELD_URI));
            String mimeType = bundle.getString(FIELD_MIME_TYPE);
            String language = bundle.getString(FIELD_LANGUAGE);
            int selectionFlags = bundle.getInt(FIELD_SELECTION_FLAGS, 0);
            int roleFlags = bundle.getInt(FIELD_ROLE_FLAGS, 0);
            String label = bundle.getString(FIELD_LABEL);
            String id = bundle.getString(FIELD_ID);
            Builder builder = new Builder(uri);
            return builder.setMimeType(mimeType).setLanguage(language).setSelectionFlags(selectionFlags).setRoleFlags(roleFlags).setLabel(label).setId(id).build();
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putParcelable(FIELD_URI, this.uri);
            if (this.mimeType != null) {
                bundle.putString(FIELD_MIME_TYPE, this.mimeType);
            }
            if (this.language != null) {
                bundle.putString(FIELD_LANGUAGE, this.language);
            }
            if (this.selectionFlags != 0) {
                bundle.putInt(FIELD_SELECTION_FLAGS, this.selectionFlags);
            }
            if (this.roleFlags != 0) {
                bundle.putInt(FIELD_ROLE_FLAGS, this.roleFlags);
            }
            if (this.label != null) {
                bundle.putString(FIELD_LABEL, this.label);
            }
            if (this.id != null) {
                bundle.putString(FIELD_ID, this.id);
            }
            return bundle;
        }
    }

    @Deprecated
    public static final class Subtitle extends SubtitleConfiguration {
        @Deprecated
        public Subtitle(Uri uri, String mimeType, String language) {
            this(uri, mimeType, language, 0);
        }

        @Deprecated
        public Subtitle(Uri uri, String mimeType, String language, int selectionFlags) {
            this(uri, mimeType, language, selectionFlags, 0, null);
        }

        @Deprecated
        public Subtitle(Uri uri, String mimeType, String language, int selectionFlags, int roleFlags, String label) {
            super(uri, mimeType, language, selectionFlags, roleFlags, label, null);
        }

        private Subtitle(SubtitleConfiguration.Builder builder) {
            super(builder);
        }
    }

    public static class ClippingConfiguration {
        public final long endPositionMs;
        public final long endPositionUs;
        public final boolean relativeToDefaultPosition;
        public final boolean relativeToLiveWindow;
        public final long startPositionMs;
        public final long startPositionUs;
        public final boolean startsAtKeyFrame;
        public static final ClippingConfiguration UNSET = new Builder().build();
        private static final String FIELD_START_POSITION_MS = Util.intToStringMaxRadix(0);
        private static final String FIELD_END_POSITION_MS = Util.intToStringMaxRadix(1);
        private static final String FIELD_RELATIVE_TO_LIVE_WINDOW = Util.intToStringMaxRadix(2);
        private static final String FIELD_RELATIVE_TO_DEFAULT_POSITION = Util.intToStringMaxRadix(3);
        private static final String FIELD_STARTS_AT_KEY_FRAME = Util.intToStringMaxRadix(4);
        static final String FIELD_START_POSITION_US = Util.intToStringMaxRadix(5);
        static final String FIELD_END_POSITION_US = Util.intToStringMaxRadix(6);

        public static final class Builder {
            private long endPositionUs;
            private boolean relativeToDefaultPosition;
            private boolean relativeToLiveWindow;
            private long startPositionUs;
            private boolean startsAtKeyFrame;

            public Builder() {
                this.endPositionUs = Long.MIN_VALUE;
            }

            private Builder(ClippingConfiguration clippingConfiguration) {
                this.startPositionUs = clippingConfiguration.startPositionUs;
                this.endPositionUs = clippingConfiguration.endPositionUs;
                this.relativeToLiveWindow = clippingConfiguration.relativeToLiveWindow;
                this.relativeToDefaultPosition = clippingConfiguration.relativeToDefaultPosition;
                this.startsAtKeyFrame = clippingConfiguration.startsAtKeyFrame;
            }

            public Builder setStartPositionMs(long startPositionMs) {
                return setStartPositionUs(Util.msToUs(startPositionMs));
            }

            public Builder setStartPositionUs(long startPositionUs) {
                Assertions.checkArgument(startPositionUs >= 0);
                this.startPositionUs = startPositionUs;
                return this;
            }

            public Builder setEndPositionMs(long endPositionMs) {
                return setEndPositionUs(Util.msToUs(endPositionMs));
            }

            public Builder setEndPositionUs(long endPositionUs) {
                Assertions.checkArgument(endPositionUs == Long.MIN_VALUE || endPositionUs >= 0);
                this.endPositionUs = endPositionUs;
                return this;
            }

            public Builder setRelativeToLiveWindow(boolean relativeToLiveWindow) {
                this.relativeToLiveWindow = relativeToLiveWindow;
                return this;
            }

            public Builder setRelativeToDefaultPosition(boolean relativeToDefaultPosition) {
                this.relativeToDefaultPosition = relativeToDefaultPosition;
                return this;
            }

            public Builder setStartsAtKeyFrame(boolean startsAtKeyFrame) {
                this.startsAtKeyFrame = startsAtKeyFrame;
                return this;
            }

            public ClippingConfiguration build() {
                return new ClippingConfiguration(this);
            }

            @Deprecated
            public ClippingProperties buildClippingProperties() {
                return new ClippingProperties(this);
            }
        }

        private ClippingConfiguration(Builder builder) {
            this.startPositionMs = Util.usToMs(builder.startPositionUs);
            this.endPositionMs = Util.usToMs(builder.endPositionUs);
            this.startPositionUs = builder.startPositionUs;
            this.endPositionUs = builder.endPositionUs;
            this.relativeToLiveWindow = builder.relativeToLiveWindow;
            this.relativeToDefaultPosition = builder.relativeToDefaultPosition;
            this.startsAtKeyFrame = builder.startsAtKeyFrame;
        }

        public Builder buildUpon() {
            return new Builder();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ClippingConfiguration)) {
                return false;
            }
            ClippingConfiguration other = (ClippingConfiguration) obj;
            return this.startPositionUs == other.startPositionUs && this.endPositionUs == other.endPositionUs && this.relativeToLiveWindow == other.relativeToLiveWindow && this.relativeToDefaultPosition == other.relativeToDefaultPosition && this.startsAtKeyFrame == other.startsAtKeyFrame;
        }

        public int hashCode() {
            return (((((((((int) (this.startPositionUs ^ (this.startPositionUs >>> 32))) * 31) + ((int) (this.endPositionUs ^ (this.endPositionUs >>> 32)))) * 31) + (this.relativeToLiveWindow ? 1 : 0)) * 31) + (this.relativeToDefaultPosition ? 1 : 0)) * 31) + (this.startsAtKeyFrame ? 1 : 0);
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            if (this.startPositionMs != UNSET.startPositionMs) {
                bundle.putLong(FIELD_START_POSITION_MS, this.startPositionMs);
            }
            if (this.endPositionMs != UNSET.endPositionMs) {
                bundle.putLong(FIELD_END_POSITION_MS, this.endPositionMs);
            }
            if (this.startPositionUs != UNSET.startPositionUs) {
                bundle.putLong(FIELD_START_POSITION_US, this.startPositionUs);
            }
            if (this.endPositionUs != UNSET.endPositionUs) {
                bundle.putLong(FIELD_END_POSITION_US, this.endPositionUs);
            }
            if (this.relativeToLiveWindow != UNSET.relativeToLiveWindow) {
                bundle.putBoolean(FIELD_RELATIVE_TO_LIVE_WINDOW, this.relativeToLiveWindow);
            }
            if (this.relativeToDefaultPosition != UNSET.relativeToDefaultPosition) {
                bundle.putBoolean(FIELD_RELATIVE_TO_DEFAULT_POSITION, this.relativeToDefaultPosition);
            }
            if (this.startsAtKeyFrame != UNSET.startsAtKeyFrame) {
                bundle.putBoolean(FIELD_STARTS_AT_KEY_FRAME, this.startsAtKeyFrame);
            }
            return bundle;
        }

        public static ClippingProperties fromBundle(Bundle bundle) {
            Builder clippingConfiguration = new Builder().setStartPositionMs(bundle.getLong(FIELD_START_POSITION_MS, UNSET.startPositionMs)).setEndPositionMs(bundle.getLong(FIELD_END_POSITION_MS, UNSET.endPositionMs)).setRelativeToLiveWindow(bundle.getBoolean(FIELD_RELATIVE_TO_LIVE_WINDOW, UNSET.relativeToLiveWindow)).setRelativeToDefaultPosition(bundle.getBoolean(FIELD_RELATIVE_TO_DEFAULT_POSITION, UNSET.relativeToDefaultPosition)).setStartsAtKeyFrame(bundle.getBoolean(FIELD_STARTS_AT_KEY_FRAME, UNSET.startsAtKeyFrame));
            long startPositionUs = bundle.getLong(FIELD_START_POSITION_US, UNSET.startPositionUs);
            if (startPositionUs != UNSET.startPositionUs) {
                clippingConfiguration.setStartPositionUs(startPositionUs);
            }
            long endPositionUs = bundle.getLong(FIELD_END_POSITION_US, UNSET.endPositionUs);
            if (endPositionUs != UNSET.endPositionUs) {
                clippingConfiguration.setEndPositionUs(endPositionUs);
            }
            return clippingConfiguration.buildClippingProperties();
        }
    }

    @Deprecated
    public static final class ClippingProperties extends ClippingConfiguration {
        public static final ClippingProperties UNSET = new ClippingConfiguration.Builder().buildClippingProperties();

        private ClippingProperties(ClippingConfiguration.Builder builder) {
            super(builder);
        }
    }

    public static final class RequestMetadata {
        public final Bundle extras;
        public final Uri mediaUri;
        public final String searchQuery;
        public static final RequestMetadata EMPTY = new Builder().build();
        private static final String FIELD_MEDIA_URI = Util.intToStringMaxRadix(0);
        private static final String FIELD_SEARCH_QUERY = Util.intToStringMaxRadix(1);
        private static final String FIELD_EXTRAS = Util.intToStringMaxRadix(2);

        public static final class Builder {
            private Bundle extras;
            private Uri mediaUri;
            private String searchQuery;

            public Builder() {
            }

            private Builder(RequestMetadata requestMetadata) {
                this.mediaUri = requestMetadata.mediaUri;
                this.searchQuery = requestMetadata.searchQuery;
                this.extras = requestMetadata.extras;
            }

            public Builder setMediaUri(Uri mediaUri) {
                this.mediaUri = mediaUri;
                return this;
            }

            public Builder setSearchQuery(String searchQuery) {
                this.searchQuery = searchQuery;
                return this;
            }

            public Builder setExtras(Bundle extras) {
                this.extras = extras;
                return this;
            }

            public RequestMetadata build() {
                return new RequestMetadata(this);
            }
        }

        private RequestMetadata(Builder builder) {
            this.mediaUri = builder.mediaUri;
            this.searchQuery = builder.searchQuery;
            this.extras = builder.extras;
        }

        public Builder buildUpon() {
            return new Builder();
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RequestMetadata)) {
                return false;
            }
            RequestMetadata that = (RequestMetadata) o;
            if (Util.areEqual(this.mediaUri, that.mediaUri) && Util.areEqual(this.searchQuery, that.searchQuery)) {
                if ((this.extras == null) == (that.extras == null)) {
                    return true;
                }
            }
            return false;
        }

        public int hashCode() {
            int result = this.mediaUri == null ? 0 : this.mediaUri.hashCode();
            return (((result * 31) + (this.searchQuery == null ? 0 : this.searchQuery.hashCode())) * 31) + (this.extras != null ? 1 : 0);
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            if (this.mediaUri != null) {
                bundle.putParcelable(FIELD_MEDIA_URI, this.mediaUri);
            }
            if (this.searchQuery != null) {
                bundle.putString(FIELD_SEARCH_QUERY, this.searchQuery);
            }
            if (this.extras != null) {
                bundle.putBundle(FIELD_EXTRAS, this.extras);
            }
            return bundle;
        }

        public static RequestMetadata fromBundle(Bundle bundle) {
            return new Builder().setMediaUri((Uri) bundle.getParcelable(FIELD_MEDIA_URI)).setSearchQuery(bundle.getString(FIELD_SEARCH_QUERY)).setExtras(bundle.getBundle(FIELD_EXTRAS)).build();
        }
    }

    private MediaItem(String mediaId, ClippingProperties clippingConfiguration, LocalConfiguration localConfiguration, LiveConfiguration liveConfiguration, MediaMetadata mediaMetadata, RequestMetadata requestMetadata) {
        this.mediaId = mediaId;
        this.localConfiguration = localConfiguration;
        this.playbackProperties = localConfiguration;
        this.liveConfiguration = liveConfiguration;
        this.mediaMetadata = mediaMetadata;
        this.clippingConfiguration = clippingConfiguration;
        this.clippingProperties = clippingConfiguration;
        this.requestMetadata = requestMetadata;
    }

    public Builder buildUpon() {
        return new Builder();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MediaItem)) {
            return false;
        }
        MediaItem other = (MediaItem) obj;
        return Util.areEqual(this.mediaId, other.mediaId) && this.clippingConfiguration.equals(other.clippingConfiguration) && Util.areEqual(this.localConfiguration, other.localConfiguration) && Util.areEqual(this.liveConfiguration, other.liveConfiguration) && Util.areEqual(this.mediaMetadata, other.mediaMetadata) && Util.areEqual(this.requestMetadata, other.requestMetadata);
    }

    public int hashCode() {
        int result = this.mediaId.hashCode();
        return (((((((((result * 31) + (this.localConfiguration != null ? this.localConfiguration.hashCode() : 0)) * 31) + this.liveConfiguration.hashCode()) * 31) + this.clippingConfiguration.hashCode()) * 31) + this.mediaMetadata.hashCode()) * 31) + this.requestMetadata.hashCode();
    }

    private Bundle toBundle(boolean includeLocalConfiguration) {
        Bundle bundle = new Bundle();
        if (!this.mediaId.equals("")) {
            bundle.putString(FIELD_MEDIA_ID, this.mediaId);
        }
        if (!this.liveConfiguration.equals(LiveConfiguration.UNSET)) {
            bundle.putBundle(FIELD_LIVE_CONFIGURATION, this.liveConfiguration.toBundle());
        }
        if (!this.mediaMetadata.equals(MediaMetadata.EMPTY)) {
            bundle.putBundle(FIELD_MEDIA_METADATA, this.mediaMetadata.toBundle());
        }
        if (!this.clippingConfiguration.equals(ClippingConfiguration.UNSET)) {
            bundle.putBundle(FIELD_CLIPPING_PROPERTIES, this.clippingConfiguration.toBundle());
        }
        if (!this.requestMetadata.equals(RequestMetadata.EMPTY)) {
            bundle.putBundle(FIELD_REQUEST_METADATA, this.requestMetadata.toBundle());
        }
        if (includeLocalConfiguration && this.localConfiguration != null) {
            bundle.putBundle(FIELD_LOCAL_CONFIGURATION, this.localConfiguration.toBundle());
        }
        return bundle;
    }

    public Bundle toBundle() {
        return toBundle(false);
    }

    public Bundle toBundleIncludeLocalConfiguration() {
        return toBundle(true);
    }

    public static MediaItem fromBundle(Bundle bundle) {
        LiveConfiguration liveConfiguration;
        MediaMetadata mediaMetadata;
        ClippingProperties clippingConfiguration;
        RequestMetadata requestMetadata;
        LocalConfiguration localConfiguration;
        String mediaId = (String) Assertions.checkNotNull(bundle.getString(FIELD_MEDIA_ID, ""));
        Bundle liveConfigurationBundle = bundle.getBundle(FIELD_LIVE_CONFIGURATION);
        if (liveConfigurationBundle == null) {
            liveConfiguration = LiveConfiguration.UNSET;
        } else {
            LiveConfiguration liveConfiguration2 = LiveConfiguration.fromBundle(liveConfigurationBundle);
            liveConfiguration = liveConfiguration2;
        }
        Bundle mediaMetadataBundle = bundle.getBundle(FIELD_MEDIA_METADATA);
        if (mediaMetadataBundle == null) {
            mediaMetadata = MediaMetadata.EMPTY;
        } else {
            MediaMetadata mediaMetadata2 = MediaMetadata.fromBundle(mediaMetadataBundle);
            mediaMetadata = mediaMetadata2;
        }
        Bundle clippingConfigurationBundle = bundle.getBundle(FIELD_CLIPPING_PROPERTIES);
        if (clippingConfigurationBundle == null) {
            clippingConfiguration = ClippingProperties.UNSET;
        } else {
            ClippingProperties clippingConfiguration2 = ClippingConfiguration.fromBundle(clippingConfigurationBundle);
            clippingConfiguration = clippingConfiguration2;
        }
        Bundle requestMetadataBundle = bundle.getBundle(FIELD_REQUEST_METADATA);
        if (requestMetadataBundle == null) {
            requestMetadata = RequestMetadata.EMPTY;
        } else {
            RequestMetadata requestMetadata2 = RequestMetadata.fromBundle(requestMetadataBundle);
            requestMetadata = requestMetadata2;
        }
        Bundle localConfigurationBundle = bundle.getBundle(FIELD_LOCAL_CONFIGURATION);
        if (localConfigurationBundle == null) {
            localConfiguration = null;
        } else {
            LocalConfiguration localConfiguration2 = LocalConfiguration.fromBundle(localConfigurationBundle);
            localConfiguration = localConfiguration2;
        }
        return new MediaItem(mediaId, clippingConfiguration, localConfiguration, liveConfiguration, mediaMetadata, requestMetadata);
    }
}
