package com.playtv.premium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.AudioAttributes;
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
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.ui.PlayerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import native_playtv0.PlayTV;
import native_playtv0.hidden.Hidden0;

/* JADX INFO: loaded from: classes2.dex */
public class MainActivity extends Activity {
    private static final String ADULTS = "ADULTOS";
    private static final String ADULTS_PASSWORD = null;
    private static final String ALL_CHANNELS = null;
    private static final String APK_URL = null;
    private static final String PREFS_NAME = "playtv_prefs";
    private static final int PREVIEW_MAX_403_RETRIES = 2;
    private static final String RELEASE_TXT_URL = null;
    private static final String TAG = "PLAYTV";
    private static long doubleback;
    private AppConfig appConfig;
    private ListView categoryList;
    private View channelInfoOverlay;
    private ListView channelList;
    private Channel currentPreviewChannel;
    private GestureDetector gestureDetector;
    private ImageButton lockIconBtn;
    private ImageView overlayChannelLogo;
    private TextView overlayChannelName;
    private TextView overlayResolution;
    private FrameLayout previewContainer;
    private ExoPlayer previewPlayer;
    private ProgressBar previewProgress;
    private PlayerView previewView;
    private ProgressBar progress;
    private ImageButton searchIconBtn;
    private LinearLayout selectionLayout;
    private TextView title;
    private final List<Category> categories = new ArrayList();
    private final List<Category> displayCategories = new ArrayList();
    private int selectedCategoryIndex = 0;
    private int selectedChannelIndex = -1;
    private int previewRequestId = 0;
    private int preview403Retries = 0;
    private boolean isFullscreen = false;
    private boolean isAdultAccessGranted = false;
    private boolean controlsVisible = false;
    private final Handler controlsHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideOverlayRunnable = new Runnable(this) { // from class: com.playtv.premium.MainActivity$$ExternalSyntheticLambda4
        public final MainActivity f$0;

        {
            this.f$0 = this;
        }

        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.m251lambda$new$0$complaytvpremiumMainActivity();
        }
    };

    /* JADX INFO: renamed from: com.playtv.premium.MainActivity$1, reason: invalid class name */
    class AnonymousClass1 extends GestureDetector.SimpleOnGestureListener {
        private static final float AXIS_LOCK = 1.5f;
        private static final int SWIPE_MIN = 80;
        final MainActivity this$0;

        AnonymousClass1(MainActivity mainActivity) {
            this.this$0 = mainActivity;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (!MainActivity.m223$$Nest$fgetisFullscreen(this.this$0) || MainActivity.m221$$Nest$fgetcontrolsVisible(this.this$0)) {
                return false;
            }
            float x = motionEvent2.getX() - motionEvent.getX();
            float y = motionEvent2.getY() - motionEvent.getY();
            float fAbs = Math.abs(x);
            float fAbs2 = Math.abs(y);
            if (fAbs < 80.0f && fAbs2 < 80.0f) {
                return false;
            }
            if (fAbs > fAbs2 * AXIS_LOCK) {
                MainActivity mainActivity = this.this$0;
                if (x > 0.0f) {
                    MainActivity.m233$$Nest$mexitFullscreen(mainActivity);
                } else {
                    MainActivity.m238$$Nest$mshowPlayerControls(mainActivity);
                }
            } else if (fAbs2 > AXIS_LOCK * fAbs) {
                MainActivity mainActivity2 = this.this$0;
                if (y < 0.0f) {
                    MainActivity.m229$$Nest$mchangeFullscreenChannel(mainActivity2, 1);
                } else {
                    MainActivity.m229$$Nest$mchangeFullscreenChannel(mainActivity2, -1);
                }
            }
            MainActivity.m234$$Nest$mhideSystemBars(this.this$0);
            return true;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return true;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            if (MainActivity.m223$$Nest$fgetisFullscreen(this.this$0)) {
                return false;
            }
            MainActivity.m232$$Nest$menterFullscreen(this.this$0);
            return true;
        }
    }

    /* JADX INFO: renamed from: com.playtv.premium.MainActivity$2, reason: invalid class name */
    class AnonymousClass2 implements AdapterView.OnItemSelectedListener {
        final MainActivity this$0;

        AnonymousClass2(MainActivity mainActivity) {
            this.this$0 = mainActivity;
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
            MainActivity.m236$$Nest$mselectCategory(this.this$0, i);
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    /* JADX INFO: renamed from: com.playtv.premium.MainActivity$3, reason: invalid class name */
    class AnonymousClass3 implements AdapterView.OnItemSelectedListener {
        final MainActivity this$0;

        AnonymousClass3(MainActivity mainActivity) {
            this.this$0 = mainActivity;
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
            if (MainActivity.m220$$Nest$fgetchannelList(this.this$0).hasFocus()) {
                MainActivity.m237$$Nest$mselectChannel(this.this$0, i);
            }
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    /* JADX INFO: renamed from: com.playtv.premium.MainActivity$4, reason: invalid class name */
    class AnonymousClass4 implements TextWatcher {
        final MainActivity this$0;
        final ChannelAdapter val$adapter;
        final List val$base;
        final List val$current;

        AnonymousClass4(MainActivity mainActivity, List list, List list2, ChannelAdapter channelAdapter) {
            this.this$0 = mainActivity;
            this.val$current = list;
            this.val$base = list2;
            this.val$adapter = channelAdapter;
        }

        @Override // android.text.TextWatcher
        public void afterTextChanged(Editable editable) {
            String lowerCase = editable.toString().trim().toLowerCase(Locale.US);
            this.val$current.clear();
            if (lowerCase.isEmpty()) {
                this.val$current.addAll(this.val$base);
            } else {
                for (Channel channel : this.val$base) {
                    if (channel.name != null && channel.name.toLowerCase(Locale.US).contains(lowerCase)) {
                        this.val$current.add(channel);
                    }
                }
            }
            this.val$adapter.notifyDataSetChanged();
        }

        @Override // android.text.TextWatcher
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override // android.text.TextWatcher
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }
    }

    /* JADX INFO: renamed from: com.playtv.premium.MainActivity$5, reason: invalid class name */
    class AnonymousClass5 implements Player.Listener {
        private static short[] g = {26329, 26363, 26348, 26367, 26336, 26348, 26366, 26281, 26342, 26343, 26329, 26341, 26344, 26352, 26348, 26363, 26316, 26363, 26363, 26342, 26363, 26291, 26281, 27362, 27297, 27299, 27319, 27313, 27303, 27391, 26609, 26605, 26592, 26616, 26613, 26615, 32363, 32375, 32378, 32354, 32367, 32365, 31991, 31957, 31938, 31953, 31950, 31938, 31952, 31879, 31891, 31895, 31892, 31901, 31879, 31957, 31938, 31937, 31957, 31938, 31956, 31951, 31879, 31942, 31956, 31966, 31945, 31940, 31879, 31950, 31945, 31955, 31938, 31945, 31955, 31944, 31879, 31876, 30517, 30510, 30519, 30519, 21608, 21599, 21599, 21570, 21599, 21517, 21577, 21576, 21517, 21599, 21576, 21597, 21599, 21570, 21577, 21592, 21582, 21582, 21572, 21726, 21571, 21527, 21517};
        final MainActivity this$0;
        final Channel val$channel;

        AnonymousClass5(MainActivity mainActivity, Channel channel) {
            this.this$0 = mainActivity;
            this.val$channel = channel;
        }

        private static String g(int i, int i2, int i3) {
            char[] cArr = new char[i2 - i];
            for (int i4 = 0; i4 < i2 - i; i4++) {
                cArr[i4] = (char) (g[i + i4] ^ i3);
            }
            return new String(cArr);
        }

        /* JADX INFO: renamed from: lambda$onPlayerError$0$com-playtv-premium-MainActivity$5, reason: not valid java name */
        /* synthetic */ void m261lambda$onPlayerError$0$complaytvpremiumMainActivity$5(Channel channel, String str) {
            if (MainActivity.m225$$Nest$fgetpreviewPlayer(this.this$0) == null || MainActivity.m222$$Nest$fgetcurrentPreviewChannel(this.this$0) != channel) {
                return;
            }
            MainActivity.m225$$Nest$fgetpreviewPlayer(this.this$0).setMediaItem(MainActivity.m228$$Nest$mbuildMediaItem(this.this$0, channel, str, MainActivity.m230$$Nest$mchooseHeaders(this.this$0, channel, str)));
            MainActivity.m225$$Nest$fgetpreviewPlayer(this.this$0).setPlayWhenReady(true);
            MainActivity.m225$$Nest$fgetpreviewPlayer(this.this$0).prepare();
        }

        /* JADX INFO: renamed from: lambda$onPlayerError$1$com-playtv-premium-MainActivity$5, reason: not valid java name */
        /* synthetic */ void m262lambda$onPlayerError$1$complaytvpremiumMainActivity$5(final Channel channel, FlowTokenManager.TokenInfo tokenInfo) {
            if (tokenInfo == null) {
                return;
            }
            final String strResolvePlayableUrl = FlowSigner.resolvePlayableUrl(channel, MainActivity.m219$$Nest$fgetappConfig(this.this$0));
            this.this$0.runOnUiThread(new Runnable(this, channel, strResolvePlayableUrl) { // from class: com.playtv.premium.MainActivity$5$$ExternalSyntheticLambda1
                public final MainActivity.AnonymousClass5 f$0;
                public final Channel f$1;
                public final String f$2;

                {
                    this.f$0 = this;
                    this.f$1 = channel;
                    this.f$2 = strResolvePlayableUrl;
                }

                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m261lambda$onPlayerError$0$complaytvpremiumMainActivity$5(this.f$1, this.f$2);
                }
            });
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
            MainActivity.m226$$Nest$fgetpreviewProgress(this.this$0).setVisibility(i == 2 ? 0 : 8);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlaybackSuppressionReasonChanged(int i) {
            Player.Listener.CC.$default$onPlaybackSuppressionReasonChanged(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlayerError(PlaybackException playbackException) {
            Log.e(g(30, 36, 26529), g(0, 23, 26249) + playbackException.getErrorCodeName() + g(23, 30, 27330) + (playbackException.getCause() != null ? playbackException.getCause().getMessage() : g(78, 82, 30555)));
            if (!MainActivity.m235$$Nest$misPreview403Like(this.this$0, playbackException) || MainActivity.m222$$Nest$fgetcurrentPreviewChannel(this.this$0) != this.val$channel || MainActivity.m224$$Nest$fgetpreview403Retries(this.this$0) >= 2) {
                Toast.makeText(this.this$0, g(82, 105, 21549) + playbackException.getErrorCodeName(), 0).show();
                return;
            }
            MainActivity mainActivity = this.this$0;
            MainActivity.m227$$Nest$fputpreview403Retries(mainActivity, MainActivity.m224$$Nest$fgetpreview403Retries(mainActivity) + 1);
            Log.d(g(36, 42, 32315), g(42, 78, 31911) + MainActivity.m224$$Nest$fgetpreview403Retries(this.this$0));
            AppConfig appConfigM219$$Nest$fgetappConfig = MainActivity.m219$$Nest$fgetappConfig(this.this$0);
            final Channel channel = this.val$channel;
            FlowTokenManager.refreshAsync(appConfigM219$$Nest$fgetappConfig, new FlowTokenManager.RefreshCallback(this, channel) { // from class: com.playtv.premium.MainActivity$5$$ExternalSyntheticLambda0
                public final MainActivity.AnonymousClass5 f$0;
                public final Channel f$1;

                {
                    this.f$0 = this;
                    this.f$1 = channel;
                }

                @Override // com.playtv.premium.FlowTokenManager.RefreshCallback
                public final void onRefreshed(FlowTokenManager.TokenInfo tokenInfo) {
                    this.f$0.m262lambda$onPlayerError$1$complaytvpremiumMainActivity$5(this.f$1, tokenInfo);
                }
            });
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
    }

    private final class ChannelAdapter extends ArrayAdapter<Channel> {
        private static short[] t = {-16412, -16406};
        final MainActivity this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        ChannelAdapter(MainActivity mainActivity, List<Channel> list) {
            super(mainActivity, 0, list);
            this.this$0 = mainActivity;
        }

        private static String t(int i, int i2, int i3) {
            char[] cArr = new char[i2 - i];
            for (int i4 = 0; i4 < i2 - i; i4++) {
                cArr[i4] = (char) (t[i + i4] ^ i3);
            }
            return new String(cArr);
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            LinearLayout linearLayout;
            ImageView imageView;
            TextView textView;
            if ((view instanceof LinearLayout) && (view.getTag() instanceof View[])) {
                linearLayout = (LinearLayout) view;
                View[] viewArr = (View[]) linearLayout.getTag();
                imageView = (ImageView) viewArr[0];
                textView = (TextView) viewArr[1];
            } else {
                linearLayout = new LinearLayout(this.this$0);
                linearLayout.setOrientation(0);
                linearLayout.setGravity(16);
                linearLayout.setPadding(MainActivity.m231$$Nest$mdpToPx(this.this$0, 10), MainActivity.m231$$Nest$mdpToPx(this.this$0, 6), MainActivity.m231$$Nest$mdpToPx(this.this$0, 10), MainActivity.m231$$Nest$mdpToPx(this.this$0, 6));
                imageView = new ImageView(this.this$0);
                linearLayout.addView(imageView, new LinearLayout.LayoutParams(MainActivity.m231$$Nest$mdpToPx(this.this$0, 36), MainActivity.m231$$Nest$mdpToPx(this.this$0, 36)));
                TextView textView2 = new TextView(this.this$0);
                textView2.setTextColor(-1);
                textView2.setTextSize(18.0f);
                textView2.setPadding(MainActivity.m231$$Nest$mdpToPx(this.this$0, 10), 0, 0, 0);
                linearLayout.addView(textView2, new LinearLayout.LayoutParams(0, MainActivity.m231$$Nest$mdpToPx(this.this$0, 48), 1.0f));
                linearLayout.setTag(new View[]{imageView, textView2});
                textView = textView2;
            }
            Channel item = getItem(i);
            if (item != null) {
                textView.setText(item.globalIndex + t(0, 2, -16438) + item.name);
                if (item.icon == null || item.icon.isEmpty()) {
                    imageView.setImageDrawable(null);
                    imageView.setVisibility(4);
                } else {
                    Glide.with((Activity) this.this$0).load(item.icon).into(imageView);
                    imageView.setVisibility(0);
                }
            }
            return linearLayout;
        }
    }

    public static native /* synthetic */ void $r8$lambda$A4cIfB_N83uvRaQwaTKCFClIkCM(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$fgetappConfig, reason: not valid java name */
    static native /* bridge */ /* synthetic */ AppConfig m219$$Nest$fgetappConfig(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$fgetchannelList, reason: not valid java name */
    static native /* bridge */ /* synthetic */ ListView m220$$Nest$fgetchannelList(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$fgetcontrolsVisible, reason: not valid java name */
    static native /* bridge */ /* synthetic */ boolean m221$$Nest$fgetcontrolsVisible(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$fgetcurrentPreviewChannel, reason: not valid java name */
    static native /* bridge */ /* synthetic */ Channel m222$$Nest$fgetcurrentPreviewChannel(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$fgetisFullscreen, reason: not valid java name */
    static native /* bridge */ /* synthetic */ boolean m223$$Nest$fgetisFullscreen(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$fgetpreview403Retries, reason: not valid java name */
    static native /* bridge */ /* synthetic */ int m224$$Nest$fgetpreview403Retries(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$fgetpreviewPlayer, reason: not valid java name */
    static native /* bridge */ /* synthetic */ ExoPlayer m225$$Nest$fgetpreviewPlayer(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$fgetpreviewProgress, reason: not valid java name */
    static native /* bridge */ /* synthetic */ ProgressBar m226$$Nest$fgetpreviewProgress(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$fputpreview403Retries, reason: not valid java name */
    static native /* bridge */ /* synthetic */ void m227$$Nest$fputpreview403Retries(MainActivity mainActivity, int i);

    /* JADX INFO: renamed from: -$$Nest$mbuildMediaItem, reason: not valid java name */
    static native /* bridge */ /* synthetic */ MediaItem m228$$Nest$mbuildMediaItem(MainActivity mainActivity, Channel channel, String str, Map map);

    /* JADX INFO: renamed from: -$$Nest$mchangeFullscreenChannel, reason: not valid java name */
    static native /* bridge */ /* synthetic */ void m229$$Nest$mchangeFullscreenChannel(MainActivity mainActivity, int i);

    /* JADX INFO: renamed from: -$$Nest$mchooseHeaders, reason: not valid java name */
    static native /* bridge */ /* synthetic */ Map m230$$Nest$mchooseHeaders(MainActivity mainActivity, Channel channel, String str);

    /* JADX INFO: renamed from: -$$Nest$mdpToPx, reason: not valid java name */
    static native /* bridge */ /* synthetic */ int m231$$Nest$mdpToPx(MainActivity mainActivity, int i);

    /* JADX INFO: renamed from: -$$Nest$menterFullscreen, reason: not valid java name */
    static native /* bridge */ /* synthetic */ void m232$$Nest$menterFullscreen(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$mexitFullscreen, reason: not valid java name */
    static native /* bridge */ /* synthetic */ void m233$$Nest$mexitFullscreen(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$mhideSystemBars, reason: not valid java name */
    static native /* bridge */ /* synthetic */ void m234$$Nest$mhideSystemBars(MainActivity mainActivity);

    /* JADX INFO: renamed from: -$$Nest$misPreview403Like, reason: not valid java name */
    static native /* bridge */ /* synthetic */ boolean m235$$Nest$misPreview403Like(MainActivity mainActivity, PlaybackException playbackException);

    /* JADX INFO: renamed from: -$$Nest$mselectCategory, reason: not valid java name */
    static native /* bridge */ /* synthetic */ void m236$$Nest$mselectCategory(MainActivity mainActivity, int i);

    /* JADX INFO: renamed from: -$$Nest$mselectChannel, reason: not valid java name */
    static native /* bridge */ /* synthetic */ void m237$$Nest$mselectChannel(MainActivity mainActivity, int i);

    /* JADX INFO: renamed from: -$$Nest$mshowPlayerControls, reason: not valid java name */
    static native /* bridge */ /* synthetic */ void m238$$Nest$mshowPlayerControls(MainActivity mainActivity);

    static {
        PlayTV.registerNativesForClass(0, MainActivity.class);
        Hidden0.special_clinit_0_950(MainActivity.class);
    }

    private native MediaItem buildMediaItem(Channel channel, String str, Map<String, String> map);

    private native void buildUi();

    private native void changeFullscreenChannel(int i);

    private native void checkForUpdate();

    private native Map<String, String> chooseHeaders(Channel channel, String str);

    private native void downloadAndInstall();

    private native int dpToPx(int i);

    private native void enterFullscreen();

    private native void exitFullscreen();

    private static native String extractTimestamp(String str);

    private native void fetchCatalog(boolean z);

    private native int findDisplayCategoryIndex(String str);

    private native String getInstallDate();

    private native List<Channel> getSearchableChannels();

    private native void hideSystemBars();

    private native void hydrateFlowHeaders(Channel channel);

    private native boolean isPreview403Like(PlaybackException playbackException);

    private native void jumpToChannel(Channel channel);

    static native /* synthetic */ DrmSessionManager lambda$preparePreview$18(DrmSessionManager drmSessionManager, MediaItem mediaItem);

    static native /* synthetic */ void lambda$showAdultsPasswordDialog$12(DialogInterface dialogInterface, int i);

    private native void loadCatalog();

    private native ImageButton makeIconButton(int i);

    private native void preparePreview(Channel channel, String str);

    private native void rebuildDisplayCategories();

    private native void recordInstallDate();

    private native void refreshCategoryList();

    private native void releasePreview();

    private native void reloadCatalog();

    private native void resetControlsTimer();

    private native boolean sameChannel(Channel channel, Channel channel2);

    private native void selectCategory(int i);

    private native void selectChannel(int i);

    private native void setupGestures();

    private native void showAdultsPasswordDialog();

    private native void showCategories(List<Category> list);

    private native void showChannelInfoOverlay(Channel channel);

    private native void showChannels(Category category);

    private native void showPlayerControls();

    private native void showSearchDialog();

    private native void showUpdateDialog();

    private native void startPreview(Channel channel);

    private native void toggleAdultsLock();

    private native void updatePreviewLayout();

    @Override // android.app.Activity, android.view.Window.Callback
    public native boolean dispatchKeyEvent(KeyEvent keyEvent);

    /* JADX INFO: renamed from: lambda$buildUi$1$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ WindowInsetsCompat m239lambda$buildUi$1$complaytvpremiumMainActivity(int i, View view, WindowInsetsCompat windowInsetsCompat);

    /* JADX INFO: renamed from: lambda$buildUi$2$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m240lambda$buildUi$2$complaytvpremiumMainActivity(View view);

    /* JADX INFO: renamed from: lambda$buildUi$3$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m241lambda$buildUi$3$complaytvpremiumMainActivity(View view);

    /* JADX INFO: renamed from: lambda$buildUi$4$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m242lambda$buildUi$4$complaytvpremiumMainActivity(int i);

    /* JADX INFO: renamed from: lambda$buildUi$5$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m243lambda$buildUi$5$complaytvpremiumMainActivity();

    /* JADX INFO: renamed from: lambda$checkForUpdate$19$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m244lambda$checkForUpdate$19$complaytvpremiumMainActivity(String str);

    /* JADX INFO: renamed from: lambda$downloadAndInstall$21$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m245lambda$downloadAndInstall$21$complaytvpremiumMainActivity(File file);

    /* JADX INFO: renamed from: lambda$downloadAndInstall$22$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m246lambda$downloadAndInstall$22$complaytvpremiumMainActivity(Exception exc);

    /* JADX INFO: renamed from: lambda$downloadAndInstall$23$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m247lambda$downloadAndInstall$23$complaytvpremiumMainActivity();

    /* JADX INFO: renamed from: lambda$fetchCatalog$7$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m248lambda$fetchCatalog$7$complaytvpremiumMainActivity(List list, Channel channel);

    /* JADX INFO: renamed from: lambda$fetchCatalog$8$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m249lambda$fetchCatalog$8$complaytvpremiumMainActivity(Exception exc, Channel channel);

    /* JADX INFO: renamed from: lambda$fetchCatalog$9$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m250lambda$fetchCatalog$9$complaytvpremiumMainActivity(Channel channel);

    /* JADX INFO: renamed from: lambda$new$0$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m251lambda$new$0$complaytvpremiumMainActivity();

    /* JADX INFO: renamed from: lambda$refreshCategoryList$10$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m252lambda$refreshCategoryList$10$complaytvpremiumMainActivity(AdapterView adapterView, View view, int i, long j);

    /* JADX INFO: renamed from: lambda$setupGestures$6$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ boolean m253lambda$setupGestures$6$complaytvpremiumMainActivity(View view, MotionEvent motionEvent);

    /* JADX INFO: renamed from: lambda$showAdultsPasswordDialog$11$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m254x3f416813(EditText editText, DialogInterface dialogInterface, int i);

    /* JADX INFO: renamed from: lambda$showChannels$13$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m255lambda$showChannels$13$complaytvpremiumMainActivity(AdapterView adapterView, View view, int i, long j);

    /* JADX INFO: renamed from: lambda$showSearchDialog$14$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m256lambda$showSearchDialog$14$complaytvpremiumMainActivity(List list, AlertDialog alertDialog, AdapterView adapterView, View view, int i, long j);

    /* JADX INFO: renamed from: lambda$showUpdateDialog$20$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m257lambda$showUpdateDialog$20$complaytvpremiumMainActivity(DialogInterface dialogInterface, int i);

    /* JADX INFO: renamed from: lambda$startPreview$15$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m258lambda$startPreview$15$complaytvpremiumMainActivity(int i, Channel channel, String str);

    /* JADX INFO: renamed from: lambda$startPreview$16$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m259lambda$startPreview$16$complaytvpremiumMainActivity(int i);

    /* JADX INFO: renamed from: lambda$startPreview$17$com-playtv-premium-MainActivity, reason: not valid java name */
    native /* synthetic */ void m260lambda$startPreview$17$complaytvpremiumMainActivity(Channel channel, int i);

    @Override // android.app.Activity
    public native void onBackPressed();

    @Override // android.app.Activity
    protected native void onCreate(Bundle bundle);

    @Override // android.app.Activity
    protected native void onDestroy();

    @Override // android.app.Activity
    protected native void onPause();

    @Override // android.app.Activity
    protected native void onResume();

    @Override // android.app.Activity
    protected native void onStop();

    @Override // android.app.Activity, android.view.Window.Callback
    public native void onWindowFocusChanged(boolean z);
}
