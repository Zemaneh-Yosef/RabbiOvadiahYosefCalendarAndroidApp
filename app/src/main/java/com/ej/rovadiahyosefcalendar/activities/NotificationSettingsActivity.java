package com.ej.rovadiahyosefcalendar.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;

public class NotificationSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.zmanim_notifications_preferences, rootKey);

            boolean isLuachAmudeiHoraah = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getBoolean("LuachAmudeiHoraah", false);

            if (isLuachAmudeiHoraah) {
                Preference minchaKetana = findPreference("MinchaKetana");
                if (minchaKetana != null) {
                    minchaKetana.setVisible(false);
                }
                Preference fast = findPreference("FastEnd");
                if (fast != null) {
                    fast.setVisible(false);
                }
                Preference fastStringent = findPreference("FastEndStringent");
                if (fastStringent != null) {
                    fastStringent.setVisible(false);
                }
            } else {
                Preference plagHaMinchaYY = findPreference("PlagHaMinchaYY");
                if (plagHaMinchaYY != null) {
                    plagHaMinchaYY.setTitle("Plag HaMincha");
                }
                Preference plagHaMinchaHB = findPreference("PlagHaMinchaHB");
                if (plagHaMinchaHB != null) {
                    plagHaMinchaHB.setVisible(false);
                }
                Preference tzeitHacochavimLChumra = findPreference("TzeitHacochavimLChumra");
                if (tzeitHacochavimLChumra != null) {
                    tzeitHacochavimLChumra.setVisible(false);
                }
            }
        }


    }
}