package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class DeviceInfo {
    public static final int PLAYBACK_TYPE_LOCAL = 0;
    public static final int PLAYBACK_TYPE_REMOTE = 1;
    public final int maxVolume;
    public final int minVolume;
    public final int playbackType;
    public final String routingControllerId;
    public static final DeviceInfo UNKNOWN = new Builder(0).build();
    private static final String FIELD_PLAYBACK_TYPE = Util.intToStringMaxRadix(0);
    private static final String FIELD_MIN_VOLUME = Util.intToStringMaxRadix(1);
    private static final String FIELD_MAX_VOLUME = Util.intToStringMaxRadix(2);
    private static final String FIELD_ROUTING_CONTROLLER_ID = Util.intToStringMaxRadix(3);

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlaybackType {
    }

    public static final class Builder {
        private int maxVolume;
        private int minVolume;
        private final int playbackType;
        private String routingControllerId;

        public Builder(int playbackType) {
            this.playbackType = playbackType;
        }

        public Builder setMinVolume(int minVolume) {
            this.minVolume = minVolume;
            return this;
        }

        public Builder setMaxVolume(int maxVolume) {
            this.maxVolume = maxVolume;
            return this;
        }

        public Builder setRoutingControllerId(String routingControllerId) {
            Assertions.checkArgument(this.playbackType != 0 || routingControllerId == null);
            this.routingControllerId = routingControllerId;
            return this;
        }

        public DeviceInfo build() {
            Assertions.checkArgument(this.minVolume <= this.maxVolume);
            return new DeviceInfo(this);
        }
    }

    @Deprecated
    public DeviceInfo(int playbackType, int minVolume, int maxVolume) {
        this(new Builder(playbackType).setMinVolume(minVolume).setMaxVolume(maxVolume));
    }

    private DeviceInfo(Builder builder) {
        this.playbackType = builder.playbackType;
        this.minVolume = builder.minVolume;
        this.maxVolume = builder.maxVolume;
        this.routingControllerId = builder.routingControllerId;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DeviceInfo)) {
            return false;
        }
        DeviceInfo other = (DeviceInfo) obj;
        return this.playbackType == other.playbackType && this.minVolume == other.minVolume && this.maxVolume == other.maxVolume && Util.areEqual(this.routingControllerId, other.routingControllerId);
    }

    public int hashCode() {
        int result = (17 * 31) + this.playbackType;
        return (((((result * 31) + this.minVolume) * 31) + this.maxVolume) * 31) + (this.routingControllerId == null ? 0 : this.routingControllerId.hashCode());
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (this.playbackType != 0) {
            bundle.putInt(FIELD_PLAYBACK_TYPE, this.playbackType);
        }
        if (this.minVolume != 0) {
            bundle.putInt(FIELD_MIN_VOLUME, this.minVolume);
        }
        if (this.maxVolume != 0) {
            bundle.putInt(FIELD_MAX_VOLUME, this.maxVolume);
        }
        if (this.routingControllerId != null) {
            bundle.putString(FIELD_ROUTING_CONTROLLER_ID, this.routingControllerId);
        }
        return bundle;
    }

    public static DeviceInfo fromBundle(Bundle bundle) {
        int playbackType = bundle.getInt(FIELD_PLAYBACK_TYPE, 0);
        int minVolume = bundle.getInt(FIELD_MIN_VOLUME, 0);
        int maxVolume = bundle.getInt(FIELD_MAX_VOLUME, 0);
        String routingControllerId = bundle.getString(FIELD_ROUTING_CONTROLLER_ID);
        return new Builder(playbackType).setMinVolume(minVolume).setMaxVolume(maxVolume).setRoutingControllerId(routingControllerId).build();
    }
}
