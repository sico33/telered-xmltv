package android.support.v4.media.session;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public interface IMediaControllerCallback extends IInterface {
    public static final String DESCRIPTOR = "android.support.v4.media.session.IMediaControllerCallback";

    void onCaptioningEnabledChanged(boolean z) throws RemoteException;

    void onEvent(String str, Bundle bundle) throws RemoteException;

    void onExtrasChanged(Bundle bundle) throws RemoteException;

    void onMetadataChanged(MediaMetadataCompat mediaMetadataCompat) throws RemoteException;

    void onPlaybackStateChanged(PlaybackStateCompat playbackStateCompat) throws RemoteException;

    void onQueueChanged(List<MediaSessionCompat.QueueItem> list) throws RemoteException;

    void onQueueTitleChanged(CharSequence charSequence) throws RemoteException;

    void onRepeatModeChanged(int i) throws RemoteException;

    void onSessionDestroyed() throws RemoteException;

    void onSessionReady() throws RemoteException;

    void onShuffleModeChanged(int i) throws RemoteException;

    void onShuffleModeChangedRemoved(boolean z) throws RemoteException;

    void onVolumeInfoChanged(ParcelableVolumeInfo parcelableVolumeInfo) throws RemoteException;

    public static class Default implements IMediaControllerCallback {
        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onEvent(String event, Bundle extras) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onSessionDestroyed() throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onPlaybackStateChanged(PlaybackStateCompat state) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onMetadataChanged(MediaMetadataCompat metadata) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onQueueTitleChanged(CharSequence title) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onExtrasChanged(Bundle extras) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onVolumeInfoChanged(ParcelableVolumeInfo info) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onRepeatModeChanged(int repeatMode) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onShuffleModeChangedRemoved(boolean enabled) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onCaptioningEnabledChanged(boolean enabled) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onShuffleModeChanged(int shuffleMode) throws RemoteException {
        }

        @Override // android.support.v4.media.session.IMediaControllerCallback
        public void onSessionReady() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements IMediaControllerCallback {
        static final int TRANSACTION_onCaptioningEnabledChanged = 11;
        static final int TRANSACTION_onEvent = 1;
        static final int TRANSACTION_onExtrasChanged = 7;
        static final int TRANSACTION_onMetadataChanged = 4;
        static final int TRANSACTION_onPlaybackStateChanged = 3;
        static final int TRANSACTION_onQueueChanged = 5;
        static final int TRANSACTION_onQueueTitleChanged = 6;
        static final int TRANSACTION_onRepeatModeChanged = 9;
        static final int TRANSACTION_onSessionDestroyed = 2;
        static final int TRANSACTION_onSessionReady = 13;
        static final int TRANSACTION_onShuffleModeChanged = 12;
        static final int TRANSACTION_onShuffleModeChangedRemoved = 10;
        static final int TRANSACTION_onVolumeInfoChanged = 8;

        public Stub() {
            attachInterface(this, IMediaControllerCallback.DESCRIPTOR);
        }

        public static IMediaControllerCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IMediaControllerCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IMediaControllerCallback)) {
                return (IMediaControllerCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IMediaControllerCallback.DESCRIPTOR);
            }
            if (code == 1598968902) {
                reply.writeString(IMediaControllerCallback.DESCRIPTOR);
                return true;
            }
            boolean _arg0 = false;
            switch (code) {
                case 1:
                    String _arg1 = data.readString();
                    Bundle _arg2 = (Bundle) _Parcel.readTypedObject(data, Bundle.CREATOR);
                    onEvent(_arg1, _arg2);
                    return true;
                case 2:
                    onSessionDestroyed();
                    return true;
                case 3:
                    onPlaybackStateChanged((PlaybackStateCompat) _Parcel.readTypedObject(data, PlaybackStateCompat.CREATOR));
                    return true;
                case 4:
                    onMetadataChanged((MediaMetadataCompat) _Parcel.readTypedObject(data, MediaMetadataCompat.CREATOR));
                    return true;
                case 5:
                    onQueueChanged(data.createTypedArrayList(MediaSessionCompat.QueueItem.CREATOR));
                    return true;
                case 6:
                    onQueueTitleChanged((CharSequence) _Parcel.readTypedObject(data, TextUtils.CHAR_SEQUENCE_CREATOR));
                    return true;
                case 7:
                    onExtrasChanged((Bundle) _Parcel.readTypedObject(data, Bundle.CREATOR));
                    return true;
                case 8:
                    onVolumeInfoChanged((ParcelableVolumeInfo) _Parcel.readTypedObject(data, ParcelableVolumeInfo.CREATOR));
                    return true;
                case 9:
                    onRepeatModeChanged(data.readInt());
                    return true;
                case 10:
                    if (data.readInt() != 0) {
                        _arg0 = true;
                    }
                    onShuffleModeChangedRemoved(_arg0);
                    return true;
                case 11:
                    if (data.readInt() != 0) {
                        _arg0 = true;
                    }
                    onCaptioningEnabledChanged(_arg0);
                    return true;
                case 12:
                    onShuffleModeChanged(data.readInt());
                    return true;
                case 13:
                    onSessionReady();
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IMediaControllerCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IMediaControllerCallback.DESCRIPTOR;
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onEvent(String event, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _data.writeString(event);
                    _Parcel.writeTypedObject(_data, extras, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onSessionDestroyed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onPlaybackStateChanged(PlaybackStateCompat state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _Parcel.writeTypedObject(_data, state, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onMetadataChanged(MediaMetadataCompat metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _Parcel.writeTypedObject(_data, metadata, 0);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _Parcel.writeTypedList(_data, queue, 0);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onQueueTitleChanged(CharSequence title) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    if (title != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(title, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onExtrasChanged(Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _Parcel.writeTypedObject(_data, extras, 0);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onVolumeInfoChanged(ParcelableVolumeInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _Parcel.writeTypedObject(_data, info, 0);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onRepeatModeChanged(int repeatMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _data.writeInt(repeatMode);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onShuffleModeChangedRemoved(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onCaptioningEnabledChanged(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onShuffleModeChanged(int shuffleMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    _data.writeInt(shuffleMode);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.session.IMediaControllerCallback
            public void onSessionReady() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaControllerCallback.DESCRIPTOR);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }

    public static class _Parcel {
        /* JADX INFO: Access modifiers changed from: private */
        public static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> c) {
            if (parcel.readInt() != 0) {
                return c.createFromParcel(parcel);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedObject(Parcel parcel, T value, int parcelableFlags) {
            if (value != null) {
                parcel.writeInt(1);
                value.writeToParcel(parcel, parcelableFlags);
            } else {
                parcel.writeInt(0);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedList(Parcel parcel, List<T> value, int parcelableFlags) {
            if (value == null) {
                parcel.writeInt(-1);
                return;
            }
            int N = value.size();
            parcel.writeInt(N);
            for (int i = 0; i < N; i++) {
                writeTypedObject(parcel, value.get(i), parcelableFlags);
            }
        }
    }
}
