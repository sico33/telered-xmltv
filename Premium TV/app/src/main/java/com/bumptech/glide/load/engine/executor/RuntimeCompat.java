package com.bumptech.glide.load.engine.executor;

import android.os.StrictMode;
import android.util.Log;
import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class RuntimeCompat {
    private static final String CPU_LOCATION = "/sys/devices/system/cpu/";
    private static final String CPU_NAME_REGEX = "cpu[0-9]+";
    private static final String TAG = "GlideRuntimeCompat";

    private RuntimeCompat() {
    }

    static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    private static int getCoreCountPre17() {
        File[] fileArrListFiles = null;
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
        try {
            fileArrListFiles = new File(CPU_LOCATION).listFiles(new FilenameFilter(Pattern.compile(CPU_NAME_REGEX)) { // from class: com.bumptech.glide.load.engine.executor.RuntimeCompat.1
                final Pattern val$cpuNamePattern;

                {
                    this.val$cpuNamePattern = pattern;
                }

                @Override // java.io.FilenameFilter
                public boolean accept(File file, String str) {
                    return this.val$cpuNamePattern.matcher(str).matches();
                }
            });
        } catch (Throwable th) {
            try {
                if (Log.isLoggable(TAG, 6)) {
                    Log.e(TAG, "Failed to calculate accurate cpu count", th);
                }
            } catch (Throwable th2) {
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                throw th2;
            }
        }
        StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
        return Math.max(1, fileArrListFiles != null ? fileArrListFiles.length : 0);
    }
}
