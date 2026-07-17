package androidx.media3.exoplayer.text;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.BaseRenderer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.extractor.text.CueDecoder;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.SubtitleDecoder;
import androidx.media3.extractor.text.SubtitleDecoderException;
import androidx.media3.extractor.text.SubtitleInputBuffer;
import androidx.media3.extractor.text.SubtitleOutputBuffer;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.dataflow.qual.SideEffectFree;

/* JADX INFO: loaded from: classes.dex */
public final class TextRenderer extends BaseRenderer implements Handler.Callback {
    private static final int MSG_UPDATE_OUTPUT = 1;
    private static final int REPLACEMENT_STATE_NONE = 0;
    private static final int REPLACEMENT_STATE_SIGNAL_END_OF_STREAM = 1;
    private static final int REPLACEMENT_STATE_WAIT_END_OF_STREAM = 2;
    private static final String TAG = "TextRenderer";
    private final CueDecoder cueDecoder;
    private final DecoderInputBuffer cueDecoderInputBuffer;
    private CuesResolver cuesResolver;
    private int decoderReplacementState;
    private long finalStreamEndPositionUs;
    private final FormatHolder formatHolder;
    private boolean inputStreamEnded;
    private long lastRendererPositionUs;
    private boolean legacyDecodingEnabled;
    private SubtitleOutputBuffer nextSubtitle;
    private int nextSubtitleEventIndex;
    private SubtitleInputBuffer nextSubtitleInputBuffer;
    private final TextOutput output;
    private final Handler outputHandler;
    private boolean outputStreamEnded;
    private long outputStreamOffsetUs;
    private Format streamFormat;
    private SubtitleOutputBuffer subtitle;
    private SubtitleDecoder subtitleDecoder;
    private final SubtitleDecoderFactory subtitleDecoderFactory;
    private boolean waitingForKeyFrame;

    public TextRenderer(TextOutput output, Looper outputLooper) {
        this(output, outputLooper, SubtitleDecoderFactory.DEFAULT);
    }

    public TextRenderer(TextOutput output, Looper outputLooper, SubtitleDecoderFactory subtitleDecoderFactory) {
        super(3);
        this.output = (TextOutput) Assertions.checkNotNull(output);
        this.outputHandler = outputLooper == null ? null : Util.createHandler(outputLooper, this);
        this.subtitleDecoderFactory = subtitleDecoderFactory;
        this.cueDecoder = new CueDecoder();
        this.cueDecoderInputBuffer = new DecoderInputBuffer(1);
        this.formatHolder = new FormatHolder();
        this.finalStreamEndPositionUs = C.TIME_UNSET;
        this.outputStreamOffsetUs = C.TIME_UNSET;
        this.lastRendererPositionUs = C.TIME_UNSET;
        this.legacyDecodingEnabled = false;
    }

    @Override // androidx.media3.exoplayer.Renderer, androidx.media3.exoplayer.RendererCapabilities
    public String getName() {
        return TAG;
    }

    @Override // androidx.media3.exoplayer.RendererCapabilities
    public int supportsFormat(Format format) {
        if (isCuesWithTiming(format) || this.subtitleDecoderFactory.supportsFormat(format)) {
            return RendererCapabilities.CC.create(format.cryptoType == 0 ? 4 : 2);
        }
        if (MimeTypes.isText(format.sampleMimeType)) {
            return RendererCapabilities.CC.create(1);
        }
        return RendererCapabilities.CC.create(0);
    }

    public void setFinalStreamEndPositionUs(long streamEndPositionUs) {
        Assertions.checkState(isCurrentStreamFinal());
        this.finalStreamEndPositionUs = streamEndPositionUs;
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onStreamChanged(Format[] formats, long startPositionUs, long offsetUs, MediaSource.MediaPeriodId mediaPeriodId) {
        CuesResolver replacingCuesResolver;
        this.outputStreamOffsetUs = offsetUs;
        this.streamFormat = formats[0];
        if (!isCuesWithTiming(this.streamFormat)) {
            assertLegacyDecodingEnabledIfRequired();
            if (this.subtitleDecoder != null) {
                this.decoderReplacementState = 1;
                return;
            } else {
                initSubtitleDecoder();
                return;
            }
        }
        if (this.streamFormat.cueReplacementBehavior == 1) {
            replacingCuesResolver = new MergingCuesResolver();
        } else {
            replacingCuesResolver = new ReplacingCuesResolver();
        }
        this.cuesResolver = replacingCuesResolver;
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onPositionReset(long positionUs, boolean joining) {
        this.lastRendererPositionUs = positionUs;
        if (this.cuesResolver != null) {
            this.cuesResolver.clear();
        }
        clearOutput();
        this.inputStreamEnded = false;
        this.outputStreamEnded = false;
        this.finalStreamEndPositionUs = C.TIME_UNSET;
        if (this.streamFormat != null && !isCuesWithTiming(this.streamFormat)) {
            if (this.decoderReplacementState != 0) {
                replaceSubtitleDecoder();
                return;
            }
            releaseSubtitleBuffers();
            SubtitleDecoder subtitleDecoder = (SubtitleDecoder) Assertions.checkNotNull(this.subtitleDecoder);
            subtitleDecoder.flush();
            subtitleDecoder.setOutputStartTimeUs(getLastResetPositionUs());
        }
    }

    @Override // androidx.media3.exoplayer.Renderer
    public void render(long positionUs, long elapsedRealtimeUs) {
        if (isCurrentStreamFinal() && this.finalStreamEndPositionUs != C.TIME_UNSET && positionUs >= this.finalStreamEndPositionUs) {
            releaseSubtitleBuffers();
            this.outputStreamEnded = true;
        }
        if (this.outputStreamEnded) {
            return;
        }
        if (isCuesWithTiming((Format) Assertions.checkNotNull(this.streamFormat))) {
            Assertions.checkNotNull(this.cuesResolver);
            renderFromCuesWithTiming(positionUs);
        } else {
            assertLegacyDecodingEnabledIfRequired();
            renderFromSubtitles(positionUs);
        }
    }

    @Deprecated
    public void experimentalSetLegacyDecodingEnabled(boolean legacyDecodingEnabled) {
        this.legacyDecodingEnabled = legacyDecodingEnabled;
    }

    @RequiresNonNull({"this.cuesResolver"})
    private void renderFromCuesWithTiming(long positionUs) {
        boolean outputNeedsUpdating = readAndDecodeCuesWithTiming(positionUs);
        long nextCueChangeTimeUs = this.cuesResolver.getNextCueChangeTimeUs(this.lastRendererPositionUs);
        if (nextCueChangeTimeUs == Long.MIN_VALUE && this.inputStreamEnded && !outputNeedsUpdating) {
            this.outputStreamEnded = true;
        }
        if (nextCueChangeTimeUs != Long.MIN_VALUE && nextCueChangeTimeUs <= positionUs) {
            outputNeedsUpdating = true;
        }
        if (outputNeedsUpdating) {
            ImmutableList<Cue> cuesAtTimeUs = this.cuesResolver.getCuesAtTimeUs(positionUs);
            long previousCueChangeTimeUs = this.cuesResolver.getPreviousCueChangeTimeUs(positionUs);
            updateOutput(new CueGroup(cuesAtTimeUs, getPresentationTimeUs(previousCueChangeTimeUs)));
            this.cuesResolver.discardCuesBeforeTimeUs(previousCueChangeTimeUs);
        }
        this.lastRendererPositionUs = positionUs;
    }

    @RequiresNonNull({"this.cuesResolver"})
    private boolean readAndDecodeCuesWithTiming(long positionUs) {
        if (this.inputStreamEnded) {
            return false;
        }
        int readResult = readSource(this.formatHolder, this.cueDecoderInputBuffer, 0);
        switch (readResult) {
            case -4:
                if (this.cueDecoderInputBuffer.isEndOfStream()) {
                    this.inputStreamEnded = true;
                    return false;
                }
                this.cueDecoderInputBuffer.flip();
                ByteBuffer cueData = (ByteBuffer) Assertions.checkNotNull(this.cueDecoderInputBuffer.data);
                CuesWithTiming cuesWithTiming = this.cueDecoder.decode(this.cueDecoderInputBuffer.timeUs, cueData.array(), cueData.arrayOffset(), cueData.limit());
                this.cueDecoderInputBuffer.clear();
                return this.cuesResolver.addCues(cuesWithTiming, positionUs);
            default:
                return false;
        }
    }

    private void renderFromSubtitles(long positionUs) {
        this.lastRendererPositionUs = positionUs;
        if (this.nextSubtitle == null) {
            ((SubtitleDecoder) Assertions.checkNotNull(this.subtitleDecoder)).setPositionUs(positionUs);
            try {
                this.nextSubtitle = ((SubtitleDecoder) Assertions.checkNotNull(this.subtitleDecoder)).dequeueOutputBuffer();
            } catch (SubtitleDecoderException e) {
                handleDecoderError(e);
                return;
            }
        }
        if (getState() != 2) {
            return;
        }
        boolean textRendererNeedsUpdate = false;
        if (this.subtitle != null) {
            long subtitleNextEventTimeUs = getNextEventTime();
            while (subtitleNextEventTimeUs <= positionUs) {
                this.nextSubtitleEventIndex++;
                subtitleNextEventTimeUs = getNextEventTime();
                textRendererNeedsUpdate = true;
            }
        }
        if (this.nextSubtitle != null) {
            SubtitleOutputBuffer nextSubtitle = this.nextSubtitle;
            if (nextSubtitle.isEndOfStream()) {
                if (!textRendererNeedsUpdate && getNextEventTime() == Long.MAX_VALUE) {
                    if (this.decoderReplacementState == 2) {
                        replaceSubtitleDecoder();
                    } else {
                        releaseSubtitleBuffers();
                        this.outputStreamEnded = true;
                    }
                }
            } else if (nextSubtitle.timeUs <= positionUs) {
                if (this.subtitle != null) {
                    this.subtitle.release();
                }
                this.nextSubtitleEventIndex = nextSubtitle.getNextEventTimeIndex(positionUs);
                this.subtitle = nextSubtitle;
                this.nextSubtitle = null;
                textRendererNeedsUpdate = true;
            }
        }
        if (textRendererNeedsUpdate) {
            Assertions.checkNotNull(this.subtitle);
            long presentationTimeUs = getPresentationTimeUs(getCurrentEventTimeUs(positionUs));
            CueGroup cueGroup = new CueGroup(this.subtitle.getCues(positionUs), presentationTimeUs);
            updateOutput(cueGroup);
        }
        if (this.decoderReplacementState == 2) {
            return;
        }
        while (!this.inputStreamEnded) {
            try {
                SubtitleInputBuffer nextInputBuffer = this.nextSubtitleInputBuffer;
                if (nextInputBuffer == null) {
                    nextInputBuffer = ((SubtitleDecoder) Assertions.checkNotNull(this.subtitleDecoder)).dequeueInputBuffer();
                    if (nextInputBuffer == null) {
                        return;
                    } else {
                        this.nextSubtitleInputBuffer = nextInputBuffer;
                    }
                }
                if (this.decoderReplacementState == 1) {
                    nextInputBuffer.setFlags(4);
                    ((SubtitleDecoder) Assertions.checkNotNull(this.subtitleDecoder)).queueInputBuffer(nextInputBuffer);
                    this.nextSubtitleInputBuffer = null;
                    this.decoderReplacementState = 2;
                    return;
                }
                int result = readSource(this.formatHolder, nextInputBuffer, 0);
                if (result == -4) {
                    if (nextInputBuffer.isEndOfStream()) {
                        this.inputStreamEnded = true;
                        this.waitingForKeyFrame = false;
                    } else {
                        Format format = this.formatHolder.format;
                        if (format == null) {
                            return;
                        }
                        nextInputBuffer.subsampleOffsetUs = format.subsampleOffsetUs;
                        nextInputBuffer.flip();
                        this.waitingForKeyFrame = (nextInputBuffer.isKeyFrame() ? false : true) & this.waitingForKeyFrame;
                    }
                    if (!this.waitingForKeyFrame) {
                        ((SubtitleDecoder) Assertions.checkNotNull(this.subtitleDecoder)).queueInputBuffer(nextInputBuffer);
                        this.nextSubtitleInputBuffer = null;
                    }
                } else if (result == -3) {
                    return;
                }
            } catch (SubtitleDecoderException e2) {
                handleDecoderError(e2);
                return;
            }
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onDisabled() {
        this.streamFormat = null;
        this.finalStreamEndPositionUs = C.TIME_UNSET;
        clearOutput();
        this.outputStreamOffsetUs = C.TIME_UNSET;
        this.lastRendererPositionUs = C.TIME_UNSET;
        if (this.subtitleDecoder != null) {
            releaseSubtitleDecoder();
        }
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isEnded() {
        return this.outputStreamEnded;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isReady() {
        return true;
    }

    private void releaseSubtitleBuffers() {
        this.nextSubtitleInputBuffer = null;
        this.nextSubtitleEventIndex = -1;
        if (this.subtitle != null) {
            this.subtitle.release();
            this.subtitle = null;
        }
        if (this.nextSubtitle != null) {
            this.nextSubtitle.release();
            this.nextSubtitle = null;
        }
    }

    private void releaseSubtitleDecoder() {
        releaseSubtitleBuffers();
        ((SubtitleDecoder) Assertions.checkNotNull(this.subtitleDecoder)).release();
        this.subtitleDecoder = null;
        this.decoderReplacementState = 0;
    }

    private void initSubtitleDecoder() {
        this.waitingForKeyFrame = true;
        this.subtitleDecoder = this.subtitleDecoderFactory.createDecoder((Format) Assertions.checkNotNull(this.streamFormat));
        this.subtitleDecoder.setOutputStartTimeUs(getLastResetPositionUs());
    }

    private void replaceSubtitleDecoder() {
        releaseSubtitleDecoder();
        initSubtitleDecoder();
    }

    private long getNextEventTime() {
        if (this.nextSubtitleEventIndex == -1) {
            return Long.MAX_VALUE;
        }
        Assertions.checkNotNull(this.subtitle);
        if (this.nextSubtitleEventIndex >= this.subtitle.getEventTimeCount()) {
            return Long.MAX_VALUE;
        }
        return this.subtitle.getEventTime(this.nextSubtitleEventIndex);
    }

    private void updateOutput(CueGroup cueGroup) {
        if (this.outputHandler != null) {
            this.outputHandler.obtainMessage(1, cueGroup).sendToTarget();
        } else {
            invokeUpdateOutputInternal(cueGroup);
        }
    }

    private void clearOutput() {
        updateOutput(new CueGroup(ImmutableList.of(), getPresentationTimeUs(this.lastRendererPositionUs)));
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                invokeUpdateOutputInternal((CueGroup) msg.obj);
                return true;
            default:
                throw new IllegalStateException();
        }
    }

    private void invokeUpdateOutputInternal(CueGroup cueGroup) {
        this.output.onCues(cueGroup.cues);
        this.output.onCues(cueGroup);
    }

    private void handleDecoderError(SubtitleDecoderException e) {
        Log.e(TAG, "Subtitle decoding failed. streamFormat=" + this.streamFormat, e);
        clearOutput();
        replaceSubtitleDecoder();
    }

    @RequiresNonNull({"subtitle"})
    @SideEffectFree
    private long getCurrentEventTimeUs(long positionUs) {
        int nextEventTimeIndex = this.subtitle.getNextEventTimeIndex(positionUs);
        if (nextEventTimeIndex == 0 || this.subtitle.getEventTimeCount() == 0) {
            return this.subtitle.timeUs;
        }
        SubtitleOutputBuffer subtitleOutputBuffer = this.subtitle;
        if (nextEventTimeIndex == -1) {
            return subtitleOutputBuffer.getEventTime(this.subtitle.getEventTimeCount() - 1);
        }
        return subtitleOutputBuffer.getEventTime(nextEventTimeIndex - 1);
    }

    @SideEffectFree
    private long getPresentationTimeUs(long positionUs) {
        Assertions.checkState(positionUs != C.TIME_UNSET);
        Assertions.checkState(this.outputStreamOffsetUs != C.TIME_UNSET);
        return positionUs - this.outputStreamOffsetUs;
    }

    @RequiresNonNull({"streamFormat"})
    private void assertLegacyDecodingEnabledIfRequired() {
        Assertions.checkState(this.legacyDecodingEnabled || Objects.equals(this.streamFormat.sampleMimeType, MimeTypes.APPLICATION_CEA608) || Objects.equals(this.streamFormat.sampleMimeType, MimeTypes.APPLICATION_MP4CEA608) || Objects.equals(this.streamFormat.sampleMimeType, MimeTypes.APPLICATION_CEA708), "Legacy decoding is disabled, can't handle " + this.streamFormat.sampleMimeType + " samples (expected " + MimeTypes.APPLICATION_MEDIA3_CUES + ").");
    }

    @SideEffectFree
    private static boolean isCuesWithTiming(Format format) {
        return Objects.equals(format.sampleMimeType, MimeTypes.APPLICATION_MEDIA3_CUES);
    }
}
