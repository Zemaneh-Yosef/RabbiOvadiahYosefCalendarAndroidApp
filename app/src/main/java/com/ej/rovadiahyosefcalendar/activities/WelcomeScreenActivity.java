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
        haskamot.setOnClickListener(l -> {
            String[] options = {
                    getString(R.string.haskama_by_rabbi_yitzhak_yosef),
                    getString(R.string.by_rav_ben_chaim),
                    getString(R.string.by_rav_elbaz),
                    getString(R.string.by_rav_dahan)
            };

            new MaterialAlertDialogBuilder(this)
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
        });
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