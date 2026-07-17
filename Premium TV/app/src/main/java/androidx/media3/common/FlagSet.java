package androidx.media3.common;

import android.util.SparseBooleanArray;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class FlagSet {
    private final SparseBooleanArray flags;

    public static final class Builder {
        private boolean buildCalled;
        private final SparseBooleanArray flags = new SparseBooleanArray();

        public Builder add(int flag) {
            Assertions.checkState(!this.buildCalled);
            this.flags.append(flag, true);
            return this;
        }

        public Builder addIf(int flag, boolean condition) {
            if (condition) {
                return add(flag);
            }
            return this;
        }

        public Builder addAll(int... flags) {
            for (int flag : flags) {
                add(flag);
            }
            return this;
        }

        public Builder addAll(FlagSet flags) {
            for (int i = 0; i < flags.size(); i++) {
                add(flags.get(i));
            }
            return this;
        }

        public Builder remove(int flag) {
            Assertions.checkState(!this.buildCalled);
            this.flags.delete(flag);
            return this;
        }

        public Builder removeIf(int flag, boolean condition) {
            if (condition) {
                return remove(flag);
            }
            return this;
        }

        public Builder removeAll(int... flags) {
            for (int flag : flags) {
                remove(flag);
            }
            return this;
        }

        public FlagSet build() {
            Assertions.checkState(!this.buildCalled);
            this.buildCalled = true;
            return new FlagSet(this.flags);
        }
    }

    private FlagSet(SparseBooleanArray flags) {
        this.flags = flags;
    }

    public boolean contains(int flag) {
        return this.flags.get(flag);
    }

    public boolean containsAny(int... flags) {
        for (int flag : flags) {
            if (contains(flag)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return this.flags.size();
    }

    public int get(int index) {
        Assertions.checkIndex(index, 0, size());
        return this.flags.keyAt(index);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlagSet)) {
            return false;
        }
        FlagSet that = (FlagSet) o;
        if (Util.SDK_INT < 24) {
            if (size() != that.size()) {
                return false;
            }
            for (int i = 0; i < size(); i++) {
                if (get(i) != that.get(i)) {
                    return false;
                }
            }
            return true;
        }
        return this.flags.equals(that.flags);
    }

    public int hashCode() {
        if (Util.SDK_INT < 24) {
            int hashCode = size();
            for (int i = 0; i < size(); i++) {
                hashCode = (hashCode * 31) + get(i);
            }
            return hashCode;
        }
        return this.flags.hashCode();
    }
}
