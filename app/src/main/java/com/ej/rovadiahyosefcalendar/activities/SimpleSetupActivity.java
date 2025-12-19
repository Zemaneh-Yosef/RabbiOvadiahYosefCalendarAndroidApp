package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesCountries;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesOptionsList;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesWebJava;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.util.GeoLocation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class SimpleSetupActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private Spinner mCountrySpinner;
    private Spinner mStateSpinner;
    private Spinner mMetroAreaSpinner;
    private ChaiTablesCountries mCountry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_simple_setup);

        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        if (Utils.isLocaleHebrew()) {
            materialToolbar.setSubtitle("");
        }

        LocationResolver locationResolver = new LocationResolver(this, this);
        locationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
        TextView locationTextView = findViewById(R.id.location);
        locationTextView.setText(sCurrentLocationName);

        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        Button downloadButton = findViewById(R.id.quickSetupDownloadButton);
        downloadButton.setEnabled(false);//disable button at the beginning

        mCountrySpinner = findViewById(R.id.countrySpinner);
        Context context = this;
        mCountrySpinner.setAdapter(new ArrayAdapter<>(context, R.layout.custom_spinner_item, ChaiTablesOptionsList.countries));

        TextView stateTextView = findViewById(R.id.selectState);
        mStateSpinner = findViewById(R.id.stateSpinner);

        TextView metroArea = findViewById(R.id.selectMetroArea);
        mMetroAreaSpinner = findViewById(R.id.metroAreaSpinner);

        mCountrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSharedPreferences.edit().putInt("selectedCountry", position).apply();
                String s = (String) parent.getItemAtPosition(position);
                s = s.replace("-", "_")
                        .replace(" ", "_")
                        .replace("(", "")
                        .replace(")", "")
                        .toUpperCase();
                mCountry = ChaiTablesCountries.valueOf(s);
                if (mCountry.equals(ChaiTablesCountries.USA) || mCountry.equals(ChaiTablesCountries.CANADA)) {
                    mStateSpinner.setVisibility(View.VISIBLE);
                    stateTextView.setVisibility(View.VISIBLE);
                    String[] states = ChaiTablesOptionsList.selectCountry(mCountry);
                    Set<String> stateSet = new HashSet<>();//get last 2 letters of each string and add to a set
                    for (String s1 : states) {
                        s1 = s1.substring(s1.length() - 2);
                        stateSet.add(s1);
                    }
                    String[] stateArray = stateSet.toArray(new String[0]);//create an array from the set
                    Arrays.sort(stateArray);
                    mStateSpinner.setAdapter(new ArrayAdapter<>(context, R.layout.custom_spinner_item, stateArray));
                    metroArea.setVisibility(View.GONE);
                } else {
                    mStateSpinner.setVisibility(View.GONE);
                    stateTextView.setVisibility(View.GONE);
                    metroArea.setVisibility(View.VISIBLE);
                    mMetroAreaSpinner.setVisibility(View.VISIBLE);
                    mMetroAreaSpinner.setAdapter(new ArrayAdapter<>(context, R.layout.custom_spinner_item, ChaiTablesOptionsList.selectCountry(mCountry)));
                }
                downloadButton.setEnabled(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mStateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSharedPreferences.edit().putInt("selectedState", position).apply();
                String s = (String) parent.getItemAtPosition(position);
                //find all strings that end with the selected last two letters of the state
                String[] metroAreas = ChaiTablesOptionsList.selectCountry(mCountry);
                Set<String> metroAreaSet = new HashSet<>();
                for (String s1 : metroAreas) {
                    if (s1.endsWith(s)) {
                        metroAreaSet.add(s1);
                    }
                }
                String[] metroAreaArray = metroAreaSet.toArray(new String[0]);//create an array from the set
                metroArea.setVisibility(View.VISIBLE);
                mMetroAreaSpinner.setVisibility(View.VISIBLE);
                mMetroAreaSpinner.setAdapter(new ArrayAdapter<>(context, R.layout.custom_spinner_item, metroAreaArray));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mMetroAreaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSharedPreferences.edit().putInt("selectedMetroArea", position).apply();
                ChaiTablesOptionsList.selectMetropolitanArea((String) parent.getItemAtPosition(position));
                downloadButton.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        downloadButton.setOnClickListener(v -> {
            downloadButton.setEnabled(false);

            ProgressBar progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);

            JewishDate jDate = new JewishDate();
            GeoLocation geoLocation = new GeoLocation("", sLatitude, sLongitude, TimeZone.getTimeZone(sCurrentTimeZoneID));
            ChaiTablesWebJava betterScraper = new ChaiTablesWebJava(geoLocation, jDate);
            betterScraper.setOtherData(mCountry.label, ChaiTablesOptionsList.indexOfMetroArea);

            Thread thread = new Thread(() -> {
                try {
                    ChaiTablesWebJava.ChaiTablesResult[] result = betterScraper.formatInterfacer();

                    File baseDir = context.getExternalFilesDir(null);  // Only Android-specific call
                    int jewishYear = jDate.getJewishYear();

                    for (ChaiTablesWebJava.ChaiTablesResult r : result) {

                        File file = new File(baseDir, "visibleSunriseTable" + sCurrentLocationName + jewishYear + ".dat");

                        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                            oos.writeObject(r.times());   // List<Long>
                        }

                        jewishYear++;
                    }

                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), getString(R.string.success), Toast.LENGTH_SHORT).show();
                    mSharedPreferences.edit().putString("chaitablesLink" + sCurrentLocationName, result[0].url()).apply(); //save the link for this location to automatically download again next time

                    mSharedPreferences.edit().putBoolean("UseTable" + sCurrentLocationName, true).apply();
                    mSharedPreferences.edit().putBoolean("showMishorSunrise" + sCurrentLocationName, false).apply();
                    mSharedPreferences.edit().putBoolean("isSetup", true).apply();
                    mSharedPreferences.edit().putBoolean("useElevation", true).apply();
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("elevation", mSharedPreferences.getString("elevation" + sCurrentLocationName, ""));
                    setResult(Activity.RESULT_OK, returnIntent);

                    finish();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            thread.start();
        });

        TextView areaNotListedButton = findViewById(R.id.notListedArea);
        areaNotListedButton.setPaintFlags(areaNotListedButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        areaNotListedButton.setOnClickListener(v -> {
            ProgressBar progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);
            areaNotListedButton.setEnabled(false);
            mSharedPreferences.edit().putBoolean("UseTable" + sCurrentLocationName, false).apply();
            mSharedPreferences.edit().putBoolean("showMishorSunrise" + sCurrentLocationName, true).apply();
            mSharedPreferences.edit().putBoolean("isSetup", true).apply();
            mSharedPreferences.edit().putBoolean("useElevation", true).apply();
            Thread thread = new Thread(() ->
                    locationResolver.getElevationFromWebService(new Handler(getMainLooper()), null, () -> {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("elevation", mSharedPreferences.getString("elevation" + sCurrentLocationName, ""));
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    }));
            thread.start();
        });

        ViewCompat.setOnApplyWindowInsetsListener(areaNotListedButton, (v, windowInsets) -> {
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
                startActivity(new Intent(SimpleSetupActivity.this, SetupChooserActivity.class)
                        .putExtra("fromSetup", SimpleSetupActivity.this.getIntent().getBooleanExtra("fromSetup", false)));
                finish();
            }
        });

        /*This code is from https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
        It may be useful in the future if we want to find the highest point in the area.
        double meters = 50;

        // number of km per degree = ~111km (111.32 in google maps, but range varies
        between 110.567km at the equator and 111.699km at the poles)
        // 1km in degree = 1 / 111.32km = 0.0089
        // 1m in degree = 0.0089 / 1000 = 0.0000089
        double coef = meters * 0.0000089;

        double new_lat = my_lat + coef;

        // pi / 180 = 0.018
        double new_long = my_long + coef / Math.cos(my_lat * 0.018);
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCountrySpinner.setSelection(mSharedPreferences.getInt("selectedCountry", 0));
        mStateSpinner.setSelection(mSharedPreferences.getInt("selectedState", 0));
        mMetroAreaSpinner.setSelection(mSharedPreferences.getInt("selectedMetroArea", 0));
    }
}