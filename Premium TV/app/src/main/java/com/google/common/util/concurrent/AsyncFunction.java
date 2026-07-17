package com.google.common.util.concurrent;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface AsyncFunction<I, O> {
    ListenableFuture<O> apply(@ParametricNullness I i) throws Exception;
}
