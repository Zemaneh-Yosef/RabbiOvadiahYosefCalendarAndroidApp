package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentLocationName;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.LocaleChecker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AdvancedSetupActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_advanced_setup);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        if (LocaleChecker.isLocaleHebrew()) {
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
            } else if (id == R.id.restart) {
                startActivity(new Intent(this, FullSetupActivity.class));
                finish();
                return true;
            }
            return false;
        });
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();

        EditText tableLink = findViewById(R.id.tableLink);
        Button setLink = findViewById(R.id.set_link_button);
        Button website = findViewById(R.id.chaitables_button);
        Button skip = findViewById(R.id.skip);

        setLink.setOnClickListener(v -> {
            if (tableLink.getText().toString().isEmpty()) {
                new MaterialAlertDialogBuilder(AdvancedSetupActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(R.string.please_enter_a_link)
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                String link = tableLink.getText().toString();
                editor.putString("chaitablesLink" + sCurrentLocationName, link);
                editor.putBoolean("UseTable" + sCurrentLocationName, true).apply();
                editor.putBoolean("showMishorSunrise" + sCurrentLocationName, false).apply();
                startActivity(new Intent(this, SetupElevationActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .putExtra("downloadTable", true)
                        .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
                finish();
            }
        });

        website.setOnClickListener(v -> {//TODO hebrew this
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(AdvancedSetupActivity.this)
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

        skip.setTypeface(Typeface.DEFAULT_BOLD);
        skip.setOnClickListener(v -> {
            editor.putBoolean("UseTable" + sCurrentLocationName, false).apply();
            editor.putBoolean("showMishorSunrise" + sCurrentLocationName, true).apply();
            startActivity(new Intent(this, SetupElevationActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtra("downloadTable", false)
                    .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(skip, (v, windowInsets) -> {
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
                startActivity(new Intent(AdvancedSetupActivity.this, SetupChooserActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .putExtra("fromMenu", getIntent().getBooleanExtra("fromMenu", false)));
                finish();
            }
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
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.how_to_get_info_from_chaitables_com)
                .setMessage(R.string.i_recommend_that_you_visit_the_website_first_choose_your_area)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }
}