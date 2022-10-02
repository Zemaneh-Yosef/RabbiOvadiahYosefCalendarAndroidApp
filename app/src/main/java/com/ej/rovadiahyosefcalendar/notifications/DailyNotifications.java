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
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DailyNotifications extends BroadcastReceiver {

    private static int MID = 50;
    private LocationResolver mLocationResolver;
    private SharedPreferences mSharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        JewishDateInfo jewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael",false), true);
        mLocationResolver = new LocationResolver(context, new Activity());
        if (mSharedPreferences.getBoolean("isSetup",false)) {
            AstronomicalCalendar calendar = getROZmanimCalendar(context);

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

                if (!mSharedPreferences.getString("lastKnownDay","").equals(jewishDateInfo.getJewishDate())) {//We only want 1 notification a day.
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
                    mSharedPreferences.edit().putString("lastKnownDay", jewishDateInfo.getJewishDate()).apply();
                }
            }
            Calendar cal = Calendar.getInstance();
            checkIfTekufaIsToday(context, jewishDateInfo, cal);
            updateAlarm(context, calendar, cal);
            startUpDailyZmanim(context, mSharedPreferences);//we need to start the zmanim service every day because there might be a person who will just want to see candle lighting time every week or a similar case by pesach zmanim.
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

    private void startUpDailyZmanim(Context context, SharedPreferences sp) {
        Intent zmanIntent = new Intent(context.getApplicationContext(), ZmanimNotifications.class);
        sp.edit().putBoolean("fromThisNotification", false).apply();
        PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),0,zmanIntent,PendingIntent.FLAG_IMMUTABLE);
        try {
            zmanimPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private void checkIfTekufaIsToday(Context context, JewishDateInfo jewishDateInfo, Calendar cal) {
        cal.add(Calendar.DATE, 1);//start checking from tomorrow
        jewishDateInfo.setCalendar(cal);
        if (jewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(cal.getTime(), jewishDateInfo.getJewishCalendar().getTekufaAsDate())) {//if next day hebrew has tekufa today
            setupTekufaNotification(context, cal, jewishDateInfo);
        }

        cal.add(Calendar.DATE, -1);
        jewishDateInfo.setCalendar(cal);//reset
        if (jewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(cal.getTime(), jewishDateInfo.getJewishCalendar().getTekufaAsDate())) {//if today hebrew has tekufa today
            setupTekufaNotification(context, cal, jewishDateInfo);
        }
    }

    private void setupTekufaNotification(Context context, Calendar cal, JewishDateInfo jewishDateInfo) {
        Calendar tekufaCal = (Calendar) cal.clone();//clone to avoid changing the original calendar
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        tekufaCal.setTimeInMillis(DateUtils.addHours(jewishDateInfo.getJewishCalendar().getTekufaAsDate(), -1).getTime());
        PendingIntent tekufaPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), TekufaNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        am.cancel(tekufaPendingIntent);
        am.set(AlarmManager.RTC_WAKEUP, tekufaCal.getTimeInMillis(), tekufaPendingIntent);
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
