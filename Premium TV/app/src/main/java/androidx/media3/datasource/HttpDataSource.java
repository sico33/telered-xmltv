package androidx.media3.datasource;

import android.text.TextUtils;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import com.google.common.base.Ascii;
import com.google.common.base.Predicate;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public interface HttpDataSource extends DataSource {
    public static final Predicate<String> REJECT_PAYWALL_TYPES = new Predicate() { // from class: androidx.media3.datasource.HttpDataSource$$ExternalSyntheticLambda0
        @Override // com.google.common.base.Predicate
        public final boolean apply(Object obj) {
            return HttpDataSource.CC.lambda$static$0((String) obj);
        }
    };

    void clearAllRequestProperties();

    void clearRequestProperty(String str);

    @Override // androidx.media3.datasource.DataSource
    void close() throws HttpDataSourceException;

    int getResponseCode();

    @Override // androidx.media3.datasource.DataSource
    Map<String, List<String>> getResponseHeaders();

    @Override // androidx.media3.datasource.DataSource
    long open(DataSpec dataSpec) throws HttpDataSourceException;

    @Override // androidx.media3.common.DataReader
    int read(byte[] bArr, int i, int i2) throws HttpDataSourceException;

    void setRequestProperty(String str, String str2);

    public interface Factory extends DataSource.Factory {
        @Override // androidx.media3.datasource.DataSource.Factory
        HttpDataSource createDataSource();

        Factory setDefaultRequestProperties(Map<String, String> map);

        /* JADX INFO: renamed from: androidx.media3.datasource.HttpDataSource$Factory$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
        }
    }

    public static final class RequestProperties {
        private final Map<String, String> requestProperties = new HashMap();
        private Map<String, String> requestPropertiesSnapshot;

        public synchronized void set(String name, String value) {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.put(name, value);
        }

        public synchronized void set(Map<String, String> properties) {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.putAll(properties);
        }

        public synchronized void clearAndSet(Map<String, String> properties) {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.clear();
            this.requestProperties.putAll(properties);
        }

        public synchronized void remove(String name) {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.remove(name);
        }

        public synchronized void clear() {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.clear();
        }

        public synchronized Map<String, String> getSnapshot() {
            if (this.requestPropertiesSnapshot == null) {
                this.requestPropertiesSnapshot = Collections.unmodifiableMap(new HashMap(this.requestProperties));
            }
            return this.requestPropertiesSnapshot;
        }
    }

    public static abstract class BaseFactory implements Factory {
        private final RequestProperties defaultRequestProperties = new RequestProperties();

        protected abstract HttpDataSource createDataSourceInternal(RequestProperties requestProperties);

        @Override // androidx.media3.datasource.DataSource.Factory
        public final HttpDataSource createDataSource() {
            return createDataSourceInternal(this.defaultRequestProperties);
        }

        @Override // androidx.media3.datasource.HttpDataSource.Factory
        public final Factory setDefaultRequestProperties(Map<String, String> defaultRequestProperties) {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties);
            return this;
        }
    }

    /* JADX INFO: renamed from: androidx.media3.datasource.HttpDataSource$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        static {
            Predicate<String> predicate = HttpDataSource.REJECT_PAYWALL_TYPES;
        }

        public static /* synthetic */ boolean lambda$static$0(String contentType) {
            if (contentType == null) {
                return false;
            }
            String contentType2 = Ascii.toLowerCase(contentType);
            if (TextUtils.isEmpty(contentType2)) {
                return false;
            }
            return ((contentType2.contains("text") && !contentType2.contains(MimeTypes.TEXT_VTT)) || contentType2.contains("html") || contentType2.contains("xml")) ? false : true;
        }
    }

    public static class HttpDataSourceException extends DataSourceException {
        public static final int TYPE_CLOSE = 3;
        public static final int TYPE_OPEN = 1;
        public static final int TYPE_READ = 2;
        public final DataSpec dataSpec;
        public final int type;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {
        }

        public static HttpDataSourceException createForIOException(IOException cause, DataSpec dataSpec, int type) {
            int errorCode;
            String message = cause.getMessage();
            if (cause instanceof SocketTimeoutException) {
                errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT;
            } else if (cause instanceof InterruptedIOException) {
                errorCode = 1004;
            } else if (message != null && Ascii.toLowerCase(message).matches("cleartext.*not permitted.*")) {
                errorCode = PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED;
            } else {
                errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED;
            }
            if (errorCode == 2007) {
                return new CleartextNotPermittedException(cause, dataSpec);
            }
            return new HttpDataSourceException(cause, dataSpec, errorCode, type);
        }

        @Deprecated
        public HttpDataSourceException(DataSpec dataSpec, int type) {
            this(dataSpec, 2000, type);
        }

        public HttpDataSourceException(DataSpec dataSpec, int errorCode, int type) {
            super(assignErrorCode(errorCode, type));
            this.dataSpec = dataSpec;
            this.type = type;
        }

        @Deprecated
        public HttpDataSourceException(String message, DataSpec dataSpec, int type) {
            this(message, dataSpec, 2000, type);
        }

        public HttpDataSourceException(String message, DataSpec dataSpec, int errorCode, int type) {
            super(message, assignErrorCode(errorCode, type));
            this.dataSpec = dataSpec;
            this.type = type;
        }

        @Deprecated
        public HttpDataSourceException(IOException cause, DataSpec dataSpec, int type) {
            this(cause, dataSpec, 2000, type);
        }

        public HttpDataSourceException(IOException cause, DataSpec dataSpec, int errorCode, int type) {
            super(cause, assignErrorCode(errorCode, type));
            this.dataSpec = dataSpec;
            this.type = type;
        }

        @Deprecated
        public HttpDataSourceException(String message, IOException cause, DataSpec dataSpec, int type) {
            this(message, cause, dataSpec, 2000, type);
        }

        public HttpDataSourceException(String message, IOException cause, DataSpec dataSpec, int errorCode, int type) {
            super(message, cause, assignErrorCode(errorCode, type));
            this.dataSpec = dataSpec;
            this.type = type;
        }

        private static int assignErrorCode(int errorCode, int type) {
            if (errorCode == 2000 && type == 1) {
                return PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED;
            }
            return errorCode;
        }
    }

    public static final class CleartextNotPermittedException extends HttpDataSourceException {
        public CleartextNotPermittedException(IOException cause, DataSpec dataSpec) {
            super("Cleartext HTTP traffic not permitted. See https://developer.android.com/guide/topics/media/issues/cleartext-not-permitted", cause, dataSpec, PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED, 1);
        }
    }

    public static final class InvalidContentTypeException extends HttpDataSourceException {
        public final String contentType;

        public InvalidContentTypeException(String contentType, DataSpec dataSpec) {
            super("Invalid content type: " + contentType, dataSpec, PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE, 1);
            this.contentType = contentType;
        }
    }

    public static final class InvalidResponseCodeException extends HttpDataSourceException {
        public final Map<String, List<String>> headerFields;
        public final byte[] responseBody;
        public final int responseCode;
        public final String responseMessage;

        public InvalidResponseCodeException(int responseCode, String responseMessage, IOException cause, Map<String, List<String>> headerFields, DataSpec dataSpec, byte[] responseBody) {
            super("Response code: " + responseCode, cause, dataSpec, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 1);
            this.responseCode = responseCode;
            this.responseMessage = responseMessage;
            this.headerFields = headerFields;
            this.responseBody = responseBody;
        }
    }
}
