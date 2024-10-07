package com.ej.rovadiahyosefcalendar.notifications;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanInformationHolder;
import com.ej.rovadiahyosefcalendar.classes.ZmanimNames;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Consumer;

public class ZmanimNotifications extends BroadcastReceiver implements Consumer<Location> {

    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsSharedPreferences;
    private LocationResolver mLocationResolver;
    private Context context;
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mLocationResolver = new LocationResolver(context, new Activity());
        if (mSharedPreferences.getBoolean("isSetup",false) && mSettingsSharedPreferences.getBoolean("zmanim_notifications", true)) {
            Runnable mAlarmUpdater = () -> {
                ROZmanimCalendar zmanimCalendar = getROZmanimCalendar();
                if (ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {// if we can get the user's location, we will go to the accept method
                    JewishCalendar jewishCalendar = new JewishCalendar();
                    zmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
                    String candles = mSettingsSharedPreferences.getString("CandleLightingOffset", "20");
                    if (candles.isEmpty()) {
                        candles = "20";
                    }
                    zmanimCalendar.setCandleLightingOffset(Double.parseDouble(candles));
                    String shabbat = mSettingsSharedPreferences.getString("EndOfShabbatOffset", mSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40");
                    if (shabbat.isEmpty()) {// for some reason this is happening
                        shabbat = "40";
                    }
                    zmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(shabbat));
                    if (mSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
                        zmanimCalendar.setAteretTorahSunsetOffset(30);
                    }
                    mSharedPreferences.edit().putString("locationNameFN", zmanimCalendar.getGeoLocation().getLocationName()).apply();
                    setAlarms(zmanimCalendar, jewishCalendar);
                }
            };
            mAlarmUpdater.run();
        }
    }

    /**
     * This method will instantiate a ROZmanimCalendar object if the user has allowed the app to use their location, otherwise, it will use the last known location.
     */
    @NonNull
    private ROZmanimCalendar getROZmanimCalendar() {
        if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            mLocationResolver.getRealtimeNotificationData(this);// we will continue in the accept method
        }
        return new ROZmanimCalendar(new GeoLocation(
                mSharedPreferences.getString("name", ""),
                Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)),
                Double.longBitsToDouble(mSharedPreferences.getLong("long", 0)),
                getLastKnownElevation(Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)), Double.longBitsToDouble(mSharedPreferences.getLong("long", 0))),
                TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", TimeZone.getDefault().getID()))));
    }

    private double getLastKnownElevation(double latitude, double longitude) {
        double elevation;
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            elevation = 0;
        } else if (ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mLocationResolver.getLocationName(latitude, longitude), "0"));//get the elevation using the location name
        } else {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        }
        return elevation;
    }

    /**
     * This method will set all the alarms/notifications for the upcoming zmanim. This method is
     * called everytime the app starts and everyday by the {@link DailyNotifications} class. If
     * there are zmanim already set to be notified, it removes them all and reschedules them.
     */
    private void setAlarms(ROZmanimCalendar c, JewishCalendar jewishCalendar) {
        Calendar calendar = c.getCalendar();

        calendar.add(Calendar.DATE, -1);
        c.setCalendar(calendar);
        jewishCalendar.setDate(calendar);//set the calendars to the previous day

        ArrayList<ZmanInformationHolder> zmanimOver3Days = new ArrayList<>(getArrayOfZmanim(c, jewishCalendar));

        calendar.add(Calendar.DATE, 1);
        c.setCalendar(calendar);
        jewishCalendar.setDate(calendar);//set the calendars to the current day
        zmanimOver3Days.addAll(getArrayOfZmanim(c, jewishCalendar));

        calendar.add(Calendar.DATE, 1);
        c.setCalendar(calendar);
        jewishCalendar.setDate(calendar);//set the calendars to the next day
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
//                            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 60000, zmanPendingIntent);
//                            am.setAlarmClock(alarmClockInfo, zmanPendingIntent);
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
        if (mSettingsSharedPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            return getArrayOfZmanimAmudeiHoraah(c, jewishCalendar);
        }
        ArrayList<ZmanInformationHolder> pairArrayList = new ArrayList<>();
        ZmanimNames zmanimNames = new ZmanimNames(
                mSharedPreferences.getBoolean("isZmanimInHebrew", false),
                mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false));

        int minutesBefore = mSettingsSharedPreferences.getInt("NightChatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getChatzotLaylaString(), c.getSolarMidnight(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("RT", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getRTString(), c.getTzais72Zmanis(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("ShabbatEnd", -1);
        if (minutesBefore >= 0) {
            if (jewishCalendar.isAssurBemelacha() && !jewishCalendar.hasCandleLighting()) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitString() + getShabbatAndOrChag(jewishCalendar),
                        c.getTzaisAteretTorah(), minutesBefore));//only add if it's shabbat or yom tov
            }
        }

        if (jewishCalendar.isTaanis() && jewishCalendar.getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {//only add if it's a taanit and not yom kippur
            minutesBefore = mSettingsSharedPreferences.getInt("FastEnd", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitString() + zmanimNames.getTaanitString(), c.getTzaitTaanit(), minutesBefore));
            }
        }

        if (mSettingsSharedPreferences.getBoolean("alwaysShowTzeitLChumra", false)) {
            minutesBefore = mSettingsSharedPreferences.getInt("TzeitHacochavimLChumra", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), c.getTzaitTaanit(), minutesBefore));
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("TzeitHacochavim", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitHacochavimString(), c.getTzeit(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Shkia", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getSunsetString(), c.getSunset(), minutesBefore));//always add
        }

        if ((jewishCalendar.hasCandleLighting() &&
                !jewishCalendar.isAssurBemelacha()) ||
                jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {//only add if it's a day before shabbat/yom tov and not a 2 day yom tov or shabbat
            minutesBefore = mSettingsSharedPreferences.getInt("CandleLighting", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getCandleLightingString(), c.getCandleLighting(), minutesBefore));
            }
        }

        String plagOpinions = mSettingsSharedPreferences.getString("plagOpinion", "1");
        if (plagOpinions.equals("1")) {
            minutesBefore = mSettingsSharedPreferences.getInt("PlagHaMinchaYY", -1);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString(), c.getPlagHaminchaYalkutYosef(), minutesBefore));//always add
            }
        }
        if (plagOpinions.equals("2")) {
            minutesBefore = mSettingsSharedPreferences.getInt("PlagHaMinchaHB", -1);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedHalachaBerurahString(),
                        c.getPlagHamincha(), minutesBefore));//always add
            }
        }
        if (plagOpinions.equals("3")) {
            minutesBefore = mSettingsSharedPreferences.getInt("PlagHaMinchaYY", -1);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString(), c.getPlagHaminchaYalkutYosef(), minutesBefore));//always add
            }
            minutesBefore = mSettingsSharedPreferences.getInt("PlagHaMinchaHB", -1);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedHalachaBerurahString(),
                        c.getPlagHaminchaYalkutYosef(), minutesBefore));//always add
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("MinchaKetana", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getMinchaKetanaString(), c.getMinchaKetana(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("MinchaGedola", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getMinchaGedolaString(), c.getMinchaGedolaGreaterThan30(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Chatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getChatzotString(), c.getChatzot(), minutesBefore));//always add
        }

        if (jewishCalendar.getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanBiurChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBiurChametzString(), c.getSofZmanBiurChametzMGA(), minutesBefore));//only add if it's erev pesach
            }

            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBrachotShmaString(), c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }

            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanAchilatChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getAchilatChametzString(), c.getSofZmanTfilaMGA72MinutesZmanis(), minutesBefore));//Achilat Chametz
            }
        } else {
            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBrachotShmaString(), c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("SofZmanShmaGRA", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getShmaGraString(), c.getSofZmanShmaGRA(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("SofZmanShmaMGA", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getShmaMgaString(), c.getSofZmanShmaMGA72MinutesZmanis(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("HaNetz", -1);
        if (minutesBefore >= 0) {
            Date sunrise = c.getHaNetz(c.getGeoLocation().getLocationName());
            if (sunrise != null) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getHaNetzString(), sunrise, minutesBefore));//always add
            } else {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getHaNetzString() + " " + zmanimNames.getMishorString(),
                        c.getSeaLevelSunrise(), minutesBefore));//always add
            }

        }

        minutesBefore = mSettingsSharedPreferences.getInt("TalitTefilin", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTalitTefilinString(), c.getEarliestTalitTefilin(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Alot", -1);
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

        int minutesBefore = mSettingsSharedPreferences.getInt("NightChatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getChatzotLaylaString(), c.getSolarMidnight(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("RT", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getRTString(), c.getTzais72ZmanisAmudeiHoraahLkulah(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("ShabbatEnd", -1);
        if (minutesBefore >= 0) {
            if (jewishCalendar.isAssurBemelacha() && !jewishCalendar.hasCandleLighting()) {//only add if it's shabbat or yom tov
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitString() + getShabbatAndOrChag(jewishCalendar), c.getTzaitShabbatAmudeiHoraah(), minutesBefore));
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("TzeitHacochavimLChumra", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), c.getTzeitAmudeiHoraahLChumra(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("TzeitHacochavim", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTzaitHacochavimString(), c.getTzeitAmudeiHoraah(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Shkia", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getSunsetString(), c.getSeaLevelSunset(), minutesBefore));//always add
        }

        if ((jewishCalendar.hasCandleLighting() &&
                !jewishCalendar.isAssurBemelacha()) ||
                jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {//only add if it's a day before shabbat/yom tov and not a 2 day yom tov or shabbat
            minutesBefore = mSettingsSharedPreferences.getInt("CandleLighting", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getCandleLightingString(), c.getCandleLighting(), minutesBefore));
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("PlagHaMinchaYY", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString()
                    + " " + zmanimNames.getAbbreviatedYalkutYosefString(), c.getPlagHaminchaYalkutYosefAmudeiHoraah(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("PlagHaMinchaHB", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getPlagHaminchaString()
                    + " " + zmanimNames.getAbbreviatedHalachaBerurahString(), c.getPlagHamincha(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("MinchaKetana", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getMinchaKetanaString(), c.getMinchaKetana(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("MinchaGedola", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getMinchaGedolaString(), c.getMinchaGedolaGreaterThan30(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Chatzot", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getChatzotString(), c.getChatzos(), minutesBefore));//always add
        }

        if (jewishCalendar.getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanBiurChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBiurChametzString(), c.getSofZmanBiurChametzMGAAmudeiHoraah(), minutesBefore));//only add if it's erev pesach
            }

            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBrachotShmaString(), c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }

            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanAchilatChametz", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getAchilatChametzString(), c.getSofZmanAchilatChametzAmudeiHoraah(), minutesBefore));//Achilat Chametz
            }
        } else {
            minutesBefore = mSettingsSharedPreferences.getInt("SofZmanTefila", 15);
            if (minutesBefore >= 0) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getBrachotShmaString(), c.getSofZmanTfilaGRA(), minutesBefore));//always add
            }
        }

        minutesBefore = mSettingsSharedPreferences.getInt("SofZmanShmaGRA", -1);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getShmaGraString(), c.getSofZmanShmaGRA(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("SofZmanShmaMGA", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getShmaMgaString(), c.getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("HaNetz", -1);
        if (minutesBefore >= 0) {
            Date sunrise = c.getHaNetz(c.getGeoLocation().getLocationName());
            if (sunrise != null) {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getHaNetzString(), sunrise, minutesBefore));//always add
            } else {
                pairArrayList.add(new ZmanInformationHolder(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")",
                        c.getSeaLevelSunrise(), minutesBefore));//always add
            }

        }

        minutesBefore = mSettingsSharedPreferences.getInt("TalitTefilin", 15);
        if (minutesBefore >= 0) {
            pairArrayList.add(new ZmanInformationHolder(zmanimNames.getTalitTefilinString(), c.getEarliestTalitTefilinAmudeiHoraah(), minutesBefore));//always add
        }

        minutesBefore = mSettingsSharedPreferences.getInt("Alot", -1);
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

    @Override
    public void accept(Location location) {
        if (location != null) {
            ROZmanimCalendar zmanimCalendar = new ROZmanimCalendar(new GeoLocation(
                    mLocationResolver.getLocationName(location.getLatitude(), location.getLongitude()),
                    location.getLatitude(),
                    location.getLongitude(),
                    getLastKnownElevation(location.getLatitude(), location.getLongitude()),
                    mLocationResolver.getTimeZone()));
            zmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
            String candles = mSettingsSharedPreferences.getString("CandleLightingOffset", "20");
            if (candles.isEmpty()) {
                candles = "20";
            }
            zmanimCalendar.setCandleLightingOffset(Double.parseDouble(candles));
            String shabbat = mSettingsSharedPreferences.getString("EndOfShabbatOffset", mSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40");
            if (shabbat.isEmpty()) {// for some reason this is happening
                shabbat = "40";
            }
            zmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(shabbat));
            if (mSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
                zmanimCalendar.setAteretTorahSunsetOffset(30);
            }
            mSharedPreferences.edit().putString("locationNameFN", zmanimCalendar.getGeoLocation().getLocationName()).apply();
            setAlarms(zmanimCalendar, new JewishCalendar());
        }
    }
}
