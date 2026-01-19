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
import com.ej.rovadiahyosefcalendar.classes.SecondTreatment;
import com.ej.rovadiahyosefcalendar.classes.Utils;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;

public class AmudeiHoraahTekufaNotifications extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        JewishDateInfo jewishDateInfo = new JewishDateInfo(sp.getBoolean("inIsrael", false));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel("Amudei Horaah Tekufa Notifications",
                "Amudei Horaah Tekufa Notifications",
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
        if (tekufaTime == null) {return;} //it should never be null, but just in case

        Date halfHourBefore = new Date(tekufaTime.getTime() - DateUtils.MILLIS_PER_HOUR / 2);
        Date halfHourAfter = new Date(tekufaTime.getTime() + DateUtils.MILLIS_PER_HOUR / 2);

        NotificationCompat.Builder mNotifyBuilder;
        String contentText;
        String contentTitle;

        if (Utils.isLocaleHebrew(context)) {
            contentText = "התקופות משתנות היום ב " + Utils.formatZmanTime(context, tekufaTime, SecondTreatment.ROUND_EARLIER) + ". " +
                    "נא לא לשתות מים מ- " +
                    Utils.formatZmanTime(context, halfHourBefore, SecondTreatment.ROUND_EARLIER) + " - " + Utils.formatZmanTime(context, halfHourAfter, SecondTreatment.ROUND_EARLIER);
            contentTitle = "התקופות משתנות";
        } else {
            contentText = "The tekufas (seasons) change today at " + Utils.formatZmanTime(context, tekufaTime, SecondTreatment.ROUND_EARLIER) + ". Preferably, do not drink water from " +
                    Utils.formatZmanTime(context, halfHourBefore, SecondTreatment.ROUND_EARLIER) + " - " + Utils.formatZmanTime(context, halfHourAfter, SecondTreatment.ROUND_EARLIER);
            contentTitle = "Tekufa/Season Change";
        }
        mNotifyBuilder = new NotificationCompat.Builder(context,
                "Amudei Horaah Tekufa Notifications")
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(getSeasonalIcon(jewishDateInfo.getJewishCalendar().getTekufaName(Utils.isLocaleHebrew(context))))
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
        notificationManager.notify(51, mNotifyBuilder.build());
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
        Date result = jewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate();
        while (result == null) {
            cal.add(Calendar.DATE, 1);
            jewishDateInfo.setCalendar(cal);
            result = jewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate();
        }// Do not reset the date
        return result;
    }
}
