package androidx.media3.exoplayer.upstream.experimental;

/* JADX INFO: loaded from: classes.dex */
public class ExponentialWeightedAverageStatistic implements BandwidthStatistic {
    public static final double DEFAULT_SMOOTHING_FACTOR = 0.9999d;
    private long bitrateEstimate;
    private final double smoothingFactor;

    public ExponentialWeightedAverageStatistic() {
        this(0.9999d);
    }

    public ExponentialWeightedAverageStatistic(double smoothingFactor) {
        this.smoothingFactor = smoothingFactor;
        this.bitrateEstimate = Long.MIN_VALUE;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthStatistic
    public void addSample(long bytes, long durationUs) {
        long bitrate = (8000000 * bytes) / durationUs;
        if (this.bitrateEstimate == Long.MIN_VALUE) {
            this.bitrateEstimate = bitrate;
        } else {
            double factor = Math.pow(this.smoothingFactor, Math.sqrt(bytes));
            this.bitrateEstimate = (long) ((this.bitrateEstimate * factor) + ((1.0d - factor) * bitrate));
        }
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthStatistic
    public long getBandwidthEstimate() {
        return this.bitrateEstimate;
    }

    @Override // androidx.media3.exoplayer.upstream.experimental.BandwidthStatistic
    public void reset() {
        this.bitrateEstimate = Long.MIN_VALUE;
    }
}
