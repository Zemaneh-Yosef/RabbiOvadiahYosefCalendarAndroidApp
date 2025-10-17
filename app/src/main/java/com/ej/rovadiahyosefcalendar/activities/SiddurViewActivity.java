package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.WindowCompat;
import androidx.preference.PreferenceManager;
import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.HighlightString;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.SiddurComposeView; // Import your custom view
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.Objects;

public class SiddurViewActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_siddur_view);
        sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        // --- 1. SETUP THE TOOLBAR (Unchanged) ---
        String siddurTitle = getIntent().getStringExtra("prayer");
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        if (siddurTitle != null) {
            materialToolbar.setTitle((!siddurTitle.isEmpty() ? siddurTitle : getString(R.string.show_siddur)));
        }

        // --- 2. LOAD THE DATA (Unchanged) ---
        JewishDateInfo mJewishDateInfo = new JewishDateInfo(sharedPreferences.getBoolean("inIsrael", false));
        mJewishDateInfo.getJewishCalendar().setJewishDate(
            getIntent().getIntExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear()),
            getIntent().getIntExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth()),
            getIntent().getIntExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
        );
        mJewishDateInfo.setCalendar(mJewishDateInfo.getJewishCalendar().getGregorianCalendar());
        mJewishDateInfo.getJewishCalendar().setIsMukafChoma(sharedPreferences.getBoolean("isMukafChoma", false));
        mJewishDateInfo.getJewishCalendar().setIsSafekMukafChoma(sharedPreferences.getBoolean("isSafekMukafChoma", false));
        SiddurMaker siddurMaker = new SiddurMaker(mJewishDateInfo, 0);
        ArrayList<HighlightString> prayers = new ArrayList<>();
        if (siddurTitle != null) {
            prayers = switch (siddurTitle) {
                // ... switch statement is unchanged ...
                case "סליחות" -> siddurMaker.getSelichotPrayers(getIntent().getBooleanExtra("isAfterChatzot", false));
                case "שחרית" -> siddurMaker.getShacharitPrayers();
                case "מוסף" -> siddurMaker.getMusafPrayers();
                case "מנחה" -> siddurMaker.getMinchaPrayers();
                case "ערבית" -> siddurMaker.getArvitPrayers();
                case "ספירת העומר" -> siddurMaker.getSefiratHaOmerPrayers();
                case "הדלקת נרות חנוכה" -> siddurMaker.getHadlakatNeirotChanukaPrayers();
                case "הבדלה" -> siddurMaker.getHavdalahPrayers();
                case "סדר סיום מסכת" -> siddurMaker.getSiyumMasechetPrayer(Objects.requireNonNull(getIntent().getStringArrayExtra("masechtas")));
                case "ברכת המזון" -> siddurMaker.getBirchatHamazonPrayers();
                case "תפלת הדרך" -> siddurMaker.getTefilatHaderechPrayer();
                case "ברכת הלבנה" -> siddurMaker.getBirchatHalevanaPrayers();
                case "תיקון חצות" -> siddurMaker.getTikkunChatzotPrayers(getIntent().getBooleanExtra("isNightTikkunChatzot", true));
                case "ק״ש שעל המיטה" -> siddurMaker.getKriatShemaShealHamitaPrayers(getIntent().getBooleanExtra("isBeforeChatzot", false));
                case "ברכת מעין שלוש" -> siddurMaker.getBirchatMeeyinShaloshPrayers(Objects.requireNonNull(getIntent().getStringArrayExtra("itemsForMeyinShalosh")));
                default -> prayers;
            };
        }

        // --- 3. FIND THE CUSTOM VIEW AND SET ITS DATA ---
        SiddurComposeView siddurView = findViewById(R.id.siddur_compose_view);

        // --- THIS IS THE FIX ---
        // DELETE the setContent block. It does not exist on our custom view.
        // The setData method is all you need.
        siddurView.setData(prayers, mJewishDateInfo);
        // --- END OF FIX ---


        // --- 4. KEEP SCREEN-ON LOGIC (Unchanged) ---
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("siddurAlwaysOn", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
