package androidx.media3.exoplayer.video.spherical;

import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.BaseRenderer;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor;
import androidx.media3.exoplayer.source.MediaSource;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public final class CameraMotionRenderer extends BaseRenderer {
    private static final int SAMPLE_WINDOW_DURATION_US = 100000;
    private static final String TAG = "CameraMotionRenderer";
    private final DecoderInputBuffer buffer;
    private long lastTimestampUs;
    private CameraMotionListener listener;
    private long offsetUs;
    private final ParsableByteArray scratch;

    public CameraMotionRenderer() {
        super(6);
        this.buffer = new DecoderInputBuffer(1);
        this.scratch = new ParsableByteArray();
    }

    @Override // androidx.media3.exoplayer.Renderer, androidx.media3.exoplayer.RendererCapabilities
    public String getName() {
        return TAG;
    }

    @Override // androidx.media3.exoplayer.RendererCapabilities
    public int supportsFormat(Format format) {
        if (MimeTypes.APPLICATION_CAMERA_MOTION.equals(format.sampleMimeType)) {
            return RendererCapabilities.CC.create(4);
        }
        return RendererCapabilities.CC.create(0);
    }

    @Override // androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.PlayerMessage.Target
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        if (messageType == 8) {
            this.listener = (CameraMotionListener) message;
        } else {
            super.handleMessage(messageType, message);
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onStreamChanged(Format[] formats, long startPositionUs, long offsetUs, MediaSource.MediaPeriodId mediaPeriodId) {
        this.offsetUs = offsetUs;
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onPositionReset(long positionUs, boolean joining) {
        this.lastTimestampUs = Long.MIN_VALUE;
        resetListener();
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onDisabled() {
        resetListener();
    }

    @Override // androidx.media3.exoplayer.Renderer
    public void render(long positionUs, long elapsedRealtimeUs) {
        while (!hasReadStreamToEnd() && this.lastTimestampUs < SilenceSkippingAudioProcessor.DEFAULT_MINIMUM_SILENCE_DURATION_US + positionUs) {
            this.buffer.clear();
            FormatHolder formatHolder = getFormatHolder();
            int result = readSource(formatHolder, this.buffer, 0);
            if (result != -4 || this.buffer.isEndOfStream()) {
                return;
            }
            this.lastTimestampUs = this.buffer.timeUs;
            boolean isDecodeOnly = this.lastTimestampUs < getLastResetPositionUs();
            if (this.listener != null && !isDecodeOnly) {
                this.buffer.flip();
                float[] rotation = parseMetadata((ByteBuffer) Util.castNonNull(this.buffer.data));
                if (rotation != null) {
                    ((CameraMotionListener) Util.castNonNull(this.listener)).onCameraMotion(this.lastTimestampUs - this.offsetUs, rotation);
                }
            }
        }
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isEnded() {
        return hasReadStreamToEnd();
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isReady() {
        return true;
    }

    private float[] parseMetadata(ByteBuffer data) {
        if (data.remaining() != 16) {
            return null;
        }
        this.scratch.reset(data.array(), data.limit());
        this.scratch.setPosition(data.arrayOffset() + 4);
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            result[i] = Float.intBitsToFloat(this.scratch.readLittleEndianInt());
        }
        return result;
    }

    private void resetListener() {
        if (this.listener != null) {
            this.listener.onCameraMotionReset();
        }
    }
}
