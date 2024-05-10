package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sCurrentLocationName;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;

import java.util.Locale;
import java.util.Objects;

@Deprecated //This class is not currently used, however, I will keep the code here because it might be used in the future.
public class CurrentLocationActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private LocationResolver mLocationResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_location);

        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_custom);//center the title

        mSharedPreferences = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        mLocationResolver = new LocationResolver(this, this);

        Button allowLocationButton = findViewById(R.id.allow_location_button);
        allowLocationButton.setOnClickListener(v -> mLocationResolver.acquireLatitudeAndLongitude());

        Button enterZipcodeButton = findViewById(R.id.enter_zipcode_button);
        enterZipcodeButton.setOnClickListener(v -> createZipcodeDialog());

    }

    @Override
    protected void onResume() {
        super.onResume();
        TextClock clock = Objects.requireNonNull(getSupportActionBar()).getCustomView().findViewById(R.id.clock);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            clock.setVisibility(View.VISIBLE);
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                clock.setFormat24Hour("H:mm:ss");
            }
        } else {
            clock.setVisibility(View.GONE);
        }
    }

    /**
     * This method will create a new AlertDialog that asks the user to use their location and it
     * will also give the option to use a zipcode through the EditText field.
     */
    private void createZipcodeDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.alertDialog);
        final EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setHint(R.string.enter_zipcode_or_address);
        input.setSingleLine();
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        alertDialog.setTitle(R.string.search_for_a_place)
                .setMessage(R.string.warning_zmanim_will_be_based_on_your_approximate_area)
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (input.getText().toString().isEmpty()) {
                        Toast.makeText(this, R.string.please_enter_a_valid_value_for_example_11024, Toast.LENGTH_SHORT)
                                .show();
                        createZipcodeDialog();//restart
                    } else {
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putBoolean("useZipcode", true).apply();
                        editor.putString("Zipcode", input.getText().toString()).apply();
                        mLocationResolver.getLatitudeAndLongitudeFromSearchQuery();
                        mLocationResolver.setTimeZoneID();
                        mLocationResolver.start();
                        try {
                            mLocationResolver.join();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        mSharedPreferences.edit().putBoolean("isSetup", true).apply();
                        mSharedPreferences.edit().putBoolean("useElevation", true).apply();
                        if (!mSharedPreferences.getBoolean("inIsrael", false)) {
                            startActivity(new Intent(this, CalendarChooserActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                        }
                        finish();//end the activity
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog ad = alertDialog.create();
        ad.show();

        input.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                ad.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
    }

    /**
     * This method will be called when the user has given permission to use their location.
     *
     * @param requestCode The request code that was passed to the Activity.
     * @param permissions The permissions that were requested.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (grantResults.length > 0 && grantResults[0] == 0) {
                mLocationResolver.acquireLatitudeAndLongitude();
                mLocationResolver.setTimeZoneID();
                mLocationResolver.start();
                try {
                    mLocationResolver.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                mSharedPreferences.edit().putBoolean("UseTable" + sCurrentLocationName, false).apply();
                mSharedPreferences.edit().putBoolean("showMishorSunrise" + sCurrentLocationName, true).apply();
                mSharedPreferences.edit().putBoolean("isSetup", true).apply();
                mSharedPreferences.edit().putBoolean("useElevation", true).apply();
                if (!mSharedPreferences.getBoolean("inIsrael", false)) {
                    startActivity(new Intent(this, CalendarChooserActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                }
                finish();
            }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, ZmanimLanguageActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
        finish();
    }
}