package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class ThreadFactoryBuilder {

    @CheckForNull
    private String nameFormat = null;

    @CheckForNull
    private Boolean daemon = null;

    @CheckForNull
    private Integer priority = null;

    @CheckForNull
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = null;

    @CheckForNull
    private ThreadFactory backingThreadFactory = null;

    private static ThreadFactory doBuild(ThreadFactoryBuilder threadFactoryBuilder) {
        String str = threadFactoryBuilder.nameFormat;
        return new ThreadFactory(threadFactoryBuilder.backingThreadFactory != null ? threadFactoryBuilder.backingThreadFactory : Executors.defaultThreadFactory(), str, str != null ? new AtomicLong(0L) : null, threadFactoryBuilder.daemon, threadFactoryBuilder.priority, threadFactoryBuilder.uncaughtExceptionHandler) { // from class: com.google.common.util.concurrent.ThreadFactoryBuilder.1
            final ThreadFactory val$backingThreadFactory;
            final AtomicLong val$count;
            final Boolean val$daemon;
            final String val$nameFormat;
            final Integer val$priority;
            final Thread.UncaughtExceptionHandler val$uncaughtExceptionHandler;

            {
                this.val$backingThreadFactory = threadFactory;
                this.val$nameFormat = str;
                this.val$count = atomicLong;
                this.val$daemon = bool;
                this.val$priority = num;
                this.val$uncaughtExceptionHandler = uncaughtExceptionHandler;
            }

            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(Runnable runnable) {
                Thread threadNewThread = this.val$backingThreadFactory.newThread(runnable);
                Objects.requireNonNull(threadNewThread);
                if (this.val$nameFormat != null) {
                    threadNewThread.setName(ThreadFactoryBuilder.format(this.val$nameFormat, Long.valueOf(((AtomicLong) Objects.requireNonNull(this.val$count)).getAndIncrement())));
                }
                if (this.val$daemon != null) {
                    threadNewThread.setDaemon(this.val$daemon.booleanValue());
                }
                if (this.val$priority != null) {
                    threadNewThread.setPriority(this.val$priority.intValue());
                }
                if (this.val$uncaughtExceptionHandler != null) {
                    threadNewThread.setUncaughtExceptionHandler(this.val$uncaughtExceptionHandler);
                }
                return threadNewThread;
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String format(String str, Object... objArr) {
        return String.format(Locale.ROOT, str, objArr);
    }

    public ThreadFactory build() {
        return doBuild(this);
    }

    public ThreadFactoryBuilder setDaemon(boolean z) {
        this.daemon = Boolean.valueOf(z);
        return this;
    }

    public ThreadFactoryBuilder setNameFormat(String str) {
        format(str, 0);
        this.nameFormat = str;
        return this;
    }

    public ThreadFactoryBuilder setPriority(int i) {
        Preconditions.checkArgument(i >= 1, "Thread priority (%s) must be >= %s", i, 1);
        Preconditions.checkArgument(i <= 10, "Thread priority (%s) must be <= %s", i, 10);
        this.priority = Integer.valueOf(i);
        return this;
    }

    public ThreadFactoryBuilder setThreadFactory(ThreadFactory threadFactory) {
        this.backingThreadFactory = (ThreadFactory) Preconditions.checkNotNull(threadFactory);
        return this;
    }

    public ThreadFactoryBuilder setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = (Thread.UncaughtExceptionHandler) Preconditions.checkNotNull(uncaughtExceptionHandler);
        return this;
    }
}
