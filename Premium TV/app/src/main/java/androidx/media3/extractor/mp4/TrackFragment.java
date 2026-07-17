package androidx.media3.extractor.mp4;

import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.ExtractorInput;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class TrackFragment {
    public long atomPosition;
    public long auxiliaryDataPosition;
    public long dataPosition;
    public boolean definesEncryptionData;
    public DefaultSampleValues header;
    public long nextFragmentDecodeTime;
    public boolean nextFragmentDecodeTimeIncludesMoov;
    public int sampleCount;
    public boolean sampleEncryptionDataNeedsFill;
    public TrackEncryptionBox trackEncryptionBox;
    public int trunCount;
    public long[] trunDataPosition = new long[0];
    public int[] trunLength = new int[0];
    public int[] sampleSizeTable = new int[0];
    public long[] samplePresentationTimesUs = new long[0];
    public boolean[] sampleIsSyncFrameTable = new boolean[0];
    public boolean[] sampleHasSubsampleEncryptionTable = new boolean[0];
    public final ParsableByteArray sampleEncryptionData = new ParsableByteArray();

    public void reset() {
        this.trunCount = 0;
        this.nextFragmentDecodeTime = 0L;
        this.nextFragmentDecodeTimeIncludesMoov = false;
        this.definesEncryptionData = false;
        this.sampleEncryptionDataNeedsFill = false;
        this.trackEncryptionBox = null;
    }

    public void initTables(int trunCount, int sampleCount) {
        this.trunCount = trunCount;
        this.sampleCount = sampleCount;
        if (this.trunLength.length < trunCount) {
            this.trunDataPosition = new long[trunCount];
            this.trunLength = new int[trunCount];
        }
        if (this.sampleSizeTable.length < sampleCount) {
            int tableSize = (sampleCount * 125) / 100;
            this.sampleSizeTable = new int[tableSize];
            this.samplePresentationTimesUs = new long[tableSize];
            this.sampleIsSyncFrameTable = new boolean[tableSize];
            this.sampleHasSubsampleEncryptionTable = new boolean[tableSize];
        }
    }

    public void initEncryptionData(int length) {
        this.sampleEncryptionData.reset(length);
        this.definesEncryptionData = true;
        this.sampleEncryptionDataNeedsFill = true;
    }

    public void fillEncryptionData(ExtractorInput input) throws IOException {
        input.readFully(this.sampleEncryptionData.getData(), 0, this.sampleEncryptionData.limit());
        this.sampleEncryptionData.setPosition(0);
        this.sampleEncryptionDataNeedsFill = false;
    }

    public void fillEncryptionData(ParsableByteArray source) {
        source.readBytes(this.sampleEncryptionData.getData(), 0, this.sampleEncryptionData.limit());
        this.sampleEncryptionData.setPosition(0);
        this.sampleEncryptionDataNeedsFill = false;
    }

    public long getSamplePresentationTimeUs(int index) {
        return this.samplePresentationTimesUs[index];
    }

    public boolean sampleHasSubsampleEncryptionTable(int index) {
        return this.definesEncryptionData && this.sampleHasSubsampleEncryptionTable[index];
    }
}
