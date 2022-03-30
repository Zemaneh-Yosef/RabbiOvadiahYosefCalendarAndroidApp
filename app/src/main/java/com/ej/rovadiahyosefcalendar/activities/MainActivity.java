package com.ej.rovadiahyosefcalendar.activities;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import com.ej.rovadiahyosefcalendar.classes.CustomDatePickerDialog;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanAdapter;
import com.ej.rovadiahyosefcalendar.notifications.DailyNotifications;
import com.ej.rovadiahyosefcalendar.notifications.OmerNotifications;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;
import com.kosherjava.zmanim.util.GeoLocation;
import com.kosherjava.zmanim.util.ZmanimFormatter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import us.dustinj.timezonemap.TimeZoneMap;

public class MainActivity extends AppCompatActivity {

    private boolean mNetworkLocationServiceIsDisabled;
    private boolean mGPSLocationServiceIsDisabled;
    private boolean mBackHasBeenPressed = false;
    private boolean mInitialized = false;
    private boolean mShabbatMode;
    private int mCurrentPosition;
    private double mElevation = 0;
    private double mLatitude;
    private double mLongitude;
    private View mLayout;
    private Button mNextDate;
    private Geocoder mGeocoder;
    private Button mPreviousDate;
    private Button mCalendarButton;
    private Handler mHandler = null;
    private Runnable mZmanimUpdater;
    private String mCurrentTimeZoneID;
    private AlertDialog mAlertDialog;
    private TextView mShabbatModeBanner;
    private JewishDateInfo mJewishDateInfo;
    private RecyclerView mMainRecyclerView;
    private GestureDetector mGestureDetector;
    private String mCurrentLocationName = "";
    private ROZmanimCalendar mROZmanimCalendar;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsPreferences;
    private ActivityResultLauncher<Intent> mSetupLauncher;
    public static final String SHARED_PREF = "MyPrefsFile";
    private Calendar mCurrentDateShown = Calendar.getInstance();
    private final Calendar mCurrentDate = Calendar.getInstance();
    private static final int TWENTY_FOUR_HOURS_IN_MILLI = 86_400_000;
    private final ZmanimFormatter mZmanimFormatter = new ZmanimFormatter(TimeZone.getDefault());
    private final static Calendar dafYomiStartDate = new GregorianCalendar(1923, Calendar.SEPTEMBER, 11);
    private final static Calendar dafYomiYerushalmiStartDate = new GregorianCalendar(1980, Calendar.FEBRUARY, 2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {//TODO banner themes
        setTheme(R.style.AppTheme); //splash screen
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.main_layout);
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mGeocoder = new Geocoder(this);
        mGestureDetector = new GestureDetector(MainActivity.this, new ZmanimGestureListener());
        mZmanimFormatter.setTimeFormat(ZmanimFormatter.SEXAGESIMAL_FORMAT);
        createAlertDialog();
        initializeSetupResult();
        setupShabbatModeBanner();
        acquireLatitudeAndLongitude();
        mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
        if (!ChaiTables.visibleSunriseFileExists(getExternalFilesDir(null), mJewishDateInfo.getJewishCalendar())//it should only not exist the first time running the app
                && mSharedPreferences.getBoolean("UseTable", true)
                && savedInstanceState == null) {
            mSetupLauncher.launch(new Intent(this, FullSetupActivity.class));
        }
        if (mGPSLocationServiceIsDisabled && mNetworkLocationServiceIsDisabled) {
            Toast.makeText(MainActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT).show();
        } else {
            if ((!mInitialized && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED)
                    || mSharedPreferences.getBoolean("useZipcode", false)) {
                initializeMainView();
            }
        }
    }

    /**
     * This method registers the setupLauncher to receive the data that the user entered in the
     * SetupActivity. This is a new way of getting results back from activities.
     */
    private void initializeSetupResult() {
        mSetupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (result.getData() != null) {
                            mElevation = result.getData().getDoubleExtra("elevation", 0);
                            editor.putString("lastLocation", mCurrentLocationName).apply();
                        } else {
                            mElevation = Double.parseDouble(mSharedPreferences.getString("elevation", "0"));
                        }
                    } else {
                        mElevation = Double.parseDouble(mSharedPreferences.getString("elevation", "0"));
                    }
                    acquireLatitudeAndLongitude();
                    if (mCurrentTimeZoneID == null) return;
                    instantiateZmanimCalendar();
                    mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                }
        );
    }

    private void initializeMainView() {
        mInitialized = true;
        setTimeZoneID();
        getAndConfirmLastElevationAndVisibleSunriseData();
        instantiateZmanimCalendar();
        saveGeoLocationInfo();
        setupRecyclerView();
        setupButtons();
        seeIfTablesNeedToBeUpdated();
        updateNotifications();
    }

    private void seeIfTablesNeedToBeUpdated() {
        if (mSharedPreferences.getBoolean("isSetup", false) //only check after the app has been setup before
                && !mSharedPreferences.getBoolean("showMishorSunrise", true)) { //only if the tables are being used

            int firstYearOfTables = mSharedPreferences.getInt("firstYearOfTables", 0);
            int secondYearOfTables = mSharedPreferences.getInt("secondYearOfTables", 0);
            int firstYearOfTablesGreg = mSharedPreferences.getInt("firstYearOfTablesGregorian", 0);
            int secondYearOfTablesGreg = mSharedPreferences.getInt("secondYearOfTablesGregorian", 0);

            if (firstYearOfTables == mJewishDateInfo.getJewishCalendar().getJewishYear())
                return;//We don't want to ask during the first year

            if (mSharedPreferences.getBoolean("askNextYear", false) &&
                    mSharedPreferences.getBoolean("askAgainTables", true)) {
                if (secondYearOfTables < mJewishDateInfo.getJewishCalendar().getJewishYear()) {//if we are past 2 years
                    String message = "The visible sunrise tables need to be updated now. " +
                            "If you do not want to update them, you can always choose to show mishor sunrise." + "\n\n" +
                            "Here are the current years for the tables:" + "\n\n" +
                            "First Year: " + firstYearOfTables + " (" + firstYearOfTablesGreg + ")" + "\n" +
                            "Second Year: " + secondYearOfTables + " (" + secondYearOfTablesGreg + ")" + "\n\n" +
                            "Would you like to rerun the setup now?";
                    new AlertDialog.Builder(this)
                            .setTitle("Shana Tovah! You should update!")
                            .setMessage(message)
                            .setPositiveButton("Yes", (dialogInterface, i) ->
                                    mSetupLauncher.launch(new Intent(this, SetupChooserActivity.class)))
                            .setNeutralButton("Do not ask again", (dialogInterface, i) ->
                                    mSharedPreferences.edit().putBoolean("askAgainTables", false).apply())
                            .show();
                }
                return;//only prompt if the current year is after the second year
            }

            if (ChaiTables.visibleSunriseFileExists(getExternalFilesDir(null), mJewishDateInfo.getJewishCalendar())) {
                if (mSharedPreferences.getBoolean("askAgainTables", true)) {//only prompt user if he has not asked to be left alone
                    String message = "Shana Tovah! The visible sunrise tables need to be updated at least " +
                            "once every two years. You can choose to update them now or next year." + "\n\n" +
                            "Here are the current years for the tables:" + "\n\n" +
                            "First Year: " + firstYearOfTables + " (" + firstYearOfTablesGreg + ")" + "\n" +
                            "Second Year: " + secondYearOfTables + " (" + secondYearOfTablesGreg + ")" + "\n\n" +
                            "Would you like to rerun the setup now?";
                    new AlertDialog.Builder(this)
                            .setTitle("Shana Tovah! You should update!")
                            .setMessage(message)
                            .setPositiveButton("Yes", (dialogInterface, i) ->
                                    mSetupLauncher.launch(new Intent(this, SetupChooserActivity.class)))
                            .setNegativeButton("Ask next year", (dialogInterface, i) ->
                                    mSharedPreferences.edit().putBoolean("askNextYear", true).apply())
                            .setNeutralButton("Do not ask again", (dialogInterface, i) ->
                                    mSharedPreferences.edit().putBoolean("askAgainTables", false).apply())
                            .create();
                }
            }
        }
    }

    private void instantiateZmanimCalendar() {
        mROZmanimCalendar = new ROZmanimCalendar(new GeoLocation(
                mCurrentLocationName,
                mLatitude,
                mLongitude,
                mElevation,
                TimeZone.getTimeZone(mCurrentTimeZoneID)));
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
    private void setupNextDayButton() {
        mNextDate = findViewById(R.id.next_day);
        mNextDate.setOnClickListener(v -> {
            mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
            mCurrentDateShown.add(Calendar.DATE, 1);
            mROZmanimCalendar.setCalendar(mCurrentDateShown);
            mJewishDateInfo.setCalendar(mCurrentDateShown);
            mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
        });
    }

    /**
     * Setup the calendar button.
     */
    private void setupCalendarButton() {
        mCalendarButton = findViewById(R.id.calendar);
        DatePickerDialog dialog = createDialog();

        mCalendarButton.setOnClickListener(v -> {
            dialog.updateDate(mROZmanimCalendar.getCalendar().get(Calendar.YEAR),
                    mROZmanimCalendar.getCalendar().get(Calendar.MONTH),
                    mROZmanimCalendar.getCalendar().get(Calendar.DAY_OF_MONTH));
            dialog.show();
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
     * Sets up the previous day button
     */
    private void setupPreviousDayButton() {
        mPreviousDate = findViewById(R.id.prev_day);
        mPreviousDate.setOnClickListener(v -> {
            mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
            mCurrentDateShown.add(Calendar.DATE, -1);
            mROZmanimCalendar.setCalendar(mCurrentDateShown);
            mJewishDateInfo.setCalendar(mCurrentDateShown);
            mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
        });
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
        if (mShabbatMode) {
            startShabbatMode();//left in just in case thread in shabbatMode stops working
        }
        super.onRestart();
    }

    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mShabbatMode) {
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
        super.onResume();
    }

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
     *
     */
    private void saveGeoLocationInfo() {//needed for notifications
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
        editor.putString("name", mCurrentLocationName).apply();
        editor.putLong("lat", Double.doubleToRawLongBits(mLatitude)).apply();//see here: https://stackoverflow.com/a/18098090/13593159
        editor.putLong("long", Double.doubleToRawLongBits(mLongitude)).apply();
        editor.putString("timezoneID", mCurrentTimeZoneID).apply();
    }

    /**
     * This method will called every time the user opens the app. It will reset the notifications every time the app is opened since the user might
     * have changed his location.
     * @see #saveGeoLocationInfo()
     */
    private void updateNotifications() {
        Calendar calendar = (Calendar) mROZmanimCalendar.getCalendar().clone();
        calendar.setTimeInMillis(mROZmanimCalendar.getSunrise().getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                new Intent(getApplicationContext(), DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(dailyPendingIntent);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dailyPendingIntent);

        calendar.setTimeInMillis(mROZmanimCalendar.getSunset().getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                new Intent(getApplicationContext(), OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        am.cancel(omerPendingIntent);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), omerPendingIntent);
    }

    /**
     * Override this method to make sure nothing is blocking the app over shabbat/yom tov
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus)
            if (mShabbatMode) {
                startActivity(getIntent());
            }
    }

    /**
     * This method is called when the user clicks on shabbat mode. The main point of this method is to automatically scroll through the list of zmanim
     * and update the date when the time reaches the next date at 12:00:02am. It will also update the shabbat banner to reflect the next day's date.
     * @see #startScrollingThread() to start the thread that will scroll through the list of zmanim
     * @see #setShabbatBannersText(boolean) to set the text of the shabbat banners
     */
    private void startShabbatMode() {
        if (!mShabbatMode) {
            mShabbatMode = true;
            setShabbatBannersText(true);
            mShabbatModeBanner.setVisibility(View.VISIBLE);
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
     *                    Since it will be called at 12:00:02am the next day, we are already one day ahead of the date we want to show.
     */
    @SuppressLint("SetTextI18n")
    private void setShabbatBannersText(boolean isFirstTime) {//TODO mix colors for shabbat
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
                break;
            default:
                mShabbatModeBanner.setText("SHABBAT MODE                " +
                        "SHABBAT MODE               " +
                        "SHABBAT MODE               " +
                        "SHABBAT MODE               " +
                        "SHABBAT MODE");
                mShabbatModeBanner.setBackgroundColor(getColor(R.color.dark_blue));
                mShabbatModeBanner.setTextColor(getColor(R.color.white));
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
                        if (!mShabbatMode) break;
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
                        if (!mShabbatMode) break;
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
                if (mShabbatMode) {
                    startScrollingThread();
                }
        });
        scrollingThread.start();
    }

    /**
     * This method is called when the user wants to end shabbat mode. It will hide the banner and remove the automatic zmanim updater queued task
     * from the handler.
     * @see #startScrollingThread()
     * @see #startShabbatMode()
     */
    private void endShabbatMode() {
        if (mShabbatMode) {
            mShabbatMode = false;
            mShabbatModeBanner.setVisibility(View.GONE);
            mHandler.removeCallbacksAndMessages(mZmanimUpdater);
        }
    }

    /**
     * This method is the main method for updating the Zmanim in the recyclerview. It is called everytime the user changes the dateo or updates
     * any settings. This method returns a list of Zmanim strings, which are then added to the recyclerview.
     * @return the updated information and Zmanim for the current day in a List of Strings with the following format:
     * zman= 12:00(:00) (seconds are optional)
     */
    private List<String> getZmanimList() {
        DateFormat zmanimFormat;
        if (mSettingsPreferences.getBoolean("ShowSeconds", false)) {
            zmanimFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(mCurrentTimeZoneID)); //set the formatters time zone

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

        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            addHebrewZmanim(zmanimFormat, zmanim);
        } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
            addTranslatedEnglishZmanim(zmanimFormat, zmanim);
        } else {
            addEnglishZmanim(zmanimFormat, zmanim);
        }

        zmanim.add("Additional info:");

        if (!mCurrentDateShown.before(dafYomiStartDate)) {
            zmanim.add("Daf Yomi: " + YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechta()
                    + " " +
                    formatHebrewNumber(YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf()));
        }
        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            zmanim.add("Yerushalmi Yomi: " + YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar()).getMasechta()
                    + " " +
                    formatHebrewNumber(YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar()).getDaf()));
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
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise", true)) {
            zmanim.add("HaNetz= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getHaNetz())));
        } else {
            zmanim.add("HaNetz (Mishor)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise", true) &&
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
                    if (stringSet.contains("Show Regular Minutes")) {
                        zmanim.add("Tzait " + getShabbatAndOrChag() + " (Tom) "
                                + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "= " +
                                zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
                    }
                    if (stringSet.contains("Show Rabbeinu Tam")) {
                        if (mSettingsPreferences.getBoolean("RoundUpRT", false)) {
                            DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                            zmanim.add("Rabbeinu Tam (Tom)= " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
                        } else {
                            zmanim.add("Rabbeinu Tam (Tom)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        zmanim.add("Shkia= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSunset())));
        zmanim.add("Tzait Hacochavim= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzait())));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add("Candle Lighting= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzait())));
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
            if (mSettingsPreferences.getBoolean("RoundUpRT", false)) {
                DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                zmanim.add("Rabbeinu Tam = " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
            } else {
                zmanim.add("Rabbeinu Tam = " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", false)) {
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
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise", true)) {
            zmanim.add("Sunrise= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getHaNetz())));
        } else {
            zmanim.add("Sunrise (Sea Level)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise", true) &&
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
                    if (stringSet.contains("Show Regular Minutes")) {
                        zmanim.add(getShabbatAndOrChag() + " Ends (Tom) "
                                + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "= " +
                                zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
                    }
                    if (stringSet.contains("Show Rabbeinu Tam")) {
                        if (mSettingsPreferences.getBoolean("RoundUpRT", false)) {
                            DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                            zmanim.add("Rabbeinu Tam (Tom)= " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
                        } else {
                            zmanim.add("Rabbeinu Tam (Tom)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
                        }
                    }
                }
                mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
                mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
            }
        }
        zmanim.add("Sunset= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSunset())));
        zmanim.add("Nightfall= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzait())));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add("Candle Lighting= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzait())));
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
            if (mSettingsPreferences.getBoolean("RoundUpRT", false)) {
                DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                zmanim.add("Rabbeinu Tam = " + roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
            } else {
                zmanim.add("Rabbeinu Tam = " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
            }
        }
        if (mSettingsPreferences.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.getJewishCalendar().isAssurBemelacha() && !mJewishDateInfo.getJewishCalendar().hasCandleLighting())) {//if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (mSettingsPreferences.getBoolean("RoundUpRT", false)) {
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
        if (mROZmanimCalendar.getHaNetz() != null && !mSharedPreferences.getBoolean("showMishorSunrise", true)) {
            zmanim.add("\u05D4\u05E0\u05E5= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getHaNetz())));
        } else {
            zmanim.add("\u05D4\u05E0\u05E5 (\u05DE\u05D9\u05E9\u05D5\u05E8)= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
        }
        if (mROZmanimCalendar.getHaNetz() != null &&
                !mSharedPreferences.getBoolean("showMishorSunrise", true) &&
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
                    if (stringSet.contains("Show Regular Minutes")) {
                        zmanim.add("\u05E6\u05D0\u05EA " + getShabbatAndOrChag() + " (\u05DE\u05D7\u05E8) "
                                + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "= " +
                                zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
                    }
                    if (stringSet.contains("Show Rabbeinu Tam")) {
                        if (mSettingsPreferences.getBoolean("RoundUpRT", false)) {
                            DateFormat roundUpFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());//just to remove the seconds
                            zmanim.add("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD (\u05DE\u05D7\u05E8)= " +
                                    roundUpFormat.format(checkNull(addMinuteToZman(mROZmanimCalendar.getTzais72Zmanis()))));
                        } else {
                            zmanim.add("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD (\u05DE\u05D7\u05E8)= " +
                                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
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
                zmanimFormat.format(checkNull(mROZmanimCalendar.getTzait())));
        if (mJewishDateInfo.getJewishCalendar().hasCandleLighting() &&
                mJewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
            if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                zmanim.add("\u05D4\u05D3\u05DC\u05E7\u05EA \u05E0\u05E8\u05D5\u05EA= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzait())));
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
            if (mSettingsPreferences.getBoolean("RoundUpRT", false)) {
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
                if (mSettingsPreferences.getBoolean("RoundUpRT", false)) {
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
     * This is a simple convenience method to check if the given date it null or not. If the date is not null,
     * it will return a the same date with a minute added to it.
     * Otherwise, if the date is null, it will return null.
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
     * This is a simple convenience method to check if the given date it null or not. If the date is not null,
     * it will return exactly what was given.
     * However, if the date is null, it will change the date to a string that says "N/A" (Not Available).
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
     * This is a simple convenience method to check if the given date is on shabbat or yom tov or both
     * @return a string that says whether it is shabbat and chag or just shabbat or just chag (in Hebrew or English)
     */
    private String getShabbatAndOrChag() {
        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            if (mJewishDateInfo.getJewishCalendar().isYomTov() &&
                    mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "\u05E9\u05D1\u05EA/\u05D7\u05D2";
            } else if (mJewishDateInfo.getJewishCalendar().getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                return "\u05E9\u05D1\u05EA";
            } else {
                return "\u05D7\u05D2";
            }
        } else {
            if (mJewishDateInfo.getJewishCalendar().isYomTov() &&
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
     * This method uses the TimeZoneMap to get the current timezone ID based on the latitude and longitude of the device.
     * If the latitude and longitude are not known, it will use the default timezone ID.
     */
    private void setTimeZoneID() {
        if (mLatitude != 0 && mLongitude != 0) {
            TimeZoneMap timeZoneMap = TimeZoneMap.forRegion(
                    Math.floor(mLatitude), Math.floor(mLongitude),
                    Math.ceil(mLatitude), Math.ceil(mLongitude));//trying to avoid using the forEverywhere() method
            mCurrentTimeZoneID = Objects.requireNonNull(timeZoneMap.getOverlappingTimeZone(mLatitude, mLongitude)).getZoneId();
        } else {
            mCurrentTimeZoneID = TimeZone.getDefault().getID();
        }
    }

    /**
     * This method uses the Geocoder class to try and get the current location's name. I have
     * tried to make my results similar to the zmanim app by JGindin on the Play Store. In america,
     * it will get the current location by state and city. Whereas, in other areas of the world, it
     * will get the country and the city.
     *
     * @return a string containing the name of the current city and state/country that the user is located in.
     * @see Geocoder
     */
    private String getLocationAsName() {
        StringBuilder result = new StringBuilder();
        List<Address> addresses = null;
        try {
            addresses = mGeocoder.getFromLocation(mLatitude, mLongitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && addresses.size() > 0) {

            String city = addresses.get(0).getLocality();
            if (city != null) {
                result.append(city).append(", ");
            }

            String state = addresses.get(0).getAdminArea();
            if (state != null) {
                result.append(state);
            }

            if (result.toString().endsWith(", ")) {
                result.deleteCharAt(result.length() - 2);
            }

            if (city == null && state == null) {
                String country = addresses.get(0).getCountryName();
                result.append(country);
            }
        }
        return result.toString();
    }

    /**
     * This method checks if the user has already setup the elevation and visible sunrise from the last time he started
     * the app. If he has setup the elevation and visible sunrise, then it checks if the user is in the
     * same city as the last time he setup the app based on the getLocationAsName method. If the user is in the same city,
     * all is good. If the user is in another city, we create an AlertDialog to warn the user that the elevation data
     * and visible sunrise data are not accurate.
     *
     * @see #createAlertDialog()
     * @see #getLocationAsName()
     */
    private void getAndConfirmLastElevationAndVisibleSunriseData() {
        String lastLocation = mSharedPreferences.getString("lastLocation", "");

        String message ="The elevation and visible sunrise data change depending on the city you are in. " +
                        "Therefore, it is recommended that you update your elevation and visible sunrise " +
                        "data according to your current location." + "\n\n" +
                        "Last Location: " + lastLocation + "\n" +
                        "Current Location: " + mCurrentLocationName + "\n\n" +
                        "Would you like to rerun the setup now?";

        if (mSharedPreferences.getBoolean("askagain", true)) {//only prompt user if he has not asked to be left alone
            if (!lastLocation.isEmpty()) {//only check after the app has been setup before
                try {//TODO this needs to be removed but cannot be removed for now because it is needed for people who have setup the app before
                    mElevation = Double.parseDouble(mSharedPreferences.getString("elevation", "0"));//get and set the last value
                } catch (Exception e) {
                    try {
                        mElevation = mSharedPreferences.getFloat("elevation", 0);//get and set the last value
                    } catch (Exception e1) {
                        mElevation = 0;
                        e1.printStackTrace();
                    }
                }
                if (!lastLocation.equals(mCurrentLocationName) && mElevation != 0) {//user should update his elevation in another city
                    mAlertDialog.setMessage(message);
                    mAlertDialog.show();
                }
            }
        }
    }

    /**
     * This method creates the AlertDialog that will be shown to the user if the user is in another city and he has setup the app before.
     * The AlertDialog will have two buttons: "Yes" and "No". If the user clicks "Yes", then the user will be taken to the
     * elevation and visible sunrise setup activity. If the user clicks "No", then the user will be taken to the main activity.
     * @see #getAndConfirmLastElevationAndVisibleSunriseData()
     */
    private void createAlertDialog() {
        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle("You are not in the same city as the last time that you " +
                        "setup the app!")
                .setPositiveButton("Yes", (dialogInterface, i) ->
                        mSetupLauncher.launch(new Intent(this, SetupChooserActivity.class)
                                .putExtra("fromMenu",true)))
                .setNegativeButton("No", (dialogInterface, i) -> Toast.makeText(
                        this, "Using visible sunrise and elevation for your last location", Toast.LENGTH_LONG)
                        .show())
                .setNeutralButton("Do not ask again", (dialogInterface, i) -> {
                    mSharedPreferences.edit().putBoolean("askagain", false).apply();
                    Toast.makeText(this, "Your current elevation is: " + mElevation, Toast.LENGTH_LONG)
                            .show();
                }).create();
    }

    /**
     * This method gets the devices last known latitude and longitude. It will ask for permission
     * if we do not have it, and it will alert the user if location services is disabled.
     * <p>
     * As of Android 11 (API 30) there is a more accurate way of getting the current location of the
     * device, however, the process is slower as it needs to actually make a call to the GPS service
     * if the location has not been updated recently. This newer call made the app look slow at
     * startup, therefore, I added a splash screen and a Toast to let the user know that the app
     * is working.
     * <p>
     * This method will now first check if the user wants to use a zip code. If the user entered a
     * zip code before, the app will use that zip code for as the current location.
     */
    @SuppressWarnings("BusyWait")
    private void acquireLatitudeAndLongitude() {
        if (mSharedPreferences.getBoolean("useZipcode", false)) {
            getLatitudeAndLongitudeFromZipcode();
        } else {
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
            } else {
                try {
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager != null) {
                        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            mNetworkLocationServiceIsDisabled = true;
                        }
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            mGPSLocationServiceIsDisabled = true;
                        }
                        LocationListener locationListener = new LocationListener() {
                            @Override
                            public void onLocationChanged(@NonNull Location location) { }
                            @Override
                            public void onProviderEnabled(@NonNull String provider) { }
                            @Override
                            public void onProviderDisabled(@NonNull String provider) { }
                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) { }
                        };
                        if (!mNetworkLocationServiceIsDisabled) {
                            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
                        }
                        if (!mGPSLocationServiceIsDisabled) {
                            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                        }
                        if (!mNetworkLocationServiceIsDisabled || !mGPSLocationServiceIsDisabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//newer implementation
                                locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER,
                                        null, Runnable::run,
                                        location -> {
                                            if (location != null) {
                                                mLatitude = location.getLatitude();
                                                mLongitude = location.getLongitude();
                                            }
                                        });
                                locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER,
                                        null, Runnable::run,
                                        location -> {
                                            if (location != null) {
                                                mLatitude = location.getLatitude();
                                                mLongitude = location.getLongitude();
                                            }
                                        });
                                long tenSeconds = System.currentTimeMillis() + 10000;
                                while ((mLatitude == 0 && mLongitude == 0) && System.currentTimeMillis() < tenSeconds) {
                                    Thread.sleep(0);//we MUST wait for the location data to be set or else the app will crash
                                }
                                if (mLatitude == 0 && mLongitude == 0) {//if 10 seconds passed and we still don't have the location, use the older implementation
                                    Location location;//location might be old
                                    if (!mNetworkLocationServiceIsDisabled) {
                                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                    } else {
                                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                    }
                                    if (location != null) {
                                        mLatitude = location.getLatitude();
                                        mLongitude = location.getLongitude();
                                    }
                                }
                            } else {//older implementation
                                Location location = null;//location might be old
                                if (!mNetworkLocationServiceIsDisabled) {
                                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                }
                                if (location != null) {
                                    mLatitude = location.getLatitude();
                                    mLongitude = location.getLongitude();
                                }
                                if (!mGPSLocationServiceIsDisabled) {
                                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                }
                                if (location != null && (mLatitude == 0 && mLongitude == 0)) {
                                    mLatitude = location.getLatitude();
                                    mLongitude = location.getLongitude();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        resolveCurrentLocationName();
    }

    /**
     * Resolves the current location name to be a latitude and longitude if mCurrentLocationName is empty
     * @see #mCurrentLocationName
     */
    private void resolveCurrentLocationName() {
        mCurrentLocationName = getLocationAsName();
        if (mCurrentLocationName.isEmpty()) {
            if (mLatitude != 0 && mLongitude != 0) {
                String lat = String.valueOf(mLatitude);
                if (lat.contains("-")) {
                    lat = lat.substring(0, 5);
                } else {
                    lat = lat.substring(0, 4);
                }
                String longitude = String.valueOf(mLongitude);
                if (longitude.contains("-")) {
                    longitude = longitude.substring(0, 5);
                } else {
                    longitude = longitude.substring(0, 4);
                }
                mCurrentLocationName = "Lat: " + lat + " Long: " + longitude;
            }
        }
    }

    /**
     * This method will let us know if the user accepted the location permissions. If not, it will
     * create an Alert Dialog box to ask the user to accept the permission again or enter a zipcode.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        if (requestCode == 1) {
            if (permissions.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                acquireLatitudeAndLongitude();
                if (!mInitialized) {
                    initializeMainView();
                }
            } else {
                createLocationDialog();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * This method will create a new AlertDialog that asks the user to use their location and it
     * will also give the option to use a zipcode through the createZipcodeDialog method which
     * will create another dialog. This method will
     *
     * @see #createZipcodeDialog()
     */
    private void createLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_location_permission)
                .setMessage(R.string.text_location_permission)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    acquireLatitudeAndLongitude();//restart
                })
                .setNeutralButton(R.string.zipcode, (dialogInterface, i) -> createZipcodeDialog())
                .setCancelable(false)
                .create()
                .show();
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
                        Toast.makeText(MainActivity.this, "Please Enter a valid value, for example: 11024", Toast.LENGTH_SHORT)
                                .show();
                        createLocationDialog();
                    } else {
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putBoolean("useZipcode", true).apply();
                        editor.putString("Zipcode", input.getText().toString()).apply();
                        getLatitudeAndLongitudeFromZipcode();
                        if (!mInitialized) {
                            initializeMainView();
                        } else {
                            setTimeZoneID();
                            instantiateZmanimCalendar();
                            mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                            getAndConfirmLastElevationAndVisibleSunriseData();
                        }
                    }
                })
                .setNeutralButton("Use location", (dialog, which) -> {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean("useZipcode", false).apply();
                    acquireLatitudeAndLongitude();
                    setTimeZoneID();
                    instantiateZmanimCalendar();
                    mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
                    getAndConfirmLastElevationAndVisibleSunriseData();
                })
                .create()
                .show();
    }

    /**
     * This method uses the Geocoder class to get a latitude and longitude coordinate from the user
     * specified zip code. If it can not find am address it will make a toast saying that an error
     * occurred.
     *
     * @see Geocoder
     */
    private void getLatitudeAndLongitudeFromZipcode() {
        String zipcode = mSharedPreferences.getString("Zipcode", "");
        List<Address> address = null;
        try {
            address = mGeocoder.getFromLocationName(zipcode, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if ((address != null ? address.size() : 0) > 0) {
            Address first = address.get(0);
            mLatitude = first.getLatitude();
            mLongitude = first.getLongitude();
            mCurrentLocationName = getLocationAsName();
            mSharedPreferences.edit().putLong("oldLat", Double.doubleToRawLongBits(mLatitude)).apply();
            mSharedPreferences.edit().putLong("oldLong", Double.doubleToRawLongBits(mLongitude)).apply();
        } else {
            getOldZipcodeLocation();
        }
    }

    /**
     * This method retrieves the old location data from the devices storage if it has already been
     * setup beforehand.
     *
     * @see #getLatitudeAndLongitudeFromZipcode()
     */
    private void getOldZipcodeLocation() {
        double oldLat = Double.longBitsToDouble(mSharedPreferences.getLong("oldLat", 0));
        double oldLong = Double.longBitsToDouble(mSharedPreferences.getLong("oldLong", 0));

        if (oldLat == mLatitude && oldLong == mLongitude) {
            Toast.makeText(MainActivity.this,
                    "Unable to change location, using old location.", Toast.LENGTH_LONG).show();
        }

        if (oldLat != 0 && oldLong != 0) {
            mLatitude = oldLat;
            mLongitude = oldLong;
        } else {
            Toast.makeText(MainActivity.this,
                    "An error occurred getting zipcode coordinates", Toast.LENGTH_LONG).show();
        }
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
            acquireLatitudeAndLongitude();
            setTimeZoneID();
            mCurrentDate.setTimeInMillis(new Date().getTime());
            mCurrentDateShown.setTime(mCurrentDate.getTime());
            mJewishDateInfo.setCalendar(mCurrentDate);
            mROZmanimCalendar.setCalendar(mCurrentDate);
            mMainRecyclerView.setAdapter(new ZmanAdapter(this, getZmanimList()));
            getAndConfirmLastElevationAndVisibleSunriseData();
            return true;
        } else if (id == R.id.enterZipcode) {
            createZipcodeDialog();
            return true;
        } else if (id == R.id.shabbat_mode) {
            if (!mShabbatMode) {
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