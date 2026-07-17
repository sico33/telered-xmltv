package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class CollectCollectors {
    private static final Collector<Object, ?, ImmutableList<Object>> TO_IMMUTABLE_LIST = Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda17
        @Override // java.util.function.Supplier
        public final Object get() {
            return ImmutableList.builder();
        }
    }, new BiConsumer() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda20
        @Override // java.util.function.BiConsumer
        public final void accept(Object obj, Object obj2) {
            ((ImmutableList.Builder) obj).add(obj2);
        }
    }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda21
        @Override // java.util.function.BiFunction
        public final Object apply(Object obj, Object obj2) {
            return ((ImmutableList.Builder) obj).combine((ImmutableList.Builder) obj2);
        }
    }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda22
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return ((ImmutableList.Builder) obj).build();
        }
    }, new Collector.Characteristics[0]);
    private static final Collector<Object, ?, ImmutableSet<Object>> TO_IMMUTABLE_SET = Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda23
        @Override // java.util.function.Supplier
        public final Object get() {
            return ImmutableSet.builder();
        }
    }, new BiConsumer() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda24
        @Override // java.util.function.BiConsumer
        public final void accept(Object obj, Object obj2) {
            ((ImmutableSet.Builder) obj).add(obj2);
        }
    }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda25
        @Override // java.util.function.BiFunction
        public final Object apply(Object obj, Object obj2) {
            return ((ImmutableSet.Builder) obj).combine((ImmutableSet.Builder) obj2);
        }
    }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda26
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return ((ImmutableSet.Builder) obj).build();
        }
    }, new Collector.Characteristics[0]);
    private static final Collector<Range<Comparable<?>>, ?, ImmutableRangeSet<Comparable<?>>> TO_IMMUTABLE_RANGE_SET = Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda27
        @Override // java.util.function.Supplier
        public final Object get() {
            return ImmutableRangeSet.builder();
        }
    }, new BiConsumer() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda28
        @Override // java.util.function.BiConsumer
        public final void accept(Object obj, Object obj2) {
            ((ImmutableRangeSet.Builder) obj).add((Range) obj2);
        }
    }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda18
        @Override // java.util.function.BiFunction
        public final Object apply(Object obj, Object obj2) {
            return ((ImmutableRangeSet.Builder) obj).combine((ImmutableRangeSet.Builder) obj2);
        }
    }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda19
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return ((ImmutableRangeSet.Builder) obj).build();
        }
    }, new Collector.Characteristics[0]);

    /* JADX INFO: Access modifiers changed from: private */
    static class EnumMapAccumulator<K extends Enum<K>, V> {

        @CheckForNull
        private EnumMap<K, V> map = null;
        private final BinaryOperator<V> mergeFunction;

        EnumMapAccumulator(BinaryOperator<V> binaryOperator) {
            this.mergeFunction = binaryOperator;
        }

        EnumMapAccumulator<K, V> combine(EnumMapAccumulator<K, V> enumMapAccumulator) {
            if (this.map == null) {
                return enumMapAccumulator;
            }
            if (enumMapAccumulator.map == null) {
                return this;
            }
            enumMapAccumulator.map.forEach(new BiConsumer(this) { // from class: com.google.common.collect.CollectCollectors$EnumMapAccumulator$$ExternalSyntheticLambda0
                public final CollectCollectors.EnumMapAccumulator f$0;

                {
                    this.f$0 = this;
                }

                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    this.f$0.put((Enum) obj, obj2);
                }
            });
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void put(K k, V v) {
            if (this.map == null) {
                this.map = new EnumMap<>(Collections.singletonMap(k, v));
            } else {
                this.map.merge(k, v, this.mergeFunction);
            }
        }

        ImmutableMap<K, V> toImmutableMap() {
            return this.map == null ? ImmutableMap.of() : ImmutableEnumMap.asImmutable(this.map);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class EnumSetAccumulator<E extends Enum<E>> {
        static final Collector<Enum<?>, ?, ImmutableSet<? extends Enum<?>>> TO_IMMUTABLE_ENUM_SET = CollectCollectors.toImmutableEnumSetGeneric();

        @CheckForNull
        private EnumSet<E> set;

        private EnumSetAccumulator() {
        }

        void add(E e) {
            if (this.set == null) {
                this.set = EnumSet.of((Enum) e);
            } else {
                this.set.add(e);
            }
        }

        EnumSetAccumulator<E> combine(EnumSetAccumulator<E> enumSetAccumulator) {
            if (this.set == null) {
                return enumSetAccumulator;
            }
            if (enumSetAccumulator.set == null) {
                return this;
            }
            this.set.addAll(enumSetAccumulator.set);
            return this;
        }

        ImmutableSet<E> toImmutableSet() {
            if (this.set == null) {
                return ImmutableSet.of();
            }
            ImmutableSet<E> immutableSetAsImmutable = ImmutableEnumSet.asImmutable(this.set);
            this.set = null;
            return immutableSetAsImmutable;
        }
    }

    private CollectCollectors() {
    }

    static <T, K, V> Collector<T, ?, ImmutableListMultimap<K, V>> flatteningToImmutableListMultimap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends Stream<? extends V>> function2) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        Function function3 = new Function(function) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda31
            public final Function f$0;

            {
                this.f$0 = function;
            }

            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return Preconditions.checkNotNull(this.f$0.apply(obj));
            }
        };
        Function function4 = new Function(function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda32
            public final Function f$0;

            {
                this.f$0 = function2;
            }

            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((Stream) this.f$0.apply(obj)).peek(new CollectCollectors$$ExternalSyntheticLambda45());
            }
        };
        final MultimapBuilder.ListMultimapBuilder<Object, Object> listMultimapBuilderArrayListValues = MultimapBuilder.linkedHashKeys().arrayListValues();
        Objects.requireNonNull(listMultimapBuilderArrayListValues);
        return Collectors.collectingAndThen(flatteningToMultimap(function3, function4, new Supplier(listMultimapBuilderArrayListValues) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda33
            public final MultimapBuilder.ListMultimapBuilder f$0;

            {
                this.f$0 = listMultimapBuilderArrayListValues;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return this.f$0.build();
            }
        }), new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda34
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ImmutableListMultimap.copyOf((Multimap) obj);
            }
        });
    }

    static <T, K, V> Collector<T, ?, ImmutableSetMultimap<K, V>> flatteningToImmutableSetMultimap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends Stream<? extends V>> function2) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        Function function3 = new Function(function) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda39
            public final Function f$0;

            {
                this.f$0 = function;
            }

            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return Preconditions.checkNotNull(this.f$0.apply(obj));
            }
        };
        Function function4 = new Function(function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda40
            public final Function f$0;

            {
                this.f$0 = function2;
            }

            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((Stream) this.f$0.apply(obj)).peek(new CollectCollectors$$ExternalSyntheticLambda45());
            }
        };
        final MultimapBuilder.SetMultimapBuilder<Object, Object> setMultimapBuilderLinkedHashSetValues = MultimapBuilder.linkedHashKeys().linkedHashSetValues();
        Objects.requireNonNull(setMultimapBuilderLinkedHashSetValues);
        return Collectors.collectingAndThen(flatteningToMultimap(function3, function4, new Supplier(setMultimapBuilderLinkedHashSetValues) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda41
            public final MultimapBuilder.SetMultimapBuilder f$0;

            {
                this.f$0 = setMultimapBuilderLinkedHashSetValues;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return this.f$0.build();
            }
        }), new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda42
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ImmutableSetMultimap.copyOf((Multimap) obj);
            }
        });
    }

    static <T, K, V, M extends Multimap<K, V>> Collector<T, ?, M> flatteningToMultimap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends Stream<? extends V>> function2, Supplier<M> supplier) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        Preconditions.checkNotNull(supplier);
        return Collector.of(supplier, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda29
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                CollectCollectors.lambda$flatteningToMultimap$26(this.f$0, this.f$1, (Multimap) obj, obj2);
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda30
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return CollectCollectors.lambda$flatteningToMultimap$27((Multimap) obj, (Multimap) obj2);
            }
        }, new Collector.Characteristics[0]);
    }

    static /* synthetic */ void lambda$flatteningToMultimap$26(Function function, Function function2, Multimap multimap, Object obj) {
        final Collection collection = multimap.get(function.apply(obj));
        Stream stream = (Stream) function2.apply(obj);
        Objects.requireNonNull(collection);
        stream.forEachOrdered(new Consumer(collection) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda68
            public final Collection f$0;

            {
                this.f$0 = collection;
            }

            @Override // java.util.function.Consumer
            public final void accept(Object obj2) {
                this.f$0.add(obj2);
            }
        });
    }

    static /* synthetic */ Multimap lambda$flatteningToMultimap$27(Multimap multimap, Multimap multimap2) {
        multimap.putAll(multimap2);
        return multimap;
    }

    static /* synthetic */ Object lambda$toImmutableEnumMap$12(Object obj, Object obj2) {
        throw new IllegalArgumentException("Multiple values for key: " + obj + ", " + obj2);
    }

    static /* synthetic */ EnumMapAccumulator lambda$toImmutableEnumMap$13() {
        return new EnumMapAccumulator(new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda0
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return CollectCollectors.lambda$toImmutableEnumMap$12(obj, obj2);
            }
        });
    }

    static /* synthetic */ EnumMapAccumulator lambda$toImmutableEnumMap$15(BinaryOperator binaryOperator) {
        return new EnumMapAccumulator(binaryOperator);
    }

    static /* synthetic */ EnumSetAccumulator lambda$toImmutableEnumSetGeneric$1() {
        return new EnumSetAccumulator();
    }

    static /* synthetic */ Multiset lambda$toImmutableMultiset$3(Multiset multiset, Multiset multiset2) {
        multiset.addAll(multiset2);
        return multiset;
    }

    static /* synthetic */ TreeMap lambda$toImmutableSortedMap$10(Comparator comparator) {
        return new TreeMap(comparator);
    }

    static /* synthetic */ ImmutableSortedMap.Builder lambda$toImmutableSortedMap$8(Comparator comparator) {
        return new ImmutableSortedMap.Builder(comparator);
    }

    static /* synthetic */ ImmutableSortedSet.Builder lambda$toImmutableSortedSet$0(Comparator comparator) {
        return new ImmutableSortedSet.Builder(comparator);
    }

    static /* synthetic */ Multimap lambda$toMultimap$25(Multimap multimap, Multimap multimap2) {
        multimap.putAll(multimap2);
        return multimap;
    }

    static /* synthetic */ Multiset lambda$toMultiset$6(Multiset multiset, Multiset multiset2) {
        multiset.addAll(multiset2);
        return multiset;
    }

    static <T, K, V> Collector<T, ?, ImmutableBiMap<K, V>> toImmutableBiMap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends V> function2) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        return Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda9
            @Override // java.util.function.Supplier
            public final Object get() {
                return new ImmutableBiMap.Builder();
            }
        }, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda10
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ImmutableBiMap.Builder) obj).put(this.f$0.apply(obj2), this.f$1.apply(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda11
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((ImmutableBiMap.Builder) obj).combine((ImmutableMap.Builder) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda12
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ImmutableBiMap.Builder) obj).buildOrThrow();
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, K extends Enum<K>, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableEnumMap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends V> function2) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        return Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda46
            @Override // java.util.function.Supplier
            public final Object get() {
                return CollectCollectors.lambda$toImmutableEnumMap$13();
            }
        }, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda47
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((CollectCollectors.EnumMapAccumulator) obj).put((Enum) Preconditions.checkNotNull((Enum) this.f$0.apply(obj2), "Null key for input %s", obj2), Preconditions.checkNotNull(this.f$1.apply(obj2), "Null value for input %s", obj2));
            }
        }, new CollectCollectors$$ExternalSyntheticLambda48(), new CollectCollectors$$ExternalSyntheticLambda49(), Collector.Characteristics.UNORDERED);
    }

    static <T, K extends Enum<K>, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableEnumMap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends V> function2, final BinaryOperator<V> binaryOperator) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        Preconditions.checkNotNull(binaryOperator);
        return Collector.of(new Supplier(binaryOperator) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda71
            public final BinaryOperator f$0;

            {
                this.f$0 = binaryOperator;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return CollectCollectors.lambda$toImmutableEnumMap$15(this.f$0);
            }
        }, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda72
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((CollectCollectors.EnumMapAccumulator) obj).put((Enum) Preconditions.checkNotNull((Enum) this.f$0.apply(obj2), "Null key for input %s", obj2), Preconditions.checkNotNull(this.f$1.apply(obj2), "Null value for input %s", obj2));
            }
        }, new CollectCollectors$$ExternalSyntheticLambda48(), new CollectCollectors$$ExternalSyntheticLambda49(), new Collector.Characteristics[0]);
    }

    static <E extends Enum<E>> Collector<E, ?, ImmutableSet<E>> toImmutableEnumSet() {
        return (Collector<E, ?, ImmutableSet<E>>) EnumSetAccumulator.TO_IMMUTABLE_ENUM_SET;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <E extends Enum<E>> Collector<E, EnumSetAccumulator<E>, ImmutableSet<E>> toImmutableEnumSetGeneric() {
        return Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda13
            @Override // java.util.function.Supplier
            public final Object get() {
                return CollectCollectors.lambda$toImmutableEnumSetGeneric$1();
            }
        }, new BiConsumer() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda14
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((CollectCollectors.EnumSetAccumulator) obj).add((Enum) obj2);
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda15
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((CollectCollectors.EnumSetAccumulator) obj).combine((CollectCollectors.EnumSetAccumulator) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda16
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((CollectCollectors.EnumSetAccumulator) obj).toImmutableSet();
            }
        }, Collector.Characteristics.UNORDERED);
    }

    static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() {
        return (Collector<E, ?, ImmutableList<E>>) TO_IMMUTABLE_LIST;
    }

    static <T, K, V> Collector<T, ?, ImmutableListMultimap<K, V>> toImmutableListMultimap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends V> function2) {
        Preconditions.checkNotNull(function, "keyFunction");
        Preconditions.checkNotNull(function2, "valueFunction");
        return Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda58
            @Override // java.util.function.Supplier
            public final Object get() {
                return ImmutableListMultimap.builder();
            }
        }, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda59
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ImmutableListMultimap.Builder) obj).put(this.f$0.apply(obj2), this.f$1.apply(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda60
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((ImmutableListMultimap.Builder) obj).combine((ImmutableMultimap.Builder) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda61
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ImmutableListMultimap.Builder) obj).build();
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends V> function2) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        return Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda50
            @Override // java.util.function.Supplier
            public final Object get() {
                return new ImmutableMap.Builder();
            }
        }, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda51
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ImmutableMap.Builder) obj).put(this.f$0.apply(obj2), this.f$1.apply(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda52
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((ImmutableMap.Builder) obj).combine((ImmutableMap.Builder) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda53
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ImmutableMap.Builder) obj).buildOrThrow();
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(Function<? super T, ? extends K> function, Function<? super T, ? extends V> function2, BinaryOperator<V> binaryOperator) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        Preconditions.checkNotNull(binaryOperator);
        return Collectors.collectingAndThen(Collectors.toMap(function, function2, binaryOperator, new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda66
            @Override // java.util.function.Supplier
            public final Object get() {
                return new LinkedHashMap();
            }
        }), new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda67
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ImmutableMap.copyOf((Map) obj);
            }
        });
    }

    static <T, E> Collector<T, ?, ImmutableMultiset<E>> toImmutableMultiset(final Function<? super T, ? extends E> function, final ToIntFunction<? super T> toIntFunction) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(toIntFunction);
        return Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda35
            @Override // java.util.function.Supplier
            public final Object get() {
                return LinkedHashMultiset.create();
            }
        }, new BiConsumer(function, toIntFunction) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda36
            public final Function f$0;
            public final ToIntFunction f$1;

            {
                this.f$0 = function;
                this.f$1 = toIntFunction;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((Multiset) obj).add(Preconditions.checkNotNull(this.f$0.apply(obj2)), this.f$1.applyAsInt(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda37
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return CollectCollectors.lambda$toImmutableMultiset$3((Multiset) obj, (Multiset) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda38
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ImmutableMultiset.copyFromEntries(((Multiset) obj).entrySet());
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, K extends Comparable<? super K>, V> Collector<T, ?, ImmutableRangeMap<K, V>> toImmutableRangeMap(final Function<? super T, Range<K>> function, final Function<? super T, ? extends V> function2) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        return Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                return ImmutableRangeMap.builder();
            }
        }, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda2
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ImmutableRangeMap.Builder) obj).put((Range) this.f$0.apply(obj2), this.f$1.apply(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda3
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((ImmutableRangeMap.Builder) obj).combine((ImmutableRangeMap.Builder) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda4
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ImmutableRangeMap.Builder) obj).build();
            }
        }, new Collector.Characteristics[0]);
    }

    static <E extends Comparable<? super E>> Collector<Range<E>, ?, ImmutableRangeSet<E>> toImmutableRangeSet() {
        return (Collector<Range<E>, ?, ImmutableRangeSet<E>>) TO_IMMUTABLE_RANGE_SET;
    }

    static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
        return (Collector<E, ?, ImmutableSet<E>>) TO_IMMUTABLE_SET;
    }

    static <T, K, V> Collector<T, ?, ImmutableSetMultimap<K, V>> toImmutableSetMultimap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends V> function2) {
        Preconditions.checkNotNull(function, "keyFunction");
        Preconditions.checkNotNull(function2, "valueFunction");
        return Collector.of(new Supplier() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda62
            @Override // java.util.function.Supplier
            public final Object get() {
                return ImmutableSetMultimap.builder();
            }
        }, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda63
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ImmutableSetMultimap.Builder) obj).put(this.f$0.apply(obj2), this.f$1.apply(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda64
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((ImmutableSetMultimap.Builder) obj).combine((ImmutableMultimap.Builder) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda65
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ImmutableSetMultimap.Builder) obj).build();
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, K, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(final Comparator<? super K> comparator, final Function<? super T, ? extends K> function, final Function<? super T, ? extends V> function2) {
        Preconditions.checkNotNull(comparator);
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        return Collector.of(new Supplier(comparator) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda5
            public final Comparator f$0;

            {
                this.f$0 = comparator;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return CollectCollectors.lambda$toImmutableSortedMap$8(this.f$0);
            }
        }, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda6
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ImmutableSortedMap.Builder) obj).put(this.f$0.apply(obj2), this.f$1.apply(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda7
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((ImmutableSortedMap.Builder) obj).combine((ImmutableSortedMap.Builder) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda8
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ImmutableSortedMap.Builder) obj).buildOrThrow();
            }
        }, Collector.Characteristics.UNORDERED);
    }

    static <T, K, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(final Comparator<? super K> comparator, Function<? super T, ? extends K> function, Function<? super T, ? extends V> function2, BinaryOperator<V> binaryOperator) {
        Preconditions.checkNotNull(comparator);
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        Preconditions.checkNotNull(binaryOperator);
        return Collectors.collectingAndThen(Collectors.toMap(function, function2, binaryOperator, new Supplier(comparator) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda43
            public final Comparator f$0;

            {
                this.f$0 = comparator;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return CollectCollectors.lambda$toImmutableSortedMap$10(this.f$0);
            }
        }), new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda44
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ImmutableSortedMap.copyOfSorted((TreeMap) obj);
            }
        });
    }

    static <E> Collector<E, ?, ImmutableSortedSet<E>> toImmutableSortedSet(final Comparator<? super E> comparator) {
        Preconditions.checkNotNull(comparator);
        return Collector.of(new Supplier(comparator) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda54
            public final Comparator f$0;

            {
                this.f$0 = comparator;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return CollectCollectors.lambda$toImmutableSortedSet$0(this.f$0);
            }
        }, new BiConsumer() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda55
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ImmutableSortedSet.Builder) obj).add(obj2);
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda56
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((ImmutableSortedSet.Builder) obj).combine((ImmutableSet.Builder) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda57
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ImmutableSortedSet.Builder) obj).build();
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, K, V, M extends Multimap<K, V>> Collector<T, ?, M> toMultimap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends V> function2, Supplier<M> supplier) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        Preconditions.checkNotNull(supplier);
        return Collector.of(supplier, new BiConsumer(function, function2) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda73
            public final Function f$0;
            public final Function f$1;

            {
                this.f$0 = function;
                this.f$1 = function2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((Multimap) obj).put(this.f$0.apply(obj2), this.f$1.apply(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda74
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return CollectCollectors.lambda$toMultimap$25((Multimap) obj, (Multimap) obj2);
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, E, M extends Multiset<E>> Collector<T, ?, M> toMultiset(final Function<? super T, E> function, final ToIntFunction<? super T> toIntFunction, Supplier<M> supplier) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(toIntFunction);
        Preconditions.checkNotNull(supplier);
        return Collector.of(supplier, new BiConsumer(function, toIntFunction) { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda69
            public final Function f$0;
            public final ToIntFunction f$1;

            {
                this.f$0 = function;
                this.f$1 = toIntFunction;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((Multiset) obj).add(this.f$0.apply(obj2), this.f$1.applyAsInt(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.CollectCollectors$$ExternalSyntheticLambda70
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return CollectCollectors.lambda$toMultiset$6((Multiset) obj, (Multiset) obj2);
            }
        }, new Collector.Characteristics[0]);
    }
}
