package com.playtv.premium;

import android.util.Log;
import java.util.Locale;
import java.util.Map;

/* JADX INFO: loaded from: classes2.dex */
final class FlowSigner {
    private static short[] r = {16586, 16576, 16579, 16603, 16627, 16606, 16585, 16586, 16585, 16606, 16585, 16606, 16627, 16584, 16579, 16577, 16589, 16581, 16578, 28573, 28545, 28545, 28549, 28550, 28623, 28634, 28634, 24970, 24982, 24982, 24978, 25048, 25037, 25037, 28929, 27460, 27470, 27469, 27477, 27517, 27472, 27463, 27460, 27463, 27472, 27463, 27472, 27517, 27462, 27469, 27471, 27459, 27467, 27468, 27517, 27474, 27463, 27472, 27473, 27469, 27468, 27459, 27470, 32576, 32604, 32604, 32600, 32603, 32530, 32519, 32519, 29632, 29660, 29660, 29656, 29586, 29575, 29575, 30017, -11614, -11595, -11594, -11595, -11614, -11595, -11614, 3769, 3834, 3815, 3827, 3817, 3754, 3767, 3747, 3832, 4150, 4213, 4200, 4220, 4155, 156, 137, 158, 139, 139, 137, 209, 156, 144, 146, 209, 158, 141, 4207, 4218, 4205, 4216, 4216, 4218, 4130, 4207, 4195, 4193, 4130, 4220, 4213, 3881, 3875, 3872, 3896, 3937, 3884, 3872, 3874, 3937, 3886, 3901, 664, 658, 657, 649, 720, 669, 657, 659, 720, 654, 647, 6929, 6916, 6931, 6918, 6918, 6916, 7004, 6933, 6935, 6918, 7007, 6932, 6935, 6943, 6941, 6940, 7004, 6940, 6935, 6918, 33, 48, 48, 110, 38, 44, 47, 55, 110, 35, 47, 45, -19763, -19759, -19748, -19772, -19767, -19765, -19774, -19749, -19759, -19758, -19766, -22000, -21993, -22007, -21915, -21974, -21961, -21972, -21982, -21972, -21973, -21980, -21975, -21889, -21915, -17535, -17507, -17520, -17528, -17531, -17529, -17522, -17513, -17507, -17506, -17530, -19834, -19801, -19736, -19781, -19795, -19736, -19784, -19779, -19796, -19801, -19736, -19801, -19798, -19780, -19795, -19802, -19795, -19782, -19736, -19780, -19801, -19805, -19795, -19802, -19736, -19829, -19828, -19834, -19740, -19736, -19796, -19795, -19778, -19801, -19804, -19778, -19807, -19795, -19802, -19796, -19801, -19736, -19811, -19814, -19836, -19736, -19801, -19782, -19807, -19793, -19807, -19802, -19799, -19804, -22070, -22058, -22053, -22077, -22066, -22068, -22075, -22052, -22058, -22059, -22067, -18781, -18814, -18739, -18786, -18808, -18739, -18787, -18792, -18807, -18814, -18739, -18808, -18795, -18791, -18785, -18804, -18808, -18785, -18739, -18787, -18804, -18791, -18811, -18739, -18785, -18808, -18815, -18804, -18791, -18812, -18789, -18814, -18739, -18807, -18808, -18729, -18739, -29919, -29655, -29643, -29643, -29647, -29646, -29573, -29586, -29586, -24555, -28966, -17433, -17413, -17418, -17426, -17437, -17439, -17432, -17423, -17413, -17416, -17440, -21034, -21039, -21041, -21085, -21019, -21014, -21007, -21010, -21022, -21017, -21022, -21063, -21085};

    private FlowSigner() {
    }

    private static boolean isNormalFlow(Channel channel, AppConfig appConfig) {
        String strReferer = referer(channel);
        String strReplace = appConfig.getOrDefault(r(0, 19, 16556), "").toLowerCase(Locale.US).replace(r(19, 27, 28661), "").replace(r(27, 34, 25058), "");
        if (strReplace.endsWith(r(34, 35, 28974))) {
            strReplace = strReplace.substring(0, strReplace.length() - 1);
        }
        return !strReplace.isEmpty() && strReferer.contains(strReplace);
    }

    private static boolean isPersonalFlow(Channel channel, AppConfig appConfig) {
        String strReferer = referer(channel);
        String strReplace = appConfig.getOrDefault(r(35, 63, 27426), "").toLowerCase(Locale.US).replace(r(63, 71, 32552), "").replace(r(71, 78, 29608), "");
        if (strReplace.endsWith(r(78, 79, 30062))) {
            strReplace = strReplace.substring(0, strReplace.length() - 1);
        }
        return !strReplace.isEmpty() && strReferer.contains(strReplace);
    }

    private static String r(int i, int i2, int i3) {
        char[] cArr = new char[i2 - i];
        for (int i4 = 0; i4 < i2 - i; i4++) {
            cArr[i4] = (char) (r[i + i4] ^ i3);
        }
        return new String(cArr);
    }

    private static String referer(Channel channel) {
        if (channel.headers == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : channel.headers.entrySet()) {
            if (r(79, 86, -11568).equalsIgnoreCase(entry.getKey())) {
                return entry.getValue() == null ? "" : entry.getValue().toLowerCase(Locale.US);
            }
        }
        return "";
    }

    static boolean requiresSigning(Channel channel, AppConfig appConfig) {
        String lowerCase = channel.originalUrl == null ? "" : channel.originalUrl.toLowerCase(Locale.US);
        if (!(lowerCase.endsWith(r(86, 90, 3735)) || lowerCase.contains(r(90, 95, 3783)) || lowerCase.contains(r(95, 100, 4120)))) {
            return false;
        }
        if (isPersonalFlow(channel, appConfig) || isNormalFlow(channel, appConfig)) {
            return true;
        }
        return lowerCase.contains(r(100, 113, 255)) || lowerCase.contains(r(113, 126, 4108)) || lowerCase.contains(r(126, 137, 3919)) || lowerCase.contains(r(137, 148, 766)) || lowerCase.contains(r(148, 168, 7026)) || lowerCase.contains(r(168, 180, 64));
    }

    static String resolvePlayableUrl(Channel channel, AppConfig appConfig) {
        if (!requiresSigning(channel, appConfig)) {
            return channel.originalUrl;
        }
        Log.d(r(180, 191, -19811), r(191, 205, -21947) + channel.originalUrl);
        FlowTokenManager.TokenInfo freshToken = FlowTokenManager.getFreshToken(appConfig);
        if (freshToken == null || freshToken.host == null || freshToken.host.isEmpty() || freshToken.token == null || freshToken.token.isEmpty()) {
            Log.e(r(205, 216, -17455), r(216, 270, -19768));
            return channel.originalUrl;
        }
        String strExtractRelativePath = FlowTokenManager.extractRelativePath(channel.originalUrl);
        if (strExtractRelativePath == null || strExtractRelativePath.isEmpty()) {
            Log.w(r(270, 281, -22118), r(281, 318, -18707) + channel.originalUrl);
            return channel.originalUrl;
        }
        while (strExtractRelativePath.startsWith(r(318, 319, -29938))) {
            strExtractRelativePath = strExtractRelativePath.substring(1);
        }
        String str = r(319, 327, -29631) + freshToken.host + r(327, 328, -24518) + freshToken.token + r(328, 329, -28939) + strExtractRelativePath;
        Log.d(r(329, 340, -17481), r(340, 353, -21117) + str);
        return str;
    }
}
