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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class SetupChooserActivity extends AppCompatActivity {

    private AlertDialog mAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup_chooser);

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
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
            finish();
        });

        advancedSetupButton.setOnClickListener(v -> {
            startActivity(new Intent(this, AdvancedSetupActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                            .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
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