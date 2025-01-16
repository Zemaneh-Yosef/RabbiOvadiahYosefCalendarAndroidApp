package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.CategoryOnClick;
import com.ej.rovadiahyosefcalendar.classes.HighlightString;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.SiddurAdapter;
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

        JewishDateInfo mJewishDateInfo = new JewishDateInfo(sharedPreferences.getBoolean("inIsrael", false));
        mJewishDateInfo.getJewishCalendar().setJewishDate(
                getIntent().getIntExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear()),
                getIntent().getIntExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth()),
                getIntent().getIntExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
        );
        mJewishDateInfo.setCalendar(mJewishDateInfo.getJewishCalendar().getGregorianCalendar());// not my best work
        mJewishDateInfo.getJewishCalendar().setIsMukafChoma(sharedPreferences.getBoolean("isMukafChoma", false));
        mJewishDateInfo.getJewishCalendar().setIsSafekMukafChoma(sharedPreferences.getBoolean("isSafekMukafChoma", false));

        SiddurMaker siddurMaker = new SiddurMaker(mJewishDateInfo);
        ArrayList<HighlightString> prayers = new ArrayList<>();
        if (siddurTitle != null) {
            prayers = switch (siddurTitle) {
                case "סליחות" -> siddurMaker.getSelichotPrayers(false, getIntent().getBooleanExtra("isAfterChatzot", false));
                case "שחרית" -> siddurMaker.getShacharitPrayers();
                case "מוסף" -> siddurMaker.getMusafPrayers();
                case "מנחה" -> siddurMaker.getMinchaPrayers();
                case "ערבית" -> siddurMaker.getArvitPrayers();
                case "הדלקת נרות חנוכה" -> siddurMaker.getHadlakatNeirotChanukaPrayers();
                case "הבדלה" -> siddurMaker.getHavdalah();
                case "ברכת המזון" -> siddurMaker.getBirchatHamazonPrayers();
                case "ברכת הלבנה" -> siddurMaker.getBirchatHalevanaPrayers();
                case "תיקון חצות" -> siddurMaker.getTikkunChatzotPrayers(getIntent().getBooleanExtra("isNightTikkunChatzot", true));
                case "ק״ש שעל המיטה" -> siddurMaker.getKriatShemaShealHamitaPrayers(getIntent().getBooleanExtra("isBeforeChatzot", false));
                case "ברכת מעין שלוש" -> siddurMaker.getBirchatMeeyinShaloshPrayers();
                default -> prayers;
            };
        }
        ListView siddur = findViewById(R.id.siddur);
        siddur.setAdapter(new SiddurAdapter(this,
                prayers,
                sharedPreferences.getInt("siddurTextSize", 20),
                sharedPreferences.getBoolean("isJustified", false),
                mJewishDateInfo));
        siddur.setDivider(null);
        Map<Integer, HighlightString> categories = new LinkedHashMap<>();
        int index = 0;
        for (HighlightString string: prayers) {
            if (string.isCategory()) {
                if (categories.containsValue(string)) {
                    string.setString(string + "\u200E");// this unicode character is invisible. So it will always increment without showing in the UI
                }
                categories.put(index, string);
                index++;
            }
        }
        if (!categories.isEmpty()) {
            materialToolbar.setTitle(materialToolbar.getTitle() + "▼");
        }
        ArrayList<HighlightString> finalPrayers = prayers;
        CategoryOnClick.OnItemClickListener listener = category -> {
            int position = finalPrayers.indexOf(category);
            if (position != -1) {
                siddur.setSelection(position);
            }
        };
        materialToolbar.setOnClickListener(v -> showPopupMenu(v, new ArrayList<>(categories.values()), listener));

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

        Button textAlignment = findViewById(R.id.textAlignment);
        textAlignment.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, sharedPreferences.getBoolean("isJustified", false) ? R.drawable.baseline_format_align_justify_24 : R.drawable.baseline_format_align_right_24);
        textAlignment.setOnClickListener(v -> {
            boolean isJustified = sharedPreferences.getBoolean("isJustified", false);
            isJustified = !isJustified;
            sharedPreferences.edit().putBoolean("isJustified", isJustified).apply();
            textAlignment.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, isJustified ? R.drawable.baseline_format_align_justify_24 : R.drawable.baseline_format_align_right_24);
            SiddurAdapter sa = (SiddurAdapter) siddur.getAdapter();
            sa.setIsJustified(isJustified);
            siddur.invalidateViews();
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

    private void showPopupMenu(View view, ArrayList<HighlightString> categories, CategoryOnClick.OnItemClickListener listener) {
        final ViewGroup root = getWindow().getDecorView().findViewById(android.R.id.content);
        final View empty = new View(this);
        empty.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        empty.setBackgroundColor(Color.TRANSPARENT);

        root.addView(empty);

        empty.setX(view.getX() + (view.getWidth() / 3f));
        empty.setY(0);

        PopupMenu popupMenu = new PopupMenu(this, empty);

        // Add items to the PopupMenu
        for (HighlightString item : categories) {
            popupMenu.getMenu().add(item.toString());
        }

        // Set a listener for item selection
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getTitle() != null) {
                String selectedItem = menuItem.getTitle().toString();
                Optional<HighlightString> result = categories.stream()
                        .filter(obj -> obj.toString().equals(selectedItem))
                        .findFirst();
                result.ifPresent(listener::onItemClick);
            }
            return true;
        });

        // Show the PopupMenu
        popupMenu.show();
    }
}