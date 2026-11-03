package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;

import androidx.annotation.NonNull;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.time.Instant;

public class JewishCalendarWithExtraMethods extends JewishCalendar {

    private boolean isSafekMukafChoma = false;

    public boolean getIsSafekMukafChoma() {
        return isSafekMukafChoma;
    }

    public void setIsSafekMukafChoma(boolean isSafekMukafChoma) {
        this.isSafekMukafChoma = isSafekMukafChoma;
    }

    public Context mContext;

    @Override
    public boolean isPurim() {
        if (getIsMukafChoma()) {
            return getYomTovIndex() == SHUSHAN_PURIM;
        } else if (isSafekMukafChoma) {
            return getYomTovIndex() == PURIM || getYomTovIndex() == SHUSHAN_PURIM;
        } else {
            return getYomTovIndex() == PURIM;
        }
    }

    public boolean isRegularTaanis() {
        int holidayIndex = getYomTovIndex();
        return holidayIndex == SEVENTEEN_OF_TAMMUZ || holidayIndex == TISHA_BEAV
                || holidayIndex == FAST_OF_GEDALYAH || holidayIndex == TENTH_OF_TEVES || holidayIndex == FAST_OF_ESTHER;
    }

    /**
     * This method is used to get the current date of the jewish calendar based on sunset. The start of the hebrew date is based on the night time.
     * Therefore, the date changes after sunset. However, the Gregorian date is based on Midnight. Internally, the JewishCalendar uses the Gregorian
     * date, and this causes the hebrew date to be a day before even after sunset. This method will check for sunset and adjust the date accordingly.
     * For example, if the date is 18 July 2025, and the hebrew date is 22 Tammuz 5785, this method will return 22 Tammuz 5785 before sunset, and
     * 23 Tammuz 5785 after sunset.
     * @param zmanimCalendar the zmanim calendar set to the location where sunset occurs
     * @return the current jewish date based on if it's before or after sunset/dawn
     */
    public String currentToString(ROZmanimCalendar zmanimCalendar) {
        JewishCalendarWithExtraMethods jewishCalendar = new JewishCalendarWithExtraMethods();
        jewishCalendar.mContext = this.mContext;
        jewishCalendar.setGregorianDate(zmanimCalendar.getLocalDate());// set the date to the date of the zmanim calendar, because we are going based on that sunset
        if (zmanimCalendar.getSunset() != null && Instant.now().isAfter(zmanimCalendar.getSunsetBasedOnElevationSetting())) {
            jewishCalendar.setGregorianDate(jewishCalendar.getLocalDate().plusDays(1));
        }
        return jewishCalendar.toString();
    }

	/**
	 * This method is used to get the current date of the jewish calendar based on sunset. The start of the hebrew date is based on the night time.
	 * Therefore, the date changes after sunset. However, the Gregorian date is based on Midnight. Internally, the JewishCalendar uses the Gregorian
	 * date, and this causes the hebrew date to be a day before even after sunset. This method will check for sunset and adjust the date accordingly.
	 * For example, if the date is 18 July 2025, and the hebrew date is 22 Tammuz 5785, this method will return 22 Tammuz 5785 before sunset, and
	 * 23 Tam 5785 after sunset.
	 * @param zmanimCalendar the zmanim calendar set to the location where sunset occurs
	 * @return the current jewish date based on if it's before or after sunset/dawn
	 */
	public String currentToShortString(ROZmanimCalendar zmanimCalendar) {
		JewishCalendarWithExtraMethods jewishCalendar = new JewishCalendarWithExtraMethods();
		jewishCalendar.mContext = this.mContext;
		jewishCalendar.setGregorianDate(zmanimCalendar.getLocalDate());// set the date to the date of the zmanim calendar, because we are going based on that sunset
		if (zmanimCalendar.getSunset() != null && Instant.now().isAfter(zmanimCalendar.getSunset())) {
            jewishCalendar.setGregorianDate(jewishCalendar.getLocalDate().plusDays(1));
		}
		return jewishCalendar.toShortString();
	}

    @NonNull
    @Override
    public String toString() {
        HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
        if (mContext != null) {
            hebrewDateFormatter.setHebrewFormat(Utils.isLocaleHebrew(mContext));
        }
        return hebrewDateFormatter.format(this)
                .replace("Teves", "Tevet")
                .replace("Tishrei", "Tishri")
                .replace("Cheshvan", "Ḥeshvan");
    }

	public String toShortString() {
		HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
		if (mContext != null) {
			hebrewDateFormatter.setHebrewFormat(Utils.isLocaleHebrew(mContext));
		}
		return hebrewDateFormatter.format(this)
			.replace("Tishrei", "Tish")
			.replace("Cheshvan", "Ḥesh")
			.replace("Kislev", "Kis")
			.replace("Teves", "Tev")
			.replace("Shevat", "Shev")
			// Adar is not needed
			.replace("Nissan", "Nis")
			// Iyar is not needed
			.replace("Sivan", "Siv")
			.replace("Tammuz", "Tam");
			// Av and Elul are not needed
	}
}
