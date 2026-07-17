package androidx.media3.exoplayer.drm;

import android.media.ResourceBusyException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/* JADX INFO: loaded from: classes.dex */
public class DefaultDrmSessionManager implements DrmSessionManager {
    public static final long DEFAULT_SESSION_KEEPALIVE_MS = 300000;
    public static final int INITIAL_DRM_REQUEST_RETRY_COUNT = 3;
    public static final int MODE_DOWNLOAD = 2;
    public static final int MODE_PLAYBACK = 0;
    public static final int MODE_QUERY = 1;
    public static final int MODE_RELEASE = 3;
    public static final String PLAYREADY_CUSTOM_DATA_KEY = "PRCustomData";
    private static final String TAG = "DefaultDrmSessionMgr";
    private final MediaDrmCallback callback;
    private ExoMediaDrm exoMediaDrm;
    private final ExoMediaDrm.Provider exoMediaDrmProvider;
    private final Set<DefaultDrmSession> keepaliveSessions;
    private final HashMap<String, String> keyRequestParameters;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    volatile MediaDrmHandler mediaDrmHandler;
    private int mode;
    private final boolean multiSession;
    private DefaultDrmSession noMultiSessionDrmSession;
    private byte[] offlineLicenseKeySetId;
    private DefaultDrmSession placeholderDrmSession;
    private final boolean playClearSamplesWithoutKeys;
    private Handler playbackHandler;
    private Looper playbackLooper;
    private PlayerId playerId;
    private final Set<PreacquiredSessionReference> preacquiredSessionReferences;
    private int prepareCallsCount;
    private final ProvisioningManagerImpl provisioningManagerImpl;
    private final ReferenceCountListenerImpl referenceCountListener;
    private final long sessionKeepaliveMs;
    private final List<DefaultDrmSession> sessions;
    private final int[] useDrmSessionsForClearContentTrackTypes;
    private final UUID uuid;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    public static final class Builder {
        private boolean multiSession;
        private final HashMap<String, String> keyRequestParameters = new HashMap<>();
        private UUID uuid = C.WIDEVINE_UUID;
        private ExoMediaDrm.Provider exoMediaDrmProvider = FrameworkMediaDrm.DEFAULT_PROVIDER;
        private int[] useDrmSessionsForClearContentTrackTypes = new int[0];
        private boolean playClearSamplesWithoutKeys = true;
        private LoadErrorHandlingPolicy loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy();
        private long sessionKeepaliveMs = 300000;

        public Builder setKeyRequestParameters(Map<String, String> keyRequestParameters) {
            this.keyRequestParameters.clear();
            if (keyRequestParameters != null) {
                this.keyRequestParameters.putAll(keyRequestParameters);
            }
            return this;
        }

        public Builder setUuidAndExoMediaDrmProvider(UUID uuid, ExoMediaDrm.Provider exoMediaDrmProvider) {
            this.uuid = (UUID) Assertions.checkNotNull(uuid);
            this.exoMediaDrmProvider = (ExoMediaDrm.Provider) Assertions.checkNotNull(exoMediaDrmProvider);
            return this;
        }

        public Builder setMultiSession(boolean multiSession) {
            this.multiSession = multiSession;
            return this;
        }

        public Builder setUseDrmSessionsForClearContent(int... useDrmSessionsForClearContentTrackTypes) {
            for (int trackType : useDrmSessionsForClearContentTrackTypes) {
                boolean z = true;
                if (trackType != 2 && trackType != 1) {
                    z = false;
                }
                Assertions.checkArgument(z);
            }
            this.useDrmSessionsForClearContentTrackTypes = (int[]) useDrmSessionsForClearContentTrackTypes.clone();
            return this;
        }

        public Builder setPlayClearSamplesWithoutKeys(boolean playClearSamplesWithoutKeys) {
            this.playClearSamplesWithoutKeys = playClearSamplesWithoutKeys;
            return this;
        }

        public Builder setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
            this.loadErrorHandlingPolicy = (LoadErrorHandlingPolicy) Assertions.checkNotNull(loadErrorHandlingPolicy);
            return this;
        }

        public Builder setSessionKeepaliveMs(long sessionKeepaliveMs) {
            Assertions.checkArgument(sessionKeepaliveMs > 0 || sessionKeepaliveMs == C.TIME_UNSET);
            this.sessionKeepaliveMs = sessionKeepaliveMs;
            return this;
        }

        public DefaultDrmSessionManager build(MediaDrmCallback mediaDrmCallback) {
            return new DefaultDrmSessionManager(this.uuid, this.exoMediaDrmProvider, mediaDrmCallback, this.keyRequestParameters, this.multiSession, this.useDrmSessionsForClearContentTrackTypes, this.playClearSamplesWithoutKeys, this.loadErrorHandlingPolicy, this.sessionKeepaliveMs);
        }
    }

    public static final class MissingSchemeDataException extends Exception {
        private MissingSchemeDataException(UUID uuid) {
            super("Media does not support uuid: " + uuid);
        }
    }

    private DefaultDrmSessionManager(UUID uuid, ExoMediaDrm.Provider exoMediaDrmProvider, MediaDrmCallback callback, HashMap<String, String> keyRequestParameters, boolean multiSession, int[] useDrmSessionsForClearContentTrackTypes, boolean playClearSamplesWithoutKeys, LoadErrorHandlingPolicy loadErrorHandlingPolicy, long sessionKeepaliveMs) {
        Assertions.checkNotNull(uuid);
        Assertions.checkArgument(!C.COMMON_PSSH_UUID.equals(uuid), "Use C.CLEARKEY_UUID instead");
        this.uuid = uuid;
        this.exoMediaDrmProvider = exoMediaDrmProvider;
        this.callback = callback;
        this.keyRequestParameters = keyRequestParameters;
        this.multiSession = multiSession;
        this.useDrmSessionsForClearContentTrackTypes = useDrmSessionsForClearContentTrackTypes;
        this.playClearSamplesWithoutKeys = playClearSamplesWithoutKeys;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.provisioningManagerImpl = new ProvisioningManagerImpl();
        this.referenceCountListener = new ReferenceCountListenerImpl();
        this.mode = 0;
        this.sessions = new ArrayList();
        this.preacquiredSessionReferences = Sets.newIdentityHashSet();
        this.keepaliveSessions = Sets.newIdentityHashSet();
        this.sessionKeepaliveMs = sessionKeepaliveMs;
    }

    public void setMode(int mode, byte[] offlineLicenseKeySetId) {
        Assertions.checkState(this.sessions.isEmpty());
        if (mode == 1 || mode == 3) {
            Assertions.checkNotNull(offlineLicenseKeySetId);
        }
        this.mode = mode;
        this.offlineLicenseKeySetId = offlineLicenseKeySetId;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionManager
    public final void prepare() {
        verifyPlaybackThread(true);
        int i = this.prepareCallsCount;
        this.prepareCallsCount = i + 1;
        if (i != 0) {
            return;
        }
        if (this.exoMediaDrm == null) {
            this.exoMediaDrm = this.exoMediaDrmProvider.acquireExoMediaDrm(this.uuid);
            this.exoMediaDrm.setOnEventListener(new MediaDrmEventListener());
        } else if (this.sessionKeepaliveMs != C.TIME_UNSET) {
            for (int i2 = 0; i2 < this.sessions.size(); i2++) {
                this.sessions.get(i2).acquire(null);
            }
        }
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionManager
    public final void release() {
        verifyPlaybackThread(true);
        int i = this.prepareCallsCount - 1;
        this.prepareCallsCount = i;
        if (i != 0) {
            return;
        }
        if (this.sessionKeepaliveMs != C.TIME_UNSET) {
            List<DefaultDrmSession> sessions = new ArrayList<>(this.sessions);
            for (int i2 = 0; i2 < sessions.size(); i2++) {
                sessions.get(i2).release(null);
            }
        }
        releaseAllPreacquiredSessions();
        maybeReleaseMediaDrm();
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionManager
    public void setPlayer(Looper playbackLooper, PlayerId playerId) {
        initPlaybackLooper(playbackLooper);
        this.playerId = playerId;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionManager
    public DrmSessionManager.DrmSessionReference preacquireSession(DrmSessionEventListener.EventDispatcher eventDispatcher, Format format) {
        Assertions.checkState(this.prepareCallsCount > 0);
        Assertions.checkStateNotNull(this.playbackLooper);
        PreacquiredSessionReference preacquiredSessionReference = new PreacquiredSessionReference(eventDispatcher);
        preacquiredSessionReference.acquire(format);
        return preacquiredSessionReference;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionManager
    public DrmSession acquireSession(DrmSessionEventListener.EventDispatcher eventDispatcher, Format format) {
        verifyPlaybackThread(false);
        Assertions.checkState(this.prepareCallsCount > 0);
        Assertions.checkStateNotNull(this.playbackLooper);
        return acquireSession(this.playbackLooper, eventDispatcher, format, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DrmSession acquireSession(Looper playbackLooper, DrmSessionEventListener.EventDispatcher eventDispatcher, Format format, boolean shouldReleasePreacquiredSessionsBeforeRetrying) {
        DefaultDrmSession session;
        maybeCreateMediaDrmHandler(playbackLooper);
        if (format.drmInitData == null) {
            return maybeAcquirePlaceholderSession(MimeTypes.getTrackType(format.sampleMimeType), shouldReleasePreacquiredSessionsBeforeRetrying);
        }
        List<DrmInitData.SchemeData> schemeDatas = null;
        if (this.offlineLicenseKeySetId == null) {
            schemeDatas = getSchemeDatas((DrmInitData) Assertions.checkNotNull(format.drmInitData), this.uuid, false);
            if (schemeDatas.isEmpty()) {
                MissingSchemeDataException error = new MissingSchemeDataException(this.uuid);
                Log.e(TAG, "DRM error", error);
                if (eventDispatcher != null) {
                    eventDispatcher.drmSessionManagerError(error);
                }
                return new ErrorStateDrmSession(new DrmSession.DrmSessionException(error, PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR));
            }
        }
        if (!this.multiSession) {
            session = this.noMultiSessionDrmSession;
        } else {
            session = null;
            for (DefaultDrmSession existingSession : this.sessions) {
                if (Util.areEqual(existingSession.schemeDatas, schemeDatas)) {
                    session = existingSession;
                    break;
                }
            }
        }
        if (session == null) {
            session = createAndAcquireSessionWithRetry(schemeDatas, false, eventDispatcher, shouldReleasePreacquiredSessionsBeforeRetrying);
            if (!this.multiSession) {
                this.noMultiSessionDrmSession = session;
            }
            this.sessions.add(session);
        } else {
            session.acquire(eventDispatcher);
        }
        return session;
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionManager
    public int getCryptoType(Format format) {
        verifyPlaybackThread(false);
        int cryptoType = ((ExoMediaDrm) Assertions.checkNotNull(this.exoMediaDrm)).getCryptoType();
        if (format.drmInitData == null) {
            int trackType = MimeTypes.getTrackType(format.sampleMimeType);
            if (Util.linearSearch(this.useDrmSessionsForClearContentTrackTypes, trackType) == -1) {
                return 0;
            }
            return cryptoType;
        }
        if (canAcquireSession(format.drmInitData)) {
            return cryptoType;
        }
        return 1;
    }

    private DrmSession maybeAcquirePlaceholderSession(int trackType, boolean shouldReleasePreacquiredSessionsBeforeRetrying) {
        ExoMediaDrm exoMediaDrm = (ExoMediaDrm) Assertions.checkNotNull(this.exoMediaDrm);
        boolean avoidPlaceholderDrmSessions = exoMediaDrm.getCryptoType() == 2 && FrameworkCryptoConfig.WORKAROUND_DEVICE_NEEDS_KEYS_TO_CONFIGURE_CODEC;
        if (avoidPlaceholderDrmSessions || Util.linearSearch(this.useDrmSessionsForClearContentTrackTypes, trackType) == -1 || exoMediaDrm.getCryptoType() == 1) {
            return null;
        }
        if (this.placeholderDrmSession != null) {
            this.placeholderDrmSession.acquire(null);
        } else {
            DefaultDrmSession placeholderDrmSession = createAndAcquireSessionWithRetry(ImmutableList.of(), true, null, shouldReleasePreacquiredSessionsBeforeRetrying);
            this.sessions.add(placeholderDrmSession);
            this.placeholderDrmSession = placeholderDrmSession;
        }
        return this.placeholderDrmSession;
    }

    private boolean canAcquireSession(DrmInitData drmInitData) {
        if (this.offlineLicenseKeySetId != null) {
            return true;
        }
        List<DrmInitData.SchemeData> schemeDatas = getSchemeDatas(drmInitData, this.uuid, true);
        if (schemeDatas.isEmpty()) {
            if (drmInitData.schemeDataCount != 1 || !drmInitData.get(0).matches(C.COMMON_PSSH_UUID)) {
                return false;
            }
            Log.w(TAG, "DrmInitData only contains common PSSH SchemeData. Assuming support for: " + this.uuid);
        }
        String schemeType = drmInitData.schemeType;
        if (schemeType == null || C.CENC_TYPE_cenc.equals(schemeType)) {
            return true;
        }
        if (C.CENC_TYPE_cbcs.equals(schemeType)) {
            return Util.SDK_INT >= 25;
        }
        return (C.CENC_TYPE_cbc1.equals(schemeType) || C.CENC_TYPE_cens.equals(schemeType)) ? false : true;
    }

    @EnsuresNonNull({"this.playbackLooper", "this.playbackHandler"})
    private synchronized void initPlaybackLooper(Looper playbackLooper) {
        if (this.playbackLooper == null) {
            this.playbackLooper = playbackLooper;
            this.playbackHandler = new Handler(playbackLooper);
        } else {
            Assertions.checkState(this.playbackLooper == playbackLooper);
            Assertions.checkNotNull(this.playbackHandler);
        }
    }

    private void maybeCreateMediaDrmHandler(Looper playbackLooper) {
        if (this.mediaDrmHandler == null) {
            this.mediaDrmHandler = new MediaDrmHandler(playbackLooper);
        }
    }

    private DefaultDrmSession createAndAcquireSessionWithRetry(List<DrmInitData.SchemeData> schemeDatas, boolean isPlaceholderSession, DrmSessionEventListener.EventDispatcher eventDispatcher, boolean shouldReleasePreacquiredSessionsBeforeRetrying) {
        DefaultDrmSession session = createAndAcquireSession(schemeDatas, isPlaceholderSession, eventDispatcher);
        if (acquisitionFailedIndicatingResourceShortage(session) && !this.keepaliveSessions.isEmpty()) {
            releaseAllKeepaliveSessions();
            undoAcquisition(session, eventDispatcher);
            session = createAndAcquireSession(schemeDatas, isPlaceholderSession, eventDispatcher);
        }
        if (acquisitionFailedIndicatingResourceShortage(session) && shouldReleasePreacquiredSessionsBeforeRetrying && !this.preacquiredSessionReferences.isEmpty()) {
            releaseAllPreacquiredSessions();
            if (!this.keepaliveSessions.isEmpty()) {
                releaseAllKeepaliveSessions();
            }
            undoAcquisition(session, eventDispatcher);
            return createAndAcquireSession(schemeDatas, isPlaceholderSession, eventDispatcher);
        }
        return session;
    }

    private static boolean acquisitionFailedIndicatingResourceShortage(DrmSession session) {
        if (session.getState() != 1) {
            return false;
        }
        Throwable cause = ((DrmSession.DrmSessionException) Assertions.checkNotNull(session.getError())).getCause();
        return (cause instanceof ResourceBusyException) || DrmUtil.isFailureToConstructResourceBusyException(cause);
    }

    private void undoAcquisition(DrmSession session, DrmSessionEventListener.EventDispatcher eventDispatcher) {
        session.release(eventDispatcher);
        if (this.sessionKeepaliveMs != C.TIME_UNSET) {
            session.release(null);
        }
    }

    private void releaseAllKeepaliveSessions() {
        ImmutableSet<DefaultDrmSession> keepaliveSessions = ImmutableSet.copyOf((Collection) this.keepaliveSessions);
        UnmodifiableIterator<DefaultDrmSession> it = keepaliveSessions.iterator();
        while (it.hasNext()) {
            DefaultDrmSession keepaliveSession = it.next();
            keepaliveSession.release(null);
        }
    }

    private void releaseAllPreacquiredSessions() {
        ImmutableSet<PreacquiredSessionReference> preacquiredSessionReferences = ImmutableSet.copyOf((Collection) this.preacquiredSessionReferences);
        UnmodifiableIterator<PreacquiredSessionReference> it = preacquiredSessionReferences.iterator();
        while (it.hasNext()) {
            PreacquiredSessionReference preacquiredSessionReference = it.next();
            preacquiredSessionReference.release();
        }
    }

    private DefaultDrmSession createAndAcquireSession(List<DrmInitData.SchemeData> schemeDatas, boolean isPlaceholderSession, DrmSessionEventListener.EventDispatcher eventDispatcher) {
        Assertions.checkNotNull(this.exoMediaDrm);
        boolean playClearSamplesWithoutKeys = this.playClearSamplesWithoutKeys | isPlaceholderSession;
        DefaultDrmSession session = new DefaultDrmSession(this.uuid, this.exoMediaDrm, this.provisioningManagerImpl, this.referenceCountListener, schemeDatas, this.mode, playClearSamplesWithoutKeys, isPlaceholderSession, this.offlineLicenseKeySetId, this.keyRequestParameters, this.callback, (Looper) Assertions.checkNotNull(this.playbackLooper), this.loadErrorHandlingPolicy, (PlayerId) Assertions.checkNotNull(this.playerId));
        session.acquire(eventDispatcher);
        if (this.sessionKeepaliveMs != C.TIME_UNSET) {
            session.acquire(null);
        }
        return session;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeReleaseMediaDrm() {
        if (this.exoMediaDrm != null && this.prepareCallsCount == 0 && this.sessions.isEmpty() && this.preacquiredSessionReferences.isEmpty()) {
            ((ExoMediaDrm) Assertions.checkNotNull(this.exoMediaDrm)).release();
            this.exoMediaDrm = null;
        }
    }

    private void verifyPlaybackThread(boolean allowBeforeSetPlayer) {
        if (allowBeforeSetPlayer && this.playbackLooper == null) {
            Log.w(TAG, "DefaultDrmSessionManager accessed before setPlayer(), possibly on the wrong thread.", new IllegalStateException());
        } else if (Thread.currentThread() != ((Looper) Assertions.checkNotNull(this.playbackLooper)).getThread()) {
            Log.w(TAG, "DefaultDrmSessionManager accessed on the wrong thread.\nCurrent thread: " + Thread.currentThread().getName() + "\nExpected thread: " + this.playbackLooper.getThread().getName(), new IllegalStateException());
        }
    }

    private static List<DrmInitData.SchemeData> getSchemeDatas(DrmInitData drmInitData, UUID uuid, boolean allowMissingData) {
        List<DrmInitData.SchemeData> matchingSchemeDatas = new ArrayList<>(drmInitData.schemeDataCount);
        for (int i = 0; i < drmInitData.schemeDataCount; i++) {
            DrmInitData.SchemeData schemeData = drmInitData.get(i);
            boolean uuidMatches = schemeData.matches(uuid) || (C.CLEARKEY_UUID.equals(uuid) && schemeData.matches(C.COMMON_PSSH_UUID));
            if (uuidMatches && (schemeData.data != null || allowMissingData)) {
                matchingSchemeDatas.add(schemeData);
            }
        }
        return matchingSchemeDatas;
    }

    private class MediaDrmHandler extends Handler {
        public MediaDrmHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            byte[] sessionId = (byte[]) msg.obj;
            if (sessionId != null) {
                for (DefaultDrmSession session : DefaultDrmSessionManager.this.sessions) {
                    if (session.hasSessionId(sessionId)) {
                        session.onMediaDrmEvent(msg.what);
                        return;
                    }
                }
            }
        }
    }

    private class ProvisioningManagerImpl implements DefaultDrmSession.ProvisioningManager {
        private DefaultDrmSession provisioningSession;
        private final Set<DefaultDrmSession> sessionsAwaitingProvisioning = new HashSet();

        public ProvisioningManagerImpl() {
        }

        @Override // androidx.media3.exoplayer.drm.DefaultDrmSession.ProvisioningManager
        public void provisionRequired(DefaultDrmSession session) {
            this.sessionsAwaitingProvisioning.add(session);
            if (this.provisioningSession != null) {
                return;
            }
            this.provisioningSession = session;
            session.provision();
        }

        @Override // androidx.media3.exoplayer.drm.DefaultDrmSession.ProvisioningManager
        public void onProvisionCompleted() {
            this.provisioningSession = null;
            ImmutableList<DefaultDrmSession> sessionsToNotify = ImmutableList.copyOf((Collection) this.sessionsAwaitingProvisioning);
            this.sessionsAwaitingProvisioning.clear();
            UnmodifiableIterator<DefaultDrmSession> it = sessionsToNotify.iterator();
            while (it.hasNext()) {
                DefaultDrmSession session = it.next();
                session.onProvisionCompleted();
            }
        }

        @Override // androidx.media3.exoplayer.drm.DefaultDrmSession.ProvisioningManager
        public void onProvisionError(Exception error, boolean thrownByExoMediaDrm) {
            this.provisioningSession = null;
            ImmutableList<DefaultDrmSession> sessionsToNotify = ImmutableList.copyOf((Collection) this.sessionsAwaitingProvisioning);
            this.sessionsAwaitingProvisioning.clear();
            UnmodifiableIterator<DefaultDrmSession> it = sessionsToNotify.iterator();
            while (it.hasNext()) {
                DefaultDrmSession session = it.next();
                session.onProvisionError(error, thrownByExoMediaDrm);
            }
        }

        public void onSessionFullyReleased(DefaultDrmSession session) {
            this.sessionsAwaitingProvisioning.remove(session);
            if (this.provisioningSession == session) {
                this.provisioningSession = null;
                if (!this.sessionsAwaitingProvisioning.isEmpty()) {
                    this.provisioningSession = this.sessionsAwaitingProvisioning.iterator().next();
                    this.provisioningSession.provision();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    class ReferenceCountListenerImpl implements DefaultDrmSession.ReferenceCountListener {
        private ReferenceCountListenerImpl() {
        }

        @Override // androidx.media3.exoplayer.drm.DefaultDrmSession.ReferenceCountListener
        public void onReferenceCountIncremented(DefaultDrmSession session, int newReferenceCount) {
            if (DefaultDrmSessionManager.this.sessionKeepaliveMs != C.TIME_UNSET) {
                DefaultDrmSessionManager.this.keepaliveSessions.remove(session);
                ((Handler) Assertions.checkNotNull(DefaultDrmSessionManager.this.playbackHandler)).removeCallbacksAndMessages(session);
            }
        }

        @Override // androidx.media3.exoplayer.drm.DefaultDrmSession.ReferenceCountListener
        public void onReferenceCountDecremented(final DefaultDrmSession session, int newReferenceCount) {
            if (newReferenceCount == 1 && DefaultDrmSessionManager.this.prepareCallsCount > 0 && DefaultDrmSessionManager.this.sessionKeepaliveMs != C.TIME_UNSET) {
                DefaultDrmSessionManager.this.keepaliveSessions.add(session);
                ((Handler) Assertions.checkNotNull(DefaultDrmSessionManager.this.playbackHandler)).postAtTime(new Runnable() { // from class: androidx.media3.exoplayer.drm.DefaultDrmSessionManager$ReferenceCountListenerImpl$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        session.release(null);
                    }
                }, session, SystemClock.uptimeMillis() + DefaultDrmSessionManager.this.sessionKeepaliveMs);
            } else if (newReferenceCount == 0) {
                DefaultDrmSessionManager.this.sessions.remove(session);
                if (DefaultDrmSessionManager.this.placeholderDrmSession == session) {
                    DefaultDrmSessionManager.this.placeholderDrmSession = null;
                }
                if (DefaultDrmSessionManager.this.noMultiSessionDrmSession == session) {
                    DefaultDrmSessionManager.this.noMultiSessionDrmSession = null;
                }
                DefaultDrmSessionManager.this.provisioningManagerImpl.onSessionFullyReleased(session);
                if (DefaultDrmSessionManager.this.sessionKeepaliveMs != C.TIME_UNSET) {
                    ((Handler) Assertions.checkNotNull(DefaultDrmSessionManager.this.playbackHandler)).removeCallbacksAndMessages(session);
                    DefaultDrmSessionManager.this.keepaliveSessions.remove(session);
                }
            }
            DefaultDrmSessionManager.this.maybeReleaseMediaDrm();
        }
    }

    private class MediaDrmEventListener implements ExoMediaDrm.OnEventListener {
        private MediaDrmEventListener() {
        }

        @Override // androidx.media3.exoplayer.drm.ExoMediaDrm.OnEventListener
        public void onEvent(ExoMediaDrm md, byte[] sessionId, int event, int extra, byte[] data) {
            ((MediaDrmHandler) Assertions.checkNotNull(DefaultDrmSessionManager.this.mediaDrmHandler)).obtainMessage(event, sessionId).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    class PreacquiredSessionReference implements DrmSessionManager.DrmSessionReference {
        private final DrmSessionEventListener.EventDispatcher eventDispatcher;
        private boolean isReleased;
        private DrmSession session;

        public PreacquiredSessionReference(DrmSessionEventListener.EventDispatcher eventDispatcher) {
            this.eventDispatcher = eventDispatcher;
        }

        public void acquire(final Format format) {
            ((Handler) Assertions.checkNotNull(DefaultDrmSessionManager.this.playbackHandler)).post(new Runnable() { // from class: androidx.media3.exoplayer.drm.DefaultDrmSessionManager$PreacquiredSessionReference$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m78x937f548e(format);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$acquire$0$androidx-media3-exoplayer-drm-DefaultDrmSessionManager$PreacquiredSessionReference, reason: not valid java name */
        /* synthetic */ void m78x937f548e(Format format) {
            if (DefaultDrmSessionManager.this.prepareCallsCount != 0 && !this.isReleased) {
                this.session = DefaultDrmSessionManager.this.acquireSession((Looper) Assertions.checkNotNull(DefaultDrmSessionManager.this.playbackLooper), this.eventDispatcher, format, false);
                DefaultDrmSessionManager.this.preacquiredSessionReferences.add(this);
            }
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionManager.DrmSessionReference
        public void release() {
            Util.postOrRun((Handler) Assertions.checkNotNull(DefaultDrmSessionManager.this.playbackHandler), new Runnable() { // from class: androidx.media3.exoplayer.drm.DefaultDrmSessionManager$PreacquiredSessionReference$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m79xce10c5c();
                }
            });
        }

        /* JADX INFO: renamed from: lambda$release$1$androidx-media3-exoplayer-drm-DefaultDrmSessionManager$PreacquiredSessionReference, reason: not valid java name */
        /* synthetic */ void m79xce10c5c() {
            if (this.isReleased) {
                return;
            }
            if (this.session != null) {
                this.session.release(this.eventDispatcher);
            }
            DefaultDrmSessionManager.this.preacquiredSessionReferences.remove(this);
            this.isReleased = true;
        }
    }
}
