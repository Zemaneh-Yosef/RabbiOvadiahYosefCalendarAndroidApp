package com.elyjacobi.ROvadiahYosefCalendar.notifications;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.elyjacobi.ROvadiahYosefCalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.elyjacobi.ROvadiahYosefCalendar.R;
import com.elyjacobi.ROvadiahYosefCalendar.activities.MainActivity;
import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class OmerNotifications extends BroadcastReceiver {

    private static int MID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        JewishCalendar jewishCalendar = new JewishCalendar();
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        if (sp.getBoolean("isSetup",false)) {
            ComplexZmanimCalendar c = new ComplexZmanimCalendar(new GeoLocation(
                    sp.getString("name", ""),
                    Double.longBitsToDouble(sp.getLong("lat", 0)),
                    Double.longBitsToDouble(sp.getLong("long", 0)),
                    TimeZone.getTimeZone(sp.getString("timezoneID", ""))));

            int day = jewishCalendar.getDayOfOmer();
            if (day != -1 && day != 49) {//we don't want to send a notification right before shavuot
                long when = c.getSunset().getTime();
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("Omer",
                            "Omer Notifications",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription("This notification will check daily if it is the omer and " +
                            "it will display which day it is at sunset.");
                    channel.enableLights(true);
                    channel.enableVibration(true);
                    notificationManager.createNotificationChannel(channel);
                }

                Intent notificationIntent = new Intent(context, MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                if (sp.getString("lastKnownDayOmer", "").equals(
                        jewishCalendar.toString())) {//We only want 1 notification a day.
                    NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context,
                            "Omer").setSmallIcon(R.drawable.calendar_foreground)
                            .setContentTitle("Day of Omer")
                            .setContentText("Tonight is the " +
                                    (getOrdinal(jewishCalendar.getDayOfOmer() + 1)) +
                                    " day of the omer.")
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setSound(alarmSound)
                            .setAutoCancel(true)
                            .setWhen(when)
                            .setContentIntent(pendingIntent);
                    notificationManager.notify(MID, mNotifyBuilder.build());
                    MID++;
                }
                sp.edit().putString("lastKnownDayOmer", jewishCalendar.toString()).apply();
            }
            updateAlarm(context, c);
        }
    }

    private void updateAlarm(Context context, ComplexZmanimCalendar c) {
        Calendar calendar = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        calendar.setTimeInMillis(c.getSunset().getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        am.cancel(omerPendingIntent);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), omerPendingIntent);
    }

    private String getOrdinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }
}
