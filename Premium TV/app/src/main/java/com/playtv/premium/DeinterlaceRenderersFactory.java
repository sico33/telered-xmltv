package com.playtv.premium;

import android.content.Context;
import android.media.MediaFormat;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.mediacodec.DefaultMediaCodecAdapterFactory;
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter;
import java.io.IOException;

/* JADX INFO: loaded from: classes2.dex */
final class DeinterlaceRenderersFactory extends DefaultRenderersFactory {
    private final Context context;

    DeinterlaceRenderersFactory(Context context) {
        super(context);
        this.context = context;
        setEnableDecoderFallback(true);
    }

    @Override // androidx.media3.exoplayer.DefaultRenderersFactory
    protected MediaCodecAdapter.Factory getCodecAdapterFactory() {
        return new MediaCodecAdapter.Factory(this) { // from class: com.playtv.premium.DeinterlaceRenderersFactory.1
            private static short[] h = {-15970, -15974, -15970, -15978, -6796, -6805, -6810, -6809, -6803, -6867, -10895, -10966, -10909, -10910, -10898, -10903, -10893, -10910, -10891, -10901, -10906, -10908, -10910, -13470, -13469, -13457, -13464, -13454, -13469, -13452, -13462, -13465, -13467, -13469, -13525, -13461, -13463, -13470, -13469};
            private final DefaultMediaCodecAdapterFactory delegate;
            final DeinterlaceRenderersFactory this$0;

            {
                this.this$0 = this;
                this.delegate = new DefaultMediaCodecAdapterFactory(this.this$0.context);
            }

            private static String h(int i, int i2, int i3) {
                char[] cArr = new char[i2 - i];
                for (int i4 = 0; i4 < i2 - i; i4++) {
                    cArr[i4] = (char) (h[i + i4] ^ i3);
                }
                return new String(cArr);
            }

            @Override // androidx.media3.exoplayer.mediacodec.MediaCodecAdapter.Factory
            public MediaCodecAdapter createAdapter(MediaCodecAdapter.Configuration configuration) throws IOException {
                MediaFormat mediaFormat = configuration.mediaFormat;
                String string = mediaFormat.getString(h(0, 4, -15885));
                if (string != null && string.startsWith(h(4, 10, -6910))) {
                    mediaFormat.setInteger(h(10, 23, -11001), 1);
                    mediaFormat.setInteger(h(23, 39, -13562), 1);
                }
                return this.delegate.createAdapter(configuration);
            }
        };
    }
}
