package com.ej.rovadiahyosefcalendar.activities.ui.siddur;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mCurrentDateShown;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.materialToolbar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSettingsPreferences;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.JerusalemDirectionMapsActivity;
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;
import com.ej.rovadiahyosefcalendar.classes.CustomPreferenceView;
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.databinding.FragmentSiddurBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.Daf;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;
import com.kosherjava.zmanim.util.GeoLocation;

import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SiddurFragment extends Fragment {

    private FragmentSiddurBinding binding;
    private Context mContext;
    private FragmentActivity mActivity;
    private static LinearLayout dateButtons;
    private Button mCalendarButton;
    private static boolean showAllPrayers = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = requireActivity();
    }

    @NonNull
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSiddurBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        EdgeToEdge.enable(mActivity);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateView();
        setupButtons();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mActivity.findViewById(R.id.siddur_preferences_container) != null) {
            updateView();
        }
    }

    public void updateView() {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.siddur_preferences_container, new SiddurPreferenceFragment())
                .commitNow();
    }

    public static class SiddurPreferenceFragment extends PreferenceFragmentCompat {

        private ROZmanimCalendar currentZmanimCalendar;
        private JewishDateInfo currentJewishDateInfo;
        private static final List<String> masechtosBavli = Arrays.asList(
                "ברכות",
                "שבת",
                "עירובין",
                "פסחים",
                "שקלים",
                "יומא",
                "סוכה",
                "ביצה",
                "ראש השנה",
                "תענית",
                "מגילה",
                "מועד קטן",
                "חגיגה",
                "יבמות",
                "כתובות",
                "נדרים",
                "נזיר",
                "סוטה",
                "גיטין",
                "קידושין",
                "בבא קמא",
                "בבא מציעא",
                "בבא בתרא",
                "סנהדרין",
                "מכות",
                "שבועות",
                "עבודה זרה",
                "הוריות",
                "זבחים",
                "מנחות",
                "חולין",
                "בכורות",
                "ערכין",
                "תמורה",
                "כריתות",
                "מעילה",
                "קינים",
                "תמיד",
                "מידות",
                "נדה");
        private String[] selectedMasechtot;
        private String[] selectedShaloshItems;
        private boolean isNightTikkunChatzot = true;
        private boolean isAfterSunset = false;
        private boolean isArvitAfterPlagBeforeSunset = false;
        private boolean isMinchaAfterSunsetBeforeTzeit = false;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.siddur_preference_screen, rootKey);
            initView();
        }

        @Override
        public void onResume() {
            super.onResume();
            initView();
        }

        @Override
        public void onStart() {
            super.onStart();
            initView();
        }

        private void initView() {
            if (currentZmanimCalendar == null) {
                LocationResolver locationResolver = new LocationResolver(requireContext(), requireActivity());
                currentZmanimCalendar = new ROZmanimCalendar(locationResolver.getRealtimeNotificationData(location -> {
                    if (location != null) {
                        String locationName = locationResolver.getLocationAsName(location.getLatitude(), location.getLongitude());
                        locationResolver.resolveElevation(() ->
                                currentZmanimCalendar = new ROZmanimCalendar(new GeoLocation(
                                        locationName,
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        locationResolver.getElevation(),
                                        locationResolver.getTimeZone())));
                    }
                    initView();
                }, false));
            }
            currentZmanimCalendar.setCalendar(Calendar.getInstance());// make sure the date is for right now when the user gets back to the page
            currentJewishDateInfo = new JewishDateInfo(mJewishDateInfo.getJewishCalendar().getInIsrael());
            if (currentZmanimCalendar.getSunset() != null && new Date().after(currentZmanimCalendar.getSunset())) {
                isAfterSunset = true;
            }
            Preference seeMoreTitle = findPreference("siddur_see_more_title");
            Preference seeMore = findPreference("siddur_see_more");
            if (seeMore != null) {
                seeMore.setOnPreferenceClickListener(v -> {
                    showAllPrayers = !showAllPrayers;
                    mCurrentDateShown.setTime(new Date());
                    mROZmanimCalendar.setCalendar(mCurrentDateShown);
                    mJewishDateInfo.setCalendar(mCurrentDateShown);
                    initView();
                    return true;
                });
                if (seeMoreTitle != null) {
                    seeMoreTitle.setVisible(!showAllPrayers);
                }
                seeMore.setTitle(showAllPrayers ? getString(R.string.see_prayers_currently_applicable) : getString(R.string.see_all_prayers));
            }
            if (dateButtons != null) {
                dateButtons.setVisibility(showAllPrayers ? View.VISIBLE : View.GONE);
            }

            Preference specialDay = findPreference("siddur_day_text");
            String dateAndSpecialDay = mJewishDateInfo.getJewishCalendar().getGregorianCalendar().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
            if (DateUtils.isSameDay(new Date(), mJewishDateInfo.getJewishCalendar().getGregorianCalendar().getTime())) {
                dateAndSpecialDay += " (" + getString(R.string.today) + ")";
            }
            dateAndSpecialDay += "\n" + mJewishDateInfo.getJewishCalendar().toString();
            String specialDayString = mJewishDateInfo.getSpecialDay(false);
            if (!specialDayString.isEmpty()) {
                dateAndSpecialDay += "\n" + specialDayString;
            }
            if (specialDay != null) {
                if (showAllPrayers) {
                    specialDay.setTitle(dateAndSpecialDay);
                } else {
                    specialDay.setTitle(requireContext().getString(R.string.prayers_able_to_be_said_now) + "\n" +
                            currentJewishDateInfo.getJewishCalendar().currentToString(currentZmanimCalendar));
                }
            }

            CustomPreferenceView selichot = findPreference("siddur_selichot");
            if (selichot != null) {
                selichot.setVisible(showAllPrayers ? mJewishDateInfo.isSelichotSaid() : getSunsetBasedJewishDateInfo().isSelichotSaid() && isPrayerCurrentlySaid(selichot.getKey()));
                Date tzeit = currentZmanimCalendar.getTzeit();
                Date solarMidnight = currentZmanimCalendar.getSolarMidnight();
                Calendar midnightCal = Calendar.getInstance();
                if (midnightCal.get(Calendar.HOUR_OF_DAY) < 3) {// i.e right now is after midnight
                    if (solarMidnight != null) {
                        midnightCal.setTime(solarMidnight);
                    }
                    if (midnightCal.get(Calendar.HOUR_OF_DAY) < 3) {// i.e halachic midnight is after 12AM
                        ROZmanimCalendar yesterday = mROZmanimCalendar.getCopy();
                        yesterday.getCalendar().add(Calendar.DATE, -1);
                        tzeit = yesterday.getTzeit();
                        solarMidnight = yesterday.getSolarMidnight();
                    }
                }
                selichot.setDimmed((tzeit != null && new Date().after(tzeit)) &&
                        (solarMidnight != null && new Date().before(solarMidnight)));
                selichot.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.selichot));
                    return true;
                });
                CharSequence title = selichot.getTitle();
                if (title != null) {
                    selichot.setSummary(getSecondaryText(title));
                }
                selichot.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView shacharit = findPreference("siddur_shacharit");
            if (shacharit != null) {
                shacharit.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.shacharit));
                    return true;
                });
                CharSequence title = shacharit.getTitle();
                if (title != null) {
                    shacharit.setSummary(getSecondaryText(title));
                }
                shacharit.setVisible(showAllPrayers || isPrayerCurrentlySaid(shacharit.getKey()));
                shacharit.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView mussaf = findPreference("siddur_mussaf");
            if (mussaf != null) {
                mussaf.setVisible((getSunsetBasedJewishDateInfo().getJewishCalendar().isRoshChodesh() || getSunsetBasedJewishDateInfo().getJewishCalendar().isCholHamoed())
                        && (showAllPrayers || isPrayerCurrentlySaid(mussaf.getKey())));
                mussaf.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.mussaf));
                    return true;
                });
                CharSequence title = mussaf.getTitle();
                if (title != null) {
                    mussaf.setSummary(getSecondaryText(title));
                }
                mussaf.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView mincha = findPreference("siddur_mincha");
            if (mincha != null) {
                Date sunset = currentZmanimCalendar.getSunset();
                Date tzeit = currentZmanimCalendar.getTzeit();
                if (sunset != null && tzeit != null) {
                    isMinchaAfterSunsetBeforeTzeit = new Date().after(sunset) && new Date().before(tzeit);
                }
                mincha.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.mincha));
                    return true;
                });
                CharSequence title = mincha.getTitle();
                if (title != null) {
                    mincha.setSummary(getSecondaryText(title));
                }

                mincha.setVisible(showAllPrayers || isPrayerCurrentlySaid(mincha.getKey()));
                mincha.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            Preference nightDayOfWeek = findPreference("siddur_night_text");
            String nextDateAndSpecialDay = mJewishDateInfo.getJewishCalendar().getGregorianCalendar().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
            if (DateUtils.isSameDay(new Date(), mJewishDateInfo.getJewishCalendar().getGregorianCalendar().getTime())) {
                nextDateAndSpecialDay += " (" + getString(R.string.today) + ")";
            }
            nextDateAndSpecialDay += "\n" + getString(R.string.after_sunset) + "\n" + mJewishDateInfo.tomorrow().getJewishCalendar().toString();

            String nextSpecialDayString = mJewishDateInfo.tomorrow().getSpecialDay(false);
            if (!nextSpecialDayString.isEmpty()) {
                nextDateAndSpecialDay += "\n" + nextSpecialDayString;
            }
            if (nightDayOfWeek != null) {
                nightDayOfWeek.setTitle(nextDateAndSpecialDay);
                nightDayOfWeek.setVisible(showAllPrayers);
            }

            CustomPreferenceView arvit = findPreference("siddur_arvit");
            if (arvit != null) {
                Date plag = currentZmanimCalendar.getPlagHamincha();
                Date sunset = currentZmanimCalendar.getSunset();
                if (plag != null && sunset != null) {
                    isArvitAfterPlagBeforeSunset = new Date().after(plag) && new Date().before(sunset);
                }
                arvit.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.arvit), true);
                    return true;
                });
                CharSequence title = arvit.getTitle();
                if (title != null) {
                    arvit.setSummary(getSecondaryText(title));
                }
                arvit.setVisible(showAllPrayers || isPrayerCurrentlySaid(arvit.getKey()));
                arvit.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView sefiratHaomer = findPreference("siddur_sefirat_haomer");
            if (sefiratHaomer != null) {
                sefiratHaomer.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.sefirat_haomer), true);
                    return true;
                });
                CharSequence title = sefiratHaomer.getTitle();
                if (title != null) {
                    sefiratHaomer.setSummary(getSecondaryText(title));
                }
                if (showAllPrayers) {
                    sefiratHaomer.setVisible((getSunsetBasedJewishDateInfo().tomorrow().getJewishCalendar().getDayOfOmer() != -1 && getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfOmer() <= 49));
                } else {
                    sefiratHaomer.setVisible((getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfOmer() != -1 && getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfOmer() <= 49) && isPrayerCurrentlySaid(sefiratHaomer.getKey()));
                }
                sefiratHaomer.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView hadlakatNeirotChanuka = findPreference("siddur_hadlakat_neirot_chanuka");
            if (hadlakatNeirotChanuka != null) {
                hadlakatNeirotChanuka.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.hadlakat_neirot_chanuka), true);
                    return true;
                });
                CharSequence title = hadlakatNeirotChanuka.getTitle();
                if (title != null) {
                    hadlakatNeirotChanuka.setSummary(getSecondaryText(title));
                }
                hadlakatNeirotChanuka.setVisible((showAllPrayers ? getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfChanukah() != 8
                        && (getSunsetBasedJewishDateInfo().getJewishCalendar().isChanukah() || getSunsetBasedJewishDateInfo().tomorrow().getJewishCalendar().isChanukah())
                        : isPrayerCurrentlySaid(hadlakatNeirotChanuka.getKey()) && getSunsetBasedJewishDateInfo().getJewishCalendar().isChanukah()));
                hadlakatNeirotChanuka.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView havdalah = findPreference("siddur_havdala");
            if (havdalah != null) {
                havdalah.setOnPreferenceClickListener(v -> {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.havdalah_is_only_said_on_a_flame_tonight)
                            .setMessage(getString(R.string.havdalah_will_be_completed_after_the_fast) + "\n\n" + "בָּרוּךְ אַתָּה יְהֹוָה, אֱלֹהֵֽינוּ מֶֽלֶךְ הָעוֹלָם, בּוֹרֵא מְאוֹרֵי הָאֵשׁ:")
                            .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss());
                    if (showAllPrayers && getSunsetBasedJewishDateInfo().tomorrow().getJewishCalendar().isTishaBav() && getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY) {
                        builder.show();
                    } else {
                        if (!showAllPrayers && getSunsetBasedJewishDateInfo().getJewishCalendar().isTishaBav() && getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() == Calendar.SUNDAY) {
                            builder.show();
                        } else {
                            startSiddurActivity(getString(R.string.havdala), true);
                        }
                    }
                    return true;
                });

                if (showAllPrayers) {
                    havdalah.setVisible(!getSunsetBasedJewishDateInfo().getJewishCalendar().hasCandleLighting()
                            && getSunsetBasedJewishDateInfo().getJewishCalendar().isAssurBemelacha()
                            || (getSunsetBasedJewishDateInfo().getJewishCalendar().isTishaBav()
                            && (getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY
                            || getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() == Calendar.SUNDAY)));
                } else {
                    havdalah.setVisible((!getSunsetBasedJewishDateInfo().yesterday().getJewishCalendar().hasCandleLighting()
                            && getSunsetBasedJewishDateInfo().yesterday().getJewishCalendar().isAssurBemelacha()
                            || (getSunsetBasedJewishDateInfo().yesterday().getJewishCalendar().isTishaBav()
                            && getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() == Calendar.MONDAY))// because after sunset
                            && isPrayerCurrentlySaid(havdalah.getKey()));
                }
                havdalah.setDimmed(showAllPrayers ?
                        getSunsetBasedJewishDateInfo().tomorrow().getJewishCalendar().isTishaBav()
                                && getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY
                        : getSunsetBasedJewishDateInfo().getJewishCalendar().isTishaBav()
                        && getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() == Calendar.SUNDAY
                        && isPrayerCurrentlySaid(havdalah.getKey()));

                CharSequence title = havdalah.getTitle();
                if (title != null) {
                    havdalah.setSummary(getSecondaryText(title));
                }
                havdalah.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView kriatShemaAlHamita = findPreference("siddur_kriatShema");
            if (kriatShemaAlHamita != null) {
                kriatShemaAlHamita.setVisible(showAllPrayers || isPrayerCurrentlySaid(kriatShemaAlHamita.getKey()));
                kriatShemaAlHamita.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.kriatShema), true);
                    return true;
                });
                CharSequence title = kriatShemaAlHamita.getTitle();
                if (title != null) {
                    kriatShemaAlHamita.setSummary(getSecondaryText(title));
                }
                kriatShemaAlHamita.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView tikkunChatzot = findPreference("siddur_tikkun_chatzot");
            CustomPreferenceView tikkunChatzot3Weeks = findPreference("siddur_tikkun_chatzot_3_weeks");

            Preference.OnPreferenceClickListener tikkunChatzotOnClickListener;
            if (showAllPrayers) {
                tikkunChatzotOnClickListener = v -> {
                    if (getSunsetBasedJewishDateInfo().is3Weeks()) {
                        boolean isNightTikkunSaid = getSunsetBasedJewishDateInfo().tomorrow().isNightTikkunChatzotSaid();
                        boolean isDayTikkunSaid = getSunsetBasedJewishDateInfo().isDayTikkunChatzotSaid();
                        if (isNightTikkunSaid && isDayTikkunSaid) {// ask the user
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.do_you_want_to_say_tikkun_chatzot_for_the_day)
                                    .setMessage(R.string.looking_to_say_this_version_of_tikkun_chatzot)
                                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                                        isNightTikkunChatzot = false;
                                        startSiddurActivity(getString(R.string.tikkun_chatzot));
                                    })
                                    .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                                        isNightTikkunChatzot = true;
                                        startSiddurActivity(getString(R.string.tikkun_chatzot), true);
                                    })
                                    .show();
                        } else if (isDayTikkunSaid) {
                            isNightTikkunChatzot = false;
                            startSiddurActivity(getString(R.string.tikkun_chatzot));
                        } else if (isNightTikkunSaid) {
                            isNightTikkunChatzot = true;
                            startSiddurActivity(getString(R.string.tikkun_chatzot), true);
                        } else {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.tikkun_chatzot_is_not_said_today_or_tonight)
                                    .setMessage(R.string.tikkun_chatzot_is_not_said_today_or_tonight_possible_reasons)
                                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                    } else {// not the 3 weeks
                        if (getSunsetBasedJewishDateInfo().tomorrow().isNightTikkunChatzotSaid()) {
                            isNightTikkunChatzot = true;
                            startSiddurActivity(getString(R.string.tikkun_chatzot), true);
                        } else {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.tikkun_chatzot_is_not_said_tonight)
                                    .setMessage(R.string.tikkun_chatzot_is_not_said_tonight_possible_reasons)
                                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                    }
                    return true;
                };
            } else {// based on current time
                tikkunChatzotOnClickListener = v -> {
                    if (getSunsetBasedJewishDateInfo().is3Weeks()) {
                        boolean isNightTikkunSaid = getSunsetBasedJewishDateInfo().isNightTikkunChatzotSaid();
                        boolean isDayTikkunSaid = getSunsetBasedJewishDateInfo().isDayTikkunChatzotSaid();
                        boolean isNowForDayTikkun = (currentZmanimCalendar.getChatzot() != null && new Date().after(currentZmanimCalendar.getChatzot()))
                                && (currentZmanimCalendar.getSunset() != null && new Date().before(currentZmanimCalendar.getSunset()));
                        if (isNightTikkunSaid && isDayTikkunSaid) {// figure out which it is by the time
                            isNightTikkunChatzot = !isNowForDayTikkun;
                            startSiddurActivity(getString(R.string.tikkun_chatzot), isNightTikkunChatzot);
                        } else if (isDayTikkunSaid && isNowForDayTikkun) {
                            isNightTikkunChatzot = false;
                            startSiddurActivity(getString(R.string.tikkun_chatzot));
                        } else if (isNightTikkunSaid && !isNowForDayTikkun) {
                            isNightTikkunChatzot = true;
                            startSiddurActivity(getString(R.string.tikkun_chatzot), true);
                        } else {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.tikkun_chatzot_is_not_said_today_or_tonight)
                                    .setMessage(R.string.tikkun_chatzot_is_not_said_today_or_tonight_possible_reasons)
                                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                    } else {// not the 3 weeks
                        if (getSunsetBasedJewishDateInfo().isNightTikkunChatzotSaid()) {
                            isNightTikkunChatzot = true;
                            startSiddurActivity(getString(R.string.tikkun_chatzot), true);
                        } else {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.tikkun_chatzot_is_not_said_tonight)
                                    .setMessage(R.string.tikkun_chatzot_is_not_said_tonight_possible_reasons)
                                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                    }
                    return true;
                };
            }

            if (tikkunChatzot != null) {
                tikkunChatzot.setVisible(showAllPrayers ? !getSunsetBasedJewishDateInfo(false).is3Weeks() : isPrayerCurrentlySaid(tikkunChatzot.getKey()));
                tikkunChatzot.setOnPreferenceClickListener(tikkunChatzotOnClickListener);
                CharSequence title = tikkunChatzot.getTitle();
                if (title != null) {// only add summary if not during the 3 weeks
                    tikkunChatzot.setSummary(getSecondaryText(title));
                }
                tikkunChatzot.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }
            if (tikkunChatzot3Weeks != null) {
                tikkunChatzot3Weeks.setVisible(showAllPrayers ? getSunsetBasedJewishDateInfo(false).is3Weeks() : isPrayerCurrentlySaid(tikkunChatzot3Weeks.getKey()));
                tikkunChatzot3Weeks.setOnPreferenceClickListener(tikkunChatzotOnClickListener);
                tikkunChatzot3Weeks.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            // dimming code
            if (showAllPrayers) {
                if (getSunsetBasedJewishDateInfo(false).is3Weeks()) {
                    if (tikkunChatzot3Weeks != null) {
                        tikkunChatzot3Weeks.setDimmed(!getSunsetBasedJewishDateInfo(false).isDayTikkunChatzotSaid() &&
                                !getSunsetBasedJewishDateInfo(false).tomorrow().isNightTikkunChatzotSaid());// dim it if both the night and day are not said
                    }
                } else {// not the 3 weeks
                    if (tikkunChatzot != null) {
                        tikkunChatzot.setDimmed(!getSunsetBasedJewishDateInfo(false).tomorrow().isNightTikkunChatzotSaid());
                    }
                }
            } else {
                if (getSunsetBasedJewishDateInfo().is3Weeks()) {
                    if (tikkunChatzot3Weeks != null) {
                        tikkunChatzot3Weeks.setDimmed(!getSunsetBasedJewishDateInfo().isDayTikkunChatzotSaid() && !getSunsetBasedJewishDateInfo().isNightTikkunChatzotSaid());// dim it if both the night and day are not said
                    }
                } else {
                    if (tikkunChatzot != null) {
                        tikkunChatzot.setDimmed(!getSunsetBasedJewishDateInfo().isNightTikkunChatzotSaid());
                    }
                }
            }
            // end of dimming code

            String sunset = DateFormat.getTimeInstance(DateFormat.SHORT).format(currentZmanimCalendar.getSunset() != null ? currentZmanimCalendar.getSunset() : new Date());

            CustomPreferenceView bh = findPreference("siddur_birchat_hamazon");
            if (bh != null) {
                bh.setOnPreferenceClickListener(v -> {
                    if (new SiddurMaker(getSunsetBasedJewishDateInfo(false)).getBirchatHamazonPrayers().equals(new SiddurMaker(getSunsetBasedJewishDateInfo(false).tomorrow()).getBirchatHamazonPrayers())) {
                        startSiddurActivity(getString(R.string.birchat_hamazon));//doesn't matter which day
                    } else {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.when_did_you_start_your_meal)
                                .setMessage(getString(R.string.did_you_start_your_meal_during_the_day) + " (" + sunset + ")")
                                .setPositiveButton(getString(R.string.yes), (dialog, which) -> startSiddurActivity(getString(R.string.birchat_hamazon)))
                                .setNegativeButton(getString(R.string.no), (dialog, which) -> startSiddurActivity(getString(R.string.birchat_hamazon), true));
                        if (showAllPrayers) {
                            builder.show();
                        } else {
                            if (currentZmanimCalendar.getSunset() != null && new Date().after(currentZmanimCalendar.getSunset())) {
                                builder.setPositiveButton(getString(R.string.yes), (dialog, which) -> {// it is after sunset and the user started before sunset, go back to yesterday
                                    currentJewishDateInfo.back();
                                    startSiddurActivity(getString(R.string.birchat_hamazon));
                                    currentJewishDateInfo.forward();
                                }).setNegativeButton(getString(R.string.no), (dialog, which) -> startSiddurActivity(getString(R.string.birchat_hamazon), true));
                                builder.show();
                            } else {
                                startSiddurActivity(getString(R.string.birchat_hamazon));
                            }
                        }
                    }
                    return true;
                });
                CharSequence title = bh.getTitle();
                if (title != null) {
                    bh.setSummary(getSecondaryText(title));
                }
                bh.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView bms = findPreference("siddur_birchat_meyin_shalosh");
            if (bms != null) {
                bms.setOnPreferenceClickListener(v -> {
                    String[] options = {getString(R.string.wine), getString(R.string._5_grains), getString(R.string._7_fruits), getString(R.string.other)};
                    boolean[] checkedItems = new boolean[options.length];

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.what_did_you_eat_drink)
                            .setMultiChoiceItems(options, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                            .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                                List<String> selectedOptions = new ArrayList<>();
                                for (int i = 0; i < options.length; i++) {
                                    if (checkedItems[i]) {
                                        selectedOptions.add(options[i]);
                                    }
                                }
                                if (selectedOptions.isEmpty()) {
                                    Toast.makeText(requireContext(), R.string.please_select_at_least_one_option, Toast.LENGTH_SHORT).show();
                                } else {
                                    selectedShaloshItems = selectedOptions.toArray(new String[0]);
                                    if (new SiddurMaker(getSunsetBasedJewishDateInfo(false)).getBirchatMeeyinShaloshPrayers(options).equals(new SiddurMaker(getSunsetBasedJewishDateInfo(false).tomorrow()).getBirchatMeeyinShaloshPrayers(options))) {
                                        startSiddurActivity(getString(R.string.birchat_meyin_shalosh));//doesn't matter which day
                                    } else {
                                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                                                .setTitle(R.string.when_did_you_start_your_meal)
                                                .setMessage(getString(R.string.did_you_start_your_meal_during_the_day) + " (" + sunset + ")")
                                                .setPositiveButton(getString(R.string.yes), (dialog2, which2) -> startSiddurActivity(getString(R.string.birchat_meyin_shalosh)))
                                                .setNegativeButton(getString(R.string.no), (dialog2, which2) -> startSiddurActivity(getString(R.string.birchat_meyin_shalosh), true));
                                        if (showAllPrayers) {
                                            builder.show();
                                        } else {
                                            if (currentZmanimCalendar.getSunset() != null && new Date().after(currentZmanimCalendar.getSunset())) {
                                                builder.setPositiveButton(getString(R.string.yes), (dialog2, which2) -> {// it is after sunset and the user started before sunset, go back to yesterday
                                                            currentJewishDateInfo.back();
                                                            startSiddurActivity(getString(R.string.birchat_meyin_shalosh));
                                                            currentJewishDateInfo.forward();
                                                        })
                                                        .setNegativeButton(getString(R.string.no), (dialog2, which2) -> startSiddurActivity(getString(R.string.birchat_meyin_shalosh), true));
                                                builder.show();
                                            } else {
                                                startSiddurActivity(getString(R.string.birchat_meyin_shalosh));
                                            }
                                        }
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                            .show();
                    return true;
                });
                CharSequence title = bms.getTitle();
                if (title != null) {
                    bms.setSummary(getSecondaryText(title));
                }
                bms.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView tefilatHaderech = findPreference("siddur_tefilat_haderech");
            if (tefilatHaderech != null) {
                tefilatHaderech.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.tefilat_haderech));
                    return true;
                });
                tefilatHaderech.setVisible(!(getSunsetBasedJewishDateInfo().getJewishCalendar().isAssurBemelacha() && currentZmanimCalendar.getTzeit() != null && new Date().before(currentZmanimCalendar.getTzeit()) // if today is Assur Bemelacha and it is before tzeit, don't show the button
                        || (getSunsetBasedJewishDateInfo().getJewishCalendar().hasCandleLighting() && currentZmanimCalendar.getSunset() != null && new Date().after(currentZmanimCalendar.getSunset()))));
                CharSequence title = tefilatHaderech.getTitle();
                if (title != null) {
                    tefilatHaderech.setSummary(getSecondaryText(title));
                }
                tefilatHaderech.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView sederSiyumMasechet = findPreference("siddur_seder_siyum_masechet");
            if (sederSiyumMasechet != null) {
                sederSiyumMasechet.setOnPreferenceClickListener(v -> {
                    String[] masechtosArray = masechtosBavli.toArray(new String[0]);
                    boolean[] checkedItems = new boolean[masechtosArray.length];

                    Daf currentDaf = YomiCalculator.getDafYomiBavli(getSunsetBasedJewishDateInfo().getJewishCalendar());
                    Daf nextDaf = YomiCalculator.getDafYomiBavli(getSunsetBasedJewishDateInfo().tomorrow().getJewishCalendar());

                    if (!currentDaf.getMasechta().equals(nextDaf.getMasechta())) {// we know there is a siyum today
                        for (int i = 0; i < masechtosArray.length; i++) {
                            if (masechtosArray[i].equals(currentDaf.getMasechta())) {
                                checkedItems[i] = true;// preset it
                            }
                        }
                    }

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(Utils.isLocaleHebrew() ? "בחר מסכתות" : "Choose Masekhtot")
                            .setMultiChoiceItems(masechtosArray, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                            .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                                List<String> selectedMasechtos = new ArrayList<>();
                                for (int i = 0; i < masechtosArray.length; i++) {
                                    if (checkedItems[i]) {
                                        selectedMasechtos.add(masechtosArray[i]);
                                    }
                                }
                                if (selectedMasechtos.isEmpty()) {
                                    Toast.makeText(requireContext(), R.string.please_select_at_least_one_option, Toast.LENGTH_SHORT).show();
                                } else {
                                    selectedMasechtot = selectedMasechtos.toArray(new String[0]);
                                    startSiddurActivity(getString(R.string.seder_siyum_masechet));
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                            .show();
                    return true;
                });
                CharSequence title = sederSiyumMasechet.getTitle();
                if (title != null) {
                    sederSiyumMasechet.setSummary(getSecondaryText(title));
                }
                sederSiyumMasechet.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView birchatLevana = findPreference("siddur_birchat_levana");
            if (birchatLevana != null) {
                birchatLevana.setOnPreferenceClickListener(v -> {
                    startSiddurActivity(getString(R.string.birchat_levana));
                    return true;
                });
                birchatLevana.setVisible(!getSunsetBasedJewishDateInfo().getBirchatLevana().isEmpty());// hide the button if there's no status text returned
                CharSequence title = birchatLevana.getTitle();
                if (title != null) {
                    birchatLevana.setSummary(getSecondaryText(title));
                }
                birchatLevana.setForceLTRTextDirection(!Utils.isLocaleHebrew());
            }

            CustomPreferenceView disclaimer = findPreference("siddur_misc_prayer");
            if (disclaimer != null) {
                disclaimer.setForceLTRTextDirection(!Utils.isLocaleHebrew());
                String title = "";
                String summary = "";
                if (getSunsetBasedJewishDateInfo().getJewishCalendar().getYomTovIndex() == JewishCalendar.TU_BESHVAT) {
                    if (Utils.isLocaleHebrew()) {
                        title = "תפילה לאתרוג";
                        summary = "טוב לאמר את התפילה הזו בטו בשבט";
                    } else {
                        title = "Prayer for an Etrog";
                        summary = "It is good to say this prayer on Tu'Beshvat";
                    }
                    disclaimer.setOnPreferenceClickListener(v -> {
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://elyahu41.github.io/Prayer%20for%20an%20Etrog.pdf")));
                        return true;
                    });
                }

                if (getSunsetBasedJewishDateInfo().getJewishCalendar().getUpcomingParshah() == JewishCalendar.Parsha.BESHALACH &&
                        getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() == Calendar.TUESDAY) {
                    if (Utils.isLocaleHebrew()) {
                        title = "פרשת המן";
                        summary = "טוב לאמר את התפילה הזו היום";
                    } else {
                        title = "Parshat Haman";
                        summary = "It is good to say this prayer today";
                    }
                    disclaimer.setOnPreferenceClickListener(v -> {
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://elyahu41.github.io/Parshat-Haman-3.pdf")));
                        return true;
                    });
                }

                disclaimer.setTitle(title);
                disclaimer.setSummary(summary);
                disclaimer.setVisible(!(getSunsetBasedJewishDateInfo().getJewishCalendar().getYomTovIndex() != JewishCalendar.TU_BESHVAT &&
                        !(getSunsetBasedJewishDateInfo().getJewishCalendar().getUpcomingParshah() == JewishCalendar.Parsha.BESHALACH &&
                                getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() == Calendar.TUESDAY)));

            }
        }

        /**
         * Returns a {@link JewishDateInfo} based on the current date and time. If showAllPrayers is true, the public editable date is returned.
         * Otherwise, the current date is returned taking into account if it is before/after sunset.
         *
         * @return a {@link JewishDateInfo} based on the current time or the public editable date.
         */
        private JewishDateInfo getSunsetBasedJewishDateInfo(boolean adjust) {
            if (isAfterSunset && adjust) {
                currentJewishDateInfo.forward();
            }
            JewishDateInfo jdi = showAllPrayers ? mJewishDateInfo.getCopy() : currentJewishDateInfo.getCopy();
            currentJewishDateInfo.setCalendar(Calendar.getInstance());
            return jdi;
        }

        private JewishDateInfo getSunsetBasedJewishDateInfo() {
            return getSunsetBasedJewishDateInfo(true);
        }

        private boolean isPrayerCurrentlySaid(String key) {
            if (currentZmanimCalendar.getSunset() == null || currentZmanimCalendar.getSunrise() == null) {
                return true;// show the prayer by default
            }
            return switch (key) {
                case "siddur_selichot" -> {
                    boolean isSelichotNotSaidNow = (new Date().after(currentZmanimCalendar.getSunset()) && currentZmanimCalendar.isNowBeforeSecondAshmora())
                            || (!currentZmanimCalendar.isNowBeforeSecondAshmora() && !currentZmanimCalendar.isNowAfterHalachicSolarMidnight());// easier to check for the NOT case
                    yield !isSelichotNotSaidNow;
                }
                case "siddur_shacharit" ->
                        new Date().after(currentZmanimCalendar.getAlotHashachar()) && new Date().before(currentZmanimCalendar.getChatzot());
                case "siddur_mussaf" ->
                        new Date().after(currentZmanimCalendar.getAlotHashachar()) && new Date().before(currentZmanimCalendar.getSunset());
                case "siddur_mincha" ->
                        new Date().after(currentZmanimCalendar.getMinchaGedolaGreaterThan30()) && new Date().before(currentZmanimCalendar.getTzeit());
                case "siddur_arvit" ->
                        new Date().after(currentZmanimCalendar.getPlagHamincha()) || new Date().before(currentZmanimCalendar.getAlotHashachar());
                case "siddur_sefirat_haomer",
                     "siddur_hadlakat_neirot_chanuka",
                     "siddur_havdala",
                     "siddur_kriatShema" ->
                        new Date().after(currentZmanimCalendar.getSunset()) || new Date().before(currentZmanimCalendar.getAlotHashachar());
                case "siddur_tikkun_chatzot" ->
                        !getSunsetBasedJewishDateInfo().is3Weeks() && currentZmanimCalendar.isNowAfterHalachicSolarMidnight() && new Date().before(currentZmanimCalendar.getAlotHashachar());
                case "siddur_tikkun_chatzot_3_weeks" -> getSunsetBasedJewishDateInfo().is3Weeks() &&
                        ((currentZmanimCalendar.isNowAfterHalachicSolarMidnight() && new Date().before(currentZmanimCalendar.getAlotHashachar())) // night tikkun chatzot
                                || (new Date().after(currentZmanimCalendar.getChatzot()) && new Date().before(currentZmanimCalendar.getSunset()) && getSunsetBasedJewishDateInfo().getJewishCalendar().getDayOfWeek() != Calendar.SATURDAY)); // day tikkun chatzot, even though beki'im behalacha says to NOT say it after mincha ketana. However, that is not brought down by Rabbi Ovadiah and his sons
                default -> true;
            };
        }

        @Nullable
        public String getSecondaryText(CharSequence prayer) {
            String result = "";
            JewishDateInfo timeAdjustedJDI = getSunsetBasedJewishDateInfo();

            if (prayer.equals("סליחות")) {
                if (timeAdjustedJDI.getJewishCalendar().isAseresYemeiTeshuva()) {
                    result = "עשרת ימי תשובה";
                }
            } else if (prayer.equals("שחרית")) {
                List<String> entries = new ArrayList<>();
                if (timeAdjustedJDI.getJewishCalendar().isRoshChodesh() || timeAdjustedJDI.getJewishCalendar().isCholHamoed()) {
                    entries.add("יעלה ויבוא");
                }
                if (timeAdjustedJDI.getJewishCalendar().isPurim() || timeAdjustedJDI.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM) {
                    entries.add("[על הניסים]");
                }
                if (timeAdjustedJDI.getJewishCalendar().isChanukah()) {
                    entries.add("על הניסים");
                }
                String hallel = timeAdjustedJDI.getHallelOrChatziHallel();
                if (hallel.isEmpty()) {
                    String tachanun = timeAdjustedJDI.getIsTachanunSaid()
                            .replace("צדקתך", "")
                            .replace("לא אומרים תחנון", "יהי שם")
                            .replace("אומרים תחנון רק בבוקר", "תחנון")
                            .replace("יש מדלגים תחנון במנחה", "תחנון")
                            .replace("אומרים תחנון", "תחנון")
                            .replace("No Tachanun today", "יהי שם")
                            .replace("Tachanun only in the morning", "תחנון")
                            .replace("Some say Tachanun today", "יש אומרים תחנון")
                            .replace("Some skip Tachanun by mincha", "תחנון")
                            .replace("There is Tachanun today", "תחנון");
                    if (!tachanun.isEmpty()) entries.add(tachanun);
                } else {
                    entries.add(hallel);
                }
                result = TextUtils.join(", ", entries);
            } else if (prayer.equals("מוסף")) {
                List<String> entries = new ArrayList<>();
                String ulChaparat = timeAdjustedJDI.getIsUlChaparatPeshaSaid();
                if ("אומרים וּלְכַפָּרַת פֶּשַׁע".equals(ulChaparat) || "Say וּלְכַפָּרַת פֶּשַׁע".equals(ulChaparat)) {
                    entries.add(ulChaparat.replace("אומרים ", "").replace("Say ", ""));
                }
                if (timeAdjustedJDI.getJewishCalendar().isPurim() || timeAdjustedJDI.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM) {
                    entries.add("[על הניסים]");
                }
                if (timeAdjustedJDI.getJewishCalendar().isChanukah()) {
                    entries.add("על הניסים");
                }
                result = TextUtils.join(", ", entries);
            } else if (prayer.equals("מנחה")) {
                List<String> entries = new ArrayList<>();
                if (isMinchaAfterSunsetBeforeTzeit) {
                    timeAdjustedJDI.back();
                }
                if (timeAdjustedJDI.getJewishCalendar().isRoshChodesh() || timeAdjustedJDI.getJewishCalendar().isCholHamoed()) {
                    entries.add("יעלה ויבוא");
                }
                if (timeAdjustedJDI.getJewishCalendar().isPurim() || timeAdjustedJDI.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM) {
                    entries.add("[על הניסים]");
                }
                if (timeAdjustedJDI.getJewishCalendar().isChanukah()) {
                    entries.add("על הניסים");
                }
                String tachanun = timeAdjustedJDI.getIsTachanunSaid()
                        .replace("לא אומרים תחנון", "יהי שם")
                        .replace("אומרים תחנון רק בבוקר", "יהי שם")
                        .replace("יש מדלגים תחנון במנחה", "יש אומרים תחנון")
                        .replace("אומרים תחנון", "תחנון")
                        .replace("No Tachanun today", "יהי שם")
                        .replace("Tachanun only in the morning", "יהי שם")
                        .replace("Some say Tachanun today", "יש אומרים תחנון")
                        .replace("Some skip Tachanun by mincha", "יש אומרים תחנון")
                        .replace("There is Tachanun today", "תחנון");
                if (!tachanun.isEmpty()) entries.add(tachanun);
                result = TextUtils.join(", ", entries);
                if (isMinchaAfterSunsetBeforeTzeit) {
                    timeAdjustedJDI.forward();
                }
            } else if (prayer.equals("ערבית")) {
                if (showAllPrayers || isArvitAfterPlagBeforeSunset) {
                    timeAdjustedJDI.forward();
                }
                List<String> entries = new ArrayList<>();
                if (timeAdjustedJDI.getJewishCalendar().isRoshChodesh()) {
                    entries.add("ברכי נפשי");
                }
                if (new TefilaRules().isVeseinTalUmatarStartDate(timeAdjustedJDI.getJewishCalendar())) {
                    entries.add("ברך עלינו");
                }
                if (timeAdjustedJDI.getJewishCalendar().isRoshChodesh() || timeAdjustedJDI.getJewishCalendar().isCholHamoed()) {
                    entries.add("יעלה ויבוא");
                }
                if (timeAdjustedJDI.getJewishCalendar().isPurim() || timeAdjustedJDI.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM) {
                    entries.add("[על הניסים]");
                }
                if (timeAdjustedJDI.getJewishCalendar().isChanukah()) {
                    entries.add("על הניסים");
                }
                result = TextUtils.join(", ", entries);
                if (showAllPrayers || isArvitAfterPlagBeforeSunset) {
                    timeAdjustedJDI.back();
                }
            } else if (prayer.equals("ספירת העומר")) {
                int omer = timeAdjustedJDI.getJewishCalendar().getDayOfOmer();
                if (showAllPrayers) {
                    omer = getSunsetBasedJewishDateInfo(false).tomorrow().getJewishCalendar().getDayOfOmer();
                }
                if (omer != -1) {
                    return String.valueOf(omer);
                }
            } else if (prayer.equals("ברכת המזון")) {
                List<String> entries = new ArrayList<>();
                if (timeAdjustedJDI.getJewishCalendar().isPurim() || timeAdjustedJDI.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM) {
                    entries.add("[על הניסים]");
                }
                if (timeAdjustedJDI.getJewishCalendar().isChanukah()) {
                    entries.add("על הניסים");
                }
                if (showAllPrayers && timeAdjustedJDI.getJewishCalendar().getDayOfWeek() == 7 &&
                        currentZmanimCalendar.getTzeit() != null && !new Date().before(currentZmanimCalendar.getTzeit())) {
                    entries.add("[רצה]");
                }
                if (timeAdjustedJDI.getJewishCalendar().isRoshChodesh() || timeAdjustedJDI.getJewishCalendar().isCholHamoed()
                        || timeAdjustedJDI.getJewishCalendar().isYomTovAssurBemelacha() && !timeAdjustedJDI.getJewishCalendar().isYomKippur()) {
                    entries.add("יעלה ויבוא");
                }
                result = TextUtils.join(", ", entries);
            } else if (prayer.equals("תיקון חצות")) {
                if (showAllPrayers) {
                    timeAdjustedJDI.forward();
                }
                if (timeAdjustedJDI.isNightTikkunChatzotSaid()) {
                    if (timeAdjustedJDI.getJewishCalendar().isTishaBav()) {
                        result = "תיקון רחל";
                    } else {
                        result = timeAdjustedJDI.isOnlyTikkunLeiaSaid(true) ? "תיקון לאה" : "תיקון רחל ,תיקון לאה";
                    }
                }
                if (showAllPrayers) {
                    timeAdjustedJDI.back();
                }
            } else if (prayer.equals("סדר סיום מסכת")) {
                Daf currentDaf = YomiCalculator.getDafYomiBavli(timeAdjustedJDI.getJewishCalendar());
                Daf nextDaf = YomiCalculator.getDafYomiBavli(timeAdjustedJDI.tomorrow().getJewishCalendar());
                if (currentDaf != null && nextDaf != null && !currentDaf.getMasechta().equals(nextDaf.getMasechta())) {
                    return currentDaf.getMasechta();
                }
                return "";
            } else {
                return "";
            }

            return result;
        }

        /**
         * Convenience method to start the SiddurViewActivity with a prayer based on the DAY.
         *
         * @param prayer a string representing the prayer. For example: "שחרית"
         */
        private void startSiddurActivity(String prayer) {
            startSiddurActivity(prayer, false);
        }

        /**
         * Actual method to start the SiddurViewActivity with a prayer based on the DAY. However, if forNextDay is true, the prayer passed in is for
         * the NEXT DAY. I.E. Night
         *
         * @param prayer     a string representing the prayer. For example: "שחרית"
         * @param forNextDay if true, the prayer passed in is for the NEXT DAY. I.E. Night
         */
        private void startSiddurActivity(String prayer, boolean forNextDay) {
            if (forNextDay) {
                mJewishDateInfo.forward();// forNextDay is only for the editable date. We will adjust the date for the current jdi object in the get method
            }
            if (prayer.equals(getString(R.string.mincha)) && isMinchaAfterSunsetBeforeTzeit) {
                currentJewishDateInfo.back();// edge case for mincha after sunset but before tzeit so date has changed
            }
            if (prayer.equals(getString(R.string.arvit)) && isArvitAfterPlagBeforeSunset) {
                currentJewishDateInfo.forward();// edge case for arvit after plag but before sunset so date hasn't changed
            }
            Intent intent = new Intent(requireContext(), SiddurViewActivity.class)
                    .putExtra("prayer", prayer)
                    .putExtra("JewishDay", getSunsetBasedJewishDateInfo().getJewishCalendar().getJewishDayOfMonth())
                    .putExtra("JewishMonth", getSunsetBasedJewishDateInfo().getJewishCalendar().getJewishMonth())
                    .putExtra("JewishYear", getSunsetBasedJewishDateInfo().getJewishCalendar().getJewishYear())
                    .putExtra("masechtas", selectedMasechtot)
                    .putExtra("itemsForMeyinShalosh", selectedShaloshItems)
                    .putExtra("isNightTikkunChatzot", isNightTikkunChatzot)
                    .putExtra("isAfterChatzot", currentZmanimCalendar.isNowAfterHalachicSolarMidnight());// only used for kriat shema she'al hamita
            if (forNextDay) {
                mJewishDateInfo.back();
            }
            if (prayer.equals(getString(R.string.mincha)) && isMinchaAfterSunsetBeforeTzeit) {
                currentJewishDateInfo.forward();
            }
            if (prayer.equals(getString(R.string.arvit)) && isArvitAfterPlagBeforeSunset) {
                currentJewishDateInfo.back();
            }

            if ((getSunsetBasedJewishDateInfo().getJewishCalendar().getYomTovIndex() == JewishCalendar.PURIM ||
                    getSunsetBasedJewishDateInfo().getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM)
                    && !prayer.equals(getString(R.string.birchat_levana))
                    && !prayer.equals(getString(R.string.tefilat_haderech))
                    && !prayer.equals(getString(R.string.seder_siyum_masechet))
                    && !prayer.equals(getString(R.string.tikkun_chatzot))
                    && !prayer.equals(getString(R.string.kriatShema))) {// if the prayer is dependant on isMukafChoma, we ask the user
                SharedPreferences.Editor sharedPreferences = requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.are_you_in_a_walled_mukaf_choma_city)
                        .setMessage(R.string.are_you_located_in_a_walled_mukaf_choma_city_from_the_time_of_yehoshua_bin_nun)
                        .setPositiveButton(getString(R.string.yes) + " (" + getString(R.string.jerusalem) + ")", (dialog, which) -> {
                            sharedPreferences
                                    .putBoolean("isMukafChoma", true)
                                    .putBoolean("isSafekMukafChoma", false).apply();
                            startActivity(intent);
                        })
                        .setNeutralButton(R.string.safek_doubt, (dialog, which) -> {
                            sharedPreferences
                                    .putBoolean("isMukafChoma", false)
                                    .putBoolean("isSafekMukafChoma", true).apply();
                            startActivity(intent);
                        })
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                            sharedPreferences
                                    .putBoolean("isMukafChoma", false)
                                    .putBoolean("isSafekMukafChoma", false).apply();
                            startActivity(intent);
                        })
                        .show();
            } else {
                startActivity(intent);
            }
        }
    }

    private void initMenu() {
        if (materialToolbar == null) {
            return;
        }
        materialToolbar.setTitle(mContext.getString(R.string.show_siddur));
        materialToolbar.setSubtitle(mContext.getString(R.string.short_app_name));
        //materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(mContext, R.drawable.baseline_arrow_back_24)); // if you want to show the back button
        //materialToolbar.setNavigationOnClickListener(v -> finish());
        materialToolbar.getMenu().clear();
        materialToolbar.inflateMenu(R.menu.siddur_chooser_menu);
        materialToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.jerDirection) {
                startActivity(new Intent(mContext, JerusalemDirectionMapsActivity.class));
            }
            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        initMenu();
        if (mActivity.findViewById(R.id.siddur_preferences_container) != null) {
            updateView();
        }
        if (mCalendarButton != null) {
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
        }
    }

    private void setupButtons() {
        setupPreviousDayButton();
        setupCalendarButton();
        setupNextDayButton();
        if (binding != null) {
            dateButtons = binding.dateButtons;
        }
    }

    /**
     * Sets up the previous day button
     */
    private void setupPreviousDayButton() {
        if (binding != null) {
            Button previousDate = binding.prevDay;
            previousDate.setOnClickListener(v -> {
                mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();//just get a calendar object with the same date as the current one
                mCurrentDateShown.add(Calendar.DATE, -1);//subtract one day
                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                updateView();
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
            });
        }
    }

    /**
     * Sets up the next day button
     */
    private void setupNextDayButton() {
        if (binding != null) {
            Button nextDate = binding.nextDay;
            nextDate.setOnClickListener(v -> {
                mCurrentDateShown.add(Calendar.DATE, 1);//add one day
                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                updateView();
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
            });
        }
    }

    /**
     * Setup the calendar button to show a DatePickerDialog with an additional button to switch the calendar to the hebrew one.
     */
    private void setupCalendarButton() {
        if (binding != null) {
            mCalendarButton = binding.calendar;

            mCalendarButton.setOnClickListener(v -> {
                MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                MaterialDatePicker<Long> materialDatePicker = builder
                        .setPositiveButtonText(R.string.ok)
                        .setNegativeButtonText(R.string.switch_calendar)
                        .setSelection(mCurrentDateShown.getTimeInMillis())// can be in local timezone
                        .build();
                materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                    Calendar epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    epoch.setTimeInMillis(selection);
                    mCurrentDateShown.set(
                            epoch.get(Calendar.YEAR),
                            epoch.get(Calendar.MONTH),
                            epoch.get(Calendar.DATE),
                            epoch.get(Calendar.HOUR_OF_DAY),
                            epoch.get(Calendar.MINUTE)
                    );
                    mROZmanimCalendar.setCalendar(mCurrentDateShown);
                    mJewishDateInfo.setCalendar(mCurrentDateShown);
                    updateView();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                });
                DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
                    Calendar mUserChosenDate = Calendar.getInstance();
                    mUserChosenDate.set(year, month, day);
                    mROZmanimCalendar.setCalendar(mUserChosenDate);
                    mJewishDateInfo.setCalendar(mUserChosenDate);
                    mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
                    updateView();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                };
                materialDatePicker.addOnNegativeButtonClickListener(selection -> {
                    HebrewDayMonthYearPickerDialog hdmypd = new HebrewDayMonthYearPickerDialog(materialDatePicker, mActivity.getSupportFragmentManager(), mJewishDateInfo.getJewishCalendar());
                    hdmypd.updateDate(mJewishDateInfo.getJewishCalendar().getGregorianYear(),
                            mJewishDateInfo.getJewishCalendar().getGregorianMonth(),
                            mJewishDateInfo.getJewishCalendar().getGregorianDayOfMonth());
                    hdmypd.setListener(onDateSetListener);
                    hdmypd.show(mActivity.getSupportFragmentManager(), null);
                });
                materialDatePicker.show(mActivity.getSupportFragmentManager(), null);
            });

            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}