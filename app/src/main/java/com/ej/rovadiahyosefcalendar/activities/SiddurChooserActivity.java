package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sJewishDateInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ej.rovadiahyosefcalendar.R;

public class SiddurChooserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_siddur_chooser);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView specialDay = findViewById(R.id.jewish_special_day);
        specialDay.setText(sJewishDateInfo.getSpecialDayWithoutOmer());


        Button shacharit = findViewById(R.id.shacharit);
        shacharit.setOnClickListener(v -> {
            startActivity(new Intent(this, SiddurViewActivity.class)
                    .putExtra("prayer", "Shacharit"));
        });
        Button mussaf = findViewById(R.id.mussaf);
        if (!(sJewishDateInfo.getJewishCalendar().isRoshChodesh()
                || sJewishDateInfo.getJewishCalendar().isCholHamoed())) {
            mussaf.setVisibility(View.GONE);
        } else {
            startActivity(new Intent(this, SiddurViewActivity.class)
                    .putExtra("prayer", "Musaf"));

        }
        Button mincha = findViewById(R.id.mincha);
        mincha.setOnClickListener(v -> {
            startActivity(new Intent(this, SiddurViewActivity.class)
                    .putExtra("prayer", "Mincha"));
        });
        Button neilah = findViewById(R.id.neilah);
        if (!sJewishDateInfo.getJewishCalendar().isYomKippur()) {
            neilah.setVisibility(View.GONE);
        }
        Button arvit = findViewById(R.id.arvit);
        arvit.setOnClickListener(v -> {
            startActivity(new Intent(this, SiddurViewActivity.class)
                    .putExtra("prayer", "Arvit"));
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