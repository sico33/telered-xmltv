package com.google.common.collect;

import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.Serializable;
import java.util.Iterator;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
class RegularImmutableMultiset<E> extends ImmutableMultiset<E> {
    static final RegularImmutableMultiset<Object> EMPTY = new RegularImmutableMultiset<>(ObjectCountHashMap.create());
    final transient ObjectCountHashMap<E> contents;

    @CheckForNull
    @LazyInit
    private transient ImmutableSet<E> elementSet;
    private final transient int size;

    private final class ElementSet extends IndexedImmutableSet<E> {
        final RegularImmutableMultiset this$0;

        private ElementSet(RegularImmutableMultiset regularImmutableMultiset) {
            this.this$0 = regularImmutableMultiset;
        }

        @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            return this.this$0.contains(obj);
        }

        @Override // com.google.common.collect.IndexedImmutableSet
        E get(int i) {
            return this.this$0.contents.getKey(i);
        }

        @Override // com.google.common.collect.ImmutableCollection
        boolean isPartialView() {
            return true;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return this.this$0.contents.size();
        }

        @Override // com.google.common.collect.IndexedImmutableSet, com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
        Object writeReplace() {
            return super.writeReplace();
        }
    }

    private static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final int[] counts;
        final Object[] elements;

        SerializedForm(Multiset<? extends Object> multiset) {
            int size = multiset.entrySet().size();
            this.elements = new Object[size];
            this.counts = new int[size];
            int i = 0;
            Iterator<Multiset.Entry<? extends Object>> it = multiset.entrySet().iterator();
            while (true) {
                int i2 = i;
                if (!it.hasNext()) {
                    return;
                }
                Multiset.Entry<? extends Object> next = it.next();
                this.elements[i2] = next.getElement();
                this.counts[i2] = next.getCount();
                i = i2 + 1;
            }
        }

        /* JADX WARN: Multi-variable type inference failed */
        Object readResolve() {
            ImmutableMultiset.Builder builder = new ImmutableMultiset.Builder(this.elements.length);
            for (int i = 0; i < this.elements.length; i++) {
                builder.addCopies(this.elements[i], this.counts[i]);
            }
            return builder.build();
        }
    }

    RegularImmutableMultiset(ObjectCountHashMap<E> objectCountHashMap) {
        this.contents = objectCountHashMap;
        long value = 0;
        for (int i = 0; i < objectCountHashMap.size(); i++) {
            value += (long) objectCountHashMap.getValue(i);
        }
        this.size = Ints.saturatedCast(value);
    }

    @Override // com.google.common.collect.Multiset
    public int count(@CheckForNull Object obj) {
        return this.contents.get(obj);
    }

    @Override // com.google.common.collect.ImmutableMultiset, com.google.common.collect.Multiset
    public ImmutableSet<E> elementSet() {
        ImmutableSet<E> immutableSet = this.elementSet;
        if (immutableSet != null) {
            return immutableSet;
        }
        ElementSet elementSet = new ElementSet();
        this.elementSet = elementSet;
        return elementSet;
    }

    @Override // com.google.common.collect.ImmutableMultiset
    Multiset.Entry<E> getEntry(int i) {
        return this.contents.getEntry(i);
    }

    @Override // com.google.common.collect.ImmutableCollection
    boolean isPartialView() {
        return false;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
    public int size() {
        return this.size;
    }

    @Override // com.google.common.collect.ImmutableMultiset, com.google.common.collect.ImmutableCollection
    Object writeReplace() {
        return new SerializedForm(this);
    }
}
