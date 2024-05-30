package com.ej.rovadiahyosefcalendar.activities;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings), (v, windowInsets) -> {
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