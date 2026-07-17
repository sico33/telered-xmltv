package androidx.media3.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.RepeatModeUtil;
import androidx.media3.common.util.Util;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: classes.dex */
public class PlayerControlView extends FrameLayout {
    public static final int DEFAULT_REPEAT_TOGGLE_MODES = 0;
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5000;
    public static final int DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS = 200;
    private static final int MAX_UPDATE_INTERVAL_MS = 1000;
    public static final int MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100;
    private static final float[] PLAYBACK_SPEEDS;
    private static final int SETTINGS_AUDIO_TRACK_SELECTION_POSITION = 1;
    private static final int SETTINGS_PLAYBACK_SPEED_POSITION = 0;
    private long[] adGroupTimesMs;
    private final View audioTrackButton;
    private final AudioTrackSelectionAdapter audioTrackSelectionAdapter;
    private final float buttonAlphaDisabled;
    private final float buttonAlphaEnabled;
    private final ComponentListener componentListener;
    private final PlayerControlViewLayoutManager controlViewLayoutManager;
    private long currentWindowOffset;
    private final TextView durationView;
    private long[] extraAdGroupTimesMs;
    private boolean[] extraPlayedAdGroups;
    private final View fastForwardButton;
    private final TextView fastForwardButtonTextView;
    private final StringBuilder formatBuilder;
    private final Formatter formatter;
    private final ImageView fullScreenButton;
    private final String fullScreenEnterContentDescription;
    private final Drawable fullScreenEnterDrawable;
    private final String fullScreenExitContentDescription;
    private final Drawable fullScreenExitDrawable;
    private boolean isAttachedToWindow;
    private boolean isFullScreen;
    private final ImageView minimalFullScreenButton;
    private boolean multiWindowTimeBar;
    private boolean needToHideBars;
    private final ImageView nextButton;
    private OnFullScreenModeChangedListener onFullScreenModeChangedListener;
    private final Drawable pauseButtonDrawable;
    private final Timeline.Period period;
    private final Drawable playButtonDrawable;
    private final ImageView playPauseButton;
    private final PlaybackSpeedAdapter playbackSpeedAdapter;
    private final View playbackSpeedButton;
    private boolean[] playedAdGroups;
    private Player player;
    private final TextView positionView;
    private final ImageView previousButton;
    private ProgressUpdateListener progressUpdateListener;
    private final String repeatAllButtonContentDescription;
    private final Drawable repeatAllButtonDrawable;
    private final String repeatOffButtonContentDescription;
    private final Drawable repeatOffButtonDrawable;
    private final String repeatOneButtonContentDescription;
    private final Drawable repeatOneButtonDrawable;
    private final ImageView repeatToggleButton;
    private int repeatToggleModes;
    private final Resources resources;
    private final View rewindButton;
    private final TextView rewindButtonTextView;
    private boolean scrubbing;
    private final SettingsAdapter settingsAdapter;
    private final View settingsButton;
    private final RecyclerView settingsView;
    private final PopupWindow settingsWindow;
    private final int settingsWindowMargin;
    private boolean showMultiWindowTimeBar;
    private boolean showPlayButtonIfSuppressed;
    private int showTimeoutMs;
    private final ImageView shuffleButton;
    private final Drawable shuffleOffButtonDrawable;
    private final String shuffleOffContentDescription;
    private final Drawable shuffleOnButtonDrawable;
    private final String shuffleOnContentDescription;
    private final ImageView subtitleButton;
    private final Drawable subtitleOffButtonDrawable;
    private final String subtitleOffContentDescription;
    private final Drawable subtitleOnButtonDrawable;
    private final String subtitleOnContentDescription;
    private final TextTrackSelectionAdapter textTrackSelectionAdapter;
    private final TimeBar timeBar;
    private int timeBarMinUpdateIntervalMs;
    private final TrackNameProvider trackNameProvider;
    private final Runnable updateProgressAction;
    private final CopyOnWriteArrayList<VisibilityListener> visibilityListeners;
    private final ImageView vrButton;
    private final Timeline.Window window;

    @Deprecated
    public interface OnFullScreenModeChangedListener {
        void onFullScreenModeChanged(boolean z);
    }

    public interface ProgressUpdateListener {
        void onProgressUpdate(long j, long j2);
    }

    @Deprecated
    public interface VisibilityListener {
        void onVisibilityChange(int i);
    }

    static {
        MediaLibraryInfo.registerModule("media3.ui");
        PLAYBACK_SPEEDS = new float[]{0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    }

    public PlayerControlView(Context context) {
        this(context, null);
    }

    public PlayerControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, attrs);
    }

    public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr, AttributeSet playbackAttrs) throws Throwable {
        int pauseDrawableResId;
        final PlayerControlView playerControlView;
        int repeatOneDrawableResId;
        int vrDrawableResId;
        boolean animationEnabled;
        boolean animationEnabled2;
        boolean z;
        int rewindDrawableResId;
        int fullScreenExitDrawableResId;
        int fullScreenEnterDrawableResId;
        int repeatOffDrawableResId;
        int repeatAllDrawableResId;
        int shuffleOnDrawableResId;
        int shuffleOffDrawableResId;
        int subtitleOnDrawableResId;
        int subtitleOffDrawableResId;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        int repeatAllDrawableResId2;
        int previousDrawableResId;
        boolean showFastForwardButton;
        int playDrawableResId;
        int nextDrawableResId;
        View timeBarPlaceholder;
        Context context2;
        final PlayerControlView playerControlView2;
        int fastForwardDrawableResId;
        int vrDrawableResId2;
        super(context, attrs, defStyleAttr);
        int controllerLayoutId = R.layout.exo_player_control_view;
        int playDrawableResId2 = R.drawable.exo_styled_controls_play;
        int pauseDrawableResId2 = R.drawable.exo_styled_controls_pause;
        int nextDrawableResId2 = R.drawable.exo_styled_controls_next;
        int fastForwardDrawableResId2 = R.drawable.exo_styled_controls_simple_fastforward;
        int previousDrawableResId2 = R.drawable.exo_styled_controls_previous;
        int rewindDrawableResId2 = R.drawable.exo_styled_controls_simple_rewind;
        int fullScreenExitDrawableResId2 = R.drawable.exo_styled_controls_fullscreen_exit;
        int fullScreenEnterDrawableResId2 = R.drawable.exo_styled_controls_fullscreen_enter;
        int repeatOffDrawableResId2 = R.drawable.exo_styled_controls_repeat_off;
        int repeatOneDrawableResId2 = R.drawable.exo_styled_controls_repeat_one;
        int repeatAllDrawableResId3 = R.drawable.exo_styled_controls_repeat_all;
        int shuffleOnDrawableResId2 = R.drawable.exo_styled_controls_shuffle_on;
        int shuffleOnDrawableResId3 = R.drawable.exo_styled_controls_shuffle_off;
        int shuffleOffDrawableResId2 = R.drawable.exo_styled_controls_subtitle_on;
        int subtitleOnDrawableResId2 = R.drawable.exo_styled_controls_subtitle_off;
        int subtitleOffDrawableResId2 = R.drawable.exo_styled_controls_vr;
        this.showPlayButtonIfSuppressed = true;
        this.showTimeoutMs = 5000;
        this.repeatToggleModes = 0;
        this.timeBarMinUpdateIntervalMs = 200;
        if (playbackAttrs == null) {
            pauseDrawableResId = pauseDrawableResId2;
            playerControlView = this;
            repeatOneDrawableResId = repeatOneDrawableResId2;
            vrDrawableResId = subtitleOffDrawableResId2;
            animationEnabled = true;
            animationEnabled2 = true;
            z = false;
            rewindDrawableResId = rewindDrawableResId2;
            fullScreenExitDrawableResId = fullScreenExitDrawableResId2;
            fullScreenEnterDrawableResId = fullScreenEnterDrawableResId2;
            repeatOffDrawableResId = repeatOffDrawableResId2;
            repeatAllDrawableResId = repeatAllDrawableResId3;
            shuffleOnDrawableResId = shuffleOnDrawableResId2;
            shuffleOffDrawableResId = shuffleOnDrawableResId3;
            subtitleOnDrawableResId = shuffleOffDrawableResId2;
            subtitleOffDrawableResId = subtitleOnDrawableResId2;
            z2 = true;
            z3 = true;
            z4 = false;
            z5 = false;
            repeatAllDrawableResId2 = controllerLayoutId;
            previousDrawableResId = previousDrawableResId2;
            showFastForwardButton = true;
        } else {
            TypedArray a = context.getTheme().obtainStyledAttributes(playbackAttrs, R.styleable.PlayerControlView, defStyleAttr, 0);
            try {
                controllerLayoutId = a.getResourceId(R.styleable.PlayerControlView_controller_layout_id, controllerLayoutId);
                playDrawableResId2 = a.getResourceId(R.styleable.PlayerControlView_play_icon, playDrawableResId2);
                pauseDrawableResId2 = a.getResourceId(R.styleable.PlayerControlView_pause_icon, pauseDrawableResId2);
                nextDrawableResId2 = a.getResourceId(R.styleable.PlayerControlView_next_icon, nextDrawableResId2);
                fastForwardDrawableResId2 = a.getResourceId(R.styleable.PlayerControlView_fastforward_icon, fastForwardDrawableResId2);
                int previousDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_previous_icon, previousDrawableResId2);
                int rewindDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_rewind_icon, rewindDrawableResId2);
                int fullScreenExitDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_fullscreen_exit_icon, fullScreenExitDrawableResId2);
                int fullScreenEnterDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_fullscreen_enter_icon, fullScreenEnterDrawableResId2);
                int repeatOffDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_repeat_off_icon, repeatOffDrawableResId2);
                int repeatOneDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_repeat_one_icon, repeatOneDrawableResId2);
                try {
                    int repeatAllDrawableResId4 = a.getResourceId(R.styleable.PlayerControlView_repeat_all_icon, repeatAllDrawableResId3);
                    int shuffleOnDrawableResId4 = shuffleOnDrawableResId2;
                    try {
                        shuffleOnDrawableResId4 = a.getResourceId(R.styleable.PlayerControlView_shuffle_on_icon, shuffleOnDrawableResId4);
                        int shuffleOffDrawableResId3 = shuffleOnDrawableResId3;
                        try {
                            shuffleOffDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_shuffle_off_icon, shuffleOffDrawableResId3);
                            int subtitleOnDrawableResId3 = shuffleOffDrawableResId2;
                            try {
                                subtitleOnDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_subtitle_on_icon, subtitleOnDrawableResId3);
                                int subtitleOffDrawableResId3 = subtitleOnDrawableResId2;
                                try {
                                    subtitleOffDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_subtitle_off_icon, subtitleOffDrawableResId3);
                                    try {
                                        int vrDrawableResId3 = a.getResourceId(R.styleable.PlayerControlView_vr_icon, subtitleOffDrawableResId2);
                                        try {
                                            int i = R.styleable.PlayerControlView_show_timeout;
                                            pauseDrawableResId = pauseDrawableResId2;
                                            playerControlView = this;
                                            try {
                                                int vrDrawableResId4 = playerControlView.showTimeoutMs;
                                                playerControlView.showTimeoutMs = a.getInt(i, vrDrawableResId4);
                                                playerControlView.repeatToggleModes = getRepeatToggleModes(a, playerControlView.repeatToggleModes);
                                                boolean showRewindButton = true;
                                                try {
                                                    showRewindButton = a.getBoolean(R.styleable.PlayerControlView_show_rewind_button, true);
                                                    boolean showFastForwardButton2 = true;
                                                    try {
                                                        showFastForwardButton2 = a.getBoolean(R.styleable.PlayerControlView_show_fastforward_button, true);
                                                        boolean showPreviousButton = true;
                                                        try {
                                                            showPreviousButton = a.getBoolean(R.styleable.PlayerControlView_show_previous_button, true);
                                                            boolean showNextButton = true;
                                                            try {
                                                                showNextButton = a.getBoolean(R.styleable.PlayerControlView_show_next_button, true);
                                                                boolean showShuffleButton = false;
                                                                try {
                                                                    showShuffleButton = a.getBoolean(R.styleable.PlayerControlView_show_shuffle_button, false);
                                                                    boolean showSubtitleButton = false;
                                                                    try {
                                                                        showSubtitleButton = a.getBoolean(R.styleable.PlayerControlView_show_subtitle_button, false);
                                                                        try {
                                                                            boolean showVrButton = a.getBoolean(R.styleable.PlayerControlView_show_vr_button, false);
                                                                            try {
                                                                                try {
                                                                                    playerControlView.setTimeBarMinUpdateInterval(a.getInt(R.styleable.PlayerControlView_time_bar_min_update_interval, playerControlView.timeBarMinUpdateIntervalMs));
                                                                                    try {
                                                                                        boolean animationEnabled3 = a.getBoolean(R.styleable.PlayerControlView_animation_enabled, true);
                                                                                        a.recycle();
                                                                                        animationEnabled = animationEnabled3;
                                                                                        repeatOneDrawableResId = repeatOneDrawableResId3;
                                                                                        animationEnabled2 = showRewindButton;
                                                                                        z = showVrButton;
                                                                                        vrDrawableResId = vrDrawableResId3;
                                                                                        previousDrawableResId = previousDrawableResId3;
                                                                                        rewindDrawableResId = rewindDrawableResId3;
                                                                                        fullScreenExitDrawableResId = fullScreenExitDrawableResId3;
                                                                                        fullScreenEnterDrawableResId = fullScreenEnterDrawableResId3;
                                                                                        repeatOffDrawableResId = repeatOffDrawableResId3;
                                                                                        repeatAllDrawableResId = repeatAllDrawableResId4;
                                                                                        shuffleOnDrawableResId = shuffleOnDrawableResId4;
                                                                                        shuffleOffDrawableResId = shuffleOffDrawableResId3;
                                                                                        subtitleOnDrawableResId = subtitleOnDrawableResId3;
                                                                                        subtitleOffDrawableResId = subtitleOffDrawableResId3;
                                                                                        showFastForwardButton = showFastForwardButton2;
                                                                                        z2 = showPreviousButton;
                                                                                        z3 = showNextButton;
                                                                                        z4 = showShuffleButton;
                                                                                        z5 = showSubtitleButton;
                                                                                        repeatAllDrawableResId2 = controllerLayoutId;
                                                                                    } catch (Throwable th) {
                                                                                        th = th;
                                                                                        a.recycle();
                                                                                        throw th;
                                                                                    }
                                                                                } catch (Throwable th2) {
                                                                                    th = th2;
                                                                                }
                                                                            } catch (Throwable th3) {
                                                                                th = th3;
                                                                            }
                                                                        } catch (Throwable th4) {
                                                                            th = th4;
                                                                        }
                                                                    } catch (Throwable th5) {
                                                                        th = th5;
                                                                    }
                                                                } catch (Throwable th6) {
                                                                    th = th6;
                                                                }
                                                            } catch (Throwable th7) {
                                                                th = th7;
                                                            }
                                                        } catch (Throwable th8) {
                                                            th = th8;
                                                        }
                                                    } catch (Throwable th9) {
                                                        th = th9;
                                                    }
                                                } catch (Throwable th10) {
                                                    th = th10;
                                                }
                                            } catch (Throwable th11) {
                                                th = th11;
                                            }
                                        } catch (Throwable th12) {
                                            th = th12;
                                        }
                                    } catch (Throwable th13) {
                                        th = th13;
                                    }
                                } catch (Throwable th14) {
                                    th = th14;
                                }
                            } catch (Throwable th15) {
                                th = th15;
                            }
                        } catch (Throwable th16) {
                            th = th16;
                        }
                    } catch (Throwable th17) {
                        th = th17;
                    }
                } catch (Throwable th18) {
                    th = th18;
                }
            } catch (Throwable th19) {
                th = th19;
            }
        }
        int shuffleOnDrawableResId5 = previousDrawableResId;
        LayoutInflater.from(context).inflate(repeatAllDrawableResId2, playerControlView);
        playerControlView.setDescendantFocusability(262144);
        int nextDrawableResId3 = nextDrawableResId2;
        playerControlView.componentListener = new ComponentListener();
        playerControlView.visibilityListeners = new CopyOnWriteArrayList<>();
        playerControlView.period = new Timeline.Period();
        playerControlView.window = new Timeline.Window();
        playerControlView.formatBuilder = new StringBuilder();
        int playDrawableResId3 = playDrawableResId2;
        playerControlView.formatter = new Formatter(playerControlView.formatBuilder, Locale.getDefault());
        playerControlView.adGroupTimesMs = new long[0];
        playerControlView.playedAdGroups = new boolean[0];
        playerControlView.extraAdGroupTimesMs = new long[0];
        playerControlView.extraPlayedAdGroups = new boolean[0];
        playerControlView.updateProgressAction = new Runnable() { // from class: androidx.media3.ui.PlayerControlView$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.updateProgress();
            }
        };
        playerControlView.durationView = (TextView) playerControlView.findViewById(R.id.exo_duration);
        playerControlView.positionView = (TextView) playerControlView.findViewById(R.id.exo_position);
        playerControlView.subtitleButton = (ImageView) playerControlView.findViewById(R.id.exo_subtitle);
        if (playerControlView.subtitleButton != null) {
            playerControlView.subtitleButton.setOnClickListener(playerControlView.componentListener);
        }
        playerControlView.fullScreenButton = (ImageView) playerControlView.findViewById(R.id.exo_fullscreen);
        initializeFullScreenButton(playerControlView.fullScreenButton, new View.OnClickListener() { // from class: androidx.media3.ui.PlayerControlView$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.onFullScreenButtonClicked(view);
            }
        });
        playerControlView.minimalFullScreenButton = (ImageView) playerControlView.findViewById(R.id.exo_minimal_fullscreen);
        initializeFullScreenButton(playerControlView.minimalFullScreenButton, new View.OnClickListener() { // from class: androidx.media3.ui.PlayerControlView$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.onFullScreenButtonClicked(view);
            }
        });
        playerControlView.settingsButton = playerControlView.findViewById(R.id.exo_settings);
        if (playerControlView.settingsButton != null) {
            playerControlView.settingsButton.setOnClickListener(playerControlView.componentListener);
        }
        playerControlView.playbackSpeedButton = playerControlView.findViewById(R.id.exo_playback_speed);
        if (playerControlView.playbackSpeedButton != null) {
            playerControlView.playbackSpeedButton.setOnClickListener(playerControlView.componentListener);
        }
        playerControlView.audioTrackButton = playerControlView.findViewById(R.id.exo_audio_track);
        if (playerControlView.audioTrackButton != null) {
            playerControlView.audioTrackButton.setOnClickListener(playerControlView.componentListener);
        }
        TimeBar customTimeBar = (TimeBar) playerControlView.findViewById(R.id.exo_progress);
        View timeBarPlaceholder2 = playerControlView.findViewById(R.id.exo_progress_placeholder);
        if (customTimeBar != null) {
            playerControlView.timeBar = customTimeBar;
            playDrawableResId = playDrawableResId3;
            nextDrawableResId = nextDrawableResId3;
            timeBarPlaceholder = timeBarPlaceholder2;
            context2 = context;
            playerControlView2 = playerControlView;
            fastForwardDrawableResId = fastForwardDrawableResId2;
        } else if (timeBarPlaceholder2 != null) {
            playDrawableResId = playDrawableResId3;
            nextDrawableResId = nextDrawableResId3;
            fastForwardDrawableResId = fastForwardDrawableResId2;
            playerControlView2 = this;
            context2 = context;
            DefaultTimeBar defaultTimeBar = new DefaultTimeBar(context2, null, 0, playbackAttrs, R.style.ExoStyledControls_TimeBar);
            defaultTimeBar.setId(R.id.exo_progress);
            defaultTimeBar.setLayoutParams(timeBarPlaceholder2.getLayoutParams());
            ViewGroup parent = (ViewGroup) timeBarPlaceholder2.getParent();
            timeBarPlaceholder = timeBarPlaceholder2;
            int timeBarIndex = parent.indexOfChild(timeBarPlaceholder);
            parent.removeView(timeBarPlaceholder);
            parent.addView(defaultTimeBar, timeBarIndex);
            playerControlView2.timeBar = defaultTimeBar;
        } else {
            playDrawableResId = playDrawableResId3;
            nextDrawableResId = nextDrawableResId3;
            timeBarPlaceholder = timeBarPlaceholder2;
            context2 = context;
            playerControlView2 = playerControlView;
            fastForwardDrawableResId = fastForwardDrawableResId2;
            playerControlView2.timeBar = null;
        }
        if (playerControlView2.timeBar != null) {
            playerControlView2.timeBar.addListener(playerControlView2.componentListener);
        }
        playerControlView2.resources = context2.getResources();
        playerControlView2.playPauseButton = (ImageView) playerControlView2.findViewById(R.id.exo_play_pause);
        if (playerControlView2.playPauseButton != null) {
            playerControlView2.playPauseButton.setOnClickListener(playerControlView2.componentListener);
        }
        playerControlView2.previousButton = (ImageView) playerControlView2.findViewById(R.id.exo_prev);
        if (playerControlView2.previousButton != null) {
            playerControlView2.previousButton.setImageDrawable(Util.getDrawable(context2, playerControlView2.resources, shuffleOnDrawableResId5));
            playerControlView2.previousButton.setOnClickListener(playerControlView2.componentListener);
        }
        playerControlView2.nextButton = (ImageView) playerControlView2.findViewById(R.id.exo_next);
        if (playerControlView2.nextButton != null) {
            playerControlView2.nextButton.setImageDrawable(Util.getDrawable(context2, playerControlView2.resources, nextDrawableResId));
            playerControlView2.nextButton.setOnClickListener(playerControlView2.componentListener);
        }
        Typeface typeface = ResourcesCompat.getFont(context2, R.font.roboto_medium_numbers);
        ImageView rewButton = (ImageView) playerControlView2.findViewById(R.id.exo_rew);
        TextView rewButtonWithAmount = (TextView) playerControlView2.findViewById(R.id.exo_rew_with_amount);
        if (rewButton != null) {
            rewButton.setImageDrawable(Util.getDrawable(context2, playerControlView2.resources, rewindDrawableResId));
            playerControlView2.rewindButton = rewButton;
            playerControlView2.rewindButtonTextView = null;
        } else if (rewButtonWithAmount != null) {
            rewButtonWithAmount.setTypeface(typeface);
            playerControlView2.rewindButtonTextView = rewButtonWithAmount;
            playerControlView2.rewindButton = playerControlView2.rewindButtonTextView;
        } else {
            playerControlView2.rewindButtonTextView = null;
            playerControlView2.rewindButton = null;
        }
        if (playerControlView2.rewindButton != null) {
            playerControlView2.rewindButton.setOnClickListener(playerControlView2.componentListener);
        }
        ImageView ffwdButton = (ImageView) playerControlView2.findViewById(R.id.exo_ffwd);
        TextView ffwdButtonWithAmount = (TextView) playerControlView2.findViewById(R.id.exo_ffwd_with_amount);
        if (ffwdButton != null) {
            ffwdButton.setImageDrawable(Util.getDrawable(context2, playerControlView2.resources, fastForwardDrawableResId));
            playerControlView2.fastForwardButton = ffwdButton;
            playerControlView2.fastForwardButtonTextView = null;
        } else if (ffwdButtonWithAmount != null) {
            ffwdButtonWithAmount.setTypeface(typeface);
            playerControlView2.fastForwardButtonTextView = ffwdButtonWithAmount;
            playerControlView2.fastForwardButton = playerControlView2.fastForwardButtonTextView;
        } else {
            playerControlView2.fastForwardButtonTextView = null;
            playerControlView2.fastForwardButton = null;
        }
        if (playerControlView2.fastForwardButton != null) {
            playerControlView2.fastForwardButton.setOnClickListener(playerControlView2.componentListener);
        }
        playerControlView2.repeatToggleButton = (ImageView) playerControlView2.findViewById(R.id.exo_repeat_toggle);
        if (playerControlView2.repeatToggleButton != null) {
            playerControlView2.repeatToggleButton.setOnClickListener(playerControlView2.componentListener);
        }
        playerControlView2.shuffleButton = (ImageView) playerControlView2.findViewById(R.id.exo_shuffle);
        if (playerControlView2.shuffleButton != null) {
            playerControlView2.shuffleButton.setOnClickListener(playerControlView2.componentListener);
        }
        playerControlView2.buttonAlphaEnabled = playerControlView2.resources.getInteger(R.integer.exo_media_button_opacity_percentage_enabled) / 100.0f;
        playerControlView2.buttonAlphaDisabled = playerControlView2.resources.getInteger(R.integer.exo_media_button_opacity_percentage_disabled) / 100.0f;
        playerControlView2.vrButton = (ImageView) playerControlView2.findViewById(R.id.exo_vr);
        if (playerControlView2.vrButton != null) {
            vrDrawableResId2 = vrDrawableResId;
            playerControlView2.vrButton.setImageDrawable(Util.getDrawable(context2, playerControlView2.resources, vrDrawableResId2));
            playerControlView2.updateButton(false, playerControlView2.vrButton);
        } else {
            vrDrawableResId2 = vrDrawableResId;
        }
        playerControlView2.controlViewLayoutManager = new PlayerControlViewLayoutManager(playerControlView2);
        playerControlView2.controlViewLayoutManager.setAnimationEnabled(animationEnabled);
        Resources resources = playerControlView2.resources;
        int vrDrawableResId5 = R.string.exo_controls_playback_speed;
        String[] settingTexts = {resources.getString(vrDrawableResId5), playerControlView2.resources.getString(R.string.exo_track_selection_title_audio)};
        Drawable[] settingIcons = {Util.getDrawable(context2, playerControlView2.resources, R.drawable.exo_styled_controls_speed), Util.getDrawable(context2, playerControlView2.resources, R.drawable.exo_styled_controls_audiotrack)};
        playerControlView2.settingsAdapter = playerControlView2.new SettingsAdapter(settingTexts, settingIcons);
        playerControlView2.settingsWindowMargin = playerControlView2.resources.getDimensionPixelSize(R.dimen.exo_settings_offset);
        playerControlView2.settingsView = (RecyclerView) LayoutInflater.from(context2).inflate(R.layout.exo_styled_settings_list, (ViewGroup) null);
        playerControlView2.settingsView.setAdapter(playerControlView2.settingsAdapter);
        playerControlView2.settingsView.setLayoutManager(new LinearLayoutManager(playerControlView2.getContext()));
        playerControlView2.settingsWindow = new PopupWindow((View) playerControlView2.settingsView, -2, -2, true);
        if (Util.SDK_INT < 23) {
            playerControlView2.settingsWindow.setBackgroundDrawable(new ColorDrawable(0));
        }
        playerControlView2.settingsWindow.setOnDismissListener(playerControlView2.componentListener);
        playerControlView2.needToHideBars = true;
        playerControlView2.trackNameProvider = new DefaultTrackNameProvider(playerControlView2.getResources());
        playerControlView2.subtitleOnButtonDrawable = Util.getDrawable(context2, playerControlView2.resources, subtitleOnDrawableResId);
        playerControlView2.subtitleOffButtonDrawable = Util.getDrawable(context2, playerControlView2.resources, subtitleOffDrawableResId);
        playerControlView2.subtitleOnContentDescription = playerControlView2.resources.getString(R.string.exo_controls_cc_enabled_description);
        playerControlView2.subtitleOffContentDescription = playerControlView2.resources.getString(R.string.exo_controls_cc_disabled_description);
        playerControlView2.textTrackSelectionAdapter = new TextTrackSelectionAdapter();
        playerControlView2.audioTrackSelectionAdapter = new AudioTrackSelectionAdapter();
        playerControlView2.playbackSpeedAdapter = playerControlView2.new PlaybackSpeedAdapter(playerControlView2.resources.getStringArray(R.array.exo_controls_playback_speeds), PLAYBACK_SPEEDS);
        playerControlView2.playButtonDrawable = Util.getDrawable(context2, playerControlView2.resources, playDrawableResId);
        playerControlView2.pauseButtonDrawable = Util.getDrawable(context2, playerControlView2.resources, pauseDrawableResId);
        playerControlView2.fullScreenExitDrawable = Util.getDrawable(context2, playerControlView2.resources, fullScreenExitDrawableResId);
        playerControlView2.fullScreenEnterDrawable = Util.getDrawable(context2, playerControlView2.resources, fullScreenEnterDrawableResId);
        playerControlView2.repeatOffButtonDrawable = Util.getDrawable(context2, playerControlView2.resources, repeatOffDrawableResId);
        playerControlView2.repeatOneButtonDrawable = Util.getDrawable(context2, playerControlView2.resources, repeatOneDrawableResId);
        playerControlView2.repeatAllButtonDrawable = Util.getDrawable(context2, playerControlView2.resources, repeatAllDrawableResId);
        playerControlView2.shuffleOnButtonDrawable = Util.getDrawable(context2, playerControlView2.resources, shuffleOnDrawableResId);
        playerControlView2.shuffleOffButtonDrawable = Util.getDrawable(context2, playerControlView2.resources, shuffleOffDrawableResId);
        playerControlView2.fullScreenExitContentDescription = playerControlView2.resources.getString(R.string.exo_controls_fullscreen_exit_description);
        playerControlView2.fullScreenEnterContentDescription = playerControlView2.resources.getString(R.string.exo_controls_fullscreen_enter_description);
        playerControlView2.repeatOffButtonContentDescription = playerControlView2.resources.getString(R.string.exo_controls_repeat_off_description);
        playerControlView2.repeatOneButtonContentDescription = playerControlView2.resources.getString(R.string.exo_controls_repeat_one_description);
        playerControlView2.repeatAllButtonContentDescription = playerControlView2.resources.getString(R.string.exo_controls_repeat_all_description);
        playerControlView2.shuffleOnContentDescription = playerControlView2.resources.getString(R.string.exo_controls_shuffle_on_description);
        playerControlView2.shuffleOffContentDescription = playerControlView2.resources.getString(R.string.exo_controls_shuffle_off_description);
        ViewGroup bottomBar = (ViewGroup) playerControlView2.findViewById(R.id.exo_bottom_bar);
        playerControlView2.controlViewLayoutManager.setShowButton(bottomBar, true);
        playerControlView2.controlViewLayoutManager.setShowButton(playerControlView2.fastForwardButton, showFastForwardButton);
        playerControlView2.controlViewLayoutManager.setShowButton(playerControlView2.rewindButton, animationEnabled2);
        boolean showRewindButton2 = z2;
        playerControlView2.controlViewLayoutManager.setShowButton(playerControlView2.previousButton, showRewindButton2);
        boolean showPreviousButton2 = z3;
        playerControlView2.controlViewLayoutManager.setShowButton(playerControlView2.nextButton, showPreviousButton2);
        boolean showNextButton2 = z4;
        playerControlView2.controlViewLayoutManager.setShowButton(playerControlView2.shuffleButton, showNextButton2);
        boolean showShuffleButton2 = z5;
        playerControlView2.controlViewLayoutManager.setShowButton(playerControlView2.subtitleButton, showShuffleButton2);
        boolean showSubtitleButton2 = z;
        playerControlView2.controlViewLayoutManager.setShowButton(playerControlView2.vrButton, showSubtitleButton2);
        playerControlView2.controlViewLayoutManager.setShowButton(playerControlView2.repeatToggleButton, playerControlView2.repeatToggleModes != 0);
        playerControlView2.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: androidx.media3.ui.PlayerControlView$$ExternalSyntheticLambda2
            @Override // android.view.View.OnLayoutChangeListener
            public final void onLayoutChange(View view, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
                this.f$0.onLayoutChange(view, i2, i3, i4, i5, i6, i7, i8, i9);
            }
        });
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(Player player) {
        boolean z = true;
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper());
        if (player != null && player.getApplicationLooper() != Looper.getMainLooper()) {
            z = false;
        }
        Assertions.checkArgument(z);
        if (this.player == player) {
            return;
        }
        if (this.player != null) {
            this.player.removeListener(this.componentListener);
        }
        this.player = player;
        if (player != null) {
            player.addListener(this.componentListener);
        }
        updateAll();
    }

    @Deprecated
    public void setShowMultiWindowTimeBar(boolean showMultiWindowTimeBar) {
        this.showMultiWindowTimeBar = showMultiWindowTimeBar;
        updateTimeline();
    }

    public void setShowPlayButtonIfPlaybackIsSuppressed(boolean showPlayButtonIfSuppressed) {
        this.showPlayButtonIfSuppressed = showPlayButtonIfSuppressed;
        updatePlayPauseButton();
    }

    public void setExtraAdGroupMarkers(long[] extraAdGroupTimesMs, boolean[] extraPlayedAdGroups) {
        if (extraAdGroupTimesMs == null) {
            this.extraAdGroupTimesMs = new long[0];
            this.extraPlayedAdGroups = new boolean[0];
        } else {
            boolean[] extraPlayedAdGroups2 = (boolean[]) Assertions.checkNotNull(extraPlayedAdGroups);
            Assertions.checkArgument(extraAdGroupTimesMs.length == extraPlayedAdGroups2.length);
            this.extraAdGroupTimesMs = extraAdGroupTimesMs;
            this.extraPlayedAdGroups = extraPlayedAdGroups2;
        }
        updateTimeline();
    }

    @Deprecated
    public void addVisibilityListener(VisibilityListener listener) {
        Assertions.checkNotNull(listener);
        this.visibilityListeners.add(listener);
    }

    @Deprecated
    public void removeVisibilityListener(VisibilityListener listener) {
        this.visibilityListeners.remove(listener);
    }

    public void setProgressUpdateListener(ProgressUpdateListener listener) {
        this.progressUpdateListener = listener;
    }

    public void setShowRewindButton(boolean showRewindButton) {
        this.controlViewLayoutManager.setShowButton(this.rewindButton, showRewindButton);
        updateNavigation();
    }

    public void setShowFastForwardButton(boolean showFastForwardButton) {
        this.controlViewLayoutManager.setShowButton(this.fastForwardButton, showFastForwardButton);
        updateNavigation();
    }

    public void setShowPreviousButton(boolean showPreviousButton) {
        this.controlViewLayoutManager.setShowButton(this.previousButton, showPreviousButton);
        updateNavigation();
    }

    public void setShowNextButton(boolean showNextButton) {
        this.controlViewLayoutManager.setShowButton(this.nextButton, showNextButton);
        updateNavigation();
    }

    public int getShowTimeoutMs() {
        return this.showTimeoutMs;
    }

    public void setShowTimeoutMs(int showTimeoutMs) {
        this.showTimeoutMs = showTimeoutMs;
        if (isFullyVisible()) {
            this.controlViewLayoutManager.resetHideCallbacks();
        }
    }

    public int getRepeatToggleModes() {
        return this.repeatToggleModes;
    }

    public void setRepeatToggleModes(int repeatToggleModes) {
        this.repeatToggleModes = repeatToggleModes;
        if (this.player != null && this.player.isCommandAvailable(15)) {
            int currentMode = this.player.getRepeatMode();
            if (repeatToggleModes == 0 && currentMode != 0) {
                this.player.setRepeatMode(0);
            } else if (repeatToggleModes == 1 && currentMode == 2) {
                this.player.setRepeatMode(1);
            } else if (repeatToggleModes == 2 && currentMode == 1) {
                this.player.setRepeatMode(2);
            }
        }
        this.controlViewLayoutManager.setShowButton(this.repeatToggleButton, repeatToggleModes != 0);
        updateRepeatModeButton();
    }

    public boolean getShowShuffleButton() {
        return this.controlViewLayoutManager.getShowButton(this.shuffleButton);
    }

    public void setShowShuffleButton(boolean showShuffleButton) {
        this.controlViewLayoutManager.setShowButton(this.shuffleButton, showShuffleButton);
        updateShuffleButton();
    }

    public boolean getShowSubtitleButton() {
        return this.controlViewLayoutManager.getShowButton(this.subtitleButton);
    }

    public void setShowSubtitleButton(boolean showSubtitleButton) {
        this.controlViewLayoutManager.setShowButton(this.subtitleButton, showSubtitleButton);
    }

    public boolean getShowVrButton() {
        return this.controlViewLayoutManager.getShowButton(this.vrButton);
    }

    public void setShowVrButton(boolean showVrButton) {
        this.controlViewLayoutManager.setShowButton(this.vrButton, showVrButton);
    }

    public void setVrButtonListener(View.OnClickListener onClickListener) {
        if (this.vrButton != null) {
            this.vrButton.setOnClickListener(onClickListener);
            updateButton(onClickListener != null, this.vrButton);
        }
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        this.controlViewLayoutManager.setAnimationEnabled(animationEnabled);
    }

    public boolean isAnimationEnabled() {
        return this.controlViewLayoutManager.isAnimationEnabled();
    }

    public void setTimeBarMinUpdateInterval(int minUpdateIntervalMs) {
        this.timeBarMinUpdateIntervalMs = Util.constrainValue(minUpdateIntervalMs, 16, 1000);
    }

    @Deprecated
    public void setOnFullScreenModeChangedListener(OnFullScreenModeChangedListener listener) {
        this.onFullScreenModeChangedListener = listener;
        updateFullScreenButtonVisibility(this.fullScreenButton, listener != null);
        updateFullScreenButtonVisibility(this.minimalFullScreenButton, listener != null);
    }

    public void show() {
        this.controlViewLayoutManager.show();
    }

    public void hide() {
        this.controlViewLayoutManager.hide();
    }

    public void hideImmediately() {
        this.controlViewLayoutManager.hideImmediately();
    }

    public boolean isFullyVisible() {
        return this.controlViewLayoutManager.isFullyVisible();
    }

    public boolean isVisible() {
        return getVisibility() == 0;
    }

    void notifyOnVisibilityChange() {
        for (VisibilityListener visibilityListener : this.visibilityListeners) {
            visibilityListener.onVisibilityChange(getVisibility());
        }
    }

    void updateAll() {
        updatePlayPauseButton();
        updateNavigation();
        updateRepeatModeButton();
        updateShuffleButton();
        updateTrackLists();
        updatePlaybackSpeedList();
        updateTimeline();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePlayPauseButton() {
        int stringRes;
        if (isVisible() && this.isAttachedToWindow && this.playPauseButton != null) {
            boolean shouldShowPlayButton = Util.shouldShowPlayButton(this.player, this.showPlayButtonIfSuppressed);
            Drawable drawable = shouldShowPlayButton ? this.playButtonDrawable : this.pauseButtonDrawable;
            if (shouldShowPlayButton) {
                stringRes = R.string.exo_controls_play_description;
            } else {
                stringRes = R.string.exo_controls_pause_description;
            }
            this.playPauseButton.setImageDrawable(drawable);
            this.playPauseButton.setContentDescription(this.resources.getString(stringRes));
            boolean enablePlayPause = shouldEnablePlayPauseButton();
            updateButton(enablePlayPause, this.playPauseButton);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNavigation() {
        boolean zIsCommandAvailable;
        if (!isVisible() || !this.isAttachedToWindow) {
            return;
        }
        Player player = this.player;
        boolean enableSeeking = false;
        boolean enablePrevious = false;
        boolean enableRewind = false;
        boolean enableFastForward = false;
        boolean enableNext = false;
        if (player != null) {
            if (this.showMultiWindowTimeBar && canShowMultiWindowTimeBar(player, this.window)) {
                zIsCommandAvailable = player.isCommandAvailable(10);
            } else {
                zIsCommandAvailable = player.isCommandAvailable(5);
            }
            enableSeeking = zIsCommandAvailable;
            enablePrevious = player.isCommandAvailable(7);
            enableRewind = player.isCommandAvailable(11);
            enableFastForward = player.isCommandAvailable(12);
            enableNext = player.isCommandAvailable(9);
        }
        if (enableRewind) {
            updateRewindButton();
        }
        if (enableFastForward) {
            updateFastForwardButton();
        }
        updateButton(enablePrevious, this.previousButton);
        updateButton(enableRewind, this.rewindButton);
        updateButton(enableFastForward, this.fastForwardButton);
        updateButton(enableNext, this.nextButton);
        if (this.timeBar != null) {
            this.timeBar.setEnabled(enableSeeking);
        }
    }

    private void updateRewindButton() {
        long rewindMs = this.player != null ? this.player.getSeekBackIncrement() : 5000L;
        int rewindSec = (int) (rewindMs / 1000);
        if (this.rewindButtonTextView != null) {
            this.rewindButtonTextView.setText(String.valueOf(rewindSec));
        }
        if (this.rewindButton != null) {
            this.rewindButton.setContentDescription(this.resources.getQuantityString(R.plurals.exo_controls_rewind_by_amount_description, rewindSec, Integer.valueOf(rewindSec)));
        }
    }

    private void updateFastForwardButton() {
        long fastForwardMs = this.player != null ? this.player.getSeekForwardIncrement() : C.DEFAULT_SEEK_FORWARD_INCREMENT_MS;
        int fastForwardSec = (int) (fastForwardMs / 1000);
        if (this.fastForwardButtonTextView != null) {
            this.fastForwardButtonTextView.setText(String.valueOf(fastForwardSec));
        }
        if (this.fastForwardButton != null) {
            this.fastForwardButton.setContentDescription(this.resources.getQuantityString(R.plurals.exo_controls_fastforward_by_amount_description, fastForwardSec, Integer.valueOf(fastForwardSec)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRepeatModeButton() {
        if (!isVisible() || !this.isAttachedToWindow || this.repeatToggleButton == null) {
            return;
        }
        if (this.repeatToggleModes == 0) {
            updateButton(false, this.repeatToggleButton);
        }
        Player player = this.player;
        if (player == null || !player.isCommandAvailable(15)) {
            updateButton(false, this.repeatToggleButton);
            this.repeatToggleButton.setImageDrawable(this.repeatOffButtonDrawable);
            this.repeatToggleButton.setContentDescription(this.repeatOffButtonContentDescription);
            return;
        }
        updateButton(true, this.repeatToggleButton);
        switch (player.getRepeatMode()) {
            case 0:
                this.repeatToggleButton.setImageDrawable(this.repeatOffButtonDrawable);
                this.repeatToggleButton.setContentDescription(this.repeatOffButtonContentDescription);
                break;
            case 1:
                this.repeatToggleButton.setImageDrawable(this.repeatOneButtonDrawable);
                this.repeatToggleButton.setContentDescription(this.repeatOneButtonContentDescription);
                break;
            case 2:
                this.repeatToggleButton.setImageDrawable(this.repeatAllButtonDrawable);
                this.repeatToggleButton.setContentDescription(this.repeatAllButtonContentDescription);
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateShuffleButton() {
        String str;
        if (!isVisible() || !this.isAttachedToWindow || this.shuffleButton == null) {
            return;
        }
        Player player = this.player;
        if (!this.controlViewLayoutManager.getShowButton(this.shuffleButton)) {
            updateButton(false, this.shuffleButton);
            return;
        }
        if (player == null || !player.isCommandAvailable(14)) {
            updateButton(false, this.shuffleButton);
            this.shuffleButton.setImageDrawable(this.shuffleOffButtonDrawable);
            this.shuffleButton.setContentDescription(this.shuffleOffContentDescription);
            return;
        }
        updateButton(true, this.shuffleButton);
        this.shuffleButton.setImageDrawable(player.getShuffleModeEnabled() ? this.shuffleOnButtonDrawable : this.shuffleOffButtonDrawable);
        ImageView imageView = this.shuffleButton;
        if (player.getShuffleModeEnabled()) {
            str = this.shuffleOnContentDescription;
        } else {
            str = this.shuffleOffContentDescription;
        }
        imageView.setContentDescription(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTrackLists() {
        initTrackSelectionAdapter();
        updateButton(this.textTrackSelectionAdapter.getItemCount() > 0, this.subtitleButton);
        updateSettingsButton();
    }

    private void initTrackSelectionAdapter() {
        this.textTrackSelectionAdapter.clear();
        this.audioTrackSelectionAdapter.clear();
        if (this.player == null || !this.player.isCommandAvailable(30) || !this.player.isCommandAvailable(29)) {
            return;
        }
        Tracks tracks = this.player.getCurrentTracks();
        this.audioTrackSelectionAdapter.init(gatherSupportedTrackInfosOfType(tracks, 1));
        boolean showButton = this.controlViewLayoutManager.getShowButton(this.subtitleButton);
        TextTrackSelectionAdapter textTrackSelectionAdapter = this.textTrackSelectionAdapter;
        if (showButton) {
            textTrackSelectionAdapter.init(gatherSupportedTrackInfosOfType(tracks, 3));
        } else {
            textTrackSelectionAdapter.init(ImmutableList.of());
        }
    }

    private ImmutableList<TrackInformation> gatherSupportedTrackInfosOfType(Tracks tracks, int trackType) {
        ImmutableList.Builder<TrackInformation> trackInfos = new ImmutableList.Builder<>();
        List<Tracks.Group> trackGroups = tracks.getGroups();
        for (int trackGroupIndex = 0; trackGroupIndex < trackGroups.size(); trackGroupIndex++) {
            Tracks.Group trackGroup = trackGroups.get(trackGroupIndex);
            if (trackGroup.getType() == trackType) {
                for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                    if (trackGroup.isTrackSupported(trackIndex)) {
                        Format trackFormat = trackGroup.getTrackFormat(trackIndex);
                        if ((trackFormat.selectionFlags & 2) == 0) {
                            String trackName = this.trackNameProvider.getTrackName(trackFormat);
                            trackInfos.add(new TrackInformation(tracks, trackGroupIndex, trackIndex, trackName));
                        }
                    }
                }
            }
        }
        return trackInfos.build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTimeline() {
        Timeline timeline;
        int j;
        Player player = this.player;
        if (player != null) {
            int i = 1;
            this.multiWindowTimeBar = this.showMultiWindowTimeBar && canShowMultiWindowTimeBar(player, this.window);
            long j2 = 0;
            this.currentWindowOffset = 0L;
            long durationUs = 0;
            int adGroupCount = 0;
            if (player.isCommandAvailable(17)) {
                timeline = player.getCurrentTimeline();
            } else {
                timeline = Timeline.EMPTY;
            }
            boolean zIsEmpty = timeline.isEmpty();
            long j3 = C.TIME_UNSET;
            if (!zIsEmpty) {
                int currentWindowIndex = player.getCurrentMediaItemIndex();
                int firstWindowIndex = this.multiWindowTimeBar ? 0 : currentWindowIndex;
                int lastWindowIndex = this.multiWindowTimeBar ? timeline.getWindowCount() - 1 : currentWindowIndex;
                int i2 = firstWindowIndex;
                while (i2 <= lastWindowIndex) {
                    if (i2 == currentWindowIndex) {
                        this.currentWindowOffset = Util.usToMs(durationUs);
                    }
                    timeline.getWindow(i2, this.window);
                    if (this.window.durationUs == j3) {
                        Assertions.checkState(!this.multiWindowTimeBar);
                        break;
                    }
                    int j4 = this.window.firstPeriodIndex;
                    while (j4 <= this.window.lastPeriodIndex) {
                        timeline.getPeriod(j4, this.period);
                        int removedGroups = this.period.getRemovedAdGroupCount();
                        int totalGroups = this.period.getAdGroupCount();
                        long j5 = j3;
                        int adGroupIndex = removedGroups;
                        while (adGroupIndex < totalGroups) {
                            long adGroupTimeInPeriodUs = this.period.getAdGroupTimeUs(adGroupIndex);
                            if (adGroupTimeInPeriodUs == Long.MIN_VALUE) {
                                j = j4;
                                if (this.period.durationUs != j5) {
                                    adGroupTimeInPeriodUs = this.period.durationUs;
                                }
                                adGroupCount = adGroupCount;
                                adGroupIndex++;
                                j4 = j;
                            } else {
                                j = j4;
                            }
                            long adGroupTimeInWindowUs = adGroupTimeInPeriodUs + this.period.getPositionInWindowUs();
                            if (adGroupTimeInWindowUs < j2) {
                                adGroupCount = adGroupCount;
                            } else {
                                if (adGroupCount == this.adGroupTimesMs.length) {
                                    int newLength = this.adGroupTimesMs.length == 0 ? i : this.adGroupTimesMs.length * 2;
                                    this.adGroupTimesMs = Arrays.copyOf(this.adGroupTimesMs, newLength);
                                    this.playedAdGroups = Arrays.copyOf(this.playedAdGroups, newLength);
                                }
                                this.adGroupTimesMs[adGroupCount] = Util.usToMs(durationUs + adGroupTimeInWindowUs);
                                this.playedAdGroups[adGroupCount] = this.period.hasPlayedAdGroup(adGroupIndex);
                                adGroupCount++;
                            }
                            adGroupIndex++;
                            j4 = j;
                        }
                        j4++;
                        j3 = j5;
                    }
                    durationUs += this.window.durationUs;
                    i2++;
                    i = i;
                    j2 = j2;
                }
            } else if (player.isCommandAvailable(16)) {
                long playerDurationMs = player.getContentDuration();
                if (playerDurationMs != C.TIME_UNSET) {
                    durationUs = Util.msToUs(playerDurationMs);
                }
            }
            long durationMs = Util.usToMs(durationUs);
            if (this.durationView != null) {
                this.durationView.setText(Util.getStringForTime(this.formatBuilder, this.formatter, durationMs));
            }
            if (this.timeBar != null) {
                this.timeBar.setDuration(durationMs);
                int extraAdGroupCount = this.extraAdGroupTimesMs.length;
                int totalAdGroupCount = adGroupCount + extraAdGroupCount;
                if (totalAdGroupCount > this.adGroupTimesMs.length) {
                    this.adGroupTimesMs = Arrays.copyOf(this.adGroupTimesMs, totalAdGroupCount);
                    this.playedAdGroups = Arrays.copyOf(this.playedAdGroups, totalAdGroupCount);
                }
                System.arraycopy(this.extraAdGroupTimesMs, 0, this.adGroupTimesMs, adGroupCount, extraAdGroupCount);
                System.arraycopy(this.extraPlayedAdGroups, 0, this.playedAdGroups, adGroupCount, extraAdGroupCount);
                this.timeBar.setAdGroupTimesMs(this.adGroupTimesMs, this.playedAdGroups, totalAdGroupCount);
            }
            updateProgress();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateProgress() {
        long mediaTimeDelayMs;
        if (!isVisible() || !this.isAttachedToWindow) {
            return;
        }
        Player player = this.player;
        long position = 0;
        long bufferedPosition = 0;
        if (player != null && player.isCommandAvailable(16)) {
            position = this.currentWindowOffset + player.getContentPosition();
            bufferedPosition = this.currentWindowOffset + player.getContentBufferedPosition();
        }
        if (this.positionView != null && !this.scrubbing) {
            this.positionView.setText(Util.getStringForTime(this.formatBuilder, this.formatter, position));
        }
        if (this.timeBar != null) {
            this.timeBar.setPosition(position);
            this.timeBar.setBufferedPosition(bufferedPosition);
        }
        if (this.progressUpdateListener != null) {
            this.progressUpdateListener.onProgressUpdate(position, bufferedPosition);
        }
        removeCallbacks(this.updateProgressAction);
        int playbackState = player == null ? 1 : player.getPlaybackState();
        if (player != null && player.isPlaying()) {
            if (this.timeBar != null) {
                mediaTimeDelayMs = this.timeBar.getPreferredUpdateDelay();
            } else {
                mediaTimeDelayMs = 1000;
            }
            long mediaTimeUntilNextFullSecondMs = 1000 - (position % 1000);
            long mediaTimeDelayMs2 = Math.min(mediaTimeDelayMs, mediaTimeUntilNextFullSecondMs);
            float playbackSpeed = player.getPlaybackParameters().speed;
            long delayMs = playbackSpeed > 0.0f ? (long) (mediaTimeDelayMs2 / playbackSpeed) : 1000L;
            postDelayed(this.updateProgressAction, Util.constrainValue(delayMs, this.timeBarMinUpdateIntervalMs, 1000L));
            return;
        }
        if (playbackState != 4 && playbackState != 1) {
            postDelayed(this.updateProgressAction, 1000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePlaybackSpeedList() {
        if (this.player == null) {
            return;
        }
        this.playbackSpeedAdapter.updateSelectedIndex(this.player.getPlaybackParameters().speed);
        this.settingsAdapter.setSubTextAtPosition(0, this.playbackSpeedAdapter.getSelectedText());
        updateSettingsButton();
    }

    private void updateSettingsButton() {
        updateButton(this.settingsAdapter.hasSettingsToShow(), this.settingsButton);
    }

    private void updateSettingsWindowSize() {
        this.settingsView.measure(0, 0);
        int maxWidth = getWidth() - (this.settingsWindowMargin * 2);
        int itemWidth = this.settingsView.getMeasuredWidth();
        int width = Math.min(itemWidth, maxWidth);
        this.settingsWindow.setWidth(width);
        int maxHeight = getHeight() - (this.settingsWindowMargin * 2);
        int totalHeight = this.settingsView.getMeasuredHeight();
        int height = Math.min(maxHeight, totalHeight);
        this.settingsWindow.setHeight(height);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void displaySettingsWindow(RecyclerView.Adapter<?> adapter, View anchorView) {
        this.settingsView.setAdapter(adapter);
        updateSettingsWindowSize();
        this.needToHideBars = false;
        this.settingsWindow.dismiss();
        this.needToHideBars = true;
        int xoff = (getWidth() - this.settingsWindow.getWidth()) - this.settingsWindowMargin;
        int yoff = (-this.settingsWindow.getHeight()) - this.settingsWindowMargin;
        this.settingsWindow.showAsDropDown(anchorView, xoff, yoff);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPlaybackSpeed(float speed) {
        if (this.player == null || !this.player.isCommandAvailable(13)) {
            return;
        }
        this.player.setPlaybackParameters(this.player.getPlaybackParameters().withSpeed(speed));
    }

    void requestPlayPauseFocus() {
        if (this.playPauseButton != null) {
            this.playPauseButton.requestFocus();
        }
    }

    private void updateButton(boolean enabled, View view) {
        if (view == null) {
            return;
        }
        view.setEnabled(enabled);
        view.setAlpha(enabled ? this.buttonAlphaEnabled : this.buttonAlphaDisabled);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void seekToTimeBarPosition(Player player, long positionMs) {
        if (this.multiWindowTimeBar) {
            if (player.isCommandAvailable(17) && player.isCommandAvailable(10)) {
                Timeline timeline = player.getCurrentTimeline();
                int windowCount = timeline.getWindowCount();
                int windowIndex = 0;
                while (true) {
                    long windowDurationMs = timeline.getWindow(windowIndex, this.window).getDurationMs();
                    if (positionMs < windowDurationMs) {
                        break;
                    }
                    if (windowIndex == windowCount - 1) {
                        positionMs = windowDurationMs;
                        break;
                    } else {
                        positionMs -= windowDurationMs;
                        windowIndex++;
                    }
                }
                player.seekTo(windowIndex, positionMs);
            }
        } else if (player.isCommandAvailable(5)) {
            player.seekTo(positionMs);
        }
        updateProgress();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onFullScreenButtonClicked(View v) {
        if (this.onFullScreenModeChangedListener == null) {
            return;
        }
        this.isFullScreen = !this.isFullScreen;
        updateFullScreenButtonForState(this.fullScreenButton, this.isFullScreen);
        updateFullScreenButtonForState(this.minimalFullScreenButton, this.isFullScreen);
        if (this.onFullScreenModeChangedListener != null) {
            this.onFullScreenModeChangedListener.onFullScreenModeChanged(this.isFullScreen);
        }
    }

    private void updateFullScreenButtonForState(ImageView fullScreenButton, boolean isFullScreen) {
        if (fullScreenButton == null) {
            return;
        }
        if (isFullScreen) {
            fullScreenButton.setImageDrawable(this.fullScreenExitDrawable);
            fullScreenButton.setContentDescription(this.fullScreenExitContentDescription);
        } else {
            fullScreenButton.setImageDrawable(this.fullScreenEnterDrawable);
            fullScreenButton.setContentDescription(this.fullScreenEnterContentDescription);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSettingViewClicked(int position) {
        if (position == 0) {
            displaySettingsWindow(this.playbackSpeedAdapter, (View) Assertions.checkNotNull(this.settingsButton));
        } else if (position == 1) {
            displaySettingsWindow(this.audioTrackSelectionAdapter, (View) Assertions.checkNotNull(this.settingsButton));
        } else {
            this.settingsWindow.dismiss();
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.controlViewLayoutManager.onAttachedToWindow();
        this.isAttachedToWindow = true;
        if (isFullyVisible()) {
            this.controlViewLayoutManager.resetHideCallbacks();
        }
        updateAll();
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.controlViewLayoutManager.onDetachedFromWindow();
        this.isAttachedToWindow = false;
        removeCallbacks(this.updateProgressAction);
        this.controlViewLayoutManager.removeHideCallbacks();
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchKeyEvent(KeyEvent event) {
        return dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    public boolean dispatchMediaKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        Player player = this.player;
        if (player == null || !isHandledMediaKey(keyCode)) {
            return false;
        }
        if (event.getAction() == 0) {
            if (keyCode == 90) {
                if (player.getPlaybackState() != 4 && player.isCommandAvailable(12)) {
                    player.seekForward();
                    return true;
                }
                return true;
            }
            if (keyCode == 89 && player.isCommandAvailable(11)) {
                player.seekBack();
                return true;
            }
            if (event.getRepeatCount() == 0) {
                switch (keyCode) {
                    case 79:
                    case 85:
                        Util.handlePlayPauseButtonAction(player, this.showPlayButtonIfSuppressed);
                        return true;
                    case 87:
                        if (player.isCommandAvailable(9)) {
                            player.seekToNext();
                            return true;
                        }
                        return true;
                    case 88:
                        if (player.isCommandAvailable(7)) {
                            player.seekToPrevious();
                            return true;
                        }
                        return true;
                    case 126:
                        Util.handlePlayButtonAction(player);
                        return true;
                    case 127:
                        Util.handlePauseButtonAction(player);
                        return true;
                    default:
                        return true;
                }
            }
            return true;
        }
        return true;
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.controlViewLayoutManager.onLayout(changed, left, top, right, bottom);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        int width = right - left;
        int height = bottom - top;
        int oldWidth = oldRight - oldLeft;
        int oldHeight = oldBottom - oldTop;
        if ((width != oldWidth || height != oldHeight) && this.settingsWindow.isShowing()) {
            updateSettingsWindowSize();
            int xOffset = (getWidth() - this.settingsWindow.getWidth()) - this.settingsWindowMargin;
            int yOffset = (-this.settingsWindow.getHeight()) - this.settingsWindowMargin;
            this.settingsWindow.update(v, xOffset, yOffset, -1, -1);
        }
    }

    private boolean shouldEnablePlayPauseButton() {
        return (this.player == null || !this.player.isCommandAvailable(1) || (this.player.isCommandAvailable(17) && this.player.getCurrentTimeline().isEmpty())) ? false : true;
    }

    private static boolean isHandledMediaKey(int keyCode) {
        return keyCode == 90 || keyCode == 89 || keyCode == 85 || keyCode == 79 || keyCode == 126 || keyCode == 127 || keyCode == 87 || keyCode == 88;
    }

    private static boolean canShowMultiWindowTimeBar(Player player, Timeline.Window window) {
        Timeline timeline;
        int windowCount;
        if (!player.isCommandAvailable(17) || (windowCount = (timeline = player.getCurrentTimeline()).getWindowCount()) <= 1 || windowCount > 100) {
            return false;
        }
        for (int i = 0; i < windowCount; i++) {
            if (timeline.getWindow(i, window).durationUs == C.TIME_UNSET) {
                return false;
            }
        }
        return true;
    }

    private static void initializeFullScreenButton(View fullScreenButton, View.OnClickListener listener) {
        if (fullScreenButton == null) {
            return;
        }
        fullScreenButton.setVisibility(8);
        fullScreenButton.setOnClickListener(listener);
    }

    private static void updateFullScreenButtonVisibility(View fullScreenButton, boolean visible) {
        if (fullScreenButton == null) {
            return;
        }
        if (visible) {
            fullScreenButton.setVisibility(0);
        } else {
            fullScreenButton.setVisibility(8);
        }
    }

    private static int getRepeatToggleModes(TypedArray a, int defaultValue) {
        return a.getInt(R.styleable.PlayerControlView_repeat_toggle_modes, defaultValue);
    }

    private final class ComponentListener implements Player.Listener, TimeBar.OnScrubListener, View.OnClickListener, PopupWindow.OnDismissListener {
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

        private ComponentListener() {
        }

        @Override // androidx.media3.common.Player.Listener
        public void onEvents(Player player, Player.Events events) {
            if (events.containsAny(4, 5, 13)) {
                PlayerControlView.this.updatePlayPauseButton();
            }
            if (events.containsAny(4, 5, 7, 13)) {
                PlayerControlView.this.updateProgress();
            }
            if (events.containsAny(8, 13)) {
                PlayerControlView.this.updateRepeatModeButton();
            }
            if (events.containsAny(9, 13)) {
                PlayerControlView.this.updateShuffleButton();
            }
            if (events.containsAny(8, 9, 11, 0, 16, 17, 13)) {
                PlayerControlView.this.updateNavigation();
            }
            if (events.containsAny(11, 0, 13)) {
                PlayerControlView.this.updateTimeline();
            }
            if (events.containsAny(12, 13)) {
                PlayerControlView.this.updatePlaybackSpeedList();
            }
            if (events.containsAny(2, 13)) {
                PlayerControlView.this.updateTrackLists();
            }
        }

        @Override // androidx.media3.ui.TimeBar.OnScrubListener
        public void onScrubStart(TimeBar timeBar, long position) {
            PlayerControlView.this.scrubbing = true;
            if (PlayerControlView.this.positionView != null) {
                PlayerControlView.this.positionView.setText(Util.getStringForTime(PlayerControlView.this.formatBuilder, PlayerControlView.this.formatter, position));
            }
            PlayerControlView.this.controlViewLayoutManager.removeHideCallbacks();
        }

        @Override // androidx.media3.ui.TimeBar.OnScrubListener
        public void onScrubMove(TimeBar timeBar, long position) {
            if (PlayerControlView.this.positionView != null) {
                PlayerControlView.this.positionView.setText(Util.getStringForTime(PlayerControlView.this.formatBuilder, PlayerControlView.this.formatter, position));
            }
        }

        @Override // androidx.media3.ui.TimeBar.OnScrubListener
        public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
            PlayerControlView.this.scrubbing = false;
            if (!canceled && PlayerControlView.this.player != null) {
                PlayerControlView.this.seekToTimeBarPosition(PlayerControlView.this.player, position);
            }
            PlayerControlView.this.controlViewLayoutManager.resetHideCallbacks();
        }

        @Override // android.widget.PopupWindow.OnDismissListener
        public void onDismiss() {
            if (PlayerControlView.this.needToHideBars) {
                PlayerControlView.this.controlViewLayoutManager.resetHideCallbacks();
            }
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            Player player = PlayerControlView.this.player;
            if (player != null) {
                PlayerControlView.this.controlViewLayoutManager.resetHideCallbacks();
                if (PlayerControlView.this.nextButton != view) {
                    if (PlayerControlView.this.previousButton != view) {
                        if (PlayerControlView.this.fastForwardButton != view) {
                            if (PlayerControlView.this.rewindButton != view) {
                                ImageView imageView = PlayerControlView.this.playPauseButton;
                                PlayerControlView playerControlView = PlayerControlView.this;
                                if (imageView == view) {
                                    Util.handlePlayPauseButtonAction(player, playerControlView.showPlayButtonIfSuppressed);
                                    return;
                                }
                                if (playerControlView.repeatToggleButton != view) {
                                    if (PlayerControlView.this.shuffleButton != view) {
                                        View view2 = PlayerControlView.this.settingsButton;
                                        PlayerControlView playerControlView2 = PlayerControlView.this;
                                        if (view2 == view) {
                                            playerControlView2.controlViewLayoutManager.removeHideCallbacks();
                                            PlayerControlView.this.displaySettingsWindow(PlayerControlView.this.settingsAdapter, PlayerControlView.this.settingsButton);
                                            return;
                                        }
                                        View view3 = playerControlView2.playbackSpeedButton;
                                        PlayerControlView playerControlView3 = PlayerControlView.this;
                                        if (view3 == view) {
                                            playerControlView3.controlViewLayoutManager.removeHideCallbacks();
                                            PlayerControlView.this.displaySettingsWindow(PlayerControlView.this.playbackSpeedAdapter, PlayerControlView.this.playbackSpeedButton);
                                            return;
                                        }
                                        View view4 = playerControlView3.audioTrackButton;
                                        PlayerControlView playerControlView4 = PlayerControlView.this;
                                        if (view4 == view) {
                                            playerControlView4.controlViewLayoutManager.removeHideCallbacks();
                                            PlayerControlView.this.displaySettingsWindow(PlayerControlView.this.audioTrackSelectionAdapter, PlayerControlView.this.audioTrackButton);
                                            return;
                                        } else {
                                            if (playerControlView4.subtitleButton == view) {
                                                PlayerControlView.this.controlViewLayoutManager.removeHideCallbacks();
                                                PlayerControlView.this.displaySettingsWindow(PlayerControlView.this.textTrackSelectionAdapter, PlayerControlView.this.subtitleButton);
                                                return;
                                            }
                                            return;
                                        }
                                    }
                                    if (player.isCommandAvailable(14)) {
                                        player.setShuffleModeEnabled(!player.getShuffleModeEnabled());
                                        return;
                                    }
                                    return;
                                }
                                if (player.isCommandAvailable(15)) {
                                    player.setRepeatMode(RepeatModeUtil.getNextRepeatMode(player.getRepeatMode(), PlayerControlView.this.repeatToggleModes));
                                    return;
                                }
                                return;
                            }
                            if (player.isCommandAvailable(11)) {
                                player.seekBack();
                                return;
                            }
                            return;
                        }
                        if (player.getPlaybackState() != 4 && player.isCommandAvailable(12)) {
                            player.seekForward();
                            return;
                        }
                        return;
                    }
                    if (player.isCommandAvailable(7)) {
                        player.seekToPrevious();
                        return;
                    }
                    return;
                }
                if (player.isCommandAvailable(9)) {
                    player.seekToNext();
                }
            }
        }
    }

    private class SettingsAdapter extends RecyclerView.Adapter<SettingViewHolder> {
        private final Drawable[] iconIds;
        private final String[] mainTexts;
        private final String[] subTexts;

        public SettingsAdapter(String[] mainTexts, Drawable[] iconIds) {
            this.mainTexts = mainTexts;
            this.subTexts = new String[mainTexts.length];
            this.iconIds = iconIds;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public SettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(PlayerControlView.this.getContext()).inflate(R.layout.exo_styled_settings_list_item, parent, false);
            return PlayerControlView.this.new SettingViewHolder(v);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(SettingViewHolder holder, int position) {
            if (shouldShowSetting(position)) {
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
            } else {
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            }
            holder.mainTextView.setText(this.mainTexts[position]);
            if (this.subTexts[position] == null) {
                holder.subTextView.setVisibility(8);
            } else {
                holder.subTextView.setText(this.subTexts[position]);
            }
            if (this.iconIds[position] == null) {
                holder.iconView.setVisibility(8);
            } else {
                holder.iconView.setImageDrawable(this.iconIds[position]);
            }
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public long getItemId(int position) {
            return position;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.mainTexts.length;
        }

        public void setSubTextAtPosition(int position, String subText) {
            this.subTexts[position] = subText;
        }

        public boolean hasSettingsToShow() {
            if (shouldShowSetting(1) || shouldShowSetting(0)) {
                return true;
            }
            return false;
        }

        private boolean shouldShowSetting(int position) {
            if (PlayerControlView.this.player == null) {
                return false;
            }
            switch (position) {
                case 0:
                    return PlayerControlView.this.player.isCommandAvailable(13);
                case 1:
                    return PlayerControlView.this.player.isCommandAvailable(30) && PlayerControlView.this.player.isCommandAvailable(29);
                default:
                    return true;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class SettingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView mainTextView;
        private final TextView subTextView;

        public SettingViewHolder(View itemView) {
            super(itemView);
            if (Util.SDK_INT < 26) {
                itemView.setFocusable(true);
            }
            this.mainTextView = (TextView) itemView.findViewById(R.id.exo_main_text);
            this.subTextView = (TextView) itemView.findViewById(R.id.exo_sub_text);
            this.iconView = (ImageView) itemView.findViewById(R.id.exo_icon);
            itemView.setOnClickListener(new View.OnClickListener() { // from class: androidx.media3.ui.PlayerControlView$SettingViewHolder$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.m164x7eeeb754(view);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$new$0$androidx-media3-ui-PlayerControlView$SettingViewHolder, reason: not valid java name */
        /* synthetic */ void m164x7eeeb754(View v) {
            PlayerControlView.this.onSettingViewClicked(getBindingAdapterPosition());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class PlaybackSpeedAdapter extends RecyclerView.Adapter<SubSettingViewHolder> {
        private final String[] playbackSpeedTexts;
        private final float[] playbackSpeeds;
        private int selectedIndex;

        public PlaybackSpeedAdapter(String[] playbackSpeedTexts, float[] playbackSpeeds) {
            this.playbackSpeedTexts = playbackSpeedTexts;
            this.playbackSpeeds = playbackSpeeds;
        }

        public void updateSelectedIndex(float playbackSpeed) {
            int closestMatchIndex = 0;
            float closestMatchDifference = Float.MAX_VALUE;
            for (int i = 0; i < this.playbackSpeeds.length; i++) {
                float difference = Math.abs(playbackSpeed - this.playbackSpeeds[i]);
                if (difference < closestMatchDifference) {
                    closestMatchIndex = i;
                    closestMatchDifference = difference;
                }
            }
            this.selectedIndex = closestMatchIndex;
        }

        public String getSelectedText() {
            return this.playbackSpeedTexts[this.selectedIndex];
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public SubSettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(PlayerControlView.this.getContext()).inflate(R.layout.exo_styled_sub_settings_list_item, parent, false);
            return new SubSettingViewHolder(v);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(SubSettingViewHolder holder, final int position) {
            if (position < this.playbackSpeedTexts.length) {
                holder.textView.setText(this.playbackSpeedTexts[position]);
            }
            if (position == this.selectedIndex) {
                holder.itemView.setSelected(true);
                holder.checkView.setVisibility(0);
            } else {
                holder.itemView.setSelected(false);
                holder.checkView.setVisibility(4);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() { // from class: androidx.media3.ui.PlayerControlView$PlaybackSpeedAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.m163x9de2ddb7(position, view);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onBindViewHolder$0$androidx-media3-ui-PlayerControlView$PlaybackSpeedAdapter, reason: not valid java name */
        /* synthetic */ void m163x9de2ddb7(int position, View v) {
            if (position != this.selectedIndex) {
                PlayerControlView.this.setPlaybackSpeed(this.playbackSpeeds[position]);
            }
            PlayerControlView.this.settingsWindow.dismiss();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.playbackSpeedTexts.length;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class TrackInformation {
        public final Tracks.Group trackGroup;
        public final int trackIndex;
        public final String trackName;

        public TrackInformation(Tracks tracks, int trackGroupIndex, int trackIndex, String trackName) {
            this.trackGroup = tracks.getGroups().get(trackGroupIndex);
            this.trackIndex = trackIndex;
            this.trackName = trackName;
        }

        public boolean isSelected() {
            return this.trackGroup.isTrackSelected(this.trackIndex);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class TextTrackSelectionAdapter extends TrackSelectionAdapter {
        private TextTrackSelectionAdapter() {
            super();
        }

        @Override // androidx.media3.ui.PlayerControlView.TrackSelectionAdapter
        public void init(List<TrackInformation> trackInformations) {
            boolean subtitleIsOn = false;
            for (int i = 0; i < trackInformations.size(); i++) {
                if (trackInformations.get(i).isSelected()) {
                    subtitleIsOn = true;
                    break;
                }
            }
            if (PlayerControlView.this.subtitleButton != null) {
                ImageView imageView = PlayerControlView.this.subtitleButton;
                PlayerControlView playerControlView = PlayerControlView.this;
                imageView.setImageDrawable(subtitleIsOn ? playerControlView.subtitleOnButtonDrawable : playerControlView.subtitleOffButtonDrawable);
                ImageView imageView2 = PlayerControlView.this.subtitleButton;
                PlayerControlView playerControlView2 = PlayerControlView.this;
                imageView2.setContentDescription(subtitleIsOn ? playerControlView2.subtitleOnContentDescription : playerControlView2.subtitleOffContentDescription);
            }
            this.tracks = trackInformations;
        }

        @Override // androidx.media3.ui.PlayerControlView.TrackSelectionAdapter
        public void onBindViewHolderAtZeroPosition(SubSettingViewHolder holder) {
            holder.textView.setText(R.string.exo_track_selection_none);
            boolean isTrackSelectionOff = true;
            for (int i = 0; i < this.tracks.size(); i++) {
                if (this.tracks.get(i).isSelected()) {
                    isTrackSelectionOff = false;
                    break;
                }
            }
            holder.checkView.setVisibility(isTrackSelectionOff ? 0 : 4);
            holder.itemView.setOnClickListener(new View.OnClickListener() { // from class: androidx.media3.ui.PlayerControlView$TextTrackSelectionAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.m165x7bd5d809(view);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onBindViewHolderAtZeroPosition$0$androidx-media3-ui-PlayerControlView$TextTrackSelectionAdapter, reason: not valid java name */
        /* synthetic */ void m165x7bd5d809(View v) {
            if (PlayerControlView.this.player != null && PlayerControlView.this.player.isCommandAvailable(29)) {
                TrackSelectionParameters trackSelectionParameters = PlayerControlView.this.player.getTrackSelectionParameters();
                PlayerControlView.this.player.setTrackSelectionParameters(trackSelectionParameters.buildUpon().clearOverridesOfType(3).setIgnoredTextSelectionFlags(-3).build());
                PlayerControlView.this.settingsWindow.dismiss();
            }
        }

        @Override // androidx.media3.ui.PlayerControlView.TrackSelectionAdapter, androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(SubSettingViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            if (position > 0) {
                TrackInformation track = this.tracks.get(position - 1);
                holder.checkView.setVisibility(track.isSelected() ? 0 : 4);
            }
        }

        @Override // androidx.media3.ui.PlayerControlView.TrackSelectionAdapter
        public void onTrackSelection(String subtext) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class AudioTrackSelectionAdapter extends TrackSelectionAdapter {
        private AudioTrackSelectionAdapter() {
            super();
        }

        @Override // androidx.media3.ui.PlayerControlView.TrackSelectionAdapter
        public void onBindViewHolderAtZeroPosition(SubSettingViewHolder holder) {
            holder.textView.setText(R.string.exo_track_selection_auto);
            TrackSelectionParameters parameters = ((Player) Assertions.checkNotNull(PlayerControlView.this.player)).getTrackSelectionParameters();
            boolean hasSelectionOverride = hasSelectionOverride(parameters);
            holder.checkView.setVisibility(hasSelectionOverride ? 4 : 0);
            holder.itemView.setOnClickListener(new View.OnClickListener() { // from class: androidx.media3.ui.PlayerControlView$AudioTrackSelectionAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.m162xa84b12b0(view);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onBindViewHolderAtZeroPosition$0$androidx-media3-ui-PlayerControlView$AudioTrackSelectionAdapter, reason: not valid java name */
        /* synthetic */ void m162xa84b12b0(View v) {
            if (PlayerControlView.this.player != null && PlayerControlView.this.player.isCommandAvailable(29)) {
                TrackSelectionParameters trackSelectionParameters = PlayerControlView.this.player.getTrackSelectionParameters();
                ((Player) Util.castNonNull(PlayerControlView.this.player)).setTrackSelectionParameters(trackSelectionParameters.buildUpon().clearOverridesOfType(1).setTrackTypeDisabled(1, false).build());
                PlayerControlView.this.settingsAdapter.setSubTextAtPosition(1, PlayerControlView.this.getResources().getString(R.string.exo_track_selection_auto));
                PlayerControlView.this.settingsWindow.dismiss();
            }
        }

        private boolean hasSelectionOverride(TrackSelectionParameters trackSelectionParameters) {
            for (int i = 0; i < this.tracks.size(); i++) {
                TrackGroup trackGroup = this.tracks.get(i).trackGroup.getMediaTrackGroup();
                if (trackSelectionParameters.overrides.containsKey(trackGroup)) {
                    return true;
                }
            }
            return false;
        }

        @Override // androidx.media3.ui.PlayerControlView.TrackSelectionAdapter
        public void onTrackSelection(String subtext) {
            PlayerControlView.this.settingsAdapter.setSubTextAtPosition(1, subtext);
        }

        @Override // androidx.media3.ui.PlayerControlView.TrackSelectionAdapter
        public void init(List<TrackInformation> trackInformations) {
            this.tracks = trackInformations;
            TrackSelectionParameters params = ((Player) Assertions.checkNotNull(PlayerControlView.this.player)).getTrackSelectionParameters();
            if (trackInformations.isEmpty()) {
                PlayerControlView.this.settingsAdapter.setSubTextAtPosition(1, PlayerControlView.this.getResources().getString(R.string.exo_track_selection_none));
                return;
            }
            if (!hasSelectionOverride(params)) {
                PlayerControlView.this.settingsAdapter.setSubTextAtPosition(1, PlayerControlView.this.getResources().getString(R.string.exo_track_selection_auto));
                return;
            }
            for (int i = 0; i < trackInformations.size(); i++) {
                TrackInformation track = trackInformations.get(i);
                if (track.isSelected()) {
                    PlayerControlView.this.settingsAdapter.setSubTextAtPosition(1, track.trackName);
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    abstract class TrackSelectionAdapter extends RecyclerView.Adapter<SubSettingViewHolder> {
        protected List<TrackInformation> tracks = new ArrayList();

        public abstract void init(List<TrackInformation> list);

        protected abstract void onBindViewHolderAtZeroPosition(SubSettingViewHolder subSettingViewHolder);

        protected abstract void onTrackSelection(String str);

        protected TrackSelectionAdapter() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public SubSettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(PlayerControlView.this.getContext()).inflate(R.layout.exo_styled_sub_settings_list_item, parent, false);
            return new SubSettingViewHolder(v);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(SubSettingViewHolder holder, int position) {
            final Player player = PlayerControlView.this.player;
            if (player == null) {
                return;
            }
            if (position == 0) {
                onBindViewHolderAtZeroPosition(holder);
                return;
            }
            final TrackInformation track = this.tracks.get(position - 1);
            final TrackGroup mediaTrackGroup = track.trackGroup.getMediaTrackGroup();
            TrackSelectionParameters params = player.getTrackSelectionParameters();
            boolean explicitlySelected = params.overrides.get(mediaTrackGroup) != null && track.isSelected();
            holder.textView.setText(track.trackName);
            holder.checkView.setVisibility(explicitlySelected ? 0 : 4);
            holder.itemView.setOnClickListener(new View.OnClickListener() { // from class: androidx.media3.ui.PlayerControlView$TrackSelectionAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.m166x45c3fb1a(player, mediaTrackGroup, track, view);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onBindViewHolder$0$androidx-media3-ui-PlayerControlView$TrackSelectionAdapter, reason: not valid java name */
        /* synthetic */ void m166x45c3fb1a(Player player, TrackGroup mediaTrackGroup, TrackInformation track, View v) {
            if (!player.isCommandAvailable(29)) {
                return;
            }
            TrackSelectionParameters trackSelectionParameters = player.getTrackSelectionParameters();
            player.setTrackSelectionParameters(trackSelectionParameters.buildUpon().setOverrideForType(new TrackSelectionOverride(mediaTrackGroup, ImmutableList.of(Integer.valueOf(track.trackIndex)))).setTrackTypeDisabled(track.trackGroup.getType(), false).build());
            onTrackSelection(track.trackName);
            PlayerControlView.this.settingsWindow.dismiss();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            if (this.tracks.isEmpty()) {
                return 0;
            }
            return this.tracks.size() + 1;
        }

        protected void clear() {
            this.tracks = Collections.emptyList();
        }
    }

    private static class SubSettingViewHolder extends RecyclerView.ViewHolder {
        public final View checkView;
        public final TextView textView;

        public SubSettingViewHolder(View itemView) {
            super(itemView);
            if (Util.SDK_INT < 26) {
                itemView.setFocusable(true);
            }
            this.textView = (TextView) itemView.findViewById(R.id.exo_text);
            this.checkView = itemView.findViewById(R.id.exo_check);
        }
    }
}
