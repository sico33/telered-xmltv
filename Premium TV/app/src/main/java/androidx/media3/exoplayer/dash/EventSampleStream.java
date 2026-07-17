package androidx.media3.exoplayer.dash;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.dash.manifest.EventStream;
import androidx.media3.exoplayer.source.SampleStream;
import androidx.media3.extractor.metadata.emsg.EventMessageEncoder;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class EventSampleStream implements SampleStream {
    private int currentIndex;
    private EventStream eventStream;
    private boolean eventStreamAppendable;
    private long[] eventTimesUs;
    private boolean isFormatSentDownstream;
    private final Format upstreamFormat;
    private final EventMessageEncoder eventMessageEncoder = new EventMessageEncoder();
    private long pendingSeekPositionUs = C.TIME_UNSET;

    public EventSampleStream(EventStream eventStream, Format upstreamFormat, boolean eventStreamAppendable) {
        this.upstreamFormat = upstreamFormat;
        this.eventStream = eventStream;
        this.eventTimesUs = eventStream.presentationTimesUs;
        updateEventStream(eventStream, eventStreamAppendable);
    }

    public String eventStreamId() {
        return this.eventStream.id();
    }

    public void updateEventStream(EventStream eventStream, boolean eventStreamAppendable) {
        long lastReadPositionUs = this.currentIndex == 0 ? -9223372036854775807L : this.eventTimesUs[this.currentIndex - 1];
        this.eventStreamAppendable = eventStreamAppendable;
        this.eventStream = eventStream;
        this.eventTimesUs = eventStream.presentationTimesUs;
        if (this.pendingSeekPositionUs != C.TIME_UNSET) {
            seekToUs(this.pendingSeekPositionUs);
        } else if (lastReadPositionUs != C.TIME_UNSET) {
            this.currentIndex = Util.binarySearchCeil(this.eventTimesUs, lastReadPositionUs, false, false);
        }
    }

    public void seekToUs(long positionUs) {
        this.currentIndex = Util.binarySearchCeil(this.eventTimesUs, positionUs, true, false);
        boolean isPendingSeek = this.eventStreamAppendable && this.currentIndex == this.eventTimesUs.length;
        this.pendingSeekPositionUs = isPendingSeek ? positionUs : C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public boolean isReady() {
        return true;
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public void maybeThrowError() throws IOException {
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
        boolean noMoreEventsInStream = this.currentIndex == this.eventTimesUs.length;
        if (noMoreEventsInStream && !this.eventStreamAppendable) {
            buffer.setFlags(4);
            return -4;
        }
        if ((readFlags & 2) != 0 || !this.isFormatSentDownstream) {
            formatHolder.format = this.upstreamFormat;
            this.isFormatSentDownstream = true;
            return -5;
        }
        if (noMoreEventsInStream) {
            return -3;
        }
        int sampleIndex = this.currentIndex;
        if ((readFlags & 1) == 0) {
            this.currentIndex++;
        }
        if ((readFlags & 4) == 0) {
            byte[] serializedEvent = this.eventMessageEncoder.encode(this.eventStream.events[sampleIndex]);
            buffer.ensureSpaceForWrite(serializedEvent.length);
            buffer.data.put(serializedEvent);
        }
        buffer.timeUs = this.eventTimesUs[sampleIndex];
        buffer.setFlags(1);
        return -4;
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public int skipData(long positionUs) {
        int newIndex = Math.max(this.currentIndex, Util.binarySearchCeil(this.eventTimesUs, positionUs, true, false));
        int skipped = newIndex - this.currentIndex;
        this.currentIndex = newIndex;
        return skipped;
    }
}
