package com.google.common.cache;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class CacheLoader<K, V> {

    /* JADX INFO: renamed from: com.google.common.cache.CacheLoader$1, reason: invalid class name */
    class AnonymousClass1 extends CacheLoader<K, V> {
        final Executor val$executor;
        final CacheLoader val$loader;

        AnonymousClass1(CacheLoader cacheLoader, Executor executor) {
            this.val$loader = cacheLoader;
            this.val$executor = executor;
        }

        @Override // com.google.common.cache.CacheLoader
        public V load(K k) throws Exception {
            return (V) this.val$loader.load(k);
        }

        @Override // com.google.common.cache.CacheLoader
        public Map<K, V> loadAll(Iterable<? extends K> iterable) throws Exception {
            return this.val$loader.loadAll(iterable);
        }

        @Override // com.google.common.cache.CacheLoader
        public ListenableFuture<V> reload(final K k, final V v) {
            final CacheLoader cacheLoader = this.val$loader;
            ListenableFutureTask listenableFutureTaskCreate = ListenableFutureTask.create(new Callable(cacheLoader, k, v) { // from class: com.google.common.cache.CacheLoader$1$$ExternalSyntheticLambda0
                public final CacheLoader f$0;
                public final Object f$1;
                public final Object f$2;

                {
                    this.f$0 = cacheLoader;
                    this.f$1 = k;
                    this.f$2 = v;
                }

                @Override // java.util.concurrent.Callable
                public final Object call() {
                    return this.f$0.reload(this.f$1, this.f$2).get();
                }
            });
            this.val$executor.execute(listenableFutureTaskCreate);
            return listenableFutureTaskCreate;
        }
    }

    private static final class FunctionToCacheLoader<K, V> extends CacheLoader<K, V> implements Serializable {
        private static final long serialVersionUID = 0;
        private final Function<K, V> computingFunction;

        public FunctionToCacheLoader(Function<K, V> function) {
            this.computingFunction = (Function) Preconditions.checkNotNull(function);
        }

        @Override // com.google.common.cache.CacheLoader
        public V load(K k) {
            return this.computingFunction.apply((K) Preconditions.checkNotNull(k));
        }
    }

    public static final class InvalidCacheLoadException extends RuntimeException {
        public InvalidCacheLoadException(String str) {
            super(str);
        }
    }

    private static final class SupplierToCacheLoader<V> extends CacheLoader<Object, V> implements Serializable {
        private static final long serialVersionUID = 0;
        private final Supplier<V> computingSupplier;

        public SupplierToCacheLoader(Supplier<V> supplier) {
            this.computingSupplier = (Supplier) Preconditions.checkNotNull(supplier);
        }

        @Override // com.google.common.cache.CacheLoader
        public V load(Object obj) {
            Preconditions.checkNotNull(obj);
            return this.computingSupplier.get();
        }
    }

    public static final class UnsupportedLoadingOperationException extends UnsupportedOperationException {
        UnsupportedLoadingOperationException() {
        }
    }

    protected CacheLoader() {
    }

    public static <K, V> CacheLoader<K, V> asyncReloading(CacheLoader<K, V> cacheLoader, Executor executor) {
        Preconditions.checkNotNull(cacheLoader);
        Preconditions.checkNotNull(executor);
        return new AnonymousClass1(cacheLoader, executor);
    }

    public static <K, V> CacheLoader<K, V> from(Function<K, V> function) {
        return new FunctionToCacheLoader(function);
    }

    public static <V> CacheLoader<Object, V> from(Supplier<V> supplier) {
        return new SupplierToCacheLoader(supplier);
    }

    public abstract V load(K k) throws Exception;

    public Map<K, V> loadAll(Iterable<? extends K> iterable) throws Exception {
        throw new UnsupportedLoadingOperationException();
    }

    public ListenableFuture<V> reload(K k, V v) throws Exception {
        Preconditions.checkNotNull(k);
        Preconditions.checkNotNull(v);
        return Futures.immediateFuture(load(k));
    }
}
