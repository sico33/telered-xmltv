package com.bumptech.glide.load.model;

import androidx.core.util.Pools;
import com.bumptech.glide.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class ModelLoaderRegistry {
    private final ModelLoaderCache cache;
    private final MultiModelLoaderFactory multiModelLoaderFactory;

    private static class ModelLoaderCache {
        private final Map<Class<?>, Entry<?>> cachedModelLoaders = new HashMap();

        private static class Entry<Model> {
            final List<ModelLoader<Model, ?>> loaders;

            public Entry(List<ModelLoader<Model, ?>> list) {
                this.loaders = list;
            }
        }

        ModelLoaderCache() {
        }

        public void clear() {
            this.cachedModelLoaders.clear();
        }

        public <Model> List<ModelLoader<Model, ?>> get(Class<Model> cls) {
            Entry<?> entry = this.cachedModelLoaders.get(cls);
            if (entry == null) {
                return null;
            }
            return (List<ModelLoader<Model, ?>>) entry.loaders;
        }

        public <Model> void put(Class<Model> cls, List<ModelLoader<Model, ?>> list) {
            if (this.cachedModelLoaders.put(cls, new Entry<>(list)) != null) {
                throw new IllegalStateException("Already cached loaders for model: " + cls);
            }
        }
    }

    public ModelLoaderRegistry(Pools.Pool<List<Throwable>> pool) {
        this(new MultiModelLoaderFactory(pool));
    }

    private ModelLoaderRegistry(MultiModelLoaderFactory multiModelLoaderFactory) {
        this.cache = new ModelLoaderCache();
        this.multiModelLoaderFactory = multiModelLoaderFactory;
    }

    private static <A> Class<A> getClass(A a) {
        return (Class<A>) a.getClass();
    }

    private <A> List<ModelLoader<A, ?>> getModelLoadersForClass(Class<A> cls) {
        List<ModelLoader<A, ?>> listUnmodifiableList;
        synchronized (this) {
            listUnmodifiableList = this.cache.get(cls);
            if (listUnmodifiableList == null) {
                listUnmodifiableList = Collections.unmodifiableList(this.multiModelLoaderFactory.build(cls));
                this.cache.put(cls, listUnmodifiableList);
            }
        }
        return listUnmodifiableList;
    }

    private <Model, Data> void tearDown(List<ModelLoaderFactory<? extends Model, ? extends Data>> list) {
        Iterator<ModelLoaderFactory<? extends Model, ? extends Data>> it = list.iterator();
        while (it.hasNext()) {
            it.next().teardown();
        }
    }

    public <Model, Data> void append(Class<Model> cls, Class<Data> cls2, ModelLoaderFactory<? extends Model, ? extends Data> modelLoaderFactory) {
        synchronized (this) {
            this.multiModelLoaderFactory.append(cls, cls2, modelLoaderFactory);
            this.cache.clear();
        }
    }

    public <Model, Data> ModelLoader<Model, Data> build(Class<Model> cls, Class<Data> cls2) {
        ModelLoader<Model, Data> modelLoaderBuild;
        synchronized (this) {
            modelLoaderBuild = this.multiModelLoaderFactory.build(cls, cls2);
        }
        return modelLoaderBuild;
    }

    public List<Class<?>> getDataClasses(Class<?> cls) {
        List<Class<?>> dataClasses;
        synchronized (this) {
            dataClasses = this.multiModelLoaderFactory.getDataClasses(cls);
        }
        return dataClasses;
    }

    public <A> List<ModelLoader<A, ?>> getModelLoaders(A a) {
        List<ModelLoader<A, ?>> modelLoadersForClass = getModelLoadersForClass(getClass(a));
        if (modelLoadersForClass.isEmpty()) {
            throw new Registry.NoModelLoaderAvailableException(a);
        }
        int size = modelLoadersForClass.size();
        boolean z = true;
        List<ModelLoader<A, ?>> listEmptyList = Collections.emptyList();
        int i = 0;
        while (i < size) {
            ModelLoader<A, ?> modelLoader = modelLoadersForClass.get(i);
            if (modelLoader.handles(a)) {
                if (z) {
                    listEmptyList = new ArrayList<>(size - i);
                    z = false;
                }
                listEmptyList.add(modelLoader);
            }
            i++;
            z = z;
            listEmptyList = listEmptyList;
        }
        if (listEmptyList.isEmpty()) {
            throw new Registry.NoModelLoaderAvailableException(a, modelLoadersForClass);
        }
        return listEmptyList;
    }

    public <Model, Data> void prepend(Class<Model> cls, Class<Data> cls2, ModelLoaderFactory<? extends Model, ? extends Data> modelLoaderFactory) {
        synchronized (this) {
            this.multiModelLoaderFactory.prepend(cls, cls2, modelLoaderFactory);
            this.cache.clear();
        }
    }

    public <Model, Data> void remove(Class<Model> cls, Class<Data> cls2) {
        synchronized (this) {
            tearDown(this.multiModelLoaderFactory.remove(cls, cls2));
            this.cache.clear();
        }
    }

    public <Model, Data> void replace(Class<Model> cls, Class<Data> cls2, ModelLoaderFactory<? extends Model, ? extends Data> modelLoaderFactory) {
        synchronized (this) {
            tearDown(this.multiModelLoaderFactory.replace(cls, cls2, modelLoaderFactory));
            this.cache.clear();
        }
    }
}
