package com.playtv.premium;

import android.content.Context;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
final class CatalogLoader {
    private static short[] W = {-11731, -11765, -11747, -11766, -11691, -11719, -11745, -11747, -11754, -11764, -8888, -8887, -8886, -8883, -8871, -8896, -8872, -8845, -8871, -8865, -8887, -8866, -8845, -8883, -8885, -8887, -8894, -8872, -13155, -13151, -13140, -13132, -13159, -13157, -13155, -13121, -13144, -13152, -13148, -13128, -13152, -10239, -10236, -10237, -10234, -10190, -10216, -10209, -10239, -10898, -10894, -10894, -10890, -10891, -10948, -10967, -10967, -10905, -10892, -10907, -10898, -10897, -10896, -10909, -10968, -10903, -10892, -10911, -10967, -10910, -10903, -10895, -10904, -10902, -10903, -10905, -10910, -10967, -10890, -10892, -10893, -10909, -10908, -10905, -10945, -10919, -10956, -10954, -10956, -10960, -10954, -10959, -10967, -10890, -10892, -10893, -10909, -10908, -10905, -10968, -10945, -10967, -10890, -10892, -10893, -10909, -10908, -10905, -10945, -10968, -10900, -10891, -10903, -10904, -10167, -10165, -10146, -10161, -10163, -10171, -10152, -10173, -10161, -10151, -13402, -13399, -13403, -13395, -15937, -15971, -15992, -15975, -15973, -15981, -15986, -15979, -15971, -6501, -6519, -6523, -6504, -6524, -6515, -6501, 16742, 16756, 16756, 16738, 16755, 16701, 16680, 16680, 29605, 29623, 29623, 29601, 29616, 29694, 29675, 29675};

    private CatalogLoader() {
    }

    private static String W(int i, int i2, int i3) {
        char[] cArr = new char[i2 - i];
        for (int i4 = 0; i4 < i2 - i; i4++) {
            cArr[i4] = (char) (W[i + i4] ^ i3);
        }
        return new String(cArr);
    }

    static List<Category> load(Context context, AppConfig appConfig) throws Exception {
        HashMap map = new HashMap();
        map.put(W(0, 10, -11656), appConfig.getOrDefault(W(10, 28, -8916), W(28, 41, -13107)));
        appConfig.get(W(41, 49, -10131));
        JSONArray jSONArrayOptJSONArray = new JSONObject(loadCatalogBody(context, W(49, 114, -11002), map)).optJSONArray(W(114, 124, -10198));
        ArrayList arrayList = new ArrayList();
        if (jSONArrayOptJSONArray == null) {
            return arrayList;
        }
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i);
            Category category = new Category(jSONObject.optString(W(124, 128, -13368), W(128, 137, -15876)));
            JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray(W(137, 144, -6424));
            if (jSONArrayOptJSONArray2 != null) {
                for (int i2 = 0; i2 < jSONArrayOptJSONArray2.length(); i2++) {
                    category.channels.add(Channel.fromJson(jSONArrayOptJSONArray2.getJSONObject(i2), category.name));
                }
            }
            if (!category.channels.isEmpty()) {
                arrayList.add(category);
            }
        }
        return arrayList;
    }

    private static String loadCatalogBody(Context context, String str, Map<String, String> map) throws Exception {
        if (str == null || !str.startsWith(W(144, 152, 16647))) {
            return Net.get(str, map);
        }
        InputStream inputStreamOpen = context.getAssets().open(str.substring(W(152, 160, 29636).length()));
        try {
            byte[] bArr = new byte[inputStreamOpen.available()];
            String str2 = new String(bArr, 0, inputStreamOpen.read(bArr), StandardCharsets.UTF_8);
            if (inputStreamOpen == null) {
                return str2;
            }
            inputStreamOpen.close();
            return str2;
        } catch (Throwable th) {
            if (inputStreamOpen != null) {
                try {
                    inputStreamOpen.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }
}
