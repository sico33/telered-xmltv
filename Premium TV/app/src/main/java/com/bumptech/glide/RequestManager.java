package com.bumptech.glide;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.manager.ConnectivityMonitor;
import com.bumptech.glide.manager.ConnectivityMonitorFactory;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.manager.TargetTracker;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Util;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: classes.dex */
public class RequestManager implements ComponentCallbacks2, LifecycleListener, ModelTypes<RequestBuilder<Drawable>> {
    private static final RequestOptions DECODE_TYPE_BITMAP = RequestOptions.decodeTypeOf(Bitmap.class).lock();
    private static final RequestOptions DECODE_TYPE_GIF = RequestOptions.decodeTypeOf(GifDrawable.class).lock();
    private static final RequestOptions DOWNLOAD_ONLY_OPTIONS = RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.DATA).priority(Priority.LOW).skipMemoryCache(true);
    private final Runnable addSelfToLifecycle;
    private boolean clearOnStop;
    private final ConnectivityMonitor connectivityMonitor;
    protected final Context context;
    private final CopyOnWriteArrayList<RequestListener<Object>> defaultRequestListeners;
    protected final Glide glide;
    final Lifecycle lifecycle;
    private boolean pauseAllRequestsOnTrimMemoryModerate;
    private RequestOptions requestOptions;
    private final RequestTracker requestTracker;
    private final TargetTracker targetTracker;
    private final RequestManagerTreeNode treeNode;

    private static class ClearTarget extends CustomViewTarget<View, Object> {
        ClearTarget(View view) {
            super(view);
        }

        @Override // com.bumptech.glide.request.target.Target
        public void onLoadFailed(Drawable drawable) {
        }

        @Override // com.bumptech.glide.request.target.CustomViewTarget
        protected void onResourceCleared(Drawable drawable) {
        }

        @Override // com.bumptech.glide.request.target.Target
        public void onResourceReady(Object obj, Transition<? super Object> transition) {
        }
    }

    private class RequestManagerConnectivityListener implements ConnectivityMonitor.ConnectivityListener {
        private final RequestTracker requestTracker;
        final RequestManager this$0;

        RequestManagerConnectivityListener(RequestManager requestManager, RequestTracker requestTracker) {
            this.this$0 = requestManager;
            this.requestTracker = requestTracker;
        }

        @Override // com.bumptech.glide.manager.ConnectivityMonitor.ConnectivityListener
        public void onConnectivityChanged(boolean z) {
            if (z) {
                synchronized (this.this$0) {
                    this.requestTracker.restartRequests();
                }
            }
        }
    }

    public RequestManager(Glide glide, Lifecycle lifecycle, RequestManagerTreeNode requestManagerTreeNode, Context context) {
        this(glide, lifecycle, requestManagerTreeNode, new RequestTracker(), glide.getConnectivityMonitorFactory(), context);
    }

    RequestManager(Glide glide, Lifecycle lifecycle, RequestManagerTreeNode requestManagerTreeNode, RequestTracker requestTracker, ConnectivityMonitorFactory connectivityMonitorFactory, Context context) {
        this.targetTracker = new TargetTracker();
        this.addSelfToLifecycle = new Runnable(this) { // from class: com.bumptech.glide.RequestManager.1
            final RequestManager this$0;

            {
                this.this$0 = this;
            }

            @Override // java.lang.Runnable
            public void run() {
                this.this$0.lifecycle.addListener(this.this$0);
            }
        };
        this.glide = glide;
        this.lifecycle = lifecycle;
        this.treeNode = requestManagerTreeNode;
        this.requestTracker = requestTracker;
        this.context = context;
        this.connectivityMonitor = connectivityMonitorFactory.build(context.getApplicationContext(), new RequestManagerConnectivityListener(this, requestTracker));
        glide.registerRequestManager(this);
        if (Util.isOnBackgroundThread()) {
            Util.postOnUiThread(this.addSelfToLifecycle);
        } else {
            lifecycle.addListener(this);
        }
        lifecycle.addListener(this.connectivityMonitor);
        this.defaultRequestListeners = new CopyOnWriteArrayList<>(glide.getGlideContext().getDefaultRequestListeners());
        setRequestOptions(glide.getGlideContext().getDefaultRequestOptions());
    }

    private void clearRequests() {
        synchronized (this) {
            Iterator<Target<?>> it = this.targetTracker.getAll().iterator();
            while (it.hasNext()) {
                clear(it.next());
            }
            this.targetTracker.clear();
        }
    }

    private void untrackOrDelegate(Target<?> target) {
        boolean zUntrack = untrack(target);
        Request request = target.getRequest();
        if (zUntrack || this.glide.removeFromManagers(target) || request == null) {
            return;
        }
        target.setRequest(null);
        request.clear();
    }

    private void updateRequestOptions(RequestOptions requestOptions) {
        synchronized (this) {
            this.requestOptions = this.requestOptions.apply(requestOptions);
        }
    }

    public RequestManager addDefaultRequestListener(RequestListener<Object> requestListener) {
        this.defaultRequestListeners.add(requestListener);
        return this;
    }

    public RequestManager applyDefaultRequestOptions(RequestOptions requestOptions) {
        synchronized (this) {
            updateRequestOptions(requestOptions);
        }
        return this;
    }

    public <ResourceType> RequestBuilder<ResourceType> as(Class<ResourceType> cls) {
        return new RequestBuilder<>(this.glide, this, cls, this.context);
    }

    public RequestBuilder<Bitmap> asBitmap() {
        return as(Bitmap.class).apply((BaseRequestOptions<?>) DECODE_TYPE_BITMAP);
    }

    public RequestBuilder<Drawable> asDrawable() {
        return as(Drawable.class);
    }

    public RequestBuilder<File> asFile() {
        return as(File.class).apply((BaseRequestOptions<?>) RequestOptions.skipMemoryCacheOf(true));
    }

    public RequestBuilder<GifDrawable> asGif() {
        return as(GifDrawable.class).apply((BaseRequestOptions<?>) DECODE_TYPE_GIF);
    }

    public void clear(View view) {
        clear(new ClearTarget(view));
    }

    public void clear(Target<?> target) {
        if (target == null) {
            return;
        }
        untrackOrDelegate(target);
    }

    public RequestManager clearOnStop() {
        synchronized (this) {
            this.clearOnStop = true;
        }
        return this;
    }

    public RequestBuilder<File> download(Object obj) {
        return downloadOnly().load(obj);
    }

    public RequestBuilder<File> downloadOnly() {
        return as(File.class).apply((BaseRequestOptions<?>) DOWNLOAD_ONLY_OPTIONS);
    }

    List<RequestListener<Object>> getDefaultRequestListeners() {
        return this.defaultRequestListeners;
    }

    RequestOptions getDefaultRequestOptions() {
        RequestOptions requestOptions;
        synchronized (this) {
            requestOptions = this.requestOptions;
        }
        return requestOptions;
    }

    <T> TransitionOptions<?, T> getDefaultTransitionOptions(Class<T> cls) {
        return this.glide.getGlideContext().getDefaultTransitionOptions(cls);
    }

    public boolean isPaused() {
        boolean zIsPaused;
        synchronized (this) {
            zIsPaused = this.requestTracker.isPaused();
        }
        return zIsPaused;
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<Drawable> load(Bitmap bitmap) {
        return asDrawable().load(bitmap);
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<Drawable> load(Drawable drawable) {
        return asDrawable().load(drawable);
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<Drawable> load(Uri uri) {
        return asDrawable().load(uri);
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<Drawable> load(File file) {
        return asDrawable().load(file);
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<Drawable> load(Integer num) {
        return asDrawable().load(num);
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<Drawable> load(Object obj) {
        return asDrawable().load(obj);
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<Drawable> load(String str) {
        return asDrawable().load(str);
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.ModelTypes
    @Deprecated
    public RequestBuilder<Drawable> load(URL url) {
        return asDrawable().load(url);
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<Drawable> load(byte[] bArr) {
        return asDrawable().load(bArr);
    }

    @Override // android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
    }

    @Override // com.bumptech.glide.manager.LifecycleListener
    public void onDestroy() {
        synchronized (this) {
            this.targetTracker.onDestroy();
            clearRequests();
            this.requestTracker.clearRequests();
            this.lifecycle.removeListener(this);
            this.lifecycle.removeListener(this.connectivityMonitor);
            Util.removeCallbacksOnUiThread(this.addSelfToLifecycle);
            this.glide.unregisterRequestManager(this);
        }
    }

    @Override // android.content.ComponentCallbacks
    public void onLowMemory() {
    }

    @Override // com.bumptech.glide.manager.LifecycleListener
    public void onStart() {
        synchronized (this) {
            resumeRequests();
            this.targetTracker.onStart();
        }
    }

    @Override // com.bumptech.glide.manager.LifecycleListener
    public void onStop() {
        synchronized (this) {
            this.targetTracker.onStop();
            if (this.clearOnStop) {
                clearRequests();
            } else {
                pauseRequests();
            }
        }
    }

    @Override // android.content.ComponentCallbacks2
    public void onTrimMemory(int i) {
        if (i == 60 && this.pauseAllRequestsOnTrimMemoryModerate) {
            pauseAllRequestsRecursive();
        }
    }

    public void pauseAllRequests() {
        synchronized (this) {
            this.requestTracker.pauseAllRequests();
        }
    }

    public void pauseAllRequestsRecursive() {
        synchronized (this) {
            pauseAllRequests();
            Iterator<RequestManager> it = this.treeNode.getDescendants().iterator();
            while (it.hasNext()) {
                it.next().pauseAllRequests();
            }
        }
    }

    public void pauseRequests() {
        synchronized (this) {
            this.requestTracker.pauseRequests();
        }
    }

    public void pauseRequestsRecursive() {
        synchronized (this) {
            pauseRequests();
            Iterator<RequestManager> it = this.treeNode.getDescendants().iterator();
            while (it.hasNext()) {
                it.next().pauseRequests();
            }
        }
    }

    public void resumeRequests() {
        synchronized (this) {
            this.requestTracker.resumeRequests();
        }
    }

    public void resumeRequestsRecursive() {
        synchronized (this) {
            Util.assertMainThread();
            resumeRequests();
            Iterator<RequestManager> it = this.treeNode.getDescendants().iterator();
            while (it.hasNext()) {
                it.next().resumeRequests();
            }
        }
    }

    public RequestManager setDefaultRequestOptions(RequestOptions requestOptions) {
        synchronized (this) {
            setRequestOptions(requestOptions);
        }
        return this;
    }

    public void setPauseAllRequestsOnTrimMemoryModerate(boolean z) {
        this.pauseAllRequestsOnTrimMemoryModerate = z;
    }

    protected void setRequestOptions(RequestOptions requestOptions) {
        synchronized (this) {
            this.requestOptions = requestOptions.mo182clone().autoClone();
        }
    }

    public String toString() {
        String str;
        synchronized (this) {
            str = super.toString() + "{tracker=" + this.requestTracker + ", treeNode=" + this.treeNode + "}";
        }
        return str;
    }

    void track(Target<?> target, Request request) {
        synchronized (this) {
            this.targetTracker.track(target);
            this.requestTracker.runRequest(request);
        }
    }

    boolean untrack(Target<?> target) {
        synchronized (this) {
            Request request = target.getRequest();
            if (request == null) {
                return true;
            }
            if (!this.requestTracker.clearAndRemove(request)) {
                return false;
            }
            this.targetTracker.untrack(target);
            target.setRequest(null);
            return true;
        }
    }
}
