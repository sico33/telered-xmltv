package com.google.common.util.concurrent;

import com.google.common.math.LongMath;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class SmoothRateLimiter extends RateLimiter {
    double maxPermits;
    private long nextFreeTicketMicros;
    double stableIntervalMicros;
    double storedPermits;

    static final class SmoothBursty extends SmoothRateLimiter {
        final double maxBurstSeconds;

        SmoothBursty(RateLimiter.SleepingStopwatch sleepingStopwatch, double d) {
            super(sleepingStopwatch);
            this.maxBurstSeconds = d;
        }

        @Override // com.google.common.util.concurrent.SmoothRateLimiter
        double coolDownIntervalMicros() {
            return this.stableIntervalMicros;
        }

        @Override // com.google.common.util.concurrent.SmoothRateLimiter
        void doSetRate(double d, double d2) {
            double d3 = this.maxPermits;
            this.maxPermits = this.maxBurstSeconds * d;
            if (d3 == Double.POSITIVE_INFINITY) {
                this.storedPermits = this.maxPermits;
            } else {
                this.storedPermits = d3 != 0.0d ? (this.storedPermits * this.maxPermits) / d3 : 0.0d;
            }
        }

        @Override // com.google.common.util.concurrent.SmoothRateLimiter
        long storedPermitsToWaitTime(double d, double d2) {
            return 0L;
        }
    }

    static final class SmoothWarmingUp extends SmoothRateLimiter {
        private double coldFactor;
        private double slope;
        private double thresholdPermits;
        private final long warmupPeriodMicros;

        SmoothWarmingUp(RateLimiter.SleepingStopwatch sleepingStopwatch, long j, TimeUnit timeUnit, double d) {
            super(sleepingStopwatch);
            this.warmupPeriodMicros = timeUnit.toMicros(j);
            this.coldFactor = d;
        }

        private double permitsToTime(double d) {
            return this.stableIntervalMicros + (this.slope * d);
        }

        @Override // com.google.common.util.concurrent.SmoothRateLimiter
        double coolDownIntervalMicros() {
            return this.warmupPeriodMicros / this.maxPermits;
        }

        @Override // com.google.common.util.concurrent.SmoothRateLimiter
        void doSetRate(double d, double d2) {
            double d3 = this.maxPermits;
            double d4 = this.coldFactor * d2;
            this.thresholdPermits = (this.warmupPeriodMicros * 0.5d) / d2;
            this.maxPermits = this.thresholdPermits + ((this.warmupPeriodMicros * 2.0d) / (d2 + d4));
            this.slope = (d4 - d2) / (this.maxPermits - this.thresholdPermits);
            if (d3 == Double.POSITIVE_INFINITY) {
                this.storedPermits = 0.0d;
            } else {
                this.storedPermits = d3 == 0.0d ? this.maxPermits : (this.storedPermits * this.maxPermits) / d3;
            }
        }

        @Override // com.google.common.util.concurrent.SmoothRateLimiter
        long storedPermitsToWaitTime(double d, double d2) {
            double d3 = d - this.thresholdPermits;
            long jPermitsToTime = 0;
            if (d3 > 0.0d) {
                double dMin = Math.min(d3, d2);
                jPermitsToTime = (long) (((permitsToTime(d3) + permitsToTime(d3 - dMin)) * dMin) / 2.0d);
                d2 -= dMin;
            }
            return jPermitsToTime + ((long) (this.stableIntervalMicros * d2));
        }
    }

    private SmoothRateLimiter(RateLimiter.SleepingStopwatch sleepingStopwatch) {
        super(sleepingStopwatch);
        this.nextFreeTicketMicros = 0L;
    }

    abstract double coolDownIntervalMicros();

    @Override // com.google.common.util.concurrent.RateLimiter
    final double doGetRate() {
        return TimeUnit.SECONDS.toMicros(1L) / this.stableIntervalMicros;
    }

    abstract void doSetRate(double d, double d2);

    @Override // com.google.common.util.concurrent.RateLimiter
    final void doSetRate(double d, long j) {
        resync(j);
        double micros = TimeUnit.SECONDS.toMicros(1L) / d;
        this.stableIntervalMicros = micros;
        doSetRate(d, micros);
    }

    @Override // com.google.common.util.concurrent.RateLimiter
    final long queryEarliestAvailable(long j) {
        return this.nextFreeTicketMicros;
    }

    @Override // com.google.common.util.concurrent.RateLimiter
    final long reserveEarliestAvailable(int i, long j) {
        resync(j);
        long j2 = this.nextFreeTicketMicros;
        double dMin = Math.min(i, this.storedPermits);
        this.nextFreeTicketMicros = LongMath.saturatedAdd(this.nextFreeTicketMicros, ((long) ((((double) i) - dMin) * this.stableIntervalMicros)) + storedPermitsToWaitTime(this.storedPermits, dMin));
        this.storedPermits -= dMin;
        return j2;
    }

    void resync(long j) {
        if (j > this.nextFreeTicketMicros) {
            this.storedPermits = Math.min(this.maxPermits, ((j - this.nextFreeTicketMicros) / coolDownIntervalMicros()) + this.storedPermits);
            this.nextFreeTicketMicros = j;
        }
    }

    abstract long storedPermitsToWaitTime(double d, double d2);
}
