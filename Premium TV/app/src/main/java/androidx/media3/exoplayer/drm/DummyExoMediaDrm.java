package androidx.media3.exoplayer.drm;

import android.media.MediaDrmException;
import android.os.PersistableBundle;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.CryptoConfig;
import androidx.media3.exoplayer.analytics.PlayerId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class DummyExoMediaDrm implements ExoMediaDrm {
    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public /* synthetic */ List getOfflineLicenseKeySetIds() {
        return ExoMediaDrm.CC.$default$getOfflineLicenseKeySetIds(this);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public /* synthetic */ void removeOfflineLicense(byte[] bArr) {
        ExoMediaDrm.CC.$default$removeOfflineLicense(this, bArr);
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public /* synthetic */ void setPlayerIdForSession(byte[] bArr, PlayerId playerId) {
        ExoMediaDrm.CC.$default$setPlayerIdForSession(this, bArr, playerId);
    }

    public static DummyExoMediaDrm getInstance() {
        return new DummyExoMediaDrm();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setOnEventListener(ExoMediaDrm.OnEventListener listener) {
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setOnKeyStatusChangeListener(ExoMediaDrm.OnKeyStatusChangeListener listener) {
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setOnExpirationUpdateListener(ExoMediaDrm.OnExpirationUpdateListener listener) {
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public byte[] openSession() throws MediaDrmException {
        throw new MediaDrmException("Attempting to open a session using a dummy ExoMediaDrm.");
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void closeSession(byte[] sessionId) {
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public ExoMediaDrm.KeyRequest getKeyRequest(byte[] scope, List<DrmInitData.SchemeData> schemeDatas, int keyType, HashMap<String, String> optionalParameters) {
        throw new IllegalStateException();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public byte[] provideKeyResponse(byte[] scope, byte[] response) {
        throw new IllegalStateException();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public ExoMediaDrm.ProvisionRequest getProvisionRequest() {
        throw new IllegalStateException();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void provideProvisionResponse(byte[] response) {
        throw new IllegalStateException();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public Map<String, String> queryKeyStatus(byte[] sessionId) {
        throw new IllegalStateException();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public boolean requiresSecureDecoder(byte[] sessionId, String mimeType) {
        throw new IllegalStateException();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void acquire() {
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void release() {
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void restoreKeys(byte[] sessionId, byte[] keySetId) {
        throw new IllegalStateException();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public PersistableBundle getMetrics() {
        return null;
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public String getPropertyString(String propertyName) {
        return "";
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public byte[] getPropertyByteArray(String propertyName) {
        return Util.EMPTY_BYTE_ARRAY;
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setPropertyString(String propertyName, String value) {
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public void setPropertyByteArray(String propertyName, byte[] value) {
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public CryptoConfig createCryptoConfig(byte[] sessionId) {
        throw new IllegalStateException();
    }

    @Override // androidx.media3.exoplayer.drm.ExoMediaDrm
    public int getCryptoType() {
        return 1;
    }
}
