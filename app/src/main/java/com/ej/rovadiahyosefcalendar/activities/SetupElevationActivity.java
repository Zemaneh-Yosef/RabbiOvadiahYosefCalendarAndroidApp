package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentLocationName;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesScraper;
import com.ej.rovadiahyosefcalendar.classes.LocaleChecker;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

public class SetupElevationActivity extends AppCompatActivity {

    private String mElevation = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup_elevation);

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
            }
            return false;
        });

        LocationResolver locationResolver = new LocationResolver(this, this);
        locationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
        locationResolver.setTimeZoneID();

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        TextView setupHeader = findViewById(R.id.setup_header);
        setupHeader.setPaintFlags(setupHeader.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        Button mishorButton = findViewById(R.id.mishor);
        mishorButton.setOnClickListener(v -> {
            mishorButton.setEnabled(false);
            editor.putString("elevation" + sCurrentLocationName, mElevation).apply();
            editor.putBoolean("useElevation", false).apply();
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
                    editor.putString("elevation" + sCurrentLocationName, mElevation).apply();
                    editor.putBoolean("isSetup", true).apply();
                    editor.putBoolean("useElevation", true).apply();
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
            editor.putBoolean("useElevation", true).apply();
            editor.putBoolean("isSetup", true).apply();
            locationResolver.start();
            try {
                locationResolver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent returnIntent = new Intent();
            returnIntent.putExtra("elevation" + sCurrentLocationName, sharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
            setResult(Activity.RESULT_OK, returnIntent);
            if (getIntent().getBooleanExtra("downloadTable",false)) {
                downloadTablesAndFinish(sharedPreferences);
            } else {
                finish();
            }
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
                if (!getIntent().getBooleanExtra("fromMenu", false)) {
                    startActivity(new Intent(SetupElevationActivity.this, AdvancedSetupActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                            .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
                }
                finish();
            }
        });
    }

    private void downloadTablesAndFinish(SharedPreferences sharedPreferences) {
        ProgressBar progressBar = findViewById(R.id.progressBarElevation);
        progressBar.setVisibility(View.VISIBLE);
        if (sharedPreferences.getBoolean("UseTable" + sCurrentLocationName, true)) {
            int userID = sharedPreferences.getInt("USER_ID", 10000);
            ChaiTablesScraper scraper = new ChaiTablesScraper();
            String link = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getString("chaitablesLink" + sCurrentLocationName, "");
            scraper.setUrl(link);
            scraper.setExternalFilesDir(getExternalFilesDir(null));
            scraper.setJewishDate(new JewishDate());
            scraper.start();
            try {
                scraper.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (scraper.isSearchRadiusTooSmall()) {
                Toast.makeText(getApplicationContext(), R.string.something_went_wrong_is_the_link_correct, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AdvancedSetupActivity.class));
            } else if (scraper.isWebsiteError()) {
                Toast.makeText(getApplicationContext(), R.string.something_went_wrong_connecting_to_the_website, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AdvancedSetupActivity.class));
            } else {
                Toast.makeText(getApplicationContext(), R.string.success, Toast.LENGTH_SHORT).show();
                userID++;
                sharedPreferences.edit().putInt("USER_ID", userID).apply();
            }
        }
        finish();
    }
}