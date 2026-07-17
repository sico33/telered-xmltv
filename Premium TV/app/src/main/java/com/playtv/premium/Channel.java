package com.playtv.premium;

import android.net.Uri;
import android.util.Base64;
import androidx.media3.exoplayer.RendererCapabilities;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
final class Channel {
    private static short[] H = {-28153, -28066, -28137, -28135, -28155, -28145, -28066, -28090, -28121, -28153, -28066, -28137, -28152, -28155, -28066, -28090, -28066, -28141, -28129, -28152, -28066, -28080, -28066, -28137, -28139, -28136, -28066, -28090, -28066, -25031, -25033, -25031, -24976, -25031, -25055, -25031, -23194, -23239, -23271, -23192, -23194, -23248, -23235, -23244, -23263, -23194, -23170, -23194, -23248, -23263, -23255, -23244, -23253, -23242, -23259, -23242, -23235, -23194, -23239, -23215, -23212, -23231, -23212, -23281, -23212, -23227, -23227, -23207, -23204, -23210, -23212, -23231, -23204, -23206, -23205, -23270, -23201, -23226, -23206, -23205, -23282, -23209, -23212, -23226, -23216, -23293, -23295, -23271, -29634, -29647, -29635, -29643, -26168, -26134, -26139, -26134, -26137, -27118, -27120, -27131, -27116, -27114, -27106, -27133, -27128, -27943, -27964, -27937, -27951, -27937, -27944, -27945, -27942, -27927, -27965, -27964, -27942, -32562, -32567, -32553, -25583, -25572, -25579, -25600, -18718, -18714, -18695, -30559, -30549, -30553, -30554, -30553, -20128, -20106, -20119, -20133, -20120, -20115, -20121, -20127, -20118, -20105, -20127, -20133, -20111, -20106, -20115, -19659, -19650, -19651, -19664, -19661, -19650, -19685, -19652, -19658, -19657, -19670, -31968, -31955, -31959, -31956, -31955, -31942, -31941, -30477, -30466, -30470, -30465, -30466, -30487, -30488, -30506, -30552, -30482, -30557, -28162, -28173, -28169, -28174, -28173, -28188, -28187, -28221, -28188, -28166, -31096, -31099, -31103, -31100, -31099, -31086, -31085, -31022, -29131, -29125, -29145, -29129, -29126, -26279, -26281, -26293, -12605, -12602, -12589, -12602, -12643, -8324, -11207, -11205, -11210, -11160, -10641, -10712, -10631, -14600, -15894, -15896, -15899, -15941, -8785, -8706, -10204, -10198, -10186, -10202, -10197, -10413, -10403, -10431, -13350, -13437, -13366, -13372, -13352, -13358, -13437, -13413, -13318, -13350, -13437, -13366, -13355, -13352, -13437, -13413, -13437, -13362, -13374, -13355, -13437, -13427, -13437, -13366, -13368, -13371, -13437, -13413, -13437, -1042, -1056, -1042, -1113, -1042, -1034, -1042, -1183, -1218, -1250, -1169, -1183, -1225, -1222, -1229, -1242, -1183, -1159, -1183, -1225, -1242, -1234, -1229, -1236, -1231, -1246, -1231, -1222, -1183, -1218, -16824, -16825, -16821, -16829, -20812, -20810, -20829, -20814, -20816, -20808, -20827, -20818, -24741, -24762, -24739, -24749, -24739, -24742, -24747, -24744, -24725, -24767, -24762, -24744, -19391, -19380, -19387, -19376, -17309, -17303, -17307, -17308, -17307, -19672, -19650, -19679, -19693, -19680, -19675, -19665, -19671, -19678, -19649, -19671, -19693, -19655, -19650, -19675, -17146, -17139, -17138, -17149, -17152, -17139, -17112, -17137, -17147, -17148, -17127, -22555, -22552, -22548, -22551, -22552, -22529, -22530, -20910, -20897, -20901, -20898, -20897, -20920, -20919, -20873, -20983, -20913, -20990, -24098, -24109, -24105, -24110, -24109, -24124, -24123, -24093, -24124, -24102, -23973, -23978, -23982, -23977, -23978, -23999, -24000, -24063};
    String category;
    String drmLicenseUri;
    int globalIndex;
    String icon;
    String name;
    String originalUrl;
    String type;
    Map<String, String> headers = new HashMap();
    Map<String, String> headersM3u8 = new HashMap();
    Map<String, String> headersUrl = new HashMap();
    Map<String, String> headers2 = new HashMap();

    Channel() {
    }

    private static String H(int i, int i2, int i3) {
        char[] cArr = new char[i2 - i];
        for (int i4 = 0; i4 < i2 - i; i4++) {
            cArr[i4] = (char) (H[i + i4] ^ i3);
        }
        return new String(cArr);
    }

    static String buildClearKeyDataUri(String str, String str2) throws Exception {
        return H(59, 88, -23243) + Base64.encodeToString((H(0, 29, -28036) + Base64.encodeToString(hexToBytes(str), 11) + H(29, 36, -25061) + Base64.encodeToString(hexToBytes(str2), 11) + H(36, 59, -23228)).getBytes(), 2);
    }

    static Channel fromJson(JSONObject jSONObject, String str) {
        Channel channel = new Channel();
        channel.name = jSONObject.optString(H(88, 92, -29616), H(92, 97, -26229));
        channel.category = jSONObject.optString(H(97, 105, -27023), str);
        channel.originalUrl = jSONObject.optString(H(105, 117, -27978), jSONObject.optString(H(117, 120, -32581), ""));
        channel.type = jSONObject.optString(H(120, 124, -25499), H(124, 127, -18774));
        channel.icon = jSONObject.optString(H(127, 132, -30520), "");
        channel.drmLicenseUri = jSONObject.optString(H(132, 147, -20220), "");
        channel.globalIndex = jSONObject.optInt(H(147, 158, -19630), 0);
        channel.headers = readMap(jSONObject.optJSONObject(H(158, 165, -31928)));
        channel.headersM3u8 = readMap(jSONObject.optJSONObject(H(165, 176, -30565)));
        channel.headersUrl = readMap(jSONObject.optJSONObject(H(176, 186, -28266)));
        channel.headers2 = readMap(jSONObject.optJSONObject(H(186, 194, -31008)));
        return channel;
    }

    private static byte[] hexToBytes(String str) {
        int length = str.length();
        byte[] bArr = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }

    private static Map<String, String> readMap(JSONObject jSONObject) {
        HashMap map = new HashMap();
        if (jSONObject == null) {
            return map;
        }
        Iterator<String> itKeys = jSONObject.keys();
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            map.put(next, jSONObject.optString(next, ""));
        }
        return map;
    }

    String toClearKeyDataUri() {
        if (this.drmLicenseUri == null || this.drmLicenseUri.isEmpty()) {
            return "";
        }
        try {
            Uri uri = Uri.parse(this.drmLicenseUri);
            String queryParameter = uri.getQueryParameter(H(194, 199, -29090));
            String queryParameter2 = uri.getQueryParameter(H(199, 202, -26318));
            return (queryParameter == null || queryParameter2 == null) ? this.drmLicenseUri : buildClearKeyDataUri(queryParameter, queryParameter2);
        } catch (Exception e) {
            return this.drmLicenseUri;
        }
    }

    String toClearKeyJson() {
        String strEncodeToString;
        String strEncodeToString2;
        if (this.drmLicenseUri == null || this.drmLicenseUri.isEmpty()) {
            return "";
        }
        try {
            boolean zStartsWith = this.drmLicenseUri.startsWith(H(202, 207, -12633));
            String str = this.drmLicenseUri;
            if (zStartsWith) {
                return new String(Base64.decode(str.substring(this.drmLicenseUri.indexOf(H(207, 208, -8368)) + 1), 0));
            }
            if (str.contains(H(208, 212, -11182)) && this.drmLicenseUri.contains(H(212, 215, -10685))) {
                strEncodeToString = "";
                strEncodeToString2 = "";
                for (String str2 : this.drmLicenseUri.split(H(215, 216, -14636))) {
                    if (str2.startsWith(H(216, 220, -15999))) {
                        strEncodeToString = str2.substring(4).trim();
                    } else if (str2.startsWith(H(220, 222, -8764))) {
                        strEncodeToString2 = str2.substring(2).trim();
                    }
                }
            } else {
                Uri uri = Uri.parse(this.drmLicenseUri);
                String queryParameter = uri.getQueryParameter(H(222, 227, -10161));
                String queryParameter2 = uri.getQueryParameter(H(227, 230, -10440));
                if (queryParameter == null || queryParameter2 == null) {
                    return "";
                }
                strEncodeToString = Base64.encodeToString(hexToBytes(queryParameter), 11);
                strEncodeToString2 = Base64.encodeToString(hexToBytes(queryParameter2), 11);
            }
            return (strEncodeToString.isEmpty() || strEncodeToString2.isEmpty()) ? "" : H(230, 259, -13407) + strEncodeToString + H(259, 266, -1076) + strEncodeToString2 + H(266, 289, -1213);
        } catch (Exception e) {
            return "";
        }
    }

    JSONObject toJson() throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put(H(289, 293, -16858), this.name);
        jSONObject.put(H(293, 301, -20777), this.category);
        jSONObject.put(H(301, 313, -24780), this.originalUrl);
        jSONObject.put(H(313, 317, -19403), this.type);
        jSONObject.put(H(317, 322, -17398), this.icon);
        jSONObject.put(H(322, 337, -19636), this.drmLicenseUri);
        jSONObject.put(H(337, 348, -17055), this.globalIndex);
        jSONObject.put(H(348, 355, -22643), new JSONObject(this.headers));
        jSONObject.put(H(355, 366, -20934), new JSONObject(this.headersM3u8));
        jSONObject.put(H(366, 376, -24138), new JSONObject(this.headersUrl));
        jSONObject.put(H(376, RendererCapabilities.DECODER_SUPPORT_MASK, -24013), new JSONObject(this.headers2));
        return jSONObject;
    }
}
