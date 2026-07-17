package androidx.media3.extractor;

/* JADX INFO: loaded from: classes.dex */
public class ForwardingSeekMap implements SeekMap {
    private final SeekMap seekMap;

    public ForwardingSeekMap(SeekMap seekMap) {
        this.seekMap = seekMap;
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return this.seekMap.isSeekable();
    }

    @Override // androidx.media3.extractor.SeekMap
    public long getDurationUs() {
        return this.seekMap.getDurationUs();
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        return this.seekMap.getSeekPoints(timeUs);
    }
}
