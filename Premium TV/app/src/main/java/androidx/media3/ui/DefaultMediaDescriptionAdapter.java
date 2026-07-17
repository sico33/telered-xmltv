package androidx.media3.ui;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import androidx.media3.common.Player;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultMediaDescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {
    private final PendingIntent pendingIntent;

    @Override // androidx.media3.ui.PlayerNotificationManager.MediaDescriptionAdapter
    public /* synthetic */ CharSequence getCurrentSubText(Player player) {
        return PlayerNotificationManager.MediaDescriptionAdapter.CC.$default$getCurrentSubText(this, player);
    }

    public DefaultMediaDescriptionAdapter(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    @Override // androidx.media3.ui.PlayerNotificationManager.MediaDescriptionAdapter
    public CharSequence getCurrentContentTitle(Player player) {
        if (!player.isCommandAvailable(18)) {
            return "";
        }
        CharSequence displayTitle = player.getMediaMetadata().displayTitle;
        if (!TextUtils.isEmpty(displayTitle)) {
            return displayTitle;
        }
        CharSequence title = player.getMediaMetadata().title;
        return title != null ? title : "";
    }

    @Override // androidx.media3.ui.PlayerNotificationManager.MediaDescriptionAdapter
    public PendingIntent createCurrentContentIntent(Player player) {
        return this.pendingIntent;
    }

    @Override // androidx.media3.ui.PlayerNotificationManager.MediaDescriptionAdapter
    public CharSequence getCurrentContentText(Player player) {
        if (!player.isCommandAvailable(18)) {
            return null;
        }
        CharSequence artist = player.getMediaMetadata().artist;
        if (!TextUtils.isEmpty(artist)) {
            return artist;
        }
        return player.getMediaMetadata().albumArtist;
    }

    @Override // androidx.media3.ui.PlayerNotificationManager.MediaDescriptionAdapter
    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
        byte[] data;
        if (player.isCommandAvailable(18) && (data = player.getMediaMetadata().artworkData) != null) {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        return null;
    }
}
