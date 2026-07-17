package androidx.media3.extractor.metadata.scte35;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;

/* JADX INFO: loaded from: classes.dex */
public final class TimeSignalCommand extends SpliceCommand {
    public static final Parcelable.Creator<TimeSignalCommand> CREATOR = new Parcelable.Creator<TimeSignalCommand>() { // from class: androidx.media3.extractor.metadata.scte35.TimeSignalCommand.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeSignalCommand createFromParcel(Parcel in) {
            return new TimeSignalCommand(in.readLong(), in.readLong());
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeSignalCommand[] newArray(int size) {
            return new TimeSignalCommand[size];
        }
    };
    public final long playbackPositionUs;
    public final long ptsTime;

    private TimeSignalCommand(long ptsTime, long playbackPositionUs) {
        this.ptsTime = ptsTime;
        this.playbackPositionUs = playbackPositionUs;
    }

    static TimeSignalCommand parseFromSection(ParsableByteArray sectionData, long ptsAdjustment, TimestampAdjuster timestampAdjuster) {
        long ptsTime = parseSpliceTime(sectionData, ptsAdjustment);
        long playbackPositionUs = timestampAdjuster.adjustTsTimestamp(ptsTime);
        return new TimeSignalCommand(ptsTime, playbackPositionUs);
    }

    static long parseSpliceTime(ParsableByteArray sectionData, long ptsAdjustment) {
        long firstByte = sectionData.readUnsignedByte();
        if ((128 & firstByte) == 0) {
            return C.TIME_UNSET;
        }
        long ptsTime = ((1 & firstByte) << 32) | sectionData.readUnsignedInt();
        return (ptsTime + ptsAdjustment) & 8589934591L;
    }

    @Override // androidx.media3.extractor.metadata.scte35.SpliceCommand
    public String toString() {
        return "SCTE-35 TimeSignalCommand { ptsTime=" + this.ptsTime + ", playbackPositionUs= " + this.playbackPositionUs + " }";
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.ptsTime);
        dest.writeLong(this.playbackPositionUs);
    }
}
