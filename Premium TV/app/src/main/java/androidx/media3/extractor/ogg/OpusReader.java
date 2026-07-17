package androidx.media3.extractor.ogg;

import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.OpusUtil;
import androidx.media3.extractor.VorbisUtil;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/* JADX INFO: loaded from: classes.dex */
final class OpusReader extends StreamReader {
    private boolean firstCommentHeaderSeen;
    private static final byte[] OPUS_ID_HEADER_SIGNATURE = {79, 112, 117, 115, 72, 101, 97, 100};
    private static final byte[] OPUS_COMMENT_HEADER_SIGNATURE = {79, 112, 117, 115, 84, 97, 103, 115};

    OpusReader() {
    }

    public static boolean verifyBitstreamType(ParsableByteArray data) {
        return peekPacketStartsWith(data, OPUS_ID_HEADER_SIGNATURE);
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    protected void reset(boolean headerData) {
        super.reset(headerData);
        if (headerData) {
            this.firstCommentHeaderSeen = false;
        }
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    protected long preparePayload(ParsableByteArray packet) {
        return convertTimeToGranule(OpusUtil.getPacketDurationUs(packet.getData()));
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    @EnsuresNonNullIf(expression = {"#3.format"}, result = false)
    protected boolean readHeaders(ParsableByteArray packet, long position, StreamReader.SetupData setupData) throws ParserException {
        if (peekPacketStartsWith(packet, OPUS_ID_HEADER_SIGNATURE)) {
            byte[] headerBytes = Arrays.copyOf(packet.getData(), packet.limit());
            int channelCount = OpusUtil.getChannelCount(headerBytes);
            List<byte[]> initializationData = OpusUtil.buildInitializationData(headerBytes);
            if (setupData.format != null) {
                return true;
            }
            setupData.format = new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_OPUS).setChannelCount(channelCount).setSampleRate(OpusUtil.SAMPLE_RATE).setInitializationData(initializationData).build();
            return true;
        }
        if (peekPacketStartsWith(packet, OPUS_COMMENT_HEADER_SIGNATURE)) {
            Assertions.checkStateNotNull(setupData.format);
            if (this.firstCommentHeaderSeen) {
                return true;
            }
            this.firstCommentHeaderSeen = true;
            packet.skipBytes(OPUS_COMMENT_HEADER_SIGNATURE.length);
            VorbisUtil.CommentHeader commentHeader = VorbisUtil.readVorbisCommentHeader(packet, false, false);
            Metadata vorbisMetadata = VorbisUtil.parseVorbisComments(ImmutableList.copyOf(commentHeader.comments));
            if (vorbisMetadata == null) {
                return true;
            }
            setupData.format = setupData.format.buildUpon().setMetadata(vorbisMetadata.copyWithAppendedEntriesFrom(setupData.format.metadata)).build();
            return true;
        }
        Assertions.checkStateNotNull(setupData.format);
        return false;
    }

    private static boolean peekPacketStartsWith(ParsableByteArray packet, byte[] expectedPrefix) {
        if (packet.bytesLeft() < expectedPrefix.length) {
            return false;
        }
        int startPosition = packet.getPosition();
        byte[] header = new byte[expectedPrefix.length];
        packet.readBytes(header, 0, expectedPrefix.length);
        packet.setPosition(startPosition);
        return Arrays.equals(header, expectedPrefix);
    }
}
