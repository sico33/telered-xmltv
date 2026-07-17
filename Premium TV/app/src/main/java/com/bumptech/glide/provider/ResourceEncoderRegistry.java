package com.bumptech.glide.provider;

import com.bumptech.glide.load.ResourceEncoder;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class ResourceEncoderRegistry {
    private final List<Entry<?>> encoders = new ArrayList();

    private static final class Entry<T> {
        final ResourceEncoder<T> encoder;
        private final Class<T> resourceClass;

        Entry(Class<T> cls, ResourceEncoder<T> resourceEncoder) {
            this.resourceClass = cls;
            this.encoder = resourceEncoder;
        }

        boolean handles(Class<?> cls) {
            return this.resourceClass.isAssignableFrom(cls);
        }
    }

    public <Z> void append(Class<Z> cls, ResourceEncoder<Z> resourceEncoder) {
        synchronized (this) {
            this.encoders.add(new Entry<>(cls, resourceEncoder));
        }
    }

    public <Z> ResourceEncoder<Z> get(Class<Z> cls) {
        synchronized (this) {
            int size = this.encoders.size();
            for (int i = 0; i < size; i++) {
                Entry<?> entry = this.encoders.get(i);
                if (entry.handles(cls)) {
                    return (ResourceEncoder<Z>) entry.encoder;
                }
            }
            return null;
        }
    }

    public <Z> void prepend(Class<Z> cls, ResourceEncoder<Z> resourceEncoder) {
        synchronized (this) {
            this.encoders.add(0, new Entry<>(cls, resourceEncoder));
        }
    }
}
