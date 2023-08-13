package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SetupChooserActivity extends AppCompatActivity {

    private SharedPreferences.Editor mEditor;
    private AlertDialog mAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_chooser);
        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_custom);//center the title
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mEditor = sharedPreferences.edit();

        mAlertDialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
                .setTitle(R.string.Introduction)
                .setMessage(R.string.intro)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> { })
                .setCancelable(false)
                .create();

        TextView showIntro = findViewById(R.id.showIntro);
        showIntro.setPaintFlags(showIntro.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        showIntro.setOnClickListener(v -> mAlertDialog.show());

        TextView skip = findViewById(R.id.skipForNow);
        skip.setPaintFlags(skip.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        skip.setOnClickListener(v -> {
            mEditor.putBoolean("isSetup", true).apply();
            finish();
        });

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

        if (getIntent().getBooleanExtra("fromMenu", false)) {
            mEditor.putBoolean("isSetup", false).apply();//reset the preferences
        }
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra("fromMenu", false)) {
            mEditor.putBoolean("isSetup", true).apply();//undo the preferences changes in onCreate
            finish();
            super.onBackPressed();
            return;
        }
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.help) {
            new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_DayNight)
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
        return super.onOptionsItemSelected(item);
    }
}