package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.Utils;

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
        JewishDateInfo jewishDateInfo = new JewishDateInfo(sp.getBoolean("inIsrael", false));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel("Tekufa Notifications",
                "Tekufa Notifications",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("This notification will check daily if the seasons change and will show a notification to the user at an hour and a half before.");
        channel.enableLights(true);
        channel.enableVibration(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(true);
        }
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setLightColor(Color.BLUE);
        notificationManager.createNotificationChannel(channel);

        Intent notificationIntent = new Intent(context, MainFragmentManagerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Date tekufaTime = findTekufaTime(jewishDateInfo);
        DateFormat zmanimFormat;
        if (Utils.isLocaleHebrew()) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());//no need for seconds as the tekufa never has seconds
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//no need for seconds as the tekufa never has seconds
        }
        zmanimFormat.setTimeZone(TimeZone.getDefault());

        if (tekufaTime == null) {return;}// it should never be null, but just in case
        Date halfHourBefore = new Date(tekufaTime.getTime() - DateUtils.MILLIS_PER_HOUR / 2);
        Date halfHourAfter = new Date(tekufaTime.getTime() + DateUtils.MILLIS_PER_HOUR / 2);

        NotificationCompat.Builder mNotifyBuilder;
        String contentText;
        String contentTitle;
        if (Utils.isLocaleHebrew()) {
            contentText = "התקופות משתנות היום ב " + zmanimFormat.format(tekufaTime) + ". " +
                    "נא לא לשתות מים מ- " +
                    zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter);
            contentTitle = "התקופות משתנות";
        } else {
            contentText = "The tekufas (seasons) change today at " + zmanimFormat.format(tekufaTime) + ". Preferably, do not drink water from " +
                    zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter);
            contentTitle = "Tekufa/Season Change";
        }
        mNotifyBuilder = new NotificationCompat.Builder(context, "Tekufa Notifications")
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(getSeasonalIcon(jewishDateInfo.getJewishCalendar().getTekufaName()))
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(contentTitle)
                        .setSummaryText(sp.getString("name", ""))
                        .bigText(contentText))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setColor(context.getColor(R.color.dark_gold))
                .setAutoCancel(true)
                .setWhen(tekufaTime.getTime())
                .setContentIntent(pendingIntent);
        notificationManager.notify(MID, mNotifyBuilder.build());
        MID++;
    }

    private int getSeasonalIcon(String tekufaName) {
        return switch (tekufaName) {
            case "Tevet" -> R.drawable.winter;
            case "Nissan" -> R.drawable.spring;
            case "Tammuz" -> R.drawable.summer;
            default -> R.drawable.autumn;
        };
    }

    private Date findTekufaTime(JewishDateInfo jewishDateInfo) {
        // this code should be called on the day that the tekufa falls out, however, since the tekufa could be on a different gregorian date, go back 2 days and keep going forward until you find the tekufa.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        jewishDateInfo.setCalendar(cal);
        Date result = jewishDateInfo.getJewishCalendar().getTekufaAsDate();
        while (result == null) {
            cal.add(Calendar.DATE, 1);
            jewishDateInfo.setCalendar(cal);
            result = jewishDateInfo.getJewishCalendar().getTekufaAsDate();
        }// Do not reset the date
        return result;
    }
}
