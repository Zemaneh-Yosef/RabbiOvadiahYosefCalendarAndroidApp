package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

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

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManager;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocaleChecker;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

public class DailyNotifications extends BroadcastReceiver implements Consumer<Location> {

    private static int MID = 50;
    private LocationResolver mLocationResolver;
    private SharedPreferences mSharedPreferences;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        JewishDateInfo jewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael",false));
        mLocationResolver = new LocationResolver(context, null);
        if (mSharedPreferences.getBoolean("isSetup",false)) {
            ROZmanimCalendar calendar = getROZmanimCalendar();
            if (!calendar.getGeoLocation().equals(new GeoLocation())) {
                init(context, jewishDateInfo, calendar);
            }
        }
    }

    private void init(Context context, JewishDateInfo jewishDateInfo, ROZmanimCalendar calendar) {
        String specialDay = jewishDateInfo.getSpecialDay(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ShowDayOfOmer",false));

        if (!specialDay.isEmpty()) {
            long when = calendar.getSunrise().getTime();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            }

            Intent notificationIntent = new Intent(context, MainFragmentManager.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if (!mSharedPreferences.getString("lastKnownDay","").equals(jewishDateInfo.getJewishDate())) {//We only want 1 notification a day.
                String title = (LocaleChecker.isLocaleHebrew() ? "יום מיוחד ביהדות" : "Jewish Special Day");
                String content = (LocaleChecker.isLocaleHebrew() ? "היום הוא " : "Today is ") + specialDay;

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
                notificationManager.notify(MID, mNotifyBuilder.build());

                MID++;
                mSharedPreferences.edit().putString("lastKnownDay", jewishDateInfo.getJewishDate()).apply();
            }
        }
        Calendar cal = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Class<?> notifClass = TekufaNotifications.class;
        Date tekufaDate = jewishDateInfo.getJewishCalendar().getTekufaAsDate();
        String tekufaOpinions = PreferenceManager.getDefaultSharedPreferences(context).getString("TekufaOpinions", "1");
        if (tekufaOpinions.equals("1")) {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("LuachAmudeiHoraah", false)) {
                notifClass = AmudeiHoraahTekufaNotifications.class;
            }// otherwise leave alone
        }
        // 2, just leave it alone
        else if (tekufaOpinions.equals("3")) {
            notifClass = AmudeiHoraahTekufaNotifications.class;
        }
        else if (tekufaOpinions.equals("4")) {
            notifClass = CombinedTekufaNotifications.class;
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
        }

        updateAlarm(context, am, calendar);// for next day
        startUpDailyZmanim(context);//we need to start the zmanim service every day because there might be a person who will just want to see candle lighting time every week or once a year for pesach zmanim.
    }

    private ROZmanimCalendar getROZmanimCalendar() {
        return new ROZmanimCalendar(mLocationResolver.getRealtimeNotificationData(this));// we will continue in the accept method
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
    }

    @Override
    public void accept(Location location) {
        if (location != null) {
            String locationName = mLocationResolver.getLocationAsName(location.getLatitude(), location.getLongitude());
            mLocationResolver.resolveElevation(() ->
                    init(context, new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)), new ROZmanimCalendar(new GeoLocation(
                            locationName,
                            location.getLatitude(),
                            location.getLongitude(),
                            mLocationResolver.getElevation(),
                            mLocationResolver.getTimeZone()))));
        }
    }
}
