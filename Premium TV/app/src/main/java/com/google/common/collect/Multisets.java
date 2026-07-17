package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Multisets {

    /* JADX INFO: Add missing generic type declarations: [E] */
    /* JADX INFO: renamed from: com.google.common.collect.Multisets$1, reason: invalid class name */
    class AnonymousClass1<E> extends ViewMultiset<E> {
        final Multiset val$multiset1;
        final Multiset val$multiset2;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass1(Multiset multiset, Multiset multiset2) {
            super(null);
            this.val$multiset1 = multiset;
            this.val$multiset2 = multiset2;
        }

        @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
        public boolean contains(@CheckForNull Object obj) {
            return this.val$multiset1.contains(obj) || this.val$multiset2.contains(obj);
        }

        @Override // com.google.common.collect.Multiset
        public int count(@CheckForNull Object obj) {
            return Math.max(this.val$multiset1.count(obj), this.val$multiset2.count(obj));
        }

        @Override // com.google.common.collect.AbstractMultiset
        Set<E> createElementSet() {
            return Sets.union(this.val$multiset1.elementSet(), this.val$multiset2.elementSet());
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<E> elementIterator() {
            throw new AssertionError("should never be called");
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<Multiset.Entry<E>> entryIterator() {
            return new AbstractIterator<Multiset.Entry<E>>(this, this.val$multiset1.entrySet().iterator(), this.val$multiset2.entrySet().iterator()) { // from class: com.google.common.collect.Multisets.1.1
                final AnonymousClass1 this$0;
                final Iterator val$iterator1;
                final Iterator val$iterator2;

                {
                    this.this$0 = this;
                    this.val$iterator1 = it;
                    this.val$iterator2 = it;
                }

                /* JADX INFO: Access modifiers changed from: protected */
                @Override // com.google.common.collect.AbstractIterator
                @CheckForNull
                public Multiset.Entry<E> computeNext() {
                    if (this.val$iterator1.hasNext()) {
                        Multiset.Entry entry = (Multiset.Entry) this.val$iterator1.next();
                        Object element = entry.getElement();
                        return Multisets.immutableEntry(element, Math.max(entry.getCount(), this.this$0.val$multiset2.count(element)));
                    }
                    while (this.val$iterator2.hasNext()) {
                        Multiset.Entry entry2 = (Multiset.Entry) this.val$iterator2.next();
                        Object element2 = entry2.getElement();
                        if (!this.this$0.val$multiset1.contains(element2)) {
                            return Multisets.immutableEntry(element2, entry2.getCount());
                        }
                    }
                    return endOfData();
                }
            };
        }

        @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
        public boolean isEmpty() {
            return this.val$multiset1.isEmpty() && this.val$multiset2.isEmpty();
        }
    }

    /* JADX INFO: Add missing generic type declarations: [E] */
    /* JADX INFO: renamed from: com.google.common.collect.Multisets$2, reason: invalid class name */
    class AnonymousClass2<E> extends ViewMultiset<E> {
        final Multiset val$multiset1;
        final Multiset val$multiset2;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass2(Multiset multiset, Multiset multiset2) {
            super(null);
            this.val$multiset1 = multiset;
            this.val$multiset2 = multiset2;
        }

        @Override // com.google.common.collect.Multiset
        public int count(@CheckForNull Object obj) {
            int iCount = this.val$multiset1.count(obj);
            if (iCount == 0) {
                return 0;
            }
            return Math.min(iCount, this.val$multiset2.count(obj));
        }

        @Override // com.google.common.collect.AbstractMultiset
        Set<E> createElementSet() {
            return Sets.intersection(this.val$multiset1.elementSet(), this.val$multiset2.elementSet());
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<E> elementIterator() {
            throw new AssertionError("should never be called");
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<Multiset.Entry<E>> entryIterator() {
            return new AbstractIterator<Multiset.Entry<E>>(this, this.val$multiset1.entrySet().iterator()) { // from class: com.google.common.collect.Multisets.2.1
                final AnonymousClass2 this$0;
                final Iterator val$iterator1;

                {
                    this.this$0 = this;
                    this.val$iterator1 = it;
                }

                /* JADX INFO: Access modifiers changed from: protected */
                @Override // com.google.common.collect.AbstractIterator
                @CheckForNull
                public Multiset.Entry<E> computeNext() {
                    while (this.val$iterator1.hasNext()) {
                        Multiset.Entry entry = (Multiset.Entry) this.val$iterator1.next();
                        Object element = entry.getElement();
                        int iMin = Math.min(entry.getCount(), this.this$0.val$multiset2.count(element));
                        if (iMin > 0) {
                            return Multisets.immutableEntry(element, iMin);
                        }
                    }
                    return endOfData();
                }
            };
        }
    }

    /* JADX INFO: Add missing generic type declarations: [E] */
    /* JADX INFO: renamed from: com.google.common.collect.Multisets$3, reason: invalid class name */
    class AnonymousClass3<E> extends ViewMultiset<E> {
        final Multiset val$multiset1;
        final Multiset val$multiset2;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass3(Multiset multiset, Multiset multiset2) {
            super(null);
            this.val$multiset1 = multiset;
            this.val$multiset2 = multiset2;
        }

        @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
        public boolean contains(@CheckForNull Object obj) {
            return this.val$multiset1.contains(obj) || this.val$multiset2.contains(obj);
        }

        @Override // com.google.common.collect.Multiset
        public int count(@CheckForNull Object obj) {
            return this.val$multiset1.count(obj) + this.val$multiset2.count(obj);
        }

        @Override // com.google.common.collect.AbstractMultiset
        Set<E> createElementSet() {
            return Sets.union(this.val$multiset1.elementSet(), this.val$multiset2.elementSet());
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<E> elementIterator() {
            throw new AssertionError("should never be called");
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<Multiset.Entry<E>> entryIterator() {
            return new AbstractIterator<Multiset.Entry<E>>(this, this.val$multiset1.entrySet().iterator(), this.val$multiset2.entrySet().iterator()) { // from class: com.google.common.collect.Multisets.3.1
                final AnonymousClass3 this$0;
                final Iterator val$iterator1;
                final Iterator val$iterator2;

                {
                    this.this$0 = this;
                    this.val$iterator1 = it;
                    this.val$iterator2 = it;
                }

                /* JADX INFO: Access modifiers changed from: protected */
                @Override // com.google.common.collect.AbstractIterator
                @CheckForNull
                public Multiset.Entry<E> computeNext() {
                    if (this.val$iterator1.hasNext()) {
                        Multiset.Entry entry = (Multiset.Entry) this.val$iterator1.next();
                        Object element = entry.getElement();
                        return Multisets.immutableEntry(element, entry.getCount() + this.this$0.val$multiset2.count(element));
                    }
                    while (this.val$iterator2.hasNext()) {
                        Multiset.Entry entry2 = (Multiset.Entry) this.val$iterator2.next();
                        Object element2 = entry2.getElement();
                        if (!this.this$0.val$multiset1.contains(element2)) {
                            return Multisets.immutableEntry(element2, entry2.getCount());
                        }
                    }
                    return endOfData();
                }
            };
        }

        @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
        public boolean isEmpty() {
            return this.val$multiset1.isEmpty() && this.val$multiset2.isEmpty();
        }

        @Override // com.google.common.collect.Multisets.ViewMultiset, java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
        public int size() {
            return IntMath.saturatedAdd(this.val$multiset1.size(), this.val$multiset2.size());
        }
    }

    /* JADX INFO: Add missing generic type declarations: [E] */
    /* JADX INFO: renamed from: com.google.common.collect.Multisets$4, reason: invalid class name */
    class AnonymousClass4<E> extends ViewMultiset<E> {
        final Multiset val$multiset1;
        final Multiset val$multiset2;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass4(Multiset multiset, Multiset multiset2) {
            super(null);
            this.val$multiset1 = multiset;
            this.val$multiset2 = multiset2;
        }

        @Override // com.google.common.collect.Multisets.ViewMultiset, com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.Multiset
        public int count(@CheckForNull Object obj) {
            int iCount = this.val$multiset1.count(obj);
            if (iCount == 0) {
                return 0;
            }
            return Math.max(0, iCount - this.val$multiset2.count(obj));
        }

        @Override // com.google.common.collect.Multisets.ViewMultiset, com.google.common.collect.AbstractMultiset
        int distinctElements() {
            return Iterators.size(entryIterator());
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<E> elementIterator() {
            return new AbstractIterator<E>(this, this.val$multiset1.entrySet().iterator()) { // from class: com.google.common.collect.Multisets.4.1
                final AnonymousClass4 this$0;
                final Iterator val$iterator1;

                {
                    this.this$0 = this;
                    this.val$iterator1 = it;
                }

                /* JADX WARN: Type inference fix 'apply assigned field type' failed
                java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
                	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:596)
                	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
                	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
                	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
                 */
                @Override // com.google.common.collect.AbstractIterator
                @CheckForNull
                protected E computeNext() {
                    while (this.val$iterator1.hasNext()) {
                        Multiset.Entry entry = (Multiset.Entry) this.val$iterator1.next();
                        E e = (E) entry.getElement();
                        if (entry.getCount() > this.this$0.val$multiset2.count(e)) {
                            return e;
                        }
                    }
                    return endOfData();
                }
            };
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<Multiset.Entry<E>> entryIterator() {
            return new AbstractIterator<Multiset.Entry<E>>(this, this.val$multiset1.entrySet().iterator()) { // from class: com.google.common.collect.Multisets.4.2
                final AnonymousClass4 this$0;
                final Iterator val$iterator1;

                {
                    this.this$0 = this;
                    this.val$iterator1 = it;
                }

                /* JADX INFO: Access modifiers changed from: protected */
                @Override // com.google.common.collect.AbstractIterator
                @CheckForNull
                public Multiset.Entry<E> computeNext() {
                    while (this.val$iterator1.hasNext()) {
                        Multiset.Entry entry = (Multiset.Entry) this.val$iterator1.next();
                        Object element = entry.getElement();
                        int count = entry.getCount() - this.this$0.val$multiset2.count(element);
                        if (count > 0) {
                            return Multisets.immutableEntry(element, count);
                        }
                    }
                    return endOfData();
                }
            };
        }
    }

    static abstract class AbstractEntry<E> implements Multiset.Entry<E> {
        AbstractEntry() {
        }

        @Override // com.google.common.collect.Multiset.Entry
        public boolean equals(@CheckForNull Object obj) {
            if (!(obj instanceof Multiset.Entry)) {
                return false;
            }
            Multiset.Entry entry = (Multiset.Entry) obj;
            return getCount() == entry.getCount() && Objects.equal(getElement(), entry.getElement());
        }

        @Override // com.google.common.collect.Multiset.Entry
        public int hashCode() {
            E element = getElement();
            return (element == null ? 0 : element.hashCode()) ^ getCount();
        }

        @Override // com.google.common.collect.Multiset.Entry
        public String toString() {
            String strValueOf = String.valueOf(getElement());
            int count = getCount();
            return count == 1 ? strValueOf : strValueOf + " x " + count;
        }
    }

    private static final class DecreasingCount implements Comparator<Multiset.Entry<?>> {
        static final Comparator<Multiset.Entry<?>> INSTANCE = new DecreasingCount();

        private DecreasingCount() {
        }

        @Override // java.util.Comparator
        public int compare(Multiset.Entry<?> entry, Multiset.Entry<?> entry2) {
            return entry2.getCount() - entry.getCount();
        }
    }

    static abstract class ElementSet<E> extends Sets.ImprovedAbstractSet<E> {
        ElementSet() {
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            multiset().clear();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            return multiset().contains(obj);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean containsAll(Collection<?> collection) {
            return multiset().containsAll(collection);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean isEmpty() {
            return multiset().isEmpty();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public abstract Iterator<E> iterator();

        abstract Multiset<E> multiset();

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            return multiset().remove(obj, Integer.MAX_VALUE) > 0;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return multiset().entrySet().size();
        }
    }

    static abstract class EntrySet<E> extends Sets.ImprovedAbstractSet<Multiset.Entry<E>> {
        EntrySet() {
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            multiset().clear();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(@CheckForNull Object obj) {
            if (!(obj instanceof Multiset.Entry)) {
                return false;
            }
            Multiset.Entry entry = (Multiset.Entry) obj;
            return entry.getCount() > 0 && multiset().count(entry.getElement()) == entry.getCount();
        }

        abstract Multiset<E> multiset();

        /* JADX WARN: Multi-variable type inference failed */
        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            if (!(obj instanceof Multiset.Entry)) {
                return false;
            }
            Multiset.Entry entry = (Multiset.Entry) obj;
            Object element = entry.getElement();
            int count = entry.getCount();
            if (count != 0) {
                return multiset().setCount(element, count, 0);
            }
            return false;
        }
    }

    private static final class FilteredMultiset<E> extends ViewMultiset<E> {
        final Predicate<? super E> predicate;
        final Multiset<E> unfiltered;

        FilteredMultiset(Multiset<E> multiset, Predicate<? super E> predicate) {
            super(null);
            this.unfiltered = (Multiset) Preconditions.checkNotNull(multiset);
            this.predicate = (Predicate) Preconditions.checkNotNull(predicate);
        }

        @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        public int add(@ParametricNullness E e, int i) {
            Preconditions.checkArgument(this.predicate.apply(e), "Element %s does not match predicate %s", e, this.predicate);
            return this.unfiltered.add(e, i);
        }

        @Override // com.google.common.collect.Multiset
        public int count(@CheckForNull Object obj) {
            int iCount = this.unfiltered.count(obj);
            if (iCount <= 0 || !this.predicate.apply(obj)) {
                return 0;
            }
            return iCount;
        }

        @Override // com.google.common.collect.AbstractMultiset
        Set<E> createElementSet() {
            return Sets.filter(this.unfiltered.elementSet(), this.predicate);
        }

        @Override // com.google.common.collect.AbstractMultiset
        Set<Multiset.Entry<E>> createEntrySet() {
            return Sets.filter(this.unfiltered.entrySet(), new Predicate<Multiset.Entry<E>>(this) { // from class: com.google.common.collect.Multisets.FilteredMultiset.1
                final FilteredMultiset this$0;

                {
                    this.this$0 = this;
                }

                @Override // com.google.common.base.Predicate
                public boolean apply(Multiset.Entry<E> entry) {
                    return this.this$0.predicate.apply(entry.getElement());
                }
            });
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<E> elementIterator() {
            throw new AssertionError("should never be called");
        }

        @Override // com.google.common.collect.AbstractMultiset
        Iterator<Multiset.Entry<E>> entryIterator() {
            throw new AssertionError("should never be called");
        }

        @Override // com.google.common.collect.Multisets.ViewMultiset, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, com.google.common.collect.Multiset
        public UnmodifiableIterator<E> iterator() {
            return Iterators.filter(this.unfiltered.iterator(), this.predicate);
        }

        @Override // com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        public int remove(@CheckForNull Object obj, int i) {
            CollectPreconditions.checkNonnegative(i, "occurrences");
            if (i == 0) {
                return count(obj);
            }
            if (contains(obj)) {
                return this.unfiltered.remove(obj, i);
            }
            return 0;
        }
    }

    static class ImmutableEntry<E> extends AbstractEntry<E> implements Serializable {
        private static final long serialVersionUID = 0;
        private final int count;

        @ParametricNullness
        private final E element;

        ImmutableEntry(@ParametricNullness E e, int i) {
            this.element = e;
            this.count = i;
            CollectPreconditions.checkNonnegative(i, "count");
        }

        @Override // com.google.common.collect.Multiset.Entry
        public final int getCount() {
            return this.count;
        }

        @Override // com.google.common.collect.Multiset.Entry
        @ParametricNullness
        public final E getElement() {
            return this.element;
        }

        @CheckForNull
        public ImmutableEntry<E> nextInBucket() {
            return null;
        }
    }

    static final class MultisetIteratorImpl<E> implements Iterator<E> {
        private boolean canRemove;

        @CheckForNull
        private Multiset.Entry<E> currentEntry;
        private final Iterator<Multiset.Entry<E>> entryIterator;
        private int laterCount;
        private final Multiset<E> multiset;
        private int totalCount;

        MultisetIteratorImpl(Multiset<E> multiset, Iterator<Multiset.Entry<E>> it) {
            this.multiset = multiset;
            this.entryIterator = it;
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return this.laterCount > 0 || this.entryIterator.hasNext();
        }

        @Override // java.util.Iterator
        @ParametricNullness
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (this.laterCount == 0) {
                this.currentEntry = this.entryIterator.next();
                int count = this.currentEntry.getCount();
                this.laterCount = count;
                this.totalCount = count;
            }
            this.laterCount--;
            this.canRemove = true;
            return (E) ((Multiset.Entry) java.util.Objects.requireNonNull(this.currentEntry)).getElement();
        }

        @Override // java.util.Iterator
        public void remove() {
            CollectPreconditions.checkRemove(this.canRemove);
            if (this.totalCount == 1) {
                this.entryIterator.remove();
            } else {
                this.multiset.remove(((Multiset.Entry) java.util.Objects.requireNonNull(this.currentEntry)).getElement());
            }
            this.totalCount--;
            this.canRemove = false;
        }
    }

    static class UnmodifiableMultiset<E> extends ForwardingMultiset<E> implements Serializable {
        private static final long serialVersionUID = 0;
        final Multiset<? extends E> delegate;

        @CheckForNull
        @LazyInit
        transient Set<E> elementSet;

        @CheckForNull
        @LazyInit
        transient Set<Multiset.Entry<E>> entrySet;

        UnmodifiableMultiset(Multiset<? extends E> multiset) {
            this.delegate = multiset;
        }

        @Override // com.google.common.collect.ForwardingMultiset, com.google.common.collect.Multiset
        public int add(@ParametricNullness E e, int i) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Queue
        public boolean add(@ParametricNullness E e) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection
        public boolean addAll(Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public void clear() {
            throw new UnsupportedOperationException();
        }

        Set<E> createElementSet() {
            return Collections.unmodifiableSet(this.delegate.elementSet());
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.collect.ForwardingMultiset, com.google.common.collect.ForwardingCollection, com.google.common.collect.ForwardingObject
        public Multiset<E> delegate() {
            return this.delegate;
        }

        @Override // com.google.common.collect.ForwardingMultiset, com.google.common.collect.Multiset
        public Set<E> elementSet() {
            Set<E> set = this.elementSet;
            if (set != null) {
                return set;
            }
            Set<E> setCreateElementSet = createElementSet();
            this.elementSet = setCreateElementSet;
            return setCreateElementSet;
        }

        @Override // com.google.common.collect.ForwardingMultiset, com.google.common.collect.Multiset
        public Set<Multiset.Entry<E>> entrySet() {
            Set<Multiset.Entry<E>> set = this.entrySet;
            if (set != null) {
                return set;
            }
            Set<Multiset.Entry<E>> setUnmodifiableSet = Collections.unmodifiableSet(this.delegate.entrySet());
            this.entrySet = setUnmodifiableSet;
            return setUnmodifiableSet;
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<E> iterator() {
            return Iterators.unmodifiableIterator(this.delegate.iterator());
        }

        @Override // com.google.common.collect.ForwardingMultiset, com.google.common.collect.Multiset
        public int remove(@CheckForNull Object obj, int i) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean remove(@CheckForNull Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Set
        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingMultiset, com.google.common.collect.Multiset
        public int setCount(@ParametricNullness E e, int i) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.ForwardingMultiset, com.google.common.collect.Multiset
        public boolean setCount(@ParametricNullness E e, int i, int i2) {
            throw new UnsupportedOperationException();
        }
    }

    private static abstract class ViewMultiset<E> extends AbstractMultiset<E> {
        private ViewMultiset() {
        }

        /* synthetic */ ViewMultiset(AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override // com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
        public void clear() {
            elementSet().clear();
        }

        @Override // com.google.common.collect.AbstractMultiset
        int distinctElements() {
            return elementSet().size();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, com.google.common.collect.Multiset
        public Iterator<E> iterator() {
            return Multisets.iteratorImpl(this);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, com.google.common.collect.Multiset
        public int size() {
            return Multisets.linearTimeSizeImpl(this);
        }
    }

    private Multisets() {
    }

    private static <E> boolean addAllImpl(Multiset<E> multiset, AbstractMapBasedMultiset<? extends E> abstractMapBasedMultiset) {
        if (abstractMapBasedMultiset.isEmpty()) {
            return false;
        }
        abstractMapBasedMultiset.addTo(multiset);
        return true;
    }

    private static <E> boolean addAllImpl(Multiset<E> multiset, Multiset<? extends E> multiset2) {
        if (multiset2 instanceof AbstractMapBasedMultiset) {
            return addAllImpl((Multiset) multiset, (AbstractMapBasedMultiset) multiset2);
        }
        if (multiset2.isEmpty()) {
            return false;
        }
        for (Multiset.Entry<? extends E> entry : multiset2.entrySet()) {
            multiset.add(entry.getElement(), entry.getCount());
        }
        return true;
    }

    static <E> boolean addAllImpl(Multiset<E> multiset, Collection<? extends E> collection) {
        Preconditions.checkNotNull(multiset);
        Preconditions.checkNotNull(collection);
        if (collection instanceof Multiset) {
            return addAllImpl((Multiset) multiset, cast(collection));
        }
        if (collection.isEmpty()) {
            return false;
        }
        return Iterators.addAll(multiset, collection.iterator());
    }

    static <T> Multiset<T> cast(Iterable<T> iterable) {
        return (Multiset) iterable;
    }

    public static boolean containsOccurrences(Multiset<?> multiset, Multiset<?> multiset2) {
        Preconditions.checkNotNull(multiset);
        Preconditions.checkNotNull(multiset2);
        for (Multiset.Entry<?> entry : multiset2.entrySet()) {
            if (multiset.count(entry.getElement()) < entry.getCount()) {
                return false;
            }
        }
        return true;
    }

    public static <E> ImmutableMultiset<E> copyHighestCountFirst(Multiset<E> multiset) {
        Multiset.Entry[] entryArr = (Multiset.Entry[]) multiset.entrySet().toArray(new Multiset.Entry[0]);
        Arrays.sort(entryArr, DecreasingCount.INSTANCE);
        return ImmutableMultiset.copyFromEntries(Arrays.asList(entryArr));
    }

    public static <E> Multiset<E> difference(Multiset<E> multiset, Multiset<?> multiset2) {
        Preconditions.checkNotNull(multiset);
        Preconditions.checkNotNull(multiset2);
        return new AnonymousClass4(multiset, multiset2);
    }

    static <E> Iterator<E> elementIterator(Iterator<Multiset.Entry<E>> it) {
        return new TransformedIterator<Multiset.Entry<E>, E>(it) { // from class: com.google.common.collect.Multisets.5
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.google.common.collect.TransformedIterator
            @ParametricNullness
            public E transform(Multiset.Entry<E> entry) {
                return entry.getElement();
            }
        };
    }

    static boolean equalsImpl(Multiset<?> multiset, @CheckForNull Object obj) {
        if (obj == multiset) {
            return true;
        }
        if (!(obj instanceof Multiset)) {
            return false;
        }
        Multiset multiset2 = (Multiset) obj;
        if (multiset.size() != multiset2.size() || multiset.entrySet().size() != multiset2.entrySet().size()) {
            return false;
        }
        for (Multiset.Entry entry : multiset2.entrySet()) {
            if (multiset.count(entry.getElement()) != entry.getCount()) {
                return false;
            }
        }
        return true;
    }

    public static <E> Multiset<E> filter(Multiset<E> multiset, Predicate<? super E> predicate) {
        if (!(multiset instanceof FilteredMultiset)) {
            return new FilteredMultiset(multiset, predicate);
        }
        FilteredMultiset filteredMultiset = (FilteredMultiset) multiset;
        return new FilteredMultiset(filteredMultiset.unfiltered, Predicates.and(filteredMultiset.predicate, predicate));
    }

    public static <E> Multiset.Entry<E> immutableEntry(@ParametricNullness E e, int i) {
        return new ImmutableEntry(e, i);
    }

    static int inferDistinctElements(Iterable<?> iterable) {
        if (iterable instanceof Multiset) {
            return ((Multiset) iterable).elementSet().size();
        }
        return 11;
    }

    public static <E> Multiset<E> intersection(Multiset<E> multiset, Multiset<?> multiset2) {
        Preconditions.checkNotNull(multiset);
        Preconditions.checkNotNull(multiset2);
        return new AnonymousClass2(multiset, multiset2);
    }

    static <E> Iterator<E> iteratorImpl(Multiset<E> multiset) {
        return new MultisetIteratorImpl(multiset, multiset.entrySet().iterator());
    }

    static int linearTimeSizeImpl(Multiset<?> multiset) {
        long count = 0;
        Iterator<Multiset.Entry<?>> it = multiset.entrySet().iterator();
        while (true) {
            long j = count;
            if (!it.hasNext()) {
                return Ints.saturatedCast(j);
            }
            count = ((long) it.next().getCount()) + j;
        }
    }

    static boolean removeAllImpl(Multiset<?> multiset, Collection<?> collection) {
        if (collection instanceof Multiset) {
            collection = ((Multiset) collection).elementSet();
        }
        return multiset.elementSet().removeAll(collection);
    }

    public static boolean removeOccurrences(Multiset<?> multiset, Multiset<?> multiset2) {
        Preconditions.checkNotNull(multiset);
        Preconditions.checkNotNull(multiset2);
        boolean z = false;
        Iterator<Multiset.Entry<?>> it = multiset.entrySet().iterator();
        while (true) {
            boolean z2 = z;
            if (!it.hasNext()) {
                return z2;
            }
            Multiset.Entry<?> next = it.next();
            int iCount = multiset2.count(next.getElement());
            if (iCount >= next.getCount()) {
                it.remove();
                z = true;
            } else if (iCount > 0) {
                multiset.remove(next.getElement(), iCount);
                z = true;
            } else {
                z = z2;
            }
        }
    }

    public static boolean removeOccurrences(Multiset<?> multiset, Iterable<?> iterable) {
        if (iterable instanceof Multiset) {
            return removeOccurrences(multiset, (Multiset<?>) iterable);
        }
        Preconditions.checkNotNull(multiset);
        Preconditions.checkNotNull(iterable);
        boolean zRemove = false;
        Iterator<?> it = iterable.iterator();
        while (it.hasNext()) {
            zRemove |= multiset.remove(it.next());
        }
        return zRemove;
    }

    static boolean retainAllImpl(Multiset<?> multiset, Collection<?> collection) {
        Preconditions.checkNotNull(collection);
        if (collection instanceof Multiset) {
            collection = ((Multiset) collection).elementSet();
        }
        return multiset.elementSet().retainAll(collection);
    }

    public static boolean retainOccurrences(Multiset<?> multiset, Multiset<?> multiset2) {
        return retainOccurrencesImpl(multiset, multiset2);
    }

    private static <E> boolean retainOccurrencesImpl(Multiset<E> multiset, Multiset<?> multiset2) {
        Preconditions.checkNotNull(multiset);
        Preconditions.checkNotNull(multiset2);
        Iterator<Multiset.Entry<E>> it = multiset.entrySet().iterator();
        boolean z = false;
        while (true) {
            boolean z2 = z;
            if (!it.hasNext()) {
                return z2;
            }
            Multiset.Entry<E> next = it.next();
            int iCount = multiset2.count(next.getElement());
            if (iCount == 0) {
                it.remove();
                z = true;
            } else if (iCount < next.getCount()) {
                multiset.setCount(next.getElement(), iCount);
                z = true;
            } else {
                z = z2;
            }
        }
    }

    static <E> int setCountImpl(Multiset<E> multiset, @ParametricNullness E e, int i) {
        CollectPreconditions.checkNonnegative(i, "count");
        int iCount = multiset.count(e);
        int i2 = i - iCount;
        if (i2 > 0) {
            multiset.add(e, i2);
        } else if (i2 < 0) {
            multiset.remove(e, -i2);
        }
        return iCount;
    }

    static <E> boolean setCountImpl(Multiset<E> multiset, @ParametricNullness E e, int i, int i2) {
        CollectPreconditions.checkNonnegative(i, "oldCount");
        CollectPreconditions.checkNonnegative(i2, "newCount");
        if (multiset.count(e) != i) {
            return false;
        }
        multiset.setCount(e, i2);
        return true;
    }

    public static <E> Multiset<E> sum(Multiset<? extends E> multiset, Multiset<? extends E> multiset2) {
        Preconditions.checkNotNull(multiset);
        Preconditions.checkNotNull(multiset2);
        return new AnonymousClass3(multiset, multiset2);
    }

    static <T, E, M extends Multiset<E>> Collector<T, ?, M> toMultiset(Function<? super T, E> function, ToIntFunction<? super T> toIntFunction, Supplier<M> supplier) {
        return CollectCollectors.toMultiset(function, toIntFunction, supplier);
    }

    public static <E> Multiset<E> union(Multiset<? extends E> multiset, Multiset<? extends E> multiset2) {
        Preconditions.checkNotNull(multiset);
        Preconditions.checkNotNull(multiset2);
        return new AnonymousClass1(multiset, multiset2);
    }

    @Deprecated
    public static <E> Multiset<E> unmodifiableMultiset(ImmutableMultiset<E> immutableMultiset) {
        return (Multiset) Preconditions.checkNotNull(immutableMultiset);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static <E> Multiset<E> unmodifiableMultiset(Multiset<? extends E> multiset) {
        return ((multiset instanceof UnmodifiableMultiset) || (multiset instanceof ImmutableMultiset)) ? multiset : new UnmodifiableMultiset((Multiset) Preconditions.checkNotNull(multiset));
    }

    public static <E> SortedMultiset<E> unmodifiableSortedMultiset(SortedMultiset<E> sortedMultiset) {
        return new UnmodifiableSortedMultiset((SortedMultiset) Preconditions.checkNotNull(sortedMultiset));
    }
}
