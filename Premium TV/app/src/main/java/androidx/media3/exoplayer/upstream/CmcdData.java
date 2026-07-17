package androidx.media3.exoplayer.upstream;

import android.net.Uri;
import android.text.TextUtils;
import androidx.media3.common.C;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSpec;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public final class CmcdData {
    private static final Joiner COMMA_JOINER = Joiner.on(",");
    private final CmcdObject cmcdObject;
    private final CmcdRequest cmcdRequest;
    private final CmcdSession cmcdSession;
    private final CmcdStatus cmcdStatus;
    private final int dataTransmissionMode;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ObjectType {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface StreamType {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface StreamingFormat {
    }

    public static final class Factory {
        private static final Pattern CUSTOM_KEY_NAME_PATTERN = Pattern.compile(".*-.*");
        public static final String OBJECT_TYPE_AUDIO_ONLY = "a";
        public static final String OBJECT_TYPE_INIT_SEGMENT = "i";
        public static final String OBJECT_TYPE_MUXED_AUDIO_AND_VIDEO = "av";
        public static final String OBJECT_TYPE_VIDEO_ONLY = "v";
        public static final String STREAMING_FORMAT_DASH = "d";
        public static final String STREAMING_FORMAT_HLS = "h";
        public static final String STREAMING_FORMAT_SS = "s";
        public static final String STREAM_TYPE_LIVE = "l";
        public static final String STREAM_TYPE_VOD = "v";
        private final long bufferedDurationUs;
        private long chunkDurationUs;
        private final CmcdConfiguration cmcdConfiguration;
        private final boolean didRebuffer;
        private final boolean isBufferEmpty;
        private final boolean isLive;
        private String nextObjectRequest;
        private String nextRangeRequest;
        private String objectType;
        private final float playbackRate;
        private final String streamingFormat;
        private final ExoTrackSelection trackSelection;

        public Factory(CmcdConfiguration cmcdConfiguration, ExoTrackSelection trackSelection, long bufferedDurationUs, float playbackRate, String streamingFormat, boolean isLive, boolean didRebuffer, boolean isBufferEmpty) {
            boolean z = true;
            Assertions.checkArgument(bufferedDurationUs >= 0);
            if (playbackRate != -3.4028235E38f && playbackRate <= 0.0f) {
                z = false;
            }
            Assertions.checkArgument(z);
            this.cmcdConfiguration = cmcdConfiguration;
            this.trackSelection = trackSelection;
            this.bufferedDurationUs = bufferedDurationUs;
            this.playbackRate = playbackRate;
            this.streamingFormat = streamingFormat;
            this.isLive = isLive;
            this.didRebuffer = didRebuffer;
            this.isBufferEmpty = isBufferEmpty;
            this.chunkDurationUs = C.TIME_UNSET;
        }

        public static String getObjectType(ExoTrackSelection trackSelection) {
            Assertions.checkArgument(trackSelection != null);
            int trackType = MimeTypes.getTrackType(trackSelection.getSelectedFormat().sampleMimeType);
            if (trackType == -1) {
                trackType = MimeTypes.getTrackType(trackSelection.getSelectedFormat().containerMimeType);
            }
            if (trackType == 1) {
                return OBJECT_TYPE_AUDIO_ONLY;
            }
            if (trackType == 2) {
                return "v";
            }
            return null;
        }

        public Factory setChunkDurationUs(long chunkDurationUs) {
            Assertions.checkArgument(chunkDurationUs >= 0);
            this.chunkDurationUs = chunkDurationUs;
            return this;
        }

        public Factory setObjectType(String objectType) {
            this.objectType = objectType;
            return this;
        }

        public Factory setNextObjectRequest(String nextObjectRequest) {
            this.nextObjectRequest = nextObjectRequest;
            return this;
        }

        public Factory setNextRangeRequest(String nextRangeRequest) {
            this.nextRangeRequest = nextRangeRequest;
            return this;
        }

        public CmcdData createCmcdData() {
            ImmutableListMultimap<String, String> customData = this.cmcdConfiguration.requestConfig.getCustomData();
            UnmodifiableIterator<String> it = customData.keySet().iterator();
            while (it.hasNext()) {
                String headerKey = it.next();
                validateCustomDataListFormat(customData.get(headerKey));
            }
            int bitrateKbps = Util.ceilDivide(this.trackSelection.getSelectedFormat().bitrate, 1000);
            CmcdObject.Builder cmcdObject = new CmcdObject.Builder();
            if (!getIsInitSegment()) {
                if (this.cmcdConfiguration.isBitrateLoggingAllowed()) {
                    cmcdObject.setBitrateKbps(bitrateKbps);
                }
                if (this.cmcdConfiguration.isTopBitrateLoggingAllowed()) {
                    TrackGroup trackGroup = this.trackSelection.getTrackGroup();
                    int topBitrate = this.trackSelection.getSelectedFormat().bitrate;
                    for (int i = 0; i < trackGroup.length; i++) {
                        topBitrate = Math.max(topBitrate, trackGroup.getFormat(i).bitrate);
                    }
                    cmcdObject.setTopBitrateKbps(Util.ceilDivide(topBitrate, 1000));
                }
                if (this.cmcdConfiguration.isObjectDurationLoggingAllowed()) {
                    cmcdObject.setObjectDurationMs(Util.usToMs(this.chunkDurationUs));
                }
            }
            if (this.cmcdConfiguration.isObjectTypeLoggingAllowed()) {
                cmcdObject.setObjectType(this.objectType);
            }
            if (customData.containsKey(CmcdConfiguration.KEY_CMCD_OBJECT)) {
                cmcdObject.setCustomDataList(customData.get(CmcdConfiguration.KEY_CMCD_OBJECT));
            }
            CmcdRequest.Builder cmcdRequest = new CmcdRequest.Builder();
            if (!getIsInitSegment() && this.cmcdConfiguration.isBufferLengthLoggingAllowed()) {
                cmcdRequest.setBufferLengthMs(Util.usToMs(this.bufferedDurationUs));
            }
            if (this.cmcdConfiguration.isMeasuredThroughputLoggingAllowed() && this.trackSelection.getLatestBitrateEstimate() != -2147483647L) {
                cmcdRequest.setMeasuredThroughputInKbps(Util.ceilDivide(this.trackSelection.getLatestBitrateEstimate(), 1000L));
            }
            if (this.cmcdConfiguration.isDeadlineLoggingAllowed()) {
                cmcdRequest.setDeadlineMs(Util.usToMs((long) (this.bufferedDurationUs / this.playbackRate)));
            }
            if (this.cmcdConfiguration.isStartupLoggingAllowed()) {
                cmcdRequest.setStartup(this.didRebuffer || this.isBufferEmpty);
            }
            if (this.cmcdConfiguration.isNextObjectRequestLoggingAllowed()) {
                cmcdRequest.setNextObjectRequest(this.nextObjectRequest);
            }
            if (this.cmcdConfiguration.isNextRangeRequestLoggingAllowed()) {
                cmcdRequest.setNextRangeRequest(this.nextRangeRequest);
            }
            if (customData.containsKey(CmcdConfiguration.KEY_CMCD_REQUEST)) {
                cmcdRequest.setCustomDataList(customData.get(CmcdConfiguration.KEY_CMCD_REQUEST));
            }
            CmcdSession.Builder cmcdSession = new CmcdSession.Builder();
            if (this.cmcdConfiguration.isContentIdLoggingAllowed()) {
                cmcdSession.setContentId(this.cmcdConfiguration.contentId);
            }
            if (this.cmcdConfiguration.isSessionIdLoggingAllowed()) {
                cmcdSession.setSessionId(this.cmcdConfiguration.sessionId);
            }
            if (this.cmcdConfiguration.isStreamingFormatLoggingAllowed()) {
                cmcdSession.setStreamingFormat(this.streamingFormat);
            }
            if (this.cmcdConfiguration.isStreamTypeLoggingAllowed()) {
                cmcdSession.setStreamType(this.isLive ? STREAM_TYPE_LIVE : "v");
            }
            if (this.cmcdConfiguration.isPlaybackRateLoggingAllowed()) {
                cmcdSession.setPlaybackRate(this.playbackRate);
            }
            if (customData.containsKey(CmcdConfiguration.KEY_CMCD_SESSION)) {
                cmcdSession.setCustomDataList(customData.get(CmcdConfiguration.KEY_CMCD_SESSION));
            }
            CmcdStatus.Builder cmcdStatus = new CmcdStatus.Builder();
            if (this.cmcdConfiguration.isMaximumRequestThroughputLoggingAllowed()) {
                cmcdStatus.setMaximumRequestedThroughputKbps(this.cmcdConfiguration.requestConfig.getRequestedMaximumThroughputKbps(bitrateKbps));
            }
            if (this.cmcdConfiguration.isBufferStarvationLoggingAllowed()) {
                cmcdStatus.setBufferStarvation(this.didRebuffer);
            }
            if (customData.containsKey(CmcdConfiguration.KEY_CMCD_STATUS)) {
                cmcdStatus.setCustomDataList(customData.get(CmcdConfiguration.KEY_CMCD_STATUS));
            }
            return new CmcdData(cmcdObject.build(), cmcdRequest.build(), cmcdSession.build(), cmcdStatus.build(), this.cmcdConfiguration.dataTransmissionMode);
        }

        private boolean getIsInitSegment() {
            return this.objectType != null && this.objectType.equals(OBJECT_TYPE_INIT_SEGMENT);
        }

        private void validateCustomDataListFormat(List<String> customDataList) {
            for (String customData : customDataList) {
                String key = Util.split(customData, "=")[0];
                Assertions.checkState(CUSTOM_KEY_NAME_PATTERN.matcher(key).matches());
            }
        }
    }

    private CmcdData(CmcdObject cmcdObject, CmcdRequest cmcdRequest, CmcdSession cmcdSession, CmcdStatus cmcdStatus, int datatTransmissionMode) {
        this.cmcdObject = cmcdObject;
        this.cmcdRequest = cmcdRequest;
        this.cmcdSession = cmcdSession;
        this.cmcdStatus = cmcdStatus;
        this.dataTransmissionMode = datatTransmissionMode;
    }

    public DataSpec addToDataSpec(DataSpec dataSpec) {
        ArrayListMultimap<String, String> cmcdDataMap = ArrayListMultimap.create();
        this.cmcdObject.populateCmcdDataMap(cmcdDataMap);
        this.cmcdRequest.populateCmcdDataMap(cmcdDataMap);
        this.cmcdSession.populateCmcdDataMap(cmcdDataMap);
        this.cmcdStatus.populateCmcdDataMap(cmcdDataMap);
        if (this.dataTransmissionMode == 0) {
            ImmutableMap.Builder<String, String> httpRequestHeaders = ImmutableMap.builder();
            for (String headerKey : cmcdDataMap.keySet()) {
                List<String> headerValues = cmcdDataMap.get((Object) headerKey);
                Collections.sort(headerValues);
                httpRequestHeaders.put(headerKey, COMMA_JOINER.join(headerValues));
            }
            return dataSpec.withAdditionalHeaders(httpRequestHeaders.buildOrThrow());
        }
        List<String> keyValuePairs = new ArrayList<>();
        for (Collection<String> values : cmcdDataMap.asMap().values()) {
            keyValuePairs.addAll(values);
        }
        Collections.sort(keyValuePairs);
        Uri.Builder uriBuilder = dataSpec.uri.buildUpon().appendQueryParameter(CmcdConfiguration.CMCD_QUERY_PARAMETER_KEY, COMMA_JOINER.join(keyValuePairs));
        return dataSpec.buildUpon().setUri(uriBuilder.build()).build();
    }

    private static final class CmcdObject {
        public final int bitrateKbps;
        public final ImmutableList<String> customDataList;
        public final long objectDurationMs;
        public final String objectType;
        public final int topBitrateKbps;

        public static final class Builder {
            private String objectType;
            private int bitrateKbps = C.RATE_UNSET_INT;
            private int topBitrateKbps = C.RATE_UNSET_INT;
            private long objectDurationMs = C.TIME_UNSET;
            private ImmutableList<String> customDataList = ImmutableList.of();

            public Builder setBitrateKbps(int bitrateKbps) {
                Assertions.checkArgument(bitrateKbps >= 0 || bitrateKbps == -2147483647);
                this.bitrateKbps = bitrateKbps;
                return this;
            }

            public Builder setTopBitrateKbps(int topBitrateKbps) {
                Assertions.checkArgument(topBitrateKbps >= 0 || topBitrateKbps == -2147483647);
                this.topBitrateKbps = topBitrateKbps;
                return this;
            }

            public Builder setObjectDurationMs(long objectDurationMs) {
                Assertions.checkArgument(objectDurationMs >= 0 || objectDurationMs == C.TIME_UNSET);
                this.objectDurationMs = objectDurationMs;
                return this;
            }

            public Builder setObjectType(String objectType) {
                this.objectType = objectType;
                return this;
            }

            public Builder setCustomDataList(List<String> customDataList) {
                this.customDataList = ImmutableList.copyOf((Collection) customDataList);
                return this;
            }

            public CmcdObject build() {
                return new CmcdObject(this);
            }
        }

        private CmcdObject(Builder builder) {
            this.bitrateKbps = builder.bitrateKbps;
            this.topBitrateKbps = builder.topBitrateKbps;
            this.objectDurationMs = builder.objectDurationMs;
            this.objectType = builder.objectType;
            this.customDataList = builder.customDataList;
        }

        public void populateCmcdDataMap(ArrayListMultimap<String, String> cmcdDataMap) {
            ArrayList<String> keyValuePairs = new ArrayList<>();
            if (this.bitrateKbps != -2147483647) {
                keyValuePairs.add("br=" + this.bitrateKbps);
            }
            if (this.topBitrateKbps != -2147483647) {
                keyValuePairs.add("tb=" + this.topBitrateKbps);
            }
            if (this.objectDurationMs != C.TIME_UNSET) {
                keyValuePairs.add("d=" + this.objectDurationMs);
            }
            if (!TextUtils.isEmpty(this.objectType)) {
                keyValuePairs.add("ot=" + this.objectType);
            }
            keyValuePairs.addAll(this.customDataList);
            if (!keyValuePairs.isEmpty()) {
                cmcdDataMap.putAll(CmcdConfiguration.KEY_CMCD_OBJECT, keyValuePairs);
            }
        }
    }

    private static final class CmcdRequest {
        public final long bufferLengthMs;
        public final ImmutableList<String> customDataList;
        public final long deadlineMs;
        public final long measuredThroughputInKbps;
        public final String nextObjectRequest;
        public final String nextRangeRequest;
        public final boolean startup;

        public static final class Builder {
            private String nextObjectRequest;
            private String nextRangeRequest;
            private boolean startup;
            private long bufferLengthMs = C.TIME_UNSET;
            private long measuredThroughputInKbps = -2147483647L;
            private long deadlineMs = C.TIME_UNSET;
            private ImmutableList<String> customDataList = ImmutableList.of();

            public Builder setBufferLengthMs(long bufferLengthMs) {
                Assertions.checkArgument(bufferLengthMs >= 0 || bufferLengthMs == C.TIME_UNSET);
                this.bufferLengthMs = ((50 + bufferLengthMs) / 100) * 100;
                return this;
            }

            public Builder setMeasuredThroughputInKbps(long measuredThroughputInKbps) {
                Assertions.checkArgument(measuredThroughputInKbps >= 0 || measuredThroughputInKbps == -2147483647L);
                this.measuredThroughputInKbps = ((50 + measuredThroughputInKbps) / 100) * 100;
                return this;
            }

            public Builder setDeadlineMs(long deadlineMs) {
                Assertions.checkArgument(deadlineMs >= 0 || deadlineMs == C.TIME_UNSET);
                this.deadlineMs = ((50 + deadlineMs) / 100) * 100;
                return this;
            }

            public Builder setStartup(boolean startup) {
                this.startup = startup;
                return this;
            }

            public Builder setNextObjectRequest(String nextObjectRequest) {
                this.nextObjectRequest = nextObjectRequest == null ? null : Uri.encode(nextObjectRequest);
                return this;
            }

            public Builder setNextRangeRequest(String nextRangeRequest) {
                this.nextRangeRequest = nextRangeRequest;
                return this;
            }

            public Builder setCustomDataList(List<String> customDataList) {
                this.customDataList = ImmutableList.copyOf((Collection) customDataList);
                return this;
            }

            public CmcdRequest build() {
                return new CmcdRequest(this);
            }
        }

        private CmcdRequest(Builder builder) {
            this.bufferLengthMs = builder.bufferLengthMs;
            this.measuredThroughputInKbps = builder.measuredThroughputInKbps;
            this.deadlineMs = builder.deadlineMs;
            this.startup = builder.startup;
            this.nextObjectRequest = builder.nextObjectRequest;
            this.nextRangeRequest = builder.nextRangeRequest;
            this.customDataList = builder.customDataList;
        }

        public void populateCmcdDataMap(ArrayListMultimap<String, String> cmcdDataMap) {
            ArrayList<String> keyValuePairs = new ArrayList<>();
            if (this.bufferLengthMs != C.TIME_UNSET) {
                keyValuePairs.add("bl=" + this.bufferLengthMs);
            }
            if (this.measuredThroughputInKbps != -2147483647L) {
                keyValuePairs.add("mtp=" + this.measuredThroughputInKbps);
            }
            if (this.deadlineMs != C.TIME_UNSET) {
                keyValuePairs.add("dl=" + this.deadlineMs);
            }
            if (this.startup) {
                keyValuePairs.add(CmcdConfiguration.KEY_STARTUP);
            }
            if (!TextUtils.isEmpty(this.nextObjectRequest)) {
                keyValuePairs.add(Util.formatInvariant("%s=\"%s\"", CmcdConfiguration.KEY_NEXT_OBJECT_REQUEST, this.nextObjectRequest));
            }
            if (!TextUtils.isEmpty(this.nextRangeRequest)) {
                keyValuePairs.add(Util.formatInvariant("%s=\"%s\"", CmcdConfiguration.KEY_NEXT_RANGE_REQUEST, this.nextRangeRequest));
            }
            keyValuePairs.addAll(this.customDataList);
            if (!keyValuePairs.isEmpty()) {
                cmcdDataMap.putAll(CmcdConfiguration.KEY_CMCD_REQUEST, keyValuePairs);
            }
        }
    }

    private static final class CmcdSession {
        public static final int VERSION = 1;
        public final String contentId;
        public final ImmutableList<String> customDataList;
        public final float playbackRate;
        public final String sessionId;
        public final String streamType;
        public final String streamingFormat;

        public static final class Builder {
            private String contentId;
            private ImmutableList<String> customDataList = ImmutableList.of();
            private float playbackRate;
            private String sessionId;
            private String streamType;
            private String streamingFormat;

            public Builder setContentId(String contentId) {
                Assertions.checkArgument(contentId == null || contentId.length() <= 64);
                this.contentId = contentId;
                return this;
            }

            public Builder setSessionId(String sessionId) {
                Assertions.checkArgument(sessionId == null || sessionId.length() <= 64);
                this.sessionId = sessionId;
                return this;
            }

            public Builder setStreamingFormat(String streamingFormat) {
                this.streamingFormat = streamingFormat;
                return this;
            }

            public Builder setStreamType(String streamType) {
                this.streamType = streamType;
                return this;
            }

            public Builder setPlaybackRate(float playbackRate) {
                Assertions.checkArgument(playbackRate > 0.0f || playbackRate == -3.4028235E38f);
                this.playbackRate = playbackRate;
                return this;
            }

            public Builder setCustomDataList(List<String> customDataList) {
                this.customDataList = ImmutableList.copyOf((Collection) customDataList);
                return this;
            }

            public CmcdSession build() {
                return new CmcdSession(this);
            }
        }

        private CmcdSession(Builder builder) {
            this.contentId = builder.contentId;
            this.sessionId = builder.sessionId;
            this.streamingFormat = builder.streamingFormat;
            this.streamType = builder.streamType;
            this.playbackRate = builder.playbackRate;
            this.customDataList = builder.customDataList;
        }

        public void populateCmcdDataMap(ArrayListMultimap<String, String> cmcdDataMap) {
            ArrayList<String> keyValuePairs = new ArrayList<>();
            if (!TextUtils.isEmpty(this.contentId)) {
                keyValuePairs.add(Util.formatInvariant("%s=\"%s\"", CmcdConfiguration.KEY_CONTENT_ID, this.contentId));
            }
            if (!TextUtils.isEmpty(this.sessionId)) {
                keyValuePairs.add(Util.formatInvariant("%s=\"%s\"", CmcdConfiguration.KEY_SESSION_ID, this.sessionId));
            }
            if (!TextUtils.isEmpty(this.streamingFormat)) {
                keyValuePairs.add("sf=" + this.streamingFormat);
            }
            if (!TextUtils.isEmpty(this.streamType)) {
                keyValuePairs.add("st=" + this.streamType);
            }
            if (this.playbackRate != -3.4028235E38f && this.playbackRate != 1.0f) {
                keyValuePairs.add(Util.formatInvariant("%s=%.2f", CmcdConfiguration.KEY_PLAYBACK_RATE, Float.valueOf(this.playbackRate)));
            }
            keyValuePairs.addAll(this.customDataList);
            if (!keyValuePairs.isEmpty()) {
                cmcdDataMap.putAll(CmcdConfiguration.KEY_CMCD_SESSION, keyValuePairs);
            }
        }
    }

    private static final class CmcdStatus {
        public final boolean bufferStarvation;
        public final ImmutableList<String> customDataList;
        public final int maximumRequestedThroughputKbps;

        public static final class Builder {
            private boolean bufferStarvation;
            private int maximumRequestedThroughputKbps = C.RATE_UNSET_INT;
            private ImmutableList<String> customDataList = ImmutableList.of();

            public Builder setMaximumRequestedThroughputKbps(int maximumRequestedThroughputKbps) {
                int i;
                Assertions.checkArgument(maximumRequestedThroughputKbps >= 0 || maximumRequestedThroughputKbps == -2147483647);
                if (maximumRequestedThroughputKbps == -2147483647) {
                    i = maximumRequestedThroughputKbps;
                } else {
                    i = ((maximumRequestedThroughputKbps + 50) / 100) * 100;
                }
                this.maximumRequestedThroughputKbps = i;
                return this;
            }

            public Builder setBufferStarvation(boolean bufferStarvation) {
                this.bufferStarvation = bufferStarvation;
                return this;
            }

            public Builder setCustomDataList(List<String> customDataList) {
                this.customDataList = ImmutableList.copyOf((Collection) customDataList);
                return this;
            }

            public CmcdStatus build() {
                return new CmcdStatus(this);
            }
        }

        private CmcdStatus(Builder builder) {
            this.maximumRequestedThroughputKbps = builder.maximumRequestedThroughputKbps;
            this.bufferStarvation = builder.bufferStarvation;
            this.customDataList = builder.customDataList;
        }

        public void populateCmcdDataMap(ArrayListMultimap<String, String> cmcdDataMap) {
            ArrayList<String> keyValuePairs = new ArrayList<>();
            if (this.maximumRequestedThroughputKbps != -2147483647) {
                keyValuePairs.add("rtp=" + this.maximumRequestedThroughputKbps);
            }
            if (this.bufferStarvation) {
                keyValuePairs.add(CmcdConfiguration.KEY_BUFFER_STARVATION);
            }
            keyValuePairs.addAll(this.customDataList);
            if (!keyValuePairs.isEmpty()) {
                cmcdDataMap.putAll(CmcdConfiguration.KEY_CMCD_STATUS, keyValuePairs);
            }
        }
    }
}
