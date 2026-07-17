package com.google.common.util.concurrent;

import java.util.logging.Logger;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class LazyLogger {
    private volatile Logger logger;
    private final String loggerName;

    LazyLogger(Class<?> cls) {
        this.loggerName = cls.getName();
    }

    Logger get() {
        Logger logger = this.logger;
        if (logger == null) {
            synchronized (this) {
                logger = this.logger;
                if (logger == null) {
                    logger = Logger.getLogger(this.loggerName);
                    this.logger = logger;
                }
            }
        }
        return logger;
    }
}
