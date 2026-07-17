package androidx.media3.exoplayer.source;

import android.media.MediaParser;
import android.net.Uri;
import android.util.Pair;
import androidx.media3.common.DataReader;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.mediaparser.InputReaderAdapterV30;
import androidx.media3.exoplayer.source.mediaparser.MediaParserUtil;
import androidx.media3.exoplayer.source.mediaparser.OutputConsumerAdapterV30;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class MediaParserExtractorAdapter implements ProgressiveMediaExtractor {

    @Deprecated
    public static final ProgressiveMediaExtractor.Factory FACTORY = new ProgressiveMediaExtractor.Factory() { // from class: androidx.media3.exoplayer.source.MediaParserExtractorAdapter$$ExternalSyntheticLambda0
        @Override // androidx.media3.exoplayer.source.ProgressiveMediaExtractor.Factory
        public final ProgressiveMediaExtractor createProgressiveMediaExtractor(PlayerId playerId) {
            return MediaParserExtractorAdapter.lambda$static$0(playerId);
        }
    };
    private final InputReaderAdapterV30 inputReaderAdapter;
    private final MediaParser mediaParser;
    private final OutputConsumerAdapterV30 outputConsumerAdapter;
    private String parserName;

    static /* synthetic */ ProgressiveMediaExtractor lambda$static$0(PlayerId playerId) {
        return new MediaParserExtractorAdapter(playerId, ImmutableMap.of());
    }

    public static final class Factory implements ProgressiveMediaExtractor.Factory {
        private static final Map<String, Object> parameters = new HashMap();

        public void setConstantBitrateSeekingEnabled(boolean enabled) {
            if (enabled) {
                parameters.put("android.media.mediaparser.adts.enableCbrSeeking", true);
                parameters.put("android.media.mediaparser.amr.enableCbrSeeking", true);
                parameters.put("android.media.mediaparser.mp3.enableCbrSeeking", true);
            } else {
                parameters.remove("android.media.mediaparser.adts.enableCbrSeeking");
                parameters.remove("android.media.mediaparser.amr.enableCbrSeeking");
                parameters.remove("android.media.mediaparser.mp3.enableCbrSeeking");
            }
        }

        @Override // androidx.media3.exoplayer.source.ProgressiveMediaExtractor.Factory
        public MediaParserExtractorAdapter createProgressiveMediaExtractor(PlayerId playerId) {
            return new MediaParserExtractorAdapter(playerId, parameters);
        }
    }

    @Deprecated
    public MediaParserExtractorAdapter(PlayerId playerId) {
        this(playerId, ImmutableMap.of());
    }

    private MediaParserExtractorAdapter(PlayerId playerId, Map<String, Object> parameters) {
        this.outputConsumerAdapter = new OutputConsumerAdapterV30();
        this.inputReaderAdapter = new InputReaderAdapterV30();
        this.mediaParser = MediaParser.create(this.outputConsumerAdapter, new String[0]);
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_EAGERLY_EXPOSE_TRACK_TYPE, true);
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_IN_BAND_CRYPTO_INFO, true);
        this.mediaParser.setParameter(MediaParserUtil.PARAMETER_INCLUDE_SUPPLEMENTAL_DATA, true);
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            this.mediaParser.setParameter(parameter.getKey(), parameter.getValue());
        }
        this.parserName = "android.media.mediaparser.UNKNOWN";
        if (Util.SDK_INT >= 31) {
            MediaParserUtil.setLogSessionIdOnMediaParser(this.mediaParser, playerId);
        }
    }

    @Override // androidx.media3.exoplayer.source.ProgressiveMediaExtractor
    public void init(DataReader dataReader, Uri uri, Map<String, List<String>> responseHeaders, long position, long length, ExtractorOutput output) throws IOException {
        this.outputConsumerAdapter.setExtractorOutput(output);
        this.inputReaderAdapter.setDataReader(dataReader, length);
        this.inputReaderAdapter.setCurrentPosition(position);
        String currentParserName = this.mediaParser.getParserName();
        if ("android.media.mediaparser.UNKNOWN".equals(currentParserName)) {
            this.mediaParser.advance(this.inputReaderAdapter);
            this.parserName = this.mediaParser.getParserName();
            this.outputConsumerAdapter.setSelectedParserName(this.parserName);
        } else if (!currentParserName.equals(this.parserName)) {
            this.parserName = this.mediaParser.getParserName();
            this.outputConsumerAdapter.setSelectedParserName(this.parserName);
        }
    }

    @Override // androidx.media3.exoplayer.source.ProgressiveMediaExtractor
    public void release() {
        this.mediaParser.release();
    }

    @Override // androidx.media3.exoplayer.source.ProgressiveMediaExtractor
    public void disableSeekingOnMp3Streams() {
        if ("android.media.mediaparser.Mp3Parser".equals(this.parserName)) {
            this.outputConsumerAdapter.disableSeeking();
        }
    }

    @Override // androidx.media3.exoplayer.source.ProgressiveMediaExtractor
    public long getCurrentInputPosition() {
        return this.inputReaderAdapter.getPosition();
    }

    @Override // androidx.media3.exoplayer.source.ProgressiveMediaExtractor
    public void seek(long position, long seekTimeUs) {
        this.inputReaderAdapter.setCurrentPosition(position);
        Pair<MediaParser.SeekPoint, MediaParser.SeekPoint> seekPoints = this.outputConsumerAdapter.getSeekPoints(seekTimeUs);
        this.mediaParser.seek((MediaParser.SeekPoint) (((MediaParser.SeekPoint) seekPoints.second).position == position ? seekPoints.second : seekPoints.first));
    }

    @Override // androidx.media3.exoplayer.source.ProgressiveMediaExtractor
    public int read(PositionHolder positionHolder) throws IOException {
        boolean shouldContinue = this.mediaParser.advance(this.inputReaderAdapter);
        positionHolder.position = this.inputReaderAdapter.getAndResetSeekPosition();
        if (!shouldContinue) {
            return -1;
        }
        if (positionHolder.position != -1) {
            return 1;
        }
        return 0;
    }
}
