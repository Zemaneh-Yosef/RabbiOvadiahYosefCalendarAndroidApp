package com.ej.rovadiahyosefcalendar.activities;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SetupChooserActivity extends AppCompatActivity {

    private AlertDialog mAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup_chooser);

        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        if (Utils.isLocaleHebrew()) {
            materialToolbar.setSubtitle("");
        }

        mAlertDialog = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.Introduction)
            .setMessage(R.string.intro)
            .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
            .setCancelable(false)
            .create();

        TextView showIntro = findViewById(R.id.showIntro);
        showIntro.setPaintFlags(showIntro.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        showIntro.setOnClickListener(v -> mAlertDialog.show());

        Button quickSetupButton = findViewById(R.id.quickSetup);
        Button advancedSetupButton = findViewById(R.id.advancedSetup);

        quickSetupButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SimpleSetupActivity.class)
                .putExtra("fromSetup", SetupChooserActivity.this.getIntent().getBooleanExtra("fromSetup", false)));
            finish();
        });

        advancedSetupButton.setOnClickListener(v -> {
            startActivity(new Intent(this, AdvancedSetupActivity.class)
                .putExtra("fromSetup", SetupChooserActivity.this.getIntent().getBooleanExtra("fromSetup", false)));
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(showIntro, (v, windowInsets) -> {
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
            public void handleOnBackPressed() {finish();}
        });
    }
}