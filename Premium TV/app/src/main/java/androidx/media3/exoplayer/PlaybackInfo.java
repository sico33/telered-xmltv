package androidx.media3.exoplayer;

import android.os.SystemClock;
import androidx.media3.common.C;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.TrackSelectorResult;
import com.google.common.collect.ImmutableList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class PlaybackInfo {
    private static final MediaSource.MediaPeriodId PLACEHOLDER_MEDIA_PERIOD_ID = new MediaSource.MediaPeriodId(new Object());
    public volatile long bufferedPositionUs;
    public final long discontinuityStartPositionUs;
    public final boolean isLoading;
    public final MediaSource.MediaPeriodId loadingMediaPeriodId;
    public final MediaSource.MediaPeriodId periodId;
    public final boolean playWhenReady;
    public final int playWhenReadyChangeReason;
    public final ExoPlaybackException playbackError;
    public final PlaybackParameters playbackParameters;
    public final int playbackState;
    public final int playbackSuppressionReason;
    public volatile long positionUpdateTimeMs;
    public volatile long positionUs;
    public final long requestedContentPositionUs;
    public final boolean sleepingForOffload;
    public final List<Metadata> staticMetadata;
    public final Timeline timeline;
    public volatile long totalBufferedDurationUs;
    public final TrackGroupArray trackGroups;
    public final TrackSelectorResult trackSelectorResult;

    public static PlaybackInfo createDummy(TrackSelectorResult emptyTrackSelectorResult) {
        return new PlaybackInfo(Timeline.EMPTY, PLACEHOLDER_MEDIA_PERIOD_ID, C.TIME_UNSET, 0L, 1, null, false, TrackGroupArray.EMPTY, emptyTrackSelectorResult, ImmutableList.of(), PLACEHOLDER_MEDIA_PERIOD_ID, false, 1, 0, PlaybackParameters.DEFAULT, 0L, 0L, 0L, 0L, false);
    }

    public PlaybackInfo(Timeline timeline, MediaSource.MediaPeriodId periodId, long requestedContentPositionUs, long discontinuityStartPositionUs, int playbackState, ExoPlaybackException playbackError, boolean isLoading, TrackGroupArray trackGroups, TrackSelectorResult trackSelectorResult, List<Metadata> staticMetadata, MediaSource.MediaPeriodId loadingMediaPeriodId, boolean playWhenReady, int playWhenReadyChangeReason, int playbackSuppressionReason, PlaybackParameters playbackParameters, long bufferedPositionUs, long totalBufferedDurationUs, long positionUs, long positionUpdateTimeMs, boolean sleepingForOffload) {
        this.timeline = timeline;
        this.periodId = periodId;
        this.requestedContentPositionUs = requestedContentPositionUs;
        this.discontinuityStartPositionUs = discontinuityStartPositionUs;
        this.playbackState = playbackState;
        this.playbackError = playbackError;
        this.isLoading = isLoading;
        this.trackGroups = trackGroups;
        this.trackSelectorResult = trackSelectorResult;
        this.staticMetadata = staticMetadata;
        this.loadingMediaPeriodId = loadingMediaPeriodId;
        this.playWhenReady = playWhenReady;
        this.playWhenReadyChangeReason = playWhenReadyChangeReason;
        this.playbackSuppressionReason = playbackSuppressionReason;
        this.playbackParameters = playbackParameters;
        this.bufferedPositionUs = bufferedPositionUs;
        this.totalBufferedDurationUs = totalBufferedDurationUs;
        this.positionUs = positionUs;
        this.positionUpdateTimeMs = positionUpdateTimeMs;
        this.sleepingForOffload = sleepingForOffload;
    }

    public static MediaSource.MediaPeriodId getDummyPeriodForEmptyTimeline() {
        return PLACEHOLDER_MEDIA_PERIOD_ID;
    }

    public PlaybackInfo copyWithNewPosition(MediaSource.MediaPeriodId periodId, long positionUs, long requestedContentPositionUs, long discontinuityStartPositionUs, long totalBufferedDurationUs, TrackGroupArray trackGroups, TrackSelectorResult trackSelectorResult, List<Metadata> staticMetadata) {
        return new PlaybackInfo(this.timeline, periodId, requestedContentPositionUs, discontinuityStartPositionUs, this.playbackState, this.playbackError, this.isLoading, trackGroups, trackSelectorResult, staticMetadata, this.loadingMediaPeriodId, this.playWhenReady, this.playWhenReadyChangeReason, this.playbackSuppressionReason, this.playbackParameters, this.bufferedPositionUs, totalBufferedDurationUs, positionUs, SystemClock.elapsedRealtime(), this.sleepingForOffload);
    }

    public PlaybackInfo copyWithTimeline(Timeline timeline) {
        return new PlaybackInfo(timeline, this.periodId, this.requestedContentPositionUs, this.discontinuityStartPositionUs, this.playbackState, this.playbackError, this.isLoading, this.trackGroups, this.trackSelectorResult, this.staticMetadata, this.loadingMediaPeriodId, this.playWhenReady, this.playWhenReadyChangeReason, this.playbackSuppressionReason, this.playbackParameters, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs, this.positionUpdateTimeMs, this.sleepingForOffload);
    }

    public PlaybackInfo copyWithPlaybackState(int playbackState) {
        return new PlaybackInfo(this.timeline, this.periodId, this.requestedContentPositionUs, this.discontinuityStartPositionUs, playbackState, this.playbackError, this.isLoading, this.trackGroups, this.trackSelectorResult, this.staticMetadata, this.loadingMediaPeriodId, this.playWhenReady, this.playWhenReadyChangeReason, this.playbackSuppressionReason, this.playbackParameters, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs, this.positionUpdateTimeMs, this.sleepingForOffload);
    }

    public PlaybackInfo copyWithPlaybackError(ExoPlaybackException playbackError) {
        return new PlaybackInfo(this.timeline, this.periodId, this.requestedContentPositionUs, this.discontinuityStartPositionUs, this.playbackState, playbackError, this.isLoading, this.trackGroups, this.trackSelectorResult, this.staticMetadata, this.loadingMediaPeriodId, this.playWhenReady, this.playWhenReadyChangeReason, this.playbackSuppressionReason, this.playbackParameters, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs, this.positionUpdateTimeMs, this.sleepingForOffload);
    }

    public PlaybackInfo copyWithIsLoading(boolean isLoading) {
        return new PlaybackInfo(this.timeline, this.periodId, this.requestedContentPositionUs, this.discontinuityStartPositionUs, this.playbackState, this.playbackError, isLoading, this.trackGroups, this.trackSelectorResult, this.staticMetadata, this.loadingMediaPeriodId, this.playWhenReady, this.playWhenReadyChangeReason, this.playbackSuppressionReason, this.playbackParameters, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs, this.positionUpdateTimeMs, this.sleepingForOffload);
    }

    public PlaybackInfo copyWithLoadingMediaPeriodId(MediaSource.MediaPeriodId loadingMediaPeriodId) {
        return new PlaybackInfo(this.timeline, this.periodId, this.requestedContentPositionUs, this.discontinuityStartPositionUs, this.playbackState, this.playbackError, this.isLoading, this.trackGroups, this.trackSelectorResult, this.staticMetadata, loadingMediaPeriodId, this.playWhenReady, this.playWhenReadyChangeReason, this.playbackSuppressionReason, this.playbackParameters, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs, this.positionUpdateTimeMs, this.sleepingForOffload);
    }

    public PlaybackInfo copyWithPlayWhenReady(boolean playWhenReady, int playWhenReadyChangeReason, int playbackSuppressionReason) {
        return new PlaybackInfo(this.timeline, this.periodId, this.requestedContentPositionUs, this.discontinuityStartPositionUs, this.playbackState, this.playbackError, this.isLoading, this.trackGroups, this.trackSelectorResult, this.staticMetadata, this.loadingMediaPeriodId, playWhenReady, playWhenReadyChangeReason, playbackSuppressionReason, this.playbackParameters, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs, this.positionUpdateTimeMs, this.sleepingForOffload);
    }

    public PlaybackInfo copyWithPlaybackParameters(PlaybackParameters playbackParameters) {
        return new PlaybackInfo(this.timeline, this.periodId, this.requestedContentPositionUs, this.discontinuityStartPositionUs, this.playbackState, this.playbackError, this.isLoading, this.trackGroups, this.trackSelectorResult, this.staticMetadata, this.loadingMediaPeriodId, this.playWhenReady, this.playWhenReadyChangeReason, this.playbackSuppressionReason, playbackParameters, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs, this.positionUpdateTimeMs, this.sleepingForOffload);
    }

    public PlaybackInfo copyWithSleepingForOffload(boolean sleepingForOffload) {
        return new PlaybackInfo(this.timeline, this.periodId, this.requestedContentPositionUs, this.discontinuityStartPositionUs, this.playbackState, this.playbackError, this.isLoading, this.trackGroups, this.trackSelectorResult, this.staticMetadata, this.loadingMediaPeriodId, this.playWhenReady, this.playWhenReadyChangeReason, this.playbackSuppressionReason, this.playbackParameters, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs, this.positionUpdateTimeMs, sleepingForOffload);
    }

    public PlaybackInfo copyWithEstimatedPosition() {
        return new PlaybackInfo(this.timeline, this.periodId, this.requestedContentPositionUs, this.discontinuityStartPositionUs, this.playbackState, this.playbackError, this.isLoading, this.trackGroups, this.trackSelectorResult, this.staticMetadata, this.loadingMediaPeriodId, this.playWhenReady, this.playWhenReadyChangeReason, this.playbackSuppressionReason, this.playbackParameters, this.bufferedPositionUs, this.totalBufferedDurationUs, getEstimatedPositionUs(), SystemClock.elapsedRealtime(), this.sleepingForOffload);
    }

    public void updatePositionUs(long positionUs) {
        this.positionUs = positionUs;
        this.positionUpdateTimeMs = SystemClock.elapsedRealtime();
    }

    public long getEstimatedPositionUs() {
        long positionUpdateTimeMs;
        long positionUs;
        if (!isPlaying()) {
            return this.positionUs;
        }
        do {
            positionUpdateTimeMs = this.positionUpdateTimeMs;
            positionUs = this.positionUs;
        } while (positionUpdateTimeMs != this.positionUpdateTimeMs);
        long elapsedTimeMs = SystemClock.elapsedRealtime() - positionUpdateTimeMs;
        long estimatedPositionMs = Util.usToMs(positionUs) + ((long) (elapsedTimeMs * this.playbackParameters.speed));
        return Util.msToUs(estimatedPositionMs);
    }

    public boolean isPlaying() {
        return this.playbackState == 3 && this.playWhenReady && this.playbackSuppressionReason == 0;
    }
}
