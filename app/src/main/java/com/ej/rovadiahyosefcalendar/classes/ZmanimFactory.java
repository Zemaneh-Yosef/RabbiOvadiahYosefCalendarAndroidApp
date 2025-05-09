package com.ej.rovadiahyosefcalendar.classes;

import android.content.SharedPreferences;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class ZmanimFactory {

    public static void addZmanim(List<ZmanListEntry> zmanim,
                                  boolean isForWeeklyZmanim,
                                  SharedPreferences mSettingsPreferences,
                                  SharedPreferences mSharedPreferences,
                                  ROZmanimCalendar mROZmanimCalendar,
                                  JewishDateInfo mJewishDateInfo,
                                  boolean mIsZmanimInHebrew,
                                 boolean mIsZmanimEnglishTranslated,
                                 boolean add66MisheyakirZman) {
        boolean useAHZmanim = mSettingsPreferences.getBoolean("LuachAmudeiHoraah", false);
        ZmanimNames zmanimNames = new ZmanimNames(mIsZmanimInHebrew, mIsZmanimEnglishTranslated);
        if (mJewishDateInfo.getJewishCalendar().isTaanis()
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.TISHA_BEAV
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), useAHZmanim ? mROZmanimCalendar.getAlotAmudeiHoraah() : mROZmanimCalendar.getAlos72Zmanis(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getAlotString(), useAHZmanim ? mROZmanimCalendar.getAlotAmudeiHoraah() : mROZmanimCalendar.getAlos72Zmanis(), true));
        if (isForWeeklyZmanim) {
            ZmanListEntry misheyakir66 = new ZmanListEntry(zmanimNames.getTalitTefilinString() + " (66)", useAHZmanim ? mROZmanimCalendar.getMisheyakir66AmudeiHoraah() : mROZmanimCalendar.getMisheyakir66ZmaniyotMinutes(), true);
            misheyakir66.setIs66MisheyakirZman(true);
            zmanim.add(misheyakir66);
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getTalitTefilinString(), useAHZmanim ? mROZmanimCalendar.getMisheyakir60AmudeiHoraah() : mROZmanimCalendar.getMisheyakir60ZmaniyotMinutes(), true));
        if (add66MisheyakirZman && !isForWeeklyZmanim) {
            ZmanListEntry misheyakir66 = new ZmanListEntry(zmanimNames.getTalitTefilinString() + " (66)", useAHZmanim ? mROZmanimCalendar.getMisheyakir66AmudeiHoraah() : mROZmanimCalendar.getMisheyakir66ZmaniyotMinutes(), true);
            misheyakir66.setIs66MisheyakirZman(true);
            zmanim.add(misheyakir66);
        }
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " " + zmanimNames.getElevatedString(), mROZmanimCalendar.getSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString(), mROZmanimCalendar.getHaNetz(), true, true));
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaMgaString(), useAHZmanim ? mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah() : mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis(), true));
        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            ZmanListEntry birchatHachama = new ZmanListEntry(zmanimNames.getBirkatHachamaString(), mROZmanimCalendar.getSofZmanShmaGRA(), true);
            birchatHachama.setBirchatHachamahZman(true);
            birchatHachama.setNoteworthyZman(true);
            zmanim.add(birchatHachama);
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaGraString(), mROZmanimCalendar.getSofZmanShmaGRA(), true));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            ZmanListEntry zman = new ZmanListEntry(zmanimNames.getAchilatChametzString(), useAHZmanim ? mROZmanimCalendar.getSofZmanAchilatChametzAmudeiHoraah() : mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
            zman = new ZmanListEntry(zmanimNames.getBiurChametzString(), useAHZmanim ? mROZmanimCalendar.getSofZmanBiurChametzMGAAmudeiHoraah() : mROZmanimCalendar.getSofZmanBiurChametzMGA(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotString(), mROZmanimCalendar.getChatzot(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaGedolaString(), mROZmanimCalendar.getMinchaGedolaGreaterThan30(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaKetanaString(), mROZmanimCalendar.getMinchaKetana(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " (" + zmanimNames.getHalachaBerurahString() + ")", mROZmanimCalendar.getPlagHamincha(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " (" + zmanimNames.getYalkutYosefString() + ")", useAHZmanim ? mROZmanimCalendar.getPlagHaminchaYalkutYosefAmudeiHoraah() : mROZmanimCalendar.getPlagHaminchaYalkutYosef(), true));
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
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false) && !isForWeeklyZmanim) {
            if (mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {//only add if shabbat/yom tov ends tomorrow
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            addShabbatEndsZman(zmanim, mSettingsPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, useAHZmanim, zmanimNames, false, true);
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            addRTZman(zmanim, mSettingsPreferences, mROZmanimCalendar, zmanimNames, useAHZmanim ,true);
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        if (mJewishDateInfo.tomorrow().getJewishCalendar().isTishaBav()) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getElevationAdjustedSunset(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getSunsetString(), mROZmanimCalendar.getSunset(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString(), useAHZmanim ? mROZmanimCalendar.getTzeitAmudeiHoraah() : mROZmanimCalendar.getTzeit(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), useAHZmanim ? mROZmanimCalendar.getTzeitAmudeiHoraahLChumra() : mROZmanimCalendar.getTzaitTaanit(), true));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {// we already added Candles
                if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {// Shabbat going into Yom Tov
                    addShabbatEndsZman(zmanim, mSettingsPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, useAHZmanim, zmanimNames, true, false);
                } else {// Yom Tov going into Yom Tov
                    zmanim.add(new ZmanListEntry(zmanimNames.getCandleLightingString(), useAHZmanim ? mROZmanimCalendar.getTzeitAmudeiHoraahLChumra() : mROZmanimCalendar.getTzaitTaanit(), true));
                }
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis() && !mJewishDateInfo.getJewishCalendar().isYomKippur()) {
            ZmanListEntry fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString(), useAHZmanim ? mROZmanimCalendar.getTzeitAmudeiHoraahLChumra() : mROZmanimCalendar.getTzaitTaanit(), true);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            addShabbatEndsZman(zmanim, mSettingsPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, useAHZmanim, zmanimNames, false, false);
            addRTZman(zmanim, mSettingsPreferences, mROZmanimCalendar, zmanimNames, useAHZmanim, false);
            //If shabbat/yom tov ends today, we want to dim the tzeit hacochavim zmanim in the GUI
            for (ZmanListEntry zman: zmanim) {
                if (zman.getTitle().equals(zmanimNames.getTzaitHacochavimString()) ||
                        zman.getTitle().equals(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString())) {
                    zman.setShouldBeDimmed(true);
                }
            }
        } else if (mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY) {// always add RT for shabbat
            addRTZman(zmanim, mSettingsPreferences, mROZmanimCalendar, zmanimNames, useAHZmanim, false);
        } else if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                addRTZman(zmanim, mSettingsPreferences, mROZmanimCalendar, zmanimNames, useAHZmanim, false);
            }
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotLaylaString(), mROZmanimCalendar.getSolarMidnight(), true));
    }

    private static void addRTZman(List<ZmanListEntry> zmanim, SharedPreferences mSettingsPreferences, ROZmanimCalendar mROZmanimCalendar, ZmanimNames zmanimNames, boolean useAHZmanim, boolean isForTomorrow) {
        ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), mROZmanimCalendar.getTzais72Zmanis(), true);
        if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
            rt.setZman(addMinuteToZman(useAHZmanim ? mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah() : mROZmanimCalendar.getTzais72Zmanis()));
        } else {
            rt.setZman(useAHZmanim ? mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah() : mROZmanimCalendar.getTzais72Zmanis());
        }
        if (mSettingsPreferences.getBoolean("overrideRTZman", false)) {
            rt.setZman(mSettingsPreferences.getBoolean("RoundUpRT", true) ? addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()) : mROZmanimCalendar.getTzais72Zmanis());
        }
        if (isForTomorrow) {
            rt.setTitle(rt.getTitle() + zmanimNames.getMacharString());
        }
        rt.setRTZman(true);
        rt.setNoteworthyZman(true);
        zmanim.add(rt);
    }

    private static void addShabbatEndsZman(List<ZmanListEntry> zmanim, SharedPreferences mSettingsPreferences, ROZmanimCalendar mROZmanimCalendar, JewishDateInfo mJewishDateInfo, boolean mIsZmanimInHebrew, boolean useAHZmanim, ZmanimNames zmanimNames, boolean isForCandleLigthting, boolean isForTomorrow) {
        ZmanListEntry endShabbat;
        if (useAHZmanim) {
            endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + " (7.14°)", mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
        } else {
            endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString()
                    + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), true);
        }
        if (mSettingsPreferences.getBoolean("overrideAHEndShabbatTime", false)) {
            if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString()
                        + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), true);
            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + " (7.14°)", mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("3")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), true);
            }
        }
        if (isForTomorrow) {
            endShabbat.setTitle(endShabbat.getTitle() + zmanimNames.getMacharString());
        }
        if (isForCandleLigthting) {
            endShabbat.setTitle(zmanimNames.getCandleLightingString());
        }
        endShabbat.setNoteworthyZman(true);
        zmanim.add(endShabbat);
    }

    /**
     * This is a simple convenience method to add a minute to a date object. If the date is not null,
     * it will return the same date with a minute added to it. Otherwise, if the date is null, it will return null.
     * @param date the date object to add a minute to
     * @return the given date a minute ahead if not null
     */
    private static Date addMinuteToZman(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime() + 60_000);
    }

    /**
     * This is a simple convenience method to check if the current date is on shabbat or yom tov or both and return the correct string.
     * @return a string that says whether it is shabbat and chag or just shabbat or just chag (in Hebrew or English)
     */
    private static String getShabbatAndOrChag(boolean mIsZmanimInHebrew, JewishDateInfo mJewishDateInfo) {
        if (mIsZmanimInHebrew) {
            if (mJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha()
                    && mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "שבת/חג";
            } else if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "שבת";
            } else {
                return "חג";
            }
        } else {
            if (mJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha()
                    && mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat/Chag";
            } else if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat";
            } else {
                return "Chag";
            }
        }
    }

    public static ZmanListEntry getNextUpcomingZman(Calendar mCurrentDateShown, ROZmanimCalendar mROZmanimCalendar, JewishDateInfo mJewishDateInfo, SharedPreferences mSettingsPreferences, SharedPreferences mSharedPreferences, boolean mIsZmanimInHebrew, boolean mIsZmanimEnglishTranslated) {
        ZmanListEntry theZman = null;
        List<ZmanListEntry> zmanim = new ArrayList<>();
        Calendar today = Calendar.getInstance();

        today.add(Calendar.DATE, -1);
        mROZmanimCalendar.setCalendar(today);
        mJewishDateInfo.setCalendar(today);
        // we only care about the date
        addZmanim(zmanim, false, mSettingsPreferences, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated, true);//for the previous day

        today.add(Calendar.DATE, 1);
        mROZmanimCalendar.setCalendar(today);
        mJewishDateInfo.setCalendar(today);
        addZmanim(zmanim, false, mSettingsPreferences, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated, true);//for the current day

        today.add(Calendar.DATE, 1);
        mROZmanimCalendar.setCalendar(today);
        mJewishDateInfo.setCalendar(today);
        addZmanim(zmanim, false, mSettingsPreferences, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated, true);//for the next day

        mROZmanimCalendar.setCalendar(mCurrentDateShown);
        mJewishDateInfo.setCalendar(mCurrentDateShown);//reset
        //find the next upcoming zman that is after the current time and before all the other zmanim
        for (ZmanListEntry zmanListEntry: zmanim) {
            if (zmanListEntry.getZman() != null && zmanListEntry.getZman().after(new Date()) && (theZman == null || zmanListEntry.getZman().before(theZman.getZman()))) {
                theZman = zmanListEntry;
            }
        }
        return theZman;
    }
}
