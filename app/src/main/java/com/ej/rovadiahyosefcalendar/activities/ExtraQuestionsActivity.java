package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSharedPreferences;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;
@Deprecated
public class ExtraQuestionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_extra_questions);

        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        if (Utils.isLocaleHebrew()) {
            materialToolbar.setSubtitle("");
        }

//        LinearLayout layout = findViewById(R.id.calendar_buttons);
//        float screenWidth = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
//        if (screenWidth < 400) {
//            layout.setOrientation(LinearLayout.VERTICAL);
//        }

        Button yes = findViewById(R.id.yesButton);
        Button skipForNow = findViewById(R.id.skipForNowButton);

        yes.setOnClickListener(v -> {
            startActivity(new Intent(this, SetupChooserActivity.class).putExtra("fromSetup", true));
            finish();
        });
        skipForNow.setOnClickListener(v -> {
            sSharedPreferences.edit().putBoolean("isSetup", true).apply();
            if (sSharedPreferences.getBoolean("hasNotShownTipScreen", true)) {
                startActivity(new Intent(getBaseContext(), TipScreenActivity.class));
                sSharedPreferences.edit().putBoolean("hasNotShownTipScreen", false).apply();
            }
            finish();
        });

        Button difference = findViewById(R.id.whats_this_button);
        difference.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.what_is_this)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .setMessage(R.string.visible_sunrise_message)
                .show());

        ViewCompat.setOnApplyWindowInsetsListener(difference, (v, windowInsets) -> {
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
                if (Objects.equals(getIntent().getStringExtra("sender"), "ZmanimLang")) {
                    startActivity(new Intent(getApplicationContext(), ZmanimLanguageActivity.class));
                } else if (Objects.equals(getIntent().getStringExtra("sender"), "InIsrael")) {
                    startActivity(new Intent(getApplicationContext(), InIsraelActivity.class));
                } else if (Objects.equals(getIntent().getStringExtra("sender"), "GetUserLocation")) {
                    startActivity(new Intent(getApplicationContext(), GetUserLocationWithMapActivity.class));
                }
                finish();
            }
        });
    }
}