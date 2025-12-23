package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.OmerActivity.omerList;

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

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.BuildConfig;
import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity;
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Omer notifications called at: " + new Date() + "\n\n").apply();
        }
        JewishDateInfo jewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false));
        mLocationResolver = new LocationResolver(context, null);
        if (mSharedPreferences.getBoolean("isSetup", false)) {
            ROZmanimCalendar c = getROZmanimCalendar();
            if (!c.getGeoLocation().equals(new GeoLocation())) {
                if (BuildConfig.DEBUG) {
                    mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Omer notifications init called with a saved location/zipcode" + "\n\n").apply();
                }
                init(context, jewishDateInfo, c);
            }
        }
    }

    private void init(Context context, JewishDateInfo jewishDateInfo, ROZmanimCalendar c) {
        int day = jewishDateInfo.getJewishCalendar().getDayOfOmer();
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "init started with Omer day as: " + day + "\n\n").apply();
        }
        if (day != -1 && day != 49) {//we don't want to send a notification right before shavuot
            long when = System.currentTimeMillis();
            c.setAmudehHoraah(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("LuachAmudeiHoraah", false));
            if (c.getTzeit() != null) {
                when = c.getTzeit().getTime();
            }
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel("Omer",
                    "Omer Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("This notification will check daily if it is the omer and it will display which day it is at sunset.");
            channel.enableLights(true);
            channel.enableVibration(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setAllowBubbles(true);
            }
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setLightColor(Color.BLUE);
            notificationManager.createNotificationChannel(channel);

            Intent notificationIntent = new Intent(context, SiddurViewActivity.class)
                    .putExtra("prayer", context.getString(R.string.sefirat_haomer))
                    .putExtra("JewishDay", jewishDateInfo.tomorrow().getJewishCalendar().getJewishDayOfMonth())
                    .putExtra("JewishMonth", jewishDateInfo.tomorrow().getJewishCalendar().getJewishMonth())
                    .putExtra("JewishYear", jewishDateInfo.tomorrow().getJewishCalendar().getJewishYear());
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String nextJewishDay = jewishDateInfo.tomorrow().getJewishCalendar().toString();
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "The hebrew date for the omer is: " + nextJewishDay + "\n\n").apply();
            }

            if (!mSharedPreferences.getString("lastKnownDayOmer", "").equals(nextJewishDay)) {//We only want 1 notification a day.
                mSharedPreferences.edit().putString("lastKnownDayOmer", nextJewishDay).apply();
                if (BuildConfig.DEBUG) {
                    mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "The hebrew date for the omer was not the same as the last known day: " + nextJewishDay + "\n\n").apply();
                }
                NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "Omer")
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setSmallIcon(R.drawable.omer_wheat)
                        .setContentTitle(Locale.getDefault().getDisplayLanguage(new Locale.Builder().setLanguage("en").setRegion("US").build()).equals("Hebrew") ? "יום בעומר" : "Day of Omer")
                        .setContentText(omerList.get(day))
                        .setStyle(new NotificationCompat
                                .BigTextStyle()
                                .setBigContentTitle(nextJewishDay)
                                .setSummaryText(Locale.getDefault().getDisplayLanguage(new Locale.Builder().setLanguage("en").setRegion("US").build()).equals("Hebrew") ? "אל תשכח לספור!" : "Don't forget to count!")
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
                if (BuildConfig.DEBUG) {
                    mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Omer notification was sent" + "\n\n").apply();
                }
            }
        }// end of omer code
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "check if barech aleinu is tonight" + "\n\n").apply();
        }
        if (new TefilaRules().isVeseinTalUmatarStartDate(jewishDateInfo.tomorrow().getJewishCalendar())) {// we need to know if user is in Israel or not
            if (mSharedPreferences.getInt("lastKnownBarechAleinu", 0) != jewishDateInfo.getJewishCalendar().getJewishYear()) {//We only want 1 notification a year.
                notifyBarechAleinu(context);
                mSharedPreferences.edit().putInt("lastKnownBarechAleinu", jewishDateInfo.getJewishCalendar().getJewishYear()).apply();
            }
        }
        updateAlarm(context, c);
    }

    private void notifyBarechAleinu(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

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

        Intent notificationIntent = new Intent(context, MainFragmentManagerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (Utils.isLocaleHebrew()) {
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
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "barech aleinu notification was sent" + "\n\n").apply();
        }
    }

    private ROZmanimCalendar getROZmanimCalendar() {
        return new ROZmanimCalendar(mLocationResolver.getRealtimeNotificationData(this, false));// we will continue in the accept method
    }

    private void updateAlarm(Context context, ROZmanimCalendar c) {
        Calendar calendar = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Date tzeit = c.getTzeit();
        if (tzeit == null) {
            tzeit = new Date();
        }
        calendar.setTimeInMillis(tzeit.getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationUtils.setExactAndAllowWhileIdle(am, calendar.getTimeInMillis(), omerPendingIntent);
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Omer notifications alarm was updated to: " + calendar.getTime() + "\n\n").apply();
        }
    }

    @Override
    public void accept(Location location) {
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "got a location object in OmerNotifications" + "\n\n").apply();
        }
        if (location != null) {
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "location object in OmerNotifications was not null" + "\n\n").apply();
            }
            String locationName = mLocationResolver.getLocationAsName(location.getLatitude(), location.getLongitude());
            mLocationResolver.resolveElevation(() ->
                    init(context, new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)), new ROZmanimCalendar(new GeoLocation(
                            locationName,
                            location.getLatitude(),
                            location.getLongitude(),
                            mLocationResolver.getElevation(),
                            mLocationResolver.getTimeZone()))));
        } else {
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "location object in OmerNotifications WAS null" + "\n\n").apply();
            }
            init(context, new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)), new ROZmanimCalendar(mLocationResolver.getLastKnownGeoLocation()));
        }
    }
}
