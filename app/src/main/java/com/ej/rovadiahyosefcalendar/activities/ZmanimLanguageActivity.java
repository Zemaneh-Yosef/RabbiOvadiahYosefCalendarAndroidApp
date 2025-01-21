package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.LocaleChecker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ZmanimLanguageActivity extends AppCompatActivity {

    SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zmanim_language);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        if (LocaleChecker.isLocaleHebrew()) {
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
                startActivity(new Intent(this, FullSetupActivity.class));
                finish();
                return true;
            }
            return false;
        });

        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        Button hebrew = findViewById(R.id.hebrew);
        Button english = findViewById(R.id.english);
        Button englishTranslated = findViewById(R.id.englishTranslated);

        String hebrewText = "עלות השחר\n" +
                "טלית ותפילין\n" +
                "הנץ\n" +
                "סוף זמן שמע מג\"א\n" +
                "סוף זמן שמע גר\"א\n" +
                "סוף זמן ברכות שמע\n" +
                "חצות\n" +
                "מנחה גדולה\n" +
                "מנחה קטנה\n" +
                "פלג המנחה\n" +
                "שקיעה\n" +
                "צאת הכוכבים\n" +
                "וגו...";
        hebrew.setText(hebrewText);

        String englishText = "Alot Hashachar" + "\n" +
                "Earliest Talit/Tefilin" + "\n" +
                "HaNetz" + "\n" +
                "Sof Zeman Shema MG'A" + "\n" +
                "Sof Zeman Shema GR'A" + "\n" +
                "Sof Zeman Berakhot Shema" + "\n" +
                "Ḥatzot" + "\n" +
                "Minḥa Gedola" + "\n" +
                "Minḥa Ketana" + "\n" +
                "Plag HaMinḥa" + "\n" +
                "Sheqi'a" + "\n" +
                "Tzet Hakokhavim" + "\n" +
                "etc...";
        english.setText(englishText);

        String englishTranslatedText = "Dawn" + "\n" +
                "Earliest Talit/Tefilin" + "\n" +
                "Sunrise" + "\n" +
                "Latest Shema MG'A" + "\n" +
                "Latest Shema GR'A" + "\n" +
                "Latest Berakhot Shema" + "\n" +
                "Mid-Day" + "\n" +
                "Earliest Minḥa" + "\n" +
                "Minḥa Ketana" + "\n" +
                "Plag HaMinḥa" + "\n" +
                "Sunset" + "\n" +
                "Nightfall" + "\n" +
                "etc...";
        englishTranslated.setText(englishTranslatedText);

        hebrew.setOnClickListener(v -> saveInfoAndStartActivity(true, false));
        english.setOnClickListener(v -> saveInfoAndStartActivity(false, false));
        englishTranslated.setOnClickListener(v -> saveInfoAndStartActivity(false, true));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.lang_options), (v, windowInsets) -> {
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
                startActivity(new Intent(ZmanimLanguageActivity.this, FullSetupActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                finish();
            }
        });
    }

    private void saveInfoAndStartActivity(boolean isHebrew, boolean isTranslated) {
        mSharedPreferences.edit().putBoolean("isZmanimInHebrew", isHebrew).apply();
        mSharedPreferences.edit().putBoolean("isZmanimEnglishTranslated", isTranslated).apply();
        if ((ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED)
                && !mSharedPreferences.getBoolean("useZipcode", false)) {
            startActivity(new Intent(this, GetUserLocationWithMapActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
            finish();
            return;
        }
        if (!mSharedPreferences.getBoolean("inIsrael", false)) {
            startActivity(new Intent(this, CalendarChooserActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
        }
        mSharedPreferences.edit().putBoolean("isSetup", true).apply();
        finish();
    }
}