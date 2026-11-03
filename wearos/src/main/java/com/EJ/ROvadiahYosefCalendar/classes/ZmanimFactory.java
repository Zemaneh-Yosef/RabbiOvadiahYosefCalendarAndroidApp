package com.EJ.ROvadiahYosefCalendar.classes;

import static com.EJ.ROvadiahYosefCalendar.classes.ZmanListEntryType.*;
import static com.EJ.ROvadiahYosefCalendar.classes.SecondTreatment.ALWAYS_DISPLAY;
import static com.EJ.ROvadiahYosefCalendar.classes.SecondTreatment.ROUND_EARLIER;
import static com.EJ.ROvadiahYosefCalendar.classes.SecondTreatment.ROUND_LATER;

import android.content.SharedPreferences;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class ZmanimFactory {

    public static void addZmanim(List<ZmanListEntry> zmanim,
                                 boolean isForWeeklyZmanim,
                                 SharedPreferences mSharedPreferences,
                                 ROZmanimCalendar roZmanimCalendar,
                                 JewishDateInfo mJewishDateInfo,
                                 boolean add66MisheyakirZman) {
        ROZmanimCalendar mROZmanimCalendar = roZmanimCalendar.getCopy();
        mROZmanimCalendar.setAmudehHoraah(mSharedPreferences.getBoolean("LuachAmudeiHoraah", false));

        ZmanimNames zmanimNames = new ZmanimNames(
                mSharedPreferences.getBoolean("isZmanimInHebrew", false),
                mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false),
                mSharedPreferences.getBoolean("isZmanimAmericanized", false));
        if (mJewishDateInfo.getJewishCalendar().isTaanis()
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.TISHA_BEAV
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(),
                    mROZmanimCalendar.getAlotHashachar(),
                    ROUND_EARLIER,
                    FAST_STARTS_NON_TISHA_BAV_ZMAN,
                    ""));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getAlotString(),
                mROZmanimCalendar.getAlotHashachar(),
                ROUND_EARLIER,
                ALOT_HASHACHAR_ZMAN,
                "Alot"));
        if (isForWeeklyZmanim) {
            ZmanListEntry misheyakir66 = new ZmanListEntry(zmanimNames.getTalitTefilinString() + " (66)",
                    mROZmanimCalendar.getMisheyakir66ZmaniyotMinutes(),
                    ROUND_LATER,
                    MISHEYAKIR_66_ZMAN,
                    "");
            zmanim.add(misheyakir66);
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getTalitTefilinString(),
                mROZmanimCalendar.getMisheyakir60ZmaniyotMinutes(),
                ROUND_LATER,
                MISHEYAKIR_60_ZMAN,
                "TalitTefilin"));
        if (add66MisheyakirZman && !isForWeeklyZmanim) {
            ZmanListEntry misheyakir66 = new ZmanListEntry(zmanimNames.getTalitTefilinString() + " (66)",
                    mROZmanimCalendar.getMisheyakir66ZmaniyotMinutes(),
                    ROUND_LATER,
                    MISHEYAKIR_66_ZMAN,
                    "");
            zmanim.add(misheyakir66);
        }
        if (mSharedPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " " + zmanimNames.getElevatedString(),
                    mROZmanimCalendar.getSunrise(),
                    ROUND_LATER,
                    HANETZ_ZMAN,
                    ""));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString(),
                    mROZmanimCalendar.getHaNetz(),
                    ALWAYS_DISPLAY,
                    HANETZ_ZMAN,
                    "HaNetz"));
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")",
                    mROZmanimCalendar.getSeaLevelSunrise(),
                    ROUND_LATER,
                    HANETZ_ZMAN,
                    "HaNetz"));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + mROZmanimCalendar.getGeoLocation().getLocationName(), true) &&
                mSharedPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")",
                    mROZmanimCalendar.getSeaLevelSunrise(),
                    ROUND_LATER,
                    HANETZ_ZMAN,
                    "HaNetz"));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaMgaString(),
                mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis(),
                ROUND_EARLIER,
                SOF_ZMAN_SHMA_MGA_ZMAN,
                "SofZmanShmaMGA"));
        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            ZmanListEntry birchatHachama = new ZmanListEntry(zmanimNames.getBirkatHachamaString(),
                    mROZmanimCalendar.getSofZmanShmaGRA(),
                    ROUND_EARLIER,
                    BIRKAT_HACHMAH_ZMAN,
                    "");
            birchatHachama.setNoteworthyZman(true);
            zmanim.add(birchatHachama);
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaGraString(),
                mROZmanimCalendar.getSofZmanShmaGRA(),
                ROUND_EARLIER,
                SOF_ZMAN_SHMA_GRA_ZMAN,
                "SofZmanShmaGRA"));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            ZmanListEntry zman = new ZmanListEntry(zmanimNames.getAchilatChametzString(),
                    mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis(),
                    ROUND_EARLIER,
                    SOF_ZMAN_ACHILAT_CHAMETZ_ZMAN,
                    "SofZmanAchilatChametz");
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(),
                    mROZmanimCalendar.getSofZmanTfilaGRA(),
                    ROUND_EARLIER,
                    SOF_ZMAN_BERACHOT_SHMA_ZMAN,
                    "SofZmanTefila"));
            zman = new ZmanListEntry(zmanimNames.getBiurChametzString(),
                    mROZmanimCalendar.getSofZmanBiurChametzMGA(),
                    ROUND_EARLIER,
                    SOF_ZMAN_BIUR_CHAMETZ_ZMAN,
                    "SofZmanBiurChametz");
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(),
                    mROZmanimCalendar.getSofZmanTfilaGRA(),
                    ROUND_EARLIER,
                    SOF_ZMAN_BERACHOT_SHMA_ZMAN,
                    "SofZmanTefila"));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotString(),
                mROZmanimCalendar.getChatzot(),
                ROUND_EARLIER,
                CHATZOT_ZMAN,
                "Chatzot"));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaGedolaString(),
                mROZmanimCalendar.getMinchaGedolaGreaterThan30(),
                ROUND_LATER,
                MINCHA_GEDOLAH_ZMAN,
                "MinchaGedola"));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaKetanaString(),
                mROZmanimCalendar.getMinchaKetanaGRA(),
                ROUND_LATER,
                MINCHA_KETANA_ZMAN,
                "MinchaKetana"));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " (" + zmanimNames.getHalachaBerurahString() + ")",
                mROZmanimCalendar.getPlagHaminchaGRA(),
                ROUND_LATER,
                PLAG_HAMINCHA_HB_ZMAN,
                "PlagHaMinchaHB"));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " (" + zmanimNames.getYalkutYosefString() + ")",
                mROZmanimCalendar.getPlagHaminchaYalkutYosef(),
                ROUND_LATER,
                PLAG_HAMINCHA_YY_ZMAN,
                "PlagHaMinchaYY"));
        if ((mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.FRIDAY) {
            ZmanListEntry candleLightingZman = new ZmanListEntry(
                    zmanimNames.getCandleLightingString() + " (" + (int) mROZmanimCalendar.getCandleLightingOffset() + ")",
                    mROZmanimCalendar.getCandleLighting(),
                    ROUND_EARLIER,
                    CANDLELIGHTING_ZMAN,
                    "CandleLighting");
            candleLightingZman.setNoteworthyZman(true);
            zmanim.add(candleLightingZman);
        }
        if (mSharedPreferences.getBoolean("ShowWhenShabbatChagEnds", false) && !isForWeeklyZmanim) {
            if (mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                LocalDate localDate = mROZmanimCalendar.getLocalDate();
                mROZmanimCalendar.setLocalDate(localDate.plusDays(1));
                mJewishDateInfo.setLocalDate(mROZmanimCalendar.getLocalDate());
                if (!mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {//only add if shabbat/yom tov ends tomorrow
                    Set<String> stringSet = mSharedPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            addShabbatEndsZman(zmanim, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, zmanimNames, false, true);
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            addRTZman(zmanim, mSharedPreferences, mROZmanimCalendar, zmanimNames ,true);
                        }
                    }
                }
                mROZmanimCalendar.setLocalDate(localDate);
                mJewishDateInfo.setLocalDate(localDate);
            }
        }
        if (mJewishDateInfo.tomorrow().getJewishCalendar().isTishaBav()) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(),
                    mROZmanimCalendar.getSunsetBasedOnElevationSetting(),
                    ROUND_EARLIER,
                    FAST_STARTS_TISHA_BAV_ZMAN,
                    "Shkia"));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getSunsetString(),
                mROZmanimCalendar.getSunsetBasedOnElevationSetting(),
                ROUND_EARLIER,
                SUNSET_ZMAN,
                "Shkia"));

        ZmanListEntry[] nightfallTimes = new ZmanListEntry[2];
        nightfallTimes[0] = new ZmanListEntry(zmanimNames.getTzaitHacochavimString(),
                mROZmanimCalendar.getTzeit(),
                ROUND_LATER,
                TZET_HAKOKHAVIM_ZMAN,
                "TzeitHacochavim");
        nightfallTimes[1] = new ZmanListEntry(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(),
                mROZmanimCalendar.getTzeitLChumra(),
                ROUND_LATER,
                TZET_HAKOKHAVIM_LCHUMRA_ZMAN,
                "TzeitHacochavimLChumra");

        for (ZmanListEntry nightfallTime : nightfallTimes) {
            if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
                nightfallTime.setShouldBeDimmed(true);
            }
            zmanim.add(nightfallTime);
        }

        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getDayOfWeek() != Calendar.FRIDAY) {// we already added Candles
                if (mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY) {// Shabbat going into Yom Tov
                    addShabbatEndsZman(zmanim, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, zmanimNames, true, false);
                } else {// Yom Tov going into Yom Tov
                    zmanim.add(new ZmanListEntry(zmanimNames.getCandleLightingString(),
                            mROZmanimCalendar.getTzeitLChumra(),
                            ROUND_LATER,
                            CANDLELIGHTING_ZMAN,
                            "TzeitHacochavimLChumra"));
                }
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis() && !mJewishDateInfo.getJewishCalendar().isYomKippur()) {
            ZmanListEntry fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString(),
                    mROZmanimCalendar.getTzeitLChumra(),
                    ROUND_LATER,
                    FAST_ENDS_ZMAN,
                    "FastEnd");
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
        }
        boolean doesChagShabbatEnd = mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov();
        if (doesChagShabbatEnd) {// if it is Shabbat/Yom Tov and the next day is not Shabbat/Yom Tov, add end time and RT
            addShabbatEndsZman(zmanim, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, zmanimNames, false, false);
            addRTZman(zmanim, mSharedPreferences, mROZmanimCalendar, zmanimNames, false);
        } else if (mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY // always add RT for shabbat
                || mSharedPreferences.getBoolean("AlwaysShowRT", false)) {
            addRTZman(zmanim, mSharedPreferences, mROZmanimCalendar, zmanimNames, false);
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotLaylaString(),
                mROZmanimCalendar.getSolarMidnight(),
                ROUND_LATER,
                CHATZOT_LAYLA_ZMAN,
                "NightChatzot"));
    }

    private static void addRTZman(List<ZmanListEntry> zmanim, SharedPreferences mSettingsPreferences, ROZmanimCalendar mROZmanimCalendar, ZmanimNames zmanimNames, boolean isForTomorrow) {
        Instant rtZman = mSettingsPreferences.getBoolean("overrideRTZman", false) ?
                mROZmanimCalendar.getTzais72ForceRegZmanis() : mROZmanimCalendar.isUseAmudehHoraah() ?
                mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah() : mROZmanimCalendar.getTzais72Zmanis();

        boolean isFixed = true;
        if (rtZman != null) {
            isFixed = rtZman.equals(mROZmanimCalendar.getTzais72Minutes());
        }
        ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString() +
                zmanimNames.getRTType(isFixed) +
                (isForTomorrow ? zmanimNames.getMacharString() : ""),
                rtZman,
                ROUND_LATER,
                RABBENU_TAM_ZMAN,
                (isForTomorrow ? "" : "RT"));

        rt.setNoteworthyZman(true);
        zmanim.add(rt);
    }

    private static void addShabbatEndsZman(List<ZmanListEntry> zmanim, SharedPreferences mSettingsPreferences, ROZmanimCalendar mROZmanimCalendar, JewishDateInfo mJewishDateInfo, ZmanimNames zmanimNames, boolean isForCandleLigthting, boolean isForTomorrow) {
        ZmanListEntry endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag(zmanimNames.isZmanimInHebrew(), zmanimNames.isZmanimAmericanized(), mJewishDateInfo) + zmanimNames.getEndsString(),
                mROZmanimCalendar.isUseAmudehHoraah() ? mROZmanimCalendar.getTzeitShabbatAmudeiHoraah() : mROZmanimCalendar.getTzaisAteretTorah(),
                ROUND_LATER,
                isForCandleLigthting ? CANDLELIGHTING_ZMAN : SHABBAT_CHAG_ENDS_ZMAN,
                "ShabbatEnd");
        if (mSettingsPreferences.getBoolean("overrideAHEndShabbatTime", false)) {
            String setting = mSettingsPreferences.getString("EndOfShabbatOpinion", "1");
            switch (setting) {
                case "1" -> endShabbat.setZman(mROZmanimCalendar.getTzaisAteretTorah());
                case "2" -> endShabbat.setZman(mROZmanimCalendar.getTzeitShabbatAmudeiHoraah());
                case "3" -> endShabbat.setZman(mROZmanimCalendar.getTzeitShabbatAmudeiHoraahLesserThan40());
            }
        }
        if (isForTomorrow) {
            endShabbat.setTitle(endShabbat.getTitle() + zmanimNames.getMacharString());
            endShabbat.setNotificationKey("");
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
    private static String getShabbatAndOrChag(boolean mIsZmanimInHebrew, boolean mIsZmanimAmericanized, JewishDateInfo mJewishDateInfo) {
        if (mJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha()
                && mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY) {
            return mIsZmanimInHebrew ? "שבת/חג" : "Shabbat/Ḥag";
        } else if (mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY) {
            return mIsZmanimInHebrew ? "שבת" : "Shabbat";
        } else {
            return mIsZmanimInHebrew ? "חג" :
                    mIsZmanimAmericanized ? "Chag" : "Ḥag";
        }
    }

    public static ZmanListEntry getNextUpcomingZman(LocalDate mCurrentDateShown, ROZmanimCalendar roZmanimCalendar, JewishDateInfo mJewishDateInfo, SharedPreferences mSharedPreferences) {
        ZmanListEntry theZman = null;
        List<ZmanListEntry> zmanim = new ArrayList<>();
        LocalDate today = LocalDate.now();
        ROZmanimCalendar mROZmanimCalendar = roZmanimCalendar.getCopy();

        today = today.minusDays(1);
        mROZmanimCalendar.setLocalDate(today);
        mJewishDateInfo.setLocalDate(today);
        // we only care about the date
        addZmanim(zmanim, false, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, true);//for the previous day

        today = today.plusDays(1);
        mROZmanimCalendar.setLocalDate(today);
        mJewishDateInfo.setLocalDate(today);
        addZmanim(zmanim, false, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, true);//for the current day

        today = today.plusDays(1);
        mROZmanimCalendar.setLocalDate(today);
        mJewishDateInfo.setLocalDate(today);
        addZmanim(zmanim, false, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, true);//for the next day

        mROZmanimCalendar.setLocalDate(mCurrentDateShown);
        mJewishDateInfo.setLocalDate(mCurrentDateShown);//reset
        //find the next upcoming zman that is after the current time and before all the other zmanim
        for (ZmanListEntry zmanListEntry: zmanim) {
            if (zmanListEntry.getZman() != null && zmanListEntry.getZman().isAfter(Instant.now()) && (theZman == null || zmanListEntry.getZman().isBefore(theZman.getZman()))) {
                theZman = zmanListEntry;
            }
        }
        return theZman;
    }
}
