package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLongitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSharedPreferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ZmanimLanguageActivity extends AppCompatActivity {

    SharedPreferences mSharedPreferences;
    boolean translated;

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

        ImageView imageView = findViewById(R.id.langImageView);
        RadioGroup group = findViewById(R.id.radioGroup);
        RadioButton hebrew = findViewById(R.id.hebrew);
        RadioButton english = findViewById(R.id.english);
        CheckBox englishTranslated = findViewById(R.id.englishTranslated);

        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            group.check(R.id.hebrew);
            englishTranslated.setEnabled(false);
            englishTranslated.setChecked(false);
            imageView.setImageResource(R.drawable.hebrew);
        } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
            translated = true;
            group.check(R.id.english);
            englishTranslated.setEnabled(true);
            englishTranslated.setChecked(true);
            imageView.setImageResource(R.drawable.translated);
        } else {
            group.check(R.id.english);
            englishTranslated.setEnabled(true);
            imageView.setImageResource(R.drawable.english);
        }

        hebrew.setOnClickListener(v -> {
            englishTranslated.setChecked(false);
            englishTranslated.setEnabled(false);
            imageView.setImageResource(R.drawable.hebrew);
        });
        english.setOnClickListener(v -> {
            englishTranslated.setChecked(false);
            englishTranslated.setEnabled(true);
            imageView.setImageResource(R.drawable.english);
        });
        englishTranslated.setOnCheckedChangeListener((buttonView, isChecked) -> {
            translated = isChecked;
            if (isChecked) {
                imageView.setImageResource(R.drawable.translated);
            } else {
                imageView.setImageResource(R.drawable.english);
            }
        });

        Button confirm = findViewById(R.id.confirm);
        confirm.setOnClickListener(v -> saveInfoAndFinish(group.getCheckedRadioButtonId() == R.id.hebrew, translated));

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
        sSharedPreferences.edit().putBoolean("isSetup", true).apply();
        finish();
    }
}