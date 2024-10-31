package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

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
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManager;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocaleChecker;

import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CombinedTekufaNotifications extends BroadcastReceiver {

    private static int MID = 100;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        JewishDateInfo jewishDateInfo = new JewishDateInfo(sp.getBoolean("inIsrael",false));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        }

        Intent notificationIntent = new Intent(context, MainFragmentManager.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Date earlierTekufaTime = findEarlierTekufaTime(jewishDateInfo);
        DateFormat zmanimFormat;
        if (LocaleChecker.isLocaleHebrew()) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());//no need for seconds as the tekufa never has seconds
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//no need for seconds as the tekufa never has seconds
        }
        zmanimFormat.setTimeZone(TimeZone.getDefault());

        if (earlierTekufaTime != null) {//it should never be null, but just in case
            Date halfHourBefore = new Date(earlierTekufaTime.getTime() - DateUtils.MILLIS_PER_HOUR/2);
            Date tekufaTime = new Date(earlierTekufaTime.getTime() + (DateUtils.MILLIS_PER_MINUTE * 21));
            Date halfHourAfterPlus21Minutes = new Date(earlierTekufaTime.getTime() + DateUtils.MILLIS_PER_HOUR/2 + (DateUtils.MILLIS_PER_MINUTE * 21));

            NotificationCompat.Builder mNotifyBuilder;

            if (LocaleChecker.isLocaleHebrew()) {
                String contentText = "התקופות משתנות היום ב "
                        + zmanimFormat.format(earlierTekufaTime) + "/" + zmanimFormat.format(tekufaTime) + ". " +
                        "נא לא לשתות מים מ- " +
                        zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfterPlus21Minutes);

                mNotifyBuilder = new NotificationCompat.Builder(context,
                        "Tekufa Notifications")
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setSmallIcon(getSeasonalIcon(jewishDateInfo.getJewishCalendar().getTekufaName()))
                        .setContentTitle("התקופות משתנות")
                        .setContentText(contentText)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .setBigContentTitle("התקופות משתנות")
                                .setSummaryText(sp.getString("name", ""))
                                .bigText(contentText))
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setSound(alarmSound)
                        .setColor(context.getColor(R.color.dark_gold))
                        .setAutoCancel(true)
                        .setWhen(earlierTekufaTime.getTime())
                        .setContentIntent(pendingIntent);
                notificationManager.notify(MID, mNotifyBuilder.build());
            } else {
                String contentText = "The tekufas (seasons) change today at "
                        + zmanimFormat.format(earlierTekufaTime) + "/" + zmanimFormat.format(tekufaTime) +
                        ". Preferably, do not drink water from " +
                        zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfterPlus21Minutes);

                mNotifyBuilder = new NotificationCompat.Builder(context,
                        "Tekufa Notifications")
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setSmallIcon(getSeasonalIcon(jewishDateInfo.getJewishCalendar().getTekufaName()))
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
                        .setWhen(earlierTekufaTime.getTime())
                        .setContentIntent(pendingIntent);
                notificationManager.notify(MID, mNotifyBuilder.build());
            }

            MID++;
        }
    }

    private int getSeasonalIcon(String tekufaName) {
        switch (tekufaName) {
            case "Tishri":
                return R.drawable.autumn;
            case "Tevet":
                return R.drawable.winter;
            case "Nissan":
                return R.drawable.spring;
            case "Tammuz":
                return R.drawable.summer;
        }
        return R.drawable.autumn;//we should never get here but to keep the compiler happy
    }

    private Date findEarlierTekufaTime(JewishDateInfo jewishDateInfo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        jewishDateInfo.setCalendar(cal);
        if (jewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(new Date(), jewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())) {//if next day hebrew has tekufa today
            return jewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate();
        }

        cal.add(Calendar.DATE, -1);
        jewishDateInfo.setCalendar(cal);//reset
        if (jewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(new Date(), jewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())) {//if today hebrew has tekufa today
            return jewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate();
        }
        return null;//it should not return null because this notification will be called when the tekufa is today or tomorrow
    }
}
