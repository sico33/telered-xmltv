package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Iterables {

    private static final class UnmodifiableIterable<T> extends FluentIterable<T> {
        private final Iterable<? extends T> iterable;

        private UnmodifiableIterable(Iterable<? extends T> iterable) {
            this.iterable = iterable;
        }

        @Override // java.lang.Iterable
        public Iterator<T> iterator() {
            return Iterators.unmodifiableIterator(this.iterable.iterator());
        }

        @Override // com.google.common.collect.FluentIterable
        public String toString() {
            return this.iterable.toString();
        }
    }

    private Iterables() {
    }

    public static <T> boolean addAll(Collection<T> collection, Iterable<? extends T> iterable) {
        return iterable instanceof Collection ? collection.addAll((Collection) iterable) : Iterators.addAll(collection, ((Iterable) Preconditions.checkNotNull(iterable)).iterator());
    }

    public static <T> boolean all(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.all(iterable.iterator(), predicate);
    }

    public static <T> boolean any(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.any(iterable.iterator(), predicate);
    }

    private static <E> Collection<E> castOrCopyToCollection(Iterable<E> iterable) {
        return iterable instanceof Collection ? (Collection) iterable : Lists.newArrayList(iterable.iterator());
    }

    public static <T> Iterable<T> concat(Iterable<? extends Iterable<? extends T>> iterable) {
        return FluentIterable.concat(iterable);
    }

    public static <T> Iterable<T> concat(Iterable<? extends T> iterable, Iterable<? extends T> iterable2) {
        return FluentIterable.concat(iterable, iterable2);
    }

    public static <T> Iterable<T> concat(Iterable<? extends T> iterable, Iterable<? extends T> iterable2, Iterable<? extends T> iterable3) {
        return FluentIterable.concat(iterable, iterable2, iterable3);
    }

    public static <T> Iterable<T> concat(Iterable<? extends T> iterable, Iterable<? extends T> iterable2, Iterable<? extends T> iterable3, Iterable<? extends T> iterable4) {
        return FluentIterable.concat(iterable, iterable2, iterable3, iterable4);
    }

    @SafeVarargs
    public static <T> Iterable<T> concat(Iterable<? extends T>... iterableArr) {
        return FluentIterable.concat(iterableArr);
    }

    public static <T> Iterable<T> consumingIterable(Iterable<T> iterable) {
        Preconditions.checkNotNull(iterable);
        return new FluentIterable<T>(iterable) { // from class: com.google.common.collect.Iterables.8
            final Iterable val$iterable;

            {
                this.val$iterable = iterable;
            }

            @Override // java.lang.Iterable
            public Iterator<T> iterator() {
                return this.val$iterable instanceof Queue ? new ConsumingQueueIterator((Queue) this.val$iterable) : Iterators.consumingIterator(this.val$iterable.iterator());
            }

            @Override // com.google.common.collect.FluentIterable
            public String toString() {
                return "Iterables.consumingIterable(...)";
            }
        };
    }

    public static boolean contains(Iterable<? extends Object> iterable, @CheckForNull Object obj) {
        return iterable instanceof Collection ? Collections2.safeContains((Collection) iterable, obj) : Iterators.contains(iterable.iterator(), obj);
    }

    public static <T> Iterable<T> cycle(Iterable<T> iterable) {
        Preconditions.checkNotNull(iterable);
        return new FluentIterable<T>(iterable) { // from class: com.google.common.collect.Iterables.1
            final Iterable val$iterable;

            {
                this.val$iterable = iterable;
            }

            @Override // java.lang.Iterable
            public Iterator<T> iterator() {
                return Iterators.cycle(this.val$iterable);
            }

            @Override // com.google.common.collect.FluentIterable
            public String toString() {
                return this.val$iterable.toString() + " (cycled)";
            }
        };
    }

    @SafeVarargs
    public static <T> Iterable<T> cycle(T... tArr) {
        return cycle(Lists.newArrayList(tArr));
    }

    public static boolean elementsEqual(Iterable<?> iterable, Iterable<?> iterable2) {
        if ((iterable instanceof Collection) && (iterable2 instanceof Collection) && ((Collection) iterable).size() != ((Collection) iterable2).size()) {
            return false;
        }
        return Iterators.elementsEqual(iterable.iterator(), iterable2.iterator());
    }

    public static <T> Iterable<T> filter(Iterable<T> iterable, Predicate<? super T> predicate) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkNotNull(predicate);
        return new FluentIterable<T>(iterable, predicate) { // from class: com.google.common.collect.Iterables.4
            final Predicate val$retainIfTrue;
            final Iterable val$unfiltered;

            {
                this.val$unfiltered = iterable;
                this.val$retainIfTrue = predicate;
            }

            @Override // java.lang.Iterable
            public Iterator<T> iterator() {
                return Iterators.filter(this.val$unfiltered.iterator(), this.val$retainIfTrue);
            }
        };
    }

    public static <T> Iterable<T> filter(Iterable<?> iterable, Class<T> cls) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkNotNull(cls);
        return filter(iterable, Predicates.instanceOf(cls));
    }

    @ParametricNullness
    public static <T> T find(Iterable<T> iterable, Predicate<? super T> predicate) {
        return (T) Iterators.find(iterable.iterator(), predicate);
    }

    @CheckForNull
    public static <T> T find(Iterable<? extends T> iterable, Predicate<? super T> predicate, @CheckForNull T t) {
        return (T) Iterators.find(iterable.iterator(), predicate, t);
    }

    public static int frequency(Iterable<?> iterable, @CheckForNull Object obj) {
        if (iterable instanceof Multiset) {
            return ((Multiset) iterable).count(obj);
        }
        return iterable instanceof Set ? ((Set) iterable).contains(obj) ? 1 : 0 : Iterators.frequency(iterable.iterator(), obj);
    }

    @ParametricNullness
    public static <T> T get(Iterable<T> iterable, int i) {
        Preconditions.checkNotNull(iterable);
        return iterable instanceof List ? (T) ((List) iterable).get(i) : (T) Iterators.get(iterable.iterator(), i);
    }

    @ParametricNullness
    public static <T> T get(Iterable<? extends T> iterable, int i, @ParametricNullness T t) {
        Preconditions.checkNotNull(iterable);
        Iterators.checkNonnegative(i);
        if (iterable instanceof List) {
            List listCast = Lists.cast(iterable);
            return i < listCast.size() ? (T) listCast.get(i) : t;
        }
        Iterator<? extends T> it = iterable.iterator();
        Iterators.advance(it, i);
        return (T) Iterators.getNext(it, t);
    }

    @ParametricNullness
    public static <T> T getFirst(Iterable<? extends T> iterable, @ParametricNullness T t) {
        return (T) Iterators.getNext(iterable.iterator(), t);
    }

    @ParametricNullness
    public static <T> T getLast(Iterable<T> iterable) {
        if (!(iterable instanceof List)) {
            return (T) Iterators.getLast(iterable.iterator());
        }
        List list = (List) iterable;
        if (list.isEmpty()) {
            throw new NoSuchElementException();
        }
        return (T) getLastInNonemptyList(list);
    }

    @ParametricNullness
    public static <T> T getLast(Iterable<? extends T> iterable, @ParametricNullness T t) {
        if (iterable instanceof Collection) {
            if (((Collection) iterable).isEmpty()) {
                return t;
            }
            if (iterable instanceof List) {
                return (T) getLastInNonemptyList(Lists.cast(iterable));
            }
        }
        return (T) Iterators.getLast(iterable.iterator(), t);
    }

    @ParametricNullness
    private static <T> T getLastInNonemptyList(List<T> list) {
        return list.get(list.size() - 1);
    }

    @ParametricNullness
    public static <T> T getOnlyElement(Iterable<T> iterable) {
        return (T) Iterators.getOnlyElement(iterable.iterator());
    }

    @ParametricNullness
    public static <T> T getOnlyElement(Iterable<? extends T> iterable, @ParametricNullness T t) {
        return (T) Iterators.getOnlyElement(iterable.iterator(), t);
    }

    public static <T> int indexOf(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.indexOf(iterable.iterator(), predicate);
    }

    public static boolean isEmpty(Iterable<?> iterable) {
        return iterable instanceof Collection ? ((Collection) iterable).isEmpty() : !iterable.iterator().hasNext();
    }

    public static <T> Iterable<T> limit(Iterable<T> iterable, int i) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkArgument(i >= 0, "limit is negative");
        return new FluentIterable<T>(iterable, i) { // from class: com.google.common.collect.Iterables.7
            final Iterable val$iterable;
            final int val$limitSize;

            {
                this.val$iterable = iterable;
                this.val$limitSize = i;
            }

            @Override // java.lang.Iterable
            public Iterator<T> iterator() {
                return Iterators.limit(this.val$iterable.iterator(), this.val$limitSize);
            }
        };
    }

    public static <T> Iterable<T> mergeSorted(Iterable<? extends Iterable<? extends T>> iterable, Comparator<? super T> comparator) {
        Preconditions.checkNotNull(iterable, "iterables");
        Preconditions.checkNotNull(comparator, "comparator");
        return new UnmodifiableIterable(new FluentIterable<T>(iterable, comparator) { // from class: com.google.common.collect.Iterables.9
            final Comparator val$comparator;
            final Iterable val$iterables;

            {
                this.val$iterables = iterable;
                this.val$comparator = comparator;
            }

            @Override // java.lang.Iterable
            public Iterator<T> iterator() {
                return Iterators.mergeSorted(Iterables.transform(this.val$iterables, new Function() { // from class: com.google.common.collect.Iterables$9$$ExternalSyntheticLambda0
                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return ((Iterable) obj).iterator();
                    }
                }), this.val$comparator);
            }
        });
    }

    public static <T> Iterable<List<T>> paddedPartition(Iterable<T> iterable, int i) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkArgument(i > 0);
        return new FluentIterable<List<T>>(iterable, i) { // from class: com.google.common.collect.Iterables.3
            final Iterable val$iterable;
            final int val$size;

            {
                this.val$iterable = iterable;
                this.val$size = i;
            }

            @Override // java.lang.Iterable
            public Iterator<List<T>> iterator() {
                return Iterators.paddedPartition(this.val$iterable.iterator(), this.val$size);
            }
        };
    }

    public static <T> Iterable<List<T>> partition(Iterable<T> iterable, int i) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkArgument(i > 0);
        return new FluentIterable<List<T>>(iterable, i) { // from class: com.google.common.collect.Iterables.2
            final Iterable val$iterable;
            final int val$size;

            {
                this.val$iterable = iterable;
                this.val$size = i;
            }

            @Override // java.lang.Iterable
            public Iterator<List<T>> iterator() {
                return Iterators.partition(this.val$iterable.iterator(), this.val$size);
            }
        };
    }

    public static boolean removeAll(Iterable<?> iterable, Collection<?> collection) {
        return iterable instanceof Collection ? ((Collection) iterable).removeAll((Collection) Preconditions.checkNotNull(collection)) : Iterators.removeAll(iterable.iterator(), collection);
    }

    @CheckForNull
    static <T> T removeFirstMatching(Iterable<T> iterable, Predicate<? super T> predicate) {
        Preconditions.checkNotNull(predicate);
        Iterator<T> it = iterable.iterator();
        while (it.hasNext()) {
            T next = it.next();
            if (predicate.apply(next)) {
                it.remove();
                return next;
            }
        }
        return null;
    }

    public static <T> boolean removeIf(Iterable<T> iterable, Predicate<? super T> predicate) {
        return ((iterable instanceof RandomAccess) && (iterable instanceof List)) ? removeIfFromRandomAccessList((List) iterable, (Predicate) Preconditions.checkNotNull(predicate)) : Iterators.removeIf(iterable.iterator(), predicate);
    }

    private static <T> boolean removeIfFromRandomAccessList(List<T> list, Predicate<? super T> predicate) {
        int i = 0;
        int i2 = 0;
        while (i2 < list.size()) {
            T t = list.get(i2);
            if (!predicate.apply(t)) {
                if (i2 > i) {
                    try {
                        list.set(i, t);
                    } catch (IllegalArgumentException e) {
                        slowRemoveIfForRemainingElements(list, predicate, i, i2);
                        return true;
                    } catch (UnsupportedOperationException e2) {
                        slowRemoveIfForRemainingElements(list, predicate, i, i2);
                        return true;
                    }
                }
                i++;
            }
            i2++;
        }
        list.subList(i, list.size()).clear();
        return i2 != i;
    }

    public static boolean retainAll(Iterable<?> iterable, Collection<?> collection) {
        return iterable instanceof Collection ? ((Collection) iterable).retainAll((Collection) Preconditions.checkNotNull(collection)) : Iterators.retainAll(iterable.iterator(), collection);
    }

    public static int size(Iterable<?> iterable) {
        return iterable instanceof Collection ? ((Collection) iterable).size() : Iterators.size(iterable.iterator());
    }

    public static <T> Iterable<T> skip(Iterable<T> iterable, int i) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkArgument(i >= 0, "number to skip cannot be negative");
        return new FluentIterable<T>(iterable, i) { // from class: com.google.common.collect.Iterables.6
            final Iterable val$iterable;
            final int val$numberToSkip;

            {
                this.val$iterable = iterable;
                this.val$numberToSkip = i;
            }

            @Override // java.lang.Iterable
            public Iterator<T> iterator() {
                boolean z = this.val$iterable instanceof List;
                Iterable iterable2 = this.val$iterable;
                if (z) {
                    List list = (List) iterable2;
                    return list.subList(Math.min(list.size(), this.val$numberToSkip), list.size()).iterator();
                }
                Iterator<T> it = iterable2.iterator();
                Iterators.advance(it, this.val$numberToSkip);
                return new Iterator<T>(this, it) { // from class: com.google.common.collect.Iterables.6.1
                    boolean atStart = true;
                    final Iterator val$iterator;

                    {
                        this.val$iterator = it;
                    }

                    @Override // java.util.Iterator
                    public boolean hasNext() {
                        return this.val$iterator.hasNext();
                    }

                    @Override // java.util.Iterator
                    @ParametricNullness
                    public T next() {
                        T t = (T) this.val$iterator.next();
                        this.atStart = false;
                        return t;
                    }

                    @Override // java.util.Iterator
                    public void remove() {
                        CollectPreconditions.checkRemove(!this.atStart);
                        this.val$iterator.remove();
                    }
                };
            }
        };
    }

    private static <T> void slowRemoveIfForRemainingElements(List<T> list, Predicate<? super T> predicate, int i, int i2) {
        for (int size = list.size() - 1; size > i2; size--) {
            if (predicate.apply(list.get(size))) {
                list.remove(size);
            }
        }
        for (int i3 = i2 - 1; i3 >= i; i3--) {
            list.remove(i3);
        }
    }

    static Object[] toArray(Iterable<?> iterable) {
        return castOrCopyToCollection(iterable).toArray();
    }

    public static <T> T[] toArray(Iterable<? extends T> iterable, Class<T> cls) {
        return (T[]) toArray(iterable, ObjectArrays.newArray(cls, 0));
    }

    static <T> T[] toArray(Iterable<? extends T> iterable, T[] tArr) {
        return (T[]) castOrCopyToCollection(iterable).toArray(tArr);
    }

    public static String toString(Iterable<?> iterable) {
        return Iterators.toString(iterable.iterator());
    }

    public static <F, T> Iterable<T> transform(Iterable<F> iterable, Function<? super F, ? extends T> function) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkNotNull(function);
        return new FluentIterable<T>(iterable, function) { // from class: com.google.common.collect.Iterables.5
            final Iterable val$fromIterable;
            final Function val$function;

            {
                this.val$fromIterable = iterable;
                this.val$function = function;
            }

            @Override // java.lang.Iterable
            public Iterator<T> iterator() {
                return Iterators.transform(this.val$fromIterable.iterator(), this.val$function);
            }
        };
    }

    public static <T> Optional<T> tryFind(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.tryFind(iterable.iterator(), predicate);
    }

    @Deprecated
    public static <E> Iterable<E> unmodifiableIterable(ImmutableCollection<E> immutableCollection) {
        return (Iterable) Preconditions.checkNotNull(immutableCollection);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static <T> Iterable<T> unmodifiableIterable(Iterable<? extends T> iterable) {
        Preconditions.checkNotNull(iterable);
        return ((iterable instanceof UnmodifiableIterable) || (iterable instanceof ImmutableCollection)) ? iterable : new UnmodifiableIterable(iterable);
    }
}
