package androidx.media3.extractor.flv;

import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.AacUtil;
import androidx.media3.extractor.TrackOutput;
import java.util.Collections;

/* JADX INFO: loaded from: classes.dex */
final class AudioTagPayloadReader extends TagPayloadReader {
    private static final int AAC_PACKET_TYPE_AAC_RAW = 1;
    private static final int AAC_PACKET_TYPE_SEQUENCE_HEADER = 0;
    private static final int AUDIO_FORMAT_AAC = 10;
    private static final int AUDIO_FORMAT_ALAW = 7;
    private static final int AUDIO_FORMAT_MP3 = 2;
    private static final int AUDIO_FORMAT_ULAW = 8;
    private static final int[] AUDIO_SAMPLING_RATE_TABLE = {5512, 11025, 22050, 44100};
    private int audioFormat;
    private boolean hasOutputFormat;
    private boolean hasParsedAudioDataHeader;

    public AudioTagPayloadReader(TrackOutput output) {
        super(output);
    }

    @Override // androidx.media3.extractor.flv.TagPayloadReader
    public void seek() {
    }

    @Override // androidx.media3.extractor.flv.TagPayloadReader
    protected boolean parseHeader(ParsableByteArray data) throws TagPayloadReader.UnsupportedFormatException {
        if (!this.hasParsedAudioDataHeader) {
            int header = data.readUnsignedByte();
            this.audioFormat = (header >> 4) & 15;
            if (this.audioFormat == 2) {
                int sampleRateIndex = (header >> 2) & 3;
                int sampleRate = AUDIO_SAMPLING_RATE_TABLE[sampleRateIndex];
                Format format = new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_MPEG).setChannelCount(1).setSampleRate(sampleRate).build();
                this.output.format(format);
                this.hasOutputFormat = true;
            } else if (this.audioFormat == 7 || this.audioFormat == 8) {
                String mimeType = this.audioFormat == 7 ? MimeTypes.AUDIO_ALAW : MimeTypes.AUDIO_MLAW;
                Format format2 = new Format.Builder().setSampleMimeType(mimeType).setChannelCount(1).setSampleRate(8000).build();
                this.output.format(format2);
                this.hasOutputFormat = true;
            } else if (this.audioFormat != 10) {
                throw new TagPayloadReader.UnsupportedFormatException("Audio format not supported: " + this.audioFormat);
            }
            this.hasParsedAudioDataHeader = true;
        } else {
            data.skipBytes(1);
        }
        return true;
    }

    @Override // androidx.media3.extractor.flv.TagPayloadReader
    protected boolean parsePayload(ParsableByteArray data, long timeUs) throws ParserException {
        if (this.audioFormat == 2) {
            int sampleSize = data.bytesLeft();
            this.output.sampleData(data, sampleSize);
            this.output.sampleMetadata(timeUs, 1, sampleSize, 0, null);
            return true;
        }
        int packetType = data.readUnsignedByte();
        if (packetType == 0 && !this.hasOutputFormat) {
            byte[] audioSpecificConfig = new byte[data.bytesLeft()];
            data.readBytes(audioSpecificConfig, 0, audioSpecificConfig.length);
            AacUtil.Config aacConfig = AacUtil.parseAudioSpecificConfig(audioSpecificConfig);
            Format format = new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_AAC).setCodecs(aacConfig.codecs).setChannelCount(aacConfig.channelCount).setSampleRate(aacConfig.sampleRateHz).setInitializationData(Collections.singletonList(audioSpecificConfig)).build();
            this.output.format(format);
            this.hasOutputFormat = true;
            return false;
        }
        if (this.audioFormat == 10 && packetType != 1) {
            return false;
        }
        int sampleSize2 = data.bytesLeft();
        this.output.sampleData(data, sampleSize2);
        this.output.sampleMetadata(timeUs, 1, sampleSize2, 0, null);
        return true;
    }
}
