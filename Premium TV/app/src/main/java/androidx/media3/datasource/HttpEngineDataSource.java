package androidx.media3.datasource;

import android.net.Uri;
import android.net.http.HttpEngine;
import android.net.http.HttpException;
import android.net.http.NetworkException;
import android.net.http.UrlRequest;
import android.net.http.UrlResponseInfo;
import android.text.TextUtils;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.ConditionVariable;
import androidx.media3.common.util.Util;
import com.google.common.base.Ascii;
import com.google.common.base.Predicate;
import com.google.common.net.HttpHeaders;
import com.google.common.primitives.Longs;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public final class HttpEngineDataSource extends BaseDataSource implements HttpDataSource {
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 8000;
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 8000;
    private static final int READ_BUFFER_SIZE_BYTES = 32768;
    private long bytesRemaining;
    private final Clock clock;
    private final int connectTimeoutMs;
    private Predicate<String> contentTypePredicate;
    private volatile long currentConnectTimeoutMs;
    private DataSpec currentDataSpec;
    private UrlRequestWrapper currentUrlRequestWrapper;
    private final HttpDataSource.RequestProperties defaultRequestProperties;
    private IOException exception;
    private final Executor executor;
    private boolean finished;
    private final boolean handleSetCookieRequests;
    private final HttpEngine httpEngine;
    private final boolean keepPostFor302Redirects;
    private boolean opened;
    private final ConditionVariable operation;
    private ByteBuffer readBuffer;
    private final int readTimeoutMs;
    private final int requestPriority;
    private final HttpDataSource.RequestProperties requestProperties;
    private final boolean resetTimeoutOnRedirects;
    private UrlResponseInfo responseInfo;
    private final String userAgent;

    public static final class Factory implements HttpDataSource.Factory {
        private Predicate<String> contentTypePredicate;
        private final Executor executor;
        private boolean handleSetCookieRequests;
        private final HttpEngine httpEngine;
        private boolean keepPostFor302Redirects;
        private boolean resetTimeoutOnRedirects;
        private TransferListener transferListener;
        private String userAgent;
        private final HttpDataSource.RequestProperties defaultRequestProperties = new HttpDataSource.RequestProperties();
        private int requestPriority = 3;
        private int connectTimeoutMs = 8000;
        private int readTimeoutMs = 8000;

        @Override // androidx.media3.datasource.HttpDataSource.Factory
        public /* bridge */ /* synthetic */ HttpDataSource.Factory setDefaultRequestProperties(Map map) {
            return setDefaultRequestProperties((Map<String, String>) map);
        }

        public Factory(HttpEngine httpEngine, Executor executor) {
            this.httpEngine = (HttpEngine) Assertions.checkNotNull(httpEngine);
            this.executor = executor;
        }

        @Override // androidx.media3.datasource.HttpDataSource.Factory
        public final Factory setDefaultRequestProperties(Map<String, String> defaultRequestProperties) {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties);
            return this;
        }

        public Factory setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Factory setRequestPriority(int requestPriority) {
            this.requestPriority = requestPriority;
            return this;
        }

        public Factory setConnectionTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Factory setResetTimeoutOnRedirects(boolean resetTimeoutOnRedirects) {
            this.resetTimeoutOnRedirects = resetTimeoutOnRedirects;
            return this;
        }

        public Factory setHandleSetCookieRequests(boolean handleSetCookieRequests) {
            this.handleSetCookieRequests = handleSetCookieRequests;
            return this;
        }

        public Factory setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        public Factory setContentTypePredicate(Predicate<String> contentTypePredicate) {
            this.contentTypePredicate = contentTypePredicate;
            return this;
        }

        public Factory setKeepPostFor302Redirects(boolean keepPostFor302Redirects) {
            this.keepPostFor302Redirects = keepPostFor302Redirects;
            return this;
        }

        public Factory setTransferListener(TransferListener transferListener) {
            this.transferListener = transferListener;
            return this;
        }

        @Override // androidx.media3.datasource.DataSource.Factory
        public HttpDataSource createDataSource() {
            HttpEngineDataSource dataSource = new HttpEngineDataSource(this.httpEngine, this.executor, this.requestPriority, this.connectTimeoutMs, this.readTimeoutMs, this.resetTimeoutOnRedirects, this.handleSetCookieRequests, this.userAgent, this.defaultRequestProperties, this.contentTypePredicate, this.keepPostFor302Redirects);
            if (this.transferListener != null) {
                dataSource.addTransferListener(this.transferListener);
            }
            return dataSource;
        }
    }

    public static final class OpenException extends HttpDataSource.HttpDataSourceException {
        public final int httpEngineConnectionStatus;

        public OpenException(IOException cause, DataSpec dataSpec, int errorCode, int httpEngineConnectionStatus) {
            super(cause, dataSpec, errorCode, 1);
            this.httpEngineConnectionStatus = httpEngineConnectionStatus;
        }

        public OpenException(String errorMessage, DataSpec dataSpec, int errorCode, int httpEngineConnectionStatus) {
            super(errorMessage, dataSpec, errorCode, 1);
            this.httpEngineConnectionStatus = httpEngineConnectionStatus;
        }

        public OpenException(DataSpec dataSpec, int errorCode, int httpEngineConnectionStatus) {
            super(dataSpec, errorCode, 1);
            this.httpEngineConnectionStatus = httpEngineConnectionStatus;
        }
    }

    HttpEngineDataSource(HttpEngine httpEngine, Executor executor, int requestPriority, int connectTimeoutMs, int readTimeoutMs, boolean resetTimeoutOnRedirects, boolean handleSetCookieRequests, String userAgent, HttpDataSource.RequestProperties defaultRequestProperties, Predicate<String> contentTypePredicate, boolean keepPostFor302Redirects) {
        super(true);
        this.httpEngine = (HttpEngine) Assertions.checkNotNull(httpEngine);
        this.executor = (Executor) Assertions.checkNotNull(executor);
        this.requestPriority = requestPriority;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.resetTimeoutOnRedirects = resetTimeoutOnRedirects;
        this.handleSetCookieRequests = handleSetCookieRequests;
        this.userAgent = userAgent;
        this.defaultRequestProperties = defaultRequestProperties;
        this.contentTypePredicate = contentTypePredicate;
        this.keepPostFor302Redirects = keepPostFor302Redirects;
        this.clock = Clock.DEFAULT;
        this.requestProperties = new HttpDataSource.RequestProperties();
        this.operation = new ConditionVariable();
    }

    @Override // androidx.media3.datasource.HttpDataSource
    public void setRequestProperty(String name, String value) {
        this.requestProperties.set(name, value);
    }

    @Override // androidx.media3.datasource.HttpDataSource
    public void clearRequestProperty(String name) {
        this.requestProperties.remove(name);
    }

    @Override // androidx.media3.datasource.HttpDataSource
    public void clearAllRequestProperties() {
        this.requestProperties.clear();
    }

    @Override // androidx.media3.datasource.HttpDataSource
    public int getResponseCode() {
        if (this.responseInfo == null || this.responseInfo.getHttpStatusCode() <= 0) {
            return -1;
        }
        return this.responseInfo.getHttpStatusCode();
    }

    @Override // androidx.media3.datasource.BaseDataSource, androidx.media3.datasource.DataSource
    public Map<String, List<String>> getResponseHeaders() {
        return this.responseInfo == null ? Collections.emptyMap() : this.responseInfo.getHeaders().getAsMap();
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        if (this.responseInfo == null) {
            return null;
        }
        return Uri.parse(this.responseInfo.getUrl());
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws HttpDataSource.HttpDataSourceException {
        byte[] responseBody;
        IOException cause;
        long j;
        long bytesToSkip;
        String contentType;
        Assertions.checkNotNull(dataSpec);
        Assertions.checkState(!this.opened);
        this.operation.close();
        resetConnectTimeout();
        this.currentDataSpec = dataSpec;
        try {
            UrlRequestWrapper urlRequestWrapper = buildRequestWrapper(dataSpec);
            this.currentUrlRequestWrapper = urlRequestWrapper;
            urlRequestWrapper.start();
            transferInitializing(dataSpec);
            try {
                boolean connectionOpened = blockUntilConnectTimeout();
                IOException connectionOpenException = this.exception;
                if (connectionOpenException != null) {
                    String message = connectionOpenException.getMessage();
                    if (message != null && Ascii.toLowerCase(message).contains("err_cleartext_not_permitted")) {
                        throw new HttpDataSource.CleartextNotPermittedException(connectionOpenException, dataSpec);
                    }
                    throw new OpenException(connectionOpenException, dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, urlRequestWrapper.getStatus());
                }
                if (!connectionOpened) {
                    throw new OpenException(new SocketTimeoutException(), dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, urlRequestWrapper.getStatus());
                }
                UrlResponseInfo responseInfo = (UrlResponseInfo) Assertions.checkNotNull(this.responseInfo);
                int responseCode = responseInfo.getHttpStatusCode();
                Map<String, List<String>> responseHeaders = responseInfo.getHeaders().getAsMap();
                if (responseCode < 200 || responseCode > 299) {
                    long j2 = 0;
                    long j3 = -1;
                    if (responseCode == 416) {
                        long documentSize = HttpUtil.getDocumentSize(getFirstHeader(responseHeaders, HttpHeaders.CONTENT_RANGE));
                        if (dataSpec.position == documentSize) {
                            this.opened = true;
                            transferStarted(dataSpec);
                            return dataSpec.length != j3 ? dataSpec.length : j2;
                        }
                    }
                    try {
                        byte[] responseBody2 = readResponseBody();
                        responseBody = responseBody2;
                    } catch (IOException e) {
                        responseBody = Util.EMPTY_BYTE_ARRAY;
                    }
                    if (responseCode == 416) {
                        cause = new DataSourceException(2008);
                    } else {
                        cause = null;
                    }
                    throw new HttpDataSource.InvalidResponseCodeException(responseCode, responseInfo.getHttpStatusText(), cause, responseHeaders, dataSpec, responseBody);
                }
                Predicate<String> contentTypePredicate = this.contentTypePredicate;
                if (contentTypePredicate != null && (contentType = getFirstHeader(responseHeaders, HttpHeaders.CONTENT_TYPE)) != null && !contentTypePredicate.apply(contentType)) {
                    throw new HttpDataSource.InvalidContentTypeException(contentType, dataSpec);
                }
                if (responseCode == 200) {
                    j = 0;
                    if (dataSpec.position != 0) {
                        bytesToSkip = dataSpec.position;
                    }
                    if (!isCompressed(responseInfo) || dataSpec.length != -1) {
                        this.bytesRemaining = dataSpec.length;
                    } else {
                        long contentLength = HttpUtil.getContentLength(getFirstHeader(responseHeaders, HttpHeaders.CONTENT_LENGTH), getFirstHeader(responseHeaders, HttpHeaders.CONTENT_RANGE));
                        this.bytesRemaining = contentLength != -1 ? contentLength - bytesToSkip : -1L;
                    }
                    this.opened = true;
                    transferStarted(dataSpec);
                    skipFully(bytesToSkip, dataSpec);
                    return this.bytesRemaining;
                }
                j = 0;
                bytesToSkip = j;
                if (!isCompressed(responseInfo)) {
                    this.bytesRemaining = dataSpec.length;
                } else {
                    this.bytesRemaining = dataSpec.length;
                }
                this.opened = true;
                transferStarted(dataSpec);
                skipFully(bytesToSkip, dataSpec);
                return this.bytesRemaining;
            } catch (InterruptedException e2) {
                Thread.currentThread().interrupt();
                throw new OpenException(new InterruptedIOException(), dataSpec, 1004, -1);
            }
        } catch (IOException e3) {
            if (e3 instanceof HttpDataSource.HttpDataSourceException) {
                throw ((HttpDataSource.HttpDataSourceException) e3);
            }
            throw new OpenException(e3, dataSpec, 2000, 0);
        }
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws HttpDataSource.HttpDataSourceException {
        Assertions.checkState(this.opened);
        if (length == 0) {
            return 0;
        }
        if (this.bytesRemaining == 0) {
            return -1;
        }
        ByteBuffer readBuffer = getOrCreateReadBuffer();
        if (!readBuffer.hasRemaining()) {
            this.operation.close();
            readBuffer.clear();
            readInternal(readBuffer, (DataSpec) Util.castNonNull(this.currentDataSpec));
            if (this.finished) {
                this.bytesRemaining = 0L;
                return -1;
            }
            readBuffer.flip();
            Assertions.checkState(readBuffer.hasRemaining());
        }
        int bytesRead = (int) Longs.min(this.bytesRemaining != -1 ? this.bytesRemaining : Long.MAX_VALUE, readBuffer.remaining(), length);
        readBuffer.get(buffer, offset, bytesRead);
        if (this.bytesRemaining != -1) {
            this.bytesRemaining -= (long) bytesRead;
        }
        bytesTransferred(bytesRead);
        return bytesRead;
    }

    public int read(ByteBuffer buffer) throws HttpDataSource.HttpDataSourceException {
        int copyBytes;
        Assertions.checkState(this.opened);
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("Passed buffer is not a direct ByteBuffer");
        }
        if (!buffer.hasRemaining()) {
            return 0;
        }
        if (this.bytesRemaining == 0) {
            return -1;
        }
        int readLength = buffer.remaining();
        if (this.readBuffer != null && (copyBytes = copyByteBuffer(this.readBuffer, buffer)) != 0) {
            if (this.bytesRemaining != -1) {
                this.bytesRemaining -= (long) copyBytes;
            }
            bytesTransferred(copyBytes);
            return copyBytes;
        }
        this.operation.close();
        readInternal(buffer, (DataSpec) Util.castNonNull(this.currentDataSpec));
        if (this.finished) {
            this.bytesRemaining = 0L;
            return -1;
        }
        Assertions.checkState(readLength > buffer.remaining());
        int bytesRead = readLength - buffer.remaining();
        if (this.bytesRemaining != -1) {
            this.bytesRemaining -= (long) bytesRead;
        }
        bytesTransferred(bytesRead);
        return bytesRead;
    }

    @Override // androidx.media3.datasource.DataSource
    public synchronized void close() {
        if (this.currentUrlRequestWrapper != null) {
            this.currentUrlRequestWrapper.close();
            this.currentUrlRequestWrapper = null;
        }
        if (this.readBuffer != null) {
            this.readBuffer.limit(0);
        }
        this.currentDataSpec = null;
        this.responseInfo = null;
        this.exception = null;
        this.finished = false;
        if (this.opened) {
            this.opened = false;
            transferEnded();
        }
    }

    UrlRequest.Callback getCurrentUrlRequestCallback() {
        if (this.currentUrlRequestWrapper == null) {
            return null;
        }
        return this.currentUrlRequestWrapper.getUrlRequestCallback();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UrlRequestWrapper buildRequestWrapper(DataSpec dataSpec) throws IOException {
        UrlRequestCallback callback = new UrlRequestCallback();
        return new UrlRequestWrapper(buildRequestBuilder(dataSpec, callback).build(), callback);
    }

    private UrlRequest.Builder buildRequestBuilder(DataSpec dataSpec, UrlRequest.Callback urlRequestCallback) throws IOException {
        UrlRequest.Builder requestBuilder = this.httpEngine.newUrlRequestBuilder(dataSpec.uri.toString(), this.executor, urlRequestCallback).setPriority(this.requestPriority).setDirectExecutorAllowed(true);
        Map<String, String> requestHeaders = new HashMap<>();
        if (this.defaultRequestProperties != null) {
            requestHeaders.putAll(this.defaultRequestProperties.getSnapshot());
        }
        requestHeaders.putAll(this.requestProperties.getSnapshot());
        requestHeaders.putAll(dataSpec.httpRequestHeaders);
        for (Map.Entry<String, String> headerEntry : requestHeaders.entrySet()) {
            String key = headerEntry.getKey();
            String value = headerEntry.getValue();
            requestBuilder.addHeader(key, value);
        }
        if (dataSpec.httpBody != null && !requestHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            throw new OpenException("HTTP request with non-empty body must set Content-Type", dataSpec, 1004, 0);
        }
        String rangeHeader = HttpUtil.buildRangeRequestHeader(dataSpec.position, dataSpec.length);
        if (rangeHeader != null) {
            requestBuilder.addHeader(HttpHeaders.RANGE, rangeHeader);
        }
        if (this.userAgent != null) {
            requestBuilder.addHeader(HttpHeaders.USER_AGENT, this.userAgent);
        }
        requestBuilder.setHttpMethod(dataSpec.getHttpMethodString());
        if (dataSpec.httpBody != null) {
            requestBuilder.setUploadDataProvider(new ByteArrayUploadDataProvider(dataSpec.httpBody), this.executor);
        }
        return requestBuilder;
    }

    private boolean blockUntilConnectTimeout() throws InterruptedException {
        long now = this.clock.elapsedRealtime();
        boolean opened = false;
        while (!opened && now < this.currentConnectTimeoutMs) {
            opened = this.operation.block((this.currentConnectTimeoutMs - now) + 5);
            now = this.clock.elapsedRealtime();
        }
        return opened;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetConnectTimeout() {
        this.currentConnectTimeoutMs = this.clock.elapsedRealtime() + ((long) this.connectTimeoutMs);
    }

    private void skipFully(long bytesToSkip, DataSpec dataSpec) throws HttpDataSource.HttpDataSourceException {
        int i;
        if (bytesToSkip == 0) {
            return;
        }
        ByteBuffer readBuffer = getOrCreateReadBuffer();
        while (bytesToSkip > 0) {
            try {
                this.operation.close();
                readBuffer.clear();
                readInternal(readBuffer, dataSpec);
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException();
                }
                if (this.finished) {
                    throw new OpenException(dataSpec, 2008, 14);
                }
                readBuffer.flip();
                Assertions.checkState(readBuffer.hasRemaining());
                int bytesSkipped = (int) Math.min(readBuffer.remaining(), bytesToSkip);
                readBuffer.position(readBuffer.position() + bytesSkipped);
                bytesToSkip -= (long) bytesSkipped;
            } catch (IOException e) {
                if (e instanceof HttpDataSource.HttpDataSourceException) {
                    throw ((HttpDataSource.HttpDataSourceException) e);
                }
                if (e instanceof SocketTimeoutException) {
                    i = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT;
                } else {
                    i = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED;
                }
                throw new OpenException(e, dataSpec, i, 14);
            }
        }
    }

    private byte[] readResponseBody() throws IOException {
        byte[] responseBody = Util.EMPTY_BYTE_ARRAY;
        ByteBuffer readBuffer = getOrCreateReadBuffer();
        while (!this.finished) {
            this.operation.close();
            readBuffer.clear();
            readInternal(readBuffer, (DataSpec) Util.castNonNull(this.currentDataSpec));
            readBuffer.flip();
            if (readBuffer.remaining() > 0) {
                int existingResponseBodyEnd = responseBody.length;
                responseBody = Arrays.copyOf(responseBody, responseBody.length + readBuffer.remaining());
                readBuffer.get(responseBody, existingResponseBodyEnd, readBuffer.remaining());
            }
        }
        return responseBody;
    }

    private void readInternal(ByteBuffer buffer, DataSpec dataSpec) throws HttpDataSource.HttpDataSourceException {
        ((UrlRequestWrapper) Util.castNonNull(this.currentUrlRequestWrapper)).read(buffer);
        try {
            if (!this.operation.block(this.readTimeoutMs)) {
                throw new SocketTimeoutException();
            }
        } catch (InterruptedException e) {
            if (buffer == this.readBuffer) {
                this.readBuffer = null;
            }
            Thread.currentThread().interrupt();
            this.exception = new InterruptedIOException();
        } catch (SocketTimeoutException e2) {
            if (buffer == this.readBuffer) {
                this.readBuffer = null;
            }
            this.exception = new HttpDataSource.HttpDataSourceException(e2, dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, 2);
        }
        if (this.exception != null) {
            boolean z = this.exception instanceof HttpDataSource.HttpDataSourceException;
            IOException iOException = this.exception;
            if (z) {
                throw ((HttpDataSource.HttpDataSourceException) iOException);
            }
            throw HttpDataSource.HttpDataSourceException.createForIOException(iOException, dataSpec, 2);
        }
    }

    private ByteBuffer getOrCreateReadBuffer() {
        if (this.readBuffer == null) {
            this.readBuffer = ByteBuffer.allocateDirect(32768);
            this.readBuffer.limit(0);
        }
        return this.readBuffer;
    }

    private static boolean isCompressed(UrlResponseInfo info) {
        for (Map.Entry<String, String> entry : info.getHeaders().getAsList()) {
            if (entry.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_ENCODING)) {
                return !entry.getValue().equalsIgnoreCase("identity");
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String parseCookies(List<String> setCookieHeaders) {
        if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
            return null;
        }
        return TextUtils.join(";", setCookieHeaders);
    }

    private static String getFirstHeader(Map<String, List<String>> allHeaders, String headerName) {
        List<String> headers = allHeaders.get(headerName);
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        return headers.get(0);
    }

    private static int copyByteBuffer(ByteBuffer src, ByteBuffer dst) {
        int remaining = Math.min(src.remaining(), dst.remaining());
        int limit = src.limit();
        src.limit(src.position() + remaining);
        dst.put(src);
        src.limit(limit);
        return remaining;
    }

    private static final class UrlRequestWrapper {
        private final UrlRequest urlRequest;
        private final UrlRequestCallback urlRequestCallback;

        UrlRequestWrapper(UrlRequest urlRequest, UrlRequestCallback urlRequestCallback) {
            this.urlRequest = urlRequest;
            this.urlRequestCallback = urlRequestCallback;
        }

        public void start() {
            this.urlRequest.start();
        }

        public void read(ByteBuffer buffer) {
            this.urlRequest.read(buffer);
        }

        public void close() {
            this.urlRequestCallback.close();
            this.urlRequest.cancel();
        }

        public UrlRequest.Callback getUrlRequestCallback() {
            return this.urlRequestCallback;
        }

        public int getStatus() throws InterruptedException {
            final ConditionVariable conditionVariable = new ConditionVariable();
            final int[] statusHolder = new int[1];
            this.urlRequest.getStatus(new UrlRequest.StatusListener() { // from class: androidx.media3.datasource.HttpEngineDataSource.UrlRequestWrapper.1
                public void onStatus(int status) {
                    statusHolder[0] = status;
                    conditionVariable.open();
                }
            });
            conditionVariable.block();
            return statusHolder[0];
        }
    }

    private final class UrlRequestCallback implements UrlRequest.Callback {
        private volatile boolean isClosed;

        private UrlRequestCallback() {
            this.isClosed = false;
        }

        public void close() {
            this.isClosed = true;
        }

        public synchronized void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
            DataSpec redirectUrlDataSpec;
            DataSpec redirectUrlDataSpec2;
            if (this.isClosed) {
                return;
            }
            DataSpec dataSpec = (DataSpec) Assertions.checkNotNull(HttpEngineDataSource.this.currentDataSpec);
            int responseCode = info.getHttpStatusCode();
            if (dataSpec.httpMethod != 2 || (responseCode != 307 && responseCode != 308)) {
                if (HttpEngineDataSource.this.resetTimeoutOnRedirects) {
                    HttpEngineDataSource.this.resetConnectTimeout();
                }
                boolean shouldKeepPost = HttpEngineDataSource.this.keepPostFor302Redirects && dataSpec.httpMethod == 2 && responseCode == 302;
                if (shouldKeepPost || HttpEngineDataSource.this.handleSetCookieRequests) {
                    String cookieHeadersValue = HttpEngineDataSource.parseCookies((List) info.getHeaders().getAsMap().get(HttpHeaders.SET_COOKIE));
                    if (!shouldKeepPost && TextUtils.isEmpty(cookieHeadersValue)) {
                        request.followRedirect();
                        return;
                    }
                    request.cancel();
                    if (!shouldKeepPost && dataSpec.httpMethod == 2) {
                        redirectUrlDataSpec = dataSpec.buildUpon().setUri(newLocationUrl).setHttpMethod(1).setHttpBody(null).build();
                    } else {
                        redirectUrlDataSpec = dataSpec.withUri(Uri.parse(newLocationUrl));
                    }
                    if (TextUtils.isEmpty(cookieHeadersValue)) {
                        redirectUrlDataSpec2 = redirectUrlDataSpec;
                    } else {
                        Map<String, String> requestHeaders = new HashMap<>();
                        requestHeaders.putAll(dataSpec.httpRequestHeaders);
                        requestHeaders.put(HttpHeaders.COOKIE, cookieHeadersValue);
                        redirectUrlDataSpec2 = redirectUrlDataSpec.buildUpon().setHttpRequestHeaders(requestHeaders).build();
                    }
                    try {
                        UrlRequestWrapper redirectUrlRequestWrapper = HttpEngineDataSource.this.buildRequestWrapper(redirectUrlDataSpec2);
                        if (HttpEngineDataSource.this.currentUrlRequestWrapper != null) {
                            HttpEngineDataSource.this.currentUrlRequestWrapper.close();
                        }
                        HttpEngineDataSource.this.currentUrlRequestWrapper = redirectUrlRequestWrapper;
                        HttpEngineDataSource.this.currentUrlRequestWrapper.start();
                        return;
                    } catch (IOException e) {
                        HttpEngineDataSource.this.exception = e;
                        return;
                    }
                }
                request.followRedirect();
                return;
            }
            HttpEngineDataSource.this.exception = new HttpDataSource.InvalidResponseCodeException(responseCode, info.getHttpStatusText(), null, info.getHeaders().getAsMap(), dataSpec, Util.EMPTY_BYTE_ARRAY);
            HttpEngineDataSource.this.operation.open();
        }

        public synchronized void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
            if (this.isClosed) {
                return;
            }
            HttpEngineDataSource.this.responseInfo = info;
            HttpEngineDataSource.this.operation.open();
        }

        public synchronized void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer buffer) {
            if (this.isClosed) {
                return;
            }
            HttpEngineDataSource.this.operation.open();
        }

        public synchronized void onSucceeded(UrlRequest request, UrlResponseInfo info) {
            if (this.isClosed) {
                return;
            }
            HttpEngineDataSource.this.finished = true;
            HttpEngineDataSource.this.operation.open();
        }

        public synchronized void onFailed(UrlRequest request, UrlResponseInfo info, HttpException error) {
            if (this.isClosed) {
                return;
            }
            if (!(error instanceof NetworkException) || ((NetworkException) error).getErrorCode() != 1) {
                HttpEngineDataSource.this.exception = error;
            } else {
                HttpEngineDataSource.this.exception = new UnknownHostException();
            }
            HttpEngineDataSource.this.operation.open();
        }

        public synchronized void onCanceled(UrlRequest request, UrlResponseInfo info) {
        }
    }
}
