package com.ej.rovadiahyosefcalendar.activities.ui;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sElevation;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sHebrewDateFormatter;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSharedPreferences;
import static com.ej.rovadiahyosefcalendar.classes.Utils.inputStreamToString;
import static com.kosherjava.zmanim.AstronomicalCalendar.getTimeOffset;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.GetUserLocationWithMapActivity;
import com.ej.rovadiahyosefcalendar.activities.SetupElevationActivity;
import com.ej.rovadiahyosefcalendar.classes.MakamJCal;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.SecondTreatment;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.classes.ZmanAdapter;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.databinding.DailySummaryBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fills the summary of the current day (location name, date, parsha and chanuka cards) from the shared date state
 * (sROZmanimCalendar / sJewishDateInfo). On phones it is shown at the top of the zmanim tab. On wide screens it is the
 * sidebar next to every tab, and also shows the info rows (Tachanun, Birchat Halevana, prayer changes, etc.).
 */
public class DailySummaryController {

    private final Context mContext;
    private final DailySummaryBinding mBinding;
    private final boolean mShowInfoRows;
    private JSONArray mMakamNames;
    private Runnable mOnChangeDate;

    /**
     * @param context an activity context, since the summary opens dialogs
     * @param showInfoRows true for the wide-screen sidebar, which shows the info rows in their own card
     */
    public DailySummaryController(Context context, DailySummaryBinding binding, boolean showInfoRows) {
        mContext = context;
        mBinding = binding;
        mShowInfoRows = showInfoRows;
        reloadMakamNames();
        mBinding.dailyLocationName.setPaintFlags(mBinding.dailyLocationName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mBinding.dailyCard.setCardElevation(12f);
        mBinding.parshaCard.setCardElevation(12f);
        if (mShowInfoRows) {
            mBinding.infoCard.setCardElevation(12f);
            mBinding.infoRV.setLayoutManager(new LinearLayoutManager(mContext));
            mBinding.infoRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
            if (sSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
                mBinding.infoRV.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            }
        }
        setupClickListeners();
        setSafeSettingsForSpecificManufacturers();
    }

    /**
     * Sets what the "change date" button does in the dialog that opens when the date card is tapped. Without it, the
     * dialog only has a dismiss button.
     */
    public void setOnChangeDateListener(@Nullable Runnable onChangeDate) {
        mOnChangeDate = onChangeDate;
    }

    /**
     * Reloads the makam names, in case the app's language changed.
     */
    public void reloadMakamNames() {
        try {
            mMakamNames = new JSONArray(inputStreamToString(mContext.getResources().openRawResource(Utils.isLocaleHebrew(mContext) ? R.raw.makam_names_he : R.raw.makam_names)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Context getContext() {
        return mContext;
    }

    public void setLocationName(String locationName) {
        mBinding.dailyLocationName.setText(locationName);
    }

    /**
     * Redraws the summary for the date currently in sROZmanimCalendar / sJewishDateInfo.
     * @param infoEntries the info rows to show in the info card, or null to leave them as they are. Only used when showInfoRows is true.
     */
    public void refresh(@Nullable List<ZmanListEntry> infoEntries) {
        String engDate = sROZmanimCalendar.getLocalDate().getDayOfMonth() +
                " " +
                sROZmanimCalendar.getLocalDate().getMonth().getDisplayName(TextStyle.FULL,
                        mContext.getResources()
                        .getConfiguration()
                        .getLocales()
                        .get(0)) +
                ", " +
                sROZmanimCalendar.getLocalDate().getYear();

        if (sROZmanimCalendar.getLocalDate().equals(LocalDate.now())) {
            mBinding.dailyCard.setStrokeColor(ContextCompat.getColor(mContext, R.color.sunset_orange));
            mBinding.dailyCard.setStrokeWidth(6);

            engDate = mContext.getString(R.string.today) + " — " + engDate;
            String hebDate;
            if (sROZmanimCalendar.getSunsetBasedOnElevationSetting() != null && Instant.now().isAfter(sROZmanimCalendar.getSunsetBasedOnElevationSetting())) {
                hebDate = mContext.getString(R.string.post_sunset_date);
            } else {
                hebDate = mContext.getString(R.string.pre_sunset_date);
            }
            hebDate += " " + sJewishDateInfo.getJewishCalendar().currentToString(sROZmanimCalendar);
            mBinding.topDate.setText(engDate);
            mBinding.bottomDate.setText(hebDate);
        } else {
            mBinding.dailyCard.setStrokeColor(ContextCompat.getColor(mContext, R.color.cardview_border));
            mBinding.dailyCard.setStrokeWidth(3);
            mBinding.topDate.setText(engDate);
            mBinding.bottomDate.setText(sJewishDateInfo.getJewishCalendar().toString());
        }
        if (Utils.isLocaleHebrew(mContext)) {
            CharSequence top = mBinding.topDate.getText();
            CharSequence bottom = mBinding.bottomDate.getText();
            mBinding.topDate.setText(bottom);
            mBinding.bottomDate.setText(top);
        }
        mBinding.weekday.setText(Utils.isLocaleHebrew(mContext) ?
                sROZmanimCalendar.getLocalDate().getDayOfWeek().getDisplayName(TextStyle.FULL, mContext.getResources()
                        .getConfiguration()
                        .getLocales()
                        .get(0))
                :
                sROZmanimCalendar.getLocalDate().getDayOfWeek().getDisplayName(TextStyle.FULL, mContext
                                .getResources()
                                .getConfiguration()
                                .getLocales()
                                .get(0))
                        + " / " +
                        sROZmanimCalendar.getLocalDate().getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale.Builder()
                                .setLanguage("he")
                                .setRegion("IL")
                                .build()));
        mBinding.parsha.setText(sJewishDateInfo.getThisWeeksParsha());
        CharSequence haftara = sJewishDateInfo.getThisWeeksHaftarah();
        if (haftara.length() == 0) {
            mBinding.haftaraLayout.setVisibility(View.GONE);
            mBinding.makamLayout.setVisibility(View.GONE);
        } else {
            mBinding.haftaraLayout.setVisibility(View.VISIBLE);
            mBinding.haftara.setText(haftara);
            mBinding.makamLayout.setVisibility(View.VISIBLE);
        }

        try {
            Map<String, List<MakamJCal.Makam>> shabbatMakam = sJewishDateInfo.getThisWeeksMakam();
            if (shabbatMakam.containsKey("GABRIEL A SHREM 1964 SUHV")) {
                setMakam(shabbatMakam, "GABRIEL A SHREM 1964 SUHV");
            } else if (shabbatMakam.containsKey("ADES: 24793")) {
                setMakam(shabbatMakam, "ADES: 24793");
            } else if (shabbatMakam.containsKey("Eliahou Yaaqob DWECK-KESAR")) {
                setMakam(shabbatMakam, "Eliahou Yaaqob DWECK-KESAR");
            } else {
                mBinding.makamLayout.setVisibility(View.GONE);
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
            mBinding.makamLayout.setVisibility(View.GONE);
        }
        if (sJewishDateInfo.getJewishCalendar().getDayOfChanukah() != 8
                && (sJewishDateInfo.tomorrow().getJewishCalendar().isChanukah() || sJewishDateInfo.getJewishCalendar().isChanukah())) {
            int night = sJewishDateInfo.getJewishCalendar().getDayOfChanukah() + 1;
            if (night == 0) {// edge case for erev chanukah
                night = 1;
            }
            mBinding.nightOfChanuka.setText(new StringBuilder(mContext.getString(R.string.chanukah_night))
                    .append(" ")
                    .append(Utils.isLocaleHebrew(mContext) ? sHebrewDateFormatter.formatHebrewNumber(night) : night));
            mBinding.lchatchilaLightingTime.setPaintFlags(mBinding.nightOfChanuka.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);// underline
            mBinding.lchatchilaLightingTime.setText(new StringBuilder(mContext.getString(R.string.ideal))
                    .append(" ")
                    .append(Utils.formatZmanTime(mContext, sROZmanimCalendar.getTzeit(), SecondTreatment.ROUND_LATER))
                    .append(" - ")
                    .append(Utils.formatZmanTime(mContext, getTimeOffset(sROZmanimCalendar.getTzeit(), Duration.of(30, ChronoUnit.MINUTES)), SecondTreatment.ROUND_EARLIER)));
            ROZmanimCalendar tomorrow = sROZmanimCalendar.getCopy();
            tomorrow.setLocalDate(tomorrow.getLocalDate().plusDays(1));
            String alotHashacharForTomorrow = Utils.formatZmanTime(mContext, tomorrow.getAlotHashachar(), SecondTreatment.ROUND_EARLIER);
            mBinding.fullWindowLightingTime.setText(new StringBuilder(mContext.getString(R.string.earliest))
                    .append(" ")
                    .append(Utils.formatZmanTime(mContext, sROZmanimCalendar.getPlagHaminchaYalkutYosef(), SecondTreatment.ROUND_LATER))
                    .append(" - ")
                    .append(mContext.getString(R.string.latest))
                    .append(" ")
                    .append(alotHashacharForTomorrow));
            mBinding.chanukaCard.setVisibility(View.VISIBLE);
            mBinding.chanukaCard.setCardElevation(12f);
        } else {
            mBinding.chanukaCard.setVisibility(View.GONE);
        }
        if (mShowInfoRows && infoEntries != null) {
            mBinding.infoRV.setAdapter(new ZmanAdapter(mContext, infoEntries, () -> {}));
            mBinding.infoCard.setVisibility(infoEntries.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Applies the user's custom card background and text colors, the same way the zmanim tab does.
     */
    public void applyThemeColors() {
        if (!sSharedPreferences.getBoolean("useImage", false)) {
            int cardColor = sSharedPreferences.getBoolean("customBackgroundColor", false) && !sSharedPreferences.getBoolean("useDefaultBackgroundColor", false)
                    ? sSharedPreferences.getInt("bColor", 0x32312C)
                    : ContextCompat.getColor(mContext, R.color.cardview_background);
            mBinding.dailyCard.setCardBackgroundColor(cardColor);
            mBinding.parshaCard.setCardBackgroundColor(cardColor);
            mBinding.infoCard.setCardBackgroundColor(cardColor);
        }
        int textColor = sSharedPreferences.getBoolean("customTextColor", false) ? sSharedPreferences.getInt("tColor", 0xFFFFFFFF) : ContextCompat.getColor(mContext, R.color.textColor);
        mBinding.dailyLocationName.setTextColor(textColor);
        mBinding.weekday.setTextColor(textColor);
        mBinding.topDate.setTextColor(textColor);
        mBinding.bottomDate.setTextColor(textColor);
        mBinding.parsha.setTextColor(textColor);
        mBinding.haftara.setTextColor(textColor);
        mBinding.makam.setTextColor(textColor);
        mBinding.nightOfChanuka.setTextColor(textColor);
        mBinding.lchatchilaLightingTime.setTextColor(textColor);
        mBinding.fullWindowLightingTime.setTextColor(textColor);
    }

    private void setupClickListeners() {
        mBinding.dailyLocationName.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
            .setTitle(mContext.getString(R.string.location_info_for) + " " + sROZmanimCalendar.getGeoLocation().getLocationName())
            .setMessage(mContext.getString(R.string.location_name) + " " + sROZmanimCalendar.getGeoLocation().getLocationName() + "\n" +
                mContext.getString(R.string.latitude) + " " + sROZmanimCalendar.getGeoLocation().getLatitude() + "\n" +
                mContext.getString(R.string.longitude) + " " + sROZmanimCalendar.getGeoLocation().getLongitude() + "\n" +
                mContext.getString(R.string.elevation) + " " +
                (sSharedPreferences.getBoolean("useElevation", true) ?
                    sSharedPreferences.getString("elevation" + sROZmanimCalendar.getGeoLocation().getLocationName(), "0") : "0")
                + " " + mContext.getString(R.string.meters) + "\n" +
                mContext.getString(R.string.time_zone) + sROZmanimCalendar.getGeoLocation().getZoneId().getId())
            .setPositiveButton(R.string.share, ((dialog, which) -> {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_TEXT, "https://royzmanim.com/calendar?locationName=" + sCurrentLocationName.replace(" ", "+").replace(",", "%2C") + "&lat=" + sLatitude + "&long=" + sLongitude + "&elevation=" + sElevation + "&timeZone=" + sCurrentTimeZoneID);
                Utils.startActivitySafely(mContext, Intent.createChooser(sendIntent, mContext.getString(R.string.share)));
            }))
            .setNeutralButton(R.string.change_location, (dialog, which) -> mContext.startActivity(new Intent(mContext, GetUserLocationWithMapActivity.class).putExtra("loneActivity", true)))
            .setNegativeButton(mContext.getString(R.string.setup_elevation), (dialog, which) -> mContext.startActivity(new Intent(mContext, SetupElevationActivity.class).putExtra("loneActivity", true)))
            .show());
        mBinding.parsha.setOnClickListener(l -> {
            String title = mBinding.parsha.getText().toString();
            if (!title.equals("No Weekly Parsha") && !title.equals("אין פרשת שבוע")) {
                String parsha;
                if (title.equals("לך לך")
                        || title.equals("חיי שרה")
                        || title.equals("כי תשא")
                        || title.equals("אחרי מות")
                        || title.equals("שלח לך")
                        || title.equals("כי תצא")
                        || title.equals("כי תבוא")
                        || title.equals("וזאת הברכה ")) {
                    parsha = title;// ugly, but leave the first word and second word in these cases
                } else {
                    if (title.contains("אחרי מות")) {// edge case for Acharei Mot Kedoshim
                        parsha = "אחרי מות";
                    } else {
                        parsha = title.split(" ")[0];//get first word
                    }
                }
                String parshaLink = "https://www.sefaria.org/" + parsha;
                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext);
                dialogBuilder.setTitle(mContext.getString(R.string.open_sefaria_link_for) + parsha + "?");
                dialogBuilder.setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_weekly_parsha);
                dialogBuilder.setPositiveButton(mContext.getString(R.string.ok), (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(parshaLink));
                    Utils.startActivitySafely(mContext, intent);
                });
                dialogBuilder.setNegativeButton(mContext.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
                dialogBuilder.show();
            }
        });
        mBinding.dailyCard.setOnClickListener(l -> {
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext)
                    .setTitle(sJewishDateInfo.hebrewDateFormatter.formatMonth(sJewishDateInfo.getJewishCalendar()))
                    .setMessage(getMonthMessage())
                    .setNegativeButton(R.string.dismiss, (dialogInterface, i) -> dialogInterface.dismiss());
            if (mOnChangeDate != null) {
                dialogBuilder.setPositiveButton(R.string.change_date, ((dialogInterface, i) -> mOnChangeDate.run()));
            }
            dialogBuilder.show();
        });
    }

    private void setMakam(Map<String, List<MakamJCal.Makam>> shabbatMakam, String book) throws JSONException {
        List<MakamJCal.Makam> makamObj = shabbatMakam.get(book);
        StringBuilder makamText = new StringBuilder(mContext.getString(R.string.makam));
        if (makamObj != null) {
            for (int i = 0; i < makamObj.size(); i++) {
                makamText.append(mMakamNames.get(makamObj.get(i).ordinal())).append(" ");
            }
        }

        String finalMakamText = makamText.toString();

        mBinding.makam.setText(finalMakamText);
    }

    private String getMonthMessage() {
        switch (sJewishDateInfo.getJewishCalendar().getJewishMonth()) {
            case JewishCalendar.TISHREI -> {
                return mContext.getString(R.string.tishri_desc);
            }
            case JewishCalendar.CHESHVAN -> {
                return mContext.getString(R.string.cheshvan_desc);
            }
            case JewishCalendar.KISLEV -> {
                return mContext.getString(R.string.kislev_desc);
            }
            case JewishCalendar.TEVES -> {
                return mContext.getString(R.string.tevet_desc);
            }
            case JewishCalendar.SHEVAT -> {
                return mContext.getString(R.string.shevat_desc);
            }
            case JewishCalendar.ADAR -> {
                return mContext.getString(R.string.adar_desc);
            }
            case JewishCalendar.ADAR_II -> {
                return mContext.getString(R.string.adar_ii_desc);
            }
            case JewishCalendar.NISSAN -> {
                return mContext.getString(R.string.nissan_desc);
            }
            case JewishCalendar.IYAR -> {
                return mContext.getString(R.string.iyar_desc);
            }
            case JewishCalendar.SIVAN -> {
                return mContext.getString(R.string.sivan_desc);
            }
            case JewishCalendar.TAMMUZ -> {
                return mContext.getString(R.string.tammuz_desc);
            }
            case JewishCalendar.AV -> {
                return mContext.getString(R.string.av_desc);
            }
            case JewishCalendar.ELUL -> {
                return mContext.getString(R.string.elul_desc);
            }
            default -> {
                return "I don't know how you got here... There should be no new Hebrew months...";
            }
        }
    }

    /**
     * Some manufacturers' phones struggle with auto-sized text, so it is turned off for them (see ZmanimFragment).
     */
    private void setSafeSettingsForSpecificManufacturers() {
        if (Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {
            TextView[] textViews = {mBinding.weekday, mBinding.topDate, mBinding.bottomDate, mBinding.parsha, mBinding.haftara, mBinding.makam, mBinding.nightOfChanuka, mBinding.lchatchilaLightingTime, mBinding.fullWindowLightingTime};
            for (TextView textView : textViews) {
                TextViewCompat.setAutoSizeTextTypeWithDefaults(
                        textView,
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            }
        }
    }
}
