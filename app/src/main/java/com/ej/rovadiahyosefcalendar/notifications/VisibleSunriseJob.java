package com.ej.rovadiahyosefcalendar.notifications;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;

import com.ej.rovadiahyosefcalendar.activities.ZmanimAppWidget;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Computes the visible sunrise of yesterday, today and tomorrow for the notifications, then sets them again with it.
 * <p>
 * The notifications are set from a broadcast receiver, often with no screen of the app open. When a day's visible sunrise isn't
 * computed yet (it takes a few seconds), the receiver can't wait for it (that risks an ANR), so HaNetz is first set at the sea-level
 * time, and the background computation could be cut short when the process ends. This job does the computation instead: a job may
 * run for minutes. The results are stored (see RefractionSupport), so the notifications find them when they are set again.
 */
public class VisibleSunriseJob extends JobService {

    private static final String TAG = "VisibleSunriseJob";
    private static final int JOB_ID = 4802;

    /** Schedules the computation for a place, unless it is already waiting or running. */
    public static void schedule(Context context, GeoLocation geo) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null || scheduler.getPendingJob(JOB_ID) != null) {
            return;// scheduling it again would cancel the one that is waiting or running
        }
        PersistableBundle extras = new PersistableBundle();
        extras.putString("name", geo.getLocationName());
        extras.putDouble("lat", geo.getLatitude());
        extras.putDouble("lon", geo.getLongitude());
        extras.putDouble("elev", geo.getElevation());
        extras.putString("zone", geo.getZoneId().getId());
        try {
            scheduler.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(context, VisibleSunriseJob.class))
                    .setOverrideDeadline(10_000)// no conditions: it only reads the stored data
                    .setExtras(extras)
                    .build());
        } catch (Exception e) {// e.g. too many jobs scheduled
            Log.w(TAG, "Couldn't schedule the visible sunrise computation", e);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle extras = params.getExtras();
        GeoLocation geo = new GeoLocation(extras.getString("name"), extras.getDouble("lat"), extras.getDouble("lon"),
                extras.getDouble("elev"), ZoneId.of(extras.getString("zone")));
        new Thread(() -> {
            boolean computed = false;
            try {
                ROZmanimCalendar calendar = new ROZmanimCalendar(geo);
                LocalDate today = LocalDate.now(geo.getZoneId());
                for (int i = -1; i <= 1; i++) {
                    calendar.setLocalDate(today.plusDays(i));
                    if (!calendar.isHaNetzKnown()) {
                        calendar.getHaNetz(true);// computed here and stored
                        computed = true;
                    }
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Computing the visible sunrise failed", e);
            } finally {
                if (computed) {// set the notifications again with them, and redraw the widget
                    sendBroadcast(new Intent(this, ZmanimNotifications.class));
                    ZmanimAppWidget.updateAllWidgets(this);
                }
                jobFinished(params, false);
            }
        }, "visible-sunrise-job").start();
        return true;// still working: jobFinished is called when done
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;// stopped by the system: try again later
    }
}
