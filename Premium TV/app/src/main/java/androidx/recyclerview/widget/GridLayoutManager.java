package androidx.recyclerview.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public class GridLayoutManager extends LinearLayoutManager {
    private static final boolean DEBUG = false;
    public static final int DEFAULT_SPAN_COUNT = -1;
    private static final String TAG = "GridLayoutManager";
    int[] mCachedBorders;
    final Rect mDecorInsets;
    boolean mPendingSpanCountChange;
    final SparseIntArray mPreLayoutSpanIndexCache;
    final SparseIntArray mPreLayoutSpanSizeCache;
    View[] mSet;
    int mSpanCount;
    SpanSizeLookup mSpanSizeLookup;
    private boolean mUsingSpansToEstimateScrollBarDimensions;

    public GridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mPendingSpanCountChange = false;
        this.mSpanCount = -1;
        this.mPreLayoutSpanSizeCache = new SparseIntArray();
        this.mPreLayoutSpanIndexCache = new SparseIntArray();
        this.mSpanSizeLookup = new DefaultSpanSizeLookup();
        this.mDecorInsets = new Rect();
        RecyclerView.LayoutManager.Properties properties = getProperties(context, attrs, defStyleAttr, defStyleRes);
        setSpanCount(properties.spanCount);
    }

    public GridLayoutManager(Context context, int spanCount) {
        super(context);
        this.mPendingSpanCountChange = false;
        this.mSpanCount = -1;
        this.mPreLayoutSpanSizeCache = new SparseIntArray();
        this.mPreLayoutSpanIndexCache = new SparseIntArray();
        this.mSpanSizeLookup = new DefaultSpanSizeLookup();
        this.mDecorInsets = new Rect();
        setSpanCount(spanCount);
    }

    public GridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        this.mPendingSpanCountChange = false;
        this.mSpanCount = -1;
        this.mPreLayoutSpanSizeCache = new SparseIntArray();
        this.mPreLayoutSpanIndexCache = new SparseIntArray();
        this.mSpanSizeLookup = new DefaultSpanSizeLookup();
        this.mDecorInsets = new Rect();
        setSpanCount(spanCount);
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager
    public void setStackFromEnd(boolean stackFromEnd) {
        if (stackFromEnd) {
            throw new UnsupportedOperationException("GridLayoutManager does not support stack from end. Consider using reverse layout");
        }
        super.setStackFromEnd(false);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public int getRowCountForAccessibility(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (this.mOrientation == 0) {
            return this.mSpanCount;
        }
        if (state.getItemCount() < 1) {
            return 0;
        }
        return getSpanGroupIndex(recycler, state, state.getItemCount() - 1) + 1;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public int getColumnCountForAccessibility(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (this.mOrientation == 1) {
            return this.mSpanCount;
        }
        if (state.getItemCount() < 1) {
            return 0;
        }
        return getSpanGroupIndex(recycler, state, state.getItemCount() - 1) + 1;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler, RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {
        ViewGroup.LayoutParams lp = host.getLayoutParams();
        if (!(lp instanceof LayoutParams)) {
            super.onInitializeAccessibilityNodeInfoForItem(host, info);
            return;
        }
        LayoutParams glp = (LayoutParams) lp;
        int spanGroupIndex = getSpanGroupIndex(recycler, state, glp.getViewLayoutPosition());
        if (this.mOrientation == 0) {
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(glp.getSpanIndex(), glp.getSpanSize(), spanGroupIndex, 1, false, false));
        } else {
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(spanGroupIndex, 1, glp.getSpanIndex(), glp.getSpanSize(), false, false));
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onInitializeAccessibilityNodeInfo(RecyclerView.Recycler recycler, RecyclerView.State state, AccessibilityNodeInfoCompat info) {
        super.onInitializeAccessibilityNodeInfo(recycler, state, info);
        info.setClassName(GridView.class.getName());
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout()) {
            cachePreLayoutSpanMapping();
        }
        super.onLayoutChildren(recycler, state);
        clearPreLayoutSpanMappingCache();
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        this.mPendingSpanCountChange = false;
    }

    private void clearPreLayoutSpanMappingCache() {
        this.mPreLayoutSpanSizeCache.clear();
        this.mPreLayoutSpanIndexCache.clear();
    }

    private void cachePreLayoutSpanMapping() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            int viewPosition = lp.getViewLayoutPosition();
            this.mPreLayoutSpanSizeCache.put(viewPosition, lp.getSpanSize());
            this.mPreLayoutSpanIndexCache.put(viewPosition, lp.getSpanIndex());
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
        this.mSpanSizeLookup.invalidateSpanGroupIndexCache();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onItemsChanged(RecyclerView recyclerView) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
        this.mSpanSizeLookup.invalidateSpanGroupIndexCache();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
        this.mSpanSizeLookup.invalidateSpanGroupIndexCache();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
        this.mSpanSizeLookup.invalidateSpanGroupIndexCache();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
        this.mSpanSizeLookup.invalidateSpanGroupIndexCache();
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (this.mOrientation == 0) {
            return new LayoutParams(-2, -1);
        }
        return new LayoutParams(-1, -2);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        this.mSpanSizeLookup = spanSizeLookup;
    }

    public SpanSizeLookup getSpanSizeLookup() {
        return this.mSpanSizeLookup;
    }

    private void updateMeasurements() {
        int totalSpace;
        if (getOrientation() == 1) {
            totalSpace = (getWidth() - getPaddingRight()) - getPaddingLeft();
        } else {
            int totalSpace2 = getHeight();
            totalSpace = (totalSpace2 - getPaddingBottom()) - getPaddingTop();
        }
        calculateItemBorders(totalSpace);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void setMeasuredDimension(Rect childrenBounds, int wSpec, int hSpec) {
        int usedHeight;
        int width;
        if (this.mCachedBorders == null) {
            super.setMeasuredDimension(childrenBounds, wSpec, hSpec);
        }
        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        if (this.mOrientation == 1) {
            int usedHeight2 = childrenBounds.height() + verticalPadding;
            width = chooseSize(hSpec, usedHeight2, getMinimumHeight());
            usedHeight = chooseSize(wSpec, this.mCachedBorders[this.mCachedBorders.length - 1] + horizontalPadding, getMinimumWidth());
        } else {
            int width2 = childrenBounds.width();
            int usedWidth = width2 + horizontalPadding;
            int width3 = chooseSize(wSpec, usedWidth, getMinimumWidth());
            usedHeight = width3;
            width = chooseSize(hSpec, this.mCachedBorders[this.mCachedBorders.length - 1] + verticalPadding, getMinimumHeight());
        }
        setMeasuredDimension(usedHeight, width);
    }

    private void calculateItemBorders(int totalSpace) {
        this.mCachedBorders = calculateItemBorders(this.mCachedBorders, this.mSpanCount, totalSpace);
    }

    static int[] calculateItemBorders(int[] cachedBorders, int spanCount, int totalSpace) {
        if (cachedBorders == null || cachedBorders.length != spanCount + 1 || cachedBorders[cachedBorders.length - 1] != totalSpace) {
            cachedBorders = new int[spanCount + 1];
        }
        cachedBorders[0] = 0;
        int sizePerSpan = totalSpace / spanCount;
        int sizePerSpanRemainder = totalSpace % spanCount;
        int consumedPixels = 0;
        int additionalSize = 0;
        for (int i = 1; i <= spanCount; i++) {
            int itemSize = sizePerSpan;
            additionalSize += sizePerSpanRemainder;
            if (additionalSize > 0 && spanCount - additionalSize < sizePerSpanRemainder) {
                itemSize++;
                additionalSize -= spanCount;
            }
            consumedPixels += itemSize;
            cachedBorders[i] = consumedPixels;
        }
        return cachedBorders;
    }

    int getSpaceForSpanRange(int startSpan, int spanSize) {
        if (this.mOrientation == 1 && isLayoutRTL()) {
            return this.mCachedBorders[this.mSpanCount - startSpan] - this.mCachedBorders[(this.mSpanCount - startSpan) - spanSize];
        }
        return this.mCachedBorders[startSpan + spanSize] - this.mCachedBorders[startSpan];
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager
    void onAnchorReady(RecyclerView.Recycler recycler, RecyclerView.State state, LinearLayoutManager.AnchorInfo anchorInfo, int itemDirection) {
        super.onAnchorReady(recycler, state, anchorInfo, itemDirection);
        updateMeasurements();
        if (state.getItemCount() > 0 && !state.isPreLayout()) {
            ensureAnchorIsInCorrectSpan(recycler, state, anchorInfo, itemDirection);
        }
        ensureViewSet();
    }

    private void ensureViewSet() {
        if (this.mSet == null || this.mSet.length != this.mSpanCount) {
            this.mSet = new View[this.mSpanCount];
        }
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        updateMeasurements();
        ensureViewSet();
        return super.scrollHorizontallyBy(dx, recycler, state);
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        updateMeasurements();
        ensureViewSet();
        return super.scrollVerticallyBy(dy, recycler, state);
    }

    private void ensureAnchorIsInCorrectSpan(RecyclerView.Recycler recycler, RecyclerView.State state, LinearLayoutManager.AnchorInfo anchorInfo, int itemDirection) {
        boolean layingOutInPrimaryDirection = itemDirection == 1;
        int span = getSpanIndex(recycler, state, anchorInfo.mPosition);
        if (!layingOutInPrimaryDirection) {
            int indexLimit = state.getItemCount() - 1;
            int pos = anchorInfo.mPosition;
            int bestSpan = span;
            while (pos < indexLimit) {
                int next = getSpanIndex(recycler, state, pos + 1);
                if (next <= bestSpan) {
                    break;
                }
                pos++;
                bestSpan = next;
            }
            anchorInfo.mPosition = pos;
            return;
        }
        while (span > 0 && anchorInfo.mPosition > 0) {
            anchorInfo.mPosition--;
            span = getSpanIndex(recycler, state, anchorInfo.mPosition);
        }
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager
    View findReferenceChild(RecyclerView.Recycler recycler, RecyclerView.State state, boolean layoutFromEnd, boolean traverseChildrenInReverseOrder) {
        int start = 0;
        int end = getChildCount();
        int diff = 1;
        if (traverseChildrenInReverseOrder) {
            start = getChildCount() - 1;
            end = -1;
            diff = -1;
        }
        int itemCount = state.getItemCount();
        ensureLayoutState();
        View invalidMatch = null;
        View outOfBoundsMatch = null;
        int boundsStart = this.mOrientationHelper.getStartAfterPadding();
        int boundsEnd = this.mOrientationHelper.getEndAfterPadding();
        for (int i = start; i != end; i += diff) {
            View view = getChildAt(i);
            int position = getPosition(view);
            if (position >= 0 && position < itemCount) {
                int span = getSpanIndex(recycler, state, position);
                if (span != 0) {
                    continue;
                } else if (((RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view;
                    }
                } else if (this.mOrientationHelper.getDecoratedStart(view) >= boundsEnd || this.mOrientationHelper.getDecoratedEnd(view) < boundsStart) {
                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view;
                    }
                } else {
                    return view;
                }
            }
        }
        return outOfBoundsMatch != null ? outOfBoundsMatch : invalidMatch;
    }

    private int getSpanGroupIndex(RecyclerView.Recycler recycler, RecyclerView.State state, int viewPosition) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getCachedSpanGroupIndex(viewPosition, this.mSpanCount);
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(viewPosition);
        if (adapterPosition == -1) {
            Log.w(TAG, "Cannot find span size for pre layout position. " + viewPosition);
            return 0;
        }
        return this.mSpanSizeLookup.getCachedSpanGroupIndex(adapterPosition, this.mSpanCount);
    }

    private int getSpanIndex(RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getCachedSpanIndex(pos, this.mSpanCount);
        }
        int cached = this.mPreLayoutSpanIndexCache.get(pos, -1);
        if (cached != -1) {
            return cached;
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            Log.w(TAG, "Cannot find span size for pre layout position. It is not cached, not in the adapter. Pos:" + pos);
            return 0;
        }
        return this.mSpanSizeLookup.getCachedSpanIndex(adapterPosition, this.mSpanCount);
    }

    private int getSpanSize(RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getSpanSize(pos);
        }
        int cached = this.mPreLayoutSpanSizeCache.get(pos, -1);
        if (cached != -1) {
            return cached;
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            Log.w(TAG, "Cannot find span size for pre layout position. It is not cached, not in the adapter. Pos:" + pos);
            return 1;
        }
        return this.mSpanSizeLookup.getSpanSize(adapterPosition);
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager
    void collectPrefetchPositionsForLayoutState(RecyclerView.State state, LinearLayoutManager.LayoutState layoutState, RecyclerView.LayoutManager.LayoutPrefetchRegistry layoutPrefetchRegistry) {
        int remainingSpan = this.mSpanCount;
        for (int count = 0; count < this.mSpanCount && layoutState.hasMore(state) && remainingSpan > 0; count++) {
            int pos = layoutState.mCurrentPosition;
            layoutPrefetchRegistry.addPosition(pos, Math.max(0, layoutState.mScrollingOffset));
            int spanSize = this.mSpanSizeLookup.getSpanSize(pos);
            remainingSpan -= spanSize;
            layoutState.mCurrentPosition += layoutState.mItemDirection;
        }
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager
    void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, LinearLayoutManager.LayoutState layoutState, LinearLayoutManager.LayoutChunkResult result) {
        int maxSize;
        int bottom;
        int right;
        int bottom2;
        int wSpec;
        int hSpec;
        boolean z;
        View view;
        GridLayoutManager gridLayoutManager = this;
        int otherDirSpecMode = gridLayoutManager.mOrientationHelper.getModeInOther();
        boolean flexibleInOtherDir = otherDirSpecMode != 1073741824;
        int currentOtherDirSize = gridLayoutManager.getChildCount() > 0 ? gridLayoutManager.mCachedBorders[gridLayoutManager.mSpanCount] : 0;
        if (flexibleInOtherDir) {
            gridLayoutManager.updateMeasurements();
        }
        boolean layingOutInPrimaryDirection = layoutState.mItemDirection == 1;
        int count = 0;
        int remainingSpan = gridLayoutManager.mSpanCount;
        if (!layingOutInPrimaryDirection) {
            int itemSpanIndex = gridLayoutManager.getSpanIndex(recycler, state, layoutState.mCurrentPosition);
            int itemSpanSize = gridLayoutManager.getSpanSize(recycler, state, layoutState.mCurrentPosition);
            remainingSpan = itemSpanIndex + itemSpanSize;
        }
        while (count < count && layoutState.hasMore(state) && remainingSpan > 0) {
            int pos = layoutState.mCurrentPosition;
            int spanSize = gridLayoutManager.getSpanSize(recycler, state, pos);
            if (spanSize > gridLayoutManager.mSpanCount) {
                throw new IllegalArgumentException("Item at position " + pos + " requires " + spanSize + " spans but GridLayoutManager has only " + gridLayoutManager.mSpanCount + " spans.");
            }
            remainingSpan -= spanSize;
            if (remainingSpan < 0 || (view = layoutState.next(recycler)) == null) {
                break;
            }
            gridLayoutManager.mSet[count] = view;
            count++;
        }
        if (count == 0) {
            result.mFinished = true;
            return;
        }
        int maxSize2 = 0;
        float maxSizeInOther = 0.0f;
        gridLayoutManager.assignSpans(recycler, state, count, layingOutInPrimaryDirection);
        int i = 0;
        while (i < count) {
            View view2 = gridLayoutManager.mSet[i];
            if (layoutState.mScrapList == null) {
                if (layingOutInPrimaryDirection) {
                    gridLayoutManager.addView(view2);
                    z = false;
                } else {
                    z = false;
                    gridLayoutManager.addView(view2, 0);
                }
            } else {
                z = false;
                if (layingOutInPrimaryDirection) {
                    gridLayoutManager.addDisappearingView(view2);
                } else {
                    gridLayoutManager.addDisappearingView(view2, 0);
                }
            }
            gridLayoutManager.calculateItemDecorationsForChild(view2, gridLayoutManager.mDecorInsets);
            gridLayoutManager.measureChild(view2, otherDirSpecMode, z);
            int size = gridLayoutManager.mOrientationHelper.getDecoratedMeasurement(view2);
            if (size > maxSize2) {
                maxSize2 = size;
            }
            int maxSize3 = maxSize2;
            float otherSize = (gridLayoutManager.mOrientationHelper.getDecoratedMeasurementInOther(view2) * 1.0f) / ((LayoutParams) view2.getLayoutParams()).mSpanSize;
            if (otherSize > maxSizeInOther) {
                maxSizeInOther = otherSize;
            }
            i++;
            maxSize2 = maxSize3;
        }
        if (!flexibleInOtherDir) {
            maxSize = maxSize2;
        } else {
            gridLayoutManager.guessMeasurement(maxSizeInOther, currentOtherDirSize);
            int maxSize4 = 0;
            for (int i2 = 0; i2 < count; i2++) {
                View view3 = gridLayoutManager.mSet[i2];
                gridLayoutManager.measureChild(view3, 1073741824, true);
                int size2 = gridLayoutManager.mOrientationHelper.getDecoratedMeasurement(view3);
                if (size2 > maxSize4) {
                    maxSize4 = size2;
                }
            }
            maxSize = maxSize4;
        }
        int i3 = 0;
        while (i3 < count) {
            View view4 = gridLayoutManager.mSet[i3];
            if (gridLayoutManager.mOrientationHelper.getDecoratedMeasurement(view4) != maxSize) {
                LayoutParams lp = (LayoutParams) view4.getLayoutParams();
                Rect decorInsets = lp.mDecorInsets;
                int i4 = decorInsets.top;
                int verticalInsets = i4 + decorInsets.bottom + lp.topMargin + lp.bottomMargin;
                int horizontalInsets = decorInsets.left + decorInsets.right + lp.leftMargin + lp.rightMargin;
                int totalSpaceInOther = gridLayoutManager.getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
                if (gridLayoutManager.mOrientation == 1) {
                    wSpec = getChildMeasureSpec(totalSpaceInOther, 1073741824, horizontalInsets, lp.width, false);
                    hSpec = View.MeasureSpec.makeMeasureSpec(maxSize - verticalInsets, 1073741824);
                } else {
                    wSpec = View.MeasureSpec.makeMeasureSpec(maxSize - horizontalInsets, 1073741824);
                    hSpec = getChildMeasureSpec(totalSpaceInOther, 1073741824, verticalInsets, lp.height, false);
                }
                gridLayoutManager.measureChildWithDecorationsAndMargin(view4, wSpec, hSpec, true);
            } else {
                otherDirSpecMode = otherDirSpecMode;
            }
            i3++;
            otherDirSpecMode = otherDirSpecMode;
            maxSizeInOther = maxSizeInOther;
        }
        result.mConsumed = maxSize;
        int left = 0;
        int left2 = 0;
        int top = 0;
        int bottom3 = 0;
        if (gridLayoutManager.mOrientation == 1) {
            if (layoutState.mLayoutDirection == -1) {
                bottom3 = layoutState.mOffset;
                top = bottom3 - maxSize;
            } else {
                top = layoutState.mOffset;
                bottom3 = top + maxSize;
            }
        } else if (layoutState.mLayoutDirection == -1) {
            left2 = layoutState.mOffset;
            left = left2 - maxSize;
        } else {
            left = layoutState.mOffset;
            left2 = left + maxSize;
        }
        int i5 = 0;
        while (true) {
            View[] viewArr = gridLayoutManager.mSet;
            if (i5 < count) {
                int left3 = left;
                View view5 = viewArr[i5];
                LayoutParams params = (LayoutParams) view5.getLayoutParams();
                int right2 = left2;
                if (gridLayoutManager.mOrientation == 1) {
                    if (gridLayoutManager.isLayoutRTL()) {
                        int right3 = gridLayoutManager.getPaddingLeft() + gridLayoutManager.mCachedBorders[gridLayoutManager.mSpanCount - params.mSpanIndex];
                        int i6 = bottom3;
                        bottom2 = right3;
                        right = right3 - gridLayoutManager.mOrientationHelper.getDecoratedMeasurementInOther(view5);
                        bottom = i6;
                    } else {
                        int right4 = gridLayoutManager.getPaddingLeft();
                        right = right4 + gridLayoutManager.mCachedBorders[params.mSpanIndex];
                        bottom = bottom3;
                        bottom2 = gridLayoutManager.mOrientationHelper.getDecoratedMeasurementInOther(view5) + right;
                    }
                } else {
                    top = gridLayoutManager.getPaddingTop() + gridLayoutManager.mCachedBorders[params.mSpanIndex];
                    bottom = top + gridLayoutManager.mOrientationHelper.getDecoratedMeasurementInOther(view5);
                    right = left3;
                    bottom2 = right2;
                }
                gridLayoutManager.layoutDecoratedWithMargins(view5, right, top, bottom2, bottom);
                if (params.isItemRemoved() || params.isItemChanged()) {
                    result.mIgnoreConsumed = true;
                }
                result.mFocusable |= view5.hasFocusable();
                i5++;
                gridLayoutManager = this;
                left = right;
                left2 = bottom2;
                bottom3 = bottom;
            } else {
                Arrays.fill(viewArr, (Object) null);
                return;
            }
        }
    }

    private void measureChild(View view, int otherDirParentSpecMode, boolean alreadyMeasured) {
        int hSpec;
        int wSpec;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        Rect decorInsets = lp.mDecorInsets;
        int verticalInsets = decorInsets.top + decorInsets.bottom + lp.topMargin + lp.bottomMargin;
        int horizontalInsets = decorInsets.left + decorInsets.right + lp.leftMargin + lp.rightMargin;
        int availableSpaceInOther = getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
        if (this.mOrientation == 1) {
            wSpec = getChildMeasureSpec(availableSpaceInOther, otherDirParentSpecMode, horizontalInsets, lp.width, false);
            hSpec = getChildMeasureSpec(this.mOrientationHelper.getTotalSpace(), getHeightMode(), verticalInsets, lp.height, true);
        } else {
            int wSpec2 = lp.height;
            hSpec = getChildMeasureSpec(availableSpaceInOther, otherDirParentSpecMode, verticalInsets, wSpec2, false);
            wSpec = getChildMeasureSpec(this.mOrientationHelper.getTotalSpace(), getWidthMode(), horizontalInsets, lp.width, true);
        }
        measureChildWithDecorationsAndMargin(view, wSpec, hSpec, alreadyMeasured);
    }

    private void guessMeasurement(float maxSizeInOther, int currentOtherDirSize) {
        int contentSize = Math.round(this.mSpanCount * maxSizeInOther);
        calculateItemBorders(Math.max(contentSize, currentOtherDirSize));
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec, boolean alreadyMeasured) {
        boolean measure;
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        if (alreadyMeasured) {
            measure = shouldReMeasureChild(child, widthSpec, heightSpec, lp);
        } else {
            measure = shouldMeasureChild(child, widthSpec, heightSpec, lp);
        }
        if (measure) {
            child.measure(widthSpec, heightSpec);
        }
    }

    private void assignSpans(RecyclerView.Recycler recycler, RecyclerView.State state, int count, boolean layingOutInPrimaryDirection) {
        int start;
        int end;
        int diff;
        if (layingOutInPrimaryDirection) {
            start = 0;
            end = count;
            diff = 1;
        } else {
            start = count - 1;
            end = -1;
            diff = -1;
        }
        int span = 0;
        for (int i = start; i != end; i += diff) {
            View view = this.mSet[i];
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            params.mSpanSize = getSpanSize(recycler, state, getPosition(view));
            params.mSpanIndex = span;
            span += params.mSpanSize;
        }
    }

    public int getSpanCount() {
        return this.mSpanCount;
    }

    public void setSpanCount(int spanCount) {
        if (spanCount == this.mSpanCount) {
            return;
        }
        this.mPendingSpanCountChange = true;
        if (spanCount < 1) {
            throw new IllegalArgumentException("Span count should be at least 1. Provided " + spanCount);
        }
        this.mSpanCount = spanCount;
        this.mSpanSizeLookup.invalidateSpanIndexCache();
        requestLayout();
    }

    public static abstract class SpanSizeLookup {
        final SparseIntArray mSpanIndexCache = new SparseIntArray();
        final SparseIntArray mSpanGroupIndexCache = new SparseIntArray();
        private boolean mCacheSpanIndices = false;
        private boolean mCacheSpanGroupIndices = false;

        public abstract int getSpanSize(int i);

        public void setSpanIndexCacheEnabled(boolean cacheSpanIndices) {
            if (!cacheSpanIndices) {
                this.mSpanGroupIndexCache.clear();
            }
            this.mCacheSpanIndices = cacheSpanIndices;
        }

        public void setSpanGroupIndexCacheEnabled(boolean cacheSpanGroupIndices) {
            if (!cacheSpanGroupIndices) {
                this.mSpanGroupIndexCache.clear();
            }
            this.mCacheSpanGroupIndices = cacheSpanGroupIndices;
        }

        public void invalidateSpanIndexCache() {
            this.mSpanIndexCache.clear();
        }

        public void invalidateSpanGroupIndexCache() {
            this.mSpanGroupIndexCache.clear();
        }

        public boolean isSpanIndexCacheEnabled() {
            return this.mCacheSpanIndices;
        }

        public boolean isSpanGroupIndexCacheEnabled() {
            return this.mCacheSpanGroupIndices;
        }

        int getCachedSpanIndex(int position, int spanCount) {
            if (!this.mCacheSpanIndices) {
                return getSpanIndex(position, spanCount);
            }
            int existing = this.mSpanIndexCache.get(position, -1);
            if (existing != -1) {
                return existing;
            }
            int value = getSpanIndex(position, spanCount);
            this.mSpanIndexCache.put(position, value);
            return value;
        }

        int getCachedSpanGroupIndex(int position, int spanCount) {
            if (!this.mCacheSpanGroupIndices) {
                return getSpanGroupIndex(position, spanCount);
            }
            int existing = this.mSpanGroupIndexCache.get(position, -1);
            if (existing != -1) {
                return existing;
            }
            int value = getSpanGroupIndex(position, spanCount);
            this.mSpanGroupIndexCache.put(position, value);
            return value;
        }

        public int getSpanIndex(int position, int spanCount) {
            int prevKey;
            int positionSpanSize = getSpanSize(position);
            if (positionSpanSize == spanCount) {
                return 0;
            }
            int span = 0;
            int startPos = 0;
            if (this.mCacheSpanIndices && (prevKey = findFirstKeyLessThan(this.mSpanIndexCache, position)) >= 0) {
                span = this.mSpanIndexCache.get(prevKey) + getSpanSize(prevKey);
                startPos = prevKey + 1;
            }
            for (int i = startPos; i < position; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                } else if (span > spanCount) {
                    span = size;
                }
            }
            int i2 = span + positionSpanSize;
            if (i2 > spanCount) {
                return 0;
            }
            return span;
        }

        static int findFirstKeyLessThan(SparseIntArray cache, int position) {
            int lo = 0;
            int hi = cache.size() - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int midVal = cache.keyAt(mid);
                if (midVal < position) {
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            int index = lo - 1;
            if (index >= 0 && index < cache.size()) {
                return cache.keyAt(index);
            }
            return -1;
        }

        public int getSpanGroupIndex(int adapterPosition, int spanCount) {
            int prevKey;
            int span = 0;
            int group = 0;
            int start = 0;
            if (this.mCacheSpanGroupIndices && (prevKey = findFirstKeyLessThan(this.mSpanGroupIndexCache, adapterPosition)) != -1) {
                group = this.mSpanGroupIndexCache.get(prevKey);
                start = prevKey + 1;
                span = getCachedSpanIndex(prevKey, spanCount) + getSpanSize(prevKey);
                if (span == spanCount) {
                    span = 0;
                    group++;
                }
            }
            int positionSpanSize = getSpanSize(adapterPosition);
            for (int i = start; i < adapterPosition; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                    group++;
                } else if (span > spanCount) {
                    span = size;
                    group++;
                }
            }
            int i2 = span + positionSpanSize;
            if (i2 > spanCount) {
                return group + 1;
            }
            return group;
        }
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public View onFocusSearchFailed(View focused, int direction, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int start;
        int inc;
        int limit;
        View prevFocusedChild;
        int focusableSpanGroupIndex;
        int i;
        int focusableWeakCandidateSpanIndex;
        int focusableWeakCandidateOverlap;
        RecyclerView.Recycler recycler2 = recycler;
        RecyclerView.State state2 = state;
        View prevFocusedChild2 = findContainingItemView(focused);
        if (prevFocusedChild2 == null) {
            return null;
        }
        LayoutParams lp = (LayoutParams) prevFocusedChild2.getLayoutParams();
        int prevSpanStart = lp.mSpanIndex;
        int prevSpanEnd = lp.mSpanIndex + lp.mSpanSize;
        View view = super.onFocusSearchFailed(focused, direction, recycler, state);
        if (view != null) {
            int layoutDir = convertFocusDirectionToLayoutDirection(direction);
            boolean ascend = (layoutDir == 1) != this.mShouldReverseLayout;
            if (ascend) {
                start = getChildCount() - 1;
                inc = -1;
                limit = -1;
            } else {
                start = 0;
                inc = 1;
                limit = getChildCount();
            }
            boolean preferLastSpan = this.mOrientation == 1 && isLayoutRTL();
            View focusableWeakCandidate = null;
            View unfocusableWeakCandidate = null;
            int focusableSpanGroupIndex2 = getSpanGroupIndex(recycler2, state2, start);
            int focusableWeakCandidateSpanIndex2 = -1;
            int focusableWeakCandidateOverlap2 = 0;
            int unfocusableWeakCandidateSpanIndex = -1;
            int layoutDir2 = 0;
            int i2 = start;
            while (i2 != limit) {
                int start2 = start;
                int spanGroupIndex = getSpanGroupIndex(recycler2, state2, i2);
                View candidate = getChildAt(i2);
                if (candidate == prevFocusedChild2) {
                    break;
                }
                if (candidate.hasFocusable() && spanGroupIndex != focusableSpanGroupIndex2) {
                    if (focusableWeakCandidate != null) {
                        break;
                    }
                    prevFocusedChild = prevFocusedChild2;
                    focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                    focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                    focusableSpanGroupIndex = focusableSpanGroupIndex2;
                    i = i2;
                } else {
                    LayoutParams candidateLp = (LayoutParams) candidate.getLayoutParams();
                    prevFocusedChild = prevFocusedChild2;
                    int candidateStart = candidateLp.mSpanIndex;
                    focusableSpanGroupIndex = focusableSpanGroupIndex2;
                    int focusableSpanGroupIndex3 = candidateLp.mSpanIndex;
                    int candidateEnd = focusableSpanGroupIndex3 + candidateLp.mSpanSize;
                    if (candidate.hasFocusable() && candidateStart == prevSpanStart && candidateEnd == prevSpanEnd) {
                        return candidate;
                    }
                    boolean assignAsWeek = false;
                    if ((candidate.hasFocusable() && focusableWeakCandidate == null) || (!candidate.hasFocusable() && unfocusableWeakCandidate == null)) {
                        assignAsWeek = true;
                        focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                        focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                        i = i2;
                    } else {
                        int maxStart = Math.max(candidateStart, prevSpanStart);
                        int minEnd = Math.min(candidateEnd, prevSpanEnd);
                        i = i2;
                        int i3 = minEnd - maxStart;
                        if (candidate.hasFocusable()) {
                            if (i3 > focusableWeakCandidateOverlap2) {
                                assignAsWeek = true;
                                focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                                focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                            } else {
                                if (i3 == focusableWeakCandidateOverlap2) {
                                    focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                                    if (preferLastSpan == (candidateStart > focusableWeakCandidateSpanIndex2)) {
                                        assignAsWeek = true;
                                        focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                                    }
                                } else {
                                    focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                                }
                                focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                            }
                        } else {
                            focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                            if (focusableWeakCandidate != null) {
                                focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                            } else {
                                focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                                if (isViewPartiallyVisible(candidate, false, true)) {
                                    if (i3 > layoutDir2) {
                                        assignAsWeek = true;
                                    } else if (i3 == layoutDir2) {
                                        if (preferLastSpan == (candidateStart > unfocusableWeakCandidateSpanIndex)) {
                                            assignAsWeek = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (assignAsWeek) {
                        if (candidate.hasFocusable()) {
                            int focusableWeakCandidateSpanIndex3 = candidateLp.mSpanIndex;
                            focusableWeakCandidateOverlap = Math.min(candidateEnd, prevSpanEnd) - Math.max(candidateStart, prevSpanStart);
                            focusableWeakCandidate = candidate;
                            focusableWeakCandidateSpanIndex2 = focusableWeakCandidateSpanIndex3;
                        } else {
                            unfocusableWeakCandidateSpanIndex = candidateLp.mSpanIndex;
                            unfocusableWeakCandidate = candidate;
                            layoutDir2 = Math.min(candidateEnd, prevSpanEnd) - Math.max(candidateStart, prevSpanStart);
                            focusableWeakCandidateSpanIndex2 = focusableWeakCandidateSpanIndex;
                        }
                    }
                    i2 = i + inc;
                    recycler2 = recycler;
                    state2 = state;
                    focusableWeakCandidateOverlap2 = focusableWeakCandidateOverlap;
                    start = start2;
                    prevFocusedChild2 = prevFocusedChild;
                    focusableSpanGroupIndex2 = focusableSpanGroupIndex;
                }
                focusableWeakCandidateSpanIndex2 = focusableWeakCandidateSpanIndex;
                i2 = i + inc;
                recycler2 = recycler;
                state2 = state;
                focusableWeakCandidateOverlap2 = focusableWeakCandidateOverlap;
                start = start2;
                prevFocusedChild2 = prevFocusedChild;
                focusableSpanGroupIndex2 = focusableSpanGroupIndex;
            }
            return focusableWeakCandidate != null ? focusableWeakCandidate : unfocusableWeakCandidate;
        }
        return null;
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public boolean supportsPredictiveItemAnimations() {
        return this.mPendingSavedState == null && !this.mPendingSpanCountChange;
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        if (this.mUsingSpansToEstimateScrollBarDimensions) {
            return computeScrollRangeWithSpanInfo(state);
        }
        return super.computeHorizontalScrollRange(state);
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public int computeVerticalScrollRange(RecyclerView.State state) {
        if (this.mUsingSpansToEstimateScrollBarDimensions) {
            return computeScrollRangeWithSpanInfo(state);
        }
        return super.computeVerticalScrollRange(state);
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        if (this.mUsingSpansToEstimateScrollBarDimensions) {
            return computeScrollOffsetWithSpanInfo(state);
        }
        return super.computeHorizontalScrollOffset(state);
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        if (this.mUsingSpansToEstimateScrollBarDimensions) {
            return computeScrollOffsetWithSpanInfo(state);
        }
        return super.computeVerticalScrollOffset(state);
    }

    public void setUsingSpansToEstimateScrollbarDimensions(boolean useSpansToEstimateScrollBarDimensions) {
        this.mUsingSpansToEstimateScrollBarDimensions = useSpansToEstimateScrollBarDimensions;
    }

    public boolean isUsingSpansToEstimateScrollbarDimensions() {
        return this.mUsingSpansToEstimateScrollBarDimensions;
    }

    private int computeScrollRangeWithSpanInfo(RecyclerView.State state) {
        if (getChildCount() == 0 || state.getItemCount() == 0) {
            return 0;
        }
        ensureLayoutState();
        View startChild = findFirstVisibleChildClosestToStart(!isSmoothScrollbarEnabled(), true);
        View endChild = findFirstVisibleChildClosestToEnd(!isSmoothScrollbarEnabled(), true);
        if (startChild == null || endChild == null) {
            return 0;
        }
        if (!isSmoothScrollbarEnabled()) {
            return this.mSpanSizeLookup.getCachedSpanGroupIndex(state.getItemCount() - 1, this.mSpanCount) + 1;
        }
        int laidOutArea = this.mOrientationHelper.getDecoratedEnd(endChild) - this.mOrientationHelper.getDecoratedStart(startChild);
        int firstVisibleSpan = this.mSpanSizeLookup.getCachedSpanGroupIndex(getPosition(startChild), this.mSpanCount);
        int lastVisibleSpan = this.mSpanSizeLookup.getCachedSpanGroupIndex(getPosition(endChild), this.mSpanCount);
        int totalSpans = this.mSpanSizeLookup.getCachedSpanGroupIndex(state.getItemCount() - 1, this.mSpanCount) + 1;
        int laidOutSpans = (lastVisibleSpan - firstVisibleSpan) + 1;
        return (int) ((laidOutArea / laidOutSpans) * totalSpans);
    }

    private int computeScrollOffsetWithSpanInfo(RecyclerView.State state) {
        int spansBefore;
        if (getChildCount() == 0 || state.getItemCount() == 0) {
            return 0;
        }
        ensureLayoutState();
        boolean smoothScrollEnabled = isSmoothScrollbarEnabled();
        View startChild = findFirstVisibleChildClosestToStart(!smoothScrollEnabled, true);
        View endChild = findFirstVisibleChildClosestToEnd(!smoothScrollEnabled, true);
        if (startChild == null || endChild == null) {
            return 0;
        }
        int startChildSpan = this.mSpanSizeLookup.getCachedSpanGroupIndex(getPosition(startChild), this.mSpanCount);
        int endChildSpan = this.mSpanSizeLookup.getCachedSpanGroupIndex(getPosition(endChild), this.mSpanCount);
        int minSpan = Math.min(startChildSpan, endChildSpan);
        int maxSpan = Math.max(startChildSpan, endChildSpan);
        int totalSpans = this.mSpanSizeLookup.getCachedSpanGroupIndex(state.getItemCount() - 1, this.mSpanCount) + 1;
        if (this.mShouldReverseLayout) {
            spansBefore = Math.max(0, (totalSpans - maxSpan) - 1);
        } else {
            spansBefore = Math.max(0, minSpan);
        }
        if (!smoothScrollEnabled) {
            return spansBefore;
        }
        int laidOutArea = Math.abs(this.mOrientationHelper.getDecoratedEnd(endChild) - this.mOrientationHelper.getDecoratedStart(startChild));
        int firstVisibleSpan = this.mSpanSizeLookup.getCachedSpanGroupIndex(getPosition(startChild), this.mSpanCount);
        int lastVisibleSpan = this.mSpanSizeLookup.getCachedSpanGroupIndex(getPosition(endChild), this.mSpanCount);
        int laidOutSpans = (lastVisibleSpan - firstVisibleSpan) + 1;
        float avgSizePerSpan = laidOutArea / laidOutSpans;
        return Math.round((spansBefore * avgSizePerSpan) + (this.mOrientationHelper.getStartAfterPadding() - this.mOrientationHelper.getDecoratedStart(startChild)));
    }

    public static final class DefaultSpanSizeLookup extends SpanSizeLookup {
        @Override // androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
        public int getSpanSize(int position) {
            return 1;
        }

        @Override // androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
        public int getSpanIndex(int position, int spanCount) {
            return position % spanCount;
        }
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {
        public static final int INVALID_SPAN_ID = -1;
        int mSpanIndex;
        int mSpanSize;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.mSpanIndex = -1;
            this.mSpanSize = 0;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.mSpanIndex = -1;
            this.mSpanSize = 0;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            this.mSpanIndex = -1;
            this.mSpanSize = 0;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.mSpanIndex = -1;
            this.mSpanSize = 0;
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
            this.mSpanIndex = -1;
            this.mSpanSize = 0;
        }

        public int getSpanIndex() {
            return this.mSpanIndex;
        }

        public int getSpanSize() {
            return this.mSpanSize;
        }
    }
}
