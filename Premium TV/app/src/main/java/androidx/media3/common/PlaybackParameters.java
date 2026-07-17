package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class PlaybackParameters {
    public final float pitch;
    private final int scaledUsPerMs;
    public final float speed;
    public static final PlaybackParameters DEFAULT = new PlaybackParameters(1.0f);
    private static final String FIELD_SPEED = Util.intToStringMaxRadix(0);
    private static final String FIELD_PITCH = Util.intToStringMaxRadix(1);

    public PlaybackParameters(float speed) {
        this(speed, 1.0f);
    }

    public PlaybackParameters(float speed, float pitch) {
        Assertions.checkArgument(speed > 0.0f);
        Assertions.checkArgument(pitch > 0.0f);
        this.speed = speed;
        this.pitch = pitch;
        this.scaledUsPerMs = Math.round(1000.0f * speed);
    }

    public long getMediaTimeUsForPlayoutTimeMs(long timeMs) {
        return ((long) this.scaledUsPerMs) * timeMs;
    }

    public PlaybackParameters withSpeed(float speed) {
        return new PlaybackParameters(speed, this.pitch);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PlaybackParameters other = (PlaybackParameters) obj;
        if (this.speed == other.speed && this.pitch == other.pitch) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + Float.floatToRawIntBits(this.speed);
        return (result * 31) + Float.floatToRawIntBits(this.pitch);
    }

    public String toString() {
        return Util.formatInvariant("PlaybackParameters(speed=%.2f, pitch=%.2f)", Float.valueOf(this.speed), Float.valueOf(this.pitch));
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putFloat(FIELD_SPEED, this.speed);
        bundle.putFloat(FIELD_PITCH, this.pitch);
        return bundle;
    }

    public static PlaybackParameters fromBundle(Bundle bundle) {
        float speed = bundle.getFloat(FIELD_SPEED, 1.0f);
        float pitch = bundle.getFloat(FIELD_PITCH, 1.0f);
        return new PlaybackParameters(speed, pitch);
    }
}
