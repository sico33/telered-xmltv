package com.google.common.cache;

import com.google.common.base.Preconditions;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class RemovalListeners {
    private RemovalListeners() {
    }

    public static <K, V> RemovalListener<K, V> asynchronous(final RemovalListener<K, V> removalListener, final Executor executor) {
        Preconditions.checkNotNull(removalListener);
        Preconditions.checkNotNull(executor);
        return new RemovalListener(executor, removalListener) { // from class: com.google.common.cache.RemovalListeners$$ExternalSyntheticLambda1
            public final Executor f$0;
            public final RemovalListener f$1;

            {
                this.f$0 = executor;
                this.f$1 = removalListener;
            }

            @Override // com.google.common.cache.RemovalListener
            public final void onRemoval(RemovalNotification removalNotification) {
                this.f$0.execute(new Runnable(this.f$1, removalNotification) { // from class: com.google.common.cache.RemovalListeners$$ExternalSyntheticLambda0
                    public final RemovalListener f$0;
                    public final RemovalNotification f$1;

                    {
                        this.f$0 = removalListener;
                        this.f$1 = removalNotification;
                    }

                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.onRemoval(this.f$1);
                    }
                });
            }
        };
    }
}
