package androidx.media3.extractor.text;

import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class SubtitleTranscodingExtractor implements Extractor {
    private final Extractor delegate;
    private final SubtitleParser.Factory subtitleParserFactory;
    private SubtitleTranscodingExtractorOutput transcodingExtractorOutput;

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ List getSniffFailureDetails() {
        return ImmutableList.of();
    }

    public SubtitleTranscodingExtractor(Extractor delegate, SubtitleParser.Factory subtitleParserFactory) {
        this.delegate = delegate;
        this.subtitleParserFactory = subtitleParserFactory;
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        return this.delegate.sniff(input);
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.transcodingExtractorOutput = new SubtitleTranscodingExtractorOutput(output, this.subtitleParserFactory);
        this.delegate.init(this.transcodingExtractorOutput);
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        return this.delegate.read(input, seekPosition);
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        if (this.transcodingExtractorOutput != null) {
            this.transcodingExtractorOutput.resetSubtitleParsers();
        }
        this.delegate.seek(position, timeUs);
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
        this.delegate.release();
    }

    @Override // androidx.media3.extractor.Extractor
    public Extractor getUnderlyingImplementation() {
        return this.delegate;
    }
}
