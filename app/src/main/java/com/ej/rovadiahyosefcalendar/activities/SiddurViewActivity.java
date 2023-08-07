package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sJewishDateInfo;

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

public class SiddurViewActivity extends AppCompatActivity {

    private SiddurMaker siddurMaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_siddur_view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        siddurMaker = new SiddurMaker(sJewishDateInfo);
        ArrayList<HighlightString> prayers = new ArrayList<>();
        switch ((String) getIntent().getExtras().get("prayer")) {
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
        siddur.setAdapter(new SiddurAdapter(this, prayers, 20));

        SeekBar seekBar = findViewById(R.id.siddur_seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SiddurAdapter sa = (SiddurAdapter) siddur.getAdapter();
                sa.setTextSize(Math.max(progress, 11));
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