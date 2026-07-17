package androidx.media3.datasource;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public abstract class BaseDataSource implements DataSource {
    private DataSpec dataSpec;
    private final boolean isNetwork;
    private int listenerCount;
    private final ArrayList<TransferListener> listeners = new ArrayList<>(1);

    @Override // androidx.media3.datasource.DataSource
    public /* synthetic */ Map getResponseHeaders() {
        return Collections.emptyMap();
    }

    protected BaseDataSource(boolean isNetwork) {
        this.isNetwork = isNetwork;
    }

    @Override // androidx.media3.datasource.DataSource
    public final void addTransferListener(TransferListener transferListener) {
        Assertions.checkNotNull(transferListener);
        if (!this.listeners.contains(transferListener)) {
            this.listeners.add(transferListener);
            this.listenerCount++;
        }
    }

    protected final void transferInitializing(DataSpec dataSpec) {
        for (int i = 0; i < this.listenerCount; i++) {
            this.listeners.get(i).onTransferInitializing(this, dataSpec, this.isNetwork);
        }
    }

    protected final void transferStarted(DataSpec dataSpec) {
        this.dataSpec = dataSpec;
        for (int i = 0; i < this.listenerCount; i++) {
            this.listeners.get(i).onTransferStart(this, dataSpec, this.isNetwork);
        }
    }

    protected final void bytesTransferred(int bytesTransferred) {
        DataSpec dataSpec = (DataSpec) Util.castNonNull(this.dataSpec);
        for (int i = 0; i < this.listenerCount; i++) {
            this.listeners.get(i).onBytesTransferred(this, dataSpec, this.isNetwork, bytesTransferred);
        }
    }

    protected final void transferEnded() {
        DataSpec dataSpec = (DataSpec) Util.castNonNull(this.dataSpec);
        for (int i = 0; i < this.listenerCount; i++) {
            this.listeners.get(i).onTransferEnd(this, dataSpec, this.isNetwork);
        }
        this.dataSpec = null;
    }
}
