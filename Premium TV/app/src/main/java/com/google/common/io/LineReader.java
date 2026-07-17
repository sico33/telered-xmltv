package com.google.common.io;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class LineReader {
    private final Readable readable;

    @CheckForNull
    private final Reader reader;
    private final CharBuffer cbuf = CharStreams.createBuffer();
    private final char[] buf = this.cbuf.array();
    private final Queue<String> lines = new ArrayDeque();
    private final LineBuffer lineBuf = new LineBuffer(this) { // from class: com.google.common.io.LineReader.1
        final LineReader this$0;

        {
            this.this$0 = this;
        }

        @Override // com.google.common.io.LineBuffer
        protected void handleLine(String str, String str2) {
            this.this$0.lines.add(str);
        }
    };

    public LineReader(Readable readable) {
        this.readable = (Readable) Preconditions.checkNotNull(readable);
        this.reader = readable instanceof Reader ? (Reader) readable : null;
    }

    @CheckForNull
    public String readLine() throws IOException {
        while (this.lines.peek() == null) {
            Java8Compatibility.clear(this.cbuf);
            int i = this.reader != null ? this.reader.read(this.buf, 0, this.buf.length) : this.readable.read(this.cbuf);
            LineBuffer lineBuffer = this.lineBuf;
            if (i == -1) {
                lineBuffer.finish();
                break;
            }
            lineBuffer.add(this.buf, 0, i);
        }
        return this.lines.poll();
    }
}
