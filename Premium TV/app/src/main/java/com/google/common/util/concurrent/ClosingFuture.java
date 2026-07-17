package com.google.common.util.concurrent;

import androidx.media3.exoplayer.mediacodec.AsynchronousMediaCodecBufferEnqueuer$$ExternalSyntheticBackportWithForwarding0;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.DoNotMock;
import java.io.Closeable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@DoNotMock("Use ClosingFuture.from(Futures.immediate*Future)")
@ElementTypesAreNonnullByDefault
public final class ClosingFuture<V> {
    private static final LazyLogger logger = new LazyLogger(ClosingFuture.class);
    private final CloseableList closeables;
    private final FluentFuture<V> future;
    private final AtomicReference<State> state;

    /* JADX INFO: renamed from: com.google.common.util.concurrent.ClosingFuture$11, reason: invalid class name */
    static /* synthetic */ class AnonymousClass11 {
        static final int[] $SwitchMap$com$google$common$util$concurrent$ClosingFuture$State = new int[State.values().length];

        static {
            try {
                $SwitchMap$com$google$common$util$concurrent$ClosingFuture$State[State.SUBSUMED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$ClosingFuture$State[State.WILL_CREATE_VALUE_AND_CLOSER.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$ClosingFuture$State[State.WILL_CLOSE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$ClosingFuture$State[State.CLOSING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$ClosingFuture$State[State.CLOSED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$ClosingFuture$State[State.OPEN.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    public interface AsyncClosingCallable<V> {
        ClosingFuture<V> call(DeferredCloser deferredCloser) throws Exception;
    }

    public interface AsyncClosingFunction<T, U> {
        ClosingFuture<U> apply(DeferredCloser deferredCloser, @ParametricNullness T t) throws Exception;
    }

    private static final class CloseableList extends IdentityHashMap<Closeable, Executor> implements Closeable {
        private volatile boolean closed;
        private final DeferredCloser closer;

        @CheckForNull
        private volatile CountDownLatch whenClosed;

        private CloseableList() {
            this.closer = new DeferredCloser(this);
        }

        void add(@CheckForNull Closeable closeable, Executor executor) {
            Preconditions.checkNotNull(executor);
            if (closeable == null) {
                return;
            }
            synchronized (this) {
                if (this.closed) {
                    ClosingFuture.closeQuietly(closeable, executor);
                } else {
                    put(closeable, executor);
                }
            }
        }

        <V, U> FluentFuture<U> applyAsyncClosingFunction(AsyncClosingFunction<V, U> asyncClosingFunction, @ParametricNullness V v) throws Exception {
            CloseableList closeableList = new CloseableList();
            try {
                ClosingFuture<U> closingFutureApply = asyncClosingFunction.apply(closeableList.closer, v);
                closingFutureApply.becomeSubsumedInto(closeableList);
                return ((ClosingFuture) closingFutureApply).future;
            } finally {
                add(closeableList, MoreExecutors.directExecutor());
            }
        }

        /* JADX WARN: Multi-variable type inference failed */
        <V, U> ListenableFuture<U> applyClosingFunction(ClosingFunction<? super V, U> closingFunction, @ParametricNullness V v) throws Exception {
            CloseableList closeableList = new CloseableList();
            try {
                return Futures.immediateFuture(closingFunction.apply(closeableList.closer, v));
            } finally {
                add(closeableList, MoreExecutors.directExecutor());
            }
        }

        @Override // java.io.Closeable, java.lang.AutoCloseable
        public void close() {
            if (this.closed) {
                return;
            }
            synchronized (this) {
                if (!this.closed) {
                    this.closed = true;
                    for (Map.Entry<Closeable, Executor> entry : entrySet()) {
                        ClosingFuture.closeQuietly(entry.getKey(), entry.getValue());
                    }
                    clear();
                    if (this.whenClosed != null) {
                        this.whenClosed.countDown();
                    }
                }
            }
        }

        CountDownLatch whenClosedCountDown() {
            CountDownLatch countDownLatch;
            if (this.closed) {
                return new CountDownLatch(0);
            }
            synchronized (this) {
                if (this.closed) {
                    countDownLatch = new CountDownLatch(0);
                } else {
                    Preconditions.checkState(this.whenClosed == null);
                    countDownLatch = new CountDownLatch(1);
                    this.whenClosed = countDownLatch;
                }
            }
            return countDownLatch;
        }
    }

    public interface ClosingCallable<V> {
        @ParametricNullness
        V call(DeferredCloser deferredCloser) throws Exception;
    }

    public interface ClosingFunction<T, U> {
        @ParametricNullness
        U apply(DeferredCloser deferredCloser, @ParametricNullness T t) throws Exception;
    }

    @DoNotMock("Use ClosingFuture.whenAllSucceed() or .whenAllComplete() instead.")
    public static class Combiner {
        private final boolean allMustSucceed;
        private final CloseableList closeables;
        protected final ImmutableList<ClosingFuture<?>> inputs;

        public interface AsyncCombiningCallable<V> {
            ClosingFuture<V> call(DeferredCloser deferredCloser, Peeker peeker) throws Exception;
        }

        public interface CombiningCallable<V> {
            @ParametricNullness
            V call(DeferredCloser deferredCloser, Peeker peeker) throws Exception;
        }

        private Combiner(boolean z, Iterable<? extends ClosingFuture<?>> iterable) {
            this.closeables = new CloseableList();
            this.allMustSucceed = z;
            this.inputs = ImmutableList.copyOf(iterable);
            Iterator<? extends ClosingFuture<?>> it = iterable.iterator();
            while (it.hasNext()) {
                it.next().becomeSubsumedInto(this.closeables);
            }
        }

        private Futures.FutureCombiner<Object> futureCombiner() {
            return this.allMustSucceed ? Futures.whenAllSucceed(inputFutures()) : Futures.whenAllComplete(inputFutures());
        }

        private ImmutableList<FluentFuture<?>> inputFutures() {
            return FluentIterable.from(this.inputs).transform(new Function() { // from class: com.google.common.util.concurrent.ClosingFuture$Combiner$$ExternalSyntheticLambda0
                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return ((ClosingFuture) obj).future;
                }
            }).toList();
        }

        public <V> ClosingFuture<V> call(CombiningCallable<V> combiningCallable, Executor executor) {
            ClosingFuture<V> closingFuture = new ClosingFuture<>(futureCombiner().call(new Callable<V>(this, combiningCallable) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner.1
                final Combiner this$0;
                final CombiningCallable val$combiningCallable;

                {
                    this.this$0 = this;
                    this.val$combiningCallable = combiningCallable;
                }

                @Override // java.util.concurrent.Callable
                @ParametricNullness
                public V call() throws Exception {
                    return (V) new Peeker(this.this$0.inputs).call(this.val$combiningCallable, this.this$0.closeables);
                }

                public String toString() {
                    return this.val$combiningCallable.toString();
                }
            }, executor));
            ((ClosingFuture) closingFuture).closeables.add(this.closeables, MoreExecutors.directExecutor());
            return closingFuture;
        }

        public <V> ClosingFuture<V> callAsync(AsyncCombiningCallable<V> asyncCombiningCallable, Executor executor) {
            ClosingFuture<V> closingFuture = new ClosingFuture<>(futureCombiner().callAsync(new AsyncCallable<V>(this, asyncCombiningCallable) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner.2
                final Combiner this$0;
                final AsyncCombiningCallable val$combiningCallable;

                {
                    this.this$0 = this;
                    this.val$combiningCallable = asyncCombiningCallable;
                }

                @Override // com.google.common.util.concurrent.AsyncCallable
                public ListenableFuture<V> call() throws Exception {
                    return new Peeker(this.this$0.inputs).callAsync(this.val$combiningCallable, this.this$0.closeables);
                }

                public String toString() {
                    return this.val$combiningCallable.toString();
                }
            }, executor));
            ((ClosingFuture) closingFuture).closeables.add(this.closeables, MoreExecutors.directExecutor());
            return closingFuture;
        }
    }

    public static final class Combiner2<V1, V2> extends Combiner {
        private final ClosingFuture<V1> future1;
        private final ClosingFuture<V2> future2;

        public interface AsyncClosingFunction2<V1, V2, U> {
            ClosingFuture<U> apply(DeferredCloser deferredCloser, @ParametricNullness V1 v1, @ParametricNullness V2 v2) throws Exception;
        }

        public interface ClosingFunction2<V1, V2, U> {
            @ParametricNullness
            U apply(DeferredCloser deferredCloser, @ParametricNullness V1 v1, @ParametricNullness V2 v2) throws Exception;
        }

        private Combiner2(ClosingFuture<V1> closingFuture, ClosingFuture<V2> closingFuture2) {
            super(true, ImmutableList.of((ClosingFuture<V2>) closingFuture, closingFuture2));
            this.future1 = closingFuture;
            this.future2 = closingFuture2;
        }

        public <U> ClosingFuture<U> call(ClosingFunction2<V1, V2, U> closingFunction2, Executor executor) {
            return call(new Combiner.CombiningCallable<U>(this, closingFunction2) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner2.1
                final Combiner2 this$0;
                final ClosingFunction2 val$function;

                {
                    this.this$0 = this;
                    this.val$function = closingFunction2;
                }

                /* JADX WARN: Multi-variable type inference failed */
                @Override // com.google.common.util.concurrent.ClosingFuture.Combiner.CombiningCallable
                @ParametricNullness
                public U call(DeferredCloser deferredCloser, Peeker peeker) throws Exception {
                    return (U) this.val$function.apply(deferredCloser, peeker.getDone(this.this$0.future1), peeker.getDone(this.this$0.future2));
                }

                public String toString() {
                    return this.val$function.toString();
                }
            }, executor);
        }

        public <U> ClosingFuture<U> callAsync(AsyncClosingFunction2<V1, V2, U> asyncClosingFunction2, Executor executor) {
            return callAsync(new Combiner.AsyncCombiningCallable<U>(this, asyncClosingFunction2) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner2.2
                final Combiner2 this$0;
                final AsyncClosingFunction2 val$function;

                {
                    this.this$0 = this;
                    this.val$function = asyncClosingFunction2;
                }

                /* JADX WARN: Multi-variable type inference failed */
                @Override // com.google.common.util.concurrent.ClosingFuture.Combiner.AsyncCombiningCallable
                public ClosingFuture<U> call(DeferredCloser deferredCloser, Peeker peeker) throws Exception {
                    return this.val$function.apply(deferredCloser, peeker.getDone(this.this$0.future1), peeker.getDone(this.this$0.future2));
                }

                public String toString() {
                    return this.val$function.toString();
                }
            }, executor);
        }
    }

    public static final class Combiner3<V1, V2, V3> extends Combiner {
        private final ClosingFuture<V1> future1;
        private final ClosingFuture<V2> future2;
        private final ClosingFuture<V3> future3;

        public interface AsyncClosingFunction3<V1, V2, V3, U> {
            ClosingFuture<U> apply(DeferredCloser deferredCloser, @ParametricNullness V1 v1, @ParametricNullness V2 v2, @ParametricNullness V3 v3) throws Exception;
        }

        public interface ClosingFunction3<V1, V2, V3, U> {
            @ParametricNullness
            U apply(DeferredCloser deferredCloser, @ParametricNullness V1 v1, @ParametricNullness V2 v2, @ParametricNullness V3 v3) throws Exception;
        }

        private Combiner3(ClosingFuture<V1> closingFuture, ClosingFuture<V2> closingFuture2, ClosingFuture<V3> closingFuture3) {
            super(true, ImmutableList.of((ClosingFuture<V3>) closingFuture, (ClosingFuture<V3>) closingFuture2, closingFuture3));
            this.future1 = closingFuture;
            this.future2 = closingFuture2;
            this.future3 = closingFuture3;
        }

        public <U> ClosingFuture<U> call(ClosingFunction3<V1, V2, V3, U> closingFunction3, Executor executor) {
            return call(new Combiner.CombiningCallable<U>(this, closingFunction3) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner3.1
                final Combiner3 this$0;
                final ClosingFunction3 val$function;

                {
                    this.this$0 = this;
                    this.val$function = closingFunction3;
                }

                /* JADX WARN: Multi-variable type inference failed */
                @Override // com.google.common.util.concurrent.ClosingFuture.Combiner.CombiningCallable
                @ParametricNullness
                public U call(DeferredCloser deferredCloser, Peeker peeker) throws Exception {
                    return (U) this.val$function.apply(deferredCloser, peeker.getDone(this.this$0.future1), peeker.getDone(this.this$0.future2), peeker.getDone(this.this$0.future3));
                }

                public String toString() {
                    return this.val$function.toString();
                }
            }, executor);
        }

        public <U> ClosingFuture<U> callAsync(AsyncClosingFunction3<V1, V2, V3, U> asyncClosingFunction3, Executor executor) {
            return callAsync(new Combiner.AsyncCombiningCallable<U>(this, asyncClosingFunction3) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner3.2
                final Combiner3 this$0;
                final AsyncClosingFunction3 val$function;

                {
                    this.this$0 = this;
                    this.val$function = asyncClosingFunction3;
                }

                /* JADX WARN: Multi-variable type inference failed */
                @Override // com.google.common.util.concurrent.ClosingFuture.Combiner.AsyncCombiningCallable
                public ClosingFuture<U> call(DeferredCloser deferredCloser, Peeker peeker) throws Exception {
                    return this.val$function.apply(deferredCloser, peeker.getDone(this.this$0.future1), peeker.getDone(this.this$0.future2), peeker.getDone(this.this$0.future3));
                }

                public String toString() {
                    return this.val$function.toString();
                }
            }, executor);
        }
    }

    public static final class Combiner4<V1, V2, V3, V4> extends Combiner {
        private final ClosingFuture<V1> future1;
        private final ClosingFuture<V2> future2;
        private final ClosingFuture<V3> future3;
        private final ClosingFuture<V4> future4;

        public interface AsyncClosingFunction4<V1, V2, V3, V4, U> {
            ClosingFuture<U> apply(DeferredCloser deferredCloser, @ParametricNullness V1 v1, @ParametricNullness V2 v2, @ParametricNullness V3 v3, @ParametricNullness V4 v4) throws Exception;
        }

        public interface ClosingFunction4<V1, V2, V3, V4, U> {
            @ParametricNullness
            U apply(DeferredCloser deferredCloser, @ParametricNullness V1 v1, @ParametricNullness V2 v2, @ParametricNullness V3 v3, @ParametricNullness V4 v4) throws Exception;
        }

        private Combiner4(ClosingFuture<V1> closingFuture, ClosingFuture<V2> closingFuture2, ClosingFuture<V3> closingFuture3, ClosingFuture<V4> closingFuture4) {
            super(true, ImmutableList.of((ClosingFuture<V4>) closingFuture, (ClosingFuture<V4>) closingFuture2, (ClosingFuture<V4>) closingFuture3, closingFuture4));
            this.future1 = closingFuture;
            this.future2 = closingFuture2;
            this.future3 = closingFuture3;
            this.future4 = closingFuture4;
        }

        public <U> ClosingFuture<U> call(ClosingFunction4<V1, V2, V3, V4, U> closingFunction4, Executor executor) {
            return call(new Combiner.CombiningCallable<U>(this, closingFunction4) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner4.1
                final Combiner4 this$0;
                final ClosingFunction4 val$function;

                {
                    this.this$0 = this;
                    this.val$function = closingFunction4;
                }

                /* JADX WARN: Multi-variable type inference failed */
                @Override // com.google.common.util.concurrent.ClosingFuture.Combiner.CombiningCallable
                @ParametricNullness
                public U call(DeferredCloser deferredCloser, Peeker peeker) throws Exception {
                    return (U) this.val$function.apply(deferredCloser, peeker.getDone(this.this$0.future1), peeker.getDone(this.this$0.future2), peeker.getDone(this.this$0.future3), peeker.getDone(this.this$0.future4));
                }

                public String toString() {
                    return this.val$function.toString();
                }
            }, executor);
        }

        public <U> ClosingFuture<U> callAsync(AsyncClosingFunction4<V1, V2, V3, V4, U> asyncClosingFunction4, Executor executor) {
            return callAsync(new Combiner.AsyncCombiningCallable<U>(this, asyncClosingFunction4) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner4.2
                final Combiner4 this$0;
                final AsyncClosingFunction4 val$function;

                {
                    this.this$0 = this;
                    this.val$function = asyncClosingFunction4;
                }

                /* JADX WARN: Multi-variable type inference failed */
                @Override // com.google.common.util.concurrent.ClosingFuture.Combiner.AsyncCombiningCallable
                public ClosingFuture<U> call(DeferredCloser deferredCloser, Peeker peeker) throws Exception {
                    return this.val$function.apply(deferredCloser, peeker.getDone(this.this$0.future1), peeker.getDone(this.this$0.future2), peeker.getDone(this.this$0.future3), peeker.getDone(this.this$0.future4));
                }

                public String toString() {
                    return this.val$function.toString();
                }
            }, executor);
        }
    }

    public static final class Combiner5<V1, V2, V3, V4, V5> extends Combiner {
        private final ClosingFuture<V1> future1;
        private final ClosingFuture<V2> future2;
        private final ClosingFuture<V3> future3;
        private final ClosingFuture<V4> future4;
        private final ClosingFuture<V5> future5;

        public interface AsyncClosingFunction5<V1, V2, V3, V4, V5, U> {
            ClosingFuture<U> apply(DeferredCloser deferredCloser, @ParametricNullness V1 v1, @ParametricNullness V2 v2, @ParametricNullness V3 v3, @ParametricNullness V4 v4, @ParametricNullness V5 v5) throws Exception;
        }

        public interface ClosingFunction5<V1, V2, V3, V4, V5, U> {
            @ParametricNullness
            U apply(DeferredCloser deferredCloser, @ParametricNullness V1 v1, @ParametricNullness V2 v2, @ParametricNullness V3 v3, @ParametricNullness V4 v4, @ParametricNullness V5 v5) throws Exception;
        }

        private Combiner5(ClosingFuture<V1> closingFuture, ClosingFuture<V2> closingFuture2, ClosingFuture<V3> closingFuture3, ClosingFuture<V4> closingFuture4, ClosingFuture<V5> closingFuture5) {
            super(true, ImmutableList.of((ClosingFuture<V5>) closingFuture, (ClosingFuture<V5>) closingFuture2, (ClosingFuture<V5>) closingFuture3, (ClosingFuture<V5>) closingFuture4, closingFuture5));
            this.future1 = closingFuture;
            this.future2 = closingFuture2;
            this.future3 = closingFuture3;
            this.future4 = closingFuture4;
            this.future5 = closingFuture5;
        }

        public <U> ClosingFuture<U> call(ClosingFunction5<V1, V2, V3, V4, V5, U> closingFunction5, Executor executor) {
            return call(new Combiner.CombiningCallable<U>(this, closingFunction5) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner5.1
                final Combiner5 this$0;
                final ClosingFunction5 val$function;

                {
                    this.this$0 = this;
                    this.val$function = closingFunction5;
                }

                /* JADX WARN: Multi-variable type inference failed */
                @Override // com.google.common.util.concurrent.ClosingFuture.Combiner.CombiningCallable
                @ParametricNullness
                public U call(DeferredCloser deferredCloser, Peeker peeker) throws Exception {
                    return (U) this.val$function.apply(deferredCloser, peeker.getDone(this.this$0.future1), peeker.getDone(this.this$0.future2), peeker.getDone(this.this$0.future3), peeker.getDone(this.this$0.future4), peeker.getDone(this.this$0.future5));
                }

                public String toString() {
                    return this.val$function.toString();
                }
            }, executor);
        }

        public <U> ClosingFuture<U> callAsync(AsyncClosingFunction5<V1, V2, V3, V4, V5, U> asyncClosingFunction5, Executor executor) {
            return callAsync(new Combiner.AsyncCombiningCallable<U>(this, asyncClosingFunction5) { // from class: com.google.common.util.concurrent.ClosingFuture.Combiner5.2
                final Combiner5 this$0;
                final AsyncClosingFunction5 val$function;

                {
                    this.this$0 = this;
                    this.val$function = asyncClosingFunction5;
                }

                /* JADX WARN: Multi-variable type inference failed */
                @Override // com.google.common.util.concurrent.ClosingFuture.Combiner.AsyncCombiningCallable
                public ClosingFuture<U> call(DeferredCloser deferredCloser, Peeker peeker) throws Exception {
                    return this.val$function.apply(deferredCloser, peeker.getDone(this.this$0.future1), peeker.getDone(this.this$0.future2), peeker.getDone(this.this$0.future3), peeker.getDone(this.this$0.future4), peeker.getDone(this.this$0.future5));
                }

                public String toString() {
                    return this.val$function.toString();
                }
            }, executor);
        }
    }

    public static final class DeferredCloser {
        private final CloseableList list;

        DeferredCloser(CloseableList closeableList) {
            this.list = closeableList;
        }

        @ParametricNullness
        public <C extends Closeable> C eventuallyClose(@ParametricNullness C c, Executor executor) {
            Preconditions.checkNotNull(executor);
            if (c != null) {
                this.list.add(c, executor);
            }
            return c;
        }
    }

    public static final class Peeker {
        private volatile boolean beingCalled;
        private final ImmutableList<ClosingFuture<?>> futures;

        private Peeker(ImmutableList<ClosingFuture<?>> immutableList) {
            this.futures = (ImmutableList) Preconditions.checkNotNull(immutableList);
        }

        /* JADX INFO: Access modifiers changed from: private */
        @ParametricNullness
        public <V> V call(Combiner.CombiningCallable<V> combiningCallable, CloseableList closeableList) throws Exception {
            this.beingCalled = true;
            CloseableList closeableList2 = new CloseableList();
            try {
                return combiningCallable.call(closeableList2.closer, this);
            } finally {
                closeableList.add(closeableList2, MoreExecutors.directExecutor());
                this.beingCalled = false;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public <V> FluentFuture<V> callAsync(Combiner.AsyncCombiningCallable<V> asyncCombiningCallable, CloseableList closeableList) throws Exception {
            this.beingCalled = true;
            CloseableList closeableList2 = new CloseableList();
            try {
                ClosingFuture<V> closingFutureCall = asyncCombiningCallable.call(closeableList2.closer, this);
                closingFutureCall.becomeSubsumedInto(closeableList);
                return ((ClosingFuture) closingFutureCall).future;
            } finally {
                closeableList.add(closeableList2, MoreExecutors.directExecutor());
                this.beingCalled = false;
            }
        }

        @ParametricNullness
        public final <D> D getDone(ClosingFuture<D> closingFuture) throws ExecutionException {
            Preconditions.checkState(this.beingCalled);
            Preconditions.checkArgument(this.futures.contains(closingFuture));
            return (D) Futures.getDone(((ClosingFuture) closingFuture).future);
        }
    }

    enum State {
        OPEN,
        SUBSUMED,
        WILL_CLOSE,
        CLOSING,
        CLOSED,
        WILL_CREATE_VALUE_AND_CLOSER
    }

    public static final class ValueAndCloser<V> {
        private final ClosingFuture<? extends V> closingFuture;

        ValueAndCloser(ClosingFuture<? extends V> closingFuture) {
            this.closingFuture = (ClosingFuture) Preconditions.checkNotNull(closingFuture);
        }

        public void closeAsync() {
            this.closingFuture.close();
        }

        @ParametricNullness
        public V get() throws ExecutionException {
            return (V) Futures.getDone(((ClosingFuture) this.closingFuture).future);
        }
    }

    public interface ValueAndCloserConsumer<V> {
        void accept(ValueAndCloser<V> valueAndCloser);
    }

    private ClosingFuture(AsyncClosingCallable<V> asyncClosingCallable, Executor executor) {
        this.state = new AtomicReference<>(State.OPEN);
        this.closeables = new CloseableList();
        Preconditions.checkNotNull(asyncClosingCallable);
        TrustedListenableFutureTask trustedListenableFutureTaskCreate = TrustedListenableFutureTask.create(new AsyncCallable<V>(this, asyncClosingCallable) { // from class: com.google.common.util.concurrent.ClosingFuture.3
            final ClosingFuture this$0;
            final AsyncClosingCallable val$callable;

            {
                this.this$0 = this;
                this.val$callable = asyncClosingCallable;
            }

            @Override // com.google.common.util.concurrent.AsyncCallable
            public ListenableFuture<V> call() throws Exception {
                CloseableList closeableList = new CloseableList();
                try {
                    ClosingFuture<V> closingFutureCall = this.val$callable.call(closeableList.closer);
                    closingFutureCall.becomeSubsumedInto(this.this$0.closeables);
                    return ((ClosingFuture) closingFutureCall).future;
                } finally {
                    this.this$0.closeables.add(closeableList, MoreExecutors.directExecutor());
                }
            }

            public String toString() {
                return this.val$callable.toString();
            }
        });
        executor.execute(trustedListenableFutureTaskCreate);
        this.future = trustedListenableFutureTaskCreate;
    }

    private ClosingFuture(ClosingCallable<V> closingCallable, Executor executor) {
        this.state = new AtomicReference<>(State.OPEN);
        this.closeables = new CloseableList();
        Preconditions.checkNotNull(closingCallable);
        TrustedListenableFutureTask trustedListenableFutureTaskCreate = TrustedListenableFutureTask.create(new Callable<V>(this, closingCallable) { // from class: com.google.common.util.concurrent.ClosingFuture.2
            final ClosingFuture this$0;
            final ClosingCallable val$callable;

            {
                this.this$0 = this;
                this.val$callable = closingCallable;
            }

            @Override // java.util.concurrent.Callable
            @ParametricNullness
            public V call() throws Exception {
                return (V) this.val$callable.call(this.this$0.closeables.closer);
            }

            public String toString() {
                return this.val$callable.toString();
            }
        });
        executor.execute(trustedListenableFutureTaskCreate);
        this.future = trustedListenableFutureTaskCreate;
    }

    private ClosingFuture(ListenableFuture<V> listenableFuture) {
        this.state = new AtomicReference<>(State.OPEN);
        this.closeables = new CloseableList();
        this.future = FluentFuture.from(listenableFuture);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void becomeSubsumedInto(CloseableList closeableList) {
        checkAndUpdateState(State.OPEN, State.SUBSUMED);
        closeableList.add(this.closeables, MoreExecutors.directExecutor());
    }

    /* JADX WARN: Multi-variable type inference failed */
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
    private <X extends Throwable, W extends V> ClosingFuture<V> catchingAsyncMoreGeneric(Class<X> cls, AsyncClosingFunction<? super X, W> asyncClosingFunction, Executor executor) {
        Preconditions.checkNotNull(asyncClosingFunction);
        return (ClosingFuture<V>) derive(this.future.catchingAsync(cls, new AsyncFunction<X, W>(this, asyncClosingFunction) { // from class: com.google.common.util.concurrent.ClosingFuture.8
            final ClosingFuture this$0;
            final AsyncClosingFunction val$fallback;

            {
                this.this$0 = this;
                this.val$fallback = asyncClosingFunction;
            }

            /* JADX WARN: Incorrect types in method signature: (TX;)Lcom/google/common/util/concurrent/ListenableFuture<TW;>; */
            @Override // com.google.common.util.concurrent.AsyncFunction
            public ListenableFuture apply(Throwable th) throws Exception {
                return this.this$0.closeables.applyAsyncClosingFunction(this.val$fallback, th);
            }

            public String toString() {
                return this.val$fallback.toString();
            }
        }, executor));
    }

    /* JADX WARN: Multi-variable type inference failed */
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
    private <X extends Throwable, W extends V> ClosingFuture<V> catchingMoreGeneric(Class<X> cls, ClosingFunction<? super X, W> closingFunction, Executor executor) {
        Preconditions.checkNotNull(closingFunction);
        return (ClosingFuture<V>) derive(this.future.catchingAsync(cls, new AsyncFunction<X, W>(this, closingFunction) { // from class: com.google.common.util.concurrent.ClosingFuture.7
            final ClosingFuture this$0;
            final ClosingFunction val$fallback;

            {
                this.this$0 = this;
                this.val$fallback = closingFunction;
            }

            /* JADX WARN: Incorrect types in method signature: (TX;)Lcom/google/common/util/concurrent/ListenableFuture<TW;>; */
            @Override // com.google.common.util.concurrent.AsyncFunction
            public ListenableFuture apply(Throwable th) throws Exception {
                return this.this$0.closeables.applyClosingFunction(this.val$fallback, th);
            }

            public String toString() {
                return this.val$fallback.toString();
            }
        }, executor));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndUpdateState(State state, State state2) {
        Preconditions.checkState(compareAndUpdateState(state, state2), "Expected state to be %s, but it was %s", state, state2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void close() {
        logger.get().log(Level.FINER, "closing {0}", this);
        this.closeables.close();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void closeQuietly(@CheckForNull final Closeable closeable, Executor executor) {
        if (closeable == null) {
            return;
        }
        try {
            executor.execute(new Runnable(closeable) { // from class: com.google.common.util.concurrent.ClosingFuture$$ExternalSyntheticLambda1
                public final Closeable f$0;

                {
                    this.f$0 = closeable;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    ClosingFuture.lambda$closeQuietly$0(this.f$0);
                }
            });
        } catch (RejectedExecutionException e) {
            if (logger.get().isLoggable(Level.WARNING)) {
                logger.get().log(Level.WARNING, String.format("while submitting close to %s; will close inline", executor), (Throwable) e);
            }
            closeQuietly(closeable, MoreExecutors.directExecutor());
        }
    }

    private boolean compareAndUpdateState(State state, State state2) {
        return AsynchronousMediaCodecBufferEnqueuer$$ExternalSyntheticBackportWithForwarding0.m(this.state, state, state2);
    }

    private <U> ClosingFuture<U> derive(FluentFuture<U> fluentFuture) {
        ClosingFuture<U> closingFuture = new ClosingFuture<>(fluentFuture);
        becomeSubsumedInto(closingFuture.closeables);
        return closingFuture;
    }

    @Deprecated
    public static <C extends Closeable> ClosingFuture<C> eventuallyClosing(ListenableFuture<C> listenableFuture, Executor executor) {
        Preconditions.checkNotNull(executor);
        ClosingFuture<C> closingFuture = new ClosingFuture<>(Futures.nonCancellationPropagating(listenableFuture));
        Futures.addCallback(listenableFuture, new FutureCallback<Closeable>(closingFuture, executor) { // from class: com.google.common.util.concurrent.ClosingFuture.1
            final Executor val$closingExecutor;
            final ClosingFuture val$closingFuture;

            {
                this.val$closingFuture = closingFuture;
                this.val$closingExecutor = executor;
            }

            @Override // com.google.common.util.concurrent.FutureCallback
            public void onFailure(Throwable th) {
            }

            @Override // com.google.common.util.concurrent.FutureCallback
            public void onSuccess(@CheckForNull Closeable closeable) {
                this.val$closingFuture.closeables.closer.eventuallyClose(closeable, this.val$closingExecutor);
            }
        }, MoreExecutors.directExecutor());
        return closingFuture;
    }

    public static <V> ClosingFuture<V> from(ListenableFuture<V> listenableFuture) {
        return new ClosingFuture<>(listenableFuture);
    }

    static /* synthetic */ void lambda$closeQuietly$0(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            Platform.restoreInterruptIfIsInterruptedException(e);
            logger.get().log(Level.WARNING, "thrown by close()", (Throwable) e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <C, V extends C> void provideValueAndCloser(ValueAndCloserConsumer<C> valueAndCloserConsumer, ClosingFuture<V> closingFuture) {
        valueAndCloserConsumer.accept(new ValueAndCloser<>(closingFuture));
    }

    public static <V> ClosingFuture<V> submit(ClosingCallable<V> closingCallable, Executor executor) {
        return new ClosingFuture<>(closingCallable, executor);
    }

    public static <V> ClosingFuture<V> submitAsync(AsyncClosingCallable<V> asyncClosingCallable, Executor executor) {
        return new ClosingFuture<>(asyncClosingCallable, executor);
    }

    public static Combiner whenAllComplete(ClosingFuture<?> closingFuture, ClosingFuture<?>... closingFutureArr) {
        return whenAllComplete(Lists.asList(closingFuture, closingFutureArr));
    }

    public static Combiner whenAllComplete(Iterable<? extends ClosingFuture<?>> iterable) {
        return new Combiner(false, iterable);
    }

    public static <V1, V2> Combiner2<V1, V2> whenAllSucceed(ClosingFuture<V1> closingFuture, ClosingFuture<V2> closingFuture2) {
        return new Combiner2<>(closingFuture2);
    }

    public static <V1, V2, V3> Combiner3<V1, V2, V3> whenAllSucceed(ClosingFuture<V1> closingFuture, ClosingFuture<V2> closingFuture2, ClosingFuture<V3> closingFuture3) {
        return new Combiner3<>(closingFuture2, closingFuture3);
    }

    public static <V1, V2, V3, V4> Combiner4<V1, V2, V3, V4> whenAllSucceed(ClosingFuture<V1> closingFuture, ClosingFuture<V2> closingFuture2, ClosingFuture<V3> closingFuture3, ClosingFuture<V4> closingFuture4) {
        return new Combiner4<>(closingFuture2, closingFuture3, closingFuture4);
    }

    public static <V1, V2, V3, V4, V5> Combiner5<V1, V2, V3, V4, V5> whenAllSucceed(ClosingFuture<V1> closingFuture, ClosingFuture<V2> closingFuture2, ClosingFuture<V3> closingFuture3, ClosingFuture<V4> closingFuture4, ClosingFuture<V5> closingFuture5) {
        return new Combiner5<>(closingFuture2, closingFuture3, closingFuture4, closingFuture5);
    }

    public static Combiner whenAllSucceed(ClosingFuture<?> closingFuture, ClosingFuture<?> closingFuture2, ClosingFuture<?> closingFuture3, ClosingFuture<?> closingFuture4, ClosingFuture<?> closingFuture5, ClosingFuture<?> closingFuture6, ClosingFuture<?>... closingFutureArr) {
        return whenAllSucceed(FluentIterable.of(closingFuture, closingFuture2, closingFuture3, closingFuture4, closingFuture5, closingFuture6).append(closingFutureArr));
    }

    public static Combiner whenAllSucceed(Iterable<? extends ClosingFuture<?>> iterable) {
        return new Combiner(true, iterable);
    }

    public static <V, U> AsyncClosingFunction<V, U> withoutCloser(AsyncFunction<V, U> asyncFunction) {
        Preconditions.checkNotNull(asyncFunction);
        return new AsyncClosingFunction<V, U>(asyncFunction) { // from class: com.google.common.util.concurrent.ClosingFuture.6
            final AsyncFunction val$function;

            {
                this.val$function = asyncFunction;
            }

            @Override // com.google.common.util.concurrent.ClosingFuture.AsyncClosingFunction
            public ClosingFuture<U> apply(DeferredCloser deferredCloser, V v) throws Exception {
                return ClosingFuture.from(this.val$function.apply(v));
            }
        };
    }

    public boolean cancel(boolean z) {
        logger.get().log(Level.FINER, "cancelling {0}", this);
        boolean zCancel = this.future.cancel(z);
        if (zCancel) {
            close();
        }
        return zCancel;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public <X extends Throwable> ClosingFuture<V> catching(Class<X> cls, ClosingFunction<? super X, ? extends V> closingFunction, Executor executor) {
        return catchingMoreGeneric(cls, closingFunction, executor);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public <X extends Throwable> ClosingFuture<V> catchingAsync(Class<X> cls, AsyncClosingFunction<? super X, ? extends V> asyncClosingFunction, Executor executor) {
        return catchingAsyncMoreGeneric(cls, asyncClosingFunction, executor);
    }

    protected void finalize() {
        if (this.state.get().equals(State.OPEN)) {
            logger.get().log(Level.SEVERE, "Uh oh! An open ClosingFuture has leaked and will close: {0}", this);
            finishToFuture();
        }
    }

    public FluentFuture<V> finishToFuture() {
        if (!compareAndUpdateState(State.OPEN, State.WILL_CLOSE)) {
            switch (AnonymousClass11.$SwitchMap$com$google$common$util$concurrent$ClosingFuture$State[this.state.get().ordinal()]) {
                case 1:
                    throw new IllegalStateException("Cannot call finishToFuture() after deriving another step");
                case 2:
                    throw new IllegalStateException("Cannot call finishToFuture() after calling finishToValueAndCloser()");
                case 3:
                case 4:
                case 5:
                    throw new IllegalStateException("Cannot call finishToFuture() twice");
                case 6:
                    throw new AssertionError();
            }
        }
        logger.get().log(Level.FINER, "will close {0}", this);
        this.future.addListener(new Runnable(this) { // from class: com.google.common.util.concurrent.ClosingFuture.9
            final ClosingFuture this$0;

            {
                this.this$0 = this;
            }

            @Override // java.lang.Runnable
            public void run() {
                this.this$0.checkAndUpdateState(State.WILL_CLOSE, State.CLOSING);
                this.this$0.close();
                this.this$0.checkAndUpdateState(State.CLOSING, State.CLOSED);
            }
        }, MoreExecutors.directExecutor());
        return this.future;
    }

    public void finishToValueAndCloser(ValueAndCloserConsumer<? super V> valueAndCloserConsumer, Executor executor) {
        Preconditions.checkNotNull(valueAndCloserConsumer);
        if (compareAndUpdateState(State.OPEN, State.WILL_CREATE_VALUE_AND_CLOSER)) {
            this.future.addListener(new Runnable(this, valueAndCloserConsumer) { // from class: com.google.common.util.concurrent.ClosingFuture.10
                final ClosingFuture this$0;
                final ValueAndCloserConsumer val$consumer;

                {
                    this.this$0 = this;
                    this.val$consumer = valueAndCloserConsumer;
                }

                @Override // java.lang.Runnable
                public void run() {
                    ClosingFuture.provideValueAndCloser(this.val$consumer, this.this$0);
                }
            }, executor);
            return;
        }
        switch (AnonymousClass11.$SwitchMap$com$google$common$util$concurrent$ClosingFuture$State[this.state.get().ordinal()]) {
            case 1:
                throw new IllegalStateException("Cannot call finishToValueAndCloser() after deriving another step");
            case 2:
                throw new IllegalStateException("Cannot call finishToValueAndCloser() twice");
            case 3:
            case 4:
            case 5:
                throw new IllegalStateException("Cannot call finishToValueAndCloser() after calling finishToFuture()");
            default:
                throw new AssertionError(this.state);
        }
    }

    public ListenableFuture<?> statusFuture() {
        return Futures.nonCancellationPropagating(this.future.transform(Functions.constant(null), MoreExecutors.directExecutor()));
    }

    public String toString() {
        return MoreObjects.toStringHelper(this).add("state", this.state.get()).addValue(this.future).toString();
    }

    public <U> ClosingFuture<U> transform(ClosingFunction<? super V, U> closingFunction, Executor executor) {
        Preconditions.checkNotNull(closingFunction);
        return derive(this.future.transformAsync(new AsyncFunction<V, U>(this, closingFunction) { // from class: com.google.common.util.concurrent.ClosingFuture.4
            final ClosingFuture this$0;
            final ClosingFunction val$function;

            {
                this.this$0 = this;
                this.val$function = closingFunction;
            }

            @Override // com.google.common.util.concurrent.AsyncFunction
            public ListenableFuture<U> apply(V v) throws Exception {
                return this.this$0.closeables.applyClosingFunction(this.val$function, v);
            }

            public String toString() {
                return this.val$function.toString();
            }
        }, executor));
    }

    public <U> ClosingFuture<U> transformAsync(AsyncClosingFunction<? super V, U> asyncClosingFunction, Executor executor) {
        Preconditions.checkNotNull(asyncClosingFunction);
        return derive(this.future.transformAsync(new AsyncFunction<V, U>(this, asyncClosingFunction) { // from class: com.google.common.util.concurrent.ClosingFuture.5
            final ClosingFuture this$0;
            final AsyncClosingFunction val$function;

            {
                this.this$0 = this;
                this.val$function = asyncClosingFunction;
            }

            @Override // com.google.common.util.concurrent.AsyncFunction
            public ListenableFuture<U> apply(V v) throws Exception {
                return this.this$0.closeables.applyAsyncClosingFunction(this.val$function, v);
            }

            public String toString() {
                return this.val$function.toString();
            }
        }, executor));
    }

    CountDownLatch whenClosedCountDown() {
        return this.closeables.whenClosedCountDown();
    }
}
