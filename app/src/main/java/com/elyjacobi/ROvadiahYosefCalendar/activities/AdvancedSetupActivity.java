package com.elyjacobi.ROvadiahYosefCalendar.activities;

import static com.elyjacobi.ROvadiahYosefCalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.elyjacobi.ROvadiahYosefCalendar.R;

import org.jetbrains.annotations.NotNull;

public class AdvancedSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_setup);
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();

        Button setupBoth = findViewById(R.id.setup_both);
        Button setupElevation = findViewById(R.id.setup_elevation);
        Button setupVisibleSunrise = findViewById(R.id.setup_visible_sunrise);
        Button setupNeither = findViewById(R.id.neither_button);

        setupBoth.setOnClickListener(v -> {
                    startActivity(new Intent(this, SetupElevationActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                            .putExtra("downloadTable", true));
                    finish();
                }
        );

        setupElevation.setOnClickListener(v -> {
            editor.putBoolean("UseTable", false).apply();
            editor.putBoolean("showMishorSunrise", true).apply();
            startActivity(new Intent(this, SetupElevationActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
            finish();
                }
        );

        setupVisibleSunrise.setOnClickListener(v -> {
                    startActivity(new Intent(this, QuickSetupActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                            .putExtra("onlyTable",true));
                    finish();
                }
        );

        setupNeither.setOnClickListener(v -> {
            editor.putBoolean("isSetup", true).apply();
            editor.putBoolean("UseTable", false).apply();
            editor.putBoolean("showMishorSunrise", true).apply();
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, SetupChooserActivity.class)
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