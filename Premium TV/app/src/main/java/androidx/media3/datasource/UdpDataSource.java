package androidx.media3.datasource;

import android.net.Uri;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Assertions;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

/* JADX INFO: loaded from: classes.dex */
public final class UdpDataSource extends BaseDataSource {
    public static final int DEFAULT_MAX_PACKET_SIZE = 2000;
    public static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 8000;
    public static final int UDP_PORT_UNSET = -1;
    private InetAddress address;
    private MulticastSocket multicastSocket;
    private boolean opened;
    private final DatagramPacket packet;
    private final byte[] packetBuffer;
    private int packetRemaining;
    private DatagramSocket socket;
    private final int socketTimeoutMillis;
    private Uri uri;

    public static final class UdpDataSourceException extends DataSourceException {
        public UdpDataSourceException(Throwable cause, int errorCode) {
            super(cause, errorCode);
        }
    }

    public UdpDataSource() {
        this(2000);
    }

    public UdpDataSource(int maxPacketSize) {
        this(maxPacketSize, 8000);
    }

    public UdpDataSource(int maxPacketSize, int socketTimeoutMillis) {
        super(true);
        this.socketTimeoutMillis = socketTimeoutMillis;
        this.packetBuffer = new byte[maxPacketSize];
        this.packet = new DatagramPacket(this.packetBuffer, 0, maxPacketSize);
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws UdpDataSourceException {
        this.uri = dataSpec.uri;
        String host = (String) Assertions.checkNotNull(this.uri.getHost());
        int port = this.uri.getPort();
        transferInitializing(dataSpec);
        try {
            this.address = InetAddress.getByName(host);
            InetSocketAddress socketAddress = new InetSocketAddress(this.address, port);
            if (this.address.isMulticastAddress()) {
                this.multicastSocket = new MulticastSocket(socketAddress);
                this.multicastSocket.joinGroup(this.address);
                this.socket = this.multicastSocket;
            } else {
                this.socket = new DatagramSocket(socketAddress);
            }
            this.socket.setSoTimeout(this.socketTimeoutMillis);
            this.opened = true;
            transferStarted(dataSpec);
            return -1L;
        } catch (IOException e) {
            throw new UdpDataSourceException(e, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED);
        } catch (SecurityException e2) {
            throw new UdpDataSourceException(e2, PlaybackException.ERROR_CODE_IO_NO_PERMISSION);
        }
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws UdpDataSourceException {
        if (length == 0) {
            return 0;
        }
        if (this.packetRemaining == 0) {
            try {
                ((DatagramSocket) Assertions.checkNotNull(this.socket)).receive(this.packet);
                this.packetRemaining = this.packet.getLength();
                bytesTransferred(this.packetRemaining);
            } catch (SocketTimeoutException e) {
                throw new UdpDataSourceException(e, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT);
            } catch (IOException e2) {
                throw new UdpDataSourceException(e2, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED);
            }
        }
        int packetOffset = this.packet.getLength() - this.packetRemaining;
        int bytesToRead = Math.min(this.packetRemaining, length);
        System.arraycopy(this.packetBuffer, packetOffset, buffer, offset, bytesToRead);
        this.packetRemaining -= bytesToRead;
        return bytesToRead;
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        return this.uri;
    }

    @Override // androidx.media3.datasource.DataSource
    public void close() {
        this.uri = null;
        if (this.multicastSocket != null) {
            try {
                this.multicastSocket.leaveGroup((InetAddress) Assertions.checkNotNull(this.address));
            } catch (IOException e) {
            }
            this.multicastSocket = null;
        }
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;
        }
        this.address = null;
        this.packetRemaining = 0;
        if (this.opened) {
            this.opened = false;
            transferEnded();
        }
    }

    public int getLocalPort() {
        if (this.socket == null) {
            return -1;
        }
        return this.socket.getLocalPort();
    }
}
