package androidx.media3.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaLibraryInfo;
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
import androidx.media3.common.util.RepeatModeUtil;
import androidx.media3.common.util.Util;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: classes.dex */
public class LegacyPlayerControlView extends FrameLayout {
    public static final int DEFAULT_REPEAT_TOGGLE_MODES = 0;
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5000;
    public static final int DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS = 200;
    private static final int MAX_UPDATE_INTERVAL_MS = 1000;
    public static final int MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100;
    private long[] adGroupTimesMs;
    private final float buttonAlphaDisabled;
    private final float buttonAlphaEnabled;
    private final ComponentListener componentListener;
    private long currentBufferedPosition;
    private long currentPosition;
    private long currentWindowOffset;
    private final TextView durationView;
    private long[] extraAdGroupTimesMs;
    private boolean[] extraPlayedAdGroups;
    private final View fastForwardButton;
    private final StringBuilder formatBuilder;
    private final Formatter formatter;
    private final Runnable hideAction;
    private long hideAtMs;
    private boolean isAttachedToWindow;
    private boolean multiWindowTimeBar;
    private final View nextButton;
    private final View pauseButton;
    private final Timeline.Period period;
    private final View playButton;
    private boolean[] playedAdGroups;
    private Player player;
    private final TextView positionView;
    private final View previousButton;
    private ProgressUpdateListener progressUpdateListener;
    private final String repeatAllButtonContentDescription;
    private final Drawable repeatAllButtonDrawable;
    private final String repeatOffButtonContentDescription;
    private final Drawable repeatOffButtonDrawable;
    private final String repeatOneButtonContentDescription;
    private final Drawable repeatOneButtonDrawable;
    private final ImageView repeatToggleButton;
    private int repeatToggleModes;
    private final View rewindButton;
    private boolean scrubbing;
    private boolean showFastForwardButton;
    private boolean showMultiWindowTimeBar;
    private boolean showNextButton;
    private boolean showPlayButtonIfSuppressed;
    private boolean showPreviousButton;
    private boolean showRewindButton;
    private boolean showShuffleButton;
    private int showTimeoutMs;
    private final ImageView shuffleButton;
    private final Drawable shuffleOffButtonDrawable;
    private final String shuffleOffContentDescription;
    private final Drawable shuffleOnButtonDrawable;
    private final String shuffleOnContentDescription;
    private final TimeBar timeBar;
    private int timeBarMinUpdateIntervalMs;
    private final Runnable updateProgressAction;
    private final CopyOnWriteArrayList<VisibilityListener> visibilityListeners;
    private final View vrButton;
    private final Timeline.Window window;

    public interface ProgressUpdateListener {
        void onProgressUpdate(long j, long j2);
    }

    public interface VisibilityListener {
        void onVisibilityChange(int i);
    }

    static {
        MediaLibraryInfo.registerModule("media3.ui");
    }

    public LegacyPlayerControlView(Context context) {
        this(context, null);
    }

    public LegacyPlayerControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LegacyPlayerControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, attrs);
    }

    public LegacyPlayerControlView(Context context, AttributeSet attrs, int defStyleAttr, AttributeSet playbackAttrs) {
        super(context, attrs, defStyleAttr);
        int controllerLayoutId = R.layout.exo_legacy_player_control_view;
        this.showPlayButtonIfSuppressed = true;
        this.showTimeoutMs = 5000;
        this.repeatToggleModes = 0;
        this.timeBarMinUpdateIntervalMs = 200;
        this.hideAtMs = C.TIME_UNSET;
        this.showRewindButton = true;
        this.showFastForwardButton = true;
        this.showPreviousButton = true;
        this.showNextButton = true;
        this.showShuffleButton = false;
        if (playbackAttrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(playbackAttrs, R.styleable.LegacyPlayerControlView, defStyleAttr, 0);
            try {
                this.showTimeoutMs = a.getInt(R.styleable.LegacyPlayerControlView_show_timeout, this.showTimeoutMs);
                controllerLayoutId = a.getResourceId(R.styleable.LegacyPlayerControlView_controller_layout_id, controllerLayoutId);
                this.repeatToggleModes = getRepeatToggleModes(a, this.repeatToggleModes);
                this.showRewindButton = a.getBoolean(R.styleable.LegacyPlayerControlView_show_rewind_button, this.showRewindButton);
                this.showFastForwardButton = a.getBoolean(R.styleable.LegacyPlayerControlView_show_fastforward_button, this.showFastForwardButton);
                this.showPreviousButton = a.getBoolean(R.styleable.LegacyPlayerControlView_show_previous_button, this.showPreviousButton);
                this.showNextButton = a.getBoolean(R.styleable.LegacyPlayerControlView_show_next_button, this.showNextButton);
                this.showShuffleButton = a.getBoolean(R.styleable.LegacyPlayerControlView_show_shuffle_button, this.showShuffleButton);
                setTimeBarMinUpdateInterval(a.getInt(R.styleable.LegacyPlayerControlView_time_bar_min_update_interval, this.timeBarMinUpdateIntervalMs));
                a.recycle();
            } catch (Throwable th) {
                a.recycle();
                throw th;
            }
        }
        this.visibilityListeners = new CopyOnWriteArrayList<>();
        this.period = new Timeline.Period();
        this.window = new Timeline.Window();
        this.formatBuilder = new StringBuilder();
        this.formatter = new Formatter(this.formatBuilder, Locale.getDefault());
        this.adGroupTimesMs = new long[0];
        this.playedAdGroups = new boolean[0];
        this.extraAdGroupTimesMs = new long[0];
        this.extraPlayedAdGroups = new boolean[0];
        this.componentListener = new ComponentListener();
        this.updateProgressAction = new Runnable() { // from class: androidx.media3.ui.LegacyPlayerControlView$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.updateProgress();
            }
        };
        this.hideAction = new Runnable() { // from class: androidx.media3.ui.LegacyPlayerControlView$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.hide();
            }
        };
        LayoutInflater.from(context).inflate(controllerLayoutId, this);
        setDescendantFocusability(262144);
        TimeBar customTimeBar = (TimeBar) findViewById(R.id.exo_progress);
        View timeBarPlaceholder = findViewById(R.id.exo_progress_placeholder);
        if (customTimeBar != null) {
            this.timeBar = customTimeBar;
        } else if (timeBarPlaceholder != null) {
            DefaultTimeBar defaultTimeBar = new DefaultTimeBar(context, null, 0, playbackAttrs);
            defaultTimeBar.setId(R.id.exo_progress);
            defaultTimeBar.setLayoutParams(timeBarPlaceholder.getLayoutParams());
            ViewGroup parent = (ViewGroup) timeBarPlaceholder.getParent();
            int timeBarIndex = parent.indexOfChild(timeBarPlaceholder);
            parent.removeView(timeBarPlaceholder);
            parent.addView(defaultTimeBar, timeBarIndex);
            this.timeBar = defaultTimeBar;
        } else {
            this.timeBar = null;
        }
        this.durationView = (TextView) findViewById(R.id.exo_duration);
        this.positionView = (TextView) findViewById(R.id.exo_position);
        if (this.timeBar != null) {
            this.timeBar.addListener(this.componentListener);
        }
        this.playButton = findViewById(R.id.exo_play);
        if (this.playButton != null) {
            this.playButton.setOnClickListener(this.componentListener);
        }
        this.pauseButton = findViewById(R.id.exo_pause);
        if (this.pauseButton != null) {
            this.pauseButton.setOnClickListener(this.componentListener);
        }
        this.previousButton = findViewById(R.id.exo_prev);
        if (this.previousButton != null) {
            this.previousButton.setOnClickListener(this.componentListener);
        }
        this.nextButton = findViewById(R.id.exo_next);
        if (this.nextButton != null) {
            this.nextButton.setOnClickListener(this.componentListener);
        }
        this.rewindButton = findViewById(R.id.exo_rew);
        if (this.rewindButton != null) {
            this.rewindButton.setOnClickListener(this.componentListener);
        }
        this.fastForwardButton = findViewById(R.id.exo_ffwd);
        if (this.fastForwardButton != null) {
            this.fastForwardButton.setOnClickListener(this.componentListener);
        }
        this.repeatToggleButton = (ImageView) findViewById(R.id.exo_repeat_toggle);
        if (this.repeatToggleButton != null) {
            this.repeatToggleButton.setOnClickListener(this.componentListener);
        }
        this.shuffleButton = (ImageView) findViewById(R.id.exo_shuffle);
        if (this.shuffleButton != null) {
            this.shuffleButton.setOnClickListener(this.componentListener);
        }
        this.vrButton = findViewById(R.id.exo_vr);
        setShowVrButton(false);
        updateButton(false, false, this.vrButton);
        Resources resources = context.getResources();
        this.buttonAlphaEnabled = resources.getInteger(R.integer.exo_media_button_opacity_percentage_enabled) / 100.0f;
        this.buttonAlphaDisabled = resources.getInteger(R.integer.exo_media_button_opacity_percentage_disabled) / 100.0f;
        this.repeatOffButtonDrawable = Util.getDrawable(context, resources, R.drawable.exo_legacy_controls_repeat_off);
        this.repeatOneButtonDrawable = Util.getDrawable(context, resources, R.drawable.exo_legacy_controls_repeat_one);
        this.repeatAllButtonDrawable = Util.getDrawable(context, resources, R.drawable.exo_legacy_controls_repeat_all);
        this.shuffleOnButtonDrawable = Util.getDrawable(context, resources, R.drawable.exo_legacy_controls_shuffle_on);
        this.shuffleOffButtonDrawable = Util.getDrawable(context, resources, R.drawable.exo_legacy_controls_shuffle_off);
        this.repeatOffButtonContentDescription = resources.getString(R.string.exo_controls_repeat_off_description);
        this.repeatOneButtonContentDescription = resources.getString(R.string.exo_controls_repeat_one_description);
        this.repeatAllButtonContentDescription = resources.getString(R.string.exo_controls_repeat_all_description);
        this.shuffleOnContentDescription = resources.getString(R.string.exo_controls_shuffle_on_description);
        this.shuffleOffContentDescription = resources.getString(R.string.exo_controls_shuffle_off_description);
        this.currentPosition = C.TIME_UNSET;
        this.currentBufferedPosition = C.TIME_UNSET;
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

    public void addVisibilityListener(VisibilityListener listener) {
        Assertions.checkNotNull(listener);
        this.visibilityListeners.add(listener);
    }

    public void removeVisibilityListener(VisibilityListener listener) {
        this.visibilityListeners.remove(listener);
    }

    public void setProgressUpdateListener(ProgressUpdateListener listener) {
        this.progressUpdateListener = listener;
    }

    public void setShowRewindButton(boolean showRewindButton) {
        this.showRewindButton = showRewindButton;
        updateNavigation();
    }

    public void setShowFastForwardButton(boolean showFastForwardButton) {
        this.showFastForwardButton = showFastForwardButton;
        updateNavigation();
    }

    public void setShowPreviousButton(boolean showPreviousButton) {
        this.showPreviousButton = showPreviousButton;
        updateNavigation();
    }

    public void setShowNextButton(boolean showNextButton) {
        this.showNextButton = showNextButton;
        updateNavigation();
    }

    public int getShowTimeoutMs() {
        return this.showTimeoutMs;
    }

    public void setShowTimeoutMs(int showTimeoutMs) {
        this.showTimeoutMs = showTimeoutMs;
        if (isVisible()) {
            hideAfterTimeout();
        }
    }

    public int getRepeatToggleModes() {
        return this.repeatToggleModes;
    }

    public void setRepeatToggleModes(int repeatToggleModes) {
        this.repeatToggleModes = repeatToggleModes;
        if (this.player != null) {
            int currentMode = this.player.getRepeatMode();
            if (repeatToggleModes == 0 && currentMode != 0) {
                this.player.setRepeatMode(0);
            } else if (repeatToggleModes == 1 && currentMode == 2) {
                this.player.setRepeatMode(1);
            } else if (repeatToggleModes == 2 && currentMode == 1) {
                this.player.setRepeatMode(2);
            }
        }
        updateRepeatModeButton();
    }

    public boolean getShowShuffleButton() {
        return this.showShuffleButton;
    }

    public void setShowShuffleButton(boolean showShuffleButton) {
        this.showShuffleButton = showShuffleButton;
        updateShuffleButton();
    }

    public boolean getShowVrButton() {
        return this.vrButton != null && this.vrButton.getVisibility() == 0;
    }

    public void setShowVrButton(boolean showVrButton) {
        if (this.vrButton != null) {
            this.vrButton.setVisibility(showVrButton ? 0 : 8);
        }
    }

    public void setVrButtonListener(View.OnClickListener onClickListener) {
        if (this.vrButton != null) {
            this.vrButton.setOnClickListener(onClickListener);
            updateButton(getShowVrButton(), onClickListener != null, this.vrButton);
        }
    }

    public void setTimeBarMinUpdateInterval(int minUpdateIntervalMs) {
        this.timeBarMinUpdateIntervalMs = Util.constrainValue(minUpdateIntervalMs, 16, 1000);
    }

    public void show() {
        if (!isVisible()) {
            setVisibility(0);
            for (VisibilityListener visibilityListener : this.visibilityListeners) {
                visibilityListener.onVisibilityChange(getVisibility());
            }
            updateAll();
            requestPlayPauseFocus();
            requestPlayPauseAccessibilityFocus();
        }
        hideAfterTimeout();
    }

    public void hide() {
        if (isVisible()) {
            setVisibility(8);
            for (VisibilityListener visibilityListener : this.visibilityListeners) {
                visibilityListener.onVisibilityChange(getVisibility());
            }
            removeCallbacks(this.updateProgressAction);
            removeCallbacks(this.hideAction);
            this.hideAtMs = C.TIME_UNSET;
        }
    }

    public boolean isVisible() {
        return getVisibility() == 0;
    }

    private void hideAfterTimeout() {
        removeCallbacks(this.hideAction);
        if (this.showTimeoutMs > 0) {
            this.hideAtMs = SystemClock.uptimeMillis() + ((long) this.showTimeoutMs);
            if (this.isAttachedToWindow) {
                postDelayed(this.hideAction, this.showTimeoutMs);
                return;
            }
            return;
        }
        this.hideAtMs = C.TIME_UNSET;
    }

    private void updateAll() {
        updatePlayPauseButton();
        updateNavigation();
        updateRepeatModeButton();
        updateShuffleButton();
        updateTimeline();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePlayPauseButton() {
        boolean z;
        if (!isVisible() || !this.isAttachedToWindow) {
            return;
        }
        boolean requestPlayPauseFocus = false;
        boolean requestPlayPauseAccessibilityFocus = false;
        boolean shouldShowPlayButton = Util.shouldShowPlayButton(this.player, this.showPlayButtonIfSuppressed);
        boolean z2 = true;
        if (this.playButton != null) {
            requestPlayPauseFocus = false | (!shouldShowPlayButton && this.playButton.isFocused());
            if (Util.SDK_INT < 21) {
                z = requestPlayPauseFocus;
            } else {
                z = !shouldShowPlayButton && Api21.isAccessibilityFocused(this.playButton);
            }
            requestPlayPauseAccessibilityFocus = false | z;
            this.playButton.setVisibility(shouldShowPlayButton ? 0 : 8);
        }
        if (this.pauseButton != null) {
            requestPlayPauseFocus |= shouldShowPlayButton && this.pauseButton.isFocused();
            if (Util.SDK_INT < 21) {
                z2 = requestPlayPauseFocus;
            } else if (!shouldShowPlayButton || !Api21.isAccessibilityFocused(this.pauseButton)) {
                z2 = false;
            }
            requestPlayPauseAccessibilityFocus |= z2;
            this.pauseButton.setVisibility(shouldShowPlayButton ? 8 : 0);
        }
        if (requestPlayPauseFocus) {
            requestPlayPauseFocus();
        }
        if (requestPlayPauseAccessibilityFocus) {
            requestPlayPauseAccessibilityFocus();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNavigation() {
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
            enableSeeking = player.isCommandAvailable(5);
            enablePrevious = player.isCommandAvailable(7);
            enableRewind = player.isCommandAvailable(11);
            enableFastForward = player.isCommandAvailable(12);
            enableNext = player.isCommandAvailable(9);
        }
        updateButton(this.showPreviousButton, enablePrevious, this.previousButton);
        updateButton(this.showRewindButton, enableRewind, this.rewindButton);
        updateButton(this.showFastForwardButton, enableFastForward, this.fastForwardButton);
        updateButton(this.showNextButton, enableNext, this.nextButton);
        if (this.timeBar != null) {
            this.timeBar.setEnabled(enableSeeking);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRepeatModeButton() {
        if (!isVisible() || !this.isAttachedToWindow || this.repeatToggleButton == null) {
            return;
        }
        if (this.repeatToggleModes == 0) {
            updateButton(false, false, this.repeatToggleButton);
            return;
        }
        Player player = this.player;
        ImageView imageView = this.repeatToggleButton;
        if (player == null) {
            updateButton(true, false, imageView);
            this.repeatToggleButton.setImageDrawable(this.repeatOffButtonDrawable);
            this.repeatToggleButton.setContentDescription(this.repeatOffButtonContentDescription);
            return;
        }
        updateButton(true, true, imageView);
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
        this.repeatToggleButton.setVisibility(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateShuffleButton() {
        String str;
        if (!isVisible() || !this.isAttachedToWindow || this.shuffleButton == null) {
            return;
        }
        Player player = this.player;
        if (!this.showShuffleButton) {
            updateButton(false, false, this.shuffleButton);
            return;
        }
        ImageView imageView = this.shuffleButton;
        if (player == null) {
            updateButton(true, false, imageView);
            this.shuffleButton.setImageDrawable(this.shuffleOffButtonDrawable);
            this.shuffleButton.setContentDescription(this.shuffleOffContentDescription);
            return;
        }
        updateButton(true, true, imageView);
        this.shuffleButton.setImageDrawable(player.getShuffleModeEnabled() ? this.shuffleOnButtonDrawable : this.shuffleOffButtonDrawable);
        ImageView imageView2 = this.shuffleButton;
        if (player.getShuffleModeEnabled()) {
            str = this.shuffleOnContentDescription;
        } else {
            str = this.shuffleOffContentDescription;
        }
        imageView2.setContentDescription(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTimeline() {
        int totalGroups;
        Player player = this.player;
        if (player != null) {
            boolean z = true;
            this.multiWindowTimeBar = this.showMultiWindowTimeBar && canShowMultiWindowTimeBar(player.getCurrentTimeline(), this.window);
            long j = 0;
            this.currentWindowOffset = 0L;
            long durationUs = 0;
            int adGroupCount = 0;
            Timeline timeline = player.getCurrentTimeline();
            if (!timeline.isEmpty()) {
                int currentWindowIndex = player.getCurrentMediaItemIndex();
                int firstWindowIndex = this.multiWindowTimeBar ? 0 : currentWindowIndex;
                int lastWindowIndex = this.multiWindowTimeBar ? timeline.getWindowCount() - 1 : currentWindowIndex;
                int i = firstWindowIndex;
                while (i <= lastWindowIndex) {
                    if (i == currentWindowIndex) {
                        this.currentWindowOffset = Util.usToMs(durationUs);
                    }
                    timeline.getWindow(i, this.window);
                    if (this.window.durationUs == C.TIME_UNSET) {
                        Assertions.checkState(z ^ this.multiWindowTimeBar);
                        break;
                    }
                    int j2 = this.window.firstPeriodIndex;
                    while (j2 <= this.window.lastPeriodIndex) {
                        timeline.getPeriod(j2, this.period);
                        int removedGroups = this.period.getRemovedAdGroupCount();
                        int totalGroups2 = this.period.getAdGroupCount();
                        long j3 = j;
                        int adGroupIndex = removedGroups;
                        while (adGroupIndex < totalGroups2) {
                            long adGroupTimeInPeriodUs = this.period.getAdGroupTimeUs(adGroupIndex);
                            if (adGroupTimeInPeriodUs == Long.MIN_VALUE) {
                                totalGroups = totalGroups2;
                                if (this.period.durationUs == C.TIME_UNSET) {
                                    player = player;
                                } else {
                                    adGroupTimeInPeriodUs = this.period.durationUs;
                                }
                                adGroupIndex++;
                                totalGroups2 = totalGroups;
                                player = player;
                            } else {
                                totalGroups = totalGroups2;
                            }
                            long adGroupTimeInWindowUs = adGroupTimeInPeriodUs + this.period.getPositionInWindowUs();
                            if (adGroupTimeInWindowUs < j3) {
                                player = player;
                            } else {
                                if (adGroupCount == this.adGroupTimesMs.length) {
                                    int newLength = this.adGroupTimesMs.length == 0 ? 1 : this.adGroupTimesMs.length * 2;
                                    this.adGroupTimesMs = Arrays.copyOf(this.adGroupTimesMs, newLength);
                                    this.playedAdGroups = Arrays.copyOf(this.playedAdGroups, newLength);
                                }
                                this.adGroupTimesMs[adGroupCount] = Util.usToMs(durationUs + adGroupTimeInWindowUs);
                                this.playedAdGroups[adGroupCount] = this.period.hasPlayedAdGroup(adGroupIndex);
                                adGroupCount++;
                            }
                            adGroupIndex++;
                            totalGroups2 = totalGroups;
                            player = player;
                        }
                        j2++;
                        j = j3;
                    }
                    durationUs += this.window.durationUs;
                    i++;
                    player = player;
                    z = true;
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
        if (player != null) {
            position = this.currentWindowOffset + player.getContentPosition();
            bufferedPosition = this.currentWindowOffset + player.getContentBufferedPosition();
        }
        boolean positionChanged = position != this.currentPosition;
        boolean bufferedPositionChanged = bufferedPosition != this.currentBufferedPosition;
        this.currentPosition = position;
        this.currentBufferedPosition = bufferedPosition;
        if (this.positionView != null && !this.scrubbing && positionChanged) {
            this.positionView.setText(Util.getStringForTime(this.formatBuilder, this.formatter, position));
        }
        if (this.timeBar != null) {
            this.timeBar.setPosition(position);
            this.timeBar.setBufferedPosition(bufferedPosition);
        }
        if (this.progressUpdateListener != null && (positionChanged || bufferedPositionChanged)) {
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

    private void requestPlayPauseFocus() {
        boolean shouldShowPlayButton = Util.shouldShowPlayButton(this.player, this.showPlayButtonIfSuppressed);
        if (shouldShowPlayButton && this.playButton != null) {
            this.playButton.requestFocus();
        } else if (!shouldShowPlayButton && this.pauseButton != null) {
            this.pauseButton.requestFocus();
        }
    }

    private void requestPlayPauseAccessibilityFocus() {
        boolean shouldShowPlayButton = Util.shouldShowPlayButton(this.player, this.showPlayButtonIfSuppressed);
        if (shouldShowPlayButton && this.playButton != null) {
            this.playButton.sendAccessibilityEvent(8);
        } else if (!shouldShowPlayButton && this.pauseButton != null) {
            this.pauseButton.sendAccessibilityEvent(8);
        }
    }

    private void updateButton(boolean visible, boolean enabled, View view) {
        if (view == null) {
            return;
        }
        view.setEnabled(enabled);
        view.setAlpha(enabled ? this.buttonAlphaEnabled : this.buttonAlphaDisabled);
        view.setVisibility(visible ? 0 : 8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void seekToTimeBarPosition(Player player, long positionMs) {
        int windowIndex;
        Timeline timeline = player.getCurrentTimeline();
        if (this.multiWindowTimeBar && !timeline.isEmpty()) {
            int windowCount = timeline.getWindowCount();
            windowIndex = 0;
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
        } else {
            windowIndex = player.getCurrentMediaItemIndex();
        }
        seekTo(player, windowIndex, positionMs);
        updateProgress();
    }

    private void seekTo(Player player, int windowIndex, long positionMs) {
        player.seekTo(windowIndex, positionMs);
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.isAttachedToWindow = true;
        if (this.hideAtMs != C.TIME_UNSET) {
            long delayMs = this.hideAtMs - SystemClock.uptimeMillis();
            if (delayMs <= 0) {
                hide();
            } else {
                postDelayed(this.hideAction, delayMs);
            }
        } else if (isVisible()) {
            hideAfterTimeout();
        }
        updateAll();
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.isAttachedToWindow = false;
        removeCallbacks(this.updateProgressAction);
        removeCallbacks(this.hideAction);
    }

    @Override // android.view.ViewGroup, android.view.View
    public final boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            removeCallbacks(this.hideAction);
        } else if (ev.getAction() == 1) {
            hideAfterTimeout();
        }
        return super.dispatchTouchEvent(ev);
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
                if (player.getPlaybackState() != 4) {
                    player.seekForward();
                    return true;
                }
                return true;
            }
            if (keyCode == 89) {
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
                        player.seekToNext();
                        return true;
                    case 88:
                        player.seekToPrevious();
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

    private static boolean isHandledMediaKey(int keyCode) {
        return keyCode == 90 || keyCode == 89 || keyCode == 85 || keyCode == 79 || keyCode == 126 || keyCode == 127 || keyCode == 87 || keyCode == 88;
    }

    private static boolean canShowMultiWindowTimeBar(Timeline timeline, Timeline.Window window) {
        if (timeline.getWindowCount() > 100) {
            return false;
        }
        int windowCount = timeline.getWindowCount();
        for (int i = 0; i < windowCount; i++) {
            if (timeline.getWindow(i, window).durationUs == C.TIME_UNSET) {
                return false;
            }
        }
        return true;
    }

    private static int getRepeatToggleModes(TypedArray a, int defaultValue) {
        return a.getInt(R.styleable.LegacyPlayerControlView_repeat_toggle_modes, defaultValue);
    }

    private final class ComponentListener implements Player.Listener, TimeBar.OnScrubListener, View.OnClickListener {
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
            if (events.containsAny(4, 5)) {
                LegacyPlayerControlView.this.updatePlayPauseButton();
            }
            if (events.containsAny(4, 5, 7)) {
                LegacyPlayerControlView.this.updateProgress();
            }
            if (events.contains(8)) {
                LegacyPlayerControlView.this.updateRepeatModeButton();
            }
            if (events.contains(9)) {
                LegacyPlayerControlView.this.updateShuffleButton();
            }
            if (events.containsAny(8, 9, 11, 0, 13)) {
                LegacyPlayerControlView.this.updateNavigation();
            }
            if (events.containsAny(11, 0)) {
                LegacyPlayerControlView.this.updateTimeline();
            }
        }

        @Override // androidx.media3.ui.TimeBar.OnScrubListener
        public void onScrubStart(TimeBar timeBar, long position) {
            LegacyPlayerControlView.this.scrubbing = true;
            if (LegacyPlayerControlView.this.positionView != null) {
                LegacyPlayerControlView.this.positionView.setText(Util.getStringForTime(LegacyPlayerControlView.this.formatBuilder, LegacyPlayerControlView.this.formatter, position));
            }
        }

        @Override // androidx.media3.ui.TimeBar.OnScrubListener
        public void onScrubMove(TimeBar timeBar, long position) {
            if (LegacyPlayerControlView.this.positionView != null) {
                LegacyPlayerControlView.this.positionView.setText(Util.getStringForTime(LegacyPlayerControlView.this.formatBuilder, LegacyPlayerControlView.this.formatter, position));
            }
        }

        @Override // androidx.media3.ui.TimeBar.OnScrubListener
        public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
            LegacyPlayerControlView.this.scrubbing = false;
            if (!canceled && LegacyPlayerControlView.this.player != null) {
                LegacyPlayerControlView.this.seekToTimeBarPosition(LegacyPlayerControlView.this.player, position);
            }
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            Player player = LegacyPlayerControlView.this.player;
            if (player != null) {
                if (LegacyPlayerControlView.this.nextButton != view) {
                    if (LegacyPlayerControlView.this.previousButton != view) {
                        if (LegacyPlayerControlView.this.fastForwardButton != view) {
                            if (LegacyPlayerControlView.this.rewindButton != view) {
                                if (LegacyPlayerControlView.this.playButton != view) {
                                    if (LegacyPlayerControlView.this.pauseButton != view) {
                                        if (LegacyPlayerControlView.this.repeatToggleButton != view) {
                                            if (LegacyPlayerControlView.this.shuffleButton == view) {
                                                player.setShuffleModeEnabled(!player.getShuffleModeEnabled());
                                                return;
                                            }
                                            return;
                                        }
                                        player.setRepeatMode(RepeatModeUtil.getNextRepeatMode(player.getRepeatMode(), LegacyPlayerControlView.this.repeatToggleModes));
                                        return;
                                    }
                                    Util.handlePauseButtonAction(player);
                                    return;
                                }
                                Util.handlePlayButtonAction(player);
                                return;
                            }
                            player.seekBack();
                            return;
                        }
                        if (player.getPlaybackState() != 4) {
                            player.seekForward();
                            return;
                        }
                        return;
                    }
                    player.seekToPrevious();
                    return;
                }
                player.seekToNext();
            }
        }
    }

    private static final class Api21 {
        private Api21() {
        }

        public static boolean isAccessibilityFocused(View view) {
            return view.isAccessibilityFocused();
        }
    }
}
