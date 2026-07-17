package com.bumptech.glide.load.engine;

import android.util.Log;
import androidx.core.util.Pools;
import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.Executors;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.pool.FactoryPools;
import java.util.Map;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public class Engine implements EngineJobListener, MemoryCache.ResourceRemovedListener, EngineResource.ResourceListener {
    private static final int JOB_POOL_SIZE = 150;
    private static final String TAG = "Engine";
    private static final boolean VERBOSE_IS_LOGGABLE = Log.isLoggable(TAG, 2);
    private final ActiveResources activeResources;
    private final MemoryCache cache;
    private final DecodeJobFactory decodeJobFactory;
    private final LazyDiskCacheProvider diskCacheProvider;
    private final EngineJobFactory engineJobFactory;
    private final Jobs jobs;
    private final EngineKeyFactory keyFactory;
    private final ResourceRecycler resourceRecycler;

    static class DecodeJobFactory {
        private int creationOrder;
        final DecodeJob.DiskCacheProvider diskCacheProvider;
        final Pools.Pool<DecodeJob<?>> pool = FactoryPools.threadSafe(Engine.JOB_POOL_SIZE, new FactoryPools.Factory<DecodeJob<?>>(this) { // from class: com.bumptech.glide.load.engine.Engine.DecodeJobFactory.1
            final DecodeJobFactory this$0;

            {
                this.this$0 = this;
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.bumptech.glide.util.pool.FactoryPools.Factory
            public DecodeJob<?> create() {
                return new DecodeJob<>(this.this$0.diskCacheProvider, this.this$0.pool);
            }
        });

        DecodeJobFactory(DecodeJob.DiskCacheProvider diskCacheProvider) {
            this.diskCacheProvider = diskCacheProvider;
        }

        <R> DecodeJob<R> build(GlideContext glideContext, Object obj, EngineKey engineKey, Key key, int i, int i2, Class<?> cls, Class<R> cls2, Priority priority, DiskCacheStrategy diskCacheStrategy, Map<Class<?>, Transformation<?>> map, boolean z, boolean z2, boolean z3, Options options, DecodeJob.Callback<R> callback) {
            DecodeJob decodeJob = (DecodeJob) Preconditions.checkNotNull(this.pool.acquire());
            int i3 = this.creationOrder;
            this.creationOrder = i3 + 1;
            return decodeJob.init(glideContext, obj, engineKey, key, i, i2, cls, cls2, priority, diskCacheStrategy, map, z, z2, z3, options, callback, i3);
        }
    }

    static class EngineJobFactory {
        final GlideExecutor animationExecutor;
        final GlideExecutor diskCacheExecutor;
        final EngineJobListener engineJobListener;
        final Pools.Pool<EngineJob<?>> pool = FactoryPools.threadSafe(Engine.JOB_POOL_SIZE, new FactoryPools.Factory<EngineJob<?>>(this) { // from class: com.bumptech.glide.load.engine.Engine.EngineJobFactory.1
            final EngineJobFactory this$0;

            {
                this.this$0 = this;
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.bumptech.glide.util.pool.FactoryPools.Factory
            public EngineJob<?> create() {
                return new EngineJob<>(this.this$0.diskCacheExecutor, this.this$0.sourceExecutor, this.this$0.sourceUnlimitedExecutor, this.this$0.animationExecutor, this.this$0.engineJobListener, this.this$0.resourceListener, this.this$0.pool);
            }
        });
        final EngineResource.ResourceListener resourceListener;
        final GlideExecutor sourceExecutor;
        final GlideExecutor sourceUnlimitedExecutor;

        EngineJobFactory(GlideExecutor glideExecutor, GlideExecutor glideExecutor2, GlideExecutor glideExecutor3, GlideExecutor glideExecutor4, EngineJobListener engineJobListener, EngineResource.ResourceListener resourceListener) {
            this.diskCacheExecutor = glideExecutor;
            this.sourceExecutor = glideExecutor2;
            this.sourceUnlimitedExecutor = glideExecutor3;
            this.animationExecutor = glideExecutor4;
            this.engineJobListener = engineJobListener;
            this.resourceListener = resourceListener;
        }

        <R> EngineJob<R> build(Key key, boolean z, boolean z2, boolean z3, boolean z4) {
            return ((EngineJob) Preconditions.checkNotNull(this.pool.acquire())).init(key, z, z2, z3, z4);
        }

        void shutdown() {
            Executors.shutdownAndAwaitTermination(this.diskCacheExecutor);
            Executors.shutdownAndAwaitTermination(this.sourceExecutor);
            Executors.shutdownAndAwaitTermination(this.sourceUnlimitedExecutor);
            Executors.shutdownAndAwaitTermination(this.animationExecutor);
        }
    }

    private static class LazyDiskCacheProvider implements DecodeJob.DiskCacheProvider {
        private volatile DiskCache diskCache;
        private final DiskCache.Factory factory;

        LazyDiskCacheProvider(DiskCache.Factory factory) {
            this.factory = factory;
        }

        void clearDiskCacheIfCreated() {
            synchronized (this) {
                if (this.diskCache == null) {
                    return;
                }
                this.diskCache.clear();
            }
        }

        @Override // com.bumptech.glide.load.engine.DecodeJob.DiskCacheProvider
        public DiskCache getDiskCache() {
            if (this.diskCache == null) {
                synchronized (this) {
                    if (this.diskCache == null) {
                        this.diskCache = this.factory.build();
                    }
                    if (this.diskCache == null) {
                        this.diskCache = new DiskCacheAdapter();
                    }
                }
            }
            return this.diskCache;
        }
    }

    public class LoadStatus {
        private final ResourceCallback cb;
        private final EngineJob<?> engineJob;
        final Engine this$0;

        LoadStatus(Engine engine, ResourceCallback resourceCallback, EngineJob<?> engineJob) {
            this.this$0 = engine;
            this.cb = resourceCallback;
            this.engineJob = engineJob;
        }

        public void cancel() {
            synchronized (this.this$0) {
                this.engineJob.removeCallback(this.cb);
            }
        }
    }

    Engine(MemoryCache memoryCache, DiskCache.Factory factory, GlideExecutor glideExecutor, GlideExecutor glideExecutor2, GlideExecutor glideExecutor3, GlideExecutor glideExecutor4, Jobs jobs, EngineKeyFactory engineKeyFactory, ActiveResources activeResources, EngineJobFactory engineJobFactory, DecodeJobFactory decodeJobFactory, ResourceRecycler resourceRecycler, boolean z) {
        this.cache = memoryCache;
        this.diskCacheProvider = new LazyDiskCacheProvider(factory);
        activeResources = activeResources == null ? new ActiveResources(z) : activeResources;
        this.activeResources = activeResources;
        activeResources.setListener(this);
        this.keyFactory = engineKeyFactory == null ? new EngineKeyFactory() : engineKeyFactory;
        this.jobs = jobs == null ? new Jobs() : jobs;
        this.engineJobFactory = engineJobFactory == null ? new EngineJobFactory(glideExecutor, glideExecutor2, glideExecutor3, glideExecutor4, this, this) : engineJobFactory;
        this.decodeJobFactory = decodeJobFactory == null ? new DecodeJobFactory(this.diskCacheProvider) : decodeJobFactory;
        this.resourceRecycler = resourceRecycler == null ? new ResourceRecycler() : resourceRecycler;
        memoryCache.setResourceRemovedListener(this);
    }

    public Engine(MemoryCache memoryCache, DiskCache.Factory factory, GlideExecutor glideExecutor, GlideExecutor glideExecutor2, GlideExecutor glideExecutor3, GlideExecutor glideExecutor4, boolean z) {
        this(memoryCache, factory, glideExecutor, glideExecutor2, glideExecutor3, glideExecutor4, null, null, null, null, null, null, z);
    }

    private EngineResource<?> getEngineResourceFromCache(Key key) {
        Resource<?> resourceRemove = this.cache.remove(key);
        if (resourceRemove == null) {
            return null;
        }
        return resourceRemove instanceof EngineResource ? (EngineResource) resourceRemove : new EngineResource<>(resourceRemove, true, true, key, this);
    }

    private EngineResource<?> loadFromActiveResources(Key key) {
        EngineResource<?> engineResource = this.activeResources.get(key);
        if (engineResource != null) {
            engineResource.acquire();
        }
        return engineResource;
    }

    private EngineResource<?> loadFromCache(Key key) {
        EngineResource<?> engineResourceFromCache = getEngineResourceFromCache(key);
        if (engineResourceFromCache != null) {
            engineResourceFromCache.acquire();
            this.activeResources.activate(key, engineResourceFromCache);
        }
        return engineResourceFromCache;
    }

    private EngineResource<?> loadFromMemory(EngineKey engineKey, boolean z, long j) {
        if (!z) {
            return null;
        }
        EngineResource<?> engineResourceLoadFromActiveResources = loadFromActiveResources(engineKey);
        if (engineResourceLoadFromActiveResources != null) {
            if (VERBOSE_IS_LOGGABLE) {
                logWithTimeAndKey("Loaded resource from active resources", j, engineKey);
            }
            return engineResourceLoadFromActiveResources;
        }
        EngineResource<?> engineResourceLoadFromCache = loadFromCache(engineKey);
        if (engineResourceLoadFromCache == null) {
            return null;
        }
        if (VERBOSE_IS_LOGGABLE) {
            logWithTimeAndKey("Loaded resource from cache", j, engineKey);
        }
        return engineResourceLoadFromCache;
    }

    private static void logWithTimeAndKey(String str, long j, Key key) {
        Log.v(TAG, str + " in " + LogTime.getElapsedMillis(j) + "ms, key: " + key);
    }

    private <R> LoadStatus waitForExistingOrStartNewJob(GlideContext glideContext, Object obj, Key key, int i, int i2, Class<?> cls, Class<R> cls2, Priority priority, DiskCacheStrategy diskCacheStrategy, Map<Class<?>, Transformation<?>> map, boolean z, boolean z2, Options options, boolean z3, boolean z4, boolean z5, boolean z6, ResourceCallback resourceCallback, Executor executor, EngineKey engineKey, long j) {
        EngineJob<?> engineJob = this.jobs.get(engineKey, z6);
        if (engineJob != null) {
            engineJob.addCallback(resourceCallback, executor);
            if (VERBOSE_IS_LOGGABLE) {
                logWithTimeAndKey("Added to existing load", j, engineKey);
            }
            return new LoadStatus(this, resourceCallback, engineJob);
        }
        EngineJob<R> engineJobBuild = this.engineJobFactory.build(engineKey, z3, z4, z5, z6);
        DecodeJob<R> decodeJobBuild = this.decodeJobFactory.build(glideContext, obj, engineKey, key, i, i2, cls, cls2, priority, diskCacheStrategy, map, z, z2, z6, options, engineJobBuild);
        this.jobs.put(engineKey, engineJobBuild);
        engineJobBuild.addCallback(resourceCallback, executor);
        engineJobBuild.start(decodeJobBuild);
        if (VERBOSE_IS_LOGGABLE) {
            logWithTimeAndKey("Started new load", j, engineKey);
        }
        return new LoadStatus(this, resourceCallback, engineJobBuild);
    }

    public void clearDiskCache() {
        this.diskCacheProvider.getDiskCache().clear();
    }

    /* JADX WARN: Bottom block not found for handler: all -> 0x006e */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public <R> com.bumptech.glide.load.engine.Engine.LoadStatus load(com.bumptech.glide.GlideContext r31, java.lang.Object r32, com.bumptech.glide.load.Key r33, int r34, int r35, java.lang.Class<?> r36, java.lang.Class<R> r37, com.bumptech.glide.Priority r38, com.bumptech.glide.load.engine.DiskCacheStrategy r39, java.util.Map<java.lang.Class<?>, com.bumptech.glide.load.Transformation<?>> r40, boolean r41, boolean r42, com.bumptech.glide.load.Options r43, boolean r44, boolean r45, boolean r46, boolean r47, com.bumptech.glide.request.ResourceCallback r48, java.util.concurrent.Executor r49) throws java.lang.Throwable {
        /*
            r30 = this;
            boolean r6 = com.bumptech.glide.load.engine.Engine.VERBOSE_IS_LOGGABLE
            if (r6 == 0) goto L5d
            long r28 = com.bumptech.glide.util.LogTime.getLogTime()
        L8:
            r0 = r30
            com.bumptech.glide.load.engine.EngineKeyFactory r6 = r0.keyFactory
            r7 = r32
            r8 = r33
            r9 = r34
            r10 = r35
            r11 = r40
            r12 = r36
            r13 = r37
            r14 = r43
            com.bumptech.glide.load.engine.EngineKey r27 = r6.buildKey(r7, r8, r9, r10, r11, r12, r13, r14)
            monitor-enter(r30)
            r0 = r30
            r1 = r27
            r2 = r44
            r3 = r28
            com.bumptech.glide.load.engine.EngineResource r6 = r0.loadFromMemory(r1, r2, r3)     // Catch: java.lang.Throwable -> L6b
            if (r6 != 0) goto L60
            r7 = r30
            r8 = r31
            r9 = r32
            r10 = r33
            r11 = r34
            r12 = r35
            r13 = r36
            r14 = r37
            r15 = r38
            r16 = r39
            r17 = r40
            r18 = r41
            r19 = r42
            r20 = r43
            r21 = r44
            r22 = r45
            r23 = r46
            r24 = r47
            r25 = r48
            r26 = r49
            com.bumptech.glide.load.engine.Engine$LoadStatus r6 = r7.waitForExistingOrStartNewJob(r8, r9, r10, r11, r12, r13, r14, r15, r16, r17, r18, r19, r20, r21, r22, r23, r24, r25, r26, r27, r28)     // Catch: java.lang.Throwable -> L70
            monitor-exit(r30)     // Catch: java.lang.Throwable -> L70
        L5c:
            return r6
        L5d:
            r28 = 0
            goto L8
        L60:
            monitor-exit(r30)     // Catch: java.lang.Throwable -> L70
            com.bumptech.glide.load.DataSource r7 = com.bumptech.glide.load.DataSource.MEMORY_CACHE
            r8 = 0
            r0 = r48
            r0.onResourceReady(r6, r7, r8)
            r6 = 0
            goto L5c
        L6b:
            r6 = move-exception
        L6c:
            monitor-exit(r30)     // Catch: java.lang.Throwable -> L6e
            throw r6
        L6e:
            r6 = move-exception
            goto L6c
        L70:
            r6 = move-exception
            goto L6c
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.load.engine.Engine.load(com.bumptech.glide.GlideContext, java.lang.Object, com.bumptech.glide.load.Key, int, int, java.lang.Class, java.lang.Class, com.bumptech.glide.Priority, com.bumptech.glide.load.engine.DiskCacheStrategy, java.util.Map, boolean, boolean, com.bumptech.glide.load.Options, boolean, boolean, boolean, boolean, com.bumptech.glide.request.ResourceCallback, java.util.concurrent.Executor):com.bumptech.glide.load.engine.Engine$LoadStatus");
    }

    @Override // com.bumptech.glide.load.engine.EngineJobListener
    public void onEngineJobCancelled(EngineJob<?> engineJob, Key key) {
        synchronized (this) {
            this.jobs.removeIfCurrent(key, engineJob);
        }
    }

    @Override // com.bumptech.glide.load.engine.EngineJobListener
    public void onEngineJobComplete(EngineJob<?> engineJob, Key key, EngineResource<?> engineResource) {
        synchronized (this) {
            if (engineResource != null) {
                if (engineResource.isMemoryCacheable()) {
                    this.activeResources.activate(key, engineResource);
                }
            }
            this.jobs.removeIfCurrent(key, engineJob);
        }
    }

    @Override // com.bumptech.glide.load.engine.EngineResource.ResourceListener
    public void onResourceReleased(Key key, EngineResource<?> engineResource) {
        this.activeResources.deactivate(key);
        if (engineResource.isMemoryCacheable()) {
            this.cache.put(key, engineResource);
        } else {
            this.resourceRecycler.recycle(engineResource, false);
        }
    }

    @Override // com.bumptech.glide.load.engine.cache.MemoryCache.ResourceRemovedListener
    public void onResourceRemoved(Resource<?> resource) {
        this.resourceRecycler.recycle(resource, true);
    }

    public void release(Resource<?> resource) {
        if (!(resource instanceof EngineResource)) {
            throw new IllegalArgumentException("Cannot release anything but an EngineResource");
        }
        ((EngineResource) resource).release();
    }

    public void shutdown() {
        this.engineJobFactory.shutdown();
        this.diskCacheProvider.clearDiskCacheIfCreated();
        this.activeResources.shutdown();
    }
}
