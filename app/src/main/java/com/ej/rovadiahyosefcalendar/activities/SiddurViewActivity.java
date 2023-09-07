package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sJewishDateInfo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.HighlightString;
import com.ej.rovadiahyosefcalendar.classes.SiddurAdapter;
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker;

import java.util.ArrayList;
import java.util.Objects;

public class SiddurViewActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_siddur_view);
        sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.show_siddur));
        }
        SiddurMaker siddurMaker = new SiddurMaker(sJewishDateInfo);
        ArrayList<HighlightString> prayers = new ArrayList<>();
        switch ((String) Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("prayer"))) {
            case "Selichot":
                prayers = siddurMaker.getSelichotPrayers();
                break;
            case "Shacharit":
                prayers = siddurMaker.getShacharitPrayers();
                break;
            case "Musaf":
                prayers = siddurMaker.getMusafPrayers();
                break;
            case "Mincha":
                prayers = siddurMaker.getMinchaPrayers();
                break;
            case "Arvit":
                prayers = siddurMaker.getArvitPrayers();
                break;
        }
        ListView siddur = findViewById(R.id.siddur);
        siddur.setAdapter(new SiddurAdapter(this, prayers, sharedPreferences.getInt("siddurTextSize", 20)));
        siddur.setDivider(null);

        SeekBar seekBar = findViewById(R.id.siddur_seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SiddurAdapter sa = (SiddurAdapter) siddur.getAdapter();
                sa.setTextSize(Math.max(progress, 11));
                sharedPreferences.edit().putInt("siddurTextSize", progress).apply();
                siddur.invalidateViews();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}