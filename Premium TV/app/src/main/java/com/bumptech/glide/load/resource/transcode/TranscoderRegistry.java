package com.bumptech.glide.load.resource.transcode;

import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class TranscoderRegistry {
    private final List<Entry<?, ?>> transcoders = new ArrayList();

    private static final class Entry<Z, R> {
        final Class<Z> fromClass;
        final Class<R> toClass;
        final ResourceTranscoder<Z, R> transcoder;

        Entry(Class<Z> cls, Class<R> cls2, ResourceTranscoder<Z, R> resourceTranscoder) {
            this.fromClass = cls;
            this.toClass = cls2;
            this.transcoder = resourceTranscoder;
        }

        public boolean handles(Class<?> cls, Class<?> cls2) {
            return this.fromClass.isAssignableFrom(cls) && cls2.isAssignableFrom(this.toClass);
        }
    }

    public <Z, R> ResourceTranscoder<Z, R> get(Class<Z> cls, Class<R> cls2) {
        ResourceTranscoder<Z, R> resourceTranscoder;
        synchronized (this) {
            if (!cls2.isAssignableFrom(cls)) {
                for (Entry<?, ?> entry : this.transcoders) {
                    if (entry.handles(cls, cls2)) {
                        resourceTranscoder = (ResourceTranscoder<Z, R>) entry.transcoder;
                    }
                }
                throw new IllegalArgumentException("No transcoder registered to transcode from " + cls + " to " + cls2);
            }
            resourceTranscoder = UnitTranscoder.get();
        }
        return resourceTranscoder;
    }

    public <Z, R> List<Class<R>> getTranscodeClasses(Class<Z> cls, Class<R> cls2) {
        synchronized (this) {
            ArrayList arrayList = new ArrayList();
            if (cls2.isAssignableFrom(cls)) {
                arrayList.add(cls2);
                return arrayList;
            }
            for (Entry<?, ?> entry : this.transcoders) {
                if (entry.handles(cls, cls2) && !arrayList.contains(entry.toClass)) {
                    arrayList.add(entry.toClass);
                }
            }
            return arrayList;
        }
    }

    public <Z, R> void register(Class<Z> cls, Class<R> cls2, ResourceTranscoder<Z, R> resourceTranscoder) {
        synchronized (this) {
            this.transcoders.add(new Entry<>(cls, cls2, resourceTranscoder));
        }
    }
}
