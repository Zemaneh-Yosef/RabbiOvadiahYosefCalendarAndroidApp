package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public class BootNotifications extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.requireNonNull(intent.getAction()).equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
            if (sp.getBoolean("isSetup",false)) {
                ComplexZmanimCalendar c = new ComplexZmanimCalendar(new GeoLocation(
                        sp.getString("name", ""),
                        Double.longBitsToDouble(sp.getLong("lat", 0)),
                        Double.longBitsToDouble(sp.getLong("long", 0)),
                        TimeZone.getTimeZone(sp.getString("timezoneID", ""))));

                Calendar calendar = Calendar.getInstance();
                AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

                calendar.setTimeInMillis(c.getSunrise().getTime());
                if (calendar.getTime().compareTo(new Date()) < 0) {
                    calendar.add(Calendar.DATE, 1);
                }
                PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                        0, new Intent(context, DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
                am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dailyPendingIntent);

                calendar.setTimeInMillis(c.getSunset().getTime());
                if (calendar.getTime().compareTo(new Date()) < 0) {
                    calendar.add(Calendar.DATE, 1);
                }
                PendingIntent omerPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                        0, new Intent(context, OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
                am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), omerPendingIntent);

                Intent zmanIntent = new Intent(context.getApplicationContext(), ZmanimNotifications.class);
                sp.edit().putBoolean("findNextZman", true).apply();
                PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),0,zmanIntent,PendingIntent.FLAG_IMMUTABLE);
                try {
                    zmanimPendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
