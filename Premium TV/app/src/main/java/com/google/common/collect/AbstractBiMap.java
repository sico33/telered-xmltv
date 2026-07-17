package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class AbstractBiMap<K, V> extends ForwardingMap<K, V> implements BiMap<K, V>, Serializable {
    private static final long serialVersionUID = 0;
    private transient Map<K, V> delegate;

    @CheckForNull
    @LazyInit
    private transient Set<Map.Entry<K, V>> entrySet;
    transient AbstractBiMap<V, K> inverse;

    @CheckForNull
    @LazyInit
    private transient Set<K> keySet;

    @CheckForNull
    @LazyInit
    private transient Set<V> valueSet;

    class BiMapEntry extends ForwardingMapEntry<K, V> {
        private final Map.Entry<K, V> delegate;
        final AbstractBiMap this$0;

        BiMapEntry(AbstractBiMap abstractBiMap, Map.Entry<K, V> entry) {
            this.this$0 = abstractBiMap;
            this.delegate = entry;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.ForwardingMapEntry, com.google.common.collect.ForwardingObject
        public Map.Entry<K, V> delegate() {
            return this.delegate;
        }

        @Override // com.google.common.collect.ForwardingMapEntry, java.util.Map.Entry
        public V setValue(V v) {
            this.this$0.checkValue(v);
            Preconditions.checkState(this.this$0.entrySet().contains(this), "entry no longer in map");
            if (Objects.equal(v, getValue())) {
                return v;
            }
            Preconditions.checkArgument(!this.this$0.containsValue(v), "value already present: %s", v);
            V value = this.delegate.setValue(v);
            Preconditions.checkState(Objects.equal(v, this.this$0.get(getKey())), "entry no longer in map");
            this.this$0.updateInverseMap(getKey(), true, value, v);
            return value;
        }
    }

    private class EntrySet extends ForwardingSet<Map.Entry<K, V>> {
        final Set<Map.Entry<K, V>> esDelegate;
        final AbstractBiMap this$0;

        private EntrySet(AbstractBiMap abstractBiMap) {
            this.this$0 = abstractBiMap;
            this.esDelegate = this.this$0.delegate.entrySet();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public void clear() {
            this.this$0.clear();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            return Maps.containsEntryImpl(delegate(), obj);
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean containsAll(Collection<?> collection) {
            return standardContainsAll(collection);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.ForwardingSet, com.google.common.collect.ForwardingCollection, com.google.common.collect.ForwardingObject
        public Set<Map.Entry<K, V>> delegate() {
            return this.esDelegate;
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<Map.Entry<K, V>> iterator() {
            return this.this$0.entrySetIterator();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            if (!this.esDelegate.contains(obj) || !(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            ((AbstractBiMap) this.this$0.inverse).delegate.remove(entry.getValue());
            this.esDelegate.remove(entry);
            return true;
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean removeAll(Collection<?> collection) {
            return standardRemoveAll(collection);
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean retainAll(Collection<?> collection) {
            return standardRetainAll(collection);
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public Object[] toArray() {
            return standardToArray();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public <T> T[] toArray(T[] tArr) {
            return (T[]) standardToArray(tArr);
        }
    }

    static class Inverse<K, V> extends AbstractBiMap<K, V> {
        private static final long serialVersionUID = 0;

        Inverse(Map<K, V> map, AbstractBiMap<V, K> abstractBiMap) {
            super(map, abstractBiMap);
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            setInverse((AbstractBiMap) java.util.Objects.requireNonNull(objectInputStream.readObject()));
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(inverse());
        }

        @Override // com.google.common.collect.AbstractBiMap
        @ParametricNullness
        K checkKey(@ParametricNullness K k) {
            return this.inverse.checkValue(k);
        }

        @Override // com.google.common.collect.AbstractBiMap
        @ParametricNullness
        V checkValue(@ParametricNullness V v) {
            return this.inverse.checkKey(v);
        }

        @Override // com.google.common.collect.AbstractBiMap, com.google.common.collect.ForwardingMap, com.google.common.collect.ForwardingObject
        protected /* bridge */ /* synthetic */ Object delegate() {
            return super.delegate();
        }

        Object readResolve() {
            return inverse().inverse();
        }

        @Override // com.google.common.collect.AbstractBiMap, com.google.common.collect.ForwardingMap, java.util.Map
        public /* bridge */ /* synthetic */ Collection values() {
            return super.values();
        }
    }

    private class KeySet extends ForwardingSet<K> {
        final AbstractBiMap this$0;

        private KeySet(AbstractBiMap abstractBiMap) {
            this.this$0 = abstractBiMap;
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public void clear() {
            this.this$0.clear();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.ForwardingSet, com.google.common.collect.ForwardingCollection, com.google.common.collect.ForwardingObject
        public Set<K> delegate() {
            return this.this$0.delegate.keySet();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<K> iterator() {
            return Maps.keyIterator(this.this$0.entrySet().iterator());
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            if (!contains(obj)) {
                return false;
            }
            this.this$0.removeFromBothMaps(obj);
            return true;
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean removeAll(Collection<?> collection) {
            return standardRemoveAll(collection);
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean retainAll(Collection<?> collection) {
            return standardRetainAll(collection);
        }
    }

    private class ValueSet extends ForwardingSet<V> {
        final AbstractBiMap this$0;
        final Set<V> valuesDelegate;

        private ValueSet(AbstractBiMap abstractBiMap) {
            this.this$0 = abstractBiMap;
            this.valuesDelegate = this.this$0.inverse.keySet();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.ForwardingSet, com.google.common.collect.ForwardingCollection, com.google.common.collect.ForwardingObject
        public Set<V> delegate() {
            return this.valuesDelegate;
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<V> iterator() {
            return Maps.valueIterator(this.this$0.entrySet().iterator());
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public Object[] toArray() {
            return standardToArray();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public <T> T[] toArray(T[] tArr) {
            return (T[]) standardToArray(tArr);
        }

        @Override // com.google.common.collect.ForwardingObject
        public String toString() {
            return standardToString();
        }
    }

    private AbstractBiMap(Map<K, V> map, AbstractBiMap<V, K> abstractBiMap) {
        this.delegate = map;
        this.inverse = abstractBiMap;
    }

    AbstractBiMap(Map<K, V> map, Map<V, K> map2) {
        setDelegates(map, map2);
    }

    @CheckForNull
    private V putInBothMaps(@ParametricNullness K k, @ParametricNullness V v, boolean z) {
        checkKey(k);
        checkValue(v);
        boolean zContainsKey = containsKey(k);
        if (zContainsKey && Objects.equal(v, get(k))) {
            return v;
        }
        if (z) {
            inverse().remove(v);
        } else {
            Preconditions.checkArgument(!containsValue(v), "value already present: %s", v);
        }
        V vPut = this.delegate.put(k, v);
        updateInverseMap(k, zContainsKey, vPut, v);
        return vPut;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @ParametricNullness
    public V removeFromBothMaps(@CheckForNull Object obj) {
        V v = (V) NullnessCasts.uncheckedCastNullableTToT(this.delegate.remove(obj));
        removeFromInverseMap(v);
        return v;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeFromInverseMap(@ParametricNullness V v) {
        this.inverse.delegate.remove(v);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    public void updateInverseMap(@ParametricNullness K k, boolean z, @CheckForNull V v, @ParametricNullness V v2) {
        if (z) {
            removeFromInverseMap(NullnessCasts.uncheckedCastNullableTToT(v));
        }
        this.inverse.delegate.put(v2, k);
    }

    @ParametricNullness
    K checkKey(@ParametricNullness K k) {
        return k;
    }

    @ParametricNullness
    V checkValue(@ParametricNullness V v) {
        return v;
    }

    @Override // com.google.common.collect.ForwardingMap, java.util.Map
    public void clear() {
        this.delegate.clear();
        this.inverse.delegate.clear();
    }

    @Override // com.google.common.collect.ForwardingMap, java.util.Map
    public boolean containsValue(@CheckForNull Object obj) {
        return this.inverse.containsKey(obj);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.google.common.collect.ForwardingMap, com.google.common.collect.ForwardingObject
    public Map<K, V> delegate() {
        return this.delegate;
    }

    @Override // com.google.common.collect.ForwardingMap, java.util.Map
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = this.entrySet;
        if (set != null) {
            return set;
        }
        EntrySet entrySet = new EntrySet();
        this.entrySet = entrySet;
        return entrySet;
    }

    Iterator<Map.Entry<K, V>> entrySetIterator() {
        return new Iterator<Map.Entry<K, V>>(this, this.delegate.entrySet().iterator()) { // from class: com.google.common.collect.AbstractBiMap.1

            @CheckForNull
            Map.Entry<K, V> entry;
            final AbstractBiMap this$0;
            final Iterator val$iterator;

            {
                this.this$0 = this;
                this.val$iterator = it;
            }

            @Override // java.util.Iterator
            public boolean hasNext() {
                return this.val$iterator.hasNext();
            }

            @Override // java.util.Iterator
            public Map.Entry<K, V> next() {
                this.entry = (Map.Entry) this.val$iterator.next();
                return new BiMapEntry(this.this$0, this.entry);
            }

            @Override // java.util.Iterator
            public void remove() {
                if (this.entry == null) {
                    throw new IllegalStateException("no calls to next() since the last call to remove()");
                }
                V value = this.entry.getValue();
                this.val$iterator.remove();
                this.this$0.removeFromInverseMap(value);
                this.entry = null;
            }
        };
    }

    @Override // com.google.common.collect.BiMap
    @CheckForNull
    public V forcePut(@ParametricNullness K k, @ParametricNullness V v) {
        return putInBothMaps(k, v, true);
    }

    @Override // com.google.common.collect.BiMap
    public BiMap<V, K> inverse() {
        return this.inverse;
    }

    @Override // com.google.common.collect.ForwardingMap, java.util.Map
    public Set<K> keySet() {
        Set<K> set = this.keySet;
        if (set != null) {
            return set;
        }
        KeySet keySet = new KeySet();
        this.keySet = keySet;
        return keySet;
    }

    AbstractBiMap<V, K> makeInverse(Map<V, K> map) {
        return new Inverse(map, this);
    }

    @Override // com.google.common.collect.ForwardingMap, java.util.Map, com.google.common.collect.BiMap
    @CheckForNull
    public V put(@ParametricNullness K k, @ParametricNullness V v) {
        return putInBothMaps(k, v, false);
    }

    @Override // com.google.common.collect.ForwardingMap, java.util.Map, com.google.common.collect.BiMap
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override // com.google.common.collect.ForwardingMap, java.util.Map
    @CheckForNull
    public V remove(@CheckForNull Object obj) {
        if (containsKey(obj)) {
            return removeFromBothMaps(obj);
        }
        return null;
    }

    void setDelegates(Map<K, V> map, Map<V, K> map2) {
        Preconditions.checkState(this.delegate == null);
        Preconditions.checkState(this.inverse == null);
        Preconditions.checkArgument(map.isEmpty());
        Preconditions.checkArgument(map2.isEmpty());
        Preconditions.checkArgument(map != map2);
        this.delegate = map;
        this.inverse = makeInverse(map2);
    }

    void setInverse(AbstractBiMap<V, K> abstractBiMap) {
        this.inverse = abstractBiMap;
    }

    @Override // com.google.common.collect.ForwardingMap, java.util.Map
    public Set<V> values() {
        Set<V> set = this.valueSet;
        if (set != null) {
            return set;
        }
        ValueSet valueSet = new ValueSet();
        this.valueSet = valueSet;
        return valueSet;
    }
}
