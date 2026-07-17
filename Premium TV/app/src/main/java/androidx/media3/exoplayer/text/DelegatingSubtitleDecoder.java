package androidx.media3.exoplayer.text;

import androidx.media3.extractor.text.SimpleSubtitleDecoder;
import androidx.media3.extractor.text.Subtitle;
import androidx.media3.extractor.text.SubtitleParser;

/* JADX INFO: loaded from: classes.dex */
final class DelegatingSubtitleDecoder extends SimpleSubtitleDecoder {
    private final SubtitleParser subtitleParser;

    public DelegatingSubtitleDecoder(String name, SubtitleParser subtitleParser) {
        super(name);
        this.subtitleParser = subtitleParser;
    }

    @Override // androidx.media3.extractor.text.SimpleSubtitleDecoder
    protected Subtitle decode(byte[] data, int length, boolean reset) {
        if (reset) {
            this.subtitleParser.reset();
        }
        return this.subtitleParser.parseToLegacySubtitle(data, 0, length);
    }
}
