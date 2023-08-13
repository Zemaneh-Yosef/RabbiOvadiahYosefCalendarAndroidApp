package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.classes.JewishDateInfo.formatHebrewNumber;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.ChaiTables;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesScraper;
import com.ej.rovadiahyosefcalendar.classes.CustomDatePickerDialog;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanAdapter;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.classes.ZmanimNames;
import com.ej.rovadiahyosefcalendar.notifications.DailyNotifications;
import com.ej.rovadiahyosefcalendar.notifications.OmerNotifications;
import com.ej.rovadiahyosefcalendar.notifications.ZmanimNotifications;
import com.kosherjava.zmanim.hebrewcalendar.Daf;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;
import com.kosherjava.zmanim.util.GeoLocation;
import com.kosherjava.zmanim.util.ZmanimFormatter;

import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static boolean sShabbatMode;
    public static boolean sNetworkLocationServiceIsDisabled;
    public static boolean sGPSLocationServiceIsDisabled;
    public static boolean sUserIsOffline;
    public static boolean sFromSettings;
    private boolean mIsZmanimInHebrew;
    private boolean mIsZmanimEnglishTranslated;
    private boolean mBackHasBeenPressed = false;
    private boolean mUpdateTablesDialogShown;
    private boolean mInitialized = false;
    private int mCurrentPosition;//current top position in the list of zmanim to return to
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

    //android views for weekly zmanim:
    private TextView mEnglishMonthYear;//E.G. "June 2021 - 2022"
    private TextView mLocationName;//E.G. "New York, NY"
    private TextView mHebrewMonthYear;//E.G. "Sivan 5781 - 5782"
    private final ListView[] mListViews = new ListView[7];//one for each day of the week
    private final TextView[] mSunday = new TextView[6];
    private final TextView[] mMonday = new TextView[6];
    private final TextView[] mTuesday = new TextView[6];
    private final TextView[] mWednesday = new TextView[6];
    private final TextView[] mThursday = new TextView[6];
    private final TextView[] mFriday = new TextView[6];
    private final TextView[] mSaturday = new TextView[6];
    private TextView mWeeklyParsha;
    private TextView mWeeklyDafs;

    //This array holds the zmanim that we want to display in the announcements section of the weekly view:
    private ArrayList<String> mZmanimForAnnouncements;

    //custom classes/kosherjava classes:
    private LocationResolver mLocationResolver;
    private ROZmanimCalendar mROZmanimCalendar;
    public static JewishDateInfo sJewishDateInfo;
    private final ZmanimFormatter mZmanimFormatter = new ZmanimFormatter(TimeZone.getDefault());

    //android classes:
    private Handler mHandler = null;
    private Runnable mZmanimUpdater;
    private GestureDetector mGestureDetector;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsPreferences;
    public static final String SHARED_PREF = "MyPrefsFile";
    public static ActivityResultLauncher<Intent> sSetupLauncher;

    /**
     * The current date shown in the main activity.
     */
    private Calendar mCurrentDateShown = Calendar.getInstance();

    /**
     * The zman that is coming up next.
     */
    public static Date sNextUpcomingZman = null;

    /**
     * These calendars are used to know when daf/yerushalmi yomi started
     */
    private final static Calendar dafYomiStartDate = new GregorianCalendar(1923, Calendar.SEPTEMBER, 11);
    private final static Calendar dafYomiYerushalmiStartDate = new GregorianCalendar(1980, Calendar.FEBRUARY, 2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        setTheme(R.style.AppTheme); //splash screen
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_custom);//center the title
        mLayout = findViewById(R.id.main_layout);
        mHandler = new Handler(getMainLooper());
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mGestureDetector = new GestureDetector(MainActivity.this, new ZmanimGestureListener());
        mZmanimFormatter.setTimeFormat(ZmanimFormatter.SEXAGESIMAL_FORMAT);
        initSetupResult();
        setupShabbatModeBanner();
        mLocationResolver = new LocationResolver(this, this);
        sJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
        if (!ChaiTables.visibleSunriseFileExists(getExternalFilesDir(null), sCurrentLocationName, sJewishDateInfo.getJewishCalendar())
                && mSharedPreferences.getBoolean("UseTable" + sCurrentLocationName, true)
                && !mSharedPreferences.getBoolean("isSetup", false)
                && savedInstanceState == null) {//it should only not exist the first time running the app and only if the user has not set up the app
            sSetupLauncher.launch(new Intent(this, FullSetupActivity.class));
            initZmanimNotificationDefaults();
        } else {
            mLocationResolver.acquireLatitudeAndLongitude();
        }
        findAllWeeklyViews();
        if (sGPSLocationServiceIsDisabled && sNetworkLocationServiceIsDisabled) {// this is will only be true if the user has disabled both location services and is not using a zipcode
            Toast.makeText(MainActivity.this, R.string.please_enable_gps, Toast.LENGTH_SHORT).show();
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
        mSettingsPreferences.edit().putInt("TalitTefilin", -1).apply();
        mSettingsPreferences.edit().putInt("HaNetz", -1).apply();
        mSettingsPreferences.edit().putInt("SofZmanShmaMGA", 15).apply();
        mSettingsPreferences.edit().putInt("SofZmanShmaGRA", 15).apply();
        mSettingsPreferences.edit().putInt("SofZmanTefila", 15).apply();
        mSettingsPreferences.edit().putInt("SofZmanAchilatChametz", 15).apply();
        mSettingsPreferences.edit().putInt("SofZmanBiurChametz", 15).apply();
        mSettingsPreferences.edit().putInt("Chatzot", 20).apply();
        mSettingsPreferences.edit().putInt("MinchaGedola", -1).apply();
        mSettingsPreferences.edit().putInt("MinchaKetana", -1).apply();
        mSettingsPreferences.edit().putInt("PlagHaMinchaYY", -1).apply();
        mSettingsPreferences.edit().putInt("PlagHaMinchaHB", -1).apply();
        mSettingsPreferences.edit().putInt("CandleLighting", 15).apply();
        mSettingsPreferences.edit().putInt("Shkia", 15).apply();
        mSettingsPreferences.edit().putInt("TzeitHacochavim", 15).apply();
        mSettingsPreferences.edit().putInt("FastEnd", 15).apply();
        mSettingsPreferences.edit().putInt("FastEndStringent", 15).apply();
        mSettingsPreferences.edit().putInt("ShabbatEnd", -1).apply();
        mSettingsPreferences.edit().putInt("RT", 0).apply();
        mSettingsPreferences.edit().putInt("NightChatzot", -1).apply();
    }

    /**
     * This method registers the setupLauncher to receive the data that the user entered in the
     * SetupActivity. When the user finishes setting up the app, the setupLauncher will receive the
     * data and set the SharedPreferences to indicate that the user has set up the app.
     * It will also reinitialize the main view with the updated settings.
     */
    private void initSetupResult() {
        sSetupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
//                    if (result.getResultCode() == Activity.RESULT_OK) {
//                        if (result.getData() != null) {
//
//                        }
//                    }
                    if (!mInitialized) {
                        initMainView();
                    }
                    instantiateZmanimCalendar();
                    setZmanimLanguageBools();
                    if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                        updateWeeklyZmanim();
                    } else {
                        updateViewsInList();
                    }
                }
        );
    }

    private void showWeeklyTextViews() {
        TextView englishMonthYear = findViewById(R.id.englishMonthYear);
        TextView locationName = findViewById(R.id.location_name);
        TextView hebrewMonthYear = findViewById(R.id.hebrewMonthYear);
        LinearLayout mainWeekly = findViewById(R.id.main_weekly_layout);
        TextView weeklyParsha = findViewById(R.id.weeklyParsha);
        TextView weeklyDafs = findViewById(R.id.weeklyDafs);

        englishMonthYear.setVisibility(View.VISIBLE);
        locationName.setVisibility(View.VISIBLE);
        hebrewMonthYear.setVisibility(View.VISIBLE);
        mainWeekly.setVisibility(View.VISIBLE);
        weeklyParsha.setVisibility(View.VISIBLE);
        weeklyDafs.setVisibility(View.VISIBLE);
        mMainRecyclerView.setVisibility(View.GONE);
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setVisibility(View.GONE);
    }

    private void hideWeeklyTextViews() {
        LinearLayout mainWeekly = findViewById(R.id.main_weekly_layout);

        mEnglishMonthYear.setVisibility(View.GONE);
        mLocationName.setVisibility(View.GONE);
        mHebrewMonthYear.setVisibility(View.GONE);
        mainWeekly.setVisibility(View.GONE);
        mWeeklyParsha.setVisibility(View.GONE);
        mWeeklyDafs.setVisibility(View.GONE);
        mMainRecyclerView.setVisibility(View.VISIBLE);
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void findAllWeeklyViews() {
        mEnglishMonthYear = findViewById(R.id.englishMonthYear);
        mLocationName = findViewById(R.id.location_name);
        mHebrewMonthYear = findViewById(R.id.hebrewMonthYear);
        //there are 7 of these sets of views
        mListViews[0] = findViewById(R.id.zmanim);
        mSunday[1] = findViewById(R.id.announcements);
        mSunday[2] = findViewById(R.id.hebrewDay);
        mSunday[3] = findViewById(R.id.hebrewDate);
        mSunday[4] = findViewById(R.id.englishDay);
        mSunday[5] = findViewById(R.id.englishDateNumber);

        mListViews[1] = findViewById(R.id.zmanim2);
        mMonday[1] = findViewById(R.id.announcements2);
        mMonday[2] = findViewById(R.id.hebrewDay2);
        mMonday[3] = findViewById(R.id.hebrewDate2);
        mMonday[4] = findViewById(R.id.englishDay2);
        mMonday[5] = findViewById(R.id.englishDateNumber2);

        mListViews[2] = findViewById(R.id.zmanim3);
        mTuesday[1] = findViewById(R.id.announcements3);
        mTuesday[2] = findViewById(R.id.hebrewDay3);
        mTuesday[3] = findViewById(R.id.hebrewDate3);
        mTuesday[4] = findViewById(R.id.englishDay3);
        mTuesday[5] = findViewById(R.id.englishDateNumber3);

        mListViews[3] = findViewById(R.id.zmanim4);
        mWednesday[1] = findViewById(R.id.announcements4);
        mWednesday[2] = findViewById(R.id.hebrewDay4);
        mWednesday[3] = findViewById(R.id.hebrewDate4);
        mWednesday[4] = findViewById(R.id.englishDay4);
        mWednesday[5] = findViewById(R.id.englishDateNumber4);

        mListViews[4] = findViewById(R.id.zmanim5);
        mThursday[1] = findViewById(R.id.announcements5);
        mThursday[2] = findViewById(R.id.hebrewDay5);
        mThursday[3] = findViewById(R.id.hebrewDate5);
        mThursday[4] = findViewById(R.id.englishDay5);
        mThursday[5] = findViewById(R.id.englishDateNumber5);

        mListViews[5] = findViewById(R.id.zmanim6);
        mFriday[1] = findViewById(R.id.announcements6);
        mFriday[2] = findViewById(R.id.hebrewDay6);
        mFriday[3] = findViewById(R.id.hebrewDate6);
        mFriday[4] = findViewById(R.id.englishDay6);
        mFriday[5] = findViewById(R.id.englishDateNumber6);

        mListViews[6] = findViewById(R.id.zmanim7);
        mSaturday[1] = findViewById(R.id.announcements7);
        mSaturday[2] = findViewById(R.id.hebrewDay7);
        mSaturday[3] = findViewById(R.id.hebrewDate7);
        mSaturday[4] = findViewById(R.id.englishDay7);
        mSaturday[5] = findViewById(R.id.englishDateNumber7);

        mWeeklyParsha = findViewById(R.id.weeklyParsha);
        mWeeklyDafs = findViewById(R.id.weeklyDafs);
    }

    /**
     * This method initializes the main view. This method should only be called when we are able to initialize the @{link mROZmanimCalendar} object
     * with the correct latitude, longitude, elevation, and timezone.
     */
    private void initMainView() {
        mInitialized = true;
        if (sLatitude == 0 && sLongitude == 0) {//initMainView() is called after the location is acquired, however, this is a failsafe
            mLocationResolver.acquireLatitudeAndLongitude();
        }
        mLocationResolver.setTimeZoneID();
        resolveElevationAndVisibleSunrise();
        instantiateZmanimCalendar();
        saveGeoLocationInfo();
        setZmanimLanguageBools();
        setNextUpcomingZman();
        setupRecyclerViewAndTextViews();
        createBackgroundThreadForNextUpcomingZman();
        setupButtons();
        setNotifications();
        checkIfUserIsInIsraelOrNot();
        askForRealTimeNotificationPermissions();
        updateWidget();
    }

    private void updateWidget() {
        Intent intent = new Intent(this, ZmanimAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), ZmanimAppWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void setZmanimLanguageBools() {
        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            mIsZmanimInHebrew = true;
            mIsZmanimEnglishTranslated = false;
        } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
            mIsZmanimInHebrew = false;
            mIsZmanimEnglishTranslated = true;
        } else {
            mIsZmanimInHebrew = false;
            mIsZmanimEnglishTranslated = false;
        }
    }

    private void askForRealTimeNotificationPermissions() {
        if (mSharedPreferences.getBoolean("useZipcode", false)) {
            return;//if the user is using a zipcode, we don't need to ask for background location permission as we don't use the device's location
        }
        if (!mSharedPreferences.getBoolean("askedForRealtimeNotifications", false)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.alertDialog);
            builder.setTitle(R.string.would_you_like_to_receive_real_time_notifications_for_zmanim);
            builder.setMessage(R.string.if_you_would_like_to_receive_real_time_zmanim_notifications);
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
                new AlertDialog.Builder(this, R.style.alertDialog)
                        .setTitle(R.string.are_u_in_israel)
                        .setMessage(R.string.if_you_are_in_israel_now_please_confirm_below)
                        .setPositiveButton(R.string.yes_i_am_in_israel, (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("inIsrael", true).apply();
                            mSettingsPreferences.edit().putBoolean("LuachAmudeiHoraah", false).apply();
                            sJewishDateInfo = new JewishDateInfo(true, true);
                            Toast.makeText(this, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateViewsInList();
                            }
                        })
                        .setNegativeButton(R.string.no_i_am_not_in_israel, (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("askedInIsrael", true).apply();//save that we asked already
                            dialog.dismiss();
                        })
                        .setNeutralButton(R.string.do_not_ask_me_again, (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("neverAskInIsraelOrNot", true).apply();//save that we should never ask again
                            dialog.dismiss();
                        })
                        .show();
            }
        } else {//user is not in israel
            mSharedPreferences.edit().putBoolean("askedInIsrael", false).apply();//reset that we asked in israel
            if (mSharedPreferences.getBoolean("inIsrael", false) && //user was in israel before
                    !mSharedPreferences.getBoolean("askedInNotIsrael", false)) {//and we did not ask already
                new AlertDialog.Builder(this, R.style.alertDialog)
                        .setTitle(R.string.have_you_left_israel)
                        .setMessage(R.string.if_you_are_not_in_israel_now_please_confirm_below_otherwise_ignore_this_message)
                        .setPositiveButton(R.string.yes_i_have_left_israel, (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("inIsrael", false).apply();
                            sJewishDateInfo = new JewishDateInfo(false, true);
                            Toast.makeText(this, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateViewsInList();
                            }
                        })
                        .setNegativeButton(R.string.no_i_have_not_left_israel, (dialog, which) -> {
                            mSharedPreferences.edit().putBoolean("askedInNotIsrael", true).apply();//save that we asked
                            dialog.dismiss();
                        })
                        .setNeutralButton(R.string.do_not_ask_me_again, (dialog, which) -> {
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

            if (!ChaiTables.visibleSunriseFileExists(getExternalFilesDir(null), sCurrentLocationName, sJewishDateInfo.getJewishCalendar())) {
                if (!mUpdateTablesDialogShown) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.alertDialog);
                    builder.setTitle(R.string.update_tables);
                    builder.setMessage(R.string.the_visible_sunrise_tables_for_the_current_location_and_year_need_to_be_updated_do_you_want_to_update_the_tables_now);
                    builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                        String chaitablesURL = mSharedPreferences.getString("chaitablesLink" + sCurrentLocationName, "");
                        if (!chaitablesURL.isEmpty()) {//it should not be empty if the user has set up the app, but it is good to check
                            String hebrewYear = String.valueOf(sJewishDateInfo.getJewishCalendar().getJewishYear());
                            Pattern pattern = Pattern.compile("&cgi_yrheb=\\d{4}");
                            Matcher matcher = pattern.matcher(chaitablesURL);
                            if (matcher.find()) {
                                chaitablesURL = chaitablesURL.replace(matcher.group(), "&cgi_yrheb=" + hebrewYear);//replace the year in the URL with the current year
                            }
                            ChaiTablesScraper scraper = new ChaiTablesScraper();
                            scraper.setDownloadSettings(chaitablesURL, getExternalFilesDir(null), sJewishDateInfo.getJewishCalendar());
                            scraper.start();
                            try {
                                scraper.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateViewsInList();
                            }
                        }
                    });
                    builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                    builder.show();
                    if (fromButton) {
                        mUpdateTablesDialogShown = true;
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
        mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(mSettingsPreferences.getString("EndOfShabbatOffset",
                mSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40")));
    }

    private void setupRecyclerViewAndTextViews() {
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> new Thread(() -> {
            Looper.prepare();
            if (mLocationResolver == null) {
                mLocationResolver = new LocationResolver(MainActivity.this, MainActivity.this);
            }
            mLocationResolver.acquireLatitudeAndLongitude();
            mLocationResolver.setTimeZoneID();
            if (mCurrentDateShown != null
                    && sJewishDateInfo != null
                    && mROZmanimCalendar != null
                    && mMainRecyclerView != null) {
                mCurrentDateShown.setTime(new Date());
                sJewishDateInfo.setCalendar(new GregorianCalendar());
                resolveElevationAndVisibleSunrise();
                instantiateZmanimCalendar();
                instantiateZmanimCalendar();
                setNextUpcomingZman();
                runOnUiThread(this::updateViewsInList);
                runOnUiThread(() -> mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable()));
            }
            swipeRefreshLayout.setRefreshing(false);
            Objects.requireNonNull(Looper.myLooper()).quit();
        }).start());
        mMainRecyclerView = findViewById(R.id.mainRV);
        mMainRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMainRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mMainRecyclerView.setOnTouchListener((view, motionEvent) -> mGestureDetector.onTouchEvent(motionEvent));
        mLayout.setOnTouchListener((view, motionEvent) -> mGestureDetector.onTouchEvent(motionEvent));
        if (mSharedPreferences.getBoolean("weeklyMode", false)) {
            showWeeklyTextViews();
            updateWeeklyZmanim();
        } else {
            hideWeeklyTextViews();
            mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateViewsInList() {
//        ZmanAdapter zmanAdapter = (ZmanAdapter) mMainRecyclerView.getAdapter();
//        if (zmanAdapter != null) {
//            zmanAdapter.setZmanim(getZmanimList());
//            zmanAdapter.notifyDataSetChanged();
//        }
        mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
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
     * This method initializes the shabbat mode banner and sets up the functionality of hiding the banner when the user taps on it.
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
                mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();//just get a calendar object with the same date as the current one
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    mCurrentDateShown.add(Calendar.DATE, -7);//subtract seven days
                } else {
                    mCurrentDateShown.add(Calendar.DATE, -1);//subtract one day
                }
                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                sJewishDateInfo.setCalendar(mCurrentDateShown);
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateViewsInList();
                }
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
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    mCurrentDateShown.add(Calendar.DATE, 7);//add seven days
                } else {
                    mCurrentDateShown.add(Calendar.DATE, 1);//add one day
                }
                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                sJewishDateInfo.setCalendar(mCurrentDateShown);
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateViewsInList();
                }
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
        dialog.setCancelable(true);

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
            sJewishDateInfo.setCalendar(mUserChosenDate);
            mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            } else {
                updateViewsInList();
            }
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
            seeIfTablesNeedToBeUpdated(true);
        };

        return new CustomDatePickerDialog(this, onDateSetListener,
                mROZmanimCalendar.getCalendar().get(Calendar.YEAR),
                mROZmanimCalendar.getCalendar().get(Calendar.MONTH),
                mROZmanimCalendar.getCalendar().get(Calendar.DAY_OF_MONTH),
                sJewishDateInfo.getJewishCalendar());
    }

    /**
     * Returns the current calendar drawable depending on the current day of the month.
     */
    private int getCurrentCalendarDrawable() {
        if (!mSettingsPreferences.getBoolean("useDarkCalendarIcon", false)) {
            return getCurrentCalendarDrawableLight();
        } else {
            return getCurrentCalendarDrawableDark();
        }
    }

    private int getCurrentCalendarDrawableLight() {
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

    private int getCurrentCalendarDrawableDark() {
        switch (mROZmanimCalendar.getCalendar().get(Calendar.DATE)) {
            case (1):
                return R.drawable.calendar_1_dark;
            case (2):
                return R.drawable.calendar_2_dark;
            case (3):
                return R.drawable.calendar_3_dark;
            case (4):
                return R.drawable.calendar_4_dark;
            case (5):
                return R.drawable.calendar_5_dark;
            case (6):
                return R.drawable.calendar_6_dark;
            case (7):
                return R.drawable.calendar_7_dark;
            case (8):
                return R.drawable.calendar_8_dark;
            case (9):
                return R.drawable.calendar_9_dark;
            case (10):
                return R.drawable.calendar_10_dark;
            case (11):
                return R.drawable.calendar_11_dark;
            case (12):
                return R.drawable.calendar_12_dark;
            case (13):
                return R.drawable.calendar_13_dark;
            case (14):
                return R.drawable.calendar_14_dark;
            case (15):
                return R.drawable.calendar_15_dark;
            case (16):
                return R.drawable.calendar_16_dark;
            case (17):
                return R.drawable.calendar_17_dark;
            case (18):
                return R.drawable.calendar_18_dark;
            case (19):
                return R.drawable.calendar_19_dark;
            case (20):
                return R.drawable.calendar_20_dark;
            case (21):
                return R.drawable.calendar_21_dark;
            case (22):
                return R.drawable.calendar_22_dark;
            case (23):
                return R.drawable.calendar_23_dark;
            case (24):
                return R.drawable.calendar_24_dark;
            case (25):
                return R.drawable.calendar_25_dark;
            case (26):
                return R.drawable.calendar_26_dark;
            case (27):
                return R.drawable.calendar_27_dark;
            case (28):
                return R.drawable.calendar_28_dark;
            case (29):
                return R.drawable.calendar_29_dark;
            case (30):
                return R.drawable.calendar_30_dark;
            default:
                return R.drawable.calendar_31_dark;
        }
    }

    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mMainRecyclerView != null) {
            mCurrentPosition = ((LinearLayoutManager) Objects.requireNonNull(mMainRecyclerView.getLayoutManager())).findFirstVisibleItemPosition();
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
        if (sJewishDateInfo.getJewishCalendar().getInIsrael() != mSharedPreferences.getBoolean("inIsrael", false)) {
            sJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
            sJewishDateInfo.setCalendar(mCurrentDateShown);
        }
        setNextUpcomingZman();
        setZmanimLanguageBools();
        if (sFromSettings) {
            sFromSettings = false;
            instantiateZmanimCalendar();
            mROZmanimCalendar.setCalendar(mCurrentDateShown);
            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            } else {
                mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                mMainRecyclerView.scrollToPosition(mCurrentPosition);
            }
        }
        resolveElevationAndVisibleSunrise();
        if (!DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), new Date())
        && (new Date().getTime() - mCurrentDateShown.getTime().getTime()) > 7_200_000) {//two hours
            mCurrentDateShown.setTime(new Date());
            mROZmanimCalendar.setCalendar(mCurrentDateShown);
            sJewishDateInfo.setCalendar(mCurrentDateShown);
            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            } else {
                mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                mMainRecyclerView.scrollToPosition(mCurrentPosition);
            }
        }
        resetTheme();
        if (mSharedPreferences.getBoolean("useImage", false)) {
            Bitmap bitmap = BitmapFactory.decodeFile(mSharedPreferences.getString("imageLocation", ""));
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            mLayout.setBackground(drawable);
        }
        if (sShabbatMode) {
            setShabbatBannerColors(false);
        } else {
            if (mSharedPreferences.getBoolean("useDefaultCalButtonColor", true)) {
                mCalendarButton.setBackgroundColor(getColor(R.color.dark_blue));
            } else {
                mCalendarButton.setBackgroundColor(mSharedPreferences.getInt("CalButtonColor", 0x18267C));
            }
        }
        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
        //this is to update the zmanim notifications if the user changed the settings to start showing them
        PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                0,
                new Intent(getApplicationContext(), ZmanimNotifications.class),
                PendingIntent.FLAG_IMMUTABLE);
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
                if (getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                    break;
                }
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Day":
                if (getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
                    break;
                }
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Night":
                if (getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    break;
                }
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    /**
     * This method saves the information needed to restore a GeoLocation object in the notification classes.
     */
    private void saveGeoLocationInfo() {//needed for notifications
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("name", sCurrentLocationName).apply();
        editor.putLong("lat", Double.doubleToRawLongBits(sLatitude)).apply();//see here: https://stackoverflow.com/a/18098090/13593159
        editor.putLong("long", Double.doubleToRawLongBits(sLongitude)).apply();
        editor.putString("timezoneID", sCurrentTimeZoneID).apply();
    }

    /**
     * This method will be called every time the user opens the app. It will reset the notifications every time the app is opened since the user might
     * have changed his location or settings.
     */
    private void setNotifications() {
        if (mSettingsPreferences.getBoolean("zmanim_notifications", true)) {//if the user wants notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {// ask for permission to send notifications for newer versions of android ughhhh...
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                }
            }
        }
        Calendar calendar = (Calendar) mROZmanimCalendar.getCalendar().clone();
        if (mROZmanimCalendar.getSunrise() != null) {
            calendar.setTimeInMillis(mROZmanimCalendar.getSunrise().getTime());
        }
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                new Intent(getApplicationContext(), DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(dailyPendingIntent);//cancel any previous alarms
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dailyPendingIntent);

        if (mROZmanimCalendar.getTzeit() != null) {
            if (mSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
                calendar.setTimeInMillis(mROZmanimCalendar.getTzeitAmudeiHoraah().getTime());
            } else {
                calendar.setTimeInMillis(mROZmanimCalendar.getTzeit().getTime());
            }
        }
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                new Intent(getApplicationContext(), OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        am.cancel(omerPendingIntent);//cancel any previous alarms
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), omerPendingIntent);

        //zmanim notifications are set in the onResume method
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
                startShabbatMode();
            }
    }

    /**
     * This method is called when the user clicks on shabbat mode. The main point of this method is to automatically scroll through the list of zmanim
     * and update the date when the time reaches the next date at 12:00:02am. It will also update the shabbat banner to reflect the next day's date.
     * (The reason why I chose 12:00:02am is to avoid a hiccup if the device is too fast to update the time, although it is probably not a big deal.)
     * @see #startScrollingThread() to start the thread that will scroll through the list of zmanim
     * @see #setShabbatBannerColors(boolean) to set the text of the shabbat banners
     */
    private void startShabbatMode() {
        if (!sShabbatMode) {
            sShabbatMode = true;
            setShabbatBannerColors(true);
            mShabbatModeBanner.setVisibility(View.VISIBLE);
            WindowManager windowManager =  (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Configuration configuration = getResources().getConfiguration();
            int rotation = windowManager.getDefaultDisplay().getRotation();
            // Search for the natural position of the device
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) ||
                    configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
                            (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)) {
                switch (rotation) {// Natural position is Landscape
                    case Surface.ROTATION_0:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                    case Surface.ROTATION_90:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        break;
                    case Surface.ROTATION_180:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        break;
                    case Surface.ROTATION_270:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                }
            } else {// Natural position is Portrait
                switch (rotation) {
                    case Surface.ROTATION_0:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                    case Surface.ROTATION_90:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                    case Surface.ROTATION_180:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        break;
                    case Surface.ROTATION_270:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        break;
                }
            }
            mNextDate.setVisibility(View.GONE);
            mPreviousDate.setVisibility(View.GONE);
            Calendar calendar = Calendar.getInstance();
            Calendar calendar2 = (Calendar) calendar.clone();
            mZmanimUpdater = () -> {
                calendar.setTimeInMillis(new Date().getTime());
                mCurrentDateShown.setTimeInMillis(calendar.getTime().getTime());
                mROZmanimCalendar.setCalendar(calendar);
                sJewishDateInfo.setCalendar(calendar);
                setShabbatBannerColors(false);
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateViewsInList();
                }
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
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
    private void setShabbatBannerColors(boolean isFirstTime) {
        if (isFirstTime) {
            mCurrentDateShown.add(Calendar.DATE,1);
            sJewishDateInfo.setCalendar(mCurrentDateShown);
        }

        boolean isShabbat = mCurrentDateShown.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;

        StringBuilder sb = new StringBuilder();

        switch (sJewishDateInfo.getJewishCalendar().getYomTovIndex()) {
            case JewishCalendar.PESACH:
                for (int i = 0; i < 4; i++) {
                    sb.append(getString(R.string.PESACH));
                    if (isShabbat) {
                        sb.append(getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.lightYellow));
                mShabbatModeBanner.setTextColor(getColor(R.color.black));
                mCalendarButton.setBackgroundColor(getColor(R.color.lightYellow));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark());
                break;
            case JewishCalendar.SHAVUOS:
                for (int i = 0; i < 4; i++) {
                    sb.append(getString(R.string.SHAVUOT));
                    if (isShabbat) {
                        sb.append(getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.light_blue));
                mShabbatModeBanner.setTextColor(getColor(R.color.white));
                mCalendarButton.setBackgroundColor(getColor(R.color.light_blue));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight());
                break;
            case JewishCalendar.SUCCOS:
                for (int i = 0; i < 4; i++) {
                    sb.append(getString(R.string.SUCCOT));
                    if (isShabbat) {
                        sb.append(getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.light_green));
                mShabbatModeBanner.setTextColor(getColor(R.color.black));
                mCalendarButton.setBackgroundColor(getColor(R.color.light_green));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark());
                break;
            case JewishCalendar.SHEMINI_ATZERES:
                for (int i = 0; i < 4; i++) {
                    sb.append(getString(R.string.SHEMINI_ATZERET));
                    if (isShabbat) {
                        sb.append(getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.light_green));
                mShabbatModeBanner.setTextColor(getColor(R.color.black));
                mCalendarButton.setBackgroundColor(getColor(R.color.light_green));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark());
                break;
            case JewishCalendar.SIMCHAS_TORAH:
                for (int i = 0; i < 4; i++) {
                    sb.append(getString(R.string.SIMCHAT_TORAH));
                    if (isShabbat) {
                        sb.append(getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.green));
                mShabbatModeBanner.setTextColor(getColor(R.color.black));
                mCalendarButton.setBackgroundColor(getColor(R.color.green));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark());
                break;
            case JewishCalendar.ROSH_HASHANA:
                for (int i = 0; i < 4; i++) {
                    sb.append(getString(R.string.ROSH_HASHANA));
                    if (isShabbat) {
                        sb.append(getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.dark_red));
                mShabbatModeBanner.setTextColor(getColor(R.color.white));
                mCalendarButton.setBackgroundColor(getColor(R.color.dark_red));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight());
                break;
            case JewishCalendar.YOM_KIPPUR:
                for (int i = 0; i < 4; i++) {
                    sb.append(getString(R.string.YOM_KIPPUR));
                    if (isShabbat) {
                        sb.append(getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.white));
                mShabbatModeBanner.setTextColor(getColor(R.color.black));
                mCalendarButton.setBackgroundColor(getColor(R.color.white));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark());
                break;
            default:
                mShabbatModeBanner.setText(getString(R.string.SHABBAT_MODE) +
                        "                " +
                        getString(R.string.SHABBAT_MODE) +
                        "               " +
                        getString(R.string.SHABBAT_MODE) +
                        "               " +
                        getString(R.string.SHABBAT_MODE) +
                        "               " +
                        getString(R.string.SHABBAT_MODE));
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.dark_blue));
                mShabbatModeBanner.setTextColor(getColor(R.color.white));
                mCalendarButton.setBackgroundColor(getColor(R.color.dark_blue));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight());
        }

        if (isFirstTime) {
            mCurrentDateShown.add(Calendar.DATE,-1);
            sJewishDateInfo.setCalendar(mCurrentDateShown);
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
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
            mNextDate.setVisibility(View.VISIBLE);
            mPreviousDate.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This is the main method for updating the Zmanim in the list view. It is called everytime the user changes the date or updates
     * any setting that affects the zmanim. This method returns a list of ZmanListEntry objects that will be used to populate the list view.
     * @return List of ZmanListEntry objects that will be used to populate the list view.
     * @see ZmanListEntry
     * */
    private List<ZmanListEntry> getZmanimList() {
        List<ZmanListEntry> zmanim = new ArrayList<>();

        zmanim.add(new ZmanListEntry(mROZmanimCalendar.getGeoLocation().getLocationName()));

        StringBuilder sb = new StringBuilder();
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            sb.append(formatHebrewNumber(mROZmanimCalendar.getCalendar().get(Calendar.DATE)));
        } else {
            sb.append(mROZmanimCalendar.getCalendar().get(Calendar.DATE));
        }
        sb.append(" ");
        sb.append(mROZmanimCalendar.getCalendar().getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
        sb.append(", ");
        sb.append(mROZmanimCalendar.getCalendar().get(Calendar.YEAR));

        if (DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), new Date())) {
            sb.append("   ▼   ");//add a down arrow to indicate that this is the current day
        } else {
            sb.append("      ");
        }

        sb.append(sJewishDateInfo.getJewishCalendar().toString()
                .replace("Teves", "Tevet")
                .replace("Tishrei", "Tishri"));

        zmanim.add(new ZmanListEntry(sb.toString()));

        zmanim.add(new ZmanListEntry(sJewishDateInfo.getThisWeeksParsha()));

        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        if (mSettingsPreferences.getBoolean("showShabbatMevarchim", true)) {
            if (sJewishDateInfo.getJewishCalendar().isShabbosMevorchim()) {
                zmanim.add(new ZmanListEntry("שבת מברכים"));
            }
        }
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());//reset


        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            zmanim.add(new ZmanListEntry(mROZmanimCalendar.getCalendar()
                    .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())));
        } else {
            zmanim.add(new ZmanListEntry(mROZmanimCalendar.getCalendar()
                    .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                    + " / " +
                    sJewishDateInfo.getJewishDayOfWeek()));
        }

        String day = sJewishDateInfo.getSpecialDay();
        if (!day.isEmpty()) {
            zmanim.add(new ZmanListEntry(day));
        }

        if (sJewishDateInfo.is3Weeks()) {
            if (sJewishDateInfo.is9Days()) {
                if (sJewishDateInfo.isShevuahShechalBo()) {
                    zmanim.add(new ZmanListEntry(getString(R.string.shevuah_shechal_bo)));
                } else {
                    zmanim.add(new ZmanListEntry(getString(R.string.nine_days)));
                }
            } else {
                zmanim.add(new ZmanListEntry(getString(R.string.three_weeks)));
            }
        }

        String isOKToListenToMusic = sJewishDateInfo.isOKToListenToMusic();
        if (!isOKToListenToMusic.isEmpty()) {
            zmanim.add(new ZmanListEntry(isOKToListenToMusic));
        }

        String hallel = sJewishDateInfo.getHallelOrChatziHallel();
        if (!hallel.isEmpty()) {
            zmanim.add(new ZmanListEntry(hallel));
        }

        String ulChaparatPesha = sJewishDateInfo.getIsUlChaparatPeshaSaid();
        if (!ulChaparatPesha.isEmpty()) {
            zmanim.add(new ZmanListEntry(ulChaparatPesha));
        }

        zmanim.add(new ZmanListEntry(sJewishDateInfo.getIsTachanunSaid()));

        String birchatLevana = sJewishDateInfo.getBirchatLevana();
        if (!birchatLevana.isEmpty()) {
            zmanim.add(new ZmanListEntry(birchatLevana));
        }

        if (sJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            zmanim.add(new ZmanListEntry(getString(R.string.birchat_hachamah_is_said_today)));
        }

        String tekufaOpinions = mSettingsPreferences.getString("TekufaOpinions", "1");
        if (tekufaOpinions.equals("1")) {
            addTekufaTime(zmanim, false);
        }
        if (tekufaOpinions.equals("2")) {
            addAmudeiHoraahTekufaTime(zmanim, false);
        }
        if (tekufaOpinions.equals("3")) {
            addTekufaTime(zmanim, false);
            addAmudeiHoraahTekufaTime(zmanim, false);
        }

        addZmanim(zmanim, false);

        if (!mCurrentDateShown.before(dafYomiStartDate)) {
            zmanim.add(new ZmanListEntry(getString(R.string.daf_yomi)  + " " + YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getMasechta()
                    + " " +
                    formatHebrewNumber(YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getDaf()),
                    sJewishDateInfo.getJewishCalendar().getGregorianCalendar().getTime(),false));
        }
        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            Daf dafYomiYerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar());
            if (dafYomiYerushalmi != null) {
                String masechta = dafYomiYerushalmi.getYerushalmiMasechta();
                String daf = formatHebrewNumber(dafYomiYerushalmi.getDaf());
                zmanim.add(new ZmanListEntry(getString(R.string.yerushalmi_yomi) + " " + masechta + " " + daf));
            } else {
                zmanim.add(new ZmanListEntry(getString(R.string.no_daf_yomi_yerushalmi)));
            }
        }

        zmanim.add(new ZmanListEntry(sJewishDateInfo.getIsMashivHaruchOrMoridHatalSaid()
                + " / "
                + sJewishDateInfo.getIsBarcheinuOrBarechAleinuSaid()));

        zmanim.add(new ZmanListEntry(getString(R.string.show_siddur)));

        if (!mSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            zmanim.add(new ZmanListEntry(getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanisGra())
                    + " " + getString(R.string.mg_a) + " " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanis72MinutesZmanis())));
        } else {
            long shaahZmanitMGA = mROZmanimCalendar.getTemporalHour(mROZmanimCalendar.getAlotAmudeiHoraah(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraah());
            zmanim.add(new ZmanListEntry(getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanisGra())
                    + " " + getString(R.string.mg_a) + " " + mZmanimFormatter.format(shaahZmanitMGA)));
        }

        if (mSettingsPreferences.getBoolean("ShowLeapYear", false)) {
            zmanim.add(new ZmanListEntry(sJewishDateInfo.isJewishLeapYear()));
        }

        if (mSettingsPreferences.getBoolean("ShowDST", false)) {
            if (mROZmanimCalendar.getGeoLocation().getTimeZone().inDaylightTime(mROZmanimCalendar.getSeaLevelSunrise())) {
                zmanim.add(new ZmanListEntry(getString(R.string.daylight_savings_time_is_on)));
            } else {
                zmanim.add(new ZmanListEntry(getString(R.string.daylight_savings_time_is_off)));
            }
        }

        if (mSettingsPreferences.getBoolean("ShowElevation", false)) {
            zmanim.add(new ZmanListEntry(getString(R.string.elevation) + " " + mElevation + " " + getString(R.string.meters)));
        }

        return zmanim;
    }

    private void createBackgroundThreadForNextUpcomingZman() {
        Runnable nextZmanUpdater = () -> {
            setNextUpcomingZman();
            if (mMainRecyclerView != null && !mSharedPreferences.getBoolean("weeklyMode", false)) {
                mCurrentPosition = ((LinearLayoutManager) Objects.requireNonNull(mMainRecyclerView.getLayoutManager())).findFirstVisibleItemPosition();
                updateViewsInList();
                if (mCurrentPosition < Objects.requireNonNull(mMainRecyclerView.getAdapter()).getItemCount()) {
                    mMainRecyclerView.scrollToPosition(mCurrentPosition);
                }
            } else if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            }
            createBackgroundThreadForNextUpcomingZman();//start a new thread to update the next upcoming zman
        };
        if (sNextUpcomingZman != null) {
            mHandler.postDelayed(nextZmanUpdater,sNextUpcomingZman.getTime() - new Date().getTime() + 1_000);//add 1 second to make sure we don't get the same zman again
        }
    }

    public void setNextUpcomingZman() {
        Date theZman = null;
        List<ZmanListEntry> zmanim = new ArrayList<>();
        Calendar today = Calendar.getInstance();

        today.add(Calendar.DATE, -1);
        mROZmanimCalendar.setCalendar(today);
        sJewishDateInfo.setCalendar(today);
        addZmanim(zmanim, false);//for the previous day

        today.add(Calendar.DATE, 1);
        mROZmanimCalendar.setCalendar(today);
        sJewishDateInfo.setCalendar(today);
        addZmanim(zmanim, false);//for the current day

        today.add(Calendar.DATE, 1);
        mROZmanimCalendar.setCalendar(today);
        sJewishDateInfo.setCalendar(today);
        addZmanim(zmanim, false);//for the next day

        mROZmanimCalendar.setCalendar(mCurrentDateShown);
        sJewishDateInfo.setCalendar(mCurrentDateShown);//reset
        //find the next upcoming zman that is after the current time and before all the other zmanim
        for (ZmanListEntry zmanEntry : zmanim) {
            Date zman = zmanEntry.getZman();
            if (zman != null && zman.after(new Date()) && (theZman == null || zman.before(theZman))) {
                theZman = zman;
            }
        }
        sNextUpcomingZman = theZman;
    }

    private String getAnnouncements() {
        StringBuilder announcements = new StringBuilder();

        String day = sJewishDateInfo.getSpecialDay();
        if (!day.isEmpty()) {
            announcements.append(day.replace("/ ","\n")).append("\n");
        }

        String isOKToListenToMusic = sJewishDateInfo.isOKToListenToMusic();
        if (!isOKToListenToMusic.isEmpty()) {
            announcements.append(isOKToListenToMusic).append("\n");
        }

        String ulChaparatPesha = sJewishDateInfo.getIsUlChaparatPeshaSaid();
        if (!ulChaparatPesha.isEmpty()) {
            announcements.append(ulChaparatPesha).append("\n");
        }

        String hallel = sJewishDateInfo.getHallelOrChatziHallel();
        if (!hallel.isEmpty()) {
            announcements.append(hallel).append("\n");
        }

        TefilaRules tefilaRules = new TefilaRules();

        if (tefilaRules.isMashivHaruachEndDate(sJewishDateInfo.getJewishCalendar())) {
            announcements.append("מוריד הטל/ברכנו").append("\n");
        }

        if (tefilaRules.isMashivHaruachStartDate(sJewishDateInfo.getJewishCalendar())) {
            announcements.append("משיב הרוח").append("\n");
        }

        if (tefilaRules.isVeseinTalUmatarStartDate(sJewishDateInfo.getJewishCalendar())) {
            announcements.append("ברך עלינו").append("\n");
        }

        String tachanun = sJewishDateInfo.getIsTachanunSaid();
        if (!tachanun.equals(getString(R.string.there_is_tachanun_today))) {
            announcements.append(tachanun).append("\n");
        }

        String tonightStartOrEndBirchatLevana = sJewishDateInfo.getBirchatLevana();
        if (!tonightStartOrEndBirchatLevana.isEmpty()) {
            announcements.append(tonightStartOrEndBirchatLevana).append("\n");
        }

        if (sJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            announcements.append(getString(R.string.birchat_hachamah_is_said_today)).append("\n");
        }

        List<ZmanListEntry> tekufa = new ArrayList<>();
        String tekufaOpinions = mSettingsPreferences.getString("TekufaOpinions", "1");
        if (tekufaOpinions.equals("1")) {
            addTekufaTime(tekufa, true);
        }
        if (tekufaOpinions.equals("2")) {
            addAmudeiHoraahTekufaTime(tekufa, true);
        }
        if (tekufaOpinions.equals("3")) {
            addTekufaTime(tekufa, true);
            addAmudeiHoraahTekufaTime(tekufa, true);
        }
        if (!tekufa.isEmpty()) {
            for (ZmanListEntry tekufaEntry : tekufa) {
                announcements.append(tekufaEntry.getTitle()).append("\n");
            }
        }

        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            if (YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar()) == null) {
                announcements.append(getString(R.string.no_daf_yomi_yerushalmi)).append("\n");
            }
        }
        return announcements.toString();
    }

    private void updateWeeklyZmanim() {
        Calendar backupCal = (Calendar) mCurrentDateShown.clone();
        while (mCurrentDateShown.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            mCurrentDateShown.add(Calendar.DATE, -1);
        }
        mROZmanimCalendar.setCalendar(mCurrentDateShown);//set the calendar to the sunday of that week
        sJewishDateInfo.setCalendar(mCurrentDateShown);

        HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
        List<TextView[]> weeklyInfo = Arrays.asList(mSunday, mMonday, mTuesday, mWednesday, mThursday, mFriday, mSaturday);

        String month = mCurrentDateShown.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        String year = String.valueOf(mCurrentDateShown.get(Calendar.YEAR));

        String hebrewMonth = hebrewDateFormatter.formatMonth(sJewishDateInfo.getJewishCalendar())
                .replace("Tishrei", "Tishri")
                .replace("Teves", "Tevet");
        String hebrewYear = String.valueOf(sJewishDateInfo.getJewishCalendar().getJewishYear());

        String masechta = "";
        String yerushalmiMasechta = "";
        String daf = "";
        String yerushalmiDaf = "";

        if (!mCurrentDateShown.before(dafYomiStartDate)) {
            masechta = YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getMasechta();
            daf = formatHebrewNumber(YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getDaf());
        }
        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            yerushalmiMasechta = YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar()).getMasechta();
            yerushalmiDaf = formatHebrewNumber(YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar()).getDaf());
        }

        for (int i = 0; i < 7; i++) {
            if (DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), new Date())) {
                weeklyInfo.get(i)[4].setBackgroundColor(getColor(R.color.dark_gold));
            } else {
                weeklyInfo.get(i)[4].setBackground(null);
            }
            StringBuilder announcements = new StringBuilder();
            mZmanimForAnnouncements = new ArrayList<>();//clear the list, it will be filled again in the getShortZmanim method
            mListViews[i].setAdapter(new ArrayAdapter<>(this, R.layout.zman_list_view, getShortZmanim()));//E.G. "Sunrise: 5:45 AM, Sunset: 8:30 PM, etc."
            if (!mZmanimForAnnouncements.isEmpty()) {
                for (String zman : mZmanimForAnnouncements) {
                    announcements.append(zman).append("\n");
                }
            }
            announcements.append(getAnnouncements());
            weeklyInfo.get(i)[1].setText(announcements.toString());//E.G. "Yom Tov, Yom Kippur, etc."
            weeklyInfo.get(i)[2].setText(sJewishDateInfo.getJewishDayOfWeek());//E.G. "יום ראשון"
            weeklyInfo.get(i)[3].setText(formatHebrewNumber(sJewishDateInfo.getJewishCalendar().getJewishDayOfMonth()));//E.G. "א"
            weeklyInfo.get(i)[4].setText(mROZmanimCalendar.getCalendar().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));//E.G. "Sun"
            weeklyInfo.get(i)[5].setText(String.valueOf(mROZmanimCalendar.getCalendar().get(Calendar.DAY_OF_MONTH)));//E.G. "6"
            if (i != 6) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        if (month != null && !month.equals(mROZmanimCalendar.getCalendar().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()))) {
            month += " - " + mROZmanimCalendar.getCalendar().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        }
        if (!year.equals(String.valueOf(mROZmanimCalendar.getCalendar().get(Calendar.YEAR)))) {
            year += " / " + mROZmanimCalendar.getCalendar().get(Calendar.YEAR);
        }
        if (!hebrewMonth.equals(hebrewDateFormatter.formatMonth(sJewishDateInfo.getJewishCalendar())
                        .replace("Tishrei", "Tishri")
                        .replace("Teves", "Tevet"))) {
            hebrewMonth += " - " + hebrewDateFormatter.formatMonth(sJewishDateInfo.getJewishCalendar())
                    .replace("Tishrei", "Tishri")
                    .replace("Teves", "Tevet");
        }
        if (!hebrewYear.equals(String.valueOf(sJewishDateInfo.getJewishCalendar().getJewishYear()))) {
            hebrewYear += " / " + sJewishDateInfo.getJewishCalendar().getJewishYear();
        }
        if (!masechta.equals(YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getMasechta())) {
            masechta += " " + daf + " - " + YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getMasechta() + " " +
                    formatHebrewNumber(YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getDaf());
        } else {
            masechta += " " + daf + " - " + formatHebrewNumber(YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getDaf());
        }
        if (!yerushalmiMasechta.equals(YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar()).getMasechta())) {
            if (YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar()).getDaf() == 0) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
            yerushalmiMasechta += " " + yerushalmiDaf + " - " + YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar()).getMasechta() + " " +
                    formatHebrewNumber(YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar()).getDaf());
        } else {
            yerushalmiMasechta += " " + yerushalmiDaf + " - " + formatHebrewNumber(YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar()).getDaf());
        }
        String dafs = getString(R.string.daf_yomi) + " " + masechta + "       " + getString(R.string.yerushalmi_yomi) + " " + yerushalmiMasechta;
        String monthYear = month + " " + year;
        mEnglishMonthYear.setText(monthYear);
        mLocationName.setText(sCurrentLocationName);
        String hebrewMonthYear = hebrewMonth + " " + hebrewYear;
        mHebrewMonthYear.setText(hebrewMonthYear);
        mWeeklyDafs.setText(dafs);
        mWeeklyParsha.setText(sJewishDateInfo.getThisWeeksParsha());
        mROZmanimCalendar.getCalendar().setTimeInMillis(backupCal.getTimeInMillis());
        sJewishDateInfo.setCalendar(backupCal);
        mCurrentDateShown = backupCal;
    }

    private String[] getShortZmanim() {
        List<ZmanListEntry> zmanim = new ArrayList<>();
        addZmanim(zmanim, true);
        DateFormat zmanimFormat;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            if (mSettingsPreferences.getBoolean("ShowSeconds", false)) {
                zmanimFormat = new SimpleDateFormat("H:mm:ss", Locale.getDefault());
            } else {
                zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
            }
        } else {
            if (mSettingsPreferences.getBoolean("ShowSeconds", false)) {
                zmanimFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
            } else {
                zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
            }
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));

        //filter out important zmanim
        List<ZmanListEntry> zmansToRemove = new ArrayList<>();
        if (mIsZmanimInHebrew) {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isNoteworthyZman()) {
                    if (zman.isRTZman() && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("RoundUpRT", false)) {
                        DateFormat rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                        rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                        mZmanimForAnnouncements.add(rtFormat.format(zman.getZman()) + ":" + zman.getTitle().replaceAll("\\(.*\\)", "").trim());
                    } else {
                        mZmanimForAnnouncements.add(zmanimFormat.format(zman.getZman()) + ":" + zman.getTitle().replaceAll("\\(.*\\)", "").trim());
                    }
                    zmansToRemove.add(zman);
                }
            }
        } else {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isNoteworthyZman()) {
                    if (zman.isRTZman() && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("RoundUpRT", false)) {
                        DateFormat rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                        rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                        mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ":" + rtFormat.format(zman.getZman()));
                    } else {
                        mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ":" + zmanimFormat.format(zman.getZman()));
                    }
                    zmansToRemove.add(zman);
                }
            }
        }
        zmanim.removeAll(zmansToRemove);

        //here is where we actually create the list of zmanim to display
        String[] shortZmanim = new String[zmanim.size()];
        if (mIsZmanimInHebrew) {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isRTZman() && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("RoundUpRT", false)) {
                    DateFormat rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                    shortZmanim[zmanim.indexOf(zman)] = rtFormat.format(zman.getZman()) + ":" + zman.getTitle();
                } else {
                    shortZmanim[zmanim.indexOf(zman)] = zmanimFormat.format(zman.getZman()) + ":" + zman.getTitle()
                            .replace("סוף זמן", "");
                }
                if (zman.getZman().equals(sNextUpcomingZman)) {
                    shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] + "←";
                }
            }
        } else {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isRTZman() && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("RoundUpRT", false)) {
                    DateFormat rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                    shortZmanim[zmanim.indexOf(zman)] = zman.getTitle() + ":" + rtFormat.format(zman.getZman());
                } else {
                    shortZmanim[zmanim.indexOf(zman)] = zman.getTitle()
                            .replace("Earliest ","")
                            .replace("Sof Zman ", "")
                            .replace("Hacochavim", "")
                            .replace("Latest ", "")
                            + ":" + zmanimFormat.format(zman.getZman());
                }
                if (zman.getZman().equals(sNextUpcomingZman)) {
                    shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] + "➤";
                }
            }
        }
        return shortZmanim;
    }

    private void addZmanim(List<ZmanListEntry> zmanim, boolean isForWeeklyZmanim) {
        if (mSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            addAmudeiHoraahZmanim(zmanim, isForWeeklyZmanim);
            return;
        }
        ZmanimNames zmanimNames = new ZmanimNames(mIsZmanimInHebrew, mIsZmanimEnglishTranslated);
        if (sJewishDateInfo.getJewishCalendar().isTaanis()
                && sJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.TISHA_BEAV
                && sJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getAlos72Zmanis(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getAlotString(), mROZmanimCalendar.getAlos72Zmanis(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTalitTefilinString(), mROZmanimCalendar.getEarliestTalitTefilin(), true));
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " " + zmanimNames.getElevatedString(), mROZmanimCalendar.getSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString(), mROZmanimCalendar.getHaNetz(), true));
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaMgaString(), mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaGraString(), mROZmanimCalendar.getSofZmanShmaGRA(), true));
        if (sJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            ZmanListEntry zman = new ZmanListEntry(zmanimNames.getAchilatChametzString(), mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
            zman = new ZmanListEntry(zmanimNames.getBiurChametzString(), mROZmanimCalendar.getSofZmanBiurChametzMGA(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotString(), mROZmanimCalendar.getChatzot(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaGedolaString(), mROZmanimCalendar.getMinchaGedolaGreaterThan30(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaKetanaString(), mROZmanimCalendar.getMinchaKetana(), true));
        String plagOpinions = mSettingsPreferences.getString("plagOpinion", "1");
        if (plagOpinions.equals("1")) {
            zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString(), mROZmanimCalendar.getPlagHaminchaYalkutYosef(), true));
        }
        if (plagOpinions.equals("2")) {
            zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString(), mROZmanimCalendar.getPlagHamincha(), true));
        }
        if (plagOpinions.equals("3")) {
            zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedHalachaBerurahString(),
                    mROZmanimCalendar.getPlagHamincha(), true));
            zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedYalkutYosefString(),
                    mROZmanimCalendar.getPlagHaminchaYalkutYosef(), true));
        }
        if ((sJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !sJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                sJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            ZmanListEntry candleLightingZman = new ZmanListEntry(
                    zmanimNames.getCandleLightingString() + " (" + (int) mROZmanimCalendar.getCandleLightingOffset() + ")",
                    mROZmanimCalendar.getCandleLighting(),
                    true);
            candleLightingZman.setNoteworthyZman(true);
            zmanim.add(candleLightingZman);
        }
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false) && !isForWeeklyZmanim) {
            if (sJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!sJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {//only add if shabbat/yom tov ends tomorrow and not the day after
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            ZmanListEntry endShabbat;
                            if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1")) {
                                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString()
                                        + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaisAteretTorah(), true);
                            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
                            } else {
                                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), true);
                            }
                            endShabbat.setNoteworthyZman(true);
                            zmanim.add(endShabbat);
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString() + zmanimNames.getMacharString(), addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            } else {
                                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzais72Zmanis(), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            }
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
        if (sJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TISHA_BEAV) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getSunset(), true));
        }
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        zmanim.add(new ZmanListEntry(zmanimNames.getSunsetString(), mROZmanimCalendar.getSunset(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString(), mROZmanimCalendar.getTzeit(), true));
        if (sJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                sJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (sJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add(new ZmanListEntry(zmanimNames.getCandleLightingString(), mROZmanimCalendar.getTzeit(), true));
            }
        }
        if (sJewishDateInfo.getJewishCalendar().isTaanis() && sJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            ZmanListEntry fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitTaanit(), true);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
            if (!isForWeeklyZmanim) {//remove the second fast time, it just confuses people
                fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString() + " " + zmanimNames.getLChumraString(), mROZmanimCalendar.getTzaitTaanitLChumra(), true);
                fastEnds.setNoteworthyZman(true);
                zmanim.add(fastEnds);
            }
        } else if (mSettingsPreferences.getBoolean("alwaysShowTzeitLChumra", false)) {
            ZmanListEntry tzeitLChumra = new ZmanListEntry(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), mROZmanimCalendar.getTzaitTaanit(), true);
            zmanim.add(tzeitLChumra);
        }
        if (sJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !sJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            ZmanListEntry endShabbat;
            if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("1")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString()
                        + " (" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")", mROZmanimCalendar.getTzaisAteretTorah(), true);
            } else if (mSettingsPreferences.getString("EndOfShabbatOpinion", "1").equals("2")) {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
            } else {
                endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraahLesserThan40(), true);
            }
            endShabbat.setNoteworthyZman(true);
            zmanim.add(endShabbat);
            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            } else {
                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), mROZmanimCalendar.getTzais72Zmanis(), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            }
            //If it is shabbat/yom tov,we want to dim the tzeit hacochavim zmanim in the GUI
            for (ZmanListEntry zman: zmanim) {
                if (zman.getTitle().equals(zmanimNames.getTzaitHacochavimString()) ||
                        zman.getTitle().equals(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString())) {
                    zman.setShouldBeDimmed(true);
                }
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(sJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !sJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                    ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                } else {
                    ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), mROZmanimCalendar.getTzais72Zmanis(), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                }
            }
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotLaylaString(), mROZmanimCalendar.getSolarMidnight(), true));
    }

    private void addAmudeiHoraahZmanim(List<ZmanListEntry> zmanim, boolean isForWeeklyZmanim) {
        mROZmanimCalendar.setUseElevation(false);
        ZmanimNames zmanimNames = new ZmanimNames(mIsZmanimInHebrew, mIsZmanimEnglishTranslated);
        if (sJewishDateInfo.getJewishCalendar().isTaanis()
                && sJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.TISHA_BEAV
                && sJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getAlotAmudeiHoraah(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getAlotString(), mROZmanimCalendar.getAlotAmudeiHoraah(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTalitTefilinString(), mROZmanimCalendar.getEarliestTalitTefilinAmudeiHoraah(), true));
        if (mSettingsPreferences.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " " + zmanimNames.getElevatedString(), mROZmanimCalendar.getSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString(), mROZmanimCalendar.getHaNetz(), true));
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise" + sCurrentLocationName, true) &&
                mSettingsPreferences.getBoolean("ShowMishorAlways", false)) {
            zmanim.add(new ZmanListEntry(zmanimNames.getHaNetzString() + " (" + zmanimNames.getMishorString() + ")", mROZmanimCalendar.getSeaLevelSunrise(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaMgaString(), mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getShmaGraString(), mROZmanimCalendar.getSofZmanShmaGRA(), true));
        if (sJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            ZmanListEntry zman = new ZmanListEntry(zmanimNames.getAchilatChametzString(), mROZmanimCalendar.getSofZmanAchilatChametzAmudeiHoraah(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
            zman = new ZmanListEntry(zmanimNames.getBiurChametzString(), mROZmanimCalendar.getSofZmanBiurChametzMGAAmudeiHoraah(), true);
            zman.setNoteworthyZman(true);
            zmanim.add(zman);
        } else {
            zmanim.add(new ZmanListEntry(zmanimNames.getBrachotShmaString(), mROZmanimCalendar.getSofZmanTfilaGRA(), true));
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotString(), mROZmanimCalendar.getChatzot(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaGedolaString(), mROZmanimCalendar.getMinchaGedolaGreaterThan30(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getMinchaKetanaString(), mROZmanimCalendar.getMinchaKetana(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedHalachaBerurahString(), mROZmanimCalendar.getPlagHamincha(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getPlagHaminchaString() + " " + zmanimNames.getAbbreviatedYalkutYosefString(), mROZmanimCalendar.getPlagHaminchaYalkutYosefAmudeiHoraah(), true));
        if ((sJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                !sJewishDateInfo.getJewishCalendar().isAssurBemelacha()) ||
                sJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            ZmanListEntry candleLightingZman = new ZmanListEntry(
                    zmanimNames.getCandleLightingString() + " (" + (int) mROZmanimCalendar.getCandleLightingOffset() + ")",
                    mROZmanimCalendar.getCandleLighting(),
                    true);
            candleLightingZman.setNoteworthyZman(true);
            zmanim.add(candleLightingZman);
        }
        if (mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false) && !isForWeeklyZmanim) {
            if (sJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
                if (!sJewishDateInfo.getJewishCalendar().isTomorrowShabbosOrYomTov()) {
                    Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
                    if (stringSet != null) {
                        if (stringSet.contains("Show Regular Minutes")) {
                            zmanim.add(new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true));
                        }
                        if (stringSet.contains("Show Rabbeinu Tam")) {
                            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString() + zmanimNames.getMacharString(), addMinuteToZman(mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah()), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            } else {
                                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString() + zmanimNames.getMacharString(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah(), true);
                                rt.setRTZman(true);
                                zmanim.add(rt);
                            }
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
        if (sJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TISHA_BEAV) {
            zmanim.add(new ZmanListEntry(zmanimNames.getTaanitString() + zmanimNames.getStartsString(), mROZmanimCalendar.getSunset(), true));
        }
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        zmanim.add(new ZmanListEntry(zmanimNames.getSunsetString(), mROZmanimCalendar.getSeaLevelSunset(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString(), mROZmanimCalendar.getTzeitAmudeiHoraah(), true));
        zmanim.add(new ZmanListEntry(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString(), mROZmanimCalendar.getTzeitAmudeiHoraahLChumra(), true));
        if (sJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                sJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (sJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add(new ZmanListEntry(zmanimNames.getCandleLightingString(), mROZmanimCalendar.getTzeitAmudeiHoraah(), true));
            }
        }
        if (sJewishDateInfo.getJewishCalendar().isTaanis() && sJewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
            ZmanListEntry fastEnds = new ZmanListEntry(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzeitAmudeiHoraahLChumra(), true);
            fastEnds.setNoteworthyZman(true);
            zmanim.add(fastEnds);
        }
        if (sJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !sJewishDateInfo.getJewishCalendar().hasCandleLighting()) {
            ZmanListEntry endShabbat = new ZmanListEntry(zmanimNames.getTzaitString() + getShabbatAndOrChag() + zmanimNames.getEndsString(), mROZmanimCalendar.getTzaitShabbatAmudeiHoraah(), true);
            endShabbat.setNoteworthyZman(true);
            zmanim.add(endShabbat);
            if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah()), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            } else {
                ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah(), true);
                rt.setRTZman(true);
                rt.setNoteworthyZman(true);
                zmanim.add(rt);
            }
            //If it is shabbat/yom tov,we want to dim the tzeit hacochavim zmanim in the GUI
            for (ZmanListEntry zman: zmanim) {
                if (zman.getTitle().equals(zmanimNames.getTzaitHacochavimString()) ||
                        zman.getTitle().equals(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString())) {
                    zman.setShouldBeDimmed(true);
                }
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(sJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !sJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", true)) {
                    ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), addMinuteToZman(mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah()), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                } else {
                    ZmanListEntry rt = new ZmanListEntry(zmanimNames.getRTString(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraahLkulah(), true);
                    rt.setRTZman(true);
                    zmanim.add(rt);
                }
            }
        }
        zmanim.add(new ZmanListEntry(zmanimNames.getChatzotLaylaString(), mROZmanimCalendar.getSolarMidnight(), true));
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
     * This is a simple convenience method to check if the current date is on shabbat or yom tov or both and return the correct string.
     * @return a string that says whether it is shabbat and chag or just shabbat or just chag (in Hebrew or English)
     */
    private String getShabbatAndOrChag() {
        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            if (sJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha()
                    && sJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "שבת/חג";
            } else if (sJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "שבת";
            } else {
                return "חג";
            }
        } else {
            if (sJewishDateInfo.getJewishCalendar().isYomTovAssurBemelacha()
                    && sJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat/Chag";
            } else if (sJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "Shabbat";
            } else {
                return "Chag";
            }
        }
    }

    /**
     * This method will check if the tekufa happens within the next 48 hours and it will add the tekufa to the list passed in if it happens
     * on the current date.
     * TODO : Fix timezone issues with tekufa. If you try to see when the tekufa is in a different timezone, it will sometimes not work because the Dates are not the same.
     * @param zmanim the list of zmanim to add to
     * @param shortStyle if the tekufa should be added as "Tekufa Nissan : 4:30" or "Tekufa Nissan is today at 4:30"
     */
    private void addTekufaTime(List<ZmanListEntry> zmanim, boolean shortStyle) {
        DateFormat zmanimFormat;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
        mROZmanimCalendar.getCalendar().add(Calendar.DATE,1);//check next day for tekufa, because the tekufa time can go back a day
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE,-1);//reset the calendar
        if (sJewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), sJewishDateInfo.getJewishCalendar().getTekufaAsDate())) {
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                if (shortStyle) {
                    zmanim.add(new ZmanListEntry("תקופת " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("תקופת " + sJewishDateInfo.getJewishCalendar().getTekufaName() +
                            " היום בשעה " + zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                }
            } else {
                if (shortStyle) {
                    zmanim.add(new ZmanListEntry("Tekufa " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("Tekufa " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                }
            }
        }
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());//reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (sJewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), sJewishDateInfo.getJewishCalendar().getTekufaAsDate())) {
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                if (shortStyle) {
                    zmanim.add(new ZmanListEntry("תקופת " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("תקופת " + sJewishDateInfo.getJewishCalendar().getTekufaName() +
                            " היום בשעה " + zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                }
            } else {
                if (shortStyle) {
                    zmanim.add(new ZmanListEntry("Tekufa " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("Tekufa " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                }
            }
        }
    }

    /**
     * This method will check if the tekufa happens within the next 48 hours and it will add the tekufa to the list passed in if it happens
     * on the current date.
     * TODO : Fix timezone issues with tekufa. If you try to see when the tekufa is in a different timezone, it will sometimes not work because the Dates are not the same.
     * @param zmanim the list of zmanim to add to
     * @param shortStyle if the tekufa should be added as "Tekufa Nissan : 4:30" or "Tekufa Nissan is today at 4:30"
     */
    private void addAmudeiHoraahTekufaTime(List<ZmanListEntry> zmanim, boolean shortStyle) {
        DateFormat zmanimFormat;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
        mROZmanimCalendar.getCalendar().add(Calendar.DATE,1);//check next day for tekufa, because the tekufa time can go back a day
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE,-1);//reset the calendar
        if (sJewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())) {
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                if (shortStyle) {
                    zmanim.add(new ZmanListEntry("תקופת " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("תקופת " + sJewishDateInfo.getJewishCalendar().getTekufaName() +
                            " היום בשעה " + zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                }
            } else {
                if (shortStyle) {
                    zmanim.add(new ZmanListEntry("Tekufa " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("Tekufa " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                }
            }
        }
        sJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());//reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (sJewishDateInfo.getJewishCalendar().getTekufa() != null &&
                DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())) {
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                if (shortStyle) {
                    zmanim.add(new ZmanListEntry("תקופת " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("תקופת " + sJewishDateInfo.getJewishCalendar().getTekufaName() +
                            " היום בשעה " + zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                }
            } else {
                if (shortStyle) {
                    zmanim.add(new ZmanListEntry("Tekufa " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("Tekufa " + sJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                            zmanimFormat.format(sJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                }
            }
        }
    }

    /**
     * This method will check if elevation has been set for the current location and if not, it will automatically set it by connecting to geonames
     * and getting the elevation for the current location via the LocationResolver class. It will then set the elevation in the shared preferences
     * for the current location. If the user is offline, it will not do anything. The only time it will not set the elevation is if the user has
     * disabled the elevation setting in the settings menu.
     *
     * @see LocationResolver#getLocationAsName()
     */
    private void resolveElevationAndVisibleSunrise() {
        if (sCurrentLocationName.contains("Lat:") && sCurrentLocationName.contains("Long:")
                && mSettingsPreferences.getBoolean("SetElevationToLastKnownLocation", false)) {//only if the user has enabled the setting to set the elevation to the last known location
            sUserIsOffline = true;
            mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        } else {//user is online, get the elevation from the shared preferences for the current location
            mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));//get the last value of the current location or 0 if it doesn't exist
        }

        if (!sUserIsOffline && mSharedPreferences.getBoolean("useElevation", true)
        && !mSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {//update if the user is online and the elevation setting is enabled
            if (!mSharedPreferences.contains("elevation" + sCurrentLocationName)) {//if the elevation for this location has never been set
                mLocationResolver = new LocationResolver(this, this);
                mLocationResolver.start();
                try {
                    mLocationResolver.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
                seeIfTablesNeedToBeUpdated(false);
            } else {
                mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
            }
        }

        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            mElevation = 0;
        }
    }

    /**
     * This method will create a new AlertDialog that asks the user to use their location and it
     * will also give the option to enter an address/zipcode through the EditText field.
     */
    private void createZipcodeDialog() {
        final EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setHint(R.string.enter_zipcode_or_address);
        new AlertDialog.Builder(this, R.style.alertDialog)
                .setTitle(R.string.search_for_a_place)
                .setMessage(R.string.warning_zmanim_will_be_based_on_your_approximate_area)
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (input.getText().toString().isEmpty()) {
                        Toast.makeText(this, R.string.please_enter_something, Toast.LENGTH_SHORT).show();
                        createZipcodeDialog();
                    } else {
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putBoolean("useZipcode", true).apply();
                        editor.putString("Zipcode", input.getText().toString()).apply();
                        mLocationResolver = new LocationResolver(this, this);
                        mLocationResolver.getLatitudeAndLongitudeFromSearchQuery();
                        if (mSharedPreferences.getBoolean("useElevation", true)) {
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
                            resolveElevationAndVisibleSunrise();
                            instantiateZmanimCalendar();
                            setNextUpcomingZman();
                            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateViewsInList();
                            }
                            checkIfUserIsInIsraelOrNot();
                        }
                    }
                })
                .setNeutralButton(R.string.use_location, (dialog, which) -> {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean("useZipcode", false).apply();
                    mLocationResolver = new LocationResolver(this, this);
                    mLocationResolver.acquireLatitudeAndLongitude();
                    mLocationResolver.setTimeZoneID();
                    if (mSharedPreferences.getBoolean("useElevation", true)) {
                        mLocationResolver.start();
                        try {
                            mLocationResolver.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    resolveElevationAndVisibleSunrise();
                    instantiateZmanimCalendar();
                    setNextUpcomingZman();
                    if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                        updateWeeklyZmanim();
                    } else {
                        updateViewsInList();
                    }
                    checkIfUserIsInIsraelOrNot();
                })
                .create()
                .show();
    }

    @Override
    public void onBackPressed() {
        if (!mBackHasBeenPressed) {
            mBackHasBeenPressed = true;
            Toast.makeText(this, R.string.press_back_again_to_close_the_app, Toast.LENGTH_SHORT).show();
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.weekly_mode).setChecked(mSharedPreferences.getBoolean("weeklyMode", false));
        menu.findItem(R.id.use_elevation).setChecked(mSharedPreferences.getBoolean("useElevation", true));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.enterZipcode) {
            createZipcodeDialog();
            return true;
        } else if (id == R.id.shabbat_mode) {
            if (!sShabbatMode && sJewishDateInfo != null && mROZmanimCalendar != null && mMainRecyclerView != null) {
                mCurrentDateShown.setTime(new Date());
                sJewishDateInfo.setCalendar(new GregorianCalendar());
                mROZmanimCalendar.setCalendar(new GregorianCalendar());
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateViewsInList();
                }
                startShabbatMode();
                item.setChecked(true);
            } else {
                endShabbatMode();
                item.setChecked(false);
            }
            return true;
        } else if (id == R.id.weekly_mode) {
            mSharedPreferences.edit().putBoolean("weeklyMode", !mSharedPreferences.getBoolean("weeklyMode", false)).apply();
            item.setChecked(mSharedPreferences.getBoolean("weeklyMode", false));//save the state of the menu item
            if (mMainRecyclerView == null) {
                return true;// Prevent a crash
            }
            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                showWeeklyTextViews();
                updateWeeklyZmanim();
            } else {
                hideWeeklyTextViews();
                updateViewsInList();
            }
            return true;
        } else if (id == R.id.use_elevation) {
            item.setChecked(mSharedPreferences.getBoolean("useElevation", false));//save the state of the menu item
            mSharedPreferences.edit().putBoolean("useElevation", !mSharedPreferences.getBoolean("useElevation", false)).apply();
            resolveElevationAndVisibleSunrise();
            instantiateZmanimCalendar();
            setNextUpcomingZman();
            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            } else {
                updateViewsInList();
            }
            return true;
        } else if (id == R.id.molad) {
            startActivity(new Intent(this, MoladActivity.class));
            return true;
        } else if (id == R.id.fullSetup) {
            sSetupLauncher.launch(new Intent(this, FullSetupActivity.class)
                    .putExtra("fromMenu",true));
            return true;
        } else if (id == R.id.settings) {
            sFromSettings = true;
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (id == R.id.website) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.royzmanim.com"));
            startActivity(browserIntent);
            return true;
        } else if (id == R.id.help) {
            new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_DayNight)
                    .setTitle(R.string.help_using_this_app)
                    .setPositiveButton(R.string.ok, null)
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
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }
    }
}