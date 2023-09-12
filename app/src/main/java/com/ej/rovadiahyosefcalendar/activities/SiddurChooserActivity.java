package com.ej.rovadiahyosefcalendar.activities;

import static android.text.Html.fromHtml;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sJewishDateInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ej.rovadiahyosefcalendar.R;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Locale;

public class SiddurChooserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_siddur_chooser);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.show_siddur));
        }

        TextView specialDay = findViewById(R.id.jewish_special_day);
        specialDay.setText(sJewishDateInfo.getSpecialDayWithoutOmer());

        Button selichot = findViewById(R.id.selichot);
        if (sJewishDateInfo.isSelichotSaid()) {
            selichot.setVisibility(View.VISIBLE);
        } else {
            selichot.setVisibility(View.INVISIBLE);
        }
        selichot.setOnClickListener(v -> startActivity(new Intent(this, SiddurViewActivity.class)
                .putExtra("prayer", "Selichot")));

        Button shacharit = findViewById(R.id.shacharit);
        shacharit.setOnClickListener(v -> startActivity(new Intent(this, SiddurViewActivity.class)
                .putExtra("prayer", "Shacharit")));

        Button mussaf = findViewById(R.id.mussaf);
        if (!(sJewishDateInfo.getJewishCalendar().isRoshChodesh()
                || sJewishDateInfo.getJewishCalendar().isCholHamoed())) {
            mussaf.setVisibility(View.GONE);
        } else {
            mussaf.setOnClickListener(v -> startActivity(new Intent(this, SiddurViewActivity.class)
                    .putExtra("prayer", "Musaf")));
        }

        Button mincha = findViewById(R.id.mincha);
        mincha.setOnClickListener(v -> startActivity(new Intent(this, SiddurViewActivity.class)
                .putExtra("prayer", "Mincha")));

        Button neilah = findViewById(R.id.neilah);
        //if (!sJewishDateInfo.getJewishCalendar().isYomKippur()) {
            neilah.setVisibility(View.GONE);
        //}

        Button arvit = findViewById(R.id.arvit);
        arvit.setOnClickListener(v -> startActivity(new Intent(this, SiddurViewActivity.class)
                .putExtra("prayer", "Arvit")));

        TextView disclaimer = findViewById(R.id.siddur_disclaimer);
        disclaimer.setGravity(Gravity.CENTER);
        disclaimer.setClickable(true);
        disclaimer.setMovementMethod(LinkMovementMethod.getInstance());
        if (sJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM) {
            disclaimer.setVisibility(View.VISIBLE);
            disclaimer.setText(getString(R.string.purim_disclaimer));
        }

        if (sJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TU_BESHVAT) {
            disclaimer.setVisibility(View.VISIBLE);
            String text = "";
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                text = "טוב לאמר את התפילה הזו בטו בשבט:<br><br> <a href='https://elyahu41.github.io/Prayer%20for%20an%20Etrog.pdf'>תפילה לאתרוג</a>";
            } else {
                text = "It is good to say this prayer on Tu'Beshvat:<br><br> <a href='https://elyahu41.github.io/Prayer%20for%20an%20Etrog.pdf'>Prayer for Etrog</a>";
            }
            disclaimer.setText(fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}