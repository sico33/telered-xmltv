package androidx.media3.extractor.metadata.scte35;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class SpliceInsertCommand extends SpliceCommand {
    public static final Parcelable.Creator<SpliceInsertCommand> CREATOR = new Parcelable.Creator<SpliceInsertCommand>() { // from class: androidx.media3.extractor.metadata.scte35.SpliceInsertCommand.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SpliceInsertCommand createFromParcel(Parcel in) {
            return new SpliceInsertCommand(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SpliceInsertCommand[] newArray(int size) {
            return new SpliceInsertCommand[size];
        }
    };
    public final boolean autoReturn;
    public final int availNum;
    public final int availsExpected;
    public final long breakDurationUs;
    public final List<ComponentSplice> componentSpliceList;
    public final boolean outOfNetworkIndicator;
    public final boolean programSpliceFlag;
    public final long programSplicePlaybackPositionUs;
    public final long programSplicePts;
    public final boolean spliceEventCancelIndicator;
    public final long spliceEventId;
    public final boolean spliceImmediateFlag;
    public final int uniqueProgramId;

    private SpliceInsertCommand(long spliceEventId, boolean spliceEventCancelIndicator, boolean outOfNetworkIndicator, boolean programSpliceFlag, boolean spliceImmediateFlag, long programSplicePts, long programSplicePlaybackPositionUs, List<ComponentSplice> componentSpliceList, boolean autoReturn, long breakDurationUs, int uniqueProgramId, int availNum, int availsExpected) {
        this.spliceEventId = spliceEventId;
        this.spliceEventCancelIndicator = spliceEventCancelIndicator;
        this.outOfNetworkIndicator = outOfNetworkIndicator;
        this.programSpliceFlag = programSpliceFlag;
        this.spliceImmediateFlag = spliceImmediateFlag;
        this.programSplicePts = programSplicePts;
        this.programSplicePlaybackPositionUs = programSplicePlaybackPositionUs;
        this.componentSpliceList = Collections.unmodifiableList(componentSpliceList);
        this.autoReturn = autoReturn;
        this.breakDurationUs = breakDurationUs;
        this.uniqueProgramId = uniqueProgramId;
        this.availNum = availNum;
        this.availsExpected = availsExpected;
    }

    private SpliceInsertCommand(Parcel in) {
        this.spliceEventId = in.readLong();
        this.spliceEventCancelIndicator = in.readByte() == 1;
        this.outOfNetworkIndicator = in.readByte() == 1;
        this.programSpliceFlag = in.readByte() == 1;
        this.spliceImmediateFlag = in.readByte() == 1;
        this.programSplicePts = in.readLong();
        this.programSplicePlaybackPositionUs = in.readLong();
        int componentSpliceListSize = in.readInt();
        List<ComponentSplice> componentSpliceList = new ArrayList<>(componentSpliceListSize);
        for (int i = 0; i < componentSpliceListSize; i++) {
            componentSpliceList.add(ComponentSplice.createFromParcel(in));
        }
        this.componentSpliceList = Collections.unmodifiableList(componentSpliceList);
        this.autoReturn = in.readByte() == 1;
        this.breakDurationUs = in.readLong();
        this.uniqueProgramId = in.readInt();
        this.availNum = in.readInt();
        this.availsExpected = in.readInt();
    }

    static SpliceInsertCommand parseFromSection(ParsableByteArray sectionData, long ptsAdjustment, TimestampAdjuster timestampAdjuster) {
        boolean durationFlag;
        boolean outOfNetworkIndicator;
        boolean programSpliceFlag;
        long programSplicePts;
        int availsExpected;
        List<ComponentSplice> componentSplices;
        int uniqueProgramId;
        int availNum;
        boolean autoReturn;
        long breakDurationUs;
        long spliceEventId = sectionData.readUnsignedInt();
        boolean spliceEventCancelIndicator = (sectionData.readUnsignedByte() & 128) != 0;
        long programSplicePts2 = C.TIME_UNSET;
        List<ComponentSplice> componentSplices2 = Collections.emptyList();
        boolean autoReturn2 = false;
        long breakDurationUs2 = C.TIME_UNSET;
        if (spliceEventCancelIndicator) {
            spliceEventCancelIndicator = spliceEventCancelIndicator;
            spliceEventId = spliceEventId;
            durationFlag = false;
            outOfNetworkIndicator = false;
            programSpliceFlag = false;
            programSplicePts = -9223372036854775807L;
            availsExpected = 0;
            componentSplices = componentSplices2;
            uniqueProgramId = 0;
            availNum = 0;
            autoReturn = false;
            breakDurationUs = -9223372036854775807L;
        } else {
            int headerByte = sectionData.readUnsignedByte();
            boolean outOfNetworkIndicator2 = (headerByte & 128) != 0;
            boolean programSpliceFlag2 = (headerByte & 64) != 0;
            boolean durationFlag2 = (headerByte & 32) != 0;
            boolean spliceImmediateFlag = (headerByte & 16) != 0;
            if (programSpliceFlag2 && !spliceImmediateFlag) {
                programSplicePts2 = TimeSignalCommand.parseSpliceTime(sectionData, ptsAdjustment);
            }
            if (!programSpliceFlag2) {
                int componentCount = sectionData.readUnsignedByte();
                List<ComponentSplice> componentSplices3 = new ArrayList<>(componentCount);
                int i = 0;
                while (i < componentCount) {
                    int componentTag = sectionData.readUnsignedByte();
                    long componentSplicePts = C.TIME_UNSET;
                    if (!spliceImmediateFlag) {
                        componentSplicePts = TimeSignalCommand.parseSpliceTime(sectionData, ptsAdjustment);
                    }
                    int i2 = i;
                    long componentSplicePts2 = componentSplicePts;
                    componentSplices3.add(new ComponentSplice(componentTag, componentSplicePts2, timestampAdjuster.adjustTsTimestamp(componentSplicePts2)));
                    i = i2 + 1;
                    headerByte = headerByte;
                }
                componentSplices2 = componentSplices3;
            }
            if (durationFlag2) {
                long firstByte = sectionData.readUnsignedByte();
                boolean autoReturn3 = (128 & firstByte) != 0;
                long breakDuration90khz = ((firstByte & 1) << 32) | sectionData.readUnsignedInt();
                autoReturn2 = autoReturn3;
                breakDurationUs2 = (1000 * breakDuration90khz) / 90;
            }
            int uniqueProgramId2 = sectionData.readUnsignedShort();
            int availNum2 = sectionData.readUnsignedByte();
            int availsExpected2 = sectionData.readUnsignedByte();
            durationFlag = outOfNetworkIndicator2;
            outOfNetworkIndicator = programSpliceFlag2;
            programSpliceFlag = spliceImmediateFlag;
            programSplicePts = programSplicePts2;
            availsExpected = availsExpected2;
            componentSplices = componentSplices2;
            long j = breakDurationUs2;
            uniqueProgramId = uniqueProgramId2;
            availNum = availNum2;
            autoReturn = autoReturn2;
            breakDurationUs = j;
        }
        return new SpliceInsertCommand(spliceEventId, spliceEventCancelIndicator, durationFlag, outOfNetworkIndicator, programSpliceFlag, programSplicePts, timestampAdjuster.adjustTsTimestamp(programSplicePts), componentSplices, autoReturn, breakDurationUs, uniqueProgramId, availNum, availsExpected);
    }

    public static final class ComponentSplice {
        public final long componentSplicePlaybackPositionUs;
        public final long componentSplicePts;
        public final int componentTag;

        private ComponentSplice(int componentTag, long componentSplicePts, long componentSplicePlaybackPositionUs) {
            this.componentTag = componentTag;
            this.componentSplicePts = componentSplicePts;
            this.componentSplicePlaybackPositionUs = componentSplicePlaybackPositionUs;
        }

        public void writeToParcel(Parcel dest) {
            dest.writeInt(this.componentTag);
            dest.writeLong(this.componentSplicePts);
            dest.writeLong(this.componentSplicePlaybackPositionUs);
        }

        public static ComponentSplice createFromParcel(Parcel in) {
            return new ComponentSplice(in.readInt(), in.readLong(), in.readLong());
        }
    }

    @Override // androidx.media3.extractor.metadata.scte35.SpliceCommand
    public String toString() {
        return "SCTE-35 SpliceInsertCommand { programSplicePts=" + this.programSplicePts + ", programSplicePlaybackPositionUs= " + this.programSplicePlaybackPositionUs + " }";
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.spliceEventId);
        parcel.writeByte(this.spliceEventCancelIndicator ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.outOfNetworkIndicator ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.programSpliceFlag ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.spliceImmediateFlag ? (byte) 1 : (byte) 0);
        parcel.writeLong(this.programSplicePts);
        parcel.writeLong(this.programSplicePlaybackPositionUs);
        int size = this.componentSpliceList.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            this.componentSpliceList.get(i2).writeToParcel(parcel);
        }
        parcel.writeByte(this.autoReturn ? (byte) 1 : (byte) 0);
        parcel.writeLong(this.breakDurationUs);
        parcel.writeInt(this.uniqueProgramId);
        parcel.writeInt(this.availNum);
        parcel.writeInt(this.availsExpected);
    }
}
