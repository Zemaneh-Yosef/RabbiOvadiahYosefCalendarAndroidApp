package com.ej.rovadiahyosefcalendar.activities;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.appbar.MaterialToolbar;

public class NotificationSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.settings_activity);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setTitle(getString(R.string.zmanim_notifications_settings));
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onUserInteraction() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        super.onUserInteraction();
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