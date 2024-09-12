package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                break;
            case "Day":
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                break;
            case "Night":
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                break;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.settings_activity);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
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
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private ActivityResultLauncher<Intent> mResultLauncher;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference zmanimPreferences = findPreference("zmanim_settings");
            if (zmanimPreferences != null) {
                zmanimPreferences.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(getActivity(), ZmanimSettingsActivity.class);
                    startActivity(intent);
                    return true;
                });
            }

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
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.do_not_rely_on_the_seconds)
                                .setMessage(R.string.do_not_rely_on_the_seconds_message)
                                .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
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
                                Cursor returnCursor = null;
                                if (selectedImage != null) {
                                    returnCursor = requireContext().getContentResolver()
                                            .query(selectedImage, null, null, null, null);
                                }
                                int nameIndex = 0;
                                if (returnCursor != null) {
                                    nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                }
                                if (returnCursor != null) {
                                    returnCursor.moveToFirst();
                                    String name = (returnCursor.getString(nameIndex));
                                }
                                File file = new File(requireContext().getFilesDir(), "background");
                                try {
                                    InputStream inputStream = null;
                                    if (selectedImage != null) {
                                        inputStream = requireContext().getContentResolver().openInputStream(selectedImage);
                                    }
                                    FileOutputStream outputStream = new FileOutputStream(file);
                                    int read = 0;
                                    int maxBufferSize = 1024 * 1024;
                                    int bytesAvailable = 0;
                                    if (inputStream != null) {
                                        bytesAvailable = inputStream.available();
                                    }
                                    int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                                    final byte[] buffers = new byte[bufferSize];
                                    while ((read = inputStream != null ? inputStream.read(buffers) : -1) != -1) {
                                        outputStream.write(buffers, 0, read);
                                    }
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                    outputStream.close();
                                    if (returnCursor != null) {
                                        returnCursor.close();
                                    }
                                } catch (Exception e) {
                                    Log.e("Exception", Objects.requireNonNull(e.getMessage()));
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
                    email.putExtra(Intent.EXTRA_SUBJECT,"Zmanei Yosef (Android)"); //Email's Subject
                    email.putExtra(Intent.EXTRA_TEXT,""); //Email's Greeting text

                    if (packageManager.resolveActivity(email,0) != null) { // there is an activity that can handle it
                        startActivity(email);
                    } else {
                        Toast.makeText(getContext(), R.string.No_email_app_error, Toast.LENGTH_SHORT)
                                .show();
                    }
                    return false;
                });
            }

            Preference haskamaPref = findPreference("haskamot");
            if (haskamaPref != null) {
                haskamaPref.setOnPreferenceClickListener(v -> {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.haskamot)
                            .setMessage(R.string.haskamot_message)
                            .setPositiveButton(R.string.by_rav_elbaz, (dialog, which) -> {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://royzmanim.com/assets/Haskamah.pdf"));
                                startActivity(browserIntent);
                            })
                            .setNegativeButton(R.string.by_rav_dahan, (dialog, which) -> {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://royzmanim.com/assets/%D7%94%D7%A1%D7%9B%D7%9E%D7%94.pdf"));
                                startActivity(browserIntent);
                            })
                            .create()
                            .show();
                    return false;
                });
            }

            Preference help = findPreference("help");
            if (help != null) {
                help.setOnPreferenceClickListener(v -> {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.help_using_this_app)
                            .setPositiveButton(R.string.ok, null)
                            .setMessage(R.string.helper_text)
                            .show();
                    return false;
                });
            }

            Preference lang = findPreference("language");
            if (lang != null) {
                lang.setOnPreferenceChangeListener((preference, newValue) -> {
                    Objects.requireNonNull(preference.getSharedPreferences()).edit().putString(preference.getKey(), (String) newValue).apply();
                    Toast.makeText(getContext(), getString(R.string.restart_app), Toast.LENGTH_LONG).show();
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
                                editor.putBoolean("useDefaultBackgroundColor", true)
                                        .putInt("bColor", (Integer) 0x32312C)
                                        .apply();
                            } else {
                                editor.putBoolean("useDefaultBackgroundColor", false)
                                        .putInt("bColor", (Integer) newValue)
                                        .apply();
                            }
                            editor.putBoolean("useImage", false)
                                    .putBoolean("customBackgroundColor", true)
                                    .apply();
                            break;
                        case "textColor":
                            if (newValue == null) {
                                editor.putBoolean("customTextColor", false).apply();
                            } else {
                                editor.putBoolean("customTextColor", true)
                                        .putInt("tColor", (Integer) newValue)
                                        .apply();
                            }
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
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                break;
            case "Day":
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                break;
            case "Night":
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                break;
        }
        super.onResume();
    }
}
