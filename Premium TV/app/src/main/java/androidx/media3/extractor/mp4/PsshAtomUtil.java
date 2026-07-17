package androidx.media3.extractor.mp4;

import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import java.nio.ByteBuffer;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public final class PsshAtomUtil {
    private static final String TAG = "PsshAtomUtil";

    private PsshAtomUtil() {
    }

    public static byte[] buildPsshAtom(UUID systemId, byte[] data) {
        return buildPsshAtom(systemId, null, data);
    }

    public static byte[] buildPsshAtom(UUID systemId, UUID[] keyIds, byte[] data) {
        int dataLength = data != null ? data.length : 0;
        int psshBoxLength = dataLength + 32;
        if (keyIds != null) {
            psshBoxLength += (keyIds.length * 16) + 4;
        }
        ByteBuffer psshBox = ByteBuffer.allocate(psshBoxLength);
        psshBox.putInt(psshBoxLength);
        psshBox.putInt(Atom.TYPE_pssh);
        psshBox.putInt(keyIds != null ? 16777216 : 0);
        psshBox.putLong(systemId.getMostSignificantBits());
        psshBox.putLong(systemId.getLeastSignificantBits());
        if (keyIds != null) {
            psshBox.putInt(keyIds.length);
            for (UUID keyId : keyIds) {
                psshBox.putLong(keyId.getMostSignificantBits());
                psshBox.putLong(keyId.getLeastSignificantBits());
            }
        }
        if (data != null && data.length != 0) {
            psshBox.putInt(data.length);
            psshBox.put(data);
        } else {
            psshBox.putInt(0);
        }
        return psshBox.array();
    }

    public static boolean isPsshAtom(byte[] data) {
        return parsePsshAtom(data) != null;
    }

    public static UUID parseUuid(byte[] atom) {
        PsshAtom parsedAtom = parsePsshAtom(atom);
        if (parsedAtom == null) {
            return null;
        }
        return parsedAtom.uuid;
    }

    public static int parseVersion(byte[] atom) {
        PsshAtom parsedAtom = parsePsshAtom(atom);
        if (parsedAtom == null) {
            return -1;
        }
        return parsedAtom.version;
    }

    public static byte[] parseSchemeSpecificData(byte[] atom, UUID uuid) {
        PsshAtom parsedAtom = parsePsshAtom(atom);
        if (parsedAtom == null) {
            return null;
        }
        if (!uuid.equals(parsedAtom.uuid)) {
            Log.w(TAG, "UUID mismatch. Expected: " + uuid + ", got: " + parsedAtom.uuid + ".");
            return null;
        }
        return parsedAtom.schemeData;
    }

    public static PsshAtom parsePsshAtom(byte[] atom) {
        PsshAtom psshAtom;
        ParsableByteArray atomData = new ParsableByteArray(atom);
        PsshAtom psshAtom2 = null;
        if (atomData.limit() < 32) {
            return null;
        }
        atomData.setPosition(0);
        int bufferLength = atomData.bytesLeft();
        int atomSize = atomData.readInt();
        if (atomSize != bufferLength) {
            Log.w(TAG, "Advertised atom size (" + atomSize + ") does not match buffer size: " + bufferLength);
            return null;
        }
        int atomType = atomData.readInt();
        if (atomType != 1886614376) {
            Log.w(TAG, "Atom type is not pssh: " + atomType);
            return null;
        }
        int atomVersion = Atom.parseFullAtomVersion(atomData.readInt());
        if (atomVersion > 1) {
            Log.w(TAG, "Unsupported pssh version: " + atomVersion);
            return null;
        }
        UUID uuid = new UUID(atomData.readLong(), atomData.readLong());
        UUID[] keyIds = null;
        if (atomVersion != 1) {
            psshAtom = null;
        } else {
            int keyIdCount = atomData.readUnsignedIntToInt();
            keyIds = new UUID[keyIdCount];
            int i = 0;
            while (i < keyIdCount) {
                keyIds[i] = new UUID(atomData.readLong(), atomData.readLong());
                i++;
                psshAtom2 = psshAtom2;
                atomSize = atomSize;
            }
            psshAtom = psshAtom2;
        }
        int dataSize = atomData.readUnsignedIntToInt();
        int bufferLength2 = atomData.bytesLeft();
        if (dataSize != bufferLength2) {
            Log.w(TAG, "Atom data size (" + dataSize + ") does not match the bytes left: " + bufferLength2);
            return psshAtom;
        }
        byte[] data = new byte[dataSize];
        atomData.readBytes(data, 0, dataSize);
        return new PsshAtom(uuid, atomVersion, data, keyIds);
    }

    public static final class PsshAtom {
        public final UUID[] keyIds;
        public final byte[] schemeData;
        public final UUID uuid;
        public final int version;

        PsshAtom(UUID uuid, int version, byte[] schemeData, UUID[] keyIds) {
            this.uuid = uuid;
            this.version = version;
            this.schemeData = schemeData;
            this.keyIds = keyIds;
        }
    }
}
