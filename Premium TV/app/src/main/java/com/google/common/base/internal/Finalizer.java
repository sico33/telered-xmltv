package com.google.common.base.internal;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
public class Finalizer implements Runnable {
    private static final String FINALIZABLE_REFERENCE = "com.google.common.base.FinalizableReference";

    @CheckForNull
    private static final Field inheritableThreadLocals;
    private final WeakReference<Class<?>> finalizableReferenceClassReference;
    private final PhantomReference<Object> frqReference;
    private final ReferenceQueue<Object> queue;
    private static final Logger logger = Logger.getLogger(Finalizer.class.getName());

    @CheckForNull
    private static final Constructor<Thread> bigThreadConstructor = getBigThreadConstructor();

    static {
        inheritableThreadLocals = bigThreadConstructor == null ? getInheritableThreadLocalsField() : null;
    }

    private Finalizer(Class<?> cls, ReferenceQueue<Object> referenceQueue, PhantomReference<Object> phantomReference) {
        this.queue = referenceQueue;
        this.finalizableReferenceClassReference = new WeakReference<>(cls);
        this.frqReference = phantomReference;
    }

    private boolean cleanUp(Reference<?> reference) {
        Reference<? extends Object> referencePoll;
        Method finalizeReferentMethod = getFinalizeReferentMethod();
        if (finalizeReferentMethod == null || !finalizeReference(reference, finalizeReferentMethod)) {
            return false;
        }
        do {
            referencePoll = this.queue.poll();
            if (referencePoll == null) {
                return true;
            }
        } while (finalizeReference(referencePoll, finalizeReferentMethod));
        return false;
    }

    private boolean finalizeReference(Reference<?> reference, Method method) {
        reference.clear();
        if (reference == this.frqReference) {
            return false;
        }
        try {
            method.invoke(reference, new Object[0]);
        } catch (Throwable th) {
            logger.log(Level.SEVERE, "Error cleaning up after reference.", th);
        }
        return true;
    }

    @CheckForNull
    private static Constructor<Thread> getBigThreadConstructor() {
        try {
            return Thread.class.getConstructor(ThreadGroup.class, Runnable.class, String.class, Long.TYPE, Boolean.TYPE);
        } catch (Throwable th) {
            return null;
        }
    }

    @CheckForNull
    private Method getFinalizeReferentMethod() {
        Class<?> cls = this.finalizableReferenceClassReference.get();
        if (cls == null) {
            return null;
        }
        try {
            return cls.getMethod("finalizeReferent", new Class[0]);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @CheckForNull
    private static Field getInheritableThreadLocalsField() {
        try {
            Field declaredField = Thread.class.getDeclaredField("inheritableThreadLocals");
            declaredField.setAccessible(true);
            return declaredField;
        } catch (Throwable th) {
            logger.log(Level.INFO, "Couldn't access Thread.inheritableThreadLocals. Reference finalizer threads will inherit thread local values.");
            return null;
        }
    }

    public static void startFinalizer(Class<?> cls, ReferenceQueue<Object> referenceQueue, PhantomReference<Object> phantomReference) {
        Thread threadNewInstance;
        if (!cls.getName().equals(FINALIZABLE_REFERENCE)) {
            throw new IllegalArgumentException("Expected com.google.common.base.FinalizableReference.");
        }
        Finalizer finalizer = new Finalizer(cls, referenceQueue, phantomReference);
        String name = Finalizer.class.getName();
        if (bigThreadConstructor != null) {
            try {
                threadNewInstance = bigThreadConstructor.newInstance(null, finalizer, name, 0L, false);
            } catch (Throwable th) {
                logger.log(Level.INFO, "Failed to create a thread without inherited thread-local values", th);
                threadNewInstance = null;
            }
        } else {
            threadNewInstance = null;
        }
        if (threadNewInstance == null) {
            threadNewInstance = new Thread(null, finalizer, name);
        }
        threadNewInstance.setDaemon(true);
        try {
            if (inheritableThreadLocals != null) {
                inheritableThreadLocals.set(threadNewInstance, null);
            }
        } catch (Throwable th2) {
            logger.log(Level.INFO, "Failed to clear thread local values inherited by reference finalizer thread.", th2);
        }
        threadNewInstance.start();
    }

    @Override // java.lang.Runnable
    public void run() {
        while (cleanUp(this.queue.remove())) {
        }
    }
}
