package com.bumptech.glide.request.transition;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/* JADX INFO: loaded from: classes.dex */
public class BitmapTransitionFactory extends BitmapContainerTransitionFactory<Bitmap> {
    public BitmapTransitionFactory(TransitionFactory<Drawable> transitionFactory) {
        super(transitionFactory);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.bumptech.glide.request.transition.BitmapContainerTransitionFactory
    public Bitmap getBitmap(Bitmap bitmap) {
        return bitmap;
    }
}
