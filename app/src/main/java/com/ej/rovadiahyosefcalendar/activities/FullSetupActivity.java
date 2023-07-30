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
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.ej.rovadiahyosefcalendar.R;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class FullSetupActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_setup);
        LinearLayout layout = findViewById(R.id.israel_buttons);
        float screenWidth = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
        if (screenWidth < 400) {
            layout.setOrientation(LinearLayout.VERTICAL);
        }
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        Button inIsraelButton = findViewById(R.id.inIsraelButton);
        Button notInIsrael = findViewById(R.id.notInIsraelButton);

        inIsraelButton.setOnClickListener(v -> saveInfoAndStartActivity(true));
        notInIsrael.setOnClickListener(v -> saveInfoAndStartActivity(false));
    }

    private void saveInfoAndStartActivity(boolean b) {
        mSharedPreferences.edit().putBoolean("inIsrael", b).apply();
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            mSharedPreferences.edit().putBoolean("isZmanimInHebrew", true).apply();
            mSharedPreferences.edit().putBoolean("isZmanimEnglishTranslated", false).apply();
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
                    !mSharedPreferences.getBoolean("useZipcode", false)) {
                startActivity(new Intent(this, CurrentLocationActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
            }
            mSharedPreferences.edit().putBoolean("isSetup", true).apply();
        } else {
            startActivity(new Intent(this, ZmanimLanguageActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_FORWARD_RESULT));
        }
        finish();
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}