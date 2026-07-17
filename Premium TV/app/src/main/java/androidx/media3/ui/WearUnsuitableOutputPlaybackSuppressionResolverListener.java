package androidx.media3.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.SystemClock;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ts.TsExtractor;
import java.util.List;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public final class WearUnsuitableOutputPlaybackSuppressionResolverListener implements Player.Listener {
    public static final long DEFAULT_PLAYBACK_SUPPRESSION_AUTO_RESUME_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);
    private static final String EXTRA_BLUETOOTH_SETTINGS_CLOSE_ON_CONNECT = "EXTRA_CLOSE_ON_CONNECT";
    private static final String EXTRA_BLUETOOTH_SETTINGS_CONNECTION_ONLY = "EXTRA_CONNECTION_ONLY";
    private static final String EXTRA_BLUETOOTH_SETTINGS_FILTER_TYPE = "android.bluetooth.devicepicker.extra.FILTER_TYPE";
    private static final String EXTRA_OUTPUT_SWITCHER_PACKAGE_NAME = "com.android.settings.panel.extra.PACKAGE_NAME";
    private static final int FILTER_TYPE_AUDIO = 1;
    private static final String OUTPUT_SWITCHER_INTENT_ACTION_NAME = "com.android.settings.panel.action.MEDIA_OUTPUT";
    private final Context applicationContext;
    private final long autoResumeTimeoutAfterUnsuitableOutputSuppressionMs;
    private final Clock clock;
    private long unsuitableOutputPlaybackSuppressionStartRealtimeMs;

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
    public /* synthetic */ void onPlaybackStateChanged(int i) {
        Player.Listener.CC.$default$onPlaybackStateChanged(this, i);
    }

    @Override // androidx.media3.common.Player.Listener
    public /* synthetic */ void onPlaybackSuppressionReasonChanged(int i) {
        Player.Listener.CC.$default$onPlaybackSuppressionReasonChanged(this, i);
    }

    @Override // androidx.media3.common.Player.Listener
    public /* synthetic */ void onPlayerError(PlaybackException playbackException) {
        Player.Listener.CC.$default$onPlayerError(this, playbackException);
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

    public WearUnsuitableOutputPlaybackSuppressionResolverListener(Context context) {
        this(context, DEFAULT_PLAYBACK_SUPPRESSION_AUTO_RESUME_TIMEOUT_MS);
    }

    public WearUnsuitableOutputPlaybackSuppressionResolverListener(Context context, long autoResumeTimeoutMs) {
        this(context, autoResumeTimeoutMs, SystemClock.DEFAULT);
    }

    WearUnsuitableOutputPlaybackSuppressionResolverListener(Context context, long autoResumeTimeoutMs, Clock clock) {
        Assertions.checkArgument(autoResumeTimeoutMs >= 0);
        this.applicationContext = context.getApplicationContext();
        this.autoResumeTimeoutAfterUnsuitableOutputSuppressionMs = autoResumeTimeoutMs;
        this.clock = clock;
        this.unsuitableOutputPlaybackSuppressionStartRealtimeMs = C.TIME_UNSET;
    }

    @Override // androidx.media3.common.Player.Listener
    public void onEvents(Player player, Player.Events events) {
        if (!Util.isWear(this.applicationContext)) {
            return;
        }
        if ((events.contains(6) || events.contains(5)) && player.getPlayWhenReady() && player.getPlaybackSuppressionReason() == 3) {
            player.pause();
            this.unsuitableOutputPlaybackSuppressionStartRealtimeMs = this.clock.elapsedRealtime();
            if (events.contains(5)) {
                launchSystemMediaOutputSwitcherUi(this.applicationContext);
                return;
            }
            return;
        }
        if (events.contains(6) && player.getPlaybackSuppressionReason() == 0 && this.unsuitableOutputPlaybackSuppressionStartRealtimeMs != C.TIME_UNSET && this.clock.elapsedRealtime() - this.unsuitableOutputPlaybackSuppressionStartRealtimeMs < this.autoResumeTimeoutAfterUnsuitableOutputSuppressionMs) {
            this.unsuitableOutputPlaybackSuppressionStartRealtimeMs = C.TIME_UNSET;
            player.play();
        }
    }

    private static void launchSystemMediaOutputSwitcherUi(Context context) {
        Intent outputSwitcherLaunchIntent = new Intent(OUTPUT_SWITCHER_INTENT_ACTION_NAME).addFlags(268435456).putExtra(EXTRA_OUTPUT_SWITCHER_PACKAGE_NAME, context.getPackageName());
        ComponentName outputSwitcherSystemComponentName = getSystemOrSystemUpdatedAppComponent(context, outputSwitcherLaunchIntent);
        if (outputSwitcherSystemComponentName != null) {
            outputSwitcherLaunchIntent.setComponent(outputSwitcherSystemComponentName);
            context.startActivity(outputSwitcherLaunchIntent);
            return;
        }
        Intent bluetoothSettingsLaunchIntent = new Intent("android.settings.BLUETOOTH_SETTINGS").addFlags(268468224).putExtra(EXTRA_BLUETOOTH_SETTINGS_CLOSE_ON_CONNECT, true).putExtra(EXTRA_BLUETOOTH_SETTINGS_CONNECTION_ONLY, true).putExtra(EXTRA_BLUETOOTH_SETTINGS_FILTER_TYPE, 1);
        ComponentName bluetoothSettingsSystemComponentName = getSystemOrSystemUpdatedAppComponent(context, bluetoothSettingsLaunchIntent);
        if (bluetoothSettingsSystemComponentName != null) {
            bluetoothSettingsLaunchIntent.setComponent(bluetoothSettingsSystemComponentName);
            context.startActivity(bluetoothSettingsLaunchIntent);
        }
    }

    private static ComponentName getSystemOrSystemUpdatedAppComponent(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null && activityInfo.applicationInfo != null) {
                ApplicationInfo appInfo = activityInfo.applicationInfo;
                if ((appInfo.flags & TsExtractor.TS_STREAM_TYPE_AC3) != 0) {
                    return new ComponentName(activityInfo.packageName, activityInfo.name);
                }
            }
        }
        return null;
    }
}
