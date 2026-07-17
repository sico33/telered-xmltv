package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class AudioAttributes {
    public final int allowedCapturePolicy;
    private AudioAttributesV21 audioAttributesV21;
    public final int contentType;
    public final int flags;
    public final int spatializationBehavior;
    public final int usage;
    public static final AudioAttributes DEFAULT = new Builder().build();
    private static final String FIELD_CONTENT_TYPE = Util.intToStringMaxRadix(0);
    private static final String FIELD_FLAGS = Util.intToStringMaxRadix(1);
    private static final String FIELD_USAGE = Util.intToStringMaxRadix(2);
    private static final String FIELD_ALLOWED_CAPTURE_POLICY = Util.intToStringMaxRadix(3);
    private static final String FIELD_SPATIALIZATION_BEHAVIOR = Util.intToStringMaxRadix(4);

    public static final class AudioAttributesV21 {
        public final android.media.AudioAttributes audioAttributes;

        private AudioAttributesV21(AudioAttributes audioAttributes) {
            android.media.AudioAttributes.Builder builder = new android.media.AudioAttributes.Builder().setContentType(audioAttributes.contentType).setFlags(audioAttributes.flags).setUsage(audioAttributes.usage);
            if (Util.SDK_INT >= 29) {
                Api29.setAllowedCapturePolicy(builder, audioAttributes.allowedCapturePolicy);
            }
            if (Util.SDK_INT >= 32) {
                Api32.setSpatializationBehavior(builder, audioAttributes.spatializationBehavior);
            }
            this.audioAttributes = builder.build();
        }
    }

    public static final class Builder {
        private int contentType = 0;
        private int flags = 0;
        private int usage = 1;
        private int allowedCapturePolicy = 1;
        private int spatializationBehavior = 0;

        public Builder setContentType(int contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder setFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public Builder setUsage(int usage) {
            this.usage = usage;
            return this;
        }

        public Builder setAllowedCapturePolicy(int allowedCapturePolicy) {
            this.allowedCapturePolicy = allowedCapturePolicy;
            return this;
        }

        public Builder setSpatializationBehavior(int spatializationBehavior) {
            this.spatializationBehavior = spatializationBehavior;
            return this;
        }

        public AudioAttributes build() {
            return new AudioAttributes(this.contentType, this.flags, this.usage, this.allowedCapturePolicy, this.spatializationBehavior);
        }
    }

    private AudioAttributes(int contentType, int flags, int usage, int allowedCapturePolicy, int spatializationBehavior) {
        this.contentType = contentType;
        this.flags = flags;
        this.usage = usage;
        this.allowedCapturePolicy = allowedCapturePolicy;
        this.spatializationBehavior = spatializationBehavior;
    }

    public AudioAttributesV21 getAudioAttributesV21() {
        if (this.audioAttributesV21 == null) {
            this.audioAttributesV21 = new AudioAttributesV21();
        }
        return this.audioAttributesV21;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AudioAttributes other = (AudioAttributes) obj;
        if (this.contentType == other.contentType && this.flags == other.flags && this.usage == other.usage && this.allowedCapturePolicy == other.allowedCapturePolicy && this.spatializationBehavior == other.spatializationBehavior) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + this.contentType;
        return (((((((result * 31) + this.flags) * 31) + this.usage) * 31) + this.allowedCapturePolicy) * 31) + this.spatializationBehavior;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(FIELD_CONTENT_TYPE, this.contentType);
        bundle.putInt(FIELD_FLAGS, this.flags);
        bundle.putInt(FIELD_USAGE, this.usage);
        bundle.putInt(FIELD_ALLOWED_CAPTURE_POLICY, this.allowedCapturePolicy);
        bundle.putInt(FIELD_SPATIALIZATION_BEHAVIOR, this.spatializationBehavior);
        return bundle;
    }

    public static AudioAttributes fromBundle(Bundle bundle) {
        Builder builder = new Builder();
        if (bundle.containsKey(FIELD_CONTENT_TYPE)) {
            builder.setContentType(bundle.getInt(FIELD_CONTENT_TYPE));
        }
        if (bundle.containsKey(FIELD_FLAGS)) {
            builder.setFlags(bundle.getInt(FIELD_FLAGS));
        }
        if (bundle.containsKey(FIELD_USAGE)) {
            builder.setUsage(bundle.getInt(FIELD_USAGE));
        }
        if (bundle.containsKey(FIELD_ALLOWED_CAPTURE_POLICY)) {
            builder.setAllowedCapturePolicy(bundle.getInt(FIELD_ALLOWED_CAPTURE_POLICY));
        }
        if (bundle.containsKey(FIELD_SPATIALIZATION_BEHAVIOR)) {
            builder.setSpatializationBehavior(bundle.getInt(FIELD_SPATIALIZATION_BEHAVIOR));
        }
        return builder.build();
    }

    private static final class Api29 {
        private Api29() {
        }

        public static void setAllowedCapturePolicy(android.media.AudioAttributes.Builder builder, int allowedCapturePolicy) {
            builder.setAllowedCapturePolicy(allowedCapturePolicy);
        }
    }

    private static final class Api32 {
        private Api32() {
        }

        public static void setSpatializationBehavior(android.media.AudioAttributes.Builder builder, int spatializationBehavior) {
            builder.setSpatializationBehavior(spatializationBehavior);
        }
    }
}
