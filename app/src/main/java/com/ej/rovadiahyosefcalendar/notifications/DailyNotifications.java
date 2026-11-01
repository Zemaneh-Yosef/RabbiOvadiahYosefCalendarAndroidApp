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
import android.graphics.Color;
import android.graphics.drawable.Icon;
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
import com.kosherjava.zmanim.util.GeoLocation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DailyNotifications extends BroadcastReceiver implements Consumer<Location> {

    private LocationResolver mLocationResolver;
    private SharedPreferences mSharedPreferences;
    private Context context;
    private PendingResult mPendingResult;
    private final AtomicBoolean mFinished = new AtomicBoolean();
    private final AtomicBoolean mGotLocation = new AtomicBoolean();
    private static volatile boolean channelCreated = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        mPendingResult = goAsync();
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
                init(jewishDateInfo, calendar);
                finishBroadcast();
            }
        } else {
            finishBroadcast();
        }
    }

    private void finishBroadcast() {
        if (mFinished.compareAndSet(false, true)) {
            mPendingResult.finish(); // REQUIRED TO AVOID ANR
        }
    }

    private void init(JewishDateInfo jewishDateInfo, ROZmanimCalendar calendar) {
        jewishDateInfo.resetLocale(context);
        String specialDay = jewishDateInfo.getSpecialDay(false);
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", "ShowDayOfOmer: " + PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ShowDayOfOmer",false) + "\n\n").apply();
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "init started with special day as: " + specialDay + "\n\n").apply();
        }

        if (!specialDay.isEmpty()) {
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "special day was not empty" + "\n\n").apply();
            }
            long when;
            if (calendar.getSunsetBasedOnElevationSetting() != null) {
                when = calendar.getSunsetBasedOnElevationSetting().toEpochMilli();
            } else {
                when = System.currentTimeMillis();
            }
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            ensureChannel(context);

            Intent notificationIntent = new Intent(context, MainFragmentManagerActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String title = (Utils.isLocaleHebrew(context) ? "יום מיוחד ביהדות" : "Jewish Special Day");
            String content = (Utils.isLocaleHebrew(context) ? "היום הוא " : "Today is ") + specialDay;

            NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "Jewish Special Day")
                    .setLargeIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
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
                    .setTimeoutAfter(259_200_000)// remove the notification after 3 days (259,200,000 milliseconds)
                    .setContentIntent(pendingIntent);
            notificationManager.notify(50, mNotifyBuilder.build());// keep the notification ID the same as it will just overwrite the last sent daily notification.
            // This is overall a better solution instead of keeping track of the date (like we used to) to ensure one notification per day.
            // We will just let the code notify the user and use the same ID to make it look like it only ran and notified once.
            // For reference:
            // IDs 0-49 are designated for Omer Notifications
            // ID 50 is designated for Daily Notifications (the ID will not iterate anymore, IT WILL OVERWRITE THE LAST DAILY NOTIFICATION)
            // ID 51 is designated to tekufa notifications (the ID will not iterate anymore as it only occurs 4 times a year, and it's unlikely to ever overwrite the last tekufa notification)
            // ID 52 is designated to the Barech Aleinu notification
            // ID 53 is designated to the Birkat Halevana notification
            // ID Integer.MAX_VALUE is designated to the Visible sunrise notification due to ChatGPT recommendation, we could make it 53... but what's done is done
            // The zemanim notifications are based on the timestamp of the zeman (which is bigger than Integer.MAX_VALUE so we remainder it, either way they should never overwrite each other)
        }
        setTekufaNotification(context, jewishDateInfo);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        updateAlarm(am, calendar);// for next day
        startUpDailyZmanim();//we need to start the zmanim service every day because there might be a person who will just want to see candle lighting time every week or once a year for pesach zmanim.
    }

    static void setTekufaNotification(Context context, JewishDateInfo jewishDateInfo) {
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Class<?> notifClass = TekufaNotifications.class;
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
        for (Class<?> tekufaClass : new Class<?>[] {TekufaNotifications.class, AmudeiHoraahTekufaNotifications.class, CombinedTekufaNotifications.class}) {
            if (!tekufaClass.equals(notifClass)) {
                am.cancel(PendingIntent.getBroadcast(context.getApplicationContext(), 0, new Intent(context.getApplicationContext(), tekufaClass), PendingIntent.FLAG_IMMUTABLE));
            }
        }
        boolean luachAmudeiHoraah = notifClass.equals(AmudeiHoraahTekufaNotifications.class) || notifClass.equals(CombinedTekufaNotifications.class);
        Instant tekufaInstant = jewishDateInfo.getJewishCalendar().getTekufaAsInstant(luachAmudeiHoraah);
        while (tekufaInstant == null) {
            jewishDateInfo.setLocalDate(jewishDateInfo.getJewishCalendar().getLocalDate().plusDays(1));
            tekufaInstant = jewishDateInfo.getJewishCalendar().getTekufaAsInstant(luachAmudeiHoraah);
        }
        // set reminder to go off one hour before the tekufa occurs. I.E. half an hour before the prohibition
        if (tekufaInstant.compareTo(Instant.now()) > 0) {// only do this code if the tekufa hasn't passed
            PendingIntent tekufaPendingIntent = PendingIntent.getBroadcast(
                    context.getApplicationContext(),
                    0,
                    new Intent(context.getApplicationContext(), notifClass),
                    PendingIntent.FLAG_IMMUTABLE);
            NotificationUtils.setExactAndAllowWhileIdle(am, tekufaInstant.toEpochMilli(), tekufaPendingIntent);
            if (BuildConfig.DEBUG) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
                sharedPreferences.edit().putString("debugNotifs", sharedPreferences.getString("debugNotifs", "") + "Tekufa notification was set for: " + tekufaInstant + "\n\n").apply();
            }
        }
    }

    private ROZmanimCalendar getROZmanimCalendar() {
        return new ROZmanimCalendar(mLocationResolver.getRealtimeNotificationData(this, false));// we will continue in the accept method
    }

    /**
     * This method makes sure to only create the channel once per process to avoid unneeded computation.
     * @param context the context
     */
    static void ensureChannel(Context context) {
        if (channelCreated) return;
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                "Jewish Special Day", "Daily Special Day Notifications", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("This notification will check daily if there is a special jewish day and display it at sunrise.");
        channel.enableLights(true);
        channel.enableVibration(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(true);
        }
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setLightColor(Color.BLUE);
        notificationManager.createNotificationChannel(channel);
        channelCreated = true;
    }

    private void startUpDailyZmanim() {
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

    private void updateAlarm(AlarmManager am, ROZmanimCalendar c) {
        ROZmanimCalendar tomorrow = c.getCopy();
        tomorrow.setLocalDate(tomorrow.getLocalDate().plusDays(1));// only our alarm starts this receiver, so today's notification was just sent
        Instant sunrise = tomorrow.getSunriseBasedOnElevationSetting();
        if (sunrise == null) {
            sunrise = Instant.now();
        }
        if (sunrise.compareTo(Instant.now()) < 0) {
            if (tomorrow.getSunriseBasedOnElevationSetting() != null) {
                sunrise = tomorrow.getSunriseBasedOnElevationSetting();
            } else {
                sunrise = sunrise.plus(1, ChronoUnit.DAYS);
            }
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationUtils.setExactAndAllowWhileIdle(am, sunrise.toEpochMilli(), dailyPendingIntent);
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Daily notifications alarm was updated to: " + sunrise.toEpochMilli() + "\n\n").apply();
        }
    }

    @Override
    public void accept(Location location) {
        if (!mGotLocation.compareAndSet(false, true)) {
            return;
        }
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "got a location object in DailyNotifications" + "\n\n").apply();
        }
        if (location != null) {
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "location object in DailyNotifications was not null" + "\n\n").apply();
            }
            String locationName = mLocationResolver.getLocationAsName(location.getLatitude(), location.getLongitude());
            mLocationResolver.resolveElevation(() -> {
                init(new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)), new ROZmanimCalendar(new GeoLocation(
                        locationName,
                        location.getLatitude(),
                        location.getLongitude(),
                        mLocationResolver.getElevation(),
                        mLocationResolver.getZoneID())));
                finishBroadcast();
            });
        } else {
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "location object in DailyNotifications WAS null" + "\n\n").apply();
            }
            init(new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)), new ROZmanimCalendar(mLocationResolver.getLastKnownGeoLocation()));
            finishBroadcast();
        }
    }
}
