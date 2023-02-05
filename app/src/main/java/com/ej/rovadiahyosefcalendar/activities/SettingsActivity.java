package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sFromSettings;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.rarepebble.colorpicker.ColorPreference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;

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
            sFromSettings = true;
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private ActivityResultLauncher<Intent> mResultLauncher;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference notificationPreference = findPreference("zmanim_notifications_settings");
            if (notificationPreference != null) {
                notificationPreference.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(getActivity(), NotificationSettingsActivity.class);
                    startActivity(intent);
                    return true;
                });
            }

            Preference themePref = findPreference("theme");
            if (themePref != null) {
                themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String theme = (String) newValue;
                    Objects.requireNonNull(preference.getSharedPreferences()).edit().putString(preference.getKey(), theme).apply();
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

            Preference showSecondsPref = findPreference("ShowSeconds");
            if (showSecondsPref != null) {
                showSecondsPref.setOnPreferenceClickListener(preference  -> {
                    boolean isOn = Objects.requireNonNull(preference.getSharedPreferences()).getBoolean("ShowSeconds",false);
                    if (isOn) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Do NOT rely on the seconds!")
                                .setMessage("DO NOT RELY ON THESE SECONDS. " +
                                        "The only zman that can be relied on to the second is the visible sunrise time based on chaitables.com. " +
                                        "Otherwise, these zmanim are NOT accurate to the second! You should always round up or down a minute " +
                                        "or two just in case.")
                                .setPositiveButton("Ok", (dialogInterface, i) -> { })
                                .create()
                                .show();
                    }
                    return false;
                });
            }

            Preference backgroundPref = findPreference("background");
            if (backgroundPref != null) {
                backgroundPref.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    mResultLauncher.launch(intent);
                    return false;
                });
            }

            mResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            if (result.getData() != null) {
                                Intent picture = result.getData();
                                Uri selectedImage = picture.getData();
                                Cursor returnCursor = requireContext().getContentResolver()
                                        .query(selectedImage, null, null, null, null);
                                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                returnCursor.moveToFirst();
                                String name = (returnCursor.getString(nameIndex));
                                File file = new File(requireContext().getFilesDir(), "background");
                                try {
                                    InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImage);
                                    FileOutputStream outputStream = new FileOutputStream(file);
                                    int read = 0;
                                    int maxBufferSize = 1024 * 1024;
                                    int bytesAvailable = inputStream.available();
                                    int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                                    final byte[] buffers = new byte[bufferSize];
                                    while ((read = inputStream.read(buffers)) != -1) {
                                        outputStream.write(buffers, 0, read);
                                    }
                                    inputStream.close();
                                    outputStream.close();
                                    returnCursor.close();
                                } catch (Exception e) {
                                    Log.e("Exception", e.getMessage());
                                }

                                requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE)
                                        .edit()
                                        .putString("imageLocation", file.getPath())
                                        .putBoolean("useImage", true)
                                        .apply();
                            }
                        }
                    }
            );

            Preference contactUsPref = findPreference(getResources().getString(R.string.contact_header));
            PackageManager packageManager = requireActivity().getPackageManager();
            if (contactUsPref != null) {
                contactUsPref.setOnPreferenceClickListener(v -> {
                    Intent email = new Intent(Intent.ACTION_SENDTO);
                    email.setData(Uri.parse("mailto:"));
                    email.putExtra(Intent.EXTRA_EMAIL, new String[]{"elyahujacobi@gmail.com"}); //developer's email
                    email.putExtra(Intent.EXTRA_SUBJECT,"Support Ticket"); //Email's Subject
                    email.putExtra(Intent.EXTRA_TEXT,"Dear Mr. Elyahu, "); //Email's Greeting text

                    if (packageManager.resolveActivity(email,0) != null) { // there is an activity that can handle it
                        startActivity(email);
                    } else {
                        Toast.makeText(getContext(),"No email app...", Toast.LENGTH_SHORT)
                                .show();
                    }
                    return false;
                });
            }

            Preference help = findPreference("help");
            if (help != null) {
                help.setOnPreferenceClickListener(v -> {
                    new AlertDialog.Builder(requireContext(), R.style.Theme_AppCompat_DayNight)
                            .setTitle("Help using this app:")
                            .setPositiveButton("ok", null)
                            .setMessage(R.string.helper_text)
                            .show();
                    return false;
                });
            }
        }

        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            if (preference instanceof ColorPreference) {
                ColorPreference colorPreference = ((ColorPreference) preference);
                colorPreference.showDialog(this, 0);
                SharedPreferences.Editor editor = requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
                colorPreference.setOnPreferenceChangeListener((preference1, newValue) -> {
                    switch (preference1.getKey()) {
                        case "backgroundColor"://FIXME there is a bug with the dialog that will make the color transparent by default. Only happens for the background
                            if (newValue == null) {
                                newValue = 0x32312C;//default gray hex
                                editor.putBoolean("useDefaultBackgroundColor", true)
                                        .putInt("bColor", (Integer) newValue)
                                        .apply();
                            } else {
                                editor.putBoolean("useDefaultBackgroundColor", false)
                                        .putInt("bColor", (Integer) newValue)
                                        .apply();
                            }
                            editor.putBoolean("useImage", false)
                                    .putBoolean("customBackgroundColor", true)
                                    .putInt("bColor", (Integer) newValue)
                                    .apply();
                            break;
                        case "textColor":
                            editor.putBoolean("customTextColor", true)
                                    .putInt("tColor", (Integer) newValue)
                                    .apply();
                            break;
                        case "calendarButtonColor":
                            if (newValue != null) {
                                editor.putBoolean("useDefaultCalButtonColor", false)
                                        .apply();
                                editor.putInt("CalButtonColor", (Integer) newValue)
                                        .apply();
                            } else {
                                editor.putBoolean("useDefaultCalButtonColor", true)
                                        .apply();
                            }
                            break;
                    }
                    colorPreference.setColor((Integer) newValue);
                    return false;
                });
            } else {
                super.onDisplayPreferenceDialog(preference);
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

    @Override
    public void onBackPressed() {
        sFromSettings = true;
        super.onBackPressed();
    }
}
