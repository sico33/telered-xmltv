package androidx.media3.common;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Pair;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Util;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class Timeline {
    public static final Timeline EMPTY = new Timeline() { // from class: androidx.media3.common.Timeline.1
        @Override // androidx.media3.common.Timeline
        public int getWindowCount() {
            return 0;
        }

        @Override // androidx.media3.common.Timeline
        public Window getWindow(int windowIndex, Window window, long defaultPositionProjectionUs) {
            throw new IndexOutOfBoundsException();
        }

        @Override // androidx.media3.common.Timeline
        public int getPeriodCount() {
            return 0;
        }

        @Override // androidx.media3.common.Timeline
        public Period getPeriod(int periodIndex, Period period, boolean setIds) {
            throw new IndexOutOfBoundsException();
        }

        @Override // androidx.media3.common.Timeline
        public int getIndexOfPeriod(Object uid) {
            return -1;
        }

        @Override // androidx.media3.common.Timeline
        public Object getUidOfPeriod(int periodIndex) {
            throw new IndexOutOfBoundsException();
        }
    };
    private static final String FIELD_WINDOWS = Util.intToStringMaxRadix(0);
    private static final String FIELD_PERIODS = Util.intToStringMaxRadix(1);
    private static final String FIELD_SHUFFLED_WINDOW_INDICES = Util.intToStringMaxRadix(2);

    public abstract int getIndexOfPeriod(Object obj);

    public abstract Period getPeriod(int i, Period period, boolean z);

    public abstract int getPeriodCount();

    public abstract Object getUidOfPeriod(int i);

    public abstract Window getWindow(int i, Window window, long j);

    public abstract int getWindowCount();

    public static final class Window {
        public long defaultPositionUs;
        public long durationUs;
        public long elapsedRealtimeEpochOffsetMs;
        public int firstPeriodIndex;
        public boolean isDynamic;
        public boolean isPlaceholder;
        public boolean isSeekable;
        public int lastPeriodIndex;
        public MediaItem.LiveConfiguration liveConfiguration;
        public Object manifest;
        public long positionInFirstPeriodUs;
        public long presentationStartTimeMs;

        @Deprecated
        public Object tag;
        public long windowStartTimeMs;
        public static final Object SINGLE_WINDOW_UID = new Object();
        private static final Object FAKE_WINDOW_UID = new Object();
        private static final MediaItem PLACEHOLDER_MEDIA_ITEM = new MediaItem.Builder().setMediaId("androidx.media3.common.Timeline").setUri(Uri.EMPTY).build();
        private static final String FIELD_MEDIA_ITEM = Util.intToStringMaxRadix(1);
        private static final String FIELD_PRESENTATION_START_TIME_MS = Util.intToStringMaxRadix(2);
        private static final String FIELD_WINDOW_START_TIME_MS = Util.intToStringMaxRadix(3);
        private static final String FIELD_ELAPSED_REALTIME_EPOCH_OFFSET_MS = Util.intToStringMaxRadix(4);
        private static final String FIELD_IS_SEEKABLE = Util.intToStringMaxRadix(5);
        private static final String FIELD_IS_DYNAMIC = Util.intToStringMaxRadix(6);
        private static final String FIELD_LIVE_CONFIGURATION = Util.intToStringMaxRadix(7);
        private static final String FIELD_IS_PLACEHOLDER = Util.intToStringMaxRadix(8);
        private static final String FIELD_DEFAULT_POSITION_US = Util.intToStringMaxRadix(9);
        private static final String FIELD_DURATION_US = Util.intToStringMaxRadix(10);
        private static final String FIELD_FIRST_PERIOD_INDEX = Util.intToStringMaxRadix(11);
        private static final String FIELD_LAST_PERIOD_INDEX = Util.intToStringMaxRadix(12);
        private static final String FIELD_POSITION_IN_FIRST_PERIOD_US = Util.intToStringMaxRadix(13);
        public Object uid = SINGLE_WINDOW_UID;
        public MediaItem mediaItem = PLACEHOLDER_MEDIA_ITEM;

        public Window set(Object uid, MediaItem mediaItem, Object manifest, long presentationStartTimeMs, long windowStartTimeMs, long elapsedRealtimeEpochOffsetMs, boolean isSeekable, boolean isDynamic, MediaItem.LiveConfiguration liveConfiguration, long defaultPositionUs, long durationUs, int firstPeriodIndex, int lastPeriodIndex, long positionInFirstPeriodUs) {
            Object obj;
            this.uid = uid;
            this.mediaItem = mediaItem != null ? mediaItem : PLACEHOLDER_MEDIA_ITEM;
            if (mediaItem != null && mediaItem.localConfiguration != null) {
                obj = mediaItem.localConfiguration.tag;
            } else {
                obj = null;
            }
            this.tag = obj;
            this.manifest = manifest;
            this.presentationStartTimeMs = presentationStartTimeMs;
            this.windowStartTimeMs = windowStartTimeMs;
            this.elapsedRealtimeEpochOffsetMs = elapsedRealtimeEpochOffsetMs;
            this.isSeekable = isSeekable;
            this.isDynamic = isDynamic;
            this.liveConfiguration = liveConfiguration;
            this.defaultPositionUs = defaultPositionUs;
            this.durationUs = durationUs;
            this.firstPeriodIndex = firstPeriodIndex;
            this.lastPeriodIndex = lastPeriodIndex;
            this.positionInFirstPeriodUs = positionInFirstPeriodUs;
            this.isPlaceholder = false;
            return this;
        }

        public long getDefaultPositionMs() {
            return Util.usToMs(this.defaultPositionUs);
        }

        public long getDefaultPositionUs() {
            return this.defaultPositionUs;
        }

        public long getDurationMs() {
            return Util.usToMs(this.durationUs);
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public long getPositionInFirstPeriodMs() {
            return Util.usToMs(this.positionInFirstPeriodUs);
        }

        public long getPositionInFirstPeriodUs() {
            return this.positionInFirstPeriodUs;
        }

        public long getCurrentUnixTimeMs() {
            return Util.getNowUnixTimeMs(this.elapsedRealtimeEpochOffsetMs);
        }

        public boolean isLive() {
            return this.liveConfiguration != null;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !getClass().equals(obj.getClass())) {
                return false;
            }
            Window that = (Window) obj;
            if (Util.areEqual(this.uid, that.uid) && Util.areEqual(this.mediaItem, that.mediaItem) && Util.areEqual(this.manifest, that.manifest) && Util.areEqual(this.liveConfiguration, that.liveConfiguration) && this.presentationStartTimeMs == that.presentationStartTimeMs && this.windowStartTimeMs == that.windowStartTimeMs && this.elapsedRealtimeEpochOffsetMs == that.elapsedRealtimeEpochOffsetMs && this.isSeekable == that.isSeekable && this.isDynamic == that.isDynamic && this.isPlaceholder == that.isPlaceholder && this.defaultPositionUs == that.defaultPositionUs && this.durationUs == that.durationUs && this.firstPeriodIndex == that.firstPeriodIndex && this.lastPeriodIndex == that.lastPeriodIndex && this.positionInFirstPeriodUs == that.positionInFirstPeriodUs) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (((((((((((((((((((((((((((((7 * 31) + this.uid.hashCode()) * 31) + this.mediaItem.hashCode()) * 31) + (this.manifest == null ? 0 : this.manifest.hashCode())) * 31) + (this.liveConfiguration != null ? this.liveConfiguration.hashCode() : 0)) * 31) + ((int) (this.presentationStartTimeMs ^ (this.presentationStartTimeMs >>> 32)))) * 31) + ((int) (this.windowStartTimeMs ^ (this.windowStartTimeMs >>> 32)))) * 31) + ((int) (this.elapsedRealtimeEpochOffsetMs ^ (this.elapsedRealtimeEpochOffsetMs >>> 32)))) * 31) + (this.isSeekable ? 1 : 0)) * 31) + (this.isDynamic ? 1 : 0)) * 31) + (this.isPlaceholder ? 1 : 0)) * 31) + ((int) (this.defaultPositionUs ^ (this.defaultPositionUs >>> 32)))) * 31) + ((int) (this.durationUs ^ (this.durationUs >>> 32)))) * 31) + this.firstPeriodIndex) * 31) + this.lastPeriodIndex) * 31) + ((int) (this.positionInFirstPeriodUs ^ (this.positionInFirstPeriodUs >>> 32)));
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            if (!MediaItem.EMPTY.equals(this.mediaItem)) {
                bundle.putBundle(FIELD_MEDIA_ITEM, this.mediaItem.toBundle());
            }
            if (this.presentationStartTimeMs != C.TIME_UNSET) {
                bundle.putLong(FIELD_PRESENTATION_START_TIME_MS, this.presentationStartTimeMs);
            }
            if (this.windowStartTimeMs != C.TIME_UNSET) {
                bundle.putLong(FIELD_WINDOW_START_TIME_MS, this.windowStartTimeMs);
            }
            if (this.elapsedRealtimeEpochOffsetMs != C.TIME_UNSET) {
                bundle.putLong(FIELD_ELAPSED_REALTIME_EPOCH_OFFSET_MS, this.elapsedRealtimeEpochOffsetMs);
            }
            if (this.isSeekable) {
                bundle.putBoolean(FIELD_IS_SEEKABLE, this.isSeekable);
            }
            if (this.isDynamic) {
                bundle.putBoolean(FIELD_IS_DYNAMIC, this.isDynamic);
            }
            MediaItem.LiveConfiguration liveConfiguration = this.liveConfiguration;
            if (liveConfiguration != null) {
                bundle.putBundle(FIELD_LIVE_CONFIGURATION, liveConfiguration.toBundle());
            }
            if (this.isPlaceholder) {
                bundle.putBoolean(FIELD_IS_PLACEHOLDER, this.isPlaceholder);
            }
            if (this.defaultPositionUs != 0) {
                bundle.putLong(FIELD_DEFAULT_POSITION_US, this.defaultPositionUs);
            }
            if (this.durationUs != C.TIME_UNSET) {
                bundle.putLong(FIELD_DURATION_US, this.durationUs);
            }
            if (this.firstPeriodIndex != 0) {
                bundle.putInt(FIELD_FIRST_PERIOD_INDEX, this.firstPeriodIndex);
            }
            if (this.lastPeriodIndex != 0) {
                bundle.putInt(FIELD_LAST_PERIOD_INDEX, this.lastPeriodIndex);
            }
            if (this.positionInFirstPeriodUs != 0) {
                bundle.putLong(FIELD_POSITION_IN_FIRST_PERIOD_US, this.positionInFirstPeriodUs);
            }
            return bundle;
        }

        public static Window fromBundle(Bundle bundle) {
            MediaItem.LiveConfiguration liveConfiguration;
            Bundle mediaItemBundle = bundle.getBundle(FIELD_MEDIA_ITEM);
            MediaItem mediaItem = mediaItemBundle != null ? MediaItem.fromBundle(mediaItemBundle) : MediaItem.EMPTY;
            long presentationStartTimeMs = bundle.getLong(FIELD_PRESENTATION_START_TIME_MS, C.TIME_UNSET);
            long windowStartTimeMs = bundle.getLong(FIELD_WINDOW_START_TIME_MS, C.TIME_UNSET);
            long elapsedRealtimeEpochOffsetMs = bundle.getLong(FIELD_ELAPSED_REALTIME_EPOCH_OFFSET_MS, C.TIME_UNSET);
            boolean isSeekable = bundle.getBoolean(FIELD_IS_SEEKABLE, false);
            boolean isDynamic = bundle.getBoolean(FIELD_IS_DYNAMIC, false);
            Bundle liveConfigurationBundle = bundle.getBundle(FIELD_LIVE_CONFIGURATION);
            if (liveConfigurationBundle != null) {
                liveConfiguration = MediaItem.LiveConfiguration.fromBundle(liveConfigurationBundle);
            } else {
                liveConfiguration = null;
            }
            boolean isPlaceHolder = bundle.getBoolean(FIELD_IS_PLACEHOLDER, false);
            long defaultPositionUs = bundle.getLong(FIELD_DEFAULT_POSITION_US, 0L);
            long durationUs = bundle.getLong(FIELD_DURATION_US, C.TIME_UNSET);
            int firstPeriodIndex = bundle.getInt(FIELD_FIRST_PERIOD_INDEX, 0);
            int lastPeriodIndex = bundle.getInt(FIELD_LAST_PERIOD_INDEX, 0);
            long positionInFirstPeriodUs = bundle.getLong(FIELD_POSITION_IN_FIRST_PERIOD_US, 0L);
            Window window = new Window();
            window.set(FAKE_WINDOW_UID, mediaItem, null, presentationStartTimeMs, windowStartTimeMs, elapsedRealtimeEpochOffsetMs, isSeekable, isDynamic, liveConfiguration, defaultPositionUs, durationUs, firstPeriodIndex, lastPeriodIndex, positionInFirstPeriodUs);
            window.isPlaceholder = isPlaceHolder;
            return window;
        }
    }

    public static final class Period {
        private AdPlaybackState adPlaybackState = AdPlaybackState.NONE;
        public long durationUs;
        public Object id;
        public boolean isPlaceholder;
        public long positionInWindowUs;
        public Object uid;
        public int windowIndex;
        private static final String FIELD_WINDOW_INDEX = Util.intToStringMaxRadix(0);
        private static final String FIELD_DURATION_US = Util.intToStringMaxRadix(1);
        private static final String FIELD_POSITION_IN_WINDOW_US = Util.intToStringMaxRadix(2);
        private static final String FIELD_PLACEHOLDER = Util.intToStringMaxRadix(3);
        private static final String FIELD_AD_PLAYBACK_STATE = Util.intToStringMaxRadix(4);

        public Period set(Object id, Object uid, int windowIndex, long durationUs, long positionInWindowUs) {
            return set(id, uid, windowIndex, durationUs, positionInWindowUs, AdPlaybackState.NONE, false);
        }

        public Period set(Object id, Object uid, int windowIndex, long durationUs, long positionInWindowUs, AdPlaybackState adPlaybackState, boolean isPlaceholder) {
            this.id = id;
            this.uid = uid;
            this.windowIndex = windowIndex;
            this.durationUs = durationUs;
            this.positionInWindowUs = positionInWindowUs;
            this.adPlaybackState = adPlaybackState;
            this.isPlaceholder = isPlaceholder;
            return this;
        }

        public long getDurationMs() {
            return Util.usToMs(this.durationUs);
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public long getPositionInWindowMs() {
            return Util.usToMs(this.positionInWindowUs);
        }

        public long getPositionInWindowUs() {
            return this.positionInWindowUs;
        }

        public Object getAdsId() {
            return this.adPlaybackState.adsId;
        }

        public int getAdGroupCount() {
            return this.adPlaybackState.adGroupCount;
        }

        public int getRemovedAdGroupCount() {
            return this.adPlaybackState.removedAdGroupCount;
        }

        public long getAdGroupTimeUs(int adGroupIndex) {
            return this.adPlaybackState.getAdGroup(adGroupIndex).timeUs;
        }

        public int getFirstAdIndexToPlay(int adGroupIndex) {
            return this.adPlaybackState.getAdGroup(adGroupIndex).getFirstAdIndexToPlay();
        }

        public int getNextAdIndexToPlay(int adGroupIndex, int lastPlayedAdIndex) {
            return this.adPlaybackState.getAdGroup(adGroupIndex).getNextAdIndexToPlay(lastPlayedAdIndex);
        }

        public boolean hasPlayedAdGroup(int adGroupIndex) {
            return !this.adPlaybackState.getAdGroup(adGroupIndex).hasUnplayedAds();
        }

        public int getAdGroupIndexForPositionUs(long positionUs) {
            return this.adPlaybackState.getAdGroupIndexForPositionUs(positionUs, this.durationUs);
        }

        public int getAdGroupIndexAfterPositionUs(long positionUs) {
            return this.adPlaybackState.getAdGroupIndexAfterPositionUs(positionUs, this.durationUs);
        }

        public int getAdCountInAdGroup(int adGroupIndex) {
            return this.adPlaybackState.getAdGroup(adGroupIndex).count;
        }

        public long getAdDurationUs(int adGroupIndex, int adIndexInAdGroup) {
            AdPlaybackState.AdGroup adGroup = this.adPlaybackState.getAdGroup(adGroupIndex);
            return adGroup.count != -1 ? adGroup.durationsUs[adIndexInAdGroup] : C.TIME_UNSET;
        }

        public int getAdState(int adGroupIndex, int adIndexInAdGroup) {
            AdPlaybackState.AdGroup adGroup = this.adPlaybackState.getAdGroup(adGroupIndex);
            if (adGroup.count != -1) {
                return adGroup.states[adIndexInAdGroup];
            }
            return 0;
        }

        public boolean isLivePostrollPlaceholder(int adGroupIndex) {
            return adGroupIndex == getAdGroupCount() - 1 && this.adPlaybackState.isLivePostrollPlaceholder(adGroupIndex);
        }

        public long getAdResumePositionUs() {
            return this.adPlaybackState.adResumePositionUs;
        }

        public boolean isServerSideInsertedAdGroup(int adGroupIndex) {
            return this.adPlaybackState.getAdGroup(adGroupIndex).isServerSideInserted;
        }

        public long getContentResumeOffsetUs(int adGroupIndex) {
            return this.adPlaybackState.getAdGroup(adGroupIndex).contentResumeOffsetUs;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !getClass().equals(obj.getClass())) {
                return false;
            }
            Period that = (Period) obj;
            if (Util.areEqual(this.id, that.id) && Util.areEqual(this.uid, that.uid) && this.windowIndex == that.windowIndex && this.durationUs == that.durationUs && this.positionInWindowUs == that.positionInWindowUs && this.isPlaceholder == that.isPlaceholder && Util.areEqual(this.adPlaybackState, that.adPlaybackState)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (((((((((((((7 * 31) + (this.id == null ? 0 : this.id.hashCode())) * 31) + (this.uid != null ? this.uid.hashCode() : 0)) * 31) + this.windowIndex) * 31) + ((int) (this.durationUs ^ (this.durationUs >>> 32)))) * 31) + ((int) (this.positionInWindowUs ^ (this.positionInWindowUs >>> 32)))) * 31) + (this.isPlaceholder ? 1 : 0)) * 31) + this.adPlaybackState.hashCode();
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            if (this.windowIndex != 0) {
                bundle.putInt(FIELD_WINDOW_INDEX, this.windowIndex);
            }
            if (this.durationUs != C.TIME_UNSET) {
                bundle.putLong(FIELD_DURATION_US, this.durationUs);
            }
            if (this.positionInWindowUs != 0) {
                bundle.putLong(FIELD_POSITION_IN_WINDOW_US, this.positionInWindowUs);
            }
            if (this.isPlaceholder) {
                bundle.putBoolean(FIELD_PLACEHOLDER, this.isPlaceholder);
            }
            if (!this.adPlaybackState.equals(AdPlaybackState.NONE)) {
                bundle.putBundle(FIELD_AD_PLAYBACK_STATE, this.adPlaybackState.toBundle());
            }
            return bundle;
        }

        public static Period fromBundle(Bundle bundle) {
            AdPlaybackState adPlaybackState;
            int windowIndex = bundle.getInt(FIELD_WINDOW_INDEX, 0);
            long durationUs = bundle.getLong(FIELD_DURATION_US, C.TIME_UNSET);
            long positionInWindowUs = bundle.getLong(FIELD_POSITION_IN_WINDOW_US, 0L);
            boolean isPlaceholder = bundle.getBoolean(FIELD_PLACEHOLDER, false);
            Bundle adPlaybackStateBundle = bundle.getBundle(FIELD_AD_PLAYBACK_STATE);
            if (adPlaybackStateBundle != null) {
                adPlaybackState = AdPlaybackState.fromBundle(adPlaybackStateBundle);
            } else {
                adPlaybackState = AdPlaybackState.NONE;
            }
            Period period = new Period();
            period.set(null, null, windowIndex, durationUs, positionInWindowUs, adPlaybackState, isPlaceholder);
            return period;
        }
    }

    protected Timeline() {
    }

    public final boolean isEmpty() {
        return getWindowCount() == 0;
    }

    public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
        switch (repeatMode) {
            case 0:
                if (windowIndex == getLastWindowIndex(shuffleModeEnabled)) {
                    return -1;
                }
                return windowIndex + 1;
            case 1:
                return windowIndex;
            case 2:
                if (windowIndex == getLastWindowIndex(shuffleModeEnabled)) {
                    return getFirstWindowIndex(shuffleModeEnabled);
                }
                return windowIndex + 1;
            default:
                throw new IllegalStateException();
        }
    }

    public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
        switch (repeatMode) {
            case 0:
                if (windowIndex == getFirstWindowIndex(shuffleModeEnabled)) {
                    return -1;
                }
                return windowIndex - 1;
            case 1:
                return windowIndex;
            case 2:
                if (windowIndex == getFirstWindowIndex(shuffleModeEnabled)) {
                    return getLastWindowIndex(shuffleModeEnabled);
                }
                return windowIndex - 1;
            default:
                throw new IllegalStateException();
        }
    }

    public int getLastWindowIndex(boolean shuffleModeEnabled) {
        if (isEmpty()) {
            return -1;
        }
        return getWindowCount() - 1;
    }

    public int getFirstWindowIndex(boolean shuffleModeEnabled) {
        return isEmpty() ? -1 : 0;
    }

    public final Window getWindow(int windowIndex, Window window) {
        return getWindow(windowIndex, window, 0L);
    }

    public final int getNextPeriodIndex(int periodIndex, Period period, Window window, int repeatMode, boolean shuffleModeEnabled) {
        int windowIndex = getPeriod(periodIndex, period).windowIndex;
        if (getWindow(windowIndex, window).lastPeriodIndex == periodIndex) {
            int nextWindowIndex = getNextWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
            if (nextWindowIndex == -1) {
                return -1;
            }
            return getWindow(nextWindowIndex, window).firstPeriodIndex;
        }
        return periodIndex + 1;
    }

    public final boolean isLastPeriod(int periodIndex, Period period, Window window, int repeatMode, boolean shuffleModeEnabled) {
        return getNextPeriodIndex(periodIndex, period, window, repeatMode, shuffleModeEnabled) == -1;
    }

    @Deprecated
    public final Pair<Object, Long> getPeriodPosition(Window window, Period period, int windowIndex, long windowPositionUs) {
        return getPeriodPositionUs(window, period, windowIndex, windowPositionUs);
    }

    @Deprecated
    public final Pair<Object, Long> getPeriodPosition(Window window, Period period, int windowIndex, long windowPositionUs, long defaultPositionProjectionUs) {
        return getPeriodPositionUs(window, period, windowIndex, windowPositionUs, defaultPositionProjectionUs);
    }

    public final Pair<Object, Long> getPeriodPositionUs(Window window, Period period, int windowIndex, long windowPositionUs) {
        return (Pair) Assertions.checkNotNull(getPeriodPositionUs(window, period, windowIndex, windowPositionUs, 0L));
    }

    public final Pair<Object, Long> getPeriodPositionUs(Window window, Period period, int windowIndex, long windowPositionUs, long defaultPositionProjectionUs) {
        Assertions.checkIndex(windowIndex, 0, getWindowCount());
        getWindow(windowIndex, window, defaultPositionProjectionUs);
        if (windowPositionUs == C.TIME_UNSET) {
            windowPositionUs = window.getDefaultPositionUs();
            if (windowPositionUs == C.TIME_UNSET) {
                return null;
            }
        }
        int periodIndex = window.firstPeriodIndex;
        getPeriod(periodIndex, period);
        while (periodIndex < window.lastPeriodIndex && period.positionInWindowUs != windowPositionUs && getPeriod(periodIndex + 1, period).positionInWindowUs <= windowPositionUs) {
            periodIndex++;
        }
        getPeriod(periodIndex, period, true);
        long periodPositionUs = windowPositionUs - period.positionInWindowUs;
        if (period.durationUs != C.TIME_UNSET) {
            periodPositionUs = Math.min(periodPositionUs, period.durationUs - 1);
        }
        return Pair.create(Assertions.checkNotNull(period.uid), Long.valueOf(Math.max(0L, periodPositionUs)));
    }

    public Period getPeriodByUid(Object periodUid, Period period) {
        return getPeriod(getIndexOfPeriod(periodUid), period, true);
    }

    public final Period getPeriod(int periodIndex, Period period) {
        return getPeriod(periodIndex, period, false);
    }

    public boolean equals(Object obj) {
        int lastWindowIndex;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Timeline)) {
            return false;
        }
        Timeline other = (Timeline) obj;
        if (other.getWindowCount() != getWindowCount() || other.getPeriodCount() != getPeriodCount()) {
            return false;
        }
        Window window = new Window();
        Period period = new Period();
        Window otherWindow = new Window();
        Period otherPeriod = new Period();
        for (int i = 0; i < getWindowCount(); i++) {
            if (!getWindow(i, window).equals(other.getWindow(i, otherWindow))) {
                return false;
            }
        }
        for (int i2 = 0; i2 < getPeriodCount(); i2++) {
            if (!getPeriod(i2, period, true).equals(other.getPeriod(i2, otherPeriod, true))) {
                return false;
            }
        }
        int windowIndex = getFirstWindowIndex(true);
        if (windowIndex != other.getFirstWindowIndex(true) || (lastWindowIndex = getLastWindowIndex(true)) != other.getLastWindowIndex(true)) {
            return false;
        }
        while (windowIndex != lastWindowIndex) {
            int nextWindowIndex = getNextWindowIndex(windowIndex, 0, true);
            if (nextWindowIndex != other.getNextWindowIndex(windowIndex, 0, true)) {
                return false;
            }
            windowIndex = nextWindowIndex;
        }
        return true;
    }

    public int hashCode() {
        Window window = new Window();
        Period period = new Period();
        int result = (7 * 31) + getWindowCount();
        for (int i = 0; i < getWindowCount(); i++) {
            result = (result * 31) + getWindow(i, window).hashCode();
        }
        int i2 = result * 31;
        int result2 = i2 + getPeriodCount();
        for (int i3 = 0; i3 < getPeriodCount(); i3++) {
            result2 = (result2 * 31) + getPeriod(i3, period, true).hashCode();
        }
        int windowIndex = getFirstWindowIndex(true);
        while (windowIndex != -1) {
            result2 = (result2 * 31) + windowIndex;
            windowIndex = getNextWindowIndex(windowIndex, 0, true);
        }
        return result2;
    }

    public final Bundle toBundle() {
        List<Bundle> windowBundles = new ArrayList<>();
        int windowCount = getWindowCount();
        Window window = new Window();
        for (int i = 0; i < windowCount; i++) {
            windowBundles.add(getWindow(i, window, 0L).toBundle());
        }
        List<Bundle> periodBundles = new ArrayList<>();
        int periodCount = getPeriodCount();
        Period period = new Period();
        for (int i2 = 0; i2 < periodCount; i2++) {
            periodBundles.add(getPeriod(i2, period, false).toBundle());
        }
        int[] shuffledWindowIndices = new int[windowCount];
        if (windowCount > 0) {
            shuffledWindowIndices[0] = getFirstWindowIndex(true);
        }
        for (int i3 = 1; i3 < windowCount; i3++) {
            shuffledWindowIndices[i3] = getNextWindowIndex(shuffledWindowIndices[i3 - 1], 0, true);
        }
        Bundle bundle = new Bundle();
        bundle.putBinder(FIELD_WINDOWS, new BundleListRetriever(windowBundles));
        bundle.putBinder(FIELD_PERIODS, new BundleListRetriever(periodBundles));
        bundle.putIntArray(FIELD_SHUFFLED_WINDOW_INDICES, shuffledWindowIndices);
        return bundle;
    }

    public final Timeline copyWithSingleWindow(int windowIndex) {
        if (getWindowCount() == 1) {
            return this;
        }
        Window window = getWindow(windowIndex, new Window(), 0L);
        ImmutableList.Builder<Period> periods = ImmutableList.builder();
        for (int i = window.firstPeriodIndex; i <= window.lastPeriodIndex; i++) {
            Period period = getPeriod(i, new Period(), true);
            period.windowIndex = 0;
            periods.add(period);
        }
        window.lastPeriodIndex -= window.firstPeriodIndex;
        window.firstPeriodIndex = 0;
        return new RemotableTimeline(ImmutableList.of(window), periods.build(), new int[]{0});
    }

    public static Timeline fromBundle(Bundle bundle) {
        int[] iArrGenerateUnshuffledIndices;
        ImmutableList<Window> windows = fromBundleListRetriever(new Function() { // from class: androidx.media3.common.Timeline$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return Timeline.Window.fromBundle((Bundle) obj);
            }
        }, bundle.getBinder(FIELD_WINDOWS));
        ImmutableList<Period> periods = fromBundleListRetriever(new Function() { // from class: androidx.media3.common.Timeline$$ExternalSyntheticLambda1
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return Timeline.Period.fromBundle((Bundle) obj);
            }
        }, bundle.getBinder(FIELD_PERIODS));
        int[] shuffledWindowIndices = bundle.getIntArray(FIELD_SHUFFLED_WINDOW_INDICES);
        if (shuffledWindowIndices == null) {
            iArrGenerateUnshuffledIndices = generateUnshuffledIndices(windows.size());
        } else {
            iArrGenerateUnshuffledIndices = shuffledWindowIndices;
        }
        return new RemotableTimeline(windows, periods, iArrGenerateUnshuffledIndices);
    }

    private static <T> ImmutableList<T> fromBundleListRetriever(Function<Bundle, T> fromBundleFunc, IBinder binder) {
        if (binder == null) {
            return ImmutableList.of();
        }
        return BundleCollectionUtil.fromBundleList(fromBundleFunc, BundleListRetriever.getList(binder));
    }

    private static int[] generateUnshuffledIndices(int n) {
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        return indices;
    }

    public static final class RemotableTimeline extends Timeline {
        private final ImmutableList<Period> periods;
        private final int[] shuffledWindowIndices;
        private final int[] windowIndicesInShuffled;
        private final ImmutableList<Window> windows;

        public RemotableTimeline(ImmutableList<Window> windows, ImmutableList<Period> periods, int[] shuffledWindowIndices) {
            Assertions.checkArgument(windows.size() == shuffledWindowIndices.length);
            this.windows = windows;
            this.periods = periods;
            this.shuffledWindowIndices = shuffledWindowIndices;
            this.windowIndicesInShuffled = new int[shuffledWindowIndices.length];
            for (int i = 0; i < shuffledWindowIndices.length; i++) {
                this.windowIndicesInShuffled[shuffledWindowIndices[i]] = i;
            }
        }

        @Override // androidx.media3.common.Timeline
        public int getWindowCount() {
            return this.windows.size();
        }

        @Override // androidx.media3.common.Timeline
        public Window getWindow(int windowIndex, Window window, long defaultPositionProjectionUs) {
            Window w = this.windows.get(windowIndex);
            window.set(w.uid, w.mediaItem, w.manifest, w.presentationStartTimeMs, w.windowStartTimeMs, w.elapsedRealtimeEpochOffsetMs, w.isSeekable, w.isDynamic, w.liveConfiguration, w.defaultPositionUs, w.durationUs, w.firstPeriodIndex, w.lastPeriodIndex, w.positionInFirstPeriodUs);
            window.isPlaceholder = w.isPlaceholder;
            return window;
        }

        @Override // androidx.media3.common.Timeline
        public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            if (repeatMode == 1) {
                return windowIndex;
            }
            if (windowIndex == getLastWindowIndex(shuffleModeEnabled)) {
                if (repeatMode == 2) {
                    return getFirstWindowIndex(shuffleModeEnabled);
                }
                return -1;
            }
            if (shuffleModeEnabled) {
                return this.shuffledWindowIndices[this.windowIndicesInShuffled[windowIndex] + 1];
            }
            return windowIndex + 1;
        }

        @Override // androidx.media3.common.Timeline
        public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            if (repeatMode == 1) {
                return windowIndex;
            }
            if (windowIndex == getFirstWindowIndex(shuffleModeEnabled)) {
                if (repeatMode == 2) {
                    return getLastWindowIndex(shuffleModeEnabled);
                }
                return -1;
            }
            if (shuffleModeEnabled) {
                return this.shuffledWindowIndices[this.windowIndicesInShuffled[windowIndex] - 1];
            }
            return windowIndex - 1;
        }

        @Override // androidx.media3.common.Timeline
        public int getLastWindowIndex(boolean shuffleModeEnabled) {
            if (isEmpty()) {
                return -1;
            }
            if (shuffleModeEnabled) {
                return this.shuffledWindowIndices[getWindowCount() - 1];
            }
            return getWindowCount() - 1;
        }

        @Override // androidx.media3.common.Timeline
        public int getFirstWindowIndex(boolean shuffleModeEnabled) {
            if (isEmpty()) {
                return -1;
            }
            if (shuffleModeEnabled) {
                return this.shuffledWindowIndices[0];
            }
            return 0;
        }

        @Override // androidx.media3.common.Timeline
        public int getPeriodCount() {
            return this.periods.size();
        }

        @Override // androidx.media3.common.Timeline
        public Period getPeriod(int periodIndex, Period period, boolean setIds) {
            Period p = this.periods.get(periodIndex);
            period.set(p.id, p.uid, p.windowIndex, p.durationUs, p.positionInWindowUs, p.adPlaybackState, p.isPlaceholder);
            return period;
        }

        @Override // androidx.media3.common.Timeline
        public int getIndexOfPeriod(Object uid) {
            throw new UnsupportedOperationException();
        }

        @Override // androidx.media3.common.Timeline
        public Object getUidOfPeriod(int periodIndex) {
            throw new UnsupportedOperationException();
        }
    }
}
