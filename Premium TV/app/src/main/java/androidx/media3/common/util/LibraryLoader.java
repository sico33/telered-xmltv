package androidx.media3.common.util;

import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public abstract class LibraryLoader {
    private static final String TAG = "LibraryLoader";
    private boolean isAvailable;
    private boolean loadAttempted;
    private String[] nativeLibraries;

    protected abstract void loadLibrary(String str);

    public LibraryLoader(String... libraries) {
        this.nativeLibraries = libraries;
    }

    public synchronized void setLibraries(String... libraries) {
        Assertions.checkState(!this.loadAttempted, "Cannot set libraries after loading");
        this.nativeLibraries = libraries;
    }

    public synchronized boolean isAvailable() {
        if (this.loadAttempted) {
            return this.isAvailable;
        }
        this.loadAttempted = true;
        try {
            for (String lib : this.nativeLibraries) {
                loadLibrary(lib);
            }
            this.isAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to load " + Arrays.toString(this.nativeLibraries));
        }
        return this.isAvailable;
    }
}
