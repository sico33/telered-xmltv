package com.bumptech.glide.load.engine;

import androidx.core.util.Pools;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.Executors;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.pool.FactoryPools;
import com.bumptech.glide.util.pool.StateVerifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: classes.dex */
class EngineJob<R> implements DecodeJob.Callback<R>, FactoryPools.Poolable {
    private static final EngineResourceFactory DEFAULT_FACTORY = new EngineResourceFactory();
    private final GlideExecutor animationExecutor;
    final ResourceCallbacksAndExecutors cbs;
    DataSource dataSource;
    private DecodeJob<R> decodeJob;
    private final GlideExecutor diskCacheExecutor;
    private final EngineJobListener engineJobListener;
    EngineResource<?> engineResource;
    private final EngineResourceFactory engineResourceFactory;
    GlideException exception;
    private boolean hasLoadFailed;
    private boolean hasResource;
    private boolean isCacheable;
    private volatile boolean isCancelled;
    private boolean isLoadedFromAlternateCacheKey;
    private Key key;
    private boolean onlyRetrieveFromCache;
    private final AtomicInteger pendingCallbacks;
    private final Pools.Pool<EngineJob<?>> pool;
    private Resource<?> resource;
    private final EngineResource.ResourceListener resourceListener;
    private final GlideExecutor sourceExecutor;
    private final GlideExecutor sourceUnlimitedExecutor;
    private final StateVerifier stateVerifier;
    private boolean useAnimationPool;
    private boolean useUnlimitedSourceGeneratorPool;

    private class CallLoadFailed implements Runnable {
        private final ResourceCallback cb;
        final EngineJob this$0;

        CallLoadFailed(EngineJob engineJob, ResourceCallback resourceCallback) {
            this.this$0 = engineJob;
            this.cb = resourceCallback;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (this.cb.getLock()) {
                synchronized (this.this$0) {
                    if (this.this$0.cbs.contains(this.cb)) {
                        this.this$0.callCallbackOnLoadFailed(this.cb);
                    }
                    this.this$0.decrementPendingCallbacks();
                }
            }
        }
    }

    private class CallResourceReady implements Runnable {
        private final ResourceCallback cb;
        final EngineJob this$0;

        CallResourceReady(EngineJob engineJob, ResourceCallback resourceCallback) {
            this.this$0 = engineJob;
            this.cb = resourceCallback;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (this.cb.getLock()) {
                synchronized (this.this$0) {
                    if (this.this$0.cbs.contains(this.cb)) {
                        this.this$0.engineResource.acquire();
                        this.this$0.callCallbackOnResourceReady(this.cb);
                        this.this$0.removeCallback(this.cb);
                    }
                    this.this$0.decrementPendingCallbacks();
                }
            }
        }
    }

    static class EngineResourceFactory {
        EngineResourceFactory() {
        }

        public <R> EngineResource<R> build(Resource<R> resource, boolean z, Key key, EngineResource.ResourceListener resourceListener) {
            return new EngineResource<>(resource, z, true, key, resourceListener);
        }
    }

    static final class ResourceCallbackAndExecutor {
        final ResourceCallback cb;
        final Executor executor;

        ResourceCallbackAndExecutor(ResourceCallback resourceCallback, Executor executor) {
            this.cb = resourceCallback;
            this.executor = executor;
        }

        public boolean equals(Object obj) {
            if (obj instanceof ResourceCallbackAndExecutor) {
                return this.cb.equals(((ResourceCallbackAndExecutor) obj).cb);
            }
            return false;
        }

        public int hashCode() {
            return this.cb.hashCode();
        }
    }

    static final class ResourceCallbacksAndExecutors implements Iterable<ResourceCallbackAndExecutor> {
        private final List<ResourceCallbackAndExecutor> callbacksAndExecutors;

        ResourceCallbacksAndExecutors() {
            this(new ArrayList(2));
        }

        ResourceCallbacksAndExecutors(List<ResourceCallbackAndExecutor> list) {
            this.callbacksAndExecutors = list;
        }

        private static ResourceCallbackAndExecutor defaultCallbackAndExecutor(ResourceCallback resourceCallback) {
            return new ResourceCallbackAndExecutor(resourceCallback, Executors.directExecutor());
        }

        void add(ResourceCallback resourceCallback, Executor executor) {
            this.callbacksAndExecutors.add(new ResourceCallbackAndExecutor(resourceCallback, executor));
        }

        void clear() {
            this.callbacksAndExecutors.clear();
        }

        boolean contains(ResourceCallback resourceCallback) {
            return this.callbacksAndExecutors.contains(defaultCallbackAndExecutor(resourceCallback));
        }

        ResourceCallbacksAndExecutors copy() {
            return new ResourceCallbacksAndExecutors(new ArrayList(this.callbacksAndExecutors));
        }

        boolean isEmpty() {
            return this.callbacksAndExecutors.isEmpty();
        }

        @Override // java.lang.Iterable
        public Iterator<ResourceCallbackAndExecutor> iterator() {
            return this.callbacksAndExecutors.iterator();
        }

        void remove(ResourceCallback resourceCallback) {
            this.callbacksAndExecutors.remove(defaultCallbackAndExecutor(resourceCallback));
        }

        int size() {
            return this.callbacksAndExecutors.size();
        }
    }

    EngineJob(GlideExecutor glideExecutor, GlideExecutor glideExecutor2, GlideExecutor glideExecutor3, GlideExecutor glideExecutor4, EngineJobListener engineJobListener, EngineResource.ResourceListener resourceListener, Pools.Pool<EngineJob<?>> pool) {
        this(glideExecutor, glideExecutor2, glideExecutor3, glideExecutor4, engineJobListener, resourceListener, pool, DEFAULT_FACTORY);
    }

    EngineJob(GlideExecutor glideExecutor, GlideExecutor glideExecutor2, GlideExecutor glideExecutor3, GlideExecutor glideExecutor4, EngineJobListener engineJobListener, EngineResource.ResourceListener resourceListener, Pools.Pool<EngineJob<?>> pool, EngineResourceFactory engineResourceFactory) {
        this.cbs = new ResourceCallbacksAndExecutors();
        this.stateVerifier = StateVerifier.newInstance();
        this.pendingCallbacks = new AtomicInteger();
        this.diskCacheExecutor = glideExecutor;
        this.sourceExecutor = glideExecutor2;
        this.sourceUnlimitedExecutor = glideExecutor3;
        this.animationExecutor = glideExecutor4;
        this.engineJobListener = engineJobListener;
        this.resourceListener = resourceListener;
        this.pool = pool;
        this.engineResourceFactory = engineResourceFactory;
    }

    private GlideExecutor getActiveSourceExecutor() {
        if (this.useUnlimitedSourceGeneratorPool) {
            return this.sourceUnlimitedExecutor;
        }
        return this.useAnimationPool ? this.animationExecutor : this.sourceExecutor;
    }

    private boolean isDone() {
        return this.hasLoadFailed || this.hasResource || this.isCancelled;
    }

    private void release() {
        synchronized (this) {
            if (this.key == null) {
                throw new IllegalArgumentException();
            }
            this.cbs.clear();
            this.key = null;
            this.engineResource = null;
            this.resource = null;
            this.hasLoadFailed = false;
            this.isCancelled = false;
            this.hasResource = false;
            this.isLoadedFromAlternateCacheKey = false;
            this.decodeJob.release(false);
            this.decodeJob = null;
            this.exception = null;
            this.dataSource = null;
            this.pool.release(this);
        }
    }

    void addCallback(ResourceCallback resourceCallback, Executor executor) {
        synchronized (this) {
            this.stateVerifier.throwIfRecycled();
            this.cbs.add(resourceCallback, executor);
            if (this.hasResource) {
                incrementPendingCallbacks(1);
                executor.execute(new CallResourceReady(this, resourceCallback));
            } else if (this.hasLoadFailed) {
                incrementPendingCallbacks(1);
                executor.execute(new CallLoadFailed(this, resourceCallback));
            } else {
                Preconditions.checkArgument(this.isCancelled ? false : true, "Cannot add callbacks to a cancelled EngineJob");
            }
        }
    }

    void callCallbackOnLoadFailed(ResourceCallback resourceCallback) {
        try {
            resourceCallback.onLoadFailed(this.exception);
        } catch (Throwable th) {
            throw new CallbackException(th);
        }
    }

    void callCallbackOnResourceReady(ResourceCallback resourceCallback) {
        try {
            resourceCallback.onResourceReady(this.engineResource, this.dataSource, this.isLoadedFromAlternateCacheKey);
        } catch (Throwable th) {
            throw new CallbackException(th);
        }
    }

    void cancel() {
        if (isDone()) {
            return;
        }
        this.isCancelled = true;
        this.decodeJob.cancel();
        this.engineJobListener.onEngineJobCancelled(this, this.key);
    }

    void decrementPendingCallbacks() {
        EngineResource<?> engineResource = null;
        synchronized (this) {
            this.stateVerifier.throwIfRecycled();
            Preconditions.checkArgument(isDone(), "Not yet complete!");
            int iDecrementAndGet = this.pendingCallbacks.decrementAndGet();
            Preconditions.checkArgument(iDecrementAndGet >= 0, "Can't decrement below 0");
            if (iDecrementAndGet == 0) {
                engineResource = this.engineResource;
                release();
            }
        }
        if (engineResource != null) {
            engineResource.release();
        }
    }

    @Override // com.bumptech.glide.util.pool.FactoryPools.Poolable
    public StateVerifier getVerifier() {
        return this.stateVerifier;
    }

    void incrementPendingCallbacks(int i) {
        synchronized (this) {
            Preconditions.checkArgument(isDone(), "Not yet complete!");
            if (this.pendingCallbacks.getAndAdd(i) == 0 && this.engineResource != null) {
                this.engineResource.acquire();
            }
        }
    }

    EngineJob<R> init(Key key, boolean z, boolean z2, boolean z3, boolean z4) {
        synchronized (this) {
            this.key = key;
            this.isCacheable = z;
            this.useUnlimitedSourceGeneratorPool = z2;
            this.useAnimationPool = z3;
            this.onlyRetrieveFromCache = z4;
        }
        return this;
    }

    boolean isCancelled() {
        boolean z;
        synchronized (this) {
            z = this.isCancelled;
        }
        return z;
    }

    void notifyCallbacksOfException() {
        synchronized (this) {
            this.stateVerifier.throwIfRecycled();
            if (this.isCancelled) {
                release();
                return;
            }
            if (this.cbs.isEmpty()) {
                throw new IllegalStateException("Received an exception without any callbacks to notify");
            }
            if (this.hasLoadFailed) {
                throw new IllegalStateException("Already failed once");
            }
            this.hasLoadFailed = true;
            Key key = this.key;
            ResourceCallbacksAndExecutors resourceCallbacksAndExecutorsCopy = this.cbs.copy();
            incrementPendingCallbacks(resourceCallbacksAndExecutorsCopy.size() + 1);
            this.engineJobListener.onEngineJobComplete(this, key, null);
            for (ResourceCallbackAndExecutor resourceCallbackAndExecutor : resourceCallbacksAndExecutorsCopy) {
                resourceCallbackAndExecutor.executor.execute(new CallLoadFailed(this, resourceCallbackAndExecutor.cb));
            }
            decrementPendingCallbacks();
        }
    }

    /* JADX WARN: Type inference fix 'apply assigned field type' failed
    java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$PrimitiveArg
    	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:596)
    	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
    	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
     */
    void notifyCallbacksOfResult() {
        synchronized (this) {
            this.stateVerifier.throwIfRecycled();
            if (this.isCancelled) {
                this.resource.recycle();
                release();
                return;
            }
            if (this.cbs.isEmpty()) {
                throw new IllegalStateException("Received a resource without any callbacks to notify");
            }
            if (this.hasResource) {
                throw new IllegalStateException("Already have resource");
            }
            this.engineResource = this.engineResourceFactory.build(this.resource, this.isCacheable, this.key, this.resourceListener);
            this.hasResource = true;
            ResourceCallbacksAndExecutors resourceCallbacksAndExecutorsCopy = this.cbs.copy();
            incrementPendingCallbacks(resourceCallbacksAndExecutorsCopy.size() + 1);
            this.engineJobListener.onEngineJobComplete(this, this.key, this.engineResource);
            for (ResourceCallbackAndExecutor resourceCallbackAndExecutor : resourceCallbacksAndExecutorsCopy) {
                resourceCallbackAndExecutor.executor.execute(new CallResourceReady(this, resourceCallbackAndExecutor.cb));
            }
            decrementPendingCallbacks();
        }
    }

    @Override // com.bumptech.glide.load.engine.DecodeJob.Callback
    public void onLoadFailed(GlideException glideException) {
        synchronized (this) {
            this.exception = glideException;
        }
        notifyCallbacksOfException();
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.bumptech.glide.load.engine.DecodeJob.Callback
    public void onResourceReady(Resource<R> resource, DataSource dataSource, boolean z) {
        synchronized (this) {
            this.resource = resource;
            this.dataSource = dataSource;
            this.isLoadedFromAlternateCacheKey = z;
        }
        notifyCallbacksOfResult();
    }

    boolean onlyRetrieveFromCache() {
        return this.onlyRetrieveFromCache;
    }

    void removeCallback(ResourceCallback resourceCallback) {
        synchronized (this) {
            this.stateVerifier.throwIfRecycled();
            this.cbs.remove(resourceCallback);
            if (this.cbs.isEmpty()) {
                cancel();
                if ((this.hasResource || this.hasLoadFailed) && this.pendingCallbacks.get() == 0) {
                    release();
                }
            }
        }
    }

    @Override // com.bumptech.glide.load.engine.DecodeJob.Callback
    public void reschedule(DecodeJob<?> decodeJob) {
        getActiveSourceExecutor().execute(decodeJob);
    }

    public void start(DecodeJob<R> decodeJob) {
        synchronized (this) {
            this.decodeJob = decodeJob;
            (decodeJob.willDecodeFromCache() ? this.diskCacheExecutor : getActiveSourceExecutor()).execute(decodeJob);
        }
    }
}
