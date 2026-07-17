package androidx.media3.exoplayer.analytics;

import android.util.Base64;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.source.MediaSource;
import com.google.common.base.Supplier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultPlaybackSessionManager implements PlaybackSessionManager {
    public static final Supplier<String> DEFAULT_SESSION_ID_GENERATOR = new Supplier() { // from class: androidx.media3.exoplayer.analytics.DefaultPlaybackSessionManager$$ExternalSyntheticLambda0
        @Override // com.google.common.base.Supplier
        public final Object get() {
            return DefaultPlaybackSessionManager.generateDefaultSessionId();
        }
    };
    private static final Random RANDOM = new Random();
    private static final int SESSION_ID_LENGTH = 12;
    private String currentSessionId;
    private Timeline currentTimeline;
    private long lastRemovedCurrentWindowSequenceNumber;
    private PlaybackSessionManager.Listener listener;
    private final Timeline.Period period;
    private final Supplier<String> sessionIdGenerator;
    private final HashMap<String, SessionDescriptor> sessions;
    private final Timeline.Window window;

    public DefaultPlaybackSessionManager() {
        this(DEFAULT_SESSION_ID_GENERATOR);
    }

    public DefaultPlaybackSessionManager(Supplier<String> sessionIdGenerator) {
        this.sessionIdGenerator = sessionIdGenerator;
        this.window = new Timeline.Window();
        this.period = new Timeline.Period();
        this.sessions = new HashMap<>();
        this.currentTimeline = Timeline.EMPTY;
        this.lastRemovedCurrentWindowSequenceNumber = -1L;
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager
    public void setListener(PlaybackSessionManager.Listener listener) {
        this.listener = listener;
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager
    public synchronized String getSessionForMediaPeriodId(Timeline timeline, MediaSource.MediaPeriodId mediaPeriodId) {
        int windowIndex;
        windowIndex = timeline.getPeriodByUid(mediaPeriodId.periodUid, this.period).windowIndex;
        return getOrAddSession(windowIndex, mediaPeriodId).sessionId;
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager
    public synchronized boolean belongsToSession(AnalyticsListener.EventTime eventTime, String sessionId) {
        SessionDescriptor sessionDescriptor = this.sessions.get(sessionId);
        if (sessionDescriptor == null) {
            return false;
        }
        sessionDescriptor.maybeSetWindowSequenceNumber(eventTime.windowIndex, eventTime.mediaPeriodId);
        return sessionDescriptor.belongsToSession(eventTime.windowIndex, eventTime.mediaPeriodId);
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager
    public synchronized void updateSessions(AnalyticsListener.EventTime eventTime) {
        Assertions.checkNotNull(this.listener);
        if (eventTime.timeline.isEmpty()) {
            return;
        }
        if (eventTime.mediaPeriodId != null) {
            if (eventTime.mediaPeriodId.windowSequenceNumber < getMinWindowSequenceNumber()) {
                return;
            }
            SessionDescriptor currentSession = this.sessions.get(this.currentSessionId);
            if (currentSession != null && currentSession.windowSequenceNumber == -1 && currentSession.windowIndex != eventTime.windowIndex) {
                return;
            }
        }
        SessionDescriptor eventSession = getOrAddSession(eventTime.windowIndex, eventTime.mediaPeriodId);
        if (this.currentSessionId == null) {
            this.currentSessionId = eventSession.sessionId;
        }
        if (eventTime.mediaPeriodId != null && eventTime.mediaPeriodId.isAd()) {
            MediaSource.MediaPeriodId contentMediaPeriodId = new MediaSource.MediaPeriodId(eventTime.mediaPeriodId.periodUid, eventTime.mediaPeriodId.windowSequenceNumber, eventTime.mediaPeriodId.adGroupIndex);
            SessionDescriptor contentSession = getOrAddSession(eventTime.windowIndex, contentMediaPeriodId);
            if (!contentSession.isCreated) {
                contentSession.isCreated = true;
                eventTime.timeline.getPeriodByUid(eventTime.mediaPeriodId.periodUid, this.period);
                long adGroupPositionMs = Util.usToMs(this.period.getAdGroupTimeUs(eventTime.mediaPeriodId.adGroupIndex)) + this.period.getPositionInWindowMs();
                AnalyticsListener.EventTime eventTimeForContent = new AnalyticsListener.EventTime(eventTime.realtimeMs, eventTime.timeline, eventTime.windowIndex, contentMediaPeriodId, Math.max(0L, adGroupPositionMs), eventTime.currentTimeline, eventTime.currentWindowIndex, eventTime.currentMediaPeriodId, eventTime.currentPlaybackPositionMs, eventTime.totalBufferedDurationMs);
                this.listener.onSessionCreated(eventTimeForContent, contentSession.sessionId);
            }
        }
        if (!eventSession.isCreated) {
            eventSession.isCreated = true;
            this.listener.onSessionCreated(eventTime, eventSession.sessionId);
        }
        if (eventSession.sessionId.equals(this.currentSessionId) && !eventSession.isActive) {
            eventSession.isActive = true;
            this.listener.onSessionActive(eventTime, eventSession.sessionId);
        }
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager
    public synchronized void updateSessionsWithTimelineChange(AnalyticsListener.EventTime eventTime) {
        Assertions.checkNotNull(this.listener);
        Timeline previousTimeline = this.currentTimeline;
        this.currentTimeline = eventTime.timeline;
        Iterator<SessionDescriptor> iterator = this.sessions.values().iterator();
        while (iterator.hasNext()) {
            SessionDescriptor session = iterator.next();
            if (!session.tryResolvingToNewTimeline(previousTimeline, this.currentTimeline) || session.isFinishedAtEventTime(eventTime)) {
                iterator.remove();
                if (session.isCreated) {
                    if (session.sessionId.equals(this.currentSessionId)) {
                        clearCurrentSession(session);
                    }
                    this.listener.onSessionFinished(eventTime, session.sessionId, false);
                }
            }
        }
        updateCurrentSession(eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager
    public synchronized void updateSessionsWithDiscontinuity(AnalyticsListener.EventTime eventTime, int reason) {
        Assertions.checkNotNull(this.listener);
        boolean hasAutomaticTransition = reason == 0;
        Iterator<SessionDescriptor> iterator = this.sessions.values().iterator();
        while (iterator.hasNext()) {
            SessionDescriptor session = iterator.next();
            if (session.isFinishedAtEventTime(eventTime)) {
                iterator.remove();
                if (session.isCreated) {
                    boolean isRemovingCurrentSession = session.sessionId.equals(this.currentSessionId);
                    boolean isAutomaticTransition = hasAutomaticTransition && isRemovingCurrentSession && session.isActive;
                    if (isRemovingCurrentSession) {
                        clearCurrentSession(session);
                    }
                    this.listener.onSessionFinished(eventTime, session.sessionId, isAutomaticTransition);
                }
            }
        }
        updateCurrentSession(eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager
    public synchronized String getActiveSessionId() {
        return this.currentSessionId;
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager
    public synchronized void finishAllSessions(AnalyticsListener.EventTime eventTime) {
        if (this.currentSessionId != null) {
            clearCurrentSession((SessionDescriptor) Assertions.checkNotNull(this.sessions.get(this.currentSessionId)));
        }
        Iterator<SessionDescriptor> iterator = this.sessions.values().iterator();
        while (iterator.hasNext()) {
            SessionDescriptor session = iterator.next();
            iterator.remove();
            if (session.isCreated && this.listener != null) {
                this.listener.onSessionFinished(eventTime, session.sessionId, false);
            }
        }
    }

    @RequiresNonNull({"listener"})
    private void updateCurrentSession(AnalyticsListener.EventTime eventTime) {
        if (eventTime.timeline.isEmpty()) {
            if (this.currentSessionId != null) {
                clearCurrentSession((SessionDescriptor) Assertions.checkNotNull(this.sessions.get(this.currentSessionId)));
                return;
            }
            return;
        }
        SessionDescriptor previousSessionDescriptor = this.sessions.get(this.currentSessionId);
        SessionDescriptor currentSessionDescriptor = getOrAddSession(eventTime.windowIndex, eventTime.mediaPeriodId);
        this.currentSessionId = currentSessionDescriptor.sessionId;
        updateSessions(eventTime);
        if (eventTime.mediaPeriodId == null || !eventTime.mediaPeriodId.isAd()) {
            return;
        }
        if (previousSessionDescriptor == null || previousSessionDescriptor.windowSequenceNumber != eventTime.mediaPeriodId.windowSequenceNumber || previousSessionDescriptor.adMediaPeriodId == null || previousSessionDescriptor.adMediaPeriodId.adGroupIndex != eventTime.mediaPeriodId.adGroupIndex || previousSessionDescriptor.adMediaPeriodId.adIndexInAdGroup != eventTime.mediaPeriodId.adIndexInAdGroup) {
            MediaSource.MediaPeriodId contentMediaPeriodId = new MediaSource.MediaPeriodId(eventTime.mediaPeriodId.periodUid, eventTime.mediaPeriodId.windowSequenceNumber);
            SessionDescriptor contentSession = getOrAddSession(eventTime.windowIndex, contentMediaPeriodId);
            this.listener.onAdPlaybackStarted(eventTime, contentSession.sessionId, currentSessionDescriptor.sessionId);
        }
    }

    private void clearCurrentSession(SessionDescriptor currentSession) {
        if (currentSession.windowSequenceNumber != -1) {
            this.lastRemovedCurrentWindowSequenceNumber = currentSession.windowSequenceNumber;
        }
        this.currentSessionId = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getMinWindowSequenceNumber() {
        SessionDescriptor currentSession = this.sessions.get(this.currentSessionId);
        if (currentSession == null || currentSession.windowSequenceNumber == -1) {
            return this.lastRemovedCurrentWindowSequenceNumber + 1;
        }
        return currentSession.windowSequenceNumber;
    }

    private SessionDescriptor getOrAddSession(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        SessionDescriptor bestMatch = null;
        long bestMatchWindowSequenceNumber = Long.MAX_VALUE;
        for (SessionDescriptor sessionDescriptor : this.sessions.values()) {
            sessionDescriptor.maybeSetWindowSequenceNumber(windowIndex, mediaPeriodId);
            if (sessionDescriptor.belongsToSession(windowIndex, mediaPeriodId)) {
                long windowSequenceNumber = sessionDescriptor.windowSequenceNumber;
                if (windowSequenceNumber == -1 || windowSequenceNumber < bestMatchWindowSequenceNumber) {
                    bestMatch = sessionDescriptor;
                    bestMatchWindowSequenceNumber = windowSequenceNumber;
                } else if (windowSequenceNumber == bestMatchWindowSequenceNumber && ((SessionDescriptor) Util.castNonNull(bestMatch)).adMediaPeriodId != null && sessionDescriptor.adMediaPeriodId != null) {
                    bestMatch = sessionDescriptor;
                }
            }
        }
        if (bestMatch == null) {
            String sessionId = this.sessionIdGenerator.get();
            SessionDescriptor bestMatch2 = new SessionDescriptor(sessionId, windowIndex, mediaPeriodId);
            this.sessions.put(sessionId, bestMatch2);
            return bestMatch2;
        }
        return bestMatch;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String generateDefaultSessionId() {
        byte[] randomBytes = new byte[12];
        RANDOM.nextBytes(randomBytes);
        return Base64.encodeToString(randomBytes, 10);
    }

    private final class SessionDescriptor {
        private MediaSource.MediaPeriodId adMediaPeriodId;
        private boolean isActive;
        private boolean isCreated;
        private final String sessionId;
        private int windowIndex;
        private long windowSequenceNumber;

        public SessionDescriptor(String sessionId, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            this.sessionId = sessionId;
            this.windowIndex = windowIndex;
            this.windowSequenceNumber = mediaPeriodId == null ? -1L : mediaPeriodId.windowSequenceNumber;
            if (mediaPeriodId != null && mediaPeriodId.isAd()) {
                this.adMediaPeriodId = mediaPeriodId;
            }
        }

        public boolean tryResolvingToNewTimeline(Timeline oldTimeline, Timeline newTimeline) {
            this.windowIndex = resolveWindowIndexToNewTimeline(oldTimeline, newTimeline, this.windowIndex);
            if (this.windowIndex == -1) {
                return false;
            }
            if (this.adMediaPeriodId == null) {
                return true;
            }
            int newPeriodIndex = newTimeline.getIndexOfPeriod(this.adMediaPeriodId.periodUid);
            return newPeriodIndex != -1;
        }

        public boolean belongsToSession(int eventWindowIndex, MediaSource.MediaPeriodId eventMediaPeriodId) {
            if (eventMediaPeriodId == null) {
                return eventWindowIndex == this.windowIndex;
            }
            if (this.adMediaPeriodId == null) {
                return !eventMediaPeriodId.isAd() && eventMediaPeriodId.windowSequenceNumber == this.windowSequenceNumber;
            }
            return eventMediaPeriodId.windowSequenceNumber == this.adMediaPeriodId.windowSequenceNumber && eventMediaPeriodId.adGroupIndex == this.adMediaPeriodId.adGroupIndex && eventMediaPeriodId.adIndexInAdGroup == this.adMediaPeriodId.adIndexInAdGroup;
        }

        public void maybeSetWindowSequenceNumber(int eventWindowIndex, MediaSource.MediaPeriodId eventMediaPeriodId) {
            if (this.windowSequenceNumber == -1 && eventWindowIndex == this.windowIndex && eventMediaPeriodId != null && eventMediaPeriodId.windowSequenceNumber >= DefaultPlaybackSessionManager.this.getMinWindowSequenceNumber()) {
                this.windowSequenceNumber = eventMediaPeriodId.windowSequenceNumber;
            }
        }

        public boolean isFinishedAtEventTime(AnalyticsListener.EventTime eventTime) {
            if (eventTime.mediaPeriodId == null) {
                return this.windowIndex != eventTime.windowIndex;
            }
            if (this.windowSequenceNumber == -1) {
                return false;
            }
            if (eventTime.mediaPeriodId.windowSequenceNumber > this.windowSequenceNumber) {
                return true;
            }
            if (this.adMediaPeriodId == null) {
                return false;
            }
            int eventPeriodIndex = eventTime.timeline.getIndexOfPeriod(eventTime.mediaPeriodId.periodUid);
            int adPeriodIndex = eventTime.timeline.getIndexOfPeriod(this.adMediaPeriodId.periodUid);
            if (eventTime.mediaPeriodId.windowSequenceNumber < this.adMediaPeriodId.windowSequenceNumber || eventPeriodIndex < adPeriodIndex) {
                return false;
            }
            if (eventPeriodIndex > adPeriodIndex) {
                return true;
            }
            if (!eventTime.mediaPeriodId.isAd()) {
                return eventTime.mediaPeriodId.nextAdGroupIndex == -1 || eventTime.mediaPeriodId.nextAdGroupIndex > this.adMediaPeriodId.adGroupIndex;
            }
            int eventAdGroup = eventTime.mediaPeriodId.adGroupIndex;
            int eventAdIndex = eventTime.mediaPeriodId.adIndexInAdGroup;
            if (eventAdGroup <= this.adMediaPeriodId.adGroupIndex) {
                return eventAdGroup == this.adMediaPeriodId.adGroupIndex && eventAdIndex > this.adMediaPeriodId.adIndexInAdGroup;
            }
            return true;
        }

        private int resolveWindowIndexToNewTimeline(Timeline oldTimeline, Timeline newTimeline, int windowIndex) {
            if (windowIndex < oldTimeline.getWindowCount()) {
                oldTimeline.getWindow(windowIndex, DefaultPlaybackSessionManager.this.window);
                for (int periodIndex = DefaultPlaybackSessionManager.this.window.firstPeriodIndex; periodIndex <= DefaultPlaybackSessionManager.this.window.lastPeriodIndex; periodIndex++) {
                    Object periodUid = oldTimeline.getUidOfPeriod(periodIndex);
                    int newPeriodIndex = newTimeline.getIndexOfPeriod(periodUid);
                    if (newPeriodIndex != -1) {
                        return newTimeline.getPeriod(newPeriodIndex, DefaultPlaybackSessionManager.this.period).windowIndex;
                    }
                }
                return -1;
            }
            if (windowIndex < newTimeline.getWindowCount()) {
                return windowIndex;
            }
            return -1;
        }
    }
}
