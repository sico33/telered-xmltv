package com.playtv.premium;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.location.LocationRequestCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.CueGroup;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.extractor.ts.TsExtractor;
import androidx.recyclerview.widget.ItemTouchHelper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/* JADX INFO: loaded from: classes2.dex */
final class ChannelTester {
    private static short[] E = {-30028, -29961, -30039, -29969, -30046, -29685, -29670, -29670, -29690, -29693, -29687, -29685, -29666, -29693, -29691, -29692, -29627, -29678, -29625, -29689, -29670, -29681, -29683, -29633, -29640, -29658, -31999, -31934, -31905, -31925, -26984, -26999, -26999, -26987, -26992, -26982, -26984, -26995, -26992, -26986, -26985, -26922, -26979, -26984, -26998, -26991, -26926, -27007, -26988, -26987, -27126, -27116, -27111, -27112, -27125, -27116, -27117, -27112, -15399, -15462, -15420, -15486, -15409, -14486, -14516, -14502, -14515, -14574, -14466, -14504, -14502, -14511, -14517, -1720, -1719, -1718, -1715, -1703, -1728, -1704, -1677, -1703, -1697, -1719, -1698, -1677, -1715, -1717, -1719, -1726, -1704, -1720, -1676, -1671, -1695, -1716, -1714, -1720, -1686, -1667, -1675, -1679, -1683, -1675, -7547, -7536, -7545, -7534, -7534, -7536, -7480, -7547, -7543, -7541, -7480, -7545, -7532, -2916, -2935, -2914, -2933, -2933, -2935, -2863, -2916, -2928, -2926, -2863, -2929, -2938, -8376, -8355, -8374, -8353, -8353, -8355, -8443, -8372, -8370, -8353, -8442, -8371, -8370, -8378, -8380, -8379, -8443, -8379, -8370, -8353, -6476, -6466, -6467, -6491, -6404, -6479, -6467, -6465, -6404, -6477, -6496, -1981, -1975, -1974, -1966, -2037, -1978, -1974, -1976, -2037, -1963, -1956, -3152, -3075, -3087, -3085, -3152, -3090, -3097, -1449, -1461, -1461, -1457, -1460, -1531, -1520, -1520, -1457, -1456, -1459, -1461, -1442, -1453, -1519, -1442, -1457, -1457, -1519, -1447, -1453, -1456, -1464, -1519, -1444, -1456, -1454, -1519, -1457, -1466, -1621, -1642, -1651, -1661, -1651, -1654, -10015, -9987, -9987, -9991, -9990, -10061, -10074, -10074, -9991, -10010, -9989, -9987, -10008, -10011, -10073, -10008, -9991, -9991, -10073, -10001, -10011, -10010, -9986, -10073, -10006, -10010, -10012, -10073, -9991, -10000, -10074, -4271, -4250, -4251, -4250, -4239, -4250, -4239, -5452, -5464, -5464, -5460, -5457, -5402, -5389, -5389, -5460, -5453, -5458, -5464, -5443, -5456, -5390, -5443, -5460, -5460, -5390, -5446, -5456, -5453, -5461, -5390, -5441, -5453, -5455, -5390, -5443, -5458, -8474, -8454, -8454, -8450, -8451, -8524, -8543, -8543, -8450, -8479, -8452, -8454, -8465, -8478, -8544, -8465, -8450, -8450, -8544, -8472, -8478, -8479, -8455, -8544, -8467, -8479, -8477, -8544, -8465, -8452, -8543, 24054, 24042, 24039, 24063, 24050, 24048, 24057, 24050, 24035, 24053, 24050};
    private static String TAG = E(314, 325, 23974);
    private static final long TIMEOUT_MS = 10000;

    static class TestResult {
        final String category;
        final String channelName;
        final String detail;
        final int globalIndex;
        final boolean ok;
        final String testedUrl;

        TestResult(String str, String str2, int i, boolean z, String str3, String str4) {
            this.channelName = str;
            this.category = str2;
            this.globalIndex = i;
            this.ok = z;
            this.detail = str3;
            this.testedUrl = str4;
        }
    }

    static class TestSession {
        private static short[] N = {15574, 15562, 15559, 15583, 15570, 15568, 15577, 15570, 15555, 15573, 15570, 7220, 7219, 7227, 7230, 7250, 11064, 2828, 11064, 11085, 11082, 11092, 11064, 11118, 11129, 11131, 11253, 11129, 12911, 12904, 12918, 12826, 12876, 12891, 12889, 13015, 12891, 8455, 8475, 8470, 8462, 8451, 8449, 8456, 8451, 8466, 8452, 8451, 13610, 13595, 13581, 13578, 13591, 13584, 13593, 13662, 15735, 7365, 15735, 13850, 13867, 13884, 13880, 13867, 13945, 13865, 13877, 13880, 13856, 13884, 13867, 13923, 13945, 10996, 10984, 10981, 11005, 10992, 10994, 11003, 10992, 10977, 10999, 10992, 7840, 7844, 7887, 7887, 13513, 5373, 13513, 13499, 13484, 13480, 13485, 13488, 13513, 13452, 13447, 13513, 12485, 12507, 11009, 11030, 11026, 11031, 11018, 11123, 11062, 11069, 11123, 15068, 15042, 13991, 14011, 14006, 13998, 13987, 13985, 13992, 13987, 14002, 13988, 13987, 8538, 8538, 12900, 12845, 12842, 12848, 12833, 12854, 12854, 12849, 12852, 12848, 12833, 12832, 12900, 12833, 12855, 12852, 12833, 12854, 12837, 12842, 12832, 12843, 12900, 12822, 12801, 12805, 12800, 12829, 7621, 7641, 7636, 7628, 7617, 7619, 7626, 7617, 7632, 7622, 7617, 6846, 6841, 6833, 6836, 6872, 12638, 4458, 12638, 10065, 10059, 10060, 9986, 10055, 10065, 10070, 10051, 10054, 10061, 10885, 10905, 10900, 10892, 10881, 10883, 10890, 10881, 10896, 10886, 10881, 10451, 10452, 10460, 10457, 10421, 6826, 15006, 6826, 6878, 6883, 6887, 6895, 6885, 6911, 6910, 6826, 6818, 12159, 12075, 12077, 12094, 12076, 12159, 13367, 13421, 14751, 14754, 14758, 14766, 14756, 14782, 14783, 14827, 14819, 10431, 10475, 10477, 10494, 10476, 10431, 14510, 14580, 13576, 13599, 13580, 13580, 13583, 13592, 13571, 13572, 13581, 14732, 14747, 14751, 14746, 14727, 13398, 13405, 13399, 13398, 13399, 14392, 14389, 14397, 14388, 11661, 8998, 9018, 9015, 9007, 8994, 8992, 9001, 8994, 9011, 8997, 8994, 12585, 12585, 14112, 14185, 14190, 14196, 14181, 14194, 14194, 14197, 14192, 14196, 14181, 14180, 14112, 14179, 14194, 14181, 14177, 14190, 14180, 14191, 14112, 14192, 14188, 14177, 14201, 14181, 14194, 12686, 12713, 12723, 12706, 12725, 12725, 12722, 12727, 12723, 12706, 12707, 12674, 12735, 12708, 12706, 12727, 12723, 12718, 12712, 12713, 12775, 12710, 12715, 12775, 12708, 12725, 12706, 12710, 12725, 12775, 12727, 12715, 12710, 12734, 12706, 12725, 14815, 14787, 14798, 14806, 14811, 14809, 14800, 14811, 14794, 14812, 14811, 11732, 11731, 11739, 11742, 11698, 15740, 7496, 15740, 15662, 15673, 15663, 15667, 15664, 15658, 15673, 15628, 15664, 15677, 15653, 15677, 15678, 15664, 15673, 15625, 15662, 15664, 15718, 15740, 16166, 16177, 16167, 16187, 16184, 16162, 16177, 16129, 16134, 16152, 16238, 16244, 15769, 15747, -21759, -21721, -21711, -21722, -21639, -21739, -21709, -21711, -21702, -21728, -23920, -23919, -23918, -23915, -23935, -23912, -23936, -23893, -23935, -23929, -23919, -23930, -23893, -23915, -23917, -23919, -23910, -23936, -23082, -23062, -23065, -23041, -23086, -23088, -23082, -23052, -23069, -23061, -23057, -23053, -23061, -18457, -18495, -18473, -18496, -18529, -18445, -18475, -18473, -18468, -18490, -17942, -17930, -17925, -17949, -17938, -17940, -17947, -17938, -17921, -17943, -17938, -23828, -23828, -22572, -22601, -22618, -22607, -22603, -22624, -22607, -22572, -22607, -22618, -22618, -22597, -22618, -22578, -22572, -18244, -18266, 21790, 21786, 21785, 22990, 22994, 23007, 22983, 22986, 22984, 22977, 22986, 23003, 22989, 22986, 22497, 22501, 22502, 22517, 22448, 22459, 22517, 25837, 25825, 25779, 25764, 25767, 25779, 25764, 25778, 25762, 25760, 25775, 25765, 25774, 25825, 25781, 25774, 25770, 25764, 25775, 25825, 25784, 25825, 25779, 25764, 25768, 25775, 25781, 25764, 25775, 25781, 25760, 25775, 25765, 25774, 23232, 23236, 23215, 23207, 23291, 23293, 23278, 23292, 23215, 23293, 23274, 23273, 23293, 23274, 23292, 23271, 23215, 23291, 23264, 23268, 23274, 23265, 23206};
        private final Context context;

        TestSession(Context context) {
            this.context = context.getApplicationContext();
        }

        private static String N(int i, int i2, int i3) {
            char[] cArr = new char[i2 - i];
            for (int i4 = 0; i4 < i2 - i; i4++) {
                cArr[i4] = (char) (N[i + i4] ^ i3);
            }
            return new String(cArr);
        }

        static /* synthetic */ DrmSessionManager lambda$testOnce$0(DrmSessionManager drmSessionManager, DrmSessionManagerProvider drmSessionManagerProvider, MediaItem mediaItem) {
            MediaItem.DrmConfiguration drmConfiguration = mediaItem.localConfiguration != null ? mediaItem.localConfiguration.drmConfiguration : null;
            return (drmConfiguration == null || !C.CLEARKEY_UUID.equals(drmConfiguration.scheme)) ? drmSessionManagerProvider.get(mediaItem) : drmSessionManager;
        }

        private TestResult testOnce(final Channel channel, final AppConfig appConfig) throws InterruptedException {
            boolean zAwait;
            String strN;
            String str = channel.originalUrl == null ? "" : channel.originalUrl;
            try {
                ChannelTester.hydrateFlowHeaders(channel, appConfig);
                final String strResolvePlayableUrl = FlowSigner.resolvePlayableUrl(channel, appConfig);
                if (strResolvePlayableUrl == null || strResolvePlayableUrl.isEmpty()) {
                    Log.w(N(0, 11, 15494), N(11, 16, 7282) + channel.name + N(16, 28, 11032));
                    return ChannelTester.fail(channel, N(28, 37, 12858), str);
                }
                Log.d(N(37, 48, 8535), N(48, 56, 13694) + channel.name + N(56, 59, 15703) + strResolvePlayableUrl);
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                final AtomicReference atomicReference = new AtomicReference(null);
                final AtomicReference atomicReference2 = new AtomicReference(null);
                final AtomicReference atomicReference3 = new AtomicReference(null);
                long jCurrentTimeMillis = System.currentTimeMillis();
                final CountDownLatch countDownLatch2 = new CountDownLatch(1);
                final AtomicReference atomicReference4 = new AtomicReference(null);
                new Handler(Looper.getMainLooper()).post(new Runnable(this, channel, strResolvePlayableUrl, appConfig, atomicReference3, atomicReference2, countDownLatch, atomicReference, atomicReference4, countDownLatch2) { // from class: com.playtv.premium.ChannelTester$TestSession$$ExternalSyntheticLambda0
                    public final ChannelTester.TestSession f$0;
                    public final Channel f$1;
                    public final String f$2;
                    public final AppConfig f$3;
                    public final AtomicReference f$4;
                    public final AtomicReference f$5;
                    public final CountDownLatch f$6;
                    public final AtomicReference f$7;
                    public final AtomicReference f$8;
                    public final CountDownLatch f$9;

                    {
                        this.f$0 = this;
                        this.f$1 = channel;
                        this.f$2 = strResolvePlayableUrl;
                        this.f$3 = appConfig;
                        this.f$4 = atomicReference3;
                        this.f$5 = atomicReference2;
                        this.f$6 = countDownLatch;
                        this.f$7 = atomicReference;
                        this.f$8 = atomicReference4;
                        this.f$9 = countDownLatch2;
                    }

                    @Override // java.lang.Runnable
                    public final void run() throws Throwable {
                        this.f$0.m217lambda$testOnce$1$complaytvpremiumChannelTester$TestSession(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9);
                    }
                });
                try {
                    try {
                        countDownLatch2.await(5L, TimeUnit.SECONDS);
                        if (atomicReference4.get() != null) {
                            return ChannelTester.fail(channel, N(59, 73, 13913) + ((String) atomicReference4.get()), strResolvePlayableUrl);
                        }
                        try {
                            try {
                                zAwait = countDownLatch.await(10000L, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                Log.e(N(113, 124, 14071), N(124, 126, 8570) + channel.name + N(126, 154, 12868));
                                zAwait = false;
                            }
                        } catch (InterruptedException e2) {
                        }
                        long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
                        final ExoPlayer exoPlayer = (ExoPlayer) atomicReference3.get();
                        if (exoPlayer != null) {
                            new Handler(Looper.getMainLooper()).post(new Runnable(exoPlayer) { // from class: com.playtv.premium.ChannelTester$TestSession$$ExternalSyntheticLambda1
                                public final ExoPlayer f$0;

                                {
                                    this.f$0 = exoPlayer;
                                }

                                @Override // java.lang.Runnable
                                public final void run() {
                                    this.f$0.release();
                                }
                            });
                        }
                        if (zAwait && atomicReference2.get() != null && (((Integer) atomicReference2.get()).intValue() == 3 || ((Integer) atomicReference2.get()).intValue() == 4)) {
                            Log.d(N(73, 84, 10916), N(84, 88, 7919) + channel.name + N(88, 100, 13545) + jCurrentTimeMillis2 + N(100, LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY, 12456));
                            return ChannelTester.ok(channel, N(LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY, 111, 11091) + jCurrentTimeMillis2 + N(111, 113, 15025), strResolvePlayableUrl);
                        }
                        if (atomicReference.get() != null) {
                            Log.d(N(154, 165, 7573), N(165, 170, 6904) + channel.name + N(170, 173, 12670) + ((String) atomicReference.get()));
                            return ChannelTester.fail(channel, (String) atomicReference.get(), strResolvePlayableUrl);
                        }
                        Integer num = (Integer) atomicReference2.get();
                        if (num == null) {
                            strN = N(173, 183, 10018);
                        } else if (num.intValue() == 2) {
                            strN = N(236, 245, 13674);
                        } else if (num.intValue() == 3) {
                            strN = N(245, ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION, 14846);
                        } else if (num.intValue() == 4) {
                            strN = N(ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION, 255, 13363);
                        } else {
                            strN = num.intValue() == 1 ? N(255, 259, 14417) : N(259, 260, 11698) + num;
                        }
                        Log.d(N(183, 194, 10965), N(194, 199, 10389) + channel.name + N(199, 211, 6794) + strN + N(211, 217, 12127) + 10L + N(217, 219, 13380));
                        return ChannelTester.fail(channel, N(219, 228, 14795) + strN + N(228, 234, 10399) + 10L + N(234, 236, 14557), strResolvePlayableUrl);
                    } catch (InterruptedException e3) {
                        Log.e(N(260, 271, 9078), N(271, 273, 12553) + channel.name + N(273, 300, 14080));
                        return ChannelTester.fail(channel, N(300, 336, 12743), strResolvePlayableUrl);
                    }
                } catch (InterruptedException e4) {
                }
            } catch (Exception e5) {
                Log.e(N(336, 347, 14735), N(347, 352, 11666) + channel.name + N(352, 375, 15708) + e5.getMessage());
                return ChannelTester.fail(channel, N(375, 387, 16212) + e5.getClass().getSimpleName() + N(387, 389, 15779) + e5.getMessage(), str);
            }
        }

        void close() {
        }

        /* JADX INFO: renamed from: lambda$testOnce$1$com-playtv-premium-ChannelTester$TestSession, reason: not valid java name */
        /* synthetic */ void m217lambda$testOnce$1$complaytvpremiumChannelTester$TestSession(Channel channel, String str, AppConfig appConfig, AtomicReference atomicReference, AtomicReference atomicReference2, CountDownLatch countDownLatch, AtomicReference atomicReference3, AtomicReference atomicReference4, CountDownLatch countDownLatch2) throws Throwable {
            Exception exc;
            try {
                Map mapChooseHeaders = ChannelTester.chooseHeaders(channel, str, appConfig);
                try {
                    DefaultHttpDataSource.Factory userAgent = new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).setUserAgent((String) mapChooseHeaders.getOrDefault(N(389, 399, -21676), appConfig.getOrDefault(N(399, 417, -23820), N(417, 430, -23162))));
                    HashMap map = new HashMap(mapChooseHeaders);
                    map.remove(N(430, 440, -18510));
                    userAgent.setDefaultRequestProperties((Map<String, String>) map);
                    DefaultMediaSourceFactory defaultMediaSourceFactory = new DefaultMediaSourceFactory(new DefaultDataSource.Factory(this.context, userAgent));
                    String clearKeyJson = channel.toClearKeyJson();
                    if (!clearKeyJson.isEmpty()) {
                        final DefaultDrmSessionManager defaultDrmSessionManagerBuild = new DefaultDrmSessionManager.Builder().setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER).build(new LocalMediaDrmCallback(clearKeyJson.getBytes(StandardCharsets.UTF_8)));
                        final DefaultDrmSessionManagerProvider defaultDrmSessionManagerProvider = new DefaultDrmSessionManagerProvider();
                        defaultMediaSourceFactory.setDrmSessionManagerProvider(new DrmSessionManagerProvider(defaultDrmSessionManagerBuild, defaultDrmSessionManagerProvider) { // from class: com.playtv.premium.ChannelTester$TestSession$$ExternalSyntheticLambda2
                            public final DrmSessionManager f$0;
                            public final DrmSessionManagerProvider f$1;

                            {
                                this.f$0 = defaultDrmSessionManagerBuild;
                                this.f$1 = defaultDrmSessionManagerProvider;
                            }

                            @Override // androidx.media3.exoplayer.drm.DrmSessionManagerProvider
                            public final DrmSessionManager get(MediaItem mediaItem) {
                                return ChannelTester.TestSession.lambda$testOnce$0(this.f$0, this.f$1, mediaItem);
                            }
                        });
                    }
                    ExoPlayer exoPlayerBuild = new ExoPlayer.Builder(this.context).setMediaSourceFactory(defaultMediaSourceFactory).build();
                    try {
                        atomicReference.set(exoPlayerBuild);
                        exoPlayerBuild.addListener(new Player.Listener(this, channel, atomicReference2, countDownLatch, atomicReference3) { // from class: com.playtv.premium.ChannelTester.TestSession.1
                            private static short[] C = {-29492, -29503, -29495, -29504, -19078, -19098, -19093, -19085, -19074, -19076, -19083, -19074, -19089, -19079, -19074, -24705, -24705, -25533, -25584, -25577, -25598, -25577, -25594, -25506, -31180, -31197, -31184, -31184, -31181, -31196, -31169, -31176, -31183, -31474, -31463, -31459, -31464, -31483, -25691, -25682, -25692, -25691, -25692, -24829, 27872, 27898, 31902, 31874, 31887, 31895, 31898, 31896, 31889, 31898, 31883, 31901, 31898, 28831, 28831, 28204, 28233, 28254, 28254, 28227, 28254, 28214, 28204};
                            final TestSession this$0;
                            final Channel val$channel;
                            final AtomicReference val$errorRef;
                            final AtomicReference val$finalState;
                            final CountDownLatch val$latch;

                            {
                                this.this$0 = this;
                                this.val$channel = channel;
                                this.val$finalState = atomicReference2;
                                this.val$latch = countDownLatch;
                                this.val$errorRef = atomicReference3;
                            }

                            private static String C(int i, int i2, int i3) {
                                char[] cArr = new char[i2 - i];
                                for (int i4 = 0; i4 < i2 - i; i4++) {
                                    cArr[i4] = (char) (C[i + i4] ^ i3);
                                }
                                return new String(cArr);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onAudioAttributesChanged(AudioAttributes audioAttributes) {
                                Player.Listener.CC.$default$onAudioAttributesChanged(this, audioAttributes);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onAudioSessionIdChanged(int i) {
                                Player.Listener.CC.$default$onAudioSessionIdChanged(this, i);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onAvailableCommandsChanged(Player.Commands commands) {
                                Player.Listener.CC.$default$onAvailableCommandsChanged(this, commands);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onCues(CueGroup cueGroup) {
                                Player.Listener.CC.$default$onCues(this, cueGroup);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onCues(List list) {
                                Player.Listener.CC.$default$onCues(this, list);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onDeviceInfoChanged(DeviceInfo deviceInfo) {
                                Player.Listener.CC.$default$onDeviceInfoChanged(this, deviceInfo);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onDeviceVolumeChanged(int i, boolean z) {
                                Player.Listener.CC.$default$onDeviceVolumeChanged(this, i, z);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onEvents(Player player, Player.Events events) {
                                Player.Listener.CC.$default$onEvents(this, player, events);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onIsLoadingChanged(boolean z) {
                                Player.Listener.CC.$default$onIsLoadingChanged(this, z);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onIsPlayingChanged(boolean z) {
                                Player.Listener.CC.$default$onIsPlayingChanged(this, z);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onLoadingChanged(boolean z) {
                                Player.Listener.CC.$default$onLoadingChanged(this, z);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onMaxSeekToPreviousPositionChanged(long j) {
                                Player.Listener.CC.$default$onMaxSeekToPreviousPositionChanged(this, j);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onMediaItemTransition(MediaItem mediaItem, int i) {
                                Player.Listener.CC.$default$onMediaItemTransition(this, mediaItem, i);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
                                Player.Listener.CC.$default$onMediaMetadataChanged(this, mediaMetadata);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onMetadata(Metadata metadata) {
                                Player.Listener.CC.$default$onMetadata(this, metadata);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onPlayWhenReadyChanged(boolean z, int i) {
                                Player.Listener.CC.$default$onPlayWhenReadyChanged(this, z, i);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                                Player.Listener.CC.$default$onPlaybackParametersChanged(this, playbackParameters);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public void onPlaybackStateChanged(int i) {
                                String strC;
                                if (i == 1) {
                                    strC = C(0, 4, -29563);
                                } else if (i == 2) {
                                    strC = C(24, 33, -31114);
                                } else if (i == 3) {
                                    strC = C(33, 38, -31396);
                                } else {
                                    strC = i == 4 ? C(38, 43, -25632) : C(43, 44, -24772) + i;
                                }
                                Log.d(C(4, 15, -19158), C(15, 17, -24737) + this.val$channel.name + C(17, 24, -25501) + strC);
                                this.val$finalState.set(Integer.valueOf(i));
                                if (i == 3 || i == 4) {
                                    this.val$latch.countDown();
                                }
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onPlaybackSuppressionReasonChanged(int i) {
                                Player.Listener.CC.$default$onPlaybackSuppressionReasonChanged(this, i);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public void onPlayerError(PlaybackException playbackException) {
                                String message = (playbackException.getCause() == null || playbackException.getCause().getMessage() == null) ? playbackException.getMessage() : playbackException.getCause().getMessage();
                                String str2 = playbackException.getErrorCodeName() + (message != null ? C(44, 46, 27866) + message : "");
                                Log.e(C(46, 57, 31950), C(57, 59, 28863) + this.val$channel.name + C(59, 67, 28172) + str2);
                                this.val$errorRef.set(str2);
                                this.val$latch.countDown();
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onPlayerErrorChanged(PlaybackException playbackException) {
                                Player.Listener.CC.$default$onPlayerErrorChanged(this, playbackException);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onPlayerStateChanged(boolean z, int i) {
                                Player.Listener.CC.$default$onPlayerStateChanged(this, z, i);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onPlaylistMetadataChanged(MediaMetadata mediaMetadata) {
                                Player.Listener.CC.$default$onPlaylistMetadataChanged(this, mediaMetadata);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onPositionDiscontinuity(int i) {
                                Player.Listener.CC.$default$onPositionDiscontinuity(this, i);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onPositionDiscontinuity(Player.PositionInfo positionInfo, Player.PositionInfo positionInfo2, int i) {
                                Player.Listener.CC.$default$onPositionDiscontinuity(this, positionInfo, positionInfo2, i);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onRenderedFirstFrame() {
                                Player.Listener.CC.$default$onRenderedFirstFrame(this);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onRepeatModeChanged(int i) {
                                Player.Listener.CC.$default$onRepeatModeChanged(this, i);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onSeekBackIncrementChanged(long j) {
                                Player.Listener.CC.$default$onSeekBackIncrementChanged(this, j);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onSeekForwardIncrementChanged(long j) {
                                Player.Listener.CC.$default$onSeekForwardIncrementChanged(this, j);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onShuffleModeEnabledChanged(boolean z) {
                                Player.Listener.CC.$default$onShuffleModeEnabledChanged(this, z);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onSkipSilenceEnabledChanged(boolean z) {
                                Player.Listener.CC.$default$onSkipSilenceEnabledChanged(this, z);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onSurfaceSizeChanged(int i, int i2) {
                                Player.Listener.CC.$default$onSurfaceSizeChanged(this, i, i2);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onTimelineChanged(Timeline timeline, int i) {
                                Player.Listener.CC.$default$onTimelineChanged(this, timeline, i);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onTrackSelectionParametersChanged(TrackSelectionParameters trackSelectionParameters) {
                                Player.Listener.CC.$default$onTrackSelectionParametersChanged(this, trackSelectionParameters);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onTracksChanged(Tracks tracks) {
                                Player.Listener.CC.$default$onTracksChanged(this, tracks);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onVideoSizeChanged(VideoSize videoSize) {
                                Player.Listener.CC.$default$onVideoSizeChanged(this, videoSize);
                            }

                            @Override // androidx.media3.common.Player.Listener
                            public /* synthetic */ void onVolumeChanged(float f) {
                                Player.Listener.CC.$default$onVolumeChanged(this, f);
                            }
                        });
                        try {
                            try {
                                exoPlayerBuild.setMediaItem(ChannelTester.buildMediaItem(channel, str, mapChooseHeaders));
                                exoPlayerBuild.setPlayWhenReady(false);
                                exoPlayerBuild.prepare();
                                countDownLatch2.countDown();
                            } catch (Throwable th) {
                                th = th;
                                countDownLatch2.countDown();
                                throw th;
                            }
                        } catch (Exception e) {
                            exc = e;
                            String message = exc.getMessage();
                            if (message == null) {
                                message = exc.getClass().getSimpleName();
                            }
                            Log.e(N(440, 451, -17990), N(451, 453, -23860) + channel.name + N(453, 468, -22540) + message);
                            try {
                                atomicReference4.set(exc.getClass().getSimpleName() + N(468, 470, -18298) + message);
                                countDownLatch2.countDown();
                            } catch (Throwable th2) {
                                th = th2;
                                countDownLatch2.countDown();
                                throw th;
                            }
                        }
                    } catch (Exception e2) {
                        exc = e2;
                    } catch (Throwable th3) {
                        th = th3;
                        countDownLatch2.countDown();
                        throw th;
                    }
                } catch (Exception e3) {
                    exc = e3;
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (Exception e4) {
                exc = e4;
            } catch (Throwable th5) {
                th = th5;
            }
        }

        TestResult test(Channel channel, AppConfig appConfig) throws InterruptedException {
            TestResult testResultTestOnce = testOnce(channel, appConfig);
            if (testResultTestOnce.ok || testResultTestOnce.detail == null || !testResultTestOnce.detail.contains(N(470, 473, 21802)) || !FlowSigner.requiresSigning(channel, appConfig)) {
                return testResultTestOnce;
            }
            Log.d(N(473, 484, 22942), N(484, 491, 22485) + channel.name + N(491, 525, 25793));
            FlowTokenManager.refreshToken(appConfig, true);
            TestResult testResultTestOnce2 = testOnce(channel, appConfig);
            return testResultTestOnce2.ok ? new TestResult(testResultTestOnce2.channelName, testResultTestOnce2.category, testResultTestOnce2.globalIndex, true, N(525, 548, 23183), testResultTestOnce2.testedUrl) : testResultTestOnce2;
        }
    }

    private ChannelTester() {
    }

    private static String E(int i, int i2, int i3) {
        char[] cArr = new char[i2 - i];
        for (int i4 = 0; i4 < i2 - i; i4++) {
            cArr[i4] = (char) (E[i + i4] ^ i3);
        }
        return new String(cArr);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static MediaItem buildMediaItem(Channel channel, String str, Map<String, String> map) {
        MediaItem.Builder uri = new MediaItem.Builder().setUri(Uri.parse(str));
        String lowerCase = str.toLowerCase(Locale.US);
        if (lowerCase.contains(E(0, 5, -30054))) {
            uri.setMimeType(E(5, 26, -29590));
        } else if (lowerCase.contains(E(26, 30, -31953))) {
            uri.setMimeType(E(30, 50, -26887));
        }
        if (!channel.toClearKeyJson().isEmpty()) {
            uri.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).build());
        } else if (E(50, 58, -27043).equalsIgnoreCase(channel.type) && channel.drmLicenseUri != null && !channel.drmLicenseUri.isEmpty()) {
            uri.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID).setLicenseUri(channel.drmLicenseUri).setLicenseRequestHeaders(map).build());
        }
        return uri.build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Map<String, String> chooseHeaders(Channel channel, String str, AppConfig appConfig) {
        HashMap map = new HashMap();
        if (str.toLowerCase(Locale.US).contains(E(58, 63, -15369)) && !channel.headersM3u8.isEmpty()) {
            map.putAll(channel.headersM3u8);
        } else if (!channel.headers.isEmpty()) {
            map.putAll(channel.headers);
        }
        map.putIfAbsent(E(63, 73, -14529), appConfig.getOrDefault(E(73, 91, -1748), E(91, LocationRequestCompat.QUALITY_LOW_POWER, -1768)));
        return map;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TestResult fail(Channel channel, String str, String str2) {
        return new TestResult(channel.name, channel.category, channel.globalIndex, false, str, str2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void hydrateFlowHeaders(Channel channel, AppConfig appConfig) {
        if (channel.originalUrl == null) {
            return;
        }
        String lowerCase = channel.originalUrl.toLowerCase(Locale.US);
        if (lowerCase.contains(E(LocationRequestCompat.QUALITY_LOW_POWER, 117, -7450)) || lowerCase.contains(E(117, TsExtractor.TS_STREAM_TYPE_HDMV_DTS, -2817)) || lowerCase.contains(E(TsExtractor.TS_STREAM_TYPE_HDMV_DTS, 150, -8405)) || lowerCase.contains(E(150, 161, -6446)) || lowerCase.contains(E(161, TsExtractor.TS_STREAM_TYPE_AC4, -2011))) {
            boolean zContains = lowerCase.contains(E(TsExtractor.TS_STREAM_TYPE_AC4, 179, -3170));
            channel.headers.putIfAbsent(E(209, 215, -1564), zContains ? E(179, 209, -1473) : E(253, 283, -5412));
            channel.headers.putIfAbsent(E(246, 253, -4349), zContains ? E(215, 246, -10103) : E(283, 314, -8562));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TestResult ok(Channel channel, String str, String str2) {
        return new TestResult(channel.name, channel.category, channel.globalIndex, true, str, str2);
    }
}
