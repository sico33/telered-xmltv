package com.bumptech.glide.load.model.stream;

import androidx.media3.exoplayer.DefaultLoadControl;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.HttpUrlFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import java.io.InputStream;

/* JADX INFO: loaded from: classes.dex */
public class HttpGlideUrlLoader implements ModelLoader<GlideUrl, InputStream> {
    public static final Option<Integer> TIMEOUT = Option.memory("com.bumptech.glide.load.model.stream.HttpGlideUrlLoader.Timeout", Integer.valueOf(DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS));
    private final ModelCache<GlideUrl, GlideUrl> modelCache;

    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
        private final ModelCache<GlideUrl, GlideUrl> modelCache = new ModelCache<>(500);

        @Override // com.bumptech.glide.load.model.ModelLoaderFactory
        public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory multiModelLoaderFactory) {
            return new HttpGlideUrlLoader(this.modelCache);
        }

        @Override // com.bumptech.glide.load.model.ModelLoaderFactory
        public void teardown() {
        }
    }

    public HttpGlideUrlLoader() {
        this(null);
    }

    public HttpGlideUrlLoader(ModelCache<GlideUrl, GlideUrl> modelCache) {
        this.modelCache = modelCache;
    }

    @Override // com.bumptech.glide.load.model.ModelLoader
    public ModelLoader.LoadData<InputStream> buildLoadData(GlideUrl glideUrl, int i, int i2, Options options) {
        if (this.modelCache != null) {
            GlideUrl glideUrl2 = this.modelCache.get(glideUrl, 0, 0);
            if (glideUrl2 == null) {
                this.modelCache.put(glideUrl, 0, 0, glideUrl);
            } else {
                glideUrl = glideUrl2;
            }
        }
        return new ModelLoader.LoadData<>(glideUrl, new HttpUrlFetcher(glideUrl, ((Integer) options.get(TIMEOUT)).intValue()));
    }

    @Override // com.bumptech.glide.load.model.ModelLoader
    public boolean handles(GlideUrl glideUrl) {
        return true;
    }
}
