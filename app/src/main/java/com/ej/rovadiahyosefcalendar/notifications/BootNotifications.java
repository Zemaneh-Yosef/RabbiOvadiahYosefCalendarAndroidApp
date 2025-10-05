package com.ej.rovadiahyosefcalendar.notifications;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.ActivityCompat;

import com.ej.rovadiahyosefcalendar.activities.ZmanimAppWidget;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class BootNotifications extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
            LocationResolver mLocationResolver = new LocationResolver(context, null);
            if (mSharedPreferences.getBoolean("isSetup", false)) {
                if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
                    mLocationResolver.getRealtimeNotificationData(location -> {
                        if (location != null) {
                            setDailyNotifications(context, new ROZmanimCalendar(new GeoLocation(
                                    "",// not needed
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    0,// it barely makes a difference on when the notification sends, not worth the network call IMO
                                    mLocationResolver.getTimeZone())));
                        } else {
                            setDailyNotifications(context, new ROZmanimCalendar(mLocationResolver.getLastKnownGeoLocation()));
                        }
                    }
                    , false);
                } else {
                    setDailyNotifications(context, new ROZmanimCalendar(mLocationResolver.getRealtimeNotificationData(null, false)));
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
    }

    private void setDailyNotifications(Context context, ROZmanimCalendar zmanimCalendar) {
        Calendar calendar = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        Date sunrise = zmanimCalendar.getSunrise();
        if (sunrise == null) {
            sunrise = new Date();
        }
        calendar.setTimeInMillis(sunrise.getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context, DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationUtils.setExactAndAllowWhileIdle(am, calendar.getTimeInMillis(), dailyPendingIntent);

        Date sunset = zmanimCalendar.getSunset();
        if (sunset == null) {
            sunset = new Date();
        }
        calendar.setTimeInMillis(sunset.getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context, OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationUtils.setExactAndAllowWhileIdle(am, calendar.getTimeInMillis(), omerPendingIntent);
    }
}
