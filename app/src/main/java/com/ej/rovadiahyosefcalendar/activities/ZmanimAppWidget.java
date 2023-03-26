package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
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
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
        mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
        SimpleDateFormat sZmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        sZmanimFormat.setTimeZone(mROZmanimCalendar.getCalendar().getTimeZone());

        String jewishDate = mJewishDateInfo.getJewishDate().replace(",", "");
        String parsha = mJewishDateInfo.getThisWeeksParsha();
        ZmanListEntry nextUpcomingZman = getNextUpcomingZman();
        String zman = nextUpcomingZman.getTitle();
        String time = sZmanimFormat.format(nextUpcomingZman.getZman());
        String tachanun = mJewishDateInfo.getIsTachanunSaid();
        String dafYomi = mJewishDateInfo.getJewishCalendar().getDafYomiBavli().getMasechta()
                + " " + JewishDateInfo.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getDafYomiBavli().getDaf());

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
            views.setViewVisibility(R.id.zman, View.INVISIBLE);// initially hide the other views
            views.setViewVisibility(R.id.zman_time, View.INVISIBLE);
            views.setViewVisibility(R.id.tachanun, View.INVISIBLE);
            views.setViewVisibility(R.id.daf, View.INVISIBLE);
            mSharedPreferences.edit().putBoolean("widgetInitialized", true).apply();
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
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
        addZmanim(zmanim);//for the previous day
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        addZmanim(zmanim);//for the current day
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        addZmanim(zmanim);//for the next day
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
        zmanim.add(new ZmanListEntry(getAlotString(), mROZmanimCalendar.getAlos72Zmanis(), true));
        zmanim.add(new ZmanListEntry(getTalitTefilinString(), mROZmanimCalendar.getEarliestTalitTefilin(), true));
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(new ZmanListEntry(getHaNetzString() + " " + getElevatedString(), mROZmanimCalendar.getSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true)) {
            zmanim.add(new ZmanListEntry(getHaNetzString(), mROZmanimCalendar.getHaNetz(), true));
        } else {
            zmanim.add(new ZmanListEntry(getHaNetzString() + " (" + getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(getHaNetzString() + " (" + getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        zmanim.add(new ZmanListEntry(getShmaMgaString(), mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis(), true));
        zmanim.add(new ZmanListEntry(getShmaGraString(), mROZmanimCalendar.getSofZmanShmaGRA(), true));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            ZmanListEntry zman = new ZmanListEntry(getAchilatChametzString(), mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
            zmanim.add(new ZmanListEntry(getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
            zman = new ZmanListEntry(getBiurChametzString(), mROZmanimCalendar.getSofZmanBiurChametzMGA(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
        } else {
            zmanim.add(new ZmanListEntry(getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
        }
        zmanim.add(new ZmanListEntry(getChatzotString(), mROZmanimCalendar.getChatzot(), true));
        zmanim.add(new ZmanListEntry(getMinchaGedolaString(), mROZmanimCalendar.getMinchaGedolaGreaterThan30(), true));
        zmanim.add(new ZmanListEntry(getMinchaKetanaString(), mROZmanimCalendar.getMinchaKetana(), true));
        zmanim.add(new ZmanListEntry(getPlagHaminchaString(), mROZmanimCalendar.getPlagHamincha(), true));
        if ((mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            ZmanListEntry candleLightingZman = new ZmanListEntry(
                    getCandleLightingString() + " (" + (int) mROZmanimCalendar.getCandleLightingOffset() + ")",
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
                            zmanim.add(new ZmanListEntry(getTzaitString() + getShabbatAndOrChag() + getEndsString() + getMacharString()
                                    + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), true));
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                                ZmanListEntry rt = new ZmanListEntry(getRTString() + getMacharString(), addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            } else {
                                ZmanListEntry rt = new ZmanListEntry(getRTString() + getMacharString(), mROZmanimCalendar.getTzais72Zmanis(), true);
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
        zmanim.add(new ZmanListEntry(getSunsetString(), mROZmanimCalendar.getSunset(), true));
        zmanim.add(new ZmanListEntry(getTzaitHacochavimString(), mROZmanimCalendar.getTzeit(), true));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add(new ZmanListEntry(getCandleLightingString(), mROZmanimCalendar.getTzeit(), true));
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis() && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            ZmanListEntry fastEnds = new ZmanListEntry(getTzaitString() + getTaanitString() + getEndsString(), mROZmanimCalendar.getTzaitTaanit(), true);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
            fastEnds = new ZmanListEntry(getTzaitString() + getTaanitString() + getEndsString() + " " + getLChumraString(), mROZmanimCalendar.getTzaitTaanitLChumra(), true);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            ZmanListEntry endShabbat = new ZmanListEntry(getTzaitString() + getShabbatAndOrChag() + getEndsString()
                    + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), true);
            endShabbat.setNoteworthyZman(true);
            zmanim.add(endShabbat);
            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                ZmanListEntry rt = new ZmanListEntry(getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            } else {
                ZmanListEntry rt = new ZmanListEntry(getRTString(), mROZmanimCalendar.getTzais72Zmanis(), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                    ZmanListEntry rt = new ZmanListEntry(getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                } else {
                    ZmanListEntry rt = new ZmanListEntry(getRTString(), mROZmanimCalendar.getTzais72Zmanis(), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                }
            }
        }
        zmanim.add(new ZmanListEntry(getChatzotLaylaString(), mROZmanimCalendar.getSolarMidnight(), true));
    }


    private static void addAmudeiHoraahZmanim(List<ZmanListEntry> zmanim) {
        mROZmanimCalendar.setUseElevation(false);
        zmanim.add(new ZmanListEntry(getAlotString(), mROZmanimCalendar.getAlotAmudeiHoraah(), true));
        zmanim.add(new ZmanListEntry(getTalitTefilinString(), mROZmanimCalendar.getEarliestTalitTefilinAmudeiHoraah(), true));
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(new ZmanListEntry(getHaNetzString() + " " + getElevatedString(), mROZmanimCalendar.getSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true)) {
            zmanim.add(new ZmanListEntry(getHaNetzString(), mROZmanimCalendar.getHaNetz(), true));
        } else {
            zmanim.add(new ZmanListEntry(getHaNetzString(), mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(getHaNetzString(), mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        zmanim.add(new ZmanListEntry(getShmaMgaString(), mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah(), true));
        zmanim.add(new ZmanListEntry(getShmaGraString(), mROZmanimCalendar.getSofZmanShmaGRA(), true));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            ZmanListEntry zman = new ZmanListEntry(getAchilatChametzString(), mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
            zmanim.add(new ZmanListEntry(getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
            zman = new ZmanListEntry(getBiurChametzString(), mROZmanimCalendar.getSofZmanBiurChametzMGA(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
        } else {
            zmanim.add(new ZmanListEntry(getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
        }
        zmanim.add(new ZmanListEntry(getChatzotString(), mROZmanimCalendar.getChatzot(), true));
        zmanim.add(new ZmanListEntry(getMinchaGedolaString(), mROZmanimCalendar.getMinchaGedolaGreaterThan30(), true));
        zmanim.add(new ZmanListEntry(getMinchaKetanaString(), mROZmanimCalendar.getMinchaKetana(), true));
        zmanim.add(new ZmanListEntry(getPlagHaminchaString(), mROZmanimCalendar.getPlagHamincha(), true));
        zmanim.add(new ZmanListEntry(getPlagHaminchaString(), mROZmanimCalendar.getPlagHaminchaHalachaBerurah(), true));
        if ((mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            ZmanListEntry candleLightingZman = new ZmanListEntry(
                    getCandleLightingString() + " (" + (int) mROZmanimCalendar.getCandleLightingOffset() + ")",
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
                            zmanim.add(new ZmanListEntry(getTzaitString() + getShabbatAndOrChag() + getEndsString() + getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true));
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                                ZmanListEntry rt = new ZmanListEntry(getRTString() + getMacharString(), addMinuteToZman(mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah()), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            } else {
                                ZmanListEntry rt = new ZmanListEntry(getRTString() + getMacharString(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah(), true);
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
        zmanim.add(new ZmanListEntry(getSunsetString(), mROZmanimCalendar.getSeaLevelSunset(), true));
        zmanim.add(new ZmanListEntry(getTzaitHacochavimString(), mROZmanimCalendar.getTzeitAmudeiHoraah(), true));
        zmanim.add(new ZmanListEntry(getTzaitHacochavimString() + " " + getLChumraString(), mROZmanimCalendar.getTzeitAmudeiHoraahLChumra(), true));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add(new ZmanListEntry(getCandleLightingString(), mROZmanimCalendar.getTzeitAmudeiHoraah(), true));
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis() && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            ZmanListEntry fastEnds = new ZmanListEntry(getTzaitString() + getTaanitString() + getEndsString(), mROZmanimCalendar.getTzeitAmudeiHoraahLChumra(), true);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            ZmanListEntry endShabbat = new ZmanListEntry(getTzaitString() + getShabbatAndOrChag() + getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
            endShabbat.setNoteworthyZman(true);
            zmanim.add(endShabbat);
            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                ZmanListEntry rt = new ZmanListEntry(getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah()), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            } else {
                ZmanListEntry rt = new ZmanListEntry(getRTString(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah(), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                    ZmanListEntry rt = new ZmanListEntry(getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah()), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                } else {
                    ZmanListEntry rt = new ZmanListEntry(getRTString(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah(), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                }
            }
        }
        zmanim.add(new ZmanListEntry(getChatzotLaylaString(), mROZmanimCalendar.getSolarMidnight(), true));
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
                return "\u05E9\u05D1\u05EA/\u05D7\u05D2";
            } else if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "\u05E9\u05D1\u05EA";
            } else {
                return "\u05D7\u05D2";
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

        if (maxWidth >= 250 || maxHeight > maxWidth) {// if the widget is wide enough, show the zman
            views.setViewVisibility(R.id.zman, View.VISIBLE);
            views.setViewVisibility(R.id.zman_time, View.VISIBLE);
            views.setViewVisibility(R.id.tachanun, View.INVISIBLE);
            views.setViewVisibility(R.id.daf, View.INVISIBLE);
        } else {
            views.setViewVisibility(R.id.zman, View.INVISIBLE);
            views.setViewVisibility(R.id.zman_time, View.INVISIBLE);
            views.setViewVisibility(R.id.tachanun, View.INVISIBLE);
            views.setViewVisibility(R.id.daf, View.INVISIBLE);
        }
        if (maxWidth >= 350 || maxHeight > maxWidth) {// if the widget is wide enough, show the daf as well
            views.setViewVisibility(R.id.tachanun, View.VISIBLE);
            views.setViewVisibility(R.id.daf, View.VISIBLE);
        }

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
    }

    private static String getChatzotLaylaString() {
        if (mIsZmanimInHebrew) {
            return "חצות לילה";
        } else if (mIsZmanimEnglishTranslated) {
            return "Midnight";
        } else {
            return "Chatzot Layla";
        }
    }

    private static String getLChumraString() {
        if (mIsZmanimInHebrew) {
            return "לחומרה";
        } else if (mIsZmanimEnglishTranslated) {
            return "(Stringent)";
        } else {
            return "L'Chumra";
        }
    }

    private static String getTaanitString() {
        if (mIsZmanimInHebrew) {
            return "תענית";
        } else if (mIsZmanimEnglishTranslated) {
            return "Fast";
        } else {
            return "Taanit";
        }
    }

    private static String getTzaitHacochavimString() {
        if (mIsZmanimInHebrew) {
            return "צאת הכוכבים";
        } else if (mIsZmanimEnglishTranslated) {
            return "Nightfall";
        } else {
            return "Tzait Hacochavim";
        }
    }

    private static String getSunsetString() {
        if (mIsZmanimInHebrew) {
            return "שקיעה";
        } else if (mIsZmanimEnglishTranslated) {
            return "Sunset";
        } else {
            return "Shkia";
        }
    }

    private static String getRTString() {
        if (mIsZmanimInHebrew) {
            return "רבינו תם";
        } else {
            return "Rabbeinu Tam";
        }
    }

    private static String getMacharString() {
        if (mIsZmanimInHebrew) {
            return " (מחר) ";
        } else {
            return " (Tom) ";
        }
    }

    private static String getEndsString() {
        if (mIsZmanimEnglishTranslated) {
            return " Ends";
        } else {
            return "";
        }
    }

    private static String getTzaitString() {
        if (mIsZmanimInHebrew) {
            return "צאת ";
        } else if (!mIsZmanimEnglishTranslated) {
            return "Tzait ";
        } else {
            return "";//if we are translating to English, we don't want to show the word Tzait first, just {Zman} Ends
        }
    }

    private static String getCandleLightingString() {
        if (mIsZmanimInHebrew) {
            return "הדלקת נרות";
        } else {
            return "Candle Lighting";
        }
    }

    private static String getPlagHaminchaString() {
        if (mIsZmanimInHebrew) {
            return "פלג המנחה";
        } else {
            return "Plag HaMincha";
        }
    }

    private static String getMinchaKetanaString() {
        if (mIsZmanimInHebrew) {
            return "מנחה קטנה";
        } else {
            return "Mincha Ketana";
        }
    }

    private static String getMinchaGedolaString() {
        if (mIsZmanimInHebrew) {
            return "מנחה גדולה";
        } else if (mIsZmanimEnglishTranslated) {
            return "Earliest Mincha";
        } else {
            return "Mincha Gedola";
        }
    }

    private static String getChatzotString() {
        if (mIsZmanimInHebrew) {
            return "חצות";
        } else if (mIsZmanimEnglishTranslated) {
            return "Mid-day";
        } else {
            return "Chatzot";
        }
    }

    private static String getBiurChametzString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן ביעור חמץ";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest time to burn Chametz";
        } else {
            return "Sof Zman Biur Chametz";
        }
    }

    private static String getBrachotShmaString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן ברכות שמע";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest Brachot Shma";
        } else {
            return "Sof Zman Brachot Shma";
        }
    }

    private static String getAchilatChametzString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן אכילת חמץ";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest time to eat Chametz";
        } else {
            return "Sof Zman Achilat Chametz";
        }
    }

    private static String getShmaGraString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן שמע גר\"א";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest Shma GR\"A";
        } else {
            return "Sof Zman Shma GR\"A";
        }
    }

    private static String getShmaMgaString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן שמע מג\"א";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest Shma MG\"A";
        } else {
            return "Sof Zman Shma MG\"A";
        }
    }

    private static String getMishorString() {
        if (mIsZmanimInHebrew) {
            return "מישור";
        } else if (mIsZmanimEnglishTranslated) {
            return "Sea Level";
        } else {
            return "Mishor";
        }
    }

    private static String getElevatedString() {
        if (mIsZmanimInHebrew) {
            return "(גבוה)";
        } else {
            return "(Elevated)";
        }
    }

    private static String getHaNetzString() {
        if (mIsZmanimInHebrew) {
            return "הנץ";
        } else if (mIsZmanimEnglishTranslated) {
            return "Sunrise";
        } else {
            return "HaNetz";
        }
    }

    private static String getTalitTefilinString() {
        if (mIsZmanimInHebrew) {
            return "טלית ותפילין";
        } else {
            return "Earliest Talit/Tefilin";
        }
    }

    private static String getAlotString() {
        if (mIsZmanimInHebrew) {
            return "עלות השחר";
        } else if (mIsZmanimEnglishTranslated) {
            return "Dawn";
        } else {
            return "Alot Hashachar";
        }
    }
}