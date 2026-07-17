package androidx.media3.exoplayer.trackselection;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.chunk.MediaChunk;
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AdaptiveTrackSelection extends BaseTrackSelection {
    public static final float DEFAULT_BANDWIDTH_FRACTION = 0.7f;
    public static final float DEFAULT_BUFFERED_FRACTION_TO_LIVE_EDGE_FOR_QUALITY_INCREASE = 0.75f;
    public static final int DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS = 25000;
    public static final int DEFAULT_MAX_HEIGHT_TO_DISCARD = 719;
    public static final int DEFAULT_MAX_WIDTH_TO_DISCARD = 1279;
    public static final int DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS = 10000;
    public static final int DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 25000;
    private static final long MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS = 1000;
    private static final String TAG = "AdaptiveTrackSelection";
    private final ImmutableList<AdaptationCheckpoint> adaptationCheckpoints;
    private final float bandwidthFraction;
    private final BandwidthMeter bandwidthMeter;
    private final float bufferedFractionToLiveEdgeForQualityIncrease;
    private final Clock clock;
    private MediaChunk lastBufferEvaluationMediaChunk;
    private long lastBufferEvaluationMs;
    private long latestBitrateEstimate;
    private final long maxDurationForQualityDecreaseUs;
    private final int maxHeightToDiscard;
    private final int maxWidthToDiscard;
    private final long minDurationForQualityIncreaseUs;
    private final long minDurationToRetainAfterDiscardUs;
    private float playbackSpeed;
    private int reason;
    private int selectedIndex;

    public static class Factory implements ExoTrackSelection.Factory {
        private final float bandwidthFraction;
        private final float bufferedFractionToLiveEdgeForQualityIncrease;
        private final Clock clock;
        private final int maxDurationForQualityDecreaseMs;
        private final int maxHeightToDiscard;
        private final int maxWidthToDiscard;
        private final int minDurationForQualityIncreaseMs;
        private final int minDurationToRetainAfterDiscardMs;

        public Factory() {
            this(10000, 25000, 25000, 0.7f);
        }

        public Factory(int minDurationForQualityIncreaseMs, int maxDurationForQualityDecreaseMs, int minDurationToRetainAfterDiscardMs, float bandwidthFraction) {
            this(minDurationForQualityIncreaseMs, maxDurationForQualityDecreaseMs, minDurationToRetainAfterDiscardMs, AdaptiveTrackSelection.DEFAULT_MAX_WIDTH_TO_DISCARD, AdaptiveTrackSelection.DEFAULT_MAX_HEIGHT_TO_DISCARD, bandwidthFraction, 0.75f, Clock.DEFAULT);
        }

        public Factory(int minDurationForQualityIncreaseMs, int maxDurationForQualityDecreaseMs, int minDurationToRetainAfterDiscardMs, int maxWidthToDiscard, int maxHeightToDiscard, float bandwidthFraction) {
            this(minDurationForQualityIncreaseMs, maxDurationForQualityDecreaseMs, minDurationToRetainAfterDiscardMs, maxWidthToDiscard, maxHeightToDiscard, bandwidthFraction, 0.75f, Clock.DEFAULT);
        }

        public Factory(int minDurationForQualityIncreaseMs, int maxDurationForQualityDecreaseMs, int minDurationToRetainAfterDiscardMs, float bandwidthFraction, float bufferedFractionToLiveEdgeForQualityIncrease, Clock clock) {
            this(minDurationForQualityIncreaseMs, maxDurationForQualityDecreaseMs, minDurationToRetainAfterDiscardMs, AdaptiveTrackSelection.DEFAULT_MAX_WIDTH_TO_DISCARD, AdaptiveTrackSelection.DEFAULT_MAX_HEIGHT_TO_DISCARD, bandwidthFraction, bufferedFractionToLiveEdgeForQualityIncrease, clock);
        }

        public Factory(int minDurationForQualityIncreaseMs, int maxDurationForQualityDecreaseMs, int minDurationToRetainAfterDiscardMs, int maxWidthToDiscard, int maxHeightToDiscard, float bandwidthFraction, float bufferedFractionToLiveEdgeForQualityIncrease, Clock clock) {
            this.minDurationForQualityIncreaseMs = minDurationForQualityIncreaseMs;
            this.maxDurationForQualityDecreaseMs = maxDurationForQualityDecreaseMs;
            this.minDurationToRetainAfterDiscardMs = minDurationToRetainAfterDiscardMs;
            this.maxWidthToDiscard = maxWidthToDiscard;
            this.maxHeightToDiscard = maxHeightToDiscard;
            this.bandwidthFraction = bandwidthFraction;
            this.bufferedFractionToLiveEdgeForQualityIncrease = bufferedFractionToLiveEdgeForQualityIncrease;
            this.clock = clock;
        }

        @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection.Factory
        public final ExoTrackSelection[] createTrackSelections(ExoTrackSelection.Definition[] definitions, BandwidthMeter bandwidthMeter, MediaSource.MediaPeriodId mediaPeriodId, Timeline timeline) {
            ExoTrackSelection exoTrackSelectionCreateAdaptiveTrackSelection;
            ImmutableList<ImmutableList<AdaptationCheckpoint>> adaptationCheckpoints = AdaptiveTrackSelection.getAdaptationCheckpoints(definitions);
            ExoTrackSelection[] selections = new ExoTrackSelection[definitions.length];
            for (int i = 0; i < definitions.length; i++) {
                ExoTrackSelection.Definition definition = definitions[i];
                if (definition != null && definition.tracks.length != 0) {
                    if (definition.tracks.length == 1) {
                        exoTrackSelectionCreateAdaptiveTrackSelection = new FixedTrackSelection(definition.group, definition.tracks[0], definition.type);
                    } else {
                        exoTrackSelectionCreateAdaptiveTrackSelection = createAdaptiveTrackSelection(definition.group, definition.tracks, definition.type, bandwidthMeter, adaptationCheckpoints.get(i));
                    }
                    selections[i] = exoTrackSelectionCreateAdaptiveTrackSelection;
                }
            }
            return selections;
        }

        protected AdaptiveTrackSelection createAdaptiveTrackSelection(TrackGroup group, int[] tracks, int type, BandwidthMeter bandwidthMeter, ImmutableList<AdaptationCheckpoint> adaptationCheckpoints) {
            return new AdaptiveTrackSelection(group, tracks, type, bandwidthMeter, this.minDurationForQualityIncreaseMs, this.maxDurationForQualityDecreaseMs, this.minDurationToRetainAfterDiscardMs, this.maxWidthToDiscard, this.maxHeightToDiscard, this.bandwidthFraction, this.bufferedFractionToLiveEdgeForQualityIncrease, adaptationCheckpoints, this.clock);
        }
    }

    public AdaptiveTrackSelection(TrackGroup group, int[] tracks, BandwidthMeter bandwidthMeter) {
        this(group, tracks, 0, bandwidthMeter, Renderer.DEFAULT_DURATION_TO_PROGRESS_US, 25000L, 25000L, DEFAULT_MAX_WIDTH_TO_DISCARD, DEFAULT_MAX_HEIGHT_TO_DISCARD, 0.7f, 0.75f, ImmutableList.of(), Clock.DEFAULT);
    }

    protected AdaptiveTrackSelection(TrackGroup group, int[] tracks, int type, BandwidthMeter bandwidthMeter, long minDurationForQualityIncreaseMs, long maxDurationForQualityDecreaseMs, long minDurationToRetainAfterDiscardMs, int maxWidthToDiscard, int maxHeightToDiscard, float bandwidthFraction, float bufferedFractionToLiveEdgeForQualityIncrease, List<AdaptationCheckpoint> adaptationCheckpoints, Clock clock) {
        long minDurationToRetainAfterDiscardMs2;
        super(group, tracks, type);
        if (minDurationToRetainAfterDiscardMs >= minDurationForQualityIncreaseMs) {
            minDurationToRetainAfterDiscardMs2 = minDurationToRetainAfterDiscardMs;
        } else {
            Log.w(TAG, "Adjusting minDurationToRetainAfterDiscardMs to be at least minDurationForQualityIncreaseMs");
            minDurationToRetainAfterDiscardMs2 = minDurationForQualityIncreaseMs;
        }
        this.bandwidthMeter = bandwidthMeter;
        this.minDurationForQualityIncreaseUs = minDurationForQualityIncreaseMs * 1000;
        this.maxDurationForQualityDecreaseUs = maxDurationForQualityDecreaseMs * 1000;
        this.minDurationToRetainAfterDiscardUs = 1000 * minDurationToRetainAfterDiscardMs2;
        this.maxWidthToDiscard = maxWidthToDiscard;
        this.maxHeightToDiscard = maxHeightToDiscard;
        this.bandwidthFraction = bandwidthFraction;
        this.bufferedFractionToLiveEdgeForQualityIncrease = bufferedFractionToLiveEdgeForQualityIncrease;
        this.adaptationCheckpoints = ImmutableList.copyOf((Collection) adaptationCheckpoints);
        this.clock = clock;
        this.playbackSpeed = 1.0f;
        this.reason = 0;
        this.lastBufferEvaluationMs = C.TIME_UNSET;
        this.latestBitrateEstimate = -2147483647L;
    }

    @Override // androidx.media3.exoplayer.trackselection.BaseTrackSelection, androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public void enable() {
        this.lastBufferEvaluationMs = C.TIME_UNSET;
        this.lastBufferEvaluationMediaChunk = null;
    }

    @Override // androidx.media3.exoplayer.trackselection.BaseTrackSelection, androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public void disable() {
        this.lastBufferEvaluationMediaChunk = null;
    }

    @Override // androidx.media3.exoplayer.trackselection.BaseTrackSelection, androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public void onPlaybackSpeed(float playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public void updateSelectedTrack(long playbackPositionUs, long bufferedDurationUs, long availableDurationUs, List<? extends MediaChunk> queue, MediaChunkIterator[] mediaChunkIterators) {
        long nowMs = this.clock.elapsedRealtime();
        long chunkDurationUs = getNextChunkDurationUs(mediaChunkIterators, queue);
        if (this.reason == 0) {
            this.reason = 1;
            this.selectedIndex = determineIdealSelectedIndex(nowMs, chunkDurationUs);
            return;
        }
        int previousSelectedIndex = this.selectedIndex;
        int previousReason = this.reason;
        int formatIndexOfPreviousChunk = queue.isEmpty() ? -1 : indexOf(((MediaChunk) Iterables.getLast(queue)).trackFormat);
        if (formatIndexOfPreviousChunk != -1) {
            previousSelectedIndex = formatIndexOfPreviousChunk;
            previousReason = ((MediaChunk) Iterables.getLast(queue)).trackSelectionReason;
        }
        int newSelectedIndex = determineIdealSelectedIndex(nowMs, chunkDurationUs);
        if (newSelectedIndex != previousSelectedIndex && !isTrackExcluded(previousSelectedIndex, nowMs)) {
            Format currentFormat = getFormat(previousSelectedIndex);
            Format selectedFormat = getFormat(newSelectedIndex);
            long minDurationForQualityIncreaseUs = minDurationForQualityIncreaseUs(availableDurationUs, chunkDurationUs);
            if (selectedFormat.bitrate > currentFormat.bitrate && bufferedDurationUs < minDurationForQualityIncreaseUs) {
                newSelectedIndex = previousSelectedIndex;
            } else if (selectedFormat.bitrate < currentFormat.bitrate && bufferedDurationUs >= this.maxDurationForQualityDecreaseUs) {
                newSelectedIndex = previousSelectedIndex;
            }
        }
        this.reason = newSelectedIndex == previousSelectedIndex ? previousReason : 3;
        this.selectedIndex = newSelectedIndex;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public int getSelectionReason() {
        return this.reason;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public Object getSelectionData() {
        return null;
    }

    @Override // androidx.media3.exoplayer.trackselection.BaseTrackSelection, androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public int evaluateQueueSize(long playbackPositionUs, List<? extends MediaChunk> queue) {
        List<? extends MediaChunk> list = queue;
        long nowMs = this.clock.elapsedRealtime();
        if (!shouldEvaluateQueueSize(nowMs, list)) {
            return list.size();
        }
        this.lastBufferEvaluationMs = nowMs;
        this.lastBufferEvaluationMediaChunk = list.isEmpty() ? null : (MediaChunk) Iterables.getLast(list);
        if (list.isEmpty()) {
            return 0;
        }
        int queueSize = list.size();
        MediaChunk lastChunk = list.get(queueSize - 1);
        long playoutBufferedDurationBeforeLastChunkUs = Util.getPlayoutDurationForMediaDuration(lastChunk.startTimeUs - playbackPositionUs, this.playbackSpeed);
        long minDurationToRetainAfterDiscardUs = getMinDurationToRetainAfterDiscardUs();
        if (playoutBufferedDurationBeforeLastChunkUs >= minDurationToRetainAfterDiscardUs) {
            int idealSelectedIndex = determineIdealSelectedIndex(nowMs, getLastChunkDurationUs(list));
            Format idealFormat = getFormat(idealSelectedIndex);
            int i = 0;
            while (i < queueSize) {
                MediaChunk chunk = list.get(i);
                Format format = chunk.trackFormat;
                long nowMs2 = nowMs;
                long mediaDurationBeforeThisChunkUs = chunk.startTimeUs - playbackPositionUs;
                long playoutDurationBeforeThisChunkUs = Util.getPlayoutDurationForMediaDuration(mediaDurationBeforeThisChunkUs, this.playbackSpeed);
                if (playoutDurationBeforeThisChunkUs >= minDurationToRetainAfterDiscardUs && format.bitrate < idealFormat.bitrate && format.height != -1 && format.height <= this.maxHeightToDiscard && format.width != -1 && format.width <= this.maxWidthToDiscard && format.height < idealFormat.height) {
                    return i;
                }
                i++;
                list = queue;
                nowMs = nowMs2;
            }
            return queueSize;
        }
        return queueSize;
    }

    @Override // androidx.media3.exoplayer.trackselection.BaseTrackSelection, androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public long getLatestBitrateEstimate() {
        return this.latestBitrateEstimate;
    }

    protected boolean canSelectFormat(Format format, int trackBitrate, long effectiveBitrate) {
        return ((long) trackBitrate) <= effectiveBitrate;
    }

    protected boolean shouldEvaluateQueueSize(long nowMs, List<? extends MediaChunk> queue) {
        return this.lastBufferEvaluationMs == C.TIME_UNSET || nowMs - this.lastBufferEvaluationMs >= 1000 || !(queue.isEmpty() || ((MediaChunk) Iterables.getLast(queue)).equals(this.lastBufferEvaluationMediaChunk));
    }

    protected long getMinDurationToRetainAfterDiscardUs() {
        return this.minDurationToRetainAfterDiscardUs;
    }

    private int determineIdealSelectedIndex(long nowMs, long chunkDurationUs) {
        long effectiveBitrate = getAllocatedBandwidth(chunkDurationUs);
        int lowestBitrateAllowedIndex = 0;
        for (int i = 0; i < this.length; i++) {
            if (nowMs == Long.MIN_VALUE || !isTrackExcluded(i, nowMs)) {
                Format format = getFormat(i);
                if (canSelectFormat(format, format.bitrate, effectiveBitrate)) {
                    return i;
                }
                lowestBitrateAllowedIndex = i;
            }
        }
        return lowestBitrateAllowedIndex;
    }

    private long minDurationForQualityIncreaseUs(long availableDurationUs, long chunkDurationUs) {
        if (availableDurationUs == C.TIME_UNSET) {
            return this.minDurationForQualityIncreaseUs;
        }
        if (chunkDurationUs != C.TIME_UNSET) {
            availableDurationUs -= chunkDurationUs;
        }
        long adjustedMinDurationForQualityIncreaseUs = (long) (availableDurationUs * this.bufferedFractionToLiveEdgeForQualityIncrease);
        return Math.min(adjustedMinDurationForQualityIncreaseUs, this.minDurationForQualityIncreaseUs);
    }

    private long getNextChunkDurationUs(MediaChunkIterator[] mediaChunkIterators, List<? extends MediaChunk> queue) {
        if (this.selectedIndex < mediaChunkIterators.length && mediaChunkIterators[this.selectedIndex].next()) {
            MediaChunkIterator iterator = mediaChunkIterators[this.selectedIndex];
            return iterator.getChunkEndTimeUs() - iterator.getChunkStartTimeUs();
        }
        for (MediaChunkIterator iterator2 : mediaChunkIterators) {
            if (iterator2.next()) {
                return iterator2.getChunkEndTimeUs() - iterator2.getChunkStartTimeUs();
            }
        }
        return getLastChunkDurationUs(queue);
    }

    private long getLastChunkDurationUs(List<? extends MediaChunk> queue) {
        if (queue.isEmpty()) {
            return C.TIME_UNSET;
        }
        MediaChunk lastChunk = (MediaChunk) Iterables.getLast(queue);
        return (lastChunk.startTimeUs == C.TIME_UNSET || lastChunk.endTimeUs == C.TIME_UNSET) ? C.TIME_UNSET : lastChunk.endTimeUs - lastChunk.startTimeUs;
    }

    private long getAllocatedBandwidth(long chunkDurationUs) {
        long totalBandwidth = getTotalAllocatableBandwidth(chunkDurationUs);
        if (this.adaptationCheckpoints.isEmpty()) {
            return totalBandwidth;
        }
        int nextIndex = 1;
        while (nextIndex < this.adaptationCheckpoints.size() - 1 && this.adaptationCheckpoints.get(nextIndex).totalBandwidth < totalBandwidth) {
            nextIndex++;
        }
        AdaptationCheckpoint previous = this.adaptationCheckpoints.get(nextIndex - 1);
        AdaptationCheckpoint next = this.adaptationCheckpoints.get(nextIndex);
        float fractionBetweenCheckpoints = (totalBandwidth - previous.totalBandwidth) / (next.totalBandwidth - previous.totalBandwidth);
        return previous.allocatedBandwidth + ((long) ((next.allocatedBandwidth - previous.allocatedBandwidth) * fractionBetweenCheckpoints));
    }

    private long getTotalAllocatableBandwidth(long chunkDurationUs) {
        this.latestBitrateEstimate = this.bandwidthMeter.getBitrateEstimate();
        long cautiousBandwidthEstimate = (long) (this.latestBitrateEstimate * this.bandwidthFraction);
        long timeToFirstByteEstimateUs = this.bandwidthMeter.getTimeToFirstByteEstimateUs();
        if (timeToFirstByteEstimateUs == C.TIME_UNSET || chunkDurationUs == C.TIME_UNSET) {
            float availableTimeToLoadUs = cautiousBandwidthEstimate;
            return (long) (availableTimeToLoadUs / this.playbackSpeed);
        }
        float availableTimeToLoadUs2 = Math.max((chunkDurationUs / this.playbackSpeed) - timeToFirstByteEstimateUs, 0.0f);
        return (long) ((cautiousBandwidthEstimate * availableTimeToLoadUs2) / chunkDurationUs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ImmutableList<ImmutableList<AdaptationCheckpoint>> getAdaptationCheckpoints(ExoTrackSelection.Definition[] definitions) {
        List<ImmutableList.Builder<AdaptationCheckpoint>> checkPointBuilders = new ArrayList<>();
        for (int i = 0; i < definitions.length; i++) {
            if (definitions[i] != null && definitions[i].tracks.length > 1) {
                ImmutableList.Builder<AdaptationCheckpoint> builder = ImmutableList.builder();
                builder.add(new AdaptationCheckpoint(0L, 0L));
                checkPointBuilders.add(builder);
            } else {
                checkPointBuilders.add(null);
            }
        }
        long[][] trackBitrates = getSortedTrackBitrates(definitions);
        int[] currentTrackIndices = new int[trackBitrates.length];
        long[] currentTrackBitrates = new long[trackBitrates.length];
        for (int i2 = 0; i2 < trackBitrates.length; i2++) {
            currentTrackBitrates[i2] = trackBitrates[i2].length == 0 ? 0L : trackBitrates[i2][0];
        }
        addCheckpoint(checkPointBuilders, currentTrackBitrates);
        ImmutableList<Integer> switchOrder = getSwitchOrder(trackBitrates);
        for (int i3 = 0; i3 < switchOrder.size(); i3++) {
            int switchIndex = switchOrder.get(i3).intValue();
            int newTrackIndex = currentTrackIndices[switchIndex] + 1;
            currentTrackIndices[switchIndex] = newTrackIndex;
            currentTrackBitrates[switchIndex] = trackBitrates[switchIndex][newTrackIndex];
            addCheckpoint(checkPointBuilders, currentTrackBitrates);
        }
        for (int i4 = 0; i4 < definitions.length; i4++) {
            if (checkPointBuilders.get(i4) != null) {
                currentTrackBitrates[i4] = currentTrackBitrates[i4] * 2;
            }
        }
        addCheckpoint(checkPointBuilders, currentTrackBitrates);
        ImmutableList.Builder<ImmutableList<AdaptationCheckpoint>> output = ImmutableList.builder();
        for (int i5 = 0; i5 < checkPointBuilders.size(); i5++) {
            ImmutableList.Builder<AdaptationCheckpoint> builder2 = checkPointBuilders.get(i5);
            output.add(builder2 == null ? ImmutableList.of() : builder2.build());
        }
        return output.build();
    }

    private static long[][] getSortedTrackBitrates(ExoTrackSelection.Definition[] definitions) {
        long[][] trackBitrates = new long[definitions.length][];
        for (int i = 0; i < definitions.length; i++) {
            ExoTrackSelection.Definition definition = definitions[i];
            if (definition == null) {
                trackBitrates[i] = new long[0];
            } else {
                trackBitrates[i] = new long[definition.tracks.length];
                for (int j = 0; j < definition.tracks.length; j++) {
                    long bitrate = definition.group.getFormat(definition.tracks[j]).bitrate;
                    trackBitrates[i][j] = bitrate == -1 ? 0L : bitrate;
                }
                Arrays.sort(trackBitrates[i]);
            }
        }
        return trackBitrates;
    }

    private static ImmutableList<Integer> getSwitchOrder(long[][] trackBitrates) {
        Multimap<Double, Integer> switchPoints = MultimapBuilder.treeKeys().arrayListValues().build();
        for (int i = 0; i < trackBitrates.length; i++) {
            if (trackBitrates[i].length > 1) {
                double[] logBitrates = new double[trackBitrates[i].length];
                int j = 0;
                while (true) {
                    double dLog = 0.0d;
                    if (j >= trackBitrates[i].length) {
                        break;
                    }
                    if (trackBitrates[i][j] != -1) {
                        dLog = Math.log(trackBitrates[i][j]);
                    }
                    logBitrates[j] = dLog;
                    j++;
                }
                int j2 = logBitrates.length;
                double totalBitrateDiff = logBitrates[j2 - 1] - logBitrates[0];
                int j3 = 0;
                for (int i2 = 1; j3 < logBitrates.length - i2; i2 = 1) {
                    double switchBitrate = (logBitrates[j3] + logBitrates[j3 + 1]) * 0.5d;
                    double switchPoint = totalBitrateDiff == 0.0d ? 1.0d : (switchBitrate - logBitrates[0]) / totalBitrateDiff;
                    switchPoints.put(Double.valueOf(switchPoint), Integer.valueOf(i));
                    j3++;
                }
            }
        }
        return ImmutableList.copyOf((Collection) switchPoints.values());
    }

    private static void addCheckpoint(List<ImmutableList.Builder<AdaptationCheckpoint>> checkPointBuilders, long[] checkpointBitrates) {
        long totalBitrate = 0;
        for (long j : checkpointBitrates) {
            totalBitrate += j;
        }
        for (int i = 0; i < checkPointBuilders.size(); i++) {
            ImmutableList.Builder<AdaptationCheckpoint> builder = checkPointBuilders.get(i);
            if (builder != null) {
                builder.add(new AdaptationCheckpoint(totalBitrate, checkpointBitrates[i]));
            }
        }
    }

    public static final class AdaptationCheckpoint {
        public final long allocatedBandwidth;
        public final long totalBandwidth;

        public AdaptationCheckpoint(long totalBandwidth, long allocatedBandwidth) {
            this.totalBandwidth = totalBandwidth;
            this.allocatedBandwidth = allocatedBandwidth;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AdaptationCheckpoint)) {
                return false;
            }
            AdaptationCheckpoint that = (AdaptationCheckpoint) o;
            return this.totalBandwidth == that.totalBandwidth && this.allocatedBandwidth == that.allocatedBandwidth;
        }

        public int hashCode() {
            return (((int) this.totalBandwidth) * 31) + ((int) this.allocatedBandwidth);
        }
    }
}
