package androidx.media3.extractor.metadata.scte35;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes.dex */
public final class SpliceNullCommand extends SpliceCommand {
    public static final Parcelable.Creator<SpliceNullCommand> CREATOR = new Parcelable.Creator<SpliceNullCommand>() { // from class: androidx.media3.extractor.metadata.scte35.SpliceNullCommand.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SpliceNullCommand createFromParcel(Parcel in) {
            return new SpliceNullCommand();
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SpliceNullCommand[] newArray(int size) {
            return new SpliceNullCommand[size];
        }
    };

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
    }
}
