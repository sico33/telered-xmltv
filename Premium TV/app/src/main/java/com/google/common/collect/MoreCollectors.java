package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import javax.annotation.CheckForNull;
import kotlin.text.Typography;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class MoreCollectors {
    private static final Collector<Object, ?, Optional<Object>> TO_OPTIONAL = Collector.of(new Supplier() { // from class: com.google.common.collect.MoreCollectors$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new MoreCollectors.ToOptionalState();
        }
    }, new BiConsumer() { // from class: com.google.common.collect.MoreCollectors$$ExternalSyntheticLambda1
        @Override // java.util.function.BiConsumer
        public final void accept(Object obj, Object obj2) {
            ((MoreCollectors.ToOptionalState) obj).add(obj2);
        }
    }, new BinaryOperator() { // from class: com.google.common.collect.MoreCollectors$$ExternalSyntheticLambda2
        @Override // java.util.function.BiFunction
        public final Object apply(Object obj, Object obj2) {
            return ((MoreCollectors.ToOptionalState) obj).combine((MoreCollectors.ToOptionalState) obj2);
        }
    }, new Function() { // from class: com.google.common.collect.MoreCollectors$$ExternalSyntheticLambda3
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return ((MoreCollectors.ToOptionalState) obj).getOptional();
        }
    }, Collector.Characteristics.UNORDERED);
    private static final Object NULL_PLACEHOLDER = new Object();
    private static final Collector<Object, ?, Object> ONLY_ELEMENT = Collector.of(new Supplier() { // from class: com.google.common.collect.MoreCollectors$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new MoreCollectors.ToOptionalState();
        }
    }, new BiConsumer() { // from class: com.google.common.collect.MoreCollectors$$ExternalSyntheticLambda4
        @Override // java.util.function.BiConsumer
        public final void accept(Object obj, Object obj2) {
            MoreCollectors.lambda$static$0((MoreCollectors.ToOptionalState) obj, obj2);
        }
    }, new BinaryOperator() { // from class: com.google.common.collect.MoreCollectors$$ExternalSyntheticLambda2
        @Override // java.util.function.BiFunction
        public final Object apply(Object obj, Object obj2) {
            return ((MoreCollectors.ToOptionalState) obj).combine((MoreCollectors.ToOptionalState) obj2);
        }
    }, new Function() { // from class: com.google.common.collect.MoreCollectors$$ExternalSyntheticLambda5
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return MoreCollectors.lambda$static$1((MoreCollectors.ToOptionalState) obj);
        }
    }, Collector.Characteristics.UNORDERED);

    /* JADX INFO: Access modifiers changed from: private */
    static final class ToOptionalState {
        static final int MAX_EXTRAS = 4;

        @CheckForNull
        Object element = null;
        List<Object> extras = Collections.emptyList();

        ToOptionalState() {
        }

        void add(Object obj) {
            Preconditions.checkNotNull(obj);
            if (this.element == null) {
                this.element = obj;
                return;
            }
            if (this.extras.isEmpty()) {
                this.extras = new ArrayList(4);
                this.extras.add(obj);
            } else {
                if (this.extras.size() >= 4) {
                    throw multiples(true);
                }
                this.extras.add(obj);
            }
        }

        ToOptionalState combine(ToOptionalState toOptionalState) {
            if (this.element == null) {
                return toOptionalState;
            }
            if (toOptionalState.element == null) {
                return this;
            }
            if (this.extras.isEmpty()) {
                this.extras = new ArrayList();
            }
            this.extras.add(toOptionalState.element);
            this.extras.addAll(toOptionalState.extras);
            if (this.extras.size() <= 4) {
                return this;
            }
            this.extras.subList(4, this.extras.size()).clear();
            throw multiples(true);
        }

        Object getElement() {
            if (this.element == null) {
                throw new NoSuchElementException();
            }
            if (this.extras.isEmpty()) {
                return this.element;
            }
            throw multiples(false);
        }

        Optional<Object> getOptional() {
            if (this.extras.isEmpty()) {
                return Optional.ofNullable(this.element);
            }
            throw multiples(false);
        }

        IllegalArgumentException multiples(boolean z) {
            StringBuilder sbAppend = new StringBuilder().append("expected one element but was: <").append(this.element);
            Iterator<Object> it = this.extras.iterator();
            while (it.hasNext()) {
                sbAppend.append(", ").append(it.next());
            }
            if (z) {
                sbAppend.append(", ...");
            }
            sbAppend.append(Typography.greater);
            throw new IllegalArgumentException(sbAppend.toString());
        }
    }

    private MoreCollectors() {
    }

    static /* synthetic */ void lambda$static$0(ToOptionalState toOptionalState, Object obj) {
        if (obj == null) {
            obj = NULL_PLACEHOLDER;
        }
        toOptionalState.add(obj);
    }

    static /* synthetic */ Object lambda$static$1(ToOptionalState toOptionalState) {
        Object element = toOptionalState.getElement();
        if (element == NULL_PLACEHOLDER) {
            return null;
        }
        return element;
    }

    public static <T> Collector<T, ?, T> onlyElement() {
        return (Collector<T, ?, T>) ONLY_ELEMENT;
    }

    public static <T> Collector<T, ?, Optional<T>> toOptional() {
        return (Collector<T, ?, Optional<T>>) TO_OPTIONAL;
    }
}
