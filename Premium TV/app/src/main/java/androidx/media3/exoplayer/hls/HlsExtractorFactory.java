package androidx.media3.exoplayer.hls;

import android.net.Uri;
import androidx.media3.common.Format;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.text.SubtitleParser;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public interface HlsExtractorFactory {
    public static final HlsExtractorFactory DEFAULT = new DefaultHlsExtractorFactory();

    HlsMediaChunkExtractor createExtractor(Uri uri, Format format, List<Format> list, TimestampAdjuster timestampAdjuster, Map<String, List<String>> map, ExtractorInput extractorInput, PlayerId playerId) throws IOException;

    HlsExtractorFactory experimentalParseSubtitlesDuringExtraction(boolean z);

    Format getOutputTextFormat(Format format);

    HlsExtractorFactory setSubtitleParserFactory(SubtitleParser.Factory factory);

    /* JADX INFO: renamed from: androidx.media3.exoplayer.hls.HlsExtractorFactory$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static HlsExtractorFactory $default$setSubtitleParserFactory(HlsExtractorFactory _this, SubtitleParser.Factory subtitleParserFactory) {
            return _this;
        }

        public static HlsExtractorFactory $default$experimentalParseSubtitlesDuringExtraction(HlsExtractorFactory _this, boolean parseSubtitlesDuringExtraction) {
            return _this;
        }

        public static Format $default$getOutputTextFormat(HlsExtractorFactory _this, Format sourceFormat) {
            return sourceFormat;
        }
    }
}
