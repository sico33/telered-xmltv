package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import java.util.concurrent.Callable;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Callables {
    private Callables() {
    }

    public static <T> AsyncCallable<T> asAsyncCallable(final Callable<T> callable, final ListeningExecutorService listeningExecutorService) {
        Preconditions.checkNotNull(callable);
        Preconditions.checkNotNull(listeningExecutorService);
        return new AsyncCallable(listeningExecutorService, callable) { // from class: com.google.common.util.concurrent.Callables$$ExternalSyntheticLambda1
            public final ListeningExecutorService f$0;
            public final Callable f$1;

            {
                this.f$0 = listeningExecutorService;
                this.f$1 = callable;
            }

            @Override // com.google.common.util.concurrent.AsyncCallable
            public final ListenableFuture call() {
                return this.f$0.submit(this.f$1);
            }
        };
    }

    static /* synthetic */ Object lambda$returning$0(Object obj) throws Exception {
        return obj;
    }

    static /* synthetic */ Object lambda$threadRenaming$2(Supplier supplier, Callable callable) throws Exception {
        Thread threadCurrentThread = Thread.currentThread();
        String name = threadCurrentThread.getName();
        boolean zTrySetName = trySetName((String) supplier.get(), threadCurrentThread);
        try {
            return callable.call();
        } finally {
            if (zTrySetName) {
                trySetName(name, threadCurrentThread);
            }
        }
    }

    static /* synthetic */ void lambda$threadRenaming$3(Supplier supplier, Runnable runnable) {
        Thread threadCurrentThread = Thread.currentThread();
        String name = threadCurrentThread.getName();
        boolean zTrySetName = trySetName((String) supplier.get(), threadCurrentThread);
        try {
            runnable.run();
        } finally {
            if (zTrySetName) {
                trySetName(name, threadCurrentThread);
            }
        }
    }

    public static <T> Callable<T> returning(@ParametricNullness final T t) {
        return new Callable(t) { // from class: com.google.common.util.concurrent.Callables$$ExternalSyntheticLambda2
            public final Object f$0;

            {
                this.f$0 = t;
            }

            @Override // java.util.concurrent.Callable
            public final Object call() {
                return Callables.lambda$returning$0(this.f$0);
            }
        };
    }

    static Runnable threadRenaming(final Runnable runnable, final Supplier<String> supplier) {
        Preconditions.checkNotNull(supplier);
        Preconditions.checkNotNull(runnable);
        return new Runnable(supplier, runnable) { // from class: com.google.common.util.concurrent.Callables$$ExternalSyntheticLambda3
            public final Supplier f$0;
            public final Runnable f$1;

            {
                this.f$0 = supplier;
                this.f$1 = runnable;
            }

            @Override // java.lang.Runnable
            public final void run() {
                Callables.lambda$threadRenaming$3(this.f$0, this.f$1);
            }
        };
    }

    static <T> Callable<T> threadRenaming(final Callable<T> callable, final Supplier<String> supplier) {
        Preconditions.checkNotNull(supplier);
        Preconditions.checkNotNull(callable);
        return new Callable(supplier, callable) { // from class: com.google.common.util.concurrent.Callables$$ExternalSyntheticLambda0
            public final Supplier f$0;
            public final Callable f$1;

            {
                this.f$0 = supplier;
                this.f$1 = callable;
            }

            @Override // java.util.concurrent.Callable
            public final Object call() {
                return Callables.lambda$threadRenaming$2(this.f$0, this.f$1);
            }
        };
    }

    private static boolean trySetName(String str, Thread thread) {
        try {
            thread.setName(str);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
