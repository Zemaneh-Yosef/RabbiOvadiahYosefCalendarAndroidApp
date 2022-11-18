package com.ej.rovadiahyosefcalendar.notifications;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanInformationHolder;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ZmanimNotifications extends BroadcastReceiver {

    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsSharedPreferences;
    private LocationResolver mLocationResolver;

    @Override
    public void onReceive(Context context, Intent intent) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mLocationResolver = new LocationResolver(context, new Activity());
        if (mSharedPreferences.getBoolean("isSetup",false) && mSettingsSharedPreferences.getBoolean("zmanim_notifications", true)) {
            JewishCalendar jewishCalendar = new JewishCalendar();
            ROZmanimCalendar zmanimCalendar = getROZmanimCalendar(context);
            mSharedPreferences.edit().putString("locationNameFN", zmanimCalendar.getGeoLocation().getLocationName()).apply();
            setAlarmToNextZman(context, zmanimCalendar, jewishCalendar);
        }
    }

    /**
     * This method will instantiate a ROZmanimCalendar object if the user has allowed the app to use their location, otherwise, it will use the last known location.
     */
    @NonNull
    private ROZmanimCalendar getROZmanimCalendar(Context context) {
        if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            mLocationResolver.getRealtimeNotificationData();
            //Log.w("ZmanimNotifications", "Latitude: " + mLocationResolver.getLatitude() + " Longitude: " + mLocationResolver.getLongitude());
            if (mLocationResolver.getLatitude() == 0 && mLocationResolver.getLongitude() == 0) {
                //Log.w("ZmanimNotifications", "Latitude and Longitude are both 0, we will use the last location");
                return new ROZmanimCalendar(new GeoLocation(
                        mSharedPreferences.getString("name", ""),
                        Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)),
                        Double.longBitsToDouble(mSharedPreferences.getLong("long", 0)),
                        getLastKnownElevation(),
                        TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", ""))));
            } else {
                //Log.w("ZmanimNotifications", "Latitude and Longitude are not 0, we will use the current location");
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

    /**
     * This method will set an alarm to the next zman that is coming up.
     * FIXME this method is not working properly, it is not setting the alarm to the next zman if there is a zman within the minutes before the zman afterwards.
     */
    private void setAlarmToNextZman(Context context, ROZmanimCalendar c, JewishCalendar jewishCalendar) {
        mSharedPreferences.edit().putString("zman", "").apply();//clear the last zman

        ZmanInformationHolder nextZman = null;
        int nextZmanIndex = 0;
        boolean isZmanYesterday = false;
        boolean isZmanToday = false;

        ArrayList<ZmanInformationHolder> zmanimYesterday;
        ArrayList<ZmanInformationHolder> zmanimToday;//we will get all the zmanim for today, tomorrow, and yesterday
        ArrayList<ZmanInformationHolder> zmanimTomorrow;//note that these zmanim start from chatzot layla to alot hashachar, so we will check from nightfall to dawn

        c.getCalendar().add(Calendar.DATE, -1);
        jewishCalendar.setDate(c.getCalendar());//set the calendars to the previous day
        zmanimYesterday = getArrayOfZmanim(c, jewishCalendar, context);

        c.getCalendar().add(Calendar.DATE, 1);
        jewishCalendar.setDate(c.getCalendar());//set the calendars to the current day
        zmanimToday = getArrayOfZmanim(c, jewishCalendar, context);

        c.getCalendar().add(Calendar.DATE, 1);
        jewishCalendar.setDate(c.getCalendar());//set the calendars to the next day
        zmanimTomorrow = getArrayOfZmanim(c, jewishCalendar, context);

        for (int i = 0; i < zmanimYesterday.size(); i++) {//check if the upcoming zman is yesterday
            Date zmanDate = zmanimYesterday.get(i).getZmanDate();
            if (zmanDate != null && zmanDate.after(new Date())) {
                if (nextZman == null) {
                    nextZman = zmanimYesterday.get(i);
                    isZmanYesterday = true;
                    nextZmanIndex = i;
                } else if (zmanDate.before(nextZman.getZmanDate())) {//probably a very rare occurrence
                    nextZman = zmanimYesterday.get(i);
                    nextZmanIndex = i;
                }
            }
        }

        if (nextZman == null || nextZman.getZmanDate().before(new Date())) {//if the next zman is in the past or null because there are no zmanim coming up for yesterday
            nextZman = null;
            isZmanYesterday = false;
            nextZmanIndex = 0;//reset the values
            for (int i = 0; i < zmanimToday.size(); i++) {//now we check today's zmanim
                Date zmanDate = zmanimToday.get(i).getZmanDate();
                if (zmanDate != null && zmanDate.after(new Date())) {
                    if (nextZman == null) {
                        nextZman = zmanimToday.get(i);
                        isZmanToday = true;
                        nextZmanIndex = i;
                    } else if (zmanDate.before(nextZman.getZmanDate())) {
                        nextZman = zmanimToday.get(i);
                        nextZmanIndex = i;
                    }
                }
            }
        }//most likely the next zman is in today's zmanim, however, if it is not, then we need to check tomorrow's zmanim

        if (nextZman == null || nextZman.getZmanDate().before(new Date())) {//the next zman is in the next day if nextZman is null or in the past
            nextZman = null;
            isZmanToday = false;
            nextZmanIndex = 0;//reset the values
            for (int i = 0; i < zmanimTomorrow.size(); i++) {//now we check tomorrow's zmanim
                ZmanInformationHolder zman = zmanimTomorrow.get(i);
                Date zmanDate = zman.getZmanDate();
                if (zmanDate != null && zmanDate.after(new Date())) {
                    if (nextZman == null) {
                        nextZman = zmanimTomorrow.get(i);
                        nextZmanIndex = i;
                    } else if (zmanDate.before(nextZman.getZmanDate())) {
                        nextZman = zmanimTomorrow.get(i);
                        nextZmanIndex = i;
                    }
                }
            }
        }//by now we have the next zman, unless the user just wants notifications for very distant zmanim like candle lighting and biur chametz

        if (nextZman != null) {
//            ZmanInformationHolder zmanAfterNextZman = null;
//            //find the next zman after the current zman just in case we already set an alarm for the current zman
//            if (isZmanYesterday) {
//                if (nextZmanIndex + 1 < zmanimYesterday.size()) {//if there is a next zman yesterday
//                    zmanAfterNextZman = zmanimYesterday.get(nextZmanIndex + 1);//set the next zman to the next zman in the list
//                } else if (nextZmanIndex + 1 == zmanimYesterday.size()) {//if there is no next zman yesterday, but there is a next zman today
//                    zmanAfterNextZman = zmanimToday.get(0);//set the next zman to the first zman in today's list
//                }
//            } else if (isZmanToday) {//if the next zman is today
//                if (nextZmanIndex + 1 < zmanimToday.size()) {//if there is another zman today
//                    zmanAfterNextZman = zmanimToday.get(nextZmanIndex + 1);//set the next zman to the next zman in the list
//                } else if (nextZmanIndex + 1 == zmanimToday.size()) {//if there is no next zman today, but there is a next zman tomorrow
//                    zmanAfterNextZman = zmanimTomorrow.get(0);//set the next zman to the first zman in tomorrow's list
//                }
//            } else {//if the next zman is tomorrow
//                if (nextZmanIndex + 1 < zmanimTomorrow.size()) {//if there is another zman tomorrow
//                    zmanAfterNextZman = zmanimTomorrow.get(nextZmanIndex + 1);//set the next zman to the next zman in the list
//                }
//            }
//            Log.d("ZmanimNotifications", "NEXT ZMAN FOUND!: "+ nextZman.getZmanName() + " : " + nextZman.getZmanDate());
//            Log.d("ZmanimNotifications", "NEXT ZMAN AFTER NEXT ZMAN: " + zmanAfterNextZman.getZmanDate());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(nextZman.getZmanDate());
            String zmanAndNotifDay = "" + calendar.get(Calendar.DAY_OF_YEAR) + "" + calendar.get(Calendar.YEAR);
            if (mSharedPreferences.getString("lastZmanNameAndDay", "").equals(nextZman.getZmanName() + zmanAndNotifDay) &&
                    new Date().after(new Date(nextZman.getZmanDate().getTime() - (long) nextZman.getNotificationDelay() * 60 * 1000)) &&
                    new Date().before(nextZman.getZmanDate())) {
                return;//not exactly sure why this check is necessary, but it is because we keep getting notifications for the same zman over and over again
            }
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            PendingIntent pendingIntentForScheduling = PendingIntent.getBroadcast(context.getApplicationContext(), 0, new Intent(context, ZmanimNotifications.class), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent zmanPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),0,new Intent(context, ZmanNotification.class),PendingIntent.FLAG_IMMUTABLE);
            if (mSharedPreferences.getString("lastZmanNameAndDay", "").equals(nextZman.getZmanName() + zmanAndNotifDay)
                    && mSharedPreferences.getBoolean("fromThisNotification", false)) {

//                Log.d("ZmanimNotifications", "The zman is the same as the last one we set an alarm for " + mSharedPreferences.getString("lastZmanNameAndDay", ""));
//                Log.d("ZmanimNotifications", "Set the alarm to come back here 1 minute after this zman passes");

                mSharedPreferences.edit().putBoolean("fromThisNotification", true).apply();
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextZman.getZmanDate().getTime() + 60_000, pendingIntentForScheduling);
                return;
            }

            String nextZmanTime = nextZman.getZmanDate().getTime() + "";
            mSharedPreferences.edit().putString("zman", nextZman.getZmanName() + ":" + nextZmanTime).apply();//save the zman name and time for the notification e.g. "Chatzot Layla:1331313311"
            mSharedPreferences.edit().putString("lastZmanNameAndDay", nextZman.getZmanName() + zmanAndNotifDay).apply();//save the next zman name with the day so we can check if it is the same as the last one we set an alarm for
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextZman.getZmanDate().getTime() - (60_000L * nextZman.getNotificationDelay()), zmanPendingIntent);

            mSharedPreferences.edit().putBoolean("fromThisNotification", true).apply();
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextZman.getZmanDate().getTime() + 60_000, pendingIntentForScheduling);

//            Log.d("ZmanimNotifications", "ZmanNotification was set to " + new Date(nextZman.getZmanDate().getTime() - (60_000L * nextZman.getNotificationDelay())) + " which is 15 minutes before the zman");
//            Log.d("ZmanimNotifications", "we will set an alarm to come back here 1 minute after this zman passes at " + new Date(nextZman.getZmanDate().getTime() + 60_000));
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
                    "Earliest Mincha",
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

        //we only add the zmanim that are relevant to the current day that the method is called on and if the user has chosen to show them

        int minutesBefore = mSettingsSharedPreferences.getInt("NightChatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[0], c.getSolarMidnight(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("RT", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[1], c.getTzais72Zmanis(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("ShabbatEnd", -1);
        if (minutesBefore >= 0) {
            if (jewishCalendar.isAssurBemelacha() && !jewishCalendar.hasCandleLighting()) {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[2], c.getTzaisAteretTorah(), minutesBefore));//only add if it's shabbat or yom tov
            }
        }

        if (jewishCalendar.isTaanis() && jewishCalendar.getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {//only add if it's a taanit and not yom kippur
            minutesBefore = mSettingsSharedPreferences.getInt("FastEndStringent", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[3], c.getTzaitTaanitLChumra(), minutesBefore));
            }
            minutesBefore = mSettingsSharedPreferences.getInt("FastEnd", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[4], c.getTzaitTaanit(), minutesBefore));
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("TzeitHacochavim", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[5], c.getTzeit(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Shkia", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[6], c.getSunset(), minutesBefore));//always add
        }

        if ((jewishCalendar.hasCandleLighting() &&
                !jewishCalendar.isAssurBemelacha()) ||
                jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {//only add if it's a day before shabbat/yom tov and not a 2 day yom tov or shabbat
            minutesBefore = mSettingsSharedPreferences.getInt("CandleLighting", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[7], c.getCandleLighting(), minutesBefore));
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("PlagHaMincha", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[8], c.getPlagHamincha(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("MinchaKetana", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[9], c.getMinchaKetana(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("MinchaGedola", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[10], c.getMinchaGedolaGreaterThan30(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Chatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[11], c.getChatzos(), minutesBefore));//always add
        }

        if (jewishCalendar.getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanBiurChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[12], c.getSofZmanBiurChametzMGA(), minutesBefore));//only add if it's erev pesach
            }

            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[13], c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }

            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanAchilatChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[14], c.getSofZmanTfilaMGA72MinutesZmanis(), minutesBefore));//Achilat Chametz
            }
        } else {
            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[13], c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("SofZmanShmaGRA", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[15], c.getSofZmanShmaGRA(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("SofZmanShmaMGA", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[16], c.getSofZmanShmaMGA72MinutesZmanis(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("HaNetz", -1);
        if (minutesBefore >= 0) {
            Date sunrise = c.getHaNetz(c.getGeoLocation().getLocationName());
            if (sunrise != null) {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[17], sunrise, minutesBefore));//always add
            } else {
                pairArrayList.add(new ZmanInformationHolder(zmanNames[17], c.getSeaLevelSunrise(), minutesBefore));//always add
            }

        }

        minutesBefore = mSettingsSharedPreferences.getInt("TalitTefilin", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[18], c.getEarliestTalitTefilin(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Alot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanNames[19], c.getAlos72Zmanis(), minutesBefore));//always add
        }

        return pairArrayList;
    }

    private String getShabbatAndOrChag(JewishCalendar jewishCalendar) {
        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            if (jewishCalendar.isYomTovAssurBemelacha() &&
                    jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "\u05E9\u05D1\u05EA/\u05D7\u05D2";
            } else if (jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "\u05E9\u05D1\u05EA";
            } else {
                return "\u05D7\u05D2";
            }
        } else {
            if (jewishCalendar.isYomTovAssurBemelacha() &&
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
