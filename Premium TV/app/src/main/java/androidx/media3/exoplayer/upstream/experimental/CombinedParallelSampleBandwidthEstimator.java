package androidx.media3.exoplayer.upstream.experimental;

import android.os.Handler;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.upstream.BandwidthMeter;

/* JADX INFO: loaded from: classes.dex */
public class CombinedParallelSampleBandwidthEstimator implements BandwidthEstimator {
    private long bandwidthEstimate;
    private final BandwidthStatistic bandwidthStatistic;
    private final Clock clock;
    private final BandwidthMeter.EventListener.EventDispatcher eventDispatcher;
    private long lastReportedBandwidthEstimate;
    private final long minBytesTransferred;
    private final int minSamples;
    private long sampleBytesTransferred;
    private long sampleStartTimeMs;
    private int streamCount;
    private long totalBytesTransferred;
    private int totalSamplesAdded;

    public static class Builder {
        private BandwidthStatistic bandwidthStatistic = new SlidingWeightedAverageBandwidthStatistic();
        private Clock clock = Clock.DEFAULT;
        private long minBytesTransferred;
        private int minSamples;

        public Builder setBandwidthStatistic(BandwidthStatistic bandwidthStatistic) {
            Assertions.checkNotNull(bandwidthStatistic);
            this.bandwidthStatistic = bandwidthStatistic;
            return this;
        }

        public Builder setMinSamples(int minSamples) {
            Assertions.checkArgument(minSamples >= 0);
            this.minSamples = minSamples;
            return this;
        }

        public Builder setMinBytesTransferred(long minBytesTransferred) {
            Assertions.checkArgument(minBytesTransferred >= 0);
            this.minBytesTransferred = minBytesTransferred;
            return this;
        }

        Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public CombinedParallelSampleBandwidthEstimator build() {
            return new CombinedParallelSampleBandwidthEstimator(this);
        }
    }

    private CombinedParallelSampleBandwidthEstimator(Builder builder) {
        this.bandwidthStatistic = builder.bandwidthStatistic;
        this.minSamples = builder.minSamples;
        this.minBytesTransferred = builder.minBytesTransferred;
        this.clock = builder.clock;
        this.eventDispatcher = new BandwidthMeter.EventListener.EventDispatcher();
        this.bandwidthEstimate = Long.MIN_VALUE;
        this.lastReportedBandwidthEstimate = Long.MIN_VALUE;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthEstimator
    public void addEventListener(Handler eventHandler, BandwidthMeter.EventListener eventListener) {
        this.eventDispatcher.addListener(eventHandler, eventListener);
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthEstimator
    public void removeEventListener(BandwidthMeter.EventListener eventListener) {
        this.eventDispatcher.removeListener(eventListener);
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthEstimator
    public void onTransferInitializing(DataSource source) {
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthEstimator
    public void onTransferStart(DataSource source) {
        if (this.streamCount == 0) {
            this.sampleStartTimeMs = this.clock.elapsedRealtime();
        }
        this.streamCount++;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthEstimator
    public void onBytesTransferred(DataSource source, int bytesTransferred) {
        this.sampleBytesTransferred += (long) bytesTransferred;
        this.totalBytesTransferred += (long) bytesTransferred;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthEstimator
    public void onTransferEnd(DataSource source) {
        Assertions.checkState(this.streamCount > 0);
        this.streamCount--;
        if (this.streamCount > 0) {
            return;
        }
        long nowMs = this.clock.elapsedRealtime();
        long sampleElapsedTimeMs = (int) (nowMs - this.sampleStartTimeMs);
        if (sampleElapsedTimeMs > 0) {
            this.bandwidthStatistic.addSample(this.sampleBytesTransferred, 1000 * sampleElapsedTimeMs);
            this.totalSamplesAdded++;
            if (this.totalSamplesAdded > this.minSamples && this.totalBytesTransferred > this.minBytesTransferred) {
                this.bandwidthEstimate = this.bandwidthStatistic.getBandwidthEstimate();
            }
            maybeNotifyBandwidthSample((int) sampleElapsedTimeMs, this.sampleBytesTransferred, this.bandwidthEstimate);
            this.sampleBytesTransferred = 0L;
        }
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthEstimator
    public long getBandwidthEstimate() {
        return this.bandwidthEstimate;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthEstimator
    public void onNetworkTypeChange(long newBandwidthEstimate) {
        long nowMs = this.clock.elapsedRealtime();
        int sampleElapsedTimeMs = this.streamCount > 0 ? (int) (nowMs - this.sampleStartTimeMs) : 0;
        maybeNotifyBandwidthSample(sampleElapsedTimeMs, this.sampleBytesTransferred, newBandwidthEstimate);
        this.bandwidthStatistic.reset();
        this.bandwidthEstimate = Long.MIN_VALUE;
        this.sampleStartTimeMs = nowMs;
        this.sampleBytesTransferred = 0L;
        this.totalSamplesAdded = 0;
        this.totalBytesTransferred = 0L;
    }

    private void maybeNotifyBandwidthSample(int elapsedMs, long bytesTransferred, long bandwidthEstimate) {
        if (bandwidthEstimate != Long.MIN_VALUE) {
            if (elapsedMs == 0 && bytesTransferred == 0 && bandwidthEstimate == this.lastReportedBandwidthEstimate) {
                return;
            }
            this.lastReportedBandwidthEstimate = bandwidthEstimate;
            this.eventDispatcher.bandwidthSample(elapsedMs, bytesTransferred, bandwidthEstimate);
        }
    }
}
