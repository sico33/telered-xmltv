package com.bumptech.glide.load.model;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.signature.ObjectKey;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public class ByteArrayLoader<Data> implements ModelLoader<byte[], Data> {
    private final Converter<Data> converter;

    public static class ByteBufferFactory implements ModelLoaderFactory<byte[], ByteBuffer> {
        @Override // com.bumptech.glide.load.model.ModelLoaderFactory
        public ModelLoader<byte[], ByteBuffer> build(MultiModelLoaderFactory multiModelLoaderFactory) {
            return new ByteArrayLoader(new Converter<ByteBuffer>(this) { // from class: com.bumptech.glide.load.model.ByteArrayLoader.ByteBufferFactory.1
                final ByteBufferFactory this$0;

                {
                    this.this$0 = this;
                }

                @Override // com.bumptech.glide.load.model.ByteArrayLoader.Converter
                public ByteBuffer convert(byte[] bArr) {
                    return ByteBuffer.wrap(bArr);
                }

                @Override // com.bumptech.glide.load.model.ByteArrayLoader.Converter
                public Class<ByteBuffer> getDataClass() {
                    return ByteBuffer.class;
                }
            });
        }

        @Override // com.bumptech.glide.load.model.ModelLoaderFactory
        public void teardown() {
        }
    }

    public interface Converter<Data> {
        Data convert(byte[] bArr);

        Class<Data> getDataClass();
    }

    private static class Fetcher<Data> implements DataFetcher<Data> {
        private final Converter<Data> converter;
        private final byte[] model;

        Fetcher(byte[] bArr, Converter<Data> converter) {
            this.model = bArr;
            this.converter = converter;
        }

        @Override // com.bumptech.glide.load.data.DataFetcher
        public void cancel() {
        }

        @Override // com.bumptech.glide.load.data.DataFetcher
        public void cleanup() {
        }

        @Override // com.bumptech.glide.load.data.DataFetcher
        public Class<Data> getDataClass() {
            return this.converter.getDataClass();
        }

        @Override // com.bumptech.glide.load.data.DataFetcher
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }

        @Override // com.bumptech.glide.load.data.DataFetcher
        public void loadData(Priority priority, DataFetcher.DataCallback<? super Data> dataCallback) {
            dataCallback.onDataReady(this.converter.convert(this.model));
        }
    }

    public static class StreamFactory implements ModelLoaderFactory<byte[], InputStream> {
        @Override // com.bumptech.glide.load.model.ModelLoaderFactory
        public ModelLoader<byte[], InputStream> build(MultiModelLoaderFactory multiModelLoaderFactory) {
            return new ByteArrayLoader(new Converter<InputStream>(this) { // from class: com.bumptech.glide.load.model.ByteArrayLoader.StreamFactory.1
                final StreamFactory this$0;

                {
                    this.this$0 = this;
                }

                /* JADX WARN: Can't rename method to resolve collision */
                @Override // com.bumptech.glide.load.model.ByteArrayLoader.Converter
                public InputStream convert(byte[] bArr) {
                    return new ByteArrayInputStream(bArr);
                }

                @Override // com.bumptech.glide.load.model.ByteArrayLoader.Converter
                public Class<InputStream> getDataClass() {
                    return InputStream.class;
                }
            });
        }

        @Override // com.bumptech.glide.load.model.ModelLoaderFactory
        public void teardown() {
        }
    }

    public ByteArrayLoader(Converter<Data> converter) {
        this.converter = converter;
    }

    @Override // com.bumptech.glide.load.model.ModelLoader
    public ModelLoader.LoadData<Data> buildLoadData(byte[] bArr, int i, int i2, Options options) {
        return new ModelLoader.LoadData<>(new ObjectKey(bArr), new Fetcher(bArr, this.converter));
    }

    @Override // com.bumptech.glide.load.model.ModelLoader
    public boolean handles(byte[] bArr) {
        return true;
    }
}
