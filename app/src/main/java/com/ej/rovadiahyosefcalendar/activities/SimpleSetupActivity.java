package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sLongitude;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesCountries;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesOptionsList;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesScraper;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SimpleSetupActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private Spinner mCountrySpinner;
    private Spinner mStateSpinner;
    private Spinner mMetroAreaSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_setup);
        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_custom);//center the title
        JewishDate jewishDate = new JewishDate();
        LocationResolver locationResolver = new LocationResolver(this, this);
        locationResolver.acquireLatitudeAndLongitude();
        TextView locationTextView = findViewById(R.id.location);
        locationTextView.setText(MainActivity.sCurrentLocationName);

        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        Button downloadButton = findViewById(R.id.quickSetupDownloadButton);
        downloadButton.setEnabled(false);//disable button at the beginning

        mCountrySpinner = findViewById(R.id.countrySpinner);
        Context context = this;
        mCountrySpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, ChaiTablesOptionsList.countries));

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
                ChaiTablesCountries country = ChaiTablesCountries.valueOf(s);
                if (country.equals(ChaiTablesCountries.USA)) {
                    mStateSpinner.setVisibility(View.VISIBLE);
                    stateTextView.setVisibility(View.VISIBLE);
                    String[] states = ChaiTablesOptionsList.selectCountry(country);
                    Set<String> stateSet = new HashSet<>();//get last 2 letters of each string and add to a set
                    for (String s1 : states) {
                        s1 = s1.substring(s1.length() - 2);
                        stateSet.add(s1);
                    }
                    String[] stateArray = stateSet.toArray(new String[0]);//create an array from the set
                    Arrays.sort(stateArray);
                    mStateSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, stateArray));
                    metroArea.setVisibility(View.GONE);
                } else {
                    mStateSpinner.setVisibility(View.GONE);
                    stateTextView.setVisibility(View.GONE);
                    metroArea.setVisibility(View.VISIBLE);
                    mMetroAreaSpinner.setVisibility(View.VISIBLE);
                    mMetroAreaSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, ChaiTablesOptionsList.selectCountry(country)));
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
                String[] metroAreas = ChaiTablesOptionsList.selectCountry(ChaiTablesCountries.USA);
                Set<String> metroAreaSet = new HashSet<>();
                for (String s1 : metroAreas) {
                    if (s1.endsWith(s)) {
                        metroAreaSet.add(s1);
                    }
                }
                String[] metroAreaArray = metroAreaSet.toArray(new String[0]);//create an array from the set
                metroArea.setVisibility(View.VISIBLE);
                mMetroAreaSpinner.setVisibility(View.VISIBLE);
                mMetroAreaSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, metroAreaArray));
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
            ProgressBar progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);
            int userID = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getInt("USER_ID", 10000);
            ChaiTablesScraper scraper = new ChaiTablesScraper();
            String link = ChaiTablesOptionsList.getChaiTablesLink(sLatitude, sLongitude, -5, 8, 0, jewishDate.getJewishYear(), userID);
            scraper.setUrl(link);
            scraper.setExternalFilesDir(getExternalFilesDir(null));
            scraper.setJewishDate(jewishDate);
            locationResolver.start();
            scraper.start();
            try {
                scraper.join();
                locationResolver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (scraper.isSearchRadiusTooSmall()) {
                Toast.makeText(getApplicationContext(), R.string.something_went_wrong_did_you_choose_the_right_area, Toast.LENGTH_SHORT).show();
                startActivity(getIntent());
            } else if (scraper.isWebsiteError()) {
                Toast.makeText(getApplicationContext(), R.string.something_went_wrong_connecting_to_the_website_please_try_again_later, Toast.LENGTH_SHORT).show();
                startActivity(getIntent());
            } else {
                Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_SHORT).show();
                userID++;
                mSharedPreferences.edit().putInt("USER_ID", userID).apply();
                mSharedPreferences.edit().putString("chaitablesLink" + sCurrentLocationName, link).apply();//save the link for this location to automatically download again next time
            }
            mSharedPreferences.edit().putBoolean("UseTable" + sCurrentLocationName, true).apply();
            mSharedPreferences.edit().putBoolean("showMishorSunrise" + sCurrentLocationName, false).apply();
            mSharedPreferences.edit().putBoolean("isSetup", true).apply();
            mSharedPreferences.edit().putBoolean("useElevation", true).apply();
            Intent returnIntent = new Intent();
            returnIntent.putExtra("elevation", mSharedPreferences.getString("elevation" + sCurrentLocationName, ""));
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        });

        TextView areaNotListedButton = findViewById(R.id.notListedArea);
        areaNotListedButton.setPaintFlags(areaNotListedButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        areaNotListedButton.setOnClickListener(v -> {
            ProgressBar progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);
            mSharedPreferences.edit().putBoolean("UseTable" + sCurrentLocationName, false).apply();
            mSharedPreferences.edit().putBoolean("showMishorSunrise" + sCurrentLocationName, true).apply();
            mSharedPreferences.edit().putBoolean("isSetup", true).apply();
            mSharedPreferences.edit().putBoolean("useElevation", true).apply();
            locationResolver.start();
            try {
                locationResolver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent returnIntent = new Intent();
            returnIntent.putExtra("elevation", mSharedPreferences.getString("elevation" + sCurrentLocationName, ""));
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.help) {
            new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_DayNight)
                    .setTitle(R.string.help_using_this_app)
                    .setPositiveButton(R.string.ok, null)
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
        if (getIntent().getBooleanExtra("onlyTable", false)) {
            startActivity(new Intent(this, AdvancedSetupActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
        } else {
            startActivity(new Intent(this, SetupChooserActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
        }
        finish();
        super.onBackPressed();
    }
}