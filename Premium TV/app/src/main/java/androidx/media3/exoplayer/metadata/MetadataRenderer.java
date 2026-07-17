package androidx.media3.exoplayer.metadata;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.BaseRenderer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.extractor.metadata.MetadataDecoder;
import androidx.media3.extractor.metadata.MetadataInputBuffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectFree;

/* JADX INFO: loaded from: classes.dex */
public final class MetadataRenderer extends BaseRenderer implements Handler.Callback {
    private static final int MSG_INVOKE_RENDERER = 1;
    private static final String TAG = "MetadataRenderer";
    private final MetadataInputBuffer buffer;
    private MetadataDecoder decoder;
    private final MetadataDecoderFactory decoderFactory;
    private boolean inputStreamEnded;
    private final MetadataOutput output;
    private final Handler outputHandler;
    private final boolean outputMetadataEarly;
    private boolean outputStreamEnded;
    private long outputStreamOffsetUs;
    private Metadata pendingMetadata;
    private long subsampleOffsetUs;

    public MetadataRenderer(MetadataOutput output, Looper outputLooper) {
        this(output, outputLooper, MetadataDecoderFactory.DEFAULT);
    }

    public MetadataRenderer(MetadataOutput output, Looper outputLooper, MetadataDecoderFactory decoderFactory) {
        this(output, outputLooper, decoderFactory, false);
    }

    public MetadataRenderer(MetadataOutput output, Looper outputLooper, MetadataDecoderFactory decoderFactory, boolean outputMetadataEarly) {
        super(5);
        this.output = (MetadataOutput) Assertions.checkNotNull(output);
        this.outputHandler = outputLooper == null ? null : Util.createHandler(outputLooper, this);
        this.decoderFactory = (MetadataDecoderFactory) Assertions.checkNotNull(decoderFactory);
        this.outputMetadataEarly = outputMetadataEarly;
        this.buffer = new MetadataInputBuffer();
        this.outputStreamOffsetUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.Renderer, androidx.media3.exoplayer.RendererCapabilities
    public String getName() {
        return TAG;
    }

    @Override // androidx.media3.exoplayer.RendererCapabilities
    public int supportsFormat(Format format) {
        if (this.decoderFactory.supportsFormat(format)) {
            return RendererCapabilities.CC.create(format.cryptoType == 0 ? 4 : 2);
        }
        return RendererCapabilities.CC.create(0);
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onStreamChanged(Format[] formats, long startPositionUs, long offsetUs, MediaSource.MediaPeriodId mediaPeriodId) {
        this.decoder = this.decoderFactory.createDecoder(formats[0]);
        if (this.pendingMetadata != null) {
            this.pendingMetadata = this.pendingMetadata.copyWithPresentationTimeUs((this.pendingMetadata.presentationTimeUs + this.outputStreamOffsetUs) - offsetUs);
        }
        this.outputStreamOffsetUs = offsetUs;
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onPositionReset(long positionUs, boolean joining) {
        this.pendingMetadata = null;
        this.inputStreamEnded = false;
        this.outputStreamEnded = false;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public void render(long positionUs, long elapsedRealtimeUs) {
        boolean working = true;
        while (working) {
            readMetadata();
            working = outputMetadata(positionUs);
        }
    }

    private void decodeWrappedMetadata(Metadata metadata, List<Metadata.Entry> decodedEntries) {
        for (int i = 0; i < metadata.length(); i++) {
            Format wrappedMetadataFormat = metadata.get(i).getWrappedMetadataFormat();
            if (wrappedMetadataFormat != null && this.decoderFactory.supportsFormat(wrappedMetadataFormat)) {
                MetadataDecoder wrappedMetadataDecoder = this.decoderFactory.createDecoder(wrappedMetadataFormat);
                byte[] wrappedMetadataBytes = (byte[]) Assertions.checkNotNull(metadata.get(i).getWrappedMetadataBytes());
                this.buffer.clear();
                this.buffer.ensureSpaceForWrite(wrappedMetadataBytes.length);
                ((ByteBuffer) Util.castNonNull(this.buffer.data)).put(wrappedMetadataBytes);
                this.buffer.flip();
                Metadata innerMetadata = wrappedMetadataDecoder.decode(this.buffer);
                if (innerMetadata != null) {
                    decodeWrappedMetadata(innerMetadata, decodedEntries);
                }
            } else {
                decodedEntries.add(metadata.get(i));
            }
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onDisabled() {
        this.pendingMetadata = null;
        this.decoder = null;
        this.outputStreamOffsetUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isEnded() {
        return this.outputStreamEnded;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isReady() {
        return true;
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                invokeRendererInternal((Metadata) msg.obj);
                return true;
            default:
                throw new IllegalStateException();
        }
    }

    private void readMetadata() {
        if (!this.inputStreamEnded && this.pendingMetadata == null) {
            this.buffer.clear();
            FormatHolder formatHolder = getFormatHolder();
            int result = readSource(formatHolder, this.buffer, 0);
            if (result == -4) {
                if (this.buffer.isEndOfStream()) {
                    this.inputStreamEnded = true;
                    return;
                }
                if (this.buffer.timeUs >= getLastResetPositionUs()) {
                    this.buffer.subsampleOffsetUs = this.subsampleOffsetUs;
                    this.buffer.flip();
                    Metadata metadata = ((MetadataDecoder) Util.castNonNull(this.decoder)).decode(this.buffer);
                    if (metadata != null) {
                        List<Metadata.Entry> entries = new ArrayList<>(metadata.length());
                        decodeWrappedMetadata(metadata, entries);
                        if (!entries.isEmpty()) {
                            Metadata expandedMetadata = new Metadata(getPresentationTimeUs(this.buffer.timeUs), entries);
                            this.pendingMetadata = expandedMetadata;
                            return;
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            if (result == -5) {
                this.subsampleOffsetUs = ((Format) Assertions.checkNotNull(formatHolder.format)).subsampleOffsetUs;
            }
        }
    }

    private boolean outputMetadata(long positionUs) {
        boolean didOutput = false;
        if (this.pendingMetadata != null && (this.outputMetadataEarly || this.pendingMetadata.presentationTimeUs <= getPresentationTimeUs(positionUs))) {
            invokeRenderer(this.pendingMetadata);
            this.pendingMetadata = null;
            didOutput = true;
        }
        if (this.inputStreamEnded && this.pendingMetadata == null) {
            this.outputStreamEnded = true;
        }
        return didOutput;
    }

    private void invokeRenderer(Metadata metadata) {
        if (this.outputHandler != null) {
            this.outputHandler.obtainMessage(1, metadata).sendToTarget();
        } else {
            invokeRendererInternal(metadata);
        }
    }

    private void invokeRendererInternal(Metadata metadata) {
        this.output.onMetadata(metadata);
    }

    @SideEffectFree
    private long getPresentationTimeUs(long positionUs) {
        Assertions.checkState(positionUs != C.TIME_UNSET);
        Assertions.checkState(this.outputStreamOffsetUs != C.TIME_UNSET);
        return positionUs - this.outputStreamOffsetUs;
    }
}
