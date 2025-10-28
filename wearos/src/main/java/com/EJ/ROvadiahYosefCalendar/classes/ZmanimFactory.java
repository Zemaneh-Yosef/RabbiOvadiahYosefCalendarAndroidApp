package com.EJ.ROvadiahYosefCalendar.classes;

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
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), useAHZmanim ? mROZmanimCalendar.getAlotAmudeiHoraah() : mROZmanimCalendar.getAlos72Zmanis(), SecondTreatment.ROUND_EARLIER));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getAlotString(), useAHZmanim ? mROZmanimCalendar.getAlotAmudeiHoraah() : mROZmanimCalendar.getAlos72Zmanis(), SecondTreatment.ROUND_EARLIER));
        if (isForWeeklyZmanim) {
            ZmanListEntry misheyakir66 = new ZmanListEntry(zmanimNames.getTalitTefilinString() + " (66)", useAHZmanim ? mROZmanimCalendar.getMisheyakir66AmudeiHoraah() : mROZmanimCalendar.getMisheyakir66ZmaniyotMinutes(), SecondTreatment.ROUND_LATER);
            misheyakir66.setIs66MisheyakirZman(true);
            zmanim.add(misheyakir66);
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getTalitTefilinString(), useAHZmanim ? mROZmanimCalendar.getMisheyakir60AmudeiHoraah() : mROZmanimCalendar.getMisheyakir60ZmaniyotMinutes(), SecondTreatment.ROUND_LATER));
        if (add66MisheyakirZman && !isForWeeklyZmanim) {
            ZmanListEntry misheyakir66 = new ZmanListEntry(zmanimNames.getTalitTefilinString() + " (66)", useAHZmanim ? mROZmanimCalendar.getMisheyakir66AmudeiHoraah() : mROZmanimCalendar.getMisheyakir66ZmaniyotMinutes(), SecondTreatment.ROUND_LATER);
            misheyakir66.setIs66MisheyakirZman(true);
            zmanim.add(misheyakir66);
        }
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " " + zmanimNames.getElevatedString(), mROZmanimCalendar.getSunrise(), SecondTreatment.ROUND_LATER));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString(), mROZmanimCalendar.getHaNetz(), SecondTreatment.ALWAYS_DISPLAY));
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), SecondTreatment.ROUND_LATER));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), SecondTreatment.ROUND_LATER));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaMgaString(), useAHZmanim ? mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah() : mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis(), SecondTreatment.ROUND_EARLIER));
        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            ZmanListEntry birchatHachama = new ZmanListEntry(zmanimNames.getBirkatHachamaString(), mROZmanimCalendar.getSofZmanShmaGRA(), SecondTreatment.ROUND_EARLIER);
            birchatHachama.setBirchatHachamahZman(true);
            birchatHachama.setNoteworthyZman(true);
            zmanim.add(birchatHachama);
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaGraString(), mROZmanimCalendar.getSofZmanShmaGRA(), SecondTreatment.ROUND_EARLIER));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            ZmanListEntry zman = new ZmanListEntry(zmanimNames.getAchilatChametzString(), useAHZmanim ? mROZmanimCalendar.getSofZmanAchilatChametzAmudeiHoraah() : mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis(), SecondTreatment.ROUND_EARLIER);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), SecondTreatment.ROUND_EARLIER));
            zman = new ZmanListEntry(zmanimNames.getBiurChametzString(), useAHZmanim ? mROZmanimCalendar.getSofZmanBiurChametzMGAAmudeiHoraah() : mROZmanimCalendar.getSofZmanBiurChametzMGA(), SecondTreatment.ROUND_EARLIER);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), SecondTreatment.ROUND_EARLIER));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotString(), mROZmanimCalendar.getChatzot(), SecondTreatment.ROUND_EARLIER));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaGedolaString(), mROZmanimCalendar.getMinchaGedolaGreaterThan30(), SecondTreatment.ROUND_LATER));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaKetanaString(), mROZmanimCalendar.getMinchaKetana(), SecondTreatment.ROUND_LATER));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " (" + zmanimNames.getHalachaBerurahString() + ")", mROZmanimCalendar.getPlagHamincha(), SecondTreatment.ROUND_LATER));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " (" + zmanimNames.getYalkutYosefString() + ")", useAHZmanim ? mROZmanimCalendar.getPlagHaminchaYalkutYosefAmudeiHoraah() : mROZmanimCalendar.getPlagHaminchaYalkutYosef(), SecondTreatment.ROUND_LATER));
        if ((mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            ZmanListEntry candleLightingZman = new ZmanListEntry(
                    zmanimNames.getCandleLightingString() + " (" + (int) mROZmanimCalendar.getCandleLightingOffset() + ")",
                    mROZmanimCalendar.getCandleLighting(),
                    SecondTreatment.ROUND_EARLIER);
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
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getElevationAdjustedSunset(), SecondTreatment.ROUND_EARLIER));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getSunsetString(), mROZmanimCalendar.getSunset(), SecondTreatment.ROUND_EARLIER));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString(), useAHZmanim ? mROZmanimCalendar.getTzeitAmudeiHoraah() : mROZmanimCalendar.getTzeit(), SecondTreatment.ROUND_LATER));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), useAHZmanim ? mROZmanimCalendar.getTzeitAmudeiHoraahLChumra() : mROZmanimCalendar.getTzaitTaanit(), SecondTreatment.ROUND_LATER));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {// we already added Candles
                if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {// Shabbat going into Yom Tov
                    addShabbatEndsZman(zmanim, mSettingsPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, useAHZmanim, zmanimNames, true, false);
                } else {// Yom Tov going into Yom Tov
                    zmanim.add(new ZmanListEntry(zmanimNames.getCandleLightingString(), useAHZmanim ? mROZmanimCalendar.getTzeitAmudeiHoraahLChumra() : mROZmanimCalendar.getTzaitTaanit(), SecondTreatment.ROUND_LATER));
                }
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis() && !mJewishDateInfo.getJewishCalendar().isYomKippur()) {
            ZmanListEntry fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString(), useAHZmanim ? mROZmanimCalendar.getTzeitAmudeiHoraahLChumra() : mROZmanimCalendar.getTzaitTaanit(), SecondTreatment.ROUND_LATER);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            addShabbatEndsZman(zmanim, mSettingsPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, useAHZmanim, zmanimNames, false, false);
            addRTZman(zmanim, mSettingsPreferences, mROZmanimCalendar, zmanimNames, useAHZmanim, false);
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
                addRTZman(zmanim, mSettingsPreferences, mROZmanimCalendar, zmanimNames, useAHZmanim, false);
            }
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotLaylaString(), mROZmanimCalendar.getSolarMidnight(), SecondTreatment.ROUND_LATER));
    }

    private static void addRTZman(List<ZmanListEntry> zmanim, SharedPreferences mSettingsPreferences, ROZmanimCalendar mROZmanimCalendar, ZmanimNames zmanimNames, boolean useAHZmanim, boolean isForTomorrow) {
        ZmanListEntry rt = new ZmanListEntry(
                zmanimNames.getRTString() + (isForTomorrow ? zmanimNames.getMacharString() : ""),
                useAHZmanim && !mSettingsPreferences.getBoolean("overrideRTZman", false) ? mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah() : mROZmanimCalendar.getTzais72Zmanis(),
                SecondTreatment.ROUND_LATER
        );

        rt.setNoteworthyZman(true);
        zmanim.add(rt);
    }

    private static void addShabbatEndsZman(List<ZmanListEntry> zmanim, SharedPreferences mSettingsPreferences, ROZmanimCalendar mROZmanimCalendar, JewishDateInfo mJewishDateInfo, boolean mIsZmanimInHebrew, boolean useAHZmanim, ZmanimNames zmanimNames, boolean isForCandleLigthting, boolean isForTomorrow) {
        ZmanListEntry endShabbat;
        if (useAHZmanim) {
            endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + " (7.165°)", mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), SecondTreatment.ROUND_LATER);
        } else {
            endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString()
                    + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), SecondTreatment.ROUND_LATER);
        }
        if (mSettingsPreferences.getBoolean("overrideAHEndShabbatTime", false)) {
            if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString()
                        + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), SecondTreatment.ROUND_LATER);
            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + " (7.165°)", mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), SecondTreatment.ROUND_LATER);
            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("3")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), SecondTreatment.ROUND_LATER);
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
     * This is a simple convenience method to check if the current date is on shabbat or yom tov or both and return the correct string.
     * @return a string that says whether it is shabbat and chag or just shabbat or just chag (in Hebrew or English)
     */
    private static String getShabbatAndOrChag(boolean mIsZmanimInHebrew, JewishDateInfo mJewishDateInfo) {
        if (mJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha()
                && mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY) {
            return mIsZmanimInHebrew ? "שבת/חג" : "Shabbat/Chag";
        } else if (mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY) {
            return mIsZmanimInHebrew ? "שבת" : "Shabbat";
        } else {
            return mIsZmanimInHebrew ? "חג" : "Chag";
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
