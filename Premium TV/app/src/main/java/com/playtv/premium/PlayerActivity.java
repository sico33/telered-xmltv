package com.playtv.premium;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.ui.PlayerView;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/* JADX INFO: loaded from: classes2.dex */
public class PlayerActivity extends Activity {
    private static final int MAX_403_RETRIES = 3;
    private static short[] v = {-30357, -30424, -30346, -30416, -30339, -24170, -24185, -24185, -24165, -24162, -24172, -24170, -24189, -24162, -24168, -24167, -24104, -24177, -24102, -24166, -24185, -24174, -24176, -24158, -24155, -24133, -29697, -29764, -29791, -29771, -23576, -23559, -23559, -23579, -23584, -23574, -23576, -23555, -23584, -23578, -23577, -23642, -23571, -23576, -23558, -23583, -23646, -23567, -23580, -23579, -32211, -32205, -32194, -32193, -32212, -32205, -32204, -32193, -12715, -12722, -12722, -12730, -12730, -12730, -12730, -12730, -12730, -15942, -15879, -15961, -15903, -15956, -9544, -9570, -9592, -9569, -9536, -9556, -9590, -9592, -9597, -9575, -14703, -14704, -14701, -14700, -14720, -14695, -14719, -14678, -14720, -14714, -14704, -14713, -14678, -14700, -14702, -14704, -14693, -14719, -9561, -9573, -9578, -9586, -9565, -9567, -9561, -9595, -9582, -9574, -9570, -9598, -9574, -15868, -15848, -15851, -15859, -15872, -15870, -15861, -15854, -15848, -15845, -15869, -3735, -3643, -3748, -3836, -3754, -3775, -3760, -3754, -3763, -3775, -3753, -3836, -3824, -3820, -3817, -3836, -3771, -3768, -3769, -3771, -3766, -3746, -3771, -3776, -3765, -3836, -3828, -3817, -3827, -3832, -3836, -3766, -3765, -3836, -3754, -3775, -3763, -3766, -3760, -3775, -3766, -3760, -3765, -15306, -15318, -15321, -15297, -15310, -15312, -15303, -15328, -15318, -15319, -15311, -14772, -14729, -14733, -14723, -14730, -14792, -14723, -14752, -14744, -14735, -14742, -14727, -14724, -14729, -14792, -14800, -14804, -14808, -14805, -14799, -14794, -14792, -14774, -14723, -14722, -14742, -14723, -14741, -14736, -14792, -14727, -14741, -14751, -14730, -14725, -14792, -14735, -14730, -14740, -14723, -14730, -14740, -14729, -14792, -14789, -3518, -3497, -3520, -3499, -3499, -3497, -3569, -3518, -3506, -3508, -3569, -3520, -3501, -176, -187, -174, -185, -185, -187, -227, -176, -164, -162, -227, -189, -182, -4044, -4063, -4042, -4061, -4061, -4063, -3975, -4048, -4046, -4061, -3974, -4047, -4046, -4038, -4040, -4039, -3975, -4039, -4046, -4061, -2, -12, -9, -17, -74, -5, -9, -11, -74, -7, -22, -3535, -3525, -3528, -3552, -3463, -3532, -3528, -3526, -3463, -3545, -3538, -5229, -5154, -5166, -5168, -5229, -5171, -5180, -2040, -2028, -2028, -2032, -2029, -1958, -1969, -1969, -2032, -2033, -2030, -2028, -2047, -2036, -1970, -2047, -2032, -2032, -1970, -2042, -2036, -2033, -2025, -1970, -2045, -2033, -2035, -1970, -2032, -2023, -425, -406, -399, -385, -399, -394, -7712, -7684, -7684, -7688, -7685, -7758, -7769, -7769, -7688, -7705, -7686, -7684, -7703, -7708, -7770, -7703, -7688, -7688, -7770, -7698, -7708, -7705, -7681, -7770, -7701, -7705, -7707, -7770, -7688, -7695, -7769, -5972, -5989, -5992, -5989, -6004, -5989, -6004, -7657, -7669, -7669, -7665, -7668, -7611, -7600, -7600, -7665, -7664, -7667, -7669, -7650, -7661, -7599, -7650, -7665, -7665, -7599, -7655, -7661, -7664, -7672, -7599, -7652, -7664, -7662, -7599, -7650, -7667, -7346, -7342, -7342, -7338, -7339, -7396, -7415, -7415, -7338, -7351, -7340, -7342, -7353, -7350, -7416, -7353, -7338, -7338, -7416, -7360, -7350, -7351, -7343, -7416, -7355, -7351, -7349, -7416, -7353, -7340, -7415, 10690, 10694, 10693, 9136, 9140, 9143, 31231, 31193, 31183, 31192, 31111, 31211, 31181, 31183, 31172, 31198, 28225, 28224, 28227, 28228, 28240, 28233, 28241, 28282, 28240, 28246, 28224, 28247, 28282, 28228, 28226, 28224, 28235, 28241, 30751, 30755, 30766, 30774, 30747, 30745, 30751, 30781, 30762, 30754, 30758, 30778, 30754, 24669, 24699, 24685, 24698, 24613, 24649, 24687, 24685, 24678, 24700, 27412, 27419, 27409, 27399, 27418, 27420, 27409, 27483, 27398, 27418, 27411, 27393, 27394, 27412, 27399, 27408, 27483, 27417, 27408, 27412, 27419, 27415, 27412, 27414, 27422, 3418, 3409, 3416, 3415, 3415, 3420, 3413, 3430, 3408, 3415, 3421, 3420, 3393, 30301, 30335, 30320, 30335, 30322, 30270, 25010, 25084, 25085, 25010, 25079, 25084, 25073, 25085, 25084, 25062, 25056, 25075, 25078, 25085, -20235, -20274, -20278, -20284, -20273, -20351, -20284, -20263, -20271, -20280, -20269, -20288, -20283, -20274, -20337, -20351, -20254, -20288, -20276, -20285, -20280, -20416, -20351, -20283, -20284, -20351, -20286, -20288, -20273, -20288, -20275, -20351, -20264, -20351, -20265, -20274, -20275, -20265, -20408, -20337, 23193, 23214, 23205, 23204, 23229, 23210, 23205, 23215, 23204, 23275, 23231, 23204, 23200, 23214, 23205, 23269, 23269, 23269, 6629, 6596, 6539, 6616, 6606, 6539, 6619, 6622, 6607, 6596, 6539, 6617, 6606, 6597, 6596, 6621, 6602, 6617, 6539, 6606, 6599, 6539, 6623, 6596, 6592, 6606, 6597, -18751, -18723, -18736, -18744, -18747, -18745, -18738, -18729, -18723, -18722, -18746, -26516, -26533, -26536, -26548, -26533, -26547, -26538, -26594, -26536, -26529, -26542, -26542, -26419, -26606, -26594, -26544, -26543, -26594, -26529, -26531, -26550, -26549, -26529, -26542, -26537, -26556, -26543, -26594, -26509, -26533, -26534, -26537, -26529, -26505, -26550, -26533, -26541, -19007, -18979, -18992, -19000, -19003, -19001, -18994, -18985, -18979, -18978, -19002, -23957, -23984, -24000, -23981, -23996, -24059, -23952, -23945, -23959, -24059, -23997, -23988, -23977, -23992, -23996, -23999, -23996, -24033, -24059, 1540, 1587, 1587, 1582, 1587, 1659, 1633, 25262, 25253, 25260, 25251, 25251, 25256, 25249, 25234, 25252, 25251, 25257, 25256, 25269, 21175, 21120, 21120, 21149, 21120, 21202, 21137, 21139, 21120, 21141, 21139, 21148, 21142, 21149, 21202, 21137, 21149, 21148, 21140, 21147, 21141};
    private List<Channel> allChannels;
    private AppConfig appConfig;
    private int currentChannelIndex;
    private Channel currentPlaybackChannel;
    private GestureDetector gestureDetector;
    private ChannelInfoOverlay infoOverlay;
    private ExoPlayer player;
    private PlayerView playerView;
    private ProgressBar progress;
    private TextView resolutionText;
    private SideMenuLayout rootLayout;
    private final Handler numberHandler = new Handler(Looper.getMainLooper());
    private final Handler controlsHandler = new Handler(Looper.getMainLooper());
    private final Handler resolutionHandler = new Handler(Looper.getMainLooper());
    private final StringBuilder pendingChannelNumber = new StringBuilder();
    private final Runnable tunePendingChannel = new Runnable(this) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda13
        public final PlayerActivity f$0;

        {
            this.f$0 = this;
        }

        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.tunePendingChannelNumber();
        }
    };
    private final Runnable hideControlsRunnable = new Runnable(this) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda14
        public final PlayerActivity f$0;

        {
            this.f$0 = this;
        }

        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.m280lambda$new$0$complaytvpremiumPlayerActivity();
        }
    };
    private final Runnable hideResolutionRunnable = new Runnable(this) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda15
        public final PlayerActivity f$0;

        {
            this.f$0 = this;
        }

        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.m281lambda$new$1$complaytvpremiumPlayerActivity();
        }
    };
    private int token403Retries = 0;

    /* JADX INFO: renamed from: com.playtv.premium.PlayerActivity$2, reason: invalid class name */
    class AnonymousClass2 implements Player.Listener {
        private static short[] w = {-20971, -20958, -20958, -20929, -20958, -20886, -20880, 30715, 30714, 30660, 30712, 30709, 30701, 30705, 30694, 30673, 30694, 30694, 30715, 30694, 30638, 30644, 21094, 21029, 21031, 21043, 21045, 21027, 21115, 30563, 30591, 30578, 30570, 30567, 30565, 30572, 30581, 30591, 30588, 30564, 31831, 31820, 31829, 31829};
        final PlayerActivity this$0;
        final AppConfig val$config;

        AnonymousClass2(PlayerActivity playerActivity, AppConfig appConfig) {
            this.this$0 = playerActivity;
            this.val$config = appConfig;
        }

        private static String w(int i, int i2, int i3) {
            char[] cArr = new char[i2 - i];
            for (int i4 = 0; i4 < i2 - i; i4++) {
                cArr[i4] = (char) (w[i + i4] ^ i3);
            }
            return new String(cArr);
        }

        /* JADX INFO: renamed from: lambda$onPlayerError$0$com-playtv-premium-PlayerActivity$2, reason: not valid java name */
        /* synthetic */ void m289lambda$onPlayerError$0$complaytvpremiumPlayerActivity$2(PlaybackException playbackException) {
            Toast.makeText(this.this$0, w(0, 7, -20912) + playbackException.getErrorCodeName(), 0).show();
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
            this.this$0.progress.setVisibility(i == 2 ? 0 : 8);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlaybackSuppressionReasonChanged(int i) {
            Player.Listener.CC.$default$onPlaybackSuppressionReasonChanged(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlayerError(final PlaybackException playbackException) {
            Log.e(w(29, 40, 30515), w(7, 22, 30612) + playbackException.getErrorCodeName() + w(22, 29, 21062) + (playbackException.getCause() != null ? playbackException.getCause().getMessage() : w(40, 44, 31801)));
            if (!this.this$0.is403Like(playbackException) || this.this$0.currentPlaybackChannel == null) {
                this.this$0.runOnUiThread(new Runnable(this, playbackException) { // from class: com.playtv.premium.PlayerActivity$2$$ExternalSyntheticLambda0
                    public final PlayerActivity.AnonymousClass2 f$0;
                    public final PlaybackException f$1;

                    {
                        this.f$0 = this;
                        this.f$1 = playbackException;
                    }

                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m289lambda$onPlayerError$0$complaytvpremiumPlayerActivity$2(this.f$1);
                    }
                });
            } else {
                this.this$0.handleTokenExpired(this.this$0.currentPlaybackChannel, this.val$config);
            }
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
        public void onVideoSizeChanged(VideoSize videoSize) {
            this.this$0.showResolution(videoSize.height);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onVolumeChanged(float f) {
            Player.Listener.CC.$default$onVolumeChanged(this, f);
        }
    }

    private void appendChannelDigit(int i) {
        if (this.pendingChannelNumber.length() >= 4) {
            this.pendingChannelNumber.setLength(0);
        }
        this.pendingChannelNumber.append(i);
        Toast.makeText(this, this.pendingChannelNumber.toString(), 0).show();
        this.numberHandler.removeCallbacks(this.tunePendingChannel);
        this.numberHandler.postDelayed(this.tunePendingChannel, 900L);
    }

    private MediaItem buildMediaItem(Channel channel, String str, Map<String, String> map) {
        MediaItem.Builder uri = new MediaItem.Builder().setUri(Uri.parse(str));
        String lowerCase = str.toLowerCase(Locale.US);
        if (lowerCase.contains(v(0, 5, -30395))) {
            uri.setMimeType(v(5, 26, -24073));
        } else if (lowerCase.contains(v(26, 30, -29743))) {
            uri.setMimeType(v(30, 50, -23671));
        }
        if (!channel.toClearKeyJson().isEmpty()) {
            uri.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).build());
        } else if (v(50, 58, -32134).equalsIgnoreCase(channel.type) && channel.drmLicenseUri != null && !channel.drmLicenseUri.isEmpty()) {
            uri.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID).setLicenseUri(channel.drmLicenseUri).setLicenseRequestHeaders(map).build());
        }
        return uri.build();
    }

    private void buildUi() {
        this.playerView = new PlayerView(this);
        this.playerView.setKeepScreenOn(true);
        this.playerView.setControllerAutoShow(false);
        this.playerView.setControllerShowTimeoutMs(0);
        this.playerView.setControllerHideOnTouch(false);
        this.resolutionText = new TextView(this);
        this.resolutionText.setTextColor(-1);
        this.resolutionText.setTextSize(18.0f);
        this.resolutionText.setBackgroundColor(Color.parseColor(v(58, 67, -12682)));
        this.resolutionText.setPadding(18, 8, 18, 8);
        this.resolutionText.setVisibility(8);
        this.progress = new ProgressBar(this);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(96, 96);
        layoutParams.gravity = 17;
        this.infoOverlay = new ChannelInfoOverlay(this);
        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(-2, -2);
        layoutParams2.gravity = 8388661;
        layoutParams2.setMargins(0, 40, 40, 0);
        FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(-2, -2);
        layoutParams3.gravity = 8388659;
        layoutParams3.setMargins(40, 40, 0, 0);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.addView(this.playerView, new FrameLayout.LayoutParams(-1, -1));
        frameLayout.addView(this.progress, layoutParams);
        frameLayout.addView(this.resolutionText, layoutParams3);
        frameLayout.addView(this.infoOverlay, layoutParams2);
        this.rootLayout = new SideMenuLayout(this, frameLayout);
        setupSideMenu();
        setContentView(this.rootLayout);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void changeChannel(int i) {
        if (this.allChannels.isEmpty()) {
            return;
        }
        hidePlaybackControls();
        this.currentChannelIndex = ((this.currentChannelIndex + i) + this.allChannels.size()) % this.allChannels.size();
        startPlayback();
    }

    private Map<String, String> chooseHeaders(Channel channel, String str, AppConfig appConfig) {
        HashMap map = new HashMap();
        if (str.toLowerCase(Locale.US).contains(v(67, 72, -15980)) && !channel.headersM3u8.isEmpty()) {
            map.putAll(channel.headersM3u8);
        } else if (!channel.headers.isEmpty()) {
            map.putAll(channel.headers);
        }
        map.putIfAbsent(v(72, 82, -9491), appConfig.getOrDefault(v(82, 100, -14603), v(100, 113, -9481)));
        return map;
    }

    private int digitForKeyCode(int i) {
        if (i >= 7 && i <= 16) {
            return i - 7;
        }
        if (i < 144 || i > 153) {
            return -1;
        }
        return i - 144;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleTokenExpired(final Channel channel, final AppConfig appConfig) {
        if (this.token403Retries >= 3) {
            Log.e(v(113, 124, -15788), v(124, 167, -3804));
            runOnUiThread(new Runnable(this) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda7
                public final PlayerActivity f$0;

                {
                    this.f$0 = this;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m275lambda$handleTokenExpired$10$complaytvpremiumPlayerActivity();
                }
            });
        } else {
            this.token403Retries++;
            Log.d(v(167, 178, -15258), v(178, 223, -14824) + this.token403Retries);
            runOnUiThread(new Runnable(this) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda8
                public final PlayerActivity f$0;

                {
                    this.f$0 = this;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m276lambda$handleTokenExpired$11$complaytvpremiumPlayerActivity();
                }
            });
            FlowTokenManager.refreshAsync(appConfig, new FlowTokenManager.RefreshCallback(this, channel, appConfig) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda9
                public final PlayerActivity f$0;
                public final Channel f$1;
                public final AppConfig f$2;

                {
                    this.f$0 = this;
                    this.f$1 = channel;
                    this.f$2 = appConfig;
                }

                @Override // com.playtv.premium.FlowTokenManager.RefreshCallback
                public final void onRefreshed(FlowTokenManager.TokenInfo tokenInfo) {
                    this.f$0.m279lambda$handleTokenExpired$14$complaytvpremiumPlayerActivity(this.f$1, this.f$2, tokenInfo);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hidePlaybackControls() {
        this.controlsHandler.removeCallbacks(this.hideControlsRunnable);
        this.playerView.hideController();
        this.playerView.requestFocus();
    }

    private void hideResolution() {
        this.resolutionHandler.removeCallbacks(this.hideResolutionRunnable);
        this.resolutionText.setVisibility(8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideSystemBars() {
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setSystemBarsBehavior(2);
        insetsController.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void hydrateFlowHeaders(Channel channel) {
        if (channel.originalUrl == null) {
            return;
        }
        if (channel.originalUrl.contains(v(223, 236, -3551)) || channel.originalUrl.contains(v(236, 249, -205)) || channel.originalUrl.contains(v(249, 269, -4009)) || channel.originalUrl.contains(v(269, 280, PlaybackException.ERROR_CODE_CONCURRENT_STREAM_LIMIT)) || channel.originalUrl.contains(v(280, 291, -3497))) {
            boolean zContains = channel.originalUrl.contains(v(291, 298, -5187));
            channel.headers.putIfAbsent(v(328, 334, -488), zContains ? v(298, 328, -1952) : v(372, 402, -7553));
            channel.headers.putIfAbsent(v(365, 372, -5890), zContains ? v(334, 365, -7800) : v(402, 433, -7386));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean is403Like(PlaybackException playbackException) {
        String message;
        if (playbackException == null) {
            return false;
        }
        int i = playbackException.errorCode;
        if (i == 2004 || i == 2004 || i == 1002) {
            return true;
        }
        Throwable cause = playbackException.getCause();
        if (cause != null && (message = cause.getMessage()) != null && message.contains(v(433, 436, 10742))) {
            return true;
        }
        String message2 = playbackException.getMessage();
        return message2 != null && message2.contains(v(436, 439, 9092));
    }

    static /* synthetic */ DrmSessionManager lambda$preparePlayer$9(DrmSessionManager drmSessionManager, DrmSessionManagerProvider drmSessionManagerProvider, MediaItem mediaItem) {
        MediaItem.DrmConfiguration drmConfiguration = mediaItem.localConfiguration != null ? mediaItem.localConfiguration.drmConfiguration : null;
        return (drmConfiguration == null || !C.CLEARKEY_UUID.equals(drmConfiguration.scheme)) ? drmSessionManagerProvider.get(mediaItem) : drmSessionManager;
    }

    private void preparePlayer(Channel channel, String str, AppConfig appConfig) {
        if (this.player != null) {
            this.player.release();
        }
        Map<String, String> mapChooseHeaders = chooseHeaders(channel, str, appConfig);
        DefaultHttpDataSource.Factory userAgent = new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).setUserAgent(mapChooseHeaders.getOrDefault(v(439, 449, 31146), appConfig.getOrDefault(v(449, 467, 28197), v(467, 480, 30799))));
        HashMap map = new HashMap(mapChooseHeaders);
        map.remove(v(480, 490, 24584));
        userAgent.setDefaultRequestProperties((Map<String, String>) map);
        DefaultMediaSourceFactory defaultMediaSourceFactory = new DefaultMediaSourceFactory(new DefaultDataSource.Factory(this, userAgent));
        String clearKeyJson = channel.toClearKeyJson();
        if (!clearKeyJson.isEmpty()) {
            final DefaultDrmSessionManager defaultDrmSessionManagerBuild = new DefaultDrmSessionManager.Builder().setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER).build(new LocalMediaDrmCallback(clearKeyJson.getBytes(StandardCharsets.UTF_8)));
            final DefaultDrmSessionManagerProvider defaultDrmSessionManagerProvider = new DefaultDrmSessionManagerProvider();
            defaultMediaSourceFactory.setDrmSessionManagerProvider(new DrmSessionManagerProvider(defaultDrmSessionManagerBuild, defaultDrmSessionManagerProvider) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda2
                public final DrmSessionManager f$0;
                public final DrmSessionManagerProvider f$1;

                {
                    this.f$0 = defaultDrmSessionManagerBuild;
                    this.f$1 = defaultDrmSessionManagerProvider;
                }

                @Override // androidx.media3.exoplayer.drm.DrmSessionManagerProvider
                public final DrmSessionManager get(MediaItem mediaItem) {
                    return PlayerActivity.lambda$preparePlayer$9(this.f$0, this.f$1, mediaItem);
                }
            });
        }
        this.player = new ExoPlayer.Builder(this).setRenderersFactory(new DeinterlaceRenderersFactory(this)).setMediaSourceFactory(defaultMediaSourceFactory).build();
        this.player.setTrackSelectionParameters(this.player.getTrackSelectionParameters().buildUpon().setMaxVideoSize(Integer.MAX_VALUE, Integer.MAX_VALUE).setMaxVideoBitrate(Integer.MAX_VALUE).setForceHighestSupportedBitrate(true).build());
        this.player.addAnalyticsListener(new EventLogger());
        boolean zHasSystemFeature = getPackageManager().hasSystemFeature(v(490, 515, 27509));
        ExoPlayer exoPlayer = this.player;
        if (zHasSystemFeature) {
            exoPlayer.setVideoChangeFrameRateStrategy(0);
        } else {
            exoPlayer.setVideoChangeFrameRateStrategy(Integer.MIN_VALUE);
        }
        this.player.addListener(new AnonymousClass2(this, appConfig));
        this.playerView.setPlayer(this.player);
        hidePlaybackControls();
        this.player.setMediaItem(buildMediaItem(channel, str, mapChooseHeaders));
        this.player.setPlayWhenReady(true);
        this.player.prepare();
        hidePlaybackControls();
    }

    private void resetControlsTimeout() {
        this.controlsHandler.removeCallbacks(this.hideControlsRunnable);
        this.controlsHandler.postDelayed(this.hideControlsRunnable, 5000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void returnToMainMenu() {
        Intent intent = new Intent();
        intent.putExtra(v(515, 528, 3385), this.currentChannelIndex);
        setResult(-1, intent);
        finish();
    }

    private void setupGestures() {
        this.gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(this) { // from class: com.playtv.premium.PlayerActivity.1
            final PlayerActivity this$0;

            {
                this.this$0 = this;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
                if (motionEvent == null) {
                    return false;
                }
                float y = motionEvent2.getY() - motionEvent.getY();
                float x = motionEvent2.getX() - motionEvent.getX();
                if (Math.abs(x) <= Math.abs(y)) {
                    if (y < -100.0f) {
                        this.this$0.changeChannel(1);
                        return true;
                    }
                    if (y <= 100.0f) {
                        return false;
                    }
                    this.this$0.changeChannel(-1);
                    return true;
                }
                if (x < -100.0f) {
                    this.this$0.hidePlaybackControls();
                    this.this$0.returnToMainMenu();
                    return true;
                }
                if (x <= 100.0f) {
                    return false;
                }
                boolean zIsMenuOpen = this.this$0.rootLayout.isMenuOpen();
                PlayerActivity playerActivity = this.this$0;
                if (zIsMenuOpen) {
                    playerActivity.rootLayout.closeMenu();
                    this.this$0.hideSystemBars();
                } else {
                    playerActivity.showPlaybackControls();
                }
                return true;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                return true;
            }
        });
        this.playerView.setOnTouchListener(new View.OnTouchListener(this) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda6
            public final PlayerActivity f$0;

            {
                this.f$0 = this;
            }

            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return this.f$0.m282lambda$setupGestures$3$complaytvpremiumPlayerActivity(view, motionEvent);
            }
        });
    }

    private void setupSideMenu() {
        ArrayList arrayList = new ArrayList();
        Iterator<Channel> it = this.allChannels.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().name);
        }
        this.rootLayout.menuList.setAdapter((ListAdapter) new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList));
        this.rootLayout.menuList.setOnItemClickListener(new AdapterView.OnItemClickListener(this) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda5
            public final PlayerActivity f$0;

            {
                this.f$0 = this;
            }

            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                this.f$0.m283lambda$setupSideMenu$2$complaytvpremiumPlayerActivity(adapterView, view, i, j);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showPlaybackControls() {
        if (this.rootLayout.isMenuOpen()) {
            return;
        }
        this.playerView.showController();
        this.playerView.requestFocus();
        resetControlsTimeout();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showResolution(int i) {
        if (i <= 0) {
            return;
        }
        this.resolutionText.setText(String.valueOf(i));
        this.resolutionText.setVisibility(0);
        this.resolutionHandler.removeCallbacks(this.hideResolutionRunnable);
        this.resolutionHandler.postDelayed(this.hideResolutionRunnable, ExoPlayer.DEFAULT_DETACH_SURFACE_TIMEOUT_MS);
    }

    private void startPlayback() {
        runOnUiThread(new Runnable(this) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda10
            public final PlayerActivity f$0;

            {
                this.f$0 = this;
            }

            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m284lambda$startPlayback$4$complaytvpremiumPlayerActivity();
            }
        });
        final Channel channel = this.allChannels.get(this.currentChannelIndex);
        this.currentPlaybackChannel = channel;
        this.token403Retries = 0;
        runOnUiThread(new Runnable(this, channel) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda11
            public final PlayerActivity f$0;
            public final Channel f$1;

            {
                this.f$0 = this;
                this.f$1 = channel;
            }

            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m285lambda$startPlayback$5$complaytvpremiumPlayerActivity(this.f$1);
            }
        });
        new Thread(new Runnable(this, channel) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda12
            public final PlayerActivity f$0;
            public final Channel f$1;

            {
                this.f$0 = this;
                this.f$1 = channel;
            }

            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m288lambda$startPlayback$8$complaytvpremiumPlayerActivity(this.f$1);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tunePendingChannelNumber() {
        int i;
        if (this.pendingChannelNumber.length() == 0 || this.allChannels.isEmpty()) {
            return;
        }
        try {
            int i2 = Integer.parseInt(this.pendingChannelNumber.toString());
            this.pendingChannelNumber.setLength(0);
            int i3 = 0;
            while (true) {
                if (i3 >= this.allChannels.size()) {
                    i = -1;
                    break;
                } else {
                    if (this.allChannels.get(i3).globalIndex == i2) {
                        i = i3;
                        break;
                    }
                    i3++;
                }
            }
            if (i < 0 && i2 >= 1 && i2 <= this.allChannels.size()) {
                i = i2 - 1;
            }
            if (i < 0) {
                Toast.makeText(this, v(528, 534, 30238) + i2 + v(534, 548, 24978), 0).show();
            } else {
                this.currentChannelIndex = i;
                startPlayback();
            }
        } catch (NumberFormatException e) {
            this.pendingChannelNumber.setLength(0);
        }
    }

    private static String v(int i, int i2, int i3) {
        char[] cArr = new char[i2 - i];
        for (int i4 = 0; i4 < i2 - i; i4++) {
            cArr[i4] = (char) (v[i + i4] ^ i3);
        }
        return new String(cArr);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == 0) {
            if (this.playerView.isControllerFullyVisible()) {
                resetControlsTimeout();
            }
            int iDigitForKeyCode = digitForKeyCode(keyEvent.getKeyCode());
            if (iDigitForKeyCode >= 0) {
                appendChannelDigit(iDigitForKeyCode);
                return true;
            }
            switch (keyEvent.getKeyCode()) {
                case 4:
                    if (this.rootLayout.isMenuOpen()) {
                        this.rootLayout.closeMenu();
                        hideSystemBars();
                        return true;
                    }
                    if (this.playerView.isControllerFullyVisible()) {
                        hidePlaybackControls();
                        return true;
                    }
                    break;
                case 19:
                    changeChannel(-1);
                    return true;
                case 20:
                    changeChannel(1);
                    return true;
                case 21:
                    if (!this.rootLayout.isMenuOpen()) {
                        hidePlaybackControls();
                        returnToMainMenu();
                        return true;
                    }
                    break;
                case 22:
                    if (!this.rootLayout.isMenuOpen()) {
                        showPlaybackControls();
                        return true;
                    }
                    this.rootLayout.closeMenu();
                    hideSystemBars();
                    return true;
            }
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    /* JADX INFO: renamed from: lambda$handleTokenExpired$10$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m275lambda$handleTokenExpired$10$complaytvpremiumPlayerActivity() {
        Toast.makeText(this, v(548, 588, -20319), 1).show();
    }

    /* JADX INFO: renamed from: lambda$handleTokenExpired$11$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m276lambda$handleTokenExpired$11$complaytvpremiumPlayerActivity() {
        this.progress.setVisibility(0);
        Toast.makeText(this, v(588, 606, 23243), 0).show();
    }

    /* JADX INFO: renamed from: lambda$handleTokenExpired$12$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m277lambda$handleTokenExpired$12$complaytvpremiumPlayerActivity() {
        this.progress.setVisibility(8);
        Toast.makeText(this, v(606, 633, 6571), 0).show();
    }

    /* JADX INFO: renamed from: lambda$handleTokenExpired$13$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m278lambda$handleTokenExpired$13$complaytvpremiumPlayerActivity(Channel channel, String str, AppConfig appConfig) {
        if (this.player == null || this.currentPlaybackChannel != channel) {
            return;
        }
        this.player.setMediaItem(buildMediaItem(channel, str, chooseHeaders(channel, str, appConfig)));
        this.player.setPlayWhenReady(true);
        this.player.prepare();
    }

    /* JADX INFO: renamed from: lambda$handleTokenExpired$14$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m279lambda$handleTokenExpired$14$complaytvpremiumPlayerActivity(final Channel channel, final AppConfig appConfig, FlowTokenManager.TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            Log.e(v(633, 644, -18799), v(644, 681, -26562));
            runOnUiThread(new Runnable(this) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda0
                public final PlayerActivity f$0;

                {
                    this.f$0 = this;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m277lambda$handleTokenExpired$12$complaytvpremiumPlayerActivity();
                }
            });
        } else {
            final String strResolvePlayableUrl = FlowSigner.resolvePlayableUrl(channel, appConfig);
            Log.d(v(681, 692, -19055), v(692, 711, -24027) + strResolvePlayableUrl);
            runOnUiThread(new Runnable(this, channel, strResolvePlayableUrl, appConfig) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda1
                public final PlayerActivity f$0;
                public final Channel f$1;
                public final String f$2;
                public final AppConfig f$3;

                {
                    this.f$0 = this;
                    this.f$1 = channel;
                    this.f$2 = strResolvePlayableUrl;
                    this.f$3 = appConfig;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m278lambda$handleTokenExpired$13$complaytvpremiumPlayerActivity(this.f$1, this.f$2, this.f$3);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$new$0$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m280lambda$new$0$complaytvpremiumPlayerActivity() {
        if (this.playerView != null) {
            this.playerView.hideController();
            this.playerView.requestFocus();
        }
    }

    /* JADX INFO: renamed from: lambda$new$1$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m281lambda$new$1$complaytvpremiumPlayerActivity() {
        if (this.resolutionText != null) {
            this.resolutionText.setVisibility(8);
        }
    }

    /* JADX INFO: renamed from: lambda$setupGestures$3$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ boolean m282lambda$setupGestures$3$complaytvpremiumPlayerActivity(View view, MotionEvent motionEvent) {
        boolean zIsControllerFullyVisible = this.playerView.isControllerFullyVisible();
        GestureDetector gestureDetector = this.gestureDetector;
        if (!zIsControllerFullyVisible) {
            gestureDetector.onTouchEvent(motionEvent);
            return true;
        }
        gestureDetector.onTouchEvent(motionEvent);
        resetControlsTimeout();
        return false;
    }

    /* JADX INFO: renamed from: lambda$setupSideMenu$2$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m283lambda$setupSideMenu$2$complaytvpremiumPlayerActivity(AdapterView adapterView, View view, int i, long j) {
        this.currentChannelIndex = i;
        startPlayback();
        this.rootLayout.closeMenu();
        hideSystemBars();
    }

    /* JADX INFO: renamed from: lambda$startPlayback$4$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m284lambda$startPlayback$4$complaytvpremiumPlayerActivity() {
        this.progress.setVisibility(0);
        hidePlaybackControls();
        hideResolution();
    }

    /* JADX INFO: renamed from: lambda$startPlayback$5$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m285lambda$startPlayback$5$complaytvpremiumPlayerActivity(Channel channel) {
        this.infoOverlay.show(channel);
    }

    /* JADX INFO: renamed from: lambda$startPlayback$6$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m286lambda$startPlayback$6$complaytvpremiumPlayerActivity(Channel channel, String str) {
        preparePlayer(channel, str, this.appConfig);
    }

    /* JADX INFO: renamed from: lambda$startPlayback$7$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m287lambda$startPlayback$7$complaytvpremiumPlayerActivity(Exception exc) {
        this.progress.setVisibility(8);
        Toast.makeText(this, v(711, 718, 1601) + exc.getMessage(), 0).show();
    }

    /* JADX INFO: renamed from: lambda$startPlayback$8$com-playtv-premium-PlayerActivity, reason: not valid java name */
    /* synthetic */ void m288lambda$startPlayback$8$complaytvpremiumPlayerActivity(final Channel channel) {
        try {
            hydrateFlowHeaders(channel);
            final String strResolvePlayableUrl = FlowSigner.resolvePlayableUrl(channel, this.appConfig);
            runOnUiThread(new Runnable(this, channel, strResolvePlayableUrl) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda3
                public final PlayerActivity f$0;
                public final Channel f$1;
                public final String f$2;

                {
                    this.f$0 = this;
                    this.f$1 = channel;
                    this.f$2 = strResolvePlayableUrl;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m286lambda$startPlayback$6$complaytvpremiumPlayerActivity(this.f$1, this.f$2);
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable(this, e) { // from class: com.playtv.premium.PlayerActivity$$ExternalSyntheticLambda4
                public final PlayerActivity f$0;
                public final Exception f$1;

                {
                    this.f$0 = this;
                    this.f$1 = e;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m287lambda$startPlayback$7$complaytvpremiumPlayerActivity(this.f$1);
                }
            });
        }
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(128);
        hideSystemBars();
        this.allChannels = CatalogState.getAllChannels();
        this.currentChannelIndex = getIntent().getIntExtra(v(718, 731, 25293), 0);
        try {
            this.appConfig = AppConfig.load(this);
            buildUi();
            setupGestures();
            startPlayback();
        } catch (Exception e) {
            Toast.makeText(this, v(731, 752, 21234), 0).show();
            finish();
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        if (this.player != null) {
            this.player.setVideoChangeFrameRateStrategy(Integer.MIN_VALUE);
            this.player.release();
            this.player = null;
        }
        this.numberHandler.removeCallbacks(this.tunePendingChannel);
        this.controlsHandler.removeCallbacks(this.hideControlsRunnable);
        this.resolutionHandler.removeCallbacks(this.hideResolutionRunnable);
    }
}
