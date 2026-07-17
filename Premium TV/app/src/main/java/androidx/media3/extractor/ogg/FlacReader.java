package androidx.media3.extractor.ogg;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.FlacFrameReader;
import androidx.media3.extractor.FlacMetadataReader;
import androidx.media3.extractor.FlacSeekTableSeekMap;
import androidx.media3.extractor.FlacStreamMetadata;
import androidx.media3.extractor.SeekMap;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/* JADX INFO: loaded from: classes.dex */
final class FlacReader extends StreamReader {
    private static final byte AUDIO_PACKET_TYPE = -1;
    private static final int FRAME_HEADER_SAMPLE_NUMBER_OFFSET = 4;
    private FlacOggSeeker flacOggSeeker;
    private FlacStreamMetadata streamMetadata;

    FlacReader() {
    }

    public static boolean verifyBitstreamType(ParsableByteArray data) {
        return data.bytesLeft() >= 5 && data.readUnsignedByte() == 127 && data.readUnsignedInt() == 1179402563;
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    protected void reset(boolean headerData) {
        super.reset(headerData);
        if (headerData) {
            this.streamMetadata = null;
            this.flacOggSeeker = null;
        }
    }

    private static boolean isAudioPacket(byte[] data) {
        return data[0] == -1;
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    protected long preparePayload(ParsableByteArray packet) {
        if (!isAudioPacket(packet.getData())) {
            return -1L;
        }
        return getFlacFrameBlockSize(packet);
    }

    @Override // androidx.media3.extractor.ogg.StreamReader
    @EnsuresNonNullIf(expression = {"#3.format"}, result = false)
    protected boolean readHeaders(ParsableByteArray packet, long position, StreamReader.SetupData setupData) {
        byte[] data = packet.getData();
        FlacStreamMetadata streamMetadata = this.streamMetadata;
        if (streamMetadata == null) {
            FlacStreamMetadata streamMetadata2 = new FlacStreamMetadata(data, 17);
            this.streamMetadata = streamMetadata2;
            byte[] metadata = Arrays.copyOfRange(data, 9, packet.limit());
            setupData.format = streamMetadata2.getFormat(metadata, null);
            return true;
        }
        if ((data[0] & 127) == 3) {
            FlacStreamMetadata.SeekTable seekTable = FlacMetadataReader.readSeekTableMetadataBlock(packet);
            FlacStreamMetadata streamMetadata3 = streamMetadata.copyWithSeekTable(seekTable);
            this.streamMetadata = streamMetadata3;
            this.flacOggSeeker = new FlacOggSeeker(streamMetadata3, seekTable);
            return true;
        }
        if (!isAudioPacket(data)) {
            return true;
        }
        if (this.flacOggSeeker != null) {
            this.flacOggSeeker.setFirstFrameOffset(position);
            setupData.oggSeeker = this.flacOggSeeker;
        }
        Assertions.checkNotNull(setupData.format);
        return false;
    }

    private int getFlacFrameBlockSize(ParsableByteArray packet) {
        int blockSizeKey = (packet.getData()[2] & 255) >> 4;
        if (blockSizeKey == 6 || blockSizeKey == 7) {
            packet.skipBytes(4);
            packet.readUtf8EncodedLong();
        }
        int result = FlacFrameReader.readFrameBlockSizeSamplesFromKey(packet, blockSizeKey);
        packet.setPosition(0);
        return result;
    }

    private static final class FlacOggSeeker implements OggSeeker {
        private long firstFrameOffset = -1;
        private long pendingSeekGranule = -1;
        private FlacStreamMetadata.SeekTable seekTable;
        private FlacStreamMetadata streamMetadata;

        public FlacOggSeeker(FlacStreamMetadata streamMetadata, FlacStreamMetadata.SeekTable seekTable) {
            this.streamMetadata = streamMetadata;
            this.seekTable = seekTable;
        }

        public void setFirstFrameOffset(long firstFrameOffset) {
            this.firstFrameOffset = firstFrameOffset;
        }

        @Override // androidx.media3.extractor.ogg.OggSeeker
        public long read(ExtractorInput input) {
            if (this.pendingSeekGranule < 0) {
                return -1L;
            }
            long result = -(this.pendingSeekGranule + 2);
            this.pendingSeekGranule = -1L;
            return result;
        }

        @Override // androidx.media3.extractor.ogg.OggSeeker
        public void startSeek(long targetGranule) {
            long[] seekPointGranules = this.seekTable.pointSampleNumbers;
            int index = Util.binarySearchFloor(seekPointGranules, targetGranule, true, true);
            this.pendingSeekGranule = seekPointGranules[index];
        }

        @Override // androidx.media3.extractor.ogg.OggSeeker
        public SeekMap createSeekMap() {
            Assertions.checkState(this.firstFrameOffset != -1);
            return new FlacSeekTableSeekMap(this.streamMetadata, this.firstFrameOffset);
        }
    }
}
