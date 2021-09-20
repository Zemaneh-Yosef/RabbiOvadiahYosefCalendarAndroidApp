package com.elyjacobi.ROvadiahYosefCalendar.activities;

import static com.elyjacobi.ROvadiahYosefCalendar.activities.MainActivity.SHARED_PREF;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.elyjacobi.ROvadiahYosefCalendar.R;
import com.elyjacobi.ROvadiahYosefCalendar.classes.ChaiTablesScraper;

import org.jetbrains.annotations.NotNull;

public class SetupElevationActivity extends AppCompatActivity {

    private double mElevation = 0;
    private final String mChaiTablesURL = "http://chaitables.com";
    private WebView mWebView;
    private Button mMishorButton;
    private Button mManualButton;
    private Button mChaitablesButton;
    private TextView mSetupHeader;
    private TextView mElevationInfo;
    private TextView mMishorRequest;
    private TextView mManualRequest;
    private TextView mChaitablesRequest;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        mSetupHeader = findViewById(R.id.setup_header);
        mElevationInfo = findViewById(R.id.elevation_info);
        mMishorRequest = findViewById(R.id.mishor_request);
        mManualRequest = findViewById(R.id.manual_request);
        mChaitablesRequest = findViewById(R.id.chaiTables_request);
        if (getIntent().getBooleanExtra("downloadTable",false)) {
            mChaitablesRequest.setText(R.string.chaitables_requestV2);
        }
        mWebView = findViewById(R.id.web_view);

        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();

        mMishorButton = findViewById(R.id.mishor);
        mMishorButton.setOnClickListener(v -> {
            editor.putFloat("elevation", (float)mElevation).apply();
            editor.putBoolean("isElevationSetup", true).apply();
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            if (getIntent().getBooleanExtra("downloadTable",false)) {
                startActivity(new Intent(this, QuickSetupActivity.class)
                        .putExtra("onlyTable",true));
            } else {
                editor.putBoolean("isSetup", true).apply();
            }
            finish();
        });

        mManualButton = findViewById(R.id.manual);
        mManualButton.setOnClickListener(v -> {
            final EditText input = new EditText(this);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter elevation in meters:");
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                if (input.getText().toString().isEmpty() ||
                        !input.getText().toString().matches("[0-9]+.?[0-9]*")) {//regex to check for a proper number input
                    Toast.makeText(
                            SetupElevationActivity.this,
                            "Please Enter a valid value, for example: 30 or 30.0",
                            Toast.LENGTH_SHORT).show();
                } else {
                    mElevation = Double.parseDouble(input.getText().toString());
                    editor.putFloat("elevation", (float) mElevation).apply();
                    editor.putBoolean("isElevationSetup", true).apply();
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("elevation", mElevation);
                    setResult(Activity.RESULT_OK, returnIntent);
                    if (getIntent().getBooleanExtra("downloadTable",false)) {
                        startActivity(new Intent(this, QuickSetupActivity.class)
                                .putExtra("onlyTable",true));
                    } else {
                        editor.putBoolean("isSetup", true).apply();
                    }
                    finish();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.create();
            builder.show();
        });

        mChaitablesButton = findViewById(R.id.chaiTables);
        mChaitablesButton.setOnClickListener(v -> {
            showDialogBox();
            setVisibilityOfViews(View.INVISIBLE);
            mWebView.setVisibility(View.VISIBLE);

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            mWebView.loadUrl(mChaiTablesURL);
            mWebView.setWebViewClient(new WebViewClient() {
                @Override public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (view.getUrl().startsWith("http://chaitables.com/cgi-bin/")) {//this is enough to know that it is showing the table with the info we need

                        ChaiTablesScraper scraper = new ChaiTablesScraper();

                        scraper.setDownloadSettings(
                                view.getUrl(),
                                getExternalFilesDir(null),
                                getIntent().getBooleanExtra("downloadTable", false));

                        scraper.start();
                        try {
                            scraper.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        double result = scraper.getResult();
                        SharedPreferences.Editor editor = getSharedPreferences(
                                SHARED_PREF, MODE_PRIVATE).edit();
                        editor.putFloat("elevation", (float) result).apply();
                        editor.putBoolean("isElevationSetup", true).apply();

                        if (!getIntent().getBooleanExtra("downloadTable",false)) {
                            editor.putBoolean("isSetup", true).apply();
                        }

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("elevation", result);
                        setResult(Activity.RESULT_OK, returnIntent);

                        Toast.makeText(SetupElevationActivity.this,
                                "Elevation received from ChaiTables!: " + result,
                                Toast.LENGTH_LONG).show();

                        finish();
                    }
                }
            });
        });
    }

    /**
     * Self explanatory convenience method to hide all the views except the WebView.
     * @param visibility either values of visibilities mentioned in the {@link View} class.
     * @see View
     */
    private void setVisibilityOfViews(int visibility) {
        mMishorButton.setVisibility(visibility);
        mManualButton.setVisibility(visibility);
        mChaitablesButton.setVisibility(visibility);
        mSetupHeader.setVisibility(visibility);
        mElevationInfo.setVisibility(visibility);
        mMishorRequest.setVisibility(visibility);
        mManualRequest.setVisibility(visibility);
        mChaitablesRequest.setVisibility(visibility);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                } else if (mWebView.getVisibility() == View.INVISIBLE) {
                    startActivity(
                            new Intent(this, AdvancedSetupActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                    finish();
                } else {
                    setVisibilityOfViews(View.VISIBLE);
                    mWebView.setVisibility(View.INVISIBLE);
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_elevation_setup, menu);
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
        }
        return super.onOptionsItemSelected(item);
    }
}