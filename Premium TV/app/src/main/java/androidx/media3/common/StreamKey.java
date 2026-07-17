package androidx.media3.common;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class StreamKey implements Comparable<StreamKey>, Parcelable {
    public final int groupIndex;
    public final int periodIndex;
    public final int streamIndex;
    public static final Parcelable.Creator<StreamKey> CREATOR = new Parcelable.Creator<StreamKey>() { // from class: androidx.media3.common.StreamKey.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public StreamKey createFromParcel(Parcel in) {
            return new StreamKey(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public StreamKey[] newArray(int size) {
            return new StreamKey[size];
        }
    };
    private static final String FIELD_PERIOD_INDEX = Util.intToStringMaxRadix(0);
    private static final String FIELD_GROUP_INDEX = Util.intToStringMaxRadix(1);
    private static final String FIELD_STREAM_INDEX = Util.intToStringMaxRadix(2);

    public StreamKey(int groupIndex, int streamIndex) {
        this(0, groupIndex, streamIndex);
    }

    public StreamKey(int periodIndex, int groupIndex, int streamIndex) {
        this.periodIndex = periodIndex;
        this.groupIndex = groupIndex;
        this.streamIndex = streamIndex;
    }

    StreamKey(Parcel in) {
        this.periodIndex = in.readInt();
        this.groupIndex = in.readInt();
        this.streamIndex = in.readInt();
    }

    public String toString() {
        return this.periodIndex + "." + this.groupIndex + "." + this.streamIndex;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StreamKey that = (StreamKey) o;
        if (this.periodIndex == that.periodIndex && this.groupIndex == that.groupIndex && this.streamIndex == that.streamIndex) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = this.periodIndex;
        return (((result * 31) + this.groupIndex) * 31) + this.streamIndex;
    }

    @Override // java.lang.Comparable
    public int compareTo(StreamKey o) {
        int result = this.periodIndex - o.periodIndex;
        if (result == 0) {
            int result2 = this.groupIndex - o.groupIndex;
            if (result2 == 0) {
                return this.streamIndex - o.streamIndex;
            }
            return result2;
        }
        return result;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.periodIndex);
        dest.writeInt(this.groupIndex);
        dest.writeInt(this.streamIndex);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (this.periodIndex != 0) {
            bundle.putInt(FIELD_PERIOD_INDEX, this.periodIndex);
        }
        if (this.groupIndex != 0) {
            bundle.putInt(FIELD_GROUP_INDEX, this.groupIndex);
        }
        if (this.streamIndex != 0) {
            bundle.putInt(FIELD_STREAM_INDEX, this.streamIndex);
        }
        return bundle;
    }

    public static StreamKey fromBundle(Bundle bundle) {
        return new StreamKey(bundle.getInt(FIELD_PERIOD_INDEX, 0), bundle.getInt(FIELD_GROUP_INDEX, 0), bundle.getInt(FIELD_STREAM_INDEX, 0));
    }
}
