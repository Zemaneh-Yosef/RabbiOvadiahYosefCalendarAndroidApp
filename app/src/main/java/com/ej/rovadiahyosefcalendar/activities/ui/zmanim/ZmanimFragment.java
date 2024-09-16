package com.ej.rovadiahyosefcalendar.activities.ui.zmanim;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.ALARM_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.dafYomiYerushalmiStartDate;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mCurrentDateShown;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mHebrewDateFormatter;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mNavView;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mViewPager;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.materialToolbar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sElevation;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLastTimeUserWasInApp;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLongitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSettingsPreferences;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSetupLauncher;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSharedPreferences;
import static com.ej.rovadiahyosefcalendar.classes.CalendarDrawable.getCurrentCalendarDrawableDark;
import static com.ej.rovadiahyosefcalendar.classes.CalendarDrawable.getCurrentCalendarDrawableLight;
import static com.ej.rovadiahyosefcalendar.classes.ZmanimFactory.addZmanim;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.CalendarChooserActivity;
import com.ej.rovadiahyosefcalendar.activities.FullSetupActivity;
import com.ej.rovadiahyosefcalendar.activities.GetUserLocationWithMapActivity;
import com.ej.rovadiahyosefcalendar.activities.JerusalemDirectionMapsActivity;
import com.ej.rovadiahyosefcalendar.activities.MoladActivity;
import com.ej.rovadiahyosefcalendar.activities.NetzActivity;
import com.ej.rovadiahyosefcalendar.activities.SettingsActivity;
import com.ej.rovadiahyosefcalendar.classes.CalendarDrawable;
import com.ej.rovadiahyosefcalendar.classes.ChaiTables;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesScraper;
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocaleChecker;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.PrefToWatchSender;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanAdapter;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory;
import com.ej.rovadiahyosefcalendar.databinding.FragmentZmanimBinding;
import com.ej.rovadiahyosefcalendar.notifications.DailyNotifications;
import com.ej.rovadiahyosefcalendar.notifications.NextZmanCountdownNotification;
import com.ej.rovadiahyosefcalendar.notifications.OmerNotifications;
import com.ej.rovadiahyosefcalendar.notifications.ZmanimNotifications;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.util.GeoLocation;
import com.kosherjava.zmanim.util.ZmanimFormatter;

import org.apache.commons.lang3.time.DateUtils;
import org.shredzone.commons.suncalc.MoonTimes;

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
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZmanimFragment extends Fragment implements Consumer<Location> {

    public static boolean sShabbatMode;
    public static boolean sUserIsOffline;
    private boolean mIsZmanimInHebrew;
    private boolean mIsZmanimEnglishTranslated;
    private static final int TWENTY_FOUR_HOURS_IN_MILLI = 86_400_000;
    private int mCurrentPosition;//current position in the list of zmanim to return to
    private boolean mUpdateTablesDialogShown;

    private FragmentZmanimBinding binding;
    private Context mContext;
    private FragmentActivity mActivity;

    //android views:
    private View mLayout;
    private TextView mShabbatModeBanner;
    private RecyclerView mMainRecyclerView;
    private Button mNextDate;
    private Button mPreviousDate;
    private Button mCalendarButton;

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
    private TextView mWeeklyHaftorah;

    //This array holds the zmanim that we want to display in the announcements section of the weekly view:
    private ArrayList<String> mZmanimForAnnouncements;

    /**
     * The zman that is coming up next.
     */
    public static Date sNextUpcomingZman = null;

    private LocationResolver mLocationResolver;
    private final ZmanimFormatter mZmanimFormatter = new ZmanimFormatter(TimeZone.getDefault());
    public static ActivityResultLauncher<Intent> sNotificationLauncher;
    private Handler mHandler = null;
    private Runnable mZmanimUpdater;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPrefListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = requireActivity();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentZmanimBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        super.onCreate(savedInstanceState);

        mLayout = binding.mainLayout;
        mHandler = new Handler(mContext.getMainLooper());
        initMenu();
        initNotifResult();
        setupShabbatModeBanner();

        mZmanimFormatter.setTimeFormat(ZmanimFormatter.SEXAGESIMAL_SECONDS_FORMAT);
        mLocationResolver = new LocationResolver(mContext, mActivity);
        if (mLocationName == null) {// if first view is null
            findAllWeeklyViews();
        }

        if (sSharedPreferences.getBoolean("isSetup", false)) {
            initMainView();
        }
        sharedPrefListener = (prefs, key) -> {
            if (key != null && key.equals("isSetup")) {
                initMainView();
            }
        };
        sSharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefListener);

        Intent intent = mActivity.getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.equals("text/plain")) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // Update UI to reflect text being shared
                    // We set the zipcode location to what was sent to us
                    sSharedPreferences.edit().putBoolean("useLocation1", false)
                            .putBoolean("useLocation2", false)
                            .putBoolean("useLocation3", false)
                            .putBoolean("useLocation4", false)
                            .putBoolean("useLocation5", false)
                            .apply();
                    SharedPreferences.Editor editor = sSharedPreferences.edit();
                    editor.putBoolean("useAdvanced", false).apply();
                    editor.putBoolean("useZipcode", true).apply();
                    editor.putString("Zipcode", sharedText).apply();
                    mLocationResolver = new LocationResolver(mContext, mActivity);
                    mLocationResolver.getLatitudeAndLongitudeFromSearchQuery();
                    if (sSharedPreferences.getBoolean("useElevation", true)) {
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
                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                        updateWeeklyZmanim();
                    } else {
                        updateDailyZmanim();
                    }
                    checkIfUserIsInIsraelOrNot();
                    saveGeoLocationInfo();
                    setNotifications();
                    PrefToWatchSender.send(mContext);
                }
            }
        }

        return root;
    }

    private void initNotifResult() {
        sNotificationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> setNotifications()
        );
    }

    /**
     * This method initializes the main view. This method should only be called when we are able to initialize the @{link mROZmanimCalendar} object
     * with the correct latitude, longitude, elevation, and timezone.
     */
    private void initMainView() {
        if (sLatitude == 0 && sLongitude == 0) {//initMainView() is called after the location is acquired, however, this is a failsafe
            mLocationResolver.acquireLatitudeAndLongitude(this);
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
        askForRealTimeNotificationPermissions();
        checkIfUserIsInIsraelOrNot();
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
        if (binding != null) {
            mPreviousDate = binding.prevDay;
            mPreviousDate.setOnClickListener(v -> {
                if (!sShabbatMode) {
                    mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();//just get a calendar object with the same date as the current one
                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                        mCurrentDateShown.add(Calendar.DATE, -7);//subtract seven days
                    } else {
                        mCurrentDateShown.add(Calendar.DATE, -1);//subtract one day
                    }
                    mROZmanimCalendar.setCalendar(mCurrentDateShown);
                    mJewishDateInfo.setCalendar(mCurrentDateShown);
                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                        updateWeeklyZmanim();
                    } else {
                        updateDailyZmanim();
                    }
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                    seeIfTablesNeedToBeUpdated(true);
                }
            });
        }
    }

    /**
     * Sets up the next day button
     */
    private void setupNextDayButton() {
        if (binding != null) {
            mNextDate = binding.nextDay;
            mNextDate.setOnClickListener(v -> {
                if (!sShabbatMode) {
                    mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                        mCurrentDateShown.add(Calendar.DATE, 7);//add seven days
                    } else {
                        mCurrentDateShown.add(Calendar.DATE, 1);//add one day
                    }
                    mROZmanimCalendar.setCalendar(mCurrentDateShown);
                    mJewishDateInfo.setCalendar(mCurrentDateShown);
                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                        updateWeeklyZmanim();
                    } else {
                        updateDailyZmanim();
                    }
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                    seeIfTablesNeedToBeUpdated(true);
                }
            });
        }
    }

    /**
     * Setup the calendar button to show a DatePickerDialog with an additional button to switch the calendar to the hebrew one.
     */
    private void setupCalendarButton() {
        if (binding != null) {
            mCalendarButton = binding.calendar;
            mCalendarButton.setOnClickListener(v -> {
                if (!sShabbatMode) {
                    MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                    MaterialDatePicker<Long> materialDatePicker = builder
                            .setPositiveButtonText(R.string.ok)
                            .setNegativeButtonText(R.string.switch_calendar)
                            .setSelection(mCurrentDateShown.getTimeInMillis())// can be in local timezone
                            .build();
                    materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                        Calendar epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        epoch.setTimeInMillis(selection);
                        mCurrentDateShown.set(
                                epoch.get(Calendar.YEAR),
                                epoch.get(Calendar.MONTH),
                                epoch.get(Calendar.DATE),
                                epoch.get(Calendar.HOUR_OF_DAY),
                                epoch.get(Calendar.MINUTE)
                        );
                        mROZmanimCalendar.setCalendar(mCurrentDateShown);
                        mJewishDateInfo.setCalendar(mCurrentDateShown);
                        if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                            updateWeeklyZmanim();
                        } else {
                            updateDailyZmanim();
                        }
                        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                        seeIfTablesNeedToBeUpdated(true);
                    });
                    DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
                        Calendar mUserChosenDate = Calendar.getInstance();
                        mUserChosenDate.set(year, month, day);
                        mROZmanimCalendar.setCalendar(mUserChosenDate);
                        mJewishDateInfo.setCalendar(mUserChosenDate);
                        mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
                        if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                            updateWeeklyZmanim();
                        } else {
                            updateDailyZmanim();
                        }
                        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                        seeIfTablesNeedToBeUpdated(true);
                    };
                    materialDatePicker.addOnNegativeButtonClickListener(selection -> {
                        HebrewDayMonthYearPickerDialog hdmypd = new HebrewDayMonthYearPickerDialog(materialDatePicker, mActivity.getSupportFragmentManager(), mJewishDateInfo.getJewishCalendar());
                        hdmypd.updateDate(mJewishDateInfo.getJewishCalendar().getGregorianYear(),
                                mJewishDateInfo.getJewishCalendar().getGregorianMonth(),
                                mJewishDateInfo.getJewishCalendar().getGregorianDayOfMonth());
                        hdmypd.setListener(onDateSetListener);
                        hdmypd.show(mActivity.getSupportFragmentManager(), null);
                    });
                    materialDatePicker.show(mActivity.getSupportFragmentManager(), null);
                }
            });

            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
        }
    }

    /**
     * This method will automatically update the tables if the user has setup the app before for the current location.
     * @param fromButton if the method is called from the buttons, it will not ask more than once if the user wants to update the tables.
     */
    private void seeIfTablesNeedToBeUpdated(boolean fromButton) {
        if (sSharedPreferences.getBoolean("isSetup", false) //only check after the app has been setup before
                && sSharedPreferences.getBoolean("UseTable" + sCurrentLocationName, false)) { //and only if the tables are being used

            if (ChaiTables.visibleSunriseFileDoesNotExist(mActivity.getExternalFilesDir(null), sCurrentLocationName, mJewishDateInfo.getJewishCalendar())) {
                if (!mUpdateTablesDialogShown) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext);
                    builder.setTitle(R.string.update_tables);
                    builder.setMessage(R.string.the_visible_sunrise_tables_for_the_current_location_and_year_need_to_be_updated_do_you_want_to_update_the_tables_now);
                    builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                        String chaitablesURL = sSharedPreferences.getString("chaitablesLink" + sCurrentLocationName, "");
                        if (!chaitablesURL.isEmpty()) {//it should not be empty if the user has set up the app, but it is good to check
                            String hebrewYear = String.valueOf(mJewishDateInfo.getJewishCalendar().getJewishYear());
                            Pattern pattern = Pattern.compile("&cgi_yrheb=\\d{4}");
                            Matcher matcher = pattern.matcher(chaitablesURL);
                            if (matcher.find()) {
                                chaitablesURL = chaitablesURL.replace(matcher.group(), "&cgi_yrheb=" + hebrewYear);//replace the year in the URL with the current year
                            }
                            ChaiTablesScraper scraper = new ChaiTablesScraper();
                            scraper.setDownloadSettings(chaitablesURL, mActivity.getExternalFilesDir(null), mJewishDateInfo.getJewishCalendar());
                            scraper.start();
                            try {
                                scraper.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
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

    @SuppressLint("ClickableViewAccessibility")
    private void setupRecyclerViewAndTextViews() {
        SwipeRefreshLayout swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(() -> new Thread(() -> {
            Looper.prepare();
            if (mLocationResolver == null) {
                mLocationResolver = new LocationResolver(mContext, mActivity);
            }
            mLocationResolver.acquireLatitudeAndLongitude(this);
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
                sSharedPreferences.edit().putString("Full"+mROZmanimCalendar.getGeoLocation().getLocationName(), "").apply();
                mActivity.runOnUiThread(() -> {
                    updateDailyZmanim();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                });
            }
            swipeRefreshLayout.setRefreshing(false);
            Objects.requireNonNull(Looper.myLooper()).quit();
        }).start());
        mMainRecyclerView = binding.mainRV;
        mMainRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mMainRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        if (mIsZmanimInHebrew) {
            mMainRecyclerView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        if (sSharedPreferences.getBoolean("weeklyMode", false)) {
            showWeeklyTextViews();
            updateWeeklyZmanim();
        } else {
            hideWeeklyTextViews();
            updateDailyZmanim();
        }
    }

    private void checkIfUserIsInIsraelOrNot() {
        if (sSharedPreferences.getBoolean("neverAskInIsraelOrNot", false)) {return;}

        if (sCurrentTimeZoneID.equals("Asia/Jerusalem")) {//user is in or near israel now
            sSharedPreferences.edit().putBoolean("askedNotInIsrael", false).apply();//reset that we asked outside israel for next time
            if (!sSharedPreferences.getBoolean("inIsrael", false) && //user was not in israel before
                    !sSharedPreferences.getBoolean("askedInIsrael", false)) {//and we did not ask already
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.are_u_in_israel)
                        .setMessage(R.string.if_you_are_in_israel_now_please_confirm_below)
                        .setPositiveButton(R.string.yes_i_am_in_israel, (dialog, which) -> {
                            sSharedPreferences.edit().putBoolean("inIsrael", true).apply();
                            sSettingsPreferences.edit().putBoolean("LuachAmudeiHoraah", false).apply();
                            mJewishDateInfo = new JewishDateInfo(true);
                            initMenu();
                            Toast.makeText(mContext, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateDailyZmanim();
                            }
                        })
                        .setNegativeButton(R.string.no_i_am_not_in_israel, (dialog, which) -> {
                            sSharedPreferences.edit().putBoolean("askedInIsrael", true).apply();//save that we asked already
                            dialog.dismiss();
                        })
                        .setNeutralButton(R.string.do_not_ask_me_again, (dialog, which) -> {
                            sSharedPreferences.edit().putBoolean("neverAskInIsraelOrNot", true).apply();//save that we should never ask again
                            dialog.dismiss();
                        })
                        .show();
            }
        } else {//user is not in israel
            sSharedPreferences.edit().putBoolean("askedInIsrael", false).apply();//reset that we asked in israel
            if (sSharedPreferences.getBoolean("inIsrael", false) && //user was in israel before
                    !sSharedPreferences.getBoolean("askedInNotIsrael", false)) {//and we did not ask already
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.have_you_left_israel)
                        .setMessage(R.string.if_you_are_not_in_israel_now_please_confirm_below_otherwise_ignore_this_message)
                        .setPositiveButton(R.string.yes_i_have_left_israel, (dialog, which) -> {
                            sSharedPreferences.edit().putBoolean("inIsrael", false).apply();
                            mJewishDateInfo = new JewishDateInfo(false);
                            Toast.makeText(mContext, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateDailyZmanim();
                            }
                            startActivity(new Intent(mContext, CalendarChooserActivity.class));
                        })
                        .setNegativeButton(R.string.no_i_have_not_left_israel, (dialog, which) -> {
                            sSharedPreferences.edit().putBoolean("askedInNotIsrael", true).apply();//save that we asked
                            dialog.dismiss();
                        })
                        .setNeutralButton(R.string.do_not_ask_me_again, (dialog, which) -> {
                            sSharedPreferences.edit().putBoolean("neverAskInIsraelOrNot", true).apply();//save that we should never ask again
                            dialog.dismiss();
                        })
                        .show();
            }
        }
    }

    private void showWeeklyTextViews() {
        LinearLayout mainWeekly = binding.mainWeeklyLayout;

        mEnglishMonthYear.setVisibility(View.VISIBLE);
        mLocationName.setVisibility(View.VISIBLE);
        mHebrewMonthYear.setVisibility(View.VISIBLE);
        mainWeekly.setVisibility(View.VISIBLE);
        mWeeklyParsha.setVisibility(View.VISIBLE);
        mWeeklyHaftorah.setVisibility(View.VISIBLE);
        mMainRecyclerView.setVisibility(View.GONE);
        SwipeRefreshLayout swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setVisibility(View.GONE);

        // 800dp is perfect for the weekly view. However, smaller screens have less space, so we need to shrink it
        float screenHeight = getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density;
        if (screenHeight < 750) {
            binding.hebrewDay.setTextSize(12);
            binding.hebrewDay2.setTextSize(12);
            binding.hebrewDay3.setTextSize(12);
            binding.hebrewDay4.setTextSize(12);
            binding.hebrewDay5.setTextSize(12);
            binding.hebrewDay6.setTextSize(12);
            binding.hebrewDay7.setTextSize(12);

            binding.hebrewDate.setTextSize(16);
            binding.hebrewDate2.setTextSize(16);
            binding.hebrewDate3.setTextSize(16);
            binding.hebrewDate4.setTextSize(16);
            binding.hebrewDate5.setTextSize(16);
            binding.hebrewDate6.setTextSize(16);
            binding.hebrewDate7.setTextSize(16);
        }
    }

    private void hideWeeklyTextViews() {
        LinearLayout mainWeekly = binding.mainWeeklyLayout;

        mEnglishMonthYear.setVisibility(View.GONE);
        mLocationName.setVisibility(View.GONE);
        mHebrewMonthYear.setVisibility(View.GONE);
        mainWeekly.setVisibility(View.GONE);
        mWeeklyParsha.setVisibility(View.GONE);
        mWeeklyHaftorah.setVisibility(View.GONE);
        mMainRecyclerView.setVisibility(View.VISIBLE);
        SwipeRefreshLayout swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void findAllWeeklyViews() {
        mLocationName = binding.locationName;
        mEnglishMonthYear = binding.englishMonthYear;
        mHebrewMonthYear = binding.hebrewMonthYear;
        //there are 7 of these sets of views
        mListViews[0] = binding.zmanim1;
        mSunday[1] = binding.announcements;
        mSunday[2] = binding.hebrewDay;
        mSunday[3] = binding.hebrewDate;
        mSunday[4] = binding.englishDay;
        mSunday[5] = binding.englishDateNumber;

        mListViews[1] = binding.zmanim2;
        mMonday[1] = binding.announcements2;
        mMonday[2] = binding.hebrewDay2;
        mMonday[3] = binding.hebrewDate2;
        mMonday[4] = binding.englishDay2;
        mMonday[5] = binding.englishDateNumber2;

        mListViews[2] = binding.zmanim3;
        mTuesday[1] = binding.announcements3;
        mTuesday[2] = binding.hebrewDay3;
        mTuesday[3] = binding.hebrewDate3;
        mTuesday[4] = binding.englishDay3;
        mTuesday[5] = binding.englishDateNumber3;

        mListViews[3] = binding.zmanim4;
        mWednesday[1] = binding.announcements4;
        mWednesday[2] = binding.hebrewDay4;
        mWednesday[3] = binding.hebrewDate4;
        mWednesday[4] = binding.englishDay4;
        mWednesday[5] = binding.englishDateNumber4;

        mListViews[4] = binding.zmanim5;
        mThursday[1] = binding.announcements5;
        mThursday[2] = binding.hebrewDay5;
        mThursday[3] = binding.hebrewDate5;
        mThursday[4] = binding.englishDay5;
        mThursday[5] = binding.englishDateNumber5;

        mListViews[5] = binding.zmanim6;
        mFriday[1] = binding.announcements6;
        mFriday[2] = binding.hebrewDay6;
        mFriday[3] = binding.hebrewDate6;
        mFriday[4] = binding.englishDay6;
        mFriday[5] = binding.englishDateNumber6;

        mListViews[6] = binding.zmanim7;
        mSaturday[1] = binding.announcements7;
        mSaturday[2] = binding.hebrewDay7;
        mSaturday[3] = binding.hebrewDate7;
        mSaturday[4] = binding.englishDay7;
        mSaturday[5] = binding.englishDateNumber7;

        mWeeklyParsha = binding.weeklyParsha;
        mWeeklyHaftorah = binding.weeklyHaftorah;
        updateWeeklyTextViewTextColor();
    }

    private void updateWeeklyTextViewTextColor() {
        if (sSharedPreferences.getBoolean("customTextColor", false)) {
            int textColor = sSharedPreferences.getInt("tColor", 0xFFFFFFFF);
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

            mWeeklyParsha.setTextColor(textColor);
            mWeeklyHaftorah.setTextColor(textColor);
        }
    }

    private void setZmanimLanguageBools() {
        if (sSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            mIsZmanimInHebrew = true;
            mIsZmanimEnglishTranslated = false;
        } else if (sSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
            mIsZmanimInHebrew = false;
            mIsZmanimEnglishTranslated = true;
        } else {
            mIsZmanimInHebrew = false;
            mIsZmanimEnglishTranslated = false;
        }
    }

    private void instantiateZmanimCalendar() {
        mROZmanimCalendar = new ROZmanimCalendar(new GeoLocation(
                sCurrentLocationName,
                sLatitude,
                sLongitude,
                sElevation,
                TimeZone.getTimeZone(sCurrentTimeZoneID == null ? TimeZone.getDefault().getID() : sCurrentTimeZoneID)));
        mROZmanimCalendar.setExternalFilesDir(mActivity.getExternalFilesDir(null));
        String candles = sSettingsPreferences.getString("CandleLightingOffset", "20");
        if (candles.isEmpty()) {
            candles = "20";
        }
        mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(candles));
        String shabbat = sSettingsPreferences.getString("EndOfShabbatOffset", sSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40");
        if (shabbat.isEmpty()) {// for some reason this is happening
            shabbat = "40";
        }
        mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(shabbat));
        if (sSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
            mROZmanimCalendar.setAteretTorahSunsetOffset(30);
        }
    }

    /**
     * This method saves the information needed to restore a GeoLocation object in the notification classes.
     */
    private void saveGeoLocationInfo() {//needed for notifications
        SharedPreferences.Editor editor = sSharedPreferences.edit();
        editor.putString("name", sCurrentLocationName).apply();
        editor.putLong("lat", Double.doubleToRawLongBits(sLatitude)).apply();//see here: https://stackoverflow.com/a/18098090/13593159
        editor.putLong("long", Double.doubleToRawLongBits(sLongitude)).apply();
        editor.putString("timezoneID", sCurrentTimeZoneID).apply();
    }

    private void askForRealTimeNotificationPermissions() {
        if (ActivityCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(mContext, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            if (!sSharedPreferences.getBoolean("askedForRealtimeNotifications", false)
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext);
                builder.setTitle(R.string.would_you_like_to_receive_real_time_notifications_for_zmanim);
                builder.setMessage(R.string.if_you_would_like_to_receive_real_time_zmanim_notifications);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (ActivityCompat.checkSelfPermission(mContext, ACCESS_BACKGROUND_LOCATION) != PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(mActivity, new String[]{ACCESS_BACKGROUND_LOCATION}, 1);
                    }
                    sSharedPreferences.edit().putBoolean("askedForRealtimeNotifications", true).apply();
                });
                builder.setNegativeButton(R.string.no, (dialog, which) -> {
                    sSharedPreferences.edit().putBoolean("askedForRealtimeNotifications", true).apply();
                    dialog.dismiss();
                });
                if (!mActivity.isFinishing()) {
                    builder.show();
                }
            }
        }
    }

    /**
     * This method will be called every time the user opens the app. It will reset the notifications every time the app is opened since the user might
     * have changed his location or settings.
     */
    private void setNotifications() {
        if (sSettingsPreferences.getBoolean("zmanim_notifications", true)) {//if the user wants notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {// ask for permission to send notifications for newer versions of android ughhhh...
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.SCHEDULE_EXACT_ALARM}, 1);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !((AlarmManager) mContext.getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()) {// more annoying android permission garbage
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext);
                builder.setTitle(R.string.zmanim_notifications_will_not_work);
                builder.setMessage(R.string.if_you_would_like_to_receive_zmanim_notifications);
                builder.setCancelable(false);
                builder.setPositiveButton(mContext.getString(R.string.yes), (dialog, which) -> sNotificationLauncher.launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+ mContext.getPackageName()))));
                builder.setNegativeButton(mContext.getString(R.string.no), (dialog, which) -> dialog.dismiss());
                if (!mActivity.isFinishing()) {
                    builder.show();
                }
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
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(mContext, 0,
                new Intent(mContext, DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
        am.cancel(dailyPendingIntent);//cancel any previous alarms
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dailyPendingIntent);

        if (mROZmanimCalendar.getTzeit() != null) {
            if (sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
                calendar.setTimeInMillis(mROZmanimCalendar.getTzeitAmudeiHoraah().getTime());
            } else {
                calendar.setTimeInMillis(mROZmanimCalendar.getTzeit().getTime());
            }
        }
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mContext, OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        am.cancel(omerPendingIntent);//cancel any previous alarms
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), omerPendingIntent);

        Intent zmanIntent = new Intent(mContext, ZmanimNotifications.class);
        PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(mContext,0, zmanIntent, PendingIntent.FLAG_IMMUTABLE);
        try {
            zmanimPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private void updateDailyZmanim() {
        mMainRecyclerView.setAdapter(new ZmanAdapter(mContext, getZmanimList()));
    }

    private void initMenu() {
        if (materialToolbar == null) {
            return;
        }
        if (LocaleChecker.isLocaleHebrew()) {
            materialToolbar.setSubtitle("");
        } else {
            materialToolbar.setTitle(mContext.getString(R.string.app_name));
        }
        materialToolbar.getMenu().clear();
        materialToolbar.inflateMenu(R.menu.menu_main);
        materialToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.search_for_a_place) {
                sSharedPreferences.edit().putBoolean("shouldRefresh", true).apply();
                startActivity(new Intent(mContext, GetUserLocationWithMapActivity.class));
                return true;
            } else if (id == R.id.shabbat_mode) {
                if (!sShabbatMode && mJewishDateInfo != null && mROZmanimCalendar != null && mMainRecyclerView != null) {
                    mCurrentDateShown.setTime(new Date());
                    mJewishDateInfo.setCalendar(new GregorianCalendar());
                    mROZmanimCalendar.setCalendar(new GregorianCalendar());
                    startShabbatMode();
                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                        updateWeeklyZmanim();
                    } else {
                        updateDailyZmanim();
                    }
                } else {
                    endShabbatMode();
                }
                item.setChecked(sShabbatMode);
                return true;
            } else if (id == R.id.weekly_mode) {
                sSharedPreferences.edit().putBoolean("weeklyMode", !sSharedPreferences.getBoolean("weeklyMode", false)).apply();
                item.setChecked(sSharedPreferences.getBoolean("weeklyMode", false));//save the state of the menu item
                if (mMainRecyclerView == null) {
                    return true;// Prevent a crash
                }
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    showWeeklyTextViews();
                    updateWeeklyZmanim();
                } else {
                    hideWeeklyTextViews();
                    updateDailyZmanim();
                }
                return true;
            } else if (id == R.id.use_elevation) {
                sSharedPreferences.edit().putBoolean("useElevation", !sSharedPreferences.getBoolean("useElevation", false)).apply();
                item.setChecked(sSharedPreferences.getBoolean("useElevation", false));//save the state of the menu item
                resolveElevationAndVisibleSunrise();
                instantiateZmanimCalendar();
                setNextUpcomingZman();
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
                }
                return true;
            } else if (id == R.id.jerDirection) {
                startActivity(new Intent(mContext, JerusalemDirectionMapsActivity.class));
                return true;
            } else if (id == R.id.netzView) {
                startActivity(new Intent(mContext, NetzActivity.class));
                return true;
            } else if (id == R.id.molad) {
                startActivity(new Intent(mContext, MoladActivity.class));
                return true;
            } else if (id == R.id.fullSetup) {
                sSharedPreferences.edit().putBoolean("shouldRefresh", true).apply();
                sSetupLauncher.launch(new Intent(mContext, FullSetupActivity.class).putExtra("fromMenu",true));
                return true;
            } else if (id == R.id.settings) {
                sSharedPreferences.edit().putBoolean("shouldRefresh", true).apply();
                startActivity(new Intent(mContext, SettingsActivity.class));
                return true;
            } else if (id == R.id.website) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.royzmanim.com"));
                startActivity(browserIntent);
                return true;
            }
            return false;
        });
        Menu menu = materialToolbar.getMenu();
        MenuCompat.setGroupDividerEnabled(menu, true);
        menu.findItem(R.id.shabbat_mode).setChecked(sShabbatMode);
        menu.findItem(R.id.weekly_mode).setChecked(sSharedPreferences.getBoolean("weeklyMode", false));
        menu.findItem(R.id.use_elevation).setChecked(sSharedPreferences.getBoolean("useElevation", true));
        menu.findItem(R.id.use_elevation).setVisible(!sSharedPreferences.getBoolean("LuachAmudeiHoraah", false));
    }

    /**
     * This is the main method for updating the Zmanim in the list view. It is called everytime the user changes the date or updates
     * any setting that affects the zmanim. This method returns a list of ZmanListEntry objects that will be used to populate the list view.
     * @return List of ZmanListEntry objects that will be used to populate the list view.
     * @see ZmanListEntry
     * */
    private List<ZmanListEntry> getZmanimList() {
        List<ZmanListEntry> zmanim = new ArrayList<>();

        String locationName = sSharedPreferences.getString("Full"+mROZmanimCalendar.getGeoLocation().getLocationName(), "");
        if (locationName.isEmpty()) {
            locationName = mLocationResolver.getFullLocationName();
            sSharedPreferences.edit().putString("Full"+mROZmanimCalendar.getGeoLocation().getLocationName(), locationName).apply();
        }
        if (locationName != null && locationName.isEmpty()) {//if it's still empty, use backup. NPE was thrown here for some reason
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

        String haftorah = mJewishDateInfo.getThisWeeksHaftarah();
        if (!haftorah.isEmpty()) {
            zmanim.add(new ZmanListEntry(haftorah));
        }

        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        if (sSettingsPreferences.getBoolean("showShabbatMevarchim", true)) {
            if (mJewishDateInfo.getJewishCalendar().isShabbosMevorchim()) {
                zmanim.add(new ZmanListEntry("שבת מברכים"));
            }
        }
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());//reset

        if (LocaleChecker.isLocaleHebrew()) {
            zmanim.add(new ZmanListEntry(mROZmanimCalendar.getCalendar()
                    .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())));
        } else {
            zmanim.add(new ZmanListEntry(mROZmanimCalendar.getCalendar()
                    .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                    + " / " +
                    mROZmanimCalendar.getCalendar()
                            .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, new Locale("he","IL"))));
        }

        String day = mJewishDateInfo.getSpecialDay(false);
        if (!day.isEmpty()) {
            zmanim.add(new ZmanListEntry(day));
        }
        String dayOfOmer = mJewishDateInfo.addDayOfOmer("");
        if (!dayOfOmer.isEmpty()) {
            zmanim.add(new ZmanListEntry(dayOfOmer));
        }

        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.ROSH_HASHANA &&
                mJewishDateInfo.isShmitaYear()) {
            zmanim.add(new ZmanListEntry(mContext.getString(R.string.this_year_is_a_shmita_year)));
        }

        if (mJewishDateInfo.is3Weeks()) {
            if (mJewishDateInfo.is9Days()) {
                if (mJewishDateInfo.isShevuahShechalBo()) {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.shevuah_shechal_bo)));
                } else {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.nine_days)));
                }
            } else {
                zmanim.add(new ZmanListEntry(mContext.getString(R.string.three_weeks)));
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
                MoonTimes moonTimes = MoonTimes.compute()
                        .on(mCurrentDateShown.getTime())
                        .at(sLatitude, sLongitude)
                        .timezone(sCurrentTimeZoneID)
                        .execute();
                if (moonTimes.isAlwaysUp()) {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.the_moon_is_up_all_night)));
                } else if (moonTimes.isAlwaysDown()) {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.there_is_no_moon_tonight)));
                } else {
                    SimpleDateFormat moonFormat;
                    if (LocaleChecker.isLocaleHebrew()) {
                        moonFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
                    } else {
                        moonFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    }
                    String moonRiseSet = "";
                    if (moonTimes.getRise() != null) {
                        moonRiseSet += mContext.getString(R.string.moonrise) + moonFormat.format(moonTimes.getRise());
                    }
                    if (moonTimes.getSet() != null) {
                        if (!moonRiseSet.isEmpty()) {
                            moonRiseSet += " - ";
                        }
                        moonRiseSet += mContext.getString(R.string.moonset) + moonFormat.format(moonTimes.getSet());
                    }
                    if (!moonRiseSet.isEmpty()) {
                        zmanim.add(new ZmanListEntry(moonRiseSet));
                    }
            }
        }

        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            zmanim.add(new ZmanListEntry(mContext.getString(R.string.birchat_hachamah_is_said_today)));
        }

        String tekufaOpinions = sSettingsPreferences.getString("TekufaOpinions", "1");
        if (tekufaOpinions.equals("1") && !sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            addTekufaTime(zmanim, false);
        }
        if (tekufaOpinions.equals("2") || sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            addAmudeiHoraahTekufaTime(zmanim, false);
        }
        if (tekufaOpinions.equals("3")) {
            addAmudeiHoraahTekufaTime(zmanim, false);
            addTekufaTime(zmanim, false);
        }
        addTekufaLength(zmanim, tekufaOpinions);

        addZmanim(zmanim, false, sSettingsPreferences, sSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated);

        zmanim.add(new ZmanListEntry(mJewishDateInfo.getIsMashivHaruchOrMoridHatalSaid()
                + " / "
                + mJewishDateInfo.getIsBarcheinuOrBarechAleinuSaid()));

        if (!sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            zmanim.add(new ZmanListEntry(mContext.getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanisGra())));
            zmanim.add(new ZmanListEntry(mContext.getString(R.string.mg_a) + " (" + mContext.getString(R.string.ohr_hachaim) + ") " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanis72MinutesZmanis())));
        } else {
            long shaahZmanitMGA = mROZmanimCalendar.getTemporalHour(mROZmanimCalendar.getAlotAmudeiHoraah(), mROZmanimCalendar.getTzais72ZmanisAmudeiHoraah());
            zmanim.add(new ZmanListEntry(mContext.getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanisGra())));
            zmanim.add(new ZmanListEntry(mContext.getString(R.string.mg_a) + " (" + mContext.getString(R.string.amudei_horaah) + ") " + mZmanimFormatter.format(shaahZmanitMGA)));
        }

        if (sSettingsPreferences.getBoolean("ShowLeapYear", false)) {
            zmanim.add(new ZmanListEntry(mJewishDateInfo.isJewishLeapYear()));
        }

        if (sSettingsPreferences.getBoolean("ShowDST", false)) {
            if (mROZmanimCalendar.getGeoLocation().getTimeZone().inDaylightTime(mROZmanimCalendar.getSeaLevelSunrise())) {
                zmanim.add(new ZmanListEntry(mContext.getString(R.string.daylight_savings_time_is_on)));
            } else {
                zmanim.add(new ZmanListEntry(mContext.getString(R.string.daylight_savings_time_is_off)));
            }
        }

        if (sSettingsPreferences.getBoolean("ShowShmitaYear", false)) {
            if (mJewishDateInfo.isShmitaYear()) {
                zmanim.add(new ZmanListEntry(mContext.getString(R.string.this_year_is_a_shmita_year)));
            } else {
                zmanim.add(new ZmanListEntry(mContext.getString(R.string.this_year_is_not_a_shmita_year)));
            }
        }

        if (sSettingsPreferences.getBoolean("ShowElevation", false)) {
            zmanim.add(new ZmanListEntry(mContext.getString(R.string.elevation) + " " + sElevation + " " + mContext.getString(R.string.meters)));
        }

        return zmanim;
    }

    private void createBackgroundThreadForNextUpcomingZman() {
        Runnable nextZmanUpdater = () -> {
            setNextUpcomingZman();
            if (mMainRecyclerView != null && !sSharedPreferences.getBoolean("weeklyMode", false)) {
                mCurrentPosition = ((LinearLayoutManager) Objects.requireNonNull(mMainRecyclerView.getLayoutManager())).findFirstVisibleItemPosition();
                updateDailyZmanim();
                if (mCurrentPosition < Objects.requireNonNull(mMainRecyclerView.getAdapter()).getItemCount()) {
                    mMainRecyclerView.scrollToPosition(mCurrentPosition);
                }
            } else if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            }
            createBackgroundThreadForNextUpcomingZman();//start a new thread to update the next upcoming zman
        };
        if (sNextUpcomingZman != null) {
            mHandler.postDelayed(nextZmanUpdater,sNextUpcomingZman.getTime() - new Date().getTime() + 1_000);//add 1 second to make sure we don't get the same zman again
        }
    }

    public void setNextUpcomingZman() {
        sNextUpcomingZman = ZmanimFactory.getNextUpcomingZman(mCurrentDateShown, mROZmanimCalendar, mJewishDateInfo, sSettingsPreferences, sSharedPreferences, mIsZmanimInHebrew, mIsZmanimEnglishTranslated).getZman();
    }

    private String getAnnouncements() {
        StringBuilder announcements = new StringBuilder();

        String day = mJewishDateInfo.getSpecialDay(true);
        if (!day.isEmpty()) {
            announcements.append(day.replace("/ ","\n")).append("\n");
        }

        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        if (sSettingsPreferences.getBoolean("showShabbatMevarchim", true)) {
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
        if (!tachanun.equals(mContext.getString(R.string.there_is_tachanun_today))) {
            announcements.append(tachanun).append("\n");
        }

        String birchatLevana = mJewishDateInfo.getBirchatLevana();
        if (!birchatLevana.isEmpty() && !birchatLevana.contains("until") && !birchatLevana.contains("עד")) {
            announcements.append(birchatLevana).append("\n");
        }

        if (mJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            announcements.append(mContext.getString(R.string.birchat_hachamah_is_said_today)).append("\n");
        }

        List<ZmanListEntry> tekufa = new ArrayList<>();
        String tekufaOpinions = sSettingsPreferences.getString("TekufaOpinions", "1");
        if (tekufaOpinions.equals("1") && !sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            addTekufaTime(tekufa, true);
        }
        if (tekufaOpinions.equals("2") || sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
            addAmudeiHoraahTekufaTime(tekufa, true);
        }
        if (tekufaOpinions.equals("3")) {
            addAmudeiHoraahTekufaTime(tekufa, true);
            addTekufaTime(tekufa, true);
        }
        if (!tekufa.isEmpty()) {
            for (ZmanListEntry tekufaEntry : tekufa) {
                announcements.append(tekufaEntry.getTitle()).append("\n");
            }
        }

        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            if (YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar()) == null) {
                announcements.append(mContext.getString(R.string.no_daf_yomi_yerushalmi)).append("\n");
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
        if (LocaleChecker.isLocaleHebrew()) {
            hebrewDateFormatter.setHebrewFormat(true);
        }
        List<TextView[]> weeklyInfo = Arrays.asList(mSunday, mMonday, mTuesday, mWednesday, mThursday, mFriday, mSaturday);

        String month = mCurrentDateShown.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        String year = String.valueOf(mCurrentDateShown.get(Calendar.YEAR));

        String hebrewMonth = hebrewDateFormatter.formatMonth(mJewishDateInfo.getJewishCalendar())
                .replace("Tishrei", "Tishri")
                .replace("Teves", "Tevet");
        String hebrewYear = String.valueOf(mJewishDateInfo.getJewishCalendar().getJewishYear());
        if (LocaleChecker.isLocaleHebrew()) {
            hebrewYear = hebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getJewishYear());
        }

        for (int i = 0; i < 7; i++) {
            if (DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), new Date())) {
                weeklyInfo.get(i)[4].setBackgroundColor(mContext.getColor(R.color.dark_gold));
            } else {
                weeklyInfo.get(i)[4].setBackground(null);
            }
            StringBuilder announcements = new StringBuilder();
            mZmanimForAnnouncements = new ArrayList<>();//clear the list, it will be filled again in the getShortZmanim method
            mListViews[i].setAdapter(new ArrayAdapter<String>(mContext, R.layout.zman_list_view, getShortZmanim()) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);

                    TextView textView = view.findViewById(R.id.zman_in_list);

                    if (textView != null) {
                        if (sSharedPreferences.getBoolean("customTextColor", false)) {
                            textView.setTextColor(sSharedPreferences.getInt("tColor", 0xFFFFFFFF));
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
        if (LocaleChecker.isLocaleHebrew()) {
            if (!hebrewYear.equals(hebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getJewishYear()))) {
                hebrewYear += " / " + hebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getJewishYear());
            }
        } else {
            if (!hebrewYear.equals(String.valueOf(mJewishDateInfo.getJewishCalendar().getJewishYear()))) {
                hebrewYear += " / " + mJewishDateInfo.getJewishCalendar().getJewishYear();
            }
        }
        String monthYear = month + " " + year;
        mEnglishMonthYear.setText(monthYear);
        if (LocaleChecker.isLocaleHebrew()) {
            mEnglishMonthYear.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            mHebrewMonthYear.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
        mLocationName.setText(sCurrentLocationName);
        String hebrewMonthYear = hebrewMonth + " " + hebrewYear;
        mHebrewMonthYear.setText(hebrewMonthYear);
        mWeeklyParsha.setText(mJewishDateInfo.getThisWeeksParsha());
        if (mJewishDateInfo.getThisWeeksHaftarah().isEmpty()) {
            mWeeklyHaftorah.setVisibility(View.GONE);
        } else {
            mWeeklyHaftorah.setText(mJewishDateInfo.getThisWeeksHaftarah());
        }
        mROZmanimCalendar.getCalendar().setTimeInMillis(backupCal.getTimeInMillis());
        mJewishDateInfo.setCalendar(backupCal);
        mCurrentDateShown = backupCal;
    }

    private String[] getShortZmanim() {
        List<ZmanListEntry> zmanim = new ArrayList<>();
        addZmanim(zmanim, true, sSettingsPreferences, sSharedPreferences, mROZmanimCalendar, mJewishDateInfo, mIsZmanimInHebrew, mIsZmanimEnglishTranslated);
        DateFormat zmanimFormat;
        if (LocaleChecker.isLocaleHebrew()) {
            if (sSettingsPreferences.getBoolean("ShowSeconds", false)) {
                zmanimFormat = new SimpleDateFormat("H:mm:ss", Locale.getDefault());
            } else {
                zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
            }
        } else {
            if (sSettingsPreferences.getBoolean("ShowSeconds", false)) {
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
                    if (zman.isRTZman() && sSettingsPreferences.getBoolean("RoundUpRT", false)) {
                        DateFormat rtFormat;
                        if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                            if (sSettingsPreferences.getBoolean("ShowSeconds", false)) {
                                rtFormat = new SimpleDateFormat("H:mm:ss", Locale.getDefault());
                            } else {
                                rtFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
                            }
                        } else {
                            if (sSettingsPreferences.getBoolean("ShowSeconds", false)) {
                                rtFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
                            } else {
                                rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                            }
                        }
                        rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                        if (!Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                            mZmanimForAnnouncements.add(rtFormat.format(zman.getZman()) + " :" + zman.getTitle().replaceAll("\\(.*\\)", "").trim());
                        } else {
                            mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ": " + rtFormat.format(zman.getZman()));
                        }
                    } else {
                        if (!Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                            mZmanimForAnnouncements.add(zmanimFormat.format(zman.getZman()) + " :" + zman.getTitle().replaceAll("\\(.*\\)", "").trim());
                        } else {
                            mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ": " + zmanimFormat.format(zman.getZman()));
                        }
                    }
                    zmansToRemove.add(zman);
                }
            }
        } else {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isNoteworthyZman()) {
                    if (zman.isRTZman() && sSettingsPreferences.getBoolean("RoundUpRT", false)) {
                        DateFormat rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                        rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                        mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ": " + rtFormat.format(zman.getZman()));
                    } else {
                        mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ": " + zmanimFormat.format(zman.getZman()));
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
                if (zman.isRTZman() && sSettingsPreferences.getBoolean("RoundUpRT", false)) {
                    DateFormat rtFormat;
                    if (LocaleChecker.isLocaleHebrew()) {
                        if (sSettingsPreferences.getBoolean("ShowSeconds", false)) {
                            rtFormat = new SimpleDateFormat("H:mm:ss", Locale.getDefault());
                        } else {
                            rtFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
                        }
                    } else {
                        if (sSettingsPreferences.getBoolean("ShowSeconds", false)) {
                            rtFormat = new SimpleDateFormat("h:mm:ss aa", Locale.getDefault());
                        } else {
                            rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                        }
                    }
                    rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                    if (!Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                        shortZmanim[zmanim.indexOf(zman)] = rtFormat.format(zman.getZman()) + " :" +  zman.getTitle();
                    } else {
                        shortZmanim[zmanim.indexOf(zman)] = zman.getTitle() + " : " +  rtFormat.format(zman.getZman());
                    }
                } else {
                    if (!Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                        shortZmanim[zmanim.indexOf(zman)] = zmanimFormat.format(zman.getZman()) + " :" + zman.getTitle().replace("סוף זמן ", "");
                    } else {
                        shortZmanim[zmanim.indexOf(zman)] = zman.getTitle().replace("סוף זמן ", "") + ": " + zmanimFormat.format(zman.getZman());
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
                if (zman.isRTZman() && sSettingsPreferences.getBoolean("RoundUpRT", false)) {
                    DateFormat rtFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    rtFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
                    shortZmanim[zmanim.indexOf(zman)] = zman.getTitle() + ": " + rtFormat.format(zman.getZman());
                } else {
                    shortZmanim[zmanim.indexOf(zman)] = zman.getTitle()
                            .replace("Earliest ","")
                            .replace("Sof Zman ", "")
                            .replace("Hacochavim", "")
                            .replace("Latest ", "")
                            + ": " + zmanimFormat.format(zman.getZman());
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
        if (LocaleChecker.isLocaleHebrew()) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
        mROZmanimCalendar.getCalendar().add(Calendar.DATE,1);//check next day for tekufa, because the tekufa time can go back a day
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE,-1);//reset the calendar to check for the current date
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
        mROZmanimCalendar.getCalendar().add(Calendar.DATE,-1);//reset the calendar to check for the current date

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

    public void addTekufaLength(List<ZmanListEntry> zmanim, String opinion) {
        DateFormat zmanimFormat;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));

        Date tekufa = null;
        Date aHTekufa = null;

        mROZmanimCalendar.getCalendar().add(Calendar.DATE, 1);//check next day for tekufa, because the tekufa time can go back a day
        mJewishDateInfo.setCalendar(mROZmanimCalendar.getCalendar());
        mROZmanimCalendar.getCalendar().add(Calendar.DATE, -1);//reset the calendar to check for the current date

        if (mJewishDateInfo.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            final Calendar cal2 = (Calendar) mROZmanimCalendar.getCalendar().clone();
            cal2.setTime(mJewishDateInfo.getJewishCalendar().getTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {
                tekufa = mJewishDateInfo.getJewishCalendar().getTekufaAsDate();
                aHTekufa = mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate();
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
                tekufa = mJewishDateInfo.getJewishCalendar().getTekufaAsDate();
                aHTekufa = mJewishDateInfo.getJewishCalendar().getAmudeiHoraahTekufaAsDate();
            }
        }

        if (tekufa != null && aHTekufa != null) {
            Date halfHourBefore;
            Date halfHourAfter;
            if (opinion.equals("1") && !sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
                halfHourBefore = new Date(tekufa.getTime() - (DateUtils.MILLIS_PER_HOUR / 2));
                halfHourAfter = new Date(tekufa.getTime() + (DateUtils.MILLIS_PER_HOUR / 2));
                if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourAfter) + " - " + zmanimFormat.format(halfHourBefore)));
                } else {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter)));
                }
            }
            if (opinion.equals("2") || sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
                halfHourBefore = new Date(aHTekufa.getTime() - (DateUtils.MILLIS_PER_HOUR / 2));
                halfHourAfter = new Date(aHTekufa.getTime() + (DateUtils.MILLIS_PER_HOUR / 2));
                if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourAfter) + " - " + zmanimFormat.format(halfHourBefore)));
                } else {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter)));
                }
            }
            if (opinion.equals("3")) {
                halfHourBefore = new Date(aHTekufa.getTime() - (DateUtils.MILLIS_PER_HOUR / 2));
                halfHourAfter = new Date(tekufa.getTime() + (DateUtils.MILLIS_PER_HOUR / 2));
                if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourAfter) + " - " + zmanimFormat.format(halfHourBefore)));
                } else {
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter)));
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
        if (sCurrentLocationName != null) {// Somehow it is null and crashing in some places
            if (sCurrentLocationName.contains("Lat:") && sCurrentLocationName.contains("Long:")
                    && sSettingsPreferences.getBoolean("SetElevationToLastKnownLocation", false)) {//only if the user has enabled the setting to set the elevation to the last known location
                sUserIsOffline = true;
                sElevation = Double.parseDouble(sSharedPreferences.getString("elevation" + sSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
            } else {//user is online, get the elevation from the shared preferences for the current location
                sElevation = Double.parseDouble(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));//get the last value of the current location or 0 if it doesn't exist
            }
        }

        if (!sUserIsOffline && sSharedPreferences.getBoolean("useElevation", true)
                && !sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {//update if the user is online and the elevation setting is enabled
            if (!sSharedPreferences.contains("elevation" + sCurrentLocationName)) {//if the elevation for this location has never been set
                mLocationResolver = new LocationResolver(mContext, mActivity);
                mLocationResolver.start();
                try {
                    mLocationResolver.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sElevation = Double.parseDouble(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
                seeIfTablesNeedToBeUpdated(false);
            } else {
                sElevation = Double.parseDouble(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
            }
        }

        if (!sSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            sElevation = 0;
        }
    }

    /**
     * This method initializes the shabbat mode banner and sets up the functionality of hiding the banner when the user taps on it.
     */
    private void setupShabbatModeBanner() {
        mShabbatModeBanner = binding.shabbatMode;
        mShabbatModeBanner.setSelected(true);
        mShabbatModeBanner.setOnClickListener(v -> {
            if (v.getVisibility() == View.VISIBLE) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        });
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
            if (mNavView != null && mViewPager != null) {
                mNavView.setVisibility(View.GONE);
                mViewPager.setUserInputEnabled(false);
            }
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mActivity.getWindow().setHideOverlayWindows(true);
            }
            WindowManager windowManager =  (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Configuration configuration = getResources().getConfiguration();
            int rotation = windowManager.getDefaultDisplay().getRotation();
            // Search for the natural position of the device
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) ||
                    configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
                            (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)) {
                switch (rotation) {// Natural position is Landscape
                    case Surface.ROTATION_0:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                    case Surface.ROTATION_90:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        break;
                    case Surface.ROTATION_180:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        break;
                    case Surface.ROTATION_270:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                }
            } else {// Natural position is Portrait
                switch (rotation) {
                    case Surface.ROTATION_0:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                    case Surface.ROTATION_90:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                    case Surface.ROTATION_180:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        break;
                    case Surface.ROTATION_270:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        break;
                }
            }
            TextClock clock = binding.clock;
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                clock.setVisibility(View.VISIBLE);
                if (LocaleChecker.isLocaleHebrew()) {
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                setShabbatBannerColors(false);
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
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
                    sb.append(mContext.getString(R.string.PESACH));
                    if (isShabbat) {
                        sb.append(mContext.getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(mContext.getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(mContext.getColor(R.color.lightYellow));
                mShabbatModeBanner.setTextColor(mContext.getColor(R.color.black));
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.lightYellow));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(mCurrentDateShown));
                break;
            case JewishCalendar.SHAVUOS:
                for (int i = 0; i < 4; i++) {
                    sb.append(mContext.getString(R.string.SHAVUOT));
                    if (isShabbat) {
                        sb.append(mContext.getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(mContext.getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(mContext.getColor(R.color.light_blue));
                mShabbatModeBanner.setTextColor(mContext.getColor(R.color.white));
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.light_blue));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight(mCurrentDateShown));
                break;
            case JewishCalendar.SUCCOS:
                for (int i = 0; i < 4; i++) {
                    sb.append(mContext.getString(R.string.SUCCOT));
                    if (isShabbat) {
                        sb.append(mContext.getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(mContext.getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(mContext.getColor(R.color.light_green));
                mShabbatModeBanner.setTextColor(mContext.getColor(R.color.black));
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.light_green));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(mCurrentDateShown));
                break;
            case JewishCalendar.SHEMINI_ATZERES:
                for (int i = 0; i < 4; i++) {
                    sb.append(mContext.getString(R.string.SHEMINI_ATZERET));
                    if (isShabbat) {
                        sb.append(mContext.getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(mContext.getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(mContext.getColor(R.color.light_green));
                mShabbatModeBanner.setTextColor(mContext.getColor(R.color.black));
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.light_green));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(mCurrentDateShown));
                break;
            case JewishCalendar.SIMCHAS_TORAH:
                for (int i = 0; i < 4; i++) {
                    sb.append(mContext.getString(R.string.SIMCHAT_TORAH));
                    if (isShabbat) {
                        sb.append(mContext.getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(mContext.getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(mContext.getColor(R.color.green));
                mShabbatModeBanner.setTextColor(mContext.getColor(R.color.black));
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.green));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(mCurrentDateShown));
                break;
            case JewishCalendar.ROSH_HASHANA:
                for (int i = 0; i < 4; i++) {
                    sb.append(mContext.getString(R.string.ROSH_HASHANA));
                    if (isShabbat) {
                        sb.append(mContext.getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(mContext.getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(mContext.getColor(R.color.dark_red));
                mShabbatModeBanner.setTextColor(mContext.getColor(R.color.white));
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.dark_red));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight(mCurrentDateShown));
                break;
            case JewishCalendar.YOM_KIPPUR:
                for (int i = 0; i < 4; i++) {
                    sb.append(mContext.getString(R.string.YOM_KIPPUR));
                    if (isShabbat) {
                        sb.append(mContext.getString(R.string.slash_SHABBAT));
                    }
                    sb.append(" ").append(mContext.getString(R.string.MODE)).append("                ");
                }
                mShabbatModeBanner.setText(sb.toString());
                mShabbatModeBanner.setBackgroundColor(mContext.getColor(R.color.white));
                mShabbatModeBanner.setTextColor(mContext.getColor(R.color.black));
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.white));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(mCurrentDateShown));
                break;
            default:
                mShabbatModeBanner.setText(mContext.getString(R.string.SHABBAT_MODE) +
                        "                " +
                        mContext.getString(R.string.SHABBAT_MODE) +
                        "               " +
                        mContext.getString(R.string.SHABBAT_MODE) +
                        "               " +
                        mContext.getString(R.string.SHABBAT_MODE) +
                        "               " +
                        mContext.getString(R.string.SHABBAT_MODE));
                mShabbatModeBanner.setBackgroundColor(mContext.getColor(R.color.dark_blue));
                mShabbatModeBanner.setTextColor(mContext.getColor(R.color.white));
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.dark_blue));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight(mCurrentDateShown));
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
            while (mMainRecyclerView != null && mMainRecyclerView.canScrollVertically(1)) {
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
            while (mMainRecyclerView != null && mMainRecyclerView.canScrollVertically(-1)) {
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
            if (mNavView != null && mViewPager != null && sSettingsPreferences != null && !sSettingsPreferences.getBoolean("hideBottomBar", false)) {
                mNavView.setVisibility(View.VISIBLE);
                mViewPager.setUserInputEnabled(true);
            }
            mHandler.removeCallbacksAndMessages(mZmanimUpdater);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mActivity.getWindow().setHideOverlayWindows(false);
            }
            if (sSharedPreferences.getBoolean("useDefaultCalButtonColor", true)) {
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.dark_blue));
            } else {
                mCalendarButton.setBackgroundColor(sSharedPreferences.getInt("CalButtonColor", 0x18267C));
            }
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mROZmanimCalendar.getCalendar()));
            mNextDate.setVisibility(View.VISIBLE);
            mPreviousDate.setVisibility(View.VISIBLE);
            if (binding != null) {
                TextClock clock = binding.clock;
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    clock.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onPause() {
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mMainRecyclerView != null) {
            mCurrentPosition = ((LinearLayoutManager) Objects.requireNonNull(mMainRecyclerView.getLayoutManager())).findFirstVisibleItemPosition();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        initMenu();
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mROZmanimCalendar == null) {
            super.onResume();
            return;
        } else {// update right away in case user changed the date
            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            } else {
                updateDailyZmanim();
            }
        }
        if (sSharedPreferences.getBoolean("shouldRefresh", false)) {
            mJewishDateInfo = new JewishDateInfo(sSharedPreferences.getBoolean("inIsrael", false));
            mJewishDateInfo.setCalendar(mCurrentDateShown);
            setZmanimLanguageBools();
            resolveElevationAndVisibleSunrise();
            instantiateZmanimCalendar();
            mROZmanimCalendar.setCalendar(mCurrentDateShown);
            setNextUpcomingZman();
            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyTextViewTextColor();
                updateWeeklyZmanim();
            } else {
                updateDailyZmanim();
                mMainRecyclerView.scrollToPosition(mCurrentPosition);
            }
            resetTheme();
            //this is to update the zmanim notifications if the user changed the settings to start showing them
            saveGeoLocationInfo();
            PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(
                    mContext,
                    0,
                    new Intent(mContext, ZmanimNotifications.class),
                    PendingIntent.FLAG_IMMUTABLE);
            try {
                zmanimPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
            checkIfUserIsInIsraelOrNot();
            sSharedPreferences.edit().putBoolean("shouldRefresh", false).apply();
        }

        if (binding != null) {
            TextClock clock = binding.clock;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                clock.setVisibility(View.VISIBLE);
                if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                    clock.setFormat24Hour("H:mm:ss");
                }
            } else {
                if (sShabbatMode) {
                    clock.setVisibility(View.VISIBLE);
                }
            }
        }

        if (sSharedPreferences.getBoolean("useImage", false)) {
            Bitmap bitmap = BitmapFactory.decodeFile(sSharedPreferences.getString("imageLocation", ""));
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            mLayout.setBackground(drawable);
        } else if (sSharedPreferences.getBoolean("customBackgroundColor", false) &&
                !sSharedPreferences.getBoolean("useDefaultBackgroundColor", false)) {
            mLayout.setBackgroundColor(sSharedPreferences.getInt("bColor", 0x32312C));
        } else {
            mLayout.setBackgroundColor(0);
        }
        if (!sShabbatMode) {
            if (sSharedPreferences.getBoolean("useDefaultCalButtonColor", true)) {
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.dark_blue));
            } else {
                mCalendarButton.setBackgroundColor(sSharedPreferences.getInt("CalButtonColor", 0x18267C));
            }
        }
        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));

        if (sLastTimeUserWasInApp == null) {
            sLastTimeUserWasInApp = new Date();
        }
        if (!DateUtils.isSameDay(mCurrentDateShown.getTime(), new Date())
                && (new Date().getTime() - sLastTimeUserWasInApp.getTime()) > 7_200_000) {//two hours
            mCurrentDateShown.setTime(new Date());
            mROZmanimCalendar.setCalendar(mCurrentDateShown); // no need to check for null pointers
            mJewishDateInfo.setCalendar(mCurrentDateShown);
            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            } else {
                updateDailyZmanim();
                mMainRecyclerView.scrollToPosition(mCurrentPosition);
            }
        }
        sLastTimeUserWasInApp = new Date();

        if (sSettingsPreferences.getBoolean("showNextZmanNotification", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mActivity.startForegroundService(new Intent(mContext, NextZmanCountdownNotification.class));
            } else {
                mActivity.startService(new Intent(mContext, NextZmanCountdownNotification.class));
            }
        }

        PrefToWatchSender.send(mContext);

        super.onResume();
    }

    /**
     * sets the theme of the app according to the user's preferences.
     */
    private void resetTheme() {
        String theme = sSettingsPreferences.getString("theme", "Auto (Follow System Theme)");
        switch (theme) {
            case "Auto (Follow System Theme)":
                if (((AppCompatActivity) mActivity).getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                    break;
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Day":
                if (((AppCompatActivity) mActivity).getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
                    break;
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Night":
                if (((AppCompatActivity) mActivity).getDelegate().getLocalNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    break;
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        sSharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPrefListener);
    }

    /**
     * This method accepts a new location object after a request is made from the {@link LocationResolver} class.
     * @param location the input argument
     */
    @Override
    public void accept(Location location) {
        if (location != null) {
            sLatitude = location.getLatitude();
            sLongitude = location.getLongitude();
            mLocationResolver = new LocationResolver(mContext, mActivity);
            mLocationResolver.resolveCurrentLocationName();
            mLocationResolver.setTimeZoneID();
            mActivity.runOnUiThread(() -> {
                if (mMainRecyclerView.isFocusable()) {
                    resolveElevationAndVisibleSunrise();
                    if (mCurrentDateShown != null && mJewishDateInfo != null) {
                        mCurrentDateShown.setTime(new Date());
                        mJewishDateInfo.setCalendar(new GregorianCalendar());
                        instantiateZmanimCalendar();
                        setNextUpcomingZman();
                        if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                            updateWeeklyTextViewTextColor();
                            updateWeeklyZmanim();
                        } else {
                            updateDailyZmanim();
                            mMainRecyclerView.scrollToPosition(mCurrentPosition);
                        }
                        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                    }
                }
            });
        }
    }
}