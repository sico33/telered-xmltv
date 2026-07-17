package androidx.media3.exoplayer.source;

import java.util.Arrays;
import java.util.Random;

/* JADX INFO: loaded from: classes.dex */
public interface ShuffleOrder {
    ShuffleOrder cloneAndClear();

    ShuffleOrder cloneAndInsert(int i, int i2);

    ShuffleOrder cloneAndRemove(int i, int i2);

    int getFirstIndex();

    int getLastIndex();

    int getLength();

    int getNextIndex(int i);

    int getPreviousIndex(int i);

    public static class DefaultShuffleOrder implements ShuffleOrder {
        private final int[] indexInShuffled;
        private final Random random;
        private final int[] shuffled;

        public DefaultShuffleOrder(int length) {
            this(length, new Random());
        }

        public DefaultShuffleOrder(int length, long randomSeed) {
            this(length, new Random(randomSeed));
        }

        public DefaultShuffleOrder(int[] shuffledIndices, long randomSeed) {
            this(Arrays.copyOf(shuffledIndices, shuffledIndices.length), new Random(randomSeed));
        }

        private DefaultShuffleOrder(int length, Random random) {
            this(createShuffledList(length, random), random);
        }

        private DefaultShuffleOrder(int[] shuffled, Random random) {
            this.shuffled = shuffled;
            this.random = random;
            this.indexInShuffled = new int[shuffled.length];
            for (int i = 0; i < shuffled.length; i++) {
                this.indexInShuffled[shuffled[i]] = i;
            }
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getLength() {
            return this.shuffled.length;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getNextIndex(int index) {
            int shuffledIndex = this.indexInShuffled[index] + 1;
            if (shuffledIndex < this.shuffled.length) {
                return this.shuffled[shuffledIndex];
            }
            return -1;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getPreviousIndex(int index) {
            int shuffledIndex = this.indexInShuffled[index] - 1;
            if (shuffledIndex >= 0) {
                return this.shuffled[shuffledIndex];
            }
            return -1;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getLastIndex() {
            if (this.shuffled.length > 0) {
                return this.shuffled[this.shuffled.length - 1];
            }
            return -1;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getFirstIndex() {
            if (this.shuffled.length > 0) {
                return this.shuffled[0];
            }
            return -1;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public ShuffleOrder cloneAndInsert(int insertionIndex, int insertionCount) {
            int[] insertionPoints = new int[insertionCount];
            int[] insertionValues = new int[insertionCount];
            for (int i = 0; i < insertionCount; i++) {
                insertionPoints[i] = this.random.nextInt(this.shuffled.length + 1);
                int swapIndex = this.random.nextInt(i + 1);
                insertionValues[i] = insertionValues[swapIndex];
                insertionValues[swapIndex] = i + insertionIndex;
            }
            Arrays.sort(insertionPoints);
            int[] newShuffled = new int[this.shuffled.length + insertionCount];
            int indexInOldShuffled = 0;
            int indexInInsertionList = 0;
            for (int i2 = 0; i2 < this.shuffled.length + insertionCount; i2++) {
                if (indexInInsertionList < insertionCount && indexInOldShuffled == insertionPoints[indexInInsertionList]) {
                    newShuffled[i2] = insertionValues[indexInInsertionList];
                    indexInInsertionList++;
                } else {
                    int indexInOldShuffled2 = indexInOldShuffled + 1;
                    newShuffled[i2] = this.shuffled[indexInOldShuffled];
                    if (newShuffled[i2] >= insertionIndex) {
                        newShuffled[i2] = newShuffled[i2] + insertionCount;
                    }
                    indexInOldShuffled = indexInOldShuffled2;
                }
            }
            return new DefaultShuffleOrder(newShuffled, new Random(this.random.nextLong()));
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public ShuffleOrder cloneAndRemove(int indexFrom, int indexToExclusive) {
            int numberOfElementsToRemove = indexToExclusive - indexFrom;
            int[] newShuffled = new int[this.shuffled.length - numberOfElementsToRemove];
            int foundElementsCount = 0;
            for (int i = 0; i < this.shuffled.length; i++) {
                if (this.shuffled[i] >= indexFrom && this.shuffled[i] < indexToExclusive) {
                    foundElementsCount++;
                } else {
                    int i2 = i - foundElementsCount;
                    int i3 = this.shuffled[i];
                    int[] iArr = this.shuffled;
                    newShuffled[i2] = i3 >= indexFrom ? iArr[i] - numberOfElementsToRemove : iArr[i];
                }
            }
            return new DefaultShuffleOrder(newShuffled, new Random(this.random.nextLong()));
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public ShuffleOrder cloneAndClear() {
            return new DefaultShuffleOrder(0, new Random(this.random.nextLong()));
        }

        private static int[] createShuffledList(int length, Random random) {
            int[] shuffled = new int[length];
            for (int i = 0; i < length; i++) {
                int swapIndex = random.nextInt(i + 1);
                shuffled[i] = shuffled[swapIndex];
                shuffled[swapIndex] = i;
            }
            return shuffled;
        }
    }

    public static final class UnshuffledShuffleOrder implements ShuffleOrder {
        private final int length;

        public UnshuffledShuffleOrder(int length) {
            this.length = length;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getLength() {
            return this.length;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getNextIndex(int index) {
            int index2 = index + 1;
            if (index2 < this.length) {
                return index2;
            }
            return -1;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getPreviousIndex(int index) {
            int index2 = index - 1;
            if (index2 >= 0) {
                return index2;
            }
            return -1;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getLastIndex() {
            if (this.length > 0) {
                return this.length - 1;
            }
            return -1;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public int getFirstIndex() {
            return this.length > 0 ? 0 : -1;
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public ShuffleOrder cloneAndInsert(int insertionIndex, int insertionCount) {
            return new UnshuffledShuffleOrder(this.length + insertionCount);
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public ShuffleOrder cloneAndRemove(int indexFrom, int indexToExclusive) {
            return new UnshuffledShuffleOrder((this.length - indexToExclusive) + indexFrom);
        }

        @Override // androidx.media3.exoplayer.source.ShuffleOrder
        public ShuffleOrder cloneAndClear() {
            return new UnshuffledShuffleOrder(0);
        }
    }
}
