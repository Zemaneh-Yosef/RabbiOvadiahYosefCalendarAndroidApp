package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.classes.ZmanimNames;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Implementation of App Widget functionality.
 */
public class ZmanimAppWidget extends AppWidgetProvider {

    private static SharedPreferences mSharedPreferences;
    private static SharedPreferences mSettingsPreferences;
    private static LocationResolver mLocationResolver;
    private static JewishDateInfo mJewishDateInfo;
    private static ROZmanimCalendar mROZmanimCalendar;
    private static boolean mIsZmanimInHebrew;
    private static boolean mIsZmanimEnglishTranslated;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        mLocationResolver = new LocationResolver(context, new Activity());
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        setZmanimLanguageBools();
        mROZmanimCalendar = getROZmanimCalendar(context);
        mROZmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
        mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(mSettingsPreferences.getString("CandleLightingOffset", "20")));
        mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(mSettingsPreferences.getString("EndOfShabbatOffset", "40")));
        mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
        SimpleDateFormat sZmanimFormat;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            sZmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            sZmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        sZmanimFormat.setTimeZone(mROZmanimCalendar.getCalendar().getTimeZone());

        String jewishDate = mJewishDateInfo.getJewishDate();
        String parsha = mJewishDateInfo.getThisWeeksParsha();
        ZmanListEntry nextUpcomingZman = getNextUpcomingZman();
        String zman = nextUpcomingZman.getTitle();
        String time = sZmanimFormat.format(nextUpcomingZman.getZman());
        String tachanun = mJewishDateInfo.getIsTachanunSaid()
                .replace("No Tachanun today", "No Tachanun")
                .replace("Tachanun only in the morning", "Tachanun morning only")
                .replace("There is Tachanun today", "Tachanun");
        HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
        hebrewDateFormatter.setUseGershGershayim(false);
        String dafYomi = mJewishDateInfo.getJewishCalendar().getDafYomiBavli().getMasechta()
                + " " + hebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getDafYomiBavli().getDaf());

        RemoteViews views;
        if (mSharedPreferences.getInt("widgetMaxHeight", 0) > mSharedPreferences.getInt("widgetMaxWidth", 0)) {
            views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_horizontal);
        } else {
            views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget);
        }
        views.setTextViewText(R.id.jewish_date, jewishDate);
        views.setTextViewText(R.id.parsha, parsha);
        views.setTextViewText(R.id.zman, zman);
        views.setTextViewText(R.id.zman_time, time);
        views.setTextViewText(R.id.tachanun, tachanun);
        views.setTextViewText(R.id.daf, dafYomi);

        if (!mSharedPreferences.getBoolean("widgetInitialized", false)) {
            views.setViewVisibility(R.id.widget_next_zman, View.GONE);// initially hide the other views
            views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
        }

        Intent configIntent = new Intent(context, MainActivity.class);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget, configPendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static ROZmanimCalendar getROZmanimCalendar(Context context) {
        if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            mLocationResolver.getRealtimeNotificationData();
            if (mLocationResolver.getLatitude() != 0 && mLocationResolver.getLongitude() != 0) {
                return new ROZmanimCalendar(new GeoLocation(
                        mLocationResolver.getLocationName(),
                        mLocationResolver.getLatitude(),
                        mLocationResolver.getLongitude(),
                        getLastKnownElevation(context),
                        mLocationResolver.getTimeZone()));
            }
        }
        return new ROZmanimCalendar(new GeoLocation(
                mSharedPreferences.getString("name", ""),
                Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)),
                Double.longBitsToDouble(mSharedPreferences.getLong("long", 0)),
                getLastKnownElevation(context),
                TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", ""))));
    }

    private static double getLastKnownElevation(Context context) {
        double elevation;
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            elevation = 0;
        } else if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mLocationResolver.getLocationName(), "0"));//get the elevation using the location name
        } else {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        }
        return elevation;
    }

    public static ZmanListEntry getNextUpcomingZman() {
        ZmanListEntry theZman = null;
        List<ZmanListEntry> zmanim = new ArrayList<>();
        Calendar today = Calendar.getInstance();

        today.add(Calendar.DATE, -1);
        mROZmanimCalendar.setCalendar(today);//MUST call setCalendar() because it sets the JewishCalendar to the correct date for netz
        mJewishDateInfo.setCalendar(today);
        addZmanim(zmanim);//for the previous day

        today.add(Calendar.DATE, 1);
        mROZmanimCalendar.setCalendar(today);
        mJewishDateInfo.setCalendar(today);
        addZmanim(zmanim);//for the current day

        today.add(Calendar.DATE, 1);
        mROZmanimCalendar.setCalendar(today);
        mJewishDateInfo.setCalendar(today);
        addZmanim(zmanim);//for the next day
        mROZmanimCalendar.setCalendar(new GregorianCalendar());//reset the calendar to the current date
        mJewishDateInfo.setCalendar(new GregorianCalendar());
        //find the next upcoming zman that is after the current time and before all the other zmanim
        for (ZmanListEntry zmanListEntry: zmanim) {
                if (zmanListEntry.getZman() != null && zmanListEntry.getZman().after(new Date()) && (theZman == null || zmanListEntry.getZman().before(theZman.getZman()))) {
                    theZman = zmanListEntry;
            }
        }
        return theZman;
    }

    private static void addZmanim(List<ZmanListEntry> zmanim) {
        if (mSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            addAmudeiHoraahZmanim(zmanim);
            return;
        }
        ZmanimNames zmanimNames = new ZmanimNames(mIsZmanimInHebrew, mIsZmanimEnglishTranslated);
        if (mJewishDateInfo.getJewishCalendar().isTaanis()
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.TISHA_BEAV
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getAlos72Zmanis(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getAlotString(), mROZmanimCalendar.getAlos72Zmanis(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTalitTefilinString(), mROZmanimCalendar.getEarliestTalitTefilin(), true));
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " " + zmanimNames.getElevatedString(), mROZmanimCalendar.getSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString(), mROZmanimCalendar.getHaNetz(), true));
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaMgaString(), mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaGraString(), mROZmanimCalendar.getSofZmanShmaGRA(), true));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            ZmanListEntry zman = new ZmanListEntry(zmanimNames.getAchilatChametzString(), mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
            zman = new ZmanListEntry(zmanimNames.getBiurChametzString(), mROZmanimCalendar.getSofZmanBiurChametzMGA(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotString(), mROZmanimCalendar.getChatzot(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaGedolaString(), mROZmanimCalendar.getMinchaGedolaGreaterThan30(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaKetanaString(), mROZmanimCalendar.getMinchaKetana(), true));
        String plagOpinions = mSettingsPreferences.getString("plagOpinion", "1");
        if (plagOpinions.equals("1")) {
            zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString(), mROZmanimCalendar.getPlagHaminchaYalkutYosef(), true));
        }
        if (plagOpinions.equals("2")) {
            zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString(), mROZmanimCalendar.getPlagHamincha(), true));
        }
        if (plagOpinions.equals("3")) {
            zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedHalachaBerurahString(),
                    mROZmanimCalendar.getPlagHamincha(), true));
            zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedYalkutYosefString(),
                    mROZmanimCalendar.getPlagHaminchaYalkutYosef(), true));
        }
        if ((mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            ZmanListEntry candleLightingZman = new ZmanListEntry(
                    zmanimNames.getCandleLightingString() + " (" + (int) mROZmanimCalendar.getCandleLightingOffset() + ")",
                    mROZmanimCalendar.getCandleLighting(),
                    true);
            candleLightingZman.setNoteworthyZman(true);
            zmanim.add(candleLightingZman);
        }
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false)) {
            if (mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {//only add if shabbat/yom tov ends tomorrow and not the day after
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            ZmanListEntry endShabbat;
                            if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1")) {
                                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString()
                                        + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaisAteretTorah(), true);
                            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
                            } else {
                                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), true);
                            }
                            endShabbat.setNoteworthyZman(true);
                            zmanim.add(endShabbat);
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString() + zmanimNames.getMacharString(), addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            } else {
                                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzais72Zmanis(), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            }
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TISHA_BEAV) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getSunset(), true));
        }
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        zmanim.add(new ZmanListEntry(zmanimNames.getSunsetString(), mROZmanimCalendar.getSunset(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString(), mROZmanimCalendar.getTzeit(), true));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {//When today is Shabbat
                    ZmanListEntry endShabbat;
                    if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1")) {
                        endShabbat = new ZmanListEntry(zmanimNames.getCandleLightingString(), mROZmanimCalendar.getTzaisAteretTorah(), true);
                    } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                        endShabbat = new ZmanListEntry(zmanimNames.getCandleLightingString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
                    } else {
                        endShabbat = new ZmanListEntry(zmanimNames.getCandleLightingString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), true);
                    }
                    endShabbat.setNoteworthyZman(true);
                    zmanim.add(endShabbat);
                } else {//When today is Yom Tov
                    zmanim.add(new ZmanListEntry(zmanimNames.getCandleLightingString(), mROZmanimCalendar.getTzeit(), true));
                }
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis() && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            ZmanListEntry fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitTaanit(), true);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
            fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString() + " " + zmanimNames.getLChumraString(), mROZmanimCalendar.getTzaitTaanitLChumra(), true);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
        } else if (mSettingsPreferences.getBoolean("alwaysShowTzeitLChumra", false)) {
            ZmanListEntry tzeitLChumra = new ZmanListEntry(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), mROZmanimCalendar.getTzaitTaanit(), true);
            zmanim.add(tzeitLChumra);
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            ZmanListEntry endShabbat;
            if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString()
                        + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), true);
            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
            } else {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), true);
            }
            endShabbat.setNoteworthyZman(true);
            zmanim.add(endShabbat);
            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            } else {
                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), mROZmanimCalendar.getTzais72Zmanis(), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            }
            //If it is shabbat/yom tov,we want to dim the tzeit hacochavim zmanim in the GUI
            for (ZmanListEntry zman: zmanim) {
                if (zman.getTitle().equals(zmanimNames.getTzaitHacochavimString()) ||
                        zman.getTitle().equals(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString())) {
                    zman.setShouldBeDimmed(true);
                }
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                    ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                } else {
                    ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), mROZmanimCalendar.getTzais72Zmanis(), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                }
            }
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotLaylaString(), mROZmanimCalendar.getSolarMidnight(), true));
    }

    private static void addAmudeiHoraahZmanim(List<ZmanListEntry> zmanim) {
        mROZmanimCalendar.setUseElevation(false);
        ZmanimNames zmanimNames = new ZmanimNames(mIsZmanimInHebrew, mIsZmanimEnglishTranslated);
        if (mJewishDateInfo.getJewishCalendar().isTaanis()
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.TISHA_BEAV
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getAlotAmudeiHoraah(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getAlotString(), mROZmanimCalendar.getAlotAmudeiHoraah(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTalitTefilinString(), mROZmanimCalendar.getEarliestTalitTefilinAmudeiHoraah(), true));
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " " + zmanimNames.getElevatedString(), mROZmanimCalendar.getSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString(), mROZmanimCalendar.getHaNetz(), true));
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaMgaString(), mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaGraString(), mROZmanimCalendar.getSofZmanShmaGRA(), true));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            ZmanListEntry zman = new ZmanListEntry(zmanimNames.getAchilatChametzString(), mROZmanimCalendar.getSofZmanAchilatChametzAmudeiHoraah(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
            zman = new ZmanListEntry(zmanimNames.getBiurChametzString(), mROZmanimCalendar.getSofZmanBiurChametzMGAAmudeiHoraah(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotString(), mROZmanimCalendar.getChatzot(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaGedolaString(), mROZmanimCalendar.getMinchaGedolaGreaterThan30(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaKetanaString(), mROZmanimCalendar.getMinchaKetana(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedHalachaBerurahString(), mROZmanimCalendar.getPlagHamincha(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedYalkutYosefString(), mROZmanimCalendar.getPlagHaminchaYalkutYosefAmudeiHoraah(), true));
        if ((mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            ZmanListEntry candleLightingZman = new ZmanListEntry(
                    zmanimNames.getCandleLightingString() + " (" + (int) mROZmanimCalendar.getCandleLightingOffset() + ")",
                    mROZmanimCalendar.getCandleLighting(),
                    true);
            candleLightingZman.setNoteworthyZman(true);
            zmanim.add(candleLightingZman);
        }
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false)) {
            if (mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            zmanim.add(new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true));
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString() + zmanimNames.getMacharString(), addMinuteToZman(mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah()), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            } else {
                                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah(), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            }
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TISHA_BEAV) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getSunset(), true));
        }
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        zmanim.add(new ZmanListEntry(zmanimNames.getSunsetString(), mROZmanimCalendar.getSeaLevelSunset(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString(), mROZmanimCalendar.getTzeitAmudeiHoraah(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), mROZmanimCalendar.getTzeitAmudeiHoraahLChumra(), true));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {//When today is Shabbat
                    zmanim.add(new ZmanListEntry(zmanimNames.getCandleLightingString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true));
                } else {//When today is Yom Tov
                    zmanim.add(new ZmanListEntry(zmanimNames.getCandleLightingString(), mROZmanimCalendar.getTzeitAmudeiHoraahLChumra(), true));
                }
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis() && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            ZmanListEntry fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzeitAmudeiHoraahLChumra(), true);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            ZmanListEntry endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
            endShabbat.setNoteworthyZman(true);
            zmanim.add(endShabbat);
            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah()), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            } else {
                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah(), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            }
            //If it is shabbat/yom tov,we want to dim the tzeit hacochavim zmanim in the GUI
            for (ZmanListEntry zman: zmanim) {
                if (zman.getTitle().equals(zmanimNames.getTzaitHacochavimString()) ||
                        zman.getTitle().equals(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString())) {
                    zman.setShouldBeDimmed(true);
                }
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                    ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah()), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                } else {
                    ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah(), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                }
            }
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotLaylaString(), mROZmanimCalendar.getSolarMidnight(), true));
    }

    private static Date addMinuteToZman(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime() + 60_000);
    }

    private static void setZmanimLanguageBools() {
        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            mIsZmanimInHebrew = true;
            mIsZmanimEnglishTranslated = false;
        } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
            mIsZmanimInHebrew = false;
            mIsZmanimEnglishTranslated = true;
        } else {
            mIsZmanimInHebrew = false;
            mIsZmanimEnglishTranslated = false;
        }
    }

    private static String getShabbatAndOrChag() {
        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            if (mJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha() &&
                    mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "שבת/חג";
            } else if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "שבת";
            } else {
                return "חג";
            }
        } else {
            if (mJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha() &&
                    mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat/Chag";
            } else if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat";
            } else {
                return "Chag";
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        scheduleUpdates(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        RemoteViews views;
        if (maxHeight > maxWidth) {
            views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_horizontal);
        } else {
            views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget);
        }

        if (minWidth >= 250 || maxHeight > maxWidth) {// if the widget is wide enough, show the zman
            views.setViewVisibility(R.id.widget_next_zman, View.VISIBLE);// initially hide the other views
            views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
        } else {
            views.setViewVisibility(R.id.widget_next_zman, View.GONE);// initially hide the other views
            views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
            mSharedPreferences.edit().putBoolean("widgetInitialized", true).apply();
        }
        if (minWidth >= 350 || maxHeight > maxWidth) {// if the widget is wide enough, show the daf as well
            views.setViewVisibility(R.id.widget_next_zman, View.VISIBLE);// initially hide the other views
            views.setViewVisibility(R.id.widget_tachanun_daf, View.VISIBLE);
        }

        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSharedPreferences.edit().putInt("widgetMaxWidth", maxWidth).apply();
        mSharedPreferences.edit().putInt("widgetMaxHeight", maxHeight).apply();

        appWidgetManager.updateAppWidget(appWidgetId, views);

        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSharedPreferences.edit().putBoolean("widgetInitialized", false).apply();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSharedPreferences.edit().putBoolean("widgetInitialized", false).apply();
        cancelUpdates(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        scheduleUpdates(context);
    }

    private int[] getActiveWidgetIds(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, ZmanimAppWidget.class);

        // return ID of all active widgets within this AppWidgetProvider
        return appWidgetManager.getAppWidgetIds(componentName);
    }

    private void scheduleUpdates(Context context) {
        int[] activeWidgetIds = getActiveWidgetIds(context);

        if (activeWidgetIds.length > 0) {
            PendingIntent pendingIntent = getUpdatePendingIntent(context);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        getNextUpcomingZman().getZman().getTime() + 100,
                        pendingIntent
                );
            }
        }
    }

    private PendingIntent getUpdatePendingIntent(Context context) {
        Class<?> widgetClass = ZmanimAppWidget.class;
        int[] widgetIds = getActiveWidgetIds(context);
        Intent updateIntent = new Intent(context, widgetClass)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        int requestCode = widgetClass.getName().hashCode();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        return PendingIntent.getBroadcast(context, requestCode, updateIntent, flags);
    }

    private void cancelUpdates(Context context) {
        PendingIntent pendingIntent = getUpdatePendingIntent(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

}
