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

            showRelevantZmanim();
        }

        private void showRelevantZmanim() {
            boolean isLuachAmudeiHoraah = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getBoolean("LuachAmudeiHoraah", false);

            if (isLuachAmudeiHoraah) {
                Preference fast = findPreference("FastEnd");
                if (fast != null) {
                    fast.setVisible(false);
                }
                Preference fastStringent = findPreference("FastEndStringent");
                if (fastStringent != null) {
                    fastStringent.setVisible(false);
                }
            } else {
                String plagOpinions = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("plagOpinion", "1");
                if (plagOpinions.equals("1")) {
                    Preference plagHaMinchaYY = findPreference("PlagHaMinchaYY");
                    if (plagHaMinchaYY != null) {
                        plagHaMinchaYY.setTitle("Plag HaMincha");//only show plag haMincha for Yalkut Yosef
                    }
                    Preference plagHaMinchaHB = findPreference("PlagHaMinchaHB");
                    if (plagHaMinchaHB != null) {
                        plagHaMinchaHB.setVisible(false);
                    }
                }
                if (plagOpinions.equals("2")) {
                    Preference plagHaMinchaYY = findPreference("PlagHaMinchaYY");
                    if (plagHaMinchaYY != null) {
                        plagHaMinchaYY.setVisible(false);
                    }
                    Preference plagHaMinchaHB = findPreference("PlagHaMinchaHB");
                    if (plagHaMinchaHB != null) {
                        plagHaMinchaHB.setVisible(true);//only show plag haMincha for Halacha Berura
                    }
                }
                if (plagOpinions.equals("3")) {
                    Preference plagHaMinchaYY = findPreference("PlagHaMinchaYY");
                    if (plagHaMinchaYY != null) {
                        plagHaMinchaYY.setVisible(true);
                    }
                    Preference plagHaMinchaHB = findPreference("PlagHaMinchaHB");
                    if (plagHaMinchaHB != null) {
                        plagHaMinchaHB.setVisible(true);
                    }
                }

                boolean showTzeitLchumra = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("alwaysShowTzeitLChumra", false);
                Preference tzeitHacochavimLChumra = findPreference("TzeitHacochavimLChumra");
                if (tzeitHacochavimLChumra != null) {
                    tzeitHacochavimLChumra.setVisible(showTzeitLchumra);
                }
            }
        }


    }
}