package com.google.common.collect;

import com.google.common.base.Objects;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class ForwardingMap<K, V> extends ForwardingObject implements Map<K, V> {

    protected abstract class StandardEntrySet extends Maps.EntrySet<K, V> {
        final ForwardingMap this$0;

        protected StandardEntrySet(ForwardingMap forwardingMap) {
            this.this$0 = forwardingMap;
        }

        @Override // com.google.common.collect.Maps.EntrySet
        Map<K, V> map() {
            return this.this$0;
        }
    }

    protected class StandardKeySet extends Maps.KeySet<K, V> {
        public StandardKeySet(ForwardingMap forwardingMap) {
            super(forwardingMap);
        }
    }

    protected class StandardValues extends Maps.Values<K, V> {
        public StandardValues(ForwardingMap forwardingMap) {
            super(forwardingMap);
        }
    }

    protected ForwardingMap() {
    }

    public void clear() {
        delegate().clear();
    }

    public boolean containsKey(@CheckForNull Object obj) {
        return delegate().containsKey(obj);
    }

    public boolean containsValue(@CheckForNull Object obj) {
        return delegate().containsValue(obj);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.google.common.collect.ForwardingObject
    public abstract Map<K, V> delegate();

    public Set<Map.Entry<K, V>> entrySet() {
        return delegate().entrySet();
    }

    public boolean equals(@CheckForNull Object obj) {
        return obj == this || delegate().equals(obj);
    }

    @CheckForNull
    public V get(@CheckForNull Object obj) {
        return delegate().get(obj);
    }

    public int hashCode() {
        return delegate().hashCode();
    }

    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    public Set<K> keySet() {
        return delegate().keySet();
    }

    @CheckForNull
    public V put(@ParametricNullness K k, @ParametricNullness V v) {
        return delegate().put(k, v);
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        delegate().putAll(map);
    }

    @CheckForNull
    public V remove(@CheckForNull Object obj) {
        return delegate().remove(obj);
    }

    public int size() {
        return delegate().size();
    }

    protected void standardClear() {
        Iterators.clear(entrySet().iterator());
    }

    protected boolean standardContainsKey(@CheckForNull Object obj) {
        return Maps.containsKeyImpl(this, obj);
    }

    protected boolean standardContainsValue(@CheckForNull Object obj) {
        return Maps.containsValueImpl(this, obj);
    }

    protected boolean standardEquals(@CheckForNull Object obj) {
        return Maps.equalsImpl(this, obj);
    }

    protected int standardHashCode() {
        return Sets.hashCodeImpl(entrySet());
    }

    protected boolean standardIsEmpty() {
        return !entrySet().iterator().hasNext();
    }

    protected void standardPutAll(Map<? extends K, ? extends V> map) {
        Maps.putAllImpl(this, map);
    }

    @CheckForNull
    protected V standardRemove(@CheckForNull Object obj) {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> next = it.next();
            if (Objects.equal(next.getKey(), obj)) {
                V value = next.getValue();
                it.remove();
                return value;
            }
        }
        return null;
    }

    protected String standardToString() {
        return Maps.toStringImpl(this);
    }

    public Collection<V> values() {
        return delegate().values();
    }
}
