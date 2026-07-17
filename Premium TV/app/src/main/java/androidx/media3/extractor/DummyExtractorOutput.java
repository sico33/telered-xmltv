package androidx.media3.extractor;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public final class DummyExtractorOutput implements ExtractorOutput {
    @Override // androidx.media3.extractor.ExtractorOutput
    public TrackOutput track(int id, int type) {
        return new DiscardingTrackOutput();
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public void endTracks() {
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public void seekMap(SeekMap seekMap) {
    }
}
