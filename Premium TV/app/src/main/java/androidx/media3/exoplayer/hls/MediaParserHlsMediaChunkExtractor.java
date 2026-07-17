package androidx.media3.exoplayer.hls;

import android.media.MediaFormat;
import android.media.MediaParser;
import android.media.MediaParser$OutputConsumer;
import android.media.MediaParser$SeekableInputReader;
import android.net.Uri;
import android.text.TextUtils;
import androidx.media3.common.FileTypes;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.mediaparser.InputReaderAdapterV30;
import androidx.media3.exoplayer.source.mediaparser.MediaParserUtil;
import androidx.media3.exoplayer.source.mediaparser.OutputConsumerAdapterV30;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class MediaParserHlsMediaChunkExtractor implements HlsMediaChunkExtractor {
    public static final HlsExtractorFactory FACTORY = new HlsExtractorFactory() { // from class: androidx.media3.exoplayer.hls.MediaParserHlsMediaChunkExtractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.exoplayer.hls.HlsExtractorFactory
        public final HlsMediaChunkExtractor createExtractor(Uri uri, Format format, List list, TimestampAdjuster timestampAdjuster, Map map, ExtractorInput extractorInput, PlayerId playerId) {
            return MediaParserHlsMediaChunkExtractor.lambda$static$0(uri, format, list, timestampAdjuster, map, extractorInput, playerId);
        }

        @Override // androidx.media3.exoplayer.hls.HlsExtractorFactory
        public /* synthetic */ HlsExtractorFactory experimentalParseSubtitlesDuringExtraction(boolean z) {
            return HlsExtractorFactory.CC.$default$experimentalParseSubtitlesDuringExtraction(this, z);
        }

        @Override // androidx.media3.exoplayer.hls.HlsExtractorFactory
        public /* synthetic */ Format getOutputTextFormat(Format format) {
            return HlsExtractorFactory.CC.$default$getOutputTextFormat(this, format);
        }

        @Override // androidx.media3.exoplayer.hls.HlsExtractorFactory
        public /* synthetic */ HlsExtractorFactory setSubtitleParserFactory(SubtitleParser.Factory factory) {
            return HlsExtractorFactory.CC.$default$setSubtitleParserFactory(this, factory);
        }
    };
    private final Format format;
    private final InputReaderAdapterV30 inputReaderAdapter = new InputReaderAdapterV30();
    private final MediaParser mediaParser;
    private final ImmutableList<MediaFormat> muxedCaptionMediaFormats;
    private final OutputConsumerAdapterV30 outputConsumerAdapter;
    private final boolean overrideInBandCaptionDeclarations;
    private int pendingSkipBytes;
    private final PlayerId playerId;

    /* JADX WARN: Multi-variable type inference failed */
    static /* synthetic */ HlsMediaChunkExtractor lambda$static$0(Uri uri, Format format, List muxedCaptionFormats, TimestampAdjuster timestampAdjuster, Map responseHeaders, ExtractorInput sniffingExtractorInput, PlayerId playerId) throws IOException {
        if (FileTypes.inferFileTypeFromMimeType(format.sampleMimeType) == 13) {
            return new BundledHlsMediaChunkExtractor(new WebvttExtractor(format.language, timestampAdjuster, SubtitleParser.Factory.UNSUPPORTED, false), format, timestampAdjuster);
        }
        boolean overrideInBandCaptionDeclarations = muxedCaptionFormats != null;
        ImmutableList.Builder<MediaFormat> muxedCaptionMediaFormatsBuilder = ImmutableList.builder();
        if (muxedCaptionFormats != null) {
            for (int i = 0; i < muxedCaptionFormats.size(); i++) {
                muxedCaptionMediaFormatsBuilder.add(MediaParserUtil.toCaptionsMediaFormat((Format) muxedCaptionFormats.get(i)));
            }
        } else {
            muxedCaptionMediaFormatsBuilder.add(MediaParserUtil.toCaptionsMediaFormat(new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_CEA608).build()));
        }
        ImmutableList<MediaFormat> muxedCaptionMediaFormats = muxedCaptionMediaFormatsBuilder.build();
        OutputConsumerAdapterV30 outputConsumerAdapterV30 = new OutputConsumerAdapterV30();
        outputConsumerAdapterV30.setMuxedCaptionFormats(muxedCaptionFormats != null ? muxedCaptionFormats : ImmutableList.of());
        outputConsumerAdapterV30.setTimestampAdjuster(timestampAdjuster);
        MediaParser mediaParser = createMediaParserInstance(outputConsumerAdapterV30, format, overrideInBandCaptionDeclarations, muxedCaptionMediaFormats, playerId, "android.media.mediaparser.FragmentedMp4Parser", "android.media.mediaparser.Ac3Parser", "android.media.mediaparser.Ac4Parser", "android.media.mediaparser.AdtsParser", "android.media.mediaparser.Mp3Parser", "android.media.mediaparser.TsParser");
        PeekingInputReader peekingInputReader = new PeekingInputReader(sniffingExtractorInput);
        mediaParser.advance(peekingInputReader);
        outputConsumerAdapterV30.setSelectedParserName(mediaParser.getParserName());
        return new MediaParserHlsMediaChunkExtractor(mediaParser, outputConsumerAdapterV30, format, overrideInBandCaptionDeclarations, muxedCaptionMediaFormats, peekingInputReader.totalPeekedBytes, playerId);
    }

    public MediaParserHlsMediaChunkExtractor(MediaParser mediaParser, OutputConsumerAdapterV30 outputConsumerAdapter, Format format, boolean overrideInBandCaptionDeclarations, ImmutableList<MediaFormat> muxedCaptionMediaFormats, int leadingBytesToSkip, PlayerId playerId) {
        this.mediaParser = mediaParser;
        this.outputConsumerAdapter = outputConsumerAdapter;
        this.overrideInBandCaptionDeclarations = overrideInBandCaptionDeclarations;
        this.muxedCaptionMediaFormats = muxedCaptionMediaFormats;
        this.format = format;
        this.playerId = playerId;
        this.pendingSkipBytes = leadingBytesToSkip;
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public void init(ExtractorOutput extractorOutput) {
        this.outputConsumerAdapter.setExtractorOutput(extractorOutput);
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public boolean read(ExtractorInput extractorInput) throws IOException {
        extractorInput.skipFully(this.pendingSkipBytes);
        this.pendingSkipBytes = 0;
        this.inputReaderAdapter.setDataReader(extractorInput, extractorInput.getLength());
        return this.mediaParser.advance(this.inputReaderAdapter);
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public boolean isPackedAudioExtractor() {
        String parserName = this.mediaParser.getParserName();
        return "android.media.mediaparser.Ac3Parser".equals(parserName) || "android.media.mediaparser.Ac4Parser".equals(parserName) || "android.media.mediaparser.AdtsParser".equals(parserName) || "android.media.mediaparser.Mp3Parser".equals(parserName);
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public boolean isReusable() {
        String parserName = this.mediaParser.getParserName();
        return "android.media.mediaparser.FragmentedMp4Parser".equals(parserName) || "android.media.mediaparser.TsParser".equals(parserName);
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public HlsMediaChunkExtractor recreate() {
        Assertions.checkState(!isReusable());
        return new MediaParserHlsMediaChunkExtractor(createMediaParserInstance(this.outputConsumerAdapter, this.format, this.overrideInBandCaptionDeclarations, this.muxedCaptionMediaFormats, this.playerId, this.mediaParser.getParserName()), this.outputConsumerAdapter, this.format, this.overrideInBandCaptionDeclarations, this.muxedCaptionMediaFormats, 0, this.playerId);
    }

    @Override // androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
    public void onTruncatedSegmentParsed() {
        this.mediaParser.seek(MediaParser.SeekPoint.START);
    }

    private static MediaParser createMediaParserInstance(MediaParser$OutputConsumer outputConsumer, Format format, boolean overrideInBandCaptionDeclarations, ImmutableList<MediaFormat> muxedCaptionMediaFormats, PlayerId playerId, String... parserNames) {
        MediaParser mediaParser;
        if (parserNames.length == 1) {
            mediaParser = MediaParser.createByName(parserNames[0], outputConsumer);
        } else {
            mediaParser = MediaParser.create(outputConsumer, parserNames);
        }
        mediaParser.setParameter(MediaParserUtil.PARAMETER_EXPOSE_CAPTION_FORMATS, muxedCaptionMediaFormats);
        mediaParser.setParameter(MediaParserUtil.PARAMETER_OVERRIDE_IN_BAND_CAPTION_DECLARATIONS, Boolean.valueOf(overrideInBandCaptionDeclarations));
        mediaParser.setParameter(MediaParserUtil.PARAMETER_IN_BAND_CRYPTO_INFO, true);
        mediaParser.setParameter(MediaParserUtil.PARAMETER_EAGERLY_EXPOSE_TRACK_TYPE, true);
        mediaParser.setParameter(MediaParserUtil.PARAMETER_IGNORE_TIMESTAMP_OFFSET, true);
        mediaParser.setParameter("android.media.mediaparser.ts.ignoreSpliceInfoStream", true);
        mediaParser.setParameter("android.media.mediaparser.ts.mode", "hls");
        String codecs = format.codecs;
        if (!TextUtils.isEmpty(codecs)) {
            if (!MimeTypes.AUDIO_AAC.equals(MimeTypes.getAudioMediaMimeType(codecs))) {
                mediaParser.setParameter("android.media.mediaparser.ts.ignoreAacStream", true);
            }
            if (!MimeTypes.VIDEO_H264.equals(MimeTypes.getVideoMediaMimeType(codecs))) {
                mediaParser.setParameter("android.media.mediaparser.ts.ignoreAvcStream", true);
            }
        }
        if (Util.SDK_INT >= 31) {
            MediaParserUtil.setLogSessionIdOnMediaParser(mediaParser, playerId);
        }
        return mediaParser;
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class PeekingInputReader implements MediaParser$SeekableInputReader {
        private final ExtractorInput extractorInput;
        private int totalPeekedBytes;

        private PeekingInputReader(ExtractorInput extractorInput) {
            this.extractorInput = extractorInput;
        }

        public int read(byte[] buffer, int offset, int readLength) throws IOException {
            int peekedBytes = this.extractorInput.peek(buffer, offset, readLength);
            this.totalPeekedBytes += peekedBytes;
            return peekedBytes;
        }

        public long getPosition() {
            return this.extractorInput.getPeekPosition();
        }

        public long getLength() {
            return this.extractorInput.getLength();
        }

        public void seekToPosition(long position) {
            throw new UnsupportedOperationException();
        }
    }
}
