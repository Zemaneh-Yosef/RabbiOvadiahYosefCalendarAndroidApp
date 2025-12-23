package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.Utils;
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
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (Utils.isLocaleHebrew()) {
            materialToolbar.setSubtitle("");
        }
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
                editor.putString("chaitablesLink" + sCurrentLocationName, link)
                        .putBoolean("UseTable" + sCurrentLocationName, true)
                        .putBoolean("showMishorSunrise" + sCurrentLocationName, false)
                        .apply();
                startActivity(new Intent(this, SetupElevationActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .putExtra("downloadTable", true)
                        .putExtra("fromSetup", AdvancedSetupActivity.this.getIntent().getBooleanExtra("fromSetup", false))
                        .putExtra("loneActivity", getIntent().getBooleanExtra("loneActivity", false)));
                finish();
            }
        });

        website.setOnClickListener(v -> {
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(AdvancedSetupActivity.this)
                .setTitle("chaitables.com")
                .setPositiveButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
            WebView webView = new WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl(Utils.isLocaleHebrew() ? "https://chaitables.com/chai_heb.php" : "http://www.chaitables.com/");
            webView.setWebViewClient(new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    String url = request.getUrl().toString();
                    String allowedDomain = "chaitables.com";// block ANY external site
                    return !url.contains(allowedDomain) && !url.contains("162.253.153.219/");// this is some times the domain
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    if (url.startsWith("http://www.chaitables.com/cgi-bin/")) {
                        String visibleURL = getVisibleURL(url);
                        editor.putString("chaitablesLink" + sCurrentLocationName, visibleURL)
                                .putBoolean("UseTable" + sCurrentLocationName, true)
                                .putBoolean("showMishorSunrise" + sCurrentLocationName, false)
                                .apply();
                        alertDialog.dismiss();
                        startActivity(new Intent(getApplicationContext(), SetupElevationActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                            .putExtra("downloadTable", true)
                            .putExtra("fromSetup", AdvancedSetupActivity.this.getIntent().getBooleanExtra("fromSetup", false))
                            .putExtra("loneActivity", getIntent().getBooleanExtra("loneActivity", false)));
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
            editor.putBoolean("UseTable" + sCurrentLocationName, false)
                    .putBoolean("showMishorSunrise" + sCurrentLocationName, true)
                    .apply();
            startActivity(new Intent(this, SetupElevationActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtra("downloadTable", false)
                    .putExtra("fromSetup", AdvancedSetupActivity.this.getIntent().getBooleanExtra("fromSetup", false))
                    .putExtra("loneActivity", getIntent().getBooleanExtra("loneActivity", false)));
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
                        .putExtra("fromSetup", AdvancedSetupActivity.this.getIntent().getBooleanExtra("fromSetup", false)));
                finish();
            }
        });
    }

    /**
     * This method ensures that the right page is being scraped. The parameter &cgi_types= refers to the type of sunrise/sunset table to calculate.
     * This method will change it to 0 for visible sunrise. It will also make sure that the table is in English for data processing, but the user
     * can use the hebrew site and be non the wiser.
     * @param url the url for the sunrise/sunset table
     * @return a url for visible sunrise in English
     */
    private String getVisibleURL(String url) {
        return url
                .replace("&cgi_types=-1", "&cgi_types=0")
                .replace("&cgi_types=1", "&cgi_types=0")
                .replace("&cgi_types=2", "&cgi_types=0")
                .replace("&cgi_types=3", "&cgi_types=0")
                .replace("&cgi_types=4", "&cgi_types=0")
                .replace("&cgi_types=5", "&cgi_types=0")
                .replace("&cgi_optionheb=0", "&cgi_optionheb=1")
                .replace("&cgi_Language=Hebrew", "&cgi_Language=English");
    }

    private void showDialogBox() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.how_to_get_info_from_chaitables_com)
                .setMessage(R.string.i_recommend_that_you_visit_the_website_first_choose_your_area)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }
}