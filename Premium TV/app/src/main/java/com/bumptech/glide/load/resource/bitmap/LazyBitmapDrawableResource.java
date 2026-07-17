package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;

/* JADX INFO: loaded from: classes.dex */
public final class LazyBitmapDrawableResource implements Resource<BitmapDrawable>, Initializable {
    private final Resource<Bitmap> bitmapResource;
    private final Resources resources;

    private LazyBitmapDrawableResource(Resources resources, Resource<Bitmap> resource) {
        this.resources = (Resources) Preconditions.checkNotNull(resources);
        this.bitmapResource = (Resource) Preconditions.checkNotNull(resource);
    }

    public static Resource<BitmapDrawable> obtain(Resources resources, Resource<Bitmap> resource) {
        if (resource == null) {
            return null;
        }
        return new LazyBitmapDrawableResource(resources, resource);
    }

    @Deprecated
    public static LazyBitmapDrawableResource obtain(Context context, Bitmap bitmap) {
        return (LazyBitmapDrawableResource) obtain(context.getResources(), BitmapResource.obtain(bitmap, Glide.get(context).getBitmapPool()));
    }

    @Deprecated
    public static LazyBitmapDrawableResource obtain(Resources resources, BitmapPool bitmapPool, Bitmap bitmap) {
        return (LazyBitmapDrawableResource) obtain(resources, BitmapResource.obtain(bitmap, bitmapPool));
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.bumptech.glide.load.engine.Resource
    public BitmapDrawable get() {
        return new BitmapDrawable(this.resources, this.bitmapResource.get());
    }

    @Override // com.bumptech.glide.load.engine.Resource
    public Class<BitmapDrawable> getResourceClass() {
        return BitmapDrawable.class;
    }

    @Override // com.bumptech.glide.load.engine.Resource
    public int getSize() {
        return this.bitmapResource.getSize();
    }

    @Override // com.bumptech.glide.load.engine.Initializable
    public void initialize() {
        if (this.bitmapResource instanceof Initializable) {
            ((Initializable) this.bitmapResource).initialize();
        }
    }

    @Override // com.bumptech.glide.load.engine.Resource
    public void recycle() {
        this.bitmapResource.recycle();
    }
}
