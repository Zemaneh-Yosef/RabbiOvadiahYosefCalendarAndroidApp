package com.EJ.ROvadiahYosefCalendar.classes;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.SHARED_PREF;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

public class ZmanimNotifications extends BroadcastReceiver {

    private SharedPreferences mSharedPreferences;
    @Override
    public void onReceive(Context context, Intent intent) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        if (mSharedPreferences.getBoolean("zmanim_notifications", true)) {
            Runnable mAlarmUpdater = () -> {
                JewishCalendar jewishCalendar = new JewishCalendar();
                ROZmanimCalendar zmanimCalendar = getROZmanimCalendar();
                String candles = mSharedPreferences.getString("CandleLightingOffset", "20");
                if (candles.isEmpty()) {
                    candles = "20";
                }
                zmanimCalendar.setCandleLightingOffset(Double.parseDouble(candles));
                String shabbat = mSharedPreferences.getString("EndOfShabbatOffset", mSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40");
                if (shabbat.isEmpty()) {// for some reason this is happening
                    shabbat = "40";
                }
                zmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(shabbat));
                if (mSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
                    zmanimCalendar.setAteretTorahSunsetOffset(30);
                }
                mSharedPreferences.edit().putString("locationNameFN", zmanimCalendar.getGeoLocation().getLocationName()).apply();
                setAlarms(context, zmanimCalendar, jewishCalendar);
            };
            mAlarmUpdater.run();
        }
    }

    /**
     * This method will instantiate a ROZmanimCalendar object if the user has allowed the app to use their location, otherwise, it will use the last known location.
     */
    @NonNull
    private ROZmanimCalendar getROZmanimCalendar() {
        return new ROZmanimCalendar(new GeoLocation(
                mSharedPreferences.getString("currentLN", ""),
                Double.parseDouble(mSharedPreferences.getString("currentLat", "0")),
                Double.parseDouble(mSharedPreferences.getString("currentLong", "0")),
                getLastKnownElevation(),
                TimeZone.getTimeZone(mSharedPreferences.getString("currentTimezone", ""))), mSharedPreferences);
    }

    private double getLastKnownElevation() {
        double elevation;
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            elevation = 0;
        } else {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("currentLN", ""), "0"));//lastKnownLocation
        }
        return elevation;
    }

    /**
     * This method will set all the alarms/notifications for the upcoming zmanim.
     */
    private void setAlarms(Context context, ROZmanimCalendar c, JewishCalendar jewishCalendar) {
        c.getCalendar().add(Calendar.DATE, -1);
        jewishCalendar.setDate(c.getCalendar());//set the calendars to the previous day

        ArrayList<ZmanInformationHolder> zmanimOver3Days = new ArrayList<>(getArrayOfZmanim(c, jewishCalendar));

        c.getCalendar().add(Calendar.DATE, 1);
        jewishCalendar.setDate(c.getCalendar());//set the calendars to the current day
        zmanimOver3Days.addAll(getArrayOfZmanim(c, jewishCalendar));

        c.getCalendar().add(Calendar.DATE, 1);
        jewishCalendar.setDate(c.getCalendar());//set the calendars to the next day
        zmanimOver3Days.addAll(getArrayOfZmanim(c, jewishCalendar));

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        for (int i = 0; i < 40; i++) {//we could save the size of the array and get rid of every unique id with that size, but I don't think it will go past 40
            PendingIntent zmanPendingIntent = PendingIntent.getBroadcast(
                    context.getApplicationContext(),
                    i,
                    new Intent(context, ZmanNotification.class).setAction(String.valueOf(i)),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            am.cancel(zmanPendingIntent);//cancel the last zmanim notifications set
        }

        int max = 5;
        int set = 0;//only set 5 zmanim an hour
        for (int i = 0; i < zmanimOver3Days.size(); i++) {
            if (set < max) {
                if ((zmanimOver3Days.get(i).getZmanDate().getTime() - (60_000L * zmanimOver3Days.get(i).getNotificationDelay()) > new Date().getTime())) {
                    PendingIntent zmanPendingIntent = PendingIntent.getBroadcast(
                            context.getApplicationContext(),
                            set,
                            new Intent(context, ZmanNotification.class)
                                    .setAction(String.valueOf(set))
                                    .putExtra("zman",
                                            zmanimOver3Days.get(i).getZmanName() + ":" + zmanimOver3Days.get(i).getZmanDate().getTime()),//save the zman name and time for the notification e.g. "Chatzot Layla:1331313311"
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (am.canScheduleExactAlarms()) {
                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zmanimOver3Days.get(i).getZmanDate().getTime() - (60_000L * zmanimOver3Days.get(i).getNotificationDelay()), zmanPendingIntent);
                        }
                    } else {// on lower android version, app will not crash by setting exact alarms
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zmanimOver3Days.get(i).getZmanDate().getTime() - (60_000L * zmanimOver3Days.get(i).getNotificationDelay()), zmanPendingIntent);
                    }
                    set += 1;
                }
            }
        }
        PendingIntent schedulePendingIntent = PendingIntent.getBroadcast(
                context.getApplicationContext(),
                0,
                new Intent(context, ZmanimNotifications.class),
                PendingIntent.FLAG_IMMUTABLE);

        am.cancel(schedulePendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, new Date().getTime() + 1_800_000, schedulePendingIntent);//every half hour
            }
        } else {// on lower android version, app will not crash by setting exact alarms
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, new Date().getTime() + 1_800_000, schedulePendingIntent);//every half hour
        }
    }

    private ArrayList<ZmanInformationHolder> getArrayOfZmanim(ROZmanimCalendar c, JewishCalendar jewishCalendar) {
        if (mSharedPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            return getArrayOfZmanimAmudeiHoraah(c, jewishCalendar);
        }
        ArrayList<ZmanInformationHolder> pairArrayList = new ArrayList<>();
        ZmanimNames zmanimNames = new ZmanimNames(
                mSharedPreferences.getBoolean("isZmanimInHebrew", false),
                mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false));

        int minutesBefore = mSharedPreferences.getInt("NightChatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getChatzotLaylaString(), c.getSolarMidnight(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("RT", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getRTString(), c.getTzais72Zmanis(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("ShabbatEnd", -1);
        if (minutesBefore >= 0) {
            if (jewishCalendar.isAssurBemelacha() && !jewishCalendar.hasCandleLighting()) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitString() + getShabbatAndOrChag(jewishCalendar),
                        c.getTzaisAteretTorah(), minutesBefore));//only add if it's shabbat or yom tov
            }
        }

        if (jewishCalendar.isTaanis() && jewishCalendar.getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {//only add if it's a taanit and not yom kippur
            minutesBefore = mSharedPreferences.getInt("FastEnd", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitString() + zmanimNames.getTaanitString(), c.getTzaitTaanit(), minutesBefore));
            }
        }

        if (mSharedPreferences.getBoolean("alwaysShowTzeitLChumra", false)) {
            minutesBefore = mSharedPreferences.getInt("TzeitHacochavimLChumra", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), c.getTzaitTaanit(), minutesBefore));
            }
        }

        minutesBefore = mSharedPreferences.getInt("TzeitHacochavim", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitHacochavimString(), c.getTzeit(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("Shkia", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getSunsetString(), c.getSunset(), minutesBefore));//always add
        }

        if ((jewishCalendar.hasCandleLighting() &&
                !jewishCalendar.isAssurBemelacha()) ||
                jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {//only add if it's a day before shabbat/yom tov and not a 2 day yom tov or shabbat
            minutesBefore = mSharedPreferences.getInt("CandleLighting", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getCandleLightingString(), c.getCandleLighting(), minutesBefore));
            }
        }

        String plagOpinions = mSharedPreferences.getString("plagOpinion", "1");
        if (plagOpinions.equals("1")) {
            minutesBefore = mSharedPreferences.getInt("PlagHaMinchaYY", -1);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString(), c.getPlagHaminchaYalkutYosef(), minutesBefore));//always add
            }
        }
        if (plagOpinions.equals("2")) {
            minutesBefore = mSharedPreferences.getInt("PlagHaMinchaHB", -1);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedHalachaBerurahString(),
                        c.getPlagHamincha(), minutesBefore));//always add
            }
        }
        if (plagOpinions.equals("3")) {
            minutesBefore = mSharedPreferences.getInt("PlagHaMinchaYY", -1);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString(), c.getPlagHaminchaYalkutYosef(), minutesBefore));//always add
            }
            minutesBefore = mSharedPreferences.getInt("PlagHaMinchaHB", -1);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedHalachaBerurahString(),
                        c.getPlagHaminchaYalkutYosef(), minutesBefore));//always add
            }
        }

        minutesBefore = mSharedPreferences.getInt("MinchaKetana", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getMinchaKetanaString(), c.getMinchaKetana(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("MinchaGedola", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getMinchaGedolaString(), c.getMinchaGedolaGreaterThan30(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("Chatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getChatzotString(), c.getChatzot(), minutesBefore));//always add
        }

        if (jewishCalendar.getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            minutesBefore = mSharedPreferences.getInt("SofZmanBiurChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBiurChametzString(), c.getSofZmanBiurChametzMGA(), minutesBefore));//only add if it's erev pesach
            }

            minutesBefore = mSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBrachotShmaString(), c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }

            minutesBefore = mSharedPreferences.getInt("SofZmanAchilatChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getAchilatChametzString(), c.getSofZmanTfilaMGA72MinutesZmanis(), minutesBefore));//Achilat Chametz
            }
        } else {
            minutesBefore = mSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBrachotShmaString(), c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }
        }

        minutesBefore = mSharedPreferences.getInt("SofZmanShmaGRA", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getShmaGraString(), c.getSofZmanShmaGRA(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("SofZmanShmaMGA", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getShmaMgaString(), c.getSofZmanShmaMGA72MinutesZmanis(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("HaNetz", -1);
        if (minutesBefore >= 0) {
            Date sunrise = c.getHaNetz();
            if (sunrise != null) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getHaNetzString(), sunrise, minutesBefore));//always add
            } else {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getHaNetzString() + " " + zmanimNames.getMishorString(),
                        c.getSeaLevelSunrise(), minutesBefore));//always add
            }

        }

        minutesBefore = mSharedPreferences.getInt("TalitTefilin", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTalitTefilinString(), c.getEarliestTalitTefilin(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("Alot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getAlotString(), c.getAlos72Zmanis(), minutesBefore));//always add
        }

        Collections.reverse(pairArrayList);
        return pairArrayList;
    }

    private ArrayList<ZmanInformationHolder> getArrayOfZmanimAmudeiHoraah(ROZmanimCalendar c, JewishCalendar jewishCalendar) {
        c.setUseElevation(false);
        ArrayList<ZmanInformationHolder> pairArrayList = new ArrayList<>();
        ZmanimNames zmanimNames = new ZmanimNames(
                mSharedPreferences.getBoolean("isZmanimInHebrew", false),
                mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false));

        int minutesBefore = mSharedPreferences.getInt("NightChatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getChatzotLaylaString(), c.getSolarMidnight(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("RT", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getRTString(), c.getTzais72ZmanisAmudeiHoraahLkulah(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("ShabbatEnd", -1);
        if (minutesBefore >= 0) {
            if (jewishCalendar.isAssurBemelacha() && !jewishCalendar.hasCandleLighting()) {//only add if it's shabbat or yom tov
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitString() + getShabbatAndOrChag(jewishCalendar), c.getTzaitShabbatAmudeiHoraah(), minutesBefore));
            }
        }

        minutesBefore = mSharedPreferences.getInt("TzeitHacochavimLChumra", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), c.getTzeitAmudeiHoraahLChumra(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("TzeitHacochavim", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitHacochavimString(), c.getTzeitAmudeiHoraah(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("Shkia", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getSunsetString(), c.getSeaLevelSunset(), minutesBefore));//always add
        }

        if ((jewishCalendar.hasCandleLighting() &&
                !jewishCalendar.isAssurBemelacha()) ||
                jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {//only add if it's a day before shabbat/yom tov and not a 2 day yom tov or shabbat
            minutesBefore = mSharedPreferences.getInt("CandleLighting", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getCandleLightingString(), c.getCandleLighting(), minutesBefore));
            }
        }

        minutesBefore = mSharedPreferences.getInt("PlagHaMinchaYY", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString()
                    + " " + zmanimNames.getAbbreviatedYalkutYosefString(), c.getPlagHaminchaYalkutYosefAmudeiHoraah(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("PlagHaMinchaHB", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString()
                    + " " + zmanimNames.getAbbreviatedHalachaBerurahString(), c.getPlagHamincha(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("MinchaKetana", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getMinchaKetanaString(), c.getMinchaKetana(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("MinchaGedola", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getMinchaGedolaString(), c.getMinchaGedolaGreaterThan30(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("Chatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getChatzotString(), c.getChatzos(), minutesBefore));//always add
        }

        if (jewishCalendar.getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            minutesBefore = mSharedPreferences.getInt("SofZmanBiurChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBiurChametzString(), c.getSofZmanBiurChametzMGAAmudeiHoraah(), minutesBefore));//only add if it's erev pesach
            }

            minutesBefore = mSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBrachotShmaString(), c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }

            minutesBefore = mSharedPreferences.getInt("SofZmanAchilatChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getAchilatChametzString(), c.getSofZmanAchilatChametzAmudeiHoraah(), minutesBefore));//Achilat Chametz
            }
        } else {
            minutesBefore = mSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBrachotShmaString(), c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }
        }

        minutesBefore = mSharedPreferences.getInt("SofZmanShmaGRA", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getShmaGraString(), c.getSofZmanShmaGRA(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("SofZmanShmaMGA", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getShmaMgaString(), c.getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("HaNetz", -1);
        if (minutesBefore >= 0) {
            Date sunrise = c.getHaNetz();
            if (sunrise != null) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getHaNetzString(), sunrise, minutesBefore));//always add
            } else {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")",
                        c.getSeaLevelSunrise(), minutesBefore));//always add
            }

        }

        minutesBefore = mSharedPreferences.getInt("TalitTefilin", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTalitTefilinString(), c.getEarliestTalitTefilinAmudeiHoraah(), minutesBefore));//always add
        }

        minutesBefore = mSharedPreferences.getInt("Alot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getAlotString(), c.getAlotAmudeiHoraah(), minutesBefore));//always add
        }

        Collections.reverse(pairArrayList);
        return pairArrayList;
    }

    private String getShabbatAndOrChag(JewishCalendar jewishCalendar) {
        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            if (jewishCalendar.isYomTovAssurBemelacha() &&
                    jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "שבת/חג";
            } else if (jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "שבת";
            } else {
                return "חג";
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
