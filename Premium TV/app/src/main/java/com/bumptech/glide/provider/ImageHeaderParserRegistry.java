package com.bumptech.glide.provider;

import com.bumptech.glide.load.ImageHeaderParser;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class ImageHeaderParserRegistry {
    private final List<ImageHeaderParser> parsers = new ArrayList();

    public void add(ImageHeaderParser imageHeaderParser) {
        synchronized (this) {
            this.parsers.add(imageHeaderParser);
        }
    }

    public List<ImageHeaderParser> getParsers() {
        List<ImageHeaderParser> list;
        synchronized (this) {
            list = this.parsers;
        }
        return list;
    }
}
