package com.google.common.util.concurrent;

import java.util.concurrent.ScheduledFuture;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface ListenableScheduledFuture<V> extends ScheduledFuture<V>, ListenableFuture<V> {
}
