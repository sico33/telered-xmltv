package com.bumptech.glide.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class LruCache<T, Y> {
    private final Map<T, Entry<Y>> cache = new LinkedHashMap(100, 0.75f, true);
    private long currentSize;
    private final long initialMaxSize;
    private long maxSize;

    static final class Entry<Y> {
        final int size;
        final Y value;

        Entry(Y y, int i) {
            this.value = y;
            this.size = i;
        }
    }

    public LruCache(long j) {
        this.initialMaxSize = j;
        this.maxSize = j;
    }

    private void evict() {
        trimToSize(this.maxSize);
    }

    public void clearMemory() {
        trimToSize(0L);
    }

    public boolean contains(T t) {
        boolean zContainsKey;
        synchronized (this) {
            zContainsKey = this.cache.containsKey(t);
        }
        return zContainsKey;
    }

    public Y get(T t) {
        Y y;
        synchronized (this) {
            Entry<Y> entry = this.cache.get(t);
            y = entry != null ? entry.value : null;
        }
        return y;
    }

    protected int getCount() {
        int size;
        synchronized (this) {
            size = this.cache.size();
        }
        return size;
    }

    public long getCurrentSize() {
        long j;
        synchronized (this) {
            j = this.currentSize;
        }
        return j;
    }

    public long getMaxSize() {
        long j;
        synchronized (this) {
            j = this.maxSize;
        }
        return j;
    }

    protected int getSize(Y y) {
        return 1;
    }

    protected void onItemEvicted(T t, Y y) {
    }

    public Y put(T t, Y y) {
        synchronized (this) {
            int size = getSize(y);
            if (size >= this.maxSize) {
                onItemEvicted(t, y);
                return null;
            }
            if (y != null) {
                this.currentSize += (long) size;
            }
            Entry<Y> entryPut = this.cache.put(t, y == null ? null : new Entry<>(y, size));
            if (entryPut != null) {
                this.currentSize -= (long) entryPut.size;
                if (!entryPut.value.equals(y)) {
                    onItemEvicted(t, entryPut.value);
                }
            }
            evict();
            return entryPut != null ? entryPut.value : null;
        }
    }

    public Y remove(T t) {
        synchronized (this) {
            Entry<Y> entryRemove = this.cache.remove(t);
            if (entryRemove == null) {
                return null;
            }
            this.currentSize -= (long) entryRemove.size;
            return entryRemove.value;
        }
    }

    public void setSizeMultiplier(float f) {
        synchronized (this) {
            try {
                if (f < 0.0f) {
                    throw new IllegalArgumentException("Multiplier must be >= 0");
                }
                this.maxSize = Math.round(this.initialMaxSize * f);
                evict();
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    protected void trimToSize(long j) {
        synchronized (this) {
            while (this.currentSize > j) {
                Iterator<Map.Entry<T, Entry<Y>>> it = this.cache.entrySet().iterator();
                Map.Entry<T, Entry<Y>> next = it.next();
                Entry<Y> value = next.getValue();
                this.currentSize -= (long) value.size;
                T key = next.getKey();
                it.remove();
                onItemEvicted(key, value.value);
            }
        }
    }
}
