package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.classes.Utils.dateFormatPattern;

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
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManager;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.SecondTreatment;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ZmanNotification extends BroadcastReceiver {

    private static final long MINUTE_MILLI = 60_000;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsSharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (mSharedPreferences.getBoolean("isSetup",false)
                && mSettingsSharedPreferences.getBoolean("zmanim_notifications", true)) {
            notifyUser(context, new JewishCalendar(), intent.getStringExtra("zman"), intent.getIntExtra("secondsTreatment", 0));
        }
    }

    private void notifyUser(Context context, JewishCalendar jewishCalendar, String zman, int secondsTreatment) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Zmanim", "Daily Zemanim Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("This notification will display when zmanim are about to begin.");
            channel.enableLights(true);
            channel.enableVibration(true);
            //channel.setVibrationPattern(new long[]{0, 100, 200, 300});
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setAllowBubbles(true);
            }
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setLightColor(Color.BLUE);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(context, MainFragmentManager.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        try {
            String[] zmanSeparated = zman.split(":");//zman is in the format "zmanName:zmanTime" e.g. "Alot Hashachar:538383388"
            String zmanName = zmanSeparated[0];
            String zmanTime = zmanSeparated[1];

            if ((jewishCalendar.isAssurBemelacha() && !mSettingsSharedPreferences.getBoolean("zmanim_notifications_on_shabbat", true))) {
                return;//if the user does not want to be notified on shabbat/yom tov, then return
            }
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            jewishCalendar.setDate(calendar);
            if ((jewishCalendar.isAssurBemelacha() && !mSettingsSharedPreferences.getBoolean("zmanim_notifications_on_shabbat", true))) {
                //if tomorrow is shabbat/yom tov, then return if the zman is Tzait, Rabbeinu Tam, or Chatzot Layla (since they are obviously after shabbat/yom tov has started)
                if (zmanName.equals("חצות הלילה") ||
                        zmanName.equals("Midnight") ||
                        zmanName.equals("Ḥatzot Ha'Layla") ||
                        zmanName.equals("צאת הכוכבים") ||
                        zmanName.equals("Nightfall") ||
                        zmanName.equals("Tzet Ha'Kokhavim") ||
                        zmanName.equals("Rabbenu Tam") ||
                        zmanName.equals("רבינו תם")) {
                    return;
                }
            }
            //no need to reset the jewish calendar since we are only using it to check if tomorrow is shabbat/yom tov, but keep in mind that the date is set to tomorrow

            DateFormat noSecondDateFormat = new SimpleDateFormat(dateFormatPattern(false), Locale.getDefault());
            noSecondDateFormat.setTimeZone(new LocationResolver(context, null).getTimeZone()); //set the formatters time zone
            DateFormat yesSecondDateFormat = new SimpleDateFormat(dateFormatPattern(true), Locale.getDefault());
            yesSecondDateFormat.setTimeZone(new LocationResolver(context, null).getTimeZone()); //set the formatters time zone

            Date zmanAsDate = new Date(Long.parseLong(zmanTime));
            String time = Utils.formatZmanTime(context, zmanAsDate, SecondTreatment.values()[secondsTreatment]);
            String displayedText;
            if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
                displayedText = String.format("%s : %s", time, zmanName);
            } else {
                displayedText = zmanName + " is at " + time;
            }
            long notificationID = Long.parseLong(zmanTime);//the notification ID is the time of the zman

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Zmanim")
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                    .setSmallIcon(R.drawable.ic_baseline_alarm_24)
                    .setContentTitle(zmanName)
                    .setContentText(displayedText)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle(zmanName)
                            .setSummaryText(mSharedPreferences.getString("locationNameFN", ""))
                            .bigText(displayedText))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(alarmSound)
                    .setColor(context.getColor(R.color.dark_gold))
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .extend(new NotificationCompat.WearableExtender()
                            .setDismissalId(zman + notificationID))
                    .setContentIntent(pendingIntent);
            if (mSettingsSharedPreferences.getInt("autoDismissNotifications", -1) != -1) {
                builder.setTimeoutAfter((mSettingsSharedPreferences.getInt("autoDismissNotifications", -1) * MINUTE_MILLI) + 3000); // add 3 seconds because 0 milliseconds doesn't do anything
            }
            if (mSharedPreferences.getString("lastNotifiedZman", "").equals(zman + notificationID)) {
                return;// just in case, so we don't get two of the same zman
            }
            //the notification ID cannot be a long so we convert it to an int
            //however, the notification ID will lose precision if it is too large
            //so we make sure that the notification ID is not too large by modding it by the max int value
            notificationManager.notify((int) (notificationID % Integer.MAX_VALUE), builder.build());
            mSharedPreferences.edit().putString("lastNotifiedZman", zman + notificationID).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
