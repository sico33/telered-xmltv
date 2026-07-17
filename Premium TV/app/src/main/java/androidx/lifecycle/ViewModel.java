package androidx.lifecycle;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public abstract class ViewModel {
    private final Map<String, Object> mBagOfTags = new HashMap();
    private volatile boolean mCleared = false;

    protected void onCleared() {
    }

    final void clear() {
        this.mCleared = true;
        if (this.mBagOfTags != null) {
            synchronized (this.mBagOfTags) {
                for (Object value : this.mBagOfTags.values()) {
                    closeWithRuntimeException(value);
                }
            }
        }
        onCleared();
    }

    <T> T setTagIfAbsent(String str, T t) {
        Object obj;
        synchronized (this.mBagOfTags) {
            obj = this.mBagOfTags.get(str);
            if (obj == null) {
                this.mBagOfTags.put(str, t);
            }
        }
        T t2 = obj == null ? t : (T) obj;
        if (this.mCleared) {
            closeWithRuntimeException(t2);
        }
        return t2;
    }

    <T> T getTag(String str) {
        T t;
        if (this.mBagOfTags == null) {
            return null;
        }
        synchronized (this.mBagOfTags) {
            t = (T) this.mBagOfTags.get(str);
        }
        return t;
    }

    private static void closeWithRuntimeException(Object obj) {
        if (obj instanceof Closeable) {
            try {
                ((Closeable) obj).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
