package com.google.common.base;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Enums {
    private static final Map<Class<? extends Enum<?>>, Map<String, WeakReference<? extends Enum<?>>>> enumConstantCache = new WeakHashMap();

    private static final class StringConverter<T extends Enum<T>> extends Converter<String, T> implements Serializable {
        private static final long serialVersionUID = 0;
        private final Class<T> enumClass;

        StringConverter(Class<T> cls) {
            this.enumClass = (Class) Preconditions.checkNotNull(cls);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.base.Converter
        public String doBackward(T t) {
            return t.name();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.base.Converter
        public T doForward(String str) {
            return (T) Enum.valueOf(this.enumClass, str);
        }

        @Override // com.google.common.base.Converter, com.google.common.base.Function
        public boolean equals(@CheckForNull Object obj) {
            if (obj instanceof StringConverter) {
                return this.enumClass.equals(((StringConverter) obj).enumClass);
            }
            return false;
        }

        public int hashCode() {
            return this.enumClass.hashCode();
        }

        public String toString() {
            return "Enums.stringConverter(" + this.enumClass.getName() + ".class)";
        }
    }

    private Enums() {
    }

    static <T extends Enum<T>> Map<String, WeakReference<? extends Enum<?>>> getEnumConstants(Class<T> cls) {
        Map<String, WeakReference<? extends Enum<?>>> mapPopulateCache;
        synchronized (enumConstantCache) {
            mapPopulateCache = enumConstantCache.get(cls);
            if (mapPopulateCache == null) {
                mapPopulateCache = populateCache(cls);
            }
        }
        return mapPopulateCache;
    }

    public static Field getField(Enum<?> r2) {
        try {
            return r2.getDeclaringClass().getDeclaredField(r2.name());
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    public static <T extends Enum<T>> Optional<T> getIfPresent(Class<T> cls, String str) {
        Preconditions.checkNotNull(cls);
        Preconditions.checkNotNull(str);
        return Platform.getEnumIfPresent(cls, str);
    }

    private static <T extends Enum<T>> Map<String, WeakReference<? extends Enum<?>>> populateCache(Class<T> cls) {
        HashMap map = new HashMap();
        for (Enum r0 : EnumSet.allOf(cls)) {
            map.put(r0.name(), new WeakReference(r0));
        }
        enumConstantCache.put(cls, map);
        return map;
    }

    public static <T extends Enum<T>> Converter<String, T> stringConverter(Class<T> cls) {
        return new StringConverter(cls);
    }
}
