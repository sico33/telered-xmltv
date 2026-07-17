package androidx.media3.extractor.ogg;

import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.VorbisUtil;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/* JADX INFO: loaded from: classes.dex */
final class VorbisReader extends StreamReader {
    private VorbisUtil.CommentHeader commentHeader;
    private int previousPacketBlockSize;
    private boolean seenFirstAudioPacket;
    private VorbisUtil.VorbisIdHeader vorbisIdHeader;
    private VorbisSetup vorbisSetup;

    VorbisReader() {
    }

    public static boolean verifyBitstreamType(ParsableByteArray data) {
        try {
            return VorbisUtil.verifyVorbisHeaderCapturePattern(1, data, true);
        } catch (ParserException e) {
            return false;
        }
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    protected void reset(boolean headerData) {
        super.reset(headerData);
        if (headerData) {
            this.vorbisSetup = null;
            this.vorbisIdHeader = null;
            this.commentHeader = null;
        }
        this.previousPacketBlockSize = 0;
        this.seenFirstAudioPacket = false;
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    protected void onSeekEnd(long currentGranule) {
        super.onSeekEnd(currentGranule);
        this.seenFirstAudioPacket = currentGranule != 0;
        this.previousPacketBlockSize = this.vorbisIdHeader != null ? this.vorbisIdHeader.blockSize0 : 0;
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    protected long preparePayload(ParsableByteArray packet) {
        if ((packet.getData()[0] & 1) == 1) {
            return -1L;
        }
        int packetBlockSize = decodeBlockSize(packet.getData()[0], (VorbisSetup) Assertions.checkStateNotNull(this.vorbisSetup));
        int samplesInPacket = this.seenFirstAudioPacket ? (this.previousPacketBlockSize + packetBlockSize) / 4 : 0;
        appendNumberOfSamples(packet, samplesInPacket);
        this.seenFirstAudioPacket = true;
        this.previousPacketBlockSize = packetBlockSize;
        return samplesInPacket;
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    @EnsuresNonNullIf(expression = {"#3.format"}, result = false)
    protected boolean readHeaders(ParsableByteArray packet, long position, StreamReader.SetupData setupData) throws IOException {
        if (this.vorbisSetup != null) {
            Assertions.checkNotNull(setupData.format);
            return false;
        }
        this.vorbisSetup = readSetupHeaders(packet);
        if (this.vorbisSetup == null) {
            return true;
        }
        VorbisSetup vorbisSetup = this.vorbisSetup;
        VorbisUtil.VorbisIdHeader idHeader = vorbisSetup.idHeader;
        ArrayList<byte[]> codecInitializationData = new ArrayList<>();
        codecInitializationData.add(idHeader.data);
        codecInitializationData.add(vorbisSetup.setupHeaderData);
        Metadata metadata = VorbisUtil.parseVorbisComments(ImmutableList.copyOf(vorbisSetup.commentHeader.comments));
        setupData.format = new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_VORBIS).setAverageBitrate(idHeader.bitrateNominal).setPeakBitrate(idHeader.bitrateMaximum).setChannelCount(idHeader.channels).setSampleRate(idHeader.sampleRate).setInitializationData(codecInitializationData).setMetadata(metadata).build();
        return true;
    }

    VorbisSetup readSetupHeaders(ParsableByteArray scratch) throws IOException {
        if (this.vorbisIdHeader == null) {
            this.vorbisIdHeader = VorbisUtil.readVorbisIdentificationHeader(scratch);
            return null;
        }
        if (this.commentHeader == null) {
            this.commentHeader = VorbisUtil.readVorbisCommentHeader(scratch);
            return null;
        }
        VorbisUtil.VorbisIdHeader vorbisIdHeader = this.vorbisIdHeader;
        VorbisUtil.CommentHeader commentHeader = this.commentHeader;
        byte[] setupHeaderData = new byte[scratch.limit()];
        System.arraycopy(scratch.getData(), 0, setupHeaderData, 0, scratch.limit());
        VorbisUtil.Mode[] modes = VorbisUtil.readVorbisModes(scratch, vorbisIdHeader.channels);
        int iLogModes = VorbisUtil.iLog(modes.length - 1);
        return new VorbisSetup(vorbisIdHeader, commentHeader, setupHeaderData, modes, iLogModes);
    }

    static int readBits(byte src, int length, int leastSignificantBitIndex) {
        return (src >> leastSignificantBitIndex) & (255 >>> (8 - length));
    }

    static void appendNumberOfSamples(ParsableByteArray buffer, long packetSampleCount) {
        if (buffer.capacity() < buffer.limit() + 4) {
            buffer.reset(Arrays.copyOf(buffer.getData(), buffer.limit() + 4));
        } else {
            buffer.setLimit(buffer.limit() + 4);
        }
        byte[] data = buffer.getData();
        data[buffer.limit() - 4] = (byte) (packetSampleCount & 255);
        data[buffer.limit() - 3] = (byte) ((packetSampleCount >>> 8) & 255);
        data[buffer.limit() - 2] = (byte) ((packetSampleCount >>> 16) & 255);
        data[buffer.limit() - 1] = (byte) (255 & (packetSampleCount >>> 24));
    }

    private static int decodeBlockSize(byte firstByteOfAudioPacket, VorbisSetup vorbisSetup) {
        int modeNumber = readBits(firstByteOfAudioPacket, vorbisSetup.iLogModes, 1);
        if (!vorbisSetup.modes[modeNumber].blockFlag) {
            int currentBlockSize = vorbisSetup.idHeader.blockSize0;
            return currentBlockSize;
        }
        int currentBlockSize2 = vorbisSetup.idHeader.blockSize1;
        return currentBlockSize2;
    }

    static final class VorbisSetup {
        public final VorbisUtil.CommentHeader commentHeader;
        public final int iLogModes;
        public final VorbisUtil.VorbisIdHeader idHeader;
        public final VorbisUtil.Mode[] modes;
        public final byte[] setupHeaderData;

        public VorbisSetup(VorbisUtil.VorbisIdHeader idHeader, VorbisUtil.CommentHeader commentHeader, byte[] setupHeaderData, VorbisUtil.Mode[] modes, int iLogModes) {
            this.idHeader = idHeader;
            this.commentHeader = commentHeader;
            this.setupHeaderData = setupHeaderData;
            this.modes = modes;
            this.iLogModes = iLogModes;
        }
    }
}
