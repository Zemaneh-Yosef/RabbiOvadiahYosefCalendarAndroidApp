package com.ej.rovadiahyosefcalendar.notifications;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.activities.ZmanimAppWidget;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BootNotifications extends BroadcastReceiver {

    private PendingResult mPendingResult;
    private final AtomicBoolean mFinished = new AtomicBoolean();
    private final AtomicBoolean mGotLocation = new AtomicBoolean();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            mPendingResult = goAsync();
            SharedPreferences mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
            LocationResolver mLocationResolver = new LocationResolver(context, null);
            if (!mSharedPreferences.getBoolean("isSetup", false)) {
                finishBroadcast();
                return;
            }

            if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
                GeoLocation geoLocation = mLocationResolver.getRealtimeNotificationData(location -> {
                    if (!mGotLocation.compareAndSet(false, true)) {
                        return;
                    }
                    if (location != null) {
                        setDailyNotifications(context, new ROZmanimCalendar(new GeoLocation(
                                "",// not needed
                                location.getLatitude(),
                                location.getLongitude(),
                                0,// it barely makes a difference on when the notification sends, not worth the network call IMO
                                mLocationResolver.getZoneID())));
                    } else {
                        setDailyNotifications(context, new ROZmanimCalendar(mLocationResolver.getLastKnownGeoLocation()));
                    }
                    finishBroadcast();
                }
                , false);
                if (!geoLocation.equals(new GeoLocation())) {
                    setDailyNotifications(context, new ROZmanimCalendar(geoLocation));
                    finishBroadcast();
                }
            } else {
                setDailyNotifications(context, new ROZmanimCalendar(mLocationResolver.getRealtimeNotificationData(null, false)));
                finishBroadcast();
            }

            PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, ZmanimNotifications.class), PendingIntent.FLAG_IMMUTABLE);
            try {
                zmanimPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }

            Intent widgetIntent = new Intent(context, ZmanimAppWidget.class);
            widgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, ZmanimAppWidget.class));
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(widgetIntent);
        }
    }

    private void finishBroadcast() {
        if (mFinished.compareAndSet(false, true)) {
            mPendingResult.finish(); // REQUIRED TO AVOID ANR
        }
    }

    private void setDailyNotifications(Context context, ROZmanimCalendar zmanimCalendar) {
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        zmanimCalendar.setAmudehHoraah(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("LuachAmudeiHoraah", false));
        ROZmanimCalendar tomorrow = zmanimCalendar.getCopy();
        tomorrow.setLocalDate(tomorrow.getLocalDate().plusDays(1));
        Instant sunrise = zmanimCalendar.getSunriseBasedOnElevationSetting();
        if (sunrise == null) {
            sunrise = Instant.now();
        }
        if (sunrise.compareTo(Instant.now()) < 0) {
            if (tomorrow.getSunriseBasedOnElevationSetting() != null) {
                sunrise = tomorrow.getSunriseBasedOnElevationSetting();
            } else {
                sunrise = sunrise.plus(1, ChronoUnit.DAYS);
            }
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context, DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationUtils.setExactAndAllowWhileIdle(am, sunrise.toEpochMilli(), dailyPendingIntent);

        Instant tzeit = zmanimCalendar.getTzeit();
        if (tzeit == null) {
            tzeit = Instant.now();
        }
        if (tzeit.compareTo(Instant.now()) < 0) {
            if (tomorrow.getTzeit() != null) {
                tzeit = tomorrow.getTzeit();
            } else {
                tzeit = tzeit.plus(1, ChronoUnit.DAYS);
            }
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context, OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationUtils.setExactAndAllowWhileIdle(am, tzeit.toEpochMilli(), omerPendingIntent);
        DailyNotifications.setTekufaNotification(context, new JewishDateInfo(context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("inIsrael", false)));
    }
}
