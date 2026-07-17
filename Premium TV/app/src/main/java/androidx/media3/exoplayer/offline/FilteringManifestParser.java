package androidx.media3.exoplayer.offline;

import android.net.Uri;
import androidx.media3.common.StreamKey;
import androidx.media3.exoplayer.offline.FilterableManifest;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class FilteringManifestParser<T extends FilterableManifest<T>> implements ParsingLoadable.Parser<T> {
    private final ParsingLoadable.Parser<? extends T> parser;
    private final List<StreamKey> streamKeys;

    public FilteringManifestParser(ParsingLoadable.Parser<? extends T> parser, List<StreamKey> streamKeys) {
        this.parser = parser;
        this.streamKeys = streamKeys;
    }

    @Override // androidx.media3.exoplayer.upstream.ParsingLoadable.Parser
    public T parse(Uri uri, InputStream inputStream) throws IOException {
        T manifest = this.parser.parse(uri, inputStream);
        return (this.streamKeys == null || this.streamKeys.isEmpty()) ? manifest : (T) manifest.copy(this.streamKeys);
    }
}
