package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class SimpleTimeLimiter implements TimeLimiter {
    private final ExecutorService executor;

    /* JADX INFO: renamed from: com.google.common.util.concurrent.SimpleTimeLimiter$1, reason: invalid class name */
    class AnonymousClass1 implements InvocationHandler {
        final SimpleTimeLimiter this$0;
        final Set val$interruptibleMethods;
        final Object val$target;
        final long val$timeoutDuration;
        final TimeUnit val$timeoutUnit;

        AnonymousClass1(SimpleTimeLimiter simpleTimeLimiter, Object obj, long j, TimeUnit timeUnit, Set set) {
            this.this$0 = simpleTimeLimiter;
            this.val$target = obj;
            this.val$timeoutDuration = j;
            this.val$timeoutUnit = timeUnit;
            this.val$interruptibleMethods = set;
        }

        static /* synthetic */ Object lambda$invoke$0(Method method, Object obj, Object[] objArr) throws Exception {
            try {
                return method.invoke(obj, objArr);
            } catch (InvocationTargetException e) {
                throw SimpleTimeLimiter.throwCause(e, false);
            }
        }

        @Override // java.lang.reflect.InvocationHandler
        @CheckForNull
        public Object invoke(Object obj, final Method method, @CheckForNull final Object[] objArr) throws Throwable {
            final Object obj2 = this.val$target;
            return this.this$0.callWithTimeout(new Callable(method, obj2, objArr) { // from class: com.google.common.util.concurrent.SimpleTimeLimiter$1$$ExternalSyntheticLambda0
                public final Method f$0;
                public final Object f$1;
                public final Object[] f$2;

                {
                    this.f$0 = method;
                    this.f$1 = obj2;
                    this.f$2 = objArr;
                }

                @Override // java.util.concurrent.Callable
                public final Object call() {
                    return SimpleTimeLimiter.AnonymousClass1.lambda$invoke$0(this.f$0, this.f$1, this.f$2);
                }
            }, this.val$timeoutDuration, this.val$timeoutUnit, this.val$interruptibleMethods.contains(method));
        }
    }

    private SimpleTimeLimiter(ExecutorService executorService) {
        this.executor = (ExecutorService) Preconditions.checkNotNull(executorService);
    }

    /* JADX INFO: Access modifiers changed from: private */
    @ParametricNullness
    public <T> T callWithTimeout(Callable<T> callable, long j, TimeUnit timeUnit, boolean z) throws Exception {
        Preconditions.checkNotNull(callable);
        Preconditions.checkNotNull(timeUnit);
        checkPositiveTimeout(j);
        Future<T> futureSubmit = this.executor.submit(callable);
        try {
            return z ? futureSubmit.get(j, timeUnit) : (T) Uninterruptibles.getUninterruptibly(futureSubmit, j, timeUnit);
        } catch (InterruptedException e) {
            futureSubmit.cancel(true);
            throw e;
        } catch (ExecutionException e2) {
            throw throwCause(e2, true);
        } catch (TimeoutException e3) {
            futureSubmit.cancel(true);
            throw new UncheckedTimeoutException(e3);
        }
    }

    private static void checkPositiveTimeout(long j) {
        Preconditions.checkArgument(j > 0, "timeout must be positive: %s", j);
    }

    public static SimpleTimeLimiter create(ExecutorService executorService) {
        return new SimpleTimeLimiter(executorService);
    }

    private static boolean declaresInterruptedEx(Method method) {
        for (Class<?> cls : method.getExceptionTypes()) {
            if (cls == InterruptedException.class) {
                return true;
            }
        }
        return false;
    }

    private static Set<Method> findInterruptibleMethods(Class<?> cls) {
        HashSet hashSetNewHashSet = Sets.newHashSet();
        for (Method method : cls.getMethods()) {
            if (declaresInterruptedEx(method)) {
                hashSetNewHashSet.add(method);
            }
        }
        return hashSetNewHashSet;
    }

    private static <T> T newProxy(Class<T> cls, InvocationHandler invocationHandler) {
        return cls.cast(Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, invocationHandler));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Exception throwCause(Exception exc, boolean z) throws Exception {
        Throwable cause = exc.getCause();
        if (cause == null) {
            throw exc;
        }
        if (z) {
            cause.setStackTrace((StackTraceElement[]) ObjectArrays.concat(cause.getStackTrace(), exc.getStackTrace(), StackTraceElement.class));
        }
        if (cause instanceof Exception) {
            throw ((Exception) cause);
        }
        if (cause instanceof Error) {
            throw ((Error) cause);
        }
        throw exc;
    }

    private void wrapAndThrowExecutionExceptionOrError(Throwable th) throws ExecutionException {
        if (th instanceof Error) {
            throw new ExecutionError((Error) th);
        }
        if (!(th instanceof RuntimeException)) {
            throw new ExecutionException(th);
        }
        throw new UncheckedExecutionException(th);
    }

    private void wrapAndThrowRuntimeExecutionExceptionOrError(Throwable th) {
        if (!(th instanceof Error)) {
            throw new UncheckedExecutionException(th);
        }
        throw new ExecutionError((Error) th);
    }

    @Override // com.google.common.util.concurrent.TimeLimiter
    @ParametricNullness
    public <T> T callUninterruptiblyWithTimeout(Callable<T> callable, long j, TimeUnit timeUnit) throws ExecutionException, TimeoutException {
        Preconditions.checkNotNull(callable);
        Preconditions.checkNotNull(timeUnit);
        checkPositiveTimeout(j);
        Future<T> futureSubmit = this.executor.submit(callable);
        try {
            return (T) Uninterruptibles.getUninterruptibly(futureSubmit, j, timeUnit);
        } catch (ExecutionException e) {
            wrapAndThrowExecutionExceptionOrError(e.getCause());
            throw new AssertionError();
        } catch (TimeoutException e2) {
            futureSubmit.cancel(true);
            throw e2;
        }
    }

    @Override // com.google.common.util.concurrent.TimeLimiter
    @ParametricNullness
    public <T> T callWithTimeout(Callable<T> callable, long j, TimeUnit timeUnit) throws Throwable {
        Preconditions.checkNotNull(callable);
        Preconditions.checkNotNull(timeUnit);
        checkPositiveTimeout(j);
        Future<T> futureSubmit = this.executor.submit(callable);
        try {
            return futureSubmit.get(j, timeUnit);
        } catch (InterruptedException e) {
            e = e;
            futureSubmit.cancel(true);
            throw e;
        } catch (ExecutionException e2) {
            wrapAndThrowExecutionExceptionOrError(e2.getCause());
            throw new AssertionError();
        } catch (TimeoutException e3) {
            e = e3;
            futureSubmit.cancel(true);
            throw e;
        }
    }

    @Override // com.google.common.util.concurrent.TimeLimiter
    public <T> T newProxy(T t, Class<T> cls, long j, TimeUnit timeUnit) {
        Preconditions.checkNotNull(t);
        Preconditions.checkNotNull(cls);
        Preconditions.checkNotNull(timeUnit);
        checkPositiveTimeout(j);
        Preconditions.checkArgument(cls.isInterface(), "interfaceType must be an interface type");
        return (T) newProxy(cls, new AnonymousClass1(this, t, j, timeUnit, findInterruptibleMethods(cls)));
    }

    @Override // com.google.common.util.concurrent.TimeLimiter
    public void runUninterruptiblyWithTimeout(Runnable runnable, long j, TimeUnit timeUnit) throws Throwable {
        Preconditions.checkNotNull(runnable);
        Preconditions.checkNotNull(timeUnit);
        checkPositiveTimeout(j);
        Future<?> futureSubmit = this.executor.submit(runnable);
        try {
            Uninterruptibles.getUninterruptibly(futureSubmit, j, timeUnit);
        } catch (ExecutionException e) {
            wrapAndThrowRuntimeExecutionExceptionOrError(e.getCause());
            throw new AssertionError();
        } catch (TimeoutException e2) {
            futureSubmit.cancel(true);
            throw e2;
        }
    }

    @Override // com.google.common.util.concurrent.TimeLimiter
    public void runWithTimeout(Runnable runnable, long j, TimeUnit timeUnit) throws Throwable {
        Preconditions.checkNotNull(runnable);
        Preconditions.checkNotNull(timeUnit);
        checkPositiveTimeout(j);
        Future<?> futureSubmit = this.executor.submit(runnable);
        try {
            futureSubmit.get(j, timeUnit);
        } catch (InterruptedException e) {
            e = e;
            futureSubmit.cancel(true);
            throw e;
        } catch (ExecutionException e2) {
            wrapAndThrowRuntimeExecutionExceptionOrError(e2.getCause());
            throw new AssertionError();
        } catch (TimeoutException e3) {
            e = e3;
            futureSubmit.cancel(true);
            throw e;
        }
    }
}
