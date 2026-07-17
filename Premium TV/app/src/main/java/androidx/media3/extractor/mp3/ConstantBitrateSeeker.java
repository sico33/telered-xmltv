package androidx.media3.extractor.mp3;

import androidx.media3.extractor.ConstantBitrateSeekMap;
import androidx.media3.extractor.MpegAudioUtil;

/* JADX INFO: loaded from: classes.dex */
final class ConstantBitrateSeeker extends ConstantBitrateSeekMap implements Seeker {
    private final int bitrate;
    private final long dataEndPosition;

    public ConstantBitrateSeeker(long inputLength, long firstFramePosition, MpegAudioUtil.Header mpegAudioHeader, boolean allowSeeksIfLengthUnknown) {
        this(inputLength, firstFramePosition, mpegAudioHeader.bitrate, mpegAudioHeader.frameSize, allowSeeksIfLengthUnknown);
    }

    public ConstantBitrateSeeker(long inputLength, long firstFramePosition, int bitrate, int frameSize, boolean allowSeeksIfLengthUnknown) {
        super(inputLength, firstFramePosition, bitrate, frameSize, allowSeeksIfLengthUnknown);
        this.bitrate = bitrate;
        this.dataEndPosition = inputLength != -1 ? inputLength : -1L;
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public long getTimeUs(long position) {
        return getTimeUsAtPosition(position);
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public long getDataEndPosition() {
        return this.dataEndPosition;
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public int getAverageBitrate() {
        return this.bitrate;
    }
}
