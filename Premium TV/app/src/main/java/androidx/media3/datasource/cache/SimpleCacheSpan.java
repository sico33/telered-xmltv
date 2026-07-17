package androidx.media3.datasource.cache;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class SimpleCacheSpan extends CacheSpan {
    private static final Pattern CACHE_FILE_PATTERN_V1 = Pattern.compile("^(.+)\\.(\\d+)\\.(\\d+)\\.v1\\.exo$", 32);
    private static final Pattern CACHE_FILE_PATTERN_V2 = Pattern.compile("^(.+)\\.(\\d+)\\.(\\d+)\\.v2\\.exo$", 32);
    private static final Pattern CACHE_FILE_PATTERN_V3 = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.v3\\.exo$", 32);
    static final String COMMON_SUFFIX = ".exo";
    private static final String SUFFIX = ".v3.exo";

    public static File getCacheFile(File cacheDir, int id, long position, long timestamp) {
        return new File(cacheDir, id + "." + position + "." + timestamp + SUFFIX);
    }

    public static SimpleCacheSpan createLookup(String key, long position) {
        return new SimpleCacheSpan(key, position, -1L, C.TIME_UNSET, null);
    }

    public static SimpleCacheSpan createHole(String key, long position, long length) {
        return new SimpleCacheSpan(key, position, length, C.TIME_UNSET, null);
    }

    public static SimpleCacheSpan createCacheEntry(File file, long length, CachedContentIndex index) {
        return createCacheEntry(file, length, C.TIME_UNSET, index);
    }

    public static SimpleCacheSpan createCacheEntry(File file, long length, long lastTouchTimestamp, CachedContentIndex index) {
        File file2;
        long length2;
        long lastTouchTimestamp2;
        String name = file.getName();
        if (name.endsWith(SUFFIX)) {
            file2 = file;
        } else {
            File upgradedFile = upgradeFile(file, index);
            if (upgradedFile == null) {
                return null;
            }
            name = upgradedFile.getName();
            file2 = upgradedFile;
        }
        Matcher matcher = CACHE_FILE_PATTERN_V3.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        int id = Integer.parseInt((String) Assertions.checkNotNull(matcher.group(1)));
        String key = index.getKeyForId(id);
        if (key == null) {
            return null;
        }
        if (length != -1) {
            length2 = length;
        } else {
            length2 = file2.length();
        }
        if (length2 == 0) {
            return null;
        }
        long position = Long.parseLong((String) Assertions.checkNotNull(matcher.group(2)));
        if (lastTouchTimestamp != C.TIME_UNSET) {
            lastTouchTimestamp2 = lastTouchTimestamp;
        } else {
            lastTouchTimestamp2 = Long.parseLong((String) Assertions.checkNotNull(matcher.group(3)));
        }
        return new SimpleCacheSpan(key, position, length2, lastTouchTimestamp2, file2);
    }

    private static File upgradeFile(File file, CachedContentIndex index) {
        String key = null;
        String filename = file.getName();
        Matcher matcher = CACHE_FILE_PATTERN_V2.matcher(filename);
        if (matcher.matches()) {
            key = Util.unescapeFileName((String) Assertions.checkNotNull(matcher.group(1)));
        } else {
            matcher = CACHE_FILE_PATTERN_V1.matcher(filename);
            if (matcher.matches()) {
                key = (String) Assertions.checkNotNull(matcher.group(1));
            }
        }
        if (key == null) {
            return null;
        }
        File newCacheFile = getCacheFile((File) Assertions.checkStateNotNull(file.getParentFile()), index.assignIdForKey(key), Long.parseLong((String) Assertions.checkNotNull(matcher.group(2))), Long.parseLong((String) Assertions.checkNotNull(matcher.group(3))));
        if (!file.renameTo(newCacheFile)) {
            return null;
        }
        return newCacheFile;
    }

    private SimpleCacheSpan(String key, long position, long length, long lastTouchTimestamp, File file) {
        super(key, position, length, lastTouchTimestamp, file);
    }

    public SimpleCacheSpan copyWithFileAndLastTouchTimestamp(File file, long lastTouchTimestamp) {
        Assertions.checkState(this.isCached);
        return new SimpleCacheSpan(this.key, this.position, this.length, lastTouchTimestamp, file);
    }
}
