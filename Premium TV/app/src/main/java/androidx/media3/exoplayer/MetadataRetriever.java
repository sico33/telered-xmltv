package androidx.media3.exoplayer;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.HandlerWrapper;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.DefaultAllocator;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ExtractorsFactory;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/* JADX INFO: loaded from: classes.dex */
public final class MetadataRetriever {
    private MetadataRetriever() {
    }

    public static ListenableFuture<TrackGroupArray> retrieveMetadata(Context context, MediaItem mediaItem) {
        return retrieveMetadata(context, mediaItem, Clock.DEFAULT);
    }

    public static ListenableFuture<TrackGroupArray> retrieveMetadata(MediaSource.Factory mediaSourceFactory, MediaItem mediaItem) {
        return retrieveMetadata(mediaSourceFactory, mediaItem, Clock.DEFAULT);
    }

    static ListenableFuture<TrackGroupArray> retrieveMetadata(Context context, MediaItem mediaItem, Clock clock) {
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory().setMp4ExtractorFlags(6);
        MediaSource.Factory mediaSourceFactory = new DefaultMediaSourceFactory(context, extractorsFactory);
        return retrieveMetadata(mediaSourceFactory, mediaItem, clock);
    }

    private static ListenableFuture<TrackGroupArray> retrieveMetadata(MediaSource.Factory mediaSourceFactory, MediaItem mediaItem, Clock clock) {
        return new MetadataRetrieverInternal(mediaSourceFactory, clock).retrieveMetadata(mediaItem);
    }

    private static final class MetadataRetrieverInternal {
        private static final int MESSAGE_CHECK_FOR_FAILURE = 2;
        private static final int MESSAGE_CONTINUE_LOADING = 3;
        private static final int MESSAGE_PREPARE_SOURCE = 1;
        private static final int MESSAGE_RELEASE = 4;
        private final MediaSource.Factory mediaSourceFactory;
        private final HandlerWrapper mediaSourceHandler;
        private final HandlerThread mediaSourceThread = new HandlerThread("ExoPlayer:MetadataRetriever");
        private final SettableFuture<TrackGroupArray> trackGroupsFuture;

        public MetadataRetrieverInternal(MediaSource.Factory mediaSourceFactory, Clock clock) {
            this.mediaSourceFactory = mediaSourceFactory;
            this.mediaSourceThread.start();
            this.mediaSourceHandler = clock.createHandler(this.mediaSourceThread.getLooper(), new MediaSourceHandlerCallback());
            this.trackGroupsFuture = SettableFuture.create();
        }

        public ListenableFuture<TrackGroupArray> retrieveMetadata(MediaItem mediaItem) {
            this.mediaSourceHandler.obtainMessage(1, mediaItem).sendToTarget();
            return this.trackGroupsFuture;
        }

        private final class MediaSourceHandlerCallback implements Handler.Callback {
            private static final int ERROR_POLL_INTERVAL_MS = 100;
            private MediaPeriod mediaPeriod;
            private MediaSource mediaSource;
            private final MediaSourceCaller mediaSourceCaller = new MediaSourceCaller();

            public MediaSourceHandlerCallback() {
            }

            @Override // android.os.Handler.Callback
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        MediaItem mediaItem = (MediaItem) msg.obj;
                        this.mediaSource = MetadataRetrieverInternal.this.mediaSourceFactory.createMediaSource(mediaItem);
                        this.mediaSource.prepareSource(this.mediaSourceCaller, null, PlayerId.UNSET);
                        MetadataRetrieverInternal.this.mediaSourceHandler.sendEmptyMessage(2);
                        return true;
                    case 2:
                        try {
                            if (this.mediaPeriod == null) {
                                ((MediaSource) Assertions.checkNotNull(this.mediaSource)).maybeThrowSourceInfoRefreshError();
                            } else {
                                this.mediaPeriod.maybeThrowPrepareError();
                            }
                            MetadataRetrieverInternal.this.mediaSourceHandler.sendEmptyMessageDelayed(2, 100);
                            break;
                        } catch (Exception e) {
                            MetadataRetrieverInternal.this.trackGroupsFuture.setException(e);
                            MetadataRetrieverInternal.this.mediaSourceHandler.obtainMessage(4).sendToTarget();
                        }
                        return true;
                    case 3:
                        ((MediaPeriod) Assertions.checkNotNull(this.mediaPeriod)).continueLoading(new LoadingInfo.Builder().setPlaybackPositionUs(0L).build());
                        return true;
                    case 4:
                        if (this.mediaPeriod != null) {
                            ((MediaSource) Assertions.checkNotNull(this.mediaSource)).releasePeriod(this.mediaPeriod);
                        }
                        ((MediaSource) Assertions.checkNotNull(this.mediaSource)).releaseSource(this.mediaSourceCaller);
                        MetadataRetrieverInternal.this.mediaSourceHandler.removeCallbacksAndMessages(null);
                        MetadataRetrieverInternal.this.mediaSourceThread.quit();
                        return true;
                    default:
                        return false;
                }
            }

            private final class MediaSourceCaller implements MediaSource.MediaSourceCaller {
                private boolean mediaPeriodCreated;
                private final MediaPeriodCallback mediaPeriodCallback = new MediaPeriodCallback();
                private final Allocator allocator = new DefaultAllocator(true, 65536);

                public MediaSourceCaller() {
                }

                @Override // androidx.media3.exoplayer.source.MediaSource.MediaSourceCaller
                public void onSourceInfoRefreshed(MediaSource source, Timeline timeline) {
                    if (this.mediaPeriodCreated) {
                        return;
                    }
                    this.mediaPeriodCreated = true;
                    MediaSourceHandlerCallback.this.mediaPeriod = source.createPeriod(new MediaSource.MediaPeriodId(timeline.getUidOfPeriod(0)), this.allocator, 0L);
                    MediaSourceHandlerCallback.this.mediaPeriod.prepare(this.mediaPeriodCallback, 0L);
                }

                private final class MediaPeriodCallback implements MediaPeriod.Callback {
                    private MediaPeriodCallback() {
                    }

                    @Override // androidx.media3.exoplayer.source.MediaPeriod.Callback
                    public void onPrepared(MediaPeriod mediaPeriod) {
                        MetadataRetrieverInternal.this.trackGroupsFuture.set(mediaPeriod.getTrackGroups());
                        MetadataRetrieverInternal.this.mediaSourceHandler.obtainMessage(4).sendToTarget();
                    }

                    @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
                    public void onContinueLoadingRequested(MediaPeriod mediaPeriod) {
                        MetadataRetrieverInternal.this.mediaSourceHandler.obtainMessage(3).sendToTarget();
                    }
                }
            }
        }
    }
}
