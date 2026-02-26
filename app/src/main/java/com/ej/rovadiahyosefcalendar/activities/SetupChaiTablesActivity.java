package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;
import static com.ej.rovadiahyosefcalendar.classes.Utils.inputStreamToString;

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
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesWebJava;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.util.GeoLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class SetupChaiTablesActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private String mCountry;
    private JSONArray chaitablesIndex;
    private int mMetroIndex;
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

        try {
            chaitablesIndex = new JSONArray(inputStreamToString(getResources().openRawResource(R.raw.chaitable)));
        } catch (JSONException e) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }

        TextView locationTextView = findViewById(R.id.location);
        locationTextView.setText(sCurrentLocationName);

        mDownloadButton = findViewById(R.id.quickSetupDownloadButton);
        mDownloadButton.setEnabled(false);// disable button at the beginning

        mCountryACTV = findViewById(R.id.select_country);
        mStateACTV = findViewById(R.id.select_state);
        mStateLayout = findViewById(R.id.select_state_layout);
        mMetroACTV = findViewById(R.id.select_metro_area);
        mMetroLayout = findViewById(R.id.select_metro_layout);

        List<String> countries = new ArrayList<>();

        for (int i = 0; i < chaitablesIndex.length(); i++) {
            try {
                countries.add(chaitablesIndex
                        .getJSONObject(i)
                        .getJSONObject("info")
                        .getString("title"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mCountryACTV.setAdapter(new ArrayAdapter<>(this, R.layout.custom_spinner_item, countries));

        mCountryACTV.setOnItemClickListener((p, v, pos, id) ->
        {
            try {
                selectCountry((String) p.getItemAtPosition(pos), pos);
            } catch (JSONException e) {
                new RuntimeException(e).printStackTrace();
            }
        });

        mStateACTV.setOnItemClickListener((p, v, pos, id) ->
        {
            try {
                selectState((String) p.getItemAtPosition(pos), pos);
            } catch (JSONException e) {
                new RuntimeException(e).printStackTrace();
            }
        });

        mMetroACTV.setOnItemClickListener((p, v, pos, id) ->
        {
            try {
                selectMetro((String) p.getItemAtPosition(pos), pos);
            } catch (JSONException e) {
                new RuntimeException(e).printStackTrace();
            }
        });

        mDownloadButton.setOnClickListener(v -> {
            mDownloadButton.setEnabled(false);

            ProgressBar progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);

            JewishDate jDate = new JewishDate();
            ChaiTablesWebJava scraper = new ChaiTablesWebJava(new GeoLocation("", sLatitude, sLongitude, TimeZone.getTimeZone(sCurrentTimeZoneID)), jDate);
            scraper.setOtherData(mCountry, mMetroACTV.getText().toString(), mMetroIndex);

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
        try {
            if (mCountryACTV.getAdapter() == null && mCountryACTV.getAdapter().getCount() < mSharedPreferences.getInt("selectedCountry", 0)) return;
            selectCountry(String.valueOf(mCountryACTV.getAdapter().getItem(mSharedPreferences.getInt("selectedCountry", 0))), mSharedPreferences.getInt("selectedCountry", 0));
            if (mStateACTV.getAdapter() != null && mStateACTV.getAdapter().getCount() < mSharedPreferences.getInt("selectedState", 0)) {
                selectState(String.valueOf(mStateACTV.getAdapter().getItem(mSharedPreferences.getInt("selectedState", 0))), mSharedPreferences.getInt("selectedState", 0));
            }
            if (mMetroACTV.getAdapter() == null && mMetroACTV.getAdapter().getCount() < mSharedPreferences.getInt("selectedMetroArea", 0)) return;
            selectMetro(String.valueOf(mMetroACTV.getAdapter().getItem(mSharedPreferences.getInt("selectedMetroArea", 0))), mSharedPreferences.getInt("selectedMetroArea", 0));
        } catch (JSONException e) {
            new RuntimeException(e).printStackTrace();
        }
    }

    private void selectCountry(String country, int countryIndex) throws JSONException {
        mCountryACTV.setText(country, false);
        mSharedPreferences.edit().putInt("selectedCountry", countryIndex).apply();

        mCountry = country;

        if (chaitablesIndex.getJSONObject(countryIndex).getJSONObject("info").has("stateSeparate") && chaitablesIndex.getJSONObject(countryIndex).getJSONObject("info").getBoolean("stateSeparate")) {
            mStateLayout.setVisibility(View.VISIBLE);
            mMetroLayout.setVisibility(View.GONE);

            String[] states = extractStates(formatToStringArray(chaitablesIndex.getJSONObject(countryIndex).getJSONArray("metroAreas")));

            mStateACTV.setAdapter(new ArrayAdapter<>(this, R.layout.custom_spinner_item, states));
            int stateIndex = mSharedPreferences.getInt("selectedState", 0);
            stateIndex = Math.min(stateIndex, states.length - 1);
            selectState(states[stateIndex], stateIndex);
        } else {
            mStateLayout.setVisibility(View.GONE);
            mMetroLayout.setVisibility(View.VISIBLE);

            JSONArray jsonArray =  chaitablesIndex.getJSONObject(countryIndex).getJSONArray("metroAreas");
            String[] metros = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                metros[i] = jsonObject.getString("name");
            }
            mMetroACTV.setAdapter(new ArrayAdapter<>(this, R.layout.custom_spinner_item, metros));

            int metroIndex = mSharedPreferences.getInt("selectedMetroArea", 0);
            metroIndex = Math.min(metroIndex, metros.length - 1);
            selectMetro(metros[metroIndex], metroIndex);
        }

        mDownloadButton.setEnabled(false);
    }

    private void selectState(String state, int stateIndex) throws JSONException {
        mStateACTV.setText(state, false);
        mSharedPreferences.edit().putInt("selectedState", stateIndex).apply();

        String[] allMetros = formatToStringArray(chaitablesIndex.getJSONObject(mSharedPreferences.getInt("selectedCountry", 0)).getJSONArray("metroAreas"));
        List<String> metros = new ArrayList<>();

        for (String metro : allMetros) {
            JSONObject jsonObject = new JSONObject(metro);
            String name = jsonObject.getString("name");
            if (name.endsWith(state)) {
                metros.add(name);
            }
        }

        String[] metroArray = metros.toArray(new String[0]);
        mMetroLayout.setVisibility(View.VISIBLE);
        mMetroACTV.setAdapter(new ArrayAdapter<>(this, R.layout.custom_spinner_item, metroArray));

        int metroIndex = mSharedPreferences.getInt("selectedMetroArea", 0);
        if (metroArray.length > 0) {
            metroIndex = Math.min(metroIndex, metroArray.length - 1);
            selectMetro(metroArray[metroIndex], metroIndex);
        }
    }

    private void selectMetro(String metro, int metroIndex) throws JSONException {
        mMetroACTV.setText(metro, false);
        mSharedPreferences.edit().putInt("selectedMetroArea", metroIndex).apply();
        JSONArray metroAreas = chaitablesIndex.getJSONObject(mSharedPreferences.getInt("selectedCountry", 0)).getJSONArray("metroAreas");
        for (int i = 0; i < metroAreas.length(); i++) {
            if (metroAreas.getJSONObject(i).getString("name").equals(metro)) {
                mMetroIndex = i + 1;
                break;
            }
        }
        mDownloadButton.setEnabled(true);
    }

    private String[] extractStates(String[] metroAreas) throws JSONException {
        Set<String> stateSet = new HashSet<>();

        for (String metro : metroAreas) {
            JSONObject jsonObject = new JSONObject(metro);
            String name = jsonObject.getString("name");
            if (name.length() >= 2) {
                stateSet.add(name.substring(name.length() - 2));
            }
        }

        String[] states = stateSet.toArray(new String[0]);
        Arrays.sort(states);
        return states;
    }

    private String[] formatToStringArray(JSONArray array) {
        String[] result = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            try {
                result[i] = array.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}