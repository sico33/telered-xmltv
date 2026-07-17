package androidx.media3.extractor;

/* JADX INFO: loaded from: classes.dex */
public final class SingleSampleSeekMap implements SeekMap {
    private final long durationUs;
    private final long startPosition;

    public SingleSampleSeekMap(long durationUs) {
        this(durationUs, 0L);
    }

    public SingleSampleSeekMap(long durationUs, long startPosition) {
        this.durationUs = durationUs;
        this.startPosition = startPosition;
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return true;
    }

    @Override // androidx.media3.extractor.SeekMap
    public long getDurationUs() {
        return this.durationUs;
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        return new SeekMap.SeekPoints(new SeekPoint(timeUs, this.startPosition));
    }
}
