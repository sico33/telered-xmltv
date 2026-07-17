package androidx.media3.exoplayer.source;

import android.util.SparseArray;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Consumer;

/* JADX INFO: loaded from: classes.dex */
final class SpannedData<V> {
    private int memoizedReadIndex;
    private final Consumer<V> removeCallback;
    private final SparseArray<V> spans;

    public SpannedData() {
        this(new Consumer() { // from class: androidx.media3.exoplayer.source.SpannedData$$ExternalSyntheticLambda0
            @Override // androidx.media3.common.util.Consumer
            public final void accept(Object obj) {
                SpannedData.lambda$new$0(obj);
            }
        });
    }

    static /* synthetic */ void lambda$new$0(Object value) {
    }

    public SpannedData(Consumer<V> removeCallback) {
        this.spans = new SparseArray<>();
        this.removeCallback = removeCallback;
        this.memoizedReadIndex = -1;
    }

    public V get(int key) {
        if (this.memoizedReadIndex == -1) {
            this.memoizedReadIndex = 0;
        }
        while (this.memoizedReadIndex > 0 && key < this.spans.keyAt(this.memoizedReadIndex)) {
            this.memoizedReadIndex--;
        }
        while (this.memoizedReadIndex < this.spans.size() - 1 && key >= this.spans.keyAt(this.memoizedReadIndex + 1)) {
            this.memoizedReadIndex++;
        }
        return this.spans.valueAt(this.memoizedReadIndex);
    }

    public void appendSpan(int i, V v) {
        if (this.memoizedReadIndex == -1) {
            Assertions.checkState(this.spans.size() == 0);
            this.memoizedReadIndex = 0;
        }
        if (this.spans.size() > 0) {
            int iKeyAt = this.spans.keyAt(this.spans.size() - 1);
            Assertions.checkArgument(i >= iKeyAt);
            if (iKeyAt == i) {
                this.removeCallback.accept(this.spans.valueAt(this.spans.size() - 1));
            }
        }
        this.spans.append(i, v);
    }

    public V getEndValue() {
        return this.spans.valueAt(this.spans.size() - 1);
    }

    public void discardTo(int i) {
        for (int i2 = 0; i2 < this.spans.size() - 1 && i >= this.spans.keyAt(i2 + 1); i2++) {
            this.removeCallback.accept(this.spans.valueAt(i2));
            this.spans.removeAt(i2);
            if (this.memoizedReadIndex > 0) {
                this.memoizedReadIndex--;
            }
        }
    }

    public void discardFrom(int i) {
        for (int size = this.spans.size() - 1; size >= 0 && i < this.spans.keyAt(size); size--) {
            this.removeCallback.accept(this.spans.valueAt(size));
            this.spans.removeAt(size);
        }
        this.memoizedReadIndex = this.spans.size() > 0 ? Math.min(this.memoizedReadIndex, this.spans.size() - 1) : -1;
    }

    public void clear() {
        for (int i = 0; i < this.spans.size(); i++) {
            this.removeCallback.accept(this.spans.valueAt(i));
        }
        this.memoizedReadIndex = -1;
        this.spans.clear();
    }

    public boolean isEmpty() {
        return this.spans.size() == 0;
    }
}
