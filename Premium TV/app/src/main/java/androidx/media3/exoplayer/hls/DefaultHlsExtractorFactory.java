package androidx.media3.exoplayer.hls;

import android.net.Uri;
import android.text.TextUtils;
import androidx.media3.common.FileTypes;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.mp3.Mp3Extractor;
import androidx.media3.extractor.mp4.FragmentedMp4Extractor;
import androidx.media3.extractor.text.DefaultSubtitleParserFactory;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.ts.Ac3Extractor;
import androidx.media3.extractor.ts.Ac4Extractor;
import androidx.media3.extractor.ts.AdtsExtractor;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;
import androidx.media3.extractor.ts.TsExtractor;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultHlsExtractorFactory implements HlsExtractorFactory {
    private static final int[] DEFAULT_EXTRACTOR_ORDER = {8, 13, 11, 2, 0, 1, 7};
    private final boolean exposeCea608WhenMissingDeclarations;
    private boolean parseSubtitlesDuringExtraction;
    private final int payloadReaderFactoryFlags;
    private SubtitleParser.Factory subtitleParserFactory;

    @Override // androidx.media3.exoplayer.hls.HlsExtractorFactory
    public /* bridge */ /* synthetic */ HlsMediaChunkExtractor createExtractor(Uri uri, Format format, List list, TimestampAdjuster timestampAdjuster, Map map, ExtractorInput extractorInput, PlayerId playerId) throws IOException {
        return createExtractor(uri, format, (List<Format>) list, timestampAdjuster, (Map<String, List<String>>) map, extractorInput, playerId);
    }

    public DefaultHlsExtractorFactory() {
        this(0, true);
    }

    public DefaultHlsExtractorFactory(int payloadReaderFactoryFlags, boolean exposeCea608WhenMissingDeclarations) {
        this.payloadReaderFactoryFlags = payloadReaderFactoryFlags;
        this.exposeCea608WhenMissingDeclarations = exposeCea608WhenMissingDeclarations;
        this.subtitleParserFactory = new DefaultSubtitleParserFactory();
    }

    @Override // androidx.media3.exoplayer.hls.HlsExtractorFactory
    public BundledHlsMediaChunkExtractor createExtractor(Uri uri, Format format, List<Format> muxedCaptionFormats, TimestampAdjuster timestampAdjuster, Map<String, List<String>> responseHeaders, ExtractorInput sniffingExtractorInput, PlayerId playerId) throws IOException {
        Format format2 = format;
        int formatInferredFileType = FileTypes.inferFileTypeFromMimeType(format2.sampleMimeType);
        int responseHeadersInferredFileType = FileTypes.inferFileTypeFromResponseHeaders(responseHeaders);
        int uriInferredFileType = FileTypes.inferFileTypeFromUri(uri);
        List<Integer> fileTypeOrder = new ArrayList<>(DEFAULT_EXTRACTOR_ORDER.length);
        addFileTypeIfValidAndNotPresent(formatInferredFileType, fileTypeOrder);
        addFileTypeIfValidAndNotPresent(responseHeadersInferredFileType, fileTypeOrder);
        addFileTypeIfValidAndNotPresent(uriInferredFileType, fileTypeOrder);
        for (int i : DEFAULT_EXTRACTOR_ORDER) {
            addFileTypeIfValidAndNotPresent(i, fileTypeOrder);
        }
        sniffingExtractorInput.resetPeekPosition();
        Extractor fallBackExtractor = null;
        int i2 = 0;
        while (i2 < fileTypeOrder.size()) {
            int fileType = fileTypeOrder.get(i2).intValue();
            Extractor extractor = (Extractor) Assertions.checkNotNull(createExtractorByFileType(fileType, format2, muxedCaptionFormats, timestampAdjuster));
            if (sniffQuietly(extractor, sniffingExtractorInput)) {
                return new BundledHlsMediaChunkExtractor(extractor, format2, timestampAdjuster, this.subtitleParserFactory, this.parseSubtitlesDuringExtraction);
            }
            if (fallBackExtractor == null && (fileType == formatInferredFileType || fileType == responseHeadersInferredFileType || fileType == uriInferredFileType || fileType == 11)) {
                fallBackExtractor = extractor;
            }
            i2++;
            format2 = format;
        }
        return new BundledHlsMediaChunkExtractor((Extractor) Assertions.checkNotNull(fallBackExtractor), format, timestampAdjuster, this.subtitleParserFactory, this.parseSubtitlesDuringExtraction);
    }

    @Override // androidx.media3.exoplayer.hls.HlsExtractorFactory
    public DefaultHlsExtractorFactory setSubtitleParserFactory(SubtitleParser.Factory subtitleParserFactory) {
        this.subtitleParserFactory = subtitleParserFactory;
        return this;
    }

    @Override // androidx.media3.exoplayer.hls.HlsExtractorFactory
    public DefaultHlsExtractorFactory experimentalParseSubtitlesDuringExtraction(boolean parseSubtitlesDuringExtraction) {
        this.parseSubtitlesDuringExtraction = parseSubtitlesDuringExtraction;
        return this;
    }

    @Override // androidx.media3.exoplayer.hls.HlsExtractorFactory
    public Format getOutputTextFormat(Format sourceFormat) {
        if (this.parseSubtitlesDuringExtraction && this.subtitleParserFactory.supportsFormat(sourceFormat)) {
            return sourceFormat.buildUpon().setSampleMimeType(MimeTypes.APPLICATION_MEDIA3_CUES).setCueReplacementBehavior(this.subtitleParserFactory.getCueReplacementBehavior(sourceFormat)).setCodecs(sourceFormat.sampleMimeType + (sourceFormat.codecs != null ? " " + sourceFormat.codecs : "")).setSubsampleOffsetUs(Long.MAX_VALUE).build();
        }
        return sourceFormat;
    }

    private static void addFileTypeIfValidAndNotPresent(int fileType, List<Integer> fileTypes) {
        if (Ints.indexOf(DEFAULT_EXTRACTOR_ORDER, fileType) == -1 || fileTypes.contains(Integer.valueOf(fileType))) {
            return;
        }
        fileTypes.add(Integer.valueOf(fileType));
    }

    private Extractor createExtractorByFileType(int fileType, Format format, List<Format> muxedCaptionFormats, TimestampAdjuster timestampAdjuster) {
        switch (fileType) {
            case 0:
                return new Ac3Extractor();
            case 1:
                return new Ac4Extractor();
            case 2:
                return new AdtsExtractor();
            case 7:
                return new Mp3Extractor(0, 0L);
            case 8:
                return createFragmentedMp4Extractor(this.subtitleParserFactory, this.parseSubtitlesDuringExtraction, timestampAdjuster, format, muxedCaptionFormats);
            case 11:
                return createTsExtractor(this.payloadReaderFactoryFlags, this.exposeCea608WhenMissingDeclarations, format, muxedCaptionFormats, timestampAdjuster, this.subtitleParserFactory, this.parseSubtitlesDuringExtraction);
            case 13:
                return new WebvttExtractor(format.language, timestampAdjuster, this.subtitleParserFactory, this.parseSubtitlesDuringExtraction);
            default:
                return null;
        }
    }

    private static TsExtractor createTsExtractor(int userProvidedPayloadReaderFactoryFlags, boolean exposeCea608WhenMissingDeclarations, Format format, List<Format> muxedCaptionFormats, TimestampAdjuster timestampAdjuster, SubtitleParser.Factory subtitleParserFactory, boolean parseSubtitlesDuringExtraction) {
        List<Format> muxedCaptionFormats2;
        SubtitleParser.Factory subtitleParserFactory2;
        int extractorFlags;
        int payloadReaderFactoryFlags = userProvidedPayloadReaderFactoryFlags | 16;
        if (muxedCaptionFormats != null) {
            payloadReaderFactoryFlags |= 32;
            muxedCaptionFormats2 = muxedCaptionFormats;
        } else if (exposeCea608WhenMissingDeclarations) {
            muxedCaptionFormats2 = Collections.singletonList(new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_CEA608).build());
        } else {
            muxedCaptionFormats2 = Collections.emptyList();
        }
        String codecs = format.codecs;
        if (!TextUtils.isEmpty(codecs)) {
            if (!MimeTypes.containsCodecsCorrespondingToMimeType(codecs, MimeTypes.AUDIO_AAC)) {
                payloadReaderFactoryFlags |= 2;
            }
            if (!MimeTypes.containsCodecsCorrespondingToMimeType(codecs, MimeTypes.VIDEO_H264)) {
                payloadReaderFactoryFlags |= 4;
            }
        }
        if (parseSubtitlesDuringExtraction) {
            subtitleParserFactory2 = subtitleParserFactory;
            extractorFlags = 0;
        } else {
            SubtitleParser.Factory subtitleParserFactory3 = SubtitleParser.Factory.UNSUPPORTED;
            int extractorFlags2 = 0 | 1;
            subtitleParserFactory2 = subtitleParserFactory3;
            extractorFlags = extractorFlags2;
        }
        return new TsExtractor(2, extractorFlags, subtitleParserFactory2, timestampAdjuster, new DefaultTsPayloadReaderFactory(payloadReaderFactoryFlags, muxedCaptionFormats2), TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES);
    }

    private static FragmentedMp4Extractor createFragmentedMp4Extractor(SubtitleParser.Factory subtitleParserFactory, boolean parseSubtitlesDuringExtraction, TimestampAdjuster timestampAdjuster, Format format, List<Format> muxedCaptionFormats) {
        SubtitleParser.Factory subtitleParserFactory2;
        int flags;
        int flags2 = isFmp4Variant(format) ? 4 : 0;
        if (parseSubtitlesDuringExtraction) {
            subtitleParserFactory2 = subtitleParserFactory;
            flags = flags2;
        } else {
            SubtitleParser.Factory subtitleParserFactory3 = SubtitleParser.Factory.UNSUPPORTED;
            subtitleParserFactory2 = subtitleParserFactory3;
            flags = flags2 | 32;
        }
        return new FragmentedMp4Extractor(subtitleParserFactory2, flags, timestampAdjuster, null, muxedCaptionFormats != null ? muxedCaptionFormats : ImmutableList.of(), null);
    }

    private static boolean isFmp4Variant(Format format) {
        Metadata metadata = format.metadata;
        if (metadata == null) {
            return false;
        }
        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry entry = metadata.get(i);
            if (entry instanceof HlsTrackMetadataEntry) {
                return !((HlsTrackMetadataEntry) entry).variantInfos.isEmpty();
            }
        }
        return false;
    }

    private static boolean sniffQuietly(Extractor extractor, ExtractorInput input) throws IOException {
        boolean result = false;
        try {
            result = extractor.sniff(input);
        } catch (EOFException e) {
        } finally {
            input.resetPeekPosition();
        }
        return result;
    }
}
