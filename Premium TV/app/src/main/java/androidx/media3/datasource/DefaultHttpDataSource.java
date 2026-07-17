package androidx.media3.datasource;

import android.net.Uri;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import com.google.common.base.Predicate;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/* JADX INFO: loaded from: classes.dex */
public class DefaultHttpDataSource extends BaseDataSource implements HttpDataSource {
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 8000;
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 8000;
    private static final int HTTP_STATUS_PERMANENT_REDIRECT = 308;
    private static final int HTTP_STATUS_TEMPORARY_REDIRECT = 307;
    private static final long MAX_BYTES_TO_DRAIN = 2048;
    private static final int MAX_REDIRECTS = 20;
    private static final String TAG = "DefaultHttpDataSource";
    private final boolean allowCrossProtocolRedirects;
    private long bytesRead;
    private long bytesToRead;
    private final int connectTimeoutMillis;
    private HttpURLConnection connection;
    private final Predicate<String> contentTypePredicate;
    private final boolean crossProtocolRedirectsForceOriginal;
    private DataSpec dataSpec;
    private final HttpDataSource.RequestProperties defaultRequestProperties;
    private InputStream inputStream;
    private final boolean keepPostFor302Redirects;
    private boolean opened;
    private final int readTimeoutMillis;
    private final HttpDataSource.RequestProperties requestProperties;
    private int responseCode;
    private final String userAgent;

    public static final class Factory implements HttpDataSource.Factory {
        private boolean allowCrossProtocolRedirects;
        private Predicate<String> contentTypePredicate;
        private boolean crossProtocolRedirectsForceOriginal;
        private boolean keepPostFor302Redirects;
        private TransferListener transferListener;
        private String userAgent;
        private final HttpDataSource.RequestProperties defaultRequestProperties = new HttpDataSource.RequestProperties();
        private int connectTimeoutMs = 8000;
        private int readTimeoutMs = 8000;

        @Override // androidx.media3.datasource.HttpDataSource.Factory
        public /* bridge */ /* synthetic */ HttpDataSource.Factory setDefaultRequestProperties(Map map) {
            return setDefaultRequestProperties((Map<String, String>) map);
        }

        @Override // androidx.media3.datasource.HttpDataSource.Factory
        public Factory setDefaultRequestProperties(Map<String, String> defaultRequestProperties) {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties);
            return this;
        }

        public Factory setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Factory setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Factory setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        public Factory setAllowCrossProtocolRedirects(boolean allowCrossProtocolRedirects) {
            this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
            return this;
        }

        public Factory setCrossProtocolRedirectsForceOriginal(boolean crossProtocolRedirectsForceOriginal) {
            this.crossProtocolRedirectsForceOriginal = crossProtocolRedirectsForceOriginal;
            return this;
        }

        public Factory setContentTypePredicate(Predicate<String> contentTypePredicate) {
            this.contentTypePredicate = contentTypePredicate;
            return this;
        }

        public Factory setTransferListener(TransferListener transferListener) {
            this.transferListener = transferListener;
            return this;
        }

        public Factory setKeepPostFor302Redirects(boolean keepPostFor302Redirects) {
            this.keepPostFor302Redirects = keepPostFor302Redirects;
            return this;
        }

        @Override // androidx.media3.datasource.HttpDataSource.Factory, androidx.media3.datasource.DataSource.Factory
        public DefaultHttpDataSource createDataSource() {
            DefaultHttpDataSource dataSource = new DefaultHttpDataSource(this.userAgent, this.connectTimeoutMs, this.readTimeoutMs, this.allowCrossProtocolRedirects, this.crossProtocolRedirectsForceOriginal, this.defaultRequestProperties, this.contentTypePredicate, this.keepPostFor302Redirects);
            if (this.transferListener != null) {
                dataSource.addTransferListener(this.transferListener);
            }
            return dataSource;
        }
    }

    private DefaultHttpDataSource(String userAgent, int connectTimeoutMillis, int readTimeoutMillis, boolean allowCrossProtocolRedirects, boolean crossProtocolRedirectsForceOriginal, HttpDataSource.RequestProperties defaultRequestProperties, Predicate<String> contentTypePredicate, boolean keepPostFor302Redirects) {
        super(true);
        this.userAgent = userAgent;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
        this.crossProtocolRedirectsForceOriginal = crossProtocolRedirectsForceOriginal;
        if (allowCrossProtocolRedirects && crossProtocolRedirectsForceOriginal) {
            throw new IllegalArgumentException("crossProtocolRedirectsForceOriginal should not be set if allowCrossProtocolRedirects is true");
        }
        this.defaultRequestProperties = defaultRequestProperties;
        this.contentTypePredicate = contentTypePredicate;
        this.requestProperties = new HttpDataSource.RequestProperties();
        this.keepPostFor302Redirects = keepPostFor302Redirects;
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        if (this.connection == null) {
            return null;
        }
        return Uri.parse(this.connection.getURL().toString());
    }

    @Override // androidx.media3.datasource.HttpDataSource
    public int getResponseCode() {
        if (this.connection == null || this.responseCode <= 0) {
            return -1;
        }
        return this.responseCode;
    }

    @Override // androidx.media3.datasource.BaseDataSource, androidx.media3.datasource.DataSource
    public Map<String, List<String>> getResponseHeaders() {
        if (this.connection == null) {
            return ImmutableMap.of();
        }
        return new NullFilteringHeadersMap(this.connection.getHeaderFields());
    }

    @Override // androidx.media3.datasource.HttpDataSource
    public void setRequestProperty(String name, String value) {
        Assertions.checkNotNull(name);
        Assertions.checkNotNull(value);
        this.requestProperties.set(name, value);
    }

    @Override // androidx.media3.datasource.HttpDataSource
    public void clearRequestProperty(String name) {
        Assertions.checkNotNull(name);
        this.requestProperties.remove(name);
    }

    @Override // androidx.media3.datasource.HttpDataSource
    public void clearAllRequestProperties() {
        this.requestProperties.clear();
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws HttpDataSource.HttpDataSourceException {
        byte[] errorResponseBody;
        IOException cause;
        this.dataSpec = dataSpec;
        long j = 0;
        this.bytesRead = 0L;
        this.bytesToRead = 0L;
        transferInitializing(dataSpec);
        try {
            this.connection = makeConnection(dataSpec);
            HttpURLConnection connection = this.connection;
            this.responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            if (this.responseCode < 200 || this.responseCode > 299) {
                Map<String, List<String>> headers = connection.getHeaderFields();
                if (this.responseCode == 416) {
                    long documentSize = HttpUtil.getDocumentSize(connection.getHeaderField(HttpHeaders.CONTENT_RANGE));
                    if (dataSpec.position == documentSize) {
                        this.opened = true;
                        transferStarted(dataSpec);
                        if (dataSpec.length != -1) {
                            return dataSpec.length;
                        }
                        return 0L;
                    }
                }
                InputStream errorStream = connection.getErrorStream();
                try {
                    byte[] errorResponseBody2 = errorStream != null ? ByteStreams.toByteArray(errorStream) : Util.EMPTY_BYTE_ARRAY;
                    errorResponseBody = errorResponseBody2;
                } catch (IOException e) {
                    errorResponseBody = Util.EMPTY_BYTE_ARRAY;
                }
                closeConnectionQuietly();
                if (this.responseCode == 416) {
                    cause = new DataSourceException(2008);
                } else {
                    cause = null;
                }
                throw new HttpDataSource.InvalidResponseCodeException(this.responseCode, responseMessage, cause, headers, dataSpec, errorResponseBody);
            }
            String contentType = connection.getContentType();
            if (this.contentTypePredicate != null && !this.contentTypePredicate.apply(contentType)) {
                closeConnectionQuietly();
                throw new HttpDataSource.InvalidContentTypeException(contentType, dataSpec);
            }
            if (this.responseCode == 200 && dataSpec.position != 0) {
                j = dataSpec.position;
            }
            long bytesToSkip = j;
            boolean isCompressed = isCompressed(connection);
            if (isCompressed || dataSpec.length != -1) {
                this.bytesToRead = dataSpec.length;
            } else {
                long contentLength = HttpUtil.getContentLength(connection.getHeaderField(HttpHeaders.CONTENT_LENGTH), connection.getHeaderField(HttpHeaders.CONTENT_RANGE));
                this.bytesToRead = contentLength != -1 ? contentLength - bytesToSkip : -1L;
            }
            try {
                this.inputStream = connection.getInputStream();
                if (isCompressed) {
                    this.inputStream = new GZIPInputStream(this.inputStream);
                }
                this.opened = true;
                transferStarted(dataSpec);
                try {
                    skipFully(bytesToSkip, dataSpec);
                    return this.bytesToRead;
                } catch (IOException e2) {
                    closeConnectionQuietly();
                    if (e2 instanceof HttpDataSource.HttpDataSourceException) {
                        throw ((HttpDataSource.HttpDataSourceException) e2);
                    }
                    throw new HttpDataSource.HttpDataSourceException(e2, dataSpec, 2000, 1);
                }
            } catch (IOException e3) {
                closeConnectionQuietly();
                throw new HttpDataSource.HttpDataSourceException(e3, dataSpec, 2000, 1);
            }
        } catch (IOException e4) {
            closeConnectionQuietly();
            throw HttpDataSource.HttpDataSourceException.createForIOException(e4, dataSpec, 1);
        }
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws HttpDataSource.HttpDataSourceException {
        try {
            return readInternal(buffer, offset, length);
        } catch (IOException e) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(e, (DataSpec) Util.castNonNull(this.dataSpec), 2);
        }
    }

    @Override // androidx.media3.datasource.DataSource
    public void close() throws HttpDataSource.HttpDataSourceException {
        try {
            InputStream inputStream = this.inputStream;
            if (inputStream != null) {
                long bytesRemaining = -1;
                if (this.bytesToRead != -1) {
                    bytesRemaining = this.bytesToRead - this.bytesRead;
                }
                maybeTerminateInputStream(this.connection, bytesRemaining);
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new HttpDataSource.HttpDataSourceException(e, (DataSpec) Util.castNonNull(this.dataSpec), 2000, 3);
                }
            }
            this.inputStream = null;
            closeConnectionQuietly();
            if (this.opened) {
                this.opened = false;
                transferEnded();
            }
        } catch (Throwable th) {
            this.inputStream = null;
            closeConnectionQuietly();
            if (this.opened) {
                this.opened = false;
                transferEnded();
            }
            throw th;
        }
    }

    private HttpURLConnection makeConnection(DataSpec dataSpec) throws IOException {
        URL url = new URL(dataSpec.uri.toString());
        int httpMethod = dataSpec.httpMethod;
        byte[] httpBody = dataSpec.httpBody;
        long position = dataSpec.position;
        long length = dataSpec.length;
        int i = 1;
        boolean allowGzip = dataSpec.isFlagSet(1);
        if (!this.allowCrossProtocolRedirects && !this.crossProtocolRedirectsForceOriginal && !this.keepPostFor302Redirects) {
            return makeConnection(url, httpMethod, httpBody, position, length, allowGzip, true, dataSpec.httpRequestHeaders);
        }
        int redirectCount = 0;
        while (true) {
            int redirectCount2 = redirectCount + 1;
            if (redirectCount > 20) {
                throw new HttpDataSource.HttpDataSourceException(new NoRouteToHostException("Too many redirects: " + redirectCount2), dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, 1);
            }
            HttpURLConnection connection = makeConnection(url, httpMethod, httpBody, position, length, allowGzip, false, dataSpec.httpRequestHeaders);
            int responseCode = connection.getResponseCode();
            String location = connection.getHeaderField(HttpHeaders.LOCATION);
            if ((httpMethod == i || httpMethod == 3) && (responseCode == 300 || responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == HTTP_STATUS_TEMPORARY_REDIRECT || responseCode == HTTP_STATUS_PERMANENT_REDIRECT)) {
                connection.disconnect();
                url = handleRedirect(url, location, dataSpec);
            } else if (httpMethod == 2 && (responseCode == 300 || responseCode == 301 || responseCode == 302 || responseCode == 303)) {
                connection.disconnect();
                boolean shouldKeepPost = this.keepPostFor302Redirects && responseCode == 302;
                if (!shouldKeepPost) {
                    httpMethod = 1;
                    httpBody = null;
                }
                url = handleRedirect(url, location, dataSpec);
            } else {
                return connection;
            }
            redirectCount = redirectCount2;
            i = 1;
        }
    }

    private HttpURLConnection makeConnection(URL url, int httpMethod, byte[] httpBody, long position, long length, boolean allowGzip, boolean followRedirects, Map<String, String> requestParameters) throws IOException {
        HttpURLConnection connection = openConnection(url);
        connection.setConnectTimeout(this.connectTimeoutMillis);
        connection.setReadTimeout(this.readTimeoutMillis);
        Map<String, String> requestHeaders = new HashMap<>();
        if (this.defaultRequestProperties != null) {
            requestHeaders.putAll(this.defaultRequestProperties.getSnapshot());
        }
        requestHeaders.putAll(this.requestProperties.getSnapshot());
        requestHeaders.putAll(requestParameters);
        for (Map.Entry<String, String> property : requestHeaders.entrySet()) {
            connection.setRequestProperty(property.getKey(), property.getValue());
        }
        String rangeHeader = HttpUtil.buildRangeRequestHeader(position, length);
        if (rangeHeader != null) {
            connection.setRequestProperty(HttpHeaders.RANGE, rangeHeader);
        }
        if (this.userAgent != null) {
            connection.setRequestProperty(HttpHeaders.USER_AGENT, this.userAgent);
        }
        connection.setRequestProperty(HttpHeaders.ACCEPT_ENCODING, allowGzip ? "gzip" : "identity");
        connection.setInstanceFollowRedirects(followRedirects);
        connection.setDoOutput(httpBody != null);
        connection.setRequestMethod(DataSpec.getStringForHttpMethod(httpMethod));
        if (httpBody != null) {
            connection.setFixedLengthStreamingMode(httpBody.length);
            connection.connect();
            OutputStream os = connection.getOutputStream();
            os.write(httpBody);
            os.close();
        } else {
            connection.connect();
        }
        return connection;
    }

    HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private URL handleRedirect(URL originalUrl, String location, DataSpec dataSpec) throws HttpDataSource.HttpDataSourceException {
        if (location == null) {
            throw new HttpDataSource.HttpDataSourceException("Null location redirect", dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, 1);
        }
        try {
            URL url = new URL(originalUrl, location);
            String protocol = url.getProtocol();
            if (!"https".equals(protocol) && !"http".equals(protocol)) {
                throw new HttpDataSource.HttpDataSourceException("Unsupported protocol redirect: " + protocol, dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, 1);
            }
            if (!this.allowCrossProtocolRedirects && !protocol.equals(originalUrl.getProtocol())) {
                if (!this.crossProtocolRedirectsForceOriginal) {
                    throw new HttpDataSource.HttpDataSourceException("Disallowed cross-protocol redirect (" + originalUrl.getProtocol() + " to " + protocol + ")", dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, 1);
                }
                try {
                    return new URL(url.toString().replaceFirst(protocol, originalUrl.getProtocol()));
                } catch (MalformedURLException e) {
                    throw new HttpDataSource.HttpDataSourceException(e, dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, 1);
                }
            }
            return url;
        } catch (MalformedURLException e2) {
            throw new HttpDataSource.HttpDataSourceException(e2, dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, 1);
        }
    }

    private void skipFully(long bytesToSkip, DataSpec dataSpec) throws IOException {
        if (bytesToSkip == 0) {
            return;
        }
        byte[] skipBuffer = new byte[4096];
        while (bytesToSkip > 0) {
            int readLength = (int) Math.min(bytesToSkip, skipBuffer.length);
            int read = ((InputStream) Util.castNonNull(this.inputStream)).read(skipBuffer, 0, readLength);
            if (Thread.currentThread().isInterrupted()) {
                throw new HttpDataSource.HttpDataSourceException(new InterruptedIOException(), dataSpec, 2000, 1);
            }
            if (read == -1) {
                throw new HttpDataSource.HttpDataSourceException(dataSpec, 2008, 1);
            }
            bytesToSkip -= (long) read;
            bytesTransferred(read);
        }
    }

    private int readInternal(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) {
            return 0;
        }
        if (this.bytesToRead != -1) {
            long bytesRemaining = this.bytesToRead - this.bytesRead;
            if (bytesRemaining == 0) {
                return -1;
            }
            readLength = (int) Math.min(readLength, bytesRemaining);
        }
        int read = ((InputStream) Util.castNonNull(this.inputStream)).read(buffer, offset, readLength);
        if (read == -1) {
            return -1;
        }
        this.bytesRead += (long) read;
        bytesTransferred(read);
        return read;
    }

    private static void maybeTerminateInputStream(HttpURLConnection connection, long bytesRemaining) {
        if (connection == null || Util.SDK_INT > 20) {
            return;
        }
        try {
            InputStream inputStream = connection.getInputStream();
            if (bytesRemaining == -1) {
                if (inputStream.read() == -1) {
                    return;
                }
            } else if (bytesRemaining <= 2048) {
                return;
            }
            String className = inputStream.getClass().getName();
            if ("com.android.okhttp.internal.http.HttpTransport$ChunkedInputStream".equals(className) || "com.android.okhttp.internal.http.HttpTransport$FixedLengthInputStream".equals(className)) {
                Class<?> superclass = inputStream.getClass().getSuperclass();
                Method unexpectedEndOfInput = ((Class) Assertions.checkNotNull(superclass)).getDeclaredMethod("unexpectedEndOfInput", new Class[0]);
                unexpectedEndOfInput.setAccessible(true);
                unexpectedEndOfInput.invoke(inputStream, new Object[0]);
            }
        } catch (Exception e) {
        }
    }

    private void closeConnectionQuietly() {
        if (this.connection != null) {
            try {
                this.connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while disconnecting", e);
            }
            this.connection = null;
        }
    }

    private static boolean isCompressed(HttpURLConnection connection) {
        String contentEncoding = connection.getHeaderField(HttpHeaders.CONTENT_ENCODING);
        return "gzip".equalsIgnoreCase(contentEncoding);
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class NullFilteringHeadersMap extends ForwardingMap<String, List<String>> {
        private final Map<String, List<String>> headers;

        public NullFilteringHeadersMap(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.ForwardingMap, com.google.common.collect.ForwardingObject
        public Map<String, List<String>> delegate() {
            return this.headers;
        }

        @Override // com.google.common.collect.ForwardingMap, java.util.Map
        public boolean containsKey(Object key) {
            return key != null && super.containsKey(key);
        }

        @Override // com.google.common.collect.ForwardingMap, java.util.Map
        public List<String> get(Object key) {
            if (key == null) {
                return null;
            }
            return (List) super.get(key);
        }

        static /* synthetic */ boolean lambda$keySet$0(String key) {
            return key != null;
        }

        @Override // com.google.common.collect.ForwardingMap, java.util.Map
        public Set<String> keySet() {
            return Sets.filter(super.keySet(), new Predicate() { // from class: androidx.media3.datasource.DefaultHttpDataSource$NullFilteringHeadersMap$$ExternalSyntheticLambda1
                @Override // com.google.common.base.Predicate
                public final boolean apply(Object obj) {
                    return DefaultHttpDataSource.NullFilteringHeadersMap.lambda$keySet$0((String) obj);
                }
            });
        }

        static /* synthetic */ boolean lambda$entrySet$1(Map.Entry entry) {
            return entry.getKey() != null;
        }

        @Override // com.google.common.collect.ForwardingMap, java.util.Map
        public Set<Map.Entry<String, List<String>>> entrySet() {
            return Sets.filter(super.entrySet(), new Predicate() { // from class: androidx.media3.datasource.DefaultHttpDataSource$NullFilteringHeadersMap$$ExternalSyntheticLambda0
                @Override // com.google.common.base.Predicate
                public final boolean apply(Object obj) {
                    return DefaultHttpDataSource.NullFilteringHeadersMap.lambda$entrySet$1((Map.Entry) obj);
                }
            });
        }

        @Override // com.google.common.collect.ForwardingMap, java.util.Map
        public int size() {
            return super.size() - (super.containsKey(null) ? 1 : 0);
        }

        @Override // com.google.common.collect.ForwardingMap, java.util.Map
        public boolean isEmpty() {
            if (super.isEmpty()) {
                return true;
            }
            return super.size() == 1 && super.containsKey(null);
        }

        @Override // com.google.common.collect.ForwardingMap, java.util.Map
        public boolean containsValue(Object value) {
            return super.standardContainsValue(value);
        }

        @Override // com.google.common.collect.ForwardingMap, java.util.Map
        public boolean equals(Object object) {
            return object != null && super.standardEquals(object);
        }

        @Override // com.google.common.collect.ForwardingMap, java.util.Map
        public int hashCode() {
            return super.standardHashCode();
        }
    }
}
