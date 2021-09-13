package com.elyjacobi.ROvadiahYosefCalendar.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.elyjacobi.ROvadiahYosefCalendar.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
}
