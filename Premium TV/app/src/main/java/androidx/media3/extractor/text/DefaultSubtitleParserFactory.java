package androidx.media3.extractor.text;

import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.extractor.text.dvb.DvbParser;
import androidx.media3.extractor.text.pgs.PgsParser;
import androidx.media3.extractor.text.ssa.SsaParser;
import androidx.media3.extractor.text.subrip.SubripParser;
import androidx.media3.extractor.text.ttml.TtmlParser;
import androidx.media3.extractor.text.tx3g.Tx3gParser;
import androidx.media3.extractor.text.webvtt.Mp4WebvttParser;
import androidx.media3.extractor.text.webvtt.WebvttParser;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultSubtitleParserFactory implements SubtitleParser.Factory {
    @Override // androidx.media3.extractor.text.SubtitleParser.Factory
    public boolean supportsFormat(Format format) {
        String mimeType = format.sampleMimeType;
        return Objects.equals(mimeType, MimeTypes.TEXT_SSA) || Objects.equals(mimeType, MimeTypes.TEXT_VTT) || Objects.equals(mimeType, MimeTypes.APPLICATION_MP4VTT) || Objects.equals(mimeType, MimeTypes.APPLICATION_SUBRIP) || Objects.equals(mimeType, MimeTypes.APPLICATION_TX3G) || Objects.equals(mimeType, MimeTypes.APPLICATION_PGS) || Objects.equals(mimeType, MimeTypes.APPLICATION_DVBSUBS) || Objects.equals(mimeType, MimeTypes.APPLICATION_TTML);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:31:0x0060  */
    @Override // androidx.media3.extractor.text.SubtitleParser.Factory
    public int getCueReplacementBehavior(Format format) {
        String mimeType = format.sampleMimeType;
        if (mimeType != null) {
            switch (mimeType) {
                case "text/x-ssa":
                    return 1;
                case "text/vtt":
                    return 1;
                case "application/x-mp4-vtt":
                    return 2;
                case "application/x-subrip":
                    return 1;
                case "application/x-quicktime-tx3g":
                    return 2;
                case "application/pgs":
                    return 2;
                case "application/dvbsubs":
                    return 2;
                case "application/ttml+xml":
                    return 1;
            }
        }
        throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:31:0x005e  */
    @Override // androidx.media3.extractor.text.SubtitleParser.Factory
    public SubtitleParser create(Format format) {
        String mimeType = format.sampleMimeType;
        if (mimeType != null) {
            switch (mimeType) {
                case "text/x-ssa":
                    return new SsaParser(format.initializationData);
                case "text/vtt":
                    return new WebvttParser();
                case "application/x-mp4-vtt":
                    return new Mp4WebvttParser();
                case "application/x-subrip":
                    return new SubripParser();
                case "application/x-quicktime-tx3g":
                    return new Tx3gParser(format.initializationData);
                case "application/pgs":
                    return new PgsParser();
                case "application/dvbsubs":
                    return new DvbParser(format.initializationData);
                case "application/ttml+xml":
                    return new TtmlParser();
            }
        }
        throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
    }
}
