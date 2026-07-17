package androidx.media3.exoplayer.offline;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.StreamKey;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class DownloadRequest implements Parcelable {
    public static final Parcelable.Creator<DownloadRequest> CREATOR = new Parcelable.Creator<DownloadRequest>() { // from class: androidx.media3.exoplayer.offline.DownloadRequest.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DownloadRequest createFromParcel(Parcel in) {
            return new DownloadRequest(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DownloadRequest[] newArray(int size) {
            return new DownloadRequest[size];
        }
    };
    public final String customCacheKey;
    public final byte[] data;
    public final String id;
    public final byte[] keySetId;
    public final String mimeType;
    public final List<StreamKey> streamKeys;
    public final Uri uri;

    public static class UnsupportedRequestException extends IOException {
    }

    public static class Builder {
        private String customCacheKey;
        private byte[] data;
        private final String id;
        private byte[] keySetId;
        private String mimeType;
        private List<StreamKey> streamKeys;
        private final Uri uri;

        public Builder(String id, Uri uri) {
            this.id = id;
            this.uri = uri;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = MimeTypes.normalizeMimeType(mimeType);
            return this;
        }

        public Builder setStreamKeys(List<StreamKey> streamKeys) {
            this.streamKeys = streamKeys;
            return this;
        }

        public Builder setKeySetId(byte[] keySetId) {
            this.keySetId = keySetId;
            return this;
        }

        public Builder setCustomCacheKey(String customCacheKey) {
            this.customCacheKey = customCacheKey;
            return this;
        }

        public Builder setData(byte[] data) {
            this.data = data;
            return this;
        }

        public DownloadRequest build() {
            return new DownloadRequest(this.id, this.uri, this.mimeType, this.streamKeys != null ? this.streamKeys : ImmutableList.of(), this.keySetId, this.customCacheKey, this.data);
        }
    }

    private DownloadRequest(String id, Uri uri, String mimeType, List<StreamKey> streamKeys, byte[] keySetId, String customCacheKey, byte[] data) {
        int contentType = Util.inferContentTypeForUriAndMimeType(uri, mimeType);
        if (contentType == 0 || contentType == 2 || contentType == 1) {
            Assertions.checkArgument(customCacheKey == null, "customCacheKey must be null for type: " + contentType);
        }
        this.id = id;
        this.uri = uri;
        this.mimeType = mimeType;
        ArrayList<StreamKey> mutableKeys = new ArrayList<>(streamKeys);
        Collections.sort(mutableKeys);
        this.streamKeys = Collections.unmodifiableList(mutableKeys);
        this.keySetId = keySetId != null ? Arrays.copyOf(keySetId, keySetId.length) : null;
        this.customCacheKey = customCacheKey;
        this.data = data != null ? Arrays.copyOf(data, data.length) : Util.EMPTY_BYTE_ARRAY;
    }

    DownloadRequest(Parcel in) {
        this.id = (String) Util.castNonNull(in.readString());
        this.uri = Uri.parse((String) Util.castNonNull(in.readString()));
        this.mimeType = in.readString();
        int streamKeyCount = in.readInt();
        ArrayList<StreamKey> mutableStreamKeys = new ArrayList<>(streamKeyCount);
        for (int i = 0; i < streamKeyCount; i++) {
            mutableStreamKeys.add((StreamKey) in.readParcelable(StreamKey.class.getClassLoader()));
        }
        this.streamKeys = Collections.unmodifiableList(mutableStreamKeys);
        this.keySetId = in.createByteArray();
        this.customCacheKey = in.readString();
        this.data = (byte[]) Util.castNonNull(in.createByteArray());
    }

    public DownloadRequest copyWithId(String id) {
        return new DownloadRequest(id, this.uri, this.mimeType, this.streamKeys, this.keySetId, this.customCacheKey, this.data);
    }

    public DownloadRequest copyWithKeySetId(byte[] keySetId) {
        return new DownloadRequest(this.id, this.uri, this.mimeType, this.streamKeys, keySetId, this.customCacheKey, this.data);
    }

    public DownloadRequest copyWithMergedRequest(DownloadRequest newRequest) {
        List<StreamKey> mergedKeys;
        Assertions.checkArgument(this.id.equals(newRequest.id));
        if (this.streamKeys.isEmpty() || newRequest.streamKeys.isEmpty()) {
            List<StreamKey> mergedKeys2 = Collections.emptyList();
            mergedKeys = mergedKeys2;
        } else {
            List<StreamKey> mergedKeys3 = new ArrayList<>(this.streamKeys);
            for (int i = 0; i < newRequest.streamKeys.size(); i++) {
                StreamKey newKey = newRequest.streamKeys.get(i);
                if (!mergedKeys3.contains(newKey)) {
                    mergedKeys3.add(newKey);
                }
            }
            mergedKeys = mergedKeys3;
        }
        return new DownloadRequest(this.id, newRequest.uri, newRequest.mimeType, mergedKeys, newRequest.keySetId, newRequest.customCacheKey, newRequest.data);
    }

    public MediaItem toMediaItem() {
        return new MediaItem.Builder().setMediaId(this.id).setUri(this.uri).setCustomCacheKey(this.customCacheKey).setMimeType(this.mimeType).setStreamKeys(this.streamKeys).build();
    }

    public String toString() {
        return this.mimeType + ":" + this.id;
    }

    public boolean equals(Object o) {
        if (!(o instanceof DownloadRequest)) {
            return false;
        }
        DownloadRequest that = (DownloadRequest) o;
        return this.id.equals(that.id) && this.uri.equals(that.uri) && Util.areEqual(this.mimeType, that.mimeType) && this.streamKeys.equals(that.streamKeys) && Arrays.equals(this.keySetId, that.keySetId) && Util.areEqual(this.customCacheKey, that.customCacheKey) && Arrays.equals(this.data, that.data);
    }

    public final int hashCode() {
        int result = this.id.hashCode() * 31;
        return (((((((((((result * 31) + this.uri.hashCode()) * 31) + (this.mimeType != null ? this.mimeType.hashCode() : 0)) * 31) + this.streamKeys.hashCode()) * 31) + Arrays.hashCode(this.keySetId)) * 31) + (this.customCacheKey != null ? this.customCacheKey.hashCode() : 0)) * 31) + Arrays.hashCode(this.data);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.uri.toString());
        dest.writeString(this.mimeType);
        dest.writeInt(this.streamKeys.size());
        for (int i = 0; i < this.streamKeys.size(); i++) {
            dest.writeParcelable(this.streamKeys.get(i), 0);
        }
        dest.writeByteArray(this.keySetId);
        dest.writeString(this.customCacheKey);
        dest.writeByteArray(this.data);
    }
}
