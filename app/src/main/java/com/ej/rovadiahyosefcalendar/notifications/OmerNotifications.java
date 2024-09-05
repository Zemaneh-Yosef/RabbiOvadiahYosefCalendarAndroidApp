package com.ej.rovadiahyosefcalendar.notifications;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.OmerActivity.omerList;

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
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManager;
import com.ej.rovadiahyosefcalendar.activities.OmerActivity;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

public class OmerNotifications extends BroadcastReceiver implements Consumer<Location> {

    private static int MID = 1;
    private LocationResolver mLocationResolver;
    private SharedPreferences mSharedPreferences;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        JewishDateInfo jewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false));
        mLocationResolver = new LocationResolver(context, new Activity());

        if (mSharedPreferences.getBoolean("isSetup", false)) {
            ROZmanimCalendar c = getROZmanimCalendar(context);

            if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) != PERMISSION_GRANTED) {
                init(context, jewishDateInfo, c);
            }
        }
    }

    private void init(Context context, JewishDateInfo jewishDateInfo, ROZmanimCalendar c) {
        int day = jewishDateInfo.getJewishCalendar().getDayOfOmer();
        if (day != -1 && day != 49) {//we don't want to send a notification right before shavuot
            long when;
            if (mSharedPreferences.getBoolean("LuachAmudeiHoraah", false)) {
                when = c.getTzeitAmudeiHoraah().getTime();
            } else {
                when = c.getTzeit().getTime();
            }
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

            Intent notificationIntent = new Intent(context, OmerActivity.class).putExtra("omerDay", day);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            Calendar gc = jewishDateInfo.getJewishCalendar().getGregorianCalendar();
            gc.add(Calendar.DATE, 1);
            jewishDateInfo.getJewishCalendar().setDate(gc);
            String nextJewishDay = jewishDateInfo.getJewishCalendar().toString();
            // Do not reset to the previous day, because Barech Aleinu checks for tomorrow

            if (!mSharedPreferences.getString("lastKnownDayOmer", "").equals(jewishDateInfo.getJewishCalendar().toString())) {//We only want 1 notification a day.
                NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "Omer")
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setSmallIcon(R.drawable.omer_wheat)
                        .setContentTitle(Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew") ? "יום בעומר" : "Day of Omer")
                        .setContentText(omerList.get(day))
                        .setStyle(new NotificationCompat
                                .BigTextStyle()
                                .setBigContentTitle(nextJewishDay)
                                .setSummaryText(Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew") ? "אל תשכח לספור!" : "Don't forget to count!")
                                .bigText("בָּרוּךְ אַתָּה יְהֹוָה, אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם, אֲשֶׁר קִדְּשָׁנוּ בְּמִצְוֹתָיו וְצִוָּנוּ עַל סְפִירַת הָעֹמֶר:" + "\n\n" + omerList.get(day) + "\n\nהָרַחֲמָן הוּא יַחֲזִיר עֲבוֹדַת בֵּית הַמִּקְדָּשׁ לִמְקוֹמָהּ בִּמְהֵרָה בְיָמֵינוּ אָמֵן:"))
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setSound(alarmSound)
                        .setColor(context.getColor(R.color.dark_gold))
                        .setAutoCancel(true)
                        .setWhen(when)
                        .setContentIntent(pendingIntent)
                        .addAction(new NotificationCompat.Action(0, context.getString(R.string.see_full_text), pendingIntent));
                notificationManager.notify(MID, mNotifyBuilder.build());
                MID++;
                mSharedPreferences.edit().putString("lastKnownDayOmer", jewishDateInfo.getJewishCalendar().toString()).apply();
            }
        }
        Calendar gc = jewishDateInfo.getJewishCalendar().getGregorianCalendar();
        gc.add(Calendar.DATE, 1);
        jewishDateInfo.getJewishCalendar().setDate(gc);
        if (new TefilaRules().isVeseinTalUmatarStartDate(jewishDateInfo.getJewishCalendar())) {// we need to know if user is in Israel or not
            notifyBarechAleinu(context);
        }
        updateAlarm(context, c);
    }

    private void notifyBarechAleinu(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("BarechAleinu",
                    "Barech Aleinu Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("This notification will check daily if tonight is the start date for Barech Aleinu and it will notify you at sunset.");
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

        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "BarechAleinu")
                    .setSmallIcon(R.drawable.winter)
                    .setContentTitle("ברך עלינו הלילה!")
                    .setContentText("הלילה אנחנו מתחילים לומר ברך עלינו!")
                    .setStyle(new NotificationCompat
                            .BigTextStyle()
                            .setBigContentTitle("ברך עלינו הלילה!")
                            .setSummaryText("הלילה אנחנו מתחילים לומר ברך עלינו!")
                            .bigText("הלילה אנחנו מתחילים לומר ברך עלינו!"))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(alarmSound)
                    .setColor(context.getColor(R.color.dark_gold))
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent);
            notificationManager.notify(MID, mNotifyBuilder.build());
        } else {
            NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "BarechAleinu")
                    .setSmallIcon(R.drawable.winter)
                    .setContentTitle("Barech Aleinu tonight!")
                    .setContentText("Tonight we start saying Barech Aleinu!")
                    .setStyle(new NotificationCompat
                            .BigTextStyle()
                            .setBigContentTitle("Barech Aleinu tonight!")
                            .setSummaryText("Tonight we start saying Barech Aleinu!")
                            .bigText("Tonight we start saying Barech Aleinu!"))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(alarmSound)
                    .setColor(context.getColor(R.color.dark_gold))
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent);
            notificationManager.notify(MID, mNotifyBuilder.build());
        }
        MID++;
    }

    @NonNull
    private ROZmanimCalendar getROZmanimCalendar(Context context) {
        if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            mLocationResolver.getRealtimeNotificationData(this);// we will continue in the accept method
        }
        return new ROZmanimCalendar(new GeoLocation(
                mSharedPreferences.getString("name", ""),
                Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)),
                Double.longBitsToDouble(mSharedPreferences.getLong("long", 0)),
                getLastKnownElevation(context, Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)), Double.longBitsToDouble(mSharedPreferences.getLong("long", 0))),
                TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", TimeZone.getDefault().getID()))));
    }

    private double getLastKnownElevation(Context context, double latitude, double longitude) {
        double elevation;
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            elevation = 0;
        } else if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mLocationResolver.getLocationName(latitude, longitude), "0"));//get the elevation using the location name
        } else {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        }
        return elevation;
    }

    private void updateAlarm(Context context, ROZmanimCalendar c) {
        Calendar calendar = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (mSharedPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            calendar.setTimeInMillis(c.getTzeitAmudeiHoraah().getTime());
        } else {
            calendar.setTimeInMillis(c.getTzeit().getTime());
        }
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        am.cancel(omerPendingIntent);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), omerPendingIntent);
    }

    @Override
    public void accept(Location location) {
        if (location != null) {
            init(context, new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)), new ROZmanimCalendar(new GeoLocation(
                    mLocationResolver.getLocationName(location.getLatitude(), location.getLongitude()),
                    location.getLatitude(),
                    location.getLongitude(),
                    getLastKnownElevation(context, location.getLatitude(), location.getLongitude()),
                    mLocationResolver.getTimeZone())));
        }
    }
}
