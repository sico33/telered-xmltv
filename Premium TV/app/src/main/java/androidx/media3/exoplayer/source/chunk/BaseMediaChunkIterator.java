package androidx.media3.exoplayer.source.chunk;

import java.util.NoSuchElementException;

/* JADX INFO: loaded from: classes.dex */
public abstract class BaseMediaChunkIterator implements MediaChunkIterator {
    private long currentIndex;
    private final long fromIndex;
    private final long toIndex;

    public BaseMediaChunkIterator(long fromIndex, long toIndex) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        reset();
    }

    @Override // androidx.media3.exoplayer.source.chunk.MediaChunkIterator
    public boolean isEnded() {
        return this.currentIndex > this.toIndex;
    }

    @Override // androidx.media3.exoplayer.source.chunk.MediaChunkIterator
    public boolean next() {
        this.currentIndex++;
        return !isEnded();
    }

    @Override // androidx.media3.exoplayer.source.chunk.MediaChunkIterator
    public void reset() {
        this.currentIndex = this.fromIndex - 1;
    }

    protected final void checkInBounds() {
        if (this.currentIndex < this.fromIndex || this.currentIndex > this.toIndex) {
            throw new NoSuchElementException();
        }
    }

    protected final long getCurrentIndex() {
        return this.currentIndex;
    }
}
