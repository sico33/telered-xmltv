package com.google.common.util.concurrent;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface AsyncCallable<V> {
    ListenableFuture<V> call() throws Exception;
}
