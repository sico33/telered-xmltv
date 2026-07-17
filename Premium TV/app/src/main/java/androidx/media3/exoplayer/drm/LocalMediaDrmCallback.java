package androidx.media3.exoplayer.drm;

import androidx.media3.common.util.Assertions;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public final class LocalMediaDrmCallback implements MediaDrmCallback {
    private final byte[] keyResponse;

    public LocalMediaDrmCallback(byte[] keyResponse) {
        this.keyResponse = (byte[]) Assertions.checkNotNull(keyResponse);
    }

    @Override // androidx.media3.exoplayer.drm.MediaDrmCallback
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override // androidx.media3.exoplayer.drm.MediaDrmCallback
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) {
        return this.keyResponse;
    }
}
