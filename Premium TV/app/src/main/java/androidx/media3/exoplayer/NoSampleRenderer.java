package androidx.media3.exoplayer;

import androidx.media3.common.Format;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.SampleStream;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public abstract class NoSampleRenderer implements Renderer, RendererCapabilities {
    private RendererConfiguration configuration;
    private int index;
    private int state;
    private SampleStream stream;
    private boolean streamIsFinal;

    @Override // androidx.media3.exoplayer.RendererCapabilities
    public /* synthetic */ void clearListener() {
        RendererCapabilities.CC.$default$clearListener(this);
    }

    @Override // androidx.media3.exoplayer.Renderer
    public /* synthetic */ void enableMayRenderStartOfStream() {
        Renderer.CC.$default$enableMayRenderStartOfStream(this);
    }

    @Override // androidx.media3.exoplayer.Renderer
    public /* synthetic */ long getDurationToProgressUs(long j, long j2) {
        return Renderer.DEFAULT_DURATION_TO_PROGRESS_US;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public /* synthetic */ void release() {
        Renderer.CC.$default$release(this);
    }

    @Override // androidx.media3.exoplayer.RendererCapabilities
    public /* synthetic */ void setListener(RendererCapabilities.Listener listener) {
        RendererCapabilities.CC.$default$setListener(this, listener);
    }

    @Override // androidx.media3.exoplayer.Renderer
    public /* synthetic */ void setPlaybackSpeed(float f, float f2) throws ExoPlaybackException {
        Renderer.CC.$default$setPlaybackSpeed(this, f, f2);
    }

    @Override // androidx.media3.exoplayer.Renderer, androidx.media3.exoplayer.RendererCapabilities
    public final int getTrackType() {
        return -2;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final RendererCapabilities getCapabilities() {
        return this;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void init(int index, PlayerId playerId, Clock clock) {
        this.index = index;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public MediaClock getMediaClock() {
        return null;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final int getState() {
        return this.state;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void enable(RendererConfiguration configuration, Format[] formats, SampleStream stream, long positionUs, boolean joining, boolean mayRenderStartOfStream, long startPositionUs, long offsetUs, MediaSource.MediaPeriodId mediaPeriodId) throws ExoPlaybackException {
        Assertions.checkState(this.state == 0);
        this.configuration = configuration;
        this.state = 1;
        onEnabled(joining);
        replaceStream(formats, stream, startPositionUs, offsetUs, mediaPeriodId);
        onPositionReset(positionUs, joining);
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void start() throws ExoPlaybackException {
        Assertions.checkState(this.state == 1);
        this.state = 2;
        onStarted();
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void replaceStream(Format[] formats, SampleStream stream, long startPositionUs, long offsetUs, MediaSource.MediaPeriodId mediaPeriodId) throws ExoPlaybackException {
        Assertions.checkState(!this.streamIsFinal);
        this.stream = stream;
        onRendererOffsetChanged(offsetUs);
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final SampleStream getStream() {
        return this.stream;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final boolean hasReadStreamToEnd() {
        return true;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public long getReadingPositionUs() {
        return Long.MIN_VALUE;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void setCurrentStreamFinal() {
        this.streamIsFinal = true;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final boolean isCurrentStreamFinal() {
        return this.streamIsFinal;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void maybeThrowStreamError() throws IOException {
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void resetPosition(long positionUs) throws ExoPlaybackException {
        this.streamIsFinal = false;
        onPositionReset(positionUs, false);
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void stop() {
        Assertions.checkState(this.state == 2);
        this.state = 1;
        onStopped();
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void disable() {
        Assertions.checkState(this.state == 1);
        this.state = 0;
        this.stream = null;
        this.streamIsFinal = false;
        onDisabled();
    }

    @Override // androidx.media3.exoplayer.Renderer
    public final void reset() {
        Assertions.checkState(this.state == 0);
        onReset();
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isReady() {
        return true;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isEnded() {
        return true;
    }

    @Override // androidx.media3.exoplayer.RendererCapabilities
    public int supportsFormat(Format format) throws ExoPlaybackException {
        return RendererCapabilities.CC.create(0);
    }

    @Override // androidx.media3.exoplayer.RendererCapabilities
    public int supportsMixedMimeTypeAdaptation() throws ExoPlaybackException {
        return 0;
    }

    @Override // androidx.media3.exoplayer.PlayerMessage.Target
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
    }

    @Override // androidx.media3.exoplayer.Renderer
    public void setTimeline(Timeline timeline) {
    }

    protected void onEnabled(boolean joining) throws ExoPlaybackException {
    }

    protected void onRendererOffsetChanged(long offsetUs) throws ExoPlaybackException {
    }

    protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
    }

    protected void onStarted() throws ExoPlaybackException {
    }

    protected void onStopped() {
    }

    protected void onDisabled() {
    }

    protected void onReset() {
    }

    protected final RendererConfiguration getConfiguration() {
        return this.configuration;
    }

    protected final int getIndex() {
        return this.index;
    }
}
