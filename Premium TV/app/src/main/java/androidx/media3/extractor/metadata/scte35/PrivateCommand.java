package androidx.media3.extractor.metadata.scte35;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class PrivateCommand extends SpliceCommand {
    public static final Parcelable.Creator<PrivateCommand> CREATOR = new Parcelable.Creator<PrivateCommand>() { // from class: androidx.media3.extractor.metadata.scte35.PrivateCommand.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PrivateCommand createFromParcel(Parcel in) {
            return new PrivateCommand(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PrivateCommand[] newArray(int size) {
            return new PrivateCommand[size];
        }
    };
    public final byte[] commandBytes;
    public final long identifier;
    public final long ptsAdjustment;

    private PrivateCommand(long identifier, byte[] commandBytes, long ptsAdjustment) {
        this.ptsAdjustment = ptsAdjustment;
        this.identifier = identifier;
        this.commandBytes = commandBytes;
    }

    private PrivateCommand(Parcel in) {
        this.ptsAdjustment = in.readLong();
        this.identifier = in.readLong();
        this.commandBytes = (byte[]) Util.castNonNull(in.createByteArray());
    }

    static PrivateCommand parseFromSection(ParsableByteArray sectionData, int commandLength, long ptsAdjustment) {
        long identifier = sectionData.readUnsignedInt();
        byte[] privateBytes = new byte[commandLength - 4];
        sectionData.readBytes(privateBytes, 0, privateBytes.length);
        return new PrivateCommand(identifier, privateBytes, ptsAdjustment);
    }

    @Override // androidx.media3.extractor.metadata.scte35.SpliceCommand
    public String toString() {
        return "SCTE-35 PrivateCommand { ptsAdjustment=" + this.ptsAdjustment + ", identifier= " + this.identifier + " }";
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.ptsAdjustment);
        dest.writeLong(this.identifier);
        dest.writeByteArray(this.commandBytes);
    }
}
