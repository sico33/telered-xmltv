package com.playtv.premium;

import android.util.Log;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.extractor.ts.PsExtractor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import kotlin.jvm.internal.ShortCompanionObject;
import org.json.JSONArray;

/* JADX INFO: loaded from: classes2.dex */
final class FlowTokenManager {
    private static final String DEFAULT_SEED_URL = null;
    private static final int MAX_REDIRECTS = 5;
    private static final long SEEDS_TTL_MS = 86400000;
    private static volatile long cachedAt;
    private static volatile String cachedHost;
    private static volatile List<String> cachedSeeds;
    private static volatile String cachedTok;
    private static volatile long seedsAt;
    private static short[] E = {1863, 1856, 1820, 1799, 1795, 1847, 1843, 1846, 1863, 1845, 1859, 1857, 3992, 4060, 4057, 4038, 4053, 3999, 4051, 4076, 4052, 3995, 4053, 4052, 4035, 3999, 4075, 4078, 3983, 3987, 4077, 3994, 3993, 6792, 6851, 6908, 6852, 6795, 6853, 6852, 6867, 6799, 6907, 6910, 6815, 6787, 6909, 6794, 6793, 27564, 27568, 27581, 27557, 27560, 27562, 27555, 27578, 27568, 27571, 27563, 22486, 22517, 22512, 22452, 22488, 22523, 22519, 22517, 22496, 22525, 22523, 22522, 22452, 22524, 22513, 22517, 22512, 22513, 22502, 22446, 22452, 17925, 17928, 17925, 7635, 7638, 7625, 7642, 7568, 17325, 17329, 17340, 17316, 17321, 17323, 17314, 17339, 17329, 17330, 17322, 22070, 22032, 22026, 22029, 22020, 22083, 22016, 22018, 22016, 22027, 22022, 22023, 22083, 22048, 22055, 22061, 22083, 22039, 22028, 22024, 22022, 22029, 22083, 22091, 22018, 22020, 22022, 22110, 16632, 16614, 16572, 14928, 14924, 14913, 14937, 14932, 14934, 14943, 14918, 14924, 14927, 14935, 5345, 5366, 5365, 5345, 5366, 5344, 5371, 5330, 5344, 5354, 5373, 5360, 5299, 5366, 5345, 5345, 5372, 5345, 5289, 5299, 26211, 26217, 26218, 26226, 26202, 26230, 26208, 26208, 26209, 26202, 26224, 26231, 26217, 29023, 29013, 29014, 29006, 29030, 29002, 29020, 29020, 29021, 29030, 29016, 29020, 29002, 29030, 29010, 29020, 28992, 31332, 31282, 31334, 31280, 31342, 31279, 31330, 31333, 31277, 31334, 31330, 31285, 31333, 31266, 31330, 31331, 30097, 30135, 30113, 30134, 30185, 30085, 30115, 30113, 30122, 30128, 28939, 28938, 28937, 28942, 28954, 28931, 28955, 28976, 28954, 28956, 28938, 28957, 28976, 28942, 28936, 28938, 28929, 28955, 27663, 27699, 27710, 27686, 27659, 27657, 27663, 27693, 27706, 27698, 27702, 27690, 27698, 32218, 32198, 32203, 32211, 32222, 32220, 32213, 32204, 32198, 32197, 32221, ShortCompanionObject.MAX_VALUE, 32713, 32713, 32712, 32652, 32714, 32709, 32704, 32713, 32652, 32714, 32713, 32728, 32719, 32708, 32713, 32712, 32640, 32652, 32704, 32713, 32706, 32715, 32728, 32708, 32657, 25004, 25000, 25022, 25026, 25000, 25006, 25007, 25026, 25021, 24998, 25006, 25022, 25048, 25021, 24972, 24969, 24969, 24964, 24963, 24970, 32697, 32701, 32683, 27973, 27993, 27988, 27980, 27969, 27971, 27978, 27987, 27993, 27994, 27970, 20142, 20111, 20105, 20120, 20115, 20122, 20126, 20111, 20110, 20170, 20121, 20111, 20111, 20110, 20121, 20176, 20170, 29249, 29277, 29264, 29256, 29253, 29255, 29262, 29271, 29277, 29278, 29254, 28320, 28295, 28303, 28298, 28291, 28290, 28358, 28306, 28297, 28358, 28298, 28297, 28295, 28290, 28358, 28309, 28291, 28291, 28290, 28309, 28380, 28358, 19445, 19433, 19433, 19437, 19438, 19367, 19378, 19378, 19454, 19445, 19439, 19442, 19440, 19448, 19454, 19452, 19438, 19433, 19379, 19454, 19435, 19452, 19433, 19433, 19435, 19379, 19454, 19442, 19440, 19379, 19452, 19439, 19378, 19441, 19444, 19435, 19448, 19378, 19454, 19371, 19448, 19449, 19438, 19378, 19403, 19444, 19452, 19447, 19452, 19439, 19378, 19406, 19420, 19394, 19409, 19444, 19435, 19448, 19394, 19449, 19452, 19438, 19445, 19394, 19454, 19448, 19443, 19454, 19378, 19403, 19444, 19452, 19447, 19452, 19439, 19379, 19440, 19437, 19449, 29438, 29410, 29410, 29414, 29413, 29356, 29369, 29369, 29429, 29426, 29432, 29371, 29414, 29423, 29368, 29429, 29408, 29431, 29410, 29410, 29408, 29368, 29429, 29433, 29435, 29368, 29431, 29412, 29369, 29434, 29439, 29408, 29427, 29369, 29429, 29344, 29427, 29426, 29413, 29369, 29395, 29377, 29378, 29400, 29369, 29381, 29399, 29385, 29402, 29439, 29408, 29427, 29385, 29426, 29431, 29413, 29438, 29385, 29427, 29432, 29429, 29369, 29395, 29377, 29378, 29400, 29368, 29435, 29414, 29426, 25518, 25522, 25522, 25526, 25525, 25596, 25577, 25577, 25509, 25506, 25512, 25579, 25526, 25535, 25576, 25509, 25520, 25511, 25522, 25522, 25520, 25576, 25509, 25513, 25515, 25576, 25511, 25524, 25577, 25514, 25519, 25520, 25507, 25577, 25509, 25586, 25507, 25506, 25525, 25577, 25491, 25480, 25487, 25477, 25479, 25480, 25479, 25482, 25497, 25477, 25586, 25577, 25493, 25479, 25497, 25482, 25519, 25520, 25507, 25497, 25506, 25511, 25525, 25518, 25497, 25507, 25512, 25509, 25577, 25491, 25480, 25487, 25477, 25479, 25480, 25479, 25482, 25497, 25477, 25586, 25576, 25515, 25526, 25506, 26830, 26834, 26834, 26838, 26837, 26780, 26761, 26761, 26821, 26818, 26824, 26763, 26838, 26847, 26760, 26821, 26832, 26823, 26834, 26834, 26832, 26760, 26821, 26825, 26827, 26760, 26823, 26836, 26761, 26826, 26831, 26832, 26819, 26761, 26821, 26770, 26819, 26818, 26837, 26761, 26866, 26851, 26858, 26851, 26848, 26867, 26866, 26867, 26868, 26857, 26873, 26853, 26770, 26761, 26869, 26855, 26873, 26858, 26831, 26832, 26819, 26873, 26818, 26823, 26837, 26830, 26873, 26819, 26824, 26821, 26761, 26866, 26851, 26858, 26851, 26848, 26867, 26866, 26867, 26868, 26857, 26873, 26853, 26770, 26760, 26827, 26838, 26818, -26699, -26700, -26697, -26704, -26716, -26691, -26715, -26738, -26716, -26718, -26700, -26717, -26738, -26704, -26698, -26700, -26689, -26715, -17576, -17564, -17559, -17551, -17572, -17570, -17576, -17542, -17555, -17563, -17567, -17539, -17563, -23276, -23274, -23289, -22096, -22122, -22144, -22121, -22072, -22108, -22142, -22144, -22133, -22127, -21362, -21331, -21343, -21341, -21322, -21333, -21331, -21332, -16510, -16482, -16493, -16501, -16506, -16508, -16499, -16492, -16482, -16483, -16507, -23178, -23232, -23232, -23231, -23291, -23219, -23222, -23211, -23291, -27401, -27411, -21908, -21919, -21902, -21908, -22722, -22702, -22671, -22659, -22657, -22678, -22665, -22671, -22672, -22749, -28506, -28486, -28489, -28497, -28510, -28512, -28503, -28496, -28486, -28487, -28511, -24516, -24565, -24566, -24569, -24548, -24565, -24563, -24550, -24498, -24547, -24569, -24576, -24498, -24542, -24575, -24563, -24561, -24550, -24569, -24575, -24576, -24498, -24570, -24565, -24561, -24566, -24565, -24548, -24498, -24565, -24576, -24498, -24570, -24575, -24546, -24498, -26710, -26698, -26693, -26717, -26706, -26708, -26715, -26692, -26698, -26699, -26707, -17487, -17507, -17532, -17444, -17522, -17511, -17512, -17515, -17522, -17511, -17505, -17528, -17521, -17444, -17507, -17520, -17505, -17507, -17518, -17530, -17507, -17512, -17517, -17444, -17521, -17515, -17518, -17444, -17511, -17518, -17505, -17517, -17518, -17528, -17522, -17507, -17522, -17444, -17528, -17517, -17513, -17511, -17518, 700, 672, 685, 693, 696, 698, 691, 682, 672, 675, 699, 2180, 2195, 2192, 2180, 2195, 2181, 2206, 2231, 2181, 2191, 2200, 2197, 2252, 2262, 2199, 2202, 2180, 2195, 2199, 2194, 2191, 2262, 2180, 2195, 2192, 2180, 2195, 2181, 2206, 2207, 2200, 2193, 2266, 2262, 2181, 2205, 2207, 2182, 2182, 2207, 2200, 2193, 2852, 2830, 2829, 2837, 2870, 2829, 2825, 2823, 2828, 2864, 2823, 2820, 2832, 2823, 2833, 2826, -15245, -15249, -15262, -15238, -15241, -15243, -15236, -15259, -15249, -15252, -15244, -11427, -11413, -11413, -11414, -11474, -11429, -11428, -11454, -11395, -11468, -11474, -14538, -14550, -14553, -14529, -14542, -14544, -14535, -14560, -14550, -14551, -14543, -16088, -16128, -16101, -16049, -16101, -16128, -16124, -16118, -16127, -16049, -16121, -16128, -16100, -16101, -16046, -11748, -11704, -11693, 
    -11689, -11775, -14696, -14716, -14711, -14703, -14692, -14690, -14697, -14706, -14716, -14713, -14689, -11879, -11857, -11857, -11858, -11798, -11860, -11861, -11869, -11866, -11857, -11858, -11798, -12412, -12386, -9820, -9800, -9803, -9811, -9824, -9822, -9813, -9806, -9800, -9797, -9821, -9074, -9053, -9053, -8977, -9028, -9046, -9046, -9045, -9028, -8977, -9047, -9042, -9050, -9053, -9046, -9045, 9913, 9893, 9893, 9889, 9963, 9982, 9982, 12322, 12350, 12350, 12346, 12345, 12400, 12389, 12389, -18368, -18410, -18366, -18412, -18358, -18421, -18362, -18367, -18423, -18366, -18362, -18415, -18367, -18426, -18362, -18361, 20040, 20052, 20057, 20033, 20044, 20046, 20039, 20062, 20052, 20055, 20047};
    private static String DEFAULT_AES_KEY = E(1069, 1085, -18317);
    private static String TAG = E(1085, 1096, 19992);
    private static final Pattern TOK_PATTERN = Pattern.compile(E(0, 12, 1896));
    private static final Pattern PATH_PATTERN = Pattern.compile(E(12, 33, 4016));
    private static final Pattern PATH_PATTERN_ALT = Pattern.compile(E(33, 49, 6816));
    private static volatile long cacheTtlMs = new Random().nextInt(15000) + 45000;
    private static volatile boolean refreshing = false;

    interface RefreshCallback {
        void onRefreshed(TokenInfo tokenInfo);
    }

    static class TokenInfo {
        final String host;
        final String token;

        TokenInfo(String str, String str2) {
            this.host = str;
            this.token = str2;
        }
    }

    private FlowTokenManager() {
    }

    private static String E(int i, int i2, int i3) {
        char[] cArr = new char[i2 - i];
        for (int i4 = 0; i4 < i2 - i; i4++) {
            cArr[i4] = (char) (E[i + i4] ^ i3);
        }
        return new String(cArr);
    }

    private static TokenInfo extractFromLocation(String str) {
        try {
            URL url = new URL(str);
            String host = url.getHost();
            Matcher matcher = TOK_PATTERN.matcher(url.getPath());
            if (matcher.find()) {
                return new TokenInfo(host, matcher.group(1));
            }
        } catch (Exception e) {
            Log.w(E(49, 60, 27644), E(60, 81, 22420) + str + E(81, 84, 17957) + e.getMessage());
        }
        return null;
    }

    static String extractRelativePath(String str) {
        if (str == null) {
            return null;
        }
        Matcher matcher = PATH_PATTERN.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher matcher2 = PATH_PATTERN_ALT.matcher(str);
        if (matcher2.find()) {
            return E(84, 89, 7615) + matcher2.group(1);
        }
        return null;
    }

    static TokenInfo getFreshToken(AppConfig appConfig) {
        TokenInfo tokenInfoRefreshToken;
        synchronized (FlowTokenManager.class) {
            try {
                long jCurrentTimeMillis = System.currentTimeMillis();
                if (cachedHost == null || cachedTok == null || jCurrentTimeMillis - cachedAt >= cacheTtlMs) {
                    tokenInfoRefreshToken = refreshToken(appConfig, false);
                } else {
                    Log.d(E(89, 100, 17405), E(100, 128, 22115) + (jCurrentTimeMillis - cachedAt) + E(128, 131, 16533));
                    tokenInfoRefreshToken = new TokenInfo(cachedHost, cachedTok);
                }
            } finally {
            }
        }
        return tokenInfoRefreshToken;
    }

    static /* synthetic */ void lambda$refreshAsync$0(AppConfig appConfig, RefreshCallback refreshCallback) {
        try {
            try {
                TokenInfo tokenInfoRefreshToken = refreshToken(appConfig, true);
                if (refreshCallback != null) {
                    refreshCallback.onRefreshed(tokenInfoRefreshToken);
                }
            } catch (Exception e) {
                Log.e(E(131, 142, 14848), E(142, 162, 5267) + e.getMessage());
                if (refreshCallback != null) {
                    refreshCallback.onRefreshed(null);
                }
            }
        } finally {
            refreshing = false;
        }
    }

    private static List<String> loadSeeds(AppConfig appConfig) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (cachedSeeds != null && jCurrentTimeMillis - seedsAt < SEEDS_TTL_MS) {
            return cachedSeeds;
        }
        String str = appConfig.get(E(162, 175, 26117));
        if (str == null || str.isEmpty()) {
            str = null;
        }
        String strE = appConfig.get(E(175, PsExtractor.AUDIO_STREAM, 28985));
        if (strE == null || strE.isEmpty()) {
            strE = E(PsExtractor.AUDIO_STREAM, 208, 31319);
        }
        try {
            HashMap map = new HashMap();
            map.put(E(208, 218, 30148), appConfig.getOrDefault(E(218, 236, 29039), E(236, 249, 27743)));
            String strTrim = Net.get(str, map).trim();
            Log.d(E(249, 260, 32138), E(260, 286, 32684) + strTrim.length());
            byte[] bArrDecode = Base64.getMimeDecoder().decode(strTrim);
            byte[] bytes = strE.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance(E(286, 306, 25069));
            cipher.init(2, new SecretKeySpec(bytes, E(306, 309, 32760)));
            String strTrim2 = new String(cipher.doFinal(bArrDecode), StandardCharsets.UTF_8).trim();
            Log.d(E(309, 320, 27925), E(320, 337, 20202) + strTrim2);
            JSONArray jSONArray = new JSONArray(strTrim2);
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < jSONArray.length(); i++) {
                String strOptString = jSONArray.optString(i, "");
                if (!strOptString.isEmpty()) {
                    arrayList.add(strOptString);
                }
            }
            if (!arrayList.isEmpty()) {
                cachedSeeds = arrayList;
                seedsAt = jCurrentTimeMillis;
                return arrayList;
            }
        } catch (Exception e) {
            Log.e(E(337, 348, 29201), E(348, 370, 28390) + e.getMessage());
        }
        if (cachedSeeds != null) {
            return cachedSeeds;
        }
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(E(370, 449, 19357));
        arrayList2.add(E(449, 519, 29334));
        arrayList2.add(E(519, 603, 25542));
        arrayList2.add(E(603, 691, 26790));
        return arrayList2;
    }

    private static TokenInfo probeSeed(String str, AppConfig appConfig) throws Exception {
        TokenInfo tokenInfoExtractFromLocation;
        String strResolveRelative = str;
        int i = 0;
        String orDefault = appConfig.getOrDefault(E(691, 709, -26671), E(709, 722, -17656));
        while (true) {
            int i2 = i;
            if (i2 >= 5) {
                Log.w(E(826, 837, -26630), E(837, 880, -17412));
                return null;
            }
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(strResolveRelative).openConnection();
            try {
                httpURLConnection.setInstanceFollowRedirects(false);
                httpURLConnection.setRequestMethod(E(722, 725, -23213));
                httpURLConnection.setRequestProperty(E(725, 735, -22043), orDefault);
                httpURLConnection.setConnectTimeout(10000);
                httpURLConnection.setReadTimeout(10000);
                int responseCode = httpURLConnection.getResponseCode();
                String headerField = httpURLConnection.getHeaderField(E(735, 743, -21310));
                Log.d(E(743, 754, -16430), E(754, 763, -23259) + i2 + E(763, 765, -27443) + strResolveRelative + E(765, 769, -21940) + responseCode + E(769, 779, -22754) + headerField);
                if (headerField != null && !headerField.isEmpty() && (tokenInfoExtractFromLocation = extractFromLocation(headerField)) != null) {
                    httpURLConnection.disconnect();
                    return tokenInfoExtractFromLocation;
                }
                if (responseCode != 301 && responseCode != 302 && responseCode != 303 && responseCode != 307 && responseCode != 308) {
                    TokenInfo tokenInfoExtractFromLocation2 = extractFromLocation(strResolveRelative);
                    if (tokenInfoExtractFromLocation2 != null) {
                        httpURLConnection.disconnect();
                        return tokenInfoExtractFromLocation2;
                    }
                    httpURLConnection.disconnect();
                    return null;
                }
                if (headerField == null || headerField.isEmpty()) {
                    Log.w(E(779, 790, -28426), E(790, 826, -24466) + i2);
                    httpURLConnection.disconnect();
                    return null;
                }
                strResolveRelative = resolveRelative(strResolveRelative, headerField);
                httpURLConnection.disconnect();
                i = i2 + 1;
            } catch (Throwable th) {
                httpURLConnection.disconnect();
                throw th;
            }
        }
    }

    static void refreshAsync(final AppConfig appConfig, final RefreshCallback refreshCallback) {
        if (refreshing) {
            Log.d(E(880, 891, 748), E(891, 933, 2294));
        } else {
            refreshing = true;
            new Thread(new Runnable(appConfig, refreshCallback) { // from class: com.playtv.premium.FlowTokenManager$$ExternalSyntheticLambda0
                public final AppConfig f$0;
                public final FlowTokenManager.RefreshCallback f$1;

                {
                    this.f$0 = appConfig;
                    this.f$1 = refreshCallback;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    FlowTokenManager.lambda$refreshAsync$0(this.f$0, this.f$1);
                }
            }, E(933, 949, 2914)).start();
        }
    }

    static TokenInfo refreshToken(AppConfig appConfig, boolean z) {
        synchronized (FlowTokenManager.class) {
            if (z) {
                try {
                    cachedHost = null;
                    cachedTok = null;
                    cachedAt = 0L;
                } catch (Throwable th) {
                    throw th;
                }
            }
            List<String> listLoadSeeds = loadSeeds(appConfig);
            Log.d(E(949, 960, -15325), E(960, 971, -11506) + listLoadSeeds);
            for (String str : listLoadSeeds) {
                try {
                    TokenInfo tokenInfoProbeSeed = probeSeed(str, appConfig);
                    if (tokenInfoProbeSeed != null) {
                        cachedHost = tokenInfoProbeSeed.host;
                        cachedTok = tokenInfoProbeSeed.token;
                        cachedAt = System.currentTimeMillis();
                        cacheTtlMs = new Random().nextInt(15000) + 45000;
                        Log.d(E(971, 982, -14490), E(982, 997, -16017) + tokenInfoProbeSeed.host + E(997, 1002, -11716) + tokenInfoProbeSeed.token);
                        return tokenInfoProbeSeed;
                    }
                    continue;
                } catch (Exception e) {
                    Log.w(E(1002, 1013, -14648), E(1013, 1025, -11830) + str + E(1025, AnalyticsListener.EVENT_DRM_SESSION_RELEASED, -12354) + e.getMessage());
                }
            }
            Log.e(E(AnalyticsListener.EVENT_DRM_SESSION_RELEASED, 1038, -9740), E(1038, 1054, -9009));
            return null;
        }
    }

    private static String resolveRelative(String str, String str2) throws Exception {
        return (str2.startsWith(E(1054, 1061, 9937)) || str2.startsWith(E(1061, 1069, 12362))) ? str2 : new URL(new URL(str), str2).toString();
    }
}
