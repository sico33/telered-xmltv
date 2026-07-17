package androidx.media3.exoplayer.upstream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
public class SlidingPercentile {
    private static final int MAX_RECYCLED_SAMPLES = 5;
    private static final int SORT_ORDER_BY_INDEX = 1;
    private static final int SORT_ORDER_BY_VALUE = 0;
    private static final int SORT_ORDER_NONE = -1;
    private final int maxWeight;
    private int nextSampleIndex;
    private int recycledSampleCount;
    private int totalWeight;
    private static final Comparator<Sample> INDEX_COMPARATOR = new Comparator() { // from class: androidx.media3.exoplayer.upstream.SlidingPercentile$$ExternalSyntheticLambda0
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return SlidingPercentile.lambda$static$0((SlidingPercentile.Sample) obj, (SlidingPercentile.Sample) obj2);
        }
    };
    private static final Comparator<Sample> VALUE_COMPARATOR = new Comparator() { // from class: androidx.media3.exoplayer.upstream.SlidingPercentile$$ExternalSyntheticLambda1
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return Float.compare(((SlidingPercentile.Sample) obj).value, ((SlidingPercentile.Sample) obj2).value);
        }
    };
    private final Sample[] recycledSamples = new Sample[5];
    private final ArrayList<Sample> samples = new ArrayList<>();
    private int currentSortOrder = -1;

    static /* synthetic */ int lambda$static$0(Sample a, Sample b) {
        return a.index - b.index;
    }

    public SlidingPercentile(int maxWeight) {
        this.maxWeight = maxWeight;
    }

    public void reset() {
        this.samples.clear();
        this.currentSortOrder = -1;
        this.nextSampleIndex = 0;
        this.totalWeight = 0;
    }

    public void addSample(int weight, float value) {
        Sample newSample;
        ensureSortedByIndex();
        if (this.recycledSampleCount > 0) {
            Sample[] sampleArr = this.recycledSamples;
            int i = this.recycledSampleCount - 1;
            this.recycledSampleCount = i;
            newSample = sampleArr[i];
        } else {
            newSample = new Sample();
        }
        int i2 = this.nextSampleIndex;
        this.nextSampleIndex = i2 + 1;
        newSample.index = i2;
        newSample.weight = weight;
        newSample.value = value;
        this.samples.add(newSample);
        this.totalWeight += weight;
        while (this.totalWeight > this.maxWeight) {
            int excessWeight = this.totalWeight - this.maxWeight;
            Sample oldestSample = this.samples.get(0);
            if (oldestSample.weight <= excessWeight) {
                this.totalWeight -= oldestSample.weight;
                this.samples.remove(0);
                if (this.recycledSampleCount < 5) {
                    Sample[] sampleArr2 = this.recycledSamples;
                    int i3 = this.recycledSampleCount;
                    this.recycledSampleCount = i3 + 1;
                    sampleArr2[i3] = oldestSample;
                }
            } else {
                oldestSample.weight -= excessWeight;
                this.totalWeight -= excessWeight;
            }
        }
    }

    public float getPercentile(float percentile) {
        ensureSortedByValue();
        float desiredWeight = this.totalWeight * percentile;
        int accumulatedWeight = 0;
        int i = 0;
        while (true) {
            int size = this.samples.size();
            ArrayList<Sample> arrayList = this.samples;
            if (i < size) {
                Sample currentSample = arrayList.get(i);
                accumulatedWeight += currentSample.weight;
                if (accumulatedWeight < desiredWeight) {
                    i++;
                } else {
                    return currentSample.value;
                }
            } else {
                if (arrayList.isEmpty()) {
                    return Float.NaN;
                }
                return this.samples.get(this.samples.size() - 1).value;
            }
        }
    }

    private void ensureSortedByIndex() {
        if (this.currentSortOrder != 1) {
            Collections.sort(this.samples, INDEX_COMPARATOR);
            this.currentSortOrder = 1;
        }
    }

    private void ensureSortedByValue() {
        if (this.currentSortOrder != 0) {
            Collections.sort(this.samples, VALUE_COMPARATOR);
            this.currentSortOrder = 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class Sample {
        public int index;
        public float value;
        public int weight;

        private Sample() {
        }
    }
}
