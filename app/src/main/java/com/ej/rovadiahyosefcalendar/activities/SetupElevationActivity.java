package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sCurrentLocationName;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesScraper;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;

import org.jetbrains.annotations.NotNull;

public class SetupElevationActivity extends AppCompatActivity {

    private String mElevation = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_elevation);
        LocationResolver locationResolver = new LocationResolver(this, this);
        locationResolver.acquireLatitudeAndLongitude();
        locationResolver.setTimeZoneID();

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        TextView setupHeader = findViewById(R.id.setup_header);
        setupHeader.setPaintFlags(setupHeader.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        Button mishorButton = findViewById(R.id.mishor);
        mishorButton.setOnClickListener(v -> {
            editor.putString("elevation" + sCurrentLocationName, mElevation).apply();
            editor.putBoolean("isSetup", true).apply();
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            if (getIntent().getBooleanExtra("downloadTable",false)) {
                downloadTablesAndFinish(sharedPreferences);
            } else {
                finish();
            }
        });

        Button manualButton = findViewById(R.id.manual);
        manualButton.setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setGravity(Gravity.CENTER_HORIZONTAL);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter elevation in meters:");
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                if (input.getText().toString().isEmpty() ||
                        !input.getText().toString().matches("[0-9]+.?[0-9]*")) {//regex to check for a proper number input
                    Toast.makeText(this, "Please Enter a valid value, for example: 30 or 30.0", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    mElevation = input.getText().toString();
                    editor.putString("elevation" + sCurrentLocationName, mElevation).apply();
                    editor.putBoolean("isSetup", true).apply();
                    editor.putBoolean("isElevationSetup", true).apply();
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("elevation", mElevation);
                    setResult(Activity.RESULT_OK, returnIntent);
                    if (getIntent().getBooleanExtra("downloadTable",false)) {
                        downloadTablesAndFinish(sharedPreferences);
                    } else {
                        finish();
                    }
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.create();
            builder.show();
        });

        Button geoNamesButton = findViewById(R.id.geonamesButton);
        geoNamesButton.setOnClickListener(view -> {
            if (getIntent().getBooleanExtra("downloadTable",false)) {
                locationResolver.start();
                editor.putBoolean("isElevationSetup", true).apply();
                editor.putBoolean("isSetup", true).apply();
                Intent returnIntent = new Intent();
                returnIntent.putExtra("elevation", sharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
                setResult(Activity.RESULT_OK, returnIntent);
                downloadTablesAndFinish(sharedPreferences);
            } else {
                finish();
            }
        });
    }

    private void downloadTablesAndFinish(SharedPreferences sharedPreferences) {
        ProgressBar progressBar = findViewById(R.id.progressBarElevation);
        progressBar.setVisibility(View.VISIBLE);
        if (sharedPreferences.getBoolean("UseTable", true)) {
            int userID = sharedPreferences.getInt("USER_ID", 10000);
            ChaiTablesScraper scraper = new ChaiTablesScraper();
            String link = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getString("chaitablesLink" + sCurrentLocationName, "");
            scraper.setUrl(link);
            scraper.setExternalFilesDir(getExternalFilesDir(null));
            scraper.start();
            try {
                scraper.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (scraper.isSearchRadiusTooSmall()) {
                Toast.makeText(getApplicationContext(), "Something went wrong. Is the link correct?", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AdvancedSetupActivity.class));
            } else {
                Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_SHORT).show();
                userID++;
                sharedPreferences.edit().putInt("USER_ID", userID).apply();
            }
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_elevation_setup, menu);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, AdvancedSetupActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
        finish();
        super.onBackPressed();
    }
}