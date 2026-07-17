package com.google.common.net;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@Immutable
@ElementTypesAreNonnullByDefault
public final class HostAndPort implements Serializable {
    private static final int NO_PORT = -1;
    private static final long serialVersionUID = 0;
    private final boolean hasBracketlessColons;
    private final String host;
    private final int port;

    private HostAndPort(String str, int i, boolean z) {
        this.host = str;
        this.port = i;
        this.hasBracketlessColons = z;
    }

    public static HostAndPort fromHost(String str) {
        HostAndPort hostAndPortFromString = fromString(str);
        Preconditions.checkArgument(!hostAndPortFromString.hasPort(), "Host has a port: %s", str);
        return hostAndPortFromString;
    }

    public static HostAndPort fromParts(String str, int i) {
        Preconditions.checkArgument(isValidPort(i), "Port out of range: %s", i);
        HostAndPort hostAndPortFromString = fromString(str);
        Preconditions.checkArgument(!hostAndPortFromString.hasPort(), "Host has a port: %s", str);
        return new HostAndPort(hostAndPortFromString.host, i, hostAndPortFromString.hasBracketlessColons);
    }

    public static HostAndPort fromString(String str) {
        String strSubstring;
        boolean z;
        String strSubstring2;
        int i;
        boolean z2 = false;
        Preconditions.checkNotNull(str);
        if (str.startsWith("[")) {
            String[] hostAndPortFromBracketedHost = getHostAndPortFromBracketedHost(str);
            strSubstring2 = hostAndPortFromBracketedHost[0];
            strSubstring = hostAndPortFromBracketedHost[1];
            z = false;
        } else {
            int iIndexOf = str.indexOf(58);
            if (iIndexOf < 0 || str.indexOf(58, iIndexOf + 1) != -1) {
                boolean z3 = iIndexOf >= 0;
                strSubstring = null;
                z = z3;
                strSubstring2 = str;
            } else {
                strSubstring2 = str.substring(0, iIndexOf);
                strSubstring = str.substring(iIndexOf + 1);
                z = false;
            }
        }
        if (Strings.isNullOrEmpty(strSubstring)) {
            i = -1;
        } else {
            if (!strSubstring.startsWith("+") && CharMatcher.ascii().matchesAllOf(strSubstring)) {
                z2 = true;
            }
            Preconditions.checkArgument(z2, "Unparseable port number: %s", str);
            try {
                i = Integer.parseInt(strSubstring);
                Preconditions.checkArgument(isValidPort(i), "Port number out of range: %s", str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unparseable port number: " + str);
            }
        }
        return new HostAndPort(strSubstring2, i, z);
    }

    private static String[] getHostAndPortFromBracketedHost(String str) {
        Preconditions.checkArgument(str.charAt(0) == '[', "Bracketed host-port string must start with a bracket: %s", str);
        int iIndexOf = str.indexOf(58);
        int iLastIndexOf = str.lastIndexOf(93);
        Preconditions.checkArgument(iIndexOf > -1 && iLastIndexOf > iIndexOf, "Invalid bracketed host/port: %s", str);
        String strSubstring = str.substring(1, iLastIndexOf);
        if (iLastIndexOf + 1 == str.length()) {
            return new String[]{strSubstring, ""};
        }
        Preconditions.checkArgument(str.charAt(iLastIndexOf + 1) == ':', "Only a colon may follow a close bracket: %s", str);
        for (int i = iLastIndexOf + 2; i < str.length(); i++) {
            Preconditions.checkArgument(Character.isDigit(str.charAt(i)), "Port must be numeric: %s", str);
        }
        return new String[]{strSubstring, str.substring(iLastIndexOf + 2)};
    }

    private static boolean isValidPort(int i) {
        return i >= 0 && i <= 65535;
    }

    public boolean equals(@CheckForNull Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HostAndPort)) {
            return false;
        }
        HostAndPort hostAndPort = (HostAndPort) obj;
        return Objects.equal(this.host, hostAndPort.host) && this.port == hostAndPort.port;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        Preconditions.checkState(hasPort());
        return this.port;
    }

    public int getPortOrDefault(int i) {
        return hasPort() ? this.port : i;
    }

    public boolean hasPort() {
        return this.port >= 0;
    }

    public int hashCode() {
        return Objects.hashCode(this.host, Integer.valueOf(this.port));
    }

    public HostAndPort requireBracketsForIPv6() {
        Preconditions.checkArgument(!this.hasBracketlessColons, "Possible bracketless IPv6 literal: %s", this.host);
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(this.host.length() + 8);
        if (this.host.indexOf(58) >= 0) {
            sb.append('[').append(this.host).append(']');
        } else {
            sb.append(this.host);
        }
        if (hasPort()) {
            sb.append(':').append(this.port);
        }
        return sb.toString();
    }

    public HostAndPort withDefaultPort(int i) {
        Preconditions.checkArgument(isValidPort(i));
        return hasPort() ? this : new HostAndPort(this.host, i, this.hasBracketlessColons);
    }
}
