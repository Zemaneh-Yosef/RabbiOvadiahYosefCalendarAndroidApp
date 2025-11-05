package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.royrodriguez.transitionbutton.TransitionButton;

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
        TransitionButton getStarted = findViewById(R.id.getStartedButton);
        GradientDrawable squareBackground = new GradientDrawable();
        squareBackground.setColor(ContextCompat.getColor(this, R.color.light_blue)); // or your app color
        squareBackground.setCornerRadius(0f); // removes rounding

        getStarted.setBackground(squareBackground);
        getStarted.setOnClickListener(l -> {
            getStarted.setText("");
            getStarted.stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, () -> {
                startActivity(new Intent(getApplicationContext(), GetUserLocationWithMapActivity.class));
                finish();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out, 0);
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out, 0);
                } else {
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("isSetup", false)) {
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