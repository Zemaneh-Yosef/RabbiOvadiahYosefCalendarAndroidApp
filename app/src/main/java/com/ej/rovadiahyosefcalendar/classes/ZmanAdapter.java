package com.ej.rovadiahyosefcalendar.classes;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sNextUpcomingZman;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainActivity;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ZmanAdapter extends RecyclerView.Adapter<ZmanAdapter.ZmanViewHolder> {

    private final List<ZmanListEntry> zmanim;
    private final SharedPreferences mSharedPreferences;
    private final Context context;
    private final AlertDialog.Builder dialogBuilder;
    private final DateFormat zmanimFormat;
    private final DateFormat roundUpFormat;
    private final boolean roundUpRt;

    public ZmanAdapter(Context context, List<ZmanListEntry> zmanim) {
        this.zmanim = zmanim;
        this.context = context;
        mSharedPreferences = this.context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ShowSeconds", false)) {
            zmanimFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
        roundUpRt = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("RoundUpRT", false);
        roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        roundUpFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
        dialogBuilder = new AlertDialog.Builder(this.context);
        dialogBuilder.setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());
        dialogBuilder.create();
    }

    @NotNull
    @Override
    public ZmanViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.daily_entry, parent, false);
        return new ZmanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ZmanViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        if (zmanim.get(position).isZman()) {
            holder.mLeftTextView.setText(zmanim.get(position).getTitle());//zman name

            String zmanTime = "➤";
            if (!zmanim.get(position).getZman().equals(sNextUpcomingZman)) {
                zmanTime = "";//remove arrow
            }

            if (zmanim.get(position).isRTZman() && roundUpRt) {
                zmanTime += roundUpFormat.format(checkNull(zmanim.get(position).getZman()));
            } else {
                zmanTime += zmanimFormat.format(checkNull(zmanim.get(position).getZman()));
            }
            holder.mRightTextView.setText(zmanTime);
        } else {
            holder.mMiddleTextView.setText(zmanim.get(position).getTitle());
        }

        holder.itemView.setOnClickListener(v -> {
            if (!MainActivity.sShabbatMode && PreferenceManager.getDefaultSharedPreferences(context).getBoolean("showZmanDialogs", true)) {
                if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
                    checkHebrewZmanimForDialog(position);
                } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
                    checkTranslatedEnglishZmanimForDialog(position);
                } else {
                    checkEnglishZmanimForDialog(position);
                }

                if (zmanim.get(position).getTitle().contains("וּלְכַפָּרַת פֶּשַׁע")) {
                    showUlChaparatPeshaDialog();
                }

                if (zmanim.get(position).getTitle().contains("Elevation")) {
                    showElevationDialog();
                }

                if (zmanim.get(position).getTitle().contains("Tekufa")) {
                    showTekufaDialog();
                }

                if (zmanim.get(position).getTitle().contains("Tachanun") || zmanim.get(position).getTitle().contains("צדקתך")) {
                    showTachanunDialog();
                }
            }
        });

        if (mSharedPreferences.getBoolean("useImage", false)) {
            holder.itemView.setBackgroundResource(0);
        } else if (mSharedPreferences.getBoolean("customBackgroundColor", false) &&
                !mSharedPreferences.getBoolean("useDefaultBackgroundColor", false)) {
            holder.itemView.setBackgroundColor(mSharedPreferences.getInt("bColor", 0x32312C));
        }

        if (mSharedPreferences.getBoolean("customTextColor", false)) {
            holder.mLeftTextView.setTextColor(mSharedPreferences.getInt("tColor", 0xFFFFFFFF));
            holder.mMiddleTextView.setTextColor(mSharedPreferences.getInt("tColor", 0xFFFFFFFF));
            holder.mRightTextView.setTextColor(mSharedPreferences.getInt("tColor", 0xFFFFFFFF));
        }
    }

    @Override
    public int getItemCount() {
        return zmanim.size();
    }

    static class ZmanViewHolder extends RecyclerView.ViewHolder {

        TextView mRightTextView;
        TextView mMiddleTextView;
        TextView mLeftTextView;

        public ZmanViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            setIsRecyclable(false);
            mLeftTextView = itemView.findViewById(R.id.zmanLeftTextView);
            mLeftTextView.setTypeface(Typeface.DEFAULT_BOLD);
            mMiddleTextView = itemView.findViewById(R.id.zmanMiddleTextView);
            mRightTextView = itemView.findViewById(R.id.zmanRightTextView);
        }
    }

    /**
     * This is a simple convenience method to check if the given date is null or not. If the date is not null,
     * it will return exactly what was given. However, if the date is null, it will change the date to a string that says "N/A" (Not Available).
     * @param date the date object to check if it is null
     * @return the given date if not null or a string if null
     */
    private Object checkNull(Object date) {
        if (date != null) {
            return date;
        } else {
            return "N/A";
        }
    }

    private void checkHebrewZmanimForDialog(int position) {
        if (zmanim.get(position).getTitle().contains("\u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8")) {
            showDawnDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05D8\u05DC\u05D9\u05EA \u05D5\u05EA\u05E4\u05D9\u05DC\u05D9\u05DF")) {
            showEarliestTalitTefilinDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05D4\u05E0\u05E5")) {
            showSunriseDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05D0\u05DB\u05D9\u05DC\u05EA \u05D7\u05DE\u05E5")) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05D1\u05D9\u05E2\u05D5\u05E8 \u05D7\u05DE\u05E5")) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05E9\u05DE\u05E2 \u05DE\u05D2\"\u05D0")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).getTitle().contains("\u05E9\u05DE\u05E2 \u05D2\u05E8\"\u05D0")) {
            showShmaGRADialog();
        } else if (zmanim.get(position).getTitle().contains("\u05D1\u05E8\u05DB\u05D5\u05EA \u05E9\u05DE\u05E2")) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05D7\u05E6\u05D5\u05EA")) {
            showChatzotDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05DE\u05E0\u05D7\u05D4 \u05D2\u05D3\u05D5\u05DC\u05D4")) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05DE\u05E0\u05D7\u05D4 \u05E7\u05D8\u05E0\u05D4")) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05E4\u05DC\u05D2 \u05D4\u05DE\u05E0\u05D7\u05D4")) {
            showPlagDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05D4\u05D3\u05DC\u05E7\u05EA \u05E0\u05E8\u05D5\u05EA")) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05E9\u05E7\u05D9\u05E2\u05D4")) {
            showShkiaDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05E6\u05D0\u05EA \u05D4\u05DB\u05D5\u05DB\u05D1\u05D9\u05DD")) {
            showTzaitDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA \u05DC\u05D7\u05D5\u05DE\u05E8\u05D4")) {
            showTzaitTaanitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA")) {
            showTzaitTaanitDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05E6\u05D0\u05EA \u05E9\u05D1\u05EA/\u05D7\u05D2")
                ||zmanim.get(position).getTitle().contains("\u05E6\u05D0\u05EA \u05E9\u05D1\u05EA")
                ||zmanim.get(position).getTitle().contains("\u05E6\u05D0\u05EA \u05D7\u05D2")) {
            showTzaitShabbatDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD")) {
            showRTDialog();
        } else if (zmanim.get(position).getTitle().contains("\u05D7\u05E6\u05D5\u05EA \u05DC\u05D9\u05DC\u05D4")) {
            showChatzotLaylaDialog();
        }
    }

    private void checkTranslatedEnglishZmanimForDialog(int position) {
        if (zmanim.get(position).getTitle().contains("Dawn")) {
            showDawnDialog();
        } else if (zmanim.get(position).getTitle().contains("Earliest Talit/Tefilin")) {
            showEarliestTalitTefilinDialog();
        } else if (zmanim.get(position).getTitle().contains("Sunrise")) {
            showSunriseDialog();
        } else if (zmanim.get(position).getTitle().contains("Achilat Chametz")) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("Biur Chametz")) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("Shma MG\"A")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).getTitle().contains("Shma GR\"A")) {
            showShmaGRADialog();
        } else if (zmanim.get(position).getTitle().contains("Brachot Shma")) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).getTitle().contains("Mid-Day")) {
            showChatzotDialog();
        } else if (zmanim.get(position).getTitle().contains("Mincha Gedola")) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).getTitle().contains("Mincha Ketana")) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).getTitle().contains("Plag HaMincha")) {
            showPlagDialog();
        } else if (zmanim.get(position).getTitle().contains("Candle Lighting")) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).getTitle().contains("Sunset")) {
            showShkiaDialog();
        } else if (zmanim.get(position).getTitle().contains("Nightfall")) {
            showTzaitDialog();
        } else if (zmanim.get(position).getTitle().contains("Fast Ends (Stringent)")) {
            showTzaitTaanitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains("Fast Ends")) {
            showTzaitTaanitDialog();
        } else if (zmanim.get(position).getTitle().contains("Shabbat/Chag Ends")
                || zmanim.get(position).getTitle().contains("Shabbat Ends")
                || zmanim.get(position).getTitle().contains("Chag Ends")) {
            showTzaitShabbatDialog();
        } else if (zmanim.get(position).getTitle().contains("Rabbeinu Tam")) {
            showRTDialog();
        } else if (zmanim.get(position).getTitle().contains("Midnight")) {
            showChatzotLaylaDialog();
        }
    }

    private void checkEnglishZmanimForDialog(int position) {
        if (zmanim.get(position).getTitle().contains("Alot Hashachar")) {
            showDawnDialog();
        } else if (zmanim.get(position).getTitle().contains("Earliest Talit/Tefilin")) {
            showEarliestTalitTefilinDialog();
        } else if (zmanim.get(position).getTitle().contains("HaNetz")) {
            showSunriseDialog();
        } else if (zmanim.get(position).getTitle().contains("Achilat Chametz")) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("Biur Chametz")) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("Shma MG\"A")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).getTitle().contains("Shma GR\"A")) {
            showShmaGRADialog();
        } else if (zmanim.get(position).getTitle().contains("Brachot Shma")) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).getTitle().contains("Chatzot")) {
            showChatzotDialog();
        } else if (zmanim.get(position).getTitle().contains("Mincha Gedola")) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).getTitle().contains("Mincha Ketana")) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).getTitle().contains("Plag HaMincha")) {
            showPlagDialog();
        } else if (zmanim.get(position).getTitle().contains("Candle Lighting")) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).getTitle().contains("Shkia")) {
            showShkiaDialog();
        } else if (zmanim.get(position).getTitle().contains("Tzait Hacochavim")) {
            showTzaitDialog();
        } else if (zmanim.get(position).getTitle().contains("Tzait Taanit L'Chumra")) {
            showTzaitTaanitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains("Tzait Taanit")) {
            showTzaitTaanitDialog();
        } else if (zmanim.get(position).getTitle().contains("Tzait Shabbat/Chag")
                || zmanim.get(position).getTitle().contains("Tzait Chag")
                || zmanim.get(position).getTitle().contains("Tzait Shabbat")) {
            showTzaitShabbatDialog();
        } else if (zmanim.get(position).getTitle().contains("Rabbeinu Tam")) {
            showRTDialog();
        } else if (zmanim.get(position).getTitle().contains("Chatzot Layla")) {
            showChatzotLaylaDialog();
        }
    }

    private void showDawnDialog() {
        dialogBuilder.setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("In Tanach this time is called Alot HaShachar (בראשית לב:כה), whereas in the gemara it is called Amud HaShachar.\n\n" +
                        "This is the time when the day begins according to halacha. " +
                        "Most mitzvot (commandments), Arvit for example, that take place at night are not allowed " +
                        "to be done after this time.\nAfter this time, mitzvot that must be done in the daytime are " +
                        "allowed to be done B'dieved (after the fact) or B'shaat hadachak (in a time of need). However, one should ideally wait " +
                        "until sunrise to do them L'chatchila (optimally).\n\n" +
                        "This time is calculated as 72 zmaniyot/seasonal minutes (according to the GR\"A) before sunrise. Both sunrise and sunset " +
                        "have elevation included.")
                .show();
    }

    private void showEarliestTalitTefilinDialog() {
        dialogBuilder.setTitle("Earliest Talit/Tefilin - \u05D8\u05DC\u05D9\u05EA \u05D5\u05EA\u05E4\u05D9\u05DC\u05D9\u05DF - Misheyakir")
                .setMessage("Misheyakir (literally \"when you recognize\") is the time when a person can distinguish between blue and white. " +
                        "The gemara (ברכות ט) explains that when a person can distinguish between the blue (techelet) and white strings " +
                        "of their tzitzit, that is the earliest time a person can put on their talit and tefilin for shacharit.\n\n" +
                        "This time is calculated as 6 zmaniyot/seasonal minutes (according to the GR\"A) after Alot HaShachar (Dawn).\n\n" +
                        "Note: This time is only for people who need to go to work or leave early in the morning to travel, however, normally a " +
                        "person should put on his talit/tefilin 60 regular minutes (and in the winter 50 regular minutes) before sunrise.")
                .show();
    }

    private void showSunriseDialog() {
        dialogBuilder.setTitle("Sunrise - \u05D4\u05E0\u05E5 - HaNetz")
                .setMessage("This is the earliest time when all mitzvot (commandments) that are to be done during the daytime are allowed to be " +
                        "performed L'chatchila (optimally). Halachic sunrise is defined as the moment when the top edge of the sun appears on the " +
                        "horizon while rising. Whereas, the gentiles define sunrise as the moment when the sun is halfway through the horizon. " +
                        "This halachic sunrise is called mishor (sea level) sunrise and it is what many jews rely on when praying for Netz.\n\n" +
                        "However, it should be noted that the Shulchan Aruch writes in Orach Chayim 89:1, \"The mitzvah of shacharit starts at " +
                        "Netz, like it says in the pasuk/verse, 'יראוך עם שמש'\". Based on this, the poskim write that a person should wait until " +
                        "the sun is VISIBLE to say shacharit. In Israel, the Ohr HaChaim calendar uses a table of sunrise times from the " +
                        "luach/calendar 'לוח ביכורי יוסף' (Luach Bechoray Yosef) each year. These times were made by Chaim Keller, creator of the " +
                        "ChaiTables website. Ideally, you should download these VISIBLE sunrise times from his website with the capability of " +
                        "this app to use for the year. However, if you did not download the times, you will see 'Mishor' or 'Sea Level' sunrise instead.")
                .show();
    }

    private void showAchilatChametzDialog() {
        dialogBuilder.setTitle("Eating Chametz - \u05D0\u05DB\u05D9\u05DC\u05EA \u05D7\u05DE\u05E5 - Achilat Chametz")
                .setMessage("This is the latest time a person can eat chametz.\n\n" +
                        "This is calculated as 4 zmaniyot/seasonal hours, according to the Magen Avraham, after Alot HaShachar (Dawn) with " +
                        "elevation included. Since Chametz is a mitzvah from the torah, we are stringent and we use the Magen Avraham's time to " +
                        "calculate the last time a person can eat chametz.")
                .show();
    }

    private void showBiurChametzDialog() {
        dialogBuilder.setTitle("Burning Chametz - \u05D1\u05D9\u05E2\u05D5\u05E8 \u05D7\u05DE\u05E5 - Biur Chametz")
                .setMessage("This is the latest time a person can own chametz before pesach begins. You should get rid of all chametz in your " +
                        "possession by this time.\n\n" +
                        "This is calculated as 5 zmaniyot/seasonal hours, according to the MG\"A, after Alot HaShachar (Dawn) with " +
                        "elevation included.")
                .show();
    }

    private void showShmaMGADialog() {
        dialogBuilder.setTitle("Latest time for Shma (MG\"A) - \u05E9\u05DE\u05E2 \u05DE\u05D2\"\u05D0 - Shma MG\"A")
                .setMessage("This is the latest time a person can fulfill his obligation to say Shma everyday according to the Magen Avraham.\n\n" +
                        "The Magen Avraham/Terumat HeDeshen calculate this time as 3 zmaniyot/seasonal hours after Alot HaShachar (Dawn). " +
                        "They calculate a zmaniyot/seasonal hour by taking the time between Alot HaShachar (Dawn) and Tzeit Hachocavim (Nightfall) " +
                        "of Rabbeinu Tam and divide it into 12 equal parts.")
                .show();
    }

    private void showShmaGRADialog() {
        dialogBuilder.setTitle("Latest time for Shma (GR\"A) - \u05E9\u05DE\u05E2 \u05D2\u05E8\"\u05D0 - Shma GR\"A")
                .setMessage("This is the latest time a person can fulfill his obligation to say Shma everyday according to the GR\"A " +
                        "(HaGaon Rabbeinu Eliyahu)" +
                        "\n\n" +
                        "The GR\"A calculates this time as 3 zmaniyot/seasonal hours after sunrise (elevation included). " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts.")
                .show();
    }

    private void showBrachotShmaDialog() {
        dialogBuilder.setTitle("Brachot Shma - \u05D1\u05E8\u05DB\u05D5\u05EA \u05E9\u05DE\u05E2 - Brachot Shma")
                .setMessage("This is the latest time a person can say the Brachot Shma according to the GR\"A. However, a person can still say " +
                        "Pisukei D'Zimra until Chatzot.\n\n" +
                        "The GR\"A calculates this time as 4 zmaniyot/seasonal hours after sunrise (elevation included). " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts.")
                .show();
    }

    private void showChatzotDialog() {
        dialogBuilder.setTitle("Mid-Day - \u05D7\u05E6\u05D5\u05EA - Chatzot")
                .setMessage("This is the middle of the halachic day, when the sun is exactly in the middle of the sky relative to the length of the" +
                        " day. It should be noted, that the sun can only be directly above every person, such that they don't even have shadows, " +
                        "in the Tropic of Cancer and the Tropic of Capricorn. Everywhere else, the sun will be at an angle even in the middle of " +
                        "the day.\n\n" +
                        "After this time, you can no longer say the Amidah prayer of Shacharit, and you should preferably say Musaf before this " +
                        "time.\n\n" +
                        "This time is calculated as 6 zmaniyot/seasonal hours after sunrise. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts.\n\n")
                .show();
    }

    private void showMinchaGedolaDialog() {
        dialogBuilder.setTitle("Earliest Mincha - \u05DE\u05E0\u05D7\u05D4 \u05D2\u05D3\u05D5\u05DC\u05D4 - Mincha Gedolah")
                .setMessage("Mincha Gedolah, literally \"Greater Mincha\", is the earliest time a person can say Mincha. " +
                        "It is also the preferred time a person should say Mincha according to some poskim.\n\n" +
                        "It is called Mincha Gedolah because there is a lot of time left until sunset.\n\n" +
                        "A person should ideally start saying Korbanot AFTER this time.\n\n" +
                        "This time is calculated as 30 regular minutes after Chatzot (Mid-Day). However, if the zmaniyot/seasonal minutes are longer," +
                        " we use those minutes instead to be stringent. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.")
                .show();
    }

    private void showMinchaKetanaDialog() {
        dialogBuilder.setTitle("Mincha Ketana - \u05DE\u05E0\u05D7\u05D4 \u05E7\u05D8\u05E0\u05D4")
                .setMessage("Mincha Ketana, literally \"Lesser Mincha\", is the most preferred time a person can say Mincha according to some poskim.\n\n" +
                        "It is called Mincha Ketana because there is less time left until sunset.\n\n" +
                        "This time is calculated as 9 and a half zmaniyot/seasonal hours after sunrise. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.")
                .show();
    }

    private void showPlagDialog() {
        dialogBuilder.setTitle("Plag HaMincha - \u05E4\u05DC\u05D2 \u05D4\u05DE\u05E0\u05D7\u05D4")
                .setMessage("Plag HaMincha, literally \"Half of Mincha\", is the midpoint between Mincha Ketana and sunset. Since Mincha Ketana is " +
                        "2 and a half hours before sunset, Plag is half of that at an hour and 15 minutes before sunset.\n" +
                        "You can start saying arvit by this time according to Rabbi Yehuda in (ברכות כ'ו ע'א).\n\n" +
                        "A person should not accept shabbat before this time as well.\n\n" +
                        "This time is usually calculated as 10 and 3/4th zmaniyot/seasonal hours after sunrise, however, yalkut yosef says to " +
                        "calculate it as 1 hour and 15 zmaniyot/seasonal minutes before tzeit. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.")
                .show();
    }

    private void showCandleLightingDialog() {
        dialogBuilder.setTitle("Candle Lighting - \u05D4\u05D3\u05DC\u05E7\u05EA \u05E0\u05E8\u05D5\u05EA")
                .setMessage("This is the ideal time for a person to light the candles before shabbat/chag starts.\n" +
                        "When there is candle lighting on a day that is Yom tov/Shabbat before another day that is Yom tov, " +
                        "the candles are lit after Tzeit/Nightfall. However, if the next day is Shabbat, the candles are lit at their usual time.\n\n" +
                        "This time is calculated as " +
                        PreferenceManager.getDefaultSharedPreferences(context).getString("CandleLightingOffset", "20") + " " +
                        "regular minutes before sunset (elevation included).\n\n")
                .show();
    }

    private void showShkiaDialog() {
        dialogBuilder.setTitle("Sunset - \u05E9\u05E7\u05D9\u05E2\u05D4 - Shkia")
                .setMessage("This is the time of the day that the day starts to transition into the next day according to halacha.\n\n" +
                        "Halachic sunset is defined as the moment when the top edge of the sun disappears on the " +
                        "horizon while setting (elevation included). Whereas, the gentiles define sunset as the moment when the sun is halfway " +
                        "through the horizon.\n\n" +
                        "Immediately after the sun sets, Bein Hashmashot/twilight starts according to the Geonim, however, according to Rabbeinu Tam " +
                        "the sun continues to set for another 58.5 minutes and only after that Bein Hashmashot starts for another 13.5 minutes.\n\n" +
                        "It should be noted that many poskim, like the Mishna Berura, say that a person should ideally say mincha BEFORE sunset " +
                        "and not before Tzeit/Nightfall.\n\n" +
                        "Most mitzvot that are to be done during the day should ideally be done before this time.")
                .show();
    }

    private void showTzaitDialog() {
        dialogBuilder.setTitle("Nightfall - \u05E6\u05D0\u05EA \u05D4\u05DB\u05D5\u05DB\u05D1\u05D9\u05DD - Tzeit Hacochavim")
                .setMessage("Tzeit/Nightfall is the time when the next halachic day starts after Bein Hashmashot/twilight finishes.\n\n" +
                        "This is the latest time a person can say Mincha according Rav Ovadiah Yosef Z\"TL. A person should start mincha at " +
                        "least 2 minutes before this time.\n\n" +
                        "This time is calculated as 13 and a half zmaniyot/seasonal minutes after sunset (elevation included). " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.")
                .show();
    }

    private void showTzaitTaanitDialog() {
        dialogBuilder.setTitle("Fast Ends - \u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA - Tzeit Taanit")
                .setMessage("This is the time that the fast/taanit ends.\n\n" +
                        "This time is calculated as 20 regular minutes after sunset (elevation included).")
                .show();
    }

    private void showTzaitTaanitLChumraDialog() {
        dialogBuilder.setTitle("Fast Ends (Stringent) - \u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA \u05DC\u05D7\u05D5\u05DE\u05E8\u05D4 - Tzeit Taanit L'Chumra")
                .setMessage("This is the more stringent time that the fast/taanit ends.\n\n" +
                        "This time is calculated as 30 regular minutes after sunset (elevation included).")
                .show();
    }

    private void showTzaitShabbatDialog() {
        dialogBuilder.setTitle("Shabbat/Chag Ends - \u05E6\u05D0\u05EA \u05E9\u05D1\u05EA/\u05D7\u05D2 - Tzeit Shabbat/Chag")
                .setMessage("This is the time that Shabbat/Chag ends.\n\n" +
                        "Note that there are many customs on when shabbat ends, by default, I set it to 40 regular minutes after sunset (elevation " +
                        "included), however, you can change the time in the settings.\n\n" +
                        "This time is calculated as " +
                        PreferenceManager.getDefaultSharedPreferences(context).getString("EndOfShabbatOffset", "40") + " " +
                        "regular minutes after sunset (elevation included).")
                .show();
    }

    private void showRTDialog() {
        dialogBuilder.setTitle("Rabbeinu Tam - \u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD")
                .setMessage("This time is Tzeit/Nightfall according to Rabbeinu Tam.\n\n" +
                        "Tzeit/Nightfall is the time when the next halachic day starts after Bein Hashmashot/twilight finishes.\n\n" +
                        "This time is calculated as 72 zmaniyot/seasonal minutes after sunset (elevation included). " +
                        "According to Rabbeinu Tam, these 72 minutes are made up of 2 parts. The first part is 58 and a half minutes until the " +
                        "second sunset (see Pesachim 94a and Tosafot there). After the second sunset, there are an additional 13.5 minutes until " +
                        "Tzeit/Nightfall.\n\n" +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute in order " +
                        "to calculate 72 minutes. Another way of calculating this time is by calculating how many minutes are between sunrise and " +
                        "sunset. Take that number and divide it by 10, and then add the result to sunset. The app uses the first method.")
                .show();
    }

    private void showChatzotLaylaDialog() {
        dialogBuilder.setTitle("Midnight - \u05D7\u05E6\u05D5\u05EA \u05DC\u05D9\u05DC\u05D4 - Chatzot Layla")
                .setMessage("This is the middle of the halachic night, when the sun is exactly in the middle of the sky beneath us.\n\n" +
                        "This time is calculated as 6 zmaniyot/seasonal hours after sunset. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts.\n\n")
                .show();
    }

    private void showUlChaparatPeshaDialog() {
        dialogBuilder.setTitle("וּלְכַפָּרַת פֶּשַׁע")
                .setMessage("When Rosh Chodesh happens during a leap year, we add the words, \"וּלְכַפָּרַת פֶּשַׁע\" during Musaf. We only add these words " +
                        "from Tishri until the second month of Adar. However, for the rest of the year and during non leap years we do not say it.")
                .show();
    }

    private void showElevationDialog() {
        dialogBuilder.setTitle("Current Elevation")
                .setMessage("This number represents the amount of elevation that you are applying to your zmanim to see sunrise/sunset in your current city in meters." +
                        " If the number is set to 0, then you are calculating the zmanim by mishor/sea level sunrise and sunset.\n\n" +
                        "There is a debate as to what Rabbi Ovadiah Yosef Z\"TL " +
                        "held about using elevation for zmanim. (See Halacha Berura vol. 14, in Otzrot Yosef (Kuntrus Ki Ba Hashemesh), Siman 6, " +
                        "Perek 21 for an in depth discussion) The Ohr HaChaim calendar uses elevation for their zmanim, however, Rabbi David Yosef " +
                        "Z\"TL holds that the elevation is not used for zmanim.")
                .show();
    }

    private void showTekufaDialog() {
        dialogBuilder.setTitle("Tekufa - Season")
                .setMessage("This is the time that the tekufa (season) changes.\n\nThere are 4 tekufas every year: Tishri (autumn), Tevet (winter), " +
                        "Nissan (spring), and Tammuz (summer). Each Tekufa happens 91.3125 (365.25 / 4) days after the previous Tekufa.\n\n" +
                        "The Achronim write that a person should not drink water when the seasons change. Rabbi Ovadiah Yosef Z\"TL writes " +
                        "(in Halichot Olam, Chelek 7, Page 183, Halacha 8) that a person should not drink water from a half hour before this time " +
                        "till a half hour after this time unless there is a piece of iron in the water.\n\nNOTE: This only applies to water, not " +
                        "to other drinks.")
                .show();
    }

    private void showTachanunDialog() {
        dialogBuilder.setTitle("Tachanun - \u05EA\u05D7\u05E0\u05D5\u05DF")
                .setMessage("Here is a list of days with no tachanun:\n\n" +
                        "Rosh Chodesh\n" +
                        "The entire month of Nissan\n" +
                        "Pesach Sheni (14th of Iyar)\n" +
                        "Lag Ba'Omer\n" +
                        "Rosh Chodesh Sivan until the 12th of Sivan (12th included)\n" +
                        "9th of Av\n" +
                        "15th of Av\n" +
                        "Erev Rosh Hashanah and Rosh Hashanah\n" +
                        "Erev Yom Kippur and Yom Kippur\n" +
                        "From the 11th of Tishrei until the end of Tishrei\n" +
                        "All of Chanukah\n" +
                        "15th of Shevat\n" +
                        "14th and 15th of Adar I and Adar II (and only 14th of Adar I in a leap year)\n" +
                        "Every Shabbat\n" +
                        "Every Erev Rosh Chodesh\n" +
                        "Fast of Esther\n" +
                        "Tisha Be'av\n" +
                        "Tu Be'Shvat\n" +
                        "Lag Ba'Omer\n" +
                        "Pesach Sheni\n" +
                        "Yom Yerushalayim but not Yom Ha'atzmaut\n")
                .show();
    }
}
