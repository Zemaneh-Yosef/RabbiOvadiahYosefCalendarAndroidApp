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
                                  boolean mIsZmanimInHebrew, boolean mIsZmanimEnglishTranslated) {
        if (mSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            addAmudeiHoraahZmanim(zmanim, isForWeeklyZmanim, mSettingsPreferences, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated);
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
        if (mSettingsPreferences.getBoolean("showPreferredMisheyakirZman", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTalitTefilinString() + " " + zmanimNames.getBetterString(), mROZmanimCalendar.getEarliestProperTalitTefilin(), true));
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
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaMgaString(), mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis(), true));
        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            ZmanListEntry birchatHachama = new ZmanListEntry(zmanimNames.getBirkatHachamaString(), mROZmanimCalendar.getSofZmanShmaGRA(), true);
            birchatHachama.setBirchatHachamahZman(true);
            birchatHachama.setNoteworthyZman(true);
            zmanim.add(birchatHachama);
        }
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
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false) && !isForWeeklyZmanim) {
            if (mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {//only add if shabbat/yom tov ends tomorrow and not the day after
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            ZmanListEntry endShabbat;
                            if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1") || mSharedPreferences.getBoolean("inIsrael", false)) {
                                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString()
                                        + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaisAteretTorah(), true);
                            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
                            } else {
                                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), true);
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
                    if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1") || mSharedPreferences.getBoolean("inIsrael", false)) {
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
            if (!isForWeeklyZmanim) {//remove the second fast time, it just confuses people because we remove l'chumra
                fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString() + " " + zmanimNames.getLChumraString(), mROZmanimCalendar.getTzaitTaanitLChumra(), true);
                fastEnds.setNoteworthyZman(true);
                zmanim.add(fastEnds);
            }
        } else if (mSettingsPreferences.getBoolean("alwaysShowTzeitLChumra", false)) {
            ZmanListEntry tzeitLChumra = new ZmanListEntry(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), mROZmanimCalendar.getTzaitTaanit(), true);
            zmanim.add(tzeitLChumra);
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            ZmanListEntry endShabbat;
            if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1") || mSharedPreferences.getBoolean("inIsrael", false)) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString()
                        + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), true);
            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + " (7.14°)", mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
            } else {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), true);
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

    private static void addAmudeiHoraahZmanim(List<ZmanListEntry> zmanim,
                                              boolean isForWeeklyZmanim,
                                              SharedPreferences mSettingsPreferences,
                                              SharedPreferences mSharedPreferences,
                                              ROZmanimCalendar mROZmanimCalendar,
                                              JewishDateInfo mJewishDateInfo,
                                              boolean mIsZmanimInHebrew, boolean mIsZmanimEnglishTranslated) {
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
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString(), mROZmanimCalendar.getHaNetz(), true, true));
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaMgaString(), mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah(), true));
        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            ZmanListEntry birchatHachama = new ZmanListEntry(zmanimNames.getBirkatHachamaString(), mROZmanimCalendar.getSofZmanShmaGRA(), true);
            birchatHachama.setBirchatHachamahZman(true);
            birchatHachama.setNoteworthyZman(true);
            zmanim.add(birchatHachama);
        }
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
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false) && !isForWeeklyZmanim) {
            if (mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            zmanim.add(new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true));
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
            ZmanListEntry endShabbat;
            if (!mSettingsPreferences.getBoolean("overrideAHEndShabbatTime", false)) {// default zman
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + " (7.14°)", mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
            } else {// if user wants to override
                if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1")) {
                    endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString()
                            + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), true);
                } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                    endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString() + " (7.14°)", mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
                } else {
                    endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(mIsZmanimInHebrew, mJewishDateInfo) + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), true);
                }
            }
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
            //If it is shabbat/yom tov, we want to dim the tzeit hacochavim zmanim in the GUI
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
        addZmanim(zmanim, false, mSettingsPreferences, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated);//for the previous day

        today.add(Calendar.DATE, 1);
        mROZmanimCalendar.setCalendar(today);
        mJewishDateInfo.setCalendar(today);
        addZmanim(zmanim, false, mSettingsPreferences, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated);//for the current day

        today.add(Calendar.DATE, 1);
        mROZmanimCalendar.setCalendar(today);
        mJewishDateInfo.setCalendar(today);
        addZmanim(zmanim, false, mSettingsPreferences, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated);//for the next day

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
