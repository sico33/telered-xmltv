package com.bumptech.glide.load.engine;

import android.os.Build;
import android.util.Log;
import androidx.core.util.Pools;
import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.pool.FactoryPools;
import com.bumptech.glide.util.pool.GlideTrace;
import com.bumptech.glide.util.pool.StateVerifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
class DecodeJob<R> implements DataFetcherGenerator.FetcherReadyCallback, Runnable, Comparable<DecodeJob<?>>, FactoryPools.Poolable {
    private static final String TAG = "DecodeJob";
    private Callback<R> callback;
    private Key currentAttemptingKey;
    private Object currentData;
    private DataSource currentDataSource;
    private DataFetcher<?> currentFetcher;
    private volatile DataFetcherGenerator currentGenerator;
    private Key currentSourceKey;
    private Thread currentThread;
    private final DiskCacheProvider diskCacheProvider;
    private DiskCacheStrategy diskCacheStrategy;
    private GlideContext glideContext;
    private int height;
    private volatile boolean isCallbackNotified;
    private volatile boolean isCancelled;
    private boolean isLoadingFromAlternateCacheKey;
    private EngineKey loadKey;
    private Object model;
    private boolean onlyRetrieveFromCache;
    private Options options;
    private int order;
    private final Pools.Pool<DecodeJob<?>> pool;
    private Priority priority;
    private RunReason runReason;
    private Key signature;
    private Stage stage;
    private long startFetchTime;
    private int width;
    private final DecodeHelper<R> decodeHelper = new DecodeHelper<>();
    private final List<Throwable> throwables = new ArrayList();
    private final StateVerifier stateVerifier = StateVerifier.newInstance();
    private final DeferredEncodeManager<?> deferredEncodeManager = new DeferredEncodeManager<>();
    private final ReleaseManager releaseManager = new ReleaseManager();

    /* JADX INFO: renamed from: com.bumptech.glide.load.engine.DecodeJob$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final int[] $SwitchMap$com$bumptech$glide$load$EncodeStrategy = new int[EncodeStrategy.values().length];
        static final int[] $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$RunReason;
        static final int[] $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$Stage;

        static {
            try {
                $SwitchMap$com$bumptech$glide$load$EncodeStrategy[EncodeStrategy.SOURCE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$bumptech$glide$load$EncodeStrategy[EncodeStrategy.TRANSFORMED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$Stage = new int[Stage.values().length];
            try {
                $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$Stage[Stage.RESOURCE_CACHE.ordinal()] = 1;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$Stage[Stage.DATA_CACHE.ordinal()] = 2;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$Stage[Stage.SOURCE.ordinal()] = 3;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$Stage[Stage.FINISHED.ordinal()] = 4;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$Stage[Stage.INITIALIZE.ordinal()] = 5;
            } catch (NoSuchFieldError e7) {
            }
            $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$RunReason = new int[RunReason.values().length];
            try {
                $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$RunReason[RunReason.INITIALIZE.ordinal()] = 1;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$RunReason[RunReason.SWITCH_TO_SOURCE_SERVICE.ordinal()] = 2;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$bumptech$glide$load$engine$DecodeJob$RunReason[RunReason.DECODE_DATA.ordinal()] = 3;
            } catch (NoSuchFieldError e10) {
            }
        }
    }

    interface Callback<R> {
        void onLoadFailed(GlideException glideException);

        void onResourceReady(Resource<R> resource, DataSource dataSource, boolean z);

        void reschedule(DecodeJob<?> decodeJob);
    }

    private final class DecodeCallback<Z> implements DecodePath.DecodeCallback<Z> {
        private final DataSource dataSource;
        final DecodeJob this$0;

        DecodeCallback(DecodeJob decodeJob, DataSource dataSource) {
            this.this$0 = decodeJob;
            this.dataSource = dataSource;
        }

        @Override // com.bumptech.glide.load.engine.DecodePath.DecodeCallback
        public Resource<Z> onResourceDecoded(Resource<Z> resource) {
            return this.this$0.onResourceDecoded(this.dataSource, resource);
        }
    }

    private static class DeferredEncodeManager<Z> {
        private ResourceEncoder<Z> encoder;
        private Key key;
        private LockedResource<Z> toEncode;

        DeferredEncodeManager() {
        }

        void clear() {
            this.key = null;
            this.encoder = null;
            this.toEncode = null;
        }

        void encode(DiskCacheProvider diskCacheProvider, Options options) {
            GlideTrace.beginSection("DecodeJob.encode");
            try {
                diskCacheProvider.getDiskCache().put(this.key, new DataCacheWriter(this.encoder, this.toEncode, options));
            } finally {
                this.toEncode.unlock();
                GlideTrace.endSection();
            }
        }

        boolean hasResourceToEncode() {
            return this.toEncode != null;
        }

        /* JADX WARN: Multi-variable type inference failed */
        <X> void init(Key key, ResourceEncoder<X> resourceEncoder, LockedResource<X> lockedResource) {
            this.key = key;
            this.encoder = resourceEncoder;
            this.toEncode = lockedResource;
        }
    }

    interface DiskCacheProvider {
        DiskCache getDiskCache();
    }

    private static class ReleaseManager {
        private boolean isEncodeComplete;
        private boolean isFailed;
        private boolean isReleased;

        ReleaseManager() {
        }

        private boolean isComplete(boolean z) {
            return (this.isFailed || z || this.isEncodeComplete) && this.isReleased;
        }

        boolean onEncodeComplete() {
            boolean zIsComplete;
            synchronized (this) {
                this.isEncodeComplete = true;
                zIsComplete = isComplete(false);
            }
            return zIsComplete;
        }

        boolean onFailed() {
            boolean zIsComplete;
            synchronized (this) {
                this.isFailed = true;
                zIsComplete = isComplete(false);
            }
            return zIsComplete;
        }

        boolean release(boolean z) {
            boolean zIsComplete;
            synchronized (this) {
                this.isReleased = true;
                zIsComplete = isComplete(z);
            }
            return zIsComplete;
        }

        void reset() {
            synchronized (this) {
                this.isEncodeComplete = false;
                this.isReleased = false;
                this.isFailed = false;
            }
        }
    }

    private enum RunReason {
        INITIALIZE,
        SWITCH_TO_SOURCE_SERVICE,
        DECODE_DATA
    }

    private enum Stage {
        INITIALIZE,
        RESOURCE_CACHE,
        DATA_CACHE,
        SOURCE,
        ENCODE,
        FINISHED
    }

    DecodeJob(DiskCacheProvider diskCacheProvider, Pools.Pool<DecodeJob<?>> pool) {
        this.diskCacheProvider = diskCacheProvider;
        this.pool = pool;
    }

    private <Data> Resource<R> decodeFromData(DataFetcher<?> dataFetcher, Data data, DataSource dataSource) throws GlideException {
        if (data == null) {
            dataFetcher.cleanup();
            return null;
        }
        try {
            long logTime = LogTime.getLogTime();
            Resource<R> resourceDecodeFromFetcher = decodeFromFetcher(data, dataSource);
            if (Log.isLoggable(TAG, 2)) {
                logWithTimeAndKey("Decoded result " + resourceDecodeFromFetcher, logTime);
            }
            return resourceDecodeFromFetcher;
        } finally {
            dataFetcher.cleanup();
        }
    }

    private <Data> Resource<R> decodeFromFetcher(Data data, DataSource dataSource) throws GlideException {
        return runLoadPath(data, dataSource, this.decodeHelper.getLoadPath(data.getClass()));
    }

    private void decodeFromRetrievedData() {
        Resource<R> resourceDecodeFromData;
        if (Log.isLoggable(TAG, 2)) {
            logWithTimeAndKey("Retrieved data", this.startFetchTime, "data: " + this.currentData + ", cache key: " + this.currentSourceKey + ", fetcher: " + this.currentFetcher);
        }
        try {
            resourceDecodeFromData = decodeFromData(this.currentFetcher, this.currentData, this.currentDataSource);
        } catch (GlideException e) {
            e.setLoggingDetails(this.currentAttemptingKey, this.currentDataSource);
            this.throwables.add(e);
            resourceDecodeFromData = null;
        }
        if (resourceDecodeFromData != null) {
            notifyEncodeAndRelease(resourceDecodeFromData, this.currentDataSource, this.isLoadingFromAlternateCacheKey);
        } else {
            runGenerators();
        }
    }

    private DataFetcherGenerator getNextGenerator() {
        switch (AnonymousClass1.$SwitchMap$com$bumptech$glide$load$engine$DecodeJob$Stage[this.stage.ordinal()]) {
            case 1:
                return new ResourceCacheGenerator(this.decodeHelper, this);
            case 2:
                return new DataCacheGenerator(this.decodeHelper, this);
            case 3:
                return new SourceGenerator(this.decodeHelper, this);
            case 4:
                return null;
            default:
                throw new IllegalStateException("Unrecognized stage: " + this.stage);
        }
    }

    private Stage getNextStage(Stage stage) {
        switch (AnonymousClass1.$SwitchMap$com$bumptech$glide$load$engine$DecodeJob$Stage[stage.ordinal()]) {
            case 1:
                return this.diskCacheStrategy.decodeCachedData() ? Stage.DATA_CACHE : getNextStage(Stage.DATA_CACHE);
            case 2:
                return this.onlyRetrieveFromCache ? Stage.FINISHED : Stage.SOURCE;
            case 3:
            case 4:
                return Stage.FINISHED;
            case 5:
                return this.diskCacheStrategy.decodeCachedResource() ? Stage.RESOURCE_CACHE : getNextStage(Stage.RESOURCE_CACHE);
            default:
                throw new IllegalArgumentException("Unrecognized stage: " + stage);
        }
    }

    private Options getOptionsWithHardwareConfig(DataSource dataSource) {
        Options options = this.options;
        if (Build.VERSION.SDK_INT < 26) {
            return options;
        }
        boolean z = dataSource == DataSource.RESOURCE_DISK_CACHE || this.decodeHelper.isScaleOnlyOrNoTransform();
        Boolean bool = (Boolean) options.get(Downsampler.ALLOW_HARDWARE_CONFIG);
        if (bool != null && (!bool.booleanValue() || z)) {
            return options;
        }
        Options options2 = new Options();
        options2.putAll(this.options);
        options2.set(Downsampler.ALLOW_HARDWARE_CONFIG, Boolean.valueOf(z));
        return options2;
    }

    private int getPriority() {
        return this.priority.ordinal();
    }

    private void logWithTimeAndKey(String str, long j) {
        logWithTimeAndKey(str, j, null);
    }

    private void logWithTimeAndKey(String str, long j, String str2) {
        Log.v(TAG, str + " in " + LogTime.getElapsedMillis(j) + ", load key: " + this.loadKey + (str2 != null ? ", " + str2 : "") + ", thread: " + Thread.currentThread().getName());
    }

    private void notifyComplete(Resource<R> resource, DataSource dataSource, boolean z) {
        setNotifiedOrThrow();
        this.callback.onResourceReady(resource, dataSource, z);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void notifyEncodeAndRelease(Resource<R> resource, DataSource dataSource, boolean z) {
        LockedResource lockedResource;
        Resource<R> resourceObtain;
        GlideTrace.beginSection("DecodeJob.notifyEncodeAndRelease");
        try {
            if (resource instanceof Initializable) {
                ((Initializable) resource).initialize();
            }
            if (this.deferredEncodeManager.hasResourceToEncode()) {
                resourceObtain = LockedResource.obtain(resource);
                lockedResource = resourceObtain;
            } else {
                lockedResource = 0;
                resourceObtain = resource;
            }
            notifyComplete(resourceObtain, dataSource, z);
            this.stage = Stage.ENCODE;
            try {
                if (this.deferredEncodeManager.hasResourceToEncode()) {
                    this.deferredEncodeManager.encode(this.diskCacheProvider, this.options);
                }
                if (lockedResource != 0) {
                    lockedResource.unlock();
                }
                onEncodeComplete();
                GlideTrace.endSection();
            } catch (Throwable th) {
                if (lockedResource != 0) {
                    lockedResource.unlock();
                }
                throw th;
            }
        } catch (Throwable th2) {
            GlideTrace.endSection();
            throw th2;
        }
    }

    private void notifyFailed() {
        setNotifiedOrThrow();
        this.callback.onLoadFailed(new GlideException("Failed to load resource", new ArrayList(this.throwables)));
        onLoadFailed();
    }

    private void onEncodeComplete() {
        if (this.releaseManager.onEncodeComplete()) {
            releaseInternal();
        }
    }

    private void onLoadFailed() {
        if (this.releaseManager.onFailed()) {
            releaseInternal();
        }
    }

    private void releaseInternal() {
        this.releaseManager.reset();
        this.deferredEncodeManager.clear();
        this.decodeHelper.clear();
        this.isCallbackNotified = false;
        this.glideContext = null;
        this.signature = null;
        this.options = null;
        this.priority = null;
        this.loadKey = null;
        this.callback = null;
        this.stage = null;
        this.currentGenerator = null;
        this.currentThread = null;
        this.currentSourceKey = null;
        this.currentData = null;
        this.currentDataSource = null;
        this.currentFetcher = null;
        this.startFetchTime = 0L;
        this.isCancelled = false;
        this.model = null;
        this.throwables.clear();
        this.pool.release(this);
    }

    private void reschedule(RunReason runReason) {
        this.runReason = runReason;
        this.callback.reschedule(this);
    }

    private void runGenerators() {
        this.currentThread = Thread.currentThread();
        this.startFetchTime = LogTime.getLogTime();
        boolean zStartNext = false;
        while (!this.isCancelled && this.currentGenerator != null && !(zStartNext = this.currentGenerator.startNext())) {
            this.stage = getNextStage(this.stage);
            this.currentGenerator = getNextGenerator();
            if (this.stage == Stage.SOURCE) {
                reschedule(RunReason.SWITCH_TO_SOURCE_SERVICE);
                return;
            }
        }
        if ((this.stage == Stage.FINISHED || this.isCancelled) && !zStartNext) {
            notifyFailed();
        }
    }

    private <Data, ResourceType> Resource<R> runLoadPath(Data data, DataSource dataSource, LoadPath<Data, ResourceType, R> loadPath) throws Throwable {
        Options optionsWithHardwareConfig = getOptionsWithHardwareConfig(dataSource);
        DataRewinder<Data> rewinder = this.glideContext.getRegistry().getRewinder(data);
        try {
            try {
                Resource<R> resourceLoad = loadPath.load(rewinder, optionsWithHardwareConfig, this.width, this.height, new DecodeCallback(this, dataSource));
                rewinder.cleanup();
                return resourceLoad;
            } catch (Throwable th) {
                th = th;
                rewinder.cleanup();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void runWrapped() {
        switch (AnonymousClass1.$SwitchMap$com$bumptech$glide$load$engine$DecodeJob$RunReason[this.runReason.ordinal()]) {
            case 1:
                this.stage = getNextStage(Stage.INITIALIZE);
                this.currentGenerator = getNextGenerator();
                runGenerators();
                return;
            case 2:
                runGenerators();
                return;
            case 3:
                decodeFromRetrievedData();
                return;
            default:
                throw new IllegalStateException("Unrecognized run reason: " + this.runReason);
        }
    }

    private void setNotifiedOrThrow() {
        this.stateVerifier.throwIfRecycled();
        if (this.isCallbackNotified) {
            throw new IllegalStateException("Already notified", this.throwables.isEmpty() ? null : this.throwables.get(this.throwables.size() - 1));
        }
        this.isCallbackNotified = true;
    }

    public void cancel() {
        this.isCancelled = true;
        DataFetcherGenerator dataFetcherGenerator = this.currentGenerator;
        if (dataFetcherGenerator != null) {
            dataFetcherGenerator.cancel();
        }
    }

    @Override // java.lang.Comparable
    public int compareTo(DecodeJob<?> decodeJob) {
        int priority = getPriority() - decodeJob.getPriority();
        return priority == 0 ? this.order - decodeJob.order : priority;
    }

    @Override // com.bumptech.glide.util.pool.FactoryPools.Poolable
    public StateVerifier getVerifier() {
        return this.stateVerifier;
    }

    DecodeJob<R> init(GlideContext glideContext, Object obj, EngineKey engineKey, Key key, int i, int i2, Class<?> cls, Class<R> cls2, Priority priority, DiskCacheStrategy diskCacheStrategy, Map<Class<?>, Transformation<?>> map, boolean z, boolean z2, boolean z3, Options options, Callback<R> callback, int i3) {
        this.decodeHelper.init(glideContext, obj, key, i, i2, diskCacheStrategy, cls, cls2, priority, options, map, z, z2, this.diskCacheProvider);
        this.glideContext = glideContext;
        this.signature = key;
        this.priority = priority;
        this.loadKey = engineKey;
        this.width = i;
        this.height = i2;
        this.diskCacheStrategy = diskCacheStrategy;
        this.onlyRetrieveFromCache = z3;
        this.options = options;
        this.callback = callback;
        this.order = i3;
        this.runReason = RunReason.INITIALIZE;
        this.model = obj;
        return this;
    }

    @Override // com.bumptech.glide.load.engine.DataFetcherGenerator.FetcherReadyCallback
    public void onDataFetcherFailed(Key key, Exception exc, DataFetcher<?> dataFetcher, DataSource dataSource) {
        dataFetcher.cleanup();
        GlideException glideException = new GlideException("Fetching data failed", exc);
        glideException.setLoggingDetails(key, dataSource, dataFetcher.getDataClass());
        this.throwables.add(glideException);
        if (Thread.currentThread() != this.currentThread) {
            reschedule(RunReason.SWITCH_TO_SOURCE_SERVICE);
        } else {
            runGenerators();
        }
    }

    @Override // com.bumptech.glide.load.engine.DataFetcherGenerator.FetcherReadyCallback
    public void onDataFetcherReady(Key key, Object obj, DataFetcher<?> dataFetcher, DataSource dataSource, Key key2) {
        this.currentSourceKey = key;
        this.currentData = obj;
        this.currentFetcher = dataFetcher;
        this.currentDataSource = dataSource;
        this.currentAttemptingKey = key2;
        this.isLoadingFromAlternateCacheKey = key != this.decodeHelper.getCacheKeys().get(0);
        if (Thread.currentThread() != this.currentThread) {
            reschedule(RunReason.DECODE_DATA);
            return;
        }
        GlideTrace.beginSection("DecodeJob.decodeFromRetrievedData");
        try {
            decodeFromRetrievedData();
        } finally {
            GlideTrace.endSection();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    <Z> Resource<Z> onResourceDecoded(DataSource dataSource, Resource<Z> resource) {
        Resource<Z> resourceTransform;
        Transformation<Z> transformation;
        EncodeStrategy encodeStrategy;
        ResourceEncoder resourceEncoder;
        Key dataCacheKey;
        Class<?> cls = resource.get().getClass();
        if (dataSource != DataSource.RESOURCE_DISK_CACHE) {
            transformation = this.decodeHelper.getTransformation(cls);
            resourceTransform = transformation.transform(this.glideContext, resource, this.width, this.height);
        } else {
            resourceTransform = resource;
            transformation = null;
        }
        if (!resource.equals(resourceTransform)) {
            resource.recycle();
        }
        if (this.decodeHelper.isResourceEncoderAvailable(resourceTransform)) {
            ResourceEncoder resultEncoder = this.decodeHelper.getResultEncoder(resourceTransform);
            encodeStrategy = resultEncoder.getEncodeStrategy(this.options);
            resourceEncoder = resultEncoder;
        } else {
            encodeStrategy = EncodeStrategy.NONE;
            resourceEncoder = null;
        }
        if (!this.diskCacheStrategy.isResourceCacheable(!this.decodeHelper.isSourceKey(this.currentSourceKey), dataSource, encodeStrategy)) {
            return resourceTransform;
        }
        if (resourceEncoder == null) {
            throw new Registry.NoResultEncoderAvailableException(resourceTransform.get().getClass());
        }
        switch (AnonymousClass1.$SwitchMap$com$bumptech$glide$load$EncodeStrategy[encodeStrategy.ordinal()]) {
            case 1:
                dataCacheKey = new DataCacheKey(this.currentSourceKey, this.signature);
                break;
            case 2:
                dataCacheKey = new ResourceCacheKey(this.decodeHelper.getArrayPool(), this.currentSourceKey, this.signature, this.width, this.height, transformation, cls, this.options);
                break;
            default:
                throw new IllegalArgumentException("Unknown strategy: " + encodeStrategy);
        }
        LockedResource lockedResourceObtain = LockedResource.obtain(resourceTransform);
        this.deferredEncodeManager.init(dataCacheKey, resourceEncoder, lockedResourceObtain);
        return lockedResourceObtain;
    }

    void release(boolean z) {
        if (this.releaseManager.release(z)) {
            releaseInternal();
        }
    }

    @Override // com.bumptech.glide.load.engine.DataFetcherGenerator.FetcherReadyCallback
    public void reschedule() {
        reschedule(RunReason.SWITCH_TO_SOURCE_SERVICE);
    }

    @Override // java.lang.Runnable
    public void run() {
        GlideTrace.beginSectionFormat("DecodeJob#run(reason=%s, model=%s)", this.runReason, this.model);
        DataFetcher<?> dataFetcher = this.currentFetcher;
        try {
            try {
                if (this.isCancelled) {
                    notifyFailed();
                    if (dataFetcher != null) {
                        dataFetcher.cleanup();
                    }
                    GlideTrace.endSection();
                    return;
                }
                runWrapped();
                if (dataFetcher != null) {
                    dataFetcher.cleanup();
                }
                GlideTrace.endSection();
            } catch (CallbackException e) {
                throw e;
            } catch (Throwable th) {
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "DecodeJob threw unexpectedly, isCancelled: " + this.isCancelled + ", stage: " + this.stage, th);
                }
                if (this.stage != Stage.ENCODE) {
                    this.throwables.add(th);
                    notifyFailed();
                }
                if (!this.isCancelled) {
                    throw th;
                }
                throw th;
            }
        } catch (Throwable th2) {
            if (dataFetcher != null) {
                dataFetcher.cleanup();
            }
            GlideTrace.endSection();
            throw th2;
        }
    }

    boolean willDecodeFromCache() {
        Stage nextStage = getNextStage(Stage.INITIALIZE);
        return nextStage == Stage.RESOURCE_CACHE || nextStage == Stage.DATA_CACHE;
    }
}
