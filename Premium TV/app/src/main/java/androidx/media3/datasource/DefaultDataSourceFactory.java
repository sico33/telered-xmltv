package androidx.media3.datasource;

import android.content.Context;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public final class DefaultDataSourceFactory implements DataSource.Factory {
    private final DataSource.Factory baseDataSourceFactory;
    private final Context context;
    private final TransferListener listener;

    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    public DefaultDataSourceFactory(Context context) {
        this(context, (String) null, (TransferListener) null);
    }

    public DefaultDataSourceFactory(Context context, String userAgent) {
        this(context, userAgent, (TransferListener) null);
    }

    public DefaultDataSourceFactory(Context context, String userAgent, TransferListener listener) {
        this(context, listener, new DefaultHttpDataSource.Factory().setUserAgent(userAgent));
    }

    public DefaultDataSourceFactory(Context context, DataSource.Factory baseDataSourceFactory) {
        this(context, (TransferListener) null, baseDataSourceFactory);
    }

    public DefaultDataSourceFactory(Context context, TransferListener listener, DataSource.Factory baseDataSourceFactory) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.baseDataSourceFactory = baseDataSourceFactory;
    }

    @Override // androidx.media3.datasource.DataSource.Factory
    public DefaultDataSource createDataSource() {
        DefaultDataSource dataSource = new DefaultDataSource(this.context, this.baseDataSourceFactory.createDataSource());
        if (this.listener != null) {
            dataSource.addTransferListener(this.listener);
        }
        return dataSource;
    }
}
