package com.ej.rovadiahyosefcalendar.classes;

import static android.content.Context.MODE_PRIVATE;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.Log;
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

    public ZmanAdapter(Context context, List<String> zmanim) {
        this.zmanim = zmanim;
        this.mContext = context;
        mSharedPreferences = mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
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
            Log.d("RV", "onclickcalled");
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
        } else if (zmanim.get(position).contains("Shma Mg'a")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).contains("Shma Gr'a")) {
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
        } else if (zmanim.get(position).contains("Shma Mg'a")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).contains("Shma Gr'a")) {
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

    private void showDawnDialog() {//TODO fill these with info
        new AlertDialog.Builder(mContext)
                .setTitle("Dawn - \u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8 - Alot HaShachar")
                .setMessage("This is a long text about alot hashachar...")
                .setPositiveButton("Dismiss", (dialogInterface, i) -> { })
                .create()
                .show();
    }

    private void showEarliestTalitTefilinDialog() {
    }

    private void showSunriseDialog() {
    }

    private void showAchilatChametzDialog() {
    }

    private void showBiurChametzDialog() {
    }

    private void showShmaMGADialog() {
    }

    private void showShmaGRADialog() {
    }

    private void showBrachotShmaDialog() {
    }

    private void showChatzotDialog() {
    }

    private void showMinchaGedolaDialog() {
    }

    private void showMinchaKetanaDialog() {
    }

    private void showPlagDialog() {
    }

    private void showCandleLightingDialog() {
    }

    private void showShkiaDialog() {
    }

    private void showTzeitDialog() {
    }

    private void showTzeitTaanitDialog() {
    }

    private void showTzeitTaanitLChumraDialog() {
    }

    private void showTzeitShabbatDialog() {
    }

    private void showRTDialog() {
    }

    private void showChatzotLaylaDialog() {
    }
}
