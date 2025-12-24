package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.BuildConfig;
import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.MaterialButtonToggleGroupPreference;
import com.github.tttt55.materialyoupreferences.preferences.MaterialColorPreference;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

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
                if (getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                    break;
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Day":
                if (getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
                    break;
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Night":
                if (getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    break;
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.settings_activity);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_activity), (v, windowInsets) -> {
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
                .replace(R.id.settings_activity, new SettingsFragment())
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

            hideDisabledViews();

            Preference showSecondsPref = findPreference("ShowSeconds");
            if (showSecondsPref != null) {
                showSecondsPref.setOnPreferenceClickListener(preference -> {
                    if (Objects.requireNonNull(preference.getSharedPreferences()).getBoolean("ShowSeconds", false)) {
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

            Preference themePref = findPreference("theme");
            if (themePref != null) {
                themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String theme = (String) newValue;
                    Objects.requireNonNull(preference.getSharedPreferences()).edit().putString(preference.getKey(), theme).apply();
                    if (isAdded()) {
                        switch (theme) {
                            case "Auto (Follow System Theme)":
                                if (requireActivity() instanceof AppCompatActivity activity) {
                                    if (activity.getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                                        break;
                                    }
                                }
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                                requireActivity().recreate();
                                break;
                            case "Day":
                                if (requireActivity() instanceof AppCompatActivity activity) {
                                    if (activity.getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
                                        break;
                                    }
                                }
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                                requireActivity().recreate();
                                break;
                            case "Night":
                                if (requireActivity() instanceof AppCompatActivity activity) {
                                    if (activity.getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                                        break;
                                    }
                                }
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                                requireActivity().recreate();
                                break;
                        }
                    }
                    return false;
                });
            }

            MaterialColorPreference backgroundColorPref = findPreference("backgroundColor");
            if (backgroundColorPref != null) {
                backgroundColorPref.setOnPreferenceClickListener(preference -> {
                    new ColorPickerDialog.Builder(requireContext())
                        .setTitle(getString(R.string.set_background_color))
                        .setPreferenceName("backgroundColor")
                        .setPositiveButton(getString(R.string.confirm), (ColorEnvelopeListener) (envelope, fromUser) -> {
                            if (fromUser) {
                                requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("useDefaultBackgroundColor", false)
                                    .putBoolean("customBackgroundColor", true)
                                    .putBoolean("useImage", false)
                                    .putInt("bColor", envelope.getColor())
                                    .apply();
                                PreferenceManager.getDefaultSharedPreferences(requireContext())
                                    .edit()
                                    .putInt("backgroundColor", envelope.getColor())
                                    .apply();
                                backgroundColorPref.setColor(envelope.getColor());
                            }
                        })
                        .setNeutralButton(requireContext().getString(R.string._default), (dialogInterface, i) -> {
                            requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE)
                                .edit()
                                .putBoolean("useDefaultBackgroundColor", true)
                                .putBoolean("customBackgroundColor", false)
                                .putBoolean("useImage", false)
                                .apply();
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .edit()
                                .putInt("backgroundColor", 0x32312C)
                                .apply();
                            backgroundColorPref.setColor(0x32312C);
                        })
                        .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                        .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                        .show();
                    return true;
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
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
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
            );

            MaterialColorPreference textColorPref = findPreference("textColor");
            if (textColorPref != null) {
                textColorPref.setOnPreferenceClickListener(preference -> {
                    new ColorPickerDialog.Builder(requireContext())
                        .setTitle(getString(R.string.set_text_color))
                        .setPreferenceName("textColor")
                        .setPositiveButton(getString(R.string.confirm), (ColorEnvelopeListener) (envelope, fromUser) -> {
                            if (fromUser) {
                                requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("customTextColor", true)
                                    .putInt("tColor", envelope.getColor())
                                    .apply();
                                PreferenceManager.getDefaultSharedPreferences(requireContext())
                                    .edit()
                                    .putInt("textColor", envelope.getColor())
                                    .apply();
                                textColorPref.setColor(envelope.getColor());
                            }
                        })
                        .setNeutralButton(requireContext().getString(R.string._default), (dialogInterface, i) -> {
                            requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE)
                                .edit()
                                .putBoolean("customTextColor", false)
                                .apply();
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .edit()
                                .putInt("textColor", 0xFFFFFF)
                                .apply();
                            textColorPref.setColor(0xFFFFFF);
                        })
                        .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                        .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                        .show();
                    return true;
                });
            }

            MaterialColorPreference calendarButtonColorPref = findPreference("calendarButtonColor");
            if (calendarButtonColorPref != null) {
                calendarButtonColorPref.setOnPreferenceClickListener(preference -> {
                    new ColorPickerDialog.Builder(requireContext())
                        .setTitle(getString(R.string.set_calendar_button_color))
                        .setPreferenceName("calendarButtonColor")
                        .setPositiveButton(getString(R.string.confirm), (ColorEnvelopeListener) (envelope, fromUser) -> {
                            if (fromUser) {
                                requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("useDefaultCalButtonColor", false)
                                    .putInt("CalButtonColor", envelope.getColor())
                                    .apply();
                                PreferenceManager.getDefaultSharedPreferences(requireContext())
                                    .edit()
                                    .putInt("calendarButtonColor", envelope.getColor())
                                    .apply();
                                calendarButtonColorPref.setColor(envelope.getColor());
                            }
                        })
                        .setNeutralButton(requireContext().getString(R.string._default), (dialogInterface, i) -> {
                            requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE)
                                .edit()
                                .putBoolean("useDefaultCalButtonColor", true)
                                .apply();
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .edit()
                                .putInt("calendarButtonColor", 0xFFFFFF)
                                .apply();
                            calendarButtonColorPref.setColor(0xFFFFFF);
                        })
                        .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                        .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                        .show();
                    return true;
                });
            }

            Preference contactUsPref = findPreference(getResources().getString(R.string.contact_header));
            PackageManager packageManager = requireActivity().getPackageManager();
            if (contactUsPref != null) {
                contactUsPref.setOnPreferenceClickListener(v -> {
                    Intent email = new Intent(Intent.ACTION_SENDTO);
                    email.setData(Uri.parse("mailto:"));
                    email.putExtra(Intent.EXTRA_CC, new String[]{"neimmaor@gmail.com"});
                    email.putExtra(Intent.EXTRA_EMAIL, new String[]{"elyahujacobi@gmail.com"}); //developer's email
                    email.putExtra(Intent.EXTRA_SUBJECT,"Zemaneh Yosef (Android)"); //Email's Subject
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
                    String[] options = {
                            getString(R.string.haskama_by_rabbi_yitzhak_yosef),
                            getString(R.string.by_rav_ben_chaim),
                            getString(R.string.by_rav_elbaz),
                            getString(R.string.by_rav_dahan)
                    };

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.haskamot)
                            .setItems(options, (dialog, which) -> {
                                String url = switch (which) {
                                    case 0 -> "https://royzmanim.com/assets/haskamah-rishon-letzion.pdf";
                                    case 1 -> "https://royzmanim.com/assets/RBH_Recommendation_Final.pdf";
                                    case 2 -> "https://royzmanim.com/assets/Haskamah.pdf";
                                    case 3 -> "https://royzmanim.com/assets/%D7%94%D7%A1%D7%9B%D7%9E%D7%94.pdf";
                                    default -> null;
                                };
                                if (url != null) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    startActivity(browserIntent);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return false;
                });
            }

            MaterialButtonToggleGroupPreference lang = findPreference("language");
            if (lang != null) {
                lang.setOnPreferenceClickListener(l -> {
                    Toast.makeText(getContext(), getString(R.string.restart_app), Toast.LENGTH_SHORT).show();
                    return false;
                });
            }

            Preference debug = findPreference("showNotifDebugLog");
            if (debug != null) {
                debug.setVisible(BuildConfig.DEBUG);
                debug.setOnPreferenceClickListener(v -> {
                    Intent intent = new Intent(getContext(), ShowErrorActivity.class);
                    intent.putExtra("error", requireContext().getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getString("debugNotifs", ""))
                            .putExtra("isNotifDebug", true);
                    startActivity(intent);
                    return false;
                });
            }

        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            hideDisabledViews();
        }

        private void hideDisabledViews() {
            Preference zmanimNotificationPreference = findPreference("zmanim_notifications");
            Preference notificationPreference = findPreference("zmanim_notifications_settings");
            if (zmanimNotificationPreference != null && notificationPreference != null) {
                notificationPreference.setVisible(PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("zmanim_notifications", true));
                zmanimNotificationPreference.setOnPreferenceClickListener(preference -> {
                    notificationPreference.setVisible(Objects.requireNonNull(preference.getSharedPreferences()).getBoolean("zmanim_notifications", true));
                    return false;
                });
            }
            Preference showWhenShabbatChagEnds = findPreference("ShowWhenShabbatChagEnds");
            Preference displayRTOrShabbatRegTime = findPreference("displayRTOrShabbatRegTime");
            if (showWhenShabbatChagEnds != null && displayRTOrShabbatRegTime != null) {
                displayRTOrShabbatRegTime.setVisible(PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("ShowWhenShabbatChagEnds", false));
                showWhenShabbatChagEnds.setOnPreferenceClickListener(preference -> {
                    displayRTOrShabbatRegTime.setVisible(Objects.requireNonNull(preference.getSharedPreferences()).getBoolean("ShowWhenShabbatChagEnds", false));
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
                if (getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                    break;
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Day":
                if (getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
                    break;
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Night":
                if (getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    break;
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
        super.onResume();
    }
}
