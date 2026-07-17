package com.bumptech.glide.load.engine.bitmap_recycle;

import android.util.Log;
import com.bumptech.glide.util.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/* JADX INFO: loaded from: classes.dex */
public final class LruArrayPool implements ArrayPool {
    private static final int DEFAULT_SIZE = 4194304;
    static final int MAX_OVER_SIZE_MULTIPLE = 8;
    private static final int SINGLE_ARRAY_MAX_SIZE_DIVISOR = 2;
    private final Map<Class<?>, ArrayAdapterInterface<?>> adapters;
    private int currentSize;
    private final GroupedLinkedMap<Key, Object> groupedMap;
    private final KeyPool keyPool;
    private final int maxSize;
    private final Map<Class<?>, NavigableMap<Integer, Integer>> sortedSizes;

    private static final class Key implements Poolable {
        private Class<?> arrayClass;
        private final KeyPool pool;
        int size;

        Key(KeyPool keyPool) {
            this.pool = keyPool;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            Key key = (Key) obj;
            return this.size == key.size && this.arrayClass == key.arrayClass;
        }

        public int hashCode() {
            return (this.arrayClass != null ? this.arrayClass.hashCode() : 0) + (this.size * 31);
        }

        void init(int i, Class<?> cls) {
            this.size = i;
            this.arrayClass = cls;
        }

        @Override // com.bumptech.glide.load.engine.bitmap_recycle.Poolable
        public void offer() {
            this.pool.offer(this);
        }

        public String toString() {
            return "Key{size=" + this.size + "array=" + this.arrayClass + '}';
        }
    }

    private static final class KeyPool extends BaseKeyPool<Key> {
        KeyPool() {
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.bumptech.glide.load.engine.bitmap_recycle.BaseKeyPool
        public Key create() {
            return new Key(this);
        }

        Key get(int i, Class<?> cls) {
            Key key = get();
            key.init(i, cls);
            return key;
        }
    }

    public LruArrayPool() {
        this.groupedMap = new GroupedLinkedMap<>();
        this.keyPool = new KeyPool();
        this.sortedSizes = new HashMap();
        this.adapters = new HashMap();
        this.maxSize = 4194304;
    }

    public LruArrayPool(int i) {
        this.groupedMap = new GroupedLinkedMap<>();
        this.keyPool = new KeyPool();
        this.sortedSizes = new HashMap();
        this.adapters = new HashMap();
        this.maxSize = i;
    }

    private void decrementArrayOfSize(int i, Class<?> cls) {
        NavigableMap<Integer, Integer> sizesForAdapter = getSizesForAdapter(cls);
        Integer num = (Integer) sizesForAdapter.get(Integer.valueOf(i));
        if (num == null) {
            throw new NullPointerException("Tried to decrement empty size, size: " + i + ", this: " + this);
        }
        if (num.intValue() == 1) {
            sizesForAdapter.remove(Integer.valueOf(i));
        } else {
            sizesForAdapter.put(Integer.valueOf(i), Integer.valueOf(num.intValue() - 1));
        }
    }

    private void evict() {
        evictToSize(this.maxSize);
    }

    private void evictToSize(int i) {
        while (this.currentSize > i) {
            Object objRemoveLast = this.groupedMap.removeLast();
            Preconditions.checkNotNull(objRemoveLast);
            ArrayAdapterInterface adapterFromObject = getAdapterFromObject(objRemoveLast);
            this.currentSize -= adapterFromObject.getArrayLength(objRemoveLast) * adapterFromObject.getElementSizeInBytes();
            decrementArrayOfSize(adapterFromObject.getArrayLength(objRemoveLast), objRemoveLast.getClass());
            if (Log.isLoggable(adapterFromObject.getTag(), 2)) {
                Log.v(adapterFromObject.getTag(), "evicted: " + adapterFromObject.getArrayLength(objRemoveLast));
            }
        }
    }

    private <T> ArrayAdapterInterface<T> getAdapterFromObject(T t) {
        return getAdapterFromType(t.getClass());
    }

    private <T> ArrayAdapterInterface<T> getAdapterFromType(Class<T> cls) {
        ArrayAdapterInterface<T> byteArrayAdapter = (ArrayAdapterInterface) this.adapters.get(cls);
        if (byteArrayAdapter == null) {
            if (cls.equals(int[].class)) {
                byteArrayAdapter = new IntegerArrayAdapter();
            } else {
                if (!cls.equals(byte[].class)) {
                    throw new IllegalArgumentException("No array pool found for: " + cls.getSimpleName());
                }
                byteArrayAdapter = new ByteArrayAdapter();
            }
            this.adapters.put(cls, byteArrayAdapter);
        }
        return byteArrayAdapter;
    }

    private <T> T getArrayForKey(Key key) {
        return (T) this.groupedMap.get(key);
    }

    private <T> T getForKey(Key key, Class<T> cls) {
        ArrayAdapterInterface<T> adapterFromType = getAdapterFromType(cls);
        T t = (T) getArrayForKey(key);
        if (t != null) {
            this.currentSize -= adapterFromType.getArrayLength(t) * adapterFromType.getElementSizeInBytes();
            decrementArrayOfSize(adapterFromType.getArrayLength(t), cls);
        }
        if (t != null) {
            return t;
        }
        if (Log.isLoggable(adapterFromType.getTag(), 2)) {
            Log.v(adapterFromType.getTag(), "Allocated " + key.size + " bytes");
        }
        return adapterFromType.newArray(key.size);
    }

    private NavigableMap<Integer, Integer> getSizesForAdapter(Class<?> cls) {
        NavigableMap<Integer, Integer> navigableMap = this.sortedSizes.get(cls);
        if (navigableMap != null) {
            return navigableMap;
        }
        TreeMap treeMap = new TreeMap();
        this.sortedSizes.put(cls, treeMap);
        return treeMap;
    }

    private boolean isNoMoreThanHalfFull() {
        return this.currentSize == 0 || this.maxSize / this.currentSize >= 2;
    }

    private boolean isSmallEnoughForReuse(int i) {
        return i <= this.maxSize / 2;
    }

    private boolean mayFillRequest(int i, Integer num) {
        return num != null && (isNoMoreThanHalfFull() || num.intValue() <= i * 8);
    }

    @Override // com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
    public void clearMemory() {
        synchronized (this) {
            evictToSize(0);
        }
    }

    @Override // com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
    public <T> T get(int i, Class<T> cls) {
        T t;
        synchronized (this) {
            Integer numCeilingKey = getSizesForAdapter(cls).ceilingKey(Integer.valueOf(i));
            boolean zMayFillRequest = mayFillRequest(i, numCeilingKey);
            KeyPool keyPool = this.keyPool;
            t = (T) getForKey(zMayFillRequest ? keyPool.get(numCeilingKey.intValue(), cls) : keyPool.get(i, cls), cls);
        }
        return t;
    }

    int getCurrentSize() {
        int i = 0;
        for (Class<?> cls : this.sortedSizes.keySet()) {
            int iIntValue = i;
            for (Integer num : this.sortedSizes.get(cls).keySet()) {
                iIntValue += ((Integer) this.sortedSizes.get(cls).get(num)).intValue() * num.intValue() * getAdapterFromType(cls).getElementSizeInBytes();
            }
            i = iIntValue;
        }
        return i;
    }

    @Override // com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
    public <T> T getExact(int i, Class<T> cls) {
        T t;
        synchronized (this) {
            t = (T) getForKey(this.keyPool.get(i, cls), cls);
        }
        return t;
    }

    @Override // com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
    public <T> void put(T t) {
        synchronized (this) {
            Class<?> cls = t.getClass();
            ArrayAdapterInterface<T> adapterFromType = getAdapterFromType(cls);
            int arrayLength = adapterFromType.getArrayLength(t);
            int elementSizeInBytes = adapterFromType.getElementSizeInBytes() * arrayLength;
            if (isSmallEnoughForReuse(elementSizeInBytes)) {
                Key key = this.keyPool.get(arrayLength, cls);
                this.groupedMap.put(key, t);
                NavigableMap<Integer, Integer> sizesForAdapter = getSizesForAdapter(cls);
                Integer num = (Integer) sizesForAdapter.get(Integer.valueOf(key.size));
                sizesForAdapter.put(Integer.valueOf(key.size), Integer.valueOf(num == null ? 1 : num.intValue() + 1));
                this.currentSize += elementSizeInBytes;
                evict();
            }
        }
    }

    @Override // com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
    @Deprecated
    public <T> void put(T t, Class<T> cls) {
        put(t);
    }

    @Override // com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
    public void trimMemory(int i) {
        synchronized (this) {
            try {
                if (i >= 40) {
                    clearMemory();
                } else if (i >= 20 || i == 15) {
                    evictToSize(this.maxSize / 2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }
}
