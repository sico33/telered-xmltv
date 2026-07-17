package com.bumptech.glide.load.engine;

import android.os.Process;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.util.Preconditions;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/* JADX INFO: loaded from: classes.dex */
final class ActiveResources {
    final Map<Key, ResourceWeakReference> activeEngineResources;
    private volatile DequeuedResourceCallback cb;
    private final boolean isActiveResourceRetentionAllowed;
    private volatile boolean isShutdown;
    private EngineResource.ResourceListener listener;
    private final Executor monitorClearedResourcesExecutor;
    private final ReferenceQueue<EngineResource<?>> resourceReferenceQueue;

    interface DequeuedResourceCallback {
        void onResourceDequeued();
    }

    static final class ResourceWeakReference extends WeakReference<EngineResource<?>> {
        final boolean isCacheable;
        final Key key;
        Resource<?> resource;

        ResourceWeakReference(Key key, EngineResource<?> engineResource, ReferenceQueue<? super EngineResource<?>> referenceQueue, boolean z) {
            super(engineResource, referenceQueue);
            this.key = (Key) Preconditions.checkNotNull(key);
            this.resource = (engineResource.isMemoryCacheable() && z) ? (Resource) Preconditions.checkNotNull(engineResource.getResource()) : null;
            this.isCacheable = engineResource.isMemoryCacheable();
        }

        void reset() {
            this.resource = null;
            clear();
        }
    }

    ActiveResources(boolean z) {
        this(z, Executors.newSingleThreadExecutor(new ThreadFactory() { // from class: com.bumptech.glide.load.engine.ActiveResources.1
            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(Runnable runnable) {
                return new Thread(new Runnable(this, runnable) { // from class: com.bumptech.glide.load.engine.ActiveResources.1.1
                    final AnonymousClass1 this$1;
                    final Runnable val$r;

                    {
                        this.this$1 = this;
                        this.val$r = runnable;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        Process.setThreadPriority(10);
                        this.val$r.run();
                    }
                }, "glide-active-resources");
            }
        }));
    }

    ActiveResources(boolean z, Executor executor) {
        this.activeEngineResources = new HashMap();
        this.resourceReferenceQueue = new ReferenceQueue<>();
        this.isActiveResourceRetentionAllowed = z;
        this.monitorClearedResourcesExecutor = executor;
        executor.execute(new Runnable(this) { // from class: com.bumptech.glide.load.engine.ActiveResources.2
            final ActiveResources this$0;

            {
                this.this$0 = this;
            }

            @Override // java.lang.Runnable
            public void run() {
                this.this$0.cleanReferenceQueue();
            }
        });
    }

    void activate(Key key, EngineResource<?> engineResource) {
        synchronized (this) {
            ResourceWeakReference resourceWeakReferencePut = this.activeEngineResources.put(key, new ResourceWeakReference(key, engineResource, this.resourceReferenceQueue, this.isActiveResourceRetentionAllowed));
            if (resourceWeakReferencePut != null) {
                resourceWeakReferencePut.reset();
            }
        }
    }

    void cleanReferenceQueue() {
        while (!this.isShutdown) {
            try {
                cleanupActiveReference((ResourceWeakReference) this.resourceReferenceQueue.remove());
                DequeuedResourceCallback dequeuedResourceCallback = this.cb;
                if (dequeuedResourceCallback != null) {
                    dequeuedResourceCallback.onResourceDequeued();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void cleanupActiveReference(ResourceWeakReference resourceWeakReference) {
        synchronized (this) {
            this.activeEngineResources.remove(resourceWeakReference.key);
            if (!resourceWeakReference.isCacheable || resourceWeakReference.resource == null) {
                return;
            }
            this.listener.onResourceReleased(resourceWeakReference.key, new EngineResource<>(resourceWeakReference.resource, true, false, resourceWeakReference.key, this.listener));
        }
    }

    void deactivate(Key key) {
        synchronized (this) {
            ResourceWeakReference resourceWeakReferenceRemove = this.activeEngineResources.remove(key);
            if (resourceWeakReferenceRemove != null) {
                resourceWeakReferenceRemove.reset();
            }
        }
    }

    EngineResource<?> get(Key key) {
        synchronized (this) {
            ResourceWeakReference resourceWeakReference = this.activeEngineResources.get(key);
            if (resourceWeakReference == null) {
                return null;
            }
            EngineResource<?> engineResource = (EngineResource) resourceWeakReference.get();
            if (engineResource == null) {
                cleanupActiveReference(resourceWeakReference);
            }
            return engineResource;
        }
    }

    void setDequeuedResourceCallback(DequeuedResourceCallback dequeuedResourceCallback) {
        this.cb = dequeuedResourceCallback;
    }

    void setListener(EngineResource.ResourceListener resourceListener) {
        synchronized (resourceListener) {
            synchronized (this) {
                this.listener = resourceListener;
            }
        }
    }

    void shutdown() {
        this.isShutdown = true;
        if (this.monitorClearedResourcesExecutor instanceof ExecutorService) {
            com.bumptech.glide.util.Executors.shutdownAndAwaitTermination((ExecutorService) this.monitorClearedResourcesExecutor);
        }
    }
}
