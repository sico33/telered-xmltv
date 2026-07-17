package androidx.media3.datasource;

import android.text.TextUtils;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public final class HttpUtil {
    private static final String TAG = "HttpUtil";
    private static final Pattern CONTENT_RANGE_WITH_START_AND_END = Pattern.compile("bytes (\\d+)-(\\d+)/(?:\\d+|\\*)");
    private static final Pattern CONTENT_RANGE_WITH_SIZE = Pattern.compile("bytes (?:(?:\\d+-\\d+)|\\*)/(\\d+)");

    private HttpUtil() {
    }

    public static String buildRangeRequestHeader(long position, long length) {
        if (position == 0 && length == -1) {
            return null;
        }
        StringBuilder rangeValue = new StringBuilder();
        rangeValue.append("bytes=");
        rangeValue.append(position);
        rangeValue.append("-");
        if (length != -1) {
            rangeValue.append((position + length) - 1);
        }
        return rangeValue.toString();
    }

    public static long getDocumentSize(String contentRangeHeader) {
        if (TextUtils.isEmpty(contentRangeHeader)) {
            return -1L;
        }
        Matcher matcher = CONTENT_RANGE_WITH_SIZE.matcher(contentRangeHeader);
        if (matcher.matches()) {
            return Long.parseLong((String) Assertions.checkNotNull(matcher.group(1)));
        }
        return -1L;
    }

    public static long getContentLength(String contentLengthHeader, String contentRangeHeader) {
        long contentLength = -1;
        if (!TextUtils.isEmpty(contentLengthHeader)) {
            try {
                contentLength = Long.parseLong(contentLengthHeader);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Unexpected Content-Length [" + contentLengthHeader + "]");
            }
        }
        if (!TextUtils.isEmpty(contentRangeHeader)) {
            Matcher matcher = CONTENT_RANGE_WITH_START_AND_END.matcher(contentRangeHeader);
            if (matcher.matches()) {
                try {
                    long contentLengthFromRange = (Long.parseLong((String) Assertions.checkNotNull(matcher.group(2))) - Long.parseLong((String) Assertions.checkNotNull(matcher.group(1)))) + 1;
                    if (contentLength < 0) {
                        return contentLengthFromRange;
                    }
                    if (contentLength != contentLengthFromRange) {
                        Log.w(TAG, "Inconsistent headers [" + contentLengthHeader + "] [" + contentRangeHeader + "]");
                        return Math.max(contentLength, contentLengthFromRange);
                    }
                    return contentLength;
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "Unexpected Content-Range [" + contentRangeHeader + "]");
                    return contentLength;
                }
            }
            return contentLength;
        }
        return contentLength;
    }
}
