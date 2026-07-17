package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.Serializable;
import java.util.Comparator;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class GeneralRange<T> implements Serializable {
    private final Comparator<? super T> comparator;
    private final boolean hasLowerBound;
    private final boolean hasUpperBound;
    private final BoundType lowerBoundType;

    @CheckForNull
    private final T lowerEndpoint;

    @CheckForNull
    @LazyInit
    private transient GeneralRange<T> reverse;
    private final BoundType upperBoundType;

    @CheckForNull
    private final T upperEndpoint;

    private GeneralRange(Comparator<? super T> comparator, boolean z, @CheckForNull T t, BoundType boundType, boolean z2, @CheckForNull T t2, BoundType boundType2) {
        boolean z3 = true;
        this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
        this.hasLowerBound = z;
        this.hasUpperBound = z2;
        this.lowerEndpoint = t;
        this.lowerBoundType = (BoundType) Preconditions.checkNotNull(boundType);
        this.upperEndpoint = t2;
        this.upperBoundType = (BoundType) Preconditions.checkNotNull(boundType2);
        if (z) {
            comparator.compare((Object) NullnessCasts.uncheckedCastNullableTToT(t), (Object) NullnessCasts.uncheckedCastNullableTToT(t));
        }
        if (z2) {
            comparator.compare((Object) NullnessCasts.uncheckedCastNullableTToT(t2), (Object) NullnessCasts.uncheckedCastNullableTToT(t2));
        }
        if (z && z2) {
            int iCompare = comparator.compare((Object) NullnessCasts.uncheckedCastNullableTToT(t), (Object) NullnessCasts.uncheckedCastNullableTToT(t2));
            Preconditions.checkArgument(iCompare <= 0, "lowerEndpoint (%s) > upperEndpoint (%s)", t, t2);
            if (iCompare == 0) {
                if (boundType == BoundType.OPEN && boundType2 == BoundType.OPEN) {
                    z3 = false;
                }
                Preconditions.checkArgument(z3);
            }
        }
    }

    static <T> GeneralRange<T> all(Comparator<? super T> comparator) {
        return new GeneralRange<>(comparator, false, null, BoundType.OPEN, false, null, BoundType.OPEN);
    }

    static <T> GeneralRange<T> downTo(Comparator<? super T> comparator, @ParametricNullness T t, BoundType boundType) {
        return new GeneralRange<>(comparator, true, t, boundType, false, null, BoundType.OPEN);
    }

    static <T extends Comparable> GeneralRange<T> from(Range<T> range) {
        return new GeneralRange<>(Ordering.natural(), range.hasLowerBound(), range.hasLowerBound() ? range.lowerEndpoint() : null, range.hasLowerBound() ? range.lowerBoundType() : BoundType.OPEN, range.hasUpperBound(), range.hasUpperBound() ? range.upperEndpoint() : null, range.hasUpperBound() ? range.upperBoundType() : BoundType.OPEN);
    }

    static <T> GeneralRange<T> range(Comparator<? super T> comparator, @ParametricNullness T t, BoundType boundType, @ParametricNullness T t2, BoundType boundType2) {
        return new GeneralRange<>(comparator, true, t, boundType, true, t2, boundType2);
    }

    static <T> GeneralRange<T> upTo(Comparator<? super T> comparator, @ParametricNullness T t, BoundType boundType) {
        return new GeneralRange<>(comparator, false, null, BoundType.OPEN, true, t, boundType);
    }

    Comparator<? super T> comparator() {
        return this.comparator;
    }

    boolean contains(@ParametricNullness T t) {
        return (tooLow(t) || tooHigh(t)) ? false : true;
    }

    public boolean equals(@CheckForNull Object obj) {
        if (!(obj instanceof GeneralRange)) {
            return false;
        }
        GeneralRange generalRange = (GeneralRange) obj;
        return this.comparator.equals(generalRange.comparator) && this.hasLowerBound == generalRange.hasLowerBound && this.hasUpperBound == generalRange.hasUpperBound && getLowerBoundType().equals(generalRange.getLowerBoundType()) && getUpperBoundType().equals(generalRange.getUpperBoundType()) && Objects.equal(getLowerEndpoint(), generalRange.getLowerEndpoint()) && Objects.equal(getUpperEndpoint(), generalRange.getUpperEndpoint());
    }

    BoundType getLowerBoundType() {
        return this.lowerBoundType;
    }

    @CheckForNull
    T getLowerEndpoint() {
        return this.lowerEndpoint;
    }

    BoundType getUpperBoundType() {
        return this.upperBoundType;
    }

    @CheckForNull
    T getUpperEndpoint() {
        return this.upperEndpoint;
    }

    boolean hasLowerBound() {
        return this.hasLowerBound;
    }

    boolean hasUpperBound() {
        return this.hasUpperBound;
    }

    public int hashCode() {
        return Objects.hashCode(this.comparator, getLowerEndpoint(), getLowerBoundType(), getUpperEndpoint(), getUpperBoundType());
    }

    GeneralRange<T> intersect(GeneralRange<T> generalRange) {
        int iCompare;
        int iCompare2;
        T t;
        BoundType boundType;
        int iCompare3;
        Preconditions.checkNotNull(generalRange);
        Preconditions.checkArgument(this.comparator.equals(generalRange.comparator));
        boolean z = this.hasLowerBound;
        T lowerEndpoint = getLowerEndpoint();
        BoundType lowerBoundType = getLowerBoundType();
        if (!hasLowerBound()) {
            z = generalRange.hasLowerBound;
            lowerEndpoint = generalRange.getLowerEndpoint();
            lowerBoundType = generalRange.getLowerBoundType();
        } else if (generalRange.hasLowerBound() && ((iCompare = this.comparator.compare(getLowerEndpoint(), generalRange.getLowerEndpoint())) < 0 || (iCompare == 0 && generalRange.getLowerBoundType() == BoundType.OPEN))) {
            lowerEndpoint = generalRange.getLowerEndpoint();
            lowerBoundType = generalRange.getLowerBoundType();
        }
        boolean z2 = this.hasUpperBound;
        T upperEndpoint = getUpperEndpoint();
        BoundType upperBoundType = getUpperBoundType();
        if (!hasUpperBound()) {
            z2 = generalRange.hasUpperBound;
            upperEndpoint = generalRange.getUpperEndpoint();
            upperBoundType = generalRange.getUpperBoundType();
        } else if (generalRange.hasUpperBound() && ((iCompare2 = this.comparator.compare(getUpperEndpoint(), generalRange.getUpperEndpoint())) > 0 || (iCompare2 == 0 && generalRange.getUpperBoundType() == BoundType.OPEN))) {
            upperEndpoint = generalRange.getUpperEndpoint();
            upperBoundType = generalRange.getUpperBoundType();
        }
        if (z && z2 && ((iCompare3 = this.comparator.compare(lowerEndpoint, upperEndpoint)) > 0 || (iCompare3 == 0 && lowerBoundType == BoundType.OPEN && upperBoundType == BoundType.OPEN))) {
            boundType = BoundType.OPEN;
            upperBoundType = BoundType.CLOSED;
            t = upperEndpoint;
        } else {
            t = lowerEndpoint;
            boundType = lowerBoundType;
        }
        return new GeneralRange<>(this.comparator, z, t, boundType, z2, upperEndpoint, upperBoundType);
    }

    /* JADX WARN: Multi-variable type inference failed */
    boolean isEmpty() {
        return (hasUpperBound() && tooLow(NullnessCasts.uncheckedCastNullableTToT(getUpperEndpoint()))) || (hasLowerBound() && tooHigh(NullnessCasts.uncheckedCastNullableTToT(getLowerEndpoint())));
    }

    GeneralRange<T> reverse() {
        GeneralRange<T> generalRange = this.reverse;
        if (generalRange != null) {
            return generalRange;
        }
        GeneralRange<T> generalRange2 = new GeneralRange<>(Ordering.from(this.comparator).reverse(), this.hasUpperBound, getUpperEndpoint(), getUpperBoundType(), this.hasLowerBound, getLowerEndpoint(), getLowerBoundType());
        generalRange2.reverse = this;
        this.reverse = generalRange2;
        return generalRange2;
    }

    public String toString() {
        return this.comparator + ":" + (this.lowerBoundType == BoundType.CLOSED ? '[' : '(') + (this.hasLowerBound ? this.lowerEndpoint : "-∞") + ',' + (this.hasUpperBound ? this.upperEndpoint : "∞") + (this.upperBoundType == BoundType.CLOSED ? ']' : ')');
    }

    boolean tooHigh(@ParametricNullness T t) {
        if (!hasUpperBound()) {
            return false;
        }
        int iCompare = this.comparator.compare(t, NullnessCasts.uncheckedCastNullableTToT(getUpperEndpoint()));
        return ((iCompare == 0) & (getUpperBoundType() == BoundType.OPEN)) | (iCompare > 0);
    }

    boolean tooLow(@ParametricNullness T t) {
        if (!hasLowerBound()) {
            return false;
        }
        int iCompare = this.comparator.compare(t, NullnessCasts.uncheckedCastNullableTToT(getLowerEndpoint()));
        return ((iCompare == 0) & (getLowerBoundType() == BoundType.OPEN)) | (iCompare < 0);
    }
}
