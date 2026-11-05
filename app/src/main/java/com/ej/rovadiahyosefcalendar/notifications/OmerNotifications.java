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
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
import com.kosherjava.zmanim.util.GeoLocation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class OmerNotifications extends BroadcastReceiver implements Consumer<Location> {

    private LocationResolver mLocationResolver;
    private SharedPreferences mSharedPreferences;
    private Context context;
    private PendingResult mPendingResult;
    private final AtomicBoolean mFinished = new AtomicBoolean();
    private final AtomicBoolean mGotLocation = new AtomicBoolean();
    private static volatile boolean channelCreated = false;
    private static final String PREF_HALEVANA_DONE_MONTH = "birkatHalevanaDoneMonth";
    private static final String PREF_HALEVANA_LAST_NIGHT = "birkatHalevanaLastNotified";
    private static final String ACTION_HALEVANA_SAID = "com.ej.rovadiahyosefcalendar.HALEVANA_SAID";
    private static final String ACTION_HALEVANA_SKIP = "com.ej.rovadiahyosefcalendar.HALEVANA_SKIP";
    private static final String EXTRA_HALEVANA_MONTH = "halevanaMonth";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_HALEVANA_SAID.equals(action) || ACTION_HALEVANA_SKIP.equals(action)) {
            handleBirkatHalevanaAction(context, intent);
            return;// a button press, not the nightly alarm
        }
        mPendingResult = goAsync();
        this.context = context;
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Omer notifications called at: " + Instant.now() + "\n\n").apply();
        }
        JewishDateInfo jewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false));
        mLocationResolver = new LocationResolver(context, null);
        if (mSharedPreferences.getBoolean("isSetup", false)) {
            ROZmanimCalendar c = getROZmanimCalendar();
            if (!c.getGeoLocation().equals(new GeoLocation())) {
                if (BuildConfig.DEBUG) {
                    mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Omer notifications init called with a saved location/zipcode" + "\n\n").apply();
                }
                init(jewishDateInfo, c);
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

    private void init(JewishDateInfo jewishDateInfo, ROZmanimCalendar c) {
        jewishDateInfo.resetLocale(context);
        c.setAmudehHoraah(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("LuachAmudeiHoraah", false));
        int night = jewishDateInfo.tomorrow().getJewishCalendar().getDayOfOmer();//the count that is said tonight
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "init started with the Omer count for tonight as: " + night + "\n\n").apply();
        }
        if (night != -1) {//-1 outside the omer, and on erev shavuot there is no count to say
            long when;
            if (c.getTzeit() != null) {
                when = c.getTzeit().toEpochMilli();
            } else {
                when = System.currentTimeMillis();
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

            NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "Omer")
                    .setLargeIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                    .setSmallIcon(R.drawable.omer_wheat)
                    .setContentTitle(Utils.isLocaleHebrew(context) ? "יום לעומר" : "Day of Omer")
                    .setContentText(omerList.get(night - 1))
                    .setStyle(new NotificationCompat
                            .BigTextStyle()
                            .setBigContentTitle(nextJewishDay)
                            .setSummaryText(Utils.isLocaleHebrew(context) ? "אל תשכח לספור!" : "Don't forget to count!")
                            .bigText("בָּרוּךְ אַתָּה יְהֹוָה, אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם, אֲשֶׁר קִדְּשָׁנוּ בְּמִצְוֹתָיו וְצִוָּנוּ עַל סְפִירַת הָעֹמֶר:" + "\n\n" + omerList.get(night - 1) + "\n\nהָרַחֲמָן הוּא יַחֲזִיר עֲבוֹדַת בֵּית הַמִּקְדָּשׁ לִמְקוֹמָהּ בִּמְהֵרָה בְיָמֵינוּ אָמֵן:"))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(alarmSound)
                    .setColor(context.getColor(R.color.dark_gold))
                    .setAutoCancel(true)
                    .setWhen(when)
                    .setTimeoutAfter(259_200_000)// remove the notification after 3 days (259,200,000 milliseconds)
                    .setContentIntent(pendingIntent)
                    .addAction(new NotificationCompat.Action(0, context.getString(R.string.see_full_text), pendingIntent));
            notificationManager.notify(night - 1, mNotifyBuilder.build());
        }
        if (new TefilaRules().isVeseinTalUmatarStartDate(jewishDateInfo.tomorrow().getJewishCalendar())) {// we need to know if user is in Israel or not
            notifyBarechAleinu();
        }
        updateAlarm(c);
        try {
            notifyBirkatHalevana(jewishDateInfo, c);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyBarechAleinu() {
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
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "BarechAleinu")
                .setSmallIcon(R.drawable.winter)
                .setContentTitle(Utils.isLocaleHebrew(context) ? "ברך עלינו הלילה!" : "Barech Aleinu tonight!")
                .setContentText(Utils.isLocaleHebrew(context) ? "הלילה אנחנו מתחילים לומר ברך עלינו!" : "Tonight we start saying Barech Aleinu!")
                .setStyle(new NotificationCompat
                        .BigTextStyle()
                        .setBigContentTitle(Utils.isLocaleHebrew(context) ? "ברך עלינו הלילה!" : "Barech Aleinu tonight!")
                        .setSummaryText(Utils.isLocaleHebrew(context) ? "הלילה אנחנו מתחילים לומר ברך עלינו!" : "Tonight we start saying Barech Aleinu!")
                        .bigText(Utils.isLocaleHebrew(context) ? "הלילה אנחנו מתחילים לומר ברך עלינו!" : "Tonight we start saying Barech Aleinu!"))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setColor(context.getColor(R.color.dark_gold))
                .setAutoCancel(true)
                .setTimeoutAfter(259_200_000)// remove the notification after 3 days (259,200,000 milliseconds)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent);
        notificationManager.notify(52, mNotifyBuilder.build());
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "barech aleinu notification was sent" + "\n\n").apply();
        }
    }

    private void notifyBirkatHalevana(JewishDateInfo jewishDateInfo, ROZmanimCalendar c) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("birkat_halevana_notifications", false)) {
            return;// disabled by default
        }
        JewishCalendar today = jewishDateInfo.getJewishCalendar();
        String monthKey = today.getJewishYear() + "-" + today.getJewishMonth();
        if (monthKey.equals(mSharedPreferences.getString(PREF_HALEVANA_DONE_MONTH, ""))) {
            return;// the user already said it or doesn't want a reminder this month
        }
        String tonight = today.getLocalDate().toString();
        if (tonight.equals(mSharedPreferences.getString(PREF_HALEVANA_LAST_NIGHT, ""))) {
            return;// already notified tonight, init can run twice
        }
        String status = jewishDateInfo.getBirchatLevana(c.getGeoLocation().getZoneId());
        if (status.isEmpty()) {
            return;// outside the 7 - 14-day window
        }
        Instant now = Instant.now();
        Instant sunset = c.getSunset();
        if (sunset != null && now.isBefore(sunset)) {
            return;// only remind at night, not if the receiver ran during the day
        }

        ROZmanimCalendar tomorrow = c.getCopy();
        tomorrow.setLocalDate(tomorrow.getLocalDate().plusDays(1));
        Instant dawn = tomorrow.getAlotHashachar();
        if (dawn == null || dawn.isBefore(now)) {
            dawn = now.plus(12, ChronoUnit.HOURS);
        }
        Instant tzeit = c.getTzeit();
        if (tzeit == null || tzeit.isAfter(now)) {
            tzeit = now;
        }

        boolean hebrew = Utils.isLocaleHebrew(context);
        String moonSummary = Utils.getMoonSummary(context, c.getGeoLocation(), tzeit, dawn);

        ensureChannel(context);

        Intent openApp = new Intent(context, MainFragmentManagerActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent saidIntent = PendingIntent.getBroadcast(context, 5301,
                new Intent(context, OmerNotifications.class).setAction(ACTION_HALEVANA_SAID).putExtra(EXTRA_HALEVANA_MONTH, monthKey),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent skipIntent = PendingIntent.getBroadcast(context, 5302,
                new Intent(context, OmerNotifications.class).setAction(ACTION_HALEVANA_SKIP).putExtra(EXTRA_HALEVANA_MONTH, monthKey),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        String title = hebrew ? "ברכת הלבנה הלילה" : "Birkat Halevana tonight";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "BirkatHalevana")
                .setSmallIcon(R.drawable.moon)
                .setContentTitle(title)
                .setContentText(status)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(moonSummary.isEmpty() ? status : status + "\n\n" + moonSummary))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setColor(context.getColor(R.color.dark_gold))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setTimeoutAfter(Math.max(60_000L, dawn.toEpochMilli() - now.toEpochMilli()))// tomorrow night's reminder replaces it if not answered
                .setContentIntent(contentIntent)
                .addAction(new NotificationCompat.Action(0, hebrew ? "אמרתי" : "I said it", saidIntent))
                .addAction(new NotificationCompat.Action(0, hebrew ? "אין צורך בתזכורת החודש" : "No reminder this month", skipIntent));
        notificationManager.notify(53, builder.build());
        mSharedPreferences.edit().putString(PREF_HALEVANA_LAST_NIGHT, tonight).apply();
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "birkat halevana notification was sent" + "\n\n").apply();
        }
    }

    /**
     * This method makes sure to only create the channel once per process to avoid unneeded computation.
     * @param context the context
     */
    static void ensureChannel(Context context) {
        if (channelCreated) return;
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                "BirkatHalevana", "Birkat Halevana Notifications", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("This notification will remind you at night while Birkat Halevana can be said.");
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setLightColor(Color.BLUE);
        notificationManager.createNotificationChannel(channel);
        channelCreated = true;
    }

    private static void handleBirkatHalevanaAction(Context context, Intent intent) {
        String monthKey = intent.getStringExtra(EXTRA_HALEVANA_MONTH);
        if (monthKey != null) {// both buttons mean "stop reminding me until next month"
            context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit().putString(PREF_HALEVANA_DONE_MONTH, monthKey).apply();
        }
        context.getSystemService(NotificationManager.class).cancel(53);
    }

    private ROZmanimCalendar getROZmanimCalendar() {
        return new ROZmanimCalendar(mLocationResolver.getRealtimeNotificationData(this, false));// we will continue in the accept method
    }

    private void updateAlarm(ROZmanimCalendar c) {
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        ROZmanimCalendar tomorrow = c.getCopy();
        tomorrow.setLocalDate(tomorrow.getLocalDate().plusDays(1));// only our alarm starts this receiver, so tonight's count was just sent
        Instant tzeit = tomorrow.getTzeit();
        if (tzeit == null) {
            tzeit = Instant.now();
        }
        if (tzeit.compareTo(Instant.now()) < 0) {
            if (tomorrow.getTzeit() != null) {
                tzeit = tomorrow.getTzeit();
            } else {
                tzeit = tzeit.plus(1, ChronoUnit.DAYS);
            }
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationUtils.setExactAndAllowWhileIdle(am, tzeit.toEpochMilli(), omerPendingIntent);
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "Omer notifications alarm was updated to: " + tzeit + "\n\n").apply();
        }
    }

    @Override
    public void accept(Location location) {
        if (!mGotLocation.compareAndSet(false, true)) {
            return;
        }
        if (BuildConfig.DEBUG) {
            mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "got a location object in OmerNotifications" + "\n\n").apply();
        }
        if (location != null) {
            if (BuildConfig.DEBUG) {
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "location object in OmerNotifications was not null" + "\n\n").apply();
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
                mSharedPreferences.edit().putString("debugNotifs", mSharedPreferences.getString("debugNotifs", "") + "location object in OmerNotifications WAS null" + "\n\n").apply();
            }
            init(new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)), new ROZmanimCalendar(mLocationResolver.getLastKnownGeoLocation()));
            finishBroadcast();
        }
    }
}
