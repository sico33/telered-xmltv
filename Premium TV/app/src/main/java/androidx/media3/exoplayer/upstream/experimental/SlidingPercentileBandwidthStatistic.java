package androidx.media3.exoplayer.upstream.experimental;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.util.ArrayDeque;
import java.util.TreeSet;

/* JADX INFO: loaded from: classes.dex */
public class SlidingPercentileBandwidthStatistic implements BandwidthStatistic {
    public static final int DEFAULT_MAX_SAMPLES_COUNT = 10;
    public static final double DEFAULT_PERCENTILE = 0.5d;
    private long bitrateEstimate;
    private final int maxSampleCount;
    private final double percentile;
    private final ArrayDeque<Sample> samples;
    private final TreeSet<Sample> sortedSamples;
    private double weightSum;

    public SlidingPercentileBandwidthStatistic() {
        this(10, 0.5d);
    }

    public SlidingPercentileBandwidthStatistic(int maxSampleCount, double percentile) {
        Assertions.checkArgument(percentile >= 0.0d && percentile <= 1.0d);
        this.maxSampleCount = maxSampleCount;
        this.percentile = percentile;
        this.samples = new ArrayDeque<>();
        this.sortedSamples = new TreeSet<>();
        this.bitrateEstimate = Long.MIN_VALUE;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthStatistic
    public void addSample(long bytes, long durationUs) {
        while (this.samples.size() >= this.maxSampleCount) {
            Sample removedSample = this.samples.remove();
            this.sortedSamples.remove(removedSample);
            this.weightSum -= removedSample.weight;
        }
        double weight = Math.sqrt(bytes);
        long bitrate = (8000000 * bytes) / durationUs;
        Sample sample = new Sample(bitrate, weight);
        this.samples.add(sample);
        this.sortedSamples.add(sample);
        this.weightSum += weight;
        this.bitrateEstimate = calculateBitrateEstimate();
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthStatistic
    public long getBandwidthEstimate() {
        return this.bitrateEstimate;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthStatistic
    public void reset() {
        this.samples.clear();
        this.sortedSamples.clear();
        this.weightSum = 0.0d;
        this.bitrateEstimate = Long.MIN_VALUE;
    }

    private long calculateBitrateEstimate() {
        if (this.samples.isEmpty()) {
            return Long.MIN_VALUE;
        }
        double targetWeightSum = this.weightSum * this.percentile;
        double previousPartialWeightSum = 0.0d;
        long previousSampleBitrate = 0;
        double nextPartialWeightSum = 0.0d;
        for (Sample sample : this.sortedSamples) {
            double nextPartialWeightSum2 = nextPartialWeightSum + (sample.weight / 2.0d);
            if (nextPartialWeightSum2 >= targetWeightSum) {
                if (previousSampleBitrate == 0) {
                    return sample.bitrate;
                }
                double partialBitrateBetweenSamples = ((sample.bitrate - previousSampleBitrate) * (targetWeightSum - previousPartialWeightSum)) / (nextPartialWeightSum2 - previousPartialWeightSum);
                return ((long) partialBitrateBetweenSamples) + previousSampleBitrate;
            }
            previousSampleBitrate = sample.bitrate;
            previousPartialWeightSum = nextPartialWeightSum2;
            nextPartialWeightSum = nextPartialWeightSum2 + (sample.weight / 2.0d);
        }
        return previousSampleBitrate;
    }

    private static class Sample implements Comparable<Sample> {
        private final long bitrate;
        private final double weight;

        public Sample(long bitrate, double weight) {
            this.bitrate = bitrate;
            this.weight = weight;
        }

        @Override // java.lang.Comparable
        public int compareTo(Sample other) {
            return Util.compareLong(this.bitrate, other.bitrate);
        }
    }
}
