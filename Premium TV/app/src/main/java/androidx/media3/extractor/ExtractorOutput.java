package androidx.media3.extractor;

/* JADX INFO: loaded from: classes.dex */
public interface ExtractorOutput {
    public static final ExtractorOutput PLACEHOLDER = new ExtractorOutput() { // from class: androidx.media3.extractor.ExtractorOutput.1
        @Override // androidx.media3.extractor.ExtractorOutput
        public TrackOutput track(int id, int type) {
            throw new UnsupportedOperationException();
        }

        @Override // androidx.media3.extractor.ExtractorOutput
        public void endTracks() {
            throw new UnsupportedOperationException();
        }

        @Override // androidx.media3.extractor.ExtractorOutput
        public void seekMap(SeekMap seekMap) {
            throw new UnsupportedOperationException();
        }
    };

    void endTracks();

    void seekMap(SeekMap seekMap);

    TrackOutput track(int i, int i2);
}
