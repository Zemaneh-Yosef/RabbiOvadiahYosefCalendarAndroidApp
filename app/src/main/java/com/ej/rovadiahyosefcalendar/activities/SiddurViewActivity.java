package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.HighlightString;
import com.ej.rovadiahyosefcalendar.classes.HorizontalAdapter;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocaleChecker;
import com.ej.rovadiahyosefcalendar.classes.SiddurAdapter;
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class SiddurViewActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String siddurTitle = getIntent().getStringExtra("prayer");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_siddur_view);
        sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        if (siddurTitle != null) {
            materialToolbar.setTitle((!siddurTitle.isEmpty() ? siddurTitle : getString(R.string.show_siddur)));
        }
        if (LocaleChecker.isLocaleHebrew()) {
            materialToolbar.setSubtitle("");
        }

        JewishDateInfo mJewishDateInfo = new JewishDateInfo(sharedPreferences.getBoolean("inIsrael", false));
        mJewishDateInfo.getJewishCalendar().setJewishDate(
                getIntent().getIntExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear()),
                getIntent().getIntExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth()),
                getIntent().getIntExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
        );
        mJewishDateInfo.setCalendar(mJewishDateInfo.getJewishCalendar().getGregorianCalendar());// not my best work
        mJewishDateInfo.getJewishCalendar().setIsMukafChoma(sharedPreferences.getBoolean("isMukafChoma", false));
        mJewishDateInfo.getJewishCalendar().setIsSafekMukafChoma(sharedPreferences.getBoolean("isSafekMukafChoma", false));

        SiddurMaker siddurMaker = new SiddurMaker(mJewishDateInfo);
        ArrayList<HighlightString> prayers = new ArrayList<>();
        if (siddurTitle != null) {
            switch (siddurTitle) {
                case "סליחות":
                    prayers = siddurMaker.getSelichotPrayers(false, getIntent().getBooleanExtra("isAfterChatzot", false));
                    break;
                case "שחרית":
                    prayers = siddurMaker.getShacharitPrayers();
                    break;
                case "מוסף":
                    prayers = siddurMaker.getMusafPrayers();
                    break;
                case "מנחה":
                    prayers = siddurMaker.getMinchaPrayers();
                    break;
                case "ערבית":
                    prayers = siddurMaker.getArvitPrayers();
                    break;
                case "ברכת המזון":
                    prayers = siddurMaker.getBirchatHamazonPrayers();
                    break;
                case "ברכת הלבנה":
                    prayers = siddurMaker.getBirchatHalevanaPrayers();
                    break;
                case "תיקון חצות":
                    prayers = siddurMaker.getTikkunChatzotPrayers(getIntent().getBooleanExtra("isNightTikkunChatzot", true));
                    break;
                case "ק״ש שעל המיטה":
                    prayers = siddurMaker.getKriatShemaShealHamitaPrayers(getIntent().getBooleanExtra("isBeforeChatzot", false));
                    break;
                case "ברכת מעין שלוש":
                    prayers = siddurMaker.getBirchatMeeyinShaloshPrayers();
                    break;
            }
        }
        ListView siddur = findViewById(R.id.siddur);
        siddur.setAdapter(new SiddurAdapter(this,
                prayers,
                sharedPreferences.getInt("siddurTextSize", 20),
                sharedPreferences.getBoolean("isJustified", false),
                mJewishDateInfo));
        siddur.setDivider(null);
        Map<Integer, HighlightString> categories = new LinkedHashMap<>();
        int index = 0;
        for (HighlightString string: prayers) {
            if (string.isCategory()) {
                if (categories.containsValue(string)) {
                    string.setString(string + "\u200E");// this unicode character is invisible. So it will always increment without showing in the UI
                }
                categories.put(index, string);
                index++;
            }
        }
        RecyclerView tefilotCategories = findViewById(R.id.tefilot_categories);
        tefilotCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tefilotCategories.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));
        ArrayList<HighlightString> finalPrayers = prayers;
        HorizontalAdapter.OnItemClickListener listener = category -> {
            int position = finalPrayers.indexOf(category);
            if (position != -1) {
                siddur.setSelection(position);
            }
        };
        tefilotCategories.setAdapter(new HorizontalAdapter(new ArrayList<>(categories.values()), listener));

        SeekBar seekBar = findViewById(R.id.siddur_seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SiddurAdapter sa = (SiddurAdapter) siddur.getAdapter();
                sa.setTextSize(progress + 11);
                sharedPreferences.edit().putInt("siddurTextSize", progress + 11).apply();
                siddur.invalidateViews();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Button textAlignment = findViewById(R.id.textAlignment);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {// Justified text is not supported before Q
            textAlignment.setVisibility(View.GONE);
        }
        textAlignment.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, sharedPreferences.getBoolean("isJustified", false) ? R.drawable.baseline_format_align_justify_24 : R.drawable.baseline_format_align_right_24);
        textAlignment.setOnClickListener(v -> {
            boolean isJustified = sharedPreferences.getBoolean("isJustified", false);
            isJustified = !isJustified;
            sharedPreferences.edit().putBoolean("isJustified", isJustified).apply();
            textAlignment.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, sharedPreferences.getBoolean("isJustified", false) ? R.drawable.baseline_format_align_justify_24 : R.drawable.baseline_format_align_right_24);
            SiddurAdapter sa = (SiddurAdapter) siddur.getAdapter();
            sa.setIsJustified(isJustified);
            siddur.invalidateViews();
        });

        ViewCompat.setOnApplyWindowInsetsListener(seekBar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            v.setLayoutParams(mlp);
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });
    }
}