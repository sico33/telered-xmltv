package androidx.media3.exoplayer.source.ads;

import androidx.media3.common.AdPlaybackState;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.source.MediaSource;

/* JADX INFO: loaded from: classes.dex */
public final class ServerSideAdInsertionUtil {
    private ServerSideAdInsertionUtil() {
    }

    public static AdPlaybackState addAdGroupToAdPlaybackState(AdPlaybackState adPlaybackState, long fromPositionUs, long contentResumeOffsetUs, long... adDurationsUs) {
        long adGroupInsertionPositionUs = getMediaPeriodPositionUsForContent(fromPositionUs, -1, adPlaybackState);
        int insertionIndex = adPlaybackState.removedAdGroupCount;
        while (insertionIndex < adPlaybackState.adGroupCount && adPlaybackState.getAdGroup(insertionIndex).timeUs != Long.MIN_VALUE && adPlaybackState.getAdGroup(insertionIndex).timeUs <= adGroupInsertionPositionUs) {
            insertionIndex++;
        }
        AdPlaybackState adPlaybackState2 = adPlaybackState.withNewAdGroup(insertionIndex, adGroupInsertionPositionUs).withIsServerSideInserted(insertionIndex, true).withAdCount(insertionIndex, adDurationsUs.length).withAdDurationsUs(insertionIndex, adDurationsUs).withContentResumeOffsetUs(insertionIndex, contentResumeOffsetUs);
        for (int adIndex = 0; adIndex < adDurationsUs.length && adDurationsUs[adIndex] == 0; adIndex++) {
            adPlaybackState2 = adPlaybackState2.withSkippedAd(insertionIndex, adIndex);
        }
        return correctFollowingAdGroupTimes(adPlaybackState2, insertionIndex, Util.sum(adDurationsUs), contentResumeOffsetUs);
    }

    public static long getStreamPositionUs(Player player, AdPlaybackState adPlaybackState) {
        Timeline timeline = player.getCurrentTimeline();
        if (timeline.isEmpty()) {
            return C.TIME_UNSET;
        }
        Timeline.Period period = timeline.getPeriod(player.getCurrentPeriodIndex(), new Timeline.Period());
        if (!Util.areEqual(period.getAdsId(), adPlaybackState.adsId)) {
            return C.TIME_UNSET;
        }
        if (player.isPlayingAd()) {
            int adGroupIndex = player.getCurrentAdGroupIndex();
            int adIndexInAdGroup = player.getCurrentAdIndexInAdGroup();
            long adPositionUs = Util.msToUs(player.getCurrentPosition());
            return getStreamPositionUsForAd(adPositionUs, adGroupIndex, adIndexInAdGroup, adPlaybackState);
        }
        long periodPositionUs = Util.msToUs(player.getCurrentPosition()) - period.getPositionInWindowUs();
        return getStreamPositionUsForContent(periodPositionUs, -1, adPlaybackState);
    }

    public static long getStreamPositionUs(long positionUs, MediaSource.MediaPeriodId mediaPeriodId, AdPlaybackState adPlaybackState) {
        if (mediaPeriodId.isAd()) {
            return getStreamPositionUsForAd(positionUs, mediaPeriodId.adGroupIndex, mediaPeriodId.adIndexInAdGroup, adPlaybackState);
        }
        return getStreamPositionUsForContent(positionUs, mediaPeriodId.nextAdGroupIndex, adPlaybackState);
    }

    public static long getMediaPeriodPositionUs(long positionUs, MediaSource.MediaPeriodId mediaPeriodId, AdPlaybackState adPlaybackState) {
        if (mediaPeriodId.isAd()) {
            return getMediaPeriodPositionUsForAd(positionUs, mediaPeriodId.adGroupIndex, mediaPeriodId.adIndexInAdGroup, adPlaybackState);
        }
        return getMediaPeriodPositionUsForContent(positionUs, mediaPeriodId.nextAdGroupIndex, adPlaybackState);
    }

    public static long getStreamPositionUsForAd(long positionUs, int adGroupIndex, int adIndexInAdGroup, AdPlaybackState adPlaybackState) {
        AdPlaybackState.AdGroup currentAdGroup = adPlaybackState.getAdGroup(adGroupIndex);
        long positionUs2 = positionUs + currentAdGroup.timeUs;
        for (int i = adPlaybackState.removedAdGroupCount; i < adGroupIndex; i++) {
            AdPlaybackState.AdGroup adGroup = adPlaybackState.getAdGroup(i);
            for (int j = 0; j < getAdCountInGroup(adPlaybackState, i); j++) {
                positionUs2 += adGroup.durationsUs[j];
            }
            positionUs2 -= adGroup.contentResumeOffsetUs;
        }
        int i2 = getAdCountInGroup(adPlaybackState, adGroupIndex);
        if (adIndexInAdGroup < i2) {
            for (int i3 = 0; i3 < adIndexInAdGroup; i3++) {
                positionUs2 += currentAdGroup.durationsUs[i3];
            }
        }
        return positionUs2;
    }

    public static long getMediaPeriodPositionUsForAd(long positionUs, int adGroupIndex, int adIndexInAdGroup, AdPlaybackState adPlaybackState) {
        AdPlaybackState.AdGroup currentAdGroup = adPlaybackState.getAdGroup(adGroupIndex);
        long positionUs2 = positionUs - currentAdGroup.timeUs;
        for (int i = adPlaybackState.removedAdGroupCount; i < adGroupIndex; i++) {
            AdPlaybackState.AdGroup adGroup = adPlaybackState.getAdGroup(i);
            for (int j = 0; j < getAdCountInGroup(adPlaybackState, i); j++) {
                positionUs2 -= adGroup.durationsUs[j];
            }
            positionUs2 += adGroup.contentResumeOffsetUs;
        }
        int i2 = getAdCountInGroup(adPlaybackState, adGroupIndex);
        if (adIndexInAdGroup < i2) {
            for (int i3 = 0; i3 < adIndexInAdGroup; i3++) {
                positionUs2 -= currentAdGroup.durationsUs[i3];
            }
        }
        return positionUs2;
    }

    public static long getStreamPositionUsForContent(long positionUs, int nextAdGroupIndex, AdPlaybackState adPlaybackState) {
        long totalAdDurationBeforePositionUs = 0;
        if (nextAdGroupIndex == -1) {
            nextAdGroupIndex = adPlaybackState.adGroupCount;
        }
        for (int i = adPlaybackState.removedAdGroupCount; i < nextAdGroupIndex; i++) {
            AdPlaybackState.AdGroup adGroup = adPlaybackState.getAdGroup(i);
            if (adGroup.timeUs == Long.MIN_VALUE || adGroup.timeUs > positionUs) {
                break;
            }
            long adGroupStreamStartPositionUs = adGroup.timeUs + totalAdDurationBeforePositionUs;
            for (int j = 0; j < getAdCountInGroup(adPlaybackState, i); j++) {
                totalAdDurationBeforePositionUs += adGroup.durationsUs[j];
            }
            totalAdDurationBeforePositionUs -= adGroup.contentResumeOffsetUs;
            long adGroupResumePositionUs = adGroup.timeUs + adGroup.contentResumeOffsetUs;
            if (adGroupResumePositionUs > positionUs) {
                return Math.max(adGroupStreamStartPositionUs, positionUs + totalAdDurationBeforePositionUs);
            }
        }
        return positionUs + totalAdDurationBeforePositionUs;
    }

    public static long getMediaPeriodPositionUsForContent(long positionUs, int nextAdGroupIndex, AdPlaybackState adPlaybackState) {
        long totalAdDurationBeforePositionUs = 0;
        if (nextAdGroupIndex == -1) {
            nextAdGroupIndex = adPlaybackState.adGroupCount;
        }
        for (int i = adPlaybackState.removedAdGroupCount; i < nextAdGroupIndex; i++) {
            AdPlaybackState.AdGroup adGroup = adPlaybackState.getAdGroup(i);
            if (adGroup.timeUs == Long.MIN_VALUE || adGroup.timeUs > positionUs - totalAdDurationBeforePositionUs) {
                break;
            }
            for (int j = 0; j < getAdCountInGroup(adPlaybackState, i); j++) {
                totalAdDurationBeforePositionUs += adGroup.durationsUs[j];
            }
            totalAdDurationBeforePositionUs -= adGroup.contentResumeOffsetUs;
            long adGroupResumePositionUs = adGroup.timeUs + adGroup.contentResumeOffsetUs;
            if (adGroupResumePositionUs > positionUs - totalAdDurationBeforePositionUs) {
                return Math.max(adGroup.timeUs, positionUs - totalAdDurationBeforePositionUs);
            }
        }
        return positionUs - totalAdDurationBeforePositionUs;
    }

    public static int getAdCountInGroup(AdPlaybackState adPlaybackState, int adGroupIndex) {
        AdPlaybackState.AdGroup adGroup = adPlaybackState.getAdGroup(adGroupIndex);
        if (adGroup.count == -1) {
            return 0;
        }
        return adGroup.count;
    }

    private static AdPlaybackState correctFollowingAdGroupTimes(AdPlaybackState adPlaybackState, int adGroupInsertionIndex, long insertedAdDurationUs, long addedContentResumeOffsetUs) {
        long followingAdGroupTimeUsOffset = (-insertedAdDurationUs) + addedContentResumeOffsetUs;
        for (int i = adGroupInsertionIndex + 1; i < adPlaybackState.adGroupCount; i++) {
            long adGroupTimeUs = adPlaybackState.getAdGroup(i).timeUs;
            if (adGroupTimeUs != Long.MIN_VALUE) {
                adPlaybackState = adPlaybackState.withAdGroupTimeUs(i, adGroupTimeUs + followingAdGroupTimeUsOffset);
            }
        }
        return adPlaybackState;
    }
}
