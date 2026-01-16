package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesWebJava;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.TimeZone;

public class SetupElevationActivity extends AppCompatActivity {

    private String mElevation = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup_elevation);

        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (Utils.isLocaleHebrew(this)) {
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
            }
            return false;
        });

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        TextView setupHeader = findViewById(R.id.setup_header);
        setupHeader.setPaintFlags(setupHeader.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        Button mishorButton = findViewById(R.id.mishor);
        mishorButton.setOnClickListener(v -> {
            mishorButton.setEnabled(false);
            editor.putString("elevation" + sCurrentLocationName, mElevation)
                    .putBoolean("useElevation", false)
                    .putBoolean("isSetup", true).apply();
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
            manualButton.setEnabled(false);
            final EditText input = new EditText(this);
            input.setGravity(Gravity.CENTER_HORIZONTAL);
            input.setHint(R.string.enter_elevation_in_meters);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.enter_elevation_in_meters);
            builder.setView(input);
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                if (input.getText().toString().isEmpty() ||
                        !input.getText().toString().matches("[0-9]+.?[0-9]*")) {//regex to check for a proper number input
                    Toast.makeText(this, R.string.please_enter_a_valid_value_for_example_30_or_30_0, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    mElevation = input.getText().toString();
                    editor.putString("elevation" + sCurrentLocationName, mElevation)
                            .putBoolean("isSetup", true)
                            .putBoolean("useElevation", true).apply();
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("elevation" + sCurrentLocationName, mElevation);
                    setResult(Activity.RESULT_OK, returnIntent);
                    if (getIntent().getBooleanExtra("downloadTable",false)) {
                        downloadTablesAndFinish(sharedPreferences);
                    } else {
                        finish();
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
            builder.create();
            builder.show();
        });

        Button geoNamesButton = findViewById(R.id.geonamesButton);
        geoNamesButton.setOnClickListener(view -> {
            geoNamesButton.setEnabled(false);
            editor.putBoolean("useElevation", true)
                    .putBoolean("isSetup", true).apply();
            Thread thread = new Thread(() -> {
                LocationResolver locationResolver = new LocationResolver(this, this);
                locationResolver.getElevationFromWebService(new Handler(getMainLooper()), null, () -> {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("elevation" + sCurrentLocationName, sharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
                    setResult(Activity.RESULT_OK, returnIntent);
                    if (getIntent().getBooleanExtra("downloadTable", false)) {
                        downloadTablesAndFinish(sharedPreferences);
                    } else {
                        finish();
                    }
                });
            });
            thread.start();
        });

        ViewCompat.setOnApplyWindowInsetsListener(geoNamesButton, (v, windowInsets) -> {
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
                if (!SetupElevationActivity.this.getIntent().getBooleanExtra("loneActivity", false)) {
                    startActivity(new Intent(SetupElevationActivity.this, AdvancedSetupActivity.class)
                            .putExtra("fromSetup", SetupElevationActivity.this.getIntent().getBooleanExtra("fromSetup", false)));
                }
                finish();
            }
        });
    }

    private void downloadTablesAndFinish(SharedPreferences sharedPreferences) {
        ProgressBar progressBar = findViewById(R.id.progressBarElevation);
        progressBar.setVisibility(View.VISIBLE);

        if (sharedPreferences.getBoolean("UseTable" + sCurrentLocationName, true)) {
            String link = sharedPreferences.getString("chaitablesLink" + sCurrentLocationName, "");
            JewishDate jDate = new JewishDate();
            GeoLocation geoLocation = new GeoLocation("", sLatitude, sLongitude, TimeZone.getTimeZone(sCurrentTimeZoneID));
            ChaiTablesWebJava scraper = new ChaiTablesWebJava(geoLocation, jDate);

            Thread thread = new Thread(() -> {
                try {
                    ChaiTablesWebJava.ChaiTablesResult[] results = scraper.formatInterfacer(link);
                    int jewishYear = jDate.getJewishYear();
                    for (ChaiTablesWebJava.ChaiTablesResult r : results) {
                        ChaiTablesWebJava.saveResultsToFile(r, getExternalFilesDir(null), sCurrentLocationName, jewishYear);
                        jewishYear++;
                    }

                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), getString(R.string.success), Toast.LENGTH_SHORT).show();
                    sharedPreferences.edit().putString("chaitablesLink" + sCurrentLocationName, results[0].url()).apply(); //save the link for this location to automatically download again next time

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), R.string.something_went_wrong_is_the_link_correct, Toast.LENGTH_SHORT).show();
                    new RuntimeException(e).printStackTrace();
                } finally {
                    finish();
                }
            });
            thread.start();
        } else {
            finish();
        }
    }
}

        /*This code is from https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
        It may be useful in the future if we want to find the highest point in the area.
        double meters = 50;

        // number of km per degree = ~111km (111.32 in google maps, but range varies between 110.567km at the equator and 111.699km at the poles)
        // 1km in degree = 1 / 111.32km = 0.0089
        // 1m in degree = 0.0089 / 1000 = 0.0000089
        double coef = meters * 0.0000089;

        double new_lat = my_lat + coef;

        // pi / 180 = 0.018
        double new_long = my_long + coef / Math.cos(my_lat * 0.018);
        */