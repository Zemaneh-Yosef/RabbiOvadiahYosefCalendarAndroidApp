package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
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
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_siddur_view);
        sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        // --- 1. SETUP THE TOOLBAR ---
        String siddurTitle = getIntent().getStringExtra("prayer");
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        if (siddurTitle != null) {
            materialToolbar.setTitle((!siddurTitle.isEmpty() ? siddurTitle : getString(R.string.show_siddur)));
        }

        // --- 2. SETUP JEWISH DATE AND LOAD PRAYERS ---
        JewishDateInfo mJewishDateInfo = new JewishDateInfo(sharedPreferences.getBoolean("inIsrael", false));
        mJewishDateInfo.getJewishCalendar().setJewishDate(
            getIntent().getIntExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear()),
            getIntent().getIntExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth()),
            getIntent().getIntExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
        );
        mJewishDateInfo.setCalendar(mJewishDateInfo.getJewishCalendar().getGregorianCalendar());
        mJewishDateInfo.getJewishCalendar().setIsMukafChoma(sharedPreferences.getBoolean("isMukafChoma", false));
        mJewishDateInfo.getJewishCalendar().setIsSafekMukafChoma(sharedPreferences.getBoolean("isSafekMukafChoma", false));

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int textColor = ContextCompat.getColor(this, typedValue.resourceId);

        SiddurMaker siddurMaker = new SiddurMaker(mJewishDateInfo, textColor);
        assert siddurTitle != null;
        ArrayList<HighlightString> prayers = switch (siddurTitle) {
            case "סליחות" -> siddurMaker.getSelichotPrayers(getIntent().getBooleanExtra("isAfterChatzot", false), getIntent().getBooleanExtra("isAfterSunrise", false));
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
            case "ק״ש שעל המיטה" -> siddurMaker.getKriatShemaShealHamitaPrayers(!getIntent().getBooleanExtra("isAfterChatzot", false));
            case "ברכת מעין שלוש" -> siddurMaker.getBirchatMeeyinShaloshPrayers(Objects.requireNonNull(getIntent().getStringArrayExtra("itemsForMeyinShalosh")));
            default -> new ArrayList<>();
        };

        // --- 3. FIND THE COMPOSE VIEW ---
        SiddurComposeView siddurView = findViewById(R.id.siddur_compose_view);

        // --- 4. SETUP THE JUMP-TO MENU ---
        // Create a list of category names and a parallel list of their exact positions in the `prayers` list.
        ArrayList<String> categoryNames = new ArrayList<>();
        ArrayList<Integer> categoryPositions = new ArrayList<>();
        for (int i = 0; i < prayers.size(); i++) {
            if (prayers.get(i).getType() == HighlightString.StringType.CATEGORY) {
                categoryNames.add(prayers.get(i).toString());
                categoryPositions.add(i); // Store the index from the main `prayers` list
            }
        }

        if (!categoryNames.isEmpty()) {
            materialToolbar.inflateMenu(R.menu.siddur_menu);
            materialToolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.siddur_categories) {
                    PopupMenu popupMenu = new PopupMenu(this, materialToolbar, android.view.Gravity.END);

                    // Add each category to the menu. Use its index in our new lists as its unique ID.
                    for (int i = 0; i < categoryNames.size(); i++) {
                        popupMenu.getMenu().add(0, i, i, categoryNames.get(i));
                    }

                    // When an item is clicked, use its ID to get the correct position.
                    popupMenu.setOnMenuItemClickListener(menuItem -> {
                        int indexInCategoriesList = menuItem.getItemId(); // This is the index (0, 1, 2, ...)

                        if (indexInCategoriesList >= 0 && indexInCategoriesList < categoryPositions.size()) {
                            // Get the real position from our positions list
                            int positionToScrollTo = categoryPositions.get(indexInCategoriesList);
                            siddurView.scrollToPosition(positionToScrollTo);
                        }
                        return true;
                    });

                    popupMenu.show();
                    return true;
                }
                return false;
            });
        }

        // --- 5. SET THE DATA ON THE COMPOSE VIEW ---
        siddurView.setData(prayers, mJewishDateInfo);

        // --- 6. KEEP SCREEN-ON LOGIC ---
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("siddurAlwaysOn", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

}
