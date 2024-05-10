package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.classes.ZmanimFactory.addZmanim;

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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextClock;
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
import androidx.core.splashscreen.SplashScreen;
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
import com.ej.rovadiahyosefcalendar.classes.WearableCapabilityChecker;
import com.ej.rovadiahyosefcalendar.classes.ZmanAdapter;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory;
import com.ej.rovadiahyosefcalendar.notifications.DailyNotifications;
import com.ej.rovadiahyosefcalendar.notifications.NextZmanCountdownNotification;
import com.ej.rovadiahyosefcalendar.notifications.OmerNotifications;
import com.ej.rovadiahyosefcalendar.notifications.ZmanimNotifications;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.kosherjava.zmanim.hebrewcalendar.Daf;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;
import com.kosherjava.zmanim.util.GeoLocation;
import com.kosherjava.zmanim.util.ZmanimFormatter;

import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static boolean sShabbatMode;
    public static boolean sUserIsOffline;
    private boolean mIsZmanimInHebrew;
    private boolean mIsZmanimEnglishTranslated;
    private boolean mBackHasBeenPressed = false;
    private boolean mUpdateTablesDialogShown;
    private boolean mInitialized = false;
    private int mCurrentPosition;//current position in the list of zmanim to return to
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
    private JewishDateInfo mJewishDateInfo;
    private final HebrewDateFormatter mHebrewDateFormatter = new HebrewDateFormatter();
    private final ZmanimFormatter mZmanimFormatter = new ZmanimFormatter(TimeZone.getDefault());

    //android classes:
    private Handler mHandler = null;
    private Runnable mZmanimUpdater;
    private GestureDetector mGestureDetector;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsPreferences;
    public static final String SHARED_PREF = "MyPrefsFile";
    public static ActivityResultLauncher<Intent> sSetupLauncher;
    public static ActivityResultLauncher<Intent> sNotificationLauncher;

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
    private Date mLastTimeUserWasInApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        if (getActionBar() != null) {// only for emulator
            getActionBar().hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_custom);//center the title
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        mHebrewDateFormatter.setUseGershGershayim(false);
        mLayout = findViewById(R.id.main_layout);
        mHandler = new Handler(getMainLooper());
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String lang = mSettingsPreferences.getString("language", "Default");
        if (!lang.equals("Default")) {
            if (!Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals(lang)) {
                switch (lang) {
                    case "English":
                        Locale locale = new Locale("en", "US");
                        Locale.setDefault(locale);
                        setLocale(locale);
                        break;
                    case "Hebrew":
                        Locale helocale = new Locale("he", "IL");
                        Locale.setDefault(helocale);
                        setLocale(helocale);
                        break;
                    default:
                        break;
                }
            }
        }
        mGestureDetector = new GestureDetector(MainActivity.this, new ZmanimGestureListener());
        mZmanimFormatter.setTimeFormat(ZmanimFormatter.SEXAGESIMAL_FORMAT);
        initSetupResult();
        initNotifResult();
        setupShabbatModeBanner();
        mLocationResolver = new LocationResolver(this, this);
        mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
        if (ChaiTables.visibleSunriseFileDoesNotExist(getExternalFilesDir(null), sCurrentLocationName, mJewishDateInfo.getJewishCalendar())
                && mSharedPreferences.getBoolean("UseTable" + sCurrentLocationName, true)
                && !mSharedPreferences.getBoolean("isSetup", false)
                && savedInstanceState == null) {//it should only not exist the first time running the app and only if the user has not set up the app
            sSetupLauncher.launch(new Intent(this, FullSetupActivity.class));
            initZmanimNotificationDefaults();
        } else {
            mLocationResolver.acquireLatitudeAndLongitude();
        }
        findAllWeeklyViews();
        if ((!mInitialized && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) || mSharedPreferences.getBoolean("useZipcode", false)) {
            initMainView();
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // Update UI to reflect text being shared
                    // We set the zipcode location to what was sent to us
                    mSharedPreferences.edit().putBoolean("useLocation1", false)
                            .putBoolean("useLocation2", false)
                            .putBoolean("useLocation3", false)
                            .putBoolean("useLocation4", false)
                            .putBoolean("useLocation5", false)
                            .apply();
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean("useAdvanced", false).apply();
                    editor.putBoolean("useZipcode", true).apply();
                    editor.putString("Zipcode", sharedText).apply();
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
                    mLocationResolver.setTimeZoneID();
                    resolveElevationAndVisibleSunrise();
                    instantiateZmanimCalendar();
                    setNextUpcomingZman();
                    if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                        updateWeeklyZmanim();
                    } else {
                        updateDailyZmanim();
                    }
                    checkIfUserIsInIsraelOrNot();
                    saveGeoLocationInfo();
                    setNotifications();
                    sendPreferencesToWatch();
                }
            }
        }
    }

    public void setLocale(Locale locale) {
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.setLocale(locale);
        res.updateConfiguration(conf, dm);// not perfect
        conf.setLayoutDirection(locale);
        recreate();
    }

    private void initZmanimNotificationDefaults() {
        mSettingsPreferences.edit().putBoolean("zmanim_notifications", true).apply();
        mSettingsPreferences.edit().putInt("autoDismissNotifications", -1).apply();
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
                        updateDailyZmanim();
                    }

//                    if (!mInitialized) {
//                        initMainView();
//                    } else {
//                        mLocationResolver.setTimeZoneID();
//                        resolveElevationAndVisibleSunrise();
//                        instantiateZmanimCalendar();
//                        setNextUpcomingZman();
//                        if (mSharedPreferences.getBoolean("weeklyMode", false)) {
//                            updateWeeklyZmanim();
//                        } else {
//                            updateDailyZmanim();
//                        }
//                        checkIfUserIsInIsraelOrNot();
//                        saveGeoLocationInfo();
//                        setNotifications();
//                        sendPreferencesToWatch();
//                    }
                }
        );
    }

    private void initNotifResult() {
        sNotificationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> setNotifications()
        );
    }

    private void showWeeklyTextViews() {
        LinearLayout mainWeekly = findViewById(R.id.main_weekly_layout);

        mEnglishMonthYear.setVisibility(View.VISIBLE);
        mLocationName.setVisibility(View.VISIBLE);
        mHebrewMonthYear.setVisibility(View.VISIBLE);
        mainWeekly.setVisibility(View.VISIBLE);
        mWeeklyParsha.setVisibility(View.VISIBLE);
        mWeeklyDafs.setVisibility(View.VISIBLE);
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
        mLocationName = findViewById(R.id.location_name);
        mEnglishMonthYear = findViewById(R.id.englishMonthYear);
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

        mWeeklyDafs = findViewById(R.id.weeklyDafs);
        mWeeklyParsha = findViewById(R.id.weeklyParsha);
        updateWeeklyTextViewTextColor();
    }
    
    private void updateWeeklyTextViewTextColor() {
        if (mSharedPreferences.getBoolean("customTextColor", false)) {
            int textColor = mSharedPreferences.getInt("tColor", 0xFFFFFFFF);
            mLocationName.setTextColor(textColor);
            mEnglishMonthYear.setTextColor(textColor);
            mHebrewMonthYear.setTextColor(textColor);
            //there are 7 of these sets of views
            mSunday[1].setTextColor(textColor);
            mSunday[2].setTextColor(textColor);
            mSunday[3].setTextColor(textColor);
            mSunday[4].setTextColor(textColor);
            mSunday[5].setTextColor(textColor);

            mMonday[1].setTextColor(textColor);
            mMonday[2].setTextColor(textColor);
            mMonday[3].setTextColor(textColor);
            mMonday[4].setTextColor(textColor);
            mMonday[5].setTextColor(textColor);

            mTuesday[1].setTextColor(textColor);
            mTuesday[2].setTextColor(textColor);
            mTuesday[3].setTextColor(textColor);
            mTuesday[4].setTextColor(textColor);
            mTuesday[5].setTextColor(textColor);

            mWednesday[1].setTextColor(textColor);
            mWednesday[2].setTextColor(textColor);
            mWednesday[3].setTextColor(textColor);
            mWednesday[4].setTextColor(textColor);
            mWednesday[5].setTextColor(textColor);

            mThursday[1].setTextColor(textColor);
            mThursday[2].setTextColor(textColor);
            mThursday[3].setTextColor(textColor);
            mThursday[4].setTextColor(textColor);
            mThursday[5].setTextColor(textColor);

            mFriday[1].setTextColor(textColor);
            mFriday[2].setTextColor(textColor);
            mFriday[3].setTextColor(textColor);
            mFriday[4].setTextColor(textColor);
            mFriday[5].setTextColor(textColor);

            mSaturday[1].setTextColor(textColor);
            mSaturday[2].setTextColor(textColor);
            mSaturday[3].setTextColor(textColor);
            mSaturday[4].setTextColor(textColor);
            mSaturday[5].setTextColor(textColor);

            mWeeklyDafs.setTextColor(textColor);
            mWeeklyParsha.setTextColor(textColor);
        }
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

    private void sendPreferencesToWatch() {
        WearableCapabilityChecker wearableCapabilityChecker = new WearableCapabilityChecker(this);
        wearableCapabilityChecker.checkIfWatchExists(hasWatch -> {
            if (hasWatch) {
                new Thread(() -> {
                    // Get the connected nodes (user may have multiple watches) on the Wear network
                    Task<List<Node>> nodeListTask = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
                    try {
                        List<Node> nodes = Tasks.await(nodeListTask);
                        for (Node node : nodes) {
                            // Build the message
                            JSONObject jsonPreferences = getJSONPreferencesObject();
                            String message = jsonPreferences.toString();
                            byte[] payload = message.getBytes(StandardCharsets.UTF_8); // use UTF-8 since each ASCII character will be 1 byte

                            // Send the message
                            Task<Integer> sendMessageTask =
                                    Wearable.getMessageClient(getApplicationContext())
                                            .sendMessage(node.getId(), "prefs/", payload);

                            // Add onCompleteListener to check if the message was successfully sent
                            sendMessageTask.addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    int result = task.getResult();
                                    Log.d("From main app", "Message sent to " + node.getDisplayName() + ". Result: " + result + ". message: " +message);
                                } else {
                                    Exception exception = task.getException();
                                    Log.e("From main app", "Failed to send message to watch: " + exception);
                                }
                            });

                            try {
                                ChaiTables chaiTables = new ChaiTables(getExternalFilesDir(null), sCurrentLocationName, mJewishDateInfo.getJewishCalendar());
                                String chaiTableForThisYear = chaiTables.getFullChaiTable();
                                byte[] chaiTablePayload = chaiTableForThisYear.getBytes(StandardCharsets.UTF_8); // use UTF-8 since each ASCII character will be 1 byte

                                Task<Integer> sendChaiTablesTask =
                                        Wearable.getMessageClient(getApplicationContext())
                                                .sendMessage(node.getId(), "chaiTable/", chaiTablePayload);

                                sendChaiTablesTask.addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        int result = task.getResult();
                                        Log.d("From main app", "chaiTable sent to " + node.getDisplayName() + ". Result: " + result + ". message: " + message);
                                    } else {
                                        Exception exception = task.getException();
                                        Log.e("From main app", "Failed to send chaiTable to watch: " + exception);
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("From main app", "Failed to build ChaiTables object, file is probably missing");
                                e.printStackTrace();
                            }
                        }
                    } catch (ExecutionException | InterruptedException | JSONException exception) {
                        Log.e("From main app", "Failed to send message to watch: " + exception);
                    }
                }).start();
            }
        });
    }

    private JSONObject getJSONPreferencesObject() throws JSONException {
        // We need to be careful to remember where the preferences are in either Settings Preferences or Shared Preferences
        JSONObject jsonObject = new JSONObject().put("useElevation", mSharedPreferences.getBoolean("useElevation", false))
                .put("ShowSeconds", mSettingsPreferences.getBoolean("ShowSeconds", false))
                .put("ShowElevation", mSettingsPreferences.getBoolean("ShowElevation", false))
                .put("ShowElevatedSunrise", mSettingsPreferences.getBoolean("ShowElevatedSunrise", false))
                .put("inIsrael", mSharedPreferences.getBoolean("inIsrael", false))
                .put("tekufaOpinions", mSettingsPreferences.getString("tekufaOpinions", "1"))
                .put("RoundUpRT", mSettingsPreferences.getBoolean("RoundUpRT", false))
                .put("showShabbatMevarchim", mSettingsPreferences.getBoolean("showShabbatMevarchim", false))
                .put("LuachAmudeiHoraah", mSettingsPreferences.getBoolean("LuachAmudeiHoraah", false))
                .put("isZmanimInHebrew", mSharedPreferences.getBoolean("isZmanimInHebrew", false))
                .put("isZmanimEnglishTranslated", mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false))
                .put("ShowMishorAlways", mSettingsPreferences.getBoolean("ShowMishorAlways", false))
                .put("plagOpinion", mSettingsPreferences.getString("plagOpinion", "1"))
                .put("CandleLightingOffset", mSettingsPreferences.getString("CandleLightingOffset", "20"))
                .put("ShowWhenShabbatChagEnds", mSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false));
        if (jsonObject.getBoolean("ShowWhenShabbatChagEnds")) {
            Set<String> stringSet = mSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
            if (stringSet != null) {
                jsonObject.put("Show Regular Minutes", stringSet.contains("Show Regular Minutes"))
                        .put("Show Rabbeinu Tam", stringSet.contains("Show Rabbeinu Tam"));
            }
        }
        jsonObject.put("EndOfShabbatOffset", mSettingsPreferences.getString("EndOfShabbatOffset", "40"))
                .put("EndOfShabbatOpinion", mSettingsPreferences.getString("EndOfShabbatOpinion", "1"))
                .put("alwaysShowTzeitLChumra", mSettingsPreferences.getBoolean("alwaysShowTzeitLChumra", false))
                .put("AlwaysShowRT", mSettingsPreferences.getBoolean("AlwaysShowRT", false))
                .put("useZipcode", mSharedPreferences.getBoolean("useZipcode", false))
                .put("Zipcode", mSharedPreferences.getString("Zipcode", ""))
                .put("oldZipcode", mSharedPreferences.getString("oldZipcode", "None"))
                .put("oldLocationName", mSharedPreferences.getString("oldLocationName", ""))
                .put("oldLat", mSharedPreferences.getLong("oldLat", 0))
                .put("oldLong", mSharedPreferences.getLong("oldLong", 0))
                .put("locationName", sCurrentLocationName) // needed because we are not sure if the watches current location is the same as the app's
                .put("elevation" + sCurrentLocationName, mSharedPreferences.getString("elevation" + sCurrentLocationName, "0"))
                .put("SetElevationToLastKnownLocation", mSettingsPreferences.getBoolean("SetElevationToLastKnownLocation", false))

                .put("currentLN", sCurrentLocationName)
                .put("currentLat", String.valueOf(sLatitude))
                .put("currentLong", String.valueOf(sLongitude))
                .put("currentTimezone", sCurrentTimeZoneID)

                .put("useAdvanced", mSharedPreferences.getBoolean("useAdvanced", false))
                .put("advancedLN", mSharedPreferences.getString("advancedLN", ""))
                .put("advancedLat", mSharedPreferences.getString("advancedLat", "0"))
                .put("advancedLong", mSharedPreferences.getString("advancedLong", "0"))
                .put("advancedTimezone", mSharedPreferences.getString("advancedTimezone", ""))

                .put("useLocation1", mSharedPreferences.getBoolean("useLocation1", false))
                .put("location1", mSharedPreferences.getString("location1", ""))
                .put("location1Lat", mSharedPreferences.getLong("location1Lat", 0))
                .put("location1Long", mSharedPreferences.getLong("location1Long", 0))
                .put("location1Timezone", mSharedPreferences.getString("location1Timezone", ""))

                .put("useLocation2", mSharedPreferences.getBoolean("useLocation2", false))
                .put("location2", mSharedPreferences.getString("location2", ""))
                .put("location2Lat", mSharedPreferences.getLong("location2Lat", 0))
                .put("location2Long", mSharedPreferences.getLong("location2Long", 0))
                .put("location2Timezone", mSharedPreferences.getString("location2Timezone", ""))

                .put("useLocation3", mSharedPreferences.getBoolean("useLocation3", false))
                .put("location3", mSharedPreferences.getString("location3", ""))
                .put("location3Lat", mSharedPreferences.getLong("location3Lat", 0))
                .put("location3Long", mSharedPreferences.getLong("location3Long", 0))
                .put("location3Timezone", mSharedPreferences.getString("location3Timezone", ""))

                .put("useLocation4", mSharedPreferences.getBoolean("useLocation4", false))
                .put("location4", mSharedPreferences.getString("location4", ""))
                .put("location4Lat", mSharedPreferences.getLong("location4Lat", 0))
                .put("location4Long", mSharedPreferences.getLong("location4Long", 0))
                .put("location4Timezone", mSharedPreferences.getString("location4Timezone", ""))

                .put("useLocation5", mSharedPreferences.getBoolean("useLocation5", false))
                .put("location5", mSharedPreferences.getString("location5", ""))
                .put("location5Lat", mSharedPreferences.getLong("location5Lat", 0))
                .put("location5Long", mSharedPreferences.getLong("location5Long", 0))
                .put("location5Timezone", mSharedPreferences.getString("location5Timezone", ""));

        return jsonObject;
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
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                if (ActivityCompat.checkSelfPermission(this, ACCESS_BACKGROUND_LOCATION) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{ACCESS_BACKGROUND_LOCATION}, 1);
                }
                mSharedPreferences.edit().putBoolean("askedForRealtimeNotifications", true).apply();
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> {
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
                            mJewishDateInfo = new JewishDateInfo(true, true);
                            Toast.makeText(this, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateDailyZmanim();
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
                            mJewishDateInfo = new JewishDateInfo(false, true);
                            Toast.makeText(this, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateDailyZmanim();
                            }
                            startActivity(new Intent(this, CalendarChooserActivity.class));
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

            if (ChaiTables.visibleSunriseFileDoesNotExist(getExternalFilesDir(null), sCurrentLocationName, mJewishDateInfo.getJewishCalendar())) {
                if (!mUpdateTablesDialogShown) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.alertDialog);
                    builder.setTitle(R.string.update_tables);
                    builder.setMessage(R.string.the_visible_sunrise_tables_for_the_current_location_and_year_need_to_be_updated_do_you_want_to_update_the_tables_now);
                    builder.setPositiveButton(R.string.yes, (dialog, which) -> {
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
                            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateDailyZmanim();
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
        String candles = mSettingsPreferences.getString("CandleLightingOffset", "20");
        if (candles.isEmpty()) {
            candles = "20";
        }
        mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(candles));
        String shabbat = mSettingsPreferences.getString("EndOfShabbatOffset", mSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40");
        if (shabbat.isEmpty()) {// for some reason this is happening
            shabbat = "40";
        }
        mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(shabbat));
        if (mSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
            mROZmanimCalendar.setAteretTorahSunsetOffset(30);
        }
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
                    && mJewishDateInfo != null
                    && mROZmanimCalendar != null
                    && mMainRecyclerView != null) {
                mCurrentDateShown.setTime(new Date());
                mJewishDateInfo.setCalendar(new GregorianCalendar());
                resolveElevationAndVisibleSunrise();
                instantiateZmanimCalendar();
                setNextUpcomingZman();
                mSharedPreferences.edit().putString("Full"+mROZmanimCalendar.getGeoLocation().getLocationName(), "").apply();
                runOnUiThread(this::updateDailyZmanim);
                runOnUiThread(() -> mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable()));
            }
            swipeRefreshLayout.setRefreshing(false);
            Objects.requireNonNull(Looper.myLooper()).quit();
        }).start());
        mMainRecyclerView = findViewById(R.id.mainRV);
        if (mMainRecyclerView != null) {
            mMainRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mMainRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
            mMainRecyclerView.setOnTouchListener((view, motionEvent) -> mGestureDetector.onTouchEvent(motionEvent));
            if (mIsZmanimInHebrew) {
                mMainRecyclerView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            }
        }
        mLayout.setOnTouchListener((view, motionEvent) -> mGestureDetector.onTouchEvent(motionEvent));
        if (mSharedPreferences.getBoolean("weeklyMode", false)) {
            showWeeklyTextViews();
            updateWeeklyZmanim();
        } else {
            hideWeeklyTextViews();
            updateDailyZmanim();
        }
    }

    private void updateDailyZmanim() {
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
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
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
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
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
            mJewishDateInfo.setCalendar(mUserChosenDate);
            mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            } else {
                updateDailyZmanim();
            }
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
            seeIfTablesNeedToBeUpdated(true);
        };

        return new CustomDatePickerDialog(this, onDateSetListener,
                mROZmanimCalendar.getCalendar().get(Calendar.YEAR),
                mROZmanimCalendar.getCalendar().get(Calendar.MONTH),
                mROZmanimCalendar.getCalendar().get(Calendar.DAY_OF_MONTH),
                mJewishDateInfo.getJewishCalendar());
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
        if (mSharedPreferences.getBoolean("shouldRefresh", false)) {
            mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
            mJewishDateInfo.setCalendar(mCurrentDateShown);
            setZmanimLanguageBools();
            resolveElevationAndVisibleSunrise();
            instantiateZmanimCalendar();
            mROZmanimCalendar.setCalendar(mCurrentDateShown);
            setNextUpcomingZman();
            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyTextViewTextColor();
                updateWeeklyZmanim();
            } else {
                updateDailyZmanim();
                mMainRecyclerView.scrollToPosition(mCurrentPosition);
            }
            resetTheme();
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
            checkIfUserIsInIsraelOrNot();
            mSharedPreferences.edit().putBoolean("shouldRefresh", false).apply();
        }

        TextClock clock = Objects.requireNonNull(getSupportActionBar()).getCustomView().findViewById(R.id.clock);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            clock.setVisibility(View.VISIBLE);
            if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                clock.setFormat24Hour("H:mm:ss");
            }
        } else {
            if (!sShabbatMode) {
                clock.setVisibility(View.GONE);
            }
        }

        if (mSharedPreferences.getBoolean("useImage", false)) {
            Bitmap bitmap = BitmapFactory.decodeFile(mSharedPreferences.getString("imageLocation", ""));
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            mLayout.setBackground(drawable);
        } else if (mSharedPreferences.getBoolean("customBackgroundColor", false) &&
                !mSharedPreferences.getBoolean("useDefaultBackgroundColor", false)) {
            mLayout.setBackgroundColor(mSharedPreferences.getInt("bColor", 0x32312C));
        }
        if (!sShabbatMode) {
            if (mSharedPreferences.getBoolean("useDefaultCalButtonColor", true)) {
                mCalendarButton.setBackgroundColor(getColor(R.color.dark_blue));
            } else {
                mCalendarButton.setBackgroundColor(mSharedPreferences.getInt("CalButtonColor", 0x18267C));
            }
        }
        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());

        if (!DateUtils.isSameDay(mCurrentDateShown.getTime(), new Date())
                && (new Date().getTime() - mLastTimeUserWasInApp.getTime()) > 7_200_000) {//two hours
            mCurrentDateShown.setTime(new Date());
            mROZmanimCalendar.setCalendar(mCurrentDateShown); // no need to check for null pointers
            mJewishDateInfo.setCalendar(mCurrentDateShown);
            if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            } else {
                updateDailyZmanim();
                mMainRecyclerView.scrollToPosition(mCurrentPosition);
            }
        }
        mLastTimeUserWasInApp = new Date();

        if (mSettingsPreferences.getBoolean("showNextZmanNotification", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, NextZmanCountdownNotification.class));
            } else {
                startService(new Intent(this, NextZmanCountdownNotification.class));
            }
        }

        sendPreferencesToWatch();

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
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.SCHEDULE_EXACT_ALARM}, 1);// if the user
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !((AlarmManager) getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()) {// more annoying android permission garbage
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.alertDialog);
                builder.setTitle(R.string.zmanim_notifications_will_not_work);
                builder.setMessage(R.string.if_you_would_like_to_receive_zmanim_notifications);
                builder.setCancelable(false);
                builder.setPositiveButton(getString(R.string.yes), (dialog, which) -> sNotificationLauncher.launch(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+ getPackageName()))));
                builder.setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss());
                builder.show();
            }
        }
        setAllNotifications();
    }

    private void setAllNotifications() {
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

        Intent zmanIntent = new Intent(getApplicationContext(), ZmanimNotifications.class);
        PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),0,zmanIntent,PendingIntent.FLAG_IMMUTABLE);
        try {
            zmanimPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
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
    @SuppressLint("SourceLockedOrientationActivity")
    private void startShabbatMode() {
        if (!sShabbatMode) {
            sShabbatMode = true;
            setShabbatBannerColors(true);
            mShabbatModeBanner.setVisibility(View.VISIBLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
            TextClock clock = Objects.requireNonNull(getSupportActionBar()).getCustomView().findViewById(R.id.clock);
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                clock.setVisibility(View.VISIBLE);
                TextView title = getSupportActionBar().getCustomView().findViewById(R.id.appCompatTextView);
                title.setText(getString(R.string.short_app_name));
                if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                    clock.setFormat24Hour("hh:mm:ss");
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
                mJewishDateInfo.setCalendar(calendar);
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
                setShabbatBannerColors(false);
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
                }
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
            mJewishDateInfo.setCalendar(mCurrentDateShown);
        }

        boolean isShabbat = mCurrentDateShown.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;

        StringBuilder sb = new StringBuilder();

        switch (mJewishDateInfo.getJewishCalendar().getYomTovIndex()) {
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
            mJewishDateInfo.setCalendar(mCurrentDateShown);
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
            TextClock clock = Objects.requireNonNull(getSupportActionBar()).getCustomView().findViewById(R.id.clock);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                clock.setVisibility(View.GONE);
                TextView title = getSupportActionBar().getCustomView().findViewById(R.id.appCompatTextView);
                title.setText(getString(R.string.app_name));
            }
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

        String locationName = mSharedPreferences.getString("Full"+mROZmanimCalendar.getGeoLocation().getLocationName(), "");
        if (locationName.isEmpty()) {
            locationName = mLocationResolver.getFullLocationName();
            mSharedPreferences.edit().putString("Full"+mROZmanimCalendar.getGeoLocation().getLocationName(), locationName).apply();
        }
        if (locationName.isEmpty()) {//if it's still empty, use backup
            locationName = mROZmanimCalendar.getGeoLocation().getLocationName();
        }
        zmanim.add(new ZmanListEntry(locationName));

        StringBuilder sb = new StringBuilder();

        sb.append(mROZmanimCalendar.getCalendar().get(Calendar.DATE));
        sb.append(" ");
        sb.append(mROZmanimCalendar.getCalendar().getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
        sb.append(", ");
        sb.append(mROZmanimCalendar.getCalendar().get(Calendar.YEAR));

        if (DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), new Date())) {
            sb.append("   ▼   ");//add a down arrow to indicate that this is the current day
        } else {
            sb.append("      ");
        }

        sb.append(mJewishDateInfo.getJewishCalendar().toString()
                .replace("Teves", "Tevet")
                .replace("Tishrei", "Tishri"));

        zmanim.add(new ZmanListEntry(sb.toString()));

        zmanim.add(new ZmanListEntry(mJewishDateInfo.getThisWeeksParsha()));

        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        if (mSettingsPreferences.getBoolean("showShabbatMevarchim", true)) {
            if (mJewishDateInfo.getJewishCalendar().isShabbosMevorchim()) {
                zmanim.add(new ZmanListEntry("שבת מברכים"));
            }
        }
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());//reset

        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            zmanim.add(new ZmanListEntry(mROZmanimCalendar.getCalendar()
                    .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())));
        } else {
            zmanim.add(new ZmanListEntry(mROZmanimCalendar.getCalendar()
                    .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                    + " / " +
                    mROZmanimCalendar.getCalendar()
                            .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, new Locale("he","IL"))));
        }

        String day = mJewishDateInfo.getSpecialDay(true);
        if (!day.isEmpty()) {
            zmanim.add(new ZmanListEntry(day));
        }

        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.ROSH_HASHANA &&
                mJewishDateInfo.isShmitaYear()) {
            zmanim.add(new ZmanListEntry(getString(R.string.this_year_is_a_shmita_year)));
        }

        if (mJewishDateInfo.is3Weeks()) {
            if (mJewishDateInfo.is9Days()) {
                if (mJewishDateInfo.isShevuahShechalBo()) {
                    zmanim.add(new ZmanListEntry(getString(R.string.shevuah_shechal_bo)));
                } else {
                    zmanim.add(new ZmanListEntry(getString(R.string.nine_days)));
                }
            } else {
                zmanim.add(new ZmanListEntry(getString(R.string.three_weeks)));
            }
        }

        String isOKToListenToMusic = mJewishDateInfo.isOKToListenToMusic();
        if (!isOKToListenToMusic.isEmpty()) {
            zmanim.add(new ZmanListEntry(isOKToListenToMusic));
        }

        String hallel = mJewishDateInfo.getHallelOrChatziHallel();
        if (!hallel.isEmpty()) {
            zmanim.add(new ZmanListEntry(hallel));
        }

        String ulChaparatPesha = mJewishDateInfo.getIsUlChaparatPeshaSaid();
        if (!ulChaparatPesha.isEmpty()) {
            zmanim.add(new ZmanListEntry(ulChaparatPesha));
        }

        zmanim.add(new ZmanListEntry(mJewishDateInfo.getIsTachanunSaid()));

        String birchatLevana = mJewishDateInfo.getBirchatLevana();
        if (!birchatLevana.isEmpty()) {
            zmanim.add(new ZmanListEntry(birchatLevana));
        }

        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
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

        addZmanim(zmanim, false, mSettingsPreferences, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated);

        if (!mCurrentDateShown.before(dafYomiStartDate)) {
            zmanim.add(new ZmanListEntry(getString(R.string.daf_yomi)  + " " + YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechta()
                    + " " +
                    mHebrewDateFormatter.formatHebrewNumber(YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf()),
                    mJewishDateInfo.getJewishCalendar().getGregorianCalendar().getTime(),false));
        }
        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            Daf dafYomiYerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar());
            if (dafYomiYerushalmi != null) {
                String masechta = dafYomiYerushalmi.getYerushalmiMasechta();
                String daf = mHebrewDateFormatter.formatHebrewNumber(dafYomiYerushalmi.getDaf());
                zmanim.add(new ZmanListEntry(getString(R.string.yerushalmi_yomi) + " " + masechta + " " + daf));
            } else {
                zmanim.add(new ZmanListEntry(getString(R.string.no_daf_yomi_yerushalmi)));
            }
        }

        zmanim.add(new ZmanListEntry(mJewishDateInfo.getIsMashivHaruchOrMoridHatalSaid()
                + " / "
                + mJewishDateInfo.getIsBarcheinuOrBarechAleinuSaid()));

        if (!sShabbatMode) {
            ZmanListEntry siddurEntry = new ZmanListEntry(getString(R.string.show_siddur),
                    mJewishDateInfo.getJewishCalendar().getGregorianCalendar().getTime(),
                    false);
            if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TU_BESHVAT ||
                    (mJewishDateInfo.getJewishCalendar().getUpcomingParshah() == JewishCalendar.Parsha.BESHALACH &&
                    mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.TUESDAY)) {// if disclaimers will be shown in siddur
                siddurEntry.setTitle(getString(R.string.show_siddur) + "*");
            }
            zmanim.add(siddurEntry);
        }

        if (!mSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            zmanim.add(new ZmanListEntry(getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanisGra())
                    + " " + getString(R.string.mg_a) + " " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanis72MinutesZmanis())));
        } else {
            long shaahZmanitMGA = mROZmanimCalendar.getTemporalHour(mROZmanimCalendar.getAlotAmudeiHoraah(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraah());
            zmanim.add(new ZmanListEntry(getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanisGra())
                    + " " + getString(R.string.mg_a) + " " + mZmanimFormatter.format(shaahZmanitMGA)));
        }

        if (mSettingsPreferences.getBoolean("ShowLeapYear", false)) {
            zmanim.add(new ZmanListEntry(mJewishDateInfo.isJewishLeapYear()));
        }

        if (mSettingsPreferences.getBoolean("ShowDST", false)) {
            if (mROZmanimCalendar.getGeoLocation().getTimeZone().inDaylightTime(mROZmanimCalendar.getSeaLevelSunrise())) {
                zmanim.add(new ZmanListEntry(getString(R.string.daylight_savings_time_is_on)));
            } else {
                zmanim.add(new ZmanListEntry(getString(R.string.daylight_savings_time_is_off)));
            }
        }

        if (mSettingsPreferences.getBoolean("ShowShmitaYear", false)) {
            if (mJewishDateInfo.isShmitaYear()) {
                zmanim.add(new ZmanListEntry(getString(R.string.this_year_is_a_shmita_year)));
            } else {
                zmanim.add(new ZmanListEntry(getString(R.string.this_year_is_not_a_shmita_year)));
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
                updateDailyZmanim();
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
        sNextUpcomingZman = ZmanimFactory.getNextUpcomingZman(mCurrentDateShown, mROZmanimCalendar, mJewishDateInfo, mSettingsPreferences, mSharedPreferences, mIsZmanimInHebrew, mIsZmanimEnglishTranslated).getZman();
    }

    private String getAnnouncements() {
        StringBuilder announcements = new StringBuilder();

        String day = mJewishDateInfo.getSpecialDay(true);
        if (!day.isEmpty()) {
            announcements.append(day.replace("/ ","\n")).append("\n");
        }

        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        if (mSettingsPreferences.getBoolean("showShabbatMevarchim", true)) {
            if (mJewishDateInfo.getJewishCalendar().isShabbosMevorchim()) {
                announcements.append("שבת מברכים").append("\n");
            }
        }
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());//reset

        String isOKToListenToMusic = mJewishDateInfo.isOKToListenToMusic();
        if (!isOKToListenToMusic.isEmpty()) {
            announcements.append(isOKToListenToMusic).append("\n");
        }

        String ulChaparatPesha = mJewishDateInfo.getIsUlChaparatPeshaSaid();
        if (!ulChaparatPesha.isEmpty()) {
            announcements.append(ulChaparatPesha).append("\n");
        }

        String hallel = mJewishDateInfo.getHallelOrChatziHallel();
        if (!hallel.isEmpty()) {
            announcements.append(hallel).append("\n");
        }

        TefilaRules tefilaRules = new TefilaRules();

        if (tefilaRules.isMashivHaruachEndDate(mJewishDateInfo.getJewishCalendar())) {
            announcements.append("מוריד הטל/ברכנו").append("\n");
        }

        if (tefilaRules.isMashivHaruachStartDate(mJewishDateInfo.getJewishCalendar())) {
            announcements.append("משיב הרוח").append("\n");
        }

        if (tefilaRules.isVeseinTalUmatarStartDate(mJewishDateInfo.getJewishCalendar())) {
            announcements.append("ברך עלינו").append("\n");
        }

        String tachanun = mJewishDateInfo.getIsTachanunSaid();
        if (!tachanun.equals(getString(R.string.there_is_tachanun_today))) {
            announcements.append(tachanun).append("\n");
        }

        String birchatLevana = mJewishDateInfo.getBirchatLevana();
        if (!birchatLevana.isEmpty() && !birchatLevana.contains("until") && !birchatLevana.contains("עד")) {
            announcements.append(birchatLevana).append("\n");
        }

        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
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
            if (YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar()) == null) {
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
        mJewishDateInfo.setCalendar(mCurrentDateShown);

        HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            hebrewDateFormatter.setHebrewFormat(true);
        }
        List<TextView[]> weeklyInfo = Arrays.asList(mSunday, mMonday, mTuesday, mWednesday, mThursday, mFriday, mSaturday);

        String month = mCurrentDateShown.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        String year = String.valueOf(mCurrentDateShown.get(Calendar.YEAR));

        String hebrewMonth = hebrewDateFormatter.formatMonth(mJewishDateInfo.getJewishCalendar())
                .replace("Tishrei", "Tishri")
                .replace("Teves", "Tevet");
        String hebrewYear = String.valueOf(mJewishDateInfo.getJewishCalendar().getJewishYear());
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            hebrewYear = hebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getJewishYear());
        }

        String masechta = "";
        String daf = "";
        Daf yerushalmi;
        String yerushalmiMasechta = "";
        String yerushalmiDaf = "";

        if (!mCurrentDateShown.before(dafYomiStartDate)) {
            masechta = YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechta();
            daf = mHebrewDateFormatter.formatHebrewNumber(YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf());
        }
        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            yerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar());
            if (yerushalmi != null) {
                yerushalmiMasechta = yerushalmi.getYerushalmiMasechta();
                yerushalmiDaf = mHebrewDateFormatter.formatHebrewNumber(yerushalmi.getDaf());
            } else {//yerushalmi yomi daf is null on sunday
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());

                yerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar());
                yerushalmiMasechta = yerushalmi.getYerushalmiMasechta();
                yerushalmiDaf = mHebrewDateFormatter.formatHebrewNumber(yerushalmi.getDaf());

                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);//reset
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }

        for (int i = 0; i < 7; i++) {
            if (DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), new Date())) {
                weeklyInfo.get(i)[4].setBackgroundColor(getColor(R.color.dark_gold));
            } else {
                weeklyInfo.get(i)[4].setBackground(null);
            }
            StringBuilder announcements = new StringBuilder();
            mZmanimForAnnouncements = new ArrayList<>();//clear the list, it will be filled again in the getShortZmanim method
            mListViews[i].setAdapter(new ArrayAdapter<String>(this, R.layout.zman_list_view, getShortZmanim()) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);

                    TextView textView = view.findViewById(R.id.zman_in_list);

                    if (textView != null) {
                        if (mSharedPreferences.getBoolean("customTextColor", false)) {
                            textView.setTextColor(mSharedPreferences.getInt("tColor", 0xFFFFFFFF));
                        }
                    }

                    return view;
                }
            });//E.G. "Sunrise: 5:45 AM, Sunset: 8:30 PM, etc."
            if (!mZmanimForAnnouncements.isEmpty()) {
                for (String zman : mZmanimForAnnouncements) {
                    announcements.append(zman).append("\n");
                }
            }
            announcements.append(getAnnouncements());
            weeklyInfo.get(i)[1].setText(announcements.toString());//E.G. "Yom Tov, Yom Kippur, etc."
            if (announcements.toString().isEmpty()) {
                weeklyInfo.get(i)[1].setVisibility(View.INVISIBLE);
            } else {
                weeklyInfo.get(i)[1].setVisibility(View.VISIBLE);
            }
            weeklyInfo.get(i)[2].setText(mJewishDateInfo.getJewishDayOfWeek());//E.G. "יום ראשון"
            weeklyInfo.get(i)[3].setText(mHebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth()));//E.G. "א"
            weeklyInfo.get(i)[4].setText(mROZmanimCalendar.getCalendar().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));//E.G. "Sun"
            weeklyInfo.get(i)[5].setText(String.valueOf(mROZmanimCalendar.getCalendar().get(Calendar.DAY_OF_MONTH)));//E.G. "6"
            if (i != 6) {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        if (month != null && !month.equals(mROZmanimCalendar.getCalendar().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()))) {
            month += " - " + mROZmanimCalendar.getCalendar().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        }
        if (!year.equals(String.valueOf(mROZmanimCalendar.getCalendar().get(Calendar.YEAR)))) {
            year += " / " + mROZmanimCalendar.getCalendar().get(Calendar.YEAR);
        }
        if (!hebrewMonth.equals(hebrewDateFormatter.formatMonth(mJewishDateInfo.getJewishCalendar())
                        .replace("Tishrei", "Tishri")
                        .replace("Teves", "Tevet"))) {
            hebrewMonth += " - " + hebrewDateFormatter.formatMonth(mJewishDateInfo.getJewishCalendar())
                    .replace("Tishrei", "Tishri")
                    .replace("Teves", "Tevet");
        }
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            if (!hebrewYear.equals(hebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getJewishYear()))) {
                hebrewYear += " / " + hebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getJewishYear());
            }
        } else {
            if (!hebrewYear.equals(String.valueOf(mJewishDateInfo.getJewishCalendar().getJewishYear()))) {
                hebrewYear += " / " + mJewishDateInfo.getJewishCalendar().getJewishYear();
            }
        }
        if (!masechta.equals(YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechta())) {
            masechta += " " + daf + " - " + YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechta() + " " +
                    mHebrewDateFormatter.formatHebrewNumber(YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf());
        } else {
            masechta += " " + daf + " - " + mHebrewDateFormatter.formatHebrewNumber(YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf());
        }
        yerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar());
        if (yerushalmi != null && !yerushalmiMasechta.equals(yerushalmi.getYerushalmiMasechta())) {
            yerushalmiMasechta += " " + yerushalmiDaf + " - " + yerushalmi.getYerushalmiMasechta() + " " +
                    mHebrewDateFormatter.formatHebrewNumber(yerushalmi.getDaf());
        } else {
            if (yerushalmi != null) {
                yerushalmiMasechta += " " + yerushalmiDaf + " - " + mHebrewDateFormatter.formatHebrewNumber(yerushalmi.getDaf());
            } else {
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());

                yerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar());

                mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);//reset
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());

                if (!yerushalmiMasechta.equals(yerushalmi.getYerushalmiMasechta())) {
                    yerushalmiMasechta += " " + yerushalmiDaf + " - " + yerushalmi.getYerushalmiMasechta() + " " +
                            mHebrewDateFormatter.formatHebrewNumber(yerushalmi.getDaf());
                } else {
                    yerushalmiMasechta += " " + yerushalmiDaf + " - " + mHebrewDateFormatter.formatHebrewNumber(yerushalmi.getDaf());
                }
            }
        }
        String dafs = getString(R.string.daf_yomi) + " " + masechta + "       " + getString(R.string.just_yerushalmi_yomi) + " " + yerushalmiMasechta;
        String monthYear = month + " " + year;
        mEnglishMonthYear.setText(monthYear);
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            mEnglishMonthYear.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            mHebrewMonthYear.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
        mLocationName.setText(sCurrentLocationName);
        String hebrewMonthYear = hebrewMonth + " " + hebrewYear;
        mHebrewMonthYear.setText(hebrewMonthYear);
        mWeeklyDafs.setText(dafs);
        mWeeklyParsha.setText(mJewishDateInfo.getThisWeeksParsha());
        mROZmanimCalendar.getCalendar().setTimeInMillis(backupCal.getTimeInMillis());
        mJewishDateInfo.setCalendar(backupCal);
        mCurrentDateShown = backupCal;
    }

    private String[] getShortZmanim() {
        List<ZmanListEntry> zmanim = new ArrayList<>();
        addZmanim(zmanim, true, mSettingsPreferences, mSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated);
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
                    if (zman.isRTZman() && mSettingsPreferences.getBoolean("RoundUpRT", false)) {
                        DateFormat rtFormat;
                        if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                            if (mSettingsPreferences.getBoolean("ShowSeconds", false)) {
                                rtFormat = new SimpleDateFormat("H:mm:ss", Locale.getDefault());
                            } else {
                                rtFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
                            }
                        } else {
                            if (mSettingsPreferences.getBoolean("ShowSeconds", false)) {
                                rtFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
                            } else {
                                rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                            }
                        }
                        rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                        if (!Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                            mZmanimForAnnouncements.add(rtFormat.format(zman.getZman()) + " : " + zman.getTitle().replaceAll("\\(.*\\)", "").trim());
                        } else {
                            mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + " : " + rtFormat.format(zman.getZman()));
                        }
                    } else {
                        if (!Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                            mZmanimForAnnouncements.add(zmanimFormat.format(zman.getZman()) + " : " + zman.getTitle().replaceAll("\\(.*\\)", "").trim());
                        } else {
                            mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + " : " + zmanimFormat.format(zman.getZman()));
                        }
                    }
                    zmansToRemove.add(zman);
                }
            }
        } else {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isNoteworthyZman()) {
                    if (zman.isRTZman() && mSettingsPreferences.getBoolean("RoundUpRT", false)) {
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
                if (zman.isRTZman() && mSettingsPreferences.getBoolean("RoundUpRT", false)) {
                    DateFormat rtFormat;
                    if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                        if (mSettingsPreferences.getBoolean("ShowSeconds", false)) {
                            rtFormat = new SimpleDateFormat("H:mm:ss", Locale.getDefault());
                        } else {
                            rtFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
                        }
                    } else {
                        if (mSettingsPreferences.getBoolean("ShowSeconds", false)) {
                            rtFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
                        } else {
                            rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                        }
                    }
                    rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                    if (!Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                        shortZmanim[zmanim.indexOf(zman)] = rtFormat.format(zman.getZman()) + " : " +  zman.getTitle();
                    } else {
                        shortZmanim[zmanim.indexOf(zman)] = zman.getTitle() + " : " +  rtFormat.format(zman.getZman());
                    }
                } else {
                    if (!Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                        shortZmanim[zmanim.indexOf(zman)] = zmanimFormat.format(zman.getZman()) + " : " + zman.getTitle().replace("סוף זמן ", "");
                    } else {
                        shortZmanim[zmanim.indexOf(zman)] = zman.getTitle().replace("סוף זמן ", "") + " : " + zmanimFormat.format(zman.getZman());
                    }
                }
                if (zman.getZman().equals(sNextUpcomingZman)) {
                    if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                        shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] + " ➤ ";
                    } else {
                        shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] + " ◄ ";
                    }
                }
            }
        } else {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isRTZman() && mSettingsPreferences.getBoolean("RoundUpRT", false)) {
                    DateFormat rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                    shortZmanim[zmanim.indexOf(zman)] = zman.getTitle() + " : " + rtFormat.format(zman.getZman());
                } else {
                    shortZmanim[zmanim.indexOf(zman)] = zman.getTitle()
                            .replace("Earliest ","")
                            .replace("Sof Zman ", "")
                            .replace("Hacochavim", "")
                            .replace("Latest ", "")
                            + " : " + zmanimFormat.format(zman.getZman());
                }
                if (zman.getZman().equals(sNextUpcomingZman)) {
                    if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                        shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] + " ➤ ";
                    } else {
                        shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] + " ◄ ";
                    }
                }
            }
        }
        return shortZmanim;
    }

    /**
     * This method will check if the tekufa happens within the next 48 hours and it will add the tekufa to the list passed in if it happens
     * on the current date.
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
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE,-1);//reset the calendar
        if (mJewishDateInfo.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            final Calendar cal2 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            cal2.setTime(mJewishDateInfo.getJewishCalendar().getTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {

                if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                    if (shortStyle) {
                        zmanim.add(new ZmanListEntry("תקופת " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                    } else {
                        zmanim.add(new ZmanListEntry("תקופת " + mJewishDateInfo.getJewishCalendar().getTekufaName() +
                                " היום בשעה " + zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                    }
                } else {
                    if (shortStyle) {
                        zmanim.add(new ZmanListEntry("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                    } else {
                        zmanim.add(new ZmanListEntry("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                    }
                }
            }
        }
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());//reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (mJewishDateInfo.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            final Calendar cal2 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            cal2.setTime(mJewishDateInfo.getJewishCalendar().getTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {

                if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                    if (shortStyle) {
                        zmanim.add(new ZmanListEntry("תקופת " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                    } else {
                        zmanim.add(new ZmanListEntry("תקופת " + mJewishDateInfo.getJewishCalendar().getTekufaName() +
                                " היום בשעה " + zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                    }
                } else {
                    if (shortStyle) {
                        zmanim.add(new ZmanListEntry("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                    } else {
                        zmanim.add(new ZmanListEntry("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getTekufaAsDate())));
                    }
                }
            }
        }
    }

    /**
     * This method will check if the tekufa happens within the next 48 hours and it will add the tekufa to the list passed in if it happens
     * on the current date.
     * @param zmanim the list of zmanim to add to
     * @param shortStyle if the tekufa should be added as "Tekufa Nissan : 4:30" or "Tekufa Nissan is today at 4:30"
     */
    private void addAmudeiHoraahTekufaTime(List<ZmanListEntry> zmanim, boolean shortStyle) {
        DateFormat zmanimFormat;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);//check next day for tekufa, because the tekufa time can go back a day
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);//reset the calendar

        if (mJewishDateInfo.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            final Calendar cal2 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            cal2.setTime(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {

                if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                    if (shortStyle) {
                        zmanim.add(new ZmanListEntry("תקופת " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                    } else {
                        zmanim.add(new ZmanListEntry("תקופת " + mJewishDateInfo.getJewishCalendar().getTekufaName() +
                                " היום בשעה " + zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                    }
                } else {
                    if (shortStyle) {
                        zmanim.add(new ZmanListEntry("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                    } else {
                        zmanim.add(new ZmanListEntry("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                    }
                }
            }
        }
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());//reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (mJewishDateInfo.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            final Calendar cal2 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            cal2.setTime(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {

                if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                    if (shortStyle) {
                        zmanim.add(new ZmanListEntry("תקופת " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                    } else {
                        zmanim.add(new ZmanListEntry("תקופת " + mJewishDateInfo.getJewishCalendar().getTekufaName() +
                                " היום בשעה " + zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                    }
                } else {
                    if (shortStyle) {
                        zmanim.add(new ZmanListEntry("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " : " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                    } else {
                        zmanim.add(new ZmanListEntry("Tekufa " + mJewishDateInfo.getJewishCalendar().getTekufaName() + " is today at " +
                                zmanimFormat.format(mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                    }
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.weekly_mode).setChecked(mSharedPreferences.getBoolean("weeklyMode", false));
        menu.findItem(R.id.use_elevation).setChecked(mSharedPreferences.getBoolean("useElevation", true));
        menu.findItem(R.id.use_elevation).setVisible(!mSharedPreferences.getBoolean("LuachAmudeiHoraah", false));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.search_for_a_place) {
            mSharedPreferences.edit().putBoolean("shouldRefresh", true).apply();
            startActivity(new Intent(this, GetUserLocationWithMapActivity.class));
            return true;
        } else if (id == R.id.shabbat_mode) {
            if (!sShabbatMode && mJewishDateInfo != null && mROZmanimCalendar != null && mMainRecyclerView != null) {
                mCurrentDateShown.setTime(new Date());
                mJewishDateInfo.setCalendar(new GregorianCalendar());
                mROZmanimCalendar.setCalendar(new GregorianCalendar());
                startShabbatMode();
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
                }
                item.setChecked(true);
            } else {
                endShabbatMode();
                if (mSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
                }
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
                updateDailyZmanim();
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
                updateDailyZmanim();
            }
            return true;
        } else if (id == R.id.jerDirection) {
            startActivity(new Intent(this, JerusalemDirectionMapsActivity.class));
            return true;
        } else if (id == R.id.netzView) {
            startActivity(new Intent(this, NetzActivity.class));
            return true;
        } else if (id == R.id.molad) {
            startActivity(new Intent(this, MoladActivity.class));
            return true;
        } else if (id == R.id.fullSetup) {
            mSharedPreferences.edit().putBoolean("shouldRefresh", true).apply();
            sSetupLauncher.launch(new Intent(this, FullSetupActivity.class)
                    .putExtra("fromMenu",true));
            return true;
        } else if (id == R.id.settings) {
            mSharedPreferences.edit().putBoolean("shouldRefresh", true).apply();
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (id == R.id.website) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.royzmanim.com"));
            startActivity(browserIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                    if (mNextDate != null) {
                        mNextDate.performClick();
                    }
                } else {
                    if (mPreviousDate != null) {
                        mPreviousDate.performClick();
                    }
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