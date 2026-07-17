package androidx.media3.exoplayer.hls;

import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.mp3.Mp3Extractor;
import androidx.media3.extractor.mp4.FragmentedMp4Extractor;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.ts.Ac3Extractor;
import androidx.media3.extractor.ts.Ac4Extractor;
import androidx.media3.extractor.ts.AdtsExtractor;
import androidx.media3.extractor.ts.TsExtractor;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class BundledHlsMediaChunkExtractor implements HlsMediaChunkExtractor {
    private static final PositionHolder POSITION_HOLDER = new PositionHolder();
    final Extractor extractor;
    private final Format multivariantPlaylistFormat;
    private final boolean parseSubtitlesDuringExtraction;
    private final SubtitleParser.Factory subtitleParserFactory;
    private final TimestampAdjuster timestampAdjuster;

    public BundledHlsMediaChunkExtractor(Extractor extractor, Format multivariantPlaylistFormat, TimestampAdjuster timestampAdjuster) {
        this(extractor, multivariantPlaylistFormat, timestampAdjuster, SubtitleParser.Factory.UNSUPPORTED, false);
    }

    BundledHlsMediaChunkExtractor(Extractor extractor, Format multivariantPlaylistFormat, TimestampAdjuster timestampAdjuster, SubtitleParser.Factory subtitleParserFactory, boolean parseSubtitlesDuringExtraction) {
        this.extractor = extractor;
        this.multivariantPlaylistFormat = multivariantPlaylistFormat;
        this.timestampAdjuster = timestampAdjuster;
        this.subtitleParserFactory = subtitleParserFactory;
        this.parseSubtitlesDuringExtraction = parseSubtitlesDuringExtraction;
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public void init(ExtractorOutput extractorOutput) {
        this.extractor.init(extractorOutput);
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public boolean read(ExtractorInput extractorInput) throws IOException {
        return this.extractor.read(extractorInput, POSITION_HOLDER) == 0;
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public boolean isPackedAudioExtractor() {
        Extractor underlyingExtractor = this.extractor.getUnderlyingImplementation();
        return (underlyingExtractor instanceof AdtsExtractor) || (underlyingExtractor instanceof Ac3Extractor) || (underlyingExtractor instanceof Ac4Extractor) || (underlyingExtractor instanceof Mp3Extractor);
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public boolean isReusable() {
        Extractor underlyingExtractor = this.extractor.getUnderlyingImplementation();
        return (underlyingExtractor instanceof TsExtractor) || (underlyingExtractor instanceof FragmentedMp4Extractor);
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public HlsMediaChunkExtractor recreate() {
        Extractor newExtractorInstance;
        Assertions.checkState(!isReusable());
        Assertions.checkState(this.extractor.getUnderlyingImplementation() == this.extractor, "Can't recreate wrapped extractors. Outer type: " + this.extractor.getClass());
        if (this.extractor instanceof WebvttExtractor) {
            newExtractorInstance = new WebvttExtractor(this.multivariantPlaylistFormat.language, this.timestampAdjuster, this.subtitleParserFactory, this.parseSubtitlesDuringExtraction);
        } else {
            Extractor newExtractorInstance2 = this.extractor;
            if (newExtractorInstance2 instanceof AdtsExtractor) {
                newExtractorInstance = new AdtsExtractor();
            } else {
                Extractor newExtractorInstance3 = this.extractor;
                if (newExtractorInstance3 instanceof Ac3Extractor) {
                    newExtractorInstance = new Ac3Extractor();
                } else {
                    Extractor newExtractorInstance4 = this.extractor;
                    if (newExtractorInstance4 instanceof Ac4Extractor) {
                        newExtractorInstance = new Ac4Extractor();
                    } else {
                        Extractor newExtractorInstance5 = this.extractor;
                        if (newExtractorInstance5 instanceof Mp3Extractor) {
                            newExtractorInstance = new Mp3Extractor();
                        } else {
                            throw new IllegalStateException("Unexpected extractor type for recreation: " + this.extractor.getClass().getSimpleName());
                        }
                    }
                }
            }
        }
        return new BundledHlsMediaChunkExtractor(newExtractorInstance, this.multivariantPlaylistFormat, this.timestampAdjuster, this.subtitleParserFactory, this.parseSubtitlesDuringExtraction);
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public void onTruncatedSegmentParsed() {
        this.extractor.seek(0L, 0L);
    }
}
