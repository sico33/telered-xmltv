package androidx.media3.datasource;

import androidx.media3.common.PriorityTaskManager;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public final class PriorityDataSourceFactory implements DataSource.Factory {
    private final int priority;
    private final PriorityTaskManager priorityTaskManager;
    private final DataSource.Factory upstreamFactory;

    public PriorityDataSourceFactory(DataSource.Factory upstreamFactory, PriorityTaskManager priorityTaskManager, int priority) {
        this.upstreamFactory = upstreamFactory;
        this.priorityTaskManager = priorityTaskManager;
        this.priority = priority;
    }

    @Override // androidx.media3.datasource.DataSource.Factory
    public PriorityDataSource createDataSource() {
        return new PriorityDataSource(this.upstreamFactory.createDataSource(), this.priorityTaskManager, this.priority);
    }
}
