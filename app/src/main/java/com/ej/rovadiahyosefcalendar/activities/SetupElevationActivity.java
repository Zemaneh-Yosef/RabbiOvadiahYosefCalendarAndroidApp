package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;

import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Lets the user choose the elevation used for the current location: none (Mishor, sea level), typed in, or looked up on GeoNames.
 * Opened from the location dialog and the elevation zeman's dialog. The zemanim screen reads the new value when it comes back
 * to the front. (The visible sunrise doesn't depend on this: it comes from the refraction server's terrain horizon.)
 */
public class SetupElevationActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private Button mMishorButton;
    private Button mManualButton;
    private Button mGeoNamesButton;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup_elevation);
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        if (Utils.isLocaleHebrew(this)) {
            materialToolbar.setSubtitle("");
        }
        materialToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.help) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.help_using_this_app)
                        .setPositiveButton(R.string.ok, null)
                        .setMessage(R.string.helper_text)
                        .show();
                return true;
            }
            return false;
        });

        TextView setupHeader = findViewById(R.id.setup_header);
        setupHeader.setPaintFlags(setupHeader.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        showCurrentElevation();

        mProgressBar = findViewById(R.id.progressBarElevation);
        mMishorButton = findViewById(R.id.mishor);
        mManualButton = findViewById(R.id.manual);
        mGeoNamesButton = findViewById(R.id.geonamesButton);

        mMishorButton.setOnClickListener(v -> {
            mSharedPreferences.edit()
                    .putString("elevation" + sCurrentLocationName, "0")// some readers (the widget, the watch) don't check useElevation
                    .putBoolean("useElevation", false)
                    .putBoolean("isSetup", true).apply();
            finish();
        });

        mManualButton.setOnClickListener(v -> showManualElevationDialog());

        mGeoNamesButton.setOnClickListener(v -> {
            setButtonsEnabled(false);
            mProgressBar.setVisibility(View.VISIBLE);
            mSharedPreferences.edit()
                    .putBoolean("useElevation", true)
                    .putBoolean("isSetup", true).apply();
            LocationResolver locationResolver = new LocationResolver(this, this);
            new Thread(() -> locationResolver.getElevationFromWebService(new Handler(getMainLooper()), null, this::finish)).start();
        });

        // keep the last button above the navigation bar
        View content = findViewById(R.id.setup_elevation_content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, v.getPaddingTop(), insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** "Elevation: 23 meters" (or Mishor) for the current location, so the user knows what is set before changing it. */
    private void showCurrentElevation() {
        TextView current = findViewById(R.id.current_elevation);
        String value = mSharedPreferences.getBoolean("useElevation", true)
                ? mSharedPreferences.getString("elevation" + sCurrentLocationName, "0") + " " + getString(R.string.meters)
                : getString(R.string.mishor_sea_level);
        current.setText(sCurrentLocationName + "\n" + getString(R.string.elevation) + " " + value);
    }

    private void showManualElevationDialog() {
        final EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(R.string.enter_elevation_in_meters);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.enter_elevation_in_meters)
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String elevation = input.getText().toString();
                    if (!elevation.matches("[0-9]+(\\.[0-9]+)?")) {// a whole or decimal number of meters, e.g. 30 or 30.5
                        Toast.makeText(this, R.string.please_enter_a_valid_value_for_example_30_or_30_0, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mSharedPreferences.edit()
                            .putString("elevation" + sCurrentLocationName, elevation)
                            .putBoolean("useElevation", true)
                            .putBoolean("isSetup", true).apply();
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setButtonsEnabled(boolean enabled) {
        mMishorButton.setEnabled(enabled);
        mManualButton.setEnabled(enabled);
        mGeoNamesButton.setEnabled(enabled);
    }
}
