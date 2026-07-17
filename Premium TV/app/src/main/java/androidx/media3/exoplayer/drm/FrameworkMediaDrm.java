package androidx.media3.exoplayer.drm;

import android.media.DeniedByServerException;
import android.media.MediaCrypto;
import android.media.MediaCryptoException;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.media.NotProvisionedException;
import android.media.UnsupportedSchemeException;
import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.PersistableBundle;
import android.text.TextUtils;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.extractor.mp4.PsshAtomUtil;
import com.google.common.base.Charsets;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public final class FrameworkMediaDrm implements ExoMediaDrm {
    private static final String CENC_SCHEME_MIME_TYPE = "cenc";
    public static final ExoMediaDrm.Provider DEFAULT_PROVIDER = new ExoMediaDrm.Provider() { // from class: androidx.media3.exoplayer.drm.FrameworkMediaDrm$$ExternalSyntheticLambda0
        @Override // androidx.media3.exoplayer.drm.ExoMediaDrm.Provider
        public final ExoMediaDrm acquireExoMediaDrm(UUID uuid) {
            return FrameworkMediaDrm.lambda$static$0(uuid);
        }
    };
    private static final String MOCK_LA_URL = "<LA_URL>https://x</LA_URL>";
    private static final String MOCK_LA_URL_VALUE = "https://x";
    private static final String TAG = "FrameworkMediaDrm";
    private static final int UTF_16_BYTES_PER_CHARACTER = 2;
    private final MediaDrm mediaDrm;
    private int referenceCount;
    private final UUID uuid;

    static /* synthetic */ ExoMediaDrm lambda$static$0(UUID uuid) {
        try {
            return newInstance(uuid);
        } catch (UnsupportedDrmException e) {
            Log.e(TAG, "Failed to instantiate a FrameworkMediaDrm for uuid: " + uuid + ".");
            return new DummyExoMediaDrm();
        }
    }

    public static boolean isCryptoSchemeSupported(UUID uuid) {
        return MediaDrm.isCryptoSchemeSupported(adjustUuid(uuid));
    }

    public static FrameworkMediaDrm newInstance(UUID uuid) throws UnsupportedDrmException {
        try {
            return new FrameworkMediaDrm(uuid);
        } catch (UnsupportedSchemeException e) {
            throw new UnsupportedDrmException(1, e);
        } catch (Exception e2) {
            throw new UnsupportedDrmException(2, e2);
        }
    }

    private FrameworkMediaDrm(UUID uuid) throws UnsupportedSchemeException {
        Assertions.checkNotNull(uuid);
        Assertions.checkArgument(!C.COMMON_PSSH_UUID.equals(uuid), "Use C.CLEARKEY_UUID instead");
        this.uuid = uuid;
        this.mediaDrm = new MediaDrm(adjustUuid(uuid));
        this.referenceCount = 1;
        if (C.WIDEVINE_UUID.equals(uuid) && needsForceWidevineL3Workaround()) {
            forceWidevineL3(this.mediaDrm);
        }
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setOnEventListener(final ExoMediaDrm.OnEventListener listener) {
        MediaDrm.OnEventListener onEventListener;
        MediaDrm mediaDrm = this.mediaDrm;
        if (listener == null) {
            onEventListener = null;
        } else {
            onEventListener = new MediaDrm.OnEventListener() { // from class: androidx.media3.exoplayer.drm.FrameworkMediaDrm$$ExternalSyntheticLambda2
                @Override // android.media.MediaDrm.OnEventListener
                public final void onEvent(MediaDrm mediaDrm2, byte[] bArr, int i, int i2, byte[] bArr2) {
                    this.f$0.m86x5e84e274(listener, mediaDrm2, bArr, i, i2, bArr2);
                }
            };
        }
        mediaDrm.setOnEventListener(onEventListener);
    }

    /* JADX INFO: renamed from: lambda$setOnEventListener$1$androidx-media3-exoplayer-drm-FrameworkMediaDrm, reason: not valid java name */
    /* synthetic */ void m86x5e84e274(ExoMediaDrm.OnEventListener listener, MediaDrm mediaDrm, byte[] sessionId, int event, int extra, byte[] data) {
        listener.onEvent(this, sessionId, event, extra, data);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setOnKeyStatusChangeListener(final ExoMediaDrm.OnKeyStatusChangeListener listener) {
        MediaDrm.OnKeyStatusChangeListener onKeyStatusChangeListener;
        if (Util.SDK_INT < 23) {
            throw new UnsupportedOperationException();
        }
        MediaDrm mediaDrm = this.mediaDrm;
        if (listener == null) {
            onKeyStatusChangeListener = null;
        } else {
            onKeyStatusChangeListener = new MediaDrm.OnKeyStatusChangeListener() { // from class: androidx.media3.exoplayer.drm.FrameworkMediaDrm$$ExternalSyntheticLambda3
                @Override // android.media.MediaDrm.OnKeyStatusChangeListener
                public final void onKeyStatusChange(MediaDrm mediaDrm2, byte[] bArr, List list, boolean z) {
                    this.f$0.m88xc78bb65c(listener, mediaDrm2, bArr, list, z);
                }
            };
        }
        mediaDrm.setOnKeyStatusChangeListener(onKeyStatusChangeListener, (Handler) null);
    }

    /* JADX INFO: renamed from: lambda$setOnKeyStatusChangeListener$2$androidx-media3-exoplayer-drm-FrameworkMediaDrm, reason: not valid java name */
    /* synthetic */ void m88xc78bb65c(ExoMediaDrm.OnKeyStatusChangeListener listener, MediaDrm mediaDrm, byte[] sessionId, List keyInfo, boolean hasNewUsableKey) {
        List<ExoMediaDrm.KeyStatus> exoKeyInfo = new ArrayList<>();
        Iterator it = keyInfo.iterator();
        while (it.hasNext()) {
            MediaDrm.KeyStatus keyStatus = (MediaDrm.KeyStatus) it.next();
            exoKeyInfo.add(new ExoMediaDrm.KeyStatus(keyStatus.getStatusCode(), keyStatus.getKeyId()));
        }
        listener.onKeyStatusChange(this, sessionId, exoKeyInfo, hasNewUsableKey);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setOnExpirationUpdateListener(final ExoMediaDrm.OnExpirationUpdateListener listener) {
        MediaDrm.OnExpirationUpdateListener onExpirationUpdateListener;
        if (Util.SDK_INT < 23) {
            throw new UnsupportedOperationException();
        }
        MediaDrm mediaDrm = this.mediaDrm;
        if (listener == null) {
            onExpirationUpdateListener = null;
        } else {
            onExpirationUpdateListener = new MediaDrm.OnExpirationUpdateListener() { // from class: androidx.media3.exoplayer.drm.FrameworkMediaDrm$$ExternalSyntheticLambda1
                @Override // android.media.MediaDrm.OnExpirationUpdateListener
                public final void onExpirationUpdate(MediaDrm mediaDrm2, byte[] bArr, long j) {
                    this.f$0.m87x3bcdcffc(listener, mediaDrm2, bArr, j);
                }
            };
        }
        mediaDrm.setOnExpirationUpdateListener(onExpirationUpdateListener, (Handler) null);
    }

    /* JADX INFO: renamed from: lambda$setOnExpirationUpdateListener$3$androidx-media3-exoplayer-drm-FrameworkMediaDrm, reason: not valid java name */
    /* synthetic */ void m87x3bcdcffc(ExoMediaDrm.OnExpirationUpdateListener listener, MediaDrm mediaDrm, byte[] sessionId, long expirationTimeMs) {
        listener.onExpirationUpdate(this, sessionId, expirationTimeMs);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public byte[] openSession() throws MediaDrmException {
        return this.mediaDrm.openSession();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void closeSession(byte[] sessionId) {
        this.mediaDrm.closeSession(sessionId);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setPlayerIdForSession(byte[] sessionId, PlayerId playerId) {
        if (Util.SDK_INT >= 31) {
            try {
                Api31.setLogSessionIdOnMediaDrmSession(this.mediaDrm, sessionId, playerId);
            } catch (UnsupportedOperationException e) {
                Log.w(TAG, "setLogSessionId failed.");
            }
        }
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public ExoMediaDrm.KeyRequest getKeyRequest(byte[] scope, List<DrmInitData.SchemeData> schemeDatas, int keyType, HashMap<String, String> optionalParameters) throws NotProvisionedException {
        byte[] initData;
        String mimeType;
        DrmInitData.SchemeData schemeData = null;
        if (schemeDatas == null) {
            initData = null;
            mimeType = null;
        } else {
            schemeData = getSchemeData(this.uuid, schemeDatas);
            byte[] initData2 = adjustRequestInitData(this.uuid, (byte[]) Assertions.checkNotNull(schemeData.data));
            String mimeType2 = adjustRequestMimeType(this.uuid, schemeData.mimeType);
            initData = initData2;
            mimeType = mimeType2;
        }
        MediaDrm.KeyRequest request = this.mediaDrm.getKeyRequest(scope, initData, mimeType, keyType, optionalParameters);
        byte[] requestData = adjustRequestData(this.uuid, request.getData());
        String licenseServerUrl = adjustLicenseServerUrl(request.getDefaultUrl());
        if (TextUtils.isEmpty(licenseServerUrl) && schemeData != null && !TextUtils.isEmpty(schemeData.licenseServerUrl)) {
            licenseServerUrl = schemeData.licenseServerUrl;
        }
        int requestType = Util.SDK_INT >= 23 ? request.getRequestType() : Integer.MIN_VALUE;
        return new ExoMediaDrm.KeyRequest(requestData, licenseServerUrl, requestType);
    }

    private String adjustLicenseServerUrl(String licenseServerUrl) {
        if (MOCK_LA_URL.equals(licenseServerUrl)) {
            return "";
        }
        if (Util.SDK_INT >= 33 && "https://default.url".equals(licenseServerUrl)) {
            String pluginVersion = getPropertyString("version");
            if (Objects.equals(pluginVersion, "1.2") || Objects.equals(pluginVersion, "aidl-1")) {
                return "";
            }
        }
        return licenseServerUrl;
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public byte[] provideKeyResponse(byte[] scope, byte[] response) throws DeniedByServerException, NotProvisionedException {
        if (C.CLEARKEY_UUID.equals(this.uuid)) {
            response = ClearKeyUtil.adjustResponseData(response);
        }
        return this.mediaDrm.provideKeyResponse(scope, response);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public ExoMediaDrm.ProvisionRequest getProvisionRequest() {
        MediaDrm.ProvisionRequest request = this.mediaDrm.getProvisionRequest();
        return new ExoMediaDrm.ProvisionRequest(request.getData(), request.getDefaultUrl());
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void provideProvisionResponse(byte[] response) throws DeniedByServerException {
        this.mediaDrm.provideProvisionResponse(response);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public Map<String, String> queryKeyStatus(byte[] sessionId) {
        return this.mediaDrm.queryKeyStatus(sessionId);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public boolean requiresSecureDecoder(byte[] sessionId, String mimeType) {
        boolean result;
        boolean result2;
        if (Util.SDK_INT >= 31 && isMediaDrmRequiresSecureDecoderImplemented()) {
            result2 = Api31.requiresSecureDecoder(this.mediaDrm, mimeType);
        } else {
            MediaCrypto mediaCrypto = null;
            try {
                mediaCrypto = new MediaCrypto(this.uuid, sessionId);
                result = mediaCrypto.requiresSecureDecoderComponent(mimeType);
            } catch (MediaCryptoException e) {
                result = true;
                if (mediaCrypto != null) {
                }
                result2 = result;
                if (result2) {
                }
            } catch (Throwable th) {
                if (mediaCrypto != null) {
                    mediaCrypto.release();
                }
                throw th;
            }
            mediaCrypto.release();
            result2 = result;
        }
        return (result2 || shouldForceAllowInsecureDecoderComponents()) ? false : true;
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public synchronized void acquire() {
        Assertions.checkState(this.referenceCount > 0);
        this.referenceCount++;
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public synchronized void release() {
        int i = this.referenceCount - 1;
        this.referenceCount = i;
        if (i == 0) {
            this.mediaDrm.release();
        }
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void restoreKeys(byte[] sessionId, byte[] keySetId) {
        this.mediaDrm.restoreKeys(sessionId, keySetId);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void removeOfflineLicense(byte[] keySetId) {
        if (Util.SDK_INT < 29) {
            throw new UnsupportedOperationException();
        }
        this.mediaDrm.removeOfflineLicense(keySetId);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public List<byte[]> getOfflineLicenseKeySetIds() {
        if (Util.SDK_INT < 29) {
            throw new UnsupportedOperationException();
        }
        return this.mediaDrm.getOfflineLicenseKeySetIds();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public PersistableBundle getMetrics() {
        if (Util.SDK_INT < 28) {
            return null;
        }
        return this.mediaDrm.getMetrics();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public String getPropertyString(String propertyName) {
        return this.mediaDrm.getPropertyString(propertyName);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public byte[] getPropertyByteArray(String propertyName) {
        return this.mediaDrm.getPropertyByteArray(propertyName);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setPropertyString(String propertyName, String value) {
        this.mediaDrm.setPropertyString(propertyName, value);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setPropertyByteArray(String propertyName, byte[] value) {
        this.mediaDrm.setPropertyByteArray(propertyName, value);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public FrameworkCryptoConfig createCryptoConfig(byte[] sessionId) throws MediaCryptoException {
        boolean forceAllowInsecureDecoderComponents = shouldForceAllowInsecureDecoderComponents();
        return new FrameworkCryptoConfig(adjustUuid(this.uuid), sessionId, forceAllowInsecureDecoderComponents);
    }

    private boolean shouldForceAllowInsecureDecoderComponents() {
        return Util.SDK_INT < 21 && C.WIDEVINE_UUID.equals(this.uuid) && "L3".equals(getPropertyString("securityLevel"));
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public int getCryptoType() {
        return 2;
    }

    private boolean isMediaDrmRequiresSecureDecoderImplemented() {
        if (this.uuid.equals(C.WIDEVINE_UUID)) {
            String pluginVersion = getPropertyString("version");
            return (pluginVersion.startsWith("v5.") || pluginVersion.startsWith("14.") || pluginVersion.startsWith("15.") || pluginVersion.startsWith("16.0")) ? false : true;
        }
        return this.uuid.equals(C.CLEARKEY_UUID);
    }

    private static DrmInitData.SchemeData getSchemeData(UUID uuid, List<DrmInitData.SchemeData> schemeDatas) {
        if (!C.WIDEVINE_UUID.equals(uuid)) {
            return schemeDatas.get(0);
        }
        if (Util.SDK_INT >= 28 && schemeDatas.size() > 1) {
            DrmInitData.SchemeData firstSchemeData = schemeDatas.get(0);
            int concatenatedDataLength = 0;
            boolean canConcatenateData = true;
            for (int i = 0; i < schemeDatas.size(); i++) {
                DrmInitData.SchemeData schemeData = schemeDatas.get(i);
                byte[] schemeDataData = (byte[]) Assertions.checkNotNull(schemeData.data);
                if (Util.areEqual(schemeData.mimeType, firstSchemeData.mimeType) && Util.areEqual(schemeData.licenseServerUrl, firstSchemeData.licenseServerUrl) && PsshAtomUtil.isPsshAtom(schemeDataData)) {
                    concatenatedDataLength += schemeDataData.length;
                } else {
                    canConcatenateData = false;
                    break;
                }
            }
            if (canConcatenateData) {
                byte[] concatenatedData = new byte[concatenatedDataLength];
                int concatenatedDataPosition = 0;
                for (int i2 = 0; i2 < schemeDatas.size(); i2++) {
                    byte[] schemeDataData2 = (byte[]) Assertions.checkNotNull(schemeDatas.get(i2).data);
                    int schemeDataLength = schemeDataData2.length;
                    System.arraycopy(schemeDataData2, 0, concatenatedData, concatenatedDataPosition, schemeDataLength);
                    concatenatedDataPosition += schemeDataLength;
                }
                return firstSchemeData.copyWithData(concatenatedData);
            }
        }
        for (int i3 = 0; i3 < schemeDatas.size(); i3++) {
            DrmInitData.SchemeData schemeData2 = schemeDatas.get(i3);
            int version = PsshAtomUtil.parseVersion((byte[]) Assertions.checkNotNull(schemeData2.data));
            if (Util.SDK_INT < 23 && version == 0) {
                return schemeData2;
            }
            if (Util.SDK_INT >= 23 && version == 1) {
                return schemeData2;
            }
        }
        return schemeDatas.get(0);
    }

    private static UUID adjustUuid(UUID uuid) {
        return (Util.SDK_INT >= 27 || !C.CLEARKEY_UUID.equals(uuid)) ? uuid : C.COMMON_PSSH_UUID;
    }

    private static byte[] adjustRequestInitData(UUID uuid, byte[] initData) {
        byte[] psshData;
        if (C.PLAYREADY_UUID.equals(uuid)) {
            byte[] schemeSpecificData = PsshAtomUtil.parseSchemeSpecificData(initData, uuid);
            if (schemeSpecificData == null) {
                schemeSpecificData = initData;
            }
            initData = PsshAtomUtil.buildPsshAtom(C.PLAYREADY_UUID, addLaUrlAttributeIfMissing(schemeSpecificData));
        }
        if (((Util.SDK_INT < 23 && C.WIDEVINE_UUID.equals(uuid)) || (C.PLAYREADY_UUID.equals(uuid) && "Amazon".equals(Util.MANUFACTURER) && ("AFTB".equals(Util.MODEL) || "AFTS".equals(Util.MODEL) || "AFTM".equals(Util.MODEL) || "AFTT".equals(Util.MODEL)))) && (psshData = PsshAtomUtil.parseSchemeSpecificData(initData, uuid)) != null) {
            return psshData;
        }
        return initData;
    }

    private static String adjustRequestMimeType(UUID uuid, String mimeType) {
        if (Util.SDK_INT < 26 && C.CLEARKEY_UUID.equals(uuid) && (MimeTypes.VIDEO_MP4.equals(mimeType) || MimeTypes.AUDIO_MP4.equals(mimeType))) {
            return "cenc";
        }
        return mimeType;
    }

    private static byte[] adjustRequestData(UUID uuid, byte[] requestData) {
        if (C.CLEARKEY_UUID.equals(uuid)) {
            return ClearKeyUtil.adjustRequestData(requestData);
        }
        return requestData;
    }

    private static void forceWidevineL3(MediaDrm mediaDrm) {
        mediaDrm.setPropertyString("securityLevel", "L3");
    }

    private static boolean needsForceWidevineL3Workaround() {
        return "ASUS_Z00AD".equals(Util.MODEL);
    }

    private static byte[] addLaUrlAttributeIfMissing(byte[] data) {
        ParsableByteArray byteArray = new ParsableByteArray(data);
        int length = byteArray.readLittleEndianInt();
        int objectRecordCount = byteArray.readLittleEndianShort();
        int recordType = byteArray.readLittleEndianShort();
        if (objectRecordCount != 1 || recordType != 1) {
            Log.i(TAG, "Unexpected record count or type. Skipping LA_URL workaround.");
            return data;
        }
        int recordLength = byteArray.readLittleEndianShort();
        String xml = byteArray.readString(recordLength, Charsets.UTF_16LE);
        if (xml.contains("<LA_URL>")) {
            return data;
        }
        int endOfDataTagIndex = xml.indexOf("</DATA>");
        if (endOfDataTagIndex == -1) {
            Log.w(TAG, "Could not find the </DATA> tag. Skipping LA_URL workaround.");
        }
        String xmlWithMockLaUrl = xml.substring(0, endOfDataTagIndex) + MOCK_LA_URL + xml.substring(endOfDataTagIndex);
        int extraBytes = MOCK_LA_URL.length() * 2;
        ByteBuffer newData = ByteBuffer.allocate(length + extraBytes);
        newData.order(ByteOrder.LITTLE_ENDIAN);
        newData.putInt(length + extraBytes);
        newData.putShort((short) objectRecordCount);
        newData.putShort((short) recordType);
        newData.putShort((short) (xmlWithMockLaUrl.length() * 2));
        newData.put(xmlWithMockLaUrl.getBytes(Charsets.UTF_16LE));
        return newData.array();
    }

    private static class Api31 {
        private Api31() {
        }

        public static boolean requiresSecureDecoder(MediaDrm mediaDrm, String mimeType) {
            return mediaDrm.requiresSecureDecoder(mimeType);
        }

        public static void setLogSessionIdOnMediaDrmSession(MediaDrm mediaDrm, byte[] drmSessionId, PlayerId playerId) {
            LogSessionId logSessionId = playerId.getLogSessionId();
            if (!logSessionId.equals(LogSessionId.LOG_SESSION_ID_NONE)) {
                MediaDrm.PlaybackComponent playbackComponent = (MediaDrm.PlaybackComponent) Assertions.checkNotNull(mediaDrm.getPlaybackComponent(drmSessionId));
                playbackComponent.setLogSessionId(logSessionId);
            }
        }
    }
}
