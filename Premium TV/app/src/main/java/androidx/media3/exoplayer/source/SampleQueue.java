package androidx.media3.exoplayer.source;

import android.os.Looper;
import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Consumer;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.drm.DrmSession;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.extractor.TrackOutput;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class SampleQueue implements TrackOutput {
    static final int SAMPLE_CAPACITY_INCREMENT = 1000;
    private static final String TAG = "SampleQueue";
    private int absoluteFirstIndex;
    private DrmSession currentDrmSession;
    private Format downstreamFormat;
    private final DrmSessionEventListener.EventDispatcher drmEventDispatcher;
    private final DrmSessionManager drmSessionManager;
    private boolean isLastSampleQueued;
    private int length;
    private boolean loggedUnexpectedNonSyncSample;
    private boolean pendingSplice;
    private int readPosition;
    private int relativeFirstIndex;
    private final SampleDataQueue sampleDataQueue;
    private long sampleOffsetUs;
    private Format unadjustedUpstreamFormat;
    private Format upstreamFormat;
    private boolean upstreamFormatAdjustmentRequired;
    private UpstreamFormatChangedListener upstreamFormatChangeListener;
    private long upstreamSourceId;
    private final SampleExtrasHolder extrasHolder = new SampleExtrasHolder();
    private int capacity = 1000;
    private long[] sourceIds = new long[this.capacity];
    private long[] offsets = new long[this.capacity];
    private long[] timesUs = new long[this.capacity];
    private int[] flags = new int[this.capacity];
    private int[] sizes = new int[this.capacity];
    private TrackOutput.CryptoData[] cryptoDatas = new TrackOutput.CryptoData[this.capacity];
    private final SpannedData<SharedSampleMetadata> sharedSampleMetadata = new SpannedData<>(new Consumer() { // from class: androidx.media3.exoplayer.source.SampleQueue$$ExternalSyntheticLambda0
        @Override // androidx.media3.common.util.Consumer
        public final void accept(Object obj) {
            ((SampleQueue.SharedSampleMetadata) obj).drmSessionReference.release();
        }
    });
    private long startTimeUs = Long.MIN_VALUE;
    private long largestDiscardedTimestampUs = Long.MIN_VALUE;
    private long largestQueuedTimestampUs = Long.MIN_VALUE;
    private boolean upstreamFormatRequired = true;
    private boolean upstreamKeyframeRequired = true;
    private boolean allSamplesAreSyncSamples = true;

    public interface UpstreamFormatChangedListener {
        void onUpstreamFormatChanged(Format format);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public /* synthetic */ int sampleData(DataReader dataReader, int i, boolean z) {
        return sampleData(dataReader, i, z, 0);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public /* synthetic */ void sampleData(ParsableByteArray parsableByteArray, int i) {
        sampleData(parsableByteArray, i, 0);
    }

    public static SampleQueue createWithoutDrm(Allocator allocator) {
        return new SampleQueue(allocator, null, null);
    }

    public static SampleQueue createWithDrm(Allocator allocator, DrmSessionManager drmSessionManager, DrmSessionEventListener.EventDispatcher drmEventDispatcher) {
        return new SampleQueue(allocator, (DrmSessionManager) Assertions.checkNotNull(drmSessionManager), (DrmSessionEventListener.EventDispatcher) Assertions.checkNotNull(drmEventDispatcher));
    }

    @Deprecated
    public static SampleQueue createWithDrm(Allocator allocator, Looper playbackLooper, DrmSessionManager drmSessionManager, DrmSessionEventListener.EventDispatcher drmEventDispatcher) {
        drmSessionManager.setPlayer(playbackLooper, PlayerId.UNSET);
        return new SampleQueue(allocator, (DrmSessionManager) Assertions.checkNotNull(drmSessionManager), (DrmSessionEventListener.EventDispatcher) Assertions.checkNotNull(drmEventDispatcher));
    }

    protected SampleQueue(Allocator allocator, DrmSessionManager drmSessionManager, DrmSessionEventListener.EventDispatcher drmEventDispatcher) {
        this.drmSessionManager = drmSessionManager;
        this.drmEventDispatcher = drmEventDispatcher;
        this.sampleDataQueue = new SampleDataQueue(allocator);
    }

    public void release() {
        reset(true);
        releaseDrmSessionReferences();
    }

    public final void reset() {
        reset(false);
    }

    public void reset(boolean resetUpstreamFormat) {
        this.sampleDataQueue.reset();
        this.length = 0;
        this.absoluteFirstIndex = 0;
        this.relativeFirstIndex = 0;
        this.readPosition = 0;
        this.upstreamKeyframeRequired = true;
        this.startTimeUs = Long.MIN_VALUE;
        this.largestDiscardedTimestampUs = Long.MIN_VALUE;
        this.largestQueuedTimestampUs = Long.MIN_VALUE;
        this.isLastSampleQueued = false;
        this.sharedSampleMetadata.clear();
        if (resetUpstreamFormat) {
            this.unadjustedUpstreamFormat = null;
            this.upstreamFormat = null;
            this.upstreamFormatRequired = true;
            this.allSamplesAreSyncSamples = true;
        }
    }

    public final void setStartTimeUs(long startTimeUs) {
        this.startTimeUs = startTimeUs;
    }

    public final void sourceId(long sourceId) {
        this.upstreamSourceId = sourceId;
    }

    public final void splice() {
        this.pendingSplice = true;
    }

    public final int getWriteIndex() {
        return this.absoluteFirstIndex + this.length;
    }

    public final void discardUpstreamSamples(int discardFromIndex) {
        this.sampleDataQueue.discardUpstreamSampleBytes(discardUpstreamSampleMetadata(discardFromIndex));
    }

    public final void discardUpstreamFrom(long timeUs) {
        if (this.length == 0) {
            return;
        }
        Assertions.checkArgument(timeUs > getLargestReadTimestampUs());
        int retainCount = countUnreadSamplesBefore(timeUs);
        discardUpstreamSamples(this.absoluteFirstIndex + retainCount);
    }

    public void preRelease() {
        discardToEnd();
        releaseDrmSessionReferences();
    }

    public void maybeThrowError() throws IOException {
        if (this.currentDrmSession != null && this.currentDrmSession.getState() == 1) {
            throw ((DrmSession.DrmSessionException) Assertions.checkNotNull(this.currentDrmSession.getError()));
        }
    }

    public final int getFirstIndex() {
        return this.absoluteFirstIndex;
    }

    public final int getReadIndex() {
        return this.absoluteFirstIndex + this.readPosition;
    }

    public final synchronized long peekSourceId() {
        int relativeReadIndex;
        relativeReadIndex = getRelativeIndex(this.readPosition);
        return hasNextSample() ? this.sourceIds[relativeReadIndex] : this.upstreamSourceId;
    }

    public final synchronized Format getUpstreamFormat() {
        return this.upstreamFormatRequired ? null : this.upstreamFormat;
    }

    public final synchronized long getLargestQueuedTimestampUs() {
        return this.largestQueuedTimestampUs;
    }

    public final synchronized long getLargestReadTimestampUs() {
        return Math.max(this.largestDiscardedTimestampUs, getLargestTimestamp(this.readPosition));
    }

    public final synchronized boolean isLastSampleQueued() {
        return this.isLastSampleQueued;
    }

    public final synchronized long getFirstTimestampUs() {
        return this.length == 0 ? Long.MIN_VALUE : this.timesUs[this.relativeFirstIndex];
    }

    public synchronized boolean isReady(boolean loadingFinished) {
        boolean z = true;
        if (!hasNextSample()) {
            if (!loadingFinished && !this.isLastSampleQueued && (this.upstreamFormat == null || this.upstreamFormat == this.downstreamFormat)) {
                z = false;
            }
            return z;
        }
        if (this.sharedSampleMetadata.get(getReadIndex()).format != this.downstreamFormat) {
            return true;
        }
        return mayReadSample(getRelativeIndex(this.readPosition));
    }

    public int read(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags, boolean loadingFinished) {
        int result = peekSampleMetadata(formatHolder, buffer, (readFlags & 2) != 0, loadingFinished, this.extrasHolder);
        if (result == -4 && !buffer.isEndOfStream()) {
            boolean peek = (readFlags & 1) != 0;
            if ((readFlags & 4) == 0) {
                SampleDataQueue sampleDataQueue = this.sampleDataQueue;
                if (peek) {
                    sampleDataQueue.peekToBuffer(buffer, this.extrasHolder);
                } else {
                    sampleDataQueue.readToBuffer(buffer, this.extrasHolder);
                }
            }
            if (!peek) {
                this.readPosition++;
            }
        }
        return result;
    }

    public final synchronized boolean seekTo(int sampleIndex) {
        rewind();
        if (sampleIndex >= this.absoluteFirstIndex && sampleIndex <= this.absoluteFirstIndex + this.length) {
            this.startTimeUs = Long.MIN_VALUE;
            this.readPosition = sampleIndex - this.absoluteFirstIndex;
            return true;
        }
        return false;
    }

    public final synchronized boolean seekTo(long timeUs, boolean allowTimeBeyondBuffer) {
        long timeUs2;
        int offset;
        rewind();
        int relativeReadIndex = getRelativeIndex(this.readPosition);
        if (hasNextSample() && timeUs >= this.timesUs[relativeReadIndex]) {
            if (timeUs <= this.largestQueuedTimestampUs || allowTimeBeyondBuffer) {
                boolean z = this.allSamplesAreSyncSamples;
                int i = this.length;
                if (z) {
                    timeUs2 = timeUs;
                    offset = findSampleAfter(relativeReadIndex, i - this.readPosition, timeUs2, allowTimeBeyondBuffer);
                } else {
                    timeUs2 = timeUs;
                    offset = findSampleBefore(relativeReadIndex, i - this.readPosition, timeUs2, true);
                }
                if (offset == -1) {
                    return false;
                }
                this.startTimeUs = timeUs2;
                this.readPosition += offset;
                return true;
            }
        }
        return false;
    }

    public final synchronized int getSkipCount(long timeUs, boolean allowEndOfQueue) throws Throwable {
        Throwable th;
        try {
            try {
                int relativeReadIndex = getRelativeIndex(this.readPosition);
                if (!hasNextSample() || timeUs < this.timesUs[relativeReadIndex]) {
                    return 0;
                }
                if (timeUs > this.largestQueuedTimestampUs && allowEndOfQueue) {
                    try {
                        return this.length - this.readPosition;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } else {
                    int offset = findSampleBefore(relativeReadIndex, this.length - this.readPosition, timeUs, true);
                    if (offset == -1) {
                        return 0;
                    }
                    return offset;
                }
            } catch (Throwable th3) {
                th = th3;
                th = th;
            }
        } catch (Throwable th4) {
            th = th4;
        }
        throw th;
    }

    /* JADX WARN: Code duplicated, block: B:9:0x000e  */
    public final synchronized void skip(int count) {
        boolean z;
        if (count >= 0) {
            try {
                if (this.readPosition + count <= this.length) {
                    z = true;
                } else {
                    z = false;
                }
            } catch (Throwable th) {
                throw th;
            }
        } else {
            z = false;
        }
        Assertions.checkArgument(z);
        this.readPosition += count;
    }

    public final void discardTo(long timeUs, boolean toKeyframe, boolean stopAtReadPosition) {
        this.sampleDataQueue.discardDownstreamTo(discardSampleMetadataTo(timeUs, toKeyframe, stopAtReadPosition));
    }

    public final void discardToRead() {
        this.sampleDataQueue.discardDownstreamTo(discardSampleMetadataToRead());
    }

    public final void discardToEnd() {
        this.sampleDataQueue.discardDownstreamTo(discardSampleMetadataToEnd());
    }

    public final void setSampleOffsetUs(long sampleOffsetUs) {
        if (this.sampleOffsetUs != sampleOffsetUs) {
            this.sampleOffsetUs = sampleOffsetUs;
            invalidateUpstreamFormatAdjustment();
        }
    }

    public final void setUpstreamFormatChangeListener(UpstreamFormatChangedListener listener) {
        this.upstreamFormatChangeListener = listener;
    }

    @Override // androidx.media3.extractor.TrackOutput
    public final void format(Format format) {
        Format adjustedUpstreamFormat = getAdjustedUpstreamFormat(format);
        this.upstreamFormatAdjustmentRequired = false;
        this.unadjustedUpstreamFormat = format;
        boolean upstreamFormatChanged = setUpstreamFormat(adjustedUpstreamFormat);
        if (this.upstreamFormatChangeListener != null && upstreamFormatChanged) {
            this.upstreamFormatChangeListener.onUpstreamFormatChanged(adjustedUpstreamFormat);
        }
    }

    @Override // androidx.media3.extractor.TrackOutput
    public final int sampleData(DataReader input, int length, boolean allowEndOfInput, int sampleDataPart) throws IOException {
        return this.sampleDataQueue.sampleData(input, length, allowEndOfInput);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public final void sampleData(ParsableByteArray data, int length, int sampleDataPart) {
        this.sampleDataQueue.sampleData(data, length);
    }

    /* JADX WARN: Code duplicated, block: B:25:0x0058  */
    @Override // androidx.media3.extractor.TrackOutput
    public void sampleMetadata(long timeUs, int flags, int size, int offset, TrackOutput.CryptoData cryptoData) {
        int flags2;
        if (this.upstreamFormatAdjustmentRequired) {
            format((Format) Assertions.checkStateNotNull(this.unadjustedUpstreamFormat));
        }
        boolean isKeyframe = (flags & 1) != 0;
        if (this.upstreamKeyframeRequired) {
            if (!isKeyframe) {
                return;
            } else {
                this.upstreamKeyframeRequired = false;
            }
        }
        long timeUs2 = timeUs + this.sampleOffsetUs;
        if (!this.allSamplesAreSyncSamples) {
            flags2 = flags;
        } else {
            if (timeUs2 < this.startTimeUs) {
                return;
            }
            if ((flags & 1) == 0) {
                if (!this.loggedUnexpectedNonSyncSample) {
                    Log.w(TAG, "Overriding unexpected non-sync sample for format: " + this.upstreamFormat);
                    this.loggedUnexpectedNonSyncSample = true;
                }
                flags2 = flags | 1;
            } else {
                flags2 = flags;
            }
        }
        if (this.pendingSplice) {
            if (!isKeyframe || !attemptSplice(timeUs2)) {
                return;
            } else {
                this.pendingSplice = false;
            }
        }
        long absoluteOffset = (this.sampleDataQueue.getTotalBytesWritten() - ((long) size)) - ((long) offset);
        commitSample(timeUs2, flags2, absoluteOffset, size, cryptoData);
    }

    protected final void invalidateUpstreamFormatAdjustment() {
        this.upstreamFormatAdjustmentRequired = true;
    }

    protected Format getAdjustedUpstreamFormat(Format format) {
        if (this.sampleOffsetUs != 0 && format.subsampleOffsetUs != Long.MAX_VALUE) {
            return format.buildUpon().setSubsampleOffsetUs(format.subsampleOffsetUs + this.sampleOffsetUs).build();
        }
        return format;
    }

    private synchronized void rewind() {
        this.readPosition = 0;
        this.sampleDataQueue.rewind();
    }

    private synchronized int peekSampleMetadata(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean formatRequired, boolean loadingFinished, SampleExtrasHolder extrasHolder) {
        buffer.waitingForKeys = false;
        if (!hasNextSample()) {
            if (!loadingFinished && !this.isLastSampleQueued) {
                if (this.upstreamFormat == null || (!formatRequired && this.upstreamFormat == this.downstreamFormat)) {
                    return -3;
                }
                onFormatResult((Format) Assertions.checkNotNull(this.upstreamFormat), formatHolder);
                return -5;
            }
            buffer.setFlags(4);
            buffer.timeUs = Long.MIN_VALUE;
            return -4;
        }
        Format format = this.sharedSampleMetadata.get(getReadIndex()).format;
        if (!formatRequired && format == this.downstreamFormat) {
            int relativeReadIndex = getRelativeIndex(this.readPosition);
            if (!mayReadSample(relativeReadIndex)) {
                buffer.waitingForKeys = true;
                return -3;
            }
            buffer.setFlags(this.flags[relativeReadIndex]);
            if (this.readPosition == this.length - 1 && (loadingFinished || this.isLastSampleQueued)) {
                buffer.addFlag(C.BUFFER_FLAG_LAST_SAMPLE);
            }
            buffer.timeUs = this.timesUs[relativeReadIndex];
            extrasHolder.size = this.sizes[relativeReadIndex];
            extrasHolder.offset = this.offsets[relativeReadIndex];
            extrasHolder.cryptoData = this.cryptoDatas[relativeReadIndex];
            return -4;
        }
        onFormatResult(format, formatHolder);
        return -5;
    }

    private synchronized boolean setUpstreamFormat(Format format) {
        this.upstreamFormatRequired = false;
        if (Util.areEqual(format, this.upstreamFormat)) {
            return false;
        }
        if (!this.sharedSampleMetadata.isEmpty() && this.sharedSampleMetadata.getEndValue().format.equals(format)) {
            this.upstreamFormat = this.sharedSampleMetadata.getEndValue().format;
        } else {
            this.upstreamFormat = format;
        }
        this.allSamplesAreSyncSamples &= MimeTypes.allSamplesAreSyncSamples(this.upstreamFormat.sampleMimeType, this.upstreamFormat.codecs);
        this.loggedUnexpectedNonSyncSample = false;
        return true;
    }

    private synchronized long discardSampleMetadataTo(long timeUs, boolean toKeyframe, boolean stopAtReadPosition) {
        if (this.length == 0 || timeUs < this.timesUs[this.relativeFirstIndex]) {
            return -1L;
        }
        int searchLength = (!stopAtReadPosition || this.readPosition == this.length) ? this.length : this.readPosition + 1;
        int discardCount = findSampleBefore(this.relativeFirstIndex, searchLength, timeUs, toKeyframe);
        if (discardCount == -1) {
            return -1L;
        }
        return discardSamples(discardCount);
    }

    public synchronized long discardSampleMetadataToRead() {
        if (this.readPosition == 0) {
            return -1L;
        }
        return discardSamples(this.readPosition);
    }

    private synchronized long discardSampleMetadataToEnd() {
        if (this.length == 0) {
            return -1L;
        }
        return discardSamples(this.length);
    }

    private void releaseDrmSessionReferences() {
        if (this.currentDrmSession != null) {
            this.currentDrmSession.release(this.drmEventDispatcher);
            this.currentDrmSession = null;
            this.downstreamFormat = null;
        }
    }

    private synchronized void commitSample(long timeUs, int sampleFlags, long offset, int size, TrackOutput.CryptoData cryptoData) {
        DrmSessionManager.DrmSessionReference drmSessionReference;
        if (this.length > 0) {
            int previousSampleRelativeIndex = getRelativeIndex(this.length - 1);
            Assertions.checkArgument(this.offsets[previousSampleRelativeIndex] + ((long) this.sizes[previousSampleRelativeIndex]) <= offset);
        }
        this.isLastSampleQueued = (sampleFlags & C.BUFFER_FLAG_LAST_SAMPLE) != 0;
        this.largestQueuedTimestampUs = Math.max(this.largestQueuedTimestampUs, timeUs);
        int relativeEndIndex = getRelativeIndex(this.length);
        this.timesUs[relativeEndIndex] = timeUs;
        this.offsets[relativeEndIndex] = offset;
        this.sizes[relativeEndIndex] = size;
        this.flags[relativeEndIndex] = sampleFlags;
        this.cryptoDatas[relativeEndIndex] = cryptoData;
        this.sourceIds[relativeEndIndex] = this.upstreamSourceId;
        if (this.sharedSampleMetadata.isEmpty() || !this.sharedSampleMetadata.getEndValue().format.equals(this.upstreamFormat)) {
            Format upstreamFormat = (Format) Assertions.checkNotNull(this.upstreamFormat);
            if (this.drmSessionManager != null) {
                drmSessionReference = this.drmSessionManager.preacquireSession(this.drmEventDispatcher, upstreamFormat);
            } else {
                drmSessionReference = DrmSessionManager.DrmSessionReference.EMPTY;
            }
            this.sharedSampleMetadata.appendSpan(getWriteIndex(), new SharedSampleMetadata(upstreamFormat, drmSessionReference));
        }
        this.length++;
        if (this.length == this.capacity) {
            int newCapacity = this.capacity + 1000;
            long[] newSourceIds = new long[newCapacity];
            long[] newOffsets = new long[newCapacity];
            long[] newTimesUs = new long[newCapacity];
            int[] newFlags = new int[newCapacity];
            int[] newSizes = new int[newCapacity];
            TrackOutput.CryptoData[] newCryptoDatas = new TrackOutput.CryptoData[newCapacity];
            int beforeWrap = this.capacity - this.relativeFirstIndex;
            System.arraycopy(this.offsets, this.relativeFirstIndex, newOffsets, 0, beforeWrap);
            System.arraycopy(this.timesUs, this.relativeFirstIndex, newTimesUs, 0, beforeWrap);
            System.arraycopy(this.flags, this.relativeFirstIndex, newFlags, 0, beforeWrap);
            System.arraycopy(this.sizes, this.relativeFirstIndex, newSizes, 0, beforeWrap);
            System.arraycopy(this.cryptoDatas, this.relativeFirstIndex, newCryptoDatas, 0, beforeWrap);
            System.arraycopy(this.sourceIds, this.relativeFirstIndex, newSourceIds, 0, beforeWrap);
            int afterWrap = this.relativeFirstIndex;
            System.arraycopy(this.offsets, 0, newOffsets, beforeWrap, afterWrap);
            System.arraycopy(this.timesUs, 0, newTimesUs, beforeWrap, afterWrap);
            System.arraycopy(this.flags, 0, newFlags, beforeWrap, afterWrap);
            System.arraycopy(this.sizes, 0, newSizes, beforeWrap, afterWrap);
            System.arraycopy(this.cryptoDatas, 0, newCryptoDatas, beforeWrap, afterWrap);
            System.arraycopy(this.sourceIds, 0, newSourceIds, beforeWrap, afterWrap);
            this.offsets = newOffsets;
            this.timesUs = newTimesUs;
            this.flags = newFlags;
            this.sizes = newSizes;
            this.cryptoDatas = newCryptoDatas;
            this.sourceIds = newSourceIds;
            this.relativeFirstIndex = 0;
            this.capacity = newCapacity;
        }
    }

    private synchronized boolean attemptSplice(long timeUs) {
        if (this.length == 0) {
            return timeUs > this.largestDiscardedTimestampUs;
        }
        if (getLargestReadTimestampUs() >= timeUs) {
            return false;
        }
        int retainCount = countUnreadSamplesBefore(timeUs);
        discardUpstreamSampleMetadata(this.absoluteFirstIndex + retainCount);
        return true;
    }

    private long discardUpstreamSampleMetadata(int discardFromIndex) {
        int discardCount = getWriteIndex() - discardFromIndex;
        boolean z = false;
        Assertions.checkArgument(discardCount >= 0 && discardCount <= this.length - this.readPosition);
        this.length -= discardCount;
        this.largestQueuedTimestampUs = Math.max(this.largestDiscardedTimestampUs, getLargestTimestamp(this.length));
        if (discardCount == 0 && this.isLastSampleQueued) {
            z = true;
        }
        this.isLastSampleQueued = z;
        this.sharedSampleMetadata.discardFrom(discardFromIndex);
        if (this.length != 0) {
            int relativeLastWriteIndex = getRelativeIndex(this.length - 1);
            return this.offsets[relativeLastWriteIndex] + ((long) this.sizes[relativeLastWriteIndex]);
        }
        return 0L;
    }

    private boolean hasNextSample() {
        return this.readPosition != this.length;
    }

    private void onFormatResult(Format newFormat, FormatHolder outputFormatHolder) {
        Format formatCopyWithCryptoType;
        boolean isFirstFormat = this.downstreamFormat == null;
        DrmInitData oldDrmInitData = this.downstreamFormat == null ? null : this.downstreamFormat.drmInitData;
        this.downstreamFormat = newFormat;
        DrmInitData newDrmInitData = newFormat.drmInitData;
        if (this.drmSessionManager != null) {
            formatCopyWithCryptoType = newFormat.copyWithCryptoType(this.drmSessionManager.getCryptoType(newFormat));
        } else {
            formatCopyWithCryptoType = newFormat;
        }
        outputFormatHolder.format = formatCopyWithCryptoType;
        outputFormatHolder.drmSession = this.currentDrmSession;
        if (this.drmSessionManager == null) {
            return;
        }
        if (!isFirstFormat && Util.areEqual(oldDrmInitData, newDrmInitData)) {
            return;
        }
        DrmSession previousSession = this.currentDrmSession;
        this.currentDrmSession = this.drmSessionManager.acquireSession(this.drmEventDispatcher, newFormat);
        outputFormatHolder.drmSession = this.currentDrmSession;
        if (previousSession != null) {
            previousSession.release(this.drmEventDispatcher);
        }
    }

    private boolean mayReadSample(int relativeReadIndex) {
        return this.currentDrmSession == null || this.currentDrmSession.getState() == 4 || ((this.flags[relativeReadIndex] & 1073741824) == 0 && this.currentDrmSession.playClearSamplesWithoutKeys());
    }

    private int findSampleBefore(int relativeStartIndex, int length, long timeUs, boolean keyframe) {
        int sampleCountToTarget = -1;
        int searchIndex = relativeStartIndex;
        for (int i = 0; i < length && this.timesUs[searchIndex] <= timeUs; i++) {
            if (!keyframe || (this.flags[searchIndex] & 1) != 0) {
                sampleCountToTarget = i;
                if (this.timesUs[searchIndex] == timeUs) {
                    break;
                }
            }
            searchIndex++;
            if (searchIndex == this.capacity) {
                searchIndex = 0;
            }
        }
        return sampleCountToTarget;
    }

    private int findSampleAfter(int relativeStartIndex, int length, long timeUs, boolean allowTimeBeyondBuffer) {
        int searchIndex = relativeStartIndex;
        for (int i = 0; i < length; i++) {
            if (this.timesUs[searchIndex] >= timeUs) {
                return i;
            }
            searchIndex++;
            if (searchIndex == this.capacity) {
                searchIndex = 0;
            }
        }
        if (allowTimeBeyondBuffer) {
            return length;
        }
        return -1;
    }

    private int countUnreadSamplesBefore(long timeUs) {
        int count = this.length;
        int relativeSampleIndex = getRelativeIndex(this.length - 1);
        while (count > this.readPosition && this.timesUs[relativeSampleIndex] >= timeUs) {
            count--;
            relativeSampleIndex--;
            if (relativeSampleIndex == -1) {
                relativeSampleIndex = this.capacity - 1;
            }
        }
        return count;
    }

    private long discardSamples(int discardCount) {
        this.largestDiscardedTimestampUs = Math.max(this.largestDiscardedTimestampUs, getLargestTimestamp(discardCount));
        this.length -= discardCount;
        this.absoluteFirstIndex += discardCount;
        this.relativeFirstIndex += discardCount;
        if (this.relativeFirstIndex >= this.capacity) {
            this.relativeFirstIndex -= this.capacity;
        }
        this.readPosition -= discardCount;
        if (this.readPosition < 0) {
            this.readPosition = 0;
        }
        this.sharedSampleMetadata.discardTo(this.absoluteFirstIndex);
        if (this.length == 0) {
            int relativeLastDiscardIndex = (this.relativeFirstIndex == 0 ? this.capacity : this.relativeFirstIndex) - 1;
            return this.offsets[relativeLastDiscardIndex] + ((long) this.sizes[relativeLastDiscardIndex]);
        }
        return this.offsets[this.relativeFirstIndex];
    }

    private long getLargestTimestamp(int length) {
        if (length == 0) {
            return Long.MIN_VALUE;
        }
        long largestTimestampUs = Long.MIN_VALUE;
        int relativeSampleIndex = getRelativeIndex(length - 1);
        for (int i = 0; i < length; i++) {
            largestTimestampUs = Math.max(largestTimestampUs, this.timesUs[relativeSampleIndex]);
            if ((this.flags[relativeSampleIndex] & 1) != 0) {
                break;
            }
            relativeSampleIndex--;
            if (relativeSampleIndex == -1) {
                relativeSampleIndex = this.capacity - 1;
            }
        }
        return largestTimestampUs;
    }

    private int getRelativeIndex(int offset) {
        int relativeIndex = this.relativeFirstIndex + offset;
        return relativeIndex < this.capacity ? relativeIndex : relativeIndex - this.capacity;
    }

    static final class SampleExtrasHolder {
        public TrackOutput.CryptoData cryptoData;
        public long offset;
        public int size;

        SampleExtrasHolder() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class SharedSampleMetadata {
        public final DrmSessionManager.DrmSessionReference drmSessionReference;
        public final Format format;

        private SharedSampleMetadata(Format format, DrmSessionManager.DrmSessionReference drmSessionReference) {
            this.format = format;
            this.drmSessionReference = drmSessionReference;
        }
    }
}
