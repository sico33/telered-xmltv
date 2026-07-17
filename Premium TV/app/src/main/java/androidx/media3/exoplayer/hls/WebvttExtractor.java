package androidx.media3.exoplayer.hls;

import android.text.TextUtils;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.text.SubtitleTranscodingExtractorOutput;
import androidx.media3.extractor.text.webvtt.WebvttParserUtil;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class WebvttExtractor implements Extractor {
    private static final int HEADER_MAX_LENGTH = 9;
    private static final int HEADER_MIN_LENGTH = 6;
    private static final Pattern LOCAL_TIMESTAMP = Pattern.compile("LOCAL:([^,]+)");
    private static final Pattern MEDIA_TIMESTAMP = Pattern.compile("MPEGTS:(-?\\d+)");
    private final String language;
    private ExtractorOutput output;
    private final boolean parseSubtitlesDuringExtraction;
    private byte[] sampleData;
    private final ParsableByteArray sampleDataWrapper;
    private int sampleSize;
    private final SubtitleParser.Factory subtitleParserFactory;
    private final TimestampAdjuster timestampAdjuster;

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ List getSniffFailureDetails() {
        return ImmutableList.of();
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ Extractor getUnderlyingImplementation() {
        return Extractor.CC.$default$getUnderlyingImplementation(this);
    }

    @Deprecated
    public WebvttExtractor(String language, TimestampAdjuster timestampAdjuster) {
        this(language, timestampAdjuster, SubtitleParser.Factory.UNSUPPORTED, false);
    }

    public WebvttExtractor(String language, TimestampAdjuster timestampAdjuster, SubtitleParser.Factory subtitleParserFactory, boolean parseSubtitlesDuringExtraction) {
        this.language = language;
        this.timestampAdjuster = timestampAdjuster;
        this.sampleDataWrapper = new ParsableByteArray();
        this.sampleData = new byte[1024];
        this.subtitleParserFactory = subtitleParserFactory;
        this.parseSubtitlesDuringExtraction = parseSubtitlesDuringExtraction;
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        input.peekFully(this.sampleData, 0, 6, false);
        this.sampleDataWrapper.reset(this.sampleData, 6);
        if (WebvttParserUtil.isWebvttHeaderLine(this.sampleDataWrapper)) {
            return true;
        }
        input.peekFully(this.sampleData, 6, 3, false);
        this.sampleDataWrapper.reset(this.sampleData, 9);
        return WebvttParserUtil.isWebvttHeaderLine(this.sampleDataWrapper);
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        ExtractorOutput subtitleTranscodingExtractorOutput;
        if (this.parseSubtitlesDuringExtraction) {
            subtitleTranscodingExtractorOutput = new SubtitleTranscodingExtractorOutput(output, this.subtitleParserFactory);
        } else {
            subtitleTranscodingExtractorOutput = output;
        }
        this.output = subtitleTranscodingExtractorOutput;
        output.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        throw new IllegalStateException();
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        Assertions.checkNotNull(this.output);
        int currentFileSize = (int) input.getLength();
        if (this.sampleSize == this.sampleData.length) {
            this.sampleData = Arrays.copyOf(this.sampleData, ((currentFileSize != -1 ? currentFileSize : this.sampleData.length) * 3) / 2);
        }
        int bytesRead = input.read(this.sampleData, this.sampleSize, this.sampleData.length - this.sampleSize);
        if (bytesRead != -1) {
            this.sampleSize += bytesRead;
            if (currentFileSize == -1 || this.sampleSize != currentFileSize) {
                return 0;
            }
        }
        processSample();
        return -1;
    }

    @RequiresNonNull({"output"})
    private void processSample() throws ParserException {
        ParsableByteArray webvttData = new ParsableByteArray(this.sampleData);
        WebvttParserUtil.validateWebvttHeaderLine(webvttData);
        long vttTimestampUs = 0;
        long tsTimestampUs = 0;
        for (String line = webvttData.readLine(); !TextUtils.isEmpty(line); line = webvttData.readLine()) {
            if (line.startsWith("X-TIMESTAMP-MAP")) {
                Matcher localTimestampMatcher = LOCAL_TIMESTAMP.matcher(line);
                if (!localTimestampMatcher.find()) {
                    throw ParserException.createForMalformedContainer("X-TIMESTAMP-MAP doesn't contain local timestamp: " + line, null);
                }
                Matcher mediaTimestampMatcher = MEDIA_TIMESTAMP.matcher(line);
                if (!mediaTimestampMatcher.find()) {
                    throw ParserException.createForMalformedContainer("X-TIMESTAMP-MAP doesn't contain media timestamp: " + line, null);
                }
                vttTimestampUs = WebvttParserUtil.parseTimestampUs((String) Assertions.checkNotNull(localTimestampMatcher.group(1)));
                tsTimestampUs = TimestampAdjuster.ptsToUs(Long.parseLong((String) Assertions.checkNotNull(mediaTimestampMatcher.group(1))));
            }
        }
        Matcher cueHeaderMatcher = WebvttParserUtil.findNextCueHeader(webvttData);
        if (cueHeaderMatcher == null) {
            buildTrackOutput(0L);
            return;
        }
        long firstCueTimeUs = WebvttParserUtil.parseTimestampUs((String) Assertions.checkNotNull(cueHeaderMatcher.group(1)));
        long sampleTimeUs = this.timestampAdjuster.adjustTsTimestamp(TimestampAdjuster.usToWrappedPts((firstCueTimeUs + tsTimestampUs) - vttTimestampUs));
        long subsampleOffsetUs = sampleTimeUs - firstCueTimeUs;
        TrackOutput trackOutput = buildTrackOutput(subsampleOffsetUs);
        this.sampleDataWrapper.reset(this.sampleData, this.sampleSize);
        trackOutput.sampleData(this.sampleDataWrapper, this.sampleSize);
        trackOutput.sampleMetadata(sampleTimeUs, 1, this.sampleSize, 0, null);
    }

    @RequiresNonNull({"output"})
    private TrackOutput buildTrackOutput(long subsampleOffsetUs) {
        TrackOutput trackOutput = this.output.track(0, 3);
        trackOutput.format(new Format.Builder().setSampleMimeType(MimeTypes.TEXT_VTT).setLanguage(this.language).setSubsampleOffsetUs(subsampleOffsetUs).build());
        this.output.endTracks();
        return trackOutput;
    }
}
