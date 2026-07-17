package androidx.media3.extractor.ogg;

import android.net.Uri;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/* JADX INFO: loaded from: classes.dex */
public class OggExtractor implements Extractor {
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() { // from class: androidx.media3.extractor.ogg.OggExtractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.ExtractorsFactory
        public final Extractor[] createExtractors() {
            return OggExtractor.lambda$static$0();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ Extractor[] createExtractors(Uri uri, Map map) {
            return createExtractors();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory experimentalSetTextTrackTranscodingEnabled(boolean z) {
            return ExtractorsFactory.CC.$default$experimentalSetTextTrackTranscodingEnabled(this, z);
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory setSubtitleParserFactory(SubtitleParser.Factory factory) {
            return ExtractorsFactory.CC.$default$setSubtitleParserFactory(this, factory);
        }
    };
    private static final int MAX_VERIFICATION_BYTES = 8;
    private ExtractorOutput output;
    private StreamReader streamReader;
    private boolean streamReaderInitialized;

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ List getSniffFailureDetails() {
        return ImmutableList.of();
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ Extractor getUnderlyingImplementation() {
        return Extractor.CC.$default$getUnderlyingImplementation(this);
    }

    static /* synthetic */ Extractor[] lambda$static$0() {
        return new Extractor[]{new OggExtractor()};
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        try {
            return sniffInternal(input);
        } catch (ParserException e) {
            return false;
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.output = output;
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        if (this.streamReader != null) {
            this.streamReader.seek(position, timeUs);
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        Assertions.checkStateNotNull(this.output);
        if (this.streamReader == null) {
            if (!sniffInternal(input)) {
                throw ParserException.createForMalformedContainer("Failed to determine bitstream type", null);
            }
            input.resetPeekPosition();
        }
        if (!this.streamReaderInitialized) {
            TrackOutput trackOutput = this.output.track(0, 1);
            this.output.endTracks();
            this.streamReader.init(this.output, trackOutput);
            this.streamReaderInitialized = true;
        }
        return this.streamReader.read(input, seekPosition);
    }

    @EnsuresNonNullIf(expression = {"streamReader"}, result = true)
    private boolean sniffInternal(ExtractorInput input) throws IOException {
        OggPageHeader header = new OggPageHeader();
        if (!header.populate(input, true) || (header.type & 2) != 2) {
            return false;
        }
        int length = Math.min(header.bodySize, 8);
        ParsableByteArray scratch = new ParsableByteArray(length);
        input.peekFully(scratch.getData(), 0, length);
        if (FlacReader.verifyBitstreamType(resetPosition(scratch))) {
            this.streamReader = new FlacReader();
        } else if (VorbisReader.verifyBitstreamType(resetPosition(scratch))) {
            this.streamReader = new VorbisReader();
        } else {
            if (!OpusReader.verifyBitstreamType(resetPosition(scratch))) {
                return false;
            }
            this.streamReader = new OpusReader();
        }
        return true;
    }

    private static ParsableByteArray resetPosition(ParsableByteArray scratch) {
        scratch.setPosition(0);
        return scratch;
    }
}
