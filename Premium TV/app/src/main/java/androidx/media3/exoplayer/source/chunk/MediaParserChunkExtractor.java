package androidx.media3.exoplayer.source.chunk;

import android.media.MediaFormat;
import android.media.MediaParser;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.mediaparser.InputReaderAdapterV30;
import androidx.media3.exoplayer.source.mediaparser.MediaParserUtil;
import androidx.media3.exoplayer.source.mediaparser.OutputConsumerAdapterV30;
import androidx.media3.extractor.ChunkIndex;
import androidx.media3.extractor.DiscardingTrackOutput;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.text.SubtitleParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class MediaParserChunkExtractor implements ChunkExtractor {
    public static final ChunkExtractor.Factory FACTORY = new ChunkExtractor.Factory() { // from class: androidx.media3.exoplayer.source.chunk.MediaParserChunkExtractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor.Factory
        public final ChunkExtractor createProgressiveMediaExtractor(int i, Format format, boolean z, List list, TrackOutput trackOutput, PlayerId playerId) {
            return MediaParserChunkExtractor.lambda$static$0(i, format, z, list, trackOutput, playerId);
        }

        @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor.Factory
        public /* synthetic */ ChunkExtractor.Factory experimentalParseSubtitlesDuringExtraction(boolean z) {
            return ChunkExtractor.Factory.CC.$default$experimentalParseSubtitlesDuringExtraction(this, z);
        }

        @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor.Factory
        public /* synthetic */ Format getOutputTextFormat(Format format) {
            return ChunkExtractor.Factory.CC.$default$getOutputTextFormat(this, format);
        }

        @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor.Factory
        public /* synthetic */ ChunkExtractor.Factory setSubtitleParserFactory(SubtitleParser.Factory factory) {
            return ChunkExtractor.Factory.CC.$default$setSubtitleParserFactory(this, factory);
        }
    };
    private static final String TAG = "MediaPrsrChunkExtractor";
    private final DiscardingTrackOutput discardingTrackOutput;
    private final InputReaderAdapterV30 inputReaderAdapter = new InputReaderAdapterV30();
    private final MediaParser mediaParser;
    private final OutputConsumerAdapterV30 outputConsumerAdapter;
    private long pendingSeekUs;
    private Format[] sampleFormats;
    private ChunkExtractor.TrackOutputProvider trackOutputProvider;
    private final TrackOutputProviderAdapter trackOutputProviderAdapter;

    static /* synthetic */ ChunkExtractor lambda$static$0(int primaryTrackType, Format format, boolean enableEventMessageTrack, List closedCaptionFormats, TrackOutput playerEmsgTrackOutput, PlayerId playerId) {
        if (!MimeTypes.isText(format.containerMimeType)) {
            return new MediaParserChunkExtractor(primaryTrackType, format, closedCaptionFormats, playerId);
        }
        return null;
    }

    public MediaParserChunkExtractor(int primaryTrackType, Format manifestFormat, List<Format> closedCaptionFormats, PlayerId playerId) {
        String parserName;
        this.outputConsumerAdapter = new OutputConsumerAdapterV30(manifestFormat, primaryTrackType, true);
        String mimeType = (String) Assertions.checkNotNull(manifestFormat.containerMimeType);
        if (MimeTypes.isMatroska(mimeType)) {
            parserName = "android.media.mediaparser.MatroskaParser";
        } else {
            parserName = "android.media.mediaparser.FragmentedMp4Parser";
        }
        this.outputConsumerAdapter.setSelectedParserName(parserName);
        this.mediaParser = MediaParser.createByName(parserName, this.outputConsumerAdapter);
        this.mediaParser.setParameter("android.media.mediaparser.matroska.disableCuesSeeking", true);
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_IN_BAND_CRYPTO_INFO, true);
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_INCLUDE_SUPPLEMENTAL_DATA, true);
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_EAGERLY_EXPOSE_TRACK_TYPE, true);
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_EXPOSE_DUMMY_SEEK_MAP, true);
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_EXPOSE_CHUNK_INDEX_AS_MEDIA_FORMAT, true);
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_OVERRIDE_IN_BAND_CAPTION_DECLARATIONS, true);
        ArrayList<MediaFormat> closedCaptionMediaFormats = new ArrayList<>();
        for (int i = 0; i < closedCaptionFormats.size(); i++) {
            closedCaptionMediaFormats.add(MediaParserUtil.toCaptionsMediaFormat(closedCaptionFormats.get(i)));
        }
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_EXPOSE_CAPTION_FORMATS, closedCaptionMediaFormats);
        if (Util.SDK_INT >= 31) {
            MediaParserUtil.setLogSessionIdOnMediaParser(this.mediaParser, playerId);
        }
        this.outputConsumerAdapter.setMuxedCaptionFormats(closedCaptionFormats);
        this.trackOutputProviderAdapter = new TrackOutputProviderAdapter();
        this.discardingTrackOutput = new DiscardingTrackOutput();
        this.pendingSeekUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor
    public void init(ChunkExtractor.TrackOutputProvider trackOutputProvider, long startTimeUs, long endTimeUs) {
        this.trackOutputProvider = trackOutputProvider;
        this.outputConsumerAdapter.setSampleTimestampUpperLimitFilterUs(endTimeUs);
        this.outputConsumerAdapter.setExtractorOutput(this.trackOutputProviderAdapter);
        this.pendingSeekUs = startTimeUs;
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor
    public void release() {
        this.mediaParser.release();
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor
    public boolean read(ExtractorInput input) throws IOException {
        maybeExecutePendingSeek();
        this.inputReaderAdapter.setDataReader(input, input.getLength());
        return this.mediaParser.advance(this.inputReaderAdapter);
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor
    public ChunkIndex getChunkIndex() {
        return this.outputConsumerAdapter.getChunkIndex();
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor
    public Format[] getSampleFormats() {
        return this.sampleFormats;
    }

    private void maybeExecutePendingSeek() {
        MediaParser.SeekMap dummySeekMap = this.outputConsumerAdapter.getDummySeekMap();
        if (this.pendingSeekUs != C.TIME_UNSET && dummySeekMap != null) {
            this.mediaParser.seek((MediaParser.SeekPoint) dummySeekMap.getSeekPoints(this.pendingSeekUs).first);
            this.pendingSeekUs = C.TIME_UNSET;
        }
    }

    private class TrackOutputProviderAdapter implements ExtractorOutput {
        private TrackOutputProviderAdapter() {
        }

        @Override // androidx.media3.extractor.ExtractorOutput
        public TrackOutput track(int id, int type) {
            ChunkExtractor.TrackOutputProvider trackOutputProvider = MediaParserChunkExtractor.this.trackOutputProvider;
            MediaParserChunkExtractor mediaParserChunkExtractor = MediaParserChunkExtractor.this;
            return trackOutputProvider != null ? mediaParserChunkExtractor.trackOutputProvider.track(id, type) : mediaParserChunkExtractor.discardingTrackOutput;
        }

        @Override // androidx.media3.extractor.ExtractorOutput
        public void endTracks() {
            MediaParserChunkExtractor.this.sampleFormats = MediaParserChunkExtractor.this.outputConsumerAdapter.getSampleFormats();
        }

        @Override // androidx.media3.extractor.ExtractorOutput
        public void seekMap(SeekMap seekMap) {
        }
    }
}
