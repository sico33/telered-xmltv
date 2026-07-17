package androidx.media3.extractor.metadata.icy;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class IcyHeaders implements Metadata.Entry {
    public static final Parcelable.Creator<IcyHeaders> CREATOR = new Parcelable.Creator<IcyHeaders>() { // from class: androidx.media3.extractor.metadata.icy.IcyHeaders.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IcyHeaders createFromParcel(Parcel in) {
            return new IcyHeaders(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IcyHeaders[] newArray(int size) {
            return new IcyHeaders[size];
        }
    };
    public static final String REQUEST_HEADER_ENABLE_METADATA_NAME = "Icy-MetaData";
    public static final String REQUEST_HEADER_ENABLE_METADATA_VALUE = "1";
    private static final String RESPONSE_HEADER_BITRATE = "icy-br";
    private static final String RESPONSE_HEADER_GENRE = "icy-genre";
    private static final String RESPONSE_HEADER_METADATA_INTERVAL = "icy-metaint";
    private static final String RESPONSE_HEADER_NAME = "icy-name";
    private static final String RESPONSE_HEADER_PUB = "icy-pub";
    private static final String RESPONSE_HEADER_URL = "icy-url";
    private static final String TAG = "IcyHeaders";
    public final int bitrate;
    public final String genre;
    public final boolean isPublic;
    public final int metadataInterval;
    public final String name;
    public final String url;

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ byte[] getWrappedMetadataBytes() {
        return Metadata.Entry.CC.$default$getWrappedMetadataBytes(this);
    }

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ Format getWrappedMetadataFormat() {
        return Metadata.Entry.CC.$default$getWrappedMetadataFormat(this);
    }

    public static IcyHeaders parse(Map<String, List<String>> responseHeaders) {
        int bitrate;
        String genre;
        String name;
        String url;
        boolean isPublic;
        int metadataInterval;
        int metadataInterval2;
        boolean icyHeadersPresent = false;
        int bitrate2 = -1;
        int metadataInterval3 = -1;
        List<String> headers = responseHeaders.get(RESPONSE_HEADER_BITRATE);
        if (headers == null) {
            bitrate = -1;
        } else {
            String bitrateHeader = headers.get(0);
            try {
                bitrate2 = Integer.parseInt(bitrateHeader) * 1000;
                if (bitrate2 <= 0) {
                    Log.w(TAG, "Invalid bitrate: " + bitrateHeader);
                    bitrate2 = -1;
                } else {
                    icyHeadersPresent = true;
                }
                bitrate = bitrate2;
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid bitrate header: " + bitrateHeader);
                bitrate = bitrate2;
            }
        }
        List<String> headers2 = responseHeaders.get(RESPONSE_HEADER_GENRE);
        if (headers2 == null) {
            genre = null;
        } else {
            String genre2 = headers2.get(0);
            icyHeadersPresent = true;
            genre = genre2;
        }
        List<String> headers3 = responseHeaders.get(RESPONSE_HEADER_NAME);
        if (headers3 == null) {
            name = null;
        } else {
            String name2 = headers3.get(0);
            icyHeadersPresent = true;
            name = name2;
        }
        List<String> headers4 = responseHeaders.get(RESPONSE_HEADER_URL);
        if (headers4 == null) {
            url = null;
        } else {
            String url2 = headers4.get(0);
            icyHeadersPresent = true;
            url = url2;
        }
        List<String> headers5 = responseHeaders.get(RESPONSE_HEADER_PUB);
        if (headers5 == null) {
            isPublic = false;
        } else {
            boolean isPublic2 = headers5.get(0).equals(REQUEST_HEADER_ENABLE_METADATA_VALUE);
            icyHeadersPresent = true;
            isPublic = isPublic2;
        }
        List<String> headers6 = responseHeaders.get(RESPONSE_HEADER_METADATA_INTERVAL);
        if (headers6 == null) {
            metadataInterval = -1;
        } else {
            String metadataIntervalHeader = headers6.get(0);
            try {
                int metadataInterval4 = Integer.parseInt(metadataIntervalHeader);
                if (metadataInterval4 <= 0) {
                    try {
                        Log.w(TAG, "Invalid metadata interval: " + metadataIntervalHeader);
                        metadataInterval2 = -1;
                    } catch (NumberFormatException e2) {
                        metadataInterval3 = metadataInterval4;
                        Log.w(TAG, "Invalid metadata interval: " + metadataIntervalHeader);
                        metadataInterval = metadataInterval3;
                    }
                } else {
                    icyHeadersPresent = true;
                    metadataInterval2 = metadataInterval4;
                }
                metadataInterval = metadataInterval2;
            } catch (NumberFormatException e3) {
            }
        }
        if (icyHeadersPresent) {
            return new IcyHeaders(bitrate, genre, name, url, isPublic, metadataInterval);
        }
        return null;
    }

    public IcyHeaders(int bitrate, String genre, String name, String url, boolean isPublic, int metadataInterval) {
        Assertions.checkArgument(metadataInterval == -1 || metadataInterval > 0);
        this.bitrate = bitrate;
        this.genre = genre;
        this.name = name;
        this.url = url;
        this.isPublic = isPublic;
        this.metadataInterval = metadataInterval;
    }

    IcyHeaders(Parcel in) {
        this.bitrate = in.readInt();
        this.genre = in.readString();
        this.name = in.readString();
        this.url = in.readString();
        this.isPublic = Util.readBoolean(in);
        this.metadataInterval = in.readInt();
    }

    @Override // androidx.media3.common.Metadata.Entry
    public void populateMediaMetadata(MediaMetadata.Builder builder) {
        if (this.name != null) {
            builder.setStation(this.name);
        }
        if (this.genre != null) {
            builder.setGenre(this.genre);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IcyHeaders other = (IcyHeaders) obj;
        if (this.bitrate == other.bitrate && Util.areEqual(this.genre, other.genre) && Util.areEqual(this.name, other.name) && Util.areEqual(this.url, other.url) && this.isPublic == other.isPublic && this.metadataInterval == other.metadataInterval) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (((((((((((17 * 31) + this.bitrate) * 31) + (this.genre != null ? this.genre.hashCode() : 0)) * 31) + (this.name != null ? this.name.hashCode() : 0)) * 31) + (this.url != null ? this.url.hashCode() : 0)) * 31) + (this.isPublic ? 1 : 0)) * 31) + this.metadataInterval;
    }

    public String toString() {
        return "IcyHeaders: name=\"" + this.name + "\", genre=\"" + this.genre + "\", bitrate=" + this.bitrate + ", metadataInterval=" + this.metadataInterval;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.bitrate);
        dest.writeString(this.genre);
        dest.writeString(this.name);
        dest.writeString(this.url);
        Util.writeBoolean(dest, this.isPublic);
        dest.writeInt(this.metadataInterval);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
