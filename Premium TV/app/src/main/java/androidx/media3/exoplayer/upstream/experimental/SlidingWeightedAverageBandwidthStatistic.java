package androidx.media3.exoplayer.upstream.experimental;

import androidx.media3.common.util.Clock;
import androidx.media3.common.util.Util;
import java.util.ArrayDeque;
import java.util.Deque;

/* JADX INFO: loaded from: classes.dex */
public class SlidingWeightedAverageBandwidthStatistic implements BandwidthStatistic {
    public static final int DEFAULT_MAX_SAMPLES_COUNT = 10;
    private double bitrateWeightProductSum;
    private final Clock clock;
    private final SampleEvictionFunction sampleEvictionFunction;
    private final ArrayDeque<Sample> samples;
    private double weightSum;

    public interface SampleEvictionFunction {
        boolean shouldEvictSample(Deque<Sample> deque);
    }

    public static class Sample {
        public final long bitrate;
        public final long timeAddedMs;
        public final double weight;

        public Sample(long bitrate, double weight, long timeAddedMs) {
            this.bitrate = bitrate;
            this.weight = weight;
            this.timeAddedMs = timeAddedMs;
        }
    }

    public static SampleEvictionFunction getMaxCountEvictionFunction(final long maxSamplesCount) {
        return new SampleEvictionFunction() { // from class: androidx.media3.exoplayer.upstream.experimental.SlidingWeightedAverageBandwidthStatistic$$ExternalSyntheticLambda1
            @Override // androidx.media3.exoplayer.upstream.experimental.SlidingWeightedAverageBandwidthStatistic.SampleEvictionFunction
            public final boolean shouldEvictSample(Deque deque) {
                return SlidingWeightedAverageBandwidthStatistic.lambda$getMaxCountEvictionFunction$0(maxSamplesCount, deque);
            }
        };
    }

    static /* synthetic */ boolean lambda$getMaxCountEvictionFunction$0(long maxSamplesCount, Deque samples) {
        return ((long) samples.size()) >= maxSamplesCount;
    }

    public static SampleEvictionFunction getAgeBasedEvictionFunction(long maxAgeMs) {
        return getAgeBasedEvictionFunction(maxAgeMs, Clock.DEFAULT);
    }

    static SampleEvictionFunction getAgeBasedEvictionFunction(final long maxAgeMs, final Clock clock) {
        return new SampleEvictionFunction() { // from class: androidx.media3.exoplayer.upstream.experimental.SlidingWeightedAverageBandwidthStatistic$$ExternalSyntheticLambda0
            @Override // androidx.media3.exoplayer.upstream.experimental.SlidingWeightedAverageBandwidthStatistic.SampleEvictionFunction
            public final boolean shouldEvictSample(Deque deque) {
                return SlidingWeightedAverageBandwidthStatistic.lambda$getAgeBasedEvictionFunction$1(maxAgeMs, clock, deque);
            }
        };
    }

    static /* synthetic */ boolean lambda$getAgeBasedEvictionFunction$1(long maxAgeMs, Clock clock, Deque samples) {
        return !samples.isEmpty() && ((Sample) Util.castNonNull((Sample) samples.peek())).timeAddedMs + maxAgeMs < clock.elapsedRealtime();
    }

    public SlidingWeightedAverageBandwidthStatistic() {
        this(getMaxCountEvictionFunction(10L));
    }

    public SlidingWeightedAverageBandwidthStatistic(SampleEvictionFunction sampleEvictionFunction) {
        this(sampleEvictionFunction, Clock.DEFAULT);
    }

    SlidingWeightedAverageBandwidthStatistic(SampleEvictionFunction sampleEvictionFunction, Clock clock) {
        this.samples = new ArrayDeque<>();
        this.sampleEvictionFunction = sampleEvictionFunction;
        this.clock = clock;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthStatistic
    public void addSample(long bytes, long durationUs) {
        while (this.sampleEvictionFunction.shouldEvictSample(this.samples)) {
            Sample sample = this.samples.remove();
            this.bitrateWeightProductSum -= sample.bitrate * sample.weight;
            this.weightSum -= sample.weight;
        }
        double weight = Math.sqrt(bytes);
        long bitrate = (8000000 * bytes) / durationUs;
        Sample sample2 = new Sample(bitrate, weight, this.clock.elapsedRealtime());
        this.samples.add(sample2);
        this.bitrateWeightProductSum += sample2.bitrate * sample2.weight;
        this.weightSum += sample2.weight;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthStatistic
    public long getBandwidthEstimate() {
        if (this.samples.isEmpty()) {
            return Long.MIN_VALUE;
        }
        return (long) (this.bitrateWeightProductSum / this.weightSum);
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthStatistic
    public void reset() {
        this.samples.clear();
        this.bitrateWeightProductSum = 0.0d;
        this.weightSum = 0.0d;
    }
}
