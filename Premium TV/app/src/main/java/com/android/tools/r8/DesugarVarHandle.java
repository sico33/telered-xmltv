package com.android.tools.r8;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import sun.misc.Unsafe;

/* JADX INFO: loaded from: classes2.dex */
public final /* synthetic */ class DesugarVarHandle {
    public static final Unsafe U = getUnsafe();
    public final long arrayIndexScale;
    public final long offset;
    public final Class recv;
    public final Class type;

    /* JADX INFO: renamed from: com.android.tools.r8.DesugarVarHandle$0, reason: invalid class name */
    public final /* synthetic */ class AnonymousClass0 {
        public static /* synthetic */ boolean m(Unsafe unsafe, Object obj, long j, Object obj2, Object obj3) {
            while (!unsafe.compareAndSwapObject(obj, j, obj2, obj3)) {
                if (unsafe.getObject(obj, j) != obj2) {
                    return false;
                }
            }
            return true;
        }
    }

    public /* synthetic */ DesugarVarHandle(Class cls) {
        if (!cls.isArray()) {
            throw new IllegalArgumentException("not an array " + cls.getSimpleName());
        }
        Class<?> componentType = cls.getComponentType();
        if (componentType.isArray()) {
            throw new UnsupportedOperationException("Using a VarHandle for a multidimensional array " + arrayRequiringNativeSupport());
        }
        if (componentType.isPrimitive() && componentType != Integer.TYPE && componentType != Long.TYPE) {
            throw new UnsupportedOperationException("Using a VarHandle for an array of type '" + componentType.getName() + "' " + arrayRequiringNativeSupport());
        }
        this.recv = cls;
        this.type = cls.getComponentType();
        Unsafe unsafe = U;
        this.offset = unsafe.arrayBaseOffset(cls);
        this.arrayIndexScale = unsafe.arrayIndexScale(cls);
    }

    public /* synthetic */ DesugarVarHandle(Class cls, String str, Class cls2) {
        this.recv = cls;
        this.type = cls.getDeclaredField(str).getType();
        if (!cls2.isPrimitive() || cls2 == Integer.TYPE || cls2 == Long.TYPE) {
            this.offset = U.objectFieldOffset(cls.getDeclaredField(str));
            this.arrayIndexScale = 0L;
        } else {
            throw new UnsupportedOperationException("Using a VarHandle for a field of type '" + cls2.getName() + "' requires native VarHandle support available from Android 13. VarHandle desugaring only supports primitive types int and long and reference types.");
        }
    }

    public static /* synthetic */ Unsafe getUnsafe() {
        Field declaredField;
        try {
            declaredField = Unsafe.class.getDeclaredField("theUnsafe");
        } catch (NoSuchFieldException e) {
            Field[] declaredFields = Unsafe.class.getDeclaredFields();
            int length = declaredFields.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    declaredField = null;
                    break;
                }
                declaredField = declaredFields[i];
                if (Modifier.isStatic(declaredField.getModifiers()) && Unsafe.class.isAssignableFrom(declaredField.getType())) {
                    break;
                }
                i++;
            }
            if (declaredField != null) {
                throw new UnsupportedOperationException("Couldn't find the Unsafe", e);
            }
        }
        declaredField.setAccessible(true);
        try {
            return (Unsafe) declaredField.get(null);
        } catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }

    public /* synthetic */ String arrayRequiringNativeSupport() {
        return "requires native VarHandle support available from Android 13. VarHandle desugaring only supports single dimensional arrays of primitive typesint and long and reference types.";
    }

    public /* synthetic */ Object boxIntIfPossible(int i, Class cls) {
        if (cls == Long.class) {
            return Long.valueOf(i);
        }
        if (cls == Float.class) {
            return Float.valueOf(i);
        }
        if (cls == Double.class) {
            return Double.valueOf(i);
        }
        throw desugarWrongMethodTypeException();
    }

    public /* synthetic */ Object boxLongIfPossible(long j, Class cls) {
        if (cls == Float.class) {
            return Float.valueOf(j);
        }
        if (cls == Double.class) {
            return Double.valueOf(j);
        }
        throw desugarWrongMethodTypeException();
    }

    public /* synthetic */ boolean compareAndSet(Object obj, int i, int i2) {
        if (this.type == Integer.TYPE) {
            return U.compareAndSwapInt(obj, this.offset, i, i2);
        }
        return this.type == Long.TYPE ? U.compareAndSwapLong(obj, this.offset, i, i2) : compareAndSet(obj, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public /* synthetic */ boolean compareAndSet(Object obj, int i, Object obj2, Object obj3) {
        if (!this.recv.isArray() || this.recv != obj.getClass()) {
            throw new UnsupportedOperationException();
        }
        long j = (((long) i) * this.arrayIndexScale) + this.offset;
        Class cls = this.recv;
        if (cls == int[].class) {
            return U.compareAndSwapInt(obj, j, toIntIfPossible(obj2, false), toIntIfPossible(obj3, false));
        }
        return cls == long[].class ? U.compareAndSwapLong(obj, j, toLongIfPossible(obj2, false), toLongIfPossible(obj3, false)) : AnonymousClass0.m(U, obj, j, obj2, obj3);
    }

    public /* synthetic */ boolean compareAndSet(Object obj, long j, long j2) {
        return this.type == Long.TYPE ? U.compareAndSwapLong(obj, this.offset, j, j2) : compareAndSet(obj, Long.valueOf(j), Long.valueOf(j2));
    }

    public /* synthetic */ boolean compareAndSet(Object obj, Object obj2, Object obj3) {
        if (this.type == Integer.TYPE) {
            return U.compareAndSwapInt(obj, this.offset, toIntIfPossible(obj2, false), toIntIfPossible(obj3, false));
        }
        Class cls = this.type;
        Class cls2 = Long.TYPE;
        long j = this.offset;
        return cls == cls2 ? U.compareAndSwapLong(obj, j, toLongIfPossible(obj2, false), toLongIfPossible(obj3, false)) : AnonymousClass0.m(U, obj, j, obj2, obj3);
    }

    public /* synthetic */ boolean compareAndSet(int[] iArr, int i, int i2, int i3) {
        if (this.recv != int[].class) {
            throw new UnsupportedOperationException();
        }
        return U.compareAndSwapInt(iArr, this.offset + (((long) i) * this.arrayIndexScale), i2, i3);
    }

    public /* synthetic */ boolean compareAndSet(long[] jArr, int i, long j, long j2) {
        if (this.recv != long[].class) {
            throw new UnsupportedOperationException();
        }
        return U.compareAndSwapLong(jArr, this.offset + (((long) i) * this.arrayIndexScale), j, j2);
    }

    public /* synthetic */ RuntimeException desugarWrongMethodTypeException() {
        return new RuntimeException("java.lang.invoke.WrongMethodTypeException");
    }

    public /* synthetic */ int get(Object obj) {
        if (this.type == Integer.TYPE) {
            return U.getInt(obj, this.offset);
        }
        if (this.type != Long.TYPE) {
            return toIntIfPossible(U.getObject(obj, this.offset), true);
        }
        throw desugarWrongMethodTypeException();
    }

    public /* synthetic */ int get(int[] iArr, int i) {
        if (this.recv != int[].class) {
            throw new UnsupportedOperationException();
        }
        return U.getInt(iArr, this.offset + (((long) i) * this.arrayIndexScale));
    }

    /* JADX INFO: renamed from: get, reason: collision with other method in class */
    public /* synthetic */ long m178get(Object obj) {
        if (this.type == Long.TYPE) {
            return U.getLong(obj, this.offset);
        }
        Class cls = this.type;
        Class cls2 = Integer.TYPE;
        long j = this.offset;
        return cls == cls2 ? U.getInt(obj, j) : toLongIfPossible(U.getObject(obj, j), true);
    }

    public /* synthetic */ long get(long[] jArr, int i) {
        if (this.recv != long[].class) {
            throw new UnsupportedOperationException();
        }
        return U.getLong(jArr, this.offset + (((long) i) * this.arrayIndexScale));
    }

    /* JADX INFO: renamed from: get, reason: collision with other method in class */
    public /* synthetic */ Object m179get(Object obj) {
        if (this.type == Integer.TYPE) {
            return Integer.valueOf(U.getInt(obj, this.offset));
        }
        Class cls = this.type;
        Class cls2 = Long.TYPE;
        long j = this.offset;
        Unsafe unsafe = U;
        return cls == cls2 ? Long.valueOf(unsafe.getLong(obj, j)) : unsafe.getObject(obj, j);
    }

    public /* synthetic */ Object get(Object obj, int i) {
        if (!this.recv.isArray() || this.recv != obj.getClass()) {
            throw new UnsupportedOperationException();
        }
        long j = this.offset + (((long) i) * this.arrayIndexScale);
        if (this.type == Integer.TYPE) {
            return Integer.valueOf(U.getInt(obj, j));
        }
        return this.type == Long.TYPE ? Integer.valueOf((int) U.getLong(obj, j)) : U.getObject(obj, j);
    }

    public /* synthetic */ Object get(Object obj, int i, Class cls) {
        if (!this.recv.isArray() || this.recv != obj.getClass()) {
            throw new UnsupportedOperationException();
        }
        long j = this.offset + (((long) i) * this.arrayIndexScale);
        if (this.type == Integer.TYPE) {
            return boxIntIfPossible(U.getInt(obj, j), cls);
        }
        if (this.type == Long.TYPE) {
            return boxLongIfPossible(U.getLong(obj, j), cls);
        }
        Object object = U.getObject(obj, j);
        if (!(object instanceof Integer) || cls == Integer.class) {
            return (!(object instanceof Long) || cls == Long.class) ? object : boxLongIfPossible(((Long) object).longValue(), cls);
        }
        return boxIntIfPossible(((Integer) object).intValue(), cls);
    }

    public /* synthetic */ Object get(Object obj, Class cls) {
        if (this.type == Integer.TYPE) {
            return boxIntIfPossible(U.getInt(obj, this.offset), cls);
        }
        Class cls2 = this.type;
        Class cls3 = Long.TYPE;
        long j = this.offset;
        return cls2 == cls3 ? boxLongIfPossible(U.getLong(obj, j), cls) : U.getObject(obj, j);
    }

    public /* synthetic */ int getVolatile(Object obj) {
        if (this.type == Integer.TYPE) {
            return U.getIntVolatile(obj, this.offset);
        }
        if (this.type != Long.TYPE) {
            return toIntIfPossible(U.getObjectVolatile(obj, this.offset), true);
        }
        throw desugarWrongMethodTypeException();
    }

    public /* synthetic */ int getVolatile(int[] iArr, int i) {
        if (this.recv != int[].class) {
            throw new UnsupportedOperationException();
        }
        return U.getIntVolatile(iArr, this.offset + (((long) i) * this.arrayIndexScale));
    }

    /* JADX INFO: renamed from: getVolatile, reason: collision with other method in class */
    public /* synthetic */ long m180getVolatile(Object obj) {
        if (this.type == Long.TYPE) {
            return U.getLongVolatile(obj, this.offset);
        }
        Class cls = this.type;
        Class cls2 = Integer.TYPE;
        long j = this.offset;
        return cls == cls2 ? U.getIntVolatile(obj, j) : toLongIfPossible(U.getObjectVolatile(obj, j), true);
    }

    public /* synthetic */ long getVolatile(long[] jArr, int i) {
        if (this.recv != long[].class) {
            throw new UnsupportedOperationException();
        }
        return U.getLongVolatile(jArr, this.offset + (((long) i) * this.arrayIndexScale));
    }

    /* JADX INFO: renamed from: getVolatile, reason: collision with other method in class */
    public /* synthetic */ Object m181getVolatile(Object obj) {
        if (this.type == Integer.TYPE) {
            return Integer.valueOf(U.getIntVolatile(obj, this.offset));
        }
        Class cls = this.type;
        Class cls2 = Long.TYPE;
        long j = this.offset;
        Unsafe unsafe = U;
        return cls == cls2 ? Long.valueOf(unsafe.getLongVolatile(obj, j)) : unsafe.getObjectVolatile(obj, j);
    }

    public /* synthetic */ Object getVolatile(Object obj, int i) {
        if (!this.recv.isArray() || this.recv != obj.getClass()) {
            throw new UnsupportedOperationException();
        }
        long j = this.offset + (((long) i) * this.arrayIndexScale);
        if (this.type == Integer.TYPE) {
            return Integer.valueOf(U.getIntVolatile(obj, j));
        }
        return this.type == Long.TYPE ? Integer.valueOf((int) U.getLongVolatile(obj, j)) : U.getObjectVolatile(obj, j);
    }

    public /* synthetic */ Object getVolatile(Object obj, int i, Class cls) {
        if (!this.recv.isArray() || this.recv != obj.getClass()) {
            throw new UnsupportedOperationException();
        }
        long j = this.offset + (((long) i) * this.arrayIndexScale);
        if (this.type == Integer.TYPE) {
            return boxIntIfPossible(U.getIntVolatile(obj, j), cls);
        }
        if (this.type == Long.TYPE) {
            return boxLongIfPossible(U.getLongVolatile(obj, j), cls);
        }
        Object objectVolatile = U.getObjectVolatile(obj, j);
        if (!(objectVolatile instanceof Integer) || cls == Integer.class) {
            return (!(objectVolatile instanceof Long) || cls == Long.class) ? objectVolatile : boxLongIfPossible(((Long) objectVolatile).longValue(), cls);
        }
        return boxIntIfPossible(((Integer) objectVolatile).intValue(), cls);
    }

    public /* synthetic */ Object getVolatile(Object obj, Class cls) {
        if (this.type == Integer.TYPE) {
            return boxIntIfPossible(U.getIntVolatile(obj, this.offset), cls);
        }
        Class cls2 = this.type;
        Class cls3 = Long.TYPE;
        long j = this.offset;
        return cls2 == cls3 ? boxLongIfPossible(U.getLongVolatile(obj, j), cls) : U.getObjectVolatile(obj, j);
    }

    public /* synthetic */ void set(Object obj, int i) {
        if (this.type == Integer.TYPE) {
            U.putInt(obj, this.offset, i);
        } else if (this.type == Long.TYPE) {
            U.putLong(obj, this.offset, i);
        } else {
            set(obj, Integer.valueOf(i));
        }
    }

    public /* synthetic */ void set(Object obj, int i, Object obj2) {
        if (!this.recv.isArray() || this.recv != obj.getClass()) {
            throw new UnsupportedOperationException();
        }
        long j = (((long) i) * this.arrayIndexScale) + this.offset;
        Class cls = this.recv;
        if (cls == int[].class) {
            U.putInt(obj, j, toIntIfPossible(obj2, false));
        } else if (cls == long[].class) {
            U.putLong(obj, j, toLongIfPossible(obj2, false));
        } else {
            U.putObject(obj, j, obj2);
        }
    }

    public /* synthetic */ void set(Object obj, long j) {
        if (this.type == Long.TYPE) {
            U.putLong(obj, this.offset, j);
        } else {
            if (this.type == Integer.TYPE) {
                throw desugarWrongMethodTypeException();
            }
            U.putObject(obj, this.offset, Long.valueOf(j));
        }
    }

    public /* synthetic */ void set(Object obj, Object obj2) {
        if (this.type == Integer.TYPE) {
            set(obj, toIntIfPossible(obj2, false));
        } else if (this.type == Long.TYPE) {
            set(obj, toLongIfPossible(obj2, false));
        } else {
            U.putObject(obj, this.offset, obj2);
        }
    }

    public /* synthetic */ void set(int[] iArr, int i, int i2) {
        if (this.recv != int[].class) {
            throw new UnsupportedOperationException();
        }
        U.putInt(iArr, this.offset + (((long) i) * this.arrayIndexScale), i2);
    }

    public /* synthetic */ void set(long[] jArr, int i, long j) {
        if (this.recv != long[].class) {
            throw new UnsupportedOperationException();
        }
        U.putLong(jArr, this.offset + (((long) i) * this.arrayIndexScale), j);
    }

    public /* synthetic */ void setRelease(Object obj, int i) {
        if (this.type == Integer.TYPE) {
            U.putOrderedInt(obj, this.offset, i);
        } else if (this.type == Long.TYPE) {
            U.putOrderedLong(obj, this.offset, i);
        } else {
            setRelease(obj, Integer.valueOf(i));
        }
    }

    public /* synthetic */ void setRelease(Object obj, int i, Object obj2) {
        if (!this.recv.isArray() || this.recv != obj.getClass()) {
            throw new UnsupportedOperationException();
        }
        long j = (((long) i) * this.arrayIndexScale) + this.offset;
        Class cls = this.recv;
        if (cls == int[].class) {
            U.putOrderedInt(obj, j, toIntIfPossible(obj2, false));
        } else if (cls == long[].class) {
            U.putOrderedLong(obj, j, toLongIfPossible(obj2, false));
        } else {
            U.putOrderedObject(obj, j, obj2);
        }
    }

    public /* synthetic */ void setRelease(Object obj, long j) {
        if (this.type == Long.TYPE) {
            U.putOrderedLong(obj, this.offset, j);
        } else {
            if (this.type == Integer.TYPE) {
                throw desugarWrongMethodTypeException();
            }
            U.putOrderedObject(obj, this.offset, Long.valueOf(j));
        }
    }

    public /* synthetic */ void setRelease(Object obj, Object obj2) {
        if (this.type == Integer.TYPE) {
            setRelease(obj, toIntIfPossible(obj2, false));
        } else if (this.type == Long.TYPE) {
            setRelease(obj, toLongIfPossible(obj2, false));
        } else {
            U.putOrderedObject(obj, this.offset, obj2);
        }
    }

    public /* synthetic */ void setRelease(int[] iArr, int i, int i2) {
        if (this.recv != int[].class) {
            throw new UnsupportedOperationException();
        }
        U.putOrderedInt(iArr, this.offset + (((long) i) * this.arrayIndexScale), i2);
    }

    public /* synthetic */ void setRelease(long[] jArr, int i, long j) {
        if (this.recv != long[].class) {
            throw new UnsupportedOperationException();
        }
        U.putOrderedLong(jArr, this.offset + (((long) i) * this.arrayIndexScale), j);
    }

    public /* synthetic */ void setVolatile(Object obj, int i) {
        if (this.type == Integer.TYPE) {
            U.putIntVolatile(obj, this.offset, i);
        } else if (this.type == Long.TYPE) {
            U.putLongVolatile(obj, this.offset, i);
        } else {
            setVolatile(obj, Integer.valueOf(i));
        }
    }

    public /* synthetic */ void setVolatile(Object obj, int i, Object obj2) {
        if (!this.recv.isArray() || this.recv != obj.getClass()) {
            throw new UnsupportedOperationException();
        }
        long j = (((long) i) * this.arrayIndexScale) + this.offset;
        Class cls = this.recv;
        if (cls == int[].class) {
            U.putIntVolatile(obj, j, toIntIfPossible(obj2, false));
        } else if (cls == long[].class) {
            U.putLongVolatile(obj, j, toLongIfPossible(obj2, false));
        } else {
            U.putObjectVolatile(obj, j, obj2);
        }
    }

    public /* synthetic */ void setVolatile(Object obj, long j) {
        if (this.type == Long.TYPE) {
            U.putLongVolatile(obj, this.offset, j);
        } else {
            if (this.type == Integer.TYPE) {
                throw desugarWrongMethodTypeException();
            }
            U.putObjectVolatile(obj, this.offset, Long.valueOf(j));
        }
    }

    public /* synthetic */ void setVolatile(Object obj, Object obj2) {
        if (this.type == Integer.TYPE) {
            setVolatile(obj, toIntIfPossible(obj2, false));
        } else if (this.type == Long.TYPE) {
            setVolatile(obj, toLongIfPossible(obj2, false));
        } else {
            U.putObjectVolatile(obj, this.offset, obj2);
        }
    }

    public /* synthetic */ void setVolatile(int[] iArr, int i, int i2) {
        if (this.recv != int[].class) {
            throw new UnsupportedOperationException();
        }
        U.putIntVolatile(iArr, this.offset + (((long) i) * this.arrayIndexScale), i2);
    }

    public /* synthetic */ void setVolatile(long[] jArr, int i, long j) {
        if (this.recv != long[].class) {
            throw new UnsupportedOperationException();
        }
        U.putLongVolatile(jArr, this.offset + (((long) i) * this.arrayIndexScale), j);
    }

    public /* synthetic */ int toIntIfPossible(Object obj, boolean z) {
        if (obj instanceof Integer) {
            return ((Integer) obj).intValue();
        }
        if (obj instanceof Byte) {
            return ((Byte) obj).byteValue();
        }
        if (obj instanceof Character) {
            return ((Character) obj).charValue();
        }
        if (obj instanceof Short) {
            return ((Short) obj).shortValue();
        }
        if (z) {
            throw new ClassCastException();
        }
        throw desugarWrongMethodTypeException();
    }

    public /* synthetic */ long toLongIfPossible(Object obj, boolean z) {
        return obj instanceof Long ? ((Long) obj).longValue() : toIntIfPossible(obj, z);
    }

    public /* synthetic */ boolean weakCompareAndSet(Object obj, int i, int i2) {
        if (this.type == Integer.TYPE) {
            return U.compareAndSwapInt(obj, this.offset, i, i2);
        }
        return this.type == Long.TYPE ? U.compareAndSwapLong(obj, this.offset, i, i2) : compareAndSet(obj, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public /* synthetic */ boolean weakCompareAndSet(Object obj, int i, Object obj2, Object obj3) {
        if (!this.recv.isArray() || this.recv != obj.getClass()) {
            throw new UnsupportedOperationException();
        }
        long j = (((long) i) * this.arrayIndexScale) + this.offset;
        Class cls = this.recv;
        if (cls == int[].class) {
            return U.compareAndSwapInt(obj, j, toIntIfPossible(obj2, false), toIntIfPossible(obj3, false));
        }
        return cls == long[].class ? U.compareAndSwapLong(obj, j, toLongIfPossible(obj2, false), toLongIfPossible(obj3, false)) : AnonymousClass0.m(U, obj, j, obj2, obj3);
    }

    public /* synthetic */ boolean weakCompareAndSet(Object obj, long j, long j2) {
        return this.type == Long.TYPE ? U.compareAndSwapLong(obj, this.offset, j, j2) : compareAndSet(obj, Long.valueOf(j), Long.valueOf(j2));
    }

    public /* synthetic */ boolean weakCompareAndSet(Object obj, Object obj2, Object obj3) {
        if (this.type == Integer.TYPE) {
            return U.compareAndSwapInt(obj, this.offset, toIntIfPossible(obj2, false), toIntIfPossible(obj3, false));
        }
        Class cls = this.type;
        Class cls2 = Long.TYPE;
        long j = this.offset;
        return cls == cls2 ? U.compareAndSwapLong(obj, j, toLongIfPossible(obj2, false), toLongIfPossible(obj3, false)) : AnonymousClass0.m(U, obj, j, obj2, obj3);
    }

    public /* synthetic */ boolean weakCompareAndSet(int[] iArr, int i, int i2, int i3) {
        if (this.recv != int[].class) {
            throw new UnsupportedOperationException();
        }
        return U.compareAndSwapInt(iArr, this.offset + (((long) i) * this.arrayIndexScale), i2, i3);
    }

    public /* synthetic */ boolean weakCompareAndSet(long[] jArr, int i, long j, long j2) {
        if (this.recv != long[].class) {
            throw new UnsupportedOperationException();
        }
        return U.compareAndSwapLong(jArr, this.offset + (((long) i) * this.arrayIndexScale), j, j2);
    }
}
