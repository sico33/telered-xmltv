package androidx.media3.exoplayer.drm;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.primitives.Ints;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultDrmSessionManagerProvider implements DrmSessionManagerProvider {
    private MediaItem.DrmConfiguration drmConfiguration;
    private DataSource.Factory drmHttpDataSourceFactory;
    private LoadErrorHandlingPolicy drmLoadErrorHandlingPolicy;
    private final Object lock = new Object();
    private DrmSessionManager manager;
    private String userAgent;

    public void setDrmHttpDataSourceFactory(DataSource.Factory drmDataSourceFactory) {
        this.drmHttpDataSourceFactory = drmDataSourceFactory;
    }

    @Deprecated
    public void setDrmUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setDrmLoadErrorHandlingPolicy(LoadErrorHandlingPolicy drmLoadErrorHandlingPolicy) {
        this.drmLoadErrorHandlingPolicy = drmLoadErrorHandlingPolicy;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionManagerProvider
    public DrmSessionManager get(MediaItem mediaItem) {
        DrmSessionManager drmSessionManager;
        Assertions.checkNotNull(mediaItem.localConfiguration);
        MediaItem.DrmConfiguration drmConfiguration = mediaItem.localConfiguration.drmConfiguration;
        if (drmConfiguration == null) {
            return DrmSessionManager.DRM_UNSUPPORTED;
        }
        synchronized (this.lock) {
            if (!Util.areEqual(drmConfiguration, this.drmConfiguration)) {
                this.drmConfiguration = drmConfiguration;
                this.manager = createManager(drmConfiguration);
            }
            drmSessionManager = (DrmSessionManager) Assertions.checkNotNull(this.manager);
        }
        return drmSessionManager;
    }

    private DrmSessionManager createManager(MediaItem.DrmConfiguration drmConfiguration) {
        DataSource.Factory dataSourceFactory;
        if (this.drmHttpDataSourceFactory != null) {
            dataSourceFactory = this.drmHttpDataSourceFactory;
        } else {
            dataSourceFactory = new DefaultHttpDataSource.Factory().setUserAgent(this.userAgent);
        }
        HttpMediaDrmCallback httpDrmCallback = new HttpMediaDrmCallback(drmConfiguration.licenseUri == null ? null : drmConfiguration.licenseUri.toString(), drmConfiguration.forceDefaultLicenseUri, dataSourceFactory);
        UnmodifiableIterator<Map.Entry<String, String>> it = drmConfiguration.licenseRequestHeaders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            httpDrmCallback.setKeyRequestProperty(entry.getKey(), entry.getValue());
        }
        DefaultDrmSessionManager.Builder drmSessionManagerBuilder = new DefaultDrmSessionManager.Builder().setUuidAndExoMediaDrmProvider(drmConfiguration.scheme, FrameworkMediaDrm.DEFAULT_PROVIDER).setMultiSession(drmConfiguration.multiSession).setPlayClearSamplesWithoutKeys(drmConfiguration.playClearContentWithoutKey).setUseDrmSessionsForClearContent(Ints.toArray(drmConfiguration.forcedSessionTrackTypes));
        if (this.drmLoadErrorHandlingPolicy != null) {
            drmSessionManagerBuilder.setLoadErrorHandlingPolicy(this.drmLoadErrorHandlingPolicy);
        }
        DefaultDrmSessionManager drmSessionManager = drmSessionManagerBuilder.build(httpDrmCallback);
        drmSessionManager.setMode(0, drmConfiguration.getKeySetId());
        return drmSessionManager;
    }
}
