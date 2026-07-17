package androidx.media3.extractor;

import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.metadata.flac.PictureFrame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class FlacStreamMetadata {
    public static final int NOT_IN_LOOKUP_TABLE = -1;
    private static final String TAG = "FlacStreamMetadata";
    public final int bitsPerSample;
    public final int bitsPerSampleLookupKey;
    public final int channels;
    public final int maxBlockSizeSamples;
    public final int maxFrameSize;
    private final Metadata metadata;
    public final int minBlockSizeSamples;
    public final int minFrameSize;
    public final int sampleRate;
    public final int sampleRateLookupKey;
    public final SeekTable seekTable;
    public final long totalSamples;

    public static class SeekTable {
        public final long[] pointOffsets;
        public final long[] pointSampleNumbers;

        public SeekTable(long[] pointSampleNumbers, long[] pointOffsets) {
            this.pointSampleNumbers = pointSampleNumbers;
            this.pointOffsets = pointOffsets;
        }
    }

    public FlacStreamMetadata(byte[] data, int offset) {
        ParsableBitArray scratch = new ParsableBitArray(data);
        scratch.setPosition(offset * 8);
        this.minBlockSizeSamples = scratch.readBits(16);
        this.maxBlockSizeSamples = scratch.readBits(16);
        this.minFrameSize = scratch.readBits(24);
        this.maxFrameSize = scratch.readBits(24);
        this.sampleRate = scratch.readBits(20);
        this.sampleRateLookupKey = getSampleRateLookupKey(this.sampleRate);
        this.channels = scratch.readBits(3) + 1;
        this.bitsPerSample = scratch.readBits(5) + 1;
        this.bitsPerSampleLookupKey = getBitsPerSampleLookupKey(this.bitsPerSample);
        this.totalSamples = scratch.readBitsToLong(36);
        this.seekTable = null;
        this.metadata = null;
    }

    public FlacStreamMetadata(int minBlockSizeSamples, int maxBlockSizeSamples, int minFrameSize, int maxFrameSize, int sampleRate, int channels, int bitsPerSample, long totalSamples, ArrayList<String> vorbisComments, ArrayList<PictureFrame> pictureFrames) {
        this(minBlockSizeSamples, maxBlockSizeSamples, minFrameSize, maxFrameSize, sampleRate, channels, bitsPerSample, totalSamples, (SeekTable) null, concatenateVorbisMetadata(vorbisComments, pictureFrames));
    }

    private FlacStreamMetadata(int minBlockSizeSamples, int maxBlockSizeSamples, int minFrameSize, int maxFrameSize, int sampleRate, int channels, int bitsPerSample, long totalSamples, SeekTable seekTable, Metadata metadata) {
        this.minBlockSizeSamples = minBlockSizeSamples;
        this.maxBlockSizeSamples = maxBlockSizeSamples;
        this.minFrameSize = minFrameSize;
        this.maxFrameSize = maxFrameSize;
        this.sampleRate = sampleRate;
        this.sampleRateLookupKey = getSampleRateLookupKey(sampleRate);
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.bitsPerSampleLookupKey = getBitsPerSampleLookupKey(bitsPerSample);
        this.totalSamples = totalSamples;
        this.seekTable = seekTable;
        this.metadata = metadata;
    }

    public int getMaxDecodedFrameSize() {
        return this.maxBlockSizeSamples * this.channels * (this.bitsPerSample / 8);
    }

    public int getDecodedBitrate() {
        return this.bitsPerSample * this.sampleRate * this.channels;
    }

    public long getDurationUs() {
        return this.totalSamples == 0 ? C.TIME_UNSET : (this.totalSamples * 1000000) / ((long) this.sampleRate);
    }

    public long getSampleNumber(long timeUs) {
        long sampleNumber = (((long) this.sampleRate) * timeUs) / 1000000;
        return Util.constrainValue(sampleNumber, 0L, this.totalSamples - 1);
    }

    public long getApproxBytesPerFrame() {
        long blockSizeSamples;
        if (this.maxFrameSize > 0) {
            long approxBytesPerFrame = ((((long) this.maxFrameSize) + ((long) this.minFrameSize)) / 2) + 1;
            return approxBytesPerFrame;
        }
        if (this.minBlockSizeSamples == this.maxBlockSizeSamples && this.minBlockSizeSamples > 0) {
            blockSizeSamples = this.minBlockSizeSamples;
        } else {
            blockSizeSamples = PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
        }
        long approxBytesPerFrame2 = (((((long) this.channels) * blockSizeSamples) * ((long) this.bitsPerSample)) / 8) + 64;
        return approxBytesPerFrame2;
    }

    public Format getFormat(byte[] streamMarkerAndInfoBlock, Metadata id3Metadata) {
        streamMarkerAndInfoBlock[4] = -128;
        int maxInputSize = this.maxFrameSize > 0 ? this.maxFrameSize : -1;
        Metadata metadataWithId3 = getMetadataCopyWithAppendedEntriesFrom(id3Metadata);
        return new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_FLAC).setMaxInputSize(maxInputSize).setChannelCount(this.channels).setSampleRate(this.sampleRate).setPcmEncoding(Util.getPcmEncoding(this.bitsPerSample)).setInitializationData(Collections.singletonList(streamMarkerAndInfoBlock)).setMetadata(metadataWithId3).build();
    }

    public Metadata getMetadataCopyWithAppendedEntriesFrom(Metadata other) {
        return this.metadata == null ? other : this.metadata.copyWithAppendedEntriesFrom(other);
    }

    public FlacStreamMetadata copyWithSeekTable(SeekTable seekTable) {
        return new FlacStreamMetadata(this.minBlockSizeSamples, this.maxBlockSizeSamples, this.minFrameSize, this.maxFrameSize, this.sampleRate, this.channels, this.bitsPerSample, this.totalSamples, seekTable, this.metadata);
    }

    public FlacStreamMetadata copyWithVorbisComments(List<String> vorbisComments) {
        Metadata appendedMetadata = getMetadataCopyWithAppendedEntriesFrom(VorbisUtil.parseVorbisComments(vorbisComments));
        return new FlacStreamMetadata(this.minBlockSizeSamples, this.maxBlockSizeSamples, this.minFrameSize, this.maxFrameSize, this.sampleRate, this.channels, this.bitsPerSample, this.totalSamples, this.seekTable, appendedMetadata);
    }

    public FlacStreamMetadata copyWithPictureFrames(List<PictureFrame> pictureFrames) {
        Metadata appendedMetadata = getMetadataCopyWithAppendedEntriesFrom(new Metadata(pictureFrames));
        return new FlacStreamMetadata(this.minBlockSizeSamples, this.maxBlockSizeSamples, this.minFrameSize, this.maxFrameSize, this.sampleRate, this.channels, this.bitsPerSample, this.totalSamples, this.seekTable, appendedMetadata);
    }

    private static Metadata concatenateVorbisMetadata(List<String> vorbisComments, List<PictureFrame> pictureFrames) {
        Metadata parsedVorbisComments = VorbisUtil.parseVorbisComments(vorbisComments);
        if (parsedVorbisComments == null && pictureFrames.isEmpty()) {
            return null;
        }
        return new Metadata(pictureFrames).copyWithAppendedEntriesFrom(parsedVorbisComments);
    }

    private static int getSampleRateLookupKey(int sampleRate) {
        switch (sampleRate) {
            case 8000:
                return 4;
            case AacUtil.AAC_HE_V1_MAX_RATE_BYTES_PER_SECOND /* 16000 */:
                return 5;
            case 22050:
                return 6;
            case 24000:
                return 7;
            case 32000:
                return 8;
            case 44100:
                return 9;
            case OpusUtil.SAMPLE_RATE /* 48000 */:
                return 10;
            case 88200:
                return 1;
            case 96000:
                return 11;
            case 176400:
                return 2;
            case DtsUtil.DTS_MAX_RATE_BYTES_PER_SECOND /* 192000 */:
                return 3;
            default:
                return -1;
        }
    }

    private static int getBitsPerSampleLookupKey(int bitsPerSample) {
        switch (bitsPerSample) {
            case 8:
                return 1;
            case 12:
                return 2;
            case 16:
                return 4;
            case 20:
                return 5;
            case 24:
                return 6;
            default:
                return -1;
        }
    }
}
