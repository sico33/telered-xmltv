package androidx.media3.exoplayer.drm;

import android.net.Uri;
import android.text.TextUtils;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceInputStream;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.StatsDataSource;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public final class HttpMediaDrmCallback implements MediaDrmCallback {
    private static final int MAX_MANUAL_REDIRECTS = 5;
    private final DataSource.Factory dataSourceFactory;
    private final String defaultLicenseUrl;
    private final boolean forceDefaultLicenseUrl;
    private final Map<String, String> keyRequestProperties;

    public HttpMediaDrmCallback(String defaultLicenseUrl, DataSource.Factory dataSourceFactory) {
        this(defaultLicenseUrl, false, dataSourceFactory);
    }

    public HttpMediaDrmCallback(String defaultLicenseUrl, boolean forceDefaultLicenseUrl, DataSource.Factory dataSourceFactory) {
        Assertions.checkArgument((forceDefaultLicenseUrl && TextUtils.isEmpty(defaultLicenseUrl)) ? false : true);
        this.dataSourceFactory = dataSourceFactory;
        this.defaultLicenseUrl = defaultLicenseUrl;
        this.forceDefaultLicenseUrl = forceDefaultLicenseUrl;
        this.keyRequestProperties = new HashMap();
    }

    public void setKeyRequestProperty(String name, String value) {
        Assertions.checkNotNull(name);
        Assertions.checkNotNull(value);
        synchronized (this.keyRequestProperties) {
            this.keyRequestProperties.put(name, value);
        }
    }

    public void clearKeyRequestProperty(String name) {
        Assertions.checkNotNull(name);
        synchronized (this.keyRequestProperties) {
            this.keyRequestProperties.remove(name);
        }
    }

    public void clearAllKeyRequestProperties() {
        synchronized (this.keyRequestProperties) {
            this.keyRequestProperties.clear();
        }
    }

    @Override // androidx.media3.exoplayer.drm.MediaDrmCallback
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws MediaDrmCallbackException {
        String url = request.getDefaultUrl() + "&signedRequest=" + Util.fromUtf8Bytes(request.getData());
        return executePost(this.dataSourceFactory, url, null, Collections.emptyMap());
    }

    @Override // androidx.media3.exoplayer.drm.MediaDrmCallback
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws MediaDrmCallbackException {
        String str;
        String url = request.getLicenseServerUrl();
        String url2 = (this.forceDefaultLicenseUrl || TextUtils.isEmpty(url)) ? this.defaultLicenseUrl : url;
        if (TextUtils.isEmpty(url2)) {
            throw new MediaDrmCallbackException(new DataSpec.Builder().setUri(Uri.EMPTY).build(), Uri.EMPTY, ImmutableMap.of(), 0L, new IllegalStateException("No license URL"));
        }
        Map<String, String> requestProperties = new HashMap<>();
        if (C.PLAYREADY_UUID.equals(uuid)) {
            str = "text/xml";
        } else {
            str = C.CLEARKEY_UUID.equals(uuid) ? "application/json" : "application/octet-stream";
        }
        String contentType = str;
        requestProperties.put(HttpHeaders.CONTENT_TYPE, contentType);
        if (C.PLAYREADY_UUID.equals(uuid)) {
            requestProperties.put("SOAPAction", "http://schemas.microsoft.com/DRM/2007/03/protocols/AcquireLicense");
        }
        synchronized (this.keyRequestProperties) {
            requestProperties.putAll(this.keyRequestProperties);
        }
        return executePost(this.dataSourceFactory, url2, request.getData(), requestProperties);
    }

    private static byte[] executePost(DataSource.Factory dataSourceFactory, String url, byte[] httpBody, Map<String, String> requestProperties) throws MediaDrmCallbackException {
        StatsDataSource dataSource = new StatsDataSource(dataSourceFactory.createDataSource());
        DataSpec dataSpec = new DataSpec.Builder().setUri(url).setHttpRequestHeaders(requestProperties).setHttpMethod(2).setHttpBody(httpBody).setFlags(1).build();
        DataSpec dataSpec2 = dataSpec;
        int manualRedirectCount = 0;
        while (true) {
            try {
                DataSourceInputStream inputStream = new DataSourceInputStream(dataSource, dataSpec2);
                try {
                    byte[] byteArray = ByteStreams.toByteArray(inputStream);
                    Util.closeQuietly(inputStream);
                    return byteArray;
                } catch (HttpDataSource.InvalidResponseCodeException e) {
                    try {
                        String redirectUrl = getRedirectUrl(e, manualRedirectCount);
                        if (redirectUrl == null) {
                            throw e;
                        }
                        manualRedirectCount++;
                        dataSpec2 = dataSpec2.buildUpon().setUri(redirectUrl).build();
                        Util.closeQuietly(inputStream);
                    } catch (Throwable th) {
                        Util.closeQuietly(inputStream);
                        throw th;
                    }
                }
            } catch (Exception e2) {
                throw new MediaDrmCallbackException(dataSpec, (Uri) Assertions.checkNotNull(dataSource.getLastOpenedUri()), dataSource.getResponseHeaders(), dataSource.getBytesRead(), e2);
            }
        }
    }

    private static String getRedirectUrl(HttpDataSource.InvalidResponseCodeException exception, int manualRedirectCount) {
        Map<String, List<String>> headerFields;
        List<String> locationHeaders;
        boolean manuallyRedirect = (exception.responseCode == 307 || exception.responseCode == 308) && manualRedirectCount < 5;
        if (!manuallyRedirect || (headerFields = exception.headerFields) == null || (locationHeaders = headerFields.get(HttpHeaders.LOCATION)) == null || locationHeaders.isEmpty()) {
            return null;
        }
        return locationHeaders.get(0);
    }
}
