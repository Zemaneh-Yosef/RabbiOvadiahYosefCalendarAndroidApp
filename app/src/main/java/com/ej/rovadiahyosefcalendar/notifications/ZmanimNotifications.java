package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

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
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainActivity;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanInformationHolder;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ZmanimNotifications extends BroadcastReceiver {

    private static int MID = 50;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsSharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (mSharedPreferences.getBoolean("isSetup",false)) {
            ROZmanimCalendar zmanimCalendar = getROZmanimCalendar();
            if (!mSharedPreferences.getBoolean("findNextZman", false)) {//if true, then this call just wants us to find the next zman to set a notification for
                notifyUser(context, zmanimCalendar, mSharedPreferences.getString("zman",""));//otherwise, notify the user of the current zman to be
            }
            setAlarmToNextZman(context, zmanimCalendar);
        }
    }

    private void notifyUser(Context context, ROZmanimCalendar zmanimCalendar, String zman) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Zmanim", "Daily Zmanim Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("This notification will display when zmanim are about to begin.");
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
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String[] zmanSeparated = zman.split(":");//zman is in the format "zmanName:zmanTime" e.g. "Alot Hashachar:538383388"
        String zmanName = zmanSeparated[0];
        String zmanTime = zmanSeparated[1];

        Date zmanAsDate = new Date(Long.parseLong(zmanTime));

        DateFormat zmanimFormat;
        if (mSettingsSharedPreferences.getBoolean("ShowSeconds", false)) {
            zmanimFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", ""))); //set the formatters time zone

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                "Zmanim").setSmallIcon(R.drawable.calendar_foreground)
                .setContentTitle(zmanName)
                .setContentText(String.format("%s : %s", zmanimFormat.format(zmanAsDate), zmanName))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(zmanName)
                        .setSummaryText(zmanimCalendar.getGeoLocation().getLocationName())
                        .bigText(String.format("%s : %s", zmanimFormat.format(zmanAsDate), zmanName)))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setColor(context.getColor(R.color.dark_gold))
                .setAutoCancel(true)
                .setWhen(zmanAsDate.getTime())
                .setContentIntent(pendingIntent);
        notificationManager.notify(MID, builder.build());
        MID++;
    }

    /**
     * This method will instantiate a ROZmanimCalendar object using the last known location of the device when the app was last opened.
     */
    @NonNull
    private ROZmanimCalendar getROZmanimCalendar() {
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

    private void setAlarmToNextZman(Context context, ROZmanimCalendar c) {
        mSharedPreferences.edit().putBoolean("findNextZman", false).apply();//reset the flag to search for the next zman
        mSharedPreferences.edit().putString("zman", "").apply();//clear the last zman

        JewishCalendar jewishCalendar = new JewishCalendar();
        ArrayList<ZmanInformationHolder> zmanim = getArrayOfZmanim(c, jewishCalendar, context);

        Date nextZman = null;
        String nextZmanName = null;
        int minutesBeforeZman = 0;
        for (ZmanInformationHolder zman : zmanim) {
            Pair<String, Date> zmanPair = zman.getNameDatePair();
            if (zmanPair.second != null && zmanPair.second.after(new Date())) {
                if (nextZman == null) {
                    nextZman = zmanPair.second;
                    nextZmanName = zmanPair.first;
                    minutesBeforeZman = zman.getNotificationDelay();
                } else if (zmanPair.second.after(new Date()) && zmanPair.second.before(nextZman)) {
                    nextZman = zmanPair.second;
                    nextZmanName = zmanPair.first;
                    minutesBeforeZman = zman.getNotificationDelay();
                }
            }
        }
        if (nextZman != null && nextZman.before(new Date())) {//if the next zman is in the next day e.g. chatzot layla is at 11:44pm and we want to find alot hashachar
            nextZman = null;
            nextZmanName = null;
            minutesBeforeZman = 0;
            c.getCalendar().add(Calendar.DATE, 1);
            jewishCalendar.setDate(c.getCalendar());
            ArrayList<ZmanInformationHolder> zmanimTomorrow = getArrayOfZmanim(c, jewishCalendar, context);
            for (ZmanInformationHolder zman : zmanimTomorrow) {
                Pair<String, Date> zmanPair = zman.getNameDatePair();
                if (zmanPair.second != null && zmanPair.second.after(new Date())) {
                    if (nextZman == null) {
                        nextZman = zmanPair.second;
                        nextZmanName = zmanPair.first;
                        minutesBeforeZman = zman.getNotificationDelay();
                    } else if (zmanPair.second.after(new Date()) && zmanPair.second.before(nextZman)) {
                        nextZman = zmanPair.second;
                        nextZmanName = zmanPair.first;
                        minutesBeforeZman = zman.getNotificationDelay();
                    }
                }
            }
        }
        if (nextZman != null) {//we'll be extra careful
            String nextZmanTime = nextZman.getTime() + "";
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(context.getApplicationContext(), ZmanimNotifications.class);
            mSharedPreferences.edit().putString("zman", nextZmanName + ":" + nextZmanTime).apply();//send the zman name and time to the notification e.g. "Chatzot Layla:1331313311"
            PendingIntent zmanPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),0,intent,PendingIntent.FLAG_IMMUTABLE);
            am.set(AlarmManager.RTC_WAKEUP, nextZman.getTime() - (60_000L * minutesBeforeZman), zmanPendingIntent);
        }
    }

    private ArrayList<ZmanInformationHolder> getArrayOfZmanim(ROZmanimCalendar c, JewishCalendar jewishCalendar, Context context) {
        ArrayList<ZmanInformationHolder> pairArrayList = new ArrayList<>();
        String[] zmanNames;
        if (context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("isZmanimInHebrew", false)) {
            zmanNames = new String[]{
                    "חצות הלילה",
                    "רבינו תם",
                    "צאת " + getShabbatAndOrChag(jewishCalendar),
                    "צאת צום לחומרה",
                    "צאת צום",
                    "צאת הככבים",
                    "שקיעה",
                    "הדלקת נרות",
                    "פלג המנחה",
                    "מנחה קטנה",
                    "מנחה גדולה",
                    "חצות",
                    "סוף זמן ביעור חמץ",
                    "סוף זמן תפילה",
                    "סוף זמן אכילת חמץ",
                    "סוף זמן שמע גר'א",
                    "סוף זמן שמע מג'א",
                    "זריחה",
                    "טלית ותפילין",
                    "עלות השחר"};
        } else if (context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("isZmanimEnglishTranslated", false)) {
            zmanNames = new String[]{
                    "Midnight",
                    "Rabbeinu Tam",
                    getShabbatAndOrChag(jewishCalendar) + " Ends",
                    "Fast Ends L'Chumra",
                    "Fast Ends",
                    "Tzeit Hachochavim",
                    "Sunset",
                    "Candle Lighting",
                    "Plag HaMincha",
                    "Mincha Ketana",
                    "Mincha Gedola",
                    "Mid-Day",
                    "Latest Biur Chametz",
                    "Latest Brachot Shma",
                    "Latest Achilat Chametz",
                    "Latest Shma GRA",
                    "Latest Shma MGA",
                    "Sunrise",
                    "Earliest Talit and Tefilin",
                    "Dawn"};
        } else {
            zmanNames = new String[]{
                    "Chatzot Layla",
                    "Rabbeinu Tam",
                    getShabbatAndOrChag(jewishCalendar) + " Ends",
                    "Tzait Taanit L'Chumra",
                    "Tzait Taanit",
                    "Tzeit Hachochavim",
                    "Shkia",
                    "Candle Lighting",
                    "Plag HaMincha",
                    "Mincha Ketana",
                    "Mincha Gedola",
                    "Chatzot",
                    "Sof Zman Biur Chametz",
                    "Sof Zman Brachot Shma",
                    "Sof Zman Achilat Chametz",
                    "Sof Zman Shma GRA",
                    "Sof Zman Shma MGA",
                    "Sunrise",
                    "Earliest Talit and Tefilin",
                    "Alot Hashachar"};
        }
        if (!mSettingsSharedPreferences.getBoolean("zmanim_notifications", true)) {
            return pairArrayList;//if the user doesn't want notifications, return an empty array
        }
        //we only add the zmanim that are relevant to the current day that the method is called on and if the user has chosen to show them

        int minutesBefore = mSettingsSharedPreferences.getInt("NightChatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[0], c.getSolarMidnight()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("RabbeinuTam", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[1], c.getTzais72Zmanis()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("ShabbatEnd", -1);
        if (minutesBefore >= 0) {
            if (jewishCalendar.isAssurBemelacha() && !jewishCalendar.hasCandleLighting()) {
                pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[2], c.getTzaisAteretTorah()), minutesBefore));//only add if it's shabbat or yom tov
            }
        }

        if (jewishCalendar.isTaanis() && jewishCalendar.getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {//only add if it's a taanit and not yom kippur
            minutesBefore = mSettingsSharedPreferences.getInt("FastEndStringent", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[3], c.getTzaitTaanitLChumra()), minutesBefore));
            }
            minutesBefore = mSettingsSharedPreferences.getInt("FastEnd", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[4], c.getTzaitTaanit()), minutesBefore));
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("TzeitHacochavim", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[5], c.getTzeit()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Shkia", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[6], c.getSunset()), minutesBefore));//always add
        }

        if ((jewishCalendar.hasCandleLighting() &&
                !jewishCalendar.isAssurBemelacha()) ||
                jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {//only add if it's a day before shabbat/yom tov and not a 2 day yom tov or shabbat
            minutesBefore = mSettingsSharedPreferences.getInt("CandleLighting", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[7], c.getCandleLighting()), minutesBefore));
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("PlagHaMincha", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[8], c.getPlagHamincha()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("MinchaKetana", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[9], c.getMinchaKetana()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("MinchaGedola", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[10], c.getMinchaGedolaGreaterThan30()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Chatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[11], c.getChatzos()), minutesBefore));//always add
        }

        if (jewishCalendar.getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanBiurChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[12], c.getSofZmanBiurChametzMGA()), minutesBefore));//only add if it's erev pesach
            }

            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[13], c.getSofZmanTfilaGRA()), minutesBefore));//always add
            }

            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanAchilatChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[14], c.getSofZmanTfilaMGA72MinutesZmanis()), minutesBefore));//Achilat Chametz
            }
        } else {
            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[13], c.getSofZmanTfilaGRA()), minutesBefore));//always add
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("SofZmanShmaGRA", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[15], c.getSofZmanShmaGRA()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("SofZmanShmaMGA", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[16], c.getSofZmanShmaMGA72MinutesZmanis()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("HaNetz", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[17], c.getSunrise()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("TalitTefilin", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[18], c.getEarliestTalitTefilin()), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Alot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(new Pair<>(zmanNames[19], c.getAlos72Zmanis()), minutesBefore));//always add
        }

        return pairArrayList;
    }

    private String getShabbatAndOrChag(JewishCalendar jewishCalendar) {
        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            if (jewishCalendar.isYomTov() &&
                    jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "\u05E9\u05D1\u05EA/\u05D7\u05D2";
            } else if (jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "\u05E9\u05D1\u05EA";
            } else {
                return "\u05D7\u05D2";
            }
        } else {
            if (jewishCalendar.isYomTov() &&
                    jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat/Chag";
            } else if (jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat";
            } else {
                return "Chag";
            }
        }
    }
}
