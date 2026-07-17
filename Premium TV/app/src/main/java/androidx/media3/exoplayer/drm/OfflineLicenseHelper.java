package androidx.media3.exoplayer.drm;

import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Pair;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.MediaSource;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/* JADX INFO: loaded from: classes.dex */
public final class OfflineLicenseHelper {
    private static final Format FORMAT_WITH_EMPTY_DRM_INIT_DATA = new Format.Builder().setDrmInitData(new DrmInitData(new DrmInitData.SchemeData[0])).build();
    private final ConditionVariable drmListenerConditionVariable;
    private final DefaultDrmSessionManager drmSessionManager;
    private final DrmSessionEventListener.EventDispatcher eventDispatcher;
    private final Handler handler;
    private final HandlerThread handlerThread = new HandlerThread("ExoPlayer:OfflineLicenseHelper");

    public static OfflineLicenseHelper newWidevineInstance(String defaultLicenseUrl, DataSource.Factory dataSourceFactory, DrmSessionEventListener.EventDispatcher eventDispatcher) {
        return newWidevineInstance(defaultLicenseUrl, false, dataSourceFactory, eventDispatcher);
    }

    public static OfflineLicenseHelper newWidevineInstance(String defaultLicenseUrl, boolean forceDefaultLicenseUrl, DataSource.Factory dataSourceFactory, DrmSessionEventListener.EventDispatcher eventDispatcher) {
        return newWidevineInstance(defaultLicenseUrl, forceDefaultLicenseUrl, dataSourceFactory, null, eventDispatcher);
    }

    public static OfflineLicenseHelper newWidevineInstance(String defaultLicenseUrl, boolean forceDefaultLicenseUrl, DataSource.Factory dataSourceFactory, Map<String, String> optionalKeyRequestParameters, DrmSessionEventListener.EventDispatcher eventDispatcher) {
        return new OfflineLicenseHelper(new DefaultDrmSessionManager.Builder().setKeyRequestParameters(optionalKeyRequestParameters).build(new HttpMediaDrmCallback(defaultLicenseUrl, forceDefaultLicenseUrl, dataSourceFactory)), eventDispatcher);
    }

    public OfflineLicenseHelper(DefaultDrmSessionManager defaultDrmSessionManager, DrmSessionEventListener.EventDispatcher eventDispatcher) {
        this.drmSessionManager = defaultDrmSessionManager;
        this.eventDispatcher = eventDispatcher;
        this.handlerThread.start();
        this.handler = new Handler(this.handlerThread.getLooper());
        this.drmListenerConditionVariable = new ConditionVariable();
        DrmSessionEventListener eventListener = new DrmSessionEventListener() { // from class: androidx.media3.exoplayer.drm.OfflineLicenseHelper.1
            @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
            public /* synthetic */ void onDrmSessionAcquired(int i, MediaSource.MediaPeriodId mediaPeriodId) {
                DrmSessionEventListener.CC.$default$onDrmSessionAcquired(this, i, mediaPeriodId);
            }

            @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
            public /* synthetic */ void onDrmSessionAcquired(int i, MediaSource.MediaPeriodId mediaPeriodId, int i2) {
                DrmSessionEventListener.CC.$default$onDrmSessionAcquired(this, i, mediaPeriodId, i2);
            }

            @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
            public /* synthetic */ void onDrmSessionReleased(int i, MediaSource.MediaPeriodId mediaPeriodId) {
                DrmSessionEventListener.CC.$default$onDrmSessionReleased(this, i, mediaPeriodId);
            }

            @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
            public void onDrmKeysLoaded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
                OfflineLicenseHelper.this.drmListenerConditionVariable.open();
            }

            @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
            public void onDrmSessionManagerError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, Exception e) {
                OfflineLicenseHelper.this.drmListenerConditionVariable.open();
            }

            @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
            public void onDrmKeysRestored(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
                OfflineLicenseHelper.this.drmListenerConditionVariable.open();
            }

            @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
            public void onDrmKeysRemoved(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
                OfflineLicenseHelper.this.drmListenerConditionVariable.open();
            }
        };
        eventDispatcher.addEventListener(new Handler(this.handlerThread.getLooper()), eventListener);
    }

    public synchronized byte[] downloadLicense(Format format) throws DrmSession.DrmSessionException {
        Assertions.checkArgument(format.drmInitData != null);
        return acquireSessionAndGetOfflineLicenseKeySetIdOnHandlerThread(2, null, format);
    }

    public synchronized byte[] renewLicense(byte[] offlineLicenseKeySetId) throws DrmSession.DrmSessionException {
        Assertions.checkNotNull(offlineLicenseKeySetId);
        return acquireSessionAndGetOfflineLicenseKeySetIdOnHandlerThread(2, offlineLicenseKeySetId, FORMAT_WITH_EMPTY_DRM_INIT_DATA);
    }

    public synchronized void releaseLicense(byte[] offlineLicenseKeySetId) throws DrmSession.DrmSessionException {
        Assertions.checkNotNull(offlineLicenseKeySetId);
        acquireSessionAndGetOfflineLicenseKeySetIdOnHandlerThread(3, offlineLicenseKeySetId, FORMAT_WITH_EMPTY_DRM_INIT_DATA);
    }

    public synchronized Pair<Long, Long> getLicenseDurationRemainingSec(byte[] offlineLicenseKeySetId) throws DrmSession.DrmSessionException {
        Pair<Long, Long> pair;
        Assertions.checkNotNull(offlineLicenseKeySetId);
        try {
            final DrmSession drmSession = acquireFirstSessionOnHandlerThread(1, offlineLicenseKeySetId, FORMAT_WITH_EMPTY_DRM_INIT_DATA);
            final SettableFuture<Pair<Long, Long>> licenseDurationRemainingSec = SettableFuture.create();
            this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.drm.OfflineLicenseHelper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m92xc6195285(licenseDurationRemainingSec, drmSession);
                }
            });
            try {
                try {
                    pair = licenseDurationRemainingSec.get();
                    releaseManagerOnHandlerThread();
                } catch (InterruptedException | ExecutionException e) {
                    throw new IllegalStateException(e);
                }
            } catch (Throwable th) {
                releaseManagerOnHandlerThread();
                throw th;
            }
        } catch (DrmSession.DrmSessionException e2) {
            if (e2.getCause() instanceof KeysExpiredException) {
                return Pair.create(0L, 0L);
            }
            throw e2;
        }
        return pair;
    }

    /* JADX INFO: renamed from: lambda$getLicenseDurationRemainingSec$0$androidx-media3-exoplayer-drm-OfflineLicenseHelper, reason: not valid java name */
    /* synthetic */ void m92xc6195285(SettableFuture licenseDurationRemainingSec, DrmSession drmSession) {
        try {
            licenseDurationRemainingSec.set((Pair) Assertions.checkNotNull(WidevineUtil.getLicenseDurationRemainingSec(drmSession)));
        } catch (Throwable e) {
            try {
                licenseDurationRemainingSec.setException(e);
            } finally {
                drmSession.release(this.eventDispatcher);
            }
        }
    }

    public void release() {
        this.handlerThread.quit();
    }

    private byte[] acquireSessionAndGetOfflineLicenseKeySetIdOnHandlerThread(int licenseMode, byte[] offlineLicenseKeySetId, Format format) throws DrmSession.DrmSessionException {
        final DrmSession drmSession = acquireFirstSessionOnHandlerThread(licenseMode, offlineLicenseKeySetId, format);
        final SettableFuture<byte[]> keySetId = SettableFuture.create();
        this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.drm.OfflineLicenseHelper$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m91x4ec00c9e(keySetId, drmSession);
            }
        });
        try {
            try {
                byte[] bArr = (byte[]) Assertions.checkNotNull(keySetId.get());
                releaseManagerOnHandlerThread();
                return bArr;
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        } catch (Throwable th) {
            releaseManagerOnHandlerThread();
            throw th;
        }
    }

    /* JADX INFO: renamed from: lambda$acquireSessionAndGetOfflineLicenseKeySetIdOnHandlerThread$1$androidx-media3-exoplayer-drm-OfflineLicenseHelper, reason: not valid java name */
    /* synthetic */ void m91x4ec00c9e(SettableFuture keySetId, DrmSession drmSession) {
        try {
            keySetId.set(drmSession.getOfflineLicenseKeySetId());
        } catch (Throwable e) {
            try {
                keySetId.setException(e);
            } finally {
                drmSession.release(this.eventDispatcher);
            }
        }
    }

    private DrmSession acquireFirstSessionOnHandlerThread(final int licenseMode, final byte[] offlineLicenseKeySetId, final Format format) throws DrmSession.DrmSessionException {
        Assertions.checkNotNull(format.drmInitData);
        final SettableFuture<DrmSession> drmSessionFuture = SettableFuture.create();
        this.drmListenerConditionVariable.close();
        this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.drm.OfflineLicenseHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m89xf4292904(licenseMode, offlineLicenseKeySetId, drmSessionFuture, format);
            }
        });
        try {
            final DrmSession drmSession = drmSessionFuture.get();
            this.drmListenerConditionVariable.block();
            final SettableFuture<DrmSession.DrmSessionException> drmSessionError = SettableFuture.create();
            this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.drm.OfflineLicenseHelper$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m90x2201c363(drmSession, drmSessionError);
                }
            });
            try {
                if (drmSessionError.get() != null) {
                    throw drmSessionError.get();
                }
                return drmSession;
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        } catch (InterruptedException | ExecutionException e2) {
            throw new IllegalStateException(e2);
        }
    }

    /* JADX INFO: renamed from: lambda$acquireFirstSessionOnHandlerThread$2$androidx-media3-exoplayer-drm-OfflineLicenseHelper, reason: not valid java name */
    /* synthetic */ void m89xf4292904(int licenseMode, byte[] offlineLicenseKeySetId, SettableFuture drmSessionFuture, Format format) {
        try {
            this.drmSessionManager.setPlayer((Looper) Assertions.checkNotNull(Looper.myLooper()), PlayerId.UNSET);
            this.drmSessionManager.prepare();
            try {
                this.drmSessionManager.setMode(licenseMode, offlineLicenseKeySetId);
                drmSessionFuture.set((DrmSession) Assertions.checkNotNull(this.drmSessionManager.acquireSession(this.eventDispatcher, format)));
            } catch (Throwable e) {
                this.drmSessionManager.release();
                throw e;
            }
        } catch (Throwable e2) {
            drmSessionFuture.setException(e2);
        }
    }

    /* JADX INFO: renamed from: lambda$acquireFirstSessionOnHandlerThread$3$androidx-media3-exoplayer-drm-OfflineLicenseHelper, reason: not valid java name */
    /* synthetic */ void m90x2201c363(DrmSession drmSession, SettableFuture drmSessionError) {
        try {
            DrmSession.DrmSessionException error = drmSession.getError();
            if (drmSession.getState() == 1) {
                drmSession.release(this.eventDispatcher);
                this.drmSessionManager.release();
            }
            drmSessionError.set(error);
        } catch (Throwable e) {
            drmSessionError.setException(e);
            drmSession.release(this.eventDispatcher);
            this.drmSessionManager.release();
        }
    }

    private void releaseManagerOnHandlerThread() {
        final SettableFuture<Void> result = SettableFuture.create();
        this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.drm.OfflineLicenseHelper$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m93xf255d686(result);
            }
        });
        try {
            result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    /* JADX INFO: renamed from: lambda$releaseManagerOnHandlerThread$4$androidx-media3-exoplayer-drm-OfflineLicenseHelper, reason: not valid java name */
    /* synthetic */ void m93xf255d686(SettableFuture result) {
        try {
            this.drmSessionManager.release();
            result.set(null);
        } catch (Throwable e) {
            result.setException(e);
        }
    }
}
