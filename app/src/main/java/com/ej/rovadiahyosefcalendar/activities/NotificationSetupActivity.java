package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.classes.ZmanimShowcase;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * One page with every setting the zmanim notifications need to arrive on time. It replaces the dialogs that used to pop up on the
 * zmanim page every time the app was opened. Each row shows a check mark once its setting is fixed, and the rows are checked again
 * whenever the user comes back to the page (e.g. from the system settings).
 * It is shown once at the end of the setup, and can be opened again from the notification settings.
 */
public class NotificationSetupActivity extends AppCompatActivity {

    /**
     * Set when a setting was fixed on this page, so the zmanim page can schedule the notifications again with the new settings.
     */
    public static boolean sSettingsChanged;

    /**
     * Shows this page at the end of the first setup, and turns on the walkthrough of the zmanim list. Does nothing when the user only
     * changed their location or language later on. The preference keeps its old name, from when a tip screen was shown here, so users
     * who already went through the setup don't see either of them again.
     */
    public static void showAfterFirstSetup(Activity activity, SharedPreferences sharedPreferences) {
        if (sharedPreferences.getBoolean("hasNotShownTipScreen", true)) {
            activity.startActivity(new Intent(activity, NotificationSetupActivity.class));
            sharedPreferences.edit()
                    .putBoolean("hasNotShownTipScreen", false)
                    .putBoolean(ZmanimShowcase.PREF_SHOW_SHOWCASE, true)
                    .apply();
        }
    }

    private final List<Row> mRows = new ArrayList<>();
    private ActivityResultLauncher<String> mPermissionLauncher;
    private Runnable mOnPermissionDenied;

    private static class Row {
        final View view;
        final BooleanSupplier isDone;
        boolean wasDone;

        Row(View view, BooleanSupplier isDone) {
            this.view = view;
            this.isDone = isDone;
            this.wasDone = isDone.getAsBoolean();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_setup);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        if (getIntent().getBooleanExtra("fromSettings", false)) {
            toolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (!granted && mOnPermissionDenied != null) {
                mOnPermissionDenied.run();// Android won't ask again after the user said no, so open the settings instead
            }
            mOnPermissionDenied = null;
            refreshRows();
        });

        LinearLayout rows = findViewById(R.id.setup_rows);

        addRow(rows, R.drawable.ic_notifications_black_24dp, R.string.setup_allow_notifications, R.string.setup_allow_notifications_description,
                () -> NotificationManagerCompat.from(this).areNotificationsEnabled(),
                () -> {
                    Runnable openSettings = () -> Utils.startActivitySafely(this, new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                        mOnPermissionDenied = openSettings;
                        mPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        openSettings.run();
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addRow(rows, R.drawable.ic_baseline_alarm_24, R.string.setup_exact_alarms, R.string.setup_exact_alarms_description,
                    () -> ((AlarmManager) getSystemService(ALARM_SERVICE)).canScheduleExactAlarms(),
                    () -> {
                        try {
                            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
                        } catch (Exception e) {// e.g. on some TVs
                            openAppDetails();
                        }
                    });
        }

        addRow(rows, R.drawable.baseline_battery_full_24, R.string.setup_battery, R.string.setup_battery_description,
                () -> ((PowerManager) getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName()),
                () -> {
                    try {
                        startActivity(new Intent("android.settings.APP_BATTERY_USAGE")
                                .putExtra("android.intent.extra.PACKAGE_NAME", getPackageName())
                                .setData(Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) {// the battery page of the app doesn't exist on every device
                        openAppDetails();
                    }
                });

        // Location all the time only matters to users who use the device's location (not a zip code or a saved location)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED)) {
            addRow(rows, R.drawable.baseline_location_on_24, R.string.setup_background_location, R.string.setup_background_location_description,
                    () -> ContextCompat.checkSelfPermission(this, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED,
                    () -> {
                        mOnPermissionDenied = this::openAppDetails;
                        mPermissionLauncher.launch(ACCESS_BACKGROUND_LOCATION);
                    });
        }

        Button continueButton = findViewById(R.id.setup_continue);
        continueButton.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRows();// the user may have changed a setting in the system settings
    }

    private void addRow(ViewGroup parent, @DrawableRes int icon, @StringRes int title, @StringRes int description,
                        BooleanSupplier isDone, Runnable fix) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_notification_setup, parent, false);
        ImageView iconView = view.findViewById(R.id.setup_icon);
        iconView.setImageResource(icon);
        iconView.setImageTintList(ColorStateList.valueOf(((TextView) view.findViewById(R.id.setup_title)).getCurrentTextColor()));
        ((TextView) view.findViewById(R.id.setup_title)).setText(title);
        ((TextView) view.findViewById(R.id.setup_description)).setText(description);
        view.findViewById(R.id.setup_fix).setOnClickListener(v -> fix.run());
        parent.addView(view);
        mRows.add(new Row(view, isDone));
    }

    private void refreshRows() {
        for (Row row : mRows) {
            boolean done = row.isDone.getAsBoolean();
            if (done && !row.wasDone) {
                sSettingsChanged = true;
            }
            row.wasDone = done;
            row.view.findViewById(R.id.setup_fix).setVisibility(done ? View.GONE : View.VISIBLE);
            row.view.findViewById(R.id.setup_done).setVisibility(done ? View.VISIBLE : View.GONE);
        }
    }

    private void openAppDetails() {
        Utils.startActivitySafely(this, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }
}
