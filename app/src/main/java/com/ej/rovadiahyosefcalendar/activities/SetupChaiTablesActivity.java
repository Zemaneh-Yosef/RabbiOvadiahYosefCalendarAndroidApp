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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
import com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesCountries;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesOptionsList;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesWebJava;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.util.GeoLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class SetupChaiTablesActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private ChaiTablesCountries mCountry;
    private TextInputLayout mStateLayout;
    private TextInputLayout mMetroLayout;
    private AutoCompleteTextView mCountryACTV;
    private AutoCompleteTextView mStateACTV;
    private AutoCompleteTextView mMetroACTV;
    private Button mDownloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup_chaitables);
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        LocationResolver locationResolver = new LocationResolver(this, this);
        locationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());

        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        if (Utils.isLocaleHebrew(this)) {
            materialToolbar.setSubtitle("");
        }

        materialToolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.showIntro) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.Introduction)
                        .setMessage(R.string.intro)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                        .setCancelable(false)
                        .show();
                return true;
            } else if (itemId == R.id.advancedSetup) {
                startActivity(new Intent(this, AdvancedSetupActivity.class)
                        .putExtra("fromSetup", SetupChaiTablesActivity.this.getIntent().getBooleanExtra("fromSetup", false)));
                finish();
            }
            return false;
        });

        TextView locationTextView = findViewById(R.id.location);
        locationTextView.setText(sCurrentLocationName);

        mDownloadButton = findViewById(R.id.quickSetupDownloadButton);
        mDownloadButton.setEnabled(false);// disable button at the beginning

        mCountryACTV = findViewById(R.id.select_country);
        mStateACTV = findViewById(R.id.select_state);
        mStateLayout = findViewById(R.id.select_state_layout);
        mMetroACTV = findViewById(R.id.select_metro_area);
        mMetroLayout = findViewById(R.id.select_metro_layout);

        mCountryACTV.setAdapter(new ArrayAdapter<>(this, R.layout.custom_spinner_item, ChaiTablesOptionsList.countries));

        mCountryACTV.setOnItemClickListener((p, v, pos, id) ->
                selectCountry((String) p.getItemAtPosition(pos), pos));

        mStateACTV.setOnItemClickListener((p, v, pos, id) ->
                selectState((String) p.getItemAtPosition(pos), pos));

        mMetroACTV.setOnItemClickListener((p, v, pos, id) ->
                selectMetro((String) p.getItemAtPosition(pos), pos));


        mDownloadButton.setOnClickListener(v -> {
            mDownloadButton.setEnabled(false);

            ProgressBar progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);

            JewishDate jDate = new JewishDate();
            ChaiTablesWebJava scraper = new ChaiTablesWebJava(new GeoLocation("", sLatitude, sLongitude, TimeZone.getTimeZone(sCurrentTimeZoneID)), jDate);
            scraper.setOtherData(mCountry.label, mMetroACTV.getText().toString() , ChaiTablesOptionsList.indexOfMetroArea);

            new Thread(() -> {
                try {
                    ChaiTablesWebJava.ChaiTablesResult[] result = scraper.formatInterfacer();
                    int jewishYear = jDate.getJewishYear();
                    if (result != null) {
                        for (ChaiTablesWebJava.ChaiTablesResult r : result) {
                            ChaiTablesWebJava.saveResultsToFile(r, getExternalFilesDir(null), sCurrentLocationName, jewishYear);
                            jewishYear++;
                        }
                    }

                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), getString(R.string.success), Toast.LENGTH_SHORT).show();
                    mSharedPreferences.edit().putString("chaitablesLink" + sCurrentLocationName, result != null ? result[0].url() : null).apply(); // save the link for this location to automatically download again next time
                    mSharedPreferences.edit().putBoolean("UseTable" + sCurrentLocationName, true)
                            .putBoolean("showMishorSunrise" + sCurrentLocationName, false)
                            .putBoolean("isSetup", true)
                            .putBoolean("useElevation", true)
                            .apply();
                    setResult(Activity.RESULT_OK, new Intent().putExtra("elevation", mSharedPreferences.getString("elevation" + sCurrentLocationName, "")));
                    finish();
                } catch (IOException e) {
                    recreate();
                    e.printStackTrace();
                }
            }).start();
        });

        TextView areaNotListedButton = findViewById(R.id.notListedArea);
        areaNotListedButton.setPaintFlags(areaNotListedButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        areaNotListedButton.setOnClickListener(v -> {
            ProgressBar progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);
            areaNotListedButton.setEnabled(false);
            mSharedPreferences.edit().putBoolean("UseTable" + sCurrentLocationName, false)
                    .putBoolean("showMishorSunrise" + sCurrentLocationName, true)
                    .putBoolean("isSetup", true)
                    .putBoolean("useElevation", true)
                    .apply();
            new Thread(() ->
                    locationResolver.getElevationFromWebService(new Handler(getMainLooper()), null, () -> {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("elevation", mSharedPreferences.getString("elevation" + sCurrentLocationName, ""));
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    })).start();
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
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCountryACTV.getAdapter() == null) return;
        selectCountry(String.valueOf(mCountryACTV.getAdapter().getItem(mSharedPreferences.getInt("selectedCountry", 0))), mSharedPreferences.getInt("selectedCountry", 0));
        if (mStateACTV.getAdapter() != null) {
            selectState(String.valueOf(mStateACTV.getAdapter().getItem(mSharedPreferences.getInt("selectedState", 0))), mSharedPreferences.getInt("selectedState", 0));
        }
        if (mMetroACTV.getAdapter() == null) return;
        selectMetro(String.valueOf(mMetroACTV.getAdapter().getItem(mSharedPreferences.getInt("selectedMetroArea", 0))), mSharedPreferences.getInt("selectedMetroArea", 0));
    }

    private void selectCountry(String country, int countryIndex) {
        mCountryACTV.setText(country, false);
        mSharedPreferences.edit().putInt("selectedCountry", countryIndex).apply();

        String enumKey = country.replace("-", "_")
                .replace(" ", "_")
                .replace("(", "")
                .replace(")", "")
                .toUpperCase();

        mCountry = ChaiTablesCountries.valueOf(enumKey);

        if (mCountry == ChaiTablesCountries.USA || mCountry == ChaiTablesCountries.CANADA) {
            mStateLayout.setVisibility(View.VISIBLE);
            mMetroLayout.setVisibility(View.GONE);

            String[] states = extractStates(ChaiTablesOptionsList.selectCountry(mCountry));
            mStateACTV.setAdapter(new ArrayAdapter<>(this, R.layout.custom_spinner_item, states));

            int stateIndex = mSharedPreferences.getInt("selectedState", 0);
            stateIndex = Math.min(stateIndex, states.length - 1);

            selectState(states[stateIndex], stateIndex);
        } else {
            mStateLayout.setVisibility(View.GONE);
            mMetroLayout.setVisibility(View.VISIBLE);

            String[] metros = ChaiTablesOptionsList.selectCountry(mCountry);
            mMetroACTV.setAdapter(new ArrayAdapter<>(this, R.layout.custom_spinner_item, metros));

            int metroIndex = mSharedPreferences.getInt("selectedMetroArea", 0);
            metroIndex = Math.min(metroIndex, metros.length - 1);

            selectMetro(metros[metroIndex], metroIndex);
        }

        mDownloadButton.setEnabled(false);
    }

    private String[] extractStates(String[] metroAreas) {
        Set<String> stateSet = new HashSet<>();

        for (String metro : metroAreas) {
            if (metro != null && metro.length() >= 2) {
                stateSet.add(metro.substring(metro.length() - 2));
            }
        }

        String[] states = stateSet.toArray(new String[0]);
        Arrays.sort(states);
        return states;
    }

    private void selectState(String state, int stateIndex) {
        mStateACTV.setText(state, false);
        mSharedPreferences.edit().putInt("selectedState", stateIndex).apply();

        String[] allMetros = ChaiTablesOptionsList.selectCountry(mCountry);
        List<String> metros = new ArrayList<>();

        for (String metro : allMetros) {
            if (metro.endsWith(state)) {
                metros.add(metro);
            }
        }

        String[] metroArray = metros.toArray(new String[0]);
        mMetroLayout.setVisibility(View.VISIBLE);
        mMetroACTV.setAdapter(new ArrayAdapter<>(this, R.layout.custom_spinner_item, metroArray));

        int metroIndex = mSharedPreferences.getInt("selectedMetroArea", 0);
        metroIndex = Math.min(metroIndex, metroArray.length - 1);

        selectMetro(metroArray[metroIndex], metroIndex);
    }

    private void selectMetro(String metro, int metroIndex) {
        mMetroACTV.setText(metro, false);
        mSharedPreferences.edit().putInt("selectedMetroArea", metroIndex).apply();

        ChaiTablesOptionsList.selectMetropolitanArea(metro);
        mDownloadButton.setEnabled(true);
    }
}