package com.bumptech.glide;

import android.content.Context;
import android.content.ContextWrapper;
import android.widget.ImageView;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.util.GlideSuppliers;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class GlideContext extends ContextWrapper {
    static final TransitionOptions<?, ?> DEFAULT_TRANSITION_OPTIONS = new GenericTransitionOptions();
    private final ArrayPool arrayPool;
    private final List<RequestListener<Object>> defaultRequestListeners;
    private RequestOptions defaultRequestOptions;
    private final Glide.RequestOptionsFactory defaultRequestOptionsFactory;
    private final Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions;
    private final Engine engine;
    private final GlideExperiments experiments;
    private final ImageViewTargetFactory imageViewTargetFactory;
    private final int logLevel;
    private final GlideSuppliers.GlideSupplier<Registry> registry;

    public GlideContext(Context context, ArrayPool arrayPool, GlideSuppliers.GlideSupplier<Registry> glideSupplier, ImageViewTargetFactory imageViewTargetFactory, Glide.RequestOptionsFactory requestOptionsFactory, Map<Class<?>, TransitionOptions<?, ?>> map, List<RequestListener<Object>> list, Engine engine, GlideExperiments glideExperiments, int i) {
        super(context.getApplicationContext());
        this.arrayPool = arrayPool;
        this.imageViewTargetFactory = imageViewTargetFactory;
        this.defaultRequestOptionsFactory = requestOptionsFactory;
        this.defaultRequestListeners = list;
        this.defaultTransitionOptions = map;
        this.engine = engine;
        this.experiments = glideExperiments;
        this.logLevel = i;
        this.registry = GlideSuppliers.memorize(glideSupplier);
    }

    public <X> ViewTarget<ImageView, X> buildImageViewTarget(ImageView imageView, Class<X> cls) {
        return this.imageViewTargetFactory.buildTarget(imageView, cls);
    }

    public ArrayPool getArrayPool() {
        return this.arrayPool;
    }

    public List<RequestListener<Object>> getDefaultRequestListeners() {
        return this.defaultRequestListeners;
    }

    public RequestOptions getDefaultRequestOptions() {
        RequestOptions requestOptions;
        synchronized (this) {
            if (this.defaultRequestOptions == null) {
                this.defaultRequestOptions = this.defaultRequestOptionsFactory.build().lock();
            }
            requestOptions = this.defaultRequestOptions;
        }
        return requestOptions;
    }

    public <T> TransitionOptions<?, T> getDefaultTransitionOptions(Class<T> cls) {
        TransitionOptions<?, T> transitionOptions;
        TransitionOptions<?, T> transitionOptions2 = (TransitionOptions) this.defaultTransitionOptions.get(cls);
        if (transitionOptions2 == null) {
            Iterator<Map.Entry<Class<?>, TransitionOptions<?, ?>>> it = this.defaultTransitionOptions.entrySet().iterator();
            while (true) {
                transitionOptions = transitionOptions2;
                if (!it.hasNext()) {
                    break;
                }
                Map.Entry<Class<?>, TransitionOptions<?, ?>> next = it.next();
                transitionOptions2 = next.getKey().isAssignableFrom(cls) ? (TransitionOptions) next.getValue() : transitionOptions;
            }
            transitionOptions2 = transitionOptions;
        }
        return transitionOptions2 == null ? (TransitionOptions<?, T>) DEFAULT_TRANSITION_OPTIONS : transitionOptions2;
    }

    public Engine getEngine() {
        return this.engine;
    }

    public GlideExperiments getExperiments() {
        return this.experiments;
    }

    public int getLogLevel() {
        return this.logLevel;
    }

    public Registry getRegistry() {
        return this.registry.get();
    }
}
