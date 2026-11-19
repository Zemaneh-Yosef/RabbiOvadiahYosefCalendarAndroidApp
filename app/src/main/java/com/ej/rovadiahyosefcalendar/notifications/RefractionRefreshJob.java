package com.ej.rovadiahyosefcalendar.notifications;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.util.Log;

import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionRepository;
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionSupport;

/**
 * Downloads the weather, terrain and haze data for a place in the background, for the notifications and the widget, which otherwise
 * only get new data after a screen of the app was opened.
 * <p>
 * The receivers (notifications, widget) only schedule this job and return right away: a download can take a while, and a broadcast
 * receiver that waits for it risks an ANR. A job may run for minutes, and only runs with a network connection.
 * When new data arrive, RefractionRepository tells its listeners, and the app reschedules the notifications and redraws the widget
 * (see ZemanehYosefApplication).
 */
public class RefractionRefreshJob extends JobService {

    private static final String TAG = "RefractionRefreshJob";
    private static final int JOB_ID = 4801;
    private static final String EXTRA_LAT = "lat";
    private static final String EXTRA_LON = "lon";

    /**
     * Schedules a download of the place's data, unless one is already waiting or running. Does nothing when the data are fresh: the job
     * checks again when it runs, and downloads at most every 10 minutes per place (see {@link RefractionRepository#refreshIfStale}).
     */
    public static void schedule(Context context, double latitude, double longitude) {
        if (!RefractionSupport.getEnabled() || (latitude == 0 && longitude == 0) || RefractionRepository.forecastStaleIn(latitude, longitude) > 0) {
            return;// the data are turned off, there is no location yet, or the data are fresh
        }
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null || scheduler.getPendingJob(JOB_ID) != null) {
            return;// scheduling it again would cancel the one that is waiting or running (e.g. downloading right now)
        }
        PersistableBundle extras = new PersistableBundle();
        extras.putDouble(EXTRA_LAT, latitude);
        extras.putDouble(EXTRA_LON, longitude);
        try {
            scheduler.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(context, RefractionRefreshJob.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(extras)
                    .build());
        } catch (Exception e) {// e.g. too many jobs scheduled
            Log.w(TAG, "Couldn't schedule the refresh", e);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        double latitude = params.getExtras().getDouble(EXTRA_LAT);
        double longitude = params.getExtras().getDouble(EXTRA_LON);
        // runs on RefractionRepository's loading thread; true = still working, jobFinished is called when it's done
        return RefractionRepository.refreshIfStale(latitude, longitude, () -> jobFinished(params, false));
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;// stopped by the system (e.g. the network was lost): try again later
    }
}
