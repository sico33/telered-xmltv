package androidx.media3.exoplayer.video;

import androidx.media3.common.C;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
final class FixedFrameRateEstimator {
    public static final int CONSECUTIVE_MATCHING_FRAME_DURATIONS_FOR_SYNC = 15;
    static final long MAX_MATCHING_FRAME_DIFFERENCE_NS = 1000000;
    private boolean candidateMatcherActive;
    private int framesWithoutSyncCount;
    private boolean switchToCandidateMatcherWhenSynced;
    private Matcher currentMatcher = new Matcher();
    private Matcher candidateMatcher = new Matcher();
    private long lastFramePresentationTimeNs = C.TIME_UNSET;

    public void reset() {
        this.currentMatcher.reset();
        this.candidateMatcher.reset();
        this.candidateMatcherActive = false;
        this.lastFramePresentationTimeNs = C.TIME_UNSET;
        this.framesWithoutSyncCount = 0;
    }

    public void onNextFrame(long framePresentationTimeNs) {
        this.currentMatcher.onNextFrame(framePresentationTimeNs);
        if (this.currentMatcher.isSynced() && !this.switchToCandidateMatcherWhenSynced) {
            this.candidateMatcherActive = false;
        } else if (this.lastFramePresentationTimeNs != C.TIME_UNSET) {
            if (!this.candidateMatcherActive || this.candidateMatcher.isLastFrameOutlier()) {
                this.candidateMatcher.reset();
                this.candidateMatcher.onNextFrame(this.lastFramePresentationTimeNs);
            }
            this.candidateMatcherActive = true;
            this.candidateMatcher.onNextFrame(framePresentationTimeNs);
        }
        if (this.candidateMatcherActive && this.candidateMatcher.isSynced()) {
            Matcher previousMatcher = this.currentMatcher;
            this.currentMatcher = this.candidateMatcher;
            this.candidateMatcher = previousMatcher;
            this.candidateMatcherActive = false;
            this.switchToCandidateMatcherWhenSynced = false;
        }
        this.lastFramePresentationTimeNs = framePresentationTimeNs;
        this.framesWithoutSyncCount = this.currentMatcher.isSynced() ? 0 : this.framesWithoutSyncCount + 1;
    }

    public boolean isSynced() {
        return this.currentMatcher.isSynced();
    }

    public int getFramesWithoutSyncCount() {
        return this.framesWithoutSyncCount;
    }

    public long getMatchingFrameDurationSumNs() {
        return isSynced() ? this.currentMatcher.getMatchingFrameDurationSumNs() : C.TIME_UNSET;
    }

    public long getFrameDurationNs() {
        return isSynced() ? this.currentMatcher.getFrameDurationNs() : C.TIME_UNSET;
    }

    public float getFrameRate() {
        if (isSynced()) {
            return (float) (1.0E9d / this.currentMatcher.getFrameDurationNs());
        }
        return -1.0f;
    }

    private static final class Matcher {
        private long firstFrameDurationNs;
        private long firstFramePresentationTimeNs;
        private long frameCount;
        private long lastFramePresentationTimeNs;
        private long matchingFrameCount;
        private long matchingFrameDurationSumNs;
        private int recentFrameOutlierCount;
        private final boolean[] recentFrameOutlierFlags = new boolean[15];

        public void reset() {
            this.frameCount = 0L;
            this.matchingFrameCount = 0L;
            this.matchingFrameDurationSumNs = 0L;
            this.recentFrameOutlierCount = 0;
            Arrays.fill(this.recentFrameOutlierFlags, false);
        }

        public boolean isSynced() {
            return this.frameCount > 15 && this.recentFrameOutlierCount == 0;
        }

        public boolean isLastFrameOutlier() {
            if (this.frameCount == 0) {
                return false;
            }
            return this.recentFrameOutlierFlags[getRecentFrameOutlierIndex(this.frameCount - 1)];
        }

        public long getMatchingFrameDurationSumNs() {
            return this.matchingFrameDurationSumNs;
        }

        public long getFrameDurationNs() {
            if (this.matchingFrameCount == 0) {
                return 0L;
            }
            return this.matchingFrameDurationSumNs / this.matchingFrameCount;
        }

        public void onNextFrame(long framePresentationTimeNs) {
            if (this.frameCount == 0) {
                this.firstFramePresentationTimeNs = framePresentationTimeNs;
            } else if (this.frameCount == 1) {
                this.firstFrameDurationNs = framePresentationTimeNs - this.firstFramePresentationTimeNs;
                this.matchingFrameDurationSumNs = this.firstFrameDurationNs;
                this.matchingFrameCount = 1L;
            } else {
                long lastFrameDurationNs = framePresentationTimeNs - this.lastFramePresentationTimeNs;
                int recentFrameOutlierIndex = getRecentFrameOutlierIndex(this.frameCount);
                if (Math.abs(lastFrameDurationNs - this.firstFrameDurationNs) <= 1000000) {
                    this.matchingFrameCount++;
                    this.matchingFrameDurationSumNs += lastFrameDurationNs;
                    if (this.recentFrameOutlierFlags[recentFrameOutlierIndex]) {
                        this.recentFrameOutlierFlags[recentFrameOutlierIndex] = false;
                        this.recentFrameOutlierCount--;
                    }
                } else if (!this.recentFrameOutlierFlags[recentFrameOutlierIndex]) {
                    this.recentFrameOutlierFlags[recentFrameOutlierIndex] = true;
                    this.recentFrameOutlierCount++;
                }
            }
            this.frameCount++;
            this.lastFramePresentationTimeNs = framePresentationTimeNs;
        }

        private static int getRecentFrameOutlierIndex(long frameCount) {
            return (int) (frameCount % 15);
        }
    }
}
