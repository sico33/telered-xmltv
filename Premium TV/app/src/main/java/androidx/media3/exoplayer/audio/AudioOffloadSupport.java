package androidx.media3.exoplayer.audio;

/* JADX INFO: loaded from: classes.dex */
public final class AudioOffloadSupport {
    public static final AudioOffloadSupport DEFAULT_UNSUPPORTED = new Builder().build();
    public final boolean isFormatSupported;
    public final boolean isGaplessSupported;
    public final boolean isSpeedChangeSupported;

    public static final class Builder {
        private boolean isFormatSupported;
        private boolean isGaplessSupported;
        private boolean isSpeedChangeSupported;

        public Builder() {
        }

        public Builder(AudioOffloadSupport audioOffloadSupport) {
            this.isFormatSupported = audioOffloadSupport.isFormatSupported;
            this.isGaplessSupported = audioOffloadSupport.isGaplessSupported;
            this.isSpeedChangeSupported = audioOffloadSupport.isSpeedChangeSupported;
        }

        public Builder setIsFormatSupported(boolean isFormatSupported) {
            this.isFormatSupported = isFormatSupported;
            return this;
        }

        public Builder setIsGaplessSupported(boolean isGaplessSupported) {
            this.isGaplessSupported = isGaplessSupported;
            return this;
        }

        public Builder setIsSpeedChangeSupported(boolean isSpeedChangeSupported) {
            this.isSpeedChangeSupported = isSpeedChangeSupported;
            return this;
        }

        public AudioOffloadSupport build() {
            if (!this.isFormatSupported && (this.isGaplessSupported || this.isSpeedChangeSupported)) {
                throw new IllegalStateException("Secondary offload attribute fields are true but primary isFormatSupported is false");
            }
            return new AudioOffloadSupport(this);
        }
    }

    private AudioOffloadSupport(Builder builder) {
        this.isFormatSupported = builder.isFormatSupported;
        this.isGaplessSupported = builder.isGaplessSupported;
        this.isSpeedChangeSupported = builder.isSpeedChangeSupported;
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AudioOffloadSupport other = (AudioOffloadSupport) obj;
        if (this.isFormatSupported == other.isFormatSupported && this.isGaplessSupported == other.isGaplessSupported && this.isSpeedChangeSupported == other.isSpeedChangeSupported) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return ((this.isFormatSupported ? 1 : 0) << 2) + ((this.isGaplessSupported ? 1 : 0) << 1) + (this.isSpeedChangeSupported ? 1 : 0);
    }
}
