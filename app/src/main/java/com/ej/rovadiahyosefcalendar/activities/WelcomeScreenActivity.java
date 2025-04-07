package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSharedPreferences;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WelcomeScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome_screen);

        Button aboutUs = findViewById(R.id.aboutUsLink);
        aboutUs.setOnClickListener(l -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_us)
                .setMessage(R.string.about_us_text)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss()).show());
        Button haskamot = findViewById(R.id.haskamotLink);
        haskamot.setOnClickListener(l -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.haskamot)
                .setMessage(R.string.haskamot_message)
                .setPositiveButton(R.string.haskama_by_rabbi_yitzhak_yosef, (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://royzmanim.com/assets/haskamah-rishon-letzion.pdf"));
                    startActivity(browserIntent);
                })
                .setNeutralButton(R.string.by_rav_elbaz, (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://royzmanim.com/assets/Haskamah.pdf"));
                    startActivity(browserIntent);
                })
                .setNegativeButton(R.string.by_rav_dahan, (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://royzmanim.com/assets/%D7%94%D7%A1%D7%9B%D7%9E%D7%94.pdf"));
                    startActivity(browserIntent);
                })
                .create()
                .show());
        Button getStarted = findViewById(R.id.getStartedButton);
        getStarted.setOnClickListener(l -> {
            startActivity(new Intent(getApplicationContext(), GetUserLocationWithMapActivity.class));
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (sSharedPreferences.getBoolean("isSetup", false)) {
                    finish();
                } else {
                    finishAffinity();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.welcome_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}