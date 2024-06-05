package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class FullSetupActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_full_setup);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            materialToolbar.setSubtitle("");
        }
        materialToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.help) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.help_using_this_app)
                        .setPositiveButton(R.string.ok, null)
                        .setMessage(R.string.helper_text)
                        .show();
                return true;
            } else if (id == R.id.skipSetup) {
                finish();
                return true;
            } else if (id == R.id.restart) {
                startActivity(new Intent(this, FullSetupActivity.class));
                finish();
                return true;
            }
            return false;
        });

        LinearLayout layout = findViewById(R.id.israel_buttons);
        float screenWidth = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
        if (screenWidth < 400) {
            layout.setOrientation(LinearLayout.VERTICAL);
        }
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        Button inIsraelButton = findViewById(R.id.inIsraelButton);
        Button notInIsrael = findViewById(R.id.notInIsraelButton);

        inIsraelButton.setOnClickListener(v -> saveInfoAndStartActivity(true));
        notInIsrael.setOnClickListener(v -> saveInfoAndStartActivity(false));

        ViewCompat.setOnApplyWindowInsetsListener(layout, (v, windowInsets) -> {
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

    private void saveInfoAndStartActivity(boolean b) {
        mSharedPreferences.edit().putBoolean("inIsrael", b).apply();
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            mSharedPreferences.edit().putBoolean("isZmanimInHebrew", true).apply();
            mSharedPreferences.edit().putBoolean("isZmanimEnglishTranslated", false).apply();
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
                    !mSharedPreferences.getBoolean("useZipcode", false)) {
                mSharedPreferences.edit().putBoolean("shouldRefresh", true).apply();
                startActivity(new Intent(this, GetUserLocationWithMapActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
            } else {
                if (!b) { // not in israel
                    startActivity(new Intent(this, CalendarChooserActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                }
            }
            mSharedPreferences.edit().putBoolean("isSetup", true).apply();
        } else {
            startActivity(new Intent(this, ZmanimLanguageActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_FORWARD_RESULT));
        }
        finish();
    }
}