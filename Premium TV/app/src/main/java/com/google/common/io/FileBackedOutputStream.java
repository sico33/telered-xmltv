package com.google.common.io;

import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class FileBackedOutputStream extends OutputStream {

    @CheckForNull
    private File file;
    private final int fileThreshold;

    @CheckForNull
    private MemoryOutput memory;
    private OutputStream out;
    private final boolean resetOnFinalize;
    private final ByteSource source;

    private static class MemoryOutput extends ByteArrayOutputStream {
        private MemoryOutput() {
        }

        byte[] getBuffer() {
            return this.buf;
        }

        int getCount() {
            return this.count;
        }
    }

    public FileBackedOutputStream(int i) {
        this(i, false);
    }

    public FileBackedOutputStream(int i, boolean z) {
        Preconditions.checkArgument(i >= 0, "fileThreshold must be non-negative, but was %s", i);
        this.fileThreshold = i;
        this.resetOnFinalize = z;
        this.memory = new MemoryOutput();
        this.out = this.memory;
        if (z) {
            this.source = new ByteSource(this) { // from class: com.google.common.io.FileBackedOutputStream.1
                final FileBackedOutputStream this$0;

                {
                    this.this$0 = this;
                }

                protected void finalize() {
                    try {
                        this.this$0.reset();
                    } catch (Throwable th) {
                        th.printStackTrace(System.err);
                    }
                }

                @Override // com.google.common.io.ByteSource
                public InputStream openStream() throws IOException {
                    return this.this$0.openInputStream();
                }
            };
        } else {
            this.source = new ByteSource(this) { // from class: com.google.common.io.FileBackedOutputStream.2
                final FileBackedOutputStream this$0;

                {
                    this.this$0 = this;
                }

                @Override // com.google.common.io.ByteSource
                public InputStream openStream() throws IOException {
                    return this.this$0.openInputStream();
                }
            };
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public InputStream openInputStream() throws IOException {
        InputStream byteArrayInputStream;
        synchronized (this) {
            if (this.file != null) {
                byteArrayInputStream = new FileInputStream(this.file);
            } else {
                Objects.requireNonNull(this.memory);
                byteArrayInputStream = new ByteArrayInputStream(this.memory.getBuffer(), 0, this.memory.getCount());
            }
        }
        return byteArrayInputStream;
    }

    private void update(int i) throws IOException {
        if (this.memory == null || this.memory.getCount() + i <= this.fileThreshold) {
            return;
        }
        File fileCreateTempFile = TempFileCreator.INSTANCE.createTempFile("FileBackedOutputStream");
        if (this.resetOnFinalize) {
            fileCreateTempFile.deleteOnExit();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileCreateTempFile);
            fileOutputStream.write(this.memory.getBuffer(), 0, this.memory.getCount());
            fileOutputStream.flush();
            this.out = fileOutputStream;
            this.file = fileCreateTempFile;
            this.memory = null;
        } catch (IOException e) {
            fileCreateTempFile.delete();
            throw e;
        }
    }

    public ByteSource asByteSource() {
        return this.source;
    }

    @Override // java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        synchronized (this) {
            this.out.close();
        }
    }

    @Override // java.io.OutputStream, java.io.Flushable
    public void flush() throws IOException {
        synchronized (this) {
            this.out.flush();
        }
    }

    @CheckForNull
    File getFile() {
        File file;
        synchronized (this) {
            file = this.file;
        }
        return file;
    }

    public void reset() throws IOException {
        synchronized (this) {
            try {
                close();
                if (this.memory == null) {
                    this.memory = new MemoryOutput();
                } else {
                    this.memory.reset();
                }
                this.out = this.memory;
                if (this.file != null) {
                    File file = this.file;
                    this.file = null;
                    if (!file.delete()) {
                        throw new IOException("Could not delete: " + file);
                    }
                }
            } catch (Throwable th) {
                if (this.memory == null) {
                    this.memory = new MemoryOutput();
                } else {
                    this.memory.reset();
                }
                this.out = this.memory;
                if (this.file != null) {
                    File file2 = this.file;
                    this.file = null;
                    if (!file2.delete()) {
                        throw new IOException("Could not delete: " + file2);
                    }
                }
                throw th;
            }
        }
    }

    @Override // java.io.OutputStream
    public void write(int i) throws IOException {
        synchronized (this) {
            update(1);
            this.out.write(i);
        }
    }

    @Override // java.io.OutputStream
    public void write(byte[] bArr) throws IOException {
        synchronized (this) {
            write(bArr, 0, bArr.length);
        }
    }

    @Override // java.io.OutputStream
    public void write(byte[] bArr, int i, int i2) throws IOException {
        synchronized (this) {
            update(i2);
            this.out.write(bArr, i, i2);
        }
    }
}
