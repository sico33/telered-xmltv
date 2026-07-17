package com.bumptech.glide.load.model;

import androidx.core.util.Pools;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.util.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public class MultiModelLoaderFactory {
    private static final Factory DEFAULT_FACTORY = new Factory();
    private static final ModelLoader<Object, Object> EMPTY_MODEL_LOADER = new EmptyModelLoader();
    private final Set<Entry<?, ?>> alreadyUsedEntries;
    private final List<Entry<?, ?>> entries;
    private final Factory factory;
    private final Pools.Pool<List<Throwable>> throwableListPool;

    private static class EmptyModelLoader implements ModelLoader<Object, Object> {
        EmptyModelLoader() {
        }

        @Override // com.bumptech.glide.load.model.ModelLoader
        public ModelLoader.LoadData<Object> buildLoadData(Object obj, int i, int i2, Options options) {
            return null;
        }

        @Override // com.bumptech.glide.load.model.ModelLoader
        public boolean handles(Object obj) {
            return false;
        }
    }

    private static class Entry<Model, Data> {
        final Class<Data> dataClass;
        final ModelLoaderFactory<? extends Model, ? extends Data> factory;
        private final Class<Model> modelClass;

        public Entry(Class<Model> cls, Class<Data> cls2, ModelLoaderFactory<? extends Model, ? extends Data> modelLoaderFactory) {
            this.modelClass = cls;
            this.dataClass = cls2;
            this.factory = modelLoaderFactory;
        }

        public boolean handles(Class<?> cls) {
            return this.modelClass.isAssignableFrom(cls);
        }

        public boolean handles(Class<?> cls, Class<?> cls2) {
            return handles(cls) && this.dataClass.isAssignableFrom(cls2);
        }
    }

    static class Factory {
        Factory() {
        }

        public <Model, Data> MultiModelLoader<Model, Data> build(List<ModelLoader<Model, Data>> list, Pools.Pool<List<Throwable>> pool) {
            return new MultiModelLoader<>(list, pool);
        }
    }

    public MultiModelLoaderFactory(Pools.Pool<List<Throwable>> pool) {
        this(pool, DEFAULT_FACTORY);
    }

    MultiModelLoaderFactory(Pools.Pool<List<Throwable>> pool, Factory factory) {
        this.entries = new ArrayList();
        this.alreadyUsedEntries = new HashSet();
        this.throwableListPool = pool;
        this.factory = factory;
    }

    private <Model, Data> void add(Class<Model> cls, Class<Data> cls2, ModelLoaderFactory<? extends Model, ? extends Data> modelLoaderFactory, boolean z) {
        this.entries.add(z ? this.entries.size() : 0, new Entry<>(cls, cls2, modelLoaderFactory));
    }

    private <Model, Data> ModelLoader<Model, Data> build(Entry<?, ?> entry) {
        return (ModelLoader) Preconditions.checkNotNull(entry.factory.build(this));
    }

    private static <Model, Data> ModelLoader<Model, Data> emptyModelLoader() {
        return (ModelLoader<Model, Data>) EMPTY_MODEL_LOADER;
    }

    private <Model, Data> ModelLoaderFactory<Model, Data> getFactory(Entry<?, ?> entry) {
        return (ModelLoaderFactory<Model, Data>) entry.factory;
    }

    <Model, Data> void append(Class<Model> cls, Class<Data> cls2, ModelLoaderFactory<? extends Model, ? extends Data> modelLoaderFactory) {
        synchronized (this) {
            add(cls, cls2, modelLoaderFactory, true);
        }
    }

    public <Model, Data> ModelLoader<Model, Data> build(Class<Model> cls, Class<Data> cls2) {
        ModelLoader<Model, Data> modelLoaderEmptyModelLoader;
        synchronized (this) {
            try {
                ArrayList arrayList = new ArrayList();
                boolean z = false;
                for (Entry<?, ?> entry : this.entries) {
                    if (this.alreadyUsedEntries.contains(entry)) {
                        z = true;
                    } else if (entry.handles(cls, cls2)) {
                        this.alreadyUsedEntries.add(entry);
                        arrayList.add(build(entry));
                        this.alreadyUsedEntries.remove(entry);
                    }
                }
                if (arrayList.size() > 1) {
                    modelLoaderEmptyModelLoader = this.factory.build(arrayList, this.throwableListPool);
                } else if (arrayList.size() == 1) {
                    modelLoaderEmptyModelLoader = (ModelLoader) arrayList.get(0);
                } else {
                    if (!z) {
                        throw new Registry.NoModelLoaderAvailableException((Class<?>) cls, (Class<?>) cls2);
                    }
                    modelLoaderEmptyModelLoader = emptyModelLoader();
                }
            } catch (Throwable th) {
                this.alreadyUsedEntries.clear();
                throw th;
            }
        }
        return modelLoaderEmptyModelLoader;
    }

    <Model> List<ModelLoader<Model, ?>> build(Class<Model> cls) {
        ArrayList arrayList;
        synchronized (this) {
            try {
                arrayList = new ArrayList();
                for (Entry<?, ?> entry : this.entries) {
                    if (!this.alreadyUsedEntries.contains(entry) && entry.handles(cls)) {
                        this.alreadyUsedEntries.add(entry);
                        arrayList.add(build(entry));
                        this.alreadyUsedEntries.remove(entry);
                    }
                }
            } catch (Throwable th) {
                this.alreadyUsedEntries.clear();
                throw th;
            }
        }
        return arrayList;
    }

    List<Class<?>> getDataClasses(Class<?> cls) {
        ArrayList arrayList;
        synchronized (this) {
            arrayList = new ArrayList();
            for (Entry<?, ?> entry : this.entries) {
                if (!arrayList.contains(entry.dataClass) && entry.handles(cls)) {
                    arrayList.add(entry.dataClass);
                }
            }
        }
        return arrayList;
    }

    <Model, Data> void prepend(Class<Model> cls, Class<Data> cls2, ModelLoaderFactory<? extends Model, ? extends Data> modelLoaderFactory) {
        synchronized (this) {
            add(cls, cls2, modelLoaderFactory, false);
        }
    }

    <Model, Data> List<ModelLoaderFactory<? extends Model, ? extends Data>> remove(Class<Model> cls, Class<Data> cls2) {
        ArrayList arrayList;
        synchronized (this) {
            arrayList = new ArrayList();
            Iterator<Entry<?, ?>> it = this.entries.iterator();
            while (it.hasNext()) {
                Entry<?, ?> next = it.next();
                if (next.handles(cls, cls2)) {
                    it.remove();
                    arrayList.add(getFactory(next));
                }
            }
        }
        return arrayList;
    }

    <Model, Data> List<ModelLoaderFactory<? extends Model, ? extends Data>> replace(Class<Model> cls, Class<Data> cls2, ModelLoaderFactory<? extends Model, ? extends Data> modelLoaderFactory) {
        List<ModelLoaderFactory<? extends Model, ? extends Data>> listRemove;
        synchronized (this) {
            listRemove = remove(cls, cls2);
            append(cls, cls2, modelLoaderFactory);
        }
        return listRemove;
    }
}
