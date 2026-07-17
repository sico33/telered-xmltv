package com.bumptech.glide.util;

/* JADX INFO: loaded from: classes.dex */
public final class GlideSuppliers {

    public interface GlideSupplier<T> {
        T get();
    }

    private GlideSuppliers() {
    }

    public static <T> GlideSupplier<T> memorize(GlideSupplier<T> glideSupplier) {
        return new GlideSupplier<T>(glideSupplier) { // from class: com.bumptech.glide.util.GlideSuppliers.1
            private volatile T instance;
            final GlideSupplier val$supplier;

            {
                this.val$supplier = glideSupplier;
            }

            @Override // com.bumptech.glide.util.GlideSuppliers.GlideSupplier
            public T get() {
                if (this.instance == null) {
                    synchronized (this) {
                        if (this.instance == null) {
                            this.instance = (T) Preconditions.checkNotNull(this.val$supplier.get());
                        }
                    }
                }
                return this.instance;
            }
        };
    }
}
