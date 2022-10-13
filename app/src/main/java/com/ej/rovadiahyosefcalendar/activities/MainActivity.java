package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.classes.JewishDateInfo.formatHebrewNumber;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.ChaiTables;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesScraper;
import com.ej.rovadiahyosefcalendar.classes.CustomDatePickerDialog;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanAdapter;
import com.ej.rovadiahyosefcalendar.notifications.DailyNotifications;
import com.ej.rovadiahyosefcalendar.notifications.OmerNotifications;
import com.ej.rovadiahyosefcalendar.notifications.ZmanimNotifications;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;
import com.kosherjava.zmanim.util.GeoLocation;
import com.kosherjava.zmanim.util.ZmanimFormatter;

import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static boolean sShabbatMode;
    public static boolean sNetworkLocationServiceIsDisabled;
    public static boolean sGPSLocationServiceIsDisabled;
    public static boolean sUserIsOffline;
    private boolean mBackHasBeenPressed = false;
    private boolean updateTablesDialogShown;
    private boolean mInitialized = false;
    private int mCurrentPosition;//current position in the RecyclerView list of zmanim to return to when the user returns to the main screen
    private double mElevation = 0;
    public static double sLatitude;
    public static double sLongitude;
    private static final int TWENTY_FOUR_HOURS_IN_MILLI = 86_400_000;

    /**
     * This string is used to display the name of the current location in the app. We also use this string to save the elevation of a location to the
     * SharedPreferences, and we save the chai tables files under this name as well.
     */
    public static String sCurrentLocationName = "";
    public static String sCurrentTimeZoneID;//e.g. "America/New_York"

    //android views:
    private View mLayout;
    private Button mNextDate;
    private Button mPreviousDate;
    private Button mCalendarButton;
    private TextView mShabbatModeBanner;
    private RecyclerView mMainRecyclerView;

    //custom classes/kosherjava classes:
    private LocationResolver mLocationResolver;
    private ROZmanimCalendar mROZmanimCalendar;
    private JewishDateInfo mJewishDateInfo;
    private final ZmanimFormatter mZmanimFormatter = new ZmanimFormatter(TimeZone.getDefault());

    //android classes:
    private Handler mHandler = null;
    private Runnable mZmanimUpdater;
    private AlertDialog mAlertDialog;
    private GestureDetector mGestureDetector;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsPreferences;
    public static final String SHARED_PREF = "MyPrefsFile";
    private ActivityResultLauncher<Intent> mSetupLauncher;

    /**
     * The current date shown in the main activity. This calendar can be manipulated to change the date if needed.
     */
    private Calendar mCurrentDateShown = Calendar.getInstance();

    /**
     * The current date of the device. This calendar is not to be modified directly, it's only use is to reset the date of other objects
     */
    private final Calendar mCurrentDate = Calendar.getInstance();

    /**
     * These calendars are used to know when daf/yerushalmi yomi started
     */
    private final static Calendar dafYomiStartDate = new GregorianCalendar(1923, Calendar.SEPTEMBER, 11);
    private final static Calendar dafYomiYerushalmiStartDate = new GregorianCalendar(1980, Calendar.FEBRUARY, 2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {//TODO add weekly view option,
        setTheme(R.style.AppTheme); //splash screen
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.main_layout);
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mGestureDetector = new GestureDetector(MainActivity.this, new ZmanimGestureListener());
        mZmanimFormatter.setTimeFormat(ZmanimFormatter.SEXAGESIMAL_FORMAT);
        initAlertDialog();
        initSetupResult();
        setupShabbatModeBanner();
        mLocationResolver = new LocationResolver(this, this);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED ||
                mSharedPreferences.getBoolean("useZipcode", false)) {
            mLocationResolver.acquireLatitudeAndLongitude();
        }
        mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
        if (!ChaiTables.visibleSunriseFileExists(getExternalFilesDir(null), sCurrentLocationName, mJewishDateInfo.getJewishCalendar())
                && mSharedPreferences.getBoolean("UseTable" + sCurrentLocationName, true)
                && !mSharedPreferences.getBoolean("isSetup", false)
                && savedInstanceState == null) {//it should only not exist the first time running the app and only if the user has not set up the app
            mSetupLauncher.launch(new Intent(this, FullSetupActivity.class));
            initZmanimNotificationDefaults();
        } else {
            mLocationResolver.acquireLatitudeAndLongitude();
        }
        if (sGPSLocationServiceIsDisabled && sNetworkLocationServiceIsDisabled) {
            Toast.makeText(MainActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT).show();
        } else {
            if ((!mInitialized && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED)
                    || mSharedPreferences.getBoolean("useZipcode", false)) {
                initMainView();
            }
        }
    }

    private void initZmanimNotificationDefaults() {
        mSettingsPreferences.edit().putBoolean("zmanim_notifications", true).apply();
        mSettingsPreferences.edit().putInt("Alot", -1).apply();
        mSettingsPreferences.edit().putInt("TalitTefilin", 15).apply();
        mSettingsPreferences.edit().putInt("HaNetz", -1).apply();
        mSettingsPreferences.edit().putInt("SofZmanShmaMGA", 15).apply();
        mSettingsPreferences.edit().putInt("SofZmanShmaGRA", -1).apply();
        mSettingsPreferences.edit().putInt("SofZmanTefila", 15).apply();
        mSettingsPreferences.edit().putInt("SofZmanAchilatChametz", 15).apply();
        mSettingsPreferences.edit().putInt("SofZmanBiurChametz", 15).apply();
        mSettingsPreferences.edit().putInt("Chatzot", -1).apply();
        mSettingsPreferences.edit().putInt("MinchaGedola", -1).apply();
        mSettingsPreferences.edit().putInt("MinchaKetana", -1).apply();
        mSettingsPreferences.edit().putInt("PlagHaMincha", 15).apply();
        mSettingsPreferences.edit().putInt("CandleLighting", 15).apply();
        mSettingsPreferences.edit().putInt("Shkia", 15).apply();
        mSettingsPreferences.edit().putInt("TzeitHacochavim", 15).apply();
        mSettingsPreferences.edit().putInt("FastEnd", 15).apply();
        mSettingsPreferences.edit().putInt("FastEndStringent", 15).apply();
        mSettingsPreferences.edit().putInt("ShabbatEnd", -1).apply();
        mSettingsPreferences.edit().putInt("RT", -1).apply();
        mSettingsPreferences.edit().putInt("NightChatzot", -1).apply();
    }

    /**
     * This method registers the setupLauncher to receive the data that the user entered in the
     * SetupActivity. When the user finishes setting up the app, the setupLauncher will receive the
     * data and set the SharedPreferences to indicate that the user has set up the app.
     * It will also reinitialize the main view with the updated settings.
     */
    private void initSetupResult() {
        mSetupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (result.getData() != null) {
                            editor.putString("lastLocation", sCurrentLocationName).apply();
                        }
                    }
                    if (!mInitialized) {
                        initMainView();
                    }
                    instantiateZmanimCalendar();
                    mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                }
        );
    }

    /**
     * This method initializes the main view. This method should only be called when we are able to initialize the @{link mROZmanimCalendar} object
     * with the correct latitude, longitude, elevation, and timezone.
     */
    private void initMainView() {
        mInitialized = true;
        mLocationResolver.setTimeZoneID();
        getAndConfirmLastElevationAndVisibleSunriseData();
        instantiateZmanimCalendar();
        saveGeoLocationInfo();
        setupRecyclerView();
        setupButtons();
        setNotifications();
        checkIfUserIsInIsraelOrNot();
        askForBackgroundLocationPermission();
    }

    private void askForBackgroundLocationPermission() {
        if (mSharedPreferences.getBoolean("useZipcode", false)) {
            return;//if the user is using a zipcode, we don't need to ask for background location permission as we don't use the device's location
        }
        if (!mSharedPreferences.getBoolean("askedForRealtimeNotifications", false)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Would you like to receive real-time notifications for zmanim?");
            builder.setMessage("If you would like to receive real-time notifications for zmanim, " +
                    "please navigate to the settings page and enable location services all the time for this app. " +
                    "Would you like to do this now?");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes", (dialog, which) -> {
                if (ActivityCompat.checkSelfPermission(this, ACCESS_BACKGROUND_LOCATION) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{ACCESS_BACKGROUND_LOCATION}, 1);
                }
                mSharedPreferences.edit().putBoolean("askedForRealtimeNotifications", true).apply();
            });
            builder.setNegativeButton("No", (dialog, which) -> {
                mSharedPreferences.edit().putBoolean("askedForRealtimeNotifications", true).apply();
                dialog.dismiss();
            });
            builder.show();
        }
    }

    private void checkIfUserIsInIsraelOrNot() {
        if (mSharedPreferences.getBoolean("neverAskInIsraelOrNot", false)) {return;}

        if (sCurrentTimeZoneID.equals("Asia/Jerusalem")) {//user is in or near israel now
            mSharedPreferences.edit().putBoolean("askedNotInIsrael", false).apply();//reset that we asked outside israel for next time
            if (!mSharedPreferences.getBoolean("inIsrael", false) && //user was not in israel before
                    !mSharedPreferences.getBoolean("askedInIsrael", false)) {//and we did not ask already
                new AlertDialog.Builder(this)
                        .setTitle("Are you in Israel now?")
                        .setMessage("If you are in Israel now, please confirm below. Otherwise, ignore this message. (This setting only affects the holidays).")
                        .setPositiveButton("Yes, I am in Israel", (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("inIsrael", true).apply();
                            mJewishDateInfo = new JewishDateInfo(true, true);
                            Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show();
                            mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                        })
                        .setNegativeButton("No, I am not in Israel", (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("askedInIsrael", true).apply();//save that we asked already
                            dialog.dismiss();
                        })
                        .setNeutralButton("Do not ask me again", (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("neverAskInIsraelOrNot", true).apply();//save that we should never ask again
                            dialog.dismiss();
                        })
                        .show();
            }
        } else {//user is not in israel
            mSharedPreferences.edit().putBoolean("askedInIsrael", false).apply();//reset that we asked in israel
            if (mSharedPreferences.getBoolean("inIsrael", false) && //user was in israel before
                    !mSharedPreferences.getBoolean("askedInNotIsrael", false)) {//and we did not ask already
                new AlertDialog.Builder(this)
                        .setTitle("Have you left Israel?")
                        .setMessage("If you are not in Israel now, please confirm below. Otherwise, ignore this message. (This setting only affects the holidays).")
                        .setPositiveButton("Yes, I have left Israel", (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("inIsrael", false).apply();
                            mJewishDateInfo = new JewishDateInfo(false, true);
                            Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show();
                            mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                        })
                        .setNegativeButton("No, I have not left Israel", (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("askedInNotIsrael", true).apply();//save that we asked
                            dialog.dismiss();
                        })
                        .setNeutralButton("Do not ask me again", (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("neverAskInIsraelOrNot", true).apply();//save that we should never ask again
                            dialog.dismiss();
                        })
                        .show();
            }
        }
    }

    /**
     * This method will automatically update the tables if the user has setup the app before for the current location.
     * @param fromButton if the method is called from the buttons, it will not ask more than once if the user wants to update the tables.
     */
    private void seeIfTablesNeedToBeUpdated(boolean fromButton) {
        if (mSharedPreferences.getBoolean("isSetup", false) //only check after the app has been setup before
                && mSharedPreferences.getBoolean("UseTable" + sCurrentLocationName, false)) { //and only if the tables are being used

            if (!ChaiTables.visibleSunriseFileExists(getExternalFilesDir(null), sCurrentLocationName, mJewishDateInfo.getJewishCalendar())) {
                if (!updateTablesDialogShown) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Update Tables?");
                    builder.setMessage("The visible sunrise tables for the current location and year need to be updated.\n\n" +
                            "Do you want to update the tables now?");
                    builder.setPositiveButton("Yes", (dialog, which) -> {
                        String chaitablesURL = mSharedPreferences.getString("chaitablesLink" + sCurrentLocationName, "");
                        if (!chaitablesURL.isEmpty()) {//it should not be empty if the user has set up the app, but it is good to check
                            String hebrewYear = String.valueOf(mJewishDateInfo.getJewishCalendar().getJewishYear());
                            Pattern pattern = Pattern.compile("&cgi_yrheb=\\d{4}");
                            Matcher matcher = pattern.matcher(chaitablesURL);
                            if (matcher.find()) {
                                chaitablesURL = chaitablesURL.replace(matcher.group(), "&cgi_yrheb=" + hebrewYear);//replace the year in the URL with the current year
                            }
                            ChaiTablesScraper scraper = new ChaiTablesScraper();
                            scraper.setDownloadSettings(chaitablesURL, getExternalFilesDir(null), mJewishDateInfo.getJewishCalendar());
                            scraper.start();
                            try {
                                scraper.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                        }
                    });
                    builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                    builder.show();
                    if (fromButton) {
                        updateTablesDialogShown = true;
                    }
                }
            }
        }
    }

    private void instantiateZmanimCalendar() {
        mROZmanimCalendar = new ROZmanimCalendar(new GeoLocation(
                sCurrentLocationName,
                sLatitude,
                sLongitude,
                mElevation,
                TimeZone.getTimeZone(sCurrentTimeZoneID)));
        mROZmanimCalendar.setExternalFilesDir(getExternalFilesDir(null));
        mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(mSettingsPreferences.getString("CandleLightingOffset", "20")));
        mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(mSettingsPreferences.getString("EndOfShabbatOffset", "40")));
    }

    private void setupRecyclerView() {
        mMainRecyclerView = findViewById(R.id.mainRV);
        mMainRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMainRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
        mMainRecyclerView.setOnTouchListener((view, motionEvent) -> mGestureDetector.onTouchEvent(motionEvent));
        findViewById(R.id.main_layout).setOnTouchListener((view, motionEvent) -> mGestureDetector.onTouchEvent(motionEvent));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return mGestureDetector.onTouchEvent(ev);
    }

    /**
     * This method sets up the functionality of hiding the banner when the user taps on it.
     */
    private void setupShabbatModeBanner() {
        mShabbatModeBanner = findViewById(R.id.shabbat_mode);
        mShabbatModeBanner.setSelected(true);
        mShabbatModeBanner.setOnClickListener(v -> {
            if (v.getVisibility() == View.VISIBLE) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupButtons() {
        setupPreviousDayButton();
        setupCalendarButton();
        setupNextDayButton();
    }

    /**
     * Sets up the previous day button
     */
    private void setupPreviousDayButton() {
        mPreviousDate = findViewById(R.id.prev_day);
        mPreviousDate.setOnClickListener(v -> {
            if (!sShabbatMode) {
                mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
                mCurrentDateShown.add(Calendar.DATE, -1);
                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
                seeIfTablesNeedToBeUpdated(true);
            }
        });
    }

    /**
     * Sets up the next day button
     */
    private void setupNextDayButton() {
        mNextDate = findViewById(R.id.next_day);
        mNextDate.setOnClickListener(v -> {
            if (!sShabbatMode) {
                mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
                mCurrentDateShown.add(Calendar.DATE, 1);
                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
                seeIfTablesNeedToBeUpdated(true);
            }
        });
    }

    /**
     * Setup the calendar button.
     */
    private void setupCalendarButton() {
        mCalendarButton = findViewById(R.id.calendar);
        DatePickerDialog dialog = createDialog();

        mCalendarButton.setOnClickListener(v -> {
            if (!sShabbatMode) {
                dialog.updateDate(mROZmanimCalendar.getCalendar().get(Calendar.YEAR),
                        mROZmanimCalendar.getCalendar().get(Calendar.MONTH),
                        mROZmanimCalendar.getCalendar().get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
    }

    /**
     * Sets up the dialog for the calendar button. Added a custom date picker to the dialog as well.
     * @return The dialog.
     * @see CustomDatePickerDialog for more information.
     */
    private DatePickerDialog createDialog() {
        DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
            Calendar mUserChosenDate = Calendar.getInstance();
            mUserChosenDate.set(year, month, day);
            mROZmanimCalendar.setCalendar(mUserChosenDate);
            mJewishDateInfo.setCalendar(mUserChosenDate);
            mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
            mMainRecyclerView.setAdapter(new ZmanAdapter(MainActivity.this, MainActivity.this.getZmanimList()));
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, MainActivity.this.getCurrentCalendarDrawable());
            seeIfTablesNeedToBeUpdated(true);
        };

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return new CustomDatePickerDialog(this, onDateSetListener,
                    mROZmanimCalendar.getCalendar().get(Calendar.YEAR),
                    mROZmanimCalendar.getCalendar().get(Calendar.MONTH),
                    mROZmanimCalendar.getCalendar().get(Calendar.DAY_OF_MONTH),
                    mJewishDateInfo.getJewishCalendar());
        } else {
            return new DatePickerDialog(this, onDateSetListener,
                    mROZmanimCalendar.getCalendar().get(Calendar.YEAR),
                    mROZmanimCalendar.getCalendar().get(Calendar.MONTH),
                    mROZmanimCalendar.getCalendar().get(Calendar.DAY_OF_MONTH));
        }
    }

    /**
     * returns the current calendar drawable depending on the current day of the month.
     */
    private int getCurrentCalendarDrawable() {
        switch (mROZmanimCalendar.getCalendar().get(Calendar.DATE)) {
            case (1):
                return R.drawable.calendar1;
            case (2):
                return R.drawable.calendar2;
            case (3):
                return R.drawable.calendar3;
            case (4):
                return R.drawable.calendar4;
            case (5):
                return R.drawable.calendar5;
            case (6):
                return R.drawable.calendar6;
            case (7):
                return R.drawable.calendar7;
            case (8):
                return R.drawable.calendar8;
            case (9):
                return R.drawable.calendar9;
            case (10):
                return R.drawable.calendar10;
            case (11):
                return R.drawable.calendar11;
            case (12):
                return R.drawable.calendar12;
            case (13):
                return R.drawable.calendar13;
            case (14):
                return R.drawable.calendar14;
            case (15):
                return R.drawable.calendar15;
            case (16):
                return R.drawable.calendar16;
            case (17):
                return R.drawable.calendar17;
            case (18):
                return R.drawable.calendar18;
            case (19):
                return R.drawable.calendar19;
            case (20):
                return R.drawable.calendar20;
            case (21):
                return R.drawable.calendar21;
            case (22):
                return R.drawable.calendar22;
            case (23):
                return R.drawable.calendar23;
            case (24):
                return R.drawable.calendar24;
            case (25):
                return R.drawable.calendar25;
            case (26):
                return R.drawable.calendar26;
            case (27):
                return R.drawable.calendar27;
            case (28):
                return R.drawable.calendar28;
            case (29):
                return R.drawable.calendar29;
            case (30):
                return R.drawable.calendar30;
            default:
                return R.drawable.calendar31;
        }
    }

    @Override
    protected void onRestart() {
        if (sShabbatMode) {
            startShabbatMode();//left in just in case app is restarted while in shabbat mode.
        }
        super.onRestart();
    }

    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (sShabbatMode) {
            startActivity(getIntent());
        }
        if (mMainRecyclerView != null) {
            mCurrentPosition = ((LinearLayoutManager)mMainRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mROZmanimCalendar == null) {
            super.onResume();
            return;
        }
        if (mCurrentDateShown != null) {
            instantiateZmanimCalendar();
            mROZmanimCalendar.setCalendar(mCurrentDateShown);
            mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
            mJewishDateInfo.setCalendar(mCurrentDateShown);
        }
        mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
        mMainRecyclerView.scrollToPosition(mCurrentPosition);
        getAndConfirmLastElevationAndVisibleSunriseData();
        resetTheme();
        if (mSharedPreferences.getBoolean("useImage", false)) {
            Bitmap bitmap = BitmapFactory.decodeFile(mSharedPreferences.getString("imageLocation", ""));
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            mLayout.setBackground(drawable);
        } else {
            mLayout.setBackgroundResource(R.color.black);
        }
        if (mSharedPreferences.getBoolean("useDefaultCalButtonColor", true)) {
            mCalendarButton.setBackgroundColor(getColor(R.color.dark_blue));
        } else {
            mCalendarButton.setBackgroundColor(mSharedPreferences.getInt("CalButtonColor", 0x18267C));
        }
        Intent zmanIntent = new Intent(getApplicationContext(), ZmanimNotifications.class);//this is to update the zmanim notifications if the user changed the settings to start showing them
        mSharedPreferences.edit().putBoolean("fromThisNotification", false).apply();
        PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),0,zmanIntent,PendingIntent.FLAG_IMMUTABLE);
        try {
            zmanimPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    /**
     * sets the theme of the app according to the user's preferences.
     */
    private void resetTheme() {
        String theme = mSettingsPreferences.getString("theme", "Auto (Follow System Theme)");
        switch (theme) {
            case "Auto (Follow System Theme)":
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Day":
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Night":
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    /**
     * This method saves the information needed to restore a GeoLocation object in the notification classes.
     */
    private void saveGeoLocationInfo() {//needed for notifications
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
        editor.putString("name", sCurrentLocationName).apply();
        editor.putLong("lat", Double.doubleToRawLongBits(sLatitude)).apply();//see here: https://stackoverflow.com/a/18098090/13593159
        editor.putLong("long", Double.doubleToRawLongBits(sLongitude)).apply();
        editor.putString("timezoneID", sCurrentTimeZoneID).apply();
    }

    /**
     * This method will be called every time the user opens the app. It will reset the notifications every time the app is opened since the user might
     * have changed his location.
     */
    private void setNotifications() {
        Calendar calendar = (Calendar) mROZmanimCalendar.getCalendar().clone();
        calendar.setTimeInMillis(mROZmanimCalendar.getSunrise().getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                new Intent(getApplicationContext(), DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(dailyPendingIntent);//cancel any previous alarms
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dailyPendingIntent);

        calendar.setTimeInMillis(mROZmanimCalendar.getTzeit().getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                new Intent(getApplicationContext(), OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        am.cancel(omerPendingIntent);//cancel any previous alarms
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), omerPendingIntent);

        //zmanim notifications are set in the onResume method, doing it twice will cause the preferences to be reset to true mid way through
//        Intent zmanIntent = new Intent(getApplicationContext(), ZmanimNotifications.class);
//        mSharedPreferences.edit().putBoolean("fromThisNotification", false).apply();
//        PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),0,zmanIntent,PendingIntent.FLAG_IMMUTABLE);
//        try {
//            zmanimPendingIntent.send();
//        } catch (PendingIntent.CanceledException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Override this method to make sure nothing is blocking the app over shabbat/yom tov
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus)
            if (sShabbatMode) {
                startActivity(getIntent());
            }
    }

    /**
     * This method is called when the user clicks on shabbat mode. The main point of this method is to automatically scroll through the list of zmanim
     * and update the date when the time reaches the next date at 12:00:02am. It will also update the shabbat banner to reflect the next day's date.
     * (The reason why I chose 12:00:02am is to avoid a hiccup if the device is too fast to update the time, although it is probably not a big deal.)
     * @see #startScrollingThread() to start the thread that will scroll through the list of zmanim
     * @see #setShabbatBannersText(boolean) to set the text of the shabbat banners
     */
    private void startShabbatMode() {
        if (!sShabbatMode) {
            sShabbatMode = true;
            setShabbatBannersText(true);
            mShabbatModeBanner.setVisibility(View.VISIBLE);
            int orientation;
            int rotation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_0:
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
            setRequestedOrientation(orientation);

            Calendar calendar = Calendar.getInstance();
            Calendar calendar2 = (Calendar) calendar.clone();
            mHandler = new Handler(getMainLooper());
            mZmanimUpdater = () -> {
                calendar.setTimeInMillis(new Date().getTime());
                mCurrentDate.setTimeInMillis(calendar.getTime().getTime());
                mCurrentDateShown.setTimeInMillis(calendar.getTime().getTime());
                mROZmanimCalendar.setCalendar(calendar);
                mJewishDateInfo.setCalendar(calendar);
                setShabbatBannersText(false);
                mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                mHandler.removeCallbacks(mZmanimUpdater);
                mHandler.postDelayed(mZmanimUpdater, TWENTY_FOUR_HOURS_IN_MILLI);//run the update in 24 hours
            };
            calendar.set(Calendar.HOUR_OF_DAY,0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 2);
            calendar.add(Calendar.DATE,1);
            mHandler.postDelayed(mZmanimUpdater,calendar.getTimeInMillis() - calendar2.getTimeInMillis());//time remaining until 12:00:02am the next day
            startScrollingThread();
        }
    }

    /**
     * Sets the text of the shabbat banners based on the NEXT day's date, since most people will start shabbat mode before shabbat/chag starts.
     * @param isFirstTime if true, the text will be set based on the next day's date, otherwise it will be set based on the current date.
     *                    Since it will be called at 12:00:02am the next day, we do not need to worry about the next day's date.
     */
    @SuppressLint("SetTextI18n")
    private void setShabbatBannersText(boolean isFirstTime) {
        if (isFirstTime) {
            mCurrentDate.add(Calendar.DATE,1);
            mJewishDateInfo.setCalendar(mCurrentDate);
        }

        boolean isShabbat = mCurrentDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;

        StringBuilder sb = new StringBuilder();

        switch (mJewishDateInfo.getJewishCalendar().getYomTovIndex()) {
            case JewishCalendar.PESACH:
                for (int i = 0; i < 4; i++) {
                    sb.append("PESACH");
                    if (isShabbat) {
                        sb.append("/SHABBAT");
                    }
                    sb.append(" MODE                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.lightYellow));
                mShabbatModeBanner.setTextColor(getColor(R.color.black));
                mCalendarButton.setBackgroundColor(getColor(R.color.lightYellow));
                break;
            case JewishCalendar.SHAVUOS:
                for (int i = 0; i < 4; i++) {
                    sb.append("SHAVUOT");
                    if (isShabbat) {
                        sb.append("/SHABBAT");
                    }
                    sb.append(" MODE                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.light_blue));
                mShabbatModeBanner.setTextColor(getColor(R.color.white));
                mCalendarButton.setBackgroundColor(getColor(R.color.light_blue));
                break;
            case JewishCalendar.SUCCOS:
            case JewishCalendar.SHEMINI_ATZERES:
            case JewishCalendar.SIMCHAS_TORAH:
                for (int i = 0; i < 4; i++) {
                    sb.append("SUCCOT");
                    if (isShabbat) {
                        sb.append("/SHABBAT");
                    }
                    sb.append(" MODE                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.green));
                mShabbatModeBanner.setTextColor(getColor(R.color.black));
                mCalendarButton.setBackgroundColor(getColor(R.color.green));
                break;
            case JewishCalendar.ROSH_HASHANA:
                for (int i = 0; i < 4; i++) {
                    sb.append("ROSH HASHANA");
                    if (isShabbat) {
                        sb.append("/SHABBAT");
                    }
                    sb.append(" MODE                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.dark_red));
                mShabbatModeBanner.setTextColor(getColor(R.color.white));
                mCalendarButton.setBackgroundColor(getColor(R.color.dark_red));
                break;
            case JewishCalendar.YOM_KIPPUR:
                for (int i = 0; i < 4; i++) {
                    sb.append("YOM KIPPUR");
                    if (isShabbat) {
                        sb.append("/SHABBAT");
                    }
                    sb.append(" MODE                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.white));
                mShabbatModeBanner.setTextColor(getColor(R.color.black));
                mCalendarButton.setBackgroundColor(getColor(R.color.white));
                break;
            default:
                mShabbatModeBanner.setText("SHABBAT MODE                " +
                        "SHABBAT MODE               " +
                        "SHABBAT MODE               " +
                        "SHABBAT MODE               " +
                        "SHABBAT MODE");
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.dark_blue));
                mShabbatModeBanner.setTextColor(getColor(R.color.white));
                mCalendarButton.setBackgroundColor(getColor(R.color.dark_blue));
        }

        if (isFirstTime) {
            mCurrentDate.add(Calendar.DATE,-1);
            mJewishDateInfo.setCalendar(mCurrentDate);
        }
    }

    /**
     * This method is called when the user clicks on shabbat mode. It will create another thread that will constantly try to scroll the recycler view
     * up and down.
     */
    @SuppressWarnings({"BusyWait"})
    private void startScrollingThread() {
        Thread scrollingThread = new Thread(() -> {
                    while (mMainRecyclerView.canScrollVertically(1)) {
                        if (!sShabbatMode) break;
                        if (mMainRecyclerView.canScrollVertically(1)) {
                            mMainRecyclerView.smoothScrollBy(0,5);
                        }
                        try {//must have these busy waits for scrolling to work properly. I assume it breaks because it is currently animating something. Will have to fix this in the future, but it works for now.
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {//must have these waits or else the RecyclerView will have corrupted info
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    while (mMainRecyclerView.canScrollVertically(-1)) {
                        if (!sShabbatMode) break;
                        if (mMainRecyclerView.canScrollVertically(-1)) {
                            mMainRecyclerView.smoothScrollBy(0,-5);
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                if (sShabbatMode) {
                    startScrollingThread();
                }
        });
        scrollingThread.start();
    }

    /**
     * This method is called when the user wants to end shabbat mode. It will hide the banner and remove the automatic zmanim updater queued task
     * from the handler. I will also reset the color of the calendar button to the default color.
     * @see #startScrollingThread()
     * @see #startShabbatMode()
     */
    private void endShabbatMode() {
        if (sShabbatMode) {
            sShabbatMode = false;
            mShabbatModeBanner.setVisibility(View.GONE);
            mHandler.removeCallbacksAndMessages(mZmanimUpdater);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (mSharedPreferences.getBoolean("useDefaultCalButtonColor", true)) {
                mCalendarButton.setBackgroundColor(getColor(R.color.dark_blue));
            } else {
                mCalendarButton.setBackgroundColor(mSharedPreferences.getInt("CalButtonColor", 0x18267C));
            }
        }
    }

    /**
     * This is the main method for updating the Zmanim in the recyclerview. It is called everytime the user changes the date or updates
     * any setting that affects the zmanim. This method returns a list of strings which are added to the recyclerview.
     * The strings that are zmanim are in the following format: "Zman= 12:00:00 AM" (seconds are optional). We parse the strings in @link{ZmanAdapter}
     * @return the updated information and Zmanim for the current day in a List of Strings with the following format: zman= 12:00:00 AM
     * (seconds are optional)
     */
    private List<String> getZmanimList() {
        DateFormat zmanimFormat;
        if (mSettingsPreferences.getBoolean("ShowSeconds", false)) {
            zmanimFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID)); //set the formatters time zone

        List<String> zmanim = new ArrayList<>();

        zmanim.add(mROZmanimCalendar.getGeoLocation().getLocationName());

        zmanim.add(mJewishDateInfo.getJewishCalendar().toString()
                .replace("Teves", "Tevet").replace("Tishrei", "Tishri")
                + "      " +
                mROZmanimCalendar.getCalendar().get(Calendar.DATE)
                + " " +
                mROZmanimCalendar.getCalendar().getDisplayName(
                        Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
                + ", " +
                mROZmanimCalendar.getCalendar().get(Calendar.YEAR));

        zmanim.add(mJewishDateInfo.getThisWeeksParsha());

        zmanim.add(mROZmanimCalendar.getCalendar()
                .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                + " / " +
                mJewishDateInfo.getJewishDayOfWeek());

        String day = mJewishDateInfo.getSpecialDay();
        if (!day.isEmpty()) {
            zmanim.add(day);
        }

        String isOKToListenToMusic = mJewishDateInfo.isOKToListenToMusic();
        if (!isOKToListenToMusic.isEmpty()) {
            zmanim.add(isOKToListenToMusic);
        }

        String ulChaparatPesha = mJewishDateInfo.getIsUlChaparatPeshaSaid();
        if (!ulChaparatPesha.isEmpty()) {
            zmanim.add(ulChaparatPesha);
        }

        zmanim.add(mJewishDateInfo.getIsTachanunSaid());

        String tonightStartOrEndBirchatLevana = mJewishDateInfo.getIsTonightStartOrEndBirchatLevana();
        if (!tonightStartOrEndBirchatLevana.isEmpty()) {
            zmanim.add(tonightStartOrEndBirchatLevana);
        }

        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            zmanim.add("Birchat HaChamah is said today");
        }

        addTekufaTime(zmanimFormat, zmanim);

        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            addHebrewZmanim(zmanimFormat, zmanim);
        } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
            addTranslatedEnglishZmanim(zmanimFormat, zmanim);
        } else {
            addEnglishZmanim(zmanimFormat, zmanim);
        }

        if (!mCurrentDateShown.before(dafYomiStartDate)) {
            zmanim.add("Daf Yomi: " + YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechta()
                    + " " +
                    formatHebrewNumber(YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf()));
        }
        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            String masechta = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar()).getMasechta();
            String daf = formatHebrewNumber(YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar()).getDaf());
            if (daf == null) {
                daf = "No Yerushalmi Daf Yomi";
                zmanim.add(daf);
            } else {
                zmanim.add("Yerushalmi Yomi: " + masechta + " " + daf);
            }
        }

        zmanim.add(mJewishDateInfo.getIsMashivHaruchOrMoridHatalSaid()
                + " / "
                + mJewishDateInfo.getIsBarcheinuOrBarechAleinuSaid());

        zmanim.add("Shaah Zmanit GR\"A: " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanisGra()) +
                " MG\"A: " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanis72MinutesZmanis()));

        if (mSettingsPreferences.getBoolean("ShowLeapYear", false)) {
            zmanim.add(mJewishDateInfo.isJewishLeapYear());
        }

        if (mSettingsPreferences.getBoolean("ShowDST", false)) {
            if (mROZmanimCalendar.getGeoLocation().getTimeZone().inDaylightTime(mROZmanimCalendar.getSeaLevelSunrise())) {
                zmanim.add("Daylight Savings Time is on");
            } else {
                zmanim.add("Daylight Savings Time is off");
            }
        }

        if (mSettingsPreferences.getBoolean("ShowElevation", false)) {
            zmanim.add("Elevation: " + mElevation);
        }

        return zmanim;
    }

    private void addEnglishZmanim(DateFormat zmanimFormat, List<String> zmanim) {
        zmanim.add("Alot Hashachar= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getAlos72Zmanis())));
        zmanim.add("Earliest Talit/Tefilin= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getEarliestTalitTefilin())));
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add("HaNetz (Elevated)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSunrise())));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true)) {
            zmanim.add("HaNetz= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getHaNetz())));
        } else {
            zmanim.add("HaNetz (Mishor)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add("HaNetz (Mishor)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
        }
        zmanim.add("Sof Zman Shma MG\"A= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis())));
        zmanim.add("Sof Zman Shma GR\"A= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaGRA())));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            zmanim.add("Sof Zman Achilat Chametz= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis())));
            zmanim.add("Sof Zman Brachot Shma= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaGRA())));
            zmanim.add("Sof Zman Biur Chametz= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanBiurChametzMGA())));
        } else {
            zmanim.add("Sof Zman Brachot Shma= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaGRA())));
        }
        zmanim.add("Chatzot= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getChatzot())));
        zmanim.add("Mincha Gedola= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaGedolaGreaterThan30())));
        zmanim.add("Mincha Ketana= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaKetana())));
        zmanim.add("Plag HaMincha= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getPlagHamincha())));
        if ((mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            zmanim.add("Candle Lighting (" +
                    (int) mROZmanimCalendar.getCandleLightingOffset() + ")= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getCandleLighting())));
        }
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false)) {
            if (mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            zmanim.add("Tzait " + getShabbatAndOrChag() + " (Tom) "
                                    + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "= " +
                                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                                DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                                zmanim.add("Rabbeinu Tam (Tom)= " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
                            } else {
                                zmanim.add("Rabbeinu Tam (Tom)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
                            }
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        zmanim.add("Shkia= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSunset())));
        zmanim.add("Tzait Hacochavim= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzeit())));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add("Candle Lighting= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzeit())));
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis()
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            zmanim.add("Tzait Taanit= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanit())));
            zmanim.add("Tzait Taanit L'Chumra= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanitLChumra())));
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            zmanim.add("Tzait " + getShabbatAndOrChag() + " "
                    + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                zmanim.add("Rabbeinu Tam = " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
            } else {
                zmanim.add("Rabbeinu Tam = " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                    DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                    zmanim.add("Rabbeinu Tam = " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
                } else {
                    zmanim.add("Rabbeinu Tam = " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
                }
            }
        }
        zmanim.add("Chatzot Layla= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSolarMidnight())));
    }

    private void addTranslatedEnglishZmanim(DateFormat zmanimFormat, List<String> zmanim) {
        zmanim.add("Dawn= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getAlos72Zmanis())));
        zmanim.add("Earliest Talit/Tefilin= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getEarliestTalitTefilin())));
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add("Sunrise (Elevated)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSunrise())));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true)) {
            zmanim.add("Sunrise= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getHaNetz())));
        } else {
            zmanim.add("Sunrise (Sea Level)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add("Sunrise (Sea Level)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
        }
        zmanim.add("Latest Shma MG\"A= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis())));
        zmanim.add("Latest Shma GR\"A= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaGRA())));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            zmanim.add("Latest Achilat Chametz= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis())));
            zmanim.add("Latest Brachot Shma= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaGRA())));
            zmanim.add("Latest Biur Chametz= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanBiurChametzMGA())));
        } else {
            zmanim.add("Latest Brachot Shma= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaGRA())));
        }
        zmanim.add("Mid-Day= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getChatzot())));
        zmanim.add("Mincha Gedola= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaGedolaGreaterThan30())));
        zmanim.add("Mincha Ketana= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaKetana())));
        zmanim.add("Plag HaMincha= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getPlagHamincha())));
        if ((mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            zmanim.add("Candle Lighting (" +
                    (int) mROZmanimCalendar.getCandleLightingOffset() + ")= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getCandleLighting())));
        }
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false)) {
            if (mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            zmanim.add(getShabbatAndOrChag() + " Ends (Tom) "
                                    + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "= " +
                                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                                DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                                zmanim.add("Rabbeinu Tam (Tom)= " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
                            } else {
                                zmanim.add("Rabbeinu Tam (Tom)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
                            }
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        zmanim.add("Sunset= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSunset())));
        zmanim.add("Nightfall= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzeit())));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add("Candle Lighting= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzeit())));
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis()
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            zmanim.add("Fast Ends= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanit())));
            zmanim.add("Fast Ends (Stringent)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanitLChumra())));
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            zmanim.add(getShabbatAndOrChag() + " Ends "
                    + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                zmanim.add("Rabbeinu Tam = " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
            } else {
                zmanim.add("Rabbeinu Tam = " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                    DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                    zmanim.add("Rabbeinu Tam = " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
                } else {
                    zmanim.add("Rabbeinu Tam = " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
                }
            }
        }
        zmanim.add("Midnight= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSolarMidnight())));
    }

    private void addHebrewZmanim(DateFormat zmanimFormat, List<String> zmanim) {
        zmanim.add("\u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getAlos72Zmanis())));
        zmanim.add("\u05D8\u05DC\u05D9\u05EA \u05D5\u05EA\u05E4\u05D9\u05DC\u05D9\u05DF= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getEarliestTalitTefilin())));
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add("\u05D4\u05E0\u05E5 (גבוה)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSunrise())));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true)) {
            zmanim.add("\u05D4\u05E0\u05E5= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getHaNetz())));
        } else {
            zmanim.add("\u05D4\u05E0\u05E5 (\u05DE\u05D9\u05E9\u05D5\u05E8)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add("\u05D4\u05E0\u05E5 (\u05DE\u05D9\u05E9\u05D5\u05E8)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
        }
        zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05E9\u05DE\u05E2 \u05DE\u05D2\"\u05D0= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis())));
        zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05E9\u05DE\u05E2 \u05D2\u05E8\"\u05D0= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaGRA())));
        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05D0\u05DB\u05D9\u05DC\u05EA \u05D7\u05DE\u05E5= "
                    + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis())));
            zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05D1\u05E8\u05DB\u05D5\u05EA \u05E9\u05DE\u05E2= "
                    + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaGRA())));
            zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05D1\u05D9\u05E2\u05D5\u05E8 \u05D7\u05DE\u05E5= "
                    + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanBiurChametzMGA())));
        } else {
            zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05D1\u05E8\u05DB\u05D5\u05EA \u05E9\u05DE\u05E2= "
                    + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaGRA())));
        }
        zmanim.add("\u05D7\u05E6\u05D5\u05EA= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getChatzot())));
        zmanim.add("\u05DE\u05E0\u05D7\u05D4 \u05D2\u05D3\u05D5\u05DC\u05D4= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaGedolaGreaterThan30())));
        zmanim.add("\u05DE\u05E0\u05D7\u05D4 \u05E7\u05D8\u05E0\u05D4= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaKetana())));
        zmanim.add("\u05E4\u05DC\u05D2 \u05D4\u05DE\u05E0\u05D7\u05D4= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getPlagHamincha())));
        if ((mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            zmanim.add("\u05D4\u05D3\u05DC\u05E7\u05EA \u05E0\u05E8\u05D5\u05EA (" +
                    (int) mROZmanimCalendar.getCandleLightingOffset() + ")= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getCandleLighting())));
        }
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false)) {
            if (mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!mJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            zmanim.add("\u05E6\u05D0\u05EA " + getShabbatAndOrChag() + " (\u05DE\u05D7\u05E8) "
                                    + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "= " +
                                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                                DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                                zmanim.add("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD (\u05DE\u05D7\u05E8)= " +
                                        roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
                            } else {
                                zmanim.add("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD (\u05DE\u05D7\u05E8)= " +
                                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
                            }
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        zmanim.add("\u05E9\u05E7\u05D9\u05E2\u05D4= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getSunset())));
        zmanim.add("\u05E6\u05D0\u05EA \u05D4\u05DB\u05D5\u05DB\u05D1\u05D9\u05DD= " +
                zmanimFormat.format(checkNull(mROZmanimCalendar.getTzeit())));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add("\u05D4\u05D3\u05DC\u05E7\u05EA \u05E0\u05E8\u05D5\u05EA= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzeit())));
            }
        }
        if (mJewishDateInfo.getJewishCalendar().isTaanis()
                && mJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            zmanim.add("\u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanit())));
            zmanim.add("\u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA \u05DC\u05D7\u05D5\u05DE\u05E8\u05D4= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanitLChumra())));
        }
        if (mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            zmanim.add("\u05E6\u05D0\u05EA " + getShabbatAndOrChag() + " " +
                    "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "=" +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                zmanim.add("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD = " +
                        roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
            } else {
                zmanim.add("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD = " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                    DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                    zmanim.add("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD = " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
                } else {
                    zmanim.add("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD = " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
                }
            }
        }
        zmanim.add("\u05D7\u05E6\u05D5\u05EA \u05DC\u05D9\u05DC\u05D4= " + zmanimFormat.format(mROZmanimCalendar.getSolarMidnight()));
    }

    /**
     * This is a simple convenience method to add a minute to a date object. If the date is not null,
     * it will return the same date with a minute added to it. Otherwise, if the date is null, it will return null.
     * @param date the date object to add a minute to
     * @return the given date a minute ahead if not null
     */
    private Date addMinuteToZman(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime() + 60_000);
    }

    /**
     * This is a simple convenience method to check if the given date is null or not. If the date is not null,
     * it will return exactly what was given. However, if the date is null, it will change the date to a string that says "N/A" (Not Available).
     * @param date the date object to check if it is null
     * @return the given date if not null or a string if null
     */
    private Object checkNull(Object date) {
        if (date != null) {
            return date;
        } else {
            return "N/A";
        }
    }

    /**
     * This is a simple convenience method to check if the current date is on shabbat or yom tov or both and return the correct string.
     * @return a string that says whether it is shabbat and chag or just shabbat or just chag (in Hebrew or English)
     */
    private String getShabbatAndOrChag() {
        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            if (mJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha() &&
                    mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "\u05E9\u05D1\u05EA/\u05D7\u05D2";
            } else if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "\u05E9\u05D1\u05EA";
            } else {
                return "\u05D7\u05D2";
            }
        } else {
            if (mJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha() &&
                    mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat/Chag";
            } else if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat";
            } else {
                return "Chag";
            }
        }
    }

    /**
     * This method will check if the tekufa happens within the next 48 hours and it will add the tekufa to the list of zmanim passed in if it happens
     * on the current date.
     * @param zmanimFormat the format to use for the zmanim
     * @param zmanim the list of zmanim to add to
     */
    private void addTekufaTime(DateFormat zmanimFormat, List<String> zmanim) {
        mCurrentDateShown.add(Calendar.DATE,1);//check next day for tekufa, because the tekufa time can go back a day
        mJewishDateInfo.setCalendar(mCurrentDateShown);
        mCurrentDateShown.add(Calendar.DATE,-1);
        if (mJewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(mCurrentDateShown.getTime(), mJewishDateInfo.getJewishCalendar().getTekufaAsDate())) {
            zmanim.add("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                    zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate()));
        }
        mJewishDateInfo.setCalendar(mCurrentDateShown);//reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (mJewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(mCurrentDateShown.getTime(), mJewishDateInfo.getJewishCalendar().getTekufaAsDate())) {
            zmanim.add("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                    zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate()));
        }
    }

    /**
     * This method checks if the user has already setup the elevation and visible sunrise from the last time he started
     * the app. If he has setup the elevation and visible sunrise, then it checks if the user is in the
     * same city as the last time he setup the app based on the getLocationAsName method. If the user is in the same city,
     * all is good. If the user is in another city, we create an AlertDialog to warn the user that the elevation data
     * and visible sunrise data are not accurate.
     *
     * @see #initAlertDialog()
     * @see LocationResolver#getLocationAsName()
     */
    private void getAndConfirmLastElevationAndVisibleSunriseData() {
        String lastLocation = mSharedPreferences.getString("lastLocation", "");

        String message ="The elevation and visible sunrise data change depending on the city you are in. " +
                        "Therefore, it is recommended that you update your elevation and visible sunrise " +
                        "data according to your current location." + "\n\n" +
                        "Last Location: " + lastLocation + "\n" +
                        "Current Location: " + sCurrentLocationName + "\n\n" +
                        "Would you like to rerun the setup now?";

        try {//TODO this needs to be removed but cannot be removed for now because it is needed for people who have setup the app before we changed data types
            if (sCurrentLocationName.contains("Lat:") && sCurrentLocationName.contains("Long:")) {
                sUserIsOffline = true;
                mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
            } else {//user is online
                mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));//get the last value of the current location or 0 if it doesn't exist
            }
        } catch (Exception e) {
            try {//legacy
                mElevation = mSharedPreferences.getFloat("elevation", 0);
            } catch (Exception e1) {
                mElevation = 0;
                e1.printStackTrace();
            }
        }

        if (mSharedPreferences.getBoolean("askagain", true)) {//only prompt user if he has not asked to be left alone
            if (!lastLocation.isEmpty() && !sCurrentLocationName.isEmpty() && //location name should never be empty. It either has the name or it has Lat: and Long:
                    !lastLocation.equals(sCurrentLocationName) &&
                    !sUserIsOffline) {//don't ask if the user is offline

                if (!mSharedPreferences.getBoolean("isElevationSetup" + sCurrentLocationName, false)) {//user should update his elevation in another city
                    mAlertDialog.setMessage(message);
                    mAlertDialog.show();
                } else {//if the user is in the same place, then we just need to check if his tables need to be updated
                    seeIfTablesNeedToBeUpdated(false);
                }

            }
        } else {//if the user has asked to be left alone for elevation, then we just need to check if his tables need to be updated
            seeIfTablesNeedToBeUpdated(false);
        }
    }

    /**
     * This method initializes the AlertDialog that will be shown to the user if the user is in another city and he has setup the app before.
     * The AlertDialog will have two buttons: "Yes" and "No". If the user clicks "Yes", then the user will be taken to the
     * elevation and visible sunrise setup activity. If the user clicks "No", then the user will be taken to the main activity.
     * There is also a "Do not ask again" button that will stop the user from being prompted again.
     * @see #getAndConfirmLastElevationAndVisibleSunriseData()
     */
    private void initAlertDialog() {
        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle("You are not in the same city as the last time that you " +
                        "setup the app!")
                .setPositiveButton("Yes", (dialogInterface, i) ->
                        mSetupLauncher.launch(new Intent(this, SetupChooserActivity.class)
                                .putExtra("fromMenu",true)))
                .setNegativeButton("No", (dialogInterface, i) -> Toast.makeText(
                        this, "Using mishor/sea level values", Toast.LENGTH_LONG)
                        .show())
                .setNeutralButton("Do not ask again", (dialogInterface, i) -> {
                    mSharedPreferences.edit().putBoolean("askagain", false).apply();
                    Toast.makeText(this, "Your current elevation is: " + mElevation, Toast.LENGTH_LONG)
                            .show();
                }).create();
    }

    /**
     * This method will create a new AlertDialog that asks the user to use their location and it
     * will also give the option to use a zipcode through the EditText field.
     */
    private void createZipcodeDialog() {
        final EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        new AlertDialog.Builder(this)
                .setTitle("Enter a Zipcode")
                .setMessage("WARNING! Zmanim will NOT be accurate! Using a Zipcode will give " +
                        "you zmanim based on approximately where you are. For more accurate " +
                        "zmanim, please allow the app to see your location.")
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (input.getText().toString().isEmpty()) {// I would have loved to use a regex to validate the zipcode, however, it seems like zip codes are not uniform.
                        Toast.makeText(this, "Please Enter a valid value, for example: 11024", Toast.LENGTH_SHORT)
                                .show();
                        createZipcodeDialog();
                    } else {
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putBoolean("useZipcode", true).apply();
                        editor.putString("Zipcode", input.getText().toString()).apply();
                        mLocationResolver = new LocationResolver(this, this);
                        mLocationResolver.getLatitudeAndLongitudeFromZipcode();
                        if (mSharedPreferences.getBoolean("isElevationSetup" + sCurrentLocationName, true)) {
                            mLocationResolver.start();
                            try {
                                mLocationResolver.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (!mInitialized) {
                            initMainView();
                        } else {
                            mLocationResolver.setTimeZoneID();
                            getAndConfirmLastElevationAndVisibleSunriseData();
                            instantiateZmanimCalendar();
                            mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                        }
                    }
                })
                .setNeutralButton("Use location", (dialog, which) -> {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean("useZipcode", false).apply();
                    mLocationResolver = new LocationResolver(this, this);
                    mLocationResolver.acquireLatitudeAndLongitude();
                    mLocationResolver.setTimeZoneID();
                    if (mSharedPreferences.getBoolean("isElevationSetup" + sCurrentLocationName, true)) {
                        mLocationResolver.start();
                        try {
                            mLocationResolver.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    getAndConfirmLastElevationAndVisibleSunriseData();
                    instantiateZmanimCalendar();
                    mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                })
                .create()
                .show();
    }

    @Override
    public void onBackPressed() {
        if (!mBackHasBeenPressed) {
            mBackHasBeenPressed = true;
            Toast.makeText(this, "Press back again to close the app", Toast.LENGTH_SHORT).show();
            return;
        }
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh) {
            if (mLocationResolver != null) {
                mLocationResolver = new LocationResolver(this, this);
                mLocationResolver.acquireLatitudeAndLongitude();
                mLocationResolver.setTimeZoneID();
                if (mSharedPreferences.getBoolean("isElevationSetup" + sCurrentLocationName, true)) {
                    mLocationResolver.start();
                    try {
                        mLocationResolver.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (mCurrentDate != null
                    && mCurrentDateShown != null
                    && mJewishDateInfo != null
                    && mROZmanimCalendar != null
                    && mMainRecyclerView != null) {// Some users were getting a crash here, so I added this check.
                mCurrentDate.setTimeInMillis(new Date().getTime());
                mCurrentDateShown.setTime(mCurrentDate.getTime());
                mJewishDateInfo.setCalendar(mCurrentDate);
                mROZmanimCalendar.setCalendar(mCurrentDate);
                mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                getAndConfirmLastElevationAndVisibleSunriseData();
            }
            return true;
        } else if (id == R.id.enterZipcode) {
            createZipcodeDialog();
            return true;
        } else if (id == R.id.shabbat_mode) {
            if (!sShabbatMode && mJewishDateInfo != null && mROZmanimCalendar != null && mMainRecyclerView != null) {
                mJewishDateInfo.setCalendar(mCurrentDate);
                mROZmanimCalendar.setCalendar(mCurrentDate);
                mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                startShabbatMode();
                item.setChecked(true);
            } else {
                endShabbatMode();
                item.setChecked(false);
            }
            return true;
        } else if (id == R.id.molad) {
            startActivity(new Intent(this, MoladActivity.class));
            return true;
        } else if (id == R.id.setupChooser) {
            mSetupLauncher.launch(new Intent(this, SetupChooserActivity.class)
                    .putExtra("fromMenu",true));
            return true;
        } else if (id == R.id.fullSetup) {
            mSetupLauncher.launch(new Intent(this, FullSetupActivity.class)
                    .putExtra("fromMenu",true));
            return true;
        } else if (id == R.id.settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (id == R.id.help) {
            new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight)
                    .setTitle("Help using this app:")
                    .setPositiveButton("ok", null)
                    .setMessage(R.string.helper_text)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This class is used to change the date in the main activity if the user swipes left or right.
     */
    private class ZmanimGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_MIN_DISTANCE = 180;

        @Override
        public boolean onFling(MotionEvent startMotionEvent, MotionEvent endMotionEvent, float xVelocity, float yVelocity) {

            if ( ( startMotionEvent == null ) || ( endMotionEvent == null ) ) {
                return false;
            }

            float xDiff = startMotionEvent.getRawX() - endMotionEvent.getRawX();

            if (Math.abs(xDiff) < SWIPE_MIN_DISTANCE) {
                return false;
            }

            if (Math.abs(xVelocity) > Math.abs(yVelocity)) {
                if (xDiff > 0) {
                    mNextDate.performClick();
                } else {
                    mPreviousDate.performClick();
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }
}