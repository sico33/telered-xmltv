package com.google.common.net;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.CheckForNull;
import kotlin.UShort;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class InetAddresses {
    private static final int IPV4_PART_COUNT = 4;
    private static final int IPV6_PART_COUNT = 8;
    private static final char IPV4_DELIMITER = '.';
    private static final CharMatcher IPV4_DELIMITER_MATCHER = CharMatcher.is(IPV4_DELIMITER);
    private static final char IPV6_DELIMITER = ':';
    private static final CharMatcher IPV6_DELIMITER_MATCHER = CharMatcher.is(IPV6_DELIMITER);
    private static final Inet4Address LOOPBACK4 = (Inet4Address) forString("127.0.0.1");
    private static final Inet4Address ANY4 = (Inet4Address) forString("0.0.0.0");

    public static final class TeredoInfo {
        private final Inet4Address client;
        private final int flags;
        private final int port;
        private final Inet4Address server;

        public TeredoInfo(@CheckForNull Inet4Address inet4Address, @CheckForNull Inet4Address inet4Address2, int i, int i2) {
            Preconditions.checkArgument(i >= 0 && i <= 65535, "port '%s' is out of range (0 <= port <= 0xffff)", i);
            Preconditions.checkArgument(i2 >= 0 && i2 <= 65535, "flags '%s' is out of range (0 <= flags <= 0xffff)", i2);
            this.server = (Inet4Address) MoreObjects.firstNonNull(inet4Address, InetAddresses.ANY4);
            this.client = (Inet4Address) MoreObjects.firstNonNull(inet4Address2, InetAddresses.ANY4);
            this.port = i;
            this.flags = i2;
        }

        public Inet4Address getClient() {
            return this.client;
        }

        public int getFlags() {
            return this.flags;
        }

        public int getPort() {
            return this.port;
        }

        public Inet4Address getServer() {
            return this.server;
        }
    }

    private InetAddresses() {
    }

    private static InetAddress bytesToInetAddress(byte[] bArr) {
        try {
            return InetAddress.getByAddress(bArr);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }
    }

    public static int coerceToInteger(InetAddress inetAddress) {
        return ByteStreams.newDataInput(getCoercedIPv4Address(inetAddress).getAddress()).readInt();
    }

    private static void compressLongestRunOfZeroes(int[] iArr) {
        int i = -1;
        int i2 = -1;
        int i3 = -1;
        for (int i4 = 0; i4 < iArr.length + 1; i4++) {
            if (i4 >= iArr.length || iArr[i4] != 0) {
                if (i >= 0) {
                    int i5 = i4 - i;
                    if (i5 > i2) {
                        i2 = i5;
                    } else {
                        i = i3;
                    }
                    i3 = i;
                    i = -1;
                }
            } else if (i < 0) {
                i = i4;
            }
        }
        if (i2 >= 2) {
            Arrays.fill(iArr, i3, i3 + i2, -1);
        }
    }

    @CheckForNull
    private static String convertDottedQuadToHex(String str) {
        int iLastIndexOf = str.lastIndexOf(58);
        String strSubstring = str.substring(0, iLastIndexOf + 1);
        byte[] bArrTextToNumericFormatV4 = textToNumericFormatV4(str.substring(iLastIndexOf + 1));
        if (bArrTextToNumericFormatV4 == null) {
            return null;
        }
        return strSubstring + Integer.toHexString(((bArrTextToNumericFormatV4[0] & 255) << 8) | (bArrTextToNumericFormatV4[1] & 255)) + ":" + Integer.toHexString((bArrTextToNumericFormatV4[3] & 255) | ((bArrTextToNumericFormatV4[2] & 255) << 8));
    }

    public static InetAddress decrement(InetAddress inetAddress) {
        byte[] address = inetAddress.getAddress();
        int length = address.length - 1;
        while (length >= 0 && address[length] == 0) {
            address[length] = (byte) (-1);
            length--;
        }
        Preconditions.checkArgument(length >= 0, "Decrementing %s would wrap.", inetAddress);
        address[length] = (byte) (address[length] - 1);
        return bytesToInetAddress(address);
    }

    public static InetAddress forString(String str) {
        byte[] bArrIpStringToBytes = ipStringToBytes(str);
        if (bArrIpStringToBytes != null) {
            return bytesToInetAddress(bArrIpStringToBytes);
        }
        throw formatIllegalArgumentException("'%s' is not an IP string literal.", str);
    }

    public static InetAddress forUriString(String str) {
        InetAddress inetAddressForUriStringNoThrow = forUriStringNoThrow(str);
        if (inetAddressForUriStringNoThrow != null) {
            return inetAddressForUriStringNoThrow;
        }
        throw formatIllegalArgumentException("Not a valid URI IP literal: '%s'", str);
    }

    @CheckForNull
    private static InetAddress forUriStringNoThrow(String str) {
        int i;
        Preconditions.checkNotNull(str);
        if (str.startsWith("[") && str.endsWith("]")) {
            str = str.substring(1, str.length() - 1);
            i = 16;
        } else {
            i = 4;
        }
        byte[] bArrIpStringToBytes = ipStringToBytes(str);
        if (bArrIpStringToBytes == null || bArrIpStringToBytes.length != i) {
            return null;
        }
        return bytesToInetAddress(bArrIpStringToBytes);
    }

    private static IllegalArgumentException formatIllegalArgumentException(String str, Object... objArr) {
        return new IllegalArgumentException(String.format(Locale.ROOT, str, objArr));
    }

    private static InetAddress fromBigInteger(BigInteger bigInteger, boolean z) {
        Preconditions.checkArgument(bigInteger.signum() >= 0, "BigInteger must be greater than or equal to 0");
        int i = z ? 16 : 4;
        byte[] byteArray = bigInteger.toByteArray();
        byte[] bArr = new byte[i];
        int iMax = Math.max(0, byteArray.length - i);
        int length = byteArray.length - iMax;
        for (int i2 = 0; i2 < iMax; i2++) {
            if (byteArray[i2] != 0) {
                throw formatIllegalArgumentException("BigInteger cannot be converted to InetAddress because it has more than %d bytes: %s", Integer.valueOf(i), bigInteger);
            }
        }
        System.arraycopy(byteArray, iMax, bArr, i - length, length);
        try {
            return InetAddress.getByAddress(bArr);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }
    }

    public static Inet4Address fromIPv4BigInteger(BigInteger bigInteger) {
        return (Inet4Address) fromBigInteger(bigInteger, false);
    }

    public static Inet6Address fromIPv6BigInteger(BigInteger bigInteger) {
        return (Inet6Address) fromBigInteger(bigInteger, true);
    }

    public static Inet4Address fromInteger(int i) {
        return getInet4Address(Ints.toByteArray(i));
    }

    public static InetAddress fromLittleEndianByteArray(byte[] bArr) throws UnknownHostException {
        byte[] bArr2 = new byte[bArr.length];
        for (int i = 0; i < bArr.length; i++) {
            bArr2[i] = bArr[(bArr.length - i) - 1];
        }
        return InetAddress.getByAddress(bArr2);
    }

    public static Inet4Address get6to4IPv4Address(Inet6Address inet6Address) {
        Preconditions.checkArgument(is6to4Address(inet6Address), "Address '%s' is not a 6to4 address.", toAddrString(inet6Address));
        return getInet4Address(Arrays.copyOfRange(inet6Address.getAddress(), 2, 6));
    }

    public static Inet4Address getCoercedIPv4Address(InetAddress inetAddress) {
        boolean z;
        if (inetAddress instanceof Inet4Address) {
            return (Inet4Address) inetAddress;
        }
        byte[] address = inetAddress.getAddress();
        int i = 0;
        while (true) {
            if (i >= 15) {
                z = true;
                break;
            }
            if (address[i] != 0) {
                z = false;
                break;
            }
            i++;
        }
        if (z && address[15] == 1) {
            return LOOPBACK4;
        }
        if (z && address[15] == 0) {
            return ANY4;
        }
        Inet6Address inet6Address = (Inet6Address) inetAddress;
        int iAsInt = Hashing.murmur3_32_fixed().hashLong(hasEmbeddedIPv4ClientAddress(inet6Address) ? getEmbeddedIPv4ClientAddress(inet6Address).hashCode() : ByteBuffer.wrap(inet6Address.getAddress(), 0, 8).getLong()).asInt() | (-536870912);
        if (iAsInt == -1) {
            iAsInt = -2;
        }
        return getInet4Address(Ints.toByteArray(iAsInt));
    }

    public static Inet4Address getCompatIPv4Address(Inet6Address inet6Address) {
        Preconditions.checkArgument(isCompatIPv4Address(inet6Address), "Address '%s' is not IPv4-compatible.", toAddrString(inet6Address));
        return getInet4Address(Arrays.copyOfRange(inet6Address.getAddress(), 12, 16));
    }

    public static Inet4Address getEmbeddedIPv4ClientAddress(Inet6Address inet6Address) {
        if (isCompatIPv4Address(inet6Address)) {
            return getCompatIPv4Address(inet6Address);
        }
        if (is6to4Address(inet6Address)) {
            return get6to4IPv4Address(inet6Address);
        }
        if (isTeredoAddress(inet6Address)) {
            return getTeredoInfo(inet6Address).getClient();
        }
        throw formatIllegalArgumentException("'%s' has no embedded IPv4 address.", toAddrString(inet6Address));
    }

    private static Inet4Address getInet4Address(byte[] bArr) {
        Preconditions.checkArgument(bArr.length == 4, "Byte array has invalid length for an IPv4 address: %s != 4.", bArr.length);
        return (Inet4Address) bytesToInetAddress(bArr);
    }

    public static Inet4Address getIsatapIPv4Address(Inet6Address inet6Address) {
        Preconditions.checkArgument(isIsatapAddress(inet6Address), "Address '%s' is not an ISATAP address.", toAddrString(inet6Address));
        return getInet4Address(Arrays.copyOfRange(inet6Address.getAddress(), 12, 16));
    }

    public static TeredoInfo getTeredoInfo(Inet6Address inet6Address) {
        Preconditions.checkArgument(isTeredoAddress(inet6Address), "Address '%s' is not a Teredo address.", toAddrString(inet6Address));
        byte[] address = inet6Address.getAddress();
        Inet4Address inet4Address = getInet4Address(Arrays.copyOfRange(address, 4, 8));
        short s = ByteStreams.newDataInput(address, 8).readShort();
        short s2 = ByteStreams.newDataInput(address, 10).readShort();
        byte[] bArrCopyOfRange = Arrays.copyOfRange(address, 12, 16);
        for (int i = 0; i < bArrCopyOfRange.length; i++) {
            bArrCopyOfRange[i] = (byte) (bArrCopyOfRange[i] ^ (-1));
        }
        return new TeredoInfo(inet4Address, getInet4Address(bArrCopyOfRange), (s2 ^ (-1)) & 65535, s & UShort.MAX_VALUE);
    }

    public static boolean hasEmbeddedIPv4ClientAddress(Inet6Address inet6Address) {
        return isCompatIPv4Address(inet6Address) || is6to4Address(inet6Address) || isTeredoAddress(inet6Address);
    }

    private static String hextetsToIPv6String(int[] iArr) {
        StringBuilder sb = new StringBuilder(39);
        int i = 0;
        boolean z = false;
        while (i < iArr.length) {
            boolean z2 = iArr[i] >= 0;
            if (z2) {
                if (z) {
                    sb.append(IPV6_DELIMITER);
                }
                sb.append(Integer.toHexString(iArr[i]));
            } else if (i == 0 || z) {
                sb.append("::");
            }
            i++;
            z = z2;
        }
        return sb.toString();
    }

    public static InetAddress increment(InetAddress inetAddress) {
        byte[] address = inetAddress.getAddress();
        int length = address.length - 1;
        while (length >= 0 && address[length] == -1) {
            address[length] = (byte) 0;
            length--;
        }
        Preconditions.checkArgument(length >= 0, "Incrementing %s would wrap.", inetAddress);
        address[length] = (byte) (address[length] + 1);
        return bytesToInetAddress(address);
    }

    @CheckForNull
    private static byte[] ipStringToBytes(String str) {
        int i;
        String strSubstring;
        int i2 = 0;
        boolean z = false;
        boolean z2 = false;
        while (true) {
            if (i2 >= str.length()) {
                i = -1;
                break;
            }
            char cCharAt = str.charAt(i2);
            if (cCharAt == '.') {
                z = true;
            } else if (cCharAt == ':') {
                if (z) {
                    return null;
                }
                z2 = true;
            } else {
                if (cCharAt == '%') {
                    i = i2;
                    break;
                }
                if (Character.digit(cCharAt, 16) == -1) {
                    return null;
                }
            }
            i2++;
        }
        if (!z2) {
            if (z && i == -1) {
                return textToNumericFormatV4(str);
            }
            return null;
        }
        if (z) {
            strSubstring = convertDottedQuadToHex(str);
            if (strSubstring == null) {
                return null;
            }
        } else {
            strSubstring = str;
        }
        if (i != -1) {
            strSubstring = strSubstring.substring(0, i);
        }
        return textToNumericFormatV6(strSubstring);
    }

    public static boolean is6to4Address(Inet6Address inet6Address) {
        byte[] address = inet6Address.getAddress();
        return address[0] == 32 && address[1] == 2;
    }

    public static boolean isCompatIPv4Address(Inet6Address inet6Address) {
        if (!inet6Address.isIPv4CompatibleAddress()) {
            return false;
        }
        byte[] address = inet6Address.getAddress();
        return (address[12] == 0 && address[13] == 0 && address[14] == 0 && (address[15] == 0 || address[15] == 1)) ? false : true;
    }

    public static boolean isInetAddress(String str) {
        return ipStringToBytes(str) != null;
    }

    public static boolean isIsatapAddress(Inet6Address inet6Address) {
        if (isTeredoAddress(inet6Address)) {
            return false;
        }
        byte[] address = inet6Address.getAddress();
        return (address[8] | 3) == 3 && address[9] == 0 && address[10] == 94 && address[11] == -2;
    }

    public static boolean isMappedIPv4Address(String str) {
        byte[] bArrIpStringToBytes = ipStringToBytes(str);
        if (bArrIpStringToBytes == null || bArrIpStringToBytes.length != 16) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (bArrIpStringToBytes[i] != 0) {
                return false;
            }
        }
        for (int i2 = 10; i2 < 12; i2++) {
            if (bArrIpStringToBytes[i2] != -1) {
                return false;
            }
        }
        return true;
    }

    public static boolean isMaximum(InetAddress inetAddress) {
        for (byte b : inetAddress.getAddress()) {
            if (b != -1) {
                return false;
            }
        }
        return true;
    }

    public static boolean isTeredoAddress(Inet6Address inet6Address) {
        byte[] address = inet6Address.getAddress();
        return address[0] == 32 && address[1] == 1 && address[2] == 0 && address[3] == 0;
    }

    public static boolean isUriInetAddress(String str) {
        return forUriStringNoThrow(str) != null;
    }

    private static short parseHextet(String str, int i, int i2) {
        int i3 = i2 - i;
        if (i3 <= 0 || i3 > 4) {
            throw new NumberFormatException();
        }
        int iDigit = 0;
        while (i < i2) {
            iDigit = (iDigit << 4) | Character.digit(str.charAt(i), 16);
            i++;
        }
        return (short) iDigit;
    }

    private static byte parseOctet(String str, int i, int i2) {
        int i3 = i2 - i;
        if (i3 <= 0 || i3 > 3) {
            throw new NumberFormatException();
        }
        if (i3 > 1 && str.charAt(i) == '0') {
            throw new NumberFormatException();
        }
        int i4 = 0;
        while (i < i2) {
            int iDigit = Character.digit(str.charAt(i), 10);
            if (iDigit < 0) {
                throw new NumberFormatException();
            }
            i4 = (i4 * 10) + iDigit;
            i++;
        }
        if (i4 <= 255) {
            return (byte) i4;
        }
        throw new NumberFormatException();
    }

    @CheckForNull
    private static byte[] textToNumericFormatV4(String str) {
        if (IPV4_DELIMITER_MATCHER.countIn(str) + 1 != 4) {
            return null;
        }
        byte[] bArr = new byte[4];
        int i = 0;
        for (int i2 = 0; i2 < 4; i2++) {
            int iIndexOf = str.indexOf(46, i);
            if (iIndexOf == -1) {
                iIndexOf = str.length();
            }
            try {
                bArr[i2] = parseOctet(str, i, iIndexOf);
                i = iIndexOf + 1;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return bArr;
    }

    @CheckForNull
    private static byte[] textToNumericFormatV6(String str) {
        int iCountIn = IPV6_DELIMITER_MATCHER.countIn(str);
        if (iCountIn < 2 || iCountIn > 8) {
            return null;
        }
        boolean z = false;
        int i = 8 - (iCountIn + 1);
        for (int i2 = 0; i2 < str.length() - 1; i2++) {
            if (str.charAt(i2) == ':' && str.charAt(i2 + 1) == ':') {
                if (z) {
                    return null;
                }
                int i3 = i + 1;
                if (i2 == 0) {
                    i3++;
                }
                if (i2 == str.length() - 2) {
                    i = i3 + 1;
                    z = true;
                } else {
                    i = i3;
                    z = true;
                }
            }
        }
        if (str.charAt(0) == ':' && str.charAt(1) != ':') {
            return null;
        }
        if (str.charAt(str.length() - 1) == ':' && str.charAt(str.length() - 2) != ':') {
            return null;
        }
        if (z && i <= 0) {
            return null;
        }
        if (!z && iCountIn + 1 != 8) {
            return null;
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
        try {
            int i4 = str.charAt(0) != ':' ? 0 : 1;
            while (i4 < str.length()) {
                int iIndexOf = str.indexOf(58, i4);
                int length = iIndexOf == -1 ? str.length() : iIndexOf;
                if (str.charAt(i4) == ':') {
                    for (int i5 = 0; i5 < i; i5++) {
                        byteBufferAllocate.putShort((short) 0);
                    }
                } else {
                    byteBufferAllocate.putShort(parseHextet(str, i4, length));
                }
                i4 = length + 1;
            }
            return byteBufferAllocate.array();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toAddrString(InetAddress inetAddress) {
        Preconditions.checkNotNull(inetAddress);
        if (inetAddress instanceof Inet4Address) {
            return (String) Objects.requireNonNull(inetAddress.getHostAddress());
        }
        Preconditions.checkArgument(inetAddress instanceof Inet6Address);
        byte[] address = inetAddress.getAddress();
        int[] iArr = new int[8];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = Ints.fromBytes((byte) 0, (byte) 0, address[i * 2], address[(i * 2) + 1]);
        }
        compressLongestRunOfZeroes(iArr);
        return hextetsToIPv6String(iArr);
    }

    public static BigInteger toBigInteger(InetAddress inetAddress) {
        return new BigInteger(1, inetAddress.getAddress());
    }

    public static String toUriString(InetAddress inetAddress) {
        return inetAddress instanceof Inet6Address ? "[" + toAddrString(inetAddress) + "]" : toAddrString(inetAddress);
    }
}
