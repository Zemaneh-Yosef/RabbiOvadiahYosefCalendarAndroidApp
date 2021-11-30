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
                .setMessage("In Tanach this time is called Alot HaShachar (בראשית לב:כה), whereas in the gemara it is called Amud HaShachar.\n\n " +
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
                        "This time is calculated as 6 zmaniyot/seasonal minutes (according to the GR\"A) after Alot HaShachar (Dawn).\n\n " +
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
                        "this app to use for the year. However, if you choose not to, you will see 'Mishor' or 'Sea Level' sunrise instead.")
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
                .setMessage("This is the earliest time a person can say Mincha. Korbanot should ideally also be said AFTER this time.\n\n" +
                        "This time is calculated as 30 regular minutes after Chatzot (Mid-Day). If the zmaniyot/seasonal minutes are longer, we use " +
                        "those minutes instead to be stringent." +
                        "The GR\"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and " +
                        "divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.")
                .show();
    }

    private void showMinchaKetanaDialog() {//TODO
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showPlagDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showCandleLightingDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showShkiaDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showTzeitDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showTzeitTaanitDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showTzeitTaanitLChumraDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showTzeitShabbatDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showRTDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showChatzotLaylaDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }
}
