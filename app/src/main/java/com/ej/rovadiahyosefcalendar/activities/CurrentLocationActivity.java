package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;

public class CurrentLocationActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private LocationResolver mLocationResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_location);
        mSharedPreferences = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);

        Button allowLocationButton = findViewById(R.id.allow_location_button);
        allowLocationButton.setOnClickListener(v -> {
            mLocationResolver = new LocationResolver(this, this);
            mLocationResolver.acquireLatitudeAndLongitude();
        });

        Button enterZipcodeButton = findViewById(R.id.enter_zipcode_button);
        enterZipcodeButton.setOnClickListener(v -> createZipcodeDialog());

    }

    /**
     * This method will create a new AlertDialog that asks the user to use their location and it
     * will also give the option to use a zipcode through the EditText field.
     */
    private void createZipcodeDialog() {
        final EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        new AlertDialog.Builder(this)
                .setTitle("Enter a Zipcode")
                .setMessage("WARNING! Zmanim will NOT be accurate! Using a Zipcode will give " +
                        "you zmanim based on approximately where you are. For more accurate " +
                        "zmanim, please allow the app to see your location.")
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (input.getText().toString().isEmpty()) {// I would have loved to use a regex to validate the zipcode, however, it seems like zip codes are not uniform.
                        Toast.makeText(this, "Please Enter a valid value, for example: 11024", Toast.LENGTH_SHORT)
                                .show();
                        createZipcodeDialog();//restart
                    } else {
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putBoolean("useZipcode", true).apply();
                        editor.putString("Zipcode", input.getText().toString()).apply();
                        mLocationResolver.getLatitudeAndLongitudeFromZipcode();
                        mLocationResolver.setTimeZoneID();
                        startActivity(new Intent(this, SetupChooserActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                                .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
                        finish();//end the activity
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                })
                .create()
                .show();
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
                startActivity(new Intent(this, SetupChooserActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
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