package com.playtv.premium;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/* JADX INFO: loaded from: classes2.dex */
final class Net {
    private static short[] p = {-27422, -27394, -27394, -27398, -27510, -25391, -25397, -31491, -31519, -31519, -31515, -31595, -24962, -25043, -25033, -25040, -24962, -25027, -25045, -25029, -25044, -25042, -25039};

    private Net() {
    }

    static String get(String str, Map<String, String> map) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(15000);
        httpURLConnection.setInstanceFollowRedirects(true);
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        int responseCode = httpURLConnection.getResponseCode();
        InputStream errorStream = responseCode >= 400 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
        if (errorStream == null) {
            throw new IllegalStateException(p(7, 12, -31563) + responseCode + p(12, 23, -24994));
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
            try {
                StringBuilder sb = new StringBuilder();
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line).append('\n');
                }
                if (responseCode >= 400) {
                    throw new IllegalStateException(p(0, 5, -27478) + responseCode + p(5, 7, -25365) + ((Object) sb));
                }
                String string = sb.toString();
                bufferedReader.close();
                httpURLConnection.disconnect();
                return string;
            } catch (Throwable th) {
                try {
                    bufferedReader.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Throwable th3) {
            httpURLConnection.disconnect();
            throw th3;
        }
    }

    private static String p(int i, int i2, int i3) {
        char[] cArr = new char[i2 - i];
        for (int i4 = 0; i4 < i2 - i; i4++) {
            cArr[i4] = (char) (p[i + i4] ^ i3);
        }
        return new String(cArr);
    }
}
