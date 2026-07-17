package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Util;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
public class RequestFutureTarget<R> implements FutureTarget<R>, RequestListener<R> {
    private static final Waiter DEFAULT_WAITER = new Waiter();
    private final boolean assertBackgroundThread;
    private GlideException exception;
    private final int height;
    private boolean isCancelled;
    private boolean loadFailed;
    private Request request;
    private R resource;
    private boolean resultReceived;
    private final Waiter waiter;
    private final int width;

    static class Waiter {
        Waiter() {
        }

        void notifyAll(Object obj) {
            obj.notifyAll();
        }

        void waitForTimeout(Object obj, long j) throws InterruptedException {
            obj.wait(j);
        }
    }

    public RequestFutureTarget(int i, int i2) {
        this(i, i2, true, DEFAULT_WAITER);
    }

    RequestFutureTarget(int i, int i2, boolean z, Waiter waiter) {
        this.width = i;
        this.height = i2;
        this.assertBackgroundThread = z;
        this.waiter = waiter;
    }

    private R doGet(Long l) throws ExecutionException, InterruptedException, TimeoutException {
        R r;
        synchronized (this) {
            if (this.assertBackgroundThread && !isDone()) {
                Util.assertBackgroundThread();
            }
            if (this.isCancelled) {
                throw new CancellationException();
            }
            if (this.loadFailed) {
                throw new ExecutionException(this.exception);
            }
            if (this.resultReceived) {
                r = this.resource;
            } else {
                if (l == null) {
                    this.waiter.waitForTimeout(this, 0L);
                } else if (l.longValue() > 0) {
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    long jLongValue = l.longValue() + jCurrentTimeMillis;
                    while (!isDone() && jCurrentTimeMillis < jLongValue) {
                        this.waiter.waitForTimeout(this, jLongValue - jCurrentTimeMillis);
                        jCurrentTimeMillis = System.currentTimeMillis();
                    }
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                if (this.loadFailed) {
                    throw new ExecutionException(this.exception);
                }
                if (this.isCancelled) {
                    throw new CancellationException();
                }
                if (!this.resultReceived) {
                    throw new TimeoutException();
                }
                r = this.resource;
            }
        }
        return r;
    }

    @Override // java.util.concurrent.Future
    public boolean cancel(boolean z) {
        Request request = null;
        synchronized (this) {
            if (isDone()) {
                return false;
            }
            this.isCancelled = true;
            this.waiter.notifyAll(this);
            if (z) {
                request = this.request;
                this.request = null;
            }
            if (request == null) {
                return true;
            }
            request.clear();
            return true;
        }
    }

    @Override // java.util.concurrent.Future
    public R get() throws ExecutionException, InterruptedException {
        try {
            return doGet(null);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    @Override // java.util.concurrent.Future
    public R get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        return doGet(Long.valueOf(timeUnit.toMillis(j)));
    }

    @Override // com.bumptech.glide.request.target.Target
    public Request getRequest() {
        Request request;
        synchronized (this) {
            request = this.request;
        }
        return request;
    }

    @Override // com.bumptech.glide.request.target.Target
    public void getSize(SizeReadyCallback sizeReadyCallback) {
        sizeReadyCallback.onSizeReady(this.width, this.height);
    }

    @Override // java.util.concurrent.Future
    public boolean isCancelled() {
        boolean z;
        synchronized (this) {
            z = this.isCancelled;
        }
        return z;
    }

    @Override // java.util.concurrent.Future
    public boolean isDone() {
        boolean z;
        synchronized (this) {
            z = this.isCancelled || this.resultReceived || this.loadFailed;
        }
        return z;
    }

    @Override // com.bumptech.glide.manager.LifecycleListener
    public void onDestroy() {
    }

    @Override // com.bumptech.glide.request.target.Target
    public void onLoadCleared(Drawable drawable) {
    }

    @Override // com.bumptech.glide.request.target.Target
    public void onLoadFailed(Drawable drawable) {
        synchronized (this) {
        }
    }

    @Override // com.bumptech.glide.request.RequestListener
    public boolean onLoadFailed(GlideException glideException, Object obj, Target<R> target, boolean z) {
        synchronized (this) {
            this.loadFailed = true;
            this.exception = glideException;
            this.waiter.notifyAll(this);
        }
        return false;
    }

    @Override // com.bumptech.glide.request.target.Target
    public void onLoadStarted(Drawable drawable) {
    }

    @Override // com.bumptech.glide.request.target.Target
    public void onResourceReady(R r, Transition<? super R> transition) {
        synchronized (this) {
        }
    }

    @Override // com.bumptech.glide.request.RequestListener
    public boolean onResourceReady(R r, Object obj, Target<R> target, DataSource dataSource, boolean z) {
        synchronized (this) {
            this.resultReceived = true;
            this.resource = r;
            this.waiter.notifyAll(this);
        }
        return false;
    }

    @Override // com.bumptech.glide.manager.LifecycleListener
    public void onStart() {
    }

    @Override // com.bumptech.glide.manager.LifecycleListener
    public void onStop() {
    }

    @Override // com.bumptech.glide.request.target.Target
    public void removeCallback(SizeReadyCallback sizeReadyCallback) {
    }

    @Override // com.bumptech.glide.request.target.Target
    public void setRequest(Request request) {
        synchronized (this) {
            this.request = request;
        }
    }

    public String toString() {
        String str;
        String str2 = super.toString() + "[status=";
        Request request = null;
        synchronized (this) {
            if (this.isCancelled) {
                str = "CANCELLED";
            } else if (this.loadFailed) {
                str = "FAILURE";
            } else if (this.resultReceived) {
                str = "SUCCESS";
            } else {
                str = "PENDING";
                request = this.request;
            }
        }
        return request != null ? str2 + str + ", request=[" + request + "]]" : str2 + str + "]";
    }
}
