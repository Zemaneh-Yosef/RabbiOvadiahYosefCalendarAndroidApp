package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;

import org.jetbrains.annotations.NotNull;

public class ZmanimLanguageActivity extends AppCompatActivity {

    SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zmanim_language);
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        Button hebrew = findViewById(R.id.hebrew);
        Button english = findViewById(R.id.english);

        String hebrewText = "\u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8" + "\n" +
                "\u05D8\u05DC\u05D9\u05EA \u05D5\u05EA\u05E4\u05D9\u05DC\u05D9\u05DF" + "\n" +
                "\u05D4\u05E0\u05E5" + "\n" + "\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF" +
                " \u05E9\u05DE\u05E2 \u05DE\u05D2\"\u05D0" + "\n" + "\u05E1\u05D5\u05E3 " +
                "\u05D6\u05DE\u05DF \u05E9\u05DE\u05E2 \u05D2\u05E8\"\u05D0" + "\n" +
                "\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05D1\u05E8\u05DB\u05D5\u05EA " +
                "\u05E9\u05DE\u05E2" + "\n" + "\u05D7\u05E6\u05D5\u05EA" + "\n" +
                "\u05D5\u05D2\u05D5...";
        hebrew.setText(hebrewText);

        String englishText = "Alot Hashachar" + "\n" +
                "Earliest Talit/Tefilin" + "\n" +
                "HaNetz" + "\n" +
                "Sof Zman Shma Mg'a" + "\n" +
                "Sof Zman Shma Gr'a" + "\n" +
                "Sof Zman Brachot Shma" + "\n" +
                "Chatzot" + "\n" +
                "etc...";
        english.setText(englishText);

        hebrew.setOnClickListener(v -> saveInfoAndStartActivity(true, false));
        english.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Translate english zmanim?")
                .setMessage("Would you like to have the zmanim translated? \n\nFor Example: \"Alot Hashachar\" will become \"Dawn\"")
                .setPositiveButton("Yes", (dialogInterface, i) ->
                        saveInfoAndStartActivity(false, true))
                .setNegativeButton("No", (dialogInterface, i) ->
                        saveInfoAndStartActivity(false, false))
                .show());
    }

    private void saveInfoAndStartActivity(boolean isHebrew, boolean isTranslated) {
        mSharedPreferences.edit().putBoolean("isZmanimInHebrew", isHebrew).apply();
        mSharedPreferences.edit().putBoolean("isZmanimEnglishTranslated", isTranslated).apply();
        startActivity(new Intent(this, SetupChooserActivity.class).setFlags(
                Intent.FLAG_ACTIVITY_FORWARD_RESULT));
        finish();
    }
    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, FullSetupActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.help) {
            new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight)
                    .setTitle("Help using this app:")
                    .setPositiveButton("ok", null)
                    .setMessage(R.string.helper_text)
                    .show();
            return true;
        } else if (id == R.id.restart) {
            startActivity(new Intent(this, FullSetupActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}