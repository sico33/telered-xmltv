package androidx.media3.extractor.metadata.scte35;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class SpliceScheduleCommand extends SpliceCommand {
    public static final Parcelable.Creator<SpliceScheduleCommand> CREATOR = new Parcelable.Creator<SpliceScheduleCommand>() { // from class: androidx.media3.extractor.metadata.scte35.SpliceScheduleCommand.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SpliceScheduleCommand createFromParcel(Parcel in) {
            return new SpliceScheduleCommand(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SpliceScheduleCommand[] newArray(int size) {
            return new SpliceScheduleCommand[size];
        }
    };
    public final List<Event> events;

    public static final class Event {
        public final boolean autoReturn;
        public final int availNum;
        public final int availsExpected;
        public final long breakDurationUs;
        public final List<ComponentSplice> componentSpliceList;
        public final boolean outOfNetworkIndicator;
        public final boolean programSpliceFlag;
        public final boolean spliceEventCancelIndicator;
        public final long spliceEventId;
        public final int uniqueProgramId;
        public final long utcSpliceTime;

        private Event(long spliceEventId, boolean spliceEventCancelIndicator, boolean outOfNetworkIndicator, boolean programSpliceFlag, List<ComponentSplice> componentSpliceList, long utcSpliceTime, boolean autoReturn, long breakDurationUs, int uniqueProgramId, int availNum, int availsExpected) {
            this.spliceEventId = spliceEventId;
            this.spliceEventCancelIndicator = spliceEventCancelIndicator;
            this.outOfNetworkIndicator = outOfNetworkIndicator;
            this.programSpliceFlag = programSpliceFlag;
            this.componentSpliceList = Collections.unmodifiableList(componentSpliceList);
            this.utcSpliceTime = utcSpliceTime;
            this.autoReturn = autoReturn;
            this.breakDurationUs = breakDurationUs;
            this.uniqueProgramId = uniqueProgramId;
            this.availNum = availNum;
            this.availsExpected = availsExpected;
        }

        private Event(Parcel in) {
            this.spliceEventId = in.readLong();
            this.spliceEventCancelIndicator = in.readByte() == 1;
            this.outOfNetworkIndicator = in.readByte() == 1;
            this.programSpliceFlag = in.readByte() == 1;
            int componentSpliceListLength = in.readInt();
            ArrayList<ComponentSplice> componentSpliceList = new ArrayList<>(componentSpliceListLength);
            for (int i = 0; i < componentSpliceListLength; i++) {
                componentSpliceList.add(ComponentSplice.createFromParcel(in));
            }
            this.componentSpliceList = Collections.unmodifiableList(componentSpliceList);
            this.utcSpliceTime = in.readLong();
            this.autoReturn = in.readByte() == 1;
            this.breakDurationUs = in.readLong();
            this.uniqueProgramId = in.readInt();
            this.availNum = in.readInt();
            this.availsExpected = in.readInt();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static Event parseFromSection(ParsableByteArray sectionData) {
            boolean outOfNetworkIndicator;
            boolean outOfNetworkIndicator2;
            ArrayList<ComponentSplice> componentSplices;
            boolean autoReturn;
            int availNum;
            int availsExpected;
            long breakDurationUs;
            int availsExpected2;
            boolean durationFlag;
            long spliceEventId = sectionData.readUnsignedInt();
            boolean spliceEventCancelIndicator = (sectionData.readUnsignedByte() & 128) != 0;
            long utcSpliceTime = C.TIME_UNSET;
            ArrayList<ComponentSplice> componentSplices2 = new ArrayList<>();
            boolean autoReturn2 = false;
            long breakDurationUs2 = C.TIME_UNSET;
            if (spliceEventCancelIndicator) {
                spliceEventCancelIndicator = spliceEventCancelIndicator;
                spliceEventId = spliceEventId;
                outOfNetworkIndicator = false;
                outOfNetworkIndicator2 = false;
                componentSplices = componentSplices2;
                autoReturn = false;
                availNum = 0;
                availsExpected = 0;
                breakDurationUs = -9223372036854775807L;
                availsExpected2 = 0;
            } else {
                int headerByte = sectionData.readUnsignedByte();
                boolean outOfNetworkIndicator3 = (headerByte & 128) != 0;
                boolean programSpliceFlag = (headerByte & 64) != 0;
                boolean durationFlag2 = (headerByte & 32) != 0;
                if (programSpliceFlag) {
                    utcSpliceTime = sectionData.readUnsignedInt();
                }
                if (programSpliceFlag) {
                    durationFlag = durationFlag2;
                } else {
                    int componentCount = sectionData.readUnsignedByte();
                    ArrayList<ComponentSplice> componentSplices3 = new ArrayList<>(componentCount);
                    int i = 0;
                    while (i < componentCount) {
                        int componentTag = sectionData.readUnsignedByte();
                        int i2 = i;
                        long componentUtcSpliceTime = sectionData.readUnsignedInt();
                        componentSplices3.add(new ComponentSplice(componentTag, componentUtcSpliceTime));
                        i = i2 + 1;
                        headerByte = headerByte;
                        componentCount = componentCount;
                        durationFlag2 = durationFlag2;
                    }
                    durationFlag = durationFlag2;
                    componentSplices2 = componentSplices3;
                }
                if (durationFlag) {
                    long firstByte = sectionData.readUnsignedByte();
                    boolean autoReturn3 = (128 & firstByte) != 0;
                    long breakDuration90khz = ((firstByte & 1) << 32) | sectionData.readUnsignedInt();
                    autoReturn2 = autoReturn3;
                    breakDurationUs2 = (1000 * breakDuration90khz) / 90;
                }
                int uniqueProgramId = sectionData.readUnsignedShort();
                int availNum2 = sectionData.readUnsignedByte();
                int availsExpected3 = sectionData.readUnsignedByte();
                outOfNetworkIndicator = outOfNetworkIndicator3;
                outOfNetworkIndicator2 = programSpliceFlag;
                componentSplices = componentSplices2;
                autoReturn = autoReturn2;
                availNum = availNum2;
                availsExpected = uniqueProgramId;
                breakDurationUs = breakDurationUs2;
                availsExpected2 = availsExpected3;
            }
            return new Event(spliceEventId, spliceEventCancelIndicator, outOfNetworkIndicator, outOfNetworkIndicator2, componentSplices, utcSpliceTime, autoReturn, breakDurationUs, availsExpected, availNum, availsExpected2);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void writeToParcel(Parcel parcel) {
            parcel.writeLong(this.spliceEventId);
            parcel.writeByte(this.spliceEventCancelIndicator ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.outOfNetworkIndicator ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.programSpliceFlag ? (byte) 1 : (byte) 0);
            int size = this.componentSpliceList.size();
            parcel.writeInt(size);
            for (int i = 0; i < size; i++) {
                this.componentSpliceList.get(i).writeToParcel(parcel);
            }
            parcel.writeLong(this.utcSpliceTime);
            parcel.writeByte(this.autoReturn ? (byte) 1 : (byte) 0);
            parcel.writeLong(this.breakDurationUs);
            parcel.writeInt(this.uniqueProgramId);
            parcel.writeInt(this.availNum);
            parcel.writeInt(this.availsExpected);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static Event createFromParcel(Parcel in) {
            return new Event(in);
        }
    }

    public static final class ComponentSplice {
        public final int componentTag;
        public final long utcSpliceTime;

        private ComponentSplice(int componentTag, long utcSpliceTime) {
            this.componentTag = componentTag;
            this.utcSpliceTime = utcSpliceTime;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static ComponentSplice createFromParcel(Parcel in) {
            return new ComponentSplice(in.readInt(), in.readLong());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void writeToParcel(Parcel dest) {
            dest.writeInt(this.componentTag);
            dest.writeLong(this.utcSpliceTime);
        }
    }

    private SpliceScheduleCommand(List<Event> events) {
        this.events = Collections.unmodifiableList(events);
    }

    private SpliceScheduleCommand(Parcel in) {
        int eventsSize = in.readInt();
        ArrayList<Event> events = new ArrayList<>(eventsSize);
        for (int i = 0; i < eventsSize; i++) {
            events.add(Event.createFromParcel(in));
        }
        this.events = Collections.unmodifiableList(events);
    }

    static SpliceScheduleCommand parseFromSection(ParsableByteArray sectionData) {
        int spliceCount = sectionData.readUnsignedByte();
        ArrayList<Event> events = new ArrayList<>(spliceCount);
        for (int i = 0; i < spliceCount; i++) {
            events.add(Event.parseFromSection(sectionData));
        }
        return new SpliceScheduleCommand(events);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        int eventsSize = this.events.size();
        dest.writeInt(eventsSize);
        for (int i = 0; i < eventsSize; i++) {
            this.events.get(i).writeToParcel(dest);
        }
    }
}
