package androidx.media3.exoplayer.scheduler;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class PlatformScheduler implements Scheduler {
    private static final String KEY_REQUIREMENTS = "requirements";
    private static final String KEY_SERVICE_ACTION = "service_action";
    private static final String KEY_SERVICE_PACKAGE = "service_package";
    private static final int SUPPORTED_REQUIREMENTS;
    private static final String TAG = "PlatformScheduler";
    private final int jobId;
    private final JobScheduler jobScheduler;
    private final ComponentName jobServiceComponentName;

    static {
        SUPPORTED_REQUIREMENTS = (Util.SDK_INT >= 26 ? 16 : 0) | 15;
    }

    public PlatformScheduler(Context context, int jobId) {
        Context context2 = context.getApplicationContext();
        this.jobId = jobId;
        this.jobServiceComponentName = new ComponentName(context2, (Class<?>) PlatformSchedulerService.class);
        this.jobScheduler = (JobScheduler) Assertions.checkNotNull((JobScheduler) context2.getSystemService("jobscheduler"));
    }

    @Override // androidx.media3.exoplayer.scheduler.Scheduler
    public boolean schedule(Requirements requirements, String servicePackage, String serviceAction) {
        JobInfo jobInfo = buildJobInfo(this.jobId, this.jobServiceComponentName, requirements, serviceAction, servicePackage);
        int result = this.jobScheduler.schedule(jobInfo);
        return result == 1;
    }

    @Override // androidx.media3.exoplayer.scheduler.Scheduler
    public boolean cancel() {
        this.jobScheduler.cancel(this.jobId);
        return true;
    }

    @Override // androidx.media3.exoplayer.scheduler.Scheduler
    public Requirements getSupportedRequirements(Requirements requirements) {
        return requirements.filterRequirements(SUPPORTED_REQUIREMENTS);
    }

    private static JobInfo buildJobInfo(int jobId, ComponentName jobServiceComponentName, Requirements requirements, String serviceAction, String servicePackage) {
        Requirements filteredRequirements = requirements.filterRequirements(SUPPORTED_REQUIREMENTS);
        if (!filteredRequirements.equals(requirements)) {
            Log.w(TAG, "Ignoring unsupported requirements: " + (filteredRequirements.getRequirements() ^ requirements.getRequirements()));
        }
        JobInfo.Builder builder = new JobInfo.Builder(jobId, jobServiceComponentName);
        if (requirements.isUnmeteredNetworkRequired()) {
            builder.setRequiredNetworkType(2);
        } else if (requirements.isNetworkRequired()) {
            builder.setRequiredNetworkType(1);
        }
        builder.setRequiresDeviceIdle(requirements.isIdleRequired());
        builder.setRequiresCharging(requirements.isChargingRequired());
        if (Util.SDK_INT >= 26 && requirements.isStorageNotLowRequired()) {
            builder.setRequiresStorageNotLow(true);
        }
        builder.setPersisted(true);
        PersistableBundle extras = new PersistableBundle();
        extras.putString(KEY_SERVICE_ACTION, serviceAction);
        extras.putString(KEY_SERVICE_PACKAGE, servicePackage);
        extras.putInt("requirements", requirements.getRequirements());
        builder.setExtras(extras);
        return builder.build();
    }

    public static final class PlatformSchedulerService extends JobService {
        @Override // android.app.job.JobService
        public boolean onStartJob(JobParameters params) {
            PersistableBundle extras = params.getExtras();
            Requirements requirements = new Requirements(extras.getInt("requirements"));
            int notMetRequirements = requirements.getNotMetRequirements(this);
            if (notMetRequirements == 0) {
                String serviceAction = (String) Assertions.checkNotNull(extras.getString(PlatformScheduler.KEY_SERVICE_ACTION));
                String servicePackage = (String) Assertions.checkNotNull(extras.getString(PlatformScheduler.KEY_SERVICE_PACKAGE));
                Intent intent = new Intent(serviceAction).setPackage(servicePackage);
                Util.startForegroundService(this, intent);
                return false;
            }
            Log.w(PlatformScheduler.TAG, "Requirements not met: " + notMetRequirements);
            jobFinished(params, true);
            return false;
        }

        @Override // android.app.job.JobService
        public boolean onStopJob(JobParameters params) {
            return false;
        }
    }
}
