package androidx.media3.container;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Util;
import com.google.common.primitives.Ints;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class MdtaMetadataEntry implements Metadata.Entry {
    public static final Parcelable.Creator<MdtaMetadataEntry> CREATOR = new Parcelable.Creator<MdtaMetadataEntry>() { // from class: androidx.media3.container.MdtaMetadataEntry.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MdtaMetadataEntry createFromParcel(Parcel in) {
            return new MdtaMetadataEntry(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MdtaMetadataEntry[] newArray(int size) {
            return new MdtaMetadataEntry[size];
        }
    };
    public static final int DEFAULT_LOCALE_INDICATOR = 0;
    public static final String KEY_ANDROID_CAPTURE_FPS = "com.android.capture.fps";
    public static final int TYPE_INDICATOR_FLOAT32 = 23;
    public static final int TYPE_INDICATOR_INT32 = 67;
    public static final int TYPE_INDICATOR_STRING = 1;
    public final String key;
    public final int localeIndicator;
    public final int typeIndicator;
    public final byte[] value;

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

    public MdtaMetadataEntry(String key, byte[] value, int typeIndicator) {
        this(key, value, 0, typeIndicator);
    }

    public MdtaMetadataEntry(String key, byte[] value, int localeIndicator, int typeIndicator) {
        this.key = key;
        this.value = value;
        this.localeIndicator = localeIndicator;
        this.typeIndicator = typeIndicator;
    }

    private MdtaMetadataEntry(Parcel in) {
        this.key = (String) Util.castNonNull(in.readString());
        this.value = (byte[]) Util.castNonNull(in.createByteArray());
        this.localeIndicator = in.readInt();
        this.typeIndicator = in.readInt();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MdtaMetadataEntry other = (MdtaMetadataEntry) obj;
        if (this.key.equals(other.key) && Arrays.equals(this.value, other.value) && this.localeIndicator == other.localeIndicator && this.typeIndicator == other.typeIndicator) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + this.key.hashCode();
        return (((((result * 31) + Arrays.hashCode(this.value)) * 31) + this.localeIndicator) * 31) + this.typeIndicator;
    }

    public String toString() {
        String formattedValue;
        switch (this.typeIndicator) {
            case 1:
                formattedValue = Util.fromUtf8Bytes(this.value);
                break;
            case 23:
                formattedValue = String.valueOf(Float.intBitsToFloat(Ints.fromByteArray(this.value)));
                break;
            case TYPE_INDICATOR_INT32 /* 67 */:
                formattedValue = String.valueOf(Ints.fromByteArray(this.value));
                break;
            default:
                formattedValue = Util.toHexString(this.value);
                break;
        }
        return "mdta: key=" + this.key + ", value=" + formattedValue;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.key);
        dest.writeByteArray(this.value);
        dest.writeInt(this.localeIndicator);
        dest.writeInt(this.typeIndicator);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
