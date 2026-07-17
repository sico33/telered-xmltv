package androidx.media3.datasource;

import android.net.Uri;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.util.Assertions;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class DataSpec {
    public static final int FLAG_ALLOW_CACHE_FRAGMENTATION = 4;
    public static final int FLAG_ALLOW_GZIP = 1;
    public static final int FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN = 2;
    public static final int FLAG_MIGHT_NOT_USE_FULL_NETWORK_SPEED = 8;
    public static final int HTTP_METHOD_GET = 1;
    public static final int HTTP_METHOD_HEAD = 3;
    public static final int HTTP_METHOD_POST = 2;

    @Deprecated
    public final long absoluteStreamPosition;
    public final Object customData;
    public final int flags;
    public final byte[] httpBody;
    public final int httpMethod;
    public final Map<String, String> httpRequestHeaders;
    public final String key;
    public final long length;
    public final long position;
    public final Uri uri;
    public final long uriPositionOffset;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface HttpMethod {
    }

    static {
        MediaLibraryInfo.registerModule("media3.datasource");
    }

    public static final class Builder {
        private Object customData;
        private int flags;
        private byte[] httpBody;
        private int httpMethod;
        private Map<String, String> httpRequestHeaders;
        private String key;
        private long length;
        private long position;
        private Uri uri;
        private long uriPositionOffset;

        public Builder() {
            this.httpMethod = 1;
            this.httpRequestHeaders = Collections.emptyMap();
            this.length = -1L;
        }

        private Builder(DataSpec dataSpec) {
            this.uri = dataSpec.uri;
            this.uriPositionOffset = dataSpec.uriPositionOffset;
            this.httpMethod = dataSpec.httpMethod;
            this.httpBody = dataSpec.httpBody;
            this.httpRequestHeaders = dataSpec.httpRequestHeaders;
            this.position = dataSpec.position;
            this.length = dataSpec.length;
            this.key = dataSpec.key;
            this.flags = dataSpec.flags;
            this.customData = dataSpec.customData;
        }

        public Builder setUri(String uriString) {
            this.uri = Uri.parse(uriString);
            return this;
        }

        public Builder setUri(Uri uri) {
            this.uri = uri;
            return this;
        }

        public Builder setUriPositionOffset(long uriPositionOffset) {
            this.uriPositionOffset = uriPositionOffset;
            return this;
        }

        public Builder setHttpMethod(int httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder setHttpBody(byte[] httpBody) {
            this.httpBody = httpBody;
            return this;
        }

        public Builder setHttpRequestHeaders(Map<String, String> httpRequestHeaders) {
            this.httpRequestHeaders = httpRequestHeaders;
            return this;
        }

        public Builder setPosition(long position) {
            this.position = position;
            return this;
        }

        public Builder setLength(long length) {
            this.length = length;
            return this;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public Builder setCustomData(Object customData) {
            this.customData = customData;
            return this;
        }

        public DataSpec build() {
            Assertions.checkStateNotNull(this.uri, "The uri must be set.");
            return new DataSpec(this.uri, this.uriPositionOffset, this.httpMethod, this.httpBody, this.httpRequestHeaders, this.position, this.length, this.key, this.flags, this.customData);
        }
    }

    public static String getStringForHttpMethod(int httpMethod) {
        switch (httpMethod) {
            case 1:
                return "GET";
            case 2:
                return "POST";
            case 3:
                return "HEAD";
            default:
                throw new IllegalStateException();
        }
    }

    public DataSpec(Uri uri) {
        this(uri, 0L, -1L);
    }

    public DataSpec(Uri uri, long position, long length) {
        this(uri, position, length, null);
    }

    @Deprecated
    public DataSpec(Uri uri, long position, long length, String key) {
        this(uri, 0L, 1, null, Collections.emptyMap(), position, length, key, 0, null);
    }

    private DataSpec(Uri uri, long uriPositionOffset, int httpMethod, byte[] httpBody, Map<String, String> httpRequestHeaders, long position, long length, String key, int flags, Object customData) {
        boolean z = true;
        Assertions.checkArgument(uriPositionOffset + position >= 0);
        Assertions.checkArgument(position >= 0);
        if (length <= 0 && length != -1) {
            z = false;
        }
        Assertions.checkArgument(z);
        this.uri = (Uri) Assertions.checkNotNull(uri);
        this.uriPositionOffset = uriPositionOffset;
        this.httpMethod = httpMethod;
        this.httpBody = (httpBody == null || httpBody.length == 0) ? null : httpBody;
        this.httpRequestHeaders = Collections.unmodifiableMap(new HashMap(httpRequestHeaders));
        this.position = position;
        this.absoluteStreamPosition = uriPositionOffset + position;
        this.length = length;
        this.key = key;
        this.flags = flags;
        this.customData = customData;
    }

    public boolean isFlagSet(int flag) {
        return (this.flags & flag) == flag;
    }

    public final String getHttpMethodString() {
        return getStringForHttpMethod(this.httpMethod);
    }

    public Builder buildUpon() {
        return new Builder();
    }

    public DataSpec subrange(long offset) {
        return subrange(offset, this.length != -1 ? this.length - offset : -1L);
    }

    public DataSpec subrange(long offset, long length) {
        if (offset == 0 && this.length == length) {
            return this;
        }
        return new DataSpec(this.uri, this.uriPositionOffset, this.httpMethod, this.httpBody, this.httpRequestHeaders, this.position + offset, length, this.key, this.flags, this.customData);
    }

    public DataSpec withUri(Uri uri) {
        return new DataSpec(uri, this.uriPositionOffset, this.httpMethod, this.httpBody, this.httpRequestHeaders, this.position, this.length, this.key, this.flags, this.customData);
    }

    public DataSpec withRequestHeaders(Map<String, String> httpRequestHeaders) {
        return new DataSpec(this.uri, this.uriPositionOffset, this.httpMethod, this.httpBody, httpRequestHeaders, this.position, this.length, this.key, this.flags, this.customData);
    }

    public DataSpec withAdditionalHeaders(Map<String, String> additionalHttpRequestHeaders) {
        Map<String, String> httpRequestHeaders = new HashMap<>(this.httpRequestHeaders);
        httpRequestHeaders.putAll(additionalHttpRequestHeaders);
        return new DataSpec(this.uri, this.uriPositionOffset, this.httpMethod, this.httpBody, httpRequestHeaders, this.position, this.length, this.key, this.flags, this.customData);
    }

    public String toString() {
        return "DataSpec[" + getHttpMethodString() + " " + this.uri + ", " + this.position + ", " + this.length + ", " + this.key + ", " + this.flags + "]";
    }
}
