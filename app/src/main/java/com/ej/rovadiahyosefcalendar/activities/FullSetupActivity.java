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

public class FullSetupActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private float mElevationBackup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_setup);
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        if (getIntent().getBooleanExtra("fromMenu", false)) {
            mSharedPreferences.edit().putBoolean("isSetup", false).apply();
            mElevationBackup = mSharedPreferences.getFloat("elevation", 0);
            mSharedPreferences.edit().putFloat("elevation", 0).apply();
        }

        Button inIsraelButton = findViewById(R.id.inIsraelButton);
        Button notInIsrael = findViewById(R.id.notInIsraelButton);

        inIsraelButton.setOnClickListener(v -> saveInfoAndStartActivity(true));
        notInIsrael.setOnClickListener(v -> saveInfoAndStartActivity(false));
    }

    private void saveInfoAndStartActivity(boolean b) {
        mSharedPreferences.edit().putBoolean("inIsrael", b).apply();
        startActivity(new Intent(this, ZmanimLanguageActivity.class).setFlags(
                Intent.FLAG_ACTIVITY_FORWARD_RESULT));
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

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra("fromMenu", false)) {
            getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit().putBoolean("isSetup", true).apply();//undo what we did in the onCreate method
            getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit().putFloat("elevation", mElevationBackup).apply();
            finish();
        }
        super.onBackPressed();
    }
}