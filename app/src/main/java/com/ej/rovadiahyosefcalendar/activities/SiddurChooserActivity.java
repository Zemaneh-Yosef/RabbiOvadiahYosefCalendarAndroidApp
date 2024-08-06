package com.ej.rovadiahyosefcalendar.activities;

import static android.text.Html.fromHtml;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sLongitude;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SiddurChooserActivity extends AppCompatActivity {

    private JewishDateInfo mJewishDateInfo;
    private ROZmanimCalendar mZmanimCalendar;

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

        mJewishDateInfo = new JewishDateInfo(getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("inIsrael", false));
        mJewishDateInfo.getJewishCalendar().setJewishDate(
                getIntent().getIntExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear()),
                getIntent().getIntExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth()),
                getIntent().getIntExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
        );
        mJewishDateInfo.setCalendar(mJewishDateInfo.getJewishCalendar().getGregorianCalendar());// not my best work, we need to call setCalendar

        TextView specialDay = findViewById(R.id.jewish_special_day);
        Calendar calendar = mJewishDateInfo.getJewishCalendar().getGregorianCalendar();
        String dateAndSpecialDay = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                + "\n" +
                mJewishDateInfo.getJewishCalendar().toString();
        String specialDayString = mJewishDateInfo.getSpecialDay(false);
        if (!specialDayString.isEmpty()) {
            dateAndSpecialDay += "\n" + specialDayString;
        }
        specialDay.setText(dateAndSpecialDay);

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

        TextView nightDayOfWeek = findViewById(R.id.nightDayOfWeek);
        Calendar calendarPlusOne = (Calendar) mJewishDateInfo.getJewishCalendar().getGregorianCalendar().clone();
        calendarPlusOne.add(Calendar.DATE, 1);
        mJewishDateInfo.setCalendar(calendarPlusOne);
        String nextDateAndSpecialDay = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                + "\n" + getString(R.string.after_sunset) + "\n" +
                mJewishDateInfo.getJewishCalendar().toString();
        String nextSpecialDayString = mJewishDateInfo.getSpecialDay(false);
        if (!nextSpecialDayString.isEmpty()) {
            nextDateAndSpecialDay += "\n" + nextSpecialDayString;
        }
        nightDayOfWeek.setText(nextDateAndSpecialDay);
        mJewishDateInfo.setCalendar(calendar);//reset

        Button arvit = findViewById(R.id.arvit);
        arvit.setOnClickListener(v -> startSiddurActivity(getString(R.string.arvit)));

        Button kriatShemaAlHamita = findViewById(R.id.kriat_shema_al_hamita);
        kriatShemaAlHamita.setOnClickListener(v -> startNextDaySiddurActivity(getString(R.string.kriatShema), false));

        Button tikkunChatzot = findViewById(R.id.tikkun_chatzot);
        Button tikkunChatzot3Weeks = findViewById(R.id.tikkun_chatzot_3Weeks);

        if (mJewishDateInfo.is3Weeks()) {
            tikkunChatzot.setVisibility(View.GONE);
            tikkunChatzot3Weeks.setVisibility(View.VISIBLE);
        } else {
            tikkunChatzot.setVisibility(View.VISIBLE);
            tikkunChatzot3Weeks.setVisibility(View.GONE);
        }

        View.OnClickListener tikkunChatzotOnClickListener = v -> {
            if (mJewishDateInfo.is3Weeks()) {
                boolean isTachanunSaid = mJewishDateInfo.getIsTachanunSaid().equals("Tachanun only in the morning")
                        || mJewishDateInfo.getIsTachanunSaid().equals("אומרים תחנון רק בבוקר")
                        || mJewishDateInfo.getIsTachanunSaid().equals("אומרים תחנון")
                        || mJewishDateInfo.getIsTachanunSaid().equals("There is Tachanun today");// TODO see if tikkun chatzot for the day is said on shabbat
                if (mJewishDateInfo.isDayTikkunChatzotSaid() && isTachanunSaid) {
                    new MaterialAlertDialogBuilder(SiddurChooserActivity.this)
                            .setTitle(R.string.do_you_want_to_say_tikkun_chatzot_for_the_day)
                            .setMessage(R.string.looking_to_say_this_version_of_tikkun_chatzot)
                            .setPositiveButton(SiddurChooserActivity.this.getString(R.string.yes), (dialog, which) -> SiddurChooserActivity.this.startSiddurActivity(SiddurChooserActivity.this.getString(R.string.tikkun_chatzot)))
                            .setNegativeButton(SiddurChooserActivity.this.getString(R.string.no), (dialog, which) -> SiddurChooserActivity.this.startNextDaySiddurActivity(SiddurChooserActivity.this.getString(R.string.tikkun_chatzot), true))
                            .show();
                } else {
                    mJewishDateInfo.setCalendar(calendarPlusOne);
                    if (mJewishDateInfo.isNightTikkunChatzotSaid()) {
                        SiddurChooserActivity.this.startNextDaySiddurActivity(SiddurChooserActivity.this.getString(R.string.tikkun_chatzot), true);
                    } else {
                        new MaterialAlertDialogBuilder(SiddurChooserActivity.this)
                                .setTitle(R.string.tikkun_chatzot_is_not_said_today_or_tonight)
                                .setMessage(R.string.tikkun_chatzot_is_not_said_today_or_tonight_possible_reasons)
                                .setPositiveButton(SiddurChooserActivity.this.getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                    mJewishDateInfo.setCalendar(calendar);
                }
            } else {
                mJewishDateInfo.setCalendar(calendarPlusOne);
                if (mJewishDateInfo.isNightTikkunChatzotSaid()) {
                    SiddurChooserActivity.this.startNextDaySiddurActivity(SiddurChooserActivity.this.getString(R.string.tikkun_chatzot), true);
                } else {
                    new MaterialAlertDialogBuilder(SiddurChooserActivity.this)
                            .setTitle(R.string.tikkun_chatzot_is_not_said_tonight)
                            .setMessage(R.string.tikkun_chatzot_is_not_said_tonight_possible_reasons)
                            .setPositiveButton(SiddurChooserActivity.this.getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                            .show();
                }
                mJewishDateInfo.setCalendar(calendar);
            }
        };

        tikkunChatzot.setOnClickListener(tikkunChatzotOnClickListener);
        tikkunChatzot3Weeks.setOnClickListener(tikkunChatzotOnClickListener);

        mZmanimCalendar = new ROZmanimCalendar(new GeoLocation("", sLatitude, sLongitude, getLastKnownElevation(getSharedPreferences(SHARED_PREF, MODE_PRIVATE)), TimeZone.getTimeZone(sCurrentTimeZoneID)));
        mZmanimCalendar.setCalendar(calendar);
        String sunset = DateFormat.getTimeInstance(DateFormat.SHORT).format(mZmanimCalendar.getSunset());

        Button bh = findViewById(R.id.birchat_hamazon);
        bh.setOnClickListener(v -> {
            JewishDateInfo tomorrow = new JewishDateInfo(mJewishDateInfo.getJewishCalendar().getInIsrael());
            Calendar nextDayCalendar = (Calendar) mJewishDateInfo.getJewishCalendar().getGregorianCalendar().clone();
            nextDayCalendar.add(Calendar.DATE, 1);
            tomorrow.setCalendar(nextDayCalendar);

            if (new SiddurMaker(mJewishDateInfo).getBirchatHamazonPrayers().equals(new SiddurMaker(tomorrow).getBirchatHamazonPrayers())) {
                startSiddurActivity(getString(R.string.birchat_hamazon));//doesn't matter which day
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.when_did_you_start_your_meal)
                        .setMessage(getString(R.string.did_you_start_your_meal_during_the_day) + " (" + sunset + ")")
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> startSiddurActivity(getString(R.string.birchat_hamazon)))
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> startNextDaySiddurActivity(getString(R.string.birchat_hamazon), false))
                        .show();
            }
        });

        Button birchatLevana = findViewById(R.id.birchat_halevana);
        birchatLevana.setOnClickListener(v -> startSiddurActivity(getString(R.string.birchat_levana)));

        if (mJewishDateInfo.getBirchatLevana().isEmpty()) {// hide the button if there's no status text returned
            birchatLevana.setVisibility(View.GONE);
        }

        TextView disclaimer = findViewById(R.id.siddur_disclaimer);
        disclaimer.setGravity(Gravity.CENTER);
        disclaimer.setClickable(true);
        disclaimer.setMovementMethod(LinkMovementMethod.getInstance());
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM) {
            disclaimer.setText(getString(R.string.purim_disclaimer));
        }

        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TU_BESHVAT) {
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
            String text;
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                text = "טוב לאמר את התפילה הזו היום:<br><br> <a href='https://www.tefillos.com/Parshas-Haman-3.pdf'>פרשת המן</a>";
            } else {
                text = "It is good to say this prayer today:<br><br> <a href='https://www.tefillos.com/Parshas-Haman-3.pdf'>Parshat Haman</a>";
            }
            disclaimer.setText(fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
        }

        ViewCompat.setOnApplyWindowInsetsListener(disclaimer, (v, windowInsets) -> {
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

    private void startSiddurActivity(String prayer) {
        startActivity(new Intent(this, SiddurViewActivity.class)
                .putExtra("prayer", prayer)
                .putExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
                .putExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth())
                .putExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear())
                .putExtra("isNightTikkunChatzot", false)
        );
    }

    private void startNextDaySiddurActivity(String prayer, boolean isNightTikkunChatzot) {
        mJewishDateInfo.getJewishCalendar().forward(Calendar.DATE, 1);
        startActivity(new Intent(this, SiddurViewActivity.class)
                .putExtra("prayer", prayer)
                .putExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
                .putExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth())
                .putExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear())
                .putExtra("isBeforeChatzot", new Date().before(mZmanimCalendar.getSolarMidnight()))
                .putExtra("isNightTikkunChatzot", isNightTikkunChatzot)
        );
        mJewishDateInfo.getJewishCalendar().back();//reset
    }

    private static double getLastKnownElevation(SharedPreferences mSharedPreferences) {
        double elevation;
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            elevation = 0;
        } else {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        }
        return elevation;
    }
}