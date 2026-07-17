package com.playtv.premium;

import android.content.Context;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
final class AppConfig {
    private static short[] B = {-11443, -11435, -11448, -11434, -11426, -11442, -11444, -11508, -11500, -11440, -11447, -11435, -11436};
    final JSONObject json;

    private AppConfig(JSONObject jSONObject) {
        this.json = jSONObject;
    }

    private static String B(int i, int i2, int i3) {
        char[] cArr = new char[i2 - i];
        for (int i4 = 0; i4 < i2 - i; i4++) {
            cArr[i4] = (char) (B[i + i4] ^ i3);
        }
        return new String(cArr);
    }

    static AppConfig load(Context context) throws Exception {
        InputStream inputStreamOpen = context.getAssets().open(B(0, 13, -11462));
        try {
            byte[] bArr = new byte[inputStreamOpen.available()];
            AppConfig appConfig = new AppConfig(new JSONObject(new String(bArr, 0, inputStreamOpen.read(bArr), StandardCharsets.UTF_8)));
            if (inputStreamOpen != null) {
                inputStreamOpen.close();
            }
            return appConfig;
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

    String get(String str) {
        return this.json.optString(str, "");
    }

    String getOrDefault(String str, String str2) {
        String str3 = get(str);
        return str3.isEmpty() ? str2 : str3;
    }

    void put(String str, String str2) {
        try {
            this.json.put(str, str2);
        } catch (Exception e) {
        }
    }
}
