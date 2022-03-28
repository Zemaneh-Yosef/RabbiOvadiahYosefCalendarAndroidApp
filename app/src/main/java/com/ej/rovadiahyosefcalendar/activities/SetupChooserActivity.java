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
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;

import org.jetbrains.annotations.NotNull;

public class SetupChooserActivity extends AppCompatActivity {

    private SharedPreferences.Editor mEditor;
    private boolean mBackupSetting;
    private AlertDialog mAlertDialog;
    private String mElevationBackup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_chooser);
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mEditor = sharedPreferences.edit();

        mAlertDialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
                .setTitle("Introduction")
                .setMessage(R.string.intro)
                .setPositiveButton("Ok", (dialogInterface, i) -> { })
                .setCancelable(false)
                .create();

        TextView showIntro = findViewById(R.id.showIntro);
        showIntro.setPaintFlags(showIntro.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        showIntro.setOnClickListener(v -> mAlertDialog.show());

        TextView skip = findViewById(R.id.skipForNow);
        skip.setPaintFlags(showIntro.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        skip.setOnClickListener(v -> finish());

        Button quickSetupButton = findViewById(R.id.quickSetup);
        Button advancedSetupButton = findViewById(R.id.advancedSetup);

        quickSetupButton.setOnClickListener(v -> {
            startActivity(new Intent(this, QuickSetupActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
            finish();
        });

        advancedSetupButton.setOnClickListener(v -> {
            startActivity(
                    new Intent(this, AdvancedSetupActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                            .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
            finish();
        });

        if (!sharedPreferences.getBoolean("introShown",false)) {//if the introduction has not been shown
            mAlertDialog.show();
            mEditor.putBoolean("introShown", true).apply();
        }
        if (getIntent().getBooleanExtra("fromMenu", false)) {
            mEditor.putBoolean("isSetup", false).apply();//reset the preferences
            mElevationBackup = sharedPreferences.getString("elevation", "0");
            mEditor.putString("elevation", "0").apply();
            mBackupSetting = sharedPreferences.getBoolean("askagain", true);//save what the user set
            mEditor.putBoolean("askagain", true).apply();//if the user is re-running the setup, reset the preferences for asking whether to change the city
        }
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra("fromMenu", false)) {
            mEditor.putBoolean("isSetup", true).apply();//undo the preferences changes in onCreate
            mEditor.putBoolean("askagain", mBackupSetting).apply();
            mEditor.putString("elevation", mElevationBackup).apply();
            finish();
            super.onBackPressed();
            return;
        }
        startActivity(new Intent(this, ZmanimLanguageActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
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
            new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight)
                    .setTitle("Help using this app:")
                    .setPositiveButton("ok", null)
                    .setMessage(R.string.helper_text)
                    .show();
            return true;
        } else if (id == R.id.restart) {
            startActivity(new Intent(this, FullSetupActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}