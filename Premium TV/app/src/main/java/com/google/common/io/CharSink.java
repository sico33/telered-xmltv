package com.google.common.io;

import com.google.common.base.Preconditions;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class CharSink {
    protected CharSink() {
    }

    public Writer openBufferedStream() throws IOException {
        Writer writerOpenStream = openStream();
        return writerOpenStream instanceof BufferedWriter ? (BufferedWriter) writerOpenStream : new BufferedWriter(writerOpenStream);
    }

    public abstract Writer openStream() throws IOException;

    public void write(CharSequence charSequence) throws Throwable {
        Preconditions.checkNotNull(charSequence);
        Closer closerCreate = Closer.create();
        try {
            Writer writer = (Writer) closerCreate.register(openStream());
            writer.append(charSequence);
            writer.flush();
            closerCreate.close();
        } catch (Throwable th) {
            try {
                throw closerCreate.rethrow(th);
            } catch (Throwable th2) {
                closerCreate.close();
                throw th2;
            }
        }
    }

    public long writeFrom(Readable readable) throws Throwable {
        Preconditions.checkNotNull(readable);
        Closer closerCreate = Closer.create();
        try {
            Writer writer = (Writer) closerCreate.register(openStream());
            long jCopy = CharStreams.copy(readable, writer);
            writer.flush();
            closerCreate.close();
            return jCopy;
        } catch (Throwable th) {
            try {
                throw closerCreate.rethrow(th);
            } catch (Throwable th2) {
                closerCreate.close();
                throw th2;
            }
        }
    }

    public void writeLines(Iterable<? extends CharSequence> iterable) throws Throwable {
        writeLines(iterable, "\n");
    }

    public void writeLines(Iterable<? extends CharSequence> iterable, String str) throws Throwable {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkNotNull(str);
        Closer closerCreate = Closer.create();
        try {
            Writer writer = (Writer) closerCreate.register(openBufferedStream());
            Iterator<? extends CharSequence> it = iterable.iterator();
            while (it.hasNext()) {
                writer.append(it.next()).append((CharSequence) str);
            }
            writer.flush();
            closerCreate.close();
        } catch (Throwable th) {
            try {
                throw closerCreate.rethrow(th);
            } catch (Throwable th2) {
                closerCreate.close();
                throw th2;
            }
        }
    }
}
