package androidx.media3.exoplayer.upstream;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public interface LoaderErrorThrower {
    void maybeThrowError() throws IOException;

    void maybeThrowError(int i) throws IOException;

    public static final class Placeholder implements LoaderErrorThrower {
        @Override // androidx.media3.exoplayer.upstream.LoaderErrorThrower
        public void maybeThrowError() {
        }

        @Override // androidx.media3.exoplayer.upstream.LoaderErrorThrower
        public void maybeThrowError(int minRetryCount) {
        }
    }
}
