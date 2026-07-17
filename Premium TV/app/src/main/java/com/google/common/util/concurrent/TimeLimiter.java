package com.google.common.util.concurrent;

import com.google.errorprone.annotations.DoNotMock;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
@DoNotMock("Use FakeTimeLimiter")
@ElementTypesAreNonnullByDefault
public interface TimeLimiter {
    @ParametricNullness
    <T> T callUninterruptiblyWithTimeout(Callable<T> callable, long j, TimeUnit timeUnit) throws ExecutionException, TimeoutException;

    @ParametricNullness
    <T> T callWithTimeout(Callable<T> callable, long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException;

    <T> T newProxy(T t, Class<T> cls, long j, TimeUnit timeUnit);

    void runUninterruptiblyWithTimeout(Runnable runnable, long j, TimeUnit timeUnit) throws TimeoutException;

    void runWithTimeout(Runnable runnable, long j, TimeUnit timeUnit) throws InterruptedException, TimeoutException;
}
