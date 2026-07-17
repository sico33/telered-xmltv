package com.google.common.collect;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class IndexedImmutableSet<E> extends ImmutableSet<E> {
    IndexedImmutableSet() {
    }

    @Override // com.google.common.collect.ImmutableCollection
    int copyIntoArray(Object[] objArr, int i) {
        return asList().copyIntoArray(objArr, i);
    }

    @Override // com.google.common.collect.ImmutableSet
    ImmutableList<E> createAsList() {
        return new ImmutableList<E>(this) { // from class: com.google.common.collect.IndexedImmutableSet.1
            final IndexedImmutableSet this$0;

            {
                this.this$0 = this;
            }

            @Override // java.util.List
            public E get(int i) {
                return (E) this.this$0.get(i);
            }

            @Override // com.google.common.collect.ImmutableCollection
            boolean isPartialView() {
                return this.this$0.isPartialView();
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
            public int size() {
                return this.this$0.size();
            }

            @Override // com.google.common.collect.ImmutableList, com.google.common.collect.ImmutableCollection
            Object writeReplace() {
                return super.writeReplace();
            }
        };
    }

    abstract E get(int i);

    @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet, com.google.common.collect.SortedIterable
    public UnmodifiableIterator<E> iterator() {
        return asList().iterator();
    }

    @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
    Object writeReplace() {
        return super.writeReplace();
    }
}
