package androidx.media3.exoplayer.source.mediaparser;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaParser;
import android.media.MediaParser$InputReader;
import android.media.MediaParser$OutputConsumer;
import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.MediaFormatUtil;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ChunkIndex;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.NoOpExtractorOutput;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;
import androidx.media3.extractor.TrackOutput;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public final class OutputConsumerAdapterV30 implements MediaParser$OutputConsumer {
    private static final String MEDIA_FORMAT_KEY_CHUNK_INDEX_DURATIONS = "chunk-index-long-us-durations";
    private static final String MEDIA_FORMAT_KEY_CHUNK_INDEX_OFFSETS = "chunk-index-long-offsets";
    private static final String MEDIA_FORMAT_KEY_CHUNK_INDEX_SIZES = "chunk-index-int-sizes";
    private static final String MEDIA_FORMAT_KEY_CHUNK_INDEX_TIMES = "chunk-index-long-us-times";
    private static final String MEDIA_FORMAT_KEY_TRACK_TYPE = "track-type-string";
    private static final String TAG = "OConsumerAdapterV30";
    private String containerMimeType;
    private MediaParser.SeekMap dummySeekMap;
    private final boolean expectDummySeekMap;
    private ExtractorOutput extractorOutput;
    private ChunkIndex lastChunkIndex;
    private final ArrayList<TrackOutput.CryptoData> lastOutputCryptoDatas;
    private final ArrayList<MediaCodec.CryptoInfo> lastReceivedCryptoInfos;
    private MediaParser.SeekMap lastSeekMap;
    private List<Format> muxedCaptionFormats;
    private int primaryTrackIndex;
    private final Format primaryTrackManifestFormat;
    private final int primaryTrackType;
    private long sampleTimestampUpperLimitFilterUs;
    private final DataReaderAdapter scratchDataReaderAdapter;
    private boolean seekingDisabled;
    private TimestampAdjuster timestampAdjuster;
    private final ArrayList<Format> trackFormats;
    private final ArrayList<TrackOutput> trackOutputs;
    private boolean tracksEnded;
    private boolean tracksFoundCalled;
    private static final Pair<MediaParser.SeekPoint, MediaParser.SeekPoint> SEEK_POINT_PAIR_START = Pair.create(MediaParser.SeekPoint.START, MediaParser.SeekPoint.START);
    private static final Pattern REGEX_CRYPTO_INFO_PATTERN = Pattern.compile("pattern \\(encrypt: (\\d+), skip: (\\d+)\\)");

    public OutputConsumerAdapterV30() {
        this(null, -2, false);
    }

    public OutputConsumerAdapterV30(Format primaryTrackManifestFormat, int primaryTrackType, boolean expectDummySeekMap) {
        this.expectDummySeekMap = expectDummySeekMap;
        this.primaryTrackManifestFormat = primaryTrackManifestFormat;
        this.primaryTrackType = primaryTrackType;
        this.trackOutputs = new ArrayList<>();
        this.trackFormats = new ArrayList<>();
        this.lastReceivedCryptoInfos = new ArrayList<>();
        this.lastOutputCryptoDatas = new ArrayList<>();
        this.scratchDataReaderAdapter = new DataReaderAdapter();
        this.extractorOutput = new NoOpExtractorOutput();
        this.sampleTimestampUpperLimitFilterUs = C.TIME_UNSET;
        this.muxedCaptionFormats = ImmutableList.of();
    }

    public void setSampleTimestampUpperLimitFilterUs(long sampleTimestampUpperLimitFilterUs) {
        this.sampleTimestampUpperLimitFilterUs = sampleTimestampUpperLimitFilterUs;
    }

    public void setTimestampAdjuster(TimestampAdjuster timestampAdjuster) {
        this.timestampAdjuster = timestampAdjuster;
    }

    public void setExtractorOutput(ExtractorOutput extractorOutput) {
        this.extractorOutput = extractorOutput;
    }

    public void setMuxedCaptionFormats(List<Format> muxedCaptionFormats) {
        this.muxedCaptionFormats = muxedCaptionFormats;
    }

    public void disableSeeking() {
        this.seekingDisabled = true;
    }

    public MediaParser.SeekMap getDummySeekMap() {
        return this.dummySeekMap;
    }

    public ChunkIndex getChunkIndex() {
        return this.lastChunkIndex;
    }

    public Pair<MediaParser.SeekPoint, MediaParser.SeekPoint> getSeekPoints(long seekTimeUs) {
        return this.lastSeekMap != null ? this.lastSeekMap.getSeekPoints(seekTimeUs) : SEEK_POINT_PAIR_START;
    }

    public void setSelectedParserName(String parserName) {
        this.containerMimeType = getMimeType(parserName);
    }

    public Format[] getSampleFormats() {
        if (!this.tracksFoundCalled) {
            return null;
        }
        Format[] sampleFormats = new Format[this.trackFormats.size()];
        for (int i = 0; i < this.trackFormats.size(); i++) {
            sampleFormats[i] = (Format) Assertions.checkNotNull(this.trackFormats.get(i));
        }
        return sampleFormats;
    }

    public void onTrackCountFound(int numberOfTracks) {
        this.tracksFoundCalled = true;
        maybeEndTracks();
    }

    public void onSeekMapFound(MediaParser.SeekMap seekMap) {
        SeekMap seekMapAdapter;
        if (this.expectDummySeekMap && this.dummySeekMap == null) {
            this.dummySeekMap = seekMap;
            return;
        }
        this.lastSeekMap = seekMap;
        long durationUs = seekMap.getDurationMicros();
        ExtractorOutput extractorOutput = this.extractorOutput;
        if (this.seekingDisabled) {
            seekMapAdapter = new SeekMap.Unseekable(durationUs != -2147483648L ? durationUs : C.TIME_UNSET);
        } else {
            seekMapAdapter = new SeekMapAdapter(seekMap);
        }
        extractorOutput.seekMap(seekMapAdapter);
    }

    public void onTrackDataFound(int trackIndex, MediaParser.TrackData trackData) {
        Format formatWithManifestFormatInfo;
        String string;
        if (maybeObtainChunkIndex(trackData.mediaFormat)) {
            return;
        }
        ensureSpaceForTrackIndex(trackIndex);
        TrackOutput trackOutput = this.trackOutputs.get(trackIndex);
        if (trackOutput == null) {
            String trackTypeString = trackData.mediaFormat.getString(MEDIA_FORMAT_KEY_TRACK_TYPE);
            if (trackTypeString != null) {
                string = trackTypeString;
            } else {
                string = trackData.mediaFormat.getString("mime");
            }
            int trackType = toTrackTypeConstant(string);
            if (trackType == this.primaryTrackType) {
                this.primaryTrackIndex = trackIndex;
            }
            trackOutput = this.extractorOutput.track(trackIndex, trackType);
            this.trackOutputs.set(trackIndex, trackOutput);
            if (trackTypeString != null) {
                return;
            }
        }
        Format format = toExoPlayerFormat(trackData);
        if (this.primaryTrackManifestFormat != null && trackIndex == this.primaryTrackIndex) {
            formatWithManifestFormatInfo = format.withManifestFormatInfo(this.primaryTrackManifestFormat);
        } else {
            formatWithManifestFormatInfo = format;
        }
        trackOutput.format(formatWithManifestFormatInfo);
        this.trackFormats.set(trackIndex, format);
        maybeEndTracks();
    }

    public void onSampleDataFound(int trackIndex, MediaParser$InputReader sampleData) throws IOException {
        ensureSpaceForTrackIndex(trackIndex);
        this.scratchDataReaderAdapter.input = sampleData;
        TrackOutput trackOutput = this.trackOutputs.get(trackIndex);
        if (trackOutput == null) {
            trackOutput = this.extractorOutput.track(trackIndex, -1);
            this.trackOutputs.set(trackIndex, trackOutput);
        }
        trackOutput.sampleData((DataReader) this.scratchDataReaderAdapter, (int) sampleData.getLength(), true);
    }

    public void onSampleCompleted(int trackIndex, long timeUs, int flags, int size, int offset, MediaCodec.CryptoInfo cryptoInfo) {
        long timeUs2;
        if (this.sampleTimestampUpperLimitFilterUs != C.TIME_UNSET && timeUs >= this.sampleTimestampUpperLimitFilterUs) {
            return;
        }
        if (this.timestampAdjuster == null) {
            timeUs2 = timeUs;
        } else {
            timeUs2 = this.timestampAdjuster.adjustSampleTimestamp(timeUs);
        }
        ((TrackOutput) Assertions.checkNotNull(this.trackOutputs.get(trackIndex))).sampleMetadata(timeUs2, flags, size, offset, toExoPlayerCryptoData(trackIndex, cryptoInfo));
    }

    private boolean maybeObtainChunkIndex(MediaFormat mediaFormat) {
        ByteBuffer chunkIndexSizesByteBuffer = mediaFormat.getByteBuffer(MEDIA_FORMAT_KEY_CHUNK_INDEX_SIZES);
        if (chunkIndexSizesByteBuffer == null) {
            return false;
        }
        IntBuffer chunkIndexSizes = chunkIndexSizesByteBuffer.asIntBuffer();
        LongBuffer chunkIndexOffsets = ((ByteBuffer) Assertions.checkNotNull(mediaFormat.getByteBuffer(MEDIA_FORMAT_KEY_CHUNK_INDEX_OFFSETS))).asLongBuffer();
        LongBuffer chunkIndexDurationsUs = ((ByteBuffer) Assertions.checkNotNull(mediaFormat.getByteBuffer(MEDIA_FORMAT_KEY_CHUNK_INDEX_DURATIONS))).asLongBuffer();
        LongBuffer chunkIndexTimesUs = ((ByteBuffer) Assertions.checkNotNull(mediaFormat.getByteBuffer(MEDIA_FORMAT_KEY_CHUNK_INDEX_TIMES))).asLongBuffer();
        int[] sizes = new int[chunkIndexSizes.remaining()];
        long[] offsets = new long[chunkIndexOffsets.remaining()];
        long[] durationsUs = new long[chunkIndexDurationsUs.remaining()];
        long[] timesUs = new long[chunkIndexTimesUs.remaining()];
        chunkIndexSizes.get(sizes);
        chunkIndexOffsets.get(offsets);
        chunkIndexDurationsUs.get(durationsUs);
        chunkIndexTimesUs.get(timesUs);
        this.lastChunkIndex = new ChunkIndex(sizes, offsets, durationsUs, timesUs);
        this.extractorOutput.seekMap(this.lastChunkIndex);
        return true;
    }

    private void ensureSpaceForTrackIndex(int trackIndex) {
        for (int i = this.trackOutputs.size(); i <= trackIndex; i++) {
            this.trackOutputs.add(null);
            this.trackFormats.add(null);
            this.lastReceivedCryptoInfos.add(null);
            this.lastOutputCryptoDatas.add(null);
        }
    }

    private TrackOutput.CryptoData toExoPlayerCryptoData(int trackIndex, MediaCodec.CryptoInfo cryptoInfo) {
        int encryptedBlocks;
        int clearBlocks;
        if (cryptoInfo == null) {
            return null;
        }
        MediaCodec.CryptoInfo lastReceivedCryptoInfo = this.lastReceivedCryptoInfos.get(trackIndex);
        if (lastReceivedCryptoInfo == cryptoInfo) {
            return (TrackOutput.CryptoData) Assertions.checkNotNull(this.lastOutputCryptoDatas.get(trackIndex));
        }
        try {
            Matcher matcher = REGEX_CRYPTO_INFO_PATTERN.matcher(cryptoInfo.toString());
            matcher.find();
            encryptedBlocks = Integer.parseInt((String) Util.castNonNull(matcher.group(1)));
            clearBlocks = Integer.parseInt((String) Util.castNonNull(matcher.group(2)));
        } catch (RuntimeException e) {
            Log.e(TAG, "Unexpected error while parsing CryptoInfo: " + cryptoInfo, e);
            encryptedBlocks = 0;
            clearBlocks = 0;
        }
        TrackOutput.CryptoData cryptoDataToOutput = new TrackOutput.CryptoData(cryptoInfo.mode, cryptoInfo.key, encryptedBlocks, clearBlocks);
        this.lastReceivedCryptoInfos.set(trackIndex, cryptoInfo);
        this.lastOutputCryptoDatas.set(trackIndex, cryptoDataToOutput);
        return cryptoDataToOutput;
    }

    private void maybeEndTracks() {
        if (!this.tracksFoundCalled || this.tracksEnded) {
            return;
        }
        int size = this.trackOutputs.size();
        for (int i = 0; i < size; i++) {
            if (this.trackOutputs.get(i) == null) {
                return;
            }
        }
        this.extractorOutput.endTracks();
        this.tracksEnded = true;
    }

    private static int toTrackTypeConstant(String string) {
        if (string == null) {
            return -1;
        }
        switch (string) {
            case "audio":
                return 1;
            case "video":
                return 2;
            case "text":
                return 3;
            case "metadata":
                return 5;
            case "unknown":
                return -1;
            default:
                return MimeTypes.getTrackType(string);
        }
    }

    private Format toExoPlayerFormat(MediaParser.TrackData trackData) {
        MediaFormat mediaFormat = trackData.mediaFormat;
        String mediaFormatMimeType = mediaFormat.getString("mime");
        int mediaFormatAccessibilityChannel = mediaFormat.getInteger("caption-service-number", -1);
        Format.Builder formatBuilder = new Format.Builder().setDrmInitData(toExoPlayerDrmInitData(mediaFormat.getString("crypto-mode-fourcc"), trackData.drmInitData)).setContainerMimeType(this.containerMimeType).setPeakBitrate(mediaFormat.getInteger("bitrate", -1)).setChannelCount(mediaFormat.getInteger("channel-count", -1)).setColorInfo(MediaFormatUtil.getColorInfo(mediaFormat)).setSampleMimeType(mediaFormatMimeType).setCodecs(mediaFormat.getString("codecs-string")).setFrameRate(mediaFormat.getFloat("frame-rate", -1.0f)).setWidth(mediaFormat.getInteger("width", -1)).setHeight(mediaFormat.getInteger("height", -1)).setInitializationData(getInitializationData(mediaFormat)).setLanguage(mediaFormat.getString("language")).setMaxInputSize(mediaFormat.getInteger("max-input-size", -1)).setPcmEncoding(mediaFormat.getInteger("exo-pcm-encoding", -1)).setRotationDegrees(mediaFormat.getInteger("rotation-degrees", 0)).setSampleRate(mediaFormat.getInteger("sample-rate", -1)).setSelectionFlags(getSelectionFlags(mediaFormat)).setEncoderDelay(mediaFormat.getInteger("encoder-delay", 0)).setEncoderPadding(mediaFormat.getInteger("encoder-padding", 0)).setPixelWidthHeightRatio(mediaFormat.getFloat("pixel-width-height-ratio-float", 1.0f)).setSubsampleOffsetUs(mediaFormat.getLong("subsample-offset-us-long", Long.MAX_VALUE)).setAccessibilityChannel(mediaFormatAccessibilityChannel);
        for (int i = 0; i < this.muxedCaptionFormats.size(); i++) {
            Format muxedCaptionFormat = this.muxedCaptionFormats.get(i);
            if (Util.areEqual(muxedCaptionFormat.sampleMimeType, mediaFormatMimeType) && muxedCaptionFormat.accessibilityChannel == mediaFormatAccessibilityChannel) {
                formatBuilder.setLanguage(muxedCaptionFormat.language).setRoleFlags(muxedCaptionFormat.roleFlags).setSelectionFlags(muxedCaptionFormat.selectionFlags).setLabel(muxedCaptionFormat.label).setLabels(muxedCaptionFormat.labels).setMetadata(muxedCaptionFormat.metadata);
                break;
            }
        }
        return formatBuilder.build();
    }

    private static DrmInitData toExoPlayerDrmInitData(String schemeType, android.media.DrmInitData drmInitData) {
        if (drmInitData == null) {
            return null;
        }
        DrmInitData.SchemeData[] schemeDatas = new DrmInitData.SchemeData[drmInitData.getSchemeInitDataCount()];
        for (int i = 0; i < schemeDatas.length; i++) {
            android.media.DrmInitData.SchemeInitData schemeInitData = drmInitData.getSchemeInitDataAt(i);
            schemeDatas[i] = new DrmInitData.SchemeData(schemeInitData.uuid, schemeInitData.mimeType, schemeInitData.data);
        }
        return new DrmInitData(schemeType, schemeDatas);
    }

    private static int getSelectionFlags(MediaFormat mediaFormat) {
        int selectionFlags = 0 | getFlag(mediaFormat, "is-autoselect", 4);
        return selectionFlags | getFlag(mediaFormat, "is-default", 1) | getFlag(mediaFormat, "is-forced-subtitle", 2);
    }

    private static int getFlag(MediaFormat mediaFormat, String key, int returnValueIfPresent) {
        if (mediaFormat.getInteger(key, 0) != 0) {
            return returnValueIfPresent;
        }
        return 0;
    }

    private static List<byte[]> getInitializationData(MediaFormat mediaFormat) {
        ArrayList<byte[]> initData = new ArrayList<>();
        int i = 0;
        while (true) {
            int i2 = i + 1;
            ByteBuffer byteBuffer = mediaFormat.getByteBuffer("csd-" + i);
            if (byteBuffer != null) {
                initData.add(MediaFormatUtil.getArray(byteBuffer));
                i = i2;
            } else {
                return initData;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:47:0x009f  */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    private static String getMimeType(String parserName) {
        byte b;
        switch (parserName.hashCode()) {
            case -2063506020:
                if (!parserName.equals("android.media.mediaparser.Mp4Parser")) {
                    b = -1;
                } else {
                    b = 2;
                }
                break;
            case -1870824006:
                if (!parserName.equals("android.media.mediaparser.OggParser")) {
                    b = -1;
                } else {
                    b = 8;
                }
                break;
            case -1566427438:
                if (!parserName.equals("android.media.mediaparser.TsParser")) {
                    b = -1;
                } else {
                    b = 6;
                }
                break;
            case -900207883:
                if (!parserName.equals("android.media.mediaparser.AdtsParser")) {
                    b = -1;
                } else {
                    b = 4;
                }
                break;
            case -589864617:
                if (!parserName.equals("android.media.mediaparser.WavParser")) {
                    b = -1;
                } else {
                    b = 10;
                }
                break;
            case 52265814:
                if (!parserName.equals("android.media.mediaparser.PsParser")) {
                    b = -1;
                } else {
                    b = 9;
                }
                break;
            case 116768877:
                if (!parserName.equals("android.media.mediaparser.FragmentedMp4Parser")) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            case 376876796:
                if (!parserName.equals("android.media.mediaparser.Ac3Parser")) {
                    b = -1;
                } else {
                    b = 5;
                }
                break;
            case 703268017:
                if (!parserName.equals("android.media.mediaparser.AmrParser")) {
                    b = -1;
                } else {
                    b = Ascii.VT;
                }
                break;
            case 768643067:
                if (!parserName.equals("android.media.mediaparser.FlacParser")) {
                    b = -1;
                } else {
                    b = Ascii.CR;
                }
                break;
            case 965962719:
                if (!parserName.equals("android.media.mediaparser.MatroskaParser")) {
                    b = -1;
                } else {
                    b = 0;
                }
                break;
            case 1264380477:
                if (!parserName.equals("android.media.mediaparser.Ac4Parser")) {
                    b = -1;
                } else {
                    b = Ascii.FF;
                }
                break;
            case 1343957595:
                if (!parserName.equals("android.media.mediaparser.Mp3Parser")) {
                    b = -1;
                } else {
                    b = 3;
                }
                break;
            case 2063134683:
                if (!parserName.equals("android.media.mediaparser.FlvParser")) {
                    b = -1;
                } else {
                    b = 7;
                }
                break;
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
                return MimeTypes.VIDEO_WEBM;
            case 1:
            case 2:
                return MimeTypes.VIDEO_MP4;
            case 3:
                return MimeTypes.AUDIO_MPEG;
            case 4:
                return MimeTypes.AUDIO_AAC;
            case 5:
                return MimeTypes.AUDIO_AC3;
            case 6:
                return MimeTypes.VIDEO_MP2T;
            case 7:
                return MimeTypes.VIDEO_FLV;
            case 8:
                return MimeTypes.AUDIO_OGG;
            case 9:
                return MimeTypes.VIDEO_PS;
            case 10:
                return MimeTypes.AUDIO_RAW;
            case 11:
                return MimeTypes.AUDIO_AMR;
            case 12:
                return MimeTypes.AUDIO_AC4;
            case 13:
                return MimeTypes.AUDIO_FLAC;
            default:
                throw new IllegalArgumentException("Illegal parser name: " + parserName);
        }
    }

    private static final class SeekMapAdapter implements SeekMap {
        private final MediaParser.SeekMap adaptedSeekMap;

        public SeekMapAdapter(MediaParser.SeekMap adaptedSeekMap) {
            this.adaptedSeekMap = adaptedSeekMap;
        }

        @Override // androidx.media3.extractor.SeekMap
        public boolean isSeekable() {
            return this.adaptedSeekMap.isSeekable();
        }

        @Override // androidx.media3.extractor.SeekMap
        public long getDurationUs() {
            long durationMicros = this.adaptedSeekMap.getDurationMicros();
            return durationMicros != -2147483648L ? durationMicros : C.TIME_UNSET;
        }

        @Override // androidx.media3.extractor.SeekMap
        public SeekMap.SeekPoints getSeekPoints(long timeUs) {
            Pair<MediaParser.SeekPoint, MediaParser.SeekPoint> seekPoints = this.adaptedSeekMap.getSeekPoints(timeUs);
            if (seekPoints.first == seekPoints.second) {
                SeekMap.SeekPoints exoPlayerSeekPoints = new SeekMap.SeekPoints(asExoPlayerSeekPoint((MediaParser.SeekPoint) seekPoints.first));
                return exoPlayerSeekPoints;
            }
            SeekMap.SeekPoints exoPlayerSeekPoints2 = new SeekMap.SeekPoints(asExoPlayerSeekPoint((MediaParser.SeekPoint) seekPoints.first), asExoPlayerSeekPoint((MediaParser.SeekPoint) seekPoints.second));
            return exoPlayerSeekPoints2;
        }

        private static SeekPoint asExoPlayerSeekPoint(MediaParser.SeekPoint seekPoint) {
            return new SeekPoint(seekPoint.timeMicros, seekPoint.position);
        }
    }

    private static final class DataReaderAdapter implements DataReader {
        public MediaParser$InputReader input;

        private DataReaderAdapter() {
        }

        @Override // androidx.media3.common.DataReader
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return ((MediaParser$InputReader) Util.castNonNull(this.input)).read(buffer, offset, length);
        }
    }
}
