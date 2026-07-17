package androidx.media3.exoplayer.upstream.experimental;

import androidx.media3.common.C;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSpec;
import androidx.media3.exoplayer.upstream.TimeToFirstByteEstimator;
import java.util.LinkedHashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class ExponentialWeightedAverageTimeToFirstByteEstimator implements TimeToFirstByteEstimator {
    public static final double DEFAULT_SMOOTHING_FACTOR = 0.85d;
    private static final int MAX_DATA_SPECS = 10;
    private final Clock clock;
    private long estimateUs;
    private final LinkedHashMap<DataSpec, Long> initializedDataSpecs;
    private final double smoothingFactor;

    public ExponentialWeightedAverageTimeToFirstByteEstimator() {
        this(0.85d, Clock.DEFAULT);
    }

    public ExponentialWeightedAverageTimeToFirstByteEstimator(double smoothingFactor) {
        this(smoothingFactor, Clock.DEFAULT);
    }

    ExponentialWeightedAverageTimeToFirstByteEstimator(double smoothingFactor, Clock clock) {
        this.smoothingFactor = smoothingFactor;
        this.clock = clock;
        this.initializedDataSpecs = new FixedSizeLinkedHashMap(10);
        this.estimateUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.upstream.TimeToFirstByteEstimator
    public long getTimeToFirstByteEstimateUs() {
        return this.estimateUs;
    }

    @Override // androidx.media3.exoplayer.upstream.TimeToFirstByteEstimator
    public void reset() {
        this.estimateUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.upstream.TimeToFirstByteEstimator
    public void onTransferInitializing(DataSpec dataSpec) {
        this.initializedDataSpecs.remove(dataSpec);
        this.initializedDataSpecs.put(dataSpec, Long.valueOf(Util.msToUs(this.clock.elapsedRealtime())));
    }

    @Override // androidx.media3.exoplayer.upstream.TimeToFirstByteEstimator
    public void onTransferStart(DataSpec dataSpec) {
        Long initializationStartUs = this.initializedDataSpecs.remove(dataSpec);
        if (initializationStartUs == null) {
            return;
        }
        long timeToStartSampleUs = Util.msToUs(this.clock.elapsedRealtime()) - initializationStartUs.longValue();
        if (this.estimateUs == C.TIME_UNSET) {
            this.estimateUs = timeToStartSampleUs;
        } else {
            this.estimateUs = (long) ((this.smoothingFactor * this.estimateUs) + ((1.0d - this.smoothingFactor) * timeToStartSampleUs));
        }
    }

    private static class FixedSizeLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public FixedSizeLinkedHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override // java.util.LinkedHashMap
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > this.maxSize;
        }
    }
}
