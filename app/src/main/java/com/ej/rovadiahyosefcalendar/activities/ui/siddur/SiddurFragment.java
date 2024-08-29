package com.ej.rovadiahyosefcalendar.activities.ui.siddur;

import static android.content.Context.MODE_PRIVATE;
import static android.text.Html.fromHtml;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.materialToolbar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sElevation;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLongitude;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.JerusalemDirectionMapsActivity;
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker;
import com.ej.rovadiahyosefcalendar.databinding.FragmentSiddurBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SiddurFragment extends Fragment {

    private FragmentSiddurBinding binding;
    private Context mContext;
    private FragmentActivity mActivity;
    private ROZmanimCalendar mZmanimCalendar;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = requireActivity();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSiddurBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        EdgeToEdge.enable(mActivity);
        initMenu();

        initView();
        return root;
    }

    private void initView() {
        if (mJewishDateInfo == null) {
            mJewishDateInfo = new JewishDateInfo(mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("inIsrael", false));
        }
        TextView specialDay = binding.jewishSpecialDay;
        Calendar calendar = mJewishDateInfo.getJewishCalendar().getGregorianCalendar();
        String dateAndSpecialDay = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                + "\n" +
                mJewishDateInfo.getJewishCalendar().toString();
        String specialDayString = mJewishDateInfo.getSpecialDay(false);
        if (!specialDayString.isEmpty()) {
            dateAndSpecialDay += "\n" + specialDayString;
        }
        specialDay.setText(dateAndSpecialDay);

        Button selichot = binding.selichot;
        if (mJewishDateInfo.isSelichotSaid()) {
            selichot.setVisibility(View.VISIBLE);
        } else {
            selichot.setVisibility(View.GONE);
        }
        selichot.setOnClickListener(v -> startSiddurActivity(getString(R.string.selichot)));

        Button shacharit = binding.shacharit;
        shacharit.setOnClickListener(v -> startSiddurActivity(getString(R.string.shacharit)));

        Button mussaf = binding.mussaf;
        if (!(mJewishDateInfo.getJewishCalendar().isRoshChodesh()
                || mJewishDateInfo.getJewishCalendar().isCholHamoed())) {
            mussaf.setVisibility(View.GONE);
        } else {
            mussaf.setOnClickListener(v -> startSiddurActivity(getString(R.string.mussaf)));
        }

        Button mincha = binding.mincha;
        mincha.setOnClickListener(v -> startSiddurActivity(getString(R.string.mincha)));

        Button neilah = binding.neilah;
        //if (!sJewishDateInfo.getJewishCalendar().isYomKippur()) {
        neilah.setVisibility(View.GONE);
        //}

        TextView nightDayOfWeek = binding.nightDayOfWeek;
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

        Button arvit = binding.arvit;
        arvit.setOnClickListener(v -> startSiddurActivity(getString(R.string.arvit)));

        Button kriatShemaAlHamita = binding.kriatShemaAlHamita;
        kriatShemaAlHamita.setOnClickListener(v -> startNextDaySiddurActivity(getString(R.string.kriatShema), false));

        Button tikkunChatzot = binding.tikkunChatzot;
        Button tikkunChatzot3Weeks = binding.tikkunChatzot3Weeks;

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
                        || mJewishDateInfo.getIsTachanunSaid().equals("There is Tachanun today");
                if (mJewishDateInfo.isDayTikkunChatzotSaid() && isTachanunSaid) {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.do_you_want_to_say_tikkun_chatzot_for_the_day)
                            .setMessage(R.string.looking_to_say_this_version_of_tikkun_chatzot)
                            .setPositiveButton(mContext.getString(R.string.yes), (dialog, which) -> startSiddurActivity(mContext.getString(R.string.tikkun_chatzot)))
                            .setNegativeButton(mContext.getString(R.string.no), (dialog, which) -> startNextDaySiddurActivity(mContext.getString(R.string.tikkun_chatzot), true))
                            .show();
                } else {
                    mJewishDateInfo.setCalendar(calendarPlusOne);
                    if (mJewishDateInfo.isNightTikkunChatzotSaid()) {
                        startNextDaySiddurActivity(mContext.getString(R.string.tikkun_chatzot), true);
                    } else {
                        new MaterialAlertDialogBuilder(mContext)
                                .setTitle(R.string.tikkun_chatzot_is_not_said_today_or_tonight)
                                .setMessage(R.string.tikkun_chatzot_is_not_said_today_or_tonight_possible_reasons)
                                .setPositiveButton(mContext.getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                    mJewishDateInfo.setCalendar(calendar);
                }
            } else {
                mJewishDateInfo.setCalendar(calendarPlusOne);
                if (mJewishDateInfo.isNightTikkunChatzotSaid()) {
                    startNextDaySiddurActivity(mContext.getString(R.string.tikkun_chatzot), true);
                } else {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.tikkun_chatzot_is_not_said_tonight)
                            .setMessage(R.string.tikkun_chatzot_is_not_said_tonight_possible_reasons)
                            .setPositiveButton(mContext.getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                            .show();
                }
                mJewishDateInfo.setCalendar(calendar);
            }
        };

        tikkunChatzot.setOnClickListener(tikkunChatzotOnClickListener);
        tikkunChatzot3Weeks.setOnClickListener(tikkunChatzotOnClickListener);

        mZmanimCalendar = new ROZmanimCalendar(new GeoLocation("", sLatitude, sLongitude, sElevation, TimeZone.getDefault()));
        mZmanimCalendar.setCalendar(calendar);
        Date tzeit;
        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("LuachAmudeiHoraah", false)) {
            tzeit = mZmanimCalendar.getTzeitAmudeiHoraah();
        } else {
            tzeit = mZmanimCalendar.getTzeit();
        }
        if (new Date().after(tzeit) && new Date().before(mZmanimCalendar.getSolarMidnight())) {
            selichot.setBackground(null);
            selichot.setBackgroundColor(Color.GRAY);
        }
        String sunset = DateFormat.getTimeInstance(DateFormat.SHORT).format(mZmanimCalendar.getSunset());

        Button bh = binding.birchatHamazon;
        bh.setOnClickListener(v -> {
            JewishDateInfo tomorrow = new JewishDateInfo(mJewishDateInfo.getJewishCalendar().getInIsrael());
            Calendar nextDayCalendar = (Calendar) mJewishDateInfo.getJewishCalendar().getGregorianCalendar().clone();
            nextDayCalendar.add(Calendar.DATE, 1);
            tomorrow.setCalendar(nextDayCalendar);

            if (new SiddurMaker(mJewishDateInfo).getBirchatHamazonPrayers().equals(new SiddurMaker(tomorrow).getBirchatHamazonPrayers())) {
                startSiddurActivity(getString(R.string.birchat_hamazon));//doesn't matter which day
            } else {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.when_did_you_start_your_meal)
                        .setMessage(getString(R.string.did_you_start_your_meal_during_the_day) + " (" + sunset + ")")
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> startSiddurActivity(getString(R.string.birchat_hamazon)))
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> startNextDaySiddurActivity(getString(R.string.birchat_hamazon), false))
                        .show();
            }
        });

        Button bms = binding.birchatMeyinShalosh;
        bms.setOnClickListener(v -> {
            JewishDateInfo tomorrow = new JewishDateInfo(mJewishDateInfo.getJewishCalendar().getInIsrael());
            Calendar nextDayCalendar = (Calendar) mJewishDateInfo.getJewishCalendar().getGregorianCalendar().clone();
            nextDayCalendar.add(Calendar.DATE, 1);
            tomorrow.setCalendar(nextDayCalendar);

            if (new SiddurMaker(mJewishDateInfo).getBirchatMeeyinShaloshPrayers().equals(new SiddurMaker(tomorrow).getBirchatMeeyinShaloshPrayers())) {
                startSiddurActivity(getString(R.string.birchat_meyin_shalosh));//doesn't matter which day
            } else {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.when_did_you_start_your_meal)
                        .setMessage(getString(R.string.did_you_start_your_meal_during_the_day) + " (" + sunset + ")")
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> startSiddurActivity(getString(R.string.birchat_meyin_shalosh)))
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> startNextDaySiddurActivity(getString(R.string.birchat_meyin_shalosh), false))
                        .show();
            }
        });

        Button birchatLevana = binding.birchatHalevana;
        birchatLevana.setOnClickListener(v -> startSiddurActivity(getString(R.string.birchat_levana)));

        if (mJewishDateInfo.getBirchatLevana().isEmpty()) {// hide the button if there's no status text returned
            birchatLevana.setVisibility(View.GONE);
        }

        TextView disclaimer = binding.siddurDisclaimer;
        disclaimer.setGravity(Gravity.CENTER);
        disclaimer.setClickable(true);
        disclaimer.setMovementMethod(LinkMovementMethod.getInstance());

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
    }

    @Override
    public void onResume() {
        super.onResume();
        initMenu();
        initView();
    }

    private void initMenu() {
        if (materialToolbar == null) {
            return;
        }
        materialToolbar.setTitle(getString(R.string.show_siddur));
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            materialToolbar.setSubtitle("");
        } else {
            materialToolbar.setSubtitle(getString(R.string.short_app_name));
        }
        //materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(mContext, R.drawable.baseline_arrow_back_24)); // if you want to show the back button
        //materialToolbar.setNavigationOnClickListener(v -> finish());
        materialToolbar.getMenu().clear();
        materialToolbar.inflateMenu(R.menu.siddur_chooser_menu);
        materialToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.jerDirection) {
                startActivity(new Intent(mContext, JerusalemDirectionMapsActivity.class));
            }
            return false;
        });
    }

    private void startSiddurActivity(String prayer) {
        Intent intent = new Intent(mContext, SiddurViewActivity.class)
                .putExtra("prayer", prayer)
                .putExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
                .putExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth())
                .putExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear())
                .putExtra("isNightTikkunChatzot", false)
                .putExtra("isAfterChatzot", new Date().after(mZmanimCalendar.getSolarMidnight()));

        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.PURIM ||
                mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM &&
                        !(prayer.equals(getString(R.string.birchat_levana)) || prayer.equals(getString(R.string.tikkun_chatzot)) || prayer.equals(getString(R.string.kriatShema)))
        ) {// if the prayer is dependant on isMukafChoma, we ask the user
            SharedPreferences.Editor sharedPreferences = mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle(R.string.are_you_in_a_walled_mukaf_choma_city)
                    .setMessage(R.string.are_you_located_in_a_walled_mukaf_choma_city_from_the_time_of_yehoshua_bin_nun)
                    .setPositiveButton(getString(R.string.yes) + " (" + getString(R.string.jerusalem) + ")", (dialog, which) -> {
                        sharedPreferences.putBoolean("isMukafChoma", true).apply();
                        sharedPreferences.putBoolean("isSafekMukafChoma", false).apply();
                        startActivity(intent);
                    })
                    .setNeutralButton(R.string.safek_doubt, (dialog, which) -> {
                        sharedPreferences.putBoolean("isMukafChoma", false).apply();
                        sharedPreferences.putBoolean("isSafekMukafChoma", true).apply();
                        startActivity(intent);
                    })
                    .setNegativeButton(getString(R.string.no), (dialog, which) -> startActivity(intent))
                    .show();
        } else {
            startActivity(intent);
        }
    }

    private void startNextDaySiddurActivity(String prayer, boolean isNightTikkunChatzot) {
        mJewishDateInfo.getJewishCalendar().forward(Calendar.DATE, 1);
        Intent intent = new Intent(mContext, SiddurViewActivity.class)
                .putExtra("prayer", prayer)
                .putExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
                .putExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth())
                .putExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear())
                .putExtra("isBeforeChatzot", new Date().before(mZmanimCalendar.getSolarMidnight()))
                .putExtra("isNightTikkunChatzot", isNightTikkunChatzot);
        mJewishDateInfo.getJewishCalendar().back();//reset

        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.PURIM ||
                mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM &&
                        !(prayer.equals(getString(R.string.birchat_levana)) || prayer.equals(getString(R.string.tikkun_chatzot)) || prayer.equals(getString(R.string.kriatShema)))
        ) {// if the prayer is dependant on isMukafChoma, we ask the user
            SharedPreferences.Editor sharedPreferences = mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle(R.string.are_you_in_a_walled_mukaf_choma_city)
                    .setMessage(R.string.are_you_located_in_a_walled_mukaf_choma_city_from_the_time_of_yehoshua_bin_nun)
                    .setPositiveButton(getString(R.string.yes) + " (" + getString(R.string.jerusalem) + ")", (dialog, which) -> {
                        sharedPreferences.putBoolean("isMukafChoma", true).apply();
                        sharedPreferences.putBoolean("isSafekMukafChoma", false).apply();
                        startActivity(intent);
                    })
                    .setNeutralButton(R.string.safek_doubt, (dialog, which) -> {
                        sharedPreferences.putBoolean("isMukafChoma", false).apply();
                        sharedPreferences.putBoolean("isSafekMukafChoma", true).apply();
                        startActivity(intent);
                    })
                    .setNegativeButton(getString(R.string.no), (dialog, which) -> startActivity(intent))
                    .show();
        } else {
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}