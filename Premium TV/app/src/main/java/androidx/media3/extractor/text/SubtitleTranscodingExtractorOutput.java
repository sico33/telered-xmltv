package androidx.media3.extractor.text;

import android.util.SparseArray;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;

/* JADX INFO: loaded from: classes.dex */
public final class SubtitleTranscodingExtractorOutput implements ExtractorOutput {
    private final ExtractorOutput delegate;
    private final SubtitleParser.Factory subtitleParserFactory;
    private final SparseArray<SubtitleTranscodingTrackOutput> textTrackOutputs = new SparseArray<>();

    public SubtitleTranscodingExtractorOutput(ExtractorOutput delegate, SubtitleParser.Factory subtitleParserFactory) {
        this.delegate = delegate;
        this.subtitleParserFactory = subtitleParserFactory;
    }

    public void resetSubtitleParsers() {
        for (int i = 0; i < this.textTrackOutputs.size(); i++) {
            this.textTrackOutputs.valueAt(i).resetSubtitleParser();
        }
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public TrackOutput track(int id, int type) {
        if (type != 3) {
            return this.delegate.track(id, type);
        }
        SubtitleTranscodingTrackOutput existingTrackOutput = this.textTrackOutputs.get(id);
        if (existingTrackOutput != null) {
            return existingTrackOutput;
        }
        SubtitleTranscodingTrackOutput trackOutput = new SubtitleTranscodingTrackOutput(this.delegate.track(id, type), this.subtitleParserFactory);
        this.textTrackOutputs.put(id, trackOutput);
        return trackOutput;
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public void endTracks() {
        this.delegate.endTracks();
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public void seekMap(SeekMap seekMap) {
        this.delegate.seekMap(seekMap);
    }
}
