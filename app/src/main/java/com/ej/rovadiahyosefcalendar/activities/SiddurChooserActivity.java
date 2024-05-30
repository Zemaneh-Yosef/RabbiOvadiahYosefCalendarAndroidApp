package com.ej.rovadiahyosefcalendar.activities;

import static android.text.Html.fromHtml;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Calendar;
import java.util.Locale;

public class SiddurChooserActivity extends AppCompatActivity {

    private JewishDateInfo mJewishDateInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_siddur_chooser);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            materialToolbar.setSubtitle("");
        }
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        materialToolbar.setTitle(getString(R.string.show_siddur));
        materialToolbar.setSubtitle("");
        materialToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.jerDirection) {
                startActivity(new Intent(this, JerusalemDirectionMapsActivity.class));
            }
            return false;
        });

        mJewishDateInfo = new JewishDateInfo(getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("inIsrael", false), true);
        mJewishDateInfo.getJewishCalendar().setJewishDate(
                getIntent().getIntExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear()),
                getIntent().getIntExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth()),
                getIntent().getIntExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
        );
        mJewishDateInfo.setCalendar(mJewishDateInfo.getJewishCalendar().getGregorianCalendar());// not my best work

        TextView specialDay = findViewById(R.id.jewish_special_day);
        specialDay.setText(mJewishDateInfo.getSpecialDay(false));
        if (specialDay.getText().toString().isEmpty()) {
            specialDay.setVisibility(View.GONE);
        }

        Button selichot = findViewById(R.id.selichot);
        if (mJewishDateInfo.isSelichotSaid()) {
            selichot.setVisibility(View.VISIBLE);
        } else {
            selichot.setVisibility(View.GONE);
        }
        selichot.setOnClickListener(v -> startSiddurActivity(getString(R.string.selichot)));

        Button shacharit = findViewById(R.id.shacharit);
        shacharit.setOnClickListener(v -> startSiddurActivity(getString(R.string.shacharit)));

        Button mussaf = findViewById(R.id.mussaf);
        if (!(mJewishDateInfo.getJewishCalendar().isRoshChodesh()
                || mJewishDateInfo.getJewishCalendar().isCholHamoed())) {
            mussaf.setVisibility(View.GONE);
        } else {
            mussaf.setOnClickListener(v -> startSiddurActivity(getString(R.string.mussaf)));
        }

        Button mincha = findViewById(R.id.mincha);
        mincha.setOnClickListener(v -> startSiddurActivity(getString(R.string.mincha)));

        Button neilah = findViewById(R.id.neilah);
        //if (!sJewishDateInfo.getJewishCalendar().isYomKippur()) {
            neilah.setVisibility(View.GONE);
        //}

        Button arvit = findViewById(R.id.arvit);
        arvit.setOnClickListener(v -> startSiddurActivity(getString(R.string.arvit)));

        Button bh = findViewById(R.id.birchat_hamazon);
        bh.setOnClickListener(v -> {
            JewishDateInfo tomorrow = new JewishDateInfo(getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("inIsrael", false), true);
            Calendar calendar = (Calendar) mJewishDateInfo.getJewishCalendar().getGregorianCalendar().clone();
            calendar.add(Calendar.DATE, 1);
            tomorrow.setCalendar(calendar);

            if (new SiddurMaker(mJewishDateInfo).getBirchatHamazonPrayers().equals(new SiddurMaker(tomorrow).getBirchatHamazonPrayers())) {
                startSiddurActivity(getString(R.string.birchat_hamazon));//doesn't matter which day
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.when_did_you_start_your_meal)
                        .setMessage(R.string.did_you_start_your_meal_during_the_day)
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> startSiddurActivity(getString(R.string.birchat_hamazon)))
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> startNextDaySiddurActivity(getString(R.string.birchat_hamazon)))
                        .show();
            }
        });

        TextView disclaimer = findViewById(R.id.siddur_disclaimer);
        disclaimer.setGravity(Gravity.CENTER);
        disclaimer.setClickable(true);
        disclaimer.setMovementMethod(LinkMovementMethod.getInstance());
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM) {
            disclaimer.setVisibility(View.VISIBLE);
            disclaimer.setText(getString(R.string.purim_disclaimer));
        }

        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TU_BESHVAT) {
            disclaimer.setVisibility(View.VISIBLE);
            String text;
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                text = "טוב לאמר את התפילה הזו בטו בשבט:<br><br> <a href='https://elyahu41.github.io/Prayer%20for%20an%20Etrog.pdf'>תפילה לאתרוג</a>";
            } else {
                text = "It is good to say this prayer on Tu'Beshvat:<br><br> <a href='https://elyahu41.github.io/Prayer%20for%20an%20Etrog.pdf'>Prayer for Etrog</a>";
            }
            disclaimer.setText(fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
        }

        if (mJewishDateInfo.getJewishCalendar().getUpcomingParshah() == JewishCalendar.Parsha.BESHALACH &&
                mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.TUESDAY) {
            disclaimer.setVisibility(View.VISIBLE);
            String text;
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                text = "טוב לאמר את התפילה הזו היום:<br><br> <a href='https://www.tefillos.com/Parshas-Haman-3.pdf'>פרשת המן</a>";
            } else {
                text = "It is good to say this prayer today:<br><br> <a href='https://www.tefillos.com/Parshas-Haman-3.pdf'>Parshat Haman</a>";
            }
            disclaimer.setText(fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
        }
    }

    private void startSiddurActivity(String prayer) {
        startActivity(new Intent(this, SiddurViewActivity.class)
                .putExtra("prayer", prayer)
                .putExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
                .putExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth())
                .putExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear())
        );
    }

    private void startNextDaySiddurActivity(String prayer) {
        startActivity(new Intent(this, SiddurViewActivity.class)
                .putExtra("prayer", prayer)
                .putExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth() + 1)
                .putExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth())
                .putExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear())
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onUserInteraction() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        super.onUserInteraction();
    }
}