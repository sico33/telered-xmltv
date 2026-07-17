package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.request.ErrorRequestCoordinator;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.RequestFutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.SingleRequest;
import com.bumptech.glide.request.ThumbnailRequestCoordinator;
import com.bumptech.glide.request.target.PreloadTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.signature.AndroidResourceSignature;
import com.bumptech.glide.util.Executors;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public class RequestBuilder<TranscodeType> extends BaseRequestOptions<RequestBuilder<TranscodeType>> implements Cloneable, ModelTypes<RequestBuilder<TranscodeType>> {
    protected static final RequestOptions DOWNLOAD_ONLY_OPTIONS = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.DATA).priority(Priority.LOW).skipMemoryCache(true);
    private final Context context;
    private RequestBuilder<TranscodeType> errorBuilder;
    private final Glide glide;
    private final GlideContext glideContext;
    private boolean isDefaultTransitionOptionsSet;
    private boolean isModelSet;
    private boolean isThumbnailBuilt;
    private Object model;
    private List<RequestListener<TranscodeType>> requestListeners;
    private final RequestManager requestManager;
    private Float thumbSizeMultiplier;
    private RequestBuilder<TranscodeType> thumbnailBuilder;
    private final Class<TranscodeType> transcodeClass;
    private TransitionOptions<?, ? super TranscodeType> transitionOptions;

    /* JADX INFO: renamed from: com.bumptech.glide.RequestBuilder$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final int[] $SwitchMap$android$widget$ImageView$ScaleType;
        static final int[] $SwitchMap$com$bumptech$glide$Priority = new int[Priority.values().length];

        static {
            try {
                $SwitchMap$com$bumptech$glide$Priority[Priority.LOW.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$bumptech$glide$Priority[Priority.NORMAL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$bumptech$glide$Priority[Priority.HIGH.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$bumptech$glide$Priority[Priority.IMMEDIATE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            $SwitchMap$android$widget$ImageView$ScaleType = new int[ImageView.ScaleType.values().length];
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.CENTER_CROP.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.CENTER_INSIDE.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.FIT_CENTER.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.FIT_START.ordinal()] = 4;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.FIT_END.ordinal()] = 5;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.FIT_XY.ordinal()] = 6;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.CENTER.ordinal()] = 7;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.MATRIX.ordinal()] = 8;
            } catch (NoSuchFieldError e12) {
            }
        }
    }

    protected RequestBuilder(Glide glide, RequestManager requestManager, Class<TranscodeType> cls, Context context) {
        this.isDefaultTransitionOptionsSet = true;
        this.glide = glide;
        this.requestManager = requestManager;
        this.transcodeClass = cls;
        this.context = context;
        this.transitionOptions = requestManager.getDefaultTransitionOptions(cls);
        this.glideContext = glide.getGlideContext();
        initRequestListeners(requestManager.getDefaultRequestListeners());
        apply((BaseRequestOptions<?>) requestManager.getDefaultRequestOptions());
    }

    protected RequestBuilder(Class<TranscodeType> cls, RequestBuilder<?> requestBuilder) {
        this(requestBuilder.glide, requestBuilder.requestManager, cls, requestBuilder.context);
        this.model = requestBuilder.model;
        this.isModelSet = requestBuilder.isModelSet;
        apply((BaseRequestOptions<?>) requestBuilder);
    }

    private RequestBuilder<TranscodeType> applyResourceThemeAndSignature(RequestBuilder<TranscodeType> requestBuilder) {
        return requestBuilder.theme(this.context.getTheme()).signature(AndroidResourceSignature.obtain(this.context));
    }

    private Request buildRequest(Target<TranscodeType> target, RequestListener<TranscodeType> requestListener, BaseRequestOptions<?> baseRequestOptions, Executor executor) {
        return buildRequestRecursive(new Object(), target, requestListener, null, this.transitionOptions, baseRequestOptions.getPriority(), baseRequestOptions.getOverrideWidth(), baseRequestOptions.getOverrideHeight(), baseRequestOptions, executor);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private Request buildRequestRecursive(Object obj, Target<TranscodeType> target, RequestListener<TranscodeType> requestListener, RequestCoordinator requestCoordinator, TransitionOptions<?, ? super TranscodeType> transitionOptions, Priority priority, int i, int i2, BaseRequestOptions<?> baseRequestOptions, Executor executor) {
        ErrorRequestCoordinator errorRequestCoordinator;
        RequestCoordinator errorRequestCoordinator2;
        if (this.errorBuilder != null) {
            errorRequestCoordinator2 = new ErrorRequestCoordinator(obj, requestCoordinator);
            errorRequestCoordinator = errorRequestCoordinator2;
        } else {
            errorRequestCoordinator = 0;
            errorRequestCoordinator2 = requestCoordinator;
        }
        Request requestBuildThumbnailRequestRecursive = buildThumbnailRequestRecursive(obj, target, requestListener, errorRequestCoordinator2, transitionOptions, priority, i, i2, baseRequestOptions, executor);
        if (errorRequestCoordinator == 0) {
            return requestBuildThumbnailRequestRecursive;
        }
        int overrideWidth = this.errorBuilder.getOverrideWidth();
        int overrideHeight = this.errorBuilder.getOverrideHeight();
        if (Util.isValidDimensions(i, i2) && !this.errorBuilder.isValidOverride()) {
            overrideWidth = baseRequestOptions.getOverrideWidth();
            overrideHeight = baseRequestOptions.getOverrideHeight();
        }
        errorRequestCoordinator.setRequests(requestBuildThumbnailRequestRecursive, this.errorBuilder.buildRequestRecursive(obj, target, requestListener, errorRequestCoordinator, this.errorBuilder.transitionOptions, this.errorBuilder.getPriority(), overrideWidth, overrideHeight, this.errorBuilder, executor));
        return errorRequestCoordinator;
    }

    private Request buildThumbnailRequestRecursive(Object obj, Target<TranscodeType> target, RequestListener<TranscodeType> requestListener, RequestCoordinator requestCoordinator, TransitionOptions<?, ? super TranscodeType> transitionOptions, Priority priority, int i, int i2, BaseRequestOptions<?> baseRequestOptions, Executor executor) {
        int overrideWidth;
        int overrideHeight;
        if (this.thumbnailBuilder == null) {
            if (this.thumbSizeMultiplier == null) {
                return obtainRequest(obj, target, requestListener, baseRequestOptions, requestCoordinator, transitionOptions, priority, i, i2, executor);
            }
            ThumbnailRequestCoordinator thumbnailRequestCoordinator = new ThumbnailRequestCoordinator(obj, requestCoordinator);
            thumbnailRequestCoordinator.setRequests(obtainRequest(obj, target, requestListener, baseRequestOptions, thumbnailRequestCoordinator, transitionOptions, priority, i, i2, executor), obtainRequest(obj, target, requestListener, baseRequestOptions.mo182clone().sizeMultiplier(this.thumbSizeMultiplier.floatValue()), thumbnailRequestCoordinator, transitionOptions, getThumbnailPriority(priority), i, i2, executor));
            return thumbnailRequestCoordinator;
        }
        if (this.isThumbnailBuilt) {
            throw new IllegalStateException("You cannot use a request as both the main request and a thumbnail, consider using clone() on the request(s) passed to thumbnail()");
        }
        TransitionOptions<?, ? super TranscodeType> transitionOptions2 = this.thumbnailBuilder.isDefaultTransitionOptionsSet ? transitionOptions : this.thumbnailBuilder.transitionOptions;
        Priority priority2 = this.thumbnailBuilder.isPrioritySet() ? this.thumbnailBuilder.getPriority() : getThumbnailPriority(priority);
        int overrideWidth2 = this.thumbnailBuilder.getOverrideWidth();
        int overrideHeight2 = this.thumbnailBuilder.getOverrideHeight();
        if (!Util.isValidDimensions(i, i2) || this.thumbnailBuilder.isValidOverride()) {
            overrideWidth = overrideWidth2;
            overrideHeight = overrideHeight2;
        } else {
            overrideWidth = baseRequestOptions.getOverrideWidth();
            overrideHeight = baseRequestOptions.getOverrideHeight();
        }
        ThumbnailRequestCoordinator thumbnailRequestCoordinator2 = new ThumbnailRequestCoordinator(obj, requestCoordinator);
        Request requestObtainRequest = obtainRequest(obj, target, requestListener, baseRequestOptions, thumbnailRequestCoordinator2, transitionOptions, priority, i, i2, executor);
        this.isThumbnailBuilt = true;
        Request requestBuildRequestRecursive = this.thumbnailBuilder.buildRequestRecursive(obj, target, requestListener, thumbnailRequestCoordinator2, transitionOptions2, priority2, overrideWidth, overrideHeight, this.thumbnailBuilder, executor);
        this.isThumbnailBuilt = false;
        thumbnailRequestCoordinator2.setRequests(requestObtainRequest, requestBuildRequestRecursive);
        return thumbnailRequestCoordinator2;
    }

    private RequestBuilder<TranscodeType> cloneWithNullErrorAndThumbnail() {
        return mo182clone().error((RequestBuilder) null).thumbnail((RequestBuilder) null);
    }

    private Priority getThumbnailPriority(Priority priority) {
        switch (AnonymousClass1.$SwitchMap$com$bumptech$glide$Priority[priority.ordinal()]) {
            case 1:
                return Priority.NORMAL;
            case 2:
                return Priority.HIGH;
            case 3:
            case 4:
                return Priority.IMMEDIATE;
            default:
                throw new IllegalArgumentException("unknown priority: " + getPriority());
        }
    }

    private void initRequestListeners(List<RequestListener<Object>> list) {
        Iterator<RequestListener<Object>> it = list.iterator();
        while (it.hasNext()) {
            addListener((RequestListener) it.next());
        }
    }

    private <Y extends Target<TranscodeType>> Y into(Y y, RequestListener<TranscodeType> requestListener, BaseRequestOptions<?> baseRequestOptions, Executor executor) {
        Preconditions.checkNotNull(y);
        if (!this.isModelSet) {
            throw new IllegalArgumentException("You must call #load() before calling #into()");
        }
        Request requestBuildRequest = buildRequest(y, requestListener, baseRequestOptions, executor);
        Request request = y.getRequest();
        if (!requestBuildRequest.isEquivalentTo(request) || isSkipMemoryCacheWithCompletePreviousRequest(baseRequestOptions, request)) {
            this.requestManager.clear((Target<?>) y);
            y.setRequest(requestBuildRequest);
            this.requestManager.track(y, requestBuildRequest);
        } else if (!((Request) Preconditions.checkNotNull(request)).isRunning()) {
            request.begin();
        }
        return y;
    }

    private boolean isSkipMemoryCacheWithCompletePreviousRequest(BaseRequestOptions<?> baseRequestOptions, Request request) {
        return !baseRequestOptions.isMemoryCacheable() && request.isComplete();
    }

    private RequestBuilder<TranscodeType> loadGeneric(Object obj) {
        if (isAutoCloneEnabled()) {
            return mo182clone().loadGeneric(obj);
        }
        this.model = obj;
        this.isModelSet = true;
        return selfOrThrowIfLocked();
    }

    private RequestBuilder<TranscodeType> maybeApplyOptionsResourceUri(Uri uri, RequestBuilder<TranscodeType> requestBuilder) {
        return (uri == null || !"android.resource".equals(uri.getScheme())) ? requestBuilder : applyResourceThemeAndSignature(requestBuilder);
    }

    private Request obtainRequest(Object obj, Target<TranscodeType> target, RequestListener<TranscodeType> requestListener, BaseRequestOptions<?> baseRequestOptions, RequestCoordinator requestCoordinator, TransitionOptions<?, ? super TranscodeType> transitionOptions, Priority priority, int i, int i2, Executor executor) {
        return SingleRequest.obtain(this.context, this.glideContext, obj, this.model, this.transcodeClass, baseRequestOptions, i, i2, priority, target, requestListener, this.requestListeners, requestCoordinator, this.glideContext.getEngine(), transitionOptions.getTransitionFactory(), executor);
    }

    public RequestBuilder<TranscodeType> addListener(RequestListener<TranscodeType> requestListener) {
        if (isAutoCloneEnabled()) {
            return mo182clone().addListener(requestListener);
        }
        if (requestListener != null) {
            if (this.requestListeners == null) {
                this.requestListeners = new ArrayList();
            }
            this.requestListeners.add(requestListener);
        }
        return selfOrThrowIfLocked();
    }

    @Override // com.bumptech.glide.request.BaseRequestOptions
    public RequestBuilder<TranscodeType> apply(BaseRequestOptions<?> baseRequestOptions) {
        Preconditions.checkNotNull(baseRequestOptions);
        return (RequestBuilder) super.apply(baseRequestOptions);
    }

    @Override // com.bumptech.glide.request.BaseRequestOptions
    public /* bridge */ /* synthetic */ BaseRequestOptions apply(BaseRequestOptions baseRequestOptions) {
        return apply((BaseRequestOptions<?>) baseRequestOptions);
    }

    @Override // com.bumptech.glide.request.BaseRequestOptions
    /* JADX INFO: renamed from: clone */
    public RequestBuilder<TranscodeType> mo182clone() {
        RequestBuilder<TranscodeType> requestBuilder = (RequestBuilder) super.mo182clone();
        requestBuilder.transitionOptions = requestBuilder.transitionOptions.m183clone();
        if (requestBuilder.requestListeners != null) {
            requestBuilder.requestListeners = new ArrayList(requestBuilder.requestListeners);
        }
        if (requestBuilder.thumbnailBuilder != null) {
            requestBuilder.thumbnailBuilder = requestBuilder.thumbnailBuilder.mo182clone();
        }
        if (requestBuilder.errorBuilder != null) {
            requestBuilder.errorBuilder = requestBuilder.errorBuilder.mo182clone();
        }
        return requestBuilder;
    }

    @Deprecated
    public FutureTarget<File> downloadOnly(int i, int i2) {
        return getDownloadOnlyRequest().submit(i, i2);
    }

    @Deprecated
    public <Y extends Target<File>> Y downloadOnly(Y y) {
        return (Y) getDownloadOnlyRequest().into(y);
    }

    @Override // com.bumptech.glide.request.BaseRequestOptions
    public boolean equals(Object obj) {
        if (!(obj instanceof RequestBuilder)) {
            return false;
        }
        RequestBuilder requestBuilder = (RequestBuilder) obj;
        return super.equals(requestBuilder) && Objects.equals(this.transcodeClass, requestBuilder.transcodeClass) && this.transitionOptions.equals(requestBuilder.transitionOptions) && Objects.equals(this.model, requestBuilder.model) && Objects.equals(this.requestListeners, requestBuilder.requestListeners) && Objects.equals(this.thumbnailBuilder, requestBuilder.thumbnailBuilder) && Objects.equals(this.errorBuilder, requestBuilder.errorBuilder) && Objects.equals(this.thumbSizeMultiplier, requestBuilder.thumbSizeMultiplier) && this.isDefaultTransitionOptionsSet == requestBuilder.isDefaultTransitionOptionsSet && this.isModelSet == requestBuilder.isModelSet;
    }

    public RequestBuilder<TranscodeType> error(RequestBuilder<TranscodeType> requestBuilder) {
        if (isAutoCloneEnabled()) {
            return mo182clone().error((RequestBuilder) requestBuilder);
        }
        this.errorBuilder = requestBuilder;
        return selfOrThrowIfLocked();
    }

    public RequestBuilder<TranscodeType> error(Object obj) {
        if (obj != null) {
            return error((RequestBuilder) cloneWithNullErrorAndThumbnail().load(obj));
        }
        return error((RequestBuilder) null);
    }

    protected RequestBuilder<File> getDownloadOnlyRequest() {
        return new RequestBuilder(File.class, this).apply((BaseRequestOptions<?>) DOWNLOAD_ONLY_OPTIONS);
    }

    Object getModel() {
        return this.model;
    }

    RequestManager getRequestManager() {
        return this.requestManager;
    }

    @Override // com.bumptech.glide.request.BaseRequestOptions
    public int hashCode() {
        return Util.hashCode(this.isModelSet, Util.hashCode(this.isDefaultTransitionOptionsSet, Util.hashCode(this.thumbSizeMultiplier, Util.hashCode(this.errorBuilder, Util.hashCode(this.thumbnailBuilder, Util.hashCode(this.requestListeners, Util.hashCode(this.model, Util.hashCode(this.transitionOptions, Util.hashCode(this.transcodeClass, super.hashCode())))))))));
    }

    @Deprecated
    public FutureTarget<TranscodeType> into(int i, int i2) {
        return submit(i, i2);
    }

    public <Y extends Target<TranscodeType>> Y into(Y y) {
        return (Y) into(y, null, Executors.mainThreadExecutor());
    }

    <Y extends Target<TranscodeType>> Y into(Y y, RequestListener<TranscodeType> requestListener, Executor executor) {
        return (Y) into(y, requestListener, this, executor);
    }

    public ViewTarget<ImageView, TranscodeType> into(ImageView imageView) {
        BaseRequestOptions baseRequestOptionsOptionalCenterCrop;
        Util.assertMainThread();
        Preconditions.checkNotNull(imageView);
        if (!isTransformationSet() && isTransformationAllowed() && imageView.getScaleType() != null) {
            switch (AnonymousClass1.$SwitchMap$android$widget$ImageView$ScaleType[imageView.getScaleType().ordinal()]) {
                case 1:
                    baseRequestOptionsOptionalCenterCrop = mo182clone().optionalCenterCrop();
                    break;
                case 2:
                    baseRequestOptionsOptionalCenterCrop = mo182clone().optionalCenterInside();
                    break;
                case 3:
                case 4:
                case 5:
                    baseRequestOptionsOptionalCenterCrop = mo182clone().optionalFitCenter();
                    break;
                case 6:
                    baseRequestOptionsOptionalCenterCrop = mo182clone().optionalCenterInside();
                    break;
                default:
                    baseRequestOptionsOptionalCenterCrop = this;
                    break;
            }
        } else {
            baseRequestOptionsOptionalCenterCrop = this;
        }
        return (ViewTarget) into(this.glideContext.buildImageViewTarget(imageView, this.transcodeClass), null, baseRequestOptionsOptionalCenterCrop, Executors.mainThreadExecutor());
    }

    public RequestBuilder<TranscodeType> listener(RequestListener<TranscodeType> requestListener) {
        if (isAutoCloneEnabled()) {
            return mo182clone().listener(requestListener);
        }
        this.requestListeners = null;
        return addListener(requestListener);
    }

    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<TranscodeType> load(Bitmap bitmap) {
        return loadGeneric(bitmap).apply((BaseRequestOptions<?>) RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE));
    }

    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<TranscodeType> load(Drawable drawable) {
        return loadGeneric(drawable).apply((BaseRequestOptions<?>) RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE));
    }

    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<TranscodeType> load(Uri uri) {
        return maybeApplyOptionsResourceUri(uri, loadGeneric(uri));
    }

    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<TranscodeType> load(File file) {
        return loadGeneric(file);
    }

    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<TranscodeType> load(Integer num) {
        return applyResourceThemeAndSignature(loadGeneric(num));
    }

    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<TranscodeType> load(Object obj) {
        return loadGeneric(obj);
    }

    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<TranscodeType> load(String str) {
        return loadGeneric(str);
    }

    @Override // com.bumptech.glide.ModelTypes
    @Deprecated
    public RequestBuilder<TranscodeType> load(URL url) {
        return loadGeneric(url);
    }

    @Override // com.bumptech.glide.ModelTypes
    public RequestBuilder<TranscodeType> load(byte[] bArr) {
        RequestBuilder<TranscodeType> requestBuilderLoadGeneric = loadGeneric(bArr);
        if (!requestBuilderLoadGeneric.isDiskCacheStrategySet()) {
            requestBuilderLoadGeneric = requestBuilderLoadGeneric.apply((BaseRequestOptions<?>) RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE));
        }
        return !requestBuilderLoadGeneric.isSkipMemoryCacheSet() ? requestBuilderLoadGeneric.apply((BaseRequestOptions<?>) RequestOptions.skipMemoryCacheOf(true)) : requestBuilderLoadGeneric;
    }

    public Target<TranscodeType> preload() {
        return preload(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public Target<TranscodeType> preload(int i, int i2) {
        return into(PreloadTarget.obtain(this.requestManager, i, i2));
    }

    public FutureTarget<TranscodeType> submit() {
        return submit(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public FutureTarget<TranscodeType> submit(int i, int i2) {
        RequestFutureTarget requestFutureTarget = new RequestFutureTarget(i, i2);
        return (FutureTarget) into(requestFutureTarget, requestFutureTarget, Executors.directExecutor());
    }

    @Deprecated
    public RequestBuilder<TranscodeType> thumbnail(float f) {
        if (isAutoCloneEnabled()) {
            return mo182clone().thumbnail(f);
        }
        if (f < 0.0f || f > 1.0f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.thumbSizeMultiplier = Float.valueOf(f);
        return selfOrThrowIfLocked();
    }

    public RequestBuilder<TranscodeType> thumbnail(RequestBuilder<TranscodeType> requestBuilder) {
        if (isAutoCloneEnabled()) {
            return mo182clone().thumbnail(requestBuilder);
        }
        this.thumbnailBuilder = requestBuilder;
        return selfOrThrowIfLocked();
    }

    public RequestBuilder<TranscodeType> thumbnail(List<RequestBuilder<TranscodeType>> list) {
        RequestBuilder<TranscodeType> requestBuilder = null;
        if (list == null || list.isEmpty()) {
            return thumbnail((RequestBuilder) null);
        }
        int size = list.size() - 1;
        while (size >= 0) {
            RequestBuilder<TranscodeType> requestBuilderThumbnail = list.get(size);
            if (requestBuilderThumbnail == null) {
                requestBuilderThumbnail = requestBuilder;
            } else if (requestBuilder != null) {
                requestBuilderThumbnail = requestBuilderThumbnail.thumbnail(requestBuilder);
            }
            size--;
            requestBuilder = requestBuilderThumbnail;
        }
        return thumbnail(requestBuilder);
    }

    public RequestBuilder<TranscodeType> thumbnail(RequestBuilder<TranscodeType>... requestBuilderArr) {
        if (requestBuilderArr != null && requestBuilderArr.length != 0) {
            return thumbnail(Arrays.asList(requestBuilderArr));
        }
        return thumbnail((RequestBuilder) null);
    }

    public RequestBuilder<TranscodeType> transition(TransitionOptions<?, ? super TranscodeType> transitionOptions) {
        if (isAutoCloneEnabled()) {
            return mo182clone().transition(transitionOptions);
        }
        this.transitionOptions = (TransitionOptions) Preconditions.checkNotNull(transitionOptions);
        this.isDefaultTransitionOptionsSet = false;
        return selfOrThrowIfLocked();
    }
}
