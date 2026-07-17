package androidx.media3.exoplayer.hls;

import android.net.Uri;
import androidx.media3.common.util.Assertions;
import java.util.LinkedHashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class FullSegmentEncryptionKeyCache {
    private final LinkedHashMap<Uri, byte[]> backingMap;

    public FullSegmentEncryptionKeyCache(final int maxSize) {
        this.backingMap = new LinkedHashMap<Uri, byte[]>(maxSize + 1, 1.0f, false) { // from class: androidx.media3.exoplayer.hls.FullSegmentEncryptionKeyCache.1
            @Override // java.util.LinkedHashMap
            protected boolean removeEldestEntry(Map.Entry<Uri, byte[]> eldest) {
                return size() > maxSize;
            }
        };
    }

    public byte[] get(Uri uri) {
        if (uri == null) {
            return null;
        }
        return this.backingMap.get(uri);
    }

    public byte[] put(Uri uri, byte[] encryptionKey) {
        return this.backingMap.put((Uri) Assertions.checkNotNull(uri), (byte[]) Assertions.checkNotNull(encryptionKey));
    }

    public boolean containsUri(Uri uri) {
        return this.backingMap.containsKey(Assertions.checkNotNull(uri));
    }

    public byte[] remove(Uri uri) {
        return this.backingMap.remove(Assertions.checkNotNull(uri));
    }
}
