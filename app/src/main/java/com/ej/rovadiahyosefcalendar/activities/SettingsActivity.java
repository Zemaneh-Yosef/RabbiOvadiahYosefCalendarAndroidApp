package com.ej.rovadiahyosefcalendar.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences mSettingsPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = mSettingsPreferences.getString("theme", "Auto (Follow System Theme)");
        switch (theme) {
            case "Auto (Follow System Theme)":
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Day":
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Night":
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference themePref = findPreference("theme");
            if (themePref != null) {
                themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String theme = (String) newValue;
                    preference.getSharedPreferences().edit().putString(preference.getKey(), theme).apply();
                    switch (theme) {
                        case "Auto (Follow System Theme)":
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            requireActivity().recreate();
                            break;
                        case "Day":
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            requireActivity().recreate();
                            break;
                        case "Night":
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            requireActivity().recreate();
                            break;
                    }
                    return false;
                });
            }

            Preference contactUsPref = findPreference(getResources().getString(R.string.contact_header));

            PackageManager packageManager = requireActivity().getPackageManager();

            if (contactUsPref != null) {
                contactUsPref.setOnPreferenceClickListener(v -> {
                    Intent email = new Intent(Intent.ACTION_SENDTO);
                    email.setData(Uri.parse("mailto:"));
                    email.putExtra(Intent.EXTRA_EMAIL, new String[]{"elyahujacobi@gmail.com"}); //developer's email
                    email.putExtra(Intent.EXTRA_SUBJECT,"Support Ticket"); //Email's Subject
                    email.putExtra(Intent.EXTRA_TEXT,"Dear Mr. Elyahu,"); //Email's Greeting text

                    if (packageManager.resolveActivity(email,0) != null) { // there is an activity that can handle it
                        startActivity(email);
                    } else {
                        Toast.makeText(getContext(),"No email app...", Toast.LENGTH_SHORT)
                                .show();
                    }
                    return false;
                });
            }
        }
    }

    @Override
    protected void onResume() {
        String theme = mSettingsPreferences.getString("theme", "Auto (Follow System Theme)");
        switch (theme) {
            case "Auto (Follow System Theme)":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Day":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Night":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
        super.onResume();
    }
}
