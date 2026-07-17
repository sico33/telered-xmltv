package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class ImmutableSortedMultiset<E> extends ImmutableMultiset<E> implements SortedMultiset<E> {
    private static final long serialVersionUID = 912559;

    @CheckForNull
    @LazyInit
    transient ImmutableSortedMultiset<E> descendingMultiset;

    public static class Builder<E> extends ImmutableMultiset.Builder<E> {
        private final Comparator<? super E> comparator;
        private int[] counts;
        E[] elements;
        private boolean forceCopyElements;
        private int length;

        public Builder(Comparator<? super E> comparator) {
            super(true);
            this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
            this.elements = (E[]) new Object[4];
            this.counts = new int[4];
        }

        private void dedupAndCoalesce(boolean z) {
            if (this.length == 0) {
                return;
            }
            Object[] objArrCopyOf = Arrays.copyOf(this.elements, this.length);
            Arrays.sort(objArrCopyOf, this.comparator);
            int i = 1;
            for (int i2 = 1; i2 < objArrCopyOf.length; i2++) {
                if (this.comparator.compare(objArrCopyOf[i - 1], objArrCopyOf[i2]) < 0) {
                    objArrCopyOf[i] = objArrCopyOf[i2];
                    i++;
                }
            }
            Arrays.fill(objArrCopyOf, i, this.length, (Object) null);
            Object[] objArr = (!z || i * 4 <= this.length * 3) ? (E[]) objArrCopyOf : (E[]) Arrays.copyOf(objArrCopyOf, IntMath.saturatedAdd(this.length, (this.length / 2) + 1));
            int[] iArr = new int[objArr.length];
            for (int i3 = 0; i3 < this.length; i3++) {
                int iBinarySearch = Arrays.binarySearch(objArr, 0, i, this.elements[i3], this.comparator);
                if (this.counts[i3] >= 0) {
                    iArr[iBinarySearch] = iArr[iBinarySearch] + this.counts[i3];
                } else {
                    iArr[iBinarySearch] = this.counts[i3] ^ (-1);
                }
            }
            this.elements = (E[]) objArr;
            this.counts = iArr;
            this.length = i;
        }

        private void dedupAndCoalesceAndDeleteEmpty() {
            dedupAndCoalesce(false);
            int i = 0;
            for (int i2 = 0; i2 < this.length; i2++) {
                if (this.counts[i2] > 0) {
                    this.elements[i] = this.elements[i2];
                    this.counts[i] = this.counts[i2];
                    i++;
                }
            }
            Arrays.fill(this.elements, i, this.length, (Object) null);
            Arrays.fill(this.counts, i, this.length, 0);
            this.length = i;
        }

        private void maintenance() {
            if (this.length == this.elements.length) {
                dedupAndCoalesce(true);
            } else if (this.forceCopyElements) {
                this.elements = (E[]) Arrays.copyOf(this.elements, this.elements.length);
            }
            this.forceCopyElements = false;
        }

        @Override // com.google.common.collect.ImmutableMultiset.Builder, com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> add(E e) {
            return addCopies((Object) e, 1);
        }

        @Override // com.google.common.collect.ImmutableMultiset.Builder, com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> add(E... eArr) {
            for (E e : eArr) {
                add((Object) e);
            }
            return this;
        }

        @Override // com.google.common.collect.ImmutableMultiset.Builder, com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> addAll(Iterable<? extends E> iterable) {
            if (iterable instanceof Multiset) {
                for (Multiset.Entry<E> entry : ((Multiset) iterable).entrySet()) {
                    addCopies((Object) entry.getElement(), entry.getCount());
                }
            } else {
                Iterator<? extends E> it = iterable.iterator();
                while (it.hasNext()) {
                    add((Object) it.next());
                }
            }
            return this;
        }

        @Override // com.google.common.collect.ImmutableMultiset.Builder, com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> addAll(Iterator<? extends E> it) {
            while (it.hasNext()) {
                add((Object) it.next());
            }
            return this;
        }

        @Override // com.google.common.collect.ImmutableMultiset.Builder
        public Builder<E> addCopies(E e, int i) {
            Preconditions.checkNotNull(e);
            CollectPreconditions.checkNonnegative(i, "occurrences");
            if (i != 0) {
                maintenance();
                this.elements[this.length] = e;
                this.counts[this.length] = i;
                this.length++;
            }
            return this;
        }

        @Override // com.google.common.collect.ImmutableMultiset.Builder, com.google.common.collect.ImmutableCollection.Builder
        public ImmutableSortedMultiset<E> build() {
            dedupAndCoalesceAndDeleteEmpty();
            int i = this.length;
            Comparator<? super E> comparator = this.comparator;
            if (i == 0) {
                return ImmutableSortedMultiset.emptyMultiset(comparator);
            }
            RegularImmutableSortedSet regularImmutableSortedSet = (RegularImmutableSortedSet) ImmutableSortedSet.construct(comparator, this.length, this.elements);
            long[] jArr = new long[this.length + 1];
            for (int i2 = 0; i2 < this.length; i2++) {
                jArr[i2 + 1] = jArr[i2] + ((long) this.counts[i2]);
            }
            this.forceCopyElements = true;
            return new RegularImmutableSortedMultiset(regularImmutableSortedSet, jArr, 0, this.length);
        }

        @Override // com.google.common.collect.ImmutableMultiset.Builder
        public Builder<E> setCount(E e, int i) {
            Preconditions.checkNotNull(e);
            CollectPreconditions.checkNonnegative(i, "count");
            maintenance();
            this.elements[this.length] = e;
            this.counts[this.length] = i ^ (-1);
            this.length++;
            return this;
        }
    }

    private static final class SerializedForm<E> implements Serializable {
        final Comparator<? super E> comparator;
        final int[] counts;
        final E[] elements;

        SerializedForm(SortedMultiset<E> sortedMultiset) {
            this.comparator = sortedMultiset.comparator();
            int size = sortedMultiset.entrySet().size();
            this.elements = (E[]) new Object[size];
            this.counts = new int[size];
            int i = 0;
            Iterator<Multiset.Entry<E>> it = sortedMultiset.entrySet().iterator();
            while (true) {
                int i2 = i;
                if (!it.hasNext()) {
                    return;
                }
                Multiset.Entry<E> next = it.next();
                this.elements[i2] = next.getElement();
                this.counts[i2] = next.getCount();
                i = i2 + 1;
            }
        }

        Object readResolve() {
            int length = this.elements.length;
            Builder builder = new Builder(this.comparator);
            for (int i = 0; i < length; i++) {
                builder.addCopies((Object) this.elements[i], this.counts[i]);
            }
            return builder.build();
        }
    }

    ImmutableSortedMultiset() {
    }

    @Deprecated
    public static <E> Builder<E> builder() {
        throw new UnsupportedOperationException();
    }

    public static <E> ImmutableSortedMultiset<E> copyOf(Iterable<? extends E> iterable) {
        return copyOf(Ordering.natural(), iterable);
    }

    public static <E> ImmutableSortedMultiset<E> copyOf(Comparator<? super E> comparator, Iterable<? extends E> iterable) {
        if (iterable instanceof ImmutableSortedMultiset) {
            ImmutableSortedMultiset<E> immutableSortedMultiset = (ImmutableSortedMultiset) iterable;
            if (comparator.equals(immutableSortedMultiset.comparator())) {
                return immutableSortedMultiset.isPartialView() ? copyOfSortedEntries(comparator, immutableSortedMultiset.entrySet().asList()) : immutableSortedMultiset;
            }
        }
        return new Builder(comparator).addAll((Iterable) iterable).build();
    }

    public static <E> ImmutableSortedMultiset<E> copyOf(Comparator<? super E> comparator, Iterator<? extends E> it) {
        Preconditions.checkNotNull(comparator);
        return new Builder(comparator).addAll((Iterator) it).build();
    }

    public static <E> ImmutableSortedMultiset<E> copyOf(Iterator<? extends E> it) {
        return copyOf(Ordering.natural(), it);
    }

    /* JADX WARN: Incorrect types in method signature: <E::Ljava/lang/Comparable<-TE;>;>([TE;)Lcom/google/common/collect/ImmutableSortedMultiset<TE;>; */
    public static ImmutableSortedMultiset copyOf(Comparable[] comparableArr) {
        return copyOf(Ordering.natural(), Arrays.asList(comparableArr));
    }

    @Deprecated
    public static <Z> ImmutableSortedMultiset<Z> copyOf(Z[] zArr) {
        throw new UnsupportedOperationException();
    }

    public static <E> ImmutableSortedMultiset<E> copyOfSorted(SortedMultiset<E> sortedMultiset) {
        return copyOfSortedEntries(sortedMultiset.comparator(), Lists.newArrayList(sortedMultiset.entrySet()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <E> ImmutableSortedMultiset<E> copyOfSortedEntries(Comparator<? super E> comparator, Collection<Multiset.Entry<E>> collection) {
        if (collection.isEmpty()) {
            return emptyMultiset(comparator);
        }
        ImmutableList.Builder builder = new ImmutableList.Builder(collection.size());
        long[] jArr = new long[collection.size() + 1];
        int i = 0;
        for (Multiset.Entry<E> entry : collection) {
            builder.add((Object) entry.getElement());
            jArr[i + 1] = jArr[i] + ((long) entry.getCount());
            i++;
        }
        return new RegularImmutableSortedMultiset(new RegularImmutableSortedSet(builder.build(), comparator), jArr, 0, collection.size());
    }

    static <E> ImmutableSortedMultiset<E> emptyMultiset(Comparator<? super E> comparator) {
        return Ordering.natural().equals(comparator) ? (ImmutableSortedMultiset<E>) RegularImmutableSortedMultiset.NATURAL_EMPTY_MULTISET : new RegularImmutableSortedMultiset(comparator);
    }

    static /* synthetic */ int lambda$toImmutableSortedMultiset$0(Object obj) {
        return 1;
    }

    static /* synthetic */ Multiset lambda$toImmutableSortedMultiset$3(Multiset multiset, Multiset multiset2) {
        multiset.addAll(multiset2);
        return multiset;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    public static <T, E> void mapAndAdd(T t, Multiset<E> multiset, Function<? super T, ? extends E> function, ToIntFunction<? super T> toIntFunction) {
        multiset.add(Preconditions.checkNotNull(function.apply(t)), toIntFunction.applyAsInt(t));
    }

    public static <E extends Comparable<?>> Builder<E> naturalOrder() {
        return new Builder<>(Ordering.natural());
    }

    public static <E> ImmutableSortedMultiset<E> of() {
        return (ImmutableSortedMultiset<E>) RegularImmutableSortedMultiset.NATURAL_EMPTY_MULTISET;
    }

    /* JADX WARN: Incorrect types in method signature: <E::Ljava/lang/Comparable<-TE;>;>(TE;)Lcom/google/common/collect/ImmutableSortedMultiset<TE;>; */
    public static ImmutableSortedMultiset of(Comparable comparable) {
        return new RegularImmutableSortedMultiset((RegularImmutableSortedSet) ImmutableSortedSet.of(comparable), new long[]{0, 1}, 0, 1);
    }

    /* JADX WARN: Incorrect types in method signature: <E::Ljava/lang/Comparable<-TE;>;>(TE;TE;)Lcom/google/common/collect/ImmutableSortedMultiset<TE;>; */
    public static ImmutableSortedMultiset of(Comparable comparable, Comparable comparable2) {
        return copyOf(Ordering.natural(), Arrays.asList(comparable, comparable2));
    }

    /* JADX WARN: Incorrect types in method signature: <E::Ljava/lang/Comparable<-TE;>;>(TE;TE;TE;)Lcom/google/common/collect/ImmutableSortedMultiset<TE;>; */
    public static ImmutableSortedMultiset of(Comparable comparable, Comparable comparable2, Comparable comparable3) {
        return copyOf(Ordering.natural(), Arrays.asList(comparable, comparable2, comparable3));
    }

    /* JADX WARN: Incorrect types in method signature: <E::Ljava/lang/Comparable<-TE;>;>(TE;TE;TE;TE;)Lcom/google/common/collect/ImmutableSortedMultiset<TE;>; */
    public static ImmutableSortedMultiset of(Comparable comparable, Comparable comparable2, Comparable comparable3, Comparable comparable4) {
        return copyOf(Ordering.natural(), Arrays.asList(comparable, comparable2, comparable3, comparable4));
    }

    /* JADX WARN: Incorrect types in method signature: <E::Ljava/lang/Comparable<-TE;>;>(TE;TE;TE;TE;TE;)Lcom/google/common/collect/ImmutableSortedMultiset<TE;>; */
    public static ImmutableSortedMultiset of(Comparable comparable, Comparable comparable2, Comparable comparable3, Comparable comparable4, Comparable comparable5) {
        return copyOf(Ordering.natural(), Arrays.asList(comparable, comparable2, comparable3, comparable4, comparable5));
    }

    /* JADX WARN: Incorrect types in method signature: <E::Ljava/lang/Comparable<-TE;>;>(TE;TE;TE;TE;TE;TE;[TE;)Lcom/google/common/collect/ImmutableSortedMultiset<TE;>; */
    public static ImmutableSortedMultiset of(Comparable comparable, Comparable comparable2, Comparable comparable3, Comparable comparable4, Comparable comparable5, Comparable comparable6, Comparable... comparableArr) {
        ArrayList arrayListNewArrayListWithCapacity = Lists.newArrayListWithCapacity(comparableArr.length + 6);
        Collections.addAll(arrayListNewArrayListWithCapacity, comparable, comparable2, comparable3, comparable4, comparable5, comparable6);
        Collections.addAll(arrayListNewArrayListWithCapacity, comparableArr);
        return copyOf(Ordering.natural(), arrayListNewArrayListWithCapacity);
    }

    @Deprecated
    public static <E> ImmutableSortedMultiset<E> of(E e) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static <E> ImmutableSortedMultiset<E> of(E e, E e2) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static <E> ImmutableSortedMultiset<E> of(E e, E e2, E e3) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static <E> ImmutableSortedMultiset<E> of(E e, E e2, E e3, E e4) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static <E> ImmutableSortedMultiset<E> of(E e, E e2, E e3, E e4, E e5) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static <E> ImmutableSortedMultiset<E> of(E e, E e2, E e3, E e4, E e5, E e6, E... eArr) {
        throw new UnsupportedOperationException();
    }

    public static <E> Builder<E> orderedBy(Comparator<E> comparator) {
        return new Builder<>(comparator);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    public static <E extends Comparable<?>> Builder<E> reverseOrder() {
        return new Builder<>(Ordering.natural().reverse());
    }

    @Deprecated
    static <E> Collector<E, ?, ImmutableMultiset<E>> toImmutableMultiset() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    static <T, E> Collector<T, ?, ImmutableMultiset<E>> toImmutableMultiset(Function<? super T, ? extends E> function, ToIntFunction<? super T> toIntFunction) {
        throw new UnsupportedOperationException();
    }

    static <E> Collector<E, ?, ImmutableSortedMultiset<E>> toImmutableSortedMultiset(Comparator<? super E> comparator) {
        return toImmutableSortedMultiset(comparator, Function.identity(), new ToIntFunction() { // from class: com.google.common.collect.ImmutableSortedMultiset$$ExternalSyntheticLambda4
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                return ImmutableSortedMultiset.lambda$toImmutableSortedMultiset$0(obj);
            }
        });
    }

    static <T, E> Collector<T, ?, ImmutableSortedMultiset<E>> toImmutableSortedMultiset(final Comparator<? super E> comparator, final Function<? super T, ? extends E> function, final ToIntFunction<? super T> toIntFunction) {
        Preconditions.checkNotNull(comparator);
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(toIntFunction);
        return Collector.of(new Supplier(comparator) { // from class: com.google.common.collect.ImmutableSortedMultiset$$ExternalSyntheticLambda0
            public final Comparator f$0;

            {
                this.f$0 = comparator;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return TreeMultiset.create(this.f$0);
            }
        }, new BiConsumer(function, toIntFunction) { // from class: com.google.common.collect.ImmutableSortedMultiset$$ExternalSyntheticLambda1
            public final Function f$0;
            public final ToIntFunction f$1;

            {
                this.f$0 = function;
                this.f$1 = toIntFunction;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ImmutableSortedMultiset.mapAndAdd(obj2, (Multiset) obj, this.f$0, this.f$1);
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.ImmutableSortedMultiset$$ExternalSyntheticLambda2
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ImmutableSortedMultiset.lambda$toImmutableSortedMultiset$3((Multiset) obj, (Multiset) obj2);
            }
        }, new Function(comparator) { // from class: com.google.common.collect.ImmutableSortedMultiset$$ExternalSyntheticLambda3
            public final Comparator f$0;

            {
                this.f$0 = comparator;
            }

            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ImmutableSortedMultiset.copyOfSortedEntries(this.f$0, ((Multiset) obj).entrySet());
            }
        }, new Collector.Characteristics[0]);
    }

    @Override // com.google.common.collect.SortedMultiset, com.google.common.collect.SortedIterable
    public final Comparator<? super E> comparator() {
        return elementSet().comparator();
    }

    @Override // com.google.common.collect.SortedMultiset
    public ImmutableSortedMultiset<E> descendingMultiset() {
        ImmutableSortedMultiset<E> immutableSortedMultisetEmptyMultiset = this.descendingMultiset;
        if (immutableSortedMultisetEmptyMultiset == null) {
            immutableSortedMultisetEmptyMultiset = isEmpty() ? emptyMultiset(Ordering.from(comparator()).reverse()) : new DescendingImmutableSortedMultiset<>(this);
            this.descendingMultiset = immutableSortedMultisetEmptyMultiset;
        }
        return immutableSortedMultisetEmptyMultiset;
    }

    @Override // com.google.common.collect.ImmutableMultiset, com.google.common.collect.Multiset
    public abstract ImmutableSortedSet<E> elementSet();

    public abstract ImmutableSortedMultiset<E> headMultiset(E e, BoundType boundType);

    @Override // com.google.common.collect.SortedMultiset
    @CheckForNull
    @Deprecated
    public final Multiset.Entry<E> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    @Override // com.google.common.collect.SortedMultiset
    @CheckForNull
    @Deprecated
    public final Multiset.Entry<E> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.SortedMultiset
    public ImmutableSortedMultiset<E> subMultiset(E e, BoundType boundType, E e2, BoundType boundType2) {
        Preconditions.checkArgument(comparator().compare(e, e2) <= 0, "Expected lowerBound <= upperBound but %s > %s", e, e2);
        return tailMultiset((Object) e, boundType).headMultiset((Object) e2, boundType2);
    }

    public abstract ImmutableSortedMultiset<E> tailMultiset(E e, BoundType boundType);

    @Override // com.google.common.collect.ImmutableMultiset, com.google.common.collect.ImmutableCollection
    Object writeReplace() {
        return new SerializedForm(this);
    }
}
