package com.bumptech.glide.provider;

import com.bumptech.glide.load.Encoder;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class EncoderRegistry {
    private final List<Entry<?>> encoders = new ArrayList();

    private static final class Entry<T> {
        private final Class<T> dataClass;
        final Encoder<T> encoder;

        Entry(Class<T> cls, Encoder<T> encoder) {
            this.dataClass = cls;
            this.encoder = encoder;
        }

        boolean handles(Class<?> cls) {
            return this.dataClass.isAssignableFrom(cls);
        }
    }

    public <T> void append(Class<T> cls, Encoder<T> encoder) {
        synchronized (this) {
            this.encoders.add(new Entry<>(cls, encoder));
        }
    }

    public <T> Encoder<T> getEncoder(Class<T> cls) {
        synchronized (this) {
            for (Entry<?> entry : this.encoders) {
                if (entry.handles(cls)) {
                    return (Encoder<T>) entry.encoder;
                }
            }
            return null;
        }
    }

    public <T> void prepend(Class<T> cls, Encoder<T> encoder) {
        synchronized (this) {
            this.encoders.add(0, new Entry<>(cls, encoder));
        }
    }
}
