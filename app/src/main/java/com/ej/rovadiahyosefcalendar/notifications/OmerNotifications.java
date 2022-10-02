package com.ej.rovadiahyosefcalendar.notifications;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.Activity;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainActivity;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class OmerNotifications extends BroadcastReceiver {

    private static int MID = 1;
    private LocationResolver mLocationResolver;
    private SharedPreferences mSharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        JewishCalendar jewishCalendar = new JewishCalendar();
        mLocationResolver = new LocationResolver(context, new Activity());
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        if (mSharedPreferences.getBoolean("isSetup",false)) {
            ROZmanimCalendar c = getROZmanimCalendar(context);

            int day = jewishCalendar.getDayOfOmer();
            if (day != -1 && day != 49) {//we don't want to send a notification right before shavuot
                long when = c.getTzeit().getTime();
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("Omer",
                            "Omer Notifications",
                            NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription("This notification will check daily if it is the omer and " +
                            "it will display which day it is at sunset.");
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

                Calendar gc = jewishCalendar.getGregorianCalendar();
                gc.add(Calendar.DATE, 1);
                jewishCalendar.setDate(gc);
                String nextJewishDay = jewishCalendar.toString();
                gc.add(Calendar.DATE, -1);
                jewishCalendar.setDate(gc);

                if (!mSharedPreferences.getString("lastKnownDayOmer", "").equals(jewishCalendar.toString())) {//We only want 1 notification a day.
                    NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "Omer")
                            .setSmallIcon(R.drawable.calendar_foreground)
                            .setContentTitle("Day of Omer")
                            .setContentText("Tonight is the " +
                                    (getOrdinal(jewishCalendar.getDayOfOmer() + 1)) +
                                    " day of the omer.")
                            .setStyle(new NotificationCompat
                                    .BigTextStyle()
                                    .setBigContentTitle(nextJewishDay)
                                    .setSummaryText("Don't forget to count!")
                                    .bigText("Tonight is the " +
                                            (getOrdinal(jewishCalendar.getDayOfOmer() + 1)) +
                                            " day of the omer."))
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
                    mSharedPreferences.edit().putString("lastKnownDayOmer", jewishCalendar.toString()).apply();
                }
            }
            updateAlarm(context, c);
        }
    }

    @NonNull
    private ROZmanimCalendar getROZmanimCalendar(Context context) {
        if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            mLocationResolver.getRealtimeNotificationData();
            if (mLocationResolver.getLatitude() == 0 && mLocationResolver.getLongitude() == 0) {
                return new ROZmanimCalendar(new GeoLocation(
                        mSharedPreferences.getString("name", ""),
                        Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)),
                        Double.longBitsToDouble(mSharedPreferences.getLong("long", 0)),
                        getLastKnownElevation(),
                        TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", ""))));
            } else {
                return new ROZmanimCalendar(new GeoLocation(
                        mLocationResolver.getLocationName(),
                        mLocationResolver.getLatitude(),
                        mLocationResolver.getLongitude(),
                        getLastKnownElevation(),
                        mLocationResolver.getTimeZone()));
            }
        }
        return new ROZmanimCalendar(new GeoLocation(
                mSharedPreferences.getString("name", ""),
                Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)),
                Double.longBitsToDouble(mSharedPreferences.getLong("long", 0)),
                getLastKnownElevation(),
                TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", ""))));
    }

    private double getLastKnownElevation() {
        double elevation = 0;
        try {//TODO this needs to be removed but cannot be removed for now because it is needed for people who have setup the app before we changed data types
            //get the last value of the current location or 0 if it doesn't exist
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        } catch (Exception e) {
            try {//legacy
                elevation = mSharedPreferences.getFloat("elevation", 0);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return elevation;
    }

    private void updateAlarm(Context context, ROZmanimCalendar c) {
        Calendar calendar = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        calendar.setTimeInMillis(c.getTzeit().getTime());
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
