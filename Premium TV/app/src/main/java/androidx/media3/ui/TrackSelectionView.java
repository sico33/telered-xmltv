package androidx.media3.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.Assertions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class TrackSelectionView extends LinearLayout {
    private boolean allowAdaptiveSelections;
    private boolean allowMultipleOverrides;
    private final ComponentListener componentListener;
    private final CheckedTextView defaultView;
    private final CheckedTextView disableView;
    private final LayoutInflater inflater;
    private boolean isDisabled;
    private TrackSelectionListener listener;
    private final Map<TrackGroup, TrackSelectionOverride> overrides;
    private final int selectableItemBackgroundResourceId;
    private final List<Tracks.Group> trackGroups;
    private Comparator<TrackInfo> trackInfoComparator;
    private TrackNameProvider trackNameProvider;
    private CheckedTextView[][] trackViews;

    public interface TrackSelectionListener {
        void onTrackSelectionChanged(boolean z, Map<TrackGroup, TrackSelectionOverride> map);
    }

    public static Map<TrackGroup, TrackSelectionOverride> filterOverrides(Map<TrackGroup, TrackSelectionOverride> overrides, List<Tracks.Group> trackGroups, boolean allowMultipleOverrides) {
        HashMap<TrackGroup, TrackSelectionOverride> filteredOverrides = new HashMap<>();
        for (int i = 0; i < trackGroups.size(); i++) {
            Tracks.Group trackGroup = trackGroups.get(i);
            TrackSelectionOverride override = overrides.get(trackGroup.getMediaTrackGroup());
            if (override != null && (allowMultipleOverrides || filteredOverrides.isEmpty())) {
                filteredOverrides.put(override.mediaTrackGroup, override);
            }
        }
        return filteredOverrides;
    }

    public TrackSelectionView(Context context) {
        this(context, null);
    }

    public TrackSelectionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrackSelectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(1);
        setSaveFromParentEnabled(false);
        TypedArray attributeArray = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        this.selectableItemBackgroundResourceId = attributeArray.getResourceId(0, 0);
        attributeArray.recycle();
        this.inflater = LayoutInflater.from(context);
        this.componentListener = new ComponentListener();
        this.trackNameProvider = new DefaultTrackNameProvider(getResources());
        this.trackGroups = new ArrayList();
        this.overrides = new HashMap();
        this.disableView = (CheckedTextView) this.inflater.inflate(android.R.layout.simple_list_item_single_choice, (ViewGroup) this, false);
        this.disableView.setBackgroundResource(this.selectableItemBackgroundResourceId);
        this.disableView.setText(R.string.exo_track_selection_none);
        this.disableView.setEnabled(false);
        this.disableView.setFocusable(true);
        this.disableView.setOnClickListener(this.componentListener);
        this.disableView.setVisibility(8);
        addView(this.disableView);
        addView(this.inflater.inflate(R.layout.exo_list_divider, (ViewGroup) this, false));
        this.defaultView = (CheckedTextView) this.inflater.inflate(android.R.layout.simple_list_item_single_choice, (ViewGroup) this, false);
        this.defaultView.setBackgroundResource(this.selectableItemBackgroundResourceId);
        this.defaultView.setText(R.string.exo_track_selection_auto);
        this.defaultView.setEnabled(false);
        this.defaultView.setFocusable(true);
        this.defaultView.setOnClickListener(this.componentListener);
        addView(this.defaultView);
    }

    public void setAllowAdaptiveSelections(boolean allowAdaptiveSelections) {
        if (this.allowAdaptiveSelections != allowAdaptiveSelections) {
            this.allowAdaptiveSelections = allowAdaptiveSelections;
            updateViews();
        }
    }

    public void setAllowMultipleOverrides(boolean allowMultipleOverrides) {
        if (this.allowMultipleOverrides != allowMultipleOverrides) {
            this.allowMultipleOverrides = allowMultipleOverrides;
            if (!allowMultipleOverrides && this.overrides.size() > 1) {
                Map<TrackGroup, TrackSelectionOverride> filteredOverrides = filterOverrides(this.overrides, this.trackGroups, false);
                this.overrides.clear();
                this.overrides.putAll(filteredOverrides);
            }
            updateViews();
        }
    }

    public void setShowDisableOption(boolean showDisableOption) {
        this.disableView.setVisibility(showDisableOption ? 0 : 8);
    }

    public void setTrackNameProvider(TrackNameProvider trackNameProvider) {
        this.trackNameProvider = (TrackNameProvider) Assertions.checkNotNull(trackNameProvider);
        updateViews();
    }

    public void init(List<Tracks.Group> trackGroups, boolean isDisabled, Map<TrackGroup, TrackSelectionOverride> overrides, final Comparator<Format> trackFormatComparator, TrackSelectionListener listener) {
        Comparator<TrackInfo> comparator;
        this.isDisabled = isDisabled;
        if (trackFormatComparator == null) {
            comparator = null;
        } else {
            comparator = new Comparator() { // from class: androidx.media3.ui.TrackSelectionView$$ExternalSyntheticLambda0
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return trackFormatComparator.compare(((TrackSelectionView.TrackInfo) obj).getFormat(), ((TrackSelectionView.TrackInfo) obj2).getFormat());
                }
            };
        }
        this.trackInfoComparator = comparator;
        this.listener = listener;
        this.trackGroups.clear();
        this.trackGroups.addAll(trackGroups);
        this.overrides.clear();
        this.overrides.putAll(filterOverrides(overrides, trackGroups, this.allowMultipleOverrides));
        updateViews();
    }

    public boolean getIsDisabled() {
        return this.isDisabled;
    }

    public Map<TrackGroup, TrackSelectionOverride> getOverrides() {
        return this.overrides;
    }

    private void updateViews() {
        int trackViewLayoutId;
        for (int i = getChildCount() - 1; i >= 3; i--) {
            removeViewAt(i);
        }
        boolean zIsEmpty = this.trackGroups.isEmpty();
        CheckedTextView checkedTextView = this.disableView;
        if (zIsEmpty) {
            checkedTextView.setEnabled(false);
            this.defaultView.setEnabled(false);
            return;
        }
        checkedTextView.setEnabled(true);
        this.defaultView.setEnabled(true);
        this.trackViews = new CheckedTextView[this.trackGroups.size()][];
        boolean enableMultipleChoiceForMultipleOverrides = shouldEnableMultiGroupSelection();
        for (int trackGroupIndex = 0; trackGroupIndex < this.trackGroups.size(); trackGroupIndex++) {
            Tracks.Group trackGroup = this.trackGroups.get(trackGroupIndex);
            boolean enableMultipleChoiceForAdaptiveSelections = shouldEnableAdaptiveSelection(trackGroup);
            this.trackViews[trackGroupIndex] = new CheckedTextView[trackGroup.length];
            TrackInfo[] trackInfos = new TrackInfo[trackGroup.length];
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                trackInfos[trackIndex] = new TrackInfo(trackGroup, trackIndex);
            }
            if (this.trackInfoComparator != null) {
                Arrays.sort(trackInfos, this.trackInfoComparator);
            }
            for (int trackIndex2 = 0; trackIndex2 < trackInfos.length; trackIndex2++) {
                if (trackIndex2 == 0) {
                    addView(this.inflater.inflate(R.layout.exo_list_divider, (ViewGroup) this, false));
                }
                if (enableMultipleChoiceForAdaptiveSelections || enableMultipleChoiceForMultipleOverrides) {
                    trackViewLayoutId = android.R.layout.simple_list_item_multiple_choice;
                } else {
                    trackViewLayoutId = android.R.layout.simple_list_item_single_choice;
                }
                CheckedTextView trackView = (CheckedTextView) this.inflater.inflate(trackViewLayoutId, (ViewGroup) this, false);
                trackView.setBackgroundResource(this.selectableItemBackgroundResourceId);
                trackView.setText(this.trackNameProvider.getTrackName(trackInfos[trackIndex2].getFormat()));
                trackView.setTag(trackInfos[trackIndex2]);
                if (trackGroup.isTrackSupported(trackIndex2)) {
                    trackView.setFocusable(true);
                    trackView.setOnClickListener(this.componentListener);
                } else {
                    trackView.setFocusable(false);
                    trackView.setEnabled(false);
                }
                this.trackViews[trackGroupIndex][trackIndex2] = trackView;
                addView(trackView);
            }
        }
        updateViewStates();
    }

    private void updateViewStates() {
        this.disableView.setChecked(this.isDisabled);
        this.defaultView.setChecked(!this.isDisabled && this.overrides.size() == 0);
        for (int i = 0; i < this.trackViews.length; i++) {
            TrackSelectionOverride override = this.overrides.get(this.trackGroups.get(i).getMediaTrackGroup());
            for (int j = 0; j < this.trackViews[i].length; j++) {
                CheckedTextView[][] checkedTextViewArr = this.trackViews;
                if (override != null) {
                    TrackInfo trackInfo = (TrackInfo) Assertions.checkNotNull(checkedTextViewArr[i][j].getTag());
                    this.trackViews[i][j].setChecked(override.trackIndices.contains(Integer.valueOf(trackInfo.trackIndex)));
                } else {
                    checkedTextViewArr[i][j].setChecked(false);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onClick(View view) {
        if (view == this.disableView) {
            onDisableViewClicked();
        } else if (view == this.defaultView) {
            onDefaultViewClicked();
        } else {
            onTrackViewClicked(view);
        }
        updateViewStates();
        if (this.listener != null) {
            this.listener.onTrackSelectionChanged(getIsDisabled(), getOverrides());
        }
    }

    private void onDisableViewClicked() {
        this.isDisabled = true;
        this.overrides.clear();
    }

    private void onDefaultViewClicked() {
        this.isDisabled = false;
        this.overrides.clear();
    }

    private void onTrackViewClicked(View view) {
        this.isDisabled = false;
        TrackInfo trackInfo = (TrackInfo) Assertions.checkNotNull(view.getTag());
        TrackGroup mediaTrackGroup = trackInfo.trackGroup.getMediaTrackGroup();
        int trackIndex = trackInfo.trackIndex;
        TrackSelectionOverride override = this.overrides.get(mediaTrackGroup);
        if (override == null) {
            if (!this.allowMultipleOverrides && this.overrides.size() > 0) {
                this.overrides.clear();
            }
            this.overrides.put(mediaTrackGroup, new TrackSelectionOverride(mediaTrackGroup, ImmutableList.of(Integer.valueOf(trackIndex))));
            return;
        }
        ArrayList<Integer> trackIndices = new ArrayList<>(override.trackIndices);
        boolean isCurrentlySelected = ((CheckedTextView) view).isChecked();
        boolean isAdaptiveAllowed = shouldEnableAdaptiveSelection(trackInfo.trackGroup);
        boolean isUsingCheckBox = isAdaptiveAllowed || shouldEnableMultiGroupSelection();
        if (isCurrentlySelected && isUsingCheckBox) {
            trackIndices.remove(Integer.valueOf(trackIndex));
            boolean zIsEmpty = trackIndices.isEmpty();
            Map<TrackGroup, TrackSelectionOverride> map = this.overrides;
            if (zIsEmpty) {
                map.remove(mediaTrackGroup);
                return;
            } else {
                map.put(mediaTrackGroup, new TrackSelectionOverride(mediaTrackGroup, trackIndices));
                return;
            }
        }
        if (!isCurrentlySelected) {
            if (isAdaptiveAllowed) {
                trackIndices.add(Integer.valueOf(trackIndex));
                this.overrides.put(mediaTrackGroup, new TrackSelectionOverride(mediaTrackGroup, trackIndices));
            } else {
                this.overrides.put(mediaTrackGroup, new TrackSelectionOverride(mediaTrackGroup, ImmutableList.of(Integer.valueOf(trackIndex))));
            }
        }
    }

    private boolean shouldEnableAdaptiveSelection(Tracks.Group trackGroup) {
        return this.allowAdaptiveSelections && trackGroup.isAdaptiveSupported();
    }

    private boolean shouldEnableMultiGroupSelection() {
        return this.allowMultipleOverrides && this.trackGroups.size() > 1;
    }

    private class ComponentListener implements View.OnClickListener {
        private ComponentListener() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            TrackSelectionView.this.onClick(view);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class TrackInfo {
        public final Tracks.Group trackGroup;
        public final int trackIndex;

        public TrackInfo(Tracks.Group trackGroup, int trackIndex) {
            this.trackGroup = trackGroup;
            this.trackIndex = trackIndex;
        }

        public Format getFormat() {
            return this.trackGroup.getTrackFormat(this.trackIndex);
        }
    }
}
