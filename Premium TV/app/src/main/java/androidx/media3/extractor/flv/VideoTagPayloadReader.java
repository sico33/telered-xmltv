package androidx.media3.extractor.flv;

import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.container.NalUnitUtil;
import androidx.media3.extractor.AvcConfig;
import androidx.media3.extractor.TrackOutput;

/* JADX INFO: loaded from: classes.dex */
final class VideoTagPayloadReader extends TagPayloadReader {
    private static final int AVC_PACKET_TYPE_AVC_NALU = 1;
    private static final int AVC_PACKET_TYPE_SEQUENCE_HEADER = 0;
    private static final int VIDEO_CODEC_AVC = 7;
    private static final int VIDEO_FRAME_KEYFRAME = 1;
    private static final int VIDEO_FRAME_VIDEO_INFO = 5;
    private int frameType;
    private boolean hasOutputFormat;
    private boolean hasOutputKeyframe;
    private final ParsableByteArray nalLength;
    private final ParsableByteArray nalStartCode;
    private int nalUnitLengthFieldLength;

    public VideoTagPayloadReader(TrackOutput output) {
        super(output);
        this.nalStartCode = new ParsableByteArray(NalUnitUtil.NAL_START_CODE);
        this.nalLength = new ParsableByteArray(4);
    }

    @Override // androidx.media3.extractor.flv.TagPayloadReader
    public void seek() {
        this.hasOutputKeyframe = false;
    }

    @Override // androidx.media3.extractor.flv.TagPayloadReader
    protected boolean parseHeader(ParsableByteArray data) throws TagPayloadReader.UnsupportedFormatException {
        int header = data.readUnsignedByte();
        int frameType = (header >> 4) & 15;
        int videoCodec = header & 15;
        if (videoCodec != 7) {
            throw new TagPayloadReader.UnsupportedFormatException("Video format not supported: " + videoCodec);
        }
        this.frameType = frameType;
        return frameType != 5;
    }

    @Override // androidx.media3.extractor.flv.TagPayloadReader
    protected boolean parsePayload(ParsableByteArray data, long timeUs) throws ParserException {
        int packetType = data.readUnsignedByte();
        int compositionTimeMs = data.readInt24();
        long timeUs2 = timeUs + (((long) compositionTimeMs) * 1000);
        if (packetType == 0 && !this.hasOutputFormat) {
            ParsableByteArray videoSequence = new ParsableByteArray(new byte[data.bytesLeft()]);
            data.readBytes(videoSequence.getData(), 0, data.bytesLeft());
            AvcConfig avcConfig = AvcConfig.parse(videoSequence);
            this.nalUnitLengthFieldLength = avcConfig.nalUnitLengthFieldLength;
            Format format = new Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).setCodecs(avcConfig.codecs).setWidth(avcConfig.width).setHeight(avcConfig.height).setPixelWidthHeightRatio(avcConfig.pixelWidthHeightRatio).setInitializationData(avcConfig.initializationData).build();
            this.output.format(format);
            this.hasOutputFormat = true;
            return false;
        }
        if (packetType != 1 || !this.hasOutputFormat) {
            return false;
        }
        boolean isKeyframe = this.frameType == 1;
        if (!this.hasOutputKeyframe && !isKeyframe) {
            return false;
        }
        byte[] nalLengthData = this.nalLength.getData();
        nalLengthData[0] = 0;
        nalLengthData[1] = 0;
        nalLengthData[2] = 0;
        int nalUnitLengthFieldLengthDiff = 4 - this.nalUnitLengthFieldLength;
        int bytesWritten = 0;
        while (data.bytesLeft() > 0) {
            data.readBytes(this.nalLength.getData(), nalUnitLengthFieldLengthDiff, this.nalUnitLengthFieldLength);
            this.nalLength.setPosition(0);
            int bytesToWrite = this.nalLength.readUnsignedIntToInt();
            this.nalStartCode.setPosition(0);
            this.output.sampleData(this.nalStartCode, 4);
            this.output.sampleData(data, bytesToWrite);
            bytesWritten = bytesWritten + 4 + bytesToWrite;
        }
        this.output.sampleMetadata(timeUs2, isKeyframe ? 1 : 0, bytesWritten, 0, null);
        this.hasOutputKeyframe = true;
        return true;
    }
}
