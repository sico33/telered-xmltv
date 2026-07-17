package com.google.common.io;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Closer implements Closeable {
    private static final Suppressor SUPPRESSOR;
    private final Deque<Closeable> stack = new ArrayDeque(4);
    final Suppressor suppressor;

    @CheckForNull
    private Throwable thrown;

    static final class LoggingSuppressor implements Suppressor {
        static final LoggingSuppressor INSTANCE = new LoggingSuppressor();

        LoggingSuppressor() {
        }

        @Override // com.google.common.io.Closer.Suppressor
        public void suppress(Closeable closeable, Throwable th, Throwable th2) {
            Closeables.logger.log(Level.WARNING, "Suppressing exception thrown when closing " + closeable, th2);
        }
    }

    static final class SuppressingSuppressor implements Suppressor {
        private final Method addSuppressed;

        private SuppressingSuppressor(Method method) {
            this.addSuppressed = method;
        }

        @CheckForNull
        static SuppressingSuppressor tryCreate() {
            try {
                return new SuppressingSuppressor(Throwable.class.getMethod("addSuppressed", Throwable.class));
            } catch (Throwable th) {
                return null;
            }
        }

        @Override // com.google.common.io.Closer.Suppressor
        public void suppress(Closeable closeable, Throwable th, Throwable th2) {
            if (th == th2) {
                return;
            }
            try {
                this.addSuppressed.invoke(th, th2);
            } catch (Throwable th3) {
                LoggingSuppressor.INSTANCE.suppress(closeable, th, th2);
            }
        }
    }

    interface Suppressor {
        void suppress(Closeable closeable, Throwable th, Throwable th2);
    }

    static {
        Suppressor suppressorTryCreate = SuppressingSuppressor.tryCreate();
        if (suppressorTryCreate == null) {
            suppressorTryCreate = LoggingSuppressor.INSTANCE;
        }
        SUPPRESSOR = suppressorTryCreate;
    }

    Closer(Suppressor suppressor) {
        this.suppressor = (Suppressor) Preconditions.checkNotNull(suppressor);
    }

    public static Closer create() {
        return new Closer(SUPPRESSOR);
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() throws Throwable {
        Throwable th;
        Throwable th2 = this.thrown;
        while (!this.stack.isEmpty()) {
            Closeable closeableRemoveFirst = this.stack.removeFirst();
            try {
                closeableRemoveFirst.close();
                th = th2;
            } catch (Throwable th3) {
                if (th2 == null) {
                    th = th3;
                } else {
                    this.suppressor.suppress(closeableRemoveFirst, th2, th3);
                    th = th2;
                }
            }
            th2 = th;
        }
        if (this.thrown != null || th2 == null) {
            return;
        }
        Throwables.propagateIfPossible(th2, IOException.class);
        throw new AssertionError(th2);
    }

    @ParametricNullness
    public <C extends Closeable> C register(@ParametricNullness C c) {
        if (c != null) {
            this.stack.addFirst(c);
        }
        return c;
    }

    public RuntimeException rethrow(Throwable th) throws Throwable {
        Preconditions.checkNotNull(th);
        this.thrown = th;
        Throwables.propagateIfPossible(th, IOException.class);
        throw new RuntimeException(th);
    }

    public <X extends Exception> RuntimeException rethrow(Throwable th, Class<X> cls) throws Exception {
        Preconditions.checkNotNull(th);
        this.thrown = th;
        Throwables.propagateIfPossible(th, IOException.class);
        Throwables.propagateIfPossible(th, cls);
        throw new RuntimeException(th);
    }

    public <X1 extends Exception, X2 extends Exception> RuntimeException rethrow(Throwable th, Class<X1> cls, Class<X2> cls2) throws Exception {
        Preconditions.checkNotNull(th);
        this.thrown = th;
        Throwables.propagateIfPossible(th, IOException.class);
        Throwables.propagateIfPossible(th, cls, cls2);
        throw new RuntimeException(th);
    }
}
