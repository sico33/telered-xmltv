package androidx.media3.common.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class NotificationUtil {
    public static final int IMPORTANCE_DEFAULT = 3;
    public static final int IMPORTANCE_HIGH = 4;
    public static final int IMPORTANCE_LOW = 2;
    public static final int IMPORTANCE_MIN = 1;
    public static final int IMPORTANCE_NONE = 0;
    public static final int IMPORTANCE_UNSPECIFIED = -1000;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Importance {
    }

    public static void createNotificationChannel(Context context, String id, int nameResourceId, int descriptionResourceId, int importance) {
        if (Util.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) Assertions.checkNotNull((NotificationManager) context.getSystemService("notification"));
            NotificationChannel channel = new NotificationChannel(id, context.getString(nameResourceId), importance);
            if (descriptionResourceId != 0) {
                channel.setDescription(context.getString(descriptionResourceId));
            }
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void setNotification(Context context, int id, Notification notification) {
        NotificationManager notificationManager = (NotificationManager) Assertions.checkNotNull((NotificationManager) context.getSystemService("notification"));
        if (notification != null) {
            notificationManager.notify(id, notification);
        } else {
            notificationManager.cancel(id);
        }
    }

    private NotificationUtil() {
    }
}
