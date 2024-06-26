package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.HighlightString;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.SiddurAdapter;
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Locale;

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
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            materialToolbar.setSubtitle("");
        }

        JewishDateInfo mJewishDateInfo = new JewishDateInfo(getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("inIsrael", false));
        mJewishDateInfo.getJewishCalendar().setJewishDate(
                getIntent().getIntExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear()),
                getIntent().getIntExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth()),
                getIntent().getIntExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
        );
        mJewishDateInfo.setCalendar(mJewishDateInfo.getJewishCalendar().getGregorianCalendar());// not my best work

        SiddurMaker siddurMaker = new SiddurMaker(mJewishDateInfo);
        ArrayList<HighlightString> prayers = new ArrayList<>();
        if (siddurTitle != null) {
            switch (siddurTitle) {
                case "סליחות":
                    prayers = siddurMaker.getSelichotPrayers(false);
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
            }
        }
        ListView siddur = findViewById(R.id.siddur);
        siddur.setAdapter(new SiddurAdapter(this, prayers, sharedPreferences.getInt("siddurTextSize", 20), mJewishDateInfo));
        siddur.setDivider(null);

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