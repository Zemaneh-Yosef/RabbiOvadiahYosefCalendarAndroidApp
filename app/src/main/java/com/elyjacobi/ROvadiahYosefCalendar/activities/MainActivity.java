package com.elyjacobi.ROvadiahYosefCalendar.activities;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.elyjacobi.ROvadiahYosefCalendar.classes.JewishDateInfo.formatHebrewNumber;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elyjacobi.ROvadiahYosefCalendar.R;
import com.elyjacobi.ROvadiahYosefCalendar.classes.ChaiTables;
import com.elyjacobi.ROvadiahYosefCalendar.classes.JewishDateInfo;
import com.elyjacobi.ROvadiahYosefCalendar.classes.ROZmanimCalendar;
import com.elyjacobi.ROvadiahYosefCalendar.classes.ZmanAdapter;
import com.elyjacobi.ROvadiahYosefCalendar.notifications.DailyNotifications;
import com.elyjacobi.ROvadiahYosefCalendar.notifications.OmerNotifications;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import us.dustinj.timezonemap.TimeZoneMap;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private boolean mNetworkLocationServiceIsDisabled;
    private boolean mGPSLocationServiceIsDisabled;
    private boolean initialized = false;
    private boolean mShabbatMode;
    private double mElevation = 0;
    private double mLatitude;
    private double mLongitude;
    private Geocoder geocoder;
    private Calendar mCurrentDate;
    private Button mCalendarButton;
    private Handler mHandler = null;
    private Runnable mZmanimUpdater;
    private String mCurrentTimeZoneID;
    private TextView mShabbatModeBanner;
    private JewishDateInfo jewishDateInfo;
    private RecyclerView mMainRecyclerView;
    private String mCurrentLocationName = "";
    private ROZmanimCalendar mROZmanimCalendar;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsPreferences;
    private ActivityResultLauncher<Intent> mSetupLauncher;
    private final ZmanimFormatter zmanimFormatter = new ZmanimFormatter(TimeZone.getDefault());
    private static final int TWENTY_FOUR_HOURS_IN_MILLI = 86_400_000;
    public static final String SHARED_PREF = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme); //splash screen
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        geocoder = new Geocoder(this);
        zmanimFormatter.setTimeFormat(ZmanimFormatter.SEXAGESIMAL_FORMAT);
        initializeSetupResult();
        mShabbatModeBanner = findViewById(R.id.shabbat_mode);
        mShabbatModeBanner.setSelected(true);
        acquireLatitudeAndLongitude();
        jewishDateInfo = new JewishDateInfo(
                mSharedPreferences.getBoolean("inIsrael", false),
                true);
        if (!ChaiTables.visibleSunriseFileExists(getExternalFilesDir(null), jewishDateInfo.getJewishCalendar())//it should only not exist the first time running the app
                && mSharedPreferences.getBoolean("UseTable", true)
                && savedInstanceState == null) {
            mSetupLauncher.launch(new Intent(this, FullSetupActivity.class));
        }
        if (mGPSLocationServiceIsDisabled && mNetworkLocationServiceIsDisabled) {
            Toast.makeText(MainActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT)
                    .show();
        } else {
            if ((!initialized && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED)
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
                            mElevation = result.getData()
                                    .getDoubleExtra("elevation", 0);
                        }
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        mElevation = 0;
                        editor.putBoolean("askagain", false).apply();//If he doesn't care about elevation, we shouldn't bother him
                    }
                    editor.putString("lastLocation", mCurrentLocationName).apply();
                    if (mCurrentTimeZoneID == null) return;
                    instantiateZmanimCalendar();
                    mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
                }
        );
    }

    private void initializeMainView() {
        initialized = true;
        setTimeZoneID();
        getAndAffirmLastElevationData();
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

            if (firstYearOfTables == jewishDateInfo.getJewishCalendar().getJewishYear())
                return;//We don't want to ask during the first year

            if (mSharedPreferences.getBoolean("askNextYear", false)) {
                if (!(secondYearOfTables < jewishDateInfo.getJewishCalendar().getJewishYear()))
                    return;//only prompt if the current year is after the second year
            }

            String message = "Shana Tovah! The visible sunrise tables need to be updated at least " +
                    "once every two years. You can choose to update them now or next year." + "\n\n" +
                    "Here are the current years for the tables:" + "\n\n" +
                    "First Year: " + firstYearOfTables +
                    " (" + mROZmanimCalendar.getCalendar().get(Calendar.YEAR) + ")" + "\n" +
                    "Second Year: " + secondYearOfTables +
                    " (" + (mROZmanimCalendar.getCalendar().get(Calendar.YEAR) + 1) + ")" + "\n\n" +
                    "Would you like to rerun the setup now?";

            if (ChaiTables.visibleSunriseFileExists(getExternalFilesDir(null), jewishDateInfo.getJewishCalendar())) {
                if (mSharedPreferences.getBoolean("askAgainTables", true)) {//only prompt user if he has not asked to be left alone
                    new AlertDialog.Builder(this)
                            .setTitle("Shana Tovah! You should update!")
                            .setMessage(message)
                            .setPositiveButton("Yes", (dialogInterface, i) ->
                                    mSetupLauncher.launch(new Intent(this, SetupChooserActivity.class)))
                            .setNegativeButton("Ask next year", (dialogInterface, i) ->
                                    mSharedPreferences.edit().putBoolean("askNextYear", true).apply())
                            .setNeutralButton("Do not ask again", (dialogInterface, i) ->
                                    mSharedPreferences.edit().putBoolean("askAgainTables", false).apply())
                            .show();
                }
            }
        }
    }

    private void instantiateZmanimCalendar() {
        mCurrentDate = null;
        if (mROZmanimCalendar != null) {
            mCurrentDate = mROZmanimCalendar.getCalendar();
        }
        mROZmanimCalendar = new ROZmanimCalendar(new GeoLocation(
                mCurrentLocationName,
                mLatitude,
                mLongitude,
                mElevation,
                TimeZone.getTimeZone(mCurrentTimeZoneID)));
        if (mCurrentDate != null) {
            mROZmanimCalendar.setCalendar(mCurrentDate);
        }
        mROZmanimCalendar.setExternalFilesDir(getExternalFilesDir(null));
        mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(mSettingsPreferences.getString("CandleLightingOffset", "20")));
        mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(mSettingsPreferences.getString("EndOfShabbatOffset", "45")));
    }

    private void setupRecyclerView() {
        mMainRecyclerView = findViewById(R.id.mainRV);
        mMainRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMainRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
    }

    private void setupButtons() {
        setupPreviousDayButton();
        setupCalendarButton();
        setupNextDayButton();
    }

    private void setupNextDayButton() {
        Button next = findViewById(R.id.next_day);
        next.setOnClickListener(v -> {
            Calendar calendar = mROZmanimCalendar.getCalendar();
            calendar.add(Calendar.DATE, 1);
            mROZmanimCalendar.setCalendar(calendar);
            jewishDateInfo.setCalendar(calendar);
            mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
        });
    }

    private void setupCalendarButton() {
        mCalendarButton = findViewById(R.id.calendar);
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar mUserChosenDate = Calendar.getInstance();
            mUserChosenDate.set(year, month, day);
            mROZmanimCalendar.setCalendar(mUserChosenDate);
            jewishDateInfo.setCalendar(mUserChosenDate);
            mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
        },
                mROZmanimCalendar.getCalendar().get(Calendar.YEAR),
                mROZmanimCalendar.getCalendar().get(Calendar.MONTH),
                mROZmanimCalendar.getCalendar().get(Calendar.DAY_OF_MONTH));
        mCalendarButton.setOnClickListener(v -> dialog.show());
        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
    }

    private void setupPreviousDayButton() {
        Button prev = findViewById(R.id.prev_day);
        prev.setOnClickListener(v -> {
            Calendar calendar = mROZmanimCalendar.getCalendar();
            calendar.add(Calendar.DATE, -1);
            mROZmanimCalendar.setCalendar(calendar);
            jewishDateInfo.setCalendar(calendar);
            mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawable());
        });
    }

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
        if (mShabbatMode) {
            startActivity(getIntent());
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mCurrentTimeZoneID == null) {
            super.onResume();
            return;
        }
        instantiateZmanimCalendar();
        jewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
        jewishDateInfo.setCalendar(mCurrentDate);
        mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
        getAndAffirmLastElevationData();
        super.onResume();
    }

    private void saveGeoLocationInfo() {//needed for notifications
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
        editor.putString("name", mCurrentLocationName).apply();
        editor.putLong("lat", Double.doubleToRawLongBits(mLatitude)).apply();//see here: https://stackoverflow.com/a/18098090/13593159
        editor.putLong("long", Double.doubleToRawLongBits(mLongitude)).apply();
        editor.putString("timezoneID", mCurrentTimeZoneID).apply();
    }

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

    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    private void startShabbatMode() {
        if (!mShabbatMode) {
            mShabbatMode = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mShabbatModeBanner.setVisibility(View.VISIBLE);
            Calendar calendar = Calendar.getInstance();
            Calendar calendar2 = Calendar.getInstance();
            mHandler = new Handler(getMainLooper());
            mZmanimUpdater = () -> {
                calendar.setTimeInMillis(new Date().getTime());
                mROZmanimCalendar.setCalendar(calendar);
                jewishDateInfo.setCalendar(calendar);
                synchronized (mMainRecyclerView) {
                    mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
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

    @SuppressWarnings({"SynchronizeOnNonFinalField", "BusyWait"})
    private void startScrollingThread() {
        Thread scrollingThread = new Thread(() -> {
                synchronized (mMainRecyclerView) {//never properly tested, however, I assumed that you need the recyclerView to be synchronized or else an error will be thrown when the recyclerView is being updated and scrolled through at the same time.
                    for (int i = 0; i < Objects.requireNonNull(mMainRecyclerView.getAdapter()).getItemCount() - 1; i++) {
                        if (!mShabbatMode) break;
                        mMainRecyclerView.smoothScrollToPosition(i);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    for (int i = mMainRecyclerView.getAdapter().getItemCount() - 1; i > 0; i--) {
                        if (!mShabbatMode) break;
                        mMainRecyclerView.smoothScrollToPosition(i);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (mShabbatMode) {
                    startScrollingThread();
                }
        });
        scrollingThread.start();
    }

    private void endShabbatMode() {
        if (mShabbatMode) {
            mShabbatMode = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mShabbatModeBanner.setVisibility(View.GONE);
            mHandler.removeCallbacksAndMessages(mZmanimUpdater);
        }
    }

    private List<String> getZmanimList() {
        DateFormat zmanimFormat;
        if (mSettingsPreferences.getBoolean("ShowSeconds", true)) {
            zmanimFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(mCurrentTimeZoneID)); //set the formatters time zone

        List<String> zmanim = new ArrayList<>();

        zmanim.add(mROZmanimCalendar.getGeoLocation().getLocationName());

        zmanim.add(jewishDateInfo.getJewishCalendar().toString()
                .replace("Teves", "Tevet")
                + "      " +
                mROZmanimCalendar.getCalendar().get(Calendar.DATE)
                + " " +
                mROZmanimCalendar.getCalendar().getDisplayName(
                        Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
                + ", " +
                mROZmanimCalendar.getCalendar().get(Calendar.YEAR));

        zmanim.add(jewishDateInfo.getThisWeeksParsha());

        zmanim.add(mROZmanimCalendar.getCalendar()
                .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                + " / " +
                jewishDateInfo.getJewishDayOfWeek());

        String day = jewishDateInfo.getSpecialDay();
        if (!day.isEmpty()) {
            zmanim.add(day);
        }

        zmanim.add(jewishDateInfo.getIsTachanunSaid());

        if (jewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            zmanim.add("Birchat HaChamah is said today");
        }

        if (mSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            zmanim.add("\u05E2\u05DC\u05D5\u05EA \u05D4\u05E9\u05D7\u05E8= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getAlos72Zmanis())));
            zmanim.add("\u05D8\u05DC\u05D9\u05EA \u05D5\u05EA\u05E4\u05D9\u05DC\u05D9\u05DF= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getEarliestTalitTefilin())));
            if (mROZmanimCalendar.getHaNetz() != null
                    && !mSharedPreferences.getBoolean("showMishorSunrise", true)) {
                zmanim.add("\u05D4\u05E0\u05E5= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getHaNetz())));
            } else {
                zmanim.add("\u05D4\u05E0\u05E5 (\u05DE\u05D9\u05E9\u05D5\u05E8)= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
            }
            if (jewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
                zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05D0\u05DB\u05D9\u05DC\u05EA \u05D7\u05DE\u05E5= "
                        + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis())));
                zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05D1\u05D9\u05E2\u05D5\u05E8 \u05D7\u05DE\u05E5= "
                        + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanBiurChametzGRA())));//TODO double check this, maybe we go like MGA getTimeOffset(getAlos72Zmanis(), getShaahZmanisMGA() * 5)
            }
            zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05E9\u05DE\u05E2 \u05DE\u05D2\"\u05D0= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis())));
            zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05E9\u05DE\u05E2 \u05D2\u05E8\"\u05D0= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaGRA())));
            zmanim.add("\u05E1\u05D5\u05E3 \u05D6\u05DE\u05DF \u05D1\u05E8\u05DB\u05D5\u05EA \u05E9\u05DE\u05E2= "
                    + zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaGRA())));
            zmanim.add("\u05D7\u05E6\u05D5\u05EA= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getChatzot())));
            zmanim.add("\u05DE\u05E0\u05D7\u05D4 \u05D2\u05D3\u05D5\u05DC\u05D4= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaGedolaGreaterThan30())));
            zmanim.add("\u05DE\u05E0\u05D7\u05D4 \u05E7\u05D8\u05E0\u05D4= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaKetana())));
            zmanim.add("\u05E4\u05DC\u05D2 \u05D4\u05DE\u05E0\u05D7\u05D4= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getPlagHamincha())));
            if (jewishDateInfo.getJewishCalendar().hasCandleLighting()) {
                zmanim.add("\u05D4\u05D3\u05DC\u05E7\u05EA \u05E0\u05E8\u05D5\u05EA (" +
                        (int) mROZmanimCalendar.getCandleLightingOffset() + ")= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getCandleLighting())));
            }
            zmanim.add("\u05E9\u05E7\u05D9\u05E2\u05D4= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getSunset())));
            zmanim.add("\u05E6\u05D0\u05EA \u05D4\u05DB\u05D5\u05DB\u05D1\u05D9\u05DD= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getTzait())));
            if (jewishDateInfo.getJewishCalendar().isTaanis()
                    && jewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
                zmanim.add("\u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanit())));
                zmanim.add("\u05E6\u05D0\u05EA \u05EA\u05E2\u05E0\u05D9\u05EA \u05DC\u05D7\u05D5\u05DE\u05E8\u05D4= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanitLChumra())));
            }
            if (jewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
                zmanim.add("\u05E6\u05D0\u05EA \u05E9\u05D1\u05EA/\u05D7\u05D2 " +
                        "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "=" +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
                zmanim.add("\u05E8\u05D1\u05D9\u05E0\u05D5 \u05EA\u05DD= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
            }
            zmanim.add("\u05D7\u05E6\u05D5\u05EA \u05DC\u05D9\u05DC\u05D4= " + zmanimFormat.format(mROZmanimCalendar.getSolarMidnight()));
        } else {//Default to English//TODO add english translation
            zmanim.add("Alot Hashachar= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getAlos72Zmanis())));
            zmanim.add("Earliest Talit/Tefilin= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getEarliestTalitTefilin())));
            if (mROZmanimCalendar.getHaNetz() != null
                    && !mSharedPreferences.getBoolean("showMishorSunrise", true)) {
                zmanim.add("HaNetz= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getHaNetz())));
            } else {
                zmanim.add("HaNetz (Mishor)= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getSeaLevelSunrise())));
            }
            if (jewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
                zmanim.add("Sof Zman Achilat Chametz= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaMGA72MinutesZmanis())));
                zmanim.add("Sof Zman Biur Chametz= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanBiurChametzGRA())));//TODO double check this, maybe we go like MGA getTimeOffset(getAlos72Zmanis(), getShaahZmanisMGA() * 5)
            }
            zmanim.add("Sof Zman Shma Mg'a= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaMGA72MinutesZmanis())));
            zmanim.add("Sof Zman Shma Gr'a= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanShmaGRA())));
            zmanim.add("Sof Zman Brachot Shma= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getSofZmanTfilaGRA())));
            zmanim.add("Chatzot= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getChatzot())));
            zmanim.add("Mincha Gedola= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaGedolaGreaterThan30())));
            zmanim.add("Mincha Ketana= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getMinchaKetana())));
            zmanim.add("Plag HaMincha= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getPlagHamincha())));
            if (jewishDateInfo.getJewishCalendar().hasCandleLighting()) {
                zmanim.add("Candle Lighting (" + (int) mROZmanimCalendar.getCandleLightingOffset()
                        + ")= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getCandleLighting())));
            }
            zmanim.add("Shkia= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getSunset())));
            zmanim.add("Tzait Hacochavim= " + zmanimFormat.format(checkNull(mROZmanimCalendar.getTzait())));
            if (jewishDateInfo.getJewishCalendar().isTaanis()
                    && jewishDateInfo.getJewishCalendar().getYomTovIndex() != JewishCalendar.YOM_KIPPUR) {
                zmanim.add("Tzait Taanit= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanit())));
                zmanim.add("Tzait Taanit L'Chumra= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaitTaanitLChumra())));
            }
            if (jewishDateInfo.getJewishCalendar().isAssurBemelacha()) {
                zmanim.add("Tzait Shabbat/Chag "
                        + "(" + (int) mROZmanimCalendar.getAteretTorahSunsetOffset() + ")" + "= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzaisAteretTorah())));
                zmanim.add("Rabbeinu Tam= " +
                        zmanimFormat.format(checkNull(mROZmanimCalendar.getTzais72Zmanis())));
            }
            zmanim.add("Chatzot Layla= " +
                    zmanimFormat.format(checkNull(mROZmanimCalendar.getSolarMidnight())));
        }

        zmanim.add("Additional info:");

        zmanim.add("Daf Yomi: " + YomiCalculator.getDafYomiBavli(jewishDateInfo.getJewishCalendar()).getMasechta()
                + " " +
                formatHebrewNumber(YomiCalculator.getDafYomiBavli(jewishDateInfo.getJewishCalendar()).getDaf()));

        zmanim.add("Yerushalmi Yomi: " + YerushalmiYomiCalculator.getDafYomiYerushalmi(jewishDateInfo.getJewishCalendar()).getMasechta()
                + " " +
                formatHebrewNumber(YerushalmiYomiCalculator.getDafYomiYerushalmi(jewishDateInfo.getJewishCalendar()).getDaf()));

        zmanim.add("Shaah Zmanit GR\"A: " + zmanimFormatter.format(mROZmanimCalendar.getShaahZmanisGra()));

        if (mROZmanimCalendar.getGeoLocation().getTimeZone().inDaylightTime(mROZmanimCalendar.getSeaLevelSunrise())) {
            zmanim.add("Daylight Savings Time is on");
        } else {
            zmanim.add("Daylight Savings Time is off");
        }
        zmanim.add(jewishDateInfo.isJewishLeapYear());
        return zmanim;
    }

    /**
     * This is a simple convenience method to check if the given date it null or not. If the date is not null, it will return exactly what was given.
     * However, if the date is null, it will change the date to a string that says "N/A" (Not Available).
     * @param date the date object to check if it is null
     * @return the given date if not null or a string if null
     */
    private Object checkNull(Date date) {
        if (date != null) {
            return date;
        } else {
            return "N/A";
        }
    }

    /**
     * uses the TimeZoneMap to get the current timezone ID based on the latitude and longitude
     */
    private void setTimeZoneID() {
        TimeZoneMap timeZoneMap = TimeZoneMap.forRegion(
                Math.floor(mLatitude), Math.floor(mLongitude),
                Math.ceil(mLatitude), Math.ceil(mLongitude));//trying to avoid using the forEverywhere() method
        mCurrentTimeZoneID = Objects.requireNonNull(timeZoneMap.getOverlappingTimeZone(mLatitude, mLongitude)).getZoneId();
    }

    /**
     * This method uses the Geocoder class to try and get the current location's name. I have
     * tried to make my results similar to the zmanim app by JGindin on the Play Store. In america,
     * it will get the current location by state and city. Whereas, in other areas of the world, it
     * will get the country and the city. Note that the Geocoder class might give weird results,
     * even in the same city.
     *
     * @return a string containing the name of the current city and state/country that the user
     * is located in.
     * @see Geocoder
     */
    private String getLocationAsName() {
        StringBuilder result = new StringBuilder();
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(mLatitude, mLongitude, 1);
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
     * This method checks if the user has already setup the elevation from the last time he started
     * the app. If he has not, it will startup the setup activity. If he has setup the elevation
     * amount, then it checks if the user is in the same city as the last time he setup the app
     * based on the getLocationAsName method. If the user is in the same city, all is good. If the
     * user is in another city, we make an AlertDialog to warn the user that the elevation data
     * MIGHT not be accurate.
     *
     * @see #getLocationAsName()
     */
    private void getAndAffirmLastElevationData() {
        String lastLocation = mSharedPreferences.getString("lastLocation", "");

        String message =
                "The elevation and visible sunrise data change depending on the city you are in. " +
                        "Therefore, it is recommended that you update your elevation and visible sunrise" +
                        " data according to your current location. " + "\n\n" +
                        "Last Location: " + lastLocation + "\n" +
                        "Current Location: " + mCurrentLocationName + "\n\n" +
                        "Would you like to rerun the setup now?";

        if (mSharedPreferences.getBoolean("askagain", true)) {//only prompt user if he has not asked to be left alone
            if (!lastLocation.isEmpty()) {//only check after the app has been setup before
                mElevation = mSharedPreferences.getFloat("elevation", 0);//get the last value
                if (!lastLocation.equals(mCurrentLocationName)) {//user should update his elevation in another city

                    new AlertDialog.Builder(this)
                            .setTitle("You are not in the same city as the last time that you " +
                                    "setup the app!")
                            .setMessage(message)
                            .setPositiveButton("Yes", (dialogInterface, i) ->
                                    mSetupLauncher.launch(new Intent(this, SetupChooserActivity.class)))
                            .setNegativeButton("No", (dialogInterface, i) -> Toast.makeText(
                                    this, "Using visible sunrise and elevation for your last location", Toast.LENGTH_LONG)
                                    .show())
                            .setNeutralButton("Do not ask again", (dialogInterface, i) -> {
                                mSharedPreferences.edit().putBoolean("askagain", false).apply();
                                Toast.makeText(this, "Your current elevation is: " + mElevation,
                                        Toast.LENGTH_LONG).show();
                            })
                            .show();
                }
            }
        }
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
                        if (!mNetworkLocationServiceIsDisabled) {
                            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
                        }
                        if (!mGPSLocationServiceIsDisabled) {
                            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
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
                                Toast.makeText(this, "Trying to acquire your location...", Toast.LENGTH_SHORT)
                                        .show();//show a toast in order for the user to know that the app is working
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
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        resolveCurrentLocationName();
    }

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
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions,
                                           @NonNull @NotNull int[] grantResults) {
        if (requestCode == 1) {
            if (permissions.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                acquireLatitudeAndLongitude();
                if (!initialized) {
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
                        if (!initialized) {
                            initializeMainView();
                        } else {
                            setTimeZoneID();
                            instantiateZmanimCalendar();
                            mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
                            getAndAffirmLastElevationData();
                        }
                    }
                })
                .setNeutralButton("Use location", (dialog, which) -> {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean("useZipcode", false).apply();
                    acquireLatitudeAndLongitude();
                    setTimeZoneID();
                    instantiateZmanimCalendar();
                    mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
                    getAndAffirmLastElevationData();
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
            address = geocoder.getFromLocationName(zipcode, 1);
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
            instantiateZmanimCalendar();
            mMainRecyclerView.setAdapter(new ZmanAdapter(getZmanimList()));
            getAndAffirmLastElevationData();
            return true;
        } else if (id == R.id.enterZipcode) {
            createZipcodeDialog();
            return true;
        } else if (id == R.id.shabbat_mode) {
            if (!mShabbatMode) {
                startShabbatMode();
                item.setChecked(true);
            } else {
                endShabbatMode();
                item.setChecked(false);
            }
            return true;
        } else if (id == R.id.molad) {
            mSetupLauncher.launch(new Intent(this, MoladActivity.class));
            return true;
        } else if (id == R.id.setupChooser) {
            mSetupLauncher.launch(new Intent(this, SetupChooserActivity.class)
                    .putExtra("fromMenu",true));
            return true;
        } else if (id == R.id.fullSetup) {
            mSetupLauncher.launch(new Intent(this, FullSetupActivity.class));
            return true;
        } else if (id == R.id.settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (id == R.id.about) {
            //startActivity(new Intent(MainActivity.this, AboutActivity.class));//TODO
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

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(MainActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT).show();
    }
}