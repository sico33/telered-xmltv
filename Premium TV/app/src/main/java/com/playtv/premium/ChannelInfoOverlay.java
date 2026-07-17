package com.playtv.premium;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.media3.exoplayer.ExoPlayer;
import com.bumptech.glide.Glide;

/* JADX INFO: loaded from: classes2.dex */
final class ChannelInfoOverlay extends LinearLayout {
    private static short[] y = {10165, 10158, 10158, 10150, 10150, 10150, 10150, 10150, 10150};
    private final Handler handler;
    private final Runnable hideRunnable;
    private final ImageView logoImage;
    private final TextView nameText;
    private final TextView numberText;

    ChannelInfoOverlay(Context context) {
        super(context);
        this.handler = new Handler(Looper.getMainLooper());
        this.hideRunnable = new Runnable(this) { // from class: com.playtv.premium.ChannelInfoOverlay$$ExternalSyntheticLambda0
            public final ChannelInfoOverlay f$0;

            {
                this.f$0 = this;
            }

            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m211lambda$new$0$complaytvpremiumChannelInfoOverlay();
            }
        };
        setOrientation(0);
        setGravity(16);
        setBackgroundColor(Color.parseColor(y(0, 9, 10134)));
        setPadding(24, 12, 24, 12);
        setVisibility(8);
        this.logoImage = new ImageView(context);
        addView(this.logoImage, new LinearLayout.LayoutParams(80, 80));
        this.numberText = new TextView(context);
        this.numberText.setTextColor(-1);
        this.numberText.setTextSize(20.0f);
        this.numberText.setPadding(16, 0, 0, 0);
        addView(this.numberText, new LinearLayout.LayoutParams(-2, -2));
        this.nameText = new TextView(context);
        this.nameText.setTextColor(-1);
        this.nameText.setTextSize(20.0f);
        this.nameText.setPadding(12, 0, 0, 0);
        addView(this.nameText, new LinearLayout.LayoutParams(-2, -2));
    }

    private static String y(int i, int i2, int i3) {
        char[] cArr = new char[i2 - i];
        for (int i4 = 0; i4 < i2 - i; i4++) {
            cArr[i4] = (char) (y[i + i4] ^ i3);
        }
        return new String(cArr);
    }

    /* JADX INFO: renamed from: lambda$new$0$com-playtv-premium-ChannelInfoOverlay, reason: not valid java name */
    /* synthetic */ void m211lambda$new$0$complaytvpremiumChannelInfoOverlay() {
        setVisibility(8);
    }

    void show(Channel channel) {
        this.numberText.setText(String.valueOf(channel.globalIndex));
        this.nameText.setText(channel.name);
        if (channel.icon == null || channel.icon.isEmpty()) {
            this.logoImage.setVisibility(8);
        } else {
            Glide.with(getContext()).load(channel.icon).into(this.logoImage);
            this.logoImage.setVisibility(0);
        }
        setVisibility(0);
        this.handler.removeCallbacks(this.hideRunnable);
        this.handler.postDelayed(this.hideRunnable, ExoPlayer.DEFAULT_DETACH_SURFACE_TIMEOUT_MS);
    }
}
