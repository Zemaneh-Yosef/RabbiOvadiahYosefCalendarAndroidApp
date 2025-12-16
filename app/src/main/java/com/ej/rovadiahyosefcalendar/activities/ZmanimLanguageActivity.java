package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.classes.ZmanAdapter;
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ZmanimLanguageActivity extends AppCompatActivity {

    SharedPreferences mSharedPreferences;
    boolean isHebrew = false;
    boolean translated = false;
    boolean americanized = false;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zmanim_language);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        if (Utils.isLocaleHebrew()) {
            materialToolbar.setSubtitle("");
        }
        materialToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.help) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.help_using_this_app)
                    .setPositiveButton(R.string.ok, null)
                    .setMessage(R.string.helper_text)
                    .show();
                return true;
            } else if (id == R.id.skipSetup) {
                finish();
                return true;
            } else if (id == R.id.restart) {
                startActivity(new Intent(this, WelcomeScreenActivity.class));
                finish();
                return true;
            }
            return false;
        });

        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        mRecyclerView = findViewById(R.id.zmanim_demo_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        RadioGroup group = findViewById(R.id.radioGroup);
        RadioButton hebrew = findViewById(R.id.hebrew);
        RadioButton english = findViewById(R.id.english);
        Spinner spinner = findViewById(R.id.englishLangSelector);

        spinner.setAdapter(new ArrayAdapter<>(this, R.layout.custom_spinner_item, new String[] {
                "Translation",
                "Transliteration (Sepharadic Articulation)",
                "Transliteration (American Articulation)"}));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    translated = true;
                    americanized = false;
                } else if (position == 1) {
                    translated = false;
                    americanized = false;
                } else {
                    translated = false;
                    americanized = true;
                }
                updateRecyclerView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            isHebrew = true;
            group.check(R.id.hebrew);
        } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
            translated = true;
            group.check(R.id.english);
            spinner.setVisibility(View.VISIBLE);
        } else if (mSharedPreferences.getBoolean("isZmanimAmericanized", false)) {
            americanized = true;
            group.check(R.id.english);
            spinner.setVisibility(View.VISIBLE);
        } else {
            group.check(R.id.english);
            spinner.setVisibility(View.VISIBLE);
        }
        if (translated) {
            spinner.setSelection(0);
        } else if (americanized) {
            spinner.setSelection(2);
        } else {
            spinner.setSelection(1);
        }
        updateRecyclerView();

        hebrew.setOnClickListener(v -> {
            isHebrew = true;
            translated = false;
            americanized = false;
            group.clearCheck();
            group.check(R.id.hebrew);
            spinner.setVisibility(View.GONE);
            updateRecyclerView();
        });
        english.setOnClickListener(v -> {
            isHebrew = false;
            group.clearCheck();
            group.check(R.id.english);
            spinner.setVisibility(View.VISIBLE);
            spinner.setSelection(1);
            updateRecyclerView();
        });

        Button confirm = findViewById(R.id.confirm);
        confirm.setOnClickListener(v -> saveInfoAndFinish(isHebrew, translated, americanized));

        ViewCompat.setOnApplyWindowInsetsListener(confirm, (v, windowInsets) -> {
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (Utils.isInOrNearIsrael(sLatitude, sLongitude)) {
                    startActivity(new Intent(ZmanimLanguageActivity.this, InIsraelActivity.class));
                } else {
                    startActivity(new Intent(ZmanimLanguageActivity.this, GetUserLocationWithMapActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                }
                finish();
            }
        });
    }

    private void saveInfoAndFinish(boolean isHebrew, boolean isTranslated, boolean isAmericanized) {
        mSharedPreferences.edit().putBoolean("isZmanimInHebrew", isHebrew).apply();
        mSharedPreferences.edit().putBoolean("isZmanimEnglishTranslated", isTranslated).apply();
        mSharedPreferences.edit().putBoolean("isZmanimAmericanized", isAmericanized).apply();
        mSharedPreferences.edit().putBoolean("isSetup", true).apply();
        if (mSharedPreferences.getBoolean("hasNotShownTipScreen", true)) {
            startActivity(new Intent(getBaseContext(), TipScreenActivity.class));
            mSharedPreferences.edit().putBoolean("hasNotShownTipScreen", false).apply();
        }
        finish();
    }

    private void updateRecyclerView() {
        ZmanAdapter adapter = new ZmanAdapter(this, ZmanimFactory.getDemoZmanim(isHebrew, translated, americanized), null);
        adapter.isZmanimInHebrew = isHebrew;
        adapter.isZmanimEnglishTranslated = translated;
        adapter.isZmanimAmericanized = americanized;
        mRecyclerView.setAdapter(adapter);
    }
}