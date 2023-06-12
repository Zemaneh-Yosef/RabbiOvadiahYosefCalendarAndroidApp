package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sCurrentLocationName;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;

import org.jetbrains.annotations.NotNull;

public class AdvancedSetupActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_setup);
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();

        EditText tableLink = findViewById(R.id.tableLink);
        Button setLink = findViewById(R.id.set_link_button);
        Button website = findViewById(R.id.chaitables_button);
        TextView skip = findViewById(R.id.skip);

        setLink.setOnClickListener(v -> {
            if (tableLink.getText().toString().isEmpty()) {
                new AlertDialog.Builder(AdvancedSetupActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(R.string.please_enter_a_link)
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                String link = tableLink.getText().toString();
                editor.putString("chaitablesLink" + sCurrentLocationName, link);
                editor.putBoolean("UseTable" + sCurrentLocationName, true).apply();
                startActivity(new Intent(this, SetupElevationActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .putExtra("downloadTable", true)
                        .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
                finish();
            }
        });

        website.setOnClickListener(v -> {//TODO hebrew this
            AlertDialog alertDialog = new AlertDialog.Builder(AdvancedSetupActivity.this)
                    .setTitle("Chaitables.com")
                    .setPositiveButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create();
            WebView webView = new WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("http://www.chaitables.com/");
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    if (url.startsWith("http://www.chaitables.com/cgi-bin/")) {
                        String visibleURL = getVisibleURL(url);
                        editor.putString("chaitablesLink" + sCurrentLocationName, visibleURL);
                        editor.putBoolean("UseTable" + sCurrentLocationName, true).apply();
                        editor.putBoolean("showMishorSunrise" + sCurrentLocationName, false).apply();
                        editor.putBoolean("isSetup", true).apply();
                        alertDialog.dismiss();
                        startActivity(new Intent(getApplicationContext(), SetupElevationActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                                .putExtra("downloadTable", true)
                                .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
                        finish();
                        }
                    }
                });
            alertDialog.setView(webView);
            alertDialog.show();
            showDialogBox();
        });

        skip.setPaintFlags(skip.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        skip.setOnClickListener(v -> {
            editor.putBoolean("UseTable" + sCurrentLocationName, false).apply();
            editor.putBoolean("showMishorSunrise" + sCurrentLocationName, true).apply();
            startActivity(new Intent(this, SetupElevationActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtra("downloadTable", false)
                    .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
            finish();
        });
    }

    private String getVisibleURL(String url) {
            if (url.contains("&cgi_types=5")) {
                return url.replace("&cgi_types=5", "&cgi_types=0");
            } else if (url.contains("&cgi_types=1")) {
                return url.replace("&cgi_types=1", "&cgi_types=0");
            } else if (url.contains("&cgi_types=2")) {
                return url.replace("&cgi_types=2", "&cgi_types=0");
            } else if (url.contains("&cgi_types=3")) {
                return url.replace("&cgi_types=3", "&cgi_types=0");
            } else if (url.contains("&cgi_types=4")) {
                return url.replace("&cgi_types=4", "&cgi_types=0");
            } else if (url.contains("&cgi_types=-1")) {
                return url.replace("&cgi_types=-1", "&cgi_types=0");
            }
        return url;
    }

    private void showDialogBox() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.how_to_get_info_from_chaitables_com)
                .setMessage(R.string.i_recommend_that_you_visit_the_website_first_choose_your_area)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, SetupChooserActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
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
                    .setTitle(R.string.help_using_this_app)
                    .setPositiveButton(R.string.ok, null)
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