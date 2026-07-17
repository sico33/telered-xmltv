package com.bumptech.glide.provider;

import androidx.collection.ArrayMap;
import com.bumptech.glide.util.MultiClassKey;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/* JADX INFO: loaded from: classes.dex */
public class ModelToResourceClassCache {
    private final AtomicReference<MultiClassKey> resourceClassKeyRef = new AtomicReference<>();
    private final ArrayMap<MultiClassKey, List<Class<?>>> registeredResourceClassCache = new ArrayMap<>();

    public void clear() {
        synchronized (this.registeredResourceClassCache) {
            this.registeredResourceClassCache.clear();
        }
    }

    public List<Class<?>> get(Class<?> cls, Class<?> cls2, Class<?> cls3) {
        MultiClassKey multiClassKey;
        List<Class<?>> list;
        MultiClassKey andSet = this.resourceClassKeyRef.getAndSet(null);
        if (andSet == null) {
            multiClassKey = new MultiClassKey(cls, cls2, cls3);
        } else {
            andSet.set(cls, cls2, cls3);
            multiClassKey = andSet;
        }
        synchronized (this.registeredResourceClassCache) {
            list = this.registeredResourceClassCache.get(multiClassKey);
        }
        this.resourceClassKeyRef.set(multiClassKey);
        return list;
    }

    public void put(Class<?> cls, Class<?> cls2, Class<?> cls3, List<Class<?>> list) {
        synchronized (this.registeredResourceClassCache) {
            this.registeredResourceClassCache.put(new MultiClassKey(cls, cls2, cls3), list);
        }
    }
}
