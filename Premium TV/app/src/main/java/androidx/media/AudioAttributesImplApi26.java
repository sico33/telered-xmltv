package androidx.media;

import android.media.AudioAttributes;

/* JADX INFO: loaded from: classes.dex */
public class AudioAttributesImplApi26 extends AudioAttributesImplApi21 {
    public AudioAttributesImplApi26() {
    }

    AudioAttributesImplApi26(AudioAttributes audioAttributes) {
        super(audioAttributes, -1);
    }

    @Override // androidx.media.AudioAttributesImplApi21, androidx.media.AudioAttributesImpl
    public int getVolumeControlStream() {
        return this.mAudioAttributes.getVolumeControlStream();
    }

    static class Builder extends AudioAttributesImplApi21.Builder {
        Builder() {
        }

        Builder(Object aa) {
            super(aa);
        }

        @Override // androidx.media.AudioAttributesImplApi21.Builder, androidx.media.AudioAttributesImpl.Builder
        public AudioAttributesImpl build() {
            return new AudioAttributesImplApi26(this.mFwkBuilder.build());
        }

        @Override // androidx.media.AudioAttributesImplApi21.Builder, androidx.media.AudioAttributesImpl.Builder
        public Builder setUsage(int usage) {
            this.mFwkBuilder.setUsage(usage);
            return this;
        }
    }
}
