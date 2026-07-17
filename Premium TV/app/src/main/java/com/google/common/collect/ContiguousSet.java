package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.lang.Comparable;
import java.util.NoSuchElementException;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class ContiguousSet<C extends Comparable> extends ImmutableSortedSet<C> {
    final DiscreteDomain<C> domain;

    ContiguousSet(DiscreteDomain<C> discreteDomain) {
        super(Ordering.natural());
        this.domain = discreteDomain;
    }

    @Deprecated
    public static <E> ImmutableSortedSet.Builder<E> builder() {
        throw new UnsupportedOperationException();
    }

    public static ContiguousSet<Integer> closed(int i, int i2) {
        return create(Range.closed(Integer.valueOf(i), Integer.valueOf(i2)), DiscreteDomain.integers());
    }

    public static ContiguousSet<Long> closed(long j, long j2) {
        return create(Range.closed(Long.valueOf(j), Long.valueOf(j2)), DiscreteDomain.longs());
    }

    public static ContiguousSet<Integer> closedOpen(int i, int i2) {
        return create(Range.closedOpen(Integer.valueOf(i), Integer.valueOf(i2)), DiscreteDomain.integers());
    }

    public static ContiguousSet<Long> closedOpen(long j, long j2) {
        return create(Range.closedOpen(Long.valueOf(j), Long.valueOf(j2)), DiscreteDomain.longs());
    }

    public static <C extends Comparable> ContiguousSet<C> create(Range<C> range, DiscreteDomain<C> discreteDomain) {
        Preconditions.checkNotNull(range);
        Preconditions.checkNotNull(discreteDomain);
        try {
            Range<C> rangeIntersection = !range.hasLowerBound() ? range.intersection(Range.atLeast(discreteDomain.minValue())) : range;
            Range<C> rangeIntersection2 = !range.hasUpperBound() ? rangeIntersection.intersection(Range.atMost(discreteDomain.maxValue())) : rangeIntersection;
            boolean z = rangeIntersection2.isEmpty() || Range.compareOrThrow((Comparable) Objects.requireNonNull(range.lowerBound.leastValueAbove(discreteDomain)), (Comparable) Objects.requireNonNull(range.upperBound.greatestValueBelow(discreteDomain))) > 0;
            return z ? new EmptyContiguousSet(discreteDomain) : new RegularContiguousSet(rangeIntersection2, discreteDomain);
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override // com.google.common.collect.ImmutableSortedSet
    ImmutableSortedSet<C> createDescendingSet() {
        return new DescendingImmutableSortedSet(this);
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet, java.util.SortedSet
    public ContiguousSet<C> headSet(C c) {
        return headSetImpl((Comparable) Preconditions.checkNotNull(c), false);
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
    public ContiguousSet<C> headSet(C c, boolean z) {
        return headSetImpl((Comparable) Preconditions.checkNotNull(c), z);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.google.common.collect.ImmutableSortedSet
    public abstract ContiguousSet<C> headSetImpl(C c, boolean z);

    public abstract ContiguousSet<C> intersection(ContiguousSet<C> contiguousSet);

    public abstract Range<C> range();

    public abstract Range<C> range(BoundType boundType, BoundType boundType2);

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet, java.util.SortedSet
    public ContiguousSet<C> subSet(C c, C c2) {
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(c2);
        Preconditions.checkArgument(comparator().compare(c, c2) <= 0);
        return subSetImpl((Comparable) c, true, (Comparable) c2, false);
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
    public ContiguousSet<C> subSet(C c, boolean z, C c2, boolean z2) {
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(c2);
        Preconditions.checkArgument(comparator().compare(c, c2) <= 0);
        return subSetImpl((Comparable) c, z, (Comparable) c2, z2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.google.common.collect.ImmutableSortedSet
    public abstract ContiguousSet<C> subSetImpl(C c, boolean z, C c2, boolean z2);

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet, java.util.SortedSet
    public ContiguousSet<C> tailSet(C c) {
        return tailSetImpl((Comparable) Preconditions.checkNotNull(c), true);
    }

    @Override // com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
    public ContiguousSet<C> tailSet(C c, boolean z) {
        return tailSetImpl((Comparable) Preconditions.checkNotNull(c), z);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.google.common.collect.ImmutableSortedSet
    public abstract ContiguousSet<C> tailSetImpl(C c, boolean z);

    @Override // java.util.AbstractCollection
    public String toString() {
        return range().toString();
    }

    @Override // com.google.common.collect.ImmutableSortedSet, com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
    Object writeReplace() {
        return super.writeReplace();
    }
}
