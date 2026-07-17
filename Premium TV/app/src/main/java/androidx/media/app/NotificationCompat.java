package androidx.media.app;

import android.app.Notification;
import android.app.Notification$DecoratedMediaCustomViewStyle;
import android.app.PendingIntent;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.RemoteViews;
import androidx.core.app.NotificationBuilderWithBuilderAccessor;
import androidx.media.R;

/* JADX INFO: loaded from: classes.dex */
public class NotificationCompat {
    private NotificationCompat() {
    }

    public static class MediaStyle extends androidx.core.app.NotificationCompat.Style {
        private static final int MAX_MEDIA_BUTTONS = 5;
        private static final int MAX_MEDIA_BUTTONS_IN_COMPACT = 3;
        PendingIntent mCancelButtonIntent;
        int mDeviceIcon;
        PendingIntent mDeviceIntent;
        CharSequence mDeviceName;
        boolean mShowCancelButton;
        MediaSessionCompat.Token mToken;
        int[] mActionsToShowInCompact = null;
        boolean mShowRemotePlaybackInfo = false;

        public static MediaSessionCompat.Token getMediaSession(Notification notification) {
            Object tokenInner;
            Bundle extras = androidx.core.app.NotificationCompat.getExtras(notification);
            if (extras != null && (tokenInner = extras.getParcelable(androidx.core.app.NotificationCompat.EXTRA_MEDIA_SESSION)) != null) {
                return MediaSessionCompat.Token.fromToken(tokenInner);
            }
            return null;
        }

        public MediaStyle() {
        }

        public MediaStyle(androidx.core.app.NotificationCompat.Builder builder) {
            setBuilder(builder);
        }

        public MediaStyle setShowActionsInCompactView(int... actions) {
            this.mActionsToShowInCompact = actions;
            return this;
        }

        public MediaStyle setMediaSession(MediaSessionCompat.Token token) {
            this.mToken = token;
            return this;
        }

        public MediaStyle setRemotePlaybackInfo(CharSequence deviceName, int iconResource, PendingIntent chipIntent) {
            this.mDeviceName = deviceName;
            this.mDeviceIcon = iconResource;
            this.mDeviceIntent = chipIntent;
            this.mShowRemotePlaybackInfo = true;
            return this;
        }

        public MediaStyle setShowCancelButton(boolean show) {
            return this;
        }

        public MediaStyle setCancelButtonIntent(PendingIntent pendingIntent) {
            this.mCancelButtonIntent = pendingIntent;
            return this;
        }

        @Override // androidx.core.app.NotificationCompat.Style
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 34) {
                Api21Impl.setMediaStyle(builder.getBuilder(), Api21Impl.fillInMediaStyle(Api34Impl.setRemotePlaybackInfo(Api21Impl.createMediaStyle(), this.mDeviceName, this.mDeviceIcon, this.mDeviceIntent, Boolean.valueOf(this.mShowRemotePlaybackInfo)), this.mActionsToShowInCompact, this.mToken));
            } else {
                Api21Impl.setMediaStyle(builder.getBuilder(), Api21Impl.fillInMediaStyle(Api21Impl.createMediaStyle(), this.mActionsToShowInCompact, this.mToken));
            }
        }

        @Override // androidx.core.app.NotificationCompat.Style
        public RemoteViews makeContentView(NotificationBuilderWithBuilderAccessor builder) {
            return null;
        }

        RemoteViews generateContentView() {
            int numActionsInCompact;
            RemoteViews view = applyStandardTemplate(false, getContentViewLayoutResource(), true);
            int numActions = this.mBuilder.mActions.size();
            if (this.mActionsToShowInCompact == null) {
                numActionsInCompact = 0;
            } else {
                numActionsInCompact = Math.min(this.mActionsToShowInCompact.length, 3);
            }
            view.removeAllViews(R.id.media_actions);
            if (numActionsInCompact > 0) {
                for (int i = 0; i < numActionsInCompact; i++) {
                    if (i >= numActions) {
                        throw new IllegalArgumentException(String.format("setShowActionsInCompactView: action %d out of bounds (max %d)", Integer.valueOf(i), Integer.valueOf(numActions - 1)));
                    }
                    androidx.core.app.NotificationCompat.Action action = this.mBuilder.mActions.get(this.mActionsToShowInCompact[i]);
                    RemoteViews button = generateMediaActionButton(action);
                    view.addView(R.id.media_actions, button);
                }
            }
            if (!this.mShowCancelButton) {
                view.setViewVisibility(R.id.end_padder, 0);
                view.setViewVisibility(R.id.cancel_action, 8);
            } else {
                view.setViewVisibility(R.id.end_padder, 8);
                view.setViewVisibility(R.id.cancel_action, 0);
                view.setOnClickPendingIntent(R.id.cancel_action, this.mCancelButtonIntent);
                view.setInt(R.id.cancel_action, "setAlpha", this.mBuilder.mContext.getResources().getInteger(R.integer.cancel_button_image_alpha));
            }
            return view;
        }

        private RemoteViews generateMediaActionButton(androidx.core.app.NotificationCompat.Action action) {
            boolean tombstone = action.getActionIntent() == null;
            RemoteViews button = new RemoteViews(this.mBuilder.mContext.getPackageName(), R.layout.notification_media_action);
            button.setImageViewResource(R.id.action0, action.getIcon());
            if (!tombstone) {
                button.setOnClickPendingIntent(R.id.action0, action.getActionIntent());
            }
            Api15Impl.setContentDescription(button, R.id.action0, action.getTitle());
            return button;
        }

        int getContentViewLayoutResource() {
            return R.layout.notification_template_media;
        }

        @Override // androidx.core.app.NotificationCompat.Style
        public RemoteViews makeBigContentView(NotificationBuilderWithBuilderAccessor builder) {
            return null;
        }

        RemoteViews generateBigContentView() {
            int actionCount = Math.min(this.mBuilder.mActions.size(), 5);
            RemoteViews big = applyStandardTemplate(false, getBigContentViewLayoutResource(actionCount), false);
            big.removeAllViews(R.id.media_actions);
            if (actionCount > 0) {
                for (int i = 0; i < actionCount; i++) {
                    RemoteViews button = generateMediaActionButton(this.mBuilder.mActions.get(i));
                    big.addView(R.id.media_actions, button);
                }
            }
            if (this.mShowCancelButton) {
                big.setViewVisibility(R.id.cancel_action, 0);
                big.setInt(R.id.cancel_action, "setAlpha", this.mBuilder.mContext.getResources().getInteger(R.integer.cancel_button_image_alpha));
                big.setOnClickPendingIntent(R.id.cancel_action, this.mCancelButtonIntent);
            } else {
                big.setViewVisibility(R.id.cancel_action, 8);
            }
            return big;
        }

        int getBigContentViewLayoutResource(int actionCount) {
            if (actionCount <= 3) {
                return R.layout.notification_template_big_media_narrow;
            }
            return R.layout.notification_template_big_media;
        }
    }

    public static class DecoratedMediaCustomViewStyle extends MediaStyle {
        @Override // androidx.media.app.NotificationCompat.MediaStyle, androidx.core.app.NotificationCompat.Style
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 34) {
                Api21Impl.setMediaStyle(builder.getBuilder(), Api21Impl.fillInMediaStyle(Api34Impl.setRemotePlaybackInfo(Api24Impl.createDecoratedMediaCustomViewStyle(), this.mDeviceName, this.mDeviceIcon, this.mDeviceIntent, Boolean.valueOf(this.mShowRemotePlaybackInfo)), this.mActionsToShowInCompact, this.mToken));
            } else if (Build.VERSION.SDK_INT >= 24) {
                Api21Impl.setMediaStyle(builder.getBuilder(), Api21Impl.fillInMediaStyle(Api24Impl.createDecoratedMediaCustomViewStyle(), this.mActionsToShowInCompact, this.mToken));
            } else {
                super.apply(builder);
            }
        }

        @Override // androidx.media.app.NotificationCompat.MediaStyle, androidx.core.app.NotificationCompat.Style
        public RemoteViews makeContentView(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                return null;
            }
            boolean createCustomContent = true;
            boolean hasContentView = this.mBuilder.getContentView() != null;
            if (!hasContentView && this.mBuilder.getBigContentView() == null) {
                createCustomContent = false;
            }
            if (!createCustomContent) {
                return null;
            }
            RemoteViews contentView = generateContentView();
            if (hasContentView) {
                buildIntoRemoteViews(contentView, this.mBuilder.getContentView());
            }
            setBackgroundColor(contentView);
            return contentView;
        }

        @Override // androidx.media.app.NotificationCompat.MediaStyle
        int getContentViewLayoutResource() {
            if (this.mBuilder.getContentView() != null) {
                return R.layout.notification_template_media_custom;
            }
            return super.getContentViewLayoutResource();
        }

        @Override // androidx.media.app.NotificationCompat.MediaStyle, androidx.core.app.NotificationCompat.Style
        public RemoteViews makeBigContentView(NotificationBuilderWithBuilderAccessor builder) {
            RemoteViews innerView;
            if (Build.VERSION.SDK_INT >= 24) {
                return null;
            }
            if (this.mBuilder.getBigContentView() != null) {
                innerView = this.mBuilder.getBigContentView();
            } else {
                innerView = this.mBuilder.getContentView();
            }
            if (innerView == null) {
                return null;
            }
            RemoteViews bigContentView = generateBigContentView();
            buildIntoRemoteViews(bigContentView, innerView);
            setBackgroundColor(bigContentView);
            return bigContentView;
        }

        @Override // androidx.media.app.NotificationCompat.MediaStyle
        int getBigContentViewLayoutResource(int actionCount) {
            if (actionCount <= 3) {
                return R.layout.notification_template_big_media_narrow_custom;
            }
            return R.layout.notification_template_big_media_custom;
        }

        @Override // androidx.core.app.NotificationCompat.Style
        public RemoteViews makeHeadsUpContentView(NotificationBuilderWithBuilderAccessor builder) {
            RemoteViews innerView;
            if (Build.VERSION.SDK_INT >= 24) {
                return null;
            }
            if (this.mBuilder.getHeadsUpContentView() != null) {
                innerView = this.mBuilder.getHeadsUpContentView();
            } else {
                innerView = this.mBuilder.getContentView();
            }
            if (innerView == null) {
                return null;
            }
            RemoteViews headsUpContentView = generateBigContentView();
            buildIntoRemoteViews(headsUpContentView, innerView);
            setBackgroundColor(headsUpContentView);
            return headsUpContentView;
        }

        private void setBackgroundColor(RemoteViews views) {
            int color;
            if (this.mBuilder.getColor() != 0) {
                color = this.mBuilder.getColor();
            } else {
                color = this.mBuilder.mContext.getResources().getColor(R.color.notification_material_background_media_default_color);
            }
            views.setInt(R.id.status_bar_latest_event_content, "setBackgroundColor", color);
        }
    }

    private static class Api15Impl {
        private Api15Impl() {
        }

        static void setContentDescription(RemoteViews remoteViews, int viewId, CharSequence contentDescription) {
            remoteViews.setContentDescription(viewId, contentDescription);
        }
    }

    private static class Api21Impl {
        private Api21Impl() {
        }

        static void setMediaStyle(Notification.Builder builder, Notification.MediaStyle style) {
            builder.setStyle(style);
        }

        static Notification.MediaStyle createMediaStyle() {
            return new Notification.MediaStyle();
        }

        static Notification.MediaStyle fillInMediaStyle(Notification.MediaStyle style, int[] actionsToShowInCompact, MediaSessionCompat.Token token) {
            if (actionsToShowInCompact != null) {
                setShowActionsInCompactView(style, actionsToShowInCompact);
            }
            if (token != null) {
                setMediaSession(style, (MediaSession.Token) token.getToken());
            }
            return style;
        }

        static void setShowActionsInCompactView(Notification.MediaStyle style, int... actions) {
            style.setShowActionsInCompactView(actions);
        }

        static void setMediaSession(Notification.MediaStyle style, MediaSession.Token token) {
            style.setMediaSession(token);
        }
    }

    private static class Api24Impl {
        private Api24Impl() {
        }

        static Notification.MediaStyle createDecoratedMediaCustomViewStyle() {
            return new Notification$DecoratedMediaCustomViewStyle();
        }
    }

    private static class Api34Impl {
        private Api34Impl() {
        }

        static Notification.MediaStyle setRemotePlaybackInfo(Notification.MediaStyle style, CharSequence deviceName, int iconResource, PendingIntent chipIntent, Boolean showRemotePlaybackInfo) {
            if (showRemotePlaybackInfo.booleanValue()) {
                style.setRemotePlaybackInfo(deviceName, iconResource, chipIntent);
            }
            return style;
        }
    }
}
