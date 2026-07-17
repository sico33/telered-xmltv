package androidx.media3.exoplayer.upstream;

import android.content.Context;
import android.os.Handler;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.location.LocationRequestCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.view.MotionEventCompat;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.NetworkTypeObserver;
import androidx.media3.common.util.Util;
import androidx.media3.container.MdtaMetadataEntry;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor;
import androidx.media3.extractor.metadata.dvbsi.AppInfoTableDecoder;
import androidx.media3.extractor.ts.PsExtractor;
import androidx.media3.extractor.ts.TsExtractor;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.SignedBytes;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultBandwidthMeter implements BandwidthMeter, TransferListener {
    private static final int BYTES_TRANSFERRED_FOR_ESTIMATE = 524288;
    private static final int COUNTRY_GROUP_INDEX_2G = 1;
    private static final int COUNTRY_GROUP_INDEX_3G = 2;
    private static final int COUNTRY_GROUP_INDEX_4G = 3;
    private static final int COUNTRY_GROUP_INDEX_5G_NSA = 4;
    private static final int COUNTRY_GROUP_INDEX_5G_SA = 5;
    private static final int COUNTRY_GROUP_INDEX_WIFI = 0;
    public static final long DEFAULT_INITIAL_BITRATE_ESTIMATE = 1000000;
    public static final ImmutableList<Long> DEFAULT_INITIAL_BITRATE_ESTIMATES_3G;
    public static final ImmutableList<Long> DEFAULT_INITIAL_BITRATE_ESTIMATES_4G;
    public static final ImmutableList<Long> DEFAULT_INITIAL_BITRATE_ESTIMATES_5G_NSA;
    public static final ImmutableList<Long> DEFAULT_INITIAL_BITRATE_ESTIMATES_5G_SA;
    public static final int DEFAULT_SLIDING_WINDOW_MAX_WEIGHT = 2000;
    private static final int ELAPSED_MILLIS_FOR_ESTIMATE = 2000;
    private static DefaultBandwidthMeter singletonInstance;
    private long bitrateEstimate;
    private final Clock clock;
    private final BandwidthMeter.EventListener.EventDispatcher eventDispatcher;
    private final ImmutableMap<Integer, Long> initialBitrateEstimates;
    private long lastReportedBitrateEstimate;
    private int networkType;
    private int networkTypeOverride;
    private boolean networkTypeOverrideSet;
    private final boolean resetOnNetworkTypeChange;
    private long sampleBytesTransferred;
    private long sampleStartTimeMs;
    private final SlidingPercentile slidingPercentile;
    private int streamCount;
    private long totalBytesTransferred;
    private long totalElapsedTimeMs;
    public static final ImmutableList<Long> DEFAULT_INITIAL_BITRATE_ESTIMATES_WIFI = ImmutableList.of(4300000L, 3200000L, 2400000L, 1700000L, 860000L);
    public static final ImmutableList<Long> DEFAULT_INITIAL_BITRATE_ESTIMATES_2G = ImmutableList.of(1500000L, 980000L, 750000L, 520000L, 290000L);

    @Override // androidx.media3.exoplayer.upstream.BandwidthMeter
    public /* synthetic */ long getTimeToFirstByteEstimateUs() {
        return C.TIME_UNSET;
    }

    static {
        Long lValueOf = Long.valueOf(SilenceSkippingAudioProcessor.DEFAULT_MAX_SILENCE_TO_KEEP_DURATION_US);
        DEFAULT_INITIAL_BITRATE_ESTIMATES_3G = ImmutableList.of((long) lValueOf, 1300000L, 1000000L, 860000L, 610000L);
        DEFAULT_INITIAL_BITRATE_ESTIMATES_4G = ImmutableList.of(2500000L, 1700000L, 1200000L, 970000L, 680000L);
        DEFAULT_INITIAL_BITRATE_ESTIMATES_5G_NSA = ImmutableList.of(4700000L, 2800000L, 2100000L, 1700000L, 980000L);
        DEFAULT_INITIAL_BITRATE_ESTIMATES_5G_SA = ImmutableList.of(2700000L, (long) lValueOf, 1600000L, 1300000L, 1000000L);
    }

    public static final class Builder {
        private Clock clock;
        private final Context context;
        private Map<Integer, Long> initialBitrateEstimates;
        private boolean resetOnNetworkTypeChange;
        private int slidingWindowMaxWeight;

        public Builder(Context context) {
            this.context = context == null ? null : context.getApplicationContext();
            this.initialBitrateEstimates = getInitialBitrateEstimatesForCountry(Util.getCountryCode(context));
            this.slidingWindowMaxWeight = 2000;
            this.clock = Clock.DEFAULT;
            this.resetOnNetworkTypeChange = true;
        }

        public Builder setSlidingWindowMaxWeight(int slidingWindowMaxWeight) {
            this.slidingWindowMaxWeight = slidingWindowMaxWeight;
            return this;
        }

        public Builder setInitialBitrateEstimate(long initialBitrateEstimate) {
            for (Integer networkType : this.initialBitrateEstimates.keySet()) {
                setInitialBitrateEstimate(networkType.intValue(), initialBitrateEstimate);
            }
            return this;
        }

        public Builder setInitialBitrateEstimate(int networkType, long initialBitrateEstimate) {
            this.initialBitrateEstimates.put(Integer.valueOf(networkType), Long.valueOf(initialBitrateEstimate));
            return this;
        }

        public Builder setInitialBitrateEstimate(String countryCode) {
            this.initialBitrateEstimates = getInitialBitrateEstimatesForCountry(Ascii.toUpperCase(countryCode));
            return this;
        }

        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder setResetOnNetworkTypeChange(boolean resetOnNetworkTypeChange) {
            this.resetOnNetworkTypeChange = resetOnNetworkTypeChange;
            return this;
        }

        public DefaultBandwidthMeter build() {
            return new DefaultBandwidthMeter(this.context, this.initialBitrateEstimates, this.slidingWindowMaxWeight, this.clock, this.resetOnNetworkTypeChange);
        }

        private static Map<Integer, Long> getInitialBitrateEstimatesForCountry(String countryCode) {
            int[] groupIndices = DefaultBandwidthMeter.getInitialBitrateCountryGroupAssignment(countryCode);
            Map<Integer, Long> result = new HashMap<>(8);
            result.put(0, 1000000L);
            result.put(2, DefaultBandwidthMeter.DEFAULT_INITIAL_BITRATE_ESTIMATES_WIFI.get(groupIndices[0]));
            result.put(3, DefaultBandwidthMeter.DEFAULT_INITIAL_BITRATE_ESTIMATES_2G.get(groupIndices[1]));
            result.put(4, DefaultBandwidthMeter.DEFAULT_INITIAL_BITRATE_ESTIMATES_3G.get(groupIndices[2]));
            result.put(5, DefaultBandwidthMeter.DEFAULT_INITIAL_BITRATE_ESTIMATES_4G.get(groupIndices[3]));
            result.put(10, DefaultBandwidthMeter.DEFAULT_INITIAL_BITRATE_ESTIMATES_5G_NSA.get(groupIndices[4]));
            result.put(9, DefaultBandwidthMeter.DEFAULT_INITIAL_BITRATE_ESTIMATES_5G_SA.get(groupIndices[5]));
            result.put(7, DefaultBandwidthMeter.DEFAULT_INITIAL_BITRATE_ESTIMATES_WIFI.get(groupIndices[0]));
            return result;
        }
    }

    public static synchronized DefaultBandwidthMeter getSingletonInstance(Context context) {
        if (singletonInstance == null) {
            singletonInstance = new Builder(context).build();
        }
        return singletonInstance;
    }

    private DefaultBandwidthMeter(Context context, Map<Integer, Long> initialBitrateEstimates, int maxWeight, Clock clock, boolean resetOnNetworkTypeChange) {
        this.initialBitrateEstimates = ImmutableMap.copyOf((Map) initialBitrateEstimates);
        this.eventDispatcher = new BandwidthMeter.EventListener.EventDispatcher();
        this.slidingPercentile = new SlidingPercentile(maxWeight);
        this.clock = clock;
        this.resetOnNetworkTypeChange = resetOnNetworkTypeChange;
        if (context != null) {
            NetworkTypeObserver networkTypeObserver = NetworkTypeObserver.getInstance(context);
            this.networkType = networkTypeObserver.getNetworkType();
            this.bitrateEstimate = getInitialBitrateEstimateForNetworkType(this.networkType);
            networkTypeObserver.register(new NetworkTypeObserver.Listener() { // from class: androidx.media3.exoplayer.upstream.DefaultBandwidthMeter$$ExternalSyntheticLambda0
                @Override // androidx.media3.common.util.NetworkTypeObserver.Listener
                public final void onNetworkTypeChanged(int i) {
                    this.f$0.onNetworkTypeChanged(i);
                }
            });
            return;
        }
        this.networkType = 0;
        this.bitrateEstimate = getInitialBitrateEstimateForNetworkType(0);
    }

    public synchronized void setNetworkTypeOverride(int networkType) {
        this.networkTypeOverride = networkType;
        this.networkTypeOverrideSet = true;
        onNetworkTypeChanged(networkType);
    }

    @Override // androidx.media3.exoplayer.upstream.BandwidthMeter
    public synchronized long getBitrateEstimate() {
        return this.bitrateEstimate;
    }

    @Override // androidx.media3.exoplayer.upstream.BandwidthMeter
    public TransferListener getTransferListener() {
        return this;
    }

    @Override // androidx.media3.exoplayer.upstream.BandwidthMeter
    public void addEventListener(Handler eventHandler, BandwidthMeter.EventListener eventListener) {
        Assertions.checkNotNull(eventHandler);
        Assertions.checkNotNull(eventListener);
        this.eventDispatcher.addListener(eventHandler, eventListener);
    }

    @Override // androidx.media3.exoplayer.upstream.BandwidthMeter
    public void removeEventListener(BandwidthMeter.EventListener eventListener) {
        this.eventDispatcher.removeListener(eventListener);
    }

    @Override // androidx.media3.datasource.TransferListener
    public void onTransferInitializing(DataSource source, DataSpec dataSpec, boolean isNetwork) {
    }

    @Override // androidx.media3.datasource.TransferListener
    public synchronized void onTransferStart(DataSource source, DataSpec dataSpec, boolean isNetwork) {
        if (isTransferAtFullNetworkSpeed(dataSpec, isNetwork)) {
            if (this.streamCount == 0) {
                this.sampleStartTimeMs = this.clock.elapsedRealtime();
            }
            this.streamCount++;
        }
    }

    @Override // androidx.media3.datasource.TransferListener
    public synchronized void onBytesTransferred(DataSource source, DataSpec dataSpec, boolean isNetwork, int bytesTransferred) {
        if (isTransferAtFullNetworkSpeed(dataSpec, isNetwork)) {
            this.sampleBytesTransferred += (long) bytesTransferred;
        }
    }

    @Override // androidx.media3.datasource.TransferListener
    public synchronized void onTransferEnd(DataSource source, DataSpec dataSpec, boolean isNetwork) {
        if (isTransferAtFullNetworkSpeed(dataSpec, isNetwork)) {
            Assertions.checkState(this.streamCount > 0);
            long nowMs = this.clock.elapsedRealtime();
            int sampleElapsedTimeMs = (int) (nowMs - this.sampleStartTimeMs);
            this.totalElapsedTimeMs += (long) sampleElapsedTimeMs;
            this.totalBytesTransferred += this.sampleBytesTransferred;
            if (sampleElapsedTimeMs > 0) {
                float bitsPerSecond = (this.sampleBytesTransferred * 8000.0f) / sampleElapsedTimeMs;
                this.slidingPercentile.addSample((int) Math.sqrt(this.sampleBytesTransferred), bitsPerSecond);
                if (this.totalElapsedTimeMs >= ExoPlayer.DEFAULT_DETACH_SURFACE_TIMEOUT_MS || this.totalBytesTransferred >= PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED) {
                    this.bitrateEstimate = (long) this.slidingPercentile.getPercentile(0.5f);
                }
                maybeNotifyBandwidthSample(sampleElapsedTimeMs, this.sampleBytesTransferred, this.bitrateEstimate);
                this.sampleStartTimeMs = nowMs;
                this.sampleBytesTransferred = 0L;
            }
            this.streamCount--;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void onNetworkTypeChanged(int networkType) {
        if (this.networkType == 0 || this.resetOnNetworkTypeChange) {
            if (this.networkTypeOverrideSet) {
                networkType = this.networkTypeOverride;
            }
            if (this.networkType == networkType) {
                return;
            }
            this.networkType = networkType;
            if (networkType != 1 && networkType != 0 && networkType != 8) {
                this.bitrateEstimate = getInitialBitrateEstimateForNetworkType(networkType);
                long nowMs = this.clock.elapsedRealtime();
                int sampleElapsedTimeMs = this.streamCount > 0 ? (int) (nowMs - this.sampleStartTimeMs) : 0;
                maybeNotifyBandwidthSample(sampleElapsedTimeMs, this.sampleBytesTransferred, this.bitrateEstimate);
                this.sampleStartTimeMs = nowMs;
                this.sampleBytesTransferred = 0L;
                this.totalBytesTransferred = 0L;
                this.totalElapsedTimeMs = 0L;
                this.slidingPercentile.reset();
            }
        }
    }

    private void maybeNotifyBandwidthSample(int elapsedMs, long bytesTransferred, long bitrateEstimate) {
        if (elapsedMs == 0 && bytesTransferred == 0 && bitrateEstimate == this.lastReportedBitrateEstimate) {
            return;
        }
        this.lastReportedBitrateEstimate = bitrateEstimate;
        this.eventDispatcher.bandwidthSample(elapsedMs, bytesTransferred, bitrateEstimate);
    }

    private long getInitialBitrateEstimateForNetworkType(int networkType) {
        Long initialBitrateEstimate = this.initialBitrateEstimates.get(Integer.valueOf(networkType));
        if (initialBitrateEstimate == null) {
            initialBitrateEstimate = this.initialBitrateEstimates.get(0);
        }
        if (initialBitrateEstimate == null) {
            initialBitrateEstimate = 1000000L;
        }
        return initialBitrateEstimate.longValue();
    }

    private static boolean isTransferAtFullNetworkSpeed(DataSpec dataSpec, boolean isNetwork) {
        return isNetwork && !dataSpec.isFlagSet(8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:722:0x0b2c  */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    public static int[] getInitialBitrateCountryGroupAssignment(String country) {
        byte b;
        switch (country.hashCode()) {
            case 2083:
                if (!country.equals("AD")) {
                    b = -1;
                } else {
                    b = 205;
                }
                break;
            case 2084:
                if (!country.equals("AE")) {
                    b = -1;
                } else {
                    b = 0;
                }
                break;
            case 2085:
                if (!country.equals("AF")) {
                    b = -1;
                } else {
                    b = 168;
                }
                break;
            case 2086:
                if (!country.equals("AG")) {
                    b = -1;
                } else {
                    b = Ascii.NAK;
                }
                break;
            case 2088:
                if (!country.equals("AI")) {
                    b = -1;
                } else {
                    b = 206;
                }
                break;
            case 2091:
                if (!country.equals("AL")) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            case 2092:
                if (!country.equals("AM")) {
                    b = -1;
                } else {
                    b = 115;
                }
                break;
            case 2094:
                if (!country.equals("AO")) {
                    b = -1;
                } else {
                    b = 2;
                }
                break;
            case 2096:
                if (!country.equals("AQ")) {
                    b = -1;
                } else {
                    b = 145;
                }
                break;
            case 2097:
                if (!country.equals("AR")) {
                    b = -1;
                } else {
                    b = 3;
                }
                break;
            case 2098:
                if (!country.equals("AS")) {
                    b = -1;
                } else {
                    b = 4;
                }
                break;
            case 2099:
                if (!country.equals("AT")) {
                    b = -1;
                } else {
                    b = 137;
                }
                break;
            case 2100:
                if (!country.equals("AU")) {
                    b = -1;
                } else {
                    b = 5;
                }
                break;
            case 2102:
                if (!country.equals("AW")) {
                    b = -1;
                } else {
                    b = 6;
                }
                break;
            case 2103:
                if (!country.equals("AX")) {
                    b = -1;
                } else {
                    b = 198;
                }
                break;
            case 2105:
                if (!country.equals("AZ")) {
                    b = -1;
                } else {
                    b = 157;
                }
                break;
            case 2111:
                if (!country.equals("BA")) {
                    b = -1;
                } else {
                    b = 186;
                }
                break;
            case 2112:
                if (!country.equals("BB")) {
                    b = -1;
                } else {
                    b = 207;
                }
                break;
            case 2114:
                if (!country.equals("BD")) {
                    b = -1;
                } else {
                    b = 7;
                }
                break;
            case 2115:
                if (!country.equals("BE")) {
                    b = -1;
                } else {
                    b = 8;
                }
                break;
            case 2116:
                if (!country.equals("BF")) {
                    b = -1;
                } else {
                    b = 171;
                }
                break;
            case 2117:
                if (!country.equals("BG")) {
                    b = -1;
                } else {
                    b = 150;
                }
                break;
            case 2118:
                if (!country.equals("BH")) {
                    b = -1;
                } else {
                    b = 9;
                }
                break;
            case 2119:
                if (!country.equals("BI")) {
                    b = -1;
                } else {
                    b = 227;
                }
                break;
            case 2120:
                if (!country.equals("BJ")) {
                    b = -1;
                } else {
                    b = 10;
                }
                break;
            case 2122:
                if (!country.equals("BL")) {
                    b = -1;
                } else {
                    b = 127;
                }
                break;
            case 2123:
                if (!country.equals("BM")) {
                    b = -1;
                } else {
                    b = Ascii.VT;
                }
                break;
            case 2124:
                if (!country.equals("BN")) {
                    b = -1;
                } else {
                    b = Ascii.FF;
                }
                break;
            case 2125:
                if (!country.equals("BO")) {
                    b = -1;
                } else {
                    b = Ascii.CR;
                }
                break;
            case 2127:
                if (!country.equals("BQ")) {
                    b = -1;
                } else {
                    b = 208;
                }
                break;
            case 2128:
                if (!country.equals("BR")) {
                    b = -1;
                } else {
                    b = Ascii.SO;
                }
                break;
            case 2129:
                if (!country.equals("BS")) {
                    b = -1;
                } else {
                    b = Ascii.SI;
                }
                break;
            case 2130:
                if (!country.equals("BT")) {
                    b = -1;
                } else {
                    b = Ascii.DLE;
                }
                break;
            case 2133:
                if (!country.equals("BW")) {
                    b = -1;
                } else {
                    b = 17;
                }
                break;
            case 2135:
                if (!country.equals("BY")) {
                    b = -1;
                } else {
                    b = Ascii.DC2;
                }
                break;
            case 2136:
                if (!country.equals("BZ")) {
                    b = -1;
                } else {
                    b = Ascii.ETB;
                }
                break;
            case 2142:
                if (!country.equals("CA")) {
                    b = -1;
                } else {
                    b = 192;
                }
                break;
            case 2145:
                if (!country.equals("CD")) {
                    b = -1;
                } else {
                    b = 92;
                }
                break;
            case 2147:
                if (!country.equals("CF")) {
                    b = -1;
                } else {
                    b = 19;
                }
                break;
            case 2148:
                if (!country.equals("CG")) {
                    b = -1;
                } else {
                    b = 88;
                }
                break;
            case 2149:
                if (!country.equals("CH")) {
                    b = -1;
                } else {
                    b = Ascii.DC4;
                }
                break;
            case 2150:
                if (!country.equals("CI")) {
                    b = -1;
                } else {
                    b = Ascii.SYN;
                }
                break;
            case 2152:
                if (!country.equals("CK")) {
                    b = -1;
                } else {
                    b = Ascii.CAN;
                }
                break;
            case 2153:
                if (!country.equals("CL")) {
                    b = -1;
                } else {
                    b = 176;
                }
                break;
            case 2154:
                if (!country.equals("CM")) {
                    b = -1;
                } else {
                    b = 99;
                }
                break;
            case 2155:
                if (!country.equals("CN")) {
                    b = -1;
                } else {
                    b = Ascii.EM;
                }
                break;
            case 2156:
                if (!country.equals("CO")) {
                    b = -1;
                } else {
                    b = Ascii.SUB;
                }
                break;
            case 2159:
                if (!country.equals("CR")) {
                    b = -1;
                } else {
                    b = 108;
                }
                break;
            case 2162:
                if (!country.equals("CU")) {
                    b = -1;
                } else {
                    b = 180;
                }
                break;
            case 2163:
                if (!country.equals("CV")) {
                    b = -1;
                } else {
                    b = Ascii.ESC;
                }
                break;
            case 2164:
                if (!country.equals("CW")) {
                    b = -1;
                } else {
                    b = 209;
                }
                break;
            case 2165:
                if (!country.equals("CX")) {
                    b = -1;
                } else {
                    b = 199;
                }
                break;
            case 2166:
                if (!country.equals("CY")) {
                    b = -1;
                } else {
                    b = Ascii.FS;
                }
                break;
            case 2167:
                if (!country.equals("CZ")) {
                    b = -1;
                } else {
                    b = Ascii.GS;
                }
                break;
            case 2177:
                if (!country.equals("DE")) {
                    b = -1;
                } else {
                    b = Ascii.RS;
                }
                break;
            case 2182:
                if (!country.equals("DJ")) {
                    b = -1;
                } else {
                    b = 158;
                }
                break;
            case 2183:
                if (!country.equals("DK")) {
                    b = -1;
                } else {
                    b = Ascii.US;
                }
                break;
            case 2185:
                if (!country.equals("DM")) {
                    b = -1;
                } else {
                    b = 210;
                }
                break;
            case 2187:
                if (!country.equals("DO")) {
                    b = -1;
                } else {
                    b = 78;
                }
                break;
            case 2198:
                if (!country.equals("DZ")) {
                    b = -1;
                } else {
                    b = 178;
                }
                break;
            case 2206:
                if (!country.equals("EC")) {
                    b = -1;
                } else {
                    b = 32;
                }
                break;
            case 2208:
                if (!country.equals("EE")) {
                    b = -1;
                } else {
                    b = 138;
                }
                break;
            case 2210:
                if (!country.equals("EG")) {
                    b = -1;
                } else {
                    b = 89;
                }
                break;
            case 2221:
                if (!country.equals("ER")) {
                    b = -1;
                } else {
                    b = 146;
                }
                break;
            case 2222:
                if (!country.equals("ES")) {
                    b = -1;
                } else {
                    b = 33;
                }
                break;
            case 2223:
                if (!country.equals("ET")) {
                    b = -1;
                } else {
                    b = 34;
                }
                break;
            case 2243:
                if (!country.equals("FI")) {
                    b = -1;
                } else {
                    b = 35;
                }
                break;
            case 2244:
                if (!country.equals("FJ")) {
                    b = -1;
                } else {
                    b = 36;
                }
                break;
            case 2245:
                if (!country.equals("FK")) {
                    b = -1;
                } else {
                    b = 153;
                }
                break;
            case 2247:
                if (!country.equals("FM")) {
                    b = -1;
                } else {
                    b = 37;
                }
                break;
            case 2249:
                if (!country.equals("FO")) {
                    b = -1;
                } else {
                    b = 38;
                }
                break;
            case 2252:
                if (!country.equals("FR")) {
                    b = -1;
                } else {
                    b = 39;
                }
                break;
            case 2266:
                if (!country.equals("GA")) {
                    b = -1;
                } else {
                    b = 40;
                }
                break;
            case 2267:
                if (!country.equals("GB")) {
                    b = -1;
                } else {
                    b = 41;
                }
                break;
            case 2269:
                if (!country.equals("GD")) {
                    b = -1;
                } else {
                    b = 42;
                }
                break;
            case 2270:
                if (!country.equals("GE")) {
                    b = -1;
                } else {
                    b = 43;
                }
                break;
            case 2271:
                if (!country.equals("GF")) {
                    b = -1;
                } else {
                    b = 44;
                }
                break;
            case 2272:
                if (!country.equals("GG")) {
                    b = -1;
                } else {
                    b = 45;
                }
                break;
            case 2273:
                if (!country.equals("GH")) {
                    b = -1;
                } else {
                    b = 46;
                }
                break;
            case 2274:
                if (!country.equals("GI")) {
                    b = -1;
                } else {
                    b = 63;
                }
                break;
            case 2277:
                if (!country.equals("GL")) {
                    b = -1;
                } else {
                    b = 83;
                }
                break;
            case 2278:
                if (!country.equals("GM")) {
                    b = -1;
                } else {
                    b = 164;
                }
                break;
            case 2279:
                if (!country.equals("GN")) {
                    b = -1;
                } else {
                    b = 47;
                }
                break;
            case 2281:
                if (!country.equals("GP")) {
                    b = -1;
                } else {
                    b = 48;
                }
                break;
            case 2282:
                if (!country.equals("GQ")) {
                    b = -1;
                } else {
                    b = 228;
                }
                break;
            case 2283:
                if (!country.equals("GR")) {
                    b = -1;
                } else {
                    b = 49;
                }
                break;
            case 2285:
                if (!country.equals("GT")) {
                    b = -1;
                } else {
                    b = 50;
                }
                break;
            case 2286:
                if (!country.equals("GU")) {
                    b = -1;
                } else {
                    b = 51;
                }
                break;
            case 2288:
                if (!country.equals("GW")) {
                    b = -1;
                } else {
                    b = 52;
                }
                break;
            case 2290:
                if (!country.equals("GY")) {
                    b = -1;
                } else {
                    b = 53;
                }
                break;
            case 2307:
                if (!country.equals("HK")) {
                    b = -1;
                } else {
                    b = 54;
                }
                break;
            case 2314:
                if (!country.equals("HR")) {
                    b = -1;
                } else {
                    b = 72;
                }
                break;
            case 2316:
                if (!country.equals("HT")) {
                    b = -1;
                } else {
                    b = 229;
                }
                break;
            case 2317:
                if (!country.equals("HU")) {
                    b = -1;
                } else {
                    b = 139;
                }
                break;
            case 2331:
                if (!country.equals("ID")) {
                    b = -1;
                } else {
                    b = 55;
                }
                break;
            case 2332:
                if (!country.equals("IE")) {
                    b = -1;
                } else {
                    b = 56;
                }
                break;
            case 2339:
                if (!country.equals("IL")) {
                    b = -1;
                } else {
                    b = 57;
                }
                break;
            case 2340:
                if (!country.equals("IM")) {
                    b = -1;
                } else {
                    b = SignedBytes.MAX_POWER_OF_TWO;
                }
                break;
            case 2341:
                if (!country.equals("IN")) {
                    b = -1;
                } else {
                    b = 58;
                }
                break;
            case 2342:
                if (!country.equals("IO")) {
                    b = -1;
                } else {
                    b = 59;
                }
                break;
            case 2344:
                if (!country.equals("IQ")) {
                    b = -1;
                } else {
                    b = 60;
                }
                break;
            case 2345:
                if (!country.equals("IR")) {
                    b = -1;
                } else {
                    b = 61;
                }
                break;
            case 2346:
                if (!country.equals("IS")) {
                    b = -1;
                } else {
                    b = 140;
                }
                break;
            case 2347:
                if (!country.equals("IT")) {
                    b = -1;
                } else {
                    b = 62;
                }
                break;
            case 2363:
                if (!country.equals("JE")) {
                    b = -1;
                } else {
                    b = 65;
                }
                break;
            case 2371:
                if (!country.equals("JM")) {
                    b = -1;
                } else {
                    b = 66;
                }
                break;
            case 2373:
                if (!country.equals("JO")) {
                    b = -1;
                } else {
                    b = 187;
                }
                break;
            case 2374:
                if (!country.equals("JP")) {
                    b = -1;
                } else {
                    b = 67;
                }
                break;
            case 2394:
                if (!country.equals("KE")) {
                    b = -1;
                } else {
                    b = 68;
                }
                break;
            case 2396:
                if (!country.equals("KG")) {
                    b = -1;
                } else {
                    b = 69;
                }
                break;
            case 2397:
                if (!country.equals("KH")) {
                    b = -1;
                } else {
                    b = 70;
                }
                break;
            case 2398:
                if (!country.equals("KI")) {
                    b = -1;
                } else {
                    b = 181;
                }
                break;
            case 2402:
                if (!country.equals("KM")) {
                    b = -1;
                } else {
                    b = 218;
                }
                break;
            case 2403:
                if (!country.equals("KN")) {
                    b = -1;
                } else {
                    b = 211;
                }
                break;
            case 2407:
                if (!country.equals("KR")) {
                    b = -1;
                } else {
                    b = 71;
                }
                break;
            case 2412:
                if (!country.equals("KW")) {
                    b = -1;
                } else {
                    b = 73;
                }
                break;
            case 2414:
                if (!country.equals("KY")) {
                    b = -1;
                } else {
                    b = 212;
                }
                break;
            case 2415:
                if (!country.equals("KZ")) {
                    b = -1;
                } else {
                    b = 74;
                }
                break;
            case 2421:
                if (!country.equals("LA")) {
                    b = -1;
                } else {
                    b = 75;
                }
                break;
            case 2422:
                if (!country.equals("LB")) {
                    b = -1;
                } else {
                    b = 76;
                }
                break;
            case 2423:
                if (!country.equals("LC")) {
                    b = -1;
                } else {
                    b = 77;
                }
                break;
            case 2429:
                if (!country.equals("LI")) {
                    b = -1;
                } else {
                    b = 200;
                }
                break;
            case 2431:
                if (!country.equals("LK")) {
                    b = -1;
                } else {
                    b = 94;
                }
                break;
            case 2438:
                if (!country.equals("LR")) {
                    b = -1;
                } else {
                    b = 79;
                }
                break;
            case 2439:
                if (!country.equals("LS")) {
                    b = -1;
                } else {
                    b = 119;
                }
                break;
            case 2440:
                if (!country.equals("LT")) {
                    b = -1;
                } else {
                    b = 80;
                }
                break;
            case 2441:
                if (!country.equals("LU")) {
                    b = -1;
                } else {
                    b = 81;
                }
                break;
            case 2442:
                if (!country.equals("LV")) {
                    b = -1;
                } else {
                    b = 141;
                }
                break;
            case 2445:
                if (!country.equals("LY")) {
                    b = -1;
                } else {
                    b = 159;
                }
                break;
            case 2452:
                if (!country.equals("MA")) {
                    b = -1;
                } else {
                    b = 82;
                }
                break;
            case 2454:
                if (!country.equals("MC")) {
                    b = -1;
                } else {
                    b = 84;
                }
                break;
            case 2455:
                if (!country.equals("MD")) {
                    b = -1;
                } else {
                    b = 85;
                }
                break;
            case 2456:
                if (!country.equals("ME")) {
                    b = -1;
                } else {
                    b = 86;
                }
                break;
            case 2457:
                if (!country.equals("MF")) {
                    b = -1;
                } else {
                    b = 87;
                }
                break;
            case 2458:
                if (!country.equals("MG")) {
                    b = -1;
                } else {
                    b = 90;
                }
                break;
            case 2459:
                if (!country.equals("MH")) {
                    b = -1;
                } else {
                    b = 220;
                }
                break;
            case 2462:
                if (!country.equals("MK")) {
                    b = -1;
                } else {
                    b = 91;
                }
                break;
            case 2463:
                if (!country.equals("ML")) {
                    b = -1;
                } else {
                    b = 93;
                }
                break;
            case 2464:
                if (!country.equals("MM")) {
                    b = -1;
                } else {
                    b = 95;
                }
                break;
            case 2465:
                if (!country.equals("MN")) {
                    b = -1;
                } else {
                    b = 96;
                }
                break;
            case 2466:
                if (!country.equals("MO")) {
                    b = -1;
                } else {
                    b = 97;
                }
                break;
            case 2467:
                if (!country.equals("MP")) {
                    b = -1;
                } else {
                    b = 128;
                }
                break;
            case 2468:
                if (!country.equals("MQ")) {
                    b = -1;
                } else {
                    b = 98;
                }
                break;
            case 2469:
                if (!country.equals("MR")) {
                    b = -1;
                } else {
                    b = 100;
                }
                break;
            case 2470:
                if (!country.equals("MS")) {
                    b = -1;
                } else {
                    b = 201;
                }
                break;
            case 2471:
                if (!country.equals("MT")) {
                    b = -1;
                } else {
                    b = 142;
                }
                break;
            case 2472:
                if (!country.equals("MU")) {
                    b = -1;
                } else {
                    b = 101;
                }
                break;
            case 2473:
                if (!country.equals("MV")) {
                    b = -1;
                } else {
                    b = 102;
                }
                break;
            case 2474:
                if (!country.equals("MW")) {
                    b = -1;
                } else {
                    b = 103;
                }
                break;
            case 2475:
                if (!country.equals("MX")) {
                    b = -1;
                } else {
                    b = 104;
                }
                break;
            case 2476:
                if (!country.equals("MY")) {
                    b = -1;
                } else {
                    b = 105;
                }
                break;
            case 2477:
                if (!country.equals("MZ")) {
                    b = -1;
                } else {
                    b = 224;
                }
                break;
            case 2483:
                if (!country.equals("NA")) {
                    b = -1;
                } else {
                    b = 106;
                }
                break;
            case 2485:
                if (!country.equals("NC")) {
                    b = -1;
                } else {
                    b = 233;
                }
                break;
            case 2487:
                if (!country.equals("NE")) {
                    b = -1;
                } else {
                    b = 230;
                }
                break;
            case 2488:
                if (!country.equals("NF")) {
                    b = -1;
                } else {
                    b = 154;
                }
                break;
            case 2489:
                if (!country.equals("NG")) {
                    b = -1;
                } else {
                    b = 107;
                }
                break;
            case 2491:
                if (!country.equals("NI")) {
                    b = -1;
                } else {
                    b = 109;
                }
                break;
            case 2494:
                if (!country.equals("NL")) {
                    b = -1;
                } else {
                    b = 110;
                }
                break;
            case 2497:
                if (!country.equals("NO")) {
                    b = -1;
                } else {
                    b = 111;
                }
                break;
            case 2498:
                if (!country.equals("NP")) {
                    b = -1;
                } else {
                    b = 112;
                }
                break;
            case DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS /* 2500 */:
                if (!country.equals("NR")) {
                    b = -1;
                } else {
                    b = 182;
                }
                break;
            case 2503:
                if (!country.equals("NU")) {
                    b = -1;
                } else {
                    b = 147;
                }
                break;
            case 2508:
                if (!country.equals("NZ")) {
                    b = -1;
                } else {
                    b = 113;
                }
                break;
            case 2526:
                if (!country.equals("OM")) {
                    b = -1;
                } else {
                    b = 114;
                }
                break;
            case 2545:
                if (!country.equals("PA")) {
                    b = -1;
                } else {
                    b = 116;
                }
                break;
            case 2549:
                if (!country.equals("PE")) {
                    b = -1;
                } else {
                    b = 117;
                }
                break;
            case 2550:
                if (!country.equals("PF")) {
                    b = -1;
                } else {
                    b = 118;
                }
                break;
            case 2551:
                if (!country.equals("PG")) {
                    b = -1;
                } else {
                    b = 120;
                }
                break;
            case 2552:
                if (!country.equals("PH")) {
                    b = -1;
                } else {
                    b = 121;
                }
                break;
            case 2555:
                if (!country.equals("PK")) {
                    b = -1;
                } else {
                    b = 122;
                }
                break;
            case 2556:
                if (!country.equals("PL")) {
                    b = -1;
                } else {
                    b = 123;
                }
                break;
            case 2557:
                if (!country.equals("PM")) {
                    b = -1;
                } else {
                    b = 202;
                }
                break;
            case 2562:
                if (!country.equals("PR")) {
                    b = -1;
                } else {
                    b = 124;
                }
                break;
            case 2563:
                if (!country.equals("PS")) {
                    b = -1;
                } else {
                    b = 125;
                }
                break;
            case 2564:
                if (!country.equals("PT")) {
                    b = -1;
                } else {
                    b = 151;
                }
                break;
            case 2567:
                if (!country.equals("PW")) {
                    b = -1;
                } else {
                    b = 126;
                }
                break;
            case 2569:
                if (!country.equals("PY")) {
                    b = -1;
                } else {
                    b = 129;
                }
                break;
            case 2576:
                if (!country.equals("QA")) {
                    b = -1;
                } else {
                    b = 130;
                }
                break;
            case 2611:
                if (!country.equals("RE")) {
                    b = -1;
                } else {
                    b = 131;
                }
                break;
            case 2621:
                if (!country.equals("RO")) {
                    b = -1;
                } else {
                    b = 132;
                }
                break;
            case 2625:
                if (!country.equals("RS")) {
                    b = -1;
                } else {
                    b = 133;
                }
                break;
            case 2627:
                if (!country.equals("RU")) {
                    b = -1;
                } else {
                    b = 134;
                }
                break;
            case 2629:
                if (!country.equals("RW")) {
                    b = -1;
                } else {
                    b = 135;
                }
                break;
            case 2638:
                if (!country.equals("SA")) {
                    b = -1;
                } else {
                    b = 136;
                }
                break;
            case 2639:
                if (!country.equals("SB")) {
                    b = -1;
                } else {
                    b = 237;
                }
                break;
            case 2640:
                if (!country.equals("SC")) {
                    b = -1;
                } else {
                    b = 148;
                }
                break;
            case 2641:
                if (!country.equals("SD")) {
                    b = -1;
                } else {
                    b = 172;
                }
                break;
            case 2642:
                if (!country.equals("SE")) {
                    b = -1;
                } else {
                    b = 143;
                }
                break;
            case 2644:
                if (!country.equals("SG")) {
                    b = -1;
                } else {
                    b = 144;
                }
                break;
            case 2645:
                if (!country.equals("SH")) {
                    b = -1;
                } else {
                    b = 149;
                }
                break;
            case 2646:
                if (!country.equals("SI")) {
                    b = -1;
                } else {
                    b = 152;
                }
                break;
            case 2647:
                if (!country.equals("SJ")) {
                    b = -1;
                } else {
                    b = 155;
                }
                break;
            case 2648:
                if (!country.equals("SK")) {
                    b = -1;
                } else {
                    b = 156;
                }
                break;
            case 2649:
                if (!country.equals("SL")) {
                    b = -1;
                } else {
                    b = 160;
                }
                break;
            case 2650:
                if (!country.equals("SM")) {
                    b = -1;
                } else {
                    b = 203;
                }
                break;
            case 2651:
                if (!country.equals("SN")) {
                    b = -1;
                } else {
                    b = 161;
                }
                break;
            case 2652:
                if (!country.equals("SO")) {
                    b = -1;
                } else {
                    b = 162;
                }
                break;
            case 2655:
                if (!country.equals("SR")) {
                    b = -1;
                } else {
                    b = 163;
                }
                break;
            case 2656:
                if (!country.equals("SS")) {
                    b = -1;
                } else {
                    b = 165;
                }
                break;
            case 2657:
                if (!country.equals("ST")) {
                    b = -1;
                } else {
                    b = 166;
                }
                break;
            case 2659:
                if (!country.equals("SV")) {
                    b = -1;
                } else {
                    b = 167;
                }
                break;
            case 2661:
                if (!country.equals("SX")) {
                    b = -1;
                } else {
                    b = 213;
                }
                break;
            case 2662:
                if (!country.equals("SY")) {
                    b = -1;
                } else {
                    b = 173;
                }
                break;
            case 2663:
                if (!country.equals("SZ")) {
                    b = -1;
                } else {
                    b = 169;
                }
                break;
            case 2671:
                if (!country.equals("TC")) {
                    b = -1;
                } else {
                    b = 170;
                }
                break;
            case 2672:
                if (!country.equals("TD")) {
                    b = -1;
                } else {
                    b = 174;
                }
                break;
            case 2675:
                if (!country.equals("TG")) {
                    b = -1;
                } else {
                    b = 175;
                }
                break;
            case 2676:
                if (!country.equals("TH")) {
                    b = -1;
                } else {
                    b = 177;
                }
                break;
            case 2678:
                if (!country.equals("TJ")) {
                    b = -1;
                } else {
                    b = 179;
                }
                break;
            case 2680:
                if (!country.equals("TL")) {
                    b = -1;
                } else {
                    b = 183;
                }
                break;
            case 2681:
                if (!country.equals("TM")) {
                    b = -1;
                } else {
                    b = 221;
                }
                break;
            case 2682:
                if (!country.equals("TN")) {
                    b = -1;
                } else {
                    b = 184;
                }
                break;
            case 2683:
                if (!country.equals("TO")) {
                    b = -1;
                } else {
                    b = 185;
                }
                break;
            case 2686:
                if (!country.equals("TR")) {
                    b = -1;
                } else {
                    b = 188;
                }
                break;
            case 2688:
                if (!country.equals("TT")) {
                    b = -1;
                } else {
                    b = 189;
                }
                break;
            case 2690:
                if (!country.equals("TV")) {
                    b = -1;
                } else {
                    b = 222;
                }
                break;
            case 2691:
                if (!country.equals("TW")) {
                    b = -1;
                } else {
                    b = 190;
                }
                break;
            case 2694:
                if (!country.equals("TZ")) {
                    b = -1;
                } else {
                    b = 191;
                }
                break;
            case 2700:
                if (!country.equals("UA")) {
                    b = -1;
                } else {
                    b = 193;
                }
                break;
            case 2706:
                if (!country.equals("UG")) {
                    b = -1;
                } else {
                    b = 194;
                }
                break;
            case 2718:
                if (!country.equals("US")) {
                    b = -1;
                } else {
                    b = 195;
                }
                break;
            case 2724:
                if (!country.equals("UY")) {
                    b = -1;
                } else {
                    b = 196;
                }
                break;
            case 2725:
                if (!country.equals("UZ")) {
                    b = -1;
                } else {
                    b = 197;
                }
                break;
            case 2731:
                if (!country.equals("VA")) {
                    b = -1;
                } else {
                    b = 204;
                }
                break;
            case 2733:
                if (!country.equals("VC")) {
                    b = -1;
                } else {
                    b = 214;
                }
                break;
            case 2735:
                if (!country.equals("VE")) {
                    b = -1;
                } else {
                    b = 231;
                }
                break;
            case 2737:
                if (!country.equals("VG")) {
                    b = -1;
                } else {
                    b = 215;
                }
                break;
            case 2739:
                if (!country.equals("VI")) {
                    b = -1;
                } else {
                    b = 216;
                }
                break;
            case 2744:
                if (!country.equals("VN")) {
                    b = -1;
                } else {
                    b = 217;
                }
                break;
            case 2751:
                if (!country.equals("VU")) {
                    b = -1;
                } else {
                    b = 219;
                }
                break;
            case 2767:
                if (!country.equals("WF")) {
                    b = -1;
                } else {
                    b = 223;
                }
                break;
            case 2780:
                if (!country.equals("WS")) {
                    b = -1;
                } else {
                    b = 225;
                }
                break;
            case 2803:
                if (!country.equals("XK")) {
                    b = -1;
                } else {
                    b = 226;
                }
                break;
            case 2828:
                if (!country.equals("YE")) {
                    b = -1;
                } else {
                    b = 232;
                }
                break;
            case 2843:
                if (!country.equals("YT")) {
                    b = -1;
                } else {
                    b = 234;
                }
                break;
            case 2855:
                if (!country.equals("ZA")) {
                    b = -1;
                } else {
                    b = 235;
                }
                break;
            case 2867:
                if (!country.equals("ZM")) {
                    b = -1;
                } else {
                    b = 236;
                }
                break;
            case 2877:
                if (!country.equals("ZW")) {
                    b = -1;
                } else {
                    b = 238;
                }
                break;
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
                return new int[]{1, 4, 2, 3, 4, 1};
            case 1:
                return new int[]{1, 1, 1, 2, 2, 2};
            case 2:
                return new int[]{3, 4, 4, 3, 2, 2};
            case 3:
                return new int[]{2, 2, 2, 2, 1, 2};
            case 4:
                return new int[]{2, 2, 3, 3, 2, 2};
            case 5:
                return new int[]{0, 3, 1, 1, 3, 0};
            case 6:
                return new int[]{2, 2, 3, 4, 2, 2};
            case 7:
                return new int[]{2, 1, 3, 2, 4, 2};
            case 8:
                return new int[]{0, 0, 1, 0, 1, 2};
            case 9:
                return new int[]{1, 3, 1, 3, 4, 2};
            case 10:
                return new int[]{4, 4, 2, 3, 2, 2};
            case 11:
                return new int[]{0, 2, 0, 0, 2, 2};
            case 12:
                return new int[]{3, 2, 0, 0, 2, 2};
            case 13:
                return new int[]{1, 2, 4, 4, 2, 2};
            case 14:
                return new int[]{1, 1, 1, 1, 2, 4};
            case 15:
                return new int[]{3, 2, 1, 1, 2, 2};
            case 16:
                return new int[]{3, 1, 2, 2, 3, 2};
            case 17:
                return new int[]{3, 2, 1, 0, 2, 2};
            case 18:
                return new int[]{1, 2, 3, 3, 2, 2};
            case 19:
                return new int[]{4, 2, 4, 2, 2, 2};
            case 20:
                return new int[]{0, 1, 0, 0, 0, 2};
            case 21:
            case 22:
                return new int[]{2, 4, 3, 4, 2, 2};
            case 23:
            case 24:
                return new int[]{2, 2, 2, 1, 2, 2};
            case 25:
                return new int[]{2, 0, 1, 1, 3, 1};
            case 26:
                return new int[]{2, 3, 3, 2, 2, 2};
            case 27:
                return new int[]{2, 3, 0, 1, 2, 2};
            case 28:
                return new int[]{1, 0, 1, 0, 0, 2};
            case 29:
                return new int[]{0, 0, 2, 0, 1, 2};
            case 30:
                return new int[]{0, 1, 4, 2, 2, 1};
            case 31:
                return new int[]{0, 0, 2, 0, 0, 2};
            case 32:
                return new int[]{1, 3, 2, 1, 2, 2};
            case 33:
                return new int[]{0, 0, 0, 0, 1, 0};
            case 34:
                return new int[]{4, 3, 4, 4, 4, 2};
            case 35:
                return new int[]{0, 0, 0, 1, 0, 2};
            case 36:
                return new int[]{3, 2, 2, 3, 2, 2};
            case MotionEventCompat.AXIS_GENERIC_6 /* 37 */:
                return new int[]{4, 2, 4, 0, 2, 2};
            case 38:
                return new int[]{0, 2, 2, 0, 2, 2};
            case MotionEventCompat.AXIS_GENERIC_8 /* 39 */:
                return new int[]{1, 1, 1, 1, 0, 2};
            case MotionEventCompat.AXIS_GENERIC_9 /* 40 */:
                return new int[]{3, 4, 0, 0, 2, 2};
            case MotionEventCompat.AXIS_GENERIC_10 /* 41 */:
                return new int[]{1, 1, 3, 2, 2, 2};
            case 42:
                return new int[]{2, 2, 0, 0, 2, 2};
            case MotionEventCompat.AXIS_GENERIC_12 /* 43 */:
                return new int[]{1, 1, 0, 2, 2, 2};
            case MotionEventCompat.AXIS_GENERIC_13 /* 44 */:
                return new int[]{3, 2, 3, 3, 2, 2};
            case 45:
                return new int[]{0, 2, 1, 1, 2, 2};
            case MotionEventCompat.AXIS_GENERIC_15 /* 46 */:
                return new int[]{3, 3, 3, 2, 2, 2};
            case MotionEventCompat.AXIS_GENERIC_16 /* 47 */:
                return new int[]{3, 4, 4, 2, 2, 2};
            case 48:
                return new int[]{2, 1, 1, 3, 2, 2};
            case 49:
                return new int[]{1, 0, 0, 0, 1, 2};
            case DefaultRenderersFactory.MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY /* 50 */:
                return new int[]{2, 1, 2, 1, 2, 2};
            case 51:
                return new int[]{2, 2, 4, 3, 3, 2};
            case 52:
                return new int[]{4, 4, 1, 2, 2, 2};
            case 53:
                return new int[]{3, 1, 1, 3, 2, 2};
            case 54:
                return new int[]{0, 1, 0, 1, 1, 0};
            case 55:
                return new int[]{3, 1, 3, 3, 2, 4};
            case 56:
                return new int[]{1, 1, 1, 1, 1, 2};
            case 57:
                return new int[]{1, 2, 2, 3, 4, 2};
            case 58:
                return new int[]{1, 1, 3, 2, 2, 3};
            case 59:
                return new int[]{3, 2, 2, 0, 2, 2};
            case 60:
                return new int[]{3, 2, 3, 2, 2, 2};
            case 61:
                return new int[]{4, 2, 3, 3, 4, 3};
            case 62:
                return new int[]{0, 1, 1, 2, 1, 2};
            case HtmlCompat.FROM_HTML_MODE_COMPACT /* 63 */:
            case 64:
            case 65:
                return new int[]{0, 2, 0, 1, 2, 2};
            case 66:
                return new int[]{2, 4, 3, 1, 2, 2};
            case MdtaMetadataEntry.TYPE_INDICATOR_INT32 /* 67 */:
                return new int[]{0, 3, 2, 3, 4, 2};
            case 68:
                return new int[]{3, 2, 1, 1, 1, 2};
            case 69:
                return new int[]{2, 1, 1, 2, 2, 2};
            case 70:
                return new int[]{1, 0, 4, 2, 2, 2};
            case TsExtractor.TS_SYNC_BYTE /* 71 */:
                return new int[]{0, 2, 2, 4, 4, 4};
            case 72:
            case 73:
                return new int[]{1, 0, 0, 0, 0, 2};
            case 74:
                return new int[]{2, 1, 2, 2, 3, 2};
            case 75:
                return new int[]{1, 2, 1, 3, 2, 2};
            case 76:
                return new int[]{3, 1, 1, 2, 2, 2};
            case 77:
                return new int[]{2, 2, 1, 1, 2, 2};
            case 78:
            case 79:
                return new int[]{3, 4, 4, 4, 2, 2};
            case 80:
                return new int[]{0, 1, 0, 1, 0, 2};
            case 81:
                return new int[]{4, 0, 3, 2, 1, 3};
            case 82:
                return new int[]{3, 3, 1, 1, 2, 2};
            case 83:
            case 84:
                return new int[]{1, 2, 2, 0, 2, 2};
            case 85:
                return new int[]{1, 0, 0, 0, 2, 2};
            case 86:
                return new int[]{2, 0, 0, 1, 3, 2};
            case 87:
                return new int[]{1, 2, 2, 3, 2, 2};
            case 88:
            case TsExtractor.TS_STREAM_TYPE_DVBSUBS /* 89 */:
            case 90:
                return new int[]{3, 4, 3, 3, 2, 2};
            case 91:
                return new int[]{1, 0, 0, 1, 3, 2};
            case 92:
            case 93:
                return new int[]{3, 3, 2, 2, 2, 2};
            case 94:
            case 95:
                return new int[]{3, 2, 3, 3, 4, 2};
            case 96:
                return new int[]{2, 0, 2, 2, 2, 2};
            case 97:
                return new int[]{0, 2, 4, 4, 3, 1};
            case 98:
                return new int[]{2, 1, 2, 3, 2, 2};
            case 99:
            case 100:
                return new int[]{4, 3, 3, 4, 2, 2};
            case 101:
                return new int[]{3, 1, 0, 2, 2, 2};
            case LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY /* 102 */:
                return new int[]{3, 2, 1, 3, 4, 2};
            case 103:
                return new int[]{3, 2, 2, 1, 2, 2};
            case LocationRequestCompat.QUALITY_LOW_POWER /* 104 */:
                return new int[]{2, 4, 4, 4, 3, 2};
            case 105:
                return new int[]{1, 0, 4, 1, 1, 0};
            case 106:
                return new int[]{3, 4, 3, 2, 2, 2};
            case 107:
                return new int[]{3, 4, 2, 1, 2, 2};
            case 108:
            case 109:
                return new int[]{2, 4, 4, 4, 2, 2};
            case 110:
                return new int[]{2, 1, 4, 3, 0, 4};
            case 111:
                return new int[]{0, 0, 3, 0, 0, 2};
            case 112:
                return new int[]{2, 2, 4, 3, 2, 2};
            case 113:
                return new int[]{0, 0, 1, 2, 4, 2};
            case 114:
                return new int[]{2, 3, 1, 2, 4, 2};
            case 115:
            case AppInfoTableDecoder.APPLICATION_INFORMATION_TABLE_ID /* 116 */:
                return new int[]{2, 3, 2, 3, 2, 2};
            case 117:
                return new int[]{1, 2, 4, 4, 3, 2};
            case 118:
                return new int[]{2, 2, 3, 1, 2, 2};
            case 119:
            case 120:
                return new int[]{4, 3, 3, 3, 2, 2};
            case 121:
                return new int[]{2, 1, 2, 3, 2, 1};
            case 122:
                return new int[]{3, 3, 3, 3, 2, 2};
            case 123:
                return new int[]{1, 0, 2, 2, 4, 4};
            case 124:
                return new int[]{2, 0, 2, 1, 2, 0};
            case 125:
                return new int[]{3, 4, 1, 3, 2, 2};
            case 126:
                return new int[]{2, 2, 4, 1, 2, 2};
            case 127:
            case 128:
            case TsExtractor.TS_STREAM_TYPE_AC3 /* 129 */:
                return new int[]{1, 2, 2, 2, 2, 2};
            case TsExtractor.TS_STREAM_TYPE_HDMV_DTS /* 130 */:
                return new int[]{1, 4, 4, 4, 4, 2};
            case 131:
                return new int[]{0, 3, 2, 3, 1, 2};
            case 132:
                return new int[]{0, 0, 1, 1, 3, 2};
            case 133:
                return new int[]{1, 0, 0, 1, 2, 2};
            case TsExtractor.TS_STREAM_TYPE_SPLICE_INFO /* 134 */:
                return new int[]{1, 0, 0, 1, 3, 3};
            case TsExtractor.TS_STREAM_TYPE_E_AC3 /* 135 */:
                return new int[]{3, 3, 2, 0, 2, 2};
            case TsExtractor.TS_STREAM_TYPE_DTS_HD /* 136 */:
                return new int[]{3, 1, 1, 2, 2, 0};
            case 137:
            case TsExtractor.TS_STREAM_TYPE_DTS /* 138 */:
            case TsExtractor.TS_STREAM_TYPE_DTS_UHD /* 139 */:
            case 140:
            case 141:
            case 142:
            case 143:
                return new int[]{0, 0, 0, 0, 0, 2};
            case 144:
                return new int[]{2, 3, 3, 3, 1, 1};
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
                return new int[]{4, 2, 2, 2, 2, 2};
            case 150:
            case 151:
            case 152:
                return new int[]{0, 0, 0, 0, 1, 2};
            case 153:
            case 154:
            case 155:
                return new int[]{3, 2, 2, 2, 2, 2};
            case 156:
                return new int[]{0, 1, 1, 1, 2, 2};
            case 157:
            case 158:
            case 159:
            case 160:
                return new int[]{4, 2, 3, 3, 2, 2};
            case 161:
                return new int[]{4, 4, 3, 2, 2, 2};
            case 162:
                return new int[]{2, 2, 3, 4, 4, 2};
            case 163:
                return new int[]{2, 4, 4, 1, 2, 2};
            case 164:
            case 165:
                return new int[]{4, 3, 2, 4, 2, 2};
            case 166:
                return new int[]{2, 2, 1, 2, 2, 2};
            case 167:
                return new int[]{2, 3, 2, 1, 2, 2};
            case 168:
            case 169:
                return new int[]{4, 4, 3, 4, 2, 2};
            case 170:
                return new int[]{3, 2, 1, 2, 2, 2};
            case 171:
            case TsExtractor.TS_STREAM_TYPE_AC4 /* 172 */:
            case 173:
            case 174:
                return new int[]{4, 3, 4, 4, 2, 2};
            case 175:
                return new int[]{3, 4, 1, 0, 2, 2};
            case 176:
            case 177:
                return new int[]{0, 1, 2, 2, 2, 2};
            case 178:
            case 179:
                return new int[]{3, 3, 4, 4, 2, 2};
            case 180:
            case 181:
            case 182:
            case 183:
                return new int[]{4, 2, 4, 4, 2, 2};
            case 184:
                return new int[]{3, 1, 1, 1, 2, 2};
            case 185:
                return new int[]{3, 2, 4, 3, 2, 2};
            case 186:
            case 187:
            case TsExtractor.TS_PACKET_SIZE /* 188 */:
                return new int[]{1, 1, 1, 1, 2, 2};
            case PsExtractor.PRIVATE_STREAM_1 /* 189 */:
                return new int[]{2, 4, 1, 0, 2, 2};
            case 190:
                return new int[]{0, 0, 0, 0, 0, 0};
            case 191:
                return new int[]{3, 4, 2, 1, 3, 2};
            case PsExtractor.AUDIO_STREAM /* 192 */:
            case 193:
                return new int[]{0, 2, 1, 2, 3, 3};
            case 194:
                return new int[]{3, 3, 2, 3, 4, 2};
            case 195:
                return new int[]{2, 2, 4, 1, 3, 1};
            case 196:
                return new int[]{2, 1, 1, 2, 1, 2};
            case 197:
                return new int[]{1, 2, 3, 4, 3, 2};
            case 198:
            case 199:
            case 200:
            case 201:
            case 202:
            case 203:
            case 204:
                return new int[]{0, 2, 2, 2, 2, 2};
            case 205:
            case 206:
            case 207:
            case 208:
            case 209:
            case 210:
            case 211:
            case 212:
            case 213:
            case 214:
                return new int[]{1, 2, 0, 0, 2, 2};
            case 215:
                return new int[]{2, 2, 1, 1, 2, 4};
            case 216:
                return new int[]{0, 2, 1, 2, 2, 2};
            case 217:
                return new int[]{0, 0, 1, 2, 2, 2};
            case 218:
            case 219:
                return new int[]{4, 3, 3, 2, 2, 2};
            case 220:
            case 221:
            case 222:
            case 223:
                return new int[]{4, 2, 2, 4, 2, 2};
            case 224:
            case 225:
                return new int[]{3, 1, 2, 2, 2, 2};
            case 226:
                return new int[]{1, 2, 1, 1, 2, 2};
            case 227:
            case 228:
            case 229:
            case 230:
            case 231:
            case 232:
                return new int[]{4, 4, 4, 4, 2, 2};
            case 233:
            case 234:
                return new int[]{2, 3, 3, 4, 2, 2};
            case 235:
                return new int[]{2, 4, 2, 1, 1, 2};
            case 236:
                return new int[]{4, 4, 4, 3, 2, 2};
            case 237:
            case 238:
                return new int[]{4, 2, 4, 3, 2, 2};
            default:
                return new int[]{2, 2, 2, 2, 2, 2};
        }
    }
}
