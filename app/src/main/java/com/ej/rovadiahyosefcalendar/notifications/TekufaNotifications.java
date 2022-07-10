package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

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

import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TekufaNotifications extends BroadcastReceiver {

    private static int MID = 100;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        JewishDateInfo jewishDateInfo = new JewishDateInfo(sp.getBoolean("inIsrael",false), true);

        long when = jewishDateInfo.getJewishCalendar().getTekufaAsDate().getTime();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Tekufa Notifications",
                    "Tekufa Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("This notification will check daily if the seasons change and will show a notification to the user at an hour" +
                    " and a half before.");
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

        Date tekufaTime = findTekufaTime(jewishDateInfo);
        DateFormat zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//no need for seconds as the tekufa never has seconds
        zmanimFormat.setTimeZone(TimeZone.getDefault());

        if (tekufaTime != null) {//it should never be null, but just in case
            Date halfHourBefore = new Date(tekufaTime.getTime() - DateUtils.MILLIS_PER_HOUR/2);
            Date halfHourAfter = new Date(tekufaTime.getTime() + DateUtils.MILLIS_PER_HOUR/2);

            String contentText = "The tekufas(seasons) change today at " + zmanimFormat.format(tekufaTime) + ". Preferably, do not drink water from " +
                    zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter);

            NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context,
                    "Tekufa Notifications").setSmallIcon(R.drawable.calendar_foreground)
                    .setContentTitle("Tekufa/Season Change")
                    .setContentText(contentText)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle("Tekufa/Season Change")
                            .setSummaryText(sp.getString("name", ""))
                            .bigText(contentText))
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
        }
    }

    private Date findTekufaTime(JewishDateInfo jewishDateInfo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        jewishDateInfo.setCalendar(cal);
        cal.add(Calendar.DATE, -1);
        if (jewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(cal.getTime(), jewishDateInfo.getJewishCalendar().getTekufaAsDate())) {//if next day hebrew has tekufa today
            return jewishDateInfo.getJewishCalendar().getTekufaAsDate();
        }

        jewishDateInfo.setCalendar(cal);//reset
        if (jewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(cal.getTime(), jewishDateInfo.getJewishCalendar().getTekufaAsDate())) {//if today hebrew has tekufa today
            return jewishDateInfo.getJewishCalendar().getTekufaAsDate();
        }
        return null;//it should not return null because this notification will be called when the tekufa is today
    }
}
