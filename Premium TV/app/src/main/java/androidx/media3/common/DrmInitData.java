package androidx.media3.common;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public final class DrmInitData implements Comparator<SchemeData>, Parcelable {
    public static final Parcelable.Creator<DrmInitData> CREATOR = new Parcelable.Creator<DrmInitData>() { // from class: androidx.media3.common.DrmInitData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DrmInitData createFromParcel(Parcel in) {
            return new DrmInitData(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DrmInitData[] newArray(int size) {
            return new DrmInitData[size];
        }
    };
    private int hashCode;
    public final int schemeDataCount;
    private final SchemeData[] schemeDatas;
    public final String schemeType;

    public static DrmInitData createSessionCreationData(DrmInitData manifestData, DrmInitData mediaData) {
        ArrayList<SchemeData> result = new ArrayList<>();
        String schemeType = null;
        if (manifestData != null) {
            schemeType = manifestData.schemeType;
            for (SchemeData data : manifestData.schemeDatas) {
                if (data.hasData()) {
                    result.add(data);
                }
            }
        }
        if (mediaData != null) {
            if (schemeType == null) {
                schemeType = mediaData.schemeType;
            }
            int manifestDatasCount = result.size();
            for (SchemeData data2 : mediaData.schemeDatas) {
                if (data2.hasData() && !containsSchemeDataWithUuid(result, manifestDatasCount, data2.uuid)) {
                    result.add(data2);
                }
            }
        }
        if (result.isEmpty()) {
            return null;
        }
        return new DrmInitData(schemeType, result);
    }

    public DrmInitData(List<SchemeData> schemeDatas) {
        this(null, false, (SchemeData[]) schemeDatas.toArray(new SchemeData[0]));
    }

    public DrmInitData(String schemeType, List<SchemeData> schemeDatas) {
        this(schemeType, false, (SchemeData[]) schemeDatas.toArray(new SchemeData[0]));
    }

    public DrmInitData(SchemeData... schemeDatas) {
        this((String) null, schemeDatas);
    }

    public DrmInitData(String schemeType, SchemeData... schemeDatas) {
        this(schemeType, true, schemeDatas);
    }

    private DrmInitData(String schemeType, boolean cloneSchemeDatas, SchemeData... schemeDatas) {
        this.schemeType = schemeType;
        schemeDatas = cloneSchemeDatas ? (SchemeData[]) schemeDatas.clone() : schemeDatas;
        this.schemeDatas = schemeDatas;
        this.schemeDataCount = schemeDatas.length;
        Arrays.sort(this.schemeDatas, this);
    }

    DrmInitData(Parcel in) {
        this.schemeType = in.readString();
        this.schemeDatas = (SchemeData[]) Util.castNonNull((SchemeData[]) in.createTypedArray(SchemeData.CREATOR));
        this.schemeDataCount = this.schemeDatas.length;
    }

    public SchemeData get(int index) {
        return this.schemeDatas[index];
    }

    public DrmInitData copyWithSchemeType(String schemeType) {
        if (Util.areEqual(this.schemeType, schemeType)) {
            return this;
        }
        return new DrmInitData(schemeType, false, this.schemeDatas);
    }

    public DrmInitData merge(DrmInitData drmInitData) {
        Assertions.checkState(this.schemeType == null || drmInitData.schemeType == null || TextUtils.equals(this.schemeType, drmInitData.schemeType));
        String mergedSchemeType = this.schemeType != null ? this.schemeType : drmInitData.schemeType;
        SchemeData[] mergedSchemeDatas = (SchemeData[]) Util.nullSafeArrayConcatenation(this.schemeDatas, drmInitData.schemeDatas);
        return new DrmInitData(mergedSchemeType, mergedSchemeDatas);
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            int result = this.schemeType == null ? 0 : this.schemeType.hashCode();
            this.hashCode = (result * 31) + Arrays.hashCode(this.schemeDatas);
        }
        return this.hashCode;
    }

    @Override // java.util.Comparator
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DrmInitData other = (DrmInitData) obj;
        if (Util.areEqual(this.schemeType, other.schemeType) && Arrays.equals(this.schemeDatas, other.schemeDatas)) {
            return true;
        }
        return false;
    }

    @Override // java.util.Comparator
    public int compare(SchemeData first, SchemeData second) {
        if (C.UUID_NIL.equals(first.uuid)) {
            return C.UUID_NIL.equals(second.uuid) ? 0 : 1;
        }
        return first.uuid.compareTo(second.uuid);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.schemeType);
        dest.writeTypedArray(this.schemeDatas, 0);
    }

    private static boolean containsSchemeDataWithUuid(ArrayList<SchemeData> datas, int limit, UUID uuid) {
        for (int i = 0; i < limit; i++) {
            if (datas.get(i).uuid.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public static final class SchemeData implements Parcelable {
        public static final Parcelable.Creator<SchemeData> CREATOR = new Parcelable.Creator<SchemeData>() { // from class: androidx.media3.common.DrmInitData.SchemeData.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SchemeData createFromParcel(Parcel in) {
                return new SchemeData(in);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SchemeData[] newArray(int size) {
                return new SchemeData[size];
            }
        };
        public final byte[] data;
        private int hashCode;
        public final String licenseServerUrl;
        public final String mimeType;
        public final UUID uuid;

        public SchemeData(UUID uuid, String mimeType, byte[] data) {
            this(uuid, null, mimeType, data);
        }

        public SchemeData(UUID uuid, String licenseServerUrl, String mimeType, byte[] data) {
            this.uuid = (UUID) Assertions.checkNotNull(uuid);
            this.licenseServerUrl = licenseServerUrl;
            this.mimeType = MimeTypes.normalizeMimeType((String) Assertions.checkNotNull(mimeType));
            this.data = data;
        }

        SchemeData(Parcel in) {
            this.uuid = new UUID(in.readLong(), in.readLong());
            this.licenseServerUrl = in.readString();
            this.mimeType = (String) Util.castNonNull(in.readString());
            this.data = in.createByteArray();
        }

        public boolean matches(UUID schemeUuid) {
            return C.UUID_NIL.equals(this.uuid) || schemeUuid.equals(this.uuid);
        }

        public boolean canReplace(SchemeData other) {
            return hasData() && !other.hasData() && matches(other.uuid);
        }

        public boolean hasData() {
            return this.data != null;
        }

        public SchemeData copyWithData(byte[] data) {
            return new SchemeData(this.uuid, this.licenseServerUrl, this.mimeType, data);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SchemeData)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            SchemeData other = (SchemeData) obj;
            return Util.areEqual(this.licenseServerUrl, other.licenseServerUrl) && Util.areEqual(this.mimeType, other.mimeType) && Util.areEqual(this.uuid, other.uuid) && Arrays.equals(this.data, other.data);
        }

        public int hashCode() {
            if (this.hashCode == 0) {
                int result = this.uuid.hashCode();
                this.hashCode = (((((result * 31) + (this.licenseServerUrl == null ? 0 : this.licenseServerUrl.hashCode())) * 31) + this.mimeType.hashCode()) * 31) + Arrays.hashCode(this.data);
            }
            return this.hashCode;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.uuid.getMostSignificantBits());
            dest.writeLong(this.uuid.getLeastSignificantBits());
            dest.writeString(this.licenseServerUrl);
            dest.writeString(this.mimeType);
            dest.writeByteArray(this.data);
        }
    }
}
