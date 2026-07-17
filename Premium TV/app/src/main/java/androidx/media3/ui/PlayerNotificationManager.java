package androidx.media3.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.core.app.NotificationBuilderWithBuilderAccessor;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.NotificationUtil;
import androidx.media3.common.util.Util;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class PlayerNotificationManager {
    private static final String ACTION_DISMISS = "androidx.media3.ui.notification.dismiss";
    public static final String ACTION_FAST_FORWARD = "androidx.media3.ui.notification.ffwd";
    public static final String ACTION_NEXT = "androidx.media3.ui.notification.next";
    public static final String ACTION_PAUSE = "androidx.media3.ui.notification.pause";
    public static final String ACTION_PLAY = "androidx.media3.ui.notification.play";
    public static final String ACTION_PREVIOUS = "androidx.media3.ui.notification.prev";
    public static final String ACTION_REWIND = "androidx.media3.ui.notification.rewind";
    public static final String ACTION_STOP = "androidx.media3.ui.notification.stop";
    public static final String EXTRA_INSTANCE_ID = "INSTANCE_ID";
    private static final int MSG_START_OR_UPDATE_NOTIFICATION = 1;
    private static final int MSG_UPDATE_NOTIFICATION_BITMAP = 2;
    private static int instanceIdCounter;
    private int badgeIconType;
    private NotificationCompat.Builder builder;
    private List<NotificationCompat.Action> builderActions;
    private final String channelId;
    private int color;
    private boolean colorized;
    private final Context context;
    private int currentNotificationTag;
    private final CustomActionReceiver customActionReceiver;
    private final Map<String, NotificationCompat.Action> customActions;
    private int defaults;
    private final PendingIntent dismissPendingIntent;
    private String groupKey;
    private final int instanceId;
    private final IntentFilter intentFilter;
    private boolean isNotificationStarted;
    private final Handler mainHandler;
    private final MediaDescriptionAdapter mediaDescriptionAdapter;
    private MediaSession.Token mediaSessionToken;
    private final NotificationBroadcastReceiver notificationBroadcastReceiver;
    private final int notificationId;
    private final NotificationListener notificationListener;
    private final NotificationManagerCompat notificationManager;
    private final Map<String, NotificationCompat.Action> playbackActions;
    private Player player;
    private final Player.Listener playerListener;
    private int priority;
    private boolean showPlayButtonIfSuppressed;
    private int smallIconResourceId;
    private boolean useChronometer;
    private boolean useFastForwardAction;
    private boolean useFastForwardActionInCompactView;
    private boolean useNextAction;
    private boolean useNextActionInCompactView;
    private boolean usePlayPauseActions;
    private boolean usePreviousAction;
    private boolean usePreviousActionInCompactView;
    private boolean useRewindAction;
    private boolean useRewindActionInCompactView;
    private boolean useStopAction;
    private int visibility;

    public interface CustomActionReceiver {
        Map<String, NotificationCompat.Action> createCustomActions(Context context, int i);

        List<String> getCustomActions(Player player);

        void onCustomAction(Player player, String str, Intent intent);
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Priority {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Visibility {
    }

    public interface MediaDescriptionAdapter {
        PendingIntent createCurrentContentIntent(Player player);

        CharSequence getCurrentContentText(Player player);

        CharSequence getCurrentContentTitle(Player player);

        Bitmap getCurrentLargeIcon(Player player, BitmapCallback bitmapCallback);

        CharSequence getCurrentSubText(Player player);

        /* JADX INFO: renamed from: androidx.media3.ui.PlayerNotificationManager$MediaDescriptionAdapter$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static CharSequence $default$getCurrentSubText(MediaDescriptionAdapter _this, Player player) {
                return null;
            }
        }
    }

    public interface NotificationListener {
        void onNotificationCancelled(int i, boolean z);

        void onNotificationPosted(int i, Notification notification, boolean z);

        /* JADX INFO: renamed from: androidx.media3.ui.PlayerNotificationManager$NotificationListener$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static void $default$onNotificationCancelled(NotificationListener _this, int notificationId, boolean dismissedByUser) {
            }

            public static void $default$onNotificationPosted(NotificationListener _this, int notificationId, Notification notification, boolean ongoing) {
            }
        }
    }

    public static class Builder {
        protected int channelDescriptionResourceId;
        protected final String channelId;
        protected int channelImportance;
        protected int channelNameResourceId;
        protected final Context context;
        protected CustomActionReceiver customActionReceiver;
        protected int fastForwardActionIconResourceId;
        protected String groupKey;
        protected MediaDescriptionAdapter mediaDescriptionAdapter;
        protected int nextActionIconResourceId;
        protected final int notificationId;
        protected NotificationListener notificationListener;
        protected int pauseActionIconResourceId;
        protected int playActionIconResourceId;
        protected int previousActionIconResourceId;
        protected int rewindActionIconResourceId;
        protected int smallIconResourceId;
        protected int stopActionIconResourceId;

        @Deprecated
        public Builder(Context context, int notificationId, String channelId, MediaDescriptionAdapter mediaDescriptionAdapter) {
            this(context, notificationId, channelId);
            this.mediaDescriptionAdapter = mediaDescriptionAdapter;
        }

        public Builder(Context context, int notificationId, String channelId) {
            Assertions.checkArgument(notificationId > 0);
            this.context = context;
            this.notificationId = notificationId;
            this.channelId = channelId;
            this.channelImportance = 2;
            this.mediaDescriptionAdapter = new DefaultMediaDescriptionAdapter(null);
            this.smallIconResourceId = R.drawable.exo_notification_small_icon;
            this.playActionIconResourceId = R.drawable.exo_notification_play;
            this.pauseActionIconResourceId = R.drawable.exo_notification_pause;
            this.stopActionIconResourceId = R.drawable.exo_notification_stop;
            this.rewindActionIconResourceId = R.drawable.exo_notification_rewind;
            this.fastForwardActionIconResourceId = R.drawable.exo_notification_fastforward;
            this.previousActionIconResourceId = R.drawable.exo_notification_previous;
            this.nextActionIconResourceId = R.drawable.exo_notification_next;
        }

        public Builder setChannelNameResourceId(int channelNameResourceId) {
            this.channelNameResourceId = channelNameResourceId;
            return this;
        }

        public Builder setChannelDescriptionResourceId(int channelDescriptionResourceId) {
            this.channelDescriptionResourceId = channelDescriptionResourceId;
            return this;
        }

        public Builder setChannelImportance(int channelImportance) {
            this.channelImportance = channelImportance;
            return this;
        }

        public Builder setNotificationListener(NotificationListener notificationListener) {
            this.notificationListener = notificationListener;
            return this;
        }

        public Builder setCustomActionReceiver(CustomActionReceiver customActionReceiver) {
            this.customActionReceiver = customActionReceiver;
            return this;
        }

        public Builder setSmallIconResourceId(int smallIconResourceId) {
            this.smallIconResourceId = smallIconResourceId;
            return this;
        }

        public Builder setPlayActionIconResourceId(int playActionIconResourceId) {
            this.playActionIconResourceId = playActionIconResourceId;
            return this;
        }

        public Builder setPauseActionIconResourceId(int pauseActionIconResourceId) {
            this.pauseActionIconResourceId = pauseActionIconResourceId;
            return this;
        }

        public Builder setStopActionIconResourceId(int stopActionIconResourceId) {
            this.stopActionIconResourceId = stopActionIconResourceId;
            return this;
        }

        public Builder setRewindActionIconResourceId(int rewindActionIconResourceId) {
            this.rewindActionIconResourceId = rewindActionIconResourceId;
            return this;
        }

        public Builder setFastForwardActionIconResourceId(int fastForwardActionIconResourceId) {
            this.fastForwardActionIconResourceId = fastForwardActionIconResourceId;
            return this;
        }

        public Builder setPreviousActionIconResourceId(int previousActionIconResourceId) {
            this.previousActionIconResourceId = previousActionIconResourceId;
            return this;
        }

        public Builder setNextActionIconResourceId(int nextActionIconResourceId) {
            this.nextActionIconResourceId = nextActionIconResourceId;
            return this;
        }

        public Builder setGroup(String groupKey) {
            this.groupKey = groupKey;
            return this;
        }

        public Builder setMediaDescriptionAdapter(MediaDescriptionAdapter mediaDescriptionAdapter) {
            this.mediaDescriptionAdapter = mediaDescriptionAdapter;
            return this;
        }

        public PlayerNotificationManager build() {
            if (this.channelNameResourceId != 0) {
                NotificationUtil.createNotificationChannel(this.context, this.channelId, this.channelNameResourceId, this.channelDescriptionResourceId, this.channelImportance);
            }
            return new PlayerNotificationManager(this.context, this.channelId, this.notificationId, this.mediaDescriptionAdapter, this.notificationListener, this.customActionReceiver, this.smallIconResourceId, this.playActionIconResourceId, this.pauseActionIconResourceId, this.stopActionIconResourceId, this.rewindActionIconResourceId, this.fastForwardActionIconResourceId, this.previousActionIconResourceId, this.nextActionIconResourceId, this.groupKey);
        }
    }

    public final class BitmapCallback {
        private final int notificationTag;

        private BitmapCallback(int notificationTag) {
            this.notificationTag = notificationTag;
        }

        public void onBitmap(Bitmap bitmap) {
            if (bitmap != null) {
                PlayerNotificationManager.this.postUpdateNotificationBitmap(bitmap, this.notificationTag);
            }
        }
    }

    protected PlayerNotificationManager(Context context, String channelId, int notificationId, MediaDescriptionAdapter mediaDescriptionAdapter, NotificationListener notificationListener, CustomActionReceiver customActionReceiver, int smallIconResourceId, int playActionIconResourceId, int pauseActionIconResourceId, int stopActionIconResourceId, int rewindActionIconResourceId, int fastForwardActionIconResourceId, int previousActionIconResourceId, int nextActionIconResourceId, String groupKey) {
        Map<String, NotificationCompat.Action> mapEmptyMap;
        Context context2 = context.getApplicationContext();
        this.context = context2;
        this.channelId = channelId;
        this.notificationId = notificationId;
        this.mediaDescriptionAdapter = mediaDescriptionAdapter;
        this.notificationListener = notificationListener;
        this.customActionReceiver = customActionReceiver;
        this.smallIconResourceId = smallIconResourceId;
        this.groupKey = groupKey;
        int i = instanceIdCounter;
        instanceIdCounter = i + 1;
        this.instanceId = i;
        Handler mainHandler = Util.createHandler(Looper.getMainLooper(), new Handler.Callback() { // from class: androidx.media3.ui.PlayerNotificationManager$$ExternalSyntheticLambda0
            @Override // android.os.Handler.Callback
            public final boolean handleMessage(Message message) {
                return this.f$0.handleMessage(message);
            }
        });
        this.mainHandler = mainHandler;
        this.notificationManager = NotificationManagerCompat.from(context2);
        this.playerListener = new PlayerListener();
        this.notificationBroadcastReceiver = new NotificationBroadcastReceiver();
        this.intentFilter = new IntentFilter();
        this.usePreviousAction = true;
        this.useNextAction = true;
        this.usePlayPauseActions = true;
        this.showPlayButtonIfSuppressed = true;
        this.useRewindAction = true;
        this.useFastForwardAction = true;
        this.colorized = true;
        this.useChronometer = true;
        this.color = 0;
        this.defaults = 0;
        this.priority = -1;
        this.badgeIconType = 1;
        this.visibility = 1;
        this.playbackActions = createPlaybackActions(context2, this.instanceId, playActionIconResourceId, pauseActionIconResourceId, stopActionIconResourceId, rewindActionIconResourceId, fastForwardActionIconResourceId, previousActionIconResourceId, nextActionIconResourceId);
        for (String action : this.playbackActions.keySet()) {
            this.intentFilter.addAction(action);
        }
        if (customActionReceiver != null) {
            mapEmptyMap = customActionReceiver.createCustomActions(context2, this.instanceId);
        } else {
            mapEmptyMap = Collections.emptyMap();
        }
        this.customActions = mapEmptyMap;
        for (String action2 : this.customActions.keySet()) {
            this.intentFilter.addAction(action2);
        }
        this.dismissPendingIntent = createBroadcastIntent(ACTION_DISMISS, context2, this.instanceId);
        this.intentFilter.addAction(ACTION_DISMISS);
    }

    public final void setPlayer(Player player) {
        boolean z = true;
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper());
        if (player != null && player.getApplicationLooper() != Looper.getMainLooper()) {
            z = false;
        }
        Assertions.checkArgument(z);
        if (this.player == player) {
            return;
        }
        if (this.player != null) {
            this.player.removeListener(this.playerListener);
            if (player == null) {
                stopNotification(false);
            }
        }
        this.player = player;
        if (player != null) {
            player.addListener(this.playerListener);
            postStartOrUpdateNotification();
        }
    }

    public final void setUseNextAction(boolean useNextAction) {
        if (this.useNextAction != useNextAction) {
            this.useNextAction = useNextAction;
            invalidate();
        }
    }

    public final void setUsePreviousAction(boolean usePreviousAction) {
        if (this.usePreviousAction != usePreviousAction) {
            this.usePreviousAction = usePreviousAction;
            invalidate();
        }
    }

    public final void setUseNextActionInCompactView(boolean useNextActionInCompactView) {
        if (this.useNextActionInCompactView != useNextActionInCompactView) {
            this.useNextActionInCompactView = useNextActionInCompactView;
            if (useNextActionInCompactView) {
                this.useFastForwardActionInCompactView = false;
            }
            invalidate();
        }
    }

    public final void setUsePreviousActionInCompactView(boolean usePreviousActionInCompactView) {
        if (this.usePreviousActionInCompactView != usePreviousActionInCompactView) {
            this.usePreviousActionInCompactView = usePreviousActionInCompactView;
            if (usePreviousActionInCompactView) {
                this.useRewindActionInCompactView = false;
            }
            invalidate();
        }
    }

    public final void setUseFastForwardAction(boolean useFastForwardAction) {
        if (this.useFastForwardAction != useFastForwardAction) {
            this.useFastForwardAction = useFastForwardAction;
            invalidate();
        }
    }

    public final void setUseRewindAction(boolean useRewindAction) {
        if (this.useRewindAction != useRewindAction) {
            this.useRewindAction = useRewindAction;
            invalidate();
        }
    }

    public final void setUseFastForwardActionInCompactView(boolean useFastForwardActionInCompactView) {
        if (this.useFastForwardActionInCompactView != useFastForwardActionInCompactView) {
            this.useFastForwardActionInCompactView = useFastForwardActionInCompactView;
            if (useFastForwardActionInCompactView) {
                this.useNextActionInCompactView = false;
            }
            invalidate();
        }
    }

    public final void setUseRewindActionInCompactView(boolean useRewindActionInCompactView) {
        if (this.useRewindActionInCompactView != useRewindActionInCompactView) {
            this.useRewindActionInCompactView = useRewindActionInCompactView;
            if (useRewindActionInCompactView) {
                this.usePreviousActionInCompactView = false;
            }
            invalidate();
        }
    }

    public final void setUsePlayPauseActions(boolean usePlayPauseActions) {
        if (this.usePlayPauseActions != usePlayPauseActions) {
            this.usePlayPauseActions = usePlayPauseActions;
            invalidate();
        }
    }

    public void setShowPlayButtonIfPlaybackIsSuppressed(boolean showPlayButtonIfSuppressed) {
        if (this.showPlayButtonIfSuppressed != showPlayButtonIfSuppressed) {
            this.showPlayButtonIfSuppressed = showPlayButtonIfSuppressed;
            invalidate();
        }
    }

    public final void setUseStopAction(boolean useStopAction) {
        if (this.useStopAction == useStopAction) {
            return;
        }
        this.useStopAction = useStopAction;
        invalidate();
    }

    @Deprecated
    public final void setMediaSessionToken(MediaSessionCompat.Token compatToken) {
        if (Util.SDK_INT >= 21) {
            setMediaSessionToken((MediaSession.Token) compatToken.getToken());
        }
    }

    public final void setMediaSessionToken(MediaSession.Token token) {
        if (!Util.areEqual(this.mediaSessionToken, token)) {
            this.mediaSessionToken = token;
            invalidate();
        }
    }

    public final void setBadgeIconType(int badgeIconType) {
        if (this.badgeIconType == badgeIconType) {
            return;
        }
        switch (badgeIconType) {
            case 0:
            case 1:
            case 2:
                this.badgeIconType = badgeIconType;
                invalidate();
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    public final void setColorized(boolean colorized) {
        if (this.colorized != colorized) {
            this.colorized = colorized;
            invalidate();
        }
    }

    public final void setDefaults(int defaults) {
        if (this.defaults != defaults) {
            this.defaults = defaults;
            invalidate();
        }
    }

    public final void setColor(int color) {
        if (this.color != color) {
            this.color = color;
            invalidate();
        }
    }

    public final void setPriority(int priority) {
        if (this.priority == priority) {
            return;
        }
        switch (priority) {
            case -2:
            case -1:
            case 0:
            case 1:
            case 2:
                this.priority = priority;
                invalidate();
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    public final void setSmallIcon(int smallIconResourceId) {
        if (this.smallIconResourceId != smallIconResourceId) {
            this.smallIconResourceId = smallIconResourceId;
            invalidate();
        }
    }

    public final void setUseChronometer(boolean useChronometer) {
        if (this.useChronometer != useChronometer) {
            this.useChronometer = useChronometer;
            invalidate();
        }
    }

    public final void setVisibility(int visibility) {
        if (this.visibility == visibility) {
            return;
        }
        switch (visibility) {
            case -1:
            case 0:
            case 1:
                this.visibility = visibility;
                invalidate();
                return;
            default:
                throw new IllegalStateException();
        }
    }

    public final void invalidate() {
        if (this.isNotificationStarted) {
            postStartOrUpdateNotification();
        }
    }

    private void startOrUpdateNotification(Player player, Bitmap bitmap) {
        boolean ongoing = getOngoing(player);
        this.builder = createNotification(player, this.builder, ongoing, bitmap);
        if (this.builder == null) {
            stopNotification(false);
            return;
        }
        Notification notification = this.builder.build();
        this.notificationManager.notify(this.notificationId, notification);
        if (!this.isNotificationStarted) {
            Util.registerReceiverNotExported(this.context, this.notificationBroadcastReceiver, this.intentFilter);
        }
        if (this.notificationListener != null) {
            this.notificationListener.onNotificationPosted(this.notificationId, notification, ongoing || !this.isNotificationStarted);
        }
        this.isNotificationStarted = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopNotification(boolean dismissedByUser) {
        if (this.isNotificationStarted) {
            this.isNotificationStarted = false;
            this.mainHandler.removeMessages(1);
            this.notificationManager.cancel(this.notificationId);
            this.context.unregisterReceiver(this.notificationBroadcastReceiver);
            if (this.notificationListener != null) {
                this.notificationListener.onNotificationCancelled(this.notificationId, dismissedByUser);
            }
        }
    }

    protected NotificationCompat.Builder createNotification(Player player, NotificationCompat.Builder builder, boolean ongoing, Bitmap largeIcon) {
        NotificationCompat.Action action;
        if (player.getPlaybackState() == 1 && player.isCommandAvailable(17) && player.getCurrentTimeline().isEmpty()) {
            this.builderActions = null;
            return null;
        }
        List<String> actionNames = getActions(player);
        List<NotificationCompat.Action> actions = new ArrayList<>(actionNames.size());
        for (int i = 0; i < actionNames.size(); i++) {
            String actionName = actionNames.get(i);
            if (this.playbackActions.containsKey(actionName)) {
                action = this.playbackActions.get(actionName);
            } else {
                action = this.customActions.get(actionName);
            }
            if (action != null) {
                actions.add(action);
            }
        }
        if (builder == null || !actions.equals(this.builderActions)) {
            builder = new NotificationCompat.Builder(this.context, this.channelId);
            this.builderActions = actions;
            for (int i2 = 0; i2 < actions.size(); i2++) {
                builder.addAction(actions.get(i2));
            }
        }
        int[] actionIndicesForCompactView = getActionIndicesForCompactView(actionNames, player);
        if (Util.SDK_INT >= 21) {
            builder.setStyle(new MediaStyle(this.mediaSessionToken, actionIndicesForCompactView));
        } else {
            androidx.media.app.NotificationCompat.MediaStyle mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle();
            mediaStyle.setShowActionsInCompactView(actionIndicesForCompactView);
            mediaStyle.setShowCancelButton(!ongoing);
            mediaStyle.setCancelButtonIntent(this.dismissPendingIntent);
            builder.setStyle(mediaStyle);
        }
        builder.setDeleteIntent(this.dismissPendingIntent);
        builder.setBadgeIconType(this.badgeIconType).setOngoing(ongoing).setColor(this.color).setColorized(this.colorized).setSmallIcon(this.smallIconResourceId).setVisibility(this.visibility).setPriority(this.priority).setDefaults(this.defaults);
        if (Util.SDK_INT >= 21 && this.useChronometer && player.isCommandAvailable(16) && player.isPlaying() && !player.isPlayingAd() && !player.isCurrentMediaItemDynamic() && player.getPlaybackParameters().speed == 1.0f) {
            builder.setWhen(System.currentTimeMillis() - player.getContentPosition()).setShowWhen(true).setUsesChronometer(true);
        } else {
            builder.setShowWhen(false).setUsesChronometer(false);
        }
        builder.setContentTitle(this.mediaDescriptionAdapter.getCurrentContentTitle(player));
        builder.setContentText(this.mediaDescriptionAdapter.getCurrentContentText(player));
        builder.setSubText(this.mediaDescriptionAdapter.getCurrentSubText(player));
        if (largeIcon == null) {
            MediaDescriptionAdapter mediaDescriptionAdapter = this.mediaDescriptionAdapter;
            int i3 = this.currentNotificationTag + 1;
            this.currentNotificationTag = i3;
            largeIcon = mediaDescriptionAdapter.getCurrentLargeIcon(player, new BitmapCallback(i3));
        }
        setLargeIcon(builder, largeIcon);
        builder.setContentIntent(this.mediaDescriptionAdapter.createCurrentContentIntent(player));
        if (this.groupKey != null) {
            builder.setGroup(this.groupKey);
        }
        builder.setOnlyAlertOnce(true);
        return builder;
    }

    protected List<String> getActions(Player player) {
        boolean enablePrevious = player.isCommandAvailable(7);
        boolean enableRewind = player.isCommandAvailable(11);
        boolean enableFastForward = player.isCommandAvailable(12);
        boolean enableNext = player.isCommandAvailable(9);
        List<String> stringActions = new ArrayList<>();
        if (this.usePreviousAction && enablePrevious) {
            stringActions.add(ACTION_PREVIOUS);
        }
        if (this.useRewindAction && enableRewind) {
            stringActions.add(ACTION_REWIND);
        }
        if (this.usePlayPauseActions) {
            if (Util.shouldShowPlayButton(player, this.showPlayButtonIfSuppressed)) {
                stringActions.add(ACTION_PLAY);
            } else {
                stringActions.add(ACTION_PAUSE);
            }
        }
        if (this.useFastForwardAction && enableFastForward) {
            stringActions.add(ACTION_FAST_FORWARD);
        }
        if (this.useNextAction && enableNext) {
            stringActions.add(ACTION_NEXT);
        }
        if (this.customActionReceiver != null) {
            stringActions.addAll(this.customActionReceiver.getCustomActions(player));
        }
        if (this.useStopAction) {
            stringActions.add(ACTION_STOP);
        }
        return stringActions;
    }

    protected int[] getActionIndicesForCompactView(List<String> actionNames, Player player) {
        int leftSideActionIndex;
        int rightSideActionIndex;
        int pauseActionIndex = actionNames.indexOf(ACTION_PAUSE);
        int playActionIndex = actionNames.indexOf(ACTION_PLAY);
        if (this.usePreviousActionInCompactView) {
            leftSideActionIndex = actionNames.indexOf(ACTION_PREVIOUS);
        } else {
            leftSideActionIndex = this.useRewindActionInCompactView ? actionNames.indexOf(ACTION_REWIND) : -1;
        }
        if (this.useNextActionInCompactView) {
            rightSideActionIndex = actionNames.indexOf(ACTION_NEXT);
        } else {
            rightSideActionIndex = this.useFastForwardActionInCompactView ? actionNames.indexOf(ACTION_FAST_FORWARD) : -1;
        }
        int[] actionIndices = new int[3];
        int actionCounter = 0;
        if (leftSideActionIndex != -1) {
            int actionCounter2 = 0 + 1;
            actionIndices[0] = leftSideActionIndex;
            actionCounter = actionCounter2;
        }
        boolean shouldShowPlayButton = Util.shouldShowPlayButton(player, this.showPlayButtonIfSuppressed);
        if (pauseActionIndex != -1 && !shouldShowPlayButton) {
            actionIndices[actionCounter] = pauseActionIndex;
            actionCounter++;
        } else if (playActionIndex != -1 && shouldShowPlayButton) {
            actionIndices[actionCounter] = playActionIndex;
            actionCounter++;
        }
        if (rightSideActionIndex != -1) {
            actionIndices[actionCounter] = rightSideActionIndex;
            actionCounter++;
        }
        return Arrays.copyOf(actionIndices, actionCounter);
    }

    protected boolean getOngoing(Player player) {
        int playbackState = player.getPlaybackState();
        return (playbackState == 2 || playbackState == 3) && player.getPlayWhenReady();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postStartOrUpdateNotification() {
        if (!this.mainHandler.hasMessages(1)) {
            this.mainHandler.sendEmptyMessage(1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postUpdateNotificationBitmap(Bitmap bitmap, int notificationTag) {
        this.mainHandler.obtainMessage(2, notificationTag, -1, bitmap).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                if (this.player != null) {
                    startOrUpdateNotification(this.player, null);
                    return true;
                }
                return true;
            case 2:
                if (this.player != null && this.isNotificationStarted && this.currentNotificationTag == msg.arg1) {
                    startOrUpdateNotification(this.player, (Bitmap) msg.obj);
                    return true;
                }
                return true;
            default:
                return false;
        }
    }

    private static Map<String, NotificationCompat.Action> createPlaybackActions(Context context, int instanceId, int playActionIconResourceId, int pauseActionIconResourceId, int stopActionIconResourceId, int rewindActionIconResourceId, int fastForwardActionIconResourceId, int previousActionIconResourceId, int nextActionIconResourceId) {
        Map<String, NotificationCompat.Action> actions = new HashMap<>();
        actions.put(ACTION_PLAY, new NotificationCompat.Action(playActionIconResourceId, context.getString(R.string.exo_controls_play_description), createBroadcastIntent(ACTION_PLAY, context, instanceId)));
        actions.put(ACTION_PAUSE, new NotificationCompat.Action(pauseActionIconResourceId, context.getString(R.string.exo_controls_pause_description), createBroadcastIntent(ACTION_PAUSE, context, instanceId)));
        actions.put(ACTION_STOP, new NotificationCompat.Action(stopActionIconResourceId, context.getString(R.string.exo_controls_stop_description), createBroadcastIntent(ACTION_STOP, context, instanceId)));
        actions.put(ACTION_REWIND, new NotificationCompat.Action(rewindActionIconResourceId, context.getString(R.string.exo_controls_rewind_description), createBroadcastIntent(ACTION_REWIND, context, instanceId)));
        actions.put(ACTION_FAST_FORWARD, new NotificationCompat.Action(fastForwardActionIconResourceId, context.getString(R.string.exo_controls_fastforward_description), createBroadcastIntent(ACTION_FAST_FORWARD, context, instanceId)));
        actions.put(ACTION_PREVIOUS, new NotificationCompat.Action(previousActionIconResourceId, context.getString(R.string.exo_controls_previous_description), createBroadcastIntent(ACTION_PREVIOUS, context, instanceId)));
        actions.put(ACTION_NEXT, new NotificationCompat.Action(nextActionIconResourceId, context.getString(R.string.exo_controls_next_description), createBroadcastIntent(ACTION_NEXT, context, instanceId)));
        return actions;
    }

    private static PendingIntent createBroadcastIntent(String action, Context context, int instanceId) {
        int pendingFlags;
        Intent intent = new Intent(action).setPackage(context.getPackageName());
        intent.putExtra(EXTRA_INSTANCE_ID, instanceId);
        if (Util.SDK_INT >= 23) {
            pendingFlags = 201326592;
        } else {
            pendingFlags = C.BUFFER_FLAG_FIRST_SAMPLE;
        }
        return PendingIntent.getBroadcast(context, instanceId, intent, pendingFlags);
    }

    private static void setLargeIcon(NotificationCompat.Builder builder, Bitmap largeIcon) {
        builder.setLargeIcon(largeIcon);
    }

    private class PlayerListener implements Player.Listener {
        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onAudioAttributesChanged(AudioAttributes audioAttributes) {
            Player.Listener.CC.$default$onAudioAttributesChanged(this, audioAttributes);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onAudioSessionIdChanged(int i) {
            Player.Listener.CC.$default$onAudioSessionIdChanged(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onAvailableCommandsChanged(Player.Commands commands) {
            Player.Listener.CC.$default$onAvailableCommandsChanged(this, commands);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onCues(CueGroup cueGroup) {
            Player.Listener.CC.$default$onCues(this, cueGroup);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onCues(List list) {
            Player.Listener.CC.$default$onCues(this, list);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onDeviceInfoChanged(DeviceInfo deviceInfo) {
            Player.Listener.CC.$default$onDeviceInfoChanged(this, deviceInfo);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onDeviceVolumeChanged(int i, boolean z) {
            Player.Listener.CC.$default$onDeviceVolumeChanged(this, i, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onIsLoadingChanged(boolean z) {
            Player.Listener.CC.$default$onIsLoadingChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onIsPlayingChanged(boolean z) {
            Player.Listener.CC.$default$onIsPlayingChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onLoadingChanged(boolean z) {
            Player.Listener.CC.$default$onLoadingChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onMaxSeekToPreviousPositionChanged(long j) {
            Player.Listener.CC.$default$onMaxSeekToPreviousPositionChanged(this, j);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onMediaItemTransition(MediaItem mediaItem, int i) {
            Player.Listener.CC.$default$onMediaItemTransition(this, mediaItem, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
            Player.Listener.CC.$default$onMediaMetadataChanged(this, mediaMetadata);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onMetadata(Metadata metadata) {
            Player.Listener.CC.$default$onMetadata(this, metadata);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlayWhenReadyChanged(boolean z, int i) {
            Player.Listener.CC.$default$onPlayWhenReadyChanged(this, z, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Player.Listener.CC.$default$onPlaybackParametersChanged(this, playbackParameters);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlaybackStateChanged(int i) {
            Player.Listener.CC.$default$onPlaybackStateChanged(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlaybackSuppressionReasonChanged(int i) {
            Player.Listener.CC.$default$onPlaybackSuppressionReasonChanged(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlayerError(PlaybackException playbackException) {
            Player.Listener.CC.$default$onPlayerError(this, playbackException);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlayerErrorChanged(PlaybackException playbackException) {
            Player.Listener.CC.$default$onPlayerErrorChanged(this, playbackException);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlayerStateChanged(boolean z, int i) {
            Player.Listener.CC.$default$onPlayerStateChanged(this, z, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlaylistMetadataChanged(MediaMetadata mediaMetadata) {
            Player.Listener.CC.$default$onPlaylistMetadataChanged(this, mediaMetadata);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPositionDiscontinuity(int i) {
            Player.Listener.CC.$default$onPositionDiscontinuity(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPositionDiscontinuity(Player.PositionInfo positionInfo, Player.PositionInfo positionInfo2, int i) {
            Player.Listener.CC.$default$onPositionDiscontinuity(this, positionInfo, positionInfo2, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onRenderedFirstFrame() {
            Player.Listener.CC.$default$onRenderedFirstFrame(this);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onRepeatModeChanged(int i) {
            Player.Listener.CC.$default$onRepeatModeChanged(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onSeekBackIncrementChanged(long j) {
            Player.Listener.CC.$default$onSeekBackIncrementChanged(this, j);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onSeekForwardIncrementChanged(long j) {
            Player.Listener.CC.$default$onSeekForwardIncrementChanged(this, j);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onShuffleModeEnabledChanged(boolean z) {
            Player.Listener.CC.$default$onShuffleModeEnabledChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onSkipSilenceEnabledChanged(boolean z) {
            Player.Listener.CC.$default$onSkipSilenceEnabledChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onSurfaceSizeChanged(int i, int i2) {
            Player.Listener.CC.$default$onSurfaceSizeChanged(this, i, i2);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onTimelineChanged(Timeline timeline, int i) {
            Player.Listener.CC.$default$onTimelineChanged(this, timeline, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onTrackSelectionParametersChanged(TrackSelectionParameters trackSelectionParameters) {
            Player.Listener.CC.$default$onTrackSelectionParametersChanged(this, trackSelectionParameters);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onTracksChanged(Tracks tracks) {
            Player.Listener.CC.$default$onTracksChanged(this, tracks);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onVideoSizeChanged(VideoSize videoSize) {
            Player.Listener.CC.$default$onVideoSizeChanged(this, videoSize);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onVolumeChanged(float f) {
            Player.Listener.CC.$default$onVolumeChanged(this, f);
        }

        private PlayerListener() {
        }

        @Override // androidx.media3.common.Player.Listener
        public void onEvents(Player player, Player.Events events) {
            if (events.containsAny(4, 5, 7, 0, 12, 11, 8, 9, 14)) {
                PlayerNotificationManager.this.postStartOrUpdateNotification();
            }
        }
    }

    private class NotificationBroadcastReceiver extends BroadcastReceiver {
        private NotificationBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Player player = PlayerNotificationManager.this.player;
            if (player == null || !PlayerNotificationManager.this.isNotificationStarted || intent.getIntExtra(PlayerNotificationManager.EXTRA_INSTANCE_ID, PlayerNotificationManager.this.instanceId) != PlayerNotificationManager.this.instanceId) {
                return;
            }
            String action = intent.getAction();
            if (PlayerNotificationManager.ACTION_PLAY.equals(action)) {
                Util.handlePlayButtonAction(player);
                return;
            }
            if (PlayerNotificationManager.ACTION_PAUSE.equals(action)) {
                Util.handlePauseButtonAction(player);
                return;
            }
            if (PlayerNotificationManager.ACTION_PREVIOUS.equals(action)) {
                if (player.isCommandAvailable(7)) {
                    player.seekToPrevious();
                    return;
                }
                return;
            }
            if (PlayerNotificationManager.ACTION_REWIND.equals(action)) {
                if (player.isCommandAvailable(11)) {
                    player.seekBack();
                    return;
                }
                return;
            }
            if (PlayerNotificationManager.ACTION_FAST_FORWARD.equals(action)) {
                if (player.isCommandAvailable(12)) {
                    player.seekForward();
                    return;
                }
                return;
            }
            if (PlayerNotificationManager.ACTION_NEXT.equals(action)) {
                if (player.isCommandAvailable(9)) {
                    player.seekToNext();
                    return;
                }
                return;
            }
            if (PlayerNotificationManager.ACTION_STOP.equals(action)) {
                if (player.isCommandAvailable(3)) {
                    player.stop();
                }
                if (player.isCommandAvailable(20)) {
                    player.clearMediaItems();
                    return;
                }
                return;
            }
            if (PlayerNotificationManager.ACTION_DISMISS.equals(action)) {
                PlayerNotificationManager.this.stopNotification(true);
            } else if (action != null && PlayerNotificationManager.this.customActionReceiver != null && PlayerNotificationManager.this.customActions.containsKey(action)) {
                PlayerNotificationManager.this.customActionReceiver.onCustomAction(player, action, intent);
            }
        }
    }

    private static final class MediaStyle extends NotificationCompat.Style {
        private final int[] actionsToShowInCompact;
        private final MediaSession.Token token;

        public MediaStyle(MediaSession.Token token, int[] actionsToShowInCompact) {
            this.token = token;
            this.actionsToShowInCompact = actionsToShowInCompact;
        }

        @Override // androidx.core.app.NotificationCompat.Style
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            Notification.MediaStyle style = new Notification.MediaStyle();
            style.setShowActionsInCompactView(this.actionsToShowInCompact);
            if (this.token != null) {
                style.setMediaSession(this.token);
            }
            builder.getBuilder().setStyle(style);
        }
    }
}
