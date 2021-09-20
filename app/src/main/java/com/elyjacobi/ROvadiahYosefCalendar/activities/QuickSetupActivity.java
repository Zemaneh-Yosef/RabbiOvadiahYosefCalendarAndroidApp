package com.elyjacobi.ROvadiahYosefCalendar.activities;

import static com.elyjacobi.ROvadiahYosefCalendar.activities.MainActivity.SHARED_PREF;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.elyjacobi.ROvadiahYosefCalendar.R;
import com.elyjacobi.ROvadiahYosefCalendar.classes.ChaiTablesScraper;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import org.jetbrains.annotations.NotNull;

public class QuickSetupActivity extends AppCompatActivity {

    private final String mChaiTablesURL = "http://chaitables.com";
    private SharedPreferences.Editor mEditor;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_setup);
        JewishDate jewishDate = new JewishDate();

        mEditor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
        WebView webView = findViewById(R.id.quickSetupWebView);

        TextView textView = findViewById(R.id.quickSetupExplanation);
        if (getIntent().getBooleanExtra("onlyTable",false)) {
            textView.setText(R.string.alternate_download_request);
        }

        Button downloadButton = findViewById(R.id.quickSetupDownloadButton);
        downloadButton.setOnClickListener(v -> {
            showDialogBox();
            showWebView(webView, textView, downloadButton);
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webView.loadUrl(mChaiTablesURL);
            webView.setWebViewClient(new WebViewClient() {
                @Override public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (view.getUrl().startsWith("http://chaitables.com/cgi-bin/")) {//this is enough to know that it is showing the table with the info we need

                        ChaiTablesScraper scraper = new ChaiTablesScraper();

                        scraper.setDownloadSettings(
                                view.getUrl(),
                                getExternalFilesDir(null),
                                getIntent().getBooleanExtra("onlyTable", false));

                        scraper.start();

                        try {
                            scraper.join();
                            mEditor.putInt("firstYearOfTables",jewishDate.getJewishYear());
                            mEditor.putInt("secondYearOfTables",jewishDate.getJewishYear()+1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        double result = scraper.getResult();

                        Intent returnIntent = new Intent();
                        if (!getIntent().getBooleanExtra("onlyTable",false)) {
                            mEditor.putFloat("elevation", (float) result).apply();
                            returnIntent.putExtra("elevation", result);
                            setResult(Activity.RESULT_OK, returnIntent);
                            Toast.makeText(QuickSetupActivity.this,
                                    "Elevation received from ChaiTables!: " + result,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            setResult(Activity.RESULT_CANCELED, returnIntent);//we don't care about elevation
                        }
                        mEditor.putBoolean("showMishorSunrise", false).apply();
                        mEditor.putBoolean("isSetup", true).apply();
                        finish();
                    }
                }
            });
        });
    }

    private void showDialogBox() {
        new AlertDialog.Builder(this)
                .setTitle("How to get info from chaitables.com")
                .setMessage("(I recommend you to visit the website first.) \n\n" +
                        "Choose your area and on the next page all you need to do is to fill out steps " +
                        "1 and 2, and click the button to calculate the tables on the bottom of the page.\n\n" +
                        "Make sure your search radius is big enough and leave the jewish year alone. " +
                        "The app will do the rest.")
                .setPositiveButton("Ok", (dialogInterface, i) -> { })
                .show();
    }

    private void showWebView(WebView webView, TextView textView, Button downloadButton) {
        textView.setVisibility(View.INVISIBLE);
        downloadButton.setVisibility(View.INVISIBLE);
        webView.setVisibility(View.VISIBLE);
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

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, SetupChooserActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
        finish();
        super.onBackPressed();
    }
}