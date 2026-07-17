package androidx.media3.exoplayer.analytics;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class PlaybackStats {
    public static final PlaybackStats EMPTY = merge(new PlaybackStats[0]);
    public static final int PLAYBACK_STATE_ABANDONED = 15;
    public static final int PLAYBACK_STATE_BUFFERING = 6;
    static final int PLAYBACK_STATE_COUNT = 16;
    public static final int PLAYBACK_STATE_ENDED = 11;
    public static final int PLAYBACK_STATE_FAILED = 13;
    public static final int PLAYBACK_STATE_INTERRUPTED_BY_AD = 14;
    public static final int PLAYBACK_STATE_JOINING_BACKGROUND = 1;
    public static final int PLAYBACK_STATE_JOINING_FOREGROUND = 2;
    public static final int PLAYBACK_STATE_NOT_STARTED = 0;
    public static final int PLAYBACK_STATE_PAUSED = 4;
    public static final int PLAYBACK_STATE_PAUSED_BUFFERING = 7;
    public static final int PLAYBACK_STATE_PLAYING = 3;
    public static final int PLAYBACK_STATE_SEEKING = 5;
    public static final int PLAYBACK_STATE_STOPPED = 12;
    public static final int PLAYBACK_STATE_SUPPRESSED = 9;
    public static final int PLAYBACK_STATE_SUPPRESSED_BUFFERING = 10;
    public final int abandonedBeforeReadyCount;
    public final int adPlaybackCount;
    public final List<EventTimeAndFormat> audioFormatHistory;
    public final int backgroundJoiningCount;
    public final int endedCount;
    public final int fatalErrorCount;
    public final List<EventTimeAndException> fatalErrorHistory;
    public final int fatalErrorPlaybackCount;
    public final long firstReportedTimeMs;
    public final int foregroundPlaybackCount;
    public final int initialAudioFormatBitrateCount;
    public final int initialVideoFormatBitrateCount;
    public final int initialVideoFormatHeightCount;
    public final long maxRebufferTimeMs;
    public final List<long[]> mediaTimeHistory;
    public final int nonFatalErrorCount;
    public final List<EventTimeAndException> nonFatalErrorHistory;
    public final int playbackCount;
    private final long[] playbackStateDurationsMs;
    public final List<EventTimeAndPlaybackState> playbackStateHistory;
    public final long totalAudioFormatBitrateTimeProduct;
    public final long totalAudioFormatTimeMs;
    public final long totalAudioUnderruns;
    public final long totalBandwidthBytes;
    public final long totalBandwidthTimeMs;
    public final long totalDroppedFrames;
    public final long totalInitialAudioFormatBitrate;
    public final long totalInitialVideoFormatBitrate;
    public final int totalInitialVideoFormatHeight;
    public final int totalPauseBufferCount;
    public final int totalPauseCount;
    public final int totalRebufferCount;
    public final int totalSeekCount;
    public final long totalValidJoinTimeMs;
    public final long totalVideoFormatBitrateTimeMs;
    public final long totalVideoFormatBitrateTimeProduct;
    public final long totalVideoFormatHeightTimeMs;
    public final long totalVideoFormatHeightTimeProduct;
    public final int validJoinTimeCount;
    public final List<EventTimeAndFormat> videoFormatHistory;

    public static final class EventTimeAndPlaybackState {
        public final AnalyticsListener.EventTime eventTime;
        public final int playbackState;

        public EventTimeAndPlaybackState(AnalyticsListener.EventTime eventTime, int playbackState) {
            this.eventTime = eventTime;
            this.playbackState = playbackState;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EventTimeAndPlaybackState that = (EventTimeAndPlaybackState) o;
            if (this.playbackState != that.playbackState) {
                return false;
            }
            return this.eventTime.equals(that.eventTime);
        }

        public int hashCode() {
            int result = this.eventTime.hashCode();
            return (result * 31) + this.playbackState;
        }
    }

    public static final class EventTimeAndFormat {
        public final AnalyticsListener.EventTime eventTime;
        public final Format format;

        public EventTimeAndFormat(AnalyticsListener.EventTime eventTime, Format format) {
            this.eventTime = eventTime;
            this.format = format;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EventTimeAndFormat that = (EventTimeAndFormat) o;
            if (!this.eventTime.equals(that.eventTime)) {
                return false;
            }
            if (this.format != null) {
                return this.format.equals(that.format);
            }
            if (that.format == null) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int result = this.eventTime.hashCode();
            return (result * 31) + (this.format != null ? this.format.hashCode() : 0);
        }
    }

    public static final class EventTimeAndException {
        public final AnalyticsListener.EventTime eventTime;
        public final Exception exception;

        public EventTimeAndException(AnalyticsListener.EventTime eventTime, Exception exception) {
            this.eventTime = eventTime;
            this.exception = exception;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EventTimeAndException that = (EventTimeAndException) o;
            if (!this.eventTime.equals(that.eventTime)) {
                return false;
            }
            return this.exception.equals(that.exception);
        }

        public int hashCode() {
            int result = this.eventTime.hashCode();
            return (result * 31) + this.exception.hashCode();
        }
    }

    public static PlaybackStats merge(PlaybackStats... playbackStats) {
        int i;
        long j;
        long j2;
        long[] playbackStateDurationsMs = new long[16];
        int length = playbackStats.length;
        int fatalErrorPlaybackCount = 0;
        int fatalErrorCount = 0;
        int nonFatalErrorCount = 0;
        long totalBandwidthBytes = 0;
        long totalDroppedFrames = 0;
        int initialAudioFormatBitrateCount = 0;
        long totalInitialAudioFormatBitrate = -1;
        int initialVideoFormatHeightCount = 0;
        int initialVideoFormatBitrateCount = 0;
        long totalVideoFormatBitrateTimeProduct = 0;
        long totalVideoFormatBitrateTimeProduct2 = 0;
        int totalRebufferCount = 0;
        int totalRebufferCount2 = 0;
        int validJoinTimeCount = 0;
        int playbackCount = 0;
        int totalInitialVideoFormatHeight = -1;
        int i2 = 0;
        long totalAudioUnderruns = 0;
        long totalAudioUnderruns2 = 0;
        long totalInitialVideoFormatBitrate = -1;
        long totalAudioFormatTimeMs = 0;
        long totalAudioFormatBitrateTimeProduct = 0;
        long totalAudioFormatTimeMs2 = 0;
        long totalAudioFormatBitrateTimeProduct2 = 0;
        int adPlaybackCount = 0;
        int adPlaybackCount2 = 0;
        int foregroundPlaybackCount = 0;
        int abandonedBeforeReadyCount = 0;
        long maxRebufferTimeMs = -9223372036854775807L;
        int totalPauseCount = 0;
        int totalPauseBufferCount = 0;
        int totalPauseCount2 = 0;
        long totalValidJoinTimeMs = -9223372036854775807L;
        long firstReportedTimeMs = -9223372036854775807L;
        int i3 = 16;
        while (i2 < length) {
            PlaybackStats stats = playbackStats[i2];
            int i4 = length;
            playbackCount += stats.playbackCount;
            int i5 = 0;
            while (true) {
                i = i2;
                if (i5 >= i3) {
                    break;
                }
                playbackStateDurationsMs[i5] = playbackStateDurationsMs[i5] + stats.playbackStateDurationsMs[i5];
                i5++;
                i2 = i;
                i3 = 16;
            }
            if (firstReportedTimeMs == C.TIME_UNSET) {
                firstReportedTimeMs = stats.firstReportedTimeMs;
                j = -9223372036854775807L;
            } else {
                j = -9223372036854775807L;
                if (stats.firstReportedTimeMs != C.TIME_UNSET) {
                    firstReportedTimeMs = Math.min(firstReportedTimeMs, stats.firstReportedTimeMs);
                }
            }
            foregroundPlaybackCount += stats.foregroundPlaybackCount;
            abandonedBeforeReadyCount += stats.abandonedBeforeReadyCount;
            validJoinTimeCount += stats.endedCount;
            totalPauseCount2 += stats.backgroundJoiningCount;
            if (totalValidJoinTimeMs == j) {
                totalValidJoinTimeMs = stats.totalValidJoinTimeMs;
            } else {
                long totalValidJoinTimeMs2 = stats.totalValidJoinTimeMs;
                if (totalValidJoinTimeMs2 != j) {
                    totalValidJoinTimeMs += stats.totalValidJoinTimeMs;
                }
            }
            totalRebufferCount2 += stats.validJoinTimeCount;
            totalPauseCount += stats.totalPauseCount;
            totalPauseBufferCount += stats.totalPauseBufferCount;
            adPlaybackCount2 += stats.totalSeekCount;
            totalRebufferCount += stats.totalRebufferCount;
            if (maxRebufferTimeMs == j) {
                maxRebufferTimeMs = stats.maxRebufferTimeMs;
            } else {
                long maxRebufferTimeMs2 = stats.maxRebufferTimeMs;
                if (maxRebufferTimeMs2 != j) {
                    maxRebufferTimeMs = Math.max(maxRebufferTimeMs, stats.maxRebufferTimeMs);
                }
            }
            adPlaybackCount += stats.adPlaybackCount;
            totalVideoFormatBitrateTimeProduct2 += stats.totalVideoFormatHeightTimeMs;
            totalAudioFormatTimeMs2 += stats.totalVideoFormatHeightTimeProduct;
            totalAudioFormatBitrateTimeProduct2 += stats.totalVideoFormatBitrateTimeMs;
            totalVideoFormatBitrateTimeProduct += stats.totalVideoFormatBitrateTimeProduct;
            totalAudioFormatTimeMs += stats.totalAudioFormatTimeMs;
            totalAudioFormatBitrateTimeProduct += stats.totalAudioFormatBitrateTimeProduct;
            initialVideoFormatHeightCount += stats.initialVideoFormatHeightCount;
            initialVideoFormatBitrateCount += stats.initialVideoFormatBitrateCount;
            if (totalInitialVideoFormatHeight != -1) {
                if (stats.totalInitialVideoFormatHeight != -1) {
                    totalInitialVideoFormatHeight += stats.totalInitialVideoFormatHeight;
                }
            } else {
                totalInitialVideoFormatHeight = stats.totalInitialVideoFormatHeight;
            }
            if (totalInitialVideoFormatBitrate == -1) {
                j2 = -1;
                totalInitialVideoFormatBitrate = stats.totalInitialVideoFormatBitrate;
            } else {
                j2 = -1;
                if (stats.totalInitialVideoFormatBitrate != -1) {
                    totalInitialVideoFormatBitrate += stats.totalInitialVideoFormatBitrate;
                }
            }
            initialAudioFormatBitrateCount += stats.initialAudioFormatBitrateCount;
            if (totalInitialAudioFormatBitrate == j2) {
                totalInitialAudioFormatBitrate = stats.totalInitialAudioFormatBitrate;
            } else {
                long totalInitialAudioFormatBitrate2 = stats.totalInitialAudioFormatBitrate;
                if (totalInitialAudioFormatBitrate2 != j2) {
                    totalInitialAudioFormatBitrate += stats.totalInitialAudioFormatBitrate;
                }
            }
            totalAudioUnderruns2 += stats.totalBandwidthTimeMs;
            totalBandwidthBytes += stats.totalBandwidthBytes;
            totalDroppedFrames += stats.totalDroppedFrames;
            totalAudioUnderruns += stats.totalAudioUnderruns;
            fatalErrorPlaybackCount += stats.fatalErrorPlaybackCount;
            fatalErrorCount += stats.fatalErrorCount;
            nonFatalErrorCount += stats.nonFatalErrorCount;
            i2 = i + 1;
            length = i4;
            i3 = 16;
        }
        return new PlaybackStats(playbackCount, playbackStateDurationsMs, Collections.emptyList(), Collections.emptyList(), firstReportedTimeMs, foregroundPlaybackCount, abandonedBeforeReadyCount, validJoinTimeCount, totalPauseCount2, totalValidJoinTimeMs, totalRebufferCount2, totalPauseCount, totalPauseBufferCount, adPlaybackCount2, totalRebufferCount, maxRebufferTimeMs, adPlaybackCount, Collections.emptyList(), Collections.emptyList(), totalVideoFormatBitrateTimeProduct2, totalAudioFormatTimeMs2, totalAudioFormatBitrateTimeProduct2, totalVideoFormatBitrateTimeProduct, totalAudioFormatTimeMs, totalAudioFormatBitrateTimeProduct, initialVideoFormatHeightCount, initialVideoFormatBitrateCount, totalInitialVideoFormatHeight, totalInitialVideoFormatBitrate, initialAudioFormatBitrateCount, totalInitialAudioFormatBitrate, totalAudioUnderruns2, totalBandwidthBytes, totalDroppedFrames, totalAudioUnderruns, fatalErrorPlaybackCount, fatalErrorCount, nonFatalErrorCount, Collections.emptyList(), Collections.emptyList());
    }

    PlaybackStats(int playbackCount, long[] playbackStateDurationsMs, List<EventTimeAndPlaybackState> playbackStateHistory, List<long[]> mediaTimeHistory, long firstReportedTimeMs, int foregroundPlaybackCount, int abandonedBeforeReadyCount, int endedCount, int backgroundJoiningCount, long totalValidJoinTimeMs, int validJoinTimeCount, int totalPauseCount, int totalPauseBufferCount, int totalSeekCount, int totalRebufferCount, long maxRebufferTimeMs, int adPlaybackCount, List<EventTimeAndFormat> videoFormatHistory, List<EventTimeAndFormat> audioFormatHistory, long totalVideoFormatHeightTimeMs, long totalVideoFormatHeightTimeProduct, long totalVideoFormatBitrateTimeMs, long totalVideoFormatBitrateTimeProduct, long totalAudioFormatTimeMs, long totalAudioFormatBitrateTimeProduct, int initialVideoFormatHeightCount, int initialVideoFormatBitrateCount, int totalInitialVideoFormatHeight, long totalInitialVideoFormatBitrate, int initialAudioFormatBitrateCount, long totalInitialAudioFormatBitrate, long totalBandwidthTimeMs, long totalBandwidthBytes, long totalDroppedFrames, long totalAudioUnderruns, int fatalErrorPlaybackCount, int fatalErrorCount, int nonFatalErrorCount, List<EventTimeAndException> fatalErrorHistory, List<EventTimeAndException> nonFatalErrorHistory) {
        this.playbackCount = playbackCount;
        this.playbackStateDurationsMs = playbackStateDurationsMs;
        this.playbackStateHistory = Collections.unmodifiableList(playbackStateHistory);
        this.mediaTimeHistory = Collections.unmodifiableList(mediaTimeHistory);
        this.firstReportedTimeMs = firstReportedTimeMs;
        this.foregroundPlaybackCount = foregroundPlaybackCount;
        this.abandonedBeforeReadyCount = abandonedBeforeReadyCount;
        this.endedCount = endedCount;
        this.backgroundJoiningCount = backgroundJoiningCount;
        this.totalValidJoinTimeMs = totalValidJoinTimeMs;
        this.validJoinTimeCount = validJoinTimeCount;
        this.totalPauseCount = totalPauseCount;
        this.totalPauseBufferCount = totalPauseBufferCount;
        this.totalSeekCount = totalSeekCount;
        this.totalRebufferCount = totalRebufferCount;
        this.maxRebufferTimeMs = maxRebufferTimeMs;
        this.adPlaybackCount = adPlaybackCount;
        this.videoFormatHistory = Collections.unmodifiableList(videoFormatHistory);
        this.audioFormatHistory = Collections.unmodifiableList(audioFormatHistory);
        this.totalVideoFormatHeightTimeMs = totalVideoFormatHeightTimeMs;
        this.totalVideoFormatHeightTimeProduct = totalVideoFormatHeightTimeProduct;
        this.totalVideoFormatBitrateTimeMs = totalVideoFormatBitrateTimeMs;
        this.totalVideoFormatBitrateTimeProduct = totalVideoFormatBitrateTimeProduct;
        this.totalAudioFormatTimeMs = totalAudioFormatTimeMs;
        this.totalAudioFormatBitrateTimeProduct = totalAudioFormatBitrateTimeProduct;
        this.initialVideoFormatHeightCount = initialVideoFormatHeightCount;
        this.initialVideoFormatBitrateCount = initialVideoFormatBitrateCount;
        this.totalInitialVideoFormatHeight = totalInitialVideoFormatHeight;
        this.totalInitialVideoFormatBitrate = totalInitialVideoFormatBitrate;
        this.initialAudioFormatBitrateCount = initialAudioFormatBitrateCount;
        this.totalInitialAudioFormatBitrate = totalInitialAudioFormatBitrate;
        this.totalBandwidthTimeMs = totalBandwidthTimeMs;
        this.totalBandwidthBytes = totalBandwidthBytes;
        this.totalDroppedFrames = totalDroppedFrames;
        this.totalAudioUnderruns = totalAudioUnderruns;
        this.fatalErrorPlaybackCount = fatalErrorPlaybackCount;
        this.fatalErrorCount = fatalErrorCount;
        this.nonFatalErrorCount = nonFatalErrorCount;
        this.fatalErrorHistory = Collections.unmodifiableList(fatalErrorHistory);
        this.nonFatalErrorHistory = Collections.unmodifiableList(nonFatalErrorHistory);
    }

    public long getPlaybackStateDurationMs(int playbackState) {
        return this.playbackStateDurationsMs[playbackState];
    }

    public int getPlaybackStateAtTime(long realtimeMs) {
        int state = 0;
        for (EventTimeAndPlaybackState timeAndState : this.playbackStateHistory) {
            if (timeAndState.eventTime.realtimeMs > realtimeMs) {
                break;
            }
            state = timeAndState.playbackState;
        }
        return state;
    }

    public long getMediaTimeMsAtRealtimeMs(long realtimeMs) {
        if (this.mediaTimeHistory.isEmpty()) {
            return C.TIME_UNSET;
        }
        int nextIndex = 0;
        while (nextIndex < this.mediaTimeHistory.size() && this.mediaTimeHistory.get(nextIndex)[0] <= realtimeMs) {
            nextIndex++;
        }
        List<long[]> list = this.mediaTimeHistory;
        if (nextIndex == 0) {
            return list.get(0)[1];
        }
        int size = list.size();
        List<long[]> list2 = this.mediaTimeHistory;
        if (nextIndex == size) {
            return list2.get(this.mediaTimeHistory.size() - 1)[1];
        }
        long prevRealtimeMs = list2.get(nextIndex - 1)[0];
        long prevMediaTimeMs = this.mediaTimeHistory.get(nextIndex - 1)[1];
        long nextRealtimeMs = this.mediaTimeHistory.get(nextIndex)[0];
        long nextMediaTimeMs = this.mediaTimeHistory.get(nextIndex)[1];
        long realtimeDurationMs = nextRealtimeMs - prevRealtimeMs;
        if (realtimeDurationMs == 0) {
            return prevMediaTimeMs;
        }
        float fraction = (realtimeMs - prevRealtimeMs) / realtimeDurationMs;
        return ((long) ((nextMediaTimeMs - prevMediaTimeMs) * fraction)) + prevMediaTimeMs;
    }

    public long getMeanJoinTimeMs() {
        return this.validJoinTimeCount == 0 ? C.TIME_UNSET : this.totalValidJoinTimeMs / ((long) this.validJoinTimeCount);
    }

    public long getTotalJoinTimeMs() {
        return getPlaybackStateDurationMs(2);
    }

    public long getTotalPlayTimeMs() {
        return getPlaybackStateDurationMs(3);
    }

    public long getMeanPlayTimeMs() {
        if (this.foregroundPlaybackCount == 0) {
            return C.TIME_UNSET;
        }
        return getTotalPlayTimeMs() / ((long) this.foregroundPlaybackCount);
    }

    public long getTotalPausedTimeMs() {
        return getPlaybackStateDurationMs(4) + getPlaybackStateDurationMs(7);
    }

    public long getMeanPausedTimeMs() {
        if (this.foregroundPlaybackCount == 0) {
            return C.TIME_UNSET;
        }
        return getTotalPausedTimeMs() / ((long) this.foregroundPlaybackCount);
    }

    public long getTotalRebufferTimeMs() {
        return getPlaybackStateDurationMs(6);
    }

    public long getMeanRebufferTimeMs() {
        if (this.foregroundPlaybackCount == 0) {
            return C.TIME_UNSET;
        }
        return getTotalRebufferTimeMs() / ((long) this.foregroundPlaybackCount);
    }

    public long getMeanSingleRebufferTimeMs() {
        if (this.totalRebufferCount == 0) {
            return C.TIME_UNSET;
        }
        return (getPlaybackStateDurationMs(6) + getPlaybackStateDurationMs(7)) / ((long) this.totalRebufferCount);
    }

    public long getTotalSeekTimeMs() {
        return getPlaybackStateDurationMs(5);
    }

    public long getMeanSeekTimeMs() {
        if (this.foregroundPlaybackCount == 0) {
            return C.TIME_UNSET;
        }
        return getTotalSeekTimeMs() / ((long) this.foregroundPlaybackCount);
    }

    public long getMeanSingleSeekTimeMs() {
        return this.totalSeekCount == 0 ? C.TIME_UNSET : getTotalSeekTimeMs() / ((long) this.totalSeekCount);
    }

    public long getTotalWaitTimeMs() {
        return getPlaybackStateDurationMs(2) + getPlaybackStateDurationMs(6) + getPlaybackStateDurationMs(5);
    }

    public long getMeanWaitTimeMs() {
        if (this.foregroundPlaybackCount == 0) {
            return C.TIME_UNSET;
        }
        return getTotalWaitTimeMs() / ((long) this.foregroundPlaybackCount);
    }

    public long getTotalPlayAndWaitTimeMs() {
        return getTotalPlayTimeMs() + getTotalWaitTimeMs();
    }

    public long getMeanPlayAndWaitTimeMs() {
        if (this.foregroundPlaybackCount == 0) {
            return C.TIME_UNSET;
        }
        return getTotalPlayAndWaitTimeMs() / ((long) this.foregroundPlaybackCount);
    }

    public long getTotalElapsedTimeMs() {
        long totalTimeMs = 0;
        for (int i = 0; i < 16; i++) {
            totalTimeMs += this.playbackStateDurationsMs[i];
        }
        return totalTimeMs;
    }

    public long getMeanElapsedTimeMs() {
        return this.playbackCount == 0 ? C.TIME_UNSET : getTotalElapsedTimeMs() / ((long) this.playbackCount);
    }

    public float getAbandonedBeforeReadyRatio() {
        int foregroundAbandonedBeforeReady = this.abandonedBeforeReadyCount - (this.playbackCount - this.foregroundPlaybackCount);
        if (this.foregroundPlaybackCount == 0) {
            return 0.0f;
        }
        return foregroundAbandonedBeforeReady / this.foregroundPlaybackCount;
    }

    public float getEndedRatio() {
        if (this.foregroundPlaybackCount == 0) {
            return 0.0f;
        }
        return this.endedCount / this.foregroundPlaybackCount;
    }

    public float getMeanPauseCount() {
        if (this.foregroundPlaybackCount == 0) {
            return 0.0f;
        }
        return this.totalPauseCount / this.foregroundPlaybackCount;
    }

    public float getMeanPauseBufferCount() {
        if (this.foregroundPlaybackCount == 0) {
            return 0.0f;
        }
        return this.totalPauseBufferCount / this.foregroundPlaybackCount;
    }

    public float getMeanSeekCount() {
        if (this.foregroundPlaybackCount == 0) {
            return 0.0f;
        }
        return this.totalSeekCount / this.foregroundPlaybackCount;
    }

    public float getMeanRebufferCount() {
        if (this.foregroundPlaybackCount == 0) {
            return 0.0f;
        }
        return this.totalRebufferCount / this.foregroundPlaybackCount;
    }

    public float getWaitTimeRatio() {
        long playAndWaitTimeMs = getTotalPlayAndWaitTimeMs();
        if (playAndWaitTimeMs == 0) {
            return 0.0f;
        }
        return getTotalWaitTimeMs() / playAndWaitTimeMs;
    }

    public float getJoinTimeRatio() {
        long playAndWaitTimeMs = getTotalPlayAndWaitTimeMs();
        if (playAndWaitTimeMs == 0) {
            return 0.0f;
        }
        return getTotalJoinTimeMs() / playAndWaitTimeMs;
    }

    public float getRebufferTimeRatio() {
        long playAndWaitTimeMs = getTotalPlayAndWaitTimeMs();
        if (playAndWaitTimeMs == 0) {
            return 0.0f;
        }
        return getTotalRebufferTimeMs() / playAndWaitTimeMs;
    }

    public float getSeekTimeRatio() {
        long playAndWaitTimeMs = getTotalPlayAndWaitTimeMs();
        if (playAndWaitTimeMs == 0) {
            return 0.0f;
        }
        return getTotalSeekTimeMs() / playAndWaitTimeMs;
    }

    public float getRebufferRate() {
        long playTimeMs = getTotalPlayTimeMs();
        if (playTimeMs == 0) {
            return 0.0f;
        }
        return (this.totalRebufferCount * 1000.0f) / playTimeMs;
    }

    public float getMeanTimeBetweenRebuffers() {
        return 1.0f / getRebufferRate();
    }

    public int getMeanInitialVideoFormatHeight() {
        if (this.initialVideoFormatHeightCount == 0) {
            return -1;
        }
        return this.totalInitialVideoFormatHeight / this.initialVideoFormatHeightCount;
    }

    public int getMeanInitialVideoFormatBitrate() {
        if (this.initialVideoFormatBitrateCount == 0) {
            return -1;
        }
        return (int) (this.totalInitialVideoFormatBitrate / ((long) this.initialVideoFormatBitrateCount));
    }

    public int getMeanInitialAudioFormatBitrate() {
        if (this.initialAudioFormatBitrateCount == 0) {
            return -1;
        }
        return (int) (this.totalInitialAudioFormatBitrate / ((long) this.initialAudioFormatBitrateCount));
    }

    public int getMeanVideoFormatHeight() {
        if (this.totalVideoFormatHeightTimeMs == 0) {
            return -1;
        }
        return (int) (this.totalVideoFormatHeightTimeProduct / this.totalVideoFormatHeightTimeMs);
    }

    public int getMeanVideoFormatBitrate() {
        if (this.totalVideoFormatBitrateTimeMs == 0) {
            return -1;
        }
        return (int) (this.totalVideoFormatBitrateTimeProduct / this.totalVideoFormatBitrateTimeMs);
    }

    public int getMeanAudioFormatBitrate() {
        if (this.totalAudioFormatTimeMs == 0) {
            return -1;
        }
        return (int) (this.totalAudioFormatBitrateTimeProduct / this.totalAudioFormatTimeMs);
    }

    public int getMeanBandwidth() {
        if (this.totalBandwidthTimeMs == 0) {
            return -1;
        }
        return (int) ((this.totalBandwidthBytes * 8000) / this.totalBandwidthTimeMs);
    }

    public float getDroppedFramesRate() {
        long playTimeMs = getTotalPlayTimeMs();
        if (playTimeMs == 0) {
            return 0.0f;
        }
        return (this.totalDroppedFrames * 1000.0f) / playTimeMs;
    }

    public float getAudioUnderrunRate() {
        long playTimeMs = getTotalPlayTimeMs();
        if (playTimeMs == 0) {
            return 0.0f;
        }
        return (this.totalAudioUnderruns * 1000.0f) / playTimeMs;
    }

    public float getFatalErrorRatio() {
        if (this.foregroundPlaybackCount == 0) {
            return 0.0f;
        }
        return this.fatalErrorPlaybackCount / this.foregroundPlaybackCount;
    }

    public float getFatalErrorRate() {
        long playTimeMs = getTotalPlayTimeMs();
        if (playTimeMs == 0) {
            return 0.0f;
        }
        return (this.fatalErrorCount * 1000.0f) / playTimeMs;
    }

    public float getMeanTimeBetweenFatalErrors() {
        return 1.0f / getFatalErrorRate();
    }

    public float getMeanNonFatalErrorCount() {
        if (this.foregroundPlaybackCount == 0) {
            return 0.0f;
        }
        return this.nonFatalErrorCount / this.foregroundPlaybackCount;
    }

    public float getNonFatalErrorRate() {
        long playTimeMs = getTotalPlayTimeMs();
        if (playTimeMs == 0) {
            return 0.0f;
        }
        return (this.nonFatalErrorCount * 1000.0f) / playTimeMs;
    }

    public float getMeanTimeBetweenNonFatalErrors() {
        return 1.0f / getNonFatalErrorRate();
    }
}
