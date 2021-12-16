package com.ej.rovadiahyosefcalendar.classes;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

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

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ZmanAdapter extends RecyclerView.Adapter<ZmanAdapter.ZmanViewHolder> {

    private final List<String> zmanim;
    private final SharedPreferences mSharedPreferences;
    private final Context mContext;
    private final AlertDialog.Builder mDialogBuilder;

    public ZmanAdapter(Context context, List<String> zmanim) {
        this.zmanim = zmanim;
        this.mContext = context;
        mSharedPreferences = mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mDialogBuilder = new AlertDialog.Builder(mContext);
        mDialogBuilder.setPositiveButton("Dismiss", (dialog, which) -> {});
        mDialogBuilder.create();
    }

    @NonNull
    @NotNull
    @Override
    public ZmanViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry, parent, false);
        return new ZmanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ZmanViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        if (zmanim.get(position).contains("=")) {
            String[] zmanAndTime = zmanim.get(position).split("=");
            holder.mLeftTextView.setText(zmanAndTime[0]);//zman
            holder.mRightTextView.setText(zmanAndTime[1]);//time
        } else {
            holder.mMiddleTextView.setText(zmanim.get(position));
        }
        holder.itemView.setOnClickListener(v -> {
            if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
                checkHebrewZmanimForDialog(position);
            } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
                checkTranslatedEnglishZmanimForDialog(position);
            } else {
                checkEnglishZmanimForDialog(position);
            }
            if (zmanim.get(position).contains("Elevation")) {
                showElevationDialog();
            }
        });
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


    private void checkHebrewZmanimForDialog(int position) {
        if (zmanim.get(position).contains("\u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8")) {
            showDawnDialog();
        } else if (zmanim.get(position).contains("\u05D8\u05DC\u05D9\u05EA \u05D5\u05EA\u05E4\u05D9\u05DC\u05D9\u05DF")) {
            showEarliestTalitTefilinDialog();
        } else if (zmanim.get(position).contains("\u05D4\u05E0\u05E5")) {
            showSunriseDialog();
        } else if (zmanim.get(position).contains("\u05D0\u05DB\u05D9\u05DC\u05EA \u05D7\u05DE\u05E5")) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).contains("\u05D1\u05D9\u05E2\u05D5\u05E8 \u05D7\u05DE\u05E5")) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).contains("\u05E9\u05DE\u05E2 \u05DE\u05D2\"\u05D0")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).contains("\u05E9\u05DE\u05E2 \u05D2\u05E8\"\u05D0")) {
            showShmaGRADialog();
        } else if (zmanim.get(position).contains("\u05D1\u05E8\u05DB\u05D5\u05EA \u05E9\u05DE\u05E2")) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).contains("\u05D7\u05E6\u05D5\u05EA")) {
            showChatzotDialog();
        } else if (zmanim.get(position).contains("\u05DE\u05E0\u05D7\u05D4 \u05D2\u05D3\u05D5\u05DC\u05D4")) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).contains("\u05DE\u05E0\u05D7\u05D4 \u05E7\u05D8\u05E0\u05D4")) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).contains("\u05E4\u05DC\u05D2 \u05D4\u05DE\u05E0\u05D7\u05D4")) {
            showPlagDialog();
        } else if (zmanim.get(position).contains("\u05D4\u05D3\u05DC\u05E7\u05EA \u05E0\u05E8\u05D5\u05EA")) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).contains("\u05E9\u05E7\u05D9\u05E2\u05D4")) {
            showShkiaDialog();
        } else if (zmanim.get(position).contains("\u05E6\u05D0\u05EA \u05D4\u05DB\u05D5\u05DB\u05D1\u05D9\u05DD")) {
            showTzeitDialog();
        } else if (zmanim.get(position).contains("\u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA")) {
            showTzeitTaanitDialog();
        } else if (zmanim.get(position).contains("\u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA \u05DC\u05D7\u05D5\u05DE\u05E8\u05D4")) {
            showTzeitTaanitLChumraDialog();
        } else if (zmanim.get(position).contains("\u05E6\u05D0\u05EA \u05E9\u05D1\u05EA/\u05D7\u05D2")) {
            showTzeitShabbatDialog();
        } else if (zmanim.get(position).contains("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD")) {
            showRTDialog();
        } else if (zmanim.get(position).contains("\u05D7\u05E6\u05D5\u05EA \u05DC\u05D9\u05DC\u05D4")) {
            showChatzotLaylaDialog();
        }
    }

    private void checkTranslatedEnglishZmanimForDialog(int position) {
        if (zmanim.get(position).contains("Dawn")) {
            showDawnDialog();
        } else if (zmanim.get(position).contains("Earliest Talit/Tefilin")) {
            showEarliestTalitTefilinDialog();
        } else if (zmanim.get(position).contains("Sunrise")) {
            showSunriseDialog();
        } else if (zmanim.get(position).contains("Achilat Chametz")) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).contains("Biur Chametz")) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).contains("Shma MG\"A")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).contains("Shma GR\"A")) {
            showShmaGRADialog();
        } else if (zmanim.get(position).contains("Brachot Shma")) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).contains("Mid-Day")) {
            showChatzotDialog();
        } else if (zmanim.get(position).contains("Mincha Gedola")) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).contains("Mincha Ketana")) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).contains("Plag HaMincha")) {
            showPlagDialog();
        } else if (zmanim.get(position).contains("Candle Lighting")) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).contains("Sunset")) {
            showShkiaDialog();
        } else if (zmanim.get(position).contains("Nightfall")) {
            showTzeitDialog();
        } else if (zmanim.get(position).contains("Fast Ends")) {
            showTzeitTaanitDialog();
        } else if (zmanim.get(position).contains("Fast Ends (Stringent)")) {
            showTzeitTaanitLChumraDialog();
        } else if (zmanim.get(position).contains("Shabbat/Chag Ends")) {
            showTzeitShabbatDialog();
        } else if (zmanim.get(position).contains("Rabbeinu Tam")) {
            showRTDialog();
        } else if (zmanim.get(position).contains("Midnight")) {
            showChatzotLaylaDialog();
        }
    }

    private void checkEnglishZmanimForDialog(int position) {
        if (zmanim.get(position).contains("Alot Hashachar")) {
            showDawnDialog();
        } else if (zmanim.get(position).contains("Earliest Talit/Tefilin")) {
            showEarliestTalitTefilinDialog();
        } else if (zmanim.get(position).contains("HaNetz")) {
            showSunriseDialog();
        } else if (zmanim.get(position).contains("Achilat Chametz")) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).contains("Biur Chametz")) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).contains("Shma MG\"A")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).contains("Shma GR\"A")) {
            showShmaGRADialog();
        } else if (zmanim.get(position).contains("Brachot Shma")) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).contains("Chatzot")) {
            showChatzotDialog();
        } else if (zmanim.get(position).contains("Mincha Gedola")) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).contains("Mincha Ketana")) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).contains("Plag HaMincha")) {
            showPlagDialog();
        } else if (zmanim.get(position).contains("Candle Lighting")) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).contains("Shkia")) {
            showShkiaDialog();
        } else if (zmanim.get(position).contains("Tzait Hacochavim")) {
            showTzeitDialog();
        } else if (zmanim.get(position).contains("Tzait Taanit")) {
            showTzeitTaanitDialog();
        } else if (zmanim.get(position).contains("Tzait Taanit L'Chumra")) {
            showTzeitTaanitLChumraDialog();
        } else if (zmanim.get(position).contains("Tzait Shabbat/Chag")) {
            showTzeitShabbatDialog();
        } else if (zmanim.get(position).contains("Rabbeinu Tam")) {
            showRTDialog();
        } else if (zmanim.get(position).contains("Chatzot Layla")) {
            showChatzotLaylaDialog();
        }
    }

    private void showDawnDialog() {
        mDialogBuilder.setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("In Tanach this time is called Alot HaShachar (בראשית לב:כה), whereas in the gemara it is called Amud HaShachar.\n\n" +
                        "This is the time when the day begins according to halacha. " +
                        "Most mitzvot (commandments), Arvit for example, that take place at night are not allowed " +
                        "to be done after this time. After this time, mitzvot that must be done in the daytime are " +
                        "allowed to be done B'dieved (after the fact) or B'shaat hadachak (in a strenuous time). However, one should ideally wait " +
                        "until sunrise to do them L'chatchila (optimally).\n\n" +
                        "This time is calculated as 72 zmaniyot/seasonal minutes (according to the GR\"A) before sunrise. Both sunrise and sunset " +
                        "have elevation included.")
                .show();
    }

    private void showEarliestTalitTefilinDialog() {
        mDialogBuilder.setTitle("Earliest Talit/Tefilin - \u05D8\u05DC\u05D9\u05EA \u05D5\u05EA\u05E4\u05D9\u05DC\u05D9\u05DF - Misheyakir")
                .setMessage("Misheyakir (literally \"when you recognize\") is the time when a person can distinguish between blue and white. " +
                        "The gemara (ברכות ט) explains that when a person can distinguish between the blue (techelet) and white strings " +
                        "of their tzitzit, that is the earliest time a person can put on their talit and tefilin for shacharit.\n\n" +
                        "This time is calculated as 6 zmaniyot/seasonal minutes (according to the GR\"A) after Alot HaShachar (Dawn).\n\n" +
                        "Note: This time is only for people who need to go to work or leave early in the morning to travel, however, normally a " +
                        "person should put on his talit/tefilin 60 regular minutes (and in the winter 50 regular minutes) before sunrise.")
                .show();
    }

    private void showSunriseDialog() {
        mDialogBuilder.setTitle("Sunrise - \u05D4\u05E0\u05E5 - HaNetz")
                .setMessage("This is the earliest time when all mitzvot (commandments) that are to be done during the daytime are allowed to be " +
                        "performed L'chatchila (optimally). Halachic sunrise is defined as the moment when the top edge of the sun appears on the " +
                        "horizon while rising. Whereas, the gentiles define sunrise as the moment when the sun is halfway through the horizon. " +
                        "This halachic sunrise is called mishor (sea level) sunrise and it is what many jews rely on when praying for Netz.\n\n" +
                        "However, it should be noted that the Shulchan Aruch writes in Orach Chayim 89:1, \"The mitzvah of shacharit starts at " +
                        "Netz, like it says in the pasuk/verse, 'יראוך עם שמש'\". Based on this, the poskim write that a person should wait until " +
                        "the sun is VISIBLE to say shacharit. In Israel, the Ohr HaChaim calendar uses a table of sunrise times from the " +
                        "sefer/book 'לוח ביכורי יוסף' (Luach Bechoray Yosef) each year. These times were made by Chaim Keller, creator of the " +
                        "ChaiTables website. Ideally, you should download these VISIBLE sunrise times from his website with the capability of " +
                        "this app to use for the year. However, if you did not download the times, you will see 'Mishor' or 'Sea Level' sunrise instead.")
                .show();
    }

    private void showAchilatChametzDialog() {
        mDialogBuilder.setTitle("Eating Chametz - \u05D0\u05DB\u05D9\u05DC\u05EA \u05D7\u05DE\u05E5 - Achilat Chametz")
                .setMessage("This is the latest time a person can eat chametz.\n\n" +
                        "This is calculated as 4 zmaniyot/seasonal hours, according to the Magen Avraham, after Alot HaShachar (Dawn) with " +
                        "elevation included. Since Chametz is a mitzvah from the torah, we are stringent and we use the Magen Avraham's time to " +
                        "calculate the last time a person can eat chametz.")
                .show();
    }

    private void showBiurChametzDialog() {
        mDialogBuilder.setTitle("Burning Chametz - \u05D1\u05D9\u05E2\u05D5\u05E8 \u05D7\u05DE\u05E5 - Biur Chametz")
                .setMessage("This is the latest time a person can own chametz before pesach begins in the night time.\n\n" +//TODO check how to calculate
                        "This is calculated as 5 zmaniyot/seasonal hours, according to the Magen Avraham, after Alot HaShachar (Dawn) with " +
                        "elevation included.")
                .show();
    }

    private void showShmaMGADialog() {
        mDialogBuilder.setTitle("Latest time for Shma (MG\"A) - \u05E9\u05DE\u05E2 \u05DE\u05D2\"\u05D0 - Shma MG\"A")
                .setMessage("This is the latest time a person can say Shma according to the Magen Avraham.\n\n" +
                        "The Magen Avraham/Terumat HeDeshen calculate this time as 3 zmaniyot/seasonal hours after Alot HaShachar (Dawn). " +
                        "They calculate a zmaniyot/seasonal hour by taking the time between Alot HaShachar (Dawn) and Tzeit Hachocavim (Nightfall) " +
                        "of Rabbeinu Tam and divide it into 12 equal parts.")
                .show();
    }

    private void showShmaGRADialog() {
        mDialogBuilder.setTitle("Latest time for Shma (GR\"A) - \u05E9\u05DE\u05E2 \u05D2\u05E8\"\u05D0 - Shma GR\"A")
                .setMessage("This is the latest time a person can say Shma according to the GR\"A.\n\n" +
                        "The GR\"A calculates this time as 3 zmaniyot/seasonal hours after sunrise. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts.")
                .show();
    }

    private void showBrachotShmaDialog() {
        mDialogBuilder.setTitle("Brachot Shma - \u05D1\u05E8\u05DB\u05D5\u05EA \u05E9\u05DE\u05E2 - Brachot Shma")
                .setMessage("This is the latest time a person can say the Brachot Shma according to the GR\"A.\n\n" +
                        "The GR\"A calculates this time as 4 zmaniyot/seasonal hours after sunrise. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts.")
                .show();
    }

    private void showChatzotDialog() {
        mDialogBuilder.setTitle("Mid-Day - \u05D7\u05E6\u05D5\u05EA - Chatzot")
                .setMessage("This is the middle of the halachic day, when the sun is exactly in the middle of the sky.\n\n" +
                        "This time is calculated as 6 zmaniyot/seasonal hours after sunrise. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts.\n\n" +
                        "After this time, you can no longer say the Amidah of Shacharit, and you should say Musaf before this time.")
                .show();
    }

    private void showMinchaGedolaDialog() {
        mDialogBuilder.setTitle("Earliest Mincha - \u05DE\u05E0\u05D7\u05D4 \u05D2\u05D3\u05D5\u05DC\u05D4 - Mincha Gedolah")
                .setMessage("This is the earliest time a person can say Mincha.\n\n Korbanot should ideally also be said AFTER this time.\n\n" +
                        "This time is calculated as 30 regular minutes after Chatzot (Mid-Day). If the zmaniyot/seasonal minutes are longer, we use " +
                        "those minutes instead to be stringent." +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.")
                .show();
    }

    private void showMinchaKetanaDialog() {
        mDialogBuilder.setTitle("Mincha Ketana - \u05DE\u05E0\u05D7\u05D4 \u05E7\u05D8\u05E0\u05D4")
                .setMessage("This is the best time a person can say Mincha according to some poskim.\n\n" +
                        "This time is calculated as 9 and a half zmaniyot/seasonal hours after sunrise. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.")
                .show();
    }

    private void showPlagDialog() {
        mDialogBuilder.setTitle("Plag HaMincha - \u05E4\u05DC\u05D2 \u05D4\u05DE\u05E0\u05D7\u05D4")
                .setMessage("This is the best time a person can say Mincha according to some poskim (Rambam and others).\n\n" +
                        "This time is calculated as 9 and a half zmaniyot/seasonal hours after sunrise. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.")
                .show();
    }

    private void showCandleLightingDialog() {
        mDialogBuilder.setTitle("Candle Lighting - \u05D4\u05D3\u05DC\u05E7\u05EA \u05E0\u05E8\u05D5\u05EA")
                .setMessage("This is the best time for a person to light the candles before shabbat/chag starts.\n\n" +
                        "This time is calculated as " +
                        PreferenceManager.getDefaultSharedPreferences(mContext).getString("CandleLightingOffset", "20") + " " +
                        "regular minutes before sunset (elevation included).")
                .show();
    }

    private void showShkiaDialog() {
        mDialogBuilder.setTitle("Sunset - \u05E9\u05E7\u05D9\u05E2\u05D4 - Shkia")
                .setMessage("This is the time that the day starts to transition into the next day according to halacha.\n\n" +
                        "Halachic sunset is defined as the moment when the top edge of the sun disappears on the " +
                        "horizon while setting (elevation included). Whereas, the gentiles define sunset as the moment when the sun is halfway " +
                        "through the horizon.\n\n" +
                        "Immediately after the sun sets, Bein Hashmashot/twilight starts according to the Geonim, however, according to Rabbeinu Tam " +
                        "the sun continues to set for another 58.5 minutes and only after that Bein Hashmashot starts for another 13.5 minutes.\n\n" +
                        "It should be noted that many poskim, like the Mishna Berura, say that a person should ideally say mincha BEFORE sunset " +
                        "and not before Tzeit/Nightfall.\n\n" +
                        "Most mitzvot that are to be done during the day should be done before this time.")
                .show();
    }

    private void showTzeitDialog() {
        mDialogBuilder.setTitle("Nightfall - \u05E6\u05D0\u05EA \u05D4\u05DB\u05D5\u05DB\u05D1\u05D9\u05DD - Tzeit Hacochavim")
                .setMessage("This is the latest time a person can say Mincha according Rav Ovadiah Yosef Z\"TL. A person should FINISH mincha at " +
                        "least 2 minutes before this time.\n\n" +
                        "Tzeit/Nightfall is the time when the next halachic day starts after Bein Hashmashot/twilight finishes.\n\n" +
                        "This time is calculated as 13 and a half zmaniyot/seasonal minutes after sunset. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.")
                .show();
    }

    private void showTzeitTaanitDialog() {
        mDialogBuilder.setTitle("Fast Ends - \u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA - Tzeit Taanit")
                .setMessage("This is the time that the fast/taanit ends.\n\n" +
                        "This time is calculated as 20 regular minutes after sunset (elevation included).")
                .show();
    }

    private void showTzeitTaanitLChumraDialog() {
        mDialogBuilder.setTitle("Fast Ends (Stringent) - \u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA \u05DC\u05D7\u05D5\u05DE\u05E8\u05D4 - Tzeit Taanit L'Chumra")
                .setMessage("This is the more stringent time that the fast/taanit ends.\n\n" +
                        "This time is calculated as 30 regular minutes after sunset (elevation included).")
                .show();
    }

    private void showTzeitShabbatDialog() {
        mDialogBuilder.setTitle("Shabbat/Chag Ends - \u05E6\u05D0\u05EA \u05E9\u05D1\u05EA/\u05D7\u05D2 - Tzeit Shabbat/Chag")
                .setMessage("This is the time that Shabbat/Chag ends.\n\n" +
                        "Note that there are many customs on when shabbat ends, by default, I set it to 45 regular minutes after sunset (elevation " +
                        "included), however, you can change the time in the settings.\n\n" +
                        "This time is calculated as " +
                        PreferenceManager.getDefaultSharedPreferences(mContext).getString("EndOfShabbatOffset", "45") + " " +
                        "regular minutes after sunset (elevation included).")
                .show();
    }

    private void showRTDialog() {
        mDialogBuilder.setTitle("Rabbeinu Tam - \u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD")
                .setMessage("This time is Tzeit/Nightfall according Rabbeinu Tam.\n\n" +
                        "Tzeit/Nightfall is the time when the next halachic day starts after Bein Hashmashot/twilight finishes.\n\n" +
                        "This time is calculated as 72 zmaniyot/seasonal minutes after sunset (elevation included). " +
                        "According to Rabbeinu Tam, these 72 minutes are made up of 2 parts. The first part is 58 and a half minutes until the " +
                        "second sunset (see Pesachim 94a and Tosafot there). After the second sunset, there are an additional 13.5 minutes until " +
                        "Tzeit/Nightfall.\n\n" +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute in order " +
                        "to calculate 72 minutes. Another way of calculating this time is by calculating how many minutes are between sunrise and " +
                        "sunset. Take that number and divide it by 10, and then add the result to sunset.")
                .show();
    }

    private void showChatzotLaylaDialog() {
        mDialogBuilder.setTitle("Midnight - \u05D7\u05E6\u05D5\u05EA \u05DC\u05D9\u05DC\u05D4 - Chatzot Layla")
                .setMessage("This is the middle of the halachic night, when the sun is exactly in the middle of the sky beneath us.\n\n" +
                        "This time is calculated as 6 zmaniyot/seasonal hours after sunset. " +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts.\n\n")
                .show();
    }

    private void showElevationDialog() {
        mDialogBuilder.setTitle("Current Elevation")
                .setMessage("This number represents the highest point that you can see sunrise/sunset in your current city in meters. If the number" +
                        " is set to 0, then you are calculating the zmanim by mishor/sea level sunrise and sunset.\n\n" +
                        "There is a debate as to what Rabbi Ovadiah Yosef Z\"TL " +
                        "held about using elevation for zmanim. (See Halacha Berura vol. 14, in Otzrot Yosef (Kuntrus Ki Ba Hashemesh), Siman 6, " +
                        "Perek 21 for an in depth discussion) The Ohr HaChaim calendar uses elevation for their zmanim, however, I also added the " +
                        "ability to not use elevation for those that do not want to use it.")
                .show();
    }
}
