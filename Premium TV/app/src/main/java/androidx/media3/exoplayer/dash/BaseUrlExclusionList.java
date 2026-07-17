package androidx.media3.exoplayer.dash;

import android.os.SystemClock;
import android.util.Pair;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.dash.manifest.BaseUrl;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class BaseUrlExclusionList {
    private final Map<Integer, Long> excludedPriorities;
    private final Map<String, Long> excludedServiceLocations;
    private final Random random;
    private final Map<List<Pair<String, Integer>>, BaseUrl> selectionsTaken;

    public BaseUrlExclusionList() {
        this(new Random());
    }

    BaseUrlExclusionList(Random random) {
        this.selectionsTaken = new HashMap();
        this.random = random;
        this.excludedServiceLocations = new HashMap();
        this.excludedPriorities = new HashMap();
    }

    public void exclude(BaseUrl baseUrlToExclude, long exclusionDurationMs) {
        long excludeUntilMs = SystemClock.elapsedRealtime() + exclusionDurationMs;
        addExclusion(baseUrlToExclude.serviceLocation, excludeUntilMs, this.excludedServiceLocations);
        if (baseUrlToExclude.priority != Integer.MIN_VALUE) {
            addExclusion(Integer.valueOf(baseUrlToExclude.priority), excludeUntilMs, this.excludedPriorities);
        }
    }

    public BaseUrl selectBaseUrl(List<BaseUrl> baseUrls) {
        List<BaseUrl> includedBaseUrls = applyExclusions(baseUrls);
        if (includedBaseUrls.size() < 2) {
            return (BaseUrl) Iterables.getFirst(includedBaseUrls, null);
        }
        Collections.sort(includedBaseUrls, new Comparator() { // from class: androidx.media3.exoplayer.dash.BaseUrlExclusionList$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return BaseUrlExclusionList.compareBaseUrl((BaseUrl) obj, (BaseUrl) obj2);
            }
        });
        List<Pair<String, Integer>> candidateKeys = new ArrayList<>();
        int lowestPriority = includedBaseUrls.get(0).priority;
        for (int i = 0; i < includedBaseUrls.size(); i++) {
            BaseUrl baseUrl = includedBaseUrls.get(i);
            if (lowestPriority != baseUrl.priority) {
                if (candidateKeys.size() != 1) {
                    break;
                }
                return includedBaseUrls.get(0);
            }
            candidateKeys.add(new Pair<>(baseUrl.serviceLocation, Integer.valueOf(baseUrl.weight)));
        }
        BaseUrl baseUrl2 = this.selectionsTaken.get(candidateKeys);
        if (baseUrl2 == null) {
            BaseUrl baseUrl3 = selectWeighted(includedBaseUrls.subList(0, candidateKeys.size()));
            this.selectionsTaken.put(candidateKeys, baseUrl3);
            return baseUrl3;
        }
        return baseUrl2;
    }

    public int getPriorityCountAfterExclusion(List<BaseUrl> baseUrls) {
        Set<Integer> priorities = new HashSet<>();
        List<BaseUrl> includedBaseUrls = applyExclusions(baseUrls);
        for (int i = 0; i < includedBaseUrls.size(); i++) {
            priorities.add(Integer.valueOf(includedBaseUrls.get(i).priority));
        }
        int i2 = priorities.size();
        return i2;
    }

    public static int getPriorityCount(List<BaseUrl> baseUrls) {
        Set<Integer> priorities = new HashSet<>();
        for (int i = 0; i < baseUrls.size(); i++) {
            priorities.add(Integer.valueOf(baseUrls.get(i).priority));
        }
        int i2 = priorities.size();
        return i2;
    }

    public void reset() {
        this.excludedServiceLocations.clear();
        this.excludedPriorities.clear();
        this.selectionsTaken.clear();
    }

    private List<BaseUrl> applyExclusions(List<BaseUrl> baseUrls) {
        long nowMs = SystemClock.elapsedRealtime();
        removeExpiredExclusions(nowMs, this.excludedServiceLocations);
        removeExpiredExclusions(nowMs, this.excludedPriorities);
        List<BaseUrl> includedBaseUrls = new ArrayList<>();
        for (int i = 0; i < baseUrls.size(); i++) {
            BaseUrl baseUrl = baseUrls.get(i);
            if (!this.excludedServiceLocations.containsKey(baseUrl.serviceLocation) && !this.excludedPriorities.containsKey(Integer.valueOf(baseUrl.priority))) {
                includedBaseUrls.add(baseUrl);
            }
        }
        return includedBaseUrls;
    }

    private BaseUrl selectWeighted(List<BaseUrl> candidates) {
        int totalWeight = 0;
        for (int i = 0; i < candidates.size(); i++) {
            totalWeight += candidates.get(i).weight;
        }
        int randomChoice = this.random.nextInt(totalWeight);
        int totalWeight2 = 0;
        for (int i2 = 0; i2 < candidates.size(); i2++) {
            BaseUrl baseUrl = candidates.get(i2);
            totalWeight2 += baseUrl.weight;
            if (randomChoice < totalWeight2) {
                return baseUrl;
            }
        }
        return (BaseUrl) Iterables.getLast(candidates);
    }

    private static <T> void addExclusion(T toExclude, long excludeUntilMs, Map<T, Long> currentExclusions) {
        if (currentExclusions.containsKey(toExclude)) {
            excludeUntilMs = Math.max(excludeUntilMs, ((Long) Util.castNonNull(currentExclusions.get(toExclude))).longValue());
        }
        currentExclusions.put(toExclude, Long.valueOf(excludeUntilMs));
    }

    private static <T> void removeExpiredExclusions(long nowMs, Map<T, Long> exclusions) {
        List<T> expiredExclusions = new ArrayList<>();
        for (Map.Entry<T, Long> entries : exclusions.entrySet()) {
            if (entries.getValue().longValue() <= nowMs) {
                expiredExclusions.add(entries.getKey());
            }
        }
        for (int i = 0; i < expiredExclusions.size(); i++) {
            exclusions.remove(expiredExclusions.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int compareBaseUrl(BaseUrl a, BaseUrl b) {
        int compare = Integer.compare(a.priority, b.priority);
        return compare != 0 ? compare : a.serviceLocation.compareTo(b.serviceLocation);
    }
}
