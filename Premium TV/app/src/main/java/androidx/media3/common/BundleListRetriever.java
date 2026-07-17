package androidx.media3.common;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class BundleListRetriever extends Binder {
    private static final int REPLY_BREAK = 2;
    private static final int REPLY_CONTINUE = 1;
    private static final int REPLY_END_OF_LIST = 0;
    private static final int SUGGESTED_MAX_IPC_SIZE;
    private final ImmutableList<Bundle> list;

    static {
        SUGGESTED_MAX_IPC_SIZE = Util.SDK_INT >= 30 ? IBinder.getSuggestedMaxIpcSizeBytes() : 65536;
    }

    public BundleListRetriever(List<Bundle> list) {
        this.list = ImmutableList.copyOf((Collection) list);
    }

    @Override // android.os.Binder
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != 1) {
            return super.onTransact(code, data, reply, flags);
        }
        if (reply == null) {
            return false;
        }
        int count = this.list.size();
        int index = data.readInt();
        while (index < count && reply.dataSize() < SUGGESTED_MAX_IPC_SIZE) {
            reply.writeInt(1);
            reply.writeBundle(this.list.get(index));
            index++;
        }
        reply.writeInt(index < count ? 2 : 0);
        return true;
    }

    public static ImmutableList<Bundle> getList(IBinder binder) {
        if (binder instanceof BundleListRetriever) {
            return ((BundleListRetriever) binder).list;
        }
        return getListFromRemoteBinder(binder);
    }

    static ImmutableList<Bundle> getListFromRemoteBinder(IBinder binder) throws RemoteException {
        ImmutableList.Builder<Bundle> builder = ImmutableList.builder();
        int index = 0;
        int replyCode = 1;
        while (replyCode != 0) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInt(index);
                try {
                    binder.transact(1, data, reply, 0);
                    while (true) {
                        int i = reply.readInt();
                        replyCode = i;
                        if (i == 1) {
                            builder.add((Bundle) Assertions.checkNotNull(reply.readBundle()));
                            index++;
                        }
                    }
                    reply.recycle();
                    data.recycle();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            } catch (Throwable e2) {
                reply.recycle();
                data.recycle();
                throw e2;
            }
        }
        return builder.build();
    }
}
