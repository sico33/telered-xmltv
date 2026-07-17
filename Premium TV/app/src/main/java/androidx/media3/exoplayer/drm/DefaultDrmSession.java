package androidx.media3.exoplayer.drm;

import android.media.NotProvisionedException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Consumer;
import androidx.media3.common.util.CopyOnWriteMultiset;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.CryptoConfig;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
class DefaultDrmSession implements DrmSession {
    private static final int MAX_LICENSE_DURATION_TO_RENEW_SECONDS = 60;
    private static final int MSG_KEYS = 2;
    private static final int MSG_PROVISION = 1;
    private static final String TAG = "DefaultDrmSession";
    private final MediaDrmCallback callback;
    private CryptoConfig cryptoConfig;
    private ExoMediaDrm.KeyRequest currentKeyRequest;
    private ExoMediaDrm.ProvisionRequest currentProvisionRequest;
    private final CopyOnWriteMultiset<DrmSessionEventListener.EventDispatcher> eventDispatchers;
    private final boolean isPlaceholderSession;
    private final HashMap<String, String> keyRequestParameters;
    private DrmSession.DrmSessionException lastException;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private final ExoMediaDrm mediaDrm;
    private final int mode;
    private byte[] offlineLicenseKeySetId;
    private final boolean playClearSamplesWithoutKeys;
    private final Looper playbackLooper;
    private final PlayerId playerId;
    private final ProvisioningManager provisioningManager;
    private int referenceCount;
    private final ReferenceCountListener referenceCountListener;
    private RequestHandler requestHandler;
    private HandlerThread requestHandlerThread;
    private final ResponseHandler responseHandler;
    public final List<DrmInitData.SchemeData> schemeDatas;
    private byte[] sessionId;
    private int state;
    private final UUID uuid;

    public interface ProvisioningManager {
        void onProvisionCompleted();

        void onProvisionError(Exception exc, boolean z);

        void provisionRequired(DefaultDrmSession defaultDrmSession);
    }

    public interface ReferenceCountListener {
        void onReferenceCountDecremented(DefaultDrmSession defaultDrmSession, int i);

        void onReferenceCountIncremented(DefaultDrmSession defaultDrmSession, int i);
    }

    public static final class UnexpectedDrmSessionException extends IOException {
        public UnexpectedDrmSessionException(Throwable cause) {
            super(cause);
        }
    }

    public DefaultDrmSession(UUID uuid, ExoMediaDrm mediaDrm, ProvisioningManager provisioningManager, ReferenceCountListener referenceCountListener, List<DrmInitData.SchemeData> schemeDatas, int mode, boolean playClearSamplesWithoutKeys, boolean isPlaceholderSession, byte[] offlineLicenseKeySetId, HashMap<String, String> keyRequestParameters, MediaDrmCallback callback, Looper playbackLooper, LoadErrorHandlingPolicy loadErrorHandlingPolicy, PlayerId playerId) {
        if (mode == 1 || mode == 3) {
            Assertions.checkNotNull(offlineLicenseKeySetId);
        }
        this.uuid = uuid;
        this.provisioningManager = provisioningManager;
        this.referenceCountListener = referenceCountListener;
        this.mediaDrm = mediaDrm;
        this.mode = mode;
        this.playClearSamplesWithoutKeys = playClearSamplesWithoutKeys;
        this.isPlaceholderSession = isPlaceholderSession;
        if (offlineLicenseKeySetId != null) {
            this.offlineLicenseKeySetId = offlineLicenseKeySetId;
            this.schemeDatas = null;
        } else {
            this.schemeDatas = Collections.unmodifiableList((List) Assertions.checkNotNull(schemeDatas));
        }
        this.keyRequestParameters = keyRequestParameters;
        this.callback = callback;
        this.eventDispatchers = new CopyOnWriteMultiset<>();
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.playerId = playerId;
        this.state = 2;
        this.playbackLooper = playbackLooper;
        this.responseHandler = new ResponseHandler(playbackLooper);
    }

    public boolean hasSessionId(byte[] sessionId) {
        verifyPlaybackThread();
        return Arrays.equals(this.sessionId, sessionId);
    }

    void onMediaDrmEvent(int what) {
        switch (what) {
            case 2:
                onKeysRequired();
                break;
        }
    }

    void provision() {
        this.currentProvisionRequest = this.mediaDrm.getProvisionRequest();
        ((RequestHandler) Util.castNonNull(this.requestHandler)).post(1, Assertions.checkNotNull(this.currentProvisionRequest), true);
    }

    void onProvisionCompleted() {
        if (openInternal()) {
            doLicense(true);
        }
    }

    void onProvisionError(Exception error, boolean thrownByExoMediaDrm) {
        int i;
        if (thrownByExoMediaDrm) {
            i = 1;
        } else {
            i = 3;
        }
        onError(error, i);
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public final int getState() {
        verifyPlaybackThread();
        return this.state;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public boolean playClearSamplesWithoutKeys() {
        verifyPlaybackThread();
        return this.playClearSamplesWithoutKeys;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public final DrmSession.DrmSessionException getError() {
        verifyPlaybackThread();
        if (this.state == 1) {
            return this.lastException;
        }
        return null;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public final UUID getSchemeUuid() {
        verifyPlaybackThread();
        return this.uuid;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public final CryptoConfig getCryptoConfig() {
        verifyPlaybackThread();
        return this.cryptoConfig;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public Map<String, String> queryKeyStatus() {
        verifyPlaybackThread();
        if (this.sessionId == null) {
            return null;
        }
        return this.mediaDrm.queryKeyStatus(this.sessionId);
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public byte[] getOfflineLicenseKeySetId() {
        verifyPlaybackThread();
        return this.offlineLicenseKeySetId;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public boolean requiresSecureDecoder(String mimeType) {
        verifyPlaybackThread();
        return this.mediaDrm.requiresSecureDecoder((byte[]) Assertions.checkStateNotNull(this.sessionId), mimeType);
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public void acquire(DrmSessionEventListener.EventDispatcher eventDispatcher) {
        verifyPlaybackThread();
        if (this.referenceCount < 0) {
            Log.e(TAG, "Session reference count less than zero: " + this.referenceCount);
            this.referenceCount = 0;
        }
        if (eventDispatcher != null) {
            this.eventDispatchers.add(eventDispatcher);
        }
        int i = this.referenceCount + 1;
        this.referenceCount = i;
        if (i == 1) {
            Assertions.checkState(this.state == 2);
            this.requestHandlerThread = new HandlerThread("ExoPlayer:DrmRequestHandler");
            this.requestHandlerThread.start();
            this.requestHandler = new RequestHandler(this.requestHandlerThread.getLooper());
            if (openInternal()) {
                doLicense(true);
            }
        } else if (eventDispatcher != null && isOpen() && this.eventDispatchers.count(eventDispatcher) == 1) {
            eventDispatcher.drmSessionAcquired(this.state);
        }
        this.referenceCountListener.onReferenceCountIncremented(this, this.referenceCount);
    }

    @Override // androidx.media3.exoplayer.drm.DrmSession
    public void release(DrmSessionEventListener.EventDispatcher eventDispatcher) {
        verifyPlaybackThread();
        if (this.referenceCount <= 0) {
            Log.e(TAG, "release() called on a session that's already fully released.");
            return;
        }
        int i = this.referenceCount - 1;
        this.referenceCount = i;
        if (i == 0) {
            this.state = 0;
            ((ResponseHandler) Util.castNonNull(this.responseHandler)).removeCallbacksAndMessages(null);
            ((RequestHandler) Util.castNonNull(this.requestHandler)).release();
            this.requestHandler = null;
            ((HandlerThread) Util.castNonNull(this.requestHandlerThread)).quit();
            this.requestHandlerThread = null;
            this.cryptoConfig = null;
            this.lastException = null;
            this.currentKeyRequest = null;
            this.currentProvisionRequest = null;
            if (this.sessionId != null) {
                this.mediaDrm.closeSession(this.sessionId);
                this.sessionId = null;
            }
        }
        if (eventDispatcher != null) {
            this.eventDispatchers.remove(eventDispatcher);
            if (this.eventDispatchers.count(eventDispatcher) == 0) {
                eventDispatcher.drmSessionReleased();
            }
        }
        this.referenceCountListener.onReferenceCountDecremented(this, this.referenceCount);
    }

    /* JADX WARN: Code duplicated, block: B:12:0x003f  */
    /* JADX WARN: Code duplicated, block: B:13:0x0045  */
    @EnsuresNonNullIf(expression = {"sessionId"}, result = true)
    private boolean openInternal() {
        if (isOpen()) {
            return true;
        }
        try {
            this.sessionId = this.mediaDrm.openSession();
            this.mediaDrm.setPlayerIdForSession(this.sessionId, this.playerId);
            this.cryptoConfig = this.mediaDrm.createCryptoConfig(this.sessionId);
            this.state = 3;
            final int localState = this.state;
            dispatchEvent(new Consumer() { // from class: androidx.media3.exoplayer.drm.DefaultDrmSession$$ExternalSyntheticLambda0
                @Override // androidx.media3.common.util.Consumer
                public final void accept(Object obj) {
                    ((DrmSessionEventListener.EventDispatcher) obj).drmSessionAcquired(localState);
                }
            });
            Assertions.checkNotNull(this.sessionId);
            return true;
        } catch (NotProvisionedException e) {
            this.provisioningManager.provisionRequired(this);
            return false;
        } catch (Exception e2) {
            e = e2;
            if (DrmUtil.isFailureToConstructNotProvisionedException(e)) {
                this.provisioningManager.provisionRequired(this);
                return false;
            }
            onError(e, 1);
            return false;
        } catch (NoSuchMethodError e3) {
            e = e3;
            if (DrmUtil.isFailureToConstructNotProvisionedException(e)) {
                this.provisioningManager.provisionRequired(this);
                return false;
            }
            onError(e, 1);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProvisionResponse(Object request, Object response) {
        if (request == this.currentProvisionRequest) {
            if (this.state != 2 && !isOpen()) {
                return;
            }
            this.currentProvisionRequest = null;
            if (response instanceof Exception) {
                this.provisioningManager.onProvisionError((Exception) response, false);
                return;
            }
            try {
                this.mediaDrm.provideProvisionResponse((byte[]) response);
                this.provisioningManager.onProvisionCompleted();
            } catch (Exception e) {
                this.provisioningManager.onProvisionError(e, true);
            }
        }
    }

    @RequiresNonNull({"sessionId"})
    private void doLicense(boolean allowRetry) {
        if (this.isPlaceholderSession) {
        }
        byte[] sessionId = (byte[]) Util.castNonNull(this.sessionId);
        switch (this.mode) {
            case 0:
            case 1:
                if (this.offlineLicenseKeySetId == null) {
                    postKeyRequest(sessionId, 1, allowRetry);
                } else if (this.state == 4 || restoreKeys()) {
                    long licenseDurationRemainingSec = getLicenseDurationRemainingSec();
                    if (this.mode == 0 && licenseDurationRemainingSec <= 60) {
                        Log.d(TAG, "Offline license has expired or will expire soon. Remaining seconds: " + licenseDurationRemainingSec);
                        postKeyRequest(sessionId, 2, allowRetry);
                    } else if (licenseDurationRemainingSec <= 0) {
                        onError(new KeysExpiredException(), 2);
                    } else {
                        this.state = 4;
                        dispatchEvent(new Consumer() { // from class: androidx.media3.exoplayer.drm.DefaultDrmSession$$ExternalSyntheticLambda4
                            @Override // androidx.media3.common.util.Consumer
                            public final void accept(Object obj) {
                                ((DrmSessionEventListener.EventDispatcher) obj).drmKeysRestored();
                            }
                        });
                    }
                }
                break;
            case 2:
                if (this.offlineLicenseKeySetId == null || restoreKeys()) {
                    postKeyRequest(sessionId, 2, allowRetry);
                }
                break;
            case 3:
                Assertions.checkNotNull(this.offlineLicenseKeySetId);
                Assertions.checkNotNull(this.sessionId);
                postKeyRequest(this.offlineLicenseKeySetId, 3, allowRetry);
                break;
        }
    }

    @RequiresNonNull({"sessionId", "offlineLicenseKeySetId"})
    private boolean restoreKeys() {
        try {
            this.mediaDrm.restoreKeys(this.sessionId, this.offlineLicenseKeySetId);
            return true;
        } catch (Exception | NoSuchMethodError e) {
            onError(e, 1);
            return false;
        }
    }

    private long getLicenseDurationRemainingSec() {
        if (!C.WIDEVINE_UUID.equals(this.uuid)) {
            return Long.MAX_VALUE;
        }
        Pair<Long, Long> pair = (Pair) Assertions.checkNotNull(WidevineUtil.getLicenseDurationRemainingSec(this));
        return Math.min(((Long) pair.first).longValue(), ((Long) pair.second).longValue());
    }

    private void postKeyRequest(byte[] scope, int type, boolean allowRetry) {
        try {
            this.currentKeyRequest = this.mediaDrm.getKeyRequest(scope, this.schemeDatas, type, this.keyRequestParameters);
            ((RequestHandler) Util.castNonNull(this.requestHandler)).post(2, Assertions.checkNotNull(this.currentKeyRequest), allowRetry);
        } catch (Exception | NoSuchMethodError e) {
            onKeysError(e, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onKeyResponse(Object request, Object response) {
        if (request != this.currentKeyRequest || !isOpen()) {
            return;
        }
        this.currentKeyRequest = null;
        if ((response instanceof Exception) || (response instanceof NoSuchMethodError)) {
            onKeysError((Throwable) response, false);
            return;
        }
        try {
            byte[] responseData = (byte[]) response;
            int i = this.mode;
            ExoMediaDrm exoMediaDrm = this.mediaDrm;
            if (i == 3) {
                exoMediaDrm.provideKeyResponse((byte[]) Util.castNonNull(this.offlineLicenseKeySetId), responseData);
                dispatchEvent(new Consumer() { // from class: androidx.media3.exoplayer.drm.DefaultDrmSession$$ExternalSyntheticLambda1
                    @Override // androidx.media3.common.util.Consumer
                    public final void accept(Object obj) {
                        ((DrmSessionEventListener.EventDispatcher) obj).drmKeysRemoved();
                    }
                });
                return;
            }
            byte[] keySetId = exoMediaDrm.provideKeyResponse(this.sessionId, responseData);
            if ((this.mode == 2 || (this.mode == 0 && this.offlineLicenseKeySetId != null)) && keySetId != null && keySetId.length != 0) {
                this.offlineLicenseKeySetId = keySetId;
            }
            this.state = 4;
            dispatchEvent(new Consumer() { // from class: androidx.media3.exoplayer.drm.DefaultDrmSession$$ExternalSyntheticLambda2
                @Override // androidx.media3.common.util.Consumer
                public final void accept(Object obj) {
                    ((DrmSessionEventListener.EventDispatcher) obj).drmKeysLoaded();
                }
            });
        } catch (Exception | NoSuchMethodError e) {
            onKeysError(e, true);
        }
    }

    private void onKeysRequired() {
        if (this.mode == 0 && this.state == 4) {
            Util.castNonNull(this.sessionId);
            doLicense(false);
        }
    }

    private void onKeysError(Throwable e, boolean thrownByExoMediaDrm) {
        int i;
        if ((e instanceof NotProvisionedException) || DrmUtil.isFailureToConstructNotProvisionedException(e)) {
            this.provisioningManager.provisionRequired(this);
            return;
        }
        if (thrownByExoMediaDrm) {
            i = 1;
        } else {
            i = 2;
        }
        onError(e, i);
    }

    private void onError(final Throwable e, int errorSource) {
        this.lastException = new DrmSession.DrmSessionException(e, DrmUtil.getErrorCodeForMediaDrmException(e, errorSource));
        Log.e(TAG, "DRM session error", e);
        if (e instanceof Exception) {
            dispatchEvent(new Consumer() { // from class: androidx.media3.exoplayer.drm.DefaultDrmSession$$ExternalSyntheticLambda3
                @Override // androidx.media3.common.util.Consumer
                public final void accept(Object obj) {
                    ((DrmSessionEventListener.EventDispatcher) obj).drmSessionManagerError((Exception) e);
                }
            });
        } else if (e instanceof Error) {
            if (!DrmUtil.isFailureToConstructResourceBusyException(e) && !DrmUtil.isFailureToConstructNotProvisionedException(e)) {
                throw ((Error) e);
            }
        } else {
            throw new IllegalStateException("Unexpected Throwable subclass", e);
        }
        if (this.state != 4) {
            this.state = 1;
        }
    }

    @EnsuresNonNullIf(expression = {"sessionId"}, result = true)
    private boolean isOpen() {
        return this.state == 3 || this.state == 4;
    }

    private void dispatchEvent(Consumer<DrmSessionEventListener.EventDispatcher> event) {
        for (DrmSessionEventListener.EventDispatcher eventDispatcher : this.eventDispatchers.elementSet()) {
            event.accept(eventDispatcher);
        }
    }

    private void verifyPlaybackThread() {
        if (Thread.currentThread() != this.playbackLooper.getThread()) {
            Log.w(TAG, "DefaultDrmSession accessed on the wrong thread.\nCurrent thread: " + Thread.currentThread().getName() + "\nExpected thread: " + this.playbackLooper.getThread().getName(), new IllegalStateException());
        }
    }

    private class ResponseHandler extends Handler {
        public ResponseHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            Pair<Object, Object> requestAndResponse = (Pair) msg.obj;
            Object request = requestAndResponse.first;
            Object response = requestAndResponse.second;
            switch (msg.what) {
                case 1:
                    DefaultDrmSession.this.onProvisionResponse(request, response);
                    break;
                case 2:
                    DefaultDrmSession.this.onKeyResponse(request, response);
                    break;
            }
        }
    }

    private class RequestHandler extends Handler {
        private boolean isReleased;

        public RequestHandler(Looper backgroundLooper) {
            super(backgroundLooper);
        }

        void post(int what, Object request, boolean allowRetry) {
            RequestTask requestTask = new RequestTask(LoadEventInfo.getNewId(), allowRetry, SystemClock.elapsedRealtime(), request);
            obtainMessage(what, requestTask).sendToTarget();
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            Object objExecuteProvisionRequest;
            RequestTask requestTask = (RequestTask) message.obj;
            try {
                switch (message.what) {
                    case 1:
                        objExecuteProvisionRequest = DefaultDrmSession.this.callback.executeProvisionRequest(DefaultDrmSession.this.uuid, (ExoMediaDrm.ProvisionRequest) requestTask.request);
                        break;
                    case 2:
                        objExecuteProvisionRequest = DefaultDrmSession.this.callback.executeKeyRequest(DefaultDrmSession.this.uuid, (ExoMediaDrm.KeyRequest) requestTask.request);
                        break;
                    default:
                        throw new RuntimeException();
                }
            } catch (MediaDrmCallbackException e) {
                boolean zMaybeRetryRequest = maybeRetryRequest(message, e);
                objExecuteProvisionRequest = e;
                if (zMaybeRetryRequest) {
                    return;
                }
            } catch (Exception e2) {
                Log.w(DefaultDrmSession.TAG, "Key/provisioning request produced an unexpected exception. Not retrying.", e2);
                objExecuteProvisionRequest = e2;
            }
            DefaultDrmSession.this.loadErrorHandlingPolicy.onLoadTaskConcluded(requestTask.taskId);
            synchronized (this) {
                if (!this.isReleased) {
                    DefaultDrmSession.this.responseHandler.obtainMessage(message.what, Pair.create(requestTask.request, objExecuteProvisionRequest)).sendToTarget();
                }
            }
        }

        private boolean maybeRetryRequest(Message originalMsg, MediaDrmCallbackException exception) {
            IOException loadErrorCause;
            RequestTask requestTask = (RequestTask) originalMsg.obj;
            if (!requestTask.allowRetry) {
                return false;
            }
            requestTask.errorCount++;
            if (requestTask.errorCount > DefaultDrmSession.this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(3)) {
                return false;
            }
            LoadEventInfo loadEventInfo = new LoadEventInfo(requestTask.taskId, exception.dataSpec, exception.uriAfterRedirects, exception.responseHeaders, SystemClock.elapsedRealtime(), SystemClock.elapsedRealtime() - requestTask.startTimeMs, exception.bytesLoaded);
            MediaLoadData mediaLoadData = new MediaLoadData(3);
            if (exception.getCause() instanceof IOException) {
                loadErrorCause = (IOException) exception.getCause();
            } else {
                loadErrorCause = new UnexpectedDrmSessionException(exception.getCause());
            }
            long retryDelayMs = DefaultDrmSession.this.loadErrorHandlingPolicy.getRetryDelayMsFor(new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, mediaLoadData, loadErrorCause, requestTask.errorCount));
            if (retryDelayMs == C.TIME_UNSET) {
                return false;
            }
            synchronized (this) {
                if (this.isReleased) {
                    return false;
                }
                sendMessageDelayed(Message.obtain(originalMsg), retryDelayMs);
                return true;
            }
        }

        public synchronized void release() {
            removeCallbacksAndMessages(null);
            this.isReleased = true;
        }
    }

    private static final class RequestTask {
        public final boolean allowRetry;
        public int errorCount;
        public final Object request;
        public final long startTimeMs;
        public final long taskId;

        public RequestTask(long taskId, boolean allowRetry, long startTimeMs, Object request) {
            this.taskId = taskId;
            this.allowRetry = allowRetry;
            this.startTimeMs = startTimeMs;
            this.request = request;
        }
    }
}
