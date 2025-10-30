package com.ej.rovadiahyosefcalendar.activities;

import static android.view.View.LAYOUT_DIRECTION_LTR;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLongitude;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
        RadioGroup group = findViewById(R.id.radioGroup);
        RadioButton hebrew = findViewById(R.id.hebrew);
        RadioButton english = findViewById(R.id.english);
        CheckBox englishTranslated = findViewById(R.id.englishTranslated);

        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            isHebrew = true;
            group.check(R.id.hebrew);
            englishTranslated.setEnabled(false);
        } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
            translated = true;
            group.check(R.id.english);
            englishTranslated.setEnabled(true);
        } else {
            group.check(R.id.english);
            englishTranslated.setEnabled(true);
        }
        englishTranslated.setChecked(translated);
        updateRecyclerView();

        hebrew.setOnClickListener(v -> {
            isHebrew = true;
            translated = false;
            englishTranslated.setChecked(false);
            englishTranslated.setEnabled(false);
            updateRecyclerView();
        });
        english.setOnClickListener(v -> {
            isHebrew = false;
            translated = false;
            englishTranslated.setChecked(false);
            englishTranslated.setEnabled(true);
            updateRecyclerView();
        });
        englishTranslated.setOnCheckedChangeListener((buttonView, isChecked) -> {
            translated = isChecked;
            updateRecyclerView();
        });

        Button confirm = findViewById(R.id.confirm);
        confirm.setOnClickListener(v -> saveInfoAndFinish(isHebrew, translated));

        ViewCompat.setOnApplyWindowInsetsListener(englishTranslated, (v, windowInsets) -> {
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

    private void saveInfoAndFinish(boolean isHebrew, boolean isTranslated) {
        mSharedPreferences.edit().putBoolean("isZmanimInHebrew", isHebrew).apply();
        mSharedPreferences.edit().putBoolean("isZmanimEnglishTranslated", isTranslated).apply();
        mSharedPreferences.edit().putBoolean("isSetup", true).apply();
        if (mSharedPreferences.getBoolean("hasNotShownTipScreen", true)) {
            startActivity(new Intent(getBaseContext(), TipScreenActivity.class));
            mSharedPreferences.edit().putBoolean("hasNotShownTipScreen", false).apply();
        }
        finish();
    }

    private void updateRecyclerView() {
        if (isHebrew) {
            mRecyclerView.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        } else {
            mRecyclerView.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        }
        mRecyclerView.setAdapter(new ZmanAdapter(this, ZmanimFactory.getDemoZmanim(isHebrew, translated), null));
    }
}