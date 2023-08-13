package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.ej.rovadiahyosefcalendar.R;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ZmanimLanguageActivity extends AppCompatActivity {

    SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zmanim_language);
        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_custom);//center the title
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        Button hebrew = findViewById(R.id.hebrew);
        Button english = findViewById(R.id.english);
        Button englishTranslated = findViewById(R.id.englishTranslated);

        String hebrewText = "עלות השחר\n" +
                "תלית ותפילין\n" +
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
                "Sof Zman Shma Mg'a" + "\n" +
                "Sof Zman Shma Gr'a" + "\n" +
                "Sof Zman Brachot Shma" + "\n" +
                "Chatzot" + "\n" +
                "Mincha Gedola" + "\n" +
                "Mincha Ketana" + "\n" +
                "Plag HaMincha" + "\n" +
                "Shkia" + "\n" +
                "Tzeit Hacochavim" + "\n" +
                "etc...";
        english.setText(englishText);

        String englishTranslatedText = "Dawn" + "\n" +
                "Earliest Talit/Tefilin" + "\n" +
                "Sunrise" + "\n" +
                "Latest Shma Mg'a" + "\n" +
                "Latest Shma Gr'a" + "\n" +
                "Latest Brachot Shma" + "\n" +
                "Mid-Day" + "\n" +
                "Earliest Mincha" + "\n" +
                "Mincha Ketana" + "\n" +
                "Plag HaMincha" + "\n" +
                "Sunset" + "\n" +
                "Nightfall" + "\n" +
                "etc...";
        englishTranslated.setText(englishTranslatedText);

        hebrew.setOnClickListener(v -> saveInfoAndStartActivity(true, false));
        english.setOnClickListener(v -> saveInfoAndStartActivity(false, false));
        englishTranslated.setOnClickListener(v -> saveInfoAndStartActivity(false, true));
    }

    private void saveInfoAndStartActivity(boolean isHebrew, boolean isTranslated) {
        mSharedPreferences.edit().putBoolean("isZmanimInHebrew", isHebrew).apply();
        mSharedPreferences.edit().putBoolean("isZmanimEnglishTranslated", isTranslated).apply();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
                !mSharedPreferences.getBoolean("useZipcode", false)) {
            startActivity(new Intent(this, CurrentLocationActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
            finish();
            return;
        }
        if (!mSharedPreferences.getBoolean("inIsrael", false)) {
            startActivity(new Intent(this, CalendarChooserActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
        }
        mSharedPreferences.edit().putBoolean("isSetup", true).apply();
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
            new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_DayNight)
                    .setTitle(R.string.help_using_this_app)
                    .setPositiveButton(R.string.ok, null)
                    .setMessage(R.string.helper_text)
                    .show();
            return true;
        } else if (id == R.id.skipSetup) {
            finish();
            return true;
        }  else if (id == R.id.restart) {
            startActivity(new Intent(this, FullSetupActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}