package androidx.media3.datasource.cache;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;

/* JADX INFO: loaded from: classes.dex */
final class CachedContent {
    private static final String TAG = "CachedContent";
    private final TreeSet<SimpleCacheSpan> cachedSpans;
    public final int id;
    public final String key;
    private final ArrayList<Range> lockedRanges;
    private DefaultContentMetadata metadata;

    public CachedContent(int id, String key) {
        this(id, key, DefaultContentMetadata.EMPTY);
    }

    public CachedContent(int id, String key, DefaultContentMetadata metadata) {
        this.id = id;
        this.key = key;
        this.metadata = metadata;
        this.cachedSpans = new TreeSet<>();
        this.lockedRanges = new ArrayList<>();
    }

    public DefaultContentMetadata getMetadata() {
        return this.metadata;
    }

    public boolean applyMetadataMutations(ContentMetadataMutations mutations) {
        DefaultContentMetadata oldMetadata = this.metadata;
        this.metadata = this.metadata.copyWithMutationsApplied(mutations);
        return !this.metadata.equals(oldMetadata);
    }

    public boolean isFullyUnlocked() {
        return this.lockedRanges.isEmpty();
    }

    public boolean isFullyLocked(long position, long length) {
        for (int i = 0; i < this.lockedRanges.size(); i++) {
            if (this.lockedRanges.get(i).contains(position, length)) {
                return true;
            }
        }
        return false;
    }

    public boolean lockRange(long position, long length) {
        int i = 0;
        while (true) {
            int size = this.lockedRanges.size();
            ArrayList<Range> arrayList = this.lockedRanges;
            if (i < size) {
                if (!arrayList.get(i).intersects(position, length)) {
                    i++;
                } else {
                    return false;
                }
            } else {
                arrayList.add(new Range(position, length));
                return true;
            }
        }
    }

    public void unlockRange(long position) {
        for (int i = 0; i < this.lockedRanges.size(); i++) {
            if (this.lockedRanges.get(i).position == position) {
                this.lockedRanges.remove(i);
                return;
            }
        }
        throw new IllegalStateException();
    }

    public void addSpan(SimpleCacheSpan span) {
        this.cachedSpans.add(span);
    }

    public TreeSet<SimpleCacheSpan> getSpans() {
        return this.cachedSpans;
    }

    public SimpleCacheSpan getSpan(long position, long length) {
        SimpleCacheSpan lookupSpan = SimpleCacheSpan.createLookup(this.key, position);
        SimpleCacheSpan floorSpan = this.cachedSpans.floor(lookupSpan);
        if (floorSpan != null && floorSpan.position + floorSpan.length > position) {
            return floorSpan;
        }
        SimpleCacheSpan ceilSpan = this.cachedSpans.ceiling(lookupSpan);
        if (ceilSpan != null) {
            long holeLength = ceilSpan.position - position;
            length = length == -1 ? holeLength : Math.min(holeLength, length);
        }
        return SimpleCacheSpan.createHole(this.key, position, length);
    }

    public long getCachedBytesLength(long position, long length) {
        Assertions.checkArgument(position >= 0);
        Assertions.checkArgument(length >= 0);
        SimpleCacheSpan span = getSpan(position, length);
        if (span.isHoleSpan()) {
            return -Math.min(span.isOpenEnded() ? Long.MAX_VALUE : span.length, length);
        }
        long queryEndPosition = position + length;
        if (queryEndPosition < 0) {
            queryEndPosition = Long.MAX_VALUE;
        }
        long currentEndPosition = span.position + span.length;
        if (currentEndPosition < queryEndPosition) {
            for (SimpleCacheSpan next : this.cachedSpans.tailSet(span, false)) {
                if (next.position > currentEndPosition) {
                    break;
                }
                currentEndPosition = Math.max(currentEndPosition, next.position + next.length);
                if (currentEndPosition >= queryEndPosition) {
                    break;
                }
            }
        }
        return Math.min(currentEndPosition - position, length);
    }

    public SimpleCacheSpan setLastTouchTimestamp(SimpleCacheSpan cacheSpan, long lastTouchTimestamp, boolean updateFile) {
        long lastTouchTimestamp2;
        Assertions.checkState(this.cachedSpans.remove(cacheSpan));
        File file = (File) Assertions.checkNotNull(cacheSpan.file);
        if (!updateFile) {
            lastTouchTimestamp2 = lastTouchTimestamp;
        } else {
            File directory = (File) Assertions.checkNotNull(file.getParentFile());
            long position = cacheSpan.position;
            lastTouchTimestamp2 = lastTouchTimestamp;
            File newFile = SimpleCacheSpan.getCacheFile(directory, this.id, position, lastTouchTimestamp2);
            if (file.renameTo(newFile)) {
                file = newFile;
            } else {
                Log.w(TAG, "Failed to rename " + file + " to " + newFile);
            }
        }
        SimpleCacheSpan newCacheSpan = cacheSpan.copyWithFileAndLastTouchTimestamp(file, lastTouchTimestamp2);
        this.cachedSpans.add(newCacheSpan);
        return newCacheSpan;
    }

    public boolean isEmpty() {
        return this.cachedSpans.isEmpty();
    }

    public boolean removeSpan(CacheSpan span) {
        if (this.cachedSpans.remove(span)) {
            if (span.file != null) {
                span.file.delete();
                return true;
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = this.id;
        return (((result * 31) + this.key.hashCode()) * 31) + this.metadata.hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CachedContent that = (CachedContent) o;
        if (this.id == that.id && this.key.equals(that.key) && this.cachedSpans.equals(that.cachedSpans) && this.metadata.equals(that.metadata)) {
            return true;
        }
        return false;
    }

    private static final class Range {
        public final long length;
        public final long position;

        public Range(long position, long length) {
            this.position = position;
            this.length = length;
        }

        public boolean contains(long otherPosition, long otherLength) {
            if (this.length == -1) {
                return otherPosition >= this.position;
            }
            if (otherLength == -1) {
                return false;
            }
            return this.position <= otherPosition && otherPosition + otherLength <= this.position + this.length;
        }

        public boolean intersects(long otherPosition, long otherLength) {
            if (this.position <= otherPosition) {
                return this.length == -1 || this.position + this.length > otherPosition;
            }
            return otherLength == -1 || otherPosition + otherLength > this.position;
        }
    }
}
