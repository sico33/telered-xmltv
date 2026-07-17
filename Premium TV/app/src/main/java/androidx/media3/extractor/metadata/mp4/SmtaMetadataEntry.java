package androidx.media3.extractor.metadata.mp4;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import com.google.common.primitives.Floats;

/* JADX INFO: loaded from: classes.dex */
public final class SmtaMetadataEntry implements Metadata.Entry {
    public static final Parcelable.Creator<SmtaMetadataEntry> CREATOR = new Parcelable.Creator<SmtaMetadataEntry>() { // from class: androidx.media3.extractor.metadata.mp4.SmtaMetadataEntry.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SmtaMetadataEntry createFromParcel(Parcel in) {
            return new SmtaMetadataEntry(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SmtaMetadataEntry[] newArray(int size) {
            return new SmtaMetadataEntry[size];
        }
    };
    public final float captureFrameRate;
    public final int svcTemporalLayerCount;

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ byte[] getWrappedMetadataBytes() {
        return Metadata.Entry.CC.$default$getWrappedMetadataBytes(this);
    }

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ Format getWrappedMetadataFormat() {
        return Metadata.Entry.CC.$default$getWrappedMetadataFormat(this);
    }

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ void populateMediaMetadata(MediaMetadata.Builder builder) {
        Metadata.Entry.CC.$default$populateMediaMetadata(this, builder);
    }

    public SmtaMetadataEntry(float captureFrameRate, int svcTemporalLayerCount) {
        this.captureFrameRate = captureFrameRate;
        this.svcTemporalLayerCount = svcTemporalLayerCount;
    }

    private SmtaMetadataEntry(Parcel in) {
        this.captureFrameRate = in.readFloat();
        this.svcTemporalLayerCount = in.readInt();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SmtaMetadataEntry other = (SmtaMetadataEntry) obj;
        if (this.captureFrameRate == other.captureFrameRate && this.svcTemporalLayerCount == other.svcTemporalLayerCount) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + Floats.hashCode(this.captureFrameRate);
        return (result * 31) + this.svcTemporalLayerCount;
    }

    public String toString() {
        return "smta: captureFrameRate=" + this.captureFrameRate + ", svcTemporalLayerCount=" + this.svcTemporalLayerCount;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.captureFrameRate);
        dest.writeInt(this.svcTemporalLayerCount);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
