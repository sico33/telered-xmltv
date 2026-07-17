package androidx.media3.extractor.flv;

import androidx.media3.common.ParserException;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.TrackOutput;

/* JADX INFO: loaded from: classes.dex */
abstract class TagPayloadReader {
    protected final TrackOutput output;

    protected abstract boolean parseHeader(ParsableByteArray parsableByteArray) throws ParserException;

    protected abstract boolean parsePayload(ParsableByteArray parsableByteArray, long j) throws ParserException;

    public abstract void seek();

    public static final class UnsupportedFormatException extends ParserException {
        public UnsupportedFormatException(String msg) {
            super(msg, null, false, 1);
        }
    }

    protected TagPayloadReader(TrackOutput output) {
        this.output = output;
    }

    public final boolean consume(ParsableByteArray data, long timeUs) throws ParserException {
        return parseHeader(data) && parsePayload(data, timeUs);
    }
}
