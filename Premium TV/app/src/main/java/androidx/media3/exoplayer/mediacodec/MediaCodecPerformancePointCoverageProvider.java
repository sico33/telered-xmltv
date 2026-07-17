package androidx.media3.exoplayer.mediacodec;

import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Util;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class MediaCodecPerformancePointCoverageProvider {
    static final int COVERAGE_RESULT_NO = 1;
    static final int COVERAGE_RESULT_NO_PERFORMANCE_POINTS_UNSUPPORTED = 0;
    static final int COVERAGE_RESULT_YES = 2;
    private static Boolean shouldIgnorePerformancePoints;

    private MediaCodecPerformancePointCoverageProvider() {
    }

    public static int areResolutionAndFrameRateCovered(android.media.MediaCodecInfo.VideoCapabilities videoCapabilities, int width, int height, double frameRate) {
        if (Util.SDK_INT >= 29) {
            if (shouldIgnorePerformancePoints != null && shouldIgnorePerformancePoints.booleanValue()) {
                return 0;
            }
            return Api29.areResolutionAndFrameRateCovered(videoCapabilities, width, height, frameRate);
        }
        return 0;
    }

    private static final class Api29 {
        private Api29() {
        }

        public static int areResolutionAndFrameRateCovered(android.media.MediaCodecInfo.VideoCapabilities videoCapabilities, int width, int height, double frameRate) {
            List<android.media.MediaCodecInfo.VideoCapabilities.PerformancePoint> performancePointList = videoCapabilities.getSupportedPerformancePoints();
            if (performancePointList == null || performancePointList.isEmpty()) {
                return 0;
            }
            android.media.MediaCodecInfo.VideoCapabilities.PerformancePoint targetPerformancePoint = new android.media.MediaCodecInfo.VideoCapabilities.PerformancePoint(width, height, (int) frameRate);
            int performancePointCoverageResult = evaluatePerformancePointCoverage(performancePointList, targetPerformancePoint);
            if (performancePointCoverageResult == 1 && MediaCodecPerformancePointCoverageProvider.shouldIgnorePerformancePoints == null) {
                Boolean unused = MediaCodecPerformancePointCoverageProvider.shouldIgnorePerformancePoints = Boolean.valueOf(shouldIgnorePerformancePoints());
                if (MediaCodecPerformancePointCoverageProvider.shouldIgnorePerformancePoints.booleanValue()) {
                    return 0;
                }
            }
            return performancePointCoverageResult;
        }

        private static boolean shouldIgnorePerformancePoints() {
            List<android.media.MediaCodecInfo.VideoCapabilities.PerformancePoint> performancePointListH264;
            if (Util.SDK_INT >= 35) {
                return false;
            }
            try {
                Format formatH264 = new Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).build();
                if (formatH264.sampleMimeType != null) {
                    List<MediaCodecInfo> decoderInfos = MediaCodecUtil.getDecoderInfosSoftMatch(MediaCodecSelector.DEFAULT, formatH264, false, false);
                    for (int i = 0; i < decoderInfos.size(); i++) {
                        if (decoderInfos.get(i).capabilities != null && decoderInfos.get(i).capabilities.getVideoCapabilities() != null && (performancePointListH264 = decoderInfos.get(i).capabilities.getVideoCapabilities().getSupportedPerformancePoints()) != null && !performancePointListH264.isEmpty()) {
                            android.media.MediaCodecInfo.VideoCapabilities.PerformancePoint targetPerformancePointH264 = new android.media.MediaCodecInfo.VideoCapabilities.PerformancePoint(1280, 720, 60);
                            return evaluatePerformancePointCoverage(performancePointListH264, targetPerformancePointH264) == 1;
                        }
                    }
                }
                return true;
            } catch (MediaCodecUtil.DecoderQueryException e) {
                return true;
            }
        }

        private static int evaluatePerformancePointCoverage(List<android.media.MediaCodecInfo.VideoCapabilities.PerformancePoint> performancePointList, android.media.MediaCodecInfo.VideoCapabilities.PerformancePoint targetPerformancePoint) {
            for (int i = 0; i < performancePointList.size(); i++) {
                if (performancePointList.get(i).covers(targetPerformancePoint)) {
                    return 2;
                }
            }
            return 1;
        }
    }
}
