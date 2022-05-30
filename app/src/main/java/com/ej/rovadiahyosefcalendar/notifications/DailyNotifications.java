package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainActivity;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DailyNotifications extends BroadcastReceiver {

    private static int MID = 50;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        JewishDateInfo jewishDateInfo = new JewishDateInfo(sp.getBoolean("inIsrael",false), true);
        if (sp.getBoolean("isSetup",false)) {
            AstronomicalCalendar calendar = new AstronomicalCalendar(new GeoLocation(
                    sp.getString("name", ""),
                    Double.longBitsToDouble(sp.getLong("lat", 0)),
                    Double.longBitsToDouble(sp.getLong("long", 0)),
                    TimeZone.getTimeZone(sp.getString("timezoneID", ""))));

            if (!jewishDateInfo.getSpecialDay().isEmpty()) {
                long when = calendar.getSunrise().getTime();
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("Jewish Special Day",
                            "Daily Notifications",
                            NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription("This notification will check daily if there is a " +
                            "special jewish day and display it at sunrise.");
                    channel.enableLights(true);
                    channel.enableVibration(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        channel.setAllowBubbles(true);
                    }
                    channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    channel.setLightColor(Color.BLUE);
                    notificationManager.createNotificationChannel(channel);
                }

                Intent notificationIntent = new Intent(context, MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                if (!sp.getString("lastKnownDay","").equals(jewishDateInfo.getJewishDate())) {//We only want 1 notification a day.
                    NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context,
                            "Jewish Special Day").setSmallIcon(R.drawable.calendar_foreground)
                            .setContentTitle("Jewish Special Day")
                            .setContentText("Today is " + jewishDateInfo.getSpecialDay())
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .setBigContentTitle("Jewish Special Day")
                                    .setSummaryText(calendar.getGeoLocation().getLocationName())
                                    .bigText("Today is " + jewishDateInfo.getSpecialDay()))
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setCategory(NotificationCompat.CATEGORY_REMINDER)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setSound(alarmSound)
                            .setColor(context.getColor(R.color.dark_gold))
                            .setAutoCancel(true)
                            .setWhen(when)
                            .setContentIntent(pendingIntent);
                    notificationManager.notify(MID, mNotifyBuilder.build());
                    MID++;
                    sp.edit().putString("lastKnownDay", jewishDateInfo.getJewishDate()).apply();
                }
            }
            Calendar cal = Calendar.getInstance();
            checkIfTekufaIsToday(context, jewishDateInfo, cal);
            updateAlarm(context, calendar, cal);
        }
    }

    private void checkIfTekufaIsToday(Context context, JewishDateInfo jewishDateInfo, Calendar cal) {
        cal.add(Calendar.DATE, 1);
        jewishDateInfo.setCalendar(cal);
        cal.add(Calendar.DATE, -1);
        if (jewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(cal.getTime(), jewishDateInfo.getJewishCalendar().getTekufaAsDate())) {//if next day hebrew has tekufa today
            setupTekufaNotification(context, cal, jewishDateInfo);
        }

        jewishDateInfo.setCalendar(cal);//reset
        if (jewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(cal.getTime(), jewishDateInfo.getJewishCalendar().getTekufaAsDate())) {//if today hebrew has tekufa today
            setupTekufaNotification(context, cal, jewishDateInfo);
        }
    }

    private void setupTekufaNotification(Context context, Calendar cal, JewishDateInfo jewishDateInfo) {
        Calendar tekufaCal = (Calendar) cal.clone();
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        tekufaCal.setTimeInMillis(DateUtils.addHours(jewishDateInfo.getJewishCalendar().getTekufaAsDate(), -1).getTime());
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), TekufaNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        am.cancel(dailyPendingIntent);
        am.setExact(AlarmManager.RTC_WAKEUP, tekufaCal.getTimeInMillis(), dailyPendingIntent);
    }

    private void updateAlarm(Context context, AstronomicalCalendar c, Calendar cal) {
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        cal.setTimeInMillis(c.getSunrise().getTime());
        if (cal.getTime().compareTo(new Date()) < 0) {
            cal.add(Calendar.DATE, 1);
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        am.cancel(dailyPendingIntent);
        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), dailyPendingIntent);
    }
}
