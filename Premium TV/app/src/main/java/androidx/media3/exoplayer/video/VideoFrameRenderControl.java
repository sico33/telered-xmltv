package androidx.media3.exoplayer.video;

import androidx.media3.common.C;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.LongArrayQueue;
import androidx.media3.common.util.TimedValueQueue;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlaybackException;

/* JADX INFO: loaded from: classes.dex */
final class VideoFrameRenderControl {
    private final FrameRenderer frameRenderer;
    private long outputStreamOffsetUs;
    private VideoSize pendingOutputVideoSize;
    private final VideoFrameReleaseControl videoFrameReleaseControl;
    private final VideoFrameReleaseControl.FrameReleaseInfo videoFrameReleaseInfo = new VideoFrameReleaseControl.FrameReleaseInfo();
    private final TimedValueQueue<VideoSize> videoSizeChanges = new TimedValueQueue<>();
    private final TimedValueQueue<Long> streamOffsets = new TimedValueQueue<>();
    private final LongArrayQueue presentationTimestampsUs = new LongArrayQueue();
    private VideoSize reportedVideoSize = VideoSize.UNKNOWN;
    private long lastPresentationTimeUs = C.TIME_UNSET;

    interface FrameRenderer {
        void dropFrame();

        void onVideoSizeChanged(VideoSize videoSize);

        void renderFrame(long j, long j2, long j3, boolean z);
    }

    public VideoFrameRenderControl(FrameRenderer frameRenderer, VideoFrameReleaseControl videoFrameReleaseControl) {
        this.frameRenderer = frameRenderer;
        this.videoFrameReleaseControl = videoFrameReleaseControl;
    }

    public void flush() {
        this.presentationTimestampsUs.clear();
        this.lastPresentationTimeUs = C.TIME_UNSET;
        if (this.streamOffsets.size() > 0) {
            long lastStreamOffset = ((Long) getLastAndClear(this.streamOffsets)).longValue();
            this.streamOffsets.add(0L, Long.valueOf(lastStreamOffset));
        }
        VideoSize videoSize = this.pendingOutputVideoSize;
        TimedValueQueue<VideoSize> timedValueQueue = this.videoSizeChanges;
        if (videoSize == null) {
            if (timedValueQueue.size() > 0) {
                this.pendingOutputVideoSize = (VideoSize) getLastAndClear(this.videoSizeChanges);
                return;
            }
            return;
        }
        timedValueQueue.clear();
    }

    public boolean isReady() {
        return this.videoFrameReleaseControl.isReady(true);
    }

    public boolean hasReleasedFrame(long presentationTimeUs) {
        return this.lastPresentationTimeUs != C.TIME_UNSET && this.lastPresentationTimeUs >= presentationTimeUs;
    }

    public void setPlaybackSpeed(float speed) {
        Assertions.checkArgument(speed > 0.0f);
        this.videoFrameReleaseControl.setPlaybackSpeed(speed);
    }

    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        while (!this.presentationTimestampsUs.isEmpty()) {
            long presentationTimeUs = this.presentationTimestampsUs.element();
            if (maybeUpdateOutputStreamOffset(presentationTimeUs)) {
                this.videoFrameReleaseControl.onProcessedStreamChange();
            }
            int frameReleaseAction = this.videoFrameReleaseControl.getFrameReleaseAction(presentationTimeUs, positionUs, elapsedRealtimeUs, this.outputStreamOffsetUs, false, this.videoFrameReleaseInfo);
            switch (frameReleaseAction) {
                case 0:
                case 1:
                    this.lastPresentationTimeUs = presentationTimeUs;
                    renderFrame(frameReleaseAction == 0);
                    break;
                case 2:
                case 3:
                case 4:
                    this.lastPresentationTimeUs = presentationTimeUs;
                    dropFrame();
                    break;
                case 5:
                    return;
                default:
                    throw new IllegalStateException(String.valueOf(frameReleaseAction));
            }
        }
    }

    public void onOutputSizeChanged(int width, int height) {
        VideoSize newVideoSize = new VideoSize(width, height);
        if (!Util.areEqual(this.pendingOutputVideoSize, newVideoSize)) {
            this.pendingOutputVideoSize = newVideoSize;
        }
    }

    public void onOutputFrameAvailableForRendering(long presentationTimeUs) {
        if (this.pendingOutputVideoSize != null) {
            this.videoSizeChanges.add(presentationTimeUs, this.pendingOutputVideoSize);
            this.pendingOutputVideoSize = null;
        }
        this.presentationTimestampsUs.add(presentationTimeUs);
    }

    public void onStreamOffsetChange(long presentationTimeUs, long streamOffsetUs) {
        this.streamOffsets.add(presentationTimeUs, Long.valueOf(streamOffsetUs));
    }

    private void dropFrame() {
        Assertions.checkStateNotNull(Long.valueOf(this.presentationTimestampsUs.remove()));
        this.frameRenderer.dropFrame();
    }

    private void renderFrame(boolean shouldRenderImmediately) {
        long renderTimeNs;
        long presentationTimeUs = ((Long) Assertions.checkStateNotNull(Long.valueOf(this.presentationTimestampsUs.remove()))).longValue();
        boolean videoSizeUpdated = maybeUpdateVideoSize(presentationTimeUs);
        if (videoSizeUpdated) {
            this.frameRenderer.onVideoSizeChanged(this.reportedVideoSize);
        }
        if (shouldRenderImmediately) {
            renderTimeNs = -1;
        } else {
            renderTimeNs = this.videoFrameReleaseInfo.getReleaseTimeNs();
        }
        this.frameRenderer.renderFrame(renderTimeNs, presentationTimeUs, this.outputStreamOffsetUs, this.videoFrameReleaseControl.onFrameReleasedIsFirstFrame());
    }

    private boolean maybeUpdateOutputStreamOffset(long presentationTimeUs) {
        Long newOutputStreamOffsetUs = this.streamOffsets.pollFloor(presentationTimeUs);
        if (newOutputStreamOffsetUs != null && newOutputStreamOffsetUs.longValue() != this.outputStreamOffsetUs) {
            this.outputStreamOffsetUs = newOutputStreamOffsetUs.longValue();
            return true;
        }
        return false;
    }

    private boolean maybeUpdateVideoSize(long presentationTimeUs) {
        VideoSize videoSize = this.videoSizeChanges.pollFloor(presentationTimeUs);
        if (videoSize == null || videoSize.equals(VideoSize.UNKNOWN) || videoSize.equals(this.reportedVideoSize)) {
            return false;
        }
        this.reportedVideoSize = videoSize;
        return true;
    }

    private static <T> T getLastAndClear(TimedValueQueue<T> timedValueQueue) {
        Assertions.checkArgument(timedValueQueue.size() > 0);
        while (timedValueQueue.size() > 1) {
            timedValueQueue.pollFirst();
        }
        return (T) Assertions.checkNotNull(timedValueQueue.pollFirst());
    }
}
