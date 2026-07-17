package androidx.media3.common.util;

import android.net.Uri;
import android.text.TextUtils;
import com.google.common.base.Ascii;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class UriUtil {
    private static final int FRAGMENT = 3;
    private static final int INDEX_COUNT = 4;
    private static final int PATH = 1;
    private static final int QUERY = 2;
    private static final int SCHEME_COLON = 0;

    private UriUtil() {
    }

    public static Uri resolveToUri(String baseUri, String referenceUri) {
        return Uri.parse(resolve(baseUri, referenceUri));
    }

    public static String resolve(String baseUri, String referenceUri) {
        StringBuilder uri = new StringBuilder();
        String baseUri2 = baseUri == null ? "" : baseUri;
        String referenceUri2 = referenceUri != null ? referenceUri : "";
        int[] refIndices = getUriIndices(referenceUri2);
        if (refIndices[0] != -1) {
            uri.append(referenceUri2);
            removeDotSegments(uri, refIndices[1], refIndices[2]);
            return uri.toString();
        }
        int[] baseIndices = getUriIndices(baseUri2);
        if (refIndices[3] == 0) {
            return uri.append((CharSequence) baseUri2, 0, baseIndices[3]).append(referenceUri2).toString();
        }
        if (refIndices[2] == 0) {
            return uri.append((CharSequence) baseUri2, 0, baseIndices[2]).append(referenceUri2).toString();
        }
        if (refIndices[1] != 0) {
            int baseLimit = baseIndices[0] + 1;
            uri.append((CharSequence) baseUri2, 0, baseLimit).append(referenceUri2);
            return removeDotSegments(uri, refIndices[1] + baseLimit, refIndices[2] + baseLimit);
        }
        if (referenceUri2.charAt(refIndices[1]) == '/') {
            uri.append((CharSequence) baseUri2, 0, baseIndices[1]).append(referenceUri2);
            return removeDotSegments(uri, baseIndices[1], baseIndices[1] + refIndices[2]);
        }
        if (baseIndices[0] + 2 < baseIndices[1] && baseIndices[1] == baseIndices[2]) {
            uri.append((CharSequence) baseUri2, 0, baseIndices[1]).append('/').append(referenceUri2);
            return removeDotSegments(uri, baseIndices[1], baseIndices[1] + refIndices[2] + 1);
        }
        int lastSlashIndex = baseUri2.lastIndexOf(47, baseIndices[2] - 1);
        int baseLimit2 = lastSlashIndex == -1 ? baseIndices[1] : lastSlashIndex + 1;
        uri.append((CharSequence) baseUri2, 0, baseLimit2).append(referenceUri2);
        return removeDotSegments(uri, baseIndices[1], refIndices[2] + baseLimit2);
    }

    public static boolean isAbsolute(String uri) {
        return (uri == null || getUriIndices(uri)[0] == -1) ? false : true;
    }

    public static Uri removeQueryParameter(Uri uri, String queryParameterName) {
        Uri.Builder builder = uri.buildUpon();
        builder.clearQuery();
        for (String key : uri.getQueryParameterNames()) {
            if (!key.equals(queryParameterName)) {
                for (String value : uri.getQueryParameters(key)) {
                    builder.appendQueryParameter(key, value);
                }
            }
        }
        return builder.build();
    }

    private static String removeDotSegments(StringBuilder uri, int offset, int limit) {
        int nextSegmentStart;
        if (offset >= limit) {
            return uri.toString();
        }
        if (uri.charAt(offset) == '/') {
            offset++;
        }
        int segmentStart = offset;
        int i = offset;
        while (i <= limit) {
            if (i == limit) {
                nextSegmentStart = i;
            } else {
                int nextSegmentStart2 = uri.charAt(i);
                if (nextSegmentStart2 == 47) {
                    nextSegmentStart = i + 1;
                } else {
                    i++;
                }
            }
            if (i == segmentStart + 1 && uri.charAt(segmentStart) == '.') {
                uri.delete(segmentStart, nextSegmentStart);
                limit -= nextSegmentStart - segmentStart;
                i = segmentStart;
            } else if (i == segmentStart + 2 && uri.charAt(segmentStart) == '.' && uri.charAt(segmentStart + 1) == '.') {
                int prevSegmentStart = uri.lastIndexOf("/", segmentStart - 2) + 1;
                int removeFrom = prevSegmentStart > offset ? prevSegmentStart : offset;
                uri.delete(removeFrom, nextSegmentStart);
                limit -= nextSegmentStart - removeFrom;
                segmentStart = prevSegmentStart;
                i = prevSegmentStart;
            } else {
                i++;
                segmentStart = i;
            }
        }
        return uri.toString();
    }

    private static int[] getUriIndices(String uriString) {
        int pathIndex;
        int[] indices = new int[4];
        if (TextUtils.isEmpty(uriString)) {
            indices[0] = -1;
            return indices;
        }
        int length = uriString.length();
        int fragmentIndex = uriString.indexOf(35);
        if (fragmentIndex == -1) {
            fragmentIndex = length;
        }
        int queryIndex = uriString.indexOf(63);
        if (queryIndex == -1 || queryIndex > fragmentIndex) {
            queryIndex = fragmentIndex;
        }
        int schemeIndexLimit = uriString.indexOf(47);
        if (schemeIndexLimit == -1 || schemeIndexLimit > queryIndex) {
            schemeIndexLimit = queryIndex;
        }
        int schemeIndex = uriString.indexOf(58);
        if (schemeIndex > schemeIndexLimit) {
            schemeIndex = -1;
        }
        boolean hasAuthority = schemeIndex + 2 < queryIndex && uriString.charAt(schemeIndex + 1) == '/' && uriString.charAt(schemeIndex + 2) == '/';
        if (hasAuthority) {
            pathIndex = uriString.indexOf(47, schemeIndex + 3);
            if (pathIndex == -1 || pathIndex > queryIndex) {
                pathIndex = queryIndex;
            }
        } else {
            pathIndex = schemeIndex + 1;
        }
        indices[0] = schemeIndex;
        indices[1] = pathIndex;
        indices[2] = queryIndex;
        indices[3] = fragmentIndex;
        return indices;
    }

    public static String getRelativePath(Uri baseUri, Uri targetUri) {
        if (baseUri.isOpaque() || targetUri.isOpaque()) {
            return targetUri.toString();
        }
        String baseUriScheme = baseUri.getScheme();
        String targetUriScheme = targetUri.getScheme();
        boolean isSameScheme = false;
        if (baseUriScheme == null) {
            if (targetUriScheme == null) {
                isSameScheme = true;
            }
        } else if (targetUriScheme != null && Ascii.equalsIgnoreCase(baseUriScheme, targetUriScheme)) {
            isSameScheme = true;
        }
        if (!isSameScheme || !Util.areEqual(baseUri.getAuthority(), targetUri.getAuthority())) {
            return targetUri.toString();
        }
        List<String> basePathSegments = baseUri.getPathSegments();
        List<String> targetPathSegments = targetUri.getPathSegments();
        int commonPrefixCount = 0;
        int minSize = Math.min(basePathSegments.size(), targetPathSegments.size());
        for (int i = 0; i < minSize && basePathSegments.get(i).equals(targetPathSegments.get(i)); i++) {
            commonPrefixCount++;
        }
        StringBuilder relativePath = new StringBuilder();
        for (int i2 = commonPrefixCount; i2 < basePathSegments.size(); i2++) {
            relativePath.append("../");
        }
        for (int i3 = commonPrefixCount; i3 < targetPathSegments.size(); i3++) {
            relativePath.append(targetPathSegments.get(i3));
            if (i3 < targetPathSegments.size() - 1) {
                relativePath.append("/");
            }
        }
        return relativePath.toString();
    }
}
