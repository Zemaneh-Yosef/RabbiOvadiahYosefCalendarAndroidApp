package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;

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
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

public class DailyNotifications extends BroadcastReceiver implements Consumer<Location> {

    private LocationResolver mLocationResolver;
    private SharedPreferences mSharedPreferences;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Daily Notifications Received" + "\n\n").apply();
        }
        JewishDateInfo jewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael",false));
        mLocationResolver = new LocationResolver(context, null);
        if (mSharedPreferences.getBoolean("isSetup",false)) {
            ROZmanimCalendar calendar = getROZmanimCalendar();
            if (!calendar.getGeoLocation().equals(new GeoLocation())) {
                if (BuildConfig.DEBUG) {
                    mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Daily notifications init called with a saved location/zipcode" + "\n\n").apply();
                }
                init(context, jewishDateInfo, calendar);
            }
        }
    }

    private void init(Context context, JewishDateInfo jewishDateInfo, ROZmanimCalendar calendar) {
        String specialDay = jewishDateInfo.getSpecialDay(false);
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", "ShowDayOfOmer: " + PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ShowDayOfOmer",false) + "\n\n").apply();
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "init started with special day as: " + specialDay + "\n\n").apply();
        }

        if (!specialDay.isEmpty()) {
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "special day was not empty" + "\n\n").apply();
            }
            long when = System.currentTimeMillis();
            if (calendar.getSunrise() != null) {
                when = calendar.getSunrise().getTime();
            }
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel("Jewish Special Day",
                    "Daily Special Day Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("This notification will check daily if there is a special jewish day and display it at sunrise.");
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

            String title = (Utils.isLocaleHebrew(context) ? "יום מיוחד ביהדות" : "Jewish Special Day");
            String content = (Utils.isLocaleHebrew(context) ? "היום הוא " : "Today is ") + specialDay;

            NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "Jewish Special Day")
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                    .setSmallIcon(R.drawable.calendar_foreground)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle(title)
                            .setSummaryText(calendar.getGeoLocation().getLocationName())
                            .bigText(content))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(alarmSound)
                    .setColor(context.getColor(R.color.dark_gold))
                    .setAutoCancel(true)
                    .setWhen(when)
                    .setContentIntent(pendingIntent);
            notificationManager.notify(50, mNotifyBuilder.build());// keep the notification ID the same as it will just overwrite the last sent daily notification.
            // This is overall a better solution instead of keeping track of the date (like we used to) to ensure one notification per day.
            // We will just let the code notify the user and use the same ID to make it look like it only ran and notified once.
            // For reference:
            // IDs 0-49 are designated for Omer Notifications
            // ID 50 is designated for Daily Notifications (the ID will not iterate anymore, IT WILL OVERWRITE THE LAST DAILY NOTIFICATION)
            // ID 51 is designated to tekufa notifications (the ID will not iterate anymore as it only occurs 4 times a year and it's unlikely to ever overwrite the last tekufa notification)
            // ID 52 is designated to the Barech Aleinu notification
            // ID Integer.MAX_VALUE is designated to the Visible sunrise notification due to chatGPT recommendation, we could make it 53... but what's done is done
            // The zemanim notifications are based on the timestamp of the zeman (which is bigger than Integer.MAX_VALUE so we remainder it, either way they should never overwrite each other)
        }
        Calendar cal = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Class<?> notifClass = TekufaNotifications.class;
        Date tekufaDate = jewishDateInfo.getJewishCalendar().getTekufaAsDate();
        String tekufaOpinions = PreferenceManager.getDefaultSharedPreferences(context).getString("TekufaOpinions", "1");
        switch (tekufaOpinions) {
            case "1" -> {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("LuachAmudeiHoraah", false)) {
                    notifClass = AmudeiHoraahTekufaNotifications.class;
                }// otherwise leave alone
            }
            // 2, just leave it alone
            case "3" -> notifClass = AmudeiHoraahTekufaNotifications.class;
            case "4" -> notifClass = CombinedTekufaNotifications.class;
        }
        if (notifClass.equals(AmudeiHoraahTekufaNotifications.class) || notifClass.equals(CombinedTekufaNotifications.class)) {
            while (tekufaDate == null) {
                jewishDateInfo.getJewishCalendar().forward(Calendar.DATE, 1);
                tekufaDate = jewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate();
            }
        } else {
            while (tekufaDate == null) {
                jewishDateInfo.getJewishCalendar().forward(Calendar.DATE, 1);
                tekufaDate = jewishDateInfo.getJewishCalendar().getTekufaAsDate();
            }
        }
        cal.setTime(tekufaDate);
        cal.add(Calendar.HOUR_OF_DAY, -1);// set reminder to go off one hour before the tekufa occurs. I.E. half an hour before the prohibition
        if (cal.compareTo(Calendar.getInstance()) > 0) {// only do this code if the tekufa hasn't passed
            PendingIntent tekufaPendingIntent = PendingIntent.getBroadcast(
                    context.getApplicationContext(),
                    0,
                    new Intent(context.getApplicationContext(), notifClass),
                    PendingIntent.FLAG_IMMUTABLE);
            NotificationUtils.setExactAndAllowWhileIdle(am, cal.getTimeInMillis(), tekufaPendingIntent);
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Tekufa notification was set for: " + cal.getTime() + "\n\n").apply();
            }
        }

        updateAlarm(context, am, calendar);// for next day
        startUpDailyZmanim(context);//we need to start the zmanim service every day because there might be a person who will just want to see candle lighting time every week or once a year for pesach zmanim.
    }

    private ROZmanimCalendar getROZmanimCalendar() {
        return new ROZmanimCalendar(mLocationResolver.getRealtimeNotificationData(this, false));// we will continue in the accept method
    }

    private void startUpDailyZmanim(Context context) {
        PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(
                context.getApplicationContext(),
                0,
                new Intent(context.getApplicationContext(), ZmanimNotifications.class),
                PendingIntent.FLAG_IMMUTABLE);
        try {
            zmanimPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private void updateAlarm(Context context, AlarmManager am, AstronomicalCalendar c) {
        Calendar cal = Calendar.getInstance();
        Date sunrise = c.getSunrise();
        if (sunrise == null) {
            sunrise = new Date();
        }
        cal.setTimeInMillis(sunrise.getTime());
        if (cal.getTime().compareTo(new Date()) < 0) {
            cal.add(Calendar.DATE, 1);
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationUtils.setExactAndAllowWhileIdle(am, cal.getTimeInMillis(), dailyPendingIntent);
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Daily notifications alarm was updated to: " + cal.getTime() + "\n\n").apply();
        }
    }

    @Override
    public void accept(Location location) {
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "got a location object in DailyNotifications" + "\n\n").apply();
        }
        if (location != null) {
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "location object in DailyNotifications was not null" + "\n\n").apply();
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
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "location object in DailyNotifications WAS null" + "\n\n").apply();
            }
            init(context, new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)), new ROZmanimCalendar(mLocationResolver.getLastKnownGeoLocation()));
        }
    }
}
