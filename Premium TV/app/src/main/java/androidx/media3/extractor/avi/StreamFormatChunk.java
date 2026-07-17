package androidx.media3.extractor.avi;

import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import com.google.common.collect.ImmutableList;

/* JADX INFO: loaded from: classes.dex */
final class StreamFormatChunk implements AviChunk {
    private static final String TAG = "StreamFormatChunk";
    public final Format format;

    public static AviChunk parseFrom(int trackType, ParsableByteArray body) {
        if (trackType == 2) {
            return parseBitmapInfoHeader(body);
        }
        if (trackType == 1) {
            return parseWaveFormatEx(body);
        }
        Log.w(TAG, "Ignoring strf box for unsupported track type: " + Util.getTrackTypeString(trackType));
        return null;
    }

    public StreamFormatChunk(Format format) {
        this.format = format;
    }

    @Override // androidx.media3.extractor.avi.AviChunk
    public int getType() {
        return AviExtractor.FOURCC_strf;
    }

    private static AviChunk parseBitmapInfoHeader(ParsableByteArray body) {
        body.skipBytes(4);
        int width = body.readLittleEndianInt();
        int height = body.readLittleEndianInt();
        body.skipBytes(4);
        int compression = body.readLittleEndianInt();
        String mimeType = getMimeTypeFromCompression(compression);
        if (mimeType == null) {
            Log.w(TAG, "Ignoring track with unsupported compression " + compression);
            return null;
        }
        Format.Builder formatBuilder = new Format.Builder();
        formatBuilder.setWidth(width).setHeight(height).setSampleMimeType(mimeType);
        return new StreamFormatChunk(formatBuilder.build());
    }

    private static AviChunk parseWaveFormatEx(ParsableByteArray body) {
        int formatTag = body.readLittleEndianUnsignedShort();
        String mimeType = getMimeTypeFromTag(formatTag);
        if (mimeType == null) {
            Log.w(TAG, "Ignoring track with unsupported format tag " + formatTag);
            return null;
        }
        int channelCount = body.readLittleEndianUnsignedShort();
        int samplesPerSecond = body.readLittleEndianInt();
        body.skipBytes(6);
        int bitsPerSample = body.readLittleEndianUnsignedShort();
        int pcmEncoding = Util.getPcmEncoding(bitsPerSample);
        int cbSize = body.bytesLeft() > 0 ? body.readLittleEndianUnsignedShort() : 0;
        byte[] codecData = new byte[cbSize];
        body.readBytes(codecData, 0, codecData.length);
        Format.Builder formatBuilder = new Format.Builder();
        formatBuilder.setSampleMimeType(mimeType).setChannelCount(channelCount).setSampleRate(samplesPerSecond);
        if (MimeTypes.AUDIO_RAW.equals(mimeType) && pcmEncoding != 0) {
            formatBuilder.setPcmEncoding(pcmEncoding);
        }
        if (MimeTypes.AUDIO_AAC.equals(mimeType) && codecData.length > 0) {
            formatBuilder.setInitializationData(ImmutableList.of(codecData));
        }
        return new StreamFormatChunk(formatBuilder.build());
    }

    private static String getMimeTypeFromTag(int tag) {
        switch (tag) {
            case 1:
                return MimeTypes.AUDIO_RAW;
            case 85:
                return MimeTypes.AUDIO_MPEG;
            case 255:
                return MimeTypes.AUDIO_AAC;
            case 8192:
                return MimeTypes.AUDIO_AC3;
            case 8193:
                return MimeTypes.AUDIO_DTS;
            default:
                return null;
        }
    }

    private static String getMimeTypeFromCompression(int compression) {
        switch (compression) {
            case 808802372:
            case 877677894:
            case 1145656883:
            case 1145656920:
            case 1482049860:
            case 1684633208:
            case 2021026148:
                return MimeTypes.VIDEO_MP4V;
            case 826496577:
            case 828601953:
            case 875967048:
                return MimeTypes.VIDEO_H264;
            case 842289229:
                return MimeTypes.VIDEO_MP42;
            case 859066445:
                return MimeTypes.VIDEO_MP43;
            case 1196444237:
            case 1735420525:
                return MimeTypes.VIDEO_MJPEG;
            default:
                return null;
        }
    }
}
